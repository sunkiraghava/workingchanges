package com.punchh.server.apiTest;

import org.testng.Assert;
import org.testng.annotations.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.restassured.response.Response;
import org.openqa.selenium.WebDriver;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.punchh.server.OfferIngestionUtilityClass.OfferIngestionUtilities;
import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.Utilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;

@Listeners(TestListeners.class)
public class ParGamesAPI {
    private static Logger logger = LogManager.getLogger(ParGamesAPI.class);
    public WebDriver driver;
    private PageObj pageObj;
    private String sTCName;
    private String userEmail, businessId, businessesQuery;
    private String env;
    private static Map<String, String> dataSet;
    private Utilities utils;

    @BeforeMethod(alwaysRun = true)
    public void setUp(Method method) {
        pageObj = new PageObj(driver);
        env = pageObj.getEnvDetails().setEnv();
        sTCName = method.getName();
        dataSet = new ConcurrentHashMap<>();
        pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath("api", env), sTCName);
        dataSet = pageObj.readData().readTestData;
        pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath("ui", env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
        logger.info(sTCName + " ==> " + dataSet);
        utils = new Utilities(driver);
        businessId = dataSet.get("business_id");
        businessesQuery = OfferIngestionUtilities.businessPreferenceQuery.replace("$id", businessId);
    }

    // Helper to signup user and return token
    private String getApi2Token() {
        userEmail = pageObj.iframeSingUpPage().generateEmail();
        Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
        pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
        Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not match for API2 signup");
        TestListeners.extentTest.get().pass("Api2 user signup is successful");
        return signUpResponse.jsonPath().getString("access_token.token");
    }

    // Validate channel values
    private void validateChannels(List<String> channels, String filterChannel) {
        for (String channelValue : channels) {
            if (filterChannel.equalsIgnoreCase("mobile")) {
                Assert.assertTrue(channelValue.equalsIgnoreCase("mobile") || channelValue.equalsIgnoreCase("mobile_web"),
                        "Invalid channel found for mobile filter: " + channelValue);
            } else if (filterChannel.equalsIgnoreCase("web")) {
                Assert.assertEquals(channelValue.toLowerCase(), "web", "Invalid channel found for web filter");
            } else if (filterChannel.equalsIgnoreCase("mobile_web")) {
                Assert.assertEquals(channelValue.toLowerCase(), "mobile_web", "Invalid channel found for mobile_web filter");
            } else {
                Assert.fail("Unknown channel filter passed: " + filterChannel);
            }
        }
    }

    // ------------------- SUCCESS SCENARIOS -------------------

