/*
 * @author Kalpana Singh (kalpana.singh@partech.com)
 * @brief This class contains UI test cases for the POS Control Center > Policy Management tab.
 * @fileName ppccPolicyTest.java
 */

package com.punchh.server.ppccTest;

import java.lang.reflect.Method;
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
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)

public class ppccPolicyTest {
	static Logger logger = LogManager.getLogger(ppccPolicyTest.class);
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
        logger.info(sTCName + " ==>" + dataSet);
        pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
    }

	@Test(description = "SQ-T3110 Validate if the flag \"Has POS Integration\" is enabled with Premium, then POS Control Center will show on the punchh dashboard.")
	@Owner(name = "Kalpana")
	public void T3110_verifyPosControlCenterWhenHasPosIntegrationEnabledWithPremium() throws InterruptedException {
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.ppccPolicyPage().enableHasPosIntegrationCheckBox();
		pageObj.ppccPolicyPage().checkEnablePosControlCenterCheckBoxWithPremium();
		pageObj.ppccPolicyPage().clickOnUpdateButton();
		pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
		String headerText = pageObj.ppccPolicyPage().verifyPosControlCenterTab();
		Assert.assertEquals(headerText, "POS Control Center Manage", "POS control center tab is not displayed");
		pageObj.utils().logPass("POS Control Center is enabled with Premium Subscription.");
	}

	@Test(description = "SQ-T6276 Validate if the POS type filter functionality for PPCC policy mgmt is working as expected", groups = {
        "regression" }, priority = 1)
    @Owner(name = "Aman Jain")
	public void T6276_verifyPPCCPolicyMgmtPosTypeFilterFunctionality() throws InterruptedException {
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
		pageObj.ppccPolicyPage().navigateToPolicyManagementTab();
		pageObj.ppccPolicyPage().clickOnCreatePolicyButton();
		String policyName = pageObj.ppccPolicyPage().defineGeneralSettings("Aloha");
		pageObj.ppccPolicyPage().enterCommonConfigurations("5", "ro-RO");
		pageObj.ppccPolicyPage().enterPosConfigurations();
        pageObj.ppccPolicyPage().clickPublishButton();
		pageObj.utils().waitTillPagePaceDone();
		String filterOption = dataSet.get("filterName");
        String filterValue = dataSet.get("itemToFilter");
        String filterValueXpath = pageObj.utils().getLocatorValue("ppccPolicyPage.filterValue").replace("{filterValue}", filterValue);
        String filterOptionXpath = pageObj.utils().getLocatorValue("ppccPolicyPage.filterValue").replace("{filterValue}", filterOption);
        boolean isDataFiltered = pageObj.ppccPolicyPage().isDataFiltered(filterOptionXpath, filterValueXpath, 2, filterValue);
        Assert.assertTrue(isDataFiltered, "Filtered results do not match expected value!");
        pageObj.utils().logPass("Filtered results match expected value.");
		pageObj.ppccPolicyPage().searchPolicy(policyName);
		pageObj.utils().logPass("Policy is being searched.");
		pageObj.ppccPolicyPage().deleteActionOfPolicy();
		pageObj.utils().logPass("Created Policy is deleted Successfully.");
    }

	@Test(description = "SQ-T6277 Verify the Policy is created and published for RPOS", groups = {
        "regression" }, priority = 1)
	@Owner(name = "Aman Jain")
	public void T6277_verifyThePolicyIsCreatedAndPublishedForRPOS() throws InterruptedException {
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
		pageObj.ppccPolicyPage().navigateToPolicyManagementTab();
		pageObj.ppccPolicyPage().clickOnCreatePolicyButton();
		String createdPolicy=pageObj.ppccPolicyPage().defineGeneralSettings("RPOS");
		pageObj.ppccPolicyPage().enterCommonConfigurations("5","ro-RO");
		pageObj.ppccPolicyPage().enterPOSConfigForRpos();
        pageObj.ppccPolicyPage().clickPublishButton();
		String matchPolicy=pageObj.ppccPolicyPage().searchPolicy(createdPolicy);
		Assert.assertEquals(createdPolicy,matchPolicy,"Policy is not created successfully");
		pageObj.utils().logPass("Policy is published successfully and Policy is being searched.");
		pageObj.ppccPolicyPage().searchPolicy(createdPolicy);
		pageObj.utils().logPass("Policy is being searched.");
		pageObj.ppccPolicyPage().deleteActionOfPolicy();
		pageObj.utils().logPass("Created Policy is deleted Successfully.");
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

