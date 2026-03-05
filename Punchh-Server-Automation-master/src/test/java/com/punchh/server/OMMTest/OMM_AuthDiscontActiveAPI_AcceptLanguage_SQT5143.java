package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.punchh.server.annotations.Owner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;

import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

/*
 * @Author : Shashank Sharma 
 */
@Listeners(TestListeners.class)
public class OMM_AuthDiscontActiveAPI_AcceptLanguage_SQT5143 {
	static Logger logger = LogManager.getLogger(OMM_AuthDiscontActiveAPI_AcceptLanguage_SQT5143.class);
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


	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		apiUtils = new ApiUtils();
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

	// For subscription
	@Test(description = "OMM-T2527 (1.0) / SQ-T5143  Auth>Validate multi-language support for API->/auth/discounts/active with default language as English"
			+ "OMM-T2538 / SQ-T5144 POS>Validate multi-language support for API->/pos/discounts/lookup with 'Accept-Language' parameter set to a different language"
			+ "OMM-T2543 (1.0) /  SQ-T5146 "
			+ "OMM-T2549 (1.0) / SQ-T5149\tAPI2 Mobile>Validate multi-language support for API->api2/mobile/discounts/active with 'Accept-Language' parameter set to a different language"
			+ "OMM-T2538 (1.0) / SQ-T5144 (1.0)", groups = { "regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void T5143_VerifySubscriptionAuthDiscontActiveAPILanguage() {

		// naviagte to subscription menu
		String spNameFr = "Ne pas supprimer l'abonnement_OMMT2527";
		String descrpFr = "Ne pas supprimerSubscription_OMMT2527 descriptif";
		String spNameEnglish = "Do Not Delete Subscription_OMMT2527";
		String descriptionForEnglish = "Do Not Delete Subscription_OMMT2527 Description";
		String externalUID1 = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

		String PlanID = dataSet.get("subscriptionId");

		endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";
		String spPrice = "750";

		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);

		int subscription_id = Integer
				.parseInt(purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString());
		utils.logit(userEmail + " purchased " + subscription_id + " Plan id = " + PlanID);

		// Adding subscription into discount basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), "subscription", subscription_id + "");
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String expDiscountBasketItemId = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id");

		// OMM-T2549 (1.0) / SQ-T5149 api2/mobile/discounts/active START
		Response basketDiscountDetailsResponseFr = pageObj.endpoints()
				.getUserDiscountBasketDetailsUsingAPI2WithAcceptLanguage(token, dataSet.get("client"),
						dataSet.get("secret"), "fr");
		Assert.assertEquals(basketDiscountDetailsResponseFr.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		System.out.println("basketDiscountDetailsResponseFr-- " + basketDiscountDetailsResponseFr.asPrettyString());

		String actualSubsNameFR_active = basketDiscountDetailsResponseFr.jsonPath()
				.getString("discount_basket_items[0].discount_details.name").replace("[", "").replace("]", "");

		Assert.assertEquals(actualSubsNameFR_active, spNameFr,
				actualSubsNameFR_active + " language is not matched with " + spNameFr);

		utils.logPass("Verified that subscription name is coming in French language i.e -- " + actualSubsNameFR_active);

		Response basketDiscountDetailsResponseEN = pageObj.endpoints()
				.getUserDiscountBasketDetailsUsingAPI2WithAcceptLanguage(token, dataSet.get("client"),
						dataSet.get("secret"), "en");
		Assert.assertEquals(basketDiscountDetailsResponseEN.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		System.out.println("basketDiscountDetailsResponseEN-- " + basketDiscountDetailsResponseEN.asPrettyString());

		String actualSubsNameEN_active = basketDiscountDetailsResponseEN.jsonPath()
				.getString("discount_basket_items[0].discount_details.name").replace("[", "").replace("]", "");

		Assert.assertEquals(actualSubsNameEN_active, spNameEnglish,
				actualSubsNameEN_active + " language is not matched with " + spNameEnglish);

		utils.logPass("Verified that subscription name is coming in English language i.e -- " + actualSubsNameEN_active);

		// OMM-T2549 (1.0) / SQ-T5149 api2/mobile/discounts/active END

		// ********************** OMM-T2527 (1.0) / SQ-T5143 /api/auth/discounts/active
		// START **********************
		// Adding subscription for french language
		Response basketDiscountDetailsResponseFrench = pageObj.endpoints()
				.getUserDiscountBasketDetailsUsingAUTHWithLanguage(token, dataSet.get("client"), dataSet.get("secret"),
						"fr");
		Assert.assertEquals(basketDiscountDetailsResponseFrench.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		System.out.println(
				"basketDiscountDetailsResponseFrench--- " + basketDiscountDetailsResponseFrench.asPrettyString());

		String actualSubscriptionNameForFrench = basketDiscountDetailsResponseFrench.jsonPath()
				.getString("discount_basket_items[0].discount_details.name").replace("[", "").replace("]", "");

		Assert.assertEquals(actualSubscriptionNameForFrench, spNameFr,
				actualSubscriptionNameForFrench + " language is not matched with " + spNameFr);

		utils.logPass("Verified that subscription name is coming in French language i.e -- "
				+ actualSubscriptionNameForFrench);

		String actualSubscriptionDescriptionForFrench = basketDiscountDetailsResponseFrench.jsonPath()
				.getString("discount_basket_items[0].discount_details.description").replace("[", "").replace("]", "");

		Assert.assertEquals(actualSubscriptionDescriptionForFrench, descrpFr,
				actualSubscriptionDescriptionForFrench + " language is not matched with " + descrpFr);
		utils.logPass("Verified that subscription description is coming in French language i.e -- "
				+ actualSubscriptionDescriptionForFrench);

		// OMM-T2527 (1.0) / SQ-T5143
		Response basketDiscountDetailsResponseEnglish = pageObj.endpoints()
				.getUserDiscountBasketDetailsUsingAUTHWithLanguage(token, dataSet.get("client"), dataSet.get("secret"),
						"en");
		Assert.assertEquals(basketDiscountDetailsResponseEnglish.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		System.out.println(
				"basketDiscountDetailsResponseEnglish----- " + basketDiscountDetailsResponseEnglish.asPrettyString());

		String actualSubscriptionNameForEnglish = basketDiscountDetailsResponseEnglish.jsonPath()
				.getString("discount_basket_items[0].discount_details.name").replace("[", "").replace("]", "");

		Assert.assertEquals(actualSubscriptionNameForEnglish, spNameEnglish,
				actualSubscriptionNameForEnglish + " language is not matched with " + spNameEnglish);

		utils.logPass("Verified that subscription name is coming in English language i.e -- "
				+ actualSubscriptionNameForEnglish);

		String actualSubscriptionDescriptionForEnglish = basketDiscountDetailsResponseEnglish.jsonPath()
				.getString("discount_basket_items[0].discount_details.description").replace("[", "").replace("]", "");

		Assert.assertEquals(actualSubscriptionDescriptionForEnglish, descriptionForEnglish,
				actualSubscriptionDescriptionForEnglish + " language is not matched with " + descriptionForEnglish);
		utils.logPass("Verified that subscription description is coming in English language i.e -- "
				+ actualSubscriptionDescriptionForEnglish);
		// ********************** OMM-T2527 (1.0) / SQ-T5143 /api/auth/discounts/active
		// END **********************

		// ********************** OMM-T2538 (1.0) / SQ-T5144 (1.0)
		// /api/pos/discounts/lookup START **********************

		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.POSDiscountLookupWithLanguage(dataSet.get("locationkey"), userID, "101", "fr");
		System.out.println("batchRedemptionProcessResponse-- " + batchRedemptionProcessResponse.asPrettyString());

		String actualSubscriptionForFrench_1 = batchRedemptionProcessResponse.jsonPath()
				.getString("selected_discounts[0].discount_details.name").replace("[", "").replace("]", "");
		Assert.assertEquals(actualSubscriptionForFrench_1, spNameFr,
				actualSubscriptionForFrench_1 + " is not matched with " + spNameFr);

		utils.logPass("Verified that subscription Name is matched in French language i.e -- "
				+ actualSubscriptionForFrench_1);

		String actualSubscriptionDesForFrench_1 = batchRedemptionProcessResponse.jsonPath()
				.getString("selected_discounts[0].discount_details.description").replace("[", "").replace("]", "");
		Assert.assertEquals(actualSubscriptionDesForFrench_1, descrpFr,
				actualSubscriptionDesForFrench_1 + " is not matched with " + descrpFr);

		utils.logPass("Verified that subscription description is matched in French language i.e -- "
				+ actualSubscriptionDesForFrench_1);

		// OMM-T2538 (1.0) // Discount lookup API For English /api/pos/discounts/lookup
		Response batchRedemptionProcessResponse_English = pageObj.endpoints()
				.POSDiscountLookupWithLanguage(dataSet.get("locationkey"), userID, "101", "en");

		String actualSubscriptionForEnglish_1 = batchRedemptionProcessResponse_English.jsonPath()
				.getString("selected_discounts[0].discount_details.name").replace("[", "").replace("]", "");
		Assert.assertEquals(actualSubscriptionForEnglish_1, spNameEnglish,
				actualSubscriptionForEnglish_1 + " is not matched with " + spNameEnglish);

		utils.logPass("Verified that subscription Name is matched in English language i.e -- "
				+ actualSubscriptionForEnglish_1);

		String actualSubscriptionDesForEnglish_1 = batchRedemptionProcessResponse_English.jsonPath()
				.getString("selected_discounts[0].discount_details.description").replace("[", "").replace("]", "");
		Assert.assertEquals(actualSubscriptionDesForEnglish_1, descriptionForEnglish,
				actualSubscriptionDesForEnglish_1 + " is not matched with " + descriptionForEnglish);

		utils.logPass("Verified that subscription description is matched in English language i.e -- "
				+ actualSubscriptionDesForEnglish_1);

		// ********************** OMM-T2538 (1.0) / SQ-T5144 (1.0)
		// /api/pos/discounts/lookup END **********************

		// ********************** OMM-T2543 (1.0) / SQ-T5146 /api/pos/batch_redemptions
		// START **********************

		// send reward amount to user Reedemable
				Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "20",
						"", "", "100");

				utils.logit("Send redeemable to the user successfully");
				Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, 
						"Status code 201 did not matched for api2 send message to user");
				utils.logPass("Api2  send reward amount to user is successful");
		String externalUID2 = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

		// POS fetch active basket
		Response posBatchRedemptionResponse_fr = pageObj.endpoints().posBatchRedemptionWithQueryTrueWithAcceptLanguage(
				dataSet.get("locationkey"), userID, "101", externalUID2, "fr");

		Assert.assertEquals(posBatchRedemptionResponse_fr.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actualPOSBatchRedeemableNameForFrench = posBatchRedemptionResponse_fr.jsonPath()
				.getString("success[0].discount_details.name").replace("[", "").replace("]", "");

		Assert.assertEquals(actualPOSBatchRedeemableNameForFrench, spNameFr,
				actualPOSBatchRedeemableNameForFrench + " language is not matched with " + spNameFr);

		utils.logPass("Verified that subscription name is coming in French language i.e -- "
				+ actualPOSBatchRedeemableNameForFrench);

		String actualPOSRedemptionRedeemableDescriptionForFrench = posBatchRedemptionResponse_fr.jsonPath()
				.getString("success[0].discount_details.description").replace("[", "").replace("]", "");

		Assert.assertEquals(actualPOSRedemptionRedeemableDescriptionForFrench, descrpFr,
				actualPOSRedemptionRedeemableDescriptionForFrench + " language is not matched with " + descrpFr);
		utils.logPass("Verified that subscription description is coming in French language i.e -- "
				+ actualPOSRedemptionRedeemableDescriptionForFrench);

		// OMM-T2543 (1.0) / SQ-T5146
		// POS fetch active basket /api/pos/batch_redemptions
		Response posBatchRedemptionResponse_en = pageObj.endpoints().posBatchRedemptionWithQueryTrueWithAcceptLanguage(
				dataSet.get("locationkey"), userID, "101", externalUID2, "en");

		Assert.assertEquals(posBatchRedemptionResponse_fr.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actualPOSBatchRedeemableNameForEnglish = posBatchRedemptionResponse_en.jsonPath()
				.getString("success[0].discount_details.name").replace("[", "").replace("]", "");

		Assert.assertEquals(actualPOSBatchRedeemableNameForEnglish, spNameEnglish,
				actualPOSBatchRedeemableNameForEnglish + " language is not matched with " + spNameEnglish);

		utils.logPass("Verified that subscription name is coming in English language i.e -- "
				+ actualPOSBatchRedeemableNameForEnglish);

		String actualPOSRedemptionRedeemableDescriptionForEnglish = posBatchRedemptionResponse_en.jsonPath()
				.getString("success[0].discount_details.description").replace("[", "").replace("]", "");

		Assert.assertEquals(actualPOSRedemptionRedeemableDescriptionForEnglish, descriptionForEnglish,
				actualPOSRedemptionRedeemableDescriptionForEnglish + " language is not matched with "
						+ descriptionForEnglish);
		utils.logPass("Verified that subscription description is coming in English language i.e -- "
				+ actualPOSRedemptionRedeemableDescriptionForEnglish);

		// OMM-T2540 (1.0) / SQ-T5145
		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "",
				"100");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode());

		Response actualPOSRedemptionRedeemableDescriptionForEnglishT5145 = pageObj.endpoints()
				.authListDiscountBasketAddedForPOSAPIWithLanguage(dataSet.get("locationkey"), userID, "discount_amount",
						"5", "en");

		Assert.assertEquals(actualPOSRedemptionRedeemableDescriptionForEnglishT5145.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actualPOSBatchRedeemableNameForEnglishT5145 = actualPOSRedemptionRedeemableDescriptionForEnglishT5145
				.jsonPath().getString("discount_basket_items[0].discount_details.name").replace("[", "")
				.replace("]", "");

		Assert.assertEquals(actualPOSBatchRedeemableNameForEnglishT5145, spNameEnglish,
				actualPOSBatchRedeemableNameForEnglishT5145 + " language is not matched with " + spNameEnglish);

		utils.logPass("Verified that subscription name is coming in English language i.e -- "
				+ actualPOSBatchRedeemableNameForEnglishT5145);

		String actualRedeemableDescriptionT5145 = actualPOSRedemptionRedeemableDescriptionForEnglishT5145.jsonPath()
				.getString("discount_basket_items[0].discount_details.description").replace("[", "").replace("]", "");

		Assert.assertEquals(actualRedeemableDescriptionT5145, descriptionForEnglish,
				actualRedeemableDescriptionT5145 + " language is not matched with " + descriptionForEnglish);
		utils.logPass("Verified that subscription description is coming in English language i.e -- "
				+ actualRedeemableDescriptionT5145);

		// ********************** OMM-T2543 (1.0) / SQ-T5146 /api/pos/batch_redemptions
		// END **********************

		/*
		 * // Need to be check payload for auth api // OMM-T2548 (1.0) / SQ-T5147
		 * api/auth/batch_redemptions START
		 * 
		 * // Auth Process Batch Redemption Response batchRedemptionProcessResponse2 =
		 * pageObj.endpoints().processBatchRedemptionAUTHAPIWithAcceptLanguage(
		 * dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationKey"),
		 * token, userID, "101", externalUID1, "fr");
		 * 
		 * System.out.println("batchRedemptionProcessResponse2-- " +
		 * batchRedemptionProcessResponse2.asPrettyString());
		 * Assert.assertEquals(batchRedemptionProcessResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
		 * "Status code 200 did not match with Process Batch Redemption ");
		 * 
		 * // OMM-T2548 (1.0) / SQ-T5147 api/auth/batch_redemptions END
		 * 
		 */

		// OMM-T2546 (1.0) / SQ-T5148 /auth/discounts/select START

		// need to add code here

		// OMM-T2546 (1.0) / SQ-T5148 /auth/discounts/select END

		// OMM-T2554 (1.0) /SQ-T5150 api/mobile/discounts/unselect
		expDiscountBasketItemId = expDiscountBasketItemId.replace("[", "").replace("]", "");
		Response deleteBasketResponseFr = pageObj.endpoints().deleteDiscountBasketForUserAPIWithAcceptLanguage(token,
				dataSet.get("client"), dataSet.get("secret"), expDiscountBasketItemId, "fr");

		System.out.println("deleteBasketResponseFr-- " + deleteBasketResponseFr.asPrettyString());

	}

	// For Reward
	@Test(description = "OMM-T2527 (1.0) / SQ-T5143  Auth>Validate multi-language support for API->/auth/discounts/active with default language as English"
			+ "OMM-T2538 / SQ-T5144 POS>Validate multi-language support for API->/pos/discounts/lookup with 'Accept-Language' parameter set to a different language"
			+ "OMM-T2543 (1.0) /  SQ-T5146	POS>Validate multi-language support for API->api/pos/batch_redemptions with 'Accept-Language' parameter set to a different language"
			+ "OMM-T2554 (1.0) /SQ-T5150\tAPI Mobile>Validate multi-language support for API->api/mobile/discounts/unselect with 'Accept-Language' parameter set to a different language", groups = { "regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void T5134_VerifyApiAcceptLanguageForReward() {

		String redeemableNameEnglish = "Do No tDelete Automation T5143";
		String redeemableDescriptionEnglish = "Do No tDelete Automation T5143 Description";
		String redeemableNameFrench = "Ne pas supprimer l'automatisation T5143";
		String redeemableDescriptionFrench = "Ne pas supprimer l'automatisation T5143 descriptif";

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID = rewardResponse.jsonPath().getString("id").replace("[", "").replace("]", "");

		utils.logit("Reward id " + rewardID + " is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), "reward", rewardID);

		String expDiscountBasketItemId = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id");

		// OMM-T2549 (1.0) / SQ-T5149 api2/mobile/discounts/active START
		Response basketDiscountDetailsResponseFr = pageObj.endpoints()
				.getUserDiscountBasketDetailsUsingAPI2WithAcceptLanguage(token, dataSet.get("client"),
						dataSet.get("secret"), "fr");
		Assert.assertEquals(basketDiscountDetailsResponseFr.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		System.out.println("basketDiscountDetailsResponseFr-- " + basketDiscountDetailsResponseFr.asPrettyString());

		String actualRedeemableNameFR_active = basketDiscountDetailsResponseFr.jsonPath()
				.getString("discount_basket_items[0].discount_details.name").replace("[", "").replace("]", "");

		Assert.assertEquals(actualRedeemableNameFR_active, redeemableNameFrench,
				actualRedeemableNameFR_active + " language is not matched with " + redeemableNameFrench);

		utils.logPass("Verified that Redeemable name is coming in French language i.e -- " + actualRedeemableNameFR_active);

		Response basketDiscountDetailsResponseRedeemableEN = pageObj.endpoints()
				.getUserDiscountBasketDetailsUsingAPI2WithAcceptLanguage(token, dataSet.get("client"),
						dataSet.get("secret"), "en");
		Assert.assertEquals(basketDiscountDetailsResponseRedeemableEN.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		System.out.println(
				"basketDiscountDetailsResponseEN-- " + basketDiscountDetailsResponseRedeemableEN.asPrettyString());

		String actualRedeemableNameEN_active = basketDiscountDetailsResponseRedeemableEN.jsonPath()
				.getString("discount_basket_items[0].discount_details.name").replace("[", "").replace("]", "");

		Assert.assertEquals(actualRedeemableNameEN_active, redeemableNameEnglish,
				actualRedeemableNameEN_active + " language is not matched with " + redeemableNameEnglish);

		utils.logPass("Verified that Redeemable name is coming in English language i.e -- " + actualRedeemableNameEN_active);

		// OMM-T2549 (1.0) / SQ-T5149 api2/mobile/discounts/active START -- END

		// ********************** OMM-T2543 (1.0) / SQ-T5146 /api/pos/batch_redemptions
		// START **********************

		String externalUID2 = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

		// POS fetch active basket
		Response posBatchRedemptionResponse_fr = pageObj.endpoints().posBatchRedemptionWithQueryTrueWithAcceptLanguage(
				dataSet.get("locationKey"), userID, "101", externalUID2, "fr");

		Assert.assertEquals(posBatchRedemptionResponse_fr.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isPosBatchRedemptionSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.posBatchRedemptionSchema, posBatchRedemptionResponse_fr.asString());
		Assert.assertTrue(isPosBatchRedemptionSchemaValidated, "POS API Batch Redemption Schema Validation failed");
		String actualPOSBatchRedeemableNameForFrench = posBatchRedemptionResponse_fr.jsonPath()
				.getString("success[0].discount_details.name").replace("[", "").replace("]", "");

		Assert.assertEquals(actualPOSBatchRedeemableNameForFrench, redeemableNameFrench,
				actualPOSBatchRedeemableNameForFrench + " language is not matched with " + redeemableNameFrench);

		utils.logPass("Verified that subscription name is coming in French language i.e -- "
				+ actualPOSBatchRedeemableNameForFrench);

		String actualPOSRedemptionRedeemableDescriptionForFrench = posBatchRedemptionResponse_fr.jsonPath()
				.getString("success[0].discount_details.description").replace("[", "").replace("]", "");

		Assert.assertEquals(actualPOSRedemptionRedeemableDescriptionForFrench, redeemableDescriptionFrench,
				actualPOSRedemptionRedeemableDescriptionForFrench + " language is not matched with "
						+ redeemableDescriptionFrench);
		utils.logPass("Verified that subscription description is coming in French language i.e -- "
				+ actualPOSRedemptionRedeemableDescriptionForFrench);

		// OMM-T2543 (1.0) / SQ-T5146
		// POS fetch active basket /api/pos/batch_redemptions
		Response posBatchRedemptionResponse_en = pageObj.endpoints().posBatchRedemptionWithQueryTrueWithAcceptLanguage(
				dataSet.get("locationKey"), userID, "101", externalUID2, "en");

		Assert.assertEquals(posBatchRedemptionResponse_fr.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actualPOSBatchRedeemableNameForEnglish = posBatchRedemptionResponse_en.jsonPath()
				.getString("success[0].discount_details.name").replace("[", "").replace("]", "");

		Assert.assertEquals(actualPOSBatchRedeemableNameForEnglish, redeemableNameEnglish,
				actualPOSBatchRedeemableNameForEnglish + " language is not matched with " + redeemableNameEnglish);

		utils.logPass("Verified that subscription name is coming in English language i.e -- "
				+ actualPOSBatchRedeemableNameForEnglish);

		String actualPOSRedemptionRedeemableDescriptionForEnglish = posBatchRedemptionResponse_en.jsonPath()
				.getString("success[0].discount_details.description").replace("[", "").replace("]", "");

		Assert.assertEquals(actualPOSRedemptionRedeemableDescriptionForEnglish, redeemableDescriptionEnglish,
				actualPOSRedemptionRedeemableDescriptionForEnglish + " language is not matched with "
						+ redeemableDescriptionEnglish);
		utils.logPass("Verified that subscription description is coming in English language i.e -- "
				+ actualPOSRedemptionRedeemableDescriptionForEnglish);

		// ********************** OMM-T2543 (1.0) / SQ-T5146 /api/pos/batch_redemptions
		// END **********************

		// ********************** OMM-T2527 (1.0) / SQ-T5143 /api/auth/discounts/active
		// START **********************

		Response basketDiscountDetailsResponseFrench = pageObj.endpoints()
				.getUserDiscountBasketDetailsUsingAUTHWithLanguage(token, dataSet.get("client"), dataSet.get("secret"),
						"fr");
		Assert.assertEquals(basketDiscountDetailsResponseFrench.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actualRedeemableNameForFrench = basketDiscountDetailsResponseFrench.jsonPath()
				.getString("discount_basket_items[0].discount_details.name").replace("[", "").replace("]", "");

		Assert.assertEquals(actualRedeemableNameForFrench, redeemableNameFrench,
				actualRedeemableNameForFrench + " language is not matched with " + redeemableNameFrench);

		utils.logPass("Verified that subscription name is coming in French language i.e -- " + actualRedeemableNameForFrench);

		String actualRedeemableDescriptionForFrench = basketDiscountDetailsResponseFrench.jsonPath()
				.getString("discount_basket_items[0].discount_details.description").replace("[", "").replace("]", "");

		Assert.assertEquals(actualRedeemableDescriptionForFrench, redeemableDescriptionFrench,
				actualRedeemableDescriptionForFrench + " language is not matched with " + redeemableDescriptionFrench);
		utils.logPass("Verified that subscription description is coming in French language i.e -- "
				+ actualRedeemableDescriptionForFrench);

		/// api/auth/discounts/active
		Response basketDiscountDetailsResponseEnglish = pageObj.endpoints()
				.getUserDiscountBasketDetailsUsingAUTHWithLanguage(token, dataSet.get("client"), dataSet.get("secret"),
						"en");
		Assert.assertEquals(basketDiscountDetailsResponseEnglish.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		System.out.println(
				"basketDiscountDetailsResponseEnglish----- " + basketDiscountDetailsResponseEnglish.asPrettyString());

		String actualRedeemableNameForEnglish1 = basketDiscountDetailsResponseEnglish.jsonPath()
				.getString("discount_basket_items[0].discount_details.name").replace("[", "").replace("]", "");

		Assert.assertEquals(actualRedeemableNameForEnglish1, redeemableNameEnglish,
				actualRedeemableNameForEnglish1 + " language is not matched with " + redeemableNameEnglish);

		utils.logPass("Verified that subscription name is coming in English language i.e -- "
				+ actualRedeemableNameForEnglish1);

		String actualRedeemableDescriptionForEnglish1 = basketDiscountDetailsResponseEnglish.jsonPath()
				.getString("discount_basket_items[0].discount_details.description").replace("[", "").replace("]", "");

		Assert.assertEquals(actualRedeemableDescriptionForEnglish1, redeemableDescriptionEnglish,
				actualRedeemableDescriptionForEnglish1 + " language is not matched with "
						+ redeemableDescriptionEnglish);
		utils.logPass("Verified that subscription description is coming in English language i.e -- "
				+ actualRedeemableDescriptionForEnglish1);

		// ********************** OMM-T2527 (1.0) / SQ-T5143 /api/auth/discounts/active
		// END **********************

		// ********************** OMM-T2538 (1.0) / SQ-T5144 (1.0)
		// /api/pos/discounts/lookup START **********************

		// OMM-T2538 (1.0) // Discount lookup API for french /api/pos/discounts/lookup
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.POSDiscountLookupWithLanguage(dataSet.get("locationKey"), userID, "101", "fr");

		String actualNameForFrench_1 = batchRedemptionProcessResponse.jsonPath()
				.getString("selected_discounts[0].discount_details.name").replace("[", "").replace("]", "");
		Assert.assertEquals(actualNameForFrench_1, redeemableNameFrench,
				actualNameForFrench_1 + " is not matched with " + redeemableNameFrench);

		utils.logPass("Verified that subscription Name is matched in French language i.e -- " + actualNameForFrench_1);

		String actualDescriptionForFrench_1 = batchRedemptionProcessResponse.jsonPath()
				.getString("selected_discounts[0].discount_details.description").replace("[", "").replace("]", "");
		Assert.assertEquals(actualDescriptionForFrench_1, redeemableDescriptionFrench,
				actualDescriptionForFrench_1 + " is not matched with " + redeemableDescriptionFrench);

		utils.logPass("Verified that subscription description is matched in French language i.e -- "
				+ actualDescriptionForFrench_1);

		// OMM-T2538 (1.0) // Discount lookup API For English /api/pos/discounts/lookup
		Response batchRedemptionProcessResponse_English = pageObj.endpoints()
				.POSDiscountLookupWithLanguage(dataSet.get("locationKey"), userID, "101", "en");
		System.out.println(
				"batchRedemptionProcessResponse_English-- " + batchRedemptionProcessResponse_English.asPrettyString());

		String actualRedeemableForEnglish_1 = batchRedemptionProcessResponse_English.jsonPath()
				.getString("selected_discounts[0].discount_details.name").replace("[", "").replace("]", "");
		Assert.assertEquals(actualRedeemableForEnglish_1, redeemableNameEnglish,
				actualRedeemableForEnglish_1 + " is not matched with " + redeemableNameEnglish);

		utils.logPass("Verified that subscription Name is matched in English language i.e -- "
				+ actualRedeemableForEnglish_1);

		String actualDescriptionForEnglish_1 = batchRedemptionProcessResponse_English.jsonPath()
				.getString("selected_discounts[0].discount_details.description").replace("[", "").replace("]", "");
		Assert.assertEquals(actualDescriptionForEnglish_1, redeemableDescriptionEnglish,
				actualDescriptionForEnglish_1 + " is not matched with " + redeemableDescriptionEnglish);

		utils.logPass("Verified that subscription description is matched in English language i.e -- "
				+ actualDescriptionForEnglish_1);

		// ********************** OMM-T2538 (1.0) / SQ-T5144 (1.0)
		// /api/pos/discounts/lookup END **********************

		// OMM-T2554 (1.0) /SQ-T5150 api/mobile/discounts/unselect
		expDiscountBasketItemId = expDiscountBasketItemId.replace("[", "").replace("]", "");
		Response deleteBasketResponseFr = pageObj.endpoints().deleteDiscountBasketForUserAPIWithAcceptLanguage(token,
				dataSet.get("client"), dataSet.get("secret"), expDiscountBasketItemId, "fr");

		Assert.assertEquals(deleteBasketResponseFr.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

	}

	@Test(description = "OMM-T2527 (1.0) / SQ-T5143  Auth>Validate multi-language support for API->/auth/discounts/active with default language as English"
			+ "OMM-T2538 / SQ-T5144 POS>Validate multi-language support for API->/pos/discounts/lookup with 'Accept-Language' parameter set to a different language", groups = {
					"unstable", "regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void T5134_VerifyApiAcceptLanguageForCode() throws InterruptedException, ParseException {
		String coupanCampaignName = "Auto_CuponCampaign" + CreateDateTime.getTimeDateString();
		String englishMetadata = "English Metadata Info For Coupon";
		String frenchMetadata = "Informations sur les métadonnées en anglais pour le coupon";
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		
		//set alternate language (french)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Business");
		pageObj.dashboardpage().navigateToTabs("Address");
		pageObj.settingsPage().alternateLanguages();
		pageObj.settingsPage().clickSaveBtn();
		
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(coupanCampaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setMetadataInCouponCamp(frenchMetadata, "fr");
		pageObj.signupcampaignpage().setMetadataInCouponCamp(englishMetadata, "en");

		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), "", "",
				GlobalBenefitRedemptionThrottlingToggle);
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Campaign created success message did not displayed....");
		utils.logPass("Coupon campaign created successfuly");

		Thread.sleep(8000);
		pageObj.campaignspage().searchCampaign(coupanCampaignName);
		String generatedCodeName = pageObj.campaignspage().getPreGeneratedCuponCode();

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), "redemption_code", generatedCodeName);

		// ********************** OMM-T2543 (1.0) / SQ-T5146 /api/pos/batch_redemptions
		// START **********************

		// OMM-T2543 (1.0) / SQ-T5146
		String externalUID2 = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

		// POS fetch active basket
		Response posBatchRedemptionResponse_fr = pageObj.endpoints().posBatchRedemptionWithQueryTrueWithAcceptLanguage(
				dataSet.get("locationKey"), userID, "101", externalUID2, "fr");

		Assert.assertEquals(posBatchRedemptionResponse_fr.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actualPOSBatchRedeemableNameForFrenchMetdata = posBatchRedemptionResponse_fr.jsonPath()
				.getString("success[0].meta_data").replace("[", "").replace("]", "");

		Assert.assertEquals(actualPOSBatchRedeemableNameForFrenchMetdata, frenchMetadata,
				actualPOSBatchRedeemableNameForFrenchMetdata + " meta data is not matched with "
						+ actualPOSBatchRedeemableNameForFrenchMetdata);

		utils.logPass("Verified that Meta data  in French language i.e -- " + actualPOSBatchRedeemableNameForFrenchMetdata);

		// OMM-T2543 (1.0) / SQ-T5146
		// POS fetch active basket
		Response posBatchRedemptionResponse_en = pageObj.endpoints().posBatchRedemptionWithQueryTrueWithAcceptLanguage(
				dataSet.get("locationKey"), userID, "101", externalUID2, "en");

		Assert.assertEquals(posBatchRedemptionResponse_fr.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actualPOSBatchRedeemableNameForEnglishMetadata = posBatchRedemptionResponse_en.jsonPath()
				.getString("success[0].meta_data").replace("[", "").replace("]", "");

		Assert.assertEquals(actualPOSBatchRedeemableNameForEnglishMetadata, englishMetadata,
				actualPOSBatchRedeemableNameForEnglishMetadata + " Metadata is not matched with " + englishMetadata);

		utils.logPass("Verified that Metadata is coming in English language i.e -- "
				+ actualPOSBatchRedeemableNameForEnglishMetadata);

		// ********************** OMM-T2543 (1.0) / SQ-T5146 /api/pos/batch_redemptions
		// END **********************

		// ********************** OMM-T2527 (1.0) / SQ-T5143 /api/auth/discounts/active
		// START **********************

		// Adding subscription for french language
		Response basketDiscountDetailsResponseFrench = pageObj.endpoints()
				.getUserDiscountBasketDetailsUsingAUTHWithLanguage(token, dataSet.get("client"), dataSet.get("secret"),
						"fr");

		Assert.assertEquals(basketDiscountDetailsResponseFrench.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actualMetaDataForFrench = basketDiscountDetailsResponseFrench.jsonPath()
				.getString("discount_basket_items[0].discount_details.meta_detail").replace("[", "").replace("]", "");

		Assert.assertEquals(actualMetaDataForFrench, frenchMetadata,
				actualMetaDataForFrench + " language is not matched with " + frenchMetadata);

		utils.logPass("Verified that metadata is matched in French language i.e -- " + actualMetaDataForFrench);

		Response basketDiscountDetailsResponseEnglish = pageObj.endpoints()
				.getUserDiscountBasketDetailsUsingAUTHWithLanguage(token, dataSet.get("client"), dataSet.get("secret"),
						"en");
		Assert.assertEquals(basketDiscountDetailsResponseEnglish.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actualMetaDataForEnglish = basketDiscountDetailsResponseEnglish.jsonPath()
				.getString("discount_basket_items[0].discount_details.meta_detail").replace("[", "").replace("]", "");

		Assert.assertEquals(actualMetaDataForEnglish, englishMetadata,
				actualMetaDataForEnglish + " language is not matched with " + englishMetadata);

		utils.logPass("Verified that Metadata is matched in English language i.e -- " + actualMetaDataForEnglish);

		// ********************** OMM-T2527 (1.0) / SQ-T5143 /api/auth/discounts/active
		// END **********************

		// ********************** OMM-T2538 (1.0) / SQ-T5144 (1.0)
		// /api/pos/discounts/lookup START **********************

		// OMM-T2538 (1.0) // Discount lookup API for french
		Response batchRedemptionProcessResponseFrench = pageObj.endpoints()
				.POSDiscountLookupWithLanguage(dataSet.get("locationKey"), userID, "101", "fr");

		String actualMetaDataForFrench1 = batchRedemptionProcessResponseFrench.jsonPath()
				.getString("selected_discounts[0].meta_data").replace("[", "").replace("]", "");

		Assert.assertEquals(actualMetaDataForFrench, frenchMetadata,
				actualMetaDataForFrench + " language is not matched with " + frenchMetadata);

		utils.logPass("Verified that metadata is matched for discount lookup api in French i.e -- "
				+ actualMetaDataForFrench);

		// OMM-T2538 (1.0) // Discount lookup API for french
		Response batchRedemptionProcessResponseEnglish = pageObj.endpoints()
				.POSDiscountLookupWithLanguage(dataSet.get("locationKey"), userID, "101", "en");

		String actualMetaDataForEnglish1 = batchRedemptionProcessResponseEnglish.jsonPath()
				.getString("selected_discounts[0].meta_data").replace("[", "").replace("]", "");

		Assert.assertEquals(actualMetaDataForEnglish1, englishMetadata,
				actualMetaDataForEnglish1 + " language is not matched with " + englishMetadata);

		utils.logPass("Verified that metadata is matched in English language i.e -- " + actualMetaDataForEnglish1);
		// ********************** OMM-T2538 (1.0) / SQ-T5144 (1.0)
		// /api/pos/discounts/lookup END **********************

	}

	// For Reward
	@Test(description = "OMM-T2548 (1.0) / SQ-T5147	Auth>Validate multi-language support for API->api/auth/batch_redemptions with 'Accept-Language' parameter set to a different language", groups = { "regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void T5147_VerifyApiAcceptLanguageForRewardBbatchRedemption() throws InterruptedException {

		String redeemableNameEnglish = "Do NotDelete Automation T5143";
		String redeemableDescriptionEnglish = "Do NotDelete Automation T5143 Description";
		String redeemableNameFrench = "Ne pas supprimer l'automatisation T5143";
		String redeemableDescriptionFrench = "Ne pas supprimer l'automatisation T5143 descriptif";

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.dashboardpage().navigateToTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Reward Locking","uncheck");

		pageObj.menupage().navigateToSubMenuItem("Settings", "Business");
		pageObj.dashboardpage().navigateToTabs("Address");
		pageObj.settingsPage().selectAlternateLanguage("French");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID = rewardResponse.jsonPath().getString("id").replace("[", "").replace("]", "");

		utils.logit("Reward id " + rewardID + " is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), "reward", rewardID);

		String expDiscountBasketItemId = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id");

		String externalUID1 = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// Auth Process Batch Redemption
		Response batchRedemptionProcessResponseFrench = pageObj.endpoints()
				.processBatchRedemptionAUTHAPIWithAcceptLanguage(dataSet.get("client"), dataSet.get("secret"),
						dataSet.get("locationkey"), token, userID, "12003", externalUID1, "fr");
		Assert.assertEquals(batchRedemptionProcessResponseFrench.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isAuthBatchRedemptionSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authBatchRedemptionSchema, batchRedemptionProcessResponseFrench.asString());
		Assert.assertTrue(isAuthBatchRedemptionSchemaValidated, "Auth API Batch Redemption Schema Validation failed");
		String actualFrName = batchRedemptionProcessResponseFrench.jsonPath()
				.getString("success[0].discount_details.name").replace("[", "").replace("]", "");
		Assert.assertEquals(actualFrName, redeemableNameFrench, actualFrName + " actual french name is not matched");

		String actualFrDescriptionName = batchRedemptionProcessResponseFrench.jsonPath()
				.getString("success[0].discount_details.description").replace("[", "").replace("]", "");
		Assert.assertEquals(actualFrDescriptionName, redeemableDescriptionFrench,
				actualFrDescriptionName + " actual french redeemable description  is not matched");

	}

	@Test(description = "OMM-T2605 (1.0)   / SQ-T5158	POS>Validate that Applicable offers API end point considers the “channel” configured in Receipt Qualifier while evaluating the Qualification Criteria" +
			"SQ-T5809 POS > Validate that pos discount look up endpoint considers the “channel” configured in Receipt Qualifier while evaluating the Qualification Criteria ", groups = { "regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void T5158_ValidateReceiptQualifierForEvaluatingtheQC() throws InterruptedException {

		String redeemableNameForPOS = dataSet.get("redeemableNameForPOS");
		String redeemableDescriptionPOS = dataSet.get("redeemableDescriptionPOS");
		String redeemableIDForPOS = dataSet.get("redeemableIDForPOS");

		String redeemableNameForPosAndKiosk = dataSet.get("redeemableNameForPosKiosk");
		String redeemableDescriptionPosAndKiosk = dataSet.get("redeemableDescriptionPosKiosk");
		String redeemableIDForPosAndKiosk = dataSet.get("redeemableIDForPosKiosk");

		String redeemableNameForKiosk = dataSet.get("redeemableNameForKiosk");
		String redeemableDescriptionKiosk = dataSet.get("redeemableDescriptionKiosk");
		String redeemableIDForKiosk = dataSet.get("redeemableIDForKiosk");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		System.out.println("signUpResponse-- " + signUpResponse.asPrettyString());
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableIDForPOS, "", "");

		utils.logit("===Applicable offers API===");
		Response applicableresponse = pageObj.endpoints().posApplicableOffer(userEmail, "30",
				dataSet.get("locationkey"));
		System.out.println("applicableresponse-- " + applicableresponse.asPrettyString());
		List<Object> actualRedeemableNameList = applicableresponse.jsonPath().getList("reward.name");

		Assert.assertTrue(actualRedeemableNameList.contains(redeemableNameForPOS),
				redeemableNameForPOS + " expected is not in the json redeemable list.");

		List<Object> actualRedeemableDescriptionList = applicableresponse.jsonPath().getList("reward.description");

		Assert.assertTrue(actualRedeemableDescriptionList.contains(redeemableDescriptionPOS),
				redeemableDescriptionPOS + " expected description of redeemable is not matched");

		// User SignUp using API
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api2SignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse2, "API 2 user signup");
		String user2_Token = signUpResponse2.jsonPath().get("access_token.token").toString();

		String userID2 = signUpResponse2.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID2, dataSet.get("apiKey"), "",
				redeemableIDForPosAndKiosk, "", "");
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");

		utils.logit("===Applicable offers API===");
		Response applicableresponse2 = pageObj.endpoints().posApplicableOffer(userEmail2, "30",
				dataSet.get("locationkey"));

		System.out.println("applicableresponse2--- " + applicableresponse2.asPrettyString());
		List<Object> actualRedeemableNameList2 = applicableresponse2.jsonPath().getList("reward.name");

		Assert.assertTrue(actualRedeemableNameList2.contains(redeemableNameForPosAndKiosk),
				redeemableNameForPosAndKiosk + " expected is not in the json redeemable list.");

		List<Object> actualRedeemableDescriptionList2 = applicableresponse2.jsonPath().getList("reward.description");

		Assert.assertTrue(actualRedeemableDescriptionList2.contains(redeemableDescriptionPosAndKiosk),
				redeemableDescriptionPosAndKiosk + " expected description of redeemable is not matched");

		// User SignUp using API
		String userEmail3 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse3 = pageObj.endpoints().Api2SignUp(userEmail3, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse3, "API 2 user signup");
		String user3_Token = signUpResponse3.jsonPath().get("access_token.token").toString();

		String userID3 = signUpResponse3.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID3, dataSet.get("apiKey"), "",
				redeemableIDForKiosk, "", "");

		utils.logit("===Applicable offers API===");
		Response applicableresponse3 = pageObj.endpoints().posApplicableOffer(userEmail3, "10",
				dataSet.get("locationkey"));

		String actualRedeemableNameList3 = applicableresponse3.jsonPath().getString("reward.name").replace("[", "")
				.replace("]", "");
		Assert.assertEquals(actualRedeemableNameList3, "", redeemableNameForKiosk + " redeemable should not qualify");

		utils.logPass("Verified that " + redeemableNameForKiosk + " is not qualified ");
		String redeemableNameForPosAndKiosk2 = dataSet.get("redeemableNameForPosAndKiosk2");
		String redeemableIDForPosAndKiosk2 = dataSet.get("redeemableIDForPosAndKiosk2");


		//genearte external UID
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// send reward amount to user Reedemable
		Response sendRewardResponse4 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableIDForPosAndKiosk2, "", "");
		Assert.assertEquals(sendRewardResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user for redeemableIDForPosAndKiosk");
		utils.logit("Api2  send reward amount to user is successful for redeemableIDForPosAndKiosk");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),redeemableIDForPosAndKiosk2);
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(dataSet.get("locationkey"), userID, "reward", rewardId, externalUID);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for auth list discount basket added for POS API with external UID");
		// hit pos discount lookup api
		Response posDiscountLookupResponse = pageObj.endpoints().POSDiscountLookupWithChannel(dataSet.get("locationkey"), userID, "101", externalUID, "kiosk");
		Assert.assertEquals(posDiscountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos discount lookup api");
		// assert selected_discounts[0].qualified should be false
		boolean isQualified = posDiscountLookupResponse.jsonPath().getBoolean("selected_discounts[0].qualified");
		Assert.assertFalse(isQualified, "Selected discount should not be qualified for redeemableIDForPosAndKiosk");
		utils.logPass("Verified that selected discount is not qualified for redeemableIDForPosAndKiosk");
		String actualRedeemableNameForPosAndKiosk = posDiscountLookupResponse.jsonPath()
				.get("selected_discounts[0].discount_details.name").toString();
		Assert.assertEquals(actualRedeemableNameForPosAndKiosk, redeemableNameForPosAndKiosk2,
				actualRedeemableNameForPosAndKiosk + " is not matched with " + redeemableNameForPosAndKiosk2);
		utils.logPass("Verified that POS and KIOSK redeemable is not qualified -- " + actualRedeemableNameForPosAndKiosk);


		// send reward amount to user Reedemable
		Response sendRewardResponse5 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableIDForKiosk, "", "");
		Assert.assertEquals(sendRewardResponse5.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user for redeemableIDForKIOSK");
		utils.logit("Api2  send reward amount to user is successful for redeemableIDForKiosk");
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),redeemableIDForKiosk);
		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(dataSet.get("locationkey"), userID, "reward", rewardId2, externalUID);
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for auth list discount basket added for POS API with external UID");
		// hit pos discount lookup api
		Response posDiscountLookupResponse2 = pageObj.endpoints().POSDiscountLookupWithChannel(dataSet.get("locationkey"), userID, "101", externalUID, "kiosk");
		Assert.assertEquals(posDiscountLookupResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos discount lookup api");
		// assert selected_discounts[1].qualified should be false
		boolean isQualified2 = posDiscountLookupResponse2.jsonPath().getBoolean("selected_discounts[1].qualified");
		Assert.assertTrue(isQualified2, "Selected discount should qualified for redeemableIDForKiosk");
		utils.logPass("Verified that selected discount is qualified for redeemableIDForKiosk");
		String actualRedeemableNameForKiosk = posDiscountLookupResponse2.jsonPath()
				.get("selected_discounts[1].discount_details.name").toString();
		Assert.assertEquals(actualRedeemableNameForKiosk, redeemableNameForKiosk,
				actualRedeemableNameForKiosk + " is not matched with " + redeemableNameForKiosk);
		utils.logPass("Verified that KIOSK redeemable is qualified -- " + actualRedeemableNameForKiosk);


	}

	@Test(description = "OMM-T2606 / SQ-T5159 Auth>Validate that Applicable offers API end point considers the “channel” configured in Receipt Qualifier while evaluating the Qualification Criteria", groups = { "regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void T5159_ValidateReceiptQualifierForEvaluatingtheQC() {

		String redeemableNameForWeb = dataSet.get("redeemableNameForWeb");
		String redeemableDescriptionWeb = dataSet.get("redeemableDescriptionWeb");
		String redeemableIDForWeb = dataSet.get("redeemableIDForWeb");

		String redeemableNameForOnlineOrder = dataSet.get("redeemableNameForOnlineOrder");
		String redeemableDescriptionAuthOnlineOrder = dataSet.get("redeemableDescriptionAuthOnlineOrder");
		String redeemableIDForOnlineOrder = dataSet.get("redeemableIDForOnlineOrder");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		// user creation using auth signup api
		Response authSignUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		System.out.println("authSignUpResponse--" + authSignUpResponse.asPrettyString());
		apiUtils.verifyCreateResponse(authSignUpResponse, "Auth API user signup");
		Assert.assertEquals(authSignUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = authSignUpResponse.jsonPath().get("authentication_token");
		String authUserID = authSignUpResponse.jsonPath().get("user_id").toString();
		Assert.assertEquals(authSignUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(authUserID, dataSet.get("apiKey"), "",
				redeemableIDForWeb, "", "");
		// send reward amount to user Reedemable 7wZjQQoEC-weB-tEBys5
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(authUserID, dataSet.get("apiKey"), "",
				redeemableIDForOnlineOrder, "", "");

		utils.logit("===Applicable offers API===");
		Response applicableresponse = pageObj.endpoints().authApplicableOffersNew(dataSet.get("client"),
				dataSet.get("secret"), "101", "101", authToken, "web");
		Assert.assertEquals(applicableresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth Applicable offers API");
		boolean isAuthApplicableOffersSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.authApplicableOffersSchema, applicableresponse.asString());
		Assert.assertTrue(isAuthApplicableOffersSchemaValidated, "Auth Applicable offers API schema validation failed");
		List<Object> actualRedeemableNameList = applicableresponse.jsonPath().getList("reward.name");

		Assert.assertTrue(actualRedeemableNameList.contains(redeemableNameForWeb),
				redeemableNameForWeb + " expected is not in the json redeemable list.");

		List<Object> actualRedeemableDescriptionList = applicableresponse.jsonPath().getList("reward.description");

		Assert.assertTrue(actualRedeemableDescriptionList.contains(redeemableDescriptionWeb),
				redeemableDescriptionWeb + " expected description of redeemable is not matched");

		utils.logit("===Applicable offers API===");
		Response applicableresponse2 = pageObj.endpoints().authApplicableOffersNew(dataSet.get("client"),
				dataSet.get("secret"), "101", "101", authToken, "online_order");

		List<Object> actualRedeemableNameList2 = applicableresponse2.jsonPath().getList("reward.name");

		Assert.assertTrue(actualRedeemableNameList2.contains(redeemableNameForOnlineOrder),
				redeemableNameForOnlineOrder + " expected is not in the json redeemable list.");

		List<Object> actualRedeemableDescriptionList2 = applicableresponse2.jsonPath().getList("reward.description");

		Assert.assertTrue(actualRedeemableDescriptionList2.contains(redeemableDescriptionAuthOnlineOrder),
				redeemableDescriptionAuthOnlineOrder + " expected description of redeemable is not matched");

	}
	@Test(description = "SQ-T5810 Mobile > Validate that pos discount look up endpoint considers the “channel” configured in Receipt Qualifier while evaluating the Qualification Criteria " +
			"SQ-T5811 Auth > Validate that pos discount look up endpoint considers the “channel” configured in Receipt Qualifier while evaluating the Qualification Criteria")
	@Owner(name = "Vansham Mishra")
	public void T5810_validatePosDiscountLookupEndpointWithChannelConfiguration() throws InterruptedException {

		String redeemableNameForMobile = dataSet.get("redeemableNameForMobile");
		String redeemableDescriptionMobile = dataSet.get("DoNotDeleteRedeemableQC_Automation_T5810_Channel_Mobile");
		String redeemableIDForMobile = dataSet.get("redeemableIDForMobile");

		String redeemableNameForPos = dataSet.get("redeemableNameForPos");
		String redeemableDescriptionPos = dataSet.get("redeemableDescriptionPos");
		String redeemableIDForPos = dataSet.get("redeemableIDForPos");

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		//check Allow Location for Multiple Redemption
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().searchAndClickOnLocation("bristol");
		pageObj.dashboardpage().checkUncheckAnyFlag("Allow Location for Multiple Redemption", "check");


		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		System.out.println("signUpResponse-- " + signUpResponse.asPrettyString());
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");
		//genearte external UID
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableIDForMobile, "", "");
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableIDForPos, "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user for redeemableIDForMobile");
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user for redeemableIDForPos");
		utils.logit("Api2  send reward amount to user is successful for redeemableIDForMobile and redeemableIDForPos");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),redeemableIDForMobile);
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),redeemableIDForPos);
		// add discount into the basket
		String discountID = Utilities.getRandomNoFromRange(1, 4) + "";
		 Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(dataSet.get("locationkey"), userID, "reward", rewardId, externalUID);
		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(dataSet.get("locationkey"), userID, "reward", rewardId2, externalUID);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for auth list discount basket added for " +redeemableNameForMobile);
		utils.logit(redeemableNameForMobile+": is successfully added into the discount basket");
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for auth list discount basket added for " +redeemableNameForPos);
		utils.logit(redeemableNameForPos+": is successfully added into the discount basket");

		// hit pos discount lookup api
		Response posDiscountLookupResponse = pageObj.endpoints().POSDiscountLookupWithChannel(dataSet.get("locationkey"), userID, dataSet.get("item_id"), externalUID,dataSet.get("channelMobile"));
		Assert.assertEquals(posDiscountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos discount lookup api");
		// verify that selected_discounts[0].qualified is true
		boolean isQualified = posDiscountLookupResponse.jsonPath().getBoolean("selected_discounts[0].qualified");
		Assert.assertTrue(isQualified, "Selected discount should be qualified for "+redeemableNameForMobile);
		utils.logPass("Verified that selected discount is qualified for "+redeemableNameForMobile);
		String actualRedeemableNameForMobile = posDiscountLookupResponse.jsonPath()
				.get("selected_discounts[0].discount_details.name").toString();
		Assert.assertEquals(actualRedeemableNameForMobile, redeemableNameForMobile,
				actualRedeemableNameForMobile + " is not matched with " + redeemableNameForMobile);
		utils.logPass("Verified that mobile redeemable is selected discount -- " + actualRedeemableNameForMobile);

		// verify that selected_discounts[1].qualified is not true
		boolean isQualifiedForPOS = posDiscountLookupResponse.jsonPath().getBoolean("selected_discounts[1].qualified");
		Assert.assertFalse(isQualifiedForPOS, "Selected discount should not be qualified for" +redeemableNameForPos);
		utils.logPass("Verified that selected discount is not qualified for " +redeemableNameForPos);
		String actualRedeemableNameForPOS = posDiscountLookupResponse.jsonPath()
				.get("selected_discounts[1].discount_details.name").toString();
		Assert.assertEquals(actualRedeemableNameForPOS, redeemableNameForPos,
				actualRedeemableNameForPOS + " is not matched with " + redeemableNameForPos);
		utils.logPass("Verified that POS redeemable is not qualified -- " + actualRedeemableNameForPOS);


		String redeemableIDForPosAndKiosk = dataSet.get("redeemableIDForPosAndKiosk");
		String redeemableNameForPosAndKiosk = dataSet.get("redeemableNameForPosAndKiosk");
		String redeemableDescriptionForPosAndKiosk = dataSet.get("redeemableDescriptionForPosAndKiosk");
		String redeemableIDForKiosk = dataSet.get("redeemableIDForKiosk");
		String redeemableNameForKiosk = dataSet.get("redeemableNameForPosAndKiosk");
//
//
		// send reward amount to user Reedemable
		Response sendRewardResponse4 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableIDForPosAndKiosk, "", "");
		Assert.assertEquals(sendRewardResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user for redeemableIDForPosAndKiosk");
		utils.logit("Api2  send reward amount to user is successful for redeemableIDForPosAndKiosk");
		String rewardId3 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),redeemableIDForPosAndKiosk);
		Response discountBasketResponse3 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(dataSet.get("locationkey"), userID, "reward", rewardId3, externalUID);
		Assert.assertEquals(discountBasketResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for auth list discount basket added for POS API with external UID");
		// hit pos discount lookup api
		Response posDiscountLookupResponse3 = pageObj.endpoints().POSDiscountLookupWithChannel(dataSet.get("locationkey"), userID, dataSet.get("item_id"), externalUID, dataSet.get("channelKiosk"));
		Assert.assertEquals(posDiscountLookupResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos discount lookup api");
		// assert selected_discounts[0].qualified should be false
		boolean isQualified2 = posDiscountLookupResponse3.jsonPath().getBoolean("selected_discounts[0].qualified");
		Assert.assertFalse(isQualified2, "Selected discount should not be qualified for redeemableIDForPosAndKiosk");
		utils.logPass("Verified that selected discount is not qualified for redeemableIDForPosAndKiosk");
		String actualRedeemableNameForPosAndKiosk = posDiscountLookupResponse3.jsonPath()
				.get("selected_discounts[2].discount_details.name").toString();
		Assert.assertEquals(actualRedeemableNameForPosAndKiosk, redeemableNameForPosAndKiosk,
				actualRedeemableNameForPosAndKiosk + " is not matched with " + redeemableNameForPosAndKiosk);
		utils.logPass("Verified that POS and KIOSK redeemable is not qualified -- " + actualRedeemableNameForPosAndKiosk);


		// send reward amount to user Reedemable
		Response sendRewardResponse5 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableIDForKiosk, "", "");
		Assert.assertEquals(sendRewardResponse5.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user for redeemableIDForKIOSK");
		utils.logit("Api2  send reward amount to user is successful for redeemableIDForKiosk");
		String rewardId4 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),redeemableIDForKiosk);
		Response discountBasketResponse4 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(dataSet.get("locationkey"), userID, "reward", rewardId4, externalUID);
		Assert.assertEquals(discountBasketResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for auth list discount basket added for POS API with external UID");
		// hit pos discount lookup api
		Response posDiscountLookupResponse4 = pageObj.endpoints().POSDiscountLookupWithChannel(dataSet.get("locationkey"), userID, dataSet.get("item_id"), externalUID, "kiosk");
		Assert.assertEquals(posDiscountLookupResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos discount lookup api");
		// assert selected_discounts[1].qualified should be false
		boolean isQualified3 = posDiscountLookupResponse4.jsonPath().getBoolean("selected_discounts[1].qualified");
		Assert.assertFalse(isQualified3, "Selected discount should qualified for redeemableIDForKiosk");
		utils.logPass("Verified that selected discount is not qualified for redeemableIDForKiosk");
		String actualRedeemableNameForKiosk = posDiscountLookupResponse4.jsonPath()
				.get("selected_discounts[2].discount_details.name").toString();
		Assert.assertEquals(actualRedeemableNameForKiosk, redeemableNameForKiosk,
				actualRedeemableNameForKiosk + " is not matched with " + redeemableNameForKiosk);
		utils.logPass("Verified that POS and KIOSK redeemable is not qualified -- " + actualRedeemableNameForKiosk);


	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}