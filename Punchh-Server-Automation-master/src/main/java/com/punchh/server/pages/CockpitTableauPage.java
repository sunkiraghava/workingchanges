package com.punchh.server.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class CockpitTableauPage {
	static Logger logger = LogManager.getLogger(CockpitTableauPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;

	public CockpitTableauPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(this.driver);
		selUtils = new SeleniumUtilities(driver);
	}

	public void goToCockpitTableau() {
		logger.info("Navigated to Tableau analytics page");
		TestListeners.extentTest.get().info("Navigated to Tableau analytics page");
		utils.getLocator("cockpitTableauPage.tableauReporting").click();
		logger.info("Navigated to Tableau reporting");
		TestListeners.extentTest.get().info("Navigated to Tableau reporting");
	}

	public void enterEmbeddedLinkInLiftReport(String text) {
		utils.waitTillPagePaceDone();
		utils.getLocator("cockpitTableauPage.liftReportLink").click();
		utils.getLocator("cockpitTableauPage.liftEmbeddedLink").clear();
		utils.getLocator("cockpitTableauPage.liftEmbeddedLink").sendKeys(text);
		logger.info("Link entered in the field :- " + text);
		TestListeners.extentTest.get().info("Link entered in the field  :- " + text);

	}

	public boolean validateDateAndLocationFields(String date) {
		boolean isDateRangeFieldValid = utils.getLocator("cockpitTableauPage.dateRangeField").isDisplayed();
		if (isDateRangeFieldValid) {
			logger.info("The date range field is present");
			TestListeners.extentTest.get().info("The date range field is present");

			utils.getLocator("cockpitTableauPage.dateRangeField").click();
			utils.getLocator("cockpitTableauPage.dateRangeField").clear();
			utils.getLocator("cockpitTableauPage.dateRangeField").sendKeys(date);
			utils.getLocator("cockpitTableauPage.dateApplyButton").click();
		} else {
			logger.error("Date range field is not displayed.");
			TestListeners.extentTest.get().fail("Date range field is not displayed.");
		}

		boolean isLocationFieldValid = utils.getLocator("cockpitTableauPage.locationField").isDisplayed();

		if (isLocationFieldValid) {
			logger.info("The location field is present");
			TestListeners.extentTest.get().info("The location field is present");
		} else {
			logger.error("Location field is not displayed.");
			TestListeners.extentTest.get().fail("Location field is not displayed.");
		}
		return isDateRangeFieldValid && isLocationFieldValid;
	}

	public void navigateToLiftReports() {
		utils.getLocator("cockpitTableauPage.reports").click();
		utils.getLocator("cockpitTableauPage.liftReport").click();
		logger.info("Navigated to Lift Report");
		TestListeners.extentTest.get().info("Navigated to Lift Report");
	}

	public boolean getIndividualLocations() {
		driver.navigate().refresh();
		if (!utils.getLocator("cockpitTableauPage.locationField").isDisplayed()) {
			logger.error("Location field not displayed");
			return false;
		}
		utils.getLocator("cockpitTableauPage.locationField").click();
		List<WebElement> locationList = utils.getLocatorList("cockpitTableauPage.individualLocationList");
		List<String> individualLocationTexts = new ArrayList<>();

		if (locationList.isEmpty()) {
			logger.error("No locations found.");
			TestListeners.extentTest.get().fail("No location groups found.");
			Assert.fail("No locations found.");
			return false;
		} else {
			for (WebElement location : locationList) {
				String locationText = location.getText();
				individualLocationTexts.add(locationText);
				logger.info("Individual Location: " + locationText);
				TestListeners.extentTest.get().info(locationText);
			}
		}
		return true;
	}

	public boolean getLocationGroups() {
		driver.navigate().refresh();
		if (!utils.getLocator("cockpitTableauPage.locationField").isDisplayed()) {
			logger.error("Location field not displayed");
			return false;
		}
		utils.getLocator("cockpitTableauPage.locationField").click();
		List<WebElement> locationList = utils.getLocatorList("cockpitTableauPage.locationGroupList");
		List<String> individualLocationTexts = new ArrayList<>();

		if (locationList.isEmpty()) {
			logger.error("No location groups found.");
			TestListeners.extentTest.get().fail("No location groups found.");
			Assert.fail("No location groups found.");
			return false;
		} else {
			for (WebElement location : locationList) {
				String locationText = location.getText();
				individualLocationTexts.add(locationText);
				logger.info("Location group list: " + locationText);
				TestListeners.extentTest.get().info(locationText);
			}
		}
		return true;
	}

	public boolean getNoLocation() {
		if (!utils.getLocator("cockpitTableauPage.locationField").isDisplayed()) {
			logger.error("Location field not displayed");
			return false;
		}
		if (!utils.getLocator("cockpitTableauPage.noLocationMessage").isDisplayed()) {
			logger.error("No location message not displayed");
			return false;
		}
		logger.info("You have access to no location is displayed");
		TestListeners.extentTest.get().info("You have access to no location is displayed");
		return true;
	}

	public boolean validateFieldsOfDumpsterDiversreport(String date) {
		boolean isDateRangeFieldValid = utils.getLocator("cockpitTableauPage.dateRangeField").isDisplayed();
		if (isDateRangeFieldValid) {
			logger.info("The date range field is present");
			TestListeners.extentTest.get().info("The date range field is present");
			utils.getLocator("cockpitTableauPage.dateRangeField").click();
			utils.getLocator("cockpitTableauPage.dateRangeField").clear();
			utils.getLocator("cockpitTableauPage.dateRangeField").sendKeys(date);
			utils.getLocator("cockpitTableauPage.dateApplyButton").click();
		} else {
			logger.error("Date range field is not displayed.");
			TestListeners.extentTest.get().fail("Date range field is not displayed.");
		}
		boolean isMinAttemptFieldValid = utils.getLocator("cockpitTableauPage.minAttemptField").isDisplayed();
		if (isMinAttemptFieldValid) {
			logger.info("Minimum Attempt field is present");
			TestListeners.extentTest.get().info("Minimum Attempt field is present");

			String minAttemptValue = utils.getLocator("cockpitTableauPage.minAttemptDefaultValue")
					.getAttribute("value");
			logger.info("Minimum Attempt Default Value: " + minAttemptValue);
			TestListeners.extentTest.get().info("Minimum Attempt Default Value: " + minAttemptValue);
		} else {
			logger.error("Minimum Attempt field is not displayed.");
			TestListeners.extentTest.get().fail("Minimum Attempt field is not displayed.");
		}
		boolean isMinBarcodeScanFieldValid = utils.getLocator("cockpitTableauPage.minBarcodeScanField").isDisplayed();
		if (isMinBarcodeScanFieldValid) {
			logger.info("Minimum Barcode Scan field is present");
			TestListeners.extentTest.get().info("Minimum Barcode Scan field is present");

			String minBarcodeScanValue = utils.getLocator("cockpitTableauPage.minbarcodeScanDefaultValue")
					.getAttribute("value");
			logger.info("Minimum Barcode Scan Default Value: " + minBarcodeScanValue);
			TestListeners.extentTest.get().info("Minimum Barcode Scan Default Value: " + minBarcodeScanValue);
		} else {
			logger.error("Minimum Barcode Scan field is not displayed.");
			TestListeners.extentTest.get().fail("Minimum Barcode Scan field is not displayed.");
		}

		return isDateRangeFieldValid && isMinAttemptFieldValid && isMinBarcodeScanFieldValid;
	}

	public void enterEmbeddedLinkInDumpsterDiversReport(String text) {
		utils.waitTillPagePaceDone();
		utils.getLocator("cockpitTableauPage.dumpsterReportLink").click();
		utils.getLocator("cockpitTableauPage.dumpsterEmbeddedLink").clear();
		utils.getLocator("cockpitTableauPage.dumpsterEmbeddedLink").sendKeys(text);
		logger.info("Link entered in the field :- " + text);
		TestListeners.extentTest.get().info("Link entered in the field  :- " + text);

	}

	public void enterEmbeddedLinkInLocationReport(String text) {
		utils.waitTillPagePaceDone();
		utils.getLocator("cockpitTableauPage.liftReportLink").click();
		utils.getLocator("cockpitTableauPage.liftEmbeddedLink").clear();
		utils.getLocator("cockpitTableauPage.liftEmbeddedLink").sendKeys(text);
		logger.info("Link entered in the field :- " + text);
		TestListeners.extentTest.get().info("Link entered in the field  :- " + text);

	}

	public void enterEmbeddedLinkInCouponReport(String text) {
		utils.waitTillPagePaceDone();
		utils.getLocator("cockpitTableauPage.couponReportLink").click();
		utils.getLocator("cockpitTableauPage.couponEmbeddedLink").clear();
		utils.getLocator("cockpitTableauPage.couponEmbeddedLink").sendKeys(text);
		logger.info("Link entered in the field :- " + text);
		TestListeners.extentTest.get().info("Link entered in the field  :- " + text);

	}

	public void enterEmbeddedLinkInPromotionalReport(String text) {
		utils.waitTillPagePaceDone();
		utils.getLocator("cockpitTableauPage.promotionalReportLink").click();
		utils.getLocator("cockpitTableauPage.promotionalEmbeddedLink").clear();
		utils.getLocator("cockpitTableauPage.promotionalEmbeddedLink").sendKeys(text);
		logger.info("Link entered in the field :- " + text);
		TestListeners.extentTest.get().info("Link entered in the field  :- " + text);

	}

	public boolean validateFieldsOfPromotionalReport(String date) {
		boolean isDateRangeFieldValid = utils.getLocator("cockpitTableauPage.dateRangeField").isDisplayed();
		if (isDateRangeFieldValid) {
			logger.info("The date range field is present");
			TestListeners.extentTest.get().info("The date range field is present");

			utils.getLocator("cockpitTableauPage.dateRangeField").click();
			utils.getLocator("cockpitTableauPage.dateRangeField").clear();
			utils.getLocator("cockpitTableauPage.dateRangeField").sendKeys(date);
			utils.getLocator("cockpitTableauPage.dateApplyButton").click();
		} else {
			logger.error("Date range field is not displayed.");
			TestListeners.extentTest.get().fail("Date range field is not displayed.");
		}

		boolean isGameFieldValid = utils.getLocator("cockpitTableauPage.gameFeild").isDisplayed();
		if (isGameFieldValid) {
			logger.info("Game field is present");
			TestListeners.extentTest.get().info("Game field is present");
		} else {
			logger.error("Game field is not displayed.");
			TestListeners.extentTest.get().fail("Game field is not displayed.");
		}

		return isDateRangeFieldValid && isGameFieldValid;
	}

	public void enterEmbeddedLinkInRedemptionReport(String text) {
		utils.waitTillPagePaceDone();
		utils.getLocator("cockpitTableauPage.redemptionReportLink").click();
		utils.getLocator("cockpitTableauPage.redemptionEmbeddedLink").clear();
		utils.getLocator("cockpitTableauPage.redemptionEmbeddedLink").sendKeys(text);
		logger.info("Link entered in the field :- " + text);
		TestListeners.extentTest.get().info("Link entered in the field  :- " + text);

	}

	public void navigateToPOSScoreboard(String slug) {
		String baseUrl = "https://dashboard.staging.punchh.io";
		String posScoreboardUrl = baseUrl + "/businesses/" + slug + "/pos_scoreboard";
		driver.get(posScoreboardUrl);
		utils.waitTillPagePaceDone();
	}

	public void verifyPOSScoreboardPage() {
		// Wait until the heading is visible
		utils.waitTillPagePaceDone();
		boolean heading = utils.getLocator("cockpitTableauPage.pOSScoreboardHeading").isDisplayed();
		logger.info("POS Scoreboard page is displayed: " + heading);
		Assert.assertTrue(heading, "POS Scoreboard page is not displayed");

		// Verify Export POS Scoreboard button
		boolean exportPOSScoreboard = utils.getLocator("cockpitTableauPage.exportPOSScoreboard").isDisplayed();
		Assert.assertTrue(exportPOSScoreboard, "Export POS Scoreboard is not displayed");
		utils.getLocator("cockpitTableauPage.exportPOSScoreboard").click();
		utils.waitTillPagePaceDone();
		boolean posScoreboardMessageDisplayed = utils.getLocator("cockpitTableauPage.pOSScoreboardMessage")
				.isDisplayed();
		logger.info("POS Scoreboard confirmation message is displayed: ");
		Assert.assertTrue(posScoreboardMessageDisplayed, "POS Scoreboard confirmation message is not displayed");
	}
}
