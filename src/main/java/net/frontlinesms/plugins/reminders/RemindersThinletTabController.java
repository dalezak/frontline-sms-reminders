package net.frontlinesms.plugins.reminders;

import java.util.Collection;
import java.util.Date;
import java.util.Calendar;
import java.util.List;
import java.util.StringTokenizer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;

import net.frontlinesms.EmailServerHandler;
import net.frontlinesms.FrontlineSMS;
import net.frontlinesms.FrontlineSMSConstants;
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
import net.frontlinesms.data.domain.Message;

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
	
	private static final String TAB_XML_FILE = "/ui/plugins/reminders/remindersTab.xml";
	private static final String DIALOG_XML_FILE = "/ui/plugins/reminders/remindersForm.xml";
	
	public static final String TAB_REMINDERS_TABLE = "tableReminders";
	public static final String TAB_REMINDERS_TABLE_PANEL = "panelTableReminders";
	public static final String TAB_REMINDERS_PANEL = "panelReminders";
	public static final String TAB_REMINDERS_TOOLBAR = "toolbarReminders";
	public static final String TAB_REMINDERS_MENU = "menuReminders";
	
	public static final String TAB_REMINDERS_MENU_CREATE = "menuCreateReminder";
	public static final String TAB_REMINDERS_MENU_SEND = "menuSendReminder";
	public static final String TAB_REMINDERS_MENU_EDIT = "menuEditReminder";
	public static final String TAB_REMINDERS_MENU_DELETE = "menuDeleteReminder";
	
	public static final String TAB_REMINDERS_TOOLBAR_CREATE = "buttonCreateReminder";
	public static final String TAB_REMINDERS_TOOLBAR_SEND = "buttonSendReminder";
	public static final String TAB_REMINDERS_TOOLBAR_EDIT = "buttonEditReminder";
	public static final String TAB_REMINDERS_TOOLBAR_DELETE = "buttonDeleteReminder";
	
	public static final String DIALOG_CONTACTS_TABLE = "tableRecipients";
	public static final String DIALOG_CONTACTS_PANEL = "panelRecipients";
	
	public static final String DIALOG_CONTACTS_DATE = "textDate";
	public static final String DIALOG_CONTACTS_IS_EMAIL = "checkboxEmail";
	public static final String DIALOG_CONTACTS_IS_MESSAGE = "checkboxMessage";
	public static final String DIALOG_CONTACTS_SUBJECT = "textSubject";
	public static final String DIALOG_CONTACTS_MESSAGE = "textMessage";
	
	public static final String DIALOG_INFO_LABEL = "labelInfo";
	public static final String DIALOG_COMBO_HOUR = "comboHour";
	public static final String DIALOG_COMBO_MINUTE = "comboMinute";
	public static final String DIALOG_COMBO_AM_PM = "comboAmPm";
	
	public static final String CREATE_REMINDER = "Create Reminder";
	public static final String EDIT_REMINDER = "Edit Reminder";
	
	private ComponentPagingHandler contactListPager;
	private Object contactListComponent;
	
	private ComponentPagingHandler reminderListPager;
	private Object reminderListComponent;
	
	public RemindersThinletTabController(RemindersPluginController pluginController, UiGeneratorController uiController, ApplicationContext applicationContext) {
		super(pluginController, uiController);
		this.applicationContext = applicationContext;
		
		Object tabComponent = uiController.loadComponentFromFile(TAB_XML_FILE, this);
		super.setTabComponent(tabComponent);
		
		this.reminderDao = (ReminderDao) applicationContext.getBean("reminderDao");
		this.contactDao = this.ui.getFrontlineController().getContactDao();
		this.emailDao = this.ui.getFrontlineController().getEmailDao();
		this.emailAccountDao = this.ui.getFrontlineController().getEmailAccountFactory();
		this.emailManager = this.ui.getFrontlineController().getEmailServerHandler();
		
		this.reminderListComponent = this.ui.find(tabComponent, TAB_REMINDERS_TABLE);
		this.reminderListPager = new ComponentPagingHandler(this.ui, this, reminderListComponent);
		
		Object panelReminders = this.ui.find(tabComponent, TAB_REMINDERS_TABLE_PANEL);
		this.ui.add(panelReminders, this.reminderListPager.getPanel());
		
		this.reminderListPager.setCurrentPage(0);
		this.reminderListPager.refresh();
		
		int delay = 5000;   // delay for 5 seconds
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
			Calendar calendar = Calendar.getInstance();
			//TODO only retrieve pending reminders
			for (Reminder reminder : this.reminderDao.getAllReminders()) {
				if (reminder.getStatus() != Reminder.Status.SENT) {
					if (calendar.getTimeInMillis() >= reminder.getDate()) {
						log("Reminder Sent!");
						sendReminder(reminder);
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
	
	public void showReminderDialog() {
		showReminderDialog(null);
	}
	
	public void showReminderDialog(Object reminderList) {
		log("showReminderDialog");
		Object reminderDialog = this.ui.loadComponentFromFile(DIALOG_XML_FILE, this);
		Object panelContacts = this.ui.find(reminderDialog, DIALOG_CONTACTS_PANEL);
		
		this.contactListComponent = this.ui.find(reminderDialog, DIALOG_CONTACTS_TABLE);
		this.contactListPager = new ComponentPagingHandler(this.ui, this, this.contactListComponent);
		
		this.ui.add(panelContacts, this.contactListPager.getPanel());
		this.ui.add(reminderDialog);
		
		this.contactListPager.setCurrentPage(0);
		this.contactListPager.refresh();
		
		Object comboHour = this.ui.find(reminderDialog, DIALOG_COMBO_HOUR);
		for (int hour = 1; hour <= 12 ; hour ++) {
			this.ui.add(comboHour, this.ui.createComboboxChoice(Integer.toString(hour), hour));
		}
		Object comboMinute = this.ui.find(reminderDialog, DIALOG_COMBO_MINUTE);
		for (int minute = 0; minute < 60; minute ++) {
			this.ui.add(comboMinute, this.ui.createComboboxChoice(String.format("%02d", minute), minute));
		}
		Object comboAmPm = this.ui.find(reminderDialog, DIALOG_COMBO_AM_PM);
		this.ui.add(comboAmPm, ui.createComboboxChoice("AM", 0));
		this.ui.add(comboAmPm, ui.createComboboxChoice("PM", 1));
		
		Object textDate = this.ui.find(reminderDialog, DIALOG_CONTACTS_DATE);
		if (reminderList != null) {
			final Object selected = this.ui.getSelectedItem(reminderList);
			Reminder reminder = this.ui.getAttachedObject(selected, Reminder.class);
			if (reminder != null) {
				this.ui.setAttachedObject(reminderDialog, reminder);
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(reminder.getDate());
				
				this.ui.setText(textDate, String.format("%02d/%02d/%d", calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR)));   
				if (calendar.get(Calendar.HOUR_OF_DAY) <= 12) {
					this.ui.setSelectedIndex(comboHour, calendar.get(Calendar.HOUR_OF_DAY) - 1);
				}
				else {
					this.ui.setSelectedIndex(comboHour, calendar.get(Calendar.HOUR_OF_DAY) - 13);
				}
				this.ui.setSelectedIndex(comboMinute, calendar.get(Calendar.MINUTE));
				this.ui.setSelectedIndex(comboAmPm, calendar.get(Calendar.AM_PM));
				
				Object checkboxEmail = this.ui.find(reminderDialog, DIALOG_CONTACTS_IS_EMAIL);
				this.ui.setSelected(checkboxEmail, reminder.getType() == Reminder.Type.EMAIL);
				
				Object checkboxMessage = this.ui.find(reminderDialog, DIALOG_CONTACTS_IS_MESSAGE);
				this.ui.setSelected(checkboxMessage, reminder.getType() == Reminder.Type.MESSAGE);
				
				Object textSubject = this.ui.find(reminderDialog, DIALOG_CONTACTS_SUBJECT);
				this.ui.setText(textSubject, reminder.getSubject());
				
				Object textMessage = this.ui.find(reminderDialog, DIALOG_CONTACTS_MESSAGE);
				this.ui.setText(textMessage, reminder.getContent());
			}
			this.ui.setText(reminderDialog, EDIT_REMINDER);
		}
		else {
			this.ui.setText(reminderDialog, CREATE_REMINDER);
			Calendar calendar = Calendar.getInstance();
			this.ui.setText(textDate, String.format("%02d/%02d/%d", calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR)));
			if (calendar.get(Calendar.HOUR_OF_DAY) <= 12) {
				this.ui.setSelectedIndex(comboHour, calendar.get(Calendar.HOUR_OF_DAY) - 1);
			}
			else {
				this.ui.setSelectedIndex(comboHour, calendar.get(Calendar.HOUR_OF_DAY) - 13);
			}
			this.ui.setSelectedIndex(comboMinute, calendar.get(Calendar.MINUTE));
			this.ui.setSelectedIndex(comboAmPm, calendar.get(Calendar.AM_PM));
		}
	}
	
	public void enableOptions(Object list, Object popup, Object toolbar) {
		Object[] selectedItems = this.ui.getSelectedItems(list);
		boolean hasSelection = selectedItems.length > 0;
		for (Object o : this.ui.getItems(popup)) {
			String name = this.ui.getString(o, Thinlet.NAME);
			if (name == null) { 
				continue;
			}
			else if (name.equals(TAB_REMINDERS_MENU_CREATE)) {
				this.ui.setEnabled(o, true);
			}
			else if (name.equals(TAB_REMINDERS_MENU_EDIT)) {
				this.ui.setEnabled(o, hasSelection);
			}
			else if (name.equals(TAB_REMINDERS_MENU_DELETE)) {
				this.ui.setEnabled(o, hasSelection);
			}
			else if (name.equals(TAB_REMINDERS_MENU_SEND)) {
				this.ui.setEnabled(o, hasSelection);
			}
		}
		for (Object o : ui.getItems(toolbar)) {
			String name = ui.getString(o, Thinlet.NAME);
			if (name == null) { 
				continue;
			}
			else if (name.equals(TAB_REMINDERS_TOOLBAR_CREATE)) {
				this.ui.setEnabled(o, true);
			}
			else if (name.equals(TAB_REMINDERS_TOOLBAR_EDIT)) {
				this.ui.setEnabled(o, hasSelection);
			}
			else if (name.equals(TAB_REMINDERS_TOOLBAR_DELETE)) {
				this.ui.setEnabled(o, hasSelection);
			}
			else if (name.equals(TAB_REMINDERS_TOOLBAR_SEND)) {
				this.ui.setEnabled(o, hasSelection);
			}
		}
	}
	
	public void createReminder(Object dialogReminderForm, Object tableRecipients, Object textDate, Object comboHour, Object comboMinute, Object comboAmPm, Object checkboxEmail, Object textSubject, Object textMessage) {
		log("createReminder");
		Object labelInfo = this.ui.find(dialogReminderForm, DIALOG_INFO_LABEL);
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
			String date = this.ui.getText(textDate);
			int hour = this.ui.getSelectedIndex(comboHour) + 1;
			int minute = this.ui.getSelectedIndex(comboMinute);
			int am_pm = this.ui.getSelectedIndex(comboAmPm);
			String subject = this.ui.getText(textSubject);
			String message = this.ui.getText(textMessage);
			if (date.isEmpty()) {
				this.ui.setText(labelInfo, "Missing Field: Date is required");
			}
			else if (recipients.length() == 0) {
				this.ui.setText(labelInfo, "Missing Field: At least one recipient is required");
			}
			else if (type == Type.EMAIL && subject.isEmpty()) {
				this.ui.setText(labelInfo, "Missing Field: Subject is required");
			}
			else if (message.isEmpty()) {
				this.ui.setText(labelInfo, "Missing Field: Message is required");
			}
			else {
				log("Type: " + type.toString());
				log("Date: " + date);
				log("Hour: " + hour);
				log("Minute: " + minute);
				log("AM/PM: " + am_pm);
				log("Recipients: " + recipients.toString());
				log("Subject: " + subject);
				log("Message: " + message);
				DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
				Date dateTime = dateFormat.parse(date);
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(dateTime);
				if (am_pm == 0) {
					calendar.set(Calendar.HOUR_OF_DAY, hour);
				}
				else {
					calendar.set(Calendar.HOUR_OF_DAY, hour + 12);
				}
				calendar.set(Calendar.MINUTE, minute);
				Reminder reminder = this.ui.getAttachedObject(dialogReminderForm, Reminder.class);
				if (reminder != null) {
					reminder.setDate(calendar.getTimeInMillis());
					reminder.setType(type);
					reminder.setRecipients(recipients.toString());
					reminder.setSubject(subject);
					reminder.setContent(message);
					this.reminderDao.updateReminder(reminder);
				}
				else {
					reminder = new Reminder(calendar.getTimeInMillis(), type, recipients.toString(), subject, message);
					this.reminderDao.saveReminder(reminder);
				}
				this.ui.remove(dialogReminderForm);
			}
		} catch (Exception ex) {
			log(ex);
			this.ui.setText(labelInfo, "Error: " + ex.getMessage());
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
					reminder.setStatus(Reminder.Status.SENT);
				}
				else {
					log("Unable to send email, no EmailAccount specified");
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
				reminder.setStatus(Reminder.Status.SENT);
			}
			this.reminderDao.updateReminder(reminder);
		}
		this.reminderListPager.refresh();
	}
	
	public void typeChanged(Object checkbox, Object textSubject, Object textMessage) {
		log("typeChanged");
		if (DIALOG_CONTACTS_IS_EMAIL.equalsIgnoreCase(ui.getName(checkbox))) {
			this.ui.setEnabled(textSubject, true);
		}
		else if (DIALOG_CONTACTS_IS_MESSAGE.equalsIgnoreCase(ui.getName(checkbox))) {
			this.ui.setEnabled(textSubject, false);
		}
	}
	
	public void showDateSelecter(Object textField) {
		log("showDateSelecter");
		this.ui.showDateSelecter(textField);
	}
	
	public PagedListDetails getListDetails(Object list, int startIndex, int limit) {
		log("getListDetails:" + ui.getName(list));
		if (DIALOG_CONTACTS_TABLE.equalsIgnoreCase(ui.getName(list))) {
			final Object selected = this.ui.getSelectedItem(this.reminderListComponent);
			Reminder reminder = this.ui.getAttachedObject(selected, Reminder.class);
			List<Contact> contacts = this.contactDao.getAllContacts(startIndex, limit);
			Object[] listItems = toThinletContacts(contacts, reminder);
			return new PagedListDetails(listItems.length, listItems);
		}
		else if (TAB_REMINDERS_TABLE.equalsIgnoreCase(ui.getName(list))) {
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
		
		DateFormat dateFormat = InternationalisationUtils.getDatetimeFormat();
		Date date = new Date(reminder.getDate());
		ui.add(row, ui.createTableCell(dateFormat.format(date)));
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
	
	private void log(final String message) {
		System.out.println(message);
	}
	
	private void log(final Exception ex) {
		System.out.println(ex);
	}
}
