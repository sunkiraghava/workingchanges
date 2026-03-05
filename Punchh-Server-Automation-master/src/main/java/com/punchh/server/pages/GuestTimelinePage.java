package com.punchh.server.pages;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class GuestTimelinePage {
	static Logger logger = LogManager.getLogger(GuestTimelinePage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	String ssoEmail, temp = "temp";
	private PageObj pageObj;
	private Map<String, By> locators;

	public GuestTimelinePage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(this.driver);
		selUtils = new SeleniumUtilities(driver);
		pageObj = new PageObj(driver);
		locators = utils.getAllByMap();
	}

	public boolean verifyGuestTimelineNew(String joinedVia) {
		boolean flagResult = false;
		List<WebElement> listOfTimelineEventsText = driver
				.findElements(locators.get("guestTimeLine.joiningChannelList"));
		for (WebElement wEle : listOfTimelineEventsText) {
			String actualText = wEle.getText().trim();
			if (actualText.equalsIgnoreCase(joinedVia)) {
				flagResult = true;
				break;
			}
		}
		return flagResult;
	}

	public String getGuestJoiningChannelMobileEmail() {
		WebElement joinedChannel = driver.findElement(locators.get("guestTimeLine.joiningChannelMobile"));
		utils.scrollToElement(driver, joinedChannel);
		String channel = joinedChannel.getText().trim();
		logger.info("Joining channel is ==> " + channel);
		return channel;
	}

	public String getGuestJoiningChannelWebEmail() {
		WebElement joinedChannel = driver.findElement(locators.get("guestTimeLine.joiningChannelWeb"));
		utils.scrollToElement(driver, joinedChannel);
		String channel = joinedChannel.getText().trim();
		logger.info("Joining channel is ==> " + channel);
		return channel;
	}

	public String getGuestJoiningChannelPOS() {
		WebElement joinedChannel = driver.findElement(locators.get("guestTimeLine.joiningChannelPOS"));
		utils.scrollToElement(driver, joinedChannel);
		String channel = joinedChannel.getText().trim();
		logger.info("Joining channel is ==> " + channel);
		return channel;
	}

	public String getGuestTimelineEmail() {
		WebElement guestMail = driver.findElement(locators.get("guestTimeLine.guestMail"));
		String eMail = guestMail.getText().trim();
		return eMail;
	}

	public boolean verifyGuestTimeline(String email) {
		try {
			WebElement guestEmail = driver.findElement(
					By.xpath(utils.getLocatorValue("guestTimeLine.guestEmail").replace("temp", email.toLowerCase())));
			guestEmail.isDisplayed();
			logger.info("Successfully verified guest email on time line: " + email);
			TestListeners.extentTest.get().pass("Successfully verified guest email on time line: " + email);
			return true;
		} catch (Exception e) {
			logger.error("Error in verifying guest time line " + e);
			TestListeners.extentTest.get().fail("Error in verifying guest time line " + e);
		}
		return false;
	}

	public boolean verifyBarcodeCheckinOnGuestTimeline(String barcodeValue) {
		try {
			WebElement barcodeLoyalityLabel = driver.findElement(By
					.xpath(utils.getLocatorValue("guestTimeLine.barcodeLoyalityLabel").replace("temp", barcodeValue)));
			barcodeLoyalityLabel.isDisplayed();
			logger.info("Successfully verified barcode checkin " + barcodeValue + "on guest timne line");
			TestListeners.extentTest.get()
					.pass("Successfully verified barcode checkin " + barcodeValue + "on guest timne line");
			return true;
		} catch (Exception e) {
			logger.error("Error in verifying barcode in guest time line " + e);
			TestListeners.extentTest.get().fail("Error in verifying barcode in guest time line " + e);
		}
		return false;
	}

	public boolean verifyPosCheckinInTimeLine(String punchhKey, String amount, String baseConversionRate) {
		boolean flag = false;
		try {
			String approveLoyalityEle = utils.getLocatorValue("guestTimeLine.approveLoyalityLabel").replace("temp",
					punchhKey);
			WebElement approveLoyalityLabel = driver.findElement(By.xpath(approveLoyalityEle));
			approveLoyalityLabel.isDisplayed();
			logger.info("Successfully verified checkin on timeline ");
			TestListeners.extentTest.get().pass("Successfully verified checkin on timeline ");
			flag = verifyPointAndAmount(amount, baseConversionRate);
		} catch (Exception e) {
			logger.error("Error in verifying POS checkin in guest timeline " + e);
			TestListeners.extentTest.get().fail("Error in verifying barcode in guest timeline " + e);
		}
		return flag;
	}

	private boolean verifyPointAndAmount(String amount, String baseConversionRate) {
		boolean flag = false;
		WebElement amountLabel = driver.findElement(locators.get("guestTimeLine.amountLabel"));
		WebElement pointsLabel = driver.findElement(locators.get("guestTimeLine.pointsLabel"));
		amountLabel.isDisplayed();
		pointsLabel.isDisplayed();
		if (amountLabel.getText().trim().contains(amount)) {
			logger.info("Successfully verified amount: " + amount);
			TestListeners.extentTest.get().pass("Successfully verified amount: " + amount);
		} else {
			logger.error("Failed to verify amount" + amount);
			TestListeners.extentTest.get().fail("Failed to verify amount" + amount);
		}
		int points = Integer.parseInt(amount) * Integer.parseInt(baseConversionRate);
		if (pointsLabel.getText().trim().contains("+" + Integer.toString(points))) {
			logger.info("Points earned: " + points);
			TestListeners.extentTest.get().pass("Points earned: " + points);
			flag = true;
		} else {
			logger.error("Failed to verify amount, expected" + points + "actual:" + pointsLabel.getText());
			TestListeners.extentTest.get()
					.fail("Failed to verify points, expected" + points + "actual:" + pointsLabel.getText());
		}
		return flag;
	}

	public boolean verifyCheckinChannelAndLocation(String checkinChannel, String location) {
		boolean flag = false;
		try {
			WebElement checkinChannelEl = driver.findElement(locators.get("guestTimeLine.checkinChannel"));
			selUtils.scrollToElement(checkinChannelEl);

			checkinChannelEl.isDisplayed();
			if (checkinChannelEl.getText().trim().equals(checkinChannel.trim())) {
				logger.info("Successfully verified checkin channel: " + checkinChannel);
				TestListeners.extentTest.get().pass("Successfully verified checkin channel: " + checkinChannel);
				flag = true;
			} else {
				logger.error("Failed to checkin channel, expected: " + checkinChannel + "actual:"
						+ checkinChannelEl.getText());
				TestListeners.extentTest.get().fail("Failed to checkin channel, expected: " + checkinChannel + "actual:"
						+ checkinChannelEl.getText());
			}
			// selUtils.scrollToElement(driver.findElement(guestTimelineLocators.get("locationLabel"));
			/*
			 * driver.findElement(guestTimelineLocators.get("locationLabel")).isDisplayed();
			 * if (driver.findElement(guestTimelineLocators.get("locationLabel")).getText().
			 * trim().equals( location.trim())) {
			 * logger.info("Successfully verified location: " + location);
			 * TestListeners.extentTest.get().pass("Successfully verified location: " +
			 * location); flag = true; } else {
			 * logger.error("Failed to verify location, expected" + location + "actual: " +
			 * driver.findElement(guestTimelineLocators.get("locationLabel")).getText());
			 * TestListeners.extentTest.get().fail("Failed to verify location, expected: " +
			 * location + "actual:" +
			 * driver.findElement(guestTimelineLocators.get("locationLabel")).getText()); }
			 */
		} catch (Exception e) {
			logger.error("Error in verifying/capturing checkin channel or location " + e);
			TestListeners.extentTest.get().fail("Error in verifying/capturing checkin channel or location" + e);
		}
		return flag;
	}

	public void deleteGuest(String email) {
		try {
			logger.info("Deleting guest: " + email);
			TestListeners.extentTest.get().info("Deleting guest: " + email);
			WebElement guestEmail = driver.findElement(
					By.xpath(utils.getLocatorValue("guestTimeLine.guestEmail").replace("temp", email.toLowerCase())));
			guestEmail.isDisplayed();
			WebElement editProfileTabLink = driver.findElement(locators.get("guestTimeLine.editProfileTabLink"));
			editProfileTabLink.isDisplayed();
			editProfileTabLink.click();
			WebElement guestFunctionButton = driver.findElement(locators.get("guestTimeLine.guestFunctionButton"));
			guestFunctionButton.isDisplayed();
			guestFunctionButton.click();
			WebElement deactiveButton = driver.findElement(locators.get("guestTimeLine.deactiveButton"));
			deactiveButton.isDisplayed();
			deactiveButton.click();
			driver.switchTo().alert().accept();
			WebElement deactiveSuccessText = driver.findElement(locators.get("guestTimeLine.deactiveSuccessText"));
			deactiveSuccessText.isDisplayed();
			WebElement guestFunctionButton2 = driver.findElement(locators.get("guestTimeLine.guestFunctionButton"));
			guestFunctionButton2.click();
			WebElement deleteGuestButton = driver.findElement(locators.get("guestTimeLine.deleteGuestButton"));
			deleteGuestButton.isDisplayed();
			deleteGuestButton.click();
			WebElement deleteReasonDropdown = driver.findElement(locators.get("guestTimeLine.deleteReasonDropdown"));
			deleteReasonDropdown.isDisplayed();
			Select sel = new Select(deleteReasonDropdown);
			sel.selectByValue(prop.getProperty("deleteReason"));
			WebElement deleteButton = driver.findElement(locators.get("guestTimeLine.deleteButton"));
			deleteButton.isDisplayed();
			deleteButton.click();
			WebElement deletedGuestMessage = driver.findElement(locators.get("guestTimeLine.deletedGuestMessage"));
			deletedGuestMessage.isDisplayed();
			logger.info(deletedGuestMessage);
			TestListeners.extentTest.get().info(deletedGuestMessage.getText());
		} catch (Exception e) {
			logger.error("Error in selecting guest " + e);
			TestListeners.extentTest.get().fail("Error in selecting guest " + e);
		}
	}

	public boolean verifyCampaignNotification() throws InterruptedException {
		boolean result = false;
		int attempts = 0;
		while (attempts < 2) {
			// refreshTimeline();
			// Thread.sleep(1000);
			try {
				WebElement campaignNotification = driver
						.findElement(locators.get("guestTimeLine.campaignNotification"));
				if (campaignNotification.isDisplayed()) {
					result = true;
					logger.info("Element:" + campaignNotification + " is present");
					break;
				}
			} catch (Exception e) {
				logger.info("Element is not present ");
			}
			attempts++;
		}
		WebElement campaignNotification = driver.findElement(locators.get("guestTimeLine.campaignNotification"));
		campaignNotification.isDisplayed();
		return result;
	}

	public boolean verifyPushNotification() {

		WebElement pushNotification = driver.findElement(locators.get("guestTimeLine.pushNotification"));
		return utils.checkElementPresent(pushNotification);

	}

	public String getPushNotificationText() {

		WebElement pushNotification = driver.findElement(locators.get("guestTimeLine.pushNotification"));
		String val = pushNotification.getText();
		logger.info("Push noitification is : " + val);
		return val;
	}

	public void extractUrlFromNotificationText(String notificationText) {
		try {
			// Log the notification text
			logger.info("Notification text on user timeline: " + notificationText);

			// Regular expression to match URLs
			String urlRegex = "(https?://\\S+)";
			Pattern pattern = Pattern.compile(urlRegex);
			Matcher matcher = pattern.matcher(notificationText);

			// Extract the URL
			if (matcher.find()) {
				String extractedUrl = matcher.group(1);
				logger.info("Extracted URL: " + extractedUrl);

				// Validate the URL
				if (!extractedUrl.startsWith("http")) {
					logger.info("Invalid URL: " + extractedUrl);
					return;
				}

				// Open the extracted URL in a new tab
				JavascriptExecutor js = (JavascriptExecutor) driver;
				js.executeScript("window.open('" + extractedUrl + "', '_blank');");
				logger.info("Opened the extracted URL in a new tab.");

				// Switch to the new tab
				ArrayList<String> chromeTabs = new ArrayList<>(driver.getWindowHandles());
				driver.switchTo().window(chromeTabs.get(1));
				logger.info("Switched to the new tab.");

				utils.longWaitInSeconds(5);

				// Sign in with the Google test account
				signInWithGoogleTestAccount(prop.getProperty("testAccountEmail"),
						utils.decrypt(prop.getProperty("testAccountPwd")));
				utils.longWaitInSeconds(5);

				// Verify if the Android pass is loaded successfully
				if (driver.getCurrentUrl().contains(prop.getProperty("googlePaySaveUrl"))) {
					logger.info("Android pass of user is loaded successfully.");
				} else {
					logger.info("Some issue occurred, Android pass of user is not loaded.");
				}
			}
		} catch (Exception e) {
			logger.info("Exception occurred: " + e.getMessage());
		}

	}

	public boolean verifyRedeemedCoupon() {

		WebElement coupon = driver.findElement(locators.get("guestTimeLine.redeemedCoupon"));
		return utils.checkElementPresent(coupon);

	}

	public String verifyRedeemedCouponCode() {

		WebElement coupon = driver.findElement(locators.get("guestTimeLine.redeemedCoupon"));
		return coupon.getText();

	}

	public boolean verifyrewardedRedeemable() {

		WebElement rewardedRedeemable = driver.findElement(locators.get("guestTimeLine.rewardedRedeemable"));
		return utils.checkElementPresent(rewardedRedeemable);

	}

	public String verifyrewardedRedeemablegiftedbyCampaign(String redeemableName) {

		WebElement rewardedRedeemable = driver
				.findElement(By.xpath("//a[contains(text(),'Rewarded " + redeemableName + "')]"));
		return rewardedRedeemable.getText();

	}

	public String verifyrewardedRedeemableCampaign() {

		WebElement rewardedRedeemable = driver.findElement(locators.get("guestTimeLine.rewardedRedeemableCampaign"));
		return rewardedRedeemable.getText().trim();

	}

	public String getcampaignName() {

		WebElement campaignName = driver.findElement(locators.get("guestTimeLine.campaignName"));
		String str = campaignName.getText();
		// String name = str.substring(str.indexOf(':'));
		String[] name = str.split(":");
		return name[1].trim();

	}

	public boolean verifyPushNotificationPostCheckin() throws InterruptedException {
		/*
		 * WebElement pushNotification =
		 * driver.findElement(guestTimelineLocators.get("pushNotificationPostCheckin"));
		 * return Utilities.checkElementPresent(pushNotification);
		 */
		boolean result = false;
		int attempts = 0;
		while (attempts < 2) {
			refreshTimeline();
			Thread.sleep(3000);
			try {
				WebElement pushNotification = driver
						.findElement(locators.get("guestTimeLine.pushNotificationPostCheckin"));
				if (pushNotification.isDisplayed()) {
					result = true;
					logger.info("Element:" + pushNotification + " is present");
					break;
				}
			} catch (Exception e) {
				logger.error("Element is not present " + e);
			}
			attempts++;
		}
		return result;
	}

	public boolean verifyGiftedCampainDetails() throws InterruptedException {

		boolean result = false;
		int attempts = 0;
		while (attempts < 30) {
			refreshTimeline();
			Thread.sleep(3000);
			try {
				WebElement pushNotification = driver.findElement(locators.get("guestTimeLine.giftedPsotcheckinQC"));
				if (pushNotification.isDisplayed()) {
					result = true;
					logger.info("Element:" + pushNotification + " is present");
					break;
				}
			} catch (Exception e) {
				logger.info("Element is not present " + e);
			}
			attempts++;
		}
		return result;
	}

	public String verifyrewardedRedeemablePostCheckin() {

		WebElement rewardedRedeemable = driver.findElement(locators.get("guestTimeLine.rewardedRedeemablePostCheckin"));
		return rewardedRedeemable.getText();

	}

	public String getcampaignNamePostCheckin() {

		WebElement campaignNamePostCheckin = driver.findElement(locators.get("guestTimeLine.campaignNamePostCheckin"));
		String str = campaignNamePostCheckin.getText();
		String[] name = str.split(":");
		return name[1].trim();

	}

	public void verifyExternalSourceid() {

		WebElement sourceID = driver.findElement(locators.get("guestTimeLine.sourceID"));
		sourceID.isDisplayed();
		// String str =
		// driver.findElement(guestTimelineLocators.get("menuid")).getText();
		// int value = Integer.parseInt(str.replaceAll("[^0-9]", ""));
		logger.info("External source id present in the Punchh dashboard");
		TestListeners.extentTest.get().info("External source id present in the Punchh dashboard");
		// Assert.assertEquals(value, menuid, "Menu_id did not match");
		// return menuid;

	}
	// public void clickEditProfile() {
	// driver.findElement(guestTimelineLocators.get("editProfileTabLink")).click();
	// }

	public String setPhone1() {
		clickEditProfile();
		driver.findElement(locators.get("guestTimeLine.editphoneNumber")).clear();
		String Phone = prop.getProperty("phonePrefix").replace(temp, CreateDateTime.getTimeDateString());
		String P = Phone.substring(Phone.length() - 10);
		WebElement editphoneNumber = driver.findElement(locators.get("guestTimeLine.editphoneNumber"));
		editphoneNumber.sendKeys(Phone);
		WebElement updateEditProfile = driver.findElement(locators.get("guestTimeLine.updateEditProfile"));
		updateEditProfile.click();
		clickEditProfile();
		Utilities.longWait(20000);
		return P;
	}

	public void refreshTimeline() {
		driver.navigate().refresh();
		utils.longWaitInSeconds(3);
		/*
		 * driver.get(driver.getCurrentUrl());
		 * driver.navigate().to(driver.getCurrentUrl());
		 */
		logger.info("Refreshing timeline....");
	}

	public boolean verifyPushNotificationMassCampaign() throws InterruptedException {

		/*
		 * WebElement pushNotification =
		 * driver.findElement(guestTimelineLocators.get("pushNotificationMassCampaign"))
		 * ; return Utilities.checkElementPresent(pushNotification);
		 */

		boolean result = false;
		int attempts = 0;
		while (attempts < 2) {
			try {
				WebElement pushNotification = driver
						.findElement(locators.get("guestTimeLine.pushNotificationMassCampaign"));
				if (pushNotification.isDisplayed()) {
					result = true;
					logger.info("Element:" + pushNotification + " is present");
					break;
				}
			} catch (Exception e) {
				logger.info("Element is not present " + e);
			}
			attempts++;
		}
		return result;

	}

	public boolean verifyrewardedRedeemableMassCampaign() {
		WebElement rewardedRedeemable = driver
				.findElement(locators.get("guestTimeLine.rewardedRedeemableMasscampaign"));
		return utils.checkElementPresent(rewardedRedeemable);

	}

	public String verifyCampaignRewardName() {
		WebElement rewardedRedeemable = driver.findElement(locators.get("guestTimeLine.camGiftedreward"));
		return rewardedRedeemable.getText();
	}

	public String getcampaignNameMasscampaign(String cname) throws InterruptedException {
		String campName = "";
		int attempts = 0;
		while (attempts <= 20) {
			try {
				utils.implicitWait(2);
				utils.longWaitInSeconds(2);
				WebElement camName = driver.findElement(By.xpath("//div[contains(text(),'" + cname + "')]"));
				String str = camName.getText();
				String[] name = str.split(":");
				campName = name[1].trim();
				if (campName.equalsIgnoreCase(cname)) {
					logger.info("Campaign Name " + cname + " matched on the timeline");
					TestListeners.extentTest.get().pass("Campaign Name " + cname + " matched on the timeline");
					break;
				}
			} catch (Exception e) {
				logger.info(
						"Element is not present or Campaign Name did not matched... polling count is : " + attempts);
				TestListeners.extentTest.get().info(
						"Element is not present or Campaign Name did not matched... polling count is : " + attempts);
				utils.refreshPage();
			}
			attempts++;
		}
		utils.implicitWait(60);
		return campName;
	}

	public String getcampaignNameWithWait(String cname) throws InterruptedException {
		// this method will run for 3 minutes until campaign name is found
		String campName = "";
		int attempts = 0;
		int maxAttempts = 36;
		while (attempts < maxAttempts) {
			utils.longWaitInSeconds(8);
			try {
				utils.implicitWait(2);
				WebElement camName = driver.findElement(By.xpath("//div[contains(text(),'" + cname + "')]"));
				String str = camName.getText();
				String[] name = str.split(":");
				campName = name[1].trim();
				if (campName.equalsIgnoreCase(cname)) {
					logger.info("Campaign Name " + cname + " matched on the timeline");
					TestListeners.extentTest.get().pass("Campaign Name " + cname + " matched on the timeline");
					break;
				}
			} catch (Exception e) {
				logger.info("Element is not present or Campaign Name did not matched... polling count is : "
						+ (attempts + 1) + " of " + maxAttempts);
				TestListeners.extentTest.get()
						.info("Element is not present or Campaign Name did not matched... polling count is : "
								+ (attempts + 1) + " of " + maxAttempts);
				utils.refreshPage();
			}
			attempts++;
		}
		utils.implicitWait(60);
		return campName;
	}

	public String getcampaignNameMasscampaignShortPool(String cname) throws InterruptedException {
		String campName = "";
		int attempts = 0;
		while (attempts <= 5) {
			try {
				utils.implicitWait(2);
				utils.longWaitInSeconds(2);
				WebElement camName = driver.findElement(By.xpath("//div[contains(text(),'" + cname + "')]"));
				String str = camName.getText();
				String[] name = str.split(":");
				campName = name[1].trim();
				if (campName.equalsIgnoreCase(cname)) {
					logger.info("Campaign Name " + cname + " matched on the timeline");
					TestListeners.extentTest.get().pass("Campaign Name " + cname + " matched on the timeline");
					break;
				}
			} catch (Exception e) {
				logger.info(
						"Element is not present or Campaign Name did not matched... polling count is : " + attempts);
				TestListeners.extentTest.get().info(
						"Element is not present or Campaign Name did not matched... polling count is : " + attempts);
				utils.refreshPage();
			}
			attempts++;
		}
		utils.implicitWait(60);
		return campName;
	}

	public boolean CheckIfCampaignTriggered(String cname) throws InterruptedException {
		String campName = "";
		boolean flag = false;
		int attempts = 0;
		while (attempts < 4) {
			try {
				WebElement campaignNameMasscampaign = driver
						.findElement(locators.get("guestTimeLine.campaignNameMasscampaign"));
				String str = campaignNameMasscampaign.getText();
				String[] name = str.split(":");
				campName = name[1].trim();
				if (campName.equalsIgnoreCase(cname)) {
					flag = true;
					logger.info("Campaign Name " + cname + " matched on the timeline");
					TestListeners.extentTest.get().pass("Campaign Name " + cname + " matched on the timeline");
					break;
				}
			} catch (Exception e) {
				logger.info("Element is not present or Campaign Name did not matched...");
				utils.refreshPage();
				utils.longWaitInSeconds(5);
			}
			attempts++;
		}
		return flag;

	}

	public void messageGiftToUser(String subject, String reward, String redeemable, String giftReason) {

		WebElement messageGiftBtn = driver.findElement(locators.get("guestTimeLine.messageGiftBtn"));
		messageGiftBtn.click();
		driver.findElement(locators.get("guestTimeLine.subjectTextBox")).clear();
		WebElement subjectTextBox = driver.findElement(locators.get("guestTimeLine.subjectTextBox"));
		subjectTextBox.sendKeys(subject);

		WebElement giftTypeDrp = driver.findElement(locators.get("guestTimeLine.giftTypeDrp"));
		giftTypeDrp.click();
		List<WebElement> giftTypeDrpList = driver.findElements(locators.get("guestTimeLine.giftTypeDrpList"));
		utils.selectListDrpDwnValue(giftTypeDrpList, reward);

		WebElement redeemableDrp = driver.findElement(locators.get("guestTimeLine.redeemableDrp"));
		redeemableDrp.click();
		List<WebElement> redeemableDrpList = driver.findElements(locators.get("guestTimeLine.redeemableDrpList"));
		utils.selectListDrpDwnValue(redeemableDrpList, redeemable);

		driver.findElement(locators.get("guestTimeLine.giftReasonTxtBox")).clear();
		WebElement giftReasonTxtBox = driver.findElement(locators.get("guestTimeLine.giftReasonTxtBox"));
		giftReasonTxtBox.sendKeys(giftReason);
		WebElement messageBtn = driver.findElement(locators.get("guestTimeLine.messageBtn"));
		messageBtn.click();
		logger.info("Gift from timeline messaged to user successfully");
		TestListeners.extentTest.get().pass("Gift from timeline messaged to user successfully");

	}

	public void messagePointsToUser(String subject, String option, String giftTypes, String giftReason) {
		WebElement messageGiftBtn = driver.findElement(locators.get("guestTimeLine.messageGiftBtn"));
		messageGiftBtn.click();
		driver.findElement(locators.get("guestTimeLine.subjectTextBox")).clear();
		WebElement subjectTextBox = driver.findElement(locators.get("guestTimeLine.subjectTextBox"));
		subjectTextBox.sendKeys(subject);

		WebElement giftTypeDrp = driver.findElement(locators.get("guestTimeLine.giftTypeDrp"));
		giftTypeDrp.click();
		List<WebElement> giftTypeDrpList = driver.findElements(locators.get("guestTimeLine.giftTypeDrpList"));
		utils.selectListDrpDwnValue(giftTypeDrpList, option);

		driver.findElement(locators.get("guestTimeLine.giftPointsTxtBox")).clear();
		WebElement giftPointsTxtBox = driver.findElement(locators.get("guestTimeLine.giftPointsTxtBox"));
		giftPointsTxtBox.sendKeys(giftTypes);

		driver.findElement(locators.get("guestTimeLine.giftReasonTxtBox")).clear();
		WebElement giftReasonTxtBox = driver.findElement(locators.get("guestTimeLine.giftReasonTxtBox"));
		giftReasonTxtBox.sendKeys(giftReason);
		WebElement messageBtn = driver.findElement(locators.get("guestTimeLine.messageBtn"));
		messageBtn.click();

		logger.info("Gift points messaged to user successfully");
		TestListeners.extentTest.get().pass("Gift points messaged to user successfully");
	}

	public void messagePointsToUser(String subject, String location, String option, String giftTypes,
			String giftReason) {
		WebElement messageGiftBtn = driver.findElement(locators.get("guestTimeLine.messageGiftBtn"));
		messageGiftBtn.click();
		utils.waitTillPagePaceDone();

		driver.findElement(locators.get("guestTimeLine.subjectTextBox")).clear();
		WebElement subjectTextBox = driver.findElement(locators.get("guestTimeLine.subjectTextBox"));
		subjectTextBox.sendKeys(subject);

		WebElement locationDrpDwn = driver.findElement(locators.get("guestTimeLine.locationDrpDwn"));
		locationDrpDwn.click();
//		List<WebElement> ele = driver.findElements(guestTimelineLocators.get("locationDrpDwnList"));
//		utils.selectListDrpDwnValue(ele, location);
		WebElement searchInputField = driver.findElement(locators.get("dashboardPage.searchInputField"));
		searchInputField.sendKeys(location);
		searchInputField.sendKeys(Keys.ENTER);

		WebElement giftTypeDrp = driver.findElement(locators.get("guestTimeLine.giftTypeDrp"));
		giftTypeDrp.click();
//		List<WebElement> ele1 = driver.findElements(guestTimelineLocators.get("giftTypeDrpList"));
//		utils.selectListDrpDwnValue(ele1, option);
		WebElement searchInputField2 = driver.findElement(locators.get("dashboardPage.searchInputField"));
		searchInputField2.sendKeys(option);
		searchInputField2.sendKeys(Keys.ENTER);

		driver.findElement(locators.get("guestTimeLine.giftPointsTxtBox")).clear();
		WebElement giftPointsTxtBox = driver.findElement(locators.get("guestTimeLine.giftPointsTxtBox"));
		giftPointsTxtBox.sendKeys(giftTypes);

		driver.findElement(locators.get("guestTimeLine.giftReasonTxtBox")).clear();
		WebElement giftReasonTxtBox = driver.findElement(locators.get("guestTimeLine.giftReasonTxtBox"));
		giftReasonTxtBox.sendKeys(giftReason);
		WebElement messageBtn = driver.findElement(locators.get("guestTimeLine.messageBtn"));
		messageBtn.click();

		logger.info("Gift points messaged to user successfully");
		TestListeners.extentTest.get().pass("Gift points messaged to user successfully");
	}

	public void messageRewardAmountToUser(String subject, String location, String giftTypes, String amount,
			String giftReason) {
		try {
			WebElement messageGiftBtn = driver.findElement(locators.get("guestTimeLine.messageGiftBtn"));
			messageGiftBtn.click();
			driver.findElement(locators.get("guestTimeLine.subjectTextBox")).clear();
			WebElement subjectTextBox = driver.findElement(locators.get("guestTimeLine.subjectTextBox"));
			subjectTextBox.sendKeys(subject);

			WebElement locationDrpDwn = driver.findElement(locators.get("guestTimeLine.locationDrpDwn"));
			locationDrpDwn.click();
			List<WebElement> locationDrpDwnList = driver.findElements(locators.get("guestTimeLine.locationDrpDwnList"));
			utils.selectListDrpDwnValue(locationDrpDwnList, location);

			WebElement giftTypeDrp = driver.findElement(locators.get("guestTimeLine.giftTypeDrp"));
			giftTypeDrp.click();
			List<WebElement> giftTypeDrpList = driver.findElements(locators.get("guestTimeLine.giftTypeDrpList"));
			utils.selectListDrpDwnValue(giftTypeDrpList, giftTypes);

			driver.findElement(locators.get("guestTimeLine.rewardAmountTxtBox")).clear();
			WebElement rewardAmountTxtBox = driver.findElement(locators.get("guestTimeLine.rewardAmountTxtBox"));
			rewardAmountTxtBox.sendKeys(amount);

			driver.findElement(locators.get("guestTimeLine.giftReasonTxtBox")).clear();
			WebElement giftReasonTxtBox = driver.findElement(locators.get("guestTimeLine.giftReasonTxtBox"));
			giftReasonTxtBox.sendKeys(giftReason);
			WebElement messageBtn = driver.findElement(locators.get("guestTimeLine.messageBtn"));
			messageBtn.click();

			TestListeners.extentTest.get().pass("Gift amount messaged to user successfully");

		} catch (Exception e) {
			logger.error("Error in navigating timeline" + e);
			TestListeners.extentTest.get().fail("Error in navigating timeline" + e);
		}
	}

	public void messageOrdersToUser(String subject, String giftTypes, String giftOrders, String giftReason) {

		WebElement messageGiftBtn = driver.findElement(locators.get("guestTimeLine.messageGiftBtn"));
		messageGiftBtn.click();
		driver.findElement(locators.get("guestTimeLine.subjectTextBox")).clear();
		WebElement subjectTextBox = driver.findElement(locators.get("guestTimeLine.subjectTextBox"));
		subjectTextBox.sendKeys(subject);
		TestListeners.extentTest.get().info("Entered Subject successfuly");

		WebElement giftTypeDrp = driver.findElement(locators.get("guestTimeLine.giftTypeDrp"));
		giftTypeDrp.click();
		List<WebElement> giftTypeDrpList = driver.findElements(locators.get("guestTimeLine.giftTypeDrpList"));
		utils.selectListDrpDwnValue(giftTypeDrpList, giftTypes);
		TestListeners.extentTest.get().info("Entered gift type successfuly");

		driver.findElement(locators.get("guestTimeLine.giftPointsTxtBox")).clear();
		WebElement giftPointsTxtBox = driver.findElement(locators.get("guestTimeLine.giftPointsTxtBox"));
		giftPointsTxtBox.sendKeys(giftOrders);
		TestListeners.extentTest.get().info("Entered gift orders successfuly");

		driver.findElement(locators.get("guestTimeLine.giftReasonTxtBox")).clear();
		WebElement giftReasonTxtBox = driver.findElement(locators.get("guestTimeLine.giftReasonTxtBox"));
		giftReasonTxtBox.sendKeys(giftReason);
		WebElement messageBtn = driver.findElement(locators.get("guestTimeLine.messageBtn"));
		messageBtn.click();

		TestListeners.extentTest.get().pass("Gift amount messaged to user successfully");

	}

	public String getRewardName() {
		WebElement rewardNametext = driver.findElement(locators.get("guestTimeLine.rewardNametext"));
		return rewardNametext.getText();
	}

	public String getRewardNme() {
		WebElement rewardNametxt = driver.findElement(locators.get("guestTimeLine.rewardNametxt"));
		return rewardNametxt.getText();
	}

	public String redeemedRedemption(String text) {
		WebElement ele = driver.findElement(By.linkText(text));
		return ele.getText();

	}

	/*
	 * public String getDiscountValueWebCheckinDetails() throws InterruptedException
	 * { String discountValue = ""; try {
	 * driver.findElement(guestTimelineLocators.get("redemptionCamWeb")).isDisplayed
	 * ();
	 * driver.findElement(guestTimelineLocators.get("redemptionCamWeb")).click();
	 * Thread.sleep(3000); discountValue =
	 * driver.findElement(guestTimelineLocators.get("discounted")).getText();
	 * discountValue = discountValue.replaceAll("[$]", "");
	 * logger.info("Discounted value in web checkin reciept is :" + discountValue);
	 * TestListeners.extentTest.get()
	 * .pass("Checkin reciept displayed successfully," + "Discounted value is :" +
	 * discountValue);
	 * 
	 * } catch (Exception e) { logger.error("Error in navigating to checkin reciept"
	 * + e);
	 * TestListeners.extentTest.get().fail("Error in navigating to checkin reciept"
	 * + e); } return discountValue; }
	 */

	/*
	 * public String getDiscountValuePosCheckinDetails() throws InterruptedException
	 * { String punchhDiscount = ""; try {
	 * driver.findElement(guestTimelineLocators.get("redemptionCamPos")).isDisplayed
	 * ();
	 * driver.findElement(guestTimelineLocators.get("redemptionCamPos")).click();
	 * Thread.sleep(3000); punchhDiscount =
	 * driver.findElement(guestTimelineLocators.get("punchhDiscount")).getText();
	 * punchhDiscount = punchhDiscount.replaceAll("[- $]", "");
	 * logger.info("Discounted value in pos checkin reciept is :" + punchhDiscount);
	 * TestListeners.extentTest.get().
	 * pass("Pos checkin reciept displayed successfully,"); } catch (Exception e) {
	 * logger.error("Error in navigating to pos checkin reciept" + e);
	 * TestListeners.extentTest.get().
	 * fail("Error in navigating to pos checkin reciept" + e); } return
	 * punchhDiscount; }
	 */

	public String getDiscountValuePosCheckinDetails() throws InterruptedException {
		String punchhDiscount = "";
		// boolean result = false;
		int attempts = 0;
		while (attempts < 5) {
			refreshTimeline();
			Thread.sleep(1000);
			try {
				WebElement posCam = driver.findElement(locators.get("guestTimeLine.redemptionCamPos"));
				if (posCam.isDisplayed()) {
					// result = true;
					logger.info("Element:" + posCam + " is present");
					break;
				}
			} catch (Exception e) {
				logger.info("Error in navigating to pos checkin reciept icon" + e);
			}
			attempts++;
		}
		WebElement redemptionCamPos = driver.findElement(locators.get("guestTimeLine.redemptionCamPos"));
		redemptionCamPos.click();
		selUtils.implicitWait(5);
		WebElement punchhDiscountEl = driver.findElement(locators.get("guestTimeLine.punchhDiscount"));
		punchhDiscount = punchhDiscountEl.getText();
		punchhDiscount = punchhDiscount.replaceAll("[- $]", "");
		logger.info("Discounted value in pos checkin reciept is :" + punchhDiscount);
		TestListeners.extentTest.get().pass("Pos checkin reciept displayed successfully,");
		return punchhDiscount;
	}

	public String getDiscountValueWebCheckinDetails() throws InterruptedException {
		String discountValue = "";
		// boolean result = false;
		int attempts = 0;
		while (attempts < 8) {
			refreshTimeline();
			Thread.sleep(1000);
			try {
				WebElement webCam = driver.findElement(locators.get("guestTimeLine.redemptionCamWeb"));
				if (webCam.isDisplayed()) {
					// result = true;
					logger.info("Element:" + webCam + " is present");
					break;
				}
			} catch (Exception e) {
				logger.info("Error in navigating to web checkin reciept icon" + e);
			}
			attempts++;
		}
		WebElement redemptionCamWeb = driver.findElement(locators.get("guestTimeLine.redemptionCamWeb"));
		redemptionCamWeb.click();
		Thread.sleep(5000);
		WebElement discounted = driver.findElement(locators.get("guestTimeLine.discounted"));
		discountValue = discounted.getText();
		discountValue = discountValue.replaceAll("[$]", "");
		logger.info("Discounted value in web checkin reciept is :" + discountValue);
		TestListeners.extentTest.get()
				.pass("Checkin reciept displayed successfully," + "Discounted value is :" + discountValue);
		return discountValue;
	}

	public String getDiscountValuePosApprovedLoyaltyDetails() throws InterruptedException {
		String punchhDiscount = "";
		try {
			WebElement loyaltyApproveCamPos = driver.findElement(locators.get("guestTimeLine.loyaltyApproveCamPos"));
			loyaltyApproveCamPos.isDisplayed();
			loyaltyApproveCamPos.click();
			Thread.sleep(3000);
			WebElement punchhDiscountEl = driver.findElement(locators.get("guestTimeLine.punchhDiscount"));
			punchhDiscount = punchhDiscountEl.getText();

			punchhDiscount = punchhDiscount.replaceAll("[- $]", "");
			logger.info("Discounted value in pos checkin reciept is :" + punchhDiscount);
			TestListeners.extentTest.get().pass(
					"Pos checkin reciept displayed successfully," + "Punchh discount value is :" + punchhDiscount);

		} catch (Exception e) {
			logger.error("Error in navigating to pos checkin reciept" + e);
			TestListeners.extentTest.get().fail("Error in navigating to pos checkin reciept" + e);
		}
		return punchhDiscount;
	}

	public String getDiscountedAmountPosCheckinDetails() {

		WebElement discountedAmount = driver.findElement(locators.get("guestTimeLine.discountedAmount"));
		String val = discountedAmount.getText();
		val = val.replaceAll("[$]", "");
		logger.info("Total amount after discount in pos checkin reciept is :" + val);
		TestListeners.extentTest.get().pass("Total amount after discount in pos checkin reciept is :" + val);
		return val;

	}

	public String getTotalAmountWebCheckin() {
		WebElement totalAmt = driver.findElement(locators.get("guestTimeLine.totalAmt"));
		String val = totalAmt.getText();
		val = val.replaceAll("[$]", "");
		logger.info("Total amount in web checkin reciept is :" + val);
		TestListeners.extentTest.get().pass("Total amount in checkin reciept is :" + val);
		return val;
	}

	public void clickAccountHistory() {
		// WebElement loyalty =
		// driver.findElement(guestTimelineLocators.get("loyalty"));
		// utils.scrollToElement(driver, loyalty);
		utils.longWaitInSeconds(3);
		WebElement accountHistory = driver.findElement(locators.get("guestTimeLine.accountHistoryBtn"));
		utils.clickByJSExecutor(driver, accountHistory);
		// utils.waitTillPagePaceDone();
		logger.info("Clicked account history button");
		TestListeners.extentTest.get().info("Clicked account history button");
	}

	public void clickRewards() {
		WebElement loyalty = driver.findElement(locators.get("guestTimeLine.loyalty"));
		selUtils.scrollToElement(loyalty);
		WebElement rewardsBtn = driver.findElement(locators.get("guestTimeLine.rewardsBtn"));
		rewardsBtn.click();
		logger.info("Clicked rewards button");
		TestListeners.extentTest.get().info("Clicked rewards button");
	}

	public void clickTimeLine() {
		WebElement timelineBtn = driver.findElement(locators.get("guestTimeLine.timelineBtn"));
		timelineBtn.click();
	}

	public String validateForceRedemptiononTimeLine() {
		clickTimeLine();
		WebElement forceRedemptionNotification = driver
				.findElement(locators.get("guestTimeLine.forceRedemptionNotification"));
		String val = forceRedemptionNotification.getText();
		return val;

	}

	public String geteForceRedeemedAmount() {
		WebElement redemedAmount = driver.findElement(locators.get("guestTimeLine.redemedAmount"));
		String val = redemedAmount.getText();
		return val;
	}

	public void navigateToTabs(String tabName) {
		utils.waitTillPagePaceDone();
		try {
			String tabXpath = utils.getLocatorValue("guestTimeLine.guestTimeLineTabs").replace("$TabName",
					tabName.trim());
			selUtils.waitTillElementToBeClickable(utils.getXpathWebElements(By.xpath(tabXpath)));
			selUtils.longWait(2000);
			utils.getXpathWebElements(By.xpath(tabXpath)).click();
			selUtils.longWait(200);
			utils.waitTillPagePaceDone();
			logger.info("Clicked on " + tabName + " tab");
			TestListeners.extentTest.get().info("Clicked on " + tabName + " tab");
		} catch (Exception e) {
			WebElement clickDropDownOnTimeline = driver
					.findElement(locators.get("guestTimeLine.clickDropDownOnTimeline"));
			clickDropDownOnTimeline.click();
			String tabXpath = utils.getLocatorValue("guestTimeLine.clickDropDownOnTimelineTab").replace("$TabName",
					tabName.trim());
			selUtils.waitTillElementToBeClickable(utils.getXpathWebElements(By.xpath(tabXpath)));
			selUtils.longWait(2000);
			utils.getXpathWebElements(By.xpath(tabXpath)).click();
			selUtils.longWait(200);
			utils.waitTillPagePaceDone();
			logger.info("Clicked on " + tabName + " tab");
			TestListeners.extentTest.get().info("Clicked on " + tabName + " tab");
		}
	}

	public boolean accountHistoryForCardRedeemed() {
		WebElement CardRedeemed = driver.findElement(locators.get("guestTimeLine.cardRedeemedTag"));
		return utils.checkElementPresent(CardRedeemed);

	}

	public String getTotalRedeemed() {
		WebElement redempsChart = driver.findElement(locators.get("guestTimeLine.redempsChart"));
		String val = redempsChart.getAttribute("data-total_redeemed_checkins");
		logger.info("Total Redeemed orders :" + val);
		TestListeners.extentTest.get().info("Total Redeemed orders :" + val);
		return val;
	}

	public String getTotalRedeemable() {
		WebElement redempsChart = driver.findElement(locators.get("guestTimeLine.redempsChart"));
		String val = redempsChart.getAttribute("data-total_redeemable_checkins");
		logger.info("Total Redeemable orders :" + val);
		TestListeners.extentTest.get().info("Total Redeemable orders :" + val);
		return val;
	}

	public void messageFuelDiscountToUser(String subject, String giftTypes, String fuelAmount, String giftReason) {
		try {
			WebElement messageGiftBtn = driver.findElement(locators.get("guestTimeLine.messageGiftBtn"));
			messageGiftBtn.click();
			driver.findElement(locators.get("guestTimeLine.subjectTextBox")).clear();
			WebElement subjectTextBox = driver.findElement(locators.get("guestTimeLine.subjectTextBox"));
			subjectTextBox.sendKeys(subject);

			WebElement giftTypeDrp = driver.findElement(locators.get("guestTimeLine.giftTypeDrp"));
			giftTypeDrp.click();
			List<WebElement> giftTypeDrpList = driver.findElements(locators.get("guestTimeLine.giftTypeDrpList"));
			utils.selectListDrpDwnValue(giftTypeDrpList, giftTypes);

			driver.findElement(locators.get("guestTimeLine.fuelAmountTextBox")).clear();
			WebElement fuelAmountTextBox = driver.findElement(locators.get("guestTimeLine.fuelAmountTextBox"));
			fuelAmountTextBox.sendKeys(fuelAmount);

			driver.findElement(locators.get("guestTimeLine.giftReasonTxtBox")).clear();
			WebElement giftReasonTxtBox = driver.findElement(locators.get("guestTimeLine.giftReasonTxtBox"));
			giftReasonTxtBox.sendKeys(giftReason);
			WebElement messageBtn = driver.findElement(locators.get("guestTimeLine.messageBtn"));
			messageBtn.click();

			TestListeners.extentTest.get().pass("Fuel amount messaged to user successfully");

		} catch (Exception e) {
			logger.error("Error in navigating timeline" + e);
			TestListeners.extentTest.get().fail("Error in sending fuel amount to user" + e);
		}
	}

	public boolean verifyfuelGiftedNotification() {

		WebElement fuelGiftedNotification = driver.findElement(locators.get("guestTimeLine.fuelCard"));
		return utils.checkElementPresent(fuelGiftedNotification);

	}

	public boolean verifyRedemptionOnTimeline(String redeemedAmount) {
		int i = 0;
		boolean flag = false;
		selUtils.implicitWait(1);
		try {
			while (i < 40) {
				try {
					driver.navigate().refresh();
					driver.findElement(By.xpath(utils.getLocatorValue("guestTimeLine.redemptionLabel").replace("temp",
							redeemedAmount.trim()))).isDisplayed();
					logger.info("Sucessfully verfied amount $" + redeemedAmount + " redeemed from timeline");
					TestListeners.extentTest.get()
							.pass("Sucessfully verfied amount $" + redeemedAmount + " redeemed from timeline");
					flag = true;
					break;
				} catch (Exception e) {
				}
				i++;
			}

		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		}
		return flag;
	}

	public boolean verifyRedemptionOnAccountHistory(String redeemedAmount, String rewardEarned) {
		boolean flag = false;
		String temp = "($temp)";
		try {
			WebElement accountHistoryBtn = driver.findElement(locators.get("guestTimeLine.accountHistoryBtn"));
			accountHistoryBtn.isDisplayed();
			selUtils.jsClick(accountHistoryBtn);
			WebElement redeemedAmountLabel = driver.findElement(locators.get("guestTimeLine.redeemedAmountLabel"));
			redeemedAmountLabel.isDisplayed();
			// String num2 = utils.getNumbersFromString(rewardEarned);
			// Float num = Float.parseFloat(num2) -
			// Float.parseFloat(utils.getNumbersFromString(redeemedAmount));
			String redeemAmountExpected = temp.replace("temp", redeemedAmount);
			// String str1 =
			// utils.getNumbersFromString((driver.findElement(guestTimelineLocators.get("pendingAmountLabel")).getText()));
			// Float pendingAmountActual = Float.parseFloat(str1);
			if (redeemedAmountLabel.getText().trim().equals(redeemAmountExpected.trim())) {
				logger.info("Successfully verified redeem amount $" + redeemedAmount + " from guest account history");
				TestListeners.extentTest.get()
						.pass("Sucessfully verfied redeem amount $" + redeemedAmount + " from guest account history");
				flag = true;
			}
			// if (Float.compare(pendingAmountActual, num) == 0) {
			// logger.info("Successfully verified remaining amount: $" + rewardEarned + "
			// from guest account history");
			// TestListeners.extentTest.get()
			// .pass("Successfully verified remaining amount: $" + rewardEarned + " from
			// guest account history");
			// } else
			// flag = false;
		} catch (NumberFormatException e) {
			System.out.println(e);
			e.printStackTrace();
		}
		return flag;
	}

	public String getHonored() {
		WebElement honoredReward = driver.findElement(locators.get("guestTimeLine.honoredReward"));
		String val = honoredReward.getText();
		logger.info("Honored reward is is :" + val);
		return val;
	}

	public int getCheckinCount() {
		List<WebElement> approvedLoyalityLabelList = driver
				.findElements(locators.get("guestTimeLine.approvedLoyalityLabel"));
		return approvedLoyalityLabelList.size();
	}

	public String verifyRedeemableCard() {
		WebElement redeemableCardLabel = driver.findElement(locators.get("guestTimeLine.redeemableCardLabel"));
		String val = redeemableCardLabel.getText();
		return val;
	}

	public int getRedeemablePointCount() {
		int count = 0;
		WebElement redeemablePointLabel = driver.findElement(locators.get("guestTimeLine.redeemablePointLabel"));
		redeemablePointLabel.isDisplayed();
		count = Integer.parseInt(redeemablePointLabel.getText().replace(",", "").replace(".0", ""));
		logger.info("Redeemable point count is:" + count);
		TestListeners.extentTest.get().info("Redeemable point count is:" + count);
		return count;
	}

	public String getLoyaltyPieChartDetails() {
		WebElement loyaltyPieChart = driver.findElement(locators.get("guestTimeLine.loyaltyPieChart"));
		String value = loyaltyPieChart.getText();
		String pointsValue[] = value.split("Loyalty");
		String loyaltyPoints = pointsValue[1].trim();
		logger.info("loyalty pie chart value is :" + value);
		TestListeners.extentTest.get().info("loyalty pie chart value is :" + value);
		return loyaltyPoints;
	}

	public boolean accountHistoryForItemRedeemed() {
		WebElement itemRedeemed = driver.findElement(locators.get("guestTimeLine.itemRedeemedLabel"));
		return utils.checkElementPresent(itemRedeemed);
	}

	public boolean accountHistoryForItemRedeemed1() {
		WebElement itemRedeemed = driver.findElement(locators.get("guestTimeLine.itemRedeemedLabel1"));
		return utils.checkElementPresent(itemRedeemed);
	}

	public boolean verifyEclubGuestOnGuestTimeline(String email) {
		boolean flag = false;
		int attempts = 0;
		while (attempts <= 4) {
			utils.longWaitInSeconds(3);
			try {
				driver.navigate().refresh();
				driver.findElement(By
						.xpath(utils.getLocatorValue("guestTimeLine.guestEmail").replace("temp", email.toLowerCase())))
						.isDisplayed();
				WebElement uploadedToEclubText = driver.findElement(locators.get("guestTimeLine.uploadedToEclubText"));
				uploadedToEclubText.isDisplayed();
				logger.info("Successfully verified eclub guest:  " + email);
				TestListeners.extentTest.get().pass("Successfully verified eclub guest: " + email);
				flag = true;
				break;
			} catch (Exception e) {
				logger.info("Error in verifying guest time line " + e);
				TestListeners.extentTest.get().info("Error in verifying guest time line " + e);
			}
			attempts++;
		}
		return flag;
	}

	public String getSystemNotificatioGiftCard(String cname) throws InterruptedException {
		String campName = "";
		int attempts = 0;
		while (attempts <= 12) {
			utils.longWaitInSeconds(1);
			try {
				WebElement camName = driver.findElement(By.xpath("//div[contains(text(),'" + cname + "')]"));
				String str = camName.getText();
				String[] name = str.split(":");
				campName = name[1].trim();
				if (campName.equalsIgnoreCase(cname)) {
					logger.info("Campaign Name " + cname + " matched on the timeline");
					TestListeners.extentTest.get().pass("Campaign Name " + cname + " matched on the timeline");
					break;
				}
			} catch (Exception e) {
				logger.info(
						"Element is not present or Gift Card System Notification did not matched... polling count is : "
								+ attempts);
				TestListeners.extentTest.get().info(
						"Element is not present or Gift Card System Notification did not matched... polling count is : "
								+ attempts);
				utils.refreshPage();
			}
			attempts++;
		}
		return campName;
	}

	public String getGiftCard() {
		String val = "";
		WebElement giftCardNotification = driver.findElement(locators.get("guestTimeLine.giftCardNotification"));
		giftCardNotification.isDisplayed();
		val = giftCardNotification.getText();
		logger.info("System notification for gift card purchase is :" + val);
		return val;

	}

	public void clickGiftCards() {
		WebElement loyalty = driver.findElement(locators.get("guestTimeLine.loyalty"));
		selUtils.scrollToElement(loyalty);
		WebElement giftCardBtn = driver.findElement(locators.get("guestTimeLine.giftCardBtn"));
		giftCardBtn.click();
		logger.info("Clicked gift cards button");

	}

	public String getCardReloadNotification() {
		WebElement cardReloadNotification = driver.findElement(locators.get("guestTimeLine.cardReloadNotification"));
		cardReloadNotification.isDisplayed();
		String val = cardReloadNotification.getText();
		logger.info("Notification for gift card reload is :" + val);
		return val;

	}

	public String getCardTransactionnumber() {
		WebElement cardTxnDetails = driver.findElement(locators.get("guestTimeLine.cardTxnDetails"));
		cardTxnDetails.isDisplayed();
		String val = cardTxnDetails.getText();
		String a[] = val.split(":");
		String b[] = a[1].split(" ");
		logger.info("Transaction number  is :" + b[1].trim());
		return b[1].trim();

	}

	public String getReferralCode() {
		WebElement referralCodeLabel = driver.findElement(locators.get("guestTimeLine.referralCodeLabel"));
		referralCodeLabel.isDisplayed();
		String val = referralCodeLabel.getText();
		return val;
	}

	public String getGuestCode() {
		WebElement guestCodeLabel = driver.findElement(locators.get("guestTimeLine.guestCodeLabel"));
		selUtils.waitTillVisibilityOfElement(guestCodeLabel, "Guest Code");
		String val = guestCodeLabel.getText();
		return val;
	}

	public void verifyReferralCodeInReferredUserTimeline(String referralCode) {
		WebElement referredByLabel = driver.findElement(locators.get("guestTimeLine.referredByLabel"));
		referredByLabel.isDisplayed();
		WebElement inviteCodeLabel = driver.findElement(
				By.xpath(utils.getLocatorValue("guestTimeLine.inviteCodeLabel").replace("temp", referralCode)));
		inviteCodeLabel.isDisplayed();
		logger.info("Verified user is created via referral");
		TestListeners.extentTest.get().pass("Verified user is created via referral");

	}

	public boolean verifyReferralCampaign(String referralCampaignName, String fullName, String giftAmount) {
		// String amount = Utilities.getApiConfigProperty("checkinAmount");
		// String xptha = "//div[normalize-space(text())='Campaign Name:
		// referralCampaignName']/../..//span[contains(.,'received giftAmount referral
		// points for referring fullName')]";
		String tempLoc = utils.getLocatorValue("guestTimeLine.campaignNameLabel").replace("referralCampaignName",
				referralCampaignName);
		tempLoc = tempLoc.replace("fullName", fullName);
		tempLoc = tempLoc.replace("giftAmount", giftAmount);
		driver.navigate().refresh();
		selUtils.implicitWait(1);
		int i = 0;
		while (i < 500) {
			driver.navigate().refresh();
			List<WebElement> campaignNameLabelList = driver.findElements(By.xpath(tempLoc));
			if (campaignNameLabelList.size() > 0) {
				break;
			}
			i++;
		}
		// driver.findElement(By.xpath(tempLoc)).isDisplayed();
		WebElement campaignNameLabel = driver.findElement(By.xpath(tempLoc));
		return campaignNameLabel.isDisplayed();
	}

	public void verifyBronzeMembership() {
		WebElement bronzeLevelMembershipLabel = driver
				.findElement(locators.get("guestTimeLine.bronzeLevelMembershipLabel"));
		bronzeLevelMembershipLabel.isDisplayed();
		logger.info("Verified user's entry membership");
		TestListeners.extentTest.get().pass("Verified user's entry membership");
	}

	public void verifySilverMembership() {
		int i = 0;
		driver.navigate().refresh();
		selUtils.implicitWait(50);
		WebElement silverLevelLabel = driver.findElement(locators.get("guestTimeLine.silverLevelLabel"));
		silverLevelLabel.isDisplayed();
		logger.info("Verified user's silver level membership");
		TestListeners.extentTest.get().pass("Verified user's silver level membership");
		WebElement memberBumpRewardAmountLabel = driver
				.findElement(locators.get("guestTimeLine.memberBumpRewardAmountLabel"));
		memberBumpRewardAmountLabel.isDisplayed();
		logger.info("Verified bump reward amount");
		TestListeners.extentTest.get().pass("Verified bump reward amount");
		driver.navigate().refresh();
		selUtils.implicitWait(1);
		while (i < 50) {
			driver.navigate().refresh();
			List<WebElement> silverLeveCongrats1LabelList = driver
					.findElements(locators.get("guestTimeLine.silverLeveCongrats1Label"));
			if (silverLeveCongrats1LabelList.size() > 0) {
				break;
			}
			i++;
		}
		WebElement silverLeveCongrats1Label = driver
				.findElement(locators.get("guestTimeLine.silverLeveCongrats1Label"));
		silverLeveCongrats1Label.isDisplayed();
		logger.info("Verified user's reward on membership bump");
		TestListeners.extentTest.get().pass("Verified user's reward on membership bump");
	}

	public boolean verifyAuthCheckinInTimeLine(String externalUid, String amount, String baseConversionRate) {
		boolean flag = false;
		try {
			String approveLoyalityEle = utils.getLocatorValue("guestTimeLine.authLoyaltyLabel").replace("temp",
					externalUid);
			WebElement authLoyaltyLabel = driver.findElement(By.xpath(approveLoyalityEle));
			authLoyaltyLabel.isDisplayed();
			logger.info("Successfully verified key ");
			TestListeners.extentTest.get().pass("Successfully verified key");
			flag = verifyPointAndAmount(amount, baseConversionRate);
		} catch (Exception e) {
			logger.error("Error in verifying POS checkin in guest timeline " + e);
			TestListeners.extentTest.get().fail("Error in verifying barcode in guest timeline " + e);
		}
		return flag;
	}

	public void verifyEclubUser(String email) {
		WebElement guestEmail = driver.findElement(
				By.xpath(utils.getLocatorValue("guestTimeLine.guestEmail").replace("temp", email.toLowerCase())));
		guestEmail.isDisplayed();
		WebElement uploadedEclubLabel = driver.findElement(locators.get("guestTimeLine.uploadedEclubLabel"));
		uploadedEclubLabel.isDisplayed();
		logger.info("Verified eclub user on timeline");
		TestListeners.extentTest.get().pass("Verified eclub user on timeline");
	}

	public String giftCardTransfer() {
		WebElement transferCard = driver.findElement(locators.get("guestTimeLine.transferCard"));
		transferCard.isDisplayed();
		String val = transferCard.getText();

		return val;

	}

	public String getApprovedLoyalty() {
		WebElement approvadLoyalty = driver.findElement(locators.get("guestTimeLine.approvadLoyalty"));
		approvadLoyalty.isDisplayed();
		String val = approvadLoyalty.getText();
		return val;
	}

	public String getPendingLoyalty() {
		WebElement pendingLoyalty = driver.findElement(locators.get("guestTimeLine.pendingLoyalty"));
		pendingLoyalty.isDisplayed();
		String val = pendingLoyalty.getText();
		return val;
	}

	public String getDisapprovedLoyalty() {
		WebElement disapprovedLoyalty = driver.findElement(locators.get("guestTimeLine.disapprovedLoyalty"));
		disapprovedLoyalty.isDisplayed();
		String val = disapprovedLoyalty.getText();
		return val;
	}

	public String getPendingPoints() {
		WebElement pendingPoints = driver.findElement(locators.get("guestTimeLine.pendingPoints"));
		pendingPoints.isDisplayed();
		String val = pendingPoints.getText();
		return val;
	}

	public String getRecieptApprovedLoyaltyDetails() throws InterruptedException {
		String punchhDiscount = "";
		try {
			WebElement loyaltyApproveCamPos = driver.findElement(locators.get("guestTimeLine.loyaltyApproveCamPos"));
			loyaltyApproveCamPos.isDisplayed();
			loyaltyApproveCamPos.click();
			Thread.sleep(3000);
			WebElement punchhDiscountEl = driver.findElement(locators.get("guestTimeLine.punchhDiscount"));
			punchhDiscount = punchhDiscountEl.getText();

			punchhDiscount = punchhDiscount.replaceAll("[- $]", "");
			logger.info("value in pos checkin reciept is :" + punchhDiscount);
			TestListeners.extentTest.get()
					.pass("checkin reciept displayed successfully," + "value is :" + punchhDiscount);

		} catch (Exception e) {
			logger.error("Error in navigating to loyalty checkin reciept" + e);
			TestListeners.extentTest.get().fail("Error in navigating to loyalty checkin reciept" + e);
		}
		return punchhDiscount;
	}

	public boolean verifyEcrmTransaction(String key, String txn_no) {
		try {
			// driver.navigate().refresh();

			WebElement transactionsLabel = driver.findElement(
					By.xpath(utils.getLocatorValue("guestTimeLine.transactionsLabel").replace("temp", key)));
			transactionsLabel.isDisplayed();
			WebElement keyLabel = driver.findElement(locators.get("guestTimeLine.keyLabel"));
			System.out.println(keyLabel.getText());
			System.out.println(keyLabel.getAttribute("outerHTML"));
			keyLabel.getText().equals(txn_no);
			if (keyLabel.getText().contains(txn_no) && transactionsLabel.isDisplayed()) {
				logger.info("Verified timeline for transactional checkin");
				TestListeners.extentTest.get().pass("Verified timeline for transactional checkin");
				return true;
			} else
				return false;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	public boolean verifyUpdatedGuestTimeline(String joinedVia, String email, String updatedFname, String updateLname) {
		try {
			// driver.findElement(guestTimelineLocators.get("guestNameLabel")).isDisplayed();
			WebElement guestEmail = driver.findElement(
					By.xpath(utils.getLocatorValue("guestTimeLine.guestEmail").replace("temp", email.toLowerCase())));
			guestEmail.isDisplayed();
			String joinedViaTemp = utils.getLocatorValue("guestTimeLine.joinedViaEmailLabel").replace("temp",
					joinedVia);
			WebElement joinedViaEmailLabel = driver.findElement(By.xpath(joinedViaTemp));
			joinedViaEmailLabel.isDisplayed();
			logger.info("Successfully verified updated guest email, name and user is " + joinedViaEmailLabel.getText());
			TestListeners.extentTest.get().pass(
					"Successfully verified updated guest email, name and user is " + joinedViaEmailLabel.getText());
			System.out.println(utils.getLocatorValue("guestTimeLine.nameLabel").replace("fName", updatedFname)
					.replace("lName", updateLname));
			WebElement nameLabel = driver.findElement(By.xpath(utils.getLocatorValue("guestTimeLine.nameLabel")
					.replace("fName", updatedFname).replace("lName", updateLname)));
			nameLabel.isDisplayed();
			logger.info("Verified updated first and last name " + updatedFname + " " + updateLname);
			TestListeners.extentTest.get()
					.pass("Verified updated first and last name " + updatedFname + " " + updateLname);
			return true;
		} catch (Exception e) {
			logger.error("Error in verifying guest time line " + e);
			TestListeners.extentTest.get().fail("Error in verifying guest time line " + e);
		}
		return false;
	}

	public boolean verifyRecieptTagCheckin(String rectag) throws InterruptedException {
		boolean result = false;
		int attempts = 0;
		while (attempts < 25) {
			refreshTimeline();
			Thread.sleep(2000);
			try {
				List<WebElement> reciepttagLoyalty = driver
						.findElements(locators.get("guestTimeLine.ApprovedReciptTagList"));
				List<WebElement> reciepttagLoyaltypanel = driver
						.findElements(locators.get("guestTimeLine.RecieptTagPanelList"));

				List<String> recieptTagLoyalty = reciepttagLoyalty.stream().map(s -> s.getText())
						.collect(Collectors.toList());
				List<String> recieptTagLoyaltypanel = reciepttagLoyaltypanel.stream().map(s -> s.getText())
						.collect(Collectors.toList());

				logger.info(recieptTagLoyalty);
				logger.info(recieptTagLoyaltypanel);

				for (int i = 0; i < recieptTagLoyalty.size(); i++) {
					if ((recieptTagLoyalty.get(i).equals(rectag)) && recieptTagLoyaltypanel.get(i).equals(rectag)) {
						result = true;
						logger.info(
								"Reciept tag is displayed in loyalty notification and side bar tags panel: " + rectag);
						TestListeners.extentTest.get().pass(
								"Reciept tag is displayed successfully in loyalty notification and side bar tags panel:"
										+ rectag);
						break;
					}
				}
			} catch (Exception e) {
				logger.info("Reciept tag is not present in loyalty notification and side bar tags panel");
				TestListeners.extentTest.get().pass(
						"Reciept tag is not displayed successfully in loyalty notification and side bar tags panel:"
								+ rectag);
			}
			attempts++;
			if (result == true)
				break;
		}
		selUtils.implicitWait(50);
		return result;
	}

	public boolean verifyMigrationPoint() {
		try {
			int convertedPoints = Integer.parseInt(Utilities.getConfigProperty("orignalPoint"))
					* Integer.parseInt(Utilities.getConfigProperty("rateOfConversion"));
			String migrationPointMsgLabel = utils.getLocatorValue("guestTimeLine.migrationPointMsgLabel")
					.replace("temp", Integer.toString(convertedPoints));
			WebElement migrationPointMsgLabelEl = driver.findElement(By.xpath(migrationPointMsgLabel));
			migrationPointMsgLabelEl.isDisplayed();
			String giftPointLabel = utils.getLocatorValue("guestTimeLine.giftPointLabel").replace("temp",
					Integer.toString(convertedPoints));
			WebElement giftPointLabelEl = driver.findElement(By.xpath(giftPointLabel));
			giftPointLabelEl.isDisplayed();
			logger.info(
					"Successfully verified migrated guest points on time line: " + migrationPointMsgLabelEl.getText());
			TestListeners.extentTest.get()
					.pass("Successfully verified guest email, Name and user is " + migrationPointMsgLabelEl.getText());
			return true;
		} catch (Exception e) {
			logger.error("Error in verifying guest time line " + e);
			TestListeners.extentTest.get().fail("Error in verifying guest time line " + e);
		}
		return false;
	}

	// "oldProgPointLabel":"//div[normalize-space(text())='Old Program
	// Points']/ancestor::div[@class='mb-1']//div[normalize-space(text())='temp']",
	// "initalPointLabel":"//div[normalize-space(text())='Old Program
	// Points']/ancestor::div[@class='mb-1']//div[.='Initial Points: temp']",
	// "originalPointLabel"

	public boolean verifyOldProgramPoint() {
		try {
			int totalPointsAdded = Integer.parseInt(Utilities.getConfigProperty("orignalPoint"))
					* Integer.parseInt(Utilities.getConfigProperty("rateOfConversion"));
			String totalPointsAddedLabel = utils.getLocatorValue("guestTimeLine.migrationPointMsgLabel").replace("temp",
					Integer.toString(totalPointsAdded));
			WebElement totalPointsAddedLabelEl = driver.findElement(By.xpath(totalPointsAddedLabel));
			totalPointsAddedLabelEl.isDisplayed();
			logger.info("Verified points under Old Program Points: " + totalPointsAddedLabelEl.getText());
			TestListeners.extentTest.get()
					.pass("Verified points under Old Program Points: " + totalPointsAddedLabelEl.getText());

			String orignalPointLabel = utils.getLocatorValue("guestTimeLine.originalPointLabel").replace("temp",
					Utilities.getConfigProperty("orignalPoint"));
			WebElement orignalPointLabelEl = driver.findElement(By.xpath(orignalPointLabel));
			orignalPointLabelEl.isDisplayed();
			logger.info("Verified Orignal Points: " + orignalPointLabelEl.getText());
			TestListeners.extentTest.get().pass("Verified Orignal Points: " + orignalPointLabelEl.getText());

			String initialPointLabel = utils.getLocatorValue("guestTimeLine.initalPointLabel").replace("temp",
					Utilities.getConfigProperty("initialPoint"));
			WebElement initialPointLabelEl = driver.findElement(By.xpath(initialPointLabel));
			initialPointLabelEl.isDisplayed();
			logger.info("Verified Initial Points: " + initialPointLabelEl.getText());
			TestListeners.extentTest.get().pass("Verified Initial Points: " + initialPointLabelEl.getText());
			return true;
		} catch (Exception e) {
			logger.error("Error in verifying old program point guest time line " + e);
			TestListeners.extentTest.get().fail("Error in verifying guest time line " + e);
		}
		return false;
	}

	public void verifyRedeemableWithFutureStartDate(String redeemableName) {
		String giftTypeRedeemable = "Redeemable";
		utils.waitTillPagePaceDone();
		WebElement messageGiftBtn = driver.findElement(locators.get("guestTimeLine.messageGiftBtn"));
		utils.StaleElementclick(driver, messageGiftBtn);
		messageGiftBtn.click();
		WebElement giftTypeDrp = driver.findElement(locators.get("guestTimeLine.giftTypeDrp"));
		giftTypeDrp.click();
		List<WebElement> giftTypeDrpList = driver.findElements(locators.get("guestTimeLine.giftTypeDrpList"));
		utils.selectListDrpDwnValue(giftTypeDrpList, giftTypeRedeemable);
		WebElement redeemableDrp = driver.findElement(locators.get("guestTimeLine.redeemableDrp"));
		selUtils.jsClick(redeemableDrp);
		List<WebElement> elements = driver.findElements(locators.get("guestTimeLine.redeemableDrpList"));
		for (int i = 0; i < elements.size(); i++) {
			if (elements.get(i).getText().equalsIgnoreCase(redeemableName)) {
				logger.error("Scheduled redeemable is appearing under gift redeemable: " + redeemableName);
				TestListeners.extentTest.get()
						.fail("Scheduled redeemable is appearing under gift redeemable: " + redeemableName);
				break;
			}
		}
		logger.info("Verfied Deal created with future start_date is not appearing under gift redeemable");
		TestListeners.extentTest.get()
				.pass("Verfied Deal created with future start_date is not appearing under gift redeemable");
	}

	public boolean bronzeGuestmembershipLabel() {
		try {
			WebElement bronzeGuestmembershipLabel = driver
					.findElement(locators.get("guestTimeLine.bronzeGuestmembershipLabel"));
			bronzeGuestmembershipLabel.isDisplayed();
			logger.info("bronze membership is visible");
			return true;
		} catch (Exception e) {
			// e.printStackTrace();
			return false;
		}
	}

	public boolean silverGuestmembershipLabel() {
		try {
			WebElement silverGuestmembershipLabel = driver
					.findElement(locators.get("guestTimeLine.silverGuestmembershipLabel"));
			silverGuestmembershipLabel.isDisplayed();
			logger.info("silver membership is visible");
			return true;
		} catch (Exception e) {
			// e.printStackTrace();
			return false;
		}
	}

	public boolean goldGuestmembershipLabel() {
		try {
			WebElement goldGuestmembershipLabel = driver
					.findElement(locators.get("guestTimeLine.goldGuestmembershipLabel"));
			goldGuestmembershipLabel.isDisplayed();
			logger.info("gold membership is visible");
			return true;
		} catch (Exception e) {
			// e.printStackTrace();
			return false;
		}
	}

	public void clickEditProfile() {
		WebElement editProfileTabLink = driver.findElement(locators.get("guestTimeLine.editProfileTabLink"));
		editProfileTabLink.click();
	}

	public String setPhone() {
		clickEditProfile();
		driver.findElement(locators.get("guestTimeLine.editphoneNumber")).clear();
		WebElement editphoneNumber = driver.findElement(locators.get("guestTimeLine.editphoneNumber"));
		editphoneNumber.sendKeys("1234567890");
		WebElement updateEditProfile = driver.findElement(locators.get("guestTimeLine.updateEditProfile"));
		updateEditProfile.click();
		WebElement phoneLabel = driver.findElement(locators.get("guestTimeLine.phoneLabel"));
		String phoneresponse = phoneLabel.getText();
		return phoneresponse;
	}

	public boolean membership(String cname) throws InterruptedException {
		String campName = "";
		boolean flag = false;
		int attempts = 0;
		while (attempts < 4) {
			refreshTimeline();
			try {
				WebElement campaignNameMasscampaign = driver
						.findElement(locators.get("guestTimeLine.campaignNameMasscampaign"));
				String str = campaignNameMasscampaign.getText();
				String[] name = str.split(":");
				campName = name[1].trim();
				if (campName.equalsIgnoreCase(cname)) {
					flag = true;
					logger.info("Post Checkin Membership Campaign is displayed");
					TestListeners.extentTest.get().pass("Post Checkin Membership Campaign is displayed");
					break;
				}
			} catch (Exception e) {
				logger.info("Element is not present or Campaign Name did not matched");
			}
			attempts++;
		}
		return flag;

	}
	/*
	 * public boolean verifyPostCheckinMembershipCampaign(String segment,String
	 * campName1,String campName2) throws InterruptedException { boolean flag=
	 * false; if(segment=="Bronze Guests"){ try { boolean verify1 =
	 * membership(campName1);
	 * Assert.assertTrue(verify1,"Automation Postcheckin Bronze did not displayed");
	 * TestListeners.extentTest.get().
	 * pass("Automation Postcheckin Bronze is visible");
	 * 
	 * boolean verify2 = membership(campName2);
	 * Assert.assertFalse(verify2,"Automation Postcheckin Bronze is displayed");
	 * TestListeners.extentTest.get().
	 * pass("Automation Postcheckin Bronze is not visible");
	 * 
	 * flag=true; } catch (AssertionError e) { return false; } }
	 * if(segment=="Silver Guests"){
	 * 
	 * try { boolean verify3 = membership(campName1);
	 * Assert.assertFalse(verify3,"Automation Postcheckin Silver is displayed");
	 * TestListeners.extentTest.get().
	 * pass("Automation Postcheckin Silver is not visible");
	 * 
	 * boolean verify4 = membership(campName2);
	 * Assert.assertTrue(verify4,"Automation Postcheckin Silver did not displayed");
	 * TestListeners.extentTest.get().
	 * pass("Automation Postcheckin Silver is visible"); flag=true; } catch
	 * (AssertionError e) { return false; } } return flag; }
	 */

	public String verifyGuestmembershipLabel() {
		WebElement GuestmembershipLabel = driver.findElement(locators.get("guestTimeLine.GuestmembershipLabel"));
		String membershipName = GuestmembershipLabel.getText();
		return membershipName;
	}

	public void verifyGuestmembershipLabelPresence() {
		try {
			WebElement GuestmembershipLabel = driver.findElement(locators.get("guestTimeLine.GuestmembershipLabel"));
			String membershipName = GuestmembershipLabel.getText();
			logger.info(" Guest membership Label present - " + membershipName);
			TestListeners.extentTest.get().pass(" Guest membership Label present - " + membershipName);
		} catch (Exception e) {
			logger.info(" Guest membership Label not present ");
			TestListeners.extentTest.get().pass(" Guest membership Label not present ");
		}
	}

	public String getSubscriptionPlansFromTimeline() {
		WebElement subscriptionPlanNameLabel = driver
				.findElement(locators.get("guestTimeLine.subscriptionPlanNameLabel"));
		selUtils.waitTillVisibilityOfElement(subscriptionPlanNameLabel, "Subscription Plan Name");
		String actualSPName = subscriptionPlanNameLabel.getText();
		return actualSPName;
	}

	public int getSubscriptionPlanPriceFromTimeline() {
		utils.refreshPage();
		utils.waitTillPagePaceDone();
		WebElement subscriptionPlanPriceLabel = driver
				.findElement(locators.get("guestTimeLine.subscriptionPlanPriceLabel"));
		utils.waitTillVisibilityOfElement(subscriptionPlanPriceLabel, "subscription plan price label");
		String actualSPPrice = subscriptionPlanPriceLabel.getText();
		String[] rstArray = actualSPPrice.split("Purchase Price:");
		int spActualPrice = (int) Double.parseDouble(rstArray[1].trim());
		return spActualPrice;
	}

	public int getSubscriptionRenewID() {
		utils.refreshPage();
		utils.waitTillPagePaceDone();
		WebElement subscriptionPlanPriceLabel = driver
				.findElement(locators.get("guestTimeLine.subscriptionPlanPriceLabel"));
		selUtils.waitTillVisibilityOfElement(subscriptionPlanPriceLabel, "Renewal Plan ID");
		String subID = "0";
		String rst = subscriptionPlanPriceLabel.getText();
		String[] rstArray = rst.replace("|", "#").split("#");
		Pattern p = Pattern.compile("\\d+");
		Matcher m = p.matcher(rstArray[1]);
		// Matcher m = p.matcher(rstArray[1]);
		while (m.find()) {
			subID = m.group();
		}
		int actualSubscriptionID = Integer.parseInt(subID);
		return actualSubscriptionID;

	}

	// guestTimeLine

	public void clickOnSubscriptionCancel(String cancelType) {
		utils.waitTillPagePaceDone();
		WebElement plansSortingThreeoDots = driver.findElement(locators.get("guestTimeLine.plansSortingThreeoDots"));
		utils.waitTillElementToBeClickable(plansSortingThreeoDots);
		plansSortingThreeoDots.isEnabled();
		plansSortingThreeoDots.click();
		String cancelTpyeXpath = utils.getLocatorValue("guestTimeLine.cancelPlanType").replace("$cancelPlanType",
				cancelType);
		utils.getXpathWebElements(By.xpath(cancelTpyeXpath)).click();
	}

	public void accecptSubscriptionCancellation() {
		WebElement subscriptionCancelReason = driver
				.findElement(locators.get("guestTimeLine.subscriptionCancelReason"));
		subscriptionCancelReason.isDisplayed();
		subscriptionCancelReason.sendKeys("Please cancel my subscription , Thanks ");
		WebElement subscriptionCancelYesButton = driver
				.findElement(locators.get("guestTimeLine.subscriptionCancelYesButton"));
		subscriptionCancelYesButton.click();
	}

	public String getSubscriptionCancellationType() {
		driver.navigate().refresh();
		utils.waitTillPagePaceDone();
		// selUtils.implicitWait(10);
		// String autoRenewalStatus =
		// driver.findElement(guestTimelineLocators.get("autoRenewalStatus")).getText();
		// Assert.assertEquals(autoRenewalStatus, "OFF");
		WebElement subscriptionCancellationType = driver
				.findElement(locators.get("guestTimeLine.subscriptionCancellationType"));
		utils.waitTillVisibilityOfElement(subscriptionCancellationType, "subscription cancel type");
		String actualCancellationType = subscriptionCancellationType.getText();
		return actualCancellationType;
	}

	public void messageGiftRewardsToUser(String subject, String giftTypes, String spNameOrAmount, String giftReason) {
		// try {
		utils.waitTillPagePaceDone();
		WebElement messageGiftBtn = driver.findElement(locators.get("guestTimeLine.messageGiftBtn"));
		messageGiftBtn.click();
		driver.findElement(locators.get("guestTimeLine.subjectTextBox")).clear();
		WebElement subjectTextBox = driver.findElement(locators.get("guestTimeLine.subjectTextBox"));
		subjectTextBox.sendKeys(subject);

		WebElement selectGitTypeFromDropdown = driver
				.findElement(locators.get("guestTimeLine.selectGitTypeFromDropdown"));
		utils.selectDrpDwnValue(selectGitTypeFromDropdown, giftTypes);
		// driver.findElement(guestTimelineLocators.get("giftTypeDrp")).click();
		// List<WebElement> elem =
		// driver.findElements(guestTimelineLocators.get("giftTypeDrpList"));
		// utils.selectListDrpDwnValue(elem, giftTypes);
		switch (giftTypes) {
		case "Subscription":
			WebElement selectSegmentNameForGitTypeDropdown = driver
					.findElement(locators.get("guestTimeLine.selectSegmentNameForGitTypeDropdown"));
			utils.selectDrpDwnValue(selectSegmentNameForGitTypeDropdown, spNameOrAmount);

			// driver.findElement(guestTimelineLocators.get("subscriptionPlanMessageDropDown")).click();
			// List<WebElement> elemSubscription = utils
			// .getLocatorList("guestTimeLine.subscriptionPlanMessageDropDownList");
			// utils.selectListDrpDwnValue(elemSubscription, spNameOrAmount);
			break;

		case "Reward Amount":
			driver.findElement(locators.get("guestTimeLine.rewardAmountTxtBox")).clear();
			WebElement rewardAmountTxtBox = driver.findElement(locators.get("guestTimeLine.rewardAmountTxtBox"));
			rewardAmountTxtBox.sendKeys(spNameOrAmount);

		default:
			break;
		}
		WebElement messageBtn = driver.findElement(locators.get("guestTimeLine.messageBtn"));
		selUtils.waitTillElementToBeClickable(messageBtn);
		messageBtn.click();
		TestListeners.extentTest.get().pass("Gift amount messaged to user successfully");
		// } catch (Exception e) {
		// logger.error("Error in navigating timeline" + e);
		// TestListeners.extentTest.get().fail("Error in navigating timeline" + e);
		// }
	}

	public void navigateInsideGuestTimeline(String subMenuName) {
		String xpath = utils.getLocatorValue("guestTimeLine.subMenuName").replace("$subMenu", subMenuName);
		WebElement subMenuElement = driver.findElement(By.xpath(xpath));
		subMenuElement.click();
	}

	public boolean checkAumoutAfterForceRedemptionOnTimeline(String amount) {
		boolean flag = false;
		WebElement forceRedemptionAmount = driver.findElement(locators.get("guestTimeLine.forceRedemptionAmount"));
		String var = forceRedemptionAmount.getText();
		if (var.contains(amount)) {
			flag = true;
			logger.info("Force Redemption amount matched");
		}
		return flag;
	}

	public void verifyDeactivatedGuests(String email) {
		WebElement Guestsearch = driver.findElement(locators.get("guestTimeLine.Guestsearch"));
		Guestsearch.isDisplayed();
		Guestsearch.clear();
		WebElement GuestsearchForSendKeys = driver.findElement(locators.get("guestTimeLine.Guestsearch"));
		GuestsearchForSendKeys.sendKeys(email);
		GuestsearchForSendKeys.sendKeys(Keys.ENTER);
		WebElement deactivatedUserlabel = driver.findElement(
				By.xpath(utils.getLocatorValue("guestTimeLine.deactivatedUserlabel").replace("temp", email)));
		deactivatedUserlabel.isDisplayed();
		logger.info("Deactivated user is appearing on search: " + email);
		TestListeners.extentTest.get().info("Deactivated user is appearing on search: " + email);
	}

	public void reactivateGuests() {
		utils.longwait(3000);
		WebElement reactivateGuest = driver.findElement(locators.get("guestTimeLine.reactivateGuest"));
		reactivateGuest.click();
		logger.info("Clicked Reactivate Guests button");
		TestListeners.extentTest.get().info("Clicked Reactivate Guests button");

		Alert alert = driver.switchTo().alert();
		if (alert.getText().equals("Are you sure you want to reactivate this guest?")) {
			alert.accept();
		} else {
			logger.info("Incorrect alert message");
			TestListeners.extentTest.get().fail("Incorrect alert message");
		}
		WebElement reactivateGuestLabel = driver.findElement(locators.get("guestTimeLine.reactivateGuestLabel"));
		reactivateGuestLabel.isDisplayed();
		logger.info("Successfully reactivated the guest");
		TestListeners.extentTest.get().pass("Successfully reactivated the guest");

	}

	public String getOnlineOrderModeDeatils() throws InterruptedException {
		String orderMode = "";
		utils.longWaitInSeconds(2);
		WebElement onlineOrderCam = driver.findElement(locators.get("guestTimeLine.onlineOrderCam"));
		utils.scrollToElement(driver, onlineOrderCam);
		onlineOrderCam.isDisplayed();
		utils.clickByJSExecutor(driver, onlineOrderCam);
//		driver.findElement(guestTimelineLocators.get("onlineOrderCam")).click();
		Thread.sleep(3000);
		WebElement ordermode = driver.findElement(locators.get("guestTimeLine.ordermode"));
		orderMode = ordermode.getText();
		return orderMode;
	}

	// Author- Raja
	public boolean ProfileUpdatesforMenuSync(String updatedFname, String updateLname, String updatedEmail) {
		try {

			driver.findElement(locators.get("guestTimeLine.firstnametextbox")).clear();
			WebElement firstnametextbox = driver.findElement(locators.get("guestTimeLine.firstnametextbox"));
			firstnametextbox.sendKeys(updatedFname);
			driver.findElement(locators.get("guestTimeLine.lastnametextbox")).clear();
			WebElement lastnametextbox = driver.findElement(locators.get("guestTimeLine.lastnametextbox"));
			lastnametextbox.sendKeys(updateLname);
			driver.findElement(locators.get("guestTimeLine.emailtextbox")).clear();
			WebElement emailtextbox = driver.findElement(locators.get("guestTimeLine.emailtextbox"));
			emailtextbox.sendKeys(updatedEmail);
			// driver.findElement(guestTimelineLocators.get("birthyearDropdown")).click();
			// driver.findElement(guestTimelineLocators.get("birthyearDropdownwith2000")).click();
			WebElement updateEditProfile = driver.findElement(locators.get("guestTimeLine.updateEditProfile"));
			updateEditProfile.click();
			// utils.longwait(50000);
			WebElement firstnametextboxForGet = driver.findElement(locators.get("guestTimeLine.firstnametextbox"));
			String FN = firstnametextboxForGet.getText();

			System.out.println("updated name is" + FN);

			return true;
		} catch (Exception e) {
			logger.error("Error in verifying guest time line " + e);
			TestListeners.extentTest.get().fail("Error in verifying guest time line " + e);
		}
		return false;
	}

	public boolean verifyItemInReceiptImage(String receipt_type, String item) {
		WebElement element = null;
		switch (receipt_type) {
		case "redemption":
			element = driver.findElement(locators.get("guestTimeLine.receiptCamRedemption"));
			break;
		case "checkin":
			element = driver.findElement(locators.get("guestTimeLine.receiptCamCheckins"));
			break;
		default:
			throw new IllegalArgumentException("Invalid receipt type: " + receipt_type);
		}

		// Scroll and click
		utils.scrollToElement(driver, element);
		utils.clickByJSExecutor(driver, element);

		utils.longWaitInSeconds(1);
		logger.info("Clicked on receipt cam");
		TestListeners.extentTest.get().info("Clicked on receipt cam");

		List<WebElement> rows = driver.findElements(locators.get("guestTimeLine.receiptCamImageItemsTable"));
		boolean isItemFound = false;

		for (WebElement row : rows) {
			List<WebElement> cells = row.findElements(By.tagName("td"));
			for (WebElement cell : cells) {
				// Check if the desired text is present
				if (cell.getText().contains(item)) {
					isItemFound = true;
					break;
				}
			}
			if (isItemFound) {
				break;
			}
		}
		if (isItemFound) {
			logger.info("Item  found in receipt");
			TestListeners.extentTest.get().info("Item found in receipt");
		} else {
			logger.info("Item not found in receipt");
			TestListeners.extentTest.get().info("Item not found in receipt");
		}
		return isItemFound;
	}

	public String getClientPlatformDeatils() throws InterruptedException {
		String clientPlatform = "";
//		utils.waitTillPagePaceDone();
		selUtils.longWait(3000);
		WebElement onlineOrderCam = driver.findElement(locators.get("guestTimeLine.onlineOrderCam"));
		utils.scrollToElement(driver, onlineOrderCam);
		utils.waitTillElementToBeClickable(onlineOrderCam);
		onlineOrderCam.isDisplayed();
		// driver.findElement(guestTimelineLocators.get("onlineOrderCam")).click();
		utils.StaleElementclick(driver, onlineOrderCam);
		Thread.sleep(2000);
		WebElement clientPlatformEl = driver.findElement(locators.get("guestTimeLine.clientPlatform"));
		clientPlatform = clientPlatformEl.getText();
		return clientPlatform;
	}

	public boolean verifyPostCheckinIsDispayedOnTimeLine() throws InterruptedException {
		boolean result = false;
		int attempts = 0;
		while (attempts < 10) {
			utils.refreshPage();
			System.out.println("Page referesh count " + attempts);
			Thread.sleep(2000);
			try {
				selUtils.implicitWait(3);
				List<WebElement> list = driver.findElements(locators.get("guestTimeLine.approvedLoyalityLabel"));
				if (list.size() != 0) {
					result = true;
					logger.info("approvedLoyalityLabel is present");
					break;
				}
			} catch (Exception e) {
				logger.info("Error in navigating to web checkin reciept icon" + e);
			}
			selUtils.implicitWait(50);

			attempts++;
		}
		return result;
	}

	public double getAutoCheckinAmountOnTimeLinePage() {
		double actAmount = Double.parseDouble(driver
				.findElement(By.xpath(
						"//div[h4[div[contains(text(),'Approved Loyalty')]]]/following-sibling::div[1]/div[1]/h3/span"))
				.getText().replace("($", "").replace(")", ""));
		System.out.println("actAmount=" + actAmount);
		return actAmount;

	}

	public boolean verifyReceiptTagIsDisplayedForAutoCheckin(String expReceiptTagName) throws InterruptedException {
		boolean result = false;
		int attempts = 0;
		while (attempts < 5) {
			refreshTimeline();
			Thread.sleep(5000);
			try {
				List<WebElement> weleTagNameList = driver.findElements(
						By.xpath("//div[contains(text(),'Approved Loyalty')]/a[text()='" + expReceiptTagName + "']"));
				if (weleTagNameList.size() != 0) {
					result = true;
					logger.info(expReceiptTagName + " tag is present");
					break;
				}
			} catch (Exception e) {
				logger.info(attempts + "= tag " + expReceiptTagName + " is not present");
			}
			attempts++;
		}
		return result;
	}

	public boolean verifyPhoneNumber(String mail) {
		try {
			WebElement editProfileTabLink = driver.findElement(locators.get("guestTimeLine.editProfileTabLink"));
			editProfileTabLink.isDisplayed();
			editProfileTabLink.click();
			driver.findElement(locators.get("guestTimeLine.editphoneNumber")).clear();
			WebElement updateEditProfile = driver.findElement(locators.get("guestTimeLine.updateEditProfile"));
			updateEditProfile.click();
			WebElement editProfileLabel = driver.findElement(locators.get("guestTimeLine.editProfileLabel"));
			editProfileLabel.isDisplayed();
			logger.info("Empty phone number field updated successfully");
			TestListeners.extentTest.get().pass("Empty phone number field updated successfully");
			WebElement editProfileTabLink2 = driver.findElement(locators.get("guestTimeLine.editProfileTabLink"));
			editProfileTabLink2.isDisplayed();
			editProfileTabLink2.click();
			WebElement editphoneNumber = driver.findElement(locators.get("guestTimeLine.editphoneNumber"));
			editphoneNumber.sendKeys("123");
			WebElement updateEditProfile2 = driver.findElement(locators.get("guestTimeLine.updateEditProfile"));
			updateEditProfile2.click();
			WebElement editProfileLabel2 = driver.findElement(locators.get("guestTimeLine.editProfileLabel"));
			editProfileLabel2.isDisplayed();
			WebElement phoneNumberLabel = driver.findElement(locators.get("guestTimeLine.phoneNumberLabel"));
			phoneNumberLabel.isDisplayed();
			String phoneresponse = phoneNumberLabel.getText();
			Assert.assertEquals(phoneresponse, "123", "Phone number 123 did not match");
			// driver.findElement(guestTimelineLocators.get("phoneNumberLabel")).isDisplayed();
			// driver.findElement(guestTimelineLocators.get("phoneNumberLabel")).getText();
			logger.info("phone number field updated successfully");
			TestListeners.extentTest.get().pass("phone number field updated successfully");
			WebElement editProfileTabLink3 = driver.findElement(locators.get("guestTimeLine.editProfileTabLink"));
			editProfileTabLink3.isDisplayed();
			editProfileTabLink3.click();
			WebElement editphoneNumber2 = driver.findElement(locators.get("guestTimeLine.editphoneNumber"));
			editphoneNumber2.isDisplayed();
			editphoneNumber2.clear();
			WebElement editphoneNumberForSendKeys = driver.findElement(locators.get("guestTimeLine.editphoneNumber"));
			editphoneNumberForSendKeys.sendKeys("1-2-3");
			WebElement updateEditProfile3 = driver.findElement(locators.get("guestTimeLine.updateEditProfile"));
			updateEditProfile3.click();
			WebElement editProfileLabel3 = driver.findElement(locators.get("guestTimeLine.editProfileLabel"));
			editProfileLabel3.isDisplayed();
			WebElement phoneNumberLabel2 = driver.findElement(locators.get("guestTimeLine.phoneNumberLabel"));
			phoneNumberLabel2.isDisplayed();
			String phoneresponse1 = phoneNumberLabel2.getText();
			Assert.assertEquals(phoneresponse1, "123", "Phone number 123 did not match");
			logger.info("phone number field updated successfully");

		} catch (Exception e) {
			logger.error("Error in updating the phone number" + e);
			TestListeners.extentTest.get().fail("Error in updating the phone number" + e);
		}
		return false;
	}

	public boolean verifyIsCampaignExistOnTimeLine(String cname) throws InterruptedException {
		// String campName = "";
		boolean flag = false;
		int attempts = 0;
		while (attempts < 25) {
			refreshTimeline();
			utils.longWaitInSeconds(5);
			try {
				String strXpath = utils.getLocatorValue("guestTimeLine.campaignNameCampaign");
				List<WebElement> eleList = driver.findElements(By.xpath(strXpath));
				for (WebElement wEle : eleList) {
					String uiCmpName = wEle.getText();
					String[] arrValue = uiCmpName.split("Campaign Name:");
					if (arrValue[1].trim().equals(cname)) {
						flag = true;
						logger.info(cname + " campaign name is visible on timeline. ");
						TestListeners.extentTest.get().info(cname + " campaign name is visible on timeline. ");
						return flag;
					}
				}

			} catch (Exception e) {
				flag = false;
				logger.info(cname + " campaign name is not visible on timeline.. ");
				utils.refreshPage();

			}
			attempts++;
			// refreshTimeline();
		}

		if (!flag) {
			logger.info(cname + " Element is not present or Campaign Name did not matched");
			TestListeners.extentTest.get().info(cname + " Element is not present or Campaign Name did not matched");
		}
		return flag;
	}

	public boolean verifyCouponCampaignNotification() throws InterruptedException {
		boolean result = false;
		int attempts = 0;
		while (attempts < 2) {
			refreshTimeline();
			Thread.sleep(1000);
			try {
				WebElement campaignNotification = driver
						.findElement(locators.get("guestTimeLine.couponCampaignNotification"));
				if (campaignNotification.isDisplayed()) {
					result = true;
					logger.info("Element:" + campaignNotification + " is present");
					break;
				}
			} catch (Exception e) {
				logger.info("Element is not present ");
			}
			attempts++;
		}
		WebElement couponCampaignNotification = driver
				.findElement(locators.get("guestTimeLine.couponCampaignNotification"));
		couponCampaignNotification.isDisplayed();
		return result;
	}

	public void deactivateReactivateUser(String optionName) throws InterruptedException {
		clickOnGuestFunctionsOptions(optionName);
		selUtils.implicitWait(10);
		driver.switchTo().alert().accept();

		if (optionName.equalsIgnoreCase("Deactivate")) {
			WebElement deactivateReason = driver.findElement(locators.get("guestTimeLine.deactivateReason"));
			utils.waitTillVisibilityOfElement(deactivateReason, "delete reason");
			deactivateReason.sendKeys("Automation");
			WebElement deactivateSubmitBtn = driver.findElement(locators.get("guestTimeLine.deactivateSubmitBtn"));
			deactivateSubmitBtn.click();
			String expectedSuccessMessage = "The guest has been deactivated. Please refresh the page to view updated details of the guest profile.";
			WebElement successMessageWele = driver
					.findElement(locators.get("guestTimeLine.deactivateUserSuccessMessage"));
			utils.waitTillVisibilityOfElement(successMessageWele, "Success Message");
			String successMessage = successMessageWele.getText();
			Assert.assertEquals(successMessage, expectedSuccessMessage);
			logger.info("User is deactivated successfully");
			TestListeners.extentTest.get().pass("User is deactivated successfully");
		} else if (optionName.equalsIgnoreCase("Reactivate")) {
			String expecteSucessMsgReactivate = "The guest has been reactivated";
			WebElement successMessageWele = driver
					.findElement(locators.get("guestTimeLine.deactivateUserSuccessMessage"));
			utils.waitTillVisibilityOfElement(successMessageWele, "Success Message");
			String successMessage = successMessageWele.getText();
			Assert.assertEquals(successMessage, expecteSucessMsgReactivate);
			logger.info("User is deactivated successfully");
			TestListeners.extentTest.get().pass("User is deactivated successfully");
		}
		// selUtils.implicitWait(50);

	}

	// verify the campaign notification on timeline for any campaign
	public boolean verifyCampaginOrSystemNotificationIsDisplayed(String campName, boolean status) {
		boolean flag = false;
		if ((campName != null) || (!campName.equalsIgnoreCase("")) && (status == true)) {
			WebElement wEleNoticagtion = driver.findElement(By.xpath(
					"//div[div[contains(text(),'Campaign Name: " + campName + "')]]/preceding-sibling::div[2]/h4/div"));
			String text = wEleNoticagtion.getText();
			if (text.contains("Campaign Notification") || text.contains("System Notification")) {
				logger.info("Campaign/System Notification is displayed on Timeline page for the campaign " + campName);
				flag = true;
			} else {
				flag = false;
			}
		}
		return flag;
	}

	public boolean verifyPushNotificationIsDisplayed(String campName, String expPushNotificationMessage,
			boolean status) {
		boolean flag = false;
		if ((campName != null) || (!campName.equalsIgnoreCase("")) && (status == true)) {
			WebElement wEleNoticagtion = driver.findElement(By.xpath(
					"//div[div[contains(text(),'Campaign Name: " + campName + "')]]/preceding-sibling::div[1]/span"));
			String text = wEleNoticagtion.getText();
			if (text.contains(expPushNotificationMessage)) {
				flag = true;
				System.out.println(expPushNotificationMessage + " is displayed on Timeline page ");
			} else {
				System.out.println(expPushNotificationMessage + " is NOT displayed on Timeline page ");
				flag = false;
			}
		}
		return flag;
	}

	// Add note for doing ban user
	public void addNoteAndBanGuest() {
		selUtils.implicitWait(10);
		driver.findElement(locators.get("guestTimeLine.userNoteCommentBox")).clear();
		WebElement userNoteCommentBox = driver.findElement(locators.get("guestTimeLine.userNoteCommentBox"));
		userNoteCommentBox.sendKeys("Banned user for doing illegal ");

		WebElement banGuestCheckBox = driver.findElement(locators.get("guestTimeLine.banGuestCheckBox"));
		utils.clickByJSExecutor(driver, banGuestCheckBox);

		WebElement createUserNoteButton = driver.findElement(locators.get("guestTimeLine.createUserNoteButton"));
		createUserNoteButton.click();
		selUtils.longWait(5);
		WebElement successMessageWele = driver.findElement(locators.get("guestTimeLine.deactivateUserSuccessMessage"));
		utils.waitTillVisibilityOfElement(successMessageWele, "Success Message");

		String successMessage = successMessageWele.getText();
		Assert.assertEquals(successMessage, "Comment Saved");

		selUtils.implicitWait(50);

	}

	public void clickOnTopSuspectsTabInFraudSuspectPage() {

		WebElement topSuspectsTabButton = driver.findElement(locators.get("guestTimeLine.topSuspectsTabButton"));
		topSuspectsTabButton.click();
	}

	// Ban User
	public boolean verifyBanGuestIsVisilble(String guestEmailId) {
		boolean userIsExist = false;
		boolean counterFlag = false;
		utils.implicitWait(10);
		List<WebElement> mainList = driver.findElements(By.xpath("//ul[@class='pagination']/li"));
		int counter = 0;
		if (mainList.size() != 0) {

			do {
				logger.info("Banned user searching :: " + counter + " ---- " + guestEmailId);
				TestListeners.extentTest.get().info("Banned user searching :: " + counter + " ---- " + guestEmailId);

				// user search in loop and then click on next
				userIsExist = userSearchInFraudSusupectBannedUser(guestEmailId);
				if (userIsExist) {
					return userIsExist;
				}

				List<WebElement> mainListSub = driver.findElements(By.xpath("//ul[@class='pagination']/li"));

				if (mainListSub.get(mainListSub.size() - 1).getAttribute("class").equalsIgnoreCase("next")) {
					mainListSub.get(mainListSub.size() - 1).click();
				} else {
					logger.info("You are on the last page and no next button is available ");
					TestListeners.extentTest.get().info("You are on the last page and no next button is available ");
					counterFlag = true; // means you are on the last page and no further click on next button and need
										// to stop
				}
				counter++;
			} while ((userIsExist == false) && (counterFlag == false));

		} else {
			userIsExist = userSearchInFraudSusupectBannedUser(guestEmailId);
			logger.info("Pagination is not there , list size is ZERO ");
			TestListeners.extentTest.get().info("Pagination is not there , list size is ZERO ");
		}

		selUtils.implicitWait(50);
		return userIsExist;
	}

	public void selectDeactivatedFilter(String valueToBeSelect) {
		WebElement dropDownWebEle = driver.findElement(locators.get("guestTimeLine.deactivateFilterDropdown"));
		utils.selectDrpDwnValue(dropDownWebEle, valueToBeSelect);
	}

	public void clickOnDownloadApplePassButton() {
		WebElement downloadApplePass = driver.findElement(locators.get("guestTimeLine.downloadApplePass"));
		utils.waitTillVisibilityOfElement(downloadApplePass, "'Download Apple Pass' Button");
		downloadApplePass.click();
	}

	public String getApplePassUrl() {
		WebElement downloadApplePass = driver.findElement(locators.get("guestTimeLine.downloadApplePass"));
		String applePassUrl = downloadApplePass.getAttribute("href");
		logger.info("Apple Pass URL is : " + applePassUrl);
		return applePassUrl;
	}

	public void unzipTheApplePassZipFile(String zipFilePath, String destDir) {
		utils.unzip(zipFilePath, destDir);

	}

	/*
	 * public int timeDiffCampTrigger() throws ParseException {
	 * utils.refreshPageWithCurrentUrl(); List<WebElement> timeList =
	 * driver.findElements(guestTimelineLocators.get("eventTime")); List<String>
	 * timestamp = timeList.stream().map(s ->
	 * s.getText().trim()).collect(Collectors.toList()); String[] time1 =
	 * timestamp.get(0).split(":"); int val1 =
	 * Integer.parseInt(time1[1].replaceAll("[^0-9]", "")); String[] time2 =
	 * timestamp.get(timestamp.size() - 1).split(":"); int val2 =
	 * Integer.parseInt(time2[1].replaceAll("[^0-9]", "")); int diffMinutes = val1 -
	 * val2; return diffMinutes; }
	 */

	public int timeDiffCampTrigger() throws ParseException {
		utils.refreshPageWithCurrentUrl();
		List<WebElement> timeList = driver.findElements(locators.get("guestTimeLine.eventTime"));
		List<String> timestamp = timeList.stream().map(s -> s.getText().trim()).collect(Collectors.toList());
		String[] time1 = timestamp.get(0).split(" ");
		String val1 = time1[3] + ":00";
		LocalTime t1 = LocalTime.parse(val1);

		String[] time2 = timestamp.get(timestamp.size() - 1).split(" ");
		String val2 = time2[3] + ":00";
		LocalTime t2 = LocalTime.parse(val2);
		Duration diff = Duration.between(t2, t1);
		long timeDiff = diff.toMinutes();
		int tDiff = (int) timeDiff;
		logger.info("Time difference between campaign trigger and last event is: " + tDiff + " minutes");
		TestListeners.extentTest.get()
				.info("Time difference between campaign trigger and last event is: " + tDiff + " minutes");
		return tDiff;
	}

	public int getTimeDiffPostRedemptionCampaign() {
		utils.refreshPageWithCurrentUrl();
		List<WebElement> timeList = driver.findElements(locators.get("guestTimeLine.eventTime"));
		List<String> timestamp = timeList.stream().map(s -> s.getText().trim()).collect(Collectors.toList());
		String[] time1 = timestamp.get(0).split(" "); // cam trigger time
		String val1 = time1[3] + ":00";
		LocalTime t1 = LocalTime.parse(val1);

		WebElement redemptionTime = driver.findElement(locators.get("guestTimeLine.redemptionTime"));
		String eventTime = redemptionTime.getText();
		String[] time2 = eventTime.split(" ");
		String val2 = time2[3] + ":00";
		LocalTime t2 = LocalTime.parse(val2);
		Duration diff = Duration.between(t2, t1);
		long timeDiff = diff.toMinutes();
		int tDiff = (int) timeDiff;
		return tDiff;
	}

	public void checkUncheckFilterEvents(String filterName, String desiredAction) {
		// Expand the filter events section
		String filterEventsLinkXpath = utils.getLocatorValue("guestTimeLine.filterEvents").replace("$flag",
				"Filter Events");
		WebElement filterEventsLink = driver.findElement(By.xpath(filterEventsLinkXpath));
		filterEventsLink.click();

		// Check status of filter event
		String filterEventsStatusXpath = utils.getLocatorValue("guestTimeLine.filterEventsStatus").replace("$flag",
				filterName);
		WebElement filterEventsCheckStatus = driver.findElement(By.xpath(filterEventsStatusXpath));
		String isFilterEventChecked = filterEventsCheckStatus.getAttribute("checked");

		// Specific filter event
		String filterEventXpath = utils.getLocatorValue("guestTimeLine.filterEvents").replace("$flag", filterName);
		WebElement filterEvent = driver.findElement(By.xpath(filterEventXpath));

		// Submit button for filter events
		WebElement filterEventSubmit = driver.findElement(locators.get("guestTimeLine.submitFilterEvents"));

		/*
		 * Check or uncheck the filter event based on the desired action. If the filter
		 * event status and the desired action matches, collapse the filter section. If
		 * the filter event status and the desired action does not match, click on the
		 * filter event and submit.
		 */
		if ((isFilterEventChecked == null) && (desiredAction.equalsIgnoreCase("uncheck"))) {
			logger.info(
					filterName + " filter event is unchecked and user also want to uncheck. Therefore, did not click.");
			TestListeners.extentTest.get().info(
					filterName + " filter event is unchecked and user also want to uncheck. Therefore, did not click.");
			filterEventsLink.click();
		} else if ((isFilterEventChecked == null) && (desiredAction.equalsIgnoreCase("check"))) {
			utils.clickByJSExecutor(driver, filterEvent);
			logger.info(filterName + " filter event is unchecked and user want to check. Therefore, clicked it.");
			TestListeners.extentTest.get()
					.info(filterName + " filter event is unchecked and user want to check. Therefore, clicked it.");
			filterEventSubmit.click();
		} else if (isFilterEventChecked.equalsIgnoreCase("true") && (desiredAction.equalsIgnoreCase("uncheck"))) {
			utils.clickByJSExecutor(driver, filterEvent);
			logger.info(filterName + " filter event is checked and user want to uncheck. Therefore, clicked it.");
			TestListeners.extentTest.get()
					.info(filterName + " filter event is checked and user want to uncheck. Therefore, clicked it.");
			filterEventSubmit.click();
		} else if (isFilterEventChecked.equalsIgnoreCase("true") && (desiredAction.equalsIgnoreCase("check"))) {
			logger.info(filterName + " filter event is checked and user also want to check. Therefore, did not click.");
			TestListeners.extentTest.get().info(
					filterName + " filter event is checked and user also want to check. Therefore, did not click.");
			filterEventsLink.click();
		}
	}

	public boolean verifyRedemptionFailure() throws InterruptedException {
		boolean result = false;
		int attempts = 0;
		while (attempts < 5) {
			refreshTimeline();
			Thread.sleep(2000);
			try {
				WebElement redemptionFailure = driver.findElement(locators.get("guestTimeLine.redemptionFailure"));
				if (redemptionFailure.isDisplayed()) {
					result = true;
					logger.info("Element:" + redemptionFailure + " title is present");
					break;
				}
			} catch (Exception e) {
				logger.info("Element is not present ");
			}
			attempts++;
		}
		WebElement redemptionFailure = driver.findElement(locators.get("guestTimeLine.redemptionFailure"));
		redemptionFailure.isDisplayed();
		return result;
	}

	public boolean lockedAccountTab(int count) throws InterruptedException {
		utils.waitTillPagePaceDone();
		boolean flag = false;
		int attempts = 0;
		while (attempts < count) {
			pageObj.menupage().navigateToSubMenuItem("Guests", "Guests");
			try {
				utils.waitTillPagePaceDone();
				selUtils.implicitWait(10);
				WebElement lockedAccountTab = driver.findElement(locators.get("guestTimeLine.lockedAccountTab"));
				lockedAccountTab.isDisplayed();
				logger.info("Locked Account Tab is visible");
				TestListeners.extentTest.get().info("Locked Account Tab is visible");
				selUtils.implicitWait(50);
				flag = true;
				return flag;
			} catch (Exception e) {
				// e.printStackTrace();
				selUtils.implicitWait(50);
				logger.info("Locked Account Tab is not visible in Guest section. Attemp " + attempts);
				TestListeners.extentTest.get()
						.info("Locked Account Tab is not visible in Guest section. Attemp " + attempts);
			}
			attempts++;
		}
		logger.info("Locked Account Tab visiblity result - " + flag);
		TestListeners.extentTest.get().info("Locked Account Tab visiblity result - " + flag);
		return flag;
	}

	public void navigateToLockedAccountTab() {
		selUtils.implicitWait(15);
		WebElement lockedAccountTab = driver.findElement(locators.get("guestTimeLine.lockedAccountTab"));
		utils.clickByJSExecutor(driver, lockedAccountTab);
		// driver.findElement(guestTimelineLocators.get("lockedAccountTab")).click();
		logger.info("Locked Account Tab is clicked");
		TestListeners.extentTest.get().pass("Locked Account Tab is clicked");
		selUtils.implicitWait(50);
	}

	public boolean searchLockedAccountTab(String text, int noOfAttempts) throws InterruptedException {
		int attempts = 0;
		boolean status = false;
		while (attempts < noOfAttempts) {
//			utils.refreshPageWithCurrentUrl();
			utils.waitTillPagePaceDone();
			selUtils.implicitWait(5);
			WebElement wele = driver.findElement(locators.get("guestTimeLine.searchBox"));
			utils.StaleElementclick(driver, wele);
			wele.clear();
			wele.sendKeys(text);
			wele.sendKeys(Keys.ENTER);
			try {
				WebElement ele = driver.findElement(locators.get("guestTimeLine.guestName"));
				status = utils.checkElementPresent(ele);
				if (status) {
					logger.info("Searched locked account user is present");
					TestListeners.extentTest.get().pass("Searched locked account user is present");
					break;
				}

			} catch (Exception e) {
				pageObj.menupage().navigateToSubMenuItem("Guests", "Guests");
				navigateToLockedAccountTab();
			}
			attempts++;
		}
		selUtils.implicitWait(50);
		return status;
	}

	public boolean checkSearchOption() {
		boolean status = false;
		try {
			WebElement ele = driver.findElement(locators.get("guestTimeLine.searchBox"));
			status = utils.checkElementPresent(ele);
		} catch (Exception e) {

		}
		return status;
	}

	public void unlockOrCancelLockedAccount(String text, String queryParam) {
		utils.waitTillPagePaceDone();
		driver.findElement(locators.get("guestTimeLine.searchBox")).clear();
		WebElement searchBox = driver.findElement(locators.get("guestTimeLine.searchBox"));
		searchBox.sendKeys(text);
		searchBox.sendKeys(Keys.ENTER);
		WebElement clickUnlockButtonForUnlockAccout = driver
				.findElement(locators.get("guestTimeLine.clickUnlockButtonForUnlockAccout"));
		utils.waitTillElementToBeClickable(clickUnlockButtonForUnlockAccout);
		clickUnlockButtonForUnlockAccout.click();
		switch (queryParam) {
		case "cancel":
			driver.switchTo().alert().dismiss();
			logger.info("Searched locked account user is canceled");
			TestListeners.extentTest.get().pass("Searched locked account user is canceled");
			break;
		case "ok":
			driver.switchTo().alert().accept();
			logger.info("Searched locked account user is unlocked successfully");
			TestListeners.extentTest.get().pass("Searched locked account user is unlocked successfully");
			break;

		}
	}

	public boolean successOrErrorConfirmationMessage(String message) throws InterruptedException {
		boolean flag = false;
		try {
			String xpath = utils.getLocatorValue("guestTimeLine.confirmationMessage").replace("$message", message);
			WebElement confirmationMessage = driver.findElement(By.xpath(xpath));
			String result = confirmationMessage.getText();
			if (message.contains(result)) {
				flag = true;
			}
		} catch (Exception e) {
			flag = false;
		}
		return flag;
	}

	public String verifyUserGetRedeemable(String redeemableName) throws InterruptedException {
		String redeemableName1 = "";
		boolean flag = false;
		int attempts = 0;
		while (attempts < 10) {
			refreshTimeline();
			try {
				WebElement redeemableNameEl = driver.findElement(locators.get("guestTimeLine.redeemableName"));
				String str = redeemableNameEl.getText();
				String[] name = str.split("Rewarded ");
				redeemableName1 = name[1].trim();
				if (redeemableName1.equalsIgnoreCase(redeemableName)) {
					flag = true;
					logger.info("Redeemable Name " + redeemableName + " matched on the timeline");
					TestListeners.extentTest.get()
							.pass("Redeemable Name " + redeemableName + " matched on the timeline");
					break;
				}
			} catch (Exception e) {
				logger.info("Element is not present or Redeemable Name did not matched");
			}
			attempts++;
		}
		return redeemableName1;

	}

	public void createAndCleanDownloadBrowserDownloadFolder(String downloadFilePath) throws IOException {

		File directoryDownloadFolderFile = new File(downloadFilePath);
		if (directoryDownloadFolderFile.exists()) {
			logger.info(directoryDownloadFolderFile + " is already exist");
			TestListeners.extentTest.get().pass(directoryDownloadFolderFile + " is already exist");
			FileUtils.cleanDirectory(directoryDownloadFolderFile);
		} else {
			directoryDownloadFolderFile.mkdir();
			logger.info(directoryDownloadFolderFile + " is created successfuly");
			TestListeners.extentTest.get().pass(directoryDownloadFolderFile + " is created successfully");
		}
	}

	public boolean verifyApprovedReciptTagIsDisplayedOnTimeLine(String receiptTagName) {
		int attempts = 0;
		boolean flag = false;
		while (attempts < 25) {
			try {
				List<WebElement> reciepttagLoyaltyList = driver
						.findElements(locators.get("guestTimeLine.ApprovedReciptTagList"));
				if (reciepttagLoyaltyList.size() != 0) {
					for (WebElement wEle : reciepttagLoyaltyList) {
						String actualReceiptTagName = wEle.getText();
						if (actualReceiptTagName.equalsIgnoreCase(receiptTagName)) {
							System.out.println(receiptTagName + " is displayed on timeline page ");
							flag = true;
							break;
						}
					}
				}
			} catch (Exception e) {
				logger.info("Reciept tag is not present in loyalty notification and side bar tags panel");
				TestListeners.extentTest.get().pass(
						"Reciept tag is not displayed successfully in loyalty notification and side bar tags panel:"
								+ receiptTagName);
			}
			attempts++;
			if (flag) {
				break;
			} else {
				refreshTimeline();
			}
		}
		return flag;
	}

	public boolean verifyRecieptTagPanelTagIsDisplayedOnTimeLine(String receiptTagName) {
		int attempts = 0;
		boolean flag = false;
		while (attempts < 25) {
			try {
				List<WebElement> reciepttagLoyaltyList = driver
						.findElements(locators.get("guestTimeLine.RecieptTagPanelList"));
				for (WebElement wEle : reciepttagLoyaltyList) {
					String actualReceiptTagName = wEle.getText();
					if (actualReceiptTagName.equalsIgnoreCase(receiptTagName)) {
						System.out.println(receiptTagName + " is displayed on timeline page ");
						flag = true;
						break;
					}
				}
			} catch (Exception e) {
				logger.info("Reciept tag is not present in loyalty notification and side bar tags panel");
				TestListeners.extentTest.get().pass(
						"Reciept tag is not displayed successfully in loyalty notification and side bar tags panel:"
								+ receiptTagName);
			}
			attempts++;
			if (flag) {
				break;
			} else {
				refreshTimeline();
			}
		}
		return flag;
	}

	// used for transaction id for OLO barcode checkin
	public String getPOSTransactionIDForBarcode(String str) {
		str = str.replace("Approved Loyalty ", "");
		String[] strArr = str.split(" ");
		String checkinBarcode = strArr[0];
		String finalString = checkinBarcode.substring(8, 23);

		logger.info("POS transaction id for barcode checkin is " + finalString);
		TestListeners.extentTest.get().pass("POS transaction id for barcode checkin is " + finalString);
		return finalString;

	}

	public void verifyApprovedReceiptImageCheckin(String checkinId) {
		boolean checkinFlag = false;
		WebElement checkApprovedToolTip = driver.findElement(locators.get("guestTimeLine.checkApprovedToolTip"));
		boolean flag = checkApprovedToolTip.isDisplayed();
		if (flag == true) {
			WebElement approvedTooltipMsg = driver.findElement(locators.get("guestTimeLine.approvedTooltipMsg"));
			String getCheckinId = approvedTooltipMsg.getText();
			checkinFlag = utils.textContains(getCheckinId, checkinId);
		}
		Assert.assertTrue(checkinFlag, "Receipt approve Checkin ID verification failed.");
		logger.info("Receipt approve Checkin ID verified.");
		TestListeners.extentTest.get().pass("Receipt approve Checkin ID verified.");
	}

	// verify receipt image checkin disapprove with checkinId
	public void verifyDisapprovedReceiptImageCheckin(String checkinId, String reason) {
		boolean checkinFlag = false;
		boolean reasonFlag = false;
		WebElement checkDisapprovedToolTip = driver.findElement(locators.get("guestTimeLine.checkDisapprovedToolTip"));
		boolean flag = checkDisapprovedToolTip.isDisplayed();
		if (flag == true) {
			WebElement disapprovedTooltipMsg = driver.findElement(locators.get("guestTimeLine.disapprovedTooltipMsg"));
			String getCheckinId = disapprovedTooltipMsg.getText();
			checkinFlag = utils.textContains(getCheckinId, checkinId);
			utils.waitTillElementToBeClickable(checkDisapprovedToolTip);
		}
		Assert.assertTrue(checkinFlag, "Receipt disapproved Checkin ID verification failed.");
		logger.info("Receipt approve Checkin ID verified.");
		TestListeners.extentTest.get().pass("Receipt approve Checkin ID verified.");
	}

	public void paginationToTillLastPage() throws InterruptedException {
		boolean flag = false;
		do {
			selUtils.implicitWait(2);
			try {
				List<WebElement> mainList = driver.findElements(By.xpath("//ul[@class='pagination']/li"));
				if (mainList.get(mainList.size() - 1).getAttribute("class").equalsIgnoreCase("next")
						&& mainList.get(mainList.size() - 2).getAttribute("class").equalsIgnoreCase("disabled")) {
					flag = false;
					String pageNumber = mainList.get(mainList.size() - 3).getText();
					WebElement pageLink = driver.findElement(By.linkText(pageNumber));
					pageLink.click();

				} else if (mainList.get(mainList.size() - 1).getAttribute("class").equalsIgnoreCase("next")
						&& !mainList.get(mainList.size() - 2).getAttribute("class").equalsIgnoreCase("disabled")) {
					flag = true;
					String pageNumber = mainList.get(mainList.size() - 2).getText();
					WebElement pageLink = driver.findElement(By.linkText(pageNumber));
					pageLink.click();
					break;

				} else if (!mainList.get(mainList.size() - 1).getAttribute("class").equalsIgnoreCase("next")
						&& !mainList.get(mainList.size() - 2).getAttribute("class").equalsIgnoreCase("disabled")) {
					flag = true;
					String pageNumber = mainList.get(mainList.size() - 1).getText();
					WebElement pageLink = driver.findElement(By.linkText(pageNumber));
					pageLink.click();
					break;
				}
			} catch (Exception e) {
				logger.info("Next Button is not available now for clicking . ");
				TestListeners.extentTest.get().pass("Next Button is not available now for clicking . ");
				flag = false;
				break;
			}

		} while (flag == false);

	}

	public void navigateToAddNotTab() {
		WebElement AddNoteTab = driver.findElement(locators.get("guestTimeLine.AddNoteTab"));
		AddNoteTab.click();
		selUtils.longWait(200);

	}

	public void performGuestFunctions(String optionVal, String deleteReason) {
		clickEditProfile();
		WebElement guestFunctionsbtn = driver.findElement(locators.get("guestTimeLine.guestFunctionsbtn"));
		guestFunctionsbtn.click();
		List<WebElement> guestFunctionsOptins = driver.findElements(locators.get("guestTimeLine.guestFunctionsOptins"));
		utils.selectListDrpDwnValue(guestFunctionsOptins, optionVal);
		if (optionVal.equals("Delete/Anonymize Guest?")) {
			WebElement chooseReasonDrp = driver.findElement(locators.get("guestTimeLine.chooseReasonDrp"));
			chooseReasonDrp.click();
			List<WebElement> chooseReasonDrpOptions = driver
					.findElements(locators.get("guestTimeLine.chooseReasonDrpOptions"));
			utils.selectListDrpDwnValue(chooseReasonDrpOptions, deleteReason);
			WebElement submitBtn = driver.findElement(locators.get("guestTimeLine.submitBtn"));
			submitBtn.click();
			utils.acceptAlert(driver);
		} else {
			utils.acceptAlert(driver);
			WebElement deactivateReason = driver.findElement(locators.get("guestTimeLine.deactivateReason"));
			utils.waitTillVisibilityOfElement(deactivateReason, "delete reason");
			deactivateReason.sendKeys("Automation");
			WebElement deactivateSubmitBtn = driver.findElement(locators.get("guestTimeLine.deactivateSubmitBtn"));
			deactivateSubmitBtn.click();
		}
	}

	public boolean checkDeativatepermissions() {
		boolean status = false;
		clickEditProfile();
		WebElement guestFunctionsbtn = driver.findElement(locators.get("guestTimeLine.guestFunctionsbtn"));
		guestFunctionsbtn.click();
		try {
			utils.implicitWait(3);
			WebElement ele = driver.findElement(By.xpath("//span[text()='Deactivate']"));
			status = utils.checkElementPresent(ele);
		} catch (Exception e) {

		}
		return status;
	}

	public String deactivationStatus() {
		WebElement deactivatedlabel = driver.findElement(locators.get("guestTimeLine.deactivatedlabel"));
		String val = deactivatedlabel.getText().trim();
		return val;
	}

	public String delationStatus() {
		utils.waitTillPagePaceDone();
		WebElement deletionStatus = driver.findElement(locators.get("guestTimeLine.deletionStatus"));
		String val = deletionStatus.getText().trim();
		return val;
	}

	public void setFavLocationTimeline(String locationName) {
		clickEditProfile();
		// driver.findElement(guestTimelineLocators.get("locationTextBox")).clear();
		WebElement locationTextBox = driver.findElement(locators.get("guestTimeLine.locationTextBox"));
		locationTextBox.click();
		List<WebElement> locationTextBoxOptions = driver
				.findElements(locators.get("guestTimeLine.locationTextBoxOptions"));
		utils.selectListDrpDwnValue(locationTextBoxOptions, locationName);
		WebElement updateEditProfile = driver.findElement(locators.get("guestTimeLine.updateEditProfile"));
		updateEditProfile.click();
		utils.waitTillPagePaceDone();
	}

	public String checkLocationInAuditLogs() {

		WebElement auditLogsBtn = driver.findElement(locators.get("guestTimeLine.auditLogsBtn"));
		auditLogsBtn.click();
		utils.waitTillPagePaceDone();
		WebElement auditLogsLocation = driver.findElement(locators.get("guestTimeLine.auditLogsLocation"));
		String locationid = auditLogsLocation.getText();
		return locationid;

	}

	public String getPosScannerCheckinNotification(String notification) throws InterruptedException {
		String posNotification = "";
		boolean flag = false;
		int attempts = 0;
		while (attempts < 30) {
			refreshTimeline();
			try {
				WebElement posScannerCheckinNotification = driver
						.findElement(locators.get("guestTimeLine.posScannerCheckinNotification"));
				String str = posScannerCheckinNotification.getText();
				String[] name = str.split(":");
				posNotification = name[1].trim();
				if (posNotification.equalsIgnoreCase(notification)) {
					flag = true;
					logger.info("Pos scanner checkin notification" + posNotification + " matched on the timeline");
					TestListeners.extentTest.get()
							.pass("Pos scanner checkin notification" + posNotification + " matched on the timeline");
					break;
				}
			} catch (Exception e) {
				logger.info("Element is not present or Pos scanner checkin notification did not matched");
			}
			attempts++;
			utils.longWaitInSeconds(5);
		}
		return posNotification;

	}

	public boolean verifyTitleFromTimeline(String text) throws InterruptedException {
		boolean result = false;
		int attempts = 0;
		String xpath = utils.getLocatorValue("guestTimeLine.verifyTimelineText").replace("$text", text);
		while (attempts < 4) {
			refreshTimeline();
//			Thread.sleep(2000);
			try {
				WebElement title = driver.findElement(By.xpath(xpath));
				if (title.isDisplayed()) {
					result = true;
					logger.info("Element:" + title + " title is present");
					break;
				}
			} catch (Exception e) {
				logger.info("Element is not present " + attempts);
			}
			attempts++;
		}
		// driver.findElement(By.xpath(xpath)).isDisplayed();
		return result;
	}

	public boolean verifyChatGPTReplyonGuestTimeLine(String str) {
		WebElement replyMsgDisplay = driver.findElement(locators.get("guestTimeLine.replyMsgDisplay"));
		String text = replyMsgDisplay.getText();
		if (text.equalsIgnoreCase(str)) {
			logger.info("both the values are equal");
			return true;
		}
		logger.info("both the values are not equal");
		return false;
	}

	@SuppressWarnings("unlikely-arg-type")
	public void setFavaoriteLocation(String location) {
		WebElement favoriteLocation = driver.findElement(locators.get("guestTimeLine.favoriteLocation"));
		favoriteLocation.click();
		List<WebElement> favoriteLocationLst = driver.findElements(locators.get("guestTimeLine.favoriteLocationLst"));
		for (int i = 0; i < favoriteLocationLst.size(); i++) {
			if (favoriteLocationLst.get(i).equals(location)) {
				favoriteLocationLst.get(i).click();
			}
		}
		WebElement updateEditProfile = driver.findElement(locators.get("guestTimeLine.updateEditProfile"));
		updateEditProfile.click();
	}

	public boolean verifyMsgOptional() {
		selUtils.implicitWait(3);
		return utils.getLocator("messagePage.msgOptional").isDisplayed();
	}

	public void editReplyText(String str) {
		utils.getLocator("messagePage.feedbackReplyTextarea").clear();
		utils.getLocator("messagePage.feedbackReplyTextarea").sendKeys(str);
	}

	public String replyTextareaValue() {
		return utils.getLocator("messagePage.feedbackReplyTextarea").getText();
	}

	public void refreshpage() {
		utils.refreshPage();
		utils.implicitWait(5);
	}

	// public boolean compareText(String str1, String str2) {
	// if (str1.equalsIgnoreCase(str2)) {
	// logger.info("Both the text are same");
	// return true;
	// }
	// logger.info("Both the text are not same");
	// return false;
	// }

	public void msgBtn() {
		utils.getLocator("messagePage.msgBtn").click();
		TestListeners.extentTest.get().info("Clicked the Message button.");
		logger.info("Clicked the Message button.");
	}

	public void nagivateBack() {
		utils.navigateBackPage();
		utils.implicitWait(6);
	}

	public void selectSubscriptionCancellationReason(String str) {
		WebElement subscriptionCancelReasonDrpDown = driver
				.findElement(locators.get("guestTimeLine.subscriptionCancelReasonDrpDown"));
		subscriptionCancelReasonDrpDown.click();
		selUtils.longWait(1000);
		WebElement subscriptionCancelReasonTypeReason = driver
				.findElement(locators.get("guestTimeLine.subscriptionCancelReasonTypeReason"));
		subscriptionCancelReasonTypeReason.sendKeys(str);
		subscriptionCancelReasonTypeReason.sendKeys(Keys.ENTER);
	}

	public boolean recentAccountistory() {
		WebElement recenttab = driver.findElement(locators.get("guestTimeLine.recenttab"));
		recenttab.click();
		String val = driver.getPageSource();
		boolean result = val.contains("100 points earned for your $20.00 purchase");
		return result;
	}

	public boolean lifeTimeAccountistory() {
		WebElement lifetimeTab = driver.findElement(locators.get("guestTimeLine.lifetimeTab"));
		lifetimeTab.click();
		utils.waitTillPagePaceDone();
		String val = driver.getPageSource();
		boolean result = val.contains("100 points earned for your $20.00 purchase");
		return result;
	}

	public void setCancellationFeedbackInTextArea(String cancelReason) {
		WebElement subscriptionCancelReason = driver
				.findElement(locators.get("guestTimeLine.subscriptionCancelReason"));
		subscriptionCancelReason.sendKeys(cancelReason);
	}

	public void accecptSubscriptionCancellation(String cancelReason) {
		WebElement subscriptionCancelReasonDropDown = driver
				.findElement(locators.get("guestTimeLine.subscriptionCancelReasonDropDown"));
		utils.selectDrpDwnValue(subscriptionCancelReasonDropDown, cancelReason);
		WebElement subscriptionCancelYesButton = driver
				.findElement(locators.get("guestTimeLine.subscriptionCancelYesButton"));
		subscriptionCancelYesButton.click();
	}

	public void clickOnSubscriptionCancelUsingSubscriptionName(String cancelType, String spname) {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
		selUtils.implicitWait(5);
		String xpath = utils.getLocatorValue("guestTimeLine.plansSortingThreeoDotsUsingSubName").replace("$flag",
				spname);
		int i = 0;
		while (i < 10) {
			try {
				utils.refreshPage();
				WebElement ele = driver.findElement(By.xpath(xpath));
				selUtils.implicitWait(5);
				ele.click();
				break;
			} catch (Exception e) {
				i++;
			}
		}

		// wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
		// utils.refreshPage();
		// wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
		// ele.click();

		// utils.clickByJSExecutor(driver,ele);
		String cancelTpyeXpath = utils.getLocatorValue("guestTimeLine.cancelPlanType").replace("$cancelPlanType",
				cancelType);
		utils.getXpathWebElements(By.xpath(cancelTpyeXpath)).click();
	}

	public String getSubscriptionEndDate() {
		selUtils.implicitWait(50);
		WebElement endDateTime = driver.findElement(locators.get("guestTimeLine.endDateTime"));
		String date = endDateTime.getText();
		return date;
	}

	public void receiptBarcode(String str) {
		utils.getLocator("messagePage.receiptBarcode").clear();
		utils.getLocator("messagePage.receiptBarcode").sendKeys(str);
	}

	public void messageReceiptBarcode(String reply, String barcode, String location, String option, String giftTypes,
			String giftReason) {
		WebElement messageGiftBtn = driver.findElement(locators.get("guestTimeLine.messageGiftBtn"));
		messageGiftBtn.click();
		editReplyText(reply);
		receiptBarcode(barcode);

		WebElement locationDrpDwn = driver.findElement(locators.get("guestTimeLine.locationDrpDwn"));
		locationDrpDwn.click();
		List<WebElement> locationDrpDwnList = driver.findElements(locators.get("guestTimeLine.locationDrpDwnList"));
		utils.selectListDrpDwnValue(locationDrpDwnList, location);

		WebElement giftTypeDrp = driver.findElement(locators.get("guestTimeLine.giftTypeDrp"));
		giftTypeDrp.click();
		List<WebElement> giftTypeDrpList = driver.findElements(locators.get("guestTimeLine.giftTypeDrpList"));
		utils.selectListDrpDwnValue(giftTypeDrpList, option);

		driver.findElement(locators.get("guestTimeLine.giftPointsTxtBox")).clear();
		WebElement giftPointsTxtBox = driver.findElement(locators.get("guestTimeLine.giftPointsTxtBox"));
		giftPointsTxtBox.sendKeys(giftTypes);

		driver.findElement(locators.get("guestTimeLine.giftReasonTxtBox")).clear();
		WebElement giftReasonTxtBox = driver.findElement(locators.get("guestTimeLine.giftReasonTxtBox"));
		giftReasonTxtBox.sendKeys(giftReason);
		WebElement messageBtn = driver.findElement(locators.get("guestTimeLine.messageBtn"));
		messageBtn.click();

		// verify success message
		String msg = utils.getSuccessMessage();
		Assert.assertEquals(msg, "Message sent!", "Unable to gift receipt barcode to user");

		logger.info("Gift Receipt Barcode to user successfully");
		TestListeners.extentTest.get().pass("Gift Receipt Barcode to user successfully");
	}

	public boolean accountHistoryApi2(String client, String secret, String token, String identifierName,
			String variable) {
		List<Object> obj = new ArrayList<Object>();
		boolean status = false;
		String description;
		// User Account History
		Response accountHistoryResponse = pageObj.endpoints().Api2UserAccountHistory(client, secret, token);
		Assert.assertEquals(200, accountHistoryResponse.getStatusCode(),
				"Status code 200 did not matched for api2 Account Histor");
		obj = accountHistoryResponse.jsonPath().getList(variable);
		for (int i = 0; i < obj.size(); i++) {
			description = accountHistoryResponse.jsonPath().getString("[" + i + "]." + variable);
			if (description.contains(identifierName)) {
				status = true;
				break;
			}
		}
		return status;
	}

	public boolean accountHistoryApi1(String client, String secret, String token, String identifierName,
			String variable) {
		List<Object> obj = new ArrayList<Object>();
		boolean status = false;
		String description;
		// User Account History
		Response accountHistoryResponse = pageObj.endpoints().Api1MobileAccounts(token, client, secret);
		Assert.assertEquals(200, accountHistoryResponse.getStatusCode(),
				"Status code 200 did not matched for Api1 Account Histor");
		obj = accountHistoryResponse.jsonPath().getList(variable);
		for (int i = 0; i < obj.size(); i++) {
			description = accountHistoryResponse.jsonPath().getString("[" + i + "]." + variable);
			if (description.contains(identifierName)) {
				status = true;
				break;
			}
		}
		return status;
	}

	public boolean accountHistoryApiAuth(String client, String secret, String authToken, String identifierName,
			String variable) {
		List<Object> obj = new ArrayList<Object>();
		boolean status = false;
		String description;
		// User Account History
		Response accountHistoryResponse = pageObj.endpoints().authApiAccountHistory(authToken, client, secret);
		Assert.assertEquals(200, accountHistoryResponse.getStatusCode(),
				"Status code 200 did not matched for Auth Account History");
		obj = accountHistoryResponse.jsonPath().getList(variable);
		for (int i = 0; i < obj.size(); i++) {
			description = accountHistoryResponse.jsonPath().getString("[" + i + "]." + variable);
			if (description.contains(identifierName)) {
				status = true;
				break;
			}
		}
		return status;
	}

	public void makeGuestSuperUser() {
		WebElement guestFunctionButton = driver.findElement(locators.get("guestTimeLine.guestFunctionButton"));
		guestFunctionButton.click();
		WebElement makeSuperUser = driver.findElement(locators.get("guestTimeLine.makeSuperUser"));
		makeSuperUser.click();
		utils.acceptAlert(driver);
		WebElement deactivateUserSuccessMessage = driver
				.findElement(locators.get("guestTimeLine.deactivateUserSuccessMessage"));
		utils.waitTillVisibilityOfElement(deactivateUserSuccessMessage, "success msg");
		logger.info("successfully guest is marked as super user");
		TestListeners.extentTest.get().pass("successfully guest is marked as super user");
	}

	public void clickDeleteAnonymizeGuest(String deleteOption) {
		deactivateGuest();
		utils.waitTillCompletePageLoad();
		WebElement guestFunctionButton = driver.findElement(locators.get("guestTimeLine.guestFunctionButton"));
		utils.waitTillElementToBeClickable(guestFunctionButton);
		guestFunctionButton.click();
		WebElement deleteAnonymizeGuestButton = driver
				.findElement(locators.get("guestTimeLine.deleteAnonymizeGuestButton"));
		deleteAnonymizeGuestButton.click();
		WebElement incinerateDropDown = driver.findElement(locators.get("guestTimeLine.incinerateDropDown"));
		utils.waitTillVisibilityOfElement(incinerateDropDown, "");
		utils.selectDrpDwnValue(incinerateDropDown, deleteOption);
		WebElement submitBtn = driver.findElement(locators.get("guestTimeLine.submitBtn"));
		utils.clickByJSExecutor(driver, submitBtn);
		utils.acceptAlert(driver);
		logger.info("delete the user while selecting the " + deleteOption + " from the drop down");
		TestListeners.extentTest.get()
				.info("delete the user while selecting the " + deleteOption + " from the drop down");
		utils.waitTillPagePaceDone();
	}

	public String getRedeemableExpiryDateGuestTimeline(String reedemableName) {
		String val = "";
		refreshTimeline();
		utils.waitTillPagePaceDone();
		String xpath = utils.getLocatorValue("guestTimeLine.getReedemableExpiryDateGuestTimeline").replace("$flag",
				reedemableName);
		WebElement reedemableExpiryDateGuestTimeline = driver.findElement(By.xpath(xpath));
		val = reedemableExpiryDateGuestTimeline.getText();
		return val;
	}

	public String convertTimeZone(String date, String timezone) {
		return utils.convertDateTimeZone(date, timezone);
	}

	public String getRedeemableExpiryFromRewards(String reedemableName) {
		String val = "";
		refreshTimeline();
		utils.waitTillPagePaceDone();
		String xpath = utils.getLocatorValue("guestTimeLine.getReedemableExpiryDateFromRewards").replace("$flag",
				reedemableName);
		WebElement reedemableExpiryDateFromRewards = driver.findElement(By.xpath(xpath));
		val = reedemableExpiryDateFromRewards.getText();
		String[] newVal = val.split("Expiry:");
		return newVal[1].trim();
	}

	public String getPushNotificationForCampaign(String campName) throws InterruptedException {
		String result = "";
		int attempts = 0;
		while (attempts < 2) {
			try {
				String xpath = utils.getLocatorValue("guestTimeLine.campPushNotificationText").replace("$campName",
						campName);
				WebElement campPushNotificationText = driver.findElement(By.xpath(xpath));
				result = campPushNotificationText.getText();
				logger.info(result + " Push notification message is visible on user time line page");
				TestListeners.extentTest.get()
						.info(result + " Push notification message is visible on user time line page");
			} catch (Exception e) {
				logger.info("Push notification message is not visible ");
				TestListeners.extentTest.get().info("Push notification message is not visible ");
			}
			attempts++;
		}
		return result;
	}

	public int verifyRegiftingOfRecallCampaign(String Reason) throws InterruptedException {
		int attempts = 0;
		int rewardCount = 0;
		while (attempts < 2) {
			utils.refreshPage();
			utils.longWaitInSeconds(4);
			try {
				List<WebElement> count2 = driver.findElements(By
						.xpath(utils.getLocatorValue("guestTimeLine.recallGiftReason").replace("$campreason", Reason)));
				rewardCount = count2.size();
				logger.info(rewardCount + " : is the count of the rewards given to the user");
				TestListeners.extentTest.get().info(rewardCount + " : is the count of the rewards given to the user");
			} catch (Exception e) {
				logger.error("Reward has not assigned to the user on the attempt " + attempts);
				TestListeners.extentTest.get().fail("Reward has not assigned to the user on the attempt " + attempts);
			}
			attempts++;
		}
		return rewardCount;
	}

	public boolean verifyCampaignSystemNotificationIsVisible(String campName) throws InterruptedException {
		boolean result = false;
		try {
			String xpath = utils.getLocatorValue("guestTimeLine.campOrSystemNotificationLabel").replace("$campName",
					campName);
			WebElement campaignNotification = driver.findElement(By.xpath(xpath));
			if (campaignNotification.isDisplayed()) {
				result = true;
				logger.info("CampaignSystemNotification label is visible for the campaign -" + campName);
				TestListeners.extentTest.get()
						.info("CampaignSystemNotification label is visible for the campaign -" + campName);
			}
		} catch (Exception e) {
			logger.error("CampaignSystemNotification label is not visible for the campaign -" + campName);
			TestListeners.extentTest.get()
					.fail(result + " Push notification message is visible on user time line page");
			Assert.fail("CampaignSystemNotification label is not visible for the campaign -" + campName);
		}
		return result;
	}

	public boolean verifyApiResponseVariable(Response apiResponse, String actualText, String expectedText,
			String value1, String value2) {
		boolean flag = false;
		String jsonObjectString = apiResponse.asString();
		JSONObject finalResponse = new JSONObject(jsonObjectString);
		if (utils.textContains(actualText, expectedText)) {
			boolean variableName = finalResponse.getJSONArray(value1).getJSONObject(0).has(value2);
			flag = variableName;
		}
		return flag;
	}

	public void deactivateGuest() {
		WebElement guestFunctionButton = driver.findElement(locators.get("guestTimeLine.guestFunctionButton"));
		guestFunctionButton.click();
		WebElement deactivateUser = driver.findElement(locators.get("guestTimeLine.deactivateUser"));
		utils.clickByJSExecutor(driver, deactivateUser);
		utils.acceptAlert(driver);
		WebElement deactivateReason = driver.findElement(locators.get("guestTimeLine.deactivateReason"));
		utils.waitTillVisibilityOfElement(deactivateReason, "delete reason");
		deactivateReason.sendKeys("Automation");
		WebElement deactivateSubmitBtn = driver.findElement(locators.get("guestTimeLine.deactivateSubmitBtn"));
		deactivateSubmitBtn.click();

		logger.info("Deactivated the user");
		TestListeners.extentTest.get().info("Deactivated the user");
	}

	public void delGuest(String deleteOption) {
		WebElement guestFunctionButton = driver.findElement(locators.get("guestTimeLine.guestFunctionButton"));
		utils.waitTillElementToBeClickable(guestFunctionButton);
		guestFunctionButton.click();
		WebElement deleteAnonymizeGuestButton = driver
				.findElement(locators.get("guestTimeLine.deleteAnonymizeGuestButton"));
		deleteAnonymizeGuestButton.click();
		WebElement incinerateDropDown = driver.findElement(locators.get("guestTimeLine.incinerateDropDown"));
		utils.waitTillVisibilityOfElement(incinerateDropDown, "");
		utils.selectDrpDwnValue(incinerateDropDown, deleteOption);
		WebElement submitBtn = driver.findElement(locators.get("guestTimeLine.submitBtn"));
		utils.clickByJSExecutor(driver, submitBtn);
		utils.acceptAlert(driver);
		logger.info("delete the user while selecting the " + deleteOption + " from the drop down");
		TestListeners.extentTest.get()
				.info("delete the user while selecting the " + deleteOption + " from the drop down");
		utils.waitTillPagePaceDone();
	}

	public boolean verifyApiResponseVariableArray(Response apiResponse, String apiResponseVar, String expectedText,
			String value2) {
		try {
			String jsonObjectString = apiResponse.asString();
			JSONArray finalResponse = new JSONArray(jsonObjectString);

			// Iterate over the array elements
			for (int i = 0; i < finalResponse.length(); i++) {
				JSONObject obj = finalResponse.optJSONObject(i);
				if (obj == null)
					continue;

				// Safely extract the variable
				String variableValue = obj.optString(apiResponseVar, null);
				if (variableValue != null && utils.textContains(variableValue, expectedText)) {
					// Check if key exists in this object
					return obj.has(value2);
				}
			}

			// If no match found for expectedText
			return false;

		} catch (Exception e) {
			// Log for debugging (you can replace this with your logger)
			logger.info("Error verifying API response: " + e.getMessage());
			TestListeners.extentTest.get().info("Error verifying API response: " + e.getMessage());
			return false;
		}
	}

	public String getRedeemableRewardId(int expectedVal) {
		utils.waitTillPagePaceDone();
		int counter = 0;
		int val;
		do {
			WebElement noOfRedeemable = driver.findElement(locators.get("guestTimeLine.noOfRedeemable"));
			utils.waitTillVisibilityOfElement(noOfRedeemable, "");
			String text = noOfRedeemable.getText();
			val = Integer.parseInt(text);
			if (val == expectedVal) {
				logger.info("user get the redeemable");
				TestListeners.extentTest.get().info("user get the redeemable");
				break;
			}
			counter++;
			utils.refreshPage();
			utils.waitTillPagePaceDone();
		} while (counter < 20);

		Assert.assertTrue(val == expectedVal, "user didnot get the redeemable");

		WebElement rewardID = driver.findElement(locators.get("guestTimeLine.rewardID"));
		utils.waitTillVisibilityOfElement(rewardID, "");
		String href = rewardID.getAttribute("href");
		Pattern pattern = Pattern.compile("reward_id=(.*)");
		Matcher matcher = pattern.matcher(href);
		String rewardId = null;
		if (matcher.find()) {
			rewardId = matcher.group(1);
			logger.info("the reward id is --" + rewardId);
			TestListeners.extentTest.get().info("the reward id is --" + rewardId);
			return rewardId;
		} else {
			logger.error("Not able to fetch the redeemable reward id");
			TestListeners.extentTest.get().fail("Not able to fetch the redeemable reward id");
			Assert.assertNotEquals(rewardId, null, "Not able to fetch the redeemable reward id");
			return null;
		}
	}

	public void clickReward() {
		WebElement rewardTab = driver.findElement(locators.get("guestTimeLine.rewardTab"));
		utils.waitTillVisibilityOfElement(rewardTab, "");
		rewardTab.click();
		logger.info("clicked on the rewards");
		TestListeners.extentTest.get().info("clicked on the rewards");
	}

	public void giftRedeemableToUser(String reward, String redeemable) {
		WebElement messageGiftBtn = driver.findElement(locators.get("guestTimeLine.messageGiftBtn"));
		messageGiftBtn.click();
		utils.waitTillPagePaceDone();

		WebElement giftTypeDrp = driver.findElement(locators.get("guestTimeLine.giftTypeDrp"));
		giftTypeDrp.click();
		List<WebElement> giftTypeDrpList = driver.findElements(locators.get("guestTimeLine.giftTypeDrpList"));
		utils.selectListDrpDwnValue(giftTypeDrpList, reward);

		WebElement redeemableDrp = driver.findElement(locators.get("guestTimeLine.redeemableDrp"));
		redeemableDrp.click();
		List<WebElement> redeemableDrpList = driver.findElements(locators.get("guestTimeLine.redeemableDrpList"));
		utils.selectListDrpDwnValue(redeemableDrpList, redeemable);

		WebElement messageBtn = driver.findElement(locators.get("guestTimeLine.messageBtn"));
		utils.scrollToElement(driver, messageBtn);
		messageBtn.click();

		utils.waitTillPagePaceDone();

		logger.info("Gift from timeline messaged to user successfully");
		TestListeners.extentTest.get().info("Gift from timeline messaged to user successfully");
	}

	public int noOfTimesCampaignVisibleInTimeLine(String campaignName, int expectedTime) throws InterruptedException {
		String xpath = utils.getLocatorValue("guestTimeLine.campaignVisibleInTimeLine").replace("{$campaignName}",
				campaignName);
		List<WebElement> ele = null;
		int size;
		int counter = 0;
		do {
			ele = driver.findElements(By.xpath(xpath));
			size = ele.size();
			if (size == expectedTime) {
				break;
			}
			// Thread.sleep(1200);
			utils.refreshPage();
			utils.waitTillPagePaceDone();
			counter++;
		} while (counter < 15);
		logger.info("no of times campaign --" + campaignName + " visible on the timeline is--" + size);
		TestListeners.extentTest.get()
				.info("no of times campaign --" + campaignName + " visible on the timeline is--" + size);
		return size;
	}

	public String verifyDiscountBasketVariable(Response apiResponse, String variable, String variable2,
			String actualValue, String verifyingVar) {
		List<Object> obj = new ArrayList<Object>();
		int j = 0;
		String expectedValue, expectedValueFlag;

		obj = apiResponse.jsonPath().getList(variable);
		for (int i = 0; i < obj.size(); i++) {
			expectedValue = apiResponse.jsonPath().getString(variable + "[" + i + "]." + variable2);
			if (expectedValue.contains(actualValue)) {
				j = i;
				break;
			}
		}
		expectedValueFlag = apiResponse.jsonPath().getString(variable + "[" + j + "]." + verifyingVar);
		return expectedValueFlag;
	}

	public boolean checkGifting(String campaignName, String redeemableName, String pn) throws InterruptedException {
		String xpath = utils.getLocatorValue("guestTimeLine.checkCampaignInTimeLine")
				.replace("${campName}", campaignName).replace("${pn}", pn);
		String xpath2 = utils.getLocatorValue("guestTimeLine.redeemableGifted").replace("${campName}", campaignName)
				.replace("${redeemable}", redeemableName);
		utils.implicitWait(5);
		int counter = 0;
		boolean flag = false;
		do {
			try {
				WebElement checkCampaignInTimeLine = driver.findElement(By.xpath(xpath));
				boolean camName = checkCampaignInTimeLine.isDisplayed();
				Assert.assertTrue(camName, "Campaign name is not present");
				logger.info("Verified Campaign name is displayed in the timeline");
				TestListeners.extentTest.get().pass("Verified Campaign name is displayed in the timeline");

				WebElement redeemableGifted = driver.findElement(By.xpath(xpath2));
				boolean RedeemableName = redeemableGifted.isDisplayed();
				Assert.assertTrue(RedeemableName, "redeemable is not present");
				logger.info("Verified redeemable name is displayed in the timeline");
				TestListeners.extentTest.get().pass("Verified redeemable name is displayed in the timeline");
				flag = true;
				break;
			} catch (Exception e) {
				utils.longWaitInSeconds(1);
				utils.refreshPage();
				utils.waitTillPagePaceDone();
				counter++;
			}
		} while (counter < 30);
		utils.implicitWait(50);
		return flag;
	}

	public List<String> getRewardIds(String redeemableName, int actualSize) {
		utils.waitTillPagePaceDone();
		String xpath = utils.getLocatorValue("guestTimeLine.getRewardId").replace("${redeemableName}", redeemableName);
		List<WebElement> eleLst = driver.findElements(By.xpath(xpath));
		List<String> lst = new ArrayList<String>();
		int counter = 0;
		do {
			eleLst = driver.findElements(By.xpath(xpath));
			int size = eleLst.size();
			if (size >= actualSize) {
				for (int i = 0; i < actualSize; i++) {
					String href = eleLst.get(i).getAttribute("href");
					Pattern pattern = Pattern.compile("reward_id=(.*)");
					Matcher matcher = pattern.matcher(href);
					String rewardId = null;
					if (matcher.find()) {
						rewardId = matcher.group(1);
						lst.add(rewardId);
						logger.info("the reward id is --" + rewardId);
						TestListeners.extentTest.get().info("the reward id is --" + rewardId);
					}
				}
				break;
			}
			utils.longWaitInSeconds(1);
			utils.refreshPage();
			utils.waitTillPagePaceDone();
			counter++;
		} while (counter < 10);
		return lst;
	}

	public boolean checkPNForCampaign(String campaignName, String pn) throws InterruptedException {
		String xpath = utils.getLocatorValue("guestTimeLine.checkCampaignInTimeLine")
				.replace("${campName}", campaignName).replace("${pn}", pn);
		utils.implicitWait(5);
		int counter = 0;
		boolean flag = false;
		do {
			try {
				WebElement checkCampaignInTimeLine = driver.findElement(By.xpath(xpath));
				boolean camName = checkCampaignInTimeLine.isDisplayed();
				Assert.assertTrue(camName, "Campaign name is not present");
				logger.info("Verified Campaign name is displayed in the timeline");
				TestListeners.extentTest.get().pass("Verified Campaign name is displayed in the timeline");
				flag = true;
				break;
			} catch (Exception e) {
				utils.longWaitInSeconds(1);
				utils.refreshPage();
				utils.waitTillPagePaceDone();
				counter++;
			}
		} while (counter < 30);
		utils.implicitWait(50);
		return flag;
	}

	public boolean checkRedeemableGiftingThroughCampaignID(String campaignID, String redeemableName) {
		String xpath = utils.getLocatorValue("guestTimeLine.getRedeemableThroughCampaignID")
				.replace("{$redeemableName}", redeemableName).replace("${campaignID}", campaignID);

		final int MAX_RETRIES = 10;
		boolean isFound = false;
		utils.implicitWait(2);
		for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
			try {
				WebElement element = driver.findElement(By.xpath(xpath));
				if (element.isDisplayed()) {
					logger.info("Redeemable found for campaign ID: {}", campaignID);
					TestListeners.extentTest.get().info("Redeemable found for campaign ID: " + campaignID);
					isFound = true;
					break;
				}
			} catch (Exception e) {
				utils.refreshPage();
				utils.waitTillPagePaceDone();
				logger.info("Attempt {}: Redeemable not found. Refreshing the page.", attempt + 1);
				TestListeners.extentTest.get()
						.info("Attempt " + (attempt + 1) + ": Redeemable not found. Refreshing the page.");
			}
		}
		utils.implicitWait(50);
		return isFound;
	}

	public boolean checkRedeemableGiftingThroughCampaignIDFalse(String campaignID, String redeemableName) {
		String xpath = utils.getLocatorValue("guestTimeLine.getRedeemableThroughCampaignID")
				.replace("{$redeemableName}", redeemableName).replace("${campaignID}", campaignID);

		final int MAX_RETRIES = 5;
		boolean isFound = false;
		utils.implicitWait(2);
		for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
			try {
				WebElement element = driver.findElement(By.xpath(xpath));
				if (element.isDisplayed()) {
					logger.info("Redeemable found for campaign ID: {}", campaignID);
					TestListeners.extentTest.get().info("Redeemable found for campaign ID: " + campaignID);
					isFound = true;
					break;
				}
			} catch (Exception e) {
				utils.refreshPage();
				utils.waitTillPagePaceDone();
				logger.info("Attempt {}: Redeemable not found. Refreshing the page.", attempt + 1);
				TestListeners.extentTest.get()
						.info("Attempt " + (attempt + 1) + ": Redeemable not found. Refreshing the page.");
			}
		}
		utils.implicitWait(50);
		return isFound;
	}

	// used to verify the tags under the user email id eg. Mem_Level Bronze Guest
	// /Loyalty /Deactivated/Banned etc

	public boolean verifyTagsOnUserTimeline(String emailID, String expectedTagName) {
		String xpath = utils.getLocatorValue("guestTimeLine.tagNamesXpath").replace("${userEmailID}", emailID);
		List<WebElement> weleList = driver.findElements(By.xpath(xpath));
		if (weleList.size() != 0) {
			for (WebElement wEle : weleList) {
				String actualTagName = wEle.getText();
				if (actualTagName.equalsIgnoreCase(expectedTagName)) {
					logger.info(expectedTagName + " tag is visible on timeline ");
					TestListeners.extentTest.get().info(expectedTagName + " tag is visible on timeline ");
					return true;
				}
			}
		} else {
			logger.info("No tags are displayed on user timeline page");
			TestListeners.extentTest.get().fail("No tags are displayed on user timeline page");
		}
		return false;

	}

	public void clickOnGuestFunctionsOptions(String optionName) {
		WebElement guestFunctionButton = driver.findElement(locators.get("guestTimeLine.guestFunctionButton"));
		guestFunctionButton.click();
		String xpath = utils.getLocatorValue("guestTimeLine.guestFunctionsOptions").replace("${optionName}",
				optionName);
		WebElement wele = driver.findElement(By.xpath(xpath));
		wele.click();
	}

	public void deactivateGuestWithAllowReactivationOrNot(String value) {
		WebElement guestFunctionButton = driver.findElement(locators.get("guestTimeLine.guestFunctionButton"));
		guestFunctionButton.click();
		WebElement deactivateUser = driver.findElement(locators.get("guestTimeLine.deactivateUser"));
		utils.clickByJSExecutor(driver, deactivateUser);
		utils.acceptAlert(driver);

		String xpath = utils.getLocatorValue("guestTimeLine.allowReactivation").replace("${value}", value);
		WebElement allowReactivation = driver.findElement(By.xpath(xpath));
		utils.clickByJSExecutor(driver, allowReactivation);

		WebElement deactivateReason = driver.findElement(locators.get("guestTimeLine.deactivateReason"));
		utils.waitTillVisibilityOfElement(deactivateReason, "delete reason");
		deactivateReason.sendKeys("Automation");
		WebElement deactivateSubmitBtn = driver.findElement(locators.get("guestTimeLine.deactivateSubmitBtn"));
		deactivateSubmitBtn.click();
		utils.waitTillPagePaceDone();

		logger.info("deactivate the user with allow reactivation -- " + value);
		TestListeners.extentTest.get().info("deactivate the user with allow reactivation -- " + value);
	}

	public void updateBtn() {
		WebElement updateBtn = driver.findElement(locators.get("guestTimeLine.updateBtn"));
		utils.scrollToElement(driver, updateBtn);
		updateBtn.click();
		utils.waitTillPagePaceDone();

		logger.info("clicked on the update button");
		TestListeners.extentTest.get().info("clicked on the update button");
	}

	public void reactivateGuestFromTimeline() {
		WebElement editProfileTabLink = driver.findElement(locators.get("guestTimeLine.editProfileTabLink"));
		editProfileTabLink.click();
		WebElement guestFunctionButton = driver.findElement(locators.get("guestTimeLine.guestFunctionButton"));
		guestFunctionButton.click();
		WebElement reactivateGuestFromTimeline = driver
				.findElement(locators.get("guestTimeLine.reactivateGuestFromTimeline"));
		utils.clickByJSExecutor(driver, reactivateGuestFromTimeline);
		utils.acceptAlert(driver);
		utils.waitTillPagePaceDone();

		logger.info("reactivate the guest from the timeline");
		TestListeners.extentTest.get().info("reactivate the guest from the timeline");
	}

	public void clickAuditLog() {
		int attempts = 0, maxAttempts = 5;
		while (attempts < maxAttempts) {
			try {
				WebElement auditLogButton = driver.findElement(locators.get("guestTimeLine.auditLogsBtn"));
				selUtils.waitTillElementToBeClickable(auditLogButton);
				utils.StaleElementclick(driver, auditLogButton);
				utils.waitTillPagePaceDone();
				if (utils.verifyPartOfURL("/audit")) {
					utils.logit("Clicked on Audit Log button");
					break;
				} else {
					utils.logit(
							"Unable to navigate to Audit Log page on attempt " + (attempts + 1) + " of " + maxAttempts);
				}
			} catch (Exception e) {
				utils.logit("Failed to click on Audit Log button due to " + e.getMessage());
			}
			attempts++;
			utils.refreshPage();
			utils.waitTillPagePaceDone();
		}
	}

	public boolean checkReactivationLogs(String adminEmail) {
		boolean visible = false;
		String xpath = utils.getLocatorValue("guestTimeLine.checkReactivationLogs").replace("${adminEmail}",
				adminEmail);
		List<WebElement> checkReactivationLogsList = driver.findElements(By.xpath(xpath));
		if (checkReactivationLogsList.size() > 0) {
			visible = true;
			return visible;
		}
		return visible;
	}

	public boolean deactivationReasonVisible(String reason) {
		String xpath = utils.getLocatorValue("guestTimeLine.deactivateReasonTimeLine").replace("${reason}", reason);
		List<WebElement> deactivateReasonTimeLineList = driver.findElements(By.xpath(xpath));
		if (deactivateReasonTimeLineList.size() > 0) {
			logger.info("user deactivation reason is present in the timeline");
			TestListeners.extentTest.get().info("user deactivation reason is present in the timeline");
			return true;
		}
		logger.info("user deactivation reason is not present in the timeline");
		TestListeners.extentTest.get().fail("user deactivation reason is not present in the timeline");
		return false;
	}

	public boolean reactivationAllowedTimeline(String val) {
		WebElement allowReactivationTimeline = driver
				.findElement(locators.get("guestTimeLine.allowReactivationTimeline"));
		String value = allowReactivationTimeline.getText();
		if (value.contains(val)) {
			logger.info("reactivation allowed value is visible");
			TestListeners.extentTest.get().info("reactivation allowed value is visible");
			return true;
		}
		logger.info("reactivation allowed value is not visible");
		TestListeners.extentTest.get().fail("reactivation allowed value is not visible");
		return false;
	}

	public boolean adminDeactivateUserVisible(String name) {
		WebElement adminDeactivateUser = driver.findElement(locators.get("guestTimeLine.adminDeactivateUser"));
		String value = adminDeactivateUser.getText();
		if (value.contains(name)) {
			logger.info("admin name is visible");
			TestListeners.extentTest.get().info("admin name is visible");
			return true;
		}
		logger.info("admin name is not visible");
		TestListeners.extentTest.get().fail("admin name is not visible");
		return false;
	}

	public boolean deactivateReasonVisibleInNote(String reason, String adminName) {
		String xpath = utils.getLocatorValue("guestTimeLine.deactivateReasonVisibleInNote").replace("${reason}", reason)
				.replace("${adminName}", adminName);
		List<WebElement> deactivateReasonVisibleInNoteList = driver.findElements(By.xpath(xpath));
		if (deactivateReasonVisibleInNoteList.size() > 0) {
			logger.info("deavtivate reason is visible");
			TestListeners.extentTest.get().info("deavtivate reason is visible");
			return true;
		}
		logger.info("deavtivate reason is not visible");
		TestListeners.extentTest.get().fail("deavtivate reason is not visible");
		return false;
	}

	public boolean redeemableVisible(String campaignName, String redeemableName) throws InterruptedException {
		String xpath = utils.getLocatorValue("guestTimeLine.redeemableGifted").replace("${campName}", campaignName)
				.replace("${redeemable}", redeemableName);
		int counter = 0;
		do {
			List<WebElement> redeemableGiftedList = driver.findElements(By.xpath(xpath));
			if (redeemableGiftedList.size() > 0) {
				logger.info("found the redeemable in the timeline");
				TestListeners.extentTest.get().info("found the redeemable in the timeline");
				return true;
			} else {
				logger.info("redeemable not found refreshing the page " + counter);
				TestListeners.extentTest.get().info("redeemable not found refreshing the page " + counter);
				utils.longWaitInSeconds(1);
				utils.refreshPage();
				utils.waitTillPagePaceDone();
				counter++;
			}
		} while (counter < 30);
		logger.info("redeemable not found in the timeline even after 30 second wait");
		TestListeners.extentTest.get().fail("redeemable not found in the timeline even after 30 second wait");
		return false;
	}

	public void addComment(String comment) {
		WebElement addCommentEl = driver.findElement(locators.get("guestTimeLine.addComment"));
		addCommentEl.sendKeys(comment);
		logger.info("comment is added");
		TestListeners.extentTest.get().info("comment is added");
	}

	public void clickUserNoteButton() {
		WebElement createUserNoteButton = driver.findElement(locators.get("guestTimeLine.createUserNoteButton"));
		createUserNoteButton.click();
		utils.waitTillPagePaceDone();
		logger.info("clicked on the create user note button");
		TestListeners.extentTest.get().info("clicked on the create user note button");
	}

	public boolean labelVisible(String labelName) {
		boolean flag = false;
		String xpath = utils.getLocatorValue("guestTimeLine.labelVisible").replace("${labelName}", labelName);
		int attempts = 0;
		while (attempts < 15) {
			refreshTimeline();
			utils.longWaitInSeconds(3);
			try {
				utils.longWaitInSeconds(2);
				WebElement labelElement = driver.findElement(By.xpath(xpath));
				utils.waitTillVisibilityOfElement(labelElement, "label visibility");
				utils.scrollToElement(driver, labelElement);
				List<WebElement> labelElementList = driver.findElements(By.xpath(xpath));
				if (labelElementList.size() > 0) {
					logger.info("label is visible");
					TestListeners.extentTest.get().info("label is visible");
					flag = true;
					break;
				}
			} catch (Exception e) {
				logger.info("label is not visible");
				TestListeners.extentTest.get().info("label is not visible");
			}
			attempts++;
		}
		return flag;
	}

	public void guestTabNagivation(String tab) {
		String xpath = utils.getLocatorValue("guestTimeLine.guestTabNagivation").replace("${tabName}", tab);
		WebElement guestTabElement = driver.findElement(By.xpath(xpath));
		guestTabElement.click();
		utils.waitTillPagePaceDone();
		logger.info("clicked on the tab " + tab);
		TestListeners.extentTest.get().info("clicked on the tab " + tab);
	}

	public void searchBanGuest(String userEmail) {
		WebElement searchBanUser = driver.findElement(locators.get("guestTimeLine.searchBanUser"));
		searchBanUser.sendKeys(userEmail);
		WebElement searchIcon = driver.findElement(locators.get("guestTimeLine.searchIcon"));
		searchIcon.click();
		// driver.findElement(guestTimelineLocators.get("searchBanUser")).sendKeys(Keys.ENTER);
		utils.waitTillPagePaceDone();
		logger.info("searching ban user -- " + userEmail);
		TestListeners.extentTest.get().info("searching ban user -- " + userEmail);
	}

	public void UpdateUserPWDFromTimeline(String password) {
		WebElement editProfileTabLink = driver.findElement(locators.get("guestTimeLine.editProfileTabLink"));
		editProfileTabLink.click();
		WebElement passwordtextbox = driver.findElement(locators.get("guestTimeLine.passwordtextbox"));
		passwordtextbox.sendKeys(password);
		WebElement confpasswordtextbox = driver.findElement(locators.get("guestTimeLine.confpasswordtextbox"));
		confpasswordtextbox.sendKeys(password);
		WebElement updateBtn = driver.findElement(locators.get("guestTimeLine.updateBtn"));
		updateBtn.click();
		WebElement editProfileLabel = driver.findElement(locators.get("guestTimeLine.editProfileLabel"));
		editProfileLabel.isDisplayed();
		logger.info("User password updated ");
		TestListeners.extentTest.get().info("User password updated");

	}

	public boolean verifyBannedGuestIsVisible(String userEmail) {
		searchBanGuest(userEmail);
		String xpath = utils.getLocatorValue("guestTimeLine.banGuestVisible").replace("${userEmail}", userEmail);
		List<WebElement> banGuestVisibleList = driver.findElements(By.xpath(xpath));
		if (banGuestVisibleList.size() > 0) {
			logger.info("ban guest is visible");
			TestListeners.extentTest.get().info("ban guest is visible");
			return true;
		}
		logger.info("ban guest is not visible");
		TestListeners.extentTest.get().fail("ban guest is not visible");
		return false;
	}

	public boolean redeemablePointsRangeVisible(String pointsRange, String redeemable) {
		boolean flag = false;
		int attempts = 0;
		while (attempts < 15) {
			refreshTimeline();
			utils.longwait(3000);
			try {
				utils.longWaitInSeconds(2);
				String xpath = utils.getLocatorValue("guestTimeLine.redeemablePointRange")
						.replace("$redeemable", "Rewarded " + redeemable).replace("${range}", pointsRange);
				WebElement redeemablePointRange = driver.findElement(By.xpath(xpath));
				utils.waitTillVisibilityOfElement(redeemablePointRange, "range visibility");
				utils.scrollToElement(driver, redeemablePointRange);
				List<WebElement> redeemablePointRangeList = driver.findElements(By.xpath(xpath));
				if (redeemablePointRangeList.size() > 0) {
					logger.info(redeemable + " " + pointsRange + " range is visible");
					TestListeners.extentTest.get().info(redeemable + " " + pointsRange + " range is visible");
					flag = true;
					break;
				}
			} catch (Exception e) {

				logger.info(redeemable + " " + pointsRange + " range is NOT visible");
				TestListeners.extentTest.get().info(redeemable + " " + pointsRange + " range is NOT visible");

			}
			attempts++;
		}
		return flag;
	}

	public boolean searchDeactivatedUser(String userEmail) {
		utils.implicitWait(5);
		String xpath = utils.getLocatorValue("guestTimeLine.searchDeactivate").replace("${email}", userEmail);
		navigateToLastPage();
		List<WebElement> searchDeactivateList = driver.findElements(By.xpath(xpath));
		if (searchDeactivateList.size() > 0) {
			utils.implicitWait(50);
			logger.info("email present in deactivation tab");
			TestListeners.extentTest.get().info("email present in deactivation tab");
			return true;
		} else {
			utils.implicitWait(50);
			logger.info("email is not present in deactivation tab");
			TestListeners.extentTest.get().fail("email is not present in deactivation tab");
			return false;
		}
	}

	public String findLargestNumber(List<String> strings) {
		String largestNumber = null;
		for (String str : strings) {
			if (str.matches("-?\\d+(\\.\\d+)?")) {
				if (largestNumber == null || Integer.parseInt(str) > Integer.parseInt(largestNumber)) {
					largestNumber = str;
				}
			}
		}
		return largestNumber;
	}

	public void navigateToLastPage() {
		boolean flag = false;
		do {
			List<WebElement> mainList = driver.findElements(locators.get("guestTimeLine.paginationLst"));
			if (mainList.get(mainList.size() - 1).getAttribute("class").equalsIgnoreCase("next")
					&& mainList.get(mainList.size() - 2).getAttribute("class").equalsIgnoreCase("disabled")) {
				flag = false;
				String pageNumber = mainList.get(mainList.size() - 3).getText();
				WebElement pageLink = driver.findElement(By.linkText(pageNumber));
				pageLink.click();
				utils.waitTillPagePaceDone();
				logger.info("clicked on the page number -- " + pageNumber);
				TestListeners.extentTest.get().info("clicked on the page number -- " + pageNumber);

			} else if (mainList.get(mainList.size() - 1).getAttribute("class").equalsIgnoreCase("next")
					&& !mainList.get(mainList.size() - 2).getAttribute("class").equalsIgnoreCase("disabled")) {
				flag = true;
				String pageNumber = mainList.get(mainList.size() - 2).getText();
				WebElement pageLink = driver.findElement(By.linkText(pageNumber));
				pageLink.click();
				utils.waitTillPagePaceDone();
				logger.info("clicked on the page number -- " + pageNumber);
				TestListeners.extentTest.get().info("clicked on the page number -- " + pageNumber);
				break;

			} else if (!mainList.get(mainList.size() - 1).getAttribute("class").equalsIgnoreCase("next")
					&& !mainList.get(mainList.size() - 2).getAttribute("class").equalsIgnoreCase("disabled")) {
				flag = true;
				String pageNumber = mainList.get(mainList.size() - 1).getText();
				WebElement pageLink = driver.findElement(By.linkText(pageNumber));
				pageLink.click();
				utils.waitTillPagePaceDone();
				logger.info("clicked on the page number -- " + pageNumber);
				TestListeners.extentTest.get().info("clicked on the page number -- " + pageNumber);
				break;
			}
		} while (flag == false);
		logger.info("reach on the last page");
		TestListeners.extentTest.get().info("reach on the last page");
	}

	public String getGiftCardMessageForBlankCardNumber() {
		List<WebElement> messageList = driver.findElements(locators.get("guestTimeLine.giftCardMessage"));
		String textMessage = "";
		if (messageList.size() != 0) {
			textMessage = messageList.get(0).getText();
			logger.info(textMessage + " text message is coming on gift card page ");
			TestListeners.extentTest.get().info(textMessage + " text message is coming on gift card page ");
		}
		return textMessage;
	}

	public String getGiftCardNumberFromGiftCardTab(String userEmail) {
		String cardNumberXpath = utils.getLocatorValue("guestTimeLine.getGiftCardNumberGiftCardPage")
				.replace("${UserEmail}", userEmail);
		WebElement giftCardNumberElement = driver.findElement(By.xpath(cardNumberXpath));
		String actualCardNumberText = giftCardNumberElement.getText().trim();
		logger.info(actualCardNumberText + "  card number is coming on gift card page ");
		TestListeners.extentTest.get().info(actualCardNumberText + "  card number is coming on gift card page ");
		return actualCardNumberText;

	}

	// used for existing user
	public boolean verifyCampaignStatusWithTimeStampTimeline(String campName, String expectedTime)
			throws InterruptedException {
		boolean result = false;
		ZonedDateTime expDateTime = Utilities.parseDateTime(expectedTime);
		int attempts = 0;
		while (attempts <= 15) {
			try {
				String xpath = utils.getLocatorValue("guestTimeLine.campaignExecutionTimeList").replace("$campName",
						campName);
				List<WebElement> listOfCampName = driver.findElements(By.xpath(xpath));
				utils.implicitWait(2);
				for (WebElement wEle : listOfCampName) {
					String actualTimeFromUI = wEle.getText();
					actualTimeFromUI = actualTimeFromUI.replace("PM", "pm").replace("AM", "am");
					ZonedDateTime actuslDateTimeUI = Utilities.parseDateTime(actualTimeFromUI);
					String compareDateResult = Utilities.compareDateTimes(actuslDateTimeUI, expDateTime);
					if ((compareDateResult.equals("After")) || (compareDateResult.equals("Equals"))) {
						logger.info(actuslDateTimeUI + " actual execution time is " + compareDateResult + " from/with "
								+ expectedTime);
						TestListeners.extentTest.get().info(actuslDateTimeUI + " actual execution time is "
								+ compareDateResult + " from/with " + expectedTime);
						return true;
					}
				}

			} catch (Exception e) {
				logger.info(
						"Element is not present or Campaign Name did not matched... polling count is : " + attempts);
			}
			utils.longWaitInSeconds(4);
			refreshTimeline();
			utils.waitTillPagePaceDone();
			attempts++;
		}
		utils.implicitWait(50);
		return result;
	}

	public boolean flagPresentorNot(String flagName) {
		utils.implicitWait(3);
		boolean flag = true;
		String xpath = utils.getLocatorValue("guestTimeLine.flagPresentOrNot").replace("$flagName", flagName);
		List<WebElement> flagPresentOrNotList = driver.findElements(By.xpath(xpath));
		if (flagPresentOrNotList.size() == 0) {
			flag = false;
		}
		utils.implicitWait(50);
		return flag;
	}

	public String getGuestJoiningChannelEclub() {
		WebElement joinedChannel = driver.findElement(locators.get("guestTimeLine.joiningChannelEclub"));
		utils.scrollToElement(driver, joinedChannel);
		String channel = joinedChannel.getText().trim();
		logger.info("Joining channel is ==> " + channel);
		return channel;
	}

	public int checkMembershipNotify(String membershipValue, int times) {
		String xpath = utils.getLocatorValue("guestTimeLine.membershipNotify").replace("{$membership}",
				membershipValue);
		int count = 0;
		for (int i = 0; i < 15; i++) {
			List<WebElement> membershipNotifyList = driver.findElements(By.xpath(xpath));
			int size = membershipNotifyList.size();
			if (size == 0) {
				logger.info("membership notification not found refreshing the timeline -- " + i);
				TestListeners.extentTest.get()
						.info("membership notification not found refreshing the timeline -- " + i);
				count = size;
				utils.refreshPage();
				utils.waitTillPagePaceDone();
				continue;
			} else if (size == times || size >= times) {
				count = size;
				logger.info("found the membership notification it is been display -- " + size);
				TestListeners.extentTest.get().info("found the membership notification it is been display -- " + size);
				break;
			} else {
				count = size;
				utils.refreshPage();
				utils.waitTillPagePaceDone();
				logger.info("found the membership notification but not the expected times -- " + size);
				TestListeners.extentTest.get()
						.info("found the membership notification but not the expected times -- " + size);
				continue;
			}
		}
		return count;
	}

	public void waitTillDeactivateLabelIsVisibleOnUserTimelinePage() throws InterruptedException {
		String actualLabel = "";
		int counter = 0;
		do {
			utils.refreshPage();
			actualLabel = deactivationStatus();
			counter++;
			utils.longWaitInSeconds(1);

		} while (!actualLabel.equalsIgnoreCase("Deactivated") && (counter != 5));
	}

	public boolean checkPointsGifting(String campaignName, String giftPoints) throws InterruptedException {
		utils.implicitWait(5);
		int counter = 0;
		boolean flag = false;
		do {
			try {
				String xpath = utils.getLocatorValue("guestTimeLine.checkGiftedPointsOnTimeline")
						.replace("$points", giftPoints).replace("$campName", campaignName);
				WebElement checkGiftedPointsOnTimeline = driver.findElement(By.xpath(xpath));
				checkGiftedPointsOnTimeline.isDisplayed();
				logger.info("Verified Gifted Points are displayed in the timeline");
				TestListeners.extentTest.get().pass("Verified Gifted Points are displayed in the timeline");
				flag = true;
				break;
			} catch (Exception e) {
				utils.longWaitInSeconds(1);
				utils.refreshPage();
				utils.waitTillPagePaceDone();
				logger.info("user did not get gift points from campaign refreshing the page");
				TestListeners.extentTest.get().info("user did not get gift points from campaign refreshing the page");
				counter++;
			}
		} while (counter < 30);
		utils.implicitWait(50);
		return flag;
	}

	public boolean CheckIfCampaignTriggeredChallenge(String cname) throws InterruptedException {
		boolean flag = false;
		int attempts = 0;
		while (attempts < 20) {
			try {
				String xpath = utils.getLocatorValue("guestTimeLine.triggerdCampaignChallenge").replace("$campName",
						cname);
				WebElement triggerdCampaignChallenge = driver.findElement(By.xpath(xpath));
				String str = triggerdCampaignChallenge.getText();

				if (str.equalsIgnoreCase(cname)) {
					flag = true;
					logger.info("Campaign Name " + cname + " matched on the timeline");
					TestListeners.extentTest.get().pass("Campaign Name " + cname + " matched on the timeline");
					break;
				}
			} catch (Exception e) {
				logger.info("Element is not present or Campaign Name did not matched...");
				utils.refreshPage();
				utils.longWaitInSeconds(5);
			}
			attempts++;
		}
		return flag;
	}

	public boolean CheckChallengeCampaignStatus(String cname, String status) throws InterruptedException {
		utils.implicitWait(5);
		boolean flag = false;
		int attempts = 0;
		while (attempts < 20) {
			try {
				String xpath = utils.getLocatorValue("guestTimeLine.challengeCampaignProgress")
						.replace("$campName", cname).replace("$status", status);
				WebElement challengeCampaignProgress = driver.findElement(By.xpath(xpath));
				String str = challengeCampaignProgress.getText();

				if (str.contains(status)) {
					flag = true;
					logger.info("Campaign status on guest timeline is : " + status);
					TestListeners.extentTest.get().pass("Campaign status on guest timeline is : " + status);
					break;
				}
			} catch (Exception e) {
				logger.info("Element is not present or status did not matched...");
				utils.refreshPage();
				utils.longWaitInSeconds(5);
			}
			attempts++;
		}
		utils.implicitWait(50);
		return flag;
	}

	public void messageGiftChallengeProgressToUser(String giftTypes, String challengeName, String steps,
			String giftReason) {
		utils.waitTillPagePaceDone();
		WebElement messageGiftBtn = driver.findElement(locators.get("guestTimeLine.messageGiftBtn"));
		utils.waitTillInVisibilityOfElement(messageGiftBtn, "");
		utils.clickByJSExecutor(driver, messageGiftBtn);

		WebElement selectGitTypeFromDropdown = driver
				.findElement(locators.get("guestTimeLine.selectGitTypeFromDropdown"));
		utils.selectDrpDwnValue(selectGitTypeFromDropdown, giftTypes);
		WebElement selectChallengeCampForGiftingDropdown = driver
				.findElement(locators.get("guestTimeLine.selectChallengeCampForGiftingDropdown"));
		utils.selectDrpDwnValue(selectChallengeCampForGiftingDropdown, challengeName);
		driver.findElement(locators.get("guestTimeLine.NumberOfStepsTxtBox")).clear();
		WebElement NumberOfStepsTxtBox = driver.findElement(locators.get("guestTimeLine.NumberOfStepsTxtBox"));
		NumberOfStepsTxtBox.sendKeys(steps);

		WebElement messageBtn = driver.findElement(locators.get("guestTimeLine.messageBtn"));
		selUtils.waitTillElementToBeClickable(messageBtn);
		messageBtn.click();
		TestListeners.extentTest.get().pass("Gift amount messaged to user successfully");
	}

	public boolean userSearchInFraudSusupectBannedUser(String guestEmailId) {

		List<WebElement> mainList = driver.findElements(By.xpath("//ul[@class='pagination']/li"));
		selUtils.implicitWait(10);
		// check user email is exist or not
		List<WebElement> wEleList = driver.findElements(locators.get("guestTimeLine.banGuestList_Xpath"));
		if (wEleList.size() != 0) {
			for (WebElement wEle : wEleList) {
				String guestMailFromUI = wEle.getText();
				if (guestMailFromUI.equalsIgnoreCase(guestEmailId)) {
					selUtils.implicitWait(50);
					return true;
				}
			}
		}
		selUtils.implicitWait(50);
		return false;
	}

	public void setUserPhoneNumber(String phoneNumber) {
		pageObj.guestTimelinePage().navigateInsideGuestTimeline("Edit Profile");
		utils.longWaitInSeconds(3);
		driver.findElement(locators.get("guestTimeLine.editphoneNumber")).clear();
		WebElement editphoneNumber = driver.findElement(locators.get("guestTimeLine.editphoneNumber"));
		editphoneNumber.sendKeys(phoneNumber);
		WebElement updateEditProfile = driver.findElement(locators.get("guestTimeLine.updateEditProfile"));
		updateEditProfile.click();
		utils.longWaitInSeconds(3);
	}

	public void setEmail(String email) {
		driver.findElement(locators.get("guestTimeLine.emailtextbox")).clear();
		WebElement emailtextbox = driver.findElement(locators.get("guestTimeLine.emailtextbox"));
		emailtextbox.sendKeys(email);
	}

	public void setPhone(String phone) {
		driver.findElement(locators.get("guestTimeLine.editphoneNumber")).clear();
		WebElement editphoneNumber = driver.findElement(locators.get("guestTimeLine.editphoneNumber"));
		editphoneNumber.sendKeys(phone);
	}

	public void saveProfile() {
		WebElement updateEditProfile = driver.findElement(locators.get("guestTimeLine.updateEditProfile"));
		updateEditProfile.click();
		utils.longWaitInSeconds(3);
		WebElement successMsg = driver.findElement(locators.get("guestTimeLine.successAlert"));
		Assert.assertTrue(successMsg.isDisplayed(), "Success message is not displayed");
	}

	public String validateSuccessMessage() throws InterruptedException {
		String val = "";
		Thread.sleep(2000);
		WebElement successMsg = driver.findElement(locators.get("guestTimeLine.successAlert"));
		if (successMsg.isDisplayed()) {
			val = successMsg.getText();
		}
		return val;
	}

	public String rewardDisplayed(String message) {
		utils.refreshPage();
//		utils.waitTillPagePaceDone();
		String xpath = utils.getLocatorValue("guestTimeLine.customRedeemAmount").replace("temp", message);
		WebElement ele = driver.findElement(By.xpath(xpath));
		String val = ele.getText();
		return val;
	}

	public boolean verifyVoidRedemption() {
		boolean flag = false;
		boolean isdisplayed = true;
		int attempts = 0;
		while (attempts <= 30) {
			utils.refreshPage();
			WebElement voidRedemption = driver.findElement(locators.get("guestTimeLine.voidRedemption"));
			isdisplayed = voidRedemption.isDisplayed();
			if (isdisplayed) {
				flag = true;
				break;
			} else {
				logger.info("Void redemption is not found at attempt" + attempts);
				TestListeners.extentTest.get().info("Void redemption is not found at attempt" + attempts);
			}
			attempts++;
		}
		return flag;

	}

	public Boolean redeemedRewardDeleted(String message) {
		utils.refreshPage();
		boolean notDisplayed = false;
		utils.implicitWait(5);
		try {
			String xpath = utils.getLocatorValue("guestTimeLine.customRedeemAmount").replace("temp", message);
			WebElement ele = driver.findElement(By.xpath(xpath));
			notDisplayed = utils.checkElementPresent(ele);
		} catch (Exception e) {
			logger.info("Currency redeemption has been removed from the timeline");
			TestListeners.extentTest.get().info("Currency redeemption has been removed from the timeline");
		}
		utils.implicitWait(50);
		return notDisplayed;
	}

	public int getRedeemableRewardOrBankedCurrencyCount(String amountType) {
		int count = 0;
		String xpath = utils.getLocatorValue("guestTimeLine.rewardOrCurrencyLabel").replace("$type@", amountType);
		WebElement rewardOrCurrencyLabel = driver.findElement(By.xpath(xpath));
		rewardOrCurrencyLabel.isDisplayed();
		count = Integer.parseInt(rewardOrCurrencyLabel.getText().replace("$", "").replace(".00", ""));
		logger.info(amountType + " count is: " + count);
		TestListeners.extentTest.get().info(amountType + " count is: " + count);
		return count;
	}

	public String getCampaignNameByNotificationsAPI(String campaignName, String client, String secret, String token)
			throws InterruptedException {
		String campName = "";
		int attempts = 0;
		Response response = null;
		while (attempts <= 25) {
			try {
				response = pageObj.endpoints().Api2FetchNotifications(client, secret, token);
				Assert.assertEquals(response.getStatusCode(), 200,
						"Status code did not matched for fetch notifications");
				campName = utils.getJsonReponseKeyValueFromJsonArrayWithoutArrayName(response, "message", campaignName);

				if (campName.equalsIgnoreCase(campaignName) || campName.contains(campaignName)) {
					logger.info("Campaign Name " + campName + " matched on the timeline");
					TestListeners.extentTest.get().pass("Campaign Name " + campName + " matched on the timeline");
					break;
				} else {
					throw new Exception("Campaign Name did not matched");
				}
			} catch (Exception e) {
				logger.info("Campaign Name did not matched in api response... polling count is : " + attempts);
				TestListeners.extentTest.get()
						.info("Campaign Name did not matched in api response... polling count is : " + attempts);
				utils.longWaitInSeconds(5);
			}
			attempts++;
		}
		return campName;
	}

	public String getCampaignNameByNotificationsAPILongPolling(String campaignName, String client, String secret,
			String token) throws InterruptedException {
		// this method try for 5 mins
		String campName = "";
		int attempts = 0;
		Response response = null;
		while (attempts <= 45) {
			try {
				response = pageObj.endpoints().Api2FetchNotifications(client, secret, token);
				Assert.assertEquals(response.getStatusCode(), 200,
						"Status code did not matched for fetch notifications");
				campName = utils.getJsonReponseKeyValueFromJsonArrayWithoutArrayName(response, "message", campaignName);

				if (campName.equalsIgnoreCase(campaignName)) {
					logger.info("Campaign Name " + campName + " matched on the timeline");
					TestListeners.extentTest.get().pass("Campaign Name " + campName + " matched on the timeline");
					break;
				} else {
					throw new Exception("Campaign Name did not matched");
				}
			} catch (Exception e) {
				logger.info("Campaign Name did not matched in api response... polling count is : " + attempts);
				TestListeners.extentTest.get()
						.info("Campaign Name did not matched in api response... polling count is : " + attempts);
				utils.longWaitInSeconds(7);
			}
			attempts++;
		}
		// TestListeners.extentTest.get().info("response is : " + response.asString());
		return campName;
	}

	public String getCampaignNameByNotificationsAPIVeryLongPolling(String campaignName, String client, String secret,
			String token) throws InterruptedException {
		// this method try for 5 mins
		String campName = "";
		int attempts = 0;
		Response response = null;
		while (attempts <= 55) {
			try {
				response = pageObj.endpoints().Api2FetchNotifications(client, secret, token);
				Assert.assertEquals(response.getStatusCode(), 200,
						"Status code did not matched for fetch notifications");
				campName = utils.getJsonReponseKeyValueFromJsonArrayWithoutArrayName(response, "message", campaignName);

				if (campName.equalsIgnoreCase(campaignName)) {
					logger.info("Campaign Name " + campName + " matched on the timeline");
					TestListeners.extentTest.get().pass("Campaign Name " + campName + " matched on the timeline");
					break;
				} else {
					throw new Exception("Campaign Name did not matched");
				}
			} catch (Exception e) {
				logger.info("Campaign Name did not matched in api response... polling count is : " + attempts);
				TestListeners.extentTest.get()
						.info("Campaign Name did not matched in api response... polling count is : " + attempts);
				utils.longWaitInSeconds(7);
			}
			attempts++;
		}
		// TestListeners.extentTest.get().info("response is : " + response.asString());
		return campName;
	}

	public String getCampaignNameByNotificationsAPIShortPoll(String campaignName, String client, String secret,
			String token) throws InterruptedException {
		String campName = "";
		int attempts = 0;
		Response response = null;
		while (attempts <= 4) {
			try {
				response = pageObj.endpoints().Api2FetchNotifications(client, secret, token);
				Assert.assertEquals(response.getStatusCode(), 200,
						"Status code did not matched for fetch notifications");
				campName = utils.getJsonReponseKeyValueFromJsonArrayWithoutArrayName(response, "message", campaignName);

				if (campName.equalsIgnoreCase(campaignName)) {
					logger.info("Campaign Name " + campName + " matched on the timeline");
					TestListeners.extentTest.get().pass("Campaign Name " + campName + " matched on the timeline");
					break;
				} else {
					throw new Exception("Campaign Name did not matched");
				}
			} catch (Exception e) {
				logger.info("Campaign Name did not matched in api response... polling count is : " + attempts);
				TestListeners.extentTest.get()
						.info("Campaign Name did not matched in api response... polling count is : " + attempts);
				utils.longWaitInSeconds(5);
			}
			attempts++;
		}
		TestListeners.extentTest.get().info("response is : " + response.asString());
		return campName;
	}

	public String getUserAccountHistory(String campaignName, String client, String secret, String token) {
		Response response = pageObj.endpoints().Api2UserAccountHistory(client, secret, token);
		Assert.assertEquals(response.getStatusCode(), 200, "Status code did not matched for fetch notifications");
		String rewardGiftedName = utils.getJsonReponseKeyValueFromJsonArrayWithoutArrayNameContainsText(response,
				"description", campaignName);
		TestListeners.extentTest.get().info("Reward gifted by campaign Campaign :" + rewardGiftedName);
		return rewardGiftedName;
	}

	public long getAccountHistoryForMassFrequencyCampaign(String campaignName, String client, String secret,
			String token, int expectedCount) {
		String rewardGiftedName = "";
		long actualCount = 0;
		int attempts = 0;
		int maxAttempts = 5;
		int pollingIntervalSeconds = 5;
		Response response = null;

		while (attempts < maxAttempts) {
			try {
				response = pageObj.endpoints().Api2UserAccountHistory(client, secret, token);
				Assert.assertEquals(response.getStatusCode(), 200,
						"Status code did not matched for fetch notifications");

				// Check if reward with campaign name exists in response
				rewardGiftedName = utils.getJsonReponseKeyValueFromJsonArrayWithoutArrayNameContainsText(response,
						"description", campaignName);
				if (rewardGiftedName.contains(campaignName)) {

					List<String> messages = response.jsonPath().getList("description");
					actualCount = messages.stream().filter(msg -> msg != null && msg.contains(campaignName)).count();

					logger.info("Polling attempt {} → Reward found, count: {}", attempts + 1, actualCount);
					TestListeners.extentTest.get()
							.info("Polling attempt " + (attempts + 1) + " → Reward found, count: " + actualCount);

					// BOTH CONDITIONS MET
					if (actualCount == expectedCount) {
						logger.info("Reward {} matched with expected count {}", campaignName, expectedCount);
						TestListeners.extentTest.get()
								.pass("Reward " + rewardGiftedName + " matched with expected count " + expectedCount);
						return actualCount;
					}
				} else {
					logger.info("Campaign name not present yet in response");
				}

			} catch (Exception e) {
				logger.info("Polling attempt {} failed, retrying...", attempts + 1);
				TestListeners.extentTest.get().info("Polling attempt " + (attempts + 1) + " failed, retrying...");
			}

			attempts++;
			utils.longWaitInSeconds(pollingIntervalSeconds);
		}
		// MAX POLLING TIME LAPSED
		return actualCount;
	}

	public long getCampNameTimlineForMassFrequencyCampaign(String campaignName, String client, String secret,
			String token, int expectedCount) {
		String campName = "";
		long actualCount = 0;
		int attempts = 0;
		int maxAttempts = 20;
		int pollingIntervalSeconds = 5;
		Response response = null;

		while (attempts < maxAttempts) {
			try {
				response = pageObj.endpoints().Api2FetchNotifications(client, secret, token);
				Assert.assertEquals(response.getStatusCode(), 200,
						"Status code did not matched for fetch notifications");

				// Check if campaign name exists in response
				campName = utils.getJsonReponseKeyValueFromJsonArrayWithoutArrayName(response, "message", campaignName);
				if (campName != null && (campName.equalsIgnoreCase(campaignName) || campName.contains(campaignName))) {

					List<String> messages = response.jsonPath().getList("message");
					actualCount = messages.stream().filter(msg -> msg != null && msg.contains(campaignName)).count();

					logger.info("Polling attempt {} → Campaign found, count: {}", attempts + 1, actualCount);
					TestListeners.extentTest.get()
							.info("Polling attempt " + (attempts + 1) + " → Campaign found, count: " + actualCount);

					// BOTH CONDITIONS MET
					if (actualCount == expectedCount) {
						logger.info("Campaign {} matched with expected count {}", campaignName, expectedCount);
						TestListeners.extentTest.get()
								.pass("Campaign " + campaignName + " matched with expected count " + expectedCount);
						return actualCount;
					}
				} else {
					logger.info("Campaign name not present yet in response");
				}

			} catch (Exception e) {
				logger.info("Polling attempt {} failed, retrying...", attempts + 1);
				TestListeners.extentTest.get().info("Polling attempt " + (attempts + 1) + " failed, retrying...");
			}

			attempts++;
			utils.longWaitInSeconds(pollingIntervalSeconds);
		}

		// MAX POLLING TIME LAPSED
		return actualCount;
	}

	public void pingSessionforLongWait(int minute) {
		// Grid has a default session timeout of 300 seconds, where the session can be
		// on a stale state until it is killed or lost after 300 secs
		int attempts = 1;
		while (attempts <= minute) {
			utils.longWaitInSeconds(60);
			utils.refreshPageWithCurrentUrl();
			logger.info("Timeline pinged... polling count is : " + attempts);
			TestListeners.extentTest.get().info("Timeline pinged...... polling count is : " + attempts);
			attempts++;
		}
	}

	public int getEventTimeRedemptionTimeFromApi(Response response) {

		String value = response.jsonPath().get("status").toString();
		String val[] = value.split("by");
		String count = val[0].replaceAll("[^0-9 , :]", "");
		String rawTime[] = count.split(":");
		int time = Integer.parseInt(rawTime[1].trim());
		return time;
	}

	public int getRedeemablePointOnPrintTimeline() {
		WebElement clickPrintTimeline = driver.findElement(locators.get("guestTimeLine.clickPrintTimeline"));
		utils.scrollToElement(driver, clickPrintTimeline);
		clickPrintTimeline.isDisplayed();
		utils.clickByJSExecutor(driver, clickPrintTimeline);
//		driver.findElement(guestTimelineLocators.get("clickPrintTimeline")).click();
		logger.info("Click on Print Timeline button");
		TestListeners.extentTest.get().info("Click on Print Timeline button");
		utils.switchToWindow();
		WebElement getPointPrintTimeline = driver.findElement(locators.get("guestTimeLine.getPointPrintTimeline"));
		getPointPrintTimeline.isDisplayed();
		String pointPresent = getPointPrintTimeline.getText();
		logger.info("Points visible on Print Timeline is " + pointPresent);
		TestListeners.extentTest.get().info("Points visible on Print Timeline is " + pointPresent);
		utils.switchToParentWindow();
		return Integer.parseInt(pointPresent);

	}

	public void clickOnPrintTimeLine() {
		WebElement clickPrintTimeline = driver.findElement(locators.get("guestTimeLine.clickPrintTimeline"));
		clickPrintTimeline.isDisplayed();
		clickPrintTimeline.click();
		logger.info("Click on Print Timeline button");
		TestListeners.extentTest.get().info("Click on Print Timeline button");
	}

	public void moveToPointOnPrintTimeline() {
		WebElement clickPrintTimeline = driver.findElement(locators.get("guestTimeLine.clickPrintTimeline"));
		clickPrintTimeline.isDisplayed();
		clickPrintTimeline.click();
		logger.info("Click on Print Timeline button");
		TestListeners.extentTest.get().info("Click on Print Timeline button");
		utils.switchToWindow();
	}

	public String getPageContentsPrintTimeline() {
		WebElement bodyElement = driver.findElement(By.xpath("/html/body"));
		String pageContents = bodyElement.getText();
		logger.info("Print timeline details are :" + pageContents);
		utils.closeSeconedTabandSwitchToParentWindow();
		return pageContents;
	}

	public void refreshAccount() {
		WebElement refreshAccount = driver.findElement(locators.get("guestTimeLine.clickRefreshAccount"));
		refreshAccount.isDisplayed();
		utils.StaleElementclick(driver, refreshAccount);
		logger.info("Clicked on Refresh Account button");
		TestListeners.extentTest.get().info("Clicked on Refresh Account button");
//		utils.waitTillPagePaceDone();
//		String message = utils.getSuccessMessage();
		String message = utils.getLocator("locationPage.getSuccessErrorMessage").getText();
		Assert.assertEquals(message, "Account will be refreshed shortly.");
		logger.info("'" + message + "' is displayed");
		TestListeners.extentTest.get().info("'" + message + "' is displayed");
	}

	public String getRedeemablePieChartDetails() {
		WebElement redeemablePieChart = driver.findElement(locators.get("guestTimeLine.redeemablePieChart"));
		String value = redeemablePieChart.getText();
		String pointsValue[] = value.split("Redeemable");
		String redeemablePoints = pointsValue[1].trim();
		logger.info("Redeemable pie chart value is :" + value);
		TestListeners.extentTest.get().info("Redeemable pie chart value is :" + value);
		return redeemablePoints;
	}

	public void clickOnSuspiciousActivitiesInFraudSuspectPage() {
		WebElement suspiciousActivitiesButton = driver
				.findElement(locators.get("guestTimeLine.suspiciousActivitiesButton"));
		suspiciousActivitiesButton.click();
		utils.longwait(2);
	}

	public void deleteUserFromSuspiciousActivitiesInFraudSuspectPage(String userEmail) {
		String xpath = utils.getLocatorValue("guestTimeLine.deleteButtonForSuspiciousActivitiesUser")
				.replace("${userEmail}", userEmail);
		utils.longwait(3);
		WebElement wEle = driver.findElement(By.xpath(xpath));
		utils.waitTillElementToBeClickable(wEle);
		wEle.click();
		utils.longwait(2);
		driver.switchTo().alert().accept();
		utils.longwait(4);
		String successMsg = pageObj.whitelabelPage().getSuccessMsg();
		Assert.assertEquals(successMsg, "Suspect destroyed");
		logger.info("success msg is displayed");
	}

	public void cleanAllUsersFromSuspiciousActivitiesTab() {
		utils.implicitWait(1);
		List<WebElement> deleteFraudUsersXpathList = driver
				.findElements(locators.get("guestTimeLine.deleteFraudUsersXpath"));
		while (deleteFraudUsersXpathList.size() != 0) {
			deleteFraudUsersXpathList.get(0).click();
			utils.longwait(2);
			driver.switchTo().alert().accept();
			utils.longwait(4);
			String successMsg = pageObj.whitelabelPage().getSuccessMsg();
			Assert.assertEquals(successMsg, "Suspect destroyed");
			logger.info("success msg is displayed");
			deleteFraudUsersXpathList = driver.findElements(locators.get("guestTimeLine.deleteFraudUsersXpath"));
		}
		utils.implicitWait(50);

	}

	public void clickOnDownloadAndroidPassButton() {
		WebElement downloadAndroidPass = driver.findElement(locators.get("guestTimeLine.downloadAndroidPass"));
		downloadAndroidPass.click();
		logger.info("Clicked on download android pass button");
		TestListeners.extentTest.get().info("Clicked on download android pass button");
	}

	public void signInWithGoogleTestAccount(String email, String pwd) {
		try {
			utils.waitTillCompletePageLoad();
			WebElement emailField = driver.findElement(locators.get("guestTimeLine.emailField"));
			utils.waitTillElementToBeClickable(emailField);
			emailField.sendKeys(email);
			logger.info("test account email is Entered");
			TestListeners.extentTest.get().info("test account email is Entered");
			WebElement nextButton = driver.findElement(locators.get("guestTimeLine.nextButton"));
			nextButton.click();
			WebElement passwordField = driver.findElement(locators.get("guestTimeLine.passwordField"));
			utils.waitTillElementToBeClickable(passwordField);
			passwordField.sendKeys(pwd);
			logger.info("test account password is Entered");
			TestListeners.extentTest.get().info("test account password is Entered");
			WebElement nextButton2 = driver.findElement(locators.get("guestTimeLine.nextButton"));
			utils.waitTillElementToBeClickable(nextButton2);
			nextButton2.click();
			utils.waitTillCompletePageLoad();

		} catch (Exception e) {
			logger.error("Some issue occured while trying to sign in with google test account: " + e);
			TestListeners.extentTest.get()
					.info("Some issue occured while trying to sign in with google test account: " + e);
			Assert.fail("Some issue occured while trying to sign in with google test account: " + e);
		}
	}

	public void navigateToAndroidPassOfUser() throws Exception {
		clickOnDownloadAndroidPassButton();
		ArrayList<String> chromeTabs = new ArrayList<String>(driver.getWindowHandles());
		driver.switchTo().window(chromeTabs.get(1));
		signInWithGoogleTestAccount(prop.getProperty("testAccountEmail"),
				utils.decrypt(prop.getProperty("testAccountPwd")));
		utils.longWaitInSeconds(2);
		if (driver.getCurrentUrl().contains(prop.getProperty("googlePaySaveUrl"))) {
			logger.info("Android pass of user is loaded successfully");
			TestListeners.extentTest.get().info("Android pass of user is loaded successfully");
		} else {
			logger.info("Some issue occured, Android pass of user is not loaded ");
			TestListeners.extentTest.get().info("Some issue occured, Android pass of user is not loaded ");
		}
	}

	public void clickOnAddToWalletButton() {
		utils.waitTillCompletePageLoad();
		WebElement addToWallet = driver.findElement(locators.get("guestTimeLine.addToWallet"));
		utils.waitTillElementToBeVisible(addToWallet);
		addToWallet.click();

		// comment this wait for location viewLoyaltyCard card is not present in ui now
		logger.info("Successfully clicked on add to wallet button");
		TestListeners.extentTest.get().info("Successfully clicked on add to wallet button");
	}

	public void clickOnRemoveFromWalletButton() {
		WebElement removePass = driver.findElement(locators.get("guestTimeLine.removePass"));
		removePass.click();
		utils.waitTillCompletePageLoad();

		WebElement removeBtn = driver.findElement(locators.get("guestTimeLine.removebutton"));

		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript("arguments[0].click();", removeBtn);

		logger.info("Successfully clicked on remove from wallet button");
		TestListeners.extentTest.get().info("Successfully clicked on remove from wallet button");
	}

	public void viewFullAndroidLoyaltyPassOfUser() throws InterruptedException {
		utils.waitTillCompletePageLoad();
		WebElement viewLoyaltyCard = driver.findElement(locators.get("guestTimeLine.viewLoyaltyCard"));
		viewLoyaltyCard.click();
		utils.waitTillCompletePageLoad();
		WebElement removePass = driver.findElement(locators.get("guestTimeLine.removePass"));
		utils.waitTillElementToBeClickable(removePass);

		WebElement cardBody = driver.findElement(locators.get("guestTimeLine.cardBody"));
		if (driver.getCurrentUrl().contains(prop.getProperty("googleWalletUrl"))
				&& utils.checkElementPresent(cardBody)) {
			logger.info("Android Loyalty pass of user is loaded successfully and fully visible ");
			TestListeners.extentTest.get()
					.info("Android Loyalty pass of user is loaded successfully and fully visible ");
		} else {
			logger.info("Some issue occured, Android Loyalty pass of user is not loaded ");
			TestListeners.extentTest.get().info("Some issue occured, Android Loyalty pass of user is not loaded ");
		}
	}

	public void checkUserIdentifierOnPass(String userIdentifier) {
		String Xpath = (utils.getLocatorValue("guestTimeLine.userIdentifierOnPass")).replace("$userIdentifier",
				userIdentifier);
		WebElement userIdentifierOnPass = driver.findElement(By.xpath(Xpath));
		if (utils.checkElementPresent(userIdentifierOnPass)) {
			logger.info("User Identifier is successfully matched i.e. : " + userIdentifier);
			TestListeners.extentTest.get().info("User Identifier is successfully matched i.e. : " + userIdentifier);
		} else {
			logger.info("User Identifier does not match ");
			TestListeners.extentTest.get().info("User Identifier does not match ");
		}
	}

	public String getDetailsOnAndroidPassByLabel(String labelName) {
		utils.waitTillCompletePageLoad();
		String Xpath = (utils.getLocatorValue("guestTimeLine.labelValueOnPass")).replace("$labelName", labelName);
		WebElement element = driver.findElement(By.xpath(Xpath));
		String labelValueOnPass = element.getText();
//		String labelValueOnPass = driver.findElement(By.xpath(Xpath)).getText();
		logger.info("Details on android pass is : " + labelValueOnPass);
		TestListeners.extentTest.get().info("Details on android pass is : " + labelValueOnPass);
		return labelValueOnPass;
	}

	public String getDetailsOnAndroidOfferPassByLabel(String labelName) {
		utils.waitTillCompletePageLoad();

		ArrayList<String> chromeTabs = new ArrayList<String>(driver.getWindowHandles());
		driver.switchTo().window(chromeTabs.get(1));

		String Xpath = (utils.getLocatorValue("guestTimeLine.labelValueOnPass")).replace("$labelName", labelName);
		WebElement labelValueOnPass = driver.findElement(locators.get("guestTimeLine.labelValueOnPass"));
		utils.waitTillVisibilityOfElement(labelValueOnPass, "Expires");
		WebElement element = driver.findElement(By.xpath(Xpath));
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript("arguments[0].click();", element);

		String value = element.getText();

//		String labelValueOnPass = driver.findElement(By.xpath(Xpath)).getText();
		logger.info("Details on android pass is : " + value);
		TestListeners.extentTest.get().info("Details on android pass is : " + value);
		return value;
	}

	public String getRedemptionChannel() {
		String redemptionChannel = "";
		boolean isdisplayed = true;
		int attempts = 0;
		while (attempts <= 30) {
			utils.refreshPage();
			WebElement redemptionChannelEl = driver.findElement(locators.get("guestTimeLine.redemptionChannel"));
			isdisplayed = redemptionChannelEl.isDisplayed();
			if (isdisplayed) {
				redemptionChannel = redemptionChannelEl.getText();
				break;
			} else {
				logger.info("Redemption Channel is not found at attempt" + attempts);
				TestListeners.extentTest.get().info("Redemption Channel is not found at attempt" + attempts);
			}
			attempts++;

		}
		return redemptionChannel;
	}

	public String getRewardExpiryDate(String redeemableNmae) {
		clickReward();
		String Xpath = (utils.getLocatorValue("guestTimeLine.giftedRedeemableExpiryDate")).replace("$name",
				redeemableNmae);
		WebElement element = driver.findElement(By.xpath(Xpath));
		String val = element.getText();
		String date = val.replace("Expiry:", "");
		return date.trim();
	}

	public String getCheckinId() throws InterruptedException {
		String checkinId = "";
		int attempts = 0;
		while (attempts < 5) {
			refreshTimeline();
			Thread.sleep(2000);
			try {
				// Extract the text from the element
				WebElement checkinIdElement = driver.findElement(locators.get("guestTimeLine.getCheckinId"));
				if (checkinIdElement.isDisplayed()) {
					// Parse the checkin ID from the text
					String text = checkinIdElement.getText();
					checkinId = text.split("Checkin Id: ")[1];
					logger.info("Element:" + checkinIdElement + " title is present");
					break;
				}
			} catch (Exception e) {
				logger.info("Element is not present ");
			}
			attempts++;
		}
		return checkinId;
	}

	public String getGuestIdFromUrl() {
		// Get the current URL
		String currentUrl = driver.getCurrentUrl();

		// Extract the guest ID from the URL
		String guestId = currentUrl.substring(currentUrl.lastIndexOf('/') + 1);

		return guestId;
	}

	// This method is used to extract links from Gmail body
	public List<String> extractLinksFromGmailBody(String gmailBody) {
		List<String> links = new ArrayList<>();
		String regex = "https:\\/\\/[^\\s]+"; // Regex to match URLs
		Matcher match = Pattern.compile(regex).matcher(gmailBody);
		while (match.find()) {
			links.add(match.group());
		}
		return links;
	}

	// This method is used to click on Android Personalize Pass button.
	public void clickOnPersonalizePassBtn() throws InterruptedException {
		Thread.sleep(3000);
		String currentUrl = driver.getCurrentUrl();
		driver.get(currentUrl);
		Thread.sleep(3000);
		WebElement personalizePassBtn = driver.findElement(locators.get("guestTimeLine.personalizePassBtn"));
		utils.waitTillElementToBeVisible(personalizePassBtn);
		personalizePassBtn.click();

		Set<String> chromeTabs = driver.getWindowHandles();
		for (String window : chromeTabs) {
			driver.switchTo().window(window);
		}
		WebElement enrollmentPassHeading = driver.findElement(locators.get("guestTimeLine.enrollmentPassHeading"));
		utils.waitTillVisibilityOfElement(enrollmentPassHeading, "Google pass enrollment");
		logger.info("Successfully clicked on Android Personalize Pass button");
		TestListeners.extentTest.get().info("Successfully clicked on Android Personalize Pass button");

	}

	// This method is used to enter Details On Personalize Pass Page
	public String enterDetailsOnPersonalizePassPage(String firstName, String lastName, String email) {
		String currentUrl = "";
		try {
			utils.waitTillPagePaceDone();
			JavascriptExecutor js = (JavascriptExecutor) driver;

			WebElement firstNameField = driver.findElement(locators.get("guestTimeLine.firstNameFieldPersonalizePass"));
			js.executeScript("arguments[0].scrollIntoView(true);", firstNameField);
			firstNameField.sendKeys(firstName);
			logger.info("First Name is Entered");

			WebElement lastNameField = driver.findElement(locators.get("guestTimeLine.lastNameFieldPersonalizePass"));
			js.executeScript("arguments[0].scrollIntoView(true);", lastNameField);
			lastNameField.sendKeys(lastName);
			logger.info("Last Name is Entered");

			WebElement emailField = driver.findElement(locators.get("guestTimeLine.emailFieldPersonalizePass"));
			js.executeScript("arguments[0].scrollIntoView(true);", emailField);
			emailField.sendKeys(email);
			logger.info("Email is Entered");

//			String TCUrl = utils.getLocatorValue("guestTimeLine.termsAndConditionUrl").replace("$label",
//					"Terms & Conditions");
//			driver.findElement(By.xpath(TCUrl));
//			String privacyPolicyUrl = utils.getLocatorValue("guestTimeLine.termsAndConditionUrl").replace("$label",
//					"Privacy Policy.");
//			driver.findElement(By.xpath(privacyPolicyUrl));

			WebElement termsCheckbox = driver.findElement(locators.get("guestTimeLine.termsAndConditionCheckbox"));
			js.executeScript("arguments[0].scrollIntoView(true);", termsCheckbox);
			js.executeScript("arguments[0].click();", termsCheckbox);
			logger.info("Terms and Condition Checkbox is clicked");

			// get current url
			currentUrl = driver.getCurrentUrl();

			Thread.sleep(3000);
			WebElement submitButton = driver.findElement(locators.get("guestTimeLine.submitBtnPersonalizePass"));
			js.executeScript("arguments[0].scrollIntoView(true);", submitButton);
			js.executeScript("arguments[0].click();", submitButton);
			logger.info("Submit Button is Clicked");

			utils.waitTillCompletePageLoad();
			Thread.sleep(3000);
			WebElement removePass = driver.findElement(locators.get("guestTimeLine.removePass"));
			utils.waitTillElementToBeClickable(removePass);

			logger.info("Successfully entered details on Android Personalize Pass page");
			TestListeners.extentTest.get().info("Successfully entered details on Android Personalize Pass page");
		} catch (Exception e) {
			logger.error("Some issue occured while entering details on Android Personalize Pass page: " + e);
			TestListeners.extentTest.get()
					.fail("Some issue occured while entering details on Android Personalize Pass page: " + e);
			Assert.fail("Some issue occured while entering details on Android Personalize Pass page: " + e);
		}
		return currentUrl;
	}

	public boolean verifyDiscountBasketVariablePresent(Response apiResponse, String variable, String variable2,
			String actualValue) {
		boolean flag = false;
		List<Object> obj = new ArrayList<Object>();
		int j = 0;
		String expectedValue;

		obj = apiResponse.jsonPath().getList(variable);
		for (int i = 0; i < obj.size(); i++) {
			expectedValue = apiResponse.jsonPath().getString(variable + "[" + i + "]." + variable2);
			if (expectedValue.contains(actualValue)) {
				flag = true;
				break;
			}
		}
		return flag;
	}

	public boolean verifyRedemptionCode(String redemptionCode) {
		// this method is used to verify the redemption code is displayed on the guest
		// timeline
		String xpath = utils.getLocatorValue("guestTimeLine.getRedemptionCode").replace("${temp}", redemptionCode);
		int counter = 0;
		boolean flag = false;
		utils.implicitWait(3);
		do {
			// utils.waitTillPagePaceDone();
			try {
				WebElement getRedemptionCode = driver.findElement(By.xpath(xpath));
				if (getRedemptionCode.isDisplayed()) {
					flag = true;
					break;
				}
			} catch (Exception e) {
				utils.refreshPage();
				logger.info("Redemption code is not found on the guest timeline at attempt: " + counter);
				TestListeners.extentTest.get()
						.info("Redemption code is not found on the guest timeline at attempt: " + counter);
				counter++;
			}
		} while (counter < 30);
		utils.implicitWait(50);
		return flag;

	}

	public boolean verifyChallengeCampaignAppearedInDrpDwn(String giftTypes, String challengeName) {
		boolean flag = false;
		utils.waitTillPagePaceDone();
		WebElement messageGiftBtn = driver.findElement(locators.get("guestTimeLine.messageGiftBtn"));
		utils.waitTillInVisibilityOfElement(messageGiftBtn, "");
		utils.clickByJSExecutor(driver, messageGiftBtn);

		WebElement selectGitTypeFromDropdown = driver
				.findElement(locators.get("guestTimeLine.selectGitTypeFromDropdown"));
		utils.selectDrpDwnValue(selectGitTypeFromDropdown, giftTypes);
		WebElement challengeDropdown = driver
				.findElement(locators.get("guestTimeLine.selectChallengeCampForGiftingDropdown"));
		Select select = new Select(challengeDropdown);
		List<WebElement> options = select.getOptions();
		for (WebElement option : options) {
			if (option.getText().equals(challengeName)) {
				flag = true;
			}
		}
		return flag;
	}

	public int getGiftedSubscriptionID() {
		WebElement getActiveSubscriptionIDXpath = driver
				.findElement(locators.get("guestTimeLine.getActiveSubscriptionIDXpath"));
		selUtils.waitTillVisibilityOfElement(getActiveSubscriptionIDXpath, "Active | Subscription ID: Text");
		String subID = "0";
		String rst = getActiveSubscriptionIDXpath.getText();
		String[] rstArray = rst.replace("|", "#").split("#");
		Pattern p = Pattern.compile("\\d+");
		Matcher m = p.matcher(rstArray[1]);
		// Matcher m = p.matcher(rstArray[1]);
		while (m.find()) {
			subID = m.group();
		}
		int actualSubscriptionID = Integer.parseInt(subID);
		return actualSubscriptionID;
	}

	public boolean searchUserInLockedAccountTab(String text, int noOfAttempts) throws InterruptedException {
		int attempts = 0;
		boolean status = false;
		while (attempts < noOfAttempts) {
			refreshTimeline();
			selUtils.implicitWait(20);
			utils.longwait(5000);
			WebElement searchBox = driver.findElement(locators.get("guestTimeLine.searchBox"));
			searchBox.click();
			searchBox.clear();
			WebElement searchBoxForSendKeys = driver.findElement(locators.get("guestTimeLine.searchBox"));
			searchBoxForSendKeys.sendKeys(text);
			searchBoxForSendKeys.sendKeys(Keys.ENTER);
			try {
				WebElement ele = driver.findElement(locators.get("guestTimeLine.guestName"));
				status = utils.checkElementPresent(ele);
				if (status) {
					logger.info("Searched locked account user is present");
					TestListeners.extentTest.get().pass("Searched locked account user is present");
					break;
				}
			} catch (Exception e) {

				attempts++;
			}
		}
		selUtils.implicitWait(50);
		return status;
	}

	public String checkChallengeCampaignIcon(String cname) throws InterruptedException {
		String src = "";
		utils.waitTillPagePaceDone();
		utils.implicitWait(5);
		int attempts = 0;
		while (attempts < 10) {
			try {
				String xpath = utils.getLocatorValue("guestTimeLine.completedChallengeIcon").replace("$campName",
						cname);
				WebElement completedChallengeIcon = driver.findElement(By.xpath(xpath));
				utils.waitTillElementToBeClickable(completedChallengeIcon);
				src = completedChallengeIcon.getAttribute("src");
				if (src != "") {
					break;
				}
			} catch (Exception e) {
				logger.info("Challenge campaign icon src element is not present or status did not matched...");
				utils.refreshPage();
				utils.longWaitInSeconds(5);
			}
			attempts++;
		}
		utils.implicitWait(50);
		return src;
	}

	public boolean verifyRewardCredit(String rewardValue) {
		boolean flag = false;
		String xpath = utils.getLocatorValue("guestTimeLine.rewardCredit").replace("$rewardValue", rewardValue);
		int attempts = 0;
		while (attempts < 5) {
			try {
				WebElement rewardCredit = driver.findElement(By.xpath(xpath));
				rewardCredit.isDisplayed();
				flag = true;
				break;
			} catch (Exception e) {
				logger.info("Element is not present or Reward Credit did not matched...");
				utils.refreshPage();
				utils.longWaitInSeconds(3);
			}
			attempts++;
		}
		return flag;
	}

	public void clickLoyaltyCards() {
		WebElement loyaltyCardBtn = driver.findElement(locators.get("guestTimeLine.loyaltyCardBtn"));
		loyaltyCardBtn.click();
		logger.info("Clicked Loyalty Card Tab");
		TestListeners.extentTest.get().info("Clicked Loyalty Card Tab");
	}

	public void scrollToLoyaltyCard() {
		WebElement cardData = driver.findElement(locators.get("guestTimeLine.loyaltyCardCardHeading"));
		selUtils.scrollToElement(cardData);
		logger.info("Scrolled to Loyalty Card");
		TestListeners.extentTest.get().info("Scrolled to Loyalty Card");
	}

	public boolean matchLoyaltyCardNumber(String cardNumber) {
		boolean isMatched = false;
		try {
			String xpath = utils.getLocatorValue("guestTimeLine.loyaltyCardTable");
			List<WebElement> rows = driver.findElements(By.xpath(xpath));
			for (WebElement row : rows) {
				List<WebElement> cols = row.findElements(By.tagName("td"));
				String actCardNumber = cols.get(0).getText();
				logger.info("Actual card number is: " + actCardNumber);
				if (actCardNumber.equals(cardNumber)) {
					logger.info("Card number matched with ");
					TestListeners.extentTest.get().info("Card number matched");
					isMatched = true;
					break;
				}
			}
		} catch (Exception e) {
			logger.error("Error in verifying card number" + e);
			TestListeners.extentTest.get().fail("Error in verifying card number" + e);
		}
		return isMatched;
	}

	public boolean matchLoyaltyCardStatus(String status) {
		boolean isMatched = false;
		try {
			String xpath = utils.getLocatorValue("guestTimeLine.loyaltyCardTable");
			List<WebElement> rows = driver.findElements(By.xpath(xpath));
			for (WebElement row : rows) {
				List<WebElement> cols = row.findElements(By.tagName("td"));
				String actStatus = cols.get(1).getText();
				logger.info("Actual card status is: " + actStatus);
				if (actStatus.equals(status)) {
					logger.info("Card status matched");
					TestListeners.extentTest.get().info("Card status matched");
					return true;
				}
			}
		} catch (Exception e) {
			logger.error("Error in verifying card status" + e);
			TestListeners.extentTest.get().fail("Error in verifying card status" + e);
		}
		return isMatched;
	}

	public void verifySortedRedeemableNamesFromResponse(Response response) {
		// This method verifies if the list of redeemable names extracted from the JSON
		// response is sorted and returns a boolean result.

		// Parse the JSON response
		JSONArray jsonArray = new JSONArray(response.asString());

		// List to store names
		List<String> names = new ArrayList<>();

		// Extract names from the JSON array
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObject = jsonArray.getJSONObject(i);
			String name = jsonObject.getString("name");
			names.add(name);
		}
		List<String> sortedNames = new ArrayList<>(names);
		// sort names list by name alphabetically ignoring case
		sortedNames.sort(String.CASE_INSENSITIVE_ORDER);
		// Assert that the original list is equal to the sorted list
		Assert.assertEquals(names, sortedNames, "The names are not sorted.");
		logger.info("The names are in sorted order.");
		TestListeners.extentTest.get().pass("The names are in sorted order.");

	}

	public boolean isPaymentCardTabPresent() {
		boolean flag = false;
		String xpath = utils.getLocatorValue("guestTimeLine.paymentCardTab");
		try {
			WebElement paymentCardTab = driver.findElement(By.xpath(xpath));
			if (paymentCardTab.isDisplayed()) {
				flag = true;
			}
		} catch (Exception e) {
			logger.info("Payment card tab is not present");
			TestListeners.extentTest.get().info("Payment card tab is not present");
		}
		return flag;
	}

	public boolean verifyRedeemableUnlockedPUR(String redeemableName) {
		String xpath = utils.getLocatorValue("guestTimeLine.redeemableUnlockedOrNot").replace("$redeemableName",
				redeemableName);
		WebElement wele = driver.findElement(By.xpath(xpath));
		String classAttribute = wele.getAttribute("class");
		boolean flag = utils.textContains("success", classAttribute);
		return flag;
	}

	public int getRedeemableRewardCount() {
		int count = 0;
		WebElement redeemableRewardLabel = driver.findElement(locators.get("guestTimeLine.redeemableRewardLabel"));
		redeemableRewardLabel.isDisplayed();
		count = Integer.parseInt(redeemableRewardLabel.getText().replace("$", "").replace(".00", ""));
		logger.info("Redeemable reward count is:" + count);
		TestListeners.extentTest.get().info("Redeemable reward count is:" + count);
		return count;
	}

	public String getRewardEndtDateFromPushNotification(String cname) throws InterruptedException {
		String endDate = null;
		String xpath = utils.getLocatorValue("guestTimeLine.campaignPushNotification").replace("$temp", cname);
		WebElement pushNotification = driver.findElement(By.xpath(xpath));
		String pN = pushNotification.getText();
		String[] name = pN.split(cname);
		endDate = name[1].replaceFirst(":", "").trim();
		logger.info("End date from push notification is: " + endDate);
		TestListeners.extentTest.get().info("End date from push notification is: " + endDate);
		return endDate;
	}

	public boolean areEndDatesEqual(String endDateTime, String rewardEndDate) {
		// Parse endDateTime (assumed PDT)
		DateTimeFormatter endFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a").withLocale(Locale.ENGLISH);
		LocalDateTime endLocal = LocalDateTime.parse(endDateTime, endFormatter);
		ZonedDateTime endZoned = endLocal.atZone(ZoneId.of("America/Los_Angeles")); // PDT

		// Parse rewardEndDate (with zone in string)
		DateTimeFormatter rewardFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy hh:mm a z", Locale.ENGLISH);
		ZonedDateTime rewardZoned = ZonedDateTime.parse(rewardEndDate, rewardFormatter);

		// Compare instants
		return endZoned.toInstant().equals(rewardZoned.toInstant());
	}

	public String getForceRedemptionRewardId(String campaignName) throws URISyntaxException {
		utils.refreshPage();
		utils.waitTillPagePaceDone();
		utils.longWaitInSeconds(15);
		utils.refreshPage();
		utils.waitTillPagePaceDone();
		driver.findElement(
				By.xpath(utils.getLocatorValue("forceRedemptionPage.forceRedeem").replace("${campName}", campaignName)))
				.click();
		utils.waitTillPagePaceDone();
		// get the current url
		String currentUrl = driver.getCurrentUrl();
		String rewardId = getQueryParamValue(currentUrl, "reward_id");
		return rewardId;
	}

	public static String getQueryParamValue(String url, String paramName) throws URISyntaxException {
		URI uri = new URI(url);
		String query = uri.getQuery();
		Map<String, String> queryParams = new HashMap<>();
		for (String param : query.split("&")) {
			String[] keyValue = param.split("=");
			if (keyValue.length > 1) {
				queryParams.put(keyValue[0], keyValue[1]);
			}
		}
		return queryParams.get(paramName);
	}

	public void clickOnMessageGiftBtn() {
		WebElement messageGiftBtn = driver.findElement(locators.get("guestTimeLine.messageGiftBtn"));
		messageGiftBtn.isDisplayed();
		messageGiftBtn.click();
		logger.info("Clicked on Message Gift Button");
		TestListeners.extentTest.get().info("Clicked on Message Gift Button");
	}

	public void messagePointsToUserNew(String subject, String option, String giftTypes, String giftReason) {
		driver.findElement(locators.get("guestTimeLine.subjectTextBox")).clear();
		WebElement subjectTextBox = driver.findElement(locators.get("guestTimeLine.subjectTextBox"));
		subjectTextBox.sendKeys(subject);

		WebElement giftTypeDrp = driver.findElement(locators.get("guestTimeLine.giftTypeDrp"));
		giftTypeDrp.click();
		List<WebElement> giftTypeDrpList = driver.findElements(locators.get("guestTimeLine.giftTypeDrpList"));
		utils.selectListDrpDwnValue(giftTypeDrpList, option);

		driver.findElement(locators.get("guestTimeLine.giftPointsTxtBox")).clear();
		WebElement giftPointsTxtBox = driver.findElement(locators.get("guestTimeLine.giftPointsTxtBox"));
		giftPointsTxtBox.sendKeys(giftTypes);

		driver.findElement(locators.get("guestTimeLine.giftReasonTxtBox")).clear();
		WebElement giftReasonTxtBox = driver.findElement(locators.get("guestTimeLine.giftReasonTxtBox"));
		giftReasonTxtBox.sendKeys(giftReason);
		WebElement messageBtn = driver.findElement(locators.get("guestTimeLine.messageBtn"));
		messageBtn.click();

		logger.info("Gift points messaged to user successfully");
		TestListeners.extentTest.get().pass("Gift points messaged to user successfully");
	}

	public String getGuestVisitCount() {
		WebElement guestVisitCountText = driver.findElement(locators.get("guestTimeLine.guestVisitCountText"));
		String text = guestVisitCountText.getText();
		return text;
	}

	public void messageRewardAmountOrRedeemableToUserNew(String subject, String giftType, String giftReason,
			String redeemable, String amount) {
		driver.findElement(locators.get("guestTimeLine.subjectTextBox")).clear();
		WebElement subjectTextBox = driver.findElement(locators.get("guestTimeLine.subjectTextBox"));
		subjectTextBox.sendKeys(subject);

		WebElement giftTypeDrp = driver.findElement(locators.get("guestTimeLine.giftTypeDrp"));
		giftTypeDrp.click();
		List<WebElement> giftTypeDrpList = driver.findElements(locators.get("guestTimeLine.giftTypeDrpList"));
		utils.selectListDrpDwnValue(giftTypeDrpList, giftType);

		switch (giftType) {
		case "Redeemable":
			WebElement redeemableDrp = driver.findElement(locators.get("guestTimeLine.redeemableDrp"));
			redeemableDrp.click();
			List<WebElement> redeemableDrpList = driver.findElements(locators.get("guestTimeLine.redeemableDrpList"));
			utils.selectListDrpDwnValue(redeemableDrpList, redeemable);
			break;
		case "Reward Amount":
			driver.findElement(locators.get("guestTimeLine.rewardAmountTxtBox")).clear();
			WebElement rewardAmountTxtBox = driver.findElement(locators.get("guestTimeLine.rewardAmountTxtBox"));
			rewardAmountTxtBox.sendKeys(amount);
			break;
		default:
			logger.info("Invalid value for switch case: " + giftType);
			break;
		}
		driver.findElement(locators.get("guestTimeLine.giftReasonTxtBox")).clear();
		WebElement giftReasonTxtBox = driver.findElement(locators.get("guestTimeLine.giftReasonTxtBox"));
		giftReasonTxtBox.sendKeys(giftReason);
		WebElement messageBtn = driver.findElement(locators.get("guestTimeLine.messageBtn"));
		messageBtn.click();
		TestListeners.extentTest.get().info("Gift messaged to user successfully");
	}

	public int getJoinedViaNotificationCount(String joinedViaType) {
		int count = 0;
		String xpath = utils.getLocatorValue("guestTimeLine.joinedViaLabel").replace("$temp", joinedViaType);
		List<WebElement> joinedViaElements = driver.findElements(By.xpath(xpath));
		count = joinedViaElements.size();
		logger.info(joinedViaType + " count is: " + count);
		TestListeners.extentTest.get().info(joinedViaType + " count is: " + count);
		return count;
	}

	public String getChallengeCompletedSteps(String campaignName) {
		String completedSteps = "";
		String xpath = utils.getLocatorValue("guestTimeLine.challengeCompletedSteps").replace("$campName",
				campaignName);
		WebElement completedStepsElement = driver.findElement(By.xpath(xpath));
		completedSteps = completedStepsElement.getText();
		logger.info("Completed steps for challenge campaign " + campaignName + " is: " + completedSteps);
		TestListeners.extentTest.get()
				.info("Completed steps for challenge campaign " + campaignName + " is: " + completedSteps);
		return completedSteps;
	}

	public String getExpiredRedeemedAmount() {
		String val = "";
		int attempts = 0;
		while (attempts <= 5) {
			try {
				utils.implicitWait(2);
				utils.longWaitInSeconds(2);
				WebElement expiredAmount = driver.findElement(locators.get("guestTimeLine.expiredAmount"));
				val = expiredAmount.getText();
				if (!val.isEmpty()) {
					logger.info("Expired redeemed amount is: " + val);
					TestListeners.extentTest.get().info("Expired redeemed amount is: " + val);
					break;
				}
			} catch (Exception e) {
				logger.info("Expired redeemed amount is not present yet in response");
				TestListeners.extentTest.get().info("Expired redeemed amount is not present yet in response");
				utils.refreshPage();
			}
			attempts++;
		}
		utils.implicitWait(60);
		return val;
	}

	private static final DateTimeFormatter DB_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss",
			Locale.ENGLISH);

	// UI formatter that FORCES a normal ASCII space before AM/PM
	private static final DateTimeFormatter UI_FORMATTER = new DateTimeFormatterBuilder()
			.appendPattern("MMM d, yyyy, h:mm").appendLiteral(' ') // ← critical: no U+202F ever
			.appendText(ChronoField.AMPM_OF_DAY, TextStyle.SHORT).toFormatter(Locale.ENGLISH);

	public static String convertDbDateToUiFormat(String dbDateTime) {

		if (dbDateTime == null || dbDateTime.isBlank()) {
			return null;
		}

		LocalDateTime dateTime = LocalDateTime.parse(dbDateTime.trim(), DB_FORMATTER);
		return dateTime.format(UI_FORMATTER);
	}

	public static String normalizeSpace(String s) {
		if (s == null)
			return null;
		// replace common non breaking / narrow spaces with regular space
		s = s.replace('\u00A0', ' ') // NO-BREAK SPACE
				.replace('\u202F', ' ') // NARROW NO-BREAK SPACE
				.replace('\u2007', ' ') // FIGURE SPACE
				.replace('\u2009', ' ') // THIN SPACE
				.replace('\u200A', ' '); // HAIR SPACE
		// collapse multiple spaces and trim
		return s.replaceAll("\\s+", " ").trim();
	}

	public boolean verifyChallengeCampaignAuditLog(String campName, String tabName) {
		utils.waitTillPagePaceDone();
		boolean flag = true;
		String xpath = utils.getLocatorValue("guestTimeLine.challengeCampAuditLogLink").replace("$campName", campName)
				.replace("$tabName", tabName);
		boolean isTabPresent = pageObj.gamesPage().isPresent(xpath);
		if (!isTabPresent) {
			flag = false;
		}
		return flag;
	}

	// Verify Opt-in/Opt-out button presence for available challenge
	public String verifyAvailableChallengeOptBtn(String challengeId, String buttonType) {
		utils.waitTillPagePaceDone();
		String challengeEditXpath = utils.getLocatorValue("guestTimeLine.challengeCardBtn")
				.replace("$challengeId", challengeId).replace("$buttonType", "edit");
		WebElement challengeEditBtn = driver.findElement(By.xpath(challengeEditXpath));
		utils.scrollToElement(driver, challengeEditBtn);
		String btnText = "";
		String optInOutXpath = utils.getLocatorValue("guestTimeLine.challengeCardBtn")
				.replace("$challengeId", challengeId).replace("$buttonType", buttonType);
		WebElement optInOutBtnElement = null;
		boolean isBtnPresent = pageObj.gamesPage().isPresent(optInOutXpath);
		if (isBtnPresent) { // When button is present, button text will be returned
			optInOutBtnElement = driver.findElement(By.xpath(optInOutXpath));
			btnText = optInOutBtnElement.getText().trim();
			utils.logInfo(buttonType + " button for challenge ID '" + challengeId + "' has text: " + btnText);
		} else { // When button is not present, empty string will be returned
			utils.logInfo(buttonType + " button for challenge ID '" + challengeId + "' is not present.");
		}
		return btnText;
	}

	public void clickAuditLogInChallengeTab(String campName, String tabName) {
		String xpath = utils.getLocatorValue("guestTimeLine.challengeCampAuditLogLink").replace("$campName", campName)
				.replace("$tabName", tabName);
		WebElement challengeCampAuditLogLink = driver.findElement(By.xpath(xpath));
		challengeCampAuditLogLink.click();
		utils.logit("Clicked on Audit Log link in Challenge Campaign tab");
	}

	public List<String> extractLinksFromPNBody(String campaignName) {

		List<WebElement> elments = driver.findElements(By
				.xpath(utils.getLocatorValue("guestTimeLine.pushNotificationCampaign").replace("$temp", campaignName)));
		List<String> links = new ArrayList<>();
		String regex = "https:\\/\\/[^\\s]+"; // Regex to match URLs
		Matcher match = Pattern.compile(regex).matcher(elments.get(0).getText());
		while (match.find()) {
			links.add(match.group());
		}
		return links;
	}

	public String TimeDifferenceExample(String time1, String time2) {

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

		LocalDateTime startTime = LocalDateTime.parse(time1, formatter);
		LocalDateTime endTime = LocalDateTime.parse(time2, formatter);

		Duration duration = Duration.between(startTime, endTime);

		long days = duration.toDays();
		long hours = duration.toHours();
		long minutes = Math.abs(duration.toMinutes());

		logger.info("Days: " + days);
		logger.info("Hours: " + hours);
		logger.info("Minutes: " + minutes);
		return Long.toString(minutes);
	}
}
