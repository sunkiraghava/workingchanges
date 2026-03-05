package com.punchh.server.OMMTest;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.Listeners;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apimodel.lineitemselector.LineItemSelectorFilterItemSet;
import com.punchh.server.pages.ApiPayloadObj;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;

import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

/*
 * @Author :- Shashank Sharma
*/

@Listeners(TestListeners.class)
public class ValidationInBatchRedemptionProcessOMM_TC78 {
	static Logger logger = LogManager.getLogger(ValidationInBatchRedemptionProcessOMM_TC78.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private boolean GlobalBenefitRedemptionThrottlingToggle;
	private List<String> codeNameList;
	Utilities utils;
	private ApiPayloadObj apipayloadObj;
	private String lisExternalID, qcExternalID, redeemableExternalID, coupanCampaignName ;
	
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
		utils = new Utilities(driver);
		utils.logit(sTCName + " ==>" + dataSet);
		GlobalBenefitRedemptionThrottlingToggle = false;
		codeNameList = new ArrayList<String>();
		apipayloadObj = new ApiPayloadObj();

	}

	@Test(description="SQ-T7482 - Step 1: Verify functionality cases for redemption_code",priority=1)
	@Owner(name = "Shashank Sharma")
	public void validateRedempetionCalcutionWithoutQCDiscountLookup() throws Exception {
		
		double expectedFirstRedempetion_DiscountedAmount = Double
		        .parseDouble(dataSet.get("expectedFirstRedempetion_DiscountedAmount"));
		utils.logit("Expected First Redemption Discounted Amount: " + expectedFirstRedempetion_DiscountedAmount);

		double expectedSecondRedempetion_DiscountedAmount = Double
		        .parseDouble(dataSet.get("expectedSecondRedempetion_DiscountedAmount"));
		utils.logit("Expected Second Redemption Discounted Amount: " + expectedSecondRedempetion_DiscountedAmount);

		double expectedThirdRedempetion_DiscountedAmount = Double
		        .parseDouble(dataSet.get("expectedThirdRedempetion_DiscountedAmount"));
		utils.logit("Expected Third Redemption Discounted Amount: " + expectedThirdRedempetion_DiscountedAmount);

		String expectedQualifiedItemName = dataSet.get("expectedQualifiedItemName");
		utils.logit("Expected Qualified Item Name: " + expectedQualifiedItemName);

		String coupanCampaignName = "Auto_CouponCampaign" + CreateDateTime.getTimeDateString();
		utils.logit("Coupon Campaign Name: " + coupanCampaignName);

		GlobalBenefitRedemptionThrottlingToggle = true;
		utils.logit("GlobalBenefitRedemptionThrottlingToggle set to true");

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(coupanCampaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), "", "",
				GlobalBenefitRedemptionThrottlingToggle);

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		utils.logit("Campaign success message validation status: " + status);
		Assert.assertTrue(status, "Campaign created success message did not displayed....");
		utils.logit("Campaign created success message displayed successfully");
		pageObj.campaignspage().searchCampaign(coupanCampaignName);
		codeNameList = pageObj.campaignspage().getPreGeneratedCuponCodeList();

		String couponCode1 = codeNameList.get(0);
		String couponCode2 = codeNameList.get(1);
		String couponCode3 = codeNameList.get(2);
		utils.logit("Coupon Codes → 1: " + couponCode1 + ", 2: " + couponCode2 + ", 3: " + couponCode3);

		// Create user 1

		String userEmailUser1 = pageObj.iframeSingUpPage().generateEmail();
		utils.logit("Generated user email: " + userEmailUser1);

		Response signUpResponseUser1 = pageObj.endpoints().Api2SignUp(userEmailUser1, dataSet.get("client"), dataSet.get("secret"));
		utils.logit("User signup API HTTP status: " + signUpResponseUser1.getStatusCode());
		Assert.assertEquals(signUpResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, 
		        "API 2 user signup failed: Unexpected HTTP status code");

		String tokenUser1 = signUpResponseUser1.jsonPath().getString("access_token.token");
		utils.logit("User token: " + tokenUser1);
		Assert.assertNotNull(tokenUser1, "API 2 user signup failed: Access token is null or missing");

