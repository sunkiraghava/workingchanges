package com.punchh.server.Integration2;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.*;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.*;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

@Listeners(TestListeners.class)
public class FacebookSocialLoginTest {
    static Logger logger = LogManager.getLogger(FacebookSocialLoginTest.class);
    public WebDriver driver;
    private String userEmail;
    private ApiUtils apiUtils;
    private Properties prop;
    private PageObj pageObj;
    private String sTCName;
    private String env, run = "ui";
    private String baseUrl;
    private static Map<String, String> dataSet;
    private Utilities utils;


    @BeforeMethod
    public void setUp(Method method) {
        prop = Utilities.loadPropertiesFile("config.properties");
        driver = new BrowserUtilities().launchBrowser();
        sTCName = method.getName();
        pageObj = new PageObj(driver);
        env = pageObj.getEnvDetails().setEnv();
        baseUrl = pageObj.getEnvDetails().setBaseUrl();
        dataSet = new ConcurrentHashMap<>();
        pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
        dataSet = pageObj.readData().readTestData;
        pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
                pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
        dataSet.putAll(pageObj.readData().readTestData);
        logger.info(sTCName + " ==>" + dataSet);
        utils = new Utilities(driver);
    }
    @Test(description = "SQ-T7175 Verify the facebook sign up/Sign in /api2/mobile/users/connect_with_facebook(POST)", groups = "Regression")
    @Owner(name = "Vansham Mishra")
    public void facebookSignInApiTest() throws Exception {
//        String facebookToken = generateFacebookAccessToken();
        String facebookToken = dataSet.get("facebookAccessToken");
        // API v1 Facebook sign up/sign in
        String[] signUpResult = performFacebookSignUpV1(facebookToken, "signup");
        String email = signUpResult[0];
        String authToken = signUpResult[1];
        String punchhUserId = fetchPunchhUserId(email);
        // 9. Verify facebook  signed up/signed in api2 user update by all resource  APIs.
        updateUserInfoAndValidate(authToken, email, dataSet.get("UpdatedFName"), dataSet.get("UpdatedLName"));
        performFacebookSignUpV1(facebookToken, "signin");
        deactivateUser(punchhUserId);
        verifyDeactivatedUserCannotSignInV1(facebookToken);
        deleteUserFromDb(email);

        // API v2 Facebook sign up/sign in
        signUpResult = performFacebookSignUpV2(facebookToken,"signup");
        email = signUpResult[0];
        String accessToken = signUpResult[1];
        punchhUserId = fetchPunchhUserId(email);
        // 10. Verify facebook  signed up/signed in api1 user update by all resource APIs.
        updateUserInfoV1AndValidate(dataSet.get("client"), dataSet.get("secret"), accessToken, email, dataSet.get("phoneNumber"), dataSet.get("UpdatedFName"), dataSet.get("UpdatedLName"));
        performFacebookSignUpV2(facebookToken,"signin");
        deactivateUser(punchhUserId);
        verifyDeactivatedUserCannotSignInV2(facebookToken);
        deleteUserFromDb(email);

        // API v2 Auth Facebook social login
        signUpResult = performFacebookAuthSocialLogin(facebookToken,"signup");
        email = signUpResult[0];
        authToken = signUpResult[1];
        punchhUserId = fetchPunchhUserId(email);
        // 11. Verify facebook signed up/signed in api/auth user update by all resource APIs.
        updateUserInfoAndValidate(authToken, email, dataSet.get("UpdatedFName"), dataSet.get("UpdatedLName"));
        performFacebookAuthSocialLogin(facebookToken,"signin");
        deactivateUser(punchhUserId);
        verifyDeactivatedUserCannotSignInAuth(facebookToken);
        deleteUserFromDb(email);
    }

    public String generateFacebookAccessToken() throws Exception {
        LoginToFacebook(dataSet.get("facebookEmailId"), utils.decrypt(prop.getProperty("password")), dataSet.get("developerPageUrl"));
        WebElement accessToken = utils.getLocator("facebookPage.accessTokenField");
        Assert.assertTrue(accessToken.isDisplayed(), "Access token field not displayed");
        String token = accessToken.getAttribute("value");
        Assert.assertNotNull(token, "Facebook access token generation failed: token is null");
        logger.info("Access Token generated: " + token);
        return token;
    }

