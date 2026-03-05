package com.punchh.server.CXTest;

import java.lang.reflect.Method;
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

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class PunchhMenuSTSTest {

	private static Logger logger = LogManager.getLogger(PunchhMenuSTSTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName, env;
	private Utilities utils;
	private String run = "ui";
	private String baseUrl;
	private String userEmail;
	private static Map<String, String> dataSet;
	String activate = "/activate/";

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
		// move to All Business Page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	// can only run on moes
	@Test(description = "SQ-T3703: Punchh to Menu STS user onboarding and user profilesync", groups = "Regression", priority = 0, enabled = true)
	@Owner(name = "Rajasekhar Reddy")
	public void VerifyPunchhMenuSTSTest() throws InterruptedException {
		TestListeners.extentTest.get().info(sTCName + " ==>" + dataSet);
		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// validate Parordering configs
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Services");
		pageObj.whitelabelPage().clickMenuBtn();
		pageObj.whitelabelPage().verifyParOrderingConfigs();

		// User register/signup using API1 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api1 signup");
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String FN = signUpResponse.jsonPath().get("first_name").toString();
		String DOB = signUpResponse.jsonPath().get("birthday").toString();
		TestListeners.extentTest.get().pass("Api1 user signup is successful");

		// Fetch Menu ordering token and Menu ID using Ordering_token api
		Response OrderingTokenResponse = pageObj.endpoints().MenuOrderingtoken(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(OrderingTokenResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api1 MenuOrderingtoken");
		String Ordering_token = OrderingTokenResponse.jsonPath().get("ordering_token").toString();
		String menuid = OrderingTokenResponse.jsonPath().get("external_user_id").toString();
		TestListeners.extentTest.get().pass("api1 MenuOrderingtoken is successful");

		// User validation on guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().verifyGuestTimeline(userEmail);
		pageObj.guestTimelinePage().verifyExternalSourceid();
		// Fetch User profile details from Menu system

		Response MenuUserProfileResponse = pageObj.endpoints().MenuFetchUserProfile(menuid, Ordering_token,
				dataSet.get("appkey"));
		pageObj.apiUtils().verifyResponse(MenuUserProfileResponse, "API 1 MenuFetchUserProfile");
		String Menu_FN = MenuUserProfileResponse.jsonPath().get("data.customer_account.first_name").toString();
		String Menu_id = MenuUserProfileResponse.jsonPath().get("data.customer_account.id").toString();
		Assert.assertEquals(OrderingTokenResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api1 MenuFetchUserProfile");
		Assert.assertEquals(Menu_id, menuid, "Menu_id did not match");
		logger.info("Menu id matched in both Punchh and Menu system");
		Assert.assertEquals(FN, Menu_FN, "First name did not match");
		logger.info("First name matched in both Punchh and Menu system");

		// Update/add user Phone number,FN,LN and email in the Punchh dashboard

		String punchh_phone = pageObj.guestTimelinePage().setPhone1();
		System.out.println("Phone number in Punchh system " + punchh_phone);
		String updatedFname = "Raja";
		String updatedLname = "Renamed";
		String updatedEmail = pageObj.iframeSingUpPage().generateEmail1();
		pageObj.guestTimelinePage().ProfileUpdatesforMenuSync(updatedFname, updatedLname, updatedEmail);
		// pageObj.guestTimelinePage().changeDropdownValue("2000");
		// Fetch updated user profile details from Menu system
		Response MenuUserProfileResponse1 = pageObj.endpoints().MenuFetchUserProfile(menuid, Ordering_token,
				dataSet.get("appkey"));
		pageObj.apiUtils().verifyResponse(MenuUserProfileResponse1, "API 1 MenuFetchUserProfile");
		String Phone = MenuUserProfileResponse1.jsonPath().get("data.customer_account.phone_number").toString();
		String Menu_Phone = Phone.substring(Phone.length() - 10);
		String UpdatedFN = MenuUserProfileResponse1.jsonPath().get("data.customer_account.first_name").toString();
		String UpdatedLN = MenuUserProfileResponse1.jsonPath().get("data.customer_account.last_name").toString();
		String UpdatedEmail = MenuUserProfileResponse1.jsonPath().get("data.customer_account.email").toString();
		String MDOB = MenuUserProfileResponse1.jsonPath().get("data.customer_account.demographics.value").toString();
		String MenuDOB = (MDOB.substring(1, MDOB.length() - 1));
		System.out.println("Birthdate in Menu api:" + MenuDOB);
		Assert.assertEquals(punchh_phone, Menu_Phone, "Phonenumber did not match after update");
		logger.info("Phonenumber matched in both Punchh and Menu system after the update");

		Assert.assertEquals(updatedFname, UpdatedFN, "First name did not match after update");
		logger.info("Firstname matched in both Punchh and Menu system after the update");

		Assert.assertEquals(updatedLname, UpdatedLN, "Last name did not match after update");
		logger.info("Lastname matched in both Punchh and Menu system after the update");

		Assert.assertEquals(updatedEmail, UpdatedEmail, "Email did not match after update");
		logger.info("Email matched in both Punchh and Menu system after the update");

		Assert.assertEquals(DOB, MenuDOB, "Birthday did not match after update");
		logger.info("Date of birth matched in both Punchh and Menu system after the update");

		logger.info("Updated Profile details are- First name: " + UpdatedFN + "\n" + "Last name: " + UpdatedLN + "\n"
				+ "Phone number: " + punchh_phone + "\n" + "Email:" + UpdatedEmail + "\n" + "DOB:" + MenuDOB);
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