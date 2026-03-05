package com.punchh.server.apiConfig;

import java.util.Properties;

import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.Utilities;

public class AuthHeaders {
	ApiUtils apiUtils;
	Properties prop;

	public AuthHeaders() {
		apiUtils = new ApiUtils();
		prop = Utilities.loadPropertiesFile("apiConfig.properties");
	}

	public String authApiSignature(String payload, String secret) {
		String signature = apiUtils.getSignatureSHA1(secret, payload);
		return signature;
	}

	public String authApiContentType() {
		String contentType = prop.getProperty("contentType");
		return contentType;
	}

	public String authApiAcceptLanguage() {
		String acceptLanguage = prop.getProperty("acceptLanguage");
		return acceptLanguage;
	}

	public String authApiAccept() {
		String accept = prop.getProperty("contentType");
		return accept;
	}

}
