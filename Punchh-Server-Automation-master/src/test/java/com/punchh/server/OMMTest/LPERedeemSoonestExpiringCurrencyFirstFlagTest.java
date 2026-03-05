package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
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

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

/*
 * @Author :Shashank sharma
 */
@Listeners(TestListeners.class)

public class LPERedeemSoonestExpiringCurrencyFirstFlagTest {

	static Logger logger = LogManager.getLogger(LPERedeemSoonestExpiringCurrencyFirstFlagTest.class);
	public WebDriver driver;
	private String userEmail;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	private Utilities utils;
	public String updateExpiringDateInRewardCreditTable = "UPDATE reward_credits  SET expiring_at = '${expDate}'  WHERE user_id= '${userID}' and banked_reward_value=${amount}";
	public String getRewardID = "Select id from reward_credits where user_id= '${userID}' and banked_reward_value=${amount}";
	public String updateCreatedDateInRewardCreditTable = "UPDATE reward_credits  SET created_at = '${createdDate}'  WHERE user_id= '${userID}' and banked_reward_value=${amount}";

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
	}

	@Test(description = "LPE-T2173/ SQ-T6592  Verify reward_credit entry utilisation when user has single reward_credits present without expiring_at value present [redeem_soonest_expiring_currency_first -> true]", groups = {
			"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Shashank Sharma")
	public void LPE_T2173_ValidateRewardCreditEntryUtilisationWhenRedeemSoonestExpiringCurrencyFirstFlagTrue()
			throws Exception {

		String b_id = dataSet.get("business_id");

		// enable flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String businessPreferences = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		
//		Assert.assertTrue(businessPreferences.contains(dataSet.get("ommFlag")),
//				"Business preferences does not contain the flag: " + dataSet.get("ommFlag"));
//		utils.logPass("Business preferences contains the flag: " + dataSet.get("ommFlag"));
//		logger.info("Business preferences contains the flag: " + dataSet.get("ommFlag"));
		

		DBUtils.updateSingleValueForGivenParameter(env, businessPreferences, "true", dataSet.get("ommFlag"), b_id);

		Map<String, String> mapOfRewardIDs = new LinkedHashMap<>();

		List<String> listOfRewardAmount = new LinkedList<String>();
		listOfRewardAmount.add(dataSet.get("rewardAmount1"));
		listOfRewardAmount.add(dataSet.get("rewardAmount2"));
		listOfRewardAmount.add(dataSet.get("rewardAmount3"));

		String twoDaysFutureDate = Utilities.getFutureDateForDBUpdate(2);
		utils.logPass("Two days future date is: " + twoDaysFutureDate);

		String fiveDaysFutureDate = Utilities.getFutureDateForDBUpdate(5);
		utils.logPass("5 days future date is: " + fiveDaysFutureDate);

		String tenDaysFutureDate = Utilities.getFutureDateForDBUpdate(10);
		utils.logPass("10 days future date is: " + tenDaysFutureDate);

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		// String token =
		// signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				listOfRewardAmount.get(0), "", "", "");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				listOfRewardAmount.get(1), "", "", "");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				listOfRewardAmount.get(2), "", "", "");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse3.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		utils.logit("== Auth API List Available Rewards ==");

		// update the expiring_at value to 2,5,and 10 days from now
		String updateExpiringDateQuery1 = updateExpiringDateInRewardCreditTable.replace("${expDate}", tenDaysFutureDate)
				.replace("${userID}", userID).replace("${amount}", listOfRewardAmount.get(0));

		int rs = DBUtils.executeUpdateQuery(env, updateExpiringDateQuery1);
		Assert.assertEquals(rs, 1);

		String updateExpiringDateQuery2 = updateExpiringDateInRewardCreditTable
				.replace("${expDate}", fiveDaysFutureDate).replace("${userID}", userID)
				.replace("${amount}", listOfRewardAmount.get(1));

		int rs2 = DBUtils.executeUpdateQuery(env, updateExpiringDateQuery2);
		Assert.assertEquals(rs2, 1);

		String updateExpiringDateQuery3 = updateExpiringDateInRewardCreditTable.replace("${expDate}", twoDaysFutureDate)
				.replace("${userID}", userID).replace("${amount}", listOfRewardAmount.get(2));

		int rs3 = DBUtils.executeUpdateQuery(env, updateExpiringDateQuery3);
		Assert.assertEquals(rs3, 1);

		for (String amount : listOfRewardAmount) {
			String getRewardIDQuery = getRewardID.replace("${userID}", userID).replace("${amount}", amount);
			String rewardID = DBUtils.executeQueryAndGetColumnValue(env, getRewardIDQuery, "id");
			Assert.assertNotNull(rewardID, "Reward ID is null for amount: " + amount);
			utils.logPass("Reward id " + rewardID + " is generated successfully for the amount " + amount);
			mapOfRewardIDs.put(amount, rewardID);
		}

		// Sort the map of reward IDs in descending order by key (amount)
		Map<String, String> sortedMapOfRewardIDsAsDescendingOrderByKeyExpected = new LinkedHashMap<>();
		sortedMapOfRewardIDsAsDescendingOrderByKeyExpected = Utilities.sortByKeyDescending(mapOfRewardIDs);

		utils.logPass("Sorted Map of Reward IDs in descending order by key: "
				+ sortedMapOfRewardIDsAsDescendingOrderByKeyExpected);

		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();

		Response redemptionResponse = pageObj.endpoints().posRedemptionOfAmount(userEmail, date, key, txn,
				dataSet.get("redemptionAmount"), dataSet.get("locationkey"));
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for pos redemption of amount");
		utils.logPass("POS Redemption of amount " + dataSet.get("redemptionAmount") + " is successful");

		// SQL query to get the reward credit IDs and honored reward values in
		// descending order
		String rewardDebitsIDDescOrderQuery = "SELECT reward_credit_id , honored_reward_value FROM reward_debits WHERE user_id = "
				+ userID + " ORDER BY honored_reward_value DESC LIMIT 3;";

		int counter = 0;

		List<String> actualSortedListOfRewardCreditId = new LinkedList<>();
		List<String> actualSortedListOfHonoredRewardValue = new LinkedList<>();

		while ((actualSortedListOfRewardCreditId.isEmpty() || actualSortedListOfHonoredRewardValue.isEmpty())
				&& counter < 20) {
			actualSortedListOfRewardCreditId = DBUtils.getValueFromColumnInList(env,
					rewardDebitsIDDescOrderQuery, "reward_credit_id");
			utils.logit(
					counter + " : actualSortedListOfRewardCreditId LIST items : " + actualSortedListOfRewardCreditId);

			actualSortedListOfHonoredRewardValue = DBUtils.getValueFromColumnInList(env,
					rewardDebitsIDDescOrderQuery, "honored_reward_value");
			utils.logit(counter + " : actualSortedListOfHonoredRewardValue LIST items : "
					+ actualSortedListOfHonoredRewardValue);
			counter++;
			utils.longWaitInSeconds(3);
		}

		Assert.assertTrue(actualSortedListOfRewardCreditId.size() > 0, "reward_credit_id list is null");

		Assert.assertTrue(actualSortedListOfHonoredRewardValue.size() > 0, "honored_reward_value list is null");

		// Sort the expected amount lists in descending order
		listOfRewardAmount.sort(Comparator.reverseOrder());

		// Validate the actual and expected reward credit IDs and honored reward values
		// in order
		for (String amount : listOfRewardAmount) {

			int indexedOfAmountKey = Utilities.getKeyIndex(sortedMapOfRewardIDsAsDescendingOrderByKeyExpected, amount);

			int actualAmountFromDB = Integer
					.parseInt(actualSortedListOfHonoredRewardValue.get(indexedOfAmountKey).replace(".0", ""));
			long actualRewardCreditIDFromDB = Long.parseLong(actualSortedListOfRewardCreditId.get(indexedOfAmountKey));

			int expectedAmountFromMap = Integer.parseInt(amount); // amount is the key in the map
			long expectedRewardCreditIDFromMap = Long
					.parseLong(sortedMapOfRewardIDsAsDescendingOrderByKeyExpected.get(amount)); // value in the map

			Assert.assertEquals(actualAmountFromDB, expectedAmountFromMap,
					"Honored reward value does not match for amount: " + amount);

			utils.logPass("Both actual and expected Honored reward value match for amount: " + amount
					+ " at index number : " + indexedOfAmountKey);

			Assert.assertEquals(actualRewardCreditIDFromDB, expectedRewardCreditIDFromMap,
					"Reward credit ID does not match for amount: " + amount);

			utils.logPass(amount + " amount as Key Both actual and expected Reward credit ID match for ID : "
					+ actualRewardCreditIDFromDB + " at index number : " + indexedOfAmountKey);

		}

	}

//			Case 4
//			reward_credit_id 1 -> 17326729 [created_at 2025-06-23 09:57:06]
//			reward_credit_id 2 -> 17326730 [created_at 2025-06-23 09:56:58]
//			Result -> Redeemed via POS API -> reward_debit created with reward_credit_id 17326730/

	@Test(description = "LPE-T2174/SQ-T6593 Verify reward_credit utilisation is based on expiring_at nearest when user has multiple reward_credits present with different expiring_at present in all entries [redeem_soonest_expiring_currency_first -> true]", groups = {
			"regression", "dailyrun" }, priority = 2)
	@Owner(name = "Shashank Sharma")
	public void LPE_T2174_ValidateRewardCreditEntryUtilisationForNearestExpiryDateWhenRedeemSoonestExpiringCurrencyFirstFlagTrue()
			throws Exception {

		String b_id = dataSet.get("business_id");

		// enable flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String businessPreferences = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		
//		Assert.assertTrue(businessPreferences.contains(dataSet.get("ommFlag")),
//				"Business preferences does not contain the flag: " + dataSet.get("ommFlag"));
//		utils.logPass("Business preferences contains the flag: " + dataSet.get("ommFlag"));
//		logger.info("Business preferences contains the flag: " + dataSet.get("ommFlag"));

		DBUtils.updateSingleValueForGivenParameter(env, businessPreferences, "true", dataSet.get("ommFlag"), b_id);
		Map<String, String> mapOfRewardIDs = new LinkedHashMap<>();

		List<String> listOfRewardAmount = new LinkedList<String>();
		listOfRewardAmount.add(dataSet.get("rewardAmount1"));
		listOfRewardAmount.add(dataSet.get("rewardAmount2"));

		String twoDaysFutureDate_forCreatedDate = Utilities.getFutureDateForDBUpdate(2);
		utils.logPass("1 day future date is before one second : " + twoDaysFutureDate_forCreatedDate);

		utils.longWaitInSeconds(1);

		String twoDaysFutureDate1_forCreatedDate = Utilities.getFutureDateForDBUpdate(1);
		utils.logPass("1 day future date is after one second : " + twoDaysFutureDate1_forCreatedDate);

		String twoDaysFutureDate_forExpiryDate = Utilities.getFutureDateForDBUpdate(5);
		utils.logPass("Two days future date is before one second : " + twoDaysFutureDate_forExpiryDate);

		utils.longWaitInSeconds(1);

		String twoDaysFutureDate1_forExpiryDate = Utilities.getFutureDateForDBUpdate(5);
		utils.logPass("Two days future date is after one second : " + twoDaysFutureDate1_forExpiryDate);

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				listOfRewardAmount.get(0), "", "", "");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				listOfRewardAmount.get(1), "", "", "");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		String updateCreatedDateQuery1 = updateCreatedDateInRewardCreditTable
				.replace("${createdDate}", twoDaysFutureDate_forCreatedDate).replace("${userID}", userID)
				.replace("${amount}", listOfRewardAmount.get(0));

		int rs0 = DBUtils.executeUpdateQuery(env, updateCreatedDateQuery1);
		Assert.assertEquals(rs0, 1);

		String updateCreatedDateQuery2 = updateCreatedDateInRewardCreditTable
				.replace("${createdDate}", twoDaysFutureDate1_forCreatedDate).replace("${userID}", userID)
				.replace("${amount}", listOfRewardAmount.get(1));

		int rs01 = DBUtils.executeUpdateQuery(env, updateCreatedDateQuery2);
		Assert.assertEquals(rs01, 1);

		String updateExpiringDateQuery1 = updateExpiringDateInRewardCreditTable
				.replace("${expDate}", twoDaysFutureDate1_forExpiryDate).replace("${userID}", userID)
				.replace("${amount}", listOfRewardAmount.get(0));

		int rs1 = DBUtils.executeUpdateQuery(env, updateExpiringDateQuery1);
		Assert.assertEquals(rs1, 1);

		String updateExpiringDateQuery2 = updateExpiringDateInRewardCreditTable
				.replace("${expDate}", twoDaysFutureDate_forExpiryDate).replace("${userID}", userID)
				.replace("${amount}", listOfRewardAmount.get(1));

		int rs2 = DBUtils.executeUpdateQuery(env, updateExpiringDateQuery2);
		Assert.assertEquals(rs2, 1);

		for (String amount : listOfRewardAmount) {
			String getRewardIDQuery = getRewardID.replace("${userID}", userID).replace("${amount}", amount);
			String rewardID = DBUtils.executeQueryAndGetColumnValue(env, getRewardIDQuery, "id");
			Assert.assertNotNull(rewardID, "Reward ID is null for amount: " + amount);
			utils.logPass("Reward id " + rewardID + " is generated successfully for the amount " + amount);
			mapOfRewardIDs.put(amount, rewardID);
		}

		// Sort the map of reward IDs in descending order by key (amount)
		Map<String, String> sortedMapOfRewardIDsAsDescendingOrderByKeyExpected = new LinkedHashMap<>();
		sortedMapOfRewardIDsAsDescendingOrderByKeyExpected = Utilities.sortByKeyDescending(mapOfRewardIDs);

		utils.logPass("Sorted Map of Reward IDs in descending order by key: "
				+ sortedMapOfRewardIDsAsDescendingOrderByKeyExpected);

		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();

		Response redemptionResponse = pageObj.endpoints().posRedemptionOfAmount(userEmail, date, key, txn,
				dataSet.get("redemptionAmount"), dataSet.get("locationkey"));
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for pos redemption of amount");
		utils.logPass("POS Redemption of amount " + dataSet.get("redemptionAmount") + " is successful");

		// SQL query to get the reward credit IDs and honored reward values in
		// descending order
		String rewardDebitsIDDescOrderQuery = "SELECT reward_credit_id , honored_reward_value FROM reward_debits WHERE user_id = "
				+ userID + " ORDER BY honored_reward_value DESC LIMIT 3;";

		int counter = 0;

		List<String> actualSortedListOfRewardCreditId = new LinkedList<>();
		List<String> actualSortedListOfHonoredRewardValue = new LinkedList<>();

		while ((actualSortedListOfRewardCreditId.isEmpty() || actualSortedListOfHonoredRewardValue.isEmpty())
				&& counter < 20) {
			actualSortedListOfRewardCreditId = DBUtils.getValueFromColumnInList(env,
					rewardDebitsIDDescOrderQuery, "reward_credit_id");
			utils.logit(
					counter + " : actualSortedListOfRewardCreditId LIST items : " + actualSortedListOfRewardCreditId);

			actualSortedListOfHonoredRewardValue = DBUtils.getValueFromColumnInList(env,
					rewardDebitsIDDescOrderQuery, "honored_reward_value");
			utils.logit(counter + " : actualSortedListOfHonoredRewardValue LIST items : "
					+ actualSortedListOfHonoredRewardValue);
			counter++;
			utils.longWaitInSeconds(3);
		}

		Assert.assertTrue(actualSortedListOfRewardCreditId.size() > 0, "reward_credit_id list is null");

		Assert.assertTrue(actualSortedListOfHonoredRewardValue.size() > 0, "honored_reward_value list is null");

		// Sort the expected amount lists in descending order
		listOfRewardAmount.sort(Comparator.reverseOrder());

		// Validate the actual and expected reward credit IDs and honored reward values
		// in order
		for (String amount : listOfRewardAmount) {

			int indexedOfAmountKey = Utilities.getKeyIndex(sortedMapOfRewardIDsAsDescendingOrderByKeyExpected, amount);

			int actualAmountFromDB = Integer
					.parseInt(actualSortedListOfHonoredRewardValue.get(indexedOfAmountKey).replace(".0", ""));
			long actualRewardCreditIDFromDB = Long.parseLong(actualSortedListOfRewardCreditId.get(indexedOfAmountKey));

			int expectedAmountFromMap = Integer.parseInt(amount); // amount is the key in the map
			long expectedRewardCreditIDFromMap = Long
					.parseLong(sortedMapOfRewardIDsAsDescendingOrderByKeyExpected.get(amount)); // value in the map

			Assert.assertEquals(actualAmountFromDB, expectedAmountFromMap,
					"Honored reward value does not match for amount: " + amount);

			utils.logPass("Both actual and expected Honored reward value match for amount: " + amount
					+ " at index number : " + indexedOfAmountKey);

			Assert.assertEquals(actualRewardCreditIDFromDB, expectedRewardCreditIDFromMap,
					"Reward credit ID does not match for amount: " + amount);

			utils.logPass(amount + " amount as Key Both actual and expected Reward credit ID match for ID : "
					+ actualRewardCreditIDFromDB + " at index number : " + indexedOfAmountKey);

		}

	}

