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

import net.frontlinesms.Utils;
import net.frontlinesms.data.domain.Contact;
import net.frontlinesms.data.repository.ContactDao;
import net.frontlinesms.data.repository.EmailAccountDao;
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
public class RemindersDialogHandler implements ThinletUiEventHandler, PagedComponentItemProvider {
	
	private static Logger LOG = Utils.getLogger(RemindersDialogHandler.class);
	
	private static final String DIALOG_XML = "/ui/plugins/reminders/remindersForm.xml";
	
	private static final String DIALOG_TABLE = "tableRecipients";
	private static final String DIALOG_PANEL = "panelRecipients";
	
	private static final String DIALOG_IS_EMAIL = "checkboxEmail";
	private static final String DIALOG_IS_MESSAGE = "checkboxMessage";
	private static final String DIALOG_SUBJECT = "textSubject";
	private static final String DIALOG_MESSAGE = "textMessage";
	
	private static final String DIALOG_COMBO_OCCURRENCE = "comboOccurrence";
	
	private static final String DIALOG_COMBO_HOUR_START = "comboHourStart";
	private static final String DIALOG_COMBO_MINUTE_START = "comboMinuteStart";
	private static final String DIALOG_COMBO_AM_PM_START = "comboAmPmStart";
	private static final String DIALOG_DATE_START = "textDateStart";
	
	private static final String DIALOG_COMBO_HOUR_END = "comboHourEnd";
	private static final String DIALOG_COMBO_MINUTE_END = "comboMinuteEnd";
	private static final String DIALOG_COMBO_AM_PM_END = "comboAmPmEnd";
	private static final String DIALOG_DATE_END = "textDateEnd";
	private static final String DIALOG_BUTTON_DATE_END = "buttonDateEnd";

	private Object dialogComponent;
	private UiGeneratorController ui;
	private ApplicationContext applicationContext;
	
	private final ReminderDao reminderDao;
	private final ContactDao contactDao;
	private final EmailAccountDao emailAccountDao;
	
	private ComponentPagingHandler contactListPager;
	private Object contactListComponent;
	private Reminder selectedReminder;
	private RemindersTabHandler tabHandler;
	
	public RemindersDialogHandler(UiGeneratorController ui, ApplicationContext applicationContext, RemindersTabHandler tabHandler) {
		this.ui = ui;
		this.applicationContext = applicationContext;
		this.tabHandler = tabHandler;
		this.reminderDao = (ReminderDao) this.applicationContext.getBean("reminderDao");
		this.contactDao = this.ui.getFrontlineController().getContactDao();
		this.emailAccountDao = this.ui.getFrontlineController().getEmailAccountFactory();
	}
	
	public void init(Reminder reminder) {
		this.selectedReminder = reminder;
		this.dialogComponent = this.ui.loadComponentFromFile(DIALOG_XML, this);
		
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
			this.ui.add(comboAmPmStart, this.ui.createComboboxChoice(amPM, 0));
			this.ui.add(comboAmPmEnd, this.ui.createComboboxChoice(amPM, 0));
		}
		
