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
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.TestListeners;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class LocationStateCountryTest {
	static Logger logger = LogManager.getLogger(LocationStateCountryTest.class);
	public WebDriver driver;
	ApiUtils apiUtils;
	Properties prop;
	PageObj pageObj;
    String sTCName;
	String run = "api";
	private String env;
	private static Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void beforeMethod(Method method) {
			apiUtils = new ApiUtils();
			pageObj = new PageObj(driver);
			env = pageObj.getEnvDetails().setEnv();
			dataSet = new ConcurrentHashMap<>();
			sTCName = method.getName();
			pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
			dataSet = pageObj.readData().readTestData;
			logger.info(sTCName + " ==>" + dataSet);
	}
	
	// divya choudhary	
	@Owner(name = "Divya Choudhary")
	@Test(description = "SQ-T7190 Verify api2/dashboard/state_codes API returns error with valid authentication", groups = "api", priority = 7)
	public void SQ_T7190_StateCodesApiValidAdmin() {
		// api is functional with valid admin authorization
		Response getStateCodesresponse = pageObj.endpoints().getStateCodes(dataSet.get("adminAuthorization"));
		Assert.assertEquals(getStateCodesresponse.getStatusCode(), 200,
			     "Status code 200 matched for dashboard api2 state codes with valid admin authorization");
		TestListeners.extentTest.get()
			        .pass("Dashboard Api2 is successful with valid admin authorization.");
		logger.info("Verified state codes api with valid admin authorization");
		TestListeners.extentTest.get().pass("Dashboard api2/state_codes API is successful with valid admin authorization.");
	}	    
	  
	@Owner(name = "Divya Choudhary")
	@Test(description = "SQ-T7191 Verify api2/dashboard/state_codes API returns error with invalid authentication", groups = "api", priority = 7)
	public void SQ_T7191_StateCodesApiInvalidAdmin() {
	    // api is not functional with invalid admin authorization
		Response getStateCodesresponse = pageObj.endpoints().getStateCodes(dataSet.get("adminAuthorization"));
	    Assert.assertEquals(getStateCodesresponse.getStatusCode(), 401,
	       "Status code 401 matched for dashboard api2 state codes with invalid admin authorization");
	    TestListeners.extentTest.get()
	          .pass("Dashboard Api2 is not successful with invalid admin authorization.");
	    logger.info("Verified state codes api with invalid admin authorization");
	    TestListeners.extentTest.get().pass("Dashboard api2/state_codes API is unsuccessful with invalid admin authorization.");
	}	

	@AfterMethod(alwaysRun = true)
	public void afterMethod() {
		pageObj.utils().clearDataSet(dataSet);
	}
}