package com.punchh.server.deprecatedTest;

import org.testng.annotations.Test;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;

import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.Assert;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.Listeners;
import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class AzureTest {
	static Logger logger = LogManager.getLogger(AzureTest.class);
	public WebDriver driver;
	private String userEmail;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private static Map<String, String> dataSet;
	private String env, run = "ui";

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		// String browserName = prop.getProperty("browserName");
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		env = Utilities.getConfigProperty("environment");
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
	}

//	@Test(description = "CCA2-91 Verify that SignUpAPI2 API2 is returning IDP user access token as sent in external_source_id parameter in request body only when both External source ID flag and Use for SSO button is set ON in the Punchh server configuration.", priority = 0)
	public void Api2UserSignUpAzureValidationUseforSSO_selected_ExtSourceidflag_selected() throws InterruptedException {
		Boolean b = false;
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(prop.getProperty("instanceUrl"));
		System.out.println(prop.getProperty("instanceUrl"));
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().select_use_for_sso();
		// logger.info("== API 2 user signup Test ==");
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.instanceDashboardPage().select_cockpit_dashboard_miscalleaneous_tab_external_source_id_flag();
		String azureid = dataSet.get("external_source_id") + Utilities.getAlphaNumericString(12);
		Response signUpResponse = pageObj.endpoints().Api2SignUpWithExternalSourceAndID(userEmail,
				dataSet.get("client"), dataSet.get("secret"), azureid, dataSet.get("external_source"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("access_token.token").toString(), azureid);
		pageObj.utils().logPass("Azure IDP Integration is validated successfully");
		// decrease lines of code - to modify
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(loginResponse, "API 2 user login");
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(loginResponse.jsonPath().get("user.communicable_email").toString(),
				userEmail.toLowerCase());
		// Verifying user Signup on timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String joiningchannel = pageObj.guestTimelinePage().getGuestJoiningChannelMobileEmail();
		String guestTimelineEmail = pageObj.guestTimelinePage().getGuestTimelineEmail();

		Assert.assertEquals("Joined Us via MobileEmail", joiningchannel,
				"Joined channel guest details did not matched");
		Assert.assertEquals(userEmail, guestTimelineEmail, "Guest email did not matched on timeline header");
		pageObj.utils().logPass("Successfully verified guest email and joined channel");
	}

//	@Test(description = "CCA2-92 Verify that implementation is retuning Punchh user access token on user sign up When the External source ID flag is OFF where as Use for SSO button is set ON in the Punchh server configration", priority = 1)
	public void Api2UserSignUpAzureValidationUseforSSO_selected_ExtSourceidflag_unselected()
			throws InterruptedException {
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(prop.getProperty("instanceUrl"));
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().select_use_for_sso();

		pageObj.utils().logit("== API 2 user signup Test ==");
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.instanceDashboardPage().un_select_cockpit_dashboard_miscalleaneous_tab_external_source_id_flag();
		String azureid = dataSet.get("external_source_id") + Utilities.getAlphaNumericString(12);
		Response signUpResponse = pageObj.endpoints().Api2SignUpWithExternalSourceAndID(userEmail,
				dataSet.get("client"), dataSet.get("secret"), azureid, dataSet.get("external_source"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		Assert.assertFalse(signUpResponse.jsonPath().get("access_token.token").equals(azureid));
		pageObj.utils().logPass("Punchh token for User is released successfully");
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(loginResponse, "API 2 user login");
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(loginResponse.jsonPath().get("user.communicable_email").toString(),
				userEmail.toLowerCase());
		// Verifying user Signup on timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String joiningchannel = pageObj.guestTimelinePage().getGuestJoiningChannelMobileEmail();
		String guestTimelineEmail = pageObj.guestTimelinePage().getGuestTimelineEmail();

		Assert.assertEquals("Joined Us via MobileEmail", joiningchannel,
				"Joined channel guest details did not matched");
		Assert.assertEquals(userEmail, guestTimelineEmail, "Guest email did not matched on timeline header");
		pageObj.utils().logPass("Successfully verified guest email and joined channel");

	}

	@Test(description = "CCA2-141 Verify that  Auth API is returning Punchh user access token on user sign up when the External source ID flag is OFF where as Use for SSO button is set ON in the Punchh server configuration", priority = 2)
	public void AuthUserSignUpAzureValidationUseforSSO_selected_ExtSourceidflag_Unselected()
			throws InterruptedException {
		Boolean b = false;
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(prop.getProperty("instanceUrl"));
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().select_use_for_sso();
		pageObj.utils().logit("== AUTH user signup Test ==");
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.instanceDashboardPage().un_select_cockpit_dashboard_miscalleaneous_tab_external_source_id_flag();
		String azureid = dataSet.get("external_source_id") + Utilities.getAlphaNumericString(12);
		Response signUpResponse = pageObj.endpoints().authApiSignUpAzureValidation(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("external_source"), azureid);
		pageObj.apiUtils().verifyCreateResponse(signUpResponse, "Auth 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		Assert.assertFalse(signUpResponse.jsonPath().get("access_token").toString().equals(azureid));
		pageObj.utils().logPass("Punchh token for User is released successfully");
		Response loginResponse = pageObj.endpoints().authApiUserLogin(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyCreateResponse(loginResponse, "API 2 user login");
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		Assert.assertEquals(loginResponse.jsonPath().get("communicable_email").toString(), userEmail.toLowerCase());
//			 //Verifying user Signup on timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		String joiningchannel = pageObj.guestTimelinePage().getGuestJoiningChannelMobileEmail();
		String guestTimelineEmail = pageObj.guestTimelinePage().getGuestTimelineEmail();

		Assert.assertEquals("Joined Us via MobileEmail", joiningchannel,
				"Joined channel guest details did not matched");
		Assert.assertEquals(userEmail, guestTimelineEmail, "Guest email did not matched on timeline header");
		pageObj.utils().logPass("Successfully verified guest email and joined channel");

	}

//	@Test(description = "CCA2-142 Verify that Auth API is returning IDP user access token as sent in external_source_id parameter in request body only when both External source ID flag and Use for SSO button is set ON in the Punchh server configration", priority = 3)
	public void AuthUserSignUpAzureValidationUseforSSO_selected_ExtSourceidflag_selected() throws InterruptedException {
		Boolean b = false;
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(prop.getProperty("instanceUrl"));
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().select_use_for_sso();
		pageObj.utils().logit("== AUTH user signup Test ==");
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.instanceDashboardPage().select_cockpit_dashboard_miscalleaneous_tab_external_source_id_flag();
		String azureid = dataSet.get("external_source_id") + Utilities.getAlphaNumericString(12);
		Response signUpResponse = pageObj.endpoints().authApiSignUpAzureValidation(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("external_source"), azureid);
		pageObj.apiUtils().verifyCreateResponse(signUpResponse, "Auth 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);

		Assert.assertEquals(signUpResponse.jsonPath().get("access_token").toString(), azureid);
		pageObj.utils().logPass("Azure IDP Integration is validated successfully");

		Response loginResponse = pageObj.endpoints().authApiUserLogin(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(loginResponse, "Auth API user login");
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		Assert.assertEquals(loginResponse.jsonPath().get("communicable_email").toString(), userEmail.toLowerCase());
		// Verifying user Signup on timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		String joiningchannel = pageObj.guestTimelinePage().getGuestJoiningChannelMobileEmail();
		String guestTimelineEmail = pageObj.guestTimelinePage().getGuestTimelineEmail();

		Assert.assertEquals("Joined Us via MobileEmail", joiningchannel,
				"Joined channel guest details did not matched");
		Assert.assertEquals(userEmail, guestTimelineEmail, "Guest email did not matched on timeline header");
		pageObj.utils().logPass("Successfully verified guest email and joined channel");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		Utilities.screenShotCapture(driver, sTCName);
		driver.quit();
		logger.info("Browser closed");

	}
}