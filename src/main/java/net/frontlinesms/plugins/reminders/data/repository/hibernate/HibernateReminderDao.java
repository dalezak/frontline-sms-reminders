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
package net.frontlinesms.plugins.reminders.data.repository.hibernate;

import java.util.Collection;
import java.util.List;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;

import net.frontlinesms.data.repository.hibernate.BaseHibernateDao;

import net.frontlinesms.plugins.reminders.data.domain.Reminder;
import net.frontlinesms.plugins.reminders.data.domain.Reminder.Field;
import net.frontlinesms.plugins.reminders.data.repository.ReminderDao;

/*
 * HibernateReminderDao
 * @author Dale Zak
 * 
 * see {@link "http://www.frontlinesms.net"} for more details. 
 * copyright owned by Kiwanja.net
 */
public class HibernateReminderDao extends BaseHibernateDao<Reminder> implements ReminderDao {
	/** Create a new instance of this class */
	public HibernateReminderDao() {
		super(Reminder.class);
	}

	/** @see ReminderDao#getAllReminders() */
	public Collection<Reminder> getAllReminders() {
		DetachedCriteria c = super.getCriterion(); 
        return super.getList(c);
    }

	/** @see ReminderDao#getPendingReminders() */
	public Collection<Reminder> getPendingReminders() {
		DetachedCriteria criteria = super.getCriterion();
		Criterion notSent = Restrictions.ne(Field.STATUS.getFieldName(), Reminder.Status.SENT);
		Criterion isNull = Restrictions.isNull(Field.STATUS.getFieldName());
		criteria.add(Restrictions.or(notSent, isNull));
		return super.getList(criteria);
	}
	
	/** @see ReminderDao#getAllReminders(int,int) */
	public List<Reminder> getAllReminders(int startIndex, int limit) {
		return super.getAll(startIndex, limit);
	}
	
	/** @see ReminderDao#getReminderCount() */
	public int getReminderCount() {
		return super.countAll();
	}

	/** @see ReminderDao#getRemindersForType(Reminder.Type[]) */
	public Collection<Reminder> getRemindersForType(Reminder.Type[] type) {
		DetachedCriteria criteria = super.getCriterion();
		criteria.add(Restrictions.in(Field.TYPE.getFieldName(), type));
		return super.getList(criteria);
	}

	/** @see ReminderDao#saveReminder(Reminder) */
	public void saveReminder(Reminder reminder) {
		super.saveWithoutDuplicateHandling(reminder);
	}

	/** @see ReminderDao#updateReminder(Reminder) */
	public void updateReminder(Reminder reminder) {
		super.updateWithoutDuplicateHandling(reminder);
	}
	
	/** @see ReminderDao#deleteReminder(Reminder) */
	public void deleteReminder(Reminder reminder) {
		if (reminder != null) {
			reminder.stopReminder();
		}
		super.delete(reminder);
	}
}