//	Case 1
//	reward_credit_id 1 -> 17326753 [created_at 2025-06-25 11:31:32]
//	reward_credit_id 2 -> 17326754 [created_at 2025-06-25 11:31:35]
//	Result -> Redeemed via POS API -> reward_debit created with reward_credit_id -> 17326754

	@Test(description = "LPE-T2175 /SQ-T6594 Verify reward_credit entry utilisation when user has multiple reward_credits present with expiring_at present in some entries and expiring_at blank in some entries [redeem_soonest_expiring_currency_first -> true]", groups = {
			"regression", "dailyrun" }, priority = 3)
	@Owner(name = "Shashank Sharma")
	public void LPE_T2175_ValidateRewardCreditEntryUtilisationForNearestExpiryDateWhenRedeemSoonestExpiringCurrencyFirstFlagTrue()
			throws Exception {

		String b_id = dataSet.get("business_id");

		// enable flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String businessPreferences = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		
//		Assert.assertTrue(businessPreferences.contains(dataSet.get("ommFlag")),
//				"Business preferences does not contain the flag: " + dataSet.get("ommFlag"));
//		utils.logPass("Business preferences contains the flag: " + dataSet.get("ommFlag"));
//		logger.info("Business preferences contains the flag: " + dataSet.get("ommFlag"));

		DBUtils.updateSingleValueForGivenParameter(env, businessPreferences, "true", dataSet.get("ommFlag"), b_id);

		Map<String, String> mapOfRewardIDs = new LinkedHashMap<>();

		List<String> listOfRewardAmount = new LinkedList<String>();
		listOfRewardAmount.add(dataSet.get("rewardAmount1"));
		listOfRewardAmount.add(dataSet.get("rewardAmount2"));

		String twoDaysFutureDate_forCreatedDate = Utilities.getFutureDateForDBUpdate(2);
		utils.logPass("1 day future date is before one second : " + twoDaysFutureDate_forCreatedDate);

		utils.longWaitInSeconds(1);

		String twoDaysFutureDate1_forCreatedDate = Utilities.getFutureDateForDBUpdate(1);
		utils.logPass("1 day future date is after one second : " + twoDaysFutureDate1_forCreatedDate);

		String twoDaysFutureDate_forExpiryDate = Utilities.getFutureDateForDBUpdate(5);
		utils.logPass("Two days future date is before one second : " + twoDaysFutureDate_forExpiryDate);

		utils.longWaitInSeconds(1);

		String twoDaysFutureDate1_forExpiryDate = Utilities.getFutureDateForDBUpdate(5);
		utils.logPass("Two days future date is after one second : " + twoDaysFutureDate1_forExpiryDate);

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				listOfRewardAmount.get(0), "", "", "");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				listOfRewardAmount.get(1), "", "", "");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		String updateCreatedDateQuery1 = updateCreatedDateInRewardCreditTable
				.replace("${createdDate}", twoDaysFutureDate_forCreatedDate).replace("${userID}", userID)
				.replace("${amount}", listOfRewardAmount.get(0));

		int rs0 = DBUtils.executeUpdateQuery(env, updateCreatedDateQuery1);
		Assert.assertEquals(rs0, 1);

		String updateCreatedDateQuery2 = updateCreatedDateInRewardCreditTable
				.replace("${createdDate}", twoDaysFutureDate1_forCreatedDate).replace("${userID}", userID)
				.replace("${amount}", listOfRewardAmount.get(1));

		int rs01 = DBUtils.executeUpdateQuery(env, updateCreatedDateQuery2);
		Assert.assertEquals(rs01, 1);

