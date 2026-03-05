package com.punchh.server.apiConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.testng.annotations.Listeners;

import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class PosHeaderConfig {

	ApiUtils apiUtils;
	Properties prop;

	public PosHeaderConfig() {
		apiUtils = new ApiUtils();
		prop = Utilities.loadPropertiesFile("apiConfig.properties");
	}

	public Map<String, Object> api2LoginHeader(String payload, String secret) {
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			String signature = apiUtils.getSignature(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			defaultHeaderMap.put("Accept-Language", prop.getProperty("acceptLanguage"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return defaultHeaderMap;
	}
}
