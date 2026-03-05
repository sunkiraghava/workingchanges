package com.punchh.server.pages;

import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;
import org.testng.annotations.Listeners;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class JourneysPage {
	static Logger logger = LogManager.getLogger(MenuPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;

	public JourneysPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
	}

	public void clickOnCreateJourneyAndSetName(String journeyName) {
		utils.getLocator("journeysPage.newJourneyButton").isDisplayed();
		utils.getLocator("journeysPage.newJourneyButton").click();
		logger.info("Clicked create new journey");
		TestListeners.extentTest.get().info("Clicked create new journey");
		utils.getLocator("journeysPage.journeyNameTextbox").clear();
		utils.getLocator("journeysPage.journeyNameTextbox").sendKeys(journeyName);
		utils.getLocator("journeysPage.setNameCheckIcon").isDisplayed();
		utils.getLocator("journeysPage.setNameCheckIcon").click();
		logger.info("== Journey name is set as: " + journeyName + " ==");
		TestListeners.extentTest.get().info("== Journey name is set as: " + journeyName + " ==");
	}

	public void DragSendEmailToBlock() {
		try {
			utils.getLocator("journeysPage.emailEnvelopeIcon").isDisplayed();
			utils.getLocator("journeysPage.startBlockLabel").isDisplayed();
			selUtils.dragAndDropWithJSExecutor(utils.getLocator("journeysPage.emailEnvelopeIcon"),
					utils.getLocator("journeysPage.startBlockLabel"));
			utils.getLocator("journeysPage.sendEmailCardHeading").isDisplayed();
			logger.info("Dragged send email to start block");
			TestListeners.extentTest.get().info("Dragged send email to start block");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void DragRedeemOfferToBlock() {
		try {
			utils.getLocator("journeysPage.redeemOfferIcon").isDisplayed();
			utils.getLocator("journeysPage.startBlockLabel").isDisplayed();
			selUtils.dragAndDropWithJSExecutor(utils.getLocator("journeysPage.redeemOfferIcon"),
					utils.getLocator("journeysPage.startBlockLabel"));
			utils.getLocator("journeysPage.redeemOfferHeader").isDisplayed();
			logger.info("Dragged redeem offer to start block");
			TestListeners.extentTest.get().info("Dragged redeem offer to start block");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void DragMadePurchaseToBlock() {
		try {
			utils.getLocator("journeysPage.madePurchaseIcon").isDisplayed();
			utils.getLocator("journeysPage.startBlockLabel").isDisplayed();
			selUtils.dragAndDropWithJSExecutor(utils.getLocator("journeysPage.madePurchaseIcon"),
					utils.getLocator("journeysPage.startBlockLabel"));
			utils.getLocator("journeysPage.madePurchaseHeader").isDisplayed();
			logger.info("Dragged Made A Purchse to start block");
			TestListeners.extentTest.get().info("Dragged Made A Purchse to start block");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void enterSendEmailDetails(String subject, String preHeaderText, String segmentOption,
			String redeemableValue, String startDate, String endDate, String startTime, String endTime,
			String timezone) {
		try {
			utils.getLocator("journeysPage.subjectTextbox").isDisplayed();
			utils.getLocator("journeysPage.subjectTextbox").clear();
			utils.getLocator("journeysPage.subjectTextbox").sendKeys(subject);
			logger.info("Subject is set as: " + subject);
			TestListeners.extentTest.get().info("Subject is set as: " + subject);
			utils.getLocator("journeysPage.preheaderTextbox").isDisplayed();
			utils.getLocator("journeysPage.preheaderTextbox").clear();
			utils.getLocator("journeysPage.preheaderTextbox").sendKeys(preHeaderText);
			logger.info("Preheader text is set as: " + subject);
			TestListeners.extentTest.get().info("Preheader text is set as: " + subject);
			utils.getLocator("journeysPage.segmentDropdownButton").isDisplayed();
			utils.getLocator("journeysPage.segmentDropdownButton").click();
			// SegmentDropdown
			driver.findElement(
					By.xpath(utils.getLocatorValue("journeysPage.formDropdown").replace("temp", segmentOption)))
					.click();
			logger.info("Segment value is set as: " + segmentOption);
			TestListeners.extentTest.get().info("Segment value is set as: " + segmentOption);
			utils.getLocator("journeysPage.includeRedeemableCheckbox").isDisplayed();
			utils.getLocator("journeysPage.includeRedeemableCheckbox").click();
			utils.getLocator("journeysPage.redeemableDropdownButton").isDisplayed();
			utils.getLocator("journeysPage.redeemableDropdownButton").click();
			driver.findElement(By
					.xpath(utils.getLocatorValue("journeysPage.redeemableValueLabel").replace("temp", redeemableValue)))
					.click();
			logger.info("Redeemable value is set as: " + redeemableValue);
			TestListeners.extentTest.get().info("Redeemable value is set as: " + redeemableValue);
			selectDateTimeAndTimezone(startDate, endDate, startTime, endTime, timezone);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println(e);
			e.printStackTrace();
		}
	}

	public void clickOnCreateSendEmail() {
		utils.getLocator("journeysPage.createButton").isDisplayed();
		utils.getLocator("journeysPage.createButton").click();
		logger.info("Added Send Email start block details");
		TestListeners.extentTest.get().info("Added Send Email start block details");
	}

	public void setRepeatBehaviourAsNever() {
		selUtils.scrollToElement(utils.getLocator("journeysPage.unlimitedEntryLimit"));
		utils.getLocator("journeysPage.neverRepeatRadioButton").isDisplayed();
		utils.getLocator("journeysPage.neverRepeatRadioButton").click();
		logger.info("Repeat beaviour is set as Never repeat");
		TestListeners.extentTest.get().info("Repeat beaviour is set as Never repeat");
	}

	public void setAnyNumberOfIndividualEntryLimit() {
		selUtils.scrollToElement(utils.getLocator("journeysPage.unlimitedEntryLimit"));
		utils.getLocator("journeysPage.unlimitedEntryLimit").isDisplayed();
		utils.getLocator("journeysPage.unlimitedEntryLimit").click();
		logger.info("Individual Entry Limit is set as any number of times");
		TestListeners.extentTest.get().info("Individual Entry Limit is set as any number of times");
	}

	public void setRepertBehaviourCount(String repeatBehavioutCount) {
		utils.getLocator("journeysPage.repeatBehavioutCount").isDisplayed();
		utils.getLocator("journeysPage.repeatBehavioutCount").clear();
		utils.getLocator("journeysPage.repeatBehavioutCount").sendKeys(repeatBehavioutCount);
		logger.info("Repeat beaviour count is set as: " + repeatBehavioutCount + " days");
		TestListeners.extentTest.get().info("Repeat beaviour count is set as: " + repeatBehavioutCount + " days");
	}

	public void setIndividualEntryLimit(String individualEntryTimes, String entryEveryCount,
			String entryEveryDuration) {
		try {
			utils.getLocator("journeysPage.individualEntryTimesInput").isDisplayed();
			utils.getLocator("journeysPage.individualEntryTimesInput").clear();
			utils.getLocator("journeysPage.individualEntryTimesInput").sendKeys(individualEntryTimes);
			utils.getLocator("journeysPage.individualEntryEveryCheckbox").isDisplayed();
			utils.getLocator("journeysPage.individualEntryEveryCheckbox").click();
			utils.getLocator("journeysPage.individualEntryEveryInput").isDisplayed();
			utils.getLocator("journeysPage.individualEntryEveryInput").clear();
			utils.getLocator("journeysPage.individualEntryEveryInput").sendKeys(entryEveryCount);
			utils.getLocator("journeysPage.individualEntryEveryDropdownButton").isDisplayed();
			utils.getLocator("journeysPage.individualEntryEveryDropdownButton").click();
			// individualEntryEveryDuration
			driver.findElement(By.xpath(utils.getLocatorValue("journeysPage.individualEntryEveryDuration")
					.replace("temp", entryEveryDuration))).isDisplayed();
			driver.findElement(By.xpath(utils.getLocatorValue("journeysPage.individualEntryEveryDuration")
					.replace("temp", entryEveryDuration))).click();
			;
			logger.info("Redeemable value is set as: " + individualEntryTimes + " times, every " + entryEveryCount + " "
					+ entryEveryDuration);
			TestListeners.extentTest.get().info("Redeemable value is set as: " + individualEntryTimes + " times, every "
					+ entryEveryCount + " " + entryEveryDuration);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void selectDateTimeAndTimezone(String startDate, String endDate, String startTime, String endTime,
			String timezone) {
		utils.getLocator("journeysPage.startDateButton").isDisplayed();
		utils.getLocator("journeysPage.startDateButton").click();
		driver.findElement(By.xpath(utils.getLocatorValue("journeysPage.dayLabel").replace("temp", startDate)))
				.isDisplayed();
		driver.findElement(By.xpath(utils.getLocatorValue("journeysPage.dayLabel").replace("temp", startDate))).click();
		logger.info("Start date is set as: " + startDate);
		TestListeners.extentTest.get().info("Start date is set as: " + startDate);
		selUtils.longWait(2000);
		utils.getLocator("journeysPage.endDateButton").isDisplayed();
		utils.getLocator("journeysPage.endDateButton").click();
		driver.findElement(By.xpath(utils.getLocatorValue("journeysPage.dayLabel").replace("temp", endDate)))
				.isDisplayed();
		driver.findElement(By.xpath(utils.getLocatorValue("journeysPage.dayLabel").replace("temp", endDate))).click();
		logger.info("End date is set as: " + endDate);
		TestListeners.extentTest.get().info("End date is set as: " + endDate);
		utils.getLocator("journeysPage.startTimeButton").isDisplayed();
		utils.getLocator("journeysPage.startTimeButton").click();
		// String tempStartTime =
		// utils.getLocatorValue("journeysPage.startTimeLabel").replace("temp",
		// startTime);
		driver.findElement(By.xpath(utils.getLocatorValue("journeysPage.startTimeLabel").replace("temp", startTime)))
				.click();
		logger.info("Start time is set as: " + startTime);
		TestListeners.extentTest.get().info("Start time is set as: " + startTime);
		utils.getLocator("journeysPage.endTimeButton").isDisplayed();
		utils.getLocator("journeysPage.endTimeButton").click();
		// String tempEndTime =
		// utils.getLocatorValue("journeysPage.endTimeLabel").replace("temp", endTime);
		driver.findElement(By.xpath(utils.getLocatorValue("journeysPage.endTimeLabel").replace("temp", endTime)))
				.click();
		logger.info("End time is set as: " + endTime);
		TestListeners.extentTest.get().info("End time is set as: " + endTime);
		utils.getLocator("journeysPage.timezoneStartButton").isDisplayed();
		utils.getLocator("journeysPage.timezoneStartButton").click();
		String timeZoneLabel = utils.getLocatorValue("journeysPage.timeZoneLabel").replace("temp", timezone);
		driver.findElement(By.xpath(timeZoneLabel.replace("index", "1"))).click();
		logger.info("Start timezone is set as: " + timezone);
		TestListeners.extentTest.get().info("Start timezone is set as: " + timezone);
		utils.getLocator("journeysPage.timezoneEndButton").isDisplayed();
		utils.getLocator("journeysPage.timezoneEndButton").click();
		String timeZoneEndLabel = utils.getLocatorValue("journeysPage.timeZoneLabel").replace("temp", timezone);
		driver.findElement(By.xpath(timeZoneEndLabel.replace("index", "3"))).click();
		logger.info("End timezone is set as: " + timezone);
		TestListeners.extentTest.get().info("End timezone is set as: " + timezone);
	}

	public void selectExitIcon() {
		try {
			logger.info("Selecting exit icon");
			TestListeners.extentTest.get().info("Selecting exit icon");
			utils.getLocator("journeysPage.canvasLabel").isDisplayed();
			utils.getLocator("journeysPage.exitIcon").isDisplayed();
			selUtils.dragAndDropWithJSExecutor(utils.getLocator("journeysPage.exitIcon"),
					utils.getLocator("journeysPage.canvasLabel"));
			utils.getLocator("journeysPage.exitBlockLabel").isDisplayed();
			Actions action = new Actions(driver);
			action.clickAndHold(utils.getLocator("journeysPage.exitBlockLabel")).moveByOffset(700, 200).release()
					.build().perform();
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		}
	}

	public void connectSendEmailAndExitBlock() {
		try {
			utils.getLocator("journeysPage.sendEmailBlockLabel").isDisplayed();
			selUtils.mouseHoverOverElement(utils.getLocator("journeysPage.sendEmailBlockLabel"));
			utils.getLocator("journeysPage.sendEmailArrowCircleLabel").isDisplayed();
			selUtils.clickAndHoldAndMove(utils.getLocator("journeysPage.sendEmailArrowCircleLabel"),
					utils.getLocator("journeysPage.exitBlockLabel"));
			utils.getLocator("journeysPage.startToEndConnectingLineLabel").isDisplayed();
			logger.info("Connected start and end block");
			TestListeners.extentTest.get().info("Connected start and end block");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void connectSendEmailMiddleAndExitBlock() {
		try {
			utils.getLocator("journeysPage.sendEmailMiddleBlockLabel").isDisplayed();
			selUtils.mouseHoverOverElement(utils.getLocator("journeysPage.sendEmailMiddleBlockLabel"));
			utils.getLocator("journeysPage.sendEmailMiddleArrowCircleLabel").isDisplayed();
			selUtils.clickAndHoldAndMove(utils.getLocator("journeysPage.sendEmailMiddleArrowCircleLabel"),
					utils.getLocator("journeysPage.exitBlockLabel"));
			utils.getLocator("journeysPage.MiddelToEndConnectingLineLabel").isDisplayed();
			logger.info("Connected start and end block");
			TestListeners.extentTest.get().info("Connected middle and end block");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void enterReddemOfferDetails(String redeemable, String couponOffer, String segmentOption, String location,
			String startDate, String endDate, String startTime, String endTime, String timezone, String purchaseMethod,
			String timeOfDay, String dayOfWeek) {
		try {
			utils.getLocator("journeysPage.reedemptionCheckbox").isDisplayed();
			utils.getLocator("journeysPage.reedemptionCheckbox").click();
			utils.getLocator("journeysPage.reedemptionDropdownButton").isDisplayed();
			utils.getLocator("journeysPage.reedemptionDropdownButton").click();
			driver.findElement(By.xpath(utils.getLocatorValue("journeysPage.formDropdown").replace("temp", redeemable)))
					.click();
			logger.info("Redeemable value is set as: " + redeemable);
			TestListeners.extentTest.get().info("Redeemable value is set as: " + redeemable);
			// useful code coupon offer
//			utils.getLocator("journeysPage.couponOfferCheckbox").isDisplayed();
//			utils.getLocator("journeysPage.couponOfferCheckbox").click();
//			utils.getLocator("journeysPage.couponOfferDropdownButton").isDisplayed();
//			utils.getLocator("journeysPage.couponOfferDropdownButton").click();
//			driver.findElement(
//					By.xpath(utils.getLocatorValue("journeysPage.formDropdown").replace("temp", couponOffer)))
//					.click();

			utils.getLocator("journeysPage.segmentDropdownButton").isDisplayed();
			utils.getLocator("journeysPage.segmentDropdownButton").click();
			driver.findElement(
					By.xpath(utils.getLocatorValue("journeysPage.formDropdown").replace("temp", segmentOption)))
					.click();
			logger.info("Segment value is set as: " + segmentOption);
			TestListeners.extentTest.get().info("Segment value is set as: " + segmentOption);
			utils.getLocator("journeysPage.locationDropdown").isDisplayed();
			utils.getLocator("journeysPage.locationDropdown").click();
			driver.findElement(By.xpath(utils.getLocatorValue("journeysPage.spanTextLabel").replace("temp", location)))
					.click();
			logger.info("Location value is set as: " + location);
			TestListeners.extentTest.get().info("Location value is set as: " + location);
			driver.findElement(By.xpath(
					utils.getLocatorValue("journeysPage.purchaseMethodCheckbox").replace("temp", purchaseMethod)))
					.click();
			logger.info("Purchase method is set as: " + purchaseMethod);
			TestListeners.extentTest.get().info("Purchase method is set as: " + purchaseMethod);
			utils.getLocator("journeysPage.timeOfDayDropdownButton").isDisplayed();
			utils.getLocator("journeysPage.timeOfDayDropdownButton").click();
			driver.findElement(By.xpath(utils.getLocatorValue("journeysPage.formDropdown").replace("temp", timeOfDay)))
					.click();
			driver.findElement(By.xpath(utils.getLocatorValue("journeysPage.formDropdown").replace("temp", dayOfWeek)))
					.click();
			logger.info("Time of day is set as: " + timeOfDay + ", Day of week is set as: " + dayOfWeek);
			TestListeners.extentTest.get()
					.info("Time of day is set as: " + timeOfDay + ", Day of week is set as: " + dayOfWeek);
			selectDateTimeAndTimezone(startDate, endDate, startTime, endTime, timezone);
			utils.getLocator("journeysPage.unlimtedEntryLimitRadioBtn").isDisplayed();
			utils.getLocator("journeysPage.unlimtedEntryLimitRadioBtn").click();
			utils.getLocator("journeysPage.createButton").isDisplayed();
			utils.getLocator("journeysPage.createButton").click();
			utils.getLocator("journeysPage.redeemStartBlockLabel").isDisplayed();
			logger.info("Added redeem offer start block details");
			TestListeners.extentTest.get().info("Added redeem offer start block details");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println(e);
			e.printStackTrace();
		}
	}

	public void dragSendEmailToMiddle() {
		utils.getLocator("journeysPage.canvasLabel").isDisplayed();
		utils.getLocator("journeysPage.emailEnvelopeIcon").isDisplayed();
		if (utils.getLocatorList("journeysPage.emailEnvelopeIcon").size() > 1) {
			selUtils.dragAndDropWithJSExecutor(utils.getLocatorList("journeysPage.emailEnvelopeIcon").get(1),
					utils.getLocator("journeysPage.canvasLabel"));
		} else {
			selUtils.dragAndDropWithJSExecutor(utils.getLocator("journeysPage.emailEnvelopeIcon"),
					utils.getLocator("journeysPage.canvasLabel"));
		}
		utils.getLocator("journeysPage.createButton").isDisplayed();
		utils.getLocator("journeysPage.createButton").click();
		utils.getLocator("journeysPage.sendEmailMiddleBlockLabel").isDisplayed();
		Actions action = new Actions(driver);
		action.clickAndHold(utils.getLocator("journeysPage.sendEmailMiddleBlockLabel")).moveByOffset(400, 50).release()
				.build().perform();
		utils.getLocator("journeysPage.sendEmailMiddleBlockLabel").click();
		utils.getLocator("journeysPage.sendEmailCardHeading").isDisplayed();
		logger.info("Dragged send email to middle");
		TestListeners.extentTest.get().info("Dragged send email to middle");
	}

	public void enterSendEmailMiddleBlockDetails(String subject, String preHeaderText) {
		try {
			utils.getLocator("journeysPage.subjectTextbox").isDisplayed();
			utils.getLocator("journeysPage.subjectTextbox").clear();
			utils.getLocator("journeysPage.subjectTextbox").sendKeys(subject);
			logger.info("Subject is set as: " + subject);
			TestListeners.extentTest.get().info("Subject is set as: " + subject);
			utils.getLocator("journeysPage.preheaderTextbox").isDisplayed();
			utils.getLocator("journeysPage.preheaderTextbox").clear();
			utils.getLocator("journeysPage.preheaderTextbox").sendKeys(preHeaderText);
			logger.info("Preheader text is set as: " + subject);
			TestListeners.extentTest.get().info("Preheader text is set as: " + subject);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void createMiddleSendEmail() {
		utils.getLocator("journeysPage.createButton").isDisplayed();
		utils.getLocator("journeysPage.createButton").click();
		utils.getLocator("journeysPage.sendEmailMiddleBlockLabel").isDisplayed();
		logger.info("Added send email middle block details");
		TestListeners.extentTest.get().info("Added send email middle block details");
	}

//	"sendEmailOnDateRadioButton": "//div[.='On date']//ancestor::div[3]//input/..",
//	"sendEmailOnDateCalenderButton": "//div[.='On date']/..//*[local-name()='svg'][@data-icon='calendar-alt']//*",
//	"sendEmailAfterRadioButton": "//div[.='After']//ancestor::div[3]//div[@class='radio_radio-container_2nWWz']",
//	"sendEmailAfterTextbox": "//div[.='After']//ancestor::div[2]//div[@class='text-box_text-box_2nRz6']",
//	"sendEmailAfterDropdown": "//div[.='After']//..//input[@class='dropdownSelect_header_input_1C4OQ']",
//	"dropdownLabel": "//div[@title='temp'][@class='dropdownSelect_select-item-text_1DrEk']",
//	"sendEmailatDropdownButton": "//div[.='At']//..//input[@class='dropdownSelect_header_input_1C4OQ']",
//	"onNextDayLabel"

	public void setSendThisEmailOnDate(String startDate) {
		// dayLabel
		try {
			utils.getLocator("journeysPage.sendEmailOnDateRadioButton").isDisplayed();
			utils.getLocator("journeysPage.sendEmailOnDateRadioButton").click();
			utils.getLocator("journeysPage.sendEmailOnDateCalenderButton").isDisplayed();
			// driver.findElement(By.xpath("//div[.='On
			// date']/..//*[local-name()='svg'][@data-icon='calendar-alt']")).click();
			utils.getLocator("journeysPage.sendEmailOnDateCalenderButton").click();
			driver.findElement(By.xpath(utils.getLocatorValue("journeysPage.dayLabel").replace("temp", startDate)))
					.isDisplayed();
			driver.findElement(By.xpath(utils.getLocatorValue("journeysPage.dayLabel").replace("temp", startDate)))
					.click();
			logger.info("Send this email on date: " + startDate);
			TestListeners.extentTest.get().info("Send this email on date: " + startDate);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println(e);
			e.printStackTrace();
		}
	}

	public void setSendThisEmailAfter(String sendEmailAfterCount, String sendEmailAfterDuration, String anyTimeValue) {
		// dayLabel
		try {
			utils.getLocator("journeysPage.sendEmailAfterRadioButton").isDisplayed();
			utils.getLocator("journeysPage.sendEmailAfterRadioButton").click();
			utils.getLocator("journeysPage.sendEmailAfterTextbox").isDisplayed();
			utils.getLocator("journeysPage.sendEmailAfterTextbox").sendKeys(sendEmailAfterCount);
			utils.getLocator("journeysPage.sendEmailAfterDropdown").isDisplayed();
			utils.getLocator("journeysPage.sendEmailAfterDropdown").click();
			driver.findElement(By
					.xpath(utils.getLocatorValue("journeysPage.dropdownLabel").replace("temp", sendEmailAfterDuration)))
					.isDisplayed();
			driver.findElement(By
					.xpath(utils.getLocatorValue("journeysPage.dropdownLabel").replace("temp", sendEmailAfterDuration)))
					.click();
			logger.info("Send this email after: " + sendEmailAfterCount + sendEmailAfterDuration);
			TestListeners.extentTest.get().info("Send this email on date: " + sendEmailAfterDuration);
			setAnytime(anyTimeValue);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void setAnytime(String anyTimeValue) {
		utils.getLocator("journeysPage.sendEmailatDropdownButton").isDisplayed();
		utils.getLocator("journeysPage.sendEmailatDropdownButton").click();
		driver.findElement(By.xpath(utils.getLocatorValue("journeysPage.dropdownLabel").replace("temp", anyTimeValue)))
				.isDisplayed();
		driver.findElement(By.xpath(utils.getLocatorValue("journeysPage.dropdownLabel").replace("temp", anyTimeValue)))
				.click();
		logger.info("At time set as: " + anyTimeValue);
		TestListeners.extentTest.get().info("At time set as: " + anyTimeValue);
	}

	public void setSendThisEmailOnNext(String sendEmailOnNextValue, String anyTimeValue) {
		driver.findElement(
				By.xpath(utils.getLocatorValue("journeysPage.onNextDayLabel").replace("temp", sendEmailOnNextValue)))
				.isDisplayed();
		driver.findElement(
				By.xpath(utils.getLocatorValue("journeysPage.onNextDayLabel").replace("temp", sendEmailOnNextValue)))
				.click();
		logger.info("Send this email on next: " + sendEmailOnNextValue);
		TestListeners.extentTest.get().info("Send this email on next: " + sendEmailOnNextValue);
		setAnytime(anyTimeValue);
	}

	public void connectRedeemOfferToSendEmail() {
		utils.getLocator("journeysPage.redeemStartBlockLabel").isDisplayed();
		selUtils.mouseHoverOverElement(utils.getLocator("journeysPage.redeemStartBlockLabel"));
		utils.getLocator("journeysPage.redeemOfferArrowCircleLabel").isDisplayed();
		selUtils.clickAndHoldAndMove(utils.getLocator("journeysPage.redeemOfferArrowCircleLabel"),
				utils.getLocator("journeysPage.sendEmailMiddleBlockLabel"));
		utils.getLocator("journeysPage.startToMiddelConnectingLineLabel").isDisplayed();
		logger.info("Connected start block Redeem Offer and mid block Send Email");
		TestListeners.extentTest.get().info("Connected start block Redeem Offer and mid block Send Email");
	}

	public void connectSendEmailToSendEmail() {
		// utils.getLocator("journeysPage.redeemStartBlockLabel").isDisplayed();
		selUtils.mouseHoverOverElement(utils.getLocator("journeysPage.sendEmailArrowCircleLabel"));
		utils.getLocator("journeysPage.sendEmailArrowCircleLabel").isDisplayed();
		selUtils.clickAndHoldAndMove(utils.getLocator("journeysPage.sendEmailArrowCircleLabel"),
				utils.getLocator("journeysPage.sendEmailMiddleBlockLabel"));
		utils.getLocator("journeysPage.startToMiddelConnectingLineLabel").isDisplayed();
		logger.info("Connected start block Redeem Offer and mid block Send Email");
		TestListeners.extentTest.get().info("Connected start block Redeem Offer and mid block Send Email");
	}

	public void connectMadeAPurchaseToSendEmail() {
		utils.getLocator("journeysPage.madePurchaseStartBlockLabel").isDisplayed();
		selUtils.mouseHoverOverElement(utils.getLocator("journeysPage.madePurchaseStartBlockLabel"));
		utils.getLocator("journeysPage.madePurchaseArrowCircleLabel").isDisplayed();
		selUtils.clickAndHoldAndMove(utils.getLocator("journeysPage.madePurchaseArrowCircleLabel"),
				utils.getLocator("journeysPage.sendEmailMiddleBlockLabel"));
		utils.getLocator("journeysPage.startToMiddelConnectingLineLabel").isDisplayed();
		logger.info("Connected start block Made A Purchase and mid block Send Email");
		TestListeners.extentTest.get().info("Connected start block Made A Purchase and mid block Send Email");
	}

	public void activateJourney() {
		utils.getLocator("journeysPage.activateJourneyButton").isDisplayed();
		utils.getLocator("journeysPage.activateJourneyButton").click();
		utils.getLocator("journeysPage.YesActivateButton").isDisplayed();
		utils.getLocator("journeysPage.YesActivateButton").click();
		logger.info("Activated the journey");
		TestListeners.extentTest.get().info("Activated the journey");
		// if()
	}

	public void enterMadePurchaseDetails(String segmentOption, String location, String startDate, String endDate,
			String startTime, String endTime, String timezone, String purchaseMethod, String timeOfDay,
			String dayOfWeek) {
		try {
			utils.getLocator("journeysPage.segmentDropdownButton").isDisplayed();
			utils.getLocator("journeysPage.segmentDropdownButton").click();
			driver.findElement(
					By.xpath(utils.getLocatorValue("journeysPage.formDropdown").replace("temp", segmentOption)))
					.click();
			logger.info("Segment value is set as: " + segmentOption);
			TestListeners.extentTest.get().info("Segment value is set as: " + segmentOption);
			utils.getLocator("journeysPage.locationDropdown").isDisplayed();
			utils.getLocator("journeysPage.locationDropdown").click();
			driver.findElement(By.xpath(utils.getLocatorValue("journeysPage.spanTextLabel").replace("temp", location)))
					.click();
			driver.findElement(By.xpath(
					utils.getLocatorValue("journeysPage.purchaseMethodCheckbox").replace("temp", purchaseMethod)))
					.click();
			utils.getLocator("journeysPage.timeOfDayDropdownButton").isDisplayed();
			utils.getLocator("journeysPage.timeOfDayDropdownButton").click();
			driver.findElement(By.xpath(utils.getLocatorValue("journeysPage.formDropdown").replace("temp", timeOfDay)))
					.click();
			driver.findElement(By.xpath(utils.getLocatorValue("journeysPage.formDropdown").replace("temp", dayOfWeek)))
					.click();
			logger.info("Time of day is set as: " + timeOfDay + ", Day of week is set as: " + dayOfWeek);
			TestListeners.extentTest.get()
					.info("Time of day is set as: " + timeOfDay + ", Day of week is set as: " + dayOfWeek);
			selectDateTimeAndTimezone(startDate, endDate, startTime, endTime, timezone);
			utils.getLocator("journeysPage.unlimtedEntryLimitRadioBtn").isDisplayed();
			utils.getLocator("journeysPage.unlimtedEntryLimitRadioBtn").click();
			utils.getLocator("journeysPage.createButton").isDisplayed();
			utils.getLocator("journeysPage.createButton").click();
			utils.getLocator("journeysPage.madePurchaseStartBlockLabel").isDisplayed();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println(e);
			e.printStackTrace();
		}
	}

	public void verifyJourneyRedirectedToListingPage(String journeysName) {
		driver.findElement(
				By.xpath(utils.getLocatorValue("journeysPage.journeyNameLabel").replace("temp", journeysName)))
				.isDisplayed();
		logger.info("Sucessfully verfied created journey: " + journeysName);
		TestListeners.extentTest.get().pass("Sucessfully verfied created journey: " + journeysName);
	}

	public void test001() {
		driver.findElement(By.xpath("//span[.='Made A Purchase']/ancestor::div[@class='icon-label_container_3rOtx']"))
				.isDisplayed();

		selUtils.dragAndDropWithJSExecutor(
				driver.findElement(
						By.xpath("//span[.='Made A Purchase']/ancestor::div[@class='icon-label_container_3rOtx']")),
				utils.getLocator("journeysPage.canvasLabel"));
		utils.getLocator("journeysPage.createButton").isDisplayed();
		utils.getLocator("journeysPage.createButton").click();
		Actions action = new Actions(driver);
		action.clickAndHold(
				driver.findElement(By.xpath("//*[local-name() = 'svg']//*[normalize-space()='Made A Purchase']")))
				.moveByOffset(200, 40).release().build().perform();

		dragSendEmailToMiddle();
		dragSendEmailToMiddle1();
	}

	public void dragSendEmailToMiddle1() {
		utils.getLocator("journeysPage.canvasLabel").isDisplayed();
		utils.getLocator("journeysPage.emailEnvelopeIcon").isDisplayed();
		if (utils.getLocatorList("journeysPage.emailEnvelopeIcon").size() > 1) {
			selUtils.dragAndDropWithJSExecutor(utils.getLocatorList("journeysPage.emailEnvelopeIcon").get(1),
					utils.getLocator("journeysPage.canvasLabel"));
		} else {
			selUtils.dragAndDropWithJSExecutor(utils.getLocator("journeysPage.emailEnvelopeIcon"),
					utils.getLocator("journeysPage.canvasLabel"));
		}
		utils.getLocator("journeysPage.createButton").isDisplayed();
		utils.getLocator("journeysPage.createButton").click();
		utils.getLocator("journeysPage.sendEmailMiddleBlockLabel").isDisplayed();
		Actions action = new Actions(driver);
		action.clickAndHold(utils.getLocator("journeysPage.sendEmailMiddleBlockLabel")).moveByOffset(600, 60).release()
				.build().perform();

		ConnectMadePurchase();
		// utils.getLocator("journeysPage.sendEmailMiddleBlockLabel").click();
//		utils.getLocator("journeysPage.sendEmailCardHeading").isDisplayed();
//		logger.info("Dragged send email to middle");
//		TestListeners.extentTest.get().info("Dragged send email to middle");

		// driver.findElement(By.xpath("//*[local-name() =
		// 'svg']//*[normalize-space()='Yes']"));
	}

	public void ConnectMadePurchase() {
		try {
			Actions action = new Actions(driver);
//			action.moveToElement(driver.findElement(By.xpath(
//					"//*[local-name() = 'svg']//*[normalize-space()='Made A Purchase']/../..//*[local-name()='circle'][@class='journeyBlock_connector_OojYD']")))
//					.build().perform();

			// *[local-name() = 'svg']//*[normalize-space()='Yes']

			selUtils.mouseHoverOverElement(driver
					.findElement(By.xpath("//*[local-name() = 'svg']//*[normalize-space()='Made A Purchase']/..")));

			// selUtils.mouseHoverOverElement(utils.getLocator("journeysPage.redeemStartBlockLabel"));
			action.clickAndHold(driver.findElement(By.xpath(
					"//*[local-name() = 'svg']//*[normalize-space()='Made A Purchase']/../..//*[local-name()='circle'][@class='journeyBlock_connector_OojYD']")))
					.moveToElement(
							driver.findElement(By.xpath("//*[local-name() = 'svg']//*[normalize-space()='Yes']")))
					.moveToElement(utils.getLocator("journeysPage.sendEmailMiddleBlockLabel")).release().perform();
//
//			Actions action1 = new Actions(driver);
//			action1.clickAndHold(driver.findElement(By.xpath("//*[local-name() = 'svg']//*[normalize-space()='Yes']")))
//					.moveToElement(utils.getLocator("journeysPage.sendEmailMiddleBlockLabel")).release().build()
//					.perform();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
