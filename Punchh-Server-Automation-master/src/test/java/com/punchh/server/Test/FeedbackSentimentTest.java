package com.punchh.server.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class FeedbackSentimentTest {
	private static Logger logger = LogManager.getLogger(FeedbackSentimentTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env;
	private String baseUrl;
	private Utilities utils;
	private static Map<String, String> dataSet;
	String run = "ui";

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// single Login to instance
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.instanceDashboardPage().instanceLogin(baseUrl);
	}

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
		// move to All Business Page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}
	
	@Test(description = "SQ-T4671: Validate Feedback Sentiment processing", groups = { "regression",
			"unstable" ,"nonNightly"}, priority = 0)
	@Owner(name = "Vaibhav Agnihotri")
	public void T4671_FeedbackSentimentProcessing() throws Exception {
		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Go to Cockpit > Dashboard, ensure the required flags are turned ON
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick(dataSet.get("flagID1"), "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick(dataSet.get("flagID2"), "check");
		pageObj.dashboardpage().updateCheckBox();
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable chatgpt feedback", "check");

		// Create a user using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		TestListeners.extentTest.get().pass("Api2 user signup is successful");
		logger.info("New user is created");

		// Create a feedback with both text and rating using API call
		String feedbackText = "Good Coffee_" + CreateDateTime.getTimeDateString();
		Response createfeedbackResponse = pageObj.endpoints().api1CreateFeedback("5", feedbackText,
				dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(createfeedbackResponse.getStatusCode(), 200,
				"Status code 200 did not match for API v1 Create Guest feedback");
		String feedbackWithText_id = createfeedbackResponse.jsonPath().getString("id").toString();
		TestListeners.extentTest.get()
				.pass("API v1 Create Guest feedback call with both text and rating is successful with feedback id: "
						+ feedbackWithText_id);
		logger.info("API v1 Create Guest feedback call with both text and rating is successful with feedback id: "
				+ feedbackWithText_id);

		// Create a feedback with only rating using API call
		Response createFeedbackWithOnlyRatingResponse = pageObj.endpoints().api1CreateFeedback("4", "",
				dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(createFeedbackWithOnlyRatingResponse.getStatusCode(), 200,
				"Status code 200 did not match for API v1 Create Guest feedback");
		String feedbackWithOnlyRating_id = createFeedbackWithOnlyRatingResponse.jsonPath().getString("id").toString();
		TestListeners.extentTest.get()
				.pass("API v1 Create Guest feedback call with only rating is successful with feedback id: "
						+ feedbackWithOnlyRating_id);
		logger.info("API v1 Create Guest feedback call with only rating is successful with feedback id: "
				+ feedbackWithOnlyRating_id);

		// Go to Feedback > Feedback. Verify that Feedback with only rating is available
		// but not processed
		pageObj.menupage().navigateToSubMenuItem("Feedback", "Feedback");
		Assert.assertTrue(pageObj.feedbackPage().isFeedbackWithOnlyRatingUnprocessed(feedbackWithOnlyRating_id));
		TestListeners.extentTest.get().pass("Feedback with only rating is available but not processed");
		logger.info("Feedback with only rating is available but not processed");

		// Verify that Feedback with both text and rating is available and processed
		String selectQueryFeedbacksTable = "SELECT feedback_type FROM feedbacks WHERE business_id = '"
				+ dataSet.get("business_id") + "' AND id = '" + feedbackWithText_id + "'";
		// Added polling to wait for FeedbackCategoryWorker to process
		String feedbackTypeInFeedbacksTable = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, selectQueryFeedbacksTable, "feedback_type", 30);
		Assert.assertEquals(feedbackTypeInFeedbacksTable, "positive", "Value not matched.");
		TestListeners.extentTest.get().pass("Guest Feedback with positive type is present in feedbacks table.");
		logger.info("Guest Feedback with positive type is present in feedbacks table.");
		Assert.assertTrue(pageObj.feedbackPage().isFeedbackWithTextProcessed(feedbackWithText_id,
				dataSet.get("sentimentCategories"), feedbackText));
		TestListeners.extentTest.get().pass("Feedback with both text and rating is available and processed");
		logger.info("Feedback with both text and rating is available and processed");
	}

	@Test(description = "SQ-T4365: Verifying Feedback sentiments adapter on Stats Configuration page", groups = {
			"regression", "dailyrun" }, priority = 2)
	@Owner(name = "Vaibhav Agnihotri")
	public void T4365_FeedbackSentimentsAdapterStatsConfigurationPage() throws InterruptedException {
		// Navigate to SRE > Stats configuration
		String dbOption = "Databricks";
		pageObj.menupage().navigateToSubMenuItem("SRE", "Stats Configuration");
		utils.waitTillPagePaceDone();

		// Verify that Stats Configuration is Updated successfully
		utils.selectDrpDwnValue(utils.getLocator("menuPage.feedBackSentiments"), dbOption);
		pageObj.cockpitGuestPage().clickUpdateBtn();
		String message = utils.getSuccessMessage();
		Assert.assertEquals(message, "Stats Configuration Updated", "Success message is not same");
		TestListeners.extentTest.get()
				.pass("Stats Configuration is updated with " + dbOption + " option selected for Feedback Sentiment.");
		logger.info("Stats Configuration is updated with " + dbOption + " option selected for Feedback Sentiment.");

		// Verify that Feedback Sentiment page gets loaded successfully
		pageObj.dashboardpage().navigateToAllBusinessPage();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Feedback", "Feedback Sentiment");
		Assert.assertTrue(utils.verifyPartOfURL(dataSet.get("partOfURL")), "URL did not match");
		TestListeners.extentTest.get().pass("Navigated to Feedback Sentiment page successfully.");
		logger.info("Navigated to Feedback Sentiment page successfully.");

		// Verify that Feedback Manager page can redirect to Feedback Sentiment
		pageObj.menupage().navigateToSubMenuItem("Feedback", "Feedback");
		pageObj.segmentsBetaPage()
				.verifyPresenceAndClick(utils.getLocatorValue("feedbackPage.feedbackSentimentButton"));
		Assert.assertTrue(utils.verifyPartOfURL(dataSet.get("partOfURL")), "URL did not match");
		TestListeners.extentTest.get().pass("Navigated to Feedback Sentiment from Feedback Manager page successfully.");
		logger.info("Navigated to Feedback Sentiment from Feedback Manager page successfully.");

	}

	@Test(description = "SQ-T5291: Verify switching of ChatGPT reply option in Cockpit; "
			+ "SQ-T5227: Verify Autoresponder with different feedback replies; "
			+ "SQ-T5292: Verify ChatGPT replies show up in user timeline and database", groups = { "regression",
					"dailyrun" }, priority = 1)
	@Owner(name = "Vaibhav Agnihotri")
	public void T5227_verifyChatGptReply() throws Exception {
		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Go to Cockpit > Dashboard, ensure the 'Enable Reviews and Referrals?' flag is
		// turned ON
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_reviews", "check");
		pageObj.dashboardpage().updateCheckBox();

		// Go to Miscellaneous Config tab, ensure the 'Enable chatgpt feedback' flag is
		// turned OFF
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable chatgpt feedback", "uncheck");

		// Create a user using API call
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200,
				"Status code 200 did not match for API v2 User signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		TestListeners.extentTest.get().info("API v2 user signup call is successful.");
		logger.info("API v2 user signup call is successful.");

		// Create a feedback with both text and rating using API call
		String feedbackText = "Good Coffee.";
		Response createfeedbackResponse = pageObj.endpoints().api1CreateFeedback("5", feedbackText,
				dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(createfeedbackResponse.getStatusCode(), 200,
				"Status code 200 did not match for API v1 Create Guest feedback");
		String feedbackId = createfeedbackResponse.jsonPath().getString("id").toString();
		TestListeners.extentTest.get()
				.info("API v1 Create Guest feedback call with both text and rating is successful with feedback id: "
						+ feedbackId);
		logger.info("API v1 Create Guest feedback call with both text and rating is successful with feedback id: "
				+ feedbackId);

		// Go to Feedback > Feedback. Verify that "Reply with ChatGPT" button is not
		// available
		pageObj.menupage().navigateToSubMenuItem("Feedback", "Feedback");
		Assert.assertFalse(pageObj.feedbackPage().isReplyWithChatGPTButtonVisible(feedbackId));
		TestListeners.extentTest.get().pass("As flag is turned OFF, 'Reply with ChatGPT' button is not available.");
		logger.info("As flag is turned OFF, 'Reply with ChatGPT' button is not available.");

		// Go back to Cockpit > Dashboard > Miscellaneous Config tab, ensure the 'Enable
		// chatgpt feedback' flag is turned ON
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable chatgpt feedback", "check");

		// Go to Feedback > Feedback. Verify that Autoresponder tooltip is available and
		// click on 'Reply with ChatGPT' button
		pageObj.menupage().navigateToSubMenuItem("Feedback", "Feedback");
		pageObj.feedbackPage().searchFeedback(feedbackText);
		Assert.assertEquals(pageObj.feedbackPage().getChatGptDisclaimerText(feedbackId), dataSet.get("disclaimer"),
				"The disclaimer present in tooltip does not match.");
		pageObj.feedbackPage().clickReplyWithChatGPT(feedbackId);
		TestListeners.extentTest.get()
				.pass("As flag is turned ON, 'Reply with ChatGPT' button and tooltip is available.");
		logger.info("As flag is turned ON, 'Reply with ChatGPT' button and tooltip is available.");

		// Verify the ChatGPT generated pre-filled replies and help text on Messaging -
		// Optional page
		String preFilledText1 = pageObj.guestTimelinePage().replyTextareaValue();
		pageObj.guestTimelinePage().refreshpage();
		String preFilledText2 = pageObj.guestTimelinePage().replyTextareaValue();
		Assert.assertNotEquals(preFilledText1, preFilledText2, "Both the reply messages are same after refreshing.");
		Assert.assertTrue(utils.isTextPresent(driver, dataSet.get("replyHelpText")), "The help text is not present.");
		TestListeners.extentTest.get()
				.pass("Both the generated replies are different. Help text below reply text area is available.");
		logger.info("Both the generated replies are different. Help text below reply text area is available.");

		// Edit the ChatGPT generated reply with custom text and Send the message
		String customMsg = " custom message";
		String customReplyText = preFilledText2 + customMsg;
		pageObj.guestTimelinePage().editReplyText(customReplyText);
		pageObj.guestTimelinePage().msgBtn();

		// Verify both Guest Feedback and ChatGPT reply posted are present on user
		// timeline
		pageObj.guestTimelinePage().checkUncheckFilterEvents("Feedbacks & Replies", "check");
		Assert.assertTrue(pageObj.guestTimelinePage().verifyTitleFromTimeline(feedbackText),
				"Guest Feedback text on timeline did not match.");
		Assert.assertTrue(pageObj.guestTimelinePage().verifyChatGPTReplyonGuestTimeLine(customReplyText),
				"Feedback reply text on timeline did not match.");
		TestListeners.extentTest.get()
				.pass("Both Guest Feedback and ChatGPT reply posted are present on user timeline.");
		logger.info("Both Guest Feedback and ChatGPT reply posted are present on user timeline.");

		// Verify that Guest Feedback is present in MySQL feedbacks table
		String selectQueryFeedbacksTable = "SELECT message FROM feedbacks WHERE business_id = '"
				+ dataSet.get("businessId") + "' AND id = '" + feedbackId + "'";
		pageObj.singletonDBUtilsObj();
		String feedbackTextInFeedbacksTable = DBUtils.executeQueryAndGetColumnValue(env,
				selectQueryFeedbacksTable, "message");
		Assert.assertEquals(feedbackTextInFeedbacksTable, feedbackText, "Value not matched.");
		TestListeners.extentTest.get().pass("Guest Feedback record is present in feedbacks table.");
		logger.info("Guest Feedback record is present in feedbacks table.");

		// Verify both ChatGPT reply and Custom reply posted are present in MySQL
		// feedback_replies table
		String selectQueryFeedbackRepliesTable = "SELECT reply, chatgpt_feedback_response FROM feedback_replies WHERE business_id = '"
				+ dataSet.get("businessId") + "' AND feedback_id = '" + feedbackId + "'";
		pageObj.singletonDBUtilsObj();
		String chatGptReplyInFeedbackRepliesTable = DBUtils.executeQueryAndGetColumnValue(env,
				selectQueryFeedbackRepliesTable, "chatgpt_feedback_response");
		pageObj.singletonDBUtilsObj();
		String customReplyInFeedbackRepliesTable = DBUtils.executeQueryAndGetColumnValue(env,
				selectQueryFeedbackRepliesTable, "reply");
		Assert.assertEquals(chatGptReplyInFeedbackRepliesTable, preFilledText2, "Value not matched.");
		Assert.assertEquals(customReplyInFeedbackRepliesTable, customReplyText, "Value not matched.");
		TestListeners.extentTest.get()
				.pass("Both ChatGPT reply and Custom reply posted are present in feedback_replies table.");
		logger.info("Both ChatGPT reply and Custom reply posted are present in feedback_replies table.");

		// On Feedback page, verify that the business reply link is present and reply
		// posted is available
		pageObj.menupage().navigateToSubMenuItem("Feedback", "Feedback");
		String businessReplyDisplayedText = pageObj.feedbackPage().getBusinessReplyText(feedbackId);
		Assert.assertTrue(businessReplyDisplayedText.contains(customReplyText),
				"The Business Reply text did not match.");
		TestListeners.extentTest.get().pass("Business reply link is present and Custom reply posted is available.");
		logger.info("Business reply link is present and Custom reply posted is available.");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}

}
