package com.punchh.server.pages;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.Color;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

public class NotificationTemplatePage {

	static Logger logger = LogManager.getLogger(NotificationTemplatePage.class);
	private WebDriver driver;
	Utilities utils;
	SeleniumUtilities selUtils;
	private PageObj pageObj;

	public NotificationTemplatePage(WebDriver driver) {
		this.driver = driver;
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		pageObj = new PageObj(driver);
	}

	public void setPOSScannerCheckinOn() {
		WebElement ele = utils.getLocator("NotificationTemplatePage.posScannerCheckinSlider");
		String color = ele.getCssValue("color");
		String hexcolor = Color.fromString(color).asHex();
		if (hexcolor.equals("#37c936")) {
			logger.info("pos Scanner Checkin Slider is already on");
			TestListeners.extentTest.get().info("pos Scanner Checkin Slider is already on :" + hexcolor);
		} else {
			utils.waitTillElementToBeClickable(ele);
			utils.StaleElementclick(driver, ele);
			utils.acceptAlert(driver);
		}
	}

	public void setSystemMessageOptionSliderOnOff(String value, String onOff) {
		String xpath = utils.getLocatorValue("NotificationTemplatePage.systemMessageOptionSlider").replace("$name",
				value);
		WebElement ele = utils.getXpathWebElements(By.xpath(xpath));
		String color = ele.getCssValue("color");
		String hexcolor = Color.fromString(color).asHex();
		switch (onOff) {
		case "enable":
			if (hexcolor.equals("#37c936")) {
				logger.info(value + " Slider is already on");
				TestListeners.extentTest.get().info(value + " Slider is already on: " + hexcolor);
			} else {
				utils.waitTillElementToBeClickable(ele);
				utils.StaleElementclick(driver, ele);
				utils.acceptAlert(driver);
				logger.info(value + " Slider is turned on");
				TestListeners.extentTest.get().info(value + " Slider is turned on: " + hexcolor);
			}
			break;
		case "disable":
			if (!hexcolor.equals("#37c936")) {
				logger.info(value + " Slider is already off");
				TestListeners.extentTest.get().info(value + " Slider is already off: " + hexcolor);
			} else {
				utils.waitTillElementToBeClickable(ele);
				utils.StaleElementclick(driver, ele);
				utils.acceptAlert(driver);
				logger.info(value + " Slider is turned off");
				TestListeners.extentTest.get().info(value + " Slider is turned off: " + hexcolor);

			}
		}
	}

	/**
	 * Toggles the state of a template based on the specified action.
	 * 
	 * @param templateName The name of the template to be enabled or disabled.
	 * @param action       Pass "enable" to enable the template, "disable" to
	 *                     disable it.
	 */
	public void toggleNotificationTemplate(String templateName, String action) {
		boolean enable = "enable".equalsIgnoreCase(action);
		String actionText = enable ? "Enabling" : "Disabling";
		String status = enable ? "enabled" : "disabled";

		logger.info(actionText + " template: " + templateName);

		String xpathKey = enable ? "NotificationTemplatePage.enableTemplateBtn"
				: "NotificationTemplatePage.disableTemplateBtn";
		String xpath = utils.getLocatorValue(xpathKey).replace("$templateName", templateName);

		WebElement actionButton = null;

		try {
			actionButton = driver.findElement(By.xpath(xpath));
		} catch (Exception e) {
			logger.warn("Template " + templateName + " is already " + status + " or can't be " + status + ".");
			TestListeners.extentTest.get()
					.warning("Template " + templateName + " is already " + status + " or can't be " + status + ".");
			return;
		}

		utils.waitTillElementToBeClickable(actionButton);
		actionButton.click();
		utils.longWaitInSeconds(1);
		utils.acceptAlert(driver);
		utils.longWaitInSeconds(1);
		logger.info("Template " + templateName + " has been " + status);
		TestListeners.extentTest.get().info("Template " + templateName + " has been " + status);
	}

	public void selectNotifficationPageGivenTab(String enterTheTabName) {
		String notificationPageTabNameXpath = utils.getLocatorValue("NotificationTemplatePage.tabName");
		notificationPageTabNameXpath = notificationPageTabNameXpath.replace("${tabname}", enterTheTabName);
		WebElement tab = driver.findElement(By.xpath(notificationPageTabNameXpath));
		utils.clickByJSExecutor(driver, tab);
	}

