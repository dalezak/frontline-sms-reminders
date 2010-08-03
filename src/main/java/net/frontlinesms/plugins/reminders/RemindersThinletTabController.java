/*
 * FrontlineSMS <http://www.frontlinesms.com>
 * Copyright 2007, 2008 kiwanja
 * 
 * This file is part of FrontlineSMS.
 * 
 * FrontlineSMS is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * FrontlineSMS is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FrontlineSMS. If not, see <http://www.gnu.org/licenses/>.
 */
package net.frontlinesms.plugins.reminders;

import java.util.Date;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.text.DateFormat;

import net.frontlinesms.EmailServerHandler;
import net.frontlinesms.FrontlineSMS;
import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.data.repository.ContactDao;
import net.frontlinesms.data.repository.EmailAccountDao;
import net.frontlinesms.data.repository.EmailDao;
import net.frontlinesms.plugins.BasePluginThinletTabController;
import net.frontlinesms.ui.Icon;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.events.TabChangedNotification;
import net.frontlinesms.ui.handler.ComponentPagingHandler;
import net.frontlinesms.ui.handler.PagedComponentItemProvider;
import net.frontlinesms.ui.handler.PagedListDetails;
import net.frontlinesms.ui.i18n.InternationalisationUtils;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import thinlet.Thinlet;
import thinlet.ThinletText;

import net.frontlinesms.plugins.reminders.data.domain.Reminder;
import net.frontlinesms.plugins.reminders.data.domain.Reminder.Status;
import net.frontlinesms.plugins.reminders.data.repository.ReminderDao;

import net.frontlinesms.data.domain.Contact;
import net.frontlinesms.data.domain.Email;
import net.frontlinesms.data.domain.EmailAccount;
import net.frontlinesms.events.EventObserver;
import net.frontlinesms.events.FrontlineEventNotification;

/*
 * RemindersThinletTabController
 * @author Dale Zak
 * 
 * see {@link "http://www.frontlinesms.net"} for more details. 
 * copyright owned by Kiwanja.net
 */
public class RemindersThinletTabController extends BasePluginThinletTabController<RemindersPluginController> implements ThinletUiEventHandler, PagedComponentItemProvider, EventObserver, RemindersCallback {

	private static Logger LOG = FrontlineUtils.getLogger(RemindersThinletTabController.class);
	
	private FrontlineSMS frontlineController;
	private ApplicationContext applicationContext;
	
	private final ReminderDao reminderDao;
	private final ContactDao contactDao;
	private final EmailDao emailDao;
	private final EmailAccountDao emailAccountDao;
	private final EmailServerHandler emailManager;
	
	private ComponentPagingHandler reminderListPager;
	private Object tabComponent;
	private Object reminderListComponent;
	private Object comboEmailAccount;
	private RemindersDialogHandler dialogHandler;
	
	private static final String TAB_XML = "/ui/plugins/reminders/remindersTab.xml";
	
	private static final String TAB_TABLE = "tableReminders";
	private static final String TAB_TABLE_PANEL = "panelTableReminders";
	
	private static final String TAB_MENU = "menuReminders";
	private static final String TAB_MENU_CREATE = "menuCreateReminder";
	private static final String TAB_MENU_SEND = "menuSendReminder";
	private static final String TAB_MENU_EDIT = "menuEditReminder";
	private static final String TAB_MENU_DELETE = "menuDeleteReminder";
	
	private static final String TAB_TOOLBAR = "toolbarReminders";
	private static final String TAB_TOOLBAR_CREATE = "buttonCreateReminder";
	private static final String TAB_TOOLBAR_SEND = "buttonSendReminder";
	private static final String TAB_TOOLBAR_EDIT = "buttonEditReminder";
	private static final String TAB_TOOLBAR_DELETE = "buttonDeleteReminder";
	
	/**
	 * RemindersThinletTabController
	 * @param pluginController RemindersPluginController
	 * @param uiController UiGeneratorController
	 * @param applicationContext ApplicationContext
	 */
	public RemindersThinletTabController(RemindersPluginController pluginController, UiGeneratorController uiController, ApplicationContext applicationContext) {
		super(pluginController, uiController);
		this.applicationContext = applicationContext;
		
		this.tabComponent = uiController.loadComponentFromFile(TAB_XML, this);
		super.setTabComponent(this.tabComponent);
		
		this.reminderDao = (ReminderDao) applicationContext.getBean("reminderDao");
		this.contactDao = this.ui.getFrontlineController().getContactDao();
		this.emailDao = this.ui.getFrontlineController().getEmailDao();
		this.emailAccountDao = this.ui.getFrontlineController().getEmailAccountFactory();
		this.emailManager = this.ui.getFrontlineController().getEmailServerHandler();
		
		this.reminderListComponent = this.ui.find(this.tabComponent, TAB_TABLE);
		this.reminderListPager = new ComponentPagingHandler(this.ui, this, reminderListComponent);
		this.comboEmailAccount = this.ui.find(this.tabComponent, "comboEmailAccount");
		
		Object panelReminders = this.ui.find(this.tabComponent, TAB_TABLE_PANEL);
		this.ui.add(panelReminders, this.reminderListPager.getPanel());
		
		this.reminderListPager.setCurrentPage(0);
		this.reminderListPager.refresh();
		
		loadEmailAccounts();
	}
	
