package com.punchh.server.utilities;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.punchh.server.apiConfig.ApiConstants;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class ApiUtils {
	static Logger logger = LogManager.getLogger(BrowserUtilities.class);

	public String getSignature(String secret, String payload) {
		String signature = null;
		try {
			Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
			SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
			sha256_HMAC.init(secret_key);
			signature = Hex.encodeHexString(sha256_HMAC.doFinal(payload.getBytes()));
		} catch (Exception e) {
			logger.error("Error in generating signature:" + e);
			TestListeners.extentTest.get().fail("Error in generating signature:" + e);
		}
		return signature;
	}

//beabe368f550bade709f8a863d988d6cb017f104b978b24c97fe2b7f6e1f43d2
	public String getSignatureSHA1(String secret, String payload) {
		String signature = null;
		try {
			// System.out.println(ApiConstants.baseUri + payload);
			Mac sha1_HMAC = Mac.getInstance("HmacSHA1");
			SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(), "HmacSHA1");
			sha1_HMAC.init(secret_key);
			signature = Hex.encodeHexString(sha1_HMAC.doFinal(payload.getBytes()));
		} catch (Exception e) {
			logger.error("Error in generating signature:" + e);
			TestListeners.extentTest.get().fail("Error in generating signature:" + e);
		}
		return signature;
	}

	public String getHash(String secret) {
		String hash = null;
		try {
			hash = DigestUtils.md5Hex(secret);
		} catch (Exception e) {
			logger.error("Error in generating hash value:" + e);
			TestListeners.extentTest.get().fail("Error in generating hash value:" + e);
		}
		return hash;
	}

	public String getSpan() {
		Date date = new Date();
		long diff = date.getTime();
		// System.out.println(diff);
		return Long.toString(diff);
	}

	public void verifyResponse(Response response, String message) {
		try {
			if (response.getStatusCode() == 200) {
				logger.info(message + " successfull, response: " + response.getStatusCode());
				TestListeners.extentTest.get().pass(message + " successful, response: " + response.getStatusCode());
			} else {
				logger.error("Failed response: " + response.getStatusCode());
				TestListeners.extentTest.get().fail(message + " Failed response: " + response.getStatusCode());
				logger.info(message + " failure response is: " + response.asString());
				TestListeners.extentTest.get().fail(message + " failure response is: " + response.asString());
			}
		} catch (Exception e) {
			logger.error("Error in verifying reponse code :" + e);
			TestListeners.extentTest.get().fail("Error in verifying reponse code :" + e);
		}
	}

	public void verifyCreateResponse(Response response, String message) {
		try {
			if (response.getStatusCode() == 201) {
				logger.info(message + " successfull, response: " + response.getStatusCode());
				TestListeners.extentTest.get().pass(message + " successful, response: " + response.getStatusCode());
			} else {
				logger.error("Failed response: " + response.getStatusCode());
				TestListeners.extentTest.get().fail(message + " Failed response: " + response.getStatusCode());
				logger.info(message + " failure response is: " + response.asString());
				TestListeners.extentTest.get().pass(message + " failure response is: " + response.asString());
			}
		} catch (Exception e) {
			logger.error("Error in verifying reponse code :" + e);
			TestListeners.extentTest.get().fail("Error in verifying reponse code :" + e);
		}
	}

	public void verifyProcessResponse(Response response, String message) {
		try {
			if (response.getStatusCode() == 202) {
				logger.info(message + " successfull, response: " + response.getStatusCode());
				TestListeners.extentTest.get().pass(message + " successful, response: " + response.getStatusCode());
			} else {
				logger.error("Failed response: " + response.getStatusCode());
				TestListeners.extentTest.get().fail(message + " Failed response: " + response.getStatusCode());
				logger.info(message + " failure response is: " + response.asString());
				TestListeners.extentTest.get().pass(message + " failure response is: " + response.asString());
			}
		} catch (Exception e) {
			logger.error("Error in verifying reponse code :" + e);
			TestListeners.extentTest.get().fail("Error in verifying reponse code :" + e);
		}
	}

	public void verifyNoContentResponse(Response response, String message) {
		try {
			if (response.getStatusCode() == 204) {
				logger.info(message + " successfull, response: " + response.getStatusCode());
				TestListeners.extentTest.get().pass(message + " successful, response: " + response.getStatusCode());
			} else {
				logger.error("Failed response: " + response.getStatusCode());
				TestListeners.extentTest.get().fail(message + " Failed response: " + response.getStatusCode());
				logger.info(message + " failure response is: " + response.asString());
				TestListeners.extentTest.get().pass(message + " failure response is: " + response.asString());
			}
		} catch (Exception e) {
			logger.error("Error in verifying reponse code :" + e);
			TestListeners.extentTest.get().fail("Error in verifying reponse code :" + e);
		}
	}

	public boolean verifyErrorMessage(String actualErrorMessage, String expectedErrorMessage) {
		if (actualErrorMessage.contains(expectedErrorMessage)) {
			return true;
		} else
			return false;
	}

	public String getOloSignature(String oloSecretKey, String payload)
			throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
		Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
		SecretKeySpec secret_key = new SecretKeySpec(oloSecretKey.getBytes(("UTF-8")), "HmacSHA256");
		sha256_HMAC.init(secret_key);
		String signature = Base64.getEncoder().encodeToString(sha256_HMAC.doFinal(payload.getBytes("UTF-8")));
		return signature;

	}

	// Returns the JSON string of a qualified item inside selected_discounts
	public static String getQualifiedItemJson(Response response, int selectedDiscountIndex, int qualifiedItemIndex) {
		ObjectMapper MAPPER = new ObjectMapper();
		String responseJson = response.asString();
		String itemJson = "";
		try {
			JsonNode root = MAPPER.readTree(responseJson);
			JsonNode selectedDiscounts = root.get("selected_discounts");
			if (selectedDiscounts == null || selectedDiscounts.isMissingNode()) {
				selectedDiscounts = root.get("success");
			}
			JsonNode discount = selectedDiscounts.get(selectedDiscountIndex);
			JsonNode qualifiedItems = discount.get("qualified_items");
			JsonNode item = qualifiedItems.get(qualifiedItemIndex);
			itemJson = MAPPER.writeValueAsString(item);
			Assert.assertNotNull(itemJson, "Qualified item JSON should not be null");
			logger.info("Extracted qualified item JSON: " + itemJson);
		} catch (Exception e) {
			logger.error("Unable to parse or extract qualified item JSON", e);
		}
		return itemJson;
	}

	// Validate items in nested JSON based on item_type and field in POS discount
	// lookup/Batch redemption response
	public void validateItems(JsonPath jp, String parentNode, String itemType, String field, List<?> expectedValues) {

		// Read parent nodes (selected_discounts or success)
		List<?> parentListRaw = jp.getList(parentNode);

		if (parentListRaw == null) {
			Assert.fail("Parent node '" + parentNode + "' not found in response.");
		}

		// Convert parent list to List<Map<String,Object>> safely
		List<Map<String, Object>> parentList = new ArrayList<>();
		for (Object o : parentListRaw) {
			if (o instanceof Map) {
				parentList.add((Map<String, Object>) o);
			}
		}

		List<Object> actualValues = new ArrayList<>();

		for (Map<String, Object> parentObj : parentList) {

			Object qItemsObj = parentObj.get("qualified_items");

			List<Map<String, Object>> qualifiedItems = new ArrayList<>();

			if (qItemsObj instanceof List) {
				List<?> rawList = (List<?>) qItemsObj;
				for (Object item : rawList) {
					if (item instanceof Map) {
						qualifiedItems.add((Map<String, Object>) item);
					}
				}
			}

			for (Map<String, Object> item : qualifiedItems) {
				Object type = item.get("item_type");
				if (itemType.equals(String.valueOf(type))) {
					actualValues.add(item.get(field));
				}
			}
		}

		// Normalize → avoids double comparison 8.33 vs 8.330
		List<String> actualNorm = new ArrayList<>();
		for (Object val : actualValues) {
			actualNorm.add(String.valueOf(val));
		}

		List<String> expectedNorm = new ArrayList<>();
		for (Object val : expectedValues) {
			expectedNorm.add(String.valueOf(val));
		}

		Assert.assertEquals(actualNorm, expectedNorm,
				"Validation failed for item_type=" + itemType + " field=" + field);

		String msg = "Validated " + itemType + " " + field + ": " + expectedValues;
		logger.info(msg);
		TestListeners.extentTest.get().info(msg);
	}

}
