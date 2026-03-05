package com.punchh.server.segmentBuilderClause;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
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
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)

public class AverageCheckinTest {
	static Logger logger = LogManager.getLogger(AverageCheckinTest.class);
	public WebDriver driver;
	Properties prop;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private PageObj pageObj;
	String value;
	private Utilities utils;
	private boolean apiResponseCheck;
	int apiResponseCheckCounter = 0;

	@BeforeMethod(alwaysRun = true)
	public void beforeMethod(Method method) {

		prop = Utilities.loadPropertiesFile("segmentBeta.properties");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();

		utils = new Utilities(driver);
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;

	}

	@Test(description = "Create averageCheckin segments using segment creation API and validate their count in UI")
	public void SegmentBuilderClausesTest() throws Exception {
		// update existing user name
		String Username1 = dataSet.get("Username1");
		List<String> segmentsNames = new ArrayList<String>();
		List<String> segmentsIDS = new ArrayList<String>();
		utils.logit("Starting AverageCheckin segment creation and validation using builder clause");
		Set<Object> keys = prop.keySet();
		String prefixString = "averageCheckin";
		int counter = 0;
		for (Object k : keys) {
			String key = (String) k;

			if (key.startsWith(prefixString)) {
				value = prop.getProperty(key);
				String segmentBetaName = pageObj.segmentsBetaPage().generateSegmentBetName() + "_" + key;
				String averageCheckinBuilderClause = utils.convertToDoubleStringifiedProperties(value);
				Response segmentCreationUsingBuilderClauseResponse1 = pageObj.endpoints()
						.segmentCreationUsingBuilderClause(dataSet.get("apiKey"), segmentBetaName,
								averageCheckinBuilderClause);
				Assert.assertEquals(segmentCreationUsingBuilderClauseResponse1.getStatusCode(),
						ApiConstants.HTTP_STATUS_OK);
				String segmentId = segmentCreationUsingBuilderClauseResponse1.jsonPath().get("data.id").toString();

				Response segmentCountresponse = pageObj.endpoints().getSegmentCount(dataSet.get("apiKey"), segmentId);
				Assert.assertEquals(segmentCountresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
						"Status code 200 did not matched for Fetch segment count dashboard api");
				utils.logit("Fetch segment count Dashboard API is Successful");
				int segmentCount = Integer
						.parseInt((segmentCountresponse.jsonPath().get("count").toString()).replaceAll(",", ""));

				segmentsIDS.add(segmentId);
				segmentsNames.add(segmentBetaName);
			}

		}

		utils.logit(segmentsNames.toString());
		utils.logit("segments Names " + segmentsNames);
		utils.logit("Total segments created are :" + segmentsNames.size());
		utils.logit("Total segments created are :" + segmentsNames.size());
		utils.logit("Created Segments names are :" + segmentsNames);
		utils.logit("Created Segments names are :" + segmentsNames);
		utils.logit(String.valueOf(segmentsNames.size()));
		utils.logit("segments Names size " + segmentsNames.size());
		utils.logit(String.valueOf(segmentsIDS.size()));
		utils.logit("segments IDS size " + segmentsIDS.size());
		utils.logit(segmentsIDS.toString());
		utils.logit("segments IDS " + segmentsIDS);
		// navigate to Business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		for (int i = 0; i < segmentsIDS.size(); i++) {

			String segmentId = segmentsIDS.get(i); // already clean ID
			String segName = segmentsNames.get(i);

			// ---- Fetch segment count ----
			Response response = pageObj.endpoints().getSegmentCount(dataSet.get("apiKey"), segmentId);

			apiResponseCheck = utils.apiResponseStatusCode(response, ApiConstants.HTTP_STATUS_OK);
			if (!apiResponseCheck) {
				utils.logit("Segment count API failed for segmentId: " + segmentId);
				apiResponseCheckCounter++;
			}

			int count = Integer.parseInt(response.jsonPath().get("count").toString().replace(",", ""));

			// ---- Correct delete query (ID based) ----
			String deleteQuery = "delete from segment_definitions where id = " + segmentId;

			// ================= COUNT == 0 =================
			if (count == 0) {

				Response userInSegmentResp = pageObj.endpoints().userInSegment(Username1, dataSet.get("apiKey"),
						segmentId);

				Assert.assertEquals(userInSegmentResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
				Assert.assertEquals(userInSegmentResp.jsonPath().get("result").toString(), "false",
						"Guest unexpectedly present in segment");

				utils.logit("Guest not present in segmentId: " + segmentId);

				DBUtils.executeQuery(env, deleteQuery);
				utils.logit("Deleted segmentId: " + segmentId);

			}

			// ================= COUNT > 0 =================
			else {

				pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
				pageObj.newSegmentHomePage().clickOnSwitchToClassicBtn();
				utils.waitTillPagePaceDone();

				pageObj.segmentsPage().searchAndOpenSegment(segName, segmentId);

				pageObj.segmentsBetaPage().getGuestInSegmentCount();
				String segmentGuest = pageObj.segmentsBetaPage().getSegmentGuest();

				Response userInSegmentResp = pageObj.endpoints().userInSegment(segmentGuest, dataSet.get("apiKey"),
						segmentId);

				Assert.assertEquals(userInSegmentResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
				Assert.assertEquals(userInSegmentResp.jsonPath().get("result").toString(), "true",
						"Guest not present in segment");

				utils.logit("Guest present in segmentId: " + segmentId);

				DBUtils.executeQuery(env, deleteQuery);
				utils.logit("Deleted segmentId: " + segmentId);

			}
		}

		Assert.assertEquals(apiResponseCheckCounter, 0, "Segment count API response code is not 200");
	}

	@AfterMethod(alwaysRun = true)
	public void afterMethod() {
		driver.close();
		driver.quit();
		utils.logit("== Browser closed ==");
	}
}