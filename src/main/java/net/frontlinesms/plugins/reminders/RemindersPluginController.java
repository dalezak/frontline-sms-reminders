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

import net.frontlinesms.FrontlineSMS;
import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.plugins.BasePluginController;
import net.frontlinesms.plugins.PluginControllerProperties;
import net.frontlinesms.plugins.PluginInitialisationException;
import net.frontlinesms.plugins.reminders.data.domain.Reminder;
import net.frontlinesms.plugins.reminders.data.repository.ReminderDao;
import net.frontlinesms.ui.UiGeneratorController;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.springframework.context.ApplicationContext;

/*
 * RemindersPluginController
 * @author Dale Zak
 * 
 * see {@link "http://www.frontlinesms.net"} for more details. 
 * copyright owned by Kiwanja.net
 */
@PluginControllerProperties(name = "Reminders Plugin", iconPath = "/icons/big_reminders.png", springConfigLocation = "classpath:net/frontlinesms/plugins/reminders/reminders-spring-hibernate.xml", hibernateConfigPath = "classpath:net/frontlinesms/plugins/reminders/reminders.hibernate.cfg.xml")
public class RemindersPluginController extends BasePluginController {

	private static Logger LOG = FrontlineUtils.getLogger(RemindersPluginController.class);
	
	private ApplicationContext applicationContext;
	private FrontlineSMS frontlineController;
	private RemindersThinletTabController tabController;
	private ReminderDao reminderDao;
	
	protected Object initThinletTab(UiGeneratorController uiController) {
		LOG.debug("initThinletTab");
		this.tabController = new RemindersThinletTabController(this, uiController, applicationContext);
		this.tabController.setFrontline(this.frontlineController);
		Reminder.setCallback(this.tabController);
		return this.tabController.getTab();
	}

	public void deinit() {
		LOG.debug("deinit");
		if (this.tabController != null) {
			this.tabController.cancelAllReminders();
		}
	}

	public void init(FrontlineSMS frontlineController, ApplicationContext applicationContext) throws PluginInitialisationException {
		//Uncomment the following line to enable basic logging to console
		//BasicConfigurator.configure();
		LOG.debug("init");
		this.applicationContext = applicationContext;
		this.frontlineController = frontlineController;
		this.reminderDao = (ReminderDao) applicationContext.getBean("reminderDao");
		for (Reminder reminder : this.reminderDao.getPendingReminders()) {
			reminder.scheduleReminder();
		}
	}
}
