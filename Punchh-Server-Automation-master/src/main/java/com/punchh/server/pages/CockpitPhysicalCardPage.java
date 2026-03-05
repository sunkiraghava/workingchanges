package com.punchh.server.pages;

import java.util.List;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import net.minidev.json.JSONValue;


@Listeners(TestListeners.class)
public class CockpitPhysicalCardPage {

	static Logger logger = LogManager.getLogger(MenuPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;

	public CockpitPhysicalCardPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
	}
	
	public void selectLoyaltyCardAdapter(String adapter, String length) {
		utils.getLocator("physicalCardPage.loylatyCardAdapterDrp").click();
		utils.getLocator("physicalCardPage.loylatyCardAdapterSearch").clear();
		utils.getLocator("physicalCardPage.loylatyCardAdapterSearch").sendKeys(adapter);
		utils.getLocator("physicalCardPage.searchedAdapter").click();
		if ("Zipline Loyalty Cards".equals(adapter) || "External Vendor Loyalty Cards".equals(adapter)) {
		    selectLoyaltyCardLength(length);
		    utils.getLocator("physicalCardPage.loyaltyCardEnableUnmasking").click();
		}
		utils.getLocator("physicalCardPage.loyaltyCardEnableUnmasking").click();	
		utils.getLocator("physicalCardPage.saveBtn").click();
		logger.info("Loyalty Card Adapter is Selected as: " + adapter);
		TestListeners.extentTest.get().pass("Loyalty Card Adapter is Selected as: " + adapter);
	}
	
	public void selectLoyaltyCardLength(String length) {
		utils.getLocator("physicalCardPage.loyaltyCardLength").clear();
		utils.getLocator("physicalCardPage.loyaltyCardLength").sendKeys(length);
	}

}
