package com.punchh.server.segmentsTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class SegmentEligibilityAPITest {
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private Utilities utils;
	private static Map<String, String> dataSet;
	String run = "ui";
	private String userEmail, segmentName;
	static String randOrderId;
	static String randEmail;
	static String randomFirstName;
	static String randomLastName;

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

		prop = Utilities.loadPropertiesFile("config.properties");
		sTCName = method.getName();
		utils = new Utilities(driver);
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		pageObj.utils().logit(sTCName + " ==>" + dataSet);

	}

	@Test(description = "SQ-T6615 Verify the API responses in for the segment eligibility API", groups = { "regression",
			"dailyrun" }, priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T6615_segmentEligibilityApi() throws Exception {

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// User is eligible for segment
		// get segment members count
		Response response = pageObj.endpoints().getSegmentCount(dataSet.get("apiKey"), dataSet.get("segmentId"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 201 did not matched");
		String count = response.jsonPath().get("count");
		String cnt1 = count.replaceAll(",", "");
		int segmentCount = Integer.parseInt(cnt1);
		Assert.assertTrue(segmentCount >= 0, "Custom Segment count is not greater than 0");

		boolean eligibilityStatus = pageObj.segmentsPage().segmentEligibilityPolling(dataSet.get("client"),
				dataSet.get("secret"), token, userID, dataSet.get("segmentId"), 15);
		Assert.assertTrue(eligibilityStatus, "User is eligible for segment but API is giving Wrong Status");
		utils.logPass("User is eligible for segment and API is also giving same Status");

		// User is not eligible for segment
		// get segment members count
		Response response1 = pageObj.endpoints().getSegmentCount(dataSet.get("apiKey"), dataSet.get("segmentId1"));
		Assert.assertEquals(response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 201 did not matched");
		count = response1.jsonPath().get("count");
		cnt1 = count.replaceAll(",", "");
		segmentCount = Integer.parseInt(cnt1);
		Assert.assertTrue(segmentCount >= 0, "Custom Segment count is not greater than 0");

		boolean eligibilityStatus1 = pageObj.segmentsPage().segmentEligibilityPolling(dataSet.get("client"),
				dataSet.get("secret"), token, userID, dataSet.get("segmentId1"), 4);
		Assert.assertFalse(eligibilityStatus1, "User is not eligible for segment but API is giving Wrong Status");
		utils.logPass("User is not eligible for segment and API is also giving same Status");

		// User id is incorrect
		String incorrectUserID = userID + userID;
		Response segmentEligibilityResponse = pageObj.endpoints().Api2MobileSegmentEligibility(dataSet.get("client"),
				dataSet.get("secret"), token, incorrectUserID, dataSet.get("segmentId"));
		Assert.assertEquals(segmentEligibilityResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND, "Status code 200 did not matched");
		String eligibilityError = segmentEligibilityResponse.jsonPath().get("error").toString();
		Assert.assertEquals(eligibilityError, "User not found", "Error message is not matching for incorrect user Id");
		utils.logPass("Error message matched for incorrect user Id");

		// segment id is incorrect
		String incorrectSegmentId = dataSet.get("segmentId") + dataSet.get("segmentId");
		Response segmentEligibilityResponse1 = pageObj.endpoints().Api2MobileSegmentEligibility(dataSet.get("client"),
				dataSet.get("secret"), token, userID, incorrectSegmentId);
		Assert.assertEquals(segmentEligibilityResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND, "Status code 200 did not matched");
		String eligibilityError1 = segmentEligibilityResponse1.jsonPath().get("error").toString();
		Assert.assertEquals(eligibilityError1, "Segment not found",
				"Error message is not matching for incorrect Segment Id");
		utils.logPass("Error message matched for incorrect Segment Id");

		// User id is missing
		incorrectUserID = "";
		Response segmentEligibilityResponse2 = pageObj.endpoints().Api2MobileSegmentEligibility(dataSet.get("client"),
				dataSet.get("secret"), token, incorrectUserID, dataSet.get("segmentId"));
		Assert.assertEquals(segmentEligibilityResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST, "Status code 200 did not matched");
		String eligibilityError2 = segmentEligibilityResponse2.jsonPath().get("error").toString();
		Assert.assertEquals(eligibilityError2, "DLC Identifier and Segment ID are required",
				"Error message is not matching for missing user Id");
		utils.logPass("Error message matched for missing user Id");

		// segment id is missing
		incorrectSegmentId = "";
		Response segmentEligibilityResponse3 = pageObj.endpoints().Api2MobileSegmentEligibility(dataSet.get("client"),
				dataSet.get("secret"), token, userID, incorrectSegmentId);
		Assert.assertEquals(segmentEligibilityResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST, "Status code 200 did not matched");
		String eligibilityError3 = segmentEligibilityResponse3.jsonPath().get("error").toString();
		Assert.assertEquals(eligibilityError3, "DLC Identifier and Segment ID are required",
				"Error message is not matching for missing Segment Id");
		utils.logPass("Error message matched for missing Segment Id");
	}
	
	
	@Test(description = "SQ-T7078 Verify that the users added through PAR ordering API are reflecting as a part of the guest segmentation"
			+ "SQ-T7085 Verify the Guest Opt In api is working fine"
			+ "SQ-T7129 Verify the Par ordering API functionality with different ordering key and signature."
			+ "SQ-T7128 Verify that the PAR ordering API adding the user correctly in user tables",priority = 1)
	@Owner(name = "Apurva Agarwal")
	public void T7078_verifyParOrderAPIUsersInGuestSegmentation() throws Exception {
		// Navigate to All Business Page and select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.utils().logPass("Navigated to business: " + dataSet.get("slug"));
		
		 // Generate Random Test Data
		String randId = Integer.toString(Utilities.getRandomNoFromRange(1000, 9000));
		String randOrderId = Integer.toString(Utilities.getRandomNoFromRange(9000, 15000));
		String randPOSRef = Integer.toString(Utilities.getRandomNoFromRange(20000, 25000));
		String randCsId = Integer.toString(Utilities.getRandomNoFromRange(25000, 30000));
		String randEmail= pageObj.iframeSingUpPage().generateEmail();
		String invalidParOrderingSecretKey = Utilities.generateRandomString(10);
		String invalidStoreNo = Integer.toString(Utilities.getRandomNo(1000));
		pageObj.utils().logPass("Generated test data for API execution");
		
		// Step 1: Trigger PAR Menu Order API
		Response apiOrderingResponse = pageObj.endpoints().parMenuOrder(dataSet.get("slug"),randId,dataSet.get("apiKey"),
				randEmail,dataSet.get("store_number"),randPOSRef,dataSet.get("loginProviders_slug"),randCsId,randOrderId,dataSet.get("par_ordering_secret_key"));
		pageObj.utils().logit(apiOrderingResponse.asPrettyString()) ;
		Assert.assertEquals(apiOrderingResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched");
		Assert.assertTrue(apiOrderingResponse.asString().contains("Processed Successfully"),"Response does not contain 'Processed Successfully'");
		utils.logPass("Verified PAR ordering API returned status code 200");

		// Step 2: Navigate to Cockpit -> Dashboard
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.cockpitDashboardMiscPage().selectOrderingVendor(dataSet.get("parVendor"));
		utils.logPass("Selected ordering vendor: " + dataSet.get("parVendor"));
		
		 // Step 3: Verify User Reflects in Guest Segmentation
		Response segmentAPIResponse = pageObj.endpoints().userInSegment(randEmail,dataSet.get("apiKey"),dataSet.get("segmentId"));
		
		 // Validate API Status Code = 200
		Assert.assertEquals(segmentAPIResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched");
		pageObj.utils().logPass("Verified segmentation API returned status code 200");

		// Validate 'result' = true
		boolean result = segmentAPIResponse.jsonPath().getBoolean("result");
	    Assert.assertTrue(result, "Expected result=true in the response");
	    pageObj.utils().logPass("Verified segment API returned result=true for user: " + randEmail);

	    utils.logPass("Guest '" + randEmail + "' successfully found in segmentation: " + dataSet.get("segmentId"));
	    
		// verify eclub user created through Guest Opt-in API appear on timeline
	    pageObj.instanceDashboardPage().navigateToGuestTimeline(randEmail);
		pageObj.guestTimelinePage().verifyEclubUser(randEmail);
		String userId = utils.getUserIdFromUrl();
		
		// run the API again for the same User
		Response apiOrderingResponse2 = pageObj.endpoints().parMenuOrder(dataSet.get("slug"), randId,
				dataSet.get("apiKey"), randEmail, dataSet.get("store_number"), randPOSRef,
				dataSet.get("loginProviders_slug"), randCsId, randOrderId, dataSet.get("par_ordering_secret_key"));
		pageObj.utils().logit(apiOrderingResponse2.asPrettyString());
		Assert.assertEquals(apiOrderingResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched");
		Assert.assertTrue(apiOrderingResponse2.asString().contains("Processed Successfully"),
				"Response does not contain 'Processed Successfully'");
		utils.logPass("Guest Opt-in API executed successfully for same user : " + randEmail);

		utils.refreshPage();
		// verify user don't get double gifting or notification
		int count = pageObj.guestTimelinePage().getJoinedViaNotificationCount("Uploaded to eClub");
		Assert.assertEquals(count, 1, "Uploaded to eClub notification is more than once");
		utils.logPass("Eclub user don't get duplicate notification on timeline on multiple Guest opt-in API calls");
		
		// verify user is reflecting in users table
		String query1 = "SELECT count(*) FROM users WHERE email = '" + randEmail
				+ "';";
		int expColValue1 = DBUtils.executeQueryAndGetCount(env, query1);
		Assert.assertEquals(expColValue1, 1, "User not created in users table after Guest Opt-in API");
		utils.logPass("Verified that user is created in users table after Guest Opt-in API");

		// verify user is reflecting in user_transition table
		String query2 = "SELECT count(*) FROM user_transitions WHERE user_id = '" + userId
				+ "';";
		int expColValue2 = DBUtils.executeQueryAndGetCount(env, query2);
		Assert.assertEquals(expColValue2, 1, "User not created in user_transitions table after Guest Opt-in API");
		utils.logPass("Verified that user is created in user_transitions table after Guest Opt-in API");

		// verify user is reflecting in bulk_guest_upload_connections table
		String query4 = "SELECT count(*) FROM bulk_guest_upload_connections WHERE user_id = '" + userId + "';";
		int expColValue4 = DBUtils.executeQueryAndGetCount(env, query4);
		Assert.assertEquals(expColValue4, 1,
				"User not created in bulk_guest_upload_connections table after Guest Opt-in API");
		utils.logPass("Verified that user is created in bulk_guest_upload_connections table after Guest Opt-in API");	
		
		// verify user is reflecting in checkins table
		String query5 = "SELECT count(*) FROM checkins WHERE user_id = '" + userId + "';";
		int expColValue5 = DBUtils.executeQueryAndGetCount(env, query5);
		Assert.assertEquals(expColValue5, 1, "User not created in checkins table after Guest Opt-in API");
		utils.logPass("Verified that user is created in checkins table after Guest Opt-in API");	
		
		// loyalty sign up for the same user
		Response response = pageObj.endpoints().posSignUp(randEmail, dataSet.get("locationKey"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logPass("User created with POS user signup");
		
		utils.refreshPage();
		// verify eclub user converted to loyalty
		boolean loyalityFlag = pageObj.guestTimelinePage().flagPresentorNot("Loyalty");
		Assert.assertTrue(loyalityFlag, "Loyalty flag should displayed but it is not visible");
		utils.logPass("Eclub user is converted to loyalty user and Loyalty flag is visible for this guest");

		// guest opt in API with invalid token
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response eclubUploadResponse = pageObj.endpoints().parMenuOrder(dataSet.get("slug"), randId,"", userEmail, dataSet.get("store_number"), "", "Punchh", "",
				randOrderId, invalidParOrderingSecretKey);
		Assert.assertEquals(eclubUploadResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for close order online");
		Assert.assertEquals(eclubUploadResponse.jsonPath().getString("[0]"), "Invalid Signature",
				"Guest Opt is wokring fne with wrong par ordering secret key");
		utils.logPass("Guest Opt In API unsuccessful with invalid par ordering secret key");
		
		// guest opt in API with invalid store location
		Response eclubUploadResponse1 = pageObj.endpoints().parMenuOrder(dataSet.get("slug"), randId, "", userEmail,
				invalidStoreNo, "", "Punchh", "", randOrderId, dataSet.get("par_ordering_secret_key"));
		Assert.assertEquals(eclubUploadResponse1.jsonPath().getString("error"), "Location not found.",
				"Guest Opt is wokring fne with wrong store location number");
		utils.logPass("Guest Opt In API unsuccessful with invalid store location number");
		
	}
	
	
	@Test(description = "SQ-T7105 Verify order placed API is working fine; "
			+ "SQ-T7106 Verify PAR ordering order cancelled API is working fine", priority = 2)
	@Owner(name = "Apurva Agarwal")
	public void T7105_T7106_verifyOrderPlacedAndCancelledAPI() throws InterruptedException {
		// Navigate to All Business Page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		
		// Step 1: Navigate to SRE -> Segment Configuration
		pageObj.menupage().navigateToSubMenuItem("SRE", "Segment Configuration");
		
		// Verify PAR Ordering checkbox is enabled
		pageObj.dashboardpage().verifyGlobalConfigFlagCheckedUnchecked("Enable order placed and order cancelled events for par ordering");
		
		// Step 2: Select Business Instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		
		 // Generate Random Test Data
		randOrderId = Integer.toString(Utilities.getRandomNoFromRange(9000, 15000));
		randEmail= pageObj.iframeSingUpPage().generateEmail();
		randomFirstName = CreateDateTime.getUniqueString(prop.getProperty("firstName"));
		randomLastName = CreateDateTime.getUniqueString(prop.getProperty("lastName"));
		utils.logit("Passing random Data : ", "Order ID '"+randOrderId+"' , random Email '"+randEmail+"' and random Name: '"+randomFirstName+" "+randomLastName+"'");

		
		// Step 3: Trigger PAR Order Placed API
		Response apiOrderingResponse = pageObj.endpoints().parOrderPlacedAndOrderCancelled(dataSet.get("slug"),dataSet.get("apiKey"),
				randEmail,dataSet.get("store_number"),randOrderId, dataSet.get("par_ordering_secret_key"),dataSet.get("eventType"), randomFirstName, randomLastName);
		utils.logit("API response is : " + apiOrderingResponse.asPrettyString());
		
		 // Validate API response status code
		Assert.assertEquals(apiOrderingResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched");
		
		 // Validate API response message
		Assert.assertTrue(apiOrderingResponse.asString().contains("Processed Successfully"),"Response does not contain 'Processed Successfully'");
		utils.logit("Pass","Verified Order Placed API returned status code 200 and processed successfully");

		//Step 4: Verify Guest Timeline & EClub Label
		pageObj.instanceDashboardPage().navigateToGuestTimeline(randEmail);
		
		// Validate EClub label is displayed on Guest Profile
		Assert.assertTrue(pageObj.gamesPage().isPresent(utils.getLocatorValue("guestTimeLine.eClubLabel")),"EClub Label not displayed on Guest Profile");
		utils.logit("Pass","EClub label displayed as expected on Guest Profile");
		
		// SQ-T7106 starts here
		// Step 3: Trigger PAR Order Cancelled API
		Response apiOrderCancelledResponse = pageObj.endpoints().parOrderPlacedAndOrderCancelledWithRetry(dataSet.get("slug"),
				dataSet.get("apiKey"), randEmail, dataSet.get("store_number"), randOrderId,
				dataSet.get("par_ordering_secret_key"), dataSet.get("eventTypeCancelled"), randomFirstName, randomLastName);
		utils.logit("API response is : " + apiOrderCancelledResponse.asPrettyString());
		
		// Validate API response status code
		Assert.assertEquals(apiOrderCancelledResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match");

		// Validate API response message
		Assert.assertTrue(apiOrderCancelledResponse.asString().contains("Processed Successfully"),
				"Response does not contain 'Processed Successfully'");
		utils.logit("Pass", "Verified Order Cancelled API returned status code 200 and processed successfully");

		// Step 4: Verify Guest Timeline & EClub Label
		pageObj.instanceDashboardPage().navigateToGuestTimeline(randEmail);

		// Validate EClub label is displayed on Guest Profile
		Assert.assertTrue(pageObj.gamesPage().isPresent(utils.getLocatorValue("guestTimeLine.eClubLabel")),
				"EClub Label not displayed on Guest Profile");
		utils.logit("Pass", "EClub label displayed as expected on Guest Profile");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().logit("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		pageObj.utils().logit("Browser closed");
	}
}