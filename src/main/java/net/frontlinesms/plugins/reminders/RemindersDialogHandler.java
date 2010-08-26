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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import net.frontlinesms.FrontlineSMS;
import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.data.domain.Contact;
import net.frontlinesms.data.events.DatabaseEntityNotification;
import net.frontlinesms.data.repository.ContactDao;
import net.frontlinesms.data.repository.EmailAccountDao;
import net.frontlinesms.events.EventObserver;
import net.frontlinesms.events.FrontlineEventNotification;
import net.frontlinesms.plugins.reminders.data.domain.OnceReminder;
import net.frontlinesms.plugins.reminders.data.domain.Reminder;
import net.frontlinesms.plugins.reminders.data.domain.Reminder.Type;
import net.frontlinesms.plugins.reminders.data.repository.ReminderDao;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.handler.ComponentPagingHandler;
import net.frontlinesms.ui.handler.PagedComponentItemProvider;
import net.frontlinesms.ui.handler.PagedListDetails;
import net.frontlinesms.ui.i18n.InternationalisationUtils;

/*
 * RemindersDialogHandler
 * @author Dale Zak
 * 
 * see {@link "http://www.frontlinesms.net"} for more details. 
 * copyright owned by Kiwanja.net
 */
public class RemindersDialogHandler implements ThinletUiEventHandler, PagedComponentItemProvider, EventObserver {
	
	private static Logger LOG = FrontlineUtils.getLogger(RemindersDialogHandler.class);
	
	private static final String DIALOG_XML = "/ui/plugins/reminders/remindersForm.xml";
	
	/**
	 * FrontlineSMS
	 */
	private FrontlineSMS frontlineController;
	
	/**
	 * UiGeneratorController
	 */
	private UiGeneratorController ui;
	
	/**
	 * ApplicationContext
	 */
	private ApplicationContext applicationContext;
	
	private final ReminderDao reminderDao;
	private final ContactDao contactDao;
	private final EmailAccountDao emailAccountDao;
	
	private Object dialogReminders;
	private ComponentPagingHandler pagerRecipients;
	private Object tableRecipients;
	private Reminder selectedReminder;
	private RemindersCallback remindersCallback;
	
	private Object panelRecipients;
	private Object comboOccurrence;
	
	private Object comboHourStart;
	private Object comboHourEnd;
	
	private Object comboMinuteStart;
	private Object comboMinuteEnd;
	
	private Object comboAmPmStart;
	private Object comboAmPmEnd;
	
	private Object textDateStart;
	private Object textDateEnd;
	
	private Object checkboxEmail;
	private Object checkboxMessage;
	
	private Object buttonDateEnd;
	
	private Object textMessage;
	private Object textSubject;
	
	/**
	 * RemindersDialogHandler
	 * @param ui UiGeneratorController
	 * @param applicationContext ApplicationContext
	 * @param callback RemindersCallback
	 */
	public RemindersDialogHandler(UiGeneratorController ui, ApplicationContext applicationContext, RemindersCallback callback) {
		this.ui = ui;
		this.applicationContext = applicationContext;
		this.remindersCallback = callback;
		this.reminderDao = (ReminderDao) this.applicationContext.getBean("reminderDao");
		this.contactDao = this.ui.getFrontlineController().getContactDao();
		this.emailAccountDao = this.ui.getFrontlineController().getEmailAccountFactory();
	}
	
