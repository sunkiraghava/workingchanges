package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
import com.punchh.server.apiConfig.ApiPayloads;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.MessagesConstants;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class RewardLockingFeatureUpdate {

	private static Logger logger = LogManager.getLogger(RewardLockingFeatureUpdate.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	String rewardId = "";
	String rewardId1 = "";
	String rewardId2 = "";
	String query, expColValue = "";
	String externalUID;
	Properties prop;
	String getRedeemableID = "Select id from redeemables where uuid ='$actualExternalIdRedeemable'";
	String getExternalUid = "Select external_uid from discount_baskets where user_id ='$userID'";
	String getLockedAt = "Select locked_at from discount_baskets where user_id ='$userID'";
	String deleteRedeemableQuery = "delete from redeemables where uuid ='${redeemableExternalID}' and business_id ='${businessID}'";

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) throws Exception {

		prop = Utilities.loadPropertiesFile("segmentBeta.properties");
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
		externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

	}

	@Test(description = "OMM-T4266 MOBILE SELECT API >> Verify external_uid and locked_at are not saved in DB after calling SELECT API. || "
			+ "OMM-T4267 MOBILE GET API >> Verify external_uid and locked_at are not saved in DB after calling MOBILE GET API. ||"
			+ "OMM-T4268 MOBILE UNSELECT API >> Verify external_uid and locked_at are not saved in DB after calling MOBILE UNSELECT API", priority = 1)
	@Owner(name = "Rahul Garg")

	public void T4266_T4267_T4268_mobileApiDoesNotPersistExternalUidOrLockedAt() throws Exception {

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.dashboardpage().updateCheckBox();

		// Create Redeemable
		String redeemableName = "AutomationRedeemableExistingQC_API_" + CreateDateTime.getTimeDateString();
		String endTime = CreateDateTime.getFutureDateTimeInGivenFormate(1, "yyyy-MM-dd'T'HH:mm:ss");

		Map<String, String> redeemableMap = new HashMap<String, String>();
		redeemableMap.put("redeemableName", redeemableName);
		redeemableMap.put("external_id", "");
		redeemableMap.put("locationID", null);
		redeemableMap.put("external_id_redeemable", "");
		redeemableMap.put("qualifier_type", "flat_discount");
		redeemableMap.put("discount_amount", "10.0");
		redeemableMap.put("end_time", endTime);
		redeemableMap.put("lineitemSelector", dataSet.get("lineItemSelector"));

		// Step2 Qualifier type is provided but invalid redeeming_criterion
		Response responsRedeemable = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), redeemableMap);
		Assert.assertEquals(responsRedeemable.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actualExternalIdRedeemable = responsRedeemable.jsonPath().getString("results[0].external_id")
				.replace("[", "").replace("]", "");
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", actualExternalIdRedeemable);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		pageObj.utils().logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		pageObj.utils().logPass("API v2 User Signup call is successful");

		// Send reward to user
		pageObj.utils().logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		pageObj.utils().logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		pageObj.utils().logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		pageObj.utils().logPass("Reward Id for user is fetched: " + rewardId);

		// api/mobile Api-> Add Reward into discount basket
		Response discountBasketResponse = pageObj.endpoints().secureApiDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		logger.info("Reward added into discount basket");

		// Get external_uid value from DB
		String external_uid = DBUtils.executeQueryAndGetColumnValue(env, getExternalUid.replace("$userID", userId),
				"external_uid");
		Assert.assertEquals(external_uid, null, "No Value is present at external_uid column in discount basket ");
		logger.info("NULL value for external_uid column in discount basket after SELECT API");

		// Get locked_at value from DB
		String locked_at = DBUtils.executeQueryAndGetColumnValue(env, getLockedAt.replace("$userID", userId),
				"locked_at");
		Assert.assertEquals(locked_at, null, "No Value is present at locked_at column in discount basket ");
		logger.info("NULL value for locked_at column in discount basket after Select API");

		pageObj.utils().logPass(
				"Verified that Reward item should get added to basket successfully using mobile API but external_uid and locked_at should not get saved in discount_baskets table for corresponding user.");

		// GET ACTIVE DISCOUNT BASKET
		Response getBasketDiscountDetailsResponse = pageObj.endpoints().getDiscountBasketByUIDUsingApiMobile(token,
				dataSet.get("client"), dataSet.get("secret"), externalUID);

		Assert.assertEquals(getBasketDiscountDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API Mobile Get Active Basket");
		String locked = getBasketDiscountDetailsResponse.jsonPath().get("locked").toString();
		Assert.assertEquals(locked, "false", "locked value is missing from API response");
		logger.info("locked value-FALSE is present in GET API response");

		// Get external_uid value from DB
		String externalUid = DBUtils.executeQueryAndGetColumnValue(env, getExternalUid.replace("$userID", userId),
				"external_uid");
		Assert.assertEquals(externalUid, null, "No Value is present at external_uid column in discount basket ");
		logger.info("NULL value for external_uid column in discount basket after GET active API");

		// Get locked_at value from DB
		String lockedAt = DBUtils.executeQueryAndGetColumnValue(env, getLockedAt.replace("$userID", userId),
				"locked_at");
		Assert.assertEquals(lockedAt, null, "No Value is present at locked_at column in discount basket ");
		logger.info("NULL value for locked_at column in discount basket after GET API");

		pageObj.utils().logPass(
				"Verified that items present in basket should get fetched successfully using mobile API but external_uid and locked_at should not get saved in discount_baskets table for corresponding user.");

		String actualDiscountBasketItemIDFromBasket = getBasketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");
		String actualDiscountIDFromBasket = getBasketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "");

		Assert.assertEquals(actualDiscountIDFromBasket, rewardId);

		// DELETE Items from DISCOUNT BASKET

		Response deleteBasketResponse = pageObj.endpoints().deleteMobileDiscountBasketByUID(token,
				dataSet.get("client"), dataSet.get("secret"), actualDiscountBasketItemIDFromBasket, externalUID);

		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		String lockedUnselect = deleteBasketResponse.jsonPath().get("locked").toString();
		Assert.assertEquals(lockedUnselect, "false", "locked value is missing from API response");
		logger.info("locked value-FALSE is present in API response");

		// Get external_uid value from DB
		String externalUidUnselectDB = DBUtils.executeQueryAndGetColumnValue(env,
				getExternalUid.replace("$userID", userId), "external_uid");
		Assert.assertEquals(externalUidUnselectDB, null,
				"No Value is present at external_uid column in discount basket ");
		logger.info("NULL value for external_uid column in discount basket after DELETE API");

		// Get locked_at value from DB
		String lockedAtUnselectDB = DBUtils.executeQueryAndGetColumnValue(env, getLockedAt.replace("$userID", userId),
				"locked_at");
		Assert.assertEquals(lockedAtUnselectDB, null, "No Value is present at locked_at column in discount basket ");
		logger.info("NULL value for locked_at column in discount basket after DELETE API");

		pageObj.utils().logPass(
				"Verified that Reward item should get removed from basket successfully using API mobile but external_uid and locked_at should not get saved in discount_baskets table for corresponding user.");

		// Delete from redeemable tables

		String deleteRedeemableQuery1 = deleteRedeemableQuery
				.replace("${redeemableExternalID}", actualExternalIdRedeemable)
				.replace("${businessID}", dataSet.get("business_id"));

		DBUtils.executeQuery(env, deleteRedeemableQuery1);

		pageObj.utils().logPass(actualExternalIdRedeemable + " external redeemable is deleted successfully");

	}

	@Test(description = "OMM-T4344 (1.0) API2/MOBILE SELECT API >> Verify external_uid and locked_at are not saved in DB after calling SELECT API. ||"
			+ "OMM-T4346 (1.0) API2/MOBILE GET API >> Verify external_uid and locked_at are not saved in DB after calling API2/MOBILE GET API.||"
			+ "OMM-T4345 (1.0) API2/MOBILE UNSELECT API >> Verify external_uid and locked_at are not saved in DB after calling API@/MOBILE UNSELECT API.", priority = 1)
	@Owner(name = "Rahul Garg")
	public void T4344_T4346_T4345_mobile2ApiDoesNotPersistExternalUidOrLockedAt() throws Exception {

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.dashboardpage().updateCheckBox();

		// Create Redeemable
		String redeemableName = "AutomationRedeemableExistingQC_API_" + CreateDateTime.getTimeDateString();
		String endTime = CreateDateTime.getFutureDateTimeInGivenFormate(1, "yyyy-MM-dd'T'HH:mm:ss");

		Map<String, String> redeemableMap = new HashMap<String, String>();
		redeemableMap.put("redeemableName", redeemableName);
		redeemableMap.put("external_id", "");
		redeemableMap.put("locationID", null);
		redeemableMap.put("external_id_redeemable", "");
		redeemableMap.put("qualifier_type", "flat_discount");
		redeemableMap.put("discount_amount", "10.0");
		redeemableMap.put("end_time", endTime);
		redeemableMap.put("lineitemSelector", dataSet.get("lineItemSelector"));

		// Step2 Qualifier type is provided but invalid redeeming_criterion
		Response responsRedeemable = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), redeemableMap);
		Assert.assertEquals(responsRedeemable.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actualExternalIdRedeemable = responsRedeemable.jsonPath().getString("results[0].external_id")
				.replace("[", "").replace("]", "");
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", actualExternalIdRedeemable);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		pageObj.utils().logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		pageObj.utils().logPass("API v2 User Signup call is successful");

		// Send reward to user
		pageObj.utils().logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		pageObj.utils().logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		pageObj.utils().logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		pageObj.utils().logPass("Reward Id for user is fetched: " + rewardId);

		// api2 mobile Api-> Add Reward into discount basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketWithExtIdAPI2(token,
				dataSet.get("client"), dataSet.get("secret"), "reward", rewardId, externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");

		// Get external_uid value from DB
		String external_uid = DBUtils.executeQueryAndGetColumnValue(env, getExternalUid.replace("$userID", userId),
				"external_uid");
		Assert.assertEquals(external_uid, null, "No Value is present at external_uid column in discount basket ");
		logger.info("NULL value for external_uid column in discount basket after SELECT API");

		// Get locked_at value from DB
		String locked_at = DBUtils.executeQueryAndGetColumnValue(env, getLockedAt.replace("$userID", userId),
				"locked_at");
		Assert.assertEquals(locked_at, null, "No Value is present at locked_at column in discount basket ");
		logger.info("NULL value for locked_at column in discount basket after SELECT API");

		pageObj.utils().logPass(
				"Verified that Reward item should get added to basket successfully using API2 mobile API but external_uid and locked_at should not get saved in discount_baskets table for corresponding user.");

		// GET ACTIVE DISCOUNT BASKET
		Response getBasketDiscountDetailsResponse = pageObj.endpoints().getDiscountBasketByUIDUsingApi2Mobile(token,
				dataSet.get("client"), dataSet.get("secret"), externalUID);

		Assert.assertEquals(getBasketDiscountDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API Mobile Get Active Basket");
		String locked = getBasketDiscountDetailsResponse.jsonPath().get("locked").toString();
		Assert.assertEquals(locked, "false", "locked value is missing from API response");
		logger.info("locked value-FALSE is present in GET API response");

		// Get external_uid value from DB
		String externalUid = DBUtils.executeQueryAndGetColumnValue(env, getExternalUid.replace("$userID", userId),
				"external_uid");
		Assert.assertEquals(externalUid, null, "No Value is present at external_uid column in discount basket ");
		logger.info("NULL value for external_uid column in discount basket after GET API");

		// Get locked_at value from DB
		String lockedAt = DBUtils.executeQueryAndGetColumnValue(env, getLockedAt.replace("$userID", userId),
				"locked_at");
		Assert.assertEquals(lockedAt, null, "No Value is present at locked_at column in discount basket ");
		logger.info("NULL value for locked_at column in discount basket after GET API");

		pageObj.utils().logPass(
				"Verified that items present in basket should get fetched successfully using API2 mobile but external_uid and locked_at should not get saved in discount_baskets table for corresponding user.");

		String actualDiscountBasketItemIDFromBasket = getBasketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");
		String actualDiscountIDFromBasket = getBasketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "");

		Assert.assertEquals(actualDiscountIDFromBasket, rewardId);

		// DELETE Items from DISCOUNT BASKET

		Response deleteBasketResponse = pageObj.endpoints().deleteMobileDiscountBasketByUIDApi2(token,
				dataSet.get("client"), dataSet.get("secret"), actualDiscountBasketItemIDFromBasket, externalUID);

		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		String lockedUnselect = deleteBasketResponse.jsonPath().get("locked").toString();
		Assert.assertEquals(lockedUnselect, "false", "locked value is missing from API response");
		logger.info("locked value-FALSE is present in API response");

		// Get external_uid value from DB
		String externalUidUnselectDB = DBUtils.executeQueryAndGetColumnValue(env,
				getExternalUid.replace("$userID", userId), "external_uid");
		Assert.assertEquals(externalUidUnselectDB, null,
				"No Value is present at external_uid column in discount basket ");

		// Get locked_at value from DB
		String lockedAtUnselectDB = DBUtils.executeQueryAndGetColumnValue(env, getLockedAt.replace("$userID", userId),
				"locked_at");
		Assert.assertEquals(lockedAtUnselectDB, null, "No Value is present at locked_at column in discount basket ");

		pageObj.utils().logPass(
				"Verified that Reward item should get removed from basket successfully using API2 mobile but external_uid and locked_at should not get saved in discount_baskets table for corresponding user.");

		// Delete from redeemable tables

		String deleteRedeemableQuery1 = deleteRedeemableQuery
				.replace("${redeemableExternalID}", actualExternalIdRedeemable)
				.replace("${businessID}", dataSet.get("business_id"));

		DBUtils.executeQuery(env, deleteRedeemableQuery1);

		pageObj.utils().logPass(actualExternalIdRedeemable + " external redeemable is deleted successfully");

	}

	@Test(description = "OMM-T4198 POS SELECT API >> Verify external_uid and locked_at are not saved in DB after calling SELECT API.|| "
			+ "OMM-T4259 (1.0) POS GET API >> Verify external_uid and locked_at are not saved in DB after calling POS GET API. ||"
			+ "OMM-T4261 (1.0) POS UNSELECT API >> Verify external_uid and locked_at are not saved in DB after calling POS UNSELECT API.", priority = 1)
	@Owner(name = "Rahul Garg")
	public void T4198_T4259_T4261_posDoesNotPersistExternalUidOrLockedAt() throws Exception {

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.dashboardpage().updateCheckBox();

		// Create Redeemable
		String redeemableName = "AutomationRedeemableExistingQC_API_" + CreateDateTime.getTimeDateString();
		String endTime = CreateDateTime.getFutureDateTimeInGivenFormate(1, "yyyy-MM-dd'T'HH:mm:ss");

		Map<String, String> redeemableMap = new HashMap<String, String>();
		redeemableMap.put("redeemableName", redeemableName);
		redeemableMap.put("external_id", "");
		redeemableMap.put("locationID", null);
		redeemableMap.put("external_id_redeemable", "");
		redeemableMap.put("qualifier_type", "flat_discount");
		redeemableMap.put("discount_amount", "10.0");
		redeemableMap.put("end_time", endTime);
		redeemableMap.put("lineitemSelector", dataSet.get("lineItemSelector"));

		// Step2 Qualifier type is provided but invalid redeeming_criterion
		Response responsRedeemable = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), redeemableMap);
		Assert.assertEquals(responsRedeemable.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actualExternalIdRedeemable = responsRedeemable.jsonPath().getString("results[0].external_id")
				.replace("[", "").replace("]", "");
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", actualExternalIdRedeemable);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		pageObj.utils().logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		pageObj.utils().logPass("API v2 User Signup call is successful");

		// Send reward to user
		pageObj.utils().logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		pageObj.utils().logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		pageObj.utils().logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		pageObj.utils().logPass("Reward Id for user is fetched: " + rewardId);

		// POS API-> Add Reward into discount basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationKey"), userId, "reward", rewardId, externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");

		// Get external_uid value from DB
		String external_uid = DBUtils.executeQueryAndGetColumnValue(env, getExternalUid.replace("$userID", userId),
				"external_uid");
		Assert.assertEquals(external_uid, null, "No Value is present at external_uid column in discount basket ");
		logger.info("NULL value for external_uid column in discount basket after SELECT API");

		// Get locked_at value from DB
		String locked_at = DBUtils.executeQueryAndGetColumnValue(env, getLockedAt.replace("$userID", userId),
				"locked_at");
		Assert.assertEquals(locked_at, null, "No Value is present at locked_at column in discount basket ");
		logger.info("NULL value for locked_at column in discount basket after Select API");

		pageObj.utils().logPass(
				"Verified that Reward should get added to basket successfully using POS API but external_uid and locked_at should not get saved in discount_baskets table for corresponding user.");

		// POS API-> GET ACTIVE DISCOUNT BASKET
		Response getBasketDiscountDetailsResponse = pageObj.endpoints().fetchActiveBasketPOSAPI(userId,
				dataSet.get("locationKey"), externalUID);

		Assert.assertEquals(getBasketDiscountDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API Mobile Get Active Basket");
		String locked = getBasketDiscountDetailsResponse.jsonPath().get("locked").toString();
		Assert.assertEquals(locked, "false", "locked value is missing from API response");
		logger.info("locked value-FALSE is present in GET API response");

		// Get external_uid value from DB
		String externalUid = DBUtils.executeQueryAndGetColumnValue(env, getExternalUid.replace("$userID", userId),
				"external_uid");
		Assert.assertEquals(externalUid, null, "No Value is present at external_uid column in discount basket ");
		logger.info("NULL value for external_uid column in discount basket after GET active API");

		// Get locked_at value from DB
		String lockedAt = DBUtils.executeQueryAndGetColumnValue(env, getLockedAt.replace("$userID", userId),
				"locked_at");
		Assert.assertEquals(lockedAt, null, "No Value is present at locked_at column in discount basket ");

		pageObj.utils().logPass(
				"Verified that items present in basket should get fetched successfully using POS API but external_uid and locked_at should not get saved in discount_baskets table for corresponding user.");

		String actualDiscountBasketItemIDFromBasket = getBasketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");
		String actualDiscountIDFromBasket = getBasketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "");

		Assert.assertEquals(actualDiscountIDFromBasket, rewardId);

		// POS API->DELETE Items from DISCOUNT BASKET

		Response deleteBasketResponse = pageObj.endpoints().removeDiscountFromBasketPOSAPI(dataSet.get("locationKey"),
				userId, actualDiscountBasketItemIDFromBasket, externalUID);

		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		String lockedUnselect = deleteBasketResponse.jsonPath().get("locked").toString();
		Assert.assertEquals(lockedUnselect, "false", "locked value is missing from API response");
		logger.info("locked value-FALSE is present in API response");

		// Get external_uid value from DB
		String externalUidUnselectDB = DBUtils.executeQueryAndGetColumnValue(env,
				getExternalUid.replace("$userID", userId), "external_uid");
		Assert.assertEquals(externalUidUnselectDB, null,
				"No Value is present at external_uid column in discount basket ");

		// Get locked_at value from DB
		String lockedAtUnselectDB = DBUtils.executeQueryAndGetColumnValue(env, getLockedAt.replace("$userID", userId),
				"locked_at");
		Assert.assertEquals(lockedAtUnselectDB, null, "No Value is present at locked_at column in discount basket ");

		pageObj.utils().logPass(
				"Verified that Reward added Items using should get removed from basket successfully using POS API but external_uid and locked_at should not get saved in discount_baskets table for corresponding user.");

		// Delete from redeemable tables

		String deleteRedeemableQuery1 = deleteRedeemableQuery
				.replace("${redeemableExternalID}", actualExternalIdRedeemable)
				.replace("${businessID}", dataSet.get("business_id"));

		DBUtils.executeQuery(env, deleteRedeemableQuery1);

		pageObj.utils().logPass(actualExternalIdRedeemable + " external redeemable is deleted successfully");

	}

	@Test(description = "OMM-T4262 (1.0) AUTH SELECT API >> Verify external_uid and locked_at are not saved in DB after calling AUTH SELECT API.|| "
			+ "OMM-T4265 (1.0) AUTH UNSELECT API >> Verify external_uid and locked_at are not saved in DB after calling POS UNSELECT API. ||"
			+ "OMM-T4263 (1.0) AUTH GET API >> Verify external_uid and locked_at are not saved in DB after calling AUTH GET API.", priority = 1)
	@Owner(name = "Rahul Garg")
	public void T4262_T4265_T4263_authDoesNotPersistExternalUidOrLockedAt() throws Exception {

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.dashboardpage().updateCheckBox();

		// Create Redeemable
		String redeemableName = "AutomationRedeemableExistingQC_API_" + CreateDateTime.getTimeDateString();
		String endTime = CreateDateTime.getFutureDateTimeInGivenFormate(1, "yyyy-MM-dd'T'HH:mm:ss");

		Map<String, String> redeemableMap = new HashMap<String, String>();
		redeemableMap.put("redeemableName", redeemableName);
		redeemableMap.put("external_id", "");
		redeemableMap.put("locationID", null);
		redeemableMap.put("external_id_redeemable", "");
		redeemableMap.put("qualifier_type", "flat_discount");
		redeemableMap.put("discount_amount", "10.0");
		redeemableMap.put("end_time", endTime);
		redeemableMap.put("lineitemSelector", dataSet.get("lineItemSelector"));

		// Step2 Qualifier type is provided but invalid redeeming_criterion
		Response responsRedeemable = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), redeemableMap);
		Assert.assertEquals(responsRedeemable.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actualExternalIdRedeemable = responsRedeemable.jsonPath().getString("results[0].external_id")
				.replace("[", "").replace("]", "");
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", actualExternalIdRedeemable);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		pageObj.utils().logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		pageObj.utils().logPass("API v2 User Signup call is successful");

		// Send reward to user
		pageObj.utils().logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		pageObj.utils().logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		pageObj.utils().logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		pageObj.utils().logPass("Reward Id for user is fetched: " + rewardId);

		// AUTH API-> Add Reward into discount basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");

		// Get external_uid value from DB
		String external_uid = DBUtils.executeQueryAndGetColumnValue(env, getExternalUid.replace("$userID", userId),
				"external_uid");
		Assert.assertEquals(external_uid, null, "No Value is present at external_uid column in discount basket ");
		logger.info("NULL value for external_uid column in discount basket after SELECT API");

		// Get locked_at value from DB
		String locked_at = DBUtils.executeQueryAndGetColumnValue(env, getLockedAt.replace("$userID", userId),
				"locked_at");
		Assert.assertEquals(locked_at, null, "No Value is present at locked_at column in discount basket ");
		logger.info("NULL value for locked_at column in discount basket after Select API");

		pageObj.utils().logPass(
				"Verified that Reward added Items using should get added to basket successfully using Auth API but external_uid and locked_at should not get saved in discount_baskets table for corresponding user.");

		// AUTH API-> GET ACTIVE DISCOUNT BASKET
		Response getBasketDiscountDetailsResponse = pageObj.endpoints().fetchActiveBasketAuthApi(token,
				dataSet.get("client"), dataSet.get("secret"), externalUID);

		Assert.assertEquals(getBasketDiscountDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API Mobile Get Active Basket");
		String locked = getBasketDiscountDetailsResponse.jsonPath().get("locked").toString();
		Assert.assertEquals(locked, "false", "locked value is missing from API response");
		logger.info("locked value-FALSE is present in GET API response");

		// Get external_uid value from DB
		String externalUid = DBUtils.executeQueryAndGetColumnValue(env, getExternalUid.replace("$userID", userId),
				"external_uid");
		Assert.assertEquals(externalUid, null, "No Value is present at external_uid column in discount basket ");
		logger.info("NULL value for external_uid column in discount basket after GET active API");

		// Get locked_at value from DB
		String lockedAt = DBUtils.executeQueryAndGetColumnValue(env, getLockedAt.replace("$userID", userId),
				"locked_at");
		Assert.assertEquals(lockedAt, null, "No Value is present at locked_at column in discount basket ");

		pageObj.utils().logPass(
				"Verified that items present in basket should get fetched successfully using Auth API but external_uid and locked_at should not get saved in discount_baskets table for corresponding user.");

		String actualDiscountBasketItemIDFromBasket = getBasketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");
		String actualDiscountIDFromBasket = getBasketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "");

		Assert.assertEquals(actualDiscountIDFromBasket, rewardId);

		// AUTH API->DELETE Items from DISCOUNT BASKET

		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserWithExt_UidAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), actualDiscountBasketItemIDFromBasket, externalUID);

		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		String lockedUnselect = deleteBasketResponse.jsonPath().get("locked").toString();
		Assert.assertEquals(lockedUnselect, "false", "locked value is missing from API response");
		logger.info("locked value-FALSE is present in API response");

		// Get external_uid value from DB
		String externalUidUnselectDB = DBUtils.executeQueryAndGetColumnValue(env,
				getExternalUid.replace("$userID", userId), "external_uid");
		Assert.assertEquals(externalUidUnselectDB, null,
				"No Value is present at external_uid column in discount basket ");

		// Get locked_at value from DB
		String lockedAtUnselectDB = DBUtils.executeQueryAndGetColumnValue(env, getLockedAt.replace("$userID", userId),
				"locked_at");
		Assert.assertEquals(lockedAtUnselectDB, null, "No Value is present at locked_at column in discount basket ");

		pageObj.utils().logPass(
				"Verified that Reward added Items using should get removed from basket successfully using Auth API but external_uid and locked_at should not get saved in discount_baskets table for corresponding user.");

		// Delete from redeemable tables

		String deleteRedeemableQuery1 = deleteRedeemableQuery
				.replace("${redeemableExternalID}", actualExternalIdRedeemable)
				.replace("${businessID}", dataSet.get("business_id"));

		DBUtils.executeQuery(env, deleteRedeemableQuery1);

		pageObj.utils().logPass(actualExternalIdRedeemable + " external redeemable is deleted successfully");

	}

	@Test(description = "OMM-T4264 AUTH AUTO-SELECT API >> Verify external_uid and locked_at are not saved in DB after calling AUTO-SELECT API.|| "
			+ "OMM-T4260 (1.0) POS AUTO-SELECT API >> Verify external_uid and locked_at are not saved in DB after calling AUTO-SELECT API", priority = 1)
	@Owner(name = "Rahul Garg")

	public void T4264_T4260_autoSelectAPIDoesNotPersistExternalUidOrLockedAt() throws Exception {

		Map<String, String> detailsMap = new HashMap<String, String>();
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.dashboardpage().updateCheckBox();

		// Create Redeemable
		String redeemableName = "AutomationRedeemableExistingQC_API_" + CreateDateTime.getTimeDateString();
		String endTime = CreateDateTime.getFutureDateTimeInGivenFormate(1, "yyyy-MM-dd'T'HH:mm:ss");

		Map<String, String> redeemableMap = new HashMap<String, String>();
		redeemableMap.put("redeemableName", redeemableName);
		redeemableMap.put("external_id", "");
		redeemableMap.put("locationID", null);
		redeemableMap.put("external_id_redeemable", "");
		redeemableMap.put("qualifier_type", "flat_discount");
		redeemableMap.put("discount_amount", "10.0");
		redeemableMap.put("end_time", endTime);
		redeemableMap.put("lineitemSelector", dataSet.get("lineItemSelector"));

		// Step2 Qualifier type is provided but invalid redeeming_criterion
		Response responsRedeemable = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), redeemableMap);
		Assert.assertEquals(responsRedeemable.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actualExternalIdRedeemable = responsRedeemable.jsonPath().getString("results[0].external_id")
				.replace("[", "").replace("]", "");
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", actualExternalIdRedeemable);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		pageObj.utils().logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		pageObj.utils().logPass("API v2 User Signup call is successful");

		// Send reward to user
		pageObj.utils().logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		pageObj.utils().logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		pageObj.utils().logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		pageObj.utils().logPass("Reward Id for user is fetched: " + rewardId);

		// ------AUTH Auto Select API------
		detailsMap = pageObj.endpoints().getRecieptDetailsMap("Sandwich", "1", "10", "M", "2", "", "1.0",
				dataSet.get("item_id"));
		parentMap.put("Sandwich", detailsMap);
		Response authAutoSelectAPIResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "10", externalUID, parentMap);
		Assert.assertEquals(authAutoSelectAPIResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Auto Unlock ");

		// Verify Locked value in API response
		String lockedAutoSelect = authAutoSelectAPIResponse.jsonPath().get("locked").toString();
		Assert.assertEquals(lockedAutoSelect, "false", "locked value is missing from API response");
		logger.info("locked value-FALSE is present from API response");

		// Get external_uid value from DB
		String external_uid = DBUtils.executeQueryAndGetColumnValue(env, getExternalUid.replace("$userID", userId),
				"external_uid");
		Assert.assertEquals(external_uid, null, "No Value is present at external_uid column in discount basket ");
		logger.info("NULL value for external_uid column in discount basket after Auto select API");

		// Get locked_at value from DB
		String locked_at = DBUtils.executeQueryAndGetColumnValue(env, getLockedAt.replace("$userID", userId),
				"locked_at");
		Assert.assertEquals(locked_at, null, "No Value is present at locked_at column in discount basket ");
		logger.info("NULL value for locked_at column in discount basket after Auto Select API");

		pageObj.utils().logPass(
				"Verified that Reward item should get added to basket successfully using Auth Select API but external_uid and locked_at should not get saved in discount_baskets table for corresponding user.");

		// -------POS Auto Select API------
		Response posAutoSelectAPIResponse = pageObj.endpoints().autoSelectPosApi(userId, "30", "1", "12003",
				externalUID, dataSet.get("locationKey"));
		Assert.assertEquals(posAutoSelectAPIResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Auto Unlock ");

		// Verify Locked value in API response
		String lockedPOSAutoSelect = posAutoSelectAPIResponse.jsonPath().get("locked").toString();
		Assert.assertEquals(lockedPOSAutoSelect, "false", "locked value is missing from API response");
		logger.info("locked value-FALSE is present from API response");

		// Get external_uid value from DB
		String externalUID = DBUtils.executeQueryAndGetColumnValue(env, getExternalUid.replace("$userID", userId),
				"external_uid");
		Assert.assertEquals(externalUID, null, "No Value is present at external_uid column in discount basket ");
		logger.info("NULL value for external_uid column in discount basket after Auto Select API");

		// Get locked_at value from DB
		String lockedAt = DBUtils.executeQueryAndGetColumnValue(env, getLockedAt.replace("$userID", userId),
				"locked_at");
		Assert.assertEquals(lockedAt, null, "No Value is present at locked_at column in discount basket ");
		logger.info("NULL value for locked_at column in discount basket after Auto select API");

		pageObj.utils().logPass(
				"Verified that Reward item should get added to basket successfully using POS Select API but external_uid and locked_at should not get saved in discount_baskets table for corresponding user.");

		// Delete from redeemable tables

		String deleteRedeemableQuery1 = deleteRedeemableQuery
				.replace("${redeemableExternalID}", actualExternalIdRedeemable)
				.replace("${businessID}", dataSet.get("business_id"));

		DBUtils.executeQuery(env, deleteRedeemableQuery1);

		pageObj.utils().logPass(actualExternalIdRedeemable + " external redeemable is deleted successfully");

	}

	@Test(description = "OMM-T4347 (1.0) Verify the Basket should be locked upon POS Discount Lookup API call ||"
			+ "OMM-T4278 (1.0) Validate Batch Redemption API with valid external id when Basket locked using POS Lookup ||"
			+ "OMM-T4279 (1.0) Validate Batch Redemption API with query-TRUE and FALSE.", priority = 1)
	@Owner(name = "Rahul Garg")

	public void T4347_T4278_T4279_PosDiscountLookup_BatchRedemption_PersistUidAndLockTime() throws Exception {

		Map<String, String> detailsMap = new HashMap<String, String>();
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Offers", "Select");
		pageObj.dashboardpage().updateCheckBox();

		// Create Redeemable
		String redeemableName = "AutomationRedeemableExistingQC_API_" + CreateDateTime.getTimeDateString();
		String endTime = CreateDateTime.getFutureDateTimeInGivenFormate(1, "yyyy-MM-dd'T'HH:mm:ss");

		Map<String, String> redeemableMap = new HashMap<String, String>();
		redeemableMap.put("redeemableName", redeemableName);
		redeemableMap.put("external_id", "");
		redeemableMap.put("locationID", null);
		redeemableMap.put("external_id_redeemable", "");
		redeemableMap.put("qualifier_type", "flat_discount");
		redeemableMap.put("discount_amount", "10.0");
		redeemableMap.put("end_time", endTime);
		redeemableMap.put("lineitemSelector", dataSet.get("lineItemSelector"));

		// Step2 Qualifier type is provided but invalid redeeming_criterion
		Response responsRedeemable = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), redeemableMap);
		Assert.assertEquals(responsRedeemable.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actualExternalIdRedeemable = responsRedeemable.jsonPath().getString("results[0].external_id")
				.replace("[", "").replace("]", "");
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", actualExternalIdRedeemable);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		pageObj.utils().logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		pageObj.utils().logPass("API v2 User Signup call is successful");

		// Send reward to user
		pageObj.utils().logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		pageObj.utils().logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		pageObj.utils().logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		pageObj.utils().logPass("Reward Id for user is fetched: " + rewardId);

		// -------POS Auto Select API------
		Response posAutoSelectAPIResponse = pageObj.endpoints().autoSelectPosApi(userId, dataSet.get("amount"),
				dataSet.get("item_qty"), dataSet.get("item_id"), externalUID, dataSet.get("locationKey"));
		Assert.assertEquals(posAutoSelectAPIResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Auto Unlock ");

		// Verify Locked value in API response
		String lockedPOSAutoSelect = posAutoSelectAPIResponse.jsonPath().get("locked").toString();
		Assert.assertEquals(lockedPOSAutoSelect, "false", "locked value is missing from API response");
		logger.info("locked value-FALSE is present from API response");

		// Add multiple items dynamically

		List<Map<String, Object>> lineItems = new ArrayList<>();
		Map<String, Object> receiptItems = new HashMap<String, Object>();

		// Sandwich|1||M|10|306|522|1.0

		receiptItems = ApiPayloads.getInputForReceiptItems(dataSet.get("item_name"),
				Integer.parseInt(dataSet.get("item_qty")), Double.parseDouble(dataSet.get("amount")),
				dataSet.get("item_type"), dataSet.get("item_id"), dataSet.get("item_family"), dataSet.get("item_group"),
				dataSet.get("serial_number"));
		lineItems.add(receiptItems);

		String receipt_datetime = CreateDateTime.getCurrentDateTimeInUtc();
		String punchh_key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		// -----Hit POS discount lookup API-----

		Response posDiscountLookupResponse = pageObj.endpoints().posDiscountLookupAPIInputPayload(lineItems,
				receipt_datetime, Double.parseDouble(dataSet.get("subtotal_amount")),
				Double.parseDouble(dataSet.get("receipt_amount")), punchh_key, transaction_no, userId,
				dataSet.get("locationKey"), externalUID);

		Assert.assertEquals(posDiscountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Verify Locked value in POS Discount Lookup API response
		String lockedPOSDiscountLookup = posDiscountLookupResponse.jsonPath().get("locked").toString();
		Assert.assertEquals(lockedPOSDiscountLookup, "true", "locked value-TRUE is present in API response");
		logger.info("locked value-TRUE is present from API response");

		// -----POS batch redemption With Query Param true-----

		detailsMap = pageObj.endpoints().getRecieptDetailsMap("Sandwich", "1", "10", "M", "2", "", "1.0",
				dataSet.get("item_id"));
		parentMap.put("Sandwich", detailsMap);
		Response batchRedemptionProcessResponseUser = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId,
						dataSet.get("subtotal_amount"), "true", externalUID, parentMap);

		Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		// Get external_uid value from DB
		String externalUidBatchRedemptionDB = DBUtils.executeQueryAndGetColumnValue(env,
				getExternalUid.replace("$userID", userId), "external_uid");
		Assert.assertEquals(externalUidBatchRedemptionDB, externalUID,
				"Value is present at external_uid column in discount basket ");
		logger.info("Value is present at external_uid column in discount basket");

		// Get locked_at value from DB
		String lockedAtBatchRedemptionDB = DBUtils.executeQueryAndGetColumnValue(env,
				getLockedAt.replace("$userID", userId), "locked_at");
		Assert.assertNotNull(lockedAtBatchRedemptionDB, "Value is present at locked_at column in discount basket");
		logger.info("Value is present at locked_at column in discount basket");

		// -----Auth batch redemption With Query Param true-----

		Response authBatchRedemptionResponse = pageObj.endpoints().authBatchRedemptionWithQueryParam(lineItems,
				receipt_datetime, Double.parseDouble(dataSet.get("subtotal_amount")),
				Double.parseDouble(dataSet.get("receipt_amount")), punchh_key, transaction_no, userId,
				dataSet.get("client"), dataSet.get("secret"), token, externalUID, "true", dataSet.get("locationKey"),
				Integer.parseInt(dataSet.get("storeNum")));

		Assert.assertEquals(authBatchRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		// Get external_uid value from DB
		String externalUidAuthBatchRedemption = DBUtils.executeQueryAndGetColumnValue(env,
				getExternalUid.replace("$userID", userId), "external_uid");
		Assert.assertEquals(externalUidAuthBatchRedemption, externalUID,
				"Value is present at external_uid column in discount basket ");
		logger.info("Value is present at external_uid column in discount basket After Auth Batch redemption");

		// Get locked_at value from DB
		String lockedAtAuthBatchRedemption = DBUtils.executeQueryAndGetColumnValue(env,
				getLockedAt.replace("$userID", userId), "locked_at");
		Assert.assertNotNull(lockedAtAuthBatchRedemption, "Value is present at locked_at column in discount basket");
		logger.info("Value is present at locked_at column in discount basket After Auth Batch redemption");

		// -----POS batch redemption With Query Param FALSE-----

		detailsMap = pageObj.endpoints().getRecieptDetailsMap("Sandwich", "1", "10", "M", "2", "", "1.0",
				dataSet.get("item_id"));
		parentMap.put("Sandwich", detailsMap);
		Response batchRedemptionProcessResponseWithFalse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId,
						dataSet.get("subtotal_amount"), "false", externalUID, parentMap);

		Assert.assertEquals(batchRedemptionProcessResponseWithFalse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		// Get external_uid value from DB
		String externalUidBatchRedemptionWithFalseDB = DBUtils.executeQueryAndGetColumnValue(env,
				getExternalUid.replace("$userID", userId), "external_uid");
		Assert.assertEquals(externalUidBatchRedemptionWithFalseDB, externalUID,
				"Value is present at external_uid column in discount basket ");
		logger.info("Value is present at external_uid column in discount basket After POS Batch redemption");

		// Get locked_at value from DB
		String lockedAtBatchRedemptionWithFalseDB = DBUtils.executeQueryAndGetColumnValue(env,
				getLockedAt.replace("$userID", userId), "locked_at");
		Assert.assertNotNull(lockedAtBatchRedemptionWithFalseDB,
				"Value is present at locked_at column in discount basket");
		logger.info("Value is present at locked_at column in discount basket After POS Batch redemption");

		pageObj.utils().logPass(
				"Verified that external_uid and locked_at should get saved in discount_baskets table for corresponding user after POS discount lookup and Batch redemption");

		// Delete from redeemable tables

		String deleteRedeemableQuery1 = deleteRedeemableQuery
				.replace("${redeemableExternalID}", actualExternalIdRedeemable)
				.replace("${businessID}", dataSet.get("business_id"));

		DBUtils.executeQuery(env, deleteRedeemableQuery1);

		pageObj.utils().logPass(actualExternalIdRedeemable + " external redeemable is deleted successfully");

	}

	@Test(description = "OMM-T4214 (1.0) POS Select>Validate that an error message appears if the basket is locked via POS and the user tries to add a new discount type using the POS SELECT API without external_id or with an invalid external_id ||"
			+ "OMM-T4237 (1.0) POS GET Active>Validate that an error message appears if the basket is locked via POS and user tries to fetch basket details using the POS GET Active API without external_id or with an invalid external_id ||"
			+ "OMM-T4233 (1.0) POS Unselect>Validate that an error message appears if the basket is locked via POS and the user tries to remove a new discount type using the POS Unselect API without external_id or with an invalid external_id ||"
			+ "OMM-T4257 (1.0) POS Auto Select>Validate that an error message appears if the basket is locked via Auth and the user tries to add a new discount type using the POS Auto SELECT API without external_id or with an invalid external_id", priority = 1)
	@Owner(name = "Rahul Garg")

	public void T4214_T4237_T4233_T4257_errorOnPosAPIAfterBasketIsLocked() throws Exception {

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.dashboardpage().updateCheckBox();

		// Create Redeemable
		String redeemableName = "AutomationRedeemableExistingQC_API_" + CreateDateTime.getTimeDateString();
		String endTime = CreateDateTime.getFutureDateTimeInGivenFormate(1, "yyyy-MM-dd'T'HH:mm:ss");

		Map<String, String> redeemableMap = new HashMap<String, String>();
		redeemableMap.put("redeemableName", redeemableName);
		redeemableMap.put("external_id", "");
		redeemableMap.put("locationID", null);
		redeemableMap.put("external_id_redeemable", "");
		redeemableMap.put("qualifier_type", "flat_discount");
		redeemableMap.put("discount_amount", "10.0");
		redeemableMap.put("end_time", endTime);
		redeemableMap.put("lineitemSelector", dataSet.get("lineItemSelector"));

		// Step2 Qualifier type is provided but invalid redeeming_criterion
		Response responsRedeemable = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), redeemableMap);
		Assert.assertEquals(responsRedeemable.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actualExternalIdRedeemable = responsRedeemable.jsonPath().getString("results[0].external_id")
				.replace("[", "").replace("]", "");
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", actualExternalIdRedeemable);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		pageObj.utils().logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		pageObj.utils().logPass("API v2 User Signup call is successful");

		// Send reward to user
		pageObj.utils().logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		pageObj.utils().logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		pageObj.utils().logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		pageObj.utils().logPass("Reward Id for user is fetched: " + rewardId);

		// POS API-> Add Reward into discount basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationKey"), userId, "reward", rewardId, externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");

		String actualDiscountBasketItemIDFromBasket = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");

		// -----Hit POS discount lookup API-----

		// Add multiple items dynamically
		List<Map<String, Object>> lineItems = new ArrayList<>();
		Map<String, Object> receiptItems = new HashMap<String, Object>();

		// Sandwich|1||M|10|306|522|1.0

		receiptItems = ApiPayloads.getInputForReceiptItems(dataSet.get("item_name"),
				Integer.parseInt(dataSet.get("item_qty")), Double.parseDouble(dataSet.get("amount")),
				dataSet.get("item_type"), dataSet.get("item_id"), dataSet.get("item_family"), dataSet.get("item_group"),
				dataSet.get("serial_number"));
		lineItems.add(receiptItems);

		String receipt_datetime = CreateDateTime.getCurrentDateTimeInUtc();
		String punchh_key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		Response posDiscountLookupResponse = pageObj.endpoints().posDiscountLookupAPIInputPayload(lineItems,
				receipt_datetime, Double.parseDouble(dataSet.get("subtotal_amount")),
				Double.parseDouble(dataSet.get("receipt_amount")), punchh_key, transaction_no, userId,
				dataSet.get("locationKey"), externalUID);

		Assert.assertEquals(posDiscountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Verify Locked value in POS Discount Lookup API response
		String lockedPOSDiscountLookup = posDiscountLookupResponse.jsonPath().get("locked").toString();
		Assert.assertEquals(lockedPOSDiscountLookup, "true", "locked value-TRUE is present in API response");

		// POS API-> Add Reward into discount basket after basket getting locked
		Response discountBasketResponseAfterLock = pageObj.endpoints()
				.authListDiscountBasketAddedForPOSAPI(dataSet.get("locationKey"), userId, "reward", rewardId);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, discountBasketResponseAfterLock.getStatusCode(),
				"Status code 422 did not match with add discount to basket");

		String errorMessage = discountBasketResponseAfterLock.jsonPath().get("error").toString();
		Assert.assertEquals(errorMessage, MessagesConstants.basketLockedErrMsg, "Error message did not match ");
		logger.info("Verified the error message in SELECT API if discount basket gets locked");

		// POS API-> GET ACTIVE DISCOUNT BASKET after basket getting locked
		Response getBasketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingPOS(userId,
				dataSet.get("locationKey"));

		Assert.assertEquals(getBasketDiscountDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API Mobile Get Active Basket");
		String errorMessageGETAPI = getBasketDiscountDetailsResponse.jsonPath().get("error").toString();
		Assert.assertEquals(errorMessageGETAPI, MessagesConstants.basketLockedErrMsg, "Error message did not match");
		logger.info("Verified the error message in GET API if discount basket gets locked");

		// POS API->DELETE Items from DISCOUNT BASKET

		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserPOS(dataSet.get("locationKey"),
				userId, actualDiscountBasketItemIDFromBasket);

		String errorMessageUnselect = deleteBasketResponse.jsonPath().get("error").toString();

		Assert.assertEquals(ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(errorMessageUnselect, MessagesConstants.basketLockedErrMsg);
		logger.info("Verified the error message in DELETE API if discount basket gets locked");

		// -------POS Auto Select API------

		Response posAutoSelectAPIResponse = pageObj.endpoints().autoSelectPosApi(userId, "30", "1", "12003", "",
				dataSet.get("locationKey"));
		String errorMessageAutoSelect = posAutoSelectAPIResponse.jsonPath().get("error").toString();

		Assert.assertEquals(posAutoSelectAPIResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 200 did not match with Auto Unlock ");
		Assert.assertEquals(errorMessageAutoSelect, MessagesConstants.basketLockedErrMsg);
		logger.info("Verified the error message in Auto SELECT API if discount basket gets locked");

		// Delete from redeemable tables

		String deleteRedeemableQuery1 = deleteRedeemableQuery
				.replace("${redeemableExternalID}", actualExternalIdRedeemable)
				.replace("${businessID}", dataSet.get("business_id"));

		DBUtils.executeQuery(env, deleteRedeemableQuery1);

		pageObj.utils().logPass(actualExternalIdRedeemable + " external redeemable is deleted successfully");

	}

	@Test(description = "OMM-T4218 (1.0) Auth Select>Validate that an error message appears if the basket is locked via POS and the user tries to add a new discount type using the AUTH SELECT API without external_id or with an invalid external_id ||"
			+ "OMM-T4236 (1.0) Auth Unselect >Validate that an error message appears if the basket is locked via POS and the user tries to remove a new discount type using the AUTH Unselect API without external_id or with an invalid external_id||"
			+ "OMM-T4240 (1.0) AUTH GET Active>Validate that error message appears if the basket is locked via POS and the user tries to fetch basket details using the AUTH GET Active API without external_id or with an invalid external_id ||"
			+ "OMM-T4242 (1.0) Auth Auto Select>Validate that an error message appears if the basket is locked via POS and the user tries to add a new discount type using the AUTH Auto SELECT API without external_id or with an invalid external_id", priority = 1)
	@Owner(name = "Rahul Garg")

	public void T4218_T4236_T4240_T4242_errorOnAuthAPIAfterBasketIsLocked() throws Exception {

		Map<String, String> detailsMap = new HashMap<String, String>();
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Offers", "Select");
		pageObj.dashboardpage().updateCheckBox();

		// Create Redeemable
		String redeemableName = "AutomationRedeemableExistingQC_API_" + CreateDateTime.getTimeDateString();
		String endTime = CreateDateTime.getFutureDateTimeInGivenFormate(1, "yyyy-MM-dd'T'HH:mm:ss");

		Map<String, String> redeemableMap = new HashMap<String, String>();
		redeemableMap.put("redeemableName", redeemableName);
		redeemableMap.put("external_id", "");
		redeemableMap.put("locationID", null);
		redeemableMap.put("external_id_redeemable", "");
		redeemableMap.put("qualifier_type", "flat_discount");
		redeemableMap.put("discount_amount", "10.0");
		redeemableMap.put("end_time", endTime);
		redeemableMap.put("lineitemSelector", dataSet.get("lineItemSelector"));

		// Step2 Qualifier type is provided but invalid redeeming_criterion
		Response responsRedeemable = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), redeemableMap);
		Assert.assertEquals(responsRedeemable.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actualExternalIdRedeemable = responsRedeemable.jsonPath().getString("results[0].external_id")
				.replace("[", "").replace("]", "");
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", actualExternalIdRedeemable);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		pageObj.utils().logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		pageObj.utils().logPass("API v2 User Signup call is successful");

		// Send reward to user
		pageObj.utils().logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		pageObj.utils().logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		pageObj.utils().logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		pageObj.utils().logPass("Reward Id for user is fetched: " + rewardId);

		// POS API-> Add Reward into discount basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationKey"), userId, "reward", rewardId, externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");

		String actualDiscountBasketItemIDFromBasket = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");

		// -----Hit POS discount lookup API-----

		// Add multiple items dynamically
		List<Map<String, Object>> lineItems = new ArrayList<>();
		Map<String, Object> receiptItems = new HashMap<String, Object>();

		// Sandwich|1||M|10|306|522|1.0

		receiptItems = ApiPayloads.getInputForReceiptItems(dataSet.get("item_name"),
				Integer.parseInt(dataSet.get("item_qty")), Double.parseDouble(dataSet.get("amount")),
				dataSet.get("item_type"), dataSet.get("item_id"), dataSet.get("item_family"), dataSet.get("item_group"),
				dataSet.get("serial_number"));
		lineItems.add(receiptItems);

		String receipt_datetime = CreateDateTime.getCurrentDateTimeInUtc();
		String punchh_key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		Response posDiscountLookupResponse = pageObj.endpoints().posDiscountLookupAPIInputPayload(lineItems,
				receipt_datetime, Double.parseDouble(dataSet.get("subtotal_amount")),
				Double.parseDouble(dataSet.get("receipt_amount")), punchh_key, transaction_no, userId,
				dataSet.get("locationKey"), externalUID);

		Assert.assertEquals(posDiscountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Verify Locked value in POS Discount Lookup API response
		String lockedPOSDiscountLookup = posDiscountLookupResponse.jsonPath().get("locked").toString();
		Assert.assertEquals(lockedPOSDiscountLookup, "true", "locked value-TRUE is present in API response");
		logger.info("locked value-TRUE is present in API response");

		// AUTH API-> Add Reward into discount basket
		Response authDiscountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, authDiscountBasketResponse.getStatusCode(),
				"Status code 422 did not match with add discount to basket ");
		String addDiscountError = authDiscountBasketResponse.jsonPath().get("error.message").toString();
		Assert.assertEquals(addDiscountError, MessagesConstants.basketLockedErrMsg, "Error message did not match");
		logger.info("Verified the error message in Auth Select API is basket gets locked");

		// AUTH API-> GET ACTIVE DISCOUNT BASKET
		Response getBasketDiscountDetailsResponse = pageObj.endpoints().fetchActiveBasketAuthApi(token,
				dataSet.get("client"), dataSet.get("secret"), "");

		Assert.assertEquals(getBasketDiscountDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API Mobile Get Active Basket");
		String getActiveError = getBasketDiscountDetailsResponse.jsonPath().get("error.message").toString();
		Assert.assertEquals(getActiveError, MessagesConstants.basketLockedErrMsg, "Error message did not match");
		logger.info("Verified the error message in Auth GET API is basket gets locked");

		// AUTH API->DELETE Items from DISCOUNT BASKET

		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserWithExt_UidAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), actualDiscountBasketItemIDFromBasket, "");

		Assert.assertEquals(deleteBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API Mobile Get Active Basket");
		String deleteItemError = deleteBasketResponse.jsonPath().get("error.message").toString();
		Assert.assertEquals(deleteItemError, MessagesConstants.basketLockedErrMsg, "Error message did not match");
		logger.info("Verified the error message in Auth Delete API is basket gets locked");

		// ------AUTH Auto Select API------
		detailsMap = pageObj.endpoints().getRecieptDetailsMap("Sandwich", "1", "10", "M", "2", "", "1.0",
				dataSet.get("item_id"));
		parentMap.put("Sandwich", detailsMap);
		Response authAutoSelectAPIResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "10", "", parentMap);
		Assert.assertEquals(authAutoSelectAPIResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match with Auto Unlock ");
		String autoSelectError = deleteBasketResponse.jsonPath().get("error.message").toString();
		Assert.assertEquals(autoSelectError, MessagesConstants.basketLockedErrMsg, "Error message did not match");
		logger.info("Verified the error message in Auth Auto Select API is basket gets locked");

		// Delete from redeemable tables

		String deleteRedeemableQuery1 = deleteRedeemableQuery
				.replace("${redeemableExternalID}", actualExternalIdRedeemable)
				.replace("${businessID}", dataSet.get("business_id"));

		DBUtils.executeQuery(env, deleteRedeemableQuery1);

		pageObj.utils().logPass(actualExternalIdRedeemable + " external redeemable is deleted successfully");

	}

	@Test(description = "OMM-T4215 (1.0) Mobile Select>Validate that an error message appears if the basket is locked via POS and the user tries to add a new discount type using the API/Mobile SELECT API without external_id or with an invalid external_id "
			+ "OMM-T4234 (1.0) Mobile Unselect>Validate that an error message appears if the basket is locked via POS and the user tries to remove a new discount type using the API/Mobile Unselect API without external_id or with an invalid external_id"
			+ "OMM-T4238 (1.0) Mobile GET Active>Validate that no error message appears if the basket is locked via POS and the user tries to fetch basket details using the API/Mobile GET Active API without external_id or with an invalid external_id", priority = 1)
	@Owner(name = "Rahul Garg")

	public void T4215_T4234_T4238_MobileAPIAfterBasketIsLocked() throws Exception {

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.dashboardpage().updateCheckBox();

		// Create Redeemable
		String redeemableName = "AutomationRedeemableExistingQC_API_" + CreateDateTime.getTimeDateString();
		String endTime = CreateDateTime.getFutureDateTimeInGivenFormate(1, "yyyy-MM-dd'T'HH:mm:ss");

		Map<String, String> redeemableMap = new HashMap<String, String>();
		redeemableMap.put("redeemableName", redeemableName);
		redeemableMap.put("external_id", "");
		redeemableMap.put("locationID", null);
		redeemableMap.put("external_id_redeemable", "");
		redeemableMap.put("qualifier_type", "flat_discount");
		redeemableMap.put("discount_amount", "10.0");
		redeemableMap.put("end_time", endTime);
		redeemableMap.put("lineitemSelector", dataSet.get("lineItemSelector"));

		// Step2 Qualifier type is provided but invalid redeeming_criterion
		Response responsRedeemable = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), redeemableMap);
		Assert.assertEquals(responsRedeemable.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actualExternalIdRedeemable = responsRedeemable.jsonPath().getString("results[0].external_id")
				.replace("[", "").replace("]", "");
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", actualExternalIdRedeemable);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		pageObj.utils().logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		pageObj.utils().logPass("API v2 User Signup call is successful");

		// Send reward to user
		pageObj.utils().logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		pageObj.utils().logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		pageObj.utils().logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		pageObj.utils().logPass("Reward Id for user is fetched: " + rewardId);

		// POS API-> Add Reward into discount basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationKey"), userId, "reward", rewardId, externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");

		String actualDiscountBasketItemIDFromBasket = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");

		// -----Hit POS discount lookup API-----

		// Add multiple items dynamically
		List<Map<String, Object>> lineItems = new ArrayList<>();
		Map<String, Object> receiptItems = new HashMap<String, Object>();

		// Sandwich|1||M|10|306|522|1.0

		receiptItems = ApiPayloads.getInputForReceiptItems(dataSet.get("item_name"),
				Integer.parseInt(dataSet.get("item_qty")), Double.parseDouble(dataSet.get("amount")),
				dataSet.get("item_type"), dataSet.get("item_id"), dataSet.get("item_family"), dataSet.get("item_group"),
				dataSet.get("serial_number"));
		lineItems.add(receiptItems);

		String receipt_datetime = CreateDateTime.getCurrentDateTimeInUtc();
		String punchh_key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		Response posDiscountLookupResponse = pageObj.endpoints().posDiscountLookupAPIInputPayload(lineItems,
				receipt_datetime, Double.parseDouble(dataSet.get("subtotal_amount")),
				Double.parseDouble(dataSet.get("receipt_amount")), punchh_key, transaction_no, userId,
				dataSet.get("locationKey"), externalUID);

		Assert.assertEquals(posDiscountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Verify Locked value in POS Discount Lookup API response
		String lockedPOSDiscountLookup = posDiscountLookupResponse.jsonPath().get("locked").toString();
		Assert.assertEquals(lockedPOSDiscountLookup, "true", "locked value-TRUE is present in API response");

		// api/mobile Api-> Add Reward into discount basket
		Response secureApiDiscountBasketResponse = pageObj.endpoints().secureApiDiscountBasketAdded(token,
				dataSet.get("client"), dataSet.get("secret"), "reward", rewardId, "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, secureApiDiscountBasketResponse.getStatusCode(),
				"Status code 422 did not match with add discount to basket ");
		String addDiscountError = secureApiDiscountBasketResponse.jsonPath().get("errors[0]");
		System.out.println(addDiscountError);
		Assert.assertEquals(addDiscountError, MessagesConstants.basketLockedErrMsg, "Error message did not match");
		logger.info("Verified the error message in Mobile Select API is basket gets locked");

		// GET ACTIVE DISCOUNT BASKET
		Response getBasketDiscountDetailsResponse = pageObj.endpoints().getDiscountBasketByUIDUsingApiMobile(token,
				dataSet.get("client"), dataSet.get("secret"), "");
		String locked = getBasketDiscountDetailsResponse.jsonPath().get("locked").toString();

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, getBasketDiscountDetailsResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		Assert.assertEquals(locked, "true", "locked value is missing from API response");
		logger.info("locked value-TRUE is present in GET API response");
		logger.info("Verified the error message in Mobile GET API is basket gets locked");

		// DELETE Items from DISCOUNT BASKET
		Response deleteBasketResponse = pageObj.endpoints().deleteMobileDiscountBasketByUID(token,
				dataSet.get("client"), dataSet.get("secret"), actualDiscountBasketItemIDFromBasket, "");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, deleteBasketResponse.getStatusCode(),
				"Status code 422 did not match with add discount to basket ");
		String deleteItemError = deleteBasketResponse.jsonPath().get("errors[0]");
		Assert.assertEquals(deleteItemError, MessagesConstants.basketLockedErrMsg, "Error message did not match");
		logger.info("Verified the error message in Mobile Delete API is basket gets locked");

		// Delete from redeemable tables

		String deleteRedeemableQuery1 = deleteRedeemableQuery
				.replace("${redeemableExternalID}", actualExternalIdRedeemable)
				.replace("${businessID}", dataSet.get("business_id"));

		DBUtils.executeQuery(env, deleteRedeemableQuery1);

		pageObj.utils().logPass(actualExternalIdRedeemable + " external redeemable is deleted successfully");

	}

	@Test(description = "OMM-T4216 (1.0) API2 Mobile Select>Validate that an error message appears if the basket is locked via POS and the user tries to add a new discount type using the API2/Mobile SELECT API without external_id or with an invalid external_id"
			+ "OMM-T4239 (1.0) API2 GET Active>Validate that no error message appears if the basket is locked via POS and the user tries to fetch basket details using the API/Mobile GET Active API without external_id or with an invalid external_id"
			+ "OMM-T4235 (1.0) API2 Mobile Unselect>Validate that an error message appears if the basket is locked via POS and the user tries to remove a new discount type using the API2/Mobile Unselect API without external_id or with an invalid external_id", priority = 1)
	@Owner(name = "Rahul Garg")

	public void T4216_T4239_T4235_API2MobileAfterBasketIsLocked() throws Exception {

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.dashboardpage().updateCheckBox();

		// Create Redeemable
		String redeemableName = "AutomationRedeemableExistingQC_API_" + CreateDateTime.getTimeDateString();
		String endTime = CreateDateTime.getFutureDateTimeInGivenFormate(1, "yyyy-MM-dd'T'HH:mm:ss");

		Map<String, String> redeemableMap = new HashMap<String, String>();
		redeemableMap.put("redeemableName", redeemableName);
		redeemableMap.put("external_id", "");
		redeemableMap.put("locationID", null);
		redeemableMap.put("external_id_redeemable", "");
		redeemableMap.put("qualifier_type", "flat_discount");
		redeemableMap.put("discount_amount", "10.0");
		redeemableMap.put("end_time", endTime);
		redeemableMap.put("lineitemSelector", dataSet.get("lineItemSelector"));

		// Step2 Qualifier type is provided but invalid redeeming_criterion
		Response responsRedeemable = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), redeemableMap);
		Assert.assertEquals(responsRedeemable.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actualExternalIdRedeemable = responsRedeemable.jsonPath().getString("results[0].external_id")
				.replace("[", "").replace("]", "");
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", actualExternalIdRedeemable);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		pageObj.utils().logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		pageObj.utils().logPass("API v2 User Signup call is successful");

		// Send reward to user
		pageObj.utils().logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		pageObj.utils().logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		pageObj.utils().logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		pageObj.utils().logPass("Reward Id for user is fetched: " + rewardId);

		// POS API-> Add Reward into discount basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationKey"), userId, "reward", rewardId, externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");

		String actualDiscountBasketItemIDFromBasket = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");

		// -----Hit POS discount lookup API-----

		// Add multiple items dynamically
		List<Map<String, Object>> lineItems = new ArrayList<>();
		Map<String, Object> receiptItems = new HashMap<String, Object>();

		// Sandwich|1||M|10|306|522|1.0

		receiptItems = ApiPayloads.getInputForReceiptItems(dataSet.get("item_name"),
				Integer.parseInt(dataSet.get("item_qty")), Double.parseDouble(dataSet.get("amount")),
				dataSet.get("item_type"), dataSet.get("item_id"), dataSet.get("item_family"), dataSet.get("item_group"),
				dataSet.get("serial_number"));
		lineItems.add(receiptItems);

		String receipt_datetime = CreateDateTime.getCurrentDateTimeInUtc();
		String punchh_key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		Response posDiscountLookupResponse = pageObj.endpoints().posDiscountLookupAPIInputPayload(lineItems,
				receipt_datetime, Double.parseDouble(dataSet.get("subtotal_amount")),
				Double.parseDouble(dataSet.get("receipt_amount")), punchh_key, transaction_no, userId,
				dataSet.get("locationKey"), externalUID);

		Assert.assertEquals(posDiscountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Verify Locked value in POS Discount Lookup API response
		String lockedPOSDiscountLookup = posDiscountLookupResponse.jsonPath().get("locked").toString();
		Assert.assertEquals(lockedPOSDiscountLookup, "true", "locked value-TRUE is present in API response");

		// api2 mobile Api-> Add Reward into discount basket

		Response addDiscountBasketResponse = pageObj.endpoints().addDiscountToBasketWithExtIdAPI2(token,
				dataSet.get("client"), dataSet.get("secret"), "reward", rewardId, "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, addDiscountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		String addError = addDiscountBasketResponse.jsonPath().get("errors.locked_basket").toString();
		Assert.assertEquals(addError, MessagesConstants.basketLockedErrMsg, "Error message did not match");
		logger.info("Verified the error message in API2 Mobile Select API is basket gets locked");

		// GET ACTIVE DISCOUNT BASKET

		Response getBasketDiscountDetailsResponse = pageObj.endpoints().getDiscountBasketByUIDUsingApi2Mobile(token,
				dataSet.get("client"), dataSet.get("secret"), "");

		Assert.assertEquals(getBasketDiscountDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API Mobile Get Active Basket");
		String locked = getBasketDiscountDetailsResponse.jsonPath().get("locked").toString();
		Assert.assertEquals(locked, "true", "locked value is missing from API response");
		logger.info("locked value-TRUE is present in GET API response");
		logger.info("Verified the error message in API2 Mobile GET API is basket gets locked");

		// DELETE Items from DISCOUNT BASKET

		Response deleteBasketResponse = pageObj.endpoints().deleteMobileDiscountBasketByUIDApi2(token,
				dataSet.get("client"), dataSet.get("secret"), actualDiscountBasketItemIDFromBasket, "");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, deleteBasketResponse.getStatusCode());

		String unselectError = deleteBasketResponse.jsonPath().get("errors.locked_basket").toString();
		Assert.assertEquals(unselectError, MessagesConstants.basketLockedErrMsg, "Error message did not match");
		logger.info("Verified the error message in API2 Mobile Delete API is basket gets locked");

		// Delete from redeemable tables

		String deleteRedeemableQuery1 = deleteRedeemableQuery
				.replace("${redeemableExternalID}", actualExternalIdRedeemable)
				.replace("${businessID}", dataSet.get("business_id"));

		DBUtils.executeQuery(env, deleteRedeemableQuery1);

		pageObj.utils().logPass(actualExternalIdRedeemable + " external redeemable is deleted successfully");

	}

	@Test(description = "SQ-T6075: Validate the presence of \"Locked\" Boolean status flag in API response for POS - Add Selection to Discount Basket Redemptions 2.0.; "
			+ "SQ-T6081: POS >Validate the presence of \"Locked\" Boolean status flag in API response for GET Active discount Basket; "
			+ "SQ-T6076: Verify the \"Locked\" status flag in API response for POS - Remove Item from Discount Basket Redemptions 2.0.; "
			+ "SQ-T6083: POS >Validate the presence of \"Locked\" Boolean status flag in API response for Auto Redemption API", groups = {
					"regression", "dailyrun" })
	@Owner(name = "Vaibhav Agnihotri")
	public void verifyPosApiRewardLocking() throws Exception {
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();

		// Go to business where Reward Locking is Off
		pageObj.utils().logit("== Reward Locking is disabled: " + dataSet.get("slug") + " ==");
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reward Locking", "uncheck");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Auto-redemption", "check");

		// User Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		pageObj.utils().logPass("API2 User Signup is successful with user ID: " + userID);

		// Send reward to user and fetch reward id
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemableId"), "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemableId"));
		pageObj.utils().logPass("Reward ID: " + rewardId + " is fetched for user ID: " + userID);

		// POS Add Selection to Discount Basket without external UID
		Response addDiscountBasketResponse = pageObj.endpoints()
				.authListDiscountBasketAddedForPOSAPI(dataSet.get("locationkey"), userID, "reward", rewardId);
		Assert.assertEquals(addDiscountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean addDiscountBasketLocked = addDiscountBasketResponse.jsonPath().get("locked");
		Assert.assertEquals(addDiscountBasketLocked, false,
				"Locked value is not as expected in POS Add Discount to Basket API response");
		pageObj.utils().logPass("POS Add Selection to Discount Basket API is successful and locked has value false");

		// POS Discount Lookup without external UID
		Response discountLookupResponse = pageObj.endpoints().POSDiscountLookup(dataSet.get("locationkey"), userID,
				dataSet.get("itemId"));
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean discountLookupLocked = discountLookupResponse.asString().contains("locked");
		Assert.assertEquals(discountLookupLocked, false, "Locked key is found in POS Discount Lookup API response");
		pageObj.utils().logPass("POS Discount Lookup API is successful and doesn't have locked key");

		// POS Auto Redemption without external UID
		Response autoRedemptionResponse = pageObj.endpoints().autoSelectPosApi(userID, "30", "1", "12003", "",
				dataSet.get("locationkey"));
		Assert.assertEquals(autoRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean autoRedemptionLocked = autoRedemptionResponse.jsonPath().get("locked");
		Assert.assertEquals(autoRedemptionLocked, false, "POS Auto Redemption API locked value is not false");
		pageObj.utils().logPass("POS Auto Redemption API is successful and locked has value false");

		// POS Get Active Discount Basket without external UID
		Response posGetActiveBasketResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingPOS(userID,
				dataSet.get("locationkey"));
		Assert.assertEquals(posGetActiveBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean posGetActiveBasketLocked = posGetActiveBasketResponse.jsonPath().get("locked");
		String discountBasketItemId = posGetActiveBasketResponse.jsonPath()
				.getString("discount_basket_items[0].discount_basket_item_id");
		Assert.assertEquals(posGetActiveBasketLocked, false,
				"POS Get Active Discount Basket API locked value is not false");
		pageObj.utils().logPass("POS Get Active Discount Basket API is successful and locked has value false");

		// POS Remove Item from Discount Basket without external UID
		Response posRemoveItemFromBasketResponse = pageObj.endpoints()
				.deleteDiscountBasketForUserPOS(dataSet.get("locationkey"), userID, discountBasketItemId);
		Assert.assertEquals(posRemoveItemFromBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean posRemoveItemFromBasketLocked = posRemoveItemFromBasketResponse.jsonPath().get("locked");
		Assert.assertEquals(posRemoveItemFromBasketLocked, false,
				"POS Remove Item from Discount Basket API locked value is not false");
		pageObj.utils().logPass("POS Remove Item from Discount Basket API is successful and locked has value false");

		// Now go to business where Reward Locking is On
		pageObj.utils().logit("== Reward Locking is enabled: " + dataSet.get("slug2") + " ==");
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug2"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reward Locking", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Auto-redemption", "check");

		// User Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client2"),
				dataSet.get("secret2"));
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token2 = signUpResponse2.jsonPath().get("access_token.token").toString();
		String userID2 = signUpResponse2.jsonPath().get("user.user_id").toString();
		pageObj.utils().logPass("API2 User Signup is successful with user ID: " + userID2);

		// Send reward to user and fetch reward id
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID2, dataSet.get("apiKey2"), "",
				dataSet.get("redeemableId2"), "", "");
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		rewardId2 = pageObj.redeemablesPage().getRewardId(token2, dataSet.get("client2"), dataSet.get("secret2"),
				dataSet.get("redeemableId2"));
		pageObj.utils().logPass("Reward ID: " + rewardId2 + " is fetched for user ID: " + userID2);

		// POS Add Selection to Discount Basket
		String externalUID2 = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		Response addDiscountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationKey2"), userID2, "reward", rewardId2, externalUID2);
		Assert.assertEquals(addDiscountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean addDiscountBasketLocked2 = addDiscountBasketResponse2.jsonPath().get("locked");
		Assert.assertEquals(addDiscountBasketLocked2, false,
				"Locked value is not as expected in POS Add Selection to Discount Basket API response");
		pageObj.utils().logPass("POS Add Selection to Discount Basket API is successful and locked has value false");

		// POS Discount Lookup
		Response discountLookupResponse2 = pageObj.endpoints().discountLookUpPosApi(dataSet.get("locationKey2"),
				userID2, dataSet.get("itemId2"), "10", externalUID2);
		Assert.assertEquals(discountLookupResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean discountLookupLocked2 = discountLookupResponse2.jsonPath().get("locked");
		Assert.assertEquals(discountLookupLocked2, true,
				"Locked value is not as expected in POS Discount Lookup API response");
		pageObj.utils().logPass("POS Discount Lookup API is successful and locked has value true");

		// POS Auto Redemption
		Response autoRedemptionResponse2 = pageObj.endpoints().autoSelectPosApi(userID2, "30", "1", "12003",
				externalUID2, dataSet.get("locationKey2"));
		Assert.assertEquals(autoRedemptionResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean autoRedemptionLocked2 = autoRedemptionResponse2.jsonPath().get("locked");
		Assert.assertEquals(autoRedemptionLocked2, true, "POS Auto Redemption API locked value is not true");
		pageObj.utils().logPass("POS Auto Redemption API is successful and locked has value true");

		// Send reward to user again and fetch reward id
		sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID2, dataSet.get("apiKey2"), "",
				dataSet.get("redeemableId2"), "", "");
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		rewardId2 = pageObj.redeemablesPage().getRewardId(token2, dataSet.get("client2"), dataSet.get("secret2"),
				dataSet.get("redeemableId2"));
		pageObj.utils().logPass("Reward ID: " + rewardId2 + " is fetched for user ID: " + userID2);

		// POS Add Selection to Discount Basket again with same external UID
		addDiscountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationKey2"), userID2, "reward", rewardId2, externalUID2);
		Assert.assertEquals(addDiscountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		addDiscountBasketLocked2 = addDiscountBasketResponse2.jsonPath().get("locked");
		Assert.assertEquals(addDiscountBasketLocked2, true,
				"Locked value is not as expected in POS Add Selection to Discount Basket API response");
		pageObj.utils().logPass("POS Add Selection to Discount Basket API is successful and locked has value true");

		// POS Get Active Discount Basket
		Response posGetActiveBasketResponse2 = pageObj.endpoints().fetchActiveBasketPOSAPI(userID2,
				dataSet.get("locationKey2"), externalUID2);
		Assert.assertEquals(posGetActiveBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean posGetActiveBasketLocked2 = posGetActiveBasketResponse2.jsonPath().get("locked");
		String discountBasketItemId2 = posGetActiveBasketResponse2.jsonPath()
				.getString("discount_basket_items[0].discount_basket_item_id");
		Assert.assertEquals(posGetActiveBasketLocked2, true,
				"POS Get Active Discount Basket API locked value is not true");
		pageObj.utils().logPass("POS Get Active Discount Basket API is successful and locked has value true");

		// POS Remove Item from Discount Basket
		Response posRemoveItemFromBasketResponse2 = pageObj.endpoints().removeDiscountFromBasketPOSAPI(
				dataSet.get("locationKey2"), userID2, discountBasketItemId2, externalUID2);
		Assert.assertEquals(posRemoveItemFromBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean posRemoveItemFromBasketLocked2 = posRemoveItemFromBasketResponse2.jsonPath().get("locked");
		Assert.assertEquals(posRemoveItemFromBasketLocked2, true,
				"POS Remove Item from Discount Basket API locked value is not true");
		pageObj.utils().logPass("POS Remove Item from Discount Basket API is successful and locked has value true");

	}

	@Test(description = "SQ-T6084: Auth >Validate the presence of \"Locked\" Boolean status flag in API response for Auto Redemption API; "
			+ "SQ-T6082: Auth >Validate the presence of \"Locked\" Boolean status flag in API response for GET Active discount Basket; "
			+ "SQ-T6080: Auth >Validate the presence of \"Locked\" Boolean status flag in API response for Remove item from Basket")
	@Owner(name = "Vaibhav Agnihotri")
	public void verifyAuthApiRewardLocking() throws Exception {

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap = new HashMap<String, String>();

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();

		// Go to business where Reward Locking is Off
		pageObj.utils().logit("== Reward Locking is disabled: " + dataSet.get("slug") + " ==");
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reward Locking", "uncheck");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Auto-redemption", "check");

		// User Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		pageObj.utils().logPass("API2 User Signup is successful with user ID: " + userID);

		// Send reward to user and fetch reward id
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemableId"), "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemableId"));
		pageObj.utils().logPass("Reward ID: " + rewardId + " is fetched for user ID: " + userID);

		// Auth Add Selection to Discount Basket without external UID
		Response addDiscountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), "reward", rewardId);
		Assert.assertEquals(addDiscountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean addDiscountBasketLocked = addDiscountBasketResponse.jsonPath().get("locked");
		Assert.assertEquals(addDiscountBasketLocked, false,
				"Locked value is not as expected in Auth Add Selection to Discount Basket API response");
		pageObj.utils().logPass("Auth Add Selection to Discount Basket API is successful and locked has value false");

		// POS Discount Lookup without external UID
		Response discountLookupResponse = pageObj.endpoints().POSDiscountLookup(dataSet.get("locationkey"), userID,
				dataSet.get("itemId"));
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean discountLookupLocked = discountLookupResponse.asString().contains("locked");
		Assert.assertEquals(discountLookupLocked, false, "Locked key is found in POS Discount Lookup API response");
		pageObj.utils().logPass("POS Discount Lookup API is successful and doesn't have locked key");

		// Auth Auto Redemption without external UID
		detailsMap = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "10", "M", "10", "999", "1",
				dataSet.get("itemId"));
		parentMap.put("Pizza1", detailsMap);
		Response authAutoRedemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "10", "", parentMap);
		Assert.assertEquals(authAutoRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean authAutoRedemptionLocked = authAutoRedemptionResponse.jsonPath().get("locked");
		Assert.assertEquals(authAutoRedemptionLocked, false,
				"Locked value is not as expected in Auth Auto Redemption API response");
		pageObj.utils().logPass("Auth Auto Redemption API is successful and locked has value false");

		// Auth Get Active Discount Basket without external UID
		Response authGetActiveBasketResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingAUTH(token,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(authGetActiveBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean authGetActiveBasketLocked = authGetActiveBasketResponse.jsonPath().get("locked");
		String discountBasketItemId = authGetActiveBasketResponse.jsonPath()
				.getString("discount_basket_items[0].discount_basket_item_id");
		Assert.assertEquals(authGetActiveBasketLocked, false,
				"Auth Get Active Discount Basket API locked value is not false");
		pageObj.utils().logPass("Auth Get Active Discount Basket API is successful and locked has value false");

		// Auth Remove Item from Discount Basket without external UID
		Response authRemoveItemFromBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), discountBasketItemId);
		Assert.assertEquals(authRemoveItemFromBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean authRemoveItemFromBasketLocked = authRemoveItemFromBasketResponse.jsonPath().get("locked");
		Assert.assertEquals(authRemoveItemFromBasketLocked, false,
				"Auth Remove Item from Discount Basket API locked value is not false");
		pageObj.utils().logPass("Auth Remove Item from Discount Basket API is successful and locked has value false");

		// Now go to business where Reward Locking is On
		pageObj.utils().logit("== Reward Locking is enabled: " + dataSet.get("slug2") + " ==");
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug2"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reward Locking", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Auto-redemption", "check");

		// User Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client2"),
				dataSet.get("secret2"));
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token2 = signUpResponse2.jsonPath().get("access_token.token").toString();
		String userID2 = signUpResponse2.jsonPath().get("user.user_id").toString();
		pageObj.utils().logPass("API2 User Signup is successful with user ID: " + userID2);

		// Send reward to user and fetch reward id
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID2, dataSet.get("apiKey2"), "",
				dataSet.get("redeemableId2"), "", "");
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		rewardId2 = pageObj.redeemablesPage().getRewardId(token2, dataSet.get("client2"), dataSet.get("secret2"),
				dataSet.get("redeemableId2"));
		pageObj.utils().logPass("Reward ID: " + rewardId2 + " is fetched for user ID: " + userID2);

		// Auth Add Selection to Discount Basket
		String externalUID2 = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		Response addDiscountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationKey2"), userID2, "reward", rewardId2, externalUID2);
		Assert.assertEquals(addDiscountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean addDiscountBasketLocked2 = addDiscountBasketResponse2.jsonPath().get("locked");
		Assert.assertEquals(addDiscountBasketLocked2, false,
				"Locked value is not as expected in Auth Add Selection to Discount Basket API response");
		pageObj.utils().logPass("Auth Add Selection to Discount Basket API is successful and locked has value false");

		// POS Discount Lookup
		Response discountLookupResponse2 = pageObj.endpoints().discountLookUpPosApi(dataSet.get("locationKey2"),
				userID2, dataSet.get("itemId2"), "10", externalUID2);
		Assert.assertEquals(discountLookupResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean discountLookupLocked2 = discountLookupResponse2.jsonPath().get("locked");
		Assert.assertEquals(discountLookupLocked2, true,
				"Locked value is not as expected in POS Discount Lookup API response");
		pageObj.utils().logPass("POS Discount Lookup API is successful and locked has value true");

		// Auth Auto Redemption
		Response authAutoRedemptionResponse2 = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client2"),
				dataSet.get("secret2"), token2, "10", externalUID2, parentMap);
		Assert.assertEquals(authAutoRedemptionResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean authAutoRedemptionLocked2 = authAutoRedemptionResponse2.jsonPath().get("locked");
		Assert.assertEquals(authAutoRedemptionLocked2, true,
				"Locked value is not as expected in Auth Auto Redemption API response");
		pageObj.utils().logPass("Auth Auto Redemption API is successful and locked has value true");

		// Auth Get Active Discount Basket
		Response authGetActiveBasketResponse2 = pageObj.endpoints().fetchActiveBasketAuthApi(token2,
				dataSet.get("client2"), dataSet.get("secret2"), externalUID2);
		Assert.assertEquals(authGetActiveBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean authGetActiveBasketLocked2 = authGetActiveBasketResponse2.jsonPath().get("locked");
		String discountBasketItemId2 = authGetActiveBasketResponse2.jsonPath()
				.getString("discount_basket_items[0].discount_basket_item_id");
		Assert.assertEquals(authGetActiveBasketLocked2, true,
				"Auth Get Active Discount Basket API locked value is not true");
		pageObj.utils().logPass("Auth Get Active Discount Basket API is successful and locked has value true");

		// Auth Remove Item from Discount Basket
		Response authRemoveItemFromBasketResponse2 = pageObj.endpoints().deleteDiscountBasketForUserWithExt_UidAUTH(
				token2, dataSet.get("client2"), dataSet.get("secret2"), discountBasketItemId2, externalUID2);
		Assert.assertEquals(authRemoveItemFromBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean authRemoveItemFromBasketLocked2 = authRemoveItemFromBasketResponse2.jsonPath().get("locked");
		Assert.assertEquals(authRemoveItemFromBasketLocked2, true,
				"Auth Remove Item from Discount Basket API locked value is not true");
		pageObj.utils().logPass("Auth Remove Item from Discount Basket API is successful and locked has value true");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws SQLException {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
