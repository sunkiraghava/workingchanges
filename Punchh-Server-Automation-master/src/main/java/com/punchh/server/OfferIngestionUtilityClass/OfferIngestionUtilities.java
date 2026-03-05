package com.punchh.server.OfferIngestionUtilityClass;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

// Utility class for Offer Ingestion related operations

@Listeners(TestListeners.class)
public class OfferIngestionUtilities {
	static Logger logger = LogManager.getLogger(OfferIngestionUtilities.class);
	private Utilities utils;
	private PageObj pageObj;

	// Queries
	public final static String businessPreferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id = $id;";
	public final static String discountBasketItemForUserDiscountQuery = "SELECT * FROM discount_basket_items WHERE discount_id = $discount_id AND user_id = $user_id;";
	public final static String discountBasketItemForUserQuery = "SELECT * FROM discount_basket_items WHERE user_id = $user_id;";
	public final static String getRedeemableIdQuery = "SELECT id FROM redeemables WHERE uuid = '$external_id';";
	public final static String currentRedeemableCriteriaIdQuery = "SELECT redeeming_criterion_id FROM redeemables WHERE id = '$redeemable_id';";
	public final static String idFromQCQuery = "SELECT id FROM qualification_criteria WHERE external_id = '$external_id';";
	public final static String getUuidFromDiscountBasketsQuery = "SELECT uuid FROM `discount_baskets` WHERE `user_id`= '$user_id'";
	public final static String updateRedeemableCriteriaColQuery = "UPDATE redeemables SET redeeming_criterion_id = '$redeeming_criterion_id' WHERE id = '$redeemable_id'";
	public final static String getExtUidLockedAtDiscountBasketsQuery = "SELECT external_uid, locked_at from discount_baskets where user_id = '$user_id'";
	public final static String getCheckinDetailsForUserQuery = "SELECT * FROM checkins WHERE user_id = $user_id AND checkin_type = '$checkin_type' AND location_id = $location_id;";

	public OfferIngestionUtilities(WebDriver driver) {
		utils = new Utilities(driver);
		pageObj = new PageObj();
	}

	// Verify Auto-Select API response and its DB entry based on discount type
	public void verifyDiscountBasketItemDB(Response response, String userID, String discountType,
			boolean isDiscountAdded, String env) throws Exception {
		String autoSelectDiscountId, query;
		List<Map<String, String>> values;
		if (isDiscountAdded) { // When discount is added and type is discount_amount or fuel_reward
			if (discountType.equals("discount_amount") || discountType.equals("fuel_reward")) {
				autoSelectDiscountId = response.jsonPath().getString("discount_basket_items[0].discount_id");
				Assert.assertNull(autoSelectDiscountId, "Discount ID not null in Auto Select API response");
				String autoSelectDiscountBasketItemId = response.jsonPath()
						.getString("discount_basket_items[0].discount_basket_item_id");
				// Verify the entry in discount_basket_items table
				query = discountBasketItemForUserQuery.replace("$user_id", userID);
				values = DBUtils.executeQueryAndGetMultipleColumns(env, query, new String[] { "id", "discount_type" });
				Assert.assertEquals(values.get(0).get("discount_type"), discountType,
						"discount_type mismatch in discount_basket_items table");
				Assert.assertEquals(values.get(0).get("id"), autoSelectDiscountBasketItemId,
						"id mismatch in discount_basket_items table");
			} else { // When discount is added and type is reward or subscription
				autoSelectDiscountId = Utilities.getJsonReponseKeyValueFromJsonArrayForUnknownKeyValuePair(response,
						"discount_basket_items", "discount_type", discountType, "discount_id");
				Assert.assertTrue(!autoSelectDiscountId.isEmpty(), "Discount ID not found in Auto Select API response");
				// Verify the entry in discount_basket_items table
				query = discountBasketItemForUserDiscountQuery.replace("$discount_id", autoSelectDiscountId)
						.replace("$user_id", userID);
				values = DBUtils.executeQueryAndGetMultipleColumns(env, query,
						new String[] { "item_id", "discount_type" });
				Assert.assertEquals(values.get(0).get("discount_type"), discountType,
						"discount_type mismatch in discount_basket_items table");
				Assert.assertEquals(values.get(0).get("item_id"), autoSelectDiscountId,
						"item_id mismatch in discount_basket_items table");
			}
			utils.logit("Discount got added and found discount_basket_items entry for user");
		} else { // When discount is NOT added
			autoSelectDiscountId = response.jsonPath().get("discount_basket_items").toString();
			Assert.assertEquals(autoSelectDiscountId, "[]",
					"Discount Basket Items is not empty in Auto Select API response for user");
			// Verify NO entry in discount_basket_items table
			query = discountBasketItemForUserQuery.replace("$user_id", userID);
			values = DBUtils.executeQueryAndGetMultipleColumns(env, query, new String[] { "item_id", "discount_type" });
			Assert.assertTrue(values.isEmpty(), "Entry found in discount_basket_items table for user");
			utils.logit("Discount not added and no discount_basket_items entry found for user");
		}
	}

	// Gets value of enable_decoupled_redemption_engine flag from businesses.preferences
	public boolean getDecoupledRedemptionEngineFlagStatus(String env, String businessID) throws Exception {
		String query = businessPreferenceQuery.replace("$id", businessID);
		String preferences = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		List<String> decoupledRedemptionEngineValue = Utilities.getPreferencesKeyValue(preferences,
				"enable_decoupled_redemption_engine");
		Boolean isDecoupledRedemptionEngineEnabled = Boolean.parseBoolean(decoupledRedemptionEngineValue.get(0));
		utils.logInfo("Value of enable_decoupled_redemption_engine is " + isDecoupledRedemptionEngineEnabled);
		return isDecoupledRedemptionEngineEnabled;
	}

	// Sign up a new user and return user info
	public Map<String, String> signUpUser(String email, String client, String secret) {
		Response response = pageObj.endpoints().Api1UserSignUp(email, client, secret);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "User signup failed");

		String token = response.jsonPath().getString("auth_token.token");
		Assert.assertNotNull(token, "Missing 'auth_token.token' in signup response");

		String authToken = response.jsonPath().getString("authentication_token");
		Assert.assertNotNull(authToken, "Missing 'authentication_token' in signup response");

		String userId = response.jsonPath().getString("id");
		Assert.assertNotNull(userId, "Missing 'id' in signup response");

		Map<String, String> userInfo = new HashMap<>();
		userInfo.put("email", email);
		userInfo.put("token", token);
		userInfo.put("authToken", authToken);
		userInfo.put("userID", userId);

		return userInfo;
	}
}