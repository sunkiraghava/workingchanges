package com.punchh.server.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class CampaignSplitPage {

	static Logger logger = LogManager.getLogger(CampaignSplitPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	CreateDateTime createDateTime;

	public CampaignSplitPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		createDateTime = new CreateDateTime();
	}

	public void splitPanelVerifyonMassOfferWhomPage() {
		utils.longWaitInSeconds(2);
		WebElement splitbox = utils.getLocator("campaignSplitPage.splittestbox");
		boolean splitboxverification = splitbox.isDisplayed();
		if (splitbox.isDisplayed()) {

			logger.info("Split box is appearing on whom page after selecting segment: " + splitboxverification);
			TestListeners.extentTest.get()
					.info("Split box is appearing on whom page after selecting segment: " + splitboxverification);
		} else {
			logger.info("Split box is not appearing before selecting segment: " + splitboxverification);
			TestListeners.extentTest.get()
					.info("Split box is not appearing before selecting segment: " + splitboxverification);
		}
	}

	public void clickOnSplitButton() {
		utils.longWaitInSeconds(2);
		utils.getLocator("campaignSplitPage.editsplitbutton").click();
		logger.info("Clicked on Split Button");
		utils.longWaitInSeconds(1);
		TestListeners.extentTest.get().pass("Clicked on Split Button");

	}

	public void splitCancelButton() {

		utils.longWaitInSeconds(1);
		utils.getLocator("campaignSplitPage.splitCancelbutton").click();
		logger.info("Clcik on Cancel button");
		TestListeners.extentTest.get().pass("Clicked on Cancel Button");

	}

	public void splitYesCancelButton() {
		utils.longWaitInSeconds(1);
		utils.getLocator("campaignSplitPage.splitYesCancelbutton").click();
		logger.info("Clcik on Yes Cancel button");
		TestListeners.extentTest.get().pass("Clicked on Yes Cancel Button");
	}

	public void variantA_Audience(String varApercentage) {

		WebElement VariantApercentage = utils.getLocator("campaignSplitPage.varApercentage");
		VariantApercentage.clear();
		VariantApercentage.sendKeys(varApercentage);
		logger.info("Added Variant A percentage");
		TestListeners.extentTest.get().pass("Added Variant A percentage");
	}

	public void variantB_Audience(String VarBpercentage) {

		WebElement VariantBpercentage = utils.getLocator("campaignSplitPage.varBpercentage");
		VariantBpercentage.clear();
		VariantBpercentage.sendKeys(VarBpercentage);
		logger.info("Added Variant B percentage");
		TestListeners.extentTest.get().pass("Added Variant B percentage");
	}

	public void controlgroup_Audience(String controlgrouppercentage) {

		WebElement Controlgrouppercentage = utils.getLocator("campaignSplitPage.controlgrouppercentage");
		Controlgrouppercentage.clear();
		Controlgrouppercentage.sendKeys(controlgrouppercentage);
		utils.longWaitInSeconds(1);
		logger.info("Added control group percentage");
		TestListeners.extentTest.get().pass("Added control group percentage");
	}

	public void clickOnSaveButton() {
		utils.longWaitInSeconds(1);
		utils.getLocator("campaignSplitPage.Splitsavebutton").click();
		logger.info(" clicked on save button ");
		TestListeners.extentTest.get().pass(" clicked on save button");
		utils.waitTillPagePaceDone();
	}

	public void clickOnNextButton() {
		utils.longWaitInSeconds(1);
		WebElement nextButton = utils.getLocator("campaignSplitPage.splitNextButton");
		nextButton.click();
		utils.longWaitInMiliSeconds(500);
		logger.info(" clicked on next button ");
		TestListeners.extentTest.get().pass(" clicked on next button");
	}

	public void splitVariantAemail(String emailSubject, String emailpreHeader, String emailhtmlbodytext) {

		utils.getLocator("campaignSplitPage.splitVarAemailcheckbox").click();
		utils.getLocator("campaignSplitPage.splitVarAemailthreedot").click();
		utils.getLocator("campaignSplitPage.splitVarAaddEmail").click();

		utils.getLocator("campaignSplitPage.splitVarAemaiSubject").clear();
		utils.getLocator("campaignSplitPage.splitVarAemaiSubject").sendKeys(emailSubject);

		utils.getLocator("campaignSplitPage.splitVarAEmailPreheader").clear();
		utils.getLocator("campaignSplitPage.splitVarAEmailPreheader").sendKeys(emailpreHeader);

		utils.getLocator("campaignSplitPage.splitVarAEmailHtmlBodyText").clear();
		utils.getLocator("campaignSplitPage.splitVarAEmailHtmlBodyText").sendKeys(emailhtmlbodytext);

		utils.getLocator("campaignSplitPage.Splitsavebutton").click();

		logger.info("Added Variant A email details successfully");
		TestListeners.extentTest.get().pass(" Added Variant A email details successfully");
	}

	public void splitVariantAPushnotification(String pushTitle, String pushSubtitle, String pushBody) {

		utils.getLocator("campaignSplitPage.splitVarAPushNotificationcheckbox").click();
		utils.getLocator("campaignSplitPage.splitVarApushthreedot").click();
		utils.getLocator("campaignSplitPage.splitVarAaddpush").click();

		utils.getLocator("campaignSplitPage.splitVarAPushTitle").clear();
		utils.getLocator("campaignSplitPage.splitVarAPushTitle").sendKeys(pushTitle);

		utils.getLocator("campaignSplitPage.splitVarAPushSubtitle").clear();
		utils.getLocator("campaignSplitPage.splitVarAPushSubtitle").sendKeys(pushSubtitle);

		utils.getLocator("campaignSplitPage.splitVarAPushBody").clear();
		utils.getLocator("campaignSplitPage.splitVarAPushBody").sendKeys(pushBody);

		utils.getLocator("campaignSplitPage.Splitsavebutton").click();

		logger.info("Added Variant A push details successfully");
		TestListeners.extentTest.get().pass(" Added Variant A push details successfully");
	}

	public void setStartTimesplitmasscampaign(String dateTime) {
		utils.longWaitInSeconds(1);
		utils.getLocator("signupCampaignsPage.startTimeTxtBox").clear();
		utils.getLocator("signupCampaignsPage.startTimeTxtBox").sendKeys(dateTime);
		utils.getLocator("signupCampaignsPage.cancelbtn").click();
		Select sel = new Select(driver.findElement(By.id("schedule_timezone")));
		sel.selectByVisibleText(Utilities.getConfigProperty("timezone"));
		utils.logit("Timezone selected ");
	}

	public void splitVariatASms(String smsbody) {

		utils.getLocator("campaignSplitPage.splitVarAsmscheckbox").click();
		utils.getLocator("campaignSplitPage.splitVarAsmsthreedot").click();
		utils.getLocator("campaignSplitPage.splitVarAaddsms").click();

		utils.getLocator("campaignSplitPage.splitVarASmsBody").clear();
		utils.getLocator("campaignSplitPage.splitVarASmsBody").sendKeys(smsbody);

		utils.getLocator("campaignSplitPage.Splitsavebutton").click();

		logger.info("Added Variant A sms details successfully");
		TestListeners.extentTest.get().pass(" Added Variant A sms details successfully");
	}

	public void splitVariantArich(String richname, String richtitle, String richsubtitle, String richbody) {

		utils.getLocator("campaignSplitPage.splitVarArichcheckbox").click();
		utils.getLocator("campaignSplitPage.splitVarArichthreedot").click();
		utils.getLocator("campaignSplitPage.splitVarAaddrich").click();

		utils.getLocator("campaignSplitPage.richMessageName").clear();
		utils.getLocator("campaignSplitPage.richMessageName").sendKeys(richname);

		utils.getLocator("campaignSplitPage.richTypeButton").click();
		utils.longWaitInSeconds(1);
		utils.getLocator("campaignSplitPage.selectRichType").click();
		utils.longWaitInSeconds(1);

		utils.getLocator("campaignSplitPage.richMessageTitle").clear();
		utils.getLocator("campaignSplitPage.richMessageTitle").sendKeys(richtitle);

		utils.getLocator("campaignSplitPage.richMessageSubtitle").clear();
		utils.getLocator("campaignSplitPage.richMessageSubtitle").sendKeys(richsubtitle);

		utils.getLocator("campaignSplitPage.richMessagebody").clear();
		utils.getLocator("campaignSplitPage.richMessagebody").sendKeys(richbody);

		utils.getLocator("campaignSplitPage.Splitsavebutton").click();

		logger.info("Added Variant A rich msg details successfully");
		TestListeners.extentTest.get().pass(" Added Variant A rich msg details successfully");
	}

	public void setRedeemablesplitVarB(String redemable) {

		utils.getLocator("campaignSplitPage.splitVarBOfferCheckbox").click();
		utils.longWaitInSeconds(1);
		logger.info("Clicked on varB offer checkbox");
		utils.getLocator("campaignSplitPage.VariantBofferDropDown_expand").click();
		logger.info("Clicked on varB offer expand dropdown");
		utils.longWaitInSeconds(1);
		List<WebElement> ele = utils.getLocatorList("campaignSplitPage.VariantBredeemabledropdownlist");
		utils.selectListDrpDwnValue(ele, redemable);
		utils.longWaitInSeconds(1);
		logger.info("VarB offer value selected from dropdwn");
	}

	public void splitVariantBemail(String emailSubject, String emailpreHeader, String emailhtmlbodytext) {

		utils.getLocator("campaignSplitPage.splitVariantBemailcheckbox").click();
		utils.getLocator("campaignSplitPage.splitVariantBemailthreedot").click();
		utils.getLocator("campaignSplitPage.splitVarAaddEmail").click();

		utils.getLocator("campaignSplitPage.splitVarAemaiSubject").clear();
		utils.getLocator("campaignSplitPage.splitVarAemaiSubject").sendKeys(emailSubject);

		utils.getLocator("campaignSplitPage.splitVarAEmailPreheader").clear();
		utils.getLocator("campaignSplitPage.splitVarAEmailPreheader").sendKeys(emailpreHeader);

		utils.getLocator("campaignSplitPage.splitVarAEmailHtmlBodyText").clear();
		utils.getLocator("campaignSplitPage.splitVarAEmailHtmlBodyText").sendKeys(emailhtmlbodytext);

		utils.getLocator("campaignSplitPage.Splitsavebutton").click();

		logger.info("Added Variant B email details successfully");
		TestListeners.extentTest.get().pass(" Added Variant B email details successfully");
	}

	public void splitVariantBPushnotification(String pushTitle, String pushSubtitle, String pushBody) {

		utils.getLocator("campaignSplitPage.splitVariantBpushcheckbox").click();
		utils.getLocator("campaignSplitPage.splitVariantBpushthreedot").click();
		utils.getLocator("campaignSplitPage.splitVarAaddpush").click();

		utils.getLocator("campaignSplitPage.splitVarAPushTitle").clear();
		utils.getLocator("campaignSplitPage.splitVarAPushTitle").sendKeys(pushTitle);

		utils.getLocator("campaignSplitPage.splitVarAPushSubtitle").clear();
		utils.getLocator("campaignSplitPage.splitVarAPushSubtitle").sendKeys(pushSubtitle);

		utils.getLocator("campaignSplitPage.splitVarAPushBody").clear();
		utils.getLocator("campaignSplitPage.splitVarAPushBody").sendKeys(pushBody);

		utils.getLocator("campaignSplitPage.Splitsavebutton").click();

		logger.info("Added Variant B push details successfully");
		TestListeners.extentTest.get().pass(" Added Variant B push details successfully");
	}

	public void splitVariatBSms(String smsbody) {

		utils.getLocator("campaignSplitPage.splitVariantBsmscheckbox").click();
		utils.getLocator("campaignSplitPage.splitVariantBsmsthreedot").click();
		utils.getLocator("campaignSplitPage.splitVarAaddsms").click();

		utils.getLocator("campaignSplitPage.splitVarASmsBody").clear();
		utils.getLocator("campaignSplitPage.splitVarASmsBody").sendKeys(smsbody);

		utils.getLocator("campaignSplitPage.Splitsavebutton").click();

		logger.info("Added Variant B sms details successfully");
		TestListeners.extentTest.get().pass(" Added Variant B sms details successfully");
	}

	public void splitVariantBrich(String richname, String richtitle, String richsubtitle, String richbody) {

		utils.getLocator("campaignSplitPage.splitVariantBrichcheckbox").click();
		utils.getLocator("campaignSplitPage.splitVariantBrichthreedot").click();
		utils.getLocator("campaignSplitPage.splitVarAaddrich").click();

		utils.getLocator("campaignSplitPage.richMessageName").clear();
		utils.getLocator("campaignSplitPage.richMessageName").sendKeys(richname);

		utils.getLocator("campaignSplitPage.richTypeButton").click();
		utils.longWaitInSeconds(2);
		utils.getLocator("campaignSplitPage.selectRichType").click();
		utils.longWaitInSeconds(2);

		utils.getLocator("campaignSplitPage.richMessageTitle").clear();
		utils.getLocator("campaignSplitPage.richMessageTitle").sendKeys(richtitle);

		utils.getLocator("campaignSplitPage.richMessageSubtitle").clear();
		utils.getLocator("campaignSplitPage.richMessageSubtitle").sendKeys(richsubtitle);

		utils.getLocator("campaignSplitPage.richMessagebody").clear();
		utils.getLocator("campaignSplitPage.richMessagebody").sendKeys(richbody);

		utils.getLocator("campaignSplitPage.Splitsavebutton").click();

		logger.info("Added Variant B rich details successfully");
		TestListeners.extentTest.get().pass(" Added Variant B rich msg details successfully");
	}

	public void enterRedeemableNameVarAOfferField(String redeemableName) throws Exception {
		WebElement varAOfferField = utils.getLocator("campaignSplitPage.redeemableFieldVarA");
		varAOfferField.click();
		utils.longWaitInMiliSeconds(500);
		varAOfferField.clear();
		utils.longWaitInMiliSeconds(500);
		varAOfferField.sendKeys(redeemableName);
		utils.getLocator("campaignSplitPage.searchedRedeemable").click();
		logger.info(" Redeemable name entered successfully in Var A field ");
		TestListeners.extentTest.get().pass(" Redeemable name entered successfully in Var A field: " + redeemableName);
	}

	public void enterRedeemableNameVarBOfferField(String redeemableName) {
		WebElement varBOfferField = utils.getLocator("campaignSplitPage.redeemableFieldVarB");
		varBOfferField.click();
		utils.longWaitInMiliSeconds(500);
		varBOfferField.clear();
		utils.longWaitInMiliSeconds(500);
		varBOfferField.sendKeys(redeemableName);
		utils.getLocator("campaignSplitPage.redeemableVarBSearchedField").click();
		logger.info(" Redeemable name entered successfully in Var B field ");
		TestListeners.extentTest.get().pass(" Redeemable name entered successfully in Var B field: " + redeemableName);
	}

	public boolean visibilityOfNoMatchFoundInOfferDrpDwn() {
		return utils.checkElementPresent(utils.getLocator("campaignSplitPage.redeemableNoMatch"));
	}

	public void selectOfferOfVarB() {
		utils.getLocator("campaignSplitPage.splitVarBOfferCheckbox").click();
		logger.info(" Selected Var B offer ");
		TestListeners.extentTest.get().pass(" Selected Var B offer ");
	}

	public void clickOnExpandLessOfOfferDrpDwn() {
		utils.getLocator("campaignSplitPage.redeemableExpandLess").click();
		logger.info(" Selected Expand Less Of Offer DrpDwn ");
		TestListeners.extentTest.get().pass(" Selected Expand Less Of Offer DrpDwn  ");
	}

	public void addPercentInVarAandVarBandControlgroup(String percentA, String percentB, String controlGroup) {
		variantA_Audience(percentA);
		variantB_Audience(percentB);
		controlgroup_Audience(controlGroup);
		logger.info("Percent A, B and control group is filled successfully");
		TestListeners.extentTest.get().pass("Percent A, B and control group is entered successfully");
	}

	public void editEmailOfVarA(String varAemail) {

		utils.getLocator("campaignSplitPage.splitVarAemailthreedot").click();
		utils.getLocator("campaignSplitPage.editButton").click();

		WebElement emailSubject = utils.getLocator("campaignSplitPage.splitVarAemaiSubject");
		emailSubject.clear();
		emailSubject.sendKeys(varAemail);

		WebElement emailPreheader = utils.getLocator("campaignSplitPage.splitVarAEmailPreheader");
		emailPreheader.clear();
		emailPreheader.sendKeys(varAemail);

		WebElement emailHtmlBodyText = utils.getLocator("campaignSplitPage.splitVarAEmailHtmlBodyText");
		emailHtmlBodyText.clear();
		emailHtmlBodyText.sendKeys(varAemail);

		logger.info("Email for Var A is filled successfully");
		TestListeners.extentTest.get().pass("Email for Var A is entered successfully");

	}

	public String getSegmentNameOnWhomPage() {
		WebElement parentAndVarCamId = utils.getLocator("campaignSplitPage.segmentOnWhomPage");
		return parentAndVarCamId.getAttribute("title");
	}

	public boolean isSplitButtonVisible() {
		utils.longWaitInSeconds(2);
		WebElement splitbox = utils.getLocator("campaignSplitPage.splittestbox");
		return splitbox.isDisplayed();
	}

	public void editPNOfVarA(String varApushNotification) {

		utils.getLocator("campaignSplitPage.splitVarApushthreedot").click();
		utils.getLocator("campaignSplitPage.editButton").click();

		WebElement pushTitle = utils.getLocator("campaignSplitPage.splitVarAPushTitle");
		pushTitle.clear();
		pushTitle.sendKeys(varApushNotification);

		WebElement pushSubTitle = utils.getLocator("campaignSplitPage.splitVarAPushSubtitle");
		pushSubTitle.clear();
		pushSubTitle.sendKeys(varApushNotification);

		WebElement pushBody = utils.getLocator("campaignSplitPage.splitVarAPushBody");
		pushBody.clear();
		pushBody.sendKeys(varApushNotification);

		logger.info("Push Notification for Var A is filled successfully");
		TestListeners.extentTest.get().pass("Push Notification for Var A is entered successfully");

	}

	public void editSMSOfVarA(String varAsms) {
		utils.getLocator("campaignSplitPage.splitVarAsmsthreedot").click();
		utils.getLocator("campaignSplitPage.editButton").click();

		WebElement smsBody = utils.getLocator("campaignSplitPage.splitVarASmsBody");
		smsBody.clear();
		smsBody.sendKeys(varAsms);

		logger.info("SMS for Var A is filled successfully");
		TestListeners.extentTest.get().pass("SMS for Var A is entered successfully");
	}

	public void editRMOfVarA(String varArichMsg) {
		utils.getLocator("campaignSplitPage.splitVarArichthreedot").click();
		utils.getLocator("campaignSplitPage.editButton").click();

		WebElement richMessageName = utils.getLocator("campaignSplitPage.richMessageName");
		richMessageName.clear();
		richMessageName.sendKeys(varArichMsg);

		WebElement richMessageTitle = utils.getLocator("campaignSplitPage.richMessageTitle");
		richMessageTitle.clear();
		richMessageTitle.sendKeys(varArichMsg);

		WebElement richMessageSubtitle = utils.getLocator("campaignSplitPage.richMessageSubtitle");
		richMessageSubtitle.clear();
		richMessageSubtitle.sendKeys(varArichMsg);

		WebElement richMessagebody = utils.getLocator("campaignSplitPage.richMessagebody");
		richMessagebody.clear();
		richMessagebody.sendKeys(varArichMsg);

		logger.info("Rich Message for Var A is filled successfully");
		TestListeners.extentTest.get().pass("Rich Message for Var A is entered successfully");

	}

	public void editEmailOfVarB(String varBemail) {
		utils.getLocator("campaignSplitPage.splitVariantBemailthreedot").click();
		utils.getLocator("campaignSplitPage.editButton").click();

		WebElement emailSubject = utils.getLocator("campaignSplitPage.splitVarAemaiSubject");
		emailSubject.clear();
		emailSubject.sendKeys(varBemail);

		WebElement emailPreheader = utils.getLocator("campaignSplitPage.splitVarAEmailPreheader");
		emailPreheader.clear();
		emailPreheader.sendKeys(varBemail);

		WebElement emailHtmlBodyText = utils.getLocator("campaignSplitPage.splitVarAEmailHtmlBodyText");
		emailHtmlBodyText.clear();
		emailHtmlBodyText.sendKeys(varBemail);

		logger.info("Email for Var B is filled successfully");
		TestListeners.extentTest.get().pass("Email for Var B is entered successfully");
	}

	public void editPNOfVarB(String varBpushNotification) {
		utils.getLocator("campaignSplitPage.splitVariantBpushthreedot").click();
		utils.getLocator("campaignSplitPage.editButton").click();

		WebElement pushTitle = utils.getLocator("campaignSplitPage.splitVarAPushTitle");
		pushTitle.clear();
		pushTitle.sendKeys(varBpushNotification);

		WebElement pushSubTitle = utils.getLocator("campaignSplitPage.splitVarAPushSubtitle");
		pushSubTitle.clear();
		pushSubTitle.sendKeys(varBpushNotification);

		WebElement pushBody = utils.getLocator("campaignSplitPage.splitVarAPushBody");
		pushBody.clear();
		pushBody.sendKeys(varBpushNotification);

		logger.info("Push Notification for Var B is filled successfully");
		TestListeners.extentTest.get().pass("Push Notification for Var B is entered successfully");
	}

	public void editSMSOfVarB(String varBsms) {
		utils.getLocator("campaignSplitPage.splitVariantBsmsthreedot").click();
		utils.getLocator("campaignSplitPage.editButton").click();

		WebElement smsBody = utils.getLocator("campaignSplitPage.splitVarASmsBody");
		smsBody.clear();
		smsBody.sendKeys(varBsms);

		logger.info("SMS for Var B is filled successfully");
		TestListeners.extentTest.get().pass("SMS for Var B is entered successfully");
	}

	public void editRMOfVarB(String varBrichMsg) {
		utils.getLocator("campaignSplitPage.splitVariantBrichthreedot").click();
		utils.getLocator("campaignSplitPage.editButton").click();

		WebElement richMessageName = utils.getLocator("campaignSplitPage.richMessageName");
		richMessageName.clear();
		richMessageName.sendKeys(varBrichMsg);

		WebElement richMessageTitle = utils.getLocator("campaignSplitPage.richMessageTitle");
		richMessageTitle.clear();
		richMessageTitle.sendKeys(varBrichMsg);

		WebElement richMessageSubtitle = utils.getLocator("campaignSplitPage.richMessageSubtitle");
		richMessageSubtitle.clear();
		richMessageSubtitle.sendKeys(varBrichMsg);

		WebElement richMessagebody = utils.getLocator("campaignSplitPage.richMessagebody");
		richMessagebody.clear();
		richMessagebody.sendKeys(varBrichMsg);

		logger.info("Rich Message for Var B is filled successfully");
		TestListeners.extentTest.get().pass("Rich Message for Var B is entered successfully");
	}

	public void selectRedeemableFromVarAOfferField(String redeemableName) throws Exception {
		WebElement varAOfferField = utils.getLocator("campaignSplitPage.redeemableFieldVarA");
		varAOfferField.click();
		utils.longWaitInMiliSeconds(500);
		varAOfferField.clear();
		utils.longWaitInMiliSeconds(500);
		varAOfferField.sendKeys(redeemableName);
		logger.info(" Redeemable name entered successfully in Var A field ");
		TestListeners.extentTest.get().pass(" Redeemable name entered successfully in Var A field: " + redeemableName);
	}

	public void selectRedeemableFromVarBOfferField(String redeemableName) {
		WebElement varBOfferField = utils.getLocator("campaignSplitPage.redeemableFieldVarB");
		varBOfferField.click();
		utils.longWaitInMiliSeconds(500);
		varBOfferField.clear();
		utils.longWaitInMiliSeconds(500);
		varBOfferField.sendKeys(redeemableName);
		// utils.getLocator("campaignSplitPage.redeemableFieldVarB");
		logger.info(" Redeemable name entered successfully in Var B field ");
		TestListeners.extentTest.get().pass(" Redeemable name entered successfully in Var B field: " + redeemableName);
	}

	public void selectAndAddEmailOfVarA(String varAemail) {

		utils.getLocator("campaignSplitPage.splitVarAemailcheckbox").click();
		utils.longWaitInMiliSeconds(500);
		logger.info("Email for Var A is successfully checked");
		TestListeners.extentTest.get().pass("Email for Var A is successfully checked");
		utils.getLocator("campaignSplitPage.splitVarAemailthreedot").click();
		utils.getLocator("campaignSplitPage.splitVarAaddEmail").click();
		utils.longWaitInMiliSeconds(500);
		WebElement emailSubject = utils.getLocator("campaignSplitPage.splitVarAemaiSubject");
		emailSubject.clear();
		emailSubject.sendKeys(varAemail);

		WebElement emailPreheader = utils.getLocator("campaignSplitPage.splitVarAEmailPreheader");
		emailPreheader.clear();
		emailPreheader.sendKeys(varAemail);

		WebElement emailHtmlBodyText = utils.getLocator("campaignSplitPage.splitVarAEmailHtmlBodyText");
		emailHtmlBodyText.clear();
		emailHtmlBodyText.sendKeys(varAemail);

		logger.info("Email for Var A is filled successfully");
		TestListeners.extentTest.get().pass("Email for Var A is entered successfully");

	}

	public void selectAndAddPNOfVarA(String varApushNotification) {

		utils.getLocator("campaignSplitPage.splitVarAPushNotificationcheckbox").click();
		utils.getLocator("campaignSplitPage.splitVarApushthreedot").click();
		utils.getLocator("campaignSplitPage.splitVarAaddpush").click();

		WebElement pushTitle = utils.getLocator("campaignSplitPage.splitVarAPushTitle");
		pushTitle.clear();
		pushTitle.sendKeys(varApushNotification);

		WebElement pushSubTitle = utils.getLocator("campaignSplitPage.splitVarAPushSubtitle");
		pushSubTitle.clear();
		pushSubTitle.sendKeys(varApushNotification);

		WebElement pushBody = utils.getLocator("campaignSplitPage.splitVarAPushBody");
		pushBody.clear();
		pushBody.sendKeys(varApushNotification);

		logger.info("Push Notification for Var A is filled successfully");
		TestListeners.extentTest.get().pass("Push Notification for Var A is entered successfully");

	}

	public void selectAndAddSMSOfVarA(String varAsms) {
		utils.getLocator("campaignSplitPage.splitVarAsmscheckbox").click();
		utils.getLocator("campaignSplitPage.splitVarAsmsthreedot").click();
		utils.getLocator("campaignSplitPage.splitVarAaddsms").click();

		WebElement smsBody = utils.getLocator("campaignSplitPage.splitVarASmsBody");
		smsBody.clear();
		smsBody.sendKeys(varAsms);

		logger.info("SMS for Var A is filled successfully");
		TestListeners.extentTest.get().pass("SMS for Var A is entered successfully");
	}

	public void selectAndAddRMOfVarA(String varArichMsg) {
		utils.getLocator("campaignSplitPage.splitVarArichcheckbox").click();
		utils.getLocator("campaignSplitPage.splitVarArichthreedot").click();
		utils.getLocator("campaignSplitPage.splitVarAaddrich").click();

		WebElement richMessageName = utils.getLocator("campaignSplitPage.richMessageName");
		richMessageName.clear();
		richMessageName.sendKeys(varArichMsg);

		utils.getLocator("campaignSplitPage.richTypeButton").click();
		utils.longWaitInSeconds(1);
		utils.getLocator("campaignSplitPage.selectRichType").click();

		WebElement richMessageTitle = utils.getLocator("campaignSplitPage.richMessageTitle");
		richMessageTitle.clear();
		richMessageTitle.sendKeys(varArichMsg);

		WebElement richMessageSubtitle = utils.getLocator("campaignSplitPage.richMessageSubtitle");
		richMessageSubtitle.clear();
		richMessageSubtitle.sendKeys(varArichMsg);

		WebElement richMessagebody = utils.getLocator("campaignSplitPage.richMessagebody");
		richMessagebody.clear();
		richMessagebody.sendKeys(varArichMsg);

		logger.info("Rich Message for Var A is filled successfully");
		TestListeners.extentTest.get().pass("Rich Message for Var A is entered successfully");

	}

	public void selectAndAddEmailOfVarB(String varBemail) {
		utils.getLocator("campaignSplitPage.splitVariantBemailcheckbox").click();
		utils.getLocator("campaignSplitPage.splitVariantBemailthreedot").click();
		utils.getLocator("campaignSplitPage.splitVarAaddEmail").click();

		WebElement emailSubject = utils.getLocator("campaignSplitPage.splitVarAemaiSubject");
		emailSubject.clear();
		emailSubject.sendKeys(varBemail);

		WebElement emailPreheader = utils.getLocator("campaignSplitPage.splitVarAEmailPreheader");
		emailPreheader.clear();
		emailPreheader.sendKeys(varBemail);

		WebElement emailHtmlBodyText = utils.getLocator("campaignSplitPage.splitVarAEmailHtmlBodyText");
		emailHtmlBodyText.clear();
		emailHtmlBodyText.sendKeys(varBemail);

		logger.info("Email for Var B is filled successfully");
		TestListeners.extentTest.get().pass("Email for Var B is entered successfully");
	}

	public void selectAndAddPNOfVarB(String varBpushNotification) {
		utils.getLocator("campaignSplitPage.splitVariantBpushcheckbox").click();
		utils.getLocator("campaignSplitPage.splitVariantBpushthreedot").click();
		utils.getLocator("campaignSplitPage.splitVarAaddpush").click();

		WebElement pushTitle = utils.getLocator("campaignSplitPage.splitVarAPushTitle");
		pushTitle.clear();
		pushTitle.sendKeys(varBpushNotification);

		WebElement pushSubTitle = utils.getLocator("campaignSplitPage.splitVarAPushSubtitle");
		pushSubTitle.clear();
		pushSubTitle.sendKeys(varBpushNotification);

		WebElement pushBody = utils.getLocator("campaignSplitPage.splitVarAPushBody");
		pushBody.clear();
		pushBody.sendKeys(varBpushNotification);

		logger.info("Push Notification for Var B is filled successfully");
		TestListeners.extentTest.get().pass("Push Notification for Var B is entered successfully");
	}

	public void selectAndAddSMSOfVarB(String varBsms) {
		utils.getLocator("campaignSplitPage.splitVariantBsmscheckbox").click();
		utils.getLocator("campaignSplitPage.splitVariantBsmsthreedot").click();
		utils.getLocator("campaignSplitPage.splitVarAaddsms").click();

		WebElement smsBody = utils.getLocator("campaignSplitPage.splitVarASmsBody");
		smsBody.clear();
		smsBody.sendKeys(varBsms);

		logger.info("SMS for Var B is filled successfully");
		TestListeners.extentTest.get().pass("SMS for Var B is entered successfully");
	}

	public void selectAndAddRMOfVarB(String varBrichMsg) {
		utils.getLocator("campaignSplitPage.splitVariantBrichcheckbox").click();
		utils.getLocator("campaignSplitPage.splitVariantBrichthreedot").click();
		utils.getLocator("campaignSplitPage.splitVarAaddrich").click();

		WebElement richMessageName = utils.getLocator("campaignSplitPage.richMessageName");
		richMessageName.clear();
		richMessageName.sendKeys(varBrichMsg);

		WebElement richMessageTitle = utils.getLocator("campaignSplitPage.richMessageTitle");
		richMessageTitle.clear();
		richMessageTitle.sendKeys(varBrichMsg);

		WebElement richMessageSubtitle = utils.getLocator("campaignSplitPage.richMessageSubtitle");
		richMessageSubtitle.clear();
		richMessageSubtitle.sendKeys(varBrichMsg);

		WebElement richMessagebody = utils.getLocator("campaignSplitPage.richMessagebody");
		richMessagebody.clear();
		richMessagebody.sendKeys(varBrichMsg);

		logger.info("Rich Message for Var B is filled successfully");
		TestListeners.extentTest.get().pass("Rich Message for Var B is entered successfully");
	}

	public void setRedeemableVarBOfferField(String redeemableName) {
		WebElement varBOfferField = utils.getLocator("campaignSplitPage.redeemableFieldVarB");
		varBOfferField.click();
		utils.longWaitInMiliSeconds(500);
		varBOfferField.clear();
		utils.longWaitInMiliSeconds(500);
		varBOfferField.sendKeys(redeemableName);
		WebElement redeemable = driver.findElement(
				By.xpath(utils.getLocatorValue("campaignSplitPage.redeemableName").replace("$temp", redeemableName)));
		utils.clickByJSExecutor(driver, redeemable);
//		redeemable.click();
		logger.info(" Redeemable selected successfully in Var B field ");
		TestListeners.extentTest.get().pass(" Redeemableselected successfully in Var B field: " + redeemableName);
	}

	public void setRedeemableVarAOfferField(String redeemableName) {
		WebElement varAOfferField = utils.getLocator("campaignSplitPage.redeemableFieldVarA");
		varAOfferField.click();
		utils.longWaitInMiliSeconds(500);
		varAOfferField.clear();
		utils.longWaitInMiliSeconds(500);
		varAOfferField.sendKeys(redeemableName);
		WebElement redeemable = driver.findElement(
				By.xpath(utils.getLocatorValue("campaignSplitPage.redeemableName").replace("$temp", redeemableName)));
//		redeemable.click();
		utils.waitTillElementToBeClickable(redeemable);
		utils.clickByJSExecutor(driver, redeemable);
		logger.info(" Redeemable selected successfully in Var A field ");
		TestListeners.extentTest.get().pass(" Redeemableselected successfully in Var A field: " + redeemableName);
	}

	public void clickOnRemoveButton() {
		utils.getLocator("campaignSplitPage.removebutton").click();
		logger.info(" clicked on remove button ");
		TestListeners.extentTest.get().pass(" clicked on remove button");

	}

	public void clickOnYesRemoveButton() {
		utils.getLocator("campaignSplitPage.yesRemove").click();
		logger.info(" clicked on yes, remove button ");
		utils.longWaitInSeconds(5);
		TestListeners.extentTest.get().pass(" clicked on yes, remove button");

	}

	public String getSplitButtonText() {
		WebElement splitButton = utils.getLocator("campaignSplitPage.editsplitbutton");
		String splitButtonText = splitButton.getText();
		logger.info(" Text of split button :" + splitButtonText);
		TestListeners.extentTest.get().pass(" Text of split button :" + splitButtonText);
		return splitButtonText;
	}

	public boolean verifySplitTestElementsVisibility(String element, String variant, String checkBoxName) {
		utils.longWaitInSeconds(2);
		boolean flag = false;
		switch (element) {
		case "splitTestDialogBox":
			flag = utils.checkElementPresent(utils.getLocator("campaignSplitPage.splitTestDialogBox"));
			if (flag) {
				logger.info("Split box is present");
				TestListeners.extentTest.get().info("Split box is present");
			}
			break;
		case "splitTestTitle":
			flag = utils.checkElementPresent(utils.getLocator("campaignSplitPage.splitTestTitle"));
			if (flag) {
				logger.info("Split test title is present");
				TestListeners.extentTest.get().info("Split test title is present");
			}
			break;
		case "cancelBtn":
			flag = utils.checkElementPresent(utils.getLocator("campaignSplitPage.splitCancelbutton"));
			if (flag) {
				logger.info("Cancel button is present");
				TestListeners.extentTest.get().info("Cancel button is present");
			}
			break;
		case "close":
			flag = utils.checkElementPresent(utils.getLocator("campaignSplitPage.closeIcon"));
			if (flag) {
				logger.info("Close Icon is present");
				TestListeners.extentTest.get().info("Close Icon is present");
			}
			break;
		case "nextBtn":
			flag = utils.checkElementPresent(utils.getLocator("campaignSplitPage.splitNextButton"));
			if (flag) {
				logger.info("Next button is present");
				TestListeners.extentTest.get().info("Next button is present");
			}
			break;
		case "back":
			flag = utils.checkElementPresent(utils.getLocator("campaignSplitPage.splitBackButton"));
			if (flag) {
				logger.info("Back button is present");
				TestListeners.extentTest.get().info("Back button is present");
			}
			break;
		case "checkBox":
			String xpath = utils.getLocatorValue("campaignSplitPage.splitVariantCheckBox").replace("$variant", variant)
					.replace("$temp", checkBoxName);
			WebElement ele = driver.findElement(By.xpath(xpath));
			flag = utils.checkElementPresent(ele);
			if (flag) {
				logger.info("Check box for " + checkBoxName + " in " + variant + " is present");
				TestListeners.extentTest.get().info("Check box for " + checkBoxName + " in " + variant + " is present");
			}
			break;
		case "splitButton":
			flag = utils.checkElementPresent(utils.getLocator("campaignSplitPage.editsplitbutton"));
			if (flag) {
				logger.info("Split button is present");
				TestListeners.extentTest.get().info("Split button is present");
			}
			break;
		default:
			logger.info("No matching element provided: " + element);
			TestListeners.extentTest.get().info("No matching element provided: " + element);
			flag = false;
		}

		return flag;
	}

	public String verifySplitTestVariantPercentage(String choice) {
		String xpath = utils.getLocatorValue("campaignSplitPage.varPercentageInputBox").replace("$temp", choice);
		WebElement varPercentageInputBox = driver.findElement(By.xpath(xpath));
		String percentage = varPercentageInputBox.getAttribute("value");
		return percentage;
	}

	public void checkGivenSplitTestCheckBox(String variant, String channel) {
		String xpath = utils.getLocatorValue("campaignSplitPage.splitVariantCheckBox").replace("$variant", variant)
				.replace("$temp", channel);
		WebElement checkBox = driver.findElement(By.xpath(xpath));
		if (!checkBox.isSelected()) {
			checkBox.click();
			logger.info("Check box for " + channel + " of " + variant + " is checked");
			TestListeners.extentTest.get().info("Check box for " + channel + " of " + variant + " is checked");
		} else {
			logger.info("Check box for " + channel + " of " + variant + " is already checked");
			TestListeners.extentTest.get().info("Check box for " + channel + " of " + variant + " is already checked");
		}
	}

	public String setDuplicateContentFromVariantA(String variant) {
		utils.getLocator("campaignSplitPage.splitVariantBemailcheckbox").click();
		utils.getLocator("campaignSplitPage.splitVariantBemailthreedot").click();
		utils.getLocator("campaignSplitPage.duplicateContent").click();
		return verifyVariantEmail(variant);
	}

	public String verifyVariantEmail(String variant) {
		String xpath = utils.getLocatorValue("campaignSplitPage.variantsEmailSubtitle").replace("$variant", variant);
		WebElement emailSubtitle = driver.findElement(By.xpath(xpath));
		String text = emailSubtitle.getText();
		return text;
	}

	public List<String> verifySplitTestReviewPage(String choice) {
		String xpath = utils.getLocatorValue("campaignSplitPage.reviewPageElements").replace("$temp", choice);
		List<WebElement> breakdowns = driver.findElements(By.xpath(xpath));
		List<String> actualValues = new ArrayList<>();

		for (WebElement breakdown : breakdowns) {
			logger.info(breakdown.getText()); // original text
			String cleaned = breakdown.getText().replaceAll("\\s*\\(.*?\\)", ""); // remove values in brackets
			actualValues.add(cleaned);
		}
		return actualValues;
	}

	public boolean addEmailTemplateToVariant(String variant, String checkBoxName, String emailSubject) {
		boolean flag = false;
		String xpath = utils.getLocatorValue("campaignSplitPage.splitVariantCheckBox").replace("$variant", variant)
				.replace("$temp", checkBoxName);
		driver.findElement(By.xpath(xpath)).click();

		String xpath1 = utils.getLocatorValue("campaignSplitPage.splitVariantChannelEllipsis")
				.replace("$variant", variant).replace("$channel", checkBoxName);
		driver.findElement(By.xpath(xpath1)).click();
		utils.getLocator("campaignSplitPage.splitVarAaddEmail").click();
		utils.getLocator("campaignSplitPage.emailEditorCheckbox").click();
		utils.longWaitInSeconds(2);
		utils.getLocator("emailTemplatePage.templateCardImage").isDisplayed();
		selUtils.mouseHoverOverElement(utils.getLocator("emailTemplatePage.templateCardImage"));
		utils.getLocator("campaignSplitPage.chooseTemplate").click();
		utils.longWaitInSeconds(15);
		selUtils.waitTillElementToBeClickable(utils.getLocator("campaignSplitPage.saveAndCloseButton"));
		utils.clickByJSExecutor(driver, utils.getLocator("campaignSplitPage.saveAndCloseButton"));

		utils.getLocator("campaignSplitPage.splitVarAemaiSubject").clear();
		utils.getLocator("campaignSplitPage.splitVarAemaiSubject").sendKeys(emailSubject);

		flag = utils.checkElementPresent(utils.getLocator("campaignSplitPage.addedEmailTemplate"));
		return flag;
	}

	public String[] getParentAndVarCamIdFromSidePanel() {
		WebElement parentAndVarCamId = utils.getLocator("campaignSplitPage.parentAndVarCamIdFromSidePanel");
		String parentAndVarCamIdText = parentAndVarCamId.getText();
		String[] parentVarAandVarBCamId = parentAndVarCamIdText.split("\\|");
		String[] parentVarAandVarBCamId1 = new String[3];
		// Parent: 1819 | Variant A: 1820 | Variant B: 1821
		// parentVarAandVarBCamId[1]= Parent: 1819
		// parentVarAandVarBCamId[2]= Variant A: 1820
		// parentVarAandVarBCamId[3]=Variant B: 1821
		int lenght = parentVarAandVarBCamId.length;
		for (int i = 0; i < lenght; i++) {
			logger.info(" Element at " + i + " position is: " + parentVarAandVarBCamId[i]);
			parentVarAandVarBCamId1[i] = parentVarAandVarBCamId[i].split(":")[1].trim();
		}
		return parentVarAandVarBCamId1;

	}

	public String getCamIdsFromCSP() {
		WebElement parentAndVarCamId = utils.getLocator("campaignSplitPage.camIdsOnCSP");
		return parentAndVarCamId.getText();
	}

	public String getCamIdsFromSidePanel() {
		WebElement parentAndVarCamId = utils.getLocator("campaignSplitPage.parentAndVarCamIdFromSidePanel");
		return parentAndVarCamId.getText();
	}

	public String getCamNameFromCSP() {
		WebElement parentAndVarCamId = utils.getLocator("campaignSplitPage.camNameOnCSP");
		return parentAndVarCamId.getText().trim();
	}

	public boolean validateLivePreviewVariantASectionOnSummaryPage() {
		WebElement variantASection = utils.getLocator("campaignSplitPage.summaryPageVariantA");
		Boolean variantASectionVisible = utils.checkElementPresent(variantASection);
		logger.info(" Variant A section displayed :" + variantASectionVisible);
		TestListeners.extentTest.get().pass(" Variant A section displayed :" + variantASectionVisible);
		return variantASectionVisible;

	}

	public boolean validateLivePreviewVariantBSectionOnSummaryPage() {
		WebElement variantBSection = utils.getLocator("campaignSplitPage.summaryPageVariantB");
		Boolean variantBSectionVisible = utils.checkElementPresent(variantBSection);
		logger.info(" Variant B section displayed :" + variantBSectionVisible);
		TestListeners.extentTest.get().pass(" Variant B section displayed :" + variantBSectionVisible);
		return variantBSectionVisible;

	}

	public boolean validateLivePreviewSectionVisibleOnSummaryPage() {
		WebElement livePreviewSection = utils.getLocator("campaignSplitPage.livePreviewSection");
		Boolean livePreviewSectionVisible = utils.checkElementPresent(livePreviewSection);
		logger.info(" Live preview section displayed :" + livePreviewSectionVisible);
		TestListeners.extentTest.get().pass(" Live preview section displayed :" + livePreviewSectionVisible);
		return livePreviewSectionVisible;

	}

	public void clickPreviewLink(String channel) {
		String xpath = utils.getLocatorValue("campaignSplitPage.previewLink").replace("$channel", channel);
		driver.findElement(By.xpath(xpath)).click();
		logger.info("Clicked on preview link on Split Test review page for " + channel);
		TestListeners.extentTest.get().pass("Clicked on preview link on Split Test review page for " + channel);
	}

	public List<String> verifySplitTestNewCHPoptions() {
		utils.waitTillElementToBeClickable(utils.getLocator("newCamHomePage.templateEllipsis"));
		utils.getLocator("newCamHomePage.templateEllipsis").click();
		List<WebElement> eleLst = utils.getLocatorList("campaignSplitPage.splitTestNewChpOptions");
		List<String> optionList = new ArrayList<>();
		for (int i = 0; i < eleLst.size(); i++) {
			String text = eleLst.get(i).getText();
			optionList.add(text);
		}
		return optionList;
	}

	public List<String> verifySplitTestEllipsisOptionsClassicCPP() {
		utils.waitTillPagePaceDone();
		utils.getLocator("campaignsPage.newEllipsisBtn").click();
		List<WebElement> eleLst = utils.getLocatorList("campaignSplitPage.classicCppEllipsisOptions");
		List<String> optionList = new ArrayList<>();
		for (int i = 0; i < eleLst.size(); i++) {
			String text = eleLst.get(i).getText();
			optionList.add(text);
		}
		return optionList;
	}

	public String verifyFontFamilyAndroidApple(String device, String deviceContainer) {
		String xpath = utils.getLocatorValue("campaignSplitPage.previewPageChoices").replace("$temp", device);
		driver.findElement(By.xpath(xpath)).click();
		utils.getLocator("campaignSplitPage.newTab").click();
		utils.switchToWindow();
		String xpath1 = utils.getLocatorValue("campaignSplitPage.androidIOSPushNotification").replace("$device",
				deviceContainer);
		WebElement fontFamilyElement = driver.findElement(By.xpath(xpath1));
		// String fontFamily= fontFamilyElement.getCssValue("font-family");
		String style = fontFamilyElement.getAttribute("style");
		return style;

	}

	public boolean isSplitButtonDisabled() {
		WebElement splitButton = utils.getLocator("campaignSplitPage.editsplitbutton");
		boolean isSplitButtonDisabled = splitButton.getAttribute("class").contains("disabled_link");
		logger.info(" Split button is disabled?: " + isSplitButtonDisabled);
		TestListeners.extentTest.get().pass(" Split button is disabled?: " + isSplitButtonDisabled);
		return isSplitButtonDisabled;

	}

	public void clickXButton() {
		utils.longWaitInSeconds(1);
		WebElement xButton = utils.getLocator("campaignSplitPage.closeButton");
		xButton.click();
		logger.info(" Clicked on x button ");
		TestListeners.extentTest.get().pass(" Clicked on x button ");
	}

	public void closeSplitDialog(String choice) {
		if (choice == "xButton")
			clickXButton();
		else if (choice == "cancelButton")
			splitCancelButton();
		else
			logger.info("No matching element provided: " + choice);
		splitYesCancelButton();
		logger.info(" Closed split dialog ");
		TestListeners.extentTest.get().pass(" Closed split dialog ");

	}

	public void clickNoContinueEditingButton() {
		utils.longWaitInSeconds(1);
		WebElement noContinueEditingButton = utils.getLocator("campaignSplitPage.splitNoContinueEditing");
		noContinueEditingButton.click();
		logger.info(" Clicked on No, continue editing button ");
		TestListeners.extentTest.get().pass(" Clicked on No, continue editing button ");
	}

	public boolean isNoContinueButtonVisible() {
		boolean status = false;
		try {
			utils.longWaitInSeconds(2);
			WebElement ele = utils.getLocator("campaignSplitPage.splitNoContinueEditing");
			status = utils.checkElementPresent(ele);
		} catch (Exception e) {
		}
		selUtils.implicitWait(5);
		return status;
	}

	public String verifyTextUnderSplitTestPanel() {
		utils.getLocator("campaignSplitPage.textUnderSplitTest").isDisplayed();
		String text = utils.getLocator("campaignSplitPage.textUnderSplitTest").getText();
		return text;
	}

	public boolean checkSplitTestRedeemableFieldState() {
		WebElement ele = utils.getLocator("signupCampaignsPage.redeemableDropDownList_select");
		return ele.isEnabled();
	}

	public String getActualGiftType() {
		WebElement giftType = utils.getLocator("campaignSplitPage.giftTypeforSplit");
		String title = giftType.getAttribute("title");
		return title;
	}

	public String getActualRedeemableName() {
		WebElement redeemableName = utils.getLocator("campaignSplitPage.redeemableforSplit");
		String title = redeemableName.getAttribute("title");
		return title;
	}

	public boolean isCheckboxChecked(String checkboxName) {
		String xpath = utils.getLocatorValue("campaignSplitPage.richMessageCheckbox").replace("$temp", checkboxName);
		WebElement checkBox = driver.findElement(By.xpath(xpath));
		if (checkBox.getText().contains("check_box_outline_blank")) {
			logger.info("Checkbox " + checkboxName + " is not checked");
			TestListeners.extentTest.get().info("Checkbox " + checkboxName + " is not checked");
			return false;
		} else {
			logger.info("Checkbox " + checkboxName + " is checked");
			TestListeners.extentTest.get().info("Checkbox " + checkboxName + " is checked");
			return true;
		}
	}

	public void enablePrimaryCTAAndEnterData(String primaryCTA, String primaryCTATypeOption) {

		if (!isCheckboxChecked(primaryCTA)) {
			// Enable the primary CTA checkbox
			String xpath = utils.getLocatorValue("campaignSplitPage.richMessageCheckbox").replace("$temp", primaryCTA);
			WebElement checkBox = driver.findElement(By.xpath(xpath));
			checkBox.click();
		}

		// Enter data in the primary CTA fields
		utils.getLocator("campaignSplitPage.primaryCTATypeList").click();
		// primaryCTATypeOptions->External Web Link, Internal Web Link, Deep Link, Drill
		// Down, Survey ID
		utils.longWaitInSeconds(1);
		String xpath = utils.getLocatorValue("campaignSplitPage.primaryCTATypeOptions").replace("$temp",
				primaryCTATypeOption);
		WebElement primaryCTAType = driver.findElement(By.xpath(xpath));
		primaryCTAType.click();

		WebElement primaryCTALinkTextBox = utils.getLocator("campaignSplitPage.primaryCTALinkTextBox");
		primaryCTALinkTextBox.clear();
		primaryCTALinkTextBox.sendKeys(primaryCTA);

		WebElement primaryCTALabel = utils.getLocator("campaignSplitPage.primaryCTALabel");
		primaryCTALabel.clear();
		primaryCTALabel.sendKeys(primaryCTA);

		logger.info("Primary CTA is enabled and data is entered successfully");
		TestListeners.extentTest.get().pass("Primary CTA is enabled and data is entered successfully: " + primaryCTA);
	}

	public void enableSecondaryCTAAndEnterData(String secondaryCTA, String secondaryCTATypeOption) {

		if (!isCheckboxChecked(secondaryCTA)) {
			// Enable the primary CTA checkbox
			String xpath = utils.getLocatorValue("campaignSplitPage.richMessageCheckbox").replace("$temp",
					secondaryCTA);
			WebElement checkBox = driver.findElement(By.xpath(xpath));
			checkBox.click();
		}

		// Enter data in the primary CTA fields
		utils.getLocator("campaignSplitPage.secondaryCTATypeList").click();
		// primaryCTATypeOptions->External Web Link, Internal Web Link, Deep Link, Drill
		// Down, Survey ID
		utils.longWaitInSeconds(1);
		String xpath = utils.getLocatorValue("campaignSplitPage.secondaryCTATypeOptions").replace("$temp",
				secondaryCTATypeOption);
		WebElement secondaryCTAType = driver.findElement(By.xpath(xpath));
		secondaryCTAType.click();

		WebElement secondaryCTALinkTextBox = utils.getLocator("campaignSplitPage.secondaryCTALinkTextBox");
		secondaryCTALinkTextBox.clear();
		secondaryCTALinkTextBox.sendKeys(secondaryCTA);

		WebElement primaryCTALabel = utils.getLocator("campaignSplitPage.secondaryCTALabel");
		primaryCTALabel.clear();
		primaryCTALabel.sendKeys(secondaryCTA);

		logger.info("Secondary CTA is enabled and data is entered successfully");
		TestListeners.extentTest.get()
				.pass("Secondary CTA is enabled and data is entered successfully: " + secondaryCTA);
	}

	public void enableBackgroundContentAndEnterData(String backgroundContent, String backgroundContentTypeOption) {

		if (!isCheckboxChecked(backgroundContent)) {
			// Enable the primary CTA checkbox
			String xpath = utils.getLocatorValue("campaignSplitPage.richMessageCheckbox").replace("$temp",
					backgroundContent);
			WebElement checkBox = driver.findElement(By.xpath(xpath));
			checkBox.click();
		}
		// Enter data in the primary CTA fields
		utils.getLocator("campaignSplitPage.backgroundContentTypeList").click();
		// primaryCTATypeOptions->External Web Link, Internal Web Link, Deep Link, Drill
		// Down, Survey ID
		utils.longWaitInSeconds(1);
		String xpath = utils.getLocatorValue("campaignSplitPage.backgroundContentTypeOptions").replace("$temp",
				backgroundContentTypeOption);
		WebElement backgroundContentType = driver.findElement(By.xpath(xpath));
		backgroundContentType.click();

		WebElement backgroundContentAlternateTextBox = utils
				.getLocator("campaignSplitPage.backgroundContentAlternateTextBox");
		backgroundContentAlternateTextBox.clear();
		backgroundContentAlternateTextBox.sendKeys(backgroundContent);

		logger.info("Background Content is enabled and data is entered successfully");
		TestListeners.extentTest.get()
				.pass("Background Content is enabled and data is entered successfully: " + backgroundContent);
	}

	public void enableMetaAndEnterData(String meta) {

		if (!isCheckboxChecked(meta)) {
			// Enable the primary CTA checkbox
			String xpath = utils.getLocatorValue("campaignSplitPage.richMessageCheckbox").replace("$temp", meta);
			WebElement checkBox = driver.findElement(By.xpath(xpath));
			checkBox.click();
		}
		utils.longWaitInSeconds(1);
		WebElement metaRank = utils.getLocator("campaignSplitPage.metaRank");
		metaRank.clear();
		metaRank.sendKeys(meta);
		WebElement metaTitle = utils.getLocator("campaignSplitPage.metaTitle");
		metaTitle.clear();
		metaTitle.sendKeys(meta);

		logger.info("Meta is enabled and data is entered successfully");
		TestListeners.extentTest.get().pass("Meta is enabled and data is entered successfully: " + meta);
	}

	public void setRichMessageCTAFlag(String checkboxName, boolean isEnable) {
		boolean isChecked = isCheckboxChecked(checkboxName); // TRUE
		if (isEnable && !isChecked) {
			// Enable the checkbox if not already enabled
			String xpath = utils.getLocatorValue("campaignSplitPage.richMessageCheckbox").replace("$temp",
					checkboxName);
			WebElement checkBox = driver.findElement(By.xpath(xpath));
			checkBox.click();
			logger.info("Enabled CTA Flag: " + checkboxName);
			TestListeners.extentTest.get().pass("Enabled CTA Flag: " + checkboxName);
		} else if (!isEnable && isChecked) {
			// Disable the checkbox if not already disabled
			String xpath = utils.getLocatorValue("campaignSplitPage.richMessageCheckbox").replace("$temp",
					checkboxName);
			WebElement checkBox = driver.findElement(By.xpath(xpath));
			checkBox.click();
			logger.info("Disabled CTA Flag: " + checkboxName);
			TestListeners.extentTest.get().pass("Disabled CTA Flag: " + checkboxName);
		}
	}

	public String getTextFieldValueFor(String checkboxName) {
		String textFieldValue = "";

		switch (checkboxName) {
		case "Primary CTA":
			WebElement primaryCTALinkTextBox = utils.getLocator("campaignSplitPage.primaryCTALinkTextBox");
			textFieldValue = primaryCTALinkTextBox.getAttribute("value");
			break;

		case "Secondary CTA":
			WebElement secondaryCTALinkTextBox = utils.getLocator("campaignSplitPage.secondaryCTALinkTextBox");
			textFieldValue = secondaryCTALinkTextBox.getAttribute("value");
			break;

		case "Background Content":
			WebElement backgroundContentAlternateTextBox = utils
					.getLocator("campaignSplitPage.backgroundContentAlternateTextBox");
			textFieldValue = backgroundContentAlternateTextBox.getAttribute("value");
			break;

		case "Meta":
			WebElement metaRank = utils.getLocator("campaignSplitPage.metaRank");
			textFieldValue = metaRank.getAttribute("value");
			break;

		default:
			logger.info("No matching text field provided: " + checkboxName);
			TestListeners.extentTest.get().info("No matching text field provided: " + checkboxName);
		}

		logger.info("Text field value for " + checkboxName + ": " + textFieldValue);
		TestListeners.extentTest.get().pass("Text field value for " + checkboxName + ": " + textFieldValue);
		return textFieldValue;
	}

	public List<String> getCampaignTypeOnNewCHP() {
		List<WebElement> typeList = driver.findElements(By.xpath("//tbody//tr//td[5]//p"));
		List<String> originalList = typeList.stream().map(s -> s.getText()).distinct().collect(Collectors.toList());
		return originalList;
	}

	public boolean splitPanelVerifyNotGiftRedeemable() {
		boolean status = false;
		try {
			utils.longWaitInSeconds(2);
			WebElement splitboxlocator = utils.getLocator("campaignSplitPage.splittestbox");
			status = utils.checkElementPresent(splitboxlocator);
		} catch (Exception e) {
		}
		selUtils.implicitWait(5);
		return status;

	}

	public String checkSplitSidePanelData() {
		utils.longWaitInSeconds(3);
		String list = "";
		List<WebElement> sidePanelElementList = utils.getLocatorList("campaignSplitPage.splitSidePanelElements");
		for (WebElement element : sidePanelElementList) {
			list += element.getText() + " ";
		}
		logger.info(" Labels on Split Campaign Side Panel: " + list);
		TestListeners.extentTest.get().pass(" Labels on Split Campaign Side Panel: " + list);
		return list;
	}

	public void clickVariantASectionOnSplitSidePanel() {
		utils.longWaitInSeconds(1);
		WebElement variantASection = utils.getLocator("campaignSplitPage.splitSidePanelVariantASection");
		variantASection.click();
		logger.info(" Clicked on Variant A section of split campaign side panel ");
		TestListeners.extentTest.get().pass(" Clicked on Variant A section of split campaign side panel ");
	}

	public void clickVariantBSectionOnSplitSidePanel() {
		utils.longWaitInSeconds(1);
		WebElement variantBSection = utils.getLocator("campaignSplitPage.splitSidePanelVariantBSection");
		variantBSection.click();
		logger.info(" Clicked on Variant B section of split campaign side panel ");
		TestListeners.extentTest.get().pass(" Clicked on Variant B section of split campaign side panel ");
	}

	public void splitDuplicateContentFromVarAToVarB(String variant, String checkBoxName) {
		String checkboxXpath = utils.getLocatorValue("campaignSplitPage.splitVariantCheckBox")
				.replace("$variant", variant).replace("$temp", checkBoxName);
		driver.findElement(By.xpath(checkboxXpath)).click();
		String threeDotsXpath = utils.getLocatorValue("campaignSplitPage.splitVariantBThreeDotsIconDynamic")
				.replace("$variant", variant).replace("$temp", checkBoxName);
		driver.findElement(By.xpath(threeDotsXpath)).click();
		// click on Duplicate from variant A button
		WebElement variantBDuplicateVarABtn = utils.getLocator("campaignSplitPage.splitDuplicateFromVariantABtn");
		variantBDuplicateVarABtn.click();

	}

	public List<String> validateDataUsingDuplicateFromVariantAOption() {

		List<String> variantBComChannelData = new ArrayList<String>();
		List<WebElement> splitVariantBSection = utils.getLocatorList("campaignSplitPage.splitVariantBSectionData");
		int col = splitVariantBSection.size();
		for (int i = 0; i < col; i++) {
			String val = splitVariantBSection.get(i).getText();
			variantBComChannelData.add(val);
		}
		return variantBComChannelData;
	}

	public String getAllElementsTextOnSplitCamSummaryPage() {
		utils.longWaitInSeconds(1);
		String list = "";
		List<WebElement> summaryPageElementList = utils
				.getLocatorList("campaignSplitPage.splitCampaignSummaryPageElements");
		for (WebElement element : summaryPageElementList) {
			list += element.getText() + " ";
		}
		logger.info(" Elements on Split Campaign Summary Page: " + list);
		TestListeners.extentTest.get().pass(" Elements on Split Campaign Summary Page: " + list);
		return list;
	}

	public String getErrorLabelSplitPercent() {
		WebElement errorLabelSplitPercentIsNot100 = utils.getLocator("campaignSplitPage.errorLabelForSplitPercentage");
		String labelText = errorLabelSplitPercentIsNot100.getText();
		return labelText;
	}

	public boolean isSplitPreviewEditorOptionVisible() {
		boolean status = false;
		try {
			utils.longWaitInSeconds(2);
			WebElement ele = utils.getLocator("campaignSplitPage.previewEditorOptionsplit");
			status = utils.checkElementPresent(ele);
		} catch (Exception e) {
		}
		selUtils.implicitWait(5);
		return status;
	}

	public void clickThreeDotsIconForVariants(String variant, String checkBoxName) {
		String threeDotsXpath = utils.getLocatorValue("campaignSplitPage.splitVariantBThreeDotsIconDynamic")
				.replace("$variant", variant).replace("$temp", checkBoxName);
		driver.findElement(By.xpath(threeDotsXpath)).click();

	}

	public void clickGotItBtn() {
		utils.longWaitInSeconds(1);
		WebElement button = utils.getLocator("campaignSplitPage.gotItBtn");
		button.click();
		logger.info("Clicked on Got it button ");
		TestListeners.extentTest.get().pass("Clicked on Got it button ");
	}

	public void clickOnBackBtn() {
		utils.longWaitInSeconds(1);
		WebElement button = utils.getLocator("campaignSplitPage.splitBackButton");
		button.click();
		logger.info("Clicked on Back button ");
		TestListeners.extentTest.get().pass("Clicked on Back button ");
	}
}