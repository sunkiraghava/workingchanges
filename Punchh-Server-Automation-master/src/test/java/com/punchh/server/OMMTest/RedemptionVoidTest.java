package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.parser.ParseException;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
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

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class RedemptionVoidTest {

	private static Logger logger = LogManager.getLogger(RedemptionVoidTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String iFrameEmail;
	private String sTCName;
	private String baseUrl;
	private String env, run = "ui";
	private static Map<String, String> dataSet;

	Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {

		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		utils = new Utilities(driver);
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run , env , "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
	}

	@SuppressWarnings("unused")
	@Test(description = "SQ-T2239 Verify the Redemption through Online Order",groups = { "regression", "dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2239_verifyRedemptionthroughOnlineOrder() throws InterruptedException, ParseException {

		// user creation using auth signup api
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, signUpResponse.getStatusCode(),
				"Status code 201 did not matched for auth user signup api");
		String authentication_token = signUpResponse.jsonPath().get("authentication_token").toString();
        String userID = signUpResponse.jsonPath().get("user_id").toString();
        String token = signUpResponse.jsonPath().get("access_token").toString();
		logger.info(authentication_token);

		// Instance login and goto timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

        // send reward amount to user Reedemable
        Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID,
            dataSet.get("apiKey"), "20", dataSet.get("redeemable_id"), "", "");

        utils.logit("Send redeemable to the user successfully");
        Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
            "Status code 201 did not matched for api2 send message to user");
        utils.logit("Api2  send reward amount to user is successful");

        // get reward id
        String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"),
            dataSet.get("secret"), dataSet.get("redeemable_id"));
        Assert.assertNotEquals(rewardId, null, "Reward Id is null");
        utils.logit("Reward id " + rewardId + " is generated successfully ");

		// fetch user offers using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, offerResponse.getStatusCode(), "Status code 200 did not matched for api2 user offers");
		utils.waitTillPagePaceDone();
		String reward_id = utils.getRewardIdFromJsonArray(offerResponse, "rewards", "name", dataSet.get("redeemable"),
				"reward_id");
		logger.info(reward_id);

		// authApi customer signin
		Response signInResponse = pageObj.endpoints().authApiUserLogin(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		int code = signInResponse.getStatusCode();
		logger.info(code);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, signInResponse.getStatusCode(),
				"Status code 201 did not matched for auth user login api");

		// Auth_redemption_online_order
		Response resp = pageObj.endpoints().posRedemptionOfRewardIdAuthOnlineOrder(authentication_token, reward_id,
				dataSet.get("secret"), dataSet.get("client"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		pageObj.guestTimelinePage().refreshTimeline();
		Thread.sleep(5000);

		// Validate timeline for redemption and receipt
		String discountValueWebCheckin = pageObj.guestTimelinePage().getDiscountValueWebCheckinDetails();
		String totalamoutnWebCheckin = pageObj.guestTimelinePage().getTotalAmountWebCheckin();

		// verify gift reward in account history
		Thread.sleep(2000);
		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> Itemdata = pageObj.accounthistoryPage().getAccountDetailsforItemRedeemed();
		System.out.println(Itemdata);
		Assert.assertTrue(Itemdata.get(0).contains("Item Redeemed"), "Redemption did not redeemed in account history");
		Assert.assertTrue(Itemdata.get(0).contains("$2.0 OFF redeemed"),
				"Redemption did not redeemed in account history");
		// decreased in account balance");
		utils.logPass("Redemption of reward is validated in acount history");
	}

    @Test(description = "SQ-T2275 Verify the Void Redemption", groups = {"regression", "dailyrun"},
        priority = 1)
	@Owner(name = "Amit Kumar")
	public void T2275_verifytheVoidRedemption() throws InterruptedException {

		String reward_Code = "";
		int redemption_id;
		// User Signip using mobile api 2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Instance login and goto timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		// click message gift and gift reward to user
		pageObj.guestTimelinePage().messageGiftToUser(dataSet.get("subject"), dataSet.get("reward"),
				dataSet.get("redeemable"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		String rewardName = pageObj.guestTimelinePage().getRewardName();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		Assert.assertEquals(rewardName, "Rewarded $2.0 OFF");
		// iFrame Login and redeem reward by generating code
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLogin(iFrameEmail);
		reward_Code = pageObj.iframeSingUpPage().redeemRewardOffer(dataSet.get("rewardName"));
		// Pos redemption api
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response resp = pageObj.endpoints().posRedemptionOfCode(iFrameEmail, date, reward_Code, key, txn,
				dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos redemption api");
		JsonPath js = resp.jsonPath();
		redemption_id = js.get("redemption_id");

		// Instance login and goto timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		// verify gift reward in account history
		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> Itemdata = pageObj.accounthistoryPage().getAccountDetailsforItemRedeemed();
		System.out.println(Itemdata);
		Assert.assertTrue(Itemdata.get(0).contains("Item Redeemed"), "Redemption did not appeared in account history");
		Assert.assertTrue(Itemdata.get(0).contains("$2.0 OFF redeemed"),
				"Redemption did not appeared in account history");
		utils.logPass("Redemption of reward is validated in acount history");

		// Void redemption using api
		Response respo = pageObj.endpoints().posVoidRedemption(iFrameEmail, Integer.toString(redemption_id),
				dataSet.get("locationKey"));
		Assert.assertEquals(202, respo.getStatusCode(), "Status code 200 did not matched for pos redemption api");

		// verify gift reward in account history
		pageObj.guestTimelinePage().clickAccountHistory();
		Thread.sleep(5000);
		pageObj.guestTimelinePage().refreshTimeline();
		// Item redeemed entry must be deleted from history
		List<String> voiddata = pageObj.accounthistoryPage().getAccountDetailsforItemRedeemed();
		System.out.println(voiddata);
		Assert.assertTrue(voiddata.size() == 0, "void redemption did not appeared in account history");
		utils.logPass("Void redemption of reward is validated in acount history");
	}

	@Test(description = "SQ-T4309 Validate that user is able to select 'Redeemable attributes' upto 255 characters only)",groups = { "regression", "dailyrun" }, priority = 2)
	@Owner(name = "Hardik Bhardwaj")
	public void T4309_RedeemableAttributes() throws InterruptedException {
		String redeemableName = "AutomationRedeemable" + CreateDateTime.getTimeDateString();
		String redeemableName1 = "AutomationRedeemable_Test" + CreateDateTime.getTimeDateString();

		String RedeemableAttributes255 = dataSet.get("RedeemableAttributes255");
		String RedeemableAttributes258 = dataSet.get("RedeemableAttributes258");
		String RedeemableAttributes10 = dataSet.get("RedeemableAttributes10");
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab -> Redemption Display -> Redeemable
		// Attributes
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().enterRedeemableAttributes("clear all", RedeemableAttributes255);
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().enterRedeemableAttributes("", RedeemableAttributes258);
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().enterRedeemableAttributes("", RedeemableAttributes10);

		// navigate to setting -> redeemables
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().createRedeemable(redeemableName);
		pageObj.redeemablePage().selectRecieptRule("1");
		pageObj.redeemablePage().allowRedeemableToRunIndefinitely();
		pageObj.dashboardpage().checkUncheckToggle("Additional Options", "ON");
		pageObj.redeemablePage().attributesApplicableOnThisRedeemable(RedeemableAttributes258);
		pageObj.redeemablePage().clickOnFinishButton();

		// verify Applicable On This Redeemable has more than 255 character
		boolean flag = pageObj.redeemablePage()
				.successOrErrorConfirmationMessage("Redeemable properties is too long (maximum is 255 characters)");
		Assert.assertTrue(flag, "Redeemable properties is too long (maximum is 255 characters) error is not visible");
		utils.logPass("error is verified Redeemable properties is too long (maximum is 255 characters) for Redeemable");

		pageObj.redeemablePage().attributesApplicableOnThisRedeemable(RedeemableAttributes258);
		pageObj.redeemablePage().attributesApplicableOnThisRedeemable(RedeemableAttributes255);
		pageObj.redeemablePage().clickOnFinishButton();

		// verify Applicable On This Redeemable has 255 character
		boolean flag1 = pageObj.guestTimelinePage().successOrErrorConfirmationMessage("Redeemable successfully saved.");
		Assert.assertTrue(flag1, "Redeemable properties is more than 255 characters ");
		utils.logPass("Redeemable properties is 255 characters long");

		pageObj.redeemablePage().searchRedeemable(redeemableName);
		pageObj.redeemablePage().deleteRedeemable(redeemableName);

		pageObj.redeemablePage().createRedeemable(redeemableName1);
		pageObj.redeemablePage().selectRecieptRule("1");
		pageObj.redeemablePage().allowRedeemableToRunIndefinitely();
		pageObj.dashboardpage().checkUncheckToggle("Additional Options", "ON");
		pageObj.redeemablePage().attributesApplicableOnThisRedeemable(RedeemableAttributes10);
		pageObj.redeemablePage().clickOnFinishButton();

		// verify Applicable On This Redeemable has 10 character
		boolean flag2 = pageObj.guestTimelinePage().successOrErrorConfirmationMessage("Redeemable successfully saved.");
		Assert.assertTrue(flag2, "Redeemable has some error");
		utils.logPass("Redeemable properties is 10 characters long");

		pageObj.redeemablePage().searchRedeemable(redeemableName1);
		pageObj.redeemablePage().deleteRedeemable(redeemableName1);
	}

	// Rakhi
	@Test(description = "SQ-T5412 [Point Unlock Staged]Verify force redemption should not be void & also validate pos/auth void redemption api response", priority = 14)
	@Owner(name = "Rakhi Rawat")
	public void T5412_verifyVoidForceRedemption() throws Exception {
		String b_id = dataSet.get("business_id");

		// User signup using API1 Signup
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse1.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse1.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse1.jsonPath().get("user_id").toString();
		utils.logPass("API v1 user signup is successful");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		logger.info("-----Void Redemption enabling check_eligibility_of_void_redemption flag----");
		// enable flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"),
				b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		// Pos api checkin of 100 points
		Response resp1 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), "100");
		Assert.assertEquals(resp1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for post chekin api");

		// force redemption of 10 points
		Response forceRedeem_Response = pageObj.endpoints().pointForceRedeem(dataSet.get("apiKey"), userID, "10");
		String redemptionId = forceRedeem_Response.jsonPath().get("redemption_id").toString();
		String redemptionTrackingCode = forceRedeem_Response.jsonPath().get("redemption_tracking_code");
		Assert.assertEquals(forceRedeem_Response.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for force Redemption of Points");

		// verify gift reward in account history
		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> Itemdata = pageObj.accounthistoryPage().getAccountDetailsforRewardRedeemed();
		System.out.println(Itemdata);
		Assert.assertTrue(Itemdata.get(0).contains("Rewards Redeemed"),
				"Redemption did not appeared in account history");
		Assert.assertTrue(Itemdata.get(0).contains(redemptionTrackingCode),
				"Redemption did not appeared in account history");
		utils.logPass("Redemption of reward is validated in acount history");

		// Void force redemption
		Response voidResponse = pageObj.endpoints().posVoidRedemption(userEmail, redemptionId,
				dataSet.get("locationkey"));
		Assert.assertTrue(voidResponse.asString().contains("Force Redemption cannot be voided"),
				"Void Force redemption is successful");

		// click message gift and gift reward to user
		pageObj.guestTimelinePage().messageGiftToUser(dataSet.get("subject"), dataSet.get("reward"),
				dataSet.get("redeemable"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		String rewardName = pageObj.guestTimelinePage().getRewardName();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		Assert.assertEquals(rewardName, "Rewarded $2.0 OFF");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offer_Response = pageObj.endpoints().getUserOffers(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, offer_Response.getStatusCode(),
				"Status code 200 did not matched for api2 user offers");
		String rewardId = offer_Response.jsonPath().get("rewards[0].reward_id").toString();
		utils.logit("Api2 user fetch user offers is successful: " + rewardId);

		// Force Redeem of offer
		Response forceRedeemResponse3 = pageObj.endpoints().forceRedeem(dataSet.get("apiKey"), rewardId, userID);
		String redemptionId3 = forceRedeemResponse3.jsonPath().get("redemption_id").toString();
		String redemptionTrackingCode1 = forceRedeemResponse3.jsonPath().get("redemption_tracking_code");
		Assert.assertEquals(forceRedeemResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for Platform Api Force Redeem");
		utils.logPass("PLATFORM FUNCTIONS API Force Redeem is successful");

		// verify gift reward in account history
		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> Offerdata = pageObj.accounthistoryPage().getAccountDetailsforItemRedeemed();
		System.out.println(Offerdata);
		Assert.assertTrue(Offerdata.get(0).contains("Item Redeemed"), "Redemption did not appeared in account history");
		Assert.assertTrue(Offerdata.get(0).contains(redemptionTrackingCode1),
				"Redemption did not appeared in account history");
		utils.logPass("Redemption of reward is validated in acount history");

		// Void offer redemption
		Response voidResponse3 = pageObj.endpoints().posVoidRedemption(userEmail, redemptionId3,
				dataSet.get("locationkey"));
		Assert.assertTrue(voidResponse3.asString().contains("Force Redemption cannot be voided"),
				"Void Force redemption is successful");

		logger.info("-----Void Redemption disabling check_eligibility_of_void_redemption flag-----");

		// disable the flag from db
		query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		flag = DBUtils.updateBusinessesPreference(env, expColValue, "false",
				dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to false");
		utils.logit(dataSet.get("dbFlag") + " value is updated to false");

		// Void force redemption
		Response voidResponse4 = pageObj.endpoints().posVoidRedemption(userEmail, redemptionId,
				dataSet.get("locationkey"));
		Assert.assertEquals(202, voidResponse4.getStatusCode(),
				"Status code 202 did not matched for pos redemption api");

		pageObj.guestTimelinePage().refreshTimeline();
		// Item redeemed entry must be deleted from history
		List<String> voiddata1 = pageObj.accounthistoryPage().getAccountDetailsforRewardRedeemed();
		System.out.println(voiddata1);
		Assert.assertTrue(voiddata1.size() == 0, "void redemption did not appeared in account history");
		utils.logPass("Void redemption of reward is validated in acount history");

		// Void auth force offer redemption
		Response voidResponse6 = pageObj.endpoints().authApiVoidRedemptions(authToken, dataSet.get("client"),
				dataSet.get("secret"), redemptionId3);
		Assert.assertEquals(voidResponse6.getStatusCode(), 202);

		pageObj.guestTimelinePage().refreshTimeline();
		// Item redeemed entry must be deleted from history
		List<String> voiddata2 = pageObj.accounthistoryPage().getAccountDetailsforItemRedeemed();
		System.out.println(voiddata2);
		Assert.assertTrue(voiddata2.size() == 0, "void redemption did not appeared in account history");
		utils.logPass("Void redemption of reward is validated in acount history");

	}

	// Rakhi
	@Test(description = "SQ-T5409 Verify void reward redemption either associated checkin is expired or not", priority = 15)
	@Owner(name = "Rakhi Rawat")
	public void T5409_verifyVoidRewardRedemptionAssociatedCheckin() throws InterruptedException {

		// login to business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// set redemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// User signup using API1 Signup
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse1.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse1.jsonPath().get("authentication_token").toString();
		utils.logPass("API v1 user #1 signup is successful");

		// Pos api checkin
		Response resp = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for post chekin api");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(offerResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		utils.logit("Api2 user fetch user offers is successful: " + reward_id);

		// Create Redemption using "reward_id" (fetch redemption code)
		Response redemptionResponse = pageObj.endpoints().Api2RedemptionWithRewardId(dataSet.get("client"),
				dataSet.get("secret"), token, reward_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, redemptionResponse.getStatusCode(),
				"Status code 201 did not matched for api2 create redemption using reward_id");
		utils.logPass("Api2 Create Redemption using reward_id is successful");
		String redemption_code = redemptionResponse.jsonPath().get("redemption_tracking_code").toString();
		String redemptionId = redemptionResponse.jsonPath().get("redemption_id").toString();

		// verify gift reward in account history
		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> Itemdata = pageObj.accounthistoryPage().getAccountDetailsforItemRedeemed();
		System.out.println(Itemdata);
		Assert.assertTrue(Itemdata.get(0).contains("Item Redeemed"), "Redemption did not appeared in account history");
		Assert.assertTrue(Itemdata.get(0).contains(redemption_code), "Redemption did not appeared in account history");
		utils.logPass("Redemption of reward is validated in acount history");

		// Void redemption using API
		Response voidResponse = pageObj.endpoints().posVoidRedemption(userEmail, redemptionId,
				dataSet.get("locationkey"));
		voidResponse.then().log().all();
		Assert.assertEquals(202, voidResponse.getStatusCode(),
				"Status code 200 did not matched for pos redemption api");

		// verify gift reward in account history
		pageObj.guestTimelinePage().refreshTimeline();
		List<String> voidData = pageObj.accounthistoryPage().getAccountDetailsforItemRedeemed();
		Assert.assertTrue(voidData.size() == 0, "void redemption did not appeared in account history");
		utils.logPass("Void redemption of reward is validated in acount history");

		// Pos api checkin of 120 points
		Response response = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for post chekin api");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse1 = pageObj.endpoints().getUserOffers(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(offerResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 user offers");
		String reward_id1 = offerResponse1.jsonPath().get("rewards[0].reward_id").toString();
		utils.logit("Api2 user fetch user offers is successful: " + reward_id1);

		// Create Redemption using "reward_id" (fetch redemption code)
		Response redemptionResponse1 = pageObj.endpoints().Api2RedemptionWithRewardId(dataSet.get("client"),
				dataSet.get("secret"), token, reward_id1);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, redemptionResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 create redemption using reward_id");
		utils.logPass("Api2 Create Redemption using reward_id is successful");
		String redemption_code1 = redemptionResponse1.jsonPath().get("redemption_tracking_code").toString();
		String redemptionId1 = redemptionResponse1.jsonPath().get("redemption_id").toString();

		// verify gift reward in account history
		pageObj.guestTimelinePage().refreshTimeline();
		List<String> Itemdata1 = pageObj.accounthistoryPage().getAccountDetailsforItemRedeemed();
		System.out.println(Itemdata1);
		Assert.assertTrue(Itemdata1.get(0).contains("Item Redeemed"), "Redemption did not appeared in account history");
		Assert.assertTrue(Itemdata1.get(0).contains(redemption_code1),
				"Redemption did not appeared in account history");
		utils.logPass("Redemption of reward is validated in acount history");

		// Run rolling expiry
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Rolling Expiry Schedule");
		pageObj.schedulespage().runSchedule();

		// Auth void redemption
		Response voidRedemptionResponse = pageObj.endpoints().authApiVoidRedemptions(authToken, dataSet.get("client"),
				dataSet.get("secret"), redemptionId1);
		Assert.assertEquals(voidRedemptionResponse.getStatusCode(), 202);

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().clickAccountHistory();
		// Item redeemed entry must be deleted from history
		List<String> voiddata = pageObj.accounthistoryPage().getAccountDetailsforItemRedeemed();
		System.out.println(voiddata);
		Assert.assertTrue(voiddata.size() == 0, "void redemption did not appeared in account history");
		utils.logPass("Void redemption of reward is validated in acount history");
	}

	// Rakhi
	@Test(description = "SQ-T5414 [Point to Rewards]Verify force redemption should not be void & also validate pos/auth void redemption api response", priority = 16)
	@Owner(name = "Rakhi Rawat")
	public void T5414_verifyVoidForceRedemption() throws Exception {
		String b_id = dataSet.get("business_id");

		// User signup using API1 Signup
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse1.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse1.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse1.jsonPath().get("user_id").toString();
		utils.logPass("API v1 user signup is successful");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		logger.info("-----Void Redemption enabling check_eligibility_of_void_redemption flag----");
		// enable flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"),
				b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		// Pos api checkin of 110 points
		Response resp1 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), "110");
		Assert.assertEquals(resp1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for post chekin api");

		// force redemption of 10 points
		Response forceRedeem_Response = pageObj.endpoints().pointForceRedeemWithType(dataSet.get("apiKey"), userID,
				"10", "unbanked_points_redemption");
		String redemptionId = forceRedeem_Response.jsonPath().get("redemption_id").toString();
		String redemptionTrackingCode = forceRedeem_Response.jsonPath().get("redemption_tracking_code");
		Assert.assertEquals(forceRedeem_Response.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for force Redemption of Points");

		// verify gift reward in account history
		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> Itemdata = pageObj.accounthistoryPage().getAccountDetailsforRewardRedeemed();
		System.out.println(Itemdata);
		Assert.assertTrue(Itemdata.get(0).contains("Rewards Redeemed"),
				"Redemption did not appeared in account history");
		Assert.assertTrue(Itemdata.get(0).contains(redemptionTrackingCode),
				"Redemption did not appeared in account history");
		utils.logPass("Redemption of reward is validated in acount history");

		// Void force redemption
		Response voidResponse = pageObj.endpoints().posVoidRedemption(userEmail, redemptionId,
				dataSet.get("locationkey"));
		Assert.assertTrue(voidResponse.asString().contains("Force Redemption cannot be voided"),
				"Void Force redemption is successful");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, offerResponse.getStatusCode(), "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		utils.logit("Api2 user fetch user offers is successful: " + reward_id);

		// Force loyalty reward Redeem
		Response forceRedeemResponse2 = pageObj.endpoints().forceRedeem(dataSet.get("apiKey"), reward_id, userID);
		String redemptionId2 = forceRedeemResponse2.jsonPath().get("redemption_id").toString();
		String redemptionTrackingCode1 = forceRedeemResponse2.jsonPath().get("redemption_tracking_code");
		Assert.assertEquals(forceRedeemResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for Platform Api Force Redeem");
		utils.logPass("PLATFORM FUNCTIONS API Force Redeem is successful");

		// verify gift reward in account history
		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> Itemdata1 = pageObj.accounthistoryPage().getAccountDetailsforItemRedeemed();
		System.out.println(Itemdata1);
		Assert.assertTrue(Itemdata1.get(0).contains("Item Redeemed"), "Redemption did not appeared in account history");
		Assert.assertTrue(Itemdata1.get(0).contains(redemptionTrackingCode1),
				"Redemption did not appeared in account history");
		utils.logPass("Redemption of reward is validated in acount history");

		// Void force loyalty reward redemption
		Response voidResponse2 = pageObj.endpoints().posVoidRedemption(userEmail, redemptionId2,
				dataSet.get("locationkey"));
		Assert.assertTrue(voidResponse2.asString().contains("Force Redemption cannot be voided"),
				"Void Force redemption is successful");

		// click message gift and gift reward to user
		pageObj.guestTimelinePage().messageGiftToUser(dataSet.get("subject"), dataSet.get("reward"),
				dataSet.get("redeemable"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		String rewardName = pageObj.guestTimelinePage().getRewardName();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		Assert.assertEquals(rewardName, "Rewarded $2.0 OFF");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offer_Response = pageObj.endpoints().getUserOffers(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, offer_Response.getStatusCode(),
				"Status code 200 did not matched for api2 user offers");
		String rewardId = offer_Response.jsonPath().get("rewards[0].reward_id").toString();
		utils.logit("Api2 user fetch user offers is successful: " + rewardId);

		// Force Redeem of offer
		Response forceRedeemResponse3 = pageObj.endpoints().forceRedeem(dataSet.get("apiKey"), rewardId, userID);
		String redemptionId3 = forceRedeemResponse3.jsonPath().get("redemption_id").toString();
		String redemptionTrackingCode2 = forceRedeemResponse3.jsonPath().get("redemption_tracking_code");
		Assert.assertEquals(forceRedeemResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for Platform Api Force Redeem");
		utils.logPass("PLATFORM FUNCTIONS API Force Redeem is successful");

		// verify gift reward in account history
		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> Offerdata = pageObj.accounthistoryPage().getAccountDetailsforItemRedeemed();
		System.out.println(Offerdata);
		Assert.assertTrue(Offerdata.get(0).contains("Item Redeemed"), "Redemption did not appeared in account history");
		Assert.assertTrue(Offerdata.get(0).contains(redemptionTrackingCode2),
				"Redemption did not appeared in account history");
		utils.logPass("Redemption of reward is validated in acount history");

		// Void offer redemption
		Response voidResponse3 = pageObj.endpoints().posVoidRedemption(userEmail, redemptionId3,
				dataSet.get("locationkey"));
		Assert.assertTrue(voidResponse3.asString().contains("Force Redemption cannot be voided"),
				"Void Force redemption is successful");

		logger.info("-----Void Redemption disabling check_eligibility_of_void_redemption flag-----");

		// disable the flag from db
		query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		flag = DBUtils.updateBusinessesPreference(env, expColValue, "false",
				dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to false");
		utils.logit(dataSet.get("dbFlag") + " value is updated to false");

		// Void force redemption
		Response voidResponse4 = pageObj.endpoints().posVoidRedemption(userEmail, redemptionId,
				dataSet.get("locationkey"));
		Assert.assertEquals(202, voidResponse4.getStatusCode(),
				"Status code 202 did not matched for pos redemption api");

		pageObj.guestTimelinePage().refreshTimeline();
		// Item redeemed entry must be deleted from history
		List<String> voiddata1 = pageObj.accounthistoryPage().getAccountDetailsforRewardRedeemed();
		System.out.println(voiddata1);
		Assert.assertTrue(voiddata1.size() == 0, "void redemption did not appeared in account history");
		utils.logPass("Void redemption of reward is validated in acount history");

		// Void force loyalty reward redemption
		Response voidResponse5 = pageObj.endpoints().posVoidRedemption(userEmail, redemptionId2,
				dataSet.get("locationkey"));
		Assert.assertEquals(202, voidResponse5.getStatusCode(),
				"Status code 200 did not matched for pos redemption api");

		pageObj.guestTimelinePage().refreshTimeline();
		// Item redeemed entry must be deleted from history
		List<String> voiddata2 = pageObj.accounthistoryPage().getAccountDetailsforItemRedeemed();
		System.out.println(voiddata2);
		// Assert.assertTrue(voiddata2.size() == 0, "void redemption did not appeared in
		// account history");
		Assert.assertFalse(voiddata2.get(0).contains(redemptionTrackingCode1),
				"void redemption did not appeared in account history");
		utils.logPass("Void redemption of reward is validated in acount history");

		// Auth Void force offer redemption
		Response voidResponse6 = pageObj.endpoints().authApiVoidRedemptions(authToken, dataSet.get("client"),
				dataSet.get("secret"), redemptionId3);
		Assert.assertEquals(voidResponse6.getStatusCode(), 202);

		pageObj.guestTimelinePage().refreshTimeline();
		// Item redeemed entry must be deleted from history
		List<String> voiddata3 = pageObj.accounthistoryPage().getAccountDetailsforItemRedeemed();
		System.out.println(voiddata3);
		Assert.assertTrue(voiddata3.size() == 0, "void redemption did not appeared in account history");
		utils.logPass("Void redemption of reward is validated in acount history");

	}
	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
