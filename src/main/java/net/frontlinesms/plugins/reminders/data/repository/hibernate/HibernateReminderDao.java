/**
 * 
 */
package net.frontlinesms.plugins.reminders.data.repository.hibernate;

import java.util.Collection;
import java.util.List;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;

import net.frontlinesms.data.repository.hibernate.BaseHibernateDao;

import net.frontlinesms.plugins.reminders.data.domain.Reminder;
import net.frontlinesms.plugins.reminders.data.domain.Reminder.Field;
import net.frontlinesms.plugins.reminders.data.repository.ReminderDao;

/**
 * @author Dale Zak
 *
 */
public class HibernateReminderDao extends BaseHibernateDao<Reminder> implements ReminderDao {
	/** Create a new instance of this class */
	public HibernateReminderDao() {
		super(Reminder.class);
	}

	/** @see ReminderDao#getAllReminders() */
	public Collection<Reminder> getAllReminders() {
		return super.getAll();
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
		super.delete(reminder);
	}

}
