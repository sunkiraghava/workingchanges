package Support;

import java.util.Properties;

import org.testng.Reporter;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

public class ConfigurationClass {

	public static String BROWSER;
	public static String HUB_HOST;
	public static String RUNTYPE;
	public static String environment;
	public static String baseUrl;
	public static String suite;
	private static Properties prop;

	// private static Properties frameworkProp;

	@BeforeSuite(alwaysRun = true)
	@Parameters({ "environment", "BROWSER", "HUB_HOST", "RUNTYPE", "baseUrl", "suite" })

	public void initFramework(@Optional("") final String environment, @Optional("") final String BROWSER,
			@Optional("") final String HUB_HOST, @Optional("") final String RUNTYPE, @Optional("") final String baseUrl,
			@Optional("") final String suite) {

		/*
		 * Utilities utils = new Utilities(); utils.flushSidekiq(environment);
		 */

		setFinalFrameworkProperties(environment, BROWSER, HUB_HOST, RUNTYPE, baseUrl, suite);
		printFrameworkConfigurations();
	}

	private void setFinalFrameworkProperties(final String environment, final String BROWSER, final String HUB_HOST,
			final String RUNTYPE, final String baseUrl, final String suite) {
		ConfigurationClass.environment = getValue("environment", environment);
		ConfigurationClass.BROWSER = getValue("BROWSER", BROWSER);
		ConfigurationClass.HUB_HOST = getValue("HUB_HOST", HUB_HOST);
		ConfigurationClass.RUNTYPE = getValue("RUNTYPE", RUNTYPE);
		ConfigurationClass.baseUrl = getValue("baseUrl", baseUrl);
		ConfigurationClass.suite = getValue("suite", suite);
	}

	private static String getValue(final String key, final String value) {
// Maven command line
		if (System.getProperty(key) != null) {
			return System.getProperty(key);
		}

// TestNG file
		if (!value.isEmpty()) {
			return value;
		}

// Value from config properties file for baseUrl
		if ("baseUrl".equalsIgnoreCase(key)) {
			if (baseUrl == null) {
				GetEnvDetails obj = new GetEnvDetails();
				return obj.setBaseUrl();
			}
		}
		return null;
	}

	private void printFrameworkConfigurations() {
		Reporter.log("------------------Automation Test started-------------------", true);
		Reporter.log("HUB_HOST: " + ConfigurationClass.HUB_HOST, true);
		Reporter.log("RUNTYPE: " + ConfigurationClass.RUNTYPE, true);
		Reporter.log("Environment: " + ConfigurationClass.environment, true);
		Reporter.log("baseUrl: " + ConfigurationClass.baseUrl, true);
		Reporter.log("Suite: " + ConfigurationClass.suite, true);
		Reporter.log("BROWSER: " + ConfigurationClass.BROWSER, true);
		Reporter.log("------------------------------------------------------------", true);
	}

}
