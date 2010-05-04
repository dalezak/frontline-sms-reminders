package net.frontlinesms.plugins.reminders;

import java.util.Collection;
import java.util.Date;
import java.util.Calendar;
import java.util.List;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;

import net.frontlinesms.EmailServerHandler;
import net.frontlinesms.FrontlineSMS;
import net.frontlinesms.data.repository.ContactDao;
import net.frontlinesms.data.repository.EmailAccountDao;
import net.frontlinesms.data.repository.EmailDao;
import net.frontlinesms.plugins.BasePluginThinletTabController;
import net.frontlinesms.ui.Icon;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.handler.ComponentPagingHandler;
import net.frontlinesms.ui.handler.PagedComponentItemProvider;
import net.frontlinesms.ui.handler.PagedListDetails;
import net.frontlinesms.ui.i18n.InternationalisationUtils;

import org.springframework.context.ApplicationContext;

import thinlet.Thinlet;
import thinlet.ThinletText;

import net.frontlinesms.plugins.reminders.data.domain.Reminder;
import net.frontlinesms.plugins.reminders.data.domain.Reminder.Type;
import net.frontlinesms.plugins.reminders.data.repository.ReminderDao;

import net.frontlinesms.data.domain.Contact;
import net.frontlinesms.data.domain.Email;
import net.frontlinesms.data.domain.EmailAccount;

public class RemindersThinletTabController extends BasePluginThinletTabController<RemindersPluginController> implements ThinletUiEventHandler, PagedComponentItemProvider {

	private FrontlineSMS frontlineController;
	private ApplicationContext applicationContext;
	
	private final ReminderDao reminderDao;
	private final ContactDao contactDao;
	private final EmailDao emailDao;
	private final EmailAccountDao emailAccountDao;
	private final EmailServerHandler emailManager;
	
	private final Timer timer;
	private final TimerTask timerTask;
	
	private static final String TAB_XML = "/ui/plugins/reminders/remindersTab.xml";
	private static final String DIALOG_XML = "/ui/plugins/reminders/remindersForm.xml";
	
	public static final String TAB_TABLE = "tableReminders";
	public static final String TAB_TABLE_PANEL = "panelTableReminders";
	public static final String TAB_PANEL = "panelReminders";
	public static final String TAB_TOOLBAR = "toolbarReminders";
	public static final String TAB_MENU = "menuReminders";
	
	public static final String TAB_MENU_CREATE = "menuCreateReminder";
	public static final String TAB_MENU_SEND = "menuSendReminder";
	public static final String TAB_MENU_EDIT = "menuEditReminder";
	public static final String TAB_MENU_DELETE = "menuDeleteReminder";
	
	public static final String TAB_TOOLBAR_CREATE = "buttonCreateReminder";
	public static final String TAB_TOOLBAR_SEND = "buttonSendReminder";
	public static final String TAB_TOOLBAR_EDIT = "buttonEditReminder";
	public static final String TAB_TOOLBAR_DELETE = "buttonDeleteReminder";
	
	public static final String DIALOG_TABLE = "tableRecipients";
	public static final String DIALOG_PANEL = "panelRecipients";
	
	public static final String DIALOG_IS_EMAIL = "checkboxEmail";
	public static final String DIALOG_IS_MESSAGE = "checkboxMessage";
	public static final String DIALOG_SUBJECT = "textSubject";
	public static final String DIALOG_MESSAGE = "textMessage";
	
	public static final String DIALOG_COMBO_OCCURRENCE = "comboOccurrence";
	
	public static final String DIALOG_COMBO_HOUR_START = "comboHourStart";
	public static final String DIALOG_COMBO_MINUTE_START = "comboMinuteStart";
	public static final String DIALOG_COMBO_AM_PM_START = "comboAmPmStart";
	public static final String DIALOG_DATE_START = "textDateStart";
	public static final String DIALOG_BUTTON_DATE_START = "buttonDateStart";
	
	public static final String DIALOG_COMBO_HOUR_END = "comboHourEnd";
	public static final String DIALOG_COMBO_MINUTE_END = "comboMinuteEnd";
	public static final String DIALOG_COMBO_AM_PM_END = "comboAmPmEnd";
	public static final String DIALOG_DATE_END = "textDateEnd";
	public static final String DIALOG_BUTTON_DATE_END = "buttonDateEnd";
	
