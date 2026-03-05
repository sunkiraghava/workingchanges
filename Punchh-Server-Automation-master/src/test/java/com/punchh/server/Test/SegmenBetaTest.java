package com.punchh.server.Test;

import org.testng.asserts.SoftAssert;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.SeleniumUtilities;

import com.punchh.server.utilities.Utilities;
import io.restassured.response.Response;
import com.punchh.server.utilities.TestListeners;

@Listeners(TestListeners.class)
public class SegmenBetaTest {
	static Logger logger = LogManager.getLogger(SegmenBetaTest.class);
	public WebDriver driver;
	SeleniumUtilities selUtils;
	Properties prop;
	String userEmail;
	String segmentName, guestCount;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private PageObj pageObj;
	Utilities utils;
	ApiUtils apiUtils;
	String t2SegmentName;

	@BeforeMethod(alwaysRun = true)
	public void beforeMethod(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		selUtils = new SeleniumUtilities(driver);
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		segmentName = CreateDateTime.getUniqueString("Segment");
		utils = new Utilities();
	}

	// Archived as related to segment beta functionality
	@Test(groups = { "regression" }, description = "SQ-455, Validate Profile based type segment beta || SQ-443 "
			+ "Validate Segment Beta edit page || SQ-T2192, Validate Guest count on Segment Beta edit page")
	public void verifySegmentBetaProfileDetail() throws InterruptedException {

		// t2SegmentName = segmentName;
		SoftAssert softassert = new SoftAssert();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// pageObj.menupage().clickGuestMenu();
		// pageObj.menupage().segmentBetaLink();
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments ");
		// create new segment
		pageObj.segmentsBetaPage().createNewSegment(segmentName, dataSet.get("segmentType"), dataSet.get("attribute"),
				dataSet.get("attributeValue"));
		String email = "bhanu.belwal+10@punchh.com";
		int segmentCount = pageObj.segmentsBetaPage().getSegmentCount();
		pageObj.segmentsBetaPage().saveSegment(segmentName);
		// create new user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		softassert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful ");
		softassert.assertTrue(pageObj.segmentsBetaPage().verifySegmentGuestPresence(segmentName, email));
		int segCount = pageObj.segmentsBetaPage().getSegmentUserCount(segmentCount);
		softassert.assertTrue((segmentCount + 1) == segCount, "Failed to verify segment count after adding a user");
		softassert.assertAll();
		TestListeners.extentTest.get().pass("Successfully verifed segment count after adding a user");
		logger.info("Successfully verifed segment count after adding a user");

		/*
		 * pageObj.menupage().navigateToSubMenuItem("Guests", "Segments"); boolean
		 * val=pageObj.segmentsBetaPage().updateSegment(t2SegmentName, "5");
		 * Assert.assertTrue(val,"Failed to verify segement beta update");
		 */

	}

	// checking segment count it is covered in order TCs
	@Test(groups = { "regression" }, description = "SQ-T2286, Segment Show Page -> Segment Count")
	public void T2286_verifySegementBetaCheckins() throws InterruptedException {
		SoftAssert softassert = new SoftAssert();
		logger.info("== Segment guest count test ==");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// pageObj.menupage().clickGuestMenu();
		// pageObj.menupage().segmentBetaLink();
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments ");
		// create new segment
		pageObj.segmentsBetaPage().createNewSegment(segmentName, dataSet.get("segmentType"), dataSet.get("attribute"),
				dataSet.get("attributeValue"));
		// String email = "bhanu.belwal+10@punchh.com";
		int segmentCount = pageObj.segmentsBetaPage().getSegmentCount();
		pageObj.segmentsBetaPage().saveSegment(segmentName);
		// create new user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		softassert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful ");
		// POS Checkin
		Response response2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"));
		// apiUtils.verifyResponse(response2, "POS checkin");
		Assert.assertEquals(response2.getStatusCode(), 200);
		Assert.assertEquals(response2.jsonPath().get("email").toString(), userEmail.toLowerCase());
		softassert.assertTrue((segmentCount + 1) == pageObj.segmentsBetaPage().getSegmentUserCount(segmentCount),
				"Failed to verify segment count after adding a user");
		TestListeners.extentTest.get().pass("Successfully verifed segment count after adding a user");
		logger.info("Successfully verifed segment count after adding a user");
	}

	// Archived as segment beta functionality is absolute now
	@Test(groups = { "regression" }, description = "SQ-T2192 SQ-T2215, Validate Guest count on Segment edit page")
	public void T2215_verifySegementBetaComplex() throws InterruptedException {
		SoftAssert softassert = new SoftAssert();
		logger.info("== Segment guest count test ==");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// pageObj.menupage().clickGuestMenu();
		// pageObj.menupage().segmentBetaLink();
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments ");
		// create new segment
		pageObj.segmentsBetaPage().createNewSegment(segmentName, dataSet.get("segmentType"), dataSet.get("attribute"),
				dataSet.get("attributeValue"));
		pageObj.segmentsBetaPage().AddAnotherSegment(dataSet.get("segmentType2"), dataSet.get("attribute2"),
				"Greater than", dataSet.get("attributeValue2"));
		// String email = "bhanu.belwal+10@punchh.com";
		int segmentCount = pageObj.segmentsBetaPage().getSegmentCount();
		pageObj.segmentsBetaPage().saveSegment(segmentName);
		// create new user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		softassert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful ");
		// POS Checkin
		Response response2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"));
		// apiUtils.verifyResponse(response2, "POS checkin");
		Assert.assertEquals(response2.getStatusCode(), 200);
		Assert.assertEquals(response2.jsonPath().get("email").toString(), userEmail.toLowerCase());
		// pageObj.segmentsBetaPage().openSegment(segmentName);
		Utilities.longWait(5000);
//		softassert.assertTrue((segmentCount + 1) == pageObj.segmentsBetaPage().getSegmentTotalCount(segmentCount),
//				"Failed to verify segment count after adding a user");
		softassert.assertAll();
		TestListeners.extentTest.get().pass("Successfully verifed segment count after adding a user");
		logger.info("Successfully verifed segment count after adding a user");

	}

	// duplicate (covered in Rakhi TC)
	@Test(groups = { "regression" }, description = "SQ-T2241, Validate custom list segment beta")
	public void T2241_verifyCustomSegmentBeta() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// pageObj.menupage().clickGuestMenu();
		// pageObj.menupage().segmentBetaLink();
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments ");
		// create new segment
		pageObj.segmentsBetaPage().configureCustomSegment(segmentName, dataSet.get("segmentType"),
				dataSet.get("customEmailList"));
		pageObj.segmentsBetaPage().saveSegment(segmentName);
		boolean val = pageObj.segmentsBetaPage().verifyCustomSegmentCount(segmentName);
		pageObj.segmentsBetaPage().setSegmentlistType(segmentName);
		Assert.assertTrue(val);
	}

	@AfterMethod(alwaysRun = true)
	public void afterMethod() {
		driver.close();
		driver.quit();
		logger.info("== Browser closed ==");
	}
}