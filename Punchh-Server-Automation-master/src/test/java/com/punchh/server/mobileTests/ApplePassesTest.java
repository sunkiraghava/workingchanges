package com.punchh.server.mobileTests;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.mobilePages.mobilePageObj;
import com.punchh.server.mobileUtilities.AppiumUtils;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBManager;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.GmailConnection;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.appium.java_client.ios.IOSDriver;
import io.restassured.response.Response;

// Author:- Nipun Jain
@Listeners(TestListeners.class)
public class ApplePassesTest {

	static Logger logger = LogManager.getLogger(ApplePassesTest.class);

	// Mobile objects
	public IOSDriver mobileDriver;
	private mobilePageObj mobilePageObj;
	private AppiumUtils appiumUtils;

	// Web objects
	public WebDriver driver;
	private PageObj pageObj;

	private String sTCName;
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String env, run = "ui";
	private Utilities utils;

	@BeforeClass
	public void beforeClass() {
		appiumUtils = new AppiumUtils();
		appiumUtils.startAppiumServer();
	}

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) throws IOException {

		// Mobile objects
		mobileDriver = appiumUtils.getIOSDriver(null);
		mobilePageObj = new mobilePageObj(mobileDriver);

		// Web objects
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		utils = new Utilities(driver);
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);

		// Put without overwriting existing keys
		pageObj.readData().readTestData.forEach((key, value) -> dataSet.putIfAbsent(key, value));
		logger.info(sTCName + " ==>" + dataSet);
	}

	@Test(description = "SQ-T3594 (1.0) Verify apple pass functionality with NFC and App Link ON")
	public void T3594_verify_ApplePass_NFC_And_Applink_ON() throws Exception {
		verifyApplePassNFCAndApplinkFunctionality(true, true);
	}

	@Test(description = "SQ-T3595 (1.0) Verify apple pass functionality with NFC and App Link both OFF")
	public void T3595_verify_ApplePass_NFC_And_Applink_OFF() throws Exception {
		verifyApplePassNFCAndApplinkFunctionality(false, false);
	}

	@Test(description = "SQ-T5834_CCA2-736 | New user signup via personalise apple pass")
	public void T5834_verify_User_SignUp_Via_ApplePersonalisePass() throws Exception {
		String slug = dataSet.get("slug");
		String personalisePassLinkGeneratorURL = baseUrl + "/v1/pospass/" + slug;
		String email = "ApplePassAutoUser_" + utils.getTimestampInNanoseconds() + "@partech.com";
		String Fname = "TestFn_" + utils.getTimestampInNanoseconds();
		String Lname = "TestLn_" + utils.getTimestampInNanoseconds();

		// Enable Passes in Cockpit
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(slug);
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.instanceDashboardPage().checkUncheckFlagOnCockpitDasboard("Enable Passes?", "check");

		driver.navigate().to(personalisePassLinkGeneratorURL);

		// Get text from the URL
		WebElement ele = driver.findElement(By.tagName("pre"));
		String jsonText = ele.getText();

		String passURL = utils.findValueByKeyFromJsonAsString(jsonText, "passDownload");
		Assert.assertNotNull(passURL, "Personalise pass URL is null");
		utils.logit("Extracted personalise pass URL as: " + passURL);

		mobileDriver.get(passURL);
		utils.logit("Opened personalise pass URL in device : " + passURL);

		mobilePageObj.applePassesPage().clickNextButton();
		mobilePageObj.applePassesPage().enterName(Fname + " " + Lname);
		mobilePageObj.applePassesPage().enterEmail(email);
		mobilePageObj.applePassesPage().clickAddButton();
		mobilePageObj.applePassesPage().clickAgreeButton();
		mobilePageObj.applePassesPage().openAppleWallet();
		Assert.assertTrue(mobilePageObj.applePassesPage().isPassPresentInWallet(),
				"Apple pass is not present in the wallet");

		utils.logPass("Apple pass is present in the wallet");

		// Validate user created in Punchh
		String query = "select id from users where email = '" + email + "' and first_name = '" + Fname
				+ "' and last_name = '" + Lname + "'";

		String userID = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(userID, "User is not created in Punchh");
		utils.logit("User is created in Punchh with ID: " + userID);

		utils.logit("Pass", "User signed up successfully via Apple personalise pass");
	}

	@Test(description = "SQ-T3599  CCA2-801, 804 | Verify redemption apple pass generation - BankedRewardRedemption")
	public void T3599_Validate_Redemption_Apple_Pass_BankedRewardRedemption() throws Exception {
		String pkpassFilePath = System.getProperty("user.dir") + "/resources/ExportData";
		pageObj.guestTimelinePage().createAndCleanDownloadBrowserDownloadFolder(pkpassFilePath);
		Utilities.clearFolder(pkpassFilePath, ".pkpass");

		logger.info("== Iframe fields validation test ==");
		// SoftAssert softAssertion = new SoftAssert();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Validate Iframe page options
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + "/whitelabel/" + dataSet.get("slug"));
		// iframe signup with empty data
		String iframemptydata = pageObj.iframeSingUpValidationPageClass().iframeSignUpForApplePass();
