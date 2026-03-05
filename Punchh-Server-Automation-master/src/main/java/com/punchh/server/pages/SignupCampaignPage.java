package com.punchh.server.pages;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class SignupCampaignPage {

	static Logger logger = LogManager.getLogger(SignupCampaignPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	CreateDateTime createDateTime;
	private PageObj pageObj;

	private Map<String, By> locators;

	public SignupCampaignPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		createDateTime = new CreateDateTime();
		pageObj = new PageObj(driver);
		locators = utils.getAllByMap();
	}

	public void createWhatDetailsSignupCampaign(String name, String giftType, String giftreason, String redemable)
			throws InterruptedException {

		driver.findElement(locators.get("signupCampaignsPage.signupCampaignName")).clear();
		WebElement signupCampaignName = driver.findElement(locators.get("signupCampaignsPage.signupCampaignName"));
		signupCampaignName.sendKeys(name);
		logger.info("Entered campaign name: " + name);
		TestListeners.extentTest.get().pass("Entered campaign name: " + name);
		WebElement giftTypeDrp = driver.findElement(locators.get("signupCampaignsPage.giftTypeDrp"));
		giftTypeDrp.click();

		List<WebElement> giftTypeList = driver.findElements(locators.get("signupCampaignsPage.giftTypeList"));
		utils.selectListDrpDwnValue(giftTypeList, giftType);
		WebElement giftReason = driver.findElement(locators.get("signupCampaignsPage.giftReason"));
		giftReason.click();
		driver.findElement(locators.get("signupCampaignsPage.giftReason")).clear();
		WebElement giftReasonForSendKeys = driver.findElement(locators.get("signupCampaignsPage.giftReason"));
		giftReasonForSendKeys.sendKeys(giftreason);
		logger.info("Entered gift reason");

		/*
		 * driver.findElement(locators.get("signupCampaignsPage.redemableDrp")).click();
		 * 
		 * List<WebElement> elements =
		 * driver.findElements(locators.get("signupCampaignsPage.redemableList"));
		 * utils.selectListDrpDwnValue(elements, redemable);
		 */
		WebElement redeemableDropDownList_select = driver.findElement(locators.get("signupCampaignsPage.redeemableDropDownList_select"));
		utils.selectDrpDwnValue(redeemableDropDownList_select, redemable);
		WebElement nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		nextBtn.click();
		logger.info("Clicked next button");
		utils.waitTillPagePaceDone();
		TestListeners.extentTest.get()
				.pass("New signup campaign what details gift type, giftreason, redemable entered successfully:" + name);

	}

	public void createWhomDetailsSignupCampaign(String pushNotification, String emailSubject, String emailTemplate) {

		WebElement perDeviceChkBox = driver.findElement(locators.get("signupCampaignsPage.perDeviceChkBox"));
		perDeviceChkBox.click();
		driver.findElement(locators.get("signupCampaignsPage.pushNotificationTxtBox")).clear();
		WebElement pushNotificationTxtBox = driver.findElement(locators.get("signupCampaignsPage.pushNotificationTxtBox"));
		pushNotificationTxtBox.sendKeys(pushNotification);
		driver.findElement(locators.get("signupCampaignsPage.emailSubjectTxtBox")).clear();
		WebElement emailSubjectTxtBox = driver.findElement(locators.get("signupCampaignsPage.emailSubjectTxtBox"));
		emailSubjectTxtBox.sendKeys(emailSubject);
		driver.findElement(locators.get("signupCampaignsPage.emailTemplateTxtBox")).clear();
		WebElement emailTemplateTxtBox = driver.findElement(locators.get("signupCampaignsPage.emailTemplateTxtBox"));
		emailTemplateTxtBox.sendKeys(emailTemplate);
		WebElement nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		nextBtn.click();
		utils.waitTillPagePaceDone();
		logger.info("Clicked next button");
		TestListeners.extentTest.get().pass("New signup campaign whom details entered successfully");
	}

	public void createWhenDetailsCampaign1() {
		try {
			Date date = new Date();
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat();
			simpleDateFormat.applyPattern("dd");
			System.out.println("Day  : " + simpleDateFormat.format(date));
			String dateStr = utils.removeLeadingZero(simpleDateFormat.format(date));
			WebElement referralStartDateInput = driver.findElement(locators.get("signupCampaignsPage.referralStartDateInput"));
			referralStartDateInput.isDisplayed();
			referralStartDateInput.click();
			By dayLabelBy = By.xpath(utils.getLocatorValue("signupCampaignsPage.dayLabel").replace("temp", dateStr));
			WebElement dayLabel = driver.findElement(dayLabelBy);
			dayLabel.isDisplayed();
			dayLabel.click();
//			utils.getLocator(“signupCampaignsPage.dayLabel”).isDisplayed();
//			utils.getLocator(“signupCampaignsPage.dayLabel”).click();
			WebElement referralEndDateInput = driver.findElement(locators.get("signupCampaignsPage.referralEndDateInput"));
			referralEndDateInput.isDisplayed();
			referralEndDateInput.click();
			WebElement endDateLabel = driver.findElement(locators.get("signupCampaignsPage.endDateLabel"));
			endDateLabel.isDisplayed();
			endDateLabel.click();
//			driver.findElement(By.xpath(utils.getLocatorValue(“signupCampaignsPage.dayLabel”).replace(“temp”,
//					simpleDateFormat.format(date)))).isDisplayed();
//			driver.findElement(By.xpath(utils.getLocatorValue(“signupCampaignsPage.dayLabel”).replace(“temp”,
//					simpleDateFormat.format(date)))).click();

			WebElement activateBtn = driver.findElement(locators.get("signupCampaignsPage.activateBtn"));
			activateBtn.click();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String createWhenDetailsCampaign() {
		// get campaign id and activate campaign
		String url = driver.getCurrentUrl();
		String campaignId = url.replaceAll("[^0-9]", "");
		WebElement activateBtn = driver.findElement(locators.get("signupCampaignsPage.activateBtn"));
		utils.StaleElementclick(driver, activateBtn);
		// utils.waitTillPagePaceDone();
		logger.info("Campaign when details entered and clicked Activate button successfuly");
		TestListeners.extentTest.get().pass("Campaign when details entered and clicked Activate button successfuly");
		logger.info("Campaign Id is : " + campaignId);
		TestListeners.extentTest.get().info("Campaign Id is : " + campaignId);
		return campaignId;
	}

	public String getCampaignid() {
		String url = driver.getCurrentUrl();
		String campaignId = url.replaceAll("[^0-9]", "");
		logger.info("Campaign Id is : " + campaignId);
		TestListeners.extentTest.get().info("Campaign Id is : " + campaignId);
		return campaignId;
	}

	public void setExecutionDelay(String val) {
		driver.findElement(locators.get("signupCampaignsPage.excutionDelay")).clear();
		WebElement excutionDelay = driver.findElement(locators.get("signupCampaignsPage.excutionDelay"));
		excutionDelay.sendKeys(val);
	}

	public void setStartDate() { // do not change this flow
		String fullDate = CreateDateTime.getCurrentDate();
		String date[] = fullDate.split("-");
		String currentDate = date[2];
		String currentYear = date[0];
		int currentMonth = Integer.parseInt(date[1]);
		currentMonth = currentMonth - 1;
		if (currentDate.charAt(0) == '0') {
			currentDate = Character.toString(currentDate.charAt(1));
		}
		// clicked on start date Or Start date and time calendar
		WebElement startDate = driver.findElement(locators.get("signupCampaignsPage.startDate"));
		startDate.click();
		try {
			selUtils.implicitWait(1);
			// Check apply button is coming or not
			WebElement applyBtn = driver.findElement(locators.get("signupCampaignsPage.applyBtn"));
			applyBtn.isDisplayed();
			// select current year and month
			List<WebElement> setStartYear = driver.findElements(locators.get("signupCampaignsPage.setStartYear"));
			utils.selectDrpDwnValue(setStartYear.get(0), currentYear + "");

			List<WebElement> setStartCurrentMonth = driver.findElements(locators.get("signupCampaignsPage.setStartCurrentMonth"));
			utils.selectDrpDwnValueNew(setStartCurrentMonth.get(0), currentMonth + "");
			// get all element of current date
			String xpath = utils.getLocatorValue("signupCampaignsPage.calendarDaysWithAppyButton")
					.replace("${CurrentDate}", currentDate);
			List<WebElement> eleList = driver.findElements(By.xpath(xpath));
			for (WebElement wEle : eleList) {
				wEle.getAttribute("class");
				// if (attributeValue.equalsIgnoreCase("available")) {
				try {
					wEle.click();
					WebElement applyBtnInLoop = driver.findElement(locators.get("signupCampaignsPage.applyBtn"));
					applyBtnInLoop.click();
					logger.info("START Date is selected and clicked on applied button :" + currentDate);
					TestListeners.extentTest.get()
							.info("START Date is selected and clicked on applied button :" + currentDate);
					break;
				} catch (Exception exp) {

				}
			}
		} catch (Exception e) {
			selUtils.implicitWait(1);
			String xpathWithoutAppyBtn = utils.getLocatorValue("signupCampaignsPage.calendarDaysWithoutApplyButton")
					.replace("${CurrentDate}", currentDate);

			// this is for where apply button is not coming i.e old calendar
			List<WebElement> eleList = driver.findElements(By.xpath(xpathWithoutAppyBtn));
			for (WebElement wEle : eleList) {
				try {
					wEle.click();
					logger.info(
							"Start Date is selected and apply button not applicable for this calendar :" + currentDate);
					TestListeners.extentTest.get().info(
							"Start Date is selected and apply button not applicable for this calendar :" + currentDate);
					break;
				} catch (Exception exp) {

				}
			}
		}
		selUtils.implicitWait(50);
	}

	public void setStartDateAndTime() {
		String fullDate = CreateDateTime.getCurrentDate();
		String date[] = fullDate.split("-");
		String currentDate = date[2];
		if (currentDate.charAt(0) == '0') {
			currentDate = Character.toString(currentDate.charAt(1));
		}
		WebElement startDate = driver.findElement(locators.get("signupCampaignsPage.startDate"));
		startDate.click();
		List<WebElement> startDateList = driver.findElements(locators.get("signupCampaignsPage.startDateList"));
		utils.selectListDrpDwnValue(startDateList, currentDate);
		logger.info("Start Date is entered");
		TestListeners.extentTest.get().info("Start Date is entered");
	}

	public void setEndDateforCouponCampaign(int days) throws ParseException {
		WebElement wEleAppyButton = null;
		PageObj pageObj = new PageObj(driver);
		String nextdayDate = CreateDateTime.getFutureDateTimeUTC(days);
		LocalDate date1 = LocalDate.parse(nextdayDate);
		String monthName = date1.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
		// boolean resultFalg = compareDatesWithLastDateOfMonth(nextdayDate);
		String date[] = nextdayDate.split("-");
		String currentDate = date[2];
		String currentYear = date[0];

		nextdayDate = nextdayDate + " 02:30 PM";
		int currentMonth = Integer.parseInt(date[1]) - 1;
		if (currentDate.charAt(0) == '0') {
			currentDate = Character.toString(currentDate.charAt(1));
		}
		WebElement endDate = driver.findElement(locators.get("signupCampaignsPage.endDate"));
		endDate.click();

		try {
			try {
				selUtils.implicitWait(1);
				WebElement applyButton = driver.findElement(locators.get("signupCampaignsPage.applyButton"));
				applyButton.isDisplayed();
				wEleAppyButton = applyButton;
			} catch (Exception e) {
				WebElement dateApplyButton = driver.findElement(locators.get("signupCampaignsPage.dateApplyButton"));
				dateApplyButton.isDisplayed();
				wEleAppyButton = dateApplyButton;
			}
			// selUtils.implicitWait(50);
			try {
				selectCalendarYear(currentYear);
			} catch (Exception e) {
				logger.info("Year is not selected");
			}
			selectCalendarMonth(currentMonth + "");
			String xpath = utils.getLocatorValue("signupCampaignsPage.calendarDaysWithAppyButton")
					.replace("${CurrentDate}", currentDate);
			List<WebElement> eleList = driver.findElements(By.xpath(xpath));
			for (WebElement wEle : eleList) {
				String colorCode = pageObj.mobileconfigurationPage().getTextColour(wEle);
				if (colorCode.equalsIgnoreCase("#33343d")) {
					try {
						selUtils.implicitWait(1);
						wEle.click();
						break;
					} catch (Exception e) {

					}
				}

			}
			wEleAppyButton.click();
			selUtils.implicitWait(50);
			logger.info(currentDate + " END Date is selected and clicked on applied button");
			TestListeners.extentTest.get().info(currentDate + " END Date is selected and clicked on applied button");
		} catch (Exception e) {
			selectCalendarDateWithoutApplyButton(monthName, currentDate);
		}
		selUtils.implicitWait(50);
	}

	public void createWhatDetailsPostcheckinCampaign(String name, String giftType, String giftreason,
			String redemable) {
		// Set Name
		driver.findElement(locators.get("signupCampaignsPage.postCheckinCampaignName")).clear();
		WebElement postCheckinCampaignName = driver.findElement(locators.get("signupCampaignsPage.postCheckinCampaignName"));
		postCheckinCampaignName.sendKeys(name);
		logger.info("Entered campaign name");
		TestListeners.extentTest.get().pass("Entered campaign name : " + name);
		// Set Gift type
		WebElement postCheckingiftTypeDrp = driver.findElement(locators.get("signupCampaignsPage.postCheckingiftTypeDrp"));
		postCheckingiftTypeDrp.click();
		List<WebElement> postCheckingiftTypeList = driver.findElements(locators.get("signupCampaignsPage.postCheckingiftTypeList"));
		utils.longWaitInSeconds(1);
		utils.selectListDrpDwnValue(postCheckingiftTypeList, giftType);
		TestListeners.extentTest.get().pass("Gift type selected successfully: " + giftType);
		// Set gift reason
		driver.findElement(locators.get("signupCampaignsPage.postCheckingiftReason")).clear();
		WebElement postCheckingiftReason = driver.findElement(locators.get("signupCampaignsPage.postCheckingiftReason"));
		postCheckingiftReason.sendKeys(giftreason);
		logger.info("Entered gift reason");
		TestListeners.extentTest.get().pass("Entered gift reason: " + giftreason);

		WebElement nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		utils.scrollToElement(driver, nextBtn);
		// Set Redeemable
		// utils.waitTillElementToBeClickable(driver.findElement(locators.get("signupCampaignsPage.redeemableDropDownList_select"));
		WebElement redeemableDropDownList_select = driver.findElement(locators.get("signupCampaignsPage.redeemableDropDownList_select"));
		utils.selectDrpDwnValue(redeemableDropDownList_select, redemable);
		TestListeners.extentTest.get().pass("Entered redeemable: " + redemable);
		nextBtn.click();
		utils.waitTillPagePaceDone();
		logger.info("Clicked next button");
		TestListeners.extentTest.get().pass(
				"New post checkin campaign what details name, giftType, giftreason and redemable entered successfully: "
						+ name);

	}

	public void createWhatDetailsPostcheckinQCCampaign(String name, String giftType, String giftreason,
			String giftPoints, String pointType) {

		driver.findElement(locators.get("signupCampaignsPage.postCheckinCampaignName")).clear();
		WebElement postCheckinCampaignName = driver.findElement(locators.get("signupCampaignsPage.postCheckinCampaignName"));
		postCheckinCampaignName.sendKeys(name);
		logger.info("Entered Postcheckin campaign name");
		WebElement postCheckingiftTypeDrp = driver.findElement(locators.get("signupCampaignsPage.postCheckingiftTypeDrp"));
		postCheckingiftTypeDrp.click();

		List<WebElement> postCheckingiftTypeList = driver.findElements(locators.get("signupCampaignsPage.postCheckingiftTypeList"));
		utils.selectListDrpDwnValue(postCheckingiftTypeList, giftType);

		driver.findElement(locators.get("signupCampaignsPage.postCheckingiftReason")).clear();
		WebElement postCheckingiftReason = driver.findElement(locators.get("signupCampaignsPage.postCheckingiftReason"));
		postCheckingiftReason.sendKeys(giftreason);
		logger.info("Entered gift reason");

		/*
		 * Thread.sleep(5000);
		 * driver.findElement(locators.get("signupCampaignsPage.postCheckinredemableDrp")).click();
		 * 
		 * List<WebElement> elements =
		 * driver.findElements(locators.get("signupCampaignsPage.postCheckinredemableList");
		 * utils.selectListDrpDwnValue(elements, redemable);
		 */
		WebElement fixedPointsDrp = driver.findElement(locators.get("signupCampaignsPage.fixedPointsDrp"));
		fixedPointsDrp.click();
		List<WebElement> fixedPointsDrpList = driver.findElements(locators.get("signupCampaignsPage.fixedPointsDrpList"));
		utils.selectListDrpDwnValue(fixedPointsDrpList, pointType);

		driver.findElement(locators.get("signupCampaignsPage.giftPointsTextBox")).clear();
		WebElement giftPointsTextBox = driver.findElement(locators.get("signupCampaignsPage.giftPointsTextBox"));
		giftPointsTextBox.sendKeys(giftPoints);

		WebElement qcCheckbox = driver.findElement(locators.get("signupCampaignsPage.qcCheckbox"));
		qcCheckbox.click();

		// driver.findElement(locators.get("signupCampaignsPage.nextBtn")).click();
		WebElement nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		utils.StaleElementclick(driver, nextBtn);
		logger.info("Clicked next button");

		TestListeners.extentTest.get().pass("New post checkin campaign what details entered successfully");

	}

	public void createWhomDetailsPostcheckinCampaign(String pushNotification, String emailSubject,
			String emailTemplate) {
		// driver.findElement(locators.get("signupCampaignsPage.perDeviceChkBox")).click();
		driver.findElement(locators.get("signupCampaignsPage.postCheckinPushNotificationTxtBox")).clear();
		WebElement postCheckinPushNotificationTxtBox = driver.findElement(locators.get("signupCampaignsPage.postCheckinPushNotificationTxtBox"));
		postCheckinPushNotificationTxtBox.sendKeys(pushNotification);
		driver.findElement(locators.get("signupCampaignsPage.postCheckinEmailSubjectTxtBox")).clear();
		WebElement postCheckinEmailSubjectTxtBox = driver.findElement(locators.get("signupCampaignsPage.postCheckinEmailSubjectTxtBox"));
		postCheckinEmailSubjectTxtBox.sendKeys(emailSubject);
		driver.findElement(locators.get("signupCampaignsPage.postCheckinemailTemplateTxtBox")).clear();
		WebElement postCheckinemailTemplateTxtBox = driver.findElement(locators.get("signupCampaignsPage.postCheckinemailTemplateTxtBox"));
		postCheckinemailTemplateTxtBox.sendKeys(emailTemplate);
		WebElement nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		nextBtn.click();
		utils.waitTillPagePaceDone();
		logger.info("Clicked next button");
		TestListeners.extentTest.get()
				.pass("New post checkin campaign whom details email and push notifications entered successfully");
	}

	public void createWhomDetailsPostcheckinQCCampaign(String pushNotification, String emailSubject,
			String emailTemplate, String qcItem) {
		utils.longWaitInSeconds(4);
		WebElement menuItemQualificationDrp = driver.findElement(locators.get("signupCampaignsPage.menuItemQualificationDrp"));
		menuItemQualificationDrp.click();
		List<WebElement> menuItemQualificationDrpList = driver.findElements(locators.get("signupCampaignsPage.menuItemQualificationDrpList"));
		utils.selectListDrpDwnValue(menuItemQualificationDrpList, qcItem);
		driver.findElement(locators.get("signupCampaignsPage.postCheckinPushNotificationTxtBox")).clear();
		WebElement postCheckinPushNotificationTxtBox = driver.findElement(locators.get("signupCampaignsPage.postCheckinPushNotificationTxtBox"));
		postCheckinPushNotificationTxtBox.sendKeys(pushNotification);
		driver.findElement(locators.get("signupCampaignsPage.postCheckinEmailSubjectTxtBox")).clear();
		WebElement postCheckinEmailSubjectTxtBox = driver.findElement(locators.get("signupCampaignsPage.postCheckinEmailSubjectTxtBox"));
		postCheckinEmailSubjectTxtBox.sendKeys(emailSubject);
		driver.findElement(locators.get("signupCampaignsPage.postCheckinemailTemplateTxtBox")).clear();
		WebElement postCheckinemailTemplateTxtBox = driver.findElement(locators.get("signupCampaignsPage.postCheckinemailTemplateTxtBox"));
		postCheckinemailTemplateTxtBox.sendKeys(emailTemplate);
		WebElement nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		nextBtn.click();
		logger.info("Clicked next button");
		TestListeners.extentTest.get().pass("New post checkin campaign whom details entered successfully");
	}

	public void createWhatDetailsMassCampaign(String name, String giftType, String redemable)
			throws InterruptedException {

		driver.findElement(locators.get("signupCampaignsPage.campaignName")).clear();
		WebElement campaignName = driver.findElement(locators.get("signupCampaignsPage.campaignName"));
		campaignName.sendKeys(name);
		logger.info("Entered mass campaign name as " + name);
		/*
		 * driver.findElement(locators.get("signupCampaignsPage.massGiftTypeDrp")).click();
		 * selUtils.implicitWait(50); List<WebElement> ele =
		 * driver.findElements(locators.get("signupCampaignsPage.massGiftTypeList");
		 * utils.selectListDrpDwnValue(ele, giftType);
		 */
		WebElement massGiftTypeDrp = driver.findElement(locators.get("signupCampaignsPage.massGiftTypeDrp"));
		utils.selectDrpDwnValue(massGiftTypeDrp, giftType);
		WebElement redeemableDropDownList_select = driver.findElement(locators.get("signupCampaignsPage.redeemableDropDownList_select"));
		utils.selectDrpDwnValue(redeemableDropDownList_select, redemable);
		/*
		 * driver.findElement(locators.get("signupCampaignsPage.massredemableDrp")).click();
		 * List<WebElement> elements =
		 * driver.findElements(locators.get("signupCampaignsPage.massredemableList");
		 * utils.selectListDrpDwnValue(elements, redemable);
		 */
		// driver.findElement(locators.get("signupCampaignsPage.nextBtn")).click();
		WebElement nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		utils.StaleElementclick(driver, nextBtn);
		utils.waitTillPagePaceDone();
		logger.info("Clicked next button");
		TestListeners.extentTest.get().pass("New mass campaign what details entered successfully : " + name);

	}

	public void createWhatDetailsCoupanCampaign(String name) {
		driver.findElement(locators.get("signupCampaignsPage.couponCampaignname")).clear();
		WebElement couponCampaignname = driver.findElement(locators.get("signupCampaignsPage.couponCampaignname"));
		couponCampaignname.sendKeys(name);
		logger.info("Entered coupon campaign name: " + name);
		WebElement couponDescriptionInput = driver.findElement(locators.get("signupCampaignsPage.couponDescriptionInput"));
		couponDescriptionInput.click();
		couponDescriptionInput.sendKeys("Automated Coupon");
		WebElement nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		nextBtn.click();
		logger.info("Clicked next button");
		logger.info("Campaign name entered successfully " + name);
		TestListeners.extentTest.get().pass("Campaign name entered successfully " + name);
	}

//	public void createWhatDetailsPromoCampaign(String name, String promoCode) {
//		try {
//			driver.findElement(locators.get("signupCampaignsPage.couponCampaignname")).clear();
//			driver.findElement(locators.get("signupCampaignsPage.couponCampaignname")).sendKeys(name);
//			logger.info("Entered coupon campaign name: " + name);
//			driver.findElement(locators.get("signupCampaignsPage.promoCodeTxtBox")).clear();
//			driver.findElement(locators.get("signupCampaignsPage.promoCodeTxtBox")).sendKeys(promoCode);
//			logger.info("Entered promo code as: " + promoCode);
//			driver.findElement(locators.get("signupCampaignsPage.nextBtn")).click();
//			logger.info("Clicked next button");
//			TestListeners.extentTest.get().pass("Campaign name entered successfully");
//		} catch (Exception e) {
//			logger.error("Error in navigating to whom details for campaign" + e);
//			TestListeners.extentTest.get().fail("Error in navigating to whom details for campaign" + e);
//		}
//	}

	public void createWhomDetailsMassCampaign(String audiance, String segement, String pushNotification,
			String emailSubject, String emailTemplate) {

		// utils.longWaitInSeconds(2);
		WebElement audianceTypeDrp = driver.findElement(locators.get("signupCampaignsPage.audianceTypeDrp"));
		audianceTypeDrp.click();
		List<WebElement> audianceTypeDrpList = driver.findElements(locators.get("signupCampaignsPage.audianceTypeDrpList"));
		utils.selectListDrpDwnValue(audianceTypeDrpList, audiance);
		// change xpath and use select for selecting the segment
		WebElement segmentDropDownList_select = driver.findElement(locators.get("signupCampaignsPage.segmentDropDownList_select"));
		utils.selectDrpDwnValue(segmentDropDownList_select, segement);

		driver.findElement(locators.get("signupCampaignsPage.massPushNotificationTxtBox")).clear();
		WebElement massPushNotificationTxtBox = driver.findElement(locators.get("signupCampaignsPage.massPushNotificationTxtBox"));
		massPushNotificationTxtBox.sendKeys(pushNotification);
		driver.findElement(locators.get("signupCampaignsPage.massEmailSubjectTxtBox")).clear();
		WebElement massEmailSubjectTxtBox = driver.findElement(locators.get("signupCampaignsPage.massEmailSubjectTxtBox"));
		massEmailSubjectTxtBox.sendKeys(emailSubject);
		driver.findElement(locators.get("signupCampaignsPage.massTemplateTxtBox")).clear();
		WebElement massTemplateTxtBox = driver.findElement(locators.get("signupCampaignsPage.massTemplateTxtBox"));
		massTemplateTxtBox.sendKeys(emailTemplate);
//		driver.findElement(locators.get("signupCampaignsPage.nextBtn")).click();
		WebElement nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		utils.scrollToElement(driver, nextBtn);
		selUtils.jsClick(nextBtn);
		utils.waitTillPagePaceDone();
		logger.info("Clicked next button");
		TestListeners.extentTest.get().pass("New mass campaign whom details entered successfully");
	}

	public void createWhomDetailsCouponCampaign(String noOfGuests, String usagePerGuest, String giftType,
			String amount) {

		WebElement usageType = driver.findElement(locators.get("signupCampaignsPage.usageType"));
		usageType.click();
		WebElement codeGeneration = driver.findElement(locators.get("signupCampaignsPage.codeGeneration"));
		codeGeneration.click();

		driver.findElement(locators.get("signupCampaignsPage.numberOfGuests")).clear();
		WebElement numberOfGuests = driver.findElement(locators.get("signupCampaignsPage.numberOfGuests"));
		numberOfGuests.sendKeys(noOfGuests);

		driver.findElement(locators.get("signupCampaignsPage.usagePerGuest")).clear();
		WebElement usagePerGuestElement = driver.findElement(locators.get("signupCampaignsPage.usagePerGuest"));
		usagePerGuestElement.sendKeys(usagePerGuest);

		WebElement couponGiftTypeDrp = driver.findElement(locators.get("signupCampaignsPage.couponGiftTypeDrp"));
		utils.scrollToElement(driver, couponGiftTypeDrp);
		couponGiftTypeDrp.click();
		List<WebElement> couponGiftTypeDrpList = driver.findElements(locators.get("signupCampaignsPage.couponGiftTypeDrpList"));
		utils.selectListDrpDwnValue(couponGiftTypeDrpList, giftType);

		driver.findElement(locators.get("signupCampaignsPage.amountDiscount")).clear();
		WebElement amountDiscount = driver.findElement(locators.get("signupCampaignsPage.amountDiscount"));
		amountDiscount.sendKeys(amount);

		WebElement nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		nextBtn.click();
		logger.info("Clicked next button");
		TestListeners.extentTest.get().pass("New coupon campaign whom details entered successfully");
	}

	public void createWhenDetailsMassCampaign(String frequency, String dateTime) {

		WebElement massFrequencyDrp = driver.findElement(locators.get("signupCampaignsPage.massFrequencyDrp"));
		massFrequencyDrp.click();
		List<WebElement> massFrequencyDrpList = driver.findElements(locators.get("signupCampaignsPage.massFrequencyDrpList"));
		utils.selectListDrpDwnValue(massFrequencyDrpList, frequency);
		driver.findElement(locators.get("signupCampaignsPage.startTimeTxtBox")).clear();
		WebElement startTimeTxtBox = driver.findElement(locators.get("signupCampaignsPage.startTimeTxtBox"));
		startTimeTxtBox.sendKeys(dateTime);
		TestListeners.extentTest.get().pass("Start time is entered : " + dateTime);
		WebElement cancelbtn = driver.findElement(locators.get("signupCampaignsPage.cancelbtn"));
		cancelbtn.click();
		WebElement scheduleTimezone = driver.findElement(By.id("schedule_timezone"));
		Select sel = new Select(scheduleTimezone);
		sel.selectByVisibleText(Utilities.getConfigProperty("timezone"));
		logger.info("Timezone selected ");
		TestListeners.extentTest.get().info("Timezone selected: " + Utilities.getConfigProperty("timezone"));
		WebElement scheduleBtn = driver.findElement(locators.get("signupCampaignsPage.scheduleBtn"));
		scheduleBtn.click();
		logger.info("Clicked schedule button");
		utils.acceptAlert(driver);
		utils.waitTillPagePaceDone();
		TestListeners.extentTest.get().pass("New mass campaign when details entered successfully");
	}

	public void setFrequencyAndTimeInCampaign(String frequency, String dateTime) {
		WebElement massFrequencyDrp = driver.findElement(locators.get("signupCampaignsPage.massFrequencyDrp"));
		massFrequencyDrp.click();
		List<WebElement> massFrequencyDrpList = driver.findElements(locators.get("signupCampaignsPage.massFrequencyDrpList"));
		utils.selectListDrpDwnValue(massFrequencyDrpList, frequency);
		driver.findElement(locators.get("signupCampaignsPage.startTimeTxtBox")).clear();
		WebElement startTimeTxtBox = driver.findElement(locators.get("signupCampaignsPage.startTimeTxtBox"));
		startTimeTxtBox.sendKeys(dateTime);
		WebElement cancelbtn = driver.findElement(locators.get("signupCampaignsPage.cancelbtn"));
		cancelbtn.click();
		WebElement scheduleTimezone = driver.findElement(By.id("schedule_timezone"));
		Select sel = new Select(scheduleTimezone);
		sel.selectByVisibleText(Utilities.getConfigProperty("timezone"));
		logger.info("Timezone selected ");
		TestListeners.extentTest.get().info("Timezone selected: " + Utilities.getConfigProperty("timezone"));
	}

	public void scheduleCampaign() {
		WebElement scheduleBtn = driver.findElement(locators.get("signupCampaignsPage.scheduleBtn"));
		scheduleBtn.click();
		logger.info("Clicked schedule button");
		utils.acceptAlert(driver);
		TestListeners.extentTest.get().pass("New mass campaign when details entered successfully");
	}

	public boolean setStoOn() {
		WebElement classicSto = driver.findElement(locators.get("signupCampaignsPage.classicSto"));
		boolean status = utils.checkElementPresent(classicSto);
		if (status) {
			classicSto.click();
		}
		return status;
	}

	public void setFrequency(String frequency) {

		WebElement massFrequencyDrp = driver.findElement(locators.get("signupCampaignsPage.massFrequencyDrp"));
		massFrequencyDrp.click();
		List<WebElement> massFrequencyDrpList = driver.findElements(locators.get("signupCampaignsPage.massFrequencyDrpList"));
		utils.selectListDrpDwnValue(massFrequencyDrpList, frequency);
		TestListeners.extentTest.get().pass("mass campaign frequency is set as " + frequency);

	}

	public void setRepeatOn(String val) {

		driver.findElement(locators.get("signupCampaignsPage.repeatOn")).clear();
		WebElement repeatOn = driver.findElement(locators.get("signupCampaignsPage.repeatOn"));
		repeatOn.sendKeys(val);
	}

	public void setStartEndDateTime(String startdateTime, String enddateTime) {
		driver.findElement(locators.get("signupCampaignsPage.startTimeTxtBox")).clear();
		WebElement startTimeTxtBox = driver.findElement(locators.get("signupCampaignsPage.startTimeTxtBox"));
		startTimeTxtBox.sendKeys(startdateTime);
		driver.findElement(locators.get("signupCampaignsPage.endTimeTxtBox")).clear();
		WebElement endTimeTxtBox = driver.findElement(locators.get("signupCampaignsPage.endTimeTxtBox"));
		endTimeTxtBox.sendKeys(enddateTime);
		WebElement scheduleTimezone = driver.findElement(By.id("schedule_timezone"));
		Select sel = new Select(scheduleTimezone);
		sel.selectByVisibleText(Utilities.getConfigProperty("timezone"));
		logger.info("Timezone selected ");
		TestListeners.extentTest.get().info("Timezone selected: " + Utilities.getConfigProperty("timezone"));
		// driver.findElement(locators.get("signupCampaignsPage.scheduleBtn")).click();
		WebElement scheduleBtn = driver.findElement(locators.get("signupCampaignsPage.scheduleBtn"));
		utils.clickByJSExecutor(driver, scheduleBtn);
		logger.info("Clicked schedule button");
		utils.acceptAlert(driver);
		TestListeners.extentTest.get().pass("New mass campaign when details entered and scheduled successfully");
		utils.waitTillPagePaceDone();

	}

	public void setRepeatAndDaysOfWeek(String run, String day) {
		driver.findElement(locators.get("signupCampaignsPage.repeatEvery")).clear();
		WebElement repeatEvery = driver.findElement(locators.get("signupCampaignsPage.repeatEvery"));
		repeatEvery.sendKeys(run);
		WebElement dayofWeekDrp = driver.findElement(locators.get("signupCampaignsPage.dayofWeekDrp"));
		dayofWeekDrp.click();
		List<WebElement> dayofWeekDrpList = driver.findElements(locators.get("signupCampaignsPage.dayofWeekDrpList"));
		utils.selectListDrpDwnValue(dayofWeekDrpList, day);
	}

	public void createWhatDetailsReferralCampaign(String name, String giftType, String giftreason, String giftAmount)
			throws InterruptedException {
		selUtils.implicitWait(50);

		WebElement referralCampaignNameTextbox = driver.findElement(locators.get("signupCampaignsPage.referralCampaignNameTextbox"));
		referralCampaignNameTextbox.isDisplayed();
		driver.findElement(locators.get("signupCampaignsPage.referralCampaignNameTextbox")).clear();
		WebElement referralCampaignNameTextboxForSendKeys = driver.findElement(locators.get("signupCampaignsPage.referralCampaignNameTextbox"));
		referralCampaignNameTextboxForSendKeys.sendKeys(name);
		logger.info("Campaign name is set as: " + name);
		TestListeners.extentTest.get().info("Campaign name is set as: " + name);
		WebElement giftTypeDropDownButton = driver.findElement(locators.get("signupCampaignsPage.giftTypeDropDownButton"));
		giftTypeDropDownButton.click();
		List<WebElement> referralGiftTypeList = driver.findElements(locators.get("signupCampaignsPage.referralGiftTypeList"));
		utils.selectListDrpDwnValue(referralGiftTypeList, giftType);
		WebElement referralGiftReasonTextbox = driver.findElement(locators.get("signupCampaignsPage.referralGiftReasonTextbox"));
		referralGiftReasonTextbox.isDisplayed();
		driver.findElement(locators.get("signupCampaignsPage.referralGiftReasonTextbox")).clear();
		WebElement referralGiftReasonTextboxForSendKeys = driver.findElement(locators.get("signupCampaignsPage.referralGiftReasonTextbox"));
		referralGiftReasonTextboxForSendKeys.sendKeys(giftreason);
		logger.info("Entered gift reason");
		driver.findElement(locators.get("signupCampaignsPage.giftPointTextbox")).clear();
		WebElement giftPointTextbox = driver.findElement(locators.get("signupCampaignsPage.giftPointTextbox"));
		giftPointTextbox.sendKeys(giftAmount);
//			List<WebElement> elements = driver.findElements(locators.get("signupCampaignsPage.redemableList"));
//			utils.selectListDrpDwnValue(elements, redemable);
		WebElement nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		nextBtn.click();
		logger.info("Clicked next button");
		TestListeners.extentTest.get().pass("New signup campaign what details entered successfully");
	}

	public void createWhomDetailsReferralCampaign(String pushNotification, String emailSubject, String emailTemplate) {

		// driver.findElement(locators.get("signupCampaignsPage.perDeviceChkBox")).click();
		driver.findElement(locators.get("signupCampaignsPage.pushNotificationTitleTextbox")).clear();
		WebElement pushNotificationTitleTextbox = driver.findElement(locators.get("signupCampaignsPage.pushNotificationTitleTextbox"));
		pushNotificationTitleTextbox.sendKeys(pushNotification);
//			driver.findElement(locators.get("signupCampaignsPage.emailSubjectTxtBox")).clear();
//			driver.findElement(locators.get("signupCampaignsPage.emailSubjectTxtBox")).sendKeys(emailSubject);
//			driver.findElement(locators.get("signupCampaignsPage.emailTemplateTxtBox")).clear();
//			driver.findElement(locators.get("signupCampaignsPage.emailTemplateTxtBox")).sendKeys(emailTemplate);
		WebElement nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		nextBtn.click();
		logger.info("Clicked next button");
		TestListeners.extentTest.get().pass("New signup campaign whom details entered successfully");
	}

	public void setCampaignNameandGiftType(String name, String giftType, String giftreason) {

		driver.findElement(locators.get("signupCampaignsPage.signupCampaignName")).clear();
		WebElement signupCampaignName = driver.findElement(locators.get("signupCampaignsPage.signupCampaignName"));
		signupCampaignName.sendKeys(name);
		logger.info("Entered campaign name");

		WebElement giftTypeDrp = driver.findElement(locators.get("signupCampaignsPage.giftTypeDrp"));
		giftTypeDrp.click();
		List<WebElement> giftTypeList = driver.findElements(locators.get("signupCampaignsPage.giftTypeList"));
		utils.selectListDrpDwnValue(giftTypeList, giftType);

		driver.findElement(locators.get("signupCampaignsPage.giftReason")).clear();
		WebElement giftReason = driver.findElement(locators.get("signupCampaignsPage.giftReason"));
		giftReason.sendKeys(giftreason);
		logger.info("Entered gift reason");
		TestListeners.extentTest.get().info("Entered gift reason");

	}

	public void setCampaignName(String name) {
		driver.findElement(locators.get("signupCampaignsPage.campaignName")).clear();
		WebElement campaignName = driver.findElement(locators.get("signupCampaignsPage.campaignName"));
		campaignName.sendKeys(name);
		logger.info("Entered campaign name as :" + name);
		TestListeners.extentTest.get().info("Entered campaign name as :" + name);
	}

	public void setTargetDaysForCheckin(String days) {
		driver.findElement(locators.get("signupCampaignsPage.targetDaysForCheckin")).clear();
		WebElement targetDaysForCheckin = driver.findElement(locators.get("signupCampaignsPage.targetDaysForCheckin"));
		targetDaysForCheckin.sendKeys(days);
		logger.info("Entered target days");
		TestListeners.extentTest.get().info("Entered target days");
	}

	public void setMinMaxRange(String minRange, String MaxRange) {
		driver.findElement(locators.get("signupCampaignsPage.minRaange")).clear();
		WebElement minRaange = driver.findElement(locators.get("signupCampaignsPage.minRaange"));
		minRaange.sendKeys(minRange);

		driver.findElement(locators.get("signupCampaignsPage.maxRaange")).clear();
		WebElement maxRaange = driver.findElement(locators.get("signupCampaignsPage.maxRaange"));
		maxRaange.sendKeys(MaxRange);
		logger.info("Entered min max range");
		TestListeners.extentTest.get().info("Entered min max range");
	}

	public void setSchedulrFor(String val) {
		WebElement scheduleForDrp = driver.findElement(locators.get("signupCampaignsPage.scheduleForDrp"));
		utils.selectDrpDwnValue(scheduleForDrp, val);
		logger.info("Entred scheduled for value");
		TestListeners.extentTest.get().info("Entred scheduled for value");
	}

	public String getExistingCampaignName(String val) {

		WebElement campaignName = driver.findElement(locators.get("signupCampaignsPage.campaignName"));
		String camname = campaignName.getAttribute("value");
		String[] arr = camname.split("_");
		String name = arr[0] + "_" + val;
		logger.info("fetched existing cam name with adding timestamp is :" + name);
		TestListeners.extentTest.get().info("fetched existing cam name with adding timestamp is :" + name);
		return name;

	}

	public void setGiftType(String giftType) {
		WebElement massGiftTypeDrp = driver.findElement(locators.get("signupCampaignsPage.massGiftTypeDrp"));
		utils.selectDrpDwnValue(massGiftTypeDrp, giftType);
//		driver.findElement(locators.get("signupCampaignsPage.massGiftTypeDrp")).click();
//		List<WebElement> ele = driver.findElements(locators.get("signupCampaignsPage.massGiftTypeList");
//		utils.selectListDrpDwnValue(ele, giftType);
		logger.info("Entered gift Type as :" + giftType);
		TestListeners.extentTest.get().info("Entered gift Type as :" + giftType);
	}

	public void setGiftReason(String giftreason) {

		driver.findElement(locators.get("signupCampaignsPage.giftReason")).clear();
		WebElement giftReason = driver.findElement(locators.get("signupCampaignsPage.giftReason"));
		giftReason.sendKeys(giftreason);

		logger.info("Entered gift reason" + giftreason);
		TestListeners.extentTest.get().info("Entered gift reason" + giftreason);

	}

	public void setRedeemable(String redemable) {
		WebElement redeemableDropDownList_select = driver.findElement(locators.get("signupCampaignsPage.redeemableDropDownList_select"));
		utils.selectDrpDwnValue(redeemableDropDownList_select, redemable);
	}

	public void setFixedPoints(String points) {
		driver.findElement(locators.get("signupCampaignsPage.fixedPoints")).clear();
		WebElement fixedPoints = driver.findElement(locators.get("signupCampaignsPage.fixedPoints"));
		fixedPoints.sendKeys(points);
	}

	public void setGiftPoints(String points) {
		driver.findElement(locators.get("signupCampaignsPage.giftPointsTextBox")).clear();
		WebElement giftPointsTextBox = driver.findElement(locators.get("signupCampaignsPage.giftPointsTextBox"));
		giftPointsTextBox.sendKeys(points);
	}

	public void setGiftCurrency(String currency) {
		WebElement giftCurrency = driver.findElement(locators.get("signupCampaignsPage.giftCurrency"));
		utils.waitTillElementToBeClickable(giftCurrency);
		driver.findElement(locators.get("signupCampaignsPage.giftCurrency")).clear();
		WebElement giftCurrencyForSendKeys = driver.findElement(locators.get("signupCampaignsPage.giftCurrency"));
		giftCurrencyForSendKeys.sendKeys(currency);
		logger.info("Entered currency");

	}

	public void setDerivedReward(String reward) {
//		utils.waitTillElementToBeClickable(driver.findElement(locators.get("signupCampaignsPage.derivedRewardDrp"));
////		driver.findElement(locators.get("signupCampaignsPage.derivedRewardDrp")).click();
//		utils.clickByJSExecutor(driver, driver.findElement(locators.get("signupCampaignsPage.derivedRewardDrp"));
//		List<WebElement> elements = driver.findElements(locators.get("signupCampaignsPage.derivedRewardDrpList");
//		utils.selectListDrpDwnValue(elements, reward);
//		logger.info("Entred dynamic reward " + reward);
//		TestListeners.extentTest.get().info("Entred dynamic reward " + reward);

		WebElement ele = driver.findElement(locators.get("signupCampaignsPage.derivedRewardDrpList"));
		utils.selectDrpDwnValue(ele, reward);
		utils.logit("Entered dynamic reward " + reward);
	}

	public void setCouponCampaign(String name) {

		WebElement couponCampaignDrp = driver.findElement(locators.get("signupCampaignsPage.couponCampaignDrp"));
		couponCampaignDrp.click();
		List<WebElement> couponCampaigDrpList = driver.findElements(locators.get("signupCampaignsPage.couponCampaigDrpList"));
		utils.selectListDrpDwnValue(couponCampaigDrpList, name);

		logger.info("Entered coupon campaign name");
		TestListeners.extentTest.get().info("Entered coupon campaign name");
	}

	public void clickNextBtn() {

		WebElement nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		utils.waitTillElementToBeClickable(nextBtn);
		utils.scrollToElement(driver, nextBtn);
		utils.StaleElementclick(driver, nextBtn);
		// utils.waitTillPagePaceDone();
		utils.longWaitInMiliSeconds(500);
		logger.info("Clicked next button");

	}

	public void setCouponCampaignGuestUsagewithGiftAmount(String noOfGuests, String usagePerGuest, String giftType,
			String amount, String locationName, String qcName, boolean globalRedemptionThrottlingToggle) {
		// try {
		utils.waitTillPagePaceDone();
		driver.findElement(locators.get("signupCampaignsPage.numberOfGuests")).clear();
		WebElement numberOfGuests = driver.findElement(locators.get("signupCampaignsPage.numberOfGuests"));
		numberOfGuests.sendKeys(noOfGuests);

		driver.findElement(locators.get("signupCampaignsPage.usagePerGuest")).clear();
		WebElement usagePerGuestElement = driver.findElement(locators.get("signupCampaignsPage.usagePerGuest"));
		usagePerGuestElement.sendKeys(usagePerGuest);

		WebElement couponGiftTypeDrp = driver.findElement(locators.get("signupCampaignsPage.couponGiftTypeDrp"));
		couponGiftTypeDrp.click();
		List<WebElement> couponGiftTypeDrpList = driver.findElements(locators.get("signupCampaignsPage.couponGiftTypeDrpList"));
		utils.selectListDrpDwnValue(couponGiftTypeDrpList, giftType);

		if (giftType.equalsIgnoreCase("$ OFF")) {
			WebElement amountDiscount = driver.findElement(locators.get("signupCampaignsPage.amountDiscount"));
			utils.scrollToElement(driver, amountDiscount);
			driver.findElement(locators.get("signupCampaignsPage.amountDiscount")).clear();
			WebElement amountDiscountForSendKeys = driver.findElement(locators.get("signupCampaignsPage.amountDiscount"));
			amountDiscountForSendKeys.sendKeys(amount);
		} else if (giftType.equalsIgnoreCase("% OFF")) {

			driver.findElement(locators.get("signupCampaignsPage.percentageDiscountInput")).clear();
			WebElement percentageDiscountInput = driver.findElement(locators.get("signupCampaignsPage.percentageDiscountInput"));
			percentageDiscountInput.sendKeys(amount);
		}

		if (locationName != "") {

			// locationsSelectBox
			WebElement locationsSelectBox = driver.findElement(locators.get("signupCampaignsPage.locationsSelectBox"));
			utils.selectDrpDwnValue(locationsSelectBox, locationName);
//			driver.findElement(locators.get("signupCampaignsPage.addLocationDropBox")).click();
//
//			String addLocationUpdatedXpath = utils.getLocatorValue("signupCampaignsPage.selectLocationInDropBox")
//					.replace("$LocationName", locationName);
//			driver.findElement(By.xpath(addLocationUpdatedXpath)).click();
		}
		if (globalRedemptionThrottlingToggle) {
			By noThrottlingBy = By.xpath("//span[@title='No Throttling']");
			WebElement noThrottling = driver.findElement(noThrottlingBy);
			noThrottling.click();
			By perDayBy = By.xpath(
					"//ul[@id='select2-coupon_campaign_usage_throttling_unit-results']/li[contains(text(),'per day')]");
			WebElement perDay = driver.findElement(perDayBy);
			perDay.click();
			By throttlingInputBy = By.xpath("//input[@id='coupon_campaign_usage_throttling']");
			WebElement throttlingInput = driver.findElement(throttlingInputBy);
			throttlingInput.sendKeys("1");

		}
		if (qcName != "") {
			By qualificationCriterionBy = By.xpath("//select[@id='coupon_campaign_qualification_criterion_id']");
			WebElement qualificationCriterionSelect = driver.findElement(qualificationCriterionBy);
			utils.selectDrpDwnValue(qualificationCriterionSelect, qcName);
		}

		WebElement newButton = driver.findElement(locators.get("signupCampaignsPage.newButton"));
		newButton.click();
		utils.waitTillPagePaceDone();
		logger.info("Clicked next button");
		TestListeners.extentTest.get().pass("New coupon campaign whom details entered successfully");
	}

	public void setPromoCampaignWhomDetails(String noOfGuests, String giftType, String amount) {

		driver.findElement(locators.get("signupCampaignsPage.numberOfGuests")).clear();
		WebElement numberOfGuests = driver.findElement(locators.get("signupCampaignsPage.numberOfGuests"));
		numberOfGuests.sendKeys(noOfGuests);

		WebElement couponGiftTypeDrp = driver.findElement(locators.get("signupCampaignsPage.couponGiftTypeDrp"));
		couponGiftTypeDrp.click();
		List<WebElement> couponGiftTypeDrpList = driver.findElements(locators.get("signupCampaignsPage.couponGiftTypeDrpList"));
		utils.selectListDrpDwnValue(couponGiftTypeDrpList, giftType);

		driver.findElement(locators.get("signupCampaignsPage.amountDiscount")).clear();
		WebElement amountDiscount = driver.findElement(locators.get("signupCampaignsPage.amountDiscount"));
		amountDiscount.sendKeys(amount);

		WebElement nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		nextBtn.click();
		utils.waitTillPagePaceDone();
		logger.info("Clicked next button");
		TestListeners.extentTest.get().pass("Promo campaign whom details entered successfully");

	}

	public void setCouponCampaignUsageType(String usageType) {
		// utils.waitTillPagePaceDone();
		if (usageType.equalsIgnoreCase("POS")) {
			WebElement processedAtPOS = driver.findElement(locators.get("signupCampaignsPage.processedAtPOS"));
			utils.waitTillElementToBeClickable(processedAtPOS);
			processedAtPOS.click();
		} else {
			WebElement unlockRewardsMobileApp = driver.findElement(locators.get("signupCampaignsPage.unlockRewardsMobileApp"));
			utils.waitTillElementToBeClickable(unlockRewardsMobileApp);
			unlockRewardsMobileApp.click();
		}
		logger.info(usageType + " usage type is selected ");
		TestListeners.extentTest.get().info(usageType + " usage type is selected ");

	}

	public void setCouponCampaignCodeGenerationType(String codeType) {
		if (codeType.equalsIgnoreCase("PreGenerated")) {
			WebElement preGenerated = driver.findElement(locators.get("signupCampaignsPage.preGenerated"));
			preGenerated.click();
		} else {
			WebElement dynamicGeneration = driver.findElement(locators.get("signupCampaignsPage.dynamicGeneration"));
			dynamicGeneration.click();
		}
		logger.info(codeType + " code Type is selected ");
		TestListeners.extentTest.get().info(codeType + " code type is selected ");
	}

	public void setAudienceType(String audiance) {
		WebElement audianceTypeDrp = driver.findElement(locators.get("signupCampaignsPage.audianceTypeDrp"));
		audianceTypeDrp.click();
		// utils.StaleElementclick(driver,driver.findElement(locators.get("signupCampaignsPage.audianceTypeDrp"));
		List<WebElement> audianceTypeDrpList = driver.findElements(locators.get("signupCampaignsPage.audianceTypeDrpList"));
		utils.selectListDrpDwnValue(audianceTypeDrpList, audiance);
		TestListeners.extentTest.get().pass("Entered audiance type successfully");
	}
	
	public void setSegmentType(String segment) {
	    int attempts = 0;
	    while (attempts < 3) {
	        try {
	            utils.waitTillElementToBeClickable(utils.getLocator("signupCampaignsPage.segmentDropDownList_select"));
	            utils.selectDrpDwnValue(utils.getLocator("signupCampaignsPage.segmentDropDownList_select"), segment);
	            TestListeners.extentTest.get().pass("Entered Segment type successfully : " + segment);
	            break;  // exit loop if success
	        } catch (StaleElementReferenceException e) {
	            attempts++;
	            if (attempts == 3) {
	                throw e;
	            }
	        }
	    }
	}

	public void setLocationGroup(String location) {
		WebElement locationGrpDrp = driver.findElement(locators.get("signupCampaignsPage.locationGrpDrp"));
		locationGrpDrp.click();
		List<WebElement> locationGrpDrpList = driver.findElements(locators.get("signupCampaignsPage.locationGrpDrpList"));
		utils.selectListDrpDwnValue(locationGrpDrpList, location);
		TestListeners.extentTest.get().pass("Entered location type successfully");
	}

	public void setPushNotification(String pushNotification) {
		driver.findElement(locators.get("signupCampaignsPage.massPushNotificationTxtBox")).clear();
		WebElement massPushNotificationTxtBox = driver.findElement(locators.get("signupCampaignsPage.massPushNotificationTxtBox"));
		massPushNotificationTxtBox.sendKeys(pushNotification);
	}

	public void setEmailSubject(String emailSubject) {
		driver.findElement(locators.get("signupCampaignsPage.massEmailSubjectTxtBox")).clear();
		WebElement massEmailSubjectTxtBox = driver.findElement(locators.get("signupCampaignsPage.massEmailSubjectTxtBox"));
		massEmailSubjectTxtBox.sendKeys(emailSubject);
	}

	public void setEmailTemplate(String emailTemplate) {
		driver.findElement(locators.get("signupCampaignsPage.massTemplateTxtBox")).clear();
		WebElement massTemplateTxtBox = driver.findElement(locators.get("signupCampaignsPage.massTemplateTxtBox"));
		massTemplateTxtBox.sendKeys(emailTemplate);
	}

	public void clickNextButton() {
		WebElement nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		nextBtn.isDisplayed();
		utils.clickByJSExecutor(driver, nextBtn);
//		driver.findElement(locators.get("signupCampaignsPage.nextBtn")).click();
		logger.info("Clicked next button");
		TestListeners.extentTest.get().pass("New campaign whom details entered successfully");
		utils.waitTillPagePaceDone();
	}

	public void clickPreviousButton() {
		WebElement previousBtn = driver.findElement(locators.get("signupCampaignsPage.previousBtn"));
		previousBtn.isDisplayed();
		utils.clickByJSExecutor(driver, previousBtn);
//		driver.findElement(locators.get("signupCampaignsPage.previousBtn")).click();
		logger.info("Clicked previous button");
		utils.waitTillPagePaceDone();
	}

	public void setSampleSize() {
		driver.findElement(locators.get("signupCampaignsPage.sampleSize")).clear();
		WebElement sampleSize = driver.findElement(locators.get("signupCampaignsPage.sampleSize"));
		sampleSize.sendKeys("100");
	}

	public void setWinningCombination(String selectionStrategy) {
		WebElement selectionStrategyElement = driver.findElement(locators.get("signupCampaignsPage.selectionStrategy"));
		selectionStrategyElement.click();
		List<WebElement> selectionStrategyList = driver.findElements(locators.get("signupCampaignsPage.selectionStrategyList"));
		utils.selectListDrpDwnValue(selectionStrategyList, selectionStrategy);
		driver.findElement(locators.get("signupCampaignsPage.duration")).clear();
		WebElement duration = driver.findElement(locators.get("signupCampaignsPage.duration"));
		duration.sendKeys("1");

	}

	public void subjectTemplateGroup() {
		driver.findElement(locators.get("signupCampaignsPage.groupA")).clear();
		WebElement groupA = driver.findElement(locators.get("signupCampaignsPage.groupA"));
		groupA.sendKeys("Subject Template for Group A");
		driver.findElement(locators.get("signupCampaignsPage.groupB")).clear();
		WebElement groupB = driver.findElement(locators.get("signupCampaignsPage.groupB"));
		groupB.sendKeys("Subject Template for Group B");

	}

	public void clickScheduleBtn() {
		WebElement scheduleBtn = driver.findElement(locators.get("signupCampaignsPage.scheduleBtn"));
		scheduleBtn.click();
		logger.info("Clicked schedule button");
		utils.acceptAlert(driver);
		utils.waitTillPagePaceDone();
		logger.info("New mass campaign when details entered successfully");
		TestListeners.extentTest.get().pass("New mass campaign when details entered successfully");
	}

	public void setLapseDays(String days) {
		driver.findElement(locators.get("signupCampaignsPage.lapseDays")).clear();
		WebElement lapseDays = driver.findElement(locators.get("signupCampaignsPage.lapseDays"));
		lapseDays.sendKeys(days);
	}

	public void setStartDateforABCampaign() {
		try {
			String fullDate = CreateDateTime.getCurrentDate();
			String date[] = fullDate.split("-");
			String currentDate = date[2];
			int i = Integer.parseInt(currentDate);
			boolean isLastDate = utils.checkLsatDateOfMonth(i);
			int newDate;
			if (isLastDate) {
				newDate = 1;
			} else {
				newDate = i + 1;
			}
			String nextdayDate = Integer.toString(newDate);
			WebElement startTimeAB = driver.findElement(locators.get("signupCampaignsPage.startTimeAB"));
			utils.waitTillVisibilityOfElement(startTimeAB, "start time AB");
			startTimeAB.click();
			if (isLastDate) {
				By nextAvailableBy = By.xpath("(//thead/tr/th[@class='next available']/i)[1]");
			WebElement nextAvailable = driver.findElement(nextAvailableBy);
			nextAvailable.click();

			}
			List<WebElement> startDateListAB = driver.findElements(locators.get("signupCampaignsPage.startDateListAB"));
			utils.selectListDrpDwnValue(startDateListAB, nextdayDate);

			// select class
			WebElement dateRangePicker = driver.findElement(locators.get("signupCampaignsPage.dateRangePicker"));
			utils.selectDrpDwnValue(dateRangePicker, "5");
			WebElement applyBtn = driver.findElement(locators.get("signupCampaignsPage.applyBtn"));
			applyBtn.click();
		} catch (NumberFormatException e) {
			logger.info("Infi==" + e);
			e.printStackTrace();
		}
	}

	public void createWhatDetailsPostredemptionCampaign(String name, String giftType, String giftreason,
			String redemable) throws InterruptedException {

		driver.findElement(locators.get("signupCampaignsPage.postRedemptionCampaignName")).clear();
		WebElement postRedemptionCampaignName = driver.findElement(locators.get("signupCampaignsPage.postRedemptionCampaignName"));
		postRedemptionCampaignName.sendKeys(name);
		logger.info("Entered Postredemption campaign name : " + name);
		WebElement postRedemptiongiftTypeDrp = driver.findElement(locators.get("signupCampaignsPage.postRedemptiongiftTypeDrp"));
		postRedemptiongiftTypeDrp.click();

		List<WebElement> postRedemptiongiftTypeList = driver.findElements(locators.get("signupCampaignsPage.postRedemptiongiftTypeList"));
		utils.selectListDrpDwnValue(postRedemptiongiftTypeList, giftType);

		driver.findElement(locators.get("signupCampaignsPage.postRedemptiongiftReason")).clear();
		WebElement postRedemptiongiftReason = driver.findElement(locators.get("signupCampaignsPage.postRedemptiongiftReason"));
		postRedemptiongiftReason.sendKeys(giftreason);
		logger.info("Entered gift reason");

		Thread.sleep(5000);
		WebElement postredemptionredemableDrp = driver.findElement(locators.get("signupCampaignsPage.postredemptionredemableDrp"));
		postredemptionredemableDrp.click();

		List<WebElement> postRedemptioredemableList = driver.findElements(locators.get("signupCampaignsPage.postRedemptioredemableList"));
		utils.selectListDrpDwnValue(postRedemptioredemableList, redemable);
		logger.info("Entered redeemable as : " + redemable);
		clickNextBtn();
		logger.info("Clicked next button");
		TestListeners.extentTest.get().pass("post redemption campaign what details entered successfully");
	}

	public void setRedeemableRedemption(String redeemable, String redemptionStatus) {
		WebElement redeemableRedemptionDrp = driver.findElement(locators.get("signupCampaignsPage.redeemableRedemptionDrp"));
		redeemableRedemptionDrp.click();
		List<WebElement> redeemableRedemptionDrpList = driver.findElements(locators.get("signupCampaignsPage.redeemableRedemptionDrpList"));
		utils.selectListDrpDwnValue(redeemableRedemptionDrpList, redeemable);
		TestListeners.extentTest.get().pass("Entered redeemable type successfully");
		try {
			WebElement redemptionStatusDrp = driver.findElement(locators.get("signupCampaignsPage.redemptionStatusDrp"));
			redemptionStatusDrp.click();
			List<WebElement> redemptionStatusDrpList = driver.findElements(locators.get("signupCampaignsPage.redemptionStatusDrpList"));
			utils.selectListDrpDwnValue(redemptionStatusDrpList, redemptionStatus);
		} catch (Exception e) {
			TestListeners.extentTest.get().info("redemptionStatusDrp is not clicked and select");
		}
	}

	public void setAmountandPaymentType(String minAmount, String maxAmount, String payment) {

		driver.findElement(locators.get("signupCampaignsPage.minAmount")).clear();
		WebElement minAmountElement = driver.findElement(locators.get("signupCampaignsPage.minAmount"));
		minAmountElement.sendKeys(minAmount);

		driver.findElement(locators.get("signupCampaignsPage.maxAmount")).clear();
		WebElement maxAmountElement = driver.findElement(locators.get("signupCampaignsPage.maxAmount"));
		maxAmountElement.sendKeys(maxAmount);

		WebElement paymentType = driver.findElement(locators.get("signupCampaignsPage.paymentType"));
		paymentType.sendKeys(Keys.ENTER);
		List<WebElement> paymentTypeList = driver.findElements(locators.get("signupCampaignsPage.paymentTypeList"));
		utils.selectListDrpDwnValue(paymentTypeList, payment);
		TestListeners.extentTest.get().pass("Entered min/max amount with payment type");
	}

	public void setPNforPostMessageCampaign(String pn, String mailSubject, String mailBody) {
		driver.findElement(locators.get("signupCampaignsPage.postmessagePN")).clear();
		WebElement postmessagePN = driver.findElement(locators.get("signupCampaignsPage.postmessagePN"));
		postmessagePN.sendKeys(pn);

		driver.findElement(locators.get("signupCampaignsPage.postmessageMailSubject")).clear();
		WebElement postmessageMailSubject = driver.findElement(locators.get("signupCampaignsPage.postmessageMailSubject"));
		postmessageMailSubject.sendKeys(mailSubject);

		driver.findElement(locators.get("signupCampaignsPage.postmessageMailBody")).clear();
		WebElement postmessageMailBody = driver.findElement(locators.get("signupCampaignsPage.postmessageMailBody"));
		postmessageMailBody.sendKeys(mailBody);

	}

	public void setReferenceDateGuest(String reference) {
		// should the above reward be given in the month of signup? on
		utils.longWaitInSeconds(5);
		WebElement checkboxType = driver.findElement(locators.get("signupCampaignsPage.checkboxType"));
		checkboxType.click();

		WebElement referenceDateDrp = driver.findElement(locators.get("signupCampaignsPage.referenceDateDrp"));
		referenceDateDrp.click();
		List<WebElement> referenceDateDrpList = driver.findElements(locators.get("signupCampaignsPage.referenceDateDrpList"));
		utils.selectListDrpDwnValue(referenceDateDrpList, reference);
	}

	public void setReferenceDateOnGuest(String reference) {

		WebElement referenceDateDrp = driver.findElement(locators.get("signupCampaignsPage.referenceDateDrp"));
		referenceDateDrp.click();
		List<WebElement> referenceDateDrpList = driver.findElements(locators.get("signupCampaignsPage.referenceDateDrpList"));
		utils.selectListDrpDwnValue(referenceDateDrpList, reference);
	}

	public void setLifespanDuration(String duration) {
		WebElement lifespanDurationDrp = driver.findElement(locators.get("signupCampaignsPage.lifespanDurationDrp"));
		lifespanDurationDrp.click();
		List<WebElement> lifespanDurationDrpList = driver.findElements(locators.get("signupCampaignsPage.lifespanDurationDrpList"));
		utils.selectListDrpDwnValue(lifespanDurationDrpList, duration);
	}

	public void setLifespanDurationFixedDate(String date) {
		WebElement expiryDate = driver.findElement(locators.get("signupCampaignsPage.fixedExpiryDate"));
		expiryDate.sendKeys(date);
	}

	public void setExpeiryDateAnniversaryCampaign() {
		String fullDate = CreateDateTime.getCurrentDate();
		String date[] = fullDate.split("-");
		String currentDate = date[2];
		if (currentDate.charAt(0) == '0') {
			currentDate = Character.toString(currentDate.charAt(1));
		}
		WebElement expiryDate = driver.findElement(locators.get("signupCampaignsPage.expiryDate"));
		expiryDate.click();
		List<WebElement> startDateList = driver.findElements(locators.get("signupCampaignsPage.startDateList"));
		utils.selectListDrpDwnValue(startDateList, currentDate);
	}

	/*
	 * public void setUserFileds() { List<String> userFields =
	 * Arrays.asList("Birthday", "Phone", "Gender", "First Name", "Last Name",
	 * "Zip Code"); driver.findElement(locators.get("signupCampaignsPage.userFieldsDrp")).click();
	 * 
	 * int size = userFields.size(); for (int i = 0; i < size; i++) { String val =
	 * userFields.get(i); List<WebElement> options =
	 * driver.findElements(locators.get("signupCampaignsPage.userFieldsDrpList"));
	 * utils.selectListDrpDwnValue(options, val); }
	 * driver.findElement(locators.get("signupCampaignsPage.userFieldsDrp")).click();
	 * driver.findElement(locators.get("signupCampaignsPage.deviceSelect")).click(); }
	 */

	public void setUserFileds(String option) {

		WebElement userFieldsDrp = driver.findElement(locators.get("signupCampaignsPage.userFieldsDrp"));
		userFieldsDrp.click();
		List<WebElement> userFieldsDrpList = driver.findElements(locators.get("signupCampaignsPage.userFieldsDrpList"));
		utils.selectListDrpDwnValue(userFieldsDrpList, option);
		WebElement userFieldsDrpAgain = driver.findElement(locators.get("signupCampaignsPage.userFieldsDrp"));
		userFieldsDrpAgain.click();
		WebElement deviceSelect = driver.findElement(locators.get("signupCampaignsPage.deviceSelect"));
		deviceSelect.click();
	}

	public void setStartTime() {

		String fullDate = CreateDateTime.getCurrentDate();
		String date[] = fullDate.split("-");
		String currentDate = date[2];
		int i = Integer.parseInt(currentDate);
		boolean isLastDate = utils.checkLsatDateOfMonth(i);
		int newDate;
		if (isLastDate) {
			newDate = 1;
		} else {
			newDate = i + 1;
		}
		String nextdayDate = Integer.toString(newDate);
		WebElement startTime = driver.findElement(locators.get("signupCampaignsPage.startTime"));
		startTime.click();
		if (isLastDate) {
			By nextMonthBy = By.xpath("//div[@class='calendar left single']//thead//th[@class='next available']");
			WebElement nextMonth = driver.findElement(nextMonthBy);
			nextMonth.click();

		}
		List<WebElement> startDateListAB = driver.findElements(locators.get("signupCampaignsPage.startDateListAB"));
		utils.selectListDrpDwnValue(startDateListAB, nextdayDate);

		// select class
		/*
		 * WebElement elem= driver.findElement(locators.get("signupCampaignsPage.dateRangePicker"));
		 * utils.selectDrpDwnValue(elem,"5");
		 */
		WebElement applyBtn = driver.findElement(locators.get("signupCampaignsPage.applyBtn"));
		applyBtn.click();
	}

	public void setTimeZone(String value) {
		WebElement timeZone = driver.findElement(locators.get("signupCampaignsPage.timeZone"));
		timeZone.click();
		By textboxBy = By.xpath("//input[@role='textbox']");
		WebElement textbox = driver.findElement(textboxBy);
		textbox.sendKeys(value);
		textbox.sendKeys(Keys.ENTER);

		WebElement saveButton = driver.findElement(locators.get("signupCampaignsPage.SaveButton"));
		saveButton.click();

	}

	public void setCampaignBetaName(String name) {

		driver.findElement(locators.get("signupCampaignsPage.campaignBetaNameTextbox")).clear();
		WebElement campaignBetaNameTextbox = driver.findElement(locators.get("signupCampaignsPage.campaignBetaNameTextbox"));
		campaignBetaNameTextbox.sendKeys(name);
		logger.info("Entered mass notification campaign name: " + name);
		TestListeners.extentTest.get().info("Entered mass notification campaign name: " + name);

		logger.info("Entered campaign name");
		TestListeners.extentTest.get().info("Entered campaign name");
	}

	public void clickContinueBtn() {
		WebElement continueButton = driver.findElement(locators.get("signupCampaignsPage.continueButton"));
		continueButton.click();
		logger.info("Clicked next button");
	}

	public void verifyScheduledRedeemableInSignupCampaign(String name, String redeemableName) {
		driver.findElement(locators.get("signupCampaignsPage.signupCampaignName")).clear();
		WebElement signupCampaignName = driver.findElement(locators.get("signupCampaignsPage.signupCampaignName"));
		signupCampaignName.sendKeys(name);
		logger.info("Entered campaign name");
		WebElement giftTypeDrp = driver.findElement(locators.get("signupCampaignsPage.giftTypeDrp"));
		giftTypeDrp.click();
		String redeemable = "Gift Redeemable";
		List<WebElement> giftTypeList = driver.findElements(locators.get("signupCampaignsPage.giftTypeList"));
		utils.selectListDrpDwnValue(giftTypeList, redeemable);

//		driver.findElement(locators.get("signupCampaignsPage.giftReason")).clear();
//		driver.findElement(locators.get("signupCampaignsPage.giftReason")).sendKeys(giftreason);
//		logger.info("Entered gift reason");
		selUtils.longWait(5000);
		WebElement postredemptionredemableDrp = driver.findElement(locators.get("signupCampaignsPage.postredemptionredemableDrp"));
		selUtils.jsClick(postredemptionredemableDrp);
		List<WebElement> redemableList = driver.findElements(locators.get("signupCampaignsPage.redemableList"));
		for (int i = 0; i < redemableList.size(); i++) {
			if (redemableList.get(i).getText().equalsIgnoreCase(redeemableName)) {
				logger.error("Scheduled redeemable is appearing under gift redeemable: " + redeemableName);
				TestListeners.extentTest.get()
						.fail("Scheduled redeemable is appearing under gift redeemable: " + redeemableName);
				break;
			}
		}

		logger.info(
				"Verfied Deal created with future start_date is not appearing for signup campaign under gift redeemable");
		TestListeners.extentTest.get().pass(
				"Verfied Deal created with future start_date is not appearing for signup campaign under gift redeemable");

	}

	// sha
	public void createWhatDetailsPromoCampaign(String name, String promoCode) {
		driver.findElement(locators.get("signupCampaignsPage.couponCampaignname")).clear();
		WebElement couponCampaignname = driver.findElement(locators.get("signupCampaignsPage.couponCampaignname"));
		couponCampaignname.sendKeys(name);
		logger.info("Entered coupon campaign name: " + name);
		TestListeners.extentTest.get().info("Entered coupon campaign name: " + name);

		WebElement promoCodeInputBox = driver.findElement(locators.get("signupCampaignsPage.promoCodeInputBox"));
		promoCodeInputBox.sendKeys(promoCode);
		logger.info(promoCode + " promo code is entered ");
		TestListeners.extentTest.get().info(promoCode + " promo code is entered ");
		WebElement nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		nextBtn.click();
		logger.info("Clicked next button");
		TestListeners.extentTest.get().pass("New coupon campaign what details entered successfully");
	}

	// sha
	public void setPromoCampaignGuestUsagewithGiftAmount(String noOfGuests, String giftType, String amount,
			String locationName, String qcName) {

		driver.findElement(locators.get("signupCampaignsPage.numberOfGuests")).clear();
		WebElement numberOfGuests = driver.findElement(locators.get("signupCampaignsPage.numberOfGuests"));
		numberOfGuests.sendKeys(noOfGuests);

		WebElement couponGiftTypeDrp = driver.findElement(locators.get("signupCampaignsPage.couponGiftTypeDrp"));
		couponGiftTypeDrp.click();
		List<WebElement> couponGiftTypeDrpList = driver.findElements(locators.get("signupCampaignsPage.couponGiftTypeDrpList"));
		utils.selectListDrpDwnValue(couponGiftTypeDrpList, giftType);

		driver.findElement(locators.get("signupCampaignsPage.amountDiscount")).clear();
		WebElement amountDiscount = driver.findElement(locators.get("signupCampaignsPage.amountDiscount"));
		amountDiscount.sendKeys(amount);

		if (locationName != "") {
			WebElement addLocationDropBox = driver.findElement(locators.get("signupCampaignsPage.addLocationDropBox"));
			addLocationDropBox.click();

			String addLocationUpdatedXpath = utils.getLocatorValue("signupCampaignsPage.selectLocationInDropBox")
					.replace("$LocationName", locationName);
			By selectLocationBy = By.xpath(addLocationUpdatedXpath);
			WebElement selectLocationInDropBox = driver.findElement(selectLocationBy);
			selectLocationInDropBox.click();
		}
		if (qcName != "") {
			By qualificationCriterionBy = By.xpath("//select[@id='coupon_campaign_qualification_criterion_id']");
			WebElement qualificationCriterionSelect = driver.findElement(qualificationCriterionBy);
			utils.selectDrpDwnValue(qualificationCriterionSelect, qcName);
		}

		WebElement nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		nextBtn.click();
		logger.info("Clicked next button");
		TestListeners.extentTest.get().pass("New coupon campaign whom details entered successfully");
	}

	public void createWhenRecurringMassCampaign(String frequency, String dateTime, String dateTime1) {
		try {
			WebElement massFrequencyDrp = driver.findElement(locators.get("signupCampaignsPage.massFrequencyDrp"));
			massFrequencyDrp.click();
			List<WebElement> massFrequencyDrpList = driver.findElements(locators.get("signupCampaignsPage.massFrequencyDrpList"));
			utils.selectListDrpDwnValue(massFrequencyDrpList, frequency);
			driver.findElement(locators.get("signupCampaignsPage.startTimeTxtBox")).clear();
			WebElement startTimeTxtBox = driver.findElement(locators.get("signupCampaignsPage.startTimeTxtBox"));
			startTimeTxtBox.sendKeys(dateTime);
//			driver.findElement(locators.get("signupCampaignsPage.dateApplyButton")).click();
//			Thread.sleep(2000);
			clickOnDateApplyButton();
			// driver.findElement(locators.get("signupCampaignsPage.dailyEndDate")).click();
			// driver.findElement(locators.get("signupCampaignsPage.calendarDateList")).isDisplayed();
//			driver.findElement(locators.get("signupCampaignsPage.dailyEndDate")).sendKeys(dateTime1);
//			driver.findElement(locators.get("signupCampaignsPage.dateApplyButton")).click();
			setEndDateforMassOfferCampaign();
			clickOnDateApplyButton();

//			Select sel = new Select(driver.findElement(By.id("schedule_timezone")));
//			sel.selectByVisibleText("(GMT+05:30) Kolkata ( IST )");
			WebElement scheduleBtn = driver.findElement(locators.get("signupCampaignsPage.scheduleBtn"));
			scheduleBtn.click();
			logger.info("Clicked schedule button");
			utils.acceptAlert(driver);
			TestListeners.extentTest.get()
					.pass("New recurring mass gifting campaign when details entered successfully");

		} catch (Exception e) {
			logger.error("Error in entering when details for new mass gifting campaign" + e);
			TestListeners.extentTest.get().fail("Error in entering when details for new mass gifting campaign" + e);
		}
	}

	public void clickOnDateApplyButton() {
		List<WebElement> dateApplyButtonList = driver.findElements(locators.get("signupCampaignsPage.dateApplyButton"));
		for (WebElement dateApplyButton : dateApplyButtonList) {
			try {
				dateApplyButton.click();
				System.out.println("SignupCampaignPage.clickOnDateApplyButton() Clicked on Apply button");
				break;
			} catch (Exception e) {
			}
		}
	}

	public void setEndDateforMassOfferCampaign() throws InterruptedException {
		Thread.sleep(3000);
		String fullDate = CreateDateTime.getFutureDate(6);
		String date[] = fullDate.split("-");
		String currentDate = date[2];

		int newDate = Integer.parseInt(currentDate);
		String nextdayDate = Integer.toString(newDate);
		By scheduleEndTimeBy = By.xpath("//input[@id='schedule_end_time']");
		By availableDatesBy = By.xpath("//tbody/tr/td[contains(@class,'available')]");
		try {
			WebElement scheduleEndTime = driver.findElement(scheduleEndTimeBy);
			scheduleEndTime.click();
			List<WebElement> availableDates = driver.findElements(availableDatesBy);
			utils.selectListDrpDwnValue(availableDates, nextdayDate);
		} catch (Exception e) {
			WebElement scheduleEndTime = driver.findElement(scheduleEndTimeBy);
			scheduleEndTime.click();
			clickNextButtonForMonth();
			List<WebElement> availableDates = driver.findElements(availableDatesBy);
			utils.selectListDrpDwnValue(availableDates, nextdayDate);
		}
	}

	public void clickNextButtonForMonth() {
		List<WebElement> elem = driver
				.findElements(By.xpath("//div[@class='calendar-table']//th[@class='next available']"));
		for (WebElement ele : elem) {
			try {
				ele.click();
				System.out.println("Clicked on next button");
				break;
			} catch (Exception e) {
			}
		}
	}

	public int guestsReachCount() throws InterruptedException {
		// utils.waitTillPagePaceDone();
		// utils.waitTillElementDisappear("//span[@class='highcharts-loading-inner']");
		try {
			By highchartsLoadingBy = By.xpath("//span[@class='highcharts-loading-inner']");
			WebElement highchartsLoading = driver.findElement(highchartsLoadingBy);
			utils.waitTillElementDisappear(highchartsLoading);
		} catch (Exception e) {
		}

		WebElement guestReach = driver.findElement(locators.get("signupCampaignsPage.guestReach"));
		utils.waitTillVisibilityOfElement(guestReach, "Guest Count");
		guestReach.isDisplayed();
		String val = guestReach.getText().replaceAll("[^0-9]", "");
		int count = Integer.parseInt(val);
		return count;

	}

	public void setPostCheckinPushNotification(String pushNotification) {
		driver.findElement(locators.get("signupCampaignsPage.postCheckinPushNotification")).clear();
		WebElement postCheckinPushNotification = driver.findElement(locators.get("signupCampaignsPage.postCheckinPushNotification"));
		postCheckinPushNotification.sendKeys(pushNotification);
	}

	public void setPostCheckinPushEmailSubject(String emailSubject) {
		driver.findElement(locators.get("signupCampaignsPage.postCheckinSubjectTextBox")).clear();
		WebElement postCheckinSubjectTextBox = driver.findElement(locators.get("signupCampaignsPage.postCheckinSubjectTextBox"));
		postCheckinSubjectTextBox.sendKeys(emailSubject);
	}

	public void setPostCheckinEmailTemplate(String emailTemplate) {
		driver.findElement(locators.get("signupCampaignsPage.postCheckinEmailTemplate")).clear();
		WebElement postCheckinEmailTemplate = driver.findElement(locators.get("signupCampaignsPage.postCheckinEmailTemplate"));
		postCheckinEmailTemplate.sendKeys(emailTemplate);
	}

	public void setPostCheckinGiftReason(String giftreason) {
		driver.findElement(locators.get("signupCampaignsPage.postRedemptiongiftReason")).clear();
		WebElement postRedemptiongiftReason = driver.findElement(locators.get("signupCampaignsPage.postRedemptiongiftReason"));
		postRedemptiongiftReason.sendKeys(giftreason);
		logger.info("Entered gift reason");
	}

	public void setCouponCampaignGuestUsagewithGiftAmountForMobile(String noOfGuests, String usagePerGuest,
			String giftType, String amount, String locationName, String qcName,
			boolean globalRedemptionThrottlingToggle) {

		driver.findElement(locators.get("signupCampaignsPage.numberOfGuests")).clear();
		WebElement numberOfGuests = driver.findElement(locators.get("signupCampaignsPage.numberOfGuests"));
		numberOfGuests.sendKeys(noOfGuests);

		driver.findElement(locators.get("signupCampaignsPage.usagePerGuest")).clear();
		WebElement usagePerGuestElement = driver.findElement(locators.get("signupCampaignsPage.usagePerGuest"));
		usagePerGuestElement.sendKeys(usagePerGuest);

		WebElement couponGiftTypeDrp = driver.findElement(locators.get("signupCampaignsPage.couponGiftTypeDrp"));
		couponGiftTypeDrp.click();
		List<WebElement> couponGiftTypeDrpList = driver.findElements(locators.get("signupCampaignsPage.couponGiftTypeDrpList"));
		utils.selectListDrpDwnValue(couponGiftTypeDrpList, giftType);

		if (giftType.equalsIgnoreCase("Gift Points")) {
			driver.findElement(locators.get("signupCampaignsPage.giftpointForCoupon")).clear();
			WebElement giftpointForCoupon = driver.findElement(locators.get("signupCampaignsPage.giftpointForCoupon"));
			giftpointForCoupon.sendKeys(amount);
			driver.findElement(locators.get("signupCampaignsPage.giftReasonForCoupon")).clear();
			WebElement giftReasonForCoupon = driver.findElement(locators.get("signupCampaignsPage.giftReasonForCoupon"));
			giftReasonForCoupon.sendKeys("test");
		} else if (giftType.equalsIgnoreCase("Gift Redeemable")) {

			driver.findElement(locators.get("signupCampaignsPage.percentageDiscountInput")).clear();
			WebElement percentageDiscountInput = driver.findElement(locators.get("signupCampaignsPage.percentageDiscountInput"));
			percentageDiscountInput.sendKeys(amount);
		}

		if (locationName != "") {
			WebElement addLocationDropBox = driver.findElement(locators.get("signupCampaignsPage.addLocationDropBox"));
			addLocationDropBox.click();

			String addLocationUpdatedXpath = utils.getLocatorValue("signupCampaignsPage.selectLocationInDropBox")
					.replace("$LocationName", locationName);
			By selectLocationBy = By.xpath(addLocationUpdatedXpath);
			WebElement selectLocationInDropBox = driver.findElement(selectLocationBy);
			selectLocationInDropBox.click();
		}
		if (globalRedemptionThrottlingToggle) {
			By noThrottlingBy = By.xpath("//span[@title='No Throttling']");
			WebElement noThrottling = driver.findElement(noThrottlingBy);
			noThrottling.click();
			By perDayBy = By.xpath(
					"//ul[@id='select2-coupon_campaign_usage_throttling_unit-results']/li[contains(text(),'per day')]");
			WebElement perDay = driver.findElement(perDayBy);
			perDay.click();
			By throttlingInputBy = By.xpath("//input[@id='coupon_campaign_usage_throttling']");
			WebElement throttlingInput = driver.findElement(throttlingInputBy);
			throttlingInput.sendKeys("1");

		}
		if (qcName != "") {
			By qualificationCriterionBy = By.xpath("//select[@id='coupon_campaign_qualification_criterion_id']");
			WebElement qualificationCriterionSelect = driver.findElement(qualificationCriterionBy);
			utils.selectDrpDwnValue(qualificationCriterionSelect, qcName);
		}

		WebElement nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		nextBtn.click();
		logger.info("Clicked next button");
		TestListeners.extentTest.get().pass("New coupon campaign whom details entered successfully");
	}

	public void setDelightGuestWithValue(String reward) {

		WebElement delightGuestWithDrop = driver.findElement(locators.get("signupCampaignsPage.delightGuestWithDrop"));
		delightGuestWithDrop.click();
		List<WebElement> delightGuestWithDropList = driver.findElements(locators.get("signupCampaignsPage.delightGuestWithDropList"));
		utils.selectListDrpDwnValue(delightGuestWithDropList, reward);

	}

	public void setSurpriseOnceEvery(String value) {

		driver.findElement(locators.get("signupCampaignsPage.surpriseOnceEveryBox")).clear();
		WebElement surpriseOnceEveryBox = driver.findElement(locators.get("signupCampaignsPage.surpriseOnceEveryBox"));
		surpriseOnceEveryBox.sendKeys(value);
		logger.info("Entered Surprise Once Every as " + value);
		TestListeners.extentTest.get().pass("Entered Surprise Once Every as " + value);
	}

	public void setSendNotificationGuest(String guest) {
		driver.findElement(locators.get("signupCampaignsPage.testUserEmail")).clear();
		WebElement testUserEmail = driver.findElement(locators.get("signupCampaignsPage.testUserEmail"));
		testUserEmail.sendKeys(guest);
		WebElement includeGiftCheckBox = driver.findElement(locators.get("signupCampaignsPage.includeGiftCheckBox"));
		includeGiftCheckBox.click();
		WebElement sendTestNotificationBtn = driver.findElement(locators.get("signupCampaignsPage.sendTestNotificationBtn"));
		sendTestNotificationBtn.click();

	}

	public void setEndDateforAnniversaryCampaign() {
		String fullDate = CreateDateTime.getCurrentDate();
		String date[] = fullDate.split("-");
		String currentDate = date[2];
		int i = Integer.parseInt(currentDate);
		int newDate = i + 9;
		String nextdayDate = Integer.toString(newDate);
		WebElement endDate = driver.findElement(locators.get("signupCampaignsPage.endDate"));
		endDate.click();
		List<WebElement> startDateList = driver.findElements(locators.get("signupCampaignsPage.startDateList"));
		utils.selectListDrpDwnValue(startDateList, nextdayDate);

	}

	public void setDaysBefore(String dyas) {
		WebElement daysBeforeTextbox = driver.findElement(locators.get("signupCampaignsPage.daysBeforeTextbox"));
		daysBeforeTextbox.click();
		driver.findElement(locators.get("signupCampaignsPage.daysBeforeTextbox")).clear();
		WebElement daysBeforeTextboxForSendKeys = driver.findElement(locators.get("signupCampaignsPage.daysBeforeTextbox"));
		daysBeforeTextboxForSendKeys.sendKeys(dyas);
	}

	public void setFirstDateofMonth() {
		String fullDate = CreateDateTime.getFirstDateOfMonth();
		String date[] = fullDate.split("-");
		String currentDate = date[2];
		if (currentDate.charAt(0) == '0') {
			currentDate = Character.toString(currentDate.charAt(1));
		}
		WebElement startDate = driver.findElement(locators.get("signupCampaignsPage.startDate"));
		startDate.click();
		WebElement nextMonthicon = driver.findElement(locators.get("signupCampaignsPage.nextMonthicon"));
		nextMonthicon.click();
		List<WebElement> startDateList = driver.findElements(locators.get("signupCampaignsPage.startDateList"));
		utils.selectListDrpDwnValue(startDateList, currentDate);
	}

	public void setCampaignTrigger(String val) {
		utils.longWaitInSeconds(4);
		WebElement campaignTriggerDrp = driver.findElement(locators.get("signupCampaignsPage.campaignTriggerDrp"));
		campaignTriggerDrp.click();
		List<WebElement> campaignTriggerDrpList = driver.findElements(locators.get("signupCampaignsPage.campaignTriggerDrpList"));
		utils.selectListDrpDwnValue(campaignTriggerDrpList, val);
		TestListeners.extentTest.get().pass("Selected campaign trigger type successfully");
	}

	public void setCampTriggerRedeemable(String val) {
		WebElement campaignTriggerRedeemableDrp = driver.findElement(locators.get("signupCampaignsPage.campaignTriggerRedeemableDrp"));
		utils.waitTillElementToBeClickable(campaignTriggerRedeemableDrp);
		campaignTriggerRedeemableDrp.click();
		List<WebElement> campaignTriggerRedeemableDrpList = driver.findElements(locators.get("signupCampaignsPage.campaignTriggerRedeemableDrpList"));
		utils.selectListDrpDwnValue(campaignTriggerRedeemableDrpList, val);
		TestListeners.extentTest.get().pass("Selected redeemable name successfully");
	}

	public void turnOnAvailableAsATemplate() {
		try {
			WebElement enableAvailableTemplateButton = driver.findElement(locators.get("signupCampaignsPage.enableAvailableTemplateButton"));
			enableAvailableTemplateButton.click();
		} catch (Exception e) {
			logger.info("Not able to Template option");
			TestListeners.extentTest.get().fail("Not able to Template option " + e.getStackTrace());

		}
	}

	public void setCamTimeZone(String timeZone) {
		By timezoneSelectBy = By.xpath("//Select[contains(@id,'timezone')]");
		WebElement timezoneSelect = driver.findElement(timezoneSelectBy);
		Select sel = new Select(timezoneSelect);
		sel.selectByVisibleText(timeZone);
		logger.info("Timezone selected ");
	}

	public void createWhatDetailsPostcheckinCampaignUsingGiftPoints(String name, String giftType, String giftreason,
			String giftPoint) {

		driver.findElement(locators.get("signupCampaignsPage.postCheckinCampaignName")).clear();
		WebElement postCheckinCampaignName = driver.findElement(locators.get("signupCampaignsPage.postCheckinCampaignName"));
		postCheckinCampaignName.sendKeys(name);
		logger.info("Entered Postcheckin campaign name");
		WebElement postCheckingiftTypeDrp = driver.findElement(locators.get("signupCampaignsPage.postCheckingiftTypeDrp"));
		postCheckingiftTypeDrp.click();

		List<WebElement> postCheckingiftTypeList = driver.findElements(locators.get("signupCampaignsPage.postCheckingiftTypeList"));
		utils.selectListDrpDwnValue(postCheckingiftTypeList, giftType);

		driver.findElement(locators.get("signupCampaignsPage.postCheckingiftReason")).clear();
		WebElement postCheckingiftReason = driver.findElement(locators.get("signupCampaignsPage.postCheckingiftReason"));
		postCheckingiftReason.sendKeys(giftreason);
		logger.info("Entered gift reason");

		// Thread.sleep(5000);
		WebElement giftPointsTextBox = driver.findElement(locators.get("signupCampaignsPage.giftPointsTextBox"));
		giftPointsTextBox.sendKeys(giftPoint);

		WebElement nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		nextBtn.click();
		logger.info("Clicked next button");

		TestListeners.extentTest.get().pass("New post checkin campaign what details entered successfully");

	}

	public void createWhomDetailsPosCheckinCampaignFavPhoneNumber(String audiance, String segement,
			String pushNotification, String emailSubject, String emailTemplate) {

//			driver.findElement(locators.get("signupCampaignsPage.audianceTypeDrp")).click();
//			List<WebElement> elements = driver.findElements(locators.get("signupCampaignsPage.audianceTypeDrpList"));
//			utils.selectListDrpDwnValue(elements, audiance);
		WebElement segmentDrp = driver.findElement(locators.get("signupCampaignsPage.segmentDrp"));
		segmentDrp.click();
		List<WebElement> segmentDrpList = driver.findElements(locators.get("signupCampaignsPage.segmentDrpList"));
		utils.selectListDrpDwnValue(segmentDrpList, segement);

//			driver.findElement(locators.get("signupCampaignsPage.massPushNotificationTxtBox")).clear();
//			driver.findElement(locators.get("signupCampaignsPage.massPushNotificationTxtBox")).sendKeys(pushNotification);
//			driver.findElement(locators.get("signupCampaignsPage.massEmailSubjectTxtBox")).clear();
//			driver.findElement(locators.get("signupCampaignsPage.massEmailSubjectTxtBox")).sendKeys(emailSubject);
//			driver.findElement(locators.get("signupCampaignsPage.massTemplateTxtBox")).clear();
//			driver.findElement(locators.get("signupCampaignsPage.massTemplateTxtBox")).sendKeys(emailTemplate);
		WebElement nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		nextBtn.click();
		logger.info("Clicked next button");
		TestListeners.extentTest.get().pass("New mass campaign whom details entered successfully");
	}

	public void editGiftPointsClassicCampaign(String updatePoints) {
		// utils.waitTillVisibilityOfElement(driver.findElement(locators.get("campaignsPage.optionBtn"),
		// "option button");
		utils.longWaitInSeconds(2);
		WebElement optionBtn = driver.findElement(locators.get("campaignsPage.optionBtn"));
		utils.waitTillElementToBeClickable(optionBtn);
		// utils.StaleElementclick(driver, driver.findElement(locators.get("campaignsPage.optionBtn")));
		utils.clickByJSExecutor(driver, optionBtn);
		// driver.findElement(locators.get("campaignsPage.optionBtn")).click();
		logger.info("clicked on the option button");
		TestListeners.extentTest.get().info("clicked on the option button");

		WebElement classicPageBtn = driver.findElement(locators.get("campaignsPage.classicPageBtn"));
		utils.waitTillVisibilityOfElement(classicPageBtn, "classic button");
		utils.waitTillElementToBeClickable(classicPageBtn);
		classicPageBtn.click();
		logger.info("clicked on the classic page button");
		TestListeners.extentTest.get().info("clicked on the classic page button");

		utils.waitTillCompletePageLoad();
		WebElement ellipsisBtn = driver.findElement(locators.get("campaignsPage.ellipsisBtn"));
		utils.waitTillVisibilityOfElement(ellipsisBtn, "ellipsis button");
		ellipsisBtn.click();
		WebElement editBtn = driver.findElement(locators.get("campaignsPage.EditBtn"));
		utils.waitTillVisibilityOfElement(editBtn, "edit button");
		editBtn.click();
		logger.info("clicked on the edit button");
		TestListeners.extentTest.get().info("clicked on the edit button");

		utils.waitTillCompletePageLoad();
		setGiftPoints(updatePoints);
		clickNextBtn();
		utils.waitTillCompletePageLoad();
		clickNextBtn();
		utils.waitTillCompletePageLoad();
	}

	/*
	 * public static boolean compareDatesWithLastDateOfMonth(String nextdayDate)
	 * throws ParseException { boolean resultCompareFlag = false; nextdayDate =
	 * nextdayDate + " 02:30 PM"; Date today = new Date(); Calendar calendar =
	 * Calendar.getInstance(); calendar.setTime(today); calendar.add(Calendar.MONTH,
	 * 1); calendar.set(Calendar.DAY_OF_MONTH, 1); calendar.add(Calendar.DATE, -1);
	 * Date lastDayOfMonth = calendar.getTime(); DateFormat sdf = new
	 * SimpleDateFormat("yyyy-MM-dd"); String lastDateOfMonth =
	 * sdf.format(lastDayOfMonth); Date lastDateOfMnth = sdf.parse(lastDateOfMonth);
	 * Date futureDateToSelect = sdf.parse(nextdayDate); if
	 * (futureDateToSelect.compareTo(lastDateOfMnth) == 1) { resultCompareFlag =
	 * true; logger.info(futureDateToSelect +
	 * " date is greater than last date of the month " + lastDateOfMnth);
	 * TestListeners.extentTest.get() .info(futureDateToSelect +
	 * " date is greater than last date of the month " + lastDateOfMnth); } else {
	 * resultCompareFlag = false; logger.info(futureDateToSelect +
	 * " date is Less/Smaller than last date of the month " + lastDateOfMnth);
	 * TestListeners.extentTest.get() .info(futureDateToSelect +
	 * " date is Less/Smaller than last date of the month " + lastDateOfMnth); }
	 * 
	 * return resultCompareFlag;
	 * 
	 * }
	 */
	public static boolean compareDatesWithLastDateOfMonth(String nextdayDate) throws ParseException {
		boolean resultCompareFlag = false;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		// Parse the input string to a Date object
		Date date = sdf.parse(nextdayDate);

		// Create a Calendar instance and set the date
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);

		// Get the current day of the month
		int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

		// Move to the next day
		calendar.add(Calendar.DAY_OF_MONTH, 1);

		// Check if the next day is a new month (i.e., if it's the first day of the next
		// month)
		int nextDay = calendar.get(Calendar.DAY_OF_MONTH);

		// If the next day is 1, then the input date is the last day of the month
		if (nextDay == 1) {
			resultCompareFlag = true;
		}
		return resultCompareFlag;
	}

	public void bounceBackConversionDrpDown(String val) {
		WebElement bounceBackCampaignConverionDrpDown = driver.findElement(locators.get("signupCampaignsPage.bounceBackCampaignConverionDrpDown"));
		utils.selectDrpDwnValue(bounceBackCampaignConverionDrpDown, val);
		logger.info("value selected from the drop down -" + val);
		TestListeners.extentTest.get().info("value selected from the drop down -" + val);
	}

	public void startCampaignDrpDown(String val) {
		WebElement starterCampaign = driver.findElement(locators.get("signupCampaignsPage.starterCampaign"));
		utils.selectDrpDwnValue(starterCampaign, val);
		logger.info("value selected from the drop down -" + val);
		TestListeners.extentTest.get().info("value selected from the drop down -" + val);
	}

	public void selectFinishCampaignDrpdown(String val) {
		WebElement finishCampaign = driver.findElement(locators.get("signupCampaignsPage.finishCampaign"));
		utils.selectDrpDwnValue(finishCampaign, val);
		logger.info("value selected from the drop down -" + val);
		TestListeners.extentTest.get().info("value selected from the drop down -" + val);
	}

	public void setCouponCampaign(String noOfGuests, String usagePerGuest, String giftType, String amount,
			String locationName, String qcName, boolean globalRedemptionThrottlingToggle, String directlyPos,
			String CodeGeneration) {

		if (directlyPos.equalsIgnoreCase("Directly processed at POS")) {
			WebElement directlyProcessedAtPos = driver.findElement(locators.get("signupCampaignsPage.directlyProcessedAtPos"));
			utils.clickByJSExecutor(driver, directlyProcessedAtPos);
		}

		if (CodeGeneration.equalsIgnoreCase("Dynamic Generation")) {
			WebElement dynamicGeneration = driver.findElement(locators.get("signupCampaignsPage.dynamicGeneration"));
			utils.clickByJSExecutor(driver, dynamicGeneration);
		}

		driver.findElement(locators.get("signupCampaignsPage.numberOfGuests")).clear();
		WebElement numberOfGuests = driver.findElement(locators.get("signupCampaignsPage.numberOfGuests"));
		numberOfGuests.sendKeys(noOfGuests);

		driver.findElement(locators.get("signupCampaignsPage.usagePerGuest")).clear();
		WebElement usagePerGuestElement = driver.findElement(locators.get("signupCampaignsPage.usagePerGuest"));
		usagePerGuestElement.sendKeys(usagePerGuest);

		WebElement couponGiftTypeDrp = driver.findElement(locators.get("signupCampaignsPage.couponGiftTypeDrp"));
		couponGiftTypeDrp.click();
		List<WebElement> couponGiftTypeDrpList = driver.findElements(locators.get("signupCampaignsPage.couponGiftTypeDrpList"));
		utils.selectListDrpDwnValue(couponGiftTypeDrpList, giftType);

		if (giftType.equalsIgnoreCase("Gift Points")) {
			driver.findElement(locators.get("signupCampaignsPage.giftpointForCoupon")).clear();
			WebElement giftpointForCoupon = driver.findElement(locators.get("signupCampaignsPage.giftpointForCoupon"));
			giftpointForCoupon.sendKeys(amount);
			driver.findElement(locators.get("signupCampaignsPage.giftReasonForCoupon")).clear();
			WebElement giftReasonForCoupon = driver.findElement(locators.get("signupCampaignsPage.giftReasonForCoupon"));
			giftReasonForCoupon.sendKeys("test");
		} else if (giftType.equalsIgnoreCase("Gift Redeemable")) {
			WebElement couponCampaignRedemable = driver.findElement(locators.get("signupCampaignsPage.couponCampaignRedemable"));
			utils.selectDrpDwnValue(couponCampaignRedemable, amount);
			driver.findElement(locators.get("signupCampaignsPage.giftReasonForCoupon")).clear();
			WebElement giftReasonForCoupon = driver.findElement(locators.get("signupCampaignsPage.giftReasonForCoupon"));
			giftReasonForCoupon.sendKeys("test");
		} else if (giftType.equalsIgnoreCase("% OFF")) {
			driver.findElement(locators.get("signupCampaignsPage.percentageDiscountInput")).clear();
			WebElement percentageDiscountInput = driver.findElement(locators.get("signupCampaignsPage.percentageDiscountInput"));
			percentageDiscountInput.sendKeys(amount);
		}

		WebElement nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		nextBtn.click();
		logger.info("Clicked next button");
		TestListeners.extentTest.get().pass("New coupon campaign whom details entered successfully");
	}

	public void presenceCampaignWhatPage(String campaignName, String giftType, String redeemableName, String reason) {
		WebElement locationPresenceCampaignName = driver.findElement(locators.get("signupCampaignsPage.locationPresenceCampaignName"));
		locationPresenceCampaignName.sendKeys(campaignName);
		logger.info("campaign name -" + campaignName);
		TestListeners.extentTest.get().info("campaign name -" + campaignName);

		WebElement locationPresenceCampaignGiftType = driver.findElement(locators.get("signupCampaignsPage.locationPresenceCampaignGiftType"));
		utils.selectDrpDwnValue(locationPresenceCampaignGiftType, giftType);
		logger.info("gift type selected --" + giftType);
		TestListeners.extentTest.get().info("gift type selected --" + giftType);

		WebElement locationCampaignRedeemableName = driver.findElement(locators.get("signupCampaignsPage.locationCampaignRedeemableName"));
		utils.selectDrpDwnValue(locationCampaignRedeemableName, redeemableName);
		logger.info("redeemable selected --" + redeemableName);
		TestListeners.extentTest.get().info("redeemable selected --" + redeemableName);

		WebElement locationCampaignReason = driver.findElement(locators.get("signupCampaignsPage.locationCampaignReason"));
		locationCampaignReason.sendKeys(reason);
	}

	public void presenceCampaignWhomPage(String locationGroupName) {
		WebElement effectiveLocation = driver.findElement(locators.get("signupCampaignsPage.effectiveLocation"));
		utils.selectDrpDwnValue(effectiveLocation, locationGroupName);
		logger.info("select location--" + locationGroupName);
		TestListeners.extentTest.get().info("select location--" + locationGroupName);

		clickNextBtn();
	}

	public void activateCampaign() {
		WebElement activateBtn = driver.findElement(locators.get("signupCampaignsPage.activateBtn"));
		utils.waitTillVisibilityOfElement(activateBtn, "activate button");
		activateBtn.click();
		utils.waitTillPagePaceDone();
	}

	public void setLocationApplicability(String name) {
		WebElement locationApplicabilityDrpList_select = driver.findElement(locators.get("signupCampaignsPage.locationApplicabilityDrpList_select"));
		utils.selectDrpDwnValue(locationApplicabilityDrpList_select, name);
		TestListeners.extentTest.get().pass("Location Applicability entered successfully");
	}

	public void setPriority(String val) {
		driver.findElement(locators.get("signupCampaignsPage.PriorityTextBox")).clear();
		WebElement priorityTextBox = driver.findElement(locators.get("signupCampaignsPage.PriorityTextBox"));
		priorityTextBox.sendKeys(val);
		TestListeners.extentTest.get().pass("Priority entered successfully");
	}

	public void setSurvey(String name) {
		WebElement surveyDrpselect = driver.findElement(locators.get("signupCampaignsPage.surveyDrpselect"));
		utils.selectDrpDwnValue(surveyDrpselect, name);
		TestListeners.extentTest.get().pass("Survey entered successfully");

		/*
		 * driver.findElement(locators.get("signupCampaignsPage.surveyDrpselect")).click();
		 * List<WebElement> ele =
		 * driver.findElements(locators.get("signupCampaignsPage.postCheckingiftTypeList"));
		 * utils.selectListDrpDwnValue(ele, giftType);
		 */
	}

	public boolean verifySegmentListVisibility() {
		boolean flag = false;
		WebElement clickOnSegmentDrp = driver.findElement(locators.get("signupCampaignsPage.clickOnSegmentDrp"));
		clickOnSegmentDrp.click();
		WebElement segmentDrpList = driver.findElement(locators.get("signupCampaignsPage.segmentDrpList"));
		flag = segmentDrpList.isDisplayed();
		WebElement clickOnSegmentDrpAgain = driver.findElement(locators.get("signupCampaignsPage.clickOnSegmentDrp"));
		clickOnSegmentDrpAgain.click();
		return flag;
	}

	public void deleteSignUpCampaign(String Campaignname) throws InterruptedException {
		try {
			driver.findElement(locators.get("campaignsPage.searchBox")).clear();
			WebElement searchBox = driver.findElement(locators.get("campaignsPage.searchBox"));
			searchBox.sendKeys(Campaignname);
			searchBox.sendKeys(Keys.ENTER);
			WebElement campaignName = driver.findElement(locators.get("campaignsPage.campaignName"));
			campaignName.click();
			utils.waitTillPagePaceDone();
			utils.getLocator("segmentPage.actionOptionButton").click();
			WebElement optionEditButton = driver.findElement(locators.get("campaignsPage.optionEditButton"));
			utils.clickByJSExecutor(driver, optionEditButton);
			utils.waitTillPagePaceDone();
			WebElement clickOptionListButton = driver.findElement(locators.get("signupCampaignsPage.clickOptionListButton"));
			clickOptionListButton.click();
			WebElement deleteSignUpCam = driver.findElement(locators.get("signupCampaignsPage.deleteSignUpCam"));
			deleteSignUpCam.click();
			driver.switchTo().alert().accept();
			utils.waitTillPagePaceDone();
			logger.info("campaign deleted....");
			TestListeners.extentTest.get().info("campaign deleted....");
		} catch (Exception e) {
			logger.info("Deletion of Campaign is not possible");
			TestListeners.extentTest.get().info("Deletion of Campaign is not possible");
		}
	}

	public List<String> campaignTriggerLst() {
		WebElement campaignTriggerField = driver.findElement(locators.get("signupCampaignsPage.campaignTriggerField"));
		campaignTriggerField.click();
		List<WebElement> campaignTriggerLst = driver.findElements(locators.get("signupCampaignsPage.campaignTriggerLst"));
		List<String> lst = new ArrayList<>();
		for (int i = 0; i < campaignTriggerLst.size(); i++) {
			String text = campaignTriggerLst.get(i).getText();
			lst.add(text);
		}
		return lst;
	}

	public void selectCalendarMonth(String monthCount) {
		selUtils.implicitWait(1);
		List<WebElement> webListMonth = driver.findElements(locators.get("signupCampaignsPage.setStartCurrentMonth"));
		for (WebElement wEle : webListMonth) {
			try {
				utils.selectDrpDwnValueNew(wEle, monthCount + "");
			} catch (Exception e) {
			}
		}
	}

	public void selectCalendarDateWithoutApplyButton(String monthName, String dateToSelect) {
		String selectedMonthName = "";
		selUtils.implicitWait(1);
		List<WebElement> monthsSelectedList = driver.findElements(locators.get("signupCampaignsPage.getSelectedMonthInCalendar"));
		for (WebElement weleName : monthsSelectedList) {
			try {
				selectedMonthName = weleName.getText();
			} catch (Exception exp) {
			}
		}
		if (!selectedMonthName.startsWith(monthName)) {
			List<WebElement> nextButtonList = driver
					.findElements(locators.get("signupCampaignsPage.moveToNextMonthIconWithOutApplyButton"));
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
		String xpathWithoutAppyBtn = utils.getLocatorValue("signupCampaignsPage.calendarDaysWithoutApplyButton")
				.replace("${CurrentDate}", dateToSelect);

		List<WebElement> eleList = driver.findElements(By.xpath(xpathWithoutAppyBtn));
		for (WebElement wEle : eleList) {
			try {
				wEle.click();
				logger.info(dateToSelect + " END Date is selected and apply button not applicable for this calendar ");
				TestListeners.extentTest.get().info(
						dateToSelect + " END Date is selected and apply button not applicable for this calendar ");
				break;
			} catch (Exception exp) {
			}
		}
		selUtils.implicitWait(50);
	}

	public void setEndDateTimeForCouponCampaign(int days, String hours, String minutes, String ampmselect)
			throws ParseException {

		PageObj pageObj = new PageObj(driver);
		String nextdayDate = CreateDateTime.getFutureDateTimeUTC(days);
		LocalDate date1 = LocalDate.parse(nextdayDate);
		date1.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
		String date[] = nextdayDate.split("-");
		String currentDate = date[2];
		String year = date[0];
		int currentMonth = Integer.parseInt(date[1]) - 1;
		if (currentDate.charAt(0) == '0') {
			currentDate = Character.toString(currentDate.charAt(1));
		}
		// Open Calendar
		WebElement endDate = driver.findElement(locators.get("signupCampaignsPage.endDate"));
		endDate.click();

		// Wait for Apply button to be visible
		WebElement dateApplyButton = driver.findElement(locators.get("signupCampaignsPage.dateApplyButton"));
		utils.waitTillElementToBeClickable(dateApplyButton);

		// Select month
		selectCalendarMonth(currentMonth + "");

		// Select year
		selectHourMin(year, "signupCampaignsPage.setYearEndDate");

		// Select date
		String xpath = utils.getLocatorValue("signupCampaignsPage.calendarDaysWithAppyButton").replace("${CurrentDate}",
				currentDate);
		List<WebElement> eleList = driver.findElements(By.xpath(xpath));
		for (WebElement wEle : eleList) {
			String colorCode = pageObj.mobileconfigurationPage().getTextColour(wEle);
			if (colorCode.equalsIgnoreCase("#33343d")) {
				try {
					selUtils.implicitWait(1);
					wEle.click();
					break;
				} catch (Exception e) {

				}
			}

		}

		// Select hour between 1-12
		selectHourMin(hours, "signupCampaignsPage.hourSelectEndDate");

		// Select minutes like 0,15,30,45
		selectHourMin(minutes, "signupCampaignsPage.minuteSelectEndDate");

		// Select AM/PM
		selectHourMin(ampmselect, "signupCampaignsPage.ampmSelectEndDate");

		// Click on Apply button
		selUtils.implicitWait(10);
		WebElement dateApplyButtonForClick = driver.findElement(locators.get("signupCampaignsPage.dateApplyButton"));
		dateApplyButtonForClick.click();

		logger.info(currentDate + " END Date is selected and clicked on applied button");
		TestListeners.extentTest.get().info(currentDate + " END Date is selected and clicked on applied button");

	}

	public void selectHourMin(String hrmin, String locator) {
		List<WebElement> webListMonth = driver.findElements(locators.get(locator));
		for (WebElement wEle : webListMonth) {
			try {
				utils.selectDrpDwnValueNew(wEle, hrmin);
			} catch (Exception e) {
			}
		}
	}

	public void clickFinishButton() {
		WebElement finishButton = driver.findElement(locators.get("campaignsPage.finishButton"));
		finishButton.click();
		utils.waitTillPagePaceDone();
		logger.info(" clicked on finish button ");
		TestListeners.extentTest.get().pass(" clicked on finish button");
	}

	public String getRedeemableAttachedToCampaign() {
		utils.waitTillPagePaceDone();
		String str = null;
		try {
			WebElement getRedeemableName = driver.findElement(locators.get("signupCampaignsPage.getRedeemableName"));
			getRedeemableName.isDisplayed();
			str = getRedeemableName.getText();
			logger.info(" redeemable attached: " + str);
			TestListeners.extentTest.get().info(" redeemable attached: " + str);
		} catch (Exception e) {
			logger.info("No redeemable available");
			TestListeners.extentTest.get().info("No redeemable available");
		}
		return str;
	}

	public String getMessageUnderBox(String boxTitle) {
		utils.waitTillPagePaceDone();
		String xpath = utils.getLocatorValue("signupCampaignsPage.messageUnderBox").replace("{$title}", boxTitle);
		WebElement messageUnderBox = driver.findElement(By.xpath(xpath));
		String str = messageUnderBox.getText();
		logger.info(" Text under box: " + boxTitle + " is: " + str);
		TestListeners.extentTest.get().info(" Text under box: " + boxTitle + " is: " + str);
		return str;
	}

	public void EnableAvailableAsTemplateToggle() {
		WebElement availableAsTemplateToggle = driver.findElement(locators.get("campaignsPage.availableAsTemplateToggle"));
		availableAsTemplateToggle.click();
		utils.waitTillPagePaceDone();
		logger.info("clicked on Available As A Template Toggle");
		TestListeners.extentTest.get().pass("clicked on Available As A Template Toggle");
	}

	public void setMetadataInCouponCamp(String metadataInfo, String languageName) {
		String languageLinkXpath = utils.getLocatorValue("signupCampaignsPage.metadataLanguageLink")
				.replace("$languageName", languageName);

		By languageLinkBy = By.xpath(languageLinkXpath);
		WebElement languageLink = driver.findElement(languageLinkBy);
		languageLink.click();

		String languageLinkInputBoxXpath = utils.getLocatorValue("signupCampaignsPage.metadataInputBoxForLanguage")
				.replace("$languageName", languageName);

		WebElement inputBoxWEle = driver.findElement(By.xpath(languageLinkInputBoxXpath));
		inputBoxWEle.click();
		inputBoxWEle.clear();
		inputBoxWEle.sendKeys(metadataInfo);
		logger.info(metadataInfo + " metadata info is set for the language " + languageName);
		TestListeners.extentTest.get().info(metadataInfo + " metadata info is set for the language " + languageName);

	}

	public void editCampaignFromNewCHP() {
		WebElement nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		nextBtn.click();
		utils.waitTillPagePaceDone();
		WebElement nextBtn2 = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		nextBtn2.click();
		utils.waitTillPagePaceDone();
		WebElement approveAndSchedule = driver.findElement(locators.get("signupCampaignsPage.approveAndSchedule"));
		approveAndSchedule.click();
		utils.acceptAlert(driver);
		logger.info("Clicked approve and schedule button.");
		TestListeners.extentTest.get().info("Clicked approve and schedule button.");
		utils.waitTillPagePaceDone();
	}

	public void setGuestEmailwithVariablepoints(String email, String points) {
		driver.findElement(locators.get("signupCampaignsPage.listOfGuestsTextarea")).clear();
		WebElement listOfGuestsTextarea = driver.findElement(locators.get("signupCampaignsPage.listOfGuestsTextarea"));
		listOfGuestsTextarea.sendKeys(email + ":" + points);
		TestListeners.extentTest.get().info("Entered user email with points :" + email + ":" + points);
	}

	public void challengeAvailabilityScheduleStartTime(String time) {
		utils.getLocator("schedulesPage.clickStartTime").isDisplayed();
		utils.getLocator("schedulesPage.clickStartTime").click();
		WebElement scheduleStartTimeBox = driver.findElement(locators.get("signupCampaignsPage.scheduleStartTimeBox"));
		scheduleStartTimeBox.sendKeys(time);
		scheduleStartTimeBox.sendKeys(Keys.ENTER);
		logger.info("Entered Challenge Availability Schedule start time");
		TestListeners.extentTest.get().info("Entered Challenge Availability Schedule start time");
	}

	public void setPromoCampaignNoOfGuests(String noOfGuests) {
		driver.findElement(locators.get("signupCampaignsPage.numberOfGuests")).clear();
		WebElement numberOfGuests = driver.findElement(locators.get("signupCampaignsPage.numberOfGuests"));
		numberOfGuests.sendKeys(noOfGuests);
		logger.info("Number of Guests : " + noOfGuests);
		TestListeners.extentTest.get().info("Number of Guests : " + noOfGuests);
	}

	public void setPromoCampaignUsagePerGuest(String usagePerGuest) {
		driver.findElement(locators.get("signupCampaignsPage.usagePerGuest")).clear();
		WebElement usagePerGuestElement = driver.findElement(locators.get("signupCampaignsPage.usagePerGuest"));
		usagePerGuestElement.sendKeys(usagePerGuest);
		logger.info("Use(s) Per Guest : " + usagePerGuest);
		TestListeners.extentTest.get().info("Use(s) Per Guest : " + usagePerGuest);
	}

	public void promoGiftType(String giftType, String gift) {
		WebElement couponGiftTypeDrp = driver.findElement(locators.get("signupCampaignsPage.couponGiftTypeDrp"));
		couponGiftTypeDrp.click();
		List<WebElement> couponGiftTypeDrpList = driver.findElements(locators.get("signupCampaignsPage.couponGiftTypeDrpList"));
		utils.selectListDrpDwnValue(couponGiftTypeDrpList, giftType);
		logger.info("Gift type in Promo is " + giftType);
		TestListeners.extentTest.get().info("Gift type in Promo is " + giftType);
		switch (giftType) {
		case "Gift Points":
			driver.findElement(locators.get("signupCampaignsPage.amountDiscount")).clear();
			WebElement amountDiscount = driver.findElement(locators.get("signupCampaignsPage.amountDiscount"));
			amountDiscount.sendKeys(gift);
			logger.info("Gifting points in Promo is " + gift);
			TestListeners.extentTest.get().info("Gifting points in Promo is " + gift);
			break;

		case "Gift Redeemable":
			WebElement promoRedemableDrpDown = driver.findElement(locators.get("signupCampaignsPage.promoRedemableDrpDown"));
			promoRedemableDrpDown.click();
			List<WebElement> promoRedemableDrpDownList = driver.findElements(locators.get("signupCampaignsPage.promoRedemableDrpDownList"));
			utils.selectListDrpDwnValue(promoRedemableDrpDownList, gift);
			logger.info("Redeemable for gifting in Promo is " + gift);
			TestListeners.extentTest.get().info("Redeemable for gifting in Promo is " + gift);
			break;

		case "$ OFF":
			WebElement amountDiscountForScroll = driver.findElement(locators.get("signupCampaignsPage.amountDiscount"));
			utils.scrollToElement(driver, amountDiscountForScroll);
			driver.findElement(locators.get("signupCampaignsPage.amountDiscount")).clear();
			WebElement amountDiscountForSendKeys = driver.findElement(locators.get("signupCampaignsPage.amountDiscount"));
			amountDiscountForSendKeys.sendKeys(gift);
			logger.info("$ OFF for gifting in Promo is " + gift);
			TestListeners.extentTest.get().info("$ OFF for gifting in Promo is " + gift);
			break;

		case "% OFF":
			WebElement percentageDiscountInputForScroll = driver.findElement(locators.get("signupCampaignsPage.percentageDiscountInput"));
			utils.scrollToElement(driver, percentageDiscountInputForScroll);
			driver.findElement(locators.get("signupCampaignsPage.percentageDiscountInput")).clear();
			WebElement percentageDiscountInput = driver.findElement(locators.get("signupCampaignsPage.percentageDiscountInput"));
			percentageDiscountInput.sendKeys(gift);
			logger.info("% OFF for gifting in Promo is " + gift);
			TestListeners.extentTest.get().info("% OFF for gifting in Promo is " + gift);
			break;
		}
	}

	public void setCampaignGiftreason(String giftreason) {
		driver.findElement(locators.get("signupCampaignsPage.giftReasonForCoupon")).clear();
		WebElement giftReasonForCoupon = driver.findElement(locators.get("signupCampaignsPage.giftReasonForCoupon"));
		giftReasonForCoupon.sendKeys(giftreason);
		logger.info("Gift Reason is : " + giftreason);
		TestListeners.extentTest.get().info("Gift Reason is : " + giftreason);
	}

	public void setCampaignPushNotification(String pushNotification) {
		driver.findElement(locators.get("signupCampaignsPage.promoPN")).clear();
		WebElement promoPN = driver.findElement(locators.get("signupCampaignsPage.promoPN"));
		promoPN.sendKeys(pushNotification);
		logger.info("Push Notification is : " + pushNotification);
		TestListeners.extentTest.get().info("Push Notification is : " + pushNotification);
	}

	public void selectEmailTemplate(String emailTemplateName) {
		WebElement emailEditor = driver.findElement(locators.get("campaignsBetaPage.emailEditor"));
		utils.scrollToElement(driver, emailEditor);
		pageObj.dashboardpage().checkUncheckToggle("Enable Email Editor", "ON");
		String xpath = utils.getLocatorValue("signupCampaignsPage.selectEmailTemplate").replace("$emailTemplateName",
				emailTemplateName);
		By selectEmailTemplateBy = By.xpath(xpath);
		WebElement selectEmailTemplate = driver.findElement(selectEmailTemplateBy);
		utils.mouseHover(driver, selectEmailTemplate);
		selectEmailTemplate.click();
		WebElement saveEmailTemplate = driver.findElement(locators.get("signupCampaignsPage.saveEmailTemplate"));
		utils.waitTillElementToBeClickable(saveEmailTemplate);
		utils.longWaitInSeconds(10);
		utils.scrollToElement(driver, saveEmailTemplate);
		saveEmailTemplate.click();
		utils.longWaitInSeconds(10);
//		utils.waitTillSpinnerDisappear();
		WebElement enterEmailSubject = driver.findElement(locators.get("signupCampaignsPage.enterEmailSubject"));
		utils.waitTillElementToBeClickable(enterEmailSubject);
		logger.info(emailTemplateName + " Email Template is selected");
		TestListeners.extentTest.get().info(emailTemplateName + " Email Template is selected");
	}

	public void setCampaignEmailSubject(String emailSubject) {
		driver.findElement(locators.get("signupCampaignsPage.enterEmailSubject")).clear();
		WebElement enterEmailSubject = driver.findElement(locators.get("signupCampaignsPage.enterEmailSubject"));
		enterEmailSubject.sendKeys(emailSubject);
		logger.info("Email Subject is : " + emailSubject);
		TestListeners.extentTest.get().info("Email Subject is : " + emailSubject);
	}

	public void setPrompt(String promptType, String prompt) {
		switch (promptType) {
		case "Short Prompt":
			WebElement shortPrompt = driver.findElement(locators.get("signupCampaignsPage.shortPrompt"));
			shortPrompt.click();
			driver.findElement(locators.get("signupCampaignsPage.shortPrompt")).clear();
			WebElement shortPromptForSendKeys = driver.findElement(locators.get("signupCampaignsPage.shortPrompt"));
			shortPromptForSendKeys.sendKeys(prompt);
			logger.info("Short Prompt is : " + prompt);
			TestListeners.extentTest.get().info("Short Prompt is : " + prompt);
			break;

		case "Standard Prompt":
			WebElement standardPrompt = driver.findElement(locators.get("signupCampaignsPage.standardPrompt"));
			standardPrompt.click();
			driver.findElement(locators.get("signupCampaignsPage.standardPrompt")).clear();
			WebElement standardPromptForSendKeys = driver.findElement(locators.get("signupCampaignsPage.standardPrompt"));
			standardPromptForSendKeys.sendKeys(prompt);
			logger.info("Standard Prompt is : " + prompt);
			TestListeners.extentTest.get().info("Standard Prompt is : " + prompt);
			break;
		}
	}

	public void promoQC(String qcName) {
		WebElement qcNameDrpDown = driver.findElement(locators.get("signupCampaignsPage.qcNameDrpDown"));
		qcNameDrpDown.click();
		List<WebElement> qcNameList = driver.findElements(locators.get("signupCampaignsPage.qcNameList"));
		utils.selectListDrpDwnValue(qcNameList, qcName);
		logger.info("Gift type in Promo is " + qcName);
		TestListeners.extentTest.get().info("Gift type in Promo is " + qcName);
	}

	public void setRichMessage(String richMsg) {
		utils.longWaitInSeconds(1);
		WebElement configureRichMsgToggle = driver.findElement(locators.get("signupCampaignsPage.configureRichMsgToggle"));
		configureRichMsgToggle.click();
		driver.findElement(locators.get("signupCampaignsPage.richMsgName")).clear();
		WebElement richMsgName = driver.findElement(locators.get("signupCampaignsPage.richMsgName"));
		richMsgName.sendKeys(richMsg);
		driver.findElement(locators.get("signupCampaignsPage.richMsgTitle")).clear();
		WebElement richMsgTitle = driver.findElement(locators.get("signupCampaignsPage.richMsgTitle"));
		richMsgTitle.sendKeys(richMsg);
		driver.findElement(locators.get("signupCampaignsPage.richMsgBody")).clear();
		WebElement richMsgBody = driver.findElement(locators.get("signupCampaignsPage.richMsgBody"));
		richMsgBody.sendKeys(richMsg);
	}

	/*
	 * public void splitRichMessage(String richMsg) { utils.longWaitInSeconds(1);
	 * driver.findElement(locators.get("signupCampaignsPage.richTypeButton")).click();
	 * driver.findElement(locators.get("signupCampaignsPage.richTypeButton")).sendKeys("Image");
	 * driver.findElement(locators.get("signupCampaignsPage.richmessagetitle")).clear();
	 * driver.findElement(locators.get("signupCampaignsPage.richmessagetitle")).sendKeys(richMsg);
	 * driver.findElement(locators.get("signupCampaignsPage.richmessagebody")).clear();
	 * driver.findElement(locators.get("signupCampaignsPage.richmessagebody")).sendKeys(richMsg); }
	 */
	public void addLocationInCampaign(String locationName) {
		WebElement locationDropdownSelect = driver.findElement(locators.get("signupCampaignsPage.locationDropDownXpath"));
		utils.selectDrpDwnValue(locationDropdownSelect, locationName);
		logger.info(locationName + " location is selected in dropdown ");
		TestListeners.extentTest.get().info(locationName + " location is selected in dropdown ");
	}

	public void setChallengeCampaignName(String name) {
		driver.findElement(locators.get("signupCampaignsPage.challengeCampaignName")).clear();
		WebElement challengeCampaignName = driver.findElement(locators.get("signupCampaignsPage.challengeCampaignName"));
		challengeCampaignName.sendKeys(name);
		logger.info("Entered mass challenge campaign name as :" + name);
		TestListeners.extentTest.get().info("Entered mass challenge campaign name as :" + name);

	}

	public void selectCalendarYear(String year) {
		selUtils.implicitWait(1);
		List<WebElement> webListMonth = driver.findElements(locators.get("signupCampaignsPage.setStartCurrentYearInCalendar"));
		for (WebElement wEle : webListMonth) {
			try {
				utils.selectDrpDwnValueNew(wEle, year);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}

	public void setGuestFrequencyOfGiting(String val) {
		WebElement guestFrequencyDrpDwn = driver.findElement(locators.get("signupCampaignsPage.guestFrequencyDrpDwn"));
		guestFrequencyDrpDwn.click();
		List<WebElement> guestFrequencyDrpDwnList = driver.findElements(locators.get("signupCampaignsPage.guestFrequencyDrpDwnList"));
		utils.selectListDrpDwnValue(guestFrequencyDrpDwnList, val);
		logger.info("Selected guest frequency type successfully");
		TestListeners.extentTest.get().info("Selected guest frequency type successfully");
	}

	public void checkUncheckFlag(String flagName, String toBeOnOff) {
		String xpath = utils.getLocatorValue("signupCampaignsPage.regiftingFlag").replace("$flagName", flagName);
		WebElement regiftingFlag = driver.findElement(By.xpath(xpath));
		String checkBoxValue = regiftingFlag.getAttribute("checked");

		if ((checkBoxValue == null) && (toBeOnOff.equalsIgnoreCase("OFF"))) {
			logger.info("Auto checking box Unchecked and do not cliked it as checkBoxFlag= " + toBeOnOff);
			TestListeners.extentTest.get()
					.pass("Auto checking box Unchecked and do not cliked it as checkBoxFlag= " + toBeOnOff);
		} else if ((checkBoxValue == null) && (toBeOnOff.equalsIgnoreCase("ON"))) {
			utils.clickByJSExecutor(driver, regiftingFlag);
			logger.info("Autocheckin box is unchecked and user want to check the chekedbox");
			TestListeners.extentTest.get().pass("Autocheckin box is unchecked and user want to check the chekedbox");

		} else if (checkBoxValue.equalsIgnoreCase("true") && (toBeOnOff.equalsIgnoreCase("OFF"))) {
			utils.clickByJSExecutor(driver, regiftingFlag);

			logger.info("Autocheckin box is already cheked and user want to uncheck ");
			TestListeners.extentTest.get().pass("Autocheckin box is already cheked and user want to uncheck ");
		} else if (checkBoxValue.equalsIgnoreCase("true") && (toBeOnOff.equalsIgnoreCase("ON"))) {

			logger.info("Autocheckin box is already checked and user want to check the chekedbox, so do not click");
			TestListeners.extentTest.get()
					.pass("Autocheckin box is already checked and user want to check the chekedbox, so do not click");
		}
		selUtils.longWait(5);
	}

	public void enterTimesPerDay(String value) {
		utils.longWaitInSeconds(1);
		WebElement timesPerDay = driver.findElement(locators.get("signupCampaignsPage.timesPerDay"));
		timesPerDay.click();
		driver.findElement(locators.get("signupCampaignsPage.timesPerDay")).clear();
		WebElement timesPerDayForSendKeys = driver.findElement(locators.get("signupCampaignsPage.timesPerDay"));
		timesPerDayForSendKeys.sendKeys(value);
		logger.info("Entered guest freuency times per day as : " + value);
		TestListeners.extentTest.get().info("Entered guest freuency times per day as : " + value);
	}

	public void setFiniteGifting(String value) {
		utils.longWaitInSeconds(1);
		WebElement finiteGifting = driver.findElement(locators.get("signupCampaignsPage.finiteGifting"));
		finiteGifting.click();
		finiteGifting.clear();
		finiteGifting.sendKeys(value);
		logger.info("Entered guest freuency times per day as : " + value);
		TestListeners.extentTest.get().info("Entered guest freuency times per day as : " + value);
	}

	// sha
	public void setPromoCampaignGuestWithUnlockRewardsMobileAppOption(String noOfGuests, String giftType,
			String amount) {
		driver.findElement(locators.get("signupCampaignsPage.numberOfGuests")).clear();
		WebElement numberOfGuests = driver.findElement(locators.get("signupCampaignsPage.numberOfGuests"));
		numberOfGuests.sendKeys(noOfGuests);

		WebElement giftTypeDropdownXpath = driver.findElement(locators.get("signupCampaignsPage.giftTypeDropdownXpath"));
		Select giftTypeDropdown = new Select(giftTypeDropdownXpath);
		giftTypeDropdown.selectByVisibleText(giftType);

		driver.findElement(locators.get("signupCampaignsPage.giftpointForCoupon")).clear();
		WebElement giftpointForCoupon = driver.findElement(locators.get("signupCampaignsPage.giftpointForCoupon"));
		giftpointForCoupon.sendKeys(amount);

		driver.findElement(locators.get("signupCampaignsPage.giftReasonForCoupon")).clear();
		WebElement giftReasonForCoupon = driver.findElement(locators.get("signupCampaignsPage.giftReasonForCoupon"));
		giftReasonForCoupon.sendKeys("Testing");

		WebElement nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		nextBtn.click();
		logger.info("Clicked next button");
		TestListeners.extentTest.get().pass("New coupon campaign whom details entered successfully");
	}

	public void createWhatDetailsChallengeCampaignInLocalisedlanguage(String campName1, String campName2,
			String campDes1, String campDes2, String data1, String data2, String reason1, String reason2) {
		WebElement campaNameLocaliselanguageInput = driver.findElement(locators.get("signupCampaignsPage.campaNameLocaliselanguageInput"));
		campaNameLocaliselanguageInput.sendKeys(campName1);
		utils.getLocator("subscriptionPlansPage.frSelection1").click();
		WebElement campaNameLocaliselanguageInput2 = driver.findElement(locators.get("signupCampaignsPage.campaNameLocaliselanguageInput"));
		campaNameLocaliselanguageInput2.sendKeys(campName2);
		WebElement descriptionLocaliseLanguageInput = driver.findElement(locators.get("signupCampaignsPage.descriptionLocaliseLanguageInput"));
		descriptionLocaliseLanguageInput.sendKeys(campDes1);
		utils.getLocator("subscriptionPlansPage.frSelection2").click();
		WebElement descriptionLocaliseLanguageInput2 = driver.findElement(locators.get("signupCampaignsPage.descriptionLocaliseLanguageInput"));
		descriptionLocaliseLanguageInput2.sendKeys(campDes2);
		WebElement miscellaneousLocaliseLanguageInput = driver.findElement(locators.get("signupCampaignsPage.miscellaneousLocaliseLanguageInput"));
		miscellaneousLocaliseLanguageInput.sendKeys(data1);
		utils.getLocator("subscriptionPlansPage.frSelection3").click();
		WebElement miscellaneousLocaliseLanguageInput2 = driver.findElement(locators.get("signupCampaignsPage.miscellaneousLocaliseLanguageInput"));
		miscellaneousLocaliseLanguageInput2.sendKeys(data2);
		WebElement giftReasonLocaliseLanguageInput = driver.findElement(locators.get("signupCampaignsPage.giftReasonLocaliseLanguageInput"));
		giftReasonLocaliseLanguageInput.sendKeys(reason1);
		WebElement frSelectionGiftreason = driver.findElement(locators.get("signupCampaignsPage.frSelectionGiftreason"));
		frSelectionGiftreason.click();
		WebElement giftReasonLocaliseLanguageInput2 = driver.findElement(locators.get("signupCampaignsPage.giftReasonLocaliseLanguageInput"));
		giftReasonLocaliseLanguageInput2.sendKeys(reason2);

		logger.info(
				"Entered Challenge Campaign Name , Description , Miscellaneous Data and Gift Reason in localised language");
		TestListeners.extentTest.get().info(
				"Entered Challenge Campaign Name , Description , Miscellaneous Data and Gift Reason in localised language");
	}

	public String extractDownloadCouponUrl() {
		String urlXpath = utils.getLocatorValue("signupCampaignsPage.downloadCouponUrl");
		WebElement downloadCouponUrl = driver.findElement(By.xpath(urlXpath));
		String url = downloadCouponUrl.getAttribute("value");
		logger.info("Download coupon URL is : " + url);
		TestListeners.extentTest.get().info("Download coupon URL is : " + url);
		return url;
	}

	public String getTodayDateInTimezone(String timezone) {
		ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of(timezone));
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String formattedDate = zonedDateTime.format(formatter);
		return formattedDate;
	}

	public String getTodayDateInUTC() {
		ZonedDateTime utcNow = ZonedDateTime.now(ZoneOffset.UTC);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String formattedDate = utcNow.format(formatter);
		return formattedDate;
	}

	public int compareDates(String date1, String date2) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date d1 = sdf.parse(date1);
		Date d2 = sdf.parse(date2);
		return d1.compareTo(d2);
	}

	public String getRedeemableFieldValue() {
		// Adjust the locator key as per your actual field
		WebElement redeemableField = utils.getLocator("campaignSplitPage.redeemableDrpDown");
		// For a dropdown, get the selected option's text
		return redeemableField.getText();
	}

	public String campaignScheduleErrorMessage() {
		utils.waitTillPagePaceDone();
		String errorMessage = utils.getLocator("campaignSplitPage.campaignScheduleErrorMessage").getText();
		logger.info("Campaign Schedule Error Message: " + errorMessage);
		TestListeners.extentTest.get().info("Campaign Schedule Error Message: " + errorMessage);
		return errorMessage;
	}

	public String getTimezoneGreaterOrLessThanUTC() {
		String[] timezones = { "(GMT-12:00) International Date Line West ( -12 ) ,Etc/GMT+12",
				"(GMT-11:00) American Samoa ( SST ) ,Pacific/Pago_Pago",
				"(GMT-11:00) Midway Island ( SST ) ,Pacific/Midway", "(GMT-10:00) Hawaii ( HST ) ,Pacific/Honolulu",
				"(GMT-08:00) Alaska ( AKDT ) ,America/Juneau",
				"(GMT-07:00) Pacific Time (US & Canada) ( PDT ) ,America/Los_Angeles",
				"(GMT-07:00) Tijuana ( PDT ) ,America/Tijuana", "(GMT-07:00) Arizona ( MST ) ,America/Phoenix",
				"(GMT-06:00) Chihuahua ( MDT ) ,America/Chihuahua", "(GMT-06:00) Mazatlan ( MDT ) ,America/Mazatlan",
				"(GMT-06:00) Mountain Time (US & Canada) ( MDT ) ,America/Denver",
				"(GMT-06:00) Central America ( CST ) ,America/Guatemala",
				"(GMT-05:00) Central Time (US & Canada) ( CDT ) ,America/Chicago",
				"(GMT-05:00) Guadalajara ( CDT ) ,America/Mexico_City",
				"(GMT-05:00) Mexico City ( CDT ) ,America/Mexico_City",
				"(GMT-05:00) Monterrey ( CDT ) ,America/Monterrey", "(GMT-06:00) Saskatchewan ( CST ) ,America/Regina",
				"(GMT-05:00) Bogota ( -05 ) ,America/Bogota", "(GMT-05:00) Lima ( -05 ) ,America/Lima",
				"(GMT-05:00) Quito ( -05 ) ,America/Lima",
				"(GMT-04:00) Eastern Time (US & Canada) ( EDT ) ,America/New_York",
				"(GMT-04:00) Indiana (East) ( EDT ) ,America/Indiana/Indianapolis",
				"(GMT-04:00) Caracas ( -04 ) ,America/Caracas", "(GMT-04:00) Georgetown ( -04 ) ,America/Guyana",
				"(GMT-04:00) La Paz ( -04 ) ,America/La_Paz", "(GMT-04:00) Puerto Rico ( AST ) ,America/Puerto_Rico",
				"(GMT-04:00) Santiago ( -04 ) ,America/Santiago", "(GMT-02:30) Newfoundland ( NDT ) ,America/St_Johns",
				"(GMT-03:00) Brasilia ( -03 ) ,America/Sao_Paulo",
				"(GMT-03:00) Atlantic Time (Canada) ( ADT ) ,America/Halifax",
				"(GMT-03:00) Buenos Aires ( -03 ) ,America/Argentina/Buenos_Aires",
				"(GMT-02:00) Greenland ( -02 ) ,America/Godthab", "(GMT-03:00) Montevideo ( -03 ) ,America/Montevideo",
				"(GMT-02:00) Mid-Atlantic ( -02 ) ,Atlantic/South_Georgia",
				"(GMT-01:00) Cape Verde Is. ( -01 ) ,Atlantic/Cape_Verde", "(GMT+00:00) UTC ( UTC ) ,Etc/UTC",
				"(GMT+02:00) Amsterdam ( CEST ) ,Europe/Amsterdam", "(GMT+02:00) Belgrade ( CEST ) ,Europe/Belgrade",
				"(GMT+02:00) Berlin ( CEST ) ,Europe/Berlin", "(GMT+02:00) Bern ( CEST ) ,Europe/Zurich",
				"(GMT+02:00) Bratislava ( CEST ) ,Europe/Bratislava", "(GMT+02:00) Brussels ( CEST ) ,Europe/Brussels",
				"(GMT+02:00) Budapest ( CEST ) ,Europe/Budapest", "(GMT+01:00) Casablanca ( +01 ) ,Africa/Casablanca",
				"(GMT+02:00) Copenhagen ( CEST ) ,Europe/Copenhagen", "(GMT+01:00) Dublin ( IST ) ,Europe/Dublin",
				"(GMT+02:00) Ljubljana ( CEST ) ,Europe/Ljubljana", "(GMT+02:00) Madrid ( CEST ) ,Europe/Madrid",
				"(GMT+02:00) Paris ( CEST ) ,Europe/Paris", "(GMT+02:00) Prague ( CEST ) ,Europe/Prague",
				"(GMT+02:00) Rome ( CEST ) ,Europe/Rome", "(GMT+02:00) Sarajevo ( CEST ) ,Europe/Sarajevo",
				"(GMT+02:00) Skopje ( CEST ) ,Europe/Skopje", "(GMT+02:00) Stockholm ( CEST ) ,Europe/Stockholm",
				"(GMT+02:00) Vienna ( CEST ) ,Europe/Vienna", "(GMT+02:00) Warsaw ( CEST ) ,Europe/Warsaw",
				"(GMT+01:00) West Central Africa ( CET ) ,Africa/Algiers", "(GMT+02:00) Zagreb ( CEST ) ,Europe/Zagreb",
				"(GMT+02:00) Zurich ( CEST ) ,Europe/Zurich", "(GMT+03:00) Athens ( EEST ), Europe/Athens",
				"(GMT+03:00) Bucharest ( EEST ), Europe/Bucharest", "(GMT+02:00) Cairo ( EET ), Africa/Cairo",
				"(GMT+02:00) Harare ( CAT ), Africa/Harare", "(GMT+03:00) Helsinki ( EEST ), Europe/Helsinki",
				"(GMT+03:00) Jerusalem ( IDT ), Asia/Jerusalem", "(GMT+02:00) Kaliningrad ( EET ), Europe/Kaliningrad",
				"(GMT+03:00) Kyiv ( EEST ), Europe/Kiev", "(GMT+02:00) Pretoria ( SAST ), Africa/Johannesburg",
				"(GMT+03:00) Riga ( EEST ), Europe/Riga", "(GMT+03:00) Sofia ( EEST ), Europe/Sofia",
				"(GMT+03:00) Tallinn ( EEST ), Europe/Tallinn", "(GMT+03:00) Vilnius ( EEST ), Europe/Vilnius",
				"(GMT+03:00) Baghdad ( +03 ), Asia/Baghdad", "(GMT+03:00) Istanbul ( +03 ), Europe/Istanbul",
				"(GMT+03:00) Kuwait ( +03 ), Asia/Kuwait", "(GMT+03:00) Minsk ( +03 ), Europe/Minsk",
				"(GMT+03:00) Moscow ( MSK ), Europe/Moscow", "(GMT+03:00) Nairobi ( EAT ), Africa/Nairobi",
				"(GMT+03:00) Riyadh ( +03 ), Asia/Riyadh", "(GMT+03:00) St. Petersburg ( MSK ), Europe/Moscow",
				"(GMT+03:00) Volgograd ( +03 ), Europe/Volgograd", "(GMT+04:30) Tehran ( +0430 ), Asia/Tehran",
				"(GMT+04:00) Abu Dhabi ( +04 ), Asia/Muscat", "(GMT+04:00) Baku ( +04 ), Asia/Baku",
				"(GMT+04:00) Muscat ( +04 ), Asia/Muscat", "(GMT+04:00) Samara ( +04 ), Europe/Samara",
				"(GMT+04:00) Tbilisi ( +04 ), Asia/Tbilisi", "(GMT+04:00) Yerevan ( +04 ), Asia/Yerevan",
				"(GMT+04:30) Kabul ( +0430 ), Asia/Kabul", "(GMT+05:00) Ekaterinburg ( +05 ), Asia/Yekaterinburg",
				"(GMT+05:00) Islamabad ( PKT ), Asia/Karachi", "(GMT+05:00) Karachi ( PKT ), Asia/Karachi",
				"(GMT+05:00) Tashkent ( +05 ), Asia/Tashkent", "(GMT+05:30) Chennai ( IST ), Asia/Kolkata",
				"(GMT+05:30) Kolkata ( IST ), Asia/Kolkata", "(GMT+05:30) Mumbai ( IST ), Asia/Kolkata",
				"(GMT+05:30) New Delhi ( IST ), Asia/Kolkata",
				"(GMT+05:30) Sri Jayawardenepura ( +0530 ), Asia/Colombo",
				"(GMT+05:45) Kathmandu ( +0545 ), Asia/Kathmandu", "(GMT+06:00) Almaty ( +06 ), Asia/Almaty",
				"(GMT+06:00) Astana ( +06 ), Asia/Dhaka", "(GMT+06:00) Dhaka ( +06 ), Asia/Dhaka",
				"(GMT+06:00) Urumqi ( +06 ), Asia/Urumqi", "(GMT+06:30) Rangoon ( +0630 ), Asia/Rangoon",
				"(GMT+07:00) Bangkok ( +07 ), Asia/Bangkok", "(GMT+07:00) Hanoi ( +07 ), Asia/Bangkok",
				"(GMT+07:00) Jakarta ( WIB ), Asia/Jakarta", "(GMT+07:00) Krasnoyarsk ( +07 ), Asia/Krasnoyarsk",
				"(GMT+07:00) Novosibirsk ( +07 ), Asia/Novosibirsk", "(GMT+08:00) Beijing ( CST ), Asia/Shanghai",
				"(GMT+08:00) Chongqing ( CST ), Asia/Chongqing", "(GMT+08:00) Hong Kong ( HKT ), Asia/Hong_Kong",
				"(GMT+08:00) Irkutsk ( +08 ), Asia/Irkutsk", "(GMT+08:00) Kuala Lumpur ( +08 ), Asia/Kuala_Lumpur",
				"(GMT+08:00) Perth ( AWST ), Australia/Perth", "(GMT+08:00) Singapore ( +08 ), Asia/Singapore",
				"(GMT+08:00) Taipei ( CST ), Asia/Taipei", "(GMT+08:00) Ulaanbaatar ( +08 ), Asia/Ulaanbaatar",
				"(GMT+09:00) Osaka ( JST ), Asia/Tokyo", "(GMT+09:00) Sapporo ( JST ), Asia/Tokyo",
				"(GMT+09:00) Seoul ( KST ), Asia/Seoul", "(GMT+09:00) Tokyo ( JST ), Asia/Tokyo",
				"(GMT+09:00) Yakutsk ( +09 ), Asia/Yakutsk", "(GMT+09:30) Adelaide ( ACST ), Australia/Adelaide",
				"(GMT+09:30) Darwin ( ACST ), Australia/Darwin", "(GMT+10:00) Brisbane ( AEST ), Australia/Brisbane",
				"(GMT+10:00) Canberra ( AEST ), Australia/Canberra", "(GMT+10:00) Guam ( ChST ), Pacific/Guam",
				"(GMT+10:00) Hobart ( AEST ), Australia/Hobart", "(GMT+10:00) Melbourne ( AEST ), Australia/Melbourne",
				"(GMT+10:00) Port Moresby ( +10 ), Pacific/Port_Moresby",
				"(GMT+10:00) Sydney ( AEST ), Australia/Sydney", "(GMT+10:00) Vladivostok ( +10 ), Asia/Vladivostok",
				"(GMT+11:00) Magadan ( +11 ), Asia/Magadan", "(GMT+11:00) New Caledonia ( +11 ), Pacific/Noumea",
				"(GMT+11:00) Solomon Is. ( +11 ), Pacific/Guadalcanal",
				"(GMT+11:00) Srednekolymsk ( +11 ), Asia/Srednekolymsk",
				"(GMT+12:00) Auckland ( NZST ), Pacific/Auckland", "(GMT+12:00) Fiji ( +12 ), Pacific/Fiji",
				"(GMT+12:00) Kamchatka ( +12 ), Asia/Kamchatka", "(GMT+12:00) Marshall Is. ( +12 ), Pacific/Majuro",
				"(GMT+12:00) Wellington ( NZST ), Pacific/Auckland",
				"(GMT+12:45) Chatham Is. ( +1245 ), Pacific/Chatham",
				"(GMT+13:00) Nuku'alofa ( +13 ), Pacific/Tongatapu", "(GMT+13:00) Samoa ( +13 ), Pacific/Apia",
				"(GMT+13:00) Tokelau Is. ( +13 ), Pacific/Fakaofo" };

		String timezone = "";
		String[] timezone1 = {};
		String currentUTCDate = getTodayDateInUTC();
		for (int i = 0; i < timezones.length; i++) {
			timezone1 = timezones[i].split(",");
			String todayDateInTimezone = getTodayDateInTimezone(timezone1[1].trim());
			try {
				int result = compareDates(todayDateInTimezone, currentUTCDate);
				if (result < 0) {
					timezone = timezone1[1].trim();
					logger.info("Timezone: " + timezone + " is less than UTC");
					TestListeners.extentTest.get().info("Timezone: " + timezone + " is less than UTC");
					break;
				}
			} catch (ParseException e) {
				logger.error("Error comparing dates for timezone: " + timezone, e);
			}
		}
		if (timezone.isEmpty()) {
			for (int i = 0; i < timezones.length; i++) {
				timezone1 = timezones[i].split(",");
				String todayDateInTimezone = getTodayDateInTimezone(timezone1[1].trim());
				try {
					int result = compareDates(todayDateInTimezone, currentUTCDate);
					if (result > 0) {
						timezone = timezone1[1].trim();
						logger.info("Timezone: " + timezone + " is greater than UTC");
						TestListeners.extentTest.get().info("Timezone: " + timezone + " is greater than UTC");
						break;
					}
				} catch (ParseException e) {
					logger.error("Error comparing dates for timezone: " + timezone, e);
				}
			}
		}

		return timezone;

	}

	public boolean isRedeemablePresentInDropdown(String redeemableName) {
		boolean isPresent = false;
		try {
			String searchedRedeemableXpath = utils.getLocatorValue("dynamicRewardsPage.searchedVal").replace("$temp",
					redeemableName);
			WebElement searchedSegment = driver.findElement(By.xpath(searchedRedeemableXpath));
			isPresent = searchedSegment.isDisplayed();
		} catch (NoSuchElementException e) {
			isPresent = false;
		}

		return isPresent;

	}

	public void sendRedeemableNameInTextBox(String redeemable) {
		WebElement ele = utils.getLocator("campaignSplitPage.redeemableforSplit");
		ele.click();
		WebElement ele1 = driver.findElement(locators.get("campaignsPage.redeemableSearchOnWhatPage"));
		ele1.sendKeys(redeemable);
	}

}