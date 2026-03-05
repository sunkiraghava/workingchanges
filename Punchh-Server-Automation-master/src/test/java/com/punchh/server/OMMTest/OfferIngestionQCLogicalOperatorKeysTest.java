package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.ApiPayloadObj;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class OfferIngestionQCLogicalOperatorKeysTest {
	private static final Logger logger = LogManager.getLogger(OfferIngestionQCLogicalOperatorKeysTest.class);
	private static final String SPECIAL_CHARS = "~`!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?₹€£©®™✓★♦●■◆◇¤§¶•";
	private static final String QEO = "qualifying_expressions_operator";
	private static final String IFEO = "item_filter_expressions_operator";
	private static final String DES = "discount_evaluation_strategy";

	public WebDriver driver;
	private PageObj pageObj;
	private String env;
	private String run = "ui";
	private static Map<String, String> dataSet;
	Utilities utils;
	private ApiPayloadObj apipayloadObj;
	String qcExternalID;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) throws JsonProcessingException {
		String sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		apipayloadObj = new ApiPayloadObj();
		utils = new Utilities();
	}

	private Response createQC(String qcName, String qeo, String ifeo, String des) throws JsonProcessingException {

		// Prepare QC payload
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcName)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.setQualifyingExpressionsOperator(qeo).setItemFilterExpressionsOperator(ifeo)
				.setDiscountEvaluationStrategy(des).build();

		// Create QC via API
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "QC creation failed via API");
		utils.logInfo("QC creation API successful");
		return qcResponse;
	}

	// Generic helper for create/update/negative flows
	private void verifyCreateUpdateFlow(String key, Object createVal, String expectedAfterCreate, Object updateVal,
			String expectedAfterUpdate, boolean expectWarningOnCreate, String expectedWarningValOnCreate,
			boolean expectWarningOnUpdate, String expectedWarningValOnUpdate) throws JsonProcessingException {

		// Prepare unique QC name
		String qcname = "QC_" + key + "_" + Utilities.getTimestamp();
		// Prepare values for all keys
		String qeo = QEO.equals(key) ? (createVal == null ? null : createVal.toString()) : "all";
		String ifeo = IFEO.equals(key) ? (createVal == null ? null : createVal.toString())
				: (DES.equals(key) ? "any" : "all");
		String des = DES.equals(key) ? (createVal == null ? null : createVal.toString()) : "min";

		// --- CREATE QC ---
		Response qcResponse = createQC(qcname, qeo, ifeo, des);
		String qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		// Verify external ID is not null
		Assert.assertNotNull(qcExternalID, "QC External ID is null!");
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty");
		utils.logInfo("QC created with External ID: " + qcExternalID);

		// --- Validate warning on create ---
		if (expectWarningOnCreate) {
			String warningMessage = qcResponse.jsonPath().getString("results[0].warnings." + key + "[0].message");
			String warningItemValue = qcResponse.jsonPath().getString("results[0].warnings." + key + "[0].item." + key);
			// Validate warning message
			Assert.assertNotNull(warningMessage, "Warning message not present for " + key + " on create");
			utils.logInfo("Warning message on create for " + key + ": " + warningMessage);
			// Validate warning item value
			Assert.assertEquals(warningItemValue, expectedWarningValOnCreate,
					"Warning item value mismatch for " + key + " on create");
			utils.logInfo("Warning item value on create for " + key + ": " + warningItemValue);
		}

		// --- VERIFY GET QC AFTER CREATE ---
		Response getQCResponse = pageObj.endpoints().getQualificationListUsingAPI(dataSet.get("apiKey"), qcname, "1",
				"10");
		Assert.assertEquals(getQCResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Unable to fetch the list of QC via GET QC API");
		utils.logInfo("GET QC API after create successful");

		// Verify value after create
		String actualAfterCreate = getQCResponse.jsonPath().getString("data[0]." + key);
		Assert.assertEquals(actualAfterCreate, expectedAfterCreate, key + " mismatch after create");
		utils.logPass("Verified " + key + " after create: Expected = " + expectedAfterCreate + ", Actual = "
				+ actualAfterCreate);

		// --- UPDATE QC (if applicable) ---
		if (updateVal != null) {
			String updQeo = QEO.equals(key) ? (updateVal == null ? null : updateVal.toString()) : qeo;
			String updIfeo = IFEO.equals(key) ? (updateVal == null ? null : updateVal.toString()) : ifeo;
			String updDes = DES.equals(key) ? (updateVal == null ? null : updateVal.toString()) : des;
			String updatePayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
					.setExternalId(qcExternalID).setPercentageOfProcessedAmount(100)
					.setQCProcessingFunction("sum_amounts").setQualifyingExpressionsOperator(updQeo)
					.setItemFilterExpressionsOperator(updIfeo).setDiscountEvaluationStrategy(updDes).build();

			// Update QC via API
			Response updateResponse = pageObj.endpoints().updateQC(updatePayload, dataSet.get("apiKey"));
			Assert.assertEquals(updateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"QC update failed via API");
			utils.logInfo("QC updated with External ID: " + qcExternalID);

			// --- Validate warning on update ---
			if (expectWarningOnUpdate) {
				String warningMessage = updateResponse.jsonPath()
						.getString("results[0].warnings." + key + "[0].message");
				String warningItemValue = updateResponse.jsonPath()
						.getString("results[0].warnings." + key + "[0].item." + key);
				// Validate warning message
				Assert.assertNotNull(warningMessage, "Warning message not present for " + key + " on update");
				utils.logInfo("Warning message on update for " + key + ": " + warningMessage);
				// Validate warning item value
				Assert.assertEquals(warningItemValue, expectedWarningValOnUpdate,
						"Warning item value mismatch for " + key + " on update");
				utils.logInfo("Warning item value on update for " + key + ": " + warningItemValue);
			}

			// --- VERIFY GET QC AFTER UPDATE ---
			Response getQCResponseAfterUpdate = pageObj.endpoints().getQualificationListUsingAPI(dataSet.get("apiKey"),
					qcname, "1", "10");
			// Verify get QC response
			Assert.assertEquals(getQCResponseAfterUpdate.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Unable to fetch the list of QC via GET QC API after update");
			utils.logInfo("GET QC API after update successful");

			// Verify value after update
			String actualAfterUpdate = getQCResponseAfterUpdate.jsonPath().getString("data[0]." + key);
			Assert.assertEquals(actualAfterUpdate, expectedAfterUpdate, key + " mismatch after update");
			utils.logPass("Verified " + key + " after update: Expected = " + expectedAfterUpdate + ", Actual = "
					+ actualAfterUpdate);
		}
	}

	@DataProvider(name = "qcOperatorScenarios")
	public Object[][] qcOperatorScenarios() {
		return new Object[][] {
				// QEO scenarios
				// key, createVal, expectedAfterCreate, updateVal, expectedAfterUpdate,
				// expectWarningOnCreate, expectedWarningValOnCreate, expectWarningOnUpdate,
				// expectedWarningValOnUpdate
				{ QEO, "all", "all", "any", "any", false, null, false, null },
				{ QEO, "all", "all", "invalid", "all", false, null, true, "invalid" },
				{ QEO, "invalid", "all", null, null, true, "invalid", false, null },
				{ QEO, null, "all", null, null, true, null, false, null },
				{ QEO, "true", "all", null, null, true, "true", false, null },
				{ QEO, "false", "all", null, null, true, "false", false, null },
				{ QEO, SPECIAL_CHARS, "all", null, null, true, SPECIAL_CHARS, false, null },
				// IFEO scenarios
				{ IFEO, "all", "all", "any", "any", false, null, false, null },
				{ IFEO, "all", "all", "invalid", "all", false, null, true, "invalid" },
				{ IFEO, "invalid", "all", null, null, true, "invalid", false, null },
				{ IFEO, null, "all", null, null, true, null, false, null },
				{ IFEO, "true", "all", null, null, true, "true", false, null },
				{ IFEO, "false", "all", null, null, true, "false", false, null },
				{ IFEO, SPECIAL_CHARS, "all", null, null, true, SPECIAL_CHARS, false, null },
				// DES scenarios (note: IFEO is set to "any" in helper for DES)
				{ DES, "min", "min", "max", "max", false, null, false, null },
				{ DES, "min", "min", "invalid", "min", false, null, true, "invalid" },
				{ DES, "invalid", "min", null, null, true, "invalid", false, null },
				{ DES, null, "min", null, null, true, null, false, null },
				{ DES, "true", "min", null, null, true, "true", false, null },
				{ DES, "false", "min", null, null, true, "false", false, null },
				{ DES, SPECIAL_CHARS, "min", null, null, true, SPECIAL_CHARS, false, null } };

	}

	@Test(dataProvider = "qcOperatorScenarios", description = "SQ-T7326 Create QC with qualifying_expressions_operator = All"
			+ "SQ-T7327 Create QC with qualifying_expressions_operator = Any"
			+ "SQ-T7328 Negative Test: Create QC with invalid qualifying_expressions_operator value"
			+ "SQ-T7329 Update QC to change qualifying_expressions_operator from \"All\" to \"Any\" or vice versa"
			+ "SQ-T7332 Negative Test: qualifying_expressions_operator with null value"
			+ "SQ-T7345 Create QC with empty qualifying_expressions_operator value"
			+ "SQ-T7346 Update QC to change item_filter_expressions_operator from \"All\" to \"Any\" or vice versa "
			+ "SQ-T7347 Update QC to change discount_evaluation_strategy from \"min\" to \"max\" or vice versa"
			+ "SQ-T7348 Negative test: Update QC to change discount_evaluation_strategy to null value"
			+ "SQ-T7333 Create QC with logical_operator_line_item_filter = \"All\" (Default AND Logic)"
			+ "SQ-T7334 Create QC with logical_operator_line_item_filter = \"Any\" (OR Logic)"
			+ "SQ-T7335 Create QC with logical_operator_line_item_filter = \"Any\" and discount_evaluation_strategy = \"max\""
			+ "SQ-T7336 Create QC with logical_operator_line_item_filter = \"Any\" and discount_evaluation_strategy omitted"
			+ "SQ-T7337 Create QC with invalid logical_operator_line_item_filter value"
			+ "SQ-T7338 Update QC to change logical_operator_line_item_filter from \"All\" to \"Any\""
			+ "SQ-T7342 Negative Test: item_filter_expressions_operator with invalid values"
			+ "SQ-T7343 Validate Warning Message for Incorrect discount_evaluation_strategy"
			+ "SQ-T7344 Negative Test: logical_operator_line_item_filter with missing value"
			+ "SQ-T7341 Validate discount_evaluation_strategy,qualifying_expressions_operator and item_filter_expressions_operator values in Get QC API responses", groups = {
					"regression" })
	@Owner(name = "Rahul Garg")
	public void OMM1534_OMM1538_testQcOperatorScenarios(String key, Object createVal, String expectedAfterCreate,
			Object updateVal, String expectedAfterUpdate, boolean expectWarningOnCreate,
			String expectedWarningValOnCreate, boolean expectWarningOnUpdate, String expectedWarningValOnUpdate)
			throws JsonProcessingException {

		// Execute the generic create/update flow verification
		verifyCreateUpdateFlow(key, createVal, expectedAfterCreate, updateVal, expectedAfterUpdate,
				expectWarningOnCreate, expectedWarningValOnCreate, expectWarningOnUpdate, expectedWarningValOnUpdate);
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {

		utils.deleteLISQCRedeemable(env, null, qcExternalID, null);
		pageObj.utils().clearDataSet(dataSet);
		utils.logInfo("Data set cleared");
	}

}