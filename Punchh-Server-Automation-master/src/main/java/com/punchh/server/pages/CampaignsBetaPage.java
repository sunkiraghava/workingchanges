package com.punchh.server.pages;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class CampaignsBetaPage {

	static Logger logger = LogManager.getLogger(MenuPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	CreateDateTime createDateTime;
	SignupCampaignPage signupCampaignPage;
	private PageObj pageObj;

	public CampaignsBetaPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		createDateTime = new CreateDateTime();
		signupCampaignPage = new SignupCampaignPage(driver);
		pageObj = new PageObj(driver);
	}

	public void clickNewCampaignBtn() {
//		WebElement newCampBtn = utils.getLocator("campaignsBetaPage.newCampaignBtn");
//		newCampBtn.click();

		WebElement newCampBtn = utils.getLocator("campaignsPage.newBetaCampaignBtn");
		newCampBtn.click();
//		 utils.clickByJSExecutor(driver, newCampBtn);
		logger.info("Clicked new campaign button");
	}

	public void SetCampaignName(String name) {
		pageObj.newCamHomePage().campaignAdvertiseBlock();
		utils.getLocator("campaignsBetaPage.nameBox").clear();
		utils.getLocator("campaignsBetaPage.nameBox").sendKeys(name);
		logger.info("Entered campaign name :" + name);
		utils.longWaitInSeconds(1);
		utils.getLocator("campaignsBetaPage.descriptionBox").clear();
		utils.getLocator("campaignsBetaPage.descriptionBox").sendKeys("Test Description");
		logger.info("Entered campaign description");

	}

	public void setCampaignType(String campaignType) {
		if (campaignType.equalsIgnoreCase("message")) {
			utils.getLocator("campaignsBetaPage.messageGuest").click();
		} else {
			// For send offer code
		}
	}

	public boolean messageGuest() {
		boolean status = false;
		try {
			utils.implicitWait(1);
			WebElement msgGuestBtn = utils.getLocator("campaignsBetaPage.messageGuest");
			status = utils.checkElementPresent(msgGuestBtn);
		} catch (NoSuchElementException e) {

		}
		utils.implicitWait(60);
		return status;
	}

	public boolean sendOffer() {
		boolean status = false;
		try {
			utils.implicitWait(1);
			WebElement sendOfferBtn = utils.getLocator("campaignsBetaPage.sendOffer");
			status = utils.checkElementPresent(sendOfferBtn);
		} catch (NoSuchElementException e) {

		}
		utils.implicitWait(60);
		return status;
	}

	public void clickContinueBtn() throws InterruptedException {
		utils.longWaitInSeconds(4);
		utils.StaleElementclick(driver, utils.getLocator("campaignsBetaPage.continueBtn"));
		logger.info("clicked continue button");
		Thread.sleep(5000);
	}

	public void clickCancelBtn() {
		utils.getLocator("campaignsBetaPage.cancelBtn").click();
		// utils.StaleElementclick(driver,
		// utils.getLocator("campaignsBetaPage.cancelBtn"));
		logger.info("clicked cancel button");
	}

	public void clickYesDeleteBtn() {
		// utils.getLocator("campaignsBetaPage.cancelBtn").click();
		utils.StaleElementclick(driver, utils.getLocator("campaignsBetaPage.yesDeleteButton"));
		logger.info("clicked cancel button");
		utils.waitTillPagePaceDone();
	}

	@SuppressWarnings("static-access")
	public void clickSummaryTab() throws InterruptedException {
		Thread.sleep(5000);
		WebElement summaryTab = utils.getLocator("campaignsBetaPage.summaryTab");
		utils.scrollToElement(driver, summaryTab);
		utils.clickByJSExecutor(driver, summaryTab);
		logger.info("clicked summary tab");
	}

	@SuppressWarnings("static-access")
	public void clickWhoTab() {
		WebElement whoTab = utils.getLocator("campaignsBetaPage.whoTab");
		utils.StaleElementclick(driver, whoTab);
		logger.info("clicked who tab");
	}

	@SuppressWarnings("static-access")
	public void clickWhatTab() {
		WebElement whatTab = utils.getLocator("campaignsBetaPage.whatTab");
		utils.StaleElementclick(driver, whatTab);
		logger.info("clicked what tab");
	}

	@SuppressWarnings("static-access")
	public void clickWhenTab() throws InterruptedException {
		Thread.sleep(5000);
		WebElement whenTab = utils.getLocator("campaignsBetaPage.whenTab");
		utils.StaleElementclick(driver, whenTab);
		logger.info("clicked when tab");
	}

	public void clickSegmentBtn() throws InterruptedException {
		selUtils.longWait(15000);
		pageObj.newCamHomePage().campaignAdvertiseBlock();
		WebElement ele = utils.getLocator("campaignsBetaPage.segmentBtn");
		utils.clickByJSExecutor(driver, ele);
//		selUtils.waitTillElementToBeClickable(utils.getLocator("campaignsBetaPage.segmentBtn"));
//		utils.getLocator("campaignsBetaPage.segmentBtn").click();
//		utils.clickWithActions(utils.getLocator("campaignsBetaPage.segmentBtn"));
//		Thread.sleep(2000);
		logger.info("clicked segment button");
	}

	public void clickLocationBtn() {
		utils.getLocator("campaignsBetaPage.locationBtn").click();
		logger.info("clicked location button");
	}

	@SuppressWarnings("static-access")
	public void clickNextBtn() {
		WebElement nextBtn = utils.getLocator("campaignsBetaPage.nextBtn");
		utils.scrollToElement(driver, nextBtn);
		utils.clickByJSExecutor(driver, nextBtn);
		// utils.clickUsingActionsClass(nextBtn);
		logger.info("clicked next button");
		utils.waitTillPagePaceDone();
	}

	public void setSegmentType(String segmentName) throws InterruptedException {
		utils.longWaitInSeconds(2);
		pageObj.newCamHomePage().campaignAdvertiseBlock();
		utils.waitTillElementToBeClickable(utils.getLocator("campaignsBetaPage.segmentBtn"));
		utils.clickByJSExecutor(driver, utils.getLocator("campaignsBetaPage.segmentBtn"));
		utils.getLocator("campaignsBetaPage.segmentDrp").click();
		utils.clickByJSExecutor(driver, utils.getLocator("campaignsBetaPage.searchBox"));
		utils.getLocator("campaignsBetaPage.searchBox").clear();
		utils.getLocator("campaignsBetaPage.searchBox").sendKeys(segmentName);
		utils.longWaitInSeconds(2);
		utils.getLocator("campaignsBetaPage.searchedOption").click();
		TestListeners.extentTest.get().pass("Segment Selected");
		utils.waitTillPagePaceDone();
		logger.info("Selected segment as: " + segmentName);
	}

	public void setSegmentType1(String segmentName) throws InterruptedException {
		selUtils.longWait(6000);
//		utils.clickByJSExecutor(driver, utils.getLocator("campaignsBetaPage.segmentBtn"));
		utils.getLocator("campaignsBetaPage.segmentBtn1").click();
		utils.getLocator("campaignsBetaPage.segmentDrp").click();
//		Thread.sleep(12000);
		utils.clickByJSExecutor(driver, utils.getLocator("campaignsBetaPage.searchBox"));
		utils.getLocator("campaignsBetaPage.searchBox").clear();
		utils.getLocator("campaignsBetaPage.searchBox").sendKeys(segmentName);
		utils.getLocator("campaignsBetaPage.searchedOption").click();
//		Thread.sleep(2000);
		// utils.clickByJSExecutor(driver,
		// utils.getLocator("campaignsBetaPage.searchedOption"));
		// utils.getLocator("campaignsBetaPage.searchedOption").click();
		logger.info("Selected segment as: " + segmentName);
	}

	public void setLocationType(String locationName) throws InterruptedException {
		utils.getLocator("campaignsBetaPage.locationBtn").click();
		utils.getLocator("campaignsBetaPage.locationDrp").click();
		Thread.sleep(10000);

		utils.getLocator("campaignsBetaPage.searchBox").clear();
		utils.getLocator("campaignsBetaPage.searchBox").sendKeys(locationName);
		utils.getLocator("campaignsBetaPage.searchedOption").click();
		logger.info("Selected segment as: " + locationName);
	}

	public void setRedeemable(String redeemableName) throws InterruptedException {
		WebElement whoTab = utils.getLocator("campaignsBetaPage.whoTab");
		utils.scrollToElement(driver, whoTab);
		utils.waitTillPagePaceDone();
		utils.getLocator("campaignsBetaPage.redeemableDrp").click();
		// Thread.sleep(15000);
		utils.getLocator("campaignsBetaPage.searchBox").clear();
		utils.getLocator("campaignsBetaPage.searchBox").sendKeys(redeemableName);
		utils.getLocator("campaignsBetaPage.searchedOption").click();
		logger.info("Selected redeemable as: " + redeemableName);
		TestListeners.extentTest.get().pass("Redeemable Selected");
	}

	public void setOfferReason(String reason) {
		utils.getLocator("campaignsBetaPage.offerReason").clear();
		utils.getLocator("campaignsBetaPage.offerReason").sendKeys(reason);
	}

	public void setPushNotification(String title, String body) {
		utils.getLocator("campaignsBetaPage.pushOption").click();
		utils.getLocator("campaignsBetaPage.toggleSlider").click();
		utils.getLocator("campaignsBetaPage.pnTitle").clear();
		utils.getLocator("campaignsBetaPage.pnTitle").sendKeys(title);
		utils.getLocator("campaignsBetaPage.pnBody").sendKeys(body);
		logger.info("push notification entered");
		TestListeners.extentTest.get().pass("Push notification entered");
	}

	public void setEmailNotification(String title, String body) {
		selUtils.waitTillElementToBeClickable(utils.getLocator("campaignsBetaPage.emailOption"));
		utils.getLocator("campaignsBetaPage.emailOption").click();
		utils.getLocator("campaignsBetaPage.toggleSlider").click();
		utils.getLocator("campaignsBetaPage.emailSubjectLine").clear();
		utils.getLocator("campaignsBetaPage.emailSubjectLine").sendKeys(title);
		utils.getLocator("campaignsBetaPage.emailBody").clear();
		utils.getLocator("campaignsBetaPage.emailBody").sendKeys(body);
		utils.longWaitInMiliSeconds(500);
		logger.info("email notification entered");
		TestListeners.extentTest.get().pass("Email notification entered");
	}

	// @SuppressWarnings("static-access")
	public void setEmailNotificationWithEmailEditor(String title) throws InterruptedException {
		utils.getLocator("campaignsBetaPage.emailOption").click();
		utils.getLocator("campaignsBetaPage.toggleSlider").click();
		utils.getLocator("campaignsBetaPage.emailEditor").click();
		utils.StaleElementclick(driver, utils.getLocator("campaignsBetaPage.chooseTemplate"));
		utils.longWaitInSeconds(60);
		// utils.getLocator("campaignsBetaPage.chooseTemplate").click();
		utils.waitTillElementToBeClickable(utils.getLocator("campaignsBetaPage.saveCloseBtn"));
		utils.clickByJSExecutor(driver, utils.getLocator("campaignsBetaPage.saveCloseBtn"));
		// utils.clickUsingActionsClass(utils.getLocator("campaignsBetaPage.saveCloseBtn"));
		utils.longWaitInSeconds(8);

		utils.getLocator("campaignsBetaPage.emailSubjectLine").clear();
		utils.getLocator("campaignsBetaPage.emailSubjectLine").sendKeys(title);
		utils.longWaitInMiliSeconds(500);
		try {
			if (utils.getLocator("campaignsBetaPage.cardOverlay").isDisplayed()) {
				logger.info("email notification with email editor entered");
				TestListeners.extentTest.get().info("email notification with email editor entered");
			}
		} catch (Exception e) {
			logger.info("error in entering email notification" + e);
			TestListeners.extentTest.get().info("error in entering email notification" + e);
		}
		/*
		 * utils.getLocator("campaignsBetaPage.emailSubjectLine").clear();
		 * utils.getLocator("campaignsBetaPage.emailSubjectLine").sendKeys(title);
		 */

	}

	public void setStartDate() throws InterruptedException {

		String fullDate = CreateDateTime.getCurrentDate();
		String date[] = fullDate.split("-");
		String currentDate = date[2];
		int i = Integer.parseInt(currentDate);
		int newDate = i + 1;
		if (newDate >= 30) {
			newDate = 1;
		}
		String nextdayDate = Integer.toString(newDate);

		utils.StaleElementclick(driver, utils.getLocator("campaignsBetaPage.startDate"));
		// utils.StaleElementclick(driver,
		// driver.findElement(By.xpath("//div[@class='vc-arrow is-right']")));
		// utils.getLocator("campaignsBetaPage.startDate").click();
		Utilities.longWait(2000);
		List<WebElement> ele = utils.getLocatorList("campaignsBetaPage.startDateList");
		utils.selectListDrpDwnValue(ele, nextdayDate);
		logger.info("Entered start date");
		TestListeners.extentTest.get().pass("Start date entered");
	}

	public void setEndDate() throws InterruptedException {

		String fullDate = CreateDateTime.getCurrentDate();
		String date[] = fullDate.split("-");
		String currentDate = date[2];
		int i = Integer.parseInt(currentDate);
		int newDate = i + 8;

		if (newDate >= 24) {
			newDate = 9;
		}
		String nextdayDate = Integer.toString(newDate);
		utils.StaleElementclick(driver, utils.getLocator("campaignsBetaPage.endDate"));
		Thread.sleep(2000);
		// utils.StaleElementclick(driver,
		// driver.findElement(By.xpath("//div[@class='vc-arrow is-right']")));
		utils.clickByJSExecutor(driver, driver.findElement(By.xpath("//div[@class='vc-arrow is-right']")));
		// driver.findElement(By.xpath("//div[@class='vc-arrow is-right']")).click();
		Thread.sleep(2000);
		List<WebElement> ele = utils.getLocatorList("campaignsBetaPage.endDateList");
		utils.selectListDrpDwnValue(ele, nextdayDate);
		logger.info("Entered end date");

		// to close calendar
		WebElement elem = utils.getLocator("campaignsBetaPage.startTimeBox");
		utils.StaleElementclick(driver, elem);
		utils.longWaitInSeconds(1);
		TestListeners.extentTest.get().info("calendar box closed ...");
	}

	public void setRecommendedSendDate() {
		utils.waitTillElementToBeClickable(utils.getLocator("campaignsBetaPage.recommendedSendDate"));
		utils.clickByJSExecutor(driver, utils.getLocator("campaignsBetaPage.recommendedSendDate"));
		TestListeners.extentTest.get().pass("STO Time option checked successfully");
	}

	public boolean checkSTOPresence() {
		boolean status = false;
		try {
			utils.implicitWait(3);
			WebElement ele = utils.getLocator("campaignsBetaPage.recommendedSendDate");
			status = utils.checkElementPresent(ele);
		} catch (Exception e) {

		}
		utils.implicitWait(50);
		return status;
	}

	public void setRepeatOn() {
		utils.getLocator("campaignsBetaPage.repeatOn").click();
	}

	public List<String> CheckSummary() {
		// utils.waitTillPagePaceDone();
		pageObj.newCamHomePage().campaignAdvertiseBlock();
		List<String> summaryData = new ArrayList<String>();
		List<WebElement> summaryTable = utils.getLocatorList("campaignsBetaPage.summaryList");
		int col = summaryTable.size();
		for (int i = 0; i < col; i++) {
			String val = summaryTable.get(i).getText();
			summaryData.add(val);
		}
		return summaryData;
	}
	
	public boolean verifyNewCamBetaBtnPresent() {
		selUtils.implicitWait(2);
		boolean isPresent = false;
		try {
			WebElement newCamBetaCreateButton = utils.getLocator("campaignsBetaPage.newCampaignBetaBtn");
			isPresent = utils.checkElementPresent(newCamBetaCreateButton);
		} catch (Exception e) {
			// e.printStackTrace();
		}
		return isPresent;

	}
	
	public boolean verifyDuplicateIconPresentForBetaCamHomePage() {
		selUtils.implicitWait(2);
		boolean isPresent = false;
		try {
			WebElement betaCamDuplicateIcon = utils.getLocator("campaignsBetaPage.duplicateIconHomePage");
			isPresent = utils.checkElementPresent(betaCamDuplicateIcon);
		} catch (Exception e) {
			// e.printStackTrace();
		}
		return isPresent;

	}
	
	public boolean verifyDuplicateOptPresentForBetaCamBuilderSummPage() {
		selUtils.implicitWait(2);
		boolean var = false;
		try {
			WebElement betaCamDuplicateOption = utils.getLocator("campaignsBetaPage.duplicateOptionDropdown");
			var = utils.checkElementPresent(betaCamDuplicateOption);
		} catch (Exception e) {
			// e.printStackTrace();
		}
		return var;

	}

	public void clickSaveAndCloseBtn() {
		utils.getLocator("campaignsBetaPage.saveAndCloseBtn").click();
	}

	public void clickSubmitForApprovalBtn() throws InterruptedException {
		utils.longWaitInSeconds(1);
		utils.clickByJSExecutor(driver, utils.getLocator("campaignsBetaPage.submitforApprovalBtn"));
//		utils.getLocator("campaignsBetaPage.submitforApprovalBtn").click();
	}

	public void clickSubmitForApprovalBtn1() {
		utils.clickByJSExecutor(driver, utils.getLocator("campaignsBetaPage.submitforApprovalBtn1"));
//		utils.getLocator("campaignsBetaPage.submitforApprovalBtn1").click();
	}

	public void clickActivateCampaignBtn() {
		utils.StaleElementclick(driver, utils.getLocator("campaignsBetaPage.activateCampaignBtn"));
		logger.info("Clicked activate campaign button");
		utils.longWaitInSeconds(1);
	}

	public String searchCampaign(String name) throws InterruptedException {
		Thread.sleep(5000);
//		utils.getLocator("campaignsBetaPage.searchBox").clear();
//		utils.getLocator("campaignsBetaPage.searchBox").sendKeys(name);
//		utils.getLocator("campaignsBetaPage.searchBox").sendKeys(Keys.ENTER);
//		String val = utils.getLocator("campaignsBetaPage.searchedCampaign").getText();
//		return val;

		utils.getLocator("campaignsPage.searchBox").clear();
		utils.getLocator("campaignsPage.searchBox").sendKeys(name);
		utils.getLocator("campaignsPage.searchBox").sendKeys(Keys.ENTER);
		String val = utils.getLocator("campaignsBetaPage.searchedCampaign").getText();
		return val;
	}

	public String getCampaignStatus() {
		// String val =
		// utils.getLocator("campaignsBetaPage.StatusCol").getText().trim();
		utils.waitTillPagePaceDone();
		String val = utils.getLocator("campaignsPage.StatusCol").getText().trim();
		return val;
	}

	public List<String> filterStatus(String option) throws InterruptedException {
		Thread.sleep(7000);
		utils.getLocator("campaignsBetaPage.filterBtn").click();
		utils.getLocator("campaignsBetaPage.clearAllBtn").click();
		utils.getLocator("campaignsBetaPage.statusBtn").click();
		List<WebElement> ele = utils.getLocatorList("campaignsBetaPage.statusBtnList");
		utils.selecDrpDwnValue(ele, option);

		utils.getLocator("campaignsBetaPage.applyBtn").click();
		utils.getLocator("campaignsBetaPage.cancleBtn").click();

		List<String> statusData = new ArrayList<String>();
		List<WebElement> tablecol = utils.getLocatorList("campaignsBetaPage.StatusCol");
		int col = tablecol.size();
		for (int i = 0; i < col; i++) {
			String val = tablecol.get(i).getText();
			statusData.add(val);
		}
		return statusData;

	}

	public boolean verifyAllEqual(List<String> list) {
		return list.isEmpty() || list.stream().allMatch(list.get(0)::equals);
	}

	public String validateSuccessMessage() throws InterruptedException {
		utils.longWaitInSeconds(1);
		String val = "";
		WebElement successMsg = utils.getLocator("campaignsBetaPage.successAlert");
		if (successMsg.isDisplayed()) {
			val = successMsg.getText();
		}
		return val;
	}

	public String validateErrorMessage() {

		String val = "";
		WebElement successMsg = utils.getLocator("campaignsBetaPage.errorAlert");
		if (successMsg.isDisplayed()) {
			val = successMsg.getText();

		}
		return val;
	}

	public String validateTooltip() throws InterruptedException {
		Thread.sleep(2000);
		utils.mouseHover(driver, utils.getLocator("campaignsBetaPage.infoIcon"));
		String value = utils.getLocator("campaignsBetaPage.toolTipBox").getText();
		return value;

	}

	public void clickCampaignName() {
		utils.getLocator("campaignsBetaPage.campaignNameLink").click();
		logger.info("Clicked searched campaign campaign");
	}

	public void gotoWhoTab() {
		selUtils.longWait(4000);
		pageObj.newCamHomePage().campaignAdvertiseBlock();
		utils.waitTillInVisibilityOfElement(utils.getLocator("campaignsBetaPage.whoTab"), "Who Tab");
		utils.getLocator("campaignsBetaPage.whoTab").click();
		logger.info("Clicked Who Tab");
	}

	public void selectCampaign() {
		utils.getLocator("campaignsBetaPage.campaignNameLink").click();
		logger.info("Campaign selected");
	}

	public boolean checkElementState() {
		WebElement ele = utils.getLocator("campaignsBetaPage.submitforApprovalBtn");
		return ele.isEnabled();

	}

	public void approveCampaign() throws InterruptedException {
		WebElement ele = utils.getLocator("campaignsBetaPage.approveCampaignBtn");
		utils.clickByJSExecutor(driver, ele);
		Thread.sleep(2000);
//		selUtils.waitTillElementToBeClickable(utils.getLocator("campaignsBetaPage.commentBox"));
		utils.getLocator("campaignsBetaPage.commentBox").sendKeys("Test approving campaign");
		pageObj.newCamHomePage().campaignAdvertiseBlock();
		utils.getLocator("campaignsBetaPage.commentBoxApproveBtn").click();
	}

	public void rejectCampaign() throws InterruptedException {
		utils.getLocator("campaignsBetaPage.rejectCampaignBtn").click();
//		Thread.sleep(2000);
		utils.getLocator("campaignsBetaPage.commentBox").clear();
		utils.getLocator("campaignsBetaPage.commentBox").sendKeys("Test rejecting campaign");
		Thread.sleep(2000);
//		selUtils.waitTillElementToBeClickable(utils.getLocator("campaignsBetaPage.commentBoxRejectBtn"));
		utils.clickByJSExecutor(driver, utils.getLocator("campaignsBetaPage.commentBoxRejectBtn"));

	}

	/*
	 * public void logoutApp() throws InterruptedException { //
	 * utils.getLocator("campaignsBetaPage.imgLogo").click();
	 * utils.waitTillElementToBeClickable(utils.getLocator(
	 * "campaignsBetaPage.imgLogo")); utils.StaleElementclick(driver,
	 * utils.getLocator("campaignsBetaPage.imgLogo")); Thread.sleep(2000);
	 * utils.StaleElementclick(driver,
	 * utils.getLocator("campaignsBetaPage.logoutLink")); //
	 * utils.getLocator("campaignsBetaPage.logoutLink").click();
	 * logger.info("logged out successfully"); }
	 */

	public String elementFieldErrorMessage() {

		String val = "";
		WebElement successMsg = utils.getLocator("campaignsBetaPage.fieldErrorText");
		if (successMsg.isDisplayed()) {
			val = successMsg.getText();

		}
		return val;
	}

	public String givenCampaignName() {
		String val = utils.getLocator("campaignsBetaPage.campaignGivenName").getText();
		return val;

	}

	public String whoTitle() throws InterruptedException {
		Thread.sleep(4000);
		String val = utils.getLocator("campaignsBetaPage.whoTitle").getText();
		return val;

	}

	public String whatTitle() {
		String val = utils.getLocator("campaignsBetaPage.whatTitle").getText();
		return val;

	}

	public String whenTitle() {
		String val = utils.getLocator("campaignsBetaPage.whenTitle").getText();
		return val;

	}

	public String summaryTitle() {
		String val = utils.getLocator("campaignsBetaPage.summaryTitle").getText();
		return val;

	}

	public boolean givenCampaigninfoLabel() {
		return utils.getLocator("campaignsBetaPage.infoLabel").isDisplayed();

	}

	public boolean redeemableImage() {
		return utils.getLocator("campaignsBetaPage.redeemableImage").isDisplayed();

	}

	public void validateReachablityandControlGroup() {
		try {
			WebElement reachability = utils.getLocator("campaignsBetaPage.reachability");
			boolean status = utils.checkElementPresent(reachability);
			Assert.assertTrue(status);

			WebElement ele = driver.findElement(By.xpath("//p[contains(@class,'loadingText')]"));
			utils.waitTillElementDisappear(ele);
			WebElement reachabilityEmail = utils.getLocator("campaignsBetaPage.reachabilityEmail");
			// utils.waitTillVisibilityOfElement(reachabilityEmail, "Reachability Email");
			boolean status1 = utils.checkElementPresent(reachabilityEmail);
			Assert.assertTrue(status1);

			WebElement reachabilityPN = utils.getLocator("campaignsBetaPage.reachabilityPN");
			boolean status2 = utils.checkElementPresent(reachabilityPN);
			Assert.assertTrue(status2);

			WebElement reachabilitySMS = utils.getLocator("campaignsBetaPage.reachabilitySMS");
			boolean status3 = utils.checkElementPresent(reachabilitySMS);
			Assert.assertTrue(status3);

			WebElement controlGroup = utils.getLocator("campaignsBetaPage.controlGroup");
			boolean status4 = utils.checkElementPresent(controlGroup);
			Assert.assertTrue(status4);

			WebElement noControlGroup = utils.getLocator("campaignsBetaPage.noControlGroup");
			boolean status5 = utils.checkElementPresent(noControlGroup);
			Assert.assertTrue(status5);

		} catch (Exception e) {
			logger.info("Error in validating Reachability and Control Group" + e);
			TestListeners.extentTest.get().info("Error in validating Reachability and Control Group" + e);
		}
	}

	public void clickCurrencyBtn() {
		utils.longWaitInSeconds(2);
		utils.waitTillElementToBeClickable(utils.getLocator("campaignsBetaPage.currencyBtn"));
		utils.clickByJSExecutor(driver, utils.getLocator("campaignsBetaPage.currencyBtn"));
		// utils.getLocator("campaignsBetaPage.currencyBtn").click();
		logger.info("clicked currency button");
	}

	public void clickCouponBtn() {
		utils.getLocator("campaignsBetaPage.couponBtn").click();
		logger.info("clicked coupon button");
	}

	public void clickPointsBtn() {
		utils.waitTillElementToBeClickable(utils.getLocator("campaignsBetaPage.pointsBtn"));
		utils.clickByJSExecutor(driver, utils.getLocator("campaignsBetaPage.pointsBtn"));
		// utils.getLocator("campaignsBetaPage.pointsBtn").click();
		logger.info("clicked points button");
	}

	public void clickRedeemablesBtn() {
		utils.waitTillElementToBeClickable(utils.getLocator("campaignsBetaPage.redeemablesBtn"));
		utils.clickByJSExecutor(driver, utils.getLocator("campaignsBetaPage.redeemablesBtn"));
		// utils.getLocator("campaignsBetaPage.redeemablesBtn").click();
		logger.info("clicked redeemables button");
	}

	public void clickIncludeSurvey() {
		utils.getLocator("campaignsBetaPage.includeSurvey").click();
		logger.info("clicked includeSurvey checkbox");
	}

	public void clickIncludeAdvertisedCampaign() {
		utils.getLocator("campaignsBetaPage.includeAdvertisedCampaign").click();
		logger.info("clicked includeAdvertisedCampaign checkbox");
	}

	public void clickAttachaCoupontoMessageCampaign() {
		utils.getLocator("campaignsBetaPage.attachaCoupon").click();
		logger.info("clicked attach a coupon checkbox");
	}

	public String emailMessageFieldErrorMessage() {
		String val = "";
		utils.getLocator("campaignsBetaPage.emailOption").click();
		utils.getLocator("campaignsBetaPage.toggleSlider").click();
		WebElement successMsg = utils.getLocator("campaignsBetaPage.fieldErrorText");
		if (successMsg.isDisplayed()) {
			val = successMsg.getText();

		}
		utils.getLocator("campaignsBetaPage.toggleSlider").click();
		return val;
	}

	public String pnMessageFieldErrorMessage() {
		String val = "";
		utils.getLocator("campaignsBetaPage.pushOption").click();
		utils.getLocator("campaignsBetaPage.toggleSlider").click();
		WebElement successMsg = utils.getLocator("campaignsBetaPage.fieldErrorText");
		if (successMsg.isDisplayed()) {
			val = successMsg.getText();

		}
		utils.getLocator("campaignsBetaPage.toggleSlider").click();
		return val;
	}

	public String smsMessageFieldErrorMessage() {
		String val = "";
		utils.getLocator("campaignsBetaPage.smsOption").click();
		utils.getLocator("campaignsBetaPage.toggleSlider").click();
		WebElement successMsg = utils.getLocator("campaignsBetaPage.fieldErrorText");
		if (successMsg.isDisplayed()) {
			val = successMsg.getText();

		}
		utils.getLocator("campaignsBetaPage.toggleSlider").click();
		return val;
	}

	public String richMessageFieldErrorMessage() throws InterruptedException {
		String val = "";
//		Thread.sleep(7000);
		utils.getLocator("campaignsBetaPage.richMessageOption").click();
		utils.getLocator("campaignsBetaPage.toggleSlider").click();
		WebElement successMsg = utils.getLocator("campaignsBetaPage.fieldErrorText");
		if (successMsg.isDisplayed()) {
			val = successMsg.getText();

		}
		utils.getLocator("campaignsBetaPage.toggleSlider").click();
		return val;
	}

	public boolean blackoutDates() {
		return utils.getLocator("campaignsBetaPage.blackoutDates").isDisplayed();

	}

	public void setFrequency(String value) {
		utils.waitTillElementToBeClickable(utils.getLocator("campaignsBetaPage.frequencyDrp"));
		utils.getLocator("campaignsBetaPage.frequencyDrp").click();
		List<WebElement> ele = utils.getLocatorList("campaignsBetaPage.frequencyDrpOptions");
		utils.selectListDrpDwnValue(ele, value);
		TestListeners.extentTest.get().pass("Frequency selected as :" + value);
	}

	public void setTimeZone(String value) {
		utils.waitTillElementToBeClickable(utils.getLocator("campaignsBetaPage.timeZoneDrp"));
		utils.clickByJSExecutor(driver, utils.getLocator("campaignsBetaPage.timeZoneDrp"));
		// utils.getLocator("campaignsBetaPage.timeZoneDrp").click();
		List<WebElement> ele = utils.getLocatorList("campaignsBetaPage.timeZoneDrpOptions");
		utils.selectListDrpDwnValue(ele, value);
		utils.longWaitInSeconds(2);
	}

	public List<String> getListofErrors() {
		List<String> errorData = new ArrayList<String>();
		List<WebElement> eles = utils.getLocatorList("campaignsBetaPage.fieldErrorText");
		int col = eles.size();
		for (int i = 0; i < col; i++) {
			String val = eles.get(i).getText();
			errorData.add(val);
		}
		return errorData;

	}

	public boolean campaignPriorty() {
		utils.getLocator("campaignsBetaPage.campaignPriorty").click();
		return utils.getLocator("campaignsBetaPage.priortyDrp").isDisplayed();
	}

	public void goToAuditLogs() {
		utils.getLocator("campaignsBetaPage.popOverlistDots").click();
		utils.getLocator("campaignsBetaPage.auditLogsBtn").click();
		utils.getLocator("campaignsBetaPage.submitBtn").click();

	}
	
	public void clickPopOverListDots() {
		
		utils.getLocator("campaignsBetaPage.popOverlistDots").click();
	}

	public boolean checkAuditLogs() {
		return utils.getLocator("campaignsBetaPage.auditLogsHeader").isDisplayed();
	}

	public String getAlertmsg() {
		return utils.getLocator("campaignsBetaPage.alertMsg").getText();
	}

	public void clickOptionsLink() {
		utils.getLocator("campaignsBetaPage.optionsLinkclassic").click();
	}

	public String exportReport() {
		utils.getLocator("campaignsBetaPage.popOverlistDots").click();
		utils.getLocator("campaignsBetaPage.exportReport").click();
		utils.getLocator("campaignsBetaPage.submitBtn").click();
		return utils.getLocator("campaignsPage.successAlert").getText();

	}

	public void selectdotOptionsValue(String value) {
		utils.waitTillVisibilityOfElement(utils.getLocator("campaignsBetaPage.popOverlistDots"), "pop over list dots");
		utils.getLocator("campaignsBetaPage.popOverlistDots").click();
		List<WebElement> ele = utils.getLocatorList("campaignsBetaPage.dotOptions");
		utils.selectListDrpDwnValue(ele, value);
	}

	public void SelectClassicCSPOptionsEditDelete(String value) {
		utils.waitTillPagePaceDone();
		WebElement newOptionList = driver.findElement(By.xpath("//span[text()='Options']"));
		newOptionList.click();
		driver.findElement(By.xpath("//div[contains(@title,'" + value + "')]")).click();
		utils.waitTillPagePaceDone();
	}

	public void cspOptinsDelete() {
		utils.getLocator("campaignsBetaPage.optionsLink").click();
		utils.getLocator("campaignsBetaPage.deleteOption").click();
	}

	public String getPopUpValues() {
		return utils.getLocator("campaignsBetaPage.popupContainer").getText().trim();
	}

	public void clickSubmitBtn() {
		utils.getLocator("campaignsBetaPage.submitBtn").click();
	}

	public void clickDeleteBtn() {
		utils.getLocator("campaignsBetaPage.deleteBtn").click();
	}

	public String inactiveStatus() {
		return utils.getLocator("campaignsBetaPage.titleInactive").getText();
	}

	public void clickSendTestMsgLink() {
		utils.clickByJSExecutor(driver, utils.getLocator("campaignsBetaPage.sendTestMsgLink"));
	}

	public void sendEmail(String email) throws InterruptedException {

		utils.getLocator("campaignsBetaPage.emailText").sendKeys(email);
		clickSendTestMsgBtn();
	}

	public void clickSendTestMsgBtn() {
		// utils.getLocator("campaignsBetaPage.sendTestMsgBtn").click();
		WebElement sendMsgButton = utils.getLocator("campaignsBetaPage.sendTestMsgBtn");
		utils.clickUsingActionsClass(sendMsgButton);
	}

	public void closeSendTestMsgBtn() {
		utils.clickByJSExecutor(driver, utils.getLocator("campaignsBetaPage.closeTestMsgBtn"));
	}

	public boolean emailsent() {
		WebElement btn = utils.getLocator("campaignsBetaPage.msgSentBtn");
		boolean status = utils.checkElementPresent(btn);
		utils.getLocator("campaignsBetaPage.sendTestMsg").click();
		return status;
	}

	public String validateNfsent() {
		WebElement nfStatus = utils.getLocator("campaignsBetaPage.nfSentMessage");
		String val = nfStatus.getText();
		utils.getLocator("campaignsBetaPage.sendTestMsgLink").click(); // to close dropdown
		return val;
	}

	public boolean emailerrorMsg() {
		WebElement msg = utils.getLocator("campaignsBetaPage.invalidEmailMsg");
		boolean status = utils.checkElementPresent(msg);
		utils.getLocator("campaignsBetaPage.sendTestMsgLink").click();
		return status;
	}

	public String maxemailMsg() {
		WebElement msg = utils.getLocator("campaignsBetaPage.maxEmailmsg");
		String status = msg.getText();
		/*
		 * WebElement ele = utils.getLocator("campaignsBetaPage.sendTestMsgButton");
		 * utils.scrollToElement(driver, ele); ele.click();
		 */
		return status;
	}

	public void setServey(String name) throws InterruptedException {

		utils.getLocator("campaignsBetaPage.surveyDrp").click();
		Thread.sleep(12000);
		utils.getLocator("campaignsBetaPage.searchBox").clear();
		utils.getLocator("campaignsBetaPage.searchBox").sendKeys(name);
		utils.clickByJSExecutor(driver, utils.getLocator("campaignsBetaPage.surveyDrpList"));
		// utils.getLocator("campaignsBetaPage.searchedOption").click();
		logger.info("Selected segment as: " + name);
	}

	public void setAdvertisedCampaign(String name) throws InterruptedException {

		utils.getLocator("campaignsBetaPage.advertisedCampaignDrp").click();
		Thread.sleep(12000);
		// utils.getLocator("campaignsBetaPage.searchBox").clear();
		// utils.getLocator("campaignsBetaPage.searchBox").sendKeys(name);
		utils.clickByJSExecutor(driver, utils.getLocator("campaignsBetaPage.advertisedCampaignDrpList"));
		// utils.getLocator("campaignsBetaPage.searchedOption").click();
		logger.info("Selected segment as: " + name);
	}

	public void setRepeat(String value) {
		utils.getLocator("campaignsBetaPage.repeatDrp").click();
		List<WebElement> ele = utils.getLocatorList("campaignsBetaPage.repeatDrpOptions");
		utils.selectListDrpDwnValue(ele, value);
	}

	public void setWeekOfMonth(String value) {
		utils.getLocator("campaignsBetaPage.weakMonthDrp").click();
		List<WebElement> ele = utils.getLocatorList("campaignsBetaPage.weakMonthDrpOptions");
		utils.selectListDrpDwnValue(ele, value);
	}

	public void setDayOfMonth(String value) {
		utils.getLocator("campaignsBetaPage.dayMonthDrp").click();
		List<WebElement> ele = utils.getLocatorList("campaignsBetaPage.dayMonthDrpOptions");
		utils.selectListDrpDwnValue(ele, value);
	}

	public String viewDNDDetails() {
		utils.getLocator("campaignsBetaPage.viewDetailsBtn").click();
		String dndTime = utils.getLocator("campaignsBetaPage.dndDetails").getText().trim();
		logger.info("Dnd Time  :" + dndTime);
		utils.getLocator("campaignsBetaPage.closeBtn").click();
		return dndTime;
	}

	public void setExecutionDelay() {
		utils.getLocator("campaignsBetaPage.executionDelay").clear();
		utils.getLocator("campaignsBetaPage.executionDelay").sendKeys("0");
		utils.longWaitInMiliSeconds(500);
	}

	public String validateblackoutDate() {
		return utils.getLocator("campaignsBetaPage.blackoutDate").getText();
	}

	public void clickOnSetCampaignAsTemplateCheckbox() {
		selUtils.waitTillElementToBeClickable(utils.getLocator("campaignsBetaPage.setCampaignAsTemplateCheckBox"));
		utils.getLocator("campaignsBetaPage.setCampaignAsTemplateCheckBox").click();
	}

	public boolean getTemplateIconText(String cmpName) {
		String textXpath = utils.getLocatorValue("campaignsBetaPage.templateIcon").replace("$CampaignName", cmpName);

		String templateText = utils.getXpathWebElements(By.xpath(textXpath)).getText();
		if (templateText.equalsIgnoreCase("Template")) {
			return true;
		} else {
			return false;
		}

	}

	public boolean verifySTOIsDisplayed() {
		selUtils.implicitWait(2);
		boolean var = false;
		try {
			WebElement newOptionList = utils.getLocator("campaignsBetaPage.recommendedSendDate");
			var = utils.checkElementPresent(newOptionList);
		} catch (Exception e) {
			// e.printStackTrace();
		}
		return var;

	}

	public void deleteCampFromBetaPage() {
		utils.getLocator("campaignsBetaPage.dotIcon").click();
		utils.getLocator("campaignsBetaPage.deleteTab").click();
		utils.getLocator("campaignsBetaPage.deleteDailog").click();
	}

	public boolean checkPrormoBtn() {
		boolean status = false;
		try {
			utils.implicitWait(50);
			WebElement ele = utils.getLocator("campaignsBetaPage.promoBtn");
			status = utils.checkElementPresent(ele);
		} catch (Exception e) {

		}
		return status;
	}

	public void previousBtn() {
		utils.waitTillPagePaceDone();
		utils.getLocator("campaignsBetaPage.previousStepBtn").click();
		logger.info("clicked the Previous Step button");
	}

	public void auditLogs() {
		utils.clickByJSExecutor(driver, utils.getLocator("campaignsBetaPage.auditLogs"));
	}

	public void setPoints(String str) {
		utils.waitTillPagePaceDone();
		// utils.waitTillElementToBeClickable(null);
		clickPointsBtn();
		utils.longwait(2000);
		utils.getLocator("campaignsBetaPage.points").sendKeys(str);
	}

	public void SelectClassicCSPOptionsEditDeleteNew(String value) {
		utils.waitTillPagePaceDone();
		WebElement newOptionList = driver.findElement(By.xpath("//span[text()='Options']"));
		// newOptionList.click();
		utils.waitTillElementToBeClickable(newOptionList);
		utils.clickByJSExecutor(driver, newOptionList);
		logger.info("Clicked campaign option three dots.");
		TestListeners.extentTest.get().info("Clicked campaign option three dots.");
		utils.clickByJSExecutor(driver, driver.findElement(By.xpath("//button/span[text()='" + value + "']")));
		utils.waitTillPagePaceDone();
	}

	public void setStartDateNew(int days) throws ParseException {
		String newDate = "";
		newDate = CreateDateTime.getFutureDateTimeUTC(days);
		boolean resultFalg = SignupCampaignPage.compareDatesWithLastDateOfMonth(newDate);
		String date[] = newDate.split("-");

		utils.StaleElementclick(driver, utils.getLocator("campaignsBetaPage.startDate"));
		utils.longWaitInSeconds(1);
		if (resultFalg) {
			String currentDate = "1";
			utils.waitTillVisibilityOfElement(utils.getLocator("campaignsBetaPage.nextArrowBtn"), "next arrow");
			utils.getLocator("campaignsBetaPage.nextArrowBtn").click();
			logger.info("Moved to next month");
			TestListeners.extentTest.get().info("Moved to next month");
			utils.longwait(1000);
			List<WebElement> ele = utils.getLocatorList("campaignsBetaPage.startDateListNew");
			utils.valuePresentInList(ele, currentDate);
			utils.selectListDrpDwnValue(ele, currentDate);
			logger.info("select the date- " + newDate);
			TestListeners.extentTest.get().info("select the date- " + newDate);

		} else {
			logger.info("We are on the same month, No need to move next month");
			TestListeners.extentTest.get().info("We are on the same month, No need to move next month");
			int datenum = Integer.parseInt(date[2]) + 1;

			String currentDate = String.valueOf(datenum);

			if (currentDate.charAt(0) == '0') {
				currentDate = Character.toString(currentDate.charAt(1));
			}
			List<WebElement> ele = utils.getLocatorList("campaignsBetaPage.startDateListNew");
			utils.valuePresentInList(ele, currentDate);
			utils.selectListDrpDwnValue(ele, currentDate);
			logger.info("select the date- " + newDate);
			TestListeners.extentTest.get().info("select the date- " + newDate);
		}
		// to close calendar
		WebElement ele = utils.getLocator("campaignsBetaPage.startTimeBox");
		utils.StaleElementclick(driver, ele);
		utils.longWaitInSeconds(1);
		TestListeners.extentTest.get().info("calendar box closed ...");
	}

	public void setTime(String hour, String AMorPM) {
		utils.getLocator("campaignsBetaPage.startTime").click();
		utils.waitTillVisibilityOfElement(utils.getLocator("campaignsBetaPage.startTimeWatch"), "");
		utils.selectDrpDwnValueNew(utils.getLocator("campaignsBetaPage.startTimeHour"), hour);
		String xpath = utils.getLocatorValue("campaignsBetaPage.selectPMorAM").replace("${AM/PM}", AMorPM);
		WebElement ele = driver.findElement(By.xpath(xpath));
		String attrValue = ele.getAttribute("class");
		if (attrValue.equalsIgnoreCase("active")) {
			logger.info("already selected ->" + AMorPM);
			TestListeners.extentTest.get().info("already selected ->" + AMorPM);
		} else {
			ele.click();
			logger.info("selected ->" + AMorPM);
			TestListeners.extentTest.get().info("selected ->" + AMorPM);
		}
	}

	public void setStartDateNew() throws InterruptedException {
		String fullDate = CreateDateTime.getCurrentDate();
		String date[] = fullDate.split("-");
		String currentDate = date[2];
		int i = Integer.parseInt(currentDate);
		int newDate = i + 1;

		utils.waitTillElementToBeClickable(utils.getLocator("campaignsBetaPage.startDateNew"));
		utils.StaleElementclick(driver, utils.getLocator("campaignsBetaPage.startDateNew"));
		utils.waitTillInVisibilityOfElement(utils.getLocator("campaignsBetaPage.startDateVisible"), "");
		if (newDate > 30) {
			newDate = 1;
			utils.getLocator("campaignsBetaPage.rightArrow").click();
			utils.longWaitInSeconds(1);
		}
		String nextdayDate = Integer.toString(newDate);
		List<WebElement> ele = utils.getLocatorList("campaignsBetaPage.startDateList");
		utils.selectListDrpDwnValue(ele, nextdayDate);
		logger.info("Entered start date");
		TestListeners.extentTest.get().info("Start date entered");
		utils.clickByJSExecutor(driver, utils.getLocator("campaignsBetaPage.startDateNew"));
	}

	public void setEndDate(int days) {
		String nextdayDate = CreateDateTime.getFutureDateTimeUTC(days);
		LocalDate date1 = LocalDate.parse(nextdayDate);
		String monthName = date1.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
		// boolean resultFalg = compareDatesWithLastDateOfMonth(nextdayDate);
		String date[] = nextdayDate.split("-");
		String currentDate = date[2];
		nextdayDate = nextdayDate + " 02:30 PM";
		if (currentDate.charAt(0) == '0') {
			currentDate = Character.toString(currentDate.charAt(1));
		}
		utils.getLocator("campaignsBetaPage.endDateNew").click();
		String selectedMonthName = "";
		List<WebElement> monthsSelectedList = utils.getLocatorList("campaignsBetaPage.getSelectedMonthInCalendar");
		for (WebElement weleName : monthsSelectedList) {
			try {
				selectedMonthName = weleName.getText();
			} catch (Exception exp) {
			}
		}
		if (!selectedMonthName.startsWith(monthName)) {
			List<WebElement> nextButtonList = utils.getLocatorList("campaignsBetaPage.rightArrow");
			for (WebElement wEle : nextButtonList) {
				try {
					wEle.click();
					break;
				} catch (Exception exp) {
				}
			}
			logger.info("Moved to next month");
			TestListeners.extentTest.get().pass("Moved to next month");
		} else {
			logger.info("We are on the same month, No need to move next month");
			TestListeners.extentTest.get().pass("We are on the same month, No need to move next month");

		}
		String xpathWithoutAppyBtn = utils.getLocatorValue("campaignsBetaPage.calendarDaysWithoutApplyButton")
				.replace("${CurrentDate}", currentDate);
		List<WebElement> eleList = driver.findElements(By.xpath(xpathWithoutAppyBtn));
		for (WebElement wEle : eleList) {
			try {
				wEle.click();
				logger.info(currentDate + " END Date is selected and apply button not applicable for this calendar ");
				TestListeners.extentTest.get()
						.info(currentDate + " END Date is selected and apply button not applicable for this calendar ");
				break;
			} catch (Exception exp) {
			}
		}
	}

	public String errorMsg() {
		utils.longWaitInSeconds(2);
		utils.waitTillInVisibilityOfElement(utils.getLocator("campaignsBetaPage.errorMsg"), "message");
		String str = utils.getLocator("campaignsBetaPage.errorMsg").getText();
		return str;
	}

	public boolean recommendTimeZoneDisplay() {
		if (utils.getLocatorList("campaignsBetaPage.recommendTimeZoneDisplay").size() == 1) {
			logger.info("recommend time zone is visible");
			TestListeners.extentTest.get().info("recommend time zone is visible");
			return true;
		} else {
			logger.info("recommend time zone is not visible");
			TestListeners.extentTest.get().info("recommend time zone is not visible");
			return false;
		}
	}

	public String recommendTimeZoneToolTip() {
		selUtils.mouseHoverOverElement(utils.getLocator("campaignsBetaPage.recommendToolTipDisplay"));
		utils.longWaitInSeconds(1);
		String text = utils.getLocator("campaignsBetaPage.recommendTimeZoneToolTip").getText();
		logger.info("tool tip for the recommend time zone is -- " + text);
		TestListeners.extentTest.get().info("tool tip for the recommend time zone is -- " + text);
		return text;
	}

	public boolean checkSTOfeaturePresence() {
		boolean status = false;
		try {
			utils.implicitWait(3);
			WebElement ele = utils.getLocator("campaignsBetaPage.recommendedSendTime");
			status = utils.checkElementPresent(ele);
			TestListeners.extentTest.get().info("The checkbox for STO feature is present.");
			logger.info("The checkbox for STO feature is present.");
		} catch (Exception e) {
			TestListeners.extentTest.get().info("The checkbox for STO feature is not present.");
			logger.info("The checkbox for STO feature is not present.");
		}
		utils.implicitWait(60);
		return status;
	}

	public boolean verifyStoTooltipPresence(String expectedString) {
		boolean status = false;
		try {
			utils.implicitWait(2);
			String tooltipText = utils.getLocator("campaignsBetaPage.stoTooltipClassic")
					.getAttribute("data-original-title");
			if (tooltipText.contains(expectedString)) {
				TestListeners.extentTest.get()
						.info("The tooltip for STO feature is present with text: " + expectedString);
				logger.info("The tooltip for STO feature is present with text: " + expectedString);
				status = true;
			}
		} catch (NoSuchElementException e) {
			TestListeners.extentTest.get().info("The tooltip for STO feature is not present.");
			logger.info("The tooltip for STO feature is not present.");
		}
		utils.implicitWait(60);
		return status;
	}

	public void selectSTOcheckbox() {
		utils.getLocator("campaignsBetaPage.recommendedSendTime").click();
		logger.info("Use Recommended Send Time option is checked");
		TestListeners.extentTest.get().info("Use Recommended Send Time option is checked");
	}

	public boolean verifyStartDateSelectedOrNot(String expectedText) {
		utils.longWaitInSeconds(2);
		boolean flag = true;
		String text = utils.getLocator("campaignsBetaPage.startDateNew").getText();
		System.out.println(text);
		if (text.contains(expectedText)) {
			logger.info("start Date is not selected");
			TestListeners.extentTest.get().info("start Date is not selected");
			flag = false;
		} else {
			logger.info("Start Date is already selected");
			TestListeners.extentTest.get().info("start Date is already selected");
		}
		return flag;
	}

	public boolean verifyEndDateSelectedOrNot(String expectedText) {
		utils.longWaitInSeconds(2);
		boolean flag = true;
		String text = utils.getLocator("campaignsBetaPage.endDateNew").getText();
		System.out.println(text);
		if (text.contains(expectedText)) {
			logger.info("start Date is not selected");
			TestListeners.extentTest.get().info("start Date is not selected");
			flag = false;
		} else {
			logger.info("Start Date is already selected");
			TestListeners.extentTest.get().info("start Date is already selected");
		}
		return flag;
	}

	public String validateSuccessMessage(String campName) throws InterruptedException {
		String val = "";
		Thread.sleep(2000);
		try {
			WebElement successMsg = utils.getLocator("campaignsBetaPage.successAlert");
			if (successMsg.isDisplayed()) {
				val = successMsg.getText();

			}
		} catch (Exception e) {
			searchCampaign(campName);
			String xpath = utils.getLocatorValue("campaignsBetaPage.checkScheduleLabel").replace("$campaignName",
					campName);
			if (driver.findElement(By.xpath(xpath)).isDisplayed()) {
				val = "Schedule created successfully.";
			}
		}
		return val;
	}

	public void setStartTime() {

		String fullDate = CreateDateTime.getCurrentDate();
		String date[] = fullDate.split("-");
		String currentDate = date[2];
		int i = Integer.parseInt(currentDate);
		int newDate = i + 1;
		String nextdayDate = Integer.toString(newDate);
		utils.getLocator("campaignsBetaPage.setStartTime").click();
		List<WebElement> ele = utils.getLocatorList("signupCampaignsPage.startDateListAB");
		utils.selectListDrpDwnValue(ele, nextdayDate);

		utils.getLocator("signupCampaignsPage.applyBtn").click();
	}
}
