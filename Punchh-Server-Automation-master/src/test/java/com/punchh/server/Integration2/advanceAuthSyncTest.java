package com.punchh.server.Integration2;

import java.lang.reflect.Method;
import java.util.ArrayList;
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

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.MessagesConstants;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class advanceAuthSyncTest {
    static Logger logger = LogManager.getLogger(advanceAuthSyncTest.class);
    public WebDriver driver;
    private PageObj pageObj;
    private String sTCName;
    private String env, run = "ui";
    private String baseUrl;
    private static Map<String, String> dataSet;
    private Utilities utils;
    private IntUtils intUtils;
    private String client;
    private String guestIdentityhost = "guestIdentity";

    @BeforeMethod(alwaysRun = true)
    public void setUp(Method method) {
        driver = new BrowserUtilities().launchBrowser();
        sTCName = method.getName();
        pageObj = new PageObj(driver);
        env = pageObj.getEnvDetails().setEnv();
        baseUrl = pageObj.getEnvDetails().setBaseUrl();
        utils = new Utilities(driver);
        intUtils = new IntUtils(driver);
        dataSet = new ConcurrentHashMap<>();
        pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
        dataSet = pageObj.readData().readTestData;
        pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
                pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
        dataSet.putAll(pageObj.readData().readTestData);
        logger.info(sTCName + " ==>" + dataSet);
        client = dataSet.get("client");
    }

    @Test(description = "SQ-T6770 Verify the business reset flow on Guest Auth DB.")
    @Owner(name = "Vansham Mishra")
    public void T6770_verifyBusinessResetFlowOnGuestAuthDB() throws Exception {
        int userCount = 5;
        List<String> communicationChannels = new ArrayList<>();
        List<String> userIds = new ArrayList<>();
        List<String> identityUserIds = new ArrayList<>();

        // Step 1: Signup 5 users and do check-in for each
        for (int i = 0; i < userCount; i++) {
            String communicationChannel = intUtils.getRandomGmailEmail();
            communicationChannels.add(communicationChannel);
            intUtils.userSignUpSignInAdvanceAuth(client, communicationChannel, "internal_mobile_app", "SignUp");
            String userIdQuery = "select id from users where email = '${communicationChannel}'".replace("${communicationChannel}", communicationChannel);
            String userID = DBUtils.executeQueryAndGetColumnValue(env, userIdQuery, "id");
            userIds.add(userID);

            String identityUserIdQuery = "select id from users where email = '${communicationChannel}'".replace("${communicationChannel}", communicationChannel);
            String identityUserID = DBUtils.executeQueryAndGetColumnValue(env, guestIdentityhost, identityUserIdQuery, "id");
            identityUserIds.add(identityUserID);

            String key = CreateDateTime.getRandomNumberSixDigit() + CreateDateTime.getTimeDateString();
            String txn = CreateDateTime.getRandomNumberSixDigit() + CreateDateTime.getTimeDateString();
            String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
            Response resp = pageObj.endpoints().posCheckin(date, communicationChannel, key, txn, dataSet.get("locationKey"));
            Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos chekin api");
            utils.logPass("Status code 200 matched for pos chekin api for user " + identityUserID);

            // Validate data creation before reset
            String getAuthRequestQuery = "select count(*) as count from auth_requests where user_id='${UserId_identity}'"
                    .replace("${UserId_identity}", identityUserID);
            String authRequestCountBefore = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, guestIdentityhost, getAuthRequestQuery, "count",20);
            Assert.assertNotEquals(authRequestCountBefore, "0", "Auth Requests data not created in guest_identity.auth_requests table for user " + identityUserID);

            String getAuditLogsQuery = "select count(*) as count from audit_logs where user_id='${UserId_identity}'"
                    .replace("${UserId_identity}", identityUserID);
            String auditLogsCountBefore = DBUtils.executeQueryAndGetColumnValue(env, guestIdentityhost, getAuditLogsQuery, "count");
            Assert.assertNotEquals(auditLogsCountBefore, "0", "Audit logs data not created in guest_identity.audit_logs table for user " + identityUserID);

            String getUserDetailsQuery = "select count(*) as count from user_details where user_id='${UserId_identity}'"
                    .replace("${UserId_identity}", identityUserID);
            String userDetailsCountBefore = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, guestIdentityhost, getUserDetailsQuery, "count",20);
            Assert.assertNotEquals(userDetailsCountBefore, "0", "User details data not created in guest_identity.user_details table for user " + identityUserID);
        }

        // Step 2: Reset the business
        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        pageObj.instanceDashboardPage().loginToInstance();
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
        pageObj.dashboardpage().clickOnResetDeleteBusinessButton();
        pageObj.dashboardpage().enterSlugNameAndClickOnResetButton(dataSet.get("slug"));
        utils.logPass("Business reset successfully done");
        utils.longWaitInSeconds(15);
        // Step 3: DB verifications for all users
        for (int i = 0; i < userCount; i++) {
            String getAuthRequestQuery = "select count(*) as count from auth_requests where user_id='${UserId_identity}'"
                    .replace("${UserId_identity}", identityUserIds.get(i));
            String authRequestCount = DBUtils.executeQueryAndGetColumnValue(env, guestIdentityhost, getAuthRequestQuery, "count");
            Assert.assertEquals(authRequestCount, "0", "Auth Requests data is not deleted from the guest_identity.auth_requests table for user " + identityUserIds.get(i));

            String getUsersQuery = "select count(*) as count from users where email='${communicationChannel}'"
                    .replace("${communicationChannel}", communicationChannels.get(i));
            String usersCount = DBUtils.executeQueryAndGetColumnValue(env, guestIdentityhost, getUsersQuery, "count");
            Assert.assertEquals(usersCount, "0", "User data is not deleted from the users table for user " + identityUserIds.get(i));

            String getUserDetailsQuery = "select count(*) as count from user_details where user_id='${UserId_identity}'"
                    .replace("${UserId_identity}", identityUserIds.get(i));
            String userDetailsCount = DBUtils.executeQueryAndGetColumnValue(env, guestIdentityhost, getUserDetailsQuery, "count");
            Assert.assertEquals(userDetailsCount, "0", "User data is not deleted from the guest_identity.user_details table for user " + identityUserIds.get(i));

            String getAuditLogsQuery = "select count(*) as count from audit_logs where user_id='${UserId_identity}'"
                    .replace("${UserId_identity}", identityUserIds.get(i));
            String auditLogsCount = DBUtils.executeQueryAndGetColumnValue(env, guestIdentityhost, getAuditLogsQuery, "count");
            Assert.assertEquals(auditLogsCount, "0", "User data is not deleted from the guest_identity.audit_logs table for user " + identityUserIds.get(i));

            String userIdPunchhQuery = "select count(*) as count from users where email = '${communicationChannel}'"
                    .replace("${communicationChannel}", communicationChannels.get(i));
            String userIDPunchh = DBUtils.executeQueryAndGetColumnValue(env, userIdPunchhQuery, "count");
            Assert.assertEquals(userIDPunchh, "0", "User data is not deleted from the punchh_production.users table for user " + identityUserIds.get(i));
        }
        utils.logPass("All verifications passed for 5 users after business reset");
    }

    @Test(description = "SQ-T6766 Verify updation of a existing OAuth application with the scope advanced_auth On the Guest Auth DB.")
    @Owner(name = "Vansham Mishra")
    public void T6766_verifyOAuthApplicationScopeUpdateOnGuestAuthDB() throws Exception {
        String slug = dataSet.get("slug");
        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        pageObj.instanceDashboardPage().loginToInstance();
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Whitelabel", "OAuth Apps");

        // Create a new oauth application
        String oauthAppName = "AutoTest Oauth App " + CreateDateTime.getTimeDateAsneed("yyyyMMddHHmmss");
        pageObj.oAuthAppPage().clickNewApplication();
        pageObj.oAuthAppPage().enterAppName(oauthAppName);
        pageObj.oAuthAppPage().enterRedirectUri(baseUrl);
        pageObj.oAuthAppPage().selectApplicationType("Mobile App - iOS");
        pageObj.oAuthAppPage().selectScope("Mobile Api");
        pageObj.oAuthAppPage().clickSave();
        pageObj.oAuthAppPage().verifyDisplayedMessage(MessagesConstants.oauthAppCreateSuccessMsg);

        // No OAuth app should be created on the Guest identity
        String getOAuthAppQuery = "select count(*) as count from oauth_applications where name='${oauthAppName}'";
        getOAuthAppQuery = getOAuthAppQuery.replace("${oauthAppName}", oauthAppName);
        pageObj.singletonDBUtilsObj();
        String oauthAppCount = DBUtils.executeQueryAndGetColumnValue(env, guestIdentityhost, getOAuthAppQuery, "count");
        Assert.assertEquals(oauthAppCount, "0", "OAuth application is created on the Guest identity");
        utils.logPass("OAuth application is not created on the Guest identity");

        // step 2 - Change scope of oauth_app to advanced_auth and Business unit par_Loyalty .
        pageObj.oAuthAppPage().selectScope("Advance Auth");
        pageObj.oAuthAppPage().selectBusinessUnit("PAR Loyalty");
        pageObj.oAuthAppPage().clickSave();
        pageObj.oAuthAppPage().verifyDisplayedMessage(MessagesConstants.oauthAppUpdateSuccessMsg);
        String uiClientId = pageObj.oAuthAppPage().getCurrentClient();
        String uiSecretKey = pageObj.oAuthAppPage().getCurrentSecret();

        String ownerId = "SELECT bm.id FROM business_mappings bm JOIN business_unit_stacks bus ON bm.business_unit_stack_id = bus.id\n" +
                "JOIN business_units bu\n" +
                "    ON bus.business_unit_id = bu.id\n" +
                "WHERE bm.slug = '"+slug+"'\n" +
                "  AND bu.name = 'par_loyalty';";
        String businessMappingId = DBUtils.executeQueryAndGetColumnValue(env, guestIdentityhost, ownerId, "id");
        Assert.assertNotNull(businessMappingId, "Business mapping id is null");
        utils.logPass("Business mapping id is: " + businessMappingId);

        String getOAuthAppDetailsQuery = "SELECT uid, secret, redirect_uri, owner_id, owner_type, scopes, punchh_oauth_app_id, is_active FROM oauth_applications WHERE name='${oauthAppName}'";
        getOAuthAppDetailsQuery = getOAuthAppDetailsQuery.replace("${oauthAppName}", oauthAppName);
        String[] oauthApplicationsColumnNames = { "uid", "secret", "redirect_uri", "owner_id", "owner_type", "scopes", "punchh_oauth_app_id", "is_active" };
        List<Map<String, String>> appDetails = DBUtils.executeQueryAndGetMultipleColumns(env, guestIdentityhost, getOAuthAppDetailsQuery, oauthApplicationsColumnNames);

        Assert.assertEquals(appDetails.get(0).get("uid"), uiClientId, "Client id mismatch");
        Assert.assertEquals(appDetails.get(0).get("secret"), uiSecretKey, "Secret key mismatch");
        Assert.assertEquals(appDetails.get(0).get("redirect_uri"), baseUrl, "Validation url mismatch");
        Assert.assertEquals(appDetails.get(0).get("owner_id"), businessMappingId, "Business mapping id mismatch");
        Assert.assertEquals(appDetails.get(0).get("owner_type"), "BusinessMapping", "Owner type mismatch");
        Assert.assertEquals(appDetails.get(0).get("scopes"), "advance_auth", "Scope mismatch");
        Assert.assertNotNull(appDetails.get(0).get("punchh_oauth_app_id"), "Punchh Oauth app id is null");
        Assert.assertEquals(appDetails.get(0).get("is_active"), "1", "Status is not active");
        utils.logPass("All OAuth application details verified in a single query");


        // Change scope of oauth_app to advanced_auth and Business unit par_ordering.
        pageObj.oAuthAppPage().selectBusinessUnit("PAR Ordering");
        pageObj.oAuthAppPage().clickSave();
        pageObj.oAuthAppPage().verifyDisplayedMessage(MessagesConstants.oauthAppUpdateSuccessMsg);
        utils.logPass("Scope of oauth_app has been changed to advanced_auth and Business unit par_ordering");
        ownerId = "SELECT bm.id FROM business_mappings bm JOIN business_unit_stacks bus ON bm.business_unit_stack_id = bus.id\n" +
                "JOIN business_units bu\n" +
                "    ON bus.business_unit_id = bu.id\n" +
                "WHERE bm.slug = '"+slug+"'\n" +
                "  AND bu.name = 'par_ordering';";
        businessMappingId = DBUtils.executeQueryAndGetColumnValue(env, guestIdentityhost, ownerId, "id");
        Assert.assertNotNull(businessMappingId, "Business mapping id is null");
        utils.logPass("Business mapping id is: " + businessMappingId);
        appDetails = DBUtils.executeQueryAndGetMultipleColumns(env, guestIdentityhost, getOAuthAppDetailsQuery, oauthApplicationsColumnNames);
        Assert.assertEquals(appDetails.get(0).get("scopes"), "advance_auth", "Scope mismatch");
        Assert.assertEquals(appDetails.get(0).get("owner_id"), businessMappingId, "Business mapping id mismatch");
        utils.logPass("Verified that after changing the business unit to par_ordering, scope is still advanced_auth");
        pageObj.menupage().navigateToSubMenuItem("Whitelabel", "OAuth Apps");
        pageObj.oAuthAppPage().deleteOauthAppByName(oauthAppName);
        pageObj.oAuthAppPage().verifyDisplayedMessage(MessagesConstants.oauthAppDeleteSuccessMsg);
        utils.logPass("Created OAuthApp deleted");
    }

    @Test(description = "SQ-T6765 Verify sync for creation, scope and deletion of a new OAuth application with the scope advanced_auth on Guest Auth DB.")
    @Owner(name = "Vansham Mishra")
    public void T6765_verifyOAuthApplicationCreationScopeSyncAndDeletionOnGuestAuthDB() throws Exception {
        String slug = dataSet.get("slug");
        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        pageObj.instanceDashboardPage().loginToInstance();
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Whitelabel", "OAuth Apps");

        // Create a new oauth application
        String oauthAppName = "AutoTest Oauth App " + CreateDateTime.getTimeDateAsneed("yyyyMMddHHmmss");
        pageObj.oAuthAppPage().clickNewApplication();
        pageObj.oAuthAppPage().enterAppName(oauthAppName);
        pageObj.oAuthAppPage().enterRedirectUri(baseUrl);
        pageObj.oAuthAppPage().selectApplicationType("Mobile App - iOS");
        pageObj.oAuthAppPage().selectScope("Advance Auth");
        pageObj.oAuthAppPage().selectBusinessUnit("PAR Loyalty");
        pageObj.oAuthAppPage().clickSave();
        pageObj.oAuthAppPage().verifyDisplayedMessage(MessagesConstants.oauthAppCreateSuccessMsg);
        String uiClientId = pageObj.oAuthAppPage().getCurrentClient();
        String uiSecretKey = pageObj.oAuthAppPage().getCurrentSecret();
        String ownerId = "SELECT bm.id FROM business_mappings bm JOIN business_unit_stacks bus ON bm.business_unit_stack_id = bus.id\n" +
                "JOIN business_units bu\n" +
                "    ON bus.business_unit_id = bu.id\n" +
                "WHERE bm.slug = '"+slug+"'\n" +
                "  AND bu.name = 'par_loyalty';";
        String businessMappingId = DBUtils.executeQueryAndGetColumnValue(env, guestIdentityhost, ownerId, "id");
        Assert.assertNotNull(businessMappingId, "Business mapping id is null");
        utils.logPass("Business mapping id is: " + businessMappingId);
        String getOAuthAppDetailsQuery = "SELECT uid, secret, redirect_uri, owner_id, owner_type, scopes, punchh_oauth_app_id, is_active FROM oauth_applications WHERE name='${oauthAppName}'";
        getOAuthAppDetailsQuery = getOAuthAppDetailsQuery.replace("${oauthAppName}", oauthAppName);
        String[] oauthApplicationsColumnNames = { "uid", "secret", "redirect_uri", "owner_id", "owner_type", "scopes", "punchh_oauth_app_id", "is_active" };
        List<Map<String, String>> appDetails = DBUtils.executeQueryAndGetMultipleColumns(env, guestIdentityhost, getOAuthAppDetailsQuery, oauthApplicationsColumnNames);

        Assert.assertEquals(appDetails.get(0).get("uid"), uiClientId, "Client id mismatch");
        Assert.assertEquals(appDetails.get(0).get("secret"), uiSecretKey, "Secret key mismatch");
        Assert.assertEquals(appDetails.get(0).get("redirect_uri"), baseUrl, "Validation url mismatch");
        Assert.assertEquals(appDetails.get(0).get("owner_id"), businessMappingId, "Business mapping id mismatch");
        Assert.assertEquals(appDetails.get(0).get("owner_type"), "BusinessMapping", "Owner type mismatch");
        Assert.assertEquals(appDetails.get(0).get("scopes"), "advance_auth", "Scope mismatch");
        Assert.assertNotNull(appDetails.get(0).get("punchh_oauth_app_id"), "Punchh Oauth app id is null");
        Assert.assertEquals(appDetails.get(0).get("is_active"), "1", "Status is not active");
        utils.logPass("All OAuth application details verified in a single query");

        // Hit brand level token api
        Response generateBrandLevelTokenResponse = pageObj.endpoints()
                .identityGenerateBrandLevelToken(uiClientId, uiSecretKey);
        Assert.assertEquals(generateBrandLevelTokenResponse.getStatusCode(), 412,
                "Incorrect status code for valid IP");
        String errorMessage = generateBrandLevelTokenResponse.jsonPath().getString("errors.invalid_client[0]");
        Assert.assertEquals(errorMessage, "Invalid Client / Client not found.",
                "Identity API with new created Oauth App ran successfully, not expected");
        utils.logPass("Identity API with new created Oauth App did not ran successfully as expected");

        String communicationChannel = intUtils.getRandomGmailEmail();
        
        // Step 1 - AdvanceAuth Signup
        String[] tokensSignUp = intUtils.userSignUpSignInAdvanceAuth(uiClientId, communicationChannel, "internal_mobile_app", "SignUp");
        logger.info(tokensSignUp);
        utils.logPass("Verified that advance_auth api for the new created app is working fine");


        // Step 2 - Change scope of oauth_app to advanced_auth and Business unit par_Loyalty .
        pageObj.oAuthAppPage().selectScope("Auth Service");
        pageObj.oAuthAppPage().enterValidationURI("https://punchh.com");
        pageObj.oAuthAppPage().enterVerificationKey("Test");
        pageObj.oAuthAppPage().clickSave();
        pageObj.oAuthAppPage().verifyDisplayedMessage(MessagesConstants.oauthAppUpdateSuccessMsg);
        uiClientId = pageObj.oAuthAppPage().getCurrentClient();
        uiSecretKey = pageObj.oAuthAppPage().getCurrentSecret();
        appDetails = DBUtils.executeQueryAndGetMultipleColumns(env, guestIdentityhost, getOAuthAppDetailsQuery, oauthApplicationsColumnNames);
        Assert.assertEquals(appDetails.get(0).get("scopes"), "auth_service", "Client id mismatch");
        
        // Hit brand level token api
        Response generateBrandLevelTokenResponse2 = pageObj.endpoints()
                .identityGenerateBrandLevelToken(uiClientId, uiSecretKey);
        Assert.assertEquals(generateBrandLevelTokenResponse2.getStatusCode(), 200,
                "Incorrect status code for valid IP");
        String errorMessage2 = generateBrandLevelTokenResponse2.jsonPath().getString("status");
        Assert.assertEquals(errorMessage2, "created",
                "Identity API with new created Oauth App ran successfully, not expected");
        utils.logPass("Identity API with new created Oauth App ran successfully as expected");

        String codeVerifier = utils.generateCodeVerifier(32);
        String codeChallenge = utils.generateCodeChallenge(codeVerifier);
        boolean privacyPolicy = true;
        boolean tAndc = true;
        String communicationChannel2 = intUtils.getRandomGmailEmail();
        Response responseToken = pageObj.endpoints().advancedAuthToken(uiClientId, "otp", communicationChannel2, null, null, codeChallenge, privacyPolicy, tAndc);
        Assert.assertEquals(responseToken.getStatusCode(), 412, "Status code 412 did not matched for advanced auth token api");
        String errMsg = responseToken.jsonPath().getString("errors.invalid_client[0]");
        Assert.assertEquals(errMsg, "Invalid Client / Client not found.",
                "Advance auth API with new created Oauth App ran successfully, not expected");
        utils.logPass("Advance auth API with new created Oauth App did not ran successfully as expected");


        pageObj.menupage().navigateToSubMenuItem("Whitelabel", "OAuth Apps");
        pageObj.oAuthAppPage().deleteOauthAppByName(oauthAppName);
        pageObj.oAuthAppPage().verifyDisplayedMessage(MessagesConstants.oauthAppDeleteSuccessMsg);
        appDetails = DBUtils.executeQueryAndGetMultipleColumns(env, guestIdentityhost, getOAuthAppDetailsQuery, oauthApplicationsColumnNames);
        Assert.assertEquals(appDetails.get(0).get("is_active"), "0","OAuth application is not deleted from guest identity db");
        utils.logPass("Created OAuthApp deleted");
        String communicationChannel3 = intUtils.getRandomGmailEmail();
        Response responseToken2 = pageObj.endpoints().advancedAuthToken(uiClientId, "otp", communicationChannel3, null, null, codeChallenge, privacyPolicy, tAndc);
        Assert.assertEquals(responseToken2.getStatusCode(), 412, "Status code 412 did not matched for advanced auth token api");
        String errMsg2 = responseToken2.jsonPath().getString("errors.invalid_client[0]");
        Assert.assertEquals(errMsg2, "Invalid Client / Client not found.",
                "Advance auth API after deleting the created Oauth App ran successfully, not expected");
        utils.logPass("Advance auth API after deleting new created Oauth App did not ran successfully as expected");

        generateBrandLevelTokenResponse = pageObj.endpoints()
                .identityGenerateBrandLevelToken(uiClientId, uiSecretKey);
        Assert.assertEquals(generateBrandLevelTokenResponse.getStatusCode(), 412,
                "Incorrect status code for valid IP");
        errorMessage = generateBrandLevelTokenResponse.jsonPath().getString("errors.invalid_client[0]");
        Assert.assertEquals(errorMessage, "Invalid Client / Client not found.",
                "Identity API after deleting new created Oauth App ran successfully, not expected");
        utils.logPass("Identity API after deleting new created Oauth App did not ran successfully as expected");

    }
    
    @Test(description = "SQ-T6748 AdvancedAuth | Sync guest deactivation and deletion from Punchh to Guest Identity.")
    @Owner(name = "Vansham Mishra")
    public void T6748_syncGuestDeactivationAndDeletionFromPunchhToGuestIdentity() throws Exception {
	pageObj.sidekiqPage().sidekiqCheck(baseUrl);
        String communicationChannel = intUtils.getRandomGmailEmail();
        
        // Step 1 -AdvanceAuth Signup
        intUtils.userSignUpSignInAdvanceAuth(client, communicationChannel, "internal_mobile_app", "SignUp");

        // fetch userID from users table where email = communicationChannel
        String userIdQuery = "select id,identity_uuid from users where email = '${communicationChannel}'";
        userIdQuery = userIdQuery.replace("${communicationChannel}", communicationChannel);
        String[] userColumnNames = { "id", "identity_uuid" };
        List<Map<String, String>> appDetails = DBUtils.executeQueryAndGetMultipleColumns(env, userIdQuery, userColumnNames);
        String punchhUserID = appDetails.get(0).get("id");
        utils.logit("UserID: " + punchhUserID);

        // Deactivate user
        Response deacResponse = pageObj.endpoints().deactivateUser(punchhUserID, dataSet.get("adminKey"));
        Assert.assertEquals(deacResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not matched for deactivate user API");
        utils.logit("Pass", "User Deactivate API executed successfully");
        intUtils.validateUserSyncWithGIS(punchhUserID);
        utils.logit("Pass", "Verified that user deactivation is synced with identity");

        // delete the user
        Response response = pageObj.endpoints().deleteUser(punchhUserID, dataSet.get("adminKey"));
        Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for Delete user API");
        utils.logit("Pass", "User Delete API executed successfully");
        
        // Trigger Sidekiq job
        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.sidekiqPage().navigateToSidekiqScheduled(baseUrl);
        pageObj.sidekiqPage().filterByJob(punchhUserID);
        pageObj.sidekiqPage().checkSelectAllJobsCheckBox();
        pageObj.sidekiqPage().clickAddToQueue();
        utils.logit("Job is added in the queue");

        // verify that user should be delete from punchh users table
        String userDeleteQuery = "select count(*) as count from users where id = '${punchhUserID}'";
        userDeleteQuery = userDeleteQuery.replace("${punchhUserID}", punchhUserID);
        String userCount = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, userDeleteQuery, "count", 20);
        Assert.assertEquals(userCount, "0", "User is not deleted from punchh users table");
        utils.logit("Pass", "Verified that user is deleted from punchh users table");

        // verify that user should be delete from identity users table
        String userIdentityDeleteQuery = "select count(*) as count from users where email = '${communicationChannel}'";
        userIdentityDeleteQuery = userIdentityDeleteQuery.replace("${communicationChannel}", communicationChannel);
        String userIdentityCount = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, guestIdentityhost,
                        userIdentityDeleteQuery, "count", 20);
        Assert.assertEquals(userIdentityCount, "0", "User is not deleted from identity users table");
        utils.logit("Pass", "Verified that user is deleted from identity users table");
}
        
    @Test(description = "SQ-T6747 AdvancedAuth | Sync guest anonymisation from Punchh to Guest Identity")
    @Owner(name = "Vansham Mishra")
    public void T6747_syncGuestAnonymisationFromPunchhToGuestIdentity() throws Exception {
	pageObj.sidekiqPage().sidekiqCheck(baseUrl);
        String communicationChannel = intUtils.getRandomGmailEmail();
        
        // Step 1 - AdvanceAuth Signup
        intUtils.userSignUpSignInAdvanceAuth(client, communicationChannel, "internal_mobile_app", "SignUp");

        // fetch userID from users table where email = communicationChannel
        String userIdQuery = "select id from users where email = '${communicationChannel}'";
        userIdQuery = userIdQuery.replace("${communicationChannel}", communicationChannel);
        String punchhUserID = DBUtils.executeQueryAndGetColumnValue(env, userIdQuery, "id");
        utils.logit("UserID: " + punchhUserID);

        // get user id from identity users table where email = communicationChannel
        String identityUserIdQuery = "select id from users where email = '${communicationChannel}'";
        identityUserIdQuery = identityUserIdQuery.replace("${communicationChannel}", communicationChannel);
        String identityUserID = DBUtils.executeQueryAndGetColumnValue(env,guestIdentityhost, identityUserIdQuery, "id");
        utils.logit("Identity UserID: " + identityUserID);
       
        //Deactivate user
        Response deacResponse = pageObj.endpoints().deactivateUser(punchhUserID, dataSet.get("adminKey"));
        Assert.assertEquals(deacResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not matched for deactivate user API");
        utils.logit("Pass", "User Deactivate API executed successfully");
        intUtils.validateUserSyncWithGIS(punchhUserID);
        utils.logit("Pass", "Verified that user deactivation is synced with identity");

        // anonymise the user
        Response response = pageObj.endpoints().anonymiseUser(punchhUserID, dataSet.get("adminKey"));
        Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for Delete user API");
        utils.logit("Pass", "User Delete API executed successfully");
        
        // Trigger Sidekiq job
        pageObj.sidekiqPage().navigateToSidekiqScheduled(baseUrl);
        pageObj.sidekiqPage().filterByJob(punchhUserID);
        pageObj.sidekiqPage().checkSelectAllJobsCheckBox();
        pageObj.sidekiqPage().clickAddToQueue();
        utils.logit("Job is added in the queue");

        // verify that user should be anonymise from punchh users table
        String userAnonymiseQuery = "select count(*) as count from users where id = '${punchhUserID}' and email like '%@archived.com'";
        userAnonymiseQuery = userAnonymiseQuery.replace("${punchhUserID}", punchhUserID);
        String userAnonymiseCount = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, userAnonymiseQuery, "count",20);
        Assert.assertEquals(userAnonymiseCount, "1", "User is not anonymised from punchh users table");
        
        // verify that user should be anonymise from identity users table
        intUtils.validateUserSyncWithGIS(punchhUserID);
        utils.logit("Pass", "Verified that user anonymisation is synced with identity");
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        pageObj.utils().clearDataSet(dataSet);
        driver.quit();
        logger.info("Browser closed");
    }
}
