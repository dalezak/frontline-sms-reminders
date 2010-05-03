package net.frontlinesms.plugins.reminders;

import net.frontlinesms.FrontlineSMS;
import net.frontlinesms.plugins.BasePluginController;
import net.frontlinesms.plugins.PluginControllerProperties;
import net.frontlinesms.plugins.PluginInitialisationException;
import net.frontlinesms.ui.UiGeneratorController;

import org.springframework.context.ApplicationContext;

@PluginControllerProperties(name = "Reminders Plugin", iconPath = "/icons/big_reminders.png", springConfigLocation = "classpath:net/frontlinesms/plugins/reminders/reminders-spring-hibernate.xml", hibernateConfigPath = "classpath:net/frontlinesms/plugins/reminders/reminders.hibernate.cfg.xml")
public class RemindersPluginController extends BasePluginController {

	private ApplicationContext applicationContext;
	private FrontlineSMS frontlineController;
	
	protected Object initThinletTab(UiGeneratorController uiController) {
		RemindersThinletTabController tabController = new RemindersThinletTabController(this, uiController, applicationContext);
		tabController.setFrontline(this.frontlineController);
		return tabController.getTab();
	}

	public void deinit() {

	}

	public void init(FrontlineSMS frontlineController, ApplicationContext applicationContext) throws PluginInitialisationException {
		this.applicationContext = applicationContext;
		this.frontlineController = frontlineController;
	}

}
