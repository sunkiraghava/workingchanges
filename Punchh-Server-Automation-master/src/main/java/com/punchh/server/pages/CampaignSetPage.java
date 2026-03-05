package com.punchh.server.pages;

import java.util.Map;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Listeners;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class CampaignSetPage {

	static Logger logger = LogManager.getLogger(CampaignSetPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;

	private Map<String, By> locators;

	public CampaignSetPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);

		locators = utils.getAllByMap();
	}

	public void clickNewCampaignSetBtn() {
		WebElement newCampaignSetButton = driver.findElement(locators.get("campaignSetPage.newCampaignSetBtn"));
		newCampaignSetButton.click();
	}

	public void setCampaignName(String name) {
		WebElement campaignNameBox = driver.findElement(locators.get("campaignSetPage.nameBox"));
		campaignNameBox.clear();
		campaignNameBox.sendKeys(name);
	}

	public void selectCampaignOne(String name) {
		WebElement campaignOneDropdown = driver.findElement(locators.get("campaignSetPage.campaignOneDrp"));
		campaignOneDropdown.click();
		WebElement textbox = driver.findElement(By.xpath("//input[@role='textbox']"));
		textbox.sendKeys(name);
		textbox.sendKeys(Keys.ENTER);
	}

	public void selectCampaignTwo(String name) {
		WebElement campaignTwoDropdown = driver.findElement(locators.get("campaignSetPage.campaignTwoDrp"));
		campaignTwoDropdown.click();
		WebElement textbox = driver.findElement(By.xpath("//input[@role='textbox']"));
		textbox.sendKeys(name);
		textbox.sendKeys(Keys.ENTER);
	}

	public void clickSaveandPreviewBtn() {
		WebElement saveAndPreviewButton = driver.findElement(locators.get("campaignSetPage.saveAndPreviewBtn"));
		saveAndPreviewButton.click();
	}
}
