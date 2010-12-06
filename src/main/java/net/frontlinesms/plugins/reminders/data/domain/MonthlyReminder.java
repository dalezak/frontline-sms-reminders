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

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.apache.log4j.Logger;

import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.plugins.reminders.RemindersConstants;
import net.frontlinesms.ui.i18n.InternationalisationUtils;

@Entity
@DiscriminatorValue(value=MonthlyReminder.OCCURRENCE)
public class MonthlyReminder extends Reminder {

	public static final String OCCURRENCE = "monthly";
	
	private static final Logger LOG = FrontlineUtils.getLogger(MonthlyReminder.class);
	
	public MonthlyReminder() {}
	
	public MonthlyReminder(long startdate, long enddate, Type type, String recipients, String subject, String content) {
		super(startdate, enddate, type, recipients, subject, content);
	}
	
	@Override
	public String getOccurrenceLabel() {
		return InternationalisationUtils.getI18NString(RemindersConstants.MONTHLY);
	}
	
	@Override
	public String getOccurrence() {
		return MonthlyReminder.OCCURRENCE;
	}

	public static boolean isSatisfiedBy(String occurrence) {
		return MonthlyReminder.OCCURRENCE.equalsIgnoreCase(occurrence);
	}

	public long getPeriod() {
		return 1000 * 60 * 60 * 24;
	}
	
	public void run() {
		LOG.debug("run: " + this);
		Calendar now = Calendar.getInstance();
		Calendar start = this.getStartCalendar();
		Calendar end = this.getEndCalendar();
		if (now.after(end)) {
			this.setStatus(Status.SENT);
			this.stopReminder();
			this.refreshReminder();
		}
		else if ((now.equals(start) || now.after(start)) &&
				  now.get(Calendar.MINUTE) == start.get(Calendar.MINUTE) &&
				  now.get(Calendar.HOUR_OF_DAY) == start.get(Calendar.HOUR_OF_DAY) &&
				  now.get(Calendar.DAY_OF_MONTH) == start.get(Calendar.DAY_OF_MONTH) &&
				  	(now.get(Calendar.MONTH) >= start.get(Calendar.MONTH) ||
					 now.get(Calendar.YEAR) > start.get(Calendar.YEAR))) {
			this.sendReminder();
		}
	}
}