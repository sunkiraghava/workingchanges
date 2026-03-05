package com.punchh.server.OMMTest;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.Listeners;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.jsonwebtoken.security.InvalidKeyException;
import io.restassured.response.Response;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.text.ParseException;

@Listeners(TestListeners.class)
public class CouponLookupTest {

	private static Logger logger = LogManager.getLogger(CouponLookupTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private String userEmail;
	private static Map<String, String> dataSet;
	String run = "ui";
	private List<String> codeNameList;
	private boolean GlobalBenefitRedemptionThrottlingToggle;
	ApiUtils apiUtils;
	private Utilities utils;
	private String campaignName;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run , env , "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		utils = new Utilities(driver);
		utils.logInfo(sTCName + " ==>" + dataSet);
		GlobalBenefitRedemptionThrottlingToggle = false;
		codeNameList = new ArrayList<String>();
		apiUtils = new ApiUtils();
		campaignName = null;
	}

	@Test(description = "SQ-T2553 Verify admin is able to create Coupon Campaign||SQ-T2555 Verify admin is able to create Coupon Campaign, and perform coupon lookup", groups = {
			"regression", "unstable", "dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2553_verifyAdminIsAbleToCreateCouponCampaign()
			throws InterruptedException, InvalidKeyException, UnsupportedEncodingException, ParseException {
		String couponCampaignName = "Coupon Campaign" + CreateDateTime.getTimeDateString();
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// Select offer dropdown value and create coupon campaign
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(couponCampaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), "", "", false);
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Campaign created success message did not displayed....");
		utils.logPass("Coupon campaign created successfuly");

		// Coupon Lookup part
		pageObj.campaignspage().searchCampaign(couponCampaignName);
		String dynamicToken = pageObj.campaignspage().getDynamicToken();
		String dynamicUrl = pageObj.campaignspage().getDynamicUrl();
		String jwtCode = pageObj.campaignspage().getJWTCode(dynamicToken);
		String couponCode = pageObj.campaignspage().getCouponCode(dynamicUrl + jwtCode);
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Support", "Coupon Lookup");
		List<String> couponData = pageObj.campaignspage().CouponLookUp(couponCode);
		System.out.println(couponData);
		Assert.assertEquals(couponData.get(0), couponCode, "Coupon code did not matched in coupon lookup");
		Assert.assertEquals(couponData.get(1), couponCampaignName,
				"Coupon campaign name did not matched in coupon lookup");
		utils.logPass("Coupon lookup verified successfuly");
	}

	@Test(description = "SQ-T3967 Promo campaign having promo code similar to coupon campaign with configuration as Directly processed at POS and Pre-Generated"
			+ "SQ-T3291 (1.0) Verify Coupon Campaign generates the specified number of coupon_codes", groups = {
					"regression", "unstable", "dailyrun" }, priority = 1)
	@Owner(name = "Rakhi Rawat")
	public void T3967_verifyPromoErrorForPOSAndPreGeneratedCoupon() throws InterruptedException, ParseException {

		String coupanCampaignName = "Auto_CouponCampaign" + CreateDateTime.getTimeDateString();

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(coupanCampaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), "", "",
				GlobalBenefitRedemptionThrottlingToggle);

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Campaign created success message did not displayed....");
		utils.logPass("Coupon campaign created successfuly");
		Utilities.longWait(8000);
		pageObj.campaignspage().searchCampaign(coupanCampaignName);
		codeNameList = pageObj.campaignspage().getPreGeneratedCuponCodeList();
		int numberOfGuestExpected = Integer.parseInt(dataSet.get("noOfGuests"));
		Assert.assertEquals(codeNameList.size(), numberOfGuestExpected);
		String generatedCodeName = codeNameList.get(0).toString();

		// promo campaign
		String promoCampaignName = "Auto_PromoCampaign" + CreateDateTime.getTimeDateString();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValuePromo"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsPromoCampaign(promoCampaignName, generatedCodeName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageTypePromo"));
		pageObj.signupcampaignpage().setPromoCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuestsPromo"),
				dataSet.get("giftType"), dataSet.get("amount"), "", "");
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		String promoErrorMessage = pageObj.mobileconfigurationPage().getErrorMessage();
		String expectedErrorMessage = "Code has already been taken";
		Assert.assertEquals(expectedErrorMessage, promoErrorMessage, "Promo error message did not displayed");
		// Assert.assertTrue(promoErrorMessage.contains("Code has already been taken"),
		// "Promo error message did not displayed");
		pageObj.utils().logPass("Promo error message i.e. 'Code has already been taken' displayed successfully");
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// delete coupon campaign
		pageObj.campaignspage().searchCampaign(coupanCampaignName);
		pageObj.campaignspage().deactivateOrDeleteTheCoupon("delete");

	}

	// shaleen
	@Test(description = "SQ-T5183 Coupon & Promo: Extend selection for Validity to date and time", groups = {
			"regression", "unstable", "dailyrun" })
	@Owner(name = "Shaleen Gupta")
	public void T5183_verifyGiftingFromCouponCampaign()
			throws InterruptedException, InvalidKeyException, UnsupportedEncodingException, ParseException {
		String couponCampaignName = "Coupon Campaign" + CreateDateTime.getTimeDateString();
		String couponCampaignName2 = "Coupon Campaign2_" + CreateDateTime.getTimeDateString();

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value and create coupon campaign with unlock rewards on
		// mobile app
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(couponCampaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmountForMobile(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), "", "", false);
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(0);
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Campaign created success message did not displayed....");
		pageObj.utils().logPass("Coupon campaign created successfuly");

		// Get coupon code
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().clickOnSwitchToNCHPBtn();
		pageObj.newCamHomePage().searchCampaign(couponCampaignName);
		pageObj.newCamHomePage().clickOptionFromDotsDropDown("Coupon codes list");
		pageObj.dashboardpage().navigateToTabs("Expired Coupon Codes");
		String code = pageObj.campaignspage().getCouponCodeforCouponCamp();

		// user sign-up
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		pageObj.utils().logPass("Api1 user signup is successful");
		String access_token = signUpResponse.jsonPath().get("auth_token.token");

		// redeem coupon on mobile
		Response res = pageObj.endpoints().couponRedemptionOnMobile(dataSet.get("client"), dataSet.get("secret"),
				access_token, code);
		String responseVal = res.jsonPath().get("[0]").toString();
		Assert.assertEquals(responseVal, dataSet.get("expectedMsg"), "Expected error message did not matched");
		pageObj.utils().logPass("Verified that user is unable to redeem the coupon after the campaign end time");

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value and create coupon campaign with directly
		// processed at POS
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(couponCampaignName2);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType2"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType2"), dataSet.get("amount"), "", "", false);
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(0);
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status2 = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status2, "Campaign created success message did not displayed....");
		pageObj.utils().logPass("Coupon campaign created successfuly");

		// get coupon code
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().clickOnSwitchToNCHPBtn();
		pageObj.newCamHomePage().searchCampaign(couponCampaignName2);
		pageObj.newCamHomePage().clickOptionFromDotsDropDown("Coupon codes list");
		pageObj.dashboardpage().navigateToTabs("Expired Coupon Codes");
		String code2 = pageObj.campaignspage().getCouponCodeforCouponCamp();

		// Pos redemption api
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response res2 = pageObj.endpoints().posRedemptionOfCouponCode(userEmail, date, code2, key, txn,
				dataSet.get("locationkey"));
		String responseVal2 = res2.jsonPath().get("status");
		Assert.assertEquals(responseVal2, dataSet.get("expectedMsg2"), "Expected error message did not matched");
		pageObj.utils().logPass("Verified that user is unable to redeem the coupon after the campaign end time");

	}

