package com.punchh.server.utilities;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;

public class GoogleServiceAccountJwtGenerator {
	static Logger logger = LogManager.getLogger(GoogleServiceAccountJwtGenerator.class);
	private WebDriver driver;
	private Utilities utils;

	public GoogleServiceAccountJwtGenerator(WebDriver driver) {
		this.driver = driver;
		utils = new Utilities(driver);
	}

	public String generateIdToken(String targetAudience) throws IOException {
		// Get the credentials map
		Map<String, String> credsMap = gcpCredsJson();

		// Convert the map to JSON
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(credsMap);

		// Load credentials from the JSON string
		GoogleCredentials credentials = GoogleCredentials
				.fromStream(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

		if (!(credentials instanceof IdTokenProvider)) {
			throw new IllegalArgumentException("Provided credentials are not an ID token provider.");
		}

		// Build ID Token credentials
		IdTokenProvider idTokenProvider = (IdTokenProvider) credentials;
		IdTokenCredentials idTokenCredentials = IdTokenCredentials.newBuilder().setIdTokenProvider(idTokenProvider)
				.setTargetAudience(targetAudience).build();

		// Refresh and retrieve token
		idTokenCredentials.refresh();
		return idTokenCredentials.getAccessToken().getTokenValue();
	}

	public Map<String, String> gcpCredsJson() {
		Map<String, String> map = new HashMap<>();
		map.put("type", "service_account");
		map.put("project_id", "nifty-oxide-379506");
		map.put("client_email", "forautomationtest@nifty-oxide-379506.iam.gserviceaccount.com");
		map.put("client_id", "115635851869271980115");
		map.put("auth_uri", "https://accounts.google.com/o/oauth2/auth");
		map.put("token_uri", "https://oauth2.googleapis.com/token");
		map.put("auth_provider_x509_cert_url", "https://www.googleapis.com/oauth2/v1/certs");
		map.put("client_x509_cert_url",
				"https://www.googleapis.com/robot/v1/metadata/x509/forautomationtest%40nifty-oxide-379506.iam.gserviceaccount.com");
		map.put("universe_domain", "googleapis.com");
		String private_key = utils.decrypt(
				"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");
		map.put("private_key", private_key);
		String private_key_id = utils.decrypt("1pUFF3lpCiBJ8xYBj5h/NYT0a22hKxx9iLhjgZFWs+HctzXdNv04fRQx2kO+R4li");
		map.put("private_key_id", private_key_id);
		return map;
	}
}
