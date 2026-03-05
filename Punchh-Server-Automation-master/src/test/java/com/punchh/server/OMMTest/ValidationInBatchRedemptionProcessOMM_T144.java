package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;

import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class ValidationInBatchRedemptionProcessOMM_T144 {
	static Logger logger = LogManager.getLogger(ValidationInBatchRedemptionProcessOMM_T144.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String userEmail;
	boolean enableMenuItemAggregatorFlag;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
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
		enableMenuItemAggregatorFlag = false;
	}

	@Test(description = "SQ-T3445 [Batched Redemptions] Verify cases for Item Qualifiers expression \"Line Item Exists\"")
	public void verify144_1() throws InterruptedException {
		double expRedeemption1DiscountAmt = Double.parseDouble(dataSet.get("expRedeemptionOneDiscountAmount1"));

		double expRedeemption2DiscountAmt = Double.parseDouble(dataSet.get("expRedeemptionOneDiscountAmount2"));

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logit("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse3.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID1 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");
		String rewardID2 = rewardResponse.jsonPath().getString("id[1]").replace("[", "").replace("]", "");
		String rewardID3 = rewardResponse.jsonPath().getString("id[2]").replace("[", "").replace("]", "");

		pageObj.utils().logit("Reward id " + rewardID1 + " & " + rewardID2 + "is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID1);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response discountBasketResponse3 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID3);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints().processBatchRedemptionOfBasketPOSAPI107(
				dataSet.get("locationKey"), userID, "15", "8", "7", "3", "1001", "2001", "2001");

		System.out
				.println("batchRedemptionProcessResponseUser1=" + batchRedemptionProcessResponseUser1.asPrettyString());

		double actualRedeemption1DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption1DiscountAmt, expRedeemption1DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption1 =" + actualRedeemption1DiscountAmt);

		double actualRedeemption2DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[1].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption2DiscountAmt, expRedeemption2DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption2 =" + actualRedeemption2DiscountAmt);
		double expTotalDiscountAmount = Double.parseDouble(dataSet.get("expTotalDiscountAmount"));

		double actualTotalDiscountAmount = actualRedeemption1DiscountAmt + actualRedeemption2DiscountAmt;

		Assert.assertEquals(actualTotalDiscountAmount, expTotalDiscountAmount);
		pageObj.utils().logPass("Verified the overall total discount amount for the redeemption=" + actualTotalDiscountAmount);

		String expErrorMessage = dataSet.get("expErrorMessage");
		String actualRedeemption2ErrorMessage = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("failures[0].message").replace("[", "").replace("]", "");

		boolean result = pageObj.apiUtils().verifyErrorMessage(actualRedeemption2ErrorMessage, expErrorMessage);

		Assert.assertTrue(result);

		pageObj.utils().logPass("Verified the error message for the redeemption2");
	}

	@Test(description = "SQ-T3445 [Batched Redemptions] Verify cases for Item Qualifiers expression \"Line Item Exists\"")
	public void verify144_2() {
		double expRedeemption1DiscountAmt = Double.parseDouble(dataSet.get("expRedeemptionOneDiscountAmount1"));

		double expRedeemption2DiscountAmt = Double.parseDouble(dataSet.get("expRedeemptionOneDiscountAmount2"));

		double expRedeemption3DiscountAmt = Double.parseDouble(dataSet.get("expRedeemptionOneDiscountAmount3"));
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logit("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse3.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID1 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");
		String rewardID2 = rewardResponse.jsonPath().getString("id[1]").replace("[", "").replace("]", "");
		String rewardID3 = rewardResponse.jsonPath().getString("id[2]").replace("[", "").replace("]", "");

		pageObj.utils().logit("Reward id " + rewardID1 + " & " + rewardID2 + " & " + rewardID3 + "is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID1);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response discountBasketResponse3 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID3);
		pageObj.utils().logit(rewardID3 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasket144(dataSet.get("locationKey"), userID, "20", "8", "12", "1", "3");

		double actualRedeemption1DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption1DiscountAmt, expRedeemption1DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption1 =" + actualRedeemption1DiscountAmt);

		double actualRedeemption2DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[1].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption2DiscountAmt, expRedeemption2DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption2 =" + actualRedeemption2DiscountAmt);

		double actualRedeemption3DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[2].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption3DiscountAmt, expRedeemption3DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption3 =" + actualRedeemption3DiscountAmt);

		double expTotalDiscountAmount = Double.parseDouble(dataSet.get("expTotalDiscountAmount"));

		double actualTotalDiscountAmount = actualRedeemption1DiscountAmt + actualRedeemption2DiscountAmt
				+ actualRedeemption3DiscountAmt;

		Assert.assertEquals(actualTotalDiscountAmount, expTotalDiscountAmount);
		pageObj.utils().logPass("Verified the overall total discount amount for the redeemption=" + actualTotalDiscountAmount);

		String actualDiscountType = batchRedemptionProcessResponseUser1.jsonPath().getString("success[2].discount_type")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actualDiscountType, "reward");

		pageObj.utils().logPass("Verified the discount type in batch process response");
	}

	@Test(description = "SQ-T3445 [Batched Redemptions] Verify cases for Item Qualifiers expression \"Line Item Exists\"")
	public void verify144_3() {
		double expRedeemption1DiscountAmt = Double.parseDouble(dataSet.get("expRedeemptionOneDiscountAmount1"));

		double expRedeemption2DiscountAmt = Double.parseDouble(dataSet.get("expRedeemptionOneDiscountAmount2"));

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logit("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id1"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id1") + " to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID1 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");
		String rewardID2 = rewardResponse.jsonPath().getString("id[1]").replace("[", "").replace("]", "");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID1);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints().processBatchRedemptionOfBasketPOSAPI107(
				dataSet.get("locationKey"), userID, "15", "8", "7", "3", "1001", "2001", "2001");

		double actualRedeemption1DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption1DiscountAmt, expRedeemption1DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption1 =" + actualRedeemption1DiscountAmt);

		double actualRedeemption2DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[1].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption2DiscountAmt, expRedeemption2DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption2 =" + actualRedeemption2DiscountAmt);

		double expTotalDiscountAmount = Double.parseDouble(dataSet.get("expTotalDiscountAmount"));

		double actualTotalDiscountAmount = actualRedeemption1DiscountAmt + actualRedeemption2DiscountAmt;

		Assert.assertEquals(actualTotalDiscountAmount, expTotalDiscountAmount);
		pageObj.utils().logPass("Verified the overall total discount amount for the redeemption=" + actualTotalDiscountAmount);

	}

	@Test(description = "SQ-T3445 [Batched Redemptions] Verify cases for Item Qualifiers expression \"Line Item Exists\"")
	public void verify144_4() throws InterruptedException {

		double expRedeemption1DiscountAmt = Double.parseDouble(dataSet.get("expRedeemptionOneDiscountAmount1"));

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logit("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse3.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID1 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");
		String rewardID2 = rewardResponse.jsonPath().getString("id[1]").replace("[", "").replace("]", "");
		String rewardID3 = rewardResponse.jsonPath().getString("id[2]").replace("[", "").replace("]", "");

		pageObj.utils().logit("Reward id " + rewardID1 + " & " + rewardID2 + "is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID1);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response discountBasketResponse3 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID3);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints().processBatchRedemptionOfBasketPOSAPI144_5(
				dataSet.get("locationKey"), userID, "24", "8", "7", "3", "3", "3");

		System.out
				.println("batchRedemptionProcessResponseUser1=" + batchRedemptionProcessResponseUser1.asPrettyString());

		double actualRedeemption1DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption1DiscountAmt, expRedeemption1DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption1 =" + actualRedeemption1DiscountAmt);

		double actualTotalDiscountAmount = actualRedeemption1DiscountAmt;

		double expTotalDiscountAmount = Double.parseDouble(dataSet.get("expTotalDiscountAmount"));

		Assert.assertEquals(actualTotalDiscountAmount, expTotalDiscountAmount);
		pageObj.utils().logPass("Verified the overall total discount amount for the redeemption=" + actualTotalDiscountAmount);

		String expErrorMessage = dataSet.get("expErrorMessage");
		String actualRedeemption2ErrorMessage = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("failures[0].message").replace("[", "").replace("]", "");

		boolean result = pageObj.apiUtils().verifyErrorMessage(actualRedeemption2ErrorMessage, expErrorMessage);

		Assert.assertTrue(result);

		pageObj.utils().logPass("Verified the error message for the redeemption2");

		String actualRedeemption3ErrorMessage = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("failures[1].message").replace("[", "").replace("]", "");

		boolean result1 = pageObj.apiUtils().verifyErrorMessage(actualRedeemption3ErrorMessage, expErrorMessage);

		Assert.assertTrue(result1);

		pageObj.utils().logPass("Verified the error message for the redeemption2");
	}

	@Test(description = "SQ-T3445 [Batched Redemptions] Verify cases for Item Qualifiers expression \"Line Item Exists\"")
	public void verify144_5() throws InterruptedException {

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logit("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID1 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");
		String rewardID2 = rewardResponse.jsonPath().getString("id[1]").replace("[", "").replace("]", "");

		pageObj.utils().logit("Reward id " + rewardID1 + " & " + rewardID2 + "is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID1);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI109(dataSet.get("locationKey"), userID, "24", "8", "7", "3");

		System.out
				.println("batchRedemptionProcessResponseUser1=" + batchRedemptionProcessResponseUser1.asPrettyString());

		String expErrorMessage = dataSet.get("expErrorMessage");
		String actualRedeemption2ErrorMessage = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("failures[0].message").replace("[", "").replace("]", "");

		boolean result = pageObj.apiUtils().verifyErrorMessage(actualRedeemption2ErrorMessage, expErrorMessage);

		Assert.assertTrue(result);

		pageObj.utils().logPass("Verified the error message for the redeemption2");

		String actualRedeemption3ErrorMessage = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("failures[1].message").replace("[", "").replace("]", "");

		boolean result1 = pageObj.apiUtils().verifyErrorMessage(actualRedeemption3ErrorMessage, expErrorMessage);

		Assert.assertTrue(result1);

		pageObj.utils().logPass("Verified the error message for the redeemption2");
	}

	@Test(description = "SQ-T3445 [Batched Redemptions] Verify cases for Item Qualifiers expression \"Line Item Exists\"")
	public void verify144_6() throws InterruptedException {

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logit("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID1 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");
		String rewardID2 = rewardResponse.jsonPath().getString("id[1]").replace("[", "").replace("]", "");

		pageObj.utils().logit("Reward id " + rewardID1 + " & " + rewardID2 + "is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID1);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints().processBatchRedemptionOfBasketPOSAPI107(
				dataSet.get("locationKey"), userID, "15", "8", "7", "3", "1001", "2001", "2001");

		System.out
				.println("batchRedemptionProcessResponseUser1=" + batchRedemptionProcessResponseUser1.asPrettyString());

		String expErrorMessage = dataSet.get("expErrorMessage");
		String actualRedeemption2ErrorMessage = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("failures[0].message").replace("[", "").replace("]", "");

		boolean result = pageObj.apiUtils().verifyErrorMessage(actualRedeemption2ErrorMessage, expErrorMessage);

		Assert.assertTrue(result);

		pageObj.utils().logPass("Verified the error message for the redeemption2");

		String actualRedeemption3ErrorMessage = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("failures[1].message").replace("[", "").replace("]", "");

		boolean result1 = pageObj.apiUtils().verifyErrorMessage(actualRedeemption3ErrorMessage, expErrorMessage);

		Assert.assertTrue(result1);

		pageObj.utils().logPass("Verified the error message for the redeemption2");
	}

	@Test(description = "SQ-T3445 [Batched Redemptions] Verify cases for Item Qualifiers expression \"Line Item Exists\"")
	public void verify144_7() throws InterruptedException {
		double expRedeemption1DiscountAmt = Double.parseDouble(dataSet.get("expRedeemptionOneDiscountAmount1"));

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logit("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id1"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID1 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");
		String rewardID2 = rewardResponse.jsonPath().getString("id[1]").replace("[", "").replace("]", "");

		pageObj.utils().logit("Reward id " + rewardID1 + " & " + rewardID2 + "is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID1);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints().processBatchRedemptionOfBasketPOSAPI107(
				dataSet.get("locationKey"), userID, "15", "8", "7", "3", "1001", "2001", "101");

		System.out
				.println("batchRedemptionProcessResponseUser1=" + batchRedemptionProcessResponseUser1.asPrettyString());

		double actualRedeemption1DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption1DiscountAmt, expRedeemption1DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption1 =" + actualRedeemption1DiscountAmt);

		double actualTotalDiscountAmount = actualRedeemption1DiscountAmt;

		double expTotalDiscountAmount = Double.parseDouble(dataSet.get("expTotalDiscountAmount"));

		Assert.assertEquals(actualTotalDiscountAmount, expTotalDiscountAmount);
		pageObj.utils().logPass("Verified the overall total discount amount for the redeemption=" + actualTotalDiscountAmount);

		String expErrorMessage = dataSet.get("expErrorMessage");
		String actualRedeemption2ErrorMessage = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("failures[0].message").replace("[", "").replace("]", "");

		boolean result = pageObj.apiUtils().verifyErrorMessage(actualRedeemption2ErrorMessage, expErrorMessage);

		Assert.assertTrue(result);

		pageObj.utils().logPass("Verified the error message for the redeemption2");
	}

	@Test(description = "SQ-T3445 [Batched Redemptions] Verify cases for Item Qualifiers expression \"Line Item Exists\"")
	public void verify144_8() throws InterruptedException {
		double expRedeemption1DiscountAmt = Double.parseDouble(dataSet.get("expRedeemptionOneDiscountAmount1"));

		double expRedeemption2DiscountAmt = Double.parseDouble(dataSet.get("expRedeemptionOneDiscountAmount2"));

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logit("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id1"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID1 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");
		String rewardID2 = rewardResponse.jsonPath().getString("id[1]").replace("[", "").replace("]", "");

		pageObj.utils().logit("Reward id " + rewardID1 + " & " + rewardID2 + "is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID1);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints().processBatchRedemptionOfBasketPOSAPI107(
				dataSet.get("locationKey"), userID, "15", "8", "7", "3", "1001", "2001", "101");

		System.out
				.println("batchRedemptionProcessResponseUser1=" + batchRedemptionProcessResponseUser1.asPrettyString());

		double actualRedeemption1DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption1DiscountAmt, expRedeemption1DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption1 =" + actualRedeemption1DiscountAmt);

		double actualRedeemption2DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[1].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption2DiscountAmt, expRedeemption2DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption2 =" + actualRedeemption2DiscountAmt);
		double expTotalDiscountAmount = Double.parseDouble(dataSet.get("expTotalDiscountAmount"));

		double actualTotalDiscountAmount = actualRedeemption1DiscountAmt + actualRedeemption2DiscountAmt;

		Assert.assertEquals(actualTotalDiscountAmount, expTotalDiscountAmount);
		pageObj.utils().logPass("Verified the overall total discount amount for the redeemption=" + actualTotalDiscountAmount);

	}

	@Test(description = "SQ-T3445 [Batched Redemptions] Verify cases for Item Qualifiers expression \"Line Item Exists\"")
	public void verify144_9() throws InterruptedException {
		double expRedeemption1DiscountAmt = Double.parseDouble(dataSet.get("expRedeemptionOneDiscountAmount1"));

		double expRedeemption2DiscountAmt = Double.parseDouble(dataSet.get("expRedeemptionOneDiscountAmount2"));

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logit("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id1"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID1 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");
		String rewardID2 = rewardResponse.jsonPath().getString("id[1]").replace("[", "").replace("]", "");

		pageObj.utils().logit("Reward id " + rewardID1 + " & " + rewardID2 + "is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID1);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasket144_9(dataSet.get("locationKey"), userID, "15", "5", "10");

		System.out
				.println("batchRedemptionProcessResponseUser1=" + batchRedemptionProcessResponseUser1.asPrettyString());

		double actualRedeemption1DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption1DiscountAmt, expRedeemption1DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption1 =" + actualRedeemption1DiscountAmt);

		double actualRedeemption2DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[1].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption2DiscountAmt, expRedeemption2DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption2 =" + actualRedeemption2DiscountAmt);
		double expTotalDiscountAmount = Double.parseDouble(dataSet.get("expTotalDiscountAmount"));

		double actualTotalDiscountAmount = actualRedeemption1DiscountAmt + actualRedeemption2DiscountAmt;

		Assert.assertEquals(actualTotalDiscountAmount, expTotalDiscountAmount);
		pageObj.utils().logPass("Verified the overall total discount amount for the redeemption=" + actualTotalDiscountAmount);
	}

	@Test(description = "SQ-T3445 [Batched Redemptions] Verify cases for Item Qualifiers expression \"Line Item Exists\"")
	public void verify144_10() throws InterruptedException {
		double expRedeemption1DiscountAmt = Double.parseDouble(dataSet.get("expRedeemptionOneDiscountAmount1"));

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logit("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id1"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID1 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");
		String rewardID2 = rewardResponse.jsonPath().getString("id[1]").replace("[", "").replace("]", "");

		pageObj.utils().logit("Reward id " + rewardID1 + " & " + rewardID2 + "is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID1);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasket107(dataSet.get("locationKey"), userID, "15", "5", "10");

		System.out
				.println("batchRedemptionProcessResponseUser1=" + batchRedemptionProcessResponseUser1.asPrettyString());

		double actualRedeemption1DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption1DiscountAmt, expRedeemption1DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption1 =" + actualRedeemption1DiscountAmt);

		double actualTotalDiscountAmount = actualRedeemption1DiscountAmt;

		double expTotalDiscountAmount = Double.parseDouble(dataSet.get("expTotalDiscountAmount"));

		Assert.assertEquals(actualTotalDiscountAmount, expTotalDiscountAmount);
		pageObj.utils().logPass("Verified the overall total discount amount for the redeemption=" + actualTotalDiscountAmount);

		String expErrorMessage = dataSet.get("expErrorMessage");
		String actualRedeemption2ErrorMessage = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("failures[0].message").replace("[", "").replace("]", "");

		boolean result = pageObj.apiUtils().verifyErrorMessage(actualRedeemption2ErrorMessage, expErrorMessage);

		Assert.assertTrue(result);

		pageObj.utils().logPass("Verified the error message for the redeemption2");
	}

	@Test(description = "SQ-T3445 [Batched Redemptions] Verify cases for Item Qualifiers expression \"Line Item Exists\"")
	public void verify144_11() {
		double expRedeemption1DiscountAmt = Double.parseDouble(dataSet.get("expRedeemptionOneDiscountAmount1"));

		double expRedeemption2DiscountAmt = Double.parseDouble(dataSet.get("expRedeemptionOneDiscountAmount2"));

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logit("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse3.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse4 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse4.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID1 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");
		String rewardID2 = rewardResponse.jsonPath().getString("id[1]").replace("[", "").replace("]", "");
		String rewardID3 = rewardResponse.jsonPath().getString("id[2]").replace("[", "").replace("]", "");
		String rewardID4 = rewardResponse.jsonPath().getString("id[3]").replace("[", "").replace("]", "");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID1);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response discountBasketResponse3 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID3);
		pageObj.utils().logit(rewardID3 + " rewardid is added to the basket ");

		Response discountBasketResponse4 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID4);
		pageObj.utils().logit(rewardID4 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasket144(dataSet.get("locationKey"), userID, "40", "10", "30", "1", "3");

		double actualRedeemption1DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption1DiscountAmt, expRedeemption1DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption1 =" + actualRedeemption1DiscountAmt);

		double actualRedeemption2DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[1].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption2DiscountAmt, expRedeemption2DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption2 =" + actualRedeemption2DiscountAmt);

		double expTotalDiscountAmount = Double.parseDouble(dataSet.get("expTotalDiscountAmount"));

		double actualTotalDiscountAmount = actualRedeemption1DiscountAmt + actualRedeemption2DiscountAmt;

		Assert.assertEquals(actualTotalDiscountAmount, expTotalDiscountAmount);
		pageObj.utils().logPass("Verified the overall total discount amount for the redeemption=" + actualTotalDiscountAmount);

		String expErrorMessage = dataSet.get("expErrorMessage");
		String actualRedeemption3ErrorMessage = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("failures[0].message").replace("[", "").replace("]", "");

		boolean result = pageObj.apiUtils().verifyErrorMessage(actualRedeemption3ErrorMessage, expErrorMessage);

		Assert.assertTrue(result);

		pageObj.utils().logPass("Verified the error message for the redeemption 3");

		String actualRedeemption4ErrorMessage = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("failures[1].message").replace("[", "").replace("]", "");

		boolean result1 = pageObj.apiUtils().verifyErrorMessage(actualRedeemption4ErrorMessage, expErrorMessage);

		Assert.assertTrue(result1);

		pageObj.utils().logPass("Verified the error message for the redeemption 4");

	}
}