//	As discussed with Rahul Garg /api/auth/redemptions/online_order is not supported for coupon redemption
//	@Test(description = "SQ-T2368 verifying auth online coupons/promo redemption")
	public void verifyAuthOnlineCouponPromoRedemption() throws InterruptedException, Exception {
		String couponCampaignName = "Coupon Campaign" + CreateDateTime.getTimeDateString();
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Campaigns Link
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
		String generatedCodeName = pageObj.campaignspage().getPreGeneratedCuponCode(); // getCouponCampaignCode();

		// user creation using auth signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		apiUtils.verifyCreateResponse(signUpResponse, "Auth API user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");

		// Checkin via auth API
		String amount = "210.0";
		Response checkinResponse = pageObj.endpoints().onlineOrderCheckin(authToken, amount, dataSet.get("client"),
				dataSet.get("secret"));
		apiUtils.verifyResponse(checkinResponse, "Online order checkin");
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Auth_redemption_online_order
		Response resp = pageObj.endpoints().authOnlineCouponPromoRedemption(authToken, dataSet.get("client"),
				dataSet.get("secret"), generatedCodeName);
		apiUtils.verifyResponse(resp, "Auth online order bank currency redemption");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos redemption api");
		Assert.assertTrue(resp.jsonPath().get("status").toString().contains("Please HONOR it."));

		// Delete created campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		Thread.sleep(2000);
		pageObj.campaignspage().selectOngoingdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().removeSearchedCampaign(couponCampaignName);
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().deleteCampaignFromDb(campaignName, env);
		driver.quit();
		utils.logInfo("Browser closed");
	}

}