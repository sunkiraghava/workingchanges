/*
* @author Aman Jain (aman.jain@partech.com)
* @brief This class contains UI test cases for the POS Control Center Policy Audit Logs.
* @fileName ppccPolicyFilterAuditLogsTest.java
*/

package com.punchh.server.ppccTest;

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

@Listeners(TestListeners.class)
public class ppccPublishedPolicyAuditLogsTest {
	static Logger logger = LogManager.getLogger(ppccPolicyFilterAuditLogsTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;

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
				pageObj.readData().getJsonFilePath("ui", env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T6278 Validate if the Policy name filter functionality for PPCC policy audit logs is working as expected", groups = {
			"regression" }, priority = 1)
	@Owner(name = "Aman Jain")
	public void SQ_T6278_verifyPPCCPolicyAuditLogsPolicyNameFilterFunctionality() throws InterruptedException {

		String token = pageObj.endpoints().getAuthTokenForPPCC(Integer.parseInt(dataSet.get("businessId")),
				dataSet.get("slug"), dataSet.get("businessName"));
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
		String headerText = pageObj.ppccPolicyPage().verifyPosControlCenterTab();
		Assert.assertEquals(headerText, "POS Control Center Manage", "POS control center tab is not displayed");
		pageObj.ppccPolicyPage().navigateToPolicyManagementTab();
		String status = "published";
		String policyName = pageObj.ppccUtilities().createPolicy(token, status);
		int policyId = pageObj.ppccUtilities().getPolicyId(policyName, token);
		pageObj.ppccPolicyPage().navigateToAuditLogs();

		String filterValue = policyName;
		String filterOption = dataSet.get("filterName");
		String filterValueXpath = pageObj.utils().getLocatorValue("ppccPolicyAuditLog.filterValue")
				.replace("{filterValue}", filterValue);
		String filterOptionXpath = pageObj.utils().getLocatorValue("ppccPolicyAuditLog.filterValue")
				.replace("{filterValue}", filterOption);
		boolean isDataFiltered = pageObj.ppccPolicyAuditLogs().isDataFiltered(filterOptionXpath, filterValueXpath, 1,
				filterValue);
		Assert.assertTrue(isDataFiltered, "Filtered results do not match expected value!");
		pageObj.utils().logPass("Filtered results match expected value.");

		pageObj.ppccPolicyAuditLogs().getGoToPolicyMgmtPageLocator().click();
		pageObj.ppccUtilities().deletePolicy(policyId, token);
	}

	@Test(description = "SQ-T6278 Validate if the Policy status filter functionality for PPCC policy audit logs is working as expected", groups = {
			"regression" }, priority = 1)
	@Owner(name = "Aman Jain")
	public void SQ_T6278_verifyPPCCPolicyAuditLogsPolicyStatusFilterFunctionality() throws InterruptedException {

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
		String headerText = pageObj.ppccPolicyPage().verifyPosControlCenterTab();
		Assert.assertEquals(headerText, "POS Control Center Manage", "POS control center tab is not displayed");
		pageObj.ppccPolicyPage().navigateToPolicyManagementTab();
		pageObj.ppccPolicyPage().navigateToAuditLogs();

		String filterOption = dataSet.get("filterName");
		String filterValue = dataSet.get("itemToFilter");
		String filterValueXpath = pageObj.utils().getLocatorValue("ppccPolicyAuditLog.filterValue")
				.replace("{filterValue}", filterValue);
		String filterOptionXpath = pageObj.utils().getLocatorValue("ppccPolicyAuditLog.filterValue")
				.replace("{filterValue}", filterOption);
		boolean isDataFiltered = pageObj.ppccPolicyAuditLogs().isDataFiltered(filterOptionXpath, filterValueXpath, 3,
				filterValue);
		Assert.assertTrue(isDataFiltered, "Filtered results do not match expected value!");
		pageObj.utils().logPass("Filtered results match expected value.");
	}

	@Test(description = "SQ-T6278 Validate if the event type filter functionality for PPCC policy audit logs is working as expected", groups = {
			"regression" }, priority = 1)
	@Owner(name = "Aman Jain")
	public void SQ_T6278_verifyPPCCPolicyAuditLogsEventTypeFilterFunctionality() throws InterruptedException {

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
		String headerText = pageObj.ppccPolicyPage().verifyPosControlCenterTab();
		Assert.assertEquals(headerText, "POS Control Center Manage", "POS control center tab is not displayed");
		pageObj.ppccPolicyPage().navigateToPolicyManagementTab();
		pageObj.ppccPolicyPage().navigateToAuditLogs();

		String filterOption = dataSet.get("filterName");
		String filterValue = dataSet.get("itemToFilter");
		String filterValueXpath = pageObj.utils().getLocatorValue("ppccPolicyAuditLog.filterValue")
				.replace("{filterValue}", filterValue);
		String filterOptionXpath = pageObj.utils().getLocatorValue("ppccPolicyAuditLog.filterValue")
				.replace("{filterValue}", filterOption);
		boolean isDataFiltered = pageObj.ppccPolicyAuditLogs().isDataFiltered(filterOptionXpath, filterValueXpath, 5,
				filterValue);
		Assert.assertTrue(isDataFiltered, "Filtered results do not match expected value!");
		pageObj.utils().logPass("Filtered results match expected value.");
	}

	@Test(description = "SQ-T6278 Validate if the user name filter functionality for PPCC policy audit logs is working as expected", groups = {
			"regression" }, priority = 1)
	@Owner(name = "Aman Jain")
	public void SQ_T6278_verifyPPCCPolicyAuditLogsUserNameFilterFunctionality() throws InterruptedException {

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
		String headerText = pageObj.ppccPolicyPage().verifyPosControlCenterTab();
		Assert.assertEquals(headerText, "POS Control Center Manage", "POS control center tab is not displayed");
		pageObj.ppccPolicyPage().navigateToPolicyManagementTab();
		pageObj.ppccPolicyPage().navigateToAuditLogs();

		String filterOption = dataSet.get("filterName");
		String filterValue = dataSet.get("itemToFilter");
		String filterValueXpath = pageObj.utils().getLocatorValue("ppccPolicyAuditLog.filterValue")
				.replace("{filterValue}", filterValue);
		String filterOptionXpath = pageObj.utils().getLocatorValue("ppccPolicyAuditLog.filterValue")
				.replace("{filterValue}", filterOption);
		boolean isDataFiltered = pageObj.ppccPolicyAuditLogs().isDataFiltered(filterOptionXpath, filterValueXpath, 4,
				filterValue);
		Assert.assertTrue(isDataFiltered, "Filtered results do not match expected value!");
		pageObj.utils().logPass("Filtered results match expected value.");
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
