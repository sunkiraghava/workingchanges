package com.punchh.server.EventsFrameworkNewCases;

import java.awt.HeadlessException;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.punchh.server.annotations.Owner;
import com.punchh.server.utilities.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import com.punchh.server.pages.PageObj;
import io.restassured.response.Response;

//@Author = Rohit Doraya 
@Listeners(TestListeners.class)
public class EventFrameWorkHeadersValidationTest {
	static Logger logger = LogManager.getLogger(EventFrameWorkHeadersValidationTest.class);
	public WebDriver driver;
	String userEmail;
	String email = "autoemailTemp@punchh.com";
	ApiUtils apiUtils;
	PageObj pageObj;
	String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private String segID = "11554400";
	private String segName = "Test MP Audience " + segID;
	private static Map<String, String> dataSet;
	Utilities utils;
	private String deleteRedeemableQuery = "delete from redeemables where uuid ='$redeemableExternalID' and business_id ='$businessID'";

	@BeforeMethod()
	public void beforeMethod(Method method) throws InterruptedException {
		driver = new BrowserUtilities().launchBrowser();
		apiUtils = new ApiUtils();
		pageObj = new PageObj(driver);
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
	}

	

	//automationtwentysix
	@Test(groups = {"regression", "dailyrun" },priority = 1, description = "SQ-T5824 [EF - Custom Webhook] Verify the Header in EF logs based on Header flag (Step-7)"
			+ "SQ-T5840 [EF - Events Visibility] Event Visibility in the Webhooks & Adapter tabs based on flags available in the Configurations Tabs")
	@Owner(name = "Shashank Sharma")
	public void T5824_VerifyHeaderInEFLogsBasedOnHeaderFlag_Part1() throws InterruptedException {

		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		boolean isWebhookCreatedSuccessfully = false;
		String webhookName = CreateDateTime.getUniqueString("AutomationTestWebhook_");
		List<String> eventNameList = new ArrayList<String>();
		eventNameList.add("Loyalty Checkin");
		// Navigate to Business and select the business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Webhooks Management?", "check");
		pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
		pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration", "Enable Success Headers Logging",
				false); 
		pageObj.webhookManagerPage().clickOnSubmitButton();

		pageObj.webhookManagerPage().activeOrInactiveEventsFromConfiguration("Loyalty Checkin", "Active Events",
				"Inactive Events");
		pageObj.webhookManagerPage().activeOrInactiveEventsFromConfiguration("Rewards", "Active Events",
				"Inactive Events");

		pageObj.webhookManagerPage().clickOnSubmitButton();

		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Webhooks");
		pageObj.webhookManagerPage().clickOnCreateWebhookButton();
		boolean isLoyaltyCheckinEventDisplayed = pageObj.webhookManagerPage()
				.verifyEventIsDisplayingInEventSelectionDropDown("Loyalty Checkin");

		Assert.assertFalse(isLoyaltyCheckinEventDisplayed,
				"Loyalty Checkin event is displaying in event selection drop down");
		logger.info("Verified that Loyalty Checkin event is not displaying in event selection drop down");
		TestListeners.extentTest.get()
				.info("Verified that Loyalty Checkin event is not displaying in event selection drop down");

		boolean isRewardsEventDisplayed = pageObj.webhookManagerPage()
				.verifyEventIsDisplayingInEventSelectionDropDown("Rewards");

		Assert.assertFalse(isRewardsEventDisplayed, "Rewards event is displaying in event selection drop down");
		logger.info("Verified that Rewards event is not displaying in event selection drop down");
		TestListeners.extentTest.get()
				.info("Verified that Rewards event is not displaying in event selection drop down");

		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
		pageObj.webhookManagerPage().activeOrInactiveEventsFromConfiguration("Loyalty Checkin", "Inactive Events",
				"Active Events");
		pageObj.webhookManagerPage().activeOrInactiveEventsFromConfiguration("Rewards", "Inactive Events",
				"Active Events");
		pageObj.webhookManagerPage().clickOnSubmitButton();

		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Webhooks");
		pageObj.webhookManagerPage().clickOnCreateWebhookButton();
		isLoyaltyCheckinEventDisplayed = pageObj.webhookManagerPage()
				.verifyEventIsDisplayingInEventSelectionDropDown("Loyalty Checkin");