		// Extract and validate user ID
		String userIDUser1 = signUpResponseUser1.jsonPath().getString("user.user_id");
		utils.logit("User ID: " + userIDUser1);
		Assert.assertNotNull(userIDUser1, "API 2 user signup failed: User ID is null or missing");
		

		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userIDUser1, dataSet.get("apiKey"), "10",
				"", "", "");
		utils.logit("Send reward API response: " + sendRewardResponse.asPrettyString());
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Send reward API failed");


		Response discountBasketResponseCoupon1 = pageObj.endpoints().authListDiscountBasketAdded(
		        tokenUser1, dataSet.get("client"), dataSet.get("secret"), "redemption_code", couponCode1);
		utils.logit(couponCode1 + " code is added to basket");
		Assert.assertEquals(discountBasketResponseCoupon1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
		        "Add coupon " + couponCode1 + " to basket API failed");

		Response discountBasketResponseCoupon2 = pageObj.endpoints().authListDiscountBasketAdded(
		        tokenUser1, dataSet.get("client"), dataSet.get("secret"), "redemption_code", couponCode2);
		utils.logit(couponCode2 + " code is added to basket");
		Assert.assertEquals(discountBasketResponseCoupon2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
		        "Add coupon " + couponCode2 + " to basket API failed");

		Response discountBasketResponseCoupon3 = pageObj.endpoints().authListDiscountBasketAdded(
		        tokenUser1, dataSet.get("client"), dataSet.get("secret"), "redemption_code", couponCode3);
		utils.logit(couponCode3 + " code is added to basket");
		Assert.assertEquals(discountBasketResponseCoupon3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
		        "Add coupon " + couponCode3 + " to basket API failed");


		int exp_FirstRDP_discount_basket_item_id = discountBasketResponseCoupon3.jsonPath()
		        .getInt("discount_basket_items[0].discount_basket_item_id");
		utils.logit("First RDP Discount Basket Item ID: " + exp_FirstRDP_discount_basket_item_id);

		String exp_FirstRDP_discount_id = discountBasketResponseCoupon3.jsonPath()
		        .getString("discount_basket_items[0].discount_id");
		utils.logit("First RDP Discount ID: " + exp_FirstRDP_discount_id);

		int exp_SecondRDP_discount_basket_item_id = discountBasketResponseCoupon3.jsonPath()
		        .getInt("discount_basket_items[1].discount_basket_item_id");
		utils.logit("Second RDP Discount Basket Item ID: " + exp_SecondRDP_discount_basket_item_id);

		String exp_SecondRDP_discount_id = discountBasketResponseCoupon3.jsonPath()
		        .getString("discount_basket_items[1].discount_id");
		utils.logit("Second RDP Discount ID: " + exp_SecondRDP_discount_id);

		int exp_ThirdRDP_discount_basket_item_id = discountBasketResponseCoupon3.jsonPath()
		        .getInt("discount_basket_items[2].discount_basket_item_id");
		utils.logit("Third RDP Discount Basket Item ID: " + exp_ThirdRDP_discount_basket_item_id);

		String exp_ThirdRDP_discount_id = discountBasketResponseCoupon3.jsonPath()
		        .getString("discount_basket_items[2].discount_id");
		utils.logit("Third RDP Discount ID: " + exp_ThirdRDP_discount_id);

		utils.logit(couponCode3 + " code is added to basket");

		// Create Input Receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};
		// Input Receipt Details in below format:
		// item_name|item_qty|amount|item_type|item_family|item_group|serial_number|item_id
		addDetails.accept("Pizza1", new String[] { "Pizza1", "1", "10", "M", "10", "999", "1", "101" });

		// Process basket by user1
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSDiscountLookup(dataSet.get("locationkey"), userIDUser1,
						dataSet.get("subAmount"),parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Batch redemption API failed: Unexpected HTTP status code");


		double actualFirstRedempetion_DiscountedAmount = batchRedemptionProcessResponseUser1.jsonPath()
		        .getDouble("selected_discounts[0].discount_amount");
		utils.logit("Actual First Redemption Discounted Amount: " + actualFirstRedempetion_DiscountedAmount);

		int actual_FirstRDP_discount_basket_item_id = batchRedemptionProcessResponseUser1.jsonPath()
		        .getInt("selected_discounts[0].discount_basket_item_id");
		utils.logit("Actual First RDP Discount Basket Item ID: " + actual_FirstRDP_discount_basket_item_id);

		String actual_FirstRDP_discount_id = batchRedemptionProcessResponseUser1.jsonPath()
		        .getString("selected_discounts[0].discount_id");
		utils.logit("Actual First RDP Discount ID: " + actual_FirstRDP_discount_id);

		Assert.assertEquals(actualFirstRedempetion_DiscountedAmount, expectedFirstRedempetion_DiscountedAmount,
		        "First redemption discounted amount mismatch");
		Assert.assertEquals(actual_FirstRDP_discount_basket_item_id, exp_FirstRDP_discount_basket_item_id,
		        "First redemption discount basket item ID mismatch");
		Assert.assertEquals(actual_FirstRDP_discount_id, exp_FirstRDP_discount_id,
		        "First redemption discount ID mismatch");

		utils.logit("Verified that redemption 1 data");

		double actualSecondRedempetion_DiscountedAmount = batchRedemptionProcessResponseUser1.jsonPath()
		        .getDouble("selected_discounts[1].discount_amount");
		utils.logit("Actual Second Redemption Discounted Amount: " + actualSecondRedempetion_DiscountedAmount);

		int actual_SecondRDP_discount_basket_item_id = batchRedemptionProcessResponseUser1.jsonPath()
		        .getInt("selected_discounts[1].discount_basket_item_id");
		utils.logit("Actual Second RDP Discount Basket Item ID: " + actual_SecondRDP_discount_basket_item_id);

		String actual_SecondRDP_discount_id = batchRedemptionProcessResponseUser1.jsonPath()
		        .getString("selected_discounts[1].discount_id");
		utils.logit("Actual Second RDP Discount ID: " + actual_SecondRDP_discount_id);

		Assert.assertEquals(actualSecondRedempetion_DiscountedAmount, expectedSecondRedempetion_DiscountedAmount,
		        "Second redemption discounted amount mismatch");
		Assert.assertEquals(actual_SecondRDP_discount_basket_item_id, exp_SecondRDP_discount_basket_item_id,
		        "Second redemption discount basket item ID mismatch");
		Assert.assertEquals(actual_SecondRDP_discount_id, exp_SecondRDP_discount_id,
		        "Second redemption discount ID mismatch");

		utils.logit("Verified that redemption 2 data");

		double actualThirdRedempetion_DiscountedAmount = batchRedemptionProcessResponseUser1.jsonPath()
		        .getDouble("selected_discounts[2].discount_amount");
		utils.logit("Actual Third Redemption Discounted Amount: " + actualThirdRedempetion_DiscountedAmount);

		int actual_ThirdRDP_discount_basket_item_id = batchRedemptionProcessResponseUser1.jsonPath()
		        .getInt("selected_discounts[2].discount_basket_item_id");
		utils.logit("Actual Third RDP Discount Basket Item ID: " + actual_ThirdRDP_discount_basket_item_id);

		String actual_ThirdRDP_discount_id = batchRedemptionProcessResponseUser1.jsonPath()
		        .getString("selected_discounts[2].discount_id");
		utils.logit("Actual Third RDP Discount ID: " + actual_ThirdRDP_discount_id);

		Assert.assertEquals(actualThirdRedempetion_DiscountedAmount, expectedThirdRedempetion_DiscountedAmount,
		        "Third redemption discounted amount mismatch");
		Assert.assertEquals(actual_ThirdRDP_discount_basket_item_id, exp_ThirdRDP_discount_basket_item_id,
		        "Third redemption discount basket item ID mismatch");
		Assert.assertEquals(actual_ThirdRDP_discount_id, exp_ThirdRDP_discount_id,
		        "Third redemption discount ID mismatch");

		utils.logit("Verified that redemption 3 data");
		utils.logit(userIDUser1 + " processed the basket");

		float discountedAmount1 = Float.parseFloat(batchRedemptionProcessResponseUser1.jsonPath().getString("selected_discounts[0].discount_amount"));
		utils.logit("Discounted Amount 1: " + discountedAmount1);

		float discountedAmount2 = Float.parseFloat(batchRedemptionProcessResponseUser1.jsonPath().getString("selected_discounts[1].discount_amount"));
		utils.logit("Discounted Amount 2: " + discountedAmount2);

		float discountedAmount3 = Float.parseFloat(batchRedemptionProcessResponseUser1.jsonPath().getString("selected_discounts[2].discount_amount"));
		utils.logit("Discounted Amount 3: " + discountedAmount3);

		float totalDiscount = discountedAmount1 + discountedAmount2 + discountedAmount3;
		utils.logit("Total discount available for the receipt: " + totalDiscount);

		float expectedTotalDiscount = Float.parseFloat(dataSet.get("expectedTotalDiscount"));
		utils.logit("Expected total discount for the receipt: " + expectedTotalDiscount);

		Assert.assertEquals(totalDiscount, expectedTotalDiscount, "Total discount for the receipt mismatch");

		utils.logit("Verified the discounted amount for the receipt");

	}

	@Test(description="SQ-T7482 - Step 2: Verify functionality cases for redemption_code",priority=2)
	@Owner(name = "Shashank Sharma")
	public void validateRedempetionCalcutionQCDiscountLookup() throws Exception {
		
		double expectedFirstRedempetion_DiscountedAmount =
		        Double.parseDouble(dataSet.get("expectedFirstRedempetion_DiscountedAmount"));
		utils.logit("Expected first redemption discounted amount: " + expectedFirstRedempetion_DiscountedAmount);

		double expectedSecondRedempetion_DiscountedAmount =
		        Double.parseDouble(dataSet.get("expectedSecondRedempetion_DiscountedAmount"));
		utils.logit("Expected second redemption discounted amount: " + expectedSecondRedempetion_DiscountedAmount);

		double expectedThirdRedempetion_DiscountedAmount =
		        Double.parseDouble(dataSet.get("expectedThirdRedempetion_DiscountedAmount"));
		utils.logit("Expected third redemption discounted amount: " + expectedThirdRedempetion_DiscountedAmount);

		String expectedQualifiedItemName = dataSet.get("expectedQualifiedItemName");
		utils.logit("Expected qualified item name: " + expectedQualifiedItemName);

		String coupanCampaignName = "Auto_CuponCampaign" + CreateDateTime.getTimeDateString();
		utils.logit("Generated coupon campaign name: " + coupanCampaignName);
		
		// =====================  Create LIS =====================
		String lisName = "Automation_LIS_T7482_" + Utilities.getTimestamp();
		utils.logit("Creating LIS: " + lisName);
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName).setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "101").build();
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(),ApiConstants.HTTP_STATUS_OK, "LIS creation API failed");
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalID, "LIS external_id is null");
		utils.logit("LIS created with External ID: " + lisExternalID);


		// =====================️ Create QC =====================
		String qcname = "AutomationQC_T7482" + Utilities.getTimestamp();
		utils.logit("Creating QC: " + qcname);
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname).setPercentageOfProcessedAmount(10)
				.setQCProcessingFunction("sum_amounts").setStackDiscounting(true).setReuseQualifyingItems(true)				
				.addLineItemFilter(lisExternalID, "max_price", 1)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(),ApiConstants.HTTP_STATUS_OK, "QC creation API failed");

					// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(coupanCampaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), "",
				qcname, GlobalBenefitRedemptionThrottlingToggle);

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		pageObj.campaignspage().searchCampaign(coupanCampaignName);
		codeNameList = pageObj.campaignspage().getPreGeneratedCuponCodeList();

		String couponCode1 = codeNameList.get(0);
		String couponCode2 = codeNameList.get(1);
		String couponCode3 = codeNameList.get(2);
		utils.logit("Coupon Codes → 1: " + couponCode1 + ", 2: " + couponCode2 + ", 3: " + couponCode3);
		// Create user 1
		String userEmailUser1 = pageObj.iframeSingUpPage().generateEmail();
		utils.logit("Generated user email: " + userEmailUser1);

		Response signUpResponseUser1 = pageObj.endpoints().Api2SignUp(userEmailUser1, dataSet.get("client"), dataSet.get("secret"));
		utils.logit("User signup API HTTP status: " + signUpResponseUser1.getStatusCode());
		Assert.assertEquals(signUpResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, 
		        "API 2 user signup failed: Unexpected HTTP status code");

		String tokenUser1 = signUpResponseUser1.jsonPath().getString("access_token.token");
		utils.logit("User token: " + tokenUser1);
		Assert.assertNotNull(tokenUser1, "API 2 user signup failed: Access token is null or missing");

		// Extract and validate user ID
		String userIDUser1 = signUpResponseUser1.jsonPath().getString("user.user_id");
		utils.logit("User ID: " + userIDUser1);
		Assert.assertNotNull(userIDUser1, "API 2 user signup failed: User ID is null or missing");

		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userIDUser1, dataSet.get("apiKey"), "10",
				"", "", "");
		utils.logit("Send reward API response: " + sendRewardResponse.asPrettyString());
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Send reward API failed");
		
		Response discountBasketResponseCoupon1 = pageObj.endpoints().authListDiscountBasketAdded(tokenUser1,
				dataSet.get("client"), dataSet.get("secret"), "redemption_code", couponCode1);
		Assert.assertEquals(discountBasketResponseCoupon1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
		        "Add coupon " + couponCode1 + " to basket API failed: Unexpected HTTP status");

		Response discountBasketResponseCoupon2 = pageObj.endpoints().authListDiscountBasketAdded(tokenUser1,
				dataSet.get("client"), dataSet.get("secret"), "redemption_code", couponCode2);
		Assert.assertEquals(discountBasketResponseCoupon2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
		        "Add coupon " + couponCode2 + " to basket API failed: Unexpected HTTP status");

		Response discountBasketResponseCoupon3 = pageObj.endpoints().authListDiscountBasketAdded(tokenUser1,
				dataSet.get("client"), dataSet.get("secret"), "redemption_code", couponCode3);
		Assert.assertEquals(discountBasketResponseCoupon3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
		        "Add coupon " + couponCode3 + " to basket API failed: Unexpected HTTP status");

		int exp_FirstRDP_discount_basket_item_id = discountBasketResponseCoupon3.jsonPath().getInt("discount_basket_items[0].discount_basket_item_id");
		utils.logit("First RDP Basket Item ID: " + exp_FirstRDP_discount_basket_item_id);

		String exp_FirstRDP_discount_id = discountBasketResponseCoupon3.jsonPath().getString("discount_basket_items[0].discount_id");
		utils.logit("First RDP Discount ID: " + exp_FirstRDP_discount_id);

		int exp_SecondRDP_discount_basket_item_id = discountBasketResponseCoupon3.jsonPath().getInt("discount_basket_items[1].discount_basket_item_id");
		utils.logit("Second RDP Basket Item ID: " + exp_SecondRDP_discount_basket_item_id);

		String exp_SecondRDP_discount_id = discountBasketResponseCoupon3.jsonPath().getString("discount_basket_items[1].discount_id");
		utils.logit("Second RDP Discount ID: " + exp_SecondRDP_discount_id);

		int exp_ThirdRDP_discount_basket_item_id = discountBasketResponseCoupon3.jsonPath().getInt("discount_basket_items[2].discount_basket_item_id");
		utils.logit("Third RDP Basket Item ID: " + exp_ThirdRDP_discount_basket_item_id);

		String exp_ThirdRDP_discount_id = discountBasketResponseCoupon3.jsonPath().getString("discount_basket_items[2].discount_id");
		utils.logit("Third RDP Discount ID: " + exp_ThirdRDP_discount_id);

		utils.logit(couponCode3 + " code is added to basket");
		
		// Create Input Receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};
		// Input Receipt Details in below format:
		// item_name|item_qty|amount|item_type|item_family|item_group|serial_number|item_id
		addDetails.accept("Pizza1", new String[] { "Pizza1", "1", "10", "M", "10", "999", "1", "101" });
		addDetails.accept("Pizza2", new String[] { "Pizza2", "1", "7", "M", "10", "888", "2", "101" });
		addDetails.accept("Coffee1", new String[] { "Coffee1", "1", "4", "M", "10", "889", "3", "201" });

		// Process basket by user1
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSDiscountLookup(dataSet.get("locationkey"), userIDUser1,
						dataSet.get("subAmount"),parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Batch redemption API failed: Unexpected HTTP status code");
	
		
		double actualFirstRedempetion_DiscountedAmount = batchRedemptionProcessResponseUser1.jsonPath()
		        .getDouble("selected_discounts[0].discount_amount");
		utils.logit("Actual First Redemption Discounted Amount: " + actualFirstRedempetion_DiscountedAmount);

		int actual_FirstRDP_discount_basket_item_id = batchRedemptionProcessResponseUser1.jsonPath()
		        .getInt("selected_discounts[0].discount_basket_item_id");
		utils.logit("Actual First RDP Discount Basket Item ID: " + actual_FirstRDP_discount_basket_item_id);

		String actual_FirstRDP_discount_id = batchRedemptionProcessResponseUser1.jsonPath()
		        .getString("selected_discounts[0].discount_id");
		utils.logit("Actual First RDP Discount ID: " + actual_FirstRDP_discount_id);

		Assert.assertEquals(actualFirstRedempetion_DiscountedAmount, expectedFirstRedempetion_DiscountedAmount,
		        "First redemption discounted amount mismatch");
		Assert.assertEquals(actual_FirstRDP_discount_basket_item_id, exp_FirstRDP_discount_basket_item_id,
		        "First redemption discount basket item ID mismatch");
		Assert.assertEquals(actual_FirstRDP_discount_id, exp_FirstRDP_discount_id,
		        "First redemption discount ID mismatch");

		utils.logit("Verified that redemption 1 data");


		double actualSecondRedempetion_DiscountedAmount = batchRedemptionProcessResponseUser1.jsonPath()
		        .getDouble("selected_discounts[1].discount_amount");
		utils.logit("Actual Second Redemption Discounted Amount: " + actualSecondRedempetion_DiscountedAmount);

		int actual_SecondRDP_discount_basket_item_id = batchRedemptionProcessResponseUser1.jsonPath()
		        .getInt("selected_discounts[1].discount_basket_item_id");
		utils.logit("Actual Second RDP Discount Basket Item ID: " + actual_SecondRDP_discount_basket_item_id);

		String actual_SecondRDP_discount_id = batchRedemptionProcessResponseUser1.jsonPath()
		        .getString("selected_discounts[1].discount_id");
		utils.logit("Actual Second RDP Discount ID: " + actual_SecondRDP_discount_id);

		Assert.assertEquals(actualSecondRedempetion_DiscountedAmount, expectedSecondRedempetion_DiscountedAmount,
		        "Second redemption discounted amount mismatch");
		Assert.assertEquals(actual_SecondRDP_discount_basket_item_id, exp_SecondRDP_discount_basket_item_id,
		        "Second redemption discount basket item ID mismatch");
		Assert.assertEquals(actual_SecondRDP_discount_id, exp_SecondRDP_discount_id,
		        "Second redemption discount ID mismatch");

		utils.logit("Verified that redemption 2 data");


		double actualThirdRedempetion_DiscountedAmount = batchRedemptionProcessResponseUser1.jsonPath()
		        .getDouble("selected_discounts[2].discount_amount");
		utils.logit("Actual Third Redemption Discounted Amount: " + actualThirdRedempetion_DiscountedAmount);

		int actual_ThirdRDP_discount_basket_item_id = batchRedemptionProcessResponseUser1.jsonPath()
		        .getInt("selected_discounts[2].discount_basket_item_id");
		utils.logit("Actual Third RDP Discount Basket Item ID: " + actual_ThirdRDP_discount_basket_item_id);

		String actual_ThirdRDP_discount_id = batchRedemptionProcessResponseUser1.jsonPath()
		        .getString("selected_discounts[2].discount_id");
		utils.logit("Actual Third RDP Discount ID: " + actual_ThirdRDP_discount_id);

		Assert.assertEquals(actualThirdRedempetion_DiscountedAmount, expectedThirdRedempetion_DiscountedAmount,
		        "Third redemption discounted amount mismatch");
		Assert.assertEquals(actual_ThirdRDP_discount_basket_item_id, exp_ThirdRDP_discount_basket_item_id,
		        "Third redemption discount basket item ID mismatch");
		Assert.assertEquals(actual_ThirdRDP_discount_id, exp_ThirdRDP_discount_id,
		        "Third redemption discount ID mismatch");

		utils.logit("Verified that redemption 3 data");
		utils.logit(userIDUser1 + " processed the basket");


		float discountedAmount1 = Float.parseFloat(
		        batchRedemptionProcessResponseUser1.jsonPath().getString("selected_discounts[0].discount_amount"));
		utils.logit("Discounted Amount 1: " + discountedAmount1);

		float discountedAmount2 = Float.parseFloat(
		        batchRedemptionProcessResponseUser1.jsonPath().getString("selected_discounts[1].discount_amount"));
		utils.logit("Discounted Amount 2: " + discountedAmount2);

		float discountedAmount3 = Float.parseFloat(
		        batchRedemptionProcessResponseUser1.jsonPath().getString("selected_discounts[2].discount_amount"));
		utils.logit("Discounted Amount 3: " + discountedAmount3);

		float totalDiscount = discountedAmount1 + discountedAmount2 + discountedAmount3;
		utils.logit("Total discount available for the receipt: " + totalDiscount);

		float expectedTotalDiscount = Float.parseFloat(dataSet.get("expectedTotalDiscount"));
		utils.logit("Expected total discount for the receipt: " + expectedTotalDiscount);

		Assert.assertEquals(totalDiscount, expectedTotalDiscount,
		        "Total discount for the receipt mismatch");

		utils.logit("Verified the discounted amount for the receipt");

	}

	@Test(description="SQ-T7482 - Step 3: Verify functionality cases for redemption_code",priority=3)
	@Owner(name = "Shashank Sharma")
	public void validateRedempetionCalcution100PrecentQCPaasDiscountLookup() throws Exception {
		
		double expectedFirstRedempetion_DiscountedAmount = Double
		        .parseDouble(dataSet.get("expectedFirstRedempetion_DiscountedAmount"));
		utils.logit("Expected First Redemption Discounted Amount: "
		        + expectedFirstRedempetion_DiscountedAmount);

		double expectedSecondRedempetion_DiscountedAmount = Double
		        .parseDouble(dataSet.get("expectedSecondRedempetion_DiscountedAmount"));
		utils.logit("Expected Second Redemption Discounted Amount: "
		        + expectedSecondRedempetion_DiscountedAmount);

		double expectedThirdRedempetion_DiscountedAmount = Double
		        .parseDouble(dataSet.get("expectedThirdRedempetion_DiscountedAmount"));
		utils.logit("Expected Third Redemption Discounted Amount: "
		        + expectedThirdRedempetion_DiscountedAmount);

		String expectedQualifiedItemName = dataSet.get("expectedQualifiedItemName");
		utils.logit("Expected Qualified Item Name: " + expectedQualifiedItemName);

		String coupanCampaignName = "Auto_CuponCampaign" + CreateDateTime.getTimeDateString();
		utils.logit("coupanCampaignName :: " + coupanCampaignName);
		
		// =====================  Create LIS =====================
		String lisName = "Automation_LIS_T7482_" + Utilities.getTimestamp();
		utils.logit("Creating LIS: " + lisName);
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName).setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "101").build();
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(),ApiConstants.HTTP_STATUS_OK, "LIS creation API failed");
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalID, "LIS external_id is null");
		utils.logit("LIS created with External ID: " + lisExternalID);


		// =====================️ Create QC =====================
		String qcname = "AutomationQC_T7482" + Utilities.getTimestamp();
		utils.logit("Creating QC: " + qcname);
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname).setPercentageOfProcessedAmount(10)
				.setQCProcessingFunction("sum_amounts").setStackDiscounting(true).setReuseQualifyingItems(true)				
				.addLineItemFilter(lisExternalID, "max_price", 1)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(),ApiConstants.HTTP_STATUS_OK, "QC creation API failed");


		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(coupanCampaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), "",
				qcname, GlobalBenefitRedemptionThrottlingToggle);

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Campaign created success message did not displayed....");
		utils.logit("Coupon campaign created successfully");

		
		pageObj.campaignspage().searchCampaign(coupanCampaignName);
		codeNameList = pageObj.campaignspage().getPreGeneratedCuponCodeList();
		Assert.assertNotNull(codeNameList, "Pre-generated coupon code list is null.");
		utils.logit("Fetched pre-generated coupon code list. Total codes found: "
		        + (codeNameList != null ? codeNameList.size() : 0));
		String couponCode1 = codeNameList.get(0);
		String couponCode2 = codeNameList.get(1);
		String couponCode3 = codeNameList.get(2);
		utils.logit("Coupon Codes → 1: " + couponCode1 + ", 2: " + couponCode2 + ", 3: " + couponCode3);

		// Create user 1

		String userEmailUser1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponseUser1 = pageObj.endpoints().Api2SignUp(userEmailUser1, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponseUser1.getStatusCode(),ApiConstants.HTTP_STATUS_OK,"User signup failed");
		String tokenUser1 = signUpResponseUser1.jsonPath().get("access_token.token").toString();
		utils.logit("tokenUser =" + tokenUser1);

		String userIDUser1 = signUpResponseUser1.jsonPath().get("user.user_id").toString();
		utils.logit("userIDUser =" + userIDUser1);
		
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userIDUser1, dataSet.get("apiKey"), "10","", "", "");
		utils.logit("Send reward API response: " + sendRewardResponse.asPrettyString());
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Send reward API failed");
		
		Response discountBasketResponseCoupon1 = pageObj.endpoints().authListDiscountBasketAdded(
		        tokenUser1, dataSet.get("client"), dataSet.get("secret"), "redemption_code", couponCode1);
		Assert.assertEquals(discountBasketResponseCoupon1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
		        "Add coupon " + couponCode1 + " to basket API failed: Unexpected HTTP status");

		Response discountBasketResponseCoupon2 = pageObj.endpoints().authListDiscountBasketAdded(
		        tokenUser1, dataSet.get("client"), dataSet.get("secret"), "redemption_code", couponCode2);
		Assert.assertEquals(discountBasketResponseCoupon2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
		        "Add coupon " + couponCode2 + " to basket API failed: Unexpected HTTP status");

		Response discountBasketResponseCoupon3 = pageObj.endpoints().authListDiscountBasketAdded(
		        tokenUser1, dataSet.get("client"), dataSet.get("secret"), "redemption_code", couponCode3);
		Assert.assertEquals(discountBasketResponseCoupon3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
		        "Add coupon " + couponCode3 + " to basket API failed: Unexpected HTTP status");


		int exp_FirstRDP_discount_basket_item_id = discountBasketResponseCoupon3.jsonPath().getInt("discount_basket_items[0].discount_basket_item_id");
		String exp_FirstRDP_discount_id = discountBasketResponseCoupon3.jsonPath().getString("discount_basket_items[0].discount_id");
		utils.logit("First RDP → Basket Item ID: " + exp_FirstRDP_discount_basket_item_id + ", Discount ID: " + exp_FirstRDP_discount_id);

		int exp_SecondRDP_discount_basket_item_id = discountBasketResponseCoupon3.jsonPath().getInt("discount_basket_items[1].discount_basket_item_id");
		String exp_SecondRDP_discount_id = discountBasketResponseCoupon3.jsonPath().getString("discount_basket_items[1].discount_id");
		utils.logit("Second RDP → Basket Item ID: " + exp_SecondRDP_discount_basket_item_id + ", Discount ID: " + exp_SecondRDP_discount_id);

		int exp_ThirdRDP_discount_basket_item_id = discountBasketResponseCoupon3.jsonPath().getInt("discount_basket_items[2].discount_basket_item_id");
		String exp_ThirdRDP_discount_id = discountBasketResponseCoupon3.jsonPath().getString("discount_basket_items[2].discount_id");
		utils.logit("Third RDP → Basket Item ID: " + exp_ThirdRDP_discount_basket_item_id + ", Discount ID: " + exp_ThirdRDP_discount_id);

		
		// Create Input Receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};
		// Input Receipt Details in below format:
		// item_name|item_qty|amount|item_type|item_family|item_group|serial_number|item_id
		addDetails.accept("Pizza1", new String[] { "Pizza1", "1", "10", "M", "10", "999", "1", "101" });
		addDetails.accept("Pizza2", new String[] { "Pizza2", "1", "7", "M", "10", "888", "2", "101" });
		addDetails.accept("Coffee1", new String[] { "Coffee1", "1", "4", "M", "10", "889", "3", "201" });

		// Process basket by user1
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSDiscountLookup(dataSet.get("locationkey"), userIDUser1,
						dataSet.get("subAmount"),parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
		        "Batch redemption API failed: Unexpected HTTP status code");
		
		double actualFirstRedempetion_DiscountedAmount = batchRedemptionProcessResponseUser1.jsonPath()
		        .getDouble("selected_discounts[0].discount_amount");
		int actual_FirstRDP_discount_basket_item_id = batchRedemptionProcessResponseUser1.jsonPath()
		        .getInt("selected_discounts[0].discount_basket_item_id");
		String actual_FirstRDP_discount_id = batchRedemptionProcessResponseUser1.jsonPath()
		        .getString("selected_discounts[0].discount_id");

		utils.logit("Actual First RDP → Discount Amount: " + actualFirstRedempetion_DiscountedAmount
		        + ", Basket Item ID: " + actual_FirstRDP_discount_basket_item_id
		        + ", Discount ID: " + actual_FirstRDP_discount_id);

		Assert.assertEquals(actualFirstRedempetion_DiscountedAmount, expectedFirstRedempetion_DiscountedAmount,
		        "First redemption discounted amount mismatch");
		Assert.assertEquals(actual_FirstRDP_discount_basket_item_id, exp_FirstRDP_discount_basket_item_id,
		        "First redemption discount basket item ID mismatch");
		Assert.assertEquals(actual_FirstRDP_discount_id, exp_FirstRDP_discount_id,
		        "First redemption discount ID mismatch");

		utils.logit("Verified that redemption 1 data successfully");

		double actualSecondRedempetion_DiscountedAmount = batchRedemptionProcessResponseUser1.jsonPath()
		        .getDouble("selected_discounts[1].discount_amount");
		int actual_SecondRDP_discount_basket_item_id = batchRedemptionProcessResponseUser1.jsonPath()
		        .getInt("selected_discounts[1].discount_basket_item_id");
		String actual_SecondRDP_discount_id = batchRedemptionProcessResponseUser1.jsonPath()
		        .getString("selected_discounts[1].discount_id");

		utils.logit("Actual Second RDP → Discount Amount: " + actualSecondRedempetion_DiscountedAmount
		        + ", Basket Item ID: " + actual_SecondRDP_discount_basket_item_id
		        + ", Discount ID: " + actual_SecondRDP_discount_id);

		Assert.assertEquals(actualSecondRedempetion_DiscountedAmount, expectedSecondRedempetion_DiscountedAmount,
		        "Second redemption discounted amount mismatch");
		Assert.assertEquals(actual_SecondRDP_discount_basket_item_id, exp_SecondRDP_discount_basket_item_id,
		        "Second redemption discount basket item ID mismatch");
		Assert.assertEquals(actual_SecondRDP_discount_id, exp_SecondRDP_discount_id,
		        "Second redemption discount ID mismatch");

		utils.logit("Verified that redemption 2 data successfully");

		double actualThirdRedempetion_DiscountedAmount = batchRedemptionProcessResponseUser1.jsonPath()
		        .getDouble("selected_discounts[2].discount_amount");
		int actual_ThirdRDP_discount_basket_item_id = batchRedemptionProcessResponseUser1.jsonPath()
		        .getInt("selected_discounts[2].discount_basket_item_id");
		String actual_ThirdRDP_discount_id = batchRedemptionProcessResponseUser1.jsonPath()
		        .getString("selected_discounts[2].discount_id");

		utils.logit("Actual Third RDP → Discount Amount: " + actualThirdRedempetion_DiscountedAmount
		        + ", Basket Item ID: " + actual_ThirdRDP_discount_basket_item_id
		        + ", Discount ID: " + actual_ThirdRDP_discount_id);

		Assert.assertEquals(actualThirdRedempetion_DiscountedAmount, expectedThirdRedempetion_DiscountedAmount,
		        "Third redemption discounted amount mismatch");
		Assert.assertEquals(actual_ThirdRDP_discount_basket_item_id, exp_ThirdRDP_discount_basket_item_id,
		        "Third redemption discount basket item ID mismatch");
		Assert.assertEquals(actual_ThirdRDP_discount_id, exp_ThirdRDP_discount_id,
		        "Third redemption discount ID mismatch");

		utils.logit(userIDUser1 + " processed the basket successfully");

		float discountedAmount1 = Float.parseFloat(batchRedemptionProcessResponseUser1.jsonPath().getString("selected_discounts[0].discount_amount"));
		float discountedAmount2 = Float.parseFloat(batchRedemptionProcessResponseUser1.jsonPath().getString("selected_discounts[1].discount_amount"));
		float discountedAmount3 = Float.parseFloat(batchRedemptionProcessResponseUser1.jsonPath().getString("selected_discounts[2].discount_amount"));
		float totalDiscount = discountedAmount1 + discountedAmount2 + discountedAmount3;
		utils.logit("Discounted Amounts → 1: " + discountedAmount1 + ", 2: " + discountedAmount2 + ", 3: " + discountedAmount3 + ", Total: " + totalDiscount);

		float expectedTotalDiscount = Float.parseFloat(dataSet.get("expectedTotalDiscount"));
		Assert.assertEquals(totalDiscount, expectedTotalDiscount, "Total discount for the receipt mismatch");
		utils.logit("Verified the discounted amount for the receipt successfully");

	}

	@Test(description = "SQ-T7482 - Step 4: Verify functionality cases for redemption_code", priority=4)
	@Owner(name = "Shashank Sharma")
	public void validateRedempetionCalcutionZeroPrecentInQCDiscountLookup() throws Exception {
		
		double expectedFirstRedempetion_DiscountedAmount = Double.parseDouble(dataSet.get("expectedFirstRedempetion_DiscountedAmount"));
		utils.logit("Expected First Redemption Discount Amount: " + expectedFirstRedempetion_DiscountedAmount);

		double expectedSecondRedempetion_DiscountedAmount = Double.parseDouble(dataSet.get("expectedSecondRedempetion_DiscountedAmount"));
		utils.logit("Expected Second Redemption Discount Amount: " + expectedSecondRedempetion_DiscountedAmount);

		coupanCampaignName = "Auto_CuponCampaign" + CreateDateTime.getTimeDateString();
		utils.logit("Coupon Campaign Name: " + coupanCampaignName);
		
		// =====================  Create LIS =====================
		String lisName = "Automation_LIS_" + Utilities.getTimestamp();
		utils.logit("Creating LIS: " + lisName);
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName).setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "101").build();
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(),ApiConstants.HTTP_STATUS_OK, "LIS creation API failed");
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalID, "LIS external_id is null");
		utils.logit("LIS created with External ID: " + lisExternalID);


		// =====================️ Create QC =====================
		String qcname = "AutomationQC_" + Utilities.getTimestamp();
		utils.logit("Creating QC: " + qcname);
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setQCProcessingFunction("sum_amounts").setStackDiscounting(true).setReuseQualifyingItems(true)				
				.addLineItemFilter(lisExternalID, "max_price", 1)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(),ApiConstants.HTTP_STATUS_OK, "QC creation API failed");
				

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(coupanCampaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), "",
				qcname, GlobalBenefitRedemptionThrottlingToggle);

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Campaign created success message did not display");
		utils.logit("Coupon campaign created successfully");

		pageObj.campaignspage().searchCampaign(coupanCampaignName);
		codeNameList = pageObj.campaignspage().getPreGeneratedCuponCodeList();

		String couponCode1 = codeNameList.get(0);
		String couponCode2 = codeNameList.get(1);
		String couponCode3 = codeNameList.get(2);
		utils.logit("Coupon Codes → 1: " + couponCode1 + ", 2: " + couponCode2 + ", 3: " + couponCode3);

		// Create user 1

		String userEmailUser1 = pageObj.iframeSingUpPage().generateEmail();
		utils.logit("Generated user email: " + userEmailUser1);

		Response signUpResponseUser1 = pageObj.endpoints().Api2SignUp(userEmailUser1, dataSet.get("client"), dataSet.get("secret"));
		utils.logit("User signup API HTTP status: " + signUpResponseUser1.getStatusCode());
		Assert.assertEquals(signUpResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, 
		        "API 2 user signup failed: Unexpected HTTP status code");

		String tokenUser1 = signUpResponseUser1.jsonPath().getString("access_token.token");
		utils.logit("User token: " + tokenUser1);
		Assert.assertNotNull(tokenUser1, "API 2 user signup failed: Access token is null or missing");

		// Extract and validate user ID
		String userIDUser1 = signUpResponseUser1.jsonPath().getString("user.user_id");
		utils.logit("User ID: " + userIDUser1);
		Assert.assertNotNull(userIDUser1, "API 2 user signup failed: User ID is null or missing");
		

		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userIDUser1, dataSet.get("apiKey"), "10",
				"", "", "");
		utils.logit("Send reward API response: " + sendRewardResponse.asPrettyString());
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Send reward API failed");

		Response discountBasketResponseCoupon1 = pageObj.endpoints().authListDiscountBasketAdded(tokenUser1,
				dataSet.get("client"), dataSet.get("secret"), "redemption_code", couponCode1);
		utils.logit(couponCode1 + " code added to basket. Response: " + discountBasketResponseCoupon1.asPrettyString());
		Assert.assertEquals(discountBasketResponseCoupon1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Add coupon to basket API failed");


		Response discountBasketResponseCoupon2 = pageObj.endpoints().authListDiscountBasketAdded(tokenUser1,
				dataSet.get("client"), dataSet.get("secret"), "redemption_code", couponCode2);
		utils.logit(couponCode2 + " code added to basket. Response: " + discountBasketResponseCoupon2.asPrettyString());
		Assert.assertEquals(discountBasketResponseCoupon2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Add coupon " + couponCode2 + " to basket API failed");

		Response discountBasketResponseCoupon3 = pageObj.endpoints().authListDiscountBasketAdded(tokenUser1,
				dataSet.get("client"), dataSet.get("secret"), "redemption_code", couponCode3);
		utils.logit(couponCode3 + " code added to basket. Response: " + discountBasketResponseCoupon3.asPrettyString());
		Assert.assertEquals(discountBasketResponseCoupon3.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Add coupon " + couponCode3 + " to basket API failed");

		int exp_FirstRDP_discount_basket_item_id = discountBasketResponseCoupon1.jsonPath().getInt("discount_basket_items[0].discount_basket_item_id");
		String exp_FirstRDP_discount_id = discountBasketResponseCoupon1.jsonPath().getString("discount_basket_items[0].discount_id");
		utils.logit("First Discount Basket Item ID: " + exp_FirstRDP_discount_basket_item_id + ", Discount ID: " + exp_FirstRDP_discount_id);

		// Extract second discount basket item
		int exp_SecondRDP_discount_basket_item_id = discountBasketResponseCoupon2.jsonPath().getInt("discount_basket_items[1].discount_basket_item_id");
		String exp_SecondRDP_discount_id = discountBasketResponseCoupon2.jsonPath().getString("discount_basket_items[1].discount_id");
		utils.logit("Second Discount Basket Item ID: " + exp_SecondRDP_discount_basket_item_id + ", Discount ID: " + exp_SecondRDP_discount_id);

		// Extract third discount basket item
		int exp_ThirdRDP_discount_basket_item_id = discountBasketResponseCoupon3.jsonPath().getInt("discount_basket_items[2].discount_basket_item_id");
		String exp_ThirdRDP_discount_id = discountBasketResponseCoupon3.jsonPath().getString("discount_basket_items[2].discount_id");
		utils.logit("Third Discount Basket Item ID: " + exp_ThirdRDP_discount_basket_item_id + ", Discount ID: " + exp_ThirdRDP_discount_id);

		// Create Input Receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};
		// Input Receipt Details in below format:
		// item_name|item_qty|amount|item_type|item_family|item_group|serial_number|item_id
		addDetails.accept("Pizza1", new String[] { "Pizza1", "1", "10", "M", "10", "999", "1", "101" });
		addDetails.accept("Pizza2", new String[] { "Pizza2", "1", "7", "M", "10", "888", "2", "101" });
		addDetails.accept("Coffee1", new String[] { "Coffee1", "1", "4", "M", "10", "889", "3", "201" });

		// Process basket by user1
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSDiscountLookup(dataSet.get("locationkey"), userIDUser1,
						dataSet.get("subAmount"),parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(),ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for processBatchRedemptionOfBasketPOSNewDiscountLookup API for User1");
		utils.logit("Batch redemption processed successfully for User1");

		double actualFirstRedempetion_DiscountedAmount = batchRedemptionProcessResponseUser1.jsonPath()
				.getDouble("selected_discounts[0].discount_amount");
		utils.logit("First redemption discounted amount: " + actualFirstRedempetion_DiscountedAmount);

		int actual_FirstRDP_discount_basket_item_id = batchRedemptionProcessResponseUser1.jsonPath()
				.getInt("selected_discounts[0].discount_basket_item_id");
		utils.logit("First redemption discount basket item ID: " + actual_FirstRDP_discount_basket_item_id);
		
		String actual_FirstRDP_discount_id = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("selected_discounts[0].discount_id");
		utils.logit("First redemption discount ID: " + actual_FirstRDP_discount_id);
		
		
		Assert.assertEquals(actualFirstRedempetion_DiscountedAmount, expectedFirstRedempetion_DiscountedAmount, 
		        "First redemption discount amount does not match expected value");

		Assert.assertEquals(actual_FirstRDP_discount_basket_item_id, exp_FirstRDP_discount_basket_item_id, 
		        "First redemption discount basket item ID does not match expected value");

		Assert.assertEquals(actual_FirstRDP_discount_id, exp_FirstRDP_discount_id, 
		        "First redemption discount ID does not match expected value");
		utils.logit("Verified that redemption 1 data matches expected values.");

		double actualSecondRedempetion_DiscountedAmount = batchRedemptionProcessResponseUser1.jsonPath()
				.getDouble("selected_discounts[1].discount_amount");

		int actual_SecondRDP_discount_basket_item_id = batchRedemptionProcessResponseUser1.jsonPath()
				.getInt("selected_discounts[1].discount_basket_item_id");
		String actual_SecondRDP_discount_id = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("selected_discounts[1].discount_id");
		utils.logit("Second redemption discounted amount: " + actualSecondRedempetion_DiscountedAmount);
		utils.logit("Second redemption discount basket item ID: " + actual_SecondRDP_discount_basket_item_id);
		utils.logit("Second redemption discount ID: " + actual_SecondRDP_discount_id);

		Assert.assertEquals(actualSecondRedempetion_DiscountedAmount, expectedSecondRedempetion_DiscountedAmount, 
		        "Second redemption discount amount does not match expected value");
		Assert.assertEquals(actual_SecondRDP_discount_basket_item_id, exp_SecondRDP_discount_basket_item_id, 
		        "Second redemption discount basket item ID does not match expected value");
		Assert.assertEquals(actual_SecondRDP_discount_id, exp_SecondRDP_discount_id, 
		        "Second redemption discount ID does not match expected value");

		utils.logit("Verified that redemption 2 data matches expected values.");

		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Batch redemption API failed: Unexpected HTTP status code");
		utils.logit("Batch redemption API call succeeded with status 200");

		float discountedAmount1 = Float.parseFloat(
				batchRedemptionProcessResponseUser1.jsonPath().getString("selected_discounts[0].discount_amount"));

		float discountedAmount2 = Float.parseFloat(
				batchRedemptionProcessResponseUser1.jsonPath().getString("selected_discounts[1].discount_amount"));
		utils.logit("Discounted amount for first item: " + discountedAmount1);
		utils.logit("Discounted amount for second item: " + discountedAmount2);
		
		String actualErrorMessage = batchRedemptionProcessResponseUser1.jsonPath().getString("selected_discounts[2].message");
		utils.logit("Actual error message for third discount: " + actualErrorMessage);

		// Calculate total discount and log
		float totalDiscount = discountedAmount1 + discountedAmount2;
		utils.logit("Total discount available for the receipt: " + totalDiscount);

		// Expected total discount from dataset
		float expectedTotalDiscount = Float.parseFloat(dataSet.get("expectedTotalDiscount"));

		// Assertions with messages
		Assert.assertEquals(totalDiscount, expectedTotalDiscount, "Total discount does not match expected value");
		utils.logit("Verified that total discount matches expected value: " + expectedTotalDiscount);

		Assert.assertEquals(actualErrorMessage, dataSet.get("expectedFailureMessage"), "Error message does not match expected value");
		utils.logit("Verified expected error message: " + dataSet.get("expectedFailureMessage"));

	}

	@Test(description = "SQ-T7482 - Step 5: Verify functionality cases for redemption_code", priority=5)
	@Owner(name = "Shashank Sharma")
	public void validateRedempetionCalcutionWithNoQCInCampaignDiscountLookup() throws Exception {
		
		String userEmailUser1 = pageObj.iframeSingUpPage().generateEmail();
		utils.logit("Generated userEmailUser1: " + userEmailUser1);

		// Parse expected redemption discount amounts
		double expectedFirstRedempetion_DiscountedAmount = Double.parseDouble(dataSet.get("expectedFirstRedempetion_DiscountedAmount"));
		utils.logit("Expected First Redemption Discount Amount: " + expectedFirstRedempetion_DiscountedAmount);

		double expectedSecondRedempetion_DiscountedAmount = Double.parseDouble(dataSet.get("expectedSecondRedempetion_DiscountedAmount"));
		utils.logit("Expected Second Redemption Discount Amount: " + expectedSecondRedempetion_DiscountedAmount);

		double expectedThirdRedempetion_DiscountedAmount = Double.parseDouble(dataSet.get("expectedThirdRedempetion_DiscountedAmount"));
		utils.logit("Expected Third Redemption Discount Amount: " + expectedThirdRedempetion_DiscountedAmount);

		// Generate coupon campaign name
		coupanCampaignName = "Auto_CuponCampaign" + CreateDateTime.getTimeDateString();
		utils.logit("Generated coupon campaign name: " + coupanCampaignName);

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(coupanCampaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), "", "",
				GlobalBenefitRedemptionThrottlingToggle);

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Campaign created success message did not display");
		utils.logit("Coupon campaign created successfully");


		pageObj.campaignspage().searchCampaign(coupanCampaignName);
		codeNameList = pageObj.campaignspage().getPreGeneratedCuponCodeList();

		String couponCode1 = codeNameList.get(0);
		String couponCode2 = codeNameList.get(1);
		String couponCode3 = codeNameList.get(2);
		utils.logit("Coupon Codes → 1: " + couponCode1 + ", 2: " + couponCode2 + ", 3: " + couponCode3);

		userEmailUser1 = pageObj.iframeSingUpPage().generateEmail();
		utils.logit("Generated userEmailUser1: " + userEmailUser1);
		Response signUpResponseUser1 = pageObj.endpoints().Api2SignUp(userEmailUser1, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
		        "API 2 user signup failed. Response: " + signUpResponseUser1.asString());
		utils.logit("API 2 user signup successful");

		// Validate required fields in response
		Assert.assertNotNull(signUpResponseUser1.jsonPath().get("access_token.token"),
		        "Access token missing in signup response");
		Assert.assertNotNull(signUpResponseUser1.jsonPath().get("user.user_id"),
		        "User ID missing in signup response");

		// Extract and log values
		String tokenUser1 = signUpResponseUser1.jsonPath().getString("access_token.token");
		utils.logit("User1 access token generated");

		String userIDUser1 = signUpResponseUser1.jsonPath().getString("user.user_id");
		utils.logit("User1 user_id generated: " + userIDUser1);

		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userIDUser1, dataSet.get("apiKey"), "10",
				"", "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
		        "Send reward API failed for userID: " + userIDUser1 + ". Response: " + sendRewardResponse.asString());
		utils.logit("Reward message sent successfully to userID: " + userIDUser1);

		Response discountBasketResponseCoupon1 = pageObj.endpoints().authListDiscountBasketAdded(tokenUser1,
				dataSet.get("client"), dataSet.get("secret"), "redemption_code", couponCode1);
		Assert.assertEquals(discountBasketResponseCoupon1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
		        "Failed to add coupon code to basket: " + couponCode1 + ". Response: " + discountBasketResponseCoupon1.asString());
		utils.logit(couponCode1 + " code is added to basket successfully");

		Response discountBasketResponseCoupon2 = pageObj.endpoints().authListDiscountBasketAdded(tokenUser1,
				dataSet.get("client"), dataSet.get("secret"), "redemption_code", couponCode2);
		Assert.assertEquals(discountBasketResponseCoupon2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
		        "Failed to add coupon code to basket: " + couponCode2 + ". Response: " + discountBasketResponseCoupon2.asString());

		utils.logit(couponCode2 + " code is added to basket successfully");

		Response discountBasketResponseCoupon3 = pageObj.endpoints().authListDiscountBasketAdded(tokenUser1,
				dataSet.get("client"), dataSet.get("secret"), "redemption_code", couponCode3);
		Assert.assertEquals(discountBasketResponseCoupon3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
		        "Failed to add coupon code to basket: " + couponCode3 + ". Response: " + discountBasketResponseCoupon3.asString());
		utils.logit(couponCode3 + " code is added to basket successfully");

		
		int exp_FirstRDP_discount_basket_item_id = discountBasketResponseCoupon1.jsonPath()
		        .getInt("discount_basket_items[0].discount_basket_item_id");
		utils.logit("Expected First RDP discount_basket_item_id: " + exp_FirstRDP_discount_basket_item_id);

		String exp_FirstRDP_discount_id = discountBasketResponseCoupon1.jsonPath()
		        .getString("discount_basket_items[0].discount_id");
		utils.logit("Expected First RDP discount_id: " + exp_FirstRDP_discount_id);

		int exp_SecondRDP_discount_basket_item_id = discountBasketResponseCoupon2.jsonPath()
		        .getInt("discount_basket_items[1].discount_basket_item_id");
		utils.logit("Expected Second RDP discount_basket_item_id: " + exp_SecondRDP_discount_basket_item_id);

		String exp_SecondRDP_discount_id = discountBasketResponseCoupon2.jsonPath()
		        .getString("discount_basket_items[1].discount_id");
		utils.logit("Expected Second RDP discount_id: " + exp_SecondRDP_discount_id);

		int exp_ThirdRDP_discount_basket_item_id = discountBasketResponseCoupon3.jsonPath()
		        .getInt("discount_basket_items[2].discount_basket_item_id");
		utils.logit("Expected Third RDP discount_basket_item_id: " + exp_ThirdRDP_discount_basket_item_id);

		String exp_ThirdRDP_discount_id = discountBasketResponseCoupon3.jsonPath()
		        .getString("discount_basket_items[2].discount_id");
		utils.logit("Expected Third RDP discount_id: " + exp_ThirdRDP_discount_id);
		
		
		// Create Input Receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};
		// Input Receipt Details in below format:
		// item_name|item_qty|amount|item_type|item_family|item_group|serial_number|item_id
		addDetails.accept("Pizza1", new String[] { "Pizza1", "1", "10", "M", "10", "999", "1", "101" });
		addDetails.accept("Pizza2", new String[] { "Pizza2", "1", "7", "M", "10", "888", "2", "101" });
		addDetails.accept("Coffee1", new String[] { "Coffee1", "1", "4", "M", "10", "889", "3", "201" });

		// Process basket by user1
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSDiscountLookup(dataSet.get("locationkey"), userIDUser1,
						dataSet.get("subAmount"),parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(),
		        ApiConstants.HTTP_STATUS_OK,
		        "Batch redemption API failed for User1. Response: "
		                + batchRedemptionProcessResponseUser1.asString());
		utils.logit("Batch redemption API executed successfully for User1");

		double actualFirstRedempetion_DiscountedAmount = batchRedemptionProcessResponseUser1.jsonPath()
				.getDouble("selected_discounts[0].discount_amount");
		utils.logit("Actual First Redemption Discount Amount: " + actualFirstRedempetion_DiscountedAmount);

		int actual_FirstRDP_discount_basket_item_id = batchRedemptionProcessResponseUser1.jsonPath()
		        .getInt("selected_discounts[0].discount_basket_item_id");
		utils.logit("Actual First RDP discount_basket_item_id: " + actual_FirstRDP_discount_basket_item_id);

		String actual_FirstRDP_discount_id = batchRedemptionProcessResponseUser1.jsonPath()
		        .getString("selected_discounts[0].discount_id");
		utils.logit("Actual First RDP discount_id: " + actual_FirstRDP_discount_id);
		
		Assert.assertEquals(actualFirstRedempetion_DiscountedAmount, expectedFirstRedempetion_DiscountedAmount, "First redemption discount amount mismatch"); 
		Assert.assertEquals(actual_FirstRDP_discount_basket_item_id, exp_FirstRDP_discount_basket_item_id, "First redemption basket item ID mismatch"); 
		Assert.assertEquals(actual_FirstRDP_discount_id, exp_FirstRDP_discount_id, "First redemption discount ID mismatch"); 
		utils.logit("Verified that redemption 1 data");
		
		double actualSecondRedempetion_DiscountedAmount = batchRedemptionProcessResponseUser1.jsonPath()
				.getDouble("selected_discounts[1].discount_amount");
		utils.logit("Actual Second Redemption Discount Amount: " + actualSecondRedempetion_DiscountedAmount);

		int actual_SecondRDP_discount_basket_item_id = batchRedemptionProcessResponseUser1.jsonPath()
				.getInt("selected_discounts[1].discount_basket_item_id");
		utils.logit("Actual Second RDP discount_basket_item_id: " + actual_SecondRDP_discount_basket_item_id);

		String actual_SecondRDP_discount_id = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("selected_discounts[1].discount_id");
		utils.logit("Actual Second RDP discount_id: " + actual_SecondRDP_discount_id);

		Assert.assertEquals(actualSecondRedempetion_DiscountedAmount, expectedSecondRedempetion_DiscountedAmount,
				"Second redemption discount amount mismatch");
		Assert.assertEquals(actual_SecondRDP_discount_basket_item_id, exp_SecondRDP_discount_basket_item_id,
				"Second redemption basket item ID mismatch");
		Assert.assertEquals(actual_SecondRDP_discount_id, exp_SecondRDP_discount_id,
				"Second redemption discount ID mismatch");

		utils.logit("Verified that redemption 2 data");

		double actualThirdRedempetion_DiscountedAmount = batchRedemptionProcessResponseUser1.jsonPath()
				.getDouble("selected_discounts[2].discount_amount");
		utils.logit("Actual Third Redemption Discount Amount: " + actualThirdRedempetion_DiscountedAmount);

		int actual_ThirdRDP_discount_basket_item_id = batchRedemptionProcessResponseUser1.jsonPath()
				.getInt("selected_discounts[2].discount_basket_item_id");
		utils.logit("Actual Third RDP discount_basket_item_id: " + actual_ThirdRDP_discount_basket_item_id);

		String actual_ThirdRDP_discount_id = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("selected_discounts[2].discount_id");
		utils.logit("Actual Third RDP discount_id: " + actual_ThirdRDP_discount_id);

		Assert.assertEquals(actualThirdRedempetion_DiscountedAmount, expectedThirdRedempetion_DiscountedAmount,
				"Third redemption discount amount mismatch");
		Assert.assertEquals(actual_ThirdRDP_discount_basket_item_id, exp_ThirdRDP_discount_basket_item_id,
				"Third redemption basket item ID mismatch");
		Assert.assertEquals(actual_ThirdRDP_discount_id, exp_ThirdRDP_discount_id,
				"Third redemption discount ID mismatch");

		utils.logit("Verified that redemption 3 data");
		utils.logit(userIDUser1 + " processed the basket");

		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Batch redemption API status code mismatch for User1");

		float discountedAmount1 = Float.parseFloat(batchRedemptionProcessResponseUser1.jsonPath().getString("selected_discounts[0].discount_amount"));
		utils.logit("Discounted Amount 1: " + discountedAmount1);

		float discountedAmount2 = Float.parseFloat(batchRedemptionProcessResponseUser1.jsonPath().getString("selected_discounts[1].discount_amount"));
		utils.logit("Discounted Amount 2: " + discountedAmount2);

		float discountedAmount3 = Float.parseFloat(batchRedemptionProcessResponseUser1.jsonPath().getString("selected_discounts[2].discount_amount"));
		utils.logit("Discounted Amount 3: " + discountedAmount3);

		float totalDiscount = discountedAmount1 + discountedAmount2 + discountedAmount3;
		utils.logit("Total discount available for the receipt: " + totalDiscount);

		float expectedTotalDiscount = Float.parseFloat(dataSet.get("expectedTotalDiscount"));
		utils.logit("Expected total discount: " + expectedTotalDiscount);

		Assert.assertEquals(totalDiscount, expectedTotalDiscount, "Total discount amount for the receipt mismatch");
		utils.logit("Verified the discounted amount for the receipt");

	}

	@Test(description = "SQ-T7482 - Step 6: Verify functionality cases for redemption_code", priority=6)
	@Owner(name = "Shashank Sharma")
	public void validateRedemptionCalculationWithDollar1Configuration() throws Exception {
		
		
		String userEmailUser1 = pageObj.iframeSingUpPage().generateEmail();
		utils.logit("Generated user email: " + userEmailUser1);


		Response signUpResponseUser1 = pageObj.endpoints().Api2SignUp(userEmailUser1, dataSet.get("client"),dataSet.get("secret"));
		Assert.assertEquals(signUpResponseUser1.getStatusCode(),ApiConstants.HTTP_STATUS_OK,"User signup failed");
		String tokenUser1 = signUpResponseUser1.jsonPath().get("access_token.token").toString();
		Assert.assertNotNull(tokenUser1, "Access token is null after signup");
		utils.logit("User token generated successfully");


		String userIDUser1 = signUpResponseUser1.jsonPath().get("user.user_id").toString();
		Assert.assertNotNull(userIDUser1, "User ID is null after signup");
		utils.logit("User ID generated: " + userIDUser1);

		double expectedFirstRedempetion_DiscountedAmount = Double.parseDouble(dataSet.get("expectedFirstRedempetion_DiscountedAmount"));
		utils.logit("Expected First Redemption Discounted Amount: "+ expectedFirstRedempetion_DiscountedAmount);
		double expectedSecondRedempetion_DiscountedAmount = Double.parseDouble(dataSet.get("expectedSecondRedempetion_DiscountedAmount"));
		utils.logit("Expected Second Redemption Discounted Amount: "+ expectedSecondRedempetion_DiscountedAmount);
		double expectedThirdRedempetion_DiscountedAmount = Double.parseDouble(dataSet.get("expectedThirdRedempetion_DiscountedAmount"));
		utils.logit("Expected Third Redemption Discounted Amount: "+ expectedThirdRedempetion_DiscountedAmount);

		coupanCampaignName = "Auto_CouponCampaign" + CreateDateTime.getTimeDateString();
		utils.logit("coupanCampaignName :: " + coupanCampaignName);

		// =====================  Create LIS =====================
		String lisName = "Automation_Redemption_LIS_" + Utilities.getTimestamp();
		utils.logit("Creating LIS: " + lisName);
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName).setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "101").build();
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(),ApiConstants.HTTP_STATUS_OK, "LIS creation API failed");
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalID, "LIS external_id is null");
		utils.logit("LIS created with External ID: " + lisExternalID);


		// =====================️ Create QC =====================
		String qcname = "AutomationQC_MaxQty_" + Utilities.getTimestamp();
		utils.logit("Creating QC: " + qcname);
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(10).setQCProcessingFunction("sum_amounts").setStackDiscounting(true)
				.setReuseQualifyingItems(true)
				.addLineItemFilter(lisExternalID, "max_price", 2)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(),ApiConstants.HTTP_STATUS_OK, "QC creation API failed");

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(coupanCampaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), "",
				qcname, GlobalBenefitRedemptionThrottlingToggle);
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		pageObj.campaignspage().searchCampaign(coupanCampaignName);
		codeNameList = pageObj.campaignspage().getPreGeneratedCuponCodeList();

		String couponCode1 = codeNameList.get(0);
		String couponCode2 = codeNameList.get(1);
		String couponCode3 = codeNameList.get(2);
		utils.logit("Coupon Codes → 1: " + couponCode1 + ", 2: " + couponCode2 + ", 3: " + couponCode3);

		Response discountBasketResponseCoupon1 = pageObj.endpoints().authListDiscountBasketAdded(tokenUser1,
				dataSet.get("client"), dataSet.get("secret"), "redemption_code", couponCode1);
		Assert.assertEquals(discountBasketResponseCoupon1.getStatusCode(),ApiConstants.HTTP_STATUS_OK,"Status code 200 did not match while adding coupon code: " + couponCode1);
		utils.logit(couponCode1 + " code is added to basket");

		Response discountBasketResponseCoupon2 = pageObj.endpoints().authListDiscountBasketAdded(tokenUser1,
				dataSet.get("client"), dataSet.get("secret"), "redemption_code", couponCode2);
		Assert.assertEquals(discountBasketResponseCoupon2.getStatusCode(),ApiConstants.HTTP_STATUS_OK,"Status code 200 did not match while adding coupon code: " + couponCode2);
		utils.logit(couponCode2 + " code is added to basket");

		Response discountBasketResponseCoupon3 = pageObj.endpoints().authListDiscountBasketAdded(tokenUser1,
				dataSet.get("client"), dataSet.get("secret"), "redemption_code", couponCode3);
		Assert.assertEquals(discountBasketResponseCoupon3.getStatusCode(),ApiConstants.HTTP_STATUS_OK,"Status code 200 did not match while adding coupon code: " + couponCode3);
		utils.logit(couponCode3 + " code is added to basket");


		int exp_FirstRDP_discount_basket_item_id = discountBasketResponseCoupon1.jsonPath().getInt("discount_basket_items[0].discount_basket_item_id");
		utils.logit("Expected First RDP Discount Basket Item ID: " + exp_FirstRDP_discount_basket_item_id);
		String exp_FirstRDP_discount_id = discountBasketResponseCoupon1.jsonPath().getString("discount_basket_items[0].discount_id");
		utils.logit("Expected First RDP Discount ID: " + exp_FirstRDP_discount_id);

		int exp_SecondRDP_discount_basket_item_id = discountBasketResponseCoupon2.jsonPath().getInt("discount_basket_items[1].discount_basket_item_id");
		utils.logit("Expected Second RDP Discount Basket Item ID: " + exp_SecondRDP_discount_basket_item_id);
		String exp_SecondRDP_discount_id = discountBasketResponseCoupon2.jsonPath().getString("discount_basket_items[1].discount_id");
		utils.logit("Expected Second RDP Discount ID: " + exp_SecondRDP_discount_id);

		int exp_ThirdRDP_discount_basket_item_id = discountBasketResponseCoupon3.jsonPath().getInt("discount_basket_items[2].discount_basket_item_id");
		utils.logit("Expected Third RDP Discount Basket Item ID: " + exp_ThirdRDP_discount_basket_item_id);
		String exp_ThirdRDP_discount_id = discountBasketResponseCoupon3.jsonPath().getString("discount_basket_items[2].discount_id");
		utils.logit("Expected Third RDP Discount ID: " + exp_ThirdRDP_discount_id);

		// Create Input Receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};
		// Input Receipt Details in below format:
		// item_name|item_qty|amount|item_type|item_family|item_group|serial_number|item_id
		addDetails.accept("Pizza1", new String[] { "Pizza1", "1", "8", "M", "10", "999", "1", "101" });
		addDetails.accept("Pizza2", new String[] { "Pizza2", "1", "2", "M", "10", "888", "2", "101" });
		addDetails.accept("Pizza3", new String[] { "Pizza3", "1", "4", "M", "10", "889", "3", "201" });
				
		// Process basket by user1
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSDiscountLookup(dataSet.get("locationkey"), userIDUser1,
						dataSet.get("subAmount"),parentMap);
		
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(),ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for processBatchRedemptionOfBasketPOSNewDiscountLookup API for User1");
		utils.logit("Batch redemption processed successfully for User1");


		double actualFirstRedempetion_DiscountedAmount = batchRedemptionProcessResponseUser1.jsonPath()
				.getDouble("selected_discounts[0].discount_amount");
		utils.logit("First Redemption Discount Amount: " + actualFirstRedempetion_DiscountedAmount);

		int actual_FirstRDP_discount_basket_item_id = batchRedemptionProcessResponseUser1.jsonPath()
				.getInt("selected_discounts[0].discount_basket_item_id");
		utils.logit("First Redemption Discount Basket Item ID: " + actual_FirstRDP_discount_basket_item_id);

		String actual_FirstRDP_discount_id = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("selected_discounts[0].discount_id");
		utils.logit("First Redemption Discount ID: " + actual_FirstRDP_discount_id);

		Assert.assertEquals(actualFirstRedempetion_DiscountedAmount, expectedFirstRedempetion_DiscountedAmount);
		Assert.assertEquals(actual_FirstRDP_discount_basket_item_id, exp_FirstRDP_discount_basket_item_id);
		Assert.assertEquals(actual_FirstRDP_discount_id, exp_FirstRDP_discount_id);
		utils.logit("Verified that redemption 1 data ");

		double actualSecondRedempetion_DiscountedAmount = batchRedemptionProcessResponseUser1.jsonPath()
				.getDouble("selected_discounts[1].discount_amount");
		utils.logit("Second Redemption Discount Amount: " + actualSecondRedempetion_DiscountedAmount);
		int actual_SecondRDP_discount_basket_item_id = batchRedemptionProcessResponseUser1.jsonPath()
				.getInt("selected_discounts[1].discount_basket_item_id");
		utils.logit("Second Redemption Discount Basket Item ID: " + actual_SecondRDP_discount_basket_item_id);
		String actual_SecondRDP_discount_id = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("selected_discounts[1].discount_id");
		utils.logit("Second Redemption Discount ID: " + actual_SecondRDP_discount_id);

		Assert.assertEquals(actualSecondRedempetion_DiscountedAmount, expectedSecondRedempetion_DiscountedAmount);
		Assert.assertEquals(actual_SecondRDP_discount_basket_item_id, exp_SecondRDP_discount_basket_item_id);
		Assert.assertEquals(actual_SecondRDP_discount_id, exp_SecondRDP_discount_id);
		utils.logit("Verified that redemption 2 data ");

		double actualThirdRedempetion_DiscountedAmount = batchRedemptionProcessResponseUser1.jsonPath()
				.getDouble("selected_discounts[2].discount_amount");
		utils.logit("Third Redemption Discount Amount: " + actualThirdRedempetion_DiscountedAmount);
		int actual_ThirdRDP_discount_basket_item_id = batchRedemptionProcessResponseUser1.jsonPath()
				.getInt("selected_discounts[2].discount_basket_item_id");
		utils.logit("Third Redemption Discount Basket Item ID: " + actual_ThirdRDP_discount_basket_item_id);

		String actual_ThirdRDP_discount_id = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("selected_discounts[2].discount_id");
		utils.logit("Third Redemption Discount ID: " + actual_ThirdRDP_discount_id);

		Assert.assertEquals(actualThirdRedempetion_DiscountedAmount, expectedThirdRedempetion_DiscountedAmount);
		Assert.assertEquals(actual_ThirdRDP_discount_basket_item_id, exp_ThirdRDP_discount_basket_item_id);
		Assert.assertEquals(actual_ThirdRDP_discount_id, exp_ThirdRDP_discount_id);
		utils.logit("Verified that redemption 3 data ");
		utils.logit(userIDUser1 + " process the basket ");

		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		float discountedAmount1 = Float.parseFloat(
				batchRedemptionProcessResponseUser1.jsonPath().getString("selected_discounts[0].discount_amount"));
		utils.logit("Discounted Amount 1: " + discountedAmount1);

		float discountedAmount2 = Float.parseFloat(
				batchRedemptionProcessResponseUser1.jsonPath().getString("selected_discounts[1].discount_amount"));
		utils.logit("Discounted Amount 2: " + discountedAmount2);

		float discountedAmount3 = Float.parseFloat(
				batchRedemptionProcessResponseUser1.jsonPath().getString("selected_discounts[2].discount_amount"));
		utils.logit("Discounted Amount 3: " + discountedAmount3);

		float totalDiscount = discountedAmount1 + discountedAmount2 + discountedAmount3;
		utils.logit("Total discount available for the receipt: " + totalDiscount);

		float expectedTotalDiscount = Float.parseFloat(dataSet.get("expectedTotalDiscount"));
		utils.logit("Expected total discount: " + expectedTotalDiscount);

		Assert.assertEquals(totalDiscount, expectedTotalDiscount);
		utils.logit("Verified the discounted amount for the reciept ");

	}

	@Test(description = "SQ-T7482 - Step 7: Verify functionality cases for redemption_code", priority=7)
	@Owner(name = "Shashank Sharma")
	public void validateRedemptionCalculationWithDollar1WithNoQC() throws Exception {
		
		String userEmailUser1 = pageObj.iframeSingUpPage().generateEmail();

		Response signUpResponseUser1 = pageObj.endpoints().Api2SignUp(userEmailUser1, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponseUser1.getStatusCode(),ApiConstants.HTTP_STATUS_OK,"User signup failed");
		String tokenUser1 = signUpResponseUser1.jsonPath().get("access_token.token").toString();
		utils.logit("tokenUser =" + tokenUser1);

		String userIDUser1 = signUpResponseUser1.jsonPath().get("user.user_id").toString();
		utils.logit("userIDUser =" + userIDUser1);

		double expectedFirstRedempetion_DiscountedAmount = Double
				.parseDouble(dataSet.get("expectedFirstRedempetion_DiscountedAmount"));
		utils.logit("Expected First Redemption Discount Amount: " + expectedFirstRedempetion_DiscountedAmount);
		double expectedSecondRedempetion_DiscountedAmount = Double
				.parseDouble(dataSet.get("expectedSecondRedempetion_DiscountedAmount"));
		utils.logit("Expected Second Redemption Discount Amount: " + expectedSecondRedempetion_DiscountedAmount);
		double expectedThirdRedempetion_DiscountedAmount = Double
				.parseDouble(dataSet.get("expectedThirdRedempetion_DiscountedAmount"));
		utils.logit("Expected Third Redemption Discount Amount: " + expectedThirdRedempetion_DiscountedAmount);

		coupanCampaignName = "Auto_CuponCampaign" + CreateDateTime.getTimeDateString();
		utils.logit("coupanCampaignName == " + coupanCampaignName);

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(coupanCampaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), "", "",
				GlobalBenefitRedemptionThrottlingToggle);
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		pageObj.campaignspage().searchCampaign(coupanCampaignName);
		codeNameList = pageObj.campaignspage().getPreGeneratedCuponCodeList();

		String couponCode1 = codeNameList.get(0);
		String couponCode2 = codeNameList.get(1);
		String couponCode3 = codeNameList.get(2);
		utils.logit("Coupon Codes → 1: " + couponCode1 + ", 2: " + couponCode2 + ", 3: " + couponCode3);

		Response discountBasketResponseCoupon1 = pageObj.endpoints().authListDiscountBasketAdded(tokenUser1,
				dataSet.get("client"), dataSet.get("secret"), "redemption_code", couponCode1);
		Assert.assertEquals(discountBasketResponseCoupon1.getStatusCode(),ApiConstants.HTTP_STATUS_OK,"Status code 200 did not match while adding coupon code: " + couponCode1);
		utils.logit(couponCode1 + " code is added to basket");

		Response discountBasketResponseCoupon2 = pageObj.endpoints().authListDiscountBasketAdded(tokenUser1,
				dataSet.get("client"), dataSet.get("secret"), "redemption_code", couponCode2);
		Assert.assertEquals(discountBasketResponseCoupon2.getStatusCode(),ApiConstants.HTTP_STATUS_OK,"Status code 200 did not match while adding coupon code: " + couponCode2);
		utils.logit(couponCode2 + " code is added to basket");

		Response discountBasketResponseCoupon3 = pageObj.endpoints().authListDiscountBasketAdded(tokenUser1,
				dataSet.get("client"), dataSet.get("secret"), "redemption_code", couponCode3);
		Assert.assertEquals(discountBasketResponseCoupon3.getStatusCode(),ApiConstants.HTTP_STATUS_OK,"Status code 200 did not match while adding coupon code: " + couponCode3);
		utils.logit(couponCode3 + " code is added to basket");

		int exp_FirstRDP_discount_basket_item_id = discountBasketResponseCoupon1.jsonPath().getInt("discount_basket_items[0].discount_basket_item_id"); 
		utils.logit("Expected First RDP discount_basket_item_id: " + exp_FirstRDP_discount_basket_item_id);

		String exp_FirstRDP_discount_id = discountBasketResponseCoupon1.jsonPath().getString("discount_basket_items[0].discount_id"); 
		utils.logit("Expected First RDP discount_id: " + exp_FirstRDP_discount_id);

		int exp_SecondRDP_discount_basket_item_id = discountBasketResponseCoupon2.jsonPath().getInt("discount_basket_items[1].discount_basket_item_id"); 
		utils.logit("Expected Second RDP discount_basket_item_id: " + exp_SecondRDP_discount_basket_item_id);

		String exp_SecondRDP_discount_id = discountBasketResponseCoupon2.jsonPath().getString("discount_basket_items[1].discount_id"); 
		utils.logit("Expected Second RDP discount_id: " + exp_SecondRDP_discount_id);

		int exp_ThirdRDP_discount_basket_item_id = discountBasketResponseCoupon3.jsonPath().getInt("discount_basket_items[2].discount_basket_item_id"); 
		utils.logit("Expected Third RDP discount_basket_item_id: " + exp_ThirdRDP_discount_basket_item_id);

		String exp_ThirdRDP_discount_id = discountBasketResponseCoupon3.jsonPath().getString("discount_basket_items[2].discount_id"); 
		utils.logit("Expected Third RDP discount_id: " + exp_ThirdRDP_discount_id);

		
		// Create Input Receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};
		// Input Receipt Details in below format:
		// item_name|item_qty|amount|item_type|item_family|item_group|serial_number|item_id
		addDetails.accept("Pizza1", new String[] { "Pizza1", "1", "8", "M", "10", "999", "1", "101" });
		addDetails.accept("Pizza2", new String[] { "Pizza2", "1", "2", "M", "10", "888", "2", "101" });
		addDetails.accept("Pizza3", new String[] { "Pizza3", "1", "4", "M", "10", "889", "3", "201" });

		// Process basket by user1
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationkey"), userIDUser1,
						dataSet.get("subAmount"),parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(),ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for processBatchRedemptionOfBasketPOSNewDiscountLookup API for User1");
		utils.logit("Batch redemption processed successfully for User1");


		double actualFirstRedempetion_DiscountedAmount = batchRedemptionProcessResponseUser1.jsonPath()
				.getDouble("success[0].discount_amount");
		utils.logit("First Redemption Discount Amount: " + actualFirstRedempetion_DiscountedAmount);

		int actual_FirstRDP_discount_basket_item_id = batchRedemptionProcessResponseUser1.jsonPath()
				.getInt("success[0].discount_basket_item_id");
		utils.logit("First Redemption Discount Basket Item ID: " + actual_FirstRDP_discount_basket_item_id);

		String actual_FirstRDP_discount_id = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_id");
		utils.logit("First Redemption Discount ID: " + actual_FirstRDP_discount_id);

		Assert.assertEquals(actualFirstRedempetion_DiscountedAmount, expectedFirstRedempetion_DiscountedAmount);
		Assert.assertEquals(actual_FirstRDP_discount_basket_item_id, exp_FirstRDP_discount_basket_item_id);
		Assert.assertEquals(actual_FirstRDP_discount_id, exp_FirstRDP_discount_id);
		utils.logit("Verified that redemption 1 data ");

		double actualSecondRedempetion_DiscountedAmount = batchRedemptionProcessResponseUser1.jsonPath()
				.getDouble("success[1].discount_amount");
		utils.logit("Second Redemption Discount Amount: " + actualSecondRedempetion_DiscountedAmount);
		int actual_SecondRDP_discount_basket_item_id = batchRedemptionProcessResponseUser1.jsonPath()
				.getInt("success[1].discount_basket_item_id");
		utils.logit("Second Redemption Discount Basket Item ID: " + actual_SecondRDP_discount_basket_item_id);
		String actual_SecondRDP_discount_id = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[1].discount_id");
		utils.logit("Second Redemption Discount ID: " + actual_SecondRDP_discount_id);

		Assert.assertEquals(actualSecondRedempetion_DiscountedAmount, expectedSecondRedempetion_DiscountedAmount);
		Assert.assertEquals(actual_SecondRDP_discount_basket_item_id, exp_SecondRDP_discount_basket_item_id);
		Assert.assertEquals(actual_SecondRDP_discount_id, exp_SecondRDP_discount_id);
		utils.logit("Verified that redemption 2 data ");

		double actualThirdRedempetion_DiscountedAmount = batchRedemptionProcessResponseUser1.jsonPath()
				.getDouble("success[2].discount_amount");
		utils.logit("Third Redemption Discount Amount: " + actualThirdRedempetion_DiscountedAmount);

		int actual_ThirdRDP_discount_basket_item_id = batchRedemptionProcessResponseUser1.jsonPath()
				.getInt("success[2].discount_basket_item_id");
		utils.logit("Third Redemption Discount Basket Item ID: " + actual_ThirdRDP_discount_basket_item_id);

		String actual_ThirdRDP_discount_id = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[2].discount_id");
		utils.logit("Third Redemption Discount ID: " + actual_ThirdRDP_discount_id);

		Assert.assertEquals(actualThirdRedempetion_DiscountedAmount, expectedThirdRedempetion_DiscountedAmount);
		Assert.assertEquals(actual_ThirdRDP_discount_basket_item_id, exp_ThirdRDP_discount_basket_item_id);
		Assert.assertEquals(actual_ThirdRDP_discount_id, exp_ThirdRDP_discount_id);
		utils.logit("Verified that redemption 3 data ");
		utils.logit(userIDUser1 + " process the basket ");

		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		float discountedAmount1 = Float
				.parseFloat(batchRedemptionProcessResponseUser1.jsonPath().getString("success[0].discount_amount"));
		utils.logit("Discounted Amount 1: " + discountedAmount1);

		float discountedAmount2 = Float
				.parseFloat(batchRedemptionProcessResponseUser1.jsonPath().getString("success[1].discount_amount"));
		utils.logit("Discounted Amount 2: " + discountedAmount2);

		float discountedAmount3 = Float
				.parseFloat(batchRedemptionProcessResponseUser1.jsonPath().getString("success[2].discount_amount"));
		utils.logit("Discounted Amount 3: " + discountedAmount3);

		float totalDiscount = discountedAmount1 + discountedAmount2 + discountedAmount3;
		utils.logit("Total discount available for the receipt: " + totalDiscount);

		float expectedTotalDiscount = Float.parseFloat(dataSet.get("expectedTotalDiscount"));
		utils.logit("Expected total discount: " + expectedTotalDiscount);

		Assert.assertEquals(totalDiscount, expectedTotalDiscount);
		utils.logit("Verified the discounted amount for the reciept ");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);
		if (coupanCampaignName != null && !coupanCampaignName.isEmpty()) {
		    pageObj.utils().deleteCampaignFromDb(coupanCampaignName, env);
		}
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		utils.logit("Browser closed");
	}
}
