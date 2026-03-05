package com.punchh.server.Integration2;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.punchh.server.annotations.Owner;
import org.openqa.selenium.WebDriver;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class AdvancedAuthNegativeTest {
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private IntUtils intUtils;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	private Utilities utils; 

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv().toLowerCase();
		Utilities.loadPropertiesFile("config.properties");
		sTCName = method.getName();
		utils = new Utilities();
		intUtils = new IntUtils(driver);
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		// Merge datasets without overwriting existing keys
		pageObj.readData().readTestData.forEach(dataSet::putIfAbsent);
		utils.logit("Using env as ==> " + env);
		utils.logit(sTCName + " ==>" + dataSet);
	}

	@Test(description = "SQ-T6026_INT2-2048 | Guest Identity service | Block punchh generated emails to get otp",groups = {"nonNightly" })
	@Owner(name = "Nipun Jain")
	public void SQ_T6026_validate_blocking_of_punchh_generated_emails() throws Exception {
		
		String client = dataSet.get("client");
		String codeVerifier = utils.generateCodeVerifier(32);
		String codeChallenge = utils.generateCodeChallenge(codeVerifier);
		Response responseToken = null;

		String emailPrefix = "advanced_auth_auto_" + utils.getTimestampInNanoseconds();
		
		String[] blockedDomains = {
			"@fb.punchh.com",
			"@phone.punchh.com",
			"@archived.com",
			"@sa-phone.punchh.com",
			"@anonymise.punchh.com"
		};

		for (String domain : blockedDomains) {
			String email = emailPrefix + domain;
			responseToken = pageObj.endpoints().advancedAuthToken(client, "otp", email, null, null,
					codeChallenge, true, true);
			intUtils.verifyErrorResponse(responseToken, 422, "errors.invalid_email_format",
					"Invalid email format.", "domain");
		}
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		if (dataSet != null)
			pageObj.utils().clearDataSet(dataSet);
		utils.logit("Test Case: " + sTCName + " finished");
		driver.quit();
		utils.logit("Browser closed");
	}
}
