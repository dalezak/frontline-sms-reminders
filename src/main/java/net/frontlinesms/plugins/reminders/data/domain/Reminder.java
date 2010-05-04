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
	
	public enum Occurrence {
		/** Occuring Once */
		ONCE,
		/** Occuring Hourly */
		HOURLY,
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
	
	/** Start Date of the reminder */
//	@Column(name=COLUMN_STARTDATE)
	private long startdate;

	/** End Date of the reminder */
//	@Column(name=COLUMN_ENDDATE)
	private long enddate;
	
	/** Recipient of the reminder */
	@Column(name=COLUMN_RECIPIENTS)
	private String recipients;
	
	/** Occurrence of the reminder */
	@Column(name=COLUMN_OCCURRENCE)
	private Occurrence occurrence;

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
	public Reminder(long startdate, long enddate, Type type, String recipients, String subject, String content, Occurrence occurrence) {
		this.type = type;
		this.startdate = startdate;
		this.enddate = enddate;
		this.recipients = recipients;
		this.subject = subject;
		this.content = content;
		this.occurrence = occurrence;
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
	 * sets the occurrence of this Reminder.  Should be one of the Reminder.OCCURRENCE_ constants.
	 * @param occurrence new value for {@link #occurrence}
	 */
	public void setOccurrence(Occurrence occurrence) {
		this.occurrence = occurrence;
	}

	/**
	 * Gets the Occurrence of this Reminder.  Should be one of the Reminder.OCCURRENCE_ constants.
	 * @return {@link #occurrence}
	 */
	public Occurrence getOccurrence() {
		return this.occurrence;
	}
	
	public String getOccurrenceLabel() {
		if (this.getOccurrence() == Reminder.Occurrence.ONCE) {
			//TODO change to property file
			return "Once";
		}
		else if (this.getOccurrence() == Reminder.Occurrence.HOURLY) {
			//TODO change to property file
			return "Hourly";
		}
		else if (this.getOccurrence() == Reminder.Occurrence.DAILY) {
			//TODO change to property file
			return "Daily";
		}
		else if (this.getOccurrence() == Reminder.Occurrence.WEEKLY) {
			//TODO change to property file
			return "Weekly";
		}
		else if (this.getOccurrence() == Reminder.Occurrence.MONTHLY) {
			//TODO change to property file
			return "Monthly";
		}
		else {
			//TODO change to property file
			return "Once";
		}
	}
	
	public static Occurrence getOccurrenceForIndex(int index) {
		switch(index) {
			case 0 : return Occurrence.ONCE;
			case 1 : return Occurrence.HOURLY;
			case 2 : return Occurrence.DAILY;
			case 3 : return Occurrence.WEEKLY;
			case 4 : return Occurrence.MONTHLY;
			default: return Occurrence.ONCE;
		}
	}
	
	public static int getIndexForOccurrence(Occurrence occurrence) {
		if (occurrence == Occurrence.ONCE) return 0;
		if (occurrence == Occurrence.HOURLY) return 1;
		if (occurrence == Occurrence.DAILY) return 2;
		if (occurrence == Occurrence.WEEKLY) return 3;
		if (occurrence == Occurrence.MONTHLY) return 4;
		return 0;
	}
	
	public int getOccurrenceIndex() {
		return Reminder.getIndexForOccurrence(this.occurrence);
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
		if (occurrence != other.occurrence) return false;
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