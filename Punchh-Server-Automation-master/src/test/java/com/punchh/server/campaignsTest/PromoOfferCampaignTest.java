package com.punchh.server.campaignsTest;

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
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class PromoOfferCampaignTest {
	private static Logger logger = LogManager.getLogger(PromoOfferCampaignTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private String userEmail;
	private static Map<String, String> dataSet;
	private String promoCampaignName;
	String run = "ui";
	ApiUtils apiUtils;

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
		logger.info(sTCName + " ==>" + dataSet);
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T5383 Verify redemption of a Promo when promo is being used in Unlock rewards on Mobile App ||"
			+ "SQ-T5386 verify user is able to attach email templates in campaign", priority = 0, groups = {
					"unstable" })
	@Owner(name = "Hardik Bhardwaj")
	public void T5383_VerifyPromoInMobileApp() throws InterruptedException, ParseException {
		promoCampaignName = "Automation Promo Campaign" + CreateDateTime.getTimeDateString();
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value and create promo campaign
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		String promoCode = "P" + CreateDateTime.getTimeDateAsneed("HHmmssddMM");
		pageObj.signupcampaignpage().createWhatDetailsPromoCampaign(promoCampaignName, promoCode);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType")); // POS
		pageObj.signupcampaignpage().setPromoCampaignNoOfGuests("4");
		pageObj.signupcampaignpage().setPromoCampaignUsagePerGuest("2");
		pageObj.signupcampaignpage().promoGiftType("Gift Redeemable", "Base Redeemable");
		pageObj.signupcampaignpage().setCampaignGiftreason(promoCampaignName);
		pageObj.signupcampaignpage().setCampaignPushNotification(promoCampaignName);
		pageObj.signupcampaignpage().selectEmailTemplate("Do Not Delete Automation Email Template");
		pageObj.signupcampaignpage().setCampaignEmailSubject("Email Subject for " + promoCampaignName);
		pageObj.signupcampaignpage().clickNextButton();

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().setCamTimeZone("(GMT+05:30) New Delhi ( IST )");
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Campaign created success message did not displayed....");
		TestListeners.extentTest.get().pass("Promo campaign created successfully");

		// user creation using api2
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// promo redemption in mobile
		Response promoRedemptionResponse = pageObj.endpoints().couponRedemptionOnMobile(dataSet.get("client"),
				dataSet.get("secret"), token, promoCode);
		pageObj.apiUtils().verifyResponse(promoRedemptionResponse, "Coupon redemption on Mobile");
		Assert.assertEquals(promoRedemptionResponse.getStatusCode(), 200,
				"Status code 200 did not matched for promo redemption on Mobile");
		TestListeners.extentTest.get().pass("Promo redemption on Mobile is successful");
		logger.info("Promo redemption on Mobile is successful");

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String redeemedCouponStatus = pageObj.guestTimelinePage().verifyRedeemedCouponCode();
		Assert.assertTrue(redeemedCouponStatus.contains(promoCode),
				"Rdeemed code details did not displayed  on timeline....");
		TestListeners.extentTest.get().pass("promo campaign redeemed validated successfully on timeline");
		logger.info("promo campaign redeemed validated successfully on timeline");

		// Delete Promo Campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns"); // delete
		 * promo campaign pageObj.campaignspage().searchCampaign(promoCampaignName);
		 * pageObj.campaignspage().deactivateOrDeleteTheCoupon("delete");
		 */
	}

	@Test(description = "SQ-T5385 Verify redemption of a Promo when promo is being used in Directly processed at POS", groups = {
			"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T5385_VerifyPromoInPOS() throws InterruptedException, ParseException {
		promoCampaignName = "Automation Promo Campaign" + CreateDateTime.getTimeDateString();
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value and create promo campaign
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		String promoCode = "P" + CreateDateTime.getTimeDateAsneed("HHmmssddMM");
		pageObj.signupcampaignpage().createWhatDetailsPromoCampaign(promoCampaignName, promoCode);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setPromoCampaignNoOfGuests("4");
		pageObj.signupcampaignpage().promoGiftType("$ OFF", "4");
		pageObj.signupcampaignpage().setPrompt("Short Prompt", "Promo Short Prompt");
		pageObj.signupcampaignpage().setPrompt("Standard Prompt", "Standard Prompt for Automation Promo");
		pageObj.signupcampaignpage().promoQC(dataSet.get("qcName"));
		pageObj.signupcampaignpage().clickNextButton();

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().setCamTimeZone("(GMT+05:30) New Delhi ( IST )");
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Campaign created success message did not displayed....");
		TestListeners.extentTest.get().pass("Promo campaign created successfully");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 1 user signup");
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api1 signup");
		TestListeners.extentTest.get().pass("API1 Signup is successful");
		logger.info("API1 Signup is successful");

		// pos redemption of promo
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response promoRedemptionResponse = pageObj.endpoints().posRedemptionOfCouponCode(userEmail, date, promoCode,
				key, txn, dataSet.get("locationkey"));
		pageObj.apiUtils().verifyResponse(promoRedemptionResponse, "Coupon redemption on Mobile");
		Assert.assertEquals(promoRedemptionResponse.getStatusCode(), 200,
				"Status code 200 did not matched for Promo redemption on Mobile");
		TestListeners.extentTest.get().pass("Promo redemption on Mobile is successful");
		logger.info("Promo redemption on Mobile is successful");

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String redeemedPromoStatus = pageObj.guestTimelinePage().verifyRedeemedCouponCode();
		Assert.assertTrue(redeemedPromoStatus.contains(promoCode),
				"Rdeemed code details did not displayed  on timeline....");
		TestListeners.extentTest.get().pass("Promo campaign redeemed validated successfully on timeline");

		// Delete Promo Campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * 
		 * // delete promo campaign
		 * pageObj.campaignspage().searchCampaign(promoCampaignName);
		 * pageObj.campaignspage().deactivateOrDeleteTheCoupon("delete");
		 */
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().deleteCampaignFromDb(promoCampaignName, env);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}
