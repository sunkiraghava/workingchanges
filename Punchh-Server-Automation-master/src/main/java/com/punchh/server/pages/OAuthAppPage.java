package com.punchh.server.pages;

import java.util.List;
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
public class OAuthAppPage {
	static Logger logger = LogManager.getLogger(OAuthAppPage.class);
	private WebDriver driver;

	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	public static String client;
	public static String secret;
	public static String locationKey;

	public OAuthAppPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
	}

	public String getClient() {
		String client = "";
		utils.getLocator("instanceDashboardPage.oAuthAppH2Label").isDisplayed();
		logger.info("Navigated to OAuth Applications page");
		TestListeners.extentTest.get().info("Navigated to OAuth Applications page");
		utils.getLocator("instanceDashboardPage.authAppNameLink").isDisplayed();
		utils.getLocator("instanceDashboardPage.authAppNameLink").click();
		utils.getLocator("oAuthAppPage.clientLabel").isDisplayed();
		utils.getLocator("oAuthAppPage.secretLabel").isDisplayed();
		logger.info("Clicked on Authorize with Punchh");
		// TestListeners.extentTest.get().info("Clicked on Authorize with Punchh");
		utils.getLocator("oAuthAppPage.unmaskClintID").click();
		client = utils.getLocator("oAuthAppPage.clientLabel").getText();
		TestListeners.extentTest.get().info("Client id is : " + client);
		logger.info("Captured client key as: " + client);
		return client;
	}

	public String getSecret() {
		String secret = "";
		utils.getLocator("oAuthAppPage.secretLabel").isDisplayed();
		logger.info("Clicked on Authorize with Punchh");
		utils.getLocator("oAuthAppPage.unmaskSecret").click();
		secret = utils.getLocator("oAuthAppPage.secretLabel").getText();
		TestListeners.extentTest.get().info("Secret id is : " + secret);
		logger.info("Captured secret key as: " + secret);
		return secret;
	}

	public String getLocationKey() {
		utils.getLocator("locationPage.locationLink").isDisplayed();
		utils.getLocator("locationPage.locationLink").click();
		utils.getLocator("locationPage.posTabLink").isDisplayed();
		utils.getLocator("locationPage.posTabLink").click();
		utils.getLocator("locationPage.locationKeyLabel").isDisplayed();
		String locationKey = utils.getLocator("locationPage.locationKeyLabel").getAttribute("value");
		logger.info("Captured location key is : " + locationKey);
		TestListeners.extentTest.get().info("Captured location key is : " + locationKey);
		return locationKey;

	}

	/*
	 * public String getParams(String buisness, String param) { List<String> arrList
	 * = new ArrayList<String>(); arrList = getClientSecret(buisness); if
	 * (param.equals("client")) { System.out.println(arrList.get(0)); return
	 * (arrList.get(0)); } else if (param.equals("secret")) {
	 * System.out.println(arrList.get(1)); return (arrList.get(1)); } return null; }
	 */

	public void goToSsoSignUp() {
		/*
		 * utils.getLocator("instanceDashboardPage.whitelabel").isDisplayed();
		 * selUtils.mouseHoverOverElement(utils.getLocator(
		 * "instanceDashboardPage.whitelabel"));
		 * utils.getLocator("instanceDashboardPage.whitelabel").click();
		 * utils.getLocator("instanceDashboardPage.oAuthAppLabel").isDisplayed();
		 * utils.getLocator("instanceDashboardPage.oAuthAppLabel").click();
		 */

		utils.getLocator("instanceDashboardPage.oAuthAppH2Label").isDisplayed();
		logger.info("Navigated to OAuth Applications page");
		TestListeners.extentTest.get().info("Navigated to OAuth Applications page");
		utils.getLocator("instanceDashboardPage.authAppNameLink").isDisplayed();
		utils.getLocator("instanceDashboardPage.authAppNameLink").click();
		utils.getLocator("instanceDashboardPage.authorizeWithPunchhLink").isDisplayed();
		utils.longwait(1000);
		WebElement authorizeWithPunchhBtn = utils.getLocator("instanceDashboardPage.authorizeWithPunchhLink");
		utils.clickUsingActionsClass(authorizeWithPunchhBtn);
		logger.info("Clicked on Authorize with Punchh");
		TestListeners.extentTest.get().info("Clicked on Authorize with Punchh");
		utils.getLocator("oauthAppPage.createSsoAccountLink").isDisplayed();
		TestListeners.extentTest.get().pass("Navigated to sso signup page ");
	}

	public void enableSSO() {
		String ele = utils.getLocator("oAuthAppPage.checkSSO").getText();
		if (ele.equals("Use for SSO")) {
			utils.getLocator("oAuthAppPage.checkSSO").click();
			driver.switchTo().alert().accept();
			logger.info("Use for SSO is selected");
			TestListeners.extentTest.get().info("Use for SSO is selected");
		} else {
			logger.info("Use for SSO was already selected");
			TestListeners.extentTest.get().info("Use for SSO was already selected");
		}
		utils.waitTillPagePaceDone();
	}

	public void openAppByName(String oauthAppName) {
		driver.findElement(By.linkText(oauthAppName)).isDisplayed();
		driver.findElement(By.linkText(oauthAppName)).click();
		utils.logit("Successfully opened OAuth app: " + oauthAppName);
	}

	public void deleteOauthAppByName(String oauthAppName) {
		String xpath = utils.getLocatorValue("oAuthAppPage.deleteBtnLst").replace("$OauthAppName", oauthAppName);
		driver.findElement(By.xpath(xpath)).click();
		driver.switchTo().alert().accept();
		utils.logit("Successfully deleted app : " + oauthAppName);
	}

	public void clickNewApplication() {
		utils.getLocator("oAuthAppPage.newApplicatonBtn").click();
		utils.logit("Successfully clicked new application button");
	}

	public void enterAppName(String oauthAppName) {
		utils.getLocator("oAuthAppPage.oAuthAppNameTxt").clear();
		utils.getLocator("oAuthAppPage.oAuthAppNameTxt").sendKeys(oauthAppName);
		utils.logit("Successfully entered app name: " + oauthAppName);
	}

	public void enterRedirectUri(String redirectUri) {
		utils.getLocator("oAuthAppPage.redirectUriTxt").clear();
		utils.getLocator("oAuthAppPage.redirectUriTxt").sendKeys(redirectUri);
		utils.logit("Successfully entered redirect URI: " + redirectUri);
	}

	public void selectApplicationType(String applicationType) {
		utils.getLocator("oAuthAppPage.applicationTypeDrpDwn").isDisplayed();
		utils.getLocator("oAuthAppPage.applicationTypeDrpDwn").click();
		List<WebElement> drpdwnLst = utils.getLocatorList("oAuthAppPage.applicationTypeDrpDwnLst");
		utils.selectListDrpDwnValue(drpdwnLst, applicationType);
		utils.logit("Successfully selected application type: " + applicationType);
	}

	public void selectScope(String scope) {
		utils.getLocator("oAuthAppPage.scopeDrpDwn").isDisplayed();
		utils.getLocator("oAuthAppPage.scopeDrpDwn").click();
		List<WebElement> drpdwnLst = utils.getLocatorList("oAuthAppPage.scopeDrpDwnLst");
		utils.selectListDrpDwnValue(drpdwnLst, scope);
		utils.logit("Successfully selected scope: " + scope);
	}

	public void selectBusinessUnit(String scope) {
		utils.getLocator("oAuthAppPage.businessUnitDrpDwn").isDisplayed();
		utils.getLocator("oAuthAppPage.businessUnitDrpDwn").click();
		List<WebElement> drpdwnLst = utils.getLocatorList("oAuthAppPage.businessUnitDrpDwnLst");
		utils.selectListDrpDwnValue(drpdwnLst, scope);
		utils.logit("Successfully selected business unit: " + scope);
	}

	public void enterValidationURI(String validationURI) {
		utils.getLocator("oAuthAppPage.validationURITxt").clear();
		utils.getLocator("oAuthAppPage.validationURITxt").sendKeys(validationURI);
		utils.logit("Successfully entered validation URI: " + validationURI);
	}

	public void enterVerificationKey(String verificationKey) {
		utils.getLocator("oAuthAppPage.verificationKeyTxt").clear();
		utils.getLocator("oAuthAppPage.verificationKeyTxt").sendKeys(verificationKey);
		utils.logit("Successfully entered verification key: " + verificationKey);
	}

	public void enterWhitelistIP(String ip) {
		utils.getLocator("oAuthAppPage.whitelistIPTxt").clear();
		utils.getLocator("oAuthAppPage.whitelistIPTxt").sendKeys(ip);
		utils.getLocator("oAuthAppPage.saveBtn").click();
		utils.logit("Successfully updated whitelist IP: " + ip);
	}

	public void clickSave() {
		utils.getLocator("oAuthAppPage.saveBtn").click();
		utils.logit("Successfully clicked on save button");
	}

	public void verifyDisplayedMessage(String msg) {
        utils.waitTillPagePaceDone();
		String xpath = utils.getLocatorValue("oAuthAppPage.message").replace("$msg", msg);
		boolean flag = driver.findElement(By.xpath(xpath)).isDisplayed();
		Assert.assertTrue(flag);
		utils.logit("Success message verified");
	}

	public void createIdentityOauthApp(String oauthAppName, String redirectUri, String scope, String validationURI,
			String verificationKey, String successMsg) {
		clickNewApplication();
		enterAppName(oauthAppName);
		enterRedirectUri(redirectUri);
		selectScope(scope);
		enterValidationURI(validationURI);
		enterVerificationKey(verificationKey);
		clickSave();
		verifyDisplayedMessage(successMsg);
		utils.logit("Identity OauthApp created successfully");
	}

	public void createAdvanceAuthOauthApp(String oauthAppName, String redirectUri, String scope, String businessUnit, String successMsg) {
		clickNewApplication();
		enterAppName(oauthAppName);
		enterRedirectUri(redirectUri);
		selectScope(scope);
		selectBusinessUnit(businessUnit);
		clickSave();
		verifyDisplayedMessage(successMsg);
		utils.logit("Advance Auth OauthApp created successfully");
	}

	public void createOauthApp(String oauthAppName, String redirectUri, String scope, String successMsg) {
		clickNewApplication();
		enterAppName(oauthAppName);
		enterRedirectUri(redirectUri);
		selectScope(scope);
		clickSave();
		verifyDisplayedMessage(successMsg);
		utils.logit("OauthApp created successfully with name " + oauthAppName);
	}

    public String getCurrentClient() {
        String client = "";
        String xpath = utils.getLocatorValue("oAuthAppPage.unmaskClintID");
        driver.findElement(By.xpath(xpath)).isDisplayed();
        driver.findElement(By.xpath(xpath)).click();
        client = utils.getLocator("oAuthAppPage.clientLabel").getText();
        return client;
    }

    public String getCurrentSecret() {
        String secret = "";
        String xpath = utils.getLocatorValue("oAuthAppPage.unmaskSecret");
        driver.findElement(By.xpath(xpath)).isDisplayed();
        driver.findElement(By.xpath(xpath)).click();
        secret = utils.getLocator("oAuthAppPage.secretLabel").getText();
        return secret;
    }
    
    public void goToSsoSignUp(String oauthAppName) {
		utils.getLocator("instanceDashboardPage.oAuthAppH2Label").isDisplayed();
		logger.info("Navigated to OAuth Applications page");
		TestListeners.extentTest.get().info("Navigated to OAuth Applications page");
		
		WebElement wOAuthAppsElement =  this.driver.findElement(By.linkText(oauthAppName));
		wOAuthAppsElement.click();
		
//		utils.getLocator("instanceDashboardPage.authAppNameLink").isDisplayed();
//		utils.getLocator("instanceDashboardPage.authAppNameLink").click();
		utils.getLocator("instanceDashboardPage.authorizeWithPunchhLink").isDisplayed();
		utils.longwait(1000);
		WebElement authorizeWithPunchhBtn = utils.getLocator("instanceDashboardPage.authorizeWithPunchhLink");
		utils.clickUsingActionsClass(authorizeWithPunchhBtn);
		logger.info("Clicked on Authorize with Punchh");
		TestListeners.extentTest.get().info("Clicked on Authorize with Punchh");
		utils.getLocator("oauthAppPage.createSsoAccountLink").isDisplayed();
		TestListeners.extentTest.get().pass("Navigated to sso signup page ");
	}
}
