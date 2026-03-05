package com.punchh.server.apiConfig;

import java.util.Properties;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.Utilities;

public class Api2Headers {

	ApiUtils apiUtils;
	Properties prop;

	public Api2Headers() {
		apiUtils = new ApiUtils();
		prop = Utilities.loadPropertiesFile("apiConfig.properties");
	}

	public String api2Signature(String payload, String secret) {
		String signature = apiUtils.getSignature(secret, payload);
		return signature;
	}

	public String api2ContentType() {
		String contentType = prop.getProperty("contentType");
		return contentType;
	}

	public String api2acceptLanguage() {
		String acceptLanguage = prop.getProperty("acceptLanguage");
		return acceptLanguage;
	}

	public String api2userAgent() {
		String userAgent = prop.getProperty("userAgent2");
		return userAgent;
	}

	public String api2appDeviceId() {
		String appDeviceId = prop.getProperty("appDeviceId");
		return appDeviceId;
	}

	public String api2Accept() {
		String accept = prop.getProperty("contentType");
		return accept;
	}

	public String api2acceptTimezone() {
		String acceptTimezone = prop.getProperty("acceptTimezone");
		return acceptTimezone;
	}
}
