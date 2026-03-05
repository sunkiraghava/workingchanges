package com.punchh.server.apiConfig;

import java.util.Properties;

import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.Utilities;

public class API1Headers {

	ApiUtils apiUtils;
	Properties prop;

	public API1Headers() {
		apiUtils = new ApiUtils();
		prop = Utilities.loadPropertiesFile("apiConfig.properties");
	}

	public String signature(String payload, String secret) {
		String signature = apiUtils.getSignature(secret, payload);
		return signature;
	}

	public String hash(String secret, String epoch) {
		String hash = apiUtils.getHash(secret + epoch);
		return hash;
	}

	public String contentType() {
		String contentType = prop.getProperty("contentType");
		return contentType;
	}

	public String acceptLanguage() {
		String acceptLanguage = prop.getProperty("acceptLanguage");
		return acceptLanguage;
	}

	public String cacheControl() {
		String cacheControl = prop.getProperty("cacheControl");
		return cacheControl;
	}

	public String acceptTimeZone() {
		String acceptTimeZone = prop.getProperty("acceptTimezone");
		return acceptTimeZone;
	}

	public String postmanToken() {
		String postmanToken = prop.getProperty("postManTokcenLogin");
		return postmanToken;
	}

	public String userAgent() {
		String userAgent = prop.getProperty("userAgent1");
		return userAgent;
	}

	public String appBuild() {
		String app_build = prop.getProperty("appBuild");
		return app_build;
	}

	public String appModel() {
		String appModel = prop.getProperty("appModel");
		return appModel;
	}

	public String appOs() {
		String appOs = prop.getProperty("appOs");
		return appOs;
	}

	public String appDeviceId() {
		String appDeviceId = prop.getProperty("appDeviceId");
		return appDeviceId;
	}

	public String accept() {
		String accept = prop.getProperty("contentType");
		return accept;
	}

}