	public void notify(FrontlineEventNotification notification) {
		if (notification instanceof TabChangedNotification) {
			loadEmailAccounts();
		}
	}
	
	/**
	 * The ScheduledExecutorService provider used for scheduling tasks
	 */
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	
	/**
	 * Hashmap to store Reminder to ScheduledFuture to allow cancelling of scheduled tasks
	 */
	private final HashMap<Reminder,ScheduledFuture<?>> futures = new HashMap<Reminder,ScheduledFuture<?>>();
	
	/**
	 * Schedule a Reminder
	 */
	public void scheduleReminder(Reminder reminder) {
		LOG.debug("Callback.scheduleReminder: " + reminder);
		if (reminder != null && reminder.getStatus() != Status.SENT) {
			Calendar start = (Calendar)reminder.getStartCalendar().clone();
			Calendar now = Calendar.getInstance();
			if (reminder.getPeriod() > 0) {
				while (start.before(now)) {
					start.add(Calendar.MILLISECOND, (int)reminder.getPeriod());
				}
				LOG.debug("Reminder Scheduled: " + reminder);
				long initialDelay = start.getTimeInMillis() - now.getTimeInMillis();
				ScheduledFuture<?> future = this.scheduler.scheduleAtFixedRate(reminder, initialDelay, reminder.getPeriod(), TimeUnit.MILLISECONDS);
				this.futures.put(reminder, future);
			}
			else {
				LOG.debug("Reminder Scheduled: " + reminder);
				long initialDelay = start.getTimeInMillis() - now.getTimeInMillis();
				ScheduledFuture<?> future = this.scheduler.schedule(reminder, initialDelay, TimeUnit.MILLISECONDS);
				this.futures.put(reminder, future);
			}
		}
	}
	
	/**
	 * Stop a Reminder
	 */
	public void stopReminder(Reminder reminder) {
		LOG.debug("Callback.stopReminder: " + reminder);
		if (reminder != null && this.futures.containsKey(reminder)) {
			ScheduledFuture<?> future = futures.get(reminder);
			if (future != null) {
				boolean result = future.cancel(true);
				LOG.debug("Cancel: " + result);	
			}	
		}
	}
	
	/**
	 * Refresh Reminders
	 */
	public void refreshReminders(Reminder reminder) {
		LOG.debug("Callback.refreshReminders: " + reminder);
		if (reminder != null) {
			this.reminderDao.updateReminder(reminder);
		}
		this.reminderListPager.refresh();
	}
	
	/**
	 * Send a collection of Reminders
	 * @param list
	 */
	public void sendReminder(Object list) {
		for (Object selected : this.ui.getSelectedItems(list)) {
			sendReminder((Reminder)this.ui.getAttachedObject(selected, Reminder.class));
		}
	}
	
	/**
	 * Send a Reminder
	 */
	public void sendReminder(Reminder reminder) {
		LOG.debug("sendReminder: " + reminder);	
		if (reminder != null) {
			if (reminder.getType() == Reminder.Type.EMAIL) {
				EmailAccount emailAccount = this.getEmailAccount();
				if (emailAccount != null) {
					for (String contactName : reminder.getRecipientsArray()) {
						LOG.trace("Sending EMAIL");
						Contact contact = this.contactDao.getContactByName(contactName);
						Email email = new Email(emailAccount, contact.getEmailAddress(), reminder.getSubject(), reminder.getContent());	
						this.emailDao.saveEmail(email);
						this.emailManager.sendEmail(email);
					}
					this.ui.setStatus(InternationalisationUtils.getI18NString(RemindersConstants.EMAIL_REMINDER_SENT));
				}
				else {
					this.ui.alert(InternationalisationUtils.getI18NString(RemindersConstants.MISSING_EMAIL_ACCOUNT));
				}
			}
			else if (reminder.getType() == Reminder.Type.MESSAGE) {
				for (String contactName : reminder.getRecipientsArray()) {
					Contact contact = this.contactDao.getContactByName(contactName);
					if (contact != null) {
						LOG.trace("Sending MESSAGE");
						this.frontlineController.sendTextMessage(contact.getPhoneNumber(), reminder.getContent());
					}
				}
				this.ui.setStatus(InternationalisationUtils.getI18NString(RemindersConstants.SMS_REMINDER_SENT));
			}
			Calendar now = Calendar.getInstance();
			if (now.equals(reminder.getEndCalendar())) {
				LOG.debug("now == end: " + reminder);
				reminder.setStatus(Reminder.Status.SENT);
			} 
			else if (now.after(reminder.getEndCalendar())) {
				LOG.debug("now > end: " + reminder);
				reminder.setStatus(Reminder.Status.SENT);
			}
			this.reminderDao.updateReminder(reminder);
		}
		this.reminderListPager.refresh();
	}
	
