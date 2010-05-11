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

import java.util.Timer;
import java.util.TimerTask;

import net.frontlinesms.FrontlineSMS;
import net.frontlinesms.Utils;
import net.frontlinesms.plugins.BasePluginController;
import net.frontlinesms.plugins.PluginControllerProperties;
import net.frontlinesms.plugins.PluginInitialisationException;
import net.frontlinesms.plugins.reminders.data.repository.ReminderDao;
import net.frontlinesms.ui.UiGeneratorController;

import org.apache.log4j.Logger;
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

	private static Logger LOG = Utils.getLogger(RemindersPluginController.class);
	
	private ApplicationContext applicationContext;
	private FrontlineSMS frontlineController;
	private RemindersThinletTabController tabController;
	private ReminderDao reminderDao;
	
	private Timer timer;
	private TimerTask timerTask;
	
	protected Object initThinletTab(UiGeneratorController uiController) {
		this.tabController = new RemindersThinletTabController(this, uiController, applicationContext);
		this.tabController.setFrontline(this.frontlineController);
		startTimerTask();
		return this.tabController.getTab();
	}

	public void deinit() {
		LOG.trace("deinit");
	}

	public void init(FrontlineSMS frontlineController, ApplicationContext applicationContext) throws PluginInitialisationException {
		this.applicationContext = applicationContext;
		this.frontlineController = frontlineController;
		this.reminderDao = (ReminderDao) applicationContext.getBean("reminderDao");
	}

	private void startTimerTask() {
		int delay = 5000;   // delay for five seconds
		int period = 1000 * 60;  // repeat every minute
		this.timer = new Timer();
		this.timerTask = new ReminderTimerTask(this.tabController, this.reminderDao);
		this.timer.schedule(this.timerTask, delay, period);
	}
	
}
