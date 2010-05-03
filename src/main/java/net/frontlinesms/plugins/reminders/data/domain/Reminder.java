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
	/** Column name for {@link #occurance} */
	private static final String COLUMN_OCCURANCE = "occurance";
	
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
		CONTENT(COLUMN_CONTENT),
		/** field mapping for {@link Reminder#occurance} */
		OCCURANCE(COLUMN_OCCURANCE);
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
	
	public enum Occurance {
		/** Occuring Once */
		ONCE,
		/** Occuring Daily */
		DAILY,
		/** Occuring Weekly */
		WEEKLY,
		/** Occuring Monthly */
		MONTHLY
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
	
	/** Occurance of the reminder */
	@Column(name=COLUMN_OCCURANCE)
	private Occurance occurance;

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
	public Reminder(long date, Type type, String recipients, String subject, String content, Occurance occurance) {
		this.type = type;
		this.date = date;
		this.recipients = recipients;
		this.subject = subject;
		this.content = content;
		this.occurance = occurance;
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
	 * sets the occurance of this Reminder.  Should be one of the Reminder.OCCURANCE_ constants.
	 * @param occurance new value for {@link #occurance}
	 */
	public void setOccurance(Occurance occurance) {
		this.occurance = occurance;
	}


	/**
	 * Gets the occurance of this Reminder.  Should be one of the Reminder.OCCURANCE_ constants.
	 * @return {@link #occurance}
	 */
	public Occurance getOccurance() {
		return this.occurance;
	}
	
	public String getOccuranceLabel() {
		if (this.getOccurance() == Reminder.Occurance.ONCE) {
			return "Once";
		}
		else if (this.getOccurance() == Reminder.Occurance.DAILY) {
			return "Daily";
		}
		else if (this.getOccurance() == Reminder.Occurance.WEEKLY) {
			return "Weekly";
		}
		else if (this.getOccurance() == Reminder.Occurance.MONTHLY) {
			return "Monthly";
		}
		else {
			return "Once";
		}
	}
	
	public static Occurance getOccuranceForIndex(int index) {
		switch(index) {
			case 0 : return Occurance.ONCE;
			case 1 : return Occurance.DAILY;
			case 2 : return Occurance.WEEKLY;
			case 3 : return Occurance.MONTHLY;
			default: return Occurance.ONCE;
		}
	}
	
	public static int getIndexForOccurance(Occurance occurance) {
		if (occurance == Occurance.ONCE) return 0;
		if (occurance == Occurance.DAILY) return 1;
		if (occurance == Occurance.WEEKLY) return 2;
		if (occurance == Occurance.WEEKLY) return 3;
		return 0;
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