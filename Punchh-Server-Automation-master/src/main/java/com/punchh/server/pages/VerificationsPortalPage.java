package com.punchh.server.pages;

import java.util.List;
import java.util.Random;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

public class VerificationsPortalPage {
	private WebDriver driver;
	private static final Logger logger = LogManager.getLogger(VerificationsPortalPage.class);
	private Utilities utils;
	private Properties prop;

	public VerificationsPortalPage(WebDriver driver) {
		this.driver = driver;
		utils = new Utilities(driver);
		prop = Utilities.loadPropertiesFile("config.properties");
	}

	public void navigateToVerificationsPortal(String url) {
		try {
			driver.get(url);
			logger.info("Navigated to verifications login page");
			TestListeners.extentTest.get().info("Navigated to verifications login page with URL: " + url);
		} catch (Exception e) {
			logger.error("Error in navigating to verifications login page: " + e);
			TestListeners.extentTest.get().fail("Error in navigating to verifications login page: " + e);
			throw new RuntimeException("Failed to navigate to verifications portal", e);
		}
	}

	public void enterLoginEmail(String username) {
		utils.getLocator("verificationsPortalPage.emailTxtBx").clear();
		utils.getLocator("verificationsPortalPage.emailTxtBx").sendKeys(username);
		logger.info("Username is entered" + username);
		TestListeners.extentTest.get().info("Username is entered" + username);
	}

	public void enterLoginPassword(String password) {
		utils.getLocator("verificationsPortalPage.passwordTxtBx").clear();
		utils.getLocator("verificationsPortalPage.passwordTxtBx").sendKeys(password);
		logger.info("Password is entered");
		TestListeners.extentTest.get().info("Password is entered");
	}

	public void clickLoginButton() {
		WebElement loginButton = utils.getLocator("verificationsPortalPage.loginBtn");
		utils.clickByJSExecutor(driver, loginButton);
		logger.info("Clicked Login Button");
		TestListeners.extentTest.get().info("Clicked Login Button");
	}

	public boolean verifyLogin() {
		if (utils.checkElementPresent(utils.getLocator("verificationsPortalPage.successLoginLabel"))) {
			logger.info("User logged in to verifications portal successfully");
			TestListeners.extentTest.get().info("User logged in to verifications portal successfully");
			return true;
		} else {
			logger.error("User login verification failed.");
			TestListeners.extentTest.get().fail("User login verification failed.");
			return false;
		}
	}

	public boolean verifyDeactivatedLogin() {
		if (utils.checkElementPresent(utils.getLocator("verificationsPortalPage.deactivatedLoginLabel"))) {
			logger.info("Deactivated user unable to login");
			TestListeners.extentTest.get().info("Deactivated user unable to login");
			return true;
		} else {
			return false;
		}
	}

	public void searchCheckIn(String checkinId) {
		WebElement searchCheckInTextbox = utils.getLocator("verificationsPortalPage.searchCheckInTxtBx");
		searchCheckInTextbox.isDisplayed();
		searchCheckInTextbox.click();
		searchCheckInTextbox.sendKeys(checkinId);
		searchCheckInTextbox.sendKeys(Keys.ENTER);
		String checkInHeader = utils.getLocatorValue("verificationsPortalPage.checkinHeader").replace("$checkinId",
				checkinId);
		if (driver.findElement(By.xpath(checkInHeader)).isDisplayed()) {
			logger.info("Navigated to receipt check-in page successfully");
			TestListeners.extentTest.get().info("Navigated to receipt check-in page successfully");
		} else {
			logger.error("Fail to Navigate to receipt check-in page successfully");
			TestListeners.extentTest.get().fail("Fail to navigated to receipt check-in page successfully");
		}
	}

	public void enterReceiptNumber(String receiptNumber) {
		utils.getLocator("verificationsPortalPage.receiptNumberTxtBx").sendKeys(receiptNumber);
		logger.info("Entered receipt number as: " + receiptNumber);
		TestListeners.extentTest.get().info("Entered receipt number as: " + receiptNumber);
	}

	public void enterReceiptAmount(String receiptAmount) {
		utils.getLocator("verificationsPortalPage.receiptAmountTxtBx").sendKeys(receiptAmount);
		logger.info("Entered receipt amount as: " + receiptAmount);
		TestListeners.extentTest.get().info("Entered receipt amount as: " + receiptAmount);
	}

	public String selectRandomComment() {
		utils.getLocator("verificationsPortalPage.receiptCommentDrpDwn").click();
		List<WebElement> commentList = utils.getLocatorList("verificationsPortalPage.receiptCommentDrpDwnValues");
		Random random = new Random();
		int randomIndex = random.nextInt(commentList.size());
		WebElement selectedCommentElement = commentList.get(randomIndex);
		String selectedComment = selectedCommentElement.getText();
		utils.selectListDrpDwnValue(commentList, selectedComment);
		logger.info("Selected Receipt Comment: " + selectedComment);
		TestListeners.extentTest.get().info("Selected Receipt Comment: " + selectedComment);
		return selectedComment;
	}

	public void clickValidBtn() {
		utils.getLocator("verificationsPortalPage.validBtn").click();
		logger.info("Clicked valid button");
		TestListeners.extentTest.get().info("Clicked valid button");
	}

	public void clickInvalidBtn() {
		utils.getLocator("verificationsPortalPage.invalidBtn").click();
		logger.info("Clicked invalid button");
		TestListeners.extentTest.get().info("Clicked invalid button");
	}
}
