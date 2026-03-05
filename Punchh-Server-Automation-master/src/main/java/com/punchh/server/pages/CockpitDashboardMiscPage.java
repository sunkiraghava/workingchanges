package com.punchh.server.pages;

import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.MessagesConstants;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class CockpitDashboardMiscPage {

	static Logger logger = LogManager.getLogger(MenuPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	private PageObj pageObj;

	public CockpitDashboardMiscPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		pageObj = new PageObj(driver);
	}

	public void setBusinessTimezone(String timezone) {
		boolean isTimezoneSet = false;
		List<WebElement> timezoneList = utils.getLocatorList("cockpitDashboardPage.businessTimezoneList");
		logger.info("Business Timezone is: " + timezone);
		for (WebElement element : timezoneList) {
			String value = element.getText();
			if (value.contains(timezone)) {
				element.click();
				utils.waitTillCompletePageLoad();
				utils.getLocator("menuPage.updateInConfig").click();
				isTimezoneSet = true;
				logger.info("Business Timezone set to: " + value);
				TestListeners.extentTest.get().info("Business Timezone set to: " + value);
				break;
			}
		}
		if (!isTimezoneSet) {
			logger.error("Timezone not found in the list: " + timezone);
			TestListeners.extentTest.get().fail("Timezone not found in the list: " + timezone);
			throw new RuntimeException("Timezone not found in the list: " + timezone);
		}
		

	}
	
	public void selectOrderingVendor(String vendor)
	{
		WebElement vendorElement = utils.getLocator("cockpitPage.selectOrderingVendor");
		utils.scrollToElement(driver, vendorElement);
		vendorElement.click();
		List<WebElement> dropdownOptions = utils.getLocatorList("cockpitPage.vendorDropdownOptions");
		boolean isVendorSelected = false;
		for (WebElement option : dropdownOptions)
		{
			if (option.getText().equalsIgnoreCase(vendor))
			{
				option.click();
				isVendorSelected = true;
				break;
			}
		}
		if (!isVendorSelected) {
			logger.error("Vendor not found in the dropdown list: " + vendor);
			TestListeners.extentTest.get().fail("Vendor not found in the dropdown list: " + vendor);
			throw new RuntimeException("Vendor not found in the dropdown list: " + vendor);
		}
		
		utils.getLocator("cockpitPage.updateButton").click();
		String parVendor = utils.getLocator("cockpitPage.selectOrderingVendor").getAttribute("title");
		logger.info("Selected Par vendor Value ::" + parVendor) ;
		Assert.assertEquals(parVendor, vendor, "Ordering Vendor DropDown value not selected as expected");		
	}
    public void selectPasswordPolicy(String policy) {
        utils.waitTillPagePaceDone();
		pageObj.dashboardpage().navigateToTabs("Basic Authentication");
		utils.waitTillPagePaceDone();
        WebElement passwordPolicy = utils.getLocator("cockpitDashboardPage.passwordPolicy");
        utils.selectDrpDwnValue(passwordPolicy, policy);
		utils.getLocator("GISConfigurationPage.ba_updateButton").click();
		utils.longWaitInSeconds(2);
        Assert.assertEquals(utils.getSuccessMessage(), MessagesConstants.guestIdentityConfigUpdateSuccessMsg);
        logger.info("Password policy set to: " + policy);
        TestListeners.extentTest.get().info("Password policy set to: " + policy);
    }

    public String getSelectedNumericFieldValues(String fieldName) {
        String numericField = utils.getLocatorValue("cockpitDashboardPage.expirationFields").replace("$fieldName", fieldName);
        WebElement numericFieldValue = driver.findElement(By.xpath(numericField));
        String value = numericFieldValue.getAttribute("value");
        logger.info("Numeric field value for " + fieldName + " is: " + value);
        return value;
    }
    public void setSelectedNumericFieldValues(String fieldName,String value) {
        String numericField = utils.getLocatorValue("cockpitDashboardPage.expirationFields").replace("$fieldName", fieldName);
        WebElement numericFieldValue = driver.findElement(By.xpath(numericField));
        numericFieldValue.clear();
        numericFieldValue.sendKeys(value);
        utils.getLocator("menuPage.updateInConfig").click();
        utils.logit("Numeric field value for " + fieldName + " set to: " + value);
    }
}