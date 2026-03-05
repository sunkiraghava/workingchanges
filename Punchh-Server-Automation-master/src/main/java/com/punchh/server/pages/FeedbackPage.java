package com.punchh.server.pages;

import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Listeners;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class FeedbackPage {

	static Logger logger = LogManager.getLogger(GiftCardsPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;

	public FeedbackPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
	}

	public boolean feedbackMenuPresent() {
		selUtils.implicitWait(5);
		boolean value = false;
		try {
			String xpath = utils.getLocatorValue("menuPage.feedbackMenu");
			WebElement ele = driver.findElement(By.xpath(xpath));
			return ele.isDisplayed();
		} catch (Exception e) {
			return value;
		}
	}

	public String getPageSource() {
		String data = driver.getPageSource();
		return data;
	}

	public void searchFeedback(String str) {
		// utils.refreshPage();
		selUtils.longWait(3000);
		utils.getLocator("feedbackPage.searchFeedback").clear();
		utils.getLocator("feedbackPage.searchFeedback").sendKeys(str);
		utils.getLocator("feedbackPage.searchFeedback").sendKeys(Keys.ENTER);
		utils.implicitWait(5);
	}

	public boolean isFeedbackWithTextProcessed(String feedbackWithText_id, String sentimentCategories,
			String feedbackText) {
		utils.refreshPage();
		String feedbackText_xpath = utils.getLocatorValue("feedbackPage.feedbackText")
				.replace("$flag", feedbackWithText_id).replace("$text", feedbackText);
		WebElement feedbackText_ele = driver.findElement(By.xpath(feedbackText_xpath));
		utils.scrollToElement(driver, feedbackText_ele);
		String sentiments_actual_xpath = utils.getLocatorValue("feedbackPage.sentimentCategories").replace("$flag",
				feedbackWithText_id);
		String[] sentiments_actual = driver.findElement(By.xpath(sentiments_actual_xpath)).getText().split("\\n");

		// Verifying that all 6 expected sentiment categories are present
		String[] sentiments_expected = sentimentCategories.split(",");
		int count = 0;
		for (String sentiment : sentiments_actual) {
			for (String st : sentiments_expected) {
				if (sentiment.equals(st)) {
					count++;
					break;
				}
			}
		}

		String sentimentType_xpath = utils.getLocatorValue("feedbackPage.sentimentType").replace("$flag",
				feedbackWithText_id);
		WebElement sentimentType_ele = driver.findElement(By.xpath(sentimentType_xpath));

		if (count == sentiments_expected.length) {
			// Verifying that the overall sentiment type is Positive
			if (sentimentType_ele.getText().equalsIgnoreCase("Positive") && feedbackText_ele.isDisplayed()) {
				TestListeners.extentTest.get()
						.info("Feedback with text '" + feedbackText + "' have the overall sentiment type as Positive.");
				logger.info("Feedback with text '" + feedbackText + "' have the overall sentiment type as Positive.");
				return true;
			}
		}
		return false;
	}

	public boolean isFeedbackWithOnlyRatingUnprocessed(String feedbackWithOnlyRating_id) {
		String replyWithChatGPTBtn_xpath = utils.getLocatorValue("feedbackPage.replyWithChatGPtBtn").replace("$flag",
				feedbackWithOnlyRating_id);
		driver.findElement(By.xpath(replyWithChatGPTBtn_xpath)).isDisplayed();
		String sentimentType_xpath = utils.getLocatorValue("feedbackPage.sentimentType").replace("$flag",
				feedbackWithOnlyRating_id);

		try {
			utils.implicitWait(2);
			driver.findElement(By.xpath(sentimentType_xpath));
			TestListeners.extentTest.get().info("Feedback with only rating have the overall sentiment type");
			logger.info("Feedback with only rating have the overall sentiment type");
			return false;
		} catch (NoSuchElementException e) {
			TestListeners.extentTest.get().info("Feedback with only rating does not have the overall sentiment type");
			logger.info("Feedback with only rating does not have the overall sentiment type");
			return true;
		}
	}

	public String getChatGptDisclaimerText(String feedback_id) {
		String disclaimer_actual_xpath = utils.getLocatorValue("feedbackPage.chatGptDisclaimer").replace("$flag",
				feedback_id);
		String disclaimer_actual = driver.findElement(By.xpath(disclaimer_actual_xpath)).getAttribute("data-title");
		return disclaimer_actual;
	}

	public boolean isReplyWithChatGPTButtonVisible(String feedbackId) {
		try {
			utils.implicitWait(2);
			String replyWithChatGPTBtnXpath = utils.getLocatorValue("feedbackPage.replyWithChatGPtBtn").replace("$flag",
					feedbackId);
			driver.findElement(By.xpath(replyWithChatGPTBtnXpath));
			TestListeners.extentTest.get().info("The 'Reply with ChatGPT' button is present.");
			logger.info("The 'Reply with ChatGPT' button is present.");
			return true;
		} catch (NoSuchElementException e) {
			TestListeners.extentTest.get().info("The 'Reply with ChatGPT' button is not present.");
			logger.info("The 'Reply with ChatGPT' button is not present.");
			return false;
		}
	}

	public String getBusinessReplyText(String feedbackId) {
		String businessReplyXpath = utils.getLocatorValue("feedbackPage.businessReply").replace("$flag", feedbackId);
		driver.findElement(By.xpath(businessReplyXpath)).click();
		String text = selUtils.getTextUsingJS(utils.getLocator("feedbackPage.businessReplyText"));
		return text;
	}

	public boolean clickReplyWithChatGPT(String str) {
		String xpath = utils.getLocatorValue("feedbackPage.replyWithChatGPtBtn").replace("$flag", str);
		WebElement ele = driver.findElement(By.xpath(xpath));

		if (ele.isDisplayed()) {
			ele.click();
			TestListeners.extentTest.get().info("reply with chatGPT option is clicked");
			return true;
		} else {
			TestListeners.extentTest.get().info("reply with chatGPT option is not  present");
			return false;
		}

	}

}
