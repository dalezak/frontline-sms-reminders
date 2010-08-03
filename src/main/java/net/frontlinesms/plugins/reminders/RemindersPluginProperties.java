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

import net.frontlinesms.resources.UserHomeFilePropertySet;

/**
 * RemindersPluginProperties
 * @author dalezak
 *
 */
public class RemindersPluginProperties extends UserHomeFilePropertySet {

	private static final String EMAIL_ACCOUNT = "email.account";
	
	private static RemindersPluginProperties instance;
	
	public RemindersPluginProperties() {
		super("reminders");
	}
	
	private static synchronized RemindersPluginProperties getInstance() {
		if (instance == null) {
			instance = new RemindersPluginProperties();
		}
		return instance;
	}
	
	/**
	 * Get EmailAccount
	 * @return EmailAccount
	 */
	public static String getEmailAccount() {
		return getInstance().getProperty(EMAIL_ACCOUNT);
	}
	
	/**
	 * Set EmailAccount
	 * @param emailAccount EmailAccount
	 */
	public static void setEmailAccount(String emailAccount) {
		if (emailAccount != null) {
			getInstance().setProperty(EMAIL_ACCOUNT, emailAccount);
		}
		else {
			getInstance().setProperty(EMAIL_ACCOUNT, null);
		}
		getInstance().saveToDisk();
	}
	
}