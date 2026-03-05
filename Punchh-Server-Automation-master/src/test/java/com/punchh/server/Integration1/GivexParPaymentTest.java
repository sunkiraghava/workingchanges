package com.punchh.server.Integration1;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class GivexParPaymentTest {
	static Logger logger = LogManager.getLogger(GivexParPaymentTest.class);
	public WebDriver driver;
	String userEmail;
	String email = "autoemailTemp@punchh.com";
	ApiUtils apiUtils;
	PageObj pageObj;
	String sTCName;
	private String timeStamp;
	private String env, run = "ui";
	private String baseUrl;
	private String userToken;
	private String paycardUUID;
	private String sourceGCUUID;
	private String targetGCUUID;
	private String walletGCUUID;
	private String mockPaymentToken = "30920042429541310767000000246338";
	private String mockParPayURL = "https://c18f05d3-17bb-49e6-a0b8-c4119887e683.mock.pstmn.io";
	private static Map<String, String> dataSet;
	private Utilities utils;
	private Properties prop;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// single Login to instance
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.instanceDashboardPage().instanceLogin(baseUrl);
	}

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T5488, SQ-T5517 [Recurring Payment] Givex Gift Card Actions with Par Payment Saved Cards", priority = 0)

	public void T5488_T5517_VerifyGivexWithSavedParPaymentCards() throws Exception {

		// Set value of enable_returning_recurring_payment_token (business) to true
		String bizPreferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";

		String bizPreferenceData = DBUtils.executeQueryAndGetColumnValue(env, bizPreferenceQuery, "preferences");
		boolean bizUpdatePrefFlag = DBUtils.updateBusinessesPreference(env, bizPreferenceData, "true",
				"enable_returning_recurring_payment_token", dataSet.get("business_id"));
		Assert.assertTrue(bizUpdatePrefFlag,
				"Failed to update business preference for enable_returning_recurring_payment_token");

		String bizPreferenceData1 = DBUtils.executeQueryAndGetColumnValue(env, bizPreferenceQuery, "preferences");
		boolean bizUpdatePrefFlag1 = DBUtils.updateBusinessesPreference(env, bizPreferenceData1, "true",
				"vault_flag_optimisation", dataSet.get("business_id"));
		Assert.assertTrue(bizUpdatePrefFlag1, "Failed to update business preference for vault_flag_optimisation");

		String bizPreferenceData2 = DBUtils.executeQueryAndGetColumnValue(env, bizPreferenceQuery, "preferences");
		boolean bizUpdatePrefFlag2 = DBUtils.updateBusinessesPreference(env, bizPreferenceData2, "true",
				"enable_recurring_payment", dataSet.get("business_id"));
		Assert.assertTrue(bizUpdatePrefFlag2, "Failed to update business preference for enable_recurring_payment");

		// SQ-T5517
		// Select Test Business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Gift Card page and select the gift card adapter as "Givex
		// (without PIN verify)"
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");
		pageObj.giftcardsPage().selectGiftCardAdapter("Givex (without PIN verify)");
		pageObj.giftcardsPage().setGiftcardSeriesLength("603628", "21");
		// pageObj.giftcardsPage().enableGiftcardAutoreloadConfig(false,
		// DataSet.get("bizGcThresholdAmt"), DataSet.get("bizGcDefaultAmt"));
		pageObj.giftcardsPage().enableGiftCardAutoReloadFlag(false);
		pageObj.giftcardsPage().clickOnUpdateButton();

		// Navigate to Payment page and select the payment adapter as "PAR Payments"
		pageObj.giftcardsPage().selectPaymentAdapter("PAR Payments");

		// Navigate to Subscriptions page and set the payment adapter as "PAR Payment"
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Subscriptions");
		pageObj.dashboardpage().setPaymentAdapterInSubscriptionPage("PAR Payment");
		pageObj.dashboardpage().clickOnUpdateButton();

		// Navigate to Integration Services, set original Par Payment URL and enable the
		// recurring payment flag
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Integration Services");
		pageObj.giftcardsPage().parPaymentCredential(false, "https://UATps42.aurusepay.com");

		// Create the user and get the user token
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for api1 signup");
		userToken = signUpResponse.jsonPath().get("auth_token.token");
		pageObj.utils().logPass(userEmail + " Api1 user signup is successful");

		// Navigate to Integration Services, set mock Par Payment URL and enable the
		// recurring payment flag
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Integration Services");
		pageObj.giftcardsPage().parPaymentCredential(true, mockParPayURL);

		// Add physical card and validate in api response
		Response addPhysicalCardResp = pageObj.endpoints().api1ImportGiftCard(dataSet.get("non_wallet_gc_design_id"),
				dataSet.get("physical_card_no"), "1234", dataSet.get("client"), dataSet.get("secret"), userToken);
		Assert.assertEquals(addPhysicalCardResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed to add physical card with");
		String uuid = addPhysicalCardResp.jsonPath().getString("uuid").replace("[", "").replace("]", "");
		String actCardNumber = addPhysicalCardResp.jsonPath().getString("card_number").replace("[", "").replace("]",
				"");
		Assert.assertEquals(actCardNumber, dataSet.get("physical_card_no"), "Physical card number is not matched");
		pageObj.utils().logit("Physical card has added successfully with uuid: " + uuid);

		String sQuery = "SELECT `gift_cards`.id FROM `gift_cards` WHERE `gift_cards`.uuid='" + uuid + "';";
		String gc_id = DBUtils.executeQueryAndGetColumnValue(env, sQuery, "id");
		pageObj.utils().logit("Giftcard is present in gift_cards table with id: " + gc_id);

		sQuery = "SELECT `gift_card_versions`.event FROM `gift_card_versions` WHERE `gift_card_versions`.gift_card_id='"
				+ gc_id + "' order by id asc limit 1;";
		String card_event = DBUtils.executeQueryAndGetColumnValue(env, sQuery, "event");
		Assert.assertEquals(card_event, "card_added", "Physical GC event is not matched with card_added event");
		pageObj.utils().logit("Physical card event is matched with card_added event");

		// Delete the Gift Card from GiftCards table
		String delQuery = "DELETE FROM `gift_cards` WHERE `gift_cards`.id='" + gc_id + "';";
		boolean isDeleted = DBUtils.executeQuery(env, delQuery);
		Assert.assertTrue(isDeleted, "Physical card is not deleted from DB");

		// Delete the Gift Card from GiftCardVersions table
		String delQuery1 = "DELETE FROM `gift_card_versions` WHERE `gift_card_versions`.gift_card_id='" + gc_id + "';";
		boolean isDeleted1 = DBUtils.executeQuery(env, delQuery1);
		Assert.assertTrue(isDeleted1, "Gift Card version is not deleted from DB");
		pageObj.utils().logPass("Physical card is added and deleted successfully");

		// Generating the Saved Payment Card
		Response paymentCardResp = pageObj.endpoints().createPaymentCard(dataSet.get("client"), dataSet.get("secret"),
				userToken, "par_payment", mockPaymentToken, "Rohit", true);
		paycardUUID = paymentCardResp.jsonPath().getString("uuid");
		Assert.assertEquals(paymentCardResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed POS payment api");
		String uuidPaymentCard = paymentCardResp.jsonPath().getString("uuid").replace("[", "").replace("]", "");
		pageObj.utils().logit("Payment card has created successfully with uuid: " + uuidPaymentCard);

		// Purchase Gift card with saved payment card
		Response giftCardPurchaseResp = pageObj.endpoints().Api1PurchaseGiftCardWithRecurring(dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("non_wallet_gc_design_id"), "15", userToken, paycardUUID);
		Assert.assertEquals(giftCardPurchaseResp.statusCode(), ApiConstants.HTTP_STATUS_OK, "Failed to purchase the gift card");
		sourceGCUUID = giftCardPurchaseResp.jsonPath().getString("uuid").replace("[", "").replace("]", "");
		int gcPurAmt = (int) Double.parseDouble(
				giftCardPurchaseResp.jsonPath().getString("last_fetched_amount").replace("[", "").replace("]", ""));
		Assert.assertEquals(gcPurAmt, 15, "GC purchase amount is not matched with the expected amount");
		String sQuery1 = "SELECT `gift_cards`.id FROM `gift_cards` WHERE `gift_cards`.uuid='" + sourceGCUUID + "';";
		String gc_id1 = DBUtils.executeQueryAndGetColumnValue(env, sQuery1, "id");
		pageObj.utils().logit("Gift Card has purchased successfully with uuid: " + sourceGCUUID + " with amount: " + gcPurAmt);

		sQuery = "SELECT event FROM `gift_card_versions` WHERE gift_card_id='" + gc_id1 + "' order by id asc limit 1;";
		String card_event1 = DBUtils.executeQueryAndGetColumnValue(env, sQuery, "event");
		Assert.assertEquals(card_event1, "purchased", "GC purchase event is not matched with purchased event");
		pageObj.utils().logit("Gift Card event is matched with purchased event");

		String[] db_columns = { "type", "paid_for_type", "source" };
		sQuery = "SELECT type,paid_for_type,source FROM `payments` WHERE `payments`.paid_for_id='" + gc_id1
				+ "' order by id asc limit 1;";
		List<Map<String, String>> db_data = DBUtils.executeQueryAndGetMultipleColumns(env, sQuery, db_columns);
		Assert.assertEquals(db_data.get(0).get("type"), "Payment", "Type is not matched with Payment");
		Assert.assertEquals(db_data.get(0).get("paid_for_type"), "GiftCard",
				"paid_for_type is not matched with GiftCard");

		Assert.assertEquals(card_event1, "purchased", "GC purchase event is not matched with purchased event");
		pageObj.utils().logPass("Gift Card has purchased successfully with uuid: " + sourceGCUUID + " with amount: " + gcPurAmt);

		// Reload Gift card with saved payment card
		Response reloadGiftCardResp = pageObj.endpoints().Api1ReloadGiftCardRecurring(userEmail, dataSet.get("client"),
				dataSet.get("secret"), "15", userToken, sourceGCUUID, uuidPaymentCard);
		Assert.assertEquals(reloadGiftCardResp.statusCode(), ApiConstants.HTTP_STATUS_OK, "Failed to reload gift card");
		int gcreloadAmt = (int) Double.parseDouble(
				reloadGiftCardResp.jsonPath().getString("last_fetched_amount").replace("[", "").replace("]", ""));
		Assert.assertEquals(gcreloadAmt, 30, "Amount is not matched with the expected amount");
		pageObj.utils().logit("Gift Card reloaded successfully with saved payment card with amount: " + gcreloadAmt);

		// Validate the Gift Card balance by setting the last fetched amount invalid in
		// DB
		String query = "UPDATE gift_cards SET `last_fetched_amount` = '" + 15 + "' WHERE uuid = '" + sourceGCUUID
				+ "';";
		DBUtils.executeUpdateQuery(env, query);
		Response gcBalanceResp = pageObj.endpoints().api1FetchGiftCardBalance(dataSet.get("client"),
				dataSet.get("secret"), userToken, sourceGCUUID);
		int gcBalAmt = (int) Double.parseDouble(
				gcBalanceResp.jsonPath().getString("last_fetched_amount").replace("[", "").replace("]", ""));
		Assert.assertEquals(gcBalAmt, 30, "GC Balance is not matched with the expected amount");
		pageObj.utils().logit("Gift Card balance is matched with the expected amount: " + gcBalAmt);

		// Share the Gift Card with other user
		String sharerUserEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResp1 = pageObj.endpoints().Api1UserSignUp(sharerUserEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResp1.getStatusCode(), 200, "Status code 200 did not matched for api1 signup");
		String sharereUserToken = signUpResp1.jsonPath().get("auth_token.token");
		Response gcShareResp = pageObj.endpoints().Api1ShareGiftCard(sharerUserEmail, dataSet.get("client"),
				dataSet.get("secret"), userToken, sourceGCUUID);
		Assert.assertEquals(gcShareResp.statusCode(), ApiConstants.HTTP_STATUS_OK, "Failed to share the gift card");
		pageObj.utils().logit("Gift Card shared successfully with user: " + sharerUserEmail);

		sQuery = "SELECT state FROM `user_cards` WHERE gift_card_id='" + gc_id1
				+ "' and state='shared' order by id asc limit 1;";
		String state = DBUtils.executeQueryAndGetColumnValue(env, sQuery, "state");
		Assert.assertEquals(state, "shared", "Gift Card state is not matched with shared state");
		pageObj.utils().logit("Gift Card shared with user: " + sharerUserEmail);

		// Revoke the Gift Card with other user
		Response gcRevokeResp = pageObj.endpoints().Api2RevokeGiftCard(dataSet.get("client"), dataSet.get("secret"),
				userToken, sourceGCUUID, sharerUserEmail);
		Assert.assertEquals(gcRevokeResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 revoke gift card");
		String actSucMsg1 = gcRevokeResp.jsonPath().get().toString().replace("[", "").replace("]", "");
		pageObj.utils().logit("Gift Card revoked with user: " + sharerUserEmail);

		// Transfer the Gift Card to other user
		Response gcTransferResp = pageObj.endpoints().Api1TransferGiftCard(sharerUserEmail, dataSet.get("client"),
				dataSet.get("secret"), "2", userToken, sourceGCUUID);
		Assert.assertEquals(gcTransferResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 transfer gift card");
		String query1 = "Select last_fetched_amount from gift_cards WHERE uuid = '" + sourceGCUUID + "';";
		String actGCBal = DBUtils.executeQueryAndGetColumnValue(env, query1, "last_fetched_amount");
		Assert.assertEquals(actGCBal, "28.0", "Gift Card balance is not matched with the expected amount");
		pageObj.utils().logit("Gift Card transferred successfully with amount: " + actGCBal + " to user: " + sharerUserEmail);

		// SQ-T5488
		// Consolidate the Gift Card (Source and Target GC)
		Response giftCardPurchaseResp1 = pageObj.endpoints().Api1PurchaseGiftCardWithRecurring(dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("non_wallet_gc_design_id"), "15", userToken, paycardUUID);
		Assert.assertEquals(giftCardPurchaseResp1.statusCode(), ApiConstants.HTTP_STATUS_OK,
				"Failed to purchase gift card giftCardPurcahseResponse");
		targetGCUUID = giftCardPurchaseResp1.jsonPath().getString("uuid").replace("[", "").replace("]", "");
		int targetGCPurAmt = (int) Double.parseDouble(
				giftCardPurchaseResp1.jsonPath().getString("last_fetched_amount").replace("[", "").replace("]", ""));
		Assert.assertEquals(targetGCPurAmt, 15, "Amount is not matched with the expected amount");
		pageObj.utils().logit("Target Gift Card has purchased successfully with uuid: " + targetGCUUID
				+ " with amount: " + targetGCPurAmt);

		Response giftCardConsolidateResp = pageObj.endpoints().api1ConsolidateGiftCard(dataSet.get("client"),
				dataSet.get("secret"), userToken, targetGCUUID, sourceGCUUID);
		Assert.assertEquals(giftCardConsolidateResp.statusCode(), ApiConstants.HTTP_STATUS_OK, "Gift Card don't consolidated with source GC");
		String actSourceCardBal = giftCardConsolidateResp.jsonPath().get("source_card.last_fetched_amount").toString();
		String actSourceCardStatus = giftCardConsolidateResp.jsonPath().get("source_card.status").toString();
		String actTargetCardBal = giftCardConsolidateResp.jsonPath().get("destination_card.last_fetched_amount")
				.toString();
		Assert.assertEquals(actSourceCardBal, "0.0", "Source GC balance is not matched with the expected amount");
		Assert.assertEquals(actSourceCardStatus, "closed", "Source GC status is not matched with the expected status");
		Assert.assertEquals(actTargetCardBal, "43.0", "Target GC balance is not matched with the expected amount");
		pageObj.utils().logit("Gift Card consolidated successfully with source GC: " + sourceGCUUID
				+ " and target GC: " + targetGCUUID);

		// Reload the Gift Card with closed status
		Response giftCardReloadResp1 = pageObj.endpoints().Api1ReloadGiftCardRecurring(dataSet.get("client"),
				dataSet.get("secret"), "5", userToken, sourceGCUUID, paycardUUID);
		Assert.assertEquals(giftCardReloadResp1.statusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "Givex GC reloaded with closed status");
		String errMsg2 = giftCardReloadResp1.jsonPath().get().toString().replace("[", "").replace("]", "");
		Assert.assertEquals(errMsg2, "The operation is supported for active cards only.",
				"Givex GC closed status msg is not matched");
		pageObj.utils().logit("GC reload is not working with closed status");

		// Delete the Gift Card with closed status
		Response giftCardDelResp = pageObj.endpoints().Api2DeleteGiftCard(dataSet.get("client"), dataSet.get("secret"),
				userToken, sourceGCUUID);
		Assert.assertEquals(giftCardDelResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 delete gift card with closed state");
		String actSucMsg3 = giftCardDelResp.jsonPath().get().toString().replace("[", "").replace("]", "");
		Assert.assertEquals(actSucMsg3, "Gift Card successfully deleted.",
				"Failed to delete the gift card with closed state");
		pageObj.utils().logPass("Gift Card deleted successfully with closed state");
	}

	@Test(description = "SQ-T5519 Subscription Plan Purchase with Par Payment Card", priority = 1)

	public void T5519_VerifySubscriptionPurchaseWithParPaymentCard() throws Exception {

		// Select Test Business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// purchase subscription plan
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(userToken,
				dataSet.get("subscription_paln_id"), dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("subscription_amount"), paycardUUID, "Heartland");
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id = purchaseSubscriptionresponse.jsonPath().getString("subscription_id").replace("[", "")
				.replace("]", "");

		// Get the subscription details from DB and validate
		String[] db_columns = { "type", "paid_for_type", "source" };
		String sQuery = "SELECT type,paid_for_type,payment_details,source FROM `payments` WHERE `payments`.paid_for_id='"
				+ subscription_id + "' order by id asc limit 1;";
		List<Map<String, String>> db_data = DBUtils.executeQueryAndGetMultipleColumns(env, sQuery, db_columns);
		Assert.assertEquals(db_data.get(0).get("type"), "Payment",
				"Subscription Payment Type is not matched with Payment");
		Assert.assertEquals(db_data.get(0).get("paid_for_type"), "UserSubscription",
				"Subscription Paid for type is not matched with UserSubscription");
		Assert.assertEquals(db_data.get(0).get("source"), "recurring",
				"Subscription Source is not matched with recurring");
		pageObj.utils().logPass("Subscription has purchased successfully with payment reference id: " + subscription_id);
	}

	@Test(description = "SQ-T5486, SQ-T5487, SQ-T5490 Wallet Gift Card with Saved Payment Card", priority = 2)

	public void T5486_T5487_T5490_VerifyWalletGCWithSavedPayment() throws Exception {

		// Select Test Business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// SQ-T5486
		// Generate the Wallet Gift Card
		Response giftCardPurchaseResponse = pageObj.endpoints().Api1PurchaseGiftCardWithRecurring(dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("wallet_gc_design_id"), "6", userToken, paycardUUID);
		Assert.assertEquals(giftCardPurchaseResponse.statusCode(), ApiConstants.HTTP_STATUS_OK, "Failed to purchase wallet gift card");
		walletGCUUID = giftCardPurchaseResponse.jsonPath().getString("uuid").replace("[", "").replace("]", "");
		int amountWalletGC = (int) Double.parseDouble(
				giftCardPurchaseResponse.jsonPath().getString("last_fetched_amount").replace("[", "").replace("]", ""));
		logger.info("Wallet GC uuid is " + walletGCUUID);
		pageObj.utils().logPass("New Generated wallet Gift Card uuid is " + walletGCUUID + "with amount: " + amountWalletGC);

		// Validate the multiple wallet gift
		Response giftCardPurchaseResponse1 = pageObj.endpoints().Api1PurchaseGiftCardWithRecurring(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("wallet_gc_design_id"), "6", userToken,
				paycardUUID);
		Assert.assertEquals(giftCardPurchaseResponse1.statusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "Multiple wallet Gift Card generated");
		String errMsg = giftCardPurchaseResponse1.jsonPath().getString("errors").replace("[", "").replace("]", "");
		Assert.assertEquals(errMsg,
				"You cannot purchase a wallet gift card, as it's already been added to your account.",
				"Duplicate wallet card generated");
		pageObj.utils().logPass("Unique Wallet Gift Card generated");

		// SQ-T5487
		// Validate the shared action on wallet gift card
		Response giftCardShareResp = pageObj.endpoints().Api1ShareGiftCard("rohit+gbsr20920@punchh.com",
				dataSet.get("client"), dataSet.get("secret"), userToken, walletGCUUID);
		Assert.assertEquals(giftCardShareResp.statusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "Wallet GC shared with other user");
		String errMsg1 = giftCardShareResp.jsonPath().getString("errors").replace("[", "").replace("]", "");
		Assert.assertEquals(errMsg1, "You cannot perform share operation on wallet type gift card.",
				"Wallet Gift Card shared with other user");
		pageObj.utils().logit("Wallet GC Sharing is not allowed");

		// Validate the transfer action on wallet gift card
		Response giftCardTransferResp = pageObj.endpoints().Api1TransferGiftCard("rohit+gbsr20920@punchh.com",
				dataSet.get("client"), dataSet.get("secret"), "2", userToken, walletGCUUID);
		Assert.assertEquals(giftCardTransferResp.statusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "Wallet GC transferred with other user");
		String errMsg2 = giftCardTransferResp.jsonPath().getString("errors").replace("[", "").replace("]", "");
		Assert.assertEquals(errMsg2, "You cannot perform transfer operation on wallet type gift card.",
				"Wallet Gift Card transferred with other user");
		pageObj.utils().logit("Wallet GC Transferred is not allowed");

		// Consolidate Wallet GC with source GC of closed status
		Response giftCardConsolidateResp = pageObj.endpoints().api1ConsolidateGiftCard(dataSet.get("client"),
				dataSet.get("secret"), userToken, walletGCUUID, sourceGCUUID);
		Assert.assertEquals(giftCardConsolidateResp.statusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "Wallet GC consolidated with source GC");
		String errMsg3 = giftCardConsolidateResp.jsonPath().get().toString().replace("[", "").replace("]", "");
		Assert.assertEquals(errMsg3, "You should be the owner of this card to perform this action.",
				"Wallet Gift Card transferred with other user");
		pageObj.utils().logit("Wallet GC Consolidate is not allowed with closed GC");

		// Consolidate Gift card with Wallet target GC as a source GC
		Response giftCardConsolidateResp1 = pageObj.endpoints().api1ConsolidateGiftCard(dataSet.get("client"),
				dataSet.get("secret"), userToken, targetGCUUID, walletGCUUID);
		Assert.assertEquals(giftCardConsolidateResp1.statusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "Wallet GC consolidated as a source GC");
		String errMsg4 = giftCardConsolidateResp1.jsonPath().getString("errors").replace("[", "").replace("]", "");
		Assert.assertEquals(errMsg4, "You cannot perform consolidate operation on wallet type gift card.",
				"Wallet Gift Card consolidated as a source GC");
		pageObj.utils().logit("Wallet GC Consolidate is not allowed as a source GC");

		// Gift Card deletion with wallet GC
		Response giftCardDelResp = pageObj.endpoints().Api2DeleteGiftCard(dataSet.get("client"), dataSet.get("secret"),
				userToken, walletGCUUID);
		Assert.assertEquals(giftCardDelResp.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "Wallet is deleting thorugh api");
		String actSucMsg = giftCardDelResp.jsonPath().getString("errors").replace("[", "").replace("]", "");
		Assert.assertEquals(actSucMsg, "You cannot perform delete operation on wallet type gift card.",
				"Wallet gift card is able to delete");
		pageObj.utils().logPass("Gift Card deletion is not allowed for wallet");

		// SQ-T5490
		// Gift Card deletion with target GC with active status
		Response giftCardDelResp1 = pageObj.endpoints().Api2DeleteGiftCard(dataSet.get("client"), dataSet.get("secret"),
				userToken, targetGCUUID);
		Assert.assertEquals(giftCardDelResp1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Wallet is deleting thorugh api");
		String actSucMsg1 = giftCardDelResp1.jsonPath().get().toString().replace("[", "").replace("]", "");
		Assert.assertEquals(actSucMsg1, "Gift Card successfully deleted.", "Failed to delete the gift card");
		pageObj.utils().logit("Gift Card deleted successfully with active state");

		// Deleted Gift Card in Gift Card List api
		Response giftCardListResp = pageObj.endpoints().api1FetchGiftCardList(dataSet.get("client"),
				dataSet.get("secret"), userToken, walletGCUUID);
		Assert.assertEquals(giftCardListResp.statusCode(), ApiConstants.HTTP_STATUS_OK, "Failed to fetch the gift card list");
		int gc_size = giftCardListResp.jsonPath().getList("$").size();
		Assert.assertEquals(gc_size, 1, "Deleted Gift card is present in the list");
		pageObj.utils().logit("Deleted Gift Card is not present in the GC list");

		// Gift Card deletion with deleted gift card
		Response giftCardDelResp2 = pageObj.endpoints().Api2DeleteGiftCard(dataSet.get("client"), dataSet.get("secret"),
				userToken, targetGCUUID);
		Assert.assertEquals(giftCardDelResp2.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 delete gift card with deleted state");
		String actSucMsg2 = giftCardDelResp2.jsonPath().getString("errors").replace("[", "").replace("]", "");
		Assert.assertEquals(actSucMsg2, "You cannot delete this Gift Card right now. Please try again later.",
				"Failed to delete the gift card with deleted state");
		pageObj.utils().logPass("Gift Card deletion is not allowed with deleted state");

	}

	@Test(description = "SQ-5482, SQ-5484 Wallet Gift Card with Autoreload", priority = 3)

	public void T5482_5484_VerifyWalletGCWithAutoreload() throws Exception {

		// Select Test Business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");
		pageObj.giftcardsPage().selectGiftCardAdapter("Givex (without PIN verify)");
		pageObj.giftcardsPage().enableGiftcardAutoreloadConfig(true, dataSet.get("bizGcThresholdAmt"),
				dataSet.get("bizGcDefaultAmt"));
		pageObj.giftcardsPage().clickOnUpdateButton();

		// Wallet GC ID:
		String sQuery = "select id from gift_cards where uuid = '" + walletGCUUID + "';";
		String walletGCID = DBUtils.executeQueryAndGetColumnValue(env, sQuery, "id");

		// Wallet GC Autoreload with threshold amount validation
		Response giftCardUpdateResp = pageObj.endpoints().api1UpdateGiftCardwithAutoreloadConfig(dataSet.get("client"),
				dataSet.get("secret"), userToken, walletGCUUID, true, true, "1", "10", paycardUUID);
		Assert.assertEquals(giftCardUpdateResp.statusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Autoreload is enabled on wallet GC with invalid threshold amount");
		String actThresholdMsg = giftCardUpdateResp.jsonPath().get().toString().replace("[", "").replace("]", "");
		Assert.assertEquals(actThresholdMsg, "Threshold amount should be greater than or equal to $3.00",
				"Threshold amount is not greater than 3.0");
		pageObj.utils().logit("Displayed error message for threshold amount is less than 3.0");

		// Wallet GC Autoreload with default amount validation
		Response giftCardUpdateResp1 = pageObj.endpoints().api1UpdateGiftCardwithAutoreloadConfig(dataSet.get("client"),
				dataSet.get("secret"), userToken, walletGCUUID, true, true, "10", "1", paycardUUID);
		Assert.assertEquals(giftCardUpdateResp1.statusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Autoreload is enabled on wallet GC with invalid default amount");
		String actThresholdMsg1 = giftCardUpdateResp1.jsonPath().get().toString().replace("[", "").replace("]", "");
		Assert.assertEquals(actThresholdMsg1, "Default amount should be greater than or equal to $4.00",
				"Default amount is not greater than 4.0");
		pageObj.utils().logit("Displayed error message for default amount is less than 4.0");

		// Wallet GC Autoreload with invalid payment card
		Response giftCardUpdateResp2 = pageObj.endpoints().api1UpdateGiftCardwithAutoreloadConfig(dataSet.get("client"),
				dataSet.get("secret"), userToken, walletGCUUID, true, true, "10", "4", "abcd-efgh-idjkla");
		Assert.assertEquals(giftCardUpdateResp2.statusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Autoreload is enabled on wallet GC with invalid payment card");
		String actThresholdMsg2 = giftCardUpdateResp2.jsonPath().get().toString().replace("[", "").replace("]", "");
		Assert.assertEquals(actThresholdMsg2, "The payment card is not valid", "Payment card is invalid");
		pageObj.utils().logit("Displayed error message for invalid payment card");

		// Wallet GC update with autoreloadconfig enabled
		Response giftCardUpdateResp3 = pageObj.endpoints().api1UpdateGiftCardwithAutoreloadConfig(dataSet.get("client"),
				dataSet.get("secret"), userToken, walletGCUUID, true, true, dataSet.get("userGcThresholdAmt"),
				dataSet.get("userGcDefaultAmt"), paycardUUID);
		Assert.assertEquals(giftCardUpdateResp3.statusCode(), 200, "Failed to add gift card autoreload config");
		boolean actAutoreloadStatus3 = giftCardUpdateResp3.jsonPath().getBoolean("auto_reload_enabled");
		Assert.assertTrue(actAutoreloadStatus3, "Autoreload is not enable on wallet GC");
		logger.info("Wallet GC autoreload status is " + actAutoreloadStatus3);
		pageObj.utils().logit("Autorelod is enabled on wallet GC");

		// Wallet giftcard autoreload with successful transaction
		Response gcBalanceResp = pageObj.endpoints().api1FetchGiftCardBalance(dataSet.get("client"),
				dataSet.get("secret"), userToken, walletGCUUID);
		int gcBalAmt = (int) Double.parseDouble(
				gcBalanceResp.jsonPath().getString("last_fetched_amount").replace("[", "").replace("]", ""));
		Assert.assertEquals(gcBalAmt, 6, "GC Balance is not matched with the expected amount");
		String sQuery1 = "select last_fetched_amount from gift_cards where uuid = '" + walletGCUUID + "';";
		boolean autoReloaded = DBUtils.verifyDBValueWithPolling(env, sQuery1, "last_fetched_amount", "16.0", 5, 10);
		Assert.assertTrue(autoReloaded, "Auto-reload did not work as expected for wallet GC");
		logger.info("Auto-reload worked successfully for wallet GC with UUID: " + walletGCUUID
				+ " and balance is updated to 16.0");
		pageObj.utils().logit("Auto-reload worked successfully for wallet GC with UUID: " + walletGCUUID
				+ " and balance is updated to 16.0");

		String[] db_columns = { "user_agent", "source" };
		String sQuery2 = "select user_agent,source from payments where paid_for_id = '" + walletGCID
				+ "' order by id desc limit 1;";
		List<Map<String, String>> db_data = DBUtils.executeQueryAndGetMultipleColumns(env, sQuery2, db_columns);
		Assert.assertEquals(db_data.get(0).get("user_agent"), "GiftCard::AutoReloadWorker",
				"Payment is not performed through Autoreload worker");
		Assert.assertEquals(db_data.get(0).get("source"), "recurring", "payment source is not recurring");
		pageObj.utils().logPass("Giftcard is autoreloaded successfully with amount of 10 and balance is: 16");

		// Wallet Giftcard Autoreload didn't work because threshold amount is reached
		Response gcBalanceResp1 = pageObj.endpoints().api1FetchGiftCardBalance(dataSet.get("client"),
				dataSet.get("secret"), userToken, walletGCUUID);
		pageObj.utils().logPass("Gift Card balance is matched with the expected amount: " + gcBalAmt);
		String sQuery3 = "select last_fetched_amount from gift_cards where uuid = '" + walletGCUUID + "';";
		boolean autoReloaded1 = DBUtils.verifyDBValueWithPolling(env, sQuery3, "last_fetched_amount", "26.0", 10, 4);
		Assert.assertFalse(autoReloaded1, "Auto-reload worked even though threshold amount is reached for wallet GC");
		pageObj.utils().logPass("Auto-reload did not work for wallet GC as threshold amount is reached");

		// Wallet GC update with autoreload config disabled
		Response giftCardUpdateResp4 = pageObj.endpoints().api1UpdateGiftCardwithAutoreloadConfig(dataSet.get("client"),
				dataSet.get("secret"), userToken, walletGCUUID, true, false, "20", "10", paycardUUID);
		Assert.assertEquals(giftCardUpdateResp4.statusCode(), 200,
				"Failed to update gift card autoreload config to false");
		boolean actAutorelodStatus4 = giftCardUpdateResp4.jsonPath().getBoolean("auto_reload_enabled");
		Assert.assertFalse(actAutorelodStatus4, "Autoreload is enabled on wallet GC");
		logger.info("Wallet GC autoreload status is " + actAutorelodStatus4);
		pageObj.utils().logPass("Autorelod is disabled on wallet GC");

	}

	@Test(description = "SQ-T5491, SQ-T5492 Zero Amount Wallet GC Generation and validations", priority = 4)

	public void T5491_T5492_VerifyZeroAmountWalletGC() throws Exception {

		// Select Test Business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");
		pageObj.giftcardsPage().enableZeroBalWalletGCFlag(false);
		pageObj.giftcardsPage().clickOnUpdateButton();

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		userToken = signUpResponse.jsonPath().get("auth_token.token");
		pageObj.utils().logPass(userEmail + " Api1 user signup is successful");

		// Issued Gift card without enabling the Zero-Balance Wallet Gift Card
		Response giftCardIssuanceResp = pageObj.endpoints().Api1IssuanceGiftCard(dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("wallet_gc_design_id"), userToken);
		Assert.assertEquals(giftCardIssuanceResp.statusCode(), 422,
				"Generated Wallet Gift Card without enabling the zero amount flag");
		String actErrMsg = giftCardIssuanceResp.jsonPath().get().toString().replace("[", "").replace("]", "");
		Assert.assertEquals(actErrMsg, "Currently this business doesn't support card issuance with zero amount.",
				"Zero amount wallet GC is generated without enabling the zero amount flag");
		pageObj.utils().logit("Wallet Gift Card is not generated without enabling the zero amount flag");

		// Issued Gift card with zero amount and invalid design_id
		pageObj.giftcardsPage().enableZeroBalWalletGCFlag(true);
		pageObj.giftcardsPage().clickOnUpdateButton();
		Response giftCardIssuanceResp1 = pageObj.endpoints().Api1IssuanceGiftCard(dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("non_wallet_gc_design_id"), userToken);
		Assert.assertEquals(giftCardIssuanceResp1.statusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "Generated Wallet Gift Card with zero amount");
		String actErrMsg1 = giftCardIssuanceResp1.jsonPath().get().toString().replace("[", "").replace("]", "");
		Assert.assertEquals(actErrMsg1, "This operation is not supported.",
				"Zero amount wallet GC is generated with invalid GC design_id");
		pageObj.utils().logit("Wallet Gift Card is not generated with zero amount and invalid giftcard design_id");

		// Issued Gift card with zero amount and valid design_id
		Response giftCardIssuanceResp2 = pageObj.endpoints().Api1IssuanceGiftCard(dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("wallet_gc_design_id"), userToken);
		Assert.assertEquals(giftCardIssuanceResp2.statusCode(), ApiConstants.HTTP_STATUS_OK,
				"Failed to purchase wallet gift card with zero amount");
		walletGCUUID = giftCardIssuanceResp2.jsonPath().getString("uuid").replace("[", "").replace("]", "");
		int gcIssuanceAmt = (int) Double.parseDouble(
				giftCardIssuanceResp2.jsonPath().getString("last_fetched_amount").replace("[", "").replace("]", ""));
		Assert.assertEquals(gcIssuanceAmt, 0, "Amount is not matched with the zero amount");
		String sQuery = "SELECT `gift_cards`.id FROM `gift_cards` WHERE `gift_cards`.uuid='" + walletGCUUID + "';";
		String gc_id = DBUtils.executeQueryAndGetColumnValue(env, sQuery, "id");

		sQuery = "SELECT event FROM `gift_card_versions` WHERE gift_card_id='" + gc_id + "' order by id asc limit 1;";
		String card_event1 = DBUtils.executeQueryAndGetColumnValue(env, sQuery, "event");
		Assert.assertEquals(card_event1, "card_issued", "GC purchase is not matched with card_issued");
		pageObj.utils().logit(
				"Gift Card has issued successfully with uuid: " + walletGCUUID + " with amount: " + gcIssuanceAmt);

		// Multiple Wallet GC issuance validations
		Response giftCardIssuanceResp3 = pageObj.endpoints().Api1IssuanceGiftCard(dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("wallet_gc_design_id"), userToken);
		Assert.assertEquals(giftCardIssuanceResp3.statusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Multiple Wallet Gift Card generated with zero amount");
		String actErrMs3 = giftCardIssuanceResp3.jsonPath().getString("errors").replace("[", "").replace("]", "");
		Assert.assertEquals(actErrMs3,
				"You cannot purchase a wallet gift card, as it's already been added to your account.",
				"Duplicate wallet card generated with zero amount");
		pageObj.utils().logit("Duplicate Wallet Gift Card is not generated with zero amount");

		// Zero amount wallet GC with reload and credit card
		Response giftCardReloadResp = pageObj.endpoints().api1ReloadGiftCard(dataSet.get("client"),
				dataSet.get("secret"), userToken, "6", "584", mockPaymentToken, walletGCUUID);
		Assert.assertEquals(giftCardReloadResp.statusCode(), ApiConstants.HTTP_STATUS_OK, "Reload is working with zero amount wallet GC");
		gcIssuanceAmt = (int) Double.parseDouble(
				giftCardReloadResp.jsonPath().getString("last_fetched_amount").replace("[", "").replace("]", ""));
		Assert.assertTrue(gcIssuanceAmt > 0, "Amount is not matched after reload with zero amount wallet GC");
		pageObj.utils().logPass("Reload is working with zero amount wallet GC");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}

}