	private ComponentPagingHandler contactListPager;
	private Object contactListComponent;
	
	private ComponentPagingHandler reminderListPager;
	private Object reminderListComponent;
	
	public RemindersThinletTabController(RemindersPluginController pluginController, UiGeneratorController uiController, ApplicationContext applicationContext) {
		super(pluginController, uiController);
		this.applicationContext = applicationContext;
		
		Object tabComponent = uiController.loadComponentFromFile(TAB_XML, this);
		super.setTabComponent(tabComponent);
		
		this.reminderDao = (ReminderDao) applicationContext.getBean("reminderDao");
		this.contactDao = this.ui.getFrontlineController().getContactDao();
		this.emailDao = this.ui.getFrontlineController().getEmailDao();
		this.emailAccountDao = this.ui.getFrontlineController().getEmailAccountFactory();
		this.emailManager = this.ui.getFrontlineController().getEmailServerHandler();
		
		this.reminderListComponent = this.ui.find(tabComponent, TAB_TABLE);
		this.reminderListPager = new ComponentPagingHandler(this.ui, this, reminderListComponent);
		
		Object panelReminders = this.ui.find(tabComponent, TAB_TABLE_PANEL);
		this.ui.add(panelReminders, this.reminderListPager.getPanel());
		
		this.reminderListPager.setCurrentPage(0);
		this.reminderListPager.refresh();
		
		int delay = 5000;   // delay for five seconds
		int period = 1000 * 60;  // repeat every minute
		this.timer = new Timer();
		this.timerTask = new ReminderTimerTask(this.reminderDao);
		this.timer.schedule(this.timerTask, delay, period);
	}
	
	private class ReminderTimerTask extends TimerTask {
		
		private final ReminderDao reminderDao;
		
		public ReminderTimerTask(ReminderDao reminderDao) {
			this.reminderDao = reminderDao;
		}
		
		public void run() {
			log("ReminderTimerTask.run");
			Calendar now = Calendar.getInstance();
			//TODO only retrieve pending reminders
			for (Reminder reminder : this.reminderDao.getAllReminders()) {
				Calendar date = Calendar.getInstance();
				date.setTimeInMillis(reminder.getStartDate());
				if (reminder.getStatus() != Reminder.Status.SENT) {
					if (reminder.getOccurrence() == Reminder.Occurrence.ONCE) {
						if (now.getTimeInMillis() >= reminder.getStartDate()) {
							log("Once Reminder Sent!");
							sendReminder(reminder);
						}
					}
					else if (reminder.getOccurrence() == Reminder.Occurrence.HOURLY) {
						if (now.get(Calendar.MINUTE) == date.get(Calendar.MINUTE) &&
							(now.get(Calendar.HOUR_OF_DAY) >= date.get(Calendar.HOUR_OF_DAY) ||
							 now.get(Calendar.DAY_OF_YEAR) >= date.get(Calendar.DAY_OF_YEAR) ||
							 now.get(Calendar.YEAR) >= date.get(Calendar.YEAR))) {
							log("Hourly Reminder Sent!");
							sendReminder(reminder);
						}
					}
					else if (reminder.getOccurrence() == Reminder.Occurrence.DAILY) {
						if (now.get(Calendar.HOUR_OF_DAY) == date.get(Calendar.HOUR_OF_DAY) &&
							now.get(Calendar.MINUTE) == date.get(Calendar.MINUTE)) {
							log("Daily Reminder Sent!");
							sendReminder(reminder);
						}
					}
					else if (reminder.getOccurrence() == Reminder.Occurrence.WEEKLY) {
						if (now.get(Calendar.HOUR_OF_DAY) == date.get(Calendar.HOUR_OF_DAY) &&
							now.get(Calendar.MINUTE) == date.get(Calendar.MINUTE) &&
							now.get(Calendar.DAY_OF_WEEK) == date.get(Calendar.DAY_OF_WEEK) &&
							(now.get(Calendar.WEEK_OF_YEAR) >= date.get(Calendar.WEEK_OF_YEAR) ||
							 now.get(Calendar.YEAR) > date.get(Calendar.YEAR))) {
							log("Weekly Reminder Sent!");
							sendReminder(reminder);
						}				
					}
					else if (reminder.getOccurrence() == Reminder.Occurrence.MONTHLY) {
						if (now.get(Calendar.HOUR_OF_DAY) == date.get(Calendar.HOUR_OF_DAY) &&
							now.get(Calendar.MINUTE) == date.get(Calendar.MINUTE) &&
							now.get(Calendar.DAY_OF_MONTH) == date.get(Calendar.DAY_OF_MONTH) &&
							(now.get(Calendar.MONTH) >= date.get(Calendar.MONTH) ||
							 now.get(Calendar.YEAR) > date.get(Calendar.YEAR))) {
							log("Monthly Reminder Sent!");
							sendReminder(reminder);
						}	
					}
				}
			}
		}
	}
	
