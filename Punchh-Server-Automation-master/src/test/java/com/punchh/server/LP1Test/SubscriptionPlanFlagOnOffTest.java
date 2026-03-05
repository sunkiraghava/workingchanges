package com.punchh.server.LP1Test;

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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class SubscriptionPlanFlagOnOffTest {
	static Logger logger = LogManager.getLogger(SubscriptionPlanFlagOnOffTest.class);
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
	Properties prop;
	private Utilities utils;
	private String endDateTime;
	SeleniumUtilities selUtils;

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
		logger.info(sTCName + " ==> " + dataSet);
		selUtils = new SeleniumUtilities(driver);
		endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";
		// Move to All Businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

//Shashank
	@Test(description = "SQ-T4351 Verify reward_debit creation for all types of businesses when subscriptions are ON.", dataProvider = "TestData", priority = 2)
	@Owner(name = "Shashank Sharma")
	public void T4351_VerifySubscriptionPOSRedemptionOnDifferentBusinessType(String slugName, String subscriptionName,
			String subscriptionPlanID) throws InterruptedException {
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), slugName);
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);

		spName = subscriptionName;
		PlanID = subscriptionPlanID;
		amountcap = Integer.toString(Utilities.getRandomNoFromRange(10, 200));
		unitDiscount = Integer.toString(Utilities.getRandomNoFromRange(1, 10));
		spPrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));

		txn = "123456" + CreateDateTime.getTimeDateString();
		date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		key = CreateDateTime.getTimeDateString();

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(slugName);

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Enable Subscriptions?", "check");
		pageObj.dashboardpage().clickOnUpdateButton();

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
				dataSet.get("locationkey"), amountcap, "20001");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos redemption api");

		logger.info("Verified that POS redemption of Subscription");
		pageObj.utils().logPass("Verified that POS redemption of Subscription");

	}

	// Shashank
	@Test(description = "SQ-T4350 Verify reward_debit creation for all types of businesses when subscriptions are OFF.", dataProvider = "TestData", priority = 1)
	@Owner(name = "Shashank Sharma")
	public void T4350_VerifySubscriptionOFFPOSRedemptionOnDifferentBusinessType(String slugName,
			String subscriptionName, String subscriptionPlanID) throws InterruptedException {
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), slugName);
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);

		spName = subscriptionName;
		PlanID = subscriptionPlanID;
		amountcap = Integer.toString(Utilities.getRandomNoFromRange(10, 200));
		unitDiscount = Integer.toString(Utilities.getRandomNoFromRange(1, 10));
		spPrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));

		txn = "123456" + CreateDateTime.getTimeDateString();
		date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		key = CreateDateTime.getTimeDateString();

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(slugName);

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Subscriptions?", "uncheck");
		pageObj.dashboardpage().clickOnUpdateButton();

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
		purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);

		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				" Subscription should be disable but it is enable");
		boolean isApi2PurchaseSubscriptionSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2DisabledFeatureErrorArraySchema, purchaseSubscriptionresponse.asString());
		Assert.assertTrue(isApi2PurchaseSubscriptionSchemaValidated,
				"API v2 Purchase Subscription Schema Validation failed");
		logger.info("Verified that POS redemption of Subscription");
		pageObj.utils().logPass("Verified that POS redemption of Subscription");
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Subscriptions?", "check");

	}

	@DataProvider(name = "TestData")
	public Object[][] testDataProvider() {
		return new Object[][] { { "autoone", "DoNotDeleteSubscription_Automation_T4351", "4351" },
				{ "coffeebean", "DoNotDeleteAutoSubscription_T4351_T4352", "2612" }
//				{ "schlotzskys", "DoNotDeleteAutoSubscription_T4351_T4352", "2613" }
		};

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