//		String updateExpiringDateQuery1 = updateExpiringDateInRewardCreditTable
//				.replace("${expDate}", twoDaysFutureDate1_forExpiryDate).replace("${userID}", userID)
//				.replace("${amount}", listOfRewardAmount.get(0));
//
//		int rs1 = DBUtils.executeUpdateQuery(env, updateExpiringDateQuery1);
//		Assert.assertEquals(rs1, 1);

		String updateExpiringDateQuery2 = updateExpiringDateInRewardCreditTable
				.replace("${expDate}", twoDaysFutureDate_forExpiryDate).replace("${userID}", userID)
				.replace("${amount}", listOfRewardAmount.get(1));

		int rs2 = DBUtils.executeUpdateQuery(env, updateExpiringDateQuery2);
		Assert.assertEquals(rs2, 1);

		for (String amount : listOfRewardAmount) {
			String getRewardIDQuery = getRewardID.replace("${userID}", userID).replace("${amount}", amount);
			String rewardID = DBUtils.executeQueryAndGetColumnValue(env, getRewardIDQuery, "id");
			Assert.assertNotNull(rewardID, "Reward ID is null for amount: " + amount);
			utils.logPass("Reward id " + rewardID + " is generated successfully for the amount " + amount);
			mapOfRewardIDs.put(amount, rewardID);
		}

		// Sort the map of reward IDs in descending order by key (amount)
		Map<String, String> sortedMapOfRewardIDsAsDescendingOrderByKeyExpected = new LinkedHashMap<>();
		sortedMapOfRewardIDsAsDescendingOrderByKeyExpected = Utilities.sortByKeyDescending(mapOfRewardIDs);

		utils.logPass("Sorted Map of Reward IDs in descending order by key: "
				+ sortedMapOfRewardIDsAsDescendingOrderByKeyExpected);

		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();

		Response redemptionResponse = pageObj.endpoints().posRedemptionOfAmount(userEmail, date, key, txn,
				dataSet.get("redemptionAmount"), dataSet.get("locationkey"));
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for pos redemption of amount");
		utils.logPass("POS Redemption of amount " + dataSet.get("redemptionAmount") + " is successful");

		// SQL query to get the reward credit IDs and honored reward values in
		// descending order
		String rewardDebitsIDDescOrderQuery = "SELECT reward_credit_id , honored_reward_value FROM reward_debits WHERE user_id = "
				+ userID + " ORDER BY honored_reward_value DESC LIMIT 3;";

		int counter = 0;

		List<String> actualSortedListOfRewardCreditId = new LinkedList<>();
		List<String> actualSortedListOfHonoredRewardValue = new LinkedList<>();

		while ((actualSortedListOfRewardCreditId.isEmpty() || actualSortedListOfHonoredRewardValue.isEmpty())
				&& counter < 20) {
			actualSortedListOfRewardCreditId = DBUtils.getValueFromColumnInList(env,
					rewardDebitsIDDescOrderQuery, "reward_credit_id");
			utils.logit(
					counter + " : actualSortedListOfRewardCreditId LIST items : " + actualSortedListOfRewardCreditId);

			actualSortedListOfHonoredRewardValue = DBUtils.getValueFromColumnInList(env,
					rewardDebitsIDDescOrderQuery, "honored_reward_value");
			utils.logit(counter + " : actualSortedListOfHonoredRewardValue LIST items : "
					+ actualSortedListOfHonoredRewardValue);
			counter++;
			utils.longWaitInSeconds(3);
		}

		Assert.assertTrue(actualSortedListOfRewardCreditId.size() > 0, "reward_credit_id list is null");

		Assert.assertTrue(actualSortedListOfHonoredRewardValue.size() > 0, "honored_reward_value list is null");

		// Sort the expected amount lists in descending order
		listOfRewardAmount.sort(Comparator.reverseOrder());

		// Validate the actual and expected reward credit IDs and honored reward values
		// in order
		for (String amount : listOfRewardAmount) {

			int indexedOfAmountKey = Utilities.getKeyIndex(sortedMapOfRewardIDsAsDescendingOrderByKeyExpected, amount);

			int actualAmountFromDB = Integer
					.parseInt(actualSortedListOfHonoredRewardValue.get(indexedOfAmountKey).replace(".0", ""));
			long actualRewardCreditIDFromDB = Long.parseLong(actualSortedListOfRewardCreditId.get(indexedOfAmountKey));

			int expectedAmountFromMap = Integer.parseInt(amount); // amount is the key in the map
			long expectedRewardCreditIDFromMap = Long
					.parseLong(sortedMapOfRewardIDsAsDescendingOrderByKeyExpected.get(amount)); // value in the map

			Assert.assertEquals(actualAmountFromDB, expectedAmountFromMap,
					"Honored reward value does not match for amount: " + amount);

			utils.logPass("Both actual and expected Honored reward value match for amount: " + amount
					+ " at index number : " + indexedOfAmountKey);

			Assert.assertEquals(actualRewardCreditIDFromDB, expectedRewardCreditIDFromMap,
					"Reward credit ID does not match for amount: " + amount);

			utils.logPass(amount + " amount as Key Both actual and expected Reward credit ID match for ID : "
					+ actualRewardCreditIDFromDB + " at index number : " + indexedOfAmountKey);

		}

	}

