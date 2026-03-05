package com.punchh.server.pages;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class InstanceDashboard {
	static Logger logger = LogManager.getLogger(InstanceDashboard.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	String temp = "temp";
	SsoLoginSignUpPage ssoSignUpPage;
	String barcodeValue;
	String prodUrl1 = "dashboard.punchh";
	String prodUrl = "dashboard.punchh.com";
	MenuPage menuPage;
	int counter = 1;

	public InstanceDashboard(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		menuPage = new MenuPage(driver);
	}

	public void navigateToPunchhInstance(String url) {
		driver.get(url);
		utils.longWaitInSeconds(3);
		logger.info("Navigated to instance login page");
	}

	public void reloginInstance() throws Exception {
		// try {
		utils.getLocator("instanceLoginPage.emailField").clear();
		utils.getLocator("instanceLoginPage.emailField").sendKeys(prop.getProperty("userName"));
		logger.info("User Name is Entered");
		utils.getLocator("instanceLoginPage.passwordField").clear();
		utils.getLocator("instanceLoginPage.passwordField").sendKeys(utils.decrypt(prop.getProperty("password")));
		logger.info("Password is Entered");
		utils.clickByJSExecutor(driver, utils.getLocator("instanceLoginPage.loginButton"));
		logger.info("Clicked Login Button");
		// utils.getLocator("instanceLoginPage.loginButton").click();
		// utils.clickByJSExecutor(driver,utils.getLocator("instanceLoginPage.loginButton"));
		utils.getLocator("instanceDashboardPage.successLoginLabel").isDisplayed();
		logger.info("User logged in to Punchh instance successfully");
		TestListeners.extentTest.get().info("User logged in to Punchh instance successfully");
		// } catch (Exception e) {
		// logger.error("Error in login to instance " + e);
		// }
	}

	public void loginToInstancee() {
		try {
			utils.getLocator("instanceLoginPage.emailField").isDisplayed();
			utils.getLocator("instanceLoginPage.emailField").clear();
			utils.getLocator("instanceLoginPage.emailField").sendKeys(prop.getProperty("userName"));
			logger.info("User Name is Entered");
			utils.getLocator("instanceLoginPage.passwordField").clear();
			utils.getLocator("instanceLoginPage.passwordField").sendKeys(utils.decrypt(prop.getProperty("password")));
			logger.info("Password is Entered");
			TestListeners.extentTest.get().info("Entered Username and Password ");
			selUtils.waitTillElementToBeClickable(utils.getLocator("instanceLoginPage.loginButton"));
			utils.clickByJSExecutor(driver, utils.getLocator("instanceLoginPage.loginButton"));
			TestListeners.extentTest.get().info("Cliked on login button ");
			// utils.getLocator("instanceLoginPage.loginButton").click();
			// utils.getLocator("instanceLoginPage.loginButton").sendKeys(Keys.ENTER);
			logger.info("Clicked Login Button");
			/*
			 * if (driver.getCurrentUrl().contains(prodUrl)) { enterOtp();
			 * 
			 * }
			 */
			WebElement ele = utils.getLocator("instanceDashboardPage.successLoginLabel");
			if (utils.checkElementPresent(ele)) {
				logger.info("User logged in to Punchh instance successfully");
				TestListeners.extentTest.get().info("User logged in to Punchh instance successfully");
			}
		} catch (Exception e) {
			logger.error("Error in login to instance " + e);
			TestListeners.extentTest.get().info("Error in login to instance " + e);
			/*
			 * if (driver.getCurrentUrl().contains("staging")) { try { if
			 * (utils.getLocator("instanceLoginPage.emailField").isDisplayed()) {
			 * logger.error("Retrying instance login......"); reloginInstance(); }
			 * 
			 * } catch (Exception i) { TestListeners.extentTest.get().
			 * info("After retrying not able to login into application");
			 * logger.error("After retrying not able to login into application "); } } }
			 */

		}
	}

	public void loginToInstance() {
		EnterLoginEmail(prop.getProperty("userName"));
		try {
			EnterLoginPassword(utils.decrypt(prop.getProperty("password")));
		} catch (Exception e) {
			logger.info("Error in decrypting password " + e);
			TestListeners.extentTest.get().info("Error in decrypting password " + e);
		}
		clickLoginButton();
		utils.waitTillPagePaceDone();
		WebElement ele = utils.getLocator("instanceDashboardPage.successLoginLabel");
		if (utils.checkElementPresent(ele)) {
			logger.info("User logged in to Punchh instance successfully");
			TestListeners.extentTest.get().info("User logged in to Punchh instance successfully");
		}
	}

	public void instanceLogin(String url) {

		driver.get(url);
		utils.longWaitInSeconds(2);
		logger.info("Navigated to instance login page");
		EnterLoginEmail(prop.getProperty("userName"));
		try {
			EnterLoginPassword(utils.decrypt(prop.getProperty("password")));
		} catch (Exception e) {
			logger.info("Error in decrypting password " + e);
			TestListeners.extentTest.get().info("Error in decrypting password " + e);
		}
		clickLoginButton();
		utils.longWaitInSeconds(1);
		WebElement ele = utils.getLocator("instanceDashboardPage.successLoginLabel");
		if (utils.checkElementPresent(ele)) {
			logger.info("User logged in to Punchh instance successfully");
			// TestListeners.extentTest.get().info("User logged in to Punchh instance
			// successfully");
		}
	}

	public void loginToInstanceWithCheck() {
		utils.longWaitInSeconds(5);
		String currentUrl = driver.getCurrentUrl();
		if (currentUrl.contains("businesses")) {
			logger.info("User is already logged in to Punchh instance - URL contains 'businesses': " + currentUrl);
			TestListeners.extentTest.get()
					.info("User is already logged in to Punchh instance - URL contains 'businesses': " + currentUrl);
			return;
		}
		loginToInstance();
	}

	public void EnterLoginEmail(String email) {
		utils.implicitWait(2);
		utils.getLocator("instanceLoginPage.emailField").isDisplayed();
		utils.getLocator("instanceLoginPage.emailField").clear();
		utils.getLocator("instanceLoginPage.emailField").sendKeys(email);
		logger.info("User Name is Entered");
		// TestListeners.extentTest.get().info("User Name is Entered");
		utils.implicitWait(60);
	}

	public void EnterLoginPassword(String password) throws Exception {
		utils.getLocator("instanceLoginPage.passwordField").isDisplayed();
		utils.getLocator("instanceLoginPage.passwordField").clear();
		utils.getLocator("instanceLoginPage.passwordField").sendKeys(password);
		logger.info("Password is Entered");
		// TestListeners.extentTest.get().info("Password is Entered");
	}

	public void clickLoginButton() {
		WebElement loginButton = utils.getLocator("instanceLoginPage.loginButton");
		selUtils.waitTillElementToBeClickable(loginButton);
		try {
			loginButton.click();
			// utils.clickByJSExecutor(driver, loginButton);
		} catch (TimeoutException e) {
			logger.warn("Timeout after clicking login button " + e);
		}
		logger.info("Clicked Login Button");
	}

	public void enterLoginCreds(String email, String password) throws Exception {
		EnterLoginEmail(email);
		EnterLoginPassword(password);
		clickLoginButton();
		utils.waitTillPagePaceDone();
	}

	public void movedToLoginAndSelectBusiness(String business) throws InterruptedException {
		try {
			WebElement enterEmailField = utils.getLocator("instanceLoginPage.emailField");
			if (enterEmailField.isDisplayed()) {
				loginToInstance();
				selectBusiness(business);
			}
		} catch (Exception i) {

		}

	}

	public void forgotPassword() {
		utils.getLocator("instanceLoginPage.forgotPwd").isDisplayed();
		utils.getLocator("instanceLoginPage.forgotPwd").click();
		utils.getLocator("instanceLoginPage.emailField").isDisplayed();
		utils.getLocator("instanceLoginPage.emailField").clear();
		utils.getLocator("instanceLoginPage.emailField").sendKeys(prop.getProperty("userName"));
		logger.info("User Name is Entered");
		utils.getLocator("instanceLoginPage.sendPwdresetemail").isDisplayed();
		utils.getLocator("instanceLoginPage.sendPwdresetemail").click();
		// utils.getLocator("instanceLoginPage.alreadyAMember").click();
		utils.StaleElementclick(driver, utils.getLocator("instanceLoginPage.alreadyAMember"));
	}

	private void enterOtp() throws Exception {
		String otp;

		otp = Utilities.getTOTPCode(utils.decrypt(Utilities.getConfigProperty("rSecKey")));
		utils.getLocator("instanceLoginPage.mfaHeading").isDisplayed();
		utils.getLocator("instanceLoginPage.mfaTextbox").isDisplayed();
		utils.getLocator("instanceLoginPage.mfaTextbox").clear();
		utils.getLocator("instanceLoginPage.mfaTextbox").sendKeys(otp);
		utils.getLocator("instanceLoginPage.submitButton").click();
		utils.getLocator("instanceDashboardPage.twoFactorLoginMsgLabel").isDisplayed();
		utils.getLocator("dashboardPage.dataRangeTextBox").isDisplayed();
		logger.info("OTP login successfull");
		TestListeners.extentTest.get().info("OTP login successfull");

	}

	public void logintoInstance(String userNmae, String password) {
		try {
			utils.getLocator("instanceLoginPage.emailField").isDisplayed();
			utils.waitTillElementToBeClickable(utils.getLocator("instanceLoginPage.emailField"));
			utils.getLocator("instanceLoginPage.emailField").click();
			utils.getLocator("instanceLoginPage.emailField").clear();
			utils.getLocator("instanceLoginPage.emailField").sendKeys(userNmae);
			utils.getLocator("instanceLoginPage.passwordField").isDisplayed();
			utils.getLocator("instanceLoginPage.passwordField").clear();
			utils.getLocator("instanceLoginPage.passwordField").sendKeys(utils.decrypt(password));
			utils.getLocator("instanceLoginPage.loginButton").isDisplayed();
			utils.getLocator("instanceLoginPage.loginButton").click();
			utils.getLocator("instanceDashboardPage.successLoginLabel").isDisplayed();
			logger.info("User logged in to Punchh instance successfully");
		} catch (Exception e) {
			logger.error("Error in login to instance " + e);
		}
	}

	public void selectBusiness(String slug) {
		String instanceAccount = slug + " /"; // to handle businesses with same name contains
		String instanceXpath = utils.getLocatorValue("instanceDashboardPage.instanceAccountXpath")
				.replace("${instanceAccount}", instanceAccount);
		WebElement businessTile = driver.findElement(By.xpath(instanceXpath));
		utils.StaleElementclick(driver, businessTile);
		logger.info("Selected business: " + instanceAccount);
		TestListeners.extentTest.get().info("Selected business: " + instanceAccount);

		menuPage.navigateToSubMenuItem("Cockpit", "Dashboard");
		checkUncheckFlagOnCockpitDasboardCase("Enable New SideNav, Header and Footer?", "check");

		// Reselect business if dashboard not loaded properly
		if (driver.getCurrentUrl().contains("businesses?filter=all")) {
			WebElement ele = driver.findElement(By.xpath(instanceXpath));
			utils.StaleElementclick(driver, ele);
			logger.info("Reselected business: " + instanceAccount);
			TestListeners.extentTest.get().info("Reselected business: " + instanceAccount);

		}
	}

	public void moveToAllBusinessPage(String url) {
		String pageUrl = url + "/businesses?filter=all";
		driver.navigate().to(pageUrl);
		utils.waitTillPagePaceDone();
		logger.info("Navigated to all business page");
		// TestListeners.extentTest.get().info("Navigated to all business page with url
		// :" + url);
	}

	public boolean checkNewNavIsActiveAndClickOnSubMenuItem() {

		boolean flag = false;
		try {
			utils.implicitWait(2);
			WebElement newNav = driver.findElement(By.xpath("//div[@id='punchh_nav_app']"));
			flag = newNav.isDisplayed();
			if (flag) {
				logger.info("New navigation is Active ");
				TestListeners.extentTest.get().info("New navigation is Active ");
			}

		} catch (Exception e) {
			logger.info("New navigation is Not Active ");
			TestListeners.extentTest.get().info("New navigation is Not Active ");
		}
		utils.implicitWait(50);
		return flag;
	}

	public void navigateToGuestTimeline(String userEmail) throws InterruptedException {
		boolean isNewNavActive = checkNewNavIsActiveAndClickOnSubMenuItem();
		if (isNewNavActive) {
			navigateToGuestTimeline_new(userEmail);
		} else {
			navigateToGuestTimeline_old(userEmail);
		}

	}

	public void navigateToGuestTimeline_old(String email) throws InterruptedException {
		@SuppressWarnings("unused")
		boolean flag = false;
		int attempts = 0;
		while (attempts <= 10) {
			try {
				utils.getLocator("instanceDashboardPage.searchGuest").isDisplayed();
				utils.getLocator("instanceDashboardPage.searchGuest").clear();
				utils.getLocator("instanceDashboardPage.searchGuest").sendKeys(email);
				utils.longWaitInSeconds(1);
				// utils.getLocator("instanceDashboardPage.searchGuest").sendKeys(Keys.ENTER);
				utils.getLocator("instanceDashboardPage.searchGuest").submit();
				logger.info("Searching new user: " + email);
				TestListeners.extentTest.get().info("Searching new user: " + email);
				utils.waitTillPagePaceDone();
				String str = utils.getLocator("instanceDashboardPage.searchedGuest").getText();
				String[] name = str.split("\n");
				String userMail = name[1].trim();
				if (userMail.equalsIgnoreCase(email)) {
					flag = true;
					logger.info("Guest email found");
					TestListeners.extentTest.get().info("Guest email found");
					break;
				}
			} catch (Exception e) {
				logger.error("Guest did not found trying again " + e);
			}
			attempts++;
		}
		utils.getLocator("instanceDashboardPage.guestLink").isDisplayed();
		utils.getLocator("instanceDashboardPage.guestLink").click();
		// utils.waitTillPagePaceDone();
		logger.info("Guest email found : " + email);
		TestListeners.extentTest.get().pass("Guest email found : " + email);

	}

	public void navigateToGuestTimeline_new(String email) throws InterruptedException {
		int attempts = 0;
		WebElement searchedGuest = null;
		WebElement guestSearchLabel = utils.getLocator("instanceDashboardPage.guestSearchLabel");
		guestSearchLabel.isDisplayed();
		guestSearchLabel.click();
		while (attempts <= 10) {
			try {
				WebElement guestSearchBox = utils.getLocator("instanceDashboardPage.guestSearchBox");
				guestSearchBox.clear();
				guestSearchBox.sendKeys(email);
				utils.longWaitInSeconds(2);
				utils.implicitWait(3);
				searchedGuest = utils.getLocator("instanceDashboardPage.SearchedGuest");
				if (searchedGuest.isDisplayed()) {
					utils.longWaitInMiliSeconds(200);
					searchedGuest.click();
					logger.info("Clicked on searched guest and moved to guest timeline " + email);
					TestListeners.extentTest.get()
							.info("Clicked on searched guest and moved to guest timeline " + email);
					utils.implicitWait(60);
					break;
				}
			} catch (Exception e) {
				logger.info("Guest email not found : " + email + ", attempts :" + attempts);
				TestListeners.extentTest.get().info("Guest email not found: " + email + ", attempts :" + attempts);
			}
			attempts++;
		}
	}

	public boolean verifyGuestsPresence(String email) {
		try {
			utils.getLocator("instanceDashboardPage.searchGuest").isDisplayed();
			utils.getLocator("instanceDashboardPage.searchGuest").clear();
			utils.getLocator("instanceDashboardPage.searchGuest").sendKeys(email);
			utils.getLocator("instanceDashboardPage.searchGuest").submit();
			logger.info("Checking if guests exists: " + email);
			TestListeners.extentTest.get().info("Checking if guests exists: " + email);
			selUtils.implicitWait(10);
			if (utils.getLocatorList("instanceDashboardPage.searchGuest").size() > 0) {
				utils.getLocator("instanceDashboardPage.guestLink").isDisplayed();
				utils.getLocator("instanceDashboardPage.guestLink").click();
				logger.info("Guest is present: " + email);
				TestListeners.extentTest.get().info("Guest is present: " + email);
				return true;
			} else {
				logger.error("Guest doesn't exists: " + email);
				TestListeners.extentTest.get().info("Guest doesn't exists: " + email);
			}
		} catch (Exception e) {
			logger.error("Error in verifying new user " + e);
			TestListeners.extentTest.get().info("Error in verifying new user " + email);
		}
		return false;
	}

	public void goToTestBarcodes() {

		utils.getLocator("instanceDashboardPage.supportLabel").isDisplayed();
		selUtils.mouseHoverOverElement(utils.getLocator("instanceDashboardPage.supportLabel"));
		utils.getLocator("instanceDashboardPage.supportLabel").click();
		utils.getLocator("instanceDashboardPage.testBarcodeLink").isDisplayed();
		utils.getLocator("instanceDashboardPage.testBarcodeLink").click();
		utils.getLocator("instanceDashboardPage.testBarccadeHeadline").isDisplayed();
		logger.info("Navigated to test barcode page");
		TestListeners.extentTest.get().info("Navigated to test barcode page");
		// utils.getLocator("instanceDashboardPage.authAppNameLink").isDisplayed();
		// utils.getLocator("instanceDashboardPage.authAppNameLink").click();
		// utils.getLocator("instanceDashboardPage.authorizeWithPunchhLink").isDisplayed();
		// utils.getLocator("instanceDashboardPage.authorizeWithPunchhLink").click();
		// logger.info("Clicked on Authorize with Punchh");
		// TestListeners.extentTest.get().info("Clicked on Authorize with Punchh");
		// utils.getLocator("oauthAppPage.createSsoAccountLink").isDisplayed();
	}

	public void refreshPage() {
		driver.navigate().refresh();
		driver.get(driver.getCurrentUrl());
		// driver.navigate().to(driver.getCurrentUrl());
		logger.info("Refreshing Web Page");
	}

	public void generateBarcode(String location) {
		refreshPage();
		try {
			logger.info("==Setting test bar code info==");
			TestListeners.extentTest.get().info("==Setting test bar code info==");
			utils.getLocator("instanceDashboardPage.locationDropdownButton").isDisplayed();
			utils.getLocator("instanceDashboardPage.locationDropdownButton").click();

			utils.getLocator("instanceDashboardPage.firstLocation").click();
			utils.getLocator("instanceDashboardPage.dateTextbox").isDisplayed();
			utils.getLocator("instanceDashboardPage.dateTextbox").click();
			utils.getLocator("instanceDashboardPage.dateApplyButton").isDisplayed();
			utils.getLocator("instanceDashboardPage.dateApplyButton").click();
			utils.getLocator("instanceDashboardPage.minAmoutTextbox").isDisplayed();
			utils.getLocator("instanceDashboardPage.minAmoutTextbox").clear();
			utils.getLocator("instanceDashboardPage.minAmoutTextbox").sendKeys(prop.getProperty("minAmount"));
			utils.getLocator("instanceDashboardPage.maxAmoutTextbox").isDisplayed();
			utils.getLocator("instanceDashboardPage.maxAmoutTextbox").clear();
			utils.getLocator("instanceDashboardPage.maxAmoutTextbox").sendKeys(prop.getProperty("maxAmount"));
			logger.info("Min and Max amout set as: " + prop.getProperty("minAmount") + " and "
					+ prop.getProperty("maxAmount"));
			TestListeners.extentTest.get().info("Min and Max amout set as: " + prop.getProperty("minAmount") + " and "
					+ prop.getProperty("maxAmount"));
			utils.getLocator("instanceDashboardPage.menuItemTextbox").isDisplayed();
			utils.getLocator("instanceDashboardPage.menuItemTextbox").clear();
			utils.getLocator("instanceDashboardPage.menuItemTextbox").sendKeys(prop.getProperty("menuItem"));
			logger.info("Menu item is set as " + prop.getProperty("menuItem"));
			TestListeners.extentTest.get().info("Menu item is set as " + prop.getProperty("menuItem"));
			utils.getLocator("instanceDashboardPage.itemNameTextbox").isDisplayed();
			utils.getLocator("instanceDashboardPage.itemNameTextbox").clear();
			utils.getLocator("instanceDashboardPage.itemNameTextbox").sendKeys(prop.getProperty("itemName"));
			logger.info("Item name is set as " + prop.getProperty("itemName"));
			TestListeners.extentTest.get().info("Item name is set as " + prop.getProperty("itemName"));
			logger.info("Selected bar code location: " + location);
			TestListeners.extentTest.get().info("Selected bar code location: " + location);
			Select sel = new Select(utils.getLocator("instanceDashboardPage.menuItemtypeDropdown"));
			sel.selectByIndex(2);
			utils.getLocator("instanceDashboardPage.generateBarcodeButton").isDisplayed();
			utils.getLocator("instanceDashboardPage.generateBarcodeButton").click();
			// selUtils.jsClick(utils.getLocator("instanceDashboardPage.generateBarcodeButton"));
			logger.info("Clicked on generate bar code");
			TestListeners.extentTest.get().info("Clicked on generate bar code");
		} catch (Exception e) {
			logger.error("Error in generating bar code " + e);
			TestListeners.extentTest.get().fail("Error in generating bar code " + e);
		}
	}

	public String captureBarcodeAmount() {
		String barcodeAmount;
		utils.getLocator("instanceDashboardPage.barcodeAmount").isDisplayed();
		barcodeAmount = utils.getLocator("instanceDashboardPage.barcodeAmount").getText();
		return barcodeAmount;
	}

	public String captureBarcodeTransactionNumber() {
		String barcodeTransactionNo;
		utils.getLocator("instanceDashboardPage.barcodeTransactionNo").isDisplayed();
		barcodeTransactionNo = utils.getLocator("instanceDashboardPage.barcodeTransactionNo").getText();
		return barcodeTransactionNo;
	}

	public String captureBarcode() {
		String parentWindow = selUtils.switchToNewWindow();
		try {
			Thread.sleep(4000);
			getPageState();
			if (utils.getLocatorList("instanceDashboardPage.testBarcodeHeader").size() > 0) {
				utils.getLocator("instanceDashboardPage.barcodeLabel").isDisplayed();
				barcodeValue = utils.getLocator("instanceDashboardPage.barcodeLabel").getAttribute("data-barcode");
				driver.close();
				driver.switchTo().window(parentWindow);
				logger.info("BarcodeValue captured -" + barcodeValue);
				TestListeners.extentTest.get().info("BarcodeValue captured -" + barcodeValue);

				// utils.getLocator("iframeLoginPage.accountBalanceLink").isDisplayed();
			} else {
				selUtils.testFailed("Some error in generating bar code");
			}
		} catch (Exception e) {
			logger.error("Error in capturing bar code " + e);
			TestListeners.extentTest.get().fail("Error in capturing bar code " + e);
		}
		return barcodeValue;
	}

	public boolean getPageState() {
		boolean flag = false;
		int attempts = 0;
		while (attempts < 5) {

			try {
				String val = driver.getPageSource();

				if (val.contains("Test Barcodes generated")) {
					flag = true;
					logger.info("Test barcode generated successfully");
					break;
				}
			} catch (Exception e) {
				logger.info("Element is not present or Test barcodes not generated");

			}
			utils.refreshPage();
			attempts++;
		}
		return flag;
	}

	public String getBarcode() {
		try {
			int counter = 0;
			selUtils.switchToNewWindow();
			selUtils.implicitWait(5);
			while (counter < 4) {
				getPageState();
				utils.getLocator("instanceDashboardPage.testBarcodeHeader").isDisplayed();
				try {
					barcodeValue = utils.getLocator("instanceDashboardPage.barcodeLabel").getText();
					break;
				} catch (Exception e) {
					logger.info("Retrying barcode capture");
				}
				driver.navigate().refresh();
				counter++;
			}
			selUtils.implicitWait(50);
			if (barcodeValue == null)
				selUtils.testFailed("Some error in generating bar code");
		} catch (Exception e) {
			selUtils.implicitWait(50);
			logger.info("failed to capture barcode, exception: " + e);
			TestListeners.extentTest.get().info("failed to capture barcode, exception: " + e);
		}

		return barcodeValue;
	}

	public void goToSegments() {
		try {
			utils.getLocator("instanceDashboardPage.guestsLabel").isDisplayed();
			selUtils.mouseHoverOverElement(utils.getLocator("instanceDashboardPage.guestsLabel"));
			utils.getLocator("instanceDashboardPage.guestsLabel").click();
			utils.getLocator("instanceDashboardPage.segmentsLink").isDisplayed();
			utils.getLocator("instanceDashboardPage.segmentsLink").click();
			utils.getLocator("segmentPage.segementHeading").isDisplayed();
			logger.info("Navigated to segment page");
			TestListeners.extentTest.get().info("Navigated to segment page");
		} catch (Exception e) {
			logger.error("Error in navigating to segments " + e);
			TestListeners.extentTest.get().fail("Error in navigating to segments " + e);
		}
	}

	public void loginToInstance(String userName, String password) {
		try {
			utils.getLocator("instanceLoginPage.emailField").isDisplayed();
			utils.getLocator("instanceLoginPage.emailField").clear();
			utils.getLocator("instanceLoginPage.emailField").sendKeys(userName);
			utils.getLocator("instanceLoginPage.passwordField").isDisplayed();
			utils.getLocator("instanceLoginPage.passwordField").clear();
			utils.getLocator("instanceLoginPage.passwordField").sendKeys(password);
			utils.getLocator("instanceLoginPage.loginButton").isDisplayed();
			utils.getLocator("instanceLoginPage.loginButton").click();
			utils.getLocator("instanceDashboardPage.successLoginLabel").isDisplayed();
			logger.info("User logged in to Punchh instance successfully");
			TestListeners.extentTest.get().info("User logged in to Punchh instance successfully");
		} catch (Exception e) {
			logger.error("Error in login to instance " + e);
			TestListeners.extentTest.get().fail("Error in login to instance " + e);
		}
	}

	public void barcodelookup(String barcodeValue) {
		utils.getLocator("instanceDashboardPage.locationDropdown").isDisplayed();
		utils.getLocator("instanceDashboardPage.barcodeTextbox").isDisplayed();
		utils.getLocator("instanceDashboardPage.barcodeTextbox").sendKeys(barcodeValue);
		utils.getLocator("instanceDashboardPage.lookupButton").isDisplayed();
		utils.getLocator("instanceDashboardPage.lookupButton").click();
	}

	public boolean verifyBarcodeLookupTransactionNumber(String barcodeValue, String transactionNumber) {
		try {
			driver.findElement(By.xpath(
					utils.getLocatorValue("instanceDashboardPage.lookupBarcodeLabel").replace("temp", barcodeValue)))
					.isDisplayed();
			logger.info("Verified barcode lookup barcode label");
			TestListeners.extentTest.get().info("Verified barcode lookup barcode label");
			driver.findElement(By.xpath(utils.getLocatorValue("instanceDashboardPage.recieptTransactionNUmberText")
					.replace("temp", transactionNumber))).isDisplayed();
			// driver.findElement(By.xpath("//span[.='#temp']".replace("temp",
			// transactionNumber))).isDisplayed();
			logger.info("Verified transaction number");
			TestListeners.extentTest.get().info("Verified transaction number");
			return true;
		} catch (Exception e) {
			logger.info("Fail to verify transaction number: " + e);
			TestListeners.extentTest.get().info("Fail to verify transaction number: " + e);
		}
		return false;
	}

	public boolean verifyBarcodeLookupAmount(String barcodeValue, String barcodeAmount) {
		try {
			driver.findElement(
					By.xpath(utils.getLocatorValue("instanceDashboardPage.amountLabel").replace("temp", barcodeAmount)))
					.isDisplayed();
			logger.info("Verified barcode lookup amount: " + barcodeAmount);
			TestListeners.extentTest.get().pass("Verified barcode lookup amount: " + barcodeAmount);
			return true;
		} catch (Exception e) {
			logger.info("Failed to verify barcode lookup amount: " + e);
			TestListeners.extentTest.get().info("Failed to verify barcode lookup amount: " + e);
		}
		return false;
	}

	public boolean verifyBarcodeSearch() {
		String transactionNumber = "002245693";
		String amount = "$29.00";
		try {
			utils.getLocator("instanceDashboardPage.barcodeSearchTabLabel").isDisplayed();
			utils.getLocator("instanceDashboardPage.barcodeSearchTabLabel").click();
			utils.getLocator("instanceDashboardPage.dateInputbox").isDisplayed();
			utils.getLocator("instanceDashboardPage.dateInputbox").sendKeys("July 22,2021");
			utils.getLocator("instanceDashboardPage.calenderButton").click();
			Select sel = new Select(utils.getLocator("instanceDashboardPage.barcodeSearchLocationDropdown"));
			sel.selectByVisibleText("Boise State - 101559");
			utils.getLocator("instanceDashboardPage.transcationNumberTextbox").isDisplayed();
			utils.getLocator("instanceDashboardPage.transcationNumberTextbox").clear();
			utils.getLocator("instanceDashboardPage.transcationNumberTextbox").sendKeys(transactionNumber);
			utils.getLocator("instanceDashboardPage.searchButton").isDisplayed();
			utils.getLocator("instanceDashboardPage.searchButton").click();
			driver.findElement(By.xpath(utils.getLocatorValue("instanceDashboardPage.transactionNoLabel")
					.replace("temp", transactionNumber))).isDisplayed();
			driver.findElement(By.xpath(utils.getLocatorValue("instanceDashboardPage.transactionNoLabel")
					.replace("temp", transactionNumber))).click();
			driver.findElement(
					By.xpath(utils.getLocatorValue("instanceDashboardPage.amountLabel").replace("temp", amount)))
					.isDisplayed();
			logger.info("Verified barcode search amount and transaction number");
			TestListeners.extentTest.get().info("Verified barcode search amount and transaction number");
			return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	public String captureBarcodeValue() {
		try {
			// String parentWindow = selUtils.switchToNewWindow();
			if (driver.findElements(By.xpath("//h1[contains(.,'Test Barcodes')]")).size() > 0) {
				utils.getLocator("instanceDashboardPage.barcodeLabel").isDisplayed();
				barcodeValue = utils.getLocator("instanceDashboardPage.barcodeLabel").getAttribute("data-barcode");
			} else {
				selUtils.testFailed("Some error in generating bar code");
			}
		} catch (Exception e) {
			logger.error("Error in capturing bar code " + e);
			TestListeners.extentTest.get().fail("Error in capturing bar code " + e);
		}
		return barcodeValue;
	}

	public void generateOldBarcode(String location) {
		String oldDate = "02/02/2020";
		String oldDate1 = "02/10/2020";
		try {
			logger.info("==Setting test bar code info==");
			TestListeners.extentTest.get().info("==Setting test bar code info==");
			utils.getLocator("instanceDashboardPage.locationDropdownButton").isDisplayed();
			utils.getLocator("instanceDashboardPage.locationDropdownButton").click();
			utils.getLocator("instanceDashboardPage.locationSearchBox").clear();
			utils.getLocator("instanceDashboardPage.locationSearchBox").sendKeys(location);
			utils.getLocator("instanceDashboardPage.locationValue").click();
			logger.info("Selected bar code location: " + location);
			TestListeners.extentTest.get().info("Selected bar code location: " + location);
			utils.getLocator("instanceDashboardPage.dateTextbox").isDisplayed();
			utils.getLocator("instanceDashboardPage.dateTextbox").click();
			utils.getLocator("instanceDashboardPage.dateApplyButton").isDisplayed();
			driver.findElement(
					By.xpath(utils.getLocatorValue("instanceDashboardPage.datepickerInput").replace("temp", "1")))
					.clear();
			driver.findElement(
					By.xpath(utils.getLocatorValue("instanceDashboardPage.datepickerInput").replace("temp", "1")))
					.sendKeys(oldDate);
			driver.findElement(
					By.xpath(utils.getLocatorValue("instanceDashboardPage.datepickerInput").replace("temp", "2")))
					.clear();
			driver.findElement(
					By.xpath(utils.getLocatorValue("instanceDashboardPage.datepickerInput").replace("temp", "2")))
					.sendKeys(oldDate1);
			utils.getLocator("instanceDashboardPage.dateApplyButton").click();
			utils.getLocator("instanceDashboardPage.minAmoutTextbox").isDisplayed();
			utils.getLocator("instanceDashboardPage.minAmoutTextbox").clear();
			utils.getLocator("instanceDashboardPage.minAmoutTextbox").sendKeys(prop.getProperty("minAmount"));
			utils.getLocator("instanceDashboardPage.maxAmoutTextbox").isDisplayed();
			utils.getLocator("instanceDashboardPage.maxAmoutTextbox").clear();
			utils.getLocator("instanceDashboardPage.maxAmoutTextbox").sendKeys(prop.getProperty("maxAmount"));
			logger.info("Min and Max amout set as: " + prop.getProperty("minAmount") + " and "
					+ prop.getProperty("maxAmount"));
			TestListeners.extentTest.get().info("Min and Max amout set as: " + prop.getProperty("minAmount") + " and "
					+ prop.getProperty("maxAmount"));
			utils.getLocator("instanceDashboardPage.menuItemTextbox").isDisplayed();
			utils.getLocator("instanceDashboardPage.menuItemTextbox").clear();
			utils.getLocator("instanceDashboardPage.menuItemTextbox").sendKeys(prop.getProperty("menuItem"));
			logger.info("Menu item is set as " + prop.getProperty("menuItem"));
			TestListeners.extentTest.get().info("Menu item is set as " + prop.getProperty("menuItem"));
			utils.getLocator("instanceDashboardPage.itemNameTextbox").isDisplayed();
			utils.getLocator("instanceDashboardPage.itemNameTextbox").clear();
			utils.getLocator("instanceDashboardPage.itemNameTextbox").sendKeys(prop.getProperty("itemName"));
			logger.info("Item name is set as " + prop.getProperty("itemName"));
			TestListeners.extentTest.get().info("Item name is set as " + prop.getProperty("itemName"));
			logger.info("Selected bar code location: " + location);
			TestListeners.extentTest.get().info("Selected bar code location: " + location);
			Select sel = new Select(utils.getLocator("instanceDashboardPage.menuItemtypeDropdown"));
			sel.selectByIndex(2);
			utils.getLocator("instanceDashboardPage.generateBarcodeButton").isDisplayed();
			selUtils.jsClick(utils.getLocator("instanceDashboardPage.generateBarcodeButton"));
			logger.info("Clicked on generate bar code");
			TestListeners.extentTest.get().info("Clicked on generate bar code");
		} catch (Exception e) {
			logger.error("Error in generating bar code " + e);
			TestListeners.extentTest.get().fail("Error in generating bar code " + e);
		}
	}

	public void goToCockpit() {
		try {
			utils.getLocator("instanceDashboardPage.cockpitLabel").isDisplayed();
			selUtils.mouseHoverOverElement(utils.getLocator("instanceDashboardPage.cockpitLabel"));
			utils.getLocator("instanceDashboardPage.cockpitLabel").click();
			utils.getLocator("instanceDashboardPage.physicalCards").click();
			utils.getLocator("instanceDashboardPage.physicalCardsHeading").isDisplayed();
			logger.info("Navigated to physical cards page");
			TestListeners.extentTest.get().info("Navigated to physical cards page");
		} catch (Exception e) {
			logger.error("Error in navigating to physical cards page " + e);
			TestListeners.extentTest.get().fail("Error in navigating to physical cards page " + e);
		}

	}

	public boolean enableExternalVendor() {
		try {
			selUtils.scrollToElement(utils.getLocator("instanceDashboardPage.updateButton"));
			utils.getLocator("instanceDashboardPage.externalVendorPhysicalCardsChecked").isDisplayed();

			if (utils.getLocator("instanceDashboardPage.vendorCheckbox").isSelected()) {
				logger.info("External vendor flag enabled");
				TestListeners.extentTest.get().info("External vendor flag enabled");
				return true;
			} else {

				utils.getLocator("instanceDashboardPage.externalVendorPhysicalCardsChecked").click();
				logger.info("External vendor flag enabled");
				TestListeners.extentTest.get().info("External vendor flag enabled");
			}

		} catch (Exception e) {
			logger.error("Error in enabling external vendor flag " + e);
			TestListeners.extentTest.get().fail("Error in enabling external vendor flag " + e);
		}
		return false;

	}

	public boolean masking() {
		try {
			utils.getLocator("instanceDashboardPage.enableUnmasking").isDisplayed();
			if (utils.getLocator("instanceDashboardPage.maskingCheckbox").isSelected()) {
				utils.getLocator("instanceDashboardPage.enableUnmasking").click();
				utils.getLocator("instanceDashboardPage.updateButton").click();
				logger.info("masking is enabled");
				TestListeners.extentTest.get().info("masking is enabled");
				return false;
			} else {
				logger.info("masking is enabled");
				TestListeners.extentTest.get().info("masking is enabled");
			}

		} catch (Exception e) {
			logger.error("Error in enabling masking flag " + e);
			TestListeners.extentTest.get().fail("Error in enabling masking flag " + e);
		}
		return true;
	}

	public boolean Unmasking() {
		try {
			utils.getLocator("instanceDashboardPage.enableUnmasking").isDisplayed();
			if (!(utils.getLocator("instanceDashboardPage.maskingCheckbox").isSelected())) {
				utils.getLocator("instanceDashboardPage.enableUnmasking").click();
				utils.getLocator("instanceDashboardPage.updateButton").click();
				logger.info("Unmasking flag enabled");
				TestListeners.extentTest.get().info("Unmasking is enabled");
				return true;
			} else {
				logger.info("Unmasking is enabled");
				TestListeners.extentTest.get().info("Unmasking is enabled");
			}
		} catch (Exception e) {
			logger.error("Error in enabling unmasking " + e);
			TestListeners.extentTest.get().fail("Error in enabling unmasking" + e);
		}
		return false;
	}

	public void select_use_for_sso() {
		utils.getLocator("instanceDashboardPage.whitelabel").click();
		utils.getLocator("instanceDashboardPage.oauth_link").click();
		// boolean b = utils.existsElement("oauthAppPage.USE_FOR_SSO");
		selUtils.implicitWait(50);
		if (utils.existsElement("oauthAppPage.REMOVE_FOR_SSO")) {
			TestListeners.extentTest.get().pass("USE FOR SSO is already selected in Cockpit section");
			logger.info("USE FOR SSO is selected in Cockpit section");
		} else if (utils.getLocator("oauthAppPage.USE_FOR_SSO").isDisplayed()) {
			utils.getLocator("oauthAppPage.USE_FOR_SSO").click();
			logger.info("USE FOR SSO is clicked in Cockpit section");
			driver.switchTo().alert().accept();
			if (utils.existsElement("oauthAppPage.REMOVE_FOR_SSO")) {
				TestListeners.extentTest.get().pass("USE FOR SSO is selected in Cockpit section");
				logger.info("USE FOR SSO is selected in Cockpit section");
			}
		}
	}

	public void unselect_use_for_sso() {
		if (utils.existsElement("oauthAppPage.REMOVE_FOR_SSO")) {
			utils.getLocator("oauthAppPage.REMOVE_FOR_SSO").click();
			logger.info("REMOVE_FOR_SSO is selected in Cockpit section");
			TestListeners.extentTest.get().pass("USE FOR SSO is unselected in Cockpit section");
		}
	}

	public void un_select_cockpit_dashboard_miscalleaneous_tab_external_source_id_flag() {
		selUtils.longWait(1000);
		utils.getLocator("instanceDashboardPage.cockpit").click();
		utils.getLocator("instanceDashboardPage.dashboard_link").click();
		utils.getLocator("instanceDashboardPage.miscellaneous_config_tab").click();
		selUtils.longWait(100);

		if (((utils.getLocator("instanceDashboardPage.external_source_id").isSelected()))) {

			utils.getLocator("instanceDashboardPage.external_source_id_label").click();
			utils.getLocator("instanceDashboardPage.update_btn").click();
			if (!(utils.getLocator("instanceDashboardPage.external_source_id").isSelected())) {
				TestListeners.extentTest.get().pass("External source id flag is unselected in Cockpit section");
				logger.info("External source id flag is unselected in Cockpit section");
			}
		}
	}

	public void select_cockpit_dashboard_miscalleaneous_tab_external_source_id_flag() {
		try {
			utils.getLocator("instanceDashboardPage.miscellaneous_config_tab").isDisplayed();
			utils.getLocator("instanceDashboardPage.miscellaneous_config_tab").click();
			selUtils.longWait(200);

			if ((!(utils.getLocator("instanceDashboardPage.external_source_id").isSelected()))) {

				utils.getLocator("instanceDashboardPage.external_source_id_label").click();
				utils.getLocator("instanceDashboardPage.update_btn").click();
				if ((utils.getLocator("instanceDashboardPage.external_source_id").isSelected())) {
					TestListeners.extentTest.get().pass("External source id flag is selected in Cockpit section");
					logger.info("External source id flag is selected in Cockpit section");
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void select_external_identifier_updation_idp_drpdwn(String external_source) {
		utils.getLocator("instanceDashboardPage.miscellaneous_config_tab").isDisplayed();
		utils.getLocator("instanceDashboardPage.miscellaneous_config_tab").click();
		utils.getLocator("instanceDashboardPage.externalIdentifierUpdationIDPdrpdown").click();
		;

		List<WebElement> dropdownLi = utils
				.getLocatorList("instanceDashboardPage.externalIdentifierUpdationIDPdrpdownList");
		utils.selectListDrpDwnValue(dropdownLi, external_source);
		utils.getLocator("instanceDashboardPage.update_btn").click();
		logger.info("External identifier updation idp dropdown value selected as ==> " + external_source);
		TestListeners.extentTest.get()
				.pass("External identifier updation idp dropdown value selected as ==> " + external_source);
	}

	// public void
	// un_select_cockpit_dashboard_miscalleaneous_tab_external_source_id_flag() {
	// selUtils.longWait(1000);
	// utils.getLocator("instanceDashboardPage.miscellaneous_config_tab").click();
	// selUtils.longWait(100);
	//
	// if
	// (((utils.getLocator("instanceDashboardPage.external_source_id").isSelected())))
	// {
	//
	// utils.getLocator("instanceDashboardPage.external_source_id_label").click();
	// utils.getLocator("instanceDashboardPage.update_btn").click();
	// if
	// (!(utils.getLocator("instanceDashboardPage.external_source_id").isSelected()))
	// {
	// TestListeners.extentTest.get().pass("External source id flag is unselected in
	// Cockpit section");
	// logger.info("External source id flag is unselected in Cockpit section");
	// }
	// }
	// }

	public void selectSettingsLineItemSelectors() {
		try {
			// utils.getLocator("instanceDashboardPage.settings").isDisplayed();
			// utils.getLocator("instanceDashboardPage.settings").click();
			utils.getLocator("instanceDashboardPage.lineItemSelectors").isDisplayed();
			utils.getLocator("instanceDashboardPage.lineItemSelectors").click();
			logger.info("Selected Line Item Selectors option under Settings in the dashboard");
			TestListeners.extentTest.get().info("Selected Line Item Selectors option under Settings in the dashboard");
		} catch (Exception e) {
			logger.info("Error in selecting Line Item Selectors option under Settings in the dashboard");
			TestListeners.extentTest.get()
					.info("Error in selecting Line Item Selectors option under Settings in the dashboard");
		}
	}

	public void selectSettingsQualificationCriteria() {
		try {
			utils.getLocator("instanceDashboardPage.settings").isDisplayed();
			utils.getLocator("instanceDashboardPage.settings").click();
			utils.getLocator("instanceDashboardPage.QualificationCriteria").isDisplayed();
			utils.getLocator("instanceDashboardPage.QualificationCriteria").click();
			logger.info("Selected QualificationCriteria option under Settings in the dashboard");
			TestListeners.extentTest.get()
					.info("Selected QualificationCriteria option under Settings in the dashboard");

		} catch (Exception e) {
			logger.info("Error in selecting QualificationCriteria option under Settings in the dashboard");
			TestListeners.extentTest.get()
					.info("Error in selecting QualificationCriteria option under Settings in the dashboard");
		}
	}

	public void closeBanner() {
		WebElement okBtn = driver.findElement(By.xpath("//button[text()='Okay, got it!']"));
		if (okBtn.isDisplayed()) {
			okBtn.click();
		}
	}

	public void assertDashboard() {
		utils.getLocator("instanceDashboardPage.adminName").isDisplayed();
		String actualname = utils.getLocator("instanceDashboardPage.adminName").getText();
		String expectedname = "Welcome Syeda";
		Assert.assertEquals(actualname, expectedname);// To validate if directed to expected page
		logger.info("Assertion for selecting business passed");
		TestListeners.extentTest.get().info("Assertion for selecting business passed");
	}

	public void iframeconfiguration() {
		Actions action = new Actions(driver);
		WebElement menuOption = utils.getLocator("instanceDashboardPage.dashboardOption");
		action.moveToElement(menuOption).perform();
		utils.getLocator("instanceDashboardPage.pinlabel").isDisplayed();
		utils.getLocator("instanceDashboardPage.pinlabel").click();
		utils.getLocator("instanceDashboardPage.clickoption").isDisplayed();
		utils.getLocator("instanceDashboardPage.clickoption").click();
		utils.getLocator("instanceDashboardPage.whitelabelopt").isDisplayed();
		utils.getLocator("instanceDashboardPage.whitelabelopt").click();
		List<WebElement> values = utils.getLocatorList("instanceDashboardPage.dropdwnlbl");
		// List<WebElement> values = driver.findElements(By.xpath("//li[@class='nav-item
		// dropdown open']//ul//li"));
		for (int i = 1; i <= values.size(); i++) {
			if (values.get(i).getText().contains("iFrame Configuration")) {
				values.get(i).click();
				break;
			}
		}
		List<WebElement> element = utils.getLocatorList("instanceDashboardPage.iframeoption");
		// List<WebElement>
		// element=driver.findElements(By.xpath("//input[contains(@id,'iframe_configuration')]"));
		for (int i = 1; i < 20; i++) {
			String js = "arguments[0].style.visibility='visible';";
			((JavascriptExecutor) driver).executeScript(js, element.get(i));
			String val = element.get(i).getAttribute("checked");
			if (val == null) {
				JavascriptExecutor jse = (JavascriptExecutor) driver;
				jse.executeScript("arguments[0].click();", element.get(i));
				String value = element.get(i).getAttribute("name");
				System.out.println(value);

			}
		}
		utils.getLocator("instanceDashboardPage.updateopt").isDisplayed();
		utils.getLocator("instanceDashboardPage.updateopt").click();
		String actualiframemessage = "Iframe configuration updated";
		String expectediframemsg = utils.getLocator("instanceDashboardPage.iframesuccessmsg").getText();
		// String expectediframemsg=driver.findElement(By.xpath("//strong[text()='Iframe
		// configuration updated']")).getText();
		Assert.assertEquals(actualiframemessage, expectediframemsg);
		logger.info("iframe fields updated successfully");
		TestListeners.extentTest.get().info("iframe fields updated successfully");
	}

	public void iframepagevalidation() {
		String businessnm = "My Biz";
		String expectedbString = utils.getLocator("instanceDashboardPage.mybiztext").getText();
		Assert.assertEquals(businessnm, expectedbString);
		String expectedsign = "SIGN UP";
		String actualsign = utils.getLocator("instanceDashboardPage.signupopt").getText();
		Assert.assertEquals(expectedsign, actualsign);
		String expectedlogin = "LOGIN";
		// String
		// actuallogin=driver.findElement(By.xpath("//a[text()='Login']")).getText();
		String actuallogin = utils.getLocator("instanceDashboardPage.loginopt").getText();
		Assert.assertEquals(expectedlogin, actuallogin);
		String expectedtxt = "PHYSICAL LOYALTY CARD REGISTRATION";
		String actualtxt = utils.getLocator("instanceDashboardPage.physicaloyaltyopt").getText();
		// String actualtxt=driver.findElement(By.xpath("//a[text()='Physical Loyalty
		// Card Registration']")).getText();
		Assert.assertEquals(expectedtxt, actualtxt);
		logger.info("Iframe page loaded successfully");
		TestListeners.extentTest.get().info("Iframe page loaded successfully");
	}

	public void goToIntegrationServiceLogs() {
		try {
			utils.getLocator("instanceDashboardPage.giftCardIntegrationServiceLog").click();
			utils.getLocator("instanceDashboardPage.integrationServiceType").click();
			utils.getLocator("instanceDashboardPage.giftCardLogServiceTypePayments").click();
			utils.getLocator("dashboardPage.dataRangeTextBox").click();
			utils.getLocator("giftCardsPage.giftCardLogTimeToday").click();
		} catch (Exception e) {
			logger.error("Error in navigating to gift Card logs " + e);
			TestListeners.extentTest.get().fail("Error in navigating to gift Card logs " + e);
		}
	}

	// // shashank
	// public void signOutFromSuperUser() {
	// utils.getLocator("instanceLoginPage.superAdminImage").click();
	// utils.getLocator("instanceLoginPage.signOutLink").click();
	// selUtils.implicitWait(5);
	// }

	public void transactionTypeAndTime(String transactionType) {
		utils.clickByJSExecutor(driver, utils.getLocator("dashboardPage.dataRangeTextBox"));
		// utils.getLocator("dashboardPage.dataRangeTextBox").click();
		utils.clickByJSExecutor(driver, utils.getLocator("giftCardsPage.giftCardLogTimeToday"));
//			utils.getLocator("giftCardsPage.giftCardLogTimeToday").click();
		utils.getLocator("giftCardsPage.transactionTypeDrp").click();
		List<WebElement> ele = utils.getLocatorList("giftCardsPage.transactionTypeList");
		utils.selectListDrpDwnValue(ele, transactionType);
		utils.waitTillInVisibilityOfElement(utils.getLocator("giftCardsPage.legacyPaymentGiftCardLogs"),
				"grit card logs");
	}

	public void serviceTypeAndTime(String serviceTypeList) {
		boolean display = false;
		// utils.clickByJSExecutor(driver,
		// utils.getLocator("instanceDashboardPage.giftCardIntegrationServiceLog"));
		// utils.getLocator("instanceDashboardPage.giftCardIntegrationServiceLog").click();
		int attempts = 0;
		while (attempts < 7) {
			refreshPage();
			Utilities.longWait(2000);
			try {
				utils.getLocator("giftCardsPage.serviceTypeDrp").click();
				List<WebElement> ele = utils.getLocatorList("giftCardsPage.serviceTypeList");
				utils.selectListDrpDwnValue(ele, serviceTypeList);
				display = utils.getLocator("giftCardsPage.integrationServicesLogs").isDisplayed();
				display = true;
				logger.info("Gift card option is selected from dropdown");
				TestListeners.extentTest.get().info("Gift card option is selected from dropdown");
				break;
			} catch (Exception e) {
				logger.info("Element is not present " + e.getMessage());
				TestListeners.extentTest.get().fail("Element is not present " + e.getMessage());
			}
			attempts++;
		}
		utils.getLocator("dashboardPage.dataRangeTextBox").click();
		utils.getLocator("giftCardsPage.giftCardLogTimeToday").click();

		logger.error("selecting date and time");
		TestListeners.extentTest.get().pass("selecting date and time");

	}

	public boolean verifyDeletedGuests(String email) {
		try {
			utils.getLocator("instanceDashboardPage.searchGuest").isDisplayed();
			utils.getLocator("instanceDashboardPage.searchGuest").clear();
			utils.getLocator("instanceDashboardPage.searchGuest").sendKeys(email);
			utils.getLocator("instanceDashboardPage.searchGuest").submit();
			logger.info("Checking if guests exists: " + email);
			TestListeners.extentTest.get().info("Checking if guests exists: " + email);
			selUtils.implicitWait(10);
			if (utils.getLocatorList("instanceDashboardPage.searchGuest").size() > 0) {
				logger.info("Guest doesn't exists: " + email);
				TestListeners.extentTest.get().info("Guest doesn't exists: " + email);
				return true;
			} else {
				logger.error("Error in verifying new user " + email);
				TestListeners.extentTest.get().info("Error in verifying new user " + email);
			}
		} catch (Exception e) {
		}
		return false;
	}

	public void checkExternalSourceIdFlag() {
		utils.getLocator("instanceDashboardPage.miscellaneous_config_tab").isDisplayed();
		utils.getLocator("instanceDashboardPage.miscellaneous_config_tab").click();
		selUtils.longWait(200);
		if ((!(utils.getLocator("instanceDashboardPage.external_source_id").isSelected()))) {
			utils.clickByJSExecutor(driver, utils.getLocator("instanceDashboardPage.clickSExternalSourceId"));
			// utils.getLocator("instanceDashboardPage.clickSExternalSourceId").click();
			logger.info("External Identifier Updation IDP is checked");
			TestListeners.extentTest.get().info("External Identifier Updation IDP is checked");
		} else {
			logger.info("External Identifier Updation IDP was already checked");
			TestListeners.extentTest.get().info("External Identifier Updation IDP was already checked");
		}
	}

	public List<String> externalIdentifierUpdationIDPList() {
		utils.getLocator("instanceDashboardPage.externalIdentifierUpdationdrp").click();
		List<WebElement> list = utils.getLocatorList("instanceDashboardPage.externalIdentifierUpdationList");
		List<String> tabNames = list.stream().map(s -> s.getText()).collect(Collectors.toList());
		return tabNames;
	}

	public void setExternalSourceIdFlag(String adapter) {
		String externalIdentifierUpdation = (utils.getLocator("instanceDashboardPage.verfifyexternalIdentifierUpdation")
				.getText());
		if (externalIdentifierUpdation != adapter) {
			// utils.getLocator("instanceDashboardPage.external_source_id_label").click();
			utils.getLocator("instanceDashboardPage.externalIdentifierUpdationdrp").click();
			List<WebElement> ele = utils.getLocatorList("instanceDashboardPage.externalIdentifierUpdationList");
			utils.selectListDrpDwnValue(ele, adapter);
			utils.clickByJSExecutor(driver, utils.getLocator("instanceDashboardPage.update_btn"));
			// utils.getLocator("instanceDashboardPage.update_btn").click();
			logger.info("External Identifier Updation IDP is selected as : " + adapter);
			TestListeners.extentTest.get().info("External Identifier Updation IDP is selected as : " + adapter);
		} else {
			logger.info("External Identifier Updation IDP was already checked");
			TestListeners.extentTest.get().info("External Identifier Updation IDP was already checked");
		}
	}

	public void enableGenerationOfAccessTokensFlag() {
		utils.getLocator("instanceDashboardPage.miscellaneous_config_tab").isDisplayed();
		utils.getLocator("instanceDashboardPage.miscellaneous_config_tab").click();
		selUtils.longWait(200);
		if ((!(utils.getLocator("instanceDashboardPage.enableGenerationOfAccessTokens").isSelected()))) {
			utils.clickByJSExecutor(driver, utils.getLocator("instanceDashboardPage.enableGenerationOfAccessTokens"));
			// utils.getLocator("instanceDashboardPage.enableGenerationOfAccessTokens").click();
			utils.getLocator("instanceDashboardPage.update_btn").click();
			logger.info("Generation Of Access Tokens Flag is checked");
			TestListeners.extentTest.get().info("Generation Of Access Tokens Flag is checked");
		} else {
			logger.info("External Identifier Updation IDP was already checked");
			TestListeners.extentTest.get().info("Generation Of Access Tokens Flag was already checked");
		}
	}

	public void enableOfferDistributionFromExternalSystemFlag() {
		utils.getLocator("instanceDashboardPage.miscellaneous_config_tab").isDisplayed();
		utils.getLocator("instanceDashboardPage.miscellaneous_config_tab").click();
		selUtils.longWait(200);
		if ((!(utils.getLocator("instanceDashboardPage.clickOfferDistributionFromExternalSystem").isSelected()))) {
			utils.clickByJSExecutor(driver,
					utils.getLocator("instanceDashboardPage.clickOfferDistributionFromExternalSystem"));
			// utils.getLocator("instanceDashboardPage.clickOfferDistributionFromExternalSystem").click();
			utils.getLocator("instanceDashboardPage.update_btn").click();
			logger.info("External Identifier Updation IDP is checked");
			TestListeners.extentTest.get().info("External Identifier Updation IDP is checked");
		} else {
			logger.info("External Identifier Updation IDP was already checked");
			TestListeners.extentTest.get().info("External Identifier Updation IDP was already checked");
		}
	}

	public void clickOnGenerateBarcodeButton() {
		utils.scrollToElement(driver, utils.getLocator("instanceDashboardPage.generateBarcodeButton"));
		// utils.getLocator("instanceDashboardPage.generateBarcodeButton").isDisplayed();
		selUtils.jsClick(utils.getLocator("instanceDashboardPage.generateBarcodeButton"));
		utils.waitTillCompletePageLoad();
		logger.info("Clicked on generate bar code");
		TestListeners.extentTest.get().info("Clicked on generate bar code");

	}

	public void generateBarcodeWithTransactionNumber(String locationName, String TransactionNumber, String minAmount,
			String maxAmount) {
		refreshPage();
		logger.info("==Setting test bar code info==");
		TestListeners.extentTest.get().info("==Setting test bar code info==");
		WebElement locationDropdown = utils.getLocator("instanceDashboardPage.testbarcodesLocationDrpDown");
		locationDropdown.isDisplayed();
		locationDropdown.click();
		if (locationName == null || locationName == "")
			utils.getLocator("instanceDashboardPage.firstLocation").click();
		else
			utils.selectValueFromSpanExpandedDropdown(locationName);
		utils.getLocator("instanceDashboardPage.dateTextbox").isDisplayed();
		utils.getLocator("instanceDashboardPage.dateTextbox").click();
		utils.getLocator("instanceDashboardPage.dateApplyButton").isDisplayed();
		utils.getLocator("instanceDashboardPage.dateApplyButton").click();
		utils.getLocator("instanceDashboardPage.minAmoutTextbox").isDisplayed();
		utils.getLocator("instanceDashboardPage.minAmoutTextbox").clear();
		utils.getLocator("instanceDashboardPage.minAmoutTextbox").sendKeys(minAmount);
		utils.getLocator("instanceDashboardPage.maxAmoutTextbox").isDisplayed();
		utils.getLocator("instanceDashboardPage.maxAmoutTextbox").clear();
		utils.getLocator("instanceDashboardPage.maxAmoutTextbox").sendKeys(maxAmount);
		logger.info("Min and Max amout set as: " + minAmount + " and " + maxAmount);
		TestListeners.extentTest.get().info("Min and Max amout set as: " + minAmount + " and " + maxAmount);
		utils.getLocator("instanceDashboardPage.transaction_no").sendKeys(TransactionNumber);

	}

	public String captureBarcodeWithCurrentDateAndTime() throws ParseException, InterruptedException {
		// utils.waitTillPagePaceDone();
		SimpleDateFormat parser = new SimpleDateFormat("HH:mm");
		Date twelve = parser.parse("12:00");
		Date twentyThree = parser.parse("23:00");

		Calendar cal = Calendar.getInstance();
		SimpleDateFormat dateOnly = new SimpleDateFormat("MM/dd/yy");
		String crntdateStr = dateOnly.format(cal.getTime());
		Date currentDate = dateOnly.parse(crntdateStr);

		int counter = 1;
		String parentWindow = selUtils.switchToNewWindow();
		utils.longwait(5000);
//		Assert.assertTrue(utils.getLocatorList("instanceDashboardPage.testBarcodeHeader").size() > 0,
//                "Barcode is not generated successfully ");
		if (utils.getLocatorList("instanceDashboardPage.testBarcodeHeader").size() > 0) {
			// utils.refreshPage();
			utils.waitTillVisibilityOfElement(utils.getLocator("instanceDashboardPage.barcodeTargetLeftPanel"),
					"barcode list");
			List<WebElement> barcodeTimeWEleList = utils.getLocatorList("instanceDashboardPage.barcodeTargetLeftPanel");
			for (WebElement wEle : barcodeTimeWEleList) {

				String originalString = wEle.getText();
				LocalDateTime dateTime = LocalDateTime.parse(originalString,
						DateTimeFormatter.ofPattern("MM/dd/yy HH:mm:ss"));

				String newString = DateTimeFormatter.ofPattern("H:mm").format(dateTime);
				Date barcodeDate = parser.parse(newString);

				String todayDate = DateTimeFormatter.ofPattern("MM/dd/yy").format(dateTime);
				Date currentDateFromUI = dateOnly.parse(todayDate);

				if (barcodeDate.after(twelve) && barcodeDate.before(twentyThree)
						&& currentDateFromUI.equals(currentDate)) {
					barcodeValue = driver
							.findElement(By.xpath("//h3[text()='Valid']/following-sibling::table[1]//td[text()='"
									+ counter + "']/following-sibling::td[2]/div"))
							.getAttribute("data-barcode");
					break;
				}
				counter++;
			}
		}
		Assert.assertTrue(barcodeValue != null && !barcodeValue.isEmpty(), "Barcode is not generated successfully ");

		logger.info("Time is exist between 12 to 23 and the barcode is " + barcodeValue);
		TestListeners.extentTest.get().pass("Time is exist between 12 to 23 and the barcode is " + barcodeValue);
		// selUtils.switchToWindow(parentWindow);
		return barcodeValue;
	}

	// Reselecting the slug if application logout after selecting the slug
	public void selectBusinessNew(String instanceAccount) throws Exception {
		utils.waitTillPagePaceDone();
		if (driver.getCurrentUrl().contains(prodUrl)) {
			logger.info("Prod instance is by default selected");
		} else {
			clickOnSlug(instanceAccount);
			List<WebElement> eleList = utils.getLocatorList("instanceDashboardPage.whitelabel");
			if (eleList.size() != 0) {
				utils.getLocator("instanceDashboardPage.whitelabel").click();
				utils.getLocator("menuPage.whitelabelServices").click();
				logger.info("Clicked services in whitelabel");
				utils.waitTillPagePaceDone();
				utils.getLocator("menuPage.dashboardMenu").click();
				logger.info("Clicked dashboard Menu");

				// Reselect business if dashboard not loaded properly
				if (driver.getCurrentUrl().contains("businesses")) {
					String instanceXpath = utils.getLocatorValue("instanceDashboardPage.instanceAccountXpath")
							.replace("${instanceAccount}", instanceAccount);
					WebElement ele = driver.findElement(By.xpath(instanceXpath));
					utils.StaleElementclick(driver, ele);
					logger.info("Reselected business: " + instanceAccount);
					TestListeners.extentTest.get().info("Reselected business: " + instanceAccount);
					utils.waitTillPagePaceDone();
				}

			} // size is 0 means slug is not selected properly/// checking is application
				// logout
			else {
				if (counter > 0) {
					counter--;
					loginToInstance();
					selectBusinessNew(instanceAccount);
				}

			}

		}
	}

	public void clickOnSlug(String slugName) {
		utils.waitTillPagePaceDone();
		String instanceXpath = utils.getLocatorValue("instanceDashboardPage.instanceAccountXpath")
				.replace("${instanceAccount}", slugName);
		WebElement businessTile = driver.findElement(By.xpath(instanceXpath));
		utils.StaleElementclick(driver, businessTile);
		logger.info("Selected business: " + slugName);
		TestListeners.extentTest.get().info("Selected business: " + slugName);
		utils.waitTillPagePaceDone();
	}

	public int getBusinessCount() {
		List<WebElement> listOfBusiness = utils.getLocatorList("instanceDashboardPage.listOfBusinessXpath");
		int count = listOfBusiness.size() + 1;
		System.out.println("listOfBusiness size -- " + count);
		return count;
	}

	public List<String> getBusinessNameList() {
		List<String> listOfBusinessNames = new ArrayList<String>();
		List<WebElement> listOfBusiness = utils.getLocatorList("instanceDashboardPage.listOfBusinessXpath");
		for (WebElement ele : listOfBusiness) {
			listOfBusinessNames.add(ele.getText());
		}
		return listOfBusinessNames;
	}

	public void checkUncheckFlagOnCockpitDasboard(String flagName, String checkBoxFlag) {

		WebElement enableSTOCheckbox = driver
				.findElement(By.xpath("//label[text()='" + flagName + "']/preceding-sibling::input[1]"));
		String checkBoxValue = enableSTOCheckbox.getAttribute("checked");

		if ((checkBoxValue == null) && (checkBoxFlag.equalsIgnoreCase("uncheck"))) {
			logger.info("Auto checking box Unchecked and do not cliked it as checkBoxFlag= " + checkBoxFlag);
			TestListeners.extentTest.get()
					.pass("Auto checking box Unchecked and do not cliked it as checkBoxFlag= " + checkBoxFlag);
		} else if ((checkBoxValue == null) && (checkBoxFlag.equalsIgnoreCase("check"))) {
			utils.clickByJSExecutor(driver, enableSTOCheckbox);
			utils.getLocator("dashboardPage.updateBtn").click();
			logger.info("Autocheckin box is unchecked and user want to check the chekedbox");
			TestListeners.extentTest.get().pass("Autocheckin box is unchecked and user want to check the chekedbox");

		} else if (checkBoxValue.equalsIgnoreCase("true") && (checkBoxFlag.equalsIgnoreCase("uncheck"))) {
			utils.clickByJSExecutor(driver, enableSTOCheckbox);
			utils.getLocator("dashboardPage.updateBtn").click();

			logger.info("Autocheckin box is already cheked and user want to uncheck ");
			TestListeners.extentTest.get().pass("Autocheckin box is already cheked and user want to uncheck ");
		} else if (checkBoxValue.equalsIgnoreCase("true") && (checkBoxFlag.equalsIgnoreCase("check"))) {

			logger.info("Autocheckin box is already checked and user want to check the chekedbox, so do not click");
			TestListeners.extentTest.get()
					.pass("Autocheckin box is already checked and user want to check the chekedbox, so do not click");

		}

		utils.getLocator("dashboardPage.updateBtn").click();
		// utils.waitTillPagePaceDone();
	}

	public void checkUncheckFlagOnCockpitDasboardCase(String flagName, String checkBoxFlagValue) {

		WebElement checkBoxFlag = driver
				.findElement(By.xpath("//label[text()='" + flagName + "']/preceding-sibling::input[1]"));
		String checkBoxFlagCurrentStatus = checkBoxFlag.getAttribute("checked");

		switch (checkBoxFlagValue) {
		case "check":
			if (checkBoxFlagCurrentStatus == null) {
				utils.clickByJSExecutor(driver, checkBoxFlag);
				utils.getLocator("dashboardPage.updateBtn").click();
				logger.info(flagName + " checked successfully");
				TestListeners.extentTest.get().info(flagName + " checked successfully");
			} else {
				logger.info(flagName + " is already checked...");
				TestListeners.extentTest.get().info(flagName + " is already checked...");
			}

			break;
		case "uncheck":
			if (checkBoxFlagCurrentStatus != null) {
				utils.clickByJSExecutor(driver, checkBoxFlag);
				utils.getLocator("dashboardPage.updateBtn").click();
				logger.info(flagName + " unchecked successfully");
				TestListeners.extentTest.get().info(flagName + " unchecked successfully");
			} else {
				logger.info(flagName + " is already unchecked...");
				TestListeners.extentTest.get().info(flagName + " is already unchecked...");
			}

			break;
		}
	}

	public void navigateToSubMenuItem(String newMenueName, String newSubMenuName) throws InterruptedException {
		boolean status = false;
		WebElement menuSearch = utils.getLocator("menuPage.menuSearch");
		menuSearch.click();
		logger.info("Clicked on menu search box");
		TestListeners.extentTest.get().info("Clicked on menu search box");
		utils.longWaitInSeconds(1);
		// enter submenu name
		WebElement subMenuSearch = utils.getLocator("menuPage.subMenuSearch");
		subMenuSearch.clear();
		subMenuSearch.sendKeys(newSubMenuName);
		logger.info("Entered submenu name in search box");
		TestListeners.extentTest.get().info("Entered submenu name in search box");
		utils.longWaitInSeconds(1);
		String subMenuxpath = utils.getLocatorValue("menuPage.subMenu").replace("$temp", newMenueName);
		subMenuxpath = subMenuxpath.replace("temp", newSubMenuName);

		WebElement subMenu = driver.findElement(By.xpath(subMenuxpath));
		utils.waitTillElementToBeClickable(subMenu);
		// subMenu.click();
		utils.clickByJSExecutor(driver, subMenu);
		logger.info("Clicked on menu >> sub menu option :" + newMenueName + " >> " + newSubMenuName);
		TestListeners.extentTest.get()
				.info("Clicked on menu >> sub menu option :" + newMenueName + " >> " + newSubMenuName);
		utils.waitTillPagePaceDone();
	}

	public void selectBusinessNewNavMenu(String instanceAccount, String choice) throws InterruptedException {
		utils.waitTillPagePaceDone();
		if (driver.getCurrentUrl().contains(prodUrl)) {
			logger.info("Prod instance is by default selected");
		} else {

			String instanceXpath = utils.getLocatorValue("instanceDashboardPage.instanceAccountXpath")
					.replace("${instanceAccount}", instanceAccount);
			WebElement businessTile = driver.findElement(By.xpath(instanceXpath));
			utils.StaleElementclick(driver, businessTile);
			logger.info("Selected business: " + instanceAccount);
			TestListeners.extentTest.get().info("Selected business: " + instanceAccount);
			utils.waitTillPagePaceDone();
			boolean status = false;
			try {
				WebElement menuSearch = utils.getLocator("menuPage.menuSearch");
				status = utils.checkElementPresent(menuSearch);
			} catch (Exception e) {
			}

			if (status) {
				utils.getLocator("menuPage.newCockpitMenu").click();
				utils.getLocator("menuPage.cockpitDashboardLink").click();
			} else {
				utils.getLocator("menuPage.cockpitMenu").click();
				utils.getLocator("menuPage.cockpitDashboardLink").click();
			}

			// click cockpit dashboard for complete page load
//			try {
//				utils.getLocator("menuPage.newCockpitMenu").click();
//				utils.getLocator("menuPage.cockpitDashboardLink").click();
//			} catch (Exception e) {
//				utils.getLocator("menuPage.cockpitMenu").click();
//				utils.getLocator("menuPage.cockpitDashboardLink").click();
//			}
			/*
			 * try { navigateToSubMenuItem("Cockpit", "Dashboard"); } catch (Exception e) {
			 * 
			 * }
			 */
			switch (choice) {
			case "check":
				checkUncheckFlagOnCockpitDasboard("Enable New SideNav, Header and Footer?", "check");
				logger.info("Enable New SideNav, Header and Footer? checkbox has been checked");
				TestListeners.extentTest.get().info("Enable New SideNav, Header and Footer? checkbox has been checked");
				break;
			case "uncheck":
				checkUncheckFlagOnCockpitDasboard("Enable New SideNav, Header and Footer?", "uncheck");
				logger.info("Enable New SideNav, Header and Footer? checkbox has been unchecked");
				TestListeners.extentTest.get()
						.info("Enable New SideNav, Header and Footer? checkbox has been unchecked");
				break;
			}
//			checkUncheckFlagOnCockpitDasboard("Enable New SideNav, Header and Footer?", check);

			// Reselect business if dashboard not loaded properly
			if (driver.getCurrentUrl().contains("businesses?filter=all")) {
				WebElement ele = driver.findElement(By.xpath(instanceXpath));
				utils.StaleElementclick(driver, ele);
				logger.info("Reselected business: " + instanceAccount);
				TestListeners.extentTest.get().info("Reselected business: " + instanceAccount);
				utils.waitTillPagePaceDone();
			}
		}
	}

	public void navigateToGuestTimeline_newUpdated(String email) throws InterruptedException {
		@SuppressWarnings("unused")
		boolean flag = false;
		int attempts = 0;
		utils.waitTillPagePaceDone();
		utils.getLocator("instanceDashboardPage.guestSearchLabel").isDisplayed();
		utils.getLocator("instanceDashboardPage.guestSearchLabel").click();
		utils.longWaitInSeconds(2);
		while (attempts <= 10) {
			try {
				WebElement searchedBoxWele = utils.getLocator("instanceDashboardPage.guestSearchBox");
				searchedBoxWele.click();
				searchedBoxWele.clear();
				searchedBoxWele.sendKeys(email);
				utils.longWaitInSeconds(2);
				utils.implicitWait(2);
				List<WebElement> searchedList = utils.getLocatorList("instanceDashboardPage.searchedGuestListXpath");
				if (searchedList.size() != 0 && searchedList.size() == 1) {
					searchedList.get(0).click();
					flag = true;
					utils.longWaitInSeconds(3);
					TestListeners.extentTest.get().info("Searching new user: " + email);
					utils.waitTillPagePaceDone();
					String str = utils.getLocator("guestTimeLine.timelineBtn").getText();
					if (str.equalsIgnoreCase("Timeline")) {
						flag = true;
						logger.info("Guest email found and moved to timeline");
						TestListeners.extentTest.get().pass("Guesst email found and moved to timeline: " + email);
						break;
					}
				} else {
					logger.error("Inside else Guest did not found trying again " + email);
				}

			} catch (Exception e) {
				logger.error("Guest did not found trying again " + email);
			}
			attempts++;
		}
		Assert.assertTrue(flag, email + " user email is not found in User search ");
		utils.implicitWait(50);
	}

	public void setSendEmailVerification() throws InterruptedException {
		menuPage.navigateToSubMenuItem("Cockpit", "Guests");
		utils.getLocator("CockpitGuestPage.guestValidationBtn").click();
		checkUncheckFlagOnCockpitDasboard("Send Email Verification", "check");
	}

	public void LoginThroughForgetPasswordLink(String Url, String newPassword) throws InterruptedException {
		driver.get(Url);
		utils.getLocator("iframeSignUpPage.passwordTextbox").isDisplayed();
		utils.getLocator("iframeSignUpPage.passwordTextbox").sendKeys(newPassword);
		utils.getLocator("iframeSignUpPage.confPasswordTextbox").isDisplayed();
		utils.getLocator("iframeSignUpPage.confPasswordTextbox").sendKeys(newPassword);
		utils.getLocator("instanceDashboardPage.changeMyPasswordButton").isDisplayed();
		utils.getLocator("instanceDashboardPage.changeMyPasswordButton").click();
		utils.getLocator("instanceDashboardPage.ResetPasswordSuccessMsg").isDisplayed();
		logger.info("Password changed successfully through forget password link");
		TestListeners.extentTest.get().info("Password changed successfully through forget password link");

	}

	public void confirmEmail(String Url) throws InterruptedException {
		driver.get(Url);
		utils.getLocator("instanceDashboardPage.emailConfirmationSuccessMsg").isDisplayed();
		driver.navigate().back();
		logger.info("Email confirmed successfully through forget password link");
		TestListeners.extentTest.get().info("Email confirmed successfully through forget password link");

	}
}
