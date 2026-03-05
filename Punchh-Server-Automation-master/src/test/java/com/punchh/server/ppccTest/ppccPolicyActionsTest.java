/*
 * @author Kalpana Singh (kalpana.singh@partech.com)
 * @brief This class contains UI test cases for the POS Control Center > Policy Actions.
 * @fileName ppccPolicyActionsTest.java
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
public class ppccPolicyActionsTest {
    static Logger logger = LogManager.getLogger(ppccPolicyActionsTest.class);
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

    @Test(description = "SQ-T5815 Verify the Policy Is created and published."
       + "SQ-T5816 Verify the View Action of Policy."
       +  "SQ-T5817 Verify the Delete Action of Policy."
       + "SQ-T5831 Verify the Duplicate Action of Policy.")
    @Owner(name = "Kalpana")
    public void T5815_verifyThePolicyIsCreatedAndPublished() throws InterruptedException {
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
        pageObj.ppccPolicyPage().navigateToPolicyManagementTab();
        pageObj.ppccPolicyPage().clickOnCreatePolicyButton();
        String createdPolicy=pageObj.ppccPolicyPage().defineGeneralSettings("Aloha");
        pageObj.ppccPolicyPage().enterCommonConfigurations("5","ro-RO");
        pageObj.ppccPolicyPage().enterPosConfigurations();
        pageObj.ppccPolicyPage().clickPublishButton();
        String matchPolicy=pageObj.ppccPolicyPage().searchPolicy(createdPolicy);
        Assert.assertEquals(createdPolicy,matchPolicy,"Policy is not created successfully");
        pageObj.utils().logPass("Policy is published successfully and Policy is being searched.");

        // this is to view action of policy
        pageObj.ppccPolicyPage().clickViewActionOfPolicy();
        String headerText = pageObj.ppccPolicyPage().verifyPolicyDetailsHeaderOnViewPolicyAction();
        Assert.assertEquals(headerText, "Policy Detail", "Policy is not opened in View Mode");
        pageObj.utils().logPass("Policy is opened in View Mode");
        String viewheaderText = pageObj.ppccPolicyPage().verifyViewOnlyHeaderOnViewPolicyAction();
        Assert.assertEquals(viewheaderText, "View Only", "Policy is not opened in View Mode");
        pageObj.utils().logPass("View Action is working as expected.");
        pageObj.ppccPolicyPage().clickOnBackButtonOnViewPolicy();
        pageObj.ppccPolicyPage().searchPolicy(createdPolicy);
        pageObj.utils().logPass("Policy is being searched.");

        // this is to duplicate the policy
        String duplicatedPolicyName= pageObj.ppccPolicyPage().duplicateActionOfPolicy();
        pageObj.ppccPolicyPage().publishPolicy();
        String duplicatedName = pageObj.ppccPolicyPage().searchPolicy(duplicatedPolicyName);
        Assert.assertEquals(duplicatedPolicyName, duplicatedName, "Policy is not duplicated successfully");
        pageObj.utils().logPass("Policy is duplicated successfully.");

        // this is to delete action of policy
        pageObj.ppccPolicyPage().searchPolicy(createdPolicy);
        pageObj.ppccPolicyPage().deleteActionOfPolicy();
        String deletedPolicyText = pageObj.ppccPolicyPage().verifyDeleteAction();
        Assert.assertEquals(deletedPolicyText, "There are no records found.", "Policy is not deleted successfully");
        pageObj.utils().logPass("Policy is deleted Successfully.");
        pageObj.ppccPolicyPage().searchPolicy(duplicatedName);
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
