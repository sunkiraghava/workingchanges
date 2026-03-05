package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apimodel.qc.QCData;
import com.punchh.server.apimodel.redeemable.RedeemableReceiptRule;
import com.punchh.server.pages.ApiPayloadObj;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class OfferIngestionRedeemableLogicalOperatorKeysTest {
	private static final Logger logger = LogManager.getLogger(OfferIngestionRedeemableLogicalOperatorKeysTest.class);

	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private final String run = "ui";
	private static Map<String, String> dataSet;
	private Utilities utils;
	private ApiPayloadObj apipayloadObj;
	private String qcExternalID;
	private String redeemableId;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) throws JsonProcessingException {
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		apipayloadObj = new ApiPayloadObj();
		utils = new Utilities();
	}

	@DataProvider(name = "redeemableLogicalOperatorScenarios")
	public Object[][] redeemableLogicalOperatorScenarios() {
		String specialChars = "~`!@#$%^&*()-_=+[{]}\\\\|;:'\\\",<.>/?₹€£©®™✓★♦●■◆◇¤§¶•";
		return new Object[][] {
				// key, createVal, expectCreateWarn, expectedCreateWarnVal, updateVal,
				// expectUpdateWarn, expectedUpdateWarnVal
				{ "qualifying_expressions_operator", "all", false, null, "invalid", true, "invalid" },
				{ "qualifying_expressions_operator", "invalid", true, "invalid", "all", false, null },
				{ "item_filter_expressions_operator", "all", false, null, specialChars, true, specialChars },
				{ "item_filter_expressions_operator", specialChars, true, specialChars, "all", false, null },
				{ "discount_evaluation_strategy", "min", false, null, true, true, "true" },
				{ "discount_evaluation_strategy", true, true, "true", "min", false, null } };
	}

	@Test(dataProvider = "redeemableLogicalOperatorScenarios", description = "SQ-T7339 Create Redeemable (Inline QC) with item_filter_expressions_operator = \"Any\""
			+ "SQ-T7340 Update Redeemable (Inline QC) to change item_filter_expressions_operator"
			+ "SQ-T7349 Create redeemable with QC by passing new keys"
			+ "SQ-T7350 Create redeemable with QC by passing invalid value of new keys"
			+ "SQ-T7330 Create Redeemable with inline QC and logical_operator_item_qualifier = \"Any\""
			+ "SQ-T7331 Negative test: Update redeemable to change discount_evaluation_strategy/qualifying_expressions_operator/item_filter_expressions_operator to null/invalid value"
			+ "SQ-T7351 Update Redeemable to change qualifying_expressions_operator from \"All\" to \"Any\" or vice versa"
			+ "SQ-T7352 Update Redeemable to change item_filter_expressions_operator from \"All\" to \"Any\" or vice versa"
			+ "SQ-T7353 Update Redeemable to change discount_evaluation_strategy from \"min\" to \"max\" or vice versa", groups = {
					"regression" })
	@Owner(name = "Rahul Garg")
	public void OMM1534_OMM1538_testCreateGetUpdateRedeemableLogicalOperators(String key, Object createVal,
			boolean expectCreateWarn, String expectedCreateWarnVal, Object updateVal, boolean expectUpdateWarn,
			String expectedUpdateWarnVal) throws JsonProcessingException {

		// --- 1. CREATE FLOW ---
		String qeoCreate = getOperatorValue("qualifying_expressions_operator", key, createVal, "all");
		String ifeoCreate = getOperatorValue("item_filter_expressions_operator", key, createVal, "all");
		String desCreate = getOperatorValue("discount_evaluation_strategy", key, createVal, "min");
		String redeemableName = "Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);

		// Build Inline QC Data
		QCData inlineQCData = apipayloadObj.qualificationCriteriaBuilder().setPercentageOfProcessedAmount(10)
				.setQCProcessingFunction("sum_amounts").setQualifyingExpressionsOperator(qeoCreate)
				.setItemFilterExpressionsOperator(ifeoCreate).setDiscountEvaluationStrategy(desCreate).buildQCData();

		// Convert QCData to Map
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> criterionMap = mapper.convertValue(inlineQCData, Map.class);

		// Build Receipt Rule with Inline QC Data
		RedeemableReceiptRule receiptRule = RedeemableReceiptRule.builder().qualifier_type("new")
				.redeeming_criterion(criterionMap).build();

		// Build Create Redeemable Payload
		String createPayload = apipayloadObj.redeemableBuilder().startNewData().setName(redeemableName)
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setReceiptRule(receiptRule).addCurrentData().build();

		// Create Redeemable API Call
		Response createResponse = pageObj.endpoints().createRedeemable(createPayload, dataSet.get("apiKey"));
		Assert.assertEquals(createResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Create Redeemable API failed");

		// --- 1a. Warning Assertion after Create ---
		assertWarning(createResponse, key, expectCreateWarn, expectedCreateWarnVal, "create");

		// Extract redeemable ID for further operations
		redeemableId = createResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableId, "Redeemable ID is null after create!");
		Assert.assertFalse(redeemableId.isEmpty(), "Redeemable External ID is empty");

		// --- 2. GET FLOW ---
		Response getResponse = pageObj.endpoints().getLisOfRedeemableUsingApiWithPagePerItem(dataSet.get("apiKey"),
				"query", redeemableName, "1", "5");
		Assert.assertEquals(getResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Get Redeemable API failed");

		// Save the value before update for later comparison
		String actualValBeforeUpdate = extractFieldValue(getResponse, key);
		String actualIfeoBeforeUpdate = "discount_evaluation_strategy".equals(key)
				? extractFieldValue(getResponse, "item_filter_expressions_operator")
				: null;

		// --- 2a. Field Value Assertion after Create ---
		if ("discount_evaluation_strategy".equals(key)) {
			String actualGetDes = extractFieldValue(getResponse, "discount_evaluation_strategy");
			assertDiscountEvaluationStrategyGetValue(desCreate, ifeoCreate, actualGetDes);
		} else {
			String actualValAfterCreate = extractFieldValue(getResponse, key);
			String expectedValAfterCreate = (createVal == null || expectCreateWarn) ? "all" : createVal.toString();
			Assert.assertEquals(actualValAfterCreate, expectedValAfterCreate, key + " mismatch after create");
		}

		// get qc external id for cleanup
		qcExternalID = getResponse.jsonPath().getString("data[0].receipt_rule.redeeming_criterion.external_id");

		// --- 3. UPDATE FLOW ---
		String qeoUpdate = getOperatorValue("qualifying_expressions_operator", key, updateVal, qeoCreate);
		String ifeoUpdate = getOperatorValue("item_filter_expressions_operator", key, updateVal, ifeoCreate);
		String desUpdate = getOperatorValue("discount_evaluation_strategy", key, updateVal, desCreate);

		QCData updateInlineQCData = apipayloadObj.qualificationCriteriaBuilder().setName(redeemableName)
				.setPercentageOfProcessedAmount(10).setQCProcessingFunction("sum_amounts")
				.setQualifyingExpressionsOperator(qeoUpdate).setItemFilterExpressionsOperator(ifeoUpdate)
				.setDiscountEvaluationStrategy(desUpdate).buildQCData();

		criterionMap = mapper.convertValue(updateInlineQCData, Map.class);
		RedeemableReceiptRule updateReceiptRule = RedeemableReceiptRule.builder().qualifier_type("new")
				.redeeming_criterion(criterionMap).build();

		String updatePayload = apipayloadObj.redeemableBuilder().startNewData().setExternalId(redeemableId)
				.setReceiptRule(updateReceiptRule).addCurrentData().build();

		Response updateResponse = pageObj.endpoints().updateRedeemable(updatePayload, dataSet.get("apiKey"));
		Assert.assertEquals(updateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Update Redeemable API failed");

		boolean warningOnUpdate = assertWarning(updateResponse, key, expectUpdateWarn, expectedUpdateWarnVal, "update");

		// --- 4. GET FLOW AFTER UPDATE ---
		Response getResponseAfterUpdate = pageObj.endpoints()
				.getLisOfRedeemableUsingApiWithPagePerItem(dataSet.get("apiKey"), "query", redeemableName, "1", "5");
		Assert.assertEquals(getResponseAfterUpdate.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Get Redeemable API failed after update");

		if ("discount_evaluation_strategy".equals(key)) {
			String actualGetDesAfterUpdate = extractFieldValue(getResponseAfterUpdate, "discount_evaluation_strategy");
			if (warningOnUpdate) {
				// Value should remain unchanged
				assertDiscountEvaluationStrategyGetValue(actualValBeforeUpdate, actualIfeoBeforeUpdate,
						actualGetDesAfterUpdate);
			} else {
				assertDiscountEvaluationStrategyGetValue(desUpdate, ifeoUpdate, actualGetDesAfterUpdate);
			}
		} else {
			String actualValAfterUpdate = extractFieldValue(getResponseAfterUpdate, key);
			if (warningOnUpdate) {
				Assert.assertEquals(actualValAfterUpdate, actualValBeforeUpdate,
						key + " should remain unchanged after warning on update");
			} else {
				String expectedValAfterUpdate = (updateVal == null || expectUpdateWarn) ? "all" : updateVal.toString();
				Assert.assertEquals(actualValAfterUpdate, expectedValAfterUpdate, key + " mismatch after update");
			}
		}
	}

	// --- Helper Methods ---

	public void assertRedeemableWarning(Response response, String key, String expectedValue) {
		String basePath = "results[0].warnings.receipt_rule.redeeming_criterion." + key + "[0]";
		String warningMessage = response.jsonPath().getString(basePath + ".message");
		String warningValue = response.jsonPath().getString(basePath + ".item." + key);
		Assert.assertNotNull(warningMessage, "Warning message not present for " + key);
		Assert.assertEquals(warningValue, expectedValue, "Warning value mismatch for " + key);
	}

	public String extractStringFromJson(Object obj) {
		if (obj == null)
			return null;
		if (obj instanceof List) {
			List<?> list = (List<?>) obj;
			if (list.isEmpty() || list.get(0) == null)
				return null;
			return list.get(0).toString();
		}
		return obj.toString();
	}

	public void assertDiscountEvaluationStrategyGetValue(String expectedDes, String expectedIfeo, String actualGetDes) {
		List<String> validValues = Arrays.asList("min", "max");
		// If ifeo is "all" or des is invalid/null, expect null
		if ("all".equals(expectedIfeo) || expectedDes == null || !validValues.contains(expectedDes)) {
			Assert.assertTrue(actualGetDes == null || "null".equals(actualGetDes),
					"Expected null for discount_evaluation_strategy when ifeo=all or des is invalid/null, but found: "
							+ actualGetDes);
			return;
		}
		// Otherwise, value should match the valid value
		Assert.assertEquals(actualGetDes, expectedDes,
				"Unexpected value for discount_evaluation_strategy: " + actualGetDes);
	}

	private String getOperatorValue(String operatorKey, String key, Object val, String defaultVal) {
		return operatorKey.equals(key) ? (val == null ? null : val.toString()) : defaultVal;
	}

	private String extractFieldValue(Response response, String fieldKey) {
		return extractStringFromJson(response.jsonPath().get("data.receipt_rule.redeeming_criterion." + fieldKey));
	}

	private boolean assertWarning(Response response, String key, boolean expectWarn, String expectedWarnVal,
			String opType) {
		String basePath = "results[0].warnings.receipt_rule.redeeming_criterion." + key;
		if (expectWarn) {
			assertRedeemableWarning(response, key, expectedWarnVal);
			return true;
		} else {
			Assert.assertNull(response.jsonPath().get(basePath), "Unexpected warning for " + key + " in " + opType);
			return false;
		}
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		utils.deleteLISQCRedeemable(env, null, qcExternalID, redeemableId);
		pageObj.utils().clearDataSet(dataSet);
		utils.logInfo("Data set cleared");
	}

}