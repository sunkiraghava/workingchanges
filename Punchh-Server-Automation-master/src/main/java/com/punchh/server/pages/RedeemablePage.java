package com.punchh.server.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.Color;
import org.openqa.selenium.support.ui.Select;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class RedeemablePage {
	static Logger logger = LogManager.getLogger(RedeemablePage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	String ssoEmail, temp = "temp";
	String scheduleName;
	String oneDollerDiscount = "1";
	SignupCampaignPage signupCampaignPage;
	DashboardPage dashboardpage;

	private Map<String, By> locators;

	public RedeemablePage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(this.driver);
		selUtils = new SeleniumUtilities(driver);
		signupCampaignPage = new SignupCampaignPage(this.driver);
		dashboardpage = new DashboardPage(driver);
		locators = utils.getAllByMap();
	}

	public void createRedeemable(String redeemableName) {
		utils.waitTillPagePaceDone();
		WebElement redeemableHeading = driver.findElement(locators.get("redeemablePage.redeemableHeading"));
		redeemableHeading.isDisplayed();
		WebElement newRedeemableButton = driver.findElement(locators.get("redeemablePage.newRedeemableButton"));
		newRedeemableButton.isDisplayed();
		newRedeemableButton.click();
		// utils.waitTillPagePaceDone();
		WebElement newRedeemableHeading = driver.findElement(locators.get("redeemablePage.newRedeemableHeading"));
		newRedeemableHeading.isDisplayed();
		WebElement redeemableNameTextbox = driver.findElement(locators.get("redeemablePage.redeemableNameTextbox"));
		redeemableNameTextbox.isDisplayed();

		List<WebElement> redeemableInputBoxListWEle = driver.findElements(locators.get("redeemablePage.redeemableNameTextbox"));
		for (WebElement wEle : redeemableInputBoxListWEle) {
			try {
				wEle.click();
				wEle.clear();
				wEle.sendKeys(redeemableName);
				break;
			} catch (Exception e) {

			}
		}
		enterRedeemableDescriptionTextArea("Test Description " + redeemableName);
		WebElement nextButton = driver.findElement(locators.get("redeemablePage.nextButton"));
		utils.scrollToElement(driver, nextButton);
		nextButton.click();
		utils.waitTillPagePaceDone();
		logger.info("Redeemable name is set as: " + redeemableName);
		TestListeners.extentTest.get().info("Redeemable name is set as: " + redeemableName);
	}

	public void setRedeemableName(String redeemableName) {
		utils.waitTillPagePaceDone();
		WebElement redeemableHeading = driver.findElement(locators.get("redeemablePage.redeemableHeading"));
		redeemableHeading.isDisplayed();
		WebElement newRedeemableButton = driver.findElement(locators.get("redeemablePage.newRedeemableButton"));
		newRedeemableButton.isDisplayed();
		newRedeemableButton.click();
		utils.waitTillPagePaceDone();
		WebElement newRedeemableHeading = driver.findElement(locators.get("redeemablePage.newRedeemableHeading"));
		newRedeemableHeading.isDisplayed();
		WebElement redeemableNameTextbox = driver.findElement(locators.get("redeemablePage.redeemableNameTextbox"));
		redeemableNameTextbox.isDisplayed();
		List<WebElement> redeemableInputBoxListWEle = driver.findElements(locators.get("redeemablePage.redeemableNameTextbox"));
		for (WebElement wEle : redeemableInputBoxListWEle) {
			try {
				wEle.click();
				wEle.clear();
				wEle.sendKeys(redeemableName);
				break;
			} catch (Exception e) {

			}
		}
		enterRedeemableDescriptionTextArea("Test Description " + redeemableName);
		logger.info("Redeemable name is set as: " + redeemableName);
		TestListeners.extentTest.get().info("Redeemable name is set as: " + redeemableName);
	}

	public String uploadRedeemableimage() throws InterruptedException {

		WebElement imgInput = driver.findElement(locators.get("redeemablesPage.imageInput"));
		String js = "arguments[0].style.visibility='visible';";
		((JavascriptExecutor) driver).executeScript(js, imgInput);
		imgInput.sendKeys(System.getProperty("user.dir") + "/resources/images.png");
		logger.info("Redeemable image uploded button clicked successfully");
		TestListeners.extentTest.get().info("Redeemable image uploded button clicked successfully");
		WebElement imageElement = driver.findElement(locators.get("redeemablePage.imageElement"));
		String imageSrcVal = imageElement.getAttribute("src");
		WebElement nextBtn = driver.findElement(locators.get("redeemablesPage.nextBtn"));
		utils.StaleElementclick(driver, nextBtn);
		utils.waitTillPagePaceDone();
		return imageSrcVal;
	}

	public String removeUploadedRedeemableimage() throws InterruptedException {
		WebElement searchedRedeemable = driver.findElement(locators.get("redeemablePage.searchedRedeemable"));
		searchedRedeemable.isDisplayed();
		searchedRedeemable.click();
		WebElement removeButton = driver.findElement(locators.get("redeemablePage.removeButton"));
		removeButton.isDisplayed();
		removeButton.click();
		logger.info("Redeemable image removed button clicked successfully");
		TestListeners.extentTest.get().info("Redeemable image removed button clicked successfully");
		WebElement imageElement = driver.findElement(locators.get("redeemablePage.imageElement"));
		imageElement.isDisplayed();
		String imageSrcVal = imageElement.getAttribute("src");
		WebElement nextBtn = driver.findElement(locators.get("redeemablesPage.nextBtn"));
		utils.StaleElementclick(driver, nextBtn);
		utils.waitTillPagePaceDone();
		return imageSrcVal;
	}

	public void enableDistributable(String segmentName) {
		selUtils.implicitWait(50);
		selUtils.longWait(3000);
		WebElement distributableToggleButton = driver.findElement(locators.get("redeemablePage.distributableToggleButton"));
		distributableToggleButton.isDisplayed();
		distributableToggleButton.isEnabled();
		selUtils.jsClick(distributableToggleButton);
		logger.info("Enabled distributable redeemable");
		TestListeners.extentTest.get().info("Enabled distributable redeemable");
		WebElement segmentDropdown = driver.findElement(locators.get("redeemablePage.segmentDropdown"));
		segmentDropdown.isDisplayed();
		Select sel = new Select(segmentDropdown);
		sel.selectByVisibleText(segmentName);
		// utils.clickByJSExecutor(driver,
		// utils.getLocator("redeemablePage.distributableSegmentDrpDown"));
		utils.waitTillPagePaceDone();
		// selUtils.longWait(3000);
		// utils.getLocator("redeemablePage.distributableSegmentDrpDown").click();
		// utils.clickByJSExecutor(driver,
		// utils.getLocator("redeemablePage.distributableSegmentText"));
		// utils.getLocator("redeemablePage.distributableSegmentText").clear();
		// utils.getLocator("redeemablePage.distributableSegmentText").sendKeys(segmentName);
		// utils.getLocator("redeemablePage.distributableSegmentText").sendKeys(Keys.ENTER);
		logger.info("Selected segment for distributable redeemable");
		TestListeners.extentTest.get().pass("Enabled distributable redeemable");
	}

	public void selectRecieptRule(String discountAmount) throws InterruptedException {
		// selUtils.implicitWait(4);
		// Thread.sleep(4000);
		WebElement flatRadioButton = driver.findElement(locators.get("redeemablePage.flatRadioButton"));
		flatRadioButton.isDisplayed();

		utils.StaleElementclick(driver, flatRadioButton);
		// utils.getLocator("redeemablePage.flatRadioButton").click();
		WebElement flatDiscountTextbox = driver.findElement(locators.get("redeemablePage.flatDiscountTextbox"));
		flatDiscountTextbox.isDisplayed();
		flatDiscountTextbox.clear();
		flatDiscountTextbox.sendKeys(discountAmount);
		logger.info("Flat Discount is selected as - " + discountAmount);
		TestListeners.extentTest.get().info("Flat Discount is selected as - " + discountAmount);
		WebElement secondNextButton = driver.findElement(locators.get("redeemablePage.secondNextButton"));
		secondNextButton.click();
	}

	public void verifyDistributableRedeemableDateErrors() {
		// Start date error validation
		WebElement finishButton = driver.findElement(locators.get("redeemablePage.finishButton"));
		finishButton.isDisplayed();
		finishButton.click();
		// utils.getLocator("redeemablePage.startDateError").isDisplayed();
		// utils.getLocator("redeemablePage.startDateError").sendKeys(CreateDateTime.getTomorrowDateTime());
		// utils.getLocator("redeemablePage.finishButton").click();
		WebElement startDateError = driver.findElement(locators.get("redeemablePage.startDateError"));
		startDateError.isDisplayed();
		logger.info("Verified start date error: " + startDateError.getText());
		TestListeners.extentTest.get()
				.pass("Verified start date error: " + startDateError.getText());
		// End date error validation
		WebElement startDateInput = driver.findElement(locators.get("redeemablePage.startDateInput"));
		startDateInput.clear();
		startDateInput.sendKeys(CreateDateTime.getTomorrowDateTime());
		// utils.getLocator("redeemablePage.endDateError").sendKeys(CreateDateTime.getTomorrowDateTime());
		finishButton = driver.findElement(locators.get("redeemablePage.finishButton"));
		finishButton.click();
		WebElement endDateError = driver.findElement(locators.get("redeemablePage.endDateError"));
		endDateError.isDisplayed();
		logger.info("Verified start date error " + endDateError.getText());
		TestListeners.extentTest.get()
				.pass("Verified start date error " + endDateError.getText());

		// Error validation when start date is after before date
		startDateInput.clear();
		startDateInput.sendKeys(CreateDateTime.getTomorrowDateTime());
		WebElement endDateInput = driver.findElement(locators.get("redeemablePage.endDateInput"));
		endDateInput.sendKeys(CreateDateTime.getCurrentDateAATime());
		finishButton = driver.findElement(locators.get("redeemablePage.finishButton"));
		finishButton.click();
		WebElement endDateBeforeError = driver.findElement(locators.get("redeemablePage.endDateBeforeError"));
		endDateBeforeError.isDisplayed();
		logger.info("Verified start date error " + endDateBeforeError.getText());
		TestListeners.extentTest.get()
				.pass("Verified start date error " + endDateBeforeError.getText());

		// Verify error when Start time selected of past
		startDateInput.clear();
		startDateInput.sendKeys(CreateDateTime.getYesterdayDateTime());
		endDateInput.clear();
		endDateInput.sendKeys(CreateDateTime.getTomorrowDateTime());
		finishButton = driver.findElement(locators.get("redeemablePage.finishButton"));
		finishButton.click();
		WebElement startTimeBeforeError = driver.findElement(locators.get("redeemablePage.startTimeBeforeError"));
		startTimeBeforeError.isDisplayed();
		logger.info("Verified start date error " + startTimeBeforeError.getText());
		TestListeners.extentTest.get()
				.pass("Verified start date error " + startTimeBeforeError.getText());

		// ====
		// Pre-condition -> Deal created with future start_date 1.Open Signup campaign
		// 1st step 2.Select Gift Type -> Gift Redeemable 3.Search the Redeemable in
		// drop down menu

		// ==
		// Pre-condition -> Deal created with future start_date 1.On redeemable listing
		// page check the status and colour code

		// till point 11

	}

	public void verifyDealWithFutureStartDate() {
		// Pre-condition -> Deal created with future start_date 1.Click "Message / Gift"
		// (User Timeline section) 2.Select Gift Types -> Redeemable 3.Search the
		// Redeemable in drop down menu
		WebElement startDateInput = driver.findElement(locators.get("redeemablePage.startDateInput"));
		startDateInput.clear();
		startDateInput.sendKeys(CreateDateTime.getFutureDateTime(365));
		WebElement endDateInput = driver.findElement(locators.get("redeemablePage.endDateInput"));
		endDateInput.clear();
		endDateInput.sendKeys(CreateDateTime.getFutureDateTime(370));
		WebElement finishButton = driver.findElement(locators.get("redeemablePage.finishButton"));
		finishButton.click();
		WebElement redeemableSaveMessage = driver.findElement(locators.get("redeemablePage.redeemableSaveMessage"));
		redeemableSaveMessage.isDisplayed();
		logger.info(redeemableSaveMessage.getText());
		TestListeners.extentTest.get().info(redeemableSaveMessage.getText());
	}

	public void verifyRedeemablColor(String redeemableName) {

		WebElement redeemableStatusLabel = driver.findElement(
				By.xpath(utils.getLocatorValue("redeemablePage.redeemableStatusLabel").replace("temp", redeemableName)));
		redeemableStatusLabel.isDisplayed();
		String redeeemableColor = redeemableStatusLabel.getCssValue("color");
		String orangeColor = "rgba(255, 173, 0, 1)";
		if (redeeemableColor.equals(orangeColor)) {
			logger.info("Verfied scheduled redeemable color");
			TestListeners.extentTest.get().pass("Verfied scheduled redeemable color");
		}

	}

	public void deleteRedeemable(String redeemableName) {
		WebElement redeemableLink = driver.findElement(
				By.xpath(utils.getLocatorValue("redeemablePage.redeemableLink").replace("temp", redeemableName)));
		redeemableLink.isDisplayed();
		redeemableLink.click();
		WebElement ellipsisMenuButton = driver.findElement(locators.get("redeemablePage.ellipsisMenuButton"));
		ellipsisMenuButton.isDisplayed();
		ellipsisMenuButton.click();

		WebElement deleteRedeemableButton = driver.findElement(locators.get("redeemablePage.deleteRedeemableButton"));
		deleteRedeemableButton.isDisplayed();
		deleteRedeemableButton.click();

		Alert alert = driver.switchTo().alert();
		if (alert.getText().equals("Are you sure you want to delete this redeemable?")) {
			alert.accept();
		} else {
			logger.info("Incorrect alert message");
			TestListeners.extentTest.get().fail("Incorrect alert message");
		}
		utils.longWaitInSeconds(5);
		WebElement redeemableDeletedLabel = driver.findElement(locators.get("redeemablePage.redeemableDeletedLabel"));
		redeemableDeletedLabel.isDisplayed();
		logger.info("Successfully deleted redeemable: " + redeemableName);
		TestListeners.extentTest.get().pass("Successfully deleted redeemable: " + redeemableName);

	}

	public void searchRedeemable(String redeemableName) {
		selUtils.longWait(3000);
		WebElement redeemableSearchTextbox = driver.findElement(locators.get("redeemablePage.redeemableSearchTextbox"));
		redeemableSearchTextbox.isDisplayed();
		redeemableSearchTextbox.clear();
		redeemableSearchTextbox.sendKeys(redeemableName);
		redeemableSearchTextbox.sendKeys(Keys.ENTER);
		redeemableSearchTextbox.isDisplayed();
		WebElement searchIcon = driver.findElement(locators.get("redeemablePage.searchIcon"));
		selUtils.jsClick(searchIcon);
		WebElement searchResultLabel = driver.findElement(
				By.xpath(utils.getLocatorValue("redeemablePage.searchResultLabel").replace("temp", redeemableName)));
		searchResultLabel.isDisplayed();
		logger.info("Redeemable is appearing on search: " + redeemableName);
		TestListeners.extentTest.get().info("Redeemable is appearing on search: " + redeemableName);
	}

	public void createRedeemableV5redemption(String redeemableName) throws InterruptedException {
		utils.waitTillPagePaceDone();
		WebElement redeemableHeading = driver.findElement(locators.get("redeemablePage.redeemableHeading"));
		redeemableHeading.isDisplayed();
		WebElement newRedeemableButton = driver.findElement(locators.get("redeemablePage.newRedeemableButton"));
		newRedeemableButton.isDisplayed();
		newRedeemableButton.click();
		utils.waitTillPagePaceDone();
		WebElement newRedeemableHeading = driver.findElement(locators.get("redeemablePage.newRedeemableHeading"));
		newRedeemableHeading.isDisplayed();
		WebElement redeemableNameTextbox = driver.findElement(locators.get("redeemablePage.redeemableNameTextbox"));
		redeemableNameTextbox.isDisplayed();
		redeemableNameTextbox.clear();
		redeemableNameTextbox.sendKeys(redeemableName);
		WebElement redeemableDescriptionTextbox = driver.findElement(locators.get("redeemablePage.redeemableDescriptionTextbox"));
		redeemableDescriptionTextbox.clear();
		redeemableDescriptionTextbox.sendKeys("Test description");
		WebElement nextButton = driver.findElement(locators.get("redeemablePage.nextButton"));
		nextButton.click();
		logger.info("Redeemable name is set as: " + redeemableName);
		TestListeners.extentTest.get().info("	" + redeemableName);
		enableDistributable("All Signed Up");
		WebElement flatRadioButton = driver.findElement(locators.get("redeemablePage.flatRadioButton"));
		flatRadioButton.click();
		WebElement flatDiscountTextbox = driver.findElement(locators.get("redeemablePage.flatDiscountTextbox"));
		flatDiscountTextbox.sendKeys("19");
		WebElement secondNextButton = driver.findElement(locators.get("redeemablePage.secondNextButton"));
		secondNextButton.click();
		allowRedeemableToRunIndefinitely();
		WebElement activateNowSlide = driver.findElement(locators.get("redeemablePage.activateNowSlider"));
		utils.turnSliderOn(driver, activateNowSlide);
	}

	public void createRedeemableClickFinishButton() {
		WebElement ele = driver.findElement(locators.get("redeemablePage.finishButton"));
		ele.click();
		utils.waitTillPagePaceDone();
	}

	public void configureRedemptionDisplay(String points) {
		WebElement clickRedemptionDisplayPage = driver.findElement(locators.get("redeemablePage.clickRedemptionDisplayPage"));
		clickRedemptionDisplayPage.click();
		WebElement redemptionMark = driver.findElement(locators.get("redeemablePage.redemptionMark"));
		redemptionMark.clear();
		redemptionMark.sendKeys(points);
		WebElement clickUpadateButton = driver.findElement(locators.get("redeemablePage.clickUpadateButton"));
		clickUpadateButton.click();
	}

	public void clickOnRedeemableTab(String tabName) {
		selUtils.implicitWait(20);
		String xPath = utils.getLocatorValue("redeemablePage.redeemableTabName").replace("$TabName", tabName);
		WebElement wEle = driver.findElement(By.xpath(xPath));
		wEle.click();
		selUtils.implicitWait(50);

	}

	public boolean checkRedeemableIsExist(String redeemableName) throws InterruptedException {
		boolean result = false;
		selUtils.implicitWait(2);
		WebElement redeemableSearchTextbox = driver.findElement(locators.get("redeemablePage.redeemableSearchTextbox"));
		redeemableSearchTextbox.clear();
		redeemableSearchTextbox.sendKeys(redeemableName);
		redeemableSearchTextbox.sendKeys(Keys.ENTER);
		// Thread.sleep(2000);
		boolean isTextDisplayedInSource = driver.getPageSource().contains("No Matches Found");
		if (isTextDisplayedInSource) {
			WebElement redeemableSearchResultIfNotExist = driver.findElement(locators.get("redeemablePage.redeemableSearchResultIfNotExist"));
			String matchResultText = redeemableSearchResultIfNotExist.getText();
			if (matchResultText.trim().equalsIgnoreCase("No Matches Found")) {
				result = false;
				return result;
			}
		} else {
			List<WebElement> redeemableSearchResultListIfexist = driver.findElements(locators.get("redeemablePage.redeemableSearchResultListIfexist"));
			for (WebElement eleW : redeemableSearchResultListIfexist) {
				String actualTextFromList = eleW.getText();
				if (actualTextFromList.equalsIgnoreCase(redeemableName)) {
					result = true;
					return result;
				}
			}
		}
		return result;
	}

	public void createRedeemableIfNotExistWithExistingQC(String redeemableName, String receiptRule, String QCname,
			String flatDiscountAmount) throws InterruptedException {
		// utils.waitTillPagePaceDone();
		boolean isLFExist = checkRedeemableIsExist(redeemableName);
		if (!isLFExist) {
			logger.info(redeemableName + " Redeemable is not exist and Create new Redeemable ");
			TestListeners.extentTest.get().pass(redeemableName + " Redeemable is not exist and Create new Redeemable ");

			// creating new redeemable when not found
			enterRedeemableWithQCAndFlatDiscount(redeemableName, receiptRule, QCname, flatDiscountAmount);
			logger.info(redeemableName + " Redeemable is created successfuly");
			TestListeners.extentTest.get().pass(redeemableName + " Redeemable is created successfuly");

		} else {
			logger.info(redeemableName + " Redeemable  is available ");
			TestListeners.extentTest.get().pass(redeemableName + " Redeemable  is  available");

		}
	}

	public void enterRedeemableWithQCAndFlatDiscount(String redeemableName, String receiptRule, String QCname,
			String flatDiscountAmount) throws InterruptedException {
		// utils.getLocator("redeemablePage.newRedeemableButton").click();
		// utils.getLocator("redeemablePage.redeemableNameTextbox").click();
		// utils.getLocator("redeemablePage.redeemableNameTextbox").sendKeys(redeemableName);

		createRedeemable(redeemableName);
		// utils.getLocator("redeemablePage.nextButton").click();
		Thread.sleep(1000);

		switch (receiptRule) {
		case "Existing Qualifier":
			WebElement existingQualifierOption = driver.findElement(locators.get("redeemablePage.existingQualifierOption"));
			utils.clickByJSExecutor(driver, existingQualifierOption);
			Thread.sleep(2000);
			WebElement chooseAReceiptRuleDropdown = driver.findElement(locators.get("redeemablePage.chooseAReceiptRuleDropdown"));
			Select sel = new Select(chooseAReceiptRuleDropdown);
			sel.selectByVisibleText(flatDiscountAmount);
			WebElement secondNextButton = driver.findElement(locators.get("redeemablePage.secondNextButton"));
			secondNextButton.click();
			Thread.sleep(2000);
			break;

		case "Flat Discount":
			selectRecieptRule(flatDiscountAmount);
			break;
		default:
			break;
		}

		// fill expire dates
		WebElement redeemableRunIndefinitely = driver.findElement(locators.get("redeemablePage.redeemableRunIndefinitely"));
		String color = redeemableRunIndefinitely.getCssValue("background-color");
		String hexcolor = Color.fromString(color).asHex();
		if (hexcolor != "#37c936") {
			redeemableRunIndefinitely.click();
		}
		WebElement finishButton = driver.findElement(locators.get("redeemablePage.finishButton"));
		finishButton.click();

	}

	public void createDistributableRedeemable(String redeemableName, String discountAmount, String startdateTime,
			String enddateTime) {
		createRedeemable(redeemableName);
		setDistributableSliderOn();
		setRedeemableEligibilityFlatDiscount(discountAmount);
		setActivateNowAndAllowRedeemableRunIndefinitelyOff();
		checkStartEndTimeMinuteInterval();
		setScheduledValidityStartEndTime(startdateTime, enddateTime);
	}

	public void setRedeemableInfo(String redeemableName) {
		WebElement newRedeemableButton = driver.findElement(locators.get("redeemablePage.newRedeemableButton"));
		newRedeemableButton.click();
		WebElement redeemableNameTextbox = driver.findElement(locators.get("redeemablePage.redeemableNameTextbox"));
		redeemableNameTextbox.click();
		redeemableNameTextbox.sendKeys(redeemableName);
		WebElement nextButton = driver.findElement(locators.get("redeemablePage.nextButton"));
		nextButton.click();
	}

	public void setDistributableSliderOn() {

		WebElement distributableSlide = driver.findElement(locators.get("redeemablePage.distributableSlider"));
		utils.turnSliderOn(driver, distributableSlide);
		WebElement avalableLoyaltyCustomerSlide = driver.findElement(locators.get("redeemablePage.loyaltyCustomersSlider"));
		utils.turnSliderOn(driver, avalableLoyaltyCustomerSlide);
	}

	public void setActivateNowAndAllowRedeemableRunIndefinitelyOff() {

		WebElement redeemableRunIndefinitelySlide = driver.findElement(locators.get("redeemablePage.redeemableRunIndefinitely"));
		utils.turnSliderOff(driver, redeemableRunIndefinitelySlide);
		WebElement activateNowSlide = driver.findElement(locators.get("redeemablePage.activateNowSlider"));
		utils.turnSliderOff(driver, activateNowSlide);
	}

	public void setRedeemableEligibilityFlatDiscount(String discountAmount) {
		WebElement flatRadioButton = driver.findElement(locators.get("redeemablePage.flatRadioButton"));
		utils.StaleElementclick(driver, flatRadioButton);
		WebElement flatDiscountTextbox = driver.findElement(locators.get("redeemablePage.flatDiscountTextbox"));
		flatDiscountTextbox.sendKeys(discountAmount);
		WebElement secondNextButton = driver.findElement(locators.get("redeemablePage.secondNextButton"));
		secondNextButton.click();
	}

	public void setScheduledValidityStartEndTime(String startdateTime, String enddateTime) {
		WebElement redeemableStartTime = driver.findElement(locators.get("redeemablePage.redeemableStartTime"));
		redeemableStartTime.clear();
		redeemableStartTime.sendKeys(startdateTime);
		WebElement redeemableEndTime = driver.findElement(locators.get("redeemablePage.redeemableEndTime"));
		redeemableEndTime.clear();
		redeemableEndTime.sendKeys(enddateTime);
		WebElement finishBtn = driver.findElement(locators.get("redeemablePage.finishBtn"));
		utils.clickByJSExecutor(driver, finishBtn);
		logger.info("Clicked finish button");
	}

	public void checkStartEndTimeMinuteInterval() {
		List<Integer> val = IntStream.range(0, 60).boxed().collect(Collectors.toList());

		WebElement redeemableStartTime = driver.findElement(locators.get("redeemablePage.redeemableStartTime"));
		redeemableStartTime.clear();
		redeemableStartTime.click();
		WebElement startDateMinuteDrp = driver.findElement(locators.get("redeemablePage.stratMinuteDrp"));
		List<Integer> startMinute = utils.getAllDrpValuse(startDateMinuteDrp);

		WebElement redeemableEndTime = driver.findElement(locators.get("redeemablePage.redeemableEndTime"));
		redeemableEndTime.clear();
		redeemableEndTime.click();
		WebElement endDateMinuteDrp = driver.findElement(locators.get("redeemablePage.endMinuteDrp"));
		List<Integer> endMinute = utils.getAllDrpValuse(endDateMinuteDrp);
		Assert.assertTrue(val.equals(startMinute), "start minute interval did not matched");
		Assert.assertTrue(val.equals(endMinute), "end minute interval did not matched");

	}

	public String getRedeemableStatus(String redeemableName) {
		searchRedeemable(redeemableName);
		WebElement redeemableStatusCol = driver.findElement(locators.get("redeemablePage.redeemableStatusCol"));
		String status = redeemableStatusCol.getText().trim();
		return status;
	}

	public String getRedeemableID(String redeemableName) {
		selUtils.implicitWait(10);
		String URL = "";
		String redeemableID = "";
		WebElement redeemableSearchTextbox = driver.findElement(locators.get("redeemablePage.redeemableSearchTextbox"));
		redeemableSearchTextbox.clear();
		redeemableSearchTextbox.sendKeys(redeemableName);
		redeemableSearchTextbox.sendKeys(Keys.ENTER);

		List<WebElement> redeemableSearchResultListIfexist = driver.findElements(locators.get("redeemablePage.redeemableSearchResultListIfexist"));
		for (WebElement eleW : redeemableSearchResultListIfexist) {
			String actualTextFromList = eleW.getText();
			if (actualTextFromList.equalsIgnoreCase(redeemableName)) {

				URL = eleW.getAttribute("href");
				redeemableID = URL.replaceAll("[^0-9]", "");
				break;
			}
		}
		logger.info(redeemableName + "  Id is : " + redeemableID);
		TestListeners.extentTest.get().pass(redeemableName + "  Id is : " + redeemableID);
		return redeemableID;
	}

	public boolean verifyErrorMessage(String actualErrorMessage, String expectedErrorMessage) {
		if (actualErrorMessage.contains(expectedErrorMessage)) {
			return true;
		} else
			return false;
	}

	// for Moes business
	public void deactivateThRedeemable(String redeemableName, String activationType) throws InterruptedException {
		WebElement redeemableStatusElement = driver
				.findElement(By.xpath(
						"//a[text()='" + redeemableName + "']/../../../../following-sibling::td[1]//div/span[2]"));
		String redeemableStatus = redeemableStatusElement.getText();

		if (redeemableStatus.equalsIgnoreCase("Deactivated") && activationType.equalsIgnoreCase("OFF")) {

			// do not click on activate icon
			System.out.println(redeemableName + " is allready deactivate ");
		} else if (redeemableStatus.equalsIgnoreCase("Deactivated") && activationType.equalsIgnoreCase("ON")) {
			// activate the redeemable

			WebElement activateDeactivateLink = driver.findElement(
					By.xpath("//a[text()='" + redeemableName + "']/../../../../following-sibling::td[2]//a[3]"));
			activateDeactivateLink.click();

		} else if (redeemableStatus.equalsIgnoreCase("Activated") && activationType.equalsIgnoreCase("OFF")) {
			// deactivate the redeemable

			WebElement activateDeactivateLink = driver.findElement(
					By.xpath("//a[text()='" + redeemableName + "']/../../../../following-sibling::td[2]//a[3]"));
			activateDeactivateLink.click();

		} else if (redeemableStatus.equalsIgnoreCase("Activated") && activationType.equalsIgnoreCase("ON")) {

			// do not click on activate icon
			System.out.println(redeemableName + " is allready activate ");

		}
	}

	public void deactivateRedeemable(String redeemableName) {
		WebElement redeemableLink = driver.findElement(
				By.xpath(utils.getLocatorValue("redeemablePage.redeemableLink").replace("temp", redeemableName)));
		redeemableLink.isDisplayed();
		redeemableLink.click();
		WebElement ellipsisMenuButton = driver.findElement(locators.get("redeemablePage.ellipsisMenuButton"));
		ellipsisMenuButton.isDisplayed();
		ellipsisMenuButton.click();

		WebElement deactivateRedeemableButton = driver.findElement(locators.get("redeemablePage.deactivateRedeemableButton"));
		deactivateRedeemableButton.isDisplayed();
		deactivateRedeemableButton.click();

		Alert alert = driver.switchTo().alert();
		if (alert.getText().equals("Are you sure you want to deactivate this redeemable?")) {
			alert.accept();
		} else {
			logger.info("Incorrect alert message");
			TestListeners.extentTest.get().fail("Incorrect alert message");
		}

		logger.info("Successfully deactivated redeemable: " + redeemableName);
		TestListeners.extentTest.get().pass("Successfully deactivated redeemable: " + redeemableName);

	}

	public void expiryInDays(String choice, String days) {
		WebElement expiryInDaysElement = driver.findElement(locators.get("redeemablePage.expiryInDays"));
		switch (choice) {
		case "clear expiry":
			expiryInDaysElement.clear();
			logger.info("Expiry (In Days) is cleared ");
			TestListeners.extentTest.get().pass("Expiry (In Days) is cleared ");
			break;

		default:
			expiryInDaysElement.clear();
			expiryInDaysElement.sendKeys(days);
			logger.info("Expiry (In Days) is : " + days);
			TestListeners.extentTest.get().pass("Expiry (In Days) is : " + days);
			break;
		}
	}

	public void timezoneWithOrWithoutTimezone(String choice, String timezone) {
		switch (choice) {
		case "without timezone":
			WebElement clearTimezone = driver.findElement(locators.get("redeemablePage.clearTimezone"));
			clearTimezone.click();
			logger.info("Timezone is cleared");
			TestListeners.extentTest.get().pass("Timezone is cleared");
			break;

		case "with selected timezone":
			WebElement clickOnTimezoneDrpSown = driver.findElement(locators.get("redeemablePage.clickOnTimezoneDrpSown"));
			clickOnTimezoneDrpSown.click();
			List<WebElement> timezoneList = driver.findElements(locators.get("redeemablePage.timezoneList"));
			utils.selectListDrpDwnValue(timezoneList, timezone);
			logger.info("Selected Timezone is : " + timezone);
			TestListeners.extentTest.get().pass("Selected Timezone is : " + timezone);
			break;

		default:
			WebElement clearTimezoneForText = driver.findElement(locators.get("redeemablePage.clearTimezone"));
			String selectedTimezone = clearTimezoneForText.getText();
			logger.info("Selected Timezone is : " + selectedTimezone);
			TestListeners.extentTest.get().pass("Selected Timezone is : " + selectedTimezone);
			break;
		}
		WebElement clickOnTimezoneDrpSownClose = driver.findElement(locators.get("redeemablePage.clickOnTimezoneDrpSown"));
		clickOnTimezoneDrpSownClose.click();
	}

	public void endTime(String enddateTime) {
		WebElement redeemableEndTime = driver.findElement(locators.get("redeemablePage.redeemableEndTime"));
		redeemableEndTime.clear();
		redeemableEndTime.sendKeys(enddateTime);
		logger.info("End Time is : " + enddateTime);
		TestListeners.extentTest.get().pass("End Time is : " + enddateTime);
	}

	public void allowRedeemableToRunIndefinitely() throws InterruptedException {
		WebElement redeemableRunIndefinitelySlide = driver.findElement(locators.get("redeemablePage.redeemableRunIndefinitely"));
		utils.turnSliderOn(driver, redeemableRunIndefinitelySlide);
		logger.info("clicked redeemable to run indefinitely");
		TestListeners.extentTest.get().pass("clicked redeemable to run indefinitely");
	}

	public void clickOnFinishButton() {
		WebElement finishButton = driver.findElement(locators.get("redeemablePage.finishButton"));
		finishButton.click();
		logger.info("clicked on finished button");
		TestListeners.extentTest.get().pass("clicked on finished button");
	}

	public boolean successOrErrorConfirmationMessage(String message) throws InterruptedException {
		boolean flag = false;
		try {
			WebElement errorORConfirmationMessage = driver.findElement(locators.get("redeemablePage.errorORConfirmationMessage"));
			String result = errorORConfirmationMessage.getText();
			if (message.contains(result)) {
				flag = true;
			}
		} catch (Exception e) {
			flag = false;
		}
		return flag;
	}

	public String successConfirmationMessage() throws InterruptedException {
		WebElement successMsg = driver.findElement(locators.get("redeemablePage.successMsg"));
		String result = successMsg.getText();
		return result;
	}

	public void editTheRedeemable(String redeemableName, String description, String updatedAmount)
			throws InterruptedException {
		WebElement redeemableSearchTextbox = driver.findElement(locators.get("redeemablePage.redeemableSearchTextbox"));
		redeemableSearchTextbox.clear();
		redeemableSearchTextbox.sendKeys(redeemableName);
		redeemableSearchTextbox.sendKeys(Keys.ENTER);
		boolean isLFExist = checkRedeemableIsExist(redeemableName);

		WebElement redeemableLinkByXpath = driver.findElement(By.xpath("//span/a[text()='" + redeemableName + "']"));
		redeemableLinkByXpath.click();

		enterRedeemableDescriptionTextArea(description);

		// WebElement descriptionWele =
		// driver.findElement(By.xpath("//textarea[@id='redeemable_description']"));
		// descriptionWele.clear();
		// descriptionWele.sendKeys(description);

		WebElement nextButton = driver.findElement(locators.get("redeemablePage.nextButton"));
		nextButton.click();
		utils.waitTillPagePaceDone();
		selectRecieptRule(updatedAmount);
		// utils.getLocator("redeemablePage.secondNextButton").click();

		WebElement finishButton = driver.findElement(locators.get("redeemablePage.finishButton"));
		finishButton.click();
		utils.waitTillPagePaceDone();

	}

	public boolean verifyRedeemableDescriptionAndAmount(String redeemableName, String expDesc, String expAmount) {
		boolean flag = false;
		String descriptionText = "";

		List<WebElement> redeemableTextAreaBoxListWEle = driver
				.findElements(locators.get("redeemablePage.redeemableDescriptionTextbox"));
		for (WebElement wEle : redeemableTextAreaBoxListWEle) {
			try {
				descriptionText = wEle.getText();
				break;
			} catch (Exception e) {

			}
		}
		// String descriptionText =
		// driver.findElement(By.xpath("//textarea[@id='redeemable_description']")).getText();

		Assert.assertEquals(descriptionText, expDesc, "Description after updation not matched");

		WebElement nextButton = driver.findElement(locators.get("redeemablePage.nextButton"));
		nextButton.click();
		WebElement flatDiscountTextbox = driver.findElement(locators.get("redeemablePage.flatDiscountTextbox"));
		String amount = flatDiscountTextbox.getAttribute("value");

		double actualAmountAfterUpdated = Double.parseDouble(amount);
		double expAmountDouble = Double.parseDouble(expAmount);

		Assert.assertEquals(actualAmountAfterUpdated, expAmountDouble,
				actualAmountAfterUpdated + " actual amount did not matched with updated amount - " + expAmount);

		flag = true;

		return flag;

	}

	public void searchAndClickOnRedeemable(String redeemableName) throws InterruptedException {
		WebElement redeemableSearchTextbox = driver.findElement(locators.get("redeemablePage.redeemableSearchTextbox"));
		redeemableSearchTextbox.clear();
		redeemableSearchTextbox.sendKeys(redeemableName);
		redeemableSearchTextbox.sendKeys(Keys.ENTER);
		WebElement redeemableLinkByXpath = driver.findElement(By.xpath("//span/a[text()='" + redeemableName + "']"));
		redeemableLinkByXpath.click();
	}

	public void enterRedeemableWithQCAndFlatDiscountWithEndDate(String redeemableName, String receiptRule,
			String QCname, String flatDiscountAmount, String timeZone) throws InterruptedException {
		// selUtils.implicitWait(50);
		WebElement newRedeemableButton = driver.findElement(locators.get("redeemablePage.newRedeemableButton"));
		newRedeemableButton.click();
		utils.waitTillPagePaceDone();
		WebElement redeemableNameTextField = driver.findElement(locators.get("redeemablePage.redeemableNameTextField"));
		redeemableNameTextField.click();
		redeemableNameTextField.sendKeys(redeemableName);
		WebElement nextBtn = driver.findElement(locators.get("redeemablePage.NextBtn"));
		nextBtn.click();
		utils.waitTillPagePaceDone();
		// Thread.sleep(2000);

		switch (receiptRule) {
		case "Existing Qualifier":
			WebElement existingQualifierOption = driver.findElement(locators.get("redeemablePage.existingQualifierOption"));
			utils.clickByJSExecutor(driver, existingQualifierOption);
			Thread.sleep(2000);
			WebElement chooseReceiptRuleDropDown = driver.findElement(locators.get("redeemablePage.chooseReceiptRuleDropDown"));
			chooseReceiptRuleDropDown.click();

			WebElement chooseReceiptRuleDropDownSearchBox = driver.findElement(locators.get("redeemablePage.chooseReceiptRuleDropDownSearchBox"));
			chooseReceiptRuleDropDownSearchBox.sendKeys(QCname);

			String qcXpath = utils.getLocatorValue("redeemablePage.chooseReceiptRuleClickOnSearchedQCName")
					.replace("$QCname", QCname);
			WebElement wEle = driver.findElement(By.xpath(qcXpath));
			wEle.click();

			nextBtn = driver.findElement(locators.get("redeemablePage.NextBtn"));
			nextBtn.click();
			utils.waitTillPagePaceDone();
			break;

		case "Flat Discount":
			selectRecieptRule(flatDiscountAmount);
			break;
		default:
			break;
		}

		// fill endDate
		WebElement timeZoneElement = driver.findElement(locators.get("redeemablePage.timeZone"));
		utils.waitTillVisibilityOfElement(timeZoneElement, "timezone");
		utils.selectDrpDwnValue(timeZoneElement, timeZone);
	}

	public void clickFinishBtn() {
		WebElement finishButton = driver.findElement(locators.get("redeemablePage.finishButton"));
		utils.clickByJSExecutor(driver, finishButton);
		logger.info("Clicked on Finish Button");
		TestListeners.extentTest.get().info("Clicked on Finish Button");
		// utils.getLocator("redeemablePage.finishButton").click();
		utils.waitTillPagePaceDone();
	}

	public String getEndDateOfRedeemable(String redeemable) throws InterruptedException {
		searchAndClickOnRedeemable(redeemable);
		WebElement nextButton = driver.findElement(locators.get("redeemablePage.nextButton"));
		nextButton.click();
		utils.waitTillVisibilityOfElement(nextButton, "");
		utils.StaleElementclick(driver, nextButton);
		// utils.getLocator("redeemablePage.nextButton").click();

		WebElement redeemableEndTime = driver.findElement(locators.get("redeemablePage.redeemableEndTime"));
		String val = redeemableEndTime.getAttribute("value");
		return val;
	}

	public void attributesApplicableOnThisRedeemable(String text) {
		WebElement selectAttributesApplicableOnThisRedeemable = driver.findElement(By.xpath(utils.getLocatorValue("redeemablePage.selectAttributesApplicableOnThisRedeemable")
				.replace("$temp", text)));
		selectAttributesApplicableOnThisRedeemable.click();
		logger.info("Selected text for Attributes Applicable On This Redeemable " + text);
		TestListeners.extentTest.get().pass("Selected text for Attributes Applicable On This Redeemable " + text);
	}

	// shashank
	public boolean verifyEnableAutoRedemptionToggleDisplayed(String redeemableName, String labelName)
			throws InterruptedException {
		WebElement redeemableSearchTextbox = driver.findElement(locators.get("redeemablePage.redeemableSearchTextbox"));
		redeemableSearchTextbox.clear();
		redeemableSearchTextbox.sendKeys(redeemableName);
		// utils.getLocator("redeemablePage.redeemableSearchTextbox").sendKeys(Keys.ENTER);
		// boolean isLFExist = checkRedeemableIsExist(redeemableName);
		WebElement redeemableLinkByXpath = driver.findElement(By.xpath("//span/a[text()='" + redeemableName + "']"));
		redeemableLinkByXpath.click();
		utils.waitTillPagePaceDone();
		WebElement nextButton = driver.findElement(locators.get("redeemablePage.nextButton"));
		nextButton.click();
		utils.longwait(2000);
		utils.waitTillPagePaceDone();
		boolean flagIsPresent = utils.isTextPresent(driver, labelName);
		return flagIsPresent;
	}

	public boolean verifyEnableAutoRedemptionVisisbleWhileCreatingRedeemable(String redeemableName, String labelName)
			throws InterruptedException {
		selUtils.implicitWait(10);
		WebElement newRedeemableButton = driver.findElement(locators.get("redeemablePage.newRedeemableButton"));
		newRedeemableButton.click();
		WebElement redeemableNameTextbox = driver.findElement(locators.get("redeemablePage.redeemableNameTextbox"));
		redeemableNameTextbox.click();
		redeemableNameTextbox.sendKeys(redeemableName);
//		utils.getLocator("redeemablePage.nextButton").click(); 
		WebElement nextButton = driver.findElement(locators.get("redeemablePage.nextButton"));
		utils.waitTillVisibilityOfElement(nextButton, "Next Button");
		utils.clickByJSExecutor(driver, nextButton);
		// utils.waitTillPagePaceDone();
		boolean flagIsPresent = utils.checkFlagVisiblityOnUi(labelName);
		return flagIsPresent;
	}

	public void enableLoyaltyFlagAndCreateRedeemable(String points) throws InterruptedException {
		allowRedeemableToRunIndefinitely();

		// click additional settings
		dashboardpage.checkBoxFlagOnOffAndClick("redeemable_additional_steps", "check");
		logger.info("clicked additional settings");
		TestListeners.extentTest.get().info("clicked additional settings");

		WebElement pointsToRedeemField = driver.findElement(locators.get("redeemablePage.pointsToRedeemField"));
		utils.waitTillVisibilityOfElement(pointsToRedeemField,
				"Points Needed to Redeem field");
		pointsToRedeemField.clear();
		pointsToRedeemField.sendKeys(points);
		logger.info("Enter " + points + " points in the field points to redeem");
		TestListeners.extentTest.get().info("Enter " + points + " points in the field points to redeem");

		// enable the flag 'Should be available as loyalty points based redemption?'
		dashboardpage.checkBoxFlagOnOffAndClick("redeemable_applicable_as_loyalty_redemption", "check");
		clickFinishBtn();
	}

	public void clickRedeemable(String redeemableName) {
		String xpath = utils.getLocatorValue("redeemablePage.redeemableName").replace("{redeemableName}",
				redeemableName);
		WebElement redeemableNameElement = driver.findElement(By.xpath(xpath));
		redeemableNameElement.click();
		logger.info("clicked on  the redeemable " + redeemableName);
		TestListeners.extentTest.get().pass("clicked on  the redeemable " + redeemableName);
	}

	public void offLoyaltyFlagInRedeemable() {
		WebElement newBtn = driver.findElement(locators.get("redeemablePage.newBtn"));
		utils.scrollToElement(driver, newBtn);
		utils.clickByJSExecutor(driver, newBtn);
		// utils.getLocator("redeemablePage.newBtn").click();
		utils.scrollToElement(driver, newBtn);
		utils.clickByJSExecutor(driver, newBtn);
		// utils.getLocator("redeemablePage.newBtn").click();
		// utils.waitTillPagePaceDone();
		dashboardpage.checkBoxFlagOnOffAndClick("redeemable_applicable_as_loyalty_redemption", "uncheck");
		clickFinishBtn();
	}

	public void enterRedeemableDescriptionTextArea(String description) {
		List<WebElement> redeemableTextAreaBoxListWEle = driver
				.findElements(locators.get("redeemablePage.redeemableDescriptionTextbox"));
		for (WebElement wEle : redeemableTextAreaBoxListWEle) {
			try {
				wEle.click();
				wEle.clear();
				wEle.sendKeys(description);
				break;
			} catch (Exception e) {

			}
		}

	}

	public void editRedeemableExistingQualifier(String redeemableName, String receiptRule, String QCname,
			String flatDiscountAmount) throws InterruptedException {
		searchAndClickOnRedeemable(redeemableName);
		WebElement nextBtn = driver.findElement(locators.get("redeemablePage.NextBtn"));
		nextBtn.click();
		utils.waitTillPagePaceDone();
		switch (receiptRule) {
		case "Existing Qualifier":
			WebElement existingQualifierOption = driver.findElement(locators.get("redeemablePage.existingQualifierOption"));
			utils.clickByJSExecutor(driver, existingQualifierOption);
			utils.waitTillPagePaceDone();
			WebElement chooseAReceiptRuleDropdown = driver.findElement(locators.get("redeemablePage.chooseAReceiptRuleDropdown"));
			Select sel = new Select(chooseAReceiptRuleDropdown);
			sel.selectByVisibleText(flatDiscountAmount);
			WebElement secondNextButton = driver.findElement(locators.get("redeemablePage.secondNextButton"));
			secondNextButton.click();
			utils.waitTillPagePaceDone();
			logger.info("Existing Qualifier is selected as - " + QCname);
			TestListeners.extentTest.get().info("Existing Qualifier is selected as - " + QCname);
			break;

		case "Flat Discount":
			selectRecieptRule(flatDiscountAmount);
			break;
		case "Remove Existing Qualifier":
			WebElement existingQualifierOptionRemove = driver.findElement(locators.get("redeemablePage.existingQualifierOption"));
			utils.clickByJSExecutor(driver, existingQualifierOptionRemove);
			utils.waitTillPagePaceDone();
			WebElement removeExistingQC = driver.findElement(locators.get("redeemablePage.removeExistingQC"));
			removeExistingQC.click();
			WebElement chooseReceiptRuleDropDown = driver.findElement(locators.get("redeemablePage.chooseReceiptRuleDropDown"));
			chooseReceiptRuleDropDown.click();
			WebElement secondNextButtonRemove = driver.findElement(locators.get("redeemablePage.secondNextButton"));
			secondNextButtonRemove.click();
			break;
		}
		utils.waitTillPagePaceDone();
		clickFinishBtn();
	}

	public boolean verifyRedeemableDetailsCreatedByAPI(Map<String, String> mapOfDetials, String receiptRuleType) {
		boolean result = false;
		selUtils.implicitWait(6);
		WebElement nextButton = driver.findElement(locators.get("redeemablePage.nextButton"));
		nextButton.click();

		switch (receiptRuleType.toLowerCase()) {
		case "new":
			WebElement receiptRuleNewQualifierRadioButton = driver.findElement(locators.get("redeemablePage.receiptRuleNewQualifierRadioButton"));
			utils.waitTillElementToBeClickable(receiptRuleNewQualifierRadioButton);
			selUtils.implicitWait(5);
			String newQualifierIsSelected = receiptRuleNewQualifierRadioButton.getAttribute("checked");
			Assert.assertEquals(newQualifierIsSelected, "true", "New Qualifier is not selected");

			logger.info("Verified that New Qualifier is selected in UI ");
			TestListeners.extentTest.get().pass("Verified that New Qualifier is selected in UI ");

			// String redeemableProcessingFunctionXpath = "//label[text()='Redeemable
			// processing
			// function']/following-sibling::div[1]/div/select[@id='dollar_or_percent_off-processing-method']";
			WebElement redeemableProcessingFunctionDropDownXpath = driver.findElement(locators.get("redeemablePage.redeemableProcessingFunctionDropDownXpath"));
			Select sel = new Select(redeemableProcessingFunctionDropDownXpath);
			String redeemableProcessingFunctionValue = sel.getFirstSelectedOption().getText();
			Assert.assertEquals(redeemableProcessingFunctionValue.toLowerCase(),
					mapOfDetials.get("redeemableProcessingFunction").toLowerCase(),
					"Redeemable Processing Function is not matched");

			logger.info("Verified that Redeemable Processing Function is selected in UI "
					+ mapOfDetials.get("redeemableProcessingFunction"));
			TestListeners.extentTest.get().pass("Verified that Redeemable Processing Function is selected in UI "
					+ mapOfDetials.get("redeemableProcessingFunction"));

			WebElement wEle = driver.findElement(locators.get("redeemablePage.redeemableAmountCapXpath"));
			utils.scrollToElement(driver, wEle);
			utils.longWaitInSeconds(2);
			double actual_amount_cap = Double.parseDouble(wEle.getAttribute("value"));
			double expValue_amount_cap = Double.parseDouble(mapOfDetials.get("amount_cap"));
			Assert.assertEquals(actual_amount_cap, expValue_amount_cap, "Amount Cap is not matched");
			logger.info("Verified that " + expValue_amount_cap + " is coming in Amount Cap ");
			TestListeners.extentTest.get().pass("Verified that " + expValue_amount_cap + " is coming in Amount Cap ");
			result = true;
			break;

		case "existing":
			WebElement redeemableCriterionIDXpath = driver.findElement(locators.get("redeemablePage.redeemableCriterionIDXpath"));
			utils.waitTillElementToBeClickable(redeemableCriterionIDXpath);
			selUtils.implicitWait(5);
			Select sel1 = new Select(redeemableCriterionIDXpath);

			String existingQCSelected = sel1.getFirstSelectedOption().getText();
			Assert.assertEquals(existingQCSelected.toLowerCase(), mapOfDetials.get("expQCName").toLowerCase(),
					existingQCSelected + " Existing QC  is not selected");

			logger.info("Verified that existing QC is selected in UI " + existingQCSelected);
			TestListeners.extentTest.get().pass("Verified that existing QC is selected in UI " + existingQCSelected);
			result = true;
			break;

		case "flat_discount":
			WebElement flatDiscountTextbox = driver.findElement(locators.get("redeemablePage.flatDiscountTextbox"));
			double faltDiscountValue = Double.parseDouble(flatDiscountTextbox.getAttribute("value"));
			double expValue = Double.parseDouble(mapOfDetials.get("discount_amount"));
			Assert.assertEquals(faltDiscountValue, expValue,
					expValue + " flat discount amount is not matched with UI value");
			logger.info("Verified that Flat discount is showing in UI " + expValue);
			TestListeners.extentTest.get().pass("Verified that Flat discount is showing in UI " + expValue);

			result = true;
			break;

		default:
			break;
		}

		return result;

	}

	public void verifyCalenderEndDateInRedeemable(String expEndDate) {
		WebElement secondNextButton = driver.findElement(locators.get("redeemablePage.secondNextButton"));
		utils.waitTillElementToBeClickable(secondNextButton);
		secondNextButton.click();
		utils.longWaitInSeconds(3);
		WebElement redeemableEndTime = driver.findElement(locators.get("redeemablePage.redeemableEndTime"));
		String redeemableEndDate = redeemableEndTime.getAttribute("value")
				.replace("AM", "am").replace("PM", "pm");
		Assert.assertEquals(redeemableEndDate.toLowerCase(), expEndDate.toLowerCase(),
				expEndDate + " End Date is not matched");
		logger.info("Verified that End Date is matched in UI " + expEndDate);
		TestListeners.extentTest.get().pass("Verified that End Date is matched in UI " + expEndDate);
	}

	public void verifyExpiryDaysInRedeemable(String expExpiryDays) {
		utils.longWaitInSeconds(3);
		WebElement expiryDayBox = driver.findElement(locators.get("redeemablePage.expiryDayBox"));
		int actExpiryDays = Integer.parseInt(expiryDayBox.getAttribute("value"));
		int expExpiryDaysInt = Integer.parseInt(expExpiryDays);
		Assert.assertEquals(actExpiryDays, expExpiryDaysInt, expExpiryDays + " Expiry Days is not matched");
		logger.info("Verified that expiry days is matched in UI " + expExpiryDays);
		TestListeners.extentTest.get().pass("Verified that expiry days is matched in UI " + expExpiryDays);
	}

	public void verifyRedeemableIndefiniteExpiryToggleButtonStatus(String expStatus) {
		WebElement redeemableIndefiniteExpiryToggleButtonXpath = driver.findElement(locators.get("redeemablePage.redeemableIndefiniteExpiryToggleButtonXpath"));
		String actValue = redeemableIndefiniteExpiryToggleButtonXpath.getAttribute("checked");
		System.out.println("Actual value is : " + actValue);
		Assert.assertEquals(actValue, expStatus, expStatus + " Indefinite expiry toggle button status is not matched");
		logger.info("Verified that status of Indefinite expiry toggle button is matched in UI " + expStatus);
		TestListeners.extentTest.get()
				.pass("Verified that status of Indefinite expiry toggle button is matched in UI " + expStatus);

	}

	public void clickOnSecondButton() {
		WebElement secondNextButton = driver.findElement(locators.get("redeemablePage.secondNextButton"));
		utils.waitTillElementToBeClickable(secondNextButton);
		utils.waitTillElementToBeClickable(secondNextButton);
		secondNextButton.click();
		utils.longWaitInSeconds(3);
	}

	public void clickOnFirstNextButton() {
		WebElement nextButton = driver.findElement(locators.get("redeemablePage.nextButton"));
		utils.waitTillElementToBeClickable(nextButton);
		nextButton.click();
		utils.longWaitInSeconds(3);
	}

	public String redeemableImageUrl() {
		WebElement redeemableImageUrl = driver.findElement(locators.get("redeemablePage.redeemableImageUrl"));
		String URL = redeemableImageUrl.getAttribute("src");
		logger.info("Redeemable image url is:- " + URL);
		TestListeners.extentTest.get().pass("Redeemable image url is:- " + URL);
		return URL;

	}

	public void removeExistingQualifier() {
		WebElement nextBtn = driver.findElement(locators.get("redeemablePage.NextBtn"));
		nextBtn.click();
		utils.waitTillPagePaceDone();
		try {
			WebElement removeExistingQC = driver.findElement(locators.get("redeemablePage.removeExistingQC"));
			if (removeExistingQC.isDisplayed()) {
				removeExistingQC.click();
				WebElement existingQualifierOption = driver.findElement(locators.get("redeemablePage.existingQualifierOption"));
				existingQualifierOption.click();
				utils.clickByJSExecutor(driver, existingQualifierOption);
				logger.info("Removed Existing Qualifier");
				TestListeners.extentTest.get().info("Removed Existing Qualifier");
			}
		} catch (Exception e) {
			logger.info("Existing Qualifier is not applied");
			TestListeners.extentTest.get().info("Existing Qualifier is not applied");
		}
	}

	public void addQCinRedeemable(String QCname) {
		WebElement existingQualifierOption = driver.findElement(locators.get("redeemablePage.existingQualifierOption"));
		utils.waitTillElementToBeClickable(existingQualifierOption);
		utils.clickByJSExecutor(driver, existingQualifierOption);
		utils.waitTillPagePaceDone();
		WebElement chooseAReceiptRuleDropdown = driver.findElement(locators.get("redeemablePage.chooseAReceiptRuleDropdown"));
		Select sel = new Select(chooseAReceiptRuleDropdown);
		sel.selectByVisibleText(QCname);
		clickOnSecondButton();
//		utils.waitTillPagePaceDone();
		logger.info("Existing Qualifier is selected as - " + QCname);
		TestListeners.extentTest.get().info("Existing Qualifier is selected as - " + QCname);
	}

	public String getAuditLogOfRedeemable(String fieldName, String col) {
		WebElement redeemableAuditLogs = driver.findElement(By.xpath(utils.getLocatorValue("redeemablePage.redeemableAuditLogs")
				.replace("$fieldName", fieldName).replace("$col", col)));
		String fieldValue = redeemableAuditLogs.getText();
		logger.info("Audit log of " + fieldName + " is : " + fieldValue);
		TestListeners.extentTest.get().pass("Audit log of " + fieldName + " is : " + fieldValue);
		return fieldValue;
	}

	public void selectRedeemableEllipsisOption(String redeemableName, String option) {
		driver.findElement(
				By.xpath(utils.getLocatorValue("redeemablePage.redeemableLink").replace("temp", redeemableName)))
				.isDisplayed();
		driver.findElement(
				By.xpath(utils.getLocatorValue("redeemablePage.redeemableLink").replace("temp", redeemableName)))
				.click();
		WebElement ellipsisMenuButton = driver.findElement(locators.get("redeemablePage.ellipsisMenuButton"));
		ellipsisMenuButton.isDisplayed();
		ellipsisMenuButton.click();

		driver.findElement(
				By.xpath(utils.getLocatorValue("redeemablePage.redeemableEllipsisOption").replace("$option", option)))
				.isDisplayed();
		driver.findElement(
				By.xpath(utils.getLocatorValue("redeemablePage.redeemableEllipsisOption").replace("$option", option)))
				.click();
		logger.info("Successfully click on " + option + " ellipsis option for redeemable: " + redeemableName);
		TestListeners.extentTest.get()
				.info("Successfully click on " + option + " ellipsis option for redeemable: " + redeemableName);
	}

	public void addRedeemingLocation(String location) throws InterruptedException {
		// click additional settings
		dashboardpage.checkBoxFlagOnOffAndClick("redeemable_additional_steps", "check");
		logger.info("clicked additional settings");
		TestListeners.extentTest.get().info("clicked additional settings");

		WebElement redeemingLocationsDrpDwn = driver.findElement(locators.get("redeemablePage.redeemingLocationsDrpDwn"));
		utils.waitTillElementToBeClickable(redeemingLocationsDrpDwn);
		// utils.clickByJSExecutor(driver,utils.getLocator("redeemablePage.redeemingLocationsDrpDwn"));
		redeemingLocationsDrpDwn.click();
		List<WebElement> redeemingLocationsList = driver.findElements(locators.get("redeemablePage.redeemingLocationsList"));
		utils.selectListDrpDwnValue(redeemingLocationsList, location);
		logger.info("Selected location is : " + location);
		TestListeners.extentTest.get().pass("Selected location is : " + location);
	}

	public boolean verifyArchivedLocationInRedeemingLocationsDrpDwn(String location) {
		boolean flag = false;

		WebElement redeemingLocationsDrpDwn = driver.findElement(locators.get("redeemablePage.redeemingLocationsDrpDwn"));
		utils.waitTillElementToBeClickable(redeemingLocationsDrpDwn);
		// utils.clickByJSExecutor(driver,utils.getLocator("redeemablePage.redeemingLocationsDrpDwn"));
		redeemingLocationsDrpDwn.click();
		List<WebElement> redeemingLocationsList = driver.findElements(locators.get("redeemablePage.redeemingLocationsList"));
		for (WebElement wEle : redeemingLocationsList) {
			if (wEle.getText().contains(location)) {
				flag = true;
				break;
			}
		}
		utils.longWaitInSeconds(2);
		redeemingLocationsDrpDwn.click();
		return flag;

	}

	public boolean isFieldPresent(String fieldName) {
		String xpath = utils.getLocatorValue("redeemablePage.redeemableValidityField").replace("$fieldName", fieldName);
		WebElement field = driver.findElement(By.xpath(xpath));
		if (field != null && field.isDisplayed()) {
			return true; // Field is present and displayed
		} else {
			return false; // Field is not present or not displayed
		}
	}

	public void clickrunIndefinitelyToggle() {
		String toggleXpath = utils.getLocatorValue("redeemablePage.allowthisredeemabletorunindefinitely");
		WebElement toggleButton = driver.findElement(By.xpath(toggleXpath));
		toggleButton.isDisplayed();
		toggleButton.click();
		logger.info("Clicked on Run Indefinitely Toggle");
		TestListeners.extentTest.get().info("Clicked on Run Indefinitely Toggle");
	}

	public List<String> getRedeemingLocationList() throws InterruptedException {
		// click additional settings
		dashboardpage.checkBoxFlagOnOffAndClick("redeemable_additional_steps", "check");
		logger.info("clicked additional settings");
		TestListeners.extentTest.get().info("clicked additional settings");
		WebElement redeemingLocationsDrpDwn = driver.findElement(locators.get("redeemablePage.redeemingLocationsDrpDwn"));
		utils.waitTillElementToBeClickable(redeemingLocationsDrpDwn);
		// utils.clickByJSExecutor(driver,utils.getLocator("redeemablePage.redeemingLocationsDrpDwn"));
		redeemingLocationsDrpDwn.click();
		List<WebElement> redeemingLocationGroupList = driver.findElements(locators.get("redeemablePage.redeemingLocationGroupList"));
		List<String> locationList = new ArrayList<String>();
		int col = redeemingLocationGroupList.size();
		for (int i = 0; i < col; i++) {
			String val = redeemingLocationGroupList.get(i).getText();
			locationList.add(val);
		}
		logger.info("Locations in redeeming location dropdown are : " + locationList);
		TestListeners.extentTest.get().pass("Locations in redeeming location dropdown are : " + locationList);
		return locationList;
	}

	public void createRedeemableWithFlatDiscountAndEndDate(String redeemableName, String flatDiscountAmount,
			String endDateTime, String timezone) throws InterruptedException {
		// Click "New Redeemable" and enter name
		WebElement newRedeemableButton = driver.findElement(locators.get("redeemablePage.newRedeemableButton"));
		newRedeemableButton.click();
		utils.waitTillPagePaceDone();
		WebElement redeemableNameTextbox = driver.findElement(locators.get("redeemablePage.redeemableNameTextbox"));
		redeemableNameTextbox.clear();
		redeemableNameTextbox.sendKeys(redeemableName);
		WebElement nextButton = driver.findElement(locators.get("redeemablePage.nextButton"));
		nextButton.click();
		utils.waitTillPagePaceDone();

		// Select Flat Discount and enter amount
		WebElement flatRadioButton = driver.findElement(locators.get("redeemablePage.flatRadioButton"));
		flatRadioButton.click();
		WebElement flatDiscountTextbox = driver.findElement(locators.get("redeemablePage.flatDiscountTextbox"));
		flatDiscountTextbox.clear();
		flatDiscountTextbox.sendKeys(flatDiscountAmount);
		WebElement secondNextButton = driver.findElement(locators.get("redeemablePage.secondNextButton"));
		secondNextButton.click();
		utils.waitTillPagePaceDone();

		// Set end date
		endTime(endDateTime);
		// utils.getLocator("redeemablePage.redeemableEndTime").clear();
		// utils.getLocator("redeemablePage.redeemableEndTime").sendKeys(endDateTime);

		// Set timezone to (GMT+05:30) New Delhi ( IST )
		WebElement clickOnTimezoneDrpSown = driver.findElement(locators.get("redeemablePage.clickOnTimezoneDrpSown"));
		clickOnTimezoneDrpSown.click();
		List<WebElement> timezoneList = driver.findElements(locators.get("redeemablePage.timezoneList"));
		utils.selectListDrpDwnValue(timezoneList, timezone);

		// Finish creation
		WebElement finishButton = driver.findElement(locators.get("redeemablePage.finishButton"));
		finishButton.click();
		utils.waitTillPagePaceDone();

		logger.info("Created redeemable '" + redeemableName + "' with flat discount " + flatDiscountAmount
				+ ", end date " + endDateTime + ", timezone (GMT+05:30) New Delhi ( IST )");
		TestListeners.extentTest.get().pass("Created redeemable '" + redeemableName + "' with flat discount "
				+ flatDiscountAmount + ", end date " + endDateTime + ", timezone (GMT+05:30) New Delhi ( IST )");
	}

	public void createRedeemableWithFlatDiscountAndExpiryInDays(String redeemableName, String flatDiscountAmount,
			String days, String timezone) throws InterruptedException {
		// Click "New Redeemable" and enter name
		WebElement newRedeemableButton = driver.findElement(locators.get("redeemablePage.newRedeemableButton"));
		newRedeemableButton.click();
		utils.waitTillPagePaceDone();
		WebElement redeemableNameTextbox = driver.findElement(locators.get("redeemablePage.redeemableNameTextbox"));
		redeemableNameTextbox.clear();
		redeemableNameTextbox.sendKeys(redeemableName);
		WebElement nextButton = driver.findElement(locators.get("redeemablePage.nextButton"));
		nextButton.click();
		utils.waitTillPagePaceDone();

		// Select Flat Discount and enter amount
		WebElement flatRadioButton = driver.findElement(locators.get("redeemablePage.flatRadioButton"));
		flatRadioButton.click();
		WebElement flatDiscountTextbox = driver.findElement(locators.get("redeemablePage.flatDiscountTextbox"));
		flatDiscountTextbox.clear();
		flatDiscountTextbox.sendKeys(flatDiscountAmount);
		WebElement secondNextButton = driver.findElement(locators.get("redeemablePage.secondNextButton"));
		secondNextButton.click();
		utils.waitTillPagePaceDone();

		expiryInDays("", days); // Clear any existing expiry
		// Set end date
		// utils.getLocator("redeemablePage.redeemableEndTime").clear();
		// utils.getLocator("redeemablePage.redeemableEndTime").sendKeys(endDateTime);

		// Set timezone to (GMT+05:30) New Delhi ( IST )
		WebElement clickOnTimezoneDrpSown = driver.findElement(locators.get("redeemablePage.clickOnTimezoneDrpSown"));
		clickOnTimezoneDrpSown.click();
		List<WebElement> timezoneList = driver.findElements(locators.get("redeemablePage.timezoneList"));
		utils.selectListDrpDwnValue(timezoneList, timezone);

		// Finish creation
		WebElement finishButton = driver.findElement(locators.get("redeemablePage.finishButton"));
		finishButton.click();
		utils.waitTillPagePaceDone();

		logger.info("Created redeemable '" + redeemableName + "' with flat discount " + flatDiscountAmount
				+ ", expiry in days " + days + ", timezone (GMT+05:30) New Delhi ( IST )");
		TestListeners.extentTest.get().pass("Created redeemable '" + redeemableName + "' with flat discount "
				+ flatDiscountAmount + ", expiry in days " + days + ", timezone (GMT+05:30) New Delhi ( IST )");
	}
	 public void verifyAndroidOfferPassExpiryDate() {
	        try {
	            // Calculate the expected expiry date (7 days from today)
	        //    LocalDate today = LocalDate.now();
	        //    LocalDate expectedExpiryDate = today.plusDays(7);
	        //    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // Adjust format as per your application
	        //    String expectedExpiryDateStr = expectedExpiryDate.format(formatter);
	        //    logger.info("Expected expiry date: " + expectedExpiryDateStr);

	       
	            // Locate the expiry date element using the provided XPath
	        	
	        	WebElement expiryDate = driver.findElement(locators.get("redeemablePage.expiryDate"));
	        	String actualExpiryDate = expiryDate.getText();
	       //     WebElement expiryDateElement = driver.findElement(By.xpath("//div[text()='Expires']/following-sibling::div"));

	            // Extract the text of the expiry date
	         //   String actualExpiryDate = expiryDateElement.getText();
	            logger.info("Actual expiry date on the pass: " + actualExpiryDate);

	            // Verify the expiry date matches the expected value
	       //     Assert.assertEquals(actualExpiryDate, expectedExpiryDateStr, "Expiry date does not match!");
	        //    logger.info("Expiry date verification passed. Expected: " + expectedExpiryDateStr + ", Actual: " + actualExpiryDate);

	        } catch (Exception e) {
	            logger.error("Error while verifying expiry date on the pass: " + e.getMessage(), e);
	            Assert.fail("Test failed due to an exception.");
	        }
	    }
	}