//		pageObj.iframeSingUpValidationPageClass().clickOnSaveProfile();

		// login via auth API using Invalid user Password
		Response loginResponse1 = pageObj.endpoints().authApiUserLogin(iframemptydata, dataSet.get("client"),
				dataSet.get("secret"));

		String userID = loginResponse1.jsonPath().getString("user_id").replace("[", "").replace("]", "");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "100", "",
				"", "");

		utils.logPass("Send redeemable to the user successfully");

		String redeemCode = pageObj.iframeSingUpPage().redeemAmount("20");
		long redeemCode_exp = Long.parseLong(redeemCode);

		// create URL
		// https://dashboard.staging.punchh.io/v1/redemption_pass/nativegrill/1135610
		String url = baseUrl + "/v1/redemption_pass/nativegrill/" + redeemCode;
		pageObj.instanceDashboardPage().navigateToPunchhInstance(url);

		// Extract and validate the .pkpass file
		JsonNode passJson = extractJsonFromPKPass(pkpassFilePath + "/redemption_pass.pkpass");
		SoftAssert softAssert = new SoftAssert();

		utils.logPass("Starting validation for .pkpass file. Extracted pass.json content: " + passJson.toPrettyString());

		String redemptionIDQuery = "select id from redemptions where user_id = '" + userID + "'";
		String redemptionID = DBUtils.executeQueryAndGetColumnValue(env, redemptionIDQuery, "id");
		validatePKPassJsonAllCommonFields("nativegrill", redemptionID, passJson, softAssert);

		// Verify that the Apple pass is successfully added to the wallet
		Assert.assertTrue(addAndVerifyPassInWallet(mobileDriver, url),
				"Apple pass is not added to the wallet successfully");
		utils.logPass("Apple pass is added to the wallet successfully");

		String dbQuery = "select internal_tracking_code  , type from redemptions where user_id = '" + userID + "'";
		long internal_tracking_code = Long
				.parseLong(DBUtils.executeQueryAndGetColumnValue(env, dbQuery, "internal_tracking_code"));
		Assert.assertEquals(internal_tracking_code, redeemCode_exp,
				redeemCode_exp + " - Redemption code is not matched in DB");

		utils.logPass(internal_tracking_code + " - Redemption code is matched in DB");

		String redeemptionType = DBUtils.executeQueryAndGetColumnValue(env, dbQuery, "type");
		Assert.assertEquals(redeemptionType, "BankedRewardRedemption",
				"BankedRewardRedemption - Redemption type is not matched in DB");
		utils.logPass(redeemptionType + " - Redemption Type is matched in DB");

		validateField(passJson, "coupon.primaryFields[0].value", "20.0", softAssert);

		validateField(passJson, "coupon.auxiliaryFields[0].label", "Auxiliary field label", softAssert);

		validateField(passJson, "coupon.secondaryFields[0].value", "Redeemable", softAssert);
		softAssert.assertAll();
		utils.logPass("Verified all validation points in .pkpass file");

	}

	@Test(description = "SQ-T3598 CCA2-801, 804 | Verify redemption apple pass generation - RewardRedemption")
	public void T3598_Validate_Redemption_Apple_Pass_RewardRedemption() throws Exception {
		String pkpassFilePath = System.getProperty("user.dir") + "/resources/ExportData";
		Utilities.clearFolder(pkpassFilePath, ".pkpass");

		logger.info("== Iframe fields validation test ==");
		// SoftAssert softAssertion = new SoftAssert();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Validate Iframe page options
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + "/whitelabel/" + dataSet.get("slug"));
		// iframe signup with empty data
		String iframemptydata = pageObj.iframeSingUpValidationPageClass().iframeSignUpForApplePass();
		// pageObj.iframeSingUpValidationPageClass().clickOnSaveProfile();

		// login via auth API using Invalid user Password
		Response loginResponse1 = pageObj.endpoints().authApiUserLogin(iframemptydata, dataSet.get("client"),
				dataSet.get("secret"));

		String userID = loginResponse1.jsonPath().getString("user_id").replace("[", "").replace("]", "");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("admin_key"), "",
				"2822746", "", "");

		utils.logPass("Send redeemable to the user successfully");

		String redeemCode = pageObj.iframeSingUpPage()
				.redeemRedeemableReward("Do NotDelete Automation ApplePass_T3599 (Never Expires)");
		long redeemCode_exp = Long.parseLong(redeemCode);

		// create URL
		// https://dashboard.staging.punchh.io/v1/redemption_pass/nativegrill/1135610
		String url = baseUrl + "/v1/redemption_pass/nativegrill/" + redeemCode;
		pageObj.instanceDashboardPage().navigateToPunchhInstance(url);

		// Extract and validate the .pkpass file
		JsonNode passJson = extractJsonFromPKPass(pkpassFilePath + "/redemption_pass.pkpass");
		SoftAssert softAssert = new SoftAssert();

		utils.logPass("Starting validation for .pkpass file. Extracted pass.json content: " + passJson.toPrettyString());

		String redemptionIDQuery = "select id from redemptions where user_id = '" + userID + "'";
		String redemptionID = DBUtils.executeQueryAndGetColumnValue(env, redemptionIDQuery, "id");
		validatePKPassJsonAllCommonFields("nativegrill", redemptionID, passJson, softAssert);

		// Verify that the Apple pass is successfully added to the wallet
		Assert.assertTrue(addAndVerifyPassInWallet(mobileDriver, url),
				"Apple pass is not added to the wallet successfully");
		utils.logPass("Apple pass is added to the wallet successfully");

		String dbQuery = "select internal_tracking_code  , type from redemptions where user_id = '" + userID + "'";
		long internal_tracking_code = Long
				.parseLong(DBUtils.executeQueryAndGetColumnValue(env, dbQuery, "internal_tracking_code"));
		Assert.assertEquals(internal_tracking_code, redeemCode_exp,
				redeemCode_exp + " - Redemption code is not matched in DB");

		utils.logPass(internal_tracking_code + " - Redemption code is matched in DB");

		String redeemptionType = DBUtils.executeQueryAndGetColumnValue(env, dbQuery, "type");
		Assert.assertEquals(redeemptionType, "RewardRedemption",
				"BankedRewardRedemption - Redemption type is not matched in DB");
		utils.logPass(redeemptionType + " - Redemption Type is matched in DB");

		validateField(passJson, "coupon.primaryFields[0].value", "Do NotDelete Automation ApplePass_T3599", softAssert);

		validateField(passJson, "coupon.auxiliaryFields[0].label", "Auxiliary field label", softAssert);

		validateField(passJson, "coupon.secondaryFields[0].value", "Redeemable", softAssert);
		softAssert.assertAll();
		utils.logPass("Verified all validation points in .pkpass file");
	}

	@Test(description = "SQ-T3521 INT2-770 | INT2-771 | INT2-1409 | Verify apple redemption coupon pass")
	public void T3521_ValidateAddApplePassLinkToCouponListPageFagEnabled()
			throws InterruptedException, ParseException, IOException {
		String pkpassFilePath = System.getProperty("user.dir") + "/resources/ExportData";
		Utilities.clearFolder(pkpassFilePath, ".pkpass");

		String couponCampaignName = "Coupon Campaign" + CreateDateTime.getTimeDateString();
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.instanceDashboardPage().checkUncheckFlagOnCockpitDasboard("Enable Passes?", "check");
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value and create coupon campaign
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(couponCampaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmountForMobile(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), "", "", false);
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Campaign created success message did not displayed....");
		utils.logPass("Coupon campaign created successfully");
		Utilities.longWait(8000);
		pageObj.campaignspage().searchCampaign(couponCampaignName);
		List<String> generatedCodeNameList = pageObj.campaignspage().getPreGeneratedCuponCodeList(); // getCouponCampaignCode();
		Assert.assertTrue(generatedCodeNameList.size() > 0,
				"Coupon code did not generated for the coupon campaign - " + couponCampaignName);
		List<String> applePassURl_List = pageObj.campaignspage().verifiedAndGetApplePassURlList(true);
		Assert.assertTrue(applePassURl_List.size() > 0,
				"Apple pass URL should generate if opeion is enabled for the coupon camp - " + couponCampaignName);

		utils.logit("Apple Pass URL List size " + applePassURl_List.size());
		utils.logPass("Apple pass url generated is - " + applePassURl_List.get(0));

		pageObj.instanceDashboardPage().navigateToPunchhInstance(applePassURl_List.get(0));
		utils.longWaitInSeconds(3);

		// Extract and validate the .pkpass file
		JsonNode passJson = extractJsonFromPKPass(pkpassFilePath + "/coupon_pass.pkpass");
		SoftAssert softAssert = new SoftAssert();

		utils.logPass("Starting validation for .pkpass file. Extracted pass.json content: " + passJson.toPrettyString());

		validateField(passJson, "coupon.primaryFields[0].value", generatedCodeNameList.get(0), softAssert);
		validateField(passJson, "coupon.primaryFields[0].label", "Primary field label", softAssert);

		validateField(passJson, "coupon.auxiliaryFields[0].label", "Auxiliary field label", softAssert);

		validateField(passJson, "coupon.secondaryFields[0].value", "Active", softAssert);
		softAssert.assertAll();
		utils.logPass("Verified all validation points in .pkpass file");
		// Verify that the Apple pass is successfully added to the wallet
		Assert.assertTrue(addAndVerifyPassInWallet(mobileDriver, applePassURl_List.get(0)),
				"Apple pass is not added to the wallet successfully");
		utils.logPass("Apple pass is added to the wallet successfully");
	}

	@Test(description = "SQ-T5451 INT2-1405 | INT2-1406 - Add apple pass URL to coupon list page and coupon export report")
	public void T5451_ValidateAddApplePassLinkToCouponListPageFlagDisabled()
			throws InterruptedException, ParseException, IOException {
		String couponCampaignName = "Coupon Campaign" + CreateDateTime.getTimeDateString();
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		String pkpassFilePath = System.getProperty("user.dir") + "/resources/ExportData";
		pageObj.guestTimelinePage().createAndCleanDownloadBrowserDownloadFolder(pkpassFilePath);
		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.instanceDashboardPage().checkUncheckFlagOnCockpitDasboard("Enable Passes?", "uncheck");
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value and create coupon campaign
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(couponCampaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmountForMobile(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), "", "", false);
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Campaign created success message did not displayed....");
		utils.logPass("Coupon campaign created successfully");
		Utilities.longWait(8000);
		pageObj.campaignspage().searchCampaign(couponCampaignName);
		List<String> generatedCodeNameList = pageObj.campaignspage().getPreGeneratedCuponCodeList(); // getCouponCampaignCode();
		Assert.assertTrue(generatedCodeNameList.size() > 0,
				"Coupon code did not generated for the coupon campaign - " + couponCampaignName);
		List<String> applePassURl_List = pageObj.campaignspage().verifiedAndGetApplePassURlList(true);
		Assert.assertTrue(applePassURl_List.size() == 0,
				"Apple pass URL should NOT generate if opeion is enabled for the coupon camp - " + couponCampaignName);

		utils.logPass("Verified that Apple pass url not generated if apple pass is disabled");

	}

	@Test(description = "SQ-T3964 INT2-778 | INT2-1409 | Verify apple redemption promo pass - Directly processed at POS")
	public void T3964_VerifiedAppleRedemptionPromoPass() throws InterruptedException, ParseException, IOException {
		String pkpassFilePath = System.getProperty("user.dir") + "/resources/ExportData";
		Utilities.clearFolder(pkpassFilePath, ".pkpass");

		String promoCode = CreateDateTime.getRandomString(6).toUpperCase() + Utilities.getRandomNoFromRange(500, 2000);
		String coupanCampaignName = "Auto_PromoCampaign" + CreateDateTime.getTimeDateString();

		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsPromoCampaign(coupanCampaignName, promoCode);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));

		pageObj.signupcampaignpage().setPromoCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
				dataSet.get("giftType"), dataSet.get("amount"), "", "");

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Campaign created success message did not displayed....");
		utils.logPass("Coupon campaign created successfuly");

		String appPassURL_promo = baseUrl + "/v1/coupon_pass/nativegrill/" + promoCode;
		utils.logit("appPassURL_promo--- " + appPassURL_promo);
		pageObj.instanceDashboardPage().navigateToPunchhInstance(appPassURL_promo);
		utils.longWaitInSeconds(3);
		// Extract and validate the .pkpass file
		JsonNode passJson = extractJsonFromPKPass(pkpassFilePath + "/coupon_pass.pkpass");
		SoftAssert softAssert = new SoftAssert();

		utils.logPass("Starting validation for .pkpass file. Extracted pass.json content: " + passJson.toPrettyString());

		// Verify that the Apple pass is successfully added to the wallet
		Assert.assertTrue(addAndVerifyPassInWallet(mobileDriver, appPassURL_promo),
				"Apple pass is not added to the wallet successfully");
		utils.logPass("Apple pass is added to the wallet successfully");

		validateField(passJson, "coupon.primaryFields[0].value", promoCode, softAssert);
		validateField(passJson, "coupon.primaryFields[0].label", "Primary field label", softAssert);

		validateField(passJson, "coupon.auxiliaryFields[0].label", "Auxiliary field label", softAssert);

		validateField(passJson, "coupon.secondaryFields[0].value", "Active", softAssert);
		softAssert.assertAll();
		utils.logPass("Verified all validation points in .pkpass file");

	}

	@Test(description = "SQ-T3961 INT2-778 | INT2-1409 | Verify apple redemption promo pass - Unlock rewards on Mobile App")
	public void T3961_VerifiedAppleRedemptionPromoPassUnlockRewardsOnMobileApp() throws Exception {
		String pkpassFilePath = System.getProperty("user.dir") + "/resources/ExportData";
		Utilities.clearFolder(pkpassFilePath, ".pkpass");
		String promoCode = CreateDateTime.getRandomString(6).toUpperCase() + Utilities.getRandomNoFromRange(500, 2000);
		String coupanCampaignName = "Auto_PromoCampaign" + CreateDateTime.getTimeDateString();

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsPromoCampaign(coupanCampaignName, promoCode);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));

		String appPassURL_promo = pageObj.signupcampaignpage().extractDownloadCouponUrl();

		pageObj.signupcampaignpage().setPromoCampaignGuestWithUnlockRewardsMobileAppOption(dataSet.get("noOfGuests"),
				dataSet.get("giftType"), dataSet.get("amount"));

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Campaign created success message did not displayed....");
		utils.logPass("Coupon campaign created successfuly");

