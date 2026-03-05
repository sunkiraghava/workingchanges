package com.punchh.server.pages;

import java.util.List;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class IframeFieldValidationPage {
	static Logger logger = LogManager.getLogger(IframeFieldValidationPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	String iframeEmail, iframeEmailinvalid, temp = "temp";
	String iframeEmailvalid;
	String iframeEmaildata;

	public IframeFieldValidationPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
	}

	public void navigateToIframe(String url) {
		try {
			driver.navigate().to(url);
			utils.getLocator("iframePage.singUpLink").isDisplayed();
			utils.getLocator("iframePage.loginLink").isDisplayed();
			logger.info("Navigated to iframe signup/login page");
			TestListeners.extentTest.get().info("Navigated to iframe signup/login page");
		} catch (Exception e) {
			logger.info("Error in navigating to iFrame signup/login" + e);
			TestListeners.extentTest.get().info("Error in navigating to iFrame signup/login" + e);
		}
	}

	public String iframeSignUpField() {
		String parentWindow;
		logger.info("==Signing up in iframe==");
		TestListeners.extentTest.get().info("==Signing up in iframe==");
		utils.getLocator("iframePage.singUpLink").click();
		if (driver.getWindowHandles().size() == 3)
			parentWindow = selUtils.switchToThirdWindowUsingWebElement("iframeSignUpfieldsPage.emailTextbox");
		else
			parentWindow = selUtils.switchToNewWindow();
		utils.getLocator("iframeSignUpfieldsPage.membershipcard").isDisplayed();

		utils.getLocator("iframeSignUpfieldsPage.membershipcard").sendKeys("");
		utils.getLocator("iframeSignUpfieldsPage.emailTextbox").isDisplayed();
		iframeEmail = prop.getProperty("iFrameEmailPrefix").replace(temp,
				CreateDateTime.getTimeDateString() + CreateDateTime.getRandomString(6).toLowerCase());
		utils.getLocator("iframeSignUpPage.emailTextbox").sendKeys("");
		// logger.info("Signup email is set as: " + iframeEmail);
		// TestListeners.extentTest.get().info("Signup email is set as: " +
		// iframeEmail);
		utils.getLocator("iframeSignUpfieldsPage.title").isDisplayed();
		utils.getLocator("iframeSignUpfieldsPage.fNameTextbox").isDisplayed();
		utils.getLocator("iframeSignUpfieldsPage.fNameTextbox").sendKeys("");
		utils.getLocator("iframeSignUpfieldsPage.gender").isDisplayed();
		utils.getLocator("iframeSignUpfieldsPage.phonenumber").isDisplayed();
		utils.getLocator("iframeSignUpfieldsPage.phonenumber").sendKeys("");
		utils.getLocator("iframeSignUpfieldsPage.addressTextbox").isDisplayed();
		utils.getLocator("iframeSignUpfieldsPage.addressTextbox").sendKeys("");
		utils.getLocator("iframeSignUpPage.lNameTextbox").isDisplayed();
		utils.getLocator("iframeSignUpPage.lNameTextbox").sendKeys("");
		utils.getLocator("iframeSignUpPage.passwordTextbox").isDisplayed();
		utils.getLocator("iframeSignUpPage.passwordTextbox").sendKeys("");
		utils.getLocator("iframeSignUpPage.confPasswordTextbox").isDisplayed();
		utils.getLocator("iframeSignUpPage.confPasswordTextbox").sendKeys("");
		utils.getLocator("iframeSignUpfieldsPage.birthDayDropdown").isDisplayed();
		utils.getLocator("iframeSignUpfieldsPage.birthMonthDropdown").isDisplayed();
		utils.getLocator("iframeSignUpfieldsPage.birthYearDropdown").isDisplayed();
		utils.getLocator("iframeSignUpfieldsPage.anniversaryDropdown").isDisplayed();
		utils.getLocator("iframeSignUpfieldsPage.anniversaryMonthDropdown").isDisplayed();
		utils.getLocator("iframeSignUpfieldsPage.anniversaryYearDropdown").isDisplayed();
		utils.getLocator("iframeSignUpfieldsPage.Favoritelocation").isDisplayed();
		utils.getLocator("iframeSignUpPage.subscriptionEmailCheckbox").isDisplayed();
		// utils.getLocator("iframeSignUpPage.subscriptionEmailCheckbox").click();
		utils.getLocator("iframeSignUpfieldsPage.preferredlancheckbox").isDisplayed();
		utils.getLocator("iframeSignUpfieldsPage.termsandcondcheckbox").isDisplayed();
		utils.getLocator("iframeSignUpPage.submitButton").isDisplayed();
		utils.getLocator("iframeSignUpPage.submitButton").click();
		logger.info("Clicked in signup submit button");
		TestListeners.extentTest.get().info("Clicked in signup submit button");
		// driver.switchTo().window(parentWindow);
//			utils.getLocator("iframePage.accountBalanceLink").isDisplayed();
//			logger.info("Signup via iframe is successful");
//			TestListeners.extentTest.get().pass("Signup via iframe is successful");

		return iframeEmail;
	}

	public String iframeSignUpFieldinvaliddata() {
		String parentWindow;
		try {
			logger.info("==Signing up in iframe==");
			TestListeners.extentTest.get().info("==Signing up in iframe==");
			utils.getLocator("iframePage.singUpLink").click();
			if (driver.getWindowHandles().size() == 3)
				parentWindow = selUtils.switchToThirdWindowUsingWebElement("iframeSignUpfieldsPage.emailTextbox");
			else
				parentWindow = selUtils.switchToNewWindow();
			utils.getLocator("iframeSignUpfieldsPage.membershipcard").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.membershipcard").sendKeys("12345");
			utils.getLocator("iframeSignUpfieldsPage.emailTextbox").isDisplayed();
			iframeEmailinvalid = prop.getProperty("iFrameEmailPrefix").replace(temp,
					CreateDateTime.getTimeDateString() + CreateDateTime.getRandomString(6).toLowerCase());
			utils.getLocator("iframeSignUpPage.emailTextbox").sendKeys("");
			// logger.info("Signup email is set as: " + iframeEmail);
			// TestListeners.extentTest.get().info("Signup email is set as: " +
			// iframeEmail);
			utils.getLocator("iframeSignUpfieldsPage.title").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.fNameTextbox").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.fNameTextbox").sendKeys("");
			utils.getLocator("iframeSignUpfieldsPage.gender").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.phonenumber").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.phonenumber").sendKeys("989765");
			utils.getLocator("iframeSignUpfieldsPage.addressTextbox").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.addressTextbox").sendKeys("");
			utils.getLocator("iframeSignUpPage.lNameTextbox").isDisplayed();
			utils.getLocator("iframeSignUpPage.lNameTextbox").sendKeys("1234");
			utils.getLocator("iframeSignUpPage.passwordTextbox").isDisplayed();
			utils.getLocator("iframeSignUpPage.passwordTextbox").sendKeys("12");
			utils.getLocator("iframeSignUpPage.confPasswordTextbox").isDisplayed();
			utils.getLocator("iframeSignUpPage.confPasswordTextbox").sendKeys("123");
			utils.getLocator("iframeSignUpfieldsPage.birthDayDropdown").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.birthMonthDropdown").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.birthYearDropdown").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.anniversaryDropdown").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.anniversaryMonthDropdown").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.anniversaryYearDropdown").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.zipcode").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.zipcode").sendKeys("5");
			Thread.sleep(2000);
			utils.getLocator("iframeSignUpfieldsPage.Favoritelocation").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.Favoritelocation").click();
			Select favloc = new Select(utils.getLocator("iframeSignUpfieldsPage.Favoritelocation"));
			favloc.selectByValue("305019");
			utils.getLocator("iframeSignUpPage.subscriptionEmailCheckbox").isDisplayed();
			utils.getLocator("iframeSignUpPage.subscriptionEmailCheckbox").click();
			utils.getLocator("iframeSignUpfieldsPage.preferredlancheckbox").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.preferredlancheckbox").click();
			utils.getLocator("iframeSignUpfieldsPage.termsandcondcheckbox").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.termsandcondcheckbox").click();
			utils.getLocator("iframeSignUpPage.submitButton").isDisplayed();
			utils.getLocator("iframeSignUpPage.submitButton").click();
			logger.info("Clicked in signup submit button");
			TestListeners.extentTest.get().info("Clicked in signup submit button");
			// driver.switchTo().window(parentWindow);
			// utils.getLocator("iframePage.accountBalanceLink").isDisplayed();
			logger.info("Signup via iframe is successful");
			TestListeners.extentTest.get().pass("Signup via iframe is successful");
		} catch (Exception e) {
			List<WebElement> errors = driver.findElements(By.xpath("//form[@id='user-form']//div[2]"));
			for (int i = 0; i < errors.size(); i++) {
				String error = errors.get(i).getText();
				logger.info(error);
				TestListeners.extentTest.get().info(error);

			}
//			String error=utils.getLocator("iframeSignUpfieldsPage.errormessage").getText();
//			logger.error(error);
//			logger.error("5 errors prohibited this user from being saved:\n"
//					+ "Email can't be blank\n"
//					+ "Email is invalid\n"
//					+ "Confirm Password doesn't match Password\n"
//					+ "Please enter a valid phone number of minimum 10 digits.\n"
//					+ "Password must have at least one number, alphabet and special character except spaces and backslashes(\\).minimum length is 8 characters.5 errors prohibited this user from being saved:\n"
//					+ "Email can't be blank\n"
//					+ "Email is invalid\n"
//					+ "Confirm Password doesn't match Password\n"
//					+ "Please enter a valid phone number of minimum 10 digits.\n"
//					+ "Password must have at least one number, alphabet and special character except spaces and backslashes(\\).minimum length is 8 characters. " + e);
//		TestListeners.extentTest.get().fail("Error in navigating to iFrame signup/login " + e);
//			List<WebElement> errors=driver.findElements(By.xpath("//li"));
//			for(int i=0;i<errors.size();i++)
//			{
//				String error=errors.get(i).getText();
//				System.out.println(error);
//			}
		}
		return iframeEmailinvalid;
	}

	public String iframeSignUpFieldvaliddata() {
		String parentWindow;
		try {
			logger.info("==Signing up in iframe==");
			TestListeners.extentTest.get().info("==Signing up in iframe==");
			utils.getLocator("iframePage.singUpLink").click();
			if (driver.getWindowHandles().size() == 3)
				parentWindow = selUtils.switchToThirdWindowUsingWebElement("iframeSignUpfieldsPage.emailTextbox");
			else
				parentWindow = selUtils.switchToNewWindow();
			utils.getLocator("iframeSignUpfieldsPage.membershipcard").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.membershipcard").sendKeys("12345");
			utils.getLocator("iframeSignUpfieldsPage.emailTextbox").isDisplayed();
			iframeEmailvalid = prop.getProperty("iFrameEmailPrefix").replace(temp, CreateDateTime.getTimeDateString());
			utils.getLocator("iframeSignUpPage.emailTextbox").sendKeys(iframeEmailvalid);
			logger.info("Signup email is set as: " + iframeEmailvalid);
			TestListeners.extentTest.get().info("Signup email is set as: " + iframeEmailvalid);
			utils.getLocator("iframeSignUpfieldsPage.title").isDisplayed();
			Select title = new Select(utils.getLocator("iframeSignUpfieldsPage.title"));
			title.selectByIndex(1);
			utils.getLocator("iframeSignUpfieldsPage.fNameTextbox").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.fNameTextbox")
					.sendKeys(CreateDateTime.getUniqueString(prop.getProperty("firstName")));
			utils.getLocator("iframeSignUpfieldsPage.lNameTextbox").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.lNameTextbox")
					.sendKeys(CreateDateTime.getUniqueString(prop.getProperty("lastName")));
			utils.getLocator("iframeSignUpfieldsPage.gender").isDisplayed();
			// utils.getLocator("iframeSignUpfieldsPage.gender").click();
			Select gender = new Select(utils.getLocator("iframeSignUpfieldsPage.gender"));
			gender.selectByIndex(2);
			utils.getLocator("iframeSignUpfieldsPage.phonenumber").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.phonenumber").sendKeys(CreateDateTime.getTimeDateString());
			utils.getLocator("iframeSignUpfieldsPage.addressTextbox").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.addressTextbox").sendKeys("qwerty");
			utils.getLocator("iframeSignUpfieldsPage.passwordTextbox").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.passwordTextbox").sendKeys(prop.getProperty("iFramePassword"));
			utils.getLocator("iframeSignUpfieldsPage.confPasswordTextbox").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.confPasswordTextbox").sendKeys(prop.getProperty("iFramePassword"));
			utils.getLocator("iframeSignUpfieldsPage.birthDayDropdown").isDisplayed();
			Select selDay = new Select(utils.getLocator("iframeSignUpfieldsPage.birthDayDropdown"));
			selDay.selectByValue(Integer.toString(Utilities.getRandomNoWithoutZero(28)));
			Select selMonth = new Select(utils.getLocator("iframeSignUpfieldsPage.birthMonthDropdown"));
			selMonth.selectByValue(Integer.toString(Utilities.getRandomNoWithoutZero(12)));
			Select selYear = new Select(utils.getLocator("iframeSignUpfieldsPage.birthYearDropdown"));
			selYear.selectByValue(Integer.toString(Utilities.getRandomNoFromRange(1931, 2007)));
			utils.getLocator("iframeSignUpfieldsPage.anniversaryDropdown").isDisplayed();
			Select aaniday = new Select(utils.getLocator("iframeSignUpfieldsPage.anniversaryDropdown"));
			aaniday.selectByIndex(6);
			utils.getLocator("iframeSignUpfieldsPage.anniversaryMonthDropdown").isDisplayed();
			Select annimonth = new Select(utils.getLocator("iframeSignUpfieldsPage.anniversaryMonthDropdown"));
			annimonth.selectByIndex(4);
			utils.getLocator("iframeSignUpfieldsPage.anniversaryYearDropdown").isDisplayed();
			Select anniyear = new Select(utils.getLocator("iframeSignUpfieldsPage.anniversaryYearDropdown"));
			anniyear.selectByIndex(3);
			utils.getLocator("iframeSignUpfieldsPage.zipcode").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.zipcode").sendKeys("567894");
			utils.getLocator("iframeSignUpfieldsPage.Favoritelocation").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.Favoritelocation").click();
			Select favloc = new Select(utils.getLocator("iframeSignUpfieldsPage.Favoritelocation"));
			favloc.selectByIndex(3);
			utils.getLocator("iframeSignUpfieldsPage.emailsubcheckbox").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.emailsubcheckbox").click();
			utils.getLocator("iframeSignUpfieldsPage.preferredlancheckbox").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.preferredlancheckbox").click();
			utils.getLocator("iframeSignUpfieldsPage.termsandcondcheckbox").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.termsandcondcheckbox").click();
			utils.getLocator("iframeSignUpfieldsPage.submitButton").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.submitButton").click();
			logger.info("Clicked in signup submit button");
			TestListeners.extentTest.get().info("Clicked in signup submit button");
			driver.switchTo().window(parentWindow);
			utils.getLocator("iframePage.accountBalanceLink").isDisplayed();
			logger.info("Signup via iframe is successful");
			TestListeners.extentTest.get().pass("Signup via iframe is successful");
		} catch (Exception e) {
			logger.error("Error in navigating to iFrame signup/login " + e);
			TestListeners.extentTest.get().fail("Error in navigating to iFrame signup/login " + e);
		}
		return iframeEmailvalid;
	}

	public String iframeSignUp() {
		String parentWindow;
		try {
			logger.info("==Signing up in iframe==");
			TestListeners.extentTest.get().info("==Signing up in iframe==");
			utils.getLocator("iframePage.singUpLink").click();
			if (driver.getWindowHandles().size() == 3)
				parentWindow = selUtils.switchToThirdWindowUsingWebElement("iframeSignUpPage.emailTextbox");
			else
				parentWindow = selUtils.switchToNewWindow();
			utils.getLocator("iframeSignUpfieldsPage.emailTextbox").isDisplayed();
			iframeEmaildata = prop.getProperty("iFrameEmailPrefix").replace(temp,
					CreateDateTime.getTimeDateString() + CreateDateTime.getRandomString(6).toLowerCase());
			utils.getLocator("iframeSignUpfieldsPage.emailTextbox").sendKeys(iframeEmaildata);
			logger.info("Signup email is set as: " + iframeEmaildata);
			TestListeners.extentTest.get().info("Signup email is set as: " + iframeEmaildata);
			utils.getLocator("iframeSignUpfieldsPage.fNameTextbox").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.fNameTextbox")
					.sendKeys(CreateDateTime.getUniqueString(prop.getProperty("firstName")));
			utils.getLocator("iframeSignUpfieldsPage.lNameTextbox").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.lNameTextbox")
					.sendKeys(CreateDateTime.getUniqueString(prop.getProperty("lastName")));
			utils.getLocator("iframeSignUpfieldsPage.passwordTextbox").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.passwordTextbox").sendKeys(prop.getProperty("iFramePassword"));
			utils.getLocator("iframeSignUpfieldsPage.confPasswordTextbox").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.confPasswordTextbox").sendKeys(prop.getProperty("iFramePassword"));
			utils.getLocator("iframeSignUpfieldsPage.subscriptionEmailCheckbox").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.subscriptionEmailCheckbox").click();
			utils.getLocator("iframeSignUpfieldsPage.submitButton").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.submitButton").click();
			logger.info("Clicked in signup submit button");
			TestListeners.extentTest.get().info("Clicked in signup submit button");
			driver.switchTo().window(parentWindow);
			utils.getLocator("iframePage.accountBalanceLink").isDisplayed();
			logger.info("Signup via iframe is successful");
			TestListeners.extentTest.get().pass("Signup via iframe is successful");
		} catch (Exception e) {
			logger.error("Error in navigating to iFrame signup/login " + e);
			TestListeners.extentTest.get().fail("Error in navigating to iFrame signup/login " + e);
		}

		return iframeEmaildata;
	}

	public void iframeLoginvalid(String email) {
		String parentWindow = null;
		try {
			utils.getLocator("iframePage.loginLink").isDisplayed();
			utils.getLocator("iframePage.loginLink").click();
			parentWindow = selUtils.switchToNewWindow();
			utils.getLocator("iframeSignUpfieldsPage.emailTextbox").sendKeys(email);
			utils.getLocator("iframeSignUpfieldsPage.passwordTextbox").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.passwordTextbox").sendKeys(prop.getProperty("iFramePassword"));
			utils.getLocator("iframeSignUpfieldsPage.submitButton").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.submitButton").click();
			driver.switchTo().window(parentWindow);
			utils.getLocator("iframePage.accountBalanceLink").isDisplayed();
			utils.getLocator("iframePage.editProfileLink").isDisplayed();
			utils.getLocator("iframePage.editProfileLink").click();
			utils.getLocator("iframePage.emailLabel").isDisplayed();
			logger.info("Login via iframe is successful");
			TestListeners.extentTest.get().pass("Login via iframe is successful");
		} catch (Exception e) {
			logger.error("Error in navigating to iFrame signup/login " + e);
			TestListeners.extentTest.get().fail("Error in navigating to iFrame signup/login " + e);
		}
	}

	public void iframeLoginvalid1(String email) {
		String parentWindow = null;
		try {
			utils.getLocator("iframePage.loginLink").isDisplayed();
			utils.getLocator("iframePage.loginLink").click();
			parentWindow = selUtils.switchToNewWindow();
			utils.getLocator("iframeSignUpfieldsPage.emailTextbox").sendKeys(email);
			utils.getLocator("iframeSignUpfieldsPage.passwordTextbox").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.passwordTextbox").sendKeys(prop.getProperty("iFramePassword"));
			utils.getLocator("iframeSignUpfieldsPage.submitButton").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.submitButton").click();
			driver.switchTo().window(parentWindow);
			utils.getLocator("iframePage.accountBalanceLink").isDisplayed();
			logger.info("Login via iframe is successful");
			TestListeners.extentTest.get().pass("Login via iframe is successful");
		} catch (Exception e) {
			logger.error("Error in navigating to iFrame signup/login " + e);
			TestListeners.extentTest.get().fail("Error in navigating to iFrame signup/login " + e);
		}
	}

	public String generateEmail() {
		iframeEmail = prop.getProperty("iFrameEmailPrefix").replace(temp,
				CreateDateTime.getTimeDateString() + CreateDateTime.getRandomString(6).toLowerCase());
		System.out.println(iframeEmail);
		return iframeEmail;
	}

	public void iframeSignOut() {
		try {
			utils.getLocator("iframePage.signOutLink").isDisplayed();
			utils.getLocator("iframePage.signOutLink").click();
		} catch (Exception e) {
			logger.error("Error in iFrame signout");
			TestListeners.extentTest.get().fail("Error in iFrame signout");
		}
	}

	public String getAccountBalance() {
		String points = null;
		try {
			utils.switchToParentWindow();
			utils.getLocator("iframePage.accountBalanceLink").isDisplayed();
			utils.getLocator("iframePage.accountBalanceLink").click();
			utils.getLocator("iframePage.getCurrentPoint").isDisplayed();
			points = utils.getLocator("iframePage.getCurrentPoint").getText();
			logger.info("Current Points is " + points);
			TestListeners.extentTest.get().info("Current Points is " + points);
		} catch (Exception e) {
			logger.error("Error in fetching Current Points " + e);
			TestListeners.extentTest.get().fail("Error in fetching Current Points " + e);
			Assert.fail();
		}
		return points;
	}

	public String getMembershiplevel() {
		String membership = utils.getLocator("iframePage.membershipLevel").getText();
		logger.info("Current Points is " + membership);
		TestListeners.extentTest.get().info("Current membership is " + membership);
		return membership;
	}

	public void clickOnSaveProfile() {
		utils.longWaitInSeconds(3);
		// driver.switchTo().defaultContent();
		utils.waitTillElementToBeClickable(utils.getLocator("iframePage.saveProfileButton"));
		utils.getLocator("iframePage.saveProfileButton").click();
		utils.longWaitInSeconds(2);

	}

	public String iframeSignUpForApplePass() {
		String parentWindow;
		try {
			logger.info("==Signing up in iframe==");
			TestListeners.extentTest.get().info("==Signing up in iframe==");
			Thread.sleep(3000);
			utils.getLocator("iframePage.singUpLink").click();
			if (driver.getWindowHandles().size() == 3)
				parentWindow = selUtils.switchToThirdWindowUsingWebElement("iframeSignUpPage.emailTextbox");
			else
				parentWindow = selUtils.switchToNewWindow();
			utils.getLocator("iframeSignUpfieldsPage.emailTextbox").isDisplayed();
			iframeEmaildata = prop.getProperty("iFrameEmailPrefix").replace(temp,
					CreateDateTime.getTimeDateString() + CreateDateTime.getRandomString(6).toLowerCase());
			utils.getLocator("iframeSignUpfieldsPage.emailTextbox").sendKeys(iframeEmaildata);
			logger.info("Signup email is set as: " + iframeEmaildata);
			TestListeners.extentTest.get().info("Signup email is set as: " + iframeEmaildata);
			utils.getLocator("iframeSignUpfieldsPage.fNameTextbox").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.fNameTextbox")
					.sendKeys(CreateDateTime.getUniqueString(prop.getProperty("firstName")));
			utils.getLocator("iframeSignUpfieldsPage.lNameTextbox").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.lNameTextbox")
					.sendKeys(CreateDateTime.getUniqueString(prop.getProperty("lastName")));
			utils.getLocator("iframeSignUpfieldsPage.passwordTextbox").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.passwordTextbox").sendKeys(prop.getProperty("iFramePassword"));
			utils.getLocator("iframeSignUpfieldsPage.confPasswordTextbox").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.confPasswordTextbox").sendKeys(prop.getProperty("iFramePassword"));
			utils.getLocator("iframeSignUpfieldsPage.submitButton").isDisplayed();
			utils.getLocator("iframeSignUpfieldsPage.submitButton").click();
			logger.info("Clicked in signup submit button");
			TestListeners.extentTest.get().info("Clicked in signup submit button");
		} catch (Exception e) {
			logger.error("Error in navigating to iFrame signup/login " + e);
			TestListeners.extentTest.get().fail("Error in navigating to iFrame signup/login " + e);
		}

		return iframeEmaildata;
	}

}
