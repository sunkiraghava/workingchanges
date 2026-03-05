package com.punchh.server.LP1Test;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Statement;
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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.ApiPayloadObj;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class SubscriptionPlanPurchaseTest {
	static Logger logger = LogManager.getLogger(SubscriptionPlanPurchaseTest.class);
	public WebDriver driver;
	String userEmail;
	String email = "autoemailTemp@punchh.com";
	ApiUtils apiUtils;
	PageObj pageObj;
	String sTCName;
	private String timeStamp, QCname, spPrice, spName, PlanID, iFrameEmail, txn, date, key;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String amountcap, unitDiscount;
	private boolean GlobalBenefitRedemptionThrottlingToggle;
	Properties prop;
	private Utilities utils;
	private Connection conn;
	private Statement stmt;
	private String endDateTime;
	SeleniumUtilities selUtils;
	private ApiPayloadObj apipayloadObj;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// single login to instance
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
		endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";
		selUtils = new SeleniumUtilities(driver);
		apiUtils = new ApiUtils();
		utils = new Utilities(driver);
		GlobalBenefitRedemptionThrottlingToggle = false;
		timeStamp = CreateDateTime.getTimeDateString();
		userEmail = email.replace("Temp", timeStamp);
		// move to All Businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	// need to change in flow --
	@Test(description = "SQ-T3553 Verify the Subscription plan is purchased with Heartland adapter and renewed via scheduler", groups = {
			"regression" }, priority = 1)
	@Owner(name = "Shashank Sharma")
	public void T3553_ValidateRenewalSubscriptionPlanWithHeartlandPaymentAdapter() throws InterruptedException {
		String UUID = dataSet.get("uuid");
		iFrameEmail = dataSet.get("userEmailID");

		logger.info("== START -- T2798 Validate The Renew subscription plan ==");
		spName = "SubcriptionPlan_" + CreateDateTime.getTimeDateString();
		QCname = "QcSubscription_" + CreateDateTime.getTimeDateString();
		amountcap = Integer.toString(Utilities.getRandomNoFromRange(10, 200));
		unitDiscount = Integer.toString(Utilities.getRandomNoFromRange(1, 10));
		spPrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));
		int expSpPrice = Integer.parseInt(spPrice);

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Subscriptions");
		pageObj.dashboardpage().setPaymentAdapterInSubscriptionPage("Heartland v2.0");
		pageObj.dashboardpage().clickOnUpdateButton();

		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().createQualificationCriteria(QCname, amountcap,
				dataSet.get("qcFucntionName"), unitDiscount, true, dataSet.get("lineItemSelectorName"));

		// pageObj.menupage().clickSubscriptionsMenuIcon();
		// pageObj.menupage().clickSubscriptionPlansLink();
		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Plans");
		pageObj.subscriptionPlansPage().createSubscriptionPlan(spName, spPrice, QCname, dataSet.get("qcFucntionName"),
				GlobalBenefitRedemptionThrottlingToggle, endDateTime, false);
		PlanID = pageObj.subscriptionPlansPage().getSbuscriptionPlanID(spName);

		Response signInResponse = pageObj.endpoints().Api1UserLogin(dataSet.get("userEmailID"), dataSet.get("client"),
				dataSet.get("secret"));

		String auth_token = signInResponse.jsonPath().get("auth_token.token").toString();
		// single token
		Response singleTokenResponse = pageObj.endpoints().generateHeartlandPaymentToken(dataSet.get("adminToken"),
				dataSet.get("heartLandApikey"));
		Assert.assertEquals(singleTokenResponse.statusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Not able to generate single Token for heartland adaptor");
		String finalSingleScanToken = singleTokenResponse.jsonPath().getString("token_value");
		pageObj.utils().logit("Heartland Single token is generated: " + finalSingleScanToken);

		// generate UUID
		Response posPaymentCard = pageObj.endpoints().POSPaymentCard(dataSet.get("client"), dataSet.get("secret"),
				auth_token, finalSingleScanToken);
		Assert.assertEquals(posPaymentCard.statusCode(), ApiConstants.HTTP_STATUS_OK);
		String uuid = posPaymentCard.jsonPath().get("uuid").toString();

		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(auth_token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, uuid, "Heartland");
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);

		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();

		// scheduler run now --

		// Remove API part

		// Verify subscription on UI , user timeline

		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		pageObj.guestTimelinePage().navigateToTabs("Subscriptions");

		String actualSubscriptionPlanName = pageObj.guestTimelinePage().getSubscriptionPlansFromTimeline();
		Assert.assertEquals(actualSubscriptionPlanName, spName);
		logger.info("Verified the subscription name " + spName + " on timeline subscription page");
		TestListeners.extentTest.get()
				.pass("Verified the subscription name " + spName + " on timeline subscription page");

		int actualSbuscriptionPlanPrice = pageObj.guestTimelinePage().getSubscriptionPlanPriceFromTimeline();
		Assert.assertEquals(actualSbuscriptionPlanPrice, expSpPrice);
		logger.info(
				"Verified the subscription price " + actualSbuscriptionPlanPrice + " on timeline subscription page");
		pageObj.utils().logPass(
				"Verified the subscription price " + actualSbuscriptionPlanPrice + " on timeline subscription page");

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Subscription Renewal Schedule");
		pageObj.schedulespage().findMassCampaignNameandRun(spName);

		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

//		Response renewalSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchaseRenew("EyUjwUNtFzMjDSQvy5Mr",
//				subscription_id, dataSet.get("client"), dataSet.get("secret"), spPrice, "Heartland");
//
//		Assert.assertEquals(renewalSubscriptionresponse.getStatusCode(), 200);
//		int renewed_subscription_id = Integer
//				.parseInt(renewalSubscriptionresponse.jsonPath().get("subscription_id").toString());
//		pageObj.driver.navigate().refresh();
//		int actualSubscriptionID = pageObj.guestTimelinePage().getSubscriptionRenewID();
//
//		Assert.assertEquals(actualSubscriptionID, renewed_subscription_id);

