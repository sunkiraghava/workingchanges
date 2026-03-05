package com.punchh.server.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.Color;
import org.openqa.selenium.support.ui.Select;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class IframeLoginSignUpPage {
	static Logger logger = LogManager.getLogger(IframeLoginSignUpPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	String iframeEmail, temp = "temp";
//	private int counter = 0;
//	private final Object lock = new Object();

	private Map<String, By> locators;

	public IframeLoginSignUpPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		locators = utils.getAllByMap();
	}

	public void navigateToIframe(String url) {

		driver.navigate().to(url);

		WebElement singUpLink = driver.findElement(locators.get("iframePage.singUpLink"));
		singUpLink.isDisplayed();
		WebElement loginLink = driver.findElement(locators.get("iframePage.loginLink"));
		loginLink.isDisplayed();
		logger.info("Navigated to iframe signup/login page");
		TestListeners.extentTest.get().info("Navigated to iframe signup/login page with url :" + url);
	}

	public String iframeSignUp() {
		String parentWindow;
		logger.info("==Signing up in iframe==");
		TestListeners.extentTest.get().info("==Signing up in iframe==");
		WebElement singUpLink = driver.findElement(locators.get("iframePage.singUpLink"));
		utils.waitTillVisibilityOfElement(singUpLink, "Sign up button");
		singUpLink.click();
		if (driver.getWindowHandles().size() == 3)
			parentWindow = selUtils.switchToThirdWindowUsingWebElement("iframeSignUpPage.emailTextbox");
		else
			parentWindow = selUtils.switchToNewWindow();
		WebElement emailTextbox = driver.findElement(locators.get("iframeSignUpPage.emailTextbox"));
		emailTextbox.isDisplayed();
		iframeEmail = prop.getProperty("iFrameEmailPrefix").replace(temp,
				CreateDateTime.getTimeDateString() + CreateDateTime.getRandomString(6).toLowerCase());
		emailTextbox.sendKeys(iframeEmail);
		logger.info("Signup email is set as: " + iframeEmail);
		TestListeners.extentTest.get().info("Signup email is set as: " + iframeEmail);
		WebElement fNameTextbox = driver.findElement(locators.get("iframeSignUpPage.fNameTextbox"));
		fNameTextbox.sendKeys(CreateDateTime.getUniqueString(prop.getProperty("firstName")));
		WebElement lNameTextbox = driver.findElement(locators.get("iframeSignUpPage.lNameTextbox"));
		lNameTextbox.sendKeys(CreateDateTime.getUniqueString(prop.getProperty("lastName")));
		WebElement passwordTextbox = driver.findElement(locators.get("iframeSignUpPage.passwordTextbox"));
		passwordTextbox.sendKeys(prop.getProperty("iFramePassword"));
		WebElement confPasswordTextbox = driver.findElement(locators.get("iframeSignUpPage.confPasswordTextbox"));
		confPasswordTextbox.sendKeys(prop.getProperty("iFramePassword"));
		WebElement submitButton = driver.findElement(locators.get("iframeSignUpPage.submitButton"));
		submitButton.isDisplayed();
		// driver.findElement(locators.get("iframeSignUpPage.submitButton")).click();
		utils.longWaitInSeconds(1);
		submitButton.sendKeys(Keys.ENTER);
		try {
			WebElement saveProfileButton = driver.findElement(locators.get("iframeSignUpPage.saveProfileButton"));
			saveProfileButton.click();
			Thread.sleep(2000);
			logger.info("Save Profile button is visible and clicked");
			TestListeners.extentTest.get().info("Save Profile button is visible and clicked");
		} catch (Exception e) {
			logger.info("Save Profile button is not visible");
			TestListeners.extentTest.get().info("Save Profile button is not visible");
		}
		logger.info("Clicked in signup submit button");
		TestListeners.extentTest.get().info("Clicked in signup submit button");
		driver.switchTo().window(parentWindow);
		WebElement signOutLink = driver.findElement(locators.get("iframePage.signOutLink"));
		signOutLink.isDisplayed();
		logger.info("Signup via iframe is successful");
		TestListeners.extentTest.get().pass("Signup via iframe is successful");
		return iframeEmail;
	}

	public void navigateToEcrm(String url) {
		try {
			driver.navigate().to(url);
		} catch (Exception e) {
			logger.error("Error in navigating to iFrame signup/login" + e);
			TestListeners.extentTest.get().fail("Navigated to iframe signup/login page" + e);
		}
	}

	public String ecrmSignUp(String location) {
		WebElement emailTextbox = driver.findElement(locators.get("iframeSignUpPage.emailTextbox"));
		emailTextbox.isDisplayed();
		String userEmail = prop.getProperty("iFrameEmailPrefix").replace(temp, CreateDateTime.getTimeDateString());
		emailTextbox.sendKeys(userEmail);
		logger.info("Signup email is set as: " + userEmail);
		TestListeners.extentTest.get().info("Signup email is set as: " + userEmail);
		WebElement fNameTextbox = driver.findElement(locators.get("iframeSignUpPage.fNameTextbox"));
		fNameTextbox.isDisplayed();
		fNameTextbox.sendKeys(CreateDateTime.getUniqueString(prop.getProperty("firstName")));
		WebElement lNameTextbox = driver.findElement(locators.get("iframeSignUpPage.lNameTextbox"));
		lNameTextbox.isDisplayed();
		lNameTextbox.sendKeys(CreateDateTime.getUniqueString(prop.getProperty("lastName")));
		WebElement phoneNumberDropdown = driver.findElement(locators.get("iframeSignUpPage.phoneNumberDropdown"));
		phoneNumberDropdown.sendKeys(CreateDateTime.getTimeDateString());
		WebElement birthDayDropdown = driver.findElement(locators.get("iframeSignUpPage.birthDayDropdown"));
		Select selDay = new Select(birthDayDropdown);
		selDay.selectByValue(Integer.toString(Utilities.getRandomNoWithoutZero(28)));
		WebElement birthMonthDropdown = driver.findElement(locators.get("iframeSignUpPage.birthMonthDropdown"));
		Select selMonth = new Select(birthMonthDropdown);
		selMonth.selectByValue(Integer.toString(Utilities.getRandomNoWithoutZero(12)));
		WebElement birthYearDropdown = driver.findElement(locators.get("iframeSignUpPage.birthYearDropdown"));
		Select selYear = new Select(birthYearDropdown);
		selYear.selectByValue(Integer.toString(Utilities.getRandomNoFromRange(1931, 2007)));

		WebElement addressTextbox = driver.findElement(locators.get("iframeSignUpPage.addressTextbox"));
		addressTextbox.sendKeys("Test Address");
		WebElement cityTextbox = driver.findElement(locators.get("iframeSignUpPage.cityTextbox"));
		cityTextbox.sendKeys("Test City");
		WebElement stateTextbox = driver.findElement(locators.get("iframeSignUpPage.stateTextbox"));
		stateTextbox.sendKeys("Test State");
		WebElement zipTextbox = driver.findElement(locators.get("iframeSignUpPage.zipTextbox"));
		zipTextbox.sendKeys("Test Zip");
		WebElement locationDropdown = driver.findElement(locators.get("iframeSignUpPage.locationDropdown"));
		locationDropdown.click();
		Select selLoc = new Select(locationDropdown);
		selLoc.selectByIndex(1);
		// driver.findElement(locators.get("iframeSignUpPage.smsNotificationEnableCheckbox")).isDisplayed();
		// driver.findElement(locators.get("iframeSignUpPage.smsNotificationEnableCheckbox")).click();
		WebElement acceptTCCheckbox = driver.findElement(locators.get("iframeSignUpPage.acceptT&CCheckbox"));
		acceptTCCheckbox.isDisplayed();
		acceptTCCheckbox.click();
		WebElement privacyPolicy = driver.findElement(locators.get("iframeSignUpPage.privacyPolicy"));
		privacyPolicy.isDisplayed();
		privacyPolicy.click();
		WebElement subscriptionEmailCheckbox = driver.findElement(locators.get("iframeSignUpPage.subscriptionEmailCheckbox"));
		subscriptionEmailCheckbox.isDisplayed();
		subscriptionEmailCheckbox.click();
		WebElement submitButton = driver.findElement(locators.get("iframeSignUpPage.submitButton"));
		submitButton.isDisplayed();
		submitButton.click();
		logger.info("Clicked in signup submit button");
		TestListeners.extentTest.get().info("Clicked in signup submit button");
		WebElement submittedMsgLabel = driver.findElement(locators.get("iframeSignUpPage.submittedMsgLabel"));
		submittedMsgLabel.isDisplayed();
		logger.info("Guest details are submitted");
		TestListeners.extentTest.get().info("Guest details are submitted");

		return userEmail;
	}

	public String facebookSignUp(String email) {
		String parentWindow;
		try {
			logger.info("==Signing up in iframe==");
			TestListeners.extentTest.get().info("==Signing up in iframe==");
			WebElement singUpLink = driver.findElement(locators.get("iframePage.singUpLink"));
			singUpLink.click();
			parentWindow = selUtils.switchToNewWindow();
			WebElement facebookLink = driver.findElement(By.xpath("//span[@class='facebook-link']"));
			facebookLink.isDisplayed();
			facebookLink.click();
			WebElement facebookLogo = driver.findElement(locators.get("iframeSignUpPage.facebookLogo"));
			facebookLogo.isDisplayed();
			WebElement facebookSignupButton = driver.findElement(locators.get("iframeSignUpPage.facebookSignupButton"));
			facebookSignupButton.isDisplayed();
			WebElement loginFacebookLabel = driver.findElement(locators.get("iframeSignUpPage.LoginFacebookLabel"));
			loginFacebookLabel.isDisplayed();
			WebElement createNewAccLabel = driver.findElement(locators.get("iframeSignUpPage.createNewAccLabel"));
			createNewAccLabel.isDisplayed();
			TestListeners.extentTest.get().pass("facebook loginsingup options are appearing");
			logger.info("facebook loginsingup options are appearing");
			driver.close();
			driver.switchTo().window(parentWindow);
		} catch (Exception e) {
			logger.error("Error in navigating to iFrame signup/login " + e);
			TestListeners.extentTest.get().fail("Error in navigating to iFrame signup/login " + e);
		}
		return iframeEmail;
	}

	public String facebookLogin(String fbuserName, String fbPwd) {
		String parentWindow;
		try {
			logger.info("==Facebook Login==");
			TestListeners.extentTest.get().info("==Facebook Login==");
			WebElement loginLink = driver.findElement(locators.get("iframePage.loginLink"));
			loginLink.isDisplayed();
			loginLink.click();
			parentWindow = selUtils.switchToNewWindow();
			TestListeners.extentTest.get().info("Clicked in login FB button");
			WebElement facebookLoginButton = driver.findElement(locators.get("iframePage.facebookLoginButton"));
			facebookLoginButton.isDisplayed();
			facebookLoginButton.click();
			WebElement fbEmailTextbox = driver.findElement(locators.get("iframeSignUpPage.fbEmailTextbox"));
			fbEmailTextbox.isDisplayed();
			fbEmailTextbox.clear();
			fbEmailTextbox.sendKeys(fbuserName);
			WebElement fbPasswordTextbox = driver.findElement(locators.get("iframeSignUpPage.fbPasswordTextbox"));
			fbPasswordTextbox.isDisplayed();
			fbPasswordTextbox.clear();
			fbPasswordTextbox.sendKeys(fbPwd);
			WebElement fbLoginButton = driver.findElement(locators.get("iframeSignUpPage.fbLoginButton"));
			fbLoginButton.isDisplayed();
			fbLoginButton.click();
			driver.switchTo().window(parentWindow);
			WebElement accountBalanceLink = driver.findElement(locators.get("iframePage.accountBalanceLink"));
			accountBalanceLink.isDisplayed();
			WebElement editProfileLink = driver.findElement(locators.get("iframePage.editProfileLink"));
			editProfileLink.isDisplayed();
			editProfileLink.click();
			WebElement emailLabel = driver.findElement(locators.get("iframePage.emailLabel"));
			emailLabel.isDisplayed();
			logger.info("Login via facebook is successful");
			TestListeners.extentTest.get().pass("Login via facebook is successful");
		} catch (Exception e) {
			logger.error("Error in navigating to iFrame signup/login " + e);
			TestListeners.extentTest.get().fail("Error in navigating to iFrame signup/login " + e);
		}
		return iframeEmail;
	}

	public void iframeLogin(String email) {
		String parentWindow = null;

		WebElement loginLink = driver.findElement(locators.get("iframePage.loginLink"));
		loginLink.isDisplayed();
		loginLink.click();
		TestListeners.extentTest.get().pass("Clicked on iframe login link");
		parentWindow = selUtils.switchToNewWindow();
		WebElement emailTextbox = driver.findElement(locators.get("iframeSignUpPage.emailTextbox"));
		emailTextbox.sendKeys(email);
		WebElement passwordTextbox = driver.findElement(locators.get("iframeSignUpPage.passwordTextbox"));
		passwordTextbox.isDisplayed();
		passwordTextbox.sendKeys(prop.getProperty("iFramePassword"));
		WebElement submitButton = driver.findElement(locators.get("iframeSignUpPage.submitButton"));
		submitButton.isDisplayed();
		utils.longWait(2000);
		submitButton.sendKeys(Keys.ENTER);
		TestListeners.extentTest.get().pass("Entered email and password in iframe login and clicked on submit button");
		// driver.findElement(locators.get("iframeSignUpPage.submitButton")).click();
		// utils.clickByJSExecutor(driver,
		// driver.findElement(locators.get("iframeSignUpPage.submitButton"));
		// driver.findElement(locators.get("iframeSignUpPage.submitButton")).sendKeys(Keys.ENTER);
		driver.switchTo().window(parentWindow);
		WebElement signOutLink = driver.findElement(locators.get("iframePage.signOutLink"));
		signOutLink.isDisplayed();
		/*
		 * driver.findElement(locators.get("iframePage.accountBalanceLink")).isDisplayed();
		 * driver.findElement(locators.get("iframePage.editProfileLink")).isDisplayed();
		 * driver.findElement(locators.get("iframePage.editProfileLink")).click();
		 * driver.findElement(locators.get("iframePage.emailLabel")).isDisplayed();
		 */
		logger.info("Login via iframe is successful");
		TestListeners.extentTest.get().pass("Login via iframe is successful");
	}

	public String iframeInvalidLogin(String email, String password) throws InterruptedException {
		String parentWindow = null;
		WebElement loginLink = driver.findElement(locators.get("iframePage.loginLink"));
		loginLink.isDisplayed();
		loginLink.click();
		parentWindow = selUtils.switchToNewWindow();
		WebElement emailTextbox = driver.findElement(locators.get("iframeSignUpPage.emailTextbox"));
		emailTextbox.sendKeys(email);
		WebElement passwordTextbox = driver.findElement(locators.get("iframeSignUpPage.passwordTextbox"));
		passwordTextbox.isDisplayed();
		passwordTextbox.sendKeys(password);
		WebElement submitButton = driver.findElement(locators.get("iframeSignUpPage.submitButton"));
		submitButton.isDisplayed();
		submitButton.click();
		Thread.sleep(2000);
		WebElement pwdHelpMsg = driver.findElement(locators.get("iframeSignUpPage.pwdHelpMsg"));
		String msg = pwdHelpMsg.getText();
		System.out.println(msg);
		return msg;
	}

	public String forgotPassword(String email) throws InterruptedException {
		WebElement forgotPassword = driver.findElement(locators.get("iframeSignUpPage.forgotPassword"));
		forgotPassword.isDisplayed();
		forgotPassword.click();
		WebElement emailTextbox = driver.findElement(locators.get("iframeSignUpPage.emailTextbox"));
		emailTextbox.isDisplayed();
		emailTextbox.sendKeys(email);
		WebElement sendResetPwd = driver.findElement(locators.get("iframeSignUpPage.sendResetPwd"));
		sendResetPwd.isDisplayed();
		sendResetPwd.sendKeys(Keys.ENTER);
		WebElement pwdResetMsg = driver.findElement(locators.get("iframeSignUpPage.pwdResetMsg"));
		pwdResetMsg.isDisplayed();
		Thread.sleep(2000);
		String msg = pwdResetMsg.getText();
		// utils.switchToParentWindow();
		return msg;
	}

	public boolean iframeLogin(String email, String password) {
		String parentWindow = null;
		boolean flag = false;
		WebElement loginLink = driver.findElement(locators.get("iframePage.loginLink"));
		loginLink.isDisplayed();
		loginLink.click();
		parentWindow = selUtils.switchToNewWindow();
		WebElement emailTextbox = driver.findElement(locators.get("iframeSignUpPage.emailTextbox"));
		emailTextbox.sendKeys(email);
		WebElement passwordTextbox = driver.findElement(locators.get("iframeSignUpPage.passwordTextbox"));
		passwordTextbox.isDisplayed();
		passwordTextbox.sendKeys(password);
		WebElement submitButton = driver.findElement(locators.get("iframeSignUpPage.submitButton"));
		submitButton.isDisplayed();
		submitButton.click();
		driver.switchTo().window(parentWindow);
		WebElement signOutLink = driver.findElement(locators.get("iframePage.signOutLink"));
		signOutLink.isDisplayed();
		/*
		 * driver.findElement(locators.get("iframePage.accountBalanceLink")).isDisplayed();
		 * driver.findElement(locators.get("iframePage.editProfileLink")).isDisplayed();
		 * driver.findElement(locators.get("iframePage.editProfileLink")).click();
		 * driver.findElement(locators.get("iframePage.emailLabel")).isDisplayed();
		 */
		logger.info("Login via iframe is successful");
		TestListeners.extentTest.get().pass("Login via iframe is successful");
		flag = true;
		return flag;
	}

	public void iframeCheckin(String barcode) {
		submitBarcode(barcode);
		WebElement checkinSuccessLabel = driver.findElement(locators.get("iframePage.checkinSuccessLabel"));
		checkinSuccessLabel.isDisplayed();
		logger.info("Barcode is successfully checked in");
		TestListeners.extentTest.get().info("Barcode is successfully checked in");
	}

	public boolean verifyIframeCheckinWithDuplicateBarcode(String barcode) {
		try {
			submitBarcode(barcode);
			WebElement duplicateRecieptMsgLabel = driver.findElement(locators.get("iframePage.duplicateRecieptMsgLabel"));
			duplicateRecieptMsgLabel.isDisplayed();
			logger.info("Verfied already used barcode warning: " + duplicateRecieptMsgLabel.getText());
			TestListeners.extentTest.get().info("Verfied already used barcode warning: " + duplicateRecieptMsgLabel.getText());
			return true;
		} catch (Exception e) {
			logger.error("Error in iframe checkin " + e);
			TestListeners.extentTest.get().fail("Error in iframe checkin " + e);
		}
		return false;
	}

	public boolean verifyIframeCheckinWithInvalidBarcode(String barcode) {
		try {
			submitBarcode(barcode);
			WebElement invalidRecieptMsgLabel = driver.findElement(locators.get("iframePage.invalidRecieptMsgLabel"));
			invalidRecieptMsgLabel.isDisplayed();
			logger.info("Verified invalid barcode warning: " + invalidRecieptMsgLabel.getText());
			TestListeners.extentTest.get().info("Verified invalid barcode warning: " + invalidRecieptMsgLabel.getText());
			return true;
		} catch (Exception e) {
			logger.error("Error in iframe checkin " + e);
			TestListeners.extentTest.get().fail("Error in iframe checkin " + e);
		}
		return false;
	}

	public boolean verifyIframeCheckinWithOldBarcode(String barcode) {
		try {
			submitBarcode(barcode);
			WebElement oldRecieptMsgLabel = driver.findElement(locators.get("iframePage.oldRecieptMsgLabel"));
			oldRecieptMsgLabel.isDisplayed();
			logger.info("Verified old barcode warning: " + oldRecieptMsgLabel.getText());
			TestListeners.extentTest.get().info("Verified old barcode warning: " + oldRecieptMsgLabel.getText());
			return true;
		} catch (Exception e) {
			logger.error("Error in iframe checkin " + e);
			TestListeners.extentTest.get().fail("Error in iframe checkin " + e);
		}
		return false;
	}

	public boolean verifyIframeCheckinWithOtherBuisnessBarcode(String barcode) {
		try {
			submitBarcode(barcode);
			WebElement invalidRecieptMsgLabel = driver.findElement(locators.get("iframePage.invalidRecieptMsgLabel"));
			invalidRecieptMsgLabel.isDisplayed();
			logger.info("Verified other buisness barcode as invlid, warning: " + invalidRecieptMsgLabel.getText());
			TestListeners.extentTest.get().info("Verified other buisness barcode as invlid, warning: " + invalidRecieptMsgLabel.getText());
			return true;
		} catch (Exception e) {
			logger.error("Error in iframe checkin " + e);
			TestListeners.extentTest.get().fail("Error in iframe checkin " + e);
		}
		return false;
	}

	private void submitBarcode(String barcode) {
		logger.info("==Checkin in Iframe==");
		TestListeners.extentTest.get().info("==Checkin in Iframe==");
		WebElement checkinLink = driver.findElement(locators.get("iframePage.checkinLink"));
		checkinLink.isDisplayed();
		checkinLink.click();
		WebElement checkinTextbox = driver.findElement(locators.get("iframePage.checkinTextbox"));
		checkinTextbox.isDisplayed();
		checkinTextbox.clear();
		checkinTextbox.sendKeys(barcode);
		logger.info("Barcode is set as: " + barcode);
		TestListeners.extentTest.get().info("Barcode is set as: " + barcode);
		WebElement commentTextbox = driver.findElement(locators.get("iframePage.commentTextbox"));
		commentTextbox.isDisplayed();
		commentTextbox.sendKeys("Entering duplicate barcode");
		WebElement sumitButton = driver.findElement(locators.get("iframePage.sumitButton"));
		sumitButton.isDisplayed();
		sumitButton.click();
		logger.info("Clicked in barcode checkin submit");
		TestListeners.extentTest.get().info("Clicked in barcode checkin submit");
	}

	public void iframeSignOut() {
		WebElement signOutLink = driver.findElement(locators.get("iframePage.signOutLink"));
		signOutLink.isDisplayed();
		signOutLink.click();

	}

	/*
	 * public String generateEmail() { // iframeEmail =
	 * prop.getProperty("iFrameEmailPrefix").replace(temp, //
	 * CreateDateTime.getTimeDateString() +
	 * CreateDateTime.getRandomString(6).toLowerCase()); synchronized(lock) {
	 * iframeEmail = prop.getProperty("iFrameEmailPrefix").replace(temp, <<<<<<<
	 * HEAD CreateDateTime.getRandomString(6).toLowerCase() +
	 * CreateDateTime.getTimeDateString()); =======
	 * CreateDateTime.getRandomString(6).toLowerCase()) +
	 * CreateDateTime.getTimeDateString();
	 * System.out.println("Email is => "+iframeEmail); return iframeEmail; } }
	 */

	public String generateEmail() {
		iframeEmail = prop.getProperty("iFrameEmailPrefix").replace(temp,
				CreateDateTime.getTimeDateString() + CreateDateTime.getRandomString(6).toLowerCase());
		logger.info(iframeEmail + " user is created");
		TestListeners.extentTest.get().pass(iframeEmail + " user random email created");
		return iframeEmail;
	}

	public String generateEmailWithDomainPartech() {
		String prefix = "autoiframetemp@partech.com";
		iframeEmail = prefix.replace(temp,
				CreateDateTime.getTimeDateString() + CreateDateTime.getRandomString(6).toLowerCase());
		logger.info(iframeEmail + " user is created");
		TestListeners.extentTest.get().pass(iframeEmail + " user random email created");
		return iframeEmail;
	}

	public String redeemRewardOffer(String rewardName) {
		// $2.0 OFF (Never Expires)
		// click redeem button accept alert
		String reward_code = "";
		for (int i = 1; i <= 3; i++) {
			try {
				// utils.longWaitInSeconds(3);
				WebElement offerLink = driver.findElement(locators.get("iframePage.offerLink"));
				utils.waitTillElementToBeClickable(offerLink);
				// utils.waitTillVisibilityOfElement(driver.findElement(locators.get("iframePage.offerLink"),"");
				utils.clickUsingActionsClass(offerLink);
				WebElement rewardsDrpDwn = driver.findElement(locators.get("iframePage.rewardsDrpDwn"));
				utils.selectDrpDwnValue(rewardsDrpDwn, rewardName);
				WebElement redeemBtn = driver.findElement(locators.get("iframePage.redeemBtn"));
				redeemBtn.click();
				utils.acceptAlert(driver);
				WebElement redemptionCode = driver.findElement(locators.get("iframePage.redemptionCode"));
				reward_code = redemptionCode.getText();
				logger.info("Congratulations! Your redemption code is: " + reward_code);
				TestListeners.extentTest.get().pass("Congratulations! Your redemption code is: " + reward_code);
				break;
			} catch (Exception e) {
				logger.info("Error in generating redemption code " + e);
				TestListeners.extentTest.get().info("Error in generating redemption code " + e);
			}
			utils.refreshPageWithCurrentUrl();
		}
		return reward_code;
	}

	public void redemptionOfRewardOffer(String rewardName) {
		List<WebElement> offerLinkList = driver.findElements(locators.get("iframePage.offerLink"));
		int size = offerLinkList.size();
		if (size >= 1) {
			WebElement offerLink = driver.findElement(locators.get("iframePage.offerLink"));
			offerLink.click();
			WebElement rewardsDrpDwn = driver.findElement(locators.get("iframePage.rewardsDrpDwn"));
			utils.selectDrpDwnValue(rewardsDrpDwn, rewardName);
			WebElement redeemBtn = driver.findElement(locators.get("iframePage.redeemBtn"));
			redeemBtn.click();
			utils.acceptAlert(driver);
			logger.info("clicked on redeem button");
			TestListeners.extentTest.get().info("clicked on redeem button");
		} else {
			logger.error("Error in generating redemption code ");
			TestListeners.extentTest.get().fail("Error in generating redemption code ");
		}
	}

	public String redeemRewardFromRedeemrewards(String rewardName) {
		// $2.0 OFF (Never Expires)
		// click redeem button accepet alert
		String reward_code = "";
		try {
			WebElement redeemRewardsLink = driver.findElement(locators.get("iframePage.redeemRewardsLink"));
			redeemRewardsLink.isDisplayed();
			redeemRewardsLink.click();
			WebElement redemption_redeemable_idDrpDwn = driver.findElement(locators.get("iframePage.redemption_redeemable_idDrpDwn"));
			utils.selectDrpDwnValue(redemption_redeemable_idDrpDwn, rewardName);
//			driver.findElement(locators.get("iframePage.redeemBtn")).click();
			utils.acceptAlert(driver);
			WebElement redemptionCode = driver.findElement(locators.get("iframePage.redemptionCode"));
			reward_code = redemptionCode.getText();
			logger.info("Congratulations! Your redemption code is: " + reward_code);
			TestListeners.extentTest.get().pass("Congratulations! Your redemption code is: " + reward_code);
		} catch (Exception e) {
			logger.error("Error in generating redemption code " + e);
			TestListeners.extentTest.get().fail("Error in generating redemption code " + e);
		}
		return reward_code;
	}

	public String iframeSignUpWithReferralCode(String referralCode) {
		String parentWindow;
		logger.info("==Signing up in iframe==");
		TestListeners.extentTest.get().info("==Signing up in iframe==");
		WebElement singUpLink = driver.findElement(locators.get("iframePage.singUpLink"));
		singUpLink.click();
		if (driver.getWindowHandles().size() == 3)
			parentWindow = selUtils.switchToThirdWindowUsingWebElement("iframeSignUpPage.emailTextbox");
		else
			parentWindow = selUtils.switchToNewWindow();
		WebElement emailTextbox = driver.findElement(locators.get("iframeSignUpPage.emailTextbox"));
		emailTextbox.isDisplayed();
		iframeEmail = prop.getProperty("iFrameEmailPrefix").replace(temp, CreateDateTime.getTimeDateString());
		emailTextbox.sendKeys(iframeEmail);
		logger.info("Signup email is set as: " + iframeEmail);
		TestListeners.extentTest.get().info("Signup email is set as: " + iframeEmail);
		WebElement fNameTextbox = driver.findElement(locators.get("iframeSignUpPage.fNameTextbox"));
		fNameTextbox.isDisplayed();
		fNameTextbox.sendKeys(CreateDateTime.getUniqueString(prop.getProperty("firstName")));
		WebElement lNameTextbox = driver.findElement(locators.get("iframeSignUpPage.lNameTextbox"));
		lNameTextbox.isDisplayed();
		lNameTextbox.sendKeys(CreateDateTime.getUniqueString(prop.getProperty("lastName")));
		WebElement passwordTextbox = driver.findElement(locators.get("iframeSignUpPage.passwordTextbox"));
		passwordTextbox.isDisplayed();
		passwordTextbox.sendKeys(prop.getProperty("iFramePassword"));
		WebElement confPasswordTextbox = driver.findElement(locators.get("iframeSignUpPage.confPasswordTextbox"));
		confPasswordTextbox.isDisplayed();
		confPasswordTextbox.sendKeys(prop.getProperty("iFramePassword"));
		// driver.findElement(locators.get("iframeSignUpPage.subscriptionEmailCheckbox")).isDisplayed();
		// driver.findElement(locators.get("iframeSignUpPage.subscriptionEmailCheckbox")).click();
		WebElement referralCodeTextCode = driver.findElement(locators.get("iframeSignUpPage.referralCodeTextCode"));
		referralCodeTextCode.isDisplayed();
		referralCodeTextCode.clear();
		referralCodeTextCode.sendKeys(referralCode);
		WebElement submitButton = driver.findElement(locators.get("iframeSignUpPage.submitButton"));
		submitButton.isDisplayed();
		submitButton.click();
		logger.info("Clicked in signup submit button");
		TestListeners.extentTest.get().info("Clicked in signup submit button");
		driver.switchTo().window(parentWindow);
		// driver.findElement(locators.get("iframePage.accountBalanceLink")).isDisplayed();
		WebElement signOutLink = driver.findElement(locators.get("iframePage.signOutLink"));
		signOutLink.isDisplayed();
		logger.info("Signup via iframe is successful");
		TestListeners.extentTest.get().pass("Signup via iframe is successful");
		return iframeEmail;
	}

	public void editProfile(String userEmail, String updatedFname, String updatedLname) {
		logger.info("Updating guest profile");
		TestListeners.extentTest.get().info("Updating guest profile");
		WebElement editProfileTabLink = driver.findElement(locators.get("guestTimeLine.editProfileTabLink"));
		editProfileTabLink.isDisplayed();
		editProfileTabLink.click();
		WebElement emailTextbox = driver.findElement(locators.get("iframeSignUpPage.emailTextbox"));
		emailTextbox.clear();
		emailTextbox.sendKeys(userEmail);
		logger.info("Signup email is updated as: " + userEmail);
		TestListeners.extentTest.get().info("Signup email is set as: " + userEmail);
		WebElement fNameTextbox = driver.findElement(locators.get("iframeSignUpPage.fNameTextbox"));
		fNameTextbox.isDisplayed();
		fNameTextbox.clear();
		fNameTextbox.sendKeys(updatedFname);
		WebElement lNameTextbox = driver.findElement(locators.get("iframeSignUpPage.lNameTextbox"));
		lNameTextbox.isDisplayed();
		lNameTextbox.clear();
		lNameTextbox.sendKeys(updatedLname);
		WebElement ucurrentPasswordTextbox = driver.findElement(locators.get("iframePage.ucurrentPasswordTextbox"));
		ucurrentPasswordTextbox.isDisplayed();
		ucurrentPasswordTextbox.sendKeys(prop.getProperty("iFramePassword"));
		WebElement passwordTextbox = driver.findElement(locators.get("iframeSignUpPage.passwordTextbox"));
		passwordTextbox.isDisplayed();
		passwordTextbox.sendKeys(prop.getProperty("iFrameUpdatedPassword"));
		WebElement confPasswordTextbox = driver.findElement(locators.get("iframeSignUpPage.confPasswordTextbox"));
		confPasswordTextbox.isDisplayed();
		confPasswordTextbox.sendKeys(prop.getProperty("iFrameUpdatedPassword"));
//		driver.findElement(locators.get("iframeSignUpPage.subscriptionEmailCheckbox")).isDisplayed();
//		driver.findElement(locators.get("iframeSignUpPage.subscriptionEmailCheckbox")).click();
		// driver.findElement(locators.get("iframeSignUpPage.zipTextbox")).isDisplayed();
		// driver.findElement(locators.get("iframeSignUpPage.zipTextbox")).sendKeys(prop.getProperty("zipCode"));
		WebElement editSubmitButton = driver.findElement(locators.get("iframeSignUpPage.editSubmitButton"));
		editSubmitButton.isDisplayed();
		editSubmitButton.click();
		WebElement accountUpdateLabel = driver.findElement(locators.get("iframeSignUpPage.accountUpdateLabel"));
		accountUpdateLabel.isDisplayed();
		logger.info("Updated account successfully message is appearing");
		TestListeners.extentTest.get().info("Updated account successfully message is appearing");
	}

	public void updateGuestFavLocationinIframe(String location) {
		WebElement editProfileTabLink = driver.findElement(locators.get("guestTimeLine.editProfileTabLink"));
		editProfileTabLink.isDisplayed();
		editProfileTabLink.click();
		WebElement favLocationDropdown = driver.findElement(locators.get("iframeSignUpPage.favLocationDropdown"));
		utils.selectDrpDwnValue(favLocationDropdown, location);
		WebElement ucurrentPasswordTextbox = driver.findElement(locators.get("iframePage.ucurrentPasswordTextbox"));
		ucurrentPasswordTextbox.sendKeys(prop.getProperty("iFramePassword"));
		WebElement editSubmitButton = driver.findElement(locators.get("iframeSignUpPage.editSubmitButton"));
		editSubmitButton.click();
		TestListeners.extentTest.get().info("Updated favLocation in iframe  successfully");
	}

	public String iframeSignUpWithZipcode(String zipCode) {
		String parentWindow;
		try {
			logger.info("==Signing up in iframe==");
			TestListeners.extentTest.get().info("==Signing up in iframe==");
			WebElement singUpLink = driver.findElement(locators.get("iframePage.singUpLink"));
			singUpLink.click();
			if (driver.getWindowHandles().size() == 3)
				parentWindow = selUtils.switchToThirdWindowUsingWebElement("iframeSignUpPage.emailTextbox");
			else
				parentWindow = selUtils.switchToNewWindow();
			WebElement emailTextbox = driver.findElement(locators.get("iframeSignUpPage.emailTextbox"));
			emailTextbox.isDisplayed();
			iframeEmail = prop.getProperty("iFrameEmailPrefix").replace(temp,
					CreateDateTime.getTimeDateString() + CreateDateTime.getRandomString(6).toLowerCase());
			emailTextbox.sendKeys(iframeEmail);
			logger.info("Signup email is set as: " + iframeEmail);
			TestListeners.extentTest.get().info("Signup email is set as: " + iframeEmail);
			WebElement fNameTextbox = driver.findElement(locators.get("iframeSignUpPage.fNameTextbox"));
			fNameTextbox.isDisplayed();
			fNameTextbox.sendKeys(CreateDateTime.getUniqueString(prop.getProperty("firstName")));
			WebElement lNameTextbox = driver.findElement(locators.get("iframeSignUpPage.lNameTextbox"));
			lNameTextbox.isDisplayed();
			lNameTextbox.sendKeys(CreateDateTime.getUniqueString(prop.getProperty("lastName")));
			WebElement passwordTextbox = driver.findElement(locators.get("iframeSignUpPage.passwordTextbox"));
			passwordTextbox.isDisplayed();
			passwordTextbox.sendKeys(prop.getProperty("iFramePassword"));
			WebElement confPasswordTextbox = driver.findElement(locators.get("iframeSignUpPage.confPasswordTextbox"));
			confPasswordTextbox.isDisplayed();
			confPasswordTextbox.sendKeys(prop.getProperty("iFramePassword"));
			WebElement zipCodeTextbox = driver.findElement(locators.get("iframeSignUpPage.zipCodeTextbox"));
			zipCodeTextbox.isDisplayed();
			zipCodeTextbox.sendKeys(zipCode);
			WebElement subscriptionEmailCheckbox = driver.findElement(locators.get("iframeSignUpPage.subscriptionEmailCheckbox"));
			subscriptionEmailCheckbox.isDisplayed();
			subscriptionEmailCheckbox.click();
			WebElement submitButton = driver.findElement(locators.get("iframeSignUpPage.submitButton"));
			submitButton.isDisplayed();
			submitButton.click();
			logger.info("Clicked in signup submit button");
			TestListeners.extentTest.get().info("Clicked in signup submit button");
			if (zipCode.length() <= 10) {
				driver.switchTo().window(parentWindow);
				WebElement signOutLink = driver.findElement(locators.get("iframePage.signOutLink"));
				signOutLink.isDisplayed();
				logger.info("Signup via iframe is successful");
				TestListeners.extentTest.get().pass("Signup via iframe is successful");
			} else {
				WebElement signUpError = driver.findElement(locators.get("iframeSignUpPage.signUpError"));
				signUpError.isDisplayed();
				String errorMsg = signUpError.getText();
				Assert.assertEquals(errorMsg, "ZipCode is too long (maximum is 10 characters)",
						"Error message is not as expected");
				logger.info("Signup via iframe is unsuccessful due to invalid zipcode");
				TestListeners.extentTest.get().pass("Signup via iframe is unsuccessful due to invalid zipcode");
			}
		} catch (Exception e) {
			logger.error("Error in iFrame signup" + e);
			TestListeners.extentTest.get().fail("Error in iFrame signup" + e);
		}
		return iframeEmail;

	}

	public String generateEmail1() {
		iframeEmail = prop.getProperty("iFrameEmailPrefix1").replace(temp, CreateDateTime.getRandomNumberSixDigit());
		logger.info(iframeEmail + " user is created");
		TestListeners.extentTest.get().pass(iframeEmail + " user random email created");
		return iframeEmail;
	}

	public String editZipcodeInIframe(String zipCode, String status) {
		logger.info("Updating guest profile");
		TestListeners.extentTest.get().info("Updating guest profile");
		String msgOnUi = null;
		WebElement editProfileTabLink = driver.findElement(locators.get("guestTimeLine.editProfileTabLink"));
		editProfileTabLink.isDisplayed();
		editProfileTabLink.click();
		WebElement ucurrentPasswordTextbox = driver.findElement(locators.get("iframePage.ucurrentPasswordTextbox"));
		ucurrentPasswordTextbox.isDisplayed();
		ucurrentPasswordTextbox.sendKeys(prop.getProperty("iFramePassword"));
		WebElement zipTextbox = driver.findElement(locators.get("iframeSignUpPage.zipTextbox"));
		zipTextbox.isDisplayed();
		zipTextbox.clear();
		zipTextbox.sendKeys(zipCode);
		WebElement editSubmitButton = driver.findElement(locators.get("iframeSignUpPage.editSubmitButton"));
		utils.scrollToElement(driver, editSubmitButton);
		editSubmitButton.isDisplayed();
		utils.waitTillElementToBeClickable(editSubmitButton);
		utils.clickByJSExecutor(driver, editSubmitButton);
//		driver.findElement(locators.get("iframeSignUpPage.editSubmitButton")).click();
		if (status.equalsIgnoreCase("validZipCode")) {
			WebElement iframeErrorMessage = driver.findElement(locators.get("iframeSignUpPage.iframeErrorMessage"));
			iframeErrorMessage.isDisplayed();
			msgOnUi = iframeErrorMessage.getText();
		} else if (status.equalsIgnoreCase("inValidZipCode")) {
			WebElement signUpError = driver.findElement(locators.get("iframeSignUpPage.signUpError"));
			signUpError.isDisplayed();
			msgOnUi = signUpError.getText();
		}
		logger.info("Message on Iframe is appearing as : " + msgOnUi);
		TestListeners.extentTest.get().pass("Message on Iframe is appearing as : " + msgOnUi);
		return msgOnUi;
	}

	public String verifyIframeLoginErrorMessage(String email, String password) {
		String parentWindow = null;
		String errorMessage = "";
		WebElement loginLink = driver.findElement(locators.get("iframePage.loginLink"));
		loginLink.isDisplayed();
		loginLink.click();
		parentWindow = selUtils.switchToNewWindow();
		WebElement emailTextbox = driver.findElement(locators.get("iframeSignUpPage.emailTextbox"));
		emailTextbox.sendKeys(email);
		WebElement passwordTextbox = driver.findElement(locators.get("iframeSignUpPage.passwordTextbox"));
		passwordTextbox.isDisplayed();
		passwordTextbox.sendKeys(password);
		WebElement submitButton = driver.findElement(locators.get("iframeSignUpPage.submitButton"));
		submitButton.isDisplayed();
		submitButton.click();
		selUtils.longWait(2);
		WebElement iframeErrorMessage = driver.findElement(locators.get("iframeSignUpPage.iframeErrorMessage"));
		errorMessage = iframeErrorMessage.getText();
		System.out.println("Iframe errorMessage - " + errorMessage);
		logger.error("Iframe errorMessage - " + errorMessage);
		TestListeners.extentTest.get().pass("Iframe errorMessage - " + errorMessage);
		return errorMessage;
	}

	// CheckIn via receipt upload
	public void receiptCheckIn(String location) {
		try {
			WebElement earnLink = driver.findElement(locators.get("iframePage.earnLink"));
			earnLink.isDisplayed();
			earnLink.click();
			WebElement locationDrpDwnList = driver.findElement(locators.get("iframePage.locationDrpDwnList"));
			utils.selectDrpDwnValue(locationDrpDwnList, location);
			WebElement checkinReceiptImage = driver.findElement(locators.get("iframePage.checkinReceiptImage"));
			checkinReceiptImage.isDisplayed();
			checkinReceiptImage.sendKeys(System.getProperty("user.dir") + "/resources/Images/Receipt.jpeg");
			WebElement submitBtn = driver.findElement(locators.get("iframeSignUpPage.submitBtn"));
			submitBtn.isDisplayed();
			submitBtn.click();
		} catch (Exception e) {
			logger.error("Error while doing receipt checkin	: " + e);
			TestListeners.extentTest.get().fail("Error while doing receipt checkin: " + e);
		}
	}

	public String iframeSignUpWrongPassword() {
		String parentWindow;

		logger.info("==Signing up in iframe==");
		TestListeners.extentTest.get().info("==Signing up in iframe==");
		WebElement singUpLink = driver.findElement(locators.get("iframePage.singUpLink"));
		singUpLink.click();
		if (driver.getWindowHandles().size() == 3)
			parentWindow = selUtils.switchToThirdWindowUsingWebElement("iframeSignUpPage.emailTextbox");
		else
			parentWindow = selUtils.switchToNewWindow();
		WebElement emailTextbox = driver.findElement(locators.get("iframeSignUpPage.emailTextbox"));
		emailTextbox.isDisplayed();
		iframeEmail = prop.getProperty("iFrameEmailPrefix").replace(temp,
				CreateDateTime.getTimeDateString() + CreateDateTime.getRandomString(6).toLowerCase());
		emailTextbox.sendKeys(iframeEmail);
		logger.info("Signup email is set as: " + iframeEmail);
		TestListeners.extentTest.get().info("Signup email is set as: " + iframeEmail);
		WebElement fNameTextbox = driver.findElement(locators.get("iframeSignUpPage.fNameTextbox"));
		fNameTextbox.isDisplayed();
		fNameTextbox.sendKeys(CreateDateTime.getUniqueString(prop.getProperty("firstName")));
		WebElement lNameTextbox = driver.findElement(locators.get("iframeSignUpPage.lNameTextbox"));
		lNameTextbox.isDisplayed();
		lNameTextbox.sendKeys(CreateDateTime.getUniqueString(prop.getProperty("lastName")));
		WebElement passwordTextbox = driver.findElement(locators.get("iframeSignUpPage.passwordTextbox"));
		passwordTextbox.isDisplayed();
		passwordTextbox.sendKeys("12345678");
		WebElement confPasswordTextbox = driver.findElement(locators.get("iframeSignUpPage.confPasswordTextbox"));
		confPasswordTextbox.isDisplayed();
		confPasswordTextbox.sendKeys("87654321");

		WebElement submitButton = driver.findElement(locators.get("iframeSignUpPage.submitButton"));
		submitButton.isDisplayed();
		submitButton.sendKeys(Keys.ENTER);
		selUtils.longWait(3000);
		logger.info("Clicked in signup submit button");

		String errorMsg = errorSignUp();
		TestListeners.extentTest.get().info("clicked in the signup button and error is displayed");

		driver.switchTo().window(parentWindow);

		return errorMsg;
	}

	public String errorSignUp() {
		WebElement signUpError = driver.findElement(locators.get("iframeSignUpPage.signUpError"));
		String errorMsg = signUpError.getText();
		return errorMsg;
	}

	public void verifyUserPrefilledDetails(String phoneNumber, String userFName) {
		WebElement phoneNMumberInputBoxXpath = driver.findElement(locators.get("iframeSignUpPage.phoneNMumberInputBoxXpath"));
		String actualPhoneNumber = phoneNMumberInputBoxXpath.getAttribute("value");
		String isPhoneDisable = phoneNMumberInputBoxXpath.getAttribute("disabled");

		long actualPhone = Long.parseLong(actualPhoneNumber);
		long expPhoneNum = Long.parseLong(phoneNumber);
		Assert.assertEquals(actualPhone, expPhoneNum);
		TestListeners.extentTest.get().info(phoneNumber + " phone number is matched ");

		Assert.assertEquals(isPhoneDisable, "true", "Phone number input box is editable ");
		TestListeners.extentTest.get().info("Verified that phone input box is not editable ");

		WebElement fNameTextbox = driver.findElement(locators.get("iframeSignUpPage.fNameTextbox"));
		String actualFName = fNameTextbox.getAttribute("value");
		Assert.assertEquals(actualFName, userFName, userFName + " username is not matched ");
		TestListeners.extentTest.get().info(userFName + " user name is matched , Verified ");

	}

	public int getCurrentPointAfterSignupForPrefilledURL(String emailID) throws InterruptedException {
		WebElement emailTextbox = driver.findElement(locators.get("iframeSignUpPage.emailTextbox"));
		emailTextbox.sendKeys(emailID);
		WebElement passwordTextbox = driver.findElement(locators.get("iframeSignUpPage.passwordTextbox"));
		passwordTextbox.isDisplayed();
		passwordTextbox.sendKeys(prop.getProperty("iFramePassword"));
		WebElement confPasswordTextbox = driver.findElement(locators.get("iframeSignUpPage.confPasswordTextbox"));
		confPasswordTextbox.isDisplayed();
		confPasswordTextbox.sendKeys(prop.getProperty("iFramePassword"));
		Utilities.longWait(1000);
		WebElement submitButton = driver.findElement(locators.get("iframeSignUpPage.submitButton"));
		submitButton.sendKeys(Keys.ENTER);
		logger.info("Clicked in signup submit button");
		TestListeners.extentTest.get().info("Clicked in signup submit button");
//		Utilities.longWait(4000);
		int currentPoints = 0;
		int counter = 0;
		do {
			WebElement currentPointXpath = driver.findElement(locators.get("iframeSignUpPage.currentPointXpath"));
			currentPoints = Integer.parseInt(currentPointXpath.getText());
			utils.refreshPage();
			utils.longwait(1500);
			counter++;
		} while ((currentPoints != 2) && (counter != 20));
		return currentPoints;
	}

	public String redeemRewardFromRedeemrewardsWithNewUI(String rewardName) {
		String reward_code = "";
		WebElement redeemRewardsLink = driver.findElement(locators.get("iframePage.redeemRewardsLink"));
		redeemRewardsLink.isDisplayed();
		redeemRewardsLink.click();
		String rewardRadioXpath = utils.getLocatorValue("iframePage.selectReward_RadioButton");
		rewardRadioXpath = rewardRadioXpath.replace("${rewardName}", rewardName);
		WebElement selectRewardRadioButton = driver.findElement(By.xpath(rewardRadioXpath));
		selectRewardRadioButton.click();
		WebElement redeemAddress = driver.findElement(locators.get("iframePage.redeemAddress"));
		if (redeemAddress.isDisplayed()) {
			redeemAddress.sendKeys(prop.getProperty("iframeAddress"));
			WebElement redeemCityName = driver.findElement(locators.get("iframePage.redeemCityName"));
			redeemCityName.sendKeys(prop.getProperty("iframeCity"));
			WebElement redeemState = driver.findElement(locators.get("iframePage.redeemState"));
			redeemState.sendKeys(prop.getProperty("iframeState"));
			WebElement redeemPhoneNumber = driver.findElement(locators.get("iframePage.redeemPhoneNumber"));
			redeemPhoneNumber.sendKeys(prop.getProperty("iframePhoneNumber"));
			WebElement redeemZipCode = driver.findElement(locators.get("iframePage.redeemZipCode"));
			redeemZipCode.sendKeys(prop.getProperty("iFramePassword"));
			WebElement redeemMerchandizeSize = driver.findElement(locators.get("iframePage.redeemMerchandizeSize"));
			redeemMerchandizeSize.sendKeys(prop.getProperty("iframeMerchandizeSize"));
		}
		WebElement redeemBtn = driver.findElement(locators.get("iframePage.redeemBtn"));
		redeemBtn.click();
		utils.acceptAlert(driver);
		WebElement redemptionCode = driver.findElement(locators.get("iframePage.redemptionCode"));
		reward_code = redemptionCode.getText();
		logger.info("Congratulations! Your redemption code is: " + reward_code);
		TestListeners.extentTest.get().pass("Congratulations! Your redemption code is: " + reward_code);
		return reward_code;
	}

	public void clickRedeemReward() {
		WebElement redeemRewardsButton = driver.findElement(locators.get("iframePage.redeemRewardsButton"));
		utils.waitTillVisibilityOfElement(redeemRewardsButton, "redeem rewards");
		redeemRewardsButton.click();

		WebElement redeemForm = driver.findElement(locators.get("iframePage.redeemForm"));
		utils.waitTillVisibilityOfElement(redeemForm, "form");

		logger.info("clicked on the redeem rewards");
		TestListeners.extentTest.get().info("clicked on the redeem rewards");
	}

	public List<String> clickRedeemBtnGetMsg() {
		List<String> lst = new ArrayList<>();
		WebElement redeemBtn = driver.findElement(locators.get("iframePage.redeemBtn"));
		utils.waitTillVisibilityOfElement(redeemBtn, "redeem button");
		redeemBtn.click();
		utils.acceptAlert(driver);

		WebElement errorMsg = driver.findElement(locators.get("iframePage.errorMsg"));
		utils.waitTillVisibilityOfElement(errorMsg, "error msg");
		String text = errorMsg.getText();
		String color = errorMsg.getCssValue("color");
		String hexcode = Color.fromString(color).asHex();
		lst.add(text);
		lst.add(hexcode);
		return lst;
	}

	public boolean viewMoreVisible() {
		boolean val = false;
		try {
			utils.implicitWait(3);
			WebElement viewMoreBtn = driver.findElement(locators.get("iframePage.viewMoreBtn"));
			if (utils.checkElementPresent(viewMoreBtn)) {
				val = true;
				logger.info("view button is displayed");
				TestListeners.extentTest.get().info("view button is displayed");
			}
		} catch (Exception e) {
			logger.info("view button not displayed " + e);
		}
		utils.implicitWait(50);
		return val;
	}

	public boolean iframeLoginUsingWindowIndex(String email, String password, int index) {
		boolean flag = false;
		try {
			WebElement loginLink = driver.findElement(locators.get("iframePage.loginLink"));
			loginLink.isDisplayed();
			loginLink.click();
			utils.switchToWindowByIndex(driver, index);
			WebElement emailTextbox = driver.findElement(locators.get("iframeSignUpPage.emailTextbox"));
			emailTextbox.sendKeys(email);
			WebElement passwordTextbox = driver.findElement(locators.get("iframeSignUpPage.passwordTextbox"));
			passwordTextbox.isDisplayed();
			passwordTextbox.sendKeys(password);
			WebElement submitButton = driver.findElement(locators.get("iframeSignUpPage.submitButton"));
			submitButton.isDisplayed();
			submitButton.click();
			utils.switchToWindowByIndex(driver, 1);
			WebElement signOutLink = driver.findElement(locators.get("iframePage.signOutLink"));
			signOutLink.isDisplayed();
			logger.info("Login via iframe is successful");
			TestListeners.extentTest.get().pass("Login via iframe is successful");
			flag = true;
		} catch (Exception e) {
			flag = false;
			logger.error("Error in navigating to iFrame signup/login " + e);
			TestListeners.extentTest.get().fail("Error in navigating to iFrame signup/login " + e);
		}
		return flag;
	}

	public void clickViewMoreButton() {
		WebElement viewMoreBtn = driver.findElement(locators.get("iframePage.viewMoreBtn"));
		viewMoreBtn.click();
		logger.info("clicked on view more button");
		TestListeners.extentTest.get().info("clicked on view more button");
	}

	public int getLockedRedeemableList() {
		List<WebElement> webEle = driver.findElements(locators.get("iframePage.lockedRedeemableLst"));
		int size = webEle.size();
		return size;
	}

	public List<String> redeemUnlockedReward(String redeemableName) {
		List<String> lst = new ArrayList<>();
		String xpath = utils.getLocatorValue("iframePage.unlockedRedeemableName").replace("{$redeemableName}",
				redeemableName);
		WebElement unlockedRedeemableName = driver.findElement(By.xpath(xpath));
		unlockedRedeemableName.click();
		WebElement redeemBtn = driver.findElement(locators.get("iframePage.redeemBtn"));
		redeemBtn.click();
		utils.acceptAlert(driver);

		WebElement errorMsg = driver.findElement(locators.get("iframePage.errorMsg"));
		utils.waitTillVisibilityOfElement(errorMsg, "error msg");
		String text = errorMsg.getText();
		String color = errorMsg.getCssValue("color");
		String hexcode = Color.fromString(color).asHex();
		lst.add(text);
		lst.add(hexcode);
		return lst;
	}

	public boolean pointsVisible(String pointsName, String points) {
		String xpath = utils.getLocatorValue("iframePage.pointsVisible").replace("{pointsName}", pointsName)
				.replace("{points}", points);
		System.out.println(xpath);
		try {
			WebElement pointsVisible = driver.findElement(By.xpath(xpath));
			pointsVisible.isDisplayed();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean unlockedRewardVisible(String redeemableName) {
		String xpath = utils.getLocatorValue("iframePage.unlockedRewardVisible").replace("{redeemableName}",
				redeemableName);
		try {
			utils.refreshPage();
			clickRedeemReward();
			utils.implicitWait(5);
			WebElement unlockedRewardVisible = driver.findElement(By.xpath(xpath));
			unlockedRewardVisible.isDisplayed();
			return true;
		} catch (Exception e) {
			utils.implicitWait(50);
			return false;
		}
	}

	public void iframeLoginToCheckReactivation(String email, String password, int index) {
		WebElement loginLink = driver.findElement(locators.get("iframePage.loginLink"));
		loginLink.isDisplayed();
		loginLink.click();
		utils.switchToWindowByIndex(driver, index);
		WebElement emailTextbox = driver.findElement(locators.get("iframeSignUpPage.emailTextbox"));
		emailTextbox.sendKeys(email);
		WebElement passwordTextbox = driver.findElement(locators.get("iframeSignUpPage.passwordTextbox"));
		passwordTextbox.isDisplayed();
		passwordTextbox.sendKeys(password);
		WebElement submitButton = driver.findElement(locators.get("iframeSignUpPage.submitButton"));
		submitButton.isDisplayed();
		submitButton.click();
		logger.info("clicked on the submit button ");
		TestListeners.extentTest.get().info("clicked on the submit button ");
		// utils.switchToWindowByIndex(driver, 1);
	}

	public String msgVisible() {
		utils.longwait(2000);
		WebElement msgVisible = driver.findElement(locators.get("iframeSignUpPage.msgVisible"));
		utils.waitTillInVisibilityOfElement(msgVisible, "deactivate msg");
		String text = msgVisible.getText();
		logger.info("returning the message " + text);
		TestListeners.extentTest.get().info("returning the message " + text);
		return text;
	}

	public void clickReactivateBtn() {
		WebElement reactivateBtn = driver.findElement(locators.get("iframeSignUpPage.reactivateBtn"));
		utils.waitTillInVisibilityOfElement(reactivateBtn, "reactivate button");
		utils.clickByJSExecutor(driver, reactivateBtn);
		logger.info("clicked on the reactivation button");
		TestListeners.extentTest.get().info("clicked on the reactivation button");
	}

	public void sendReactivateEmail(String mail) {
		WebElement reactivateEmailBox = driver.findElement(locators.get("iframeSignUpPage.reactivateEmailBox"));
		utils.waitTillInVisibilityOfElement(reactivateEmailBox, "email field");
		reactivateEmailBox.sendKeys(mail);
		WebElement sendReactivationButton = driver.findElement(locators.get("iframeSignUpPage.sendReactivationButton"));
		sendReactivationButton.click();
		utils.longwait(1000);
	}

	public String getIframeMessage(String barcode) {
		submitBarcode(barcode);
		WebElement getIframeMessage = driver.findElement(locators.get("iframeSignUpPage.getIframeMessage"));
		String text = getIframeMessage.getText();
		logger.info("Iframe message : " + text);
		TestListeners.extentTest.get().info("Iframe message : " + text);
		return text;
	}

	public void verifyACDeactivation() {
		utils.waitTillCompletePageLoad();
		WebElement deactivateAccountCTA = driver.findElement(locators.get("iframeEditProfilePage.DeactivateAccountCTA"));
		deactivateAccountCTA.isDisplayed();
		logger.info("DeactivateAccountCTA is available");
		TestListeners.extentTest.get().info("DeactivateAccountCTA is available");

		Actions actions = new Actions(driver);

		utils.StaleElementclick(driver, deactivateAccountCTA);
		// actions.moveToElement(driver.findElement(locators.get("iframeEditProfilePage.DeactivateAccountCTA"))).click().perform();
		// driver.findElement(locators.get("iframeEditProfilePage.DeactivateAccountCTA")).click();

		logger.info("clicked on DeactivateAccountCTA");
		TestListeners.extentTest.get().info("clicked on DeactivateAccountCTA");
		utils.longWait(1000);
		WebElement deactivateAccountConfirmationPopupClose = driver.findElement(locators.get("iframeEditProfilePage.DeactivateAccountConfirmationPopupClose"));
		deactivateAccountConfirmationPopupClose.isDisplayed();
		actions.moveToElement(deactivateAccountConfirmationPopupClose).click()
				.perform();
		logger.info("clicked on DeactivateAccountConfirmationPopup Close button");
		TestListeners.extentTest.get().info("clicked on DeactivateAccountConfirmationPopup Close button");
		utils.longWait(1000);
		WebElement deactivateAccountCTAAgain = driver.findElement(locators.get("iframeEditProfilePage.DeactivateAccountCTA"));
		actions.moveToElement(deactivateAccountCTAAgain).click().perform();
		logger.info("clicked on DeactivateAccountCTA again");
		TestListeners.extentTest.get().info("clicked on DeactivateAccountCTA again");
		utils.longWait(1000);
		WebElement deactivateAccountConfirmationCTA = driver.findElement(locators.get("iframeEditProfilePage.DeactivateAccountConfirmationCTA"));
		deactivateAccountConfirmationCTA.isDisplayed();
		actions.moveToElement(deactivateAccountConfirmationCTA).click()
				.perform();
		logger.info("clicked on DeactivateAccountConfirmationCTA");
		TestListeners.extentTest.get().info("clicked on DeactivateAccountConfirmationCTA");
		WebElement deactiveSuccessMessage = driver.findElement(locators.get("iframeEditProfilePage.DeactiveSuccessMessage"));
		String message = deactiveSuccessMessage.getText();
		Assert.assertEquals(message, "DEACTIVATED SUCCESSFULLY");

		logger.info(" Account Deactivate is successfull ");
		TestListeners.extentTest.get().info("Account Deactivate is successfull");

	}

	public void verifyACDeletion() {
		WebElement deleteAccountCTA = driver.findElement(locators.get("iframeEditProfilePage.DeleteAccountCTA"));
		deleteAccountCTA.isDisplayed();
		logger.info("DeleteAccountCTA is available");
		TestListeners.extentTest.get().info("DeleteAccountCTA is available");

		Actions actions = new Actions(driver);

		// actions.moveToElement(driver.findElement(locators.get("iframeEditProfilePage.DeleteAccountCTA"))).click().perform();
		utils.StaleElementclick(driver, deleteAccountCTA);
		logger.info("clicked on DeleteAccountCTA");
		TestListeners.extentTest.get().info("clicked on DeleteAccountCTA");
		utils.longWait(1000);
		WebElement deleteAccountConfirmationPopupClose = driver.findElement(locators.get("iframeEditProfilePage.DeleteAccountConfirmationPopupClose"));
		deleteAccountConfirmationPopupClose.isDisplayed();
		actions.moveToElement(deleteAccountConfirmationPopupClose).click()
				.perform();
		logger.info("clicked on DeleteAccountConfirmationPopup Close button");
		TestListeners.extentTest.get().info("clicked on DeleteAccountConfirmationPopup Close  button");
		utils.longWait(1000);
		WebElement deleteAccountCTAAgain = driver.findElement(locators.get("iframeEditProfilePage.DeleteAccountCTA"));
		actions.moveToElement(deleteAccountCTAAgain).click().perform();
		logger.info("clicked on DeleteAccountCTA again");
		TestListeners.extentTest.get().info("clicked on DeleteAccountCTA again");
		utils.longWait(1000);
		WebElement deleteAccountConfirmationCTA = driver.findElement(locators.get("iframeEditProfilePage.DeleteAccountConfirmationCTA"));
		deleteAccountConfirmationCTA.isDisplayed();
		actions.moveToElement(deleteAccountConfirmationCTA).click().perform();
		logger.info("clicked on DeleteAccountConfirmationCTA");
		TestListeners.extentTest.get().info("clicked on DeleteAccountConfirmationCTA");
		WebElement deleteSuccessMessage = driver.findElement(locators.get("iframeEditProfilePage.DeleteSuccessMessage"));
		String message = deleteSuccessMessage.getText();
		Assert.assertEquals(message, "DELETED SUCCESSFULLY");

		logger.info(" Account deletion was successfull ");
		TestListeners.extentTest.get().info("Account Deactivate was successfull");

		WebElement returnHomeCTA = driver.findElement(locators.get("iframeEditProfilePage.ReturnHomeCTA"));
		returnHomeCTA.click();
		utils.longWait(1000);
		String Durl = driver.getCurrentUrl();
		Assert.assertEquals(Durl, "https://punchh.com/");
		logger.info(" Directed to " + Durl + "after deletion");

	}

	public void iframeSignUp(String userEmail) {
		String parentWindow;
		try {
			logger.info("==Signing up in iframe==");
			TestListeners.extentTest.get().info("==Signing up in iframe==");
			WebElement singUpLink = driver.findElement(locators.get("iframePage.singUpLink"));
			utils.waitTillVisibilityOfElement(singUpLink, "Sign up button");
			singUpLink.click();
			if (driver.getWindowHandles().size() == 3)
				parentWindow = selUtils.switchToThirdWindowUsingWebElement("iframeSignUpPage.emailTextbox");
			else
				parentWindow = selUtils.switchToNewWindow();
			WebElement emailTextbox = driver.findElement(locators.get("iframeSignUpPage.emailTextbox"));
			emailTextbox.isDisplayed();
			emailTextbox.sendKeys(userEmail);
			logger.info("Signup email is set as: " + userEmail);
			TestListeners.extentTest.get().info("Signup email is set as: " + userEmail);
			WebElement fNameTextbox = driver.findElement(locators.get("iframeSignUpPage.fNameTextbox"));
			fNameTextbox.isDisplayed();
			fNameTextbox.sendKeys(CreateDateTime.getUniqueString(prop.getProperty("firstName")));
			WebElement lNameTextbox = driver.findElement(locators.get("iframeSignUpPage.lNameTextbox"));
			lNameTextbox.isDisplayed();
			lNameTextbox.sendKeys(CreateDateTime.getUniqueString(prop.getProperty("lastName")));
			WebElement passwordTextbox = driver.findElement(locators.get("iframeSignUpPage.passwordTextbox"));
			passwordTextbox.isDisplayed();
			passwordTextbox.sendKeys(prop.getProperty("iFramePassword"));
			WebElement confPasswordTextbox = driver.findElement(locators.get("iframeSignUpPage.confPasswordTextbox"));
			confPasswordTextbox.isDisplayed();
			confPasswordTextbox.sendKeys(prop.getProperty("iFramePassword"));
//			driver.findElement(locators.get("iframeSignUpPage.subscriptionEmailCheckbox")).isDisplayed();
//			driver.findElement(locators.get("iframeSignUpPage.subscriptionEmailCheckbox")).click();
			WebElement submitButton = driver.findElement(locators.get("iframeSignUpPage.submitButton"));
			submitButton.isDisplayed();
			// driver.findElement(locators.get("iframeSignUpPage.submitButton")).click();
			utils.longWait(1000);
			submitButton.sendKeys(Keys.ENTER);
			try {
				WebElement saveProfileButton = driver.findElement(locators.get("iframeSignUpPage.saveProfileButton"));
				saveProfileButton.click();
				Thread.sleep(2000);
				logger.info("Save Profile button is visible and clicked");
				TestListeners.extentTest.get().info("Save Profile button is visible and clicked");
			} catch (Exception e) {
				logger.info("Save Profile button is not visible");
				TestListeners.extentTest.get().info("Save Profile button is not visible");
			}
			logger.info("Clicked in signup submit button");
			TestListeners.extentTest.get().info("Clicked in signup submit button");
			driver.switchTo().window(parentWindow);
			// driver.findElement(locators.get("iframePage.accountBalanceLink")).isDisplayed();
			WebElement signOutLink = driver.findElement(locators.get("iframePage.signOutLink"));
			signOutLink.isDisplayed();
			logger.info("Signup via iframe is successful");
			TestListeners.extentTest.get().pass("Signup via iframe is successful");
		} catch (Exception e) {
			WebElement submitButton = driver.findElement(locators.get("iframeSignUpPage.submitButton"));
			submitButton.click();
			logger.error("Error in iFrame signup" + e);
			TestListeners.extentTest.get().fail("Error in iFrame signup" + e);
		}
	}

	public void clickEditprofileButton() {
		WebElement editProfileLink = driver.findElement(locators.get("iframePage.editProfileLink"));
		editProfileLink.click();
		logger.info("clicked on edit profile section");
		TestListeners.extentTest.get().info("clicked on edit profile section");
	}

	public void clickChekinButton() {
		WebElement checkinLink = driver.findElement(locators.get("iframePage.checkinLink"));
		checkinLink.click();
		logger.info("clicked on checkin section");
		TestListeners.extentTest.get().info("clicked on checkinLink section");
	}

	public void verifyQRCodeinCheckinPage() {
		// driver.findElement(locators.get("iframePage.locationDrpDwnList")).click();
		WebElement qRCodeInCheckinPage = driver.findElement(locators.get("iframePage.QRCodeInCheckinPage"));
		qRCodeInCheckinPage.isDisplayed();
		logger.info("QR code is displayed when loctiondrop is not available");
		TestListeners.extentTest.get().info("QR code is available when loctiondrop is not available");
	}

	public void verifyQRCodeinCheckinPageWhenLoctionSelected() {

		WebElement locationDrpdown = driver.findElement(locators.get("iframePage.LocationDrpdown"));
		locationDrpdown.click();
		WebElement locationDrpdownlist1 = driver.findElement(locators.get("iframePage.LocationDrpdownlist1"));
		locationDrpdownlist1.click();
		utils.longWait(1000);
		WebElement qRCodeInCheckinPage = driver.findElement(locators.get("iframePage.QRCodeInCheckinPage"));
		qRCodeInCheckinPage.isDisplayed();
		logger.info("QR Code inCheckin Page was available based on Loction Selected(");
		TestListeners.extentTest.get().info("QR Code inCheckin Page was available based on Loction Selected");

	}

	public void VerifyMSGWhenNOPhone() {

		WebElement locationDrpdown = driver.findElement(locators.get("iframePage.LocationDrpdown"));
		locationDrpdown.click();
		WebElement locationDrpdownlist2 = driver.findElement(locators.get("iframePage.LocationDrpdownlist2"));
		locationDrpdownlist2.click();
		WebElement msgWhenNOPhone = driver.findElement(locators.get("iframePage.MsgWhenNOPhone"));
		String Msg = msgWhenNOPhone.getText();
		Assert.assertEquals(Msg, "Add a phone number to your loyalty account and share it at the register to earn.");
		logger.info(Msg + " displays when user does have a Phone number");
		TestListeners.extentTest.get().info(Msg + " displays when user does have a Phone number");
	}

	public String redeemAmount(String amount) throws InterruptedException {
		String reward_code = "";
		Set<String> listWindow = driver.getWindowHandles();
		for (String str : listWindow) {
			driver.switchTo().window(str);
		}
		// Click on redeem reward link
		utils.longWaitInSeconds(2);
		WebElement redeemRewardXpath = driver.findElement(locators.get("iframePage.redeemRewardXpath"));
		utils.waitTillElementToBeClickable(redeemRewardXpath);
		redeemRewardXpath.click();
		WebElement redeemptionAmountInputBoxXpath = driver.findElement(locators.get("iframePage.redeemptionAmountInputBoxXpath"));
		redeemptionAmountInputBoxXpath.clear();
		redeemptionAmountInputBoxXpath.sendKeys(amount);
		WebElement redeemSubmitButtonXpath = driver.findElement(locators.get("iframePage.redeemSubmitButtonXpath"));
		redeemSubmitButtonXpath.click();
		utils.acceptAlert(driver);
		utils.longWaitInSeconds(2);
		WebElement redemptionCode = driver.findElement(locators.get("iframePage.redemptionCode"));
		reward_code = redemptionCode.getText();
		logger.info("Congratulations! Your redemption code is: " + reward_code);
		TestListeners.extentTest.get().pass("Congratulations! Your redemption code is: " + reward_code);
		return reward_code;

	}

	// shashank
	public String redeemRedeemableReward(String redeemableName) throws InterruptedException {
		String reward_code = "";
		Set<String> listWindow = driver.getWindowHandles();
		for (String str : listWindow) {
			driver.switchTo().window(str);
		}
		// CLICK ON redeem reward link
		utils.longWaitInSeconds(2);
		WebElement offersLink = driver.findElement(locators.get("iframePage.offersLink"));
		utils.waitTillElementToBeClickable(offersLink);
		offersLink.click();
		// select reward from dropdown
		WebElement rewardsDrpDwn = driver.findElement(locators.get("iframePage.rewardsDrpDwn"));
		utils.selectDrpDwnValue(rewardsDrpDwn, redeemableName);
		WebElement redeemSubmitButtonXpath = driver.findElement(locators.get("iframePage.redeemSubmitButtonXpath"));
		redeemSubmitButtonXpath.click();
		utils.acceptAlert(driver);
		utils.longWaitInSeconds(2);
		WebElement redemptionCode = driver.findElement(locators.get("iframePage.redemptionCode"));
		reward_code = redemptionCode.getText();
		logger.info("Congratulations! Your redemption code is: " + reward_code);
		TestListeners.extentTest.get().pass("Congratulations! Your redemption code is: " + reward_code);
		return reward_code;

	}

	public void verifyBarCodeinCheckinPageWhenLoctionSelected() {
		clickChekinButton();
		WebElement locationDrpdown = driver.findElement(locators.get("iframePage.LocationDrpdown"));
		locationDrpdown.click();
		WebElement locationDrpdownlist2 = driver.findElement(locators.get("iframePage.LocationDrpdownlist2"));
		locationDrpdownlist2.click();
		utils.longWait(1000);
		WebElement barCodeInCheckinPage = driver.findElement(locators.get("iframePage.BarCodeInCheckinPage"));
		barCodeInCheckinPage.isDisplayed();
		logger.info("Bar Code in Checkin Page was available based on Loction Selected(");
		TestListeners.extentTest.get().info("Bar Code in Checkin Page was available based on Loction Selected");
	}

	public void AddPhoneForaUserOniFrame() {

		clickEditprofileButton();
		String Phone = prop.getProperty("phonePrefix").replace(temp, CreateDateTime.getTimeDateString());
		WebElement editProfilePhone = driver.findElement(locators.get("iframePage.EditProfilePhone"));
		editProfilePhone.sendKeys(Phone);
		WebElement editProfileCurrentPWD = driver.findElement(locators.get("iframePage.EditProfileCurrentPWD"));
		editProfileCurrentPWD.sendKeys(prop.getProperty("iFramePassword"));
		WebElement editProfileSubmit = driver.findElement(locators.get("iframePage.EditProfileSubmit"));
		editProfileSubmit.click();

	}

	public void verifyNOQR_BarCodeWhenFlagoff() {
		try {
			WebElement barCodeInCheckinPage = driver.findElement(locators.get("iframePage.BarCodeInCheckinPage"));
			barCodeInCheckinPage.isDisplayed();
			logger.info("Bar Code in Checkin Page was available based on Loction Selected(");
			TestListeners.extentTest.get().info("Bar Code in Checkin Page was available based on Loction Selected");

		} catch (Exception e) {
			logger.info(
					"No QR Code or Barcode displays on iframe  when Show QR code on Checkin screen for earning? is turned off");
			TestListeners.extentTest.get().info(
					"No QR Code or Barcode displays on iframe  when Show QR code on Checkin screen for earning? is turned off");
		}
	}

	public String getPhonePlaceholder() {
		String parentWindow;
		logger.info("==Signing up in iframe==");
		TestListeners.extentTest.get().info("==Signing up in iframe==");
		WebElement singUpLink = driver.findElement(locators.get("iframePage.singUpLink"));
		utils.waitTillVisibilityOfElement(singUpLink, "Sign up button");
		singUpLink.click();
		if (driver.getWindowHandles().size() == 3)
			parentWindow = selUtils.switchToThirdWindowUsingWebElement("iframeSignUpPage.emailTextbox");
		else
			parentWindow = selUtils.switchToNewWindow();
		WebElement phoneNumberDropdown = driver.findElement(locators.get("iframeSignUpPage.phoneNumberDropdown"));
		phoneNumberDropdown.isDisplayed();
		String phnplaceholdertxt = phoneNumberDropdown.getAttribute("placeholder");
		WebElement emailTextbox = driver.findElement(locators.get("iframeSignUpPage.emailTextbox"));
		emailTextbox.isDisplayed();
		iframeEmail = prop.getProperty("iFrameEmailPrefix").replace(temp,
				CreateDateTime.getTimeDateString() + CreateDateTime.getRandomString(6).toLowerCase());
		emailTextbox.sendKeys(iframeEmail);
		logger.info("Signup email is set as: " + iframeEmail);
		TestListeners.extentTest.get().info("Signup email is set as: " + iframeEmail);
		WebElement fNameTextbox = driver.findElement(locators.get("iframeSignUpPage.fNameTextbox"));
		fNameTextbox.sendKeys(CreateDateTime.getUniqueString(prop.getProperty("firstName")));
		WebElement lNameTextbox = driver.findElement(locators.get("iframeSignUpPage.lNameTextbox"));
		lNameTextbox.sendKeys(CreateDateTime.getUniqueString(prop.getProperty("lastName")));
		WebElement passwordTextbox = driver.findElement(locators.get("iframeSignUpPage.passwordTextbox"));
		passwordTextbox.sendKeys(prop.getProperty("iFramePassword"));
		WebElement confPasswordTextbox = driver.findElement(locators.get("iframeSignUpPage.confPasswordTextbox"));
		confPasswordTextbox.sendKeys(prop.getProperty("iFramePassword"));
		WebElement submitButton = driver.findElement(locators.get("iframeSignUpPage.submitButton"));
		submitButton.isDisplayed();
		// driver.findElement(locators.get("iframeSignUpPage.submitButton")).click();
		utils.longWaitInSeconds(1);
		submitButton.sendKeys(Keys.ENTER);
		try {
			WebElement saveProfileButton = driver.findElement(locators.get("iframeSignUpPage.saveProfileButton"));
			saveProfileButton.click();
			Thread.sleep(2000);
			logger.info("Save Profile button is visible and clicked");
			TestListeners.extentTest.get().info("Save Profile button is visible and clicked");
		} catch (Exception e) {
			logger.info("Save Profile button is not visible");
			TestListeners.extentTest.get().info("Save Profile button is not visible");
		}
		logger.info("Clicked in signup submit button");
		TestListeners.extentTest.get().info("Clicked in signup submit button");
		driver.switchTo().window(parentWindow);
		WebElement signOutLink = driver.findElement(locators.get("iframePage.signOutLink"));
		signOutLink.isDisplayed();
		logger.info("Signup via iframe is successful");
		TestListeners.extentTest.get().pass("Signup via iframe is successful");
		return phnplaceholdertxt;
	}

	public String getEditProfilePhonePlaceholder() {
		clickEditprofileButton();
		WebElement editProfilePhone = driver.findElement(locators.get("iframePage.EditProfilePhone"));
		editProfilePhone.isDisplayed();
		String phnplaceholdertxt = editProfilePhone.getAttribute("placeholder");
		return phnplaceholdertxt;
	}

	public String navigateToEmailConfirmationPage(String url) {
		driver.navigate().to(url);
		WebElement singUpLink = driver.findElement(locators.get("iframePage.singUpLink"));
		singUpLink.isDisplayed();
		WebElement loginLink = driver.findElement(locators.get("iframePage.loginLink"));
		loginLink.isDisplayed();
		logger.info("Navigated to iframe landing page");
		WebElement iframelandingpageAlertMsg = driver.findElement(locators.get("iframeSignUpPage.iframelandingpageAlertMsg"));
		iframelandingpageAlertMsg.isDisplayed();
		String emailconfirmtxtfirst = iframelandingpageAlertMsg.getText();
		logger.info("Navigated to iframe landing page for email confirmation");
		TestListeners.extentTest.get().info("Navigated to iframe landing page for email confirmation :" + url);
		logger.info("Message displayed for the first time was : " + emailconfirmtxtfirst);
		return emailconfirmtxtfirst;
	}

	public String navigateToEmailAlreadyConfirmPage(String url) {
		driver.navigate().to(url);
		selUtils.longWait(3000);
		WebElement singUpLink = driver.findElement(locators.get("iframePage.singUpLink"));
		singUpLink.isDisplayed();
		WebElement loginLink = driver.findElement(locators.get("iframePage.loginLink"));
		loginLink.isDisplayed();
		logger.info("Navigated to iframe landing page");
		WebElement iframelandingpageAlertMsg = driver.findElement(locators.get("iframeSignUpPage.iframelandingpageAlertMsg"));
		iframelandingpageAlertMsg.isDisplayed();
		String emailalreadyconfirmtxt = iframelandingpageAlertMsg.getText();
		logger.info("Navigated to iframe landing page with already confirmed link");
		TestListeners.extentTest.get().info("Navigated to iframe landing page with already confirmed link :" + url);
		logger.info("Message displayed for the already confimed email was : " + emailalreadyconfirmtxt);
		return emailalreadyconfirmtxt;
	}

	public String verifyEmailCustomConfirmationTxtWhenUserloggedIn(String url) {

		driver.navigate().to(url);
		WebElement iframelandingpageAlertMsg = driver.findElement(locators.get("iframeSignUpPage.iframelandingpageAlertMsg"));
		String alreadyconfirmtxt2 = iframelandingpageAlertMsg.getText();
		logger.info("Message displayed when user logged in : " + alreadyconfirmtxt2);
		return alreadyconfirmtxt2;
	}

	public String generateEmailPunchhPar() {
		iframeEmail = prop.getProperty("iFrameEmailPrefixPunchhPar").replace(temp,
				CreateDateTime.getTimeDateString() + CreateDateTime.getRandomString(6).toLowerCase());
		logger.info(iframeEmail + " user is created");
		TestListeners.extentTest.get().pass(iframeEmail + " user random email created");
		return iframeEmail;
	}
}
