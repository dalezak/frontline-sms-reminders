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

import java.util.Calendar;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import net.frontlinesms.Utils;
import net.frontlinesms.plugins.reminders.data.domain.Reminder;
import net.frontlinesms.plugins.reminders.data.repository.ReminderDao;

/*
 * ReminderTimerTask
 * @author Dale Zak
 * 
 * see {@link "http://www.frontlinesms.net"} for more details. 
 * copyright owned by Kiwanja.net
 */
public class ReminderTimerTask extends TimerTask {
	
	private static Logger LOG = Utils.getLogger(ReminderTimerTask.class);
	
	private final RemindersTabHandler tabHandler;
	private final ReminderDao reminderDao;
	
	public ReminderTimerTask(RemindersTabHandler tabHandler, ReminderDao reminderDao) {
		this.tabHandler = tabHandler;
		this.reminderDao = reminderDao;
	}
	
	public void run() {
		LOG.trace("ReminderTimerTask.run");
		Calendar now = Calendar.getInstance();
		//TODO only retrieve pending reminders
		for (Reminder reminder : this.reminderDao.getAllReminders()) {
			Calendar date = Calendar.getInstance();
			date.setTimeInMillis(reminder.getStartDate());
			if (reminder.getStatus() != Reminder.Status.SENT) {
				if (reminder.getOccurrence() == Reminder.Occurrence.ONCE) {
					if (now.getTimeInMillis() >= reminder.getStartDate()) {
						LOG.trace("Once Reminder Sent!");
						this.tabHandler.sendReminder(reminder);
					}
				}
				else if (reminder.getOccurrence() == Reminder.Occurrence.HOURLY) {
					if (now.get(Calendar.MINUTE) == date.get(Calendar.MINUTE) &&
						(now.get(Calendar.HOUR_OF_DAY) >= date.get(Calendar.HOUR_OF_DAY) ||
						 now.get(Calendar.DAY_OF_YEAR) >= date.get(Calendar.DAY_OF_YEAR) ||
						 now.get(Calendar.YEAR) >= date.get(Calendar.YEAR))) {
						LOG.trace("Hourly Reminder Sent!");
						this.tabHandler.sendReminder(reminder);
					}
				}
				else if (reminder.getOccurrence() == Reminder.Occurrence.DAILY) {
					if (now.get(Calendar.HOUR_OF_DAY) == date.get(Calendar.HOUR_OF_DAY) &&
						now.get(Calendar.MINUTE) == date.get(Calendar.MINUTE)) {
						LOG.trace("Daily Reminder Sent!");
						this.tabHandler.sendReminder(reminder);
					}
				}
				else if (reminder.getOccurrence() == Reminder.Occurrence.WEEKLY) {
					if (now.get(Calendar.HOUR_OF_DAY) == date.get(Calendar.HOUR_OF_DAY) &&
						now.get(Calendar.MINUTE) == date.get(Calendar.MINUTE) &&
						now.get(Calendar.DAY_OF_WEEK) == date.get(Calendar.DAY_OF_WEEK) &&
						(now.get(Calendar.WEEK_OF_YEAR) >= date.get(Calendar.WEEK_OF_YEAR) ||
						 now.get(Calendar.YEAR) > date.get(Calendar.YEAR))) {
						LOG.trace("Weekly Reminder Sent!");
						this.tabHandler.sendReminder(reminder);
					}				
				}
				else if (reminder.getOccurrence() == Reminder.Occurrence.MONTHLY) {
					if (now.get(Calendar.HOUR_OF_DAY) == date.get(Calendar.HOUR_OF_DAY) &&
						now.get(Calendar.MINUTE) == date.get(Calendar.MINUTE) &&
						now.get(Calendar.DAY_OF_MONTH) == date.get(Calendar.DAY_OF_MONTH) &&
						(now.get(Calendar.MONTH) >= date.get(Calendar.MONTH) ||
						 now.get(Calendar.YEAR) > date.get(Calendar.YEAR))) {
						LOG.trace("Monthly Reminder Sent!");
						this.tabHandler.sendReminder(reminder);
					}	
				}
			}
		}
	}
}