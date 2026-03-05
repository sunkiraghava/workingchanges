package com.punchh.server.deprecatedTest;

import java.lang.reflect.Method;
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
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.SeleniumUtilities;

import com.punchh.server.utilities.TestListeners;
import io.restassured.response.Response;

//@author=Rajasekhar
@Listeners(TestListeners.class)
public class AdditionalURLsForLocations {

	private static Logger logger = LogManager.getLogger(AdditionalURLsForLocations.class);
	SeleniumUtilities selUtils;
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	String storeNumber = "";

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {

		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		selUtils = new SeleniumUtilities(driver);
		env = pageObj.getEnvDetails().setEnv();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
	}

	// @author=Rajasekhar
	@Test(description = "Verify the presence of new filed Alternate Store Number/Identifier field in the Location tab", groups = "PickupFunctional", priority = 0)
	public void VerifyAlternateStoreNumber() throws InterruptedException {

		/*
		 * // Instance login and goto dashboard
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 * pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug")); <<<<<<<
		 * HEAD ======= pageObj.menupage().clickDashboardMenu(); >>>>>>>
		 * 8cafe8ed7820596bd05a75846ea53822d0ebcf10
		 * pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		 * pageObj.locationPage().verifyAlternativeStoreID(dataSet.get("altstorenum"));
		 */

		// Meta API validation
		Response cardsResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 cards");
		pageObj.utils().logPass(
				"Api1 cards is successful with response code : " + cardsResponse.statusCode());
		String altstore = pageObj.locationPage().verifyAlternativeStoreIDKeyValuesInCardsAPI(cardsResponse, "locations",
				"name", "Automation", "alternate_store_number");

		// String
		// altstore=utils.getJsonReponseKeyValueFromJsonArrayForUnknownKeyValuePair(cardsResponse,"[0].locations","name","ABC
		// test","alternate_store_number");
		logger.info("Alternative storenumber is................................................... " + altstore);

		// String altstorenum =
		// cardsResponse.jsonPath().get("[0].locations").toString();
		// String altstorenum1 =
		// cardsResponse.jsonPath().get("[0].locations.alternate_store_number[1]");
		// System.out.println("Alternative storenumber is "+altstorenum);
		// System.out.println("Alternative storenumber1 is "+altstorenum1);
		/*
		 * Assert.assertTrue(altstorenum.equalsIgnoreCase(dataSet.get("altstorenum")),
		 * "Alternative storenumber did not matched with api response");
		 * Assert.assertnull(altstorenum1,"null",
		 * "Alternative storenumber1 did not matched in api response")
		 */;
	}
	// @author=Rajasekhar
	/*
	 * @Test(description =
	 * "Verify that the new \"Additional URL Labels\" field is present under Cockpit → Locations"
	 * , groups = "regression", priority = 0) public void
	 * VerifyAdditionalURLsforLocation() throws InterruptedException {
	 * 
	 * 
	 * // Instance login and goto dashboard
	 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
	 * pageObj.instanceDashboardPage().loginToInstance();
	 * pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
	 * pageObj.menupage().clickDashboardMenu();
	 * //pageObj.menupage().clickSettingsMenu();
	 * //pageObj.menupage().clickCockPitMenu();
	 * pageObj.menupage().clickCockpitLocation();
	 * pageObj.cockpitLocationPage().verifyAdditonalURLs();
	 * 
	 * 
	 * 
	 * }
	 */

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();

	}
}