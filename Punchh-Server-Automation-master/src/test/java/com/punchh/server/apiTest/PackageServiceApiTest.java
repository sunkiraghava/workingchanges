/*
 * @author Aman Jain (aman.jain@partech.com)
 * @brief This class contains API test cases for the Package service APIs.
 * @fileName PackageServiceApiTest.java
 */
package com.punchh.server.apiTest;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
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

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.RestAssured;
import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class PackageServiceApiTest {
    static Logger logger = LogManager.getLogger(PackageServiceApiTest.class);
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
        logger.info(sTCName + " ==>" + dataSet);
    }

    @Test(description = "SQ-T6285 PPCC verify the updating of package with empty token", groups = { "regression" }, priority = 1)
    public void SQ_T6285_VerifyUpdatePackageAPIWithInvalidToken() throws Exception {
    
        String packageId = dataSet.get("packageId");
        String token = "";
        String updatedDescription = "Description update by automation script " + Utilities.getRandomNo(1000);
        Response updatePackageResponse = pageObj.endpoints().updatePackage(packageId, updatedDescription, token);
        Assert.assertEquals(updatePackageResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED, "Status code is not 401");
        Assert.assertEquals(updatePackageResponse.jsonPath().getList("errors"), 
                    Arrays.asList("Token is invalid or expired."), 
                    "Response has no errors");
        Assert.assertEquals(updatePackageResponse.jsonPath().get("message"), "Not Authorized.", "Message do not match");
        TestListeners.extentTest.get().pass("With invalid token Update package API is throwing 401 status code");
    }

    @Test(description = "SQ-T837 Verify the response of package service APIs", groups = { "regression" }, priority = 1)
    public void SQ_T837_VerifyResponsesOfPackageServiceAPIs() throws Exception {

        String token = pageObj.endpoints().getAuthForPackagesService("IN_STORE");

        // publish package API
        Response publishPackageResponse = pageObj.endpoints().publishPackage(token, "resources/Testdata/pp/10.7.31.0.zip", "resources/Testdata/pp/10.7.31.0.json", "1.1.1.1");
        Assert.assertEquals(publishPackageResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code is not 201");
        Assert.assertEquals(publishPackageResponse.jsonPath().get("message"), "Successfully published the package.", "Messages do not match");
        Assert.assertEquals(publishPackageResponse.jsonPath().get("status"), "Success", "Status do not match");
        Assert.assertEquals(publishPackageResponse.jsonPath().getList("errors"), Collections.emptyList(), "Response has some errors");
        TestListeners.extentTest.get().pass("Publish package API executed successfully");

        // get packages list API
        String version = dataSet.get("version");
        String packageListQueryParam = "?search="+ version;
        Response getPackageListResponse = pageObj.endpoints().getPackageList(token, packageListQueryParam);
        Assert.assertEquals(getPackageListResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code is not 200");
        Assert.assertEquals(getPackageListResponse.jsonPath().get("message"), "Listing populated successfully!", "Messages do not match");
        Assert.assertEquals(getPackageListResponse.jsonPath().get("status"), "Success", "Status do not match");

        // Asserting components
        Assert.assertEquals(getPackageListResponse.jsonPath().getString("data[0].components[0].name"), "PunchhMonitor.exe", "Component name does not match");
        Assert.assertEquals(getPackageListResponse.jsonPath().getString("data[0].components[0].version"), "10.7.31.0", "Component version does not match");
        Assert.assertEquals(getPackageListResponse.jsonPath().getString("data[0].components[0].checksum"), "c94fcb5ce8cd2171c55381db97b68a4bc4990707710116d3af2ccd9947b92989", "Component checksum does not match");

        String createdAt = getPackageListResponse.jsonPath().getString("data[0].created_at");
        Instant createdAtInstant = Instant.parse(createdAt);
        Instant now = Instant.now();
        Duration diff = Duration.between(createdAtInstant, now);
        Assert.assertTrue(Math.abs(diff.toMinutes()) < 2, "Created date is not recent");

        Assert.assertEquals(getPackageListResponse.jsonPath().getString("data[0].description"), null, "Description is not null");
        Assert.assertEquals(getPackageListResponse.jsonPath().getInt("data[0].is_active"), 1, "is_active does not match");
        Assert.assertEquals(getPackageListResponse.jsonPath().getInt("data[0].is_assignable"), 1, "is_assignable does not match");
        Assert.assertEquals(getPackageListResponse.jsonPath().getInt("data[0].is_generic"), 0, "is_generic does not match");
        Assert.assertEquals(getPackageListResponse.jsonPath().getString("data[0].name"), "1.1.1.1", "Package name does not match");
        Assert.assertEquals(getPackageListResponse.jsonPath().getString("data[0].pos_type[0]"), "Aloha", "POS type does not match");

        // Asserting release_notes
        Assert.assertEquals(getPackageListResponse.jsonPath().getString("data[0].release_notes.external"), "Publish external release notes for package with automation", "External release notes do not match");
        Assert.assertEquals(getPackageListResponse.jsonPath().getString("data[0].release_notes.internal"), "Publish internal release notes for package with automation", "Internal release notes do not match");

        String packageId = getPackageListResponse.jsonPath().getString("data[0].version_id");
        Assert.assertEquals(getPackageListResponse.jsonPath().getString("data[0].stage"), "Development", "Stage does not match");
        Assert.assertEquals(getPackageListResponse.jsonPath().getString("data[0].storage_path"), packageId + "/1.1.1.1.zip", "Storage path does not match");
        Assert.assertEquals(getPackageListResponse.jsonPath().getInt("data[0].type_id"), 1, "Type ID does not match");
        Assert.assertEquals(getPackageListResponse.jsonPath().getString("data[0].version"), "1.1.1.1", "Version does not match");

        Assert.assertEquals(getPackageListResponse.jsonPath().getList("errors"), Collections.emptyList(), "Response has some errors");
        TestListeners.extentTest.get().pass("Get package API executed successfully");

        // update package API
        String updatedDescription = "Description update by automation script " + Utilities.getRandomNo(1000);
        Response updatePackageResponse = pageObj.endpoints().updatePackage(packageId, updatedDescription, token);
        Assert.assertEquals(updatePackageResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code is not 200");
        Assert.assertEquals(updatePackageResponse.jsonPath().get("message"), "Package updated successfully.", "Messages do not match");
        Assert.assertEquals(updatePackageResponse.jsonPath().get("status"), "Success", "Status do not match");
        Assert.assertEquals(updatePackageResponse.jsonPath().getList("errors"), Collections.emptyList(), "Response has some errors");
        TestListeners.extentTest.get().pass("Update package API executed successfully");

        // retrive package details API
        Response packageDetailsResponse = pageObj.endpoints().getPackageDetails(packageId, token);
        Assert.assertEquals(packageDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code is not 200");
        Assert.assertEquals(packageDetailsResponse.jsonPath().get("data.description"), updatedDescription, "Updated Description do not match");
        Assert.assertEquals(packageDetailsResponse.jsonPath().get("status"), "Success", "Status do not match");
        Assert.assertEquals(packageDetailsResponse.jsonPath().getList("errors"), Collections.emptyList(), "Response has some errors");

        String updatedAt = packageDetailsResponse.jsonPath().getString("data.updated_at");
        Instant updatedAtInstant = Instant.parse(updatedAt);
        now = Instant.now();
        diff = Duration.between(updatedAtInstant, now);
        Assert.assertTrue(Math.abs(diff.toMinutes()) < 2, "Updated date is not recent");
        TestListeners.extentTest.get().pass("Retrieve package details API executed successfully");

        // Asserting components
        Assert.assertEquals(packageDetailsResponse.jsonPath().getString("data.components[0].name"), "Updated" + getPackageListResponse.jsonPath().getString("data[0].components[0].name"), "Component name does not match");
        Assert.assertEquals(packageDetailsResponse.jsonPath().getString("data.components[0].version"), "Updated" + getPackageListResponse.jsonPath().getString("data[0].components[0].version"), "Component version does not match");
        Assert.assertEquals(packageDetailsResponse.jsonPath().getString("data.components[0].checksum"), "Updated" + getPackageListResponse.jsonPath().getString("data[0].components[0].checksum"), "Component checksum does not match");
        
        Assert.assertEquals(packageDetailsResponse.jsonPath().getString("data.created_at"), createdAt, "Created date is not recent");
        Assert.assertEquals(packageDetailsResponse.jsonPath().getString("data.description"), updatedDescription, "Description is not null");
        Assert.assertEquals(packageDetailsResponse.jsonPath().getInt("data.is_active"), getPackageListResponse.jsonPath().getInt("data[0].is_active"), "is_active does not match");
        Assert.assertEquals(packageDetailsResponse.jsonPath().getInt("data.is_assignable"), getPackageListResponse.jsonPath().getInt("data[0].is_assignable"), "is_assignable does not match");
        Assert.assertEquals(packageDetailsResponse.jsonPath().getInt("data.is_generic"), getPackageListResponse.jsonPath().getInt("data[0].is_generic"), "is_generic does not match");
        Assert.assertEquals(packageDetailsResponse.jsonPath().getString("data.name"), getPackageListResponse.jsonPath().getString("data[0].name"), "Package name does not match");
        Assert.assertEquals(packageDetailsResponse.jsonPath().getString("data.pos_type[0]"), getPackageListResponse.jsonPath().getString("data[0].pos_type[0]"), "POS type does not match");

        // Asserting release_notes
        Assert.assertEquals(packageDetailsResponse.jsonPath().getString("data.release_notes.external"), "Updated external release notes for package with automation", "External release notes do not match");
        Assert.assertEquals(packageDetailsResponse.jsonPath().getString("data.release_notes.internal"), "Updated internal release notes for package with automation", "Internal release notes do not match");

        Assert.assertEquals(packageDetailsResponse.jsonPath().getString("data.stage"), getPackageListResponse.jsonPath().getString("data[0].stage"), "Stage does not match");
        Assert.assertEquals(packageDetailsResponse.jsonPath().getString("data.storage_path"), getPackageListResponse.jsonPath().getString("data[0].storage_path"), "Storage path does not match");
        Assert.assertEquals(packageDetailsResponse.jsonPath().getInt("data.type_id"), getPackageListResponse.jsonPath().getInt("data[0].type_id"), "Type ID does not match");
        Assert.assertEquals(packageDetailsResponse.jsonPath().getString("data.version"), getPackageListResponse.jsonPath().getString("data[0].version"), "Version does not match");
        TestListeners.extentTest.get().pass("Retrieve package API executed successfully");

        // get package download API
        Response downloadPackageDetailsResponse = pageObj.endpoints().getPackageDownloadLink(token, packageId);
        Assert.assertEquals(downloadPackageDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code is not 200");
        String downloadLink = downloadPackageDetailsResponse.jsonPath().getString("data.download_link");
        Assert.assertNotNull(downloadLink, "Download link is null");
        Assert.assertFalse(downloadLink.isEmpty(), "Download link is empty");
        Assert.assertEquals(downloadPackageDetailsResponse.jsonPath().getList("errors"), Collections.emptyList(), "Response has some errors");
        Assert.assertEquals(downloadPackageDetailsResponse.jsonPath().get("message"), "Download link successfully Created.", "Messages do not match");
        Assert.assertEquals(downloadPackageDetailsResponse.jsonPath().get("status"), "Success", "Status do not match");
        TestListeners.extentTest.get().pass("Download package API executed successfully");
        
        // Verify downalod link works
        Response downloadResponse = RestAssured.given()
            .relaxedHTTPSValidation()
            .when()
            .get(downloadLink)
            .then()
            .extract()
            .response();

        Assert.assertEquals(downloadResponse.statusCode(), ApiConstants.HTTP_STATUS_OK, "Download failed - unexpected status code");
        TestListeners.extentTest.get().pass("Package successfully downloaded using the download link.");

        // delete package
        token = pageObj.endpoints().getAuthForPackagesService("PPCC_ADMIN_USERS");
        Response deletePackageResponse = pageObj.endpoints().deletePackage(token, packageId);
        Assert.assertEquals(deletePackageResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code is not 200");
        Assert.assertEquals(deletePackageResponse.jsonPath().get("message"), "Package deleted successfully.", "Messages do not match");
        Assert.assertEquals(deletePackageResponse.jsonPath().get("status"), "Success", "Status do not match");
        Assert.assertEquals(deletePackageResponse.jsonPath().getList("errors"), Collections.emptyList(), "Response has some errors");
        TestListeners.extentTest.get().pass("Delete package API executed successfully");
    }

    @AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
