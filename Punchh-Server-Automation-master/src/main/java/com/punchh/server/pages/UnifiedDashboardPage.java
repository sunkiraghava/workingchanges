package com.punchh.server.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.github.javafaker.Faker;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

public class UnifiedDashboardPage {
    static Logger logger = LogManager.getLogger(UnifiedDashboardPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	Faker faker;

	public UnifiedDashboardPage(WebDriver driver) {
    this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		faker = new Faker();
		selUtils = new SeleniumUtilities(driver);
	}

    public String getTilesLocator() {
        String tiles = utils.getLocatorValue("unifiedDashboardService.tiles");
        return tiles;
    }

    public List<String> getAllApplicationNamesFromUI() {
        List<String> applicationNames = new ArrayList<>();
        try {
            String xpath = getTilesLocator();
            List<WebElement> elements = driver.findElements(By.xpath(xpath));
            logger.info("Found " + elements.size() + " application tiles with XPath: " + xpath);
            
            for (int i = 0; i < elements.size(); i++) {
                String text = elements.get(i).getText().trim();
                if (!text.isEmpty()) {
                    applicationNames.add(text);
                    logger.info("Found application name at index " + i + ": " + text);
                }
            }
            
            logger.info("Total application names found: " + applicationNames.size() + " - " + applicationNames);
            TestListeners.extentTest.get().info("Total application names found: " + applicationNames.size() + " - " + applicationNames);
        } catch (Exception e) {
            logger.error("Error extracting application names from UI: " + e.getMessage());
            TestListeners.extentTest.get().fail("Error extracting application names from UI: " + e.getMessage());
        }
        return applicationNames;
    }

    public void clickOnApplicationByIndex(int index) {
        try {
            String xpath = getTilesLocator();
            List<WebElement> elements = driver.findElements(By.xpath(xpath));
            if (elements.size() > index && index >= 0) {
                String applicationName = elements.get(index).getText().trim();
                utils.StaleElementclick(driver, elements.get(index));
                logger.info("Clicked on application at index " + index + ": " + applicationName);
                utils.longWaitInSeconds(3);
                TestListeners.extentTest.get().info("Clicked on application at index " + index + ": " + applicationName);
            } else {
                logger.error("Invalid index: " + index + ". Total applications found: " + elements.size());
                TestListeners.extentTest.get().fail("Invalid index: " + index + ". Total applications found: " + elements.size());
            }
        } catch (Exception e) {
            logger.error("Error clicking on application at index " + index + ": " + e.getMessage());
            TestListeners.extentTest.get().fail("Error clicking on application at index " + index + ": " + e.getMessage());
        }
    }

    public void logoutFromUnifiedDashboard() {
        utils.getLocator("unifiedLoginPage.logoutIcon").click();
        utils.waitTillPagePaceDone();
    }

    public String getValueFromAdminTable(String userEmail, String env, String columnName) throws Exception {
        String query = "SELECT " + columnName + " FROM admins WHERE email = '" + userEmail + "' LIMIT 1";
        String data = DBUtils.executeQueryAndGetColumnValue(env, query, columnName);
        return data;
    }
}