//		String appPassURL_promo = baseUrl + "/v1/coupon_pass/nativegrill/" + promoCode;
//		utils.logit("appPassURL_promo--- " + appPassURL_promo);
		pageObj.instanceDashboardPage().navigateToPunchhInstance(appPassURL_promo);
		utils.longWaitInSeconds(3);
		// Extract and validate the .pkpass file
		JsonNode passJson = extractJsonFromPKPass(pkpassFilePath + "/coupon_pass.pkpass");

		SoftAssert softAssert = new SoftAssert();

		utils.logPass("Starting validation for .pkpass file. Extracted pass.json content: " + passJson.toPrettyString());

		// Verify that the Apple pass is successfully added to the wallet
		Assert.assertTrue(addAndVerifyPassInWallet(mobileDriver, appPassURL_promo),
				"Apple pass is not added to the wallet successfully");
		utils.logPass("Apple pass is added to the wallet successfully");

		validateField(passJson, "coupon.primaryFields[0].value", promoCode, softAssert);
		validateField(passJson, "coupon.primaryFields[0].label", "Primary field label", softAssert);

		validateField(passJson, "coupon.auxiliaryFields[0].label", "Auxiliary field label", softAssert);

		validateField(passJson, "coupon.secondaryFields[0].value", "Active", softAssert);
		softAssert.assertAll();
		utils.logPass("Verified extra fields value in download");

		// Get id from Coupons table
		String query1 = "select id from coupons where business_id = '" + dataSet.get("business_id") + "' and code = '"
				+ promoCode + "'";
		String couponCodeId = DBUtils.executeQueryAndGetColumnValue(env, query1, "id");
		// verify entry created in apple_passes table
		String query2 = "select COUNT(*) as count,owner_type from apple_passes where owner_id = '" + couponCodeId + "'";
		boolean expValue2 = DBUtils.verifyValueFromDBUsingPolling(env, query2, "count", "1");
		boolean expValue2a = DBUtils.verifyValueFromDBUsingPolling(env, query2, "owner_type", "Coupon");
		Assert.assertTrue(expValue2, "Entry not created in apple_passes table");
		Assert.assertTrue(expValue2a, "Owner type is not correct in apple_passes table");
		utils.logPass("Verified proper entry is created in apple_passes table");

	}

	@Test(description = "SQ-T3522 - INT2-770 | INT2-771 | INT2-1409 | Verify apple redemption coupon pass - request_coupon page")
	public void T3522_verifyAppleRedemptionCouponPass_request_coupon() throws Exception {

		String pkpassFilePath = System.getProperty("user.dir") + "/resources/ExportData";
		Utilities.clearFolder(pkpassFilePath, ".pkpass");

		// user sign-up
		String userEmail = CreateDateTime.getUniqueString("auto_") + "@partech.com";
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		utils.logPass("Api1 user signup is successful");
		String access_token = signUpResponse.jsonPath().get("auth_token.token");

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.instanceDashboardPage().checkUncheckFlagOnCockpitDasboard("Enable Passes?", "check");
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value and create coupon campaign
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		String couponCampaignName = "Coupon_Campaign" + CreateDateTime.getTimeDateString();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(couponCampaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmountForMobile(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), "", "", false);
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		String expiryDate = CreateDateTime.getFutureDateTimeUTC(1);
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Success message did not displayed....");
		utils.logPass("Coupon campaign created successfully");
		Utilities.longWait(8000);
		pageObj.campaignspage().searchCampaign(couponCampaignName);
		String dynamicToken = pageObj.campaignspage().getDynamicToken();
		String dynamicUrl = pageObj.campaignspage().getDynamicUrl();
		String jwtCode = pageObj.campaignspage().getJWTCodeWithEmailAndToken(dynamicToken, userEmail);
		String couponCode = pageObj.campaignspage().getCouponCode(dynamicUrl + jwtCode);

		String appleCouponPassURL = baseUrl + "/v1/coupon_pass/nativegrill/" + couponCode;
		pageObj.instanceDashboardPage().navigateToPunchhInstance(appleCouponPassURL);
		utils.longWaitInSeconds(3);

		// Extract and validate the .pkpass file
		JsonNode passJson = extractJsonFromPKPass(pkpassFilePath + "/coupon_pass.pkpass");
		utils.logPass("Starting validation for .pkpass file. Extracted pass.json content: " + passJson.toPrettyString());

		SoftAssert softAssert = new SoftAssert();
		validateField(passJson, "coupon.primaryFields[0].value", couponCode, softAssert);
		validateField(passJson, "coupon.secondaryFields[0].value", "Active", softAssert);
		validateField(passJson, "coupon.auxiliaryFields[0].value", expiryDate, softAssert);
		validateField(passJson, "barcode.message", couponCode, softAssert);
		validateField(passJson, "nfc.message", "R:" + couponCode, softAssert);
		softAssert.assertAll();
		utils.logPass("Successfully verified various fields in coupon pass");

		// Get id from Coupons table
		String query1 = "select id from coupons where business_id = '" + dataSet.get("business_id") + "' and code = '"
				+ couponCode + "'";
		String couponCodeId = DBUtils.executeQueryAndGetColumnValue(env, query1, "id");
		// verify entry created in apple_passes table
		String query2 = "select COUNT(*) as count,owner_type from apple_passes where owner_id = '" + couponCodeId + "'";
		boolean expValue2 = DBUtils.verifyValueFromDBUsingPolling(env, query2, "count", "1");
		boolean expValue2a = DBUtils.verifyValueFromDBUsingPolling(env, query2, "owner_type", "Coupon");
		Assert.assertTrue(expValue2, "Entry not created in apple_passes table");
		Assert.assertTrue(expValue2a, "Owner type is not correct in apple_passes table");
		utils.logPass("Verified proper entry is created in apple_passes table");

	}

	@Test(description = "SQ-T3597 - CCA2-736 | Verify user personalize apple pass is replaced with loyalty pass for an existing loyalty user with existing user details on the pass")
	public void T3597_verify_existingUser_SignUp_Via_ApplePersonalisePass() throws Exception {

		// user sign-up
		String userEmail = CreateDateTime.getUniqueString("autoPassUser_") + "@partech.com";
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		utils.logPass("Api1 user signup is successful");
		String access_token = signUpResponse.jsonPath().get("auth_token.token");
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		String firstName = signUpResponse.jsonPath().get("first_name");
		String lastName = signUpResponse.jsonPath().get("last_name");

		// update user account balance
		Response sendGiftResponse = pageObj.endpoints().sendPointsToUser(userID, dataSet.get("points"),
				dataSet.get("admin_key"));
		Assert.assertEquals(sendGiftResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched ");
		utils.logPass("Successfully gifted points to user ");

		String slug = dataSet.get("slug");
		String pospassUrl = baseUrl + "/v1/pospass/" + slug;
		String Fname = CreateDateTime.getUniqueString("testfn_");
		String Lname = CreateDateTime.getUniqueString("testln_");

		// Enable Passes in Cockpit
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(slug);
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.instanceDashboardPage().checkUncheckFlagOnCockpitDasboard("Enable Passes?", "check");

		driver.navigate().to(pospassUrl);
		WebElement ele = driver.findElement(By.xpath("//pre"));
		String textOnUI = ele.getText();

		String personalisePassURL = utils.findValueByKeyFromJsonAsString(textOnUI, "passDownload");
		Assert.assertNotNull(personalisePassURL, "Personalise pass URL is null");
		utils.logit("Personalise pass URL is - " + personalisePassURL);
//
		mobileDriver.get(personalisePassURL);
		utils.logit("Personalise pass URL is opened in mobile browser");

		mobilePageObj.applePassesPage().clickNextButton();
		mobilePageObj.applePassesPage().enterName(Fname + " " + Lname);
		mobilePageObj.applePassesPage().enterEmail(userEmail);
		mobilePageObj.applePassesPage().clickAddButton();
		mobilePageObj.applePassesPage().clickAgreeButton();
		mobilePageObj.applePassesPage().openAppleWallet();
		Assert.assertTrue(mobilePageObj.applePassesPage().isPassPresentInWallet(),
				"Apple pass is not present in the wallet");

		utils.logPass("Apple pass is present in the wallet");

		// verify Personalize pass is converted to loyalty pass of existing user
		String expectedData = "POINTS, " + dataSet.get("points");
		String actualData = mobilePageObj.applePassesPage().getDetailsOfApplePassInWallet("value"); // here attribute
																									// can be value,
																									// name and label
		utils.logit("Data on Apple pass is ------ " + actualData);
		Assert.assertEquals(actualData, expectedData, "Data on Apple pass in wallet is not matched");
		utils.logPass("Successfully verified Personalize pass is converted to loyalty pass of existing user");

		// Verify entry created in the user_transitions table with channel as 'AppleVAS'
		String query = "select COUNT(*) as count, channel from user_transitions where user_id = '" + userID + "'";
		boolean expValue = DBUtils.verifyValueFromDBUsingPolling(env, query, "count", "1");
		boolean expValue1 = DBUtils.verifyValueFromDBUsingPolling(env, query, "channel", "AppleVAS");
		Assert.assertTrue(expValue, "Entry not created in user_transitions table");
		Assert.assertTrue(expValue1, "Channel is not correct in user_transitions table for this user");
		utils.logPass("Verified proper entry is created in user_transitions table");

	}

	@Test(description = "SQ-T3276 - INT2-772 | Dynamic tag for apple pass | Mass notification campaign | Personalizable pass | Existing user with no apple pass")
	public void T3276_verifyDynamicTagForApplePersonalizePass_massNotification() throws Exception {

		// open business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// enable passes in cockpit
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_apple_passes", "check");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// check allow personalize pass option in android loyalty pass settings
		pageObj.menupage().navigateToSubMenuItem("Settings", "Passes");
		pageObj.settingsPage().clickOnDesiredPassButton(dataSet.get("passType"), dataSet.get("buttonType"));
		pageObj.settingsPage().clickOnApplePassTab(dataSet.get("tab"));
		pageObj.settingsPage().clickOnApplePassCheckBox("Allow personalize pass", "ON");
		pageObj.settingsPage().clickOnApplePassSaveButton();

		// user sign-up
		String userEmail = CreateDateTime.getUniqueString("autoPassUser_") + "@partech.com";
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		utils.logPass("Api1 user signup is successful");
		String access_token = signUpResponse.jsonPath().get("auth_token.token");
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		String firstName = signUpResponse.jsonPath().get("first_name");
		String lastName = signUpResponse.jsonPath().get("last_name");

		// navigate to campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaignNCHP(dataSet.get("DND_campaign"));
		pageObj.newCamHomePage().selectCampaignOption("Edit");
		pageObj.signupcampaignpage().clickNextBtn();
		String emailSubject = "testEmailSubject_" + CreateDateTime.getTimeDateString();
		pageObj.signupcampaignpage().setEmailSubject(emailSubject);
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().clickPreviousButton();

		pageObj.campaignspage().sendTestNotification(dataSet.get("DND_email"));

		String email = GmailConnection.getGmailEmailBody("subject", emailSubject, true);

		List<String> personalizePassLinks = pageObj.guestTimelinePage().extractLinksFromGmailBody(email);

		// open personalize pass link in mobile browser
		mobileDriver.get(personalizePassLinks.get(0));
		utils.logit("Personalise pass URL is opened in mobile browser");

		mobilePageObj.applePassesPage().clickNextButton();
		mobilePageObj.applePassesPage().enterName(firstName + " " + lastName);
		mobilePageObj.applePassesPage().enterEmail(userEmail);
		mobilePageObj.applePassesPage().clickAddButton();
		mobilePageObj.applePassesPage().clickAgreeButton();
		mobilePageObj.applePassesPage().openAppleWallet();
		Assert.assertTrue(mobilePageObj.applePassesPage().isPassPresentInWallet(),
				"Apple pass is not present in the wallet");

		utils.logPass("Apple pass is present in the wallet");

		// verify entry created in apple_passes table with source as email corresponding
		// to clicked link
		String query = "select COUNT(*) as count, source from apple_passes where owner_id = '" + userID + "'";
		boolean expValue = DBUtils.verifyValueFromDBUsingPolling(env, query, "count", "1");
		boolean expValue1 = DBUtils.verifyValueFromDBUsingPolling(env, query, "source", "email");
		Assert.assertTrue(expValue, "Entry not created in apple_passes table");
		Assert.assertTrue(expValue1, "Source is not correct in apple_passes table for this user");
		utils.logPass("Verified proper entry is created in apple_passes table");

	}

	@Test(description = "SQ-T3277 - INT2-772 | Dynamic tag apple pass for Mass offer campaign | Personalizable pass | Existing user with apple pass")
	public void T3277_verifyDynamicTagForApplePersonalizePass_massOffer() throws Exception {

		// open business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// enable passes in cockpit
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_apple_passes", "check");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// check allow personalize pass option in android loyalty pass settings
		pageObj.menupage().navigateToSubMenuItem("Settings", "Passes");
		pageObj.settingsPage().clickOnDesiredPassButton(dataSet.get("passType"), dataSet.get("buttonType"));
		pageObj.settingsPage().clickOnApplePassTab(dataSet.get("tab"));
		pageObj.settingsPage().clickOnApplePassCheckBox("Allow personalize pass", "ON");
		pageObj.settingsPage().clickOnApplePassSaveButton();

		// user sign-up
		String userEmail = CreateDateTime.getUniqueString("autoPassUser_") + "@partech.com";
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		utils.logPass("Api1 user signup is successful");
		String access_token = signUpResponse.jsonPath().get("auth_token.token");
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		String firstName = signUpResponse.jsonPath().get("first_name");
		String lastName = signUpResponse.jsonPath().get("last_name");

		// navigate to user timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().clickOnDownloadApplePassButton();

		// navigate to campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaignNCHP(dataSet.get("DND_campaign"));
		pageObj.newCamHomePage().selectCampaignOption("Edit");
		pageObj.signupcampaignpage().clickNextBtn();
		String emailSubject = "testEmailSubject_" + CreateDateTime.getTimeDateString();
		pageObj.signupcampaignpage().setEmailSubject(emailSubject);

		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().clickPreviousButton();

		pageObj.campaignspage().sendTestNotification(dataSet.get("DND_email"));

		String email = GmailConnection.getGmailEmailBody("subject", emailSubject, true);

		List<String> personalizePassLinks = pageObj.guestTimelinePage().extractLinksFromGmailBody(email);

		// open personalize pass link in mobile browser
		mobileDriver.get(personalizePassLinks.get(0));
		utils.logit("Personalise pass URL is opened in mobile browser");

		mobilePageObj.applePassesPage().clickNextButton();
		mobilePageObj.applePassesPage().enterName(firstName + " " + lastName);
		mobilePageObj.applePassesPage().enterEmail(userEmail);
		mobilePageObj.applePassesPage().clickAddButton();
		mobilePageObj.applePassesPage().clickAgreeButton();
		mobilePageObj.applePassesPage().openAppleWallet();
		Assert.assertTrue(mobilePageObj.applePassesPage().isPassPresentInWallet(),
				"Apple pass is not present in the wallet");

		utils.logPass("Apple pass is present in the wallet");

		// verify entry in apple_passes table with source as email
		String query = "select COUNT(*) as count, source from apple_passes where owner_id = '" + userID + "'";
		boolean expValue = DBUtils.verifyValueFromDBUsingPolling(env, query, "count", "1");
		boolean expValue1 = DBUtils.verifyValueFromDBUsingPolling(env, query, "source", "email");
		Assert.assertTrue(expValue, "Entry not created in apple_passes table");
		Assert.assertTrue(expValue1, "Source is not correct in apple_passes table for this user");
		utils.logPass("Verified entry in apple_passes table");

	}

	private void verifyApplePassNFCAndApplinkFunctionality(boolean isNfcOn, boolean isAppLinkOn) throws Exception {
		String client = dataSet.get("client");
		String secret = dataSet.get("secret");
		String slug = dataSet.get("slug");

		// User register/signup using API2 Signup
		String userEmail = "applePassuser_" + Utilities.getTimestamp() + "@partech.com";
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for api2 signup");
		utils.logPass("Api2 user signup is successful");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		String pkpassFilePath = System.getProperty("user.dir") + "/resources/ExportData";
		pageObj.guestTimelinePage().createAndCleanDownloadBrowserDownloadFolder(pkpassFilePath);

		// Navigate and perform configuration
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "Passes");

		pageObj.settingsPage().clickOnEditLoyaltyPassApplePass();
		pageObj.settingsPage().clickOnApplePassCheckBox("Show app launch link", isAppLinkOn ? "ON" : "OFF");
		pageObj.settingsPage().clickOnApplePassTab("Settings");
		pageObj.settingsPage().clickOnApplePassCheckBox("Near Field Communication (NFC) capability",
				isNfcOn ? "ON" : "OFF");
		pageObj.settingsPage().clickOnApplePassSaveButton();

		// Download and validate Apple pass
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String applePassURL = pageObj.guestTimelinePage().getApplePassUrl();
		pageObj.guestTimelinePage().clickOnDownloadApplePassButton();
		Thread.sleep(5000); // Wait for download to complete

		// Verify that the Apple pass is successfully added to the wallet
		Assert.assertTrue(addAndVerifyPassInWallet(mobileDriver, applePassURL),
				"Apple pass is not added to the wallet successfully");
		utils.logPass("Apple pass is added to the wallet successfully");

		// Extract and validate the .pkpass file
		JsonNode passJson = extractJsonFromPKPass(pkpassFilePath + "/loyalty_pass.pkpass");
		SoftAssert softAssert = new SoftAssert();

		utils.logPass("Starting validation for .pkpass file. Extracted pass.json content: " + passJson.toPrettyString());

		validatePKPassJsonAllCommonFields(slug, userID, passJson, softAssert);

		// Validate NFC and appLaunchURL fields based on the configuration
		if (isNfcOn) {
			validateField(passJson, "nfc.message", null, softAssert);
			validateField(passJson, "nfc.encryptionPublicKey", null, softAssert);
		} else {
			validateFieldAbsence(passJson, "nfc.message", softAssert);
			validateFieldAbsence(passJson, "nfc.encryptionPublicKey", softAssert);
		}

		if (isAppLinkOn)
			validateField(passJson, "appLaunchURL", null, softAssert);
		else
			validateFieldAbsence(passJson, "appLaunchURL", softAssert);

		try {
			softAssert.assertAll(); // Perform all validations/assertions
		} catch (AssertionError e) {
			logger.error("Validation errors in pkpass json: " + e.getMessage());
			TestListeners.extentTest.get().fail("Validation errors in pkpass json: " + e.getMessage());
			throw e; // Re-throw to propagate failure
		}
	}

	public void validatePKPassJsonAllCommonFields(String slug, String userID, JsonNode passJson, SoftAssert softAssert)
			throws Exception {

		// Data collection - businesses
		String query = "select concat(id , '|', name) as idAndName from businesses where slug ='" + slug + "';";
		pageObj.singletonDBUtilsObj();
		String busIdAndName = DBUtils.executeQueryAndGetColumnValue(env, query, "idAndName");
		String businessId = busIdAndName.split("\\|")[0];
		String businessName = busIdAndName.split("\\|")[1];

		// Data collection - apple_pass_designs
		query = "select * from apple_pass_designs where business_id =" + businessId
				+ " and type = 'AppleLoyaltyPassDesign'"; // need to dynamically change the type based on the pass type.
		pageObj.singletonDBUtilsObj();
		ResultSet rs = DBUtils.getResultSet(env, query);

		Map<String, String> passDesignData = new HashMap<>();
		if (rs.next()) {
			passDesignData.put("passTypeIdentifier", rs.getString("pass_type_identifier"));
			passDesignData.put("description", rs.getString("description"));
			passDesignData.put("logoText", rs.getString("logo_text"));
			passDesignData.put("backgroundColor", hexToFormattedRgb(rs.getString("background_color")));
			passDesignData.put("foregroundColor", hexToFormattedRgb(rs.getString("foreground_color")));
			passDesignData.put("labelColor", hexToFormattedRgb(rs.getString("label_color")));
		} else {
			TestListeners.extentTest.get().fail("No record found in apple_pass_designs for business_id: " + businessId);
			Assert.assertNotNull(rs, "No record found in apple_pass_designs for business_id: " + businessId);
		}
		DBManager.closeConnection();

		// Data collection - Apple Service
		query = "select properties from services where business_id =" + businessId + " and type = 'AppleService'";
		pageObj.singletonDBUtilsObj();
		String properties = DBUtils.executeQueryAndGetColumnValue(env, query, "properties");

		// Properties in Map
		Map<String, String> propertiesMap = extractProperties(properties);

		// Data collection - User Pass
		query = "select pass_auth_token from apple_passes where owner_id = " + userID;
		// query = "select pass_auth_token from apple_passes where owner_id = " +
		// 426001827;

		pageObj.singletonDBUtilsObj();
		String authenticationToken = DBUtils.executeQueryAndGetColumnValue(env, query, "pass_auth_token");

		// Expected values map
		Map<String, String> expectedValues = new HashMap<>();
		expectedValues.put("formatVersion", "1");
		expectedValues.put("passTypeIdentifier", passDesignData.get("passTypeIdentifier"));
		expectedValues.put("teamIdentifier", propertiesMap.get("ios_team_id"));
		expectedValues.put("webServiceURL", baseUrl + "/");
		expectedValues.put("authenticationToken", authenticationToken);
		expectedValues.put("organizationName", businessName);
		expectedValues.put("description", passDesignData.get("description"));
		expectedValues.put("logoText", passDesignData.get("logoText"));
		expectedValues.put("backgroundColor", passDesignData.get("backgroundColor"));
		expectedValues.put("foregroundColor", passDesignData.get("foregroundColor"));
		expectedValues.put("labelColor", passDesignData.get("labelColor"));

		// Validations
		for (Map.Entry<String, String> key : expectedValues.entrySet()) {
			String field = key.getKey();
			String expectedValue = key.getValue();
			validateField(passJson, field, expectedValue, softAssert);
		}
	}

	private void validateFieldAbsence(JsonNode passJson, String field, SoftAssert softAssert) {
		softAssert.assertFalse(passJson.has(field), field + " should not be present in pass json");
		utils.logPass(field + " is not present in pass json");
	}

	// Helper method to validate both presence and value of fields, including nested
	// ones
	private void validateField(JsonNode passJson, String field, String expectedValue, SoftAssert softAssert) {
		String[] fieldParts = field.split("\\."); // Split by dot to access nested properties
		JsonNode currentNode = passJson;

		for (String part : fieldParts) {
			if (part.matches(".*\\[\\d+\\]")) { // If part contains array index like addresses[0]
				String fieldName = part.substring(0, part.indexOf("[")); // Extract field name (e.g., "addresses")
				String indexString = part.replaceAll("[^\\d]", ""); // Extract numeric index
				int index = Integer.parseInt(indexString); // Convert to int

				if (currentNode.has(fieldName) && currentNode.get(fieldName).isArray()) {
					JsonNode arrayNode = currentNode.get(fieldName);
					if (arrayNode.size() > index) {
						currentNode = arrayNode.get(index); // Navigate to the indexed element
						utils.logPass("Successfully navigated to array element " + field);
					} else {
						softAssert.fail("Array index " + index + " out of bounds for field " + field);
						TestListeners.extentTest.get()
								.fail("Array index " + index + " out of bounds for field " + field);
						return; // Exit function
					}
				} else {
					softAssert.fail("Field " + fieldName + " is not a valid array in JSON");
					TestListeners.extentTest.get().fail("Field " + fieldName + " is not a valid array in JSON");
					return; // Exit function
				}
			} else {
				// Navigate through object fields
				if (currentNode.has(part)) {
					currentNode = currentNode.get(part); // Move deeper in JSON
					utils.logPass("Successfully found field " + part + " in " + field);
				} else {
					softAssert.fail("Field " + field + " not found in JSON");
					TestListeners.extentTest.get().fail("Field " + field + " not found in JSON");
					return; // Exit function
				}
			}
		}

		// Validate final field value
		if (expectedValue != null) {
			String actualValue = currentNode.asText();
			if (actualValue.equals(expectedValue)) {
				utils.logPass("Successfully validated field " + field + " with expected value: " + expectedValue);
			}
			softAssert.assertEquals(actualValue, expectedValue, "Invalid value for " + field);
		}

		// Final success check
		softAssert.assertTrue(true, "Field " + field + " is present in JSON");
		utils.logPass("Field " + field + " is present in JSON and validated successfully.");
	}

	public JsonNode extractJsonFromPKPass(String pkpassFilePath) {
		logger.info("Extracting pass.json from .pkpass file: {}", pkpassFilePath);
		JsonNode jsonNode = null;

		File pkpassFile = new File(pkpassFilePath);
		try (ZipInputStream zipStream = new ZipInputStream(new FileInputStream(pkpassFile))) {
			ZipEntry entry;

			while ((entry = zipStream.getNextEntry()) != null) {
				if ("pass.json".equals(entry.getName())) {
					logger.info("pass.json located. Parsing...");
					ObjectMapper objectMapper = new ObjectMapper();
					jsonNode = objectMapper.readTree(zipStream);
					break;
				}
			}
		} catch (IOException e) {
			logger.error("Error occurred while extracting pass.json from pkpass file: {}", e.getMessage());
			TestListeners.extentTest.get().fail("Failed to extract pass.json from the pkpass file.");
			Assert.fail("Failed to extract pass.json from the pkpass file.");
		}

		Assert.assertNotNull(jsonNode, "pass.json not found in the .pkpass file");
		return jsonNode;
	}

	public static String hexToFormattedRgb(String hex) {
		if (hex == null || hex.length() != 6) {
			logger.error("Hex color code must be 6 characters long.");
			TestListeners.extentTest.get().fail("Hex color code must be 6 characters long.");
			throw new IllegalArgumentException("Hex color code must be 6 characters long.");
		}
		int red = Integer.parseInt(hex.substring(0, 2), 16);
		int green = Integer.parseInt(hex.substring(2, 4), 16);
		int blue = Integer.parseInt(hex.substring(4, 6), 16);
		return String.format("rgb(%d, %d, %d)", red, green, blue);
	}

	private Map<String, String> extractProperties(String properties) {
		Map<String, String> map = new HashMap<>();
		Pattern pattern = Pattern.compile(":(\\S+):\\s*(.*)");
		Matcher matcher = pattern.matcher(properties);
		while (matcher.find()) {
			map.put(matcher.group(1), matcher.group(2));
		}
		return map;
	}

	public boolean addAndVerifyPassInWallet(IOSDriver mobileDriver, String passURL) {
		try {
			// Step 1: Add the pass to Apple Wallet
			mobileDriver.get(passURL);
			utils.logit("Opened pass URL: " + passURL);
			// Step 2: Click the "Add" button to add the pass to Apple Wallet
			mobilePageObj.applePassesPage().clickAddButton();
			// Step 3: Open Apple Wallet App
			mobilePageObj.applePassesPage().openAppleWallet();
			// Step 4: Verify that the pass is present in Apple Wallet
			Assert.assertTrue(mobilePageObj.applePassesPage().isPassPresentInWallet(),
					"Apple pass is not present in the wallet");
			return true;
		} catch (Exception e) {
			utils.logit("Error", "Error in adding, opening, or verifying pass in Apple Wallet: " + e.toString());
			return false;
		}
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		mobileDriver.quit();
		driver.quit();
	}

	@AfterClass
	public void afterClass() {
		try {
			appiumUtils.stopAppiumServer();
			logger.info("Appium server stopped successfully.");
		} catch (Exception e) {
			logger.info("Error while stopping Appium server: ", e);
		}
	}

}