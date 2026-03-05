package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
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

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;

import com.punchh.server.utilities.TestListeners;

import io.restassured.response.Response;

/*
 * @Author :- Ashwini Shetty
 * 
 * TC - OMM-T77
*/

@Listeners(TestListeners.class)
public class ValidationInBatchProcessRedemptionOMM_T111 {
	static Logger logger = LogManager.getLogger(ValidationInBatchProcessRedemptionOMM_T111.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String userEmail;
	private boolean GlobalBenefitRedemptionThrottlingToggle;
	private List<String> codeNameList;

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
		GlobalBenefitRedemptionThrottlingToggle = false;
		codeNameList = new ArrayList<String>();
	}

	@Test(description = "OMM-T111 (1.0) STEP -1 Verify cases for Item Qualifiers expression \"Net Quantity Equal To\" with Global Stacking -> On and Global Reusability -> On", priority = 0)
	public void validatePercentageDiscount_111() {
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

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

		pageObj.utils().logPass("Reward id " + rewardID1 + " & " + rewardID2 + " & " + rewardID3 + "is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID1);
		pageObj.utils().logPass(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		pageObj.utils().logPass(rewardID2 + " rewardid is added to the basket ");

		Response discountBasketResponse3 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID3);
		pageObj.utils().logPass(rewardID3 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasket111(dataSet.get("locationKey"), userID, "20", "3", "1");

		String expErrorMessage = dataSet.get("expErrorMessage");
		String actualRedeemption1ErrorMessage = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("failures[0].message").replace("[", "").replace("]", "");
		boolean result = pageObj.apiUtils().verifyErrorMessage(actualRedeemption1ErrorMessage, expErrorMessage);
		Assert.assertTrue(result);
		pageObj.utils().logPass("Verified the error message for the redeemption 1");

		String actualRedeemption2ErrorMessage = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("failures[1].message").replace("[", "").replace("]", "");
		boolean result1 = pageObj.apiUtils().verifyErrorMessage(actualRedeemption2ErrorMessage, expErrorMessage);
		Assert.assertTrue(result1);
		pageObj.utils().logPass("Verified the error message for the redeemption 2");

		String actualRedeemption3ErrorMessage = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("failures[1].message").replace("[", "").replace("]", "");
		boolean result2 = pageObj.apiUtils().verifyErrorMessage(actualRedeemption3ErrorMessage, expErrorMessage);
		Assert.assertTrue(result2);
		pageObj.utils().logPass("Verified the error message for the redeemption 3");

	}

	@Test(description = "OMM-T111 (1.0) STEP -2 Verify cases for Item Qualifiers expression \"Net Quantity Equal To\" with Global Stacking -> On and Global Reusability -> On", priority = 0)
	public void validatePercentageDiscount1_111() {

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
		pageObj.utils().logPass("API2 Signup is successful");

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

		pageObj.utils().logPass("Reward id " + rewardID1 + " & " + rewardID2 + " & " + rewardID3 + "is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID1);
		pageObj.utils().logPass(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		pageObj.utils().logPass(rewardID2 + " rewardid is added to the basket ");

		Response discountBasketResponse3 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID3);
		pageObj.utils().logPass(rewardID3 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasket111(dataSet.get("locationKey"), userID, "20", "2", "1");

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
		pageObj.utils().logPass("Verified the overall total discount amount for the redeemption =" + actualTotalDiscountAmount);

		String actualDiscountType = batchRedemptionProcessResponseUser1.jsonPath().getString("success[2].discount_type")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actualDiscountType, "reward");

		pageObj.utils().logPass("Verified the discount type in batch process response");
	}

	@Test(description = "OMM-T111 (1.0) STEP -2 Verify cases for Item Qualifiers expression \"Net Quantity Equal To\" with Global Stacking -> On and Global Reusability -> On", priority = 0)
	public void validatePercentageDiscount2_111() {

		double expRedeemption1DiscountAmt = Double.parseDouble(dataSet.get("expRedeemptionOneDiscountAmount"));

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

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

		pageObj.utils().logPass("Reward id " + rewardID1 + " & " + rewardID2 + " & " + rewardID3 + "is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID1);
		pageObj.utils().logPass(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		pageObj.utils().logPass(rewardID2 + " rewardid is added to the basket ");

		Response discountBasketResponse3 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID3);
		pageObj.utils().logPass(rewardID3 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasket111(dataSet.get("locationKey"), userID, "20", "2", "1");

		double actualRedeemption1DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption1DiscountAmt, expRedeemption1DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption1 =" + actualRedeemption1DiscountAmt);

		String expErrorMessage = dataSet.get("expErrorMessage");
		String actualRedeemption2ErrorMessage = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("failures[0].message").replace("[", "").replace("]", "");
		boolean result = pageObj.apiUtils().verifyErrorMessage(actualRedeemption2ErrorMessage, expErrorMessage);
		Assert.assertTrue(result);
		pageObj.utils().logPass("Verified the error message for the redeemption 2");

		String actualRedeemption3ErrorMessage = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("failures[1].message").replace("[", "").replace("]", "");
		boolean result1 = pageObj.apiUtils().verifyErrorMessage(actualRedeemption3ErrorMessage, expErrorMessage);
		Assert.assertTrue(result1);
		pageObj.utils().logPass("Verified the error message for the redeemption 3");
	}

}
