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
package net.frontlinesms.plugins.reminders.data.repository;

import java.util.Collection;
import java.util.List;

import net.frontlinesms.plugins.reminders.data.domain.Reminder;

/*
 * ReminderDao
 * @author Dale Zak
 * 
 * see {@link "http://www.frontlinesms.net"} for more details. 
 * copyright owned by Kiwanja.net
 */
public interface ReminderDao {
	/**
	 * Gets all reminder with particular statuses.
	 * @param status
	 * @return all reminders with the supplied statuses 
	 */
	public Collection<Reminder> getRemindersForType(Reminder.Type[] type);
	
	/** @return all reminders */
	public Collection<Reminder> getAllReminders();
	
	/** @return all pending reminders */
	public Collection<Reminder> getPendingReminders();
	
	/**
	 * Returns all reminders from a particular start index, with a maximum number of returned reminders set.
	 * @param startIndex index of the first reminder to fetch
	 * @param limit max number of reminders to fetch
	 * @return a subset of all the reminders
	 */
	public List<Reminder> getAllReminders(int startIndex, int limit);
	
	/** @return number of reminder in the data source */
	public int getReminderCount();
	
	/**
	 * Delete an reminder from the data source. 
	 * @param reminder reminder to delete.
	 */
	public void deleteReminder(Reminder reminder);

	/**
	 * Save this reminder to the data source.
	 * @param reminder the reminder to save
	 */
	public void saveReminder(Reminder reminder);

	/**
	 * Update this reminder to the data source.
	 * @param reminder the reminder to update
	 */
	public void updateReminder(Reminder reminder);
}