	/**
	 * Initialize dialog
	 * @param reminder Reminder
	 */
	public void init(Reminder reminder) {
		this.selectedReminder = reminder;
		this.dialogReminders = this.ui.loadComponentFromFile(DIALOG_XML, this);
		this.panelRecipients = this.ui.find(this.dialogReminders, "panelRecipients");
		
		this.tableRecipients = this.ui.find(this.dialogReminders, "tableRecipients");
		this.pagerRecipients = new ComponentPagingHandler(this.ui, this, this.tableRecipients);
		
		this.ui.add(this.panelRecipients, this.pagerRecipients.getPanel());
		this.ui.add(this.dialogReminders);
		
		this.pagerRecipients.setCurrentPage(0);
		this.pagerRecipients.refresh();
		
		this.comboOccurrence = this.ui.find(this.dialogReminders, "comboOccurrence");
		for (Reminder reminderClass : RemindersFactory.getReminderClasses()) {
			Object comboBoxChoice = this.ui.createComboboxChoice(reminderClass.getOccurrenceLabel(), reminderClass.getOccurrence());
			this.ui.add(this.comboOccurrence, comboBoxChoice);
		}
		
		this.comboHourStart = this.ui.find(this.dialogReminders, "comboHourStart");
		this.comboHourEnd = this.ui.find(this.dialogReminders, "comboHourEnd");
		for (int hour = 1; hour <= 12 ; hour ++) {
			this.ui.add(this.comboHourStart, this.ui.createComboboxChoice(Integer.toString(hour), hour));
			this.ui.add(this.comboHourEnd, this.ui.createComboboxChoice(Integer.toString(hour), hour));
		}
		
		this.comboMinuteStart = this.ui.find(this.dialogReminders, "comboMinuteStart");
		this.comboMinuteEnd = this.ui.find(this.dialogReminders, "comboMinuteEnd");
		for (int minute = 0; minute < 60; minute ++) {
			this.ui.add(this.comboMinuteStart, this.ui.createComboboxChoice(String.format("%02d", minute), minute));
			this.ui.add(this.comboMinuteEnd, this.ui.createComboboxChoice(String.format("%02d", minute), minute));
		}
		
		this.comboAmPmStart = this.ui.find(this.dialogReminders, "comboAmPmStart");
		this.comboAmPmEnd = this.ui.find(this.dialogReminders, "comboAmPmEnd");
		for (String amPm : new String [] {"AM", "PM"}) {
			this.ui.add(this.comboAmPmStart, this.ui.createComboboxChoice(amPm, amPm));
			this.ui.add(this.comboAmPmEnd, this.ui.createComboboxChoice(amPm, amPm));
		}
		
		this.textDateStart = this.ui.find(this.dialogReminders, "textDateStart");
		this.textDateEnd = this.ui.find(this.dialogReminders, "textDateEnd");
		this.buttonDateEnd = this.ui.find(this.dialogReminders, "buttonDateEnd");
		
		this.checkboxEmail = this.ui.find(this.dialogReminders, "checkboxEmail");
		this.checkboxMessage = this.ui.find(this.dialogReminders, "checkboxMessage");
		
		this.textSubject = this.ui.find(this.dialogReminders, "textSubject");
		this.textMessage = this.ui.find(this.dialogReminders, "textMessage");
		
		if (reminder != null) {
			this.ui.setAttachedObject(this.dialogReminders, reminder);
			this.ui.setEnabled(this.comboOccurrence, false);
			for (int index = 0; index < this.ui.getCount(this.comboOccurrence); index++) {
				Object comboOccurrenceItem = this.ui.getItem(this.comboOccurrence, index);
				String comboOccurrenceItemOccurrence = this.ui.getAttachedObject(comboOccurrenceItem).toString();
				if (reminder.getOccurrence().equalsIgnoreCase(comboOccurrenceItemOccurrence)) {
					this.ui.setSelectedIndex(this.comboOccurrence, index);
					break;
				}				
			}
			occurrenceChanged(this.dialogReminders, this.comboOccurrence);
			setDateFields(reminder.getStartCalendar(), this.textDateStart, this.comboHourStart, this.comboMinuteStart, this.comboAmPmStart);
			setDateFields(reminder.getEndCalendar(), this.textDateEnd, this.comboHourEnd, this.comboMinuteEnd, this.comboAmPmEnd);
			this.ui.setSelected(this.checkboxEmail, reminder.getType() == Reminder.Type.EMAIL);
			this.ui.setSelected(this.checkboxMessage, reminder.getType() == Reminder.Type.MESSAGE);
			this.ui.setText(this.textSubject, reminder.getSubject());
			this.ui.setText(this.textMessage, reminder.getContent());
			this.ui.setText(this.dialogReminders, InternationalisationUtils.getI18NString(RemindersConstants.EDIT_REMINDER));
			this.ui.setIcon(this.dialogReminders, "/icons/reminders_edit.png");
		}
		else {
			Calendar now = Calendar.getInstance();
			setDateFields(now, this.textDateStart, this.comboHourStart, this.comboMinuteStart, this.comboAmPmStart);
			setDateFields(now, this.textDateEnd, this.comboHourEnd, this.comboMinuteEnd, this.comboAmPmEnd);
			this.ui.setSelected(this.checkboxEmail, true);
			this.ui.setSelected(this.checkboxMessage, false);
			this.ui.setText(this.textSubject, "");
			this.ui.setText(this.textMessage, "");
			this.ui.setEnabled(this.comboOccurrence, true);
			for (int index = 0; index < this.ui.getCount(this.comboOccurrence); index++) {
				Object comboOccurrenceItem = this.ui.getItem(this.comboOccurrence, index);
				if (OnceReminder.isSatisfiedBy(this.ui.getAttachedObject(comboOccurrenceItem).toString())) {
					this.ui.setSelectedIndex(this.comboOccurrence, index);
					break;
				}
			}
			this.ui.setText(this.dialogReminders, InternationalisationUtils.getI18NString(RemindersConstants.CREATE_REMINDER));
			this.ui.setIcon(this.dialogReminders, "/icons/reminders_add.png");
		}
	}
	
