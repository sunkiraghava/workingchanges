package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.punchh.server.LisCreationUtilityClasses.BaseItemClauses;
import com.punchh.server.LisCreationUtilityClasses.ModifiersItemsClauses;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.utilities.*;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import com.punchh.server.pages.PageObj;
import org.testng.annotations.Test;

/*
 * @Author : Vansham Mishra
 */
@Listeners(TestListeners.class)

public class ValidationsForProcessingFunctions {

    static Logger logger = LogManager.getLogger(ValidationsForProcessingFunctions.class);
    public WebDriver driver;
    private String userEmail;
    private ApiUtils apiUtils;
    private Properties prop;
    private PageObj pageObj;
    private String sTCName;
    private String env, run = "ui";
    private String baseUrl;
    private static Map<String, String> dataSet;
    private boolean GlobalBenefitRedemptionThrottlingToggle;
    private String endDateTime;
    private Utilities utils;
    public List<BaseItemClauses> listBaseItemClauses = new ArrayList();
    public List<ModifiersItemsClauses> listModifiresItemClauses = new ArrayList();
    String lisDeleteBaseQuery =
            "Delete from line_item_selectors where external_id = '${externalID}' and business_id='${businessID}'";
    String QCDeleteBaseQuery =
            "Delete from qualification_criteria where external_id = '${externalID2}' and business_id='${businessID}'";
    String deleteRedeemableQuery =
            "delete from redeemables where uuid ='$redeemableExternalID' and business_id ='${businessID}'";
    String getQC_idString =
            "select id from qualification_criteria where external_id = '$qcExternalID' and business_id = '${businessID}'";
    String deleteQCFromQualification_criteriaQuery =
            "delete from qualification_criteria where external_id = '$qcExternalID' and business_id = '${businessID}'";
    String deleteQCQueryFromQualifying_expressionsQuery =
            "delete from qualifying_expressions where qualification_criterion_id ='$qcID'";
    @BeforeMethod
    public void setUp(Method method) {
        prop = Utilities.loadPropertiesFile("config.properties");
        driver = new BrowserUtilities().launchBrowser();
        sTCName = method.getName();
        pageObj = new PageObj(driver);
        env = pageObj.getEnvDetails().setEnv();
        baseUrl = pageObj.getEnvDetails().setBaseUrl();
        dataSet = new ConcurrentHashMap<>();
        pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
        dataSet = pageObj.readData().readTestData;
        pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
                pageObj.readData().getJsonFilePath(run , env , "Secrets"), dataSet.get("slug"));
        dataSet.putAll(pageObj.readData().readTestData);
        logger.info(sTCName + " ==>" + dataSet);
        GlobalBenefitRedemptionThrottlingToggle = false;
        endDateTime = CreateDateTime.getTomorrowDate() + " 10:00 AM";
        utils = new Utilities(driver);
    }
    @Test(description = "SQ-T5592 Validate the validations for Target bundle price processing function", priority = 2)
    public void T5592_ValidateValidationsForTargetBundlePriceProcessingFunction() throws Exception {

        Map<String, String> map = new HashMap<String, String>();
        String external_id = "";
        String adminKey = dataSet.get("apiKey");
        String lisName3 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
        String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
        String QCName2 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
        String QCName3 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
        String QCName4 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
        String QCName5 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
        String QCName6 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
        String processingFunction2 = "bundle_price_target";
        map.put("receipt_qualifiers", "[{\"attribute\":\"amount\",\"operator\":\"==\",\"value\":\"10\"}]");
        String baseItemClauses1 = dataSet.get("baseItemClauses1");
        String modifiersItemClauses1 = dataSet.get("modifierItemClauses");
        listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

        // Add Modifiers clauses
        listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
                .getListOfModifiersItemClauses(modifiersItemClauses1);
        String businessID = dataSet.get("businessId");

        Response createLISResponse3 = pageObj.endpoints().createLISUsingApi(adminKey, lisName3, external_id,
                "base_only", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
        Assert.assertEquals(createLISResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "STEP-1 Create LIS response status code is not 200");
        boolean actualSuccessMessage3 = createLISResponse3.jsonPath().getBoolean("results[0].success");
        Assert.assertTrue(actualSuccessMessage3, "STEP-1  Success message is not True");
        String actualExternalIdLIS3 = createLISResponse3.jsonPath().getString("results[0].external_id").replace("[", "")
                .replace("]", "");
        map.put("item_qualifiers", "[{\"quantity\":1,\"processing_method\":\"\",\"line_item_selector\":\""
                + actualExternalIdLIS3 + "\"}]");
        map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS3
                + "\",\"processing_method\":\"\",\"quantity\":\"1\"}]");
        Response qcCreateResponse3 = pageObj.endpoints().createQCUsingApi(adminKey, QCName, "", actualExternalIdLIS3,
                "actualExternalIdLIS2", processingFunction2, "10", dataSet.get("locationID"), map);
        Assert.assertEquals(qcCreateResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                QCName + " QC is not created and status code is not 200");
        String actualExternalIdQC3 = qcCreateResponse3.jsonPath().getString("results[0].external_id").replace("[", "")
                .replace("]", "");
        String errorMsg3 = qcCreateResponse3.jsonPath().getString("results[0].errors");
        Assert.assertEquals(errorMsg3, "[Processing Function is not included in the list]",
                "Error message for qc creation with invalid line item filter id doesn't match");
        boolean actualSuccessMessageQC3 = qcCreateResponse3.jsonPath().getBoolean("results[0].success");
        Assert.assertFalse(actualSuccessMessageQC3, "QC success message is True");
        utils.logPass("User is unable to create QC successfully with processing function bundle_price_target, expected");

        // LIF gets created with sum of amounts(i.e in get qc api response processing
        // method of line item filter should be blank)
        Response getQCDetailsResponse = pageObj.endpoints().getQualificationListUsingAPI(adminKey, QCName, "2", "13");
        Assert.assertEquals(getQCDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Unable to fetch the list of LIS");
        String actualProcessingMethod = getQCDetailsResponse.jsonPath()
                .getString("data[0].line_item_filters[0].processing_method");
        Assert.assertNull(actualProcessingMethod, "Processing method is not null in get Qc response");
        utils.logPass("Verified that LIF is created with null processing method");

        // creating QC with "target_price": -1,
        map.put("target_price", "-1");
        Response qcCreateResponse4 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", actualExternalIdLIS3,
                "actualExternalIdLIS2", processingFunction2, "10", dataSet.get("locationID"), map);
        Assert.assertEquals(qcCreateResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                QCName2 + " QC is not created and status code is not 200");
        String errorMsg4 = qcCreateResponse4.jsonPath().getString("results[0].errors[0]");
        Assert.assertEquals(errorMsg4, "Processing Function is not included in the list",
                "Error message for qc creation with invalid line item filter id doesn't match");
        boolean actualSuccessMessageQC4 = qcCreateResponse4.jsonPath().getBoolean("results[0].success");
        Assert.assertFalse(actualSuccessMessageQC4,
                "QC success message is True on creating QC with  target_price: -1,");
        utils.logPass("User is not able to create QC with negative target price, expected");

        // creating QC with "target_price": null,
        map.put("target_price", "null");
        Response qcCreateResponse5 = pageObj.endpoints().createQCUsingApi(adminKey, QCName3, "", actualExternalIdLIS3,
                "actualExternalIdLIS2", processingFunction2, "10", dataSet.get("locationID"), map);
        Assert.assertEquals(qcCreateResponse5.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                QCName3 + " QC is not created and status code is not 200");
        String errorMsg5 = qcCreateResponse5.jsonPath().getString("results[0].errors[0]");
        Assert.assertEquals(errorMsg5, "Processing Function is not included in the list",
                "Error message for qc creation with invalid line item filter id doesn't match");
        boolean actualSuccessMessageQC5 = qcCreateResponse5.jsonPath().getBoolean("results[0].success");
        Assert.assertFalse(actualSuccessMessageQC5,
                "QC success message is True on creating QC with target_price: null,");
        utils.logPass("User is not able to create QC with target_price: null, expected");

        // creating QC with "target_price": "ABC",
        map.put("target_price", "ABC");
        Response qcCreateResponse6 = pageObj.endpoints().createQCUsingApi(adminKey, QCName4, "", actualExternalIdLIS3,
                "actualExternalIdLIS2", processingFunction2, "10", dataSet.get("locationID"), map);
        Assert.assertEquals(qcCreateResponse5.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                QCName4 + " QC is not created and status code is not 200");
        String errorMsg6 = qcCreateResponse6.jsonPath().getString("results[0].errors[0]");
        Assert.assertEquals(errorMsg6, "Processing Function is not included in the list",
                "Error message for qc creation with invalid line item filter id doesn't match");
        boolean actualSuccessMessageQC6 = qcCreateResponse6.jsonPath().getBoolean("results[0].success");
        Assert.assertFalse(actualSuccessMessageQC6,
                "QC success message is True on creating QC with  target_price: -1,");
        utils.logPass("User is not able to create QC with target_price: ABC, expected");

        // creating QC with negative amount cap
        map.put("amount_cap", "-1");
        Response qcCreateResponse7 = pageObj.endpoints().createQCUsingApi(adminKey, QCName5, "", actualExternalIdLIS3,
                "actualExternalIdLIS2", processingFunction2, "10", dataSet.get("locationID"), map);
        Assert.assertEquals(qcCreateResponse7.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                QCName5 + " QC is not created and status code is not 200");
        String errorMsg7 = qcCreateResponse7.jsonPath().getString("results[0].errors[1]");
        Assert.assertEquals(errorMsg7, "Target Unit Price is not a number",
                "Error message for qc creation with invalid line item filter id doesn't match");
        boolean actualSuccessMessageQC7 = qcCreateResponse7.jsonPath().getBoolean("results[0].success");
        Assert.assertFalse(actualSuccessMessageQC7,
                "QC success message is True on creating QC with  amount_cap: -1,");
        utils.logPass("User is not able to create QC with negative amount cap, expected");

        // creating QC with negative max discount units
        map.put("amount_cap", "1");
        map.put("max_discount_units","-1");
        Response qcCreateResponse8 = pageObj.endpoints().createQCUsingApi(adminKey, QCName6, "", actualExternalIdLIS3,
                "actualExternalIdLIS2", processingFunction2, "10", dataSet.get("locationID"), map);
        Assert.assertEquals(qcCreateResponse8.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                QCName6 + " QC is not created and status code is not 200");
        String errorMsg8 = qcCreateResponse8.jsonPath().getString("results[0].errors[1]");
        Assert.assertEquals(errorMsg8, "Target Unit Price is not a number",
                "Error message for qc creation with invalid line item filter id doesn't match");
        boolean actualSuccessMessageQC8 = qcCreateResponse8.jsonPath().getBoolean("results[0].success");
        Assert.assertFalse(actualSuccessMessageQC8,
                "QC success message is True on creating QC with  max_discount_units: -1,");
        utils.logPass("User is not able to create QC with negative max_discount_units, expected");

        // creating QC with negative percentage_of_processed_amount
        map.put("amount_cap", "1");
        map.put("max_discount_units","1");
        map.put("target_price","-10");
        Response qcCreateResponse9 = pageObj.endpoints().createQCUsingApi(adminKey, QCName6, "", actualExternalIdLIS3,
                "actualExternalIdLIS2", processingFunction2, "-10", dataSet.get("locationID"), map);
        Assert.assertEquals(qcCreateResponse9.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                QCName6 + " QC is not created and status code is not 200");
        String errorMsg9 = qcCreateResponse9.jsonPath().getString("results[0].errors[0]");
        Assert.assertEquals(errorMsg9, "Processing Function is not included in the list",
                "Error message for qc creation with invalid line item filter id doesn't match");
        boolean actualSuccessMessageQC9 = qcCreateResponse9.jsonPath().getBoolean("results[0].success");
        Assert.assertFalse(actualSuccessMessageQC9,
                "QC success message is True on creating QC with  percentage_of_processed_amount: -1,");
        utils.logPass("User is not able to create QC with negative percentage_of_processed_amount, expected");


        // Delete LIS 1
        String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS3)
                .replace("${businessID}", businessID);
        boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
        Assert.assertTrue(deleteLisStatus1, lisName3 + " LIS is not deleted successfully");
        utils.logit(lisName3 + " LIS is deleted successfully");


        // Delete QC
        String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", actualExternalIdQC3)
                .replace("${businessID}", businessID);
        String qcID_DB =
                DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");
        String deleteQCFromQualifying_expressions =
                deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID", qcID_DB);
        boolean statusQualifying_expressionsQuery =
                DBUtils.executeQuery(env, deleteQCFromQualifying_expressions);
        Assert.assertFalse(statusQualifying_expressionsQuery,
                QCName + " QC is not deleted successfully");
        utils.logit(QCName + " QC is deleted successfully");


    }

    @Test(description = "SQ-T5647 Verify creating a Qualification criteria using the new create QC API with only Base items.", groups = {
            "regression", "unstable" })
    public void T5647_VerifyQcCreationWithLisHavingFilterItemBaseOnlyItems() throws Exception {
        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        pageObj.instanceDashboardPage().loginToInstance();
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

        String external_id = "";
        String adminKey = dataSet.get("apiKey");
        String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
        String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
        String processingFunction = "sum_amounts";
        String processingFunctionNameUI = "Sum of Amounts";
        String updatedProcessingFunction = "sum_qty";
        String updatedProcessingfunctionNameUI = "Sum of Quantities";
        String businessID = dataSet.get("business_id");

        // filter_item_set == base_only, modifiers_only and base_and_modifiers
        // Add base item cluases

        String baseItemClauses1 = dataSet.get("baseItemClauses");
        String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

        listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

        // Add Modifiers clauses
        listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
                .getListOfModifiersItemClauses(modifiersItemClauses1);

        // Step1 - If pass blank external_id, LIS will be created and system will
        // automatically generate the external_id and assigned to that LIS.
        Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id, "base_only",
                listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
        Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "STEP-1 Create LIS response status code is not 200");
        boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
        Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
        String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
                .replace("]", "");
        utils.logit("LIS having filter item set as base_only has been created successfully");

        Response qcCreateResponse = pageObj.endpoints().createQualificationCriteriaUsingApi(adminKey, QCName, "",
                actualExternalIdLIS, processingFunction, "10");
        Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                QCName + " QC is not created and status code is not 200");

        boolean actualSuccessMessageQC = qcCreateResponse.jsonPath().getBoolean("results[0].success");
        Assert.assertTrue(actualSuccessMessageQC, "QC success message is not True");

        utils.logit(QCName + " QC is created successfully with LIS " + lisName1
                + " having filter item set as base_only has been created successfully");

        String actualExternalIdQC = qcCreateResponse.jsonPath().getString("results[0].external_id").replace("[", "")
                .replace("]", "");
        pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
        pageObj.qualificationcriteriapage().SearchQC(QCName);

        String actualProcessingFunctionUI = pageObj.qualificationcriteriapage().getSelectedProcessingFunctionsValue();
        Assert.assertEquals(actualProcessingFunctionUI, processingFunctionNameUI,
                actualProcessingFunctionUI + " Processing function is not updated successfully in UI");
        utils.logPass("Verified that Proceesing function name is selected in UI : " + actualProcessingFunctionUI);

        String actualSelectedLineItemFilterValue = pageObj.qualificationcriteriapage()
                .getFirstSelectedLineItemFilterName();
        Assert.assertEquals(actualSelectedLineItemFilterValue, lisName1, actualSuccessMessage
                + " actual Line Item Filter Name is not matched with expected Line Item Filter Name");
        utils.logPass(
                "Verified that Line Item Filter Name is matched with expected Line Item Filter Name in QC from UI");

        String actualSelectedLineItemQualifierValue = pageObj.qualificationcriteriapage()
                .getFirstSelectedLineItemQualifierName();
        Assert.assertEquals(actualSelectedLineItemQualifierValue, lisName1, actualSuccessMessage
                + " actual Line Item Qualifier Name is not matched with expected Line Item Filter Name");
        utils.logPass(
                "Verified that Line Item Qualifier Name is matched with expected Line Item Qualifier Name in QC from UI");


        // Delete LIS 1
        String deleteLISQuery2 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
                .replace("${businessID}", businessID);
        boolean deleteLisStatus2 = DBUtils.executeQuery(env, deleteLISQuery2);
        Assert.assertTrue(deleteLisStatus2, lisName1 + " LIS is not deleted successfully");
        utils.logit(lisName1 + " LIS is deleted successfully");

        // Delete QC
        String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", actualExternalIdQC)
                .replace("${businessID}", businessID);
        String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");

        String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
                qcID_DB);

        boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env,
                deleteQCFromQualifying_expressions);
        Assert.assertTrue(statusQualifying_expressionsQuery, QCName + " QC is not deleted successfully");
        utils.logit(QCName + " QC is deleted successfully");

        String deleteQCFromQualification_criteria = deleteQCFromQualification_criteriaQuery
                .replace("$qcExternalID", actualExternalIdQC).replace("${businessID}", dataSet.get("business_id"));

        boolean statusQualification_criteriaQuery = DBUtils.executeQuery(env,
                deleteQCFromQualification_criteria);
        Assert.assertTrue(statusQualification_criteriaQuery, QCName + " QC is not deleted successfully");
        utils.logit(QCName + " QC is deleted successfully ");

    }
    @Test(description = "SQ-T5611 Verify creating a QC using the BOGO (fix item) processing_function with mandatory and optional fields.", groups = {
            "regression", "unstable" })
    public void T5611_VerifyQcCreationUsingProcessingFunctionBOGO() throws Exception {
        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        pageObj.instanceDashboardPage().loginToInstance();
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

        String external_id = "";
        String adminKey = dataSet.get("apiKey");
        String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
        String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
        String processingFunction = "bogof";
        String processingFunctionNameUI = "Buy one get one free (fix item)";
        String businessID = dataSet.get("business_id");

        // filter_item_set == base_only, modifiers_only and base_and_modifiers
        // Add base item cluases

        String baseItemClauses1 = dataSet.get("baseItemClauses");
        String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

        listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

        // Add Modifiers clauses
        listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
                .getListOfModifiersItemClauses(modifiersItemClauses1);

        // Step1 - If pass blank external_id, LIS will be created and system will
        // automatically generate the external_id and assigned to that LIS.
        Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id, "base_only",
                listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
        Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "STEP-1 Create LIS response status code is not 200");
        boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
        Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
        String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
                .replace("]", "");
        utils.logit("LIS having filter item set as base_only has been created successfully");

        Response qcCreateResponse = pageObj.endpoints().createQualificationCriteriaUsingApi(adminKey, QCName, "",
                actualExternalIdLIS, processingFunction, "10");
        Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                QCName + " QC is not created and status code is not 200");

        boolean actualSuccessMessageQC = qcCreateResponse.jsonPath().getBoolean("results[0].success");
        Assert.assertTrue(actualSuccessMessageQC, "QC success message is not True");

        utils.logit(QCName + " QC is created successfully with LIS " + lisName1
                + " having filter item set as base_only has been created successfully");
        String actualExternalIdQC = qcCreateResponse.jsonPath().getString("results[0].external_id").replace("[", "")
                .replace("]", "");
        pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
        pageObj.qualificationcriteriapage().SearchQC(QCName);
        utils.waitTillPagePaceDone();
        String actualProcessingFunctionUI = pageObj.qualificationcriteriapage().getSelectedProcessingFunctionsValue();
        Assert.assertEquals(actualProcessingFunctionUI, processingFunctionNameUI,
                actualProcessingFunctionUI + " Processing function is not updated successfully in UI");
        utils.logPass("Verified that Proceesing function name is selected in UI : " + actualProcessingFunctionUI);

        String actualSelectedLineItemFilterValue = pageObj.qualificationcriteriapage()
                .getFirstSelectedLineItemFilterName();
        Assert.assertEquals(actualSelectedLineItemFilterValue, lisName1, actualSuccessMessage
                + " actual Line Item Filter Name is not matched with expected Line Item Filter Name");
        utils.logPass(
                "Verified that Line Item Filter Name is matched with expected Line Item Filter Name in QC from UI");

        String actualSelectedLineItemQualifierValue = pageObj.qualificationcriteriapage()
                .getFirstSelectedLineItemQualifierName();
        Assert.assertEquals(actualSelectedLineItemQualifierValue, lisName1, actualSuccessMessage
                + " actual Line Item Qualifier Name is not matched with expected Line Item Filter Name");
        utils.logPass(
                "Verified that Line Item Qualifier Name is matched with expected Line Item Qualifier Name in QC from UI");


        // Delete LIS 1
        String deleteLISQuery2 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
                .replace("${businessID}", businessID);
        boolean deleteLisStatus2 = DBUtils.executeQuery(env, deleteLISQuery2);
        Assert.assertTrue(deleteLisStatus2, lisName1 + " LIS is not deleted successfully");
        utils.logit(lisName1 + " LIS is deleted successfully");

        // Delete QC
        String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", actualExternalIdQC)
                .replace("${businessID}", businessID);
        String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");

        String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
                qcID_DB);

        boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env,
                deleteQCFromQualifying_expressions);
        Assert.assertTrue(statusQualifying_expressionsQuery, QCName + " QC is not deleted successfully");
        utils.logit(QCName + " QC is deleted successfully");

        String deleteQCFromQualification_criteria = deleteQCFromQualification_criteriaQuery
                .replace("$qcExternalID", actualExternalIdQC).replace("${businessID}", dataSet.get("business_id"));

        boolean statusQualification_criteriaQuery = DBUtils.executeQuery(env,
                deleteQCFromQualification_criteria);
        Assert.assertTrue(statusQualification_criteriaQuery, QCName + " QC is not deleted successfully");
        utils.logit(QCName + " QC is deleted successfully ");

    }
    @Test(description = "SQ-T5614 Verify creating a QC using the BOGO (any item) processing_function with mandatory and optional fields.", groups = {
            "regression", "unstable" })
    public void T5614_VerifyQcCreationUsingProcessingFunctionBOGO2() throws Exception {
        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        pageObj.instanceDashboardPage().loginToInstance();
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

        String external_id = "";
        String adminKey = dataSet.get("apiKey");
        String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
        String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
        String processingFunction = "bogof2";
        String processingFunctionNameUI = "Buy one get one free (any item)";
        String businessID = dataSet.get("business_id");

        // filter_item_set == base_only, modifiers_only and base_and_modifiers
        // Add base item cluases

        String baseItemClauses1 = dataSet.get("baseItemClauses");
        String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

        listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

        // Add Modifiers clauses
        listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
                .getListOfModifiersItemClauses(modifiersItemClauses1);

        // Step1 - If pass blank external_id, LIS will be created and system will
        // automatically generate the external_id and assigned to that LIS.
        Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id, "base_only",
                listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
        Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "STEP-1 Create LIS response status code is not 200");
        boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
        Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
        String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
                .replace("]", "");
        utils.logit("LIS having filter item set as base_only has been created successfully");

        Response qcCreateResponse = pageObj.endpoints().createQualificationCriteriaUsingApi(adminKey, QCName, "",
                actualExternalIdLIS, processingFunction, "10");
        Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                QCName + " QC is not created and status code is not 200");

        boolean actualSuccessMessageQC = qcCreateResponse.jsonPath().getBoolean("results[0].success");
        Assert.assertTrue(actualSuccessMessageQC, "QC success message is not True");

        utils.logit(QCName + " QC is created successfully with LIS " + lisName1
                + " having filter item set as base_only has been created successfully");
        String actualExternalIdQC = qcCreateResponse.jsonPath().getString("results[0].external_id").replace("[", "")
                .replace("]", "");
        pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
        pageObj.qualificationcriteriapage().SearchQC(QCName);

        String actualProcessingFunctionUI = pageObj.qualificationcriteriapage().getSelectedProcessingFunctionsValue();
        Assert.assertEquals(actualProcessingFunctionUI, processingFunctionNameUI,
                actualProcessingFunctionUI + " Processing function is not updated successfully in UI");
        utils.logPass("Verified that Proceesing function name is selected in UI : " + actualProcessingFunctionUI);

        String actualSelectedLineItemFilterValue = pageObj.qualificationcriteriapage()
                .getFirstSelectedLineItemFilterName();
        Assert.assertEquals(actualSelectedLineItemFilterValue, lisName1, actualSuccessMessage
                + " actual Line Item Filter Name is not matched with expected Line Item Filter Name");
        utils.logPass(
                "Verified that Line Item Filter Name is matched with expected Line Item Filter Name in QC from UI");

        String actualSelectedLineItemQualifierValue = pageObj.qualificationcriteriapage()
                .getFirstSelectedLineItemQualifierName();
        Assert.assertEquals(actualSelectedLineItemQualifierValue, lisName1, actualSuccessMessage
                + " actual Line Item Qualifier Name is not matched with expected Line Item Filter Name");
        utils.logPass(
                "Verified that Line Item Qualifier Name is matched with expected Line Item Qualifier Name in QC from UI");


        // Delete LIS 1
        String deleteLISQuery2 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
                .replace("${businessID}", businessID);
        boolean deleteLisStatus2 = DBUtils.executeQuery(env, deleteLISQuery2);
        Assert.assertTrue(deleteLisStatus2, lisName1 + " LIS is not deleted successfully");
        utils.logit(lisName1 + " LIS is deleted successfully");

        // Delete QC
        String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", actualExternalIdQC)
                .replace("${businessID}", businessID);
        String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");

        String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
                qcID_DB);

        boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env,
                deleteQCFromQualifying_expressions);
        Assert.assertTrue(statusQualifying_expressionsQuery, QCName + " QC is not deleted successfully");
        utils.logit(QCName + " QC is deleted successfully");

        String deleteQCFromQualification_criteria = deleteQCFromQualification_criteriaQuery
                .replace("$qcExternalID", actualExternalIdQC).replace("${businessID}", dataSet.get("business_id"));

        boolean statusQualification_criteriaQuery = DBUtils.executeQuery(env,
                deleteQCFromQualification_criteria);
        Assert.assertTrue(statusQualification_criteriaQuery, QCName + " QC is not deleted successfully");
        utils.logit(QCName + " QC is deleted successfully ");

    }
    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        pageObj.utils().clearDataSet(dataSet);
        driver.quit();
        logger.info("Browser closed");
    }
}