//	Case 4
//	reward_credit_id 1 -> 17326729 [created_at 2025-06-23 09:57:06]
//	reward_credit_id 2 -> 17326730 [created_at 2025-06-23 09:56:58]
//	Result -> Redeemed via POS API -> reward_debit created with reward_credit_id 17326729

	@Test(description = "LPE-T2187 / SQ-T6595 Verify reward_credit utilisation is based on reward_credit_id earned first when user has multiple reward_credits present with same expiring_at present in all entries [redeem_soonest_expiring_currency_first -> true]", groups = {
			"regression", "dailyrun" }, priority = 4)
	@Owner(name = "Shashank Sharma")
	public void LPE_T2187_ValidateRewardCreditEntryUtilisationForNearestExpiryDateWhenRedeemSoonestExpiringCurrencyFirstFlagTrue()
			throws Exception {

		String b_id = dataSet.get("business_id");

		// enable flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String businessPreferences = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		
//		Assert.assertTrue(businessPreferences.contains(dataSet.get("ommFlag")),
//				"Business preferences does not contain the flag: " + dataSet.get("ommFlag"));
//		utils.logPass("Business preferences contains the flag: " + dataSet.get("ommFlag"));
//		logger.info("Business preferences contains the flag: " + dataSet.get("ommFlag"));

		DBUtils.updateSingleValueForGivenParameter(env, businessPreferences, "true", dataSet.get("ommFlag"), b_id);
		Map<String, String> mapOfRewardIDs = new LinkedHashMap<>();

		List<String> listOfRewardAmount = new LinkedList<String>();
		listOfRewardAmount.add(dataSet.get("rewardAmount1"));
		listOfRewardAmount.add(dataSet.get("rewardAmount2"));

		String twoDaysFutureDate_forCreatedDate = Utilities.getFutureDateForDBUpdate(2);
		utils.logPass("1 day future date is before one second : " + twoDaysFutureDate_forCreatedDate);

		String twoDaysFutureDate_forExpiryDate = Utilities.getFutureDateForDBUpdate(5);
		utils.logPass("Two days future date is before one second : " + twoDaysFutureDate_forExpiryDate);

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				listOfRewardAmount.get(0), "", "", "");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				listOfRewardAmount.get(1), "", "", "");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		String updateCreatedDateQuery1 = updateCreatedDateInRewardCreditTable
				.replace("${createdDate}", twoDaysFutureDate_forCreatedDate).replace("${userID}", userID)
				.replace("${amount}", listOfRewardAmount.get(0));

		int rs0 = DBUtils.executeUpdateQuery(env, updateCreatedDateQuery1);
		Assert.assertEquals(rs0, 1);

		String updateCreatedDateQuery2 = updateCreatedDateInRewardCreditTable
				.replace("${createdDate}", twoDaysFutureDate_forCreatedDate).replace("${userID}", userID)
				.replace("${amount}", listOfRewardAmount.get(1));

		int rs01 = DBUtils.executeUpdateQuery(env, updateCreatedDateQuery2);
		Assert.assertEquals(rs01, 1);

		String updateExpiringDateQuery1 = updateExpiringDateInRewardCreditTable
				.replace("${expDate}", twoDaysFutureDate_forExpiryDate).replace("${userID}", userID)
				.replace("${amount}", listOfRewardAmount.get(0));

		int rs1 = DBUtils.executeUpdateQuery(env, updateExpiringDateQuery1);
		Assert.assertEquals(rs1, 1);

		String updateExpiringDateQuery2 = updateExpiringDateInRewardCreditTable
				.replace("${expDate}", twoDaysFutureDate_forExpiryDate).replace("${userID}", userID)
				.replace("${amount}", listOfRewardAmount.get(1));

		int rs2 = DBUtils.executeUpdateQuery(env, updateExpiringDateQuery2);
		Assert.assertEquals(rs2, 1);

		for (String amount : listOfRewardAmount) {
			String getRewardIDQuery = getRewardID.replace("${userID}", userID).replace("${amount}", amount);
			String rewardID = DBUtils.executeQueryAndGetColumnValue(env, getRewardIDQuery, "id");
			Assert.assertNotNull(rewardID, "Reward ID is null for amount: " + amount);
			utils.logPass("Reward id " + rewardID + " is generated successfully for the amount " + amount);
			mapOfRewardIDs.put(amount, rewardID);
		}

		Assert.assertTrue(mapOfRewardIDs.size() > 0, "mapOfRewardIDs map does not have any reward id");
		utils.logPass("Sorted Map of Reward IDs in descending order by key: " + mapOfRewardIDs);

		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();

		Response redemptionResponse = pageObj.endpoints().posRedemptionOfAmount(userEmail, date, key, txn,
				dataSet.get("redemptionAmount"), dataSet.get("locationkey"));
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for pos redemption of amount");
		utils.logPass("POS Redemption of amount " + dataSet.get("redemptionAmount") + " is successful");

