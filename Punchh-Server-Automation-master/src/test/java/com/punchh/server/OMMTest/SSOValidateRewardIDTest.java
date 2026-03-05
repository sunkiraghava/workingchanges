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
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.OfferIngestionUtilityClass.OfferIngestionUtilities;
import com.punchh.server.annotations.Owner;
import com.punchh.server.api.payloadbuilder.RedeemablePayloadBuilder;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apimodel.lineitemselector.LineItemSelectorFilterItemSet;
import com.punchh.server.apimodel.redeemable.RedeemableReceiptRule;
import com.punchh.server.pages.ApiPayloadObj;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class SSOValidateRewardIDTest {
	private static Logger logger = LogManager.getLogger(SSOValidateRewardIDTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	private Utilities utils;
	private ApiPayloadObj apipayloadObj;
	private String lisExternalID, qcExternalID, redeemableExternalID;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {

		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		lisExternalID = null;
		qcExternalID = null;
		redeemableExternalID = null;
		utils = new Utilities(driver);
		apipayloadObj = new ApiPayloadObj();
	}

	@Test(description = "SQ-T3681 Valid reward id ||"
			+ "SQ-T3890 Validate that name from /single_scan_tokens gets changed to /user_tokens/generate in  and functionality remains same", groups = {
					"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T3681_ValidRewardId() throws Exception {
		// Create LIS Payload with base item 12003
		String lisName = "Automation_LIS_SQ_T3681_" + Utilities.getTimestamp();
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "12003")
				.build();
		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalID, "LIS External ID is null");
		Assert.assertFalse(lisExternalID.isEmpty(), "LIS External ID is empty");
		utils.logPass(lisName + " LIS is Created with External ID: " + lisExternalID);

		// Create QC payload
		String qcname = "Automation_QC_SQ_T3681_" + Utilities.getTimestamp();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname).setStackDiscounting(false)
				.setReuseQualifyingItems(false).setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalID, "", 0).addItemQualifier("line_item_exists", lisExternalID, 0.0, 0)
				.build();
		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null");
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty");
		utils.logPass(qcname + "QC is created with External ID: " + qcExternalID);

		// Create Redeemable Payload with above QC
		String redeemableName = "Automation_Redeemable_SQ_T3681_" + Utilities.getTimestamp();
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName).setDescription(redeemableName)
				.setAutoApplicable(false)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();
		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty");
		utils.logPass(redeemableName + " redeemable is Created with External ID: " + redeemableExternalID);
		// Get Redeemable ID from DB
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID);
		String redeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(redeemableId, "Redeemable ID is null in DB");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API user signup");
		utils.logPass("API2 Signup is successful with user ID: " + userID);

		// send reward amount with redeemable to user
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				redeemableId, "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code mismatch for API2 send message to user");
		utils.logPass("Send reward amount with redeemable to user successfully");
		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableId);
		Assert.assertNotNull(rewardId, "Reward ID is null in response");
		Assert.assertFalse(rewardId.isEmpty(), "Reward ID is empty in response");
		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		// mobile user tokens generate
		Response userTokenGenerateResponse = pageObj.endpoints().ssoUserTokenMobileApi(dataSet.get("client"),
				dataSet.get("secret"), token, rewardId, "PaypalBA", "", "", "", "", "");
		Assert.assertEquals(userTokenGenerateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Mobile user user token generate ");
		String singleScanCode = userTokenGenerateResponse.jsonPath().getString("single_scan_code");
		Assert.assertFalse(singleScanCode.isEmpty(), "Single scan code is empty in response");
		utils.logPass("Mobile user user token generate is successful. Mobile single scan code is: " + singleScanCode);

		Response singleScanCodeResponse = pageObj.endpoints().singleScanCodeWithRedeemableID(dataSet.get("client"),
				dataSet.get("secret"), token, redeemableId);
		Assert.assertEquals(singleScanCodeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the single scan code API");
		String singleScanCode1 = singleScanCodeResponse.jsonPath().getString("single_scan_code");
		Assert.assertFalse(singleScanCode1.isEmpty(), "Single scan code is empty in response");
		utils.logPass("Single Scan Code is successful. Mobile single scan code is: " + singleScanCode1);

		Response singleScanCodeResponse1 = pageObj.endpoints()
				.singleScanCodeWithRewardIDSecureApi(dataSet.get("client"), dataSet.get("secret"), token, rewardId);
		Assert.assertEquals(singleScanCodeResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the single scan code API");
		String singleScanCode2 = singleScanCodeResponse1.jsonPath().getString("single_scan_code");
		Assert.assertFalse(singleScanCode2.isEmpty(), "Single scan code is empty in response");
		utils.logPass("Single Scan Code is successful. Mobile single scan code is " + singleScanCode2);
	}

	@Test(description = "SQ-T3181 [Batched Redemptions] Verify selected_card_number is returned in {{path}}/api/pos/users/find API response when lookup_field is single_scan_code", groups = {
			"nonNightly" })
	@Owner(name = "Hardik Bhardwaj")
	public void T3181_ValidLookupFieldIsSingleScanCode() throws InterruptedException {
		// Instance login and goto timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Cockpit set giftcard adapter/payment adapter/ min max amount
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");
		pageObj.giftcardsPage().selectGiftCardAdapter("Mock Gift Card Adapter");
		pageObj.giftcardsPage().setMinMaxAmountGiftCard("", "");
		pageObj.giftcardsPage().selectPaymentAdapter("Braintree");

		// User creation using api1
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API user signup");
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();
		utils.logPass("API2 Signup is successful with user ID: " + userID);

		// send reward amount to user
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "60", "", "",
				"");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code mismatch for API2 send message to user");
		utils.logPass("Send reward amount to user successfully");

		// Purchase Gift Card API2
		Response purchaseGiftCardResponse = null;
		String uuid = "";
		int attempts = 0;
		while (attempts < 20) {
			try {
				utils.logInfo("API hit count is: " + attempts);
				purchaseGiftCardResponse = pageObj.endpoints().Api2PurchaseGiftCard(dataSet.get("client"),
						dataSet.get("secret"), token, dataSet.get("design_id"));
				uuid = purchaseGiftCardResponse.jsonPath().getString("uuid").toString();
				if (uuid != null) {
					utils.logInfo("Api2 Purchase Gift Card is successful with uuid:" + uuid);
					break;
				}
			} catch (Exception e) {

			}
			attempts++;
		}
		// Ensure Gift Card UUID was returned after all the retries
		Assert.assertNotNull(uuid, "Gift Card UUID is not returned after " + attempts + " retry attempts");
		Assert.assertEquals(purchaseGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API2 gift card purchase");
		String cardNumber = purchaseGiftCardResponse.jsonPath().get("card_number").toString();
		String uuidNumber = purchaseGiftCardResponse.jsonPath().get("uuid").toString();
		String amount = purchaseGiftCardResponse.jsonPath().get("last_fetched_amount").toString();
		Assert.assertNotNull(cardNumber, "Card number is null in purchase gift card response");
		Assert.assertNotNull(uuidNumber, "UUID is null in purchase gift card response");
		Assert.assertNotNull(amount, "Amount is null in purchase gift card response");
		utils.logPass("Gift card is purchased successfully.");

		// Single scan Token
		Response singleScanTokenResponse = pageObj.endpoints().singleScanCodeSecureApi(dataSet.get("client"),
				dataSet.get("secret"), token, "GiftCard", "gift_card_uuid", uuidNumber);
		Assert.assertEquals(singleScanTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the single scan code API");
		String singleScanCode1 = singleScanTokenResponse.jsonPath().getString("single_scan_code");
		Assert.assertFalse(singleScanCode1.isEmpty(), "Single scan code is empty in response");
		utils.logPass("Single Scan Code is successful. Mobile single scan code is: " + singleScanCode1);

		// User lookUp Api
		Response userLookupResponse = pageObj.endpoints().userLookup(singleScanCode1, dataSet.get("locationkey"));
		Assert.assertEquals(userLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the user lookup API");
		String cardNumber1 = userLookupResponse.jsonPath().get("selected_card_number").toString();
		Assert.assertEquals(cardNumber1, cardNumber, "Card Number is not matching");
		utils.logPass("User lookup using single scan code successful. card number " + cardNumber1 + " is matching");

		// Single scan Token
		Response singleScanTokenResponse1 = pageObj.endpoints().singleScanCodeSecureApi(dataSet.get("client"),
				dataSet.get("secret"), token, "PaypalBA", "", "");
		Assert.assertEquals(singleScanTokenResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the single scan code API");
		String singleScanCode2 = singleScanTokenResponse1.jsonPath().getString("single_scan_code");
		Assert.assertFalse(singleScanCode2.isEmpty(), "Single scan code is empty in response");
		utils.logPass("Single Scan Code is successful. Mobile single scan code is: " + singleScanCode2);

		// User lookUp Api
		Response userLookupResponse1 = pageObj.endpoints().userLookup(singleScanCode2, dataSet.get("locationkey"));
		Assert.assertEquals(userLookupResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the user lookup API");
		String cardNumber2 = userLookupResponse1.jsonPath().getString("selected_card_number");
		Assert.assertEquals(cardNumber2, null, "Card Number is not null");
		utils.logPass("User lookup using single scan code successful. card number is: " + cardNumber2);

		// Single scan Token
		String transactionToken = CreateDateTime.getTimeDateString();
		Response singleScanTokenResponse2 = pageObj.endpoints().singleScanCodeSecureApi(dataSet.get("client"),
				dataSet.get("secret"), token, "CreditCard", "transaction_token", transactionToken);
		Assert.assertEquals(singleScanTokenResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the single scan code API");
		String singleScanCode3 = singleScanTokenResponse2.jsonPath().getString("single_scan_code");
		Assert.assertFalse(singleScanCode3.isEmpty(), "Single scan code is empty in response");
		utils.logPass("Single Scan Code is successful. Mobile single scan code is: " + singleScanCode3);

		// User lookUp Api
		Response userLookupResponse2 = pageObj.endpoints().userLookup(singleScanCode3, dataSet.get("locationkey"));
		Assert.assertEquals(userLookupResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the user lookup API");
		String cardNumber3 = userLookupResponse2.jsonPath().getString("selected_card_number");
		Assert.assertEquals(cardNumber3, null, "Card Number is not null");
		utils.logPass("User lookup using single scan code successful. card number is: " + cardNumber3);

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}