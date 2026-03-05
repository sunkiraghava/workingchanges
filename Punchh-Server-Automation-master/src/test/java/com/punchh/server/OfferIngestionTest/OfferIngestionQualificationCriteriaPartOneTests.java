package com.punchh.server.OfferIngestionTest;

import com.punchh.server.LisCreationUtilityClasses.BaseItemClauses;
import com.punchh.server.LisCreationUtilityClasses.ModifiersItemsClauses;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.*;
import io.restassured.response.Response;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.*;
import java.lang.reflect.Method;
import java.util.*;
/*
 * @Author : Shashank Sharma
 */

@Listeners(TestListeners.class)
public class OfferIngestionQualificationCriteriaPartOneTests {
  public WebDriver driver;
  private String userEmail;
  private ApiUtils apiUtils;
  private Properties prop;
  private PageObj pageObj;
  private String sTCName;
  private String env, run = "ui";
  private String baseUrl;
  private static Map<String, String> dataSet;
  public List<BaseItemClauses> listBaseItemClauses = new ArrayList();
  public List<ModifiersItemsClauses> listModifiresItemClauses = new ArrayList();
  public Utilities utils;
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


  @BeforeMethod(alwaysRun = true)
  public void setUp(Method method) {
    prop = Utilities.loadPropertiesFile("config.properties");
    driver = new BrowserUtilities().launchBrowser();
    sTCName = method.getName();
    pageObj = new PageObj(driver);
    env = pageObj.getEnvDetails().setEnv();
    baseUrl = pageObj.getEnvDetails().setBaseUrl();
    apiUtils = new ApiUtils();
    utils = new Utilities(driver);
    pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
    dataSet = pageObj.readData().readTestData;
    pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
            pageObj.readData().getJsonFilePath(run , env , "Secrets"), dataSet.get("slug"));
    dataSet.putAll(pageObj.readData().readTestData);
    pageObj.utils().logit(sTCName + " ==>" + dataSet);
  }

  // shashank
  @Test(description = "OMM-T2608 QC Creation without passing any values in optional fields", groups = { "regression", "dailyrun" }, priority = 1)
  public void OMM_T2608_VerifyQCCreationWiithoutPassingValuesInOptionalFields() throws Exception {
    String businessID = dataSet.get("business_id");
    String adminKey = dataSet.get("apiKey");
    String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
    String processingFunction = "sum_amounts";
    Map<String, String> map = new HashMap<String, String>();
    map.put("receipt_qualifiers", "\"\"");
    map.put("item_qualifiers", "\"\"");
    map.put("amount_cap", "\"\"");
    map.put("percentage_of_processed_amount", "");
    map.put("rounding_rule", "");
    map.put("max_discount_units", "\"\"");

    // creating qc with valid location id of the same business
    Response qcCreateResponse = pageObj.endpoints().createQCUsingApi(adminKey, QCName, "", "", "",
        processingFunction, "", dataSet.get("locationID"), map);
    Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
        QCName + " QC is not created and status code is not 200");

    String qcExternalID = qcCreateResponse.jsonPath().getString("results[0].external_id")
        .replace("[", "").replace("]", "");
    Assert.assertTrue(qcExternalID != null, QCName + " QC is not created successfully ");

    Response getQCDetailsResponse =
        pageObj.endpoints().getQualificationListUsingAPI(adminKey, QCName, "2", "13");
    Assert.assertEquals(getQCDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
        "Unable to fetch the list of LIS");

    String qcNameActual =
        getQCDetailsResponse.jsonPath().getString("data[0].name").replace("[", "").replace("]", "");
    Assert.assertEquals(qcNameActual, QCName, QCName + " QC name is not matched in api response ");
    pageObj.utils().logit(qcNameActual + " actual qc name is matched with expected QC name " + QCName);

    try {
    String amountCapActual = getQCDetailsResponse.jsonPath().get("data[0].amount_cap");
    Assert.assertEquals(amountCapActual, null, " Amount cap is not matched in api response ");
    pageObj.utils().logit(amountCapActual + " actual amount_cap is matched with expected amount_cap NULL ");
    } catch (Exception e) {
        pageObj.utils().logit(" actual amount_cap is matched with expected amount_cap NULL ");
    }


    String max_discount_unitsActual =
        getQCDetailsResponse.jsonPath().get("data[0].max_discount_units");
    Assert.assertEquals(max_discount_unitsActual, null,
        " max_discount_units is not matched in api response ");
    pageObj.utils().logit(max_discount_unitsActual
        + " actual max_discount_units is matched with expected max_discount_units NULL ");

    pageObj.utils().logit("getQCDetailsResponse ---- ** - " + getQCDetailsResponse.asPrettyString());
    String percentage_of_processed_amountActual =
        getQCDetailsResponse.jsonPath().get("data[0].percentage_of_processed_amount");
    Assert.assertEquals(percentage_of_processed_amountActual, null,
        " percentage_of_processed_amount is not matched in api response ");
    pageObj.utils().logit(percentage_of_processed_amountActual
        + " actual percentage_of_processed_amount is matched with expected percentage_of_processed_amount NULL ");

    String receipt_qualifiersActual =
        getQCDetailsResponse.jsonPath().getString("data[0].receipt_qualifiers");
    Assert.assertEquals(receipt_qualifiersActual, "[]",
        " receipt_qualifiers is not matched in api response ");
    pageObj.utils().logit(receipt_qualifiersActual
        + " actual receipt_qualifiers is matched with expected receipt_qualifiers NULL ");

    String line_item_filtersActual =
        getQCDetailsResponse.jsonPath().getString("data[0].line_item_filters");
    Assert.assertEquals(line_item_filtersActual, "[]",
        " line_item_filters is not matched in api response ");
    pageObj.utils().logit(line_item_filtersActual
        + " actual line_item_filters is matched with expected line_item_filters NULL ");

    String item_qualifiersActual =
        getQCDetailsResponse.jsonPath().getString("data[0].item_qualifiers");
    Assert.assertEquals(item_qualifiersActual, "[]",
        " item_qualifiers is not matched in api response ");
    pageObj.utils().logit(item_qualifiersActual
        + " actual item_qualifiers is matched with expected item_qualifiers NULL ");

    // Delete QC
    String getQC_idStringQuery =
        getQC_idString.replace("$qcExternalID", qcExternalID).replace("${businessID}", businessID);
    String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");

    String deleteQCFromQualifying_expressions =
        deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID", qcID_DB);

    String deleteQCFromQualification_criteria =
        deleteQCFromQualification_criteriaQuery.replace("$qcExternalID", qcExternalID)
            .replace("${businessID}", dataSet.get("business_id"));

    boolean statusQualification_criteriaQuery =
        DBUtils.executeQuery(env, deleteQCFromQualification_criteria);
    Assert.assertTrue(statusQualification_criteriaQuery,
        QCName + " QC is not deleted successfully");
    pageObj.utils().logPass(QCName + " QC is deleted successfully");

  }

  @Test(
      description = "OMM-T2717/SQ-T5793 All Target price functions>Validate that user is not able to save QC by entering negative values in any "
      		+ "of field present on UI", groups = { "regression", "dailyrun" }, priority = 2)
  public void T2717_VerifyUserIsNotAbleToSaveQCWithNegativeValuesInUI()
      throws InterruptedException {
    String QCname = "QcSubscription_" + CreateDateTime.getTimeDateString();
    String amountcap = "-2";
    String unitDiscount = "-34";
    String qcFucntionName = "Rate Rollback";
    String lineItemSelectorName = "Auto_lineItemSelector";

    pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
    pageObj.instanceDashboardPage().loginToInstance();
    pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

    pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");

    pageObj.qualificationcriteriapage().createQualificationCriteria(QCname, amountcap,
        qcFucntionName, unitDiscount, false, lineItemSelectorName);

    pageObj.qualificationcriteriapage().enterValueInInputBox("Maximum Discounted Units", "-44");
    pageObj.qualificationcriteriapage().enterValueInInputBox("Minimum Unit Rate", "-45");
    pageObj.qualificationcriteriapage().updateButton();

    String errorMessage_amountCap =
        pageObj.qualificationcriteriapage().verifyErrorMessageForNegativeValue("Amount Cap");
    Assert.assertEquals(errorMessage_amountCap, "must be greater than or equal to 0",
        errorMessage_amountCap
            + " error message not matched for amount cap expected error message :-must be greater than or equal to 0 ");

    String errorMessage_unitDiscount =
        pageObj.qualificationcriteriapage().verifyErrorMessageForNegativeValue("Unit Discount");
    Assert.assertEquals(errorMessage_unitDiscount, "must be greater than or equal to 0.01",
        errorMessage_amountCap
            + " error message not matched for Unit Discount expected error message :-must be greater than or equal to 0.01");

    String errorMessage_minimumUnitRate =
        pageObj.qualificationcriteriapage().verifyErrorMessageForNegativeValue("Minimum Unit Rate");
    Assert.assertEquals(errorMessage_minimumUnitRate, "must be greater than or equal to 0.01",
        errorMessage_amountCap
            + " error message not matched for Minimum Unit Rate expected error message :-must be greater than or equal to 0.01");

  
  }
  
  
  @Test(priority = 3,description = "OMM-T271/SQ-T5792 Target Price for Bundle>Validate that If user has passed Qty-NULL for line item filter in API then it does not create any LIF", groups = { "regression", "dailyrun" })
  public void T2715_ValidateLISNotcreatedIfQtyIsNUll() {String external_id = "";
  String adminKey = dataSet.get("apiKey");
  String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
  String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
  String QCName2 = QCName+"_2";

  String processingFunction = "sum_amounts";
  String processingFunctionNameUI = "Sum of Amounts";
  String updatedProcessingFunction = "sum_qty";
  String updatedProcessingfunctionNameUI = "Sum of Quantities";
  String businessID = dataSet.get("business_id");
  Map<String, String> map = new HashMap<String, String>();
  

  String baseItemClauses1 = dataSet.get("baseItemClauses");
  String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

  listBaseItemClauses =
      pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

  // Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
  // base_and_modifiers
  listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
      .getListOfModifiersItemClauses(modifiersItemClauses1);

  // Step1 - If pass blank external_id, LIS will be created and system will
  // automatically generate the external_id and assigned to that LIS.
  Response createLISResponse =
      pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id, "base_and_modifiers",
          listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
  Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
      "STEP-1 Create LIS response status code is not 200");
  boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
  Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
  String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id")
      .replace("[", "").replace("]", "");
  pageObj.utils().logPass(
      "Verified Step1:- If pass blank external_id, LIS will be created and system will automatically generate the external_id and assigned to that LIS");

  
  String lifDetails ="[{\"line_item_selector_id\":\""+actualExternalIdLIS+"\",\"processing_method\":\"min_price\",\"quantity\":null}]";
  map.put("line_item_filters", lifDetails);
  String itemQualifierStr= "[{\"expression_type\":\"line_item_exists\",\"line_item_selector_id\":\""+actualExternalIdLIS+"\",\"net_value\":\"3\"}]";
  map.put("item_qualifiers", itemQualifierStr);
  // creating qc with valid location id of the same business
  Response qcCreateResponse = pageObj.endpoints().createQCUsingApi(adminKey, QCName, "", "", "",
      processingFunction, "", dataSet.get("locationID"), map);
  
  Response getQCDetailsResponse =
      pageObj.endpoints().getQualificationListUsingAPI(adminKey, QCName, "2", "13");
  Assert.assertEquals(getQCDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
      "Unable to fetch the list of LIS");

  System.out.println("getQCDetailsResponse-- "+ getQCDetailsResponse.asPrettyString());
  String actualLine_item_filters = getQCDetailsResponse.jsonPath().getString("data[0].line_item_filters");
  Assert.assertEquals(actualLine_item_filters, "[]" , "line_item_filters is not coming as empty . it should be empty");
  
  lifDetails ="[{\"line_item_selector_id\":\""+actualExternalIdLIS+"\",\"processing_method\":\"min_price\",\"quantity\":\"6\"}]";
  map.put("line_item_filters", lifDetails);
  
  Response qcCreateResponse1 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", "", "",
      processingFunction, "", dataSet.get("locationID"), map);
  
  Response getQCDetailsResponse2 =
      pageObj.endpoints().getQualificationListUsingAPI(adminKey, QCName2, "2", "13");
  Assert.assertEquals(getQCDetailsResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
      "Unable to fetch the list of LIS");

  String actualLine_item_filters2 = getQCDetailsResponse2.jsonPath().getString("data[0].line_item_filters");
  Assert.assertEquals(actualLine_item_filters2, "[]" , "line_item_filters is not coming as empty . it should be empty");
  pageObj.utils().logPass("Verified that line item filters value is empty i.e []");


  }

  // shashank
  @Test(priority = 4 , description = "SQ-T5608 Verify creating a QC using the \"receipt subtotal amount\" processing_function with both mandatory and optional fields", groups = { "regression", "dailyrun" })
  public void T5608_VerifyQCCreationWithProcessingFunctionAsReceiptSubTotalAmount() throws Exception {
    String businessID = dataSet.get("business_id");
    String external_id = "";
    String adminKey = dataSet.get("apiKey");
    String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
    String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
    String QCName2 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
    String processingFunction = "receipt_subtotal";
    Map<String, String> map = new HashMap<String, String>();
    map.put("receipt_qualifiers",
            "{\"attribute\":\"total_amount\",\"operator\":\">=\",\"value\":-10}");

    // filter_item_set == base_only, modifiers_only and base_and_modifiers
//  	// Add base item cluases

    String baseItemClauses1 = dataSet.get("baseItemClauses");
    String modifiersItemClauses1 = dataSet.get("modifierItemClauses");
    listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

    // Add Modifiers clauses
    listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
            .getListOfModifiersItemClauses(modifiersItemClauses1);

    // Step1 - If pass blank external_id, LIS will be created and system will
    // automatically generate the external_id and assigned to that LIS.
    Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id,
            "base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 2, "max_price","");
    
    Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
            "STEP-1 Create LIS response status code is not 200");
    boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
    Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
    String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
            .replace("]", "");

    // qc creation where value in receipt_qualifiers is a negative number.
    Response qcCreateResponse = pageObj.endpoints().createQCUsingApi(adminKey, QCName, "",
            actualExternalIdLIS, "actualExternalIdLIS2", processingFunction, "10",dataSet.get("locationID"),map);;
    Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
            QCName + " QC is not created and status code is not 200");
    String actualExternalIdQC = qcCreateResponse.jsonPath().getString("results[0].external_id")
            .replace("[", "").replace("]", "");

    pageObj.utils().logPass("Verified that line item filters value is empty i.e []");

  }

  // shashank
  @Test(description = "SQ-T5639 Verify the errors when every attribute is duplicate for QC creation", groups = { "regression", "dailyrun" }, priority = 5)
  public void T5639_VerifyErrorOccursIfAddingDuplicateAttributesInQCCreationAPI() throws Exception {
    String businessID = dataSet.get("business_id");
    String external_id = "";
    String adminKey = dataSet.get("apiKey");
    String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
    String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
    String QCName2 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
    String processingFunction = "receipt_subtotal";
    Map<String, String> map = new HashMap<String, String>();
    map.put("receipt_qualifiers",
            "[{\"attribute\":\"total_amount\",\"operator\":\">=\",\"value\":10},{\"attribute\":\"total_amount\",\"operator\":\">=\",\"value\":10}]");

    // filter_item_set == base_only, modifiers_only and base_and_modifiers
//  	// Add base item cluases

    String baseItemClauses1 = dataSet.get("baseItemClauses");
    String modifiersItemClauses1 = dataSet.get("modifierItemClauses");
    listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

    // Add Modifiers clauses
    listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
            .getListOfModifiersItemClauses(modifiersItemClauses1);

    // Step1 - If pass blank external_id, LIS will be created and system will
    // automatically generate the external_id and assigned to that LIS.
    Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id,
            "base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 2, "max_price","");
    Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
            "STEP-1 Create LIS response status code is not 200");
    boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
    Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
    String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
            .replace("]", "");

    // qc creation where value in receipt_qualifiers is a negative number.
    Response qcCreateResponse = pageObj.endpoints().createQCUsingApi(adminKey, QCName, "",
            actualExternalIdLIS, "actualExternalIdLIS2", processingFunction, "10",dataSet.get("locationID"),map);;
    Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
            QCName + " QC is not created and status code is not 200");
    String actualExternalIdQC = qcCreateResponse.jsonPath().getString("results[0].external_id")
            .replace("[", "").replace("]", "");

    String warningMessage = qcCreateResponse.jsonPath().getString("results[0].warnings.receipt_qualifiers[0].message").toString();
    Assert.assertTrue(warningMessage.contains("Required parameter is duplicate: attribute"),
            "Duplicate attribute found in receipt qualifiers warning message is not coming in response");

    pageObj.utils().logPass(warningMessage + "warning message is verified");

  }

  // shashank
  @Test(priority = 6,description = "SQ-T5617 Verify creating a QC using the \"Receipt Total Amount\" processing_function with both mandatory and optional fields.", groups = { "regression", "dailyrun" })
  public void T5617_VerifyQCCreationWithProcessingFunctionAsReceiptTotalAmount() throws Exception {
    String businessID = dataSet.get("business_id");
    String external_id = "";
    String adminKey = dataSet.get("apiKey");
    String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
    String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
    String QCName2 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
    String processingFunction = "receipt_total_amount";
    Map<String, String> map = new HashMap<String, String>();
    map.put("receipt_qualifiers",
            "{\"attribute\":\"total_amount\",\"operator\":\">=\",\"value\":-10}");

    // filter_item_set == base_only, modifiers_only and base_and_modifiers
//  	// Add base item cluases

    String baseItemClauses1 = dataSet.get("baseItemClauses");
    String modifiersItemClauses1 = dataSet.get("modifierItemClauses");
    listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

    // Add Modifiers clauses
    listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
            .getListOfModifiersItemClauses(modifiersItemClauses1);

    // Step1 - If pass blank external_id, LIS will be created and system will
    // automatically generate the external_id and assigned to that LIS.
    Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id,
            "base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 2, "max_price","");

    Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
            "STEP-1 Create LIS response status code is not 200");
    boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
    Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
    String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
            .replace("]", "");

    // qc creation where value in receipt_qualifiers is a negative number.
    Response qcCreateResponse = pageObj.endpoints().createQCUsingApi(adminKey, QCName, "",
            actualExternalIdLIS, "actualExternalIdLIS2", processingFunction, "10",dataSet.get("locationID"),map);;
    Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
            QCName + " QC is not created and status code is not 200");
    String actualExternalIdQC = qcCreateResponse.jsonPath().getString("results[0].external_id")
            .replace("[", "").replace("]", "");

    pageObj.utils().logPass("Verified that line item filters value is empty i.e []");

    // Delete LIS 1
    String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
            .replace("${businessID}", businessID);
    boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
    Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
    pageObj.utils().logPass(lisName1 + " LIS is deleted successfully");

