/*
* @author Aman Jain (aman.jain@partech.com)
* @brief This class contains API test cases for the location config override APIs.
* @fileName ppccLocationConfigOverrideApiTest.java
*/

package com.punchh.server.apiTest;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
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

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.apiConfig.ApiConstants;

import io.restassured.response.Response;
import net.minidev.json.JSONObject;

@Listeners(TestListeners.class)
public class ppccLocationConfigOverrideApiTest {
    static Logger logger = LogManager.getLogger(ppccLocationConfigOverrideApiTest.class);
    public WebDriver driver;
    PageObj pageObj;
    String sTCName;
    String run = "api";
    private String env = "api";
    private String baseUrl;
    private static Map<String, String> dataSet;

    @BeforeMethod(alwaysRun = true)
    public void beforeMethod(Method method) {
        driver = new BrowserUtilities().launchBrowser();
        pageObj = new PageObj(driver);
        env = pageObj.getEnvDetails().setEnv();
        dataSet = new ConcurrentHashMap<>();
        baseUrl = pageObj.getEnvDetails().setBaseUrl();
        sTCName = method.getName();
        pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
        dataSet = pageObj.readData().readTestData;
        pageObj.readData().ReadDataFromJsonFileForClientSecretKey(pageObj.readData().getJsonFilePath("ui" , env , "Secrets"),
            dataSet.get("slug"));
        dataSet.putAll(pageObj.readData().readTestData);

        logger.info(sTCName + " ==>" + dataSet);
    }

    @Test(description = "SQ-T6586 Verify the response of config override APi", groups = { "regression" }, priority = 1)
    public void SQ_T6586_verifyTheFlowOfConfigOverrideApiForALocation() throws Exception{
        String token = pageObj.endpoints().getAuthTokenForPPCC(Integer.parseInt(dataSet.get("businessId")), dataSet.get("slug"), dataSet.get("businessName"));
        String queryParam = "?is_provisioned=False&search=" + dataSet.get("locationName");

        // creating the policy
        String status = "published";
        String policyName = pageObj.ppccUtilities().createPolicy(token, status);

        // getting the policy id
        int policyId = pageObj.ppccUtilities().getPolicyId(policyName, token);

        // getting the unprovisioned location's ids
        int locationId = pageObj.ppccUtilities().getLocationId(dataSet.get("locationName"), false, token);
        List<Integer> locationIds = Arrays.asList(locationId);

        // provisioning the location
        logger.info("Provision the location");
        Response provisionApiResponse = pageObj.endpoints().provisionApi(token, policyId, locationIds, dataSet.get("packageVersionId"));
        Assert.assertEquals(provisionApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for provisioning API");
        TestListeners.extentTest.get().pass("Provision location is giving 200 status code");

        // get the configurations which are overridable
        queryParam =  "?pos_type=1&is_overridable=true";
        logger.info("Hitting get configurations API");
        Response getConfigResponse = pageObj.endpoints().getConfigurations(token, queryParam);
        Assert.assertEquals(getConfigResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for get configurations API");
        TestListeners.extentTest.get().pass("Get configurations API is giving 200 status code");
        String overridableConfig = getConfigResponse.jsonPath().get("data[0].label");
        logger.info("Overridable config: " + overridableConfig);

        JSONObject configsToOverride = new JSONObject();
        configsToOverride.put(overridableConfig, "12");
        Response overrideConfig = pageObj.endpoints().overrideConfig(token, locationIds, configsToOverride);
        Assert.assertEquals(overrideConfig.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for override config API");

        //sending location listing api to get the data after config override
        logger.info("Hitting location listing API after overriding the configuration");
        queryParam = "?is_provisioned=True&search=" + dataSet.get("locationName");
        Response locationListResponse = pageObj.endpoints().getLocationList(token, queryParam);
        Assert.assertEquals(locationListResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for location list API");
        TestListeners.extentTest.get().pass("Location list API is giving 200 status code");
        Map<String, Object> configOverrides = locationListResponse
            .jsonPath()
            .getMap("data[0].config_overrides");
        Assert.assertEquals(configOverrides.get(overridableConfig), "12", overridableConfig + " is not coming as 12");
        String config_fetch_after = locationListResponse.jsonPath().get("data[0].config_fetch_after");
        String configFetchDate = config_fetch_after.substring(0, 10);
        Assert.assertEquals(configFetchDate, CreateDateTime.getCurrentDate());
        TestListeners.extentTest.get().pass("Config override applied: 'config_overrides' is not empty and contains expected values.");

        // deprovisioning the location
        logger.info("Deprovision the location");
        Response deprovisionApiResponse = pageObj.endpoints().deprovisionApi(token, locationIds);
        Assert.assertEquals(deprovisionApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for provisioning API");
        TestListeners.extentTest.get().pass("Deprovision location is giving 200 status code");

        // deleting the first policy
        pageObj.ppccUtilities().deletePolicy(policyId, token);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        pageObj.utils().clearDataSet(dataSet);
        driver.quit();
        logger.info("Browser closed");
    }
}
