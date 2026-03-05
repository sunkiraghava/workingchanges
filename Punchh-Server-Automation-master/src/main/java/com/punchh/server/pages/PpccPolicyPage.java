package com.punchh.server.pages;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Listeners;

import com.github.javafaker.Faker;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class PpccPolicyPage {
	static Logger logger = LogManager.getLogger(PpccPolicyPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	Faker faker;

	public PpccPolicyPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		faker = new Faker();
		selUtils = new SeleniumUtilities(driver);
	}

	public void clickOnCockpitMenu() {
		utils.getLocator("ppccPolicyPage.dashboardMenuLink").click();
		logger.info("Dashboard Menu is clicked");
		utils.getLocator("ppccPolicyPage.cockpitMenuLink").click();
		logger.info("Cockpit Menu is clicked");
	}
	public void clickOnSettingsTab() {
		utils.getLocator("ppccPolicyPage.settingsMenuLink").click();
		logger.info("Settings is clicked");
	}
	public void clickOnUpdateButton() {
		utils.getLocator("ppccPolicyPage.updateButton").click();
		logger.info("Update Button is clicked");
		TestListeners.extentTest.get().pass("Settings Are saved in POS Integration Tab");
	}

	public void navigateToPosIntegrationTab() {
		utils.waitTillElementToBeClickable(utils.getLocator("ppccPolicyPage.posIntegrationLink"));
		utils.getLocator("ppccPolicyPage.posIntegrationLink").click();
		logger.info("POS Integration Tab is clicked");
	}

	public void navigateToAdminUsersTab() {
		utils.waitTillElementToBeClickable(utils.getLocator("ppccPolicyPage.adminUsersLink"));
		utils.getLocator("ppccPolicyPage.adminUsersLink").click();
		logger.info("User navigated to Admin Tab");
	}

	public void navigateToPermissionsTab() {
		utils.waitTillElementToBeClickable(utils.getLocator("ppccPolicyPage.permissionTab"));
		utils.getLocator("ppccPolicyPage.permissionTab").click();
		logger.info("Permission Tab is selected");
	}

	public void navigateToPosControlCenterTab() {
		utils.waitTillElementToBeClickable(utils.getLocator("ppccPolicyPage.posControlCenterLink"));
		utils.getLocator("ppccPolicyPage.posControlCenterLink").click();
		logger.info("User Navigated to POS Control Center Tab");
		TestListeners.extentTest.get().pass("User Navigated to POS Control Center Tab");
	}
	public String verifyPosControlCenterTab() {
		WebElement headerText = utils.getLocator("ppccPolicyPage.posControlCenterHeading");
		String verifyheaderText = headerText.getText();
		logger.info("User navigated to POS control center tab");
		TestListeners.extentTest.get().pass("Verified POS control center tab");
		return verifyheaderText;
	}

	public void enableHasPosIntegrationCheckBox() {
		WebElement hasPosIntegrationCheckbox = utils.getLocator("ppccPolicyPage.hasPosIntegrationCheckBox");
		String js = "arguments[0].style.visibility='visible';";
		((JavascriptExecutor) driver).executeScript(js, hasPosIntegrationCheckbox);
		String val = hasPosIntegrationCheckbox.getAttribute("checked");
		if (val == null) {
			utils.clickByJSExecutor(driver, hasPosIntegrationCheckbox);
			logger.info("Has POS integration is set to true");
			TestListeners.extentTest.get().info("POS Integration checkbox is set");
		} else {
			logger.info("Has POS integration is already selected : " + val);
			TestListeners.extentTest.get().info("POS Integration checkbox is already selected");
		}
	}

	public void checkEnablePosControlCenterCheckBoxWithPremium() {
		WebElement enablePosControlCenterCheckbox = utils.getLocator("ppccPolicyPage.enablePosControlCenterCheckBox");
		String js = "arguments[0].style.visibility='visible';";
		((JavascriptExecutor) driver).executeScript(js, enablePosControlCenterCheckbox);
		String val = enablePosControlCenterCheckbox.getAttribute("checked");
		if (val == null) {
			utils.clickByJSExecutor(driver, enablePosControlCenterCheckbox);
			logger.info("Pos Control Center is set to true");
			TestListeners.extentTest.get().info("Enable POS Control Center is selected");
		} else {
			logger.info("Enable Pos Control Center is already selected : " + val);
			TestListeners.extentTest.get().info("Enable POS Control Center is already selected");
		}
		WebElement premiumSubscription= utils.getLocator("ppccPolicyPage.premiumCheckBox");
		String checkValue=premiumSubscription.getAttribute("checked");
		if (checkValue == null) {
		premiumSubscription.click();
			logger.info("Premium is selected");
			TestListeners.extentTest.get().pass("Premium is set");
	}
		else {
			logger.info("Premium is already selected : " + checkValue);
			TestListeners.extentTest.get().info("Premium is already set");
		}
	}
	public void navigateToPolicyManagementTab() {
		utils.waitTillPagePaceDone();
		utils.waitTillElementToBeClickable(utils.getLocator("ppccPolicyPage.policyManagementTab"));
		utils.getLocator("ppccPolicyPage.policyManagementTab").click();
		logger.info("User navigated to Policy Management tab");
		TestListeners.extentTest.get().pass("User navigated to Policy Management tab");
		utils.waitTillPagePaceDone();
	}

	public void clickOnCreatePolicyButton() {
		utils.waitTillPagePaceDone();
		utils.waitTillElementToBeClickable(utils.getLocator("ppccPolicyPage.createPolicyButton"));
		utils.getLocator("ppccPolicyPage.createPolicyButton").click();
		logger.info("Create Policy button is clicked");
		TestListeners.extentTest.get().pass("Create Policy Button is clicked");
		utils.waitTillPagePaceDone();
	}

	public String enterPolicyName() {

		WebElement policyNameField = utils.getLocator("ppccPolicyPage.policyNameField");
		utils.waitTillElementToBeClickable(policyNameField);
		String policyName =  "AUT " + faker.lorem().characters(5);
		policyNameField.sendKeys(policyName);
		logger.info("User entered policy name");
		TestListeners.extentTest.get().pass("Policy is created successfully : "+ policyName);
		utils.waitTillPagePaceDone();
		return policyName;
	}

	public void enterDescription(String description) {

		WebElement descriptionField = utils.getLocator("ppccPolicyPage.descriptionField");
		utils.waitTillElementToBeClickable(descriptionField);
		descriptionField.clear();
		descriptionField.sendKeys(description);
		logger.info("User entered description");
		TestListeners.extentTest.get().pass("Description is added");
		utils.waitTillPagePaceDone();
	}

	public void selectPosType(String posType) {
		WebElement posTypeDropdown = utils.getLocator("ppccPolicyPage.posTypeDropdown");
		utils.waitTillElementToBeClickable(posTypeDropdown);
		posTypeDropdown.click();
		List<WebElement> ele = utils.getLocatorList("ppccPolicyPage.posTypeList");
		utils.waitTillPagePaceDone();
		utils.selecDrpDwnValue(ele, posType);
		logger.info("Pos type Aloha is selected");
		TestListeners.extentTest.get().pass("Pos Type is selected");
	}

	public String defineGeneralSettings(String posType) {
		String policyName = enterPolicyName();
		logger.info("Policy name is"+ policyName);
		enterDescription("Automation Description");
		selectPosType(posType);
		clickNextButton();
		TestListeners.extentTest.get().pass("Define General Settings are entered successfully");
		return policyName;
	}

	public void clickOnSaveAsDraftButton() {
		utils.getLocator("ppccPolicyPage.saveAsDraftButton").click();
		logger.info("Policy is saved as Draft");
		utils.waitTillPagePaceDone();
		TestListeners.extentTest.get().pass("Save As Draft Button is clicked");
	}

	public void clickOnSaveAsDraftButtonOnIframe()
	{
		utils.getLocator("ppccPolicyPage.saveAsDraftButtonOniFrame").click();
		logger.info("Policy is saved as Draft");
		TestListeners.extentTest.get().pass("Save As Draft Button is clicked");
	}

	public void clickNextButton() {
		utils.getLocator("ppccPolicyPage.nextButton").click();
		logger.info("Next Button is clicked");
		TestListeners.extentTest.get().pass("Next Button is clicked");
		utils.waitTillPagePaceDone();
	}

	public void clickPublishButton() {
		utils.getLocator("ppccPolicyPage.publishButton").click();
		utils.waitTillPagePaceDone();
		WebElement modal = utils.getLocator("ppccPolicyPage.publishPopAlert");
		modal.click();
		utils.waitTillPagePaceDone();
		WebElement publishButton = modal.findElement(By.xpath("//div[@class='popup-footer']//button[text()='Publish']"));
		publishButton.click();
		utils.waitTillPagePaceDone();
		logger.info("Policy is published");
		TestListeners.extentTest.get().pass("Policy is published successfully");
	}

	public void enterLogLevelDropdown(String logLevel) {
		WebElement logLevelDropdown = utils.getLocator("ppccPolicyPage.logLevelDropdown");
		utils.waitTillElementToBeClickable(logLevelDropdown);
		logLevelDropdown.click();
		utils.waitTillPagePaceDone();
		List<WebElement> ele = utils.getLocatorList("ppccPolicyPage.logLevelList");
		utils.selecDrpDwnValue(ele, logLevel);
		logger.info("Log Level is selected");
		TestListeners.extentTest.get().pass("Value of Log Level is entered successfully");
	}

	public void enterPosConfigurationIntervalUpdate(int interval) {
		WebElement posconfigInterval = utils.getLocator("ppccPolicyPage.posConfigurationUpdateIntervalField");
		posconfigInterval.click();
		posconfigInterval.clear();
		posconfigInterval.sendKeys(Integer.toString(interval));
		logger.info("posConfigurationUpdateIntervalField value is selected");
		TestListeners.extentTest.get().pass("Value of posConfigurationUpdateIntervalField is entered successfully");
	}

	public void enterLanguageDropdown(String language) {
		WebElement languageDropdown = utils.getLocator("ppccPolicyPage.languageDropdown");
		utils.waitTillElementToBeClickable(languageDropdown);
		languageDropdown.click();
		utils.waitTillPagePaceDone();
		List<WebElement> ele = utils.getLocatorList("ppccPolicyPage.languageList");
		utils.selecDrpDwnValue(ele, language);
		logger.info("Language is selected");
		TestListeners.extentTest.get().pass("Value of Language is entered successfully");
	}

	public void enterRegexFilter(String regex) {
		WebElement regexFilterField = utils.getLocator("ppccPolicyPage.regexFilterField");
		regexFilterField.click();
		utils.waitTillPagePaceDone();
		regexFilterField.sendKeys(regex, Keys.ENTER);
		logger.info("Regex Filter is selected");
		TestListeners.extentTest.get().pass("Value of Regex Filter is entered successfully");
	}

	public void enterKeepSocketOpenCheckbox() {
		utils.getLocator("ppccPolicyPage.keepSocketCheckbox").click();
		utils.waitTillPagePaceDone();
		logger.info("keepSocketCheckbox is selected");
		TestListeners.extentTest.get().pass("Value of Keep Socket Checkbox is entered successfully");
	}

	public void enterXmlModeCheckbox() {
		utils.getLocator("ppccPolicyPage.xmlModeCheckbox").click();
		utils.waitTillPagePaceDone();
		logger.info("xmlModeCheckbox is selected");
		TestListeners.extentTest.get().pass("Value of XML Mode Checkbox is entered successfully");
	}

	public void enterCommonConfigurations(String logLevel, String Language) {
		enterLogLevelDropdown(logLevel);
		enterPosConfigurationIntervalUpdate(188);
		enterLanguageDropdown(Language);
		enterRegexFilter("11");
		enterKeepSocketOpenCheckbox();
		enterXmlModeCheckbox();
		logger.info("Common Configurations are entered");
		TestListeners.extentTest.get().pass("Common Configuration of Policy are entered successfully");
		//clickOnSaveAsDraftButton();
		clickNextButton();
	}

	public void enterPort(int portValue) {
		WebElement portField = utils.getLocator("ppccPolicyPage.portField");
		portField.click();
		utils.waitTillPagePaceDone();
		portField.clear();
		portField.sendKeys(Integer.toString(portValue));
		logger.info("portField value is selected");
		TestListeners.extentTest.get().pass("Value of Port Field is entered successfully.");
	}

	public void enterCompId(int comId){
		WebElement compIdField= utils.getLocator("ppccPolicyPage.compIdField");
		compIdField.click();
		utils.waitTillPagePaceDone();
		compIdField.clear();
		compIdField.sendKeys(Integer.toString(comId));
		logger.info("compIdField value is selected");
		utils.waitTillPagePaceDone();
		TestListeners.extentTest.get().pass("Value of Comp Id is entered successfully.");
	}

	public void enterSqlQueryInterval(int queryInterval){
		WebElement compIdField= utils.getLocator("ppccPolicyPage.sqlQueryInterval");
		compIdField.click();
		utils.waitTillPagePaceDone();
		compIdField.clear();
		compIdField.sendKeys(Integer.toString(queryInterval));
		logger.info("Sql Query interval value is selected");
		utils.waitTillPagePaceDone();
		TestListeners.extentTest.get().pass("Value of SQl query interval is entered successfully.");
	}

	public void enterPunchhItemId(int punchhItemId){
		WebElement punchhItemIdField= utils.getLocator("ppccPolicyPage.punchhItemIdField");
		punchhItemIdField.click();
		utils.waitTillPagePaceDone();
		punchhItemIdField.clear();
		punchhItemIdField.sendKeys(Integer.toString(punchhItemId));
		logger.info("punchhItemIdField value is selected");
		TestListeners.extentTest.get().pass("Value of Punchh Item Id is entered successfully.");
	}

	public void enterRedeemItemId(int redeemId){
		WebElement redeemItemIdField= utils.getLocator("ppccPolicyPage.redeemItemIdField");
		redeemItemIdField.click();
		utils.waitTillPagePaceDone();
		redeemItemIdField.clear();
		redeemItemIdField.sendKeys(Integer.toString(redeemId));
		logger.info("redeemItemIdField value is selected");
		TestListeners.extentTest.get().pass("Value of Redeem Item Field is entered successfully.");
	}

	public void enterVoidReason(int voidReason){
		WebElement voidReasonField= utils.getLocator("ppccPolicyPage.voidReasonField");
		voidReasonField.click();
		utils.waitTillPagePaceDone();
		voidReasonField.clear();
		voidReasonField.sendKeys(Integer.toString(voidReason));
		logger.info("voidReasonField value is selected");
		TestListeners.extentTest.get().pass("Value of Void reason is entered successfully.");
	}
	public void enterPaypalTenderId(int paypalTenderId){
		WebElement paypalTenderIdField= utils.getLocator("ppccPolicyPage.paypalTenderIdField");
		paypalTenderIdField.click();
		utils.waitTillPagePaceDone();
		paypalTenderIdField.clear();
		paypalTenderIdField.sendKeys(Integer.toString(paypalTenderId));
		logger.info("paypalTenderIdField value is selected");
		TestListeners.extentTest.get().pass("Value of PayPal Tender ID is entered successfully.");
	}

	public void enterPaypalItemId(int paypalItemId){
		WebElement paypalitemidField= utils.getLocator("ppccPolicyPage.payPalItemIdField");
		paypalitemidField.click();
		utils.waitTillPagePaceDone();
		paypalitemidField.clear();
		paypalitemidField.sendKeys(Integer.toString(paypalItemId));
		logger.info("payPalItemIdField value is selected");
		TestListeners.extentTest.get().pass("Value of Paypal Item id is entered successfully.");
	}

	public void enterVenmoTenderId(int venmoTenderId){
		WebElement venmotenderidField= utils.getLocator("ppccPolicyPage.venmoTenderIdField");
		venmotenderidField.click();
		utils.waitTillPagePaceDone();
		venmotenderidField.clear();
		venmotenderidField.sendKeys(Integer.toString(venmoTenderId));
		logger.info("venmotenderidField value is selected");
		TestListeners.extentTest.get().pass("Value of Venmo Tender Id is entered successfully.");
	}

	public void enterVenmoItemId(int venmoItemId){
		WebElement venmoitemidField= utils.getLocator("ppccPolicyPage.venmoItemIdField");
		venmoitemidField.click();
		utils.waitTillPagePaceDone();
		venmoitemidField.clear();
		venmoitemidField.sendKeys(Integer.toString(venmoItemId));
		logger.info("venmoItemIdField value is selected");
		TestListeners.extentTest.get().pass("Value of Venmo Item Id is entered successfully.");
	}
	public void enterPaymentTenderId(int paymentTenderId){
		WebElement paymentTenderIdField= utils.getLocator("ppccPolicyPage.paymentTenderIdField");
		paymentTenderIdField.click();
		utils.waitTillPagePaceDone();
		paymentTenderIdField.clear();
		paymentTenderIdField.sendKeys(Integer.toString(paymentTenderId));
		logger.info("paymentTenderIdField value is selected");
		TestListeners.extentTest.get().pass("Value of Payment Tender Id is entered successfully.");
	}

	public void enterThirdPartyItemId(int thirdPartyItemId){
		WebElement thirdpartyItemIdField= utils.getLocator("ppccPolicyPage.thirdPartyItemIdField");
		thirdpartyItemIdField.click();
		utils.waitTillPagePaceDone();
		thirdpartyItemIdField.clear();
		thirdpartyItemIdField.sendKeys(Integer.toString(thirdPartyItemId));
		logger.info("thirdpartyItemIdField value is selected");
		TestListeners.extentTest.get().pass("Value of Third Party Item ID is entered successfully.");
	}

	public void enterPaymentItemId(int paymentItemId){
		WebElement paymentItemIdField= utils.getLocator("ppccPolicyPage.paymentItemIdField");
		paymentItemIdField.click();
		utils.waitTillPagePaceDone();
		paymentItemIdField.clear();
		paymentItemIdField.sendKeys(Integer.toString(paymentItemId));
		logger.info("paymentItemIdField value is selected");
		TestListeners.extentTest.get().pass("Value of Payment Item Id is entered successfully.");
	}

	public void enterPointsItemCategory(String pointsItemCategory) {
		WebElement pointsItemCategoryField = utils.getLocator("ppccPolicyPage.pointsItemCategoryField");
		pointsItemCategoryField.click();
		utils.waitTillPagePaceDone();
		pointsItemCategoryField.clear();
		utils.waitTillPagePaceDone();
		pointsItemCategoryField.sendKeys(pointsItemCategory,Keys.ENTER);
		logger.info("pointsItemCategoryField value is entered");
		TestListeners.extentTest.get().pass("Value of Points Item Category is entered successfully.");
	}

	public void enterEarnMessage(String earnMessage) {
		WebElement earnMessageField = utils.getLocator("ppccPolicyPage.earnMessageField");
		utils.waitTillElementToBeClickable(earnMessageField);
		earnMessageField.clear();
		earnMessageField.sendKeys(earnMessage);
		logger.info("Earn message value is entered");
		TestListeners.extentTest.get().pass("Value of Earn Message is entered successfully.");
		utils.waitTillPagePaceDone();
	}
	public void enterRedeemMessage(String redeemMessage) {
		WebElement redeemMessageField = utils.getLocator("ppccPolicyPage.redeemMessageField");
		utils.waitTillElementToBeClickable(redeemMessageField);
		redeemMessageField.clear();
		redeemMessageField.sendKeys(redeemMessage);
		logger.info("Redeem message value is entered");
		TestListeners.extentTest.get().pass("Value of Redeem Message is entered successfully.");
		utils.waitTillPagePaceDone();
	}

	public void enterOrderItemsCheckbox() {
		utils.getLocator("ppccPolicyPage.orderItemsCheckBox").click();
		utils.waitTillPagePaceDone();
		logger.info("orderItems CheckBox is clicked");
		TestListeners.extentTest.get().pass("Value of Order Items Checkbox is entered successfully.");
	}

	public void enterScanAnyTimeCheckbox() {
		utils.getLocator("ppccPolicyPage.scanAnyTimeCheckBox").click();
		utils.waitTillPagePaceDone();
		logger.info("scanAnyTime CheckBox is clicked");
		TestListeners.extentTest.get().pass("Value of Scan Any Time is entered successfully.");
	}

	public void enterUseBarcodeScanInterfaceCheckbox() {
		utils.getLocator("ppccPolicyPage.useBarcodeScanInterfaceCheckBox").click();
		utils.waitTillPagePaceDone();
		logger.info("useBarcodeScanInterface CheckBox is clicked");
		TestListeners.extentTest.get().pass("Value of UserBarcode Scan Interface is entered successfully.");
	}

	public void enterMsrCheckbox() {
		utils.getLocator("ppccPolicyPage.useMSRCheckBox").click();
		utils.waitTillPagePaceDone();
		logger.info("useMSR CheckBox is clicked");
		TestListeners.extentTest.get().pass("Value of MSR checkbox is entered successfully.");
	}

	public void enterEnableBarcodePrintingCheckBox() {
		utils.getLocator("ppccPolicyPage.enableBarcodePrintingCheckBox").click();
		utils.waitTillPagePaceDone();
		logger.info("enableBarcodePrinting CheckBox is clicked");
		TestListeners.extentTest.get().pass("Value of Enable Barcode Printing is entered successfully.");
	}

	public void enterBarcodeOnRedeemCheckBox() {
		utils.getLocator("ppccPolicyPage.barcodeOnRedeemCheckBox").click();
		utils.waitTillPagePaceDone();
		logger.info("barcodeOnRedeem CheckBox is clicked");
		TestListeners.extentTest.get().pass("Value of Barcode Redeem Checkbox is entered successfully.");
	}

	public void enterPrintQRCCheckBox() {
		utils.getLocator("ppccPolicyPage.printQRCCheckBox").click();
		utils.waitTillPagePaceDone();
		logger.info("printQRCCheckBox is clicked");
		TestListeners.extentTest.get().pass("Value of PrintQRC Checkbox is entered successfully.");
	}

	public void enterDisableByOrderMode(String disableByOrderMode) {
		utils.waitTillPagePaceDone();
		utils.getLocator("ppccPolicyPage.disableByOrderModeField").click();
		utils.waitTillPagePaceDone();
		utils.getLocator("ppccPolicyPage.disableByOrderModeField").clear();
		utils.getLocator("ppccPolicyPage.disableByOrderModeField").sendKeys(disableByOrderMode,Keys.ENTER);
		logger.info("disableByOrderModeField value is entered");
		TestListeners.extentTest.get().pass("Value of Disable By Order Mode is entered successfully.");
	}

	public void enterDisableByRevenueCenter(String disableByRevenueCenter) {
		utils.waitTillPagePaceDone();
		utils.getLocator("ppccPolicyPage.disableByRevenueCenterField").click();
		utils.waitTillPagePaceDone();
		utils.getLocator("ppccPolicyPage.disableByRevenueCenterField").clear();
		utils.getLocator("ppccPolicyPage.disableByRevenueCenterField").sendKeys(disableByRevenueCenter,Keys.ENTER);
		logger.info("disableByRevenueCenterField value is entered");
		TestListeners.extentTest.get().pass("Value of Disable By Revenue Center is entered successfully.");
	}

	public void enterBarcodeOnReprintCheckBox() {
		utils.getLocator("ppccPolicyPage.barcodeOnReprintCheckBox").click();
		utils.waitTillPagePaceDone();
		logger.info("barcodeOnReprint CheckBox is clicked");
		TestListeners.extentTest.get().pass("Value of Barcode on Reprint is entered successfully.");
	}

	public void enterBarcodeOnlyOnClosedCheckCheckBox() {
		utils.getLocator("ppccPolicyPage.barcodeOnlyOnClosedCheckCheckBox").click();
		utils.waitTillPagePaceDone();
		logger.info("barcodeOnlyOnClosedCheck CheckBox is clicked");
		TestListeners.extentTest.get().pass("Value of Barcode Only On Closed Check is entered successfully.");
	}
	public void enterFilterItemCategoryField(String filterItemCategory) {
		utils.waitTillPagePaceDone();
		utils.getLocator("ppccPolicyPage.filterItemCategoryField").click();
		utils.waitTillPagePaceDone();
		utils.getLocator("ppccPolicyPage.filterItemCategoryField").clear();
		utils.getLocator("ppccPolicyPage.filterItemCategoryField").sendKeys(filterItemCategory,Keys.ENTER);
		logger.info("filterItemCategory Field value is entered");
		TestListeners.extentTest.get().pass("Value of Filter Item Category is entered successfully.");
	}

	public void enterPointsOnlyCustomerCheckBox() {
		utils.getLocator("ppccPolicyPage.pointsOnlyCustomerCheckBox").click();
		utils.waitTillPagePaceDone();
		logger.info("pointsOnlyCustomerCheckBox is clicked");
		TestListeners.extentTest.get().pass("Value of Points Only Customer checkbox is entered successfully.");
	}
	public void enterAllowSingleSignOnCheckBox() {
		utils.getLocator("ppccPolicyPage.allowSingleSignOnCheckBox").click();
		utils.waitTillPagePaceDone();
		logger.info("allowSingleSignOn CheckBox is clicked");
		TestListeners.extentTest.get().pass("Value of Allow Single SignOn is entered successfully.");
	}
	public void enterSsfAutoApplyPaymentCheckBox() {
		utils.getLocator("ppccPolicyPage.ssfAutoApplyPaymentCheckBox").click();
		utils.waitTillPagePaceDone();
		logger.info("ssfAutoApplyPaymentCheckBox is clicked");
		TestListeners.extentTest.get().pass("Value of ssf Auto Apply Payment checkbox is entered successfully.");
	}
	public void enterAllowAlphaKeyboardPopupCheckBox() {
		utils.getLocator("ppccPolicyPage.allowAlphaKeyboardPopupCheckBox").click();
		utils.waitTillPagePaceDone();
		logger.info("allowAlphaKeyboardPopup CheckBox is clicked");
		TestListeners.extentTest.get().pass("Value of Allow Alpha Keyboard checkbox is entered successfully.");
	}

	public void enterAllowBeBackCheckBox() {
		utils.getLocator("ppccPolicyPage.allowBeBackCheckBox").click();
		utils.waitTillPagePaceDone();
		logger.info("allowBeBackCheckBox is clicked");
		TestListeners.extentTest.get().pass("Value of Allow Be Back checkbox is entered successfully.");
	}

	public void enterAutoCreateCustomerCheckBox() {
		utils.getLocator("ppccPolicyPage.autoCreateCustomerCheckBox").click();
		utils.waitTillPagePaceDone();
		logger.info("autoCreateCustomer CheckBox is clicked");
		TestListeners.extentTest.get().pass("Value of Auto Create Customer checkbox is entered successfully.");
	}

	public void enterAutoCheckinCheckBox() {
		utils.getLocator("ppccPolicyPage.autoCheckinCheckBox").click();
		utils.waitTillPagePaceDone();
		logger.info("autoCheckinCheckBox is clicked");
		TestListeners.extentTest.get().pass("Value of Auto Checkin checkbox is entered successfully.");
	}
	public void enterAutoCloseOnRedeemCheckBox() {
		utils.getLocator("ppccPolicyPage.autoCloseOnRedeemCheckBox").click();
		utils.waitTillPagePaceDone();
		logger.info("autoCloseOnRedeem CheckBox is clicked");
		TestListeners.extentTest.get().pass("Value of Auto Close On Redeem checkbox is entered successfully.");
	}

	public void enterCouponPrefix(String couponPrefix){
		WebElement couponPrefixField = utils.getLocator("ppccPolicyPage.couponPrefixField");
		utils.waitTillElementToBeClickable(couponPrefixField);
		couponPrefixField.clear();
		couponPrefixField.sendKeys(couponPrefix);
		logger.info("Coupon Message value is entered");
		TestListeners.extentTest.get().pass("Value of Coupon Message is entered successfully.");
		utils.waitTillPagePaceDone();
	}

	public void enterMenuItemPrefix(){
		WebElement menuItemPrefixField = utils.getLocator("ppccPolicyPage.menuItemPrefixField");
		utils.waitTillElementToBeClickable(menuItemPrefixField);
		String menuItemPrefix = "Menu Item Prefix";
		menuItemPrefixField.sendKeys(menuItemPrefix);
		logger.info("menu Item Prefix value is entered");
		TestListeners.extentTest.get().pass("Value of Menu Item Prefix is entered successfully.");
		utils.waitTillPagePaceDone();
	}
	public void enterbarcodeOnCheckinCheckBox() {
		utils.getLocator("ppccPolicyPage.barcodeOnCheckinCheckBox").click();
		utils.waitTillPagePaceDone();
		logger.info("barcodeOnCheckin CheckBox is clicked");
		TestListeners.extentTest.get().pass("Value of Barcode On Checkin is entered successfully.");
	}

	public void enterbarcodeOnZeroCheckCheckBox() {
		utils.getLocator("ppccPolicyPage.barcodeOnZeroCheckCheckBox").click();
		utils.waitTillPagePaceDone();
		logger.info("barcodeOnZeroCheck CheckBox is clicked");
		TestListeners.extentTest.get().pass("Value of Barcode on Zero Check is entered successfully.");
	}

	public void enterEnableloyaltyChitCheckBox() {
		utils.getLocator("ppccPolicyPage.enableloyaltyChitCheckBox").click();
		utils.waitTillPagePaceDone();
		logger.info("enableloyaltyChit CheckBox is clicked");
		TestListeners.extentTest.get().pass("Value of Enable Loyalty Chit checkbox is entered successfully.");
	}

	public void enterHasGiftCardIntegrationCheckBox() {
		utils.getLocator("ppccPolicyPage.hasGiftCardIntegrationCheckBox").click();
		utils.waitTillPagePaceDone();
		logger.info("hasGiftCardIntegration CheckBox is clicked");
		TestListeners.extentTest.get().pass("Value of Has Gift Card Integration checkbox is entered successfully.");
	}

	public void enterPOSConfigForRpos()
	{
		enterPort(8888);
		enterSqlQueryInterval(1111);
	}

	public void enterPosConfigurations() {
		enterPort(8888);
		enterCompId(188);
		enterPunchhItemId(188);
		enterRedeemItemId(188);
		enterVoidReason(188);
		enterPaypalTenderId(188);
		enterPaypalItemId(188);
		enterVenmoTenderId(188);
		enterVenmoItemId(188);
		enterPaymentTenderId(188);
		enterThirdPartyItemId(188);
		enterPaymentItemId(188);
		enterPointsItemCategory("11");
		enterEarnMessage("Earn Message");
		enterRedeemMessage("Redeem Message");
		enterOrderItemsCheckbox();
		enterScanAnyTimeCheckbox();
		enterUseBarcodeScanInterfaceCheckbox();
		enterMsrCheckbox();
		enterEnableBarcodePrintingCheckBox();
		enterBarcodeOnRedeemCheckBox();
		enterPrintQRCCheckBox();
		enterDisableByOrderMode("11");
		enterDisableByRevenueCenter("11");
		enterBarcodeOnReprintCheckBox();
		enterBarcodeOnlyOnClosedCheckCheckBox();
		enterPointsOnlyCustomerCheckBox();
		enterFilterItemCategoryField("11");
		enterAllowSingleSignOnCheckBox();
		enterSsfAutoApplyPaymentCheckBox();
		enterAllowAlphaKeyboardPopupCheckBox();
		enterAllowBeBackCheckBox();
		enterAutoCreateCustomerCheckBox();
		enterAutoCheckinCheckBox();
		enterAutoCloseOnRedeemCheckBox();
		enterCouponPrefix("Coupon Prefix");
		enterbarcodeOnCheckinCheckBox();
		enterbarcodeOnZeroCheckCheckBox();
		enterEnableloyaltyChitCheckBox();
	}

	public String searchPolicy(String policyName) {
		utils.waitTillPagePaceDone();
		WebElement searchField = utils.getLocator("ppccPolicyPage.searchField");
		searchField.click();
		searchField.clear();
		searchField.sendKeys(policyName);
		utils.waitTillPagePaceDone();
		logger.info("Policy is searched");
		utils.waitTillPagePaceDone();
		WebElement firstRowPolicyName= utils.getLocator("ppccPolicyPage.firstPolicyName");
		String matchedPolicyName=firstRowPolicyName.getText();
		utils.waitTillPagePaceDone();
		TestListeners.extentTest.get().pass("Policy is successfully searched");
		return matchedPolicyName;
	}

	public void clickViewActionOfPolicy(){
		WebElement actionIcon=utils.getLocator("ppccPolicyPage.actionIconPolicy");
		utils.waitTillElementToBeClickable(actionIcon);
		actionIcon.click();
		logger.info("Action icon is clicked");
		TestListeners.extentTest.get().pass("Action icon is clicked");
		utils.waitTillPagePaceDone();
		utils.waitTillElementToBeClickable(utils.getLocator("ppccPolicyPage.viewAction"));
		utils.getLocator("ppccPolicyPage.viewAction").click();
		logger.info("View Action is clicked");
		TestListeners.extentTest.get().pass("View Action is clicked");
	}

	public String verifyPolicyDetailsHeaderOnViewPolicyAction() {
		WebElement headerText = utils.getLocator("ppccPolicyPage.policyViewHeader");
		String verifyheaderText = headerText.getText();
		logger.info("User navigated to Policy Details after View Action.");
		TestListeners.extentTest.get().pass("User navigated to Policy Details after View Action.");
		return verifyheaderText;
	}

	public String verifyViewOnlyHeaderOnViewPolicyAction() {
		WebElement headerText = utils.getLocator("ppccPolicyPage.policyViewOnlyHeader");
		String verifyheaderText = headerText.getText();
		logger.info("User navigated to Policy Details after View Action.");
		TestListeners.extentTest.get().pass("User navigated to Policy Details after View Action.");
		utils.waitTillPagePaceDone();
		return verifyheaderText;
	}
	public void deleteActionOfPolicy(){
		WebElement actionIcon=utils.getLocator("ppccPolicyPage.actionIconPolicy");
		utils.waitTillElementToBeClickable(actionIcon);
		actionIcon.click();
		logger.info("Action icon is clicked");
		TestListeners.extentTest.get().pass("Action icon is clicked");
		utils.waitTillPagePaceDone();
		utils.waitTillElementToBeClickable(utils.getLocator("ppccPolicyPage.deleteAction"));
		utils.getLocator("ppccPolicyPage.deleteAction").click();
		logger.info("Delete Action is clicked");
		TestListeners.extentTest.get().pass("Delete Action is clicked");
		utils.waitTillPagePaceDone();
		WebElement typeConfirm= utils.getLocator("ppccPolicyPage.confirmInputDeletePolicy");
		typeConfirm.sendKeys("Confirm");
		logger.info("Confirm is Typed in Delete Pop up to delete policy.");
		TestListeners.extentTest.get().info("Confirm is Typed in Delete Pop up to delete policy.");
		utils.waitTillElementToBeClickable(utils.getLocator("ppccPolicyPage.YesDeleteButtonPolicy"));
		utils.getLocator("ppccPolicyPage.YesDeleteButtonPolicy").click();
		utils.waitTillPagePaceDone();
		logger.info("Delete Action is completed");
		TestListeners.extentTest.get().pass("Delete Action is completed");
	}

	public WebElement getPublishButton()
	{
		return utils.getLocator("ppccPolicyPage.publishButton");
	}

	public void editPolicy()
	{
		utils.waitTillPagePaceDone();
		utils.getLocator("ppccPolicyPage.firstPolicyName").click();
		logger.info("Policy is clicked");
		utils.waitTillPagePaceDone();
		utils.getLocator("ppccPolicyPage.editPolicy").click();
		logger.info("Edit Policy is clicked");
		utils.waitTillPagePaceDone();
		enterLanguageDropdown("ro-RO");
		enterPort(9090);
		enterRedeemMessage("Edit Redeem Message");
		logger.info("Description is entered");
	}

	public String verifyDeleteAction()
	{
		WebElement noRecordFound= utils.getLocator("ppccPolicyPage.noRecordFoundHeaderPolicy");
		String messagePolicyTable= noRecordFound.getText();
		logger.info("Policy is deleted successfully");
		TestListeners.extentTest.get().pass("Policy is deleted successfully");
		return messagePolicyTable;
	}

	public String duplicateActionOfPolicy() {
		WebElement actionIcon = utils.getLocator("ppccPolicyPage.actionIconPolicy");
		utils.waitTillElementToBeClickable(actionIcon);
		actionIcon.click();
		logger.info("Action icon is clicked");
		TestListeners.extentTest.get().pass("Action icon is clicked");
		utils.waitTillPagePaceDone();
		utils.waitTillElementToBeClickable(utils.getLocator("ppccPolicyPage.duplicateAction"));
		utils.getLocator("ppccPolicyPage.duplicateAction").click();
		logger.info("Duplicate Action is clicked");
		TestListeners.extentTest.get().pass("Duplicate Action is clicked");
		utils.waitTillPagePaceDone();
		WebElement duplicatePolicyName = utils.getLocator("ppccPolicyPage.duplicatePopUpNameField");
		String duplicatedPolicyName = "AUT " + faker.lorem().characters(5);
		duplicatePolicyName.clear();
		duplicatePolicyName.sendKeys(duplicatedPolicyName);
		return duplicatedPolicyName;
	}

	public void publishPolicy()
	{
		utils.getLocator("ppccPolicyPage.publishButton").click();
		utils.waitTillPagePaceDone();
		logger.info("Policy is duplicated Successfully");
		TestListeners.extentTest.get().pass("Duplicate Action is completed");
	}

	public void clickOnBackButtonOnViewPolicy(){

		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
		utils.longWaitInSeconds(2);
		utils.getLocator("ppccPolicyPage.backButtonViewPolicy").click();
		utils.waitTillPagePaceDone();
		logger.info("Back Button is clicked from view details of policy");
		TestListeners.extentTest.get().pass("Back Button is clicked from view details of policy");
		utils.waitTillPagePaceDone();
	}

	public void navigateToAuditLogs()
	{
		utils.waitTillPagePaceDone();
		utils.getLocator("ppccPolicyPage.navigateToAuditLog").click();
		logger.info("User navigated to Audit Logs Tab");
		TestListeners.extentTest.get().pass("User navigated to Audit Logs Tab");
		driver.navigate().refresh();
		utils.waitTillPagePaceDone();
	}

	public boolean isDataFiltered(String filterOption, String filterValue, int columnIndex, String expectedValue)
    {
        applyFilter(filterOption, filterValue);
        boolean isDataFiltered = isFilteredResultValid(columnIndex, expectedValue);
        return isDataFiltered;
    }

	private void applyFilter(String filterOption, String filterValue)
    {
        utils.getLocator("ppccPolicyPage.filterButton").click();
        utils.getLocator("ppccPolicyPage.filterOption").click();
        driver.findElement(By.xpath(filterOption)).click();
        utils.getLocator("ppccPolicyPage.selectFilterOption").click();
        driver.findElement(By.xpath(filterValue)).click();
        utils.getLocator("ppccPolicyPage.applyButton").click();
		utils.waitTillPagePaceDone();
        logger.info("Filter applied successfully");
    }

	private boolean isFilteredResultValid(int columnIndex, String expectedValue)
    {
        String items = utils.getLocatorValue("ppccPolicyPage.itemEntry");
        List<WebElement> rows = driver.findElements(By.xpath(items));

        if (rows.isEmpty()) { return false;}
        String columnData = utils.getLocatorValue("ppccPolicyPage.columnData").replace("{columnIndex}", String.valueOf(columnIndex));
        for (WebElement row : rows)
        {
            WebElement column = row.findElement(By.xpath(columnData));
            String value = column.getText();
            logger.info("Value is: " + value);
            if (!value.contains(expectedValue))
            {
                logger.info("Found a row where data is not '" + expectedValue + "': " + value);
                return false;
            }
        }
        return true;
    }

	public String getPolicyIdOfCreatedPolicy(){
		String policyId= utils.getLocator("ppccPolicyPage.policyIDFirstRow").getText();
		logger.info("Policy ID is : "+ policyId);
		return policyId;
	}
	public boolean isFieldLabelMarkedAsRequired(String fieldLabelText) {
		utils.waitTillPagePaceDone();
		String xpath = String.format("//label[.//span[text()='%s']]", fieldLabelText);
		logger.info("Checking if the field label is marked as required using XPath: " + xpath);
		WebElement labelElement = driver.findElement(By.xpath(xpath));
		String className = labelElement.getAttribute("class");
		return className.contains("required");
	}
	public boolean isPublishButtonEnabled() {
		WebElement publishButton =  utils.getLocator("ppccPolicyPage.publishButton");
		logger.info("presence of publish button is checked");
		return publishButton.isEnabled();
	}
	public boolean isFieldVisible(String fieldName) {
		WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(1));
		try {
			WebElement element = shortWait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//span[contains(text(),'" + fieldName + "')]")));
			logger.info(fieldName + "is being checked");
			return element.isDisplayed();
		} catch (TimeoutException | NoSuchElementException e) {
			return false;
		}
	}

}