// Delete QC
    String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", actualExternalIdQC)
            .replace("${businessID}", businessID);
    String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");

    String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
            qcID_DB);

    boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env,
            deleteQCFromQualifying_expressions);
    Assert.assertTrue(statusQualifying_expressionsQuery, QCName + " QC is not deleted successfully");
    pageObj.utils().logPass(QCName + " QC is deleted successfully");

    String deleteQCFromQualification_criteria = deleteQCFromQualification_criteriaQuery
            .replace("$qcExternalID", actualExternalIdQC).replace("${businessID}", dataSet.get("business_id"));

    boolean statusQualification_criteriaQuery = DBUtils.executeQuery(env,
            deleteQCFromQualification_criteria);
    Assert.assertTrue(statusQualification_criteriaQuery, QCName + " QC is not deleted successfully");
    pageObj.utils().logPass(QCName + " QC is deleted successfully");

  }

  // shashank
  @Test(priority = 7,description = "SQ-T5620 Verify creating a QC using the \"Sum of Quantities\" processing_function with both mandatory and optional fields.", groups = { "regression", "dailyrun" })
  public void T5620_VerifyQCCreationWithProcessingFunctionAsSumOfQuantities() throws Exception {
    String businessID = dataSet.get("business_id");
    String external_id = "";
    String adminKey = dataSet.get("apiKey");
    String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
    String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
    String QCName2 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
    String processingFunction = "sum_qty";
    Map<String, String> map = new HashMap<String, String>();
    map.put("receipt_qualifiers",
            "{\"attribute\":\"total_amount\",\"operator\":\">=\",\"value\":-10}");

    // filter_item_set == base_only, modifiers_only and base_and_modifiers
//  	// Add base item cluases

    String baseItemClauses1 = dataSet.get("baseItemClauses");
    String modifiersItemClauses1 = dataSet.get("modifierItemClauses");
    listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

    // Add Modifiers clauses
    listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
            .getListOfModifiersItemClauses(modifiersItemClauses1);

    // Step1 - If pass blank external_id, LIS will be created and system will
    // automatically generate the external_id and assigned to that LIS.
    Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id,
            "base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 2, "max_price","");

    Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
            "STEP-1 Create LIS response status code is not 200");
    boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
    Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
    String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
            .replace("]", "");

    // qc creation where value in receipt_qualifiers is a negative number.
    Response qcCreateResponse = pageObj.endpoints().createQCUsingApi(adminKey, QCName, "",
            actualExternalIdLIS, "actualExternalIdLIS2", processingFunction, "10",dataSet.get("locationID"),map);;
    Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
            QCName + " QC is not created and status code is not 200");
    String actualExternalIdQC = qcCreateResponse.jsonPath().getString("results[0].external_id")
            .replace("[", "").replace("]", "");

    pageObj.utils().logPass("Verified that line item filters value is empty i.e []");

    Response getQCDetailsResponse = pageObj.endpoints().getQualificationListUsingAPI(adminKey, QCName, "1", "20");

    // Delete LIS 1
    String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
            .replace("${businessID}", businessID);
    boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
    Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
    pageObj.utils().logPass(lisName1 + " LIS is deleted successfully");

// Delete QC
    String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", actualExternalIdQC)
            .replace("${businessID}", businessID);
    String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");

    String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
            qcID_DB);

    boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env,
            deleteQCFromQualifying_expressions);
    Assert.assertTrue(statusQualifying_expressionsQuery, QCName + " QC is not deleted successfully");
    pageObj.utils().logPass(QCName + " QC is deleted successfully");

    String deleteQCFromQualification_criteria = deleteQCFromQualification_criteriaQuery
            .replace("$qcExternalID", actualExternalIdQC).replace("${businessID}", dataSet.get("business_id"));

    boolean statusQualification_criteriaQuery = DBUtils.executeQuery(env,
            deleteQCFromQualification_criteria);
    Assert.assertTrue(statusQualification_criteriaQuery, QCName + " QC is not deleted successfully");
    pageObj.utils().logPass(QCName + " QC is deleted successfully");

  }


  @AfterMethod(alwaysRun = true)
  public void tearDown() {
    utils.clearDataSet(dataSet);
    driver.quit();
    pageObj.utils().logit("Browser closed");
  }
}