    @Test(description = "SQ-T7112, SQ-T7115 Verify fetching active games list with valid token and client")
    @Owner(name = "Shivam Maurya")
    public void SQ_T7112_SQ_T7115_verifyFetchActiveGamesListSuccess() {
        String token = getApi2Token();
        TestListeners.extentTest.get().info("== Fetching active games list with valid token and client ==");
        Response response = pageObj.endpoints().apiGetGameUrls(
                dataSet.get("client"), dataSet.get("secret"), token,
                dataSet.get("page"), dataSet.get("per_page"), dataSet.get("channel")
        );
        Assert.assertEquals(response.getStatusCode(), 200, "Expected 200 OK for valid request");

        List<Map<String, Object>> gamesList = response.jsonPath().getList("games");
        Assert.assertNotNull(gamesList, "Games array should be present");

        if (!gamesList.isEmpty()) {
            // Validate first game fields
            Assert.assertNotNull(gamesList.get(0).get("id"), "Game ID should not be null");
            Assert.assertNotNull(gamesList.get(0).get("name"), "Game name should not be null");
            Assert.assertEquals(gamesList.get(0).get("status"), "active", "Game status should be active");
        }

        // Validate pagination block
        Assert.assertNotNull(response.jsonPath().get("pagination"), "Pagination metadata should be present");
        int totalCount = response.jsonPath().getInt("pagination.total_count");
        Assert.assertEquals(totalCount, gamesList.size(), "Total count should match number of returned games");

        // Validate next/prev page flags
        int totalPages = response.jsonPath().getInt("pagination.total_pages");
        int currentPage = response.jsonPath().getInt("pagination.current_page");
        boolean hasNextPage = response.jsonPath().getBoolean("pagination.has_next_page");
        boolean hasPreviousPage = response.jsonPath().getBoolean("pagination.has_previous_page");

        Assert.assertEquals(hasNextPage, currentPage < totalPages, "has_next_page mismatch");
        Assert.assertEquals(hasPreviousPage, currentPage > 1, "has_previous_page mismatch");

     // Validate channel filter only when channel is provided
        String filterChannel = dataSet.get("channel");

        // Extract channel list from response
        List<String> channels = response.jsonPath().getList("games.channel");

        if (filterChannel != null && !filterChannel.trim().isEmpty()) {
            // Apply filtering rules only when channel is actually provided
            validateChannels(channels, filterChannel);
            logger.info("Channel filter validation applied for channel: " + filterChannel);
        } else {
            // No filter → ensure some results exist
            Assert.assertFalse(channels.isEmpty(), "Games list should not be empty with no channel filter");
            logger.info("Channel parameter empty/null — validated API returned all allowed channel results");
        }

        TestListeners.extentTest.get().pass("Active games list fetched successfully");
        logger.info("Active games list fetched successfully");
    }

    @Test(description = "SQ-T7113 Verify fetching games list returns empty when no games configured")
    @Owner(name = "Shivam Maurya")
    public void SQ_T7113_verifyFetchGamesListEmpty() throws Exception {
    	// Set games flags to required values
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		DBUtils.updateBusinessFlag(env, expColValue, "false", "has_games", businessId);
		DBUtils.updateBusinessFlag(env, expColValue, "false", "has_scratch_game", businessId);
		DBUtils.updateBusinessFlag(env, expColValue, "false", "has_slot_machine_game", businessId);
		DBUtils.updateBusinessFlag(env, expColValue, "true", "enable_cataboom_integration", businessId);

        String token = getApi2Token();
        TestListeners.extentTest.get().info("== Fetching games list when no games configured ==");
        Response response = pageObj.endpoints().apiGetGameUrls(
                dataSet.get("client"), dataSet.get("secret"), token,
                dataSet.get("page"), dataSet.get("per_page"), dataSet.get("channel")
        );

        Assert.assertEquals(response.getStatusCode(), 200, "Expected 200 OK for empty games list");
        Assert.assertTrue(response.jsonPath().getList("games").isEmpty(), "Games array should be empty");
        Assert.assertEquals(response.jsonPath().getInt("pagination.total_count"), 0, "Total count should be 0 for empty state");
        Assert.assertEquals(response.jsonPath().getInt("pagination.total_pages"), 0, "Total pages should be 0 for empty data");
        Assert.assertFalse(response.jsonPath().getBoolean("pagination.has_next_page"), "has_next_page should be false");
        Assert.assertFalse(response.jsonPath().getBoolean("pagination.has_previous_page"), "has_previous_page should be false");

        TestListeners.extentTest.get().pass("Empty games list response validated successfully");
        logger.info("Empty games list response validated successfully");
    }