	/**
	 * Set Front
	 * @param frontlineController FrontlineSMS
	 */
	public void setFrontline(FrontlineSMS frontlineController) {
		this.frontlineController = frontlineController;
		this.frontlineController.getEventBus().registerObserver(this);
	}
	
	/**
	 * Handle Frontline notification
	 * @param notification
	 */
	public void notify(FrontlineEventNotification notification) {
		if (notification instanceof DatabaseEntityNotification<?>) {
			Object entity = ((DatabaseEntityNotification<?>) notification).getDatabaseEntity();
			if (entity instanceof Contact) {
				this.pagerRecipients.setCurrentPage(0);
				this.pagerRecipients.refresh();
			}
		}
	}
	
	/**
	 * Save reminder
	 * @param dialog
	 * @param table
	 */
	public void saveReminder(Object dialog, Object table) {
		LOG.debug("saveReminder");
		try {
			Type type = this.ui.isSelected(this.checkboxEmail) ? Type.EMAIL : Type.MESSAGE;
			StringBuilder recipients = new StringBuilder();
			for (Object selected : this.ui.getSelectedItems(table)) {
				Contact contact = this.ui.getAttachedObject(selected, Contact.class);
				if (recipients.length() > 0) {
					recipients.append(Reminder.RECIPIENT_SEPARATOR);
				}
				recipients.append(contact.getName());
			}
			Object occurrenceItem = this.ui.getSelectedItem(this.comboOccurrence);
			String occurrence = this.ui.getAttachedObject(occurrenceItem).toString();
			
			long startDate = getLongFromDateFields(this.textDateStart, this.comboHourStart, this.comboMinuteStart, this.comboAmPmStart);
			long endDate = getLongFromDateFields(this.textDateEnd, this.comboHourEnd, this.comboMinuteEnd, this.comboAmPmEnd);
			if (OnceReminder.isSatisfiedBy(occurrence)) {
				endDate = startDate;
			}
			String subject = (type == Reminder.Type.EMAIL) ? this.ui.getText(this.textSubject) : "";
			String message = this.ui.getText(this.textMessage);
			if (type == Type.EMAIL && this.emailAccountDao.getAllEmailAccounts().size() == 0) {
				this.ui.alert(InternationalisationUtils.getI18NString(RemindersConstants.MISSING_EMAIL_ACCOUNT));
			}
			else if (startDate == 0) {
				this.ui.alert(InternationalisationUtils.getI18NString(RemindersConstants.MISSING_START_DATE));
			}
			else if (OnceReminder.isSatisfiedBy(occurrence) == false && endDate == 0) {
				this.ui.alert(InternationalisationUtils.getI18NString(RemindersConstants.MISSING_END_DATE));
			}
			else if (OnceReminder.isSatisfiedBy(occurrence) == false && startDate > endDate) {
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
				Reminder reminder = this.ui.getAttachedObject(dialog, Reminder.class);
				if (reminder != null) {
					reminder.stopReminder();
				}
				if (reminder != null) {
					reminder.setStartDate(startDate);
					reminder.setEndDate(endDate);
					reminder.setType(type);
					reminder.setRecipients(recipients.toString());
					reminder.setSubject(subject);
					reminder.setContent(message);
					reminder.setStatus(Reminder.Status.PENDING);
					this.reminderDao.updateReminder(reminder);
					if (type == Reminder.Type.EMAIL) {
						this.ui.setStatus(InternationalisationUtils.getI18NString(RemindersConstants.EMAIL_REMINDER_UPDATED));
					}
					else {
						this.ui.setStatus(InternationalisationUtils.getI18NString(RemindersConstants.SMS_REMINDER_UPDATED));
					}	
				}
				else {
					reminder = RemindersFactory.createReminder(startDate, endDate, type, recipients.toString(), subject, message, occurrence);
					reminder.setStatus(Reminder.Status.PENDING);
					this.reminderDao.saveReminder(reminder);
					if (type == Reminder.Type.EMAIL) {
						this.ui.setStatus(InternationalisationUtils.getI18NString(RemindersConstants.EMAIL_REMINDER_CREATED));
					}
					else {
						this.ui.setStatus(InternationalisationUtils.getI18NString(RemindersConstants.SMS_REMINDER_CREATED));
					}
				}
				reminder.scheduleReminder();
				this.ui.remove(dialog);
			}
		} 
		catch (Exception ex) {
			LOG.debug(ex);
			this.ui.alert(ex.getMessage());
		}
		if (remindersCallback != null) {
			remindersCallback.refreshReminders(null);
		}
	}
	
