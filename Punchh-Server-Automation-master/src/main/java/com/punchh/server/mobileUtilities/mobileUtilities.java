package com.punchh.server.mobileUtilities;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Listeners;

import com.jayway.jsonpath.JsonPath;
import com.punchh.server.utilities.TestListeners;

import io.appium.java_client.ios.IOSDriver;

@Listeners(TestListeners.class)
public class mobileUtilities {

	static Logger logger = LogManager.getLogger(mobileUtilities.class);
	private IOSDriver driver;

	public mobileUtilities(IOSDriver driver) {
		this.driver = driver;
	}

	public WebElement getLocator(String locator) {
		String tempLocator, locatorType = null;
		File jsonFile;
		try {
			jsonFile = new File(System.getProperty("user.dir") + "//resources//Locators//mobileObjRepository.json");
			String temploc = ((String) JsonPath.read(jsonFile, "$." + locator));
			if (temploc.contains(";")) {
				String loc[] = temploc.split(";");
				locatorType = loc[0];
				tempLocator = loc[1];
			} else {
				tempLocator = temploc;
				locatorType = "";
			}
			if (locatorType.equalsIgnoreCase("xpath") || tempLocator.contains("//")) {
				return driver.findElement(By.xpath(tempLocator));
			} else if (locatorType.equalsIgnoreCase("id")) {
				return driver.findElement(By.id(tempLocator));
			} else if (locatorType.equalsIgnoreCase("name")) {
				return driver.findElement(By.name(tempLocator));
			} else if (locatorType.equalsIgnoreCase("css")) {
				return driver.findElement(By.cssSelector(tempLocator));
			} else if (locatorType.equalsIgnoreCase("text")) {
				return driver.findElement(By.linkText(tempLocator));
			} else if (locatorType.equalsIgnoreCase("class")) {
				return driver.findElement(By.className(tempLocator));
			} else if (locatorType.equalsIgnoreCase("tag")) {
				return driver.findElement(By.tagName(tempLocator));
			} else if (locatorType.equalsIgnoreCase("partialText")) {
				return driver.findElement(By.partialLinkText(tempLocator));
			}
		} catch (IOException e) {
			TestListeners.extentTest.get().fail("Error while getting locator: " + e);
			logger.error("Error while getting locator: " + e);
		}
		return null;
	}

	public List<WebElement> getLocatorList(String locator) {
		String tempLocator, locatorType = null;
		File jsonFile;
		try {
			jsonFile = new File(System.getProperty("user.dir") + "//resources//Locators//mobileObjRepository.json");
			String temploc = ((String) JsonPath.read(jsonFile, "$." + locator));
			if (temploc.contains(";")) {
				String loc[] = temploc.split(";");
				locatorType = loc[0];
				tempLocator = loc[1];
			} else {
				tempLocator = temploc;
				locatorType = "";
			}
			if (locatorType.equalsIgnoreCase("xpath") || tempLocator.contains("//")) {
				return driver.findElements(By.xpath(tempLocator));
			} else if (locatorType.equalsIgnoreCase("id")) {
				return driver.findElements(By.id(tempLocator));
			} else if (locatorType.equalsIgnoreCase("name")) {
				return driver.findElements(By.name(tempLocator));
			} else if (locatorType.equalsIgnoreCase("css")) {
				return driver.findElements(By.cssSelector(tempLocator));
			} else if (locatorType.equalsIgnoreCase("text")) {
				return driver.findElements(By.linkText(tempLocator));
			} else if (locatorType.equalsIgnoreCase("class")) {
				return driver.findElements(By.className(tempLocator));
			} else if (locatorType.equalsIgnoreCase("tag")) {
				return driver.findElements(By.tagName(tempLocator));
			} else if (locatorType.equalsIgnoreCase("partialText")) {
				return driver.findElements(By.partialLinkText(tempLocator));
			}
		} catch (IOException e) {
			TestListeners.extentTest.get().fail("Error while getting locator: " + e);
			logger.error("Error while getting locator: " + e);
		}
		return null;
	}

	public String getLocatorValue(String locator) {
		@SuppressWarnings("unused")
		String tempLocator, locatorType = null;
		File jsonFile;
		try {
			jsonFile = new File(System.getProperty("user.dir") + "//resources//Locators//mobileObjRepository.json");
			String temploc = ((String) JsonPath.read(jsonFile, "$." + locator));
			if (temploc.contains(";")) {
				String loc[] = temploc.split(";");
				locatorType = loc[0];
				tempLocator = loc[1];
			} else {
				tempLocator = temploc;
				locatorType = "";
			}
			return tempLocator;
		} catch (IOException e) {
			TestListeners.extentTest.get().fail("Error while getting locator: " + e);
			logger.error("Error while getting locator: " + e);
		}
		return null;
	}

}