    @Test(description = "SQ-T7116 Verify pagination parameters default and limits")
    @Owner(name = "Shivam Maurya")
    public void SQ_T7116_verifyPaginationDefaultsAndLimits() {
    	
    	String token = getApi2Token();
    	TestListeners.extentTest.get().info("== Pagination parameters default and limits ==");

    	Response response = pageObj.endpoints().apiGetGameUrls(
    	        dataSet.get("client"), dataSet.get("secret"), token,
    	        dataSet.get("invalid_page"), dataSet.get("invalid_per_page"), dataSet.get("channel")
    	);

    	Assert.assertEquals(response.getStatusCode(), 200, "Expected 200 OK");

    	int currentPage = response.jsonPath().getInt("pagination.current_page");
    	int perPage = response.jsonPath().getInt("pagination.per_page");
    	int totalCount = response.jsonPath().getInt("pagination.total_count");

    	// Default behavior assertions
    	Assert.assertEquals(currentPage, 1, "Current page must default to 1");
    	Assert.assertEquals(perPage, 20, "per_page must default to 20 for invalid values");

    	int expectedTotalPages = totalCount == 0 ? 0 :
    	        (int) Math.ceil((double) totalCount / perPage);

    	Assert.assertEquals(
    	        response.jsonPath().getInt("pagination.total_pages"),
    	        expectedTotalPages,
    	        "Total pages calculation mismatch"
    	);

    	// Navigation flags
    	boolean expectedHasNext = currentPage < expectedTotalPages;
    	boolean expectedHasPrev = currentPage > 1;

    	Assert.assertEquals(
    	        response.jsonPath().getBoolean("pagination.has_next_page"),
    	        expectedHasNext,
    	        "has_next_page mismatch"
    	);

    	Assert.assertEquals(
    	        response.jsonPath().getBoolean("pagination.has_previous_page"),
    	        expectedHasPrev,
    	        "has_previous_page mismatch"
    	);

    	// Validate channel filter application
    	List<String> channels = response.jsonPath().getList("games.channel");
    	validateChannels(channels, dataSet.get("channel"));

    	// Validate only ACTIVE games are returned
    	List<String> statuses = response.jsonPath().getList("games.status");
    	Assert.assertNotNull(statuses, "Game status list should not be null");

    	for (String status : statuses) {
    	    Assert.assertEquals(
    	            status,
    	            "active",
    	            "Non-active game returned in response"
    	    );
    	}

    	TestListeners.extentTest.get().pass("Pagination, Channel & Active Status validations successful");
    	logger.info("Pagination, Channel & Active Status validations successful");

    	
    }
    	

    @Test(description = "SQ-T7117 SQ-T7118 Verify GET /api2/mobile/par_games returns same structure as /api/mobile/par_games")
    @Owner(name = "Shivam Maurya")
    public void SQ_T7117_SQ_T7118_verifyApi2EndpointStructure() {
        String token = getApi2Token();
        TestListeners.extentTest.get().info("== /api2/mobile/par_games returns same structure ==");
        Response response = pageObj.endpoints().apiGetGameUrlsApi2(
                dataSet.get("client"), dataSet.get("secret"), token,
                dataSet.get("page"), dataSet.get("per_page"), dataSet.get("channel")
        );

        Assert.assertEquals(response.getStatusCode(), 200, "Expected 200 OK");

        List<Map<String, Object>> games = response.jsonPath().getList("games");
        Assert.assertNotNull(games, "Games array must be present");

        if (!games.isEmpty()) {
            for (Map<String, Object> game : games) {
                Assert.assertNotNull(game.get("id"), "Game id should not be null");
                Assert.assertNotNull(game.get("name"), "Game name should not be null");
                Assert.assertNotNull(game.get("url"), "Game url should not be null");

                String channel = game.get("channel").toString();
                Assert.assertTrue(channel.equalsIgnoreCase("mobile") || channel.equalsIgnoreCase("mobile_web") || channel.equalsIgnoreCase("web"),
                        "Invalid channel: " + channel + ". Must be mobile, mobile_web, or web");
            }
        }

        int totalCount = response.jsonPath().getInt("pagination.total_count");
        boolean hasNextPage = response.jsonPath().getBoolean("pagination.has_next_page");
        boolean hasPrevPage = response.jsonPath().getBoolean("pagination.has_previous_page");

        if (totalCount == 0) {
            Assert.assertFalse(hasNextPage, "No next page expected when no games");
            Assert.assertFalse(hasPrevPage, "No previous page expected when no games");
        } else {
            int totalPages = response.jsonPath().getInt("pagination.total_pages");
            int currentPage = response.jsonPath().getInt("pagination.current_page");
            Assert.assertEquals(hasNextPage, currentPage < totalPages, "has_next_page flag mismatch");
            Assert.assertEquals(hasPrevPage, currentPage > 1, "has_previous_page flag mismatch");
        }

        TestListeners.extentTest.get().pass("Response validated for both valid & empty results");
        logger.info("Response validation completed successfully");
    }