//SQL query to get the reward credit IDs and honored reward values in ascending order
		String rewardDebitsIDAscendingOrderQuery = "SELECT reward_credit_id , honored_reward_value FROM reward_debits WHERE user_id = "
				+ userID + " ORDER BY honored_reward_value ASC LIMIT 3;";

		int counter = 0;

		List<String> actualSortedListOfRewardCreditId = new LinkedList<>();
		List<String> actualSortedListOfHonoredRewardValue = new LinkedList<>();

		while ((actualSortedListOfRewardCreditId.isEmpty() || actualSortedListOfHonoredRewardValue.isEmpty())
				&& counter < 20) {
			actualSortedListOfRewardCreditId = DBUtils.getValueFromColumnInList(env,
					rewardDebitsIDAscendingOrderQuery, "reward_credit_id");
			utils.logit(
					counter + " : actualSortedListOfRewardCreditId LIST items : " + actualSortedListOfRewardCreditId);

			actualSortedListOfHonoredRewardValue = DBUtils.getValueFromColumnInList(env,
					rewardDebitsIDAscendingOrderQuery, "honored_reward_value");
			utils.logit(counter + " : actualSortedListOfHonoredRewardValue LIST items : "
					+ actualSortedListOfHonoredRewardValue);
			counter++;
			utils.longWaitInSeconds(3);
		}

		Assert.assertTrue(actualSortedListOfRewardCreditId.size() > 0, "reward_credit_id list is null");

		Assert.assertTrue(actualSortedListOfHonoredRewardValue.size() > 0, "honored_reward_value list is null");

