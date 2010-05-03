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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import javax.persistence.*;

import net.frontlinesms.data.EntityField;

/**
 * Object representing a reminder in our data structure.
 * 
 * @author Dale Zak
 */
@Entity
public class Reminder {
	
	public static final String RECIPIENT_SEPARATOR = ";";
	
//> COLUMN_CONSTANTS
	/** Column name for {@link #status} */
	private static final String COLUMN_STATUS = "status";
	/** Column name for {@link #date} */
	private static final String COLUMN_DATE = "date";
	/** Column name for {@link #status} */
	private static final String COLUMN_TYPE = "type";
	/** Column name for {@link #recipients} */
	private static final String COLUMN_RECIPIENTS = "recipients";
	/** Column name for {@link #subject} */
	private static final String COLUMN_SUBJECT = "subject";
	/** Column name for {@link #content} */
	private static final String COLUMN_CONTENT = "content";
	
//> ENTITY FIELDS
	/** Details of the fields that this class has. */
	public enum Field implements EntityField<Reminder> {
		/** field mapping for {@link Reminder#status} */
		STATUS(COLUMN_STATUS),
		/** field mapping for {@link Reminder#date} */
		DATE(COLUMN_DATE),
		/** field mapping for {@link Reminder#type} */
		TYPE(COLUMN_TYPE),
		/** field mapping for {@link Reminder#sender} */
		TO(COLUMN_RECIPIENTS),
		/** field mapping for {@link Reminder#subject} */
		SUBJECT(COLUMN_SUBJECT),
		/** field mapping for {@link Reminder#content} */
		CONTENT(COLUMN_CONTENT);
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
	
//> CONSTANTS
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

	/** Date of the reminder */
	@Column(name=COLUMN_DATE)
	private long date;

	/** Recipient of the reminder */
	@Column(name=COLUMN_RECIPIENTS)
	private String recipients;

//> CONSTRUCTORS
	/** Empty constructor required for hibernate. */
	Reminder() {}
	
	/**
	 * Creates an reminder with the supplied properties.
	 * @param from The account to send the reminder
	 * @param recipients The reminder recipients
	 * @param subject The reminder subject
	 * @param content The reminder content
	 */
	public Reminder(long date, Type type, String recipients, String subject, String content) {
		this.type = type;
		this.date = date;
		this.recipients = recipients;
		this.subject = subject;
		this.content = content;
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
	 * @param reminderType new value for {@link #status}
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

	/** @param date new value for {@link #date} */
	public void setDate(long date) {
		this.date = date;
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

	/**
	 * Gets the date at which this reminder was sent.
	 * @return {@link #date}
	 */
	public long getDate() {
		return this.date;
	}
	
//> GENERATED CODE
	/** @see java.lang.Object#hashCode() */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((content == null) ? 0 : content.hashCode());
		result = prime * result + (int) (date ^ (date >>> 32));
		result = prime * result
				+ ((recipients == null) ? 0 : recipients.hashCode());
		result = prime * result + ((subject == null) ? 0 : subject.hashCode());
		return result;
	}
	
	/** @see java.lang.Object#equals(java.lang.Object) */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Reminder other = (Reminder) obj;
		if (content == null) {
			if (other.content != null)
				return false;
		} else if (!content.equals(other.content))
			return false;
		if (date != other.date)
			return false;
		if (recipients == null) {
			if (other.recipients != null)
				return false;
		} else if (!recipients.equals(other.recipients))
			return false;
		if (subject == null) {
			if (other.subject != null)
				return false;
		} else if (!subject.equals(other.subject))
			return false;
		return true;
	}
}