package com.punchh.server.LP1Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;
import com.punchh.server.apiConfig.ApiConstants;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class MigrationTest {
	public WebDriver driver;
	String userEmail;
	ApiUtils apiUtils;
	PageObj pageObj;
	String sTCName;
	String timeStamp;
	String run = "ui";
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	private Utilities utilities;

	@BeforeMethod(alwaysRun = true)
	public void beforeMethod(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		apiUtils = new ApiUtils();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		utilities = new Utilities();
	}

	@Test(groups = { "regression",
			"dailyrun" }, description = "SQ-T2563, Migrated >> Verify that all the migrated guests are displayed here || SQ-T2556 Verify guest is in Awaiting Migration", priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2563_verifyMigrationGuest() throws InterruptedException {
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		timeStamp = CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// pageObj.menupage().clickGuestMenu();
		// pageObj.menupage().awaitingMigrationLink();
		pageObj.menupage().navigateToSubMenuItem("Guests", "Awaiting Migration");
		pageObj.awaitingMigrationPage().createNewMigrationGuest(userEmail, timeStamp, dataSet.get("location"),
				dataSet.get("gender"));
		boolean result = pageObj.awaitingMigrationPage().verifyMigrationUser(userEmail, timeStamp);
		Assert.assertTrue(result, "Migration user is not created successfully");
		Response response = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		apiUtils.verifyResponse(response, "API 1 user signup");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		String joiningchannel = pageObj.guestTimelinePage().getGuestJoiningChannelMobileEmail();
		String guestTimelineEmail = pageObj.guestTimelinePage().getGuestTimelineEmail();

		Assert.assertEquals(dataSet.get("joinedViaMobile"), joiningchannel,
				"Joined channel guest details did not matched");
		Assert.assertEquals(userEmail, guestTimelineEmail, "Guest email did not matched on timeline header");
		utilities.logPass("Successfully verified guest email and joined channel");

		Assert.assertTrue(pageObj.guestTimelinePage().verifyMigrationPoint(), "Error in verifying guest timeline");
		// pageObj.menupage().clickGuestMenu();
		// pageObj.menupage().migratedLink();
		pageObj.menupage().navigateToSubMenuItem("Guests", "Migrated");
		Assert.assertTrue(pageObj.awaitingMigrationPage().verifyMigratedGuest(userEmail, timeStamp));
	}

	@Test(groups = { "regression",
			"dailyrun" }, description = "SQ-T2604 (1.0) Validate that Initial Points and Original Points under “Old Program Points“ are reflected as they are received in the migration data."
					+ " || SQ-T2606 (1.0) Validate that Original points do not get increased even if guests earn loyalty on the Punchh platform after migration", priority = 1)
	@Owner(name = "Amit Kumar")
	public void T2604_verifyOldMigrationPoint() throws InterruptedException {
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		timeStamp = CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// pageObj.menupage().clickGuestMenu();
		// pageObj.menupage().awaitingMigrationLink();
		pageObj.menupage().navigateToSubMenuItem("Guests", "Awaiting Migration");
		pageObj.awaitingMigrationPage().createNewMigrationGuest(userEmail, timeStamp, dataSet.get("location"),
				dataSet.get("gender"));
		boolean result = pageObj.awaitingMigrationPage().verifyMigrationUser(userEmail, timeStamp);
		Assert.assertTrue(result, "Migration user is not created successfully");
		// user signup
		Response response = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		apiUtils.verifyResponse(response, "API 1 user signup");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String userID = response.jsonPath().get("id").toString();
		String joiningchannel = pageObj.guestTimelinePage().getGuestJoiningChannelMobileEmail();
		String guestTimelineEmail = pageObj.guestTimelinePage().getGuestTimelineEmail();

		Assert.assertEquals(dataSet.get("joinedViaMobile"), joiningchannel,
				"Joined channel guest details did not matched");
		Assert.assertEquals(userEmail, guestTimelineEmail, "Guest email did not matched on timeline header");
		utilities.logPass("Successfully verified guest email and joined channel");

		Assert.assertTrue(pageObj.guestTimelinePage().verifyMigrationPoint(),
				"Error in verifying migration point on guest timeline");
		Assert.assertTrue(pageObj.guestTimelinePage().verifyOldProgramPoint(),
				"Error in verifying migration point card");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "",
				"100");
		utilities.logPass("Send point to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		Assert.assertTrue(pageObj.guestTimelinePage().verifyOldProgramPoint(),
				"Error in verifying migration point card");
	}

	@Test(description = "SQ-T4528 Verify the business migration with blank card details added in BMU table"
			+ "SQ-T4527	Verify the gift card is getting added in BMU user when passing blank card number in create and update API.", groups = {
					"regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void T4528_VerifyBusinessMgrationWithBlankCardDetails() throws Exception {
		String carNumber = dataSet.get("validCardNumber");
		String epin = dataSet.get("epin");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		String external_source_id = CreateDateTime.getTimeDateString();

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserWithBlankResponse = pageObj.endpoints()
				.createBusinessMigrWithSingleCardDetails(userEmail, dataSet.get("apiKey"), "", "");
		Assert.assertEquals(createMigrationUserWithBlankResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Migration User");
		utilities.logPass("PLATFORM FUNCTIONS API Create Business Migration User is successful");

		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("external_source"), external_source_id);

		// Timeline validation (move to user timeline)
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().navigateToTabs("Gift Cards");
		String messgeTextForBlank = pageObj.guestTimelinePage().getGiftCardMessageForBlankCardNumber();
		Assert.assertEquals(messgeTextForBlank, "This user does not have any Gift Cards.",
				"Gift card message not coming ");

		utilities.logPass("Verified the expected error message is coming for the first user " + userEmail);

		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserWithNilResponse = pageObj.endpoints()
				.createBusinessMigrWithSingleCardDetails(userEmail2, dataSet.get("apiKey"), "nil", "nil");
		Assert.assertEquals(createMigrationUserWithNilResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Migration User");
		utilities.logPass("PLATFORM FUNCTIONS API Create Business Migration User is successful");

		Response signUpResponse2 = pageObj.endpoints().Api2SignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("external_source"), external_source_id);

		// Timeline validation (move to user timeline)
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail2);
		pageObj.guestTimelinePage().navigateToTabs("Gift Cards");
		String messgeTextForNil = pageObj.guestTimelinePage().getGiftCardMessageForBlankCardNumber();
		Assert.assertEquals(messgeTextForNil, "This user does not have any Gift Cards.",
				"Gift card message not coming for the user " + userEmail2);

		utilities.logPass("Verified the expected error message is coming for the first user " + userEmail2);

		external_source_id = CreateDateTime.getTimeDateString();
		// create user with card number

		String sql1 = "select id from gift_cards where business_id = '" + dataSet.get("slugID") + "'";

		pageObj.singletonDBUtilsObj();
		String giftCardIdFromDB = DBUtils.executeQueryAndGetColumnValue(env, sql1, "id");

		String deleteCardFromGiftCardTableSQL = "delete from gift_cards where business_id = '" + dataSet.get("slugID")
				+ "'";
		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, deleteCardFromGiftCardTableSQL);

		String deleteGiftCardFromUserCardsTableSQL = "delete from user_cards where gift_card_id = '" + giftCardIdFromDB
				+ "'";
		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, deleteGiftCardFromUserCardsTableSQL);

		String userEmail3 = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserWithCardNumberResponse = pageObj.endpoints()
				.createBusinessMigrWithSingleCardDetails(userEmail3, dataSet.get("apiKey"), carNumber, epin);
		Assert.assertEquals(createMigrationUserWithCardNumberResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Migration User");
		utilities.logPass("PLATFORM FUNCTIONS API Create Business Migration User is successful");

		Response signUpResponse3 = pageObj.endpoints().Api2SignUp(userEmail3, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("external_source"), external_source_id);
		// Timeline validation (move to user timeline)
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail3);
		pageObj.guestTimelinePage().navigateToTabs("Gift Cards");
		String messgeTextForCard = pageObj.guestTimelinePage().getGiftCardMessageForBlankCardNumber();
		Assert.assertTrue(messgeTextForCard.isBlank(), "Gift card message not coming for the user " + userEmail3);

		String actualCardNumber = pageObj.guestTimelinePage().getGiftCardNumberFromGiftCardTab(userEmail3);
		Assert.assertEquals(actualCardNumber, carNumber, carNumber + " card number is not matched on user time line ");

		utilities.logPass("Verified the expected Card number is coming on user timeline page  " + userEmail3);

	}

	// Anant
	@Test(description = "SQ-T4762 Verify If a valid gift_card_design_id is passed via BMU Migration API upload then that particular gift_card_design_id is assigned to Gift card", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void T4762_verifyGiftCardDesignIDViaBMU() throws InterruptedException {
		String cardNumber = CreateDateTime.getTimeDateString();
		String cardNumber2 = dataSet.get("cardNumber");
		String cardId = dataSet.get("cardId");
		String external_source_id = CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserWithBlankResponse = pageObj.endpoints().createMigrationUserWithGiftID(userEmail,
				dataSet.get("apiKey"), cardNumber, cardId);
		Assert.assertEquals(createMigrationUserWithBlankResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for Create Business Migration User");
		utilities.logPass("verified awaiting user is created successful");

		pageObj.menupage().navigateToSubMenuItem("Guests", "Awaiting Migration");
		boolean val = pageObj.awaitingMigrationPage().searchMigratedGuest(userEmail);
		Assert.assertTrue(val, "awaiting user -- " + userEmail + " is not created");
		utilities.logPass("Verified awaiting user -- " + userEmail + " is created");

		boolean bol = pageObj.awaitingMigrationPage().cardNumberOnMigratedGuest(userEmail, cardNumber2);
		Assert.assertTrue(bol, "card number is not visible on the migrated user");
		utilities.logPass("Verified card number is visible on the migrated user");

		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("external_source"), external_source_id);
		Assert.assertEquals(signUpResponse.statusCode(), ApiConstants.HTTP_STATUS_OK, "status code not match for the sign up api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().navigateToTabs("Gift Cards");

		String actualCardNumber = pageObj.guestTimelinePage().getGiftCardNumberFromGiftCardTab(userEmail);
		System.out.println(actualCardNumber);
		Assert.assertEquals(actualCardNumber, cardNumber,
				cardNumber + " card number is not matched on user time line ");
		utilities.logPass("Verified the expected Card number is coming on user timeline page  " + userEmail);
	}

	@Test(description = "SQ-T4526	Verify the business migration with one blank and one valid gift card details added in BMU table", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void T4526_VerifyBusinessMgrationWithDoulbeCardDetails() throws Exception {
		String carNumber = dataSet.get("validCardNumber");
		String epin = dataSet.get("epin");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		String external_source_id = CreateDateTime.getTimeDateString();
		// For test case SQ-T4526

		String sql12 = "select id from gift_cards where business_id = '" + dataSet.get("slugID") + "'";

		pageObj.singletonDBUtilsObj();
		String giftCardIdFromDB2 = DBUtils.executeQueryAndGetColumnValue(env, sql12, "id");

		String deleteCardFromGiftCardTableSQL2 = "delete from gift_cards where business_id = '" + dataSet.get("slugID")
				+ "'";
		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, deleteCardFromGiftCardTableSQL2);

		String deleteGiftCardFromUserCardsTableSQL2 = "delete from user_cards where gift_card_id = '"
				+ giftCardIdFromDB2 + "'";
		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, deleteGiftCardFromUserCardsTableSQL2);

		String userEmail4 = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserWithDoubleCardResponse = pageObj.endpoints()
				.createBusinessMigrWithDoubleCardDetails(userEmail4, dataSet.get("apiKey"), "", "", carNumber, epin);
		Assert.assertEquals(createMigrationUserWithDoubleCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Migration User");
		utilities.logPass("PLATFORM FUNCTIONS API Create Business Migration User is successful");

		Response signUpResponse4 = pageObj.endpoints().Api2SignUp(userEmail4, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("external_source"), external_source_id);
		// Timeline validation (move to user timeline)
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail4);
		pageObj.guestTimelinePage().navigateToTabs("Gift Cards");
		String messgeTextForDoubleCard = pageObj.guestTimelinePage().getGiftCardMessageForBlankCardNumber();
		Assert.assertFalse(messgeTextForDoubleCard.contains("This user does not have any Gift Cards."),
				"Gift card message not coming for the user " + userEmail4);

		String actualDoubleCardNumber = pageObj.guestTimelinePage().getGiftCardNumberFromGiftCardTab(userEmail4);
		Assert.assertEquals(actualDoubleCardNumber, carNumber,
				carNumber + " card number is not matched on user time line ");

		utilities.logPass("Verified the expected Card number is coming on user timeline page  " + userEmail4);

	}

	// Rakhi
	@Test(description = "SQ-T6101 Default Values for Migration User Creation (Drop-down as NULL)")
	@Owner(name = "Rakhi Rawat")
	public void T6101_VerifyDefaultValuesForMigrationUser() throws Exception {

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		timeStamp = CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Guests", "Awaiting Migration");
		pageObj.awaitingMigrationPage().createNewMigrationGuest(userEmail, timeStamp, dataSet.get("location"),
				dataSet.get("gender"));
		boolean result = pageObj.awaitingMigrationPage().verifyMigrationUser(userEmail, timeStamp);
		Assert.assertTrue(result, "Migration user is not created successfully");

		String query1 = "SELECT `business_migration_users`.other_details FROM `business_migration_users` WHERE email='"
				+ userEmail + "';";
		pageObj.singletonDBUtilsObj();
		String details = DBUtils.executeQueryAndGetColumnValue(env, query1, "other_details");

		List<String> marketingPnSubscription = Utilities.getPreferencesKeyValue(details, "marketing_pn_subscription");
		List<String> marketingEmailSubscription = Utilities.getPreferencesKeyValue(details,
				"marketing_email_subscription");
		Assert.assertTrue(marketingPnSubscription.isEmpty(), "marketing_pn_subscription key values is not empty");
		Assert.assertTrue(marketingEmailSubscription.isEmpty(), "marketing_email_subscription key values is not empty");
		utilities.logPass("Verified marketing_pn_subscription and marketing_email_subscription key values are empty");

	}

	// Rakhi
	@Test(description = "SQ-T6102 Value Provided at Sign-Up not Overridden by Migration Values (Drop-down as TRUE)")
	@Owner(name = "Rakhi Rawat")
	public void T6102_VerifyMigrationUserValues() throws Exception {

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		timeStamp = CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Guests", "Awaiting Migration");
		pageObj.awaitingMigrationPage().createNewMigrationGuestWithEmailAndPnSubscription(userEmail, timeStamp,
				dataSet.get("location"), dataSet.get("gender"), dataSet.get("choiceEmail"), dataSet.get("choicePn"));
		boolean result = pageObj.awaitingMigrationPage().verifyMigrationUser(userEmail, timeStamp);
		Assert.assertTrue(result, "Migration user is not created successfully");

		String query1 = "SELECT `business_migration_users`.other_details FROM `business_migration_users` WHERE email='"
				+ userEmail + "';";
		pageObj.singletonDBUtilsObj();
		String details = DBUtils.executeQueryAndGetColumnValue(env, query1, "other_details");
		pageObj.singletonDBUtilsObj();
		Map<String, String> detailsMap = utilities.parseDetailsToMap(details);

		// Fetch values
		String emailSub = detailsMap.get("marketing_email_subscription");
		String pnSub = detailsMap.get("marketing_pn_subscription");
		utilities.logit("Marketing Email Subscription: " + emailSub);
		utilities.logit("Marketing Pn Subscription: " + pnSub);

		Assert.assertEquals(pnSub, "true", "marketing_pn_subscription key values is not empty");
		Assert.assertEquals(emailSub, "true", "marketing_email_subscription key values is not empty");
		utilities.logPass("Verified marketing_pn_subscription and marketing_email_subscription key values are true");

		// signup migration user
		Response response = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		apiUtils.verifyResponse(response, "API 1 user signup");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());

		// verify Marketing Email Subscription and Marketing Email Subscription on guest
		// edit profile page
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().navigateToTabs("Edit Profile");
		String checkBox1 = pageObj.dashboardpage().checkBoxResponse("Marketing Email Subscription?");
		Assert.assertEquals(checkBox1, "true", "Marketing Email Subscription is not checked for signed up user");
		String checkBox2 = pageObj.dashboardpage().checkBoxResponse("Marketing PN Subscription?");
		Assert.assertEquals(checkBox2, "true", "Marketing Pn Subscription is not checked for signed up user");

		utilities.logPass(
				"Verified Marketing Email Subscription and Marketing Pn Subscription value provided at Sign-Up not Overridden by Migration Values (Drop-down as TRUE)");
	}

	// Rakhi
	@Test(description = "SQ-T6110 Value Provided at Sign-Up Overridden (Drop-down as NULL)")
	@Owner(name = "Rakhi Rawat")
	public void T6110_VerifyMigrationSignupValuesOverridden() throws Exception {

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		timeStamp = CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Guests", "Awaiting Migration");
		pageObj.awaitingMigrationPage().createNewMigrationGuest(userEmail, timeStamp, dataSet.get("location"),
				dataSet.get("gender"));
		boolean result = pageObj.awaitingMigrationPage().verifyMigrationUser(userEmail, timeStamp);
		Assert.assertTrue(result, "Migration user is not created successfully");

		// Fetch migration user details from DB
		String query1 = "SELECT `business_migration_users`.other_details FROM `business_migration_users` WHERE email='"
				+ userEmail + "';";
		pageObj.singletonDBUtilsObj();
		String details = DBUtils.executeQueryAndGetColumnValue(env, query1, "other_details");
		pageObj.singletonDBUtilsObj();
		Map<String, String> detailsMap = utilities.parseDetailsToMap(details);

		// Fetch values
		String emailSub = detailsMap.get("marketing_email_subscription");
		String pnSub = detailsMap.get("marketing_pn_subscription");
		utilities.logit("Marketing Email Subscription: " + emailSub);
		utilities.logit("Marketing Pn Subscription: " + pnSub);

		Assert.assertTrue(pnSub.isEmpty(), "marketing_pn_subscription key values is not null");
		Assert.assertTrue(emailSub.isEmpty(), "marketing_email_subscription key values is not null");
		utilities.logPass("Verified marketing_pn_subscription and marketing_email_subscription key values are null");

		// signup migration user with email as true and pn subscription as false
		Response response = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("choiceEmail"), dataSet.get("choicePn"));
		apiUtils.verifyResponse(response, "API 1 user signup");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().navigateToTabs("Edit Profile");
		String checkBox = pageObj.dashboardpage().checkBoxResponse("Marketing Email Subscription?");
		String checkBox1 = pageObj.dashboardpage().checkBoxResponse("Marketing PN Subscription?");

		Assert.assertEquals(checkBox, "true", "marketing_email_subscription key values is not true");
		Assert.assertNull(checkBox1, "marketing_pn_subscription key values is not false");
		utilities.logPass(
				"Verified marketing_pn_subscription and marketing_email_subscription key values provided at Sign-Up Overridden (Drop-down as NULL)");

	}

	// Rakhi
	@Test(description = "SQ-T6109 Value Provided at Sign-Up not overridden by Migration Values (Drop-down as NULL)")
	@Owner(name = "Rakhi Rawat")
	public void T6109_VerifyMigrationSignupValuesNotOverridden() throws Exception {

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		timeStamp = CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Guests", "Awaiting Migration");
		pageObj.awaitingMigrationPage().createNewMigrationGuest(userEmail, timeStamp, dataSet.get("location"),
				dataSet.get("gender"));
		boolean result = pageObj.awaitingMigrationPage().verifyMigrationUser(userEmail, timeStamp);
		Assert.assertTrue(result, "Migration user is not created successfully");

		String query1 = "SELECT `business_migration_users`.other_details FROM `business_migration_users` WHERE email='"
				+ userEmail + "';";
		pageObj.singletonDBUtilsObj();
		String details = DBUtils.executeQueryAndGetColumnValue(env, query1, "other_details");
		pageObj.singletonDBUtilsObj();
		Map<String, String> detailsMap = utilities.parseDetailsToMap(details);

		// Fetch values
		String emailSub = detailsMap.get("marketing_email_subscription");
		String pnSub = detailsMap.get("marketing_pn_subscription");
		utilities.logit("Marketing Email Subscription: " + emailSub);
		utilities.logit("Marketing Pn Subscription: " + pnSub);

		Assert.assertTrue(pnSub.isEmpty(), "marketing_pn_subscription key values is not null");
		Assert.assertTrue(emailSub.isEmpty(), "marketing_email_subscription key values is not null");
		utilities.logPass("Verified marketing_pn_subscription and marketing_email_subscription key values are null");

		// signup migration user with both email and pn subscription as false
		Response response = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"), dataSet.get("secret"),
				"", "");
		apiUtils.verifyResponse(response, "API 1 user signup");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().navigateToTabs("Edit Profile");
		String checkBox = pageObj.dashboardpage().checkBoxResponse("Marketing Email Subscription?");
		String checkBox1 = pageObj.dashboardpage().checkBoxResponse("Marketing PN Subscription?");

		Assert.assertNull(checkBox, "marketing_email_subscription key values is not false");
		Assert.assertNull(checkBox1, "marketing_pn_subscription key values is not false");
		utilities.logPass(
				"Verified marketing_pn_subscription and marketing_email_subscription key values provided at Sign-Up Overridden");

	}

	// Rakhi
	@Test(description = "SQ-T6108 Admin Changes Migration User Values to NULL After User Sign-Up")
	@Owner(name = "Rakhi Rawat")
	public void T6108_VerifyMigrationUserValuesChangeAfterUserSignup() throws Exception {

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		timeStamp = CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Guests", "Awaiting Migration");
		pageObj.awaitingMigrationPage().createNewMigrationGuestWithEmailAndPnSubscription(userEmail, timeStamp,
				dataSet.get("location"), dataSet.get("gender"), dataSet.get("choiceEmail"), dataSet.get("choicePn"));
		boolean result = pageObj.awaitingMigrationPage().verifyMigrationUser(userEmail, timeStamp);
		Assert.assertTrue(result, "Migration user is not created successfully");

		String query1 = "SELECT `business_migration_users`.other_details FROM `business_migration_users` WHERE email='"
				+ userEmail + "';";
		pageObj.singletonDBUtilsObj();
		String details = DBUtils.executeQueryAndGetColumnValue(env, query1, "other_details");
		pageObj.singletonDBUtilsObj();
		Map<String, String> detailsMap = utilities.parseDetailsToMap(details);

		// Fetch values
		String emailSub = detailsMap.get("marketing_email_subscription");
		String pnSub = detailsMap.get("marketing_pn_subscription");
		utilities.logit("Marketing Email Subscription: " + emailSub);
		utilities.logit("Marketing Pn Subscription: " + pnSub);

		Assert.assertEquals(pnSub, "true", "marketing_pn_subscription key values is not true");
		Assert.assertEquals(emailSub, "true", "marketing_email_subscription key values is not true");
		utilities.logPass(
				"Verified marketing_pn_subscription and marketing_email_subscription key values are true in dropdown");

		// signup migration user without changing email and pn subscription values
		Response response = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"), dataSet.get("secret"),
				"", "");
		apiUtils.verifyResponse(response, "API 1 user signup");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().navigateToTabs("Edit Profile");
		String checkBox = pageObj.dashboardpage().checkBoxResponse("Marketing Email Subscription?");
		String checkBox1 = pageObj.dashboardpage().checkBoxResponse("Marketing PN Subscription?");
		Assert.assertEquals(checkBox, "true", "marketing_email_subscription key values is not true");
		Assert.assertEquals(checkBox1, "true", "marketing_pn_subscription key values is not true");

		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Marketing Email Subscription?", "uncheck");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Marketing PN Subscription?", "uncheck");

		String updatedCheckBox = pageObj.dashboardpage().checkBoxResponse("Marketing Email Subscription?");
		String updatedCheckBox1 = pageObj.dashboardpage().checkBoxResponse("Marketing PN Subscription?");
		Assert.assertNull(updatedCheckBox, "marketing_email_subscription key values is not false");
		Assert.assertNull(updatedCheckBox1, "marketing_pn_subscription key values is not false");
		utilities.logPass("Verified Admin can change Migration User Values to NULL After User Sign-Up");
	}

	@AfterMethod(alwaysRun = true)
	public void afterMethod() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		utilities.logit("Browser closed");
	}
}