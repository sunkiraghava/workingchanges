/*
 * @author Aman Jain (aman.jain@partech.com)
 * @brief This class contains API test cases for the policy APIs.
 * @fileName PpccPolicyApiTest.java
 */

 package com.punchh.server.apiTest;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.github.javafaker.Faker;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.apiConfig.ApiConstants;

import io.restassured.response.Response;
 
@Listeners(TestListeners.class)
public class PpccPolicyApiTest {
    static Logger logger = LogManager.getLogger(PpccPolicyApiTest.class);
    public WebDriver driver;
    PageObj pageObj;
    String sTCName;
    String run = "api";
    private String env = "api";
    private String baseUrl;
    private static Map<String, String> dataSet;
    Faker faker;
 
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
        faker = new Faker();
    }
 
    @Test(description = "SQ-T6317 Verify the responses of Policy CRUD APIs", groups = { "regression" }, priority = 1)
    public void SQ_T6317_verifyTheResponseOfPolicyAPIs() throws Exception {
 
        String token = pageObj.endpoints().getAuthTokenForPPCC(Integer.parseInt(dataSet.get("businessId")), dataSet.get("slug"), dataSet.get("businessName"));
        String status = "published";

        // Add policy
        String policyName = pageObj.ppccUtilities().createPolicy(token, status);

        // getting the policy id
        int policyId = pageObj.ppccUtilities().getPolicyId(policyName, token);

        // checking audit logs
        logger.info("Checking audit logs for policy creation");
        String queryParam = "?policy_status=Published&event_type=Created&policy_name=" + policyName;
        Response policyListAuditLogsResponse = pageObj.endpoints().getPolicyListAuditLogs(token, queryParam);
        Assert.assertEquals(policyListAuditLogsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for get policy audit logs API");

        Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getString("data[0].event_type_display"), "Created", "Event Type Display does not match");
        Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getString("data[0].policy_status_display"), "Published", "Policy Status Display does not match");
        int auditLogId = policyListAuditLogsResponse.jsonPath().getInt("data[0].id");
        Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getInt("data[0].policy_id"), policyId, "Policy ID does not match");
        Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getString("data[0].event_type"), "created", "Event Type does not match");
        Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getString("data[0].username"), "testAutomation@ppcc.com", "Username does not match");
        Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getString("data[0].email_address"), "testAutomation@ppcc.com", "Email Address does not match");
        Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getString("data[0].policy_name"), policyName, "Policy Name does not match");
        Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getString("data[0].policy_status"), "published", "Policy Status does not match");
        TestListeners.extentTest.get().pass("Get policy audit logs is giving 200 status code");

        // retrieve Policy Audit Logs
        logger.info("retrieve Policy Audit Logs for policy creation");
        queryParam = "";
        Response retrievePolicyAuditLogsResponse = pageObj.endpoints().retrievePolicyAuditLogs(token, queryParam, auditLogId);
        Assert.assertEquals(retrievePolicyAuditLogsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for retrieve Policy Audit Logs API");
        Assert.assertEquals(retrievePolicyAuditLogsResponse.jsonPath().getString("data.event_type_display"), "Created", "Event Type Display does not match");
        Assert.assertEquals(retrievePolicyAuditLogsResponse.jsonPath().getString("data.policy_status_display"), "Published", "Policy Status Display does not match");
        Assert.assertTrue(retrievePolicyAuditLogsResponse.jsonPath().getBoolean("data.policy_redirection"), "Policy Redirection should be false");
        Assert.assertEquals(retrievePolicyAuditLogsResponse.jsonPath().getInt("data.id"), auditLogId, "ID does not match");
        Assert.assertEquals(retrievePolicyAuditLogsResponse.jsonPath().getInt("data.policy_id"), policyId, "Policy ID does not match");
        Assert.assertEquals(retrievePolicyAuditLogsResponse.jsonPath().getString("data.event_type"), "created", "Event Type does not match");
        Assert.assertEquals(retrievePolicyAuditLogsResponse.jsonPath().getString("data.username"), "testAutomation@ppcc.com", "Username does not match");
        Assert.assertEquals(retrievePolicyAuditLogsResponse.jsonPath().getString("data.email_address"), "testAutomation@ppcc.com", "Email Address does not match");
        Assert.assertEquals(retrievePolicyAuditLogsResponse.jsonPath().getString("data.policy_name"), policyName, "Policy Name does not match");
        Assert.assertEquals(retrievePolicyAuditLogsResponse.jsonPath().getString("data.policy_status"), "published", "Policy Status does not match");
        Assert.assertEquals(retrievePolicyAuditLogsResponse.jsonPath().getInt("data.business_id"), 713, "Business ID does not match");
        
        // getting the policy audit logs filters
        logger.info("Get the policy audit logs filters");
        Response getPolicyAuditLogsFiltersResponse = pageObj.endpoints().getPolicyAuditLogsFilters(token, queryParam);
        Assert.assertEquals(getPolicyAuditLogsFiltersResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for policy audit logs filters metadata API");

        List<String> expectedPolicyFilters = Arrays.asList("event_type", "policy_name", "username", "policy_id", "policy_status");
        Set<String> actualPolicyFilters = getPolicyAuditLogsFiltersResponse.jsonPath()
                                                                .getMap("data")
                                                                .keySet()
                                                                .stream()
                                                                .map(Object::toString)
                                                                .collect(Collectors.toSet());
        Assert.assertEquals(new HashSet<>(actualPolicyFilters), new HashSet<>(expectedPolicyFilters), 
        "Mismatch in keys present in 'data'");

        List<String> expectedEventTypes = Arrays.asList("Updated", "Created", "Deleted", "Duplicated");
        List<String> actualEventTypes = getPolicyAuditLogsFiltersResponse.jsonPath().getList("data.event_type");
        Assert.assertEqualsNoOrder(actualEventTypes.toArray(), expectedEventTypes.toArray(), "Event Type values do not match expected values");

        List<String> expectedPolicyStatus = Arrays.asList("Draft", "Published");
        List<String> actualPolicyStatus = getPolicyAuditLogsFiltersResponse.jsonPath().getList("data.policy_status");
        Assert.assertEqualsNoOrder(actualPolicyStatus.toArray(), expectedPolicyStatus.toArray(), "Policy status values do not match expected values");

        // getting the policy details
        logger.info("Get the policy details");
        Response policyDetailsResponse = pageObj.endpoints().getPolicyDetails(token, policyId);
        Assert.assertEquals(policyDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for policy list API");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.name").toString(), policyName, "Policy Name do not matches");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.description").toString(), policyName + " Description", "Policy Description does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().getInt("data.pos_type_id"), 1, "POS Type ID does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.status").toString(), "published", "Status does not match");

        // POS Config assertions
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config.Port"), "8008", "Port does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Comp ID']"), "1222", "Comp ID does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['UI Mode']"), "FULL", "UI Mode does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Use MSR']"), "false", "Use MSR does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Print QRC']"), "false", "Print QRC does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Order Items']"), "false", "Order Items does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Use Spanish']"), "0", "Use Spanish does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Void Reason']"), "999", "Void Reason does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Earn Message']"), "", "Earn Message does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Allow Be Back']"), "true", "Allow Be Back does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Auto Check-In']"), "true", "Auto Check-In does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Coupon Prefix']"), "", "Coupon Prefix does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Gift Card Url']"), "", "Gift Card Url does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Scan Any Time']"), "false", "Scan Any Time does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Venmo Item ID']"), "8008", "Venmo Item ID does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Gift Card User']"), "", "Gift Card User does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['PayPal Item ID']"), "8008", "PayPal Item ID does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Punchh Item ID']"), "1111", "Punchh Item ID does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Redeem Item ID']"), "1000", "Redeem Item ID does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Redeem Message']"), "Redeem Message", "Redeem Message does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Payment Item ID']"), "", "Payment Item ID does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Venmo Tender ID']"), "8008", "Venmo Tender ID does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Gift Card Vendor']"), "", "Gift Card Vendor does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Menu Item Prefix']"), "", "Menu Item Prefix does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['PayPal Tender ID']"), "12", "PayPal Tender ID does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Barcode on Redeem']"), "false", "Barcode on Redeem does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Gift Card Item ID']"), "", "Gift Card Item ID does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Payment Tender ID']"), "", "Payment Tender ID does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Barcode On Checkin']"), "false", "Barcode On Checkin does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Barcode On Reprint']"), "false", "Barcode On Reprint does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Gift Card Password']"), "", "Gift Card Password does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Gift Card Store ID']"), "", "Gift Card Store ID does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().getList("data.config.pos_config['Update Time Window']").get(0), "09:00 PM-2:00 AM", "Update Time Window does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Enable Loyalty Chit']"), "false", "Enable Loyalty Chit does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Gift Card Tender ID']"), "", "Gift Card Tender ID does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Third Party Item ID']"), "", "Third Party Item ID does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Allow Single Sign On']"), "false", "Allow Single Sign On does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Auto Close On Redeem']"), "true", "Auto Close On Redeem does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Auto Create Customer']"), "false", "Auto Create Customer does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Filter Item Category']"), "", "Filter Item Category does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Gift Card Proxy Port']"), "", "Gift Card Proxy Port does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Gift Card Routing ID']"), "", "Gift Card Routing ID does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Points Item Category']"), "", "Points Item Category does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Points Only Customer']"), "false", "Points Only Customer does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Barcode On Zero Check']"), "false", "Barcode On Zero Check does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Disable By Order Mode']"), "", "Disable By Order Mode does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Gift Card Merchant ID']"), "", "Gift Card Merchant ID does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['SSF Auto Apply Payment']"), "true", "SSF Auto Apply Payment does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Enable Barcode Printing']"), "true", "Enable Barcode Printing does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Gift Card Merchant Name']"), "", "Gift Card Merchant Name does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Disable By Revenue Center']"), "", "Disable By Revenue Center does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Has Gift Card Integration']"), "false", "Has Gift Card Integration does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Allow Alpha Keyboard Popup']"), "true", "Allow Alpha Keyboard Popup does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Use Barcode Scan Interface']"), "false", "Use Barcode Scan Interface does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.pos_config['Barcode Only on Closed Check']"), "true", "Barcode Only on Closed Check does not match");
        Map<String, Object> policyConfig = policyDetailsResponse.jsonPath().getMap("data.config.pos_config");
        Map<String, Object> policyCommonConfig = policyDetailsResponse.jsonPath().getMap("data.config.common_config");

        // Common Config assertions
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.common_config['ISL URL']"), "https://isl.staging.punchh.io/", "ISL URL does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.common_config['POS URL']"), "https://mobileapi.staging.punchh.io", "POS URL does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.common_config['Language']"), "en-US", "Language does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.common_config['XML Mode']"), "true", "XML Mode does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.common_config['Log Level']"), "6", "Log Level does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.common_config['Regex Filter']"), "", "Regex Filter does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.common_config['LOG UPLOAD URL']"), "https://punchhapi.staging.punchh.io", "LOG UPLOAD URL does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.common_config['Keep Socket Open']"), "false", "Keep Socket Open does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.common_config['Maintenance Start Time']"), "4:00 AM", "Maintenance Start Time does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.config.common_config['POS Configuration Update Interval']"), "60", "POS Configuration Update Interval does not match");
        TestListeners.extentTest.get().pass("Get policy Details is giving 200 status code and expected message");

        // update policy
        logger.info("Update the policy");
        int posTypeId = 1; // For Aloha pos Type Id is 1.
        Response updatePolicyResponse = pageObj.endpoints().updatePolicy(token, policyId, policyName, posTypeId, "published");
        Assert.assertEquals(updatePolicyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code is not 200");
        Assert.assertEquals(updatePolicyResponse.jsonPath().get("data"), "Policy `Updated " + policyName + "` updated successfuly", "Messages do not match");
        Assert.assertEquals(updatePolicyResponse.jsonPath().get("metadata"), Collections.emptyMap(), "Metadata has some value");
        Assert.assertEquals(updatePolicyResponse.jsonPath().getList("errors"), Collections.emptyList(), "Response has some errors");
        TestListeners.extentTest.get().pass("Update policy is giving 200 status code and expected message");

        // checking audit logs for update policy
        logger.info("Checking audit logs for policy updation");
        policyName = "Updated " + policyName;
        queryParam = "?policy_status=Published&event_type=Updated&policy_name=" + policyName;
        policyListAuditLogsResponse = pageObj.endpoints().getPolicyListAuditLogs(token, queryParam);
        Assert.assertEquals(policyListAuditLogsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for get policy audit logs API");

        Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getString("data[0].event_type_display"), "Updated", "Event Type Display does not match");
        Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getString("data[0].policy_status_display"), "Published", "Policy Status Display does not match");
        Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getInt("data[0].policy_id"), policyId, "Policy ID does not match");
        Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getString("data[0].event_type"), "updated", "Event Type does not match");
        Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getString("data[0].username"), "testAutomation@ppcc.com", "Username does not match");
        Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getString("data[0].email_address"), "testAutomation@ppcc.com", "Email Address does not match");
        Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getString("data[0].policy_name"), policyName, "Policy Name does not match");
        Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getString("data[0].policy_status"), "published", "Policy Status does not match");
        TestListeners.extentTest.get().pass("Get policy audit logs is giving 200 status code");

        // getting the policy details
        logger.info("Get the policy details");
        Response updatedPolicyDetailsResponse = pageObj.endpoints().getPolicyDetails(token, policyId);
        Assert.assertEquals(updatedPolicyDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for policy details API");
        Assert.assertEquals(updatedPolicyDetailsResponse.jsonPath().get("data.name").toString(), policyName, "Policy Name do not matches");
        Assert.assertEquals(updatedPolicyDetailsResponse.jsonPath().get("data.description").toString(), policyName + " Description", "Policy Description does not match");
        Assert.assertEquals(updatedPolicyDetailsResponse.jsonPath().getInt("data.pos_type_id"), 1, "POS Type ID does not match");
        Assert.assertEquals(updatedPolicyDetailsResponse.jsonPath().get("data.status").toString(), "published", "Status does not match");

        Map<String, Object> updatedPolicyConfig = updatedPolicyDetailsResponse.jsonPath().getMap("data.config.pos_config");
        Map<String, Object> updatedPolicyCommonConfig = updatedPolicyDetailsResponse.jsonPath().getMap("data.config.common_config");
        Assert.assertEquals(updatedPolicyConfig, policyConfig, "POS configs does not match");
        Assert.assertEquals(updatedPolicyCommonConfig, policyCommonConfig, "Common configs does not match");
        TestListeners.extentTest.get().pass("Get policy details is giving 200 status code and expected message");

       pageObj.ppccUtilities().deletePolicy(policyId, token);

       // checking audit logs for delete policy
       logger.info("Checking audit logs for policy deletion");
       queryParam = "?policy_status=Published&event_type=Deleted&policy_name=" + policyName;
       policyListAuditLogsResponse = pageObj.endpoints().getPolicyListAuditLogs(token, queryParam);
       Assert.assertEquals(policyListAuditLogsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
               "Status code 200 did not match for get policy audit logs API");

       Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getString("data[0].event_type_display"), "Deleted", "Event Type Display does not match");
       Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getString("data[0].policy_status_display"), "Published", "Policy Status Display does not match");
       Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getInt("data[0].policy_id"), policyId, "Policy ID does not match");
       Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getString("data[0].event_type"), "deleted", "Event Type does not match");
       Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getString("data[0].username"), "testAutomation@ppcc.com", "Username does not match");
       Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getString("data[0].email_address"), "testAutomation@ppcc.com", "Email Address does not match");
       Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getString("data[0].policy_name"), policyName, "Policy Name does not match");
       Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getString("data[0].policy_status"), "published", "Policy Status does not match");
       TestListeners.extentTest.get().pass("Get policy audit logs is giving 200 status code");
    }

    @Test(description = "SQ-T6318 Verify the flow of duplicate Policy List API", groups = { "regression" }, priority = 1)
    public void SQ_T6318_verifyTheFlowOfDuplicatePolicyAPI() throws Exception {

        String token = pageObj.endpoints().getAuthTokenForPPCC(Integer.parseInt(dataSet.get("businessId")), dataSet.get("slug"), dataSet.get("businessName"));
        String status = "published";

        // Add policy
        String policyName = pageObj.ppccUtilities().createPolicy(token, status);

        // getting the policy id
        int policyId = pageObj.ppccUtilities().getPolicyId(policyName, token);

        // duplicate the policy with the same name
        int posTypeId = 1; // For Aloha pos Type Id is 1.
        String queryParam = "?duplicate=true";
        Response duplicatePolicyResponse = pageObj.endpoints().duplicatePolicy(token, policyName, posTypeId, queryParam, status);
        Assert.assertEquals(duplicatePolicyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST, "Duplicate Policy with same name's Status code is not 400");
        Assert.assertEquals(duplicatePolicyResponse.jsonPath().get("data"), Collections.emptyMap(), "Duplicate Policy with same name's data is not empty");
        Assert.assertEquals(duplicatePolicyResponse.jsonPath().get("metadata"), Collections.emptyMap(), "Duplicate Policy with same name's Metadata has some value");
        List<String> expectedErrors = Arrays.asList("Policy with provided name already exists.");
        Assert.assertEquals(
            duplicatePolicyResponse.jsonPath().getList("errors"),
            expectedErrors,
            "Duplicate Policy with same name's Response has some other errors"
        );

        // duplicate policy with a different name
        String duplicatePolicyName = "AUT " + faker.lorem().characters(5);
        duplicatePolicyResponse = pageObj.endpoints().duplicatePolicy(token, duplicatePolicyName, posTypeId, queryParam, status);
        Assert.assertEquals(duplicatePolicyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Duplicate Policy Status code is not 200");
        Assert.assertEquals(duplicatePolicyResponse.jsonPath().get("data"), "Policy `" + duplicatePolicyName + "` duplicated successfully", "Duplicate Policy Messages do not match");
        Assert.assertEquals(duplicatePolicyResponse.jsonPath().get("metadata"), Collections.emptyMap(), "Duplicate Policy Metadata has some value");
        Assert.assertEquals(duplicatePolicyResponse.jsonPath().getList("errors"), Collections.emptyList(), "Duplicate Policy Response has some errors");
        TestListeners.extentTest.get().pass("Duplicate Policy API executed successfully");

        // getting the duplicate policy's id
        int duplicatePolicyId = pageObj.ppccUtilities().getPolicyId(duplicatePolicyName, token);

        // checking audit logs
        logger.info("Checking audit logs for policy duplication");
        queryParam = "?policy_status=Published&event_type=Duplicated&policy_name=" + duplicatePolicyName;
        Response policyListAuditLogsResponse = pageObj.endpoints().getPolicyListAuditLogs(token, queryParam);
        Assert.assertEquals(policyListAuditLogsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for get policy audit logs API");

        Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getString("data[0].event_type_display"), "Duplicated", "Event Type Display does not match");
        Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getString("data[0].policy_status_display"), "Published", "Policy Status Display does not match");
        int auditLogId = policyListAuditLogsResponse.jsonPath().getInt("data[0].id");
        Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getInt("data[0].policy_id"), duplicatePolicyId, "Policy ID does not match");
        Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getString("data[0].event_type"), "duplicated", "Event Type does not match");
        Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getString("data[0].username"), "testAutomation@ppcc.com", "Username does not match");
        Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getString("data[0].email_address"), "testAutomation@ppcc.com", "Email Address does not match");
        Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getString("data[0].policy_name"), duplicatePolicyName, "Policy Name does not match");
        Assert.assertEquals(policyListAuditLogsResponse.jsonPath().getString("data[0].policy_status"), "published", "Policy Status does not match");
        TestListeners.extentTest.get().pass("Get policy audit logs is giving 200 status code");

        // retrieve Policy Audit Logs
        logger.info("retrieve Policy Audit Logs for policy duplication");
        queryParam = "";
        Response retrievePolicyAuditLogsResponse = pageObj.endpoints().retrievePolicyAuditLogs(token, queryParam, auditLogId);
        Assert.assertEquals(retrievePolicyAuditLogsResponse.jsonPath().getString("data.event_type_display"), "Duplicated", "Event Type Display does not match");
        Assert.assertEquals(retrievePolicyAuditLogsResponse.jsonPath().getString("data.policy_status_display"), "Published", "Policy Status Display does not match");
        Assert.assertTrue(retrievePolicyAuditLogsResponse.jsonPath().getBoolean("data.policy_redirection"), "Policy Redirection should be false");
        Assert.assertEquals(retrievePolicyAuditLogsResponse.jsonPath().getInt("data.id"), auditLogId, "ID does not match");
        Assert.assertEquals(retrievePolicyAuditLogsResponse.jsonPath().getInt("data.policy_id"), duplicatePolicyId, "Policy ID does not match");
        Assert.assertEquals(retrievePolicyAuditLogsResponse.jsonPath().getString("data.event_type"), "duplicated", "Event Type does not match");
        Assert.assertEquals(retrievePolicyAuditLogsResponse.jsonPath().getString("data.username"), "testAutomation@ppcc.com", "Username does not match");
        Assert.assertEquals(retrievePolicyAuditLogsResponse.jsonPath().getString("data.email_address"), "testAutomation@ppcc.com", "Email Address does not match");
        Assert.assertEquals(retrievePolicyAuditLogsResponse.jsonPath().getString("data.policy_name"), duplicatePolicyName, "Policy Name does not match");
        Assert.assertEquals(retrievePolicyAuditLogsResponse.jsonPath().getString("data.policy_status"), "published", "Policy Status does not match");
        Assert.assertEquals(retrievePolicyAuditLogsResponse.jsonPath().getInt("data.business_id"), 713, "Business ID does not match");

        // getting the policy details
        logger.info("Get the policy details");
        Response policyDetailsResponse = pageObj.endpoints().getPolicyDetails(token, policyId);
        Assert.assertEquals(policyDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for policy list API");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.name").toString(), policyName, "Policy Name do not matches");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.description").toString(), policyName + " Description", "Policy Description does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().getInt("data.pos_type_id"), 1, "POS Type ID does not match");
        Assert.assertEquals(policyDetailsResponse.jsonPath().get("data.status").toString(), "published", "Status does not match");
        Map<String, Object> policyConfig = policyDetailsResponse.jsonPath().getMap("data.config.pos_config");
        Map<String, Object> policyCommonConfig = policyDetailsResponse.jsonPath().getMap("data.config.common_config");

        // getting the duplicate policy's details
        logger.info("Get the duplicate policy's details");
        Response duplicatePolicyDetailsResponse = pageObj.endpoints().getPolicyDetails(token, duplicatePolicyId);
        Assert.assertEquals(duplicatePolicyDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for policy list API");
        Assert.assertEquals(duplicatePolicyDetailsResponse.jsonPath().get("data.name").toString(), duplicatePolicyName, "Duplicate Policy Name do not matches");
        Assert.assertEquals(duplicatePolicyDetailsResponse.jsonPath().get("data.description").toString(), duplicatePolicyName + " Description", "Duplicte Policy Description does not match");
        Assert.assertEquals(duplicatePolicyDetailsResponse.jsonPath().getInt("data.pos_type_id"), 1, "Duplicte Policy POS Type ID does not match");
        Assert.assertEquals(duplicatePolicyDetailsResponse.jsonPath().get("data.status").toString(), "published", "Duplicte Policy Status does not match");
        Map<String, Object> duplictePolicyConfig = duplicatePolicyDetailsResponse.jsonPath().getMap("data.config.pos_config");
        Map<String, Object> duplictePolicyCommonConfig = duplicatePolicyDetailsResponse.jsonPath().getMap("data.config.common_config");

        Assert.assertEquals(duplictePolicyConfig, policyConfig, "POS configs does not match");
        Assert.assertEquals(duplictePolicyCommonConfig, policyCommonConfig, "Common configs does not match");
        TestListeners.extentTest.get().pass("Original and duplicate policy details are same and API is giving 200 status code");

        // deleting the policies
        pageObj.ppccUtilities().deletePolicy(policyId, token);
        pageObj.ppccUtilities().deletePolicy(duplicatePolicyId, token);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        pageObj.utils().clearDataSet(dataSet);
        driver.quit();
        logger.info("Browser closed");
    }
}
 