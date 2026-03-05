package com.punchh.server.pages;

import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;
import org.testng.annotations.Listeners;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class CockpitEarningPage {

	static Logger logger = LogManager.getLogger(MenuPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;

	public CockpitEarningPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
	}

	public void setCheckinExpiry(String checkinexpiry) {
		clickCheckinExpiry();
		utils.getLocator("cockpitEarningPage.expiresAfterBox").clear();
		utils.getLocator("cockpitEarningPage.expiresAfterBox").sendKeys(checkinexpiry);
		clickUpdateBtn();
	}

	public void clickCheckinExpiry() {
		utils.getLocator("cockpitEarningPage.checkinExpiryBtn").click();
	}

	public void clickUpdateBtn() {
		utils.getLocator("cockpitEarningPage.updateBtn").click();
		utils.waitTillCompletePageLoad();
	}

	public String editExpiresAfterField(String str) {
		String oldFieldVal = utils.getLocator("cockpitEarningPage.expiresAfterField").getAttribute("value");
		utils.getLocator("cockpitEarningPage.expiresAfterField").clear();
		utils.getLocator("cockpitEarningPage.expiresAfterField").sendKeys(str);
		return oldFieldVal;
	}

	public void selectAccountReEvaluationStrategyVal(String visibleText) {
		String xpath = utils.getLocatorValue("cockpitEarningPage.accountReevaluationDrpDown");
		Select select1 = new Select(driver.findElement(By.xpath(xpath)));
		select1.selectByVisibleText(visibleText);
	}

	public void refreshPage() {
		utils.refreshPage();
		utils.waitTillCompletePageLoad();
	}

	public String getInactiveDaysField() {
		String val = utils.getLocator("cockpitEarningPage.inactiveDaysField").getAttribute("value");
		return val;
	}

}
