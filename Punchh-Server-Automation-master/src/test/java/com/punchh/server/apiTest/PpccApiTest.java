package com.punchh.server.apiTest;

import org.testng.annotations.Test;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.Listeners;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.CreateDateTime;

import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;
import com.punchh.server.apiConfig.ApiConstants;

import Support.ConfigurationClass;
import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class PpccApiTest {
    static Logger logger = LogManager.getLogger(PpccApiTest.class);
    public WebDriver driver;
    String userEmail;
    String email = "AutoApiTemp@punchh.com";
    Properties uiProp;
    Properties prop;
    PageObj pageObj;
    String sTCName;
    String run = "api";
    private String env = "api";
    private static Map<String, String> dataSet;
    private Utilities utils;

    @BeforeMethod(alwaysRun = true)
    public void beforeMethod(Method method) {
        uiProp = Utilities.loadPropertiesFile("config.properties");
        utils = new Utilities();
        pageObj = new PageObj(driver);
        env = pageObj.getEnvDetails().setEnv();
        dataSet = new ConcurrentHashMap<>();
        sTCName = method.getName();
        pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
        dataSet = pageObj.readData().readTestData;
        logger.info(sTCName + " ==>" + dataSet);
    }

    @Test(description = "PPCC-T734 PPCC verify fetch config api")
    public void PPCC_T734_verifyFetchConfigAndHeartBeatApi() {

        //fetch config api
        logger.info("== PPCC Fetch Config API ==");
        Response fetchConfigResponse = pageObj.endpoints().fetchConfig(dataSet.get("locationKey"));
        Assert.assertEquals(fetchConfigResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for Fetch configuration API");
        String lastUpdatedAt = fetchConfigResponse.jsonPath().get("data.last_updated_at");
        logger.info("Last Update at is fetched :  " + lastUpdatedAt);
        logger.info("Last Updated at of policy is successfully taken");
        TestListeners.extentTest.get().pass("Verified fetch config api");

        // heartbeat api
        logger.info("== PPCC Heart Beat API ==");
        Response heartbeatApiResponse = pageObj.endpoints().heartbeatApi(dataSet.get("locationKey"),dataSet.get("packageVersion"),dataSet.get("packageVersionId"),lastUpdatedAt);
        Assert.assertEquals(heartbeatApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for heartbeat API");
        String isPolicyChanged = heartbeatApiResponse.getHeader("is-policy-changed");
        String isPackageChanged = heartbeatApiResponse.getHeader("is-package-changed");
        String remoteUpgrade = heartbeatApiResponse.getHeader("remote-upgrade");
        logger.info("Policy Status is : " + isPolicyChanged);
        logger.info("Package Status is : " + isPackageChanged);
        logger.info("Remote Upgrade Status for a location is : " + remoteUpgrade);
        TestListeners.extentTest.get().pass("Verified heartbeat api");
    }
}
