/*
 * @author Aman Jain (aman.jain@partech.com)
 * @brief This class contains UI test cases for the POS Control Center > All Locations.
 * @fileName ppccAllLocationsTest.java
 */

package com.punchh.server.ppccTest;

import java.lang.reflect.Method;
import java.util.List;
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

import com.github.javafaker.Faker;
import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class ppccAllLocationsTest {
    static Logger logger = LogManager.getLogger(ppccAllLocationsTest.class);
    public WebDriver driver;
    private Properties prop;
    private PageObj pageObj;
    private String sTCName;
    private String env, run = "ui";
    private String baseUrl;
    private static Map<String, String> dataSet;
    Faker faker;

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
        faker = new Faker();
        logger.info(sTCName + " ==>" + dataSet);
        pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
    }

    @Test(description = "SQ-T6754 Verify the cancel update action for all locations", groups = { "regression" }, priority = 1)
    @Owner(name = "Aman Jain")
    public void T6754_verifyTheSelectAllLocationCancelUpdate() throws InterruptedException{
        logger.info("Hitting location listing API after overriding the configuration");
        String queryParam = "?is_provisioned=False";
        String token = pageObj.endpoints().getAuthTokenForPPCC(Integer.parseInt(dataSet.get("businessId")), dataSet.get("slug"), dataSet.get("businessName"));
        Response locationListResponse = pageObj.endpoints().getLocationList(token, queryParam);
        Assert.assertEquals(locationListResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for location list API");

        List<Integer> locationIds = locationListResponse.jsonPath().getList("data.id", Integer.class);
        if (!locationIds.isEmpty())
        {
            logger.info("Provision the location");
            String status = "published";
            String policyName = pageObj.ppccUtilities().createPolicy(token, status);
            int policyId =  pageObj.ppccUtilities().getPolicyId(policyName, token);
            Response provisionApiResponse = pageObj.endpoints().provisionApi(token, policyId, locationIds, dataSet.get("packageVersionId"));
            Assert.assertEquals(provisionApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for provisioning API");
        }

        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
        pageObj.ppccLocationPage().navigateToLocationsTab();
        pageObj.ppccLocationPage().cancelUpdateAllLocations();
        String messageText = pageObj.ppccLocationPage().getTitleText();
        Assert.assertEquals(messageText, dataSet.get("expectedMessage"));
    }

    @Test(description = "SQ-T6755 Verify the remote upgrade action for all locations", groups = { "regression" }, priority = 1)
    @Owner(name = "Aman Jain")
    public void T6755_verifyTheSelectAllLocationRemoteUpgrade() throws InterruptedException{
        logger.info("Hitting location listing API after overriding the configuration");
        String queryParam = "?is_provisioned=False";
        String token = pageObj.endpoints().getAuthTokenForPPCC(Integer.parseInt(dataSet.get("businessId")), dataSet.get("slug"), dataSet.get("businessName"));
        Response locationListResponse = pageObj.endpoints().getLocationList(token, queryParam);
        Assert.assertEquals(locationListResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for location list API");

        List<Integer> locationIds = locationListResponse.jsonPath().getList("data.id", Integer.class);
        if (!locationIds.isEmpty())
        {
            logger.info("Provision the location");
            String status = "published";
            String policyName = pageObj.ppccUtilities().createPolicy(token, status);
            int policyId =  pageObj.ppccUtilities().getPolicyId(policyName, token);
            Response provisionApiResponse = pageObj.endpoints().provisionApi(token, policyId, locationIds, dataSet.get("packageVersionId"));
            Assert.assertEquals(provisionApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for provisioning API");
        }

        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
        pageObj.ppccLocationPage().navigateToLocationsTab();
        pageObj.ppccLocationPage().remoteUpgradeALocation(dataSet.get("packageVersionforRemoteUpgrade"));
        String messageText = pageObj.ppccLocationPage().getTitleText();
        Assert.assertEquals(messageText, dataSet.get("expectedMessage"));
    }

    @Test(description = "SQ-T6756 Verify the change policy action for all locations", groups = { "regression" }, priority = 1)
    @Owner(name = "Aman Jain")
    public void T6756_verifyTheSelectAllLocationChangePolicy() throws InterruptedException{
        String status = "published";
        logger.info("Hitting location listing API after overriding the configuration");
        String queryParam = "?is_provisioned=False";
        String token = pageObj.endpoints().getAuthTokenForPPCC(Integer.parseInt(dataSet.get("businessId")), dataSet.get("slug"), dataSet.get("businessName"));
        Response locationListResponse = pageObj.endpoints().getLocationList(token, queryParam);
        Assert.assertEquals(locationListResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for location list API");
        String policyName = pageObj.ppccUtilities().createPolicy(token, status);
        List<Integer> locationIds = locationListResponse.jsonPath().getList("data.id", Integer.class);
        if (!locationIds.isEmpty())
        {
            logger.info("Provision the location");
            int policyId =  pageObj.ppccUtilities().getPolicyId(policyName, token);
            Response provisionApiResponse = pageObj.endpoints().provisionApi(token, policyId, locationIds, dataSet.get("packageVersionId"));
            Assert.assertEquals(provisionApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for provisioning API");
        }

        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
        pageObj.ppccLocationPage().navigateToLocationsTab();
        pageObj.ppccLocationPage().changePolicyALocation(policyName);
        String messageText = pageObj.ppccLocationPage().getTitleText();
        Assert.assertEquals(messageText, dataSet.get("expectedMessage"));
        pageObj.utils().logPass("Policy is changed for provisioned location successfully");
    }

    @Test(description = "SQ-T6757 Verify the initiate update action for all locations", groups = { "regression" }, priority = 1)
    @Owner(name = "Aman Jain")
    public void T6757_verifyTheSelectAllLocationInitiateUpdate() throws InterruptedException{
        logger.info("Hitting location listing API after overriding the configuration");
        String queryParam = "?is_provisioned=False";
        String token = pageObj.endpoints().getAuthTokenForPPCC(Integer.parseInt(dataSet.get("businessId")), dataSet.get("slug"), dataSet.get("businessName"));
        Response locationListResponse = pageObj.endpoints().getLocationList(token, queryParam);
        Assert.assertEquals(locationListResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for location list API");

        List<Integer> locationIds = locationListResponse.jsonPath().getList("data.id", Integer.class);
        if (!locationIds.isEmpty())
        {
            logger.info("Provision the location");
            String status = "published";
            String policyName = pageObj.ppccUtilities().createPolicy(token, status);
            int policyId =  pageObj.ppccUtilities().getPolicyId(policyName, token);
            Response provisionApiResponse = pageObj.endpoints().provisionApi(token, policyId, locationIds, dataSet.get("packageVersionId"));
            Assert.assertEquals(provisionApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for provisioning API");
        }

        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
        pageObj.ppccLocationPage().navigateToLocationsTab();
        pageObj.ppccLocationPage().initiateUpdateAllLocations();
        String messageText = pageObj.ppccLocationPage().getTitleText();
        Assert.assertEquals(messageText, dataSet.get("expectedMessage"));
        pageObj.utils().logPass("Initiate update has been done successfully");
    }

    @Test(description = "SQ-T6758 Verify the config override action for all locations", groups = { "regression" }, priority = 1)
    @Owner(name = "Aman Jain")
    public void T6758_verifyTheSelectAllLocationConfigOverride() throws InterruptedException{
        logger.info("Hitting location listing API after overriding the configuration");
        String queryParam = "?is_provisioned=False";
        String token = pageObj.endpoints().getAuthTokenForPPCC(Integer.parseInt(dataSet.get("businessId")), dataSet.get("slug"), dataSet.get("businessName"));
        Response locationListResponse = pageObj.endpoints().getLocationList(token, queryParam);
        Assert.assertEquals(locationListResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for location list API");

        List<Integer> locationIds = locationListResponse.jsonPath().getList("data.id", Integer.class);
        queryParam = "?is_provisioned=True";
        locationListResponse = pageObj.endpoints().getLocationList(token, queryParam);
        Assert.assertEquals(locationListResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for location list API");
        List<Integer> provisionedLocationIds = locationListResponse.jsonPath().getList("data.id", Integer.class);
        int provisionedLocationsCount = provisionedLocationIds.size();
        if (provisionedLocationsCount > 50) {provisionedLocationsCount = 50;}
        if (!locationIds.isEmpty())
        {
            logger.info("Provision the location");
            String status = "published";
            String policyName = pageObj.ppccUtilities().createPolicy(token, status);
            int policyId =  pageObj.ppccUtilities().getPolicyId(policyName, token);
            Response provisionApiResponse = pageObj.endpoints().provisionApi(token, policyId, locationIds, dataSet.get("packageVersionId"));
            Assert.assertEquals(provisionApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for provisioning API");
        }

        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
        pageObj.ppccLocationPage().navigateToLocationsTab();

        String fieldName = dataSet.get("fieldName");
        String fieldValue = faker.lorem().characters(5);
        pageObj.utils().getLocator("ppccLocationPage.selectAllLocationsCheckbox").click();
        pageObj.ppccLocationPage().overrideConfigs(fieldName,fieldValue);
        boolean isConfigOverrideVerified = pageObj.ppccLocationPage().verifyValuesSavedInConfigOverridePopUp(fieldName, fieldValue);
        Assert.assertTrue(isConfigOverrideVerified, "Values are not saved successfully in Config Override Pop Up");
        pageObj.utils().logPass("Values are saved successfully in Config Override Pop Up");

        pageObj.utils().getLocator("ppccLocationPage.selectAllLocationsCheckbox").click();
        pageObj.ppccLocationPage().clickOnClearOverridesInConfigOverridePopUp();
        boolean isConfigOverrideCleared=pageObj.ppccLocationPage().verifyConfigOverrideIsCleared();
        Assert.assertTrue(isConfigOverrideCleared, "Config Override is not cleared successfully");
        pageObj.utils().logPass("Config Override is cleared successfully");
    }

    @Test(description = "SQ-T6759 Verify the de-provision action for all locations", groups = { "regression" }, priority = 1)
    @Owner(name = "Aman Jain")
    public void T6759_verifyTheSelectAllLocationDeprovision() throws InterruptedException{

        logger.info("Hitting location listing API after overriding the configuration");
        String queryParam = "?is_provisioned=False";
        String token = pageObj.endpoints().getAuthTokenForPPCC(Integer.parseInt(dataSet.get("businessId")), dataSet.get("slug"), dataSet.get("businessName"));
        Response locationListResponse = pageObj.endpoints().getLocationList(token, queryParam);
        Assert.assertEquals(locationListResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for location list API");

        List<Integer> locationIds = locationListResponse.jsonPath().getList("data.id", Integer.class);
        if (!locationIds.isEmpty())
        {
            logger.info("Provision the location");
            String status = "published";
            String policyName = pageObj.ppccUtilities().createPolicy(token, status);
            int policyId =  pageObj.ppccUtilities().getPolicyId(policyName, token);
            Response provisionApiResponse = pageObj.endpoints().provisionApi(token, policyId, locationIds, dataSet.get("packageVersionId"));
            Assert.assertEquals(provisionApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for provisioning API");
        }

        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
        pageObj.ppccLocationPage().navigateToLocationsTab();

        pageObj.ppccLocationPage().deProvisionALocation();
        String messageText = pageObj.ppccLocationPage().getTitleText();
        Assert.assertEquals(messageText, dataSet.get("expectedMessage"));
        pageObj.utils().logPass("Locations are deprovisioned successfully");
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