		Assert.assertTrue(isLoyaltyCheckinEventDisplayed,
				"Loyalty Checkin event is not displaying in event selection drop down");
		logger.info("Verified that Loyalty Checkin event is  displaying in event selection drop down");
		TestListeners.extentTest.get()
				.info("Verified that Loyalty Checkin event is  displaying in event selection drop down");

		isRewardsEventDisplayed = pageObj.webhookManagerPage()
				.verifyEventIsDisplayingInEventSelectionDropDown("Rewards");

		Assert.assertTrue(isRewardsEventDisplayed, "Rewards event is not displaying in event selection drop down");
		logger.info("Verified that Rewards event is  displaying in event selection drop down");
		TestListeners.extentTest.get().info("Verified that Rewards event is  displaying in event selection drop down");

	} // end T5824_VerifyHeaderInEFLogsBasedOnHeaderFlag need to complete
	
	
	//automationtwentysix
	@Test(groups = {"regression", "dailyrun" },priority = 2, description = "SQ-T5824 [EF - Custom Webhook] Verify the Header in EF logs based on Header flag (Step-7)"
			+ "SQ-T5821 [EF - Custom Webhook] Verification of Guest, Loyalty Checkin & Gift Checkin Events and assertion in the Logs (Step-4.1)")
	@Owner(name = "Shashank Sharma")
	public void T5824_VerifyHeaderInEFLogsBasedOnHeaderFlag_Part2() throws InterruptedException, HeadlessException, UnsupportedFlavorException, IOException {

		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		String locationKey = dataSet.get("locationkey");
		boolean isWebhookCreatedSuccessfully = false;
		String webhookName = CreateDateTime.getUniqueString("AutomationTestWebhook_");
//		try {
			List<String> eventNameList = new ArrayList<String>();
			eventNameList.add("Guest");
			eventNameList.add("Loyalty Checkin");
			// Navigate to Business and select the business
			pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
			pageObj.instanceDashboardPage().loginToInstance();
			pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
			pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
			pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Webhooks Management?", "check");

			// generateBarcode
			pageObj.menupage().navigateToSubMenuItem("Support", "Test Barcodes");
			pageObj.instanceDashboardPage().generateBarcode(dataSet.get("locationkey"));
			String barcode = pageObj.instanceDashboardPage().captureBarcode();

			pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
			pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration",
					"Enable Success Headers Logging", true);
			pageObj.webhookManagerPage().clickOnSubmitButton();

			pageObj.webhookManagerPage().activeOrInactiveEventsFromConfiguration("Loyalty Checkin", "Inactive Events",
					"Active Events");
			pageObj.webhookManagerPage().clickOnSubmitButton();
			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Webhooks");
			pageObj.webhookManagerPage().deleteAllWebhookOrAdapter("Webhooks");
			pageObj.webhookManagerPage().clickOnCreateWebhookButton();

			pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Name", webhookName);
			pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Description",
					webhookName + " Description");
			pageObj.webhookManagerPage().selectValueFromDropDownOnWebhookCreatePage("Base URL",
					"Custom Webhook-1 Base URL (https://dashboard.staging.punchh.io)");
			pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Webhook End Point", "/sidekiq");
			pageObj.webhookManagerPage().selectValueFromDropDownOnWebhookCreatePage("Authentication", "None");
			pageObj.webhookManagerPage().selectEvent(eventNameList);
			pageObj.webhookManagerPage().clickOnActiveCheckBox(true);
			pageObj.webhookManagerPage().clickOnSubmitButton();
			logger.info(webhookName + " webhook is created successfully");
			TestListeners.extentTest.get().info(webhookName + " webhook is created successfully");
			isWebhookCreatedSuccessfully = true;
			// create New User
			/// sign-up user
			userEmail = pageObj.iframeSingUpPage().generateEmail();
			Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
					dataSet.get("secret"));
			Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api1 signup");
			logger.info("Api1 user signup is successful");
			TestListeners.extentTest.get().pass("Api1 user signup is successful");
			String access_token = signUpResponse.jsonPath().get("auth_token.token");
			String timeAfterEventTriggered =  getCurrentTimeStampAfterEventTriggered();
			// checkin via APIV1
			Response checkinRes = pageObj.endpoints().Api1LoyaltyCheckinBarCode(dataSet.get("client"),
					dataSet.get("secret"), access_token, barcode);

			
			// Do checkin of 3
			Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, "50");
			Assert.assertEquals(checkinResponse1.getStatusCode(), 200,
					"Status code 200 did not match with POS Checkin ");
			logger.info("POS checkin is successful for " + "3" + " dollar");
			TestListeners.extentTest.get().pass("POS checkin is successful for " + "3" + " dollar");
			// checking logs for rewards
			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");
			
			String jsonObjectStrGuests = pageObj.webhookManagerPage().openLogsForWebhook(userEmail, "Guest", webhookName,"",timeAfterEventTriggered);
			String actualEventName=utils.findValueByKeyFromJsonAsString(jsonObjectStrGuests, "event_name") ;
			
			Assert.assertEquals(actualEventName, "users", "user event is not matched in logs");
			logger.info("Verified that Guest event is coming in logs");
			TestListeners.extentTest.get().info("Verified that Guest event is coming in logs");
			
			String actualEmail=utils.findValueByKeyFromJsonAsString(jsonObjectStrGuests, "email") ;
			Assert.assertEquals(actualEmail , userEmail, userEmail+" Guest email value is not matching in logs");
			logger.info("Verified that Loyalty Checkin event is coming in logs");
			TestListeners.extentTest.get().info("Verified that Loyalty Checkin event is coming in logs");

			String jsonObjectStrLoyaltyCheckin = pageObj.webhookManagerPage().openLogsForWebhook(userEmail, "Loyalty Checkin", webhookName,"",timeAfterEventTriggered);

			String actualEvenType=utils.findValueByKeyFromJsonAsString(jsonObjectStrLoyaltyCheckin, "event_type") ;
			Assert.assertEquals(actualEvenType , "loyalty", "loyalty event is not coming in logs");
			logger.info("Verified that loyalty event is coming in logs");
			TestListeners.extentTest.get().info("Verified that loyalty event is coming in logs");
			
			String actualEmailInLoyaltyEvent=utils.findValueByKeyFromJsonAsString(jsonObjectStrLoyaltyCheckin, "email") ;
			Assert.assertEquals(actualEmailInLoyaltyEvent , userEmail, userEmail+" emailid in loyalty event is not coming in logs");
			logger.info("Verified that Guests event is coming in logs");
			TestListeners.extentTest.get().info("Verified that Guests event is coming in logs");

			String actualEventNameInLoyaltyEvent=utils.findValueByKeyFromJsonAsString(jsonObjectStrLoyaltyCheckin, "event_name") ;
			Assert.assertEquals(actualEventNameInLoyaltyEvent , "checkins", actualEventNameInLoyaltyEvent+"  loyalty event is not coming in logs");
			
			logger.info("Verified that Loyalty Checkin event is coming in logs");
			TestListeners.extentTest.get().info("Verified that Loyalty Checkin event is coming in logs");

