package com.punchh.server.pages;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Listeners;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class DestructionPage {

	static Logger logger = LogManager.getLogger(DestructionPage.class);
	private WebDriver driver;
	Utilities utils;
	SeleniumUtilities selUtils;
	// private PageObj pageObj;

	public DestructionPage(WebDriver driver) {
		this.driver = driver;
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
	}

	public String resetAllMassCampaigns(String name) {
		WebElement slugName = utils.getLocator("destructionPage.businessSlugbox");
		slugName.sendKeys(name);
		utils.getLocator("destructionPage.resetButton").click();
		logger.info("Entered slug name and clicked reset Button");
		TestListeners.extentTest.get().info("Entered slug name and clicked reset Button");
		String msg = utils.getSuccessMessage();
		return msg;

	}
}