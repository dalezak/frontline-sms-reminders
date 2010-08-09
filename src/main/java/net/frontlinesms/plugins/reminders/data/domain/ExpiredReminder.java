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
package net.frontlinesms.plugins.reminders.data.domain;

import java.util.Calendar;

import org.apache.log4j.Logger;

import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.plugins.reminders.data.domain.Reminder.Status;

/**
 * ExpiredReminder
 * @author dalezak
 *
 */
public class ExpiredReminder implements Runnable {

	private static final Logger LOG = FrontlineUtils.getLogger(ExpiredReminder.class);
	
	/**
	 * Reminder
	 */
	private Reminder reminder;
	
	/**
	 * ExpiredReminder
	 * @param reminder
	 */
	public ExpiredReminder(Reminder reminder) {
		this.reminder = reminder;
	}
	
	/**
	 * Mark Reminder as Sent if it's past the end date
	 */
	public void run() {
		Calendar now = Calendar.getInstance();
		Calendar end = this.reminder.getEndCalendar();
		if (now.equals(end) || now.after(end)) {
			LOG.debug("Reminder Expired: " + this.reminder);
			this.reminder.setStatus(Status.SENT);
			this.reminder.stopReminder();
			this.reminder.refreshReminder();
		}
	}
	
}