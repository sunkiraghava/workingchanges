package Support;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class GetEnvDetails {
	static Logger logger = LogManager.getLogger(GetEnvDetails.class);
	public WebDriver driver;
	PageObj pageObj = new PageObj(driver);
	private static String env;
	private String baseUrl;
	private Properties prop;

	public GetEnvDetails() {
		prop = Utilities.loadPropertiesFile("config.properties");
	}

	public String setEnv() {
		env = ConfigurationClass.environment;
		if (env == null) {
			env = "pp";
			// env = "shashank_local";

		}
		return env;
	}

	public String setBaseUrl() {
		String env = setEnv().toLowerCase();

		// String environment = "";
		String instance = "";

		// Split the environment into environment and instance if needed
		if (env.contains("_")) {
			String[] parts = env.split("_");
			instance = parts[0]; // This should be the environment (e.g., "local")
			// environment = parts[1];
			// This should be the instance URL.
			// logger.info("Instance is : " + instance);
		} else {
			instance = env.toLowerCase(); // If no underscore, treat the entire value as the environment(e.g., "pp")
		}

		logger.info("Environment is : " + instance);
		baseUrl = prop.getProperty(instance + ".baseurl");
		Assert.assertNotNull(baseUrl, "Base url did not found in config file for instance: " + instance);
		logger.info("Base URL is : " + baseUrl);
		return baseUrl;
	}
}