package com.punchh.server.LP1Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

//Author:- Shashank Sharma

@Listeners(TestListeners.class)
public class DeactivateUserLoginVerification {
	static Logger logger = LogManager.getLogger(DeactivateUserLoginVerification.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String run = "ui";
	private String env;
	private String baseUrl;
	private String userEmail;
	Utilities utils;
	private static Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void beforeMethod(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run , env , "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
	}

	@Test(description = "LPE-T815 / SQ-T3690 - Verify deactivated user cannot login via iframe ,API || "
			+ "SQ-T4668 Verify Reactivation button is getting visible on iframe when deactivated user try to login || "
			+ "SQ-T4669 Verify Reactivation button is getting visible on iframe when deactivated user try to login", groups = {
					"regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void T3690_DeactivatedUserCantLoginUsingAPI() throws InterruptedException {

		// SignupAPI
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 1 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		utils.logPass("Api1 user signup is successful");
		String authToken = signUpResponse.jsonPath().get("auth_token.token");
		Response deactivateResponse = null;
		// Deactivateuser
		int i = 0;
		int statusCode;
		do {
			deactivateResponse = pageObj.endpoints().DeactivateUserAPI(dataSet.get("client"), dataSet.get("secret"),
					authToken);
			statusCode = deactivateResponse.getStatusCode();
			if (statusCode != ApiConstants.HTTP_STATUS_OK) {
				Thread.sleep(1000);
				i++;
				continue;
			}

			break;
		} while (i < 10 && statusCode != 200);
		Assert.assertEquals(statusCode, ApiConstants.HTTP_STATUS_OK, "Guest deactivated reponse is not matched");
		Assert.assertTrue(
				deactivateResponse.jsonPath().get("message").toString().contains("Guest deactivated successfully"));

		Response userLoginResponse = pageObj.endpoints().Api1UserLogin(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(userLoginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		logger.info("Verified that user not able to login after deactivate using API");
		utils.logPass("Verified that user not able to login after deactivate using API");

		String actualErrorMessage = userLoginResponse.jsonPath().getString("base").replace("[", "").replace("]", "");

		Assert.assertEquals(actualErrorMessage, dataSet.get("expErrorMessage"));
		logger.info("Verified the error message if user is de-activated using API");
		utils.logPass("Verified the error message if user is de-activated using API");
	}

	@Test(description = "LPE-T815 / SQ-T3690 - Verify deactivated user cannot login via iframe ,API", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void T3690_DeactivatedUserCantLoginFromIframe() throws InterruptedException {
		// iFrame Signup
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		String iFrameEmail = pageObj.iframeSingUpPage().iframeSignUp();

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		pageObj.guestTimelinePage().navigateToTabs("Edit Profile");
		pageObj.guestTimelinePage().deactivateReactivateUser("Deactivate");

		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		String loginResultMessage = pageObj.iframeSingUpPage().verifyIframeLoginErrorMessage(iFrameEmail,
				prop.getProperty("iFramePassword"));
		Assert.assertEquals(loginResultMessage, dataSet.get("expectedErrorMessage"));
		logger.info("Verified that user not able to login after deactivate using IFrame");
		utils.logPass("Verified that user not able to login after deactivate using IFrame");

		// clicked on the reactivate button
		pageObj.iframeSingUpPage().clickReactivateBtn();
		pageObj.iframeSingUpPage().sendReactivateEmail(iFrameEmail);
		String text2 = pageObj.iframeSingUpPage().msgVisible();
		Assert.assertEquals(text2, dataSet.get("ExpectedMsg2"), "reactivation msg is not equal");
		logger.info("Verified reactivation message is equal");
		utils.logPass("Verified reactivation message is equal");

	}

	// Anant
    @Test(
        description = "SQ-T4196 Verify delete all should be shown in logs when user_incinerate_worker run or admin try to delete the user.",
        groups = {"regression", "dailyrun"})
	@Owner(name = "Vansham Mishra")
	public void T4196_deleteUserLogs() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.cockpitGuestPage().editUserIncinerateDaysField("2");
		logger.info("edit fields user incinerate days with value 2");
		utils.logit("edit fields user incinerate days with value 2");

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		utils.logPass("Api1 user signup is successful");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().navigateToTabs("Edit Profile");
		pageObj.guestTimelinePage().clickDeleteAnonymizeGuest("Delete-General");

		pageObj.sidekiqPage().navigateToSidekiqScheduled(baseUrl);
		pageObj.sidekiqPage().filterByJob(dataSet.get("sidekiqJob"));
		pageObj.sidekiqPage().checkSelectAllJobsCheckBox();
		pageObj.sidekiqPage().clickAddToQueue();
	}

	// Anant
	@Test(description = "SQ-T4197 Verify delete all should be shown in logs when user_anonymize_worker run or admin try to delete the user."
        + "SQ-T4458 Verify User incinerate days should not accept negative Integer values",
        groups = {"regression", "dailyrun"})
	@Owner(name = "Vansham Mishra")
	public void T4197_deleteUserLogs() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.cockpitGuestPage().editUserIncinerateDaysField(dataSet.get("val"));
		String msg = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertEquals(msg, dataSet.get("expectedMsg"),
				"when updating user incinerate days with the negative value expected error msg not visible");
		logger.info(
				"Verified when updating user incinerate days with the negative value expected error msg is visible");
		utils.logPass(
				"Verified when updating user incinerate days with the negative value expected error msg is visible");

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.cockpitGuestPage().editUserIncinerateDaysField("2");
		logger.info("edit fields user incinerate days with value 2");
		utils.logit("edit fields user incinerate days with value 2");

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		utils.logPass("Api1 user signup is successful");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().navigateToTabs("Edit Profile");

		pageObj.guestTimelinePage().clickDeleteAnonymizeGuest("Anonymize-Inactivity");

		pageObj.sidekiqPage().navigateToSidekiqScheduled(baseUrl);
		pageObj.sidekiqPage().filterByJob(dataSet.get("sidekiqJob"));
		pageObj.sidekiqPage().checkSelectAllJobsCheckBox();
		pageObj.sidekiqPage().clickAddToQueue();
		logger.info("job is added in the queue");
		utils.logPass("job is added in the queue");
	}

	// covered in T3690_DeactivatedUserCantLoginUsingAPI
	// Anant
//	@Test(description = "SQ-T4668 Verify Reactivation button is getting visible on iframe when deactivated user try to login"
//        + "SQ-T4669 Verify Reactivation button is getting visible on iframe when deactivated user try to login",
//        groups = {"regression", "dailyrun"})
	@Owner(name = "Vansham Mishra")
	public void T4688_verifyReactivationButtonVisibleOnIframe() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// create a user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		logger.info("Api1 user signup is successful");
		utils.logPass("Api1 user signup is successful");

		// nagivate to user timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().navigateToTabs("Edit Profile");

		// deactivate the guest
		pageObj.guestTimelinePage().deactivateGuestWithAllowReactivationOrNot(dataSet.get("allowReactivation"));

		// switch to child window
		String parentWindow = driver.getWindowHandle();
		utils.createNewWindowAndSwitch(parentWindow);

		// login through iframe
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLoginToCheckReactivation(userEmail, prop.getProperty("iFramePassword"), 2);
		String text = pageObj.iframeSingUpPage().msgVisible();
		Assert.assertEquals(text, dataSet.get("ExpectedMsg"), "Deactivate msg is not equal");
		logger.info("Verified deactivate message is equal");
		utils.logPass("Verified deactivate message is equal");

		// clicked on the reactivate button
		pageObj.iframeSingUpPage().clickReactivateBtn();
		pageObj.iframeSingUpPage().sendReactivateEmail(userEmail);
		String text2 = pageObj.iframeSingUpPage().msgVisible();
		Assert.assertEquals(text2, dataSet.get("ExpectedMsg2"), "reactivation msg is not equal");
		logger.info("Verified reactivation message is equal");
		utils.logPass("Verified reactivation message is equal");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
