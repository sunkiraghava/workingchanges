package com.punchh.server.campaignsTest;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.text.ParseException;
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
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.jsonwebtoken.security.InvalidKeyException;
import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class CouponOfferCampaignTest {
	private static Logger logger = LogManager.getLogger(CouponOfferCampaignTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private String userEmail;
	private static Map<String, String> dataSet;
	String run = "ui";
	private String campaignName;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		prop = Utilities.loadPropertiesFile("config.properties");
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
		logger.info(sTCName + " ==> " + dataSet);
		campaignName = null;
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T3434 Verify redemption of a coupon when coupon is being used in mobile app and it is pre-generated", groups = {
			"regression", "unstable" }, priority = 0)
	@Owner(name = "Ashwini Shetty")
	public void T3434_VerifyCouponInMobileApp()
			throws InterruptedException, InvalidKeyException, UnsupportedEncodingException, ParseException {
		campaignName = "Coupon Campaign" + CreateDateTime.getTimeDateString();
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Campaigns Link
		// pageObj.menupage().clickCampaignsMenu();
		// pageObj.menupage().clickCampaignsLink();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value and create coupon campaign
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(campaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmountForMobile(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), "", "", false);
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Campaign created success message did not displayed....");
		TestListeners.extentTest.get().pass("Coupon campaign created successfully");
		Utilities.longWait(8000);
		pageObj.campaignspage().searchCampaign(campaignName);
		String generatedCodeName = pageObj.campaignspage().getPreGeneratedCuponCode();

		// user signup
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 1 user signup");
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api1 signup");
		TestListeners.extentTest.get().pass("API1 Signup is successful");
		logger.info("API1 Signup is successful");

		// coupon redemption in mobile
		Response couponRedemptionResponse = pageObj.endpoints().couponRedemptionOnMobile(dataSet.get("client"),
				dataSet.get("secret"), token, generatedCodeName);
		pageObj.apiUtils().verifyResponse(couponRedemptionResponse, "Coupon redemption on Mobile");
		Assert.assertEquals(couponRedemptionResponse.getStatusCode(), 200,
				"Status code 200 did not matched for Coupon redemption on Mobile");
		TestListeners.extentTest.get().pass("Coupon redemption on Mobile is successful");
		logger.info("Coupon redemption on Mobile is successful");

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String redeemedCouponStatus = pageObj.guestTimelinePage().verifyRedeemedCouponCode();
		Assert.assertTrue(redeemedCouponStatus.contains(generatedCodeName),
				"Rdeemed code details did not displayed  on timeline....");
		TestListeners.extentTest.get().pass("coupon campaign redeemed validated successfully on timeline");
	}

	@Test(description = "SQ-T3453 Verify redemption of a coupon when coupon is being used in mobile app and it is dynamic generated", priority = 1, groups = {
			"regression", "unstable" })
	@Owner(name = "Ashwini Shetty")
	public void T3453_VerifyCouponInMobileAppForDynamic()
			throws InterruptedException, InvalidKeyException, UnsupportedEncodingException, ParseException {
		//String couponCampaignName = "Coupon Campaign" + CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		/*
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
		TestListeners.extentTest.get().pass("Coupon campaign created successfully");

		// Coupon Lookup part
		pageObj.campaignspage().searchCampaign(couponCampaignName);
		String dynamicToken = pageObj.campaignspage().getDynamicToken();
		String dynamicUrl = pageObj.campaignspage().getDynamicUrl();
		String jwtCode = pageObj.campaignspage().getJWTCode(dynamicToken);
		String couponCode = pageObj.campaignspage().getCouponCode(dynamicUrl + jwtCode);
		*/

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api1 signup");
		TestListeners.extentTest.get().pass("API1 Signup is successful");
		logger.info("API1 Signup is successful");

		// Using coupon campaign whose Usage Type=Unlock rewards on Mobile App, Dynamic
		// Generation, Number of Guests=0, Uses per Guest=1000, Gift Type=Points (2.0),
		// Expiry Days=2.
		// Hit `/api2/dashboard/dynamic_coupons` to get Coupon code
		Response postDynamicCouponScheduledEmailResponse = pageObj.endpoints().postDynamicCoupon(dataSet.get("apiKey"),
				userEmail, dataSet.get("campaignUuid"));
		Assert.assertEquals(postDynamicCouponScheduledEmailResponse.getStatusCode(), 200, 
				"Status code not matched for Dashboard API Dynamic Coupon generation");
		String couponCode = postDynamicCouponScheduledEmailResponse.jsonPath().getString("coupon");
		Assert.assertTrue(!couponCode.isEmpty(), "Coupon code is empty");

		Thread.sleep(8000);
		// coupon redemption in mobile
		Response couponRedemptionResponse = pageObj.endpoints().couponRedemptionOnMobile(dataSet.get("client"),
				dataSet.get("secret"), token, couponCode);
		Assert.assertEquals(couponRedemptionResponse.getStatusCode(), 200,
				"Status code 200 did not matched for Coupon redemption on Mobile");
		TestListeners.extentTest.get().pass("Coupon redemption on Mobile is successful");
		logger.info("Coupon redemption on Mobile is successful");

		// timeline validation
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String redeemedCouponStatus = pageObj.guestTimelinePage().verifyRedeemedCouponCode();
		Assert.assertTrue(redeemedCouponStatus.contains(couponCode),
				"Rdeemed code details did not displayed  on timeline....");
		TestListeners.extentTest.get().pass("coupon campaign redeemed validated successfully on timeline");

	}

	@Test(description = "SQ-T3452 Verify redemption of a coupon when coupon is being used at pos and it is pre-generated", priority = 2, groups = {
			"unstable" })
	@Owner(name = "Ashwini Shetty")
	public void T3452_VerifyCouponInPos()
			throws InterruptedException, InvalidKeyException, UnsupportedEncodingException, ParseException {
		campaignName = "Coupon Campaign" + CreateDateTime.getTimeDateString();
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Campaigns Link
		// pageObj.menupage().clickCampaignsMenu();
		// pageObj.menupage().clickCampaignsLink();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value and create coupon campaign
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(campaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), "", "", false);
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Campaign created success message did not displayed....");
		TestListeners.extentTest.get().pass("Coupon campaign created successfully");
		pageObj.campaignspage().searchCampaign(campaignName);
		String generatedCodeName = pageObj.campaignspage().getPreGeneratedCuponCode();

		// user signup
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 1 user signup");
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api1 signup");
		TestListeners.extentTest.get().pass("API1 Signup is successful");
		logger.info("API1 Signup is successful");

		Thread.sleep(8000);
		// pos redemption of coupon
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response couponRedemptionResponse = pageObj.endpoints().posRedemptionOfCouponCode(userEmail, date,
				generatedCodeName, key, txn, dataSet.get("locationKey"));
		pageObj.apiUtils().verifyResponse(couponRedemptionResponse, "Coupon redemption on Mobile");
		Assert.assertEquals(couponRedemptionResponse.getStatusCode(), 200,
				"Status code 200 did not matched for Coupon redemption on Mobile");
		TestListeners.extentTest.get().pass("Coupon redemption on Mobile is successful");
		logger.info("Coupon redemption on Mobile is successful");

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String redeemedCouponStatus = pageObj.guestTimelinePage().verifyRedeemedCouponCode();
		Assert.assertTrue(redeemedCouponStatus.contains(generatedCodeName),
				"Rdeemed code details did not displayed  on timeline....");
		TestListeners.extentTest.get().pass("coupon campaign redeemed validated successfully on timeline");
	}

	@Test(description = "SQ-T3454 Verify redemption of a coupon when coupon is being used at pos and it is dynamic generated", priority = 3, groups = {
			"unstable" })
	@Owner(name = "Ashwini Shetty")
	public void T3454_VerifyCouponInPosForDynamic()
			throws InterruptedException, InvalidKeyException, UnsupportedEncodingException, ParseException {
		//String couponCampaignName = "Coupon Campaign" + CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		/*
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
		TestListeners.extentTest.get().pass("Coupon campaign created successfully");

		// Coupon Lookup part
		pageObj.campaignspage().searchCampaign(couponCampaignName);
		String dynamicToken = pageObj.campaignspage().getDynamicToken();
		String dynamicUrl = pageObj.campaignspage().getDynamicUrl();
		String jwtCode = pageObj.campaignspage().getJWTCode(dynamicToken);
		String couponCode = pageObj.campaignspage().getCouponCode(dynamicUrl + jwtCode);
		*/

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api1 signup");
		TestListeners.extentTest.get().pass("API1 Signup is successful");
		logger.info("API1 Signup is successful");

		// Using coupon campaign whose Usage Type=Directly processed at POS, Dynamic
		// Generation, Number of Guests=0, Uses per Guest=1000, Gift Type=$ Off, Amount
		// Discount=2, Expiry Days=2.
		// Hit `/api2/dashboard/dynamic_coupons` to get Coupon code
		Response postDynamicCouponScheduledEmailResponse = pageObj.endpoints().postDynamicCoupon(dataSet.get("apiKey"),
				userEmail, dataSet.get("campaignUuid"));
		Assert.assertEquals(postDynamicCouponScheduledEmailResponse.getStatusCode(), 200, 
				"Status code not matched for Dashboard API Dynamic Coupon generation");
		String couponCode = postDynamicCouponScheduledEmailResponse.jsonPath().getString("coupon");
		Assert.assertTrue(!couponCode.isEmpty(), "Coupon code is empty");

		Thread.sleep(8000);
		// pos redemption of coupon
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response couponRedemptionResponse = pageObj.endpoints().posRedemptionOfCouponCode(userEmail, date, couponCode,
				key, txn, dataSet.get("locationKey"));
		Assert.assertEquals(couponRedemptionResponse.getStatusCode(), 200,
				"Status code 200 did not matched for Coupon redemption on Mobile");
		TestListeners.extentTest.get().pass("Coupon redemption on Mobile is successful");
		logger.info("Coupon redemption on Mobile is successful");

		// timeline validation
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String redeemedCouponStatus = pageObj.guestTimelinePage().verifyRedeemedCouponCode();
		Assert.assertTrue(redeemedCouponStatus.contains(couponCode),
				"Rdeemed code details did not displayed  on timeline....");
		TestListeners.extentTest.get().pass("coupon campaign redeemed validated successfully on timeline");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().deleteCampaignFromDb(campaignName, env);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}
