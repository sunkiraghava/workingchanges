package com.punchh.server.Integration2;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.punchh.server.apiConfig.ApiConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.api.payloadbuilder.DynamicPayloadBuilder;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class SocialLoginsGISSyncTest {
	static Logger logger = LogManager.getLogger(SocialLoginsGISSyncTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private Utilities utils;
	private IntUtils intUtils;
	private String client;
	private String secret;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		utils = new Utilities(driver);
		intUtils = new IntUtils(driver);
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		client = dataSet.get("client");
		secret = dataSet.get("secret");
	}


	@DataProvider(name = "socialDataProvider")
	public Object[][] socialDataProvider() {
		return new Object[][] {
			// Enable Facebook, Enable Google, Enable Apple
			{ true, false, false },
			{ false, true, false },
			{ false, false, true },
		};
	}

	@Test(description = "SQ-T7404 INT2-2794 | Update user sync flows for new flag with social flags | Sync guest anonymisation from Punchh to Guest Identity.", dataProvider = "socialDataProvider")
	@Owner(name = "Nipun Jain")
	public void T7404_VerifySocialUserSyncForAnonymise(boolean enableFacebook, boolean enableGoogle, boolean enableApple) throws Exception {
		String adminKey = dataSet.get("adminKey");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		intUtils.updateAdvanceAndBasicAuthConfig(false, true);

		String userEmail = "social_anonymise_" + utils.getTimestampInNanoseconds() + "@partech.com";
		String strongPassword = "1A@" + utils.getTimestampInNanoseconds();
		DynamicPayloadBuilder userPayload = new DynamicPayloadBuilder();
		userPayload.setEmail(userEmail).setPassword(strongPassword).setPassword_confirmation(strongPassword)
				.setTerms_and_conditions(true).setPrivacy_policy(true);
		Response response = pageObj.endpoints().basicAuthSignUp(client, userPayload.buildPayloadMap());
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for basic auth sign up API");

		intUtils.updateAdvanceAndBasicAuthConfig(false, false);
		intUtils.updateSocialConfig(enableFacebook, enableGoogle, enableApple);	
		intUtils.verifyGISUserSyncForActivity(client, adminKey, userEmail, "Anonymise");
	}

	@Test(description = "SQ-T7405 INT2-2794 | Update user sync flows for new flag with social flags | Sync guest deletion from Punchh to Guest Identity.", dataProvider = "socialDataProvider")
	@Owner(name = "Nipun Jain")
	public void T7405_VerifySocialUserSyncForDelete(boolean enableFacebook, boolean enableGoogle, boolean enableApple) throws Exception {
		String adminKey = dataSet.get("adminKey");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		intUtils.updateAdvanceAndBasicAuthConfig(false, true);

		String userEmail = "social_delete_" + utils.getTimestampInNanoseconds() + "@partech.com";
		String strongPassword = "1A@" + utils.getTimestampInNanoseconds();
		DynamicPayloadBuilder userPayload = new DynamicPayloadBuilder();
		userPayload.setEmail(userEmail).setPassword(strongPassword).setPassword_confirmation(strongPassword)
				.setTerms_and_conditions(true).setPrivacy_policy(true);
		Response response = pageObj.endpoints().basicAuthSignUp(client, userPayload.buildPayloadMap());
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for basic auth sign up API");
				
		intUtils.updateAdvanceAndBasicAuthConfig(false, false);
		intUtils.updateSocialConfig(enableFacebook, enableGoogle, enableApple);	
		intUtils.verifyGISUserSyncForActivity(client, adminKey, userEmail, "Delete");
	}

	@DataProvider(name = "socialDataProvider1")
	public Object[][] socialDataProvider1() {
		return new Object[][] {
			// Enable Facebook, Enable Google, Enable Apple
			// { true, false, false },
			{ false, true, false },
			// { false, false, true },
		};
	}

	@Test(description = "SQ-T7406 INT2-2794 | Update user sync flows with social flags", dataProvider = "socialDataProvider1")
	@Owner(name = "Nipun Jain")
	public void T7406_VerifyUpdateUserSyncWithSocialFlags(boolean enableFacebook, boolean enableGoogle, boolean enableApple) throws Exception {
		
		String userEmail = "social_update_sync_" + utils.getTimestampInNanoseconds() + "@partech.com";
		
		// Basic Auth ON to signup user
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		intUtils.updateAdvanceAndBasicAuthConfig(false, true);

		// User signup
		String strongPassword = "1A@" + utils.getTimestampInNanoseconds();
		DynamicPayloadBuilder userPayload = new DynamicPayloadBuilder();
		userPayload.setEmail(userEmail).setPassword(strongPassword).setPassword_confirmation(strongPassword)
				.setTerms_and_conditions(true).setPrivacy_policy(true);
		Response response = pageObj.endpoints().basicAuthSignUp(client, userPayload.buildPayloadMap());
		intUtils.validateGIS_SignUp_SignIn_Response(response);
		String accessToken = response.jsonPath().get("data.access_token");

		// Basic Auth OFF after signup
		intUtils.updateAdvanceAndBasicAuthConfig(false, false);

		// Only social flags ON
		intUtils.updateSocialConfig(enableFacebook, enableGoogle, enableApple);

		// Validate user update sync with Guest Identity
		intUtils.validateUpdatedUserSyncsWithGIS(client, secret, userEmail, accessToken);
	}
    @Test(description = "SQ-T7408 INT2-2843 | Add social login flags in meta api's.", priority = 10)
    @Owner(name = "Vansham Mishra")
    public void T7408_validateSocialMediaFlagsInMetaApis() throws Exception {

        // loginto punchh
        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        pageObj.instanceDashboardPage().loginToInstance();
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        // navigate to settings page and enable basic auth flag
        intUtils.updateAdvanceAndBasicAuthConfig(true, true);
        // Only social flags ON
        intUtils.updateSocialConfig(true, true, true);

        // Meta V2 API validation
        Response cardsResponse1 = pageObj.endpoints().Api2Cards(dataSet.get("client"), dataSet.get("secret"));
        Assert.assertEquals(cardsResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 cards");
        boolean advanceAuthFlag = cardsResponse1.jsonPath().getBoolean("gis.advance_auth_enabled");
        boolean basicAuthFlag = cardsResponse1.jsonPath().getBoolean("gis.basic_auth_enabled");
        boolean googleFlag = cardsResponse1.jsonPath().getBoolean("gis.google_enabled");
        boolean facebookFlag = cardsResponse1.jsonPath().getBoolean("gis.facebook_enabled");
        boolean appleFlag = cardsResponse1.jsonPath().getBoolean("gis.apple_enabled");
        assertSocialMediaFlagsInCardsApi(advanceAuthFlag, basicAuthFlag, googleFlag, facebookFlag, appleFlag,"true");
        // Hit v1 Meta API and verify the updated values
        Response metaApiResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
        Assert.assertEquals(metaApiResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);
        advanceAuthFlag = metaApiResponse.jsonPath().getBoolean("[0].gis.advance_auth_enabled");
        basicAuthFlag = metaApiResponse.jsonPath().getBoolean("[0].gis.basic_auth_enabled");
        googleFlag = metaApiResponse.jsonPath().getBoolean("[0].gis.google_enabled");
        facebookFlag = metaApiResponse.jsonPath().getBoolean("[0].gis.facebook_enabled");
        appleFlag = metaApiResponse.jsonPath().getBoolean("[0].gis.apple_enabled");
        assertSocialMediaFlagsInCardsApi(advanceAuthFlag, basicAuthFlag, googleFlag, facebookFlag, appleFlag,"true");

        // navigate to settings page and enable basic auth flag
        intUtils.updateAdvanceAndBasicAuthConfig(false, false);
        // Only social flags ON
        intUtils.updateSocialConfig(false, false, false);
        cardsResponse1 = pageObj.endpoints().Api2Cards(dataSet.get("client"), dataSet.get("secret"));
        Assert.assertEquals(cardsResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 cards");
        advanceAuthFlag = cardsResponse1.jsonPath().getBoolean("gis.advance_auth_enabled");
        basicAuthFlag = cardsResponse1.jsonPath().getBoolean("gis.basic_auth_enabled");
        googleFlag = cardsResponse1.jsonPath().getBoolean("gis.google_enabled");
        facebookFlag = cardsResponse1.jsonPath().getBoolean("gis.facebook_enabled");
        appleFlag = cardsResponse1.jsonPath().getBoolean("gis.apple_enabled");
        assertSocialMediaFlagsInCardsApi(advanceAuthFlag, basicAuthFlag, googleFlag, facebookFlag, appleFlag,"false");
        metaApiResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
        Assert.assertEquals(metaApiResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);
        advanceAuthFlag = metaApiResponse.jsonPath().getBoolean("[0].gis.advance_auth_enabled");
        basicAuthFlag = metaApiResponse.jsonPath().getBoolean("[0].gis.basic_auth_enabled");
        googleFlag = metaApiResponse.jsonPath().getBoolean("[0].gis.google_enabled");
        facebookFlag = metaApiResponse.jsonPath().getBoolean("[0].gis.facebook_enabled");
        appleFlag = metaApiResponse.jsonPath().getBoolean("[0].gis.apple_enabled");
        assertSocialMediaFlagsInCardsApi(advanceAuthFlag, basicAuthFlag, googleFlag, facebookFlag, appleFlag,"false");
        utils.logit("Verified social media flags in cards API response");
    }
    private void assertSocialMediaFlagsInCardsApi(boolean advanceAuthFlag, boolean basicAuthFlag, boolean googleFlag, boolean facebookFlag, boolean appleFlag, String condition) {
        if (condition.equalsIgnoreCase("true")) {
            Assert.assertTrue(advanceAuthFlag, "Advance auth flag is not true in cards API response");
            Assert.assertTrue(basicAuthFlag, "Basic auth flag is not true in cards API response");
            Assert.assertTrue(googleFlag, "Google auth flag is not true in cards API response");
            Assert.assertTrue(facebookFlag, "Facebook auth flag is not true in cards API response");
            Assert.assertTrue(appleFlag, "Apple auth flag is not true in cards API response");
        } else if (condition.equalsIgnoreCase("false")) {
            Assert.assertFalse(advanceAuthFlag, "Advance auth flag is not false in cards API response");
            Assert.assertFalse(basicAuthFlag, "Basic auth flag is not false in cards API response");
            Assert.assertFalse(googleFlag, "Google auth flag is not false in cards API response");
            Assert.assertFalse(facebookFlag, "Facebook auth flag is not false in cards API response");
            Assert.assertFalse(appleFlag, "Apple auth flag is not false in cards API response");
        }
    }

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}