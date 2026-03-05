package com.punchh.server.pages;

import java.util.List;
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
public class RedemptionsPage {
	static Logger logger = LogManager.getLogger(RedemptionsPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	String ssoEmail, temp = "temp";
	String scheduleName;

	public RedemptionsPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(this.driver);
		selUtils = new SeleniumUtilities(driver);
	}

	public void verifyEnableUUIDStrategyForOnlineOrderingChannel() {
		// turn checkbox on
		utils.getLocator("redemptionsPage.redemptionCodesBtn").click();
		utils.checkUncheckFlag("Enable UUID Strategy for Online Ordering Channel", "check");
		utils.getLocator("redemptionsPage.updateBtn").click();
		// turn checkbox off
		utils.getLocator("redemptionsPage.redemptionCodesBtn").click();
		utils.checkUncheckFlag("Enable UUID Strategy for Online Ordering Channel", "uncheck");
		utils.getLocator("redemptionsPage.updateBtn").click();

	}

	public int verifyInteroperabilityExclusions(){
		// this method is used to verify the number of interoperability exclusions
		selUtils.waitTillElementToBeClickable(utils.getLocator("redemptionsPage.multipleRedemptionsTab"));
		utils.getLocator("redemptionsPage.multipleRedemptionsTab").click();
		String xpath = utils.getLocatorValue("redemptionsPage.interoperabilityExclusions");
		List<WebElement> elements = driver.findElements(By.xpath(xpath));
		return elements.size();

	}
	public void selectCalendarRangeType(String calendarType) {
		// this method is used to select the calendar range type
		selUtils.waitTillElementToBeClickable(utils.getLocator("redemptionsPage.clickCalendar"));
		utils.getLocator("redemptionsPage.clickCalendar").click();
		String xpath = utils.getLocatorValue("redemptionsPage.calendarRange").replace("$calendarType", calendarType);
		driver.findElement(By.xpath(xpath)).click();
		logger.info("Selected calendar type: " + calendarType);
		TestListeners.extentTest.get().info("Selected calendar type: " + calendarType);

	}
	public void searchRedemptionCode(String redemptionCode) {
		// this method is used to search for a redemption code
		WebElement searchBox = utils.getLocator("redemptionsPage.searchRedemptionCode");
		searchBox.sendKeys(redemptionCode);
		searchBox.sendKeys(Keys.ENTER);
		logger.info("Searched for redemption code: " + redemptionCode);
		TestListeners.extentTest.get().info("Searched for redemption code: " + redemptionCode);
	}
	public String getRedemptionCodeStatus(String redemptionCode) {
		// this method is used to get the redemption code status from the page
		utils.waitTillPagePaceDone();
		String xpath = utils.getLocatorValue("redemptionsPage.redemptionCodeStatus").replace("$redemptionCode", redemptionCode);
		WebElement statusElement = driver.findElement(By.xpath(xpath));
        return statusElement.getText();
	}

}
