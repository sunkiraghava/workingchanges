package com.punchh.server.OMMTest;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.Listeners;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;

import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;
import io.restassured.response.Response;

/*
 * @Author :- Shashank Sharma
 * 
 * TC - OMM-231 - OMM-T77
*/

@Listeners(TestListeners.class)
public class ValidationInBatchRedemptionProcessOMM_TC77 {
	static Logger logger = LogManager.getLogger(ValidationInBatchRedemptionProcessOMM_TC77.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String userEmail;
	boolean enableMenuItemAggregatorFlag;
	String location1, location2;
	String filterSetName;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
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
		enableMenuItemAggregatorFlag = false;
		location1 = " Jacksonville - 3816";
		location2 = " Daphne - 66233";
		filterSetName = "Only Base Items";
	}

	@Test
	public void validateStep25_TC77() throws InterruptedException {
		DecimalFormat decformat = new DecimalFormat("0.00");
		filterSetName = "Base Items and Modifiers";
		enableMenuItemAggregatorFlag = true;
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();

		String lineItemID1 = Integer.toString(Utilities.getRandomNoFromRange(100, 2000));
		String lineItemFilterName1 = "Automation_LF_ItemID1_" + lineItemID1;
		String itemAmount1 = Integer.toString(Utilities.getRandomNoFromRange(30, 50));
		String modifireItem1Amount = Integer.toString(Utilities.getRandomNoFromRange(20, 30));

		String QCname1 = "Automation_OMM_QC1_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();
		String percentageOff1 = Integer.toString(Utilities.getRandomNoFromRange(5, 15));
		String redeemableName1 = "Auto_OMM_Redeemable1_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();

		String modifireLineItemID = Integer.toString(Utilities.getRandomNoFromRange(100, 2000));
		String itemAmount2 = Integer.toString(Utilities.getRandomNoFromRange(5, 20));
		String QCname2 = "Automation_OMM_QC2_" + modifireLineItemID + "_" + CreateDateTime.getTimeDateString();
		String percentageOff2 = "100";
		String redeemableName2 = "Auto_OMM_Redeemable2_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();

		String subAmount = Integer.toString(Utilities.getRandomNoFromRange(100, 150));

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", itemAmount1, "M", "10", "999", "1",
				lineItemID1);

		parentMap.put("Pizza1", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Sauce1", "1", modifireItem1Amount, "M", "10", "999",
				"1.4", modifireLineItemID);

		parentMap.put("Sauce1", detailsMap2);

		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("Pizza2", "1", itemAmount2, "M", "10", "999", "2",
				lineItemID1);

		parentMap.put("Pizza2", detailsMap3);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Creating Redeemable1 with QC1 and Line item filter1
		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");
		pageObj.lineItemSelectorPage().createLineItemFilterIfNotExist(lineItemFilterName1, lineItemID1, filterSetName);

		Thread.sleep(5000);
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().createQCIfNotExist(QCname1, "", "Sum of Amounts", "", true,
				lineItemFilterName1, "Maximum Unit Price", percentageOff1, true, true, enableMenuItemAggregatorFlag,
				"Item Id", "");

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().clickOnRedeemableTab("$ Or % Off");

		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(redeemableName1, "Existing Qualifier",
				QCname1, "");
		Thread.sleep(10000);
		String redeemableID1 = pageObj.redeemablePage().getRedeemableID(redeemableName1);
		System.out.println("redeemableID1=" + redeemableID1);

		enableMenuItemAggregatorFlag = false;
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().createQCIfNotExist(QCname2, "", "Sum of Amounts", "", true,
				lineItemFilterName1, "Maximum Unit Price", percentageOff2, true, true, enableMenuItemAggregatorFlag,
				"Item Id", "");
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().clickOnRedeemableTab("$ Or % Off");

		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(redeemableName2, "Existing Qualifier",
				QCname2, "");

		Thread.sleep(10000);
		String redeemableID2 = pageObj.redeemablePage().getRedeemableID(redeemableName2);
		System.out.println("redeemableID2=" + redeemableID2);

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
				redeemableID1, "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID2, "", "");
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
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationKey"), userID, subAmount, parentMap);

		System.out.println(
				"batchRedemptionProcessResponseUser1==" + batchRedemptionProcessResponseUser1.asPrettyString());

		String actualdiscount_amount1 = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", "");

