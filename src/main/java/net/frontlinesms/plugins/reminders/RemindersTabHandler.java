package net.frontlinesms.plugins.reminders;

import net.frontlinesms.plugins.reminders.data.domain.Reminder;

public interface RemindersTabHandler {
	
	public void refreshReminders();
	public void sendReminder(Reminder reminder);
	
}