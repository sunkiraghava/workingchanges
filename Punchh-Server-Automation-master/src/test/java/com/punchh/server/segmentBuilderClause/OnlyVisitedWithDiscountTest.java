package com.punchh.server.segmentBuilderClause;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
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

public class OnlyVisitedWithDiscountTest {
	static Logger logger = LogManager.getLogger(OnlyVisitedWithDiscountTest.class);
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

	@Test(description = "Create beta segments with SQL query and validate their count in ui")
	public void betasegmentsOnlyVisitedwithDiscountTest() throws Exception {
		String type = "GenericSegmentDefinition";
		String time_zone = "Etc/UTC";
		String created_at = "0000-00-00 00:00:00";
		String updated_at = "0000-00-00 00:00:00";
		Connection conn = null;
		Statement stmt = null;
		// update existing user name
		String Username1 = dataSet.get("Username1");
		// SingletonDBUtils singletonDBObj = new SingletonDBUtils();
		List<String> segmentsNames = new ArrayList<String>();
		List<String> segmentsIDS = new ArrayList<String>();
		pageObj.singletonDBUtilsObj();
		conn = DBUtils.getDBConnection(env);
		stmt = conn.createStatement();
		logger.info("DB Connection successfully");

		List<String> segmentQueryList = new ArrayList<String>();
		Set<Object> keys = prop.keySet();
		String prefixString = "OnlyVisitedwithDiscount";
		int counter = 0;
		for (Object k : keys) {
			String key = (String) k;

			if (key.startsWith(prefixString)) {
				value = prop.getProperty(key);
				String segmentBetaName = pageObj.segmentsBetaPage().generateSegmentBetName() + "_" + key;
				String finalQuery = "insert into segment_definitions (business_id, name, type, time_zone, created_at, updated_at, builder_clauses) values ('"
						+ dataSet.get("slugid") + "', '" + segmentBetaName + "', '" + type + "', '" + time_zone + "', '"
						+ created_at + "','" + updated_at + "','" + value + "');";
				segmentQueryList.add(finalQuery);
				segmentsNames.add(segmentBetaName);

			}

		}
		// running segment creation queries
		for (String str : segmentQueryList) {
			int rs = stmt.executeUpdate(str);
			if (rs > 0) {
				utils.logit("successfully inserted record with query :" + str);
			} else {
				TestListeners.extentTest.get().warning("unsucessful insertion ");
			}
		}

		utils.logit("segments Names " + segmentsNames);
		utils.logit("Total segments created are :" + segmentsNames.size());
		utils.logit("Created Segments names are :" + segmentsNames);
		for (int i = 0; i < segmentsNames.size(); i++) {
			String getSegmentIdQuery = "select id, name from segment_definitions where name ='" + segmentsNames.get(i)
					+ "'" + "and business_id=" + dataSet.get("slugid");

			ResultSet rs = stmt.executeQuery(getSegmentIdQuery);
			utils.logit("successfully fetched segment id with query :" + getSegmentIdQuery);
			while (rs.next()) {
				String segId = rs.getString(1);
				String segname = rs.getString(2);
				utils.logit("segment id is :" + segId);
				String segmentId = segId + " : " + segname;
				segmentsIDS.add(segmentId);
			}
		}
		utils.logit("segments Names size " + segmentsNames.size());
		utils.logit("segments IDS size " + segmentsIDS.size());
		utils.logit("segments IDS " + segmentsIDS);
		// navigate to Business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		for (int i = 0; i < segmentsIDS.size(); i++) {
			TestListeners.test = TestListeners.extentReport.createTest(segmentsIDS.get(i));
			TestListeners.extentTest.set(TestListeners.test);
			String[] id = segmentsIDS.get(i).split(":");
			String segmentid = id[0].trim();
			Response response = pageObj.endpoints().getSegmentCount(dataSet.get("apiKey"), segmentid);
			apiResponseCheck = utils.apiResponseStatusCode(response, ApiConstants.HTTP_STATUS_OK);
			if (!apiResponseCheck) {
				logger.warn("Segment count API response code is not 200 for Segment with segment id : " + segmentid);
				TestListeners.extentTest.get().warning(
						"Segment count API response code is not 200 for Segment with segment id : " + segmentid);
				apiResponseCheckCounter++;
			}
			String cnt = response.jsonPath().get("count");
			String cnt1 = cnt.replaceAll(",", "");
			int count = Integer.parseInt(cnt1);
			String segName = id[1];
			String query = "delete from segment_definitions where name ='" + segName + "'";
			// do not delete segment if any count is equal to 0
			if (count == 0) {
//				logger.info("Segment name : " + segmentsIDS.get(i) + "  count is 0 hence not deleting it");
//				TestListeners.extentTest.get()
//						.warning("Segment name :" + segmentsIDS.get(i) + "count is 0 hence not deleting it");
				// verifying In_segment query using API for not existing users
				Response userInSegmentResp1 = pageObj.endpoints().userInSegment(Username1, dataSet.get("apiKey"),
						segmentsIDS.get(i));
				Assert.assertEquals(userInSegmentResp1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
						"Status code 201 did not matched for USER_INSEGMENT api with segment id: " + segmentsIDS.get(i)
								+ "builder clauses: " + value);
				String result1 = userInSegmentResp1.jsonPath().get("result").toString();
				Assert.assertEquals(result1, "false", "Guest is present in segment");
				utils.logPass("Verified that status of " + Username1 + " is not present in Segment");
				// Delete the segment
				int rs = stmt.executeUpdate(query);
				logger.info("Segment deleted is: " + segmentsIDS.get(i));
				TestListeners.extentTest.get().warning("Segment deleted is: " + segmentsIDS.get(i));
			}

			else {
				String Seg_name = segName.replace(" S", "S");
				// navigate to segments
				pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
//				pageObj.newSegmentHomePage().clickOnSwitchToClassicBtn();
				utils.waitTillPagePaceDone();
				pageObj.segmentsPage().searchAndOpenSegment(Seg_name, segmentid);
				pageObj.segmentsBetaPage().getGuestInSegmentCount();
				String segmentGuest = pageObj.segmentsBetaPage().getSegmentGuest();
				Response userInSegmentResp2 = pageObj.endpoints().userInSegment(segmentGuest, dataSet.get("apiKey"),
						segmentid);
				Assert.assertEquals(userInSegmentResp2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
						"Status code 201 did not matched for auth user signup api");
				utils.logPass("Verified that user " + segmentGuest + " is present in Segment");
				String result = userInSegmentResp2.jsonPath().get("result").toString();
				Assert.assertEquals(result, "true", "Guest is present in segment");
				utils.logPass("Verified that status of " + segmentGuest + " is present in Segment");
				utils.logit("Segment name :" + segmentsIDS.get(i) + "Guest count in segment is : " + count);
				int rs = stmt.executeUpdate(query);
				logger.info("Segment deleted is: " + segmentsIDS.get(i));
				TestListeners.extentTest.get().warning("Segment deleted is: " + segmentsIDS.get(i));

			}
		}
		Assert.assertEquals(apiResponseCheckCounter, 0, "Segment count API response code is not 200");
	}

	@AfterMethod(alwaysRun = true)
	public void afterMethod() {
		driver.close();
		driver.quit();
		logger.info("== Browser closed ==");
	}
}