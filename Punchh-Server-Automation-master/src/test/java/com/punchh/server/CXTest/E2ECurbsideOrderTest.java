package com.punchh.server.CXTest;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class E2ECurbsideOrderTest {

	private static Logger logger = LogManager.getLogger(E2ECurbsideOrderTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName, env;
	private String activate = "/activate/";
	private Utilities utils;
	private String run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		utils = new Utilities(driver);
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
	}

	@Test(description = "SQ-T6854: Verify Pickup/delivery/curbside E2E order journey using mobile api and pickup console [Part 1]; "
			+ "SQ-T3245: Validate that Multi- selection on pickup console should be worked as expected; "
			+ "SQ-T2570: Verify mobile pickup order creation through API", groups = "regression", priority = 0)
	@Owner(name = "Rajasekhar Reddy")
	public void T6854_verifyCurbsideOrderCreationFlow() throws InterruptedException {

		String orderId = "123" + CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().selectLocationSearch("Daphne");
		pageObj.dashboardpage().navigateToTabs("Pickup");
		utils.checkUncheckFlag("Curbside", "check");
		pageObj.dashboardpage().updateCheckBox();
		pageObj.locationPage().SelectLocationConsole();
		String consoleName = "Consolename_" + CreateDateTime.getTimeDateString();
		pageObj.locationPage().addConsole(consoleName);

		// Pickup console login
		pageObj.locationPage().navigateTopickupConsole(baseUrl + activate + dataSet.get("slug"));

		// User register/signup using API1 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		utils.logit("pass", "Api1 user signup is successful with email: " + userEmail);

		// Create vehicle for above registered user and get the vehicle id
		Response createVehicleResponse = pageObj.endpoints().Api1CreateVehicle(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(createVehicleResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 create Vehicle");
		utils.logit("pass", "Api1 create vehicle is successful");

		Response listVehicleResponse = pageObj.endpoints().Api1VehicleList(dataSet.get("client"), dataSet.get("secret"),
				token);
		Assert.assertEquals(listVehicleResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 create Vehicle");
		TestListeners.extentTest.get().pass("Api1 vehicle list is successful");
		utils.logPass("Vehicle list fetched succussfully for the user");
		String vechicleid = listVehicleResponse.jsonPath().get("vehicle_id").toString();
		vechicleid = vechicleid.substring(1, vechicleid.length() - 1);

		// create pickup order
		String deliveryMethod = "curbside";

		Response createPickupResponse = pageObj.endpoints().Api1CreateCurbsideOrder(dataSet.get("client"),
				dataSet.get("secret"), token, userEmail, orderId, deliveryMethod, vechicleid);
		Assert.assertEquals(createPickupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 create order pickup");
		String orderid = createPickupResponse.jsonPath().get("order_id").toString();
		String punchh_orderid = createPickupResponse.jsonPath().get("punchh_order_id").toString();
		utils.logit("pass", "Api1 create pickup order is successful and the order_id = " + orderid);

		// Open pickup console
		pageObj.locationPage().pickupConsoleLogin();

		boolean readyForCustomerButton = pageObj.locationPage().isDisplayed("pickupConsolePage.readyForCustomerButton");
		Assert.assertTrue(readyForCustomerButton, "ready for Customer Button not displayed");
		utils.logPass("ready for Customer Button is displayed");

		boolean delayedButton = pageObj.locationPage().isDisplayed("pickupConsolePage.delayedButton");
		Assert.assertTrue(delayedButton, "delayed Button not displayed");
		utils.logit("pass", "Ready for Customer Button is displayed");

		boolean pickUpButton = pageObj.locationPage().isDisplayed("pickupConsolePage.pickUpButton");
		Assert.assertTrue(pickUpButton, "pick up Button not displayed");
		utils.logit("pass", "Pickup Button is displayed");

		List<String> expTableHeadings = Arrays.asList("GUEST NAME", "ORDER ID", "CUSTOMER STATUS", "ORDER TYPE",
				"CAR DETAILS", "READY TIME", "ORDER STATUS");
		List<String> actualTableHeadings = pageObj.locationPage().getTableHeadings();

		Assert.assertTrue(expTableHeadings.equals(actualTableHeadings), "Table headings not present in the UI");
		utils.logit("pass", "All table headings are present");

		// Update customer status to enroute
		Response updateCustomerStatusResponse = pageObj.endpoints().ApiUpdateCustomerStatus(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("cus_status1"), punchh_orderid);
		Assert.assertEquals(updateCustomerStatusResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api  update customer status");
		utils.longWaitInSeconds(5); // wait for order status to be updated
		utils.refreshPage();
		pageObj.locationPage().orderSearch(orderId);
		Assert.assertEquals(pageObj.iframeConfigurationPage().getElementText("pickupConsolePage.ordertypeTextBox", "")
				.toLowerCase(), deliveryMethod);
		Assert.assertEquals(
				pageObj.iframeConfigurationPage().getElementText("pickupConsolePage.customerstatusTextBox", ""),
				"En Route");
		utils.logit("pass",
				"Api update customer status is successful. Customer status changed to " + dataSet.get("cus_status1"));

		// Update customer status to Nearby
		Response updateCustomerStatusResponse3 = pageObj.endpoints().ApiUpdateCustomerStatus(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("cus_status3"), punchh_orderid);
		Assert.assertEquals(updateCustomerStatusResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api  update customer status");
		utils.longWaitInSeconds(5); // wait for order status to be updated
		utils.refreshPage();
		pageObj.locationPage().orderSearch(orderId);
		utils.logit("pass",
				"Api update customer status is successful. Customer status changed to " + dataSet.get("cus_status3"));
		Assert.assertEquals(
				pageObj.iframeConfigurationPage().getElementText("pickupConsolePage.customerstatusTextBox", ""),
				"Expected Soon");

		// Update customer status to Arrived
		Response updateCustomerStatusResponse2 = pageObj.endpoints().ApiUpdateCustomerStatuswithArrivalInfo(
				dataSet.get("client"), dataSet.get("secret"), token, dataSet.get("cus_status2"), punchh_orderid);
		Assert.assertEquals(updateCustomerStatusResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api  update customer status");
		utils.longWaitInSeconds(6); // wait for order status to be updated
		utils.refreshPage();
		pageObj.locationPage().orderSearch(orderId);
		Assert.assertEquals(
				pageObj.iframeConfigurationPage().getElementText("pickupConsolePage.customerstatusTextBox", ""),
				"Arrived");
		utils.logit("pass",
				"Api update customer status is successful. Customer status changed to " + dataSet.get("cus_status2"));
		// Assert.assertEquals(utils.getLocator("pickupConsolePage.ordertypeTextBox","").toLowerCase(),deliveryMethod+"(#12a)");

		// Update Order status to Delay from console
		pageObj.locationPage().OrderstatustoDelayfromConsole();
		utils.longWaitInSeconds(5); // wait for order status to be updated
		Assert.assertEquals(
				pageObj.iframeConfigurationPage().getElementText("pickupConsolePage.orderstatusTextBox", ""),
				"Delayed");

		// Update Order status to Ready from console
		pageObj.locationPage().OrderstatustoReadyfromConsole();
		utils.longWaitInSeconds(5); // wait for order status to be updated
		Assert.assertEquals(
				pageObj.iframeConfigurationPage().getElementText("pickupConsolePage.orderstatusTextBox", ""), "Ready");

		// Update Order status to Pickedup from console
		pageObj.locationPage().orderStatusToPickedUpFromConsole(orderId);
		utils.longWaitInSeconds(6); // wait for order status to be updated
		Assert.assertEquals(
				pageObj.iframeConfigurationPage().getElementText("pickupConsolePage.orderstatusTextBoxPastOrders", ""),
				"Picked Up");
		utils.logPass("Order is picked up and moved to past order screen");

		// pageObj.locationPage().VerifyHowToUseTutorial();

		// deleting the console key
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().selectLocationSearch("Daphne");
		pageObj.locationPage().selectLocationConsoleList();
		pageObj.locationPage().deleteConsole(consoleName);

	}

	@Test(description = "SQ-T6854: Verify Pickup/delivery/curbside E2E order journey using mobile api and pickup console [Part 2]", groups = "regression", dataProvider = "T6854_part2DataProvider", priority = 1)
	@Owner(name = "Rajasekhar Reddy")
	public void T6854_verifyPickupAndDeliveryOrderCreationFlow(String deliveryType) throws InterruptedException {

		String orderId = "123" + CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().selectLocationSearch2("");
		pageObj.dashboardpage().navigateToTabs("Pickup");
		utils.checkUncheckFlag("Delivery", "check");
		utils.checkUncheckFlag("Pickup", "check");
		pageObj.dashboardpage().updateCheckBox();
		pageObj.locationPage().SelectLocationConsole();
		String consoleName = "Consolename_" + CreateDateTime.getTimeDateString();
		pageObj.locationPage().addConsole(consoleName);

		pageObj.locationPage().navigateTopickupConsole(baseUrl + activate + dataSet.get("slug"));

		// User register/signup using API1 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		utils.logit("pass", "Api1 user signup is successful with email: " + userEmail);

		// create pickup order
		Response createPickupResponse = pageObj.endpoints().Api1CreatePickupOrder(dataSet.get("client"),
				dataSet.get("secret"), token, userEmail, orderId, deliveryType, dataSet.get("storeNumber"));
		Assert.assertEquals(createPickupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 create order pickup");
		String orderid = createPickupResponse.jsonPath().get("order_id").toString();
		String punchh_orderid = createPickupResponse.jsonPath().get("punchh_order_id").toString();
		utils.logit("pass", "Api1 create pickup order is successful and the order_id = " + orderid);

		// Open pickup console
		pageObj.locationPage().pickupConsoleLogin();

		// Update customer status to enroute
		Response updateCustomerStatusResponse = pageObj.endpoints().ApiUpdateCustomerStatus(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("cus_status1"), punchh_orderid);
		Assert.assertEquals(updateCustomerStatusResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api  update customer status");
		utils.longWaitInSeconds(5); // wait for order status to be updated
		utils.refreshPage();
		pageObj.locationPage().orderSearch(orderId);
		Assert.assertEquals(pageObj.iframeConfigurationPage().getElementText("pickupConsolePage.ordertypeTextBox", "")
				.toLowerCase(), deliveryType);
		Assert.assertEquals(
				pageObj.iframeConfigurationPage().getElementText("pickupConsolePage.customerstatusTextBox", ""),
				"En Route");
		utils.logit("pass",
				"Api update customer status is successful. Customer status changed to " + dataSet.get("cus_status1"));

		// Update customer status to Nearby
		Response updateCustomerStatusResponse3 = pageObj.endpoints().ApiUpdateCustomerStatus(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("cus_status3"), punchh_orderid);
		Assert.assertEquals(updateCustomerStatusResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api  update customer status");
		utils.longWaitInSeconds(5); // wait for order status to be updated
		utils.refreshPage();
		pageObj.locationPage().orderSearch(orderId);
		Assert.assertEquals(
				pageObj.iframeConfigurationPage().getElementText("pickupConsolePage.customerstatusTextBox", ""),
				"Expected Soon");
		utils.logit("pass",
				"Api update customer status is successful. Customer status changed to " + dataSet.get("cus_status3"));

		// Update customer status to Arrived
		Response updateCustomerStatusResponse2 = pageObj.endpoints().ApiUpdateCustomerStatus(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("cus_status2"), punchh_orderid);
		Assert.assertEquals(updateCustomerStatusResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api  update customer status");
		utils.longWaitInSeconds(6); // wait for order status to be updated
		utils.refreshPage();
		pageObj.locationPage().orderSearch(orderId);
		Assert.assertEquals(
				pageObj.iframeConfigurationPage().getElementText("pickupConsolePage.customerstatusTextBox", ""),
				"Arrived");
		utils.logit("pass",
				"Api update customer status is successful. Customer status changed to " + dataSet.get("cus_status2"));

		// Update Order status to Delay from console
		pageObj.locationPage().OrderstatustoDelayfromConsole();
		utils.longWaitInSeconds(5); // wait for order status to be updated
		Assert.assertEquals(
				pageObj.iframeConfigurationPage().getElementText("pickupConsolePage.orderstatusTextBox", ""),
				"Delayed");

		// Update Order status to Ready from console
		pageObj.locationPage().OrderstatustoReadyfromConsole();
		utils.longWaitInSeconds(5); // wait for order status to be updated
		Assert.assertEquals(
				pageObj.iframeConfigurationPage().getElementText("pickupConsolePage.orderstatusTextBox", ""), "Ready");

		// Update Order status to Pickedup from console
		pageObj.locationPage().orderStatusToPickedUpFromConsole(orderId);
		utils.longWaitInSeconds(6); // wait for order status to be updated
		Assert.assertEquals(
				pageObj.iframeConfigurationPage().getElementText("pickupConsolePage.orderstatusTextBoxPastOrders", ""),
				"Picked Up");
		utils.logPass("Order is picked up and moved to past order screen");

		// pageObj.locationPage().VerifyHowToUseTutorial();

		// deleting the console key
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().selectLocationSearch2("");
		pageObj.locationPage().selectLocationConsoleList();
		pageObj.locationPage().deleteConsole(consoleName);

	}

	@DataProvider(name = "T6854_part2DataProvider")
	public Object[][] T6854_part2DataProvider() {
		return new Object[][] { { "delivery" }, { "pickup" } };
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}