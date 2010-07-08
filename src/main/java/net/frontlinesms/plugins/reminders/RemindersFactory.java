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

import java.util.List;
import java.util.ArrayList;
import java.util.ServiceLoader;

import org.apache.log4j.Logger;

import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.plugins.reminders.data.domain.Reminder;
import net.frontlinesms.plugins.reminders.data.domain.Reminder.Type;

/*
 * RemindersFactory
 * @author Dale Zak
 * 
 * see {@link "http://www.frontlinesms.net"} for more details. 
 * copyright owned by Kiwanja.net
 */
public final class RemindersFactory {

	private static Logger LOG = FrontlineUtils.getLogger(RemindersFactory.class);
	
	/*
	 * Get list of Reminder class implementations
	 * (To add a new Reminder classes to the project, append a new row to the file
	 * /resources/META-INF/services/net.frontlinesms.plugins.reminders.data.domain.Reminder
	 * with the full package and class name of the new implementing Reminder class)
	 */
	public static List<Reminder> getReminderClasses() {
		if (reminderClasses == null) {
			reminderClasses = new ArrayList<Reminder>();
			for (Reminder reminder : ServiceLoader.load(Reminder.class)) {
				LOG.debug("Reminder Discovered: " + reminder);
				reminderClasses.add(reminder);
		    }
		}
		return reminderClasses;
	}private static List<Reminder> reminderClasses = null;
	
	/*
	 * Create a Reminder according to it's occurrence
	 */
	public static Reminder createReminder(long startdate, long enddate, Type type, String recipients, String subject, String content, String occurrence) {
		for (Reminder reminderClass : getReminderClasses()) {
			if (reminderClass.getOccurrence().equalsIgnoreCase(occurrence)) {
				try {
					Reminder newReminder = reminderClass.getClass().newInstance();
					newReminder.setStartDate(startdate);
					newReminder.setEndDate(enddate);
					newReminder.setType(type);
					newReminder.setRecipients(recipients);
					newReminder.setSubject(subject);
					newReminder.setContent(content);
					LOG.debug("Reminder Created: " + newReminder);
					return newReminder;
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		LOG.debug("Unable to find class for occurrence: " + occurrence);
		return null;
	}
	
}