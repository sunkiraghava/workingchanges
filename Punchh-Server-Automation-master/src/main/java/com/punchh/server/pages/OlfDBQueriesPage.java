package com.punchh.server.pages;

import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.SingletonDBUtils;
import com.punchh.server.utilities.DBManager;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class OlfDBQueriesPage {
	static Logger logger = LogManager.getLogger(OlfDBQueriesPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	String ssoEmail, temp = "temp";

	public OlfDBQueriesPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(this.driver);
		selUtils = new SeleniumUtilities(driver);
	}

	public void updateDbFlags(String expColValue, String b_id, String env, String status, String flagName)
			throws Exception {

		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, status, flagName, b_id);
		Assert.assertTrue(flag, flagName + " value is not updated to " + status);
		logger.info(flagName + " value is updated to " + status);
		TestListeners.extentTest.get().info(flagName + " value is updated to " + status);

	}

}
