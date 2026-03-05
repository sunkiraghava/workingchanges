/*
 * @author Kalpana Singh (kalpana.singh@partech.com)
 * @brief This class contains UI test cases for the POS Control Center > Location > Audit Log > config overrride,calendar test and sorting.
 * @fileName ppccLocationAuditLogTest.java
 */
package com.punchh.server.ppccTest;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class ppccLocationAuditLogTest {
	static Logger logger = LogManager.getLogger(ppccLocationAuditLogTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
        prop = Utilities.loadPropertiesFile("config.properties");
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
        pageObj.readData().ReadDataFromJsonFileForClientSecretKey(pageObj.readData().getJsonFilePath("ui" , env , "Secrets"),
            dataSet.get("slug"));
        dataSet.putAll(pageObj.readData().readTestData);
        logger.info(sTCName + " ==>" + dataSet);
        pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
    }

	@Test(description = "SQ-T6473 Verify the Date search using calendar in Location Audit Log")
	@Owner(name = "Kalpana")
	public void T6473_verifyTheDateSearchUsingCalendarInLocationAuditLog() throws Exception {
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");

		//create first policy using api
		String createdPolicy = pageObj.ppccLocationPage().createPolicy();
        String token = pageObj.endpoints().getAuthTokenForPPCC(Integer.parseInt(dataSet.get("businessId")), dataSet.get("slug"), dataSet.get("businessName"));
		String queryParam = "";
		Response createPolicyResponse = pageObj.endpoints().addPolicy(token,createdPolicy,1,queryParam,"published");
		Assert.assertEquals(createPolicyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for create Policy API");
		Assert.assertEquals(createPolicyResponse.jsonPath().get("data"), "Policy `" + createdPolicy + "` created successfully", "Messages do not match");
		Assert.assertEquals(createPolicyResponse.jsonPath().get("metadata"), Collections.emptyMap(), "Metadata has some value");
		Assert.assertEquals(createPolicyResponse.jsonPath().getList("errors"), Collections.emptyList(), "Response has some errors");
		pageObj.utils().logPass("Add Policy API executed successfully");
		logger.info("Get the policy id");
		queryParam = "?search=" + createdPolicy;
		Response policyListResponse = pageObj.endpoints().getPolicyList(token, queryParam);
		int policyId = policyListResponse.jsonPath().get("data[0].id");

		// provision a location
		pageObj.ppccLocationPage().navigateToLocationsTab();
		pageObj.ppccLocationPage().searchLocationsInDeprovisionedList(dataSet.get("locationName"));
		pageObj.ppccLocationPage().provisionALocation(createdPolicy, dataSet.get("packageVersion"));
		pageObj.ppccLocationPage().searchLocationsInProvisionedList(dataSet.get("locationName"));

		// deprovision a location
		pageObj.ppccLocationPage().searchLocationsInProvisionedList(dataSet.get("locationName"));
		pageObj.ppccLocationPage().deProvisionALocation();
		pageObj.utils().logPass("Location is de-provisioned successfully");

		// navigate to Audit Log
		pageObj.ppccLocationAuditLogPage().navigateToAuditLogs();
		pageObj.ppccLocationAuditLogPage().setTodayDateInCalendar();
		String todayDate = pageObj.utils().getTodayDate();
		String values = pageObj.utils().getLocatorValue("ppccLocationAuditLogPage.auditLogColumnData").replace("{columnIndex}",
				String.valueOf("7"));
		boolean isDateCalendarFilterWorking = pageObj.ppccLocationAuditLogPage().isValuePresentOnUI(todayDate, values);
		Assert.assertTrue(isDateCalendarFilterWorking, "Date Calendar filter is not working as expected.");
		pageObj.ppccLocationAuditLogPage().clickBackButtonOnLocationAuditLog();

		//delete created policy using api
		logger.info("Delete the policy");
		Response deleteApiResponse = pageObj.endpoints().deletePolicy(token, policyId);
		Assert.assertEquals(deleteApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for delete policy API");
		Assert.assertEquals(deleteApiResponse.jsonPath().getList("errors"), Collections.emptyList(), "Response has some errors");
		pageObj.utils().logPass("Created Policy is deleted Successfully.");
	}
	@Test(description = "SQ-T6471 Verify the sorting functionality on Location Audit Log using app version")
	@Owner(name = "Kalpana")
	public void T6471_verifyTheSortingFunctionalityOnLocationAuditLogUsingAppVersion() throws Exception {
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
		pageObj.ppccLocationAuditLogPage().navigateToAuditLogs();

		// apply sort and verify data for app version in ascending order
		pageObj.ppccLocationAuditLogPage().sortColumn("App Version");
		List<String> columnValueList = pageObj.ppccLocationAuditLogPage().getColumnData("4");
		List<String> sortedColumnValueList = new ArrayList<>(columnValueList);
		Collections.sort(sortedColumnValueList);
		Assert.assertEquals(sortedColumnValueList, columnValueList, "AppVersion column data is not in ascending order.");
		pageObj.utils().logPass("AppVersion column data is in ascending order.");

		// apply sort and verify data for app version in descending order
		pageObj.ppccLocationAuditLogPage().sortColumn("App Version");
		columnValueList = pageObj.ppccLocationAuditLogPage().getColumnData("4");
		List<String> sortedColumnValueListDesc = new ArrayList<>(columnValueList);
		sortedColumnValueListDesc.sort(Collections.reverseOrder());
		Assert.assertEquals(sortedColumnValueListDesc, columnValueList, "AppVersion column data is not in descending order.");
		pageObj.utils().logPass("AppVersion column data is in descending order.");
	}

	@Test(description = "SQ-T6472 Verify the sorting functionality on Location Audit Log using event datetime")
	@Owner(name = "Kalpana")
	public void T6472_verifyTheSortingFunctionalityOnLocationAuditLogUsingEventDateTime() throws InterruptedException {
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
		pageObj.ppccLocationAuditLogPage().navigateToAuditLogs();

		// apply sort and verify data
		pageObj.ppccLocationAuditLogPage().sortColumn("Event DateTime");
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm a", Locale.ENGLISH);
		List<LocalDateTime> eventDateTimeValues = pageObj.ppccLocationAuditLogPage().getEventDateTimeColumnValues("7",
				formatter);
		List<LocalDateTime> sortedEventDateTimeValues = new ArrayList<>(eventDateTimeValues);
		Collections.sort(sortedEventDateTimeValues);
		Assert.assertEquals(eventDateTimeValues, sortedEventDateTimeValues, "Event date time column data is not in ascending order.");
		pageObj.utils().logPass("Event date time column data is in ascending order.");

		pageObj.ppccLocationAuditLogPage().sortColumn("Event DateTime");
		eventDateTimeValues = pageObj.ppccLocationAuditLogPage().getEventDateTimeColumnValues("7", formatter);
		List<LocalDateTime> sortedEventDateTimeDataDesc = new ArrayList<>(eventDateTimeValues);
		sortedEventDateTimeDataDesc.sort(Collections.reverseOrder());
		Assert.assertEquals(eventDateTimeValues, sortedEventDateTimeDataDesc, "Event date time column data is not in descending order.");
		pageObj.utils().logPass("Event date time column data is in descending order.");
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