    public String[] performFacebookSignUpV1(String token,String activity) throws Exception {
        Response response = pageObj.endpoints().api1facebookSignUp(
                dataSet.get("client"), token, dataSet.get("fbUid"),
                dataSet.get("secret"), dataSet.get("facebookEmailId"));
        Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Expected status code 200");
        String email = response.jsonPath().get("email").toString();
        String authToken = response.jsonPath().get("authentication_token").toString();
        if (activity.equalsIgnoreCase("signup")){
            Assert.assertTrue(response.jsonPath().get("facebook_signup"), "User was not signed up successfully using Facebook v1 API");
        }
        else {
            Assert.assertFalse(response.jsonPath().get("facebook_signup"), "User was not signed in successfully using Facebook v1 API");
        }

        punchhDbValidations(email, dataSet.get("business_id"), "api1");
        utils.logPass("User signed up successfully using Facebook v1 API with email: " + email);
        return new String[]{email, authToken};
    }

    public String[] performFacebookSignUpV2(String token, String activity) throws Exception {
        Response response = pageObj.endpoints().api2facebookSignUp(
                dataSet.get("client"), token, dataSet.get("secret"));
        Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Expected status code 200");
        String email = response.jsonPath().get("user.email").toString();
        String authToken = response.jsonPath().get("access_token.token").toString();
        if (activity.equalsIgnoreCase("signup")){
            Assert.assertTrue(response.jsonPath().get("user.facebook_signup"), "User was not signed up successfully using Facebook v2 API");
        }
        else {
            Assert.assertNull(response.jsonPath().get("user.facebook_signup"), "User was not signed in successfully using Facebook v2 API");
        }
        punchhDbValidations(email, dataSet.get("business_id"), "api2");
        utils.logPass("User signed up successfully using Facebook v2 API with email: " + email);
        return new String[]{email, authToken};
    }
    public String[] performFacebookAuthSocialLogin(String token, String activity) throws Exception {
        Response response = pageObj.endpoints().ApiAuthFacebookSocialLogin(
                dataSet.get("client"), token, dataSet.get("secret"), dataSet.get("fbUid"));
        String email = response.jsonPath().get("email").toString();
        String authToken = response.jsonPath().get("authentication_token").toString();
        if (activity.equalsIgnoreCase("signup")) {
            Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Expected status code 201");
            Assert.assertTrue(response.jsonPath().get("facebook_signup"), "User was not signed up successfully using Facebook Auth API");
        } else {
            Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Expected status code 201");
            Assert.assertFalse(response.jsonPath().get("facebook_signup"), "User was not signed in successfully using Facebook Auth API");
        }
        punchhDbValidations(email, dataSet.get("business_id"), "auth");
        utils.logPass("User signed up successfully using Facebook Auth API with email: " + email);
        return new String[]{email, authToken};
    }

    public String fetchPunchhUserId(String email) throws Exception {
        String query = "SELECT id FROM users WHERE email = '" + email + "'";
        String userId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
        Assert.assertFalse(userId.isEmpty(), "Punchh user ID is empty");
        logger.info("Punchh user ID: " + userId);
        return userId;
    }

    public void deactivateUser(String userId) {
        Response response = pageObj.endpoints().deactivateUser(userId, dataSet.get("apiKey"));
        Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed to deactivate user");
    }

