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

/*author: Amit Kumar*/

import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class PlatformApiNegativeTest {
	static Logger logger = LogManager.getLogger(PlatformApiNegativeTest.class);
	public WebDriver driver;
	ApiUtils apiUtils;
	String userEmail;
	Properties uiProp;
	Properties prop;
	PageObj pageObj;
	String sTCName;
	String run = "api";
	// String adminAuthorization = "mXs1DRw6nNwK9jxBYmr3";// pp
	private String env;
	private static Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void beforeMethod(Method method) {

		uiProp = Utilities.loadPropertiesFile("config.properties");
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		apiUtils = new ApiUtils();
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);

	}

	@Test(description = "SQ-T3088 verify Custom Segment Creation, Updation, Deletion and AddUser Api Negative Flow")
	public void T3088_verifyCustomSegmentCreationUpdationDeletionAddUserApiNegativeFlow() {
		String segName = "CustomSeg" + CreateDateTime.getTimeDateString();
		// create custom segment with valid authorization
		Response createSegmentResponse2 = pageObj.endpoints().createCustomSegment(segName,
				dataSet.get("validAdminAuthorization"));
		pageObj.apiUtils().verifyCreateResponse(createSegmentResponse2, "Custom segment created");
		Assert.assertEquals(createSegmentResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code did not matched as 201 for create custom segment with valid authorization");
		logger.info("Verified create custom segment with valid authorization");
		pageObj.utils().logPass("Verified create custom segment with valid authorization");
		int customSegmentId = createSegmentResponse2.jsonPath().get("custom_segment_id");

		// create custom segment with invalid authorization
		Response createSegmentResponse = pageObj.endpoints().createCustomSegment(segName,
				dataSet.get("invalidAdminAuthorization"));
		Assert.assertEquals(createSegmentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code did not matched as 401 for create custom segment with invalid authorization");
		boolean isCreateCustomSegmentInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, createSegmentResponse.asString());
		Assert.assertTrue(isCreateCustomSegmentInvalidAuthSchemaValidated,
				"Create Custom Segment Schema Validation failed");
		String createSegmentResponseError = createSegmentResponse.jsonPath().get("error");
		Assert.assertEquals(createSegmentResponseError, "You need to sign in or sign up before continuing.");
		logger.info("Verified create custom segment with invalid authorization");
		pageObj.utils().logPass("Verified create custom segment with invalid authorization");

		// create custom segment with no authorization
		Response createSegmentResponse1 = pageObj.endpoints().createCustomSegment(segName, ""); // null admin
																								// authorization
		Assert.assertEquals(createSegmentResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code did not matched as 401 for create custom segment with null authorization");
		boolean isCreateCustomSegmentNullAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, createSegmentResponse1.asString());
		Assert.assertTrue(isCreateCustomSegmentNullAuthSchemaValidated,
				"Create Custom Segment Schema Validation failed");
		String createSegmentResponseError1 = createSegmentResponse1.jsonPath().get("error");
		Assert.assertEquals(createSegmentResponseError1, "You need to sign in or sign up before continuing.");
		logger.info("Verified create custom segment with null authorization");
		pageObj.utils().logPass("Verified create custom segment with null authorization");

		// get custom segment details with invalid segment id
		int invalidcustomSegmentId = 123456;
		Response getSegmentDetailResponse = pageObj.endpoints()
				.getCustomSegmentDetails(dataSet.get("validAdminAuthorization"), invalidcustomSegmentId);
		Assert.assertEquals(getSegmentDetailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code did not matched as 200 for get custom segment details with invalid custom segment id");
		Assert.assertEquals(getSegmentDetailResponse.body().jsonPath().getList("$").size(), 0,
				"For invalid segment id reponse list size 0 did not matched");
		logger.info("Verified get custom segment details with invalid segment id");
		pageObj.utils().logPass("Verified get custom segment details with invalid segment id");

		// get custom segment details with invalid authorization
		Response getSegmentDetailResponse1 = pageObj.endpoints()
				.getCustomSegmentDetails(dataSet.get("invalidAdminAuthorization"), customSegmentId);
		Assert.assertEquals(getSegmentDetailResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code did not matched as 401 for get custom segment details with invalid authorization");
		boolean isGetSegmentDetailInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, getSegmentDetailResponse1.asString());
		Assert.assertTrue(isGetSegmentDetailInvalidAuthSchemaValidated,
				"Get Custom Segment Details Schema Validation failed");
		String getSegmentDetailResponseError1 = getSegmentDetailResponse1.jsonPath().get("error");
		Assert.assertEquals(getSegmentDetailResponseError1, "You need to sign in or sign up before continuing.");
		logger.info("Verified get custom segment details with invalid authorization");
		pageObj.utils().logPass("Verified get custom segment details with invalid authorization");

		// List all custom segments with invalid authorization
		Response getAllSegmentResponse = pageObj.endpoints()
				.listAllCustomSegments(dataSet.get("invalidAdminAuthorization"));
		Assert.assertEquals(getAllSegmentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code did not matched as 401 for List all custom segments with invalid authorization");
		boolean isGetCustomSegmentListInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, getAllSegmentResponse.asString());
		Assert.assertTrue(isGetCustomSegmentListInvalidAuthSchemaValidated,
				"List All Custom Segments Schema Validation failed");
		String getAllSegmentResponseError = getAllSegmentResponse.jsonPath().get("error");
		Assert.assertEquals(getAllSegmentResponseError, "You need to sign in or sign up before continuing.");
		logger.info("Verified list all custom segments with invalid authorization");
		pageObj.utils().logPass("Verified list all custom segments with invalid authorization");

		// List all custom segments with null authorization
		Response getAllSegmentResponse1 = pageObj.endpoints().listAllCustomSegments(dataSet.get(""));
		Assert.assertEquals(getAllSegmentResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code did not matched as 401 for List all custom segments with null authorization");
		boolean isGetCustomSegmentListNullAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, getAllSegmentResponse1.asString());
		Assert.assertTrue(isGetCustomSegmentListNullAuthSchemaValidated,
				"List All Custom Segments Schema Validation failed");
		String getAllSegmentResponseError1 = getAllSegmentResponse1.jsonPath().get("error");
		Assert.assertEquals(getAllSegmentResponseError1, "You need to sign in or sign up before continuing.");
		logger.info("Verified list all custom segments with null authorization");
		pageObj.utils().logPass("Verified list all custom segments with null authorization");

		// Update a Custom Segment with invalid authorization
		String updatedSegmentName = "updated" + segName;
		Response updateSegmentResponse = pageObj.endpoints().updateCustomSegment(updatedSegmentName,
				dataSet.get("invalidAdminAuthorization"), customSegmentId);
		Assert.assertEquals(updateSegmentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code did not matched as 401 for Update a Custom Segment with invalid authorization");
		boolean isUpdateCustomSegmentInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, updateSegmentResponse.asString());
		Assert.assertTrue(isUpdateCustomSegmentInvalidAuthSchemaValidated,
				"Update Custom Segment Schema Validation failed");
		String updateSegmentResponseError = updateSegmentResponse.jsonPath().get("error");
		Assert.assertEquals(updateSegmentResponseError, "You need to sign in or sign up before continuing.");
		logger.info("Verified update custom segment with invalid authorization");
		pageObj.utils().logPass("Verified update custom segment with invalid authorization");

		// Update a Custom Segment with invalid segmentID
		Response updateSegmentResponse1 = pageObj.endpoints().updateCustomSegment(updatedSegmentName,
				dataSet.get("validAdminAuthorization"), invalidcustomSegmentId);
		Assert.assertEquals(updateSegmentResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code did not matched as 404 for Update a Custom Segment with invalid segmentID");
		boolean isUpdateCustomSegmentInvalidIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2NotFoundErrorSchema, updateSegmentResponse1.asString());
		Assert.assertTrue(isUpdateCustomSegmentInvalidIdSchemaValidated,
				"Update Custom Segment Schema Validation failed");
		String updateSegmentResponseError1 = updateSegmentResponse1.jsonPath().get("errors.not_found");
		Assert.assertEquals(updateSegmentResponseError1, "Custom Segment not found.");
		logger.info("Verified update a custom segment with invalid segmentID");
		pageObj.utils().logPass("Verified update a custom segment with invalid segmentID");

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		// Creating user with POS user signup
		Response response = pageObj.endpoints().posSignUp(userEmail, dataSet.get("location_key"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "POS signup api response 200 did not matched");
		int userId = response.jsonPath().get("id");
		logger.info("Verified POS user signup");
		pageObj.utils().logPass("Verified POS user signup");

		// add user to custom segments with invalid authorization
		Response addUserSegmentResponse = pageObj.endpoints().addUserToCustomSegment(customSegmentId, userEmail,
				dataSet.get("invalidAdminAuthorization"));
		Assert.assertEquals(addUserSegmentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code did not matched as 401 for Add user custom segment with invalid authorization");
		boolean isAddCustomSegmentUserInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, addUserSegmentResponse.asString());
		Assert.assertTrue(isAddCustomSegmentUserInvalidAuthSchemaValidated,
				"Add User To Custom Segment Schema Validation failed");
		String addUserSegmentResponseError = addUserSegmentResponse.jsonPath().get("error");
		Assert.assertEquals(addUserSegmentResponseError, "You need to sign in or sign up before continuing.");
		logger.info("Verified add user to custom segment with invalid authorization");
		pageObj.utils().logPass("Verified add user to custom segment with invalid authorization");

		// add user to custom segments with invalid segment id
		Response addUserSegmentResponse1 = pageObj.endpoints().addUserToCustomSegment(invalidcustomSegmentId, userEmail,
				dataSet.get("validAdminAuthorization"));
		Assert.assertEquals(addUserSegmentResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code did not matched as 404 for Add user to custom segment with invalid segment id");
		boolean isAddCustomSegmentUserInvalidIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2NotFoundErrorSchema, addUserSegmentResponse1.asString());
		Assert.assertTrue(isAddCustomSegmentUserInvalidIdSchemaValidated,
				"Add User To Custom Segment Schema Validation failed");
		String addUserSegmentResponseError1 = addUserSegmentResponse1.jsonPath().get("errors.not_found");
		Assert.assertEquals(addUserSegmentResponseError1, "Custom Segment not found.");
		logger.info("Verified add user to custom segment with invalid segment id");
		pageObj.utils().logPass("Verified add user to custom segment with invalid segment id");

		String invaliduserEmail = "!%qwe.*&^%@punchh.com";
		int invaliduserId = 12345678;
		// Searching user in custom segments with invalid authorization
		Response userExistsInSegmentResponse = pageObj.endpoints().searchUserExistsInSegment(customSegmentId, userEmail,
				userId, dataSet.get("invalidAdminAuthorization"));
		Assert.assertEquals(userExistsInSegmentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code did not matched as 401 for search user in custom segment with invalid authorization");
		boolean isSearchCustomSegmentUserInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, userExistsInSegmentResponse.asString());
		Assert.assertTrue(isSearchCustomSegmentUserInvalidAuthSchemaValidated,
				"Search User in Custom Segment Schema Validation failed");
		String userExistsInSegmentResponseError = userExistsInSegmentResponse.jsonPath().get("error");
		Assert.assertEquals(userExistsInSegmentResponseError, "You need to sign in or sign up before continuing.");
		logger.info("Verified search user in custom segment with invalid authorization");
		pageObj.utils().logPass("Verified search user in custom segment with invalid authorization");

		// Searching user in custom segments with invalid user id
		Response userExistsInSegmentResponse1 = pageObj.endpoints().searchUserExistsInSegment(customSegmentId,
				userEmail, invaliduserId, dataSet.get("validAdminAuthorization"));
		Assert.assertEquals(userExistsInSegmentResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code did not matched as 404 for search user in custom segment with invalid user id");
		boolean isSearchCustomSegmentUserInvalidIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2NotFoundErrorSchema, userExistsInSegmentResponse1.asString());
		Assert.assertTrue(isSearchCustomSegmentUserInvalidIdSchemaValidated,
				"Search User in Custom Segment Schema Validation failed");
		String userExistsInSegmentResponseError1 = userExistsInSegmentResponse1.jsonPath().get("errors.not_found");
		Assert.assertEquals(userExistsInSegmentResponseError1, "Member not found.");
		logger.info("Verified search user in custom segment with invalid user id");
		pageObj.utils().logPass("Verified search user in custom segment with invalid user id");

		// Searching user in custom segments with invalid user email
		Response userExistsInSegmentResponse2 = pageObj.endpoints().searchUserExistsInSegment(customSegmentId,
				invaliduserEmail, userId, dataSet.get("validAdminAuthorization"));
		Assert.assertEquals(userExistsInSegmentResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code did not matched as 404 for search user in custom segment with invalid user email");
		boolean isSearchCustomSegmentUserInvalidEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2NotFoundErrorSchema, userExistsInSegmentResponse2.asString());
		Assert.assertTrue(isSearchCustomSegmentUserInvalidEmailSchemaValidated,
				"Search User in Custom Segment Schema Validation failed");
		String userExistsInSegmentResponseError2 = userExistsInSegmentResponse2.jsonPath().get("errors.not_found");
		Assert.assertEquals(userExistsInSegmentResponseError2, "Member not found.");
		logger.info("Verified search user in custom segment with invalid user email");
		pageObj.utils().logPass("Verified search user in custom segment with invalid user email");

		// Searching user in custom segments with invalid segment id
		Response userExistsInSegmentResponse3 = pageObj.endpoints().searchUserExistsInSegment(invalidcustomSegmentId,
				userEmail, userId, dataSet.get("validAdminAuthorization"));
		Assert.assertEquals(userExistsInSegmentResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code did not matched as 404 for search user in custom segment with invalid segment id");
		boolean isSearchCustomSegmentUserInvalidSegmentIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2NotFoundErrorSchema, userExistsInSegmentResponse3.asString());
		Assert.assertTrue(isSearchCustomSegmentUserInvalidSegmentIdSchemaValidated,
				"Search User in Custom Segment Schema Validation failed");
		String userExistsInSegmentResponseError3 = userExistsInSegmentResponse3.jsonPath().get("errors.not_found");
		Assert.assertEquals(userExistsInSegmentResponseError3, "Member not found.");
		logger.info("Verified search user in custom segment with invalid segment id");
		pageObj.utils().logPass("Verified search user in custom segment with invalid segment id");

		// delete user from custom segments with invalid user id
		Response deleteSegmenUserResponse = pageObj.endpoints().deletingUserFromCustomSegment(
				dataSet.get("validAdminAuthorization"), customSegmentId, userEmail, invaliduserId);
		Assert.assertEquals(deleteSegmenUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code did not matched as 404 for delete user from custom segment with invalid user id");
		boolean isDeleteCustomSegmentUserInvalidIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2NotFoundErrorSchema, deleteSegmenUserResponse.asString());
		Assert.assertTrue(isDeleteCustomSegmentUserInvalidIdSchemaValidated,
				"Delete User From Custom Segment Schema Validation failed");
		String deleteSegmenUserResponseError = deleteSegmenUserResponse.jsonPath().get("errors.not_found");
		Assert.assertEquals(deleteSegmenUserResponseError, "Member not found.");
		logger.info("Verified delete user from custom segment with invalid user id");
		pageObj.utils().logPass("Verified delete user from custom segment with invalid user id");

		// delete user from custom segments with invalid authorization
		Response deleteSegmenUserResponse1 = pageObj.endpoints().deletingUserFromCustomSegment(
				dataSet.get("invalidAdminAuthorization"), customSegmentId, userEmail, userId);
		Assert.assertEquals(deleteSegmenUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code did not matched as 401 for delete user from custom segment with invalid authorization");
		boolean isDeleteCustomSegmentUserInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, deleteSegmenUserResponse1.asString());
		Assert.assertTrue(isDeleteCustomSegmentUserInvalidAuthSchemaValidated,
				"Delete User From Custom Segment Schema Validation failed");
		String deleteSegmenUserResponseError1 = deleteSegmenUserResponse1.jsonPath().get("error");
		Assert.assertEquals(deleteSegmenUserResponseError1, "You need to sign in or sign up before continuing.");
		logger.info("Verified delete user from custom segment with invalid authorization");
		pageObj.utils().logPass("Verified delete user from custom segment with invalid authorization");

		// delete user from custom segments with invalid mail
		Response deleteSegmenUserResponse2 = pageObj.endpoints().deletingUserFromCustomSegment(
				dataSet.get("validAdminAuthorization"), customSegmentId, invaliduserEmail, userId);
		Assert.assertEquals(deleteSegmenUserResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code did not matched as 404 for delete user from custom segment with invalid user email");
		boolean isDeleteCustomSegmentUserInvalidEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2NotFoundErrorSchema, deleteSegmenUserResponse2.asString());
		Assert.assertTrue(isDeleteCustomSegmentUserInvalidEmailSchemaValidated,
				"Delete User From Custom Segment Schema Validation failed");
		String deleteSegmenUserResponseError2 = deleteSegmenUserResponse2.jsonPath().get("errors.not_found");
		Assert.assertEquals(deleteSegmenUserResponseError2, "Member not found.");
		logger.info("Verified delete user from custom segment with invalid user email");
		pageObj.utils().logPass("Verified delete user from custom segment with invalid user email");

		// delete custom segment with invalid custom segment id
		Response deleteSegmentResponse = pageObj.endpoints()
				.deletingCustomSegment(dataSet.get("validAdminAuthorization"), invalidcustomSegmentId);
		Assert.assertEquals(deleteSegmentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code did not matched as 404 for delete custom segment with invalid segment id");
		boolean isDeleteCustomSegmentInvalidIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2NotFoundErrorSchema, deleteSegmentResponse.asString());
		Assert.assertTrue(isDeleteCustomSegmentInvalidIdSchemaValidated,
				"Delete Custom Segment Schema Validation failed");
		String deleteSegmentResponseError2 = deleteSegmentResponse.jsonPath().get("errors.not_found");
		Assert.assertEquals(deleteSegmentResponseError2, "Custom Segment not found.");
		logger.info("Verified delete custom segment with invalid segment id");
		pageObj.utils().logPass("Verified delete custom segment with invalid segment id");

		// delete custom segment with invalid authorization
		Response deleteSegmentResponse1 = pageObj.endpoints()
				.deletingCustomSegment(dataSet.get("invalidAdminAuthorization"), customSegmentId);
		Assert.assertEquals(deleteSegmentResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code did not matched as 401 for delete custom segment with invalid authorization");
		boolean isDeleteCustomSegmentInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, deleteSegmentResponse1.asString());
		Assert.assertTrue(isDeleteCustomSegmentInvalidAuthSchemaValidated,
				"Delete Custom Segment Schema Validation failed");
		String deleteSegmentResponseError1 = deleteSegmentResponse1.jsonPath().get("error");
		Assert.assertEquals(deleteSegmentResponseError1, "You need to sign in or sign up before continuing.");
		logger.info("Verified delete custom segment with invalid authorization");
		pageObj.utils().logPass("Verified delete custom segment with invalid authorization");
	}

	@Test(description = "SQ-T3112 wifi Api Validation negative flow", groups = { "regression" })
	public void wifiApiValidationnegative() {
		String invaliduserEmail = "abc";
		// String invaliddataSet.get("adminAuthorization")="";
		logger.info("== Wifi user signup Test ==");
		String UserEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().wifiUserSignUp(UserEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("locationId"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED, "Status code 202 did not match for Wifi user signup");
		logger.info("Verified user singup successfull");
		pageObj.utils().logPass("Verified user signup successful");

		// Enroll Guests For WiFi with invalid mail
		logger.info("== Enroll Guests For WiFi with invalid email==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response enrollGuestWifiResponse = pageObj.endpoints().enrollGuestForWifi(invaliduserEmail,
				dataSet.get("client"), dataSet.get("locationId"), dataSet.get("adminAuthorization"));
		Assert.assertEquals(enrollGuestWifiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for PLATFORM FUNCTIONS API Enroll Guests For WiFi");
		boolean isEnrollGuestWifiInvalidEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2EmailErrorSchema, enrollGuestWifiResponse.asString());
		Assert.assertTrue(isEnrollGuestWifiInvalidEmailSchemaValidated,
				"Enroll Guests For WiFi API Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Enroll Guests For WiFi is unsuccessful because of invalid email");

		// Enroll Guests For WiFi with invalid client id
		logger.info("== Enroll Guests For WiFi with invalid client id==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response enrollGuestWifiResponse1 = pageObj.endpoints().enrollGuestForWifi(userEmail,
				dataSet.get("invalidclient"), dataSet.get("locationId"), dataSet.get("adminAuthorization"));
		Assert.assertEquals(enrollGuestWifiResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Enroll Guests For WiFi");
		boolean isEnrollGuestWifiInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, enrollGuestWifiResponse1.asString());
		Assert.assertTrue(isEnrollGuestWifiInvalidClientSchemaValidated,
				"Enroll Guests For WiFi API Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Enroll Guests For WiFi is unsuccessful because of invalid client");

		// Enroll Guests For WiFi with invalid location id
		logger.info("== Enroll Guests For WiFi with invalid location id==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response enrollGuestWifiResponse2 = pageObj.endpoints().enrollGuestForWifi(userEmail, dataSet.get("client"),
				dataSet.get("invalidlocationId"), dataSet.get("adminAuthorization"));
		Assert.assertEquals(enrollGuestWifiResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for PLATFORM FUNCTIONS API Enroll Guests For WiFi");
		boolean isEnrollGuestWifiInvalidLocationSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2NotFoundErrorSchema, enrollGuestWifiResponse2.asString());
		Assert.assertTrue(isEnrollGuestWifiInvalidLocationSchemaValidated,
				"Enroll Guests For WiFi API Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Enroll Guests For WiFi is unsuccessful because of invalid location id");
//		// Enroll Guests For WiFi with invalid authorization
//		userEmail = pageObj.iframeSingUpPage().generateEmail();
//		Response enrollGuestWifiResponse3 = pageObj.endpoints().enrollGuestForWifi(userEmail, dataSet.get("client"),
//				dataSet.get("locationId"), invaliddataSet.get("adminAuthorization"));
//		Assert.assertEquals(enrollGuestWifiResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
//				"Status code 401 did not matched for PLATFORM FUNCTIONS API Enroll Guests For WiFi");
//		pageObj.utils().logPass("PLATFORM FUNCTIONS API Enroll Guests For WiFi is unsuccessful");

		// Guest Lookup For WiFi Enrollment, Returns whether a guest exists in the
		// business or not with invalid mail.
		logger.info("== Guest Lookup For WiFi Enrollment using invalid email==");
		Response getwifiUserlookup = pageObj.endpoints().guestLookupWifiEnrollment(invaliduserEmail,
				dataSet.get("client"), dataSet.get("adminAuthorization"));
		Assert.assertEquals(getwifiUserlookup.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not match for Guest Lookup For WiFi Enrollment");
		logger.info("Verified Guest Lookup For WiFi Enrollment with invalid email");
		pageObj.utils().logPass("Verified Guest Lookup For WiFi Enrollment with invalid email");

		// Guest Lookup For WiFi Enrollment, Returns whether a guest exists in the
		// business or not with invalid client.
		logger.info("== Guest Lookup For WiFi Enrollment using invalid client==");
		Response getwifiUserlookup1 = pageObj.endpoints().guestLookupWifiEnrollment(userEmail,
				dataSet.get("invalidclient"), dataSet.get("adminAuthorization"));
		Assert.assertEquals(getwifiUserlookup1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for Guest Lookup For WiFi Enrollment");
		boolean isGuestLookupWifiInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, getwifiUserlookup1.asString());
		Assert.assertTrue(isGuestLookupWifiInvalidClientSchemaValidated,
				"Guest Lookup For WiFi Enrollment API Schema Validation failed");
		logger.info("Verified Guest Lookup For WiFi Enrollment with invalid client");
		pageObj.utils().logPass("Verified Guest Lookup For WiFi Enrollment with invalid client");

		// Guest Lookup For WiFi Enrollment, Returns whether a guest exists in the
		// business or not with invalid admin.
		logger.info("== Guest Lookup For WiFi Enrollment using invalid admin authorization==");
		Response getwifiUserlookup2 = pageObj.endpoints().guestLookupWifiEnrollment(userEmail, dataSet.get("client"),
				dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(getwifiUserlookup2.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not match for Guest Lookup For WiFi Enrollment");
		logger.info("Verified Guest Lookup For WiFi Enrollment with invalid admin");
		pageObj.utils().logPass("Verified Guest Lookup For WiFi Enrollment with invalid admin");
	}

	@Test(description = "SQ-T3113 locations Api Validation negative flow", groups = { "regression" })
	public void locationsApiValidationNegative() {

		String store_number = CreateDateTime.getTimeDateString();
		String invalidstore_number = "";
		String location_name = "Test Location" + store_number;
		String locationGroupName = "Location Group" + store_number;
		// String invaliddataSet.get("adminAuthorization") ="";
		String invalidlocation_group_id = "";

		// Create Location Api
		Response createLocationresponse = pageObj.endpoints().createLocation(location_name, store_number,
				dataSet.get("adminAuthorization"));
		String location_id = createLocationresponse.jsonPath().get("location_id").toString();
		String invalidlocation_id = "a1";
		String storeNumber = createLocationresponse.jsonPath().get("store_number").toString();
		String invalidstoreNumber = "";
		Assert.assertEquals(createLocationresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Create Location");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Create Location is unsuccessful");

		// Create Location with invalid authorization
		pageObj.utils().logit("== Create Location with invalid authorization ==");
		logger.info("== Create Location with invalid authorization ==");
		Response createLocationresponse1 = pageObj.endpoints().createLocation(location_name, store_number, "1");
		Assert.assertEquals(createLocationresponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isCreateLocationInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, createLocationresponse1.asString());
		Assert.assertTrue(isCreateLocationInvalidAuthSchemaValidated, "Create Location Schema Validation failed");
		String createLocationInvalidAuthMsg = createLocationresponse1.jsonPath().get("error");
		Assert.assertEquals(createLocationInvalidAuthMsg, "You need to sign in or sign up before continuing.");
		pageObj.utils().logPass("Platform Functions API Create Location with invalid auth is unsuccessful");
		logger.info("Platform Functions API Create Location with invalid auth is unsuccessful");

		// Create Location with invalid location name
		pageObj.utils().logit("== Create Location with invalid location name ==");
		logger.info("== Create Location with invalid location name ==");
		Response createLocationresponse3 = pageObj.endpoints().createLocation("", store_number,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(createLocationresponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isCreateLocationInvalidNameSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardBlankNameErrorSchema, createLocationresponse3.asString());
		Assert.assertTrue(isCreateLocationInvalidNameSchemaValidated, "Create Location Schema Validation failed");
		String createLocationInvalidNameMsg = createLocationresponse3.jsonPath().get("name[0]");
		Assert.assertEquals(createLocationInvalidNameMsg, "Name can't be blank");
		pageObj.utils().logPass("Platform Functions API Create Location with invalid name is unsuccessful");
		logger.info("Platform Functions API Create Location with invalid name is unsuccessful");

		// Get Location List with invalid authorization
		Response getLocationresponse = pageObj.endpoints().getLocationList(dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(getLocationresponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Get Location List");
		boolean isGetLocationListInvalidAuthSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorObjectSchema, getLocationresponse.asString());
		Assert.assertTrue(isGetLocationListInvalidAuthSchemaValidated,
				"Platform Functions Get Location List Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Get Location List is unsuccessful");

		// Update Location with invalid authorization
		Response updateLocationresponse = pageObj.endpoints().updateLocation(location_id, storeNumber,
				dataSet.get("invalidAdminAuthorization"));
		Assert.assertEquals(updateLocationresponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Update Location");
		boolean isUpdateLocationInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, updateLocationresponse.asString());
		Assert.assertTrue(isUpdateLocationInvalidAuthSchemaValidated,
				"Platform Functions Update Location Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Update Location is unsuccessful");

		// Update Location with invalid location id
		Response updateLocationresponse1 = pageObj.endpoints().updateLocation(invalidlocation_id, storeNumber,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(updateLocationresponse1.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not matched for PLATFORM FUNCTIONS API Update Location");
		boolean isUpdateLocationInvalidIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardInvalidDataErrorSchema, updateLocationresponse1.asString());
		Assert.assertTrue(isUpdateLocationInvalidIdSchemaValidated,
				"Platform Functions Update Location Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Update Location is unsuccessful");

		// Get Location Details with invalid authorization
		Response getLocationDetailsresponse = pageObj.endpoints().getLocationDetails(location_id, storeNumber,
				dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(getLocationDetailsresponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Get Location Details");
		boolean isGetLocationDetailsInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, getLocationDetailsresponse.asString());
		Assert.assertTrue(isGetLocationDetailsInvalidAuthSchemaValidated,
				"Platform Functions Get Location Details Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Get Location Details is unsuccessful");

		// Get Location Group List
		Response getLocationGroupListresponse = pageObj.endpoints()
				.getLocationGroupList(dataSet.get("adminAuthorization"));
		Assert.assertEquals(getLocationGroupListresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Location Group List");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Get Location Group List is successful");
		String location_group_id = getLocationGroupListresponse.jsonPath().get("[0].location_group_id").toString();

		// Get Location Group Details with invalid authorization
		Response getLocationGroupDetailsresponse = pageObj.endpoints().getLocationGroupDetails(location_group_id,
				dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(getLocationGroupDetailsresponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Get Location Group Details");
		boolean isGetLocationGroupDetailsInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, getLocationGroupDetailsresponse.asString());
		Assert.assertTrue(isGetLocationGroupDetailsInvalidAuthSchemaValidated,
				"Platform Functions Get Location Group Details Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Get Location Group Details is unsuccessful");

		// Add Location to Location Group with invalid authorization
		Response addLocationtoGroupresponse = pageObj.endpoints().addLocationtoGroup(
				dataSet.get("invalidadminAuthorization"), location_group_id, store_number, location_id);
		Assert.assertEquals(addLocationtoGroupresponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Add Location to Location Group");
		boolean isAddLocationToGroupInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, addLocationtoGroupresponse.asString());
		Assert.assertTrue(isAddLocationToGroupInvalidAuthSchemaValidated,
				"Platform Functions Add Location to Location Group Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Add Location to Location Group is unsuccessful");

		// Create Location Group
		Response createLocationGroupresponse = pageObj.endpoints().createLocationgroup(locationGroupName, storeNumber,
				location_id, dataSet.get("adminAuthorization"));
		Assert.assertEquals(createLocationGroupresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Location Group");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Create Location Group is successful");
		String locationGroup_id = createLocationGroupresponse.jsonPath().get("location_group_id").toString();
		String invalidlocationGroup_id = "q1";

		// Create Location Group with invalid authorization
		Response createLocationGroupresponse1 = pageObj.endpoints().createLocationgroup(locationGroupName, storeNumber,
				location_id, dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(createLocationGroupresponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Create Location Group");
		boolean isCreateLocationGroupInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, createLocationGroupresponse1.asString());
		Assert.assertTrue(isCreateLocationGroupInvalidAuthSchemaValidated,
				"Platform Functions Create Location Group Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Create Location Group is unsuccessful");

		// Update Location Group with invalid authorization
		Response updateLocationGroupresponse = pageObj.endpoints().updateLocationgroup(locationGroupName,
				locationGroup_id, dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(updateLocationGroupresponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Update Location Group");
		boolean isUpdateLocationGroupInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, updateLocationGroupresponse.asString());
		Assert.assertTrue(isUpdateLocationGroupInvalidAuthSchemaValidated,
				"Platform Functions Update Location Group Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Update Location Group is unsuccessful");

		// Update Location Group with invalid location group id
		Response updateLocationGroupresponse1 = pageObj.endpoints().updateLocationgroup(locationGroupName,
				invalidlocationGroup_id, dataSet.get("adminAuthorization"));
		Assert.assertEquals(updateLocationGroupresponse1.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for PLATFORM FUNCTIONS API Update Location Group");
		boolean isUpdateLocationGroupInvalidIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardNotFoundErrorSchema, updateLocationGroupresponse1.asString());
		Assert.assertTrue(isUpdateLocationGroupInvalidIdSchemaValidated,
				"Platform Functions Update Location Group Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Update Location Group is unsuccessful");

		// Delete Location Group with invalid authorization
		Response deleteLocationGroupresponse = pageObj.endpoints().deleteLocationgroup(locationGroup_id,
				dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(deleteLocationGroupresponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Delete Location Group");
		boolean isDeleteLocationGroupInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, deleteLocationGroupresponse.asString());
		Assert.assertTrue(isDeleteLocationGroupInvalidAuthSchemaValidated,
				"Platform Functions Delete Location Group Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Delete Location Group is unsuccessful");

		// Delete Location Group with invalid location group id
		Response deleteLocationGroupresponse1 = pageObj.endpoints().deleteLocationgroup(invalidlocationGroup_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deleteLocationGroupresponse1.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for PLATFORM FUNCTIONS API Delete Location Group");
		boolean isDeleteLocationGroupInvalidIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardNotFoundErrorSchema, deleteLocationGroupresponse1.asString());
		Assert.assertTrue(isDeleteLocationGroupInvalidIdSchemaValidated,
				"Platform Functions Delete Location Group Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Delete Location Group is unsuccessful");

		// Delete Location from Location Group with invalid authorization
		Response deleteLocationfromGroupresponse = pageObj.endpoints().deleteLocationfromGroup(
				dataSet.get("invalidadminAuthorization"), location_group_id, store_number, location_id);
		Assert.assertEquals(deleteLocationfromGroupresponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Delete Location from Location Group");
		boolean isDeleteLocationFromGroupInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, deleteLocationfromGroupresponse.asString());
		Assert.assertTrue(isDeleteLocationFromGroupInvalidAuthSchemaValidated,
				"Platform Functions Delete Location from Location Group Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Delete Location from Location Group is unsuccessful");

		// Delete location with invalid authorization
		Response deleteLocationresponse = pageObj.endpoints().deleteLocation(location_id, storeNumber,
				dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(deleteLocationresponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Delete location");
		boolean isDeleteLocationInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, deleteLocationresponse.asString());
		Assert.assertTrue(isDeleteLocationInvalidAuthSchemaValidated,
				"Platform Functions Delete Location Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Delete locationis unsuccessful");

	}

	@Test(description = "SQ-T3115 users Api Validation negative flow", groups = { "regression" })
	public void usersApiValidationNegative() {
		String invaliduserID = "gh";
		// String invaliddataSet.get("adminAuthorization")="";
		String invaliduserEmail = "abc";
		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// Ban a User with invalid user id
		Response banUserresponse = pageObj.endpoints().banUser(invaliduserID, dataSet.get("adminAuthorization"));
		Assert.assertEquals(banUserresponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for PLATFORM FUNCTIONS API Ban a User");
		boolean isBanUserInvalidIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardUserNotFoundErrorSchema, banUserresponse.asString());
		Assert.assertTrue(isBanUserInvalidIdSchemaValidated,
				"Platform Functions API Ban a User Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Ban a User is unsuccessful");

		// Ban a User with invalid authorization
		Response banUserresponse1 = pageObj.endpoints().banUser(userID, dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(banUserresponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Ban a User");
		boolean isBanUserInvalidAuthSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorObjectSchema, banUserresponse1.asString());
		Assert.assertTrue(isBanUserInvalidAuthSchemaValidated,
				"Platform Functions API Ban a User Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Ban a User is unsuccessful");

		// UnBan a User with invalid userId
		Response unBanUserresponse = pageObj.endpoints().unBanUser(invaliduserID, dataSet.get("adminAuthorization"));
		Assert.assertEquals(unBanUserresponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for PLATFORM FUNCTIONS API UnBan a User");
		boolean isUnBanUserInvalidIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardUserNotFoundErrorSchema, unBanUserresponse.asString());
		Assert.assertTrue(isUnBanUserInvalidIdSchemaValidated,
				"Platform Functions API UnBan a User Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API UnBan a User is unsuccessful");

		// UnBan a User with invalid authorization
		Response unBanUserresponse1 = pageObj.endpoints().unBanUser(userID, dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(unBanUserresponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API UnBan a User");
		boolean isUnBanUserInvalidAuthSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorObjectSchema, unBanUserresponse1.asString());
		Assert.assertTrue(isUnBanUserInvalidAuthSchemaValidated,
				"Platform Functions API UnBan a User Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API UnBan a User is unsuccessful");

		// Send Message to User (Parametrize for other envs) with invalid user Id
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(invaliduserID,
				dataSet.get("adminAuthorization"), dataSet.get("amount"), "2367593");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for PLATFORM FUNCTIONS API send message to user");
		boolean isSendMessageInvalidIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardUserNotFoundErrorSchema, sendRewardResponse.asString());
		Assert.assertTrue(isSendMessageInvalidIdSchemaValidated,
				"Platform Functions API Send Message to User Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Send Message to User is unsuccessful");

		// Send Message to User (Parametrize for other envs) with invalid authorization
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID,
				dataSet.get("invalidadminAuthorization"), dataSet.get("amount"), "2367593");
		Assert.assertEquals(sendRewardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API send message to user");
		boolean isSendMessageInvalidAuthSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorObjectSchema, sendRewardResponse1.asString());
		Assert.assertTrue(isSendMessageInvalidAuthSchemaValidated,
				"Platform Functions API Send Message to User Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Send Message to User is unsuccessful");

		// Send Message to User (Parametrize for other envs) with invalid amount or
		// empty
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				dataSet.get("invalidamount"), "2367593");
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for PLATFORM FUNCTIONS API send message to user");
		boolean isSendMessageInvalidAmountSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardRewardAmountErrorSchema, sendRewardResponse2.asString());
		Assert.assertTrue(isSendMessageInvalidAmountSchemaValidated,
				"Platform Functions API Send Message to User Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Send Message to User is unsuccessful");

		// Support Gifting to a User with invalid authorization
		Response supportGiftingResponse1 = pageObj.endpoints().supportGiftingToUser(userID,
				dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(supportGiftingResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Support Gifting to a User");
		boolean isSupportGiftingInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, supportGiftingResponse1.asString());
		Assert.assertTrue(isSupportGiftingInvalidAuthSchemaValidated,
				"Platform Functions API Support Gifting to a User Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Support Gifting to a User is unsuccessful");

		// Deactivate a User with invalid user id
		Response deactivateUserresponse = pageObj.endpoints().deactivateUser(invaliduserID,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deactivateUserresponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for PLATFORM FUNCTIONS API Deactivate a User");
		boolean isDeactivateUserInvalidIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardUserNotFoundErrorSchema, deactivateUserresponse.asString());
		Assert.assertTrue(isDeactivateUserInvalidIdSchemaValidated,
				"Platform Functions API Deactivate a User Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Deactivate a User is unsuccessful");

		// Deactivate a User with invalid authorization
		Response deactivateUserresponse1 = pageObj.endpoints().deactivateUser(userID,
				dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(deactivateUserresponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Deactivate a User");
		boolean isDeactivateUserInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, deactivateUserresponse1.asString());
		Assert.assertTrue(isDeactivateUserInvalidAuthSchemaValidated,
				"Platform Functions API Deactivate a User Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Deactivate a User is unsuccessful");

		// Reactivate a User
		Response reactivateUserresponse = pageObj.endpoints().reactivateUser(userID, dataSet.get("adminAuthorization"));
		Assert.assertEquals(reactivateUserresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Reactivate a User");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Reactivate a User is successful");

		// Reactivate a User with invalid user id
		Response reactivateUserresponse2 = pageObj.endpoints().reactivateUser(invaliduserID,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(reactivateUserresponse2.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for PLATFORM FUNCTIONS API Reactivate a User");
		boolean isReactivateUserInvalidIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardUserNotFoundErrorSchema, reactivateUserresponse2.asString());
		Assert.assertTrue(isReactivateUserInvalidIdSchemaValidated,
				"Platform Functions API Reactivate a User Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Reactivate a User is unsuccessful");

		// Reactivate a User with invalid authorization
		Response reactivateUserresponse1 = pageObj.endpoints().reactivateUser(userID,
				dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(reactivateUserresponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Reactivate a User");
		boolean isReactivateUserInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, reactivateUserresponse1.asString());
		Assert.assertTrue(isReactivateUserInvalidAuthSchemaValidated,
				"Platform Functions API Reactivate a User Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Reactivate a User is unsuccessful");

		// Get Location List
		Response getLocationresponse = pageObj.endpoints().getLocationList(dataSet.get("adminAuthorization"));
		Assert.assertEquals(getLocationresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Location List");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Get Location List is successful");
		String locationid = getLocationresponse.jsonPath().get("[0].location_id").toString();

		// Update a User with invalid user id
		Response updateUserResponse = pageObj.endpoints().updateUser(invaliduserID, userEmail, locationid,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(updateUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for PLATFORM FUNCTIONS API Update a User");
		boolean isUpdateUserInvalidIdSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorsObjectSchema, updateUserResponse.asString());
		Assert.assertTrue(isUpdateUserInvalidIdSchemaValidated,
				"Platform Functions API Update a User Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Update a User is unsuccessful");

		// Update a User with invalid authorization
		Response updateUserResponse1 = pageObj.endpoints().updateUser(userID, userEmail, locationid,
				dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(updateUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Update a User");
		boolean isUpdateUserInvalidAuthSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorObjectSchema, updateUserResponse1.asString());
		Assert.assertTrue(isUpdateUserInvalidAuthSchemaValidated,
				"Platform Functions API Update a User Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Update a User is unsuccessful");

		// Update a User with invalid user email
		Response updateUserResponse2 = pageObj.endpoints().updateUser(userID, invaliduserEmail, locationid,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(updateUserResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for PLATFORM FUNCTIONS API Update a User");
		boolean isUpdateUserInvalidEmailSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorsObjectSchema, updateUserResponse2.asString());
		Assert.assertTrue(isUpdateUserInvalidEmailSchemaValidated,
				"Platform Functions API Update a User Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Update a User is unsuccessful");

		// Get User Export with invalid authorization
		Response userExportResponse1 = pageObj.endpoints().userExport(userID, dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(userExportResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Get User Export");
		boolean isUserExportInvalidAuthSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorObjectSchema, userExportResponse1.asString());
		Assert.assertTrue(isUserExportInvalidAuthSchemaValidated,
				"Platform Functions API Get User Export Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Get User Export is unsuccessful");

		// Get Extended User History with invalid user id
		Response extendedUserHistoryResponse1 = pageObj.endpoints().extendedUserHistory(invaliduserID,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(extendedUserHistoryResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for PLATFORM FUNCTIONS API Get Extended User History");
		boolean isExtendedUserHistoryInvalidIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardUserNotFoundErrorSchema, extendedUserHistoryResponse1.asString());
		Assert.assertTrue(isExtendedUserHistoryInvalidIdSchemaValidated,
				"Platform Functions API Get Extended User History Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Get Extended User History is unsuccessful");

		// Get Extended User History with invalid authorization
		Response extendedUserHistoryResponse2 = pageObj.endpoints().extendedUserHistory(userID,
				dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(extendedUserHistoryResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Get Extended User History");
		boolean isExtendedUserHistoryInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, extendedUserHistoryResponse2.asString());
		Assert.assertTrue(isExtendedUserHistoryInvalidAuthSchemaValidated,
				"Platform Functions API Get Extended User History Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Get Extended User History is unsuccessful");

		// Update a User
		Response updateUserResponse3 = pageObj.endpoints().updateUser(userID, userEmail, locationid,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(updateUserResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Update a User");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Update a User is successful");

		// Get User Export
		Response userExportResponse = pageObj.endpoints().userExport(userID, dataSet.get("adminAuthorization"));
		Assert.assertEquals(userExportResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get User Export");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Get User Export is successful");

		// Get Extended User History
		Response extendedUserHistoryResponse = pageObj.endpoints().extendedUserHistory(userID,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(extendedUserHistoryResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Extended User History");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Get Extended User History is successful");

		// Fetch User Favourite Locations
		Response fetchUserLocationsResponse = pageObj.endpoints().fetchUserLocation(userID,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(fetchUserLocationsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Fetch User Favourite Locations");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Fetch User Favourite Locations is successful");
		String favourite_location_id = fetchUserLocationsResponse.jsonPath().get("[0].user_favourite_location_id")
				.toString();
		String invalidfavourite_location_id = "";

		// Fetch User Favourite Locations with invalid user id
		Response fetchUserLocationsResponse1 = pageObj.endpoints().fetchUserLocation(invaliduserID,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(fetchUserLocationsResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for PLATFORM FUNCTIONS API Fetch User Favourite Locations");
		boolean isFetchUserLocationsInvalidIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardUserNotFoundErrorSchema, fetchUserLocationsResponse1.asString());
		Assert.assertTrue(isFetchUserLocationsInvalidIdSchemaValidated,
				"Platform Functions API Fetch User Favourite Locations Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Fetch User Favourite Locations is unsuccessful");

		// Fetch User Favourite Locations with invalid authorization
		Response fetchUserLocationsResponse2 = pageObj.endpoints().fetchUserLocation(userID,
				dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(fetchUserLocationsResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Fetch User Favourite Locations");
		boolean isFetchUserLocationsInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, fetchUserLocationsResponse2.asString());
		Assert.assertTrue(isFetchUserLocationsInvalidAuthSchemaValidated,
				"Platform Functions API Fetch User Favourite Locations Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Fetch User Favourite Locations is unsuccessful");

		// Delete User Favourite Location with invalid user id
		Response deleteUserLocationsResponse = pageObj.endpoints().deleteUserLocation(invaliduserID,
				favourite_location_id, dataSet.get("adminAuthorization"));
		Assert.assertEquals(deleteUserLocationsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for PLATFORM FUNCTIONS API Delete User Favourite Location");
		boolean isDeleteUserLocationInvalidUserIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardUserNotFoundErrorSchema, deleteUserLocationsResponse.asString());
		Assert.assertTrue(isDeleteUserLocationInvalidUserIdSchemaValidated,
				"Platform Functions API Delete User Favourite Location Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Delete User Favourite Location is unsuccessful");

		// Delete User Favourite Location with invalid favourite location id
		Response deleteUserLocationsResponse1 = pageObj.endpoints().deleteUserLocation(userID,
				invalidfavourite_location_id, dataSet.get("adminAuthorization"));
		Assert.assertEquals(deleteUserLocationsResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for PLATFORM FUNCTIONS API Delete User Favourite Location");
		boolean isDeleteUserLocationInvalidLocationIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardFavLocationNotFoundErrorSchema, deleteUserLocationsResponse1.asString());
		Assert.assertTrue(isDeleteUserLocationInvalidLocationIdSchemaValidated,
				"Platform Functions API Delete User Favourite Location Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Delete User Favourite Location is unsuccessful");

		// Delete User Favourite Location with invalid authorization
		Response deleteUserLocationsResponse2 = pageObj.endpoints().deleteUserLocation(userID, favourite_location_id,
				dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(deleteUserLocationsResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Delete User Favourite Location");
		boolean isDeleteUserLocationInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, deleteUserLocationsResponse2.asString());
		Assert.assertTrue(isDeleteUserLocationInvalidAuthSchemaValidated,
				"Platform Functions API Delete User Favourite Location Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Delete User Favourite Location is unsuccessful");

		// Delete a User with invalid user id
		Response deleteUserResponse = pageObj.endpoints().deleteUser(invaliduserID, dataSet.get("adminAuthorization"));
		Assert.assertEquals(deleteUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for PLATFORM FUNCTIONS API Delete a User");
		boolean isDeleteUserInvalidIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardUserNotFoundErrorSchema, deleteUserResponse.asString());
		Assert.assertTrue(isDeleteUserInvalidIdSchemaValidated,
				"Platform Functions API Delete a User Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Delete a User is unsuccessful");

		// Delete a User with invalid authorizatioon
		Response deleteUserResponse1 = pageObj.endpoints().deleteUser(userID, dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(deleteUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Delete a User");
		boolean isDeleteUserInvalidAuthSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorObjectSchema, deleteUserResponse1.asString());
		Assert.assertTrue(isDeleteUserInvalidAuthSchemaValidated,
				"Platform Functions API Delete a User Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Delete a User is unsuccessful");
	}

	@Test(description = "SQ-T3116 business Migration User Api Validation negative flow; "
			+ "SQ-T4926 Verify API2 Migration user look-up (POST and GET calls) negative scenarios", groups = {
					"regression" })
	public void businessMigrationUserApiValidationNegative() {
		String invaliduserEmail = "abc";
		String invalidmigration_user_id = "1";

		// Create Business Migration User with valid data
		pageObj.utils().logit("== Create Business Migration User with valid data ==");
		logger.info("== Create Business Migration User with valid data ==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserResponse = pageObj.endpoints().createBusinessMigrationUse(userEmail,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(createMigrationUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Migration User");
		String migration_user_id = createMigrationUserResponse.jsonPath().get("migration_user_id").toString();
		String card_number = createMigrationUserResponse.jsonPath().get("original_membership_no");
		String email = createMigrationUserResponse.jsonPath().get("email").toString();
		pageObj.utils().logPass("Platform Functions API Create Business Migration User is successful");
		logger.info("Platform Functions API Create Business Migration User is successful");

		// Negative case: Create Business Migration User with invalid user email
		pageObj.utils().logit("== Create Business Migration User with invalid user email ==");
		logger.info("== Create Business Migration User with invalid user email ==");
		Response createMigrationUserResponse1 = pageObj.endpoints().createBusinessMigrationUse(invaliduserEmail,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(createMigrationUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for PLATFORM FUNCTIONS API Create Business Migration User");
		boolean isCreateMigrationUserInvalidEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiEmailObjectSchema, createMigrationUserResponse1.asString());
		Assert.assertTrue(isCreateMigrationUserInvalidEmailSchemaValidated,
				"Platform Functions API Create Business Migration User Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Create Business Migration User is unsuccessful");
		logger.info("Platform Functions API Create Business Migration User is unsuccessful due to invalid email");

		// Negative case: Create Business Migration User with invalid authorization
		pageObj.utils().logit("== Create Business Migration User with invalid authorization ==");
		logger.info("== Create Business Migration User with invalid authorization ==");
		Response createMigrationUserResponse2 = pageObj.endpoints().createBusinessMigrationUse(userEmail,
				dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(createMigrationUserResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Create Business Migration User");
		boolean isCreateMigrationUserInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, createMigrationUserResponse2.asString());
		Assert.assertTrue(isCreateMigrationUserInvalidAuthSchemaValidated,
				"Platform Functions API Create Business Migration User Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Create Business Migration User is unsuccessful");
		logger.info(
				"Platform Functions API Create Business Migration User is unsuccessful due to invalid authorization");

		// Migration User look-up (POST call) with valid data
		pageObj.utils().logit("== Migration User look-up (POST call) with valid data ==");
		logger.info("== Migration User look-up (POST call) with valid data ==");
		Response migrationUserPostResponse = pageObj.endpoints().api2MigrationLookupPost(card_number, userEmail,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(migrationUserPostResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isMigrationLookupPostSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2MigrationUserLookupSchema, migrationUserPostResponse.asString());
		Assert.assertTrue(isMigrationLookupPostSchemaValidated,
				"API2 Migration user look-up (POST call) Schema Validation failed");
		String migrationLookupPostEmail = migrationUserPostResponse.jsonPath().get("email").toString();
		Assert.assertEquals(migrationLookupPostEmail, email, "Email did not match.");
		pageObj.utils().logPass("API2 migration look-up (POST call) is successful");
		logger.info("API2 migration look-up (POST call) is successful");

		// Negative case: Migration User look-up (POST call) with invalid email
		pageObj.utils().logit("== Migration User look-up (POST call) with invalid email ==");
		logger.info("== Migration User look-up (POST call) with invalid email ==");
		Response migrationUserPostInvalidEmailResponse = pageObj.endpoints().api2MigrationLookupPost(card_number, "1",
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(migrationUserPostInvalidEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isMigrationLookupPostInvalidEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema, migrationUserPostInvalidEmailResponse.asString());
		Assert.assertTrue(isMigrationLookupPostInvalidEmailSchemaValidated,
				"API2 Migration user look-up (POST call) with invalid email Schema Validation failed");
		String migrationLookupPostInvalidEmailMsg = migrationUserPostInvalidEmailResponse.jsonPath().get("errors")
				.toString();
		Assert.assertEquals(migrationLookupPostInvalidEmailMsg, "Incorrect information submitted. Please retry.");
		pageObj.utils().logPass("API2 migration look-up (POST call) is unsuccessful because of invalid email");
		logger.info("API2 migration look-up (POST call) is unsuccessful because of invalid email");

		// Negative case: Migration User look-up (POST call) with invalid client
		pageObj.utils().logit("== Migration User look-up (POST call) with invalid client ==");
		logger.info("== Migration User look-up (POST call) with invalid client ==");
		Response migrationUserPostInvalidClientResponse = pageObj.endpoints().api2MigrationLookupPost(card_number,
				userEmail, "1", dataSet.get("secret"));
		Assert.assertEquals(migrationUserPostInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isMigrationLookupPostInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, migrationUserPostInvalidClientResponse.asString());
		Assert.assertTrue(isMigrationLookupPostInvalidClientSchemaValidated,
				"API2 Migration user look-up (POST call) with invalid client Schema Validation failed");
		String migrationLookupPostInvalidClientMsg = migrationUserPostInvalidClientResponse.jsonPath()
				.get("errors.unknown_client[0]").toString();
		Assert.assertEquals(migrationLookupPostInvalidClientMsg,
				"Client ID is incorrect. Please check client param or contact us");
		pageObj.utils().logPass("API2 migration look-up (POST call) is unsuccessful because of invalid client");
		logger.info("API2 migration look-up (POST call) is unsuccessful because of invalid client");

		// Negative case: Migration User look-up (POST call) with missing client
		pageObj.utils().logit("== Migration User look-up (POST call) with missing client ==");
		logger.info("== Migration User look-up (POST call) with missing client ==");
		Response migrationUserPostMissingClientResponse = pageObj.endpoints().api2MigrationLookupPost(card_number,
				userEmail, "", dataSet.get("secret"));
		Assert.assertEquals(migrationUserPostMissingClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST);
		boolean isMigrationLookupPostMissingClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2MissingClientSchema, migrationUserPostMissingClientResponse.asString());
		Assert.assertTrue(isMigrationLookupPostMissingClientSchemaValidated,
				"API2 Migration user look-up (POST call) with missing client Schema Validation failed");
		String migrationLookupPostMissingClientMsg = migrationUserPostMissingClientResponse.jsonPath()
				.get("errors.client").toString();
		Assert.assertEquals(migrationLookupPostMissingClientMsg, "Required parameter missing or the value is empty.");
		pageObj.utils().logPass("API2 migration look-up (POST call) is unsuccessful because of missing client");
		logger.info("API2 migration look-up (POST call) is unsuccessful because of missing client");

		// Negative case: Migration User look-up (POST call) with invalid secret
		pageObj.utils().logit("== Migration User look-up (POST call) with invalid secret ==");
		logger.info("== Migration User look-up (POST call) with invalid secret ==");
		Response migrationUserPostInvalidSecretResponse = pageObj.endpoints().api2MigrationLookupPost(card_number,
				userEmail, dataSet.get("client"), "1");
		Assert.assertEquals(migrationUserPostInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED);
		boolean isMigrationLookupPostInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, migrationUserPostInvalidSecretResponse.asString());
		Assert.assertTrue(isMigrationLookupPostInvalidSecretSchemaValidated,
				"API2 Migration user look-up (POST call) with invalid secret Schema Validation failed");
		String migrationLookupPostInvalidSecretMsg = migrationUserPostInvalidSecretResponse.jsonPath()
				.get("errors.invalid_signature[0]").toString();
		Assert.assertEquals(migrationLookupPostInvalidSecretMsg,
				"Signature doesn't match. For information about generating the x-pch-digest header, see https://developers.punchh.com.");
		pageObj.utils().logPass("API2 migration look-up (POST call) is unsuccessful because of invalid secret");
		logger.info("API2 migration look-up (POST call) is unsuccessful because of invalid secret");

		// Negative case: Verify API2 Migration user look-up (GET call) using invalid
		// email
		pageObj.utils().logit("== Migration user look-up (GET call) using invalid email ==");
		logger.info("== Migration user look-up (GET call) using invalid email ==");
		Response migrationLookupInvalidEmailResponse = pageObj.endpoints().api2MigrationLookup(card_number,
				dataSet.get("invalidEmail"), dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(migrationLookupInvalidEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for API2 migration look-up");
		boolean isBasicMigrationLookupInvalidEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema, migrationLookupInvalidEmailResponse.asString());
		Assert.assertTrue(isBasicMigrationLookupInvalidEmailSchemaValidated,
				"API v2 Basic Migration user look-up Schema Validation failed");
		pageObj.utils().logPass("API2 migration look-up (GET call) is unsuccessful because of invalid email");
		logger.info("API2 migration look-up (GET call) is unsuccessful because of invalid email");

		// Negative case: Verify API2 Migration user look-up using invalid client
		pageObj.utils().logit("== Migration user look-up (GET call) using invalid client ==");
		logger.info("== Migration user look-up (GET call) using invalid client ==");
		Response migrationLookupInvalidClientResponse = pageObj.endpoints().api2MigrationLookup(card_number, email,
				dataSet.get("invalidClient"), dataSet.get("secret"));
		Assert.assertEquals(migrationLookupInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for API2 migration look-up");
		boolean isBasicMigrationLookupInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, migrationLookupInvalidClientResponse.asString());
		Assert.assertTrue(isBasicMigrationLookupInvalidClientSchemaValidated,
				"API v2 Basic Migration user look-up Schema Validation failed");
		pageObj.utils().logPass("API2 migration look-up (GET call) is unsuccessful because of invalid client");
		logger.info("API2 migration look-up (GET call) is unsuccessful because of invalid client");

		// Negative case: Verify API2 Migration user look-up using invalid secret
		pageObj.utils().logit("== Migration user look-up (GET call) using invalid secret ==");
		logger.info("== Migration user look-up (GET call) using invalid secret ==");
		Response migrationLookupInvalidSecretResponse = pageObj.endpoints().api2MigrationLookup(card_number, email,
				dataSet.get("client"), dataSet.get("invalidSecret"));
		Assert.assertEquals(migrationLookupInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for API2 migration look-up");
		boolean isBasicMigrationLookupInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, migrationLookupInvalidSecretResponse.asString());
		Assert.assertTrue(isBasicMigrationLookupInvalidSecretSchemaValidated,
				"API v2 Basic Migration user look-up Schema Validation failed");
		pageObj.utils().logPass("API2 migration look-up (GET call) is unsuccessful because of invalid secret");
		logger.info("API2 migration look-up (GET call) is unsuccessful because of invalid secret");

		// Negative case: Update Business Migration User with invalid mail id
		pageObj.utils().logit("== Update Business Migration User with invalid mail id ==");
		logger.info("== Update Business Migration User with invalid mail id ==");
		Response updateMigrationUserResponse = pageObj.endpoints().updateBusinessMigrationUse(invaliduserEmail,
				dataSet.get("adminAuthorization"), migration_user_id);
		Assert.assertEquals(updateMigrationUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for PLATFORM FUNCTIONS API Update Business Migration User");
		boolean isUpdateMigrationUserInvalidEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiEmailObjectSchema, updateMigrationUserResponse.asString());
		Assert.assertTrue(isUpdateMigrationUserInvalidEmailSchemaValidated,
				"Platform Functions API Update Business Migration User Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Update Business Migration User is unsuccessful due to invalid email");
		logger.info("PLATFORM FUNCTIONS API Update Business Migration User is unsuccessful due to invalid email");

		// Negative case: Update Business Migration User with invalid authorization
		pageObj.utils().logit("== Update Business Migration User with invalid authorization ==");
		logger.info("== Update Business Migration User with invalid authorization ==");
		Response updateMigrationUserResponse1 = pageObj.endpoints().updateBusinessMigrationUse(userEmail,
				dataSet.get("invalidadminAuthorization"), migration_user_id);
		Assert.assertEquals(updateMigrationUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Update Business Migration User");
		boolean isUpdateMigrationUserInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, updateMigrationUserResponse1.asString());
		Assert.assertTrue(isUpdateMigrationUserInvalidAuthSchemaValidated,
				"Platform Functions API Update Business Migration User Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Update Business Migration User is unsuccessful");
		logger.info(
				"PLATFORM FUNCTIONS API Update Business Migration User is unsuccessful due to invalid authorization");

		// Negative case: Update Business Migration User with invalid migration user id
		pageObj.utils().logit("== Update Business Migration User with invalid migration user id ==");
		logger.info("== Update Business Migration User with invalid migration user id ==");
		Response updateMigrationUserResponse2 = pageObj.endpoints().updateBusinessMigrationUse(userEmail,
				dataSet.get("adminAuthorization"), invalidmigration_user_id);
		Assert.assertEquals(updateMigrationUserResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for PLATFORM FUNCTIONS API Update Business Migration User");
		boolean isUpdateMigrationUserInvalidIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardNotFoundErrorSchema, updateMigrationUserResponse2.asString());
		Assert.assertTrue(isUpdateMigrationUserInvalidIdSchemaValidated,
				"Platform Functions API Update Business Migration User Schema Validation failed");
		pageObj.utils().logPass(
				"PLATFORM FUNCTIONS API Update Business Migration User is successful due to invalid migration user id");
		logger.info(
				"PLATFORM FUNCTIONS API Update Business Migration User is successful due to invalid migration user id");

		// Negative case: Delete Business Migration User with invalid authorization
		pageObj.utils().logit("== Delete Business Migration User with invalid authorization ==");
		logger.info("== Delete Business Migration User with invalid authorization ==");
		Response deleteMigrationUserResponse = pageObj.endpoints()
				.deleteBusinessMigrationUse(dataSet.get("invalidadminAuthorization"), migration_user_id);
		Assert.assertEquals(deleteMigrationUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Delete Business Migration User");
		boolean isDeleteMigrationUserInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, deleteMigrationUserResponse.asString());
		Assert.assertTrue(isDeleteMigrationUserInvalidAuthSchemaValidated,
				"Platform Functions API Delete Business Migration User Schema Validation failed");
		pageObj.utils().logPass(
				"PLATFORM FUNCTIONS API Delete Business Migration User is unsuccessful due to invalid authorization");
		logger.info(
				"PLATFORM FUNCTIONS API Delete Business Migration User is unsuccessful due to invalid authorization");

		// Negative case: Delete Business Migration User with invalid migration user id
		pageObj.utils().logit("== Delete Business Migration User with invalid migration user id ==");
		logger.info("== Delete Business Migration User with invalid migration user id ==");
		Response deleteMigrationUserResponse1 = pageObj.endpoints()
				.deleteBusinessMigrationUse(dataSet.get("adminAuthorization"), invalidmigration_user_id);
		Assert.assertEquals(deleteMigrationUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for PLATFORM FUNCTIONS API Delete Business Migration User");
		boolean isDeleteMigrationUserInvalidIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardNotFoundErrorSchema, deleteMigrationUserResponse1.asString());
		Assert.assertTrue(isDeleteMigrationUserInvalidIdSchemaValidated,
				"Platform Functions API Delete Business Migration User Schema Validation failed");
		pageObj.utils().logPass(
				"PLATFORM FUNCTIONS API Delete Business Migration User is unsuccessful due to invalid migration user id");
		logger.info(
				"PLATFORM FUNCTIONS API Delete Business Migration User is unsuccessful due to invalid migration user id");

	}

	@Test(description = "SQ-T3117 business Admin Api Validation negative flow", groups = { "regression" })
	public void businessAdminApiValidationNegative() {
		// String invaliddataSet.get("adminAuthorization")="";
		String invaliduserEmail = "abc";
		String invalidrole_id = "1";
		String invalidbusiness_admin_id = "";

		// Get Admin Roles List with valid data
		pageObj.utils().logit("== Platform Functions API: Get Admin Roles List with valid data ==");
		logger.info("== Platform Functions API: Get Admin Roles List with valid data ==");
		Response getAdminRoleListResponse = pageObj.endpoints().getAdminRolesList(dataSet.get("adminAuthorization"));
		Assert.assertEquals(getAdminRoleListResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Admin Roles List with valid data");
		String role_id = getAdminRoleListResponse.jsonPath().get("[0].role_id").toString();
		pageObj.utils().logPass("Platform Functions API Get Admin Roles List call with valid data is successful");
		logger.info("Platform Functions API Get Admin Roles List call with valid data is successful");

		// Create Business Admin with valid data
		pageObj.utils().logit("== Platform Functions API: Create Business Admin with valid data ==");
		logger.info("== Platform Functions API: Create Business Admin with valid data ==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createBusinessAdminResponse = pageObj.endpoints().createBusinesAdmin(userEmail, role_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(createBusinessAdminResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Admin with valid data");
		String business_admin_id = createBusinessAdminResponse.jsonPath().get("business_admin_id").toString();
		pageObj.utils().logPass("Platform Functions API Create Business Admin call with valid data is successful");
		logger.info("Platform Functions API Create Business Admin call with valid data is successful");

		// Get Admin Roles List with invalid authorization
		pageObj.utils().logit("== Platform Functions API: Get Admin Roles List with invalid authorization ==");
		logger.info("== Platform Functions API: Get Admin Roles List with invalid authorization ==");
		Response getAdminRoleListResponse1 = pageObj.endpoints()
				.getAdminRolesList(dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(getAdminRoleListResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Get Admin Roles List with invalid authorization");
		boolean isGetAdminRoleListInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, getAdminRoleListResponse1.asString());
		Assert.assertTrue(isGetAdminRoleListInvalidAuthSchemaValidated,
				"Platform Functions API Get Admin Roles List Schema Validation failed");
		pageObj.utils().logPass("Platform Functions API Get Admin Roles List call with invalid authorization is unsuccessful");
		logger.info("Platform Functions API Get Admin Roles List call with invalid authorization is unsuccessful");

		// Create Business Admin with invalid user email
		pageObj.utils().logit("== Platform Functions API: Create Business Admin with invalid user email ==");
		logger.info("== Platform Functions API: Create Business Admin with invalid user email ==");
		// userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createBusinessAdminResponse1 = pageObj.endpoints().createBusinesAdmin(invaliduserEmail, role_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(createBusinessAdminResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for PLATFORM FUNCTIONS API Create Business Admin with invalid user email");
		boolean isCreateBusinessAdminInvalidEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2EmailErrorSchema, createBusinessAdminResponse1.asString());
		Assert.assertTrue(isCreateBusinessAdminInvalidEmailSchemaValidated,
				"Platform Functions API Create Business Admin Schema Validation failed");
		pageObj.utils().logPass("Platform Functions API Create Business Admin call with invalid user email is unsuccessful");
		logger.info("Platform Functions API Create Business Admin call with invalid user email is unsuccessful");

		// Create Business Admin with invalid role id
		pageObj.utils().logit("== Platform Functions API: Create Business Admin with invalid role id ==");
		logger.info("== Platform Functions API: Create Business Admin with invalid role id ==");
		// userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createBusinessAdminResponse2 = pageObj.endpoints().createBusinesAdmin(userEmail, invalidrole_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(createBusinessAdminResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for PLATFORM FUNCTIONS API Create Business Admin with invalid role id");
		boolean isCreateBusinessAdminInvalidRoleIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardInvalidAdminRoleErrorSchema, createBusinessAdminResponse2.asString());
		Assert.assertTrue(isCreateBusinessAdminInvalidRoleIdSchemaValidated,
				"Platform Functions API Create Business Admin Schema Validation failed");
		pageObj.utils().logPass("Platform Functions API Create Business Admin call with invalid role id is unsuccessful");
		logger.info("Platform Functions API Create Business Admin call with invalid role id is unsuccessful");

		// Create Business Admin with invalid authorization
		pageObj.utils().logit("== Platform Functions API: Create Business Admin with invalid authorization ==");
		logger.info("== Platform Functions API: Create Business Admin with invalid authorization ==");
		// userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createBusinessAdminResponse3 = pageObj.endpoints().createBusinesAdmin(userEmail, role_id,
				dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(createBusinessAdminResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Create Business Admin with invalid authorization");
		boolean isCreateBusinessAdminInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, createBusinessAdminResponse3.asString());
		Assert.assertTrue(isCreateBusinessAdminInvalidAuthSchemaValidated,
				"Platform Functions API Create Business Admin Schema Validation failed");
		pageObj.utils().logPass("Platform Functions API Create Business Admin call with invalid authorization is unsuccessful");
		logger.info("Platform Functions API Create Business Admin call with invalid authorization is unsuccessful");

		// Update Business Admin with invalid business admin id
		pageObj.utils().logit("== Platform Functions API: Update Business Admin with invalid business admin id ==");
		logger.info("== Platform Functions API: Update Business Admin with invalid business admin id ==");
		Response updateBusinessAdminResponse = pageObj.endpoints().updateBusinesAdmin(invalidbusiness_admin_id,
				userEmail, role_id, dataSet.get("adminAuthorization"));
		Assert.assertEquals(updateBusinessAdminResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not matched for PLATFORM FUNCTIONS API Update Business Admin with invalid business admin id");
		boolean isUpdateBusinessAdminInvalidIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, updateBusinessAdminResponse.asString());
		Assert.assertTrue(isUpdateBusinessAdminInvalidIdSchemaValidated,
				"Platform Functions API Update Business Admin Schema Validation failed");
		pageObj.utils().logPass(
				"Platform Functions API Update Business Admin call with invalid business admin id is unsuccessful");
		logger.info("Platform Functions API Update Business Admin call with invalid business admin id is unsuccessful");

		// Update Business Admin with invalid user mail id
		pageObj.utils().logit("== Platform Functions API: Update Business Admin with invalid user email ==");
		logger.info("== Platform Functions API: Update Business Admin with invalid user email ==");
		Response updateBusinessAdminResponse1 = pageObj.endpoints().updateBusinesAdmin(business_admin_id,
				invaliduserEmail, role_id, dataSet.get("adminAuthorization"));
		Assert.assertEquals(updateBusinessAdminResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for PLATFORM FUNCTIONS API Update Business Admin with invalid user email");
		boolean isUpdateBusinessAdminInvalidEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2EmailErrorSchema, updateBusinessAdminResponse1.asString());
		Assert.assertTrue(isUpdateBusinessAdminInvalidEmailSchemaValidated,
				"Platform Functions API Update Business Admin Schema Validation failed");
		pageObj.utils().logPass("Platform Functions API Update Business Admin call with invalid user email is unsuccessful");
		logger.info("Platform Functions API Update Business Admin call with invalid user email is unsuccessful");

		// Update Business Admin with invalid role id
		pageObj.utils().logit("== Platform Functions API: Update Business Admin with invalid role id ==");
		logger.info("== Platform Functions API: Update Business Admin with invalid role id ==");
		Response updateBusinessAdminResponse2 = pageObj.endpoints().updateBusinesAdmin(business_admin_id, userEmail,
				invalidrole_id, dataSet.get("adminAuthorization"));
		Assert.assertEquals(updateBusinessAdminResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for PLATFORM FUNCTIONS API Update Business Admin with invalid role id");
		boolean isUpdateBusinessAdminInvalidRoleIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardInvalidRoleIdErrorSchema, updateBusinessAdminResponse2.asString());
		Assert.assertTrue(isUpdateBusinessAdminInvalidRoleIdSchemaValidated,
				"Platform Functions API Update Business Admin Schema Validation failed");
		pageObj.utils().logPass("Platform Functions API Update Business Admin call with invalid role id is unsuccessful");
		logger.info("Platform Functions API Update Business Admin call with invalid role id is unsuccessful");

		// Update Business Admin with invalid authorization
		pageObj.utils().logit("== Platform Functions API: Update Business Admin with invalid authorization ==");
		logger.info("== Platform Functions API: Update Business Admin with invalid authorization ==");
		Response updateBusinessAdminResponse3 = pageObj.endpoints().updateBusinesAdmin(business_admin_id, userEmail,
				role_id, dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(updateBusinessAdminResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Update Business Admin with invalid authorization");
		boolean isUpdateBusinessAdminInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, updateBusinessAdminResponse3.asString());
		Assert.assertTrue(isUpdateBusinessAdminInvalidAuthSchemaValidated,
				"Platform Functions API Update Business Admin Schema Validation failed");
		pageObj.utils().logPass("Platform Functions API Update Business Admin call with invalid authorization is unsuccessful");
		logger.info("Platform Functions API Update Business Admin call with invalid authorization is unsuccessful");

		// Show Business Admin with invalid business admin id
		pageObj.utils().logit("== Platform Functions API: Show Business Admin with invalid business admin id ==");
		logger.info("== Platform Functions API: Show Business Admin with invalid business admin id ==");
		Response showBusinessAdminResponse = pageObj.endpoints().showBusinesAdmin(invalidbusiness_admin_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(showBusinessAdminResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not matched for PLATFORM FUNCTIONS API Show Business Admin with invalid business admin id");
		boolean isShowBusinessAdminInvalidIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, showBusinessAdminResponse.asString());
		Assert.assertTrue(isShowBusinessAdminInvalidIdSchemaValidated,
				"Platform Functions API Show Business Admin Schema Validation failed");
		pageObj.utils().logPass("Platform Functions API Show Business Admin call with invalid business admin id is unsuccessful");
		logger.info("Platform Functions API Show Business Admin call with invalid business admin id is unsuccessful");

		// Show Business Admin with invalid authorization
		pageObj.utils().logit("== Platform Functions API: Show Business Admin with invalid authorization ==");
		logger.info("== Platform Functions API: Show Business Admin with invalid authorization ==");
		Response showBusinessAdminResponse1 = pageObj.endpoints().showBusinesAdmin(business_admin_id,
				dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(showBusinessAdminResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Show Business Admin with invalid authorization");
		boolean isShowBusinessAdminInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, showBusinessAdminResponse1.asString());
		Assert.assertTrue(isShowBusinessAdminInvalidAuthSchemaValidated,
				"Platform Functions API Show Business Admin Schema Validation failed");
		pageObj.utils().logPass("Platform Functions API Show Business Admin call with invalid authorization is unsuccessful");
		logger.info("Platform Functions API Show Business Admin call with invalid authorization is unsuccessful");

		// Delete Business Admin with invalid business admin id
		pageObj.utils().logit("== Platform Functions API: Delete Business Admin with invalid business admin id ==");
		logger.info("== Platform Functions API: Delete Business Admin with invalid business admin id ==");
		Response deleteBusinessAdminResponse = pageObj.endpoints().deleteBusinesAdmin(invalidbusiness_admin_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deleteBusinessAdminResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not matched for PLATFORM FUNCTIONS API Delete Business Admin with invalid business admin id");
		boolean isDeleteBusinessAdminInvalidIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, deleteBusinessAdminResponse.asString());
		Assert.assertTrue(isDeleteBusinessAdminInvalidIdSchemaValidated,
				"Platform Functions API Delete Business Admin Schema Validation failed");
		pageObj.utils().logPass(
				"Platform Functions API Delete Business Admin call with invalid business admin id is unsuccessful");
		logger.info("Platform Functions API Delete Business Admin call with invalid business admin id is unsuccessful");

		// Delete Business Admin with invalid authorization
		pageObj.utils().logit("== Platform Functions API: Delete Business Admin with invalid authorization ==");
		logger.info("== Platform Functions API: Delete Business Admin with invalid authorization ==");
		Response deleteBusinessAdminResponse1 = pageObj.endpoints().deleteBusinesAdmin(business_admin_id,
				dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(deleteBusinessAdminResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Delete Business Admin with invalid authorization");
		boolean isDeleteBusinessAdminInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, deleteBusinessAdminResponse1.asString());
		Assert.assertTrue(isDeleteBusinessAdminInvalidAuthSchemaValidated,
				"Platform Functions API Delete Business Admin Schema Validation failed");
		pageObj.utils().logPass("Platform Functions API Delete Business Admin call with invalid authorization is unsuccessful");
		logger.info("Platform Functions API Delete Business Admin call with invalid authorization is unsuccessful");

		// Invite Business Admin with invalid user email
		pageObj.utils().logit("== Platform Functions API: Invite Business Admin with invalid user email ==");
		logger.info("== Platform Functions API: Invite Business Admin with invalid user email ==");
		// userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response inviteBusinessAdminResponse = pageObj.endpoints().inviteBusinesAdmin(invaliduserEmail, role_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(inviteBusinessAdminResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for PLATFORM FUNCTIONS API Invite Business Admin with invalid user email");
		boolean isInviteBusinessAdminInvalidEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2EmailErrorSchema, inviteBusinessAdminResponse.asString());
		Assert.assertTrue(isInviteBusinessAdminInvalidEmailSchemaValidated,
				"Platform Functions API Invite Business Admin Schema Validation failed");
		pageObj.utils().logPass("Platform Functions API Invite Business Admin with invalid user email is unsuccessful");
		logger.info("Platform Functions API Invite Business Admin with invalid user email is unsuccessful");

		// Invite Business Admin with invalid role id
		pageObj.utils().logit("== Platform Functions API: Invite Business Admin with invalid role id ==");
		logger.info("== Platform Functions API: Invite Business Admin with invalid role id ==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response inviteBusinessAdminResponse1 = pageObj.endpoints().inviteBusinesAdmin(userEmail, invalidrole_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(inviteBusinessAdminResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for PLATFORM FUNCTIONS API Invite Business Admin with invalid role id");
		boolean isInviteBusinessAdminInvalidRoleIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardInvalidRoleIdErrorSchema, inviteBusinessAdminResponse1.asString());
		Assert.assertTrue(isInviteBusinessAdminInvalidRoleIdSchemaValidated,
				"Platform Functions API Invite Business Admin Schema Validation failed");
		pageObj.utils().logPass("Platform Functions API Invite Business Admin with invalid role id is unsuccessful");
		logger.info("Platform Functions API Invite Business Admin with invalid role id is unsuccessful");

		// Invite Business Admin with invalid authorization
		pageObj.utils().logit("== Platform Functions API: Invite Business Admin with invalid authorization ==");
		logger.info("== Platform Functions API: Invite Business Admin with invalid authorization ==");
		// userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response inviteBusinessAdminResponse2 = pageObj.endpoints().inviteBusinesAdmin(userEmail, role_id,
				dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(inviteBusinessAdminResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Invite Business Admin with invalid authorization");
		boolean isInviteBusinessAdminInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, inviteBusinessAdminResponse2.asString());
		Assert.assertTrue(isInviteBusinessAdminInvalidAuthSchemaValidated,
				"Platform Functions API Invite Business Admin Schema Validation failed");
		pageObj.utils().logPass("Platform Functions API Invite Business Admin with invalid authorization is unsuccessful");
		logger.info("Platform Functions API Invite Business Admin with invalid authorization is unsuccessful");

	}

	@Test(description = "SQ-T3118 eclub And Franchise Api Validation negative flow", groups = { "regression" })
	public void eclubAndFranchiseApiValidationNegative() {
		// String invaliddataSet.get("adminAuthorization")="";
		String invaliduserEmail = "abc";
		String invalidstoreNumber = "";
		String invalidfranchisee_id = "";
		// EClub Guest Upload
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		String storeNumber = "UK9999";
		Response eClubGuestUploadResponse = pageObj.endpoints().eClubGuestUpload(userEmail, storeNumber,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(eClubGuestUploadResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API EClub Guest Upload");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API EClub Guest Upload is successful");

		// EClub Guest Upload with invalid authorization
		Response eClubGuestUploadResponse2 = pageObj.endpoints().eClubGuestUpload(userEmail, storeNumber,
				dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(eClubGuestUploadResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API EClub Guest Upload");
		boolean isEClubGuestUploadInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, eClubGuestUploadResponse2.asString());
		Assert.assertTrue(isEClubGuestUploadInvalidAuthSchemaValidated,
				"Platform Functions API EClub Guest Upload Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API EClub Guest Upload is unsuccessful");

		// EClub Guest Upload with invalid store number
		Response eClubGuestUploadResponse3 = pageObj.endpoints().eClubGuestUpload(userEmail, invalidstoreNumber,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(eClubGuestUploadResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not matched for PLATFORM FUNCTIONS API EClub Guest Upload");
		boolean isEClubGuestUploadInvalidStoreSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, eClubGuestUploadResponse3.asString());
		Assert.assertTrue(isEClubGuestUploadInvalidStoreSchemaValidated,
				"Platform Functions API EClub Guest Upload Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API EClub Guest Upload is unsuccessful");

		// Create Franchisee
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response cReateFranchiseResponse = pageObj.endpoints().cReateFranchise(userEmail,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(cReateFranchiseResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Franchisee");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Create Franchisee is successful");
		String franchisee_id = cReateFranchiseResponse.jsonPath().get("franchisee_id").toString();

		// Create Franchisee with invalid mail
		Response cReateFranchiseResponse1 = pageObj.endpoints().cReateFranchise(invaliduserEmail,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(cReateFranchiseResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for PLATFORM FUNCTIONS API Create Franchisee");
		boolean isCreateFranchiseInvalidEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiEmailObjectSchema, cReateFranchiseResponse1.asString());
		Assert.assertTrue(isCreateFranchiseInvalidEmailSchemaValidated,
				"Platform Functions API Create Franchisee Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Create Franchisee is unsuccessful");

		// Create Franchisee with invalid authorization
		Response cReateFranchiseResponse2 = pageObj.endpoints().cReateFranchise(userEmail,
				dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(cReateFranchiseResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Create Franchisee");
		boolean isCreateFranchiseInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, cReateFranchiseResponse2.asString());
		Assert.assertTrue(isCreateFranchiseInvalidAuthSchemaValidated,
				"Platform Functions API Create Franchisee Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Create Franchisee is unsuccessful");

		// Update Franchisee with invalid email
		Response uPdateFranchiseResponse = pageObj.endpoints().uPdateFranchise(invaliduserEmail, franchisee_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(uPdateFranchiseResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for PLATFORM FUNCTIONS API Update Franchisee");
		boolean isUpdateFranchiseInvalidEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiEmailObjectSchema, uPdateFranchiseResponse.asString());
		Assert.assertTrue(isUpdateFranchiseInvalidEmailSchemaValidated,
				"Platform Functions API Update Franchisee Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Update Franchisee is unsuccessful");

		// Update Franchisee with invalid franchise id
		Response uPdateFranchiseResponse1 = pageObj.endpoints().uPdateFranchise(userEmail, invalidfranchisee_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(uPdateFranchiseResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not matched for PLATFORM FUNCTIONS API Update Franchisee");
		boolean isUpdateFranchiseInvalidIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, uPdateFranchiseResponse1.asString());
		Assert.assertTrue(isUpdateFranchiseInvalidIdSchemaValidated,
				"Platform Functions API Update Franchisee Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Update Franchisee is unsuccessful");

		// Update Franchisee with invalid authorization
		Response uPdateFranchiseResponse2 = pageObj.endpoints().uPdateFranchise(userEmail, franchisee_id,
				dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(uPdateFranchiseResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Update Franchisee");
		boolean isUpdateFranchiseInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, uPdateFranchiseResponse2.asString());
		Assert.assertTrue(isUpdateFranchiseInvalidAuthSchemaValidated,
				"Platform Functions API Update Franchisee Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Update Franchisee is unsuccessful");

		// Delete Franchisee with invalid franchise id
		Response dEleteFranchiseResponse = pageObj.endpoints().dEleteFranchise(invalidfranchisee_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(dEleteFranchiseResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not matched for PLATFORM FUNCTIONS API Delete Franchisee");
		boolean isDeleteFranchiseInvalidIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, dEleteFranchiseResponse.asString());
		Assert.assertTrue(isDeleteFranchiseInvalidIdSchemaValidated,
				"Platform Functions API Delete Franchisee Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Delete Franchisee is unsuccessful");

		// Delete Franchisee with invalid authorization
		Response dEleteFranchiseResponse1 = pageObj.endpoints().dEleteFranchise(franchisee_id,
				dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(dEleteFranchiseResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Delete Franchisee");
		boolean isDeleteFranchiseInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, dEleteFranchiseResponse1.asString());
		Assert.assertTrue(isDeleteFranchiseInvalidAuthSchemaValidated,
				"Platform Functions API Delete Franchisee Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Delete Franchisee is unsuccessful");

	}

	@Test(description = "SQ-T3119 social Cause Campaigns Api Validation Negative flow", groups = { "regression" })
	public void socialCauseCampaignsApiValidationNegative() {
		// String invaliddataSet.get("adminAuthorization")="";
		String invalidsocial_cause_id = "";
		// Create Social Cause Campaigns
		String campaignName = "SocialCauseCampaign" + CreateDateTime.getTimeDateString();
		Response socialCauseCampaignResponse = pageObj.endpoints().cReateSocialcauseCampaign(campaignName,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(socialCauseCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Social Cause Campaign");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Create Social Cause Campaigns is successful");
		String social_cause_id = socialCauseCampaignResponse.jsonPath().get("social_cause_id").toString();

		// Create Social Cause Campaigns with invalid authorization
		Response socialCauseCampaignResponse1 = pageObj.endpoints().cReateSocialcauseCampaign(campaignName,
				dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(socialCauseCampaignResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Create Social Cause Campaign");
		boolean isCreateSocialCampaignInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, socialCauseCampaignResponse1.asString());
		Assert.assertTrue(isCreateSocialCampaignInvalidAuthSchemaValidated,
				"Platform Functions API Create Social Cause Campaign Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Create Social Cause Campaigns is unsuccessful");

		// Activate Social Cause Campaign
		Response activateSocialCampaignResponse = pageObj.endpoints().activateSocialCampaign(social_cause_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(activateSocialCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Activate Social Cause Campaign");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Activate Social Cause Campaign is successful");

		// Activate Social Cause Campaign with invalid social cause id
		Response activateSocialCampaignResponse1 = pageObj.endpoints().activateSocialCampaign(invalidsocial_cause_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(activateSocialCampaignResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not matched for PLATFORM FUNCTIONS API Activate Social Cause Campaign");
		boolean isActivateSocialCampaignInvalidIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, activateSocialCampaignResponse1.asString());
		Assert.assertTrue(isActivateSocialCampaignInvalidIdSchemaValidated,
				"Platform Functions API Activate Social Cause Campaign Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Activate Social Cause Campaign is unsuccessful");

		// Activate Social Cause Campaign with invalid authorization
		Response activateSocialCampaignResponse2 = pageObj.endpoints().activateSocialCampaign(social_cause_id,
				dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(activateSocialCampaignResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Activate Social Cause Campaign");
		boolean isActivateSocialCampaignInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, activateSocialCampaignResponse2.asString());
		Assert.assertTrue(isActivateSocialCampaignInvalidAuthSchemaValidated,
				"Platform Functions API Activate Social Cause Campaign Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Activate Social Cause Campaign is unsuccessful");

		// Deactivate Social Cause Campaign
		Response deactivateSocialCampaignResponse = pageObj.endpoints().deactivateSocialCampaign(social_cause_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deactivateSocialCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Deactivate Social Cause Campaign");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Deactivate Social Cause Campaign is successful");

		// Deactivate Social Cause Campaign with invalid social cause id
		Response deactivateSocialCampaignResponse1 = pageObj.endpoints()
				.deactivateSocialCampaign(invalidsocial_cause_id, dataSet.get("adminAuthorization"));
		Assert.assertEquals(deactivateSocialCampaignResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not matched for PLATFORM FUNCTIONS API Deactivate Social Cause Campaign");
		boolean isDeactivateSocialCampaignInvalidIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, deactivateSocialCampaignResponse1.asString());
		Assert.assertTrue(isDeactivateSocialCampaignInvalidIdSchemaValidated,
				"Platform Functions API Deactivate Social Cause Campaign Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Deactivate Social Cause Campaign is unsuccessful");

		// Deactivate Social Cause Campaign with invalid authorization
		Response deactivateSocialCampaignResponse2 = pageObj.endpoints().deactivateSocialCampaign(social_cause_id,
				dataSet.get("invalidadminAuthorization"));
		Assert.assertEquals(deactivateSocialCampaignResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Deactivate Social Cause Campaign");
		boolean isDeactivateSocialCampaignInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, deactivateSocialCampaignResponse2.asString());
		Assert.assertTrue(isDeactivateSocialCampaignInvalidAuthSchemaValidated,
				"Platform Functions API Deactivate Social Cause Campaign Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Deactivate Social Cause Campaign is unsuccessful");

	}

	@Test(description = "SQ-T3120 redemption Api Validation negative flow; "
			+ "SQ-T4929 Verify User Lifetime Stats negative scenarios", groups = { "regression" })
	public void redemptionApiValidationNegative() {
		// String invaliddataSet.get("adminAuthorization")="";
		String invalidredemption_code = "";
		String invalidlocation_id = "";
		String invaliduserID = "123456789";
		String invalidreward_id = "1";
		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// send reward amount to user Amount
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				dataSet.get("amount"), "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward amount to user is successful");

		// Create Redemption using "redeemable" (fetch redemption code)
		Response redeemableResponse = pageObj.endpoints().Api2RedemptionWitReedemable_id(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched for api2 create redemption using redeemable");
		pageObj.utils().logPass("Api2 Create Redemption using redeemable is successful");
		String redemption_code = redeemableResponse.jsonPath().get("redemption_tracking_code").toString();
		String location_id = redeemableResponse.jsonPath().get("location_id").toString();

		// Search Redemption Code
		Response searchRedemptionCode = pageObj.endpoints().searchRedemptionCode(dataSet.get("adminAuthorization"),
				redemption_code, location_id);
		Assert.assertEquals(searchRedemptionCode.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Platform Api Search Redemption Code");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Search Redemption Code is successful");

		// Search Redemption Code with invalid authorization
		Response searchRedemptionCode1 = pageObj.endpoints()
				.searchRedemptionCode(dataSet.get("invalidadminAuthorization"), redemption_code, location_id);
		Assert.assertEquals(searchRedemptionCode1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for Platform Api Search Redemption Code");
		boolean isSearchRedemptionCodeInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, searchRedemptionCode1.asString());
		Assert.assertTrue(isSearchRedemptionCodeInvalidAuthSchemaValidated,
				"Platform Functions API Search Redemption Code Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Search Redemption Code is unsuccessful");

		// Search Redemption Code with invalid redemption code
		Response searchRedemptionCode2 = pageObj.endpoints().searchRedemptionCode(dataSet.get("adminAuthorization"),
				invalidredemption_code, location_id);
		Assert.assertEquals(searchRedemptionCode2.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not matched for Platform Api Search Redemption Code");
		boolean isSearchRedemptionCodeInvalidRedemptionCodeSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, searchRedemptionCode2.asString());
		Assert.assertTrue(isSearchRedemptionCodeInvalidRedemptionCodeSchemaValidated,
				"Platform Functions API Search Redemption Code Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Search Redemption Code is unsuccessful");

		// Search Redemption Code with invalid location id
		Response searchRedemptionCode3 = pageObj.endpoints().searchRedemptionCode(dataSet.get("adminAuthorization"),
				redemption_code, invalidlocation_id);
		Assert.assertEquals(searchRedemptionCode3.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not matched for Platform Api Search Redemption Code");
		boolean isSearchRedemptionCodeInvalidLocationIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, searchRedemptionCode3.asString());
		Assert.assertTrue(isSearchRedemptionCodeInvalidLocationIdSchemaValidated,
				"Platform Functions API Search Redemption Code Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Search Redemption Code is unsuccessful");

		// Process Redemption
		Response processRedemptionCode = pageObj.endpoints().processRedemptionCode(dataSet.get("adminAuthorization"),
				redemption_code, location_id);
		Assert.assertEquals(processRedemptionCode.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Platform Api Process Redemption");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Process Redemption is successful");

		// Process Redemption with invalid authorization
		Response processRedemptionCode1 = pageObj.endpoints()
				.processRedemptionCode(dataSet.get("invalidadminAuthorization"), redemption_code, location_id);
		Assert.assertEquals(processRedemptionCode1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for Platform Api Process Redemption");
		boolean isProcessRedemptionCodeInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, processRedemptionCode1.asString());
		Assert.assertTrue(isProcessRedemptionCodeInvalidAuthSchemaValidated,
				"Platform Functions API Process Redemption Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Process Redemption is unsuccessful");

		// Process Redemption with invalid redemption code
		Response processRedemptionCode2 = pageObj.endpoints().processRedemptionCode(dataSet.get("adminAuthorization"),
				invalidredemption_code, location_id);
		Assert.assertEquals(processRedemptionCode2.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not matched for Platform Api Process Redemption");
		boolean isProcessRedemptionCodeInvalidCodeSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, processRedemptionCode2.asString());
		Assert.assertTrue(isProcessRedemptionCodeInvalidCodeSchemaValidated,
				"Platform Functions API Process Redemption Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Process Redemption is unsuccessful");

		// Process Redemption with invalid location id
		Response processRedemptionCode3 = pageObj.endpoints().processRedemptionCode(dataSet.get("adminAuthorization"),
				redemption_code, invalidlocation_id);
		Assert.assertEquals(processRedemptionCode3.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not matched for Platform Api Process Redemption");
		boolean isProcessRedemptionCodeInvalidLocationSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardInvalidDataErrorSchema, processRedemptionCode3.asString());
		Assert.assertTrue(isProcessRedemptionCodeInvalidLocationSchemaValidated,
				"Platform Functions API Process Redemption Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Process Redemption is unsuccessful");

		// send reward amount to user Amount
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				dataSet.get("amount"), "2367593");
		Assert.assertEquals(sendRewardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward amount to user is successful");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(offerResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		logger.info(reward_id);
		pageObj.utils().logPass("Api2 user fetch user offers is successful");

		// Force Redeem with invalid reward id
		Response forceRedeemResponse = pageObj.endpoints().forceRedeem(dataSet.get("adminAuthorization"),
				invalidreward_id, userID);
		Assert.assertEquals(forceRedeemResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for Platform Api Force Redeem");
		boolean isForceRedeemInvalidRewardIdSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, forceRedeemResponse.asString());
		Assert.assertTrue(isForceRedeemInvalidRewardIdSchemaValidated,
				"Platform Functions API Force Redeem Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Force Redeem is unsuccessful");

		// Force Redeem with invalid authorization
		Response forceRedeemResponse1 = pageObj.endpoints().forceRedeem(dataSet.get("invalidadminAuthorization"),
				reward_id, userID);
		Assert.assertEquals(forceRedeemResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for Platform Api Force Redeem");
		boolean isForceRedeemInvalidAuthSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorObjectSchema, forceRedeemResponse1.asString());
		Assert.assertTrue(isForceRedeemInvalidAuthSchemaValidated,
				"Platform Functions API Force Redeem Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Force Redeem is unsuccessful");

		// Force Redeem with invalid user id
		Response forceRedeemResponse2 = pageObj.endpoints().forceRedeem(dataSet.get("adminAuthorization"), reward_id,
				invaliduserID);
		Assert.assertEquals(forceRedeemResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for Platform Api Force Redeem");
		boolean isForceRedeemInvalidUserSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardUserNotFoundErrorSchema, forceRedeemResponse2.asString());
		Assert.assertTrue(isForceRedeemInvalidUserSchemaValidated,
				"Platform Functions API Force Redeem Schema Validation failed");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Force Redeem is unsuccessful");

		// Verify API2 User Lifetime Stats using invalid client
		Response lifetimeStatsInvalidClientResponse = pageObj.endpoints()
				.api2LifetimeStats(dataSet.get("invalidClient"), dataSet.get("secret"), token);
		Assert.assertEquals(lifetimeStatsInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 not matched for API2 Lifetime Stats");
		boolean isLifetimeStatsInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, lifetimeStatsInvalidClientResponse.asString());
		Assert.assertTrue(isLifetimeStatsInvalidClientSchemaValidated,
				"API v2 User Lifetime Stats Schema Validation failed");
		pageObj.utils().logPass("API2 user lifetime stats call is unsuccessful because of invalid client");

		// Verify API2 User Lifetime Stats using invalid secret
		Response lifetimeStatsInvalidSecretResponse = pageObj.endpoints().api2LifetimeStats(dataSet.get("client"),
				dataSet.get("invalidSecret"), token);
		Assert.assertEquals(lifetimeStatsInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 not matched for API2 Lifetime Stats");
		boolean isLifetimeStatsInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, lifetimeStatsInvalidSecretResponse.asString());
		Assert.assertTrue(isLifetimeStatsInvalidSecretSchemaValidated,
				"API v2 User Lifetime Stats Schema Validation failed");
		pageObj.utils().logPass("API2 user lifetime stats call is unsuccessful because of invalid secret");

		// Verify API2 User Lifetime Stats using invalid token
		Response lifetimeStatsInvalidTokenResponse = pageObj.endpoints().api2LifetimeStats(dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("invalidToken"));
		Assert.assertEquals(lifetimeStatsInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 not matched for API2 Lifetime Stats");
		boolean isLifetimeStatsInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, lifetimeStatsInvalidTokenResponse.asString());
		Assert.assertTrue(isLifetimeStatsInvalidTokenSchemaValidated,
				"API v2 User Lifetime Stats Schema Validation failed");
		pageObj.utils().logPass("API2 user lifetime stats call is unsuccessful because of invalid token");
	}

	@Test(description = "Verify Platform Functions API Negative Scenarios:- SQ-T5161: Create Feedback; SQ-T5162: Update Feedback; SQ-T5177: Dashboard Meta; "
			+ "SQ-T5178: Create Loyalty Checkin; SQ-T5248: Cancel Subscription; "
			+ "SQ-T4263: Verify Dashboard API for Create user Feedback; "
			+ "SQ-T4264: Verify Dashboard Update Feedback API for update user's Feedback", groups = { "api" })
	public void verifyPlatformAPIFeedbackNegative() {

		// User Sign-up
		pageObj.utils().logit("== Mobile API v2: User Sign-up ==");
		logger.info("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		pageObj.utils().logPass("API v2 User Signup call is successful");
		logger.info("API v2 User Signup call is successful");

		// Create feedback with valid data
		pageObj.utils().logit("== Platform Functions API: Create Feedback with valid data ==");
		logger.info("== Platform Functions API: Create Feedback with valid data ==");
		Response createFeedbackResponse = pageObj.endpoints().dashboardAPI2createFeedback(dataSet.get("apiKey"), userId,
				"5", "Nice place.");
		Assert.assertEquals(createFeedbackResponse.statusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Platform Functions API Create Feedback");
		boolean isCreateFeedbackSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2FeedbackSchema, createFeedbackResponse.asString());
		Assert.assertTrue(isCreateFeedbackSchemaValidated,
				"Platform Functions API Create Feedback Schema Validation failed");
		String feedbackID = createFeedbackResponse.jsonPath().get("feedback_id").toString();
		pageObj.utils().logPass("Platform Functions API Create Feedback call is successful");
		logger.info("Platform Functions API Create Feedback call is successful");

		// Create feedback with missing message and rating
		pageObj.utils().logit("== Platform Functions API: Create Feedback with missing message and rating ==");
		logger.info("== Platform Functions API: Create Feedback with missing message and rating ==");
		Response createFeedbackMissingParamResponse = pageObj.endpoints()
				.dashboardAPI2createFeedback(dataSet.get("apiKey"), userId, "", "");
		Assert.assertEquals(createFeedbackMissingParamResponse.statusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for Platform Functions API Create Feedback with missing message and rating");
		boolean isCreateFeedbackMissingParamSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, createFeedbackMissingParamResponse.asString());
		Assert.assertTrue(isCreateFeedbackMissingParamSchemaValidated,
				"Platform Functions API Create Feedback Schema Validation failed");
		String createFeedbackMissingParamMsg = createFeedbackMissingParamResponse.jsonPath().get("error").toString();
		Assert.assertEquals(createFeedbackMissingParamMsg, "Required parameter missing or the value is empty: message",
				"Message did not match");
		pageObj.utils().logPass("Platform Functions API Create Feedback call with missing message and rating is unsuccessful");
		logger.info("Platform Functions API Create Feedback call with missing message and rating is unsuccessful");

		// Create feedback with missing user id
		pageObj.utils().logit("== Platform Functions API: Create Feedback with missing user id ==");
		logger.info("== Platform Functions API: Create Feedback with missing user id ==");
		Response createFeedbackMissingUserIdResponse = pageObj.endpoints()
				.dashboardAPI2createFeedback(dataSet.get("apiKey"), "", "4", "Liked the place!");
		Assert.assertEquals(createFeedbackMissingUserIdResponse.statusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for Platform Functions API Create Feedback with missing user id");
		boolean isCreateFeedbackMissingUserIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, createFeedbackMissingUserIdResponse.asString());
		Assert.assertTrue(isCreateFeedbackMissingUserIdSchemaValidated,
				"Platform Functions API Create Feedback Schema Validation failed");
		String createFeedbackMissingUserIdMsg = createFeedbackMissingUserIdResponse.jsonPath().get("error").toString();
		Assert.assertEquals(createFeedbackMissingUserIdMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		pageObj.utils().logPass("Platform Functions API Create Feedback call with missing user id is unsuccessful");
		logger.info("Platform Functions API Create Feedback call with missing user id is unsuccessful");

		// Create feedback with missing API key
		pageObj.utils().logit("== Platform Functions API: Create Feedback with missing API key ==");
		logger.info("== Platform Functions API: Create Feedback with missing API key ==");
		Response createFeedbackMissingApiKeyResponse = pageObj.endpoints().dashboardAPI2createFeedback("", userId, "4",
				"Liked the place!");
		Assert.assertEquals(createFeedbackMissingApiKeyResponse.statusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for Platform Functions API Create Feedback with missing API key");
		String createFeedbackMissingApiKeyMsg = createFeedbackMissingApiKeyResponse.jsonPath().get("error").toString();
		boolean isCreateFeedbackMissingApiKeySchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, createFeedbackMissingApiKeyResponse.asString());
		Assert.assertTrue(isCreateFeedbackMissingApiKeySchemaValidated,
				"Platform Functions API Create Feedback Schema Validation failed");
		Assert.assertEquals(createFeedbackMissingApiKeyMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		pageObj.utils().logPass("Platform Functions API Create Feedback call with missing API key is unsuccessful");
		logger.info("Platform Functions API Create Feedback call with missing API key is unsuccessful");

		// Create feedback with invalid rating
		pageObj.utils().logit("== Platform Functions API: Create Feedback with invalid rating ==");
		logger.info("== Platform Functions API: Create Feedback with invalid rating ==");
		Response createFeedbackInvalidRatingResponse = pageObj.endpoints()
				.dashboardAPI2createFeedback(dataSet.get("apiKey"), userId, "@", "Liked the place!");
		Assert.assertEquals(createFeedbackInvalidRatingResponse.statusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for Platform Functions API Create Feedback with invalid rating");
		String createFeedbackInvalidRatingMsg = createFeedbackInvalidRatingResponse.jsonPath().get("errors.rating[0]")
				.toString();
		boolean isCreateFeedbackInvalidRatingSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardRatingErrorSchema, createFeedbackInvalidRatingResponse.asString());
		Assert.assertTrue(isCreateFeedbackInvalidRatingSchemaValidated,
				"Platform Functions API Create Feedback Schema Validation failed");
		Assert.assertEquals(createFeedbackInvalidRatingMsg, "Rating is not a number", "Message did not match");
		pageObj.utils().logPass("Platform Functions API Create Feedback call with invalid rating is unsuccessful");
		logger.info("Platform Functions API Create Feedback call with invalid rating is unsuccessful");

		// Update feedback with missing user id
		pageObj.utils().logit("== Platform Functions API: Update Feedback with missing user id ==");
		logger.info("== Platform Functions API: Update Feedback with missing user id ==");
		Response updateFeedbackMissingUserIdResponse = pageObj.endpoints()
				.dashboardAPI2updateFeedback(dataSet.get("apiKey"), "", feedbackID, "5", "Loved the place!!");
		Assert.assertEquals(updateFeedbackMissingUserIdResponse.statusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for Platform Functions API Update Feedback with missing user id");
		boolean isUpdateFeedbackMissingUserIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, updateFeedbackMissingUserIdResponse.asString());
		Assert.assertTrue(isUpdateFeedbackMissingUserIdSchemaValidated,
				"Platform Functions API Update Feedback Schema Validation failed");
		String updateFeedbackMissingUserIdMsg = updateFeedbackMissingUserIdResponse.jsonPath().get("error").toString();
		Assert.assertEquals(updateFeedbackMissingUserIdMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		pageObj.utils().logPass("Platform Functions API Update Feedback call with missing user id is unsuccessful");
		logger.info("Platform Functions API Update Feedback call with missing user id is unsuccessful");

		// Update feedback with missing API key
		pageObj.utils().logit("== Platform Functions API: Update Feedback with missing API key ==");
		logger.info("== Platform Functions API: Update Feedback with missing API key ==");
		Response updateFeedbackMissingApiKeyResponse = pageObj.endpoints().dashboardAPI2updateFeedback("", userId,
				feedbackID, "5", "Loved the place!!");
		Assert.assertEquals(updateFeedbackMissingApiKeyResponse.statusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for Platform Functions API Update Feedback with missing API key");
		boolean isUpdateFeedbackMissingApiKeySchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, updateFeedbackMissingApiKeyResponse.asString());
		Assert.assertTrue(isUpdateFeedbackMissingApiKeySchemaValidated,
				"Platform Functions API Update Feedback Schema Validation failed");
		String updateFeedbackMissingApiKeyMsg = updateFeedbackMissingApiKeyResponse.jsonPath().get("error").toString();
		Assert.assertEquals(updateFeedbackMissingApiKeyMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		pageObj.utils().logPass("Platform Functions API Update Feedback call with missing API key is unsuccessful");
		logger.info("Platform Functions API Update Feedback call with missing API key is unsuccessful");

		// Update feedback with missing message and rating
		pageObj.utils().logit("== Platform Functions API: Update Feedback with missing message and rating ==");
		logger.info("== Platform Functions API: Update Feedback with missing message and rating ==");
		Response updateFeedbackMissingParamResponse = pageObj.endpoints()
				.dashboardAPI2updateFeedback(dataSet.get("apiKey"), userId, feedbackID, "", "");
		Assert.assertEquals(updateFeedbackMissingParamResponse.statusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for Platform Functions API Update Feedback with missing message and rating");
		boolean isUpdateFeedbackMissingParamSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardMessageErrorSchema, updateFeedbackMissingParamResponse.asString());
		Assert.assertTrue(isUpdateFeedbackMissingParamSchemaValidated,
				"Platform Functions API Update Feedback Schema Validation failed");
		String updateFeedbackMissingParamMsg = updateFeedbackMissingParamResponse.jsonPath().get("errors.message[0]")
				.toString();
		Assert.assertEquals(updateFeedbackMissingParamMsg, "Message can't be blank", "Message did not match");
		pageObj.utils().logPass("Platform Functions API Update Feedback call with missing message and rating is unsuccessful");
		logger.info("Platform Functions API Update Feedback call with missing message and rating is unsuccessful");

		// Update feedback with invalid rating
		pageObj.utils().logit("== Platform Functions API: Update Feedback with invalid rating ==");
		logger.info("== Platform Functions API: Update Feedback with invalid rating ==");
		Response updateFeedbackInvalidRatingResponse = pageObj.endpoints()
				.dashboardAPI2updateFeedback(dataSet.get("apiKey"), userId, feedbackID, "@", "Loved the place!!");
		Assert.assertEquals(updateFeedbackInvalidRatingResponse.statusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for Platform Functions API Update Feedback with invalid rating");
		boolean isUpdateFeedbackInvalidRatingSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardRatingErrorSchema, updateFeedbackInvalidRatingResponse.asString());
		Assert.assertTrue(isUpdateFeedbackInvalidRatingSchemaValidated,
				"Platform Functions API Update Feedback Schema Validation failed");
		String updateFeedbackInvalidRatingMsg = updateFeedbackInvalidRatingResponse.jsonPath().get("errors.rating[0]")
				.toString();
		Assert.assertEquals(updateFeedbackInvalidRatingMsg, "Rating is not a number", "Message did not match");
		pageObj.utils().logPass("Platform Functions API Update Feedback call with invalid rating is unsuccessful");
		logger.info("Platform Functions API Update Feedback call with invalid rating is unsuccessful");

		// Update feedback with valid data
		pageObj.utils().logit("== Platform Functions API: Update Feedback with valid data ==");
		logger.info("== Platform Functions API: Update Feedback with valid data ==");
		Response updatefeedback = pageObj.endpoints().dashboardAPI2updateFeedback(dataSet.get("apiKey"), userId,
				feedbackID, "4", "feedback update");
		Assert.assertEquals(updatefeedback.statusCode(), ApiConstants.HTTP_STATUS_OK, "status code for the update feedback api did not match");
		boolean isUpdateFeedbackSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2FeedbackSchema, updatefeedback.asString());
		Assert.assertTrue(isUpdateFeedbackSchemaValidated,
				"Platform Functions API Update Feedback Schema Validation failed");
		pageObj.utils().logPass("Platform Functions API Update Feedback with valid data is successful");
		logger.info("Platform Functions API Update Feedback with valid data is successful");
		
		// Dashboard Meta with invalid authorization key
		pageObj.utils().logit("== Platform Functions API: Dashboard Meta with invalid authorization key ==");
		logger.info("== Platform Functions API: Dashboard Meta with invalid authorization key ==");
		Response dashboardMetaInvalidApiKeyResponse = pageObj.endpoints().dashboardAPI2Meta("123");
		Assert.assertEquals(dashboardMetaInvalidApiKeyResponse.statusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for Platform Functions API Dashboard Meta with invalid authorization key");
		boolean isDashboardMetaInvalidApiKeySchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, dashboardMetaInvalidApiKeyResponse.asString());
		Assert.assertTrue(isDashboardMetaInvalidApiKeySchemaValidated,
				"Platform Functions API Dashboard Meta Schema Validation failed");
		String dashboardMetaInvalidApiKeyMsg = dashboardMetaInvalidApiKeyResponse.jsonPath().get("error").toString();
		Assert.assertEquals(dashboardMetaInvalidApiKeyMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		pageObj.utils().logPass("Platform Functions API Dashboard Meta call with invalid authorization key is unsuccessful");
		logger.info("Platform Functions API Dashboard Meta call with invalid authorization key is unsuccessful");

		// Create Loyalty Checkin with missing email
		pageObj.utils().logit("== Platform Functions API: Create Loyalty Checkin with missing email ==");
		logger.info("== Platform Functions API: Create Loyalty Checkin with missing email ==");
		String startTime1 = CreateDateTime.getCurrentDate();
		Response createCheckinMissingEmailResponse = pageObj.endpoints()
				.Api2FetchCheckinDashboard(dataSet.get("apiKey"), "email", "", startTime1);
		Assert.assertEquals(createCheckinMissingEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for Platform Functions API Create Loyalty Checkin with missing email");
		boolean isCreateCheckinMissingEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, createCheckinMissingEmailResponse.asString());
		Assert.assertTrue(isCreateCheckinMissingEmailSchemaValidated,
				"Platform Functions API Create Loyalty Checkin Schema Validation failed");
		String createCheckinMissingEmailMsg = createCheckinMissingEmailResponse.jsonPath().get("error").toString();
		Assert.assertEquals(createCheckinMissingEmailMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		pageObj.utils().logPass("Platform Functions API Create Loyalty Checkin with missing email is unsuccessful");
		logger.info("Platform Functions API Create Loyalty Checkin with missing email is unsuccessful");

		// Create Loyalty Checkin with missing API key
		pageObj.utils().logit("== Platform Functions API: Create Loyalty Checkin with missing API key ==");
		logger.info("== Platform Functions API: Create Loyalty Checkin with missing API key ==");
		String startTime2 = CreateDateTime.getCurrentDate();
		Response createCheckinMissingApiKeyResponse = pageObj.endpoints().Api2FetchCheckinDashboard("", "email",
				userEmail, startTime2);
		Assert.assertEquals(createCheckinMissingApiKeyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for Platform Functions API Create Loyalty Checkin with missing API key");
		boolean isCreateCheckinMissingApiKeySchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, createCheckinMissingApiKeyResponse.asString());
		Assert.assertTrue(isCreateCheckinMissingApiKeySchemaValidated,
				"Platform Functions API Create Loyalty Checkin Schema Validation failed");
		String createCheckinMissingApiKeyMsg = createCheckinMissingApiKeyResponse.jsonPath().get("error").toString();
		Assert.assertEquals(createCheckinMissingApiKeyMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		pageObj.utils().logPass("Platform Functions API Create Loyalty Checkin with missing API key is unsuccessful");
		logger.info("Platform Functions API Create Loyalty Checkin with missing API key is unsuccessful");

		// Create Loyalty Checkin with invalid receipt date time
		pageObj.utils().logit("== Platform Functions API: Create Loyalty Checkin with invalid receipt date time ==");
		logger.info("== Platform Functions API: Create Loyalty Checkin with invalid receipt date time ==");
		String startTime3 = CreateDateTime.getDateTimeString();
		Response createCheckinInvalidTimeResponse = pageObj.endpoints().Api2FetchCheckinDashboard(dataSet.get("apiKey"),
				"email", userEmail, startTime3);
		Assert.assertEquals(createCheckinInvalidTimeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for Platform Functions API Create Loyalty Checkin with invalid receipt date time");
		boolean isCreateCheckinInvalidTimeSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, createCheckinInvalidTimeResponse.asString());
		Assert.assertTrue(isCreateCheckinInvalidTimeSchemaValidated,
				"Platform Functions API Create Loyalty Checkin Schema Validation failed");
		String createCheckinInvalidTimeMsg = createCheckinInvalidTimeResponse.jsonPath().get("error").toString();
		Assert.assertEquals(createCheckinInvalidTimeMsg, "Invalid DateTime format. Please use ISO8601",
				"Message did not match");
		pageObj.utils().logPass("Platform Functions API Create Loyalty Checkin with invalid receipt date time is unsuccessful");
		logger.info("Platform Functions API Create Loyalty Checkin with invalid receipt date time is unsuccessful");

		// Purchase Subscription
		pageObj.utils().logit("== Platform Functions API: Purchase Subscription ==");
		logger.info("== Platform Functions API: Purchase Subscription ==");
		String purchasePrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));
		String endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";

		Response purchaseSubscriptionResponse = pageObj.endpoints().dashboardSubscriptionPurchase(dataSet.get("apiKey"),
				dataSet.get("existingSubscriptionPlanID"), dataSet.get("client"), dataSet.get("secret"), purchasePrice,
				endDateTime, "false", userId);
		Assert.assertEquals(purchaseSubscriptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Platform Functions API Purchase Subscription call.");
		String subscriptionID = purchaseSubscriptionResponse.jsonPath().get("subscription_id").toString();
		pageObj.utils().logPass("Platform Functions API Purchase Subscription call is successful with Subscription ID: "
						+ subscriptionID);
		logger.info("Platform Functions API Purchase Subscription call is successful with Subscription ID: "
				+ subscriptionID);

		// Cancel Subscription (Turn off Auto Renewal) with missing subscription_id
		pageObj.utils().logit(
				"== Platform Functions API: Cancel Subscription (Turn off Auto Renewal) with missing subscription_id ==");
		logger.info(
				"== Platform Functions API: Cancel Subscription (Turn off Auto Renewal) with missing subscription_id ==");
		Response cancelSubscriptionMissingIdResponse = pageObj.endpoints()
				.dashboardSubscriptionCancel(dataSet.get("apiKey"), "", "Did not like the service", "hard_cancelled");
		Assert.assertEquals(cancelSubscriptionMissingIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_ACCEPTABLE,
				"Status code 406 did not match for Platform Functions API Cancel Subscription call with missing subscription_id.");
		boolean isCancelSubscriptionMissingIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardInvalidSubscriptionErrorSchema,
				cancelSubscriptionMissingIdResponse.asString());
		Assert.assertTrue(isCancelSubscriptionMissingIdSchemaValidated,
				"Platform Functions API Cancel Subscription Schema Validation failed");
		String cancelSubscriptionMissingIdResponseMsg = cancelSubscriptionMissingIdResponse.jsonPath()
				.get("errors.invalid_subscription[0]").toString();
		Assert.assertEquals(cancelSubscriptionMissingIdResponseMsg, "Invalid User Subscription.");
		pageObj.utils().logPass("Platform Functions API Cancel Subscription call with missing subscription_id is unsuccessful.");
		logger.info("Platform Functions API Cancel Subscription call with missing subscription_id is unsuccessful.");

		// Cancel Subscription (Turn off Auto Renewal) with invalid subscription_id
		pageObj.utils().logit(
				"== Platform Functions API: Cancel Subscription (Turn off Auto Renewal) with invalid subscription_id ==");
		logger.info(
				"== Platform Functions API: Cancel Subscription (Turn off Auto Renewal) with invalid subscription_id ==");
		Response cancelSubscriptionInvalidIdResponse = pageObj.endpoints().dashboardSubscriptionCancel(
				dataSet.get("apiKey"), "123abc", "Did not like the service", "hard_cancelled");
		Assert.assertEquals(cancelSubscriptionInvalidIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_ACCEPTABLE,
				"Status code 406 did not match for Platform Functions API Cancel Subscription call with invalid subscription_id.");
		boolean isCancelSubscriptionInvalidIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardInvalidSubscriptionErrorSchema,
				cancelSubscriptionInvalidIdResponse.asString());
		Assert.assertTrue(isCancelSubscriptionInvalidIdSchemaValidated,
				"Platform Functions API Cancel Subscription Schema Validation failed");
		String cancelSubscriptionInvalidIdResponseMsg = cancelSubscriptionInvalidIdResponse.jsonPath()
				.get("errors.invalid_subscription[0]").toString();
		Assert.assertEquals(cancelSubscriptionInvalidIdResponseMsg, "Invalid User Subscription.");
		pageObj.utils().logPass("Platform Functions API Cancel Subscription call with invalid subscription_id is unsuccessful.");
		logger.info("Platform Functions API Cancel Subscription call with invalid subscription_id is unsuccessful.");

		// Cancel Subscription (Turn off Auto Renewal) with missing cancellation_type
		pageObj.utils().logit(
				"== Platform Functions API: Cancel Subscription (Turn off Auto Renewal) with missing cancellation_type ==");
		logger.info(
				"== Platform Functions API: Cancel Subscription (Turn off Auto Renewal) with missing cancellation_type ==");
		Response cancelSubscriptionMissingTypeResponse = pageObj.endpoints()
				.dashboardSubscriptionCancel(dataSet.get("apiKey"), subscriptionID, "Did not like the service", "");
		Assert.assertEquals(cancelSubscriptionMissingTypeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for Platform Functions API Cancel Subscription call with missing cancellation_type.");
		boolean isCancelSubscriptionMissingTypeSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardCancellationTypeErrorSchema,
				cancelSubscriptionMissingTypeResponse.asString());
		Assert.assertTrue(isCancelSubscriptionMissingTypeSchemaValidated,
				"Platform Functions API Cancel Subscription Schema Validation failed");
		String cancelSubscriptionMissingTypeResponseMsg = cancelSubscriptionMissingTypeResponse.jsonPath()
				.get("errors.cancellation_type[0]").toString();
		Assert.assertEquals(cancelSubscriptionMissingTypeResponseMsg, "Cancellation type not supported.");
		pageObj.utils().logPass(
				"Platform Functions API Cancel Subscription call with missing cancellation_type is unsuccessful.");
		logger.info("Platform Functions API Cancel Subscription call with missing cancellation_type is unsuccessful.");

		// Cancel Subscription (Turn off Auto Renewal) with invalid cancellation_type
		pageObj.utils().logit(
				"== Platform Functions API: Cancel Subscription (Turn off Auto Renewal) with invalid cancellation_type ==");
		logger.info(
				"== Platform Functions API: Cancel Subscription (Turn off Auto Renewal) with invalid cancellation_type ==");
		Response cancelSubscriptionInvalidTypeResponse = pageObj.endpoints().dashboardSubscriptionCancel(
				dataSet.get("apiKey"), subscriptionID, "Did not like the service", "123abc");
		Assert.assertEquals(cancelSubscriptionInvalidTypeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for Platform Functions API Cancel Subscription call with invalid cancellation_type.");
		boolean isCancelSubscriptionInvalidTypeSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardCancellationTypeErrorSchema,
				cancelSubscriptionInvalidTypeResponse.asString());
		Assert.assertTrue(isCancelSubscriptionInvalidTypeSchemaValidated,
				"Platform Functions API Cancel Subscription Schema Validation failed");
		String cancelSubscriptionInvalidTypeResponseMsg = cancelSubscriptionInvalidTypeResponse.jsonPath()
				.get("errors.cancellation_type[0]").toString();
		Assert.assertEquals(cancelSubscriptionInvalidTypeResponseMsg, "Cancellation type not supported.");
		pageObj.utils().logPass(
				"Platform Functions API Cancel Subscription call with invalid cancellation_type is unsuccessful.");
		logger.info("Platform Functions API Cancel Subscription call with invalid cancellation_type is unsuccessful.");

		// Cancel Subscription (Turn off Auto Renewal) with missing cancellation_reason
		pageObj.utils().logit(
				"== Platform Functions API: Cancel Subscription (Turn off Auto Renewal) with missing cancellation_reason ==");
		logger.info(
				"== Platform Functions API: Cancel Subscription (Turn off Auto Renewal) with missing cancellation_reason ==");
		Response cancelSubscriptionMissingReasonResponse = pageObj.endpoints()
				.dashboardSubscriptionCancel(dataSet.get("apiKey"), subscriptionID, "", "hard_cancelled");
		Assert.assertEquals(cancelSubscriptionMissingReasonResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for Platform Functions API Cancel Subscription call with missing cancellation_reason.");
		boolean isCancelSubscriptionMissingReasonSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardCancellationReasonErrorSchema,
				cancelSubscriptionMissingReasonResponse.asString());
		Assert.assertTrue(isCancelSubscriptionMissingReasonSchemaValidated,
				"Platform Functions API Cancel Subscription Schema Validation failed");
		String cancelSubscriptionMissingReasonResponseMsg = cancelSubscriptionMissingReasonResponse.jsonPath()
				.get("errors.cancellation_reason[0]").toString();
		Assert.assertEquals(cancelSubscriptionMissingReasonResponseMsg,
				"Required parameter missing or the value is empty.");
		pageObj.utils().logPass(
				"Platform Functions API Cancel Subscription call with missing cancellation_reason is unsuccessful.");
		logger.info(
				"Platform Functions API Cancel Subscription call with missing cancellation_reason is unsuccessful.");

		// Cancel Subscription (Turn off Auto Renewal) with invalid authorisation key
		pageObj.utils().logit(
				"== Platform Functions API: Cancel Subscription (Turn off Auto Renewal) with invalid authorisation key ==");
		logger.info(
				"== Platform Functions API: Cancel Subscription (Turn off Auto Renewal) with invalid authorisation key ==");
		Response cancelSubscriptionInvalidAdminKeyResponse = pageObj.endpoints().dashboardSubscriptionCancel("123abc",
				subscriptionID, "Did not like the service", "hard_cancelled");
		Assert.assertEquals(cancelSubscriptionInvalidAdminKeyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for Platform Functions API Cancel Subscription call with invalid authorisation key.");
		String cancelSubscriptionInvalidAdminKeyResponeMsg = cancelSubscriptionInvalidAdminKeyResponse.jsonPath()
				.get("error").toString();
		boolean isCancelSubscriptionInvalidAdminKeySchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, cancelSubscriptionInvalidAdminKeyResponse.asString());
		Assert.assertTrue(isCancelSubscriptionInvalidAdminKeySchemaValidated,
				"Platform Functions API Cancel Subscription Schema Validation failed");
		Assert.assertEquals(cancelSubscriptionInvalidAdminKeyResponeMsg,
				"You need to sign in or sign up before continuing.");
		pageObj.utils().logPass(
				"Platform Functions API Cancel Subscription call with invalid authorisation key is unsuccessful.");
		logger.info("Platform Functions API Cancel Subscription call with invalid authorisation key is unsuccessful.");

		// Cancel Subscription (Turn off Auto Renewal) with valid data
		pageObj.utils().logit("== Platform Functions API: Cancel Subscription (Turn off Auto Renewal) with valid data ==");
		logger.info("== Platform Functions API: Cancel Subscription (Turn off Auto Renewal) with valid data ==");
		Response cancelSubscriptionResponse = pageObj.endpoints().dashboardSubscriptionCancel(dataSet.get("apiKey"),
				subscriptionID, "Did not like the service", "hard_cancelled");
		Assert.assertEquals(cancelSubscriptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Platform Functions API Cancel Subscription call with valid data.");
		String cancelSubscriptionResponseMsg = cancelSubscriptionResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(cancelSubscriptionResponseMsg, "Subscription auto renewal canceled.");
		pageObj.utils().logPass(
				"Platform Functions API Cancel Subscription call with valid data is successful for Subscription ID: "
						+ subscriptionID);
		logger.info(
				"Platform Functions API Cancel Subscription call with valid data is successful for Subscription ID: "
						+ subscriptionID);

		// Cancel Subscription (Turn off Auto Renewal) for already cancelled
		// subscription
		pageObj.utils().logit(
				"== Platform Functions API: Cancel Subscription (Turn off Auto Renewal) for already cancelled subscription ==");
		logger.info(
				"== Platform Functions API: Cancel Subscription (Turn off Auto Renewal) for already cancelled subscription ==");
		Response alreadyCancelledSubscriptionResponse = pageObj.endpoints().dashboardSubscriptionCancel(
				dataSet.get("apiKey"), subscriptionID, "Did not like the service", "hard_cancelled");
		Assert.assertEquals(alreadyCancelledSubscriptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for Platform Functions API Cancel Subscription call for already cancelled subscription.");
		String alreadyCancelledSubscriptionResponseMsg = alreadyCancelledSubscriptionResponse.jsonPath()
				.get("errors.already_canceled[0]").toString();
		boolean isAlreadyCancelledSubscriptionSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardAlreadyCancelledErrorSchema,
				alreadyCancelledSubscriptionResponse.asString());
		Assert.assertTrue(isAlreadyCancelledSubscriptionSchemaValidated,
				"Platform Functions API Cancel Subscription Schema Validation failed");
		Assert.assertEquals(alreadyCancelledSubscriptionResponseMsg, "Subscription is already canceled.");
		pageObj.utils().logPass(
				"Platform Functions API Cancel Subscription call for already cancelled subscription is unsuccessful for Subscription ID: "
						+ subscriptionID);
		logger.info(
				"Platform Functions API Cancel Subscription call for already cancelled subscription is unsuccessful for Subscription ID: "
						+ subscriptionID);
	}

	// Author : Amit
	@Test(description = "TD-T536 Validate the error handling for invalid requests in new support gifting api", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T536_validateEerrorHandlingForInvalidRequestsInNewSupportGiftingApi() throws InterruptedException {

		// User register/sign up using API2 Sign up
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// Hit the API endpoint with blank gift count
		Response blankGiftCountResponse = pageObj.endpoints().sendPointsToUserViaNewSupportGiftingAPI(userID,
				dataSet.get("adminAuthorization"), " ");
		Assert.assertEquals(blankGiftCountResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for dashboard api2 support gifting to user");
		String blankGiftCounterrorMsg = blankGiftCountResponse.jsonPath().get("errors.base[0]").toString();
		Assert.assertEquals(blankGiftCounterrorMsg, "Please specify any one entity for gifting.",
				"blank gifting count error message did not matched");

		// Hit the API endpoint with blank redeemable id
		Response blankRedeemableResponse = pageObj.endpoints().sendOfferToUserViaNewSupportGiftingAPI(userID,
				dataSet.get("adminAuthorization"), " ", " ");
		Assert.assertEquals(blankRedeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for dashboard api2 support gifting to user");
		String blankRedeemableerrorMsg = blankRedeemableResponse.jsonPath().get("errors.base[0]").toString();
		Assert.assertEquals(blankRedeemableerrorMsg, "Please specify any one entity for gifting.",
				"blank gifting count error message did not matched");

		// Hit the API endpoint with passing both gift count & redeemable id as blank
		Response blankRedeemableAndPointsResponse = pageObj.endpoints()
				.sendPointsRedeembalesBothToUserViaNewSupportGiftingAPI(userID, dataSet.get("adminAuthorization"), " ",
						" ");
		Assert.assertEquals(blankRedeemableAndPointsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for dashboard api2 support gifting to user");
		String blankRedeemablAndpointseerrorMsg = blankRedeemableAndPointsResponse.jsonPath().get("errors.base[0]")
				.toString();
		Assert.assertEquals(blankRedeemablAndpointseerrorMsg, "Please specify any one entity for gifting.",
				"blank gifting count error message did not matched");

		// Hit API with passing both gift count & redeemable id as some value
		/*
		 * Response redeemableAndPointsAnyResponse = pageObj.endpoints()
		 * .sendPointsRedeembalesBothToUserViaNewSupportGiftingAPI(userID,
		 * dataSet.get("adminAuthorization"), "2", "599");
		 * Assert.assertEquals(redeemableAndPointsAnyResponse.getStatusCode(), 422,
		 * "Status code 422 did not matched for dashboard api2 support gifting to user"
		 * ); String redeemablAndpointsAnyeerrorMsg =
		 * blankGiftCountResponse.jsonPath().get("errors.base[0]").toString();
		 * Assert.assertEquals(redeemablAndpointsAnyeerrorMsg,
		 * "Please specify any one entity for gifting.",
		 * "blank gifting count error message did not matched");
		 */

		// Hit API with passing gift count value as alphanumeric/boolean
		Response alphaGiftCountResponse = pageObj.endpoints().sendPointsToUserViaNewSupportGiftingAPI(userID,
				dataSet.get("adminAuthorization"), "abcd");
		Assert.assertEquals(alphaGiftCountResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for dashboard api2 support gifting to user");
		String alphakGiftCounterrorMsg = alphaGiftCountResponse.jsonPath().get("errors.gift_count[0]").toString();
		Assert.assertEquals(alphakGiftCounterrorMsg, "Gift is not a number",
				"alpha gifting count error message did not matched");

		// Hit the API endpoint with passing redeemable id value as invalid
		Response invalidRedeemableResponse = pageObj.endpoints().sendOfferToUserViaNewSupportGiftingAPI(userID,
				dataSet.get("adminAuthorization"), "123456", " ");
		Assert.assertEquals(invalidRedeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 422 did not matched for dashboard api2 support gifting to user");
		String invalidRedeemableerrorMsg = invalidRedeemableResponse.jsonPath().get("errors.redeemable_not_found")
				.toString();
		Assert.assertEquals(invalidRedeemableerrorMsg, "Redeemable Not Found",
				"invalid redeemable gifting count error message did not matched");
		// Hit the API endpoint with passing expired redeemable's id
		// to be added

		// Hit the API endpoint with passing deactivated redeemable's id
		Response deactivatedRedeemableResponse = pageObj.endpoints().sendOfferToUserViaNewSupportGiftingAPI(userID,
				dataSet.get("adminAuthorization"), dataSet.get("dectivatedRedeemable_id"), " ");
		Assert.assertEquals(deactivatedRedeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 422 did not matched for dashboard api2 support gifting to user");
		String deactivatedRedeemableerrorMsg = deactivatedRedeemableResponse.jsonPath().get("errors.invalid_redeemable")
				.toString();
		Assert.assertEquals(deactivatedRedeemableerrorMsg, "Sorry, this redeemable is either deactivated or expired",
				"deactivated redeemable gifting count error message did not matched");

		// Hit the API endpoint with invalid user id
		Response invalidUserResponse = pageObj.endpoints().sendPointsToUserViaNewSupportGiftingAPI("123456",
				dataSet.get("adminAuthorization"), "10");
		Assert.assertEquals(invalidUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 422 did not matched for dashboard api2 support gifting to user");
		String invalidUsererrorMsg = invalidUserResponse.jsonPath().get("errors.user_not_found").toString();
		Assert.assertEquals(invalidUsererrorMsg, "Cannot find corresponding user with ID: 123456",
				"invalid user error message did not matched");

		// Hit the API endpoint with passing blank user_id
		Response blankUserResponse = pageObj.endpoints().sendPointsToUserViaNewSupportGiftingAPI(" ",
				dataSet.get("adminAuthorization"), "10");
		Assert.assertEquals(blankUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 422 did not matched for dashboard api2 support gifting to user");
		String blankUsererrorMsg = blankUserResponse.jsonPath().get("error").toString();
		Assert.assertEquals(blankUsererrorMsg, "Required parameter missing or the value is empty: user_id",
				"invalid user error message did not matched");

		// Hit the API endpoint with passing blank string in location_id
		Response blankLocationResponse = pageObj.endpoints()
				.sendPointsWithLovationToUserViaNewSupportGiftingAPIInvalidJson(userID,
						dataSet.get("adminAuthorization"), "10", " ");
		Assert.assertEquals(blankLocationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 422 did not matched for dashboard api2 support gifting to user");
		String blankLocationerrorMsg = blankLocationResponse.jsonPath().get("errors.not_found").toString();
		Assert.assertEquals(blankLocationerrorMsg, "Location Not Found",
				"blank location error message did not matched");

		// Hit the API endpoint with passing invalid location_id
		Response invalidLocationResponse = pageObj.endpoints()
				.sendPointsWithLovationToUserViaNewSupportGiftingAPIInvalidJson(userID,
						dataSet.get("adminAuthorization"), "10", "1010101");
		Assert.assertEquals(invalidLocationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 422 did not matched for dashboard api2 support gifting to user");
		String invalidLocationerrorMsg = invalidLocationResponse.jsonPath().get("errors.not_found").toString();
		Assert.assertEquals(invalidLocationerrorMsg, "Location Not Found",
				"blank location error message did not matched");

		// Hit the API endpoint without passing location_id
		Response validLocationResponse = pageObj.endpoints()
				.sendPointsWithLovationToUserViaNewSupportGiftingAPIInvalidJson(userID,
						dataSet.get("adminAuthorization"), "10", dataSet.get("location_id"));
		Assert.assertEquals(validLocationResponse.getStatusCode(), 202,
				"Status code 422 did not matched for dashboard api2 support gifting to user");

	}

	@AfterMethod(alwaysRun = true)
	public void afterMethod() {

	}
}