	/**
	 * Remove dialog
	 * @param dialog
	 */
	public void removeDialog(Object dialog) {
		this.ui.removeDialog(dialog);
	}
	
	/**
	 * Reminder type changed
	 * @param checkbox
	 * @param textSubject
	 * @param textMessage
	 */
	public void typeChanged(Object checkbox, Object textSubject, Object textMessage) {
		LOG.debug("typeChanged");
		if (checkbox == this.checkboxEmail) {
			this.ui.setEnabled(textSubject, true);
			this.ui.setEditable(textSubject, true);
		}
		else if (checkbox == this.checkboxMessage) {
			this.ui.setEnabled(textSubject, false);
			this.ui.setEditable(textSubject, false);
			this.ui.setText(textSubject, "");
		}
	}
	
	/**
	 * Occurrence type changed
	 * @param dialogReminderForm
	 * @param occurrence
	 */
	public void occurrenceChanged(Object dialogReminderForm, Object occurrence) {
		LOG.debug("occurrenceChanged");
		if (this.ui.getSelectedIndex(occurrence) == 0) {
			this.ui.setEnabled(this.comboHourEnd, false);
			this.ui.setEnabled(this.comboMinuteEnd, false);
			this.ui.setEnabled(this.comboAmPmEnd, false);
			this.ui.setEnabled(this.buttonDateEnd, false);
			this.ui.setEnabled(this.textDateEnd, false);
			this.ui.setSelectedIndex(this.comboHourEnd, -1);
			this.ui.setSelectedIndex(this.comboMinuteEnd, -1);
			this.ui.setSelectedIndex(this.comboAmPmEnd, -1);
		}
		else {
			this.ui.setEnabled(this.comboHourEnd, true);
			this.ui.setEnabled(this.comboMinuteEnd, true);
			this.ui.setEnabled(this.comboAmPmEnd, true);
			this.ui.setEnabled(this.textDateEnd, true);
			this.ui.setEnabled(this.buttonDateEnd, true);
		}
	}
	
	/**
	 * Show date selector
	 * @param textField
	 */
	public void showDateSelecter(Object textField) {
		LOG.debug("showDateSelecter");
		this.ui.showDateSelecter(textField);
	}
	
	/**
	 * Populate table
	 */
	public PagedListDetails getListDetails(Object list, int startIndex, int limit) {
		LOG.debug("getListDetails:" + this.ui.getName(list));
		List<Contact> contacts = this.contactDao.getAllContacts(startIndex, limit);
		Object[] listItems = toThinletContacts(contacts, this.selectedReminder);
		return new PagedListDetails(listItems.length, listItems);
	}
	
