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

/*
 * DailyReminder
 * @author Dale Zak
 * 
 * see {@link "http://www.frontlinesms.net"} for more details. 
 * copyright owned by Kiwanja.net
 */
@Entity
@DiscriminatorValue(value=DailyReminder.OCCURRENCE)
public class DailyReminder extends Reminder {
	
	public static final String OCCURRENCE = "daily";
	
	private static final Logger LOG = FrontlineUtils.getLogger(DailyReminder.class);
	
	public DailyReminder() {}
	
	public DailyReminder(long startdate, long enddate, Type type, String recipients, String subject, String content) {
		super(startdate, enddate, type, recipients, subject, content);
	}
	
	@Override
	public String getOccurrenceLabel() {
		return InternationalisationUtils.getI18NString(RemindersConstants.DAILY);
	}
	
	@Override
	public String getOccurrence() {
		return DailyReminder.OCCURRENCE;
	}
	
	public static boolean isSatisfiedBy(String occurrence) {
		return DailyReminder.OCCURRENCE.equalsIgnoreCase(occurrence);
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
				  now.get(Calendar.HOUR_OF_DAY) == start.get(Calendar.HOUR_OF_DAY)) {
			this.sendReminder();
		}
	}
}