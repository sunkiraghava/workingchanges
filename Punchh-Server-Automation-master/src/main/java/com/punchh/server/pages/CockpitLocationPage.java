package com.punchh.server.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.formula.atp.Switch;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class CockpitLocationPage {

	// Author:Rajasekhar

	static Logger logger = LogManager.getLogger(MenuPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	private PageObj pageObj;

	public CockpitLocationPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		pageObj = new PageObj(driver);
	}

	public void verifyAdditonalURLs() {

		utils.getLocator("cockpitLocationPage.Additionalurltextfield").isDisplayed();
		logger.info("Addition url field was exist");
		utils.getLocator("cockpitLocationPage.Additionalurltextfield").sendKeys("Reservation");
		utils.getLocator("cockpitLocationPage.updateBtn").click();
		utils.waitTillPagePaceDone();
	}

	public void SelectOnlineOrderDefaultLocation(String option){
		WebElement dropdownElement = utils.getLocator("cockpitLocationPage.OnlineOrderDefaultLocation");
		utils.waitTillElementToBeClickable(dropdownElement);
		Select dropdown = new Select(dropdownElement);
		dropdown.selectByVisibleText(option);
		utils.getLocator("cockpitLocationPage.updateBtn").click();
		logger.info("Selected the option -- " + option);
		TestListeners.extentTest.get().info("Selected the option -- " + option);
	}
	
	public void SelectHQLocation(String option){
		WebElement dropdownElement = utils.getLocator("cockpitLocationPage.HQLocation");
		utils.waitTillElementToBeClickable(dropdownElement);
		Select dropdown = new Select(dropdownElement);
		dropdown.selectByVisibleText(option);
		utils.getLocator("cockpitLocationPage.updateBtn").click();
		logger.info("Selected the option -- " + option + " from HQ Location dropdown");
		TestListeners.extentTest.get().info("Selected the option -- " + option + " from HQ Location dropdown");
	}

	public static boolean isValidUUID(String uuid) {
		String UUID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
		// create the pattern object, which will be used to match the regex
		Pattern UUID_PATTERN = Pattern.compile(UUID_REGEX);
		if (uuid == null) {
			return false;
		}
		//create a Matcher object that will be used to perform the matching
		Matcher matcher = UUID_PATTERN.matcher(uuid);
		return matcher.matches(); // This performs the actual matching of the input string (uuid) with the regular expression pattern
	}

	// Select Geomodifier Yext location mapping
	public void selectGeomodifierForYextLocationMapping(String option) {
		WebElement dropdownElement = utils.getLocator("cockpitLocationPage.YextLocationMapping");
		utils.waitTillElementToBeClickable(dropdownElement);
		Select dropdown = new Select(dropdownElement);
		dropdown.selectByVisibleText(option);
		utils.getLocator("cockpitLocationPage.updateBtn").click();
		logger.info("Selected the option -- " + option + " from Yext Location Mapping");
		TestListeners.extentTest.get().info("Selected the option -- " + option + " from Yext Location Mapping");
	}

	// Deselect Geomodifier Yext location mapping
	public void deSelectGeomodifierForYextLocationMapping() {
		try {
			WebElement crossElement = utils.getLocator("cockpitLocationPage.deSelectYextLocationMapping");
			crossElement.click();

			utils.getLocator("cockpitLocationPage.clickDropdownOnYextLocationMapping").click();

			utils.getLocator("cockpitLocationPage.updateBtn").click();

		} catch (Exception e) {
			logger.error("Exception occurred while deselecting yext location mapping: " + e.getMessage());
		}
		logger.info("yext location mapping is deselected");
		TestListeners.extentTest.get().info("yext location mapping is deselected");
	}

}// end of class