		String qualifiedRItemAmount1 = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].qualified_items[1].amount").replace("[", "").replace("]", "").replace("-", "");

		String qualifiedMOdifireRItemAmount = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].qualified_items[2].amount").replace("[", "").replace("]", "").replace("-", "");

		String redemption2DiscountedAmount = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[1].discount_amount").replace("[", "").replace("]", "");

		double itemAmountDouble1 = (double) Double.parseDouble(itemAmount1);
		double modifireAmountDouble1 = (double) Double.parseDouble(modifireItem1Amount);
		double itemAmountDouble2 = (double) Double.parseDouble(itemAmount2);
		int percentageOff1Int = Integer.parseInt(percentageOff1);
		int percentageOff2Int = Integer.parseInt(percentageOff2);

		double expectedRedempetionAmount1 = (double) ((itemAmountDouble1 + modifireAmountDouble1 + itemAmountDouble2)
				* percentageOff1Int) / 100;
		expectedRedempetionAmount1 = expectedRedempetionAmount1 / 2;

		expectedRedempetionAmount1 = Double.parseDouble(decformat.format(expectedRedempetionAmount1));

		double actualdiscount_amount1Double = Double.parseDouble(actualdiscount_amount1);
		Assert.assertEquals(actualdiscount_amount1Double, expectedRedempetionAmount1);

		pageObj.utils().logPass("Verified Redemption 1 actual discount amount " + actualdiscount_amount1Double
				+ " with the expected discount amount " + expectedRedempetionAmount1);

		double qualifiedRItemAmount1_Double = Double.parseDouble(qualifiedRItemAmount1);

		double modifireRItemAmount = Double.parseDouble(qualifiedMOdifireRItemAmount);

		double itemAmountDouble1New = itemAmountDouble1 - qualifiedRItemAmount1_Double;
		double modifireAmountDouble1New = modifireAmountDouble1 - modifireRItemAmount;

		double redemption2DiscountedAmountActual = Double.parseDouble(redemption2DiscountedAmount);

		double expectedRedempetionAmount2 = (double) (itemAmountDouble1New + modifireAmountDouble1New)
				* percentageOff2Int / 100;

		expectedRedempetionAmount2 = Double.parseDouble(decformat.format(expectedRedempetionAmount2));
		Assert.assertEquals(redemption2DiscountedAmountActual, expectedRedempetionAmount2);

		pageObj.utils().logPass("Verified Redemption 2 actual discount amount " + redemption2DiscountedAmountActual
				+ " with the expected discount amount " + expectedRedempetionAmount2);

	}

	@Test
	public void validateStep17_TC77() throws InterruptedException {

		DecimalFormat decformat = new DecimalFormat("0.00");

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();

		String flatDiscount = Integer.toString(Utilities.getRandomNoFromRange(1, 10));
		double flatDiscountAmt = Double.parseDouble(flatDiscount);
		String redeemableName1 = "Auto_OMM_Redeemable1_FlatDiscount" + flatDiscountAmt + "_"
				+ CreateDateTime.getTimeDateString();

		String lineItemID2 = Integer.toString(Utilities.getRandomNoFromRange(100, 2000));
		String lineItemFilterName2 = "Automation_LF_ItemID2_" + lineItemID2;

		String QCname2 = "Automation_OMM_QC2_" + lineItemID2 + "_" + CreateDateTime.getTimeDateString();
		String percentageOff2 = Integer.toString(Utilities.getRandomNoFromRange(5, 15));
		String redeemableName2 = "Auto_OMM_Redeemable2_" + lineItemID2 + "_" + CreateDateTime.getTimeDateString();

		Thread.sleep(2000);

		String itemAmount1 = Integer.toString(Utilities.getRandomNoFromRange(50, 100));
		String itemAmount2 = Integer.toString(Utilities.getRandomNoFromRange(10, 50));

		String subAmount = Integer.toString(Utilities.getRandomNoFromRange(100, 150));
		String lineItemID3 = Integer.toString(Utilities.getRandomNoFromRange(4000, 5000));

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", itemAmount1, "M", "10", "999", "1",
				lineItemID2);

		parentMap.put("Pizza1", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Pizza2", "1", itemAmount2, "M", "10", "999", "2",
				lineItemID3);

		parentMap.put("Pizza2", detailsMap2);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		Thread.sleep(7000);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().clickOnRedeemableTab("$ Or % Off");

		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(redeemableName1, "Flat Discount", "",
				flatDiscount);

		Thread.sleep(10000);
		String redeemableID1 = pageObj.redeemablePage().getRedeemableID(redeemableName1);
		System.out.println("redeemableID1=" + redeemableID1);

		Thread.sleep(10000);
		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");

		// Creating Redeemable1 with QC1 and Line item filter1
		// pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");
		pageObj.lineItemSelectorPage().createLineItemFilterIfNotExist(lineItemFilterName2, lineItemID2, filterSetName);
		Thread.sleep(5000);
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().createQCIfNotExist(QCname2, "", "Sum of Amounts", "", true,
				lineItemFilterName2, "Maximum Unit Price", percentageOff2, true, true, enableMenuItemAggregatorFlag, "",
				"");

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().clickOnRedeemableTab("$ Or % Off");

		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(redeemableName2, "Existing Qualifier",
				QCname2, "");

		Thread.sleep(10000);
		String redeemableID2 = pageObj.redeemablePage().getRedeemableID(redeemableName2);
		System.out.println("redeemableID2=" + redeemableID2);

		// ******************************************************

		Thread.sleep(5000);

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
				redeemableID1, "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID2, "", "");
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
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationKey"), userID, subAmount, parentMap);

		System.out.println(
				"batchRedemptionProcessResponseUser1==" + batchRedemptionProcessResponseUser1.asPrettyString());

		double actualRedeemption1DiscountAmt1 = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));
		System.out.println("actualRedeemption1DiscountAmt1=" + actualRedeemption1DiscountAmt1);

		Assert.assertEquals(actualRedeemption1DiscountAmt1, flatDiscountAmt);

		double actualQualifiedItem_1_discountAmpunt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].qualified_items[0].amount").replace("[", "").replace("]", "").replace("-", ""));

		double actualRedeemption1DiscountAmt2 = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[1].discount_amount").replace("[", "").replace("]", ""));

		int itemAmount1_dbl = Integer.parseInt(itemAmount1);
		int percentageOff_int = Integer.parseInt(percentageOff2);
		double expRedeemptionDiscountAmount2_val = itemAmount1_dbl - actualQualifiedItem_1_discountAmpunt;

		double expRedeemptionDiscountAmount2 = (double) percentageOff_int * expRedeemptionDiscountAmount2_val / 100;

		expRedeemptionDiscountAmount2 = Double.parseDouble(decformat.format(expRedeemptionDiscountAmount2));

		Assert.assertEquals(actualRedeemption1DiscountAmt2, expRedeemptionDiscountAmount2);

		pageObj.utils().logPass("Redeemption2 actual amount is verified = " + actualRedeemption1DiscountAmt2
				+ " with the expected amount =" + expRedeemptionDiscountAmount2);

	}

	@Test
	public void validateStep12_TC77() throws InterruptedException {
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();

		String flatDiscount = Integer.toString(Utilities.getRandomNoFromRange(1, 20));
		double flatDiscountAmt = Double.parseDouble(flatDiscount);
		String redeemableName1 = "Auto_OMM_Redeemable1_FlatDiscount" + flatDiscountAmt + "_"
				+ CreateDateTime.getTimeDateString();

		String flatDiscount2 = Integer.toString(Utilities.getRandomNoFromRange(1, 20));
		double flatDiscountAmt2 = Double.parseDouble(flatDiscount2);
		String redeemableName2 = "Auto_OMM_Redeemable2_FlatDiscount" + flatDiscountAmt2 + "_"
				+ CreateDateTime.getTimeDateString();

		String itemAmount1 = Integer.toString(Utilities.getRandomNoFromRange(50, 100));
		String itemAmount2 = Integer.toString(Utilities.getRandomNoFromRange(50, 100));

		String lineItemID1 = Integer.toString(Utilities.getRandomNoFromRange(100, 2000));
		String lineItemID2 = Integer.toString(Utilities.getRandomNoFromRange(100, 2000));

		String subAmount = Integer.toString(Utilities.getRandomNoFromRange(100, 150));

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", itemAmount1, "M", "10", "999", "1",
				lineItemID1);

		parentMap.put("Pizza1", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Pizza2", "1", itemAmount2, "M", "10", "999", "2",
				lineItemID2);

		parentMap.put("Pizza2", detailsMap2);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().clickOnRedeemableTab("$ Or % Off");

		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(redeemableName1, "Flat Discount", "",
				flatDiscount);

		Thread.sleep(10000);
		String redeemableID1 = pageObj.redeemablePage().getRedeemableID(redeemableName1);
		System.out.println("redeemableID1=" + redeemableID1);

		Thread.sleep(10000);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().clickOnRedeemableTab("$ Or % Off");

		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(redeemableName2, "Flat Discount", "",
				flatDiscount2);

		Thread.sleep(10000);
		String redeemableID2 = pageObj.redeemablePage().getRedeemableID(redeemableName2);
		System.out.println("redeemableID2=" + redeemableID2);

		Thread.sleep(10000);

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
				redeemableID1, "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID2, "", "");
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
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationKey"), userID, subAmount, parentMap);

		System.out.println(
				"batchRedemptionProcessResponseUser1==" + batchRedemptionProcessResponseUser1.asPrettyString());

		double actualRedeemption1DiscountAmt1 = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption1DiscountAmt1, flatDiscountAmt);

		double actualRedeemption1DiscountAmt2 = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[1].discount_amount").replace("[", "").replace("]", ""));
		Assert.assertEquals(actualRedeemption1DiscountAmt2, flatDiscountAmt2);

		pageObj.utils().logPass("Verified the flat discount is applied for redemption1 & redemption2");

	}

	@Test(description = "OMM-T77 STEP -8")
	public void validateStep9_TC77() throws InterruptedException {
		DecimalFormat decformat = new DecimalFormat("0.00");

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();

		String flatDiscount = Integer.toString(Utilities.getRandomNoFromRange(1, 10));
		double flatDiscountAmt = Double.parseDouble(flatDiscount);
		String redeemableName1 = "Auto_OMM_Redeemable1_FlatDiscount" + flatDiscountAmt + "_"
				+ CreateDateTime.getTimeDateString();

		String lineItemID2 = Integer.toString(Utilities.getRandomNoFromRange(100, 2000));
		String lineItemFilterName2 = "Automation_LF_ItemID2_" + lineItemID2;

		String QCname2 = "Automation_OMM_QC2_" + lineItemID2 + "_" + CreateDateTime.getTimeDateString();
		String percentageOff2 = Integer.toString(Utilities.getRandomNoFromRange(5, 15));
		String redeemableName2 = "Auto_OMM_Redeemable2_" + lineItemID2 + "_" + CreateDateTime.getTimeDateString();

		Thread.sleep(2000);

		String itemAmount1 = Integer.toString(Utilities.getRandomNoFromRange(50, 100));
		String itemAmount2 = Integer.toString(Utilities.getRandomNoFromRange(10, 50));

		String subAmount = Integer.toString(Utilities.getRandomNoFromRange(100, 150));
		String lineItemID3 = Integer.toString(Utilities.getRandomNoFromRange(4000, 5000));

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", itemAmount1, "M", "10", "999", "1",
				lineItemID2);

		parentMap.put("Pizza1", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Pizza2", "1", itemAmount2, "M", "10", "999", "2",
				lineItemID3);

		parentMap.put("Pizza2", detailsMap2);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().clickOnRedeemableTab("$ Or % Off");

		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(redeemableName1, "Flat Discount", "",
				flatDiscount);

		Thread.sleep(10000);
		String redeemableID1 = pageObj.redeemablePage().getRedeemableID(redeemableName1);
		System.out.println("redeemableID1=" + redeemableID1);

		Thread.sleep(10000);
		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");

		// Creating Redeemable1 with QC1 and Line item filter1
		// pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");
		pageObj.lineItemSelectorPage().createLineItemFilterIfNotExist(lineItemFilterName2, lineItemID2, filterSetName);
		Thread.sleep(5000);
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().createQCIfNotExist(QCname2, "", "Sum of Amounts", "", true,
				lineItemFilterName2, "Maximum Unit Price", percentageOff2, false, true, enableMenuItemAggregatorFlag,
				"", "");

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().clickOnRedeemableTab("$ Or % Off");

		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(redeemableName2, "Existing Qualifier",
				QCname2, "");

		Thread.sleep(10000);
		String redeemableID2 = pageObj.redeemablePage().getRedeemableID(redeemableName2);
		System.out.println("redeemableID2=" + redeemableID2);

		// ******************************************************

		Thread.sleep(5000);

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
				redeemableID1, "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID2, "", "");
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
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationKey"), userID, subAmount, parentMap);

		System.out.println(
				"batchRedemptionProcessResponseUser1==" + batchRedemptionProcessResponseUser1.asPrettyString());

		double actualRedeemption1DiscountAmt1 = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));
		System.out.println("actualRedeemption1DiscountAmt1=" + actualRedeemption1DiscountAmt1);

		Assert.assertEquals(actualRedeemption1DiscountAmt1, flatDiscountAmt);

		double actualQualifiedItem_1_discountAmpunt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].qualified_items[0].amount").replace("[", "").replace("]", "").replace("-", ""));

		double actualRedeemption1DiscountAmt2 = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[1].discount_amount").replace("[", "").replace("]", ""));

		int itemAmount1_dbl = Integer.parseInt(itemAmount1);
		int percentageOff_int = Integer.parseInt(percentageOff2);
		double expRedeemptionDiscountAmount2_val = itemAmount1_dbl - actualQualifiedItem_1_discountAmpunt;

		double expRedeemptionDiscountAmount2 = (double) percentageOff_int * expRedeemptionDiscountAmount2_val / 100;

		expRedeemptionDiscountAmount2 = Double.parseDouble(decformat.format(expRedeemptionDiscountAmount2));

		Assert.assertEquals(actualRedeemption1DiscountAmt2, expRedeemptionDiscountAmount2);

		pageObj.utils().logPass("Redeemption2 actual amount is verified = " + actualRedeemption1DiscountAmt2
				+ " with the expected amount =" + expRedeemptionDiscountAmount2);

	}

	@Test(description = "OMM-T77 STEP -4")
	public void validateStep3_TC77() throws InterruptedException {

		DecimalFormat decformat = new DecimalFormat("0.00");

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();

		String lineItemID1 = Integer.toString(Utilities.getRandomNoFromRange(100, 2000));
		String lineItemFilterName1 = "Automation_LF_ItemID2_" + lineItemID1;

		String QCname1 = "Automation_OMM_QC1_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();
		String percentageOff1 = Integer.toString(Utilities.getRandomNoFromRange(5, 15));
		String redeemableName1 = "Auto_OMM_Redeemable1_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();

		String lineItemID2 = Integer.toString(Utilities.getRandomNoFromRange(2000, 4000));

		String itemAmount1 = Integer.toString(Utilities.getRandomNoFromRange(50, 100));
		String itemAmount2 = Integer.toString(Utilities.getRandomNoFromRange(10, 50));

		String subAmount = Integer.toString(Utilities.getRandomNoFromRange(100, 150));

		detailsMap1.put("item_name", "Pizza1");
		detailsMap1.put("item_qty", "2");
		detailsMap1.put("amount", itemAmount1);
		detailsMap1.put("item_type", "M");
		detailsMap1.put("item_family", "10");
		detailsMap1.put("item_group", "999");
		detailsMap1.put("serial_number", "1");
		detailsMap1.put("item_id", lineItemID1);

		parentMap.put("Pizza1", detailsMap1);

		detailsMap2.put("item_name", "Coffee");
		detailsMap2.put("item_qty", "1");
		detailsMap2.put("amount", itemAmount2);
		detailsMap2.put("item_type", "M");
		detailsMap2.put("item_family", "10");
		detailsMap2.put("item_group", "999");
		detailsMap2.put("serial_number", "2");
		detailsMap2.put("item_id", lineItemID2);

		parentMap.put("Coffee", detailsMap2);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Creating Redeemable1 with QC1 and Line item filter1
		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");
		pageObj.lineItemSelectorPage().createLineItemFilterIfNotExist(lineItemFilterName1, lineItemID1, filterSetName);

		Thread.sleep(5000);
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().createQCIfNotExist(QCname1, "", "Sum of Amounts", "", true,
				lineItemFilterName1, "Maximum Unit Price", percentageOff1, false, false, enableMenuItemAggregatorFlag,
				"", "");

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().clickOnRedeemableTab("$ Or % Off");

		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(redeemableName1, "Existing Qualifier",
				QCname1, "");

		Thread.sleep(10000);
		String redeemableID1 = pageObj.redeemablePage().getRedeemableID(redeemableName1);
		System.out.println("redeemableID1=" + redeemableID1);

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
				redeemableID1, "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID1, "", "");
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
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationKey"), userID, subAmount, parentMap);

		System.out.println(
				"batchRedemptionProcessResponseUser1==" + batchRedemptionProcessResponseUser1.asPrettyString());

		double actualRedeemption1DiscountAmt1 = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));

		String actualErrorMessageRedemption2 = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("failures[0].message").replace("[", "").replace("]", "");

		int percentageOffInt1 = Integer.parseInt(percentageOff1);
		double itemAmountInt1 = Double.parseDouble(itemAmount1) / 2;

		double expItemDiscountAmount1 = (double) percentageOffInt1 * itemAmountInt1 / 100;

		expItemDiscountAmount1 = Double.parseDouble(decformat.format(expItemDiscountAmount1));

		Assert.assertEquals(actualRedeemption1DiscountAmt1, expItemDiscountAmount1);

		pageObj.utils().logPass(expItemDiscountAmount1 + " amount is verified for the redeemption1 ");

		boolean verifyStatus = pageObj.redeemablePage().verifyErrorMessage(actualErrorMessageRedemption2,
				dataSet.get("errorMessage"));
		Assert.assertTrue(verifyStatus);
		pageObj.utils().logPass("Verified the error message for redemption2");

	}

	@Test
	public void validateStep32_TC77() throws InterruptedException {
		DecimalFormat decformat = new DecimalFormat("0.00");

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();

		String lineItemID1 = dataSet.get("lineItemID1");

		String percentageOff1 = dataSet.get("qcOnePercentage");

		String itemAmount1 = Integer.toString(Utilities.getRandomNoFromRange(50, 100));
		String itemAmount2 = Integer.toString(Utilities.getRandomNoFromRange(10, 50));
		String itemAmount3 = Integer.toString(Utilities.getRandomNoFromRange(10, 50));

		String percentageOff2 = dataSet.get("qcTwoPercentage");

		String lineItemID3 = Integer.toString(Utilities.getRandomNoFromRange(100, 2000));

		String subAmount = Integer.toString(Utilities.getRandomNoFromRange(100, 150));

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", itemAmount1, "M", "10", "999", "1",
				lineItemID1);

		parentMap.put("Pizza1", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Pizza2", "1", itemAmount2, "M", "10", "999", "2",
				lineItemID1);

		parentMap.put("Pizza2", detailsMap2);

		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("Coffee", "1", itemAmount3, "M", "10", "999", "3",
				lineItemID3);

		parentMap.put("Coffee", detailsMap3);

		String redeemableID1 = dataSet.get("firstRedeemableID");
		System.out.println("redeemableID1=" + redeemableID1);

		String redeemableID2 = dataSet.get("secondRedeemableID");
		System.out.println("redeemableID2=" + redeemableID2);

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
				redeemableID1, "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID2, "", "");
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

		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);

		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationKey"), userID, subAmount, parentMap);

		System.out.println(
				"batchRedemptionProcessResponseUser1==" + batchRedemptionProcessResponseUser1.asPrettyString());

		String failureMessage1 = batchRedemptionProcessResponseUser1.jsonPath().getString("failures[0].message")
				.replace("[", "").replace("]", "");

		boolean verifyStatus = pageObj.redeemablePage().verifyErrorMessage(failureMessage1,
				dataSet.get("errorMessage"));
		Assert.assertTrue(verifyStatus);

		double actualDiscountAmount2 = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));

		double itemOneAmountDouble = Double.parseDouble(itemAmount1);
		double percentageOff2Double = Double.parseDouble(percentageOff2);

		double expDiscountAmount2 = (itemOneAmountDouble * percentageOff2Double) / 100;

		expDiscountAmount2 = Double.parseDouble(decformat.format(expDiscountAmount2));

		Assert.assertEquals(actualDiscountAmount2, expDiscountAmount2);

	}

	@Test
	public void validateStep38_TC77() throws InterruptedException {
		DecimalFormat decformat = new DecimalFormat("0.00");
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();

		String lineItemID1 = Integer.toString(Utilities.getRandomNoFromRange(100, 2000));
		String lineItemFilterName1 = "Automation_LF_ItemID2_" + lineItemID1;

		String QCname1 = "Automation_OMM_QC1_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();
		String percentageOff1 = Integer.toString(Utilities.getRandomNoFromRange(5, 15));
		String redeemableName1 = "Auto_OMM_Redeemable1_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();

		String itemAmount1 = Integer.toString(Utilities.getRandomNoFromRange(1, 10));
		String itemAmount2 = Integer.toString(Utilities.getRandomNoFromRange(50, 100));
		String itemAmount3 = Integer.toString(Utilities.getRandomNoFromRange(10, 50));

		String lineItemID3 = Integer.toString(Utilities.getRandomNoFromRange(100, 2000));

		String subAmount = Integer.toString(Utilities.getRandomNoFromRange(100, 150));

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", itemAmount1, "D", "10", "999", "1",
				lineItemID1);

		parentMap.put("Pizza1", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Pizza2", "1", itemAmount2, "M", "10", "999", "2",
				lineItemID1);

		parentMap.put("Pizza2", detailsMap2);

		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("Coffee", "1", itemAmount3, "M", "10", "999", "3",
				lineItemID3);

		parentMap.put("Pizza3", detailsMap3);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Creating Redeemable1 with QC1 and Line item filter1
		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");
		pageObj.lineItemSelectorPage().createLineItemFilterIfNotExist(lineItemFilterName1, lineItemID1, filterSetName);

		Thread.sleep(5000);
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().createQCIfNotExist(QCname1, "", "Sum of Amounts", "", true,
				lineItemFilterName1, "Maximum Unit Price", percentageOff1, true, true, enableMenuItemAggregatorFlag,
				"Item Id", location2);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().clickOnRedeemableTab("$ Or % Off");

		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(redeemableName1, "Existing Qualifier",
				QCname1, "");

		String redeemableID1 = pageObj.redeemablePage().getRedeemableID(redeemableName1);
		System.out.println("redeemableID1=" + redeemableName1);

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
				redeemableID1, "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID1, "", "");
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

		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);

		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationKey"), userID, subAmount, parentMap);

		System.out.println(
				"batchRedemptionProcessResponseUser1==" + batchRedemptionProcessResponseUser1.asPrettyString());

		double actualRedeemption1DiscountAmt1 = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));

		int percentageOffInt1 = Integer.parseInt(percentageOff1);
		int totalAmountOfReciept = Integer.parseInt(itemAmount2);

		double expItemDiscountAmount1 = (double) (percentageOffInt1 * totalAmountOfReciept) / 100;
		Assert.assertEquals(actualRedeemption1DiscountAmt1, expItemDiscountAmount1);

		pageObj.utils().logPass(expItemDiscountAmount1 + " amount is verified for the redeemption1 ");

		double totalAmountOfReciept2 = totalAmountOfReciept - expItemDiscountAmount1;

		double expItemDiscountAmount2 = (double) (percentageOffInt1 * totalAmountOfReciept2) / 100;
		expItemDiscountAmount2 = Double.parseDouble(decformat.format(expItemDiscountAmount2));
		double actualRedeemption1DiscountAmt2 = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[1].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption1DiscountAmt2, expItemDiscountAmount2);

		pageObj.utils().logPass(expItemDiscountAmount2 + " amount is verified for the redeemption2 ");
	}

	@Test
	public void validateStep37_TC77() throws InterruptedException {

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();

		String lineItemID1 = Integer.toString(Utilities.getRandomNoFromRange(100, 2000));
		String lineItemFilterName1 = "Automation_LF_ItemID2_" + lineItemID1;

		String QCname1 = "Automation_OMM_QC1_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();
		String percentageOff1 = Integer.toString(Utilities.getRandomNoFromRange(5, 15));
		String redeemableName1 = "Auto_OMM_Redeemable1_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();

		String itemAmount1 = Integer.toString(Utilities.getRandomNoFromRange(50, 100));
		String itemAmount2 = Integer.toString(Utilities.getRandomNoFromRange(10, 50));
		String itemAmount3 = Integer.toString(Utilities.getRandomNoFromRange(10, 50));

		String lineItemID3 = Integer.toString(Utilities.getRandomNoFromRange(100, 2000));

		String subAmount = Integer.toString(Utilities.getRandomNoFromRange(100, 150));

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", itemAmount1, "D", "10", "999", "1",
				lineItemID1);

		parentMap.put("Pizza1", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Pizza2", "1", itemAmount2, "D", "10", "999", "2",
				lineItemID1);

		parentMap.put("Pizza2", detailsMap2);

		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("Coffee", "1", itemAmount3, "M", "10", "999", "3",
				lineItemID3);

		parentMap.put("Pizza3", detailsMap3);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Creating Redeemable1 with QC1 and Line item filter1
		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");
		pageObj.lineItemSelectorPage().createLineItemFilterIfNotExist(lineItemFilterName1, lineItemID1, filterSetName);

		Thread.sleep(5000);
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().createQCIfNotExist(QCname1, "", "Sum of Amounts", "", true,
				lineItemFilterName1, "Maximum Unit Price", percentageOff1, true, true, enableMenuItemAggregatorFlag,
				"Item Id", location2);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().clickOnRedeemableTab("$ Or % Off");

		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(redeemableName1, "Existing Qualifier",
				QCname1, "");

		String redeemableID1 = pageObj.redeemablePage().getRedeemableID(redeemableName1);
		System.out.println("redeemableID1=" + redeemableName1);

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
				redeemableID1, "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID1, "", "");
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

		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);

		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationKey"), userID, subAmount, parentMap);

		System.out.println(
				"batchRedemptionProcessResponseUser1==" + batchRedemptionProcessResponseUser1.asPrettyString());

		String actualErrorMessageRep1 = batchRedemptionProcessResponseUser1.jsonPath().getString("failures[0].message")
				.replace("[", "").replace("]", "");

		Assert.assertEquals(actualErrorMessageRep1, dataSet.get("errorMessage"));

		pageObj.utils().logPass("Verified the error message for redemption1");

		String actualErrorMessageRep2 = batchRedemptionProcessResponseUser1.jsonPath().getString("failures[1].message")
				.replace("[", "").replace("]", "");

		Assert.assertEquals(actualErrorMessageRep2, dataSet.get("errorMessage"));

		pageObj.utils().logPass("Verified the error message for redemption1");

	}

	@Test
	public void validateStep34_TC77() throws InterruptedException {

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();

		String lineItemID1 = Integer.toString(Utilities.getRandomNoFromRange(100, 2000));
		String lineItemFilterName1 = "Automation_LF_ItemID2_" + lineItemID1;

		String QCname1 = "Automation_OMM_QC1_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();
		String percentageOff1 = Integer.toString(Utilities.getRandomNoFromRange(5, 15));
		String redeemableName1 = "Auto_OMM_Redeemable1_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();

		String itemAmount1 = Integer.toString(Utilities.getRandomNoFromRange(50, 100));
		String itemAmount2 = Integer.toString(Utilities.getRandomNoFromRange(10, 50));
		String itemAmount3 = Integer.toString(Utilities.getRandomNoFromRange(10, 50));

		String percentageOff2 = "100";
		String redeemableName2 = "Auto_OMM_Redeemable2_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();
		String QCname2 = "Automation_OMM_QC2_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();

		String lineItemID3 = Integer.toString(Utilities.getRandomNoFromRange(100, 2000));

		String subAmount = Integer.toString(Utilities.getRandomNoFromRange(100, 150));

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", itemAmount1, "M", "10", "999", "1",
				lineItemID1);

		parentMap.put("Pizza1", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Pizza2", "1", itemAmount2, "M", "10", "999", "2",
				lineItemID1);

		parentMap.put("Pizza2", detailsMap2);

		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("Coffee", "1", itemAmount3, "M", "10", "999", "3",
				lineItemID3);

		parentMap.put("Coffee", detailsMap3);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Creating Redeemable1 with QC1 and Line item filter1
		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");
		pageObj.lineItemSelectorPage().createLineItemFilterIfNotExist(lineItemFilterName1, lineItemID1, filterSetName);

		Thread.sleep(5000);
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().createQCIfNotExist(QCname1, "", "Sum of Amounts", "", true,
				lineItemFilterName1, "Exclude", percentageOff1, true, true, enableMenuItemAggregatorFlag, "Item Id",
				location2);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().clickOnRedeemableTab("$ Or % Off");

		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(redeemableName1, "Existing Qualifier",
				QCname1, "");

		String redeemableID1 = pageObj.redeemablePage().getRedeemableID(redeemableName1);
		System.out.println("redeemableID1=" + redeemableName1);

		Thread.sleep(10000);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().createQCIfNotExist(QCname2, "", "Sum of Amounts", "", true,
				lineItemFilterName1, "Maximum Unit Price", percentageOff2, true, true, enableMenuItemAggregatorFlag,
				"Item Id", location2);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().clickOnRedeemableTab("$ Or % Off");

		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(redeemableName2, "Existing Qualifier",
				QCname2, "");

		String redeemableID2 = pageObj.redeemablePage().getRedeemableID(redeemableName2);
		System.out.println("redeemableID2=" + redeemableName2);

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
				redeemableID1, "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID2, "", "");
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

		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);

		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationKey"), userID, subAmount, parentMap);

		System.out.println(
				"batchRedemptionProcessResponseUser1==" + batchRedemptionProcessResponseUser1.asPrettyString());

		double actualRedeemption1DiscountAmt1 = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));

		String actualQualifiedItemName1 = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].qualified_items[0].item_name").replace("[", "").replace("]", "");

		double actualRedeemption1DiscountAmt2 = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[1].discount_amount").replace("[", "").replace("]", ""));

		int percentageOffInt1 = Integer.parseInt(percentageOff1);
		int totalAmountOfReciept = Integer.parseInt(itemAmount3);

		double expItemDiscountAmount1 = (double) (percentageOffInt1 * totalAmountOfReciept) / 100;
		Assert.assertEquals(actualRedeemption1DiscountAmt1, expItemDiscountAmount1);

		pageObj.utils().logPass(expItemDiscountAmount1 + " amount is verified for the redeemption1 ");

		Assert.assertEquals(actualQualifiedItemName1, "Coffee");
		pageObj.utils().logPass(actualQualifiedItemName1 + " qualified item name  is verified for the redeemption1 ");

		double expTotalDiscountRedemption2 = (double) Double.parseDouble(itemAmount1);

		Assert.assertEquals(actualRedeemption1DiscountAmt2, expTotalDiscountRedemption2);
		pageObj.utils().logPass(expTotalDiscountRedemption2 + " amount is verified for the redeemption2 ");

	}

	@Test
	public void validateStep29_TC77() throws InterruptedException {
		enableMenuItemAggregatorFlag = true;

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();

		String lineItemID1 = Integer.toString(Utilities.getRandomNoFromRange(100, 2000));
		String lineItemFilterName1 = "Automation_LF_ItemID2_" + lineItemID1;

		String QCname1 = "Automation_OMM_QC1_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();
		String percentageOff1 = Integer.toString(Utilities.getRandomNoFromRange(5, 15));
		String redeemableName1 = "Auto_OMM_Redeemable1_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();

		String itemAmount1 = Integer.toString(Utilities.getRandomNoFromRange(50, 100));
		String itemAmount2 = Integer.toString(Utilities.getRandomNoFromRange(10, 50));
		String itemAmount3 = Integer.toString(Utilities.getRandomNoFromRange(10, 50));

		String percentageOff2 = "100";
		String redeemableName2 = "Auto_OMM_Redeemable2_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();
		String QCname2 = "Automation_OMM_QC2_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();

		String lineItemID3 = Integer.toString(Utilities.getRandomNoFromRange(100, 2000));

		String subAmount = Integer.toString(Utilities.getRandomNoFromRange(100, 150));

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", itemAmount1, "M", "10", "999", "1",
				lineItemID1);

		parentMap.put("Pizza1", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Pizza2", "1", itemAmount2, "M", "10", "999", "2",
				lineItemID1);

		parentMap.put("Pizza2", detailsMap2);

		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("Pizza3", "1", itemAmount3, "M", "10", "999", "3",
				lineItemID3);

		parentMap.put("Pizza3", detailsMap3);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Creating Redeemable1 with QC1 and Line item filter1
		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");
		pageObj.lineItemSelectorPage().createLineItemFilterIfNotExist(lineItemFilterName1, lineItemID1, filterSetName);

		Thread.sleep(5000);
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().createQCIfNotExist(QCname1, "", "Sum of Amounts", "", true,
				lineItemFilterName1, "", percentageOff1, true, true, enableMenuItemAggregatorFlag, "Item Id",
				location2);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().clickOnRedeemableTab("$ Or % Off");

		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(redeemableName1, "Existing Qualifier",
				QCname1, "");

		String redeemableID1 = pageObj.redeemablePage().getRedeemableID(redeemableName1);
		System.out.println("redeemableID1=" + redeemableName1);

		Thread.sleep(10000);

		enableMenuItemAggregatorFlag = false;
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().createQCIfNotExist(QCname2, "", "Sum of Amounts", "", true,
				lineItemFilterName1, "Maximum Unit Price", percentageOff2, true, true, enableMenuItemAggregatorFlag,
				"Item Id", location2);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().clickOnRedeemableTab("$ Or % Off");

		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(redeemableName2, "Existing Qualifier",
				QCname2, "");

		String redeemableID2 = pageObj.redeemablePage().getRedeemableID(redeemableName2);
		System.out.println("redeemableID2=" + redeemableName2);

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
				redeemableID1, "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID2, "", "");
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

		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);

		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationKey"), userID, subAmount, parentMap);

		System.out.println(
				"batchRedemptionProcessResponseUser1==" + batchRedemptionProcessResponseUser1.asPrettyString());

		double actualRedeemption1DiscountAmt1 = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));

		double actualRedeemption1DiscountAmt2 = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[1].discount_amount").replace("[", "").replace("]", ""));

		int percentageOffInt1 = Integer.parseInt(percentageOff1);
		int itemAmountInt1 = Integer.parseInt(itemAmount1);
		int itemAmountInt2 = Integer.parseInt(itemAmount2);

		int totalAmountOfReciept = itemAmountInt1 + itemAmountInt2;

		double expItemDiscountAmount1 = (double) (percentageOffInt1 * totalAmountOfReciept) / 100;
		Assert.assertEquals(actualRedeemption1DiscountAmt1, expItemDiscountAmount1);

		pageObj.utils().logPass(expItemDiscountAmount1 + " amount is verified for the redeemption1 ");

		double actualRedeemption1_QualifiedItem1 = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].qualified_items[1].amount").replace("[", "").replace("]", "").replace("-", ""));

		double expTotalDiscountRedemption2 = (double) (itemAmountInt1 - actualRedeemption1_QualifiedItem1);

		Assert.assertEquals(actualRedeemption1DiscountAmt2, expTotalDiscountRedemption2);
		pageObj.utils().logPass(expTotalDiscountRedemption2 + " amount is verified for the redeemption2 ");
	}

	@Test
	public void validateStep23_TC77() throws InterruptedException {
		enableMenuItemAggregatorFlag = true;

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();

		String lineItemID1 = Integer.toString(Utilities.getRandomNoFromRange(100, 2000));
		String lineItemFilterName1 = "Automation_LF_ItemID2_" + lineItemID1;

		String QCname1 = "Automation_OMM_QC1_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();
		String percentageOff1 = Integer.toString(Utilities.getRandomNoFromRange(5, 15));
		String redeemableName1 = "Auto_OMM_Redeemable1_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();

		String itemAmount1 = Integer.toString(Utilities.getRandomNoFromRange(50, 100));
		String itemAmount2 = Integer.toString(Utilities.getRandomNoFromRange(10, 50));

		String subAmount = Integer.toString(Utilities.getRandomNoFromRange(100, 150));

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", itemAmount1, "M", "10", "999", "1",
				lineItemID1);

		parentMap.put("Pizza1", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Pizza2", "1", itemAmount2, "M", "10", "999", "2",
				lineItemID1);

		parentMap.put("Pizza2", detailsMap2);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Creating Redeemable1 with QC1 and Line item filter1
		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");
		pageObj.lineItemSelectorPage().createLineItemFilterIfNotExist(lineItemFilterName1, lineItemID1, filterSetName);

		Thread.sleep(5000);
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().createQCIfNotExist(QCname1, "", "Sum of Amounts", "", true,
				lineItemFilterName1, "", percentageOff1, true, true, enableMenuItemAggregatorFlag, "Item Id",
				location2);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().clickOnRedeemableTab("$ Or % Off");

		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(redeemableName1, "Existing Qualifier",
				QCname1, "");

		Thread.sleep(10000);
		String redeemableID1 = pageObj.redeemablePage().getRedeemableID(redeemableName1);
		System.out.println("redeemableID1=" + redeemableID1);

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
				redeemableID1, "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID1 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");

		pageObj.utils().logit("Reward id " + rewardID1 + " & " + "is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID1);

		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationKey"), userID, subAmount, parentMap);

		System.out.println(
				"batchRedemptionProcessResponseUser1==" + batchRedemptionProcessResponseUser1.asPrettyString());

		double actualRedeemption1DiscountAmt1 = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));

		int percentageOffInt1 = Integer.parseInt(percentageOff1);
		int itemAmountInt1 = Integer.parseInt(itemAmount1);
		int itemAmountInt2 = Integer.parseInt(itemAmount2);

		int totalAmountOfReciept = itemAmountInt1 + itemAmountInt2;

		double expItemDiscountAmount1 = (double) (percentageOffInt1 * totalAmountOfReciept) / 100;
		Assert.assertEquals(actualRedeemption1DiscountAmt1, expItemDiscountAmount1);

		pageObj.utils().logPass(expItemDiscountAmount1 + " amount is verified for the redeemption1 ");

	}

	@Test
	public void validateStep19_TC77() throws InterruptedException {

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();

		String lineItemID1 = Integer.toString(Utilities.getRandomNoFromRange(100, 2000));
		String lineItemFilterName1 = "Automation_LF_ItemID2_" + lineItemID1;

		String QCname1 = "Automation_OMM_QC1_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();
		String percentageOff1 = "100";
		String redeemableName1 = "Auto_OMM_Redeemable1_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();

		String lineItemID2 = Integer.toString(Utilities.getRandomNoFromRange(2000, 4000));
		String lineItemFilterName2 = "Automation_LF_ItemID2_" + lineItemID2;
		Thread.sleep(2000);
		String QCname2 = "Automation_OMM_QC2_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();
		String percentageOff2 = Integer.toString(Utilities.getRandomNoFromRange(15, 20));
		String redeemableName2 = "Auto_OMM_Redeemable2_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();

		String itemAmount1 = Integer.toString(Utilities.getRandomNoFromRange(50, 100));

		String subAmount = Integer.toString(Utilities.getRandomNoFromRange(100, 150));

		detailsMap1.put("item_name", "Pizza1");
		detailsMap1.put("item_qty", "1");
		detailsMap1.put("amount", itemAmount1);
		detailsMap1.put("item_type", "M");
		detailsMap1.put("item_family", "10");
		detailsMap1.put("item_group", "999");
		detailsMap1.put("serial_number", "1");
		detailsMap1.put("item_id", lineItemID1);

		parentMap.put("Pizza1", detailsMap1);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Creating Redeemable1 with QC1 and Line item filter1
		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");
		pageObj.lineItemSelectorPage().createLineItemFilterIfNotExist(lineItemFilterName1, lineItemID1, filterSetName);

		Thread.sleep(5000);
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().createQCIfNotExist(QCname1, "", "Sum of Amounts", "", true,
				lineItemFilterName1, "Maximum Unit Price", percentageOff1, true, true, enableMenuItemAggregatorFlag, "",
				location2);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().clickOnRedeemableTab("$ Or % Off");

		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(redeemableName1, "Existing Qualifier",
				QCname1, "");

		Thread.sleep(10000);
		String redeemableID1 = pageObj.redeemablePage().getRedeemableID(redeemableName1);
		System.out.println("redeemableID1=" + redeemableID1);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");

		// pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");
		pageObj.lineItemSelectorPage().createLineItemFilterIfNotExist(lineItemFilterName2, lineItemID2, filterSetName);

		Thread.sleep(5000);
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().createQCIfNotExist(QCname2, "", "Sum of Amounts", "", true,
				lineItemFilterName2, "Maximum Unit Price", percentageOff2, true, true, enableMenuItemAggregatorFlag, "",
				location2);
		Thread.sleep(5000);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().clickOnRedeemableTab("$ Or % Off");

		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(redeemableName2, "Existing Qualifier",
				QCname2, "");

		Thread.sleep(10000);
		String redeemableID2 = pageObj.redeemablePage().getRedeemableID(redeemableName2);
		System.out.println("redeemableID2=" + redeemableID2);

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
				redeemableID1, "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID2, "", "");
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

		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationKey"), userID, subAmount, parentMap);

		System.out.println(
				"batchRedemptionProcessResponseUser1==" + batchRedemptionProcessResponseUser1.asPrettyString());

		double actualRedeemption1DiscountAmt1 = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));

		String actualErrorMessageRedemption2 = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("failures[0].message").replace("[", "").replace("]", "");

		int percentageOffInt1 = Integer.parseInt(percentageOff1);
		int itemAmountInt1 = Integer.parseInt(itemAmount1);

		double expItemDiscountAmount1 = (double) (percentageOffInt1 * itemAmountInt1) / 100;
		Assert.assertEquals(actualRedeemption1DiscountAmt1, expItemDiscountAmount1);

		pageObj.utils().logPass(expItemDiscountAmount1 + " amount is verified for the redeemption1 ");

		boolean verifyStatus = pageObj.redeemablePage().verifyErrorMessage(actualErrorMessageRedemption2,
				dataSet.get("errorMessage"));
		Assert.assertTrue(verifyStatus);
		pageObj.utils().logPass("Verified the error message for redemption2");

	}

	@Test
	public void validateStep15() throws InterruptedException {

		DecimalFormat decformat = new DecimalFormat("0.00");
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();

		String lineItemID1 = Integer.toString(Utilities.getRandomNoFromRange(100, 2000));
		String lineItemFilterName1 = "Automation_LF_ItemID2_" + lineItemID1;

		String QCname1 = "Automation_OMM_QC1_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();
		String percentageOff1 = Integer.toString(Utilities.getRandomNoFromRange(5, 15));
		String redeemableName1 = "Auto_OMM_Redeemable1_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();

		String itemAmount1 = Integer.toString(Utilities.getRandomNoFromRange(50, 100));

		String subAmount = Integer.toString(Utilities.getRandomNoFromRange(100, 150));

		detailsMap1.put("item_name", "Pizza1");
		detailsMap1.put("item_qty", "2");
		detailsMap1.put("amount", itemAmount1);
		detailsMap1.put("item_type", "M");
		detailsMap1.put("item_family", "10");
		detailsMap1.put("item_group", "999");
		detailsMap1.put("serial_number", "1");
		detailsMap1.put("item_id", lineItemID1);

		parentMap.put("Pizza1", detailsMap1);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Creating Redeemable1 with QC1 and Line item filter1
		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");
		pageObj.lineItemSelectorPage().createLineItemFilterIfNotExist(lineItemFilterName1, lineItemID1, filterSetName);

		Thread.sleep(5000);
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().createQCIfNotExist(QCname1, "", "Sum of Amounts", "", true,
				lineItemFilterName1, "Maximum Unit Price", percentageOff1, true, true, enableMenuItemAggregatorFlag, "",
				location2);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().clickOnRedeemableTab("$ Or % Off");

		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(redeemableName1, "Existing Qualifier",
				QCname1, "");

		Thread.sleep(10000);
		String redeemableID1 = pageObj.redeemablePage().getRedeemableID(redeemableName1);
		System.out.println("redeemableID1=" + redeemableID1);

		// ******************************************************

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
				redeemableID1, "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID1, "", "");
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
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationKey"), userID, subAmount, parentMap);

		System.out.println(
				"batchRedemptionProcessResponseUser1==" + batchRedemptionProcessResponseUser1.asPrettyString());

		double actualRedeemption1DiscountAmt1 = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));

		double actualRedeemption2DiscountAmt2 = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[1].discount_amount").replace("[", "").replace("]", ""));

		int percentageOffInt1 = Integer.parseInt(percentageOff1);
		int itemAmountInt1 = Integer.parseInt(itemAmount1);
		double redemption1DiscountedAmt = (double) itemAmountInt1 / 2;

		double expItemDiscountAmount1 = (double) (percentageOffInt1 * redemption1DiscountedAmt) / 100;
		double finalExpDiscountAmtRedp1 = Double.parseDouble(decformat.format(expItemDiscountAmount1));
		System.out.println("finalExpDiscountAmtRedp1==" + finalExpDiscountAmtRedp1);

		Assert.assertEquals(actualRedeemption1DiscountAmt1, finalExpDiscountAmtRedp1);

		pageObj.utils().logPass(expItemDiscountAmount1
				+ " expected discount amount is verified with Redemption1 actual discount amount "
				+ actualRedeemption1DiscountAmt1);

		double itemAmountInt2 = itemAmountInt1 - actualRedeemption1DiscountAmt1;

		double redemption2DiscountedAmt = (double) itemAmountInt2 / 2;
		double expItemDiscountAmount2 = (double) (percentageOffInt1 * redemption2DiscountedAmt) / 100;

		double finalExpDiscountAmtRedp2 = Double.parseDouble(decformat.format(expItemDiscountAmount2));
		System.out.println("finalExpDiscountAmtRedp2==" + finalExpDiscountAmtRedp2);

		Assert.assertEquals(actualRedeemption2DiscountAmt2, finalExpDiscountAmtRedp2);

		pageObj.utils().logPass(expItemDiscountAmount2
				+ " expected discount amount is verified with Redemption2 actual discount amount "
				+ actualRedeemption2DiscountAmt2);

	}

	@Test(description = "OMM-T77 STEP -13")
	public void validateStep13_TC77() throws InterruptedException {

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();

		String lineItemID1 = Integer.toString(Utilities.getRandomNoFromRange(100, 2000));

		Thread.sleep(2000);
		String flatDiscount = "0";

		double flatDiscountAmt = Double.parseDouble(flatDiscount);
		String redeemableName2 = "Auto_OMM_Redeemable2_" + flatDiscountAmt + "Amount_"
				+ CreateDateTime.getTimeDateString();

		String itemAmount1 = Integer.toString(Utilities.getRandomNoFromRange(50, 100));
		String itemAmount2 = Integer.toString(Utilities.getRandomNoFromRange(10, 50));

		String subAmount = Integer.toString(Utilities.getRandomNoFromRange(100, 150));
		String lineItemID3 = Integer.toString(Utilities.getRandomNoFromRange(4000, 5000));

		detailsMap1.put("item_name", "Pizza1");
		detailsMap1.put("item_qty", "1");
		detailsMap1.put("amount", itemAmount1);
		detailsMap1.put("item_type", "M");
		detailsMap1.put("item_family", "10");
		detailsMap1.put("item_group", "999");
		detailsMap1.put("serial_number", "1");
		detailsMap1.put("item_id", lineItemID1);

		parentMap.put("Pizza1", detailsMap1);

		detailsMap2.put("item_name", "Pizza2");
		detailsMap2.put("item_qty", "1");
		detailsMap2.put("amount", itemAmount2);
		detailsMap2.put("item_type", "M");
		detailsMap2.put("item_family", "10");
		detailsMap2.put("item_group", "999");
		detailsMap2.put("serial_number", "2");
		detailsMap2.put("item_id", lineItemID3);

		parentMap.put("Pizza2", detailsMap2);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// ******************************************************

		Thread.sleep(5000);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().clickOnRedeemableTab("$ Or % Off");

		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(redeemableName2, "Flat Discount", "",
				flatDiscount);

		Thread.sleep(10000);
		String redeemableID2 = pageObj.redeemablePage().getRedeemableID(redeemableName2);
		System.out.println("redeemableID2=" + redeemableID2);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logit("API2 Signup is successful");

		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID2, "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID2 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationKey"), userID, subAmount, parentMap);

		System.out.println(
				"batchRedemptionProcessResponseUser1==" + batchRedemptionProcessResponseUser1.asPrettyString());

		String actualErrorMessageRedp1 = batchRedemptionProcessResponseUser1.jsonPath().getString("failures[0].message")
				.replace("[", "").replace("]", "");

		Assert.assertEquals(actualErrorMessageRedp1, dataSet.get("errorMessage"));

		pageObj.utils().logPass(actualErrorMessageRedp1 + " error message is verified for the Zero Flat discount");

	}

	@Test(description = "OMM-T77 STEP -8")
	public void validateStep8_TC77() throws InterruptedException {

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();

		String lineItemID1 = Integer.toString(Utilities.getRandomNoFromRange(100, 2000));
		String lineItemFilterName1 = "Automation_LF_ItemID2_" + lineItemID1;

		String QCname1 = "Automation_OMM_QC1_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();
		String percentageOff1 = Integer.toString(Utilities.getRandomNoFromRange(5, 15));
		String redeemableName1 = "Auto_OMM_Redeemable1_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();

		Thread.sleep(2000);
		String QCname2 = "Automation_OMM_QC2_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();
		String flatDiscount = Integer.toString(Utilities.getRandomNoFromRange(1, 10));

		double flatDiscountAmt = Double.parseDouble(flatDiscount);
		String redeemableName2 = "Auto_OMM_Redeemable2_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();

		String itemAmount1 = Integer.toString(Utilities.getRandomNoFromRange(50, 100));
		String itemAmount2 = Integer.toString(Utilities.getRandomNoFromRange(10, 50));

		String subAmount = Integer.toString(Utilities.getRandomNoFromRange(100, 150));
		String lineItemID3 = Integer.toString(Utilities.getRandomNoFromRange(4000, 5000));

		detailsMap1.put("item_name", "Pizza1");
		detailsMap1.put("item_qty", "1");
		detailsMap1.put("amount", itemAmount1);
		detailsMap1.put("item_type", "M");
		detailsMap1.put("item_family", "10");
		detailsMap1.put("item_group", "999");
		detailsMap1.put("serial_number", "1");
		detailsMap1.put("item_id", lineItemID1);

		parentMap.put("Pizza1", detailsMap1);

		detailsMap2.put("item_name", "Pizza2");
		detailsMap2.put("item_qty", "1");
		detailsMap2.put("amount", itemAmount2);
		detailsMap2.put("item_type", "M");
		detailsMap2.put("item_family", "10");
		detailsMap2.put("item_group", "999");
		detailsMap2.put("serial_number", "2");
		detailsMap2.put("item_id", lineItemID3);

		parentMap.put("Pizza2", detailsMap2);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Creating Redeemable1 with QC1 and Line item filter1
		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");
		pageObj.lineItemSelectorPage().createLineItemFilterIfNotExist(lineItemFilterName1, lineItemID1, filterSetName);

		Thread.sleep(5000);
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().createQCIfNotExist(QCname1, "", "Sum of Amounts", "", true,
				lineItemFilterName1, "Maximum Unit Price", percentageOff1, false, true, enableMenuItemAggregatorFlag,
				"", location2);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().clickOnRedeemableTab("$ Or % Off");

		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(redeemableName1, "Existing Qualifier",
				QCname1, "");

		Thread.sleep(10000);
		String redeemableID1 = pageObj.redeemablePage().getRedeemableID(redeemableName1);
		System.out.println("redeemableID1=" + redeemableID1);

		// ******************************************************

		Thread.sleep(5000);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().clickOnRedeemableTab("$ Or % Off");

		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(redeemableName2, "Flat Discount", QCname2,
				flatDiscount);

		Thread.sleep(10000);
		String redeemableID2 = pageObj.redeemablePage().getRedeemableID(redeemableName2);
		System.out.println("redeemableID2=" + redeemableID2);

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
				redeemableID1, "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID2, "", "");
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
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationKey"), userID, subAmount, parentMap);

		System.out.println(
				"batchRedemptionProcessResponseUser1==" + batchRedemptionProcessResponseUser1.asPrettyString());

		double actualRedeemption1DiscountAmt1 = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));

		double actualRedeemption1DiscountAmt2 = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[1].discount_amount").replace("[", "").replace("]", ""));

		int percentageOffInt1 = Integer.parseInt(percentageOff1);
		int itemAmountInt1 = Integer.parseInt(itemAmount1);

		double expItemDiscountAmount1 = (double) (percentageOffInt1 * itemAmountInt1) / 100;
		Assert.assertEquals(actualRedeemption1DiscountAmt1, expItemDiscountAmount1);

		pageObj.utils().logPass(expItemDiscountAmount1 + " amount is verified for the redeemption1 ");

		Assert.assertEquals(actualRedeemption1DiscountAmt2, flatDiscountAmt);
		pageObj.utils().logPass(flatDiscountAmt + " amount is verified for the redeemption2 ");

		double expTotalDiscount = expItemDiscountAmount1 + flatDiscountAmt;
		double actualTotalDiscount = actualRedeemption1DiscountAmt1 + actualRedeemption1DiscountAmt2;

		Assert.assertEquals(actualTotalDiscount, expTotalDiscount);

		pageObj.utils().logPass(expTotalDiscount + " total amount is verified ");

	}

	@Test(description = "OMM-T77 STEP -7")
	public void validateStep7_TC77() throws InterruptedException {

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();

		String lineItemID1 = Integer.toString(Utilities.getRandomNoFromRange(100, 2000));
		String lineItemFilterName1 = "Automation_LF_ItemID1_" + lineItemID1;

		String QCname1 = "Automation_OMM_QC1_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();
		String percentageOff1 = Integer.toString(Utilities.getRandomNoFromRange(5, 15));
		String redeemableName1 = "Auto_OMM_Redeemable1_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();

		String lineItemID2 = Integer.toString(Utilities.getRandomNoFromRange(2000, 4000));
		String lineItemFilterName2 = "Automation_LF_ItemID2_" + lineItemID2;
		Thread.sleep(2000);
		String QCname2 = "Automation_OMM_QC2_" + lineItemID2 + "_" + CreateDateTime.getTimeDateString();
		String percentageOff2 = Integer.toString(Utilities.getRandomNoFromRange(15, 20));
		String redeemableName2 = "Auto_OMM_Redeemable2_" + lineItemID2 + "_" + CreateDateTime.getTimeDateString();

		String itemAmount1 = Integer.toString(Utilities.getRandomNoFromRange(100, 150));
		String itemAmount2 = Integer.toString(Utilities.getRandomNoFromRange(50, 100));
		String itemAmount3 = Integer.toString(Utilities.getRandomNoFromRange(10, 50));

		String subAmount = Integer.toString(Utilities.getRandomNoFromRange(100, 150));
		String lineItemID3 = Integer.toString(Utilities.getRandomNoFromRange(4000, 5000));

		detailsMap1.put("item_name", "Pizza1");
		detailsMap1.put("item_qty", "1");
		detailsMap1.put("amount", itemAmount1);
		detailsMap1.put("item_type", "M");
		detailsMap1.put("item_family", "10");
		detailsMap1.put("item_group", "999");
		detailsMap1.put("serial_number", "1");
		detailsMap1.put("item_id", lineItemID1);

		parentMap.put("Pizza1", detailsMap1);

		detailsMap2.put("item_name", "Pizza2");
		detailsMap2.put("item_qty", "1");
		detailsMap2.put("amount", itemAmount2);
		detailsMap2.put("item_type", "M");
		detailsMap2.put("item_family", "10");
		detailsMap2.put("item_group", "999");
		detailsMap2.put("serial_number", "2");
		detailsMap2.put("item_id", lineItemID2);

		parentMap.put("Pizza2", detailsMap2);

		detailsMap3.put("item_name", "Coffee");
		detailsMap3.put("item_qty", "1");
		detailsMap3.put("amount", itemAmount3);
		detailsMap3.put("item_type", "M");
		detailsMap3.put("item_family", "10");
		detailsMap3.put("item_group", "999");
		detailsMap3.put("serial_number", "3");
		detailsMap3.put("item_id", lineItemID3);

		parentMap.put("Coffee", detailsMap3);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Creating Redeemable1 with QC1 and Line item filter1
		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");
		pageObj.lineItemSelectorPage().createLineItemFilterIfNotExist(lineItemFilterName1, lineItemID1, filterSetName);

		Thread.sleep(5000);
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().createQCIfNotExist(QCname1, "", "Sum of Amounts", "", true,
				lineItemFilterName1, "Maximum Unit Price", percentageOff1, false, true, enableMenuItemAggregatorFlag,
				"", location2);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().clickOnRedeemableTab("$ Or % Off");

		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(redeemableName1, "Existing Qualifier",
				QCname1, "");

		Thread.sleep(10000);
		String redeemableID1 = pageObj.redeemablePage().getRedeemableID(redeemableName1);
		System.out.println("redeemableID1=" + redeemableID1);

		// ******************************************************

		// Creating Redeemable2 with QC2 and Line item filter2

		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");

		// pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");
		pageObj.lineItemSelectorPage().createLineItemFilterIfNotExist(lineItemFilterName2, lineItemID2, filterSetName);

		Thread.sleep(5000);
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().createQCIfNotExist(QCname2, "", "Sum of Amounts", "", true,
				lineItemFilterName2, "Maximum Unit Price", percentageOff2, false, true, enableMenuItemAggregatorFlag,
				"", location2);
		Thread.sleep(5000);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().clickOnRedeemableTab("$ Or % Off");

		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(redeemableName2, "Existing Qualifier",
				QCname2, "");

		Thread.sleep(10000);
		String redeemableID2 = pageObj.redeemablePage().getRedeemableID(redeemableName2);
		System.out.println("redeemableID2=" + redeemableID2);

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
				redeemableID1, "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID2, "", "");
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
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationKey"), userID, subAmount, parentMap);

		System.out.println(
				"batchRedemptionProcessResponseUser1==" + batchRedemptionProcessResponseUser1.asPrettyString());

		double actualRedeemption1DiscountAmt1 = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));

		double actualRedeemption1DiscountAmt2 = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[1].discount_amount").replace("[", "").replace("]", ""));

		int percentageOffInt1 = Integer.parseInt(percentageOff1);
		int itemAmountInt1 = Integer.parseInt(itemAmount1);

		double expItemDiscountAmount1 = (double) (percentageOffInt1 * itemAmountInt1) / 100;
		Assert.assertEquals(actualRedeemption1DiscountAmt1, expItemDiscountAmount1);

		pageObj.utils().logPass(expItemDiscountAmount1 + " amount is verified for the redeemption1 ");

		int percentageOffInt2 = Integer.parseInt(percentageOff2);
		int itemAmountInt2 = Integer.parseInt(itemAmount2);

		double expItemDiscountAmount2 = (double) (percentageOffInt2 * itemAmountInt2) / 100;
		Assert.assertEquals(actualRedeemption1DiscountAmt2, expItemDiscountAmount2);

		pageObj.utils().logPass(expItemDiscountAmount2 + " amount is verified for the redeemption2 ");

	}

