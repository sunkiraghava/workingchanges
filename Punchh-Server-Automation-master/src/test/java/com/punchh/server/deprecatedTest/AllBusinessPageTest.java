package com.punchh.server.deprecatedTest;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.campaignsTest.AnniversaryOfferCampaignTest;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;

import io.restassured.response.Response;

@Listeners(TestListeners.class)

public class AllBusinessPageTest {
	private static Logger logger = LogManager.getLogger(AnniversaryOfferCampaignTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String baseUrl;
	private String env, run = "ui";
	private static Map<String, String> dataSet;

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
		logger.info(sTCName + " ==>" + dataSet);
	}

	@Test(description = "SQ-T5481 Verify all business page", groups = "Regression", priority = 1)
	public void T5481_verifyAllBusinessPage() throws InterruptedException, IOException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.allbusinessPage().verifyHeader();
		boolean isBusinesslistdisplayed = pageObj.allbusinessPage().searchBusiness(dataSet.get("businessName"),
				dataSet.get("business_id"));
		Assert.assertTrue(isBusinesslistdisplayed, "No such business Exists please recheck the slug Provided");
		List<String> footerLinkTexts = pageObj.allbusinessPage().verifyFooterLinks();
		Assert.assertEquals(footerLinkTexts, dataSet.get("expectedLinkTexts"), "footer link texts does not match");
		pageObj.utils().logit("All the links present on the footer of the pages are working fine");
		boolean submenulinks = pageObj.allbusinessPage().VerifySubmenus();
		Assert.assertTrue(submenulinks, "Sub-menu Links are broken");
		pageObj.utils().logit("Sub-menu Links are working as expected");
		boolean submenudisplayed = pageObj.allbusinessPage().VerifySubMenuSearch("Development");
		Assert.assertTrue(submenudisplayed, "Unable to find the searched menu item");
		pageObj.utils().logPass("All the Elements on all business page are working properly");
	}

	// Checked with Akansha Jain - commenting this due to flag dependency change
	//@Test(description = "SQ-T6529 Validate the new support gifting api with flag false", groups = "api", priority = 7)
	public void SQ_T6529_NewSupportGiftingApiValidationWithFlagFalse() throws Exception {
		// :enable_optimised_support_gifting: true
		// User register/sign up using API2 Sign up
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// getting data from business.preference from DB
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		logger.info(expColValue);

		// Checking and Marking "enable_optimised_support_gifting" -> false in
		// business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag1"));

		// send points to the user via new support gifting api with feature flag as
		// false
		Response sendPointsResponse = pageObj.endpoints().sendPointsToUserViaNewSupportGiftingAPI(userID,
				dataSet.get("adminAuthorization"), dataSet.get("points"));
		Assert.assertEquals(sendPointsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "Status code 422 did not matched for dashboard api2 support gifting to user");
		pageObj.utils().logPass("Api2 send offer to user is not successful as flag is false in business.");

		// getting data from business.preference from DB
		String expColValue2 = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		logger.info(expColValue2);

		// Updating "enable_optimised_support_gifting" -> true in business.preference
		// table in DB again
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue2, b_id, env, "true", dataSet.get("dbFlag1"));
		logger.info(expColValue2);

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
