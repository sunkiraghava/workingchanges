package com.punchh.server.pages;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.Color;
import org.openqa.selenium.support.ui.Select;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class SegmentsPage {
	static Logger logger = LogManager.getLogger(SegmentsPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	String ssoEmail, temp = "temp";
	String segmentDescription = "Test segment description";
	public String emailCount, segmentCount, pnCount, smsCount;
	private PageObj pageObj;

	public SegmentsPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(this.driver);
		selUtils = new SeleniumUtilities(driver);
		pageObj = new PageObj(driver);
	}

	public void saveAndShowSegment() throws InterruptedException {
		utils.getLocator("segmentPage.saveAndShowButton").isDisplayed();
		utils.getLocator("segmentPage.saveAndShowButton").click();
		Thread.sleep(2000);
		utils.logit("Clicked on Save & Show button");
		utils.waitTillPagePaceDone();		
		/*
		 * utils.getLocator("segmentPage.segmentCreatedLabel").isDisplayed();
		 * logger.info(utils.getLocator("segmentPage.segmentCreatedLabel").getText());
		 * TestListeners.extentTest.get().pass(utils.getLocator(
		 * "segmentPage.segmentCreatedLabel").getText());
		 */
	}

	public void verifyNewSegment(String segmentName, String segmentType) {
		try {
			driver.findElement(
					By.xpath(utils.getLocatorValue("segmentPage.segmentHeading").replace("temp", segmentName)))
					.isDisplayed();
			driver.findElement(
					By.xpath(utils.getLocatorValue("segmentPage.segmentLabels").replace("temp", segmentType)))
					.isDisplayed();
			driver.findElement(By.xpath(utils.getLocatorValue("segmentPage.segmentDurationLabels").replace("temp",
					segmentType.toLowerCase()))).isDisplayed();
			logger.info("Verified Segment name an segment type");
			TestListeners.extentTest.get().pass("Verified Segment name an segment type");
		} catch (Exception e) {
			logger.error("Error in verifying newly created segment" + e);
			TestListeners.extentTest.get().fail("Error in verifying newly created segment" + e);
		}
	}

	public String getGuestCount() {
		try {
			utils.getLocator("segmentPage.refreshButton").click();
			selUtils.longWait(15000);
//			utils.getLocator("segmentPage.guestCountLabel").isDisplayed();
//			selUtils.longWait(2000);
//			guestCount = utils.getLocator("segmentPage.guestCountLabel").getText();
//			logger.info("Verified Segment name and segment type");
//			TestListeners.extentTest.get().pass("Verified Segment name and segment type");
			segmentCount = utils.getLocator("segmentPage.segmentCountHeading").getText();
			emailCount = utils.getLocator("segmentPage.emailCountHeading").getText();
			pnCount = utils.getLocator("segmentPage.pnCountHeading").getText();
			smsCount = utils.getLocator("segmentPage.smsCountHeading").getText();
		} catch (Exception e) {
			logger.error("Error in getting segment guest count" + e);
			TestListeners.extentTest.get().fail("Error in getting segment guest count" + e);
		}
		return segmentCount;
	}

	public void verifyGuestCount(String segmentCount, String emailCount, String pnCount, String smsCount) {
		try {
			utils.getLocator("segmentPage.refreshButton").click();
			selUtils.longWait(7000);
			compareCount(segmentCount, utils.getLocator("segmentPage.segmentCountHeading").getText().trim(),
					"Segment count");
			compareCount(emailCount, utils.getLocator("segmentPage.emailCountHeading").getText().trim(), "Email count");
			compareCount(pnCount, utils.getLocator("segmentPage.pnCountHeading").getText().trim(), "PN count");
			compareCount(smsCount, utils.getLocator("segmentPage.smsCountHeading").getText().trim(), "SMS count");
		} catch (Exception e) {
			logger.error("Error in verifying count" + e);
			TestListeners.extentTest.get().fail("Error in verifying count" + e);
		}
	}

	private void compareCount(String expected, String actual, String fieldName) {
		try {
			if (Integer.parseInt(expected) + 1 == Integer.parseInt(actual)) {
				logger.info("Verified " + fieldName + ": " + actual);
				TestListeners.extentTest.get().pass("Verified " + fieldName + ": " + actual);
			} else {
				logger.warn("Failed to verify " + fieldName + ": " + actual);
				TestListeners.extentTest.get().warning("Failed to verify " + fieldName + ": " + actual);
			}
		} catch (NumberFormatException e) {
			logger.error("Error in comparing count" + e);
			TestListeners.extentTest.get().fail("Failed to verify" + fieldName + actual);
		}
	}

	public boolean verifyUserPresence(String currentUser, String oldUser) {
		boolean flag = false;
		try {
			selUtils.implicitWait(50);
			utils.getLocator("segmentPage.refreshButton").click();
			selUtils.longWait(9000);
			utils.getLocator("segmentPage.userSegment").isDisplayed();
			utils.getLocator("segmentPage.userSegment").clear();
			utils.getLocator("segmentPage.userSegment").sendKeys(currentUser);
			selUtils.longWait(5000);
			utils.getLocator("segmentPage.autoCompleteLabel").isDisplayed();
			utils.getLocator("segmentPage.autoCompleteLabel").click();
			utils.getLocator("segmentPage.searchResult").isDisplayed();
			selUtils.longWait(5000);
			if (prop.getProperty("greenColor")
					.equalsIgnoreCase(utils.getLocator("segmentPage.searchResult").getCssValue("background-color"))) {
				logger.info("Successfully Vvrfied new user as current month user");
				TestListeners.extentTest.get().pass("Successfully Verified new user as current month user");
			} else {
				logger.error("Failed to verify new user as current month user");
				TestListeners.extentTest.get().fail("Failed to verify new user as current month user");
				return false;
			}
			utils.getLocator("segmentPage.userSegment").clear();
			utils.getLocator("segmentPage.userSegment").sendKeys(oldUser);
			selUtils.longWait(5000);
			utils.getLocator("segmentPage.autoCompleteLabel").isDisplayed();
			utils.getLocator("segmentPage.autoCompleteLabel").click();
			selUtils.longWait(5000);
			utils.getLocator("segmentPage.searchResult").isDisplayed();
			if (prop.getProperty("redColor")
					.equalsIgnoreCase(utils.getLocator("segmentPage.searchResult").getCssValue("background-color"))) {
				logger.info("Successfully Verified new user as old user");
				TestListeners.extentTest.get().pass("Successfully verfied nuser not created in current month");
			} else {
				logger.error("Failed to verify new user as current month user");
				TestListeners.extentTest.get().fail("Failed to verify new user as current month user");
			}
			return flag;
		} catch (Exception e) {
			logger.error("Error in verifying users presence in segment" + e);
			TestListeners.extentTest.get().warning("Error in verifying users presence in segment" + e);
		}
		return flag;
	}

	public boolean verifyCustomSegmentCount() {
		try {
			utils.getLocator("segmentPage.refreshButton").click();
			selUtils.longWait(5000);
			utils.getLocator("segmentPage.segmentCountHeading").isDisplayed();
			segmentCount = utils.getLocator("segmentPage.segmentCountHeading").getText();
			emailCount = utils.getLocator("segmentPage.emailCountHeading").getText();
			if (segmentCount.equals("2") && emailCount.equals("2")) {
				logger.info("Verified segment & email count: " + segmentCount);
				TestListeners.extentTest.get().info(segmentCount);
				return true;
			} else {
				logger.info("Failed to verify segment count");
				TestListeners.extentTest.get().info("Failed to verify segment count");
			}
		} catch (Exception e) {
			logger.error("Error in getting segment guest count" + e);
			TestListeners.extentTest.get().fail("Error in getting segment guest count" + e);
		}
		return false;
	}

	public boolean getCustomSegmentSize() throws InterruptedException {
		boolean flag = false;
		int attempts = 0;
		while (attempts < 100) {
			// utils.getLocator("segmentPage.refreshButton").click();
			utils.refreshPage();
			Thread.sleep(5000);
			try {
				segmentCount = utils.getLocator("segmentPage.segmentCountHeading").getText();
				if (segmentCount.equals("2")) {
					flag = true;
					logger.info("Verified segment count : " + segmentCount);
					TestListeners.extentTest.get().pass("Verified segment count :" + segmentCount);
					break;
				}
			} catch (Exception e) {
				logger.info("Custom Segment count did not matched");
			}
			attempts++;
		}
		return flag;
	}

	public String verifyCustomEmailSubscribers() {
		return emailCount = utils.getLocator("segmentPage.emailCountHeading").getText();
	}

	public String getTextColour(WebElement ele) {
		utils.longwait(3000);
		String rgbFormat = ele.getCssValue("background-color");
		String hexcode = Color.fromString(rgbFormat).asHex();
		return hexcode;
	}

	public boolean guestTimelineSegmentColor(String segmentName, String hexCode) {
		boolean flag = false;
		WebElement wEle1 = null;
		utils.waitTillPagePaceDone();
		List<WebElement> segmentListWEle = utils.getLocatorList("segmentPage.guestTimelineSegmentList");
		for (WebElement wEle : segmentListWEle) {
			String subStr1 = wEle.getText();
			String segment = (subStr1.substring(0, subStr1.length() - 13)).trim();
			if (segment.equalsIgnoreCase(segmentName)) {
				wEle1 = wEle;
				wEle.click();
				break;
			}
		}
		Assert.assertEquals(getTextColour(wEle1), hexCode);
		logger.info("Segment color matched");
		TestListeners.extentTest.get().info("Segment color matched");
		flag = true;

		return flag;
	}

	public void auditLogsOfSegmentPage(String title) {
		String text = "";
		utils.getLocator("segmentPage.clickSegmentAuditLogs").isDisplayed();
		utils.getLocator("segmentPage.clickSegmentAuditLogs").click();
		text = utils.getLocator("segmentPage.segmentAuditLogsHeadline").getText();
		Assert.assertEquals(text, title, "Audit Logs page title is not Visible for Segment");
		logger.info("Verified that Audit Logs page title is Visible for Segment");
		TestListeners.extentTest.get().pass("Verified that Audit Logs page title is Visible for Segment");
	}

	public void segmentOverviewPageOptionList(String option) {
		selUtils.jsClick(utils.getLocator("segmentPage.clickSegmentOptionDrpDown"));
		List<WebElement> ele = utils.getLocatorList("segmentPage.segmentOptionDrpDownList");
		utils.selectListDrpDwnValue(ele, option);
		logger.info("Select " + option + " on Segment overview page");
		TestListeners.extentTest.get().info("Select " + option + " on Segment overview page");
	}

	public void markSegmentFavourite(String choice, String segmentName) {
		switch (choice) {
		case "Favorite":
			utils.getLocator("segmentPage.markFavorite").click();
			logger.info("Make " + segmentName + " as Favorite Segment");
			TestListeners.extentTest.get().info("Make " + segmentName + " as Favorite Segment");
			break;

		case "Normal":
			utils.getLocator("segmentPage.markFavorite").click();
			logger.info("Make " + segmentName + " as Normal Segment");
			TestListeners.extentTest.get().info("Make " + segmentName + " as Normal Segment");
			break;
		}
	}

	public String getSegmentID() {
		utils.waitTillPagePaceDone();
		String url = driver.getCurrentUrl();
		// Tailored regular expression to match the specific URL pattern

//        String id = url.replaceAll("[^0-9]","");
//        return id;
		Pattern pattern = Pattern.compile("segment-overview/(\\d+)");
		Matcher matcher = pattern.matcher(url);

		if (matcher.find()) {
			logger.info("segment id = " + matcher.group(1));
			TestListeners.extentTest.get().info("segment id = " + matcher.group(1));
			return matcher.group(1); // Returns the captured number
		} else {
			return null; // Or handle the case where no number is found
		}
	}

	public void searchAndOpenSegment(String segmentName, String segmentId) {
		utils.refreshPage();
		utils.waitTillSpinnerDisappear(10);
		utils.waitTillElementToBeClickable(utils.getLocator("segmentPage.serchSegment"));
		utils.getLocator("segmentPage.serchSegment").isDisplayed();
		utils.getLocator("segmentPage.serchSegment").clear();
		utils.getLocator("segmentPage.serchSegment").sendKeys(segmentName);
		utils.getLocator("segmentPage.serchSegment").sendKeys(Keys.ENTER);
		WebElement clickSearchEle = driver.findElement(
				By.xpath(utils.getLocatorValue("segmentPage.clickSearchedSegment").replace("$temp", segmentId)));
		clickSearchEle.isDisplayed();
//		utils.StaleElementclick(driver, clickSearchEle);
		utils.clickByJSExecutor(driver, clickSearchEle);
		utils.waitTillPagePaceDone();
		utils.waitTillSpinnerDisappear(10);
		utils.waitTillVisibilityOfElement(utils.getLocator("segmentPage.segmentOverviewPageHeading"), segmentName);
		String heading = utils.getLocator("segmentPage.segmentOverviewPageHeading").getText();
		Assert.assertEquals(heading, segmentName, "Segment Overview Page heading not matched");
		logger.info("Segment Overview Page heading matched i.e. " + segmentName);
		TestListeners.extentTest.get().info("Segment Overview Page heading matched i.e. " + segmentName);
	}

	// hardik
	public boolean brokenLinksUi() throws IOException {
		boolean flag1 = false;
		boolean flag = true;
		int counter = 0;
		List<WebElement> allLink = utils.getLocatorList("segmentPage.brokenUrlOfSegment");
		logger.info("Total links are " + allLink.size());
		TestListeners.extentTest.get().info("Total links are " + allLink.size());
		for (int i = 0; i < allLink.size(); i++) {
			WebElement ele = allLink.get(i);
			String url = ele.getAttribute("href");
			if ((url.contains("javascript:void(0)")) || (url.contains("http://status.punchh.com"))
					|| (url.contains("https://support.punchh.com/s/article/Segments"))) {
				continue;
			} else {
				flag1 = verifySegmentActiveLink(url);
				if (flag1 == false) {
					logger.info("Broken link found: " + url);
					TestListeners.extentTest.get().fail("Broken link found: " + url);
					counter++;
				}
			}
		}
		if (counter != 0) {
			flag = false;
		}
		return flag;
	}

	// hardik
	public boolean verifySegmentActiveLink(String linkurl) throws IOException {
		boolean flag = false;
		URL url = new URL(linkurl);
		HttpURLConnection httpUrlConnect = (HttpURLConnection) url.openConnection();
		utils.longWaitInSeconds(1);
		httpUrlConnect.setConnectTimeout(3000);
		try {
			httpUrlConnect.connect();
			if (httpUrlConnect.getResponseCode() == 200) {
				flag = true;
				logger.info(linkurl + " - " + httpUrlConnect.getResponseMessage());
				TestListeners.extentTest.get().pass(linkurl + " - " + httpUrlConnect.getResponseMessage());
			}
		} catch (Exception e) {
			logger.info(linkurl + " - " + httpUrlConnect.getResponseMessage() + " - " + HttpURLConnection.HTTP_NOT_FOUND
					+ "-" + httpUrlConnect.getResponseCode());
			TestListeners.extentTest.get().fail(linkurl + " - " + httpUrlConnect.getResponseMessage() + " - "
					+ HttpURLConnection.HTTP_NOT_FOUND + "-" + httpUrlConnect.getResponseCode());
		}

		return flag;
	}

	public void searchAndMarkSegmentFavorite(String segmentName, String segmentId) {
		utils.waitTillPagePaceDone();
		utils.getLocator("segmentPage.serchSegment").isDisplayed();
		utils.getLocator("segmentPage.serchSegment").clear();
		utils.getLocator("segmentPage.serchSegment").sendKeys(segmentName);
		utils.getLocator("segmentPage.serchSegment").sendKeys(Keys.ENTER);
		String xpath = utils.getLocatorValue("segmentPage.favoriteOrNot").replace("$temp", segmentId);
//				driver.findElement(By.xpath(utils.getLocatorValue("segmentPage.favoriteOrNot").replace("$temp", segmentName)));
		String className = driver.findElement(By.xpath(xpath)).getAttribute("class");
		if (!className.equalsIgnoreCase("is_favorite favorite-icon-section truncate")) {
			driver.findElement(By.xpath(utils.getLocatorValue("segmentPage.favoriteOrNot").replace("$temp", segmentId)))
					.click();
			logger.info("Marked " + segmentName + "Segment as Favorite");
			TestListeners.extentTest.get().info("Marked " + segmentName + "Segment as Favorite");
		} else {
			logger.info(segmentName + "Segment is already marked as Favorite");
			TestListeners.extentTest.get().info(segmentName + "Segment is already marked as Favorite");
		}
	}

	public void filterByFavorite() {
		utils.waitTillPagePaceDone();
		utils.getLocator("segmentPage.clickOnFilterByFavouriteButton").click();
		utils.getLocator("segmentPage.clickOnFilterByFavouriteOption").click();
		logger.info("Navigated to favorite Segments");
		TestListeners.extentTest.get().info("Navigated to favorite Segments");
	}

	public String getInfoOfFirstSegment(String col) {
//		utils.waitTillPagePaceDone();
		WebElement ele = driver.findElement(
				By.xpath(utils.getLocatorValue("segmentPage.nameAndDateOfFirstSegment").replace("$col", col)));
		String text = ele.getText();
		return text;
	}

	public void segmentSorting(String option) {
//		utils.waitTillPagePaceDone();
		driver.findElement(By.xpath(utils.getLocatorValue("segmentPage.segmentSorting").replace("$option", option)))
				.click();
		logger.info("Clicked on Segment " + option + " sorting option");
		TestListeners.extentTest.get().info("Clicked on Segment " + option + " sorting option");
	}

	public void clickOnNextPageButton(String choice, String number) {
		String xpath = "";
		switch (choice) {
		case "Next Button":
			List<WebElement> totalPage = utils.getLocatorList("segmentPage.clickOnNextPageButton");
			int n = totalPage.size();
			xpath = utils.getLocatorValue("segmentPage.clickOnNextPageButton") + "[" + n + "]";
			driver.findElement(By.xpath(xpath)).click();
			logger.info("Clicked on NEXT button on segment list page");
			TestListeners.extentTest.get().pass("Verified that clicked on NEXT button on segment list page");
			break;

		case "Page Number":
			xpath = utils.getLocatorValue("segmentPage.clickOnNextPageButton") + "[" + number + "]";
			driver.findElement(By.xpath(xpath)).click();
			logger.info("Clicked on page number " + number + " on segment list page");
			TestListeners.extentTest.get()
					.pass("Verified that clicked on page number " + number + " on segment list page");
			break;
		}
	}

	public void numberOfSegPerPage(String option) {
		utils.refreshPage();
		utils.scrollToElement(driver, utils.getLocator("segmentPage.numberOfSegPerPageDrpDown"));
		utils.getLocator("segmentPage.numberOfSegPerPageDrpDown").click();
		List<WebElement> ele = utils.getLocatorList("segmentPage.numberOfSegPerPage");
		utils.selectListDrpDwnValue(ele, option);
		logger.info("Number of segments per page is " + option);
		TestListeners.extentTest.get().info("Number of segments per page is " + option);
	}

	public void searchSegmentOnly(String segmentName, String segmentId) {
		utils.getLocator("segmentPage.serchSegment").isDisplayed();
		utils.getLocator("segmentPage.serchSegment").clear();
		utils.getLocator("segmentPage.serchSegment").sendKeys(segmentName);
		utils.getLocator("segmentPage.serchSegment").sendKeys(Keys.ENTER);

		driver.findElement(
				By.xpath(utils.getLocatorValue("segmentPage.clickSearchedSegment").replace("$temp", segmentId)))
				.isDisplayed();
		logger.info("Searched segments is " + segmentName);
		TestListeners.extentTest.get().info("Searched segments is " + segmentName);
	}

	public boolean checkActionOptionButtonIsClickable(String choice) {
		boolean flag = false;
		utils.getLocator("segmentPage.actionOptionButton").isDisplayed();
		utils.getLocator("segmentPage.actionOptionButton").click();
		List<WebElement> segmentListOption = utils.getLocatorList("segmentPage.actionsDrpOptionList");
		for (WebElement wEle : segmentListOption) {
			String text = wEle.getText();
			logger.info(text);
			if (text.contains(choice)) {
				flag = true;
				break;
			}
		}
		return flag;
	}

	public void findGuestInSegment(String guestEmail) {

		WebElement guest = utils.getLocator("segmentPage.findGuestInSegment");
		guest.sendKeys(guestEmail);
		logger.info("guest segments is " + guestEmail);
		TestListeners.extentTest.get().info("guest segments is " + guestEmail);
		driver.findElement(By.xpath(utils.getLocatorValue("segmentPage.searchedGuest").replace("$temp", guestEmail)))
				.click();
	}

	public boolean verifyUserColorInSegment(String customUser) throws InterruptedException {
		Actions actions = new Actions(driver);
		actions.click(utils.getLocator("segmentBetaPage.calculateLink")).build().perform();

		if (utils.getLocatorList("segmentPage.searchUserInSegment").size() >= 1) {
			for (int i = 0; i < 10; i++) {
				utils.typeTextCharacterByCharacter(utils.getLocator("segmentPage.searchUserInSegment"), customUser);
				utils.longWaitInSeconds(1);

				String xpath = utils.getLocatorValue("segmentPage.autoCompleteLabel").replace("temp", customUser);
				if (driver.findElements(By.xpath(xpath)).size() == 1) {
					utils.waitTillVisibilityOfElement(driver.findElement(By.xpath(xpath)), "");
					utils.clickUsingActionsClass(driver.findElement(By.xpath(xpath)));
					utils.waitTillPagePaceDone();

					if (utils.getLocatorList("segmentPage.searchResult").size() >= 1) {
						utils.longWaitInSeconds(2);
						WebElement wEle1 = utils.getLocator("segmentPage.searchResult");
						utils.waitTillVisibilityOfElement(wEle1, "Searched result");

						String actualHex = getTextColour(wEle1).trim().toLowerCase();
						logger.info("Hex code found for user: " + actualHex);

						if ("#ddf3dd".equals(actualHex)) {
							logger.info("User color is #ddf3dd – returning true");
							TestListeners.extentTest.get().pass("Hexcode matched #ddf3dd");
							return true;
						} else {
							logger.info("User color is " + actualHex + " – returning false");
							TestListeners.extentTest.get().pass("Hexcode not matched #ddf3dd");
							return false;
						}
					} else {
						logger.info("User found, but search result not visible.");
						TestListeners.extentTest.get().fail("User found but no search result displayed.");
						return false;
					}
				} else {
					if (i == 9) {
						logger.info("User not found after 10 retries.");
						return false;
					} else {
						logger.info("Retrying search – attempt " + (i + 1));
						utils.longWaitInSeconds(1);
						utils.refreshPage();
						utils.waitTillPagePaceDone();
					}
				}
			}
		}
		logger.info("Search input not present on segment page.");
		TestListeners.extentTest.get().fail("Search input not found.");
		return false;
	}

	public boolean verifyUserPresentInSegment(String customUser, String hexCode) throws InterruptedException {
		// utils.longWaitInSeconds(15);
		Actions actions = new Actions(driver);
		actions.click(utils.getLocator("segmentBetaPage.calculateLink")).build().perform();
		if (utils.getLocatorList("segmentPage.searchUserInSegment").size() >= 1) {
			for (int i = 0; i < 10; i++) {
				utils.typeTextCharacterByCharacter(utils.getLocator("segmentPage.searchUserInSegment"), customUser);
				utils.longWaitInSeconds(1);
				String xpath = utils.getLocatorValue("segmentPage.autoCompleteLabel").replace("temp", customUser);
				if (driver.findElements(By.xpath(xpath)).size() == 1) {
					utils.waitTillInVisibilityOfElement(driver.findElement(By.xpath(xpath)), "");
					utils.clickUsingActionsClass(driver.findElement(By.xpath(xpath)));
					utils.waitTillPagePaceDone();
					if (utils.getLocatorList("segmentPage.searchResult").size() >= 1) {
						utils.longWaitInSeconds(2);
						WebElement wEle1 = utils.getLocator("segmentPage.searchResult");
						utils.waitTillVisibilityOfElement(wEle1, "Searched result");
						Assert.assertEquals(getTextColour(wEle1), hexCode);
						logger.info("Verified hex code color is matched");
						TestListeners.extentTest.get().pass("Verified hex code color is matched");
						return true;
					} else {
						logger.info("Error in verifying users presence in segment");
						TestListeners.extentTest.get().fail("Error in verifying users presence in segment");
						return false;
					}
				} else {
					if (i == 9) {
						return false;
					} else {
						logger.info("user not found searching again -- " + i);
						TestListeners.extentTest.get().info("user not found searching again -- " + i);
						utils.longWaitInSeconds(1);
						utils.refreshPage();
						utils.waitTillPagePaceDone();
						continue;
					}
				}
			}
		}
		logger.info("Error in verifying users presence in segment");
		TestListeners.extentTest.get().fail("Error in verifying users presence in segment");
		return false;
	}

	public boolean verifyUserNotPresentInSegment(String customUser, String hexCode) {
		utils.longWaitInSeconds(9);
		if (utils.getLocatorList("segmentPage.searchUserInSegment").size() >= 1) {
			utils.getLocator("segmentPage.searchUserInSegment").sendKeys(customUser);
			String xpath = utils.getLocatorValue("segmentPage.autoCompleteLabel").replace("temp", customUser);
			driver.findElement(By.xpath(xpath)).click();
			if (utils.getLocatorList("segmentPage.searchResult").size() >= 1) {
				utils.longWaitInSeconds(5);
				WebElement wEle1 = utils.getLocator("segmentPage.searchResult");
				Assert.assertEquals(getTextColour(wEle1), hexCode);
				logger.info("Verified hex code color is matched");
				TestListeners.extentTest.get().pass("Verified hex code color is matched");
				return true;
			} else {
				logger.info("Error in verifying users presence in segment");
				TestListeners.extentTest.get().fail("Error in verifying users presence in segment");
				return false;
			}
		} else {
			logger.info("Error in verifying users presence in segment");
			TestListeners.extentTest.get().fail("Error in verifying users presence in segment");
			return false;
		}
	}

	public String removeUserFromSegment(String segmentName, String hexCode) {
		utils.getLocator("segmentPage.selectSegmentfromUserProfile").click();
		String removesengent = utils.getLocatorValue("segmentPage.removeUserFromSegment").replace("$temp", segmentName);
		driver.findElement(By.xpath(removesengent)).click();
		utils.acceptAlert(driver);
		utils.waitTillPagePaceDone();
		utils.longWaitInSeconds(1);
		WebElement wEle1 = utils.getLocator("guestTimeLine.successAlert");
		Assert.assertEquals(getTextColour(wEle1), hexCode);
		logger.info("Verified hex code color is matched");
		TestListeners.extentTest.get().pass("Verified hex code color is matched");
		String msg = utils.getLocator("guestTimeLine.successAlert").getText();
		return msg;
	}

	public String addUserToSegment(String segmentName, String hexCode) {
		utils.getLocator("segmentPage.selectSegmentfromUserProfile").click();
		String addUser = utils.getLocatorValue("segmentPage.addUserToSegment").replace("$temp", segmentName);
		driver.findElement(By.xpath(addUser)).click();
		utils.acceptAlert(driver);
		WebElement wEle1 = utils.getLocator("guestTimeLine.successAlert");
		Assert.assertEquals(getTextColour(wEle1), hexCode);
		logger.info("Verified hex code color is matched");
		TestListeners.extentTest.get().pass("Verified hex code color is matched");
		String msg = utils.getLocator("guestTimeLine.successAlert").getText();
		return msg;
	}

	public void setSegmentAttribute(String attributeType) {
		Select sel = new Select(utils.getLocator("segmentPage.attributeDropdown"));
		sel.selectByVisibleText(attributeType);
		logger.info("Segment attribute type is set as: " + sel.getFirstSelectedOption().getText());
		TestListeners.extentTest.get()
				.pass("Segment attribute type is set as: " + sel.getFirstSelectedOption().getText());
	}

	public void setSegmentOperator(String operator) {
		Select sel = new Select(utils.getLocator("segmentPage.operatorDropdown"));
		sel.selectByVisibleText(operator);
		logger.info("Segment operator is set as: " + sel.getFirstSelectedOption().getText());
		TestListeners.extentTest.get().pass("Segment operator is set as: " + sel.getFirstSelectedOption().getText());
	}

	public void setSegmentLocation(String location) {
		Select sel = new Select(utils.getLocator("segmentPage.locationDropdown"));
		sel.selectByVisibleText(location);
		logger.info("Segment location is set as: " + sel.getFirstSelectedOption().getText());
		TestListeners.extentTest.get().pass("Segment location is set as: " + sel.getFirstSelectedOption().getText());
	}

	public void clickOnNewSegmentButton() {

		utils.getLocator("segmentPage.createNewSegmentButton").isDisplayed();
		utils.getLocator("segmentPage.createNewSegmentButton").click();

	}

	public void saveAndShowSegmentBtn() {
		utils.getLocator("segmentPage.saveAndShow").isDisplayed();
		utils.clickByJSExecutor(driver, utils.getLocator("segmentPage.saveAndShow"));
//		utils.getLocator("segmentPage.saveAndShow").click();
		utils.waitTillPagePaceDone();
//		Actions actions = new Actions(driver);
//		actions.click(utils.getLocator("segmentBetaPage.previewGuestsList")).build().perform();
		utils.tryAllClick(driver, utils.getLocator("segmentBetaPage.previewGuestsList"));
		utils.waitTillSpinnerDisappear(300);
		utils.getLocator("segmentPage.searchResult").isDisplayed();
	}

	public String getGuestVisitCountText() {
		utils.getLocator("segmentPage.searchResult").isDisplayed();
		utils.getLocator("segmentPage.searchResult").click();
		String text = utils.getLocator("guestTimeLine.guestVisitCountText").getText();
		return text;
	}

	public void deleteSegment(String segmentName) {
		segmentOverviewPageOptionList("Delete");
		utils.waitTillVisibilityOfElement(utils.getLocator("segmentPage.yesDeleteButton"), "yes delete button");
		utils.getLocator("segmentPage.yesDeleteButton").click();
		utils.waitTillSpinnerDisappear(7);
//		utils.waitTillPagePaceDone();
		logger.info(segmentName + " is deleted successfully");
		TestListeners.extentTest.get().pass(segmentName + " is deleted successfully");
	}

	public List<String> getAttributeNameList() {
		utils.waitTillPagePaceDone();
		List<WebElement> getAttributeNameListWebelement = utils.getLocatorList("segmentPage.getAttributeNameList");
		List<String> getAttributeNameList = new ArrayList<String>();
		for (WebElement wEle : getAttributeNameListWebelement) {
			getAttributeNameList.add(wEle.getText());

		}

		return getAttributeNameList;
	}

	public boolean auditLogWithSegmentId(String segmentId, String hexcode) {
		int count = 0;
		int attempts = 0;
		boolean flag = false;
		while (attempts < 5) {
			utils.longWaitInSeconds(5);
			try {
				WebElement ele = driver.findElement(By
						.xpath(utils.getLocatorValue("segmentPage.segmentIdInAuditLog").replace("$segId", segmentId)));
				Assert.assertEquals(getTextColourEle(ele), hexcode);
				logger.info("Segment color for audit log matched");
				TestListeners.extentTest.get().info("Segment color for audit log matched");
				flag = true;
				break;
			} catch (Exception e) {
				logger.info("Segment id is not found in audit log ");
				TestListeners.extentTest.get().info("Segment id is not found in audit log " + attempts);

			}
			attempts++;
		}
		return flag;

	}

	public void updateButton(String buttonName, String successMessage) {
		utils.getLocator("segmentPage.updateBtn").click();
		String msg = utils.getSuccessMessage();
		Assert.assertEquals(msg, successMessage, successMessage + " is not updated");
		logger.info("Clicked on update button for " + buttonName);
		TestListeners.extentTest.get().info("Clicked on update button for " + buttonName);
	}

	public void checkSegmentRuleLimit(String givenLimit) {
		String limit = utils.getLocator("segmentPage.setRuleLimit").getAttribute("value");
		if (!limit.equalsIgnoreCase(givenLimit)) {
			utils.getLocator("segmentPage.setRuleLimit").click();
			utils.getLocator("segmentPage.setRuleLimit").clear();
			utils.getLocator("segmentPage.setRuleLimit").sendKeys(givenLimit);
			updateButton("Segment Rule Limit", "Global Configuration Updated");
			logger.info(
					"Skipping this Test Case because Segment Rule Limit in Segment Global Configuration is not matching, current limit is "
							+ limit + " and new updated limit is " + givenLimit);
			TestListeners.extentTest.get().warning(
					"Skipping this Test Case because Segment Rule Limit in Segment Global Configuration is not match, current limit is "
							+ limit + " and new updated limit is " + givenLimit);
			throw new SkipException("Segment Rule Limit in Segment Global Configuration is not matching");
		}
		logger.info("Segment Rule Limit in Segment Global Configuration is " + limit);
		TestListeners.extentTest.get().info("Segment Rule Limit in Segment Global Configuration is " + limit);
	}

	public void deleteSegmentCombination(String index) {
		String xpath = utils.getLocatorValue("segmentPage.deleteSegmentCombination").replace("$temp", index);
		driver.findElement(By.xpath(xpath)).isDisplayed();
		utils.waitTillElementToBeClickable(driver.findElement(By.xpath(xpath)));
		utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath)));
