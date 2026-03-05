package com.punchh.server.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
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
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class InternationalSupportTest {
	private static Logger logger = LogManager.getLogger(InternationalSupportTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl;
	private String endDateTime;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	Properties prop;
	Utilities utils;

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
	}

	@Test(description = "SQ_T4532 Verify the API V1 for subscriptions for internationalisation support"
			+ "SQ_T4531 Verify the subscription plan name, description and miscellaneous are appearing for internationalisation support", groups = {
					"regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T4532_API_V1UserSubscriptions() throws InterruptedException {

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to settings menu
		pageObj.menupage().navigateToSubMenuItem("Settings", "Business");
		// pageObj.menupage().address_tab();
		pageObj.dashboardpage().navigateToTabs("Address");
		pageObj.settingsPage().alternateLanguages();
		pageObj.settingsPage().clickSaveBtn();

		// naviagte to subscription menu
		String spNameFr = "Nom de l'abonnement" + CreateDateTime.getTimeDateString();
		String spNameEn = "SubscriptionPlanName" + CreateDateTime.getTimeDateString();
		String descrpFr = "Descriptionen";
		String missFr = "Miscellaneousen";
		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Plans");
		pageObj.subscriptionPlansPage().createNewSubscriptionPlanInFrench(spNameFr, spNameEn, "Automation QC - 606",
				"Rate Rollback", "400", descrpFr, missFr);
		String PlanID = pageObj.subscriptionPlansPage().getSbuscriptionPlanID(spNameEn);

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();

		Response AuthsignUpResponse = pageObj.endpoints().authApiSignUpPositive(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(AuthsignUpResponse.statusCode(), ApiConstants.HTTP_STATUS_CREATED, "user sign up fail");
		String accessToken = AuthsignUpResponse.jsonPath().get("access_token").toString();
		String userID = AuthsignUpResponse.jsonPath().get("user_id").toString();
		logger.info(userID, " user signup is successfull");
		utils.logit("user signup is successfull");

		// subscription List Api
		Response purchaseSubscriptionresponse1 = pageObj.endpoints().ApiListSubscription(accessToken,
				dataSet.get("client"), dataSet.get("secret"), "fr");
		Assert.assertEquals(purchaseSubscriptionresponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		int counter = utils.returnAPIResponseArrayIndex(purchaseSubscriptionresponse1, "plan_id", PlanID);
		String subsName = purchaseSubscriptionresponse1.jsonPath().get("[" + counter + "].name").toString();
		Assert.assertEquals(subsName, spNameFr);
		logger.info("verified subcription name in French");
		utils.logPass("verified subcription name in French");
		String descrpName = purchaseSubscriptionresponse1.jsonPath().get("[" + counter + "].description").toString();
		Assert.assertEquals(descrpName, descrpFr);
		logger.info("verified description name in French");
		utils.logPass("verified description name in French");
		String missceName = purchaseSubscriptionresponse1.jsonPath().get("[" + counter + "].miscellaneous").toString();
		Assert.assertEquals(missceName, missFr);
		logger.info("verified miscellaneous name in French");
		utils.logPass("verified miscellaneous name in French");

	}

	@Test(description = "SQ-T4533 Verify the API V2 user_subscriptions for internationalisation support"
			+ "SQ-T4519 Verify the fetch user subscription API v1 for if user had subscription ever in past", groups = {
					"regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T4533_API_V2UserSubscriptions() throws InterruptedException {

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();

		Response AuthsignUpResponse = pageObj.endpoints().authApiSignUpPositive(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(AuthsignUpResponse.statusCode(), ApiConstants.HTTP_STATUS_CREATED, "user sign up fail");
		// String authToken =
		// AuthsignUpResponse.jsonPath().get("authentication_token").toString();
		String accessToken = AuthsignUpResponse.jsonPath().get("access_token").toString();
		// String userID = AuthsignUpResponse.jsonPath().get("user_id").toString();

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to settings menu
		pageObj.menupage().navigateToSubMenuItem("Settings", "Business");
		// pageObj.menupage().address_tab();
		pageObj.dashboardpage().navigateToTabs("Address");
		pageObj.settingsPage().alternateLanguages();
		pageObj.settingsPage().clickSaveBtn();

		// naviagte to subscription menu
		String spNameFr = "Nom de l'abonnement" + CreateDateTime.getTimeDateString();
		String spNameEn = "SubscriptionPlanName" + CreateDateTime.getTimeDateString();
		String descrpFr = "Descriptionen";
		String missFr = "Miscellaneousen";
		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Plans");
		pageObj.subscriptionPlansPage().createNewSubscriptionPlanInFrench(spNameFr, spNameEn, "Automation QC - 606",
				"Rate Rollback", "400", descrpFr, missFr);
		String PlanID = pageObj.subscriptionPlansPage().getSbuscriptionPlanID(spNameEn);

		endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";
		String spPrice = "750";

		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(accessToken, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		int subscription_id = Integer
				.parseInt(purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString());
		logger.info(userEmail + " purchased " + subscription_id + " Plan id = " + PlanID);
		logger.info("User will be able to purchase the subscription plan successfully using api2");
		utils.logPass("User will be able to purchase the subscription plan successfully using api2");

		// user subscription api2
		Response userSubscriptionResponse = pageObj.endpoints().Api2UserSubscriptionWithLanguage(accessToken,
				dataSet.get("client"), dataSet.get("secret"), "fr");
		Assert.assertEquals(userSubscriptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String subsName1 = userSubscriptionResponse.jsonPath().get("subscriptions[0].name").toString();
		Assert.assertEquals(subsName1, spNameFr);
		logger.info("verified subcription name in French");
		utils.logPass("verified subcription name in French");
		String descrpName1 = userSubscriptionResponse.jsonPath().get("subscriptions[0].description").toString();
		Assert.assertEquals(descrpName1, descrpFr);
		logger.info("verified description name in French");
		utils.logPass("verified description name in French");
		String missceName1 = userSubscriptionResponse.jsonPath().get("subscriptions[0].miscellaneous").toString();
		Assert.assertEquals(missceName1, missFr);
		logger.info("verified miscellaneous name in French");
		utils.logPass("verified miscellaneous name in French");

		// past subscription api
		Response pastSubscriptionresponse = pageObj.endpoints().Api1UserSubscriptionWithPastSubscriptionwithlanguage(
				accessToken, dataSet.get("client"), dataSet.get("secret"), "fr");
		Assert.assertEquals(pastSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String subsName = pastSubscriptionresponse.jsonPath().get("subscriptions[0].name").toString();
		Assert.assertEquals(subsName, spNameFr);
		logger.info("verified subcription name in French");
		utils.logPass("verified subcription name in French");
		String descrpName = pastSubscriptionresponse.jsonPath().get("subscriptions[0].description").toString();
		Assert.assertEquals(descrpName, descrpFr);
		logger.info("verified description name in French");
		utils.logPass("verified description name in French");
		String missceName = pastSubscriptionresponse.jsonPath().get("subscriptions[0].miscellaneous").toString();
		Assert.assertEquals(missceName, missFr);
		logger.info("verified miscellaneous name in French");
		utils.logPass("verified miscellaneous name in French");

	}

	// Rakhi
	@Test(description = "SQ-T5330 Verify the toggle option for \"Make available for purchase?\" in subscription plan"
			+ "SQ-T5327 Verify deactivation of subscription plan", groups = { "regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T5330_verifyFunctionalityOfToggleOption() throws InterruptedException {

		String QCname = "QcSubscription_" + CreateDateTime.getTimeDateString();
		String spName = "SubcriptionPlan_" + CreateDateTime.getTimeDateString();

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to settings menu
		pageObj.menupage().navigateToSubMenuItem("Settings", "Business");
		// pageObj.menupage().address_tab();
		pageObj.dashboardpage().navigateToTabs("Address");
		pageObj.settingsPage().removeAlternateLanguages();
		pageObj.settingsPage().clickSaveBtn();

		// Navigate to Qualification Criteria
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().createQualificationCriteria(QCname, "90", dataSet.get("qcFucntionName"),
				"3", true, dataSet.get("lineItemSelectorName"));

		// naviagte to subscription menu
		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Plans");
		pageObj.subscriptionPlansPage().createSubscriptionPlan(spName, "450", QCname, dataSet.get("qcFucntionName"),
				false, endDateTime, false);
		String PlanID = pageObj.subscriptionPlansPage().getSbuscriptionPlanID(spName);

		endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";
		String spPrice = "750";

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();

		Response AuthsignUpResponse = pageObj.endpoints().authApiSignUpPositive(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(AuthsignUpResponse.statusCode(), ApiConstants.HTTP_STATUS_CREATED, "user sign up fail");
		String accessToken = AuthsignUpResponse.jsonPath().get("access_token").toString();

		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(accessToken, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		int subscription_id = Integer
				.parseInt(purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString());
		logger.info(userEmail + " purchased " + subscription_id + " Plan id = " + PlanID);
		logger.info("User able to purchase the subscription plan successfully using api2");
		utils.logPass("User able to purchase the subscription plan successfully using api2");

		// naviagte to subscription menu
		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Plans");
		pageObj.subscriptionPlansPage().searchAndClickSubscriptionPlan(spName);
		pageObj.subscriptionPlansPage().updateSubscriptionPlanWithMakeAvailableForPurchaseToggle();

		// subscription purchase api 2
		Response purchaseSubscriptionresponse1 = pageObj.endpoints().Api2SubscriptionPurchase(accessToken, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isApi2PurchaseSubscriptionUnavailablePlanSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema3, purchaseSubscriptionresponse1.asString());
		Assert.assertTrue(isApi2PurchaseSubscriptionUnavailablePlanSchemaValidated,
				"API v2 Purchase Subscription Schema Validation failed");
		Assert.assertTrue(purchaseSubscriptionresponse1.asString().contains("Plan is not available for purchase"),
				"User able to purchase the subscription plan successfully");
		logger.info("Plan is not available for purchase.");
		utils.logPass("Plan is not available for purchase.");

		// deactivate subscription
		pageObj.subscriptionPlansPage().inactiveActiveSubscription(spName);
		// check subscription plan status
		pageObj.subscriptionPlansPage().searchSubscriptionPlan(spName);
		Boolean status = pageObj.subscriptionPlansPage().checkSubscriptionPlanStatus(spName, "Inactive");
		Assert.assertTrue(status, "Subscription Plan status did not matched");
		logger.info("Verified successfully deactivated the Subscription Plan : " + spName);
		utils.logit("Verified successfully deactivated the Subscription Plan : " + spName);

	}

	// Rakhi
	@Test(description = "SQ-T5329 Add POS Meta text for subscription plan", groups = { "regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T5329_posMetaForSubscriptionPlan() throws InterruptedException {

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		long phone = (long) (Math.random() * Math.pow(10, 10));

		// naviagte to subscription menu
		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Plans");
		pageObj.subscriptionPlansPage().searchAndClickSubscriptionPlan(dataSet.get("SubscriptionPlan"));

		// add POS meta text with more than 25 characters
		pageObj.subscriptionPlansPage().addPosMetaText("Adding POS meta text for subscription");
		boolean flag = pageObj.subscriptionPlansPage()
				.verifyErrorMsgForPosMeta("POS meta is too long (maximum is 25 characters)");
		Assert.assertTrue(flag, "Error messsage did not verified");
		logger.info("Error message verified for POS meta");
		utils.logPass("Error message verified for POS meta");
		// add POS meta text <= 25 characters
		String text = "Adding POS meta text";
		pageObj.subscriptionPlansPage().addPosMetaText(text);
		pageObj.signupcampaignpage().clickNextBtn();
		utils.longWaitInSeconds(3);
		pageObj.subscriptionPlansPage().clickSubmitButton();

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), phone);
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String phoneNo = signUpResponse.jsonPath().get("user.phone").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// gift subscription plan to user from guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().messageGiftRewardsToUser(dataSet.get("subject"), "Subscription",
				dataSet.get("SubscriptionPlan"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		logger.info("Verified that Success message of subscription send to user ");
		utils.logPass("Verified that Success message of subscription send to user ");

		// validating POS meta text through /api/pos/users/search
		Response response = pageObj.endpoints().authUserSubscriptionMeta(dataSet.get("locationkey"), phoneNo);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String posMetaTxt = response.jsonPath().getString("subscriptions[0].pos_meta");
		Assert.assertEquals(posMetaTxt, text, "POS Meta Text did not verified with the entered one");
		logger.info("Verified POS meta Text is same as enterend in the subscription plan");
		utils.logit("Verified POS meta Text is same as enterend in the subscription plan");

	}

	// Rakhi
	@Test(description = "SQ-T5323 Verify that active subscriptions plans are gifted to the user and the active subscribers count is updated in the API - api/auth/subscriptions"
			+ "SQ-T5322 Verify that active subscriptions plans are gifted to the user and the active subscribers count is updated in the API - api2/mobile/subscriptions", groups = {
					"regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T5323_verifyActiveSubscriptionsPlansGiftedToTheUser() throws InterruptedException {

		String QCname = "QcSubscription_" + CreateDateTime.getTimeDateString();
		String spName = "SubcriptionPlan_" + CreateDateTime.getTimeDateString();
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to settings menu
		pageObj.menupage().navigateToSubMenuItem("Settings", "Business");
		pageObj.dashboardpage().navigateToTabs("Address");
		pageObj.settingsPage().removeAlternateLanguages();
		pageObj.settingsPage().clickSaveBtn();

		// Navigate to Qualification Criteria
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().createQualificationCriteria(QCname, "90", dataSet.get("qcFucntionName"),
				"3", true, dataSet.get("lineItemSelectorName"));

		// naviagte to subscription menu
		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Plans");
		pageObj.subscriptionPlansPage().createSubscriptionPlan(spName, "450", QCname, dataSet.get("qcFucntionName"),
				false, endDateTime, false);
		String PlanID = pageObj.subscriptionPlansPage().getSbuscriptionPlanID(spName);

		endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";
		String spPrice = "750";

		// sign up a user
		Response AuthsignUpResponse = pageObj.endpoints().authApiSignUpPositive(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(AuthsignUpResponse.statusCode(), ApiConstants.HTTP_STATUS_CREATED, "user sign up fail");
		String authToken = AuthsignUpResponse.jsonPath().get("authentication_token").toString();
		String accessToken = AuthsignUpResponse.jsonPath().get("access_token").toString();

		// gift subscription plan to user from guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().messageGiftRewardsToUser(dataSet.get("subject"), "Subscription", spName,
				dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		logger.info("Verified that Success message of subscription send to user ");
		utils.logPass("Verified that Success message of subscription send to user ");

		// subscription purchase using auth api
		String startDateTime = CreateDateTime.getYesterdaysDate() + " 01:00:00";
		Response authSubscriptionresponse2 = pageObj.endpoints().authApiSubscriptionPurchase(authToken, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, startDateTime, endDateTime, "false");
		Assert.assertEquals(authSubscriptionresponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		logger.info("User will be able to purchase the subscription plan successfully using api");
		utils.logPass("User will be able to purchase the subscription plan successfully using api");

		// subscription purchase api 2
		Response purchaseSubscriptionresponse2 = pageObj.endpoints().Api2SubscriptionPurchaseAutorenewal(accessToken,
				PlanID, dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime, "false");
		Assert.assertEquals(purchaseSubscriptionresponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		logger.info("User will be able to purchase the subscription plan successfully using api2");
		utils.logPass("User will be able to purchase the subscription plan successfully using api2");

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
