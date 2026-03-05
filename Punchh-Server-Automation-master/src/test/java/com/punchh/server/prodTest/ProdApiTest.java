package com.punchh.server.prodTest;

import org.testng.annotations.Test;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.Listeners;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.CreateDateTime;

import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class ProdApiTest {
	static Logger logger = LogManager.getLogger(ProdApiTest.class);
	public WebDriver driver;
	ApiUtils apiUtils;
	private Utilities utils;
	String userEmail;
	Properties prop;
	String punchKey, amount;
	PageObj pageObj;
	String sTCName;
	String run = "api";
	// String adminAuthorization="xL2KgxdnUpJtffcJ9EBe"; //prod winghouse 548
	private String env;
	private static Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void beforeMethod(Method method) {
		apiUtils = new ApiUtils();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		apiUtils = new ApiUtils();
		utils = new Utilities();
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
	}

	// ==================================================================//
	// Platform Api
	// ==================================================================//

	@Test(description = "usersApiValidation", groups = "api", priority = 0)
	public void usersApiValidation() { // Platform Apis

		// Creating user with POS user signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response response = pageObj.prodEndpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(response.getStatusCode(), 200);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());
		String userID = response.jsonPath().get("id").toString();

		// Ban a User
		Response banUserresponse = pageObj.prodEndpoints().banUser(userID, dataSet.get("adminAuthorization"));
		Assert.assertEquals(banUserresponse.getStatusCode(), 202,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Ban a User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Ban a User is successful");

		// UnBan a User
		Response unBanUserresponse = pageObj.prodEndpoints().unBanUser(userID, dataSet.get("adminAuthorization"));
		Assert.assertEquals(unBanUserresponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API UnBan a User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API UnBan a User is successful");

		// Send Message to User (Parametrize for other envs)
		Response sendRewardResponse = pageObj.prodEndpoints().sendMessageToUser(userID,
				dataSet.get("adminAuthorization"), dataSet.get("amount"), dataSet.get("reedemable_id"), "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), 201,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API send message to user");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Send Message to User is successful");

		// Support Gifting to a User
		Response supportGiftingResponse = pageObj.prodEndpoints().supportGiftingToUser(userID,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(supportGiftingResponse.getStatusCode(), 201,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Support Gifting to a User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Support Gifting to a User is successful");

		// Deactivate a User
		Response deactivateUserresponse = pageObj.prodEndpoints().deactivateUser(userID,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deactivateUserresponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Deactivate a User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Deactivate a User is successful");

		// Reactivate a User
		Response reactivateUserresponse = pageObj.prodEndpoints().reactivateUser(userID,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(reactivateUserresponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Reactivate a User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Reactivate a User is successful");

		// Get Location List
		Response getLocationresponse = pageObj.prodEndpoints().getLocationList(dataSet.get("adminAuthorization"));
		Assert.assertEquals(getLocationresponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Location List");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Get Location List is successful");
		String locationid = getLocationresponse.jsonPath().get("[0].location_id").toString();

		// Update a User
		Response updateUserResponse = pageObj.prodEndpoints().updateUser(userID, userEmail, locationid,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(updateUserResponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Update a User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Update a User is successful");

		// Get User Export
		Response userExportResponse = pageObj.prodEndpoints().userExport(userID, dataSet.get("adminAuthorization"));
		Assert.assertEquals(userExportResponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get User Export");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Get User Export is successful");

		// Get Extended User History
		Response extendedUserHistoryResponse = pageObj.prodEndpoints().extendedUserHistory(userID,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(extendedUserHistoryResponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Extended User History");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Get Extended User History is successful");

		// Fetch User Favourite Locations
		Response fetchUserLocationsResponse = pageObj.prodEndpoints().fetchUserLocation(userID,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(fetchUserLocationsResponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Fetch User Favourite Locations");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Fetch User Favourite Locations is successful");
		String favourite_location_id = fetchUserLocationsResponse.jsonPath().get("[0].user_favourite_location_id")
				.toString();

		// Delete User Favourite Location
		Response deleteUserLocationsResponse = pageObj.prodEndpoints().deleteUserLocation(userID, favourite_location_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deleteUserLocationsResponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Delete User Favourite Location");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Delete User Favourite Location is successful");

		// Deactivate a User
		Response deactivateUserresponse1 = pageObj.prodEndpoints().deactivateUser(userID,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deactivateUserresponse1.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Deactivate a User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Deactivate a User is successful");

		// Delete a User
		Response deleteUserResponse = pageObj.prodEndpoints().deleteUser(userID, dataSet.get("adminAuthorization"));
		Assert.assertEquals(deleteUserResponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Delete a User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Delete a User is successful");
	}

	@Test(description = "locationsApiValidation", groups = "api", priority = 1)
	public void locationsApiValidation() {

		String store_number = CreateDateTime.getTimeDateString();
		String location_name = "Test Location" + store_number;
		String locationGroupName = "Location Group" + store_number;
		// Create Location Api
		Response createLocationresponse = pageObj.prodEndpoints().createLocation(location_name, store_number,
				dataSet.get("adminAuthorization"));
		String location_id = createLocationresponse.jsonPath().get("location_id").toString();
		String storeNumber = createLocationresponse.jsonPath().get("store_number").toString();
		Assert.assertEquals(createLocationresponse.getStatusCode(), 201,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Create Location");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Create Location is successful");

		// Get Location List
		Response getLocationresponse = pageObj.prodEndpoints().getLocationList(dataSet.get("adminAuthorization"));
		Assert.assertEquals(getLocationresponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Location List");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Get Location List is successful");

		// Update Location
		Response updateLocationresponse = pageObj.prodEndpoints().updateLocation(location_id, storeNumber,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(updateLocationresponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Update Location");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Update Location is successful");

		// Get Location Details
		Response getLocationDetailsresponse = pageObj.prodEndpoints().getLocationDetails(location_id, storeNumber,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(getLocationDetailsresponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Location Details");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Get Location Details is successful");

		// Get Location Group List
		Response getLocationGroupListresponse = pageObj.prodEndpoints()
				.getLocationGroupList(dataSet.get("adminAuthorization"));
		Assert.assertEquals(getLocationGroupListresponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Location Group List");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Get Location Group List is successful");
		String location_group_id = getLocationGroupListresponse.jsonPath().get("[0].location_group_id").toString();

		// Get Location Group Details
		Response getLocationGroupDetailsresponse = pageObj.prodEndpoints().getLocationGroupDetails(location_group_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(getLocationGroupDetailsresponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Location Group Details");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Get Location Group Details is successful");

		// Add Location to Location Group
		Response addLocationtoGroupresponse = pageObj.prodEndpoints()
				.addLocationtoGroup(dataSet.get("adminAuthorization"), location_group_id, store_number, location_id);
		Assert.assertEquals(addLocationtoGroupresponse.getStatusCode(), 201,
				"Status code 202 did not matched for PLATFORM FUNCTIONS API Add Location to Location Group");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Add Location to Location Group is successful");

		// Create Location Group
		Response createLocationGroupresponse = pageObj.prodEndpoints().createLocationgroup(locationGroupName,
				storeNumber, location_id, dataSet.get("adminAuthorization"));
		Assert.assertEquals(createLocationGroupresponse.getStatusCode(), 201,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Location Group");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Create Location Group is successful");
		String locationGroup_id = createLocationGroupresponse.jsonPath().get("location_group_id").toString();

		// Update Location Group
		Response updateLocationGroupresponse = pageObj.prodEndpoints().updateLocationgroup(locationGroupName,
				locationGroup_id, dataSet.get("adminAuthorization"));
		Assert.assertEquals(updateLocationGroupresponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Update Location Group");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Update Location Group is successful");

		// Delete Location Group
		Response deleteLocationGroupresponse = pageObj.prodEndpoints().deleteLocationgroup(locationGroup_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deleteLocationGroupresponse.getStatusCode(), 204,
				"Status code 204 did not matched for PLATFORM FUNCTIONS API Delete Location Group");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Delete Location Group is successful");

		// Delete Location from Location Group
		Response deleteLocationfromGroupresponse = pageObj.prodEndpoints().deleteLocationfromGroup(
				dataSet.get("adminAuthorization"), location_group_id, store_number, location_id);
		Assert.assertEquals(deleteLocationfromGroupresponse.getStatusCode(), 204,
				"Status code 204 did not matched for PLATFORM FUNCTIONS API Delete Location from Location Group");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Delete Location from Location Group is successful");

		// Delete location
		Response deleteLocationresponse = pageObj.prodEndpoints().deleteLocation(location_id, storeNumber,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deleteLocationresponse.getStatusCode(), 204,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Delete location");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Delete locationis successful");

	}

	@Test(description = "Custom Segment Creation Updation Deletion AddUser Api", groups = "api", priority = 2)
	public void verifyCustomSegmentCreationUpdationDeletionAddUserApi() {
		// create Custom segments
		String segName = "CustomSeg" + CreateDateTime.getTimeDateString();
		Response createSegmentResponse = pageObj.prodEndpoints().createCustomSegment(segName,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(201, createSegmentResponse.getStatusCode(), "Status code 201 did not matched");
		int customSegmentId = createSegmentResponse.jsonPath().get("custom_segment_id");

		// get Custom segments details
		Response getSegmentDetailResponse = pageObj.prodEndpoints()
				.getCustomSegmentDetails(dataSet.get("adminAuthorization"), customSegmentId);
		Assert.assertEquals(200, getSegmentDetailResponse.getStatusCode(), "Status code 200 did not matched");

		String cSegName = getSegmentDetailResponse.jsonPath().get("[0].name").toString();
		String cSegID = getSegmentDetailResponse.jsonPath().get("[0].custom_segment_id").toString();
		Assert.assertEquals(segName, cSegName, "Custom segment name did not matched in segment details api");
		Assert.assertEquals(cSegID, Integer.toString(customSegmentId),
				"Custom segment id did not matched in segment details api");

		logger.info("Verified custom segment details");
		TestListeners.extentTest.get().pass("Verified custom segment details");

		// get list of all Custom segments
		Response getAllSegmentResponse = pageObj.prodEndpoints()
				.listAllCustomSegments(dataSet.get("adminAuthorization"));
		Assert.assertEquals(200, getAllSegmentResponse.getStatusCode(), "Status code 200 did not matched");

		getAllSegmentResponse.prettyPrint();
		Assert.assertTrue(getAllSegmentResponse.jsonPath().get("name").toString().contains(segName),
				"Failed to verify new segment");
		logger.info("Verified new custom segment");
		TestListeners.extentTest.get().pass("Verified new custom segment");

		// update Custom segments
		String updatedSegmentName = "updated" + segName;
		Response updateSegmentResponse = pageObj.prodEndpoints().updateCustomSegment(updatedSegmentName,
				dataSet.get("adminAuthorization"), customSegmentId);
		Assert.assertEquals(200, updateSegmentResponse.getStatusCode(), "Status code 200 did not matched");

		// list all Custom segments
		getAllSegmentResponse = pageObj.prodEndpoints().listAllCustomSegments(dataSet.get("adminAuthorization"));
		Assert.assertTrue(getAllSegmentResponse.jsonPath().get("name").toString().contains(updatedSegmentName),
				"Failed to verify updated custom segment");
		logger.info("Verified updated custom segment");
		TestListeners.extentTest.get().pass("Verified updated custom segment");

		// Creating user with POS user signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response response = pageObj.prodEndpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(response.getStatusCode(), 200);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());
		String userID = response.jsonPath().get("id").toString();

		// add user to custom segments
		Response addUserSegmentResponse = pageObj.prodEndpoints().addUserToCustomSegment(customSegmentId, userEmail,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(addUserSegmentResponse.getStatusCode(), 201,
				"User exists in custom segment, Status 200 not matched");

		int userId = addUserSegmentResponse.jsonPath().get("user_id");
		System.out.println(addUserSegmentResponse.jsonPath().get("email").toString());
		Assert.assertTrue(addUserSegmentResponse.jsonPath().get("email").toString().contains(userEmail.toLowerCase()),
				"Failed to verify added user to custom segment");
		logger.info("Added user to custom segment");
		TestListeners.extentTest.get().pass("Added user to custom segment");

		// Searching user in custom segments
		Response userExistsInSegmentResponse = pageObj.prodEndpoints().searchUserExistsInSegment(customSegmentId,
				userEmail, userId, dataSet.get("adminAuthorization"));
		Assert.assertEquals(userExistsInSegmentResponse.getStatusCode(), 200,
				"User exists in custom segment, Status 200 not matched");
		TestListeners.extentTest.get().pass("Verified user exists in custom segment");

		// delete user from custom segments
		Response deleteSegmenUserResponse = pageObj.prodEndpoints()
				.deletingUserFromCustomSegment(dataSet.get("adminAuthorization"), customSegmentId, userEmail, userId);
		Assert.assertEquals(deleteSegmenUserResponse.getStatusCode(), 204);
		TestListeners.extentTest.get().pass("Deleted user from custom segment");

		// Verfiying deleted user no more exists in segment
		userExistsInSegmentResponse = pageObj.prodEndpoints().searchUserExistsInSegment(customSegmentId, userEmail,
				userId, dataSet.get("adminAuthorization"));
		Assert.assertEquals(userExistsInSegmentResponse.getStatusCode(), 404,
				"User exists in custom segment, Status 400 not matched");
		String valMsg = userExistsInSegmentResponse.jsonPath().get("errors.not_found").toString();
		Assert.assertEquals("Member not found.", valMsg,
				"Member not found response message did not matched for deleted guest in segment");
		logger.info("Verified that user is deleted from custom segment");
		TestListeners.extentTest.get().pass("Verified that user is deleted from custom segment");

		// delete custom segments
		Response deleteSegmentResponse = pageObj.prodEndpoints()
				.deletingCustomSegment(dataSet.get("adminAuthorization"), customSegmentId);
		Assert.assertEquals(202, deleteSegmentResponse.getStatusCode(),
				"Status code 202 delete custom segments api did not matched");
		TestListeners.extentTest.get().pass("Verified deleted custom segment");

		// Deactivate a User
		Response deactivateUserresponse1 = pageObj.prodEndpoints().deactivateUser(userID,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deactivateUserresponse1.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Deactivate a User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Deactivate a User is successful");

		// Delete a User
		Response deleteUserResponse = pageObj.prodEndpoints().deleteUser(userID, dataSet.get("adminAuthorization"));
		Assert.assertEquals(deleteUserResponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Delete a User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Delete a User is successful");
	}

	@Test(groups = { "regression" })
	public void businessMigrationUserApiValidation() {

		// Create Business Migration User
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserResponse = pageObj.prodEndpoints().createBusinessMigrationUse(userEmail,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(createMigrationUserResponse.getStatusCode(), 201,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Migration User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Ban a User is successful");
		String migration_user_id = createMigrationUserResponse.jsonPath().get("migration_user_id").toString();

		// Update Business Migration User
		Response updateMigrationUserResponse = pageObj.prodEndpoints().updateBusinessMigrationUse(userEmail,
				dataSet.get("adminAuthorization"), migration_user_id);
		Assert.assertEquals(updateMigrationUserResponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Update Business Migration User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Update Business Migration User is successful");

		// Delete Business Migration User
		Response deleteMigrationUserResponse = pageObj.prodEndpoints()
				.deleteBusinessMigrationUse(dataSet.get("adminAuthorization"), migration_user_id);
		Assert.assertEquals(deleteMigrationUserResponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Delete Business Migration User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Delete Business Migration User is successful");

	}

	@Test(groups = { "regression" })
	public void businessAdminApiValidation() {

		// Get Admin Roles List
		Response getAdminRoleListResponse = pageObj.prodEndpoints()
				.getAdminRolesList(dataSet.get("adminAuthorization"));
		Assert.assertEquals(getAdminRoleListResponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Admin Roles List");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Get Get Admin Roles List is successful");
		String role_id = getAdminRoleListResponse.jsonPath().get("[0].role_id").toString();

		// Create Business Admin
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createBusinessAdminResponse = pageObj.prodEndpoints().createBusinesAdmin(userEmail, role_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(createBusinessAdminResponse.getStatusCode(), 201,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Admin");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Create Business Admin is successful");
		String business_admin_id = createBusinessAdminResponse.jsonPath().get("business_admin_id").toString();

		// Update Business Admin
		Response updateBusinessAdminResponse = pageObj.prodEndpoints().updateBusinesAdmin(business_admin_id, userEmail,
				role_id, dataSet.get("adminAuthorization"));
		Assert.assertEquals(updateBusinessAdminResponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Update Business Admin");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Update Business Admin is successful");

		// Show Business Admin
		Response showBusinessAdminResponse = pageObj.prodEndpoints().showBusinesAdmin(business_admin_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(showBusinessAdminResponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Show Business Admin");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Show Business Admin is successful");

		// Delete Business Admin
		Response deleteBusinessAdminResponse = pageObj.prodEndpoints().deleteBusinesAdmin(business_admin_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deleteBusinessAdminResponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Delete Business Admin");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Delete Business Admin is successful");

		// Invite Business Admin
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response inviteBusinessAdminResponse = pageObj.prodEndpoints().inviteBusinesAdmin(userEmail, role_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(inviteBusinessAdminResponse.getStatusCode(), 201,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Invite Business Admin");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Invite Business Admin is successful");
		String business_admin_id1 = inviteBusinessAdminResponse.jsonPath().get("business_admin_id").toString();

		// Delete Business Admin
		Response deleteBusinessAdminResponse1 = pageObj.prodEndpoints().deleteBusinesAdmin(business_admin_id1,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deleteBusinessAdminResponse1.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Delete Business Admin");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Delete Business Admin is successful");

	}

	@Test(groups = { "regression" })
	public void eclubAndFranchiseApiValidation() {

		// EClub Guest Upload
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		String storeNumber = "UK9998";
		Response eClubGuestUploadResponse = pageObj.prodEndpoints().eClubGuestUpload(userEmail, storeNumber,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(eClubGuestUploadResponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API EClub Guest Upload");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API EClub Guest Upload is successful");

		// Create Franchisee
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response cReateFranchiseResponse = pageObj.prodEndpoints().cReateFranchise(userEmail,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(cReateFranchiseResponse.getStatusCode(), 201,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Franchisee");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Create Franchisee is successful");
		String franchisee_id = cReateFranchiseResponse.jsonPath().get("franchisee_id").toString();

		// Update Franchisee
		Response uPdateFranchiseResponse = pageObj.prodEndpoints().uPdateFranchise(userEmail, franchisee_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(uPdateFranchiseResponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Update Franchisee");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Create Update Franchisee is successful");

		// Delete Franchisee
		Response dEleteFranchiseResponse = pageObj.prodEndpoints().dEleteFranchise(franchisee_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(dEleteFranchiseResponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Delete Franchisee");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Create Delete Franchisee is successful");

	}

	@Test(groups = { "regression" })
	public void socialCauseCampaignsApiValidation() {

		// Create Social Cause Campaigns
		String campaignName = "SocialCauseCampaign" + CreateDateTime.getTimeDateString();
		Response socialCauseCampaignResponse = pageObj.prodEndpoints().cReateSocialcauseCampaign(campaignName,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(socialCauseCampaignResponse.getStatusCode(), 201,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Social Cause Campaign");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Create Social Cause Campaigns is successful");
		String social_cause_id = socialCauseCampaignResponse.jsonPath().get("social_cause_id").toString();

		// Activate Social Cause Campaign
		Response activateSocialCampaignResponse = pageObj.prodEndpoints().activateSocialCampaign(social_cause_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(activateSocialCampaignResponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Activate Social Cause Campaign");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Activate Social Cause Campaign is successful");

		// Deactivate Social Cause Campaign
		Response deactivateSocialCampaignResponse = pageObj.prodEndpoints().deactivateSocialCampaign(social_cause_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deactivateSocialCampaignResponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Deactivate Social Cause Campaign");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Deactivate Social Cause Campaign is successful");

	}

	// ==================================================================//
	// POS API
	// ==================================================================//

	@Test(description = "POS API validation for Singup, Checkin, Redemption, void redemption")
	public void verifyPosAPiSingUp_Checkin_Redemption_VoidRedemption() {
		// user creation using auth signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response response = pageObj.prodEndpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		// apiUtils.verifyResponse(response, "POS user signup");
		Assert.assertEquals(response.getStatusCode(), 200);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());
		String userID = response.jsonPath().get("id").toString();

		// POS Checkin
		Response response2 = pageObj.prodEndpoints().posCheckin(userEmail, dataSet.get("locationKey"));
		apiUtils.verifyResponse(response2, "POS checkin");
		Assert.assertEquals(response2.getStatusCode(), 200);
		Assert.assertEquals(response2.jsonPath().get("email").toString(), userEmail.toLowerCase());

		// get user balance
		Response balanceResponse = pageObj.prodEndpoints().posUserLookupFetchBalance(userEmail,
				dataSet.get("locationKey"));
		Assert.assertEquals(balanceResponse.getStatusCode(), 200, "Error in getting user balance");
		String bankedRewardBefore = balanceResponse.jsonPath().get("balance.banked_rewards");

		// Deactivate a User
		Response deactivateUserresponse1 = pageObj.prodEndpoints().deactivateUser(userID,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deactivateUserresponse1.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Deactivate a User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Deactivate a User is successful");

		// Delete a User
		Response deleteUserResponse = pageObj.prodEndpoints().deleteUser(userID, dataSet.get("adminAuthorization"));
		Assert.assertEquals(deleteUserResponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Delete a User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Delete a User is successful");

	}

	// ==================================================================//
	// Auth online ordering API
	// ==================================================================//

	@Test(description = "Auth Signup login update user info and verify user info")
	public void verifySSOSignUpUserdetails() {

		// user creation using auth signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.prodEndpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		apiUtils.verifyCreateResponse(signUpResponse, "Auth API user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), 201,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		String fName = signUpResponse.jsonPath().get("first_name");
		String lName = signUpResponse.jsonPath().get("last_name");
		String userID = signUpResponse.jsonPath().get("id").toString();
		// login via auth API
		Response loginResponse = pageObj.prodEndpoints().authApiUserLogin(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		apiUtils.verifyCreateResponse(loginResponse, "Auth API user login");
		Assert.assertEquals(loginResponse.getStatusCode(), 201, "Failed - Auth API user login");
		String updateEmail = "updated" + userEmail;
		String updateFName = "updated" + fName;
		String updateLName = "updated" + lName;
		// Update userInfo
		Response updateUserResponse = pageObj.prodEndpoints().authApiUpdateUserInfo(dataSet.get("client"),
				dataSet.get("secret"), authToken, updateEmail, updateFName, updateLName);
		apiUtils.verifyResponse(updateUserResponse, "Auth API user login");
		Assert.assertEquals(updateUserResponse.getStatusCode(), 200, "Failed - to update user info");
		// Verify updated user info
		Response fetchUserResponse = pageObj.prodEndpoints().authApiFetchUserInfo(dataSet.get("client"),
				dataSet.get("secret"), authToken);
		Assert.assertEquals(fetchUserResponse.jsonPath().get("email").toString().toLowerCase(),
				updateEmail.toLowerCase(), "Failed to verify updated email");
		Assert.assertEquals(fetchUserResponse.jsonPath().get("first_name").toString().toLowerCase(),
				updateFName.toLowerCase(), "Failed to verify updated firstname");
		Assert.assertEquals(fetchUserResponse.jsonPath().get("last_name").toString().toLowerCase(),
				updateLName.toLowerCase(), "Failed to verify updated lastname");

		// Deactivate a User
		Response deactivateUserresponse1 = pageObj.prodEndpoints().deactivateUser(userID,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deactivateUserresponse1.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Deactivate a User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Deactivate a User is successful");

		// Delete a User
		Response deleteUserResponse = pageObj.prodEndpoints().deleteUser(userID, dataSet.get("adminAuthorization"));
		Assert.assertEquals(deleteUserResponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Delete a User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Delete a User is successful");
	}

	@Test(description = "SSO Checkin, get account history, account balance, user balance and forgot password")
	public void verifySSOSignUpUserCheckinAndBalance() {

		// user creation using auth signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.prodEndpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		apiUtils.verifyCreateResponse(signUpResponse, "Auth API user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), 201,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		String userID = signUpResponse.jsonPath().get("id").toString();
		System.out.println(signUpResponse.prettyPrint());
		// Checkin via auth API
		String amount = "110.0";
		Response checkinResponse = pageObj.prodEndpoints().onlineOrderCheckin(authToken, amount, dataSet.get("client"),
				dataSet.get("secret"));
		apiUtils.verifyResponse(checkinResponse, "Online order checkin");
		Assert.assertEquals(checkinResponse.getStatusCode(), 200);
		// get account history and verify checkin
		Response accountHistoryResponse = pageObj.prodEndpoints().authApiAccountHistory(authToken,
				dataSet.get("client"), dataSet.get("secret"));
		apiUtils.verifyCreateResponse(signUpResponse, "Auth API account history");
		Assert.assertEquals(accountHistoryResponse.getStatusCode(), 200, "Failed Auth API Account history response");
		// Fetch User Balance
		Response userBalanceResponse = pageObj.prodEndpoints().authApiFetchUserBalance(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		apiUtils.verifyResponse(userBalanceResponse, "Auth API user balance");
		Assert.assertEquals(userBalanceResponse.getStatusCode(), 200, "Failed Auth API Account balance response");
		// forgot password
		Response forgotPasswordResponse = pageObj.prodEndpoints().authApiForgotPassword(userEmail,
				dataSet.get("client"), dataSet.get("secret"));
		apiUtils.verifyResponse(forgotPasswordResponse, "Auth API forgot password");
		Assert.assertEquals(forgotPasswordResponse.getStatusCode(), 200, "Failed Auth API forgot response response");

		// Deactivate a User
		Response deactivateUserresponse1 = pageObj.prodEndpoints().deactivateUser(userID,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deactivateUserresponse1.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Deactivate a User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Deactivate a User is successful");

		// Delete a User
		Response deleteUserResponse = pageObj.prodEndpoints().deleteUser(userID, dataSet.get("adminAuthorization"));
		Assert.assertEquals(deleteUserResponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Delete a User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Delete a User is successful");

	}

	// ==================================================================//
	// Mobile API
	// ==================================================================//

	@Test(description = "SQ-T2378 Verify mobile apiv1 ", groups = "api")
	public void verifyMobileApiV1() {
		// user signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.prodEndpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret")); // working without cloudflare token
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api1 user signup is successful");
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String Fname = signUpResponse.jsonPath().get("first_name").toString();
		String Lname = signUpResponse.jsonPath().get("last_name").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// user login
		Response loginResponse = pageObj.prodEndpoints().Api1UserLogin(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		apiUtils.verifyResponse(loginResponse, "API 1 user login");
		Assert.assertEquals(loginResponse.getStatusCode(), 200);
		Assert.assertEquals(loginResponse.jsonPath().get("email").toString(), userEmail.toLowerCase());

		String userNewEmail = pageObj.iframeSingUpPage().generateEmail();
		// Update user profile using apiV1 {fname, Lname and pwd update}
		Response updateGuestResponse = pageObj.prodEndpoints().Api1MobileUpdateGuestDetails("New" + Fname,
				"New" + Lname, dataSet.get("Npwd"), dataSet.get("client"), dataSet.get("secret"), token, userNewEmail);
		Assert.assertEquals(updateGuestResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 signup");
		String UpdatedFname = updateGuestResponse.jsonPath().get("first_name").toString();
		String UpdatedLname = updateGuestResponse.jsonPath().get("last_name").toString();
		Assert.assertNotEquals(Fname, UpdatedFname);
		Assert.assertNotEquals(Lname, UpdatedLname);
		TestListeners.extentTest.get().pass("Api1 guest details update is successful");

		// Api v1 forgot password
		Response forgotPasswordResponse = pageObj.prodEndpoints().Api1MobileForgotPassword(userEmail,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(forgotPasswordResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 signup");
		String forgotPwdResponse = forgotPasswordResponse.jsonPath().get("[0]").toString();
		Assert.assertNotEquals(forgotPwdResponse,
				"You will receive an email with instructions about how to reset your password in a few minutes.");
		TestListeners.extentTest.get().pass("Api v1 forgot password is successful");

		// account balance api of mobile v1 (/api/mobile/accounts)
		Response accounts_Response = pageObj.prodEndpoints().Api1MobileAccounts(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(accounts_Response.getStatusCode(), 200,
				"Status code 200 did not matched for api v1 mobile accounts");

		// user balance api of mobile v1 (/api/mobile/users/balance")
		Response balance_Response = pageObj.prodEndpoints().Api1MobileUsersbalance(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(balance_Response.getStatusCode(), 200,
				"Status code 200 did not matched for api v1 mobile users balance");

		// user Checkins Balance api of mobile v1
		Response Api1MobileCheckinsBalanceResponse = pageObj.prodEndpoints().Api1MobileCheckinsBalance(token,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(Api1MobileCheckinsBalanceResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api mobile Checkins Balance");
		logger.info("Checkins Balance api of mobile is successful");
		TestListeners.extentTest.get().info("Checkins Balance api of mobile is successful");

		// Deactivate a User
		Response deactivateUserresponse1 = pageObj.prodEndpoints().deactivateUser(userID,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deactivateUserresponse1.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Deactivate a User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Deactivate a User is successful");

		// Delete a User
		Response deleteUserResponse = pageObj.prodEndpoints().deleteUser(userID, dataSet.get("adminAuthorization"));
		Assert.assertEquals(deleteUserResponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Delete a User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Delete a User is successful");

	}

	@Test(description = "SQ-T2378 Verify mobile api2 ", groups = "api")
	public void verifyMobileApi2() {

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.prodEndpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// User login using API2 Signin
		/*Response loginResponse = pageObj.prodEndpoints().Api2Login(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(200, loginResponse.getStatusCode(), "Status code 200 did not matched for api2 login");
		// String token = loginResponse.jsonPath().get("access_token.token").toString();
		TestListeners.extentTest.get().pass("Api2 user Login is successful ");*/

		// Fetch user information
		Response userInfoResponse = pageObj.prodEndpoints().Api2FetchUserInfo(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(200, userInfoResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user info");
		TestListeners.extentTest.get().pass("Api2 fetch user information is successful");

		// Update user profile
		Response updateUserProfileResponse = pageObj.prodEndpoints().Api2UpdateUserProfile(dataSet.get("client"),
				userEmail, dataSet.get("secret"), token);
		Assert.assertEquals(200, updateUserProfileResponse.getStatusCode(),
				"Status code 200 did not matched for api2 update user info");
		TestListeners.extentTest.get().pass("Api2 update user information is successful");

		// Create user relation
		Response createUserRelationResponse = pageObj.prodEndpoints().Api2CreateUserrelation(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(createUserRelationResponse.getStatusCode(), 201,
				"Status code 200 did not matched for api2 signup");
		int id = createUserRelationResponse.jsonPath().get("id");
		TestListeners.extentTest.get().pass("Api2 Create user relation is successful");

		// Update user relation
		Response updateUserRelationResponse = pageObj.prodEndpoints().Api2UpdateUserrelation(dataSet.get("client"),
				dataSet.get("secret"), token, id);
		Assert.assertEquals(updateUserRelationResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 Update user relation is successful");

		// Delete user relation
		Response deleteUserRelationResponse = pageObj.prodEndpoints().Api2DeleteUserRelation(dataSet.get("client"),
				dataSet.get("secret"), token, Integer.toString(id));
		Assert.assertEquals(deleteUserRelationResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 Delete user relation is successful");

		// Create Loyalty Checkin by Receipt Image
		Response receiptCheckinResponse = pageObj.prodEndpoints().Api2LoyaltyCheckinReceiptImage(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("location_id"));
		Assert.assertEquals(receiptCheckinResponse.getStatusCode(), 201,
				"Status code 201 did not matched for api2 Loyalty Checkin by Receipt Image");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.prodEndpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		// Account Balance
		Response accountBalResponse = pageObj.prodEndpoints().Api2AccountBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(accountBalResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 account balance");

		// Transaction Details
		Response txnDetailsResponse = pageObj.prodEndpoints().Api2Trasactiondetails(dataSet.get("client"),
				dataSet.get("secret"), token, checkin_id);
		Assert.assertEquals(txnDetailsResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 transaction balance");

		// Asynchronous User Update
		Response updateUserInfoResponse = pageObj.prodEndpoints().Api2AsynchronousUserUpdate(dataSet.get("client"),
				userEmail, dataSet.get("secret"), token);
		Assert.assertEquals(202, updateUserInfoResponse.getStatusCode(),
				"Status code 202 did not matched for api2 asynchronous user update");
		TestListeners.extentTest.get().pass("Api2 update user information is successful");

		// Get User Session Token
		Response sessionTokenResponse = pageObj.prodEndpoints().Api2UserSessionToken(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(200, sessionTokenResponse.getStatusCode(),
				"Status code 200 did not matched for api2 get user session token");
		TestListeners.extentTest.get().pass("Api2 get user session token is successful");

		// Send Verification Email
		Response verificationEmailResponse = pageObj.prodEndpoints().Api2SendVreificationEmail(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(200, verificationEmailResponse.getStatusCode(),
				"Status code 200 did not matched for api2 get user session token");
		TestListeners.extentTest.get().pass("Api2 get user session token is successful");

		// Forgot password
		Response forgotPasswordResponse = pageObj.prodEndpoints().Api2ForgotPassword(dataSet.get("client"),
				dataSet.get("secret"), userEmail);
		Assert.assertEquals(200, forgotPasswordResponse.getStatusCode(),
				"Status code 200 did not matched for api2 forgot password");
		TestListeners.extentTest.get().pass("Api2 Forgot password is successful");

		// Fetch user balance
		Response userBalanceResponse = pageObj.prodEndpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(200, userBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		TestListeners.extentTest.get().pass("Api2 Fetch user balance is successful");

		// Balance Timeline
		Response balanceTimelineResponse = pageObj.prodEndpoints().Api2BalanceTimeline(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(200, balanceTimelineResponse.getStatusCode(),
				"Status code 200 did not matched for api2 balance timeline");
		TestListeners.extentTest.get().pass("Api2 Balance Timeline is successful");

		// Estimate Points Earning
		Response pointsEarningResponse = pageObj.prodEndpoints().Api2EstimatePointsEarning(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(200, pointsEarningResponse.getStatusCode(),
				"Status code 200 did not matched for api2 estimate points earning");
		TestListeners.extentTest.get().pass("Api2  Estimate Points Earning is successful");

		// User Account History
		Response accountHistoryResponse = pageObj.prodEndpoints().Api2UserAccountHistory(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(200, accountHistoryResponse.getStatusCode(),
				"Status code 200 did not matched for api2 User Account History");

		// Create Social Cause Campaigns
		String campaignName = "SocialCauseCampaign" + CreateDateTime.getTimeDateString();
		Response socialCauseCampaignResponse = pageObj.prodEndpoints().cReateSocialcauseCampaign(campaignName,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(socialCauseCampaignResponse.getStatusCode(), 201,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Social Cause Campaign");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Create Social Cause Campaigns is successful");
		String social_cause_id = socialCauseCampaignResponse.jsonPath().get("social_cause_id").toString();

		// send reward amount to user Amount
		Response sendRewardResponse = pageObj.prodEndpoints().sendMessageToUser(userID,
				dataSet.get("adminAuthorization"), dataSet.get("amount"), "", "", "");
		Assert.assertEquals(201, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// Get Social Cause Campaigns
		Response getsocialCauseCampaignResponse = pageObj.prodEndpoints().Api2SocialCauseCampaign(dataSet.get("client"),
				dataSet.get("secret"), token, social_cause_id);
		Assert.assertEquals(getsocialCauseCampaignResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 Social Cause Campaign Details");
		TestListeners.extentTest.get().pass("Api2 Social Cause Campaign Details is successful");
		String getsocial_cause_id = getsocialCauseCampaignResponse.jsonPath().get("social_cause_id").toString();

		// Create Donation
		Response createDonationResponse = pageObj.prodEndpoints().Api2CreateDonation(dataSet.get("client"),
				dataSet.get("secret"), token, getsocial_cause_id);
		Assert.assertEquals(createDonationResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 Create Donation");
		TestListeners.extentTest.get().pass("Api2 Create Donation is successful");

		// Social Cause Campaign Details
		Response socialCauseCampaignDetailsResponse = pageObj.prodEndpoints().Api2SocialCausecampaigndetails(
				dataSet.get("client"), dataSet.get("secret"), token, getsocial_cause_id);
		Assert.assertEquals(socialCauseCampaignDetailsResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 Create Donation");
		TestListeners.extentTest.get().pass("Api2 Create Donation is successful");

		// Deactivate Social Cause Campaign
		Response deactivateSocialCampaignResponse = pageObj.prodEndpoints().deactivateSocialCampaign(social_cause_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deactivateSocialCampaignResponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Deactivate Social Cause Campaign");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Deactivate Social Cause Campaign is successful");

		// Create Feedback
		Response createfeedbackResponse = pageObj.prodEndpoints().Api2CreateFeedback(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(createfeedbackResponse.getStatusCode(), 201,
				"Status code 201 did not matched for api2 create feedback");
		TestListeners.extentTest.get().pass("Api2 Fetch User Notifications successful ");
		String feedback_id = createfeedbackResponse.jsonPath().getString("feedback_id").toString();

		// Update Feedback
		Response updatefeedbackResponse = pageObj.prodEndpoints().Api2UpdateFeedback(dataSet.get("client"),
				dataSet.get("secret"), token, feedback_id);
		Assert.assertEquals(updatefeedbackResponse.getStatusCode(), 201,
				"Status code 201 did not matched for api2 update feedback");
		TestListeners.extentTest.get().pass("Api2 Fetch User Notifications successful ");

		// Fetch Client Token
		Response fetchClientTokenResponse = pageObj.prodEndpoints().Api2FetchClientToken(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchClientTokenResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 fetch client token");
		TestListeners.extentTest.get().pass("Api2 Fetch User Notifications successful ");

		// Fetch User Notifications
		String notification_id = "";
		boolean flag = false;
		int attempts = 0;
		while (attempts < 20) {
			try {
				TestListeners.extentTest.get().info("API hit count is : " + attempts);
				logger.info("API hit count is : " + attempts);
				utils.longwait(5000);
				Response fetchNotificationsResponse = pageObj.prodEndpoints()
						.Api2FetchNotifications(dataSet.get("client"), dataSet.get("secret"), token);

				int statusCode = fetchNotificationsResponse.getStatusCode();
				if (statusCode == 200) {
					flag = true;
					Assert.assertEquals(200, fetchNotificationsResponse.getStatusCode(),
							"Status code 200 did not matched for api2 fetch notifications");
					TestListeners.extentTest.get().pass("Api2 Fetch User Notifications successful ");
					notification_id = fetchNotificationsResponse.jsonPath().get("[0].id").toString();
					logger.info("Response time in milliseconds is :" + fetchNotificationsResponse.getTime());
					break;
				}
			} catch (Exception e) {

			}
			attempts++;
		}

		// Delete User Notification Response
		Response deleteNotificationsResponse = pageObj.prodEndpoints().Api2DeletehNotifications(dataSet.get("client"),
				dataSet.get("secret"), token, notification_id);
		Assert.assertEquals(deleteNotificationsResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 delete notifications");
		TestListeners.extentTest.get().pass("Api2 Reload Gift Card is successful ");

		// User logout using API2 Logout
		Response logoutResponse = pageObj.prodEndpoints().Api2Logout(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(200, logoutResponse.getStatusCode(), "Status code 200 did not matched for api2 logout");
		TestListeners.extentTest.get().pass("Api2 user Logout is successful ");

		// Deactivate a User
		Response deactivateUserresponse1 = pageObj.prodEndpoints().deactivateUser(userID,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deactivateUserresponse1.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Deactivate a User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Deactivate a User is successful");

		// Delete a User
		Response deleteUserResponse = pageObj.prodEndpoints().deleteUser(userID, dataSet.get("adminAuthorization"));
		Assert.assertEquals(deleteUserResponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Delete a User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Delete a User is successful");

	}

	@AfterMethod(alwaysRun = true)
	public void afterMethod() {

	}
}