	public String toastMessages() {
		utils.waitTillPagePaceDone();
		return utils.getLocator("NotificationTemplatePage.toastMessage").getText();
	}

	public void selectSystemMessageFor(String enterTheSystemMessage) {
		String systemMessageXpath = utils.getLocatorValue("NotificationTemplatePage.userSignupNotificationTemplate");
		systemMessageXpath = systemMessageXpath.replace("${systemMessages}", enterTheSystemMessage);
		WebElement link = driver.findElement(By.xpath(systemMessageXpath));
		link.click();
	}

	public void selectEmailtemplateAndAttach(String templateName) {
		// Scroll to email template attachment section
		WebElement emailDesignLabel = utils.getLocator("NotificationTemplatePage.emailDesignLabel");
		utils.scrollToElement(driver, emailDesignLabel);
		utils.waitTillElementDisappear("NotificationTemplatePage.loadingSpinner", 3);
		utils.longWaitInSeconds(2); // waiting for template section to load properly
		// Check whether template is already attached
		boolean isSearchFieldPresent = isPresent("NotificationTemplatePage.emailTempSearchField");
		// If search field is absent, then email is already attached
		if (!isSearchFieldPresent) {
			utils.logit("Search field not present, email template already attached.");
			// Remove the attached template first using remove button
			WebElement templateCard = utils.getLocator("NotificationTemplatePage.emailTemplateCard");
			WebElement removeButton = utils.getLocator("NotificationTemplatePage.removeTheNotificationTemplate");
			selUtils.mouseHoverAndClickBYMouseAction(driver, templateCard, removeButton);
			utils.logit("Clicked on Remove button of the email template");
			WebElement confirmRemoveButton = utils
					.getLocator("NotificationTemplatePage.emailTempRemoveConfirmationButton");
			utils.clickByJSExecutor(driver, confirmRemoveButton);
			utils.logit("Confirmed the remove action.");
		}
		utils.logit("Search field present, going to search and attach email template.");
		searchAndSelectTheEmailTemplate(templateName);
		WebElement saveAndCloseButton = utils.getLocator("NotificationTemplatePage.saveAndCloseButtn");
		utils.longWaitInSeconds(5);
		utils.waitTillElementToBeClickable(saveAndCloseButton);
		utils.scrollToElement(driver, saveAndCloseButton);
		//utils.clickWithActions(saveAndCloseButton);
		utils.clickByJSExecutor(driver, saveAndCloseButton);
		utils.logit("Clicked on SAVE & CLOSE button.");
		utils.longWaitInSeconds(3); // wait for square-shaped spinner to disappear
		utils.waitTillElementDisappear("NotificationTemplatePage.loadingSpinner", 20);
	}

	// Checks if an element is present or not using locator path
	public boolean isPresent(String locator) {
		boolean status = false;
		try {
			utils.implicitWait(1);
			String xpath = utils.getLocatorValue(locator);
			WebElement element = driver.findElement(By.xpath(xpath));
			if (element.isDisplayed()) {
				status = true;
				utils.logit("Element: " + element + " is present.");
			}
		} catch (NoSuchElementException e) {
			status = false;
			utils.logit("Exception occurred: " + e.getMessage());
		} finally {
            utils.implicitWait(50);
        }
		return status;
	}

	public void clickOnSaveButton() {
		utils.waitTillPagePaceDone();
		WebElement saveBtn = utils.getLocator("NotificationTemplatePage.saveButton");
		utils.waitTillElementToBeClickable(saveBtn);
		utils.StaleElementclick(driver, saveBtn);
		logger.info("Clicked on template save button and saved");
		TestListeners.extentTest.get().info("Clicked on template save button and saved successfully");
	}

	public void setTextEmailTemplate() {
		WebElement textEmailTemplateBox = utils.getLocator("NotificationTemplatePage.textEmailTemplateBox");
		textEmailTemplateBox.clear();
		textEmailTemplateBox.sendKeys("Test email template");
	}

	public void verifyTheTestNotificationMessageOnEmail(String userEmailId) {
		WebElement userEmailField = utils.getLocator("NotificationTemplatePage.testUserEmailField");
		userEmailField.click();
		userEmailField.clear();
		userEmailField.sendKeys(userEmailId);
		WebElement testNotificationButton = utils.getLocator("NotificationTemplatePage.sendUserNotificationButton");
		utils.clickByJSExecutor(driver, testNotificationButton);
		logger.info("Test notification message sent on email id");
		TestListeners.extentTest.get().info("Test notification message sent on email on user email id");
	}

