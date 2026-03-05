package com.punchh.server.pages;

import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.Color;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class WhitelabelPage {
	static Logger logger = LogManager.getLogger(SegmentsBetaPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	String ssoEmail, temp = "temp";
	String segmentDescription = "Test segment description";
	public String emailCount, pnCount, smsCount;
	int segmentCount;
	String age = "30";
	private PageObj pageObj;

	public WhitelabelPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(this.driver);
		selUtils = new SeleniumUtilities(driver);
		pageObj = new PageObj(driver);
	}

	public void clickMenuBtn() {
		utils.getLocator("whitelabelPage.MenuBtn").click();
	}

	public String MenuTextHint(String str) {
		String xpath = utils.getLocatorValue("whitelabelPage.menuHintText").replace("$flag", str);
		return driver.findElement(By.xpath(xpath)).getText();
	}

	public boolean isHashedValue(String path) {
		String attrvalue = utils.getLocator(path).getAttribute("value");
		if (attrvalue.contains("****")) {
			logger.info("value is hashed");
			return true;
		}
		logger.info("value is not hashed");
		return false;
	}

	public void editMenuGeocode(String str) {
		utils.getLocator("whitelabelPage.menuGeocodeKeyEditBtn").click();
		utils.getLocator("whitelabelPage.menuGeocodeKey").sendKeys(str);
	}

	public void editMenuclientScret(String str) {
		utils.getLocator("whitelabelPage.menuClientSecretKeyEditBtn").click();
		utils.getLocator("whitelabelPage.menuClientSecretKey").sendKeys(str);
	}

	public void editMenuAPIVersion(String str) {
		utils.getLocator("whitelabelPage.menuAPIVersion").clear();
		utils.getLocator("whitelabelPage.menuAPIVersion").sendKeys(str);
	}

	public void clickMenuUpdate() {
		utils.getLocator("whitelabelPage.updateMenu").click();
	}

	public String getErrorMessage() {
		selUtils.implicitWait(60);
		// utils.waitTillCompletePageLoad();
		utils.waitTillVisibilityOfElement(utils.getLocator("locationPage.getSuccessErrorMessage"), "");
		String messge = "";
		messge = utils.getLocator("locationPage.getSuccessErrorMessage").getText();
		return messge;
	}

	public void clickAPImsgGuestProfile() {
		utils.getLocator("whitelabelPage.APImsgGuestProfile").click();
		// utils.longWait(8000);
	}

	public String passwordDoesnotMatchPresent() {
		String text = utils.getLocator("whitelabelPage.passwordDoesnotMatchField").getText();
		return text;
	}

	public void clearPasswordDoesnotMatch() {
		utils.getLocator("whitelabelPage.passwordDoesnotMatch").clear();
		logger.info("'password Doesnot Match' field is clear");
	}

	public void editPasswordDoesnotMatch(String str) {
		clearPasswordDoesnotMatch();
		utils.getLocator("whitelabelPage.passwordDoesnotMatch").sendKeys(str);

		logger.info("'password Doesnot Match' field is edit");
		// utils.longWait(4500);
	}

	public void saveBtn() {
		List<WebElement> listOfSaveButton = utils.getLocatorList("whitelabelPage.saveBtn");
		for (WebElement wEle : listOfSaveButton) {
			try {
				utils.implicitWait(1);
				utils.scrollToElement(driver, wEle);
				wEle.click();
				break;

			} catch (Exception e) {
				// TODO: handle exception
			}

		}
		utils.implicitWait(50);
	}

	@SuppressWarnings("static-access")
	public void cancelBtn() {
		WebElement ele = utils.getLocator("whitelabelPage.cancelBtn");
		utils.clickByJSExecutor(driver, ele);

		utils.implicitWait(5);
	}

	public String getTextColour(String path) {
		WebElement ele = utils.getLocator(path);
		String rgbFormat = ele.getCssValue("color");
		String hexcode = Color.fromString(rgbFormat).asHex();
		return hexcode;
	}

	public void cannotBeBlankPresent(String str) {
		String xpath = utils.getLocatorValue("whitelabelPage.cannotBeBlank").replace("$flag", str);
		WebElement ele = driver.findElement(By.xpath(xpath));
		Assert.assertTrue(ele.isDisplayed(), "'can't be blank' is not visible");
		logger.info("'can't be blank' is visible");
		TestListeners.extentTest.get().pass("'can't be blank' is visible");

		Assert.assertEquals(xpath, "#ff0000", "'can't be blank' is not of red color");
		logger.info("'can't be blank' is of red color");
		TestListeners.extentTest.get().pass("'can't be blank' is of red color");
	}

	public boolean isKouponMediaDisplayed() {
		try {
			return utils.getLocator("whitelabelPage.kouponMediaBtn").isDisplayed();
		} catch (Exception e) {
			return false;
		}
	}

	public void editAlohaNamespace(String str) {
		utils.getLocator("whitelabelPage.alohaNamespace").clear();
		utils.getLocator("whitelabelPage.alohaNamespace").sendKeys(str);
		logger.info("Aloha namespace field is edit");
	}

	public void editAlohaCompanyID(String str) {
		utils.getLocator("whitelabelPage.alohaCompanyID").clear();
		utils.getLocator("whitelabelPage.alohaCompanyID").sendKeys(str);
		logger.info("aloha comapny id field is edit");
	}

	public void editAlohaUserName(String str) {
		utils.getLocator("whitelabelPage.alohaUserName").click();
		utils.getLocator("whitelabelPage.alohaUserName").sendKeys(str);
		logger.info("aloha User Name field is edit");
	}

	public void clickUpdateAlohaWebServices() {
		utils.clickByJSExecutor(driver, utils.getLocator("whitelabelPage.updateAlohaWebServices"));
		// utils.getLocator("whitelabelPage.updateAlohaWebServices").click();
		logger.info("clicked Update Aloha Web Services btn");
	}

	public void clickKouponMediaBtn() {
		utils.getLocator("whitelabelPage.kouponMediaBtn").click();
		logger.info("clicked on koupon media");
	}

	public String editKouponMediaFeild(String name, String value, String id) {
		String xpath = utils.getLocatorValue("whitelabelPage.kouponMediaField").replace("$flag", id);
		WebElement AgeVerificationAPIErrorMessageBox = driver.findElement(By.xpath(xpath));
		String str = AgeVerificationAPIErrorMessageBox.getAttribute("value");
		selUtils.longWait(1500);
		AgeVerificationAPIErrorMessageBox.clear();
		AgeVerificationAPIErrorMessageBox.sendKeys(value);
		logger.info("edit the field " + name);
		return str;
	}

	public void clickUpdateKouponMedia() {
		utils.getLocator("whitelabelPage.updateKouponMedia").click();
		logger.info("clicked on update Koupon Media");
		selUtils.implicitWait(5);
	}

	public void clickEditBtn(String id) {
		String xpath = utils.getLocatorValue("whitelabelPage.editBtn").replace("$flag", id);
		driver.findElement(By.xpath(xpath)).click();
		logger.info("clicked the edit btn");
		selUtils.longWait(500);
	}

	public String verifyAsterik(String id) {
		String xpath = utils.getLocatorValue("whitelabelPage.asterik").replace("$flag", id);

		String str = driver.findElement(By.xpath(xpath)).getText();
		return str;
	}

	public String isHashed(String id) {
		String xpath = utils.getLocatorValue("whitelabelPage.kouponMediaField").replace("$flag", id);
		String str = driver.findElement(By.xpath(xpath)).getAttribute("value");
		return str;
	}

	public String getSuccessMsg() {
		selUtils.implicitWait(60);
		// utils.waitTillCompletePageLoad();
		utils.waitTillVisibilityOfElement(utils.getLocator("locationPage.getSuccessErrorMessage"), "");
		String messge = "";
		messge = utils.getLocator("locationPage.getSuccessErrorMessage").getText();
		return messge;
	}

	public void clickUpdateParPayment() {
		utils.getLocator("whitelabelPage.updateParPayment").click();
		TestListeners.extentTest.get().pass("clicked on the update Par Payment button");
	}

	public void clickUpdateHeartland() {
		utils.getLocator("whitelabelPage.updateHeartland").click();
		TestListeners.extentTest.get().pass("clicked on the update heartland button");
	}

	public void verifyParOrderingConfigs() {
		Assert.assertFalse(utils.getLocator("whitelabelPage.menuClientSecretKey").getAttribute("value").isEmpty(),
				"Parordering client and secret field must configure");
		Assert.assertFalse(utils.getLocator("whitelabelPage.menuAPIVersion").getAttribute("value").isEmpty(),
				"Parordering API version field must have some value!");
		Assert.assertTrue(utils.getLocator("whitelabelPage.ParorderingSTSFlag").isEnabled(),
				"ParorderingSTSFlag should be enabled!");
		TestListeners.extentTest.get().info("Parordering configurations are present");
		logger.info("Parordering configurations are present");

	}

	public void verifyGooglesigninConfigs() throws InterruptedException {
		utils.getLocator("CockpitGuestPage.miscConfig").click();
		Assert.assertTrue(utils.getLocator("CockpitGuestPage.googleSigninChkBox").isEnabled(),
				"Google signin should be enabled!");
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Services");
		utils.getLocator("whitelabelPage.googleSigninTab").click();
		Assert.assertFalse(utils.getLocator("whitelabelPage.googleWebClientID").getAttribute("value").isEmpty(),
				"Google client and secret field must configure");
		TestListeners.extentTest.get().info("Googlesignin configurations are present");
		logger.info("Googlesignin configurations are present");

	}

}
