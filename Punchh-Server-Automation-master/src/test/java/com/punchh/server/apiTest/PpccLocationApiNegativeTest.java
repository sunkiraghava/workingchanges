/*
* @author Aman Jain (aman.jain@partech.com)
* @brief This class contains API test cases for the location APIs.
* @fileName PpccLocationApiNegativeTest.java
*/

package com.punchh.server.apiTest;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.punchh.server.annotations.Owner;
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
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.apiConfig.ApiConstants;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class PpccLocationApiNegativeTest {
    static Logger logger = LogManager.getLogger(PpccLocationApiNegativeTest.class);
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

    @Test(description = "SQ-T6154 PPCC verify provisioning of location for non exisiting policy", groups = { "regression" }, priority = 1)
    @Owner(name = "Aman Jain")
    public void SQ_T6154_verifyProvisionLocationApiForNonExistingPolicy() throws Exception {

        String token = pageObj.endpoints().getAuthTokenForPPCC(Integer.parseInt(dataSet.get("businessId")), dataSet.get("slug"), dataSet.get("businessName"));
        int policyId = 0;

        // getting the unprovisioned location's ids
        int locationId = pageObj.ppccUtilities().getLocationId(dataSet.get("locationName"), false, token);
        List<Integer> locationIds = Arrays.asList(locationId);

        // provisioning the location using non existing policy Id
        pageObj.utils().logit("Provision the location using non existing policy Id");
        Response provisionApiResponse = pageObj.endpoints().provisionApi(token, policyId, locationIds, dataSet.get("packageVersionId"));
        Assert.assertEquals(provisionApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
                "Status code 400 did not match for provisioning API");
        Assert.assertEquals(provisionApiResponse.jsonPath().get("errors").toString(), dataSet.get("errorMsg"),
                "Error message for provisioning API did not match");
        pageObj.utils().logPass("Provision location is giving 400 status code and expected error message");
    }

    @Test(description = "SQ-T6155 PPCC verify provisioning of a non existing location", groups = { "regression" }, priority = 1)
    @Owner(name = "Aman Jain")
    public void SQ_T6155_verifyProvisionLocationApiNonExistingLocation() throws Exception {

        String token = pageObj.endpoints().getAuthTokenForPPCC(Integer.parseInt(dataSet.get("businessId")), dataSet.get("slug"), dataSet.get("businessName"));
        String status = "published";
        String policyName = pageObj.ppccUtilities().createPolicy(token, status);
        int policyId = pageObj.ppccUtilities().getPolicyId(policyName, token);

        int locationId = 0;
        List<Integer> locationIds = Arrays.asList(locationId);

        // provisioning the location
        pageObj.utils().logit("Provision the location using non existing location Id");
        Response provisionApiResponse = pageObj.endpoints().provisionApi(token, policyId, locationIds, dataSet.get("packageVersionId"));
        Assert.assertEquals(provisionApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
                "Status code 400 did not match for provisioning API");
        Assert.assertEquals(provisionApiResponse.jsonPath().get("errors").toString(), dataSet.get("errorMsg"),
                "Error message for provisioning API did not match");
        pageObj.utils().logPass("Provision location is giving 400 status code and expected error message");

        // deleting the policy
        pageObj.ppccUtilities().deletePolicy(policyId, token);
    }

    @Test(description = "SQ-T6193 PPCC verify provisioning of location using invalid auth code", groups = { "regression" }, priority = 1)
    @Owner(name = "Aman Jain")
    public void SQ_T6193_verifyProvisionLocationApiForInvalidAuthCode() throws Exception {

        String token = pageObj.endpoints().getAuthTokenForPPCC(Integer.parseInt(dataSet.get("businessId")), dataSet.get("slug"), dataSet.get("businessName"));
        int policyId = 0;

        // getting the unprovisioned location's ids 
        pageObj.utils().logit("Get the unprovisioned location's ids");
        String queryParam = "?is_provisioned=false&" + dataSet.get("locationName");
        Response locationListResponse = pageObj.endpoints().getLocationList(token, queryParam);
        Assert.assertEquals(locationListResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for location list API");
        pageObj.utils().logPass("Location list API is giving 200 status code");
        int locationId = locationListResponse.jsonPath().get("data[0].id");
        List<Integer> locationIds = Arrays.asList(locationId);

        // provisioning the location
        pageObj.utils().logit("Provision the location using invalid auth code");
        token = "";
        Response provisionApiResponse = pageObj.endpoints().provisionApi(token, policyId, locationIds, dataSet.get("packageVersionId"));
        Assert.assertEquals(provisionApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
                "Status code 401 did not match for provisioning API");
        pageObj.utils().logPass("Provision location is giving 401 status code");
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        pageObj.utils().clearDataSet(dataSet);
        driver.quit();
        logger.info("Browser closed");
    }
}