	public void searchAndSelectTheEmailTemplate(String templateName) {
		utils.waitTillPagePaceDone();
		WebElement searchField = utils.getLocator("NotificationTemplatePage.emailTempSearchField");
		searchField.clear();
		searchField.sendKeys(templateName);
		searchField.sendKeys(Keys.ENTER);
		utils.logit("Template searched successfully");
		WebElement emailtempCard = utils.getLocator("NotificationTemplatePage.emailTemplateCard");
		// utils.waitTillInVisibilityOfElement(emailtempCard, "EmailTemplateCard");
		WebElement selectButtonOnTemp = utils.getLocator("NotificationTemplatePage.selectButton");
		// utils.waitTillInVisibilityOfElement(selectButtonOnTemp, "SelectButtonOnEmailTemp");
		selUtils.mouseHoverAndClickBYMouseAction(driver, emailtempCard, selectButtonOnTemp);
		utils.waitTillElementDisappear("NotificationTemplatePage.loadingSpinner", 5);
		utils.logit("Searched email template is selected to attach.");
	}

	public String verifyTheAttachedTempInUserTimeline(String userEmailId) {
		WebElement searchButton = utils.getLocator("instanceDashboardPage.searchGuest");
		utils.clickByJSExecutor(driver, searchButton);
		logger.info("Clicked on searched guest");
		WebElement guestSearchfield = utils.getLocator("NotificationTemplatePage.searchGuestInputField");
		guestSearchfield.clear();
		guestSearchfield.sendKeys(userEmailId);
		logger.info("user email sent to the user searched field");
		WebElement searchedGuest = utils.getLocator("NotificationTemplatePage.searchedGuestByEmailId");
		searchedGuest.click();
		TestListeners.extentTest.get().info("Entered into the user timeline");

		WebElement timelineDetails = utils.getLocator("NotificationTemplatePage.userTimelineOffer");
		utils.waitTillInVisibilityOfElement(timelineDetails, "Rewards");
		String timeLineInfo = timelineDetails.getText();
		return timeLineInfo;
	}

	public void waitTillEmailSubjectVisible(String choice) {
		switch (choice) {
		case "Notification Template":
			utils.waitTillElementToBeClickable(
					utils.getLocator("NotificationTemplatePage.notificationTemplateEmailSubject"));
			logger.info("Notification Template Email Subject is Visible");
			TestListeners.extentTest.get().info("Notification Template Email Subject is Visible");
			break;

		case "Membership Level":
			utils.waitTillElementToBeClickable(utils.getLocator("NotificationTemplatePage.memberEmailSubject"));
			logger.info("Membership Level Email Subject is Visible");
			TestListeners.extentTest.get().info("Membership Level Email Subject is Visible");
			break;

		case "Signup Campaign":
			utils.waitTillElementToBeClickable(utils.getLocator("NotificationTemplatePage.signupCampaignEmailSubject"));
			logger.info("Signup Campaign Email Subject is Visible");
			TestListeners.extentTest.get().info("Signup Campaign Email Subject is Visible");
			break;

		case "Mass Notification Campaign":
			utils.waitTillElementToBeClickable(
					utils.getLocator("NotificationTemplatePage.massNotificationCampaignEmailSubject"));
			logger.info("Mass Notification Campaign Email Subject is Visible");
			TestListeners.extentTest.get().info("Mass Notification Campaign Email Subject is Visible");
			break;

		case "Post Checkin Notification Campaign":
			utils.waitTillElementToBeClickable(
					utils.getLocator("NotificationTemplatePage.postCheckinNotificationCampaignEmailSubject"));
			logger.info("Post Checkin Notification Campaign Email Subject is Visible");
			TestListeners.extentTest.get().info("Post Checkin Notification Campaign Email Subject is Visible");
			break;

		case "Checkin Survey":
			utils.waitTillElementToBeClickable(utils.getLocator("NotificationTemplatePage.checkinSurveyEmailSubject"));
			logger.info("Checkin Survey Email Subject is Visible");
			TestListeners.extentTest.get().info("Checkin Survey Email Subject is Visible");
			break;
		}

	}
}