	public void setFrontline(FrontlineSMS frontlineController) {
		this.frontlineController = frontlineController;
	}
	
	public Object getTab(){
		return super.getTabComponent();
	}

	public void removeSelectedFromReminderList() {
		log("removeSelectedFromReminderList");
		final Object[] selected = this.ui.getSelectedItems(this.reminderListComponent);
		for (Object o : selected) {
			Reminder reminder = this.ui.getAttachedObject(o, Reminder.class);
			log("Deleting Reminder:" + reminder);
			if (reminder != null) {
				this.reminderDao.deleteReminder(reminder);
			}
			else {
				throw new NullPointerException();
			}
		}
		this.ui.removeConfirmationDialog();
		this.reminderListPager.refresh();
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
		for (Object o : ui.getItems(toolbar)) {
			String name = ui.getString(o, Thinlet.NAME);
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
		log("showReminderDialog");
		Object reminderDialog = this.ui.loadComponentFromFile(DIALOG_XML, this);
		Object panelContacts = this.ui.find(reminderDialog, DIALOG_PANEL);
		
		this.contactListComponent = this.ui.find(reminderDialog, DIALOG_TABLE);
		this.contactListPager = new ComponentPagingHandler(this.ui, this, this.contactListComponent);
		
		this.ui.add(panelContacts, this.contactListPager.getPanel());
		this.ui.add(reminderDialog);
		
		this.contactListPager.setCurrentPage(0);
		this.contactListPager.refresh();
		
		Object comboOccurrence = this.ui.find(reminderDialog, DIALOG_COMBO_OCCURRENCE);
		
		Object comboHourStart = this.ui.find(reminderDialog, DIALOG_COMBO_HOUR_START);
		Object comboHourEnd = this.ui.find(reminderDialog, DIALOG_COMBO_HOUR_END);
		for (int hour = 1; hour <= 12 ; hour ++) {
			this.ui.add(comboHourStart, this.ui.createComboboxChoice(Integer.toString(hour), hour));
			this.ui.add(comboHourEnd, this.ui.createComboboxChoice(Integer.toString(hour), hour));
		}
		
		Object comboMinuteStart = this.ui.find(reminderDialog, DIALOG_COMBO_MINUTE_START);
		Object comboMinuteEnd = this.ui.find(reminderDialog, DIALOG_COMBO_MINUTE_END);
		for (int minute = 0; minute < 60; minute ++) {
			this.ui.add(comboMinuteStart, this.ui.createComboboxChoice(String.format("%02d", minute), minute));
			this.ui.add(comboMinuteEnd, this.ui.createComboboxChoice(String.format("%02d", minute), minute));
		}
		
		Object comboAmPmStart = this.ui.find(reminderDialog, DIALOG_COMBO_AM_PM_START);
		Object comboAmPmEnd = this.ui.find(reminderDialog, DIALOG_COMBO_AM_PM_END);
		for (String amPM : new String[] {"AM", "PM"}) {
			this.ui.add(comboAmPmStart, ui.createComboboxChoice(amPM, 0));
			this.ui.add(comboAmPmEnd, ui.createComboboxChoice(amPM, 0));
		}
		
		Object textDateStart = this.ui.find(reminderDialog, DIALOG_DATE_START);
		Object textDateEnd = this.ui.find(reminderDialog, DIALOG_DATE_END);
		
		if (reminderList != null) {
			final Object selected = this.ui.getSelectedItem(reminderList);
			Reminder reminder = this.ui.getAttachedObject(selected, Reminder.class);
			if (reminder != null) {
				this.ui.setAttachedObject(reminderDialog, reminder);
				this.ui.setSelectedIndex(comboOccurrence, reminder.getOccurrenceIndex());
				occurrenceChanged(reminderDialog, comboOccurrence);
				
				setDateFields(reminder.getStartCalendar(), textDateStart, comboHourStart, comboMinuteStart, comboAmPmStart);
				setDateFields(reminder.getEndCalendar(), textDateEnd, comboHourEnd, comboMinuteEnd, comboAmPmEnd);
				
				Object checkboxEmail = this.ui.find(reminderDialog, DIALOG_IS_EMAIL);
				this.ui.setSelected(checkboxEmail, reminder.getType() == Reminder.Type.EMAIL);
				
				Object checkboxMessage = this.ui.find(reminderDialog, DIALOG_IS_MESSAGE);
				this.ui.setSelected(checkboxMessage, reminder.getType() == Reminder.Type.MESSAGE);
				
				Object textSubject = this.ui.find(reminderDialog, DIALOG_SUBJECT);
				this.ui.setText(textSubject, reminder.getSubject());
				
				Object textMessage = this.ui.find(reminderDialog, DIALOG_MESSAGE);
				this.ui.setText(textMessage, reminder.getContent());
			}
			this.ui.setText(reminderDialog, InternationalisationUtils.getI18NString(RemindersConstants.EDIT_REMINDER));
		}
		else {
			Calendar now = Calendar.getInstance();
			setDateFields(now, textDateStart, comboHourStart, comboMinuteStart, comboAmPmStart);
			setDateFields(now, textDateEnd, comboHourEnd, comboMinuteEnd, comboAmPmEnd);
			this.ui.setSelectedIndex(comboOccurrence, Reminder.getIndexForOccurrence(Reminder.Occurrence.ONCE));
			this.ui.setText(reminderDialog, InternationalisationUtils.getI18NString(RemindersConstants.CREATE_REMINDER));
		}
	}
	
	public void saveReminder(Object dialogReminderForm, Object tableRecipients) {
		log("saveReminder");
		Object comboOccurrence = this.ui.find(dialogReminderForm, DIALOG_COMBO_OCCURRENCE);
		
		Object comboHourStart = this.ui.find(dialogReminderForm, DIALOG_COMBO_HOUR_START);
		Object comboHourEnd = this.ui.find(dialogReminderForm, DIALOG_COMBO_HOUR_END);
		
		Object comboMinuteStart = this.ui.find(dialogReminderForm, DIALOG_COMBO_MINUTE_START);
		Object comboMinuteEnd = this.ui.find(dialogReminderForm, DIALOG_COMBO_MINUTE_END);
		
		Object comboAmPmStart = this.ui.find(dialogReminderForm, DIALOG_COMBO_AM_PM_START);
		Object comboAmPmEnd = this.ui.find(dialogReminderForm, DIALOG_COMBO_AM_PM_END);
		
		Object textDateStart = this.ui.find(dialogReminderForm, DIALOG_DATE_START);
		Object textDateEnd = this.ui.find(dialogReminderForm, DIALOG_DATE_END);
		
		Object checkboxEmail = this.ui.find(dialogReminderForm, DIALOG_IS_EMAIL);
		
		Object textSubject = this.ui.find(dialogReminderForm, DIALOG_SUBJECT);
		Object textMessage = this.ui.find(dialogReminderForm, DIALOG_MESSAGE);
		try {
			Type type = this.ui.isSelected(checkboxEmail) ? Type.EMAIL : Type.MESSAGE;
			StringBuilder recipients = new StringBuilder();
			for (Object selected : this.ui.getSelectedItems(tableRecipients)) {
				Contact contact = this.ui.getAttachedObject(selected, Contact.class);
				if (recipients.length() > 0) {
					recipients.append(Reminder.RECIPIENT_SEPARATOR);
				}
				recipients.append(contact.getName());
			}
			int occurrence = this.ui.getSelectedIndex(comboOccurrence);
			long startDate = getLongFromDateFields(textDateStart, comboHourStart, comboMinuteStart, comboAmPmStart);
			long endDate = getLongFromDateFields(textDateEnd, comboHourEnd, comboMinuteEnd, comboAmPmEnd);
			String subject = (type == Reminder.Type.EMAIL) ? this.ui.getText(textSubject) : "";
			String message = this.ui.getText(textMessage);
			if (type == Type.EMAIL && this.emailAccountDao.getAllEmailAccounts().size() == 0) {
				this.ui.alert(InternationalisationUtils.getI18NString(RemindersConstants.MISSING_EMAIL_ACCOUNT));
			}
			else if (startDate == 0) {
				this.ui.alert(InternationalisationUtils.getI18NString(RemindersConstants.MISSING_START_DATE));
			}
			else if (occurrence > 0 && endDate == 0) {
				this.ui.alert(InternationalisationUtils.getI18NString(RemindersConstants.MISSING_END_DATE));
			}
			else if (occurrence > 0 && startDate > endDate) {
				this.ui.alert(InternationalisationUtils.getI18NString(RemindersConstants.MISSING_DATE_RANGE));
			}
			else if (recipients.length() == 0) {
				this.ui.alert(InternationalisationUtils.getI18NString(RemindersConstants.MISSING_RECIPIENT));
			}
			else if (type == Type.EMAIL && subject.isEmpty()) {
				this.ui.alert(InternationalisationUtils.getI18NString(RemindersConstants.MISSING_SUBJECT));
			}
			else if (message.isEmpty()) {
				this.ui.alert(InternationalisationUtils.getI18NString(RemindersConstants.MISSING_MESSAGE));
			}
			else {
				Reminder reminder = this.ui.getAttachedObject(dialogReminderForm, Reminder.class);
				if (reminder != null) {
					reminder.setStartDate(startDate);
					reminder.setEndDate(endDate);
					reminder.setType(type);
					reminder.setRecipients(recipients.toString());
					reminder.setSubject(subject);
					reminder.setContent(message);
					reminder.setOccurrence(Reminder.getOccurrenceForIndex(occurrence));
					this.reminderDao.updateReminder(reminder);
				}
				else {
					reminder = new Reminder(startDate,
											endDate,
											type, 
											recipients.toString(), 
											subject, 
											message, 
											Reminder.getOccurrenceForIndex(occurrence));
					this.reminderDao.saveReminder(reminder);
				}
				this.ui.remove(dialogReminderForm);
			}
		} catch (Exception ex) {
			log(ex);
			this.ui.alert(ex.getMessage());
		}
		this.reminderListPager.refresh();
	}
	
	public void sendReminder(Object list) {
		for (Object selected : this.ui.getSelectedItems(list)) {
			sendReminder((Reminder)this.ui.getAttachedObject(selected, Reminder.class));
		}
	}
	
	public void sendReminder(Reminder reminder) {
		log("sendReminder");	
		if (reminder != null) {
			if (reminder.getType() == Reminder.Type.EMAIL) {
				Collection<EmailAccount> emailAccounts = this.emailAccountDao.getAllEmailAccounts();
				if (emailAccounts.size() > 0) {
					for (String contactName : reminder.getRecipientsArray()) {
						log("sending EMAIL");
						//TODO allow user to specific a specific email account
						EmailAccount emailAccount = emailAccounts.iterator().next();
						Contact contact = this.contactDao.getContactByName(contactName);
						Email email = new Email(emailAccount, contact.getEmailAddress(), reminder.getSubject(), reminder.getContent());	
						this.emailDao.saveEmail(email);
						this.emailManager.sendEmail(email);
					}
					if (reminder.getOccurrence() == Reminder.Occurrence.ONCE ||
						Calendar.getInstance().getTimeInMillis() >= reminder.getEndDate()) {
						reminder.setStatus(Reminder.Status.SENT);
					}
				}
				else {
					this.ui.alert(InternationalisationUtils.getI18NString(RemindersConstants.MISSING_EMAIL_ACCOUNT));
				}
			}
			else if (reminder.getType() == Reminder.Type.MESSAGE) {
				for (String contactName : reminder.getRecipientsArray()) {
					Contact contact = this.contactDao.getContactByName(contactName);
					if (contact != null) {
						log("sending MESSAGE");
						this.frontlineController.sendTextMessage(contact.getPhoneNumber(), reminder.getContent());
					}
				}
				if (reminder.getOccurrence() == Reminder.Occurrence.ONCE ||
					Calendar.getInstance().getTimeInMillis() >= reminder.getEndDate()) {
					reminder.setStatus(Reminder.Status.SENT);
				}
			}
			this.reminderDao.updateReminder(reminder);
		}
		this.reminderListPager.refresh();
	}
	
	public void typeChanged(Object checkbox, Object textSubject, Object textMessage) {
		log("typeChanged");
		if (DIALOG_IS_EMAIL.equalsIgnoreCase(ui.getName(checkbox))) {
			this.ui.setEnabled(textSubject, true);
		}
		else if (DIALOG_IS_MESSAGE.equalsIgnoreCase(ui.getName(checkbox))) {
			this.ui.setEnabled(textSubject, false);
			this.ui.setText(textSubject, "");
		}
	}
	
	public void occurrenceChanged(Object dialogReminderForm, Object occurrence) {
		log("occurrenceChanged");
		Object comboHourEnd = this.ui.find(dialogReminderForm, DIALOG_COMBO_HOUR_END);
		Object comboMinuteEnd = this.ui.find(dialogReminderForm, DIALOG_COMBO_MINUTE_END);
		Object comboAmPmEnd = this.ui.find(dialogReminderForm, DIALOG_COMBO_AM_PM_END);
		Object textDateEnd = this.ui.find(dialogReminderForm, DIALOG_DATE_END);
		Object buttonEnd = this.ui.find(dialogReminderForm, DIALOG_BUTTON_DATE_END);
		if (this.ui.getSelectedIndex(occurrence) == 0) {
			this.ui.setEnabled(comboHourEnd, false);
			this.ui.setEnabled(comboMinuteEnd, false);
			this.ui.setEnabled(comboAmPmEnd, false);
			this.ui.setEnabled(buttonEnd, false);
			this.ui.setSelectedIndex(comboHourEnd, -1);
			this.ui.setSelectedIndex(comboMinuteEnd, -1);
			this.ui.setSelectedIndex(comboAmPmEnd, -1);
			this.ui.setEnabled(textDateEnd, false);
			this.ui.setEnabled(buttonEnd, false);
		}
		else {
			this.ui.setEnabled(comboHourEnd, true);
			this.ui.setEnabled(comboMinuteEnd, true);
			this.ui.setEnabled(comboAmPmEnd, true);
			this.ui.setEnabled(textDateEnd, true);
			this.ui.setEnabled(buttonEnd, true);
		}
	}
	
	public void showDateSelecter(Object textField) {
		log("showDateSelecter");
		this.ui.showDateSelecter(textField);
	}
	
	public PagedListDetails getListDetails(Object list, int startIndex, int limit) {
		log("getListDetails:" + ui.getName(list));
		if (DIALOG_TABLE.equalsIgnoreCase(ui.getName(list))) {
			final Object selected = this.ui.getSelectedItem(this.reminderListComponent);
			Reminder reminder = this.ui.getAttachedObject(selected, Reminder.class);
			List<Contact> contacts = this.contactDao.getAllContacts(startIndex, limit);
			Object[] listItems = toThinletContacts(contacts, reminder);
			return new PagedListDetails(listItems.length, listItems);
		}
		else if (TAB_TABLE.equalsIgnoreCase(ui.getName(list))) {
			List<Reminder> reminders = this.reminderDao.getAllReminders(startIndex, limit);
			Object[] listItems = toThinletReminders(reminders);
			return new PagedListDetails(listItems.length, listItems);
		}
		return null;
	}
	
	private Object[] toThinletContacts(List<Contact> contacts, Reminder reminder) {
		Object[] components = new Object[contacts.size()];
		for (int i = 0; i < components.length; i++) {
			Contact c = contacts.get(i);
			components[i] = getContactRow(c, reminder);
		}
		return components;
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
		Object row = ui.createTableRow(reminder);
		
		if (reminder.getStatus() == Reminder.Status.SENT) {
			Object statusCell = ui.createTableCell("");
			ui.setIcon(statusCell, Icon.TICK);
			ui.setChoice(statusCell, ThinletText.ALIGNMENT, ThinletText.CENTER);
			ui.add(row, statusCell);
		}
		else {
			ui.add(row, ui.createTableCell(""));
		}
		
		Object typeCell = ui.createTableCell("");
		if (reminder.getType() == Reminder.Type.EMAIL) {
			ui.setIcon(typeCell, Icon.EMAIL);
		}
		else {
			ui.setIcon(typeCell, Icon.SMS);
		}
		ui.setChoice(typeCell, ThinletText.ALIGNMENT, ThinletText.CENTER);
		ui.add(row, typeCell);
		
		Object occuranceCell = ui.createTableCell(reminder.getOccurrenceLabel());
		ui.setChoice(occuranceCell, ThinletText.ALIGNMENT, ThinletText.CENTER);
		ui.add(row, occuranceCell);
		
		DateFormat dateFormat = InternationalisationUtils.getDatetimeFormat();
		
		if (reminder.getStartDate() > 0) {
			Date startdate = new Date(reminder.getStartDate());
			ui.add(row, ui.createTableCell(dateFormat.format(startdate)));
		}
		else {
			ui.add(row, ui.createTableCell(""));
		}
		
		if (reminder.getEndDate() > 0) {
			Date enddate = new Date(reminder.getEndDate());
			ui.add(row, ui.createTableCell(dateFormat.format(enddate)));
		}
		else {
			ui.add(row, ui.createTableCell(""));
		}
		
		ui.add(row, ui.createTableCell(reminder.getRecipients()));
		ui.add(row, ui.createTableCell(reminder.getSubject()));
		ui.add(row, ui.createTableCell(reminder.getContent()));
        
		return row;
	}
	
	private Object getContactRow(Contact contact, Reminder reminder) {
		Object row = ui.createTableRow(contact);

		ui.add(row, ui.createTableCell(contact.getDisplayName()));
		ui.add(row, ui.createTableCell(contact.getPhoneNumber()));
		ui.add(row, ui.createTableCell(contact.getEmailAddress()));
		
		if (reminder != null) {
			for (String contactName : reminder.getRecipientsArray()) {
				if (contact.getName().equals(contactName)) {
					ui.setSelected(row, true);
				}
			}
		}
		
        return row;
	}
	
	private void setDateFields(Calendar calendar, Object textDate, Object comboHour, Object comboMinute, Object comboAmPm) {
		this.ui.setText(textDate, getDateStringFromCalendar(calendar));
		if (calendar.get(Calendar.HOUR_OF_DAY) <= 12) {
			this.ui.setSelectedIndex(comboHour, calendar.get(Calendar.HOUR_OF_DAY) - 1);
		}
		else {
			this.ui.setSelectedIndex(comboHour, calendar.get(Calendar.HOUR_OF_DAY) - 13);
		}
		this.ui.setSelectedIndex(comboMinute, calendar.get(Calendar.MINUTE));
		this.ui.setSelectedIndex(comboAmPm, calendar.get(Calendar.AM_PM));
	}
	
	private long getLongFromDateFields(Object textDate, Object comboHour, Object comboMinute, Object comboAmPm) {
		try {
			String date = this.ui.getText(textDate);
			int hour = this.ui.getSelectedIndex(comboHour) + 1;
			int minute = this.ui.getSelectedIndex(comboMinute);
			int amPm = this.ui.getSelectedIndex(comboAmPm);
			DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
			Date dateTime;
			dateTime = dateFormat.parse(date);
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(dateTime);
			if (amPm == 0) {
				calendar.set(Calendar.HOUR_OF_DAY, hour);
			}
			else {
				calendar.set(Calendar.HOUR_OF_DAY, hour + 12);
			}
			calendar.set(Calendar.MINUTE, minute);
			return calendar.getTimeInMillis();
		} catch (ParseException e) {
			return 0;
		}
	}
	
	private String getDateStringFromCalendar(Calendar calendar) {
		return String.format("%02d/%02d/%d", calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR));
	}
	
	private void log(final String message) {
		System.out.println(message);
	}
	
	private void log(final Exception ex) {
		System.out.println(ex);
	}
	
}
