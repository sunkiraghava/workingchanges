package com.punchh.server.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import org.testng.asserts.SoftAssert;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class OffersEnd2EndScenarioTest {

	private static Logger logger = LogManager.getLogger(OffersEnd2EndScenarioTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	String password, birthday, anniversary, first_name, last_name, item_qty, item_type, item_family, item_group;
	String blankData;
	private Utilities utils;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// Single login to instance
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
		password = Utilities.getApiConfigProperty("password");
		birthday = Utilities.getApiConfigProperty("birthday");
		anniversary = Utilities.getApiConfigProperty("anniversary");
		first_name = "first_name" + CreateDateTime.getTimeDateString();
		last_name = "last_name" + CreateDateTime.getTimeDateString();
		item_qty = "1";
		item_type = "M";
		item_family = "10";
		item_group = "999";
		blankData = "";
		utils = new Utilities(driver);
		// Move to All Businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T3550 QC created -> QC attached to a redeemable -> redeemable configured in membership level -> user receives reward on account bump -> processes the redemption via Online Order", groups = { "regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T3550_End2EndScenario_PartOne() throws InterruptedException {

		// getting data from test data file
		String item_id = dataSet.get("item_id");
		String redeemableName = dataSet.get("redeemableName");
		logger.info("redeemable Name = " + redeemableName);
		utils.logit("redeemable Name = " + redeemableName);
		String redeemableID = dataSet.get("redeemableID");
		logger.info("redeemable ID = " + redeemableID);
		utils.logit("redeemable ID = " + redeemableID);

		// login to instance
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to membership
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		pageObj.settingsPage().membershipSettingUpdateOrClear("Boosted", "update", redeemableName, "121");

		// user creation
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), password, password, birthday, anniversary, blankData, blankData, first_name,
				last_name);

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, signUpResponse.getStatusCode(),
				"Status code 201 did not matched for auth user signup api");
		String authentication_token = signUpResponse.jsonPath().get("authentication_token").toString();
		String token = signUpResponse.jsonPath().get("access_token").toString();
		logger.info("AUTH Api user signup is successful");
		utils.logPass("AUTH Api user signup is successful");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID);

		logger.info("Reward id " + rewardId + " is generated successfully ");
		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", item_qty, "11", item_type, item_family,
				item_group, "1", item_id); // string

		parentMap.put("Pizza1", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Pizza2", item_qty, "4", item_type, item_family,
				item_group, "2", item_id);

		parentMap.put("Pizza2", detailsMap2);

		Response redemptionResponse = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"),
				dataSet.get("secret"), authentication_token, rewardId, "15", parentMap);
		pageObj.apiUtils().verifyResponse(redemptionResponse, "Auth API user signup");
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		utils.logPass("AUTH online redemption Api is successful");
		logger.info("AUTH online redemption Api is successful");
		String redemptionAmount = redemptionResponse.jsonPath().get("redemption_amount").toString();
		Assert.assertEquals("9.0", redemptionAmount, "Redemption amount is not matching");
		logger.info("Expected redemption amount " + redemptionAmount + " is same as Actual redemption amount i.e. 9");
		utils.logPass("Expected redemption amount " + redemptionAmount + " is same as Actual redemption amount i.e. 9");

		// Remove membership reward
		pageObj.settingsPage().membershipSettingUpdateOrClear("Boosted", "remove", "Base Redeemable", "121");
	}

	@Test(description = "SQ-T3546 QC created -> QC attached to a redeemable -> Post checkin campaign triggered with gift_type redeemable -> user gets the reward processed via Auth online_order -> verify the redemption_amount user gets", groups = { "regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T3546_End2EndScenario_PartTwo() throws Exception {

		// getting data from test data file
		String item_id = dataSet.get("item_id");
		String redeemableName = dataSet.get("redeemableName");
		logger.info("redeemable Name = " + redeemableName);
		utils.logit("redeemable Name = " + redeemableName);
		String redeemableID = dataSet.get("redeemableID");
		logger.info("redeemable ID = " + redeemableID);
		utils.logit("redeemable ID = " + redeemableID);

		// login to instance
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		String postCheckinCampaignName = CreateDateTime.getUniqueString("Automation Postcheckin Campaign");
		// pageObj.menupage().clickCampaignsMenu();
		// pageObj.menupage().clickCampaignsLink();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsPostcheckinCampaign(postCheckinCampaignName,
				dataSet.get("giftType"), dataSet.get("giftReason"), redeemableName);
		pageObj.signupcampaignpage().createWhomDetailsPostcheckinCampaign(dataSet.get("pushNotification"),
				dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Post Checkin Campaign is not created...");

		String last_name = "last_name" + CreateDateTime.getTimeDateString();
		String first_name = "first_name" + CreateDateTime.getTimeDateString();
		// user creation
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), password, password, birthday, anniversary, blankData, blankData, first_name,
				last_name);

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, signUpResponse.getStatusCode(),
				"Status code 201 did not matched for auth user signup api");
		String authentication_token = signUpResponse.jsonPath().get("authentication_token").toString();
		String token = signUpResponse.jsonPath().get("access_token").toString();
		logger.info("AUTH Api user signup is successful");
		utils.logPass("AUTH Api user signup is successful");

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, Map<String, String>> parentMap2 = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", item_qty, "1", item_type, item_family,
				item_group, "1", item_id);
		parentMap.put("Pizza1", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Pizza2", item_qty, "10", item_type, item_family,
				item_group, "2", item_id);
		parentMap.put("Pizza2", detailsMap2);

		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("Pizza3", item_qty, "10", item_type, item_family,
				item_group, "3", item_id);
		parentMap.put("Pizza3", detailsMap3);

		// Pos api checkin
		String external_uid = CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp = pageObj.endpoints().posCheckinN_QC(dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("locationKey"), "21", userEmail, date, external_uid, "21", parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");
		utils.logPass("POS checkin Api is successful");
		logger.info("POS checkinn Api is successful");

		// navigate to user timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// timeline validation
		boolean campaignNameStaus = pageObj.guestTimelinePage()
				.verifyIsCampaignExistOnTimeLine(postCheckinCampaignName);
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotificationMassCampaign();
		SoftAssert softassert = new SoftAssert();
		softassert.assertTrue(campaignNameStaus, "Campaign name did not matched");
		softassert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");

		// QC list for API validation
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", item_qty, "10", item_type, item_family,
				item_group, "1", item_id);
		parentMap2.put("Pizza1", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Pizza2", item_qty, "4", item_type, item_family,
				item_group, "2", item_id);
		parentMap2.put("Pizza2", detailsMap2);

		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID);

		logger.info("Reward id " + rewardId + " is generated successfully ");
		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		Response redemptionResponse = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"),
				dataSet.get("secret"), authentication_token, rewardId, "14", parentMap2);
		pageObj.apiUtils().verifyResponse(redemptionResponse, "Auth API user signup");
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		utils.logPass("AUTH online redemption Api is successful");
		logger.info("AUTH online redemption Api is successful");
		String redemptionAmount = redemptionResponse.jsonPath().get("redemption_amount").toString();
		String redemptioncode = redemptionResponse.jsonPath().get("redemption_code").toString();
		Assert.assertEquals("0.7", redemptionAmount, "Redemption amount is not matching");
		logger.info("Expected redemption amount " + redemptionAmount + " is same as Actual redemption amount i.e. 0.7");
		utils.logPass(
				"Expected redemption amount " + redemptionAmount + " is same as Actual redemption amount i.e. 0.7");

		pageObj.guestTimelinePage().refreshTimeline();
		utils.longWaitInSeconds(5);
		String redeemedRedemption = pageObj.guestTimelinePage().redeemedRedemption(redemptioncode);
		Assert.assertEquals(redemptioncode, redeemedRedemption, "Redemption amount is not matching");
		logger.info("Expected redemption code " + redeemedRedemption + " is same as Actual redemption code i.e. "
				+ redemptioncode + " . Verified from Timeline");
		utils.logPass("Expected redemption code " + redeemedRedemption
				+ " is same as Actual redemption code i.e. " + redemptioncode + " . Verified from Timeline");

		pageObj.utils().deleteCampaignFromDb(postCheckinCampaignName, env);

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