    // ------------------- NEGATIVE / ERROR SCENARIOS -------------------

    @Test(description = "SQ-T7120 Verify error when client param is missing or invalid", groups = "Regression")
    @Owner(name = "Shivam Maurya")
    public void SQ_T7120_verifyClientErrors() {
        String token = getApi2Token();

        // Missing client
        Response responseMissingClient = pageObj.endpoints().apiGetGameUrls(
                null,
                dataSet.get("secret"),
                token,
                dataSet.get("page"),
                dataSet.get("per_page"),
                dataSet.get("channel")
        );
        Assert.assertEquals(responseMissingClient.getStatusCode(), 412, "Expected 412 Precondition Failed for missing client");
        Assert.assertEquals(responseMissingClient.jsonPath().getList("$").get(0), "Invalid Signature", "Error message mismatch");

        // Invalid client
        Response responseInvalidClient = pageObj.endpoints().apiGetGameUrls(	
                dataSet.get("invalid_client"),
                dataSet.get("secret"),
                token,
                dataSet.get("page"),
                dataSet.get("per_page"),
                dataSet.get("channel")
        );
        Assert.assertEquals(responseInvalidClient.getStatusCode(), 412, "Expected 412 Precondition Failed for invalid client");
        Assert.assertEquals(responseInvalidClient.jsonPath().getList("$").get(0), "Invalid Signature", "Error message mismatch");

        TestListeners.extentTest.get().pass("412 Invalid Signature validated successfully for missing/invalid client");
        logger.info("412 Invalid Signature validated successfully for missing/invalid client");
    }

    @Test(description = "SQ-T7114 Verify error when Authorization token is missing or invalid", groups = "Regression")
    @Owner(name = "Shivam Maurya")
    public void SQ_T7114_verifyAuthTokenErrors() {
        // Missing token
        Response responseMissingToken = pageObj.endpoints().apiGetGameUrls(
                dataSet.get("client"),
                dataSet.get("secret"),
                null,
                dataSet.get("page"),
                dataSet.get("per_page"),
                dataSet.get("channel")
        );
        Assert.assertEquals(responseMissingToken.getStatusCode(), 401, "Expected 401 Unauthorized for missing token");
        Assert.assertEquals(responseMissingToken.jsonPath().getString("error"),
                "You need to sign in or sign up before continuing.", "Error message mismatch for missing token");

        // Invalid token
        Response responseInvalidToken = pageObj.endpoints().apiGetGameUrls(
                dataSet.get("client"),
                dataSet.get("secret"),
                dataSet.get("invalid_authToken"),
                dataSet.get("page"),
                dataSet.get("per_page"),
                dataSet.get("channel")
        );
        Assert.assertEquals(responseInvalidToken.getStatusCode(), 401, "Expected 401 Unauthorized for invalid token");
        Assert.assertEquals(responseInvalidToken.jsonPath().getString("error"),
                "You need to sign in or sign up before continuing.", "Error message mismatch for invalid token");

        TestListeners.extentTest.get().pass("401 Unauthorized validated successfully for missing/invalid token");
        logger.info("401 Unauthorized validated successfully for missing/invalid token");
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        pageObj.utils().clearDataSet(dataSet);
        logger.info("Test data cleared");
    }
}
