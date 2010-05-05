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