	/**
	 * Cancel all Reminders
	 */
	public void cancelAllReminders() {
		LOG.debug("Callback.cancelAllReminders");
		if (this.scheduler != null) {
			this.scheduler.shutdownNow();
		}
	}
	
	public void setFrontline(FrontlineSMS frontlineController) {
		this.frontlineController = frontlineController;
		this.frontlineController.getEventBus().registerObserver(this);
	}
	
	public Object getTab(){
		return super.getTabComponent();
	}
	
	private void loadEmailAccounts() {
		System.out.println("loadEmailAccounts");
		int index = 0;
		String selectedEmailAccount = RemindersPluginProperties.getEmailAccount();
		for (EmailAccount emailAccount : this.emailAccountDao.getAllEmailAccounts()) {
			String comboBoxText = String.format("%s : %s : %s : %s", emailAccount.getAccountName(), emailAccount.getAccountServer(), emailAccount.getProtocol(), emailAccount.getAccountServerPort());
			Object comboBoxItem = this.ui.createComboboxChoice(comboBoxText, emailAccount);
			this.ui.add(this.comboEmailAccount, comboBoxItem);
			if (emailAccount.getAccountName().equalsIgnoreCase(selectedEmailAccount)) {
				this.ui.setSelectedIndex(this.comboEmailAccount, index);
			}
			index++;
		}
	}
	
	private EmailAccount getEmailAccount() {
		Object selectedItem = this.ui.getSelectedItem(this.comboEmailAccount);
		if (selectedItem != null) {
			return (EmailAccount)this.ui.getAttachedObject(selectedItem);
		}
		return null;
	}
	
	public void emailAccountChanged(Object comboEmailAccount) {
		Object selectedItem = this.ui.getSelectedItem(comboEmailAccount);
		if (selectedItem != null) {
			EmailAccount emailAccount = (EmailAccount)this.ui.getAttachedObject(selectedItem);
			if (emailAccount != null) {
				RemindersPluginProperties.setEmailAccount(emailAccount.getAccountName());
			}
			else {
				RemindersPluginProperties.setEmailAccount(null);
			}
		}
		else {
			RemindersPluginProperties.setEmailAccount(null);
		}
	}
	
	public void removeSelectedFromReminderList() {
		LOG.trace("removeSelectedFromReminderList");
		final Object[] selected = this.ui.getSelectedItems(this.reminderListComponent);
		for (Object o : selected) {
			Reminder reminder = this.ui.getAttachedObject(o, Reminder.class);
			LOG.trace("Deleting Reminder:" + reminder);
			if (reminder != null) {
				this.reminderDao.deleteReminder(reminder);
				if (reminder.getType() == Reminder.Type.EMAIL) {
					this.ui.setStatus(InternationalisationUtils.getI18NString(RemindersConstants.EMAIL_REMINDER_CREATED));
				}
				else {
					this.ui.setStatus(InternationalisationUtils.getI18NString(RemindersConstants.SMS_REMINDER_CREATED));
				}
			}
		}
		this.ui.removeConfirmationDialog();
		this.reminderListPager.refresh();
	
		Object list = this.ui.find(this.tabComponent, TAB_TABLE);
		Object popup = this.ui.find(this.tabComponent, TAB_MENU);
		Object toolbar = this.ui.find(this.tabComponent, TAB_TOOLBAR);
		enableOptions(list, popup, toolbar);
	}
	
