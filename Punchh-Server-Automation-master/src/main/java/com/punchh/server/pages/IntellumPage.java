package com.punchh.server.pages;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.Utilities;

public class IntellumPage {
	static Logger logger = LogManager.getLogger(IntellumPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;

	public IntellumPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
	}

	public void navigateToIntellumParAcademy() {
        utils.getLocator("intellumPage.navigateToParAcademy").click();
        logger.info("User navigated to Intellum Par Academy page");
    }

	public List<String> getNavBarTexts() {
		String navbarXpath = utils.getLocatorValue("intellumPage.navBarHeaders");
		List<WebElement> navBarElements = driver.findElements(By.xpath(navbarXpath));
		List<String> navBarTexts = navBarElements.stream()
				.map(WebElement::getText)
				.map(String::trim)
				.collect(Collectors.toList());
		return navBarTexts;
	}

	public WebElement getLearnUponLocator() {
		return utils.getLocator("intellumPage.learnUponAcademy");
	}

	public String getIntellumBaseUrlLocator() {
		return utils.getLocatorValue("intellumPage.baseUrl");
	}

	public String getIntellumUidLocator() {
		return utils.getLocatorValue("intellumPage.uid");
	}

	public String getIsIntellumEnabledCheckboxLocator() {
		return utils.getLocatorValue("intellumPage.isIntellumEnabled");
	}

	public String getIntellumPrivateKeyLocator() {
		return utils.getLocatorValue("intellumPage.privateKey");
	}

	public WebElement getIntellumBaseUrlField() {
		return driver.findElement(By.id(getIntellumBaseUrlLocator()));
	}

	public WebElement getIntellumUidField() {
		return driver.findElement(By.id(getIntellumUidLocator()));
	}

	public WebElement getIntellumPrivateKeyField() {
		return driver.findElement(By.id(getIntellumPrivateKeyLocator()));
	}

	public WebElement isIntellumEnabledCheckbox() {
		return driver.findElement(By.id(getIsIntellumEnabledCheckboxLocator()));
	}

	public boolean isIntellumEnabled() {
        return isIntellumEnabledCheckbox().isSelected();
    }

	public Map<String, String> getIntellumConfigValues() {
        Map<String, String> values = new HashMap<>();
        values.put("baseUrl", getIntellumBaseUrlField().getAttribute("value"));
        values.put("uid", getIntellumUidField().getAttribute("value"));
        values.put("privateKey", getIntellumPrivateKeyField().getAttribute("value"));
        return values;
    }

    public String getCurrentPageUrl() {
        return driver.getCurrentUrl();
    }

    public String getPageTitle() {
        return driver.getTitle();
    }
}

