package com.punchh.server.apiTest;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class ItemRecSysAPITest {
	static Logger logger = LogManager.getLogger(ItemRecSysAPITest.class);
	public WebDriver driver;
	PageObj pageObj;
	String sTCName, apiKey;
	private String env, run = "api";
	private static Map<String, String> dataSet;
	Properties prop;
	private Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		prop = Utilities.loadPropertiesFile("apiConfig.properties");
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
		apiKey = utils.decrypt(prop.getProperty("irsApiKey"));
	}

	@Test(description = "SQ-T6835: Verify Item Rec Sys Admin ping, settings, menu and user APIs")
	@Owner(name = "Vaibhav Agnihotri")
	public void T6835_verifyItemRecSysAdminAPIs() throws Exception {
		// Ping
		Response pingResponse = pageObj.endpoints().itemRecSysAdminApi("ping", apiKey, dataSet.get("tacoCabanaBuuid"),
				"");
		Assert.assertEquals(pingResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String pingStatus = pingResponse.jsonPath().get("status").toString();
		Assert.assertEquals(pingStatus, "ok");
		utils.logit("pass", "Verified IRS Admin Ping API");

		// Ping with full check
		Response pingFullCheckResponse = pageObj.endpoints().itemRecSysAdminApi("ping full check", apiKey,
				dataSet.get("tacoCabanaBuuid"), "1");
		Assert.assertEquals(pingFullCheckResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String pingFullCheckStatus = pingFullCheckResponse.jsonPath().get("status").toString();
		Assert.assertEquals(pingFullCheckStatus, "ok");
		utils.logit("pass", "Verified IRS Admin Ping API with full check");

		// Get business settings with invalid business uuid
		Response settingsResponse = pageObj.endpoints().itemRecSysAdminApi("get settings", apiKey, "123", "");
		Assert.assertEquals(settingsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String settingsResponseData = settingsResponse.asString();
		Assert.assertEquals(settingsResponseData, "{}");
		utils.logit("pass", "Verified IRS Admin Get Business Settings API with invalid business uuid");

		// Get user predictions data for string user id
		Response userStringResponse = pageObj.endpoints().itemRecSysAdminApi("get user", apiKey,
				dataSet.get("tacoCabanaBuuid"), dataSet.get("tacoCabanaUid"));
		Assert.assertEquals(userStringResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String userStringItems = userStringResponse.jsonPath().get("predictions.data[0]").toString();
		Assert.assertTrue(userStringItems.contains("items"));
		utils.logit("pass", "Verified IRS Admin Get User predictions API with string user id");

		// Get user predictions data for integer user id
		Response userIntResponse = pageObj.endpoints().itemRecSysAdminApi("get user", apiKey,
				dataSet.get("tropicalSmoothieCafeBuuid"), dataSet.get("tropicalSmoothieCafeUid"));
		Assert.assertEquals(userIntResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// String userIntItems = userIntResponse.jsonPath().get("predictions.data[0]").toString();
		Assert.assertTrue(userIntResponse.asString().contains("predictions"));
		utils.logit("pass", "Verified IRS Admin Get User predictions API with integer user id");

		// Get user predictions data for non-existing user id
		Response userNonExistingResponse = pageObj.endpoints().itemRecSysAdminApi("get user", apiKey,
				dataSet.get("tacoCabanaBuuid"), "111");
		Assert.assertEquals(userNonExistingResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String userNonExistingResponseData = userNonExistingResponse.jsonPath().get("predictions").toString();
		Assert.assertEquals(userNonExistingResponseData, "[]");
		utils.logit("pass", "Verified IRS Admin Get User predictions API with non-existing user id");

		// Delete user predictions data when data is not present
		Response deleteUserResponse = pageObj.endpoints().itemRecSysAdminApi("delete user", apiKey,
				dataSet.get("tacoCabanaBuuid"), "123");
		Assert.assertEquals(deleteUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean deleteUserStatus = deleteUserResponse.jsonPath().get("had_data");
		Assert.assertEquals(deleteUserStatus, false);
		utils.logit("pass", "Verified IRS Admin Delete User predictions API when data is not present");

		// Get specific business menu using default menu_id
		Response menuResponse = pageObj.endpoints().itemRecSysAdminApi("get specific menu", apiKey,
				dataSet.get("tacoCabanaBuuid"), "default");
		Assert.assertEquals(menuResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String menuResponseData = menuResponse.jsonPath().get("id").toString();
		Assert.assertEquals(menuResponseData, "default");
		utils.logit("pass", "Verified IRS Admin Get Business Menu API with default menu id");

		// Get business menu using invalid menu_id
		Response menuInvalidResponse = pageObj.endpoints().itemRecSysAdminApi("get specific menu", apiKey,
				dataSet.get("tacoCabanaBuuid"), "abc");
		Assert.assertEquals(menuInvalidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String menuInvalidResponseData = menuInvalidResponse.asString();
		Assert.assertEquals(menuInvalidResponseData, "{}");
		utils.logit("pass", "Verified IRS Admin Get Business Menu API with invalid menu id");

		// Get business menu without passing a menu_id
		Response menuWithoutIdResponse = pageObj.endpoints().itemRecSysAdminApi("get menu", apiKey,
				dataSet.get("tacoCabanaBuuid"), "");
		Assert.assertEquals(menuWithoutIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String menuWithoutIdResponseData = menuWithoutIdResponse.jsonPath().get("id").toString();
		Assert.assertEquals(menuWithoutIdResponseData, "default");
		utils.logit("pass", "Verified IRS Admin Get Business Menu API without passing a menu id");

		// Negative case: Get business settings with invalid API key
		Response settingsInvalidApiKeyResponse = pageObj.endpoints().itemRecSysAdminApi("get settings", "123",
				dataSet.get("tacoCabanaBuuid"), "");
		Assert.assertEquals(settingsInvalidApiKeyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_FORBIDDEN);
		utils.logit("pass", "Verified IRS Admin API negative case to Get Business Settings with invalid API key");

		// Negative case: Get business settings with empty API key
		Response settingsEmptyApiKeyResponse = pageObj.endpoints().itemRecSysAdminApi("get settings", "",
				dataSet.get("tacoCabanaBuuid"), "");
		Assert.assertEquals(settingsEmptyApiKeyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		utils.logit("pass", "Verified IRS Admin API negative case to Get Business Settings with empty API key");

	}

	@Test(description = "SQ-T6837: Verify Item Rec Sys Admin menu_adjuster APIs")
	@Owner(name = "Vaibhav Agnihotri")
	public void T6837_verifyItemRecSysAdminMenuAdjusterAPIs() throws Exception {

		// Update attribute's value
		Response updateExistingResponse = pageObj.endpoints().irsMenuAdjusterPostApi("update value", apiKey,
				dataSet.get("tacoCabanaBuuid"), "price_cap_percentage", "50", "");
		Assert.assertEquals(updateExistingResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String updatedValue = updateExistingResponse.asString();
		Assert.assertEquals(updatedValue, "50");
		utils.logit("pass", "Verified IRS Admin Menu Adjuster API to update existing attribute's value");

		// Update string attribute with unquoted string type value
		Response updateUnquotedStringResponse = pageObj.endpoints().irsMenuAdjusterPostApi("update unquoted type",
				apiKey, dataSet.get("tacoCabanaBuuid"), "string_attribute", "string_value", "string");
		Assert.assertEquals(updateUnquotedStringResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String updatedUnquotedStringValue = updateUnquotedStringResponse.asString();
		Assert.assertEquals(updatedUnquotedStringValue, "\"string_value\"");
		utils.logit("pass",
				"Verified IRS Admin Menu Adjuster API to update string attribute with unquoted string type value");

		// Update string attribute with single quoted string type value
		Response updateQuotedStringResponse = pageObj.endpoints().irsMenuAdjusterPostApi("update quoted string", apiKey,
				dataSet.get("tacoCabanaBuuid"), "string_attribute", "'str_value'", "");
		Assert.assertEquals(updateQuotedStringResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String updatedQuotedStringValue = updateQuotedStringResponse.asString();
		Assert.assertEquals(updatedQuotedStringValue, "\"str_value\"");
		utils.logit("pass",
				"Verified IRS Admin Menu Adjuster API to update string attribute with single quoted string type value");

		// Update other attribute with unquoted other type value
		Response updateUnquotedOtherResponse = pageObj.endpoints().irsMenuAdjusterPostApi("update unquoted type",
				apiKey, dataSet.get("tacoCabanaBuuid"), "min_price", "4.5", "other");
		Assert.assertEquals(updateUnquotedOtherResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String updatedUnquotedOtherValue = updateUnquotedOtherResponse.asString();
		Assert.assertEquals(updatedUnquotedOtherValue, "4.5");
		utils.logit("pass",
				"Verified IRS Admin Menu Adjuster API to update other attribute with unquoted other type value");

		// Negative case: Update integer attribute with unquoted int type value
		Response updateUnquotedIntResponse = pageObj.endpoints().irsMenuAdjusterPostApi("update unquoted type", apiKey,
				dataSet.get("tacoCabanaBuuid"), "price_cap_percentage", "50", "int");
		Assert.assertEquals(updateUnquotedIntResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		String errorMsg = updateUnquotedIntResponse.jsonPath().get("detail.msg[0]").toString();
		Assert.assertEquals(errorMsg, "unexpected value; permitted: 'other', 'str', 'string'");
		utils.logit("pass",
				"Verified IRS Admin Menu Adjuster API negative case to update integer attribute with unquoted int type value");

		// Negative case: Update string attribute with unquoted value and without type
		Response updateWithoutTypeResponse = pageObj.endpoints().irsMenuAdjusterPostApi("update value", apiKey,
				dataSet.get("tacoCabanaBuuid"), "string_attribute", "string_value", "");
		Assert.assertEquals(updateWithoutTypeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST);
		String errorMsgWithoutType = updateWithoutTypeResponse.jsonPath().get("detail").toString();
		Assert.assertEquals(errorMsgWithoutType, "Bad Input");
		utils.logit("pass",
				"Verified IRS Admin Menu Adjuster API negative case to update string attribute with unquoted value and without type");

		// Update dictionary attribute
		Response updateDictResponse = pageObj.endpoints().irsMenuAdjusterPostApi("update dictionary", apiKey,
				dataSet.get("tacoCabanaBuuid"), "dict_attribute", "", "");
		Assert.assertEquals(updateDictResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String updatedDictValue = updateDictResponse.jsonPath().get("key1").toString();
		Assert.assertEquals(updatedDictValue, "value1");
		utils.logit("pass", "Verified IRS Admin Menu Adjuster API to update dictionary attribute");

		// Update boolean attribute with valid value and with other endpoint
		Response updateBooleanResponse = pageObj.endpoints().irsMenuAdjusterPostApi("update value other", apiKey,
				dataSet.get("tacoCabanaBuuid"), "allow_expensive_items", "True", "");
		Assert.assertEquals(updateBooleanResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String updatedBooleanValue = updateBooleanResponse.asString();
		Assert.assertEquals(updatedBooleanValue, "true");
		utils.logit("pass",
				"Verified IRS Admin Menu Adjuster API to update boolean attribute with valid value using the other endpoint");

		// Negative case: Update boolean attribute with invalid value
		Response updateBooleanInvalidResponse = pageObj.endpoints().irsMenuAdjusterPostApi("update value", apiKey,
				dataSet.get("tacoCabanaBuuid"), "allow_expensive_items", "true", "");
		Assert.assertEquals(updateBooleanInvalidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST);
		errorMsg = updateBooleanInvalidResponse.jsonPath().get("detail").toString();
		Assert.assertEquals(errorMsg, "Bad Input");
		utils.logit("pass",
				"Verified IRS Admin Menu Adjuster API negative case to update boolean attribute with invalid value");

		// Update List[str] attribute
		Response updateListResponse = pageObj.endpoints().irsMenuAdjusterPostApi("update value", apiKey,
				dataSet.get("tacoCabanaBuuid"), "excluded_categories", dataSet.get("tacoCabanaExclCategoryQuoted"), "");
		Assert.assertEquals(updateListResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String updatedList = updateListResponse.jsonPath().getList("$").toString();
		Assert.assertEquals(updatedList, dataSet.get("tacoCabanaExclCategoryUnquoted"));
		utils.logit("pass", "Verified IRS Admin Menu Adjuster API to update List[str] attribute");

		// Get attribute's value
		Response getAttributeResponse = pageObj.endpoints().irsMenuAdjusterGetApi("get value", apiKey,
				dataSet.get("tacoCabanaBuuid"), "price_cap_percentage");
		Assert.assertEquals(getAttributeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String getAttributeValue = getAttributeResponse.asString();
		Assert.assertEquals(getAttributeValue, "50");
		utils.logit("pass", "Verified IRS Admin Menu Adjuster API to get attribute's value");

		// Get non-existing attribute's value
		Response getNonExistingAttributeResponse = pageObj.endpoints().irsMenuAdjusterGetApi("get value", apiKey,
				dataSet.get("tacoCabanaBuuid"), "abc");
		Assert.assertEquals(getNonExistingAttributeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String getNonExistingAttributeValue = getNonExistingAttributeResponse.asString();
		Assert.assertEquals(getNonExistingAttributeValue, "null");
		utils.logit("pass", "Verified IRS Admin Menu Adjuster API to get non-existing attribute's value");

		// Get attribute's value using other endpoint
		Response getAttributeOtherEndpointResponse = pageObj.endpoints().irsMenuAdjusterGetApi("get value other",
				apiKey, dataSet.get("tacoCabanaBuuid"), "price_cap_percentage");
		Assert.assertEquals(getAttributeOtherEndpointResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String getAttributeOtherValue = getAttributeOtherEndpointResponse.asString();
		Assert.assertEquals(getAttributeOtherValue, "50");
		utils.logit("pass", "Verified IRS Admin Menu Adjuster other endpoint to get attribute's value");

		// Get Business Setting to verify the updated menu adjuster values
		Response settingsResponse = pageObj.endpoints().itemRecSysAdminApi("get settings", apiKey,
				dataSet.get("tacoCabanaBuuid"), "");
		Assert.assertEquals(settingsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		int priceCapPercentage = settingsResponse.jsonPath().getInt("menu_adjuster.price_cap_percentage");
		Assert.assertEquals(priceCapPercentage, 50);
		String stringAttribute = settingsResponse.jsonPath().getString("menu_adjuster.string_attribute");
		Assert.assertEquals(stringAttribute, "str_value");
		double minPrice = settingsResponse.jsonPath().getDouble("menu_adjuster.min_price");
		Assert.assertEquals(minPrice, 4.5);
		String dictAttribute = settingsResponse.jsonPath().getString("menu_adjuster.dict_attribute.key1");
		Assert.assertEquals(dictAttribute, "value1");
		boolean allowExpensiveItems = settingsResponse.jsonPath().getBoolean("menu_adjuster.allow_expensive_items");
		Assert.assertEquals(allowExpensiveItems, true);
		List<String> excludedCategories = settingsResponse.jsonPath().getList("menu_adjuster.excluded_categories");
		Assert.assertEquals(excludedCategories.toString(), dataSet.get("tacoCabanaExclCategoryUnquoted"));
		utils.logit("pass", "Verified updated menu adjuster values in Business Settings");

	}

	@Test(description = "SQ-T6836: Verify Item Rec Sys Service ping, pick system and get items APIs; "
			+ "SQ-T7107: Verify Item Rec Sys Service pick system and get items APIs with bid and without bid")
	@Owner(name = "Vaibhav Agnihotri")
	public void T6836_verifyItemRecSysServiceAPIs() throws Exception {
		// Ping
		Response pingResponse = pageObj.endpoints().itemRecSysServiceApi("ping", apiKey, dataSet.get("tacoCabanaBuuid"),
				"", "", "", "", "");
		Assert.assertEquals(pingResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String pingStatus = pingResponse.jsonPath().get("status").toString();
		Assert.assertEquals(pingStatus, "ok");
		utils.logit("pass", "Verified IRS Service Ping API");

		// Ping with full check
		Response pingFullCheckResponse = pageObj.endpoints().itemRecSysServiceApi("ping full check", apiKey,
				dataSet.get("tacoCabanaBuuid"), "", "", "", "", "");
		Assert.assertEquals(pingFullCheckResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String pingFullCheckStatus = pingFullCheckResponse.jsonPath().get("status").toString();
		Assert.assertEquals(pingFullCheckStatus, "ok");
		utils.logit("pass", "Verified IRS Service Ping API with full check");

		// Negative case: Pick system with invalid API key
		Response systemInvalidApiKeyResponse = pageObj.endpoints().itemRecSysServiceApi("pick system",
				dataSet.get("invalidApiKey"), dataSet.get("tacoCabanaBuuid"), "", dataSet.get("tacoCabanaUid"), "", "",
				"");
		Assert.assertEquals(systemInvalidApiKeyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_FORBIDDEN);
		utils.logit("pass", "Verified IRS Service Pick System API with invalid apikey");

		// Negative case: Pick system with empty API key
		Response systemEmptyApiKeyResponse = pageObj.endpoints().itemRecSysServiceApi("pick system", "",
				dataSet.get("tacoCabanaBuuid"), "", dataSet.get("tacoCabanaUid"), "", "", "");
		Assert.assertEquals(systemEmptyApiKeyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		utils.logit("pass", "Verified IRS Service Pick System API with empty apikey");

		// Pick system with valid params: buuid, uid, bid
		Response systemResponse = pageObj.endpoints().itemRecSysServiceApi("pick system", apiKey,
				dataSet.get("tacoCabanaBuuid"), "", dataSet.get("tacoCabanaUid"), "", "", dataSet.get("validBid"));
		Assert.assertEquals(systemResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String itemRecsVars = systemResponse.jsonPath().get("get_item_recs_vars").toString();
		utils.logit("pass", "Verified IRS Service Pick System API with valid params: buuid, uid, bid");

		// Get items with valid params: buuid, uid, eid, exp_var, cids, mids, bid
		Response getRecsResponse = pageObj.endpoints().itemRecSysServiceApi("get items", apiKey,
				dataSet.get("tacoCabanaBuuid"), itemRecsVars, dataSet.get("tacoCabanaUid"),
				dataSet.get("tacoCabanaCids"), dataSet.get("tacoCabanaMids"), dataSet.get("validBid"));
		Assert.assertEquals(getRecsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String itemId = utils.getJsonReponseKeyValueFromJsonArray(getRecsResponse, "items", "ordering_global_item_id",
				"");
		Assert.assertNotNull(itemId);
		utils.logit("pass",
				"Verified IRS Service Get recs API with valid params: buuid, uid, eid, exp_var, cids, mids, bid");

		// Get items when invalid menu ID is passed
		Response getRecsInvalidMidsResponse = pageObj.endpoints().itemRecSysServiceApi("get items", apiKey,
				dataSet.get("tacoCabanaBuuid"), itemRecsVars, dataSet.get("tacoCabanaUid"),
				dataSet.get("tacoCabanaCids"), "abc", "");
		Assert.assertEquals(getRecsInvalidMidsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String getRecsInvalidMidsResponseData = getRecsInvalidMidsResponse.asString();
		Assert.assertEquals(getRecsInvalidMidsResponseData, "{}");
		utils.logit("pass", "Verified IRS Service Get recs API when invalid menu ID is passed");

		// Negative case: Pick System and get items with empty buuid and valid bid
		Response systemEmptyBuuidResponse = pageObj.endpoints().itemRecSysServiceApi("pick system", apiKey, "", "",
				dataSet.get("tacoCabanaUid"), "", "", dataSet.get("validBid"));
		Assert.assertEquals(systemEmptyBuuidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST);
		String errorMsg = systemEmptyBuuidResponse.jsonPath().get("detail").toString();
		Assert.assertEquals(errorMsg, "Must provide 'buuid'");
		Response getRecsEmptyBuuidResponse = pageObj.endpoints().itemRecSysServiceApi("get items", apiKey, "",
				itemRecsVars, dataSet.get("tacoCabanaUid"), dataSet.get("tacoCabanaCids"),
				dataSet.get("tacoCabanaMids"), dataSet.get("validBid"));
		Assert.assertEquals(getRecsEmptyBuuidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST);
		errorMsg = getRecsEmptyBuuidResponse.jsonPath().get("detail").toString();
		Assert.assertEquals(errorMsg, "Must provide 'buuid'");
		utils.logit("pass", "Verified IRS Service Pick system and Get recs API with empty business UUID and valid bid");

		// Negative case: Get items with invalid API key
		Response getRecsInvalidApiKeyResponse = pageObj.endpoints().itemRecSysServiceApi("get items",
				dataSet.get("invalidApiKey"), dataSet.get("tacoCabanaBuuid"), itemRecsVars,
				dataSet.get("tacoCabanaUid"), dataSet.get("tacoCabanaCids"), dataSet.get("tacoCabanaMids"), "");
		Assert.assertEquals(getRecsInvalidApiKeyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_FORBIDDEN);
		utils.logit("pass", "Verified IRS Service Get recs API with invalid apikey");

		// Negative case: Get items with empty API key
		Response getRecsEmptyApiKeyResponse = pageObj.endpoints().itemRecSysServiceApi("get items", "",
				dataSet.get("tacoCabanaBuuid"), itemRecsVars, dataSet.get("tacoCabanaUid"),
				dataSet.get("tacoCabanaCids"), dataSet.get("tacoCabanaMids"), "");
		Assert.assertEquals(getRecsEmptyApiKeyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		utils.logit("pass", "Verified IRS Service Get recs API with empty apikey");

		// Pick system and get items with no params except buuid
		List<String> businesses = Arrays.asList("nandos", "culvers", "beefoBradys", "tacoCabana");
		for (String business : businesses) {
			Response systemOnlyBuuidResponse = pageObj.endpoints().itemRecSysServiceApi("pick system", apiKey,
					dataSet.get(business + "Buuid"), "", "", "", "", "");
			Assert.assertEquals(systemOnlyBuuidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
			itemRecsVars = systemOnlyBuuidResponse.jsonPath().get("get_item_recs_vars").toString();
			Assert.assertNotNull(itemRecsVars);
			Response getRecsOnlyBuuidResponse = pageObj.endpoints().itemRecSysServiceApi("get items", apiKey,
					dataSet.get(business + "Buuid"), "", "", "", "", "");
			Assert.assertEquals(getRecsOnlyBuuidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
			itemId = utils.getJsonReponseKeyValueFromJsonArray(getRecsOnlyBuuidResponse, "items",
					"ordering_global_item_id", "");
			Assert.assertNotNull(itemId);
			utils.logit("pass", "Verified IRS Service Pick System and Get Items API with no params except buuid for "
					+ business);
		}

		// Negative case: Pick system and get items with invalid buuid and without bid
		Response systemInvalidBuuidResponse = pageObj.endpoints().itemRecSysServiceApi("pick system", apiKey,
				dataSet.get("invalidBuuid"), "", "", "", "", "");
		Assert.assertEquals(systemInvalidBuuidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(systemInvalidBuuidResponse.asString(), "{}");
		Response getRecsInvalidBuuidResponse = pageObj.endpoints().itemRecSysServiceApi("get items", apiKey,
				dataSet.get("invalidBuuid"), "", "", dataSet.get("nandosCids"), "", "");
		Assert.assertEquals(getRecsInvalidBuuidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(getRecsInvalidBuuidResponse.asString(), "{}");
		utils.logit("pass", "Verified IRS Service Pick System and Get Items API with invalid buuid and without bid");

		// Negative case: Pick system and get items with invalid bid and valid buuid
		Response systemInvalidBidResponse = pageObj.endpoints().itemRecSysServiceApi("pick system", apiKey,
				dataSet.get("culversBuuid"), "", "", "", "", "12ab");
		Assert.assertEquals(systemInvalidBidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		String errorMsgInvalidBid = utils.getJsonReponseKeyValueFromJsonArray(systemInvalidBidResponse, "detail", "msg",
				"");
		Assert.assertEquals(errorMsgInvalidBid, "value is not a valid integer");
		Response getRecsInvalidBidResponse = pageObj.endpoints().itemRecSysServiceApi("get items", apiKey,
				dataSet.get("culversBuuid"), "", "", dataSet.get("culversCids"), "", "12ab");
		Assert.assertEquals(getRecsInvalidBidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		errorMsgInvalidBid = utils.getJsonReponseKeyValueFromJsonArray(getRecsInvalidBidResponse, "detail", "msg", "");
		Assert.assertEquals(errorMsgInvalidBid, "value is not a valid integer");
		utils.logit("pass", "Verified IRS Service Pick System and Get Items API with invalid bid and valid buuid");

	}

	@Test(description = "SQ-T6868: Verify Item Rec Sys Admin menu-adjuster-bulk GET and POST APIs")
	@Owner(name = "Vaibhav Agnihotri")
	public void T6868_verifyItemRecSysAdminMenuAdjusterBulkAPIs() throws Exception {

		// Update menu_adjuster with empty payload
		Response updateMenuAdjusterEmptyResponse = pageObj.endpoints().irsMenuAdjusterBulkPostApi(
				"menu adjuster bulk empty", apiKey, dataSet.get("tropicalSmoothieCafeBuuid"), "", "", "", "");
		Assert.assertEquals(updateMenuAdjusterEmptyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String postMenuAdjusterEmptyResponseData = updateMenuAdjusterEmptyResponse.asString();
		Assert.assertEquals(postMenuAdjusterEmptyResponseData, "{}");
		utils.logit("pass", "Verified IRS Admin Menu Adjuster Bulk POST API with empty payload");

		// Get menu_adjuster to verify the empty payload
		Response getMenuAdjusterEmptyResponse = pageObj.endpoints().irsMenuAdjusterBulkGetApi("menu adjuster bulk",
				apiKey, dataSet.get("tropicalSmoothieCafeBuuid"));
		Assert.assertEquals(getMenuAdjusterEmptyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String getMenuAdjusterEmptyResponseData = getMenuAdjusterEmptyResponse.asString();
		Assert.assertEquals(getMenuAdjusterEmptyResponseData, "{}");
		utils.logit("pass", "Verified IRS Admin Menu Adjuster Bulk GET API to ensure the empty payload stuck around");

		// Update menu_adjuster with valid details
		String attribute1Key = "min_price";
		int attribute1Value = 2;
		String attribute2Key = "allow_expensive_items";
		boolean attribute2Value = true;
		Response updateMenuAdjusterResponse = pageObj.endpoints().irsMenuAdjusterBulkPostApi("menu adjuster bulk valid",
				apiKey, dataSet.get("beefoBradysBuuid"), attribute1Key, attribute1Value, attribute2Key, attribute2Value);
		Assert.assertEquals(updateMenuAdjusterResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		int postAttribute1Value = updateMenuAdjusterResponse.jsonPath().getInt(attribute1Key);
		Assert.assertEquals(postAttribute1Value, attribute1Value);
		boolean postAttribute2Value = updateMenuAdjusterResponse.jsonPath().getBoolean(attribute2Key);
		Assert.assertEquals(postAttribute2Value, attribute2Value);
		utils.logit("pass",
				"Verified IRS Admin Menu Adjuster Bulk POST API to update menu_adjuster with valid details");

		// Get menu_adjuster to verify the updated values
		Response getMenuAdjusterResponse = pageObj.endpoints().irsMenuAdjusterBulkGetApi("menu adjuster bulk", apiKey,
				dataSet.get("beefoBradysBuuid"));
		Assert.assertEquals(getMenuAdjusterResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		int getAttribute1Value = getMenuAdjusterResponse.jsonPath().getInt(attribute1Key);
		Assert.assertEquals(getAttribute1Value, attribute1Value);
		boolean getAttribute2Value = getMenuAdjusterResponse.jsonPath().getBoolean(attribute2Key);
		Assert.assertEquals(getAttribute2Value, attribute2Value);
		utils.logit("pass", "Verified IRS Admin Menu Adjuster Bulk GET API to ensure the updated values stuck around");

		// Update menu_adjuster with valid details using other endpoint
		attribute1Value = 3;
		Response updateMenuAdjusterOtherResponse = pageObj.endpoints().irsMenuAdjusterBulkPostApi(
				"menu adjuster bulk other", apiKey, dataSet.get("beefoBradysBuuid"), attribute1Key, attribute1Value,
				attribute2Key, attribute2Value);
		Assert.assertEquals(updateMenuAdjusterOtherResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		int postOtherAttribute1Value = updateMenuAdjusterOtherResponse.jsonPath().getInt(attribute1Key);
		Assert.assertEquals(postOtherAttribute1Value, attribute1Value);
		boolean postOtherAttribute2Value = updateMenuAdjusterOtherResponse.jsonPath().getBoolean(attribute2Key);
		Assert.assertEquals(postOtherAttribute2Value, attribute2Value);
		utils.logit("pass",
				"Verified IRS Admin Menu Adjuster Bulk POST API other endpoint to update menu_adjuster with valid details");

		// Get menu_adjuster to verify the updated values using other endpoint
		Response getMenuAdjusterOtherResponse = pageObj.endpoints()
				.irsMenuAdjusterBulkGetApi("menu adjuster bulk other", apiKey, dataSet.get("beefoBradysBuuid"));
		Assert.assertEquals(getMenuAdjusterOtherResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		int getOtherAttribute1Value = getMenuAdjusterOtherResponse.jsonPath().getInt(attribute1Key);
		Assert.assertEquals(getOtherAttribute1Value, attribute1Value);
		boolean getOtherAttribute2Value = getMenuAdjusterOtherResponse.jsonPath().getBoolean(attribute2Key);
		Assert.assertEquals(getOtherAttribute2Value, attribute2Value);
		utils.logit("pass",
				"Verified IRS Admin Menu Adjuster Bulk GET API other endpoint to ensure the updated values stuck around");

		// Negative case: Update menu_adjuster with invalid API key
		Response updateMenuAdjusterInvalidApiKeyResponse = pageObj.endpoints().irsMenuAdjusterBulkPostApi(
				"menu adjuster bulk valid", "123", dataSet.get("beefoBradysBuuid"), attribute1Key, attribute1Value,
				attribute2Key, attribute2Value);
		Assert.assertEquals(updateMenuAdjusterInvalidApiKeyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_FORBIDDEN);
		utils.logit("pass", "Verified IRS Admin Menu Adjuster Bulk POST API with invalid API key");

		// Negative case: Update menu_adjuster with empty API key
		Response updateMenuAdjusterEmptyApiKeyResponse = pageObj.endpoints().irsMenuAdjusterBulkPostApi(
				"menu adjuster bulk valid", "", dataSet.get("beefoBradysBuuid"), attribute1Key, attribute1Value,
				attribute2Key, attribute2Value);
		Assert.assertEquals(updateMenuAdjusterEmptyApiKeyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		utils.logit("pass", "Verified IRS Admin Menu Adjuster Bulk POST API with empty API key");

		// Negative case: Get menu_adjuster with invalid API key
		Response getMenuAdjusterInvalidApiKeyResponse = pageObj.endpoints()
				.irsMenuAdjusterBulkGetApi("menu adjuster bulk", "123", dataSet.get("beefoBradysBuuid"));
		Assert.assertEquals(getMenuAdjusterInvalidApiKeyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_FORBIDDEN);
		utils.logit("pass", "Verified IRS Admin Menu Adjuster Bulk GET API with invalid API key");

		// Negative case: Get menu_adjuster with empty API key
		Response getMenuAdjusterEmptyApiKeyResponse = pageObj.endpoints()
				.irsMenuAdjusterBulkGetApi("menu adjuster bulk", "", dataSet.get("beefoBradysBuuid"));
		Assert.assertEquals(getMenuAdjusterEmptyApiKeyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		utils.logit("pass", "Verified IRS Admin Menu Adjuster Bulk GET API with empty API key");

		// Negative case: Update menu_adjuster with invalid JSON payload
		Response updateMenuAdjusterInvalidJsonResponse = pageObj.endpoints().irsMenuAdjusterBulkPostApi(
				"menu adjuster bulk invalid", apiKey, dataSet.get("beefoBradysBuuid"), attribute1Key, attribute1Value,
				attribute2Key, attribute2Value);
		Assert.assertEquals(updateMenuAdjusterInvalidJsonResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		String errorMsg = updateMenuAdjusterInvalidJsonResponse.jsonPath().get("detail.msg[0]").toString();
		Assert.assertTrue(errorMsg.contains("Expecting value"));
		utils.logit("pass", "Verified IRS Admin Menu Adjuster Bulk POST API negative case with invalid JSON payload");

		// Negative case: Update menu_adjuster when JSON payload is missing
		Response updateMenuAdjusterMissingJsonResponse = pageObj.endpoints().irsMenuAdjusterBulkPostApi(
				"menu adjuster bulk", apiKey, dataSet.get("beefoBradysBuuid"), "", "", "", "");
		Assert.assertEquals(updateMenuAdjusterMissingJsonResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		errorMsg = updateMenuAdjusterMissingJsonResponse.jsonPath().get("detail.msg[0]").toString();
		Assert.assertTrue(errorMsg.contains("field required"));
		utils.logit("pass",
				"Verified IRS Admin Menu Adjuster Bulk POST API negative case when JSON payload is missing");

	}

	@Test(description = "Verify Mobile API v1:- SQ-T5184: Item Recommendation System API")
	@Owner(name = "Vaibhav Agnihotri")
	public void T5184_verifyItemRecSysMobileAPI() {

		// User sign-up
		utils.logit("== Mobile API v1: User sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();
		utils.logit("pass", "API v1 User Signup call is successful");

		// Pick Item Recommendation System
		utils.logit("== Mobile API v1: Pick Item Recommendation System ==");
		Response pickSystemResponse = pageObj.endpoints().pickItemRecommendationSystemAPI(dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("bid"), token, dataSet.get("buuid"), userID);
		Assert.assertEquals(pickSystemResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 Pick Item Recommendation System");
		String system = pickSystemResponse.jsonPath().get("system").toString();
		String pickSystemTrackSegment = pickSystemResponse.jsonPath().get("track_segment").toString();
		String itemRecsVars = pickSystemResponse.jsonPath().get("get_item_recs_vars").toString();
		Assert.assertEquals(system, "par", "System did not match");
		utils.logit("pass", "API v1 Pick Item Recommendation System call is successful");

		// Get Item Recommendations
		utils.logit("== Mobile API v1: Get Item Recommendations ==");
		Response getRecsResponse = pageObj.endpoints().getItemRecommendationsAPI(dataSet.get("client"),
				dataSet.get("secret"), token, itemRecsVars, dataSet.get("cids"), dataSet.get("bid"),
				dataSet.get("buuid"), userID);
		Assert.assertEquals(getRecsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 Get Item Recommendations");
		String itemId = utils.getJsonReponseKeyValueFromJsonArray(getRecsResponse, "items", "ordering_global_item_id",
				"");
		String getItemsTrackSegment = getRecsResponse.jsonPath().get("track_segment").toString();
		Assert.assertNotNull(itemId, "Item ID is null");
		Assert.assertEquals(getItemsTrackSegment, pickSystemTrackSegment, "Track Segment did not match");
		utils.logit("pass", "API v1 Get Item Recommendations call is successful");

	}

	@Test(description = "Verify Mobile API v1 Negative Scenarios:- SQ-T5185: Item Recommendation System API")
	@Owner(name = "Vaibhav Agnihotri")
	public void T5185_verifyItemRecSysMobileAPINegative() {

		// User sign-up
		utils.logit("== Mobile API v1: User sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String invalidValue = token + "0";
		String userID = signUpResponse.jsonPath().get("id").toString();
		utils.logit("pass", "API v1 User Signup call is successful");

		// Pick Item Recommendation System with valid parameters
		utils.logit("== Mobile API v1: Pick Item Recommendation System with valid parameters ==");
		Response pickSystemResponse = pageObj.endpoints().pickItemRecommendationSystemAPI(dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("bid"), token, dataSet.get("buuid"), userID);
		Assert.assertEquals(pickSystemResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 Pick Item Recommendation System");
		String system = pickSystemResponse.jsonPath().get("system").toString();
		String itemRecsVars = pickSystemResponse.jsonPath().get("get_item_recs_vars").toString();
		Assert.assertEquals(system, "par", "System did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Pick Item Recommendation System call with valid parameters is successful");
		utils.logit("pass", "API v1 Pick Item Recommendation System call with valid parameters is successful");

		// Negative case: Pick Item Recommendation System with invalid client
		utils.logit("== Mobile API v1: Pick Item Recommendation System with invalid client ==");
		Response pickSystemInvalidClientResponse = pageObj.endpoints().pickItemRecommendationSystemAPI(invalidValue,
				dataSet.get("secret"), dataSet.get("bid"), token, dataSet.get("buuid"), userID);
		Assert.assertEquals(pickSystemInvalidClientResponse.getStatusCode(),
				ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Pick Item Recommendation System with invalid client");
		boolean isApi1IrsPickSystemInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, pickSystemInvalidClientResponse.asString());
		Assert.assertTrue(isApi1IrsPickSystemInvalidSignatureSchemaValidated,
				"API1 Pick Item Recommendation System Schema Validation failed");
		String pickSystemInvalidClientMsg = pickSystemInvalidClientResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(pickSystemInvalidClientMsg, "Invalid Signature", "Message did not match.");
		utils.logit("pass", "API v1 Pick Item Recommendation System call with invalid client is unsuccessful");

		// Negative case: Pick Item Recommendation System with invalid secret
		utils.logit("== Mobile API v1: Pick Item Recommendation System with invalid secret ==");
		Response pickSystemInvalidSecretResponse = pageObj.endpoints().pickItemRecommendationSystemAPI(
				dataSet.get("client"), invalidValue, dataSet.get("bid"), token, dataSet.get("buuid"), userID);
		Assert.assertEquals(pickSystemInvalidSecretResponse.getStatusCode(),
				ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Pick Item Recommendation System with invalid secret");
		String pickSystemInvalidSecretMsg = pickSystemInvalidSecretResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(pickSystemInvalidSecretMsg, "Invalid Signature", "Message did not match.");
		utils.logit("pass", "API v1 Pick Item Recommendation System call with invalid secret is unsuccessful");

		// Negative case: Pick Item Recommendation System with invalid user access token
		utils.logit("== Mobile API v1: Pick Item Recommendation System with invalid user access token ==");
		Response pickSystemInvalidTokenResponse = pageObj.endpoints().pickItemRecommendationSystemAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("bid"), invalidValue, dataSet.get("buuid"),
				userID);
		Assert.assertEquals(pickSystemInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 Pick Item Recommendation System with invalid user access token");
		boolean isApi1IrsPickSystemInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, pickSystemInvalidTokenResponse.asString());
		Assert.assertTrue(isApi1IrsPickSystemInvalidTokenSchemaValidated,
				"API1 Pick Item Recommendation System Schema Validation failed");
		String pickSystemInvalidTokenMsg = pickSystemInvalidTokenResponse.jsonPath().get("error").toString();
		Assert.assertEquals(pickSystemInvalidTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match.");
		utils.logit("pass",
				"API v1 Pick Item Recommendation System call with invalid user access token is unsuccessful");

		// Negative case: Get Item Recommendations with invalid client
		utils.logit("== Mobile API v1: Get Item Recommendations with invalid client ==");
		Response getRecsInvalidClientResponse = pageObj.endpoints().getItemRecommendationsAPI(invalidValue,
				dataSet.get("secret"), token, itemRecsVars, dataSet.get("cids"), dataSet.get("bid"),
				dataSet.get("buuid"), userID);
		Assert.assertEquals(getRecsInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Get Item Recommendations with invalid client");
		boolean isApi1IrsGetRecsInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, getRecsInvalidClientResponse.asString());
		Assert.assertTrue(isApi1IrsGetRecsInvalidSignatureSchemaValidated,
				"API1 Get Item Recommendations Schema Validation failed");
		String getRecsInvalidClientMsg = getRecsInvalidClientResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(getRecsInvalidClientMsg, "Invalid Signature", "Message did not match.");
		utils.logit("pass", "API v1 Get Item Recommendations call with invalid client is unsuccessful");

		// Negative case: Get Item Recommendations with invalid secret
		utils.logit("== Mobile API v1: Get Item Recommendations with invalid secret ==");
		Response getRecsInvalidSecretResponse = pageObj.endpoints().getItemRecommendationsAPI(dataSet.get("client"),
				invalidValue, token, itemRecsVars, dataSet.get("cids"), dataSet.get("bid"), dataSet.get("buuid"),
				userID);
		Assert.assertEquals(getRecsInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Get Item Recommendations with invalid secret");
		String getRecsInvalidSecretMsg = getRecsInvalidSecretResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(getRecsInvalidSecretMsg, "Invalid Signature", "Message did not match.");
		utils.logit("pass", "API v1 Get Item Recommendations call with invalid secret is unsuccessful");

		// Negative case: Get Item Recommendations with invalid user access token
		utils.logit("== Mobile API v1: Get Item Recommendations with invalid user access token ==");
		Response getRecsInvalidTokenResponse = pageObj.endpoints().getItemRecommendationsAPI(dataSet.get("client"),
				dataSet.get("secret"), invalidValue, itemRecsVars, dataSet.get("cids"), dataSet.get("bid"),
				dataSet.get("buuid"), userID);
		Assert.assertEquals(getRecsInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 Get Item Recommendations with invalid user access token");
		boolean isApi1IrsGetRecsInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, getRecsInvalidTokenResponse.asString());
		Assert.assertTrue(isApi1IrsGetRecsInvalidTokenSchemaValidated,
				"API1 Get Item Recommendations Schema Validation failed");
		String getRecsInvalidTokenMsg = getRecsInvalidTokenResponse.jsonPath().get("error").toString();
		Assert.assertEquals(getRecsInvalidTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match.");
		utils.logit("pass", "API v1 Get Item Recommendations call with invalid user access token is unsuccessful");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}
}