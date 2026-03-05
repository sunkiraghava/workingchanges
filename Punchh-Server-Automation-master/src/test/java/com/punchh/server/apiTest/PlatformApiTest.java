package com.punchh.server.apiTest;

import java.lang.reflect.Method;
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
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class PlatformApiTest {
	static Logger logger = LogManager.getLogger(PlatformApiTest.class);
	public WebDriver driver;
	ApiUtils apiUtils;
	String userEmail;
	// Properties uiProp;
	Properties prop;
	String punchKey, amount;
	PageObj pageObj;
	String sTCName;
	String run = "api";
	// String adminAuthorization = "19Ez5ypiztii6J6we3J1";// pp
	// amit.kumar+8@punchh.com Business Owner/Manager SuperAdmin
	// 19Ez5ypiztii6J6we3J1
	// QA amit.kumar+8@punchh.com Business Owner/Manager SuperAdmin
	// SA2hZdsLzzuQGkvKc76q
	private String env;
	private Utilities utils;
	private static Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void beforeMethod(Method method) {

		// uiProp = Utilities.loadPropertiesFile("config.properties");
		apiUtils = new ApiUtils();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		apiUtils = new ApiUtils();
		utils = new Utilities();
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);

	}

	@Test(description = "SQ-T2815 wifi Api Validation", groups = "api", priority = 0)
	public void wifiApiValidation() {
		utils.logit("== Wifi user signup Test ==");
		String UserEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().wifiUserSignUp(UserEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("locationId"));
		apiUtils.verifyProcessResponse(signUpResponse, "Wifi user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED);
		utils.logPass("Verified user singup successfull");
		// Enroll Guests For WiFi
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response enrollGuestWifiResponse = pageObj.endpoints().enrollGuestForWifi(userEmail, dataSet.get("client"),
				dataSet.get("locationId"), dataSet.get("adminAuthorization"));
		Assert.assertEquals(enrollGuestWifiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 did not matched for PLATFORM FUNCTIONS API Enroll Guests For WiFi");
		utils.logPass("PLATFORM FUNCTIONS API Enroll Guests For WiFi is successful");
		// Guest Lookup For WiFi Enrollment, Returns whether a guest exists in the
		// business or not.
		utils.logit("== Guest Lookup For WiFi Enrollment==");
		Response getwifiUserlookup = pageObj.endpoints().guestLookupWifiEnrollment(userEmail, dataSet.get("client"),
				dataSet.get("adminAuthorization"));
		apiUtils.verifyResponse(getwifiUserlookup, "POS user signup");
		Assert.assertEquals(getwifiUserlookup.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logPass("Verified Guest Lookup For WiFi Enrollment, user exists in buisness");
	}

	@Test(description = "SQ-T2812 locations Api Validation", groups = "api", priority = 1)
	public void locationsApiValidation() {

		String store_number = CreateDateTime.getTimeDateString();
		String location_name = "Test Location" + store_number;
		String locationGroupName = "Location Group" + store_number;

		// Create Location Api
		Response createLocationresponse = pageObj.endpoints().createLocation(location_name, store_number,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(createLocationresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Location");
		String location_id = createLocationresponse.jsonPath().get("location_id").toString();
		String storeNumber = createLocationresponse.jsonPath().get("store_number").toString();

		boolean isCreateLocationSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardCreateLocationSchema, createLocationresponse.asString());
		Assert.assertTrue(isCreateLocationSchemaValidated,
				"Platform Functions Create Location Schema Validation failed");
		utils.logPass("PLATFORM FUNCTIONS API Create Location is successful");
		Assert.assertFalse(createLocationresponse.asString().contains("enable_daily_redemption_report"));
		utils.logPass("Verfied enable_daily_redemption_report flag is not appearing in create-location api response");

		// Get Location List
		Response getLocationresponse = pageObj.endpoints().getLocationList(dataSet.get("adminAuthorization"));
		Assert.assertEquals(getLocationresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Location List");
		boolean isGetLocationListSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.dashboardGetLocationListSchema, getLocationresponse.asString());
		Assert.assertTrue(isGetLocationListSchemaValidated,
				"Platform Functions Get Location List Schema Validation failed");
		utils.logPass("PLATFORM FUNCTIONS API Get Location List is successful");
		Assert.assertFalse(getLocationresponse.asString().contains("enable_daily_redemption_report"));
		utils.logPass("Verfied enable_daily_redemption_report flag is not appearing in get-location api response");

		// Update Location
		Response updateLocationresponse = pageObj.endpoints().updateLocation(location_id, storeNumber,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(updateLocationresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Update Location");
		boolean isUpdateLocationSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardUpdateLocationSchema, updateLocationresponse.asString());
		Assert.assertTrue(isUpdateLocationSchemaValidated,
				"Platform Functions Update Location Schema Validation failed");
		utils.logPass("PLATFORM FUNCTIONS API Update Location is successful");
		Assert.assertFalse(updateLocationresponse.asString().contains("enable_daily_redemption_report"));
		utils.logPass("Verfied enable_daily_redemption_report flag is not appearing in update-location api response");

		// Get Location Details
		Response getLocationDetailsresponse = pageObj.endpoints().getLocationDetails(location_id, storeNumber,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(getLocationDetailsresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Location Details");
		boolean isGetLocationDetailsSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.dashboardGetLocationListSchema, getLocationDetailsresponse.asString());
		Assert.assertTrue(isGetLocationDetailsSchemaValidated,
				"Platform Functions Get Location Details Schema Validation failed");
		utils.logPass("PLATFORM FUNCTIONS API Get Location Details is successful");
		Assert.assertFalse(getLocationDetailsresponse.asString().contains("enable_daily_redemption_report"));
		utils.logPass(
				"Verfied enable_daily_redemption_report flag is not appearing in update-location-details api response");

		// Get Location Group List
		Response getLocationGroupListresponse = pageObj.endpoints()
				.getLocationGroupList(dataSet.get("adminAuthorization"));
		Assert.assertEquals(getLocationGroupListresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Location Group List");
		boolean isGetLocationGroupListSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.dashboardGetLocationGroupListSchema, getLocationGroupListresponse.asString());
		Assert.assertTrue(isGetLocationGroupListSchemaValidated,
				"Platform Functions Get Location Group List Schema Validation failed");
		utils.logPass("PLATFORM FUNCTIONS API Get Location Group List is successful");
		String location_group_id = getLocationGroupListresponse.jsonPath().get("[0].location_group_id").toString();

		// Get Location Group Details
		Response getLocationGroupDetailsresponse = pageObj.endpoints().getLocationGroupDetails(location_group_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(getLocationGroupDetailsresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Location Group Details");
		boolean isGetLocationGroupDetailsSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.dashboardGetLocationGroupListSchema, getLocationGroupDetailsresponse.asString());
		Assert.assertTrue(isGetLocationGroupDetailsSchemaValidated,
				"Platform Functions Get Location Group Details Schema Validation failed");
		utils.logPass("PLATFORM FUNCTIONS API Get Location Group Details is successful");

		// Add Location to Location Group
		Response addLocationtoGroupresponse = pageObj.endpoints().addLocationtoGroup(dataSet.get("adminAuthorization"),
				location_group_id, store_number, location_id);
		Assert.assertEquals(addLocationtoGroupresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 202 did not matched for PLATFORM FUNCTIONS API Add Location to Location Group");
		boolean isAddLocationToGroupSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardCreateLocationGroupSchema, addLocationtoGroupresponse.asString());
		Assert.assertTrue(isAddLocationToGroupSchemaValidated,
				"Platform Functions Add Location to Location Group Schema Validation failed");
		utils.logPass("PLATFORM FUNCTIONS API Add Location to Location Group is successful");

		// Create Location Group
		Response createLocationGroupresponse = pageObj.endpoints().createLocationgroup(locationGroupName, storeNumber,
				location_id, dataSet.get("adminAuthorization"));
		Assert.assertEquals(createLocationGroupresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Location Group");
		boolean isCreateLocationGroupSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardCreateLocationGroupSchema, createLocationGroupresponse.asString());
		Assert.assertTrue(isCreateLocationGroupSchemaValidated,
				"Platform Functions Create Location Group Schema Validation failed");
		utils.logPass("PLATFORM FUNCTIONS API Create Location Group is successful");
		String locationGroup_id = createLocationGroupresponse.jsonPath().get("location_group_id").toString();

		// Update Location Group
		Response updateLocationGroupresponse = pageObj.endpoints().updateLocationgroup(locationGroupName,
				locationGroup_id, dataSet.get("adminAuthorization"));
		Assert.assertEquals(updateLocationGroupresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Update Location Group");
		boolean isUpdateLocationGroupSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardCreateLocationGroupSchema, updateLocationGroupresponse.asString());
		Assert.assertTrue(isUpdateLocationGroupSchemaValidated,
				"Platform Functions Update Location Group Schema Validation failed");
		utils.logPass("PLATFORM FUNCTIONS API Update Location Group is successful");

		// Delete Location Group
		Response deleteLocationGroupresponse = pageObj.endpoints().deleteLocationgroup(locationGroup_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deleteLocationGroupresponse.getStatusCode(), ApiConstants.HTTP_STATUS_NO_CONTENT,
				"Status code 204 did not matched for PLATFORM FUNCTIONS API Delete Location Group");
		utils.logPass("PLATFORM FUNCTIONS API Delete Location Group is successful");

		// Delete Location from Location Group
		Response deleteLocationfromGroupresponse = pageObj.endpoints().deleteLocationfromGroup(
				dataSet.get("adminAuthorization"), location_group_id, store_number, location_id);
		Assert.assertEquals(deleteLocationfromGroupresponse.getStatusCode(), ApiConstants.HTTP_STATUS_NO_CONTENT,
				"Status code 204 did not matched for PLATFORM FUNCTIONS API Delete Location from Location Group");
		utils.logPass("PLATFORM FUNCTIONS API Delete Location from Location Group is successful");

		// Delete location
		Response deleteLocationresponse = pageObj.endpoints().deleteLocation(location_id, storeNumber,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deleteLocationresponse.getStatusCode(), ApiConstants.HTTP_STATUS_NO_CONTENT,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Delete location");
		utils.logPass("PLATFORM FUNCTIONS API Delete locationis successful");
	}

	@Test(description = "SQ-T2814 users Api Validation", groups = "api", priority = 2)
	public void usersApiValidation() {

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("Api2 user signup is successful");

		// Ban a User
		Response banUserresponse = pageObj.endpoints().banUser(userID, dataSet.get("adminAuthorization"));
		Assert.assertEquals(banUserresponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Ban a User");
		utils.logPass("PLATFORM FUNCTIONS API Ban a User is successful");

		// UnBan a User
		Response unBanUserresponse = pageObj.endpoints().unBanUser(userID, dataSet.get("adminAuthorization"));
		Assert.assertEquals(unBanUserresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API UnBan a User");
		utils.logPass("PLATFORM FUNCTIONS API UnBan a User is successful");

		// Send Message to User (Parametrize for other envs)
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				dataSet.get("amount"), dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API send message to user");
		utils.logPass("PLATFORM FUNCTIONS API Send Message to User is successful");

		// Support Gifting to a User
		Response supportGiftingResponse = pageObj.endpoints().supportGiftingToUser(userID,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(supportGiftingResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Support Gifting to a User");
		utils.logPass("PLATFORM FUNCTIONS API Support Gifting to a User is successful");

		// Deactivate a User
		Response deactivateUserresponse = pageObj.endpoints().deactivateUser(userID, dataSet.get("adminAuthorization"));
		Assert.assertEquals(deactivateUserresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Deactivate a User");
		utils.logPass("PLATFORM FUNCTIONS API Deactivate a User is successful");

		// Reactivate a User
		Response reactivateUserresponse = pageObj.endpoints().reactivateUser(userID, dataSet.get("adminAuthorization"));
		Assert.assertEquals(reactivateUserresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Reactivate a User");
		utils.logPass("PLATFORM FUNCTIONS API Reactivate a User is successful");

		// Get Location List
		Response getLocationresponse = pageObj.endpoints().getLocationList(dataSet.get("adminAuthorization"));
		Assert.assertEquals(getLocationresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Location List");
		utils.logPass("PLATFORM FUNCTIONS API Get Location List is successful");
		String locationid = getLocationresponse.jsonPath().get("[0].location_id").toString();

		// Update a User
		Response updateUserResponse = pageObj.endpoints().updateUser(userID, userEmail, locationid,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(updateUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Update a User");
		boolean isUpdateUserSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardUpdateUserSchema, updateUserResponse.asString());
		Assert.assertTrue(isUpdateUserSchemaValidated, "Platform Functions API Update User Schema Validation failed");
		utils.logPass("PLATFORM FUNCTIONS API Update a User is successful");

		// Get User Export
		Response userExportResponse = pageObj.endpoints().userExport(userID, dataSet.get("adminAuthorization"));
		Assert.assertEquals(userExportResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get User Export");
		utils.logPass("PLATFORM FUNCTIONS API Get User Export is successful");

		// Get Extended User History
		Response extendedUserHistoryResponse = pageObj.endpoints().extendedUserHistory(userID,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(extendedUserHistoryResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Extended User History");
		boolean isExtendedUserHistorySchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardExtendedUserHistorySchema, extendedUserHistoryResponse.asString());
		Assert.assertTrue(isExtendedUserHistorySchemaValidated,
				"Platform Functions API Get Extended User History Schema Validation failed");
		utils.logPass("PLATFORM FUNCTIONS API Get Extended User History is successful");

		// Fetch User Favourite Locations
		Response fetchUserLocationsResponse = pageObj.endpoints().fetchUserLocation(userID,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(fetchUserLocationsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Fetch User Favourite Locations");
		utils.logPass("PLATFORM FUNCTIONS API Fetch User Favourite Locations is successful");
		boolean isUserFavouriteLocationSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.dashboardUserFavouriteLocationSchema, fetchUserLocationsResponse.asString());
		Assert.assertTrue(isUserFavouriteLocationSchemaValidated,
				"Platform Functions API Fetch User Favourite Locations Schema Validation failed");
		String favourite_location_id = fetchUserLocationsResponse.jsonPath().get("[0].user_favourite_location_id")
				.toString();

		// Delete User Favourite Location
		Response deleteUserLocationsResponse = pageObj.endpoints().deleteUserLocation(userID, favourite_location_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deleteUserLocationsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Delete User Favourite Location");
		utils.logPass("PLATFORM FUNCTIONS API Delete User Favourite Location is successful");

		// Deactivate a User
		Response deactivateUserresponse1 = pageObj.endpoints().deactivateUser(userID,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deactivateUserresponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Deactivate a User");
		utils.logPass("PLATFORM FUNCTIONS API Deactivate a User is successful");

		// Delete a User
		Response deleteUserResponse = pageObj.endpoints().deleteUser(userID, dataSet.get("adminAuthorization"));
		Assert.assertEquals(deleteUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Delete a User");
		boolean isDeleteUserSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiMessageObjectSchema, deleteUserResponse.asString());
		Assert.assertTrue(isDeleteUserSchemaValidated, "Platform Functions API Delete User Schema Validation failed");
		utils.logPass("PLATFORM FUNCTIONS API Delete a User is successful");
	}

	@Test(description = "SQ-T2842 business Migration User Api Validation; SQ-T4751 Verify API2 Migration user look-up", groups = "api", priority = 3)
	public void businessMigrationUserApiValidation() {

		// Create Business Migration User
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserResponse = pageObj.endpoints().createBusinessMigrationUse(userEmail,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(createMigrationUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Migration User");
		boolean isCreateMigrationUserSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardBusinessMigrationUserSchema, createMigrationUserResponse.asString());
		Assert.assertTrue(isCreateMigrationUserSchemaValidated,
				"Platform Functions API Create Business Migration User Schema Validation failed");
		utils.logPass("PLATFORM FUNCTIONS API Create Business Migration User is successful");
		String migration_user_id = createMigrationUserResponse.jsonPath().get("migration_user_id").toString();

		// Verify API2 Migration user look-up
		String card_number = createMigrationUserResponse.jsonPath().get("original_membership_no");
		String email = createMigrationUserResponse.jsonPath().get("email").toString();
		Response migrationLookupResponse = pageObj.endpoints().api2MigrationLookup(card_number, email,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(migrationLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API2 migration look-up");
		boolean isMigrationLookupSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2MigrationUserLookupSchema, migrationLookupResponse.asString());
		Assert.assertTrue(isMigrationLookupSchemaValidated,
				"API v2 Basic Migration User Lookup Schema Validation failed");
		Assert.assertEquals(migrationLookupResponse.jsonPath().get("email").toString(), email,
				"User email did not match for API2 migration look-up");
		utils.logPass("API2 migration look-up call is successful.");

		// Update Business Migration User
		Response updateMigrationUserResponse = pageObj.endpoints().updateBusinessMigrationUse(userEmail,
				dataSet.get("adminAuthorization"), migration_user_id);
		Assert.assertEquals(updateMigrationUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Update Business Migration User");
		boolean isUpdateMigrationUserSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardBusinessMigrationUserSchema, updateMigrationUserResponse.asString());
		Assert.assertTrue(isUpdateMigrationUserSchemaValidated,
				"Platform Functions API Update Business Migration User Schema Validation failed");
		utils.logPass("PLATFORM FUNCTIONS API Update Business Migration User is successful");

		// Delete Business Migration User
		Response deleteMigrationUserResponse = pageObj.endpoints()
				.deleteBusinessMigrationUse(dataSet.get("adminAuthorization"), migration_user_id);
		Assert.assertEquals(deleteMigrationUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Delete Business Migration User");
		utils.logPass("PLATFORM FUNCTIONS API Delete Business Migration User is successful");

	}

	@Test(description = "SQ-T2852 business Admin Api Validation", groups = "api", priority = 4)
	public void businessAdminApiValidation() {

		// Get Admin Roles List
		Response getAdminRoleListResponse = pageObj.endpoints().getAdminRolesList(dataSet.get("adminAuthorization"));
		Assert.assertEquals(getAdminRoleListResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Admin Roles List");
		boolean isGetAdminRoleListSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.dashboardGetAdminRolesListSchema, getAdminRoleListResponse.asString());
		Assert.assertTrue(isGetAdminRoleListSchemaValidated,
				"Platform Functions API Get Admin Roles List Schema Validation failed");
		utils.logPass("PLATFORM FUNCTIONS API Get Get Admin Roles List is successful");
		String role_id = getAdminRoleListResponse.jsonPath().get("[0].role_id").toString();

		// Create Business Admin
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createBusinessAdminResponse = pageObj.endpoints().createBusinesAdmin(userEmail, role_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(createBusinessAdminResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Admin");
		boolean isCreateBusinessAdminSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardBusinessAdminSchema, createBusinessAdminResponse.asString());
		Assert.assertTrue(isCreateBusinessAdminSchemaValidated,
				"Platform Functions API Create Business Admin Schema Validation failed");
		utils.logPass("PLATFORM FUNCTIONS API Create Business Admin is successful");
		String business_admin_id = createBusinessAdminResponse.jsonPath().get("business_admin_id").toString();

		// Update Business Admin
		Response updateBusinessAdminResponse = pageObj.endpoints().updateBusinesAdmin(business_admin_id, userEmail,
				role_id, dataSet.get("adminAuthorization"));
		Assert.assertEquals(updateBusinessAdminResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Update Business Admin");
		boolean isUpdateBusinessAdminSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardBusinessAdminSchema, updateBusinessAdminResponse.asString());
		Assert.assertTrue(isUpdateBusinessAdminSchemaValidated,
				"Platform Functions API Update Business Admin Schema Validation failed");
		utils.logPass("PLATFORM FUNCTIONS API Update Business Admin is successful");

		// Show Business Admin
		Response showBusinessAdminResponse = pageObj.endpoints().showBusinesAdmin(business_admin_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(showBusinessAdminResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Show Business Admin");
		boolean isShowBusinessAdminSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardBusinessAdminSchema, showBusinessAdminResponse.asString());
		Assert.assertTrue(isShowBusinessAdminSchemaValidated,
				"Platform Functions API Show Business Admin Schema Validation failed");
		utils.logPass("PLATFORM FUNCTIONS API Show Business Admin is successful");

		// Delete Business Admin
		Response deleteBusinessAdminResponse = pageObj.endpoints().deleteBusinesAdmin(business_admin_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deleteBusinessAdminResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Delete Business Admin");
		utils.logPass("PLATFORM FUNCTIONS API Delete Business Admin is successful");

		// Invite Business Admin
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response inviteBusinessAdminResponse = pageObj.endpoints().inviteBusinesAdmin(userEmail, role_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(inviteBusinessAdminResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Invite Business Admin");
		boolean isInviteBusinessAdminSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardBusinessAdminSchema, inviteBusinessAdminResponse.asString());
		Assert.assertTrue(isInviteBusinessAdminSchemaValidated,
				"Platform Functions API Invite Business Admin Schema Validation failed");
		utils.logPass("PLATFORM FUNCTIONS API Invite Business Admin is successful");
		String business_admin_id1 = inviteBusinessAdminResponse.jsonPath().get("business_admin_id").toString();

		// Delete Business Admin
		Response deleteBusinessAdminResponse1 = pageObj.endpoints().deleteBusinesAdmin(business_admin_id1,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deleteBusinessAdminResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Delete Business Admin");
		utils.logPass("PLATFORM FUNCTIONS API Delete Business Admin is successful");

	}

	@Test(description = "SQ-T2843 eclub And Franchise Api Validation", groups = "api", priority = 5)
	public void eclubAndFranchiseApiValidation() {

		// EClub Guest Upload iFrame Configuration >> eClub Widget >> eClub Source must
		// be set as "test"
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		String storeNumber = "UK9999";
		Response eClubGuestUploadResponse = pageObj.endpoints().eClubGuestUpload(userEmail, storeNumber,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(eClubGuestUploadResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API EClub Guest Upload");
		utils.logPass("PLATFORM FUNCTIONS API EClub Guest Upload is successful");

		// Create Franchisee
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response cReateFranchiseResponse = pageObj.endpoints().cReateFranchise(userEmail,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(cReateFranchiseResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Franchisee");
		boolean isCreateFranchiseeSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardFranchiseeSchema, cReateFranchiseResponse.asString());
		Assert.assertTrue(isCreateFranchiseeSchemaValidated,
				"Platform Functions API Create Franchisee Schema Validation failed");
		utils.logPass("PLATFORM FUNCTIONS API Create Franchisee is successful");
		String franchisee_id = cReateFranchiseResponse.jsonPath().get("franchisee_id").toString();

		// Update Franchisee
		Response uPdateFranchiseResponse = pageObj.endpoints().uPdateFranchise(userEmail, franchisee_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(uPdateFranchiseResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Update Franchisee");
		boolean isUpdateFranchiseeSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardFranchiseeSchema, uPdateFranchiseResponse.asString());
		Assert.assertTrue(isUpdateFranchiseeSchemaValidated,
				"Platform Functions API Update Franchisee Schema Validation failed");
		utils.logPass("PLATFORM FUNCTIONS API Create Update Franchisee is successful");

		// Delete Franchisee
		Response dEleteFranchiseResponse = pageObj.endpoints().dEleteFranchise(franchisee_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(dEleteFranchiseResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Delete Franchisee");
		utils.logPass("PLATFORM FUNCTIONS API Create Delete Franchisee is successful");

	}

	@Test(description = "SQ-T2854 social Cause Campaigns Api Validation", groups = "api", priority = 6)
	public void socialCauseCampaignsApiValidation() {

		// Create Social Cause Campaigns
		String campaignName = "SocialCauseCampaign" + CreateDateTime.getTimeDateString();
		Response socialCauseCampaignResponse = pageObj.endpoints().cReateSocialcauseCampaign(campaignName,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(socialCauseCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Social Cause Campaign");
		utils.logPass("PLATFORM FUNCTIONS API Create Social Cause Campaigns is successful");
		String social_cause_id = socialCauseCampaignResponse.jsonPath().get("social_cause_id").toString();

		// Activate Social Cause Campaign
		Response activateSocialCampaignResponse = pageObj.endpoints().activateSocialCampaign(social_cause_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(activateSocialCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Activate Social Cause Campaign");
		boolean isActivateSocialCampaignSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2SocialCauseCampaignSchema, activateSocialCampaignResponse.asString());
		Assert.assertTrue(isActivateSocialCampaignSchemaValidated,
				"Platform Functions API Activate Social Cause Campaign Schema Validation failed");
		utils.logPass("PLATFORM FUNCTIONS API Activate Social Cause Campaign is successful");

		// Deactivate Social Cause Campaign
		Response deactivateSocialCampaignResponse = pageObj.endpoints().deactivateSocialCampaign(social_cause_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deactivateSocialCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Deactivate Social Cause Campaign");
		boolean isDeactivateSocialCampaignSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2SocialCauseCampaignSchema, deactivateSocialCampaignResponse.asString());
		Assert.assertTrue(isDeactivateSocialCampaignSchemaValidated,
				"Platform Functions API Deactivate Social Cause Campaign Schema Validation failed");
		utils.logPass("PLATFORM FUNCTIONS API Deactivate Social Cause Campaign is successful");

	}

	@Test(description = "SQ-T2855 redemption Api Validation; SQ-T4750 Verify User Lifetime Stats", groups = "api", priority = 7)
	public void redemptionApiValidation() {

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("Api2 user signup is successful");

		// send reward amount to user Amount
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				dataSet.get("amount"), "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		// Create Redemption using "redeemable" (fetch redemption code)
		Response redeemableResponse = pageObj.endpoints().Api2RedemptionWitReedemable_id(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched for api2 create redemption using redeemable");
		utils.logPass("Api2 Create Redemption using redeemable is successful");
		String redemption_code = redeemableResponse.jsonPath().get("redemption_tracking_code").toString();
		String location_id = redeemableResponse.jsonPath().get("location_id").toString();

		// Search Redemption Code
		Response searchRedemptionCode = pageObj.endpoints().searchRedemptionCode(dataSet.get("adminAuthorization"),
				redemption_code, location_id);
		Assert.assertEquals(searchRedemptionCode.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Platform Api Search Redemption Code");
		boolean isSearchRedemptionCodeSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardRedemptionSchema, searchRedemptionCode.asString());
		Assert.assertTrue(isSearchRedemptionCodeSchemaValidated,
				"Platform Functions API Search Redemption Code Schema Validation failed");
		utils.logPass("PLATFORM FUNCTIONS API Search Redemption Code is successful");

		// Process Redemption
		Response processRedemptionCode = pageObj.endpoints().processRedemptionCode(dataSet.get("adminAuthorization"),
				redemption_code, location_id);
		Assert.assertEquals(processRedemptionCode.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Platform Api Process Redemption");
		boolean isProcessRedemptionCodeSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardRedemptionSchema, processRedemptionCode.asString());
		Assert.assertTrue(isProcessRedemptionCodeSchemaValidated,
				"Platform Functions API Process Redemption Schema Validation failed");
		utils.logPass("PLATFORM FUNCTIONS API Process Redemption is successful");

		// send reward amount to user Amount
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				dataSet.get("amount"), dataSet.get("redeemable_id"));
		Assert.assertEquals(sendRewardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(offerResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		utils.logit(reward_id);
		utils.logPass("Api2 user fetch user offers is successful");

		// Force Redeem
		Response forceRedeemResponse = pageObj.endpoints().forceRedeem(dataSet.get("adminAuthorization"), reward_id,
				userID);
		Assert.assertEquals(forceRedeemResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for Platform Api Force Redeem");
		boolean isForceRedeemSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2CreateRedemptionSchema, forceRedeemResponse.asString());
		Assert.assertTrue(isForceRedeemSchemaValidated, "Platform Functions API Force Redeem Schema Validation failed");
		utils.logPass("PLATFORM FUNCTIONS API Force Redeem is successful");

		// Verify API2 User Lifetime Stats
		Response lifetimeStatsResponse = pageObj.endpoints().api2LifetimeStats(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(lifetimeStatsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 not matched for API2 Lifetime Stats");
		boolean isLifetimeStatsSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2LifetimeStatsSchema, lifetimeStatsResponse.asString());
		Assert.assertTrue(isLifetimeStatsSchemaValidated, "API v2 User Lifetime Stats Schema Validation failed");
		Assert.assertEquals(lifetimeStatsResponse.jsonPath().get("points_earned").toString(),
				dataSet.get("lifetimePointsEarned"), "points_earned count did not match for API2 Lifetime Stats");
		Assert.assertEquals(lifetimeStatsResponse.jsonPath().get("cards_redeemed").toString(),
				dataSet.get("lifetimeRedeemedCount"), "cards_redeemed count did not match for API2 Lifetime Stats");
		Assert.assertEquals(lifetimeStatsResponse.jsonPath().get("offers_redeemed").toString(),
				dataSet.get("lifetimeRedeemedCount"), "offers_redeemed count did not match for API2 Lifetime Stats");
		utils.logPass("API2 user lifetime stats call is successful");
	}

	@Test(description = "SQ-T2808 verify Custom Segment Creation, Updation, Deletion and AddUser Api", groups = "api", priority = 8)
	public void verifyCustomSegmentCreationUpdationDeletionAddUserApi() {
		// create Custom segment
		String segName = "CustomSeg" + CreateDateTime.getTimeDateString();
		Response createSegmentResponse = pageObj.endpoints().createCustomSegment(segName,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(createSegmentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched");
		boolean isCreateCustomSegmentSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardCustomSegmentSchema, createSegmentResponse.asString());
		Assert.assertTrue(isCreateCustomSegmentSchemaValidated, "Create custom segment schema validation failed");
		int customSegmentId = createSegmentResponse.jsonPath().get("custom_segment_id");
		utils.logPass("Verified create custom segment");

		// get Custom segment's details
		Response getSegmentDetailResponse = pageObj.endpoints()
				.getCustomSegmentDetails(dataSet.get("adminAuthorization"), customSegmentId);
		Assert.assertEquals(getSegmentDetailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched");
		boolean isGetCustomSegmentDetailSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.dashboardCustomSegmentListSchema, getSegmentDetailResponse.asString());
		Assert.assertTrue(isGetCustomSegmentDetailSchemaValidated, "Get custom segment schema validation failed");
		String cSegName = getSegmentDetailResponse.jsonPath().get("[0].name").toString();
		String cSegID = getSegmentDetailResponse.jsonPath().get("[0].custom_segment_id").toString();
		Assert.assertEquals(segName, cSegName, "Custom segment name did not matched in segment details api");
		Assert.assertEquals(cSegID, Integer.toString(customSegmentId),
				"Custom segment id did not matched in segment details api");
		utils.logPass("Verified Get custom segment's details");

		// get list of all Custom segments
		Response getAllSegmentResponse = pageObj.endpoints().listAllCustomSegments(dataSet.get("adminAuthorization"));
		Assert.assertEquals(getAllSegmentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched");
		boolean isGetAllCustomSegmentSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.dashboardCustomSegmentListSchema, getAllSegmentResponse.asString());
		Assert.assertTrue(isGetAllCustomSegmentSchemaValidated, "List all custom segment schema validation failed");
		Assert.assertTrue(getAllSegmentResponse.jsonPath().get("name").toString().contains(segName),
				"Failed to verify new segment");
		utils.logPass("Verified new custom segment in list of all custom segments");

		// update Custom segments
		String updatedSegmentName = "updated" + segName;
		Response updateSegmentResponse = pageObj.endpoints().updateCustomSegment(updatedSegmentName,
				dataSet.get("adminAuthorization"), customSegmentId);
		Assert.assertEquals(updateSegmentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched");
		boolean isUpdateCustomSegmentSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardCustomSegmentSchema, updateSegmentResponse.asString());
		Assert.assertTrue(isUpdateCustomSegmentSchemaValidated, "Update custom segment schema validation failed");
		utils.logPass("Verified update custom segment");

		// list all Custom segments
		getAllSegmentResponse = pageObj.endpoints().listAllCustomSegments(dataSet.get("adminAuthorization"));
		Assert.assertTrue(getAllSegmentResponse.jsonPath().get("name").toString().contains(updatedSegmentName),
				"Failed to verify updated custom segment");
		utils.logPass("Verified updated custom segment in list of all custom segments");

		// Creating user with POS user signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response response = pageObj.endpoints().posSignUp(userEmail, dataSet.get("location_key"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());
		utils.logPass("User created with POS user signup");

		// add user to custom segments
		Response addUserSegmentResponse = pageObj.endpoints().addUserToCustomSegment(customSegmentId, userEmail,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(addUserSegmentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 not matched for Add user to custom segment");
		boolean isAddUserToCustomSegmentSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardAddCustomSegmentUserSchema, addUserSegmentResponse.asString());
		Assert.assertTrue(isAddUserToCustomSegmentSchemaValidated,
				"Add user to custom segment schema validation failed");
		int userId = addUserSegmentResponse.jsonPath().get("user_id");
		String addUserSegmentResponseUserEmail = addUserSegmentResponse.jsonPath().get("email").toString();
		Assert.assertTrue(addUserSegmentResponseUserEmail.contains(userEmail.toLowerCase()),
				"Failed to verify added user to custom segment");
		utils.logPass("Added user to custom segment");

		// Searching user in custom segments
		Response userExistsInSegmentResponse = pageObj.endpoints().searchUserExistsInSegment(customSegmentId, userEmail,
				userId, dataSet.get("adminAuthorization"));
		Assert.assertEquals(userExistsInSegmentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 not matched for Search user in custom segment");
		boolean isCustomSegmentUserExistsSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.dashboardSearchCustomSegmentUserSchema, userExistsInSegmentResponse.asString());
		Assert.assertTrue(isCustomSegmentUserExistsSchemaValidated,
				"Search user in custom segment schema validation failed");
		utils.logPass("Verified user exists in custom segment");

		// delete user from custom segments
		Response deleteSegmenUserResponse = pageObj.endpoints()
				.deletingUserFromCustomSegment(dataSet.get("adminAuthorization"), customSegmentId, userEmail, userId);
		Assert.assertEquals(deleteSegmenUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NO_CONTENT);
		utils.logPass("Deleted user from custom segment");

		// Verifying deleted user no more exists in segment
		userExistsInSegmentResponse = pageObj.endpoints().searchUserExistsInSegment(customSegmentId, userEmail, userId,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(userExistsInSegmentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 not matched for Search user in custom segment");
		boolean isCustomSegmentUserNotExistsSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2NotFoundErrorSchema, userExistsInSegmentResponse.asString());
		Assert.assertTrue(isCustomSegmentUserNotExistsSchemaValidated,
				"Search user in custom segment schema validation failed");
		String valMsg = userExistsInSegmentResponse.jsonPath().get("errors.not_found").toString();
		Assert.assertEquals("Member not found.", valMsg,
				"Member not found response message did not matched for deleted guest in segment");
		utils.logPass("Verified that user is deleted from custom segment");

		// delete custom segments
		Response deleteSegmentResponse = pageObj.endpoints().deletingCustomSegment(dataSet.get("adminAuthorization"),
				customSegmentId);
		Assert.assertEquals(deleteSegmentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 not matched for delete custom segment");
		boolean isDeleteCustomSegmentSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardDeleteCustomSegmentSchema, deleteSegmentResponse.asString());
		Assert.assertTrue(isDeleteCustomSegmentSchemaValidated, "Delete custom segment schema validation failed");
		utils.logPass("Verified delete custom segment");

		// verify deleted Custom segment
		/*
		 * Response getdeletedcustomSegmentDetailResponse = pageObj.endpoints()
		 * .getCustomSegmentDetails(dataSet.get("adminAuthorization"), customSegmentId);
		 * Assert.assertEquals(* getdeletedcustomSegmentDetailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, * "Status code 200 get custom segments api did not matched"); String delSegId =
		 * getdeletedcustomSegmentDetailResponse.jsonPath().get("custom_segment_id").
		 * toString(); Assert.assertEquals(delSegId,"[]",
		 * "Failed to verify segment Id in segment details api");
		 * utils.logPass("Verified deleted custom segment");
		 */

	}

	@Test(description = "Verify Platform Functions API:- SQ-T5176: Meta; SQ-T5247: Cancel Subscription", groups = "api", priority = 9)
	public void verifyPlatformAPIMeta() {

		// Dashboard Meta
		utils.logit("== Platform Functions API: Dashboard Meta ==");
		Response dashboardMetaResponse = pageObj.endpoints().dashboardAPI2Meta(dataSet.get("adminAuthorization"));
		Assert.assertEquals(dashboardMetaResponse.statusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Platform Functions API Dashboard Meta");
		boolean isDashboardMetaSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.dashboardMetaSchema, dashboardMetaResponse.asString());
		Assert.assertTrue(isDashboardMetaSchemaValidated, "Dashboard API Meta Schema Validation failed");
		String redeemableId = utils.getJsonReponseKeyValueFromJsonArray(dashboardMetaResponse, "redeemables",
				"redeemable_id", dataSet.get("existingRedeemableId"));
		Assert.assertEquals(redeemableId, dataSet.get("existingRedeemableId"), "Description did not match");
		utils.logPass("Platform Functions API Dashboard Meta call is successful");

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Purchase Subscription
		utils.logit("== Platform Functions API: Purchase Subscription ==");
		String purchasePrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));
		String endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";

		Response purchaseSubscriptionResponse = pageObj.endpoints().dashboardSubscriptionPurchase(
				dataSet.get("adminAuthorization"), dataSet.get("existingSubscriptionPlanID"), dataSet.get("client"),
				dataSet.get("secret"), purchasePrice, endDateTime, "false", userID);
		Assert.assertEquals(purchaseSubscriptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Platform Functions API Purchase Subscription call.");
		boolean isPurchaseSubscriptionSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authPurchaseSubscriptionSchema, purchaseSubscriptionResponse.asString());
		Assert.assertTrue(isPurchaseSubscriptionSchemaValidated,
				"Platform Functions API Purchase Subscription Schema Validation failed");
		String subscriptionID = purchaseSubscriptionResponse.jsonPath().get("subscription_id").toString();
		utils.logPass("Platform Functions API Purchase Subscription call is successful with Subscription ID: "
				+ subscriptionID);

		// Cancel Subscription (Turn off Auto Renewal)
		utils.logit("== Platform Functions API: Cancel Subscription (Turn off Auto Renewal) ==");
		Response cancelSubscriptionResponse = pageObj.endpoints().dashboardSubscriptionCancel(
				dataSet.get("adminAuthorization"), subscriptionID, "Did not like the service", "hard_cancelled");
		Assert.assertEquals(cancelSubscriptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Platform Functions API Cancel Subscription call.");
		boolean isCancelSubscriptionSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, cancelSubscriptionResponse.asString());
		Assert.assertTrue(isCancelSubscriptionSchemaValidated,
				"Platform Functions API Cancel Subscription Schema Validation failed");
		String cancelSubscriptionResponseMsg = cancelSubscriptionResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(cancelSubscriptionResponseMsg, "Subscription auto renewal canceled.");
		utils.logPass(
				"Platform Functions API Cancel Subscription call is successful for Subscription ID: " + subscriptionID);

	}

	@Test(description = "LPE-1475 - LPE-T2827 Verify UUID generation for force redemption when feature flag is enabled")
	@Owner(name = "Neha Lodha")
	public void LPET2827_ForceRedemptionFlagEnabled() throws Exception {

		String query3 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query3, "preferences");

		// set value true
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"),
				dataSet.get("business_id"));
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to false");
		utils.logit(dataSet.get("dbFlag") + " value is updated to false");

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("Api2 user signup is successful");

		// send reward redeemable_id to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				"", dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward reedemable to user is successful");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, offerResponse.getStatusCode(), "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		utils.logit(reward_id);
		utils.logPass("Api2 user fetch user offers is successful");

		// Force Redeem
		Response forceRedeemResponse = pageObj.endpoints().forceRedeem(dataSet.get("adminAuthorization"), reward_id,
				userID);
		Assert.assertEquals(forceRedeemResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for Platform Api Force Redeem");
		boolean isForceRedeemSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2CreateRedemptionSchema, forceRedeemResponse.asString());
		Assert.assertTrue(isForceRedeemSchemaValidated, "Platform Functions API Force Redeem Schema Validation failed");
		utils.logPass("PLATFORM FUNCTIONS API Force Redeem is successful");
		String redemption_tracking_code = forceRedeemResponse.jsonPath().get("redemption_tracking_code");
		Assert.assertTrue(redemption_tracking_code.length() > 8, "Redemption code should be more than 8 digits");

		// send reward amount to user
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				dataSet.get("amount"), "", "", "");
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		// generate redemption code using mobile api
		Response redemption_codeResponse1 = pageObj.endpoints().Api1MobileRedemptionRedeemed_Points(token, "1",
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(redemption_codeResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String redemption_Code = redemption_codeResponse1.jsonPath().get("internal_tracking_code").toString();
		utils.logit("internal_tracking_code => " + redemption_Code);
		String internal_tracking_code = redemption_codeResponse1.jsonPath().get("internal_tracking_code");
		Assert.assertTrue(internal_tracking_code.length() <= 8,
				"Redemption code should be less than or equal to 8 digits");

	}

	@Test(description = "LPE-1475 - LPE-T2829 Verify normal redemption code generation when feature flag is disabled")
	@Owner(name = "Neha Lodha")
	public void LPET2829_ForceRedemptionFlagDisabled() throws Exception {

		String query3 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query3, "preferences");

		pageObj.singletonDBUtilsObj();
		// set value true
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "false", dataSet.get("dbFlag"),
				dataSet.get("business_id"));
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");
		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("Api2 user signup is successful");

		// send reward redeemable_id to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				"", dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward reedemable to user is successful");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, offerResponse.getStatusCode(), "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		utils.logit(reward_id);
		utils.logPass("Api2 user fetch user offers is successful");

		// Force Redeem
		Response forceRedeemResponse = pageObj.endpoints().forceRedeem(dataSet.get("adminAuthorization"), reward_id,
				userID);
		Assert.assertEquals(forceRedeemResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for Platform Api Force Redeem");
		boolean isForceRedeemSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2CreateRedemptionSchema, forceRedeemResponse.asString());
		Assert.assertTrue(isForceRedeemSchemaValidated, "Platform Functions API Force Redeem Schema Validation failed");
		utils.logPass("PLATFORM FUNCTIONS API Force Redeem is successful");
		String redemption_tracking_code = forceRedeemResponse.jsonPath().get("redemption_tracking_code");
		Assert.assertTrue(redemption_tracking_code.length() <= 8,
				"Redemption code should be less than or equal to 8 digits");

		// send reward amount to user
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				dataSet.get("amount"), "", "", "");
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		// generate redemption code using mobile api
		Response redemption_codeResponse1 = pageObj.endpoints().Api1MobileRedemptionRedeemed_Points(token, "1",
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(redemption_codeResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String redemption_Code = redemption_codeResponse1.jsonPath().get("internal_tracking_code").toString();
		utils.logit("internal_tracking_code => " + redemption_Code);
		String internal_tracking_code = redemption_codeResponse1.jsonPath().get("internal_tracking_code");
		Assert.assertTrue(internal_tracking_code.length() <= 8,
				"Redemption code should be less than or equal to 8 digits");

	}

	@AfterMethod(alwaysRun = true)
	public void afterMethod() {
		pageObj.utils().clearDataSet(dataSet);
	}
}