    public void verifyDeactivatedUserCannotSignInV1(String token) {
        Response response = pageObj.endpoints().api1facebookSignUp(
                dataSet.get("client"), token, dataSet.get("fbUid"),
                dataSet.get("secret"), dataSet.get("facebookEmailId"));
        Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "Expected status code 422");
        String errorMessage = response.jsonPath().getString("[0]");
        Assert.assertTrue(errorMessage.contains(MessagesConstants.deactivatedUser), "Error message does not contain 'This account has been deactivated'");
        utils.logPass("Deactivated user cannot sign in using Facebook API, as expected");
    }
    public void verifyDeactivatedUserCannotSignInV2(String token) {
        Response response = pageObj.endpoints().api2facebookSignUp(
                dataSet.get("client"), token, dataSet.get("secret"));
        Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "Expected status code 422");
        String errorMessage = response.jsonPath().getString("errors.base[0]");
        Assert.assertTrue(errorMessage.contains(MessagesConstants.deactivatedUser), "Error message does not contain 'This account has been deactivated'");
        utils.logPass("Deactivated user cannot sign in using Facebook API v2, as expected");
    }
    public void verifyDeactivatedUserCannotSignInAuth(String token) {
        Response response = pageObj.endpoints().ApiAuthFacebookSocialLogin(
                dataSet.get("client"), token, dataSet.get("secret"), dataSet.get("fbUid"));
        Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "Expected status code 422");
        String errorMessage = response.jsonPath().getString("[0]");
        Assert.assertTrue(errorMessage.contains(MessagesConstants.deactivatedUser), "Error message does not contain 'This account has been deactivated'");
        utils.logPass("Deactivated user cannot sign in using Facebook API auth, as expected");
    }
    public void LoginToFacebook(String emailId, String password,String url) throws Exception {
        driver.get(url);
        WebElement ele = utils.getLocator("facebookPage.facebookLoginButton");
        ele.isDisplayed();
        ele.click();
        WebElement email = utils.getLocator("facebookPage.facebookLoginEmailField");
        email.isDisplayed();
        email.sendKeys(emailId);
        WebElement pass = utils.getLocator("facebookPage.facebookLoginPasswordField");
        pass.isDisplayed();
        pass.sendKeys(password);
        WebElement loginBtn = utils.getLocator("facebookPage.loginButton");
        loginBtn.isDisplayed();
        loginBtn.click();
        utils.waitTillVisibilityOfElement(utils.getLocator("facebookPage.successSignInPage"), "Graph API Explorer");
        utils.logit("Navigated to Facebook Graph API Explorer");
    }
    public void punchhDbValidations(String email,String businessId,String apiNameSpace)
            throws Exception {
        String signup_channel="";
        if (apiNameSpace.equalsIgnoreCase("auth")){
            signup_channel = "WebFacebook";
        }
        else {
            signup_channel = "WebFacebook";
        }
        String query = "SELECT id,joined_at,created_at,updated_at from users where email = '" + email + "' and signup_channel = '"+signup_channel+"'";
        String[] cols = {"id","joined_at","created_at","updated_at"};
        List<Map<String, String>> res = DBUtils
                .executeQueryAndGetMultipleColumnsUsingPolling(env,query, cols,2,20);
        Map<String, String> row = res.get(0);
        String user_id = row.get("id");
        String joined_at = row.get("joined_at");
        String created_at = row.get("created_at");
        String updated_at = row.get("updated_at");
        Assert.assertNotNull(user_id, "User ID is null in users table");
        Assert.assertNotNull(joined_at, "joined_at is null in users table");
        Assert.assertNotNull(created_at, "created_at is null in users table");
        Assert.assertNotNull(updated_at, "updated_at is null in users table");


    }
    // Delete User from DB table
    public void deleteUserFromDb(String email) throws Exception {
        if (email != null && !email.isEmpty()) {
            String query1 = "DELETE FROM users WHERE email = '" + email + "'";
            DBUtils.executeUpdateQuery(env, query1);
            utils.logit("Deleted user with email: " + email + " from users table");
        }
    }

    private void updateUserInfoAndValidate(String authToken, String email, String updatedFName, String updatedLName) throws Exception {
        Response updateUserResponse = pageObj.endpoints().authApiUpdateUserInfoAndPassword(
                dataSet.get("client"),
                dataSet.get("secret"),
                authToken,
                email,
                updatedFName,
                updatedLName
        );
        Assert.assertEquals(updateUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not matched for auth Api Update userInfo with email");

        // Validate updated values in DB
        String query = String.format("SELECT first_name, last_name FROM users WHERE email = '%s'", email);
        String[] cols = {"first_name","last_name"};
        List<Map<String, String>> userRow = DBUtils
                .executeQueryAndGetMultipleColumnsUsingPolling(env,query, cols,2,20);
        Map<String, String> userData = userRow.get(0);
        Assert.assertEquals(userData.get("first_name"), updatedFName, "First name not updated in DB");
        Assert.assertEquals(userData.get("last_name"), updatedLName, "Last name not updated in DB");
        utils.logPass(String.format("User info updated and validated in DB for email: %s", email));
    }

    private void updateUserInfoV1AndValidate(String client, String secret, String accessToken, String email, String phone, String firstName, String lastName) throws Exception {
        Response updateUserResponse = pageObj.endpoints().api1UpdateUser(client, secret, accessToken, email, phone, firstName, lastName);
        Assert.assertEquals(updateUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not matched for v1 API Update userInfo");

        // Validate updated values in DB
        String query = String.format("SELECT email, phone, first_name, last_name FROM users WHERE email = '%s'", email);
        String[] cols = {"email", "phone", "first_name", "last_name"};
        List<Map<String, String>> userRow = DBUtils.executeQueryAndGetMultipleColumnsUsingPolling(env, query, cols, 2, 20);
        Map<String, String> userData = userRow.get(0);
        Assert.assertEquals(userData.get("email"), email, "Email not updated in DB");
        Assert.assertEquals(userData.get("phone"), phone, "Phone not updated in DB");
        Assert.assertEquals(userData.get("first_name"), firstName, "First name not updated in DB");
        Assert.assertEquals(userData.get("last_name"), lastName, "Last name not updated in DB");
        utils.logPass(String.format("User info updated and validated in DB for email: %s", email));
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        pageObj.utils().clearDataSet(dataSet);
        driver.quit();
        logger.info("Browser closed");
    }
}