//		}catch (AssertionError ae){
//			logger.info(ae.getMessage());
//			TestListeners.extentTest.get().info(ae.getMessage());
//			Assert.fail(ae.getMessage());
//
//		} catch (Exception e) {
//			logger.info(e.getMessage());
//			TestListeners.extentTest.get().info(e.getMessage());
//			Assert.fail(e.getMessage());
//		} finally {
//			if (isWebhookCreatedSuccessfully) {
//				pageObj.webhookManagerPage().deleteWebhook("Webhooks", webhookName, "Delete");
//				logger.info(webhookName + " webhook is deleted successfully");
//				TestListeners.extentTest.get().info(webhookName + " webhook is deleted successfully");
//
//			}
//		}

	} // end T5824_VerifyHeaderInEFLogsBasedOnHeaderFlag need to complete

	
	//automationtwentysix
	@Test(groups = {"regression", "dailyrun" },description = "SQ-T5839 [EF - Webhook, Adapter Tab] Visibility of Webhook & Adapter tabs in UI based on flags in Configurations Tab.", priority = 3)
	@Owner(name = "Shashank Sharma")
	public void T5839_VisibilityOfWebhookAdapterTabsInUIBasedOnFlags() throws InterruptedException {

	//	try {
			// Navigate to Business and select the business
			pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
			pageObj.instanceDashboardPage().loginToInstance();
			pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
			pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
			pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Webhooks Management?", "check");

			pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
			pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration", "Show Webhooks Tab", false);
			//pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration", "Show Adapters Tab", false);
			pageObj.webhookManagerPage().clickOnSubmitButton();
			
			boolean isWebhookTabDisplayed = pageObj.webhookManagerPage().verifyWebhookOrAdapterTabIsDisplaying("Webhooks");
			Assert.assertFalse(isWebhookTabDisplayed, " Webhooks tab is displaying");
			logger.info("Verified that Webhooks tabs is not displaying");
			TestListeners.extentTest.get().pass("Verified that Webhooks tabs is not displaying");

//			boolean isAdaptersTabDisplayed = pageObj.webhookManagerPage().verifyWebhookOrAdapterTabIsDisplaying("Adapters");
//			Assert.assertFalse(isAdaptersTabDisplayed, " Adapters tab is displaying");
//			logger.info("Verified that Adapters tabs is not displaying");
//			TestListeners.extentTest.get().pass("Verified that Adapters tabs is not displaying");

			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
			pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration", "Show Webhooks Tab", true);
//			pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration", "Show Adapters Tab", true);
			pageObj.webhookManagerPage().clickOnSubmitButton();

			boolean isWebhookTabDisplayed1 = pageObj.webhookManagerPage().verifyWebhookOrAdapterTabIsDisplaying("Webhooks");
			Assert.assertTrue(isWebhookTabDisplayed1, " Webhooks tab is displaying");
			logger.info("Verified that Webhooks tabs is displaying");
			TestListeners.extentTest.get().pass("Verified that Webhooks tabs is displaying");

//			isAdaptersTabDisplayed = pageObj.webhookManagerPage().verifyWebhookOrAdapterTabIsDisplaying("Adapters");
//			Assert.assertTrue(isAdaptersTabDisplayed, " Adapters tab is not displaying");
//			logger.info("Verified that Adapters tabs is  displaying");
//			TestListeners.extentTest.get().pass("Verified that Adapters tabs is displaying");
			
			
			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
			pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration", "Show Webhooks Tab", true);
			pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration", "Show Adapters Tab", true);
			pageObj.webhookManagerPage().clickOnSubmitButton();
			
			
//		}catch (AssertionError ae){
//			logger.info(ae.getMessage());
//			TestListeners.extentTest.get().info(ae.getMessage());
//			Assert.fail(ae.getMessage());
//
//		}catch (Exception e){
//			logger.info(e.getMessage());
//			TestListeners.extentTest.get().info(e.getMessage());
//			Assert.fail(e.getMessage());
//		}
//		finally {
//			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
//			pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration", "Show Webhooks Tab", true);
//			pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration", "Show Adapters Tab", true);
//			pageObj.webhookManagerPage().clickOnSubmitButton();
//		}

	} // end T5824_VerifyHeaderInEFLogsBasedOnHeaderFlag need to complete
	
	
	@AfterMethod
	public void afterClass() {
		dataSet.clear();
		driver.quit();
		logger.info("Browser closed");
	}
	
	public String getCurrentTimeStampAfterEventTriggered() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy hh:mm:ss a");
        LocalDateTime now = LocalDateTime.now();
        String formatted = now.format(formatter);
        return formatted.toString();
	}
	
}
