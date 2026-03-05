package com.punchh.server.pages;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class AdminRolesPage {

	static Logger logger = LogManager.getLogger(AdminRolesPage.class);
	private WebDriver driver;
	Utilities utils;
	SeleniumUtilities selUtils;

	public AdminRolesPage(WebDriver driver) {
		this.driver = driver;
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
	}

	public void onOffAdminRoelPermission(String permissionTitle, String permissionValue) {

		WebElement permisionName = driver.findElement(By.xpath("//div[@title='" + permissionTitle + "']"));
		String bgcolor = utils.getBGColor(permisionName);
		WebElement saveButton = utils.getLocator("adminRolesPage.saveBtn");

		switch (permissionValue) {
		case "on": // turn on if not already on
			if (!bgcolor.equals("#d4edda")) {
				permisionName.click();
				saveButton.click();
				logger.info("Role permission turned on successfully");
				TestListeners.extentTest.get().info("Role permission turned on successfully");
			} else {
				logger.info("Role permission is already turned on...");
				TestListeners.extentTest.get().info("Role permission is already turned on...");
			}

			break;
		case "off": // turn off if not already off
			if (!bgcolor.equals("#f8d7da")) {
				permisionName.click();
				saveButton.click();
				logger.info("Role permission turned off successfully");
				TestListeners.extentTest.get().info("Role permission turned off successfully");
			} else {
				logger.info("Role permission is already turned off...");
				TestListeners.extentTest.get().info("Role permission is already turned off...");
			}

			break;
		}

	}
}
