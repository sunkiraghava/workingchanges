package com.punchh.server.pages;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

public class PosStatsPage {

	static Logger logger = LogManager.getLogger(PosStatsPage.class);
	private WebDriver driver;
	Utilities utils;
	SeleniumUtilities selUtils;

	public PosStatsPage(WebDriver driver) {
		this.driver = driver;
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
	}

	public String checkStoreNumber() {
		utils.getLocator("posStatsPage.posScoreboardBtn").click();
		String val = utils.getLocator("posStatsPage.storeNumber").getText().trim();
		TestListeners.extentTest.get().info("Store Number is => " + val);
		return val;
	}

	public String checkStoreId() {
		String val = utils.getLocator("posStatsPage.storeID").getText().trim();
		utils.getLocator("posStatsPage.storeID").click();
		TestListeners.extentTest.get().info("Store Id is => " + val);
		return val;
	}

	public String checkEditStorePage() {
		String val = utils.getLocator("posStatsPage.editLocationPage").getText().trim();
		return val;
	}

}
