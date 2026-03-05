package com.punchh.server.pages;

import java.util.Map;
import java.util.Properties;
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
public class SsoLoginSignUpPage {
	static Logger logger = LogManager.getLogger(SsoLoginSignUpPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	String ssoEmail, temp = "temp";

	private Map<String, By> locators;

	public SsoLoginSignUpPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(this.driver);
		selUtils = new SeleniumUtilities(driver);

		locators = utils.getAllByMap();
	}

	public String signUpViaSso(String ssoEmail) {
		WebElement createAccountLink = driver.findElement(locators.get("oauthAppPage.createSsoAccountLink"));
		createAccountLink.isDisplayed();
		createAccountLink.click();
		TestListeners.extentTest.get().info("Clicked on create account link");
		logger.info("Clicked on SSO signup button");
		WebElement submitButton = driver.findElement(locators.get("iframeSignUpPage.submitButton"));
		submitButton.isDisplayed();
		TestListeners.extentTest.get().info("Clicked on SSO signup button");
		WebElement emailTextbox = driver.findElement(locators.get("iframeSignUpPage.emailTextbox"));
		emailTextbox.isDisplayed();
		emailTextbox.sendKeys(ssoEmail);
		logger.info("SSO signup email is set as:" + ssoEmail);
		TestListeners.extentTest.get().info("SSO signup email is set as:" + ssoEmail);
		WebElement fNameTextbox = driver.findElement(locators.get("iframeSignUpPage.fNameTextbox"));
		fNameTextbox.isDisplayed();
		fNameTextbox.sendKeys(prop.getProperty("firstName"));
		WebElement lNameTextbox = driver.findElement(locators.get("iframeSignUpPage.lNameTextbox"));
		lNameTextbox.isDisplayed();
		lNameTextbox.sendKeys(prop.getProperty("lastName"));
		WebElement passwordTextbox = driver.findElement(locators.get("iframeSignUpPage.passwordTextbox"));
		passwordTextbox.isDisplayed();
		passwordTextbox.sendKeys(prop.getProperty("iFramePassword"));
		WebElement confPasswordTextbox = driver.findElement(locators.get("iframeSignUpPage.confPasswordTextbox"));
		confPasswordTextbox.isDisplayed();
		confPasswordTextbox.sendKeys(prop.getProperty("iFramePassword"));
		submitButton = driver.findElement(locators.get("iframeSignUpPage.submitButton"));
		submitButton.click();
		TestListeners.extentTest.get().info("SSO user signup is done with email :" + ssoEmail);
		return ssoEmail;
	}

	public void ssoLogin(String email) {
		WebElement emailTextbox = driver.findElement(locators.get("iframeSignUpPage.emailTextbox"));
		emailTextbox.sendKeys(email);
		WebElement passwordTextbox = driver.findElement(locators.get("iframeSignUpPage.passwordTextbox"));
		passwordTextbox.sendKeys(prop.getProperty("iFramePassword"));
		WebElement submitButton = driver.findElement(locators.get("iframeSignUpPage.submitButton"));
		submitButton.click();
	}

	public String forgotPassword(String email) {
		WebElement forgotPasswordLink = driver.findElement(locators.get("iframeSignUpPage.forgotPassword"));
		forgotPasswordLink.click();
		WebElement emailTextbox = driver.findElement(locators.get("iframeSignUpPage.emailTextbox"));
		emailTextbox.isDisplayed();
		emailTextbox.sendKeys(email);
		WebElement sendResetPwdButton = driver.findElement(locators.get("iframeSignUpPage.sendResetPwd"));
		sendResetPwdButton.click();
		WebElement pwdResetMessage = driver.findElement(locators.get("iframeSignUpPage.pwdResetMsg"));
		String msg = pwdResetMessage.getText().trim();
		return msg;
	}
}