//		driver.findElement(By.xpath(xpath)).click();
		logger.info("Clicked on delete button for segment at index " + index);
		TestListeners.extentTest.get().info("Clicked on delete button for segment at index " + index);
	}

	public String getSuccessErrorMessage(String msg) {
		String messge = "";
		utils.scrollToElement(driver, utils.getLocator("locationPage.getSuccessErrorMessage"));
		messge = utils.getLocator("locationPage.getSuccessErrorMessage").getText();
		Assert.assertEquals(messge, msg, "Error or success message is not matched");
		return messge;
	}

	public String getOperatorValue() {
		String value = utils.getLocator("segmentPage.getSelectedOperator").getText();
		logger.info("Selected Segment membership is: " + value);
		TestListeners.extentTest.get().info("Selected Segment membership is: " + value);
		return value;
	}

	public boolean checkSegmentLabel(String labelName) {
		utils.longWaitInSeconds(2);
		boolean visibilityFlag = false;
		String xpath = utils.getLocatorValue("segmentPage.segmentLabelName").replace("$labelName", labelName);
		List<WebElement> eleList = driver.findElements(By.xpath(xpath));
		if (eleList.size() == 1) {
			logger.info("Segment Label " + labelName + " is present");
			TestListeners.extentTest.get().info("Segment Label " + labelName + " is present");
			visibilityFlag = true;
		}
		return visibilityFlag;
	}

	// Get all the attributes of a segment type like Membership tier
	public List<String> getAttributeFromSegmentType(String attributeType) {
		utils.longWaitInSeconds(2);
		String xpath = utils.getLocatorValue("segmentBetaPage.removeSelectedOperatorText").replace("temp",
				attributeType);
		utils.waitTillElementToBeClickable(driver.findElement(By.xpath(xpath)));
		utils.tryAllClick(driver, driver.findElement(By.xpath(xpath)));
		logger.info("Selected operator has been removed");
		String xpath1 = utils.getLocatorValue("segmentPage.clickOnAttributeType").replace("$attributeType",
				attributeType);
		utils.waitTillElementToBeClickable(driver.findElement(By.xpath(xpath1)));
		utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath1)));
		utils.longWaitInSeconds(2);
		String listXpath = utils.getLocatorValue("segmentPage.getAttributeSegmentType").replace("$attributeType",
				attributeType);
		List<WebElement> getAttributeNameListWebelement = driver.findElements(By.xpath(listXpath));
		List<String> getAttributeNameList = new ArrayList<String>();
		for (WebElement wEle : getAttributeNameListWebelement) {
			getAttributeNameList.add(wEle.getText());
		}
		if (driver.findElement(By.xpath(listXpath)).isDisplayed()) {
			utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath1)));
		}
		return getAttributeNameList;
	}

	// Enter Segment Name or Update Segment Name
	public void setSegmentName(String segmentName) {
		utils.waitTillPagePaceDone();
		utils.getLocator("segmentPage.editSegmentNameBtn").isDisplayed();
		utils.clickByJSExecutor(driver, utils.getLocator("segmentPage.editSegmentNameBtn"));
		// utils.getLocator("segmentPage.editSegmentNameBtn").click();
		utils.getLocator("segmentPage.enterSegmentName").click();
		utils.getLocator("segmentPage.enterSegmentName").clear();
		utils.getLocator("segmentPage.enterSegmentName").sendKeys(segmentName);
		utils.longWaitInSeconds(2);
		Actions action = new Actions(driver);
		action.moveToElement(utils.getLocator("segmentPage.segmentNameCheckbutton")).click().perform();
		utils.longWaitInSeconds(2);
		logger.info("Segment name is set as: " + segmentName);
		TestListeners.extentTest.get().info("Segment name is set as: " + segmentName);
	}

	public String clickFirstUserFromSegment() {
//		Actions actions = new Actions(driver);
//		actions.click(utils.getLocator("segmentBetaPage.calculateLink")).build().perform();
//		utils.longWaitInSeconds(2);
		utils.tryAllClick(driver, utils.getLocator("segmentBetaPage.previewGuestsList"));

		utils.waitTillSpinnerDisappear(240);
		WebElement el = utils.getLocator("segmentPage.searchResult");
		utils.waitTillVisibilityOfElement(el, "GuestList");
		utils.clickUsingActionsClass(utils.getLocator("segmentPage.searchResult"));
		String userEmail = utils.getLocator("guestTimeLine.guestEmailOnTimeline").getText();
		utils.logit("Clicked first user from segment ie : " + userEmail);
		return userEmail;

	}

	public void selectAttributeHavingCheckFunctionality(String attributeType, String attributeTypeName) {

		String clickOnAttributeTypeXpath = utils.getLocatorValue("segmentPage.clickOnAttributeType")
				.replace("$attributeType", attributeType);
		driver.findElement(By.xpath(clickOnAttributeTypeXpath)).click();
		utils.longWaitInSeconds(1);

		// Remove selected option
		String removeSelectedAttributeOptionXpath = utils.getLocatorValue("segmentPage.removeSelectedAttributeOption")
				.replace("$attributeType", attributeType);
		driver.findElement(By.xpath(removeSelectedAttributeOptionXpath)).click();

		// Select option from dropdown
		String selectedAttributeOptionXpath = utils.getLocatorValue("segmentPage.attributeOptionDrpDown")
				.replace("$attributeType", attributeType);
		List<WebElement> ele = driver.findElements(By.xpath(selectedAttributeOptionXpath));
		utils.selectListDrpDwnValue(ele, attributeTypeName);
		logger.info("Option for " + attributeType + " is selected as " + attributeTypeName);
		TestListeners.extentTest.get().info("Option for " + attributeType + " is selected as " + attributeTypeName);
	}

	public void clickOnAddNewSegmentInCampaign() {
		utils.waitTillElementToBeClickable(utils.getLocator("segmentPage.addNewSegmentInCampaign"));
		utils.getLocator("segmentPage.addNewSegmentInCampaign").click();
		logger.info("Clicked on Add New Segment in Campaign");
		TestListeners.extentTest.get().pass("Clicked on Add New Segment in Campaign");
	}

	public int getSegmentCountPolling(String apiKey, String segmentId, int noOfAttempts) {
		int count = 0;
		int attempts = 0;
		while (attempts < noOfAttempts) {
			utils.longWaitInSeconds(10);
			try {
				// fetch reward id from API
				Response segmentCountResponse = pageObj.endpoints().getSegmentCount(apiKey, segmentId);
				Assert.assertEquals(segmentCountResponse.getStatusCode(), 200,
						"Status code 200 did not matched for Fetch segment count dashboard api");
				String cnt = segmentCountResponse.jsonPath().get("count");
				count = Integer.parseInt(cnt);
				if (count != 0) {
					break;
				}
			} catch (Exception e) {
				logger.info("Segment count is not found ");
			}
			logger.info("Segment count is not found, trying again " + attempts);
			attempts++;
		}
		logger.info("Segment count is - " + count);
		TestListeners.extentTest.get().info("Segment count is - " + count);

		return count;
	}

	public String extractSegmentIdFromURL(String url) {
		Pattern pattern = Pattern.compile("segment-overview/(\\d+)");
		Matcher matcher = pattern.matcher(url);
		String segmentId = null;
		if (matcher.find()) {
			segmentId = matcher.group(1);
			logger.info("Extracted segment Id : " + segmentId);
			TestListeners.extentTest.get().info("Extracted segment Id : " + segmentId);
		} else {
			logger.info("No segment Id found in the URL.");
			TestListeners.extentTest.get().info("No segment Id found in the URL.");
		}
		return segmentId;
	}

	public void searchSegmentByNameOrID(String segmentNameOrId) {
		utils.waitTillSpinnerDisappear();
		utils.waitTillElementToBeClickable(utils.getLocator("segmentPage.serchSegment"));
		WebElement searchSegment = utils.getLocator("segmentPage.serchSegment");
		searchSegment.isDisplayed();
		searchSegment.clear();
		searchSegment.sendKeys(segmentNameOrId);
		searchSegment.sendKeys(Keys.ENTER);
		logger.info("Searched segments is " + segmentNameOrId);
		TestListeners.extentTest.get().info("Searched segments is " + segmentNameOrId);
	}

	public boolean openSegmentWithName(String segmentName) {
		boolean flag = false;
		WebElement opensegment = null;
		utils.longWaitInSeconds(2);
		try {
			opensegment = driver.findElement(
					By.xpath(utils.getLocatorValue("segmentPage.openSegment").replace("$segName", segmentName)));
			flag = opensegment.isDisplayed();
			if (flag) {
				opensegment.click();
				utils.waitTillPagePaceDone();
				flag = driver
						.findElement(By.xpath(
								utils.getLocatorValue("segmentPage.segmentHeading").replace("temp", segmentName)))
						.isDisplayed();
				logger.info("Clicked on segment " + segmentName + " and opened successfully");
				TestListeners.extentTest.get().info("Clicked on segment " + segmentName + " and opened successfully");
			}
		} catch (NoSuchElementException e) {
			utils.getLocator("segmentPage.segmentNotFoundInSearch").isDisplayed();
			logger.info("Segment " + segmentName + " not found in search");
			TestListeners.extentTest.get().info("Segment " + segmentName + " not found in search");
		}
		return flag;
	}

	public String getSegmentId(String segmentName) {
		searchSegmentByNameOrID(segmentName);
		Assert.assertTrue(openSegmentWithName(segmentName),
				"Segment " + segmentName + " not found in search or not opened successfully");
		String url = driver.getCurrentUrl();
		String segmentId = extractSegmentIdFromURL(url);
		logger.info("Segment Id of segment with name " + segmentName + " is " + segmentId);
		TestListeners.extentTest.get().info("Segment Id of segment with name " + segmentName + " is " + segmentId);
		return segmentId;
	}

	public void replaceSegmentIdInUrl(String newSegmentId) {
		String currentUrl = driver.getCurrentUrl();
		String updatedUrl = currentUrl.replaceAll("/segment-overview/\\d+", "/segment-overview/" + newSegmentId);
		logger.info("Updated URL: " + updatedUrl);
		TestListeners.extentTest.get().info("Updated URL: " + updatedUrl);
		Assert.assertNotEquals(currentUrl, updatedUrl, "Current URL and Updated URL are same");

		// Navigate to the updated URL
		driver.get(updatedUrl);
		logger.info("Navigated to updated URL: " + updatedUrl);
		TestListeners.extentTest.get().info("Navigated to updated URL: " + updatedUrl);
		driver.navigate().refresh();
	}

	public boolean segmentNotFoundWithInvalidURL(String segmentName) {
		String visibleSegmentName = utils.getLocator("segmentPage.getSegmentNameOnSegmentOverviewPage").getText();
		Assert.assertNotEquals(visibleSegmentName, segmentName, "Segment name is matching with searched segment name");
		boolean flag = true;
		boolean flag2 = utils.textContains(visibleSegmentName, "Unnamed Segment");
		Assert.assertTrue(flag2, "Segment name is not 'Unnamed Segment'");
		logger.info("Segment name is not matching with searched segment name");
		TestListeners.extentTest.get().info("Segment name is not matching with searched segment name");
		return flag;
	}

	public String getSegmentNameOnSegmentHomePage() {
		String segmentName = null;
		int attempts = 1;
		while (attempts < 10) {
			segmentName = driver
					.findElement(By.xpath(utils.getLocatorValue("segmentPage.getSegmentNameOnSegmentHomePage")
							.replace("$row", Integer.toString(attempts))))
					.getAttribute("title");

			if ((segmentName.contains("Unnamed Segment") != true)) {
				break;
			} else {
				attempts++;
			}
		}
		return segmentName;
	}

	public boolean openSegmentWithId(String segmentId, String segmentName) {
		boolean flag = false;
		WebElement opensegment = null;
		utils.longWaitInSeconds(2);
		try {
			opensegment = driver.findElement(
					By.xpath(utils.getLocatorValue("segmentPage.openSegmentWithId").replace("$segId", segmentId)));
			flag = opensegment.isDisplayed();
			if (flag) {
				opensegment.click();
				utils.waitTillPagePaceDone();
				flag = driver
						.findElement(By.xpath(
								utils.getLocatorValue("segmentPage.segmentHeading").replace("temp", segmentName)))
						.isDisplayed();
				logger.info("Clicked on segment " + segmentName + " and opened successfully");
				TestListeners.extentTest.get().info("Clicked on segment " + segmentName + " and opened successfully");
			}
		} catch (NoSuchElementException e) {
			utils.getLocator("segmentPage.segmentNotFoundInSearch").isDisplayed();
			logger.info("Segment " + segmentName + " not found in search");
			TestListeners.extentTest.get().info("Segment " + segmentName + " not found in search");
		}
		return flag;
	}

	public String getTextColourEle(WebElement ele) {
		utils.longwait(3000);
		String rgbFormat = ele.getCssValue("color");
		String hexcode = Color.fromString(rgbFormat).asHex();
		return hexcode;
	}

	public boolean verifySegmentCreationPageHeading(String segmentName) {
		utils.scrollToElement(driver, driver.findElement(
				By.xpath(utils.getLocatorValue("segmentPage.segmentLabels").replace("temp", segmentName))));
		boolean flag = driver
				.findElement(By.xpath(utils.getLocatorValue("segmentPage.segmentLabels").replace("temp", segmentName)))
				.isDisplayed();
		return flag;
	}

	// check user is eligible for segment or not
	public boolean segmentEligibilityPolling(String client, String secret, String token, String userID,
			String segmentId, int noOfAttempts) {
		int attempts = 0;
		boolean flag = false;
		while (attempts < noOfAttempts) {
			utils.longWaitInSeconds(3);
			// check user is eligible for segment or not
			Response segmentEligibilityResponse = pageObj.endpoints().Api2MobileSegmentEligibility(client, secret,
					token, userID, segmentId);
			Assert.assertEquals(segmentEligibilityResponse.getStatusCode(), 200, "Status code 200 did not matched");
			String eligibilityStatus = segmentEligibilityResponse.jsonPath().get("eligible").toString();
			if (eligibilityStatus.equalsIgnoreCase("true")) {
				flag = true;
				break;
			} else {
				logger.info("User is eligible for segment but API is giving Wrong Status, trying again " + attempts);
				TestListeners.extentTest.get()
						.info("User is eligible for segment but API is giving Wrong Status, trying again " + attempts);
			}
			attempts++;
		}
		return flag;
	}

	public void setSegmentExportName(String segmentName) {
		try {
			String xpath = utils.getLocatorValue("segmentPage.segmentExportSegmentName").replace("$temp", segmentName);
			WebElement segmentExportSegmentName = driver.findElement(By.xpath(xpath));
			Assert.assertTrue(segmentExportSegmentName.isDisplayed(),
					"Segment Export Segment Name dropdown is not displayed");
		} catch (Exception e) {
			WebElement weleSelect = utils.getLocator("segmentPage.selectSegmentExportDrpDown");
			utils.scrollToElement(driver, weleSelect);
			utils.selectDrpDwnValue(weleSelect, segmentName);
		}
	}
	
}
