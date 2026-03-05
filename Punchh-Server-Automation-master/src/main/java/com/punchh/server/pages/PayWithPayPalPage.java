package com.punchh.server.pages;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class PayWithPayPalPage {
	static Logger logger = LogManager.getLogger(MenuPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;

	public PayWithPayPalPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
	}

	public void loginToPayPalPage(String userName, String password) {

		utils.getLocator("PayWithPayPalPage.PayPalPageLoginUser").sendKeys(userName);
		WebElement nextButtonWEle = utils.getLocator("PayWithPayPalPage.PayPalPageLoinNextButton");
		utils.waitTillElementToBeClickable(nextButtonWEle);
		nextButtonWEle.click();
		utils.getLocator("PayWithPayPalPage.PaypalPageLoginPassword").sendKeys(password);
		utils.getLocator("PayWithPayPalPage.PaypalPageSubmitButton").click();

	}

	public void clickOnSaveAndContinueButton() throws InterruptedException {
		utils.getLocator("PayWithPayPalPage.consentButton").click();
		Thread.sleep(5000);
	}

	public void verifySlugNameIsVisible(String slugName) {
		WebElement wEle = utils.getLocator("PayWithPayPalPage.slugHeader");
		utils.waitTillElementToBeClickable(wEle);
		String actualSlugName = wEle.getText().trim();
		Assert.assertEquals(actualSlugName, slugName, slugName + " name is not displaying on paypal page.");

	}

	// payment token through heartland 2.0
	public String generatePaymentTokenForHeartland2() throws InterruptedException {

		WebElement mainIframe = utils.getLocator("PayWithPayPalPage.heartLandMainIFrame");
		// main Iframe
		selUtils.switchToFrame(mainIframe);

		// Card Number input Iframe
		driver.switchTo().frame(utils.getLocator("PayWithPayPalPage.heartLandCarNumberIframe"));
		WebElement cardnumberInputWele = utils.getLocator("PayWithPayPalPage.heartLandCardNumberInputBox");
		utils.waitTillElementToBeClickable(cardnumberInputWele);
		cardnumberInputWele.clear();
		cardnumberInputWele.sendKeys("4111-1111-1111-1111");

		driver.switchTo().defaultContent();
		selUtils.switchToFrame(mainIframe);
		driver.switchTo().frame(utils.getLocator("PayWithPayPalPage.heartLandExpDateIframe"));

		String expDate = "12/2025";

		WebElement cardExpirationDate = utils.getLocator("PayWithPayPalPage.heartLandExpDateInputBox");
		cardExpirationDate.clear();
		cardExpirationDate.sendKeys(expDate);

		driver.switchTo().defaultContent();
		selUtils.switchToFrame(mainIframe);

		driver.switchTo().frame(utils.getLocator("PayWithPayPalPage.heartLandCVVIframe"));

		WebElement cardCVVWele = utils.getLocator("PayWithPayPalPage.heartLandCVVInputBox");
		cardCVVWele.clear();
		cardCVVWele.sendKeys("123");

		driver.switchTo().defaultContent();
		selUtils.switchToFrame(mainIframe);

		driver.switchTo().frame(utils.getLocator("PayWithPayPalPage.heartLandSubmitIframe"));

		utils.getLocator("PayWithPayPalPage.heartLandSubmitButton").click();

		Thread.sleep(2000);

		String alertText = selUtils.checkAlert();
		System.out.println("alertText== " + alertText);

		String[] singleScanToken = alertText.split(":");
		String finalesingleScanToken = singleScanToken[1].trim();

		logger.info(finalesingleScanToken + " single scan code is generated ");
		TestListeners.extentTest.get().info(finalesingleScanToken + " single scan code is generated ");

		return finalesingleScanToken;

	}

}