// Validate the actual and expected reward credit IDs and honored reward values in order
		for (String amount : listOfRewardAmount) {

			int indexedOfAmountKey = Utilities.getKeyIndex(mapOfRewardIDs, amount);

			int actualAmountFromDB = Integer
					.parseInt(actualSortedListOfHonoredRewardValue.get(indexedOfAmountKey).replace(".0", ""));
			long actualRewardCreditIDFromDB = Long.parseLong(actualSortedListOfRewardCreditId.get(indexedOfAmountKey));

			int expectedAmountFromMap = Integer.parseInt(amount); // amount is the key in the map
			long expectedRewardCreditIDFromMap = Long.parseLong(mapOfRewardIDs.get(amount)); // value in the map

			Assert.assertEquals(actualAmountFromDB, expectedAmountFromMap,
					"Honored reward value does not match for amount: " + amount);

			utils.logPass("Both actual and expected Honored reward value match for amount: " + amount
					+ " at index number : " + indexedOfAmountKey);

			Assert.assertEquals(actualRewardCreditIDFromDB, expectedRewardCreditIDFromMap,
					"Reward credit ID does not match for amount: " + amount);

			utils.logPass(amount + " amount as Key Both actual and expected Reward credit ID match for ID : "
					+ actualRewardCreditIDFromDB + " at index number : " + indexedOfAmountKey);

		}

	}