		Object textDateStart = this.ui.find(reminderDialog, DIALOG_DATE_START);
		Object textDateEnd = this.ui.find(reminderDialog, DIALOG_DATE_END);
		
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
			this.ui.setText(this.dialogComponent, InternationalisationUtils.getI18NString(RemindersConstants.EDIT_REMINDER));
			
		}
		else {
			Calendar now = Calendar.getInstance();
			setDateFields(now, textDateStart, comboHourStart, comboMinuteStart, comboAmPmStart);
			setDateFields(now, textDateEnd, comboHourEnd, comboMinuteEnd, comboAmPmEnd);
			this.ui.setSelectedIndex(comboOccurrence, Reminder.getIndexForOccurrence(Reminder.Occurrence.ONCE));
			this.ui.setText(reminderDialog, InternationalisationUtils.getI18NString(RemindersConstants.CREATE_REMINDER));
			this.ui.setText(this.dialogComponent, InternationalisationUtils.getI18NString(RemindersConstants.CREATE_REMINDER));
		}
	}
	
	public void saveReminder(Object dialog, Object table) {
		LOG.trace("saveReminder");
		Object comboOccurrence = this.ui.find(dialog, DIALOG_COMBO_OCCURRENCE);
		
		Object comboHourStart = this.ui.find(dialog, DIALOG_COMBO_HOUR_START);
		Object comboHourEnd = this.ui.find(dialog, DIALOG_COMBO_HOUR_END);
		
		Object comboMinuteStart = this.ui.find(dialog, DIALOG_COMBO_MINUTE_START);
		Object comboMinuteEnd = this.ui.find(dialog, DIALOG_COMBO_MINUTE_END);
		
		Object comboAmPmStart = this.ui.find(dialog, DIALOG_COMBO_AM_PM_START);
		Object comboAmPmEnd = this.ui.find(dialog, DIALOG_COMBO_AM_PM_END);
		
		Object textDateStart = this.ui.find(dialog, DIALOG_DATE_START);
		Object textDateEnd = this.ui.find(dialog, DIALOG_DATE_END);
		
		Object checkboxEmail = this.ui.find(dialog, DIALOG_IS_EMAIL);
		
		Object textSubject = this.ui.find(dialog, DIALOG_SUBJECT);
		Object textMessage = this.ui.find(dialog, DIALOG_MESSAGE);
		try {
			Type type = this.ui.isSelected(checkboxEmail) ? Type.EMAIL : Type.MESSAGE;
			StringBuilder recipients = new StringBuilder();
			for (Object selected : this.ui.getSelectedItems(table)) {
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
				Reminder reminder = this.ui.getAttachedObject(dialog, Reminder.class);
				if (reminder != null) {
					reminder.setStartDate(startDate);
					reminder.setEndDate(endDate);
					reminder.setType(type);
					reminder.setRecipients(recipients.toString());
					reminder.setSubject(subject);
					reminder.setContent(message);
					reminder.setOccurrence(Reminder.getOccurrenceForIndex(occurrence));
					this.reminderDao.updateReminder(reminder);
					if (type == Reminder.Type.EMAIL) {
						this.ui.setStatus(InternationalisationUtils.getI18NString(RemindersConstants.EMAIL_REMINDER_UPDATED));
					}
					else {
						this.ui.setStatus(InternationalisationUtils.getI18NString(RemindersConstants.SMS_REMINDER_UPDATED));
					}
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
					if (type == Reminder.Type.EMAIL) {
						this.ui.setStatus(InternationalisationUtils.getI18NString(RemindersConstants.EMAIL_REMINDER_CREATED));
					}
					else {
						this.ui.setStatus(InternationalisationUtils.getI18NString(RemindersConstants.SMS_REMINDER_CREATED));
					}
				}
				this.ui.remove(dialog);
			}
		} catch (Exception ex) {
			LOG.trace(ex);
			this.ui.alert(ex.getMessage());
		}
		if (tabHandler != null) {
			tabHandler.refreshReminders();
		}
	}
	
	public void removeDialog(Object dialog) {
		this.ui.removeDialog(dialog);
	}
	
	public void typeChanged(Object checkbox, Object textSubject, Object textMessage) {
		LOG.trace("typeChanged");
		if (DIALOG_IS_EMAIL.equalsIgnoreCase(ui.getName(checkbox))) {
			this.ui.setEnabled(textSubject, true);
		}
		else if (DIALOG_IS_MESSAGE.equalsIgnoreCase(ui.getName(checkbox))) {
			this.ui.setEnabled(textSubject, false);
			this.ui.setText(textSubject, "");
		}
	}
	
	public void occurrenceChanged(Object dialogReminderForm, Object occurrence) {
		LOG.trace("occurrenceChanged");
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
		LOG.trace("showDateSelecter");
		this.ui.showDateSelecter(textField);
	}
	
	public PagedListDetails getListDetails(Object list, int startIndex, int limit) {
		LOG.trace("getListDetails:" + ui.getName(list));
		List<Contact> contacts = this.contactDao.getAllContacts(startIndex, limit);
		Object[] listItems = toThinletContacts(contacts, this.selectedReminder);
		return new PagedListDetails(listItems.length, listItems);
	}
	
	private Object[] toThinletContacts(List<Contact> contacts, Reminder reminder) {
		Object[] components = new Object[contacts.size()];
		for (int i = 0; i < components.length; i++) {
			Contact c = contacts.get(i);
			components[i] = getContactRow(c, reminder);
		}
		return components;
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
}