	/**
	 * Convert contacts to table rows
	 * @param contacts collection of Contacts
	 * @param reminder Reminder
	 * @return collection of table rows
	 */
	private Object[] toThinletContacts(List<Contact> contacts, Reminder reminder) {
		Object[] components = new Object[contacts.size()];
		for (int i = 0; i < components.length; i++) {
			Contact contact = contacts.get(i);
			components[i] = getContactRow(contact, reminder);
		}
		return components;
	}
	
	/**
	 * Get contact table row
	 * @param contact Contact
	 * @param reminder Reminder
	 * @return table row
	 */
	private Object getContactRow(Contact contact, Reminder reminder) {
		Object row = this.ui.createTableRow(contact);
		this.ui.add(row, this.ui.createTableCell(contact.getDisplayName()));
		this.ui.add(row, this.ui.createTableCell(contact.getPhoneNumber()));
		this.ui.add(row, this.ui.createTableCell(contact.getEmailAddress()));
		if (reminder != null) {
			for (String contactName : reminder.getRecipientsArray()) {
				if (contact.getName().equals(contactName)) {
					this.ui.setSelected(row, true);
				}
			}
		}
        return row;
	}
	
	/**
	 * Set date fields
	 * @param calendar
	 * @param textDate
	 * @param comboHour
	 * @param comboMinute
	 * @param comboAmPm
	 */
	private void setDateFields(Calendar calendar, Object textDate, Object comboHour, Object comboMinute, Object comboAmPm) {
		this.ui.setText(textDate, getDateStringFromCalendar(calendar));
		if (calendar.get(Calendar.HOUR_OF_DAY) == 0) {
			this.ui.setSelectedIndex(comboHour, 11);
		}
		else if (calendar.get(Calendar.HOUR_OF_DAY) == 12) {
			this.ui.setSelectedIndex(comboHour, 11);
		}
		else if (calendar.get(Calendar.HOUR_OF_DAY) < 12) {
			this.ui.setSelectedIndex(comboHour, calendar.get(Calendar.HOUR_OF_DAY) - 1);
		}
		else if (calendar.get(Calendar.HOUR_OF_DAY) > 12) {
			this.ui.setSelectedIndex(comboHour, calendar.get(Calendar.HOUR_OF_DAY) - 13);
		}
		else {
			this.ui.setSelectedIndex(comboHour, 0);
		}
		this.ui.setSelectedIndex(comboMinute, calendar.get(Calendar.MINUTE));
		this.ui.setSelectedIndex(comboAmPm, calendar.get(Calendar.AM_PM));
	}
	
	/**
	 * Get time ticks from date fields
	 * @param textDate
	 * @param comboHour
	 * @param comboMinute
	 * @param comboAmPm
	 * @return time ticks
	 */
	private long getLongFromDateFields(Object textDate, Object comboHour, Object comboMinute, Object comboAmPm) {
		try {
			String date = this.ui.getText(textDate);
			int hour = this.ui.getSelectedIndex(comboHour) + 1;
			int minute = this.ui.getSelectedIndex(comboMinute); 
			boolean isAM = this.ui.getSelectedIndex(comboAmPm) == 0;
			DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
			Date dateTime = dateFormat.parse(date);
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(dateTime);
			if (isAM && hour == 12) {
				calendar.set(Calendar.HOUR_OF_DAY, 0);
			}
			else if (isAM && hour < 12) {
				calendar.set(Calendar.HOUR_OF_DAY, hour);
			}
			else if (!isAM && hour == 12) { 
				calendar.set(Calendar.HOUR_OF_DAY, 12);
			}
			else if (!isAM) {
				calendar.set(Calendar.HOUR_OF_DAY, hour + 12);
			}
			calendar.set(Calendar.MINUTE, minute);
			return calendar.getTimeInMillis();
		} 
		catch (ParseException e) {
			return 0;
		}
	}
	
	/**
	 * Get date string from Calendar object
	 * @param calendar Calendar
	 * @return date string
	 */
	private String getDateStringFromCalendar(Calendar calendar) {
		return String.format("%02d/%02d/%d", calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR));
	}
}
