package com.punchh.server.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.mobileTests.ApplePassesTest;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class AppleLoyaltyPassTest {

    private static final Logger logger = LogManager.getLogger(AppleLoyaltyPassTest.class);

    private WebDriver driver;
    private PageObj pageObj;
    private Utilities utils;
    private ApplePassesTest applePass;

    private String env;
    private String baseUrl;
    private final String run = "ui";
    private String sTCName;
    private Map<String, String> dataSet;
    private Properties prop;

    
    @BeforeClass(alwaysRun = true)
    public void openBrowser() {
        driver = new BrowserUtilities().launchBrowser();
        pageObj = new PageObj(driver);
        utils = new Utilities(driver);
        applePass = new ApplePassesTest();
        env = pageObj.getEnvDetails().setEnv();
        baseUrl = pageObj.getEnvDetails().setBaseUrl();
        pageObj.instanceDashboardPage().instanceLogin(baseUrl);
    }


    @BeforeMethod(alwaysRun = true)
    public void setUp(Method method) {
    	prop = Utilities.loadPropertiesFile("config.properties");
		sTCName = method.getName();
		utils = new Utilities(driver);
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
    }


    @DataProvider(name = "userIdentifierProvider")
    public Object[][] userIdentifierProvider(Method method) {
        return new Object[][]{{"User Code"}, {"Email"}, {"Phone"}};
    }

    @Test(dataProvider = "userIdentifierProvider", description = "SQ-T7130 Verify user identifier for apple loyalty passes || "
    		+ "SQ-T7131 Verify nfc parameter with user identifier as Email || "
    		+ "SQ-T7132 Verify nfc parameter with user identifier as Phone || "
    		+ "SQ-T7133 Verify nfc parameter with user identifier as Usercode || ")
    @Owner(name = "Apurva Agarwal")
    public void T7130_T7131_T7132_T7133_verifyNFCParameterWithUserIdentifiers(String userIdentifier) throws InterruptedException, IOException {
        // Navigate to the instance and select the required business
        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

        // Step 1: Navigate to Settings -> Passes
        pageObj.menupage().navigateToSubMenuItem("Settings", "Passes");
        
        //Step 2: Click on "Edit Loyalty Pass" button under the Apple Loyalty Pass section
        pageObj.settingsPage().clickOnDesiredPassButton(dataSet.get("passType"),dataSet.get("buttonType"));

        //Step 3: Navigate to the Apple Pass settings tab
        pageObj.settingsPage().clickOnApplePassTab(dataSet.get("tab"));
        
        //Step 4: Enable NFC capability Checkbox for the Apple Loyalty Pass
        pageObj.settingsPage().clickOnApplePassCheckBox("Near Field Communication (NFC) capability","ON");

        //Step 5: Select the User Identifier value from the dropdown
        pageObj.settingsPage().setUserIdentifierForPass(userIdentifier);
        utils.logit("User Identifier value selected as: '" + userIdentifier+"' ");
        
        //Step 6: Save the Apple Pass configuration
        pageObj.settingsPage().clickOnApplePassSaveButton();

        //Step 7: Sign up a new user via API to generate a loyalty pass
        String userEmail =CreateDateTime.getUniqueString("applePassUser_") + "@partech.com";
        utils.logit("Creating random user email :: ",userEmail);
        String phone=dataSet.get("phone")+RandomStringUtils.randomNumeric(4);
        Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail,dataSet.get("client"),dataSet.get("secret"),phone);
       
        // Validate successful user signup
        Assert.assertEquals(signUpResponse.getStatusCode(),ApiConstants.HTTP_STATUS_OK,"User signup failed");
        utils.logPass("Api1 user signup is successful");

        // Step 8: Prepare download directory for the Apple .pkpass file
        String pkpassFilePath =System.getProperty("user.dir") + "/resources/ExportData";
        utils.logit("File Download Directory Path :: ", pkpassFilePath);
        pageObj.guestTimelinePage().createAndCleanDownloadBrowserDownloadFolder(pkpassFilePath);
        
       // Step 9: Navigate to Guest Timeline and download the Apple Loyalty Pass
        pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
        pageObj.guestTimelinePage().clickOnDownloadApplePassButton();
        utils.logit("Clicked on 'Download Apple Pass' Button");

       // Wait for the .pkpass file download to complete
        utils.longwait(5000);
        
        // Step 10: Extract and parse pass.json from the downloaded .pkpass file
        JsonNode passJson = applePass.extractJsonFromPKPass(pkpassFilePath + "/loyalty_pass.pkpass");
        utils.logPass("pass.json successfully extracted and parsed.");
        utils.logit("Extracted pass.json content: " + passJson.toPrettyString());

        // Step 11: Validate NFC message in the Apple pass
        String nfcMessage = passJson.path("nfc").path("message").asText();
        utils.logit("NFC Message from pkpass: " + nfcMessage);

        // Step 12: Validate NFC message data with guest timeline data
        switch (userIdentifier) {
            case "User Code":
                String guestCode =pageObj.guestTimelinePage().getGuestCode();
                utils.logit("Guest Code from Timeline :: " + guestCode);
                Assert.assertEquals(nfcMessage,guestCode,"NFC message mismatch with Guest Code");
                break;
                
            case "Email":
                Assert.assertEquals(nfcMessage.toLowerCase(),userEmail.toLowerCase(),"NFC message mismatch with Email");
                break;

            case "Phone":
                Assert.assertEquals(nfcMessage,phone,"NFC message mismatch with Phone");
                break;
                
            default:
                Assert.fail("Unsupported User Identifier");
        }
        
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
