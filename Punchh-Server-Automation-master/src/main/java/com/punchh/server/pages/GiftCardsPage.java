package com.punchh.server.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class GiftCardsPage {

	static Logger logger = LogManager.getLogger(GiftCardsPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	PageObj pageObj;

	public GiftCardsPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		pageObj = new PageObj(driver);
	}

	public String getGiftCardPurchaseDetails() {
		String val = utils.getLocator("giftCardsPage.cardBalanceRow1").getText();
		logger.info("Card balance row text is :" + val);
		return val;

	}

	public String getGiftCardReloadDetails() {
		String val = utils.getLocator("giftCardsPage.cardBalanceRow2").getText();
		logger.info("Card balance row text is :" + val);
		return val;

	}

	public String getCardNumber() {
		String val = utils.getLocator("giftCardsPage.cardNumber").getText();
		logger.info("Card number is :" + val);
		return val;
	}

	public String getCardBalance() {
		String val = utils.getLocator("giftCardsPage.cardBalance").getText();
		logger.info("Card balance is :" + val);
		return val;
	}

	public void searchbyCard(String cardNumber) {
		utils.getLocator("giftCardsPage.searchBox").clear();
		utils.getLocator("giftCardsPage.searchBox").sendKeys(cardNumber);
		utils.getLocator("giftCardsPage.searchBox").sendKeys(Keys.ENTER);
	}

	public List<String> getCardDetailsGuestSection() {
		List<String> accountData = new ArrayList<String>();
		List<WebElement> tableRowOne = utils.getLocatorList("giftCardsPage.tableRow1");
		int col = tableRowOne.size();
		for (int i = 0; i < col; i++) {
			String val = tableRowOne.get(i).getText();
			accountData.add(val);
		}
		return accountData;

	}

	public List<String> getCardOwnerDetails() {
		List<String> ownerData = new ArrayList<String>();
		List<WebElement> tableRowOne = utils.getLocatorList("giftCardsPage.ownerRow");
		int col = tableRowOne.size();
		for (int i = 0; i < col; i++) {
			String val = tableRowOne.get(i).getText();
			ownerData.add(val);
		}
		return ownerData;

	}

	public List<String> getCardSharedDetails() {
		List<String> sharedData = new ArrayList<String>();
		List<WebElement> tableRowOne = utils.getLocatorList("giftCardsPage.sharedRow");
		int col = tableRowOne.size();
		for (int i = 0; i < col; i++) {
			String val = tableRowOne.get(i).getText();
			sharedData.add(val);
		}
		return sharedData;
	}

	public void braintreeCredentials(String braintree_public_key) {
		utils.clickByJSExecutor(driver, utils.getLocator("giftCardsPage.braintreeServices"));
//		utils.getLocator("giftCardsPage.braintreeServices").click();
		utils.getLocator("giftCardsPage.braintreePublicKey").clear();
		utils.getLocator("giftCardsPage.braintreePublicKey").sendKeys(braintree_public_key);
//		utils.getLocator("giftCardsPage.braintreePublicKey").sendKeys(Keys.ENTER);
		selUtils.implicitWait(5);
		utils.clickByJSExecutor(driver, utils.getLocator("giftCardsPage.updateBraintree"));
//		utils.getLocator("giftCardsPage.updateBraintree").click();
		logger.info("Updated brain tree credentials");
		TestListeners.extentTest.get().pass("Updated brain tree credentials");
		selUtils.implicitWait(60);
	}

	public boolean checkGiftCardLogs(String firstName, String amount) throws InterruptedException {
		boolean flag = false;
		int attempts = 0;
		while (attempts < 10) {
			Utilities.longWait(2000);
			try {
				driver.navigate().refresh();
				utils.waitTillPagePaceDone();
				String var = utils.getLocator("giftCardsPage.legacyPaymentGiftCardLogs").getText();
				Assert.assertTrue(var.contains(firstName));
				Assert.assertTrue(var.contains(amount));
				flag = true;
				logger.info("Gift card holder name matched");
				TestListeners.extentTest.get().pass("Gift card holder name matched");
				logger.info("Gift card amount  matched");
				TestListeners.extentTest.get().pass("Gift card amount matched");
				break;
			} catch (Exception e) {
				logger.info("Element is not present " + e.getMessage());
				TestListeners.extentTest.get().fail("Element is not present " + e.getMessage());
			}
			utils.longWaitInSeconds(2);
			attempts++;
		}
		return flag;
	}

	public void checkGiftCardGCLogs(String Purchase) throws InterruptedException {
		int attempts = 0;
		while (attempts <= 10) {
			try {
				String var = utils.getLocator("giftCardsPage.legacyPaymentGiftCardLogs").getText();
				if ((var.contains(Purchase))) {
					logger.info("Gift card logs matched");
					break;
				}
				Assert.assertTrue(var.contains(Purchase));
				logger.info("Gift card amount  matched");
				TestListeners.extentTest.get().pass("Gift card amount matched");
				Assert.assertTrue(var.contains("Invalid Credentials"));
				logger.info("Gift card Invalid Credentials matched");
			} catch (Exception e) {
				logger.error("Gift card logs did not matched " + e);
			} catch (AssertionError ae) {
				logger.info("Assertion true error, attempt no = " + attempts + " error message " + ae.getMessage());
				TestListeners.extentTest.get()
						.info("Assertion true error, attempt no = " + attempts + " error message " + ae.getMessage());
			}
			attempts++;
		}
		TestListeners.extentTest.get().pass("Gift card Invalid Credentials matched");
	}

	public void checkGiftCardGCAdapterLogs(String amount) throws InterruptedException {
		utils.longWaitInSeconds(4);
		String var = utils.getLocator("giftCardsPage.legacyPaymentGiftCardLogs").getText();
		Assert.assertTrue(var.contains(amount));
		logger.info("Gift card amount matched");
		TestListeners.extentTest.get().pass("Gift card amount matched");
		Assert.assertTrue(var.contains("Error"));
		logger.info("Gift card Invalid Credentials matched");
		TestListeners.extentTest.get().pass("Gift card Invalid Credentials matched");
	}

	public void checkIntegrationGiftCardLogs(String amount) throws InterruptedException {
		utils.longWaitInSeconds(10);
		List<WebElement> list = utils.getLocatorList("giftCardsPage.integrationPaymentGiftCardLogs");
		String var = list.get(1).getText();
		Assert.assertTrue(var.contains(amount));
		logger.info("Gift card amount  matched");
		TestListeners.extentTest.get().pass("Gift card amount matched");
	}

	public void braintreeV2Credentials(String braintree_public_key) throws InterruptedException {
		utils.waitTillPagePaceDone();
		utils.StaleElementclick(driver, utils.getLocator("giftCardsPage.clickBraintreeV2"));
		Thread.sleep(2000);
		utils.getLocator("giftCardsPage.braintreeV2PublicKey").clear();
		utils.getLocator("giftCardsPage.braintreeV2PublicKey").sendKeys(braintree_public_key);
//		utils.getLocator("giftCardsPage.braintreeV2PublicKey").sendKeys(Keys.ENTER);
		utils.StaleElementclick(driver, utils.getLocator("giftCardsPage.updateBraintree"));
//		utils.getLocator("giftCardsPage.updateBraintree").click();
		logger.info("Updated braintree V2 Credentials");
		TestListeners.extentTest.get().info("Updated braintree V2 Credentials");
	}

	public void selectPaymentAdapter(String adapter) throws InterruptedException {
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Payments");
		utils.waitTillPagePaceDone();
		utils.getLocator("giftCardsPage.paymentAdapterDrp").click();
		List<WebElement> ele = utils.getLocatorList("giftCardsPage.paymentAdapterList");
		utils.selectListDrpDwnValue(ele, adapter);
		utils.clickByJSExecutor(driver, utils.getLocator("dashboardPage.updateBtn"));
		utils.waitTillPagePaceDone();
		logger.info("Payment Adapter is Selected as: " + adapter);
		TestListeners.extentTest.get().info("Payment Adapter is Selected as: " + adapter);

	}

	public void selectGiftCardAdapter(String adapter) {
		utils.getLocator("giftCardsPage.giftCardAdapterDrp").click();
		utils.getLocator("giftCardsPage.giftCardAdapterSearch").clear();
		utils.getLocator("giftCardsPage.giftCardAdapterSearch").sendKeys(adapter);
		utils.getLocator("giftCardsPage.searchedAdapter").click();
		// List<WebElement> ele =
		// utils.getLocatorList("giftCardsPage.giftCardAdapterList");
		// selUtils.implicitWait(2);
		// utils.selecDrpDwnValue(ele, adapter);
		logger.info("Gift Card Adapter is Selected as: " + adapter);
		TestListeners.extentTest.get().pass("Gift Card Adapter is Selected as: " + adapter);
	}

	public void mercuryCredentials(String mercuryMerchantId) {
		utils.clickByJSExecutor(driver, utils.getLocator("giftCardsPage.mercuryServices"));
		selUtils.implicitWait(5);
//		utils.getLocator("giftCardsPage.mercuryServices").click();
		utils.getLocator("giftCardsPage.mercuryMerchantId").clear();
		utils.getLocator("giftCardsPage.mercuryMerchantId").sendKeys(mercuryMerchantId);
//		utils.getLocator("giftCardsPage.mercuryMerchantId").sendKeys(Keys.ENTER);
		selUtils.implicitWait(5);
		utils.clickByJSExecutor(driver, utils.getLocator("giftCardsPage.updateMercury"));
//		utils.getLocator("giftCardsPage.updateMercury").click();
		logger.info("Updated Mercury Gift Card Services");
		TestListeners.extentTest.get().pass("Updated Mercury Gift Card Services");
		selUtils.implicitWait(50);
	}

	public void valueLinkCredentials(String valuLinkCustomerReferenceNumber) throws InterruptedException {
		utils.StaleElementclick(driver, utils.getLocator("giftCardsPage.clickValueLink"));
//		utils.getLocator("giftCardsPage.clickValueLink").click();
		Thread.sleep(5000);
		utils.getLocator("giftCardsPage.valuLinkCustomerReferenceNumber").click();
		utils.getLocator("giftCardsPage.valuLinkCustomerReferenceNumber").clear();
		selUtils.implicitWait(5);
		utils.getLocator("giftCardsPage.valuLinkCustomerReferenceNumber").sendKeys(valuLinkCustomerReferenceNumber);
//	utils.getLocator("giftCardsPage.valuLinkCustomerReferenceNumber").sendKeys(Keys.ENTER);
		selUtils.implicitWait(5);
		utils.StaleElementclick(driver, utils.getLocator("giftCardsPage.updateValuelink"));
//	utils.getLocator("giftCardsPage.updateValuelink").click();
		logger.info("Updated ValueLink Credentials");
		TestListeners.extentTest.get().pass("Updated ValueLink Credentials");
		selUtils.implicitWait(50);
	}

	public void valueLinkCredentialsIntegration(String valuLinkCustomerReferenceNumber) throws InterruptedException {
		utils.waitTillPagePaceDone();
		utils.StaleElementclick(driver, utils.getLocator("giftCardsPage.clickValueLinkk"));
		Thread.sleep(5000);
//		utils.getLocator("giftCardsPage.clickValueLink").click();
		utils.getLocator("giftCardsPage.valuLinkCustomerReferenceNumber").click();
		// utils.getLocator("giftCardsPage.refershBtn").click();
		// utils.waitTillPagePaceDone();
		// utils.StaleElementclick(driver,
		// utils.getLocator("giftCardsPage.clickValueLinkk"));
		utils.waitTillVisibilityOfElement(utils.getLocator("giftCardsPage.valuLinkCustomerReferenceNumber"), "");
		utils.getLocator("giftCardsPage.valuLinkCustomerReferenceNumber").clear();
		utils.getLocator("giftCardsPage.valuLinkCustomerReferenceNumber").sendKeys(valuLinkCustomerReferenceNumber);
		selUtils.implicitWait(5);
		utils.StaleElementclick(driver, utils.getLocator("giftCardsPage.updateValuelink"));
		logger.info("Updated ValueLink Credentials");
		TestListeners.extentTest.get().info("Updated ValueLink Credentials");
		selUtils.implicitWait(60);
	}

	public boolean checkIntegrationGCLogs(String amount) {
		boolean flag = false;
		int attempts = 0;
		while (attempts < 7) {
			pageObj.instanceDashboardPage().refreshPage();
			Utilities.longWait(2000);
			try {
				List<WebElement> list = utils.getLocatorList("giftCardsPage.integrationPaymentGiftCardLogs");
				for (WebElement wele : list) {
					String var = wele.getText();
					if (var.contains(amount)) {
						flag = true;
						logger.info("Gift card amount  matched");
						TestListeners.extentTest.get().pass("Gift card amount  matched");
						break;
					}
				}
			} catch (Exception e) {
				logger.info("Element is not present " + e.getMessage());
				TestListeners.extentTest.get().fail("Element is not present " + e.getMessage());
			}
			attempts++;

		}
		return flag;
	}

	public boolean checkIntegrationGCAdapterLogs(String text) {
		boolean flag = false;
		int attempts = 0;
		while (attempts < 7) {
			pageObj.instanceDashboardPage().refreshPage();
			Utilities.longWait(2000);
			try {
				List<WebElement> list = utils.getLocatorList("giftCardsPage.integrationPaymentGiftCardLogs");
				for (WebElement wele : list) {
					String var = wele.getText();
					if (var.contains(text)) {
						flag = true;
						logger.info("Gift card logs matched");
						TestListeners.extentTest.get().pass("Gift card logs matched");
						break;
					}
				}
			} catch (Exception e) {
				logger.info("Gift card logs are not visible ");
				TestListeners.extentTest.get().fail("Gift card logs are not visible ");
			}
			attempts++;
		}
		return flag;
	}

	public boolean checkGiftCardGCLog(String Purchase) {

		boolean flag = false;
		List<WebElement> list = utils.getLocatorList("giftCardsPage.legacyGiftCardLogs");
		for (WebElement wele : list) {
			String var = wele.getText();
			if (var.contains(Purchase)) {
				flag = true;
				logger.info("Gift card amount  matched");
				break;
			}
		}
		return flag;
	}

	public void setMinMaxAmountGiftCard(String minAmount, String maxAmount) {
		// Set transaction amount limit
		utils.getLocator("giftCardsPage.minimumTransactionAmount").clear();
		utils.getLocator("giftCardsPage.minimumTransactionAmount").sendKeys(minAmount);
		logger.info("Minimum Transaction Amount Entered as :" + minAmount);
		utils.getLocator("giftCardsPage.maximumTransactionAmount").clear();
		utils.getLocator("giftCardsPage.maximumTransactionAmount").sendKeys(maxAmount);
		logger.info("Maximum Transaction Amount Entered as :" + minAmount);
		utils.clickByJSExecutor(driver, utils.getLocator("dashboardPage.updateBtn"));
		utils.waitTillPagePaceDone();
		TestListeners.extentTest.get().pass("Minimum Maximum Transaction Amount is set successfully");
	}

	public void clickOnUpdateButton() {
		utils.clickByJSExecutor(driver, utils.getLocator("dashboardPage.updateBtn"));
	}

	public void worldGiftCardCredentialsIntegration(String worldGiftCardLoginUserNames) throws InterruptedException {
		utils.waitTillPagePaceDone();
		utils.StaleElementclick(driver, utils.getLocator("giftCardsPage.clickWorlGiftCard"));
		Thread.sleep(5000);
//		utils.getLocator("giftCardsPage.clickValueLink").click();
		utils.getLocator("giftCardsPage.worldGiftCardLoginUserName").click();
		// utils.getLocator("giftCardsPage.refershBtn").click();
		// utils.waitTillPagePaceDone();
		// utils.StaleElementclick(driver,
		// utils.getLocator("giftCardsPage.clickValueLinkk"));
		utils.waitTillVisibilityOfElement(utils.getLocator("giftCardsPage.worldGiftCardLoginUserName"), "");
		utils.getLocator("giftCardsPage.worldGiftCardLoginUserName").clear();
		utils.getLocator("giftCardsPage.worldGiftCardLoginUserName").sendKeys(worldGiftCardLoginUserNames);
		selUtils.implicitWait(5);
		utils.StaleElementclick(driver, utils.getLocator("giftCardsPage.updateWorldGiftCard"));
		logger.info("Updated World Gift Card Adaptor");
		TestListeners.extentTest.get().info("Updated World Gift Card Adaptor");
		selUtils.implicitWait(50);
	}
	//Added by  Ajeet
	public void selectGiftCardAdaptor(String cardAdaptorName) {
	    utils.getLocator("giftCardsPage.giftCardsAdaptorDropDown").click();
	    List<WebElement> eles = utils.getLocatorList("giftCardsPage.giftCardAdapters");
	    for (WebElement ele : eles) {
	        if (ele.getText().equalsIgnoreCase(cardAdaptorName)) {
	            ele.click();
	            break;
	        }
	    }
	}
	 public String errorMessageForInvailidAmountOnGiftCards(String ErrorMessage) {
		String xpath= utils.getLocatorValue("giftCardsPage.errorHandlingMessageOnGiftCards");
		xpath=xpath.replace("${xyz}",ErrorMessage);
		WebElement message=driver.findElement(By.xpath(xpath));
		String str="";
		logger.info("String found  =>" +str);
		return str= message.getText();
		 }
	 
	 public void enableWorldPayCaptchaFlag(boolean enabledCaptcha) {
		utils.clickByJSExecutor(driver, utils.getLocator("giftCardsPage.worldPayServices"));
		logger.info("Clicked on World Pay Services");
		utils.scrollToElement(driver, utils.getLocator("giftCardsPage.captchaCheckBox"));
		logger.info("Scrolled to captcha checkbox");
		utils.setCheckboxStateViaCheckBoxText("Enable Captcha", enabledCaptcha);
		logger.info("Updated the Enable Captcha field to : " + enabledCaptcha);
		utils.clickByJSExecutor(driver, utils.getLocator("giftCardsPage.updateWorldPay"));
		logger.info("Updated World Pay Credentials");
		TestListeners.extentTest.get().pass("Updated World Pay Payment Credentials");
	}
	 
	 public void setGiftcardSeriesLength(String gcSeries, String gcLength) {
		utils.getLocator("giftCardsPage.giftCardSeries").clear();
		utils.getLocator("giftCardsPage.giftCardSeries").sendKeys(gcSeries);
		utils.getLocator("giftCardsPage.giftCardLength").clear();
		utils.getLocator("giftCardsPage.giftCardLength").sendKeys(gcLength);
		logger.info("Gift Card Series is set as: " + gcSeries + " and length is set as: " + gcLength);
		TestListeners.extentTest.get()
					.pass("Gift Card Series is set as: " + gcSeries + " and length is set as: " + gcLength);
	}
		
	public void enableGiftcardAutoreloadConfig(boolean autoReloadEnabled, String autoReloadThreshold, String autoReloadDefault) {
		enableGiftCardAutoReloadFlag(autoReloadEnabled);
		utils.getLocator("giftCardsPage.autoreloadThresholdAmt").clear();
		utils.getLocator("giftCardsPage.autoreloadThresholdAmt").sendKeys(autoReloadThreshold);
		utils.getLocator("giftCardsPage.autoreloadDefaultAmt").clear();
		utils.getLocator("giftCardsPage.autoreloadDefaultAmt").sendKeys(autoReloadDefault);
		logger.info("Autoreload business config set with Threshold Amount : " + autoReloadThreshold + " and Default Amount : " + autoReloadDefault);
		TestListeners.extentTest.get().pass("GC Autoreload business config set with Threshold Amount : " + autoReloadThreshold + " and Default Amount : " + autoReloadDefault);
	}
		
	public void enableGiftCardAutoReloadFlag(boolean autoReloadEnabled) {
		utils.setCheckboxStateViaCheckBoxText(utils.getLocatorValue("giftCardsPage.autoreloadFlag"), autoReloadEnabled);
		logger.info("Enable Autoreload flag is set with : " + autoReloadEnabled);
		TestListeners.extentTest.get().pass("Enable Autoreload flag is set with : " + autoReloadEnabled);
	}
		
	public void enableDigitalStoredValueFlag(boolean digitalStoredValueEnabled) {
		utils.setCheckboxStateViaCheckBoxText(utils.getLocatorValue("giftCardsPage.digitalStoredValueFlag"), digitalStoredValueEnabled);
		logger.info("Enable Digital Stored Value flag is set with : " + digitalStoredValueEnabled);
		TestListeners.extentTest.get().pass("Enable Digital Stored Value flag is set with : " + digitalStoredValueEnabled);
			
	}
	
	public void parPaymentCredential(boolean vaultEnabled, String url) {
		utils.clickByJSExecutor(driver, utils.getLocator("giftCardsPage.clickParPaymentService"));
		utils.getLocator("giftCardsPage.parPayBaseURL").clear();
		utils.getLocator("giftCardsPage.parPayBaseURL").sendKeys(url);
		utils.setCheckboxStateViaCheckBoxText(utils.getLocatorValue("giftCardsPage.parPayEnableRecurPaymentFlag"), vaultEnabled);
		selUtils.implicitWait(5);
		utils.clickByJSExecutor(driver, utils.getLocator("giftCardsPage.updateParPayment"));
		logger.info("Updated PAR Payment credentials");
		TestListeners.extentTest.get().pass("Updated PAR Payment credentials");
		selUtils.implicitWait(60);
	}
	
	public void enableZeroBalWalletGCFlag(boolean zeroBalWalletGCFlag) {
		utils.setCheckboxStateViaCheckBoxText(utils.getLocatorValue("giftCardsPage.zeroBalWalletGCFlag"), zeroBalWalletGCFlag);
		utils.clickByJSExecutor(driver, utils.getLocator("dashboardPage.updateBtn"));
		logger.info("Set Zero Balance Wallet GC with: " + zeroBalWalletGCFlag);
		TestListeners.extentTest.get().pass("Set Zero Balance Wallet GC with: " + zeroBalWalletGCFlag);
	}
	
	public void enableLocationBasedPayment(String adapter, boolean locBasedPaymentEnabled, boolean allowBizPaymentConfig) throws InterruptedException {
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Payments");
		utils.waitTillPagePaceDone();
		utils.getLocator("giftCardsPage.paymentAdapterDrp").click();
		List<WebElement> ele = utils.getLocatorList("giftCardsPage.paymentAdapterList");
		utils.selectListDrpDwnValue(ele, adapter);
		utils.setCheckboxStateViaCheckBoxText(utils.getLocatorValue("giftCardsPage.locationBasedPayment"), locBasedPaymentEnabled);
		if(locBasedPaymentEnabled) {
			utils.setCheckboxStateViaCheckBoxText(utils.getLocatorValue("giftCardsPage.allowBizPaymentConfig"), allowBizPaymentConfig);
		}
		utils.clickByJSExecutor(driver, utils.getLocator("dashboardPage.updateBtn"));
		utils.waitTillPagePaceDone();
		logger.info("Payment Adapter is Selected as: " + adapter);
		TestListeners.extentTest.get().info("Payment Adapter is Selected as: " + adapter);

	}
	
	/**
	 * Validates gift card event from database
	 * @param env Environment name
	 * @param giftCardId Gift card ID to validate
	 * @param expectedEvent Expected event value
	 * @return The event value from database
	 * @throws Exception if database query fails
	 */
	public String validateGiftCardEvent(String env, String giftCardId, String expectedEvent) throws Exception {
		String query = "SELECT event FROM gift_card_versions WHERE gift_card_id='" + giftCardId + "' ORDER BY id ASC LIMIT 1;";
		String event = DBUtils.executeQueryAndGetColumnValue(env, query, "event");
		logger.info("Gift card event retrieved from DB: " + event);
		Assert.assertEquals(event, expectedEvent, "Gift Card Event is not '" + expectedEvent + "'");
		TestListeners.extentTest.get().pass("Gift card event validation successful: " + event);
		return event;
	}
	
}
