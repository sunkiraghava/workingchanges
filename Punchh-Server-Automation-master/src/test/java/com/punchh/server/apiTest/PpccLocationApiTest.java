/*
 * @author Aman Jain (aman.jain@partech.com)
 * @brief This class contains API test cases for the location APIs.
 * @fileName PpccLocationApiTest.java
 */

package com.punchh.server.apiTest;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.apiConfig.ApiConstants;

import io.restassured.response.Response;
import net.minidev.json.JSONObject;

@Listeners(TestListeners.class)
public class PpccLocationApiTest {
	static Logger logger = LogManager.getLogger(PpccLocationApiTest.class);
	public WebDriver driver;
	PageObj pageObj;
	String sTCName;
	String run = "api";
	private String env = "api";
	private static Map<String, String> dataSet;
	int businessId;
	String slug;
	String businessName;

	@BeforeMethod(alwaysRun = true)
	public void beforeMethod(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		sTCName = method.getName();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath("ui", env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);

		businessId = Integer.parseInt(dataSet.get("businessId"));
		slug = dataSet.get("slug");
		businessName = dataSet.get("businessName");
	}

	@Test(description = "SQ-T6153 PPCC verify provisioning and de-provisioning locations api", groups = {
			"regression" }, priority = 1)
    @Owner(name = "Aman Jain")
	public void SQ_T6153_verifyProvisionAndDeprovisionLocationApi() throws Exception {

		String token = pageObj.endpoints().getAuthTokenForPPCC(businessId, slug, businessName);
		String status = "published";
		String policyName = pageObj.ppccUtilities().createPolicy(token, status);
		int policyId = pageObj.ppccUtilities().getPolicyId(policyName, token);

		// getting the unprovisioned location's ids
		int locationId = pageObj.ppccUtilities().getLocationId(dataSet.get("locationName"), false, token);
		List<Integer> locationIds = Arrays.asList(locationId);

		// provisioning the location
		logger.info("Provision the location");
		Response provisionApiResponse = pageObj.endpoints().provisionApi(token, policyId, locationIds,
				dataSet.get("packageVersionId"));
		Assert.assertEquals(provisionApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for provisioning API");
		TestListeners.extentTest.get().pass("Provision location is giving 200 status code");

		// checking audit logs
		logger.info("Checking audit logs for provisioning");
		String queryParam = "?location_id=" + locationId + "&event_type=provisioned";
		Response locationListAuditLogsResponse = pageObj.endpoints().getLocationListAuditLogs(token, queryParam);
		Assert.assertEquals(locationListAuditLogsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for get location audit logs API");
		int lastIndex = locationListAuditLogsResponse.jsonPath().getList("data").size() - 1;
		Assert.assertEquals(
				locationListAuditLogsResponse.jsonPath().getString("data[" + lastIndex + "].event_type_display"),
				"Provisioned", "Event type display does not match");
		Assert.assertEquals(
				locationListAuditLogsResponse.jsonPath().getString("data[" + lastIndex + "].config_status_display"),
				"Ready to Install", "Config status display does not match");
		Assert.assertEquals(
				locationListAuditLogsResponse.jsonPath().getString("data[" + lastIndex + "].package_status_display"),
				"Ready to Install", "Package status display does not match");
		int auditLogId = locationListAuditLogsResponse.jsonPath().getInt("data[" + lastIndex + "].id");
		Assert.assertEquals(locationListAuditLogsResponse.jsonPath().getInt("data[" + lastIndex + "].location_id"),
				locationId, "Location ID does not match");
		Assert.assertEquals(locationListAuditLogsResponse.jsonPath().getString("data[" + lastIndex + "].event_type"),
				"provisioned", "Event type does not match");
		Assert.assertEquals(locationListAuditLogsResponse.jsonPath().getString("data[" + lastIndex + "].username"),
				"testAutomation@ppcc.com", "Username does not match");
		Assert.assertEquals(locationListAuditLogsResponse.jsonPath().getString("data[" + lastIndex + "].email_address"),
				"testAutomation@ppcc.com", "Email address does not match");
		Assert.assertEquals(locationListAuditLogsResponse.jsonPath().getString("data[" + lastIndex + "].config_status"),
				"ready_to_install", "Config status does not match");
		Assert.assertEquals(
				locationListAuditLogsResponse.jsonPath().getString("data[" + lastIndex + "].package_status"),
				"ready_to_install", "Package status does not match");
		Assert.assertEquals(
				locationListAuditLogsResponse.jsonPath()
						.getString("data[" + lastIndex + "].location_package_version_id"),
				dataSet.get("packageVersionId"), "Location package version ID does not match");
		Assert.assertEquals(
				locationListAuditLogsResponse.jsonPath().getString("data[" + lastIndex + "].store_app_version"), null,
				"Store app version should be null");
		Assert.assertTrue(locationListAuditLogsResponse.jsonPath().getList("errors").isEmpty(),
				"Errors list is not empty");
		TestListeners.extentTest.get().pass("Get location audit logs is giving 200 status code");

		// getting the location audit logs filters
		logger.info("Get the location audit logs filters");
		Response getLocationAuditLogsFiltersResponse = pageObj.endpoints().getLocationAuditLogsFilters(token,
				queryParam);
		Assert.assertEquals(getLocationAuditLogsFiltersResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for location audit logs filters metadata API");

		List<String> expectedLocationFilters = Arrays.asList("event_type", "store_name", "username", "location_id",
				"package_version", "store_app_version", "config_status", "package_status");

		Set<String> actualLocationFilters = getLocationAuditLogsFiltersResponse.jsonPath().getMap("data").keySet()
				.stream().map(Object::toString).collect(Collectors.toSet());

		Assert.assertEquals(new HashSet<>(actualLocationFilters), new HashSet<>(expectedLocationFilters),
				"Mismatch in keys present in 'data'");

		List<String> expectedEventTypes = Arrays.asList("Provisioned", "Deprovisioned", "Reprovisioned",
				"Updates Cancelled", "Initiate Update", "Remote Upgrade", "Config Override");
		List<String> actualEventTypes = getLocationAuditLogsFiltersResponse.jsonPath().getList("data.event_type");
		Assert.assertEqualsNoOrder(actualEventTypes.toArray(), expectedEventTypes.toArray(),
				"Event Type values do not match expected values");

		List<String> expectedConfigStatus = Arrays.asList("Pending Update", "Ready to Install", "Unprovisioned",
				"Synced", "Updates Cancelled");
		List<String> actualConfigStatus = getLocationAuditLogsFiltersResponse.jsonPath().getList("data.config_status");
		Assert.assertEqualsNoOrder(actualConfigStatus.toArray(), expectedConfigStatus.toArray(),
				"Config status values do not match expected values");

		List<String> expectedPackageStatus = Arrays.asList("Pending Update", "Ready to Install", "Unprovisioned",
				"Synced");
		List<String> actualPackageStatus = getLocationAuditLogsFiltersResponse.jsonPath()
				.getList("data.package_status");
		Assert.assertEqualsNoOrder(actualPackageStatus.toArray(), expectedPackageStatus.toArray(),
				"Package status values do not match expected values");

		// retrieve Location Audit Logs API
		logger.info("retrieveing Location Audit Logs details");
		queryParam = "";
		Response retrieveLocationAuditLogsResponse = pageObj.endpoints().retrieveLocationAuditLogs(token, queryParam,
				auditLogId);
		Assert.assertEquals(retrieveLocationAuditLogsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for retrieve Location Audit Logs details API");
		Assert.assertEquals(retrieveLocationAuditLogsResponse.jsonPath().getString("data.event_type_display"),
				"Provisioned", "Event type display mismatch");
		Assert.assertEquals(retrieveLocationAuditLogsResponse.jsonPath().getString("data.config_status_display"),
				"Ready to Install", "Config status display mismatch");
		Assert.assertEquals(retrieveLocationAuditLogsResponse.jsonPath().getString("data.package_status_display"),
				"Ready to Install", "Package status display mismatch");
		Assert.assertTrue(retrieveLocationAuditLogsResponse.jsonPath().getBoolean("data.policy_redirection"),
				"Policy redirection should be true");
		Assert.assertEquals(retrieveLocationAuditLogsResponse.jsonPath().getInt("data.id"), auditLogId, "ID mismatch");
		Assert.assertEquals(retrieveLocationAuditLogsResponse.jsonPath().getInt("data.location_id"), locationId,
				"Location ID mismatch");
		Assert.assertEquals(retrieveLocationAuditLogsResponse.jsonPath().getString("data.event_type"), "provisioned",
				"Event type mismatch");
		Assert.assertEquals(retrieveLocationAuditLogsResponse.jsonPath().getString("data.username"),
				"testAutomation@ppcc.com", "Username mismatch");
		Assert.assertEquals(retrieveLocationAuditLogsResponse.jsonPath().getString("data.email_address"),
				"testAutomation@ppcc.com", "Email address mismatch");
		Assert.assertEquals(retrieveLocationAuditLogsResponse.jsonPath().getString("data.config_status"),
				"ready_to_install", "Config status mismatch");
		Assert.assertEquals(retrieveLocationAuditLogsResponse.jsonPath().getString("data.package_status"),
				"ready_to_install", "Package status mismatch");
		Assert.assertEquals(retrieveLocationAuditLogsResponse.jsonPath().getInt("data.policy_id"), policyId,
				"Policy ID mismatch");
		Assert.assertEquals(retrieveLocationAuditLogsResponse.jsonPath().getString("data.location_package_version_id"),
				dataSet.get("packageVersionId"), "Package version ID mismatch");
		Assert.assertNull(retrieveLocationAuditLogsResponse.jsonPath().get("data.store_app_version"),
				"Store app version should be null");
		Assert.assertEquals(retrieveLocationAuditLogsResponse.jsonPath().getInt("code"), 200, "Response code mismatch");
		Assert.assertTrue(retrieveLocationAuditLogsResponse.jsonPath().getList("errors").isEmpty(),
				"Errors list is not empty");
		Assert.assertEquals(retrieveLocationAuditLogsResponse.jsonPath().getString("data.changelog['POS Type'][1]"),
				"Aloha", "POS Type mismatch");
		Assert.assertEquals(retrieveLocationAuditLogsResponse.jsonPath().getInt("data.changelog['Policy ID'][1]"),
				policyId, "Policy ID mismatch");
		Assert.assertEquals(retrieveLocationAuditLogsResponse.jsonPath().getString("data.changelog['Policy Name'][1]"),
				policyName, "Policy Name mismatch");
		Assert.assertEquals(
				retrieveLocationAuditLogsResponse.jsonPath().getString("data.changelog['Config Status'][1]"),
				"Ready to Install", "Config Status mismatch");
		Assert.assertEquals(
				retrieveLocationAuditLogsResponse.jsonPath().getString("data.changelog['Package Status'][1]"),
				"Ready to Install", "Package Status mismatch");
		Assert.assertTrue(
				retrieveLocationAuditLogsResponse.jsonPath().getBoolean("data.changelog['Remote Upgrade'][1]"),
				"Remote Upgrade should be true");
		Assert.assertEquals(
				retrieveLocationAuditLogsResponse.jsonPath().getString("data.changelog['Package Version ID'][1]"),
				dataSet.get("packageVersionId"), "Package Version ID mismatch");
		TestListeners.extentTest.get().pass("retrieve Location Audit Logs API is giving 200 status code");

		// deprovisioning the location
		logger.info("Deprovision the location");
		Response deprovisionApiResponse = pageObj.endpoints().deprovisionApi(token, locationIds);
		Assert.assertEquals(deprovisionApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for deprovisioning API");
		TestListeners.extentTest.get().pass("Deprovision location is giving 200 status code");
		pageObj.ppccUtilities().deletePolicy(policyId, token);
	}

	@Test(description = "SQ-T6194 PPCC verify provisioning of multiple locations at once", groups = {
			"regression" }, priority = 1)
    @Owner(name = "Aman Jain")
	public void SQ_T6194_verifyProvisionAndDeprovisionLocationApiForMultipleLocations() throws Exception {

		String token = pageObj.endpoints().getAuthTokenForPPCC(businessId, slug, businessName);
		String status = "published";
		String policyName = pageObj.ppccUtilities().createPolicy(token, status);
		int policyId = pageObj.ppccUtilities().getPolicyId(policyName, token);
		// getting the unprovisioned location's ids
		int firstLocationId = pageObj.ppccUtilities().getLocationId(dataSet.get("firstLocationName"), false, token);
		int secondLocationId = pageObj.ppccUtilities().getLocationId(dataSet.get("secondLocationName"), false, token);
		List<Integer> locationIds = Arrays.asList(firstLocationId, secondLocationId);

		// provisioning the location
		logger.info("Provision the location");
		Response provisionApiResponse = pageObj.endpoints().provisionApi(token, policyId, locationIds,
				dataSet.get("packageVersionId"));
		Assert.assertEquals(provisionApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for provisioning API");
		Assert.assertEquals(provisionApiResponse.jsonPath().get("data").toString(), dataSet.get("successMsg"),
				"Locations provisioned successfully message did not match");
		TestListeners.extentTest.get().pass("Provision location is giving 200 status code and expected message");

		// deprovisioning the location
		logger.info("Deprovision the location");
		Response deprovisionApiResponse = pageObj.endpoints().deprovisionApi(token, locationIds);
		Assert.assertEquals(deprovisionApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for provisioning API");
		TestListeners.extentTest.get().pass("Deprovision location is giving 200 status code");
		pageObj.ppccUtilities().deletePolicy(policyId, token);
	}

	@Test(description = "SQ-T6195 PPCC verify multiple times provisioning for a single location", groups = {
			"regression" }, priority = 1)
    @Owner(name = "Aman Jain")
	public void SQ_T6195_verifyMultipleTimesProvisionForSameLocation() throws Exception {
		String token = pageObj.endpoints().getAuthTokenForPPCC(businessId, slug, businessName);
		String status = "published";
		String policyName = pageObj.ppccUtilities().createPolicy(token, status);
		int policyId = pageObj.ppccUtilities().getPolicyId(policyName, token);
		// getting the unprovisioned location's ids
		int locationId = pageObj.ppccUtilities().getLocationId(dataSet.get("locationName"), false, token);
		List<Integer> locationIds = Arrays.asList(locationId);

		// provisioning the location
		logger.info("Provision the location");
		Response provisionApiResponse = pageObj.endpoints().provisionApi(token, policyId, locationIds,
				dataSet.get("packageVersionId"));
		Assert.assertEquals(provisionApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for provisioning API");
		Assert.assertEquals(provisionApiResponse.jsonPath().get("data").toString(), dataSet.get("successMsg"),
				"Locations provisioned successfully message did not match");
		TestListeners.extentTest.get().pass("Provision location is giving 200 status code and expected message");

		// provisioning the location again
		logger.info("Provision the location again");
		provisionApiResponse = pageObj.endpoints().provisionApi(token, policyId, locationIds,
				dataSet.get("packageVersionId"));
		Assert.assertEquals(provisionApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for provisioning API");
		Assert.assertEquals(provisionApiResponse.jsonPath().get("errors").toString(),
				dataSet.get("errorMsg").replace("{locations}", locationIds.toString()),
				"Error message for provisioning API did not match");
		TestListeners.extentTest.get().pass("Provision location is giving 400 status code and expected error message");

		// deprovisioning the location
		logger.info("Deprovision the location");
		Response deprovisionApiResponse = pageObj.endpoints().deprovisionApi(token, locationIds);
		Assert.assertEquals(deprovisionApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for provisioning API");
		TestListeners.extentTest.get().pass("Deprovision location is giving 200 status code");
		pageObj.ppccUtilities().deletePolicy(policyId, token);
	}

	@Test(description = "SQ-T6196, SQ-T6197 Verify the response of Fetch Config and Location Listing API after overriding config", groups = {
			"regression" }, priority = 1)
    @Owner(name = "Aman Jain")
	public void SQ_T6196_T6197_verifyTheResponseOfFetchConfigAndLocationListingAPIAfterOverrideConfig()
			throws Exception {
		String token = pageObj.endpoints().getAuthTokenForPPCC(businessId, slug, businessName);
		String status = "published";
		String policyName = pageObj.ppccUtilities().createPolicy(token, status);
		int policyId = pageObj.ppccUtilities().getPolicyId(policyName, token);
		// getting the unprovisioned location's ids
		int locationId = pageObj.ppccUtilities().getLocationId(dataSet.get("locationName"), false, token);
		List<Integer> locationIds = Arrays.asList(locationId);

		// provisioning the location
		logger.info("Provision the location");
		Response provisionApiResponse = pageObj.endpoints().provisionApi(token, policyId, locationIds,
				dataSet.get("packageVersionId"));
		Assert.assertEquals(provisionApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for provisioning API");
		TestListeners.extentTest.get().pass("Provision location is giving 200 status code");

		// get the configurations which are overridable
		String queryParam = "?pos_type=1&is_overridable=true";
		logger.info("Hitting get configurations API");
		Response getConfigResponse = pageObj.endpoints().getConfigurations(token, queryParam);
		Assert.assertEquals(getConfigResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for get configurations API");
		TestListeners.extentTest.get().pass("Get configurations API is giving 200 status code");
		String overridableConfig = getConfigResponse.jsonPath().get("data[0].label");
		String overridableConfigApiKey = overridableConfig.trim().toLowerCase().replaceAll("\\s+", "_");
		logger.info("Overridable config: " + overridableConfig);

		JSONObject configsToOverride = new JSONObject();
		configsToOverride.put(overridableConfig, "12");
		Response overrideConfig = pageObj.endpoints().overrideConfig(token, locationIds, configsToOverride);
		Assert.assertEquals(overrideConfig.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for override config API");

		// sending fetch config api to get the data after config override
		logger.info("Hitting fetch config API after overriding the configuration");
		Response fetchConfigResponse = pageObj.endpoints().fetchConfig(dataSet.get("locationKey"));
		Assert.assertEquals(fetchConfigResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Fetch configuration API");
		String overridedConfigKey = "data.config.pos_config." + overridableConfigApiKey;
		Assert.assertEquals(fetchConfigResponse.jsonPath().getString(overridedConfigKey), "12",
				overridableConfig + " is not coming as 12");
		TestListeners.extentTest.get()
				.pass("After overriding the config, the returned configs reflect the overridden values.");

		// sending location listing api to get the data after config override
		logger.info("Hitting location listing API after overriding the configuration");
		queryParam = "?is_provisioned=True&search=" + dataSet.get("locationName");
		Response locationListResponse = pageObj.endpoints().getLocationList(token, queryParam);
		Assert.assertEquals(locationListResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for location list API");
		TestListeners.extentTest.get().pass("Location list API is giving 200 status code");
		Map<String, Object> configOverrides = locationListResponse.jsonPath().getMap("data[0].config_overrides");
		Assert.assertEquals(configOverrides.get(overridableConfig), "12", overridableConfig + " is not coming as 12");
		String config_fetch_after = locationListResponse.jsonPath().get("data[0].config_fetch_after");
		String configFetchDate = config_fetch_after.substring(0, 10);
		Assert.assertEquals(configFetchDate, CreateDateTime.getCurrentDate());
		TestListeners.extentTest.get()
				.pass("Config override applied: 'config_overrides' is not empty and contains expected values.");

		// deprovisioning the location
		logger.info("Deprovision the location");
		Response deprovisionApiResponse = pageObj.endpoints().deprovisionApi(token, locationIds);
		Assert.assertEquals(deprovisionApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for provisioning API");
		TestListeners.extentTest.get().pass("Deprovision location is giving 200 status code");
		pageObj.ppccUtilities().deletePolicy(policyId, token);
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
