package com.punchh.server.apiConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.testng.annotations.Listeners;

import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class HeaderConfig {

	ApiUtils apiUtils;
	Properties prop;

	public HeaderConfig() {
		apiUtils = new ApiUtils();
		prop = Utilities.loadPropertiesFile("apiConfig.properties");
	}

	//Default without auth digest
	public Map<String, Object> api2LoginHeader(String payload, String secret) {
		return api2LoginHeader(payload, secret, "");
	}

	//api 2 login header with authdigest
	public Map<String, Object> api2LoginHeader(String payload, String secret, String authDigest) {
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			String signature = apiUtils.getSignature(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			defaultHeaderMap.put("Accept-Language", prop.getProperty("acceptLanguage"));
			if (authDigest != null && !authDigest.isEmpty()) {
				defaultHeaderMap.put("auth-digest", authDigest);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return defaultHeaderMap;
	}

	public Map<String, Object> api2SignUpHeader(String payload, String secret) {
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			String signature = apiUtils.getSignature(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			defaultHeaderMap.put("Accept-Language", prop.getProperty("acceptLanguage"));
			defaultHeaderMap.put("Accept-Timezone", prop.getProperty("acceptTimezone"));
			defaultHeaderMap.put("User-Agent", prop.getProperty("userAgent2"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return defaultHeaderMap;
	}
	
	public Map<String, Object> authAPISignUpHeader(String payload, String secret)
	{
		return authAPISignUpHeader(payload, secret, "");
	}

	public Map<String, Object> authAPISignUpHeader(String payload, String secret, String authDigest) {
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("Accept", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			defaultHeaderMap.put("Accept-Language", prop.getProperty("acceptLanguage"));
			// defaultHeaderMap.put("Accept-Timezone", prop.getProperty("acceptTimezone"));
			if (authDigest != null && !authDigest.isEmpty()) {
				defaultHeaderMap.put("auth-digest", authDigest);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return defaultHeaderMap;
	}

	public Map<String, Object> defaultHeaders() {
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
		defaultHeaderMap.put("Accept", prop.getProperty("contentType"));
		return defaultHeaderMap;
	}

	public Map<String, Object> api2PurchaseSubscriptionsHeaders(String payload) {
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			String signature = apiUtils.getSignature(prop.getProperty("secret"), payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			defaultHeaderMap.put("Accept-Language", prop.getProperty("acceptLanguage"));
			defaultHeaderMap.put("Accept", prop.getProperty("contentType"));
			// defaultHeaderMap.put("User-Agent", prop.getProperty("userAgent2"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return defaultHeaderMap;
	}

	public Map<String, Object> api1SignUpHeader(String payload, String epoch, String secret) {
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			String signature = apiUtils.getSignature(secret, payload);
			String hash = apiUtils.getHash(secret + epoch);
			defaultHeaderMap.put("Cache-Control", prop.getProperty("cacheControl"));
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			defaultHeaderMap.put("x-pch-hash", hash);
			defaultHeaderMap.put("Accept-Language", prop.getProperty("acceptLanguage"));
			defaultHeaderMap.put("Accept-Timezone", prop.getProperty("acceptTimezone"));
			defaultHeaderMap.put("Postman-Token", prop.getProperty("postManTokenSignUp"));
			defaultHeaderMap.put("User-Agent", prop.getProperty("userAgent1"));
			defaultHeaderMap.put("app_build", prop.getProperty("appBuild"));
			defaultHeaderMap.put("app_os", prop.getProperty("appOs"));
			/*
			 * defaultHeaderMap.put("app_model", prop.getProperty("appModel"));
			 * defaultHeaderMap.put("punchh-app-device-id",
			 * prop.getProperty("appDeviceId"));
			 */
		} catch (Exception e) {
			e.printStackTrace();
		}
		return defaultHeaderMap;
	}

	//Default without auth digest
	public Map<String, Object> api1LoginHeader(String payload, String epoch, String secret) 
	{
		return api1LoginHeader(payload, epoch, secret, "");
	}

	public Map<String, Object> api1LoginHeader(String payload, String epoch, String secret, String authDigest) {
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			String signature = apiUtils.getSignature(secret, payload);
			String hash = apiUtils.getHash(secret + epoch);

			defaultHeaderMap.put("Cache-Control", prop.getProperty("cacheControl"));
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			defaultHeaderMap.put("x-pch-hash", hash);
			defaultHeaderMap.put("Accept-Language", prop.getProperty("acceptLanguage"));
			defaultHeaderMap.put("Accept-Timezone", prop.getProperty("acceptTimezone"));
			defaultHeaderMap.put("Postman-Token", prop.getProperty("postManTokcenLogin"));
			defaultHeaderMap.put("User-Agent", prop.getProperty("userAgent1"));
			defaultHeaderMap.put("app_build", prop.getProperty("appBuild"));
			defaultHeaderMap.put("app_model", prop.getProperty("appModel"));
			defaultHeaderMap.put("app_os", prop.getProperty("appOs"));
			defaultHeaderMap.put("punchh-app-device-id", prop.getProperty("appDeviceId"));
			if (authDigest != null && !authDigest.isEmpty()) {
				defaultHeaderMap.put("auth-digest", authDigest);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return defaultHeaderMap;
	}

	public Map<String, Object> onlineOrderCheckinHeader(String payload, String secret) {
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Accept-Timezone", "Etc/UTC");
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return defaultHeaderMap;
	}

	public Map<String, Object> userOffersHeader(String payload, String secret, String token) {
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			String signature = apiUtils.getSignature(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			defaultHeaderMap.put("Accept-Language", prop.getProperty("acceptLanguage"));
			defaultHeaderMap.put("Accept", prop.getProperty("contentType"));
			defaultHeaderMap.put("Authorization", "Bearer " + token);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return defaultHeaderMap;
	}

	public Map<String, Object> deactivateUserHeader(String payload, String epoch, String secret, String authToken) {
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			String signature = apiUtils.getSignature(secret, payload);
			String hash = apiUtils.getHash(secret + epoch);
			defaultHeaderMap.put("Content-Type", "application/json");
			defaultHeaderMap.put("x-pch-digest", signature);
			defaultHeaderMap.put("x-pch-hash", hash);
			defaultHeaderMap.put("Accept", "application/json");
			// defaultHeaderMap.put("Cache-Control", prop.getProperty("cacheControl"));
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			defaultHeaderMap.put("x-pch-hash", hash);
			// defaultHeaderMap.put("Accept-Language", prop.getProperty("acceptLanguage"));
			defaultHeaderMap.put("Accept", prop.getProperty("contentType"));
			// defaultHeaderMap.put("Postman-Token",
			// prop.getProperty("postManTokenSignUp"));
			defaultHeaderMap.put("User-Agent", prop.getProperty("userAgent1"));
			defaultHeaderMap.put("app_build", prop.getProperty("appBuild"));
			defaultHeaderMap.put("app_os", prop.getProperty("appOs"));
			// defaultHeaderMap.put("app_model", prop.getProperty("appModel"));
			// defaultHeaderMap.put("punchh-app-device-id",
			// prop.getProperty("appDeviceId"));
			defaultHeaderMap.put("Authorization", "Bearer " + authToken);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return defaultHeaderMap;
	}

	public Map<String, Object> deleteUserHeader(String payload, String epoch, String secret, String authToken) {
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			String signature = apiUtils.getSignature(secret, payload);
			String hash = apiUtils.getHash(secret + epoch);
//			defaultHeaderMap.put("Cache-Control", prop.getProperty("cacheControl"));
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			defaultHeaderMap.put("x-pch-hash", hash);
			// defaultHeaderMap.put("Accept-Language", prop.getProperty("acceptLanguage"));
			defaultHeaderMap.put("Accept", prop.getProperty("contentType"));
			// defaultHeaderMap.put("Postman-Token",
			// prop.getProperty("postManTokenSignUp"));
			defaultHeaderMap.put("User-Agent", prop.getProperty("userAgent1"));
			defaultHeaderMap.put("app_build", prop.getProperty("appBuild"));
			defaultHeaderMap.put("app_os", prop.getProperty("appOs"));
//			defaultHeaderMap.put("app_model", prop.getProperty("appModel"));
//			defaultHeaderMap.put("punchh-app-device-id", prop.getProperty("appDeviceId"));
			defaultHeaderMap.put("Authorization", "Bearer " + authToken);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return defaultHeaderMap;
	}
  
}
