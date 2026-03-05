package com.punchh.server.loyalty2;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.OfferIngestionUtilityClass.OfferIngestionUtilities;
import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class CockpitsTabFunctionalityValidationsTest {
	static Logger logger = LogManager.getLogger(CockpitsTabFunctionalityValidationsTest.class);
	public WebDriver driver;
	String userEmail;
	String email = "autoemailTemp@punchh.com";
	ApiUtils apiUtils;
	PageObj pageObj;
	String sTCName;
	private String timeStamp, b_id,businessPreferenceQuery;
	private String env, run = "ui";
	private String baseUrl;
	private Utilities utils;
	private static Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void beforeMethod(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run , env , "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		b_id = dataSet.get("business_id");
		businessPreferenceQuery = OfferIngestionUtilities.businessPreferenceQuery.replace("$id", b_id);
		utils = new Utilities(driver);
	}

	@Test(description = "SQ-T3271 (1.0)- Fixed days expiry for points gifted should not be more than set in Expires After")
	@Owner(name = "Shashank Sharma")
	public void T3271_verifyFixedDaysExpiryLessThanExpiresAfter() throws Exception {

		//String b_id = dataSet.get("businessID");
		// enable flag from the db
		String updateQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, updateQuery,
				"preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "false", "live",
				b_id);
		pageObj.singletonDBUtilsObj();
		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "false", "went_live",
				b_id);
		int expiryAfterDays = Utilities.getRandomNoFromRange(5, 15);
		int TransferredPointsExpiryDaysInvalid = expiryAfterDays + 5;
		int TransferredPointsExpiryDaysValid = expiryAfterDays - 3;
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("set", expiryAfterDays);

		pageObj.earningPage().navigateToEarningTabs("Transfered Loyalty Expiry");

		pageObj.earningPage().setTransferredPointsExpiryDaysAsFixedDays(TransferredPointsExpiryDaysInvalid);
		String expectedErrorMessage = "Error updating business: Transferred points expiry days should be less than or equal to "
				+ expiryAfterDays + ".";
		pageObj.earningPage().verifyMessage(expectedErrorMessage);

		pageObj.earningPage().navigateToEarningTabs("Transfered Loyalty Expiry");

		String expectedSuccessMessage = dataSet.get("expectedSuccessMessage");
		pageObj.earningPage().setTransferredPointsExpiryDaysAsFixedDays(TransferredPointsExpiryDaysValid);
		pageObj.earningPage().verifyMessage(expectedSuccessMessage);

	}

	@Test(description = "SQ-T5055 (1.0) Verify flag i.e. Turn off Redemptions is enabled user should not be able to redeem via IFrame ")
	@Owner(name = "Shashank Sharma")
	public void T5055_verifyFlagTurnOffRedemptionGenerateRedemptionCodeViaIFrame() throws InterruptedException {

		// open business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// click turn off redemption flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard(dataSet.get("flagName"),
				dataSet.get("checkBoxFlagCheck"));

		// sign-up user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		utils.logPass("Api1 user signup is successful");
		String user_id = Integer.toString(signUpResponse.jsonPath().get("user_id"));

		// send redeemable to user
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(user_id, dataSet.get("apiKey"), "",
				dataSet.get("rewardId"), "", "");
		utils.logPass("Send reward points to the user successfully");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message/rewardamount to user");
		utils.logPass("Verified reward send to user successfully using API2");

		// login user in IFrame
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLogin(userEmail, Utilities.getApiConfigProperty("password"));

		// generate redemption code via IFrame
		pageObj.iframeSingUpPage().redemptionOfRewardOffer(dataSet.get("rewardName"));
		String text = pageObj.iframeSingUpPage().msgVisible();
		Assert.assertEquals(text, dataSet.get("expectedMsg"), "expected error message did not match");
		utils.logPass("Verified user should not be able to redeem via IFrame");

		// open business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
	//	pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// click turn off redemption flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard(dataSet.get("flagName"),
				dataSet.get("checkBoxFlagUnCheck"));
	}

	@Test(description = "SQ-T5056 (1.0) -- Verify flag i.e. Turn off Redemptions is enabled user should not be able to redeem via API V2 redemption for banked currency")
	@Owner(name = "Shashank Sharma")
	public void T5056_verifyFlagTurnOffRedemptionRedeemViaAPIV2RedemptionForBankedCurrency()
			throws InterruptedException {
		// sign-up user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		utils.logPass("Api1 user signup is successful");
		String access_token = signUpResponse.jsonPath().get("auth_token.token");
		String user_id = Integer.toString(signUpResponse.jsonPath().get("user_id"));

		// open business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// click turn off redemption flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard(dataSet.get("flagName"),
				dataSet.get("checkBoxFlagCheck"));

		// send reward amount to user
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(user_id, dataSet.get("apiKey"), "30", "",
				"", "");
		utils.logit("Send reward points to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message/rewardamount to user");
		utils.logPass("Verified reward amount send to user successfully using API2");

		// hit APIV2 redemption for banked currency - this will generate redemption
		// request for banked currency
		Response redemptionRes = pageObj.endpoints().Api2RedemptionWithBankedCurrency(dataSet.get("client"),
				dataSet.get("secret"), access_token);
		Assert.assertEquals(redemptionRes.statusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"verification of flag i.e. Turn off Redemptions is failed ");
		utils.logPass("Verified user is not able to redeem via API V2 redemption for banked currency");
		String response_val = redemptionRes.jsonPath().get("[0]").toString();

		Assert.assertEquals(response_val, dataSet.get("expectedMsg"), "expected error msg did not match");
		utils.logPass("Verified user is not able to redeem via API V2 redemption for banked currency");

		// click turn off redemption flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard(dataSet.get("flagName"),
				dataSet.get("checkBoxFlagUnCheck"));
	}

	@Test(description = "SQ-T5057 (1.0) -- Verify flag i.e. Turn off Redemptions is enabled user should not be able to redeem via API V1 redeem points")
	@Owner(name = "Shashank Sharma")
	public void T5057_verifyFlagTurnOffRedemptionsUserUnableToRedeemViaAPIV1RedeemPoints() throws InterruptedException {
		// sign-up user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		utils.logPass("Api1 user signup is successful");
		String access_token = signUpResponse.jsonPath().get("auth_token.token");
		String user_id = Integer.toString(signUpResponse.jsonPath().get("user_id"));

		// open business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// click turn off redemption flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard(dataSet.get("flagName"),
				dataSet.get("checkBoxFlagCheck"));

		// send redeem points to user
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(user_id, dataSet.get("apiKey"), "20", "",
				"", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message/rewardamount to user");
		utils.logit("Verified reward send to user successfully using API2");

		// redeem via API V1 redeem points
		Response redemptionRes = pageObj.endpoints().Api1MobileRedemptionRedeemed_Points(access_token, "15",
				dataSet.get("client"), dataSet.get("secret"));
		String response_val = redemptionRes.jsonPath().get("[0]").toString();
		Assert.assertEquals(response_val, dataSet.get("expectedMsg"),
				"Verification of flag i.e. Turn off Redemptions is failed ");
		utils.logPass("Verified user is not able to redeem via API V1");

		// click turn off redemption flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard(dataSet.get("flagName"),
				dataSet.get("checkBoxFlagUnCheck"));

	}
	
	@Test(description = "SQ-T7262 Verify POS/Drive-Thru tabs are visible if Enable Loyalty Identification at Drive-Thru flag is enabled"
			+ "SQ-T7263 Verify Drive-Thru tab is not visible if Enable Loyalty Identification at Drive-Thru flag is disabled"
			+ "SQ-T7264 Verify when Drive-Thru Short Code Length is saved and business live flag is true, the dropdown becomes disabled"
			+ "SQ-T7265 Verify when Drive-Thru Short Code Length is saved and business live flag is false, the dropdown remains enabled"
			+ "SQ-T7266 Verify admin is able to update POS Integration page if Enable Loyalty Identification at Drive-Thru flag is enabled"
			+ "SQ-T7267 Verify admin is able to update POS Integration page if Enable Loyalty Identification at Drive-Thru flag is disabled")
	@Owner(name = "Rakhi Rawat")
	public void T7262_verifyTabsWithLoyaltyIdentification() throws Exception {

		String b_id = dataSet.get("business_id");
		String businessPreferenceLiveQuery = OfferIngestionUtilities.businessPreferenceQuery.replace("$id", b_id);
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Loyalty Identification at Drive-Thru", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Business Live Now?", "uncheck");

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		Assert.assertTrue(
		        pageObj.dashboardpage().tabPresentOrNot("POS"),
		        "POS tab is not present on the Dashboard page"
		);
		Assert.assertTrue(
		        pageObj.dashboardpage().tabPresentOrNot("Drive-Thru"),
		        "Drive-Thru tab is not present on the Dashboard page"
		);
		utils.logit("pass","Verified POS/Drive-Thru tabs are visible when Enable Loyalty Identification at Drive-Thru flag is enabled");
		
		//LPE-T2925
		//user updates the page without making any changes 
		pageObj.dashboardpage().navigateToTabs("Drive-Thru");
		pageObj.posIntegrationPage().clickUpdateBtn();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "User is not able to make changes when Enable Loyalty Identification at Drive-Thru flag is enabled");
		utils.logit("pass","Verified that User is able to make changes when Enable Loyalty Identification at Drive-Thru flag is enabled");

		//user updates the page after making changes in any field/flag value
		//LPE-T3062
		// select the drp down value
		pageObj.dashboardpage().navigateToTabs("Drive-Thru");
		pageObj.posIntegrationPage().selectDriveThruDrpDownValue(dataSet.get("driveThruCodeLengthStrategy"));
		pageObj.posIntegrationPage().clickUpdateBtn();
		boolean status1 = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status1, "User is not able to make changes when Enable Loyalty Identification at Drive-Thru flag is enabled");
		utils.logit("pass","Verified that User is able to make changes when Enable Loyalty Identification at Drive-Thru flag is enabled");

		pageObj.dashboardpage().navigateToTabs("Drive-Thru");
		
		//verify Drive-Thru Short Code Length field when business live flag is false
		boolean isElementClickable = pageObj.ppccUtilities().isElementPresentAndClickable(
				"PosIntegrationPage.selectDriveThruShortCodeLengthFromDropdown", "Drive-Thru Short Code Length dropdown");
		Assert.assertTrue(isElementClickable,
				"Drive-Thru Short Code Length dropdown is disabled");
		utils.logit("pass","Drive-Thru Short Code Length dropdown remains enabled when business live flag is false");
		
		businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");
		//update business live and went_live flag to true in db and ensure they are reset after the test logic
		try {
			//update business live and went_live flag to true in db
			DBUtils.updateBusinessFlag(env, businessPreferenceLiveExpColValue, "true", "live", b_id);
			DBUtils.updateBusinessFlag(env, businessPreferenceLiveExpColValue, "true", "went_live", b_id);
			utils.longWaitInSeconds(5);
			pageObj.dashboardpage().navigateToTabs("Drive-Thru");
			//LPE-T3061
			// select the drp down value
			pageObj.posIntegrationPage().selectDriveThruDrpDownValue(dataSet.get("driveThruCodeLengthStrategy"));
			pageObj.posIntegrationPage().clickUpdateBtn();
			pageObj.dashboardpage().navigateToTabs("Drive-Thru");
			
			// verify Drive-Thru Short Code Length field when business live flag is true
			boolean isElementClickable1 = pageObj.ppccUtilities().isElementPresentAndClickable(
					"PosIntegrationPage.selectDriveThruShortCodeLengthFromDropdown",
					"Drive-Thru Short Code Length dropdown");
			Assert.assertFalse(isElementClickable1, "Drive-Thru Short Code Length dropdown is enabled");
			utils.logit("pass","Drive-Thru Short Code Length dropdown becomes disabled when business live flag is true");
		} finally {
			// Reset business live and went_live flags to their non-live state to avoid test pollution
			boolean resetLiveFlag = DBUtils.updateBusinessesPreference(env,
					businessPreferenceLiveExpColValue, "false", "live", b_id);
			if (!resetLiveFlag) {
				utils.logit("warn","Failed to reset 'live' business preference flag to false after test execution");
			}

			boolean resetWentLiveFlag = DBUtils.updateBusinessesPreference(env,
					businessPreferenceLiveExpColValue, "false", "went_live", b_id);
			if (!resetWentLiveFlag) {
				utils.logit("warn","Failed to reset 'went_live' business preference flag to false after test execution");
			}
		}
		//disable flag Enable Loyalty Identification at Drive-Thru
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Loyalty Identification at Drive-Thru", "uncheck");
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		Assert.assertTrue(
		        pageObj.dashboardpage().tabPresentOrNot("POS"),
		        "POS tab is not present on the Dashboard page"
		);
		Assert.assertFalse(
		        pageObj.dashboardpage().tabPresentOrNot("Drive-Thru"),
		        "Drive-Thru tab is still present on the Dashboard page"
		);
		utils.logit("pass","Verified Drive-Thru tab is visible when Enable Loyalty Identification at Drive-Thru flag is enabled");
		
		// LPE-T2912
		// user updates the page without making any changes 
		pageObj.posIntegrationPage().clickUpdateBtn();
		boolean status3 = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status3,
				"User is not able to make changes when Enable Loyalty Identification at Drive-Thru flag is disabled");
		utils.logit("pass","Verified that User is able to make changes when Enable Loyalty Identification at Drive-Thru flag is disabled");

		// user updates the page after making changes in any field/flag value
		pageObj.dashboardpage().checkUncheckAnyFlag("Return qualifying condition to v1", "uncheck");
		boolean status4 = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status4,
				"User is not able to make changes when Enable Loyalty Identification at Drive-Thru flag is disabled");
		utils.logit("pass","Verified that User is able to make changes when Enable Loyalty Identification at Drive-Thru flag is disabled");

	}
	@Test(description = "SQ-T7364 [API2] Verify API response when Enable Loyalty Identification at Drive-Thru flag is true and drive_thru_short_code_length not saved in DB"
			+ "SQ-T7365 [Secure API] Verify API response when Enable Loyalty Identification at Drive-Thru flag is true and drive_thru_short_code_length not saved in DB"
			+ "SQ-T7367 [API2] Verify API response when Enable Loyalty Identification at Drive-Thru flag is false and drive_thru_short_code_length not saved in DB"
			+ "SQ-T7368 [Secure API] Verify API response when Enable Loyalty Identification at Drive-Thru flag is false and drive_thru_short_code_length not saved in DB")
	@Owner(name = "Rakhi Rawat")
	public void T7364_verifyResponseWhenDriveThruLengthNotSaved() throws Exception {
		
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceQuery, "preferences");
		
		//enable_meta_cache_update_on_request to true
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag2"), b_id);
		// set live and went_live to true
		DBUtils.updateBusinessFlag(env, expColValue, "true", "live", b_id);
		DBUtils.updateBusinessFlag(env, expColValue, "true", "went_live", b_id);
		
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		//set enable_loyalty_identification_at_drive_thru flag value to true
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabsNew("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Loyalty Identification at Drive-Thru", "check");
		
		// Meta v2 API response validations
		Response metaApiResponse = pageObj.endpoints().Api2Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logit("Api2 Cards response status code is 200");
		String loyaltyIdentification = metaApiResponse.jsonPath().get("enable_loyalty_identification_at_drive_thru").toString();
		String driveThruLocationLevel = metaApiResponse.jsonPath().get("drive_thru_location_level_strategy").toString();
		Assert.assertEquals(loyaltyIdentification, "true", "enable_loyalty_identification_at_drive_thru value not matched");
		Assert.assertEquals(driveThruLocationLevel, "false", "drive_thru_location_level_strategy value not matched");
		utils.logit("pass","Verified api2/mobile/meta response when Enable Loyalty Identification at Drive-Thru flag is true and drive_thru_short_code_length not saved in DB");
		
		//api/mobile/cards API
		Response cardsResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for api/mobile/cards API");
		utils.logit("Api2 Cards response status code is 200");
		String loyaltyIdentification1 = cardsResponse.jsonPath().get("enable_loyalty_identification_at_drive_thru").toString();
		Assert.assertEquals(loyaltyIdentification1,"[true]", "enable_loyalty_identification_at_drive_thru value not matched");
		String driveThruLocationLevel1 = cardsResponse.jsonPath().get("drive_thru_location_level_strategy").toString();
		Assert.assertEquals(driveThruLocationLevel1,"[false]", "drive_thru_location_level_strategy value not matched");
		utils.logit("pass","Verified api/mobile/cards response when Enable Loyalty Identification at Drive-Thru flag is true and drive_thru_short_code_length not saved in DB");
		
		//revert the flag enable_loyalty_identification_at_drive_thru to false
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabsNew("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Loyalty Identification at Drive-Thru", "uncheck");
		
		// Meta v2 API response validations
		Response metaApiResponse1 = pageObj.endpoints().Api2Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApiResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logit("Api2 Cards response status code is 200");
		String loyaltyIdentification2 = metaApiResponse1.jsonPath().get("enable_loyalty_identification_at_drive_thru")
				.toString();
		String driveThruLocationLevel2 = metaApiResponse1.jsonPath().get("drive_thru_location_level_strategy").toString();
	    Assert.assertEquals(loyaltyIdentification2, "false", "enable_loyalty_identification_at_drive_thru value not matched");
		Assert.assertEquals(driveThruLocationLevel2, "false", "drive_thru_location_level_strategy value not matched");
		utils.logit("pass","Verified api2/mobile/meta response when Enable Loyalty Identification at Drive-Thru flag is false and drive_thru_short_code_length not saved in DB");
		
		// api/mobile/cards API
		Response cardsResponse1 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for api/mobile/cards API");
		utils.logit("Api2 Cards response status code is 200");
		String loyaltyIdentification3 = cardsResponse1.jsonPath().get("enable_loyalty_identification_at_drive_thru")
				.toString();
		Assert.assertEquals(loyaltyIdentification3, "[false]",
				"enable_loyalty_identification_at_drive_thru value not matched");
		String driveThruLocationLevel3 = cardsResponse1.jsonPath().get("drive_thru_location_level_strategy").toString();
		Assert.assertEquals(driveThruLocationLevel3, "[false]", "drive_thru_location_level_strategy value not matched");
		utils.logit("pass","Verified api/mobile/cards response when Enable Loyalty Identification at Drive-Thru flag is false and drive_thru_short_code_length not saved in DB");
	}
	
	@Test(description = "SQ-T7369 [API2] Verify API response when Enable Loyalty Identification at Drive-Thru flag is true and drive_thru_short_code_length: '' is saved in DB"
			+ "SQ-T7370 [Secure API] Verify API response when Enable Loyalty Identification at Drive-Thru flag is true and drive_thru_short_code_length: '' is saved in DB"
			+ "SQ-T7371 [API2] Verify API response when Enable Loyalty Identification at Drive-Thru flag is true and value of drive_thru_short_code_length is saved in DB"
			+ "SQ-T7372 [Secure API] Verify API response when Enable Loyalty Identification at Drive-Thru flag is true and value of drive_thru_short_code_length is saved in DB")
	@Owner(name = "Rakhi Rawat")
	public void T7369_verifyResponseWhenDriveThruLengthSaved() throws Exception {
		
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceQuery, "preferences");
		
		//enable_meta_cache_update_on_request to true
		DBUtils.updateBusinessFlag(env, expColValue, "true", "enable_meta_cache_update_on_request", b_id);
		//set live and went_live to false
		DBUtils.updateBusinessFlag(env, expColValue, "false", "live", b_id);
		DBUtils.updateBusinessFlag(env, expColValue, "false", "went_live", b_id);
		// open business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		
		// set enable_loyalty_identification_at_drive_thru flag value to true
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabsNew("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Loyalty Identification at Drive-Thru", "check");

		// set drive_thru_short_code_length
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.dashboardpage().navigateToTabs("Drive-Thru");
		pageObj.posIntegrationPage().deselectDriveThruDrpDownValue();
		pageObj.posIntegrationPage().clickDriveThroughUpdateBtn();

		// Meta v2 API response validations
		Response metaApiResponse = pageObj.endpoints().Api2Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logit("Api2 Cards response status code is 200");
		String loyaltyIdentification = metaApiResponse.jsonPath().get("enable_loyalty_identification_at_drive_thru")
				.toString();
		String driveThruLocationLevel = metaApiResponse.jsonPath().get("drive_thru_location_level_strategy").toString();
		Assert.assertEquals(loyaltyIdentification, "true",
				"enable_loyalty_identification_at_drive_thru value not matched");
		Assert.assertEquals(driveThruLocationLevel, "false", "drive_thru_location_level_strategy value not matched");
		utils.logit("pass",
				"Verified api2/mobile/meta response when Enable Loyalty Identification at Drive-Thru flag is true and drive_thru_short_code_length saved '' in DB");

		// api/mobile/cards API
		Response cardsResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for api/mobile/cards API");
		utils.logit("Api2 Cards response status code is 200");
		String loyaltyIdentification1 = cardsResponse.jsonPath().get("enable_loyalty_identification_at_drive_thru")
				.toString();
		Assert.assertEquals(loyaltyIdentification1, "[true]",
				"enable_loyalty_identification_at_drive_thru value not matched");
		String driveThruLocationLevel1 = cardsResponse.jsonPath().get("drive_thru_location_level_strategy").toString();
		Assert.assertEquals(driveThruLocationLevel1, "[false]", "drive_thru_location_level_strategy value not matched");
		utils.logit("pass",
				"Verified api/mobile/cards response when Enable Loyalty Identification at Drive-Thru flag is true and drive_thru_short_code_length saved '' in DB");

		//set value for drive_thru_short_code_length
		pageObj.dashboardpage().navigateToTabs("Drive-Thru");
		pageObj.posIntegrationPage().selectDriveThruDrpDownValue(dataSet.get("driveThruCodeLengthStrategy"));
		pageObj.posIntegrationPage().clickDriveThroughUpdateBtn();
		
		// Meta v2 API response validations
		Response metaApiResponse1 = pageObj.endpoints().Api2Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApiResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logit("Api2 Cards response status code is 200");
		String loyaltyIdentification2 = metaApiResponse1.jsonPath().get("enable_loyalty_identification_at_drive_thru")
				.toString();
		String driveThruLocationLevel2 = metaApiResponse1.jsonPath().get("drive_thru_location_level_strategy")
				.toString();
		Assert.assertEquals(loyaltyIdentification2, "true",
				"enable_loyalty_identification_at_drive_thru value not matched");
		Assert.assertEquals(driveThruLocationLevel2, "true", "drive_thru_location_level_strategy value not matched");
		utils.logit("pass","Verified api2/mobile/meta response Drive-Thru flag is true and value of drive_thru_short_code_length is saved in DB");

		// api/mobile/cards API
		Response cardsResponse1 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for api/mobile/cards API");
		utils.logit("Api2 Cards response status code is 200");
		String loyaltyIdentification3 = cardsResponse1.jsonPath().get("enable_loyalty_identification_at_drive_thru")
				.toString();
		Assert.assertEquals(loyaltyIdentification3, "[true]",
				"enable_loyalty_identification_at_drive_thru value not matched");
		String driveThruLocationLevel3 = cardsResponse1.jsonPath().get("drive_thru_location_level_strategy").toString();
		Assert.assertEquals(driveThruLocationLevel3, "[true]", "drive_thru_location_level_strategy value not matched");
		utils.logit("pass",
				"Verified api/mobile/cards response when Enable Loyalty Identification at Drive-Thru flag is true and drive_thru_short_code_length saved in DB");
	}
	@Test(description = "SQ-T7373 [API2] Verify API response when Enable Loyalty Identification at Drive-Thru flag is false but value of drive_thru_short_code_length is saved in DB"
			+ "SQ-T7374 [Secure API] Verify API response when Enable Loyalty Identification at Drive-Thru flag is false but value of drive_thru_short_code_length is saved in DB"
			+ "SQ-T7375 [API2] Verify API response when enable_loyalty_identification_at_drive_thru and drive_thru_short_code_length not saved in DB"
			+ "SQ-T7376 [Secure API] Verify API response when enable_loyalty_identification_at_drive_thru and drive_thru_short_code_length not saved in DB")
	@Owner(name = "Rakhi Rawat")
	public void T7373_verifyResponseWhenDriveThruDisabled() throws Exception {
		
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceQuery, "preferences");
		
		//set enable_loyalty_identification_at_drive_thru flag value to true
		DBUtils.updateBusinessFlag(env, expColValue, "true", "enable_loyalty_identification_at_drive_thru", b_id);
		//enable_meta_cache_update_on_request to true
		DBUtils.updateBusinessFlag(env, expColValue, "true", "enable_meta_cache_update_on_request", b_id);
		//set live and went_live to false
		DBUtils.updateBusinessFlag(env, expColValue, "false", "live", b_id);
		DBUtils.updateBusinessFlag(env, expColValue, "false", "went_live", b_id);
		// open business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// set drive_thru_short_code_length
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.dashboardpage().navigateToTabs("Drive-Thru");
		pageObj.posIntegrationPage().selectDriveThruDrpDownValue(dataSet.get("driveThruCodeLengthStrategy"));
		pageObj.posIntegrationPage().clickDriveThroughUpdateBtn();

		//set enable_loyalty_identification_at_drive_thru flag value to false
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabsNew("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Loyalty Identification at Drive-Thru", "uncheck");
		
		// Meta v2 API response validations
		Response metaApiResponse = pageObj.endpoints().Api2Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logit("Api2 Cards response status code is 200");
		String loyaltyIdentification = metaApiResponse.jsonPath().get("enable_loyalty_identification_at_drive_thru")
				.toString();
		String driveThruLocationLevel = metaApiResponse.jsonPath().get("drive_thru_location_level_strategy").toString();
		Assert.assertEquals(loyaltyIdentification, "false",
				"enable_loyalty_identification_at_drive_thru value not matched");
		Assert.assertEquals(driveThruLocationLevel, "false", "drive_thru_location_level_strategy value not matched");
		utils.logit("pass",
				"Verified api2/mobile/meta response when Enable Loyalty Identification at Drive-Thru flag is false and drive_thru_short_code_length saved in DB");

		// api/mobile/cards API
		Response cardsResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for api/mobile/cards API");
		utils.logit("Api2 Cards response status code is 200");
		String loyaltyIdentification1 = cardsResponse.jsonPath().get("enable_loyalty_identification_at_drive_thru")
				.toString();
		Assert.assertEquals(loyaltyIdentification1, "[false]",
				"enable_loyalty_identification_at_drive_thru value not matched");
		String driveThruLocationLevel1 = cardsResponse.jsonPath().get("drive_thru_location_level_strategy").toString();
		Assert.assertEquals(driveThruLocationLevel1, "[false]", "drive_thru_location_level_strategy value not matched");
		utils.logit("pass",
				"Verified api/mobile/cards response when Enable Loyalty Identification at Drive-Thru flag is false and drive_thru_short_code_length saved in DB");

		//set enable_loyalty_identification_at_drive_thru flag value to true in order to update drive_thru_short_code_length to blank
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabsNew("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Loyalty Identification at Drive-Thru", "check");
		
		//set value for drive_thru_short_code_length
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.dashboardpage().navigateToTabs("Drive-Thru");
		pageObj.posIntegrationPage().deselectDriveThruDrpDownValue();
		pageObj.posIntegrationPage().clickDriveThroughUpdateBtn();
		
		// set enable_loyalty_identification_at_drive_thru flag value to false
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabsNew("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Loyalty Identification at Drive-Thru", "uncheck");
		
		// Meta v2 API response validations
		Response metaApiResponse1 = pageObj.endpoints().Api2Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApiResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logit("Api2 Cards response status code is 200");
		String loyaltyIdentification2 = metaApiResponse1.jsonPath().get("enable_loyalty_identification_at_drive_thru")
				.toString();
		String driveThruLocationLevel2 = metaApiResponse1.jsonPath().get("drive_thru_location_level_strategy")
				.toString();
		Assert.assertEquals(loyaltyIdentification2, "false",
				"enable_loyalty_identification_at_drive_thru value not matched");
		Assert.assertEquals(driveThruLocationLevel2, "false", "drive_thru_location_level_strategy value not matched");
		utils.logit("pass","Verified api2/mobile/meta response Drive-Thru flag is false and value of drive_thru_short_code_length is saved '' in DB");

		// api/mobile/cards API
		Response cardsResponse1 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for api/mobile/cards API");
		utils.logit("Api2 Cards response status code is 200");
		String loyaltyIdentification3 = cardsResponse1.jsonPath().get("enable_loyalty_identification_at_drive_thru")
				.toString();
		Assert.assertEquals(loyaltyIdentification3, "[false]",
				"enable_loyalty_identification_at_drive_thru value not matched");
		String driveThruLocationLevel3 = cardsResponse1.jsonPath().get("drive_thru_location_level_strategy").toString();
		Assert.assertEquals(driveThruLocationLevel3, "[false]", "drive_thru_location_level_strategy value not matched");
		utils.logit("pass",
				"Verified api/mobile/cards response when Enable Loyalty Identification at Drive-Thru flag is false and drive_thru_short_code_length saved '' in DB");
	}
	
	@AfterMethod(alwaysRun = true)
	public void afterClass() throws Exception {
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceQuery, "preferences");
		
		//set live and went_live to true
		DBUtils.updateBusinessFlag(env, expColValue, "true", "live", b_id);
		DBUtils.updateBusinessFlag(env, expColValue, "true", "went_live", b_id);
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