	public void enableOptions(Object list, Object popup, Object toolbar) {
		Object[] selectedItems = this.ui.getSelectedItems(list);
		boolean hasSelection = selectedItems.length > 0;
		for (Object o : this.ui.getItems(popup)) {
			String name = this.ui.getString(o, Thinlet.NAME);
			if (name == null) { 
				continue;
			}
			else if (name.equals(TAB_MENU_CREATE)) {
				this.ui.setEnabled(o, true);
			}
			else if (name.equals(TAB_MENU_EDIT)) {
				this.ui.setEnabled(o, hasSelection);
			}
			else if (name.equals(TAB_MENU_DELETE)) {
				this.ui.setEnabled(o, hasSelection);
			}
			else if (name.equals(TAB_MENU_SEND)) {
				this.ui.setEnabled(o, hasSelection);
			}
		}
		for (Object o : this.ui.getItems(toolbar)) {
			String name = this.ui.getString(o, Thinlet.NAME);
			if (name == null) { 
				continue;
			}
			else if (name.equals(TAB_TOOLBAR_CREATE)) {
				this.ui.setEnabled(o, true);
			}
			else if (name.equals(TAB_TOOLBAR_EDIT)) {
				this.ui.setEnabled(o, hasSelection);
			}
			else if (name.equals(TAB_TOOLBAR_DELETE)) {
				this.ui.setEnabled(o, hasSelection);
			}
			else if (name.equals(TAB_TOOLBAR_SEND)) {
				this.ui.setEnabled(o, hasSelection);
			}
		}
	}

	public void showReminderDialog() {
		showReminderDialog(null);
	}
	
	public void showReminderDialog(Object reminderList) {
		this.dialogHandler = new RemindersDialogHandler(this.ui, this.applicationContext, this);
		if (reminderList != null) {
			final Object selected = this.ui.getSelectedItem(reminderList);
			Reminder reminder = this.ui.getAttachedObject(selected, Reminder.class);
			this.dialogHandler.init(reminder);
		}
		else {
			this.dialogHandler.init(null);
		}
	}
	
	public PagedListDetails getListDetails(Object list, int startIndex, int limit) {
		LOG.trace("getListDetails:" + this.ui.getName(list));
			List<Reminder> reminders = this.reminderDao.getAllReminders(startIndex, limit);
			Object[] listItems = toThinletReminders(reminders);
			return new PagedListDetails(listItems.length, listItems);
	}
	
	private Object[] toThinletReminders(List<Reminder> reminders) {
		Object[] components = new Object[reminders.size()];
		for (int i = 0; i < components.length; i++) {
			Reminder r = reminders.get(i);
			components[i] = getReminderRow(r);
		}
		return components;
	}
	
	private Object getReminderRow(Reminder reminder) {
		Object row = this.ui.createTableRow(reminder);
		
		if (reminder.getStatus() == Reminder.Status.SENT) {
			Object statusCell = this.ui.createTableCell("");
			this.ui.setIcon(statusCell, Icon.TICK);
			this.ui.setChoice(statusCell, ThinletText.ALIGNMENT, ThinletText.CENTER);
			this.ui.add(row, statusCell);
		}
		else {
			this.ui.add(row, this.ui.createTableCell(""));
		}
		
		Object typeCell = this.ui.createTableCell("");
		if (reminder.getType() == Reminder.Type.EMAIL) {
			this.ui.setIcon(typeCell, Icon.EMAIL);
		}
		else {
			this.ui.setIcon(typeCell, Icon.SMS);
		}
		this.ui.setChoice(typeCell, ThinletText.ALIGNMENT, ThinletText.CENTER);
		this.ui.add(row, typeCell);
		
		Object occuranceCell = this.ui.createTableCell(reminder.getOccurrenceLabel());
		this.ui.setChoice(occuranceCell, ThinletText.ALIGNMENT, ThinletText.CENTER);
		this.ui.add(row, occuranceCell);
		
		DateFormat dateFormat = InternationalisationUtils.getDatetimeFormat();
		
		if (reminder.getStartDate() > 0) {
			Date startdate = new Date(reminder.getStartDate());
			this.ui.add(row, this.ui.createTableCell(dateFormat.format(startdate)));
		}
		else {
			this.ui.add(row, this.ui.createTableCell(""));
		}
		
		if (reminder.getEndDate() > 0) {
			Date enddate = new Date(reminder.getEndDate());
			this.ui.add(row, this.ui.createTableCell(dateFormat.format(enddate)));
		}
		else {
			this.ui.add(row, this.ui.createTableCell(""));
		}
		
		this.ui.add(row, this.ui.createTableCell(reminder.getRecipients()));
		this.ui.add(row, this.ui.createTableCell(reminder.getSubject()));
		this.ui.add(row, this.ui.createTableCell(reminder.getContent()));
        
		return row;
	}
	
}
