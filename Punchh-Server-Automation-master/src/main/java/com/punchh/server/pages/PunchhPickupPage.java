package com.punchh.server.pages;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class PunchhPickupPage {
	static Logger logger = LogManager.getLogger(MenuPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;

	public PunchhPickupPage(WebDriver driver) {
		this.driver = driver;
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
	}

	public void clickOnMobileWeb(String email) {
		utils.getLocator("punchhPickupPage.pickupheading").isDisplayed();
		utils.getLocator("punchhPickupPage.mobileweblabel").isDisplayed();
		utils.getLocator("punchhPickupPage.mobileweblabel").click();
		utils.getLocator("punchhPickupPage.privacypolicy").isDisplayed();
		utils.getLocator("punchhPickupPage.privacypolicy").clear();
		utils.getLocator("punchhPickupPage.privacypolicy").sendKeys(email);
	}

//	public void clickonupdate() {
//		utils.getLocator("punchhPickupPage.pickupupdatebutton").click();
//		utils.getLocator("punchhPickupPage.privacyupdatelabel").isDisplayed();
//	}
}
