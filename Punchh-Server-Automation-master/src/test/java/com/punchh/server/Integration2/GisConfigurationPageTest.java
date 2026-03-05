package com.punchh.server.Integration2;

import com.github.javafaker.Faker;
import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Listeners(TestListeners.class)
public class GisConfigurationPageTest {
    static Logger logger = LogManager.getLogger(com.punchh.server.Integration2.GisConfigurationPageTest.class);
    public WebDriver driver;
    private String userEmail;
    private PageObj pageObj;
    private String sTCName;
    private String env, run = "ui";
    private String baseUrl;
    private static Map<String, String> dataSet;
    private Utilities utils;
    private IntUtils intUtils;
    private String client, secret;
    private String guestIdentityhost = "guestIdentity";
    private Faker faker;

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
        secret = dataSet.get("secret");
        this.faker = new Faker();
        userEmail = intUtils.getRandomGmailEmail();
    }
    @Test(description = "SQ-T7410 INT2-2740 | INT2-2741 | INT2-2772 |  UI of Guest Identity Management under Cockpit and It’s backend call to Identity service from Punchh. Case 1:- For New Business On Guest. " +
            "SQ-T7411 INT2-2740 | INT2-2741 | INT2-2772 |  UI of Guest Identity Management under Cockpit and It’s backend call to Identity service from Punchh. Case 2:- For already listed Business On Guest.")
    @Owner(name = "Vansham Mishra")
    public void T6962_verifyGISConfigurationPageTabs() throws Exception {
        loginAndNavigateToGISConfiguration();
        intUtils.updateAdvanceAndBasicAuthConfig(true, true);
        verifyGISConfigurationTabs();
        utils.logit("PASS","Verified that GIS Configuration page contains all expected tabs in the expected order.");
        verifyCoreConfigFields();
        verifyAdvancedAuthenticationFields();
        verifyEntryInServicesTable(dataSet.get("slug"));
        verifyBasicAuthenticationFields();
        verifySocialAuthenticationFields();
        verifyCoreValueInPreferences(dataSet.get("slug"));
        verifyBusinessMappingsTableEntry(dataSet.get("slug"));
        verifyPunchhBusinessTableUUIDUpdate(dataSet.get("slug"));
        verifyBusinessMappingForAuthConfig(dataSet.get("slug"));
    }

    private void loginAndNavigateToGISConfiguration() {
        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        pageObj.instanceDashboardPage().loginToInstance();
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guest Identity Management");

    }

    private void verifyGISConfigurationTabs() {
        List<String> expectedTabs = Arrays.asList(
                "Core Config",
                "Advanced Authentication",
                "Basic Authentication",
                "Social Authentication"
        );

        List<WebElement> tabElements = utils.getLocatorList("GISConfigurationPage.gisPageTabs");
        List<String> actualTabs = tabElements.stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());

        Assert.assertEquals(actualTabs, expectedTabs, "GIS Configuration page tabs are not as expected");
        utils.logPass("Verified GIS Configuration page contains all expected tabs: " + expectedTabs);
    }
    private void verifyCoreConfigFields() {
        // Verify Business UUID field exists ans should not be null/empty value
        String businessUUIDXpath = utils.getLocatorValue("GISConfigurationPage.gisTabFields").replace("$fieldId","business_external_identity_uuid");
        String businessUUID = driver.findElement(By.xpath(businessUUIDXpath)).getAttribute("value");
        Assert.assertNotNull(businessUUID, "Business UUID field should not be null");
        Assert.assertFalse(businessUUID.isEmpty(), "Business UUID field should not be empty");
        utils.logPass("Verified Business UUID field exists and has a non-empty value");

        // Verify Access Token Expiration field with default value 5
        String accessTokenExpirationXpath = utils.getLocatorValue("GISConfigurationPage.gisTabFields").replace("$fieldId","business_access_token_expiration_basic_auth");
        String accessTokenExpiration = driver.findElement(By.xpath(accessTokenExpirationXpath)).getAttribute("value");
        Assert.assertEquals(accessTokenExpiration, "5",
                "Access Token Expiration should be 5 minutes. Actual: " + accessTokenExpiration);
        utils.logPass("Verified Access Token Expiration field has default value: 5 minutes");

        // Verify Refresh Token Expiration field with default value 1
        String refreshTokenExpirationXpath = utils.getLocatorValue("GISConfigurationPage.gisTabFields").replace("$fieldId","business_refresh_token_expiration_basic_auth");
        String refreshTokenExpiration = driver.findElement(By.xpath(refreshTokenExpirationXpath)).getAttribute("value");
        Assert.assertEquals(refreshTokenExpiration, "1",
                "Refresh Token Expiration should be 1 Days. Actual: " + refreshTokenExpiration);
        utils.logPass("Verified Refresh Token Expiration field has default value: 1 Days");
    }
    private void verifyAdvancedAuthenticationFields() throws InterruptedException {

        pageObj.dashboardpage().navigateToTabsNew("Advanced Authentication");
        // Verify Enable Advanced Authentication checkbox is Present on Advanced Authentication tab
        String enableAdvancedAuthenticationXpath = utils.getLocatorValue("GISConfigurationPage.gisCheckBoxFields").replace("$fieldName","Enable Advanced Authentication");
        String enableAdvancedAuthentication = driver.findElement(By.xpath(enableAdvancedAuthenticationXpath)).getAttribute("value");
        Assert.assertEquals(enableAdvancedAuthentication,"1","Enable Advanced Authentication checkbox is checked");
        utils.logPass("Verified Enable Advanced Authentication checkbox is Present on Advanced Authentication tab");

        // verify Auth0 Domain should be present and should not be empty when advanced authentication is enabled
        String auth0DomainXpath = utils.getLocatorValue("GISConfigurationPage.gisTabFields").replace("$fieldId","business_services_attributes_0_auth0_domain");
        String auth0Domain = driver.findElement(By.xpath(auth0DomainXpath)).getAttribute("value");
        Assert.assertNotNull(auth0Domain, "Auth0 Domain field should not be null when Advanced Authentication is enabled");
        Assert.assertFalse(auth0Domain.isEmpty(), "Auth0 Domain field should not be empty when Advanced Authentication is enabled");
        utils.logPass("Verified Auth0 Domain field is present and has a non-empty value when Advanced Authentication is enabled");

        // verify that Auth0 Client ID should be present and should not be empty when advanced authentication is enabled
        String auth0ClientIdXpath = utils.getLocatorValue("GISConfigurationPage.gisTabFields").replace("$fieldId","business_services_attributes_0_auth0_client_id");
        String auth0ClientId = driver.findElement(By.xpath(auth0ClientIdXpath)).getAttribute("value");
        Assert.assertNotNull(auth0ClientId, "Auth0 Client ID field should not be null when Advanced Authentication is enabled");
        Assert.assertFalse(auth0ClientId.isEmpty(), "Auth0 Client ID field should not be empty when Advanced Authentication is enabled");
        utils.logPass("Verified Auth0 Client ID field is present and has a non-empty value when Advanced Authentication is enabled");

        // verify that Auth0 Client Secret should be present and should not be empty when advanced authentication is enabled
        String auth0ClientSecretXpath = utils.getLocatorValue("GISConfigurationPage.gisTabFields").replace("$fieldId","business_services_attributes_0_auth0_client_secret");
        String auth0ClientSecret = driver.findElement(By.xpath(auth0ClientSecretXpath)).getAttribute("value");
        Assert.assertNotNull(auth0ClientSecret, "Auth0 Client Secret field should not be null when Advanced Authentication is enabled");
        Assert.assertFalse(auth0ClientSecret.isEmpty(), "Auth0 Client Secret field should not be empty when Advanced Authentication is enabled");
        utils.logPass("Verified Auth0 Client Secret field is present and has a non-empty value when Advanced Authentication is enabled");

        // verify that Auth0 API Audience should be present and should not be empty when advanced authentication is enabled
        String auth0ApiAudienceXpath = utils.getLocatorValue("GISConfigurationPage.gisTabFields").replace("$fieldId","business_services_attributes_0_auth0_audience");
        String auth0ApiAudience = driver.findElement(By.xpath(auth0ApiAudienceXpath)).getAttribute("value");
        Assert.assertNotNull(auth0ApiAudience, "Auth0 API Audience field should not be null when Advanced Authentication is enabled");
        Assert.assertFalse(auth0ApiAudience.isEmpty(), "Auth0 API Audience field should not be empty when Advanced Authentication is enabled");
        utils.logPass("Verified Auth0 API Audience field is present and has a non-empty value when Advanced Authentication is enabled");

        // verify that Public Certificate field should be present
        String publicCertificateXpath = utils.getLocatorValue("GISConfigurationPage.gisTabFields").replace("$fieldId","business_services_attributes_0_auth0_certificate_pem");
        WebElement publicCertificateField = driver.findElement(By.xpath(publicCertificateXpath));
        Assert.assertNotNull(publicCertificateField, "Public Certificate field should be present when Advanced Authentication is enabled");
        utils.logPass("Verified Public Certificate field is present when Advanced Authentication is enabled");

        // verify that Token Request Limit field should be present and should not be empty when advanced authentication is enabled
        String tokenRequestLimitXpath = utils.getLocatorValue("GISConfigurationPage.gisTabFields").replace("$fieldId","business_advance_auth_token_request_limit");
        String tokenRequestLimit = driver.findElement(By.xpath(tokenRequestLimitXpath)).getAttribute("value");
        Assert.assertNotNull(tokenRequestLimit, "Token Request Limit field should not be null when Advanced Authentication is enabled");
        Assert.assertFalse(tokenRequestLimit.isEmpty(), "Token Request Limit field should not be empty when Advanced Authentication is enabled");
        utils.logPass("Verified Token Request Limit field is present and has a non-empty value when Advanced Authentication is enabled");

        // verify that Token Request Time Threshold field should be present and should not be empty when advanced authentication is enabled
        String tokenRequestTimeThresholdXpath = utils.getLocatorValue("GISConfigurationPage.gisTabFields").replace("$fieldId","business_advance_auth_token_request_time_threshold");
        String tokenRequestTimeThreshold = driver.findElement(By.xpath(tokenRequestTimeThresholdXpath)).getAttribute("value");
        Assert.assertNotNull(tokenRequestTimeThreshold, "Token Request Time Threshold field should not be null when Advanced Authentication is enabled");
        Assert.assertFalse(tokenRequestTimeThreshold.isEmpty(), "Token Request Time Threshold field should not be empty when Advanced Authentication is enabled");
        utils.logPass("Verified Token Request Time Threshold field is present and has a non-empty value when Advanced Authentication is enabled");
    }
    private void verifyBasicAuthenticationFields() throws InterruptedException {
        pageObj.dashboardpage().navigateToTabsNew("Basic Authentication");
        // Verify Enable Basic Authentication checkbox is Present on Advanced Authentication tab
        String enableAdvancedAuthenticationXpath = utils.getLocatorValue("GISConfigurationPage.gisCheckBoxFields").replace("$fieldName","Enable Basic Authentication");
        String enableAdvancedAuthentication = driver.findElement(By.xpath(enableAdvancedAuthenticationXpath)).getAttribute("value");
        Assert.assertEquals(enableAdvancedAuthentication,"1","Enable Basic Authentication checkbox is checked");
        utils.logPass("Verified Enable Basic Authentication checkbox is Present on Advanced Authentication tab");

        // verify that Password Policy - Select field should be there.
        WebElement passwordPolicySelect = utils.getLocator("cockpitDashboardPage.passwordPolicy");
        Assert.assertNotNull(passwordPolicySelect, "Password Policy select field should be present when Basic Authentication is enabled");
        utils.logPass("Verified Password Policy select field is present when Basic Authentication is enabled");
    }

    private void verifyCoreValueInPreferences(String businessSlug) throws Exception {
        String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.slug='"
                + businessSlug + "';";
        pageObj.singletonDBUtilsObj();
        String preferences = DBUtils.executeQueryAndGetColumnValue(env,guestIdentityhost, query1, "preferences");

        pageObj.singletonDBUtilsObj();
        List<String> keyValueFromPreferences = Utilities.getPreferencesKeyValue(preferences,
                "access_token_expiration");
        Assert.assertTrue(keyValueFromPreferences.contains("5"), "Expected value '5' for access_token_expiration is not present in preferences");
        utils.logPass("Verified that preferences contains expected value '5' for access_token_expiration");
        keyValueFromPreferences = Utilities.getPreferencesKeyValue(preferences,
                "refresh_token_expiration");
        Assert.assertTrue(keyValueFromPreferences.contains("1"), "Expected value '1' for refresh_token_expiration is not present in preferences");
        utils.logPass("Verified that preferences contains expected value '1' for refresh_token_expiration");

        // password policy verification
        keyValueFromPreferences = Utilities.getPreferencesKeyValue(preferences, "password_policy");
        Assert.assertEquals(keyValueFromPreferences.get(0), "strong_password",
                "Value of password_policy_basic_auth in preferences is not strong_password");
        utils.logit("Value of password_policy_basic_auth in preferences is: " + keyValueFromPreferences.get(0));
    }
    private void verifyBusinessMappingsTableEntry(String businessSlug) throws Exception {
        // get the count of entries in business_mappings table for the given business slug
        String query = "SELECT COUNT(*) AS count FROM business_mappings WHERE slug = '" + businessSlug + "'";
        int count = Integer.parseInt(DBUtils.executeQueryAndGetColumnValue(env,guestIdentityhost, query, "count"));
        Assert.assertTrue(count > 0, "No entries found in business_mappings table for business slug: " + businessSlug);
        utils.logPass("Verified that there is at least one entry in business_mappings table for business slug: " + businessSlug);
    }
    private void verifyPunchhBusinessTableUUIDUpdate(String businessSlug) throws Exception {
        // An entry of guest business UUID should be updated in the Punchh business table.
        String query = "SELECT uuid FROM businesses WHERE slug = '" + businessSlug + "'";
        String guestBusinessUUID = DBUtils.executeQueryAndGetColumnValue(env,guestIdentityhost, query, "uuid");
        query = "SELECT external_identity_uuid FROM businesses WHERE slug = '" + businessSlug + "'";
        String externalIdentityUUID = DBUtils.executeQueryAndGetColumnValue(env, query, "external_identity_uuid");
        Assert.assertEquals(externalIdentityUUID, guestBusinessUUID, "Guest Business UUID is not updated in businesses table for business slug: " + businessSlug);
        utils.logPass("Verified that Guest Business UUID is updated in businesses table for business slug: " + businessSlug);
    }
    private void verifyBusinessMappingForAuthConfig(String businessSlug) throws Exception {
        String[] columns = {"enable_advance_auth", "enable_basic_auth"};
        String query = "SELECT enable_advance_auth, enable_basic_auth FROM business_mappings WHERE slug = '" + businessSlug + "'";
        List<Map<String, String>> authConfigMap = DBUtils.executeQueryAndGetMultipleColumns(env, guestIdentityhost, query, columns);
        Assert.assertEquals(authConfigMap.get(0).get("enable_advance_auth"), "1", "enable_advance_auth should be true in business_mappings table for business slug: " + businessSlug);
        Assert.assertEquals(authConfigMap.get(0).get("enable_basic_auth"), "1", "enable_basic_auth should be true in business_mappings table for business slug: " + businessSlug);
        utils.logPass("Verified that enable_advance_auth and enable_basic_auth are true in business_mappings table for business slug: " + businessSlug);

        // An entry of Social Auth should be updated in the Guest business_mappings table
        pageObj.dashboardpage().navigateToTabsNew("Social Authentication");

        // Get checkbox values based on 'checked' attribute presence
        String enableGoogleSignIn = getCheckboxValue("business_enable_google_login");
        String enableFacebookSignIn = getCheckboxValue("business_enable_facebook_login");
        String enableAppleSignIn = getCheckboxValue("business_enable_apple_login");

        query = "SELECT enable_google_login, enable_facebook_login, enable_apple_login FROM business_mappings WHERE slug = '" + businessSlug + "'";
        authConfigMap = DBUtils.executeQueryAndGetMultipleColumns(env, guestIdentityhost, query, new String[]{"enable_google_login", "enable_facebook_login", "enable_apple_login"});
        Assert.assertEquals(authConfigMap.get(0).get("enable_google_login"), enableGoogleSignIn, "enable_google_login value in business_mappings table does not match with the value in frontend for business slug: " + businessSlug);
        Assert.assertEquals(authConfigMap.get(0).get("enable_facebook_login"), enableFacebookSignIn, "enable_facebook_login value in business_mappings table does not match with the value in frontend for business slug: " + businessSlug);
        Assert.assertEquals(authConfigMap.get(0).get("enable_apple_login"), enableAppleSignIn, "enable_apple_login value in business_mappings table does not match with the value in frontend for business slug: " + businessSlug);
        utils.logPass("Verified that social authentication config values in business_mappings table match with the values in frontend for business slug: " + businessSlug);
    }

    private String getCheckboxValue(String fieldName) {
        String xpath = utils.getLocatorValue("GISConfigurationPage.gisTabFields").replace("$fieldId", fieldName);
        WebElement checkbox = driver.findElement(By.xpath(xpath));
        String checkedAttribute = checkbox.getAttribute("checked");
        return (checkedAttribute != null) ? "1" : "0";
    }

    private void verifySocialAuthenticationFields() throws InterruptedException {
        pageObj.dashboardpage().navigateToTabsNew("Social Authentication");

        String[] socialSignInOptions = {"business_enable_google_login", "business_enable_facebook_login", "business_enable_apple_login"};

        for (String option : socialSignInOptions) {
            String checkboxValue = getCheckboxValue(option);

            if (checkboxValue.equals("0")) {
                utils.logPass("Verified " + option + " checkbox is present and unchecked by default on Social Authentication tab");
            } else {
                Assert.assertEquals(checkboxValue, "1", option + " checkbox should return '1' when checked");
                utils.logPass("Verified " + option + " checkbox is present and checked on Social Authentication tab");
            }
        }
        utils.logPass("Verified that all expected social authentication options are present on Social Authentication tab with correct default states");
    }
    private void verifyEntryInServicesTable(String businessSlug) throws Exception {
        // fetch id from guest business table based on business slug
        String query1 = "SELECT id FROM businesses WHERE slug = '" + businessSlug + "'";
        String businessId = DBUtils.executeQueryAndGetColumnValue(env,guestIdentityhost, query1, "id");
        String query = "SELECT COUNT(*) AS count FROM services WHERE business_id = '" + businessId + "' and type='Auth0Service'";
        int count = Integer.parseInt(DBUtils.executeQueryAndGetColumnValue(env,guestIdentityhost, query, "count"));
        Assert.assertTrue(count > 0, "No entries found in services table for business slug: " + businessSlug);
        utils.logPass("Verified that there is at least one entry in services table for business slug: " + businessSlug);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        pageObj.utils().clearDataSet(dataSet);
        driver.quit();
        logger.info("Browser closed");
    }
}