//	Case 4
//	reward_credit_id 1 -> 17326729 [created_at 2025-06-23 09:57:06]
//	reward_credit_id 2 -> 17326730 [created_at 2025-06-23 09:56:58]
//	Result -> Redeemed via POS API -> reward_debit created with reward_credit_id 17326729

	@Test(description = "LPE-T2204 /SQ-T6596  Verify reward_credit entry utilisation old logic when user has multiple reward_credits present with expiring_at present in some entries and expiring_at blank in some entries [redeem_soonest_expiring_currency_first -> false]", groups = {
			"regression", "dailyrun" }, priority = 5)
	@Owner(name = "Shashank Sharma")
	public void  LPE_T2204_ValidateRewardCreditEntryUtilisationForNearestExpiryDateWhenRedeemSoonestExpiringCurrencyFirstFlagFalse()
			throws Exception {

		String b_id = dataSet.get("business_id");

		// enable flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String businessPreferences = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		
//		Assert.assertTrue(businessPreferences.contains(dataSet.get("ommFlag")),
//				"Business preferences does not contain the flag: " + dataSet.get("ommFlag"));
//		utils.logPass("Business preferences contains the flag: " + dataSet.get("ommFlag"));
//		logger.info("Business preferences contains the flag: " + dataSet.get("ommFlag"));
		
		DBUtils.updateSingleValueForGivenParameter(env, businessPreferences, "false", dataSet.get("ommFlag"), b_id);

		Map<String, String> mapOfRewardIDs = new LinkedHashMap<>();

		List<String> listOfRewardAmount = new LinkedList<String>();
		listOfRewardAmount.add(dataSet.get("rewardAmount1"));
		listOfRewardAmount.add(dataSet.get("rewardAmount2"));
		listOfRewardAmount.add(dataSet.get("rewardAmount3"));
		listOfRewardAmount.add(dataSet.get("rewardAmount4"));

		String twoDaysFutureDate_forCreatedDate1 = Utilities.getFutureDateForDBUpdate(2);
		utils.logPass(" days future date is before one second : " + twoDaysFutureDate_forCreatedDate1);

		utils.longWaitInSeconds(2);

		String twoDaysFutureDate_forCreatedDate2 = Utilities.getFutureDateForDBUpdate(2);
		utils.logPass(" days future date is before one second : " + twoDaysFutureDate_forCreatedDate2);

		utils.longWaitInSeconds(2);

		String twoDaysFutureDate_forCreatedDate3 = Utilities.getFutureDateForDBUpdate(2);
		utils.logPass(" days future date is before one second : " + twoDaysFutureDate_forCreatedDate3);

		utils.longWaitInSeconds(2);

		String twoDaysFutureDate_forCreatedDate4 = Utilities.getFutureDateForDBUpdate(2);
		utils.logPass(" days future date is before one second : " + twoDaysFutureDate_forCreatedDate4);

		String twoDaysFutureDate_forExpiryDate = Utilities.getFutureDateForDBUpdate(5);
		utils.logPass("Two days future date is before one second : " + twoDaysFutureDate_forExpiryDate);

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				listOfRewardAmount.get(0), "", "", "");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				listOfRewardAmount.get(1), "", "", "");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				listOfRewardAmount.get(2), "", "", "");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse3.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse4 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				listOfRewardAmount.get(3), "", "", "");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse4.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		// update expiry and created date in reward credit table

		String updateCreatedDateQuery1 = updateCreatedDateInRewardCreditTable
				.replace("${createdDate}", twoDaysFutureDate_forCreatedDate1).replace("${userID}", userID)
				.replace("${amount}", listOfRewardAmount.get(0));

		int rs0 = DBUtils.executeUpdateQuery(env, updateCreatedDateQuery1);
		Assert.assertEquals(rs0, 1);

		String updateCreatedDateQuery2 = updateCreatedDateInRewardCreditTable
				.replace("${createdDate}", twoDaysFutureDate_forCreatedDate2).replace("${userID}", userID)
				.replace("${amount}", listOfRewardAmount.get(1));

		int rs1 = DBUtils.executeUpdateQuery(env, updateCreatedDateQuery2);
		Assert.assertEquals(rs1, 1);

		String updateCreatedDateQuery3 = updateCreatedDateInRewardCreditTable
				.replace("${createdDate}", twoDaysFutureDate_forCreatedDate3).replace("${userID}", userID)
				.replace("${amount}", listOfRewardAmount.get(2));

		int rs3 = DBUtils.executeUpdateQuery(env, updateCreatedDateQuery3);
		Assert.assertEquals(rs3, 1);

		String updateCreatedDateQuery4 = updateCreatedDateInRewardCreditTable
				.replace("${createdDate}", twoDaysFutureDate_forCreatedDate4).replace("${userID}", userID)
				.replace("${amount}", listOfRewardAmount.get(3));

		int rs4 = DBUtils.executeUpdateQuery(env, updateCreatedDateQuery4);
		Assert.assertEquals(rs4, 1);

