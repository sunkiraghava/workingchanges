package com.punchh.server.Test;

import java.io.IOException;
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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class GoogleWalletNFCTest {

    private static final Logger logger = LogManager.getLogger(GoogleWalletNFCTest.class);

    private WebDriver driver;
    private PageObj pageObj;
    private Utilities utils;
    private String env;
    private final String run = "ui";
    private String sTCName;
    private Map<String, String> dataSet;

    
    @BeforeMethod(alwaysRun = true)
    public void setUp(Method method) {
    	pageObj = new PageObj(driver);
        utils = new Utilities(driver);
        env = pageObj.getEnvDetails().setEnv();
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


    @DataProvider(name = "apiEndPointsProvider")
    public Object[][] userIdentifierProvider(Method method) {
    	// Define API types to test
    	 String[] apiTypes = {"search", "find"};
    	// Define different NFC token keys to test
    	 String[] nfcTokenKeys = {"email_nfcToken","phone_nfcToken","userToken_nfcToken","redemptionCode_nfcToken","rewardId_nfcToken"};
    	// Prepare a list of test data combinations
    	 List<Object[]> data = new ArrayList<>();
    	 for (String api : apiTypes) {
    	        for (String tokenKey : nfcTokenKeys) {
    	            data.add(new Object[]{api, tokenKey});
    	        }
    	  }
    	// Convert list to 2D Object array for TestNG DataProvider
    	 return data.toArray(new Object[0][]);
    }

    @Test(dataProvider = "apiEndPointsProvider", description = "SQ-T7192 Verify successful user identification using valid Google Wallet NFC token via api/pos/users/search endpoint"
    		+ "SQ-T7193 Verify successful user identification using valid Google Wallet NFC token via api/pos/users/find endpoint")
    @Owner(name = "Apurva Agarwal")
    public void T7192_T7193_verifyuserIdentificationsUsingValidGoogleWalletNFCTokens(String apiType,String nfcTokenKey) throws InterruptedException, IOException {
        
        Response response = null;
        utils.logit("Starting test for API: " + apiType + " | NFC Token: " + nfcTokenKey);

        // Step 1: Call the appropriate API endpoint based on the apiType parameter
        // - If apiType is "search", call searchUserByNfcToken API
        // - If apiType is "find", call findUserByNfcToken API
        if ("search".equalsIgnoreCase(apiType)) {
            response = pageObj.endpoints().searchUserByNfcToken("nfc_token",dataSet.get(nfcTokenKey),dataSet.get("locationkey"));
        } else if ("find".equalsIgnoreCase(apiType)) {
            response = pageObj.endpoints().userLookupPosApiWithoutExt_uid("nfc_token", dataSet.get(nfcTokenKey), dataSet.get("locationkey"));
        }

        // Step 2: Verify that the API response returned HTTP status code 200 (OK)
        Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "API call failed");

        // Step 3: Extract relevant information from the API response
        String actualEmail = response.jsonPath().getString("email"); // User email from response
        utils.logit("Extracted Email from API response: " + actualEmail);

        String actualUserId;
        if ("search".equalsIgnoreCase(apiType)) {
            actualUserId = response.jsonPath().getString("id");      // search API returns "id"
        } else {
            actualUserId = response.jsonPath().getString("user_id"); // find API returns "user_id"
        }
        utils.logit("Extracted User ID from API response: " + actualUserId);

        // Step 4: Validate the user information returned by API against the expected test data
        utils.logit("Validating email and user ID against expected data...");
        Assert.assertEquals(actualEmail, dataSet.get("email"), "Email mismatch");
        Assert.assertEquals(actualUserId, dataSet.get("user_id"), "User ID mismatch");
        utils.logit("Test PASSED for API=" + apiType + ", NFC Token=" + nfcTokenKey + ", User=" + actualEmail);
        
    }
    
    @Test(description = "SQ-T7202 Verify proper error handling for invalid/malformed NFC tokens via api/pos/users/search endpoint"
    		+ "SQ-T7203 Verify proper error handling for invalid/malformed NFC tokens via api/pos/users/find endpoint"
    		+ "SQ-T7204 Verify API handles requests without nfc_token parameter"
    		+ "SQ-T7205 Verify existing Apple Wallet NFC functionality remains unaffected", dataProvider = "TestDataProvider")
    @Owner(name = "Rakhi Rawat")
    public void T7202_verifyErrorHandlingForInvalidNfcToken(String nfcTokenKey) throws InterruptedException, IOException {
    	
    	String invalidNfcToken = CreateDateTime.getRandomString(10);
    	
    	String queryParamKey1 = "nfc_token";
    	String queryParamKey2 = "apple_nfc_data";
    	//api/pos/users/search with invalid nfc_token 
    	Response balanceResponse = pageObj.endpoints().searchUserByNfcToken(queryParamKey1,invalidNfcToken,
				dataSet.get("locationkey"));
		Assert.assertEquals(balanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND, "Error in getting user balance");

		Assert.assertEquals(balanceResponse.jsonPath().getString("[0]"), "User not found",
				"Error message not mached for invalid nfc token with /api/pos/users/search");
		utils.logPass("Verified api/pos/users/search show proper error for invalid/malformed NFC tokens");
		
		//api/pos/users/find with invalid nfc_token
		Response balanceResponse1 = pageObj.endpoints().userLookupPosApiWithNfc("nfc_token",invalidNfcToken,
				dataSet.get("locationkey"),"");
		Assert.assertEquals(balanceResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND, "Error in getting user balance");

		Assert.assertEquals(balanceResponse1.jsonPath().getString("[0]"), "User not found",
				"Error message not matched for invalid nfc token with api/pos/users/find");
		utils.logPass("Verified api/pos/users/find show proper error for invalid/malformed NFC tokens");
		
		//pos user signup
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response response = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationkey"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String userId = response.jsonPath().get("id").toString();
		
		// api/pos/users/search without nfc_token
		Response balanceResponse2 = pageObj.endpoints().posUserLookupFetchBalance(userEmail, dataSet.get("locationkey"));
		Assert.assertEquals(balanceResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Error in getting user balance");
		Assert.assertEquals(balanceResponse2.jsonPath().get("email").toString(), userEmail.toLowerCase());
		Assert.assertEquals(balanceResponse2.jsonPath().get("id").toString(),userId);
		utils.logPass("Verified api/pos/users/search return user's details without NFC token parameter");
		
		//api/pos/users/find without nfc_token
		Response balanceResponse3 = pageObj.endpoints().userLookupPosApi("email", userEmail,
				dataSet.get("locationkey"), "");
		Assert.assertEquals(balanceResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for POS user lookUp with invalid email");
		Assert.assertEquals(balanceResponse3.jsonPath().get("email").toString(), userEmail.toLowerCase());
		Assert.assertEquals(balanceResponse3.jsonPath().get("user_id").toString(),userId );
		utils.logPass("Verified api/pos/users/find return user's details without NFC token parameter");
		
		//api/pos/users/search with apple_nfc_data 
    	Response balanceResponse4 = pageObj.endpoints().searchUserByNfcToken(queryParamKey2, dataSet.get(nfcTokenKey),
				dataSet.get("locationkey"));
		Assert.assertEquals(balanceResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Error in getting user balance");
		Assert.assertEquals(balanceResponse4.jsonPath().get("email").toString(), dataSet.get("userEmail"));
		Assert.assertEquals(balanceResponse4.jsonPath().get("id").toString(),dataSet.get("userId"));
		utils.logPass("Verified api/pos/users/search return user's details with apple_nfc_data");
		
		// api/pos/users/find with apple_nfc_data 
		Response balanceResponse5 = pageObj.endpoints().userLookupPosApiWithNfc("apple_nfc_data", dataSet.get(nfcTokenKey),
				dataSet.get("locationkey"), "");
		Assert.assertEquals(balanceResponse5.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Error in getting user balance");
		Assert.assertEquals(balanceResponse5.jsonPath().get("email").toString(), dataSet.get("userEmail"));
		Assert.assertEquals(balanceResponse5.jsonPath().get("user_id").toString(), dataSet.get("userId") );
		utils.logPass("Verified api/pos/users/find return user's details with apple_nfc_data");
		
	}
    @DataProvider(name = "TestDataProvider")

	public Object[][] testDataProvider() {

		return new Object[][] { { "email_nfcToken" }, { "phone_nfcToken" }, { "userToken_nfcToken" }, { "redemptionCode_nfcToken" }, { "rewardId_nfcToken" } };
	}

    
    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        pageObj.utils().clearDataSet(dataSet);
        logger.info("Data set cleared");
    }

}