//		logger.info("Verified the subscription renew ID  " + actualSubscriptionID + " on timeline subscription page");
//		TestListeners.extentTest.get()
//				.pass("Verified the subscription renew ID  " + actualSubscriptionID + " on timeline subscription page");

		logger.info("== END -- T2798 Validate The Renew subscription plan ==");

	}

	@Test(description = "LS-T26 - To verify dicount from 2nd discounting rule ;"
			+ "LS-T27 Combination with rate roll back")
	@Owner(name = "Shashank Sharma")
	public void LST26_LST27_verifySubscriptionDicountCombinationOfdiscountingRule() throws InterruptedException {
		spName = "SubcriptionPlan_" + CreateDateTime.getTimeDateString();
		QCname = "QcSubscription1_" + CreateDateTime.getTimeDateString();
		String QCName2 = "QcSubscription2_" + CreateDateTime.getTimeDateString();

		amountcap = Integer.toString(Utilities.getRandomNoFromRange(10, 200));
		unitDiscount = Integer.toString(Utilities.getRandomNoFromRange(1, 10));
		spPrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));

		String amountcap2 = Integer.toString(Utilities.getRandomNoFromRange(10, 200));
		String unitDiscount2 = Integer.toString(Utilities.getRandomNoFromRange(1, 10));
		String spPrice2 = Integer.toString(Utilities.getRandomNoFromRange(300, 500));

		GlobalBenefitRedemptionThrottlingToggle = false;
		txn = "123456" + CreateDateTime.getTimeDateString();
		date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		key = CreateDateTime.getTimeDateString();

		// Thread.sleep(2000);

		String txn2 = "123456" + CreateDateTime.getTimeDateString();
		String date2 = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key2 = CreateDateTime.getTimeDateString();

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Subscriptions");
		pageObj.dashboardpage()
				.checkUncheckFlagOnCockpitDasboardOLOPage("Enable Extended Credits On Subscription Plans?", "uncheck");

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Subscriptions");
		pageObj.dashboardpage().setPaymentAdapterInSubscriptionPage("No Adapter");
		pageObj.dashboardpage().clickOnUpdateButton();

		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");

		// Creating QC1
		pageObj.qualificationcriteriapage().createQualificationCriteria(QCname, amountcap,
				dataSet.get("qcFucntionName"), unitDiscount, true, dataSet.get("lineItemSelectorName"));

		pageObj.menupage().clickDashboardMenu();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");

		// Creating QC2
		pageObj.qualificationcriteriapage().createQualificationCriteria(QCName2, amountcap2,
				dataSet.get("qcFucntionName2"), amountcap2, GlobalBenefitRedemptionThrottlingToggle,
				dataSet.get("lineItemSelectorName2"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Subscriptions");
		pageObj.dashboardpage().setPaymentAdapterInSubscriptionPage("No Adapter");
		pageObj.dashboardpage().clickOnUpdateButton();

		// pageObj.menupage().clickSubscriptionsMenuIcon();
		// pageObj.menupage().clickSubscriptionPlansLink();
		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Plans");

		pageObj.subscriptionPlansPage().createSubscriptionPlanWithSecondDiscountingRule(spName, spPrice, QCname,
				dataSet.get("subscriptionQCFunctionName"), dataSet.get("subscriptionQCFunctionName2"), QCName2,
				GlobalBenefitRedemptionThrottlingToggle);
		PlanID = pageObj.subscriptionPlansPage().getSbuscriptionPlanID(spName);

		// user signup using api2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				iFrameEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		// subscription purchase api 2
		int counter = 0;
		boolean flag = false;
		Response purchaseSubscriptionresponse;
		do {
			purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
					dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
			try {
				Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
				flag = true;
				break;
			} catch (AssertionError ae) {
				flag = false;
				counter++;
				selUtils.longWait(2000);
			}
		} while (flag || counter <= 20);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
		logger.info(iFrameEmail + " purchased " + subscription_id + " Plan id = " + PlanID);

		Response resp = pageObj.endpoints().posRedemptionOfSubscription(iFrameEmail, date, subscription_id, key, txn,
				dataSet.get("locationkey"), amountcap, "101");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos redemption api");
		double redemption_amount1 = Double.parseDouble(resp.jsonPath().getDouble("redemption_amount") + "");

		Assert.assertEquals(redemption_amount1, Double.parseDouble(amountcap));
		logger.info("Verified that First redemption is happen from 1st discounting rule applied in subscription");
		TestListeners.extentTest.get()
				.pass("Verified that First redemption is happen from 1st discounting rule applied in subscription");

		Thread.sleep(8000);

		Response resp2 = pageObj.endpoints().posRedemptionOfSubscription(iFrameEmail, date, subscription_id, key2, txn2,
				dataSet.get("locationkey"), amountcap2, "102");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos redemption api");

		double redemption_amount2 = Double.parseDouble(resp2.jsonPath().getDouble("redemption_amount") + "");

		Assert.assertEquals(redemption_amount2, Double.parseDouble(amountcap2));
		logger.info("Verified that First redemption is happen from 2nd discounting rule applied in subscription");

		TestListeners.extentTest.get()
				.pass("Verified that 2nd redemption is happen from 2nd discounting rule applied in subscription");

	}

	// Anant
	@Test(description = "SQ-T4065 Verify the subscription payment credential in API V2 mobile meta")
	@Owner(name = "Shashank Sharma")
	public void T4065_subscriptionPaymentCredentialAPIV2Mobile() throws InterruptedException {

		pageObj.utils().logit("============== HeartLand related Test Case ==============");
		// Login to instance
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Subscriptions");

		// select par_payment
		pageObj.dashboardpage().setPaymentAdapterInSubscriptionPage("PAR Payment");
		pageObj.dashboardpage().clickOnUpdateButton();
		utils.longWaitInMiliSeconds(10);
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Integration Services");
//		pageObj.dashboardpage().navigateToTabs("PAR Payments");
//		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("integration_service_enable_vault_par_payment", "check");
//		pageObj.whitelabelPage().clickUpdateParPayment();
		int attempt = 0;
		while (attempt <= 10) {
			Response parPayment = pageObj.endpoints().metaAPI2SubscriptionCancelReason(dataSet.get("client"),
					dataSet.get("secret"));
			Assert.assertEquals(parPayment.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
			String paymentCredential1 = parPayment.jsonPath().get("subscriptions_payment_credential").toString();
			if (paymentCredential1.equalsIgnoreCase("{adapter_code=par_payment, enable_recurring_payments=true}")) {
				Assert.assertEquals(paymentCredential1, "{adapter_code=par_payment, enable_recurring_payments=true}");
				break;
			}
			utils.longWaitInMiliSeconds(2);
			attempt++;

		}
		// select heartland
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Subscriptions");
		pageObj.dashboardpage().setPaymentAdapterInSubscriptionPage("Heartland v2.0");
		pageObj.dashboardpage().clickOnUpdateButton();
		Thread.sleep(1000);
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Integration Services");
		pageObj.dashboardpage().navigateToTabs("Heartland");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("integration_service_enable_vault_heartland", "check");
		pageObj.whitelabelPage().clickUpdateHeartland();
		while (attempt <= 15) {
			Response heartLand = pageObj.endpoints().metaAPI2SubscriptionCancelReason(dataSet.get("client"),
					dataSet.get("secret"));
			Assert.assertEquals(heartLand.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
			String paymentCredential2 = heartLand.jsonPath().get("subscriptions_payment_credential").toString();

			if (paymentCredential2.equalsIgnoreCase("{adapter_code=par_payment, enable_recurring_payments=true}")) {
				Assert.assertEquals(paymentCredential2, "{adapter_code=par_payment, enable_recurring_payments=true}");
				break;
			}
			utils.longWaitInMiliSeconds(2);
			attempt++;
		}
		// select No Adapter
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Subscriptions");
		pageObj.dashboardpage().setPaymentAdapterInSubscriptionPage("No Adapter");
		Thread.sleep(1000);
		pageObj.dashboardpage().clickOnUpdateButton();
		Response noAdapter = pageObj.endpoints().metaAPI2SubscriptionCancelReason(dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(noAdapter.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// select heartland
		pageObj.dashboardpage().setPaymentAdapterInSubscriptionPage("Heartland v2.0");
		pageObj.dashboardpage().clickOnUpdateButton();
	}

	// merged with T3999_subscriptionCodeNotGeneratedExpiredSubscription
	// Anant
//	@Test(description = "SQ-T4000 When the Subscription date is in future, the redemption_code should not be generated")
	@Owner(name = "Shashank Sharma")
	public void T4000_subcriptionDateInFuture() throws InterruptedException {
		// Login to instance
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		spName = "SubcriptionPlan_" + CreateDateTime.getTimeDateString();
		QCname = "QcSubscription_" + CreateDateTime.getTimeDateString();
		amountcap = Integer.toString(Utilities.getRandomNoFromRange(10, 200));
		unitDiscount = Integer.toString(Utilities.getRandomNoFromRange(1, 10));
		spPrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Subscriptions");
		pageObj.dashboardpage().setPaymentAdapterInSubscriptionPage("No Adapter");
		pageObj.dashboardpage().clickOnUpdateButton();
		Thread.sleep(5000);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");

		pageObj.qualificationcriteriapage().createQualificationCriteria(QCname, amountcap,
				dataSet.get("qcFucntionName"), unitDiscount, true, dataSet.get("lineItemSelectorName"));

		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Plans");
		pageObj.subscriptionPlansPage().createSubscriptionPlanWithFutureDate(spName, spPrice, QCname,
				dataSet.get("qcFucntionName"), GlobalBenefitRedemptionThrottlingToggle);
		PlanID = pageObj.subscriptionPlansPage().getSbuscriptionPlanID(spName);

		// user signup using api2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				iFrameEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchaseFutureDate(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
		logger.info(iFrameEmail + " purchased " + subscription_id + " Plan id = " + PlanID);
		Response card2 = pageObj.endpoints().Api2SubscriptionRedemption(dataSet.get("client"), dataSet.get("secret"),
				subscription_id, token);

		String error = card2.jsonPath().get("error").toString();
		Assert.assertEquals("Redemption Code cannot be generated for Subscription with Start date in future.", error);
		TestListeners.extentTest.get()
				.pass("redemption code is not generated for an active Subscription which has start date in future");

	}

	// Anant
	@Test(description = "SQ-T3999 Verify the user is not able to generate the redemption_code if the Subscription has expired"
			+ "SQ-T4062 Verify the API2 for expired, hard/soft cancelled and renewed past subscriptions,"
			+ "SQ-T4063 Verify the API1 secure for expired, hard/soft cancelled and renewed past subscriptions"
			+ "SQ-T4064 Verify the Auth api for hard/soft cancelled, renewed, expired past subscriptions || "
			+ "SQ-T4000 When the Subscription date is in future, the redemption_code should not be generated")
	@Owner(name = "Shashank Sharma")
	public void T3999_subscriptionCodeNotGeneratedExpiredSubscription() throws Exception {
		spName = "SubcriptionPlan_" + CreateDateTime.getTimeDateString();
		QCname = "QcSubscription_" + CreateDateTime.getTimeDateString();
		amountcap = Integer.toString(Utilities.getRandomNoFromRange(10, 200));
		unitDiscount = Integer.toString(Utilities.getRandomNoFromRange(1, 10));
		spPrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Subscriptions");
		pageObj.dashboardpage().setPaymentAdapterInSubscriptionPage("No Adapter");
		pageObj.dashboardpage().clickOnUpdateButton();
		Thread.sleep(10000);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");

		pageObj.qualificationcriteriapage().createQualificationCriteria(QCname, amountcap,
				dataSet.get("qcFucntionName"), unitDiscount, true, dataSet.get("lineItemSelectorName"));

		// pageObj.menupage().clickSubscriptionsMenuIcon();
		// pageObj.menupage().clickSubscriptionPlansLink();
		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Plans");
		pageObj.subscriptionPlansPage().createSubscriptionPlan(spName, spPrice, QCname, dataSet.get("qcFucntionName"),
				GlobalBenefitRedemptionThrottlingToggle, "", false);
		PlanID = pageObj.subscriptionPlansPage().getSbuscriptionPlanID(spName);

		// user signup using api2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				iFrameEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		// subscription purchase api 2
		Response purchaseSubscriptionresponseFutureDate = pageObj.endpoints().Api2SubscriptionPurchaseFutureDate(token,
				PlanID, dataSet.get("client"), dataSet.get("secret"), spPrice);
		Assert.assertEquals(purchaseSubscriptionresponseFutureDate.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_idFutureDate = purchaseSubscriptionresponseFutureDate.jsonPath().get("subscription_id")
				.toString();
		logger.info(iFrameEmail + " purchased " + subscription_idFutureDate + " Plan id = " + PlanID);
		Response card2 = pageObj.endpoints().Api2SubscriptionRedemption(dataSet.get("client"), dataSet.get("secret"),
				subscription_idFutureDate, token);

		String errorFutureDate = card2.jsonPath().get("error").toString();
		Assert.assertEquals("Redemption Code cannot be generated for Subscription with Start date in future.",
				errorFutureDate);
		TestListeners.extentTest.get()
				.pass("redemption code is not generated for an active Subscription which has start date in future");

		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, "");
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
		logger.info(iFrameEmail + " purchased " + subscription_id + " Plan id = " + PlanID);

		String query = "update user_subscriptions set start_time = '2022-02-19 05:09:38',end_time = '2022-02-21 05:09:38' where id = '"
				+ subscription_id + "'";
		logger.info(query);
		pageObj.utils().logit(query);
		DBUtils.executeUpdateQuery(env, query);

		Response pastSubscription = pageObj.endpoints().Api2UserSubscriptionWithPastSubscription(token,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(pastSubscription.statusCode(), ApiConstants.HTTP_STATUS_OK, "api2 is not showing the past subscriptions");

		Response pastSubscription2 = pageObj.endpoints().Api1UserSubscriptionWithPastSubscription(token,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(pastSubscription2.statusCode(), ApiConstants.HTTP_STATUS_OK, "api1 is not showing the past subscriptions");

		Response pastSubscription3 = pageObj.endpoints().AuthUserSubscriptionWithPastSubscription(token,
				dataSet.get("client"), dataSet.get("secret"), "past_subscriptions");
		Assert.assertEquals(pastSubscription3.statusCode(), ApiConstants.HTTP_STATUS_OK, "api1 is not showing the past subscriptions");
		boolean isAuthFetchSubscriptionPlansSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authFetchSubscriptionPlansSchema, pastSubscription3.asString());
		Assert.assertTrue(isAuthFetchSubscriptionPlansSchemaValidated,
				"Auth Fetch Subscription Plans API schema validation failed");

		Response card = pageObj.endpoints().Api2SubscriptionRedemption(dataSet.get("client"), dataSet.get("secret"),
				subscription_id, token);
		Assert.assertEquals(card.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		String error = "Redemption Code cannot be generated for expired Subscription.";
		String errorMsgExp = card.jsonPath().get("error").toString();
		Assert.assertEquals(error, errorMsgExp,
				"not getting correct error msg when trying to generate redemption code for the expired subscripption");
		pageObj.utils().logPass("redemption code is not generated for an expired Subscription");
//		dbUtils.closeConnection();
	}

	// anant
	@Test(description = "SQ-T4020 Verify when user purchase a subscription plan,logs are displayed in Subscription Logs and "
			+ "SQ-T4024 Verify the Subscription purchase is failed, it appears in the Subscription logs")
	@Owner(name = "Shashank Sharma")
	public void T4020_subscriptionLogsDisplay() throws InterruptedException {
		String UUID = dataSet.get("uuid");
		iFrameEmail = dataSet.get("userEmailID");

		logger.info("== START -- T2798 Validate The Renew subscription plan ==");
		spName = "SubcriptionPlan_" + CreateDateTime.getTimeDateString();
		QCname = "QcSubscription_" + CreateDateTime.getTimeDateString();
		amountcap = Integer.toString(Utilities.getRandomNoFromRange(10, 200));
		unitDiscount = Integer.toString(Utilities.getRandomNoFromRange(1, 10));
		spPrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Subscriptions");
		pageObj.dashboardpage().setPaymentAdapterInSubscriptionPage("Heartland v2.0");
		pageObj.dashboardpage().clickOnUpdateButton();
		Thread.sleep(5000);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().createQualificationCriteria(QCname, amountcap,
				dataSet.get("qcFucntionName"), unitDiscount, true, dataSet.get("lineItemSelectorName"));

		// pageObj.menupage().clickSubscriptionsMenuIcon();
		// pageObj.menupage().clickSubscriptionPlansLink();
		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Plans");
		pageObj.subscriptionPlansPage().createSubscriptionPlan(spName, spPrice, QCname, dataSet.get("qcFucntionName"),
				GlobalBenefitRedemptionThrottlingToggle, "", false);
		PlanID = pageObj.subscriptionPlansPage().getSbuscriptionPlanID(spName);

		Response signInResponse = pageObj.endpoints().Api1UserLogin(dataSet.get("userEmailID"), dataSet.get("client"),
				dataSet.get("secret"));
		System.out.println("signInResponse:" + signInResponse.asPrettyString());

		String auth_token = signInResponse.jsonPath().get("auth_token.token").toString();

		// single token
		Response singleTokenResponse = pageObj.endpoints().generateHeartlandPaymentToken("",
				dataSet.get("heartLandApikey"));
		Assert.assertEquals(singleTokenResponse.statusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Not able to generate single Token for heartland adaptor");
		String finalSingleScanToken = singleTokenResponse.jsonPath().getString("token_value");
		pageObj.utils().logit("Heartland Single token is generated: " + finalSingleScanToken);

		// generate UUID
		Response posPaymentCard = pageObj.endpoints().POSPaymentCard(dataSet.get("client"), dataSet.get("secret"),
				auth_token, finalSingleScanToken);
		Assert.assertEquals(posPaymentCard.statusCode(), ApiConstants.HTTP_STATUS_OK);
		String uuid = posPaymentCard.jsonPath().get("uuid").toString();

		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(auth_token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, uuid, "Heartland");
//
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();

		pageObj.menupage().navigateToSubMenuItem("Support", "Subscription Log");
		String logStatus = pageObj.subscriptionPlansPage().userLogsStatus(spName);
		System.out.println(logStatus);
		Assert.assertTrue(logStatus.contains("Subscription Purchase Successful"), "logs are not visible");
		pageObj.utils().logPass("subscription purchase logs are visible");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		pageObj.guestTimelinePage().navigateToTabs("Subscriptions");

		pageObj.guestTimelinePage().clickOnSubscriptionCancelUsingSubscriptionName(dataSet.get("cancelType"), spName);
		// pageObj.guestTimelinePage().setCancellationFeedbackInTextArea(dataSet.get("cancellationFeedback"));
		pageObj.guestTimelinePage().accecptSubscriptionCancellation(dataSet.get("cancelReason"));

		pageObj.menupage().navigateToSubMenuItem("Support", "Subscription Log");
		logStatus = pageObj.subscriptionPlansPage().userLogsStatus(spName);
		System.out.println(logStatus);
		Assert.assertTrue(logStatus.contains("Subscription Cancellation"), "subscription cancel logs are not visible");
		pageObj.utils().logPass("subscription cancel logs are visible");
	}

	// Anant
	@Test(description = "SQ-T4055 Verify the validations for Purchase Subscription API")
	@Owner(name = "Shashank Sharma")
	public void T4055_validationsPurchaseSubscriptionAPI() throws InterruptedException {
		spName = "SubcriptionPlan_" + CreateDateTime.getTimeDateString();
		QCname = "QcSubscription_" + CreateDateTime.getTimeDateString();
		amountcap = Integer.toString(Utilities.getRandomNoFromRange(10, 200));
		unitDiscount = Integer.toString(Utilities.getRandomNoFromRange(1, 10));
		spPrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Subscriptions");
		pageObj.dashboardpage().setPaymentAdapterInSubscriptionPage("Heartland v2.0");
		pageObj.dashboardpage().clickOnUpdateButton();
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Integration Services");
		pageObj.dashboardpage().navigateToTabs("Heartland");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("integration_service_enable_vault_heartland", "uncheck");
		pageObj.whitelabelPage().clickUpdateHeartland();
		Thread.sleep(5000);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");

//		pageObj.qualificationcriteriapage().createQualificationCriteria(QCname, amountcap,
//				dataSet.get("qcFucntionName"), unitDiscount, true, dataSet.get("lineItemSelectorName"));

		pageObj.qualificationcriteriapage().createQualificationCriteria(QCname, amountcap,
				dataSet.get("qcFucntionName"), unitDiscount, true, dataSet.get("lineItemSelectorName"));

		// pageObj.menupage().clickSubscriptionsMenuIcon();
		// pageObj.menupage().clickSubscriptionPlansLink();
		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Plans");
		pageObj.subscriptionPlansPage().createSubscriptionPlan(spName, spPrice, QCname, dataSet.get("qcFucntionName"),
				GlobalBenefitRedemptionThrottlingToggle, "", false);
		PlanID = pageObj.subscriptionPlansPage().getSbuscriptionPlanID(spName);

		// user signup using api2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				iFrameEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchaseWithOutDate(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		String error = purchaseSubscriptionresponse.jsonPath().get("errors.recurring_payment_off_for_adapter")
				.toString();
		Assert.assertEquals(error, "[Payment Service not available.]");

		pageObj.utils().logPass("correct error msg appear when recurring payment is off");

		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Integration Services");
		pageObj.dashboardpage().navigateToTabs("Heartland");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("integration_service_enable_vault_heartland", "uncheck");
		pageObj.whitelabelPage().clickUpdateHeartland();

	}

	// Merged with T4078_verifyAuthApiForVoidRedemptions
	// Anant
//	@Test(description = "SQ-T4047 Verify by gifting the Subscription plan, all the dates are appearing as per the timezone")
	@Owner(name = "Vansham Mishra")
	public void T4047_datesAppearingAsTimezone() throws InterruptedException, ParseException {
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		spName = "SubcriptionPlan_" + CreateDateTime.getTimeDateString();
		QCname = "QcSubscription_" + CreateDateTime.getTimeDateString();
		amountcap = Integer.toString(Utilities.getRandomNoFromRange(10, 200));
		unitDiscount = Integer.toString(Utilities.getRandomNoFromRange(1, 10));
		spPrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));

		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().createQualificationCriteria(QCname, amountcap,
				dataSet.get("qcFucntionName"), unitDiscount, true, dataSet.get("lineItemSelectorName"));

		// pageObj.menupage().clickSubscriptionsMenuIcon();
		// pageObj.menupage().clickSubscriptionPlansLink();
		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Plans");
		pageObj.subscriptionPlansPage().createSubscriptionPlan(spName, spPrice, QCname, dataSet.get("qcFucntionName"),
				GlobalBenefitRedemptionThrottlingToggle, endDateTime, false);
		PlanID = pageObj.subscriptionPlansPage().getSbuscriptionPlanID(spName);

		// user signup using api2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				iFrameEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);

		// nagivate to user timeline and go to subscription
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		pageObj.guestTimelinePage().navigateToTabs("Subscriptions");

		String displayDate = pageObj.guestTimelinePage().getSubscriptionEndDate();
		System.out.println(displayDate);
		String actualDate = CreateDateTime.convertToMonth(endDateTime);
		Assert.assertTrue(displayDate.contains(actualDate), "end date is not same");

		pageObj.utils().logPass("End date is same");
	}

	// Merged with T4078_verifyAuthApiForVoidRedemptions
//	@Test(groups = {
//			"regression" }, description = "SQ-T2795 Validate the Update subscription plan | SQ-T2796 Validate the Delete subscription plan")
	@Owner(name = "Vansham Mishra")
	public void T2795_96_ValidateUptationAndDeletionOfSubscriptionPlan() throws InterruptedException {
		spName = "SubcriptionPlan_" + CreateDateTime.getTimeDateString();
		QCname = "QcSubscription_" + CreateDateTime.getTimeDateString();
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
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
		// pageObj.menupage().clickSubscriptionsMenuIcon();
		// pageObj.menupage().clickSubscriptionPlansLink();
		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Plans");
		pageObj.subscriptionPlansPage().createSubscriptionPlan(spName, "450", QCname, dataSet.get("qcFucntionName"),
				GlobalBenefitRedemptionThrottlingToggle, endDateTime, false);
		// search subscription
		boolean result = pageObj.subscriptionPlansPage().subscriptionPlanAvailable(spName);
		Assert.assertTrue(result, "Subscription plan not available");
		pageObj.utils().logPass("Subscription plan available");
		// update subscription
		pageObj.subscriptionPlansPage().updateSubscriptionPlan(spName, dataSet.get("validity"));
		// verify updation
		String result1 = pageObj.subscriptionPlansPage().verifyupdateSubscriptionPlan(spName, dataSet.get("validity"));
		Assert.assertEquals(dataSet.get("validity"), result1);
		logger.info(spName + " is successfully verified that it is updated ");
		pageObj.utils().logPass(spName + " is successfully verified that it is updated ");
		// delete subscription
		pageObj.subscriptionPlansPage().deleteSubscriptionPlan(spName);
		// search subscription
		boolean result2 = pageObj.subscriptionPlansPage().subscriptionPlanAvailable(spName);
		Assert.assertFalse(result2, "Subscription plan available");
		pageObj.utils().logPass("Subscription plan not available");

	}

	// Anant
	@Test(description = "SQ-T4078 Verify the Auth api for void redemptions || "
			+ "SQ-T2795 Validate the Update subscription plan | SQ-T2796 Validate the Delete subscription plan || "
			+ "SQ-T4047 Verify by gifting the Subscription plan, all the dates are appearing as per the timezone")
	@Owner(name = "Shashank Sharma")
	public void T4078_verifyAuthApiForVoidRedemptions() throws InterruptedException, ParseException {
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Subscriptions");
		pageObj.dashboardpage().setPaymentAdapterInSubscriptionPage("No Adapter");
		pageObj.dashboardpage().clickOnUpdateButton();

		// create QC and subscription Plan
		spName = "SubcriptionPlan_" + CreateDateTime.getTimeDateString();
		QCname = "QcSubscription_" + CreateDateTime.getTimeDateString();
		amountcap = Integer.toString(Utilities.getRandomNoFromRange(10, 200));
		unitDiscount = Integer.toString(Utilities.getRandomNoFromRange(1, 10));
		spPrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));

		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().createQualificationCriteria(QCname, amountcap,
				dataSet.get("qcFucntionName"), unitDiscount, true, dataSet.get("lineItemSelectorName"));

		// pageObj.menupage().clickSubscriptionsMenuIcon();
		// pageObj.menupage().clickSubscriptionPlansLink();
		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Plans");
		pageObj.subscriptionPlansPage().createSubscriptionPlan(spName, spPrice, QCname, dataSet.get("qcFucntionName"),
				GlobalBenefitRedemptionThrottlingToggle, "", false);
		PlanID = pageObj.subscriptionPlansPage().getSbuscriptionPlanID(spName);

		// user signup using api2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		apiUtils.verifyCreateResponse(signUpResponse, "Auth API user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		String token = signUpResponse.jsonPath().get("access_token");

		// purchase subscription plan
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
		logger.info(iFrameEmail + " purchased " + subscription_id + " Plan id = " + PlanID);

		// nagivate to user timeline and go to subscription
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		pageObj.guestTimelinePage().navigateToTabs("Subscriptions");

		String displayDate = pageObj.guestTimelinePage().getSubscriptionEndDate();
		System.out.println(displayDate);
		String actualDate = CreateDateTime.convertToMonth(endDateTime);
		Assert.assertTrue(displayDate.contains(actualDate), "end date is not same");

		pageObj.utils().logPass("End date is same");

		// hit redemption api
		Response card1 = pageObj.endpoints().Api2SubscriptionRedemption(dataSet.get("client"), dataSet.get("secret"),
				subscription_id, token);
		Assert.assertEquals(card1.statusCode(), ApiConstants.HTTP_STATUS_CREATED);
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		// hit pos redemption
		utils.longWaitInSeconds(6);
		Response resp = pageObj.endpoints().posRedemptionOfSubscription(iFrameEmail, "2023-12-20T13:38:32Z",
				subscription_id, "145852076666992023", "1234455", dataSet.get("locationkey"), "8", "101");
		Assert.assertEquals(resp.statusCode(), ApiConstants.HTTP_STATUS_OK);
		String redemptionId = resp.jsonPath().get("redemption_id").toString();

		//
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		Response voidRedemptionResponse = pageObj.endpoints().authApiVoidRedemptions(authToken, dataSet.get("client"),
				dataSet.get("secret"), redemptionId);
		Assert.assertEquals(voidRedemptionResponse.statusCode(), ApiConstants.HTTP_STATUS_ACCEPTED);

		// update subscription
		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Plans");
		pageObj.subscriptionPlansPage().updateSubscriptionPlan(spName, dataSet.get("validity"));
		// verify updation
		String result1 = pageObj.subscriptionPlansPage().verifyupdateSubscriptionPlan(spName, dataSet.get("validity"));
		Assert.assertEquals(dataSet.get("validity"), result1);
		logger.info(spName + " is successfully verified that it is updated ");
		pageObj.utils().logPass(spName + " is successfully verified that it is updated ");
		// delete subscription
		pageObj.subscriptionPlansPage().deleteSubscriptionPlan(spName);
		// search subscription
		boolean result2 = pageObj.subscriptionPlansPage().subscriptionPlanAvailable(spName);
		Assert.assertFalse(result2, "Subscription plan available");
		pageObj.utils().logPass("Subscription plan not available");
	}

	// Anant
	@Test(description = "SQ-T4339 Purchase single use subscription with auto renewal as False"
			+ "SQ-T4353 Validate Single Use Subscription Flag enforce validations"
			+ "SQ-T4352 Validate the Single Use Subscription Flag in Subscription Plan creation(1st) page."
			+ "SQ-T4340 Renew the single use subscription with auto renewal as False"
			+ "SQ-T4338 Purchase single use subscription with auto renewal as True")
	@Owner(name = "Shashank Sharma")
	public void T4339_PurchaseSingleUseSubscription() throws InterruptedException {
		spName = "SubcriptionPlan_" + CreateDateTime.getTimeDateString();
		QCname = "QcSubscription_" + CreateDateTime.getTimeDateString();
		amountcap = Integer.toString(Utilities.getRandomNoFromRange(10, 200));
		unitDiscount = Integer.toString(Utilities.getRandomNoFromRange(1, 10));
		spPrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Plans");
		pageObj.subscriptionPlansPage().hintTextsingleUseFlag(dataSet.get("hintText"));
		pageObj.subscriptionPlansPage().checkSingleUseFlagAlertMsg(dataSet.get("msg1"), "ON");
		pageObj.subscriptionPlansPage().checkSingleUseFlagAlertMsg(dataSet.get("msg2"), "OFF");
		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Plans");

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Subscriptions");
		pageObj.dashboardpage().setPaymentAdapterInSubscriptionPage("No Adapter");
		pageObj.dashboardpage().clickOnUpdateButton();

		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");

		pageObj.qualificationcriteriapage().createQualificationCriteria(QCname, amountcap,
				dataSet.get("qcFucntionName"), unitDiscount, true, dataSet.get("lineItemSelectorName"));

		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Plans");
		pageObj.subscriptionPlansPage().createSubscriptionPlan(spName, spPrice, QCname, dataSet.get("qcFucntionName"),
				GlobalBenefitRedemptionThrottlingToggle, "", true);
		PlanID = pageObj.subscriptionPlansPage().getSbuscriptionPlanID(spName);

		// sign up a user
		Response AuthsignUpResponse = pageObj.endpoints().authApiSignUpPositive(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(AuthsignUpResponse.statusCode(), ApiConstants.HTTP_STATUS_CREATED, "user sign up fail");
		String authToken = AuthsignUpResponse.jsonPath().get("authentication_token").toString();
		String accessToken = AuthsignUpResponse.jsonPath().get("access_token").toString();
		String userID = AuthsignUpResponse.jsonPath().get("user_id").toString();

		// subscription purchase using auth api
		String startDateTime = CreateDateTime.getYesterdaysDate() + " 01:00:00";
		Response authSubscriptionresponse2 = pageObj.endpoints().authApiSubscriptionPurchase(authToken, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, startDateTime, endDateTime, "false");
		Assert.assertEquals(authSubscriptionresponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		logger.info("User will be able to purchase the subscription plan successfully using api");
		TestListeners.extentTest.get()
				.pass("User will be able to purchase the subscription plan successfully using api");

		// subscription purchase api 1
		Response purchaseSubscriptionresponse = pageObj.endpoints().ApiSubscriptionPurchase(accessToken, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime, "false");
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		logger.info("User will be able to purchase the subscription plan successfully using api2");
		TestListeners.extentTest.get()
				.pass("User will be able to purchase the subscription plan successfully using api2");

		// subscription purchase api 2
		Response purchaseSubscriptionresponse2 = pageObj.endpoints().Api2SubscriptionPurchaseAutorenewal(accessToken,
				PlanID, dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime, "false");
		Assert.assertEquals(purchaseSubscriptionresponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		logger.info("User will be able to purchase the subscription plan successfully using api2");
		TestListeners.extentTest.get()
				.pass("User will be able to purchase the subscription plan successfully using api2");

//		// subscription purchase using dashboard api 
		Response dashboardPurchaseSubscriptionresponse = pageObj.endpoints().dashboardSubscriptionPurchase(
				dataSet.get("apiKey"), PlanID, dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime,
				"false", userID);
		Assert.assertEquals(dashboardPurchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		logger.info("User will be able to purchase the subscription plan successfully using auth api");
		TestListeners.extentTest.get()
				.pass("User will be able to purchase the subscription plan successfully using auth api");
		String subscriptionId = dashboardPurchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();

//		//renew 
		Response renewalSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchaseRenew(dataSet.get("apiKey"),
				subscriptionId, dataSet.get("client"), dataSet.get("secret"), spPrice, "");
		Assert.assertEquals(renewalSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isDashboardSubscriptionRenewSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema3, renewalSubscriptionresponse.asString());
		Assert.assertTrue(isDashboardSubscriptionRenewSchemaValidated,
				"Platform Functions API Renew Subscription Schema Validation failed");
		String errorMsg = renewalSubscriptionresponse.jsonPath().get("errors.base[0]").toString();
		Assert.assertEquals(errorMsg, "Subscription cannot be renewed.", "Different error msg is display");
		logger.info("User not able to renew subscription as expected");
		pageObj.utils().logPass("User not able to renew subscription as expected");

		// send Auto Renewal as true
		String errorMsg1 = "";

		// subscription purchase using auth api
		String startDateTime2 = CreateDateTime.getYesterdaysDate() + " 01:00:00";
		Response authSubscriptionresponse = pageObj.endpoints().authApiSubscriptionPurchase(authToken, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, startDateTime2, endDateTime, "true");
		Assert.assertEquals(authSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isAuthSubscriptionPurchaseNonRenewSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema3, authSubscriptionresponse.asString());
		Assert.assertTrue(isAuthSubscriptionPurchaseNonRenewSchemaValidated,
				"Auth API Subscription purchase Schema Validation failed");
		errorMsg1 = authSubscriptionresponse.jsonPath().get("errors.base[0]").toString();
		Assert.assertEquals(errorMsg1,
				"This is a single use subscription and cannot be renewed automatically. Please check request to send 'auto_renewal' as 'false'.",
				"In correct error msg is displayed");
		logger.info(
				"User will be unable to purchase the subscription plan as expected when auto renewal is true in auth api");
		pageObj.utils().logPass(
				"User will be unable to purchase the subscription plan as expected when auto renewal is true in auth api");

		// subscription purchase api 1
		Response purchaseSubscriptionresponse3 = pageObj.endpoints().ApiSubscriptionPurchase(accessToken, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime, "true");
		Assert.assertEquals(purchaseSubscriptionresponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		errorMsg1 = purchaseSubscriptionresponse3.jsonPath().get("base[0]").toString();
		Assert.assertEquals(errorMsg1,
				"This is a single use subscription and cannot be renewed automatically. Please check request to send 'auto_renewal' as 'false'.",
				"error msg is not same");
		logger.info(
				"User will be unable to purchase the subscription plan as expected when auto renewal is true in api");
		pageObj.utils().logPass(
				"User will be unable to purchase the subscription plan as expected when auto renewal is true in api");

		// subscription purchase api 2
		Response purchaseSubscriptionresponse4 = pageObj.endpoints().Api2SubscriptionPurchaseAutorenewal(accessToken,
				PlanID, dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime, "true");
		Assert.assertEquals(purchaseSubscriptionresponse4.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		errorMsg1 = purchaseSubscriptionresponse4.jsonPath().get("errors.base[0]").toString();
		Assert.assertEquals(errorMsg1,
				"This is a single use subscription and cannot be renewed automatically. Please check request to send 'auto_renewal' as 'false'.",
				"error msg is not correct");
		logger.info(
				"User will be unable to purchase the subscription plan as expected when auto renewal is true in api 2");
		pageObj.utils().logPass(
				"User will be unable to purchase the subscription plan as expected when auto renewal is true in api 2");

//		// subscription purchase using dashboard api 
		Response dashboardPurchaseSubscriptionresponse2 = pageObj.endpoints().dashboardSubscriptionPurchase(
				dataSet.get("apiKey"), PlanID, dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime,
				"true", userID);
		Assert.assertEquals(dashboardPurchaseSubscriptionresponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isDashboardPurchaseSubscriptionSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema3, dashboardPurchaseSubscriptionresponse2.asString());
		Assert.assertTrue(isDashboardPurchaseSubscriptionSchemaValidated,
				"Platform Functions API Purchase Subscription Schema Validation failed");
		errorMsg1 = dashboardPurchaseSubscriptionresponse2.jsonPath().get("errors.base[0]").toString();
		Assert.assertEquals(errorMsg1,
				"This is a single use subscription and cannot be renewed automatically. Please check request to send 'auto_renewal' as 'false'.",
				"error msg for the dashboard api is not equal");
		logger.info(
				"User will be unable to purchase the subscription plan as expected when auto renewal is true in dashboard api");
		pageObj.utils().logPass(
				"User will be unable to purchase the subscription plan as expected when auto renewal is true in dashboard api");

		// go to active plan having single use ON
		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Plans");
		pageObj.subscriptionPlansPage().searchAndClickSubscriptionPlan(spName);

	}

	// shashank
	@Test(description = "SQ-T4517 Check auto renewing status in the subscription Listing page", dataProvider = "TestDataProvider")
	@Owner(name = "Shashank Sharma")
	public void T4517_VerifyRenewStatusOnSubscriptionListingPage(boolean SingleUseSubscription,
			boolean isRenwalDisplayedExpected) throws InterruptedException {

		logger.info("== START -- T2798 Validate The Renew subscription plan ==");
		spName = "SubcriptionPlan_" + CreateDateTime.getTimeDateString();
		System.out.println("spName" + spName);
		QCname = "QcSubscription_" + CreateDateTime.getTimeDateString();
		amountcap = Integer.toString(Utilities.getRandomNoFromRange(10, 200));
		unitDiscount = Integer.toString(Utilities.getRandomNoFromRange(1, 10));
		spPrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Plans");
		pageObj.subscriptionPlansPage().createSubscriptionPlan(spName, spPrice, dataSet.get("qcName"),
				dataSet.get("qcFucntionName"), GlobalBenefitRedemptionThrottlingToggle, endDateTime,
				SingleUseSubscription);

		// Renewal should be displayed
		boolean isRenewalLabelExist = pageObj.subscriptionPlansPage().getSubscriptionAutoRenewalStatus(spName);
		Assert.assertEquals(isRenewalLabelExist, isRenwalDisplayedExpected, "Renwal is not dispalying");
		logger.info(spName + " Renewal tag is displaying");
		pageObj.utils().logPass(spName + " Renewal tag is displaying");

	}

	@DataProvider(name = "TestDataProvider")
	public Object[][] testDataProvider() {

		return new Object[][] {

				// {"flagONOFF","isDisplayed"},
				{ false, true }, { true, false },

		};

	}

	// Rakhi
	@Test(description = "SQ-T5328 Verify the Subscription image uploading sceanrios")
	@Owner(name = "Rakhi Rawat")
	public void T5328_subsciptionImageUploadingScenario() throws InterruptedException {

		spPrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Plans");
		pageObj.subscriptionPlansPage().clickOnNewPlanButton();

		// uploading image of 100kb
		String path100kb = System.getProperty("user.dir") + "/resources/Images/image_100kb.jpg";
		String spName1 = "SubcriptionPlan_1" + CreateDateTime.getTimeDateString();
		pageObj.subscriptionPlansPage().createSubscriptionPlanFirstPage(spName1, path100kb, spPrice, true);
		pageObj.subscriptionPlansPage().clickOnNext();
		String pageTitle = pageObj.newCamHomePage().getNewPageTitleClassic();
		Assert.assertEquals(pageTitle, "Edit " + spName1, "Page title did not matched");
		logger.info("Image of 100kb uploaded successfully");
		pageObj.utils().logPass("Image of 100kb uploaded successfully");
		pageObj.newCamHomePage().navigateToBackPage();

		// uploading image of 30mb
		String path30mb = System.getProperty("user.dir") + "/resources/Images/image_30mb.jpg";
		String spName2 = "SubcriptionPlan_2" + CreateDateTime.getTimeDateString();
		pageObj.subscriptionPlansPage().createSubscriptionPlanFirstPage(spName2, path30mb, spPrice, true);
		pageObj.subscriptionPlansPage().clickOnNext();
		pageTitle = pageObj.newCamHomePage().getNewPageTitleClassic();
		String status = pageObj.campaignspage().validateErrorsMessagee();
		Assert.assertTrue(status.contains("Subscription image size should be less than 500 KB"),
				"Image of 30mb uploaded successfully");
		logger.info("Image of 30mb can not be uploaded as Subscription image size should be less than 500 KB");
		TestListeners.extentTest.get()
				.pass("Image of 30mb can not be uploaded as Subscription image size should be less than 500 KB");

		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Plans");
		pageObj.subscriptionPlansPage().searchAndClickSubscriptionPlan(spName1);
		// delete subscription
		pageObj.subscriptionPlansPage().deleteSubscriptionPlan(spName1);

	}

	// Rakhi
	@Test(description = "SQ-T5374 Purchase single use subscription with auto renewal as True/False & payment adaptar as Heartland")
	@Owner(name = "Rakhi Rawat")
	public void T5374_purchaseSubscriptionPlanWithAutoRenewal() throws InterruptedException {
		pageObj.utils().logPass("------------ Heartland Test case -------------");
		spName = "SubcriptionPlan_" + CreateDateTime.getTimeDateString();
		QCname = "QcSubscription_" + CreateDateTime.getTimeDateString();
		amountcap = Integer.toString(Utilities.getRandomNoFromRange(10, 200));
		unitDiscount = Integer.toString(Utilities.getRandomNoFromRange(1, 10));
		spPrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Subscriptions");

		pageObj.dashboardpage().setPaymentAdapterInSubscriptionPage("Heartland v2.0");
		pageObj.dashboardpage().clickOnUpdateButton();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().createQualificationCriteria(QCname, amountcap,
				dataSet.get("qcFucntionName"), unitDiscount, true, dataSet.get("lineItemSelectorName"));

		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Plans");

		// create subscription plan with single_use flag value as 'false'
		pageObj.subscriptionPlansPage().createSubscriptionPlan(spName, spPrice, QCname, dataSet.get("qcFucntionName"),
				GlobalBenefitRedemptionThrottlingToggle, endDateTime, true);
		PlanID = pageObj.subscriptionPlansPage().getSbuscriptionPlanID(spName);

		// User register/signup using API1 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 1 user signup");
		String token = signUpResponse.jsonPath().get("auth_token.token");
		// String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		pageObj.utils().logPass("Api1 user signup is successful");

		// single token
		Response singleTokenResponse = pageObj.endpoints().generateHeartlandPaymentToken("",
				dataSet.get("heartLandApikey"));
		Assert.assertEquals(singleTokenResponse.statusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Not able to generate single Token for heartland adaptor");
		String finalSingleScanToken = singleTokenResponse.jsonPath().getString("token_value");
		pageObj.utils().logit("Heartland Single token is generated: " + finalSingleScanToken);

		// generate UUID
		Response posPaymentCard = pageObj.endpoints().POSPaymentCard(dataSet.get("client"), dataSet.get("secret"),
				token, finalSingleScanToken);
		Assert.assertEquals(posPaymentCard.statusCode(), ApiConstants.HTTP_STATUS_OK);
		String uuid = posPaymentCard.jsonPath().get("uuid").toString();

		// subscription plan purchase with auto renewal as true
		Response purchaseSubscriptionresponse1 = pageObj.endpoints().ApiSubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, uuid, "Heartland", "true");
		Assert.assertTrue(purchaseSubscriptionresponse1.asString()
				.contains("This is a single use subscription and cannot be renewed automatically"));
		logger.info("User is not able to purchase subscription.");
		pageObj.utils().logPass("User is not able to purchase subscription.");

		// subscription plan purchase with auto renewal as false
		Response purchaseSubscriptionresponse2 = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, uuid, "Heartland", "false");
		Assert.assertEquals(purchaseSubscriptionresponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		logger.info("User is able to purchase subscription.");
		pageObj.utils().logPass("User is able to purchase subscription.");

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