//		String updateExpiringDateQuery1 = updateExpiringDateInRewardCreditTable
//				.replace("${expDate}", twoDaysFutureDate_forExpiryDate).replace("${userID}", userID)
//				.replace("${amount}", listOfRewardAmount.get(0));
//
//		int rs5 = DBUtils.executeUpdateQuery(env, updateExpiringDateQuery1);
//		Assert.assertEquals(rs5, 1);
//		

		String updateExpiringDateQuery2 = updateExpiringDateInRewardCreditTable
				.replace("${expDate}", twoDaysFutureDate_forExpiryDate).replace("${userID}", userID)
				.replace("${amount}", listOfRewardAmount.get(1));

		int rs6 = DBUtils.executeUpdateQuery(env, updateExpiringDateQuery2);
		Assert.assertEquals(rs6, 1);

//		String updateExpiringDateQuery3 = updateExpiringDateInRewardCreditTable
//				.replace("${expDate}", twoDaysFutureDate_forExpiryDate).replace("${userID}", userID)
//				.replace("${amount}", listOfRewardAmount.get(2));
//
//		int rs7 = DBUtils.executeUpdateQuery(env, updateExpiringDateQuery3);
//		Assert.assertEquals(rs7, 1);

		String updateExpiringDateQuery4 = updateExpiringDateInRewardCreditTable
				.replace("${expDate}", twoDaysFutureDate_forExpiryDate).replace("${userID}", userID)
				.replace("${amount}", listOfRewardAmount.get(3));

		int rs8 = DBUtils.executeUpdateQuery(env, updateExpiringDateQuery4);
		Assert.assertEquals(rs8, 1);

		for (String amount : listOfRewardAmount) {
			String getRewardIDQuery = getRewardID.replace("${userID}", userID).replace("${amount}", amount);
			String rewardID = DBUtils.executeQueryAndGetColumnValue(env, getRewardIDQuery, "id");
			Assert.assertNotNull(rewardID, "Reward ID is null for amount: " + amount);
			utils.logPass("Reward id " + rewardID + " is generated successfully for the amount " + amount);
			mapOfRewardIDs.put(amount, rewardID);
		}

		utils.logPass("Sorted Map of Reward IDs in descending order by key: " + mapOfRewardIDs);

		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();

		Response redemptionResponse = pageObj.endpoints().posRedemptionOfAmount(userEmail, date, key, txn,
				dataSet.get("redemptionAmount"), dataSet.get("locationkey"));
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for pos redemption of amount");
		utils.logPass("POS Redemption of amount " + dataSet.get("redemptionAmount") + " is successful");

//SQL query to get the reward credit IDs and honored reward values in Ascending order
		String rewardDebitsIDAscendingOrderQuery = "SELECT reward_credit_id , honored_reward_value FROM reward_debits WHERE user_id = "
				+ userID + " ORDER BY honored_reward_value ASC LIMIT 10;";

		int counter = 0;

		List<String> actualSortedListOfRewardCreditId = new LinkedList<>();
		List<String> actualSortedListOfHonoredRewardValue = new LinkedList<>();

		while ((actualSortedListOfRewardCreditId.isEmpty() || actualSortedListOfHonoredRewardValue.isEmpty())
				&& counter < 20) {
			actualSortedListOfRewardCreditId = DBUtils.getValueFromColumnInList(env,
					rewardDebitsIDAscendingOrderQuery, "reward_credit_id");
			utils.logit(
					counter + " : actualSortedListOfRewardCreditId LIST items : " + actualSortedListOfRewardCreditId);

			actualSortedListOfHonoredRewardValue = DBUtils.getValueFromColumnInList(env,
					rewardDebitsIDAscendingOrderQuery, "honored_reward_value");
			utils.logit(counter + " : actualSortedListOfHonoredRewardValue LIST items : "
					+ actualSortedListOfHonoredRewardValue);
			counter++;
			utils.longWaitInSeconds(3);
		}

		Assert.assertTrue(actualSortedListOfRewardCreditId.size() > 0, "reward_credit_id list is null");

		Assert.assertTrue(actualSortedListOfHonoredRewardValue.size() > 0, "honored_reward_value list is null");

// Validate the actual and expected reward credit IDs and honored reward values in order
		for (String amount : listOfRewardAmount) {

			int indexedOfAmountKey = Utilities.getKeyIndex(mapOfRewardIDs, amount);

			int actualAmountFromDB = Integer
					.parseInt(actualSortedListOfHonoredRewardValue.get(indexedOfAmountKey).replace(".0", ""));
			long actualRewardCreditIDFromDB = Long.parseLong(actualSortedListOfRewardCreditId.get(indexedOfAmountKey));

			int expectedAmountFromMap = Integer.parseInt(amount); // amount is the key in the map
			long expectedRewardCreditIDFromMap = Long.parseLong(mapOfRewardIDs.get(amount)); // value in the map

			Assert.assertEquals(actualAmountFromDB, expectedAmountFromMap,
					"Honored reward value does not match for amount: " + amount);

			utils.logPass("Both actual and expected Honored reward value match for amount: " + amount
					+ " at index number : " + indexedOfAmountKey);

			Assert.assertEquals(actualRewardCreditIDFromDB, expectedRewardCreditIDFromMap,
					"Reward credit ID does not match for amount: " + amount);

			utils.logPass(amount + " amount as Key Both actual and expected Reward credit ID match for ID : "
					+ actualRewardCreditIDFromDB + " at index number : " + indexedOfAmountKey);

		}

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}