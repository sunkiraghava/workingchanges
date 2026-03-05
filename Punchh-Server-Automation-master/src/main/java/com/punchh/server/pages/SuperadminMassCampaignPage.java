package com.punchh.server.pages;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class SuperadminMassCampaignPage {

	
	static Logger logger = LogManager.getLogger(SuperadminMassCampaignPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	CreateDateTime createDateTime;

	public SuperadminMassCampaignPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		createDateTime = new CreateDateTime();
	}
	
	public void selectCamapign(String campaignname) {
		
		int attempts = 0;
		while (attempts <= 15) {
			try {
				utils.implicitWait(2);
				utils.longWaitInSeconds(2);
				WebElement massCampaign = driver.findElement(
						By.xpath(utils.getLocatorValue("superadminMassCampaignPage.massCampaignLink").replace("$temp", campaignname)));
				String str = massCampaign.getText();
				if (str.equalsIgnoreCase(campaignname)) {
					logger.info("Campaign Name " + campaignname + " matched on the superadmin page");
					TestListeners.extentTest.get().pass("Campaign Name " + campaignname + " matched on the superadmin page");
					massCampaign.click();
					logger.info("Clicked On :" + campaignname);
					TestListeners.extentTest.get().info("Clicked On :" + campaignname);
					break;
				}
			} catch (Exception e) {
				logger.info(
						"Element is not present or Campaign Name did not matched... polling count is : " + attempts);
				TestListeners.extentTest.get().info(
						"Element is not present or Campaign Name did not matched... polling count is : " + attempts);
				utils.refreshPage();
			}
			attempts++;
		}
		utils.implicitWait(60);
	}
	
	public String getCampaignURL() {
		String url = driver.getCurrentUrl();
		logger.info("Got campaign URl :" + url);
		TestListeners.extentTest.get().info("Got campaign URl :" + url);
		return url;
	}
	
	public void openCampaignURL(String url) {
		driver.get(url);
		utils.longWaitInSeconds(1);
		logger.info("Opened campaign URl :" + url);
		TestListeners.extentTest.get().info("Opened campaign URl :" + url);
	}
	
	public void selectTab(String tab) {
		WebElement superadminTab = driver.findElement(
				By.xpath(utils.getLocatorValue("superadminMassCampaignPage.superadminTabs").replace("$temp", tab)));
		superadminTab.click();
		utils.waitTillPagePaceDone();
		logger.info("Clicked On :" + tab+ " on superadmin page");
		TestListeners.extentTest.get().info("Clicked On :" + tab+ " on superadmin page");
	}
	
	
}
