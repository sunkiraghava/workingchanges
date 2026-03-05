package com.punchh.server.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class SettingsPage {
	static Logger logger = LogManager.getLogger(SettingsPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	String ftp = "FTP";

	public SettingsPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(this.driver);
		selUtils = new SeleniumUtilities(driver);

	}

	public void createNewFtp(String ftpName, String hostname, String port, String username, String password,
			String path, String email) {

		logger.info("Creating new FTP end point");
		TestListeners.extentTest.get().info("Creating new FTP end point");
		utils.getLocator("settingsPage.ftpHeading").isDisplayed();
		utils.getLocator("settingsPage.addFtpLink").isDisplayed();
		utils.getLocator("settingsPage.addFtpLink").click();
		utils.getLocator("settingsPage.newFtpHeading").isDisplayed();

		utils.getLocator("settingsPage.ftpNameTextbox").isDisplayed();
		utils.getLocator("settingsPage.ftpNameTextbox").clear();
		utils.getLocator("settingsPage.ftpNameTextbox").sendKeys(ftpName);

		logger.info("Ftp name is set as: " + ftpName);
		TestListeners.extentTest.get().info("Ftp name is set as: " + ftpName);

		utils.getLocator("settingsPage.hostTextbox").clear();
		utils.getLocator("settingsPage.hostTextbox").sendKeys(hostname);

		utils.getLocator("settingsPage.portTextbox").clear();
		utils.getLocator("settingsPage.portTextbox").sendKeys(port);

		utils.getLocator("settingsPage.usernameTextbox").clear();
		utils.getLocator("settingsPage.usernameTextbox").sendKeys(username);

		utils.getLocator("settingsPage.passwordTextbox").clear();
		utils.getLocator("settingsPage.passwordTextbox").sendKeys(password);

		utils.getLocator("settingsPage.pathTextbox").clear();
		utils.getLocator("settingsPage.pathTextbox").sendKeys(path);

		logger.info("Ftp path is set as: " + path);
		TestListeners.extentTest.get().info("Ftp path is set as: " + path);

		utils.getLocator("settingsPage.emailTextbox").clear();
		utils.getLocator("settingsPage.emailTextbox").sendKeys(email);

		utils.getLocator("settingsPage.saveButton").click();
		utils.getLocator("settingsPage.ftpSaveMsg").isDisplayed();

		driver.findElement(By.xpath(utils.getLocatorValue("settingsPage.h4FtpHeading").replace("temp", ftpName)))
				.isDisplayed();

		logger.info("New FTP connection is created");
		TestListeners.extentTest.get().info("New FTP connection is created");
	}

	public void editFtpConnection(String ftpName) {

		driver.findElement(By.xpath(utils.getLocatorValue("settingsPage.h4FtpHeading").replace("temp", ftpName)))
				.isDisplayed();

		driver.findElement(By.xpath(utils.getLocatorValue("settingsPage.ellipsisButton").replace("temp", ftpName)))
				.isDisplayed();
		driver.findElement(By.xpath(utils.getLocatorValue("settingsPage.ellipsisButton").replace("temp", ftpName)))
				.click();
		// h4/span[.='AutoFtp11592620012022']/ancestor::div[@class='card
		// ']//ul//span[.='Edit']
		driver.findElement(By.xpath(utils.getLocatorValue("settingsPage.editLabel").replace("temp", ftpName)))
				.isDisplayed();

		driver.findElement(By.xpath(utils.getLocatorValue("settingsPage.editLabel").replace("temp", ftpName))).click();

		utils.getLocator("settingsPage.emailTextbox").clear();
		utils.getLocator("settingsPage.emailTextbox").sendKeys(Utilities.getConfigProperty("updatedFtpEmail"));

		// utils.getLocator("settingsPage.ftpNameTextbox").clear();
		// utils.getLocator("settingsPage.ftpNameTextbox").sendKeys(ftpName+"Updated");
		utils.getLocator("settingsPage.saveButton").click();
		utils.getLocator("settingsPage.updatedFtpMsgLabel").isDisplayed();

		selUtils.mouseHoverOverElement(utils.getLocator("menuPage.dashboardLink"));
		utils.getLocator("menuPage.ftpLink").click();

		driver.findElement(By.xpath(utils.getLocatorValue("settingsPage.h4FtpHeading").replace("temp", ftpName)))
				.isDisplayed();
		logger.info("New FTP connection is updated");
		TestListeners.extentTest.get().info("New FTP connection is updated");
	}

	public void disbaleEnableFtpConnection(String ftpName) {
		driver.findElement(By.xpath(utils.getLocatorValue("settingsPage.ellipsisButton").replace("temp", ftpName)))
				.isDisplayed();

		selUtils.jsClick(driver
				.findElement(By.xpath(utils.getLocatorValue("settingsPage.ellipsisButton").replace("temp", ftpName))));
		driver.findElement(By.xpath(utils.getLocatorValue("settingsPage.disableLabel").replace("temp", ftpName)))
				.isDisplayed();
		driver.findElement(By.xpath(utils.getLocatorValue("settingsPage.disableLabel").replace("temp", ftpName)))
				.click();
		Alert alert = driver.switchTo().alert();
		if (alert.getText().equals(
				"Are you sure you want to disable this FTP Endpoint? This FTP Endpoint will be disconnected from all connected schedules.")) {
			alert.accept();
			utils.getLocator("settingsPage.deactivatedEndPointLabel").isDisplayed();
			logger.info("New FTP connection is disbaled");
			TestListeners.extentTest.get().info("New FTP connection is disbaled");

		} else {
			logger.error("Disbaling FTP alert message is not correct");
			Assert.fail("Disbaling FTP alert message is not correct");
		}

		driver.findElement(By.xpath(utils.getLocatorValue("settingsPage.ellipsisButton").replace("temp", ftpName)))
				.isDisplayed();
		driver.findElement(By.xpath(utils.getLocatorValue("settingsPage.ellipsisButton").replace("temp", ftpName)))
				.click();
		driver.findElement(By.xpath(utils.getLocatorValue("settingsPage.enableLabel").replace("temp", ftpName)))
				.isDisplayed();
		driver.findElement(By.xpath(utils.getLocatorValue("settingsPage.enableLabel").replace("temp", ftpName)))
				.click();
		utils.getLocator("settingsPage.activatedEndPointLabel").isDisplayed();
		logger.info("New FTP connection is enabled");
		TestListeners.extentTest.get().info("New FTP connection is enabled");
	}

	public void deleteFtpConnection(String ftpName) {
		driver.findElement(By.xpath(utils.getLocatorValue("settingsPage.ellipsisButton").replace("temp", ftpName)))
				.isDisplayed();
		driver.findElement(By.xpath(utils.getLocatorValue("settingsPage.ellipsisButton").replace("temp", ftpName)))
				.click();
		driver.findElement(By.xpath(utils.getLocatorValue("settingsPage.deleteLabel").replace("temp", ftpName)))
				.isDisplayed();
		driver.findElement(By.xpath(utils.getLocatorValue("settingsPage.deleteLabel").replace("temp", ftpName)))
				.click();
		Alert alert = driver.switchTo().alert();
		if (alert.getText().equals("Are you sure you want to delete this FTP Endpoint?")) {
			alert.accept();

		} else {
			logger.error("Disbaling FTP alert message is not correct");
			Assert.fail("Disbaling FTP alert message is not correct");
		}

		utils.getLocator("settingsPage.ftpDeletedMsgLabel").isDisplayed();
		selUtils.implicitWait(2);

		if (driver.findElements(By.xpath(utils.getLocatorValue("settingsPage.h4FtpHeading").replace("temp", ftpName)))
				.size() == 0) {
			logger.info("New FTP connection is deleted");
			TestListeners.extentTest.get().info("New FTP connection is deleted");
		} else {
			logger.info("Error in deleting New FTP connection");
			TestListeners.extentTest.get().info("Error in deleting New FTP connection");
		}

		selUtils.implicitWait(40);
	}

	public void membershipSetting(String membershipLevelName, String pointsMultiplicationFactor) {
		String xpath = utils.getLocatorValue("settingsPage.clickmembershipLevelByName").replace("$MembershipLevelName",
				membershipLevelName);
		driver.findElement(By.xpath(xpath)).click();
		utils.longWaitInSeconds(5);
		utils.getLocator("settingsPage.updateRewardValiue").clear();
		utils.getLocator("settingsPage.updateRedemptionMark").clear();
		/*
		 * utils.getLocator("settingsPage.updatePointsMultiplicationFactor").clear();
		 * utils.getLocator("settingsPage.updatePointsMultiplicationFactor").sendKeys(
		 * pointsMultiplicationFactor);
		 */
		utils.waitTillElementToBeClickable(utils.getLocator("settingsPage.clickUpdateButton"));
		utils.clickByJSExecutor(driver, utils.getLocator("settingsPage.clickUpdateButton"));
		// utils.getLocator("settingsPage.clickUpdateButton").click();
		logger.info(membershipLevelName + " is updateded successfully ");
		TestListeners.extentTest.get().info(membershipLevelName + " is updated successfully ");
	}

	public void membershipSettingUpdateOrClear(String membershipLevelName, String cases, String reward,
			String membershipLevelId) {
		String xpath = utils.getLocatorValue("settingsPage.clickmembershipLevelById").replace("$MembershipID",
				membershipLevelId);
		driver.findElement(By.xpath(xpath)).click();
		utils.waitTillPagePaceDone();
		switch (cases) {
		case "update":
			WebElement weleSelect = utils.getLocator("settingsPage.membershipLevelAttainmentRewardSelect");
			utils.selectDrpDwnValue(weleSelect, reward);
			utils.scrollToElement(driver, utils.getLocator("settingsPage.clickUpdateButton"));
			utils.getLocator("settingsPage.clickUpdateButton").click();
			logger.info(membershipLevelName + " is updateded successfully with reward = " + reward);
			TestListeners.extentTest.get()
					.pass(membershipLevelName + " is updateded successfully with reward = " + reward);
			break;
		case "remove":
			utils.getLocator("settingsPage.membershipLevelAttainmentReward").click();
			utils.getLocator("settingsPage.clearMembershipLevelAttainmentReward").click();
			utils.clickByJSExecutor(driver, utils.getLocator("settingsPage.clickUpdateButton"));
			// utils.getLocator("settingsPage.clickUpdateButton").click();
			logger.info(membershipLevelName + " is updateded successfully after clearing reward with name : " + reward);
			TestListeners.extentTest.get().info(
					membershipLevelName + " is updateded successfully after clearing reward with name : " + reward);
			break;
		}
	}

	public void clickOnRadioButtonForApplePass(WebElement radioFlagWele, String toBeOnOff) {

		String checkBoxValue = radioFlagWele.getAttribute("checked");

		if ((checkBoxValue == null) && (toBeOnOff.equalsIgnoreCase("OFF"))) {
			logger.info("Auto checking box Unchecked and do not cliked it as checkBoxFlag= " + toBeOnOff);
			TestListeners.extentTest.get()
					.info("Auto checking box Unchecked and do not cliked it as checkBoxFlag= " + toBeOnOff);
		} else if ((checkBoxValue == null) && (toBeOnOff.equalsIgnoreCase("ON"))) {
			utils.clickByJSExecutor(driver, radioFlagWele);
			logger.info("Autocheckin box is unchecked and user want to check the chekedbox");
			TestListeners.extentTest.get().info("Autocheckin box is unchecked and user want to check the chekedbox");

		} else if (checkBoxValue.equalsIgnoreCase("true") && (toBeOnOff.equalsIgnoreCase("OFF"))) {
			utils.clickByJSExecutor(driver, radioFlagWele);

			logger.info("Autocheckin box is already cheked and user want to uncheck ");
			TestListeners.extentTest.get().info("Autocheckin box is already cheked and user want to uncheck ");
		} else if (checkBoxValue.equalsIgnoreCase("true") && (toBeOnOff.equalsIgnoreCase("ON"))) {

			logger.info("Autocheckin box is already checked and user want to check the chekedbox, so do not click");
			TestListeners.extentTest.get()
					.info("Autocheckin box is already checked and user want to check the chekedbox, so do not click");

		}
	}

	public void editPreferredLanguage(String language) {
		// click Address
		utils.getLocator("settingsPage.addressBtn").click();

		// click preferred Language
		utils.getLocator("settingsPage.preferredLanguage").click();

		// select value from drop down
		utils.selectListDrpDwnValue(utils.getLocatorList("settingsPage.preferredLanguageLst"), language);

		// click saveBtn
		utils.getLocator("settingsPage.saveButton").click();

		String text = getErrorMessage();
		Assert.assertEquals(text, "Business was successfully updated.", "Error in updating business");
		logger.info("Business was successfully updated.");
		TestListeners.extentTest.get().info("Business was successfully updated.");

	}

	public String heartbeatLogPageVisibility() {
		String text = "";
		boolean flag = utils.getLocator("settingsPage.hearbeatLogVisibility").isDisplayed();
		Assert.assertTrue(flag, "Heartbeat Title is not visible");
		text = utils.getLocator("settingsPage.hearbeatLogVisibility").getText();
		return text;
	}

	public void clickAddNext() {
		utils.getLocator("settingsPage.addNextBtn").click();
		TestListeners.extentTest.get().info("clicked on Add Next button");
	}

	public void editComponentCode(String str, String val) {
		String xpath = utils.getLocatorValue("settingsPage.componentCode").replace("$flag_codeIndex", val);

		List<WebElement> wEleList = driver.findElements(By.xpath(xpath));
		for (WebElement wele : wEleList) {

			try {
				wele.sendKeys(str);
				break;
			} catch (Exception e) {
				// TODO: handle exception
			}

		}

		logger.info(val + " value is entered into edit component code textbox ");
		TestListeners.extentTest.get().info(val + " value is entered into edit component code textbox ");
	}

	public void editComponentCodeCancellationReason(String str, String val) {
		String xpath = utils.getLocatorValue("settingsPage.cancellationReason").replace("$flag_reasonIndex", val);
		List<WebElement> wEleList = driver.findElements(By.xpath(xpath));
		for (WebElement wele : wEleList) {

			try {
				wele.sendKeys(str);
				break;
			} catch (Exception e) {
				// TODO: handle exception
			}

		}

		logger.info(val + " value is entered into edit component code textbox ");
		TestListeners.extentTest.get().info(val + " value is entered into edit component code textbox ");
	}

	public void clickUpdateBtn() {
		utils.scrollToElement(driver, utils.getLocator("settingsPage.updateBtn"));
		utils.waitTillElementToBeClickable(utils.getLocator("settingsPage.updateBtn"));
		utils.getLocator("settingsPage.updateBtn").click();
		TestListeners.extentTest.get().info("click the update button");
	}

	public String getErrorMessage() {
		selUtils.implicitWait(60);
		// utils.waitTillCompletePageLoad();
		utils.waitTillVisibilityOfElement(utils.getLocator("locationPage.getSuccessErrorMessage"), "");
		String messge = "";
		messge = utils.getLocator("locationPage.getSuccessErrorMessage").getText();
		return messge;
	}

	public List<String> getComponentCodeRowNumber() {
		List<String> lst = new ArrayList<String>();
		List<WebElement> ele = utils.getLocatorList("settingsPage.componentCodeRowNo");
		for (int i = 0; i < ele.size(); i++) {
			String val = ele.get(i).getAttribute("data-row-number");
			lst.add(val);
		}
		return lst;
	}

	public void clickMemberLevel(String membershipLevelName) {
		membershipLevelName = membershipLevelName.trim();
		String xpath = utils.getLocatorValue("settingsPage.clickmembershipLevelByName").replace("$MembershipLevelName",
				membershipLevelName);
		utils.waitTillElementToBeClickable(driver.findElement(By.xpath(xpath)));
		driver.findElement(By.xpath(xpath)).click();
		// utils.waitTillPagePaceDone();
		logger.info("clicked on membership level " + membershipLevelName);
		TestListeners.extentTest.get().info("clicked on membership level " + membershipLevelName);
	}

	public void clickConversionRule(String ruleName) {
		String xpath = utils.getLocatorValue("settingsPage.conversionRule").replace("${ruleName}", ruleName);
		utils.waitTillElementToBeClickable(driver.findElement(By.xpath(xpath)));
		driver.findElement(By.xpath(xpath)).click();
		logger.info("clicked on the rule--" + ruleName);
		TestListeners.extentTest.get().info("clicked on the rule--" + ruleName);
	}

	public void clickSaveBtn() {
		utils.clickByJSExecutor(driver, utils.getLocator("settingsPage.saveButton"));
		// utils.waitTillPagePaceDone();
		logger.info("clicked on the save Btn");
		TestListeners.extentTest.get().info("clicked on the save Btn");
	}

	public List<String> tagsOnMembershipPage(String messageTag, String value) {
		utils.refreshPage();
		utils.waitTillPagePaceDone();
		List<String> eleList = new ArrayList<>();
		int counter = 0;
		do {
			utils.refreshPage();
			utils.waitTillPagePaceDone();
			String xpath = utils.getLocatorValue("settingsPage.tagsOnMembershipPage").replace("${messageTag}",
					messageTag);
			List<WebElement> WEleList = driver.findElements(By.xpath(xpath));

			for (WebElement ele : WEleList) {
				String txt = ele.getText();
				eleList.add(txt);
			}

			logger.info("added the display value in the list");
			TestListeners.extentTest.get().info("added the display value in the list");

			if (utils.valuePresentInStringList(eleList, value)) {
				logger.info("found the value in the UI -" + value);
				TestListeners.extentTest.get().info("found the value in the UI -" + value);
				break;
			}
			counter++;
			utils.refreshPage();
			utils.waitTillPagePaceDone();
			utils.longwait(1000);
		} while (counter < 30);
		return eleList;
	}

	public List<String> tagsOnMembershipPageNotVisible(String messageTag, String value) {
		utils.refreshPage();
		utils.waitTillPagePaceDone();
		List<String> eleList = new ArrayList<>();
		int counter = 0;
		do {
			utils.refreshPage();
			// utils.waitTillPagePaceDone();
			String xpath = utils.getLocatorValue("settingsPage.tagsOnMembershipPage").replace("${messageTag}",
					messageTag);
			List<WebElement> WEleList = driver.findElements(By.xpath(xpath));
			for (WebElement ele : WEleList) {
				String txt = ele.getText();
				eleList.add(txt);
			}
			logger.info("added the display value in the list");
			TestListeners.extentTest.get().info("added the display value in the list");

			// check tag not present
			if (!utils.valuePresentInStringList(eleList, value)) {
				logger.info("as expected " + value + " is not present");
				TestListeners.extentTest.get().info("as expected " + value + " is not present");
				break;
			}
			counter++;
			utils.longwait(1000);
		} while (counter < 20);
		return eleList;
	}

	public void clickOnEditLoyaltyPassApplePass() {
		utils.getLocator("settingsPage.applePassEditButton").click();
		utils.waitTillPagePaceDone();
		logger.info("Clicked on edit Apple pass button");
		TestListeners.extentTest.get().info("Clicked on edit Apple pass button");

	}

	public void clickOnApplePassCheckBox(String labelName, String toBeOnOff) {
		String checkBoxXpath = utils.getLocatorValue("settingsPage.applePassCheckBox").replace("${labelName}",
				labelName);
		WebElement labelCheckBox_WebEle = driver.findElement(By.xpath(checkBoxXpath));
		// utils.scrollToElement(driver, labelCheckBox_WebEle);
		String checkBoxTextValue = labelCheckBox_WebEle.getText();

		if ((checkBoxTextValue.equalsIgnoreCase("check_box_outline_blank")) && (toBeOnOff.equalsIgnoreCase("OFF"))) {
			logger.info(labelName + " box Unchecked and do not cliked it as checkBoxFlag= " + toBeOnOff);
			TestListeners.extentTest.get()
					.info(labelName + " box Unchecked and do not cliked it as checkBoxFlag= " + toBeOnOff);
		} else if ((checkBoxTextValue.equalsIgnoreCase("check_box_outline_blank"))
				&& (toBeOnOff.equalsIgnoreCase("ON"))) {
			utils.clickByJSExecutor(driver, labelCheckBox_WebEle);
			logger.info(labelName + " box is unchecked and user want to check the chekedbox");
			TestListeners.extentTest.get().info(labelName + " box is unchecked and user want to check the chekedbox");

		} else if ((checkBoxTextValue.equalsIgnoreCase("check_box")) && (toBeOnOff.equalsIgnoreCase("OFF"))) {
			utils.clickByJSExecutor(driver, labelCheckBox_WebEle);

			logger.info(labelName + " box is already cheked and user want to uncheck ");
			TestListeners.extentTest.get().info(labelName + " box is already cheked and user want to uncheck ");
		} else if ((checkBoxTextValue.equalsIgnoreCase("check_box")) && (toBeOnOff.equalsIgnoreCase("ON"))) {

			logger.info(labelName + " box is already checked and user want to check the chekedbox, so do not click");
			TestListeners.extentTest.get()
					.info(labelName + " box is already checked and user want to check the chekedbox, so do not click");

		}
	}

	public void clickOnApplePassTab(String tabName) throws InterruptedException {
		selUtils.longWait(2000);
		String xPath = utils.getLocatorValue("settingsPage.applePassTabsXpath").replace("${tabName}", tabName);
		WebElement wEle = driver.findElement(By.xpath(xPath));
		wEle.click();
		logger.info(tabName + " tab is clicked ");
		TestListeners.extentTest.get().info(tabName + " tab is clicked ");
	}

	public void clickOnApplePassSaveButton() throws InterruptedException {
		utils.getLocator("settingsPage.appleSaveButton").click();
		Thread.sleep(5000);
		// utils.waitTillVisibilityOfElement(utils.getLocator("settingsPage.passesHeading"),
		// "Passes Heading");
		// utils.waitTillCompletePageLoad();
		// utils.waitTillElementToBeVisible(utils.getLocator("settingsPage.applePassEditButton"));
		logger.info("Passes settings are saved successfully");
		TestListeners.extentTest.get().info("Passes settings are saved successfully");
	}

	public void editRedeemptionMarkFromMembership(String points) {
		utils.waitTillPagePaceDone();
		WebElement webEle = utils.getLocator("settingsPage.editRedeemptionMark");
///		utils.waitTillElementToBeClickable(webEle);
		utils.waitTillVisibilityOfElement(webEle, "editRedeemptionMark");
		utils.longWaitInSeconds(4);
//		webEle.click();
		webEle.clear();
		webEle.sendKeys(points);

		logger.info("in the redeemption mark field enterd the value " + points);
		TestListeners.extentTest.get().info("in the redeemption mark field enterd the value " + points);
	}

	public void editMembershipLevelMinMaxPoints(String minPoints, String maxPoints) {
		utils.waitTillPagePaceDone();
		WebElement minPointsInput = utils.getLocator("settingsPage.membershipLevelMinPoints");
		utils.waitTillVisibilityOfElement(minPointsInput, "Membership level min points");
		minPointsInput.clear();
		minPointsInput.sendKeys(minPoints);
		logger.info("Membership level Minimum points are set to " + minPoints);
		TestListeners.extentTest.get().info("Membership level Minimum points are set to " + minPoints);

		WebElement maxPointsInput = utils.getLocator("settingsPage.membershipLevelMaxPoints");
		utils.waitTillVisibilityOfElement(maxPointsInput, "Membership level max points");
		maxPointsInput.clear();
		maxPointsInput.sendKeys(maxPoints);
		logger.info("Membership level Maximum points are set to " + maxPoints);
		TestListeners.extentTest.get().info("Membership level Maximum points are set to " + maxPoints);
	}

	public void clickUpdateMembership() {
		utils.waitTillPagePaceDone();
		WebElement webEle = utils.getLocator("settingsPage.updateMembership");
///		utils.waitTillElementToBeClickable(webEle);
		utils.waitTillVisibilityOfElement(webEle, "updateMembership");
		webEle.click();
		utils.waitTillPagePaceDone();
		logger.info("clicked on the update membership button");
		TestListeners.extentTest.get().info("clicked on the update membership button");
	}

	public boolean verifyNotificationTemplateIsDisplaying(String notificationTemplateName) {
		if (!driver.findElements(By.linkText(notificationTemplateName)).isEmpty()) {
			return true;
		} else {
			return false;
		}
	}

	public void selectConversionCriteriaInConversionRules(String choice, String strategy) {
		switch (choice) {
		case "Set":
			utils.getLocator("settingsPage.clickConversionCriteriaDrpDown").click();
			List<WebElement> ele = utils.getLocatorList("settingsPage.ConversionCriteriaList");
			utils.selectListDrpDwnValue(ele, strategy);
			logger.info("Select Conversion Criteria In Conversion Rules is selected as :- " + strategy);
			TestListeners.extentTest.get()
					.info("Select Conversion Criteria In Conversion Rules is selected as :- " + strategy);
			break;

		case "Clear":
			utils.getLocator("settingsPage.clearConversionCriteria").click();
			logger.info("Clear Conversion Criteria In Conversion Rules ");
			TestListeners.extentTest.get().info("Clear Conversion Criteria In Conversion Rules ");
			break;
		}
	}

	public void liftReport(String reportHeading) {
		utils.waitTillPagePaceDone();
		String heading = utils.getLocator("settingsPage.liftReportVisibility").getText();
		Assert.assertEquals(heading, reportHeading, "Lift Report heading is not matching");
		logger.info("Lift Report heading is matching");
		TestListeners.extentTest.get().pass("Lift Report heading is matching");
	}

	public void chooseExpiryStrategy(String expiry_strategy) {
		utils.getLocator("settingsPage.expiryStrategy").click();
		List<WebElement> webList = utils.getLocatorList("settingsPage.expiryStrategyDropdown");
		for (WebElement wEle : webList) {
			try {
				utils.selectDrpDwnValueNew(wEle, expiry_strategy);
			} catch (Exception e) {
			}
		}

	}

	public void alternateLanguages() {
		try {

			utils.getLocator("settingsPage.frenchSelected").isDisplayed();
			logger.info("french is already selected");
			TestListeners.extentTest.get().info("french is already selected");
		} catch (Exception e) {
			utils.getLocator("settingsPage.alernateLanguageTab").click();
			utils.getLocator("settingsPage.buisnessProfileAlternatelanguages").click();
			logger.info("french language is selected");
			TestListeners.extentTest.get().info("french language is selected");
		}
	}

	public void clearAllAlternateLanguages() {
		while (true) {
			List<WebElement> altenatelanguages = utils.getLocatorList("settingsPage.existingalternatelanguages");

			if (altenatelanguages.isEmpty()) {
				break;
			}
			// Always click the last item
			WebElement lastLanguage = altenatelanguages.get(altenatelanguages.size() - 1);
			lastLanguage.click();
			utils.getLocator("settingsPage.alernateLanguageTab").click();
		}
		utils.StaleElementclick(driver, utils.getLocator("settingsPage.saveBtn"));
		logger.info("all alternate language is cleared");
		TestListeners.extentTest.get().info("all alternate language is cleared");
	}

	public void setExpiryDays(String expiryDays) {
		utils.getLocator("settingsPage.conversionRuleExpiryDays").clear();
		utils.getLocator("settingsPage.conversionRuleExpiryDays").sendKeys(expiryDays);

	}

	public void clickonPOSScoreBoardData() {
		driver.switchTo().frame(driver.findElement(By.xpath("//iframe")));
		utils.longWaitInSeconds(15);
		utils.waitTillElementToBeClickable(utils.getLocator("settingsPage.posStatsTableRowsXpath"));
		int wEleListSize = utils.getLocatorList("settingsPage.posStatsTableRowsXpath").size();
		int index = wEleListSize - 1;

		WebElement posScoreboardButtonWele = driver
				.findElement(By.xpath("//div[@id='dashboard-viewport']/div[@id='dashboard-spacer']/div/div[" + index
						+ "]//div[@class='tvimagesContainer']/img"));
		utils.waitTillElementToBeClickable(posScoreboardButtonWele);
		posScoreboardButtonWele.click();
		driver.switchTo().defaultContent();
		utils.longWaitInSeconds(3);

		utils.switchToWindow();
		driver.close();
		utils.switchToParentWindow();

		logger.info("Clicked on POS Scoreboard button ");
		TestListeners.extentTest.get().info("Clicked on POS Scoreboard button ");
	}

	public boolean verifyPosScoreBoardData() {
		driver.switchTo().frame(driver.findElement(By.xpath("//iframe")));
		List<WebElement> dataList = utils.getLocatorList("settingsPage.posScoreBoardListing");
		if (dataList.size() > 0) {
			driver.switchTo().defaultContent();
			return true;
		} else {
			driver.switchTo().defaultContent();
			return false;
		}
	}

	public void setPushNotificationInMembership(String str) {
		utils.scrollToElement(driver, utils.getLocator("settingsPage.pushNotificationInMembership"));
		utils.getLocator("settingsPage.pushNotificationInMembership").clear();
		utils.getLocator("settingsPage.pushNotificationInMembership").sendKeys(str);
		logger.info("in the membership add the push notification -- " + str);
		TestListeners.extentTest.get().info("in the membership add the push notification -- " + str);
	}

	public void setEmailInMembership(String str) {
		utils.scrollToElement(driver, utils.getLocator("settingsPage.emailSubjectMembership"));
		utils.getLocator("settingsPage.emailSubjectMembership").clear();
		utils.getLocator("settingsPage.emailSubjectMembership").sendKeys(str);
		utils.getLocator("settingsPage.emailTemplateMembership").clear();
		utils.getLocator("settingsPage.emailTemplateMembership").sendKeys(str);
		utils.getLocator("settingsPage.preheaderMembership").clear();
		utils.getLocator("settingsPage.preheaderMembership").sendKeys(str);
		logger.info("in the membership add email subject, email template and preheader text -- " + str);
		TestListeners.extentTest.get()
				.info("in the membership add email subject, email template and preheader text -- " + str);
	}

	public void removeAlternateLanguages() {
		List<WebElement> languagesList = utils.getLocatorList("settingsPage.availableLanguage");
		if (languagesList.size() <= 1) {
			logger.info("No Alternate Language is selected");
			TestListeners.extentTest.get().info("No Alternate Language is selected");
			return;
		} else {
			for (int i = languagesList.size() - 1; i >= 1; i--) {
//				String language = languagesList.get(i).getText();
				String j = Integer.toString(i);
				String removeLanguage = utils.getLocatorValue("settingsPage.removeAvailableLanguage").replace("$LineNo",
						j);
				driver.findElement(By.xpath(removeLanguage)).click();
				logger.info(" Alternate Language is removed");
				TestListeners.extentTest.get().info(" Alternate Language is removed");
				if (i == 1) {
					continue;
				} else {
					utils.getLocator("settingsPage.clickOnLanguageTab").click();
				}
			}
		}
	}

	// eg. Data available for last one year not counting today. Data on this page
	// was last refreshed on 15/07/2024 12:41:10 UTC.
	public String verifyPosScoreBoardDataTableMessage() {
		driver.switchTo().frame(driver.findElement(By.xpath("//iframe")));
		String actualMessage = utils.getLocator("settingsPage.dataTableMessageXpath").getText();
		driver.switchTo().defaultContent();
		return actualMessage;

	}

	public void setBankedRedeemable(String bankedRedeemable) throws InterruptedException {

		WebElement webEle = driver.findElement(By.xpath(utils.getLocatorValue("settingsPage.bankedRedeemable")));
		utils.selectDrpDwnValue(webEle, bankedRedeemable);

//		WebElement webEle = utils.getLocator("settingsPage.selectBankedRedeemable");
////		utils.waitTillElementToBeClickable(webEle);
//		utils.waitTillVisibilityOfElement(webEle, "Banked Redeemable Box");
//		webEle.click();
//		selUtils.longWait(2000);
//		String xpath = utils.getLocatorValue("settingsPage.bankedRedeemableList").replace("$value@",
//				bankedRedeemable);
//		driver.findElement(By.xpath(xpath)).click();
		logger.info("Selected banked redeemable is " + bankedRedeemable);
		TestListeners.extentTest.get().info("Selected banked redeemable is " + bankedRedeemable);
	}

	public void setRewardValueInMembershipLevel(String value) throws InterruptedException {
		WebElement webEle = utils.getLocator("settingsPage.clickRewardValueBox");
///		utils.waitTillElementToBeClickable(webEle);
		utils.waitTillVisibilityOfElement(webEle, "Reward value Box");
		webEle.click();
		webEle.clear();
		webEle.sendKeys(value);
		selUtils.longWait(2000);
		logger.info("Entered Reward value as : $" + value);
		TestListeners.extentTest.get().info("Entered Reward value as : $" + value);
	}

	public void clearBankedRedeemable() {
		try {
			utils.waitTillPagePaceDone();
			if (utils.getLocator("settingsPage.clearBankedRedeemable").isDisplayed()) {
				utils.longWaitInSeconds(2);
//			utils.clickByJSExecutor(driver, utils.getLocator("settingsPage.clearBankedRedeemable"));
				utils.getLocator("settingsPage.clearBankedRedeemable").click();
				WebElement webEle = utils.getLocator("settingsPage.selectBankedRedeemable");
				webEle.click();
				logger.info("Banked redeemable is cleared from Membership");
				TestListeners.extentTest.get().info("Banked redeemable is cleared from Membership");
			}
		} catch (Exception e) {
			logger.info("Banked redeemable is already cleared from Membership");
			TestListeners.extentTest.get().info("Banked redeemable is already cleared from Membership");
		}
	}

	public void setMembershipLevelAttainmentReward(String redeemable) throws InterruptedException {
		WebElement webEle = driver
				.findElement(By.xpath(utils.getLocatorValue("settingsPage.membershipAttainmentReward")));
		utils.selectDrpDwnValue(webEle, redeemable);
		logger.info("Selected Membership Level Attainment Reward as " + redeemable);
		TestListeners.extentTest.get().info("Selected Membership Level Attainment Reward as " + redeemable);
	}

	public void selectAlternateLanguage(String langName) {
		WebElement wEleSelete = utils.getLocator("settingsPage.selectAlternateLanguageXpath");
		utils.selectDrpDwnValue(wEleSelete, langName);
		// click saveBtn
		utils.getLocator("settingsPage.saveButton").click();
		logger.info(langName + " alternate language is selected");
		TestListeners.extentTest.get().info(langName + " alternate language is selected");

	}

	// It edits any input field
	public String editInputField(String locator, String text, String fieldName) {
		String xpath = utils.getLocatorValue(locator).replace("temp", fieldName);
		WebElement element = driver.findElement(By.xpath(xpath));
		utils.waitTillVisibilityOfElement(element, fieldName);
		utils.scrollToElement(driver, element);
		element.clear();
		element.sendKeys(text);
		TestListeners.extentTest.get().info(fieldName + " is edited with text: " + text);
		logger.info(fieldName + " is edited with text: " + text);
		return element.getText();
	}

	public void clickOnDesiredPassButton(String passType, String buttonType) {
		String buttonXpath = utils.getLocatorValue("settingsPage.desiredPassButton").replace("$passType", passType)
				.replace("$buttonType", buttonType);
		driver.findElement(By.xpath(buttonXpath)).click();
		utils.waitTillPagePaceDone();
		logger.info("Clicked on desired pass button");
		TestListeners.extentTest.get().info("Clicked on desired pass button");
	}

	public void setUserIdentifierForPass(String userIdentifier) {
		WebElement webEle = utils.getLocator("settingsPage.userIdentifierExpand");
		((JavascriptExecutor) driver).executeScript("arguments[0].click();", webEle);
		List<WebElement> webEleList = utils.getLocatorList("settingsPage.userIdentifierValues");
		utils.selecDrpDwnValue(webEleList, userIdentifier);
	}

	public boolean verifyFiledAvailableOrNot(String fieldName) {
		boolean flag = false;
		try {
			String xpath = utils.getLocatorValue("settingsPage.fieldName").replace("$fieldName", fieldName);
			WebElement element = driver.findElement(By.xpath(xpath));
			element.isDisplayed();
			flag = true;
		} catch (Exception e) {
			flag = false;
		}
		return flag;
	}

	public void setConversionRuleValues(String fieldName, String value) {
		String xpath = utils.getLocatorValue("settingsPage.conversionRuleInputField").replace("$temp", fieldName);
		WebElement ele = driver.findElement(By.xpath(xpath));
		ele.clear();
		ele.sendKeys(value);
		logger.info("Conversion Rule " + fieldName + " value is set as " + value);
		TestListeners.extentTest.get().info("Conversion Rule " + fieldName + " value is set as " + value);
	}

	public void enterSmartPassRewardLimit(String string) {
		String xPath = utils.getLocatorValue("settingsPage.availableRewardLimitInputField");
		WebElement wEle = driver.findElement(By.xpath(xPath));
		wEle.clear();
		wEle.sendKeys(string);
		logger.info("Entered Smart Pass reward limit as " + string);
		TestListeners.extentTest.get().info("Entered Smart Pass reward limit as " + string);
		selUtils.longWait(2000);
	}

	public boolean isErrorMsgDisplayedForRewardLimit() {
		boolean flag = false;
		try {
			utils.waitTillVisibilityOfElement(utils.getLocator("dashboardPage.rewardLimitErrorMessage"),
					"Reward limit error message");
			flag = true;
			logger.info("Error message is appear as expected");
			TestListeners.extentTest.get().info("Error message is appear as expected");
		} catch (Exception e) {
			flag = false;
			logger.info("Error message is not appear");
			TestListeners.extentTest.get().info("Error message is not appear");
		}
		return flag;

	}

	public String getErrorMsg2DisplayedForRewardLimit() {
		String errorMsg = "";
		try {
			utils.waitTillVisibilityOfElement(utils.getLocator("dashboardPage.errorMsgDisplayedForRewardLimit"),
					"Reward limit error message");
			errorMsg = utils.getLocator("dashboardPage.errorMsgDisplayedForRewardLimit").getText();
			logger.info("Error message is appear as expected and the message is : " + errorMsg);
			TestListeners.extentTest.get().info("Error message is appear as expected and the message is : " + errorMsg);
		} catch (Exception e) {
			logger.info("Error message is not appear");
			TestListeners.extentTest.get().info("Error message is not appear");
		}
		return errorMsg;

	}
}
