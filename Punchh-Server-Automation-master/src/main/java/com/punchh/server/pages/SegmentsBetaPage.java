package com.punchh.server.pages;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;
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
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class SegmentsBetaPage {
	static Logger logger = LogManager.getLogger(SegmentsBetaPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	String ssoEmail, temp = "temp";
	String segmentDescription = "Test segment description";
	public String emailCount, pnCount, smsCount;
	int segmentCount;
	String age = "30";
	PageObj pageObj;

	public SegmentsBetaPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(this.driver);
		selUtils = new SeleniumUtilities(driver);
		pageObj = new PageObj(driver);
	}

	public void createNewSegment(String segmentName, String segmentType, String attribute, String attributeValue)
			throws InterruptedException {
		setSegmentBetaName(segmentName);
		selectSegmentType(segmentType);
		selectAttribute(attribute);
		selectAge(attributeValue);
	}

	public void createNewSegmentGuestType(String segmentName, String segmentType, String attribute,
			String guestTypeValue) {
		// selUtils.implicitWait(55);
		switchToClassicSegment();
		setSegmentBetaName(segmentName);
		utils.longwait(2);
		selectSegmentType(segmentType);
		selectAttribute(attribute);
		selectGuestType(guestTypeValue);
	}

	private void selectGuestType(String attributeValue) {
		Actions action = new Actions(driver);
		utils.getLocator("segmentBetaPage.guestTypeDropdown").isDisplayed();
		action.click(utils.getLocator("segmentBetaPage.guestTypeDropdown")).build().perform();
		WebElement ele = driver.findElement(
				By.xpath("//label[contains(text(),'Guest Type')]/..//div[text()='" + attributeValue + "']"));
		selUtils.jsClick(ele);
		// utils.getLocator("segmentBetaPage.eClubLoyalityLabel").isDisplayed();
		logger.info("Segment Guest type is set as: " + attributeValue);
		TestListeners.extentTest.get().info("Segment Guest type is set as: " + attributeValue);
	}

	private void selectAge(String attributeValue) {
		Actions action = new Actions(driver);
		action.click(utils.getLocator("segmentBetaPage.operatorDropdownButton")).build().perform();
		utils.getLocator("segmentBetaPage.operatorLabel").isDisplayed();
		action.click(utils.getLocator("segmentBetaPage.operatorLabel")).build().perform();
		utils.getLocator("segmentBetaPage.valueTextbox").isDisplayed();

		action.sendKeys(utils.getLocator("segmentBetaPage.valueTextbox"), attributeValue)
				.click(utils.getLocator("segmentBetaPage.calculateNowButton")).build().perform();
		logger.info("Segment attribute value is set as: " + attributeValue);
		TestListeners.extentTest.get().info("Segment attribute value is set as: " + attributeValue);

	}

	public void selectAttribute(String attribute) {
		utils.getLocator("segmentBetaPage.addAttributeButton").isDisplayed();
		utils.getLocator("segmentBetaPage.addAttributeButton").click();
		utils.getLocator("segmentBetaPage.attributeDropdownButton").isDisplayed();
		// selUtils.jsClick(utils.getLocator("segmentBetaPage.attributeDropdownButton"));
		utils.clickWithActions(utils.getLocator("segmentBetaPage.attributeDropdownButton"));
		utils.longWaitInSeconds(1);
		utils.getLocator("segmentBetaPage.attributeSearchBox").clear();
		utils.getLocator("segmentBetaPage.attributeSearchBox").sendKeys(attribute);
		WebElement Searchedattribute = driver.findElement(
				By.xpath(utils.getLocatorValue("segmentBetaPage.searchedAttribute").replace("temp", attribute)));
		Searchedattribute.click();
		logger.info("Segment attribute is set as: " + attribute);
		TestListeners.extentTest.get().info("Segment attribute is set as: " + attribute);
	}

	public void selectSegmentType(String segmentType) {
		pageObj.newSegmentHomePage().segmentAdvertiseBlock();
		utils.waitTillElementToBeClickable(utils.getLocator("segmentBetaPage.addRuleButton"));
		utils.getLocator("segmentBetaPage.addRuleButton").isDisplayed();
		utils.clickByJSExecutor(driver, utils.getLocator("segmentBetaPage.addRuleButton"));
//		utils.getLocator("segmentBetaPage.addRuleButton").click();
		utils.getLocator("segmentBetaPage.segmentTypeDropdown").isDisplayed();
		utils.clickWithActions(utils.getLocator("segmentBetaPage.segmentTypeDropdown"));
		// selUtils.jsClick(utils.getLocator("segmentBetaPage.segmentTypeDropdown"));
		utils.longWaitInSeconds(1);
		utils.getLocator("segmentBetaPage.segmentTypeSearchbox").clear();
		utils.getLocator("segmentBetaPage.segmentTypeSearchbox").sendKeys(segmentType);
		WebElement Searchedattribute = driver.findElement(By
				.xpath(utils.getLocatorValue("segmentBetaPage.segmentTypeSearchedValue").replace("temp", segmentType)));
		Searchedattribute.click();
		logger.info("Segment type is set as: " + segmentType);
		TestListeners.extentTest.get().info("Segment type is set as: " + segmentType);

	}

	public void setSegmentBetaName(String segmentName) {
		// utils.waitTillPagePaceDone();
		utils.longwait(2);
		// selUtils.implicitWait(50);
		/*
		 * utils.getLocator("segmentBetaPage.segmentBetaHeading").isDisplayed();
		 * utils.getLocator("segmentBetaPage.findSegmentTextbox").isDisplayed();
		 * utils.getLocator("segmentBetaPage.newSegmentButton").isDisplayed();
		 */
		selUtils.jsClick(utils.getLocator("segmentBetaPage.newSegmentButton"));
		utils.waitTillPagePaceDone();
		utils.waitTillSpinnerDisappear();
		WebElement el = utils.getLocator("segmentBetaPage.editSegmentNameBtn");
		// utils.getLocator("segmentBetaPage.editSegmentNameBtn").isDisplayed();
		utils.waitTillElementToBeClickable(el);
		el.click();
		pageObj.newSegmentHomePage().segmentAdvertiseBlock();
		// selUtils.longWait(2000);
		Actions action = new Actions(driver);
		selUtils.clearTextUsingJS(utils.getLocator("segmentBetaPage.segmentNameTextbox"));
		// selUtils.SendKeysViaJS(utils.getLocator("segmentBetaPage.segmentNameTextbox"),
		// segmentName);
		utils.getLocator("segmentBetaPage.segmentNameTextbox").sendKeys(segmentName);
		selUtils.longWait(2000);
		utils.waitTillElementToBeClickable(utils.getLocator("segmentBetaPage.SegmentNameCheckbutton"));
		action.moveToElement(utils.getLocator("segmentBetaPage.SegmentNameCheckbutton")).click().perform();
		/*
		 * action.sendKeys(utils.getLocator("segmentBetaPage.segmentNameTextbox"),
		 * segmentName)
		 * .click(utils.getLocator("segmentBetaPage.SegmentNameCheckbutton")).build().
		 * perform();
		 */
		logger.info("Segment name is set as: " + segmentName);
		TestListeners.extentTest.get().info("Segment name is set as: " + segmentName);
	}

	public void configureCustomSegment(String segmentName, String segmentType, String customEmailList)
			throws InterruptedException {
		setSegmentBetaName(segmentName);
		selectSegmentType(segmentType);
		utils.getLocator("segmentBetaPage.listTypeDropdownButton").isDisplayed();
		selUtils.jsClick(utils.getLocator("segmentBetaPage.listTypeDropdownButton"));
		utils.getLocator("segmentBetaPage.customValueLabel").isDisplayed();
		selUtils.jsClick(utils.getLocator("segmentBetaPage.customValueLabel"));
		utils.getLocator("segmentBetaPage.customListTextarea").isDisplayed();
		utils.getLocator("segmentBetaPage.customListTextarea").clear();
		utils.getLocator("segmentBetaPage.customListTextarea").sendKeys(customEmailList);
		selUtils.jsClick(utils.getLocator("segmentBetaPage.doneButton"));
	}

	public int getSegmentCount() {
		int segmentCount = 0;

		Actions action = new Actions(driver);
		try {
			utils.longWaitInSeconds(2);
			utils.getLocator("segmentBetaPage.calculateNowBetaButton").isDisplayed();
			action.click(utils.getLocator("segmentBetaPage.calculateNowBetaButton")).build().perform();
			utils.longWaitInSeconds(4);
			utils.waitTillVisibilityOfElement(utils.getLocator("segmentBetaPage.segmentCountLabel"),
					"segmentCountLabel");
			utils.getLocator("segmentBetaPage.segmentCountLabel").isDisplayed();
			segmentCount = Integer.parseInt(utils.getLocator("segmentBetaPage.segmentCountLabel")
					.getAttribute("innerHTML").trim().replace(",", ""));
			if (segmentCount >= 0) {
				logger.info("Segment count value is appearing as: "
						+ utils.getLocator("segmentBetaPage.segmentCountLabel").getAttribute("innerHTML").trim());
				TestListeners.extentTest.get().pass("Segment count value is appearing as: "
						+ utils.getLocator("segmentBetaPage.segmentCountLabel").getAttribute("innerHTML").trim());
			} else {
				logger.error("Segment count value is not correct");
				TestListeners.extentTest.get().fail("Segment count value is not correct");
				Assert.fail("Segment count value is not correct");
			}
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return segmentCount;
	}

	public int getSegmentSizeBeforeSave() {
		utils.scrollToElement(driver, utils.getLocator("segmentBetaPage.calculateNow1Button"));
//		Actions action = new Actions(driver);
//		utils.StaleElementclick(driver, utils.getLocator("segmentBetaPage.calculateNow1Button"));
		utils.longWaitInSeconds(1);
		utils.tryAllClick(driver, utils.getLocator("segmentBetaPage.calculateNow1Button"));

		try {
			//WebElement ele = utils.getLocator("segmentBetaPage.segmentSizeIconWrapper");
			utils.waitTillElementDisappear("segmentBetaPage.segmentSizeIconWrapper", 210);
			utils.logit("Segment Size Icon Wrapper is disappeared");
		} catch (Exception e) {
			utils.logit("Segment Size Icon Wrapper not appeared");
		}
		utils.waitTillElementToBeVisible(utils.getLocator("segmentBetaPage.segmentCountLabel"));
		utils.getLocator("segmentBetaPage.segmentCountLabel").isDisplayed();
		segmentCount = Integer.parseInt(utils.getLocator("segmentBetaPage.segmentCountLabel").getAttribute("innerHTML")
				.trim().replace(",", ""));
		if (segmentCount >= 0) {
			logger.info("Segment count value is appearing as: "
					+ utils.getLocator("segmentBetaPage.segmentCountLabel").getAttribute("innerHTML").trim());
			TestListeners.extentTest.get().pass("Segment count value is appearing as: "
					+ utils.getLocator("segmentBetaPage.segmentCountLabel").getAttribute("innerHTML").trim());
		} else {
			logger.error("Segment count value is not correct");
			TestListeners.extentTest.get().fail("Segment count value is not correct");
			Assert.fail("Segment count value is not correct");
		}
		return segmentCount;
	}

	// Get user count after creating new user
	public int getSegmentUserCount(int segCount) throws InterruptedException {
		driver.navigate().refresh();
		selUtils.implicitWait(50);
		selUtils.longWait(3000);
		utils.getLocator("segmentBetaPage.calculateNow1Button").isDisplayed();
		// selUtils.waitTillElementToBeClickable(utils.getLocator("segmentBetaPage.calculateNow1Button"));
		Actions action = new Actions(driver);
		action.click(utils.getLocator("segmentBetaPage.calculateNow1Button")).build().perform();
		utils.getLocator("segmentBetaPage.syncButton1").isDisplayed();
		int attempts = 0;
		int segmentCount = 0;
		while (attempts < 20) {
			Actions actions = new Actions(driver);
			actions.click(utils.getLocator("segmentBetaPage.syncButton1")).build().perform();
			Thread.sleep(5000);
			actions.click(utils.getLocator("segmentBetaPage.syncButton1")).build().perform();
			try {
				actions.click(utils.getLocator("segmentBetaPage.syncButton1")).build().perform();
				utils.getLocator("segmentBetaPage.segmentCountLabel").isDisplayed();
				segmentCount = Integer.parseInt(utils.getLocator("segmentBetaPage.segmentCountLabel")
						.getAttribute("innerHTML").trim().replace(",", ""));
				if (segmentCount > segCount) {
					TestListeners.extentTest.get()
							.pass("Successfully verifed segment count after adding a user, new segment count value is: "
									+ segmentCount);
					logger.info("Successfully verifed segment count after adding a user, new segment count value is: "
							+ segmentCount);
					break;
				}
			} catch (Exception e) {
			}
			attempts++;
		}
		return segmentCount;
	}

	public int getLeftSideSegmentSize(String countIndex) throws InterruptedException {
		int count = 0;
		if ("1".equals(countIndex)) {
			Actions action = new Actions(driver);
			action.click(utils.getLocator("segmentBetaPage.calculateNow1Button")).build().perform();
		}
		String segmentCountVal = utils.getLocatorValue("segmentBetaPage.segmentTotalguests").replace("$temp",
				countIndex);
		WebElement segmentCountLabel = driver.findElement(By.xpath(segmentCountVal));
		utils.waitTillVisibilityOfElement(segmentCountLabel, "guestInSegmentCount");
		String val = segmentCountLabel.getAttribute("innerHTML").trim().replace(",", "");
		count = Integer.parseInt(val);
		logger.info("Left Side Segment size is: " + count);
		TestListeners.extentTest.get().info("Left Side Segment size is: " + count);
		return count;
	}

	public void saveSegment(String segmentName) throws InterruptedException {
		selUtils.implicitWait(60);
		utils.waitTillElementToBeClickable((utils.getLocator("segmentBetaPage.saveAndShowButton")));
		selUtils.jsClick(utils.getLocator("segmentBetaPage.saveAndShowButton"));
		utils.waitTillPagePaceDone();
		utils.waitTillSpinnerDisappear(300);
		driver.findElement(By.xpath(utils.getLocatorValue("segmentBetaPage.segmentLabel").replace("temp", segmentName)))
				.isDisplayed();
		logger.info("Segment is saved successfully");
		TestListeners.extentTest.get().pass("Segment is saved successfully");
	}

	public boolean verifySegmentGuestPresence(String segmentName, String user) {
		try {
			// driver.navigate().refresh();
			utils.getLocator("segmentBetaPage.previewGuestsList").isEnabled();
			utils.getLocator("segmentBetaPage.previewGuestsList").click();
			driver.findElement(
					By.xpath(utils.getLocatorValue("segmentBetaPage.segmentLabel").replace("temp", segmentName)))
					.isDisplayed();
			selUtils.jsClick(driver.findElement(
					By.xpath(utils.getLocatorValue("segmentBetaPage.segmentLabel").replace("temp", segmentName))));
			utils.getLocator("segmentBetaPage.findGuestTextbox").isDisplayed();
			Utilities.longWait(3000);
			return searchUser(user);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	private boolean searchUser(String user) {
		user = user.toLowerCase();
		utils.getLocator("segmentBetaPage.findGuestTextbox").clear();
		Actions action = new Actions(driver);
		utils.getLocator("segmentBetaPage.findGuestTextbox").click();
		Utilities.longWait(2000);
		action.sendKeys(utils.getLocator("segmentBetaPage.findGuestTextbox"), user).pause(2000)
				.sendKeys(Keys.BACK_SPACE).pause(1000).sendKeys("m").build().perform();
		Utilities.longWait(2000);
		// selUtils.implicitWait(10);
		try {
			driver.findElement(
					By.xpath(utils.getLocatorValue("segmentBetaPage.autoCompleteLabel").replace("temp", user)))
					.isDisplayed();
		} catch (Exception e) {
			Utilities.longWait(1000);
			utils.getLocator("segmentBetaPage.findGuestTextbox").clear();
			utils.getLocator("segmentBetaPage.findGuestTextbox").click();
			Utilities.longWait(1000);
			action.sendKeys(utils.getLocator("segmentBetaPage.findGuestTextbox"), user).pause(3000).build().perform();
			Utilities.longWait(3000);
			driver.findElement(
					By.xpath(utils.getLocatorValue("segmentBetaPage.autoCompleteLabel").replace("temp", user)))
					.isDisplayed();
		}
		selUtils.jsClick(driver.findElement(
				By.xpath(utils.getLocatorValue("segmentBetaPage.autoCompleteLabel").replace("temp", user))));
		if (prop.getProperty("segmentBetaGreen").equalsIgnoreCase(
				driver.findElement(By.xpath(utils.getLocatorValue("segmentBetaPage.searchLabel").replace("temp", user)))
						.getCssValue("color"))) {
			logger.info("Successfully verified user presence for: " + user);
			TestListeners.extentTest.get().pass("Successfully verified user presence for: " + user);
			return true;
		} else {
			logger.error("Failed to verify new user as current month user");
			TestListeners.extentTest.get().warning("Failed to verify new user as current month user");
		}
		return false;

	}

	public void AddAnotherSegment(String segmentType, String attribute, String operator, String attributeValue)
			throws InterruptedException {
		logger.info("== Adding second segment type and attributes ==");
		TestListeners.extentTest.get().info("== Adding second segment type and attributes ==");
		selectSegmentType(segmentType);
		utils.getLocatorList("segmentBetaPage.addAttributeButton").get(1).isDisplayed();
		utils.getLocatorList("segmentBetaPage.addAttributeButton").get(1).click();
		utils.getLocatorList("segmentBetaPage.attributeDropdownButton").get(2).click();
		utils.getLocatorList("segmentBetaPage.attributeSearchBox").get(1).clear();
		utils.getLocatorList("segmentBetaPage.attributeSearchBox").get(1).sendKeys(attribute);
		WebElement Searchedattribute = driver.findElement(
				By.xpath(utils.getLocatorValue("segmentBetaPage.searchedAttribute").replace("temp", attribute)));
		Searchedattribute.click();
		logger.info("Segment attribute is set as: " + attribute);
		TestListeners.extentTest.get().info("Segment attribute is set as: " + attribute);
		Actions action = new Actions(driver);
		action.click(utils.getLocatorList("segmentBetaPage.operatorDropdownButton").get(1)).build().perform();
		driver.findElements(
				By.xpath(utils.getLocatorValue("segmentBetaPage.operatorValueLabel").replace("temp", operator))).get(1)
				.isDisplayed();
		driver.findElements(
				By.xpath(utils.getLocatorValue("segmentBetaPage.operatorValueLabel").replace("temp", operator))).get(1)
				.click();
		utils.getLocatorList("segmentBetaPage.zipCodeValue").get(1).isDisplayed();
		utils.getLocatorList("segmentBetaPage.zipCodeValue").get(1).sendKeys(attributeValue);
		logger.info("Segment attribute value is set as: " + attributeValue);
		TestListeners.extentTest.get().info("Segment attribute value is set as: " + attributeValue);

	}

	public boolean updateSegment(String segmentName, String attributeValue) {
		try {
			driver.findElement(
					By.xpath(utils.getLocatorValue("segmentBetaPage.segmentLabel").replace("temp", segmentName)))
					.isDisplayed();
			driver.findElement(
					By.xpath(utils.getLocatorValue("segmentBetaPage.segmentLabel").replace("temp", segmentName)))
					.click();
			utils.getLocator("segmentBetaPage.optionLabel").isDisplayed();
			selUtils.jsClick(utils.getLocator("segmentBetaPage.optionLabel"));
			utils.getLocator("segmentBetaPage.editOptionLabel").isDisplayed();
			selUtils.jsClick(utils.getLocator("segmentBetaPage.editOptionLabel"));
			driver.findElement(
					By.xpath(utils.getLocatorValue("segmentBetaPage.segmentNameLabel").replace("temp", segmentName)))
					.isDisplayed();
			selUtils.mouseHoverOverElement(driver.findElement(
					By.xpath(utils.getLocatorValue("segmentBetaPage.segmentNameLabel").replace("temp", segmentName))));
//			driver.findElement(By.xpath("//div[@class='label_edit-icon-button_2qF7C']")).isDisplayed();
//			selUtils.jsClick(driver.findElement(By.xpath("//div[@class='label_edit-icon-button_2qF7C']")));
//			driver.findElement(By.xpath("//*[local-name()='svg'][@data-icon='pen']")).isDisplayed();
//			driver.findElement(By.xpath("//*[local-name()='svg'][@data-icon='pen']")).click();
			// selUtils.jsClick(
			// driver.findElement(By.xpath("//*[local-name()='svg'][@data-icon='pen']")));
			utils.getLocator("segmentBetaPage.nameEditIcon").isDisplayed();
			utils.getLocator("segmentBetaPage.nameEditIcon").click();
			utils.getLocator("segmentBetaPage.segmentNameTextbox").isDisplayed();
			utils.getLocator("segmentBetaPage.segmentNameTextbox").clear();
			utils.getLocator("segmentBetaPage.segmentNameTextbox").sendKeys(segmentName + "updated");
			utils.getLocator("segmentBetaPage.UpdateNameCheckIcon").isDisplayed();
			utils.getLocator("segmentBetaPage.UpdateNameCheckIcon").click();
			Utilities.longWait(2000);
			utils.getLocator("segmentBetaPage.valueTextbox").isDisplayed();
			selUtils.SendKeysViaJS(utils.getLocator("segmentBetaPage.valueTextbox"), "");
			// selUtils.SendKeysViaJS(utils.getLocator("segmentBetaPage.valueTextbox"),
			// attributeValue);
			Actions action = new Actions(driver);
			action.click(utils.getLocatorList("segmentBetaPage.valueTextbox").get(0)).keyDown(Keys.CONTROL)
					.sendKeys("a").keyUp(Keys.CONTROL).sendKeys(Keys.BACK_SPACE).build().perform();
			action.sendKeys(utils.getLocatorList("segmentBetaPage.valueTextbox").get(0), attributeValue)
					.click(utils.getLocator("segmentBetaPage.syncButton")).build().perform();
			logger.info("Segment attribute value is set as: " + attributeValue);
			TestListeners.extentTest.get().info("Segment attribute value is set as: " + attributeValue);
			// saveSegment(segmentName + "updated");

//			as 
			logger.info("Successfully verified segment beta update");
			TestListeners.extentTest.get().info("Segment attribute value is set as: " + attributeValue);

			int segmCount = Integer
					.parseInt(utils.getLocator("segmentBetaPage.segmentCountLabel").getAttribute("innerHTML").trim());
			if (segmCount >= 0) {
				logger.info("Segment count value is appearing on edit page as: "
						+ utils.getLocator("segmentBetaPage.segmentCountLabel").getAttribute("innerHTML").trim());
				TestListeners.extentTest.get().pass("Segment count value is appearing on edit page as: "
						+ utils.getLocator("segmentBetaPage.segmentCountLabel").getAttribute("innerHTML").trim());
			} else {
				logger.error("Segment count value is not appearing correctly on edit page");
				TestListeners.extentTest.get().fail("Segment count value is not appearing correctly on edit page");
				Assert.fail("Segment count value is not appearing correctly on edit page");
			}

			saveSegment(segmentName + "updated");
			// ====== TD =========
			// saveSegment(segmentName + "updated");
//			driver.findElement(By.xpath(
//					utils.getLocatorValue("segmentBetaPage.segmentLabel").replace("temp", segmentName + "updated")))
//					.isDisplayed();
//			logger.info("Segment is updated successfully");
//			TestListeners.extentTest.get().pass("Segment is updated successfully");

			return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	public boolean verifyCustomSegmentCount(String segmentName) {
		try {
			selUtils.implicitWait(50);
			driver.findElement(
					By.xpath(utils.getLocatorValue("segmentBetaPage.segmentLabel").replace("temp", segmentName)))
					.isDisplayed();
			driver.findElement(
					By.xpath(utils.getLocatorValue("segmentBetaPage.segmentLabel").replace("temp", segmentName)))
					.click();
			utils.getLocator("segmentBetaPage.calculateNowLabel").isDisplayed();
			Utilities.longWait(3000);
			// selUtils.jsClick(utils.getLocator("segmentBetaPage.calculateNowLabel"));
			Actions action = new Actions(driver);
			action.click(utils.getLocator("segmentBetaPage.calculateNowLabel")).build().perform();
			// action.sendKeys(utils.getLocator("segmentBetaPage.valueTextbox"),
			// attributeValue)
			// .click(utils.getLocator("segmentBetaPage.calculateNowButton")).build().perform();
			utils.getLocator("segmentBetaPage.segmentCountLabel").isDisplayed();
			int segmentCount = Integer
					.parseInt(utils.getLocator("segmentBetaPage.segmentCountLabel").getAttribute("innerHTML").trim());
			if (segmentCount == 1) {
				logger.info("Segment count value is appearing as: "
						+ utils.getLocator("segmentBetaPage.segmentCountLabel").getAttribute("innerHTML").trim());
				TestListeners.extentTest.get().pass("Segment count value is appearing as: "
						+ utils.getLocator("segmentBetaPage.segmentCountLabel").getAttribute("innerHTML").trim());
				return true;
			} else {
				logger.error("Custom segment count value is not correct: " + segmentCount);
				TestListeners.extentTest.get().fail("Custom segment count value is not correct");
				Assert.fail("Custom segment count value is not correct");
			}
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	public void openSegment(String segmentName) {
		driver.findElement(By.xpath(utils.getLocatorValue("segmentBetaPage.segmentLabel").replace("temp", segmentName)))
				.isDisplayed();
		driver.findElement(By.xpath(utils.getLocatorValue("segmentBetaPage.segmentLabel").replace("temp", segmentName)))
				.click();
	}

	public void setSegmentBetaNameAndType(String segmentName, String segmentType) {
		switchToClassicSegment();
		setSegmentBetaName(segmentName);
		selectSegmentType(segmentType);
		logger.info("Segment beta name and type set successfuly");
		TestListeners.extentTest.get().info("Segment beta name and type set successfuly");
	}

	public void switchToClassicSegment() {
		try {
			utils.implicitWait(1);
			WebElement ele = utils.getLocator("segmentBetaPage.switchToClassicBtn");
			if (ele.isDisplayed()) {
				ele.click();
				utils.waitTillPagePaceDone();
				logger.info("Switched to classic segment page");
				TestListeners.extentTest.get().info("Switched to classic segment page");
			}

		} catch (Exception e) {
			logger.info("Switched to classic segment Button is not available");
			TestListeners.extentTest.get().info("Switched to classic segment Button  is not available");
		}
		utils.implicitWait(50);
	}

	public void setAttribute(String attributeName) {
		utils.waitTillPagePaceDone();
		utils.getLocator("segmentBetaPage.addAttributeButton").isDisplayed();
		utils.getLocator("segmentBetaPage.addAttributeButton").click();
		utils.longWaitInSeconds(1);
		utils.clickWithActions(utils.getLocator("segmentBetaPage.attributeDropdownButton"));
		utils.longWaitInSeconds(1);
		utils.getLocator("segmentBetaPage.attributeSearchBox").clear();
		utils.getLocator("segmentBetaPage.attributeSearchBox").sendKeys(attributeName);
		WebElement Searchedattribute = driver.findElement(
				By.xpath(utils.getLocatorValue("segmentBetaPage.searchedAttribute").replace("temp", attributeName)));
		Searchedattribute.click();
		logger.info("Segment attribute is set as: " + attributeName);
		TestListeners.extentTest.get().info("Segment attribute is set as: " + attributeName);
	}

	public void setSegmentlistType(String segmentName) {
		utils.waitTillPagePaceDone();
		// driver.findElement(By.xpath("//label[contains(text(),'Segment
		// List')]/..//button//button[@tabindex='NaN']"))
		// .click();
		utils.longWaitInSeconds(3);
		List<WebElement> deSelectSegTypeList = driver
				.findElements(By.xpath("//label[contains(text(),'Segment List')]/..//button//button[@tabindex='NaN']"));
		int count = deSelectSegTypeList.size();
		WebElement deSelectSegType = driver.findElement(By.xpath(
				utils.getLocatorValue("segmentBetaPage.deSelectSegListType").replace("temp", String.valueOf(count))));
		deSelectSegType.click();
		utils.longWaitInSeconds(1);

		utils.clickWithActions(utils.getLocator("segmentBetaPage.segmentListDropdown"));
		utils.getLocator("segmentBetaPage.asegmentListSearchBox").clear();
		utils.getLocator("segmentBetaPage.asegmentListSearchBox").sendKeys(segmentName);
		WebElement Searchedattribute = utils.getLocator("segmentBetaPage.searchedsegmentVal");
		Searchedattribute.click();
		logger.info("Segment attribute is set as: " + segmentName);
		TestListeners.extentTest.get().info("Segment attribute is set as: " + segmentName);

	}

	public void setExternalSegmentListype(String segmentName) {
		driver.findElement(
				By.xpath("(//label[contains(text(),'Segment List')]/..//button//button[@tabindex='NaN'])[2]")).click();
		utils.longwait(1000);
		selUtils.jsClick(utils.getLocator("segmentBetaPage.segmentListDropdown"));
		utils.getLocator("segmentBetaPage.asegmentListSearchBox").clear();
		utils.getLocator("segmentBetaPage.asegmentListSearchBox").sendKeys(segmentName);
		WebElement Searchedattribute = utils.getLocator("segmentBetaPage.searchedsegmentVal");
		Searchedattribute.click();
		logger.info("Segment attribute is set as: " + segmentName);
		TestListeners.extentTest.get().info("Segment attribute is set as: " + segmentName);

	}

	public void setValue(String val) {
		utils.waitTillElementToBeClickable(utils.getLocator("segmentBetaPage.zipCodeValue"));
		WebElement element = utils.getLocator("segmentBetaPage.zipCodeValue");
		utils.longWaitInSeconds(1);
		utils.clickWithActions(element);
		utils.sendKeysUsingActionClass(element, val);
		// Actions actions = new Actions(driver);
		// actions.click(element).sendKeys(val).build().perform();
		logger.info("Segment attribute value is set as: " + val);
		TestListeners.extentTest.get().info("Segment attribute value is set as: " + val);
	}

	public void setOperator(String operatorName, String operatorValue) throws InterruptedException {
		utils.longWaitInSeconds(2);
//		String xpath = utils.getLocatorValue("segmentBetaPage.removeSelectedOperator").replace("temp", operatorName);
//		utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath)));
		String xpath1 = utils.getLocatorValue("segmentBetaPage.operatorDropdownButton").replace("temp", operatorName);
		utils.waitTillElementToBeClickable(driver.findElement(By.xpath(xpath1)));
		utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath1)));
		utils.longWaitInSeconds(2);
		driver.findElement(
				By.xpath(utils.getLocatorValue("segmentBetaPage.operatorValueLabel").replace("temp", operatorValue)))
				.isDisplayed();
		utils.clickByJSExecutor(driver, driver.findElement(
				By.xpath(utils.getLocatorValue("segmentBetaPage.operatorValueLabel").replace("temp", operatorValue))));
		logger.info("Segment attribute is set as: " + operatorValue);
		TestListeners.extentTest.get().info("Segment attribute is set as: " + operatorValue);
	}

	public int getSegmentCountSegmentDefination() throws InterruptedException {
		utils.longWaitInSeconds(4);
		utils.clickByJSExecutor(driver, utils.getLocator("segmentBetaPage.calculateNow1Button"));
		utils.longWaitInSeconds(5);
		Actions actions = new Actions(driver);
		actions.click(utils.getLocator("segmentBetaPage.syncButton1")).build().perform();
		// Thread.sleep(4000);
		utils.waitTillVisibilityOfElement(utils.getLocator("segmentBetaPage.segmentDefinationCount"),
				"segmentDefinationCount");
		String val = utils.getLocator("segmentBetaPage.segmentDefinationCount").getAttribute("innerHTML").trim()
				.replace(",", "");
		int count = Integer.parseInt(val);
		logger.info("Segment size is: " + count);
		TestListeners.extentTest.get().info("Segment size is: " + count);
		return count;

	}

	public int getSegmentDefinationTotalGuestCount() throws InterruptedException {
		String val = utils.getLocator("segmentBetaPage.totalGuestsCount").getAttribute("innerHTML").trim().replace(",",
				"");
		int count = Integer.parseInt(val);
		logger.info("Segment total guest count is: " + count);
		TestListeners.extentTest.get().info("Segment total guest count is: " + count);
		return count;

	}

	public int getGuestInSegmentCount() throws InterruptedException {
		int count = 0;
		utils.waitTillPagePaceDone();
		utils.waitTillElementToBeClickable(utils.getLocator("segmentBetaPage.previewGuestsList"));
		utils.getLocator("segmentBetaPage.previewGuestsList").click();
		// utils.getLocator("segmentBetaPage.calculateLink").click();
		// utils.StaleElementclick(driver,
		// utils.getLocator("segmentBetaPage.calculateLink"));
//		utils.longWaitInSeconds(4);
		utils.waitTillSpinnerDisappear(240);
		utils.waitTillVisibilityOfElement(utils.getLocator("segmentBetaPage.guestInSegmentCount"),
				"guestInSegmentCount");
		String val = utils.getLocator("segmentBetaPage.guestInSegmentCount").getAttribute("innerHTML").trim()
				.replace(",", "");
		utils.waitTillVisibilityOfElement(utils.getLocator("segmentBetaPage.guestInSegmentCount"),
				"guestInSegmentCount");
		count = Integer.parseInt(val);
		logger.info("Guest in Segment size is: " + count);
		TestListeners.extentTest.get().info("Guest in Segment size is: " + count);
		return count;
	}

	public String getSegmentGuest() {
		String user = utils.getLocator("segmentBetaPage.listFirstGuest").getText();
		String guest = user.replaceAll("[()]", "");
		logger.info("Segment guest is: " + guest);
		return guest;
	}

	public String verifyGuestPresenceGreenInSegment(String guest) throws InterruptedException {
		utils.getLocator("segmentBetaPage.findGuestTextbox").isDisplayed();
		utils.getLocator("segmentBetaPage.findGuestTextbox").clear();
		utils.getLocator("segmentBetaPage.findGuestTextbox").click();
		utils.getLocator("segmentBetaPage.findGuestTextbox").sendKeys(guest);
		/*
		 * Thread.sleep(1000);
		 * utils.getLocator("segmentBetaPage.findGuestTextbox").clear();
		 * utils.getLocator("segmentBetaPage.findGuestTextbox").sendKeys(guest);
		 */
		// selUtils.implicitWait(2);
		utils.getLocator("segmentBetaPage.autoCompleteresults").click();
		Thread.sleep(2000);
		String color = utils.getLocator("segmentBetaPage.searchedGuest").getAttribute("class");
		logger.info("Segment beta guest search is successfull with color :" + color);
		TestListeners.extentTest.get().info("Segment beta guest search is successfull with color :" + color);
		return color;

	}

	public void setDuration(String duration) throws InterruptedException {
		Thread.sleep(1000);
		utils.clickWithActions(utils.getLocator("segmentBetaPage.durationDrp"));
		List<WebElement> ele = utils.getLocatorList("segmentBetaPage.durationDrpOption");
		Thread.sleep(3500);
		utils.selectListDrpDwnValue(ele, duration);
		TestListeners.extentTest.get().info("Segment attribute is set as: " + duration);
		logger.info("Clicked Duration Dropdown and selected duration as :" + duration);

	}

	public void setValuDrp(String value) throws InterruptedException {
		Thread.sleep(1000);
		utils.clickWithActions(utils.getLocator("segmentBetaPage.valueDrp"));
		List<WebElement> ele = utils.getLocatorList("segmentBetaPage.valueDrpOption");
		Thread.sleep(2000);
		utils.selectListDrpDwnValue(ele, value);
		TestListeners.extentTest.get().info("Segment attribute is set as: " + value);
		logger.info("Clicked Duration Dropdown and selected duration as :" + value);

	}

	public void setDateTime() throws InterruptedException {
		Thread.sleep(2000);
		utils.clickWithActions(utils.getLocator("segmentBetaPage.dateTimeDrp"));
		Thread.sleep(2000);
		driver.findElement(By.xpath("//div[@class='vc-arrow is-left']//*[name()='svg']")).click();
		String pastMonthDate = CreateDateTime.getPastMonthDate();
		Thread.sleep(2000);
		WebElement ele = driver.findElement(By.xpath("//div[contains(@class,'" + pastMonthDate + "')]"));
		ele.isDisplayed();
		utils.clickWithActions(ele);
		utils.clickWithActions(utils.getLocator("segmentBetaPage.durationDrp"));
		utils.clickWithActions(utils.getLocator("segmentBetaPage.durationDrp"));
		Thread.sleep(2000);
		logger.info("Segment Date Time is set as: " + pastMonthDate);
		TestListeners.extentTest.get().info("Segment Date is set as: " + pastMonthDate);
	}

	public String generateSegmentBetName() {
		Utilities.longWait(1000);
		return CreateDateTime.getUniqueString("Segment");
	}

	public int findAndSelectSegment(String segmentName) {
		utils.waitTillPagePaceDone();
		utils.longwait(2);
		switchToClassicSegment();
		utils.getLocator("segmentBetaPage.findSegment").clear();
		utils.getLocator("segmentBetaPage.findSegment").sendKeys(segmentName);
		String calculateXpath = utils.getLocatorValue("segmentBetaPage.calculateLinkWithName").replace("${segmentName}",
				segmentName);
///		Actions actions = new Actions(driver);
//		actions.click(driver.findElement(By.xpath(calculateXpath))).build().perform();
		utils.waitTillElementToBeClickable(driver.findElement(By.xpath(calculateXpath)));
		utils.longWaitInSeconds(1);
		utils.clickUsingActionsClass(driver.findElement(By.xpath(calculateXpath)));
		utils.longWaitInSeconds(5);
		String val = utils.getLocator("segmentBetaPage.reachCount").getAttribute("innerHTML").trim().replace(",", "");
		int count = Integer.parseInt(val);
		utils.getLocator("segmentBetaPage.searchedSegment").click();
		logger.info("Beta Segment Searched Name is :" + segmentName);
		TestListeners.extentTest.get().pass("Beta Segment Searched Name is :" + segmentName);
		logger.info("Beta Segment count on search page is  :" + count);
		TestListeners.extentTest.get().pass("Beta Segment count on search page is  :" + count);
		return count;
	}

	public int findSegmentCountOnAllSegmentsTab(String segmentName) {
		Utilities.longWait(3000);
		switchToClassicSegment();
		utils.getLocator("segmentBetaPage.findSegment").clear();
		utils.getLocator("segmentBetaPage.findSegment").sendKeys(segmentName);
		Utilities.longWait(3000);
		Actions actions = new Actions(driver);
		actions.click(utils.getLocator("segmentBetaPage.calculateLink")).build().perform();
		String value = "";
		boolean status = false;
		int attempts = 0;
		while (attempts < 30) {
			try {
				utils.longWaitInSeconds(8);
				WebElement reachCount = utils.getLocator("segmentBetaPage.reachCount");
				status = reachCount.isDisplayed();
				if (status) {
					value = reachCount.getAttribute("innerHTML").trim().replace(",", "");
					logger.info("Segment reach count appeared: " + value);
					TestListeners.extentTest.get().info("Segment reach count appeared: " + value);
					break;
				}
			} catch (Exception e) {
				logger.info("Segment reach count did not appear after attempt " + attempts + 1);
				TestListeners.extentTest.get().info("Segment reach count did not appear after attempt " + attempts + 1);
			}
			attempts++;
		}

		int count = Integer.parseInt(value);
		utils.refreshPage();
		utils.waitTillPagePaceDone();
		return count;
	}

	public void segmentDefinationDelete() {
		utils.waitTillPagePaceDone();
		utils.StaleElementclick(driver, driver.findElement(By.xpath("//span[normalize-space()='Options']")));
		// driver.findElement(By.xpath("//span[normalize-space()='Options']")).click();
		// driver.findElement(By.xpath("//span[normalize-space()='Delete']")).click();
		utils.clickByJSExecutor(driver, driver.findElement(By.xpath("//span[normalize-space()='Delete']")));
		driver.findElement(By.xpath("//button[text()='Yes, Delete']")).click();
	}

	public void findAndDeleteSegment(String segmentName, String option) {
		Utilities.longWait(3000);
		utils.getLocator("segmentBetaPage.findSegment").clear();
		utils.getLocator("segmentBetaPage.findSegment").sendKeys(segmentName);
		utils.getLocator("segmentBetaPage.segmentEllipsis").isDisplayed();
		utils.getLocator("segmentBetaPage.segmentEllipsis").click();
		utils.longWaitInSeconds(1);
		String xpath = utils.getLocatorValue("segmentBetaPage.segmentEllipsisOption").replace("$temp", option);
		utils.clickWithActions(driver.findElement(By.xpath(xpath)));
		utils.getLocator("campaignsPage.deleteYesBtn").click();
	}

	public void setAndOrNiCondition(String condition) {

		WebElement ele = utils.getLocator("segmentBetaPage.andDrpdwn");
		utils.clickByJSExecutor(driver, utils.getLocator("segmentBetaPage.andDrpdwn"));
//		ele.click();
		List<WebElement> elem = utils.getLocatorList("segmentBetaPage.andDrpdwnOptions");
		utils.selectListDrpDwnValue(elem, condition);

	}

	public void setAttributeAnd(String attributeName, String condition) {
		WebElement ele = utils.getLocator("segmentBetaPage.andDrpdwn");
		ele.click();
		List<WebElement> elem = utils.getLocatorList("segmentBetaPage.andDrpdwnOptions");
		utils.selectListDrpDwnValue(elem, condition);
		utils.longWaitInSeconds(2);
		utils.getLocator("segmentBetaPage.addAttributeButtonAnd").isDisplayed();
		utils.getLocator("segmentBetaPage.addAttributeButtonAnd").click();

		// selUtils.jsClick(utils.getLocator("segmentBetaPage.attributeDropdownButton"));
		utils.clickUsingActionsClass(utils.getLocator("segmentBetaPage.attributeDropdownButton"));
		utils.getLocator("segmentBetaPage.attributeSearchBox").clear();
		utils.getLocator("segmentBetaPage.attributeSearchBox").sendKeys(attributeName);
		WebElement Searchedattribute = driver.findElement(
				By.xpath(utils.getLocatorValue("segmentBetaPage.searchedAttribute").replace("temp", attributeName)));
		Searchedattribute.click();
		logger.info("Segment attribute is set as: " + attributeName);
		TestListeners.extentTest.get().info("Segment attribute is set as: " + attributeName);
	}

	public void setAttributeOR(String attributeName, String condition) {
		setAndOrNiCondition(condition);
		utils.getLocator("segmentBetaPage.addAttributeButtonOr").isDisplayed();
		utils.getLocator("segmentBetaPage.addAttributeButtonOr").click();

		// selUtils.jsClick(utils.getLocator("segmentBetaPage.attributeDropdownButton"));
		utils.clickWithActions(utils.getLocator("segmentBetaPage.attributeDropdownButton"));
		utils.getLocator("segmentBetaPage.attributeSearchBox").clear();
		utils.getLocator("segmentBetaPage.attributeSearchBox").sendKeys(attributeName);
		WebElement Searchedattribute = driver.findElement(
				By.xpath(utils.getLocatorValue("segmentBetaPage.searchedAttribute").replace("temp", attributeName)));
		// Searchedattribute.click();
		utils.clickUsingActionsClass(Searchedattribute);
		logger.info("Segment attribute is set as: " + attributeName);
		TestListeners.extentTest.get().info("Segment attribute is set as: " + attributeName);
	}

	public void setAttributeNI(String attributeName, String condition) {
		WebElement ele = utils.getLocator("segmentBetaPage.andDrpdwn");
		ele.click();
		List<WebElement> elem = utils.getLocatorList("segmentBetaPage.andDrpdwnOptions");
		utils.selectListDrpDwnValue(elem, condition);

		utils.getLocator("segmentBetaPage.addAttributeButtonNi").isDisplayed();
		utils.getLocator("segmentBetaPage.addAttributeButtonNi").click();

		// selUtils.jsClick(utils.getLocator("segmentBetaPage.attributeDropdownButton"));
		utils.clickUsingActionsClass(utils.getLocator("segmentBetaPage.attributeDropdownButton"));
		utils.getLocator("segmentBetaPage.attributeSearchBox").clear();
		utils.getLocator("segmentBetaPage.attributeSearchBox").sendKeys(attributeName);
		WebElement Searchedattribute = driver.findElement(
				By.xpath(utils.getLocatorValue("segmentBetaPage.searchedAttribute").replace("temp", attributeName)));
		Searchedattribute.click();
		logger.info("Segment attribute is set as: " + attributeName);
		TestListeners.extentTest.get().info("Segment attribute is set as: " + attributeName);
	}

	public boolean checkBetaSegmentpermissions() {
		boolean status = false;
		try {
			utils.implicitWait(3);
			WebElement ele = utils.getLocator("segmentBetaPage.newSegmentButton");
			status = utils.checkElementPresent(ele);
		} catch (Exception e) {

		}
		return status;
	}

	public void findSegmentAndSelectSegment(String segmentName) {
		utils.longWaitInSeconds(2);
		utils.getLocator("segmentBetaPage.findSegment").clear();
		utils.getLocator("segmentBetaPage.findSegment").sendKeys(segmentName);
		utils.longWaitInSeconds(2);
		utils.getLocator("segmentBetaPage.searchedSegent").click();
		utils.waitTillPagePaceDone();
		TestListeners.extentTest.get().info("Segment name found and selected is : " + segmentName);
	}

	public void updateSegmentName(String segmentName) {
		WebElement optionsMenu = driver.findElement(By.xpath("//span[normalize-space()='Options']"));
		utils.StaleElementclick(driver, optionsMenu);
		driver.findElement(By.xpath("//span[normalize-space()='Edit']")).click();
		utils.waitTillPagePaceDone();

		utils.getLocator("segmentBetaPage.editSegmentNameBtn").isDisplayed();
		utils.getLocator("segmentBetaPage.editSegmentNameBtn").click();

		Actions action = new Actions(driver);
		selUtils.clearTextUsingJS(utils.getLocator("segmentBetaPage.segmentNameTextbox"));
		utils.getLocator("segmentBetaPage.segmentNameTextbox").sendKeys(segmentName);
		selUtils.longWait(2000);
		action.moveToElement(utils.getLocator("segmentBetaPage.SegmentNameCheckbutton")).click().perform();
		logger.info("Segment name is set as: " + segmentName);
		TestListeners.extentTest.get().info("Segment name is set as: " + segmentName);
	}

	public boolean verifyPresenceAndClick(String locator) {
		boolean status = false;
		try {
			utils.implicitWait(1);
			WebElement ele = driver.findElement(By.xpath(locator));
			ele.click();
			status = true;
		} catch (NoSuchElementException e) {
			status = false;
		}
		utils.implicitWait(50);
		return status;
	}

	// Compares the text of elements in the provided list with the expected string
	public boolean doesSmartSegmentSubHeadersMatch(List<WebElement> eleList, String expectedString) {
		List<String> expectedList = Arrays.asList(expectedString.split(","));
		int actualCount = 0;
		String foundText = "Found: ";
		TestListeners.extentTest.get().info("Expected: " + expectedList);
		logger.info("Expected headers list: " + expectedList);
		for (WebElement ele : eleList) {
			String actualText = ele.getText();
			if (expectedList.contains(actualText)
					|| (expectedList.contains(actualText.split(" ")[0]) && !expectedList.contains(actualText))) {
				foundText += actualText + ", ";
				actualCount++;
			}
		}
		foundText = foundText.substring(0, foundText.length() - 2);
		TestListeners.extentTest.get().info(foundText);
		logger.info(foundText);
		return actualCount == expectedList.size();
	}

	// It matches the element's text in provided list against the expected and
	// returns the count
	public String verifyAndGetTextsCount(List<WebElement> eleList, String expectedString) {
		int actualCount = 0;
		List<String> expectedStringsList = Arrays.asList(expectedString.split(","));

		for (WebElement ele : eleList) {
			String ele_text = selUtils.getTextUsingJS(ele);
			for (String subStringText : expectedStringsList) {
				if (ele_text.toLowerCase().contains(subStringText.toLowerCase())) {
					logger.info("Found text: " + ele_text);
					TestListeners.extentTest.get().info("Found text: " + ele_text);
					actualCount++;
					break;
				}
			}
		}
		return String.valueOf(actualCount);
	}

	public boolean verifyYaxisHeadings(String locator, String expectedString) {
		List<String> expectedStringsList = Arrays.asList(expectedString.split(","));

		for (String yAxisHeading_expectedText : expectedStringsList) {
			String yAxisHeading_xpath = utils.getLocatorValue(locator).replace("$flag", yAxisHeading_expectedText);
			driver.findElement(By.xpath(yAxisHeading_xpath)).isDisplayed();
		}
		return true;
	}

	public boolean doesSmartSegmentOverviewHeadersMatch() {
		List<WebElement> eleList = utils.getLocatorList("segmentBetaPage.smartSegmentCategoriesList");
		List<WebElement> rowCountElement = utils.getLocatorList("segmentBetaPage.smartSegmentCategoryCount");
		WebElement overviewHeaderElement = utils.getLocator("segmentBetaPage.smartSegmentOverviewHeader");
		int actualCount = 0;

		for (int i = 0; i < eleList.size(); i++) {
			WebElement rowHeaderElement = eleList.get(i);
			rowHeaderElement.click();
			String rowHeaderText = rowHeaderElement.getText();
			String overviewHeaderText = overviewHeaderElement.getText();

			String[] rowValueText = rowCountElement.get(i).getText().replaceAll("[^0-9. ]", "").split(" ");
			double rowPercent = Double.parseDouble(rowValueText[0]);
			int rowCount = Integer.parseInt(rowValueText[1]);

			if (overviewHeaderText.contains(rowHeaderText) && rowPercent >= 0 && rowCount >= 0) {
				logger.info("The category named '" + overviewHeaderText + "' has " + rowCount + " count and "
						+ rowPercent + "%");
				TestListeners.extentTest.get().info("The category named '" + overviewHeaderText + "' has " + rowCount
						+ " count and " + rowPercent + "%");
				actualCount++;
			}
		}
		return actualCount == eleList.size();
	}

	public boolean verifyReachabilityPopup() {
		String smartSegmentName = utils.getLocator("segmentBetaPage.smartSegmentOverviewHeader").getText();
		utils.StaleElementclick(driver, utils.getLocator("segmentBetaPage.smartSegmentPreviewReachabilityBtn"));
		String reachabilityPopupHeader_xpath = utils
				.getLocatorValue("segmentBetaPage.smartSegmentReachabilityPopupHeader")
				.replace("$flag", smartSegmentName);
		driver.findElement(By.xpath(reachabilityPopupHeader_xpath)).isDisplayed();
		driver.findElement(By.xpath(reachabilityPopupHeader_xpath + "/following-sibling::button")).click();
		return true;
	}

	public boolean createCampaignWithinSmartSegment(String campaignName) throws InterruptedException {
		boolean status = false;
		utils.getLocator("segmentBetaPage.smartSegmentCreateCampaignBtn").isDisplayed();
		utils.getLocator("segmentBetaPage.smartSegmentCreateCampaignBtn").click();
		utils.getLocator("segmentBetaPage.smartSegmentModalCampaignName").sendKeys(campaignName);
		utils.getLocator("segmentBetaPage.smartSegmentModalCampaignType").click();
		WebElement ele = utils.getLocator("segmentBetaPage.smartSegmentModalContinueBtn");
		utils.waitTillVisibilityOfElement(ele, "Continue Button");
		String parentWindow = driver.getWindowHandle();
		utils.waitTillElementToBeClickable(ele);
		pageObj.campaignsbetaPage().clickContinueBtn();
		selUtils.switchToNewWindow();
		if (driver.getWindowHandle() != parentWindow) {
			status = utils.checkElementPresent(utils.getLocator("segmentBetaPage.campaignSmartSegmentBtn"));
			TestListeners.extentTest.get().info("Campaign named '" + campaignName + "' is created successfully.");
			logger.info("Campaign named '" + campaignName + "' is created successfully.");
		}
		return status;
	}

	public boolean verifySmartSegmentCategoriesInCampaigns(String expectedString, String smartSegmentOverviewHeader) {
		List<WebElement> eleList = utils.getLocatorList("segmentBetaPage.campaignSmartSegmentDropdownCategoriesList");
		List<String> expectedList = Arrays.asList(expectedString.split(","));
		int actualCount = 0;
		String temp = smartSegmentOverviewHeader;

		for (WebElement ele : eleList) {
			String firstSelectedOption_xpath = utils.getLocatorValue("segmentBetaPage.campaignSmartSegmentDropdown")
					.replace("$flag", temp);
			WebElement firstSelectedOption_ele = driver.findElement(By.xpath(firstSelectedOption_xpath));
			firstSelectedOption_ele.click();
			utils.waitTillElementToBeClickable(ele);
			utils.clickByJSExecutor(driver, ele);
			String firstSelectedOption_text = firstSelectedOption_ele.getText();
			TestListeners.extentTest.get().info("The category named '" + firstSelectedOption_text
					+ "' is found under Smart Segment dropdown and clicked.");
			logger.info("The category named '" + firstSelectedOption_text
					+ "' is found under Smart Segment dropdown and clicked.");
			String tooltip_xpath = utils.getLocatorValue("segmentBetaPage.campaignSmartSegmentDropdownTooltip")
					.replace("$flag", firstSelectedOption_text);
			String tooltip_text = selUtils.getTextUsingJS(driver.findElement(By.xpath(tooltip_xpath)));
			temp = firstSelectedOption_text;
			if (expectedList.contains(firstSelectedOption_text) && tooltip_text.contains(firstSelectedOption_text)) {
				actualCount++;
			}
		}
		if (actualCount == expectedList.size()) {
			return true;
		}
		return false;
	}

	public boolean verifySmartSegmentInSegmentBuilder(String expectedString, String segmentToUse,
			int availableOptionsMax) {
		boolean status = false;
		List<String> expectedStringsList = Arrays.asList(expectedString.split(","));
		int totalCount = 0;
		WebElement ele = utils.getLocator("segmentBetaPage.newSegmentSmartSegmentListDropdown");
		ele.isDisplayed();
		utils.waitTillElementToBeClickable(ele);
		ele.click(); // to expand the dropdown
		utils.getLocator("segmentBetaPage.newSegmentDropdownListSelectedOption").click(); // to unselect the default
																							// selected option in
																							// dropdown
		ele.click(); // to re-expand the dropdown
		int availableOptionsCount = utils.getLocatorList("segmentBetaPage.newSegmentDropdownListAvailableOption")
				.size();
		TestListeners.extentTest.get().info("Max Available categories in dropdown are: " + availableOptionsCount);
		logger.info("Max Available categories in dropdown are: " + availableOptionsCount);
		utils.getLocator("segmentBetaPage.newSegmentSmartSegmentListSearchBox").click();

		for (String segmentName : expectedStringsList) {
			utils.getLocator("segmentBetaPage.newSegmentSmartSegmentListSearchBox").clear();
			utils.getLocator("segmentBetaPage.newSegmentSmartSegmentListSearchBox").sendKeys(segmentName);
			String availableOptionXpath = utils.getLocatorValue("segmentBetaPage.newSegmentSmartSegmentListName")
					.replace("$flag", segmentName);
			WebElement availableOption = driver.findElement(By.xpath(availableOptionXpath));
			utils.waitTillVisibilityOfElement(availableOption, segmentName);
			if (availableOption.isDisplayed()) {
				totalCount++;
			}
		}
		TestListeners.extentTest.get().info("Total Smart segment categories are: " + totalCount);
		logger.info("Total Smart segment categories are: " + totalCount);

		String segmentToUseXpath = utils.getLocatorValue("segmentBetaPage.newSegmentSmartSegmentListName")
				.replace("$flag", segmentToUse);
		driver.findElement(By.xpath(segmentToUseXpath)).click();

		if (totalCount == expectedStringsList.size() && availableOptionsCount == availableOptionsMax) {
			TestListeners.extentTest.get().info("Total count and Max Available count are as expected");
			logger.info("Total count and Max Available count are as expected");
			status = true;
		}
		return status;
	}

	public boolean verifyTooltipText(String locator, String tooltipText) {
		boolean status = false;
		String segmentTooltipXpath = locator.replace("temp", tooltipText);
		String actualTooltipText = selUtils.getTextUsingJS(driver.findElement(By.xpath(segmentTooltipXpath)));
		if (actualTooltipText.equalsIgnoreCase(tooltipText)) {
			TestListeners.extentTest.get().info("Tooltip is present with text: " + actualTooltipText);
			logger.info("Tooltip is present with text: " + actualTooltipText);
			status = true;
		}
		return status;
	}

	public int getSegmentCustomerReachabilityCount() {
		int count = 0;
		WebElement countElement = null;
		try {
			countElement = utils.getLocator("segmentBetaPage.segmentOverviewCustomerReachabilityCount");
			utils.waitTillVisibilityOfElement(countElement, "Customer Reachability Count");
		} catch (Exception e) {
			utils.logit("Customer reachability count is not displayed. Refreshing the page...");
			utils.refreshPage();
			utils.waitTillPagePaceDone();
			countElement = utils.getLocator("segmentBetaPage.segmentOverviewCustomerReachabilityCount");
			utils.waitTillVisibilityOfElement(countElement, "Customer Reachability Count");
		}
		String value = countElement.getText().split(" ")[0].replace(",", "");
		count = Integer.parseInt(value);
		utils.logit("Segment's customer reachability count is: " + count);
		return count;
	}

	public String verifySegmentDetailsPageRedirection(String smartSegmentOverviewHeader, String expectedString) {
		utils.longwait(2000);
		utils.getLocator("segmentBetaPage.smartSegmentOverviewDetailsLink").click();
		String parentWindow = selUtils.switchToNewWindow();
		utils.waitTillPagePaceDone();
		TestListeners.extentTest.get().info(
				"The Details link in the Smart Segment overview is clicked. Navigated to the Smart Segment details page.");
		logger.info(
				"The Details link in the Smart Segment overview is clicked. Navigated to the Smart Segment details page.");
		String segmentDetailsHeading = utils.getLocator("segmentBetaPage.segmentDetailsHeading").getAttribute("title");
		List<WebElement> eleList = utils.getLocatorList("segmentBetaPage.segmentDetailsSubHeadingsList");
		boolean doesSmartSegmentHeadingMatch = smartSegmentOverviewHeader.equals(segmentDetailsHeading);
		boolean doesSmartSegmentSubHeadingsMatch = doesSmartSegmentSubHeadersMatch(eleList, expectedString);
		if (doesSmartSegmentHeadingMatch && doesSmartSegmentSubHeadingsMatch) {
			driver.close();
			TestListeners.extentTest.get()
					.info("The Smart Segment details page headings are verified and the window is closed.");
			logger.info("The Smart Segment details page headings are verified and the window is closed.");
		}
		return parentWindow;
	}

//	public void searchAndSelectSegment(String segmentName) {
//		Utilities.longWait(2000);
//		utils.getLocator("segmentBetaPage.findSegment").clear();
//		utils.getLocator("segmentBetaPage.findSegment").sendKeys(segmentName);
//		Utilities.longWait(2000);
//		utils.getLocator("segmentBetaPage.searchedSegment").click();
//		logger.info("Beta Segment Searched Name is :" + segmentName);
//		TestListeners.extentTest.get().pass("Beta Segment Searched Name is :" + segmentName);
//		
//	}

	public String getSegmentGuestList(String apiKey, String segmentID) {
		List<WebElement> weleList = utils.getLocatorList("segmentBetaPage.segmentUsersList");
		String userIsExist = "";
		for (int i = 0; i < weleList.size(); i++) {
			String user = weleList.get(i).getText();
			userIsExist = user.replaceAll("[()]", "").trim();

			Response response = pageObj.endpoints().userInSegment(userIsExist, apiKey, segmentID);
			if (response.jsonPath().getBoolean("result") == true) {
				utils.logit(userIsExist + " users is present in segment with ID: " + segmentID);
				return userIsExist;
			}
		}
		return userIsExist;
	}

	public void createNewSegmentWithLocation(String segmentName, String segmentType, String attribute,
			String headingOfOpearot, String attributeValue) throws InterruptedException {
		utils.waitTillPagePaceDone();
		setSegmentBetaName(segmentName);
		selectSegmentType(segmentType);
		selectAttribute(attribute);
		selectLocation(headingOfOpearot, attributeValue);
		Actions action = new Actions(driver);
		utils.scrollToElement(driver, utils.getLocator("segmentBetaPage.calculateNow1Button"));
		action.click(utils.getLocator("segmentBetaPage.calculateNow1Button")).build().perform();

		logger.info("new segment created : " + segmentName);
		TestListeners.extentTest.get().info("new segment created : " + segmentName);
	}

	private void selectLocation(String headingOfOpearot, String attributeValue) {
		String clickOnClearLocation = utils.getLocatorValue("segmentBetaPage.clickOnClearLocation")
				.replace("$HeadingAboveSearch", headingOfOpearot);
		WebElement clearGender = driver.findElement(By.xpath(clickOnClearLocation));
		clearGender.click();
		String clickOnLocationDrpDown = utils.getLocatorValue("segmentBetaPage.clickOnLocationDrpDown")
				.replace("$HeadingAboveSearch", headingOfOpearot);
		WebElement genderDrp = driver.findElement(By.xpath(clickOnLocationDrpDown));
		// selUtils.jsClick(driver.findElement(By.xpath(clickOnLocationDrpDown)));
		utils.clickUsingActionsClass(genderDrp);
		String clickOnLocationSearch = utils.getLocatorValue("segmentBetaPage.clickOnLocationSearch")
				.replace("$HeadingAboveSearch", headingOfOpearot);
		WebElement searchBox = driver.findElement(By.xpath(clickOnLocationSearch));
		searchBox.sendKeys(attributeValue);
		String clickAfterSearch = utils.getLocatorValue("segmentBetaPage.clickAfterSearchedAttribute").replace("$Field",
				attributeValue);
		driver.findElement(By.xpath(clickAfterSearch)).click();
		logger.info("Segment attribute is set as: " + attributeValue);
		TestListeners.extentTest.get().info("Segment attribute is set as: " + attributeValue);

	}

	public void customListSegment(String csvFilePath) {
		boolean flag = utils.getLocator("segmentBetaPage.customSegmentUpload").isDisplayed();
		Assert.assertTrue(flag, "Custom Segment upload button is not displayed");
		utils.getLocator("segmentBetaPage.uploadCsvButton").isDisplayed();
		utils.getLocator("segmentBetaPage.uploadCsvButton").sendKeys(csvFilePath);
		utils.getLocator("segmentBetaPage.doneBtn").click();
		logger.info("Uploaded custom segment csv file: " + csvFilePath);
		TestListeners.extentTest.get().info("Uploaded custom segment csv file: " + csvFilePath);
	}

	public void enterValueInSegment(String segmentName, String optionName, String optionValue) {
		String xpath = utils.getLocatorValue("segmentBetaPage.enterValueTextInSegment").replace("$optionName",
				optionName);
		driver.findElement(By.xpath(xpath)).click();
		driver.findElement(By.xpath(xpath)).clear();
		driver.findElement(By.xpath(xpath)).sendKeys(optionValue);
		logger.info("Value enter in Segment " + segmentName + " is " + optionValue);
		TestListeners.extentTest.get().info("Value enter in Segment " + segmentName + " is " + optionValue);

	}

	public void setWalletStatusDrp(String option) throws InterruptedException {
		utils.longWaitInSeconds(1);
		WebElement el = utils.getLocator("segmentBetaPage.clearWalletStatusSelectedOption");
		utils.clickWithActions(el);
		utils.clickWithActions(utils.getLocator("segmentBetaPage.walletStatusDrpdwnBtn"));
		utils.longWaitInSeconds(1);
		utils.getLocator("segmentBetaPage.walletStatusSearchBox").clear();
		utils.getLocator("segmentBetaPage.walletStatusSearchBox").sendKeys(option);
		WebElement Searchedattribute = driver.findElement(
				By.xpath(utils.getLocatorValue("segmentBetaPage.searchedWalletStatus").replace("$temp", option)));
		Searchedattribute.click();
		TestListeners.extentTest.get().info("Segment attribute is set as: " + option);
		logger.info("Segment attribute is set as: " + option);
	}

	public void segmentEllipsisOptions(String option) {
		utils.longWaitInSeconds(3);
		utils.getLocator("segmentBetaPage.optionLabel").isDisplayed();
		selUtils.jsClick(utils.getLocator("segmentBetaPage.optionLabel"));
		utils.longWaitInSeconds(1);
		String xpath = utils.getLocatorValue("segmentBetaPage.segmentEllipsisOption").replace("$temp", option);
		utils.clickWithActions(driver.findElement(By.xpath(xpath)));
		utils.logit("Clicked on option '" + option + "' from segment ellipsis options");
		// selUtils.jsClick(utils.getLocator("segmentBetaPage.segmentEllipsisOption"));
		if (option.equalsIgnoreCase("Edit")) {
			utils.waitTillPagePaceDone();
		}
	}

	// Similar to getGuestInSegmentCount() method but without spinner wait
	public int getGuestsInSegmentCount() {
		int count = 0;
		utils.waitTillPagePaceDone();
		WebElement previewGuestsList = utils.getLocator("segmentBetaPage.previewGuestsList");
		utils.waitTillElementToBeClickable(previewGuestsList);
		previewGuestsList.click();
		utils.waitTillSpinnerDisappear();
		WebElement guestsInSegmentCount = utils.getLocator("segmentBetaPage.guestInSegmentCount");
		utils.waitTillVisibilityOfElement(guestsInSegmentCount, "'Guests In Segment' count");
		String guestsCount = guestsInSegmentCount.getAttribute("innerHTML").trim().replace(",", "");
		count = Integer.parseInt(guestsCount);
		logger.info("Guests in Segment size is: " + count);
		TestListeners.extentTest.get().info("Guests in Segment size is: " + count);
		return count;
	}

	public void removeSegmentAttribute() {
		utils.waitTillPagePaceDone();
		utils.getLocator("segmentBetaPage.removeAttributeIcon").isDisplayed();
		utils.getLocator("segmentBetaPage.removeAttributeIcon").click();
		logger.info("Clicked on remove attribute icon");
		TestListeners.extentTest.get().info("Clicked on remove attribute icon");
	}

	public void setCardTypeDrp(String option) throws InterruptedException {
		Thread.sleep(1000);
		WebElement el = utils.getLocator("segmentBetaPage.clearCardListSelectedOption");
		try {
			el.click();
		} catch (Exception e) {
			logger.info("Clear selected card type option icon 'x' is not displayed");
			TestListeners.extentTest.get().info("Clear selected card type option icon 'x' is not displayed");
		}
		utils.clickWithActions(utils.getLocator("segmentBetaPage.cardListDrp"));
		List<WebElement> ele = utils.getLocatorList("segmentBetaPage.searchedDrpOption");
		utils.selectListDrpDwnValue(ele, option);
		TestListeners.extentTest.get().info("Card type is set as: " + option);
		logger.info("Card type is set as: " + option);
	}

	public void setAttributeList(String attribute) {
//		utils.waitTillPagePaceDone();
		List<WebElement> addAttributeList = utils.getLocatorList("segmentBetaPage.addAttributeButton");
		int count = addAttributeList.size();
		WebElement selectAttribute = driver.findElement(By.xpath(
				utils.getLocatorValue("segmentBetaPage.selectAddAttributeBtn").replace("temp", String.valueOf(count))));
		selectAttribute.click();
		List<WebElement> attributeDropdownButton = utils.getLocatorList("segmentBetaPage.attributeDropdownButton");
		int count1 = attributeDropdownButton.size();
		WebElement selectAttributeDropdownButton = driver.findElement(By.xpath(utils
				.getLocatorValue("segmentBetaPage.selectAttributeDrpDwnBtn").replace("temp", String.valueOf(count1))));
		selectAttributeDropdownButton.click();
		List<WebElement> attributeSearchBox = utils.getLocatorList("segmentBetaPage.attributeSearchBox");
		int count2 = attributeSearchBox.size();
		WebElement selectAttributeSearchBox = driver.findElement(By.xpath(utils
				.getLocatorValue("segmentBetaPage.selectAttributeSearchBox").replace("temp", String.valueOf(count2))));
		selectAttributeSearchBox.clear();
		selectAttributeSearchBox.sendKeys(attribute);
		String xpath = utils.getLocatorValue("segmentBetaPage.searchedAttribute").replace("temp", attribute);
		List<WebElement> searchedAttributeList = driver.findElements(By.xpath(xpath));
		int count3 = searchedAttributeList.size();
		WebElement selectSearchedattribute = driver
				.findElement(By.xpath(utils.getLocatorValue("segmentBetaPage.selectSearchedAttribute")
						.replace("temp", attribute).replace("$index", String.valueOf(count3))));
		selectSearchedattribute.click();
		logger.info("Segment attribute is set as: " + attribute);
		TestListeners.extentTest.get().info("Segment attribute is set as: " + attribute);
	}

	public void setOperatorList(String operatorName, String operator) {
		utils.waitTillPagePaceDone();
		String xpath1 = utils.getLocatorValue("segmentBetaPage.operatorDropdownButtonText").replace("temp",
				operatorName);
		List<WebElement> operatorDropdownButtonList = driver.findElements(By.xpath(xpath1));
		// getLocatorList("segmentBetaPage.operatorDropdownButton");
		int count = operatorDropdownButtonList.size();
		WebElement selectOperatorDropdownButton = driver
				.findElement(By.xpath(utils.getLocatorValue("segmentBetaPage.selectOperatorDropdownButton")
						.replace("$operator", operatorName).replace("temp", String.valueOf(count))));
		selectOperatorDropdownButton.click();
		utils.longwait(1000);
		String xpath = utils.getLocatorValue("segmentBetaPage.operatorValueLabelNew").replace("temp", operator);
		List<WebElement> operatorValueLabelList = driver.findElements(By.xpath(xpath));
		int count1 = operatorValueLabelList.size();
		WebElement selectOperatorValueLabel = driver
				.findElement(By.xpath(utils.getLocatorValue("segmentBetaPage.selectOperatorValuelabelNew")
						.replace("$operator", operator).replace("temp", String.valueOf(count1))));
		selectOperatorValueLabel.click();
		logger.info("Segment operator value is set as: " + operator);
		TestListeners.extentTest.get().info("Segment operator value is set as: " + operator);
	}

	public void setValueList(String value) {
		utils.waitTillPagePaceDone();
		List<WebElement> valueList = utils.getLocatorList("segmentBetaPage.zipCodeValue");
		int count = valueList.size();
		WebElement selectValue = driver.findElement(By.xpath(
				utils.getLocatorValue("segmentBetaPage.selectZipCodeValue").replace("temp", String.valueOf(count))));
		selectValue.isDisplayed();
		selectValue.sendKeys(value);
		logger.info("Segment attribute value is set as: " + value);
		TestListeners.extentTest.get().info("Segment attribute value is set as: " + value);

	}

	// Selects a value for the latest dropdown which comes after Operator like
	// 'Location(s)'
	public void setSelectedValue(String dropdownName, String value) {
		utils.waitTillPagePaceDone();
		String dropdownXpath = utils.getLocatorValue("segmentBetaPage.operatorDropdownButtonText").replace("temp",
				dropdownName);
		List<WebElement> sameNameDropdownList = driver.findElements(By.xpath(dropdownXpath));
		int dropdownCount = sameNameDropdownList.size();
		WebElement requiredDropdown = driver
				.findElement(By.xpath(utils.getLocatorValue("segmentBetaPage.selectOperatorDropdownButton")
						.replace("$operator", dropdownName).replace("temp", String.valueOf(dropdownCount))));
		requiredDropdown.click();
		utils.longwait(1000);
		String dropdownValueXpath = utils.getLocatorValue("segmentBetaPage.selectedDropdownValue").replace("temp",
				value);
		WebElement dropdownValue = driver.findElement(By.xpath(dropdownValueXpath));
		dropdownValue.click();
		logger.info(dropdownName + " value is set as: " + value);
		TestListeners.extentTest.get().info(dropdownName + " value is set as: " + value);
	}

	public boolean verifyValueErrorMsg() {
		utils.getLocator("segmentPage.saveAndShowButton").click();
		boolean status = false;
		try {
			utils.implicitWait(3);
			WebElement ele = utils.getLocator("segmentBetaPage.valueErrorText");
			status = utils.checkElementPresent(ele);
		} catch (Exception e) {

		}
		return status;
	}

	public void selectMembership(String membership) {
		utils.getLocator("segmentBetaPage.membershipDropdownButton").isDisplayed();
		// selUtils.jsClick(utils.getLocator("segmentBetaPage.membershipDropdownButton"));
		utils.clickWithActions(utils.getLocator("segmentBetaPage.membershipDropdownButton"));

		utils.getLocator("segmentBetaPage.membershipSearchBox").clear();
		utils.getLocator("segmentBetaPage.membershipSearchBox").sendKeys(membership);
		WebElement searchedMembership = driver.findElement(
				By.xpath(utils.getLocatorValue("segmentBetaPage.searchedMembership").replace("temp", membership)));
		searchedMembership.click();
		logger.info("Segment membership is set as: " + membership);
		TestListeners.extentTest.get().info("Segment membership is set as: " + membership);
	}

	public String getMembership() {
		utils.clickWithActions(utils.getLocator("segmentBetaPage.membershipDropdownButton"));
		String value = utils.getLocator("segmentBetaPage.getSelectedMembership").getText();
		logger.info("Selected Segment membership is: " + value);
		TestListeners.extentTest.get().info("Selected Segment membership is: " + value);
		return value;
	}

	public void setAttributeORIndex(String attributeName, String condition, String index, String index2) {
		setAndOrNiCondition(condition);
		String xpath = utils.getLocatorValue("segmentBetaPage.addAttributeButtonOrIndex").replace("$index", index);
		driver.findElement(By.xpath(xpath)).isDisplayed();
		driver.findElement(By.xpath(xpath)).click();

		// selUtils.jsClick(utils.getLocator("segmentBetaPage.attributeDropdownButton"));
		utils.clickWithActions(utils.getLocator("segmentBetaPage.attributeDropdownButton"));
		utils.longWaitInSeconds(2);
		utils.getLocator("segmentBetaPage.attributeSearchBox").clear();
		utils.getLocator("segmentBetaPage.attributeSearchBox").sendKeys(attributeName);
		String xpath2 = utils.getLocatorValue("segmentBetaPage.searchedAttributeIndex").replace("temp", attributeName)
				.replace("$index", index2);
		WebElement Searchedattribute = driver.findElement(By.xpath(xpath2));
		// Searchedattribute.click();
		utils.longWaitInSeconds(2);
		utils.clickUsingActionsClass(Searchedattribute);
		logger.info("Segment attribute is set as: " + attributeName);
		TestListeners.extentTest.get().info("Segment attribute is set as: " + attributeName);
	}

	public void setAttributeAndIndex(String attributeName, String condition, String index) {
		WebElement ele = utils.getLocator("segmentBetaPage.andDrpdwn");
		ele.click();
		List<WebElement> elem = utils.getLocatorList("segmentBetaPage.andDrpdwnOptions");
		utils.selectListDrpDwnValue(elem, condition);
		utils.longWaitInSeconds(2);
		String xpath = utils.getLocatorValue("segmentBetaPage.addAttributeButtonAndIndex").replace("$index", index);
		driver.findElement(By.xpath(xpath)).isDisplayed();
		driver.findElement(By.xpath(xpath)).click();
		// selUtils.jsClick(utils.getLocator("segmentBetaPage.attributeDropdownButton"));
		utils.clickUsingActionsClass(utils.getLocator("segmentBetaPage.attributeDropdownButton"));
		utils.getLocator("segmentBetaPage.attributeSearchBox").clear();
		utils.getLocator("segmentBetaPage.attributeSearchBox").sendKeys(attributeName);
		WebElement Searchedattribute = driver.findElement(
				By.xpath(utils.getLocatorValue("segmentBetaPage.searchedAttribute").replace("temp", attributeName)));
		Searchedattribute.click();
		logger.info("Segment attribute is set as: " + attributeName);
		TestListeners.extentTest.get().info("Segment attribute is set as: " + attributeName);
	}

	public void selectAttribute(String attributeType, String attributeTypeName) {
		try {
			utils.implicitWait(15);
			String xpath1 = utils.getLocatorValue("segmentBetaPage.removeSelectedOperator").replace("temp",
					attributeType);
			utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath1)));
			utils.implicitWait(50);
		} catch (Exception e) {
			utils.implicitWait(50);
			logger.info("Clear selected attribute type option icon 'x' is not displayed");
			TestListeners.extentTest.get().info("Clear selected attribute type option icon 'x' is not displayed");
		}
		String xpath = utils.getLocatorValue("segmentPage.clickOnAttributeType").replace("$attributeType",
				attributeType);
//		driver.findElement(By.xpath(xpath)).click();
		utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath)));
		utils.longWaitInSeconds(1);
		String attributeSearchBoxXpath = utils.getLocatorValue("segmentBetaPage.searchBoxAttributeType")
				.replace("$attributeType", attributeType);
		// driver.findElement(By.xpath(attributeSearchBoxXpath)).clear();
		driver.findElement(By.xpath(attributeSearchBoxXpath)).sendKeys(attributeTypeName);
		WebElement Searchedattribute = driver
				.findElement(By.xpath(utils.getLocatorValue("segmentBetaPage.searchedAttributeWithAttributeType")
						.replace("$attributeType", attributeType).replace("$selectAttributeName", attributeTypeName)));
		// Searchedattribute.click();
		utils.clickUsingActionsClass(Searchedattribute);
		logger.info("Segment attribute is set as: " + attributeTypeName);
		TestListeners.extentTest.get().info("Segment attribute is set as: " + attributeTypeName);
	}

	// It is used to select another value for the same attribute type
	public void selectAttributeValueOnly(String attributeType, String attributeValue) {
		String xpath = utils.getLocatorValue("segmentPage.clickOnAttributeType").replace("$attributeType",
				attributeType);
		WebElement attributeTypeElement = driver.findElement(By.xpath(xpath));
		utils.clickByJSExecutor(driver, attributeTypeElement);
		String searchBoxXpath = utils.getLocatorValue("segmentBetaPage.searchBoxAttributeType")
				.replace("$attributeType", attributeType);
		WebElement searchBoxElement = driver.findElement(By.xpath(searchBoxXpath));
		searchBoxElement.sendKeys(attributeValue);
		WebElement searchedAttribute = driver
				.findElement(By.xpath(utils.getLocatorValue("segmentBetaPage.searchedAttributeWithAttributeType")
						.replace("$attributeType", attributeType).replace("$selectAttributeName", attributeValue)));
		utils.clickUsingActionsClass(searchedAttribute);
		logger.info("Segment attribute value is selected: " + searchedAttribute);
		TestListeners.extentTest.get().info("Segment attribute value is selected: " + searchedAttribute);
	}

	public void addAttributeBUttton() {
		utils.waitTillElementToBeClickable(utils.getLocator("segmentBetaPage.addAttributeButton"));
		utils.getLocator("segmentBetaPage.addAttributeButton").isDisplayed();
		utils.getLocator("segmentBetaPage.addAttributeButton").click();
		logger.info("Clicked on add attribute button");
		TestListeners.extentTest.get().info("Clicked on add attribute button");
	}

	public void setOperatorText(String operatorName, String operatorValue) throws InterruptedException {
		utils.longWaitInSeconds(2);
		String xpath = utils.getLocatorValue("segmentBetaPage.removeSelectedOperatorText").replace("temp",
				operatorName);
		utils.clickWithActions(driver.findElement(By.xpath(xpath)));
//		utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath)));
		String xpath1 = utils.getLocatorValue("segmentBetaPage.operatorDropdownButtonText").replace("temp",
				operatorName);
		driver.findElement(By.xpath(xpath1)).click();
		driver.findElement(
				By.xpath(utils.getLocatorValue("segmentBetaPage.operatorValueLabel").replace("temp", operatorValue)))
				.isDisplayed();
		utils.clickByJSExecutor(driver, driver.findElement(
				By.xpath(utils.getLocatorValue("segmentBetaPage.operatorValueLabel").replace("temp", operatorValue))));
		logger.info("Segment attribute is set as: " + operatorValue);
		TestListeners.extentTest.get().info("Segment attribute is set as: " + operatorValue);
	}

	public boolean verifyInsufficientPrivilegesPopUp() {
		Boolean status = false;
		utils.waitTillElementToBeClickable((utils.getLocator("segmentBetaPage.saveAndShowButton")));
		selUtils.jsClick(utils.getLocator("segmentBetaPage.saveAndShowButton"));
		try {
			utils.implicitWait(3);
			WebElement ele = utils.getLocator("segmentBetaPage.insufficientPrivilegesPopUp");
			status = utils.checkElementPresent(ele);
		} catch (Exception e) {

		}
		utils.implicitWait(50);
		return status;
	}

	public void selectsecondAttribute(String attribute) {
		utils.getLocator("segmentBetaPage.addAttributeButton").isDisplayed();
		utils.getLocator("segmentBetaPage.addAttributeButton").click();
		utils.clickWithActions(utils.getLocator("segmentBetaPage.secondAttributeDropdownButton"));
		utils.longWaitInSeconds(1);
		List<WebElement> attributeSearchBox = utils.getLocatorList("segmentBetaPage.attributeSearchBox");
		int count2 = attributeSearchBox.size();
		WebElement selectAttributeSearchBox = driver.findElement(By.xpath(utils
				.getLocatorValue("segmentBetaPage.selectAttributeSearchBox").replace("temp", String.valueOf(count2))));
		selectAttributeSearchBox.clear();
		selectAttributeSearchBox.sendKeys(attribute);
		String xpath2 = utils.getLocatorValue("segmentBetaPage.searchedAttributeIndex").replace("temp", attribute)
				.replace("$index", String.valueOf(count2));
		WebElement Searchedattribute = driver.findElement(By.xpath(xpath2));
		// Searchedattribute.click();
		utils.longWaitInSeconds(2);
		utils.clickUsingActionsClass(Searchedattribute);
		logger.info("Segment second attribute is set as: " + attribute);
		TestListeners.extentTest.get().info("Segment second attribute is set as: " + attribute);
	}

	public boolean customSegmentProcessed(String csvName) {
//		utils.longWaitInSeconds(20);
		String xpath = utils.getLocatorValue("segmentBetaPage.customSegmentProcessed").replace("$csvName", csvName);
		utils.waitTillVisibilityOfElement(driver.findElement(By.xpath(xpath)), "Custom Segment Processed");
		boolean status = driver.findElement(By.xpath(xpath)).isDisplayed();
		return status;
	}

	public List<String> checkCustomerReachability() {
		List<String> summaryData = new ArrayList<String>();
		List<WebElement> summaryTable = utils.getLocatorList("segmentBetaPage.segmentCustomerReachability");
		int col = summaryTable.size();
		for (int i = 0; i < col; i++) {
			String val = summaryTable.get(i).getText();
			summaryData.add(val);
		}
		return summaryData;
	}

	public int getCustomerReachabilityCount() {
		WebElement ele = utils.getLocator("segmentBetaPage.segmentCustomerReachabilityCount");

		ele.isDisplayed();
		String val = ele.getText();
		String[] parts = val.split(" ");
		String numberPart = parts[0];
		String cleanedNumber = numberPart.replace(",", "");
		int count = Integer.parseInt(cleanedNumber);
		return count;
	}

	public void buildFromSuperFan() {
		WebElement superFanRow = utils.getLocator("segmentBetaPage.SuperFanRow");
		superFanRow.isDisplayed();
		superFanRow.click();
		logger.info("Clicked super fan row");
		TestListeners.extentTest.get().info("Clicked super fan row");
		WebElement buildFromThisBtn = utils.getLocator("segmentBetaPage.buildFromThisBtn");
		buildFromThisBtn.isDisplayed();
		buildFromThisBtn.click();
		logger.info("Clicked build from this button");
		TestListeners.extentTest.get().info("Clicked build from this button");
		utils.switchToWindow();
	}

	public void updateSetSegmentName(String segmentName) {
		utils.getLocator("segmentBetaPage.editSegmentNameBtn").isDisplayed();
		utils.getLocator("segmentBetaPage.editSegmentNameBtn").click();

		Actions action = new Actions(driver);
		selUtils.clearTextUsingJS(utils.getLocator("segmentBetaPage.segmentNameTextbox"));
		utils.getLocator("segmentBetaPage.segmentNameTextbox").sendKeys(segmentName);
		selUtils.longWait(2000);
		action.moveToElement(utils.getLocator("segmentBetaPage.SegmentNameCheckbutton")).click().perform();
		logger.info("Segment name is set as: " + segmentName);
		TestListeners.extentTest.get().info("Segment name is set as: " + segmentName);
	}

	public String getRandomGuestFromSegmentList(String apiKey, String segmentID, int limit) {
		List<WebElement> weleList = utils.getLocatorList("segmentBetaPage.segmentUsersList");
		String userInList;
		int flag = 0;
		Random random = new Random();
		do {
			flag++;
			int randomIndex = random.nextInt(weleList.size());
			userInList = weleList.get(randomIndex).getText().replaceAll("[()]", "").trim();
			Response response = pageObj.endpoints().userInSegment(userInList, apiKey, segmentID);
			if (response.jsonPath().getBoolean("result")) {
				break;
			}
		} while (flag <= limit);
		utils.logit(" Random user selected is === " + userInList);
		return userInList;
	}

	public void updateCustomList(String massCampaignName, String option, String userEmail) throws InterruptedException {
		switchToClassicSegment();
		utils.waitTillPagePaceDone();
		utils.getLocator("segmentBetaPage.findSegment").clear();
		utils.getLocator("segmentBetaPage.findSegment").sendKeys(massCampaignName);
		driver.findElement(
				By.xpath(utils.getLocatorValue("segmentBetaPage.customList").replace("$temp", massCampaignName)))
				.click();
		pageObj.segmentsBetaPage().segmentEllipsisOptions("Edit");
		setOperator("List Type", "New Custom List");
		utils.getLocator("segmentBetaPage.previewGuestsListCount").click();
		driver.findElement(By.tagName("textarea")).sendKeys(userEmail);
		selUtils.jsClick(utils.getLocator("segmentBetaPage.doneBtn"));
		utils.waitTillPagePaceDone();
		utils.waitTillElementToBeClickable((utils.getLocator("segmentBetaPage.saveAndShowButton")));
		selUtils.jsClick(utils.getLocator("segmentBetaPage.saveAndShowButton"));
		utils.waitTillPagePaceDone();
	}

	public void setValueDaysAgo(String val) {
		utils.waitTillElementToBeClickable(utils.getLocator("segmentBetaPage.daysAgoValue"));
		WebElement element = utils.getLocator("segmentBetaPage.daysAgoValue");
		utils.longWaitInSeconds(1);
		utils.clickWithActions(element);
		utils.sendKeysUsingActionClass(element, val);
		// Actions actions = new Actions(driver);
		// actions.click(element).sendKeys(val).build().perform();
		logger.info("Segment attribute value is set as: " + val);
		TestListeners.extentTest.get().info("Segment attribute value is set as: " + val);
	}

	public void waitTillCustomerReachabilityClickable() {
		try {
			String wEle = utils.getLocatorValue("segmentBetaPage.reachabilityAppearanceIcon");
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(200));
			wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(wEle)));
			logger.info("Segment Side Pannel Clickable");
			TestListeners.extentTest.get().info("Segment Side Pannel Clickable");
		} catch (Exception e) {
			logger.info(e.getMessage());
			TestListeners.extentTest.get().info(e.getMessage());
		}
	}

	public String getAuditPageBuilderClauseText() {
		utils.scrollToElement(driver, utils.getLocator("segmentBetaPage.builderClauseText"));
		String builderClauseText = utils.getLocator("segmentBetaPage.builderClauseText").getText();
		Pattern pattern = Pattern.compile("\\.\\`channel\\` = '([^']+)'");
		Matcher matcher = pattern.matcher(builderClauseText);
		String channel = null;
		if (matcher.find()) // find the first (and only) occurrence
		{
			channel = matcher.group(1); // extract the value inside quotes
			if (channel == null) {
				throw new IllegalArgumentException("No channel found in builderClauseText: " + builderClauseText);
			}
		}
		return channel;
	}

	public void clickOnCancelButton() {
		WebElement cancelButton = utils.getLocator("segmentBetaPage.cancelButton");
		utils.waitTillElementToBeClickable(cancelButton);
		cancelButton.click();
	}

	public void clickOnDontSaveButton() {
		WebElement dontSaveButton = utils.getLocator("segmentBetaPage.dontSave");
		utils.waitTillElementToBeClickable(dontSaveButton);
		dontSaveButton.click();
		utils.waitTillPagePaceDone();
	}
	public int getSegmentCountWithoutCalculateBtn(){
		utils.longWaitInSeconds(1);
		utils.clickByJSExecutor(driver, utils.getLocator("segmentBetaPage.segmentSizeLoader"));
		
		try {
			WebElement ele = utils.getLocator("segmentBetaPage.segmentSizeLoaderActive");
			utils.waitTillElementDisappear(ele);
			logger.info("Segment Size Icon Wrapper is disappeared");
			TestListeners.extentTest.get().info("Segment Size Icon Wrapper is disappeared");
		} catch (Exception e) {
			logger.info("Segment Size Icon Wrapper not appeared");
			TestListeners.extentTest.get().info("Segment Size Icon Wrapper not appeared");
		}
		utils.waitTillVisibilityOfElement(utils.getLocator("segmentBetaPage.segmentCountLabel"), "count visible");
		utils.getLocator("segmentBetaPage.segmentCountLabel").isDisplayed();
		segmentCount = Integer.parseInt(utils.getLocator("segmentBetaPage.segmentCountLabel").getAttribute("innerHTML")
				.trim().replace(",", ""));
		logger.info("Segment size is: " + segmentCount);
		TestListeners.extentTest.get().info("Segment size is: " + segmentCount);
		return segmentCount;

	}
}