//	Pre-condition -> Turn On Discount Stacking Off in attached QC
//	Guest Discount Basket ->
//	Reward 1 -> 10% Off Pizza (Max Price) Reward 2 -> 20% Off Pizza (Max Price)

	@Test(description = "OMM-T77 STEP -4")
	public void validateStep4_TC77() throws InterruptedException {

		DecimalFormat decformat = new DecimalFormat("0.00");

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();

		String lineItemID1 = Integer.toString(Utilities.getRandomNoFromRange(100, 2000));
		String lineItemFilterName1 = "Automation_LF_ItemID2_" + lineItemID1;

		String QCname1 = "Automation_OMM_QC1_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();
		String percentageOff1 = Integer.toString(Utilities.getRandomNoFromRange(5, 15));
		String redeemableName1 = "Auto_OMM_Redeemable1_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();

		String lineItemID2 = Integer.toString(Utilities.getRandomNoFromRange(2000, 4000));
		String lineItemFilterName2 = "Automation_LF_ItemID2_" + lineItemID2;
		Thread.sleep(2000);
		String QCname2 = "Automation_OMM_QC2_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();
		String percentageOff2 = Integer.toString(Utilities.getRandomNoFromRange(15, 20));
		String redeemableName2 = "Auto_OMM_Redeemable2_" + lineItemID1 + "_" + CreateDateTime.getTimeDateString();

		String itemAmount1 = Integer.toString(Utilities.getRandomNoFromRange(50, 100));
		String itemAmount2 = Integer.toString(Utilities.getRandomNoFromRange(10, 50));

		String subAmount = Integer.toString(Utilities.getRandomNoFromRange(100, 150));
		String lineItemID3 = Integer.toString(Utilities.getRandomNoFromRange(4000, 5000));

		detailsMap1.put("item_name", "Pizza1");
		detailsMap1.put("item_qty", "2");
		detailsMap1.put("amount", itemAmount1);
		detailsMap1.put("item_type", "M");
		detailsMap1.put("item_family", "10");
		detailsMap1.put("item_group", "999");
		detailsMap1.put("serial_number", "1");
		detailsMap1.put("item_id", lineItemID1);

		parentMap.put("Pizza1", detailsMap1);

		detailsMap2.put("item_name", "Pizza2");
		detailsMap2.put("item_qty", "1");
		detailsMap2.put("amount", itemAmount2);
		detailsMap2.put("item_type", "M");
		detailsMap2.put("item_family", "10");
		detailsMap2.put("item_group", "999");
		detailsMap2.put("serial_number", "2");
		detailsMap2.put("item_id", lineItemID3);

		parentMap.put("Pizza2", detailsMap2);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Creating Redeemable1 with QC1 and Line item filter1
		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");
		pageObj.lineItemSelectorPage().createLineItemFilterIfNotExist(lineItemFilterName1, lineItemID1, filterSetName);

		Thread.sleep(5000);
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().createQCIfNotExist(QCname1, "", "Sum of Amounts", "", true,
				lineItemFilterName1, "Maximum Unit Price", percentageOff1, false, true, enableMenuItemAggregatorFlag,
				"", location2);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().clickOnRedeemableTab("$ Or % Off");

		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(redeemableName1, "Existing Qualifier",
				QCname1, "");

		Thread.sleep(10000);
		String redeemableID1 = pageObj.redeemablePage().getRedeemableID(redeemableName1);
		System.out.println("redeemableID1=" + redeemableID1);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");

		// pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");
		pageObj.lineItemSelectorPage().createLineItemFilterIfNotExist(lineItemFilterName2, lineItemID2, filterSetName);

		Thread.sleep(5000);
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().createQCIfNotExist(QCname2, "", "Sum of Amounts", "", true,
				lineItemFilterName2, "Maximum Unit Price", percentageOff2, false, true, enableMenuItemAggregatorFlag,
				"", location2);
		Thread.sleep(5000);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().clickOnRedeemableTab("$ Or % Off");

		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(redeemableName2, "Existing Qualifier",
				QCname2, "");

		Thread.sleep(10000);
		String redeemableID2 = pageObj.redeemablePage().getRedeemableID(redeemableName2);
		System.out.println("redeemableID2=" + redeemableID2);

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
				redeemableID1, "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID2, "", "");
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
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationKey"), userID, subAmount, parentMap);

		System.out.println(
				"batchRedemptionProcessResponseUser1==" + batchRedemptionProcessResponseUser1.asPrettyString());

		double actualRedeemption1DiscountAmt1 = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));

		String actualErrorMessageRedemption2 = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("failures[0].message").replace("[", "").replace("]", "");

		int percentageOffInt1 = Integer.parseInt(percentageOff1);
		double itemAmountInt1 = Double.parseDouble(itemAmount1) / 2;

		double expItemDiscountAmount1 = (double) percentageOffInt1 * itemAmountInt1 / 100;

		expItemDiscountAmount1 = Double.parseDouble(decformat.format(expItemDiscountAmount1));

		Assert.assertEquals(actualRedeemption1DiscountAmt1, expItemDiscountAmount1);

		pageObj.utils().logPass(expItemDiscountAmount1 + " amount is verified for the redeemption1 ");

		boolean verifyStatus = pageObj.redeemablePage().verifyErrorMessage(actualErrorMessageRedemption2,
				dataSet.get("errorMessage"));
		Assert.assertTrue(verifyStatus);
		pageObj.utils().logPass("Verified the error message for redemption2");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
