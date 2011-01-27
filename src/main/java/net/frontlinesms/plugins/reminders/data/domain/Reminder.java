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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.DiscriminatorType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.InheritanceType;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cascade;

import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.data.EntityField;
import net.frontlinesms.data.domain.EmailAccount;
import net.frontlinesms.plugins.reminders.RemindersCallback;

/*
 * Reminder
 * @author Dale Zak
 * 
 * see {@link "http://www.frontlinesms.net"} for more details. 
 * copyright owned by Kiwanja.net
 */
@Entity
@Table(name="reminder")
@DiscriminatorColumn(name="occurrence", discriminatorType=DiscriminatorType.STRING)
@DiscriminatorValue(value="reminder")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public abstract class Reminder implements Runnable {
	
	private static final Logger LOG = FrontlineUtils.getLogger(Reminder.class);
	
	public static final String RECIPIENT_SEPARATOR = ";";
	
//> COLUMN_CONSTANTS
	/** Column name for {@link #status} */
	private static final String COLUMN_STATUS = "status";
	/** Column name for {@link #startdate} */
	private static final String COLUMN_STARTDATE = "startdate";
	/** Column name for {@link #startdate} */
	private static final String COLUMN_ENDDATE = "enddate";
	/** Column name for {@link #status} */
	private static final String COLUMN_TYPE = "type";
	/** Column name for {@link #recipients} */
	private static final String COLUMN_RECIPIENTS = "recipients";
	/** Column name for {@link #subject} */
	private static final String COLUMN_SUBJECT = "subject";
	/** Column name for {@link #content} */
	private static final String COLUMN_CONTENT = "content";
	/** Column name for {@link #occurrence} */
	private static final String COLUMN_OCCURRENCE = "occurrence";
	
//> ENTITY FIELDS
	/** Details of the fields that this class has. */
	public enum Field implements EntityField<Reminder> {
		/** field mapping for {@link Reminder#status} */
		STATUS(COLUMN_STATUS),
		/** field mapping for {@link Reminder#startdate} */
		STARTDATE(COLUMN_STARTDATE),
		/** field mapping for {@link Reminder#enddate} */
		ENDDATE(COLUMN_ENDDATE),
		/** field mapping for {@link Reminder#type} */
		TYPE(COLUMN_TYPE),
		/** field mapping for {@link Reminder#sender} */
		TO(COLUMN_RECIPIENTS),
		/** field mapping for {@link Reminder#subject} */
		SUBJECT(COLUMN_SUBJECT),
		/** field mapping for {@link Reminder#content} */
		CONTENT(COLUMN_CONTENT),
		/** field mapping for {@link Reminder#occurrence} */
		OCCURRENCE(COLUMN_OCCURRENCE);
		/** name of a field */
		private final String fieldName;
		/**
		 * Creates a new {@link Field}
		 * @param fieldName name of the field
		 */
		Field(String fieldName) { this.fieldName = fieldName; }
		/** @see EntityField#getFieldName() */
		public String getFieldName() { return this.fieldName; }
	}

//> INSTANCE PROPERTIES
	/** Unique id for this entity. This is for hibernate usage. */
	@SuppressWarnings("unused")
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(unique=true,nullable=false,updatable=false)
	private long id;
	
	/** Status of this reminder */
	@Column(name=COLUMN_TYPE)
	private Type type;
	
	/** Status of this sent */
	@Column(name=COLUMN_STATUS)
	private Status status;
	
	/** Subject line of the reminder */
	@Column(name=COLUMN_SUBJECT)
	private String subject;

	/** Content of the reminder */
	@Column(name=COLUMN_CONTENT)
	private String content;
	
	/** Start Date of the reminder */
	@Column(name=COLUMN_STARTDATE)
	private long startdate;

	/** End Date of the reminder */
	@Column(name=COLUMN_ENDDATE)
	private long enddate;
	
	/** Recipient of the reminder */
	@Column(name=COLUMN_RECIPIENTS)
	private String recipients;
	
	/** Recipient of the reminder */
	@ManyToOne(cascade=CascadeType.ALL)
	@Cascade(value = { org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
	private EmailAccount emailAccount;

//> CONSTRUCTORS
	/** Empty constructor required for hibernate. */
	public Reminder() {}
	
	/**
	 * Creates an reminder with the supplied properties.
	 * @param from The account to send the reminder
	 * @param recipients The reminder recipients
	 * @param subject The reminder subject
	 * @param content The reminder content
	 */
	protected Reminder(long startdate, long enddate, Type type, String recipients, String subject, String content) {
		this.startdate = startdate;
		this.enddate = enddate;
		this.type = type;
		this.recipients = recipients;
		this.subject = subject;
		this.content = content;
	}
	
	public enum Type {
		/** Email Reminder */
		EMAIL,
		/** Message Reminder */
		MESSAGE
	}
	
	public enum Status {
		/** Pending */
		PENDING,
		/** Sent */
		SENT
	}
	
//> ABSTRACT METHODS
	/**
	 * Gets the Occurrence of this Reminder.
	 */
	public abstract String getOccurrence();
	
	/**
	 * Gets the Occurrence label of this Reminder.
	 */
	public abstract String getOccurrenceLabel();
	
	/**
	 * Gets the Period of this Reminder.
	 */
	public abstract long getPeriod();
	
	/**
	 * Each subclass will provide their own implementation
	 */
	public abstract void run();
	
	/**
	 * Schedule this Reminder.
	 */
	public void scheduleReminder() {
		LOG.debug("Reminder.scheduleReminder: " + this);
		if (remindersCallback != null) {
			remindersCallback.scheduleReminder(this);
		}
	}
	
	/**
	 * Send this Reminder.
	 */
	public void sendReminder() {
		LOG.debug("Reminder.sendReminder: " + this);
		if (remindersCallback != null) {
			remindersCallback.sendReminder(this);
		}
	}
	
	/**
	 * Refresh this Reminder.
	 */
	public void refreshReminder() {
		LOG.debug("Reminder.refreshReminder: " + this);
		if (remindersCallback != null) {
			remindersCallback.refreshReminders(this);
		}
	}
	
	/**
	 * Stop this Reminder.
	 */
	public void stopReminder() {
		LOG.debug("Reminder.stopReminder: " + this);
		if (remindersCallback != null) {
			remindersCallback.stopReminder(this);
		}
	}
	
	/*
	* Set the callback interface for sending and refreshing reminders
	**/	
	public static void setCallback(RemindersCallback callback) {
		remindersCallback = callback;
	}private static RemindersCallback remindersCallback;
	
	/*
	 * Reminder toString()
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.format("[%s, %s, %s, %s, %s, [%s], %s, %s]", 
							this.getOccurrence(), this.getType(), this.getStatus(), 
							new Date(this.startdate), new Date(this.enddate), 
							this.recipients, this.subject, this.content);
	}
	
//> ACCESSOR METHODS
	/**
	 * @param subject new value for {@link #subject} 
	 */
	public void setSubject(String subject) {
		this.subject = subject;
	}

	/** @param content new value for {@link #content} */
	public void setContent(String content) {
		this.content = content;
	}
	
	/** @param recipients new value for {@link #recipients} */
	public void setRecipients(String recipients) {
		this.recipients = recipients;
	}
	
	/**
	 * Gets the of this Reminder.  Should be one of the Reminder.STATUS_ constants.
	 * @return {@link #status}
	 */
	public Status getStatus() {
		return this.status;
	}
	
	/**
	 * sets the status of this Reminder.  Should be one of the Reminder.STATUS_ constants.
	 * @param status new value for {@link #status}
	 */
	public void setStatus(Status status) {
		this.status = status;
	}
	
	/**
	 * Gets the status of this Reminder.  Should be one of the Email.STATUS_ constants.
	 * @return {@link #status}
	 */
	public Type getType() {
		return this.type;
	}
	
	/**
	 * sets the type of this Reminder.  Should be one of the Reminder.TYPE_ constants.
	 * @param reminderType new value for {@link #reminderType}
	 */
	public void setType(Type reminderType) {
		this.type = reminderType;
	}
	
	/**
	 * Gets the text content of this reminder.
	 * @return {@link #content}
	 */
	public String getContent() {
		return this.content;
	}
	
	/**
	 * Gets this action reminder recipients.
	 * @return {@link #recipients}
	 */
	public String getRecipients() {
		return this.recipients;
	}

	public String[] getRecipientsArray() {
		List<String> recipientsArray = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(this.getRecipients(), Reminder.RECIPIENT_SEPARATOR);
		while (st.hasMoreTokens()) {
			recipientsArray.add(st.nextToken());
		}
		return recipientsArray.toArray(new String[recipientsArray.size()]);
	}
	
	/**
	 * Gets this action reminder subject.
	 * @return  {@link #subject}
	 */
	public String getSubject() {
		return this.subject;
	}

	/** @param start date new value for {@link #startdate} */
	public void setStartDate(long startdate) {
		this.startdate = startdate;
	}
	
	/**
	 * Gets the start date at which this reminder was sent.
	 * @return {@link #startdate}
	 */
	public long getStartDate() {
		return this.startdate;
	}
	
	public Calendar getStartCalendar() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(this.startdate);
		return calendar;
	}
	
	/** @param end date new value for {@link #enddate} */
	public void setEndDate(long enddate) {
		this.enddate = enddate;
	}
	
	/**
	 * Gets the end date at which this reminder was sent.
	 * @return {@link #enddate}
	 */
	public long getEndDate() {
		return this.enddate;
	}
	
	public Calendar getEndCalendar() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(this.enddate);
		return calendar;
	}
	
	public void setEmailAccount(EmailAccount emailAccount) {
		this.emailAccount = emailAccount;
	}

	public EmailAccount getEmailAccount() {
		return emailAccount;
	}

	//> GENERATED CODE
	/** @see java.lang.Object#hashCode() */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((content == null) ? 0 : content.hashCode());
		result = prime * result + (int) (startdate ^ (startdate >>> 32));
		result = prime * result + (int) (enddate ^ (enddate >>> 32));
		result = prime * result + ((recipients == null) ? 0 : recipients.hashCode());
		result = prime * result + ((subject == null) ? 0 : subject.hashCode());
		return result;
	}
	
	/** @see java.lang.Object#equals(java.lang.Object) */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Reminder other = (Reminder) obj;
		if (content == null) {
			if (other.content != null) return false;
		} else if (!content.equals(other.content)) {
			return false;
		}
		if (startdate != other.startdate) return false;
		if (enddate != other.enddate) return false;
		if (type != other.type) return false;
		if (!getOccurrence().equalsIgnoreCase(other.getOccurrence())) return false;
		if (recipients == null) {
			if (other.recipients != null)
				return false;
		} else if (!recipients.equals(other.recipients))
			return false;
		if (subject == null) {
			if (other.subject != null) return false;
		} else if (!subject.equals(other.subject)) {
			return false;
		}
		return true;
	}
}