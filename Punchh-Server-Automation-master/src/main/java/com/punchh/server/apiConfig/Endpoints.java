package com.punchh.server.apiConfig;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.Listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.punchh.server.LisCreationUtilityClasses.BaseItemClauses;
import com.punchh.server.LisCreationUtilityClasses.GenerateJson;
import com.punchh.server.LisCreationUtilityClasses.ModifiersItemsClauses;
import com.punchh.server.pages.FetchCard;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

@Listeners(TestListeners.class)
public class Endpoints {
	static Logger logger = LogManager.getLogger(Endpoints.class);
	// public WebDriver driver;
	// PageObj pageObj = new PageObj(driver);
	PageObj pageObj = new PageObj();
	ApiUtils apiUtils;
	Properties prop;
	Properties configProp;
	HeaderConfig header;
	Api2Headers api2Headers;
	AuthHeaders authHeaders;
	API1Headers api1Headers;
	JSONObject requestParams;
	String param;
	// String phoneNumber, email;
	Utilities utils;
	public String punchhKey, amount;
	ApiPayloads apipaylods;
	public String transactionNumber, externalUid;
	String str1, str4;
	String str2 = "";
	String startTimepara = "\"start_time\": \"none\",";
	String redeemableUuid = "\"redeemable_uuid\": \"none\",";
	String segment_Id = "\"segment_id\": none,";
	String massCampaign = "\"campaign_type\": \"none\",";
	String categoryTemp = "\"category\": \"none\",";
	public String baseUri = getBaseUri();
	String env = pageObj.getEnvDetails().setEnv();
	String cloudFareToken = null;
	String cloudFareBypass = null;
	public String offerModularizationBaseUrl;

	public String getBaseUri() {
		baseUri = pageObj.getEnvDetails().setBaseUrl();
		TestListeners.extentTest.get().info("Using base URI as ==> " + baseUri);
		return baseUri;
	}

	public String getMobileApiBaseUrl() {
		String mobileApiBaseUrl = "";
		if (env.equalsIgnoreCase("qa") || env.equalsIgnoreCase("pp")) {
			mobileApiBaseUrl = Utilities.getConfigProperty(env, "mobileApiBaseUrl");
		} else {
			mobileApiBaseUrl = getBaseUri();
		}
		return mobileApiBaseUrl;
	}

	public String getGuestIdentityBaseUrl() {
		String guestIdentityURL = "";
		try {
			guestIdentityURL = Utilities.getConfigProperty(env, "guestIdentityBaseUrl");
			utils.logit("Using guest identity base URL as ==> " + guestIdentityURL);
		} catch (Exception e) {
			utils.logit("Fail", e.getMessage());
		}
		return guestIdentityURL;
	}

	public Endpoints() {
		apiUtils = new ApiUtils();
		prop = Utilities.loadPropertiesFile("apiConfig.properties");
		configProp = Utilities.loadPropertiesFile("config.properties");
		header = new HeaderConfig();
		utils = new Utilities();
		apipaylods = new ApiPayloads();
		authHeaders = new AuthHeaders();
		api2Headers = new Api2Headers();
		api1Headers = new API1Headers();
		authHeaders = new AuthHeaders();
		cloudFareToken = utils.decrypt(Utilities.getApiConfigProperty("cloudFareToken"));
		cloudFareBypass = utils.decrypt(Utilities.getApiConfigProperty("cloudFareBypass"));
	}

	public Response Api2Login(String email, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile Api2 SignIn======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("email", email);
			hashmap.put("password", Utilities.getApiConfigProperty("password"));

			requestParams.put("user", hashmap);
			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.mobApi2Login + param;
			request.headers(header.api2LoginHeader(payload, secret));
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.mobApi2Login);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API Response: " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// API2 login with or with out password , via email or phone, with and without
	// auth-digest
	public Response Api2Login(String identifier, String client, String secret, Boolean usePassword, String authDigest) {
		Response response = null;
		requestParams = new JSONObject();
		utils.logit("======Mobile Api2 SignIn======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			// Identify if input is email or phone
			if (identifier.contains("@")) {
				hashmap.put("email", identifier);
			} else {
				hashmap.put("phone", identifier);
			}
			// Use password conditionally
			if (usePassword) {
				hashmap.put("password", Utilities.getApiConfigProperty("password"));
			}
			requestParams.put("user", hashmap);
			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.mobApi2Login + param;
			// Add auth-digest if provided
			request.headers(header.api2LoginHeader(payload, secret, authDigest));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.mobApi2Login);
			utils.logit("API Response:" + response.asString());
		} catch (Exception e) {
			utils.logit("FAIL", e.getMessage());
		}
		return response;
	}

	// API2 login with verification mode or firebase token
	public Response Api2Login(String validate_mode_or_token, String mode_or_token, String email, String client,
			String secret) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile Api2 SignIn======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("email", email);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			hashmap.put("password", Utilities.getApiConfigProperty("password"));
			if (validate_mode_or_token.equals(mode_or_token))
				hashmap.put("verification_mode", "abc"); // it could be any value
			else
				hashmap.put("firebase_token", "aaa"); // it could be any value
			requestParams.put("user", hashmap);
			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.mobApi2Login + param;
			request.headers(header.api2LoginHeader(payload, secret));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.mobApi2Login);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API Response: " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2SignUp(String email, String client, String secret, String... inviteCode) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile Api2 SignUp======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			// String phoneNumber =
			// CreateDateTime.getUniqueString(prop.getProperty("countryCode"));
			hashmap.put("email", email);
			hashmap.put("password", prop.getProperty("password"));
			// hashmap.put("phone", phoneNumber);
			hashmap.put("first_name", CreateDateTime.getUniqueString(prop.getProperty("firstName")));
			hashmap.put("last_name", CreateDateTime.getUniqueString(prop.getProperty("lastName")));
			hashmap.put("zip_code", prop.getProperty("zipCode"));
			hashmap.put("marketing_email_subscription", "true");
			hashmap.put("signup_channel", prop.getProperty("signUpChannel"));
			hashmap.put("privacy_policy", prop.getProperty("privatePolicy"));
			hashmap.put("birthday", prop.getProperty("birthday"));
			hashmap.put("gender", "Male");
			if (inviteCode.length > 0 && !inviteCode[0].isEmpty()) {
				hashmap.put("invite_code", inviteCode[0]);
			}
			// logger.info("Email in method for API2 ==> " + hashmap);
			requestParams.put("user", hashmap);
			requestParams.put("client", client);
			param = requestParams.toString();
			// logger.info("Email in method for API2 ==> " + param);
			String payload = ApiConstants.mobApi2SignUp + param;
			request.headers(header.api2SignUpHeader(payload, secret));
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.mobApi2SignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
			TestListeners.extentTest.get().info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2SignUpwithAnniversary(String email, String client, String secret, String anniversary) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile Api2 SignUp======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			// String phoneNumber =
			// CreateDateTime.getUniqueString(prop.getProperty("countryCode"));
			hashmap.put("email", email);
			hashmap.put("password", prop.getProperty("password"));
			// hashmap.put("phone", phoneNumber);
			hashmap.put("first_name", CreateDateTime.getUniqueString(prop.getProperty("firstName")));
			hashmap.put("last_name", CreateDateTime.getUniqueString(prop.getProperty("lastName")));
			hashmap.put("zip_code", prop.getProperty("zipCode"));
			hashmap.put("marketing_email_subscription", "true");
			hashmap.put("signup_channel", prop.getProperty("signUpChannel"));
			hashmap.put("privacy_policy", prop.getProperty("privatePolicy"));
			hashmap.put("birthday", prop.getProperty("birthday"));
			hashmap.put("gender", "Male");
			hashmap.put("anniversary", anniversary);
			// logger.info("Email in method for API2 ==> " + hashmap);
			requestParams.put("user", hashmap);
			requestParams.put("client", client);
			param = requestParams.toString();
			// logger.info("Email in method for API2 ==> " + param);
			String payload = ApiConstants.mobApi2SignUp + param;
			request.headers(header.api2SignUpHeader(payload, secret));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.mobApi2SignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2SignUpwithFavLocation(String email, String client, String secret, String favLocation) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile Api2 SignUp======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			// String phoneNumber =
			// CreateDateTime.getUniqueString(prop.getProperty("countryCode"));
			hashmap.put("email", email);
			hashmap.put("password", prop.getProperty("password"));
			// hashmap.put("phone", phoneNumber);
			hashmap.put("first_name", CreateDateTime.getUniqueString(prop.getProperty("firstName")));
			hashmap.put("last_name", CreateDateTime.getUniqueString(prop.getProperty("lastName")));
			hashmap.put("zip_code", prop.getProperty("zipCode"));
			hashmap.put("marketing_email_subscription", "true");
			hashmap.put("signup_channel", prop.getProperty("signUpChannel"));
			hashmap.put("privacy_policy", prop.getProperty("privatePolicy"));
			hashmap.put("birthday", prop.getProperty("birthday"));
			hashmap.put("gender", "Male");
			hashmap.put("favourite_location_ids", favLocation);
			// logger.info("Email in method for API2 ==> " + hashmap);
			requestParams.put("user", hashmap);
			requestParams.put("client", client);
			param = requestParams.toString();
			// logger.info("Email in method for API2 ==> " + param);
			String payload = ApiConstants.mobApi2SignUp + param;
			request.headers(header.api2SignUpHeader(payload, secret));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.mobApi2SignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2SignUp(String email, String client, String secret, long phoneNumber) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile Api2 SignUp======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			// String phoneNumber =
			// CreateDateTime.getUniqueString(prop.getProperty("countryCode"));
			hashmap.put("email", email);
			hashmap.put("password", prop.getProperty("password"));
			hashmap.put("phone", phoneNumber);
			hashmap.put("first_name", CreateDateTime.getUniqueString(prop.getProperty("firstName")));
			hashmap.put("last_name", CreateDateTime.getUniqueString(prop.getProperty("lastName")));
			hashmap.put("zip_code", prop.getProperty("zipCode"));
			hashmap.put("marketing_email_subscription", "true");
			hashmap.put("signup_channel", prop.getProperty("signUpChannel"));
			hashmap.put("privacy_policy", prop.getProperty("privatePolicy"));
			hashmap.put("birthday", prop.getProperty("birthday"));
			hashmap.put("gender", "Male");
			// logger.info("Email in method for API2 ==> " + hashmap);
			requestParams.put("user", hashmap);
			requestParams.put("client", client);
			param = requestParams.toString();
			// logger.info("Email in method for API2 ==> " + param);
			String payload = ApiConstants.mobApi2SignUp + param;
			request.headers(header.api2SignUpHeader(payload, secret));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.mobApi2SignUp);

			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2SignUpWithLoyaltyCard(String email, String client, String secret, String loyaltyCardNumber) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile Api2 SignUp======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("email", email);
			hashmap.put("password", prop.getProperty("password"));
			hashmap.put("card_number", loyaltyCardNumber);
			hashmap.put("first_name", CreateDateTime.getUniqueString(prop.getProperty("firstName")));
			hashmap.put("last_name", CreateDateTime.getUniqueString(prop.getProperty("lastName")));
			hashmap.put("zip_code", prop.getProperty("zipCode"));
			hashmap.put("marketing_email_subscription", "true");
			hashmap.put("signup_channel", prop.getProperty("signUpChannel"));
			hashmap.put("privacy_policy", prop.getProperty("privatePolicy"));
			hashmap.put("birthday", prop.getProperty("birthday"));
			hashmap.put("gender", "Male");
			requestParams.put("user", hashmap);
			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.mobApi2SignUp + param;
			request.headers(header.api2SignUpHeader(payload, secret));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.mobApi2SignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1UserSignUp(String email, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		String external_source_id = "qwertyuirtyu" + CreateDateTime.getUniqueString("");
		try {
			logger.info("======API1 User SignUp======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("email", email);
			hashmap.put("password", Utilities.getApiConfigProperty("password"));
			hashmap.put("first_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("firstName")));
			hashmap.put("last_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("lastName")));
			hashmap.put("zip_code", Utilities.getApiConfigProperty("zipCode"));
			hashmap.put("signup_channel", Utilities.getApiConfigProperty("signUpChannel"));
			hashmap.put("referral_code", Utilities.getApiConfigProperty("referralCode"));
			hashmap.put("birthday", Utilities.getApiConfigProperty("birthday"));
			hashmap.put("privacy_policy", "true");
			hashmap.put("external_source", "salesforce");
			hashmap.put("external_source_id", external_source_id);
			requestParams.put("user", hashmap);
			param = requestParams.toString();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1SignUp + "?client=" + client + "&hash=" + epoch;
			String payload = URL + param;
			request.headers(header.api1SignUpHeader(payload, epoch, secret));
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.post(ApiConstants.mobApi1SignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1UserSignUp(String email, String client, String secret, String phoneNumber) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======API1 User SignUp======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("email", email);
			hashmap.put("password", Utilities.getApiConfigProperty("password"));
			hashmap.put("first_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("firstName")));
			hashmap.put("last_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("lastName")));
			hashmap.put("zip_code", Utilities.getApiConfigProperty("zipCode"));
			hashmap.put("signup_channel", Utilities.getApiConfigProperty("signUpChannel"));
			hashmap.put("referral_code", Utilities.getApiConfigProperty("referralCode"));
			hashmap.put("birthday", Utilities.getApiConfigProperty("birthday"));
			hashmap.put("phone", phoneNumber);
			hashmap.put("privacy_policy", "true");
			requestParams.put("user", hashmap);
			param = requestParams.toString();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1SignUp + "?client=" + client + "&hash=" + epoch;
			String payload = URL + param;
			request.headers(header.api1SignUpHeader(payload, epoch, secret));
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.post(ApiConstants.mobApi1SignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1UserLogin(String email, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("email", email);
			hashmap.put("password", Utilities.getApiConfigProperty("password"));
			requestParams.put("user", hashmap);
			param = requestParams.toString();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1Login + "?client=" + client + "&hash=" + epoch;
			String payload = URL + param;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.post(ApiConstants.mobApi1Login);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// API1 login with or with out password , via email or phone, with and without
	// auth-digest
	public Response Api1UserLogin(String identifier, String client, String secret, Boolean usePassword,
			String authDigest) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();

			// Identify if input is email or phone
			if (identifier.contains("@")) {
				hashmap.put("email", identifier);
			} else {
				hashmap.put("phone", identifier);
			}

			if (usePassword) {
				hashmap.put("password", Utilities.getApiConfigProperty("password"));
			}
			requestParams.put("user", hashmap);
			param = requestParams.toString();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1Login + "?client=" + client + "&hash=" + epoch;
			String payload = URL + param;
			request.headers(header.api1LoginHeader(payload, epoch, secret, authDigest));
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.post(ApiConstants.mobApi1Login);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posSignUp(String email, String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("*****POS Signup API*****");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("email", email);
			requestParams.put("password", Utilities.getApiConfigProperty("password"));
			requestParams.put("first_name",
					CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("firstName")));
			requestParams.put("last_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("lastName")));
			requestParams.put("zip_code", Utilities.getApiConfigProperty("zipCode"));
			request.queryParam("location_key", locationKey);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.posSignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posSignUp(String email, String locationKey, String favLocationdId) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("*****POS Signup API*****");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("email", email);
			requestParams.put("password", Utilities.getApiConfigProperty("password"));
			requestParams.put("first_name",
					CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("firstName")));
			requestParams.put("last_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("lastName")));
			requestParams.put("zip_code", Utilities.getApiConfigProperty("zipCode"));
			requestParams.put("favourite_locations", favLocationdId);
			request.queryParam("location_key", locationKey);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.posSignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posSignUpwithAnniversary(String email, String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		Date now = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String time = dateFormat.format(now);
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("email", email);
			requestParams.put("password", Utilities.getApiConfigProperty("password"));
			requestParams.put("first_name",
					CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("firstName")));
			requestParams.put("last_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("lastName")));
			requestParams.put("zip_code", Utilities.getApiConfigProperty("zipCode"));
			requestParams.put("anniversary", time);
			request.queryParam("location_key", locationKey);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.posSignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posSignUpwithBirthday(String email, String locationKey, String birthday) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("email", email);
			requestParams.put("password", Utilities.getApiConfigProperty("password"));
			requestParams.put("first_name",
					CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("firstName")));
			requestParams.put("last_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("lastName")));
			requestParams.put("zip_code", Utilities.getApiConfigProperty("zipCode"));
			requestParams.put("birthday", birthday);
			request.queryParam("location_key", locationKey);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.posSignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posSignUpwithAdvanceAnniversary(String email, String locationKey, String anniversary) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("email", email);
			requestParams.put("password", Utilities.getApiConfigProperty("password"));
			requestParams.put("first_name",
					CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("firstName")));
			requestParams.put("last_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("lastName")));
			requestParams.put("zip_code", Utilities.getApiConfigProperty("zipCode"));
			requestParams.put("anniversary", anniversary);
			request.queryParam("location_key", locationKey);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.posSignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posSignUpwithAnniversary(String email, String locationKey, String anniversary) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("email", email);
			requestParams.put("password", Utilities.getApiConfigProperty("password"));
			requestParams.put("first_name",
					CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("firstName")));
			requestParams.put("last_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("lastName")));
			requestParams.put("zip_code", Utilities.getApiConfigProperty("zipCode"));
			requestParams.put("anniversary", anniversary);
			request.queryParam("location_key", locationKey);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.posSignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posSignUpwithSpouseBirthday(String email, String locationKey, String birthday) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("email", email);
			requestParams.put("password", Utilities.getApiConfigProperty("password"));
			requestParams.put("first_name",
					CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("firstName")));
			requestParams.put("last_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("lastName")));
			requestParams.put("zip_code", Utilities.getApiConfigProperty("zipCode"));
			requestParams.put("spouse_birthday", birthday);
			request.queryParam("location_key", locationKey);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.posSignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posSignUpWithFavouriteLocation(String email, String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("email", email);
			requestParams.put("password", Utilities.getApiConfigProperty("password"));
			requestParams.put("first_name",
					CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("firstName")));
			requestParams.put("last_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("lastName")));
			requestParams.put("favourite_locations", "314366");
			request.queryParam("location_key", locationKey);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.posSignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response onlineOrderCheckin(String authenticationToken, String amount, String client, String secret) {
		Response response = null;
		try {
			transactionNumber = "123456" + CreateDateTime.getTimeDateString();
			externalUid = "TestUid" + CreateDateTime.getTimeDateString();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = ApiPayloads.onlineCheckinPayLoad(authenticationToken, amount, client, transactionNumber,
					externalUid);
			String payload = ApiConstants.onlineOrderCheckin + body;
			request.headers(header.onlineOrderCheckinHeader(payload.toString(), secret));
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.onlineOrderCheckin);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authUpdateLoyaltyCheckin(String authToken, String client, String secret, String externalUid,
			String state) {
		Response response = null;
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			transactionNumber = "123456" + CreateDateTime.getTimeDateString();
			String body = apipaylods.authUpdateLoyaltyCheckinPayload(authToken, client, externalUid, state);
			String payload = ApiConstants.onlineOrderCheckin + body;
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			request.header("Content-Type", authHeaders.authApiContentType());
			request.body(body);
			response = request.put(ApiConstants.onlineOrderCheckin);
			logger.info("API Response: " + response.asString());
			TestListeners.extentTest.get().info("API response: " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authOnlineOrderCheckin(String authenticationToken, String amount, String client, String secret,
			String txn, String externalUid, String date) {
		Response response = null;
		try {

			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = ApiPayloads.authOnlineCheckinPayLoad(authenticationToken, amount, client, txn, externalUid,
					date);

			String payload = ApiConstants.onlineOrderCheckin + body;
			request.headers(header.onlineOrderCheckinHeader(payload.toString(), secret));
			request.body(body);
			response = request.post(ApiConstants.onlineOrderCheckin);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authOnlineVoidCheckin(String authenticationToken, String client, String secret,
			String externalUid) {
		Response response = null;
		try {

			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.authOnlineVoidCheckin(authenticationToken, client, externalUid);

			String payload = ApiConstants.onlineOrderVoidCheckin + body;
			request.headers(header.onlineOrderCheckinHeader(payload.toString(), secret));
			request.body(body);
			response = request.delete(ApiConstants.onlineOrderVoidCheckin);
			TestListeners.extentTest.get().info(response.statusCode() + "; " + response.asString());
			logger.info(response.statusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// public Response voidOnlineOrderCheckin(String authenticationToken, String
	// amount) {
	// Response response = null;
	// try {
	// RestAssured.baseURI = baseUri;
	// RequestSpecification request = RestAssured.given().log().all();
	// String body = apipaylods.onlineCheckinPayLoad(authenticationToken, amount);
	// String payload = ApiConstants.onlineOrderCheckin + body;
	// request.headers(header.onlineOrderCheckinHeader(payload.toString()));
	// request.body(body);
	// response = request.post(ApiConstants.onlineOrderCheckin);
	// } catch (Exception e) {
	// logger.error(e.getMessage());
	// }
	// return response;
	// }

	public Response posCheckin(String email, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		punchhKey = CreateDateTime.getRandomNumberSixDigit() + CreateDateTime.getTimeDateString();
		try {
			logger.info("*****POS Checkin API*****");
			amount = Utilities.getApiConfigProperty("checkinAmount");
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			String body = ApiPayloads.posCheckinPayLoad(email, punchhKey, amount);
			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request1.header("Authorization", "Token token=" + authorization);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request1);
			}
			request1.body(body.toString());
			response = request1.post(ApiConstants.posCheckin);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posCheckin(String email, String locationkey, String amount) {
		Response response = null;
		requestParams = new JSONObject();
		punchhKey = CreateDateTime.getTimeDateString();
		try {
			// amount = Utilities.getApiConfigProperty("checkinAmount");
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			String body = ApiPayloads.posCheckinPayLoad(email, punchhKey, amount);

			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request1.header("Authorization", "Token token=" + locationkey);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request1);
			}
			request1.body(body.toString());
			response = request1.post(ApiConstants.posCheckin);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("POS Checkin API response: " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posCheckin(String date, String email, String key, String txn_no, String locationkey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			String body = apipaylods.posCheckinPayload(date, email, key, txn_no);
			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request1.header("Authorization", "Token token=" + locationkey);
			request1.body(body);
			response = request1.post(ApiConstants.posCheckin);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posCheckinQC(String date, String email, String key, String txn_no, String locationkey,
			String menuItemid) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			String body = apipaylods.posCheckinQCPayload(date, email, key, txn_no, locationkey, menuItemid);
			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			// request1.header("Authorization", "Token token="+locationkey);
			request1.body(body);
			response = request1.post(ApiConstants.posCheckin);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posRedemptionOfCode(String email, String date, String redemption_code, String key, String txn_no,
			String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			String body = apipaylods.posRedemptionofCodePayload(email, date, redemption_code, key, txn_no);
			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request1.header("Authorization", "Token token=" + locationKey);
			request1.body(body);
			response = request1.post(ApiConstants.posRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// Perform POS Redemption with secondary email or phone
	public Response posRedemptionOfCode(String primaryEmail, String secondaryEmail, String phone, String date,
			String redemptionCode, String punchhKey, String txnNumber, String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.posRedemptionPayload(primaryEmail, secondaryEmail, phone, date, redemptionCode,
					punchhKey, txnNumber);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Token token=" + locationKey);
			request.body(body);
			response = request.post(ApiConstants.posRedemption);
			utils.logit("POS Redemption API response: " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	/*
	 * public Response posRedemptionOfCode(String email, String date, String
	 * redemption_code, String key, String txn_no, String locationKey) { Response
	 * response = null; requestParams = new JSONObject(); try { RestAssured.baseURI
	 * = baseUri; RequestSpecification request1 = RestAssured.given().log().all();
	 * String body = apipaylods.posRedemptionofCodePayload(email, date,
	 * redemption_code, key, txn_no); request1.header("Content-Type",
	 * Utilities.getApiConfigProperty("contentType"));
	 * request1.header("Authorization", "Token token=" + locationKey);
	 * request1.body(body); response = request1.post(ApiConstants.posRedemption);
	 * logger.info("API Response:" + response.asString());
	 * TestListeners.extentTest.get().info("response is : "+ response.asString());
	 * logger.info("Response code: "+response.getStatusCode()); } catch (Exception
	 * e) { logger.error(e.getMessage()); } return response; }
	 */

	public Response posRedemptionOfSubscription(String email, String date, String subscription_id, String key,
			String txn_no, String locationKey, String amount, String itemID) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			String body = apipaylods.posRedemptionofSubscriptionPayload(email, date, subscription_id, key, txn_no,
					amount, itemID);
			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			// request1.header("Authorization", "Token token=" + locationKey);
			request1.header("Authorization", "Token token=" + locationKey);
			request1.body(body);
			response = request1.post(ApiConstants.posRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posRedemptionOfCouponCode(String email, String date, String redemption_code, String key,
			String txn_no, String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			String body = apipaylods.posRedemptionofCodePayload(email, date, redemption_code, key, txn_no);
			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request1.header("Authorization", "Token token=" + locationKey);
			request1.body(body);
			response = request1.post(ApiConstants.posRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posRedemptionOfFreeGift(String email, String date, String redemption_code, String key,
			String txn_no, String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			String body = apipaylods.posRedemptionofFreeGiftPayload(email, date, redemption_code, key, txn_no);
			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request1.header("Authorization", "Token token=" + locationKey);
			request1.body(body);
			response = request1.post(ApiConstants.posRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posRedemptionOfBogoGift(String email, String date, String redemption_code, String key,
			String txn_no, String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			String body = apipaylods.posRedemptionofBogoPayload(email, date, redemption_code, key, txn_no);
			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request1.header("Authorization", "Token token=" + locationKey);
			request1.body(body);
			response = request1.post(ApiConstants.posRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posRedemptionOfPromotionaltypeRedeemable(String email, String date, String redemption_code,
			String key, String txn_no, String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			String body = apipaylods.posRedemptionofPromotionaltypeRedeemable(email, date, redemption_code, key,
					txn_no);
			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request1.header("Authorization", "Token token=" + locationKey);
			request1.body(body);
			response = request1.post(ApiConstants.posRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posRedemptionOfRedeemable(String email, String date, String key, String txn_no, String locationKey,
			String redeemable_id, String item_id) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			String body = apipaylods.posRedemptionofRedeemablePayload(email, date, key, txn_no, redeemable_id, item_id);
			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request1.header("Authorization", "Token token=" + locationKey);
			request1.body(body);
			response = request1.post(ApiConstants.posRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posRedemptionOfAmount(String email, String date, String key, String txn_no, String redeemAmount,
			String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("*****POS Redemption of Amount API*****");
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			String body = apipaylods.posRedemptionofAmountPayload(email, date, key, txn_no, redeemAmount);
			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request1.header("Authorization", "Token token=" + locationKey);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request1);
			}
			request1.body(body);
			response = request1.post(ApiConstants.posRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posPossibleRedemptionOfAmount(String email, String date, String key, String txn_no,
			String redeemAmount, String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			String body = apipaylods.posRedemptionofAmountPayload(email, date, key, txn_no, redeemAmount);
			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request1.header("Authorization", locationKey);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request1);
			}
			request1.body(body);
			response = request1.post(ApiConstants.posPossibleRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posRedemptionOfCard(String email, String date, String key, String txn_no, String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			String body = apipaylods.posRedemptionofCardPayload(email, date, key, txn_no);
			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request1.header("Authorization", "Token token=" + locationKey);
			request1.body(body);
			response = request1.post(ApiConstants.posRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posRedemptionOfFuel(String email, String date, String key, String txn_no, String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			String body = apipaylods.posRedemptionofFuelPayload(email, date, key, txn_no);
			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request1.header("Authorization", "Token token=" + locationKey);
			request1.body(body);
			response = request1.post(ApiConstants.posRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posVoidRedemption(String email, String redemption_id, String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("*****POS Void Redemption API*****");
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			String body = apipaylods.posVoidRedemptionPayload(email, redemption_id);
			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request1.header("Authorization", "Token token=" + locationKey);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request1);
			}
			request1.body(body);
			response = request1.delete(ApiConstants.posRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2SubscriptionPurchase(String token, String plan_id, String client, String secret,
			String purchase_price, String endDateTime, String... taxValueLocationIdDiscountValue) {
		Response response = null;
		// requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.api2PurchaseSubscriptionPayload(plan_id, purchase_price, endDateTime,
					taxValueLocationIdDiscountValue);
			String payload = ApiConstants.mobApi2PurchaseSubscription + "?client=" + client + body;
			// Headers
			request.header("Authorization", "Bearer " + token);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.mobApi2PurchaseSubscription);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posRedemptionOfReward(String email, String locationKey, String rewardId) {
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			String body = apipaylods.posRedemptionofRewardPayload(email, date, key, txn, locationKey, rewardId);
			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			// request1.header("Authorization", locationKey);
			request1.body(body);
			response = request1.post(ApiConstants.posRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1MobileRedemptionRedeemable_id(String token, String redeemable_id, String client,
			String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("redeemable_id", redeemable_id);
			param = requestParams.toString();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1Redemption + "?client=" + client + "&hash=" + epoch;
			String payload = URL + param;
			// Map<String, Object> headersMap = header.api1SignUpHeader(payload, epoch,
			// Utilities.getApiConfigProperty("secret"));
			request.headers(header.api1SignUpHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + token);
			// request.headers(headersMap);
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.post(ApiConstants.mobApi1Redemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1MobileRedemptionReward_id(String token, String reward_id, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("reward_id", reward_id);
			param = requestParams.toString();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1Redemption + "?client=" + client + "&hash=" + epoch;
			String payload = URL + param;
			// Map<String, Object> headersMap = header.api1SignUpHeader(payload, epoch,
			// Utilities.getApiConfigProperty("secret"));
			request.headers(header.api1SignUpHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + token);
			// request.headers(headersMap);
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.post(ApiConstants.mobApi1Redemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1MobileRedemptionRedeemed_Points(String token, String redeemed_points, String client,
			String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("redeemed_points", redeemed_points);
			param = requestParams.toString();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1Redemption + "?client=" + client + "&hash=" + epoch;
			String payload = URL + param;
			request.headers(header.api1SignUpHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + token);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.post(ApiConstants.mobApi1Redemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1MobileRedemptionCardCompletion(String token, String card_completion, String client,
			String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("card_completion", card_completion);
			param = requestParams.toString();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1Redemption + "?client=" + client + "&hash=" + epoch;
			String payload = URL + param;
			// Map<String, Object> headersMap = header.api1SignUpHeader(payload, epoch,
			// Utilities.getApiConfigProperty("secret"));
			request.headers(header.api1SignUpHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + token);
			// request.headers(headersMap);
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.post(ApiConstants.mobApi1Redemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1MobileAccounts(String token, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// requestParams.put("redeemed_points", redeemed_points);
			param = requestParams.toString();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1accounts + "?client=" + client + "&hash=" + epoch;
			String payload = URL + param;

			request.headers(header.api1SignUpHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + token);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			// request.headers(headersMap);
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.get(ApiConstants.mobApi1accounts);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1MobileUsersbalance(String token, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// requestParams.put("redeemed_points", redeemed_points);
			param = requestParams.toString();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1usersbalance + "?client=" + client + "&hash=" + epoch;
			String payload = URL + param;

			request.headers(header.api1SignUpHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + token);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			// request.headers(headersMap);
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.get(ApiConstants.mobApi1usersbalance);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiSignUp(String email, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("email", email);
			hashmap.put("password", Utilities.getApiConfigProperty("password"));
			hashmap.put("first_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("firstName")));
			hashmap.put("last_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("lastName")));
			// hashmap.put("password", Utilities.getApiConfigProperty("password"));
			hashmap.put("birthday", Utilities.getApiConfigProperty("birthday"));
			hashmap.put("anniversary", Utilities.getApiConfigProperty("anniversary"));
			// hashmap.put("phone", Utilities.getRandomNo(1000) +
			// CreateDateTime.getTimeDateString());
			hashmap.put("signup_channel", Utilities.getApiConfigProperty("ssoSignupChannel"));
			requestParams.put("user", hashmap);
			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.authApiSignUp + param;
			// Headers passing
			request.headers(header.authAPISignUpHeader(payload, secret));
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.authApiSignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiSignUp(String email, String client, String secret, String phone) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("email", email);
			hashmap.put("password", Utilities.getApiConfigProperty("password"));
			hashmap.put("first_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("firstName")));
			hashmap.put("last_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("lastName")));
			hashmap.put("birthday", Utilities.getApiConfigProperty("birthday"));
			hashmap.put("anniversary", Utilities.getApiConfigProperty("anniversary"));
			hashmap.put("phone", phone);
			hashmap.put("signup_channel", Utilities.getApiConfigProperty("ssoSignupChannel"));
			requestParams.put("user", hashmap);
			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.authApiSignUp + param;
			// Headers passing
			request.headers(header.authAPISignUpHeader(payload, secret));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.authApiSignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiSignUpLoyaltyCard(String email, String client, String secret, String loyaltyCardNumber) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("email", email);
			hashmap.put("password", Utilities.getApiConfigProperty("password"));
			hashmap.put("first_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("firstName")));
			hashmap.put("last_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("lastName")));
			hashmap.put("birthday", Utilities.getApiConfigProperty("birthday"));
			hashmap.put("anniversary", Utilities.getApiConfigProperty("anniversary"));
			hashmap.put("card_number", loyaltyCardNumber);
			hashmap.put("signup_channel", Utilities.getApiConfigProperty("ssoSignupChannel"));
			requestParams.put("user", hashmap);
			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.authApiSignUp + param;
			// Headers passing
			request.headers(header.authAPISignUpHeader(payload, secret));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.authApiSignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiSignUp(String email, String client, String secret, String password,
			String password_confirmation, String birthday, String anniversary, String signup_channel,
			String invite_code, String first_name, String last_name) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("email", email);
			hashmap.put("first_name", first_name);
			hashmap.put("last_name", last_name);
			hashmap.put("password", password);
			hashmap.put("password_confirmation", password_confirmation);
			hashmap.put("birthday", birthday);
			hashmap.put("anniversary", anniversary);
			hashmap.put("signup_channel", signup_channel);
			hashmap.put("invite_code", invite_code);

			requestParams.put("user", hashmap);
			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.authApiSignUp + param;
			// Headers passing
			request.headers(header.authAPISignUpHeader(payload, secret));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.authApiSignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiUserLogin(String email, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("email", email);
			hashmap.put("password", Utilities.getApiConfigProperty("password"));

			requestParams.put("user", hashmap);
			requestParams.put("client", client);

			param = requestParams.toString();
			String payload = ApiConstants.authApiLogin + param;
			// Headers passing
			request.headers(header.authAPISignUpHeader(payload, secret));
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.authApiLogin);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// Api Auth login with or with out password , via email or phone, with and
	// without auth-digest
	public Response authApiUserLogin(String identifier, String client, String secret, Boolean usePassword,
			String authDigest) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();

			if (identifier.contains("@")) {
				hashmap.put("email", identifier);
			} else {
				hashmap.put("phone", identifier);
			}
			// Use password conditionally
			if (usePassword) {
				hashmap.put("password", Utilities.getApiConfigProperty("password"));
			}

			requestParams.put("user", hashmap);
			requestParams.put("client", client);

			param = requestParams.toString();
			String payload = ApiConstants.authApiLogin + param;
			// Headers passing
			request.headers(header.authAPISignUpHeader(payload, secret, authDigest));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.authApiLogin);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiUserLogin(String email, String client, String secret, String password) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("====== Auth Api Login with Password ======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("email", email);
			hashmap.put("password", password);

			requestParams.put("user", hashmap);
			requestParams.put("client", client);

			param = requestParams.toString();
			String payload = ApiConstants.authApiLogin + param;
			// Headers passing
			request.headers(header.authAPISignUpHeader(payload, secret));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.authApiLogin);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// To fetch user offers rewards coupons
	public Response getUserOffers(String authorization, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.queryParam("client", client);
			String payload = ApiConstants.mobApi2UserOffer + "?client=" + client;
			request.headers(header.userOffersHeader(payload, secret, authorization));
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			response = request.get(ApiConstants.mobApi2UserOffer);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
			// logger.info("Response code: "+response.getStatusCode());
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posRedemptionOfRewardIdAuthOnlineOrder(String authentication_token, String reward_id, String secret,
			String client) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			String body = apipaylods.posRedemptionofRewardIdAuthOnlineOrder(authentication_token, reward_id, client);

			// Headers passing Signature and content type
			String payload = ApiConstants.authOnlineRedemption + body;
			request1.headers(header.authAPISignUpHeader(payload, secret));
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request1);
			}
			request1.body(body);
			response = request1.post(ApiConstants.authOnlineRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posUserLookupFetchBalance(String email, String location) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("*****POS User lookup Fetch Balance API*****");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.queryParam("email", email);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			requestParams.put("location_key", location);
			request.body(requestParams.toJSONString());
			response = request.get(ApiConstants.userLookupAndFetchBalance);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2FetchUserInfo(String access_token, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.mobApi2FetchUserInfo + param;
			// headers
			request.headers(header.api2LoginHeader(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(requestParams.toJSONString());
			response = request.get(ApiConstants.mobApi2FetchUserInfo);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api2UserShow(String access_token, String user_id, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.mobApi2UserShow + user_id + param;
			// Headers
			request.headers(header.api2LoginHeader(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(requestParams.toJSONString());

			response = request.get(ApiConstants.mobApi2UserShow + user_id);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1UserShow(String client, String secret, String accessToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String payload = ApiConstants.mobApi1UserShow + "?client=" + client + "&hash=" + epoch;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			Map<String, Object> headers = new HashMap<String, Object>();
			putIfNotEmpty(headers, "Authorization", "Bearer " + accessToken);
			request.headers(headers);
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.get(ApiConstants.mobApi1UserShow);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api2LifetimeStats(String client, String secret, String access_token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.mobApi2LifetimeStats + param;
			// Headers
			request.headers(header.api2LoginHeader(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.body(requestParams.toJSONString());

			response = request.get(ApiConstants.mobApi2LifetimeStats);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api2MigrationLookup(String original_membership_no, String email, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			requestParams.put("client", client);
			requestParams.put("card_number", original_membership_no);
			requestParams.put("email", email);
			param = requestParams.toString();
			String payload = ApiConstants.mobApi2MigrationLookup + param;
			// Headers
			request.headers(header.api2LoginHeader(payload, secret));
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(requestParams.toJSONString());
			response = request.get(ApiConstants.mobApi2MigrationLookup);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api2MigrationLookupPost(String original_membership_no, String email, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("client", client);
			requestParams.put("card_number", original_membership_no);
			requestParams.put("email", email);
			param = requestParams.toString();
			String payload = ApiConstants.mobApi2MigrationLookup + param;
			request.headers(header.api2LoginHeader(payload, secret));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.mobApi2MigrationLookup);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posPaymentCancel(String paymentReferenceId, String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.body(requestParams.toJSONString());
			request.queryParam("payment_reference_id", paymentReferenceId);
			request.queryParam("location_key", locationKey);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			response = request.delete(ApiConstants.posPayment);
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api2SingleScanCode(String authToken, String payment_type, String transaction_token, String client,
			String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.api2SingleScanCodePayload(payment_type, transaction_token, client);

			// Headers
			String payload = ApiConstants.mobApi2SingleScanCode + body;
			request.header("Authorization", "Bearer " + authToken);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			request.header("User-Agent", api2Headers.api2userAgent());
			request.header("punchh-app-device-id", api2Headers.api2appDeviceId());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);

			response = request.post(ApiConstants.mobApi2SingleScanCode);
			TestListeners.extentTest.get().info(response.getStatusCode() + "; " + response.asString());
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api2GetAccessCode(String authToken, String payment_type, String gift_card_uuid, String client,
			String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.api2GetAccessCodePayload(payment_type, gift_card_uuid, client);

			// Headers
			String payload = ApiConstants.mobApi2SingleScanCode + body;
			request.header("Authorization", "Bearer " + authToken);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			request.header("User-Agent", api2Headers.api2userAgent());
			request.header("punchh-app-device-id", api2Headers.api2appDeviceId());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);

			response = request.post(ApiConstants.mobApi2SingleScanCode);
			TestListeners.extentTest.get().info(response.getStatusCode() + "; " + response.asString());
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api2CreatePaymentCard(String authToken, String heartlandToken, String adapterCode, String client,
			String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.api2CreatePaymentCardPayload(heartlandToken, adapterCode, client);

			// Headers
			String payload = ApiConstants.mobApi2PaymentCard + body;
			request.header("Authorization", "Bearer " + authToken);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			request.header("User-Agent", api2Headers.api2userAgent());
			request.header("punchh-app-device-id", api2Headers.api2appDeviceId());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);

			response = request.post(ApiConstants.mobApi2PaymentCard);
			TestListeners.extentTest.get().info(response.getStatusCode() + " response: " + response.asString());
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api2UpdatePaymentCard(String authToken, String paymentCardUuid, String nicknameToUpdate,
			String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.api2UpdatePaymentCardPayload(nicknameToUpdate, client);

			// Headers
			String payload = ApiConstants.mobApi2PaymentCard + "/" + paymentCardUuid + body;
			request.header("Authorization", "Bearer " + authToken);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			request.header("User-Agent", api2Headers.api2userAgent());
			request.header("punchh-app-device-id", api2Headers.api2appDeviceId());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);

			response = request.put(ApiConstants.mobApi2PaymentCard + "/" + paymentCardUuid);
			TestListeners.extentTest.get().info(response.getStatusCode() + " response: " + response.asString());
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api2FetchPaymentCard(String authToken, String adapterCode, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.api2FetchPaymentCardPayload(adapterCode, client);

			// Headers
			String payload = ApiConstants.mobApi2PaymentCard + body;
			request.header("Authorization", "Bearer " + authToken);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			request.header("User-Agent", api2Headers.api2userAgent());
			request.header("punchh-app-device-id", api2Headers.api2appDeviceId());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);

			response = request.get(ApiConstants.mobApi2PaymentCard);
			TestListeners.extentTest.get().info(response.getStatusCode() + " response: " + response.asString());
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api2DeletePaymentCard(String authToken, String paymentCardUuid, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.api2DeletePaymentCardPayload(client);

			// Headers
			String payload = ApiConstants.mobApi2PaymentCard + "/" + paymentCardUuid + body;
			request.header("Authorization", "Bearer " + authToken);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			request.header("User-Agent", api2Headers.api2userAgent());
			request.header("punchh-app-device-id", api2Headers.api2appDeviceId());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);

			response = request.delete(ApiConstants.mobApi2PaymentCard + "/" + paymentCardUuid);
			TestListeners.extentTest.get().info(response.getStatusCode() + " response: " + response.asString());
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1FetchMessages(String client, String secret, String accessToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String payload = ApiConstants.mobApi1FetchMessages + "?client=" + client + "&hash=" + epoch;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + accessToken);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.get(ApiConstants.mobApi1FetchMessages);
			TestListeners.extentTest.get().info("Response: " + response.getStatusCode() + "; " + response.asString());
			logger.info("Response: " + response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api2VersionNotes(String version, String os, String model, String client) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("client", client);
			requestParams.put("version", version);
			requestParams.put("os", os);
			requestParams.put("model", model);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept", api2Headers.api2Accept());
			request.header("User-Agent", api2Headers.api2userAgent());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(requestParams.toJSONString());
			response = request.get(ApiConstants.metaV2VersionNotes);
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api2BeaconEntry(String client, String secret, String accessToken, String beaconMajor,
			String beaconMinor) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String URL = ApiConstants.mobApi2BeaconEntry;
			String body = apipaylods.api2BeaconPayload(client, accessToken, beaconMajor, beaconMinor);
			String payload = URL + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("User-Agent", api2Headers.api2userAgent());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.mobApi2BeaconEntry);
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api2BeaconExit(String client, String secret, String accessToken, String beaconMajor,
			String beaconMinor) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String URL = ApiConstants.mobApi2BeaconExit;
			String body = apipaylods.api2BeaconPayload(client, accessToken, beaconMajor, beaconMinor);
			String payload = URL + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("User-Agent", api2Headers.api2userAgent());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.delete(ApiConstants.mobApi2BeaconExit);
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api2GiftCardGiftedWithRandomAmount(String client, String secret, String accessToken,
			String userEmail, String designId, String transactionToken, String cardHolderName, String expDate,
			String cardType) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.api2GiftCardGiftedWithRandomAmountPayload(userEmail, client, designId,
					transactionToken, cardHolderName, expDate, cardType);
			String payload = ApiConstants.mobApi2GiftaCard + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Accept", api2Headers.api2Accept());
			request.header("Authorization", "Bearer " + accessToken);
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.mobApi2GiftaCard);
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api2ReloadGiftCardWithRandomAmount(String client, String secret, String accessToken, String uuid,
			String userEmail, String designId, String transactionToken, String cardHolderName, String expDate,
			String cardType) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.api2ReloadGiftCardWithRandomAmountPayload(userEmail, client, designId,
					transactionToken, cardHolderName, expDate, cardType);
			String payload = ApiConstants.mobApi2ReloadGiftCard + uuid + "/reload" + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Accept", api2Headers.api2Accept());
			request.header("Authorization", "Bearer " + accessToken);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.mobApi2ReloadGiftCard + uuid + "/reload");
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api2UserAccountDeletion(String token, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.mobApi2AccountDeletionRequest + param;
			request.headers(header.api2SignUpHeader(payload, secret));
			request.header("Authorization", "Bearer " + token);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(requestParams.toJSONString());
			response = request.delete(ApiConstants.mobApi2AccountDeletionRequest);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1UserLogout(String client, String secret, String accessToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String payload = ApiConstants.mobApi1Logout + "?client=" + client + "&hash=" + epoch;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + accessToken);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.delete(ApiConstants.mobApi1Logout);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1UpdateUser(String signupChannel, String client, String secret, String accessToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1UpdateUser + "?client=" + client + "&hash=" + epoch;
			String body = apipaylods.api1UpdateUserPayload(signupChannel);
			String payload = URL + body;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + accessToken);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.put(ApiConstants.mobApi1UpdateUser);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response basicAuthSignUp(String client, Map<String, Object> payload) {
		Response response = null;
		try {
			RequestSpecification request = RestAssured.given().log().all();
			String baseUri = getGuestIdentityBaseUrl();
			request.baseUri(baseUri);
			request.queryParam("client", client);
			request.headers(header.defaultHeaders());
			Map<String, Object> body = new HashMap<>();
			body.put("user", payload);
			request.body(body);
			response = request.post(ApiConstants.basicAuthSignUp);
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("Error", e.getMessage());
		}
		return response;
	}

	public Response basicAuthSignIn(String client, String email, String password) {
		Response response = null;
		try {
			RequestSpecification request = RestAssured.given().log().all();
			String baseUri = getGuestIdentityBaseUrl();
			request.baseUri(baseUri);
			request.queryParam("client", client);
			request.headers(header.defaultHeaders());
			Map<String, Object> user = new HashMap<>();
			user.put("email", email);
			user.put("password", password);
			Map<String, Object> body = new HashMap<>();
			body.put("user", user);
			request.body(body);
			response = request.post(ApiConstants.basicAuthSignIn);
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("Error", e.getMessage());
		}
		return response;
	}

	public Response basicAuthRefresh(String client, String refreshToken) {
		Response response = null;
		try {
			RequestSpecification request = RestAssured.given().log().all();
			String baseUri = getGuestIdentityBaseUrl();
			request.baseUri(baseUri);
			request.queryParam("client", client);
			request.headers(header.defaultHeaders());
			Map<String, Object> body = new HashMap<>();
			body.put("refresh_token", refreshToken);
			request.body(body);
			response = request.post(ApiConstants.basicAuthRefresh);
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("Error", e.getMessage());
		}
		return response;
	}

	public Response basicAuthForgotPassword(String client, String email, String token_in_response) {
		Response response = null;
		try {
			RequestSpecification request = RestAssured.given().log().all();
			String baseUri = getGuestIdentityBaseUrl();
			request.baseUri(baseUri);
			request.queryParam("client", client);
			request.headers(header.defaultHeaders());
			Map<String, Object> body = new HashMap<>();
			body.put("email", email);
			body.put("token_in_response", token_in_response);
			request.body(body);
			response = request.post(ApiConstants.basicAuthForgotPassword);
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("Error", e.getMessage());
		}
		return response;
	}

	public Response basicAuthResetPassword(String client, String reset_token, String new_password) {
		Response response = null;
		try {
			RequestSpecification request = RestAssured.given().log().all();
			String baseUri = getGuestIdentityBaseUrl();
			request.baseUri(baseUri);
			request.queryParam("client", client);
			request.headers(header.defaultHeaders());
			Map<String, Object> user = new HashMap<>();
			user.put("reset_token", reset_token);
			user.put("new_password", new_password);
			user.put("password_confirmation", new_password);
			Map<String, Object> body = new HashMap<>();
			body.put("user", user);
			request.body(body);
			response = request.post(ApiConstants.basicAuthResetPassword);
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("Error", e.getMessage());
		}
		return response;
	}

	public Response basicAuthChangePassword(String client, String accessToken, String current_password,
			String new_password) {
		Response response = null;
		try {
			RequestSpecification request = RestAssured.given().log().all();
			String baseUri = getGuestIdentityBaseUrl();
			request.baseUri(baseUri);
			request.queryParam("client", client);
			request.headers(header.defaultHeaders());
			request.header("Authorization", "Bearer " + accessToken);
			Map<String, Object> user = new HashMap<>();
			user.put("current_password", current_password);
			user.put("new_password", new_password);
			user.put("password_confirmation", new_password);
			Map<String, Object> body = new HashMap<>();
			body.put("user", user);
			request.body(body);
			response = request.post(ApiConstants.basicAuthChangePassword);
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("Error", e.getMessage());
		}
		return response;
	}

	public Response basicAuthSignOut(String client, String accessToken, String refreshToken) {
		Response response = null;
		try {
			RequestSpecification request = RestAssured.given().log().all();
			String baseUri = getGuestIdentityBaseUrl();
			request.baseUri(baseUri);
			request.queryParam("client", client);
			request.headers(header.defaultHeaders());
			Map<String, Object> body = new HashMap<>();
			body.put("refresh_token", refreshToken);
			body.put("access_token", accessToken);
			request.body(body);
			response = request.delete(ApiConstants.basicAuthSignOut);
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("Error", e.getMessage());
		}
		return response;
	}

	public Response api1UpdateUser(String client, String secret, String accessToken, String email, String phone,
			String firstName, String lastName) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1UpdateUser + "?client=" + client + "&hash=" + epoch;
			Map<String, Object> body = apipaylods.api1UpdateUserPayload(email, phone, firstName, lastName);
			String payload = URL + new ObjectMapper().writeValueAsString(body);
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + accessToken);
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.put(ApiConstants.mobApi1UpdateUser);
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("Error", e.getMessage());
		}
		return response;
	}

	public Response api1CreateFeedback(String rating, String message, String client, String secret,
			String accessToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1CreateFeedback + "?client=" + client + "&hash=" + epoch;
			String body = apipaylods.api1CreateFeedbackPayload(rating, message);
			String payload = URL + body;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + accessToken);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1CreateFeedback);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1GetScratchBoard(String client, String secret, String accessToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String payload = ApiConstants.mobApi1ScratchBoard + "?client=" + client + "&hash=" + epoch;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + accessToken);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.get(ApiConstants.mobApi1ScratchBoard);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1ImportGiftCard(String designId, String cardNumber, String epin, String client, String secret,
			String accessToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1ImportGiftCard + "?client=" + client + "&hash=" + epoch;
			String body = apipaylods.api1ImportGiftCardPayload(designId, cardNumber, epin);
			String payload = URL + body;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + accessToken);
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1ImportGiftCard);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api2ImportGiftCard(String giftCardName, String designId, String cardNumber, String epin,
			String client, String secret, String accessToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String URL = ApiConstants.mobApi2FetchGiftCard;
			String body = apipaylods.api2ImportGiftCardPayload(client, designId, cardNumber, epin, giftCardName);
			String payload = URL + body;
			request.header("Authorization", "Bearer " + accessToken);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			request.header("User-Agent", api2Headers.api2userAgent());
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.body(body);
			response = request.post(ApiConstants.mobApi2FetchGiftCard);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1CurrencyTransferToOtherUser(String recipientEmail, String amount, String client, String secret,
			String accessToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1CurrencyTransfer + "?client=" + client + "&hash=" + epoch;
			String body = apipaylods.api1CurrencyTransferToOtherUser(recipientEmail, amount);
			String payload = URL + body;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + accessToken);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1CurrencyTransfer);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1BraintreeToken(String client, String secret, String accessToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String payload = ApiConstants.mobApi1BraintreeToken + "?client=" + client + "&hash=" + epoch;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + accessToken);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.get(ApiConstants.mobApi1BraintreeToken);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1PurchaseGiftCard(String client, String secret, String amount, String accessToken,
			String designId, String transactionToken, String expDate) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.api1PurchaseGiftCardPayload(amount, designId, transactionToken, expDate);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1PurchaseGiftCard + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;
			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + accessToken);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1PurchaseGiftCard);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1PurchaseGiftCardBySendingAmount(String client, String secret, String amount, String accessToken,
			String designId, String transactionToken, String expDate) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.api1PurchaseGiftCardPayloadBySendingAmount(amount, designId, transactionToken,
					expDate);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1PurchaseGiftCard + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;
			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + accessToken);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1PurchaseGiftCard);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1UpdateGiftCard(String client, String secret, String accessToken, String preferred,
			String giftCardUuid) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String payload = ApiConstants.mobApi1GiftCard + giftCardUuid + "?client=" + client + "&hash=" + epoch
					+ "&preferred=" + preferred;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + accessToken);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.queryParam("preferred", preferred);
			response = request.put(ApiConstants.mobApi1GiftCard + giftCardUuid);
			TestListeners.extentTest.get().info(response.getStatusCode() + "; " + response.asString());
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response dashboardSubscriptionCancel(String adminKey, String subscriptionID, String cancellationReason,
			String cancellationType) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.dashboardSubscriptionCancelPayload(subscriptionID, cancellationReason,
					cancellationType);
			request.header("Authorization", "Bearer " + adminKey);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept", api2Headers.api2Accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.patch(ApiConstants.dashboardSubscriptionCancel);
			TestListeners.extentTest.get().info("response: " + response.getStatusCode() + "; " + response.asString());
			logger.info("response: " + response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1FetchGiftCardBalance(String client, String secret, String accessToken, String giftCardUuid) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String payload = ApiConstants.mobApi1GiftCard + giftCardUuid + "/balance" + "?client=" + client + "&hash="
					+ epoch;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + accessToken);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.get(ApiConstants.mobApi1GiftCard + giftCardUuid + "/balance");
			TestListeners.extentTest.get().info(response.getStatusCode() + "; " + response.asString());
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1GiftCardTransactionHistory(String client, String secret, String accessToken,
			String giftCardUuid) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String payload = ApiConstants.mobApi1GiftCard + giftCardUuid + "/history" + "?client=" + client + "&hash="
					+ epoch;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + accessToken);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.get(ApiConstants.mobApi1GiftCard + giftCardUuid + "/history");
			TestListeners.extentTest.get().info(response.getStatusCode() + "; " + response.asString());
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1ReloadGiftCard(String client, String secret, String accessToken, String amount, String designId,
			String transactionToken, String giftCardUuid) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.api1ReloadGiftCardPayload(amount, designId, transactionToken);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1GiftCard + giftCardUuid + "/reload" + "?client=" + client + "&hash="
					+ epoch;
			String payload = URL + body;
			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + accessToken);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1GiftCard + giftCardUuid + "/reload");
			TestListeners.extentTest.get().info(response.getStatusCode() + "; " + response.asString());
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1ReloadGiftCardBySendingAmount(String client, String secret, String accessToken, String amount,
			String designId, String transactionToken, String giftCardUuid) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.api1ReloadGiftCardPayloadBySendingAmount(amount, designId, transactionToken);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1GiftCard + giftCardUuid + "/reload" + "?client=" + client + "&hash="
					+ epoch;
			String payload = URL + body;
			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + accessToken);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1GiftCard + giftCardUuid + "/reload");
			TestListeners.extentTest.get().info(response.getStatusCode() + "; " + response.asString());
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1VersionNotes(String version, String os, String model, String client) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("client", client);
			requestParams.put("version", version);
			requestParams.put("os", os);
			requestParams.put("model", model);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept", api1Headers.accept());
			request.header("User-Agent", api1Headers.userAgent());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(requestParams.toJSONString());
			response = request.get(ApiConstants.mobApi1VersionNotes);
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1FetchNotifications(String client, String secret, String accessToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String payload = ApiConstants.mobApi1FetchNotifications + "?client=" + client + "&hash=" + epoch;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + accessToken);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.get(ApiConstants.mobApi1FetchNotifications);
			TestListeners.extentTest.get().info("Response: " + response.getStatusCode() + "; " + response.asString());
			logger.info("Response: " + response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1FetchUserOffers(String client, String secret, String accessToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String payload = ApiConstants.mobApi1FetchUserOffers + "?client=" + client + "&hash=" + epoch;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + accessToken);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.get(ApiConstants.mobApi1FetchUserOffers);
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1UserMigrationLookup(String original_membership_no, String email, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			requestParams.put("card_number", original_membership_no);
			requestParams.put("email", email);
			param = requestParams.toString();
			String payload = ApiConstants.mobApi1MigrationUserLookup + "?client=" + client + "&hash=" + epoch + param;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(requestParams.toJSONString());
			response = request.get(ApiConstants.mobApi1MigrationUserLookup);
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1GiftCardTip(String client, String secret, String accessToken, String checkinId,
			String giftCardUuid, String tip, String requestType) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String payload = ApiConstants.mobApi1GiftCard + giftCardUuid + "/tip" + "?client=" + client + "&hash="
					+ epoch + "&checkin_id=" + checkinId + "&tip=" + tip;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + accessToken);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.queryParam("checkin_id", checkinId);
			request.queryParam("tip", tip);
			if (requestType.equalsIgnoreCase("post")) {
				response = request.post(ApiConstants.mobApi1GiftCard + giftCardUuid + "/tip");
			} else if (requestType.equalsIgnoreCase("get")) {
				response = request.get(ApiConstants.mobApi1GiftCard + giftCardUuid + "/tip");
			}
			TestListeners.extentTest.get().info(response.getStatusCode() + "; " + response.asString());
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1GiftaCard(String client, String secret, String accessToken, String userEmail, String amount,
			String designId, String transactionToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1GiftCard + "gift" + "?client=" + client + "&hash=" + epoch;
			String body = apipaylods.api1GiftaCardPayload(userEmail, amount, designId, transactionToken);
			String payload = URL + body;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + accessToken);
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1GiftCard + "gift");
			TestListeners.extentTest.get().info(response.getStatusCode() + "; " + response.asString());
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1GiftaCardWithRandomAmount(String client, String secret, String accessToken, String userEmail,
			String designId, String transactionToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1GiftCard + "gift" + "?client=" + client + "&hash=" + epoch;
			String body = apipaylods.api1GiftaCardWithRandomAmountPayload(userEmail, designId, transactionToken);
			String payload = URL + body;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + accessToken);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1GiftCard + "gift");
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1GenerateOtpToken(String client, String secret, String accessToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String payload = ApiConstants.mobApi1GenerateOtpToken + "?client=" + client + "&hash=" + epoch;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + accessToken);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.post(ApiConstants.mobApi1GenerateOtpToken);
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1BeaconEntry(String client, String secret, String accessToken, String beaconIDs) {
		Response response = null;
		String[] beaconIDsdata = beaconIDs.split(",");
		String beaconMinor = beaconIDsdata[0];
		String beaconMajor = beaconIDsdata[1];
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String payload = ApiConstants.mobApi1BeaconEntry + "?client=" + client + "&hash=" + epoch + "&beacon_minor="
					+ beaconMinor + "&beacon_major=" + beaconMajor;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + accessToken);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.queryParam("beacon_minor", beaconMinor);
			request.queryParam("beacon_major", beaconMajor);
			response = request.post(ApiConstants.mobApi1BeaconEntry);
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1BeaconExit(String client, String secret, String accessToken, String beaconIDs) {
		Response response = null;
		String[] beaconIDsdata = beaconIDs.split(",");
		String beaconMinor = beaconIDsdata[0];
		String beaconMajor = beaconIDsdata[1];
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String payload = ApiConstants.mobApi1BeaconExit + "?client=" + client + "&hash=" + epoch + "&beacon_minor="
					+ beaconMinor + "&beacon_major=" + beaconMajor;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + accessToken);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.queryParam("beacon_minor", beaconMinor);
			request.queryParam("beacon_major", beaconMajor);
			response = request.delete(ApiConstants.mobApi1BeaconExit);
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1SocialCauseDonation(String client, String secret, String access_token, String social_cause_id,
			String donation_type, String item_to_donate) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1SocialCause + "?client=" + client + "&hash=" + epoch;
			String body = apipaylods.api1CreateDonationPayload(social_cause_id, donation_type, item_to_donate);
			String payload = URL + body;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + access_token);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1SocialCause);
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1CreatePasscode(String passcode, String client, String secret, String accessToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String payload = ApiConstants.mobApi1CreatePasscode + "?client=" + client + "&hash=" + epoch + "&passcode="
					+ passcode;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + accessToken);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.queryParam("passcode", passcode);
			response = request.post(ApiConstants.mobApi1CreatePasscode);
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1UpdatePasscode(String client, String secret, String accessToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String payload = ApiConstants.mobApi1UpdatePasscode + "?client=" + client + "&hash=" + epoch;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + accessToken);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.post(ApiConstants.mobApi1UpdatePasscode);
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1CancelRedemption(String redemptionId, String authToken, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String payload = ApiConstants.mobApi1Redemption + "/" + redemptionId + "?client=" + client + "&hash="
					+ epoch;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + authToken);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.delete(ApiConstants.mobApi1Redemption + "/" + redemptionId);
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posVoidMultipleRedemption(String email, ArrayList<String> redemptionIdList, String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			String body = apipaylods.posVoidMultipleRedemptionPayload(email, redemptionIdList);
			logger.info(body);
			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request1.header("Authorization", "Token token=" + locationKey);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request1);
			}
			request1.body(body);
			response = request1.delete(ApiConstants.voidMultipleRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// public Response posVoidRedemption(String email, String redemption_id, String
	// locationKey) {
	// Response response = null;
	// requestParams = new JSONObject();
	// try {
	// RestAssured.baseURI = baseUri;
	// RequestSpecification request1 = RestAssured.given().log().all();
	// String body = apipaylods.posVoidRedemptionPayload(email, redemption_id);
	// request1.header("Content-Type",
	// Utilities.getApiConfigProperty("contentType"));
	// request1.header("Authorization", "Token token=" + locationKey);
	// request1.body(body);
	// response = request1.delete(ApiConstants.posRedemption);
	// logger.info("API Response:" + response.asString());
	// TestListeners.extentTest.get().info("response is : "+ response.asString());
	// logger.info("Response code: "+response.getStatusCode());
	// } catch (Exception e) {
	// logger.error(e.getMessage());
	// }
	// return response;
	// }

	public Response Api2UpdateUserProfile(String client, String email, String secret, String access_token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2UpdateUserProfilePayload(client, email);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2UpdateUserInfo + body;
			request.headers(header.api2SignUpHeader(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			// request.body(requestParams.toJSONString());
			request.body(body);

			response = request.put(ApiConstants.mobApi2UpdateUserInfo);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2UpdateUserEmailPhone(String client, String secret, String access_token, String email,
			String phone, String fName, String lName) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			Map<String, Object> payloadMap = apipaylods.API2UpdateUserEmailPhonePayload(client, email, phone, fName,
					lName);
			String body = new JSONObject(payloadMap).toString();
			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2UpdateUserInfo + body;
			request.headers(header.api2SignUpHeader(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.put(ApiConstants.mobApi2UpdateUserInfo);
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("error", e.getMessage());
		}
		return response;
	}

	public Response Api2UpdateUser(String client, String secret, String access_token, String email, String phone,
			String firstName, String lastName) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			Map<String, Object> bodyMap = apipaylods.Api2UpdateUserPayload(client, email, phone, firstName, lastName);
			String bodyJson = new JSONObject(bodyMap).toJSONString();
			String payload = ApiConstants.mobApi2UpdateUserInfo + bodyJson;
			request.headers(header.api2SignUpHeader(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.body(bodyJson);
			response = request.put(ApiConstants.mobApi2UpdateUserInfo);
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("error", e.getMessage());
		}
		return response;
	}

	public Response Api2CreateUserrelation(String client, String secret, String access_token) {
		Response response = null;

		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2CreateUserRelationPayload(client);

			String payload = ApiConstants.mobApi2CreateUserrelation + body;
			// Headers
			request.header("Authorization", "Bearer " + access_token);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.mobApi2CreateUserrelation);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2CreateUserrelationSpouseBirthdate(String client, String secret, String access_token,
			String birthdate) {
		Response response = null;

		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2CreateUserRelationSpouseBirthdatePayload(client, birthdate);

			String payload = ApiConstants.mobApi2CreateUserrelation + body;
			// Headers
			request.header("Authorization", "Bearer " + access_token);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());

			request.body(body);
			response = request.post(ApiConstants.mobApi2CreateUserrelation);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2CreateUserrelationKidsBirthdate(String client, String secret, String access_token,
			String kidNane, String birthdate) {
		Response response = null;

		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2CreateUserRelationKidBirthdatePayload(client, kidNane, birthdate);

			String payload = ApiConstants.mobApi2CreateUserrelation + body;
			// Headers
			request.header("Authorization", "Bearer " + access_token);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());

			request.body(body);
			response = request.post(ApiConstants.mobApi2CreateUserrelation);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2UpdateUserrelation(String client, String secret, String access_token, int id) {
		Response response = null;

		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2UpdateUserRelationPayload(client);

			String payload = ApiConstants.mobApi2UpdateUserrelation + id + body;
			// Headers
			request.header("Authorization", "Bearer " + access_token);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.put(ApiConstants.mobApi2UpdateUserrelation + id);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2DeleteUserRelation(String client, String secret, String access_token, String id) {
		Response response = null;
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2ClientPayload(client);

			String payload = ApiConstants.mobApi2DeleteUserrelation + id + body;

			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.delete(ApiConstants.mobApi2DeleteUserrelation + id);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2Logout(String access_token, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile Api2 Logout======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			requestParams.put("client", client);
			requestParams.put("access_token", access_token);

			param = requestParams.toString();
			String payload = ApiConstants.mobApi2Logout + param;
			request.headers(header.api2LoginHeader(payload, secret));
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(requestParams.toJSONString());
			response = request.delete(ApiConstants.mobApi2Logout);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiUpdateUserInfo(String client, String secret, String authToken, String email, String fName,
			String lName) {
		Response response = null;
		requestParams = new JSONObject();
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("email", email);
			hashmap.put("password", Utilities.getApiConfigProperty("updatedPassword"));
			hashmap.put("first_name", fName);
			hashmap.put("last_name", lName);
			hashmap.put("birthday", Utilities.getApiConfigProperty("updatedBirthday"));
			hashmap.put("anniversary", Utilities.getApiConfigProperty("updatedAnniversary"));
			requestParams.put("user", hashmap);
			requestParams.put("client", client);
			requestParams.put("authentication_token", authToken);
			param = requestParams.toString();
			String payload = ApiConstants.authUpdateUserInfo + param;
			// Headers passing
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			request.headers(defaultHeaderMap);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(requestParams.toJSONString());
			response = request.put(ApiConstants.authUpdateUserInfo);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// Helper method to add non-empty values to map
	public void putIfNotEmpty(Map<String, Object> map, String key, String value) {
		if (value != null && !value.trim().isEmpty()) {
			map.put(key, value);
		}
	}

	public Response authApiUpdateUserInfoEmailPhone(String client, String secret, String authToken, String email,
			String phone, String fName, String lName) {
		Response response = null;
		requestParams = new JSONObject();
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			putIfNotEmpty(hashmap, "first_name", fName);
			putIfNotEmpty(hashmap, "last_name", lName);
			putIfNotEmpty(hashmap, "phone", phone);
			requestParams.put("user", hashmap);
			requestParams.put("client", client);
			// requestParams.put("authentication_token", authToken);
			param = requestParams.toString();
			String payload = ApiConstants.authUpdateUserInfo + param;
			// Headers passing
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("Authorization", "Bearer " + authToken);
			defaultHeaderMap.put("x-pch-digest", signature);
			request.headers(defaultHeaderMap);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(requestParams.toJSONString());
			response = request.put(ApiConstants.authUpdateUserInfo);
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("error", e.getMessage());
		}
		return response;
	}

	public Response authApiUpdateUser(String client, String secret, String accessToken, String email, String phone,
			String firstName, String lastName) {
		Response response = null;
		requestParams = new JSONObject();
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			Map<String, Object> userData = apipaylods.authApiUpdateUserPayload(email, phone, firstName, lastName);
			requestParams.put("user", userData);
			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.authUpdateUserInfo + param;
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			request.headers(defaultHeaderMap);
			request.header("Authorization", "Bearer " + accessToken);
			request.body(requestParams.toJSONString());
			response = request.put(ApiConstants.authUpdateUserInfo);
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("Error", e.getMessage());
		}
		return response;
	}

	public Response authApiUpdateUserInfoWithHeaderAuth(String client, String secret, String authToken, String email,
			String fName, String lName) {
		Response response = null;
		requestParams = new JSONObject();
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("email", email);
			hashmap.put("password", Utilities.getApiConfigProperty("updatedPassword"));
			hashmap.put("first_name", fName);
			hashmap.put("last_name", lName);
			requestParams.put("user", hashmap);
			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.authUpdateUserInfo + param;
			// Headers passing
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			defaultHeaderMap.put("Authorization", "Bearer " + authToken);
			request.headers(defaultHeaderMap);
			request.body(requestParams.toJSONString());
			response = request.put(ApiConstants.authUpdateUserInfo);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiGetUserInfoWithHeaderAuth(String client, String secret, String authToken) {
		Response response = null;
		requestParams = new JSONObject();
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.authUpdateUserInfo + param;

			// Headers passing
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			defaultHeaderMap.put("Authorization", "Bearer " + authToken);
			request.headers(defaultHeaderMap);
			request.body(requestParams.toJSONString());
			response = request.get(ApiConstants.authGetUserInfo);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2AsynchronousUserUpdate(String client, String email, String secret, String access_token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2UpdateUserProfilePayload(client, email);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2asyncUserUpdate + body;

			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);

			response = request.patch(ApiConstants.mobApi2asyncUserUpdate);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiFetchUserInfo(String client, String secret, String authToken) {
		Response response = null;
		requestParams = new JSONObject();
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("client", client);
			requestParams.put("authentication_token", authToken);
			param = requestParams.toString();
			String payload = ApiConstants.authUpdateUserInfo + param;
			// Headers passing
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			request.headers(defaultHeaderMap);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(requestParams.toJSONString());
			response = request.get(ApiConstants.authUpdateUserInfo);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2UserSessionToken(String client, String secret, String access_token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			requestParams.put("client", client);
			request.body(requestParams.toJSONString());
			param = requestParams.toString();
			String payload = ApiConstants.mobApi2Sessiontoken + param;
			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + access_token);
			request.header("Accept", api2Headers.api2Accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			response = request.post(ApiConstants.mobApi2Sessiontoken);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiAccountHistory(String authToken, String client, String secret) {
		Response response = null;
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("client", client);
			if (!utils.isJwtToken(authToken))
				requestParams.put("authentication_token", authToken);
			param = requestParams.toString();
			String payload = ApiConstants.authAccountHistory + param;
			request.body(requestParams.toJSONString());
			// Headers passing
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			defaultHeaderMap.put("Accept", prop.getProperty("contentType"));
			defaultHeaderMap.put("Accept-Language", "en");
			if (utils.isJwtToken(authToken))
				defaultHeaderMap.put("Authorization", "Bearer " + authToken);
			request.headers(defaultHeaderMap);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			response = request.get(ApiConstants.authAccountHistory);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2SendVreificationEmail(String client, String secret, String access_token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("client", client);
			request.body(requestParams.toJSONString());
			param = requestParams.toString();
			String payload = ApiConstants.mobApi2SendVerificationEmail + param;
			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + access_token);
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			response = request.post(ApiConstants.mobApi2SendVerificationEmail);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiFetchAccountBalance(String authToken, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("client", client);
			requestParams.put("authentication_token", authToken);
			param = requestParams.toString();
			String payload = ApiConstants.authAccountBalance + param;
			request.body(requestParams.toJSONString());
			// Headers passing
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			defaultHeaderMap.put("Accept", prop.getProperty("contentType"));
			defaultHeaderMap.put("Accept-Language", "en");
			request.headers(defaultHeaderMap);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			response = request.get(ApiConstants.authAccountBalance);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2ForgotPassword(String client, String secret, String email) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2ForgotPassword(client, email);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2ForgotPassword + body;

			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);

			response = request.post(ApiConstants.mobApi2ForgotPassword);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiFetchUserBalance(String authToken, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("client", client);
			requestParams.put("authentication_token", authToken);
			param = requestParams.toString();
			String payload = ApiConstants.authFetchUserBalance + param;
			request.body(requestParams.toJSONString());
			// Headers passing
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			defaultHeaderMap.put("Accept", prop.getProperty("contentType"));
			defaultHeaderMap.put("Accept-Language", "en");
			request.headers(defaultHeaderMap);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			response = request.get(ApiConstants.authFetchUserBalance);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2FetchUserBalance(String client, String secret, String access_token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			requestParams.put("client", client);
			request.body(requestParams.toJSONString());
			param = requestParams.toString();
			String payload = ApiConstants.mobApi2FetchUserBalance + param;
			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + access_token);
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			response = request.get(ApiConstants.mobApi2FetchUserBalance);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiForgotPassword(String email, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("email", email);
			requestParams.put("client", client);
			requestParams.put("user", hashmap);
			param = requestParams.toString();
			String payload = ApiConstants.authForgotPassword + param;
			request.body(requestParams.toJSONString());
			// Headers passing
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			defaultHeaderMap.put("Accept", prop.getProperty("contentType"));
			defaultHeaderMap.put("Accept-Language", "en");
			request.headers(defaultHeaderMap);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			response = request.post(ApiConstants.authForgotPassword);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2BalanceTimeline(String client, String secret, String access_token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			requestParams.put("client", client);
			requestParams.put("access_token", access_token);
			request.body(requestParams.toJSONString());
			param = requestParams.toString();
			String payload = ApiConstants.mobApi2BalanceTimeline + param;
			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			response = request.get(ApiConstants.mobApi2BalanceTimeline);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiVoidRedemptions(String authToken, String client, String secret, String redemptionId) {
		Response response = null;
		requestParams = new JSONObject();
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("client", client);
			requestParams.put("authentication_token", authToken);
			requestParams.put("redemption_id", redemptionId);
			param = requestParams.toString();
			String payload = ApiConstants.authVoidRedemption + param;
			request.body(requestParams.toJSONString());
			// Headers passing
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			defaultHeaderMap.put("Accept", prop.getProperty("contentType"));
			request.headers(defaultHeaderMap);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			response = request.delete(ApiConstants.authVoidRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2EstimatePointsEarning(String client, String secret, String access_token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2EstimatePointsEarning(client, access_token);
			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2EstimatePointsEarning + body;
			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.get(ApiConstants.mobApi2EstimatePointsEarning);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authListAvailableRewards(String authToken, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.queryParam("authentication_token", authToken);
			requestParams.put("client", client);
			param = requestParams.toString();
			String URL = ApiConstants.authListAvailableRewards + "?authentication_token=" + authToken;
			String payload = URL + param;
			request.body(requestParams.toJSONString());
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			request.headers(defaultHeaderMap);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			response = request.get(ApiConstants.authListAvailableRewards);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2RedemptionWitReedemable_id(String client, String secret, String access_token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2RedemptionWitReedemable_id(client);
			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2RedemptionsRedeemableId + body;
			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + access_token);
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.mobApi2RedemptionsRedeemableId);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// To send rewards to user
	public Response sendMessageToUser(String userID, String authToken, String reward_amount, String reedemable_id) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Send message to user======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2SendMessageToUser(userID, reward_amount, reedemable_id);
			// Headers
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + authToken);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.sendMessageToUser);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// To send points to user via new support gifting api
	public Response sendPointsToUserViaNewSupportGiftingAPI(String userID, String authToken, String gift_count) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Send points to user======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2SendPointsToUser(userID, gift_count);
			// Headers
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + authToken);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.newSupportGifting);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// To send offer redeemable_id to user via new support gifting api
	public Response sendOfferToUserViaNewSupportGiftingAPI(String userID, String authToken, String redeemable_id,
			String end_date) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Send offer to user======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2SendOfferToUser(userID, redeemable_id, end_date);
			// Headers
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + authToken);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.newSupportGifting);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// To send currency to user via new support gifting api
	public Response sendCurrencyToUserViaNewSupportGiftingAPI(String userID, String authToken, String rewardAmount) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Send currency to user======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2SendCurrencyToUser(userID, rewardAmount);
			// Headers
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + authToken);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.newSupportGifting);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// To send points and redemable both to user via new support gifting api
	public Response sendPointsRedeembalesBothToUserViaNewSupportGiftingAPI(String userID, String authToken,
			String redeemable_id, String gift_count) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Send points to user======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2SendPointsRedeemableToUser(userID, redeemable_id, gift_count);
			// Headers
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + authToken);
			request.body(body);
			response = request.post(ApiConstants.newSupportGifting);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// To send points to user via new support gifting api with invalid json
	public Response sendPointsToUserViaNewSupportGiftingAPIInvalidJson(String userID, String authToken,
			String gift_count) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Send points to user======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2SendPointsToUserInvalidJson(userID, gift_count);
			// Headers
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + authToken);
			request.body(body);
			response = request.post(ApiConstants.newSupportGifting);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// To send points wit location to user via new support gifting api with invalid
	// json
	public Response sendPointsWithLovationToUserViaNewSupportGiftingAPIInvalidJson(String userID, String authToken,
			String gift_count, String location_id) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Send points to user======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2SendPointsWitLocationToUser(userID, gift_count, location_id);
			// Headers
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + authToken);
			request.body(body);
			response = request.post(ApiConstants.newSupportGifting);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// To send points to user via support gifting api with exclude flag as false
	public Response sendPointsToUserWithExcludeFlagFalse(String userID, String authToken, String gift_count) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Send points to user======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2SendPointsToUserWithExcludeFlagFalse(userID, gift_count);
			// Headers
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + authToken);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.newSupportGifting);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// To send points to user via new support gifting api with exclude flag as true
	public Response sendPointsToUserWithExcludeFlagTrue(String userID, String authToken, String gift_count) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Send points to user======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2SendPointsToUserWithExcludeFlagTrue(userID, gift_count);
			// Headers
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + authToken);
			request.body(body);
			response = request.post(ApiConstants.newSupportGifting);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// To send offer to user via new support gifting api with exclude flag
	public Response sendOfferToUserWithExcludeFlag(String userID, String authToken, String redeemable_id,
			String end_date) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Send offer to user======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2SendOfferToUserWithExcludeFlag(userID, redeemable_id, end_date);
			// Headers
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + authToken);
			request.body(body);
			response = request.post(ApiConstants.newSupportGifting);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2RedemptionWithVisit(String client, String secret, String access_token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2RedemptionWitReedemable_id(client);
			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2RedemptionsUsingVisits + body;
			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + access_token);
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.mobApi2RedemptionsUsingVisits);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2RedemptionWithBankedCurrency(String client, String secret, String access_token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2RedemptionWithBankedCurrency(client);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2RedemptionsUsingBankedCurrency + body;

			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + access_token);
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.mobApi2RedemptionsUsingBankedCurrency);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authListAllDeals(String authToken, String client, String secret) {
		Response response = null;
		// requestParams = new JSONObject();
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given();
			request.queryParam("authentication_token", authToken);
			request.queryParam("client", client);
			String URL = ApiConstants.authDeals + "?authentication_token=" + authToken + "&client=" + client;
			String signature = apiUtils.getSignatureSHA1(secret, URL);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			request.headers(defaultHeaderMap);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			response = request.get(ApiConstants.authDeals);
			int i = 0;
			while (i < 60) {
				if (response.asString().equals("[]")) {
					Thread.sleep(1000);
					response = request.get(ApiConstants.authDeals);
				} else {
					break;
				}
				i++;
			}
			logger.info(response.statusCode());
			logger.info(response.asPrettyString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2RedemptionWithRewardId(String client, String secret, String access_token, String reward_id) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2RedemptionWithRewardId(client, reward_id);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2RedemptionsUsingRewardId + body;

			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + access_token);
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.mobApi2RedemptionsUsingRewardId);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2GiftRewardToUser(String client, String secret, String reward_id, String email, String token) {
		Response response = null;
		// requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2GiftReardtoOtherUser(email, reward_id);
			request.body(body);

			String URL = ApiConstants.mobApi2TransferReward + "?client=" + client;
			String payload = URL + body;

			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + token);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.body(body);

			response = request.post(ApiConstants.mobApi2TransferReward);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2GiftAmountToUser(String client, String secret, String email, String token) {
		Response response = null;
		// requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2GiftAmounttoOtherUser(email);
			request.body(body);

			String URL = ApiConstants.mobApi2TransferCurrency + "?client=" + client;
			String payload = URL + body;

			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + token);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.body(body);

			response = request.post(ApiConstants.mobApi2TransferCurrency);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	/*
	 * public Response Api2ListAllDeals(String client, String secret, String token)
	 * { Response response = null; requestParams = new JSONObject(); try {
	 * RestAssured.baseURI = baseUri; RequestSpecification request =
	 * RestAssured.given().log().all();
	 * 
	 * String body = apipaylods.API2ClientPayload(client);
	 * 
	 * // Headers passing Signature and content type String payload =
	 * ApiConstants.mobApi2ListAllDeals + body;
	 * 
	 * // Headers request.header("x-pch-digest", api2Headers.api2Signature(payload,
	 * secret)); request.header("Authorization", "Bearer " + token);
	 * request.header("Content-Type", api2Headers.api2ContentType());
	 * 
	 * request.body(body);
	 * 
	 * response = request.get(ApiConstants.mobApi2ListAllDeals);
	 * logger.info("API Response:" + response.asString());
	 * TestListeners.extentTest.get().info("response is : "+ response.asString());
	 * logger.info(response.statusCode()); } catch (Exception e) {
	 * logger.error(e.getMessage()); } return response; }
	 */

	/*
	 * public Response Api2getDetailsofDeals(String client, String secret, String
	 * token, String redeemable_uuid) { Response response = null; requestParams =
	 * new JSONObject(); try { RestAssured.baseURI = baseUri; RequestSpecification
	 * request = RestAssured.given().log().all();
	 * 
	 * String body = apipaylods.API2ClientPayload(client);
	 * 
	 * // Headers passing Signature and content type String payload =
	 * ApiConstants.mobApi2GetDetailsDeals + "/" + redeemable_uuid + body;
	 * 
	 * // Headers request.header("x-pch-digest", api2Headers.api2Signature(payload,
	 * secret)); request.header("Authorization", "Bearer " + token);
	 * request.header("Content-Type", api2Headers.api2ContentType());
	 * 
	 * request.body(body);
	 * 
	 * response = request.get(ApiConstants.mobApi2GetDetailsDeals + "/" +
	 * redeemable_uuid); logger.info("API Response:" + response.asString());
	 * TestListeners.extentTest.get().info("response is : "+ response.asString());
	 * logger.info(response.statusCode()); } catch (Exception e) {
	 * logger.error(e.getMessage()); } return response; }
	 */

	public Response Api2PointConversion(String client, String secret, String token, String conversionRuleId) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.API2PointConversionPayload(client, conversionRuleId);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2PointConversion + body;

			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);

			response = request.post(ApiConstants.mobApi2PointConversion);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authGrantLoyaltyCheckinAgainstReciept(String authToken, String amount, String client,
			String secret) {
		Response response = null;
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.authGrantLoyaltyPayload(authToken, client, amount);
			logger.info(body);
			String payload = ApiConstants.onlineOrderCheckin + body;
			request.headers(header.onlineOrderCheckinHeader(payload.toString(), secret));
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.onlineOrderCheckin);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2UserAccountHistory(String client, String secret, String token) {
		Response response = null;
		// requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.API2ClientPayload(client);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2UserAccountHistory + body;

			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);

			response = request.get(ApiConstants.mobApi2UserAccountHistory);
			// TestListeners.extentTest.get().info("API response: " + response.asString());
			logger.info("API Response:" + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// public Response authFetchRedemption(String authToken, String client, String
	// secret) {
	// Response response = null;
	// requestParams = new JSONObject();
	// Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
	// try {
	// RestAssured.baseURI = baseUri;
	// RequestSpecification request = RestAssured.given().log().all();
	// request.queryParam("authentication_token", authToken);
	// requestParams.put("client", client);
	// param = requestParams.toString();
	// String URL = ApiConstants.authFetchRedemption + "?authentication_token=" +
	// authToken;
	// String payload = URL + param;
	// request.body(requestParams.toJSONString());
	// String signature = apiUtils.getSignatureSHA1(secret, payload);
	// defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
	// defaultHeaderMap.put("Accept", prop.getProperty("contentType"));
	// defaultHeaderMap.put("x-pch-digest", signature);
	// request.headers(defaultHeaderMap);
	// response = request.get(ApiConstants.authFetchRedemption);
	// } catch (Exception e) {
	// logger.error(e.getMessage());
	// }
	// return response;
	// }

	public Response authOnlineBankCurrencyRedemption(String authentication_token, String client, String secret) {
		Response response = null;
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.authOnlineBankCurrencyRedemptionPayload(authentication_token, client);
			String payload = ApiConstants.authOnlineRedemption + body;
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			request.headers(defaultHeaderMap);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.authOnlineRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2ListAllDeals(String client, String secret, String token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.reset();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// Query param
			request.queryParam("client", client);
			// Payload
			String payload = ApiConstants.mobApi2ListAllDeals + "?client=" + client;
			// Headers
			Thread.sleep(60000);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + token);
			request.header("Connection", "keep-alive");
			request.header("User-Agent", "PostmanRuntime/7.28.4");
			request.header("cache-control", "no-cache");
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			response = request.get(ApiConstants.mobApi2ListAllDeals);

			logger.info(response.asPrettyString());
			Thread.sleep(20000);
			// logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			int i = 0;
			while (i < 60) {
				if (response.asString().equals("[]") || response.asString().contains("unsynced_user")) {
					Thread.sleep(1000);
					response = request.get(ApiConstants.mobApi2ListAllDeals);
				} else {
					break;
				}
				i++;
			}
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2getDetailsofDeals(String client, String secret, String token, String redeemable_uuid) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// Query param
			request.queryParam("client", client);
			// request.pathParam("redeemable_uuid", redeemable_uuid);
			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2GetDetailsDeals + "/" + redeemable_uuid + "?client=" + client;
			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + token);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			response = request.get(ApiConstants.mobApi2GetDetailsDeals + "/" + redeemable_uuid);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2SaveSelectedDeal(String client, String secret, String token, String redeemable_uuid) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// Query param
			request.queryParam("client", client);
			request.queryParam("redeemable_uuid", redeemable_uuid);
			// Payload
			String payload = ApiConstants.mobApi2SaveSelectedDeal + "?client=" + client + "&redeemable_uuid="
					+ redeemable_uuid;
			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + token);
			request.header("Connection", "keep-alive");
			request.header("User-Agent", "PostmanRuntime/7.28.4");
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			response = request.post(ApiConstants.mobApi2SaveSelectedDeal);
			Thread.sleep(2000);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2LoyaltyCheckinReceiptImage(String client, String secret, String token, String locationid) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2LoyaltyCheckinReceiptImage(client, token, locationid);
			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2CheckinReceiptImage + body;
			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.mobApi2CheckinReceiptImage);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authOnlineCardBasedRedemption(String authentication_token, String client, String secret) {
		Response response = null;
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.authOnlineBankCurrencyRedemptionPayload(authentication_token, client);
			String payload = ApiConstants.authOnlineRedemption + body;
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			request.headers(defaultHeaderMap);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.authOnlineRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2LoyaltyCheckinBarCode(String client, String secret, String token, String barCode) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2LoyaltyCheckinBarCode(barCode, client);
			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2CheckinBarCode + body;
			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.body(body);
			response = request.post(ApiConstants.mobApi2CheckinBarCode);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2LoyaltyCheckinQRCode(String client, String secret, String token, String barCode) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2LoyaltyCheckinQRCode(barCode, client, token);
			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2CheckinQRCode + body;
			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.body(body);
			response = request.post(ApiConstants.mobApi2CheckinQRCode);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authOnlineRedeemableRedemption(String authentication_token, String client, String secret,
			String item_id, String redeemable_id) {
		Response response = null;
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.authOnlineRedeemableRedemptionPayload(authentication_token, client, item_id,
					redeemable_id);
			String payload = ApiConstants.authOnlineRedemption + body;
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			request.headers(defaultHeaderMap);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.authOnlineRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2FetchCheckin(String client, String secret, String token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2ClientPayload(client);
			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2FetchCheckin + body;
			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept", api2Headers.api2Accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.get(ApiConstants.mobApi2FetchCheckin);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("Mobile API2 Fetch Checkin API response: " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authOnlineCouponPromoRedemption(String authentication_token, String client, String secret,
			String couponCode) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
			String body = apipaylods.authOnlineCouponPromoRedemptionPayload(authentication_token, client, couponCode);
			String payload = ApiConstants.authOnlineRedemption + body;
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			request.headers(defaultHeaderMap);
			request.body(body);
			response = request.post(ApiConstants.authOnlineRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2AccountBalance(String client, String secret, String token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2ClientPayload(client);
			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2AccountBalance + body;
			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.get(ApiConstants.mobApi2AccountBalance);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2Trasactiondetails(String client, String secret, String token, String checkin_id) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2TransactionDetails(client, checkin_id);
			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2TransactionDetails + body;
			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept", api2Headers.api2Accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.get(ApiConstants.mobApi2TransactionDetails);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// public Response wifiUserSignUp(String email, String client, String secret) {
	// Response response = null;
	// requestParams = new JSONObject();
	// try {
	// RestAssured.baseURI = baseUri;
	// RequestSpecification request = RestAssured.given().log().all();
	// String body =
	// apipaylods.authOnlineCouponPromoRedemptionPayload(authentication_token,
	// client, couponCode);
	// String payload = ApiConstants.authOnlineRedemption + body;
	// String signature = apiUtils.getSignatureSHA1(secret, payload);
	// defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
	// defaultHeaderMap.put("x-pch-digest", signature);
	// request.headers(defaultHeaderMap);
	// request.body(body);
	// response = request.post(ApiConstants.authOnlineRedemption);
	// logger.info("API Response:" + response.asString());
	// TestListeners.extentTest.get().info("response is : "+ response.asString());
	// logger.info("Response code: "+response.getStatusCode());
	//
	// } catch (Exception e) {
	// logger.error(e.getMessage());
	// }
	// return response;
	// }

	public Response wifiUserSignUp(String email, String client, String secret, String locationId) {
		Response response = null;
		requestParams = new JSONObject();
		try {

			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("email", email);
			hashmap.put("password", Utilities.getApiConfigProperty("password"));
			hashmap.put("first_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("firstName")));
			hashmap.put("last_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("lastName")));
			hashmap.put("phone", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("firstName")));
			hashmap.put("address_line1", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("lastName")));
			hashmap.put("state", Utilities.getApiConfigProperty("state"));
			hashmap.put("zip_code", Utilities.getApiConfigProperty("zipCode"));
			hashmap.put("birthday", Utilities.getApiConfigProperty("birthday"));
			hashmap.put("marketing_email_subscription", Utilities.getApiConfigProperty("marketingEmailSubscription"));
			if (env.equalsIgnoreCase("qa")) {
				hashmap.put("x-px-access-token", cloudFareToken);
				hashmap.put("x-bypass-cloudflare-waf", cloudFareBypass);
			}
			requestParams.put("user", hashmap);
			requestParams.put("location_id", locationId);
			requestParams.put("client", client);
			param = requestParams.toString();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.wifiSignUp + "?client=" + client + "&hash=" + epoch;
			String payload = URL + param;
			request.headers(header.api2LoginHeader(payload, secret));
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.post(ApiConstants.wifiSignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2ListOffers(String client, String secret, String token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.API2ClientPayload(client);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2ListUserOffers + body;

			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);

			response = request.get(ApiConstants.mobApi2ListUserOffers);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2ListApplicableOffers(String client, String secret, String token, String location_id) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2ListApplicableOffers();
			// request.body(body);

			String URL = ApiConstants.mobApi2ListApplicableOffers + "?client=" + client + "&location_id=" + location_id;
			String payload = URL + body;
			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("location_id", location_id);
			request.body(body);
			response = request.get(ApiConstants.mobApi2ListApplicableOffers);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1PurchaseGiftCard(String email, String client, String secret, String amount, String token,
			String cardId) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API1PurchaseGiftcard(amount, cardId);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1PurchaseGiftCard + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());

			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1PurchaseGiftCard);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1ReloadGiftCard(String email, String client, String secret, String amount, String token,
			String uid, String trToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API1ReloadGiftcard(amount, trToken);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1ReloadGiftCard + uid + "/reload" + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());

			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1ReloadGiftCard + uid + "/reload");
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1ShareGiftCard(String email, String client, String secret, String token, String uid) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API1Email(email);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1ReloadGiftCard + uid + "/share" + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1ReloadGiftCard + uid + "/share");
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1TransferGiftCard(String email, String client, String secret, String amount, String token,
			String uid) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API1TransferGiftCard(email, amount);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1ReloadGiftCard + uid + "/transfer" + "?client=" + client + "&hash="
					+ epoch;
			String payload = URL + body;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1ReloadGiftCard + uid + "/transfer");
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1LoyaltyCheckinBarCode(String client, String secret, String token, String barCode) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API1BarCodeCheckin(barCode);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1BarCodeCheckin + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;
			// Headers
			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());

			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1BarCodeCheckin);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response dashboardApiEClubUpload(String email, String token, String storeNumber, Boolean flag) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.elubGuestPayloadWithEmail(email, storeNumber, flag);
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept", api1Headers.accept());
			request.body(body);
			response = request.post(ApiConstants.eClubGuestUpload);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response dashboardApiEClubUploadWithPhone(String phone, String token, String storeNumber, String email) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.elubGuestPayloadWithPhone(phone, storeNumber, email);
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept", api1Headers.accept());
			request.body(body);
			response = request.post(ApiConstants.eClubGuestUpload);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public String isDigit(String str) {
		char ch[] = str1.toCharArray();
		str2 = str2 + ch[0];
		str2 = str2 + ch[1];
		for (int i = 2; i < ch.length - 4; i++) {

			str2 = str2 + '*';
		}
		str2 = str2 + ch[12];
		str2 = str2 + ch[13];
		str2 = str2 + ch[14];
		str2 = str2 + ch[15];

		return str2;
	}

	public Response Api1UserSignUpZiplineMasking(String email, String client, String secret) {
		str1 = new FetchCard().getCardNo();
		String str3 = isDigit(str1);
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("email", email);
			hashmap.put("password", prop.getProperty("password"));
			hashmap.put("first_name", prop.getProperty("firstName"));
			hashmap.put("last_name", prop.getProperty("lastName"));
			hashmap.put("birthDate", prop.getProperty("birthday"));
			hashmap.put("marketing_email_subscription", prop.getProperty("marketingEmailSubscription"));
			hashmap.put("marketing_pn_subscription", prop.getProperty("marketingPnSubscription"));
			hashmap.put("external_source_id", prop.getProperty("externalSourceId"));
			hashmap.put("card_number", str3);
			requestParams.put("user", hashmap);
			param = requestParams.toString();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1SignUp + "?client=" + client + "&hash=" + epoch;
			String payload = URL + param;
			request.headers(header.api1SignUpHeader(payload, epoch, secret));
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.post(ApiConstants.mobApi1SignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			// logger.info("Response code: "+response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;

	}

	// Aayushi
	public Response Api1UserSignUpZiplineUnmasking(String email, String client, String secret) {
		str4 = new FetchCard().getCardNo();
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("email", email);
			hashmap.put("password", prop.getProperty("password"));
			hashmap.put("first_name", prop.getProperty("firstName"));
			hashmap.put("last_name", prop.getProperty("lastName"));
			hashmap.put("birthDate", prop.getProperty("birthday"));
			hashmap.put("marketing_email_subscription", prop.getProperty("marketingEmailSubscription"));
			hashmap.put("marketing_pn_subscription", prop.getProperty("marketingPnSubscription"));
			hashmap.put("external_source_id", prop.getProperty("externalSourceId"));
			hashmap.put("card_number", str4);
			requestParams.put("user", hashmap);
			param = requestParams.toString();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1SignUp + "?client=" + client + "&hash=" + epoch;
			String payload = URL + param;
			request.headers(header.api1SignUpHeader(payload, epoch, secret));
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.post(ApiConstants.mobApi1SignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			// logger.info("Response code: "+response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;

	}

	// Aayushi
	public Response Api2SignUpZiplineMasking(String email, String client, String secret) {
		str1 = new FetchCard().getCardNo();
		String str3 = isDigit(str1);
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			// String phoneNumber =
			// CreateDateTime.getUniqueString(prop.getProperty("countryCode"));
			hashmap.put("email", email);
			hashmap.put("secondary_email", prop.getProperty("secondaryEmail"));
			hashmap.put("password", prop.getProperty("password"));
			hashmap.put("password_confirmation", prop.getProperty("password"));
			hashmap.put("birthDate", prop.getProperty("birthday"));
			hashmap.put("anniversary", prop.getProperty("anniv"));
			hashmap.put("apn_token", prop.getProperty("apnToken"));
			hashmap.put("gcm_token", prop.getProperty("gcmToken"));
			hashmap.put("card_number", str3);
			hashmap.put("fav_location_id", prop.getProperty("favLocation"));
			hashmap.put("phone", prop.getProperty("phoneNo"));
			hashmap.put("first_name", prop.getProperty("firstName"));
			hashmap.put("last_name", prop.getProperty("lastName"));
			requestParams.put("user", hashmap);
			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.mobApi2SignUp + param;
			request.headers(header.api2SignUpHeader(payload, secret));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.mobApi2SignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			// logger.info("Response code: "+response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// Aayushi
	public Response Api2SignUpZiplineUnmasking(String email, String client, String secret) {
		str4 = new FetchCard().getCardNo();
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			// String phoneNumber =
			// CreateDateTime.getUniqueString(prop.getProperty("countryCode"));
			hashmap.put("email", email);
			hashmap.put("secondary_email", prop.getProperty("secondaryEmail"));
			hashmap.put("password", prop.getProperty("password"));
			hashmap.put("password_confirmation", prop.getProperty("password"));
			hashmap.put("birthDate", prop.getProperty("birthday"));
			hashmap.put("anniversary", prop.getProperty("anniv"));
			hashmap.put("apn_token", prop.getProperty("apnToken"));
			hashmap.put("gcm_token", prop.getProperty("gcmToken"));
			hashmap.put("card_number", str4);
			hashmap.put("fav_location_id", prop.getProperty("favLocation"));
			hashmap.put("phone", prop.getProperty("phoneNo"));
			hashmap.put("first_name", CreateDateTime.getUniqueString(prop.getProperty("firstName")));
			hashmap.put("last_name", CreateDateTime.getUniqueString(prop.getProperty("lastName")));
			requestParams.put("user", hashmap);
			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.mobApi2SignUp + param;
			request.headers(header.api2SignUpHeader(payload, secret));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.mobApi2SignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			// logger.info("Response code: "+response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiSignUpAzureValidation(String email, String client, String secret, String external_source,
			String external_source_id) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("email", email);
			hashmap.put("password", prop.getProperty("password"));
			hashmap.put("first_name", CreateDateTime.getUniqueString(prop.getProperty("firstName")));
			hashmap.put("last_name", CreateDateTime.getUniqueString(prop.getProperty("lastName")));
			hashmap.put("external_source", external_source);
			hashmap.put("external_source_id", external_source_id);
			requestParams.put("user", hashmap);
			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.authApiSignUp + param;
			// Headers passing
			request.headers(header.authAPISignUpHeader(payload, secret));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.authApiSignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	/*
	 * public Response Api1CreatePickupOrder(String client, String secret, String
	 * token, String email, String orderId, String deliveryMethod) { Response
	 * response = null; requestParams = new JSONObject(); try { RestAssured.baseURI
	 * = baseUri; RequestSpecification request = RestAssured.given().log().all();
	 * String timeReady = CreateDateTime.get15MinAheadTime(); String body =
	 * apipaylods.Api1CreatePickupOrderPayload(email, orderId, timeReady,
	 * deliveryMethod); String epoch = apiUtils.getSpan(); String URL =
	 * ApiConstants.mobApi1createPickupOrder + "?client=" + client + "&hash=" +
	 * epoch; String payload = URL + body; // Headers request.header("x-pch-digest",
	 * api1Headers.signature(payload, secret)); request.header("x-pch-hash",
	 * api1Headers.hash(secret, epoch)); request.header("Authorization", "Bearer " +
	 * token); request.header("Content-Type", api1Headers.contentType());
	 * request.header("Accept-Language", api1Headers.acceptLanguage());
	 * request.header("Accept", api1Headers.accept());
	 * 
	 * request.queryParam("client", client); request.queryParam("hash", epoch);
	 * request.body(body); response =
	 * request.post(ApiConstants.mobApi1createPickupOrder);
	 * logger.info("API Response:" + response.asString());
	 * TestListeners.extentTest.get().info("response is : "+ response.asString());
	 * logger.info(response.statusCode()); } catch (Exception e) {
	 * logger.error(e.getMessage()); } return response; }
	 */

	/*
	 * public Response Api1CreatePickupOrder(String client, String secret, String
	 * token, String email, String orderId, String deliveryMethod) {
	 * request.queryParam("client", client); request.queryParam("hash", epoch);
	 * request.body(body); response =
	 * request.post(ApiConstants.mobApi1createPickupOrder);
	 * logger.info("API Response:" + response.asString());
	 * TestListeners.extentTest.get().info("response is : "+ response.asString());
	 * logger.info(response.statusCode()); }catch(
	 * 
	 * Exception e) { logger.error(e.getMessage()); }return response; }
	 */

	public Response Api1CreateCurbsideOrder(String client, String secret, String token, String email, String orderId,
			String deliveryMethod, String vechicleid) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String timeReady = CreateDateTime.get15MinAheadTimeInIST();
			String body = apipaylods.Api1CreateCurbsideOrderPayload(email, orderId, timeReady, deliveryMethod,
					vechicleid);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1createPickupOrder + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;
			// Headers
			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1createPickupOrder);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}
	// @author=Rajasekhar added on 3/may/2024

	public Response MenuOrderingtoken(String client, String secret, String token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======Menu Ordering token api======");
			TestListeners.extentTest.get().info("======Menu Ordering token api======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String payload = ApiConstants.menuorderingtoken + "?client=" + client + "&hash=" + epoch;
			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("Authorization", "Bearer " + token);
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.get(ApiConstants.menuorderingtoken);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// @author=Rajasekhar added on 3/may/2024
	public Response MenuFetchUserProfile(String menuid, String menuaccesstoken, String appkey) {
		Response response = null;
		requestParams = new JSONObject();
		// String epoch = apiUtils.getSpan();
		try {
			logger.info("Menuid" + menuid);
			RestAssured.baseURI = "https://api-playground.menu.app";
			RequestSpecification request = RestAssured.given().log().all();
			// request.queryParam(menuid);
			@SuppressWarnings("unused")
			String complete_endpoint = ApiConstants.MenuFetchUserProfile + menuid;
			// System.out.println("Endpoint url"+complete_endpoint);
			request.header("Application", appkey);
			// logger.info("Endpoint url"+complete_endpoint);
			// request.header("Application", "PunchhTeamMobilePgPreproduction");
			request.header("Authorization", "Bearer" + menuaccesstoken);
			// request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			response = request.get(complete_endpoint);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// @author=Rajasekhar
	public Response Api1CreateVehicle(String client, String secret, String token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.Api1CreateVehiclePayload();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1createVehicle + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;
			// Headers
			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1createVehicle);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// @author=Rajasekhar
	public Response Api1VehicleList(String client, String secret, String token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1createVehicle + "?client=" + client + "&hash=" + epoch;
			String payload = URL;
			// Headers
			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			// request.body(body);
			response = request.get(ApiConstants.mobApi1createVehicle);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// @author=Rajasekhar
	public Response ApiUpdateCustomerStatus(String client, String secret, String token, String cus_status,
			String punchh_orderid) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// String timeReady=CreateDateTime.get15MinAheadTimeInIST();
			String body = apipaylods.ApiUpdateCustomerStatusPayload(cus_status);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.updateCustomerStatus + punchh_orderid + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;
			// Headers
			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.put(ApiConstants.updateCustomerStatus + punchh_orderid);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// @author=Rajasekhar
	public Response Api1CreatePickupOrder(String client, String secret, String token, String email, String orderId,
			String deliveryMethod, String StoreNumber) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;

			RequestSpecification request = RestAssured.given().log().all();
			String timeReady = CreateDateTime.get15MinAheadTimeInIST();
			String body = apipaylods.Api1CreatePickupOrderPayload(email, orderId, timeReady, deliveryMethod,
					StoreNumber);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1createPickupOrder + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;
			// Headers
			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1createPickupOrder);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// public Response ApiUpdateCustomerStatus(String client, String secret, String
	// token, String cus_status,
	// String punchh_orderid) {
	// Response response = null;
	// requestParams = new JSONObject();
	// try {
	// RestAssured.baseURI = baseUri;
	// RequestSpecification request = RestAssured.given().log().all();
	// // String timeReady=CreateDateTime.get15MinAheadTimeInIST();
	// String body = apipaylods.ApiUpdateCustomerStatusPayload(cus_status);
	// String epoch = apiUtils.getSpan();
	// String URL = ApiConstants.updateCustomerStatus + punchh_orderid + "?client="
	// + client + "&hash=" + epoch;
	// String payload = URL + body;
	// // Headers
	// request.header("x-pch-digest", api1Headers.signature(payload, secret));
	// request.header("x-pch-hash", api1Headers.hash(secret, epoch));
	// request.header("Authorization", "Bearer " + token);
	// request.header("Content-Type", api1Headers.contentType());
	// request.header("Accept-Language", api1Headers.acceptLanguage());
	// request.header("Accept", api1Headers.accept());
	// request.queryParam("client", client);
	// request.queryParam("hash", epoch);
	// request.body(body);
	// response = request.put(ApiConstants.updateCustomerStatus + punchh_orderid);
	// logger.info("API Response:" + response.asString());
	// TestListeners.extentTest.get().info("response is : "+ response.asString());
	// logger.info(response.statusCode());
	// } catch (Exception e) {
	// logger.error(e.getMessage());
	// }
	// return response;
	// }

	// @author=Rajasekhar
	public Response ApiUpdateCustomerStatuswithArrivalInfo(String client, String secret, String token,
			String cus_status, String punchh_orderid) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// String timeReady=CreateDateTime.get15MinAheadTimeInIST();
			String body = apipaylods.ApiUpdateCustomerStatusPayload1(cus_status);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.updateCustomerStatus + punchh_orderid + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;
			// Headers
			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.put(ApiConstants.updateCustomerStatus + punchh_orderid);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2SignUpWithExternalSourceAndID(String useremail, String client, String secret,
			String external_source_id, String external_source) {
		String email = null;
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			// String phoneNumber =
			// CreateDateTime.getUniqueString(prop.getProperty("countryCode"));
			email = CreateDateTime.getCurrentSystemDateAndYear() + email;
			// source_id=CreateDateTime.getUniqueString(source_id);
			hashmap.put("email", useremail);
			hashmap.put("password", prop.getProperty("password"));
			// hashmap.put("phone", phoneNumber);
			hashmap.put("first_name", CreateDateTime.getUniqueString(prop.getProperty("firstName")));
			hashmap.put("last_name", CreateDateTime.getUniqueString(prop.getProperty("lastName")));
			hashmap.put("zip_code", prop.getProperty("zipCode"));
			hashmap.put("marketing_email_subscription", "true");
			hashmap.put("signup_channel", prop.getProperty("signUpChannel"));
			hashmap.put("privacy_policy", prop.getProperty("privatePolicy"));
			hashmap.put("external_source", external_source);
			hashmap.put("external_source_id", external_source_id);
			requestParams.put("user", hashmap);
			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.mobApi2SignUp + param;
			request.headers(header.api2SignUpHeader(payload, secret));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.mobApi2SignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response ecrmPosCheckin(String email, String locationkey, String key, String txn_no) {
		Response response = null;
		requestParams = new JSONObject();

		String date = CreateDateTime.getCurrentDate() + "T03:14:00-08:00";
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			String body = apipaylods.posCheckinPayload(date, email, key, txn_no);
			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request1.header("Authorization", "Token token=" + locationkey);
			request1.body(body);
			response = request1.post(ApiConstants.posEcrmCheckin);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response listAllCustomSegments(String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("List all Custom segments API");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + authorization);
			request.body(requestParams.toJSONString());
			response = request.get(ApiConstants.customSegments);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response createCustomSegment(String segmentName, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("List all Custom segments API");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + authorization);
			requestParams.put("name", segmentName);
			requestParams.put("description", "Auto custom segment " + segmentName);
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.customSegments);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response updateCustomSegment(String segmentName, String authorization, int segmentId) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("Updating Custom segment");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.queryParam("name", segmentName);
			request.queryParam("description", "Updated auto custom segment " + segmentName);
			request.queryParam("custom_segment_id", segmentId);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + authorization);
			response = request.patch(ApiConstants.customSegments);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response deletingCustomSegment(String authorization, int segmentId) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("Deleting Custom segment");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.queryParam("custom_segment_id", segmentId);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + authorization);
			response = request.delete(ApiConstants.customSegments);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response getCustomSegmentDetails(String authorization, int segmentId) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("Get custom segment details");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.queryParam("custom_segment_id", segmentId);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + authorization);
			response = request.get(ApiConstants.customSegments);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response addUserToCustomSegment(int segmentId, String userEmail, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("List all Custom segments API");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + authorization);
			requestParams.put("custom_segment_id", segmentId);
			requestParams.put("email", userEmail);
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.customSegmentMembers);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response searchUserExistsInSegment(int customSegmentId, String userEmail, int userId, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("Updating Custom segment");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.queryParam("email", userEmail);
			request.queryParam("user_id", userId);
			request.queryParam("custom_segment_id", customSegmentId);
			request.header("Accept", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + authorization);
			response = request.get(ApiConstants.customSegmentMembers);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response deletingUserFromCustomSegment(String authorization, int segmentId, String userEmail, int userId) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("Deleting Custom segment");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.queryParam("custom_segment_id", segmentId);
			request.queryParam("email", userEmail);
			request.queryParam("user_id", userId);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + authorization);
			response = request.delete(ApiConstants.customSegmentMembers);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response guestLookupWifiEnrollment(String email, String client, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			param = requestParams.toString();
			request.headers("Authorization", "Bearer " + authorization);
			request.queryParam("client", client);
			request.queryParam("email", email);
			if (env.equals("qa")) {
				request.headers("x-px-access-token", Utilities.getApiConfigProperty("cloudFareToken"));
				request.headers("x-bypass-cloudflare-waf", Utilities.getApiConfigProperty("cloudFareBypass"));
			}
			response = request.get(ApiConstants.wifiSignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1Cards(String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// String body = apipaylods.API1PurchaseGiftcard(amount);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1cards + "?client=" + client + "&hash=" + epoch;
			String payload = URL;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			// request.body(body);
			response = request.get(ApiConstants.mobApi1cards);
			logger.info("Meta API v1 Response: " + response.asString());
			TestListeners.extentTest.get().info("Meta API v1 response: " + response.asString());
			logger.info("Meta API v1 Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2Cards(String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String URL = ApiConstants.mobApi2cards + "?client=" + client;
			String payload = URL;

			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			response = request.get(ApiConstants.mobApi2cards);
			logger.info("Meta API v2 Response: " + response.asString());
			TestListeners.extentTest.get().info("Meta API v2 response: " + response.asString());
			logger.info("Meta API v2 Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response RegApplicableOffers(String client, String secret, String token, String location_id, String itemQty,
			String itemAmount) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.regApplicableOffers(itemQty, itemAmount);
			String URL = ApiConstants.mobApi2ListApplicableOffers + "?client=" + client + "&location_id=" + location_id;
			String payload = URL + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.queryParam("client", client);
			request.queryParam("location_id", location_id);
			request.body(body);
			response = request.get(ApiConstants.mobApi2ListApplicableOffers);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApplicableOffers(String client, String secret, String authToken, String itemQty,
			String itemAmount) {
		Response response = null;
		requestParams = new JSONObject();
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.AuthListApplicableOffers(itemQty, itemAmount);
			// request.body(body);
			String URL = ApiConstants.authRedemptionsApplicableOffers + "?client=" + client + "&authentication_token="
					+ authToken;
			String payload = URL + body;
			request.queryParam("client", client);
			request.queryParam("authentication_token", authToken);
			request.body(body);
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			defaultHeaderMap.put("Accept", prop.getProperty("contentType"));
			request.headers(defaultHeaderMap);
			response = request.get(ApiConstants.authRedemptionsApplicableOffers);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posApplicableOffers(String userEmail, String locationKey, String itemQty, String itemAmount) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.AuthListApplicableOffers(itemQty, itemAmount);
			request.queryParam("email", userEmail);
			request.queryParam("location_key", locationKey);
			request.body(body);
			request.header("Content-Type", prop.getProperty("contentType"));
			request.header("Accept", prop.getProperty("contentType"));
			// request.headers(defaultHeaderMap);
			response = request.get(ApiConstants.posApplicableOffers);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posActiveRedemptions(String userEmail, String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// String body = apipaylods.AuthListApplicableOffers(itemQty, itemAmount);
			request.queryParam("email", userEmail);
			request.queryParam("location_key", locationKey);
			// request.body(body);
			request.header("Content-Type", prop.getProperty("contentType"));
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			// request.header("Accept", prop.getProperty("contentType"));
			// request.headers(defaultHeaderMap);
			response = request.get(ApiConstants.posActiveRedemptions);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response dashboardRedeemableList(String token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// String body = apipaylods.posRedemptionofRewardPayload(email, date, key, txn,
			// locationKey, rewardId);
			request.queryParam("per_page", 2000);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + token);
			// request.body(body);
			response = request.get(ApiConstants.redeemableList);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response dashboardSegmentList(String token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// request.queryParam("page", 1);
			// request.queryParam("query", "test 1");
			// request.queryParam("per_page", 2);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + token);
			response = request.get(ApiConstants.segmentList);
			logger.info("API Response:" + response.asString());
			// TestListeners.extentTest.get().info("API response is : " +
			// response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response dashboardSegmentList(String token, String query) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// request.queryParam("page", 1);
			request.queryParam("query", query);
			request.queryParam("per_page", 20000);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + token);
			response = request.get(ApiConstants.segmentList);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response dashboardSegmentList(String token, String query, int per_page) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// request.queryParam("page", 1);
			request.queryParam("query", query);
			request.queryParam("per_page", per_page);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + token);
			response = request.get(ApiConstants.segmentList);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response dashboardSegmentList(String token, String query, int per_page, int page) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// request.queryParam("page", 1);
			request.queryParam("query", query);
			request.queryParam("per_page", per_page);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + token);
			response = request.get(ApiConstants.segmentList);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response pageDashboardSegmentList(String token, String query, int page) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.queryParam("page", page);
			request.queryParam("query", query);
			request.queryParam("per_page", 2);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + token);
			response = request.get(ApiConstants.segmentList);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response createMassCampaign(String token, String redeemableUuId, String segmentId, String startTime) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.createCampaignPayload(redeemableUuId, segmentId, startTime);
			if (startTime.equals("none"))
				body = body.replace(startTimepara, "");
			if (redeemableUuId.equals("none"))
				body = body.replace(redeemableUuid, "");
			if (segmentId.equals("none"))
				body = body.replace(segment_Id, "");
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + token);
			request.body(body);
			response = request.post(ApiConstants.createMassCampaign);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response createMassCampaign(String token, String redeemableUuId, String segmentId, String startTime,
			String CampaignType) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.createCampaignPayload(redeemableUuId, segmentId, startTime, CampaignType);
			if (CampaignType.equals("none"))
				body = body.replace(massCampaign, "");
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + token);
			request.body(body);
			response = request.post(ApiConstants.createMassCampaign);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response createMassCampaignCategoryPara(String token, String redeemableUuId, String segmentId,
			String category) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String startTime = CreateDateTime.get15MinAheadTimeInUtc() + "z";
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.createCampaignCategoryPayload(redeemableUuId, segmentId, startTime, category);
			if (category.equals("none"))
				body = body.replace(categoryTemp, "");
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + token);
			request.body(body);
			response = request.post(ApiConstants.createMassCampaign);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response createMassCampaignCategoryPara(String token, String redeemableUuId, String segmentId,
			String prameterName, String parameterValue) {
		Response response = null;
		requestParams = new JSONObject();
		// String para = "\"" + prameterName + "\": \"none\",";
		String paraVal = "\"" + prameterName + "\": \"" + parameterValue + "\",";
		try {
			String startTime = CreateDateTime.get15MinAheadTimeInUtc() + "z";
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.createCampaignPayload(redeemableUuId, segmentId, startTime);
			if (prameterName.equals("name")) {
				if (parameterValue.equals("none"))
					body = body.replace("\"name\": \"API event\",", "");
				else
					body = body.replace("\"name\": \"API event\",", paraVal);
			}
			if (prameterName.equals("category")) {
				if (parameterValue.equals("none"))
					body = body.replace("\"category\": \"gift_redeemable\",", "");
				else
					body = body.replace("\"category\": \"gift_redeemable\",", paraVal);
			}
			if (prameterName.equals("external_campaign_id")) {
				paraVal = paraVal.replace(",", "");
				if (parameterValue.equals("none"))
					body = body.replace(",\n" + "    \"external_campaign_id\": \"2\"", "");
				else
					body = body.replace("\"external_campaign_id\": \"2\"", paraVal);
			}
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + token);
			request.body(body);
			response = request.post(ApiConstants.createMassCampaign);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response dashboardRedeemableList(String token, String queryString) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// String body = apipaylods.posRedemptionofRewardPayload(email, date, key, txn,
			// locationKey, rewardId);
			request.queryParam("per_page", 2000);
			request.queryParam("query", queryString);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + token);
			// request.body(body);
			response = request.get(ApiConstants.redeemableList);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response dashboardRedeemableList(String token, int page) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// String body = apipaylods.posRedemptionofRewardPayload(email, date, key, txn,
			// locationKey, rewardId);
			if (page > 0) {
				request.queryParam("per_page", page);
			}
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + token);
			// request.body(body);
			response = request.get(ApiConstants.redeemableList);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response dynamicCouponList(String token) {
		// int page = 1000;
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// request.queryParam("per_page", page);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + token);
			// request.body(body);
			response = request.get(ApiConstants.couponCampaignList);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response dynamicCouponList(String token, String query) {
		int page = 1000;
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.queryParam("per_page", page);
			request.queryParam("query", query);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + token);
			// request.body(body);
			response = request.get(ApiConstants.couponCampaignList);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response dynamicCouponList(String token, int perPage) {
		// int page = 1000;
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// request.queryParam("per_page", page);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + token);
			// request.body(body);
			response = request.get(ApiConstants.couponCampaignList);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response dynamicCouponList(String token, String query, int perPage) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.queryParam("per_page", perPage);
			request.queryParam("query", query);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + token);
			// request.body(body);
			response = request.get(ApiConstants.couponCampaignList);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response dynamicCouponList(String token, String query, int perPage, int page) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.queryParam("per_page", perPage);
			request.queryParam("page", page);
			request.queryParam("query", query);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + token);
			// request.body(body);
			response = request.get(ApiConstants.couponCampaignList);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1ListAllDeals(String client, String secret, String token) {
		Response response = null;
		requestParams = new JSONObject();
		String epoch = apiUtils.getSpan();
		try {
			RestAssured.reset();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			String payload = ApiConstants.mobApi1Deals + "?client=" + client + "&hash=" + epoch;
			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("Authorization", "Bearer " + token);
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			response = request.get(ApiConstants.mobApi1Deals);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response getAuthDealList(String authToken, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.queryParam("client", request);
			request.queryParam("authentication_token", authToken);
			requestParams.put("authentication_token", authToken);
			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.authDeals + param;
			// Headers passing
			request.headers(header.authAPISignUpHeader(payload, secret));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.authDeals);
			logger.info(response.statusCode());
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2PostDeals(String client, String secret, String token, String redeemableUuid) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.reset();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.queryParam("client", client);
			request.queryParam("redeemable_uuid", redeemableUuid);
			String payload = ApiConstants.mobApi2ListAllDeals + "?client=" + client + "&redeemable_uuid="
					+ redeemableUuid;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			response = request.post(ApiConstants.mobApi2ListAllDeals);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1PostDeals(String client, String secret, String token, String redeemableUuid) {
		Response response = null;
		requestParams = new JSONObject();
		String epoch = apiUtils.getSpan();
		try {
			RestAssured.reset();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.queryParam("redeemable_uuid", redeemableUuid);
			String payload = ApiConstants.mobApi1Deals + "?client=" + client + "&hash=" + epoch + "&redeemable_uuid="
					+ redeemableUuid;
			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("Authorization", "Bearer " + token);
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());
			response = request.post(ApiConstants.mobApi1Deals);
			logger.info(response.prettyPrint());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authPostDeals(String authToken, String client, String secret, String redeemableUuid) {
		Response response = null;
		// requestParams = new JSONObject();
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given();
			request.queryParam("authentication_token", authToken);
			request.queryParam("client", client);
			request.queryParam("redeemable_uuid", redeemableUuid);
			String URL = ApiConstants.authDeals + "?authentication_token=" + authToken + "&client=" + client
					+ "&redeemable_uuid=" + redeemableUuid;
			String signature = apiUtils.getSignatureSHA1(secret, URL);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			request.headers(defaultHeaderMap);
			response = request.post(ApiConstants.authDeals);
			logger.info(response.statusCode());
			logger.info(response.asPrettyString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response postDynamicCoupon(String token, String email, String campaignUuid) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("campaign_uuid", campaignUuid);
			if (!(email == null))
				requestParams.put("email", email);
			// String body = apipaylods.dynamicCoupunPayload(email, campaignUuid);
			// request.body(body);
			request.body(requestParams.toJSONString());
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", prop.getProperty("contentType"));
			response = request.post(ApiConstants.dynamicCoupon);
			logger.info(response.statusCode());
			utils.logit("Dynamic Coupon Code Generation API response: " + response.asPrettyString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posLocationConfig(String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======POS Location configuration api======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// String body = apipaylods.posRedemptionofAmountPayload();
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Token token=" + locationKey);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			// request.body(body);
			response = request.get(ApiConstants.posLocationConfiguration);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2PurchaseGiftCard(String client, String secret, String access_token, String design_id) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Purchase Gift Card======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2PurchaseGiftCardPayload(client, access_token, design_id);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2PurchaseGiftCard + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.post(ApiConstants.mobApi2PurchaseGiftCard);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2PurchaseGiftCard(String client, String secret, String access_token, String design_id,
			String amount, String expDate, String firstName) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Purchase Gift Card======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2PurchaseGiftCardPayload(client, access_token, design_id, amount, expDate,
					firstName);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2PurchaseGiftCard + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.post(ApiConstants.mobApi2PurchaseGiftCard);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2UpdateGiftCard(String client, String secret, String access_token, String uuid) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Update Gift Card======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2UpdateGiftCardPayload(client);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2UpdateGiftCard + uuid + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.put(ApiConstants.mobApi2UpdateGiftCard + uuid);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2ReloadGiftCard(String client, String secret, String access_token, String uuid, String amount,
			String firstName, String expDate) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Reload Gift Card======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2ReloadGiftCardPayload(client, amount, firstName, expDate);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2ReloadGiftCard + uuid + "/reload" + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Accept", api2Headers.api2Accept());
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.body(requestParams.toJSONString());
			request.body(body);
			response = request.post(ApiConstants.mobApi2ReloadGiftCard + uuid + "/reload");
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2ReloadGiftCard(String client, String secret, String access_token, String uuid) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Reload Gift Card======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2ReloadGiftCardPayload(client);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2ReloadGiftCard + uuid + "/reload" + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Accept", api2Headers.api2Accept());
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());

			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.post(ApiConstants.mobApi2ReloadGiftCard + uuid + "/reload");
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2FetchGiftCard(String client, String secret, String access_token) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Fetch Gift Card======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2FetchGiftCardPayload(client);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2FetchGiftCard + body;

			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.get(ApiConstants.mobApi2FetchGiftCard);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2FetchGiftCardBalance(String client, String secret, String access_token, String uuid) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Fetch Gift Card Balance======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2OnlyClientBodyPayload(client);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2FetchGiftCardBalance + uuid + "/balance" + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Accept", api2Headers.api2Accept());
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.get(ApiConstants.mobApi2FetchGiftCardBalance + uuid + "/balance");
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2GiftCardTransactionHistory(String client, String secret, String access_token, String uuid) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Gift Card Transaction History======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2OnlyClientBodyPayload(client);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2GiftCardTransactionHistory + uuid + "/history" + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Accept", api2Headers.api2Accept());
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.get(ApiConstants.mobApi2GiftCardTransactionHistory + uuid + "/history");
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2TransferGiftCard(String client, String secret, String access_token, String uuid, String email) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Transfer Gift Card======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2TransferGiftCardPayload(client, email);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2TransferGiftCard + uuid + "/transfer" + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Accept", api2Headers.api2Accept());
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.post(ApiConstants.mobApi2TransferGiftCard + uuid + "/transfer");
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2ShareGiftCard(String client, String secret, String access_token, String uuid, String email) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Share Gift Card======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2ShareGiftCardPayload(client, email);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2ShareGiftCard + uuid + "/share" + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Accept", api2Headers.api2Accept());
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.post(ApiConstants.mobApi2ShareGiftCard + uuid + "/share");
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2TipGiftCard(String client, String secret, String access_token, String uuid, String checkin_id) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Tip Via Gift Card======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2TipGiftCardPayload(client, checkin_id);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2TipGiftCard + uuid + "/tip" + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Accept", api2Headers.api2Accept());
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.post(ApiConstants.mobApi2ShareGiftCard + uuid + "/tip");
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2DeleteGiftCard(String client, String secret, String access_token, String uuid) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Delete a Gift Card ======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2OnlyClientBodyPayload(client);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2DeleteGiftCard + uuid + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.delete(ApiConstants.mobApi2FetchGiftCardBalance + uuid);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2GiftaCard(String emeil, String client, String secret, String access_token, String design_id) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2  Gift a Card======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2GiftaCardPayload(emeil, client, design_id);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2GiftaCard + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Accept", api2Headers.api2Accept());
			request.header("Authorization", "Bearer " + access_token);
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Content-Type", api2Headers.api2ContentType());

			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.post(ApiConstants.mobApi2GiftaCard);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2GiftaCard(String email, String client, String secret, String access_token, String design_id,
			String amount, String expDate, String firstName) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2  Gift a Card======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2GiftaCardPayload(email, client, design_id, amount, expDate, firstName);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2GiftaCard + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Accept", api2Headers.api2Accept());
			request.header("Authorization", "Bearer " + access_token);
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Content-Type", api2Headers.api2ContentType());

			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.post(ApiConstants.mobApi2GiftaCard);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2FetchNotifications(String client, String secret, String access_token) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Fetch Notifications ======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2OnlyClientBodyPayload(client);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2FetchNotifications + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.get(ApiConstants.mobApi2FetchNotifications);
			logger.info("API Response:" + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2DeletehNotifications(String client, String secret, String access_token,
			String notification_id) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Delete Notifications ======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2OnlyClientBodyPayload(client);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2DeleteNotifications + notification_id + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.delete(ApiConstants.mobApi2DeleteNotifications + notification_id);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2FetchMessages(String client, String secret, String access_token) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Fetch Messages ======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// String body = apipaylods.API2OnlyClientBodyPayload(client);
			requestParams.put("client", client);
			param = requestParams.toString();

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2FetchMessages + param;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			request.body(requestParams.toJSONString());
			response = request.get(ApiConstants.mobApi2FetchMessages);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2CreateFeedback(String client, String secret, String access_token) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Create Feedback ======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2CreateFeedbackPayload(client);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2CreateFeedBack + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.post(ApiConstants.mobApi2CreateFeedBack);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2UpdateFeedback(String client, String secret, String access_token, String feedback_id) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Update Feedback ======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2UpdateFeedbackPayload(client);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2UpdateFeedBack + "?feedback_id=" + feedback_id + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.post(ApiConstants.mobApi2UpdateFeedBack + "?feedback_id=" + feedback_id);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2FetchClientToken(String client, String secret, String access_token) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Fetch client Token======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2FetchClientToken + "?client=" + client;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			response = request.get(ApiConstants.mobApi2FetchClientToken + "?client=" + client);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2ListChallenges(String client, String secret, String language) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 List Challenges======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2ListChallenges + "?client=" + client;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", language);
			cloudflareToken(request);
			response = request.get(ApiConstants.mobApi2ListChallenges + "?client=" + client);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2ForgotPasscode(String client, String secret, String access_token) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Forgot Passcode======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2OnlyClientBodyPayload(client);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2ForgotPasscode + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.post(ApiConstants.mobApi2ForgotPasscode);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2CreatePasscode(String client, String secret, String access_token, String passcode) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Create Passcode======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2CreatePasscode + "?passcode=" + passcode + "&client=" + client;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			response = request.post(ApiConstants.mobApi2CreatePasscode + "?passcode=" + passcode + "&client=" + client);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2GenerateEpin(String client, String secret, String access_token, String passcode, String uuid) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Generate Epin======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2GenerateEpin + uuid + "/epin" + "?passcode=" + passcode + "&client="
					+ client;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			response = request.get(
					ApiConstants.mobApi2GenerateEpin + uuid + "/epin" + "?passcode=" + passcode + "&client=" + client);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2CreateGiftCardClaimToken(String client, String secret, String access_token, String uuid) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Create Gift Card Claim Token======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.CreateGiftCardClaimToken(uuid);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2CreateGiftCardClaimToken + "?client=" + client + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			request.body(body);
			response = request.post(ApiConstants.mobApi2CreateGiftCardClaimToken + "?client=" + client);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2GetInvitations(String client, String secret, String access_token) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Get Invitations======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2GetInvitations + "?client=" + client;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			response = request.get(ApiConstants.mobApi2GetInvitations + "?client=" + client);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2CheckStatusofclaimtoken(String client, String secret, String access_token, String claim_token) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Check Status of the claim tokens======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2StatusOfClaimToken + "?client=" + client + "&claim_token="
					+ claim_token;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			cloudflareToken(request);
			response = request
					.get(ApiConstants.mobApi2StatusOfClaimToken + "?client=" + client + "&claim_token=" + claim_token);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2Transferclaimtoken(String client, String secret, String access_token, String claim_token) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Transfer a Gift Card Using Invitation Claim Token======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2TransferClaimToken + "?client=" + client + "&claim_token="
					+ claim_token;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			cloudflareToken(request);
			response = request.patch(
					ApiConstants.mobApi2TransferClaimToken + "?client=" + client + "&claim_token=" + claim_token);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2DeleteClaimToken(String client, String secret, String access_token, String invitation_id) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Delete An Invitation Claim Token======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2DeleteClaimToken + invitation_id + "?client=" + client;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			response = request.delete(ApiConstants.mobApi2DeleteClaimToken + invitation_id + "?client=" + client);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2SocialCauseCampaign(String client, String secret, String access_token, String social_cause_id) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Social Cause Campaign Details======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2SocialCauseCampaign + "/" + social_cause_id
					+ "?page=1&per_page=1&query=123456" + "&client=" + client;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			response = request.get(ApiConstants.mobApi2SocialCauseCampaign + "/" + social_cause_id
					+ "?page=1&per_page=1&query=123456" + "&client=" + client);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response createLocation(String name, String store_number, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Create Location======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			String body = apipaylods.createLocation(name, store_number);
			request.body(body);
			response = request.post(ApiConstants.createLocation);
			logger.info(response.prettyPrint());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response createLocationWithTaxRate(String name, String store_number, String authorization, String tax_rate) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			utils.logit("====== Dashboard API Create Location With Tax Rate ======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			String body = apipaylods.createLocationWithTaxRate(name, store_number, tax_rate);
			request.body(body);
			response = request.post(ApiConstants.createLocation);
			logger.info(response.prettyPrint());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response updateLocationTaxRate(String location_id, String store_number, String authorization,
			String tax_rate_updated) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			utils.logit("====== Dashboard API Update Location With Tax Rate ======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			String body = apipaylods.updateLocationTaxRate(location_id, store_number, tax_rate_updated);
			request.body(body);
			response = request.patch(ApiConstants.updateLocation);
			logger.info(response.prettyPrint());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response apiAuthSubscriptionTaxesApplicableTaxes(String client, String planId, String locationId,
			String secret) {
		Response response = null;
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String URL = ApiConstants.authSubscriptionApplicableTaxes;
			String body = apipaylods.subscriptionTaxesPayload(client, planId, locationId);
			String payload = URL + body;
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.body(body);
			response = request.post(ApiConstants.authSubscriptionApplicableTaxes);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response apiMobileSubscriptionTaxesApplicableTaxes(String planId, String locationId, String punchhAppKey,
			String secret) {
		Response response = null;
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String URL = ApiConstants.mobApi1SubscriptionApplicableTaxes;
			String body = apipaylods.mobileSubscriptionTaxesPayload(planId, locationId);
			String payload = URL + body;
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("punchh-app-key", punchhAppKey);
			request.body(body);
			response = request.post(ApiConstants.mobApi1SubscriptionApplicableTaxes);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api2MobileSubscriptionTaxesApplicableTaxes(String client, String planId, String locationId,
			String secret) {
		Response response = null;
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String URL = ApiConstants.mobApi2SubscriptionApplicableTaxes;
			String body = apipaylods.subscriptionTaxesPayload(client, planId, locationId);
			String payload = URL + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.body(body);
			response = request.post(ApiConstants.mobApi2SubscriptionApplicableTaxes);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response getLocationList(String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Get Location List======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			// String body = apipaylods.createLocation(name,store_number);
			// request.body(body);
			// param = requestParams.toString();
			response = request.get(ApiConstants.getLocation);
			logger.info(response.prettyPrint());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response updateLocation(String location_id, String store_number, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Update Location======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			String body = apipaylods.updateLocation(location_id, store_number);
			request.body(body);
			response = request.patch(ApiConstants.updateLocation);
			logger.info(response.prettyPrint());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response getLocationDetails(String location_id, String store_number, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Get Location Details======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			// String body = apipaylods.createLocation(name,store_number);
			// request.body(body);
			// param = requestParams.toString();
			response = request.get(
					ApiConstants.getLocationDetails + "?location_id=" + location_id + "&store_number=" + store_number);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response createLocationgroup(String name, String store_number, String location_id, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Create Location Group======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			String body = apipaylods.createLocationGroup(name, store_number, location_id);
			request.body(body);
			response = request.post(ApiConstants.createLocationgroup);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response updateLocationgroup(String locationGroupName, String locationGroup_id, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Update Location Group======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			String body = apipaylods.updateLocationGroup(locationGroupName + "Updated", locationGroup_id);
			request.body(body);
			response = request.patch(ApiConstants.createLocationgroup);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response deleteLocationgroup(String locationGroup_id, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Delete Location Group======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			String body = apipaylods.deleteLocationGroup(locationGroup_id);
			request.body(body);
			response = request.delete(ApiConstants.createLocationgroup);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response getLocationGroupList(String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Get Location Group List======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			response = request.get(ApiConstants.getLocationGroupListDetails);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response getLocationGroupDetails(String location_group_id, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Get Location Group Details======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			// String body = apipaylods.createLocation(name,store_number);
			// request.body(body);
			// param = requestParams.toString();
			response = request.get(ApiConstants.getLocationGroupDetails + "?location_group_id=" + location_group_id);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response addLocationtoGroup(String authorization, String location_group_id, String store_number,
			String location_id) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Add Location to Location Group======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			String body = apipaylods.adddLocationtoGroup(location_group_id, store_number, location_id);
			request.body(body);
			// param = requestParams.toString();
			response = request.post(ApiConstants.addLocationtoGroup);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response deleteLocationfromGroup(String authorization, String location_group_id, String store_number,
			String location_id) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Delete Location From Location Group======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			String body = apipaylods.deleteLocationFromGroup(location_group_id, store_number, location_id);
			request.body(body);
			// param = requestParams.toString();
			response = request.delete(ApiConstants.deleteLocationFromGroup);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response deleteLocation(String location_id, String store_number, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Delete Location======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			String body = apipaylods.deleteLocation(location_id, store_number);
			request.body(body);
			response = request.delete(ApiConstants.deleteLocation);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response banUser(String user_id, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Ban a User======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			// String body = apipaylods.deleteLocation(location_id,store_number);
			// request.body(body);
			response = request.post(ApiConstants.banUser + "?reason=apitest" + "&user_id=" + user_id);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response unBanUser(String user_id, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API UnBan a User======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			// String body = apipaylods.deleteLocation(location_id,store_number);
			// request.body(body);
			response = request.delete(ApiConstants.unBanUser + "?reason=apitest" + "&user_id=" + user_id);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// Support Gifting to a User
	public Response supportGiftingToUser(String userID, String authToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Support Gifting to a User======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.supportGiftingToUser(userID);
			// Headers
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + authToken);
			cloudflareToken(request);
			request.body(body);
			response = request.post(ApiConstants.supportGiftingToUser);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response deactivateUser(String user_id, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Deactivate a User ======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			// String body = apipaylods.deleteLocation(location_id,store_number);
			// request.body(body);
			response = request.delete(ApiConstants.deactivateUser + "?user_id=" + user_id);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response reactivateUser(String user_id, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Reactivate a User ======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			// String body = apipaylods.deleteLocation(location_id,store_number);
			// request.body(body);
			response = request.post(ApiConstants.reactivateUser + "?user_id=" + user_id);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response updateUser(String user_id, String email, String location_id, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Update a User======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			String body = apipaylods.updateUser(user_id, email, location_id);
			request.body(body);
			response = request.patch(ApiConstants.updateUser);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response updateUserPhone(String user_id, String email, String phone, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			String body = new ObjectMapper().writeValueAsString(apipaylods.updateUserPhone(user_id, email, phone));
			request.body(body);
			response = request.patch(ApiConstants.updateUser);
			logger.info("API Response:" + response.asString());
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("error", e.getMessage());
		}
		return response;
	}

	// To send rewards to user
	public Response Api2SendMessageToUser(String userID, String authToken, String amount, String reedemable_id) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2SendMessageToUser(userID, amount, reedemable_id);
			// Headers
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + authToken);
			request.body(body);
			response = request.post(ApiConstants.dashboardAPIsendMessageToUser);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// Send points/punches to user
	public Response sendPointsToUser(String userId, String gift_count, String authToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.sendPointsToUser(userId, gift_count);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + authToken);
			request.body(body);
			response = request.post(ApiConstants.sendMessageToUser);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;

	}

	public Response userExport(String user_id, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Get User Export======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			String body = apipaylods.userExport(user_id);
			request.body(body);
			response = request.post(ApiConstants.userExport);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response extendedUserHistory(String user_id, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Get Extended User History======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			// String body = apipaylods.userExport(user_id);
			// request.body(body);
			response = request.get(ApiConstants.extendedUserHistory + "?user_id=" + user_id);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response fetchUserLocation(String user_id, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Fetch User Favourite Locations======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			// String body = apipaylods.userExport(user_id);
			// request.body(body);
			response = request.get(ApiConstants.fetchUserFavLocation + "?user_id=" + user_id);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response deleteUserLocation(String user_id, String favourite_location_id, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Delete User Favourite Location======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			// String body = apipaylods.userExport(user_id);
			// request.body(body);
			response = request.delete(ApiConstants.deleteUserFavLocation + "?user_favourite_location_id="
					+ favourite_location_id + "&user_id=" + user_id);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response deleteUser(String user_id, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Delete a User ======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			String body = apipaylods.deleteUser(user_id);
			request.body(body);
			response = request.delete(ApiConstants.deleteUser);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response anonymiseUser(String user_id, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Delete a User ======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			String body = new ObjectMapper().writeValueAsString(apipaylods.anonymiseUser(user_id));
			request.body(body);
			response = request.delete(ApiConstants.deleteUser);
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("error", e.getMessage());
		}
		return response;
	}

	public Response createBusinessMigrationUse(String userEmail, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Create Business Migration User======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			String body = apipaylods.createBusinessMigrationUser(userEmail);
			request.body(body);
			response = request.post(ApiConstants.createBusinessMigrationUser);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response updateBusinessMigrationUse(String userEmail, String authorization, String migration_user_id) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Update Business Migration User======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			String body = apipaylods.updateBusinessMigrationUser(userEmail);
			request.body(body);
			response = request.patch(ApiConstants.updateBusinessMigrationUser + migration_user_id);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response deleteBusinessMigrationUse(String authorization, String migration_user_id) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Delete Business Migration User======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			// String body = apipaylods.updateBusinessMigrationUser(userEmail);
			// request.body(body);
			response = request.delete(ApiConstants.deleteBusinessMigrationUser + migration_user_id);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response getAdminRolesList(String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Get Admin Roles List======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			// String body = apipaylods.userExport(user_id);
			// request.body(body);
			response = request.get(ApiConstants.getAdminRolesList);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response createBusinesAdmin(String userEmail, String role_id, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Create Business Admint======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			String body = apipaylods.createBusinessAdmin(userEmail, role_id);
			request.body(body);
			response = request.post(ApiConstants.createBusinessAdmin);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response updateBusinesAdmin(String business_admin_id, String userEmail, String role_id,
			String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Update Business Admin======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			String body = apipaylods.updateBusinessAdmin(business_admin_id, userEmail, role_id);
			request.body(body);
			response = request.patch(ApiConstants.updateBusinessAdmin);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response showBusinesAdmin(String business_admin_id, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Show Business Admin======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			String body = apipaylods.showBusinessAdmin(business_admin_id);
			request.body(body);
			response = request.get(ApiConstants.showBusinessAdmin);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response deleteBusinesAdmin(String business_admin_id, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Delete Business Admin======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			String body = apipaylods.showBusinessAdmin(business_admin_id);
			request.body(body);
			response = request.delete(ApiConstants.deleteBusinessAdmin);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response inviteBusinesAdmin(String userEmail, String role_id, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Invite Business Admint======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			String body = apipaylods.inviteBusinessAdmin(userEmail, role_id);
			request.body(body);
			response = request.post(ApiConstants.inviteBusinessAdmin);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response eClubGuestUpload(String userEmail, String storeNumber, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API EClub Guest Upload======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			String body = apipaylods.eClubGuestUpload(userEmail, storeNumber);
			request.body(body);
			response = request.post(ApiConstants.eClubGuestUpload);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response cReateFranchise(String userEmail, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Create Franchisee======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			String body = apipaylods.cReateFranchise(userEmail);
			request.body(body);
			response = request.post(ApiConstants.cReateFranchise);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response uPdateFranchise(String userEmail, String franchisee_id, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Update Franchisee======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			String body = apipaylods.uPdateFranchise(userEmail, franchisee_id);
			request.body(body);
			response = request.patch(ApiConstants.uPdateFranchise);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response dEleteFranchise(String franchisee_id, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Delete Franchisee======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			String body = apipaylods.dEleteFranchise(franchisee_id);
			request.body(body);
			response = request.delete(ApiConstants.dEleteFranchise);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response cReateSocialcauseCampaign(String campaignName, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Create Social Cause Campaigns======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			String body = apipaylods.cReateSocialcauseCampaign(campaignName);
			request.body(body);
			response = request.post(ApiConstants.createSocialCauesCampaign);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response activateSocialCampaign(String campaigId, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Activate Social Cause Campaign ======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			String body = apipaylods.activateSocialCampaign(campaigId);
			request.body(body);
			response = request.patch(ApiConstants.activateSocialCauesCampaign);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response deactivateSocialCampaign(String campaigId, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Deactivate Social Cause Campaign ======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			String body = apipaylods.activateSocialCampaign(campaigId);
			request.body(body);
			response = request.patch(ApiConstants.deactivateSocialCauesCampaign);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response enrollGuestForWifi(String email, String client, String location_id, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Enroll Guests For WiFi ======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			String body = apipaylods.enrollGuestForWifi(email, client, location_id);
			request.body(body);
			// param = requestParams.toString();
			response = request.post(ApiConstants.wifiSignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response searchRedemptionCode(String authorization, String redemptioncode, String location_id) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Search Redemption Code ======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			// String body = apipaylods.activateSocialCampaign(campaigId);
			// request.body(body);
			response = request.get(ApiConstants.searchRedemptionCode + "?redemption_code=" + redemptioncode
					+ "&location_id=" + location_id);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processRedemptionCode(String authorization, String redemptioncode, String location_id) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Process Redemption Code ======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			String body = apipaylods.processRedemption(redemptioncode, location_id);
			request.body(body);
			response = request.patch(ApiConstants.processRedemptionCode);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response forceRedeem(String authorization, String rewardId, String userId) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Process Redemption Code ======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			String body = apipaylods.forceRedeem(rewardId, userId);
			request.body(body);
			response = request.post(ApiConstants.forceRedeem);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2CreateDonation(String client, String secret, String access_token, String social_cause_id) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Create Donation======");
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2CreateDonation(client, social_cause_id);

			String payload = ApiConstants.mobApi2CreateDonation + body;
			// Headers
			request.header("Authorization", "Bearer " + access_token);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept", api2Headers.api2Accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.mobApi2CreateDonation);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2SocialCausecampaigndetails(String client, String secret, String access_token,
			String social_cause_id) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Social Cause Campaign Details======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// String body = apipaylods.API2CreateDonation(client,social_cause_id);

			String payload = ApiConstants.mobApi2SocialCauseCampaignDetails + social_cause_id + "?client=" + client;
			// Headers
			request.header("Authorization", "Bearer " + access_token);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept", api2Headers.api2Accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			// request.body(body);
			response = request
					.get(ApiConstants.mobApi2SocialCauseCampaignDetails + social_cause_id + "?client=" + client);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// To send rewards to user
	public Response Api2SendMessageToUser(String userID, String authToken, String amount, String reedemable_id,
			String fuelAmount, String gift_count) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2SendMessageToUser(userID, amount, reedemable_id, fuelAmount, gift_count);
			// Headers
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + authToken);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.dashboardAPIsendMessageToUser);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// V5 Redemption using reward_id
	public Response v5RedemptionWithRewardId(String userEmail, String locationKey, String reward_id) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.v5RedemptionWithRewardId(reward_id);
			request.queryParam("email", userEmail);
			request.header("Authorization", "Token token=" + locationKey);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.v5RedemptionsUsingRewardId);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2SubscriptionPurchaseRenew(String token, String subscriptionID, String client, String secret,
			String purchase_price, String subscriptionAdaptorType) {
		Response response = null;
		// requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.api2PurchaseSubscriptionPayloadRenew(subscriptionID, purchase_price);
			// String payload = ApiConstants.mobApi2RenewalSubscription + "?client=" +
			// client +
			// body;
			// Headers
			request.header("Authorization", "Bearer " + token);
			request.header("x-pch-digest", api2Headers.api2Signature(body, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.dashboardAPiRenewalSubscription);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// shashank
	public Response APIOmmLookupFind(String token, String subscriptionID, String client, String secret,
			String purchase_price) {
		Response response = null;
		// requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.api2PurchaseSubscriptionPayloadRenew(subscriptionID, purchase_price);
			// String payload = ApiConstants.mobApi2RenewalSubscription + "?client=" +
			// client +
			// body;
			// Headers
			request.header("Authorization", "Bearer " + token);
			request.header("x-pch-digest", api2Headers.api2Signature(body, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.dashboardAPiRenewalSubscription);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authListAvailableRewardsNew(String authToken, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given();
			request.queryParam("client", client);
			request.header("Authorization", "Bearer " + authToken);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			requestParams.put("client", client);
			param = requestParams.toString();
			String URL = ApiConstants.authListAvailableRewards + "?client=" + client;
			String payload = URL + param;
			request.body(requestParams.toJSONString());
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			request.headers(defaultHeaderMap);
			response = request.get(ApiConstants.authListAvailableRewards);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authListDiscountBasketAdded(String authToken, String client, String secret, String discountType,
			String discountID) {
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			String epoch = apiUtils.getSpan();
			String hash = apiUtils.getHash(secret + epoch);
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.discountBasketItemsAttributes(discountType, discountID);
			String URL = ApiConstants.discountBasketAdded + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;
			request.header("x-pch-digest", apiUtils.getSignature(secret, payload));
			request.header("Authorization", "Bearer " + authToken);
			request.header("Content-Type", "application/json");
			request.header("x-pch-hash", hash);
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			// request.queryParam("location_id", location_id);
			request.body(body);
			response = request.post(ApiConstants.discountBasketAdded);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response addDiscountAmountToDiscountBasket(String authToken, String client, String secret,
			String discountType, String discountID) {
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			String epoch = apiUtils.getSpan();
			String hash = apiUtils.getHash(secret + epoch);
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.discountAmountItemPayload(discountType, discountID);
			String URL = ApiConstants.discountBasketAdded + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;
			request.header("x-pch-digest", apiUtils.getSignature(secret, payload));
			request.header("Authorization", "Bearer " + authToken);
			request.header("Content-Type", "application/json");
			request.header("x-pch-hash", hash);
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			// request.queryParam("location_id", location_id);
			request.body(body);
			response = request.post(ApiConstants.discountBasketAdded);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// add discount using POS API : shashank
	public Response authListDiscountBasketAddedForPOSAPI(String locationKey, String userID, String discountType,
			String discountID) {
		Response response = null;
		String body = "";
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			if (discountType.equalsIgnoreCase("discount_amount")) {
				body = apipaylods.discountBasketItemsAttributesDiscountAmpountPOS(userID, discountType, discountID);
			} else {
				body = apipaylods.discountBasketItemsAttributes(userID, discountType, discountID);
			}
			request.header("Authorization", "Token token=" + locationKey);
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.header("Accept-Language", "es");
			request.body(body);
			response = request.post(ApiConstants.discountBasketAddedPOS);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// for AUTH API: shashank
	public Response authListDiscountBasketAddedAUTH(String authToken, String client, String secret, String discountType,
			String discountID) {
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.discountBasketItemsAttributes(discountType, discountID);
			String URL = ApiConstants.discountBasketAddedAUTH + "?client=" + client;
			String payload = URL + body;
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			request.header("Authorization", "Bearer " + authToken);
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.discountBasketAddedAUTH);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response addDiscountToBasketAPI2(String access_token, String client, String secret, String discountType,
			String discountID) {
		Response response = null;
		requestParams = new JSONObject();

		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = "";
			if (discountType.equalsIgnoreCase("discount_amount")) {
				body = apipaylods.discountAmountItemPayload(discountType, discountID);
			} else {

				body = apipaylods.discountBasketItemsAttributes(discountType, discountID);
			}
			String URL = ApiConstants.mobApi2DiscountBasketAddeddAPI + "?client=" + client + "&access_token="
					+ access_token;
			String payload = URL + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));

			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.queryParam("access_token", access_token);

			request.body(body);
			response = request.post(ApiConstants.mobApi2DiscountBasketAddeddAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;

	}

	// API-Nwew
	public Response getDiscountBasketDetailsOfUsersAPIMobile(String authToken, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// requestParams.put("redeemed_points", redeemed_points);
			param = requestParams.toString();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.getDiscountBasketAPI + "?client=" + client + "&hash=" + epoch;
			String payload = URL + param;

			request.headers(header.api1SignUpHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + authToken);
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.get(ApiConstants.getDiscountBasketAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiFetchRedemptionCode(String auth_token, String client, String secret, String location_id,
			String redeemed_points, String reward_id) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("====== Auth Api Fetch Redemption Code ======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.authApifetchRedemptionCode(client, location_id, redeemed_points, reward_id);

			String URL = ApiConstants.authApiFetchRedemptionCode + "?authentication_token=" + auth_token;
			String payload = URL + body;
			request.queryParam("authentication_token", auth_token);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.header("Accept", "application/json");
			request.header("Content-Type", "application/json");
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			// authHeaders.authApiSignature(payload, secret));

			request.body(body);
			response = request.post(ApiConstants.authApiFetchRedemptionCode);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiFetchAvailableOffersOfTheUser(String auth_token, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("====== Auth Api Fetch available offers of the user ======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.authApifetchAvailableOffersOfTheUser(client, auth_token);

			// String URL = ApiConstants.authApifetchAvailableOffersOfTheUser;
			String payload = ApiConstants.authApifetchAvailableOffersOfTheUser + body;

			request.header("Accept", "application/json");
			request.header("Content-Type", "application/json");
			request.header("x-pch-digest", authHeaders.authApiSignature(payload, secret));
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.get(ApiConstants.authApifetchAvailableOffersOfTheUser);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiCreateLoyaltyCheckin(String auth_token, String client, String secret, String store_num,
			String barcode) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("====== Auth Api Create Loyalty Checkin of the user ======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.authApiCreateLoyaltyCheckin(client, auth_token, store_num, barcode);

			// String URL = ApiConstants.authApifetchAvailableOffersOfTheUser;
			String payload = ApiConstants.authApiCreateLoyaltyCheckin + body;

			request.header("Accept", "application/json");
			request.header("Accept-Language", "application/json");
			request.header("Content-Type", "application/json");
			request.header("x-pch-digest", authHeaders.authApiSignature(payload, secret));

			request.body(body);
			response = request.post(ApiConstants.authApiCreateLoyaltyCheckin);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiFetchACheckinByExternal_uid(String auth_token, String client, String secret,
			String external_uid) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("====== Auth Api Fetch a Checkin by external_uid of the user ======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.authApiFetchACheckinByExternal_uid(auth_token, client, external_uid);

			// String URL = ApiConstants.authApiFetchACheckinByExternal_uid +
			// "?authentication_token=" + auth_token
			// + "&client=" + client;
			String payload = ApiConstants.authApiFetchACheckinByExternal_uid + body;
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.headers(header.onlineOrderCheckinHeader(payload.toString(), secret));
			request.body(body);
			response = request.get(ApiConstants.authApiFetchACheckinByExternal_uid);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiCreateAccessToken(String client, String secret, String security_token) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("====== Auth Api Create Access Token of the user ======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.authApiCreateAccessToken(client, security_token);

			String URL = ApiConstants.authApiCreateAccessToken;
			String payload = URL + body;

			request.header("Accept", "application/json");
			request.header("Content-Type", "application/json");
			request.header("x-pch-digest", authHeaders.authApiSignature(payload, secret));
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.authApiCreateAccessToken);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiChangePassword(String auth_token, String client, String secret, String password) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("====== Auth Api Change Password of the user ======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.authApiChangePassword(client, auth_token, password);

			String URL = ApiConstants.authApiChangePassword;
			String payload = URL + body;

			request.header("Accept", "application/json");
			request.header("Content-Type", "application/json");
			request.header("x-pch-digest", authHeaders.authApiSignature(payload, secret));
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.patch(ApiConstants.authApiChangePassword);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiResetPasswordTokenOfTheUser(String client, String secret, String email) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("====== Auth Api Get reset_password_token of the user ======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// String body = apipaylods.authApiresetPasswordTokenOfTheUser(client, email);

			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("email", email);
			requestParams.put("user", hashmap);
			requestParams.put("client", client);
			param = requestParams.toString();

			String URL = ApiConstants.authApiresetPasswordTokenOfTheUser;
			String payload = URL + param;

			request.header("Content-Type", "application/json");
			request.header("x-pch-digest", authHeaders.authApiSignature(payload, secret));
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.authApiresetPasswordTokenOfTheUser);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// POS Get discount details
	public Response getUserDiscountBasketDetailsUsingPOS(String userID, String locationKey) {

		Response response = null;
		String body = "{\"user_id\":" + userID + "}";
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Authorization", "Token token=" + locationKey);
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.body(body);
			response = request.get(ApiConstants.getDiscountBasketPOS);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;

	}

	// for AUTH API: shashank
	public Response getUserDiscountBasketDetailsUsingAUTH(String authToken, String client, String secret) {
		Response response = null;
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.getDiscountBasketAUTH + param;
			request.body(requestParams.toJSONString());
			// Headers passing
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			defaultHeaderMap.put("Accept", prop.getProperty("contentType"));
			defaultHeaderMap.put("Accept-Language", "en");
			request.header("Authorization", "Bearer " + authToken);
			request.headers(defaultHeaderMap);
			response = request.get(ApiConstants.getDiscountBasketAUTH);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiEstimateLoyaltyPointsEarning(String auth_token, String client, String secret,
			String subtotal_amount) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("====== Auth Api Estimate Loyalty Points Earning ======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.authApiEstimateLoyaltyPointsEarning(auth_token, client, subtotal_amount);

			String URL = ApiConstants.authApiEstimateLoyaltyPointsEarning;
			String payload = URL + body;

			// request.queryParam("authentication_token", auth_token);
			// request.queryParam("client", client);
			// request.queryParam("subtotal_amount", subtotal_amount);

			request.header("Accept", "application/json");
			request.header("Content-Type", "application/json");
			request.header("x-pch-digest", authHeaders.authApiSignature(payload, secret));
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.get(ApiConstants.authApiEstimateLoyaltyPointsEarning);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiPointConversionAPI(String auth_token, String client, String secret,
			String conversion_rule_id, String converted_value, String source_value, String social_cause_campaign_id) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("====== Auth Api Point Conversion API ======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.authApiPointConversionAPI(auth_token, client, conversion_rule_id, converted_value,
					source_value, social_cause_campaign_id);

			String URL = ApiConstants.authApiPointConversionAPI;
			String payload = URL + body;

			// request.queryParam("authentication_token", auth_token);
			// request.queryParam("client", client);
			// request.queryParam("conversion_rule_id", conversion_rule_id);
			// request.queryParam("converted_value", converted_value);
			// request.queryParam("source_value", source_value);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.header("cache-control", "no cache");
			request.header("Content-Type", "application/json");
			request.header("x-pch-digest", authHeaders.authApiSignature(payload, secret));
			request.body(body);
			response = request.post(ApiConstants.authApiPointConversionAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiGetTheDealDetail(String auth_token, String client, String secret, String redeemable_uuid) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("====== Auth Api Get the deal detail ======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.authApiGetTheDealDetail(auth_token, client, redeemable_uuid);
			String URL = ApiConstants.authDeals + "/" + redeemable_uuid;
			String payload = URL + body;
			request.header("Accept", "application/json");
			request.header("Accept-Language", "en");
			request.header("Content-Type", "application/json");
			request.header("x-pch-digest", authHeaders.authApiSignature(payload, secret));
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.get(ApiConstants.authDeals + "/" + redeemable_uuid);
			TestListeners.extentTest.get().info(response.statusCode() + "; " + response.asString());
			logger.info(response.statusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authFetchActivePurchasableSubscriptionPlan(String client, String secret) {
		Response response = null;
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2ClientPayload(client);
			String payload = ApiConstants.authSubscriptionPurchase + body;
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			request.header("x-pch-digest", signature);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.get(ApiConstants.authSubscriptionPurchase);
			TestListeners.extentTest.get().info(response.getStatusCode() + "; " + response.asString());
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiEstimatePointsEarning(String auth_token, String client, String secret, String receipt_amount,
			String subtotal_amount, String item_amount) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("====== Auth Api Estimate Points Earning ======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.authApiEstimatePointsEarning(auth_token, client, item_amount, receipt_amount,
					subtotal_amount);

			// HashMap<String, Object> hashmap = new HashMap<String, Object>();
			// requestParams.put("authentication_token", auth_token);
			// requestParams.put("client", client);
			// hashmap.put("item_name","White rice");
			// hashmap.put("item_qty", "1");
			// hashmap.put("item_amount", item_amount);
			// hashmap.put("menu_item_type", "M");
			// hashmap.put("menu_item_id", "3419");
			// hashmap.put("menu_family", "800");
			// hashmap.put("menu_major_group", "152");
			// hashmap.put("serial_number", "1");
			// hashmap.put("subtotal_amount", subtotal_amount);
			// hashmap.put("receipt_amount", receipt_amount);
			// requestParams.put("menu_items", hashmap);
			// param = requestParams.toString();

			String URL = ApiConstants.authApiEstimatePointsEarning;
			// String payload = URL + param;
			String payload = URL + body;
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.header("Accept", "application/json");
			request.header("Content-Type", "application/json");
			request.header("x-pch-digest", authHeaders.authApiSignature(payload, secret));
			request.body(body);
			// request.body(requestParams.toJSONString());
			response = request.get(ApiConstants.authApiEstimatePointsEarning);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiBalanceTimelines(String auth_token, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("====== Auth Api Balance Timelines ======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.authApiBalanceTimelines(auth_token, client);

			String URL = ApiConstants.authApiBalanceTimelines;
			String payload = URL + body;

			request.header("x-pch-digest", authHeaders.authApiSignature(payload, secret));
			request.header("Content-Type", "application/json");
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.get(ApiConstants.authApiBalanceTimelines);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiUserEnrollment(String auth_token, String client, String secret, String item_id) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("====== Auth Api User Enrollment ======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.authApiUserEnrollment(item_id, auth_token, client);
			String URL = ApiConstants.authApiUserEnrollment;
			String payload = URL + body;
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.header("cache-control", "no cache");
			request.header("Content-Type", "application/json");
			request.header("x-pch-digest", authHeaders.authApiSignature(payload, secret));
			request.body(body);
			response = request.post(ApiConstants.authApiUserEnrollment);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiUserDisenrollment(String auth_token, String client, String secret, String item_id) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("====== Auth Api User Disenrollment ======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.authApiUserEnrollment(item_id, auth_token, client);

			String URL = ApiConstants.authApiUserDisenrollment;
			String payload = URL + body;
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.header("cache-control", "no cache");
			request.header("Content-Type", "application/json");
			request.header("x-pch-digest", authHeaders.authApiSignature(payload, secret));
			request.body(body);
			response = request.delete(ApiConstants.authApiUserDisenrollment);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiGetSSOToken(String code, String client, String secret, String grant_type,
			String redirect_uri) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("====== Auth Api Get SSO token ======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = new ObjectMapper()
					.writeValueAsString(apipaylods.authApiGetSSOToken(code, client, secret, redirect_uri, grant_type));
			request.header("Accept", "application/json");
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.authApiGetSSOToken);
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("error", e.getMessage());
		}
		return response;
	}

	// V5 Redemption using reward_id
	public Response v5RedemptionWithRedemptionId(String userEmail, String locationKey, String redeemable_id) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.v5RedemptionWithRedemptionId(redeemable_id);
			request.queryParam("email", userEmail);
			request.header("Authorization", "Token token=" + locationKey);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.v5RedemptionsUsingRewardId);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response addDiscountAmountToDiscountBasketAUTH(String authToken, String client, String secret,
			String discountType, String discountID) {
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.discountAmountItemPayload(discountType, discountID);
			String URL = ApiConstants.discountBasketAddedAUTH + "?client=" + client;
			String payload = URL + body;
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			request.header("Authorization", "Bearer " + authToken);
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.discountBasketAddedAUTH);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response getUserDiscountBasketDetailsUsingAPI2(String authToken, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();

		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String URL = ApiConstants.mobApi2GetDiscountBasketDetailsAPI + "?client=" + client + "&access_token="
					+ authToken;
			String payload = URL;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.queryParam("access_token", authToken);

			response = request.get(ApiConstants.mobApi2GetDiscountBasketDetailsAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;

	}

	// API-Nwew - Delete
	public Response deleteDiscountBasketForUserAPI(String authToken, String client, String secret,
			String discount_basket_item_id) {
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			String epoch = apiUtils.getSpan();
			String hash = apiUtils.getHash(secret + epoch);
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.deleteDiscountBasketPayload(discount_basket_item_id);
			String URL = ApiConstants.deleteBasketForUserAPI + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;
			request.header("x-pch-digest", apiUtils.getSignature(secret, payload));
			request.header("Authorization", "Bearer " + authToken);
			request.header("Content-Type", "application/json");
			request.header("x-pch-hash", hash);
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.delete(ApiConstants.deleteBasketForUserAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// AUTH - Delete
	public Response deleteDiscountBasketForUserAUTH(String authToken, String client, String secret,
			String discount_basket_item_id) {
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.deleteDiscountBasketPayload(discount_basket_item_id);
			String URL = ApiConstants.deleteBasketForUserAUTH + "?client=" + client;
			String payload = URL + body;
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			request.header("Authorization", "Bearer " + authToken);
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.body(body);
			response = request.delete(ApiConstants.deleteBasketForUserAUTH);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response deleteDiscountBasketForUserAPI2(String access_token, String client, String secret,
			String discount_basket_item_id) {
		Response response = null;
		requestParams = new JSONObject();

		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.deleteDiscountBasketPayload(discount_basket_item_id);

			String URL = ApiConstants.mobApi2DeleteBasketForUserAPI + "?client=" + client + "&access_token="
					+ access_token;
			String payload = URL + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.queryParam("access_token", access_token);

			request.body(body);
			response = request.delete(ApiConstants.mobApi2DeleteBasketForUserAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;

	}

	public Response deleteDiscountBasketForUserPOS(String locationKey, String userID, String discount_basket_item_id) {
		Response response = null;

		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.deleteDiscountBasketPayload(userID, discount_basket_item_id);
			request.header("Authorization", "Token token=" + locationKey);
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.header("Accept-Language", "es");
			request.body(body);
			response = request.delete(ApiConstants.deleteBasketForUserPOS);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionOfBasketPOS(String locationKey, String userID, String item_id) {
		logger.info("Endpoints.processBatchRedemptionOfBasketPOS()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getBatchRedemptionPayloadOMMM(locationKey, userID, item_id);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// To send rewards to user
	public Response sendMessageToUser(String userID, String authToken, String amount, String reedemable_id,
			String fuelAmount, String gift_count) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Send message to user======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2SendMessageToUser(userID, amount, reedemable_id, fuelAmount, gift_count);
			// Headers
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + authToken);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.sendMessageToUser);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// User LookUp Api
	public Response userLookUpApi(String queryParam, String userEmail, String admin_key, long phone, String userId) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Authorization", "Bearer " + admin_key);
			switch (queryParam) {
			case "phone":
				request.queryParam("phone", phone);
				response = request.get(ApiConstants.dashboardAPiUsersInfo + "?phone=" + phone);
				logger.info("===== User Look Up (phone only) ======");
				break;
			case "emailOnly":
				request.queryParam("email", userEmail);
				response = request.get(ApiConstants.dashboardAPiUsersInfo + "?email=" + userEmail);
				logger.info("===== User Look Up (email only) =====");
				break;
			case "userId_email_phone":
				request.queryParam("email", userEmail);
				request.queryParam("phone", phone);
				request.queryParam("user_id", userId);
				response = request.get(ApiConstants.dashboardAPiUsersInfo + "?phone=" + phone + "&email=" + userEmail
						+ "&user_id=" + userId);
				break;
			case "userId_phone":
				request.queryParam("phone", phone);
				request.queryParam("user_id", userId);
				response = request.get(ApiConstants.dashboardAPiUsersInfo + "?phone=" + phone + "&user_id=" + userId);
				logger.info("===== User Look Up (phone and userId both) =====");
				break;
			case "email_phone":
				request.queryParam("email", userEmail);
				request.queryParam("phone", phone);
				response = request.get(ApiConstants.dashboardAPiUsersInfo + "?phone=" + phone + "&email=" + userEmail);
				logger.info("===== User Look Up (phone and email both) =====");
				break;
			}
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response DeactivateUserAPI(String client, String secret, String authToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("=======Guest Deactivate V1 API=======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			requestParams.put("client", client);
			String URL = ApiConstants.deactivateUserAPI + "?" + "hash=" + epoch;
			String payload = URL + requestParams.toString();
			request.headers(header.deactivateUserHeader(payload, epoch, secret, authToken));
			// request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(requestParams.toString());
			response = request.delete(ApiConstants.deactivateUserAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response DeleteUserAPI(String client, String secret, String authToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("=======Guest Delete V1 API=======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// param = requestParams.toString();
			String epoch = apiUtils.getSpan();
			requestParams.put("client", client);
			// String body ="{\n"
			// + "
			// \"client\":\"98c9f6b6c352858f35b1cd9b0a889635a01f4192096104ff1d0c921ac3a5956b\"\n"
			// + "}";
			String URL = ApiConstants.deleteUserAPI + "?" + "hash=" + epoch;
			String payload = URL + requestParams.toString();
			request.headers(header.deleteUserHeader(payload, epoch, secret, authToken));
			// request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(requestParams.toString());
			response = request.delete(ApiConstants.deleteUserAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response CheckinV5API(String email, String transaction_no, String receipt_datetime, String locationkey) {
		Response response = null;
		requestParams = new JSONObject();

		CreateDateTime.getCurrentDate();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// Headers
			request.header("Authorization", "Token token=" + locationkey);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.formParam("amount", "3");
			request.formParam("receipt_datetime", receipt_datetime);
			request.formParam("menu_items[]", "Iced Tea|1|2|M|110002|1|38");
			request.formParam("email", email);
			request.formParam("subtotal_amount", "4");
			request.formParam("transaction_no", transaction_no);
			response = request.post(ApiConstants.V5checkins);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response onlineOrderCheckinModified(String order_mode, String client_platform, String authenticationToken,
			String amount, String client, String secret) {
		Response response = null;
		try {
			transactionNumber = "123456" + CreateDateTime.getTimeDateString();
			externalUid = "TestUid" + CreateDateTime.getTimeDateString();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = ApiPayloads.onlineCheckinModifiedPayLoad(order_mode, client_platform, authenticationToken,
					amount, client, transactionNumber, externalUid);
			String payload = ApiConstants.onlineOrderCheckin + body;
			request.headers(header.onlineOrderCheckinHeader(payload.toString(), secret));
			request.body(body);
			response = request.post(ApiConstants.onlineOrderCheckin);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1UserSignUpWrongZipcode(String email, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======API1 User SignUp======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("email", email);
			hashmap.put("password", Utilities.getApiConfigProperty("password"));
			hashmap.put("first_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("firstName")));
			hashmap.put("last_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("lastName")));
			hashmap.put("zip_code", Utilities.getApiConfigProperty("zipCode1"));
			hashmap.put("signup_channel", Utilities.getApiConfigProperty("signUpChannel"));
			hashmap.put("referral_code", Utilities.getApiConfigProperty("referralCode"));
			requestParams.put("user", hashmap);
			param = requestParams.toString();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1SignUp + "?client=" + client + "&hash=" + epoch;
			String payload = URL + param;
			request.headers(header.api1SignUpHeader(payload, epoch, secret));
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.post(ApiConstants.mobApi1SignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			// logger.info("Response code: "+response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posSignUpNegativeZipcode(String email, String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("*****POS Signup API*****");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("email", email);
			requestParams.put("password", Utilities.getApiConfigProperty("password"));
			requestParams.put("first_name",
					CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("firstName")));
			requestParams.put("last_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("lastName")));
			requestParams.put("zip_code", Utilities.getApiConfigProperty("zipCode1"));
			request.queryParam("location_key", locationKey);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.posSignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiSignUpPositive(String email, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("email", email);
			hashmap.put("password", Utilities.getApiConfigProperty("password"));
			hashmap.put("first_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("firstName")));
			hashmap.put("last_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("lastName")));
			// hashmap.put("password", Utilities.getApiConfigProperty("password"));
			hashmap.put("birthday", Utilities.getApiConfigProperty("birthday"));
			hashmap.put("anniversary", Utilities.getApiConfigProperty("anniversary"));
			hashmap.put("phone", Utilities.phonenumber());
			hashmap.put("signup_channel", Utilities.getApiConfigProperty("ssoSignupChannel"));
			hashmap.put("zip_code", Utilities.getApiConfigProperty("zipCode"));
			requestParams.put("user", hashmap);
			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.authApiSignUp + param;
			// Headers passing
			request.headers(header.authAPISignUpHeader(payload, secret));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.authApiSignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiSignUpNegative(String email, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("email", email);
			hashmap.put("password", Utilities.getApiConfigProperty("password"));
			hashmap.put("first_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("firstName")));
			hashmap.put("last_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("lastName")));
			// hashmap.put("password", Utilities.getApiConfigProperty("password"));
			hashmap.put("birthday", Utilities.getApiConfigProperty("birthday"));
			hashmap.put("anniversary", Utilities.getApiConfigProperty("anniversary"));
			// hashmap.put("phone", Utilities.getRandomNo(1000) +
			// CreateDateTime.getTimeDateString());
			hashmap.put("signup_channel", Utilities.getApiConfigProperty("ssoSignupChannel"));
			hashmap.put("zip_code", Utilities.getApiConfigProperty("zipCode1"));
			requestParams.put("user", hashmap);
			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.authApiSignUp + param;
			// Headers passing
			request.headers(header.authAPISignUpHeader(payload, secret));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.authApiSignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2MarkMessagesRead(String client, String secret, String access_token) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Mark Messages Read ======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2MarkMessagesRead(client);
			// param = requestParams.toString();

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2MarkMessagesRead + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.mobApi2MarkMessagesRead);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2DeleteMessages(String client, String message_id, String secret, String access_token) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Delete Messages ======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// request.queryParam("client", client);
			// request.pathParam("message_id", message_id);
			String body = apipaylods.API2OnlyClientBodyPayload(client);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2DeleteMessages + message_id + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.delete(ApiConstants.mobApi2DeleteMessages + message_id);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2GenerateOtpToken(String client, String secret, String access_token) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Generate OTP Token ======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2OnlyClientBodyPayload(client);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2GenerateOtpToken + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.mobApi2GenerateOtpToken);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posProgramMeta(String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Authorization", "Token token=" + locationKey);
			request.header("Content-Type", api2Headers.api2ContentType());
			response = request.get(ApiConstants.posProgramMeta);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posApplicableOffer(String userEmail, String amount, String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.posApplicableOffers(userEmail, amount);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Authorization", "Token token=" + locationKey);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.posApplicableOffers);
			utils.logit(response);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionOfBasketPOSNew(String locationKey, String userID) {
		logger.info("Endpoints.processBatchRedemptionOfBasketPOS()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getBatchRedemptionPayloadOMMMNew(locationKey, userID);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionOfBasketPOSNew(String locationKey, String userID, String subAmount) {
		logger.info("Endpoints.processBatchRedemptionOfBasketPOS()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getBatchRedemptionPayloadOMMMNew(locationKey, userID, subAmount);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response deleteDiscountToBasketAPI2(String access_token, String client, String secret, String basketID) {
		Response response = null;
		requestParams = new JSONObject();

		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.deleteDiscountBasketPayload(basketID);
			String URL = ApiConstants.mobApi2DeleteBasketForUserAPI + "?client=" + client + "&access_token="
					+ access_token;
			String payload = URL + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.queryParam("access_token", access_token);
			request.body(body);
			response = request.delete(ApiConstants.mobApi2DeleteBasketForUserAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;

	}

	public Response processBatchRedemptionOfBasketPOSDollar1_Off(String locationKey, String userID, String subAmount) {
		logger.info("Endpoints.processBatchRedemptionOfBasketPOS()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getBatchRedemptionPayloadOMMMForDollar1_Off(locationKey, userID, subAmount);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionOfBasketPOSAPI(String locationKey, String userID, String subAmount,
			Map<String, Map<String, String>> parentMap) {
		logger.info("Endpoints.processBatchRedemptionOfBasketPOS()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.generateReceipt(locationKey, userID, subAmount, parentMap);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionPOSAPI);
			utils.logit(response);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionOfBasket107(String locationKey, String userID, String Amount,
			String recpAmount1, String recpAmount2) {
		logger.info("Endpoints.processBatchRedemptionOfBasket()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getBatchRedemptionPayloadOMMMFor107(locationKey, userID, Amount, recpAmount1,
					recpAmount2);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionOfBasketPOSAPI107(String locationKey, String userID, String subAmount,
			String recpAmount1, String recpAmount2, String recpAmount3, String item_id1, String item_id2,
			String item_id3) {
		logger.info("Endpoints.processBatchRedemptionOfBasketPOS()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getBatchRedemptionPayloadOMMM107(locationKey, userID, subAmount, recpAmount1,
					recpAmount2, recpAmount3, item_id1, item_id2, item_id3);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionOfBasket111(String locationKey, String userID, String Amount, String qty,
			String qty1) {
		logger.info("Endpoints.processBatchRedemptionOfBasket()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getBatchRedemptionPayloadOMMMFor111(locationKey, userID, Amount, qty, qty1);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionOfBasketPOSAPI110(String locationKey, String userID, String subAmount,
			String recpAmount1, String recpAmount2, String recpAmount3) {
		logger.info("Endpoints.processBatchRedemptionOfBasketPOS()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getBatchRedemptionPayloadOMMM110(locationKey, userID, subAmount, recpAmount1,
					recpAmount2, recpAmount3);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionOfBasketPOSAPI109(String locationKey, String userID, String subAmount,
			String recpAmount1, String recpAmount2, String recpAmount3) {
		logger.info("Endpoints.processBatchRedemptionOfBasketPOS()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getBatchRedemptionPayloadOMMM109(locationKey, userID, subAmount, recpAmount1,
					recpAmount2, recpAmount3);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionOfBasketPOSAPI108(String locationKey, String userID, String subAmount,
			String recpAmount1, String recpAmount2, String recpAmount3, String recpAmount4) {
		logger.info("Endpoints.processBatchRedemptionOfBasketPOS()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getBatchRedemptionPayloadOMMM108(locationKey, userID, subAmount, recpAmount1,
					recpAmount2, recpAmount3, recpAmount4);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionOfBasket108(String locationKey, String userID, String Amount,
			String recpAmount1, String recpAmount2) {
		logger.info("Endpoints.processBatchRedemptionOfBasket()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getBatchRedemptionPayloadOMMMFor108(locationKey, userID, Amount, recpAmount1,
					recpAmount2);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionOfBasketPOSAPI108_1(String locationKey, String userID, String subAmount,
			String recpAmount1, String recpAmount2, String recpAmount3) {
		logger.info("Endpoints.processBatchRedemptionOfBasketPOS()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getBatchRedemptionPayloadOMMM108_1(locationKey, userID, subAmount, recpAmount1,
					recpAmount2, recpAmount3);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Map<String, String> getRecieptDetailsMap(String item_name, String item_qty, String amount, String item_type,
			String item_family, String item_group, String serial_number, String item_id) {

		Map<String, String> detailsMap = new HashMap<String, String>();
		detailsMap.put("item_name", item_name);
		detailsMap.put("item_qty", item_qty);
		detailsMap.put("amount", amount);
		detailsMap.put("item_type", item_type);
		detailsMap.put("item_family", item_family);
		detailsMap.put("item_group", item_group);
		detailsMap.put("serial_number", serial_number);
		detailsMap.put("item_id", item_id);

		return detailsMap;
	}

	public Response processBatchRedemptionOfBasket144(String locationKey, String userID, String Amount,
			String recpAmount1, String recpAmount2, String qty, String qty1) {
		logger.info("Endpoints.processBatchRedemptionOfBasket()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getBatchRedemptionPayloadOMMM144(locationKey, userID, Amount, recpAmount1,
					recpAmount2, qty, qty1);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionOfBasketPOSAPI144_5(String locationKey, String userID, String subAmount,
			String recpAmount1, String recpAmount2, String recpAmount3, String recpAmount4, String recpAmount5) {
		logger.info("Endpoints.processBatchRedemptionOfBasketPOS()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getBatchRedemptionPayloadOMMM144_5(locationKey, userID, subAmount, recpAmount1,
					recpAmount2, recpAmount3, recpAmount4, recpAmount5);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionOfBasket144_9(String locationKey, String userID, String Amount,
			String recpAmount1, String recpAmount2) {
		logger.info("Endpoints.processBatchRedemptionOfBasket()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getBatchRedemptionPayloadOMMMFor144_9(locationKey, userID, Amount, recpAmount1,
					recpAmount2);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionOfBasket145_1(String locationKey, String userID, String Amount,
			String recpAmount1, String recpAmount2) {
		logger.info("Endpoints.processBatchRedemptionOfBasket()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getBatchRedemptionPayloadOMMMFor145_1(locationKey, userID, Amount, recpAmount1,
					recpAmount2);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionPayloadOMMM150(String locationKey, String userID, String Amount, String qty,
			String id) {
		logger.info("Endpoints.processBatchRedemptionOfBasketPOS()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getBatchRedemptionPayloadOMMM150(locationKey, userID, Amount, qty, id);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionOfBasketPOSAPI(String locationKey, String userID, String item_id,
			String subAmount, String recpAmount1, String recpAmount2, String recpAmount3) {
		logger.info("Endpoints.processBatchRedemptionOfBasketPOS()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getBatchRedemptionPayloadOMMM(locationKey, userID, item_id, subAmount, recpAmount1,
					recpAmount2, recpAmount3);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processDiscountLookup(String locationKey, String userID, String subAmount, String recpAmount1,
			String recpAmount2, String recpAmount3) {
		logger.info("Endpoints.processBatchRedemptionOfBasketPOS()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getDiscountLookupPayloadOMMM(locationKey, userID, subAmount, recpAmount1,
					recpAmount2, recpAmount3);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchPOSDiscountAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionOfBasketPOSDiscountLookup(String locationKey, String userID, String item_id,
			String subAmount, String recpAmount1, String recpAmount2, String recpAmount3) {
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getBatchRedemptionPayloadOMMM(locationKey, userID, item_id, subAmount, recpAmount1,
					recpAmount2, recpAmount3);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchPOSDiscountAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionOfBasketPOSDiscountLookup(String locationKey, String userID, String subAmount,
			Map<String, Map<String, String>> parentMap) {
		logger.info("Endpoints.processBatchRedemptionOfBasketPOS()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.generateReceipt(locationKey, userID, subAmount, parentMap);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchPOSDiscountAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionCouponCodeDiscountLookup(String locationKey, String userID, String item_id) {
		logger.info("Endpoints.processBatchRedemptionOfBasketPOS()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getBatchRedemptionPayloadOMMMNew(locationKey, userID);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchPOSDiscountAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionOfBasketPOSNewDiscountLookup(String locationKey, String userID,
			String subAmount) {
		logger.info("Endpoints.processBatchRedemptionOfBasketPOS()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getBatchRedemptionPayloadOMMMNew(locationKey, userID, subAmount);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchPOSDiscountAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionOfBasketPOSAPI198(String locationKey, String userID, String subAmount,
			String recpAmount1, String recpAmount2, String recpAmount3) {
		logger.info("Endpoints.processBatchRedemptionOfBasketPOS()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getBatchRedemptionPayloadOMMM198(locationKey, userID, subAmount, recpAmount1,
					recpAmount2, recpAmount3);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// For batch process AUTH API

	public Response processBatchRedemptionOfBasketAUTHAPI(String client, String secret, String locationKey,
			String authToken, String userID, String item_id) {
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.getBatchRedemptionPayloadOMMM(locationKey, userID, item_id);
			String URL = ApiConstants.batchRedemptionAuthAPI + "?client=" + client;
			String payload = URL + body;
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			request.header("Authorization", "Bearer " + authToken);
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionAuthAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// For batch process AUTH API

	public Response deleteItemFromBasket_AuthAPI(String client, String secret, String locationKey, String authToken,
			String basketItemID, String external_uid) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.deleteBasketItem_Payload(basketItemID, external_uid);
			String URL = ApiConstants.deleteBasketID + "?client=" + client;
			String payload = URL + body;
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			request.header("Authorization", "Bearer " + authToken);
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.body(body);
			response = request.delete(ApiConstants.deleteBasketID);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response pointForceRedeem(String authorization, String userId, String points) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Process Redemption Code ======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			String body = apipaylods.pointForceRedeemPayload(userId, points);
			request.body(body);
			response = request.post(ApiConstants.forceRedeem);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("Force Redeem API response: " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// For batch process AUTH API
	// hardik
	public Response processBatchRedemptionOfBasketWithQueryTrueAUTHAPI(String client, String secret, String locationKey,
			String authToken, String userID, String item_id) {
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.posAPIDiscountLookUpPayload(locationKey, userID, item_id);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.discountLookupPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processDiscountLookup() END");

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response postDynamicCoupon1(String token, String email, String campaignUuid) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given();
			String body = apipaylods.dynamicCoupunPayload(email, campaignUuid);
			request.body(requestParams.toJSONString());
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", prop.getProperty("contentType"));
			request.body(body);
			response = request.post(ApiConstants.dynamicCoupon);
			logger.info(response.statusCode());
			logger.info(response.asPrettyString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response singleScanCodeWithPaypalBA(String client, String secret, String token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.singleScanPaypalBAZPayload();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobSingleScanCode + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());

			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobSingleScanCode);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response userLookup(String code, String locationkey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.userLookupPayload(code);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());
			request.header("Authorization", "Token token=" + locationkey);
			request.body(body);
			response = request.get(ApiConstants.posUserLookup);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response singleScanCodeWithCreditCard(String client, String secret, String token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.singleScanCreditCardPayload();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobSingleScanCode + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());

			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobSingleScanCode);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response singleScanCodeWithGiftcard(String client, String secret, String token, String uuid) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.singleScanGiftCardPayload(uuid);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobSingleScanCode + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());

			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobSingleScanCode);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response singleScanCodeNoPaymentMethod(String client, String secret, String token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.singleScanEmptyPayload();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobSingleScanCode + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());

			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobSingleScanCode);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response singleScanCodeWithRedeemableID(String client, String secret, String token, String id) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.singleScanRedeemableIDPayload(id);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobSingleScanCode + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());

			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobSingleScanCode);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response singleScanCodeWithRewardID(String client, String secret, String token, String id) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.singleScanrewardIDPayload(id);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobSingleScanCode + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());

			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobSingleScanCode);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response couponRedemptionOnMobile(String client, String secret, String token, String code) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1CouponRedemptionOnMobile + "?client=" + client + "&hash=" + epoch
					+ "&code=" + code;
			String payload = URL;
			// Headers
			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.queryParam("code", code);
			// request.body(body);
			response = request.post(ApiConstants.mobApi1CouponRedemptionOnMobile);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionAUTHAPIWithProcessValue(String client, String secret, String locationKey,
			String authToken, String userID, String item_id, String externalUid, String processValue, String amount) {
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.getBatchRedemptionPayloadWithProcessValueAuth(locationKey, userID, item_id,
					processValue, amount, externalUid);
			String URL = ApiConstants.batchRedemptionAuthAPI + "?client=" + client;
			String payload = URL + body;
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			request.header("Authorization", "Bearer " + authToken);
			request.header("cache-control", "no-cache");
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			// request.header("Accept-Language", langName);
			request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionAuthAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response POSDiscountLookup(String locationKey, String userID, String item_id) {
		logger.info("Endpoints.processBatchRedemptionOfBasketPOS()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.posAPIDiscountLookUpPayload(locationKey, userID, item_id);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchPOSDiscountAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionPayloadOMMMForDollar10(String locationKey, String userID, String Amount) {
		logger.info("Endpoints.processBatchRedemptionOfBasketPOS()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getBatchRedemptionPayloadOMMMForDollar10(locationKey, userID, Amount);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiSignUp(String email, String client, String secret, String external_source,
			String external_source_id) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("email", email);
			hashmap.put("password", Utilities.getApiConfigProperty("password"));
			hashmap.put("first_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("firstName")));
			hashmap.put("last_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("lastName")));
			hashmap.put("external_source", external_source);
			hashmap.put("external_source_id", external_source_id);
			hashmap.put("birthday", Utilities.getApiConfigProperty("birthday"));
			hashmap.put("anniversary", Utilities.getApiConfigProperty("anniversary"));

			requestParams.put("user", hashmap);
			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.authApiSignUp + param;
			// Headers passing
			request.headers(header.authAPISignUpHeader(payload, secret));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.authApiSignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiFetchUserInfo1(String client, String secret, String token) {
		Response response = null;
		requestParams = new JSONObject();
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.authUpdateUserInfo + param;
			// Headers passing
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			request.headers(defaultHeaderMap);
			request.headers("Authorization", "Bearer " + token);
			request.body(requestParams.toJSONString());
			response = request.get(ApiConstants.authUpdateUserInfo);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiAccountHistory1(String token, String client, String secret) {
		Response response = null;
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.authAccountHistory + param;
			request.body(requestParams.toJSONString());
			// Headers passing
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			defaultHeaderMap.put("Accept", prop.getProperty("contentType"));
			defaultHeaderMap.put("Accept-Language", "en");
			defaultHeaderMap.put("Authorization", "Bearer " + token);
			request.headers(defaultHeaderMap);
			response = request.get(ApiConstants.authAccountHistory);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2SignUp(String email, String client, String secret, String external_source,
			String external_source_id) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile Api2 SignUp======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			// String phoneNumber =
			// CreateDateTime.getUniqueString(prop.getProperty("countryCode"));
			hashmap.put("email", email);
			hashmap.put("password", prop.getProperty("password"));
			// hashmap.put("phone", phoneNumber);
			hashmap.put("first_name", CreateDateTime.getUniqueString(prop.getProperty("firstName")));
			hashmap.put("last_name", CreateDateTime.getUniqueString(prop.getProperty("lastName")));
			hashmap.put("zip_code", prop.getProperty("zipCode"));
			hashmap.put("marketing_email_subscription", "true");
			hashmap.put("signup_channel", prop.getProperty("signUpChannel"));
			hashmap.put("privacy_policy", prop.getProperty("privatePolicy"));
			hashmap.put("birthday", prop.getProperty("birthday"));
			hashmap.put("gender", "Male");
			hashmap.put("external_source", external_source);
			hashmap.put("external_source_id", external_source_id);
			// logger.info("Email in method for API2 ==> " + hashmap);
			requestParams.put("user", hashmap);
			requestParams.put("client", client);
			param = requestParams.toString();
			// logger.info("Email in method for API2 ==> " + param);
			String payload = ApiConstants.mobApi2SignUp + param;
			request.headers(header.api2SignUpHeader(payload, secret));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.mobApi2SignUp);

			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
			// logger.info("Response code: "+response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posSignUpwithAnniversaryForNrewardingDays(String email, String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("email", email);
			requestParams.put("password", Utilities.getApiConfigProperty("password"));
			requestParams.put("first_name",
					CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("firstName")));
			requestParams.put("last_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("lastName")));
			requestParams.put("zip_code", Utilities.getApiConfigProperty("zipCode"));
			requestParams.put("anniversary", CreateDateTime.getFutureDateTime(7));
			request.queryParam("location_key", locationKey);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.posSignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			// TODO Auto-generated catch block
		}
		return response;
	}

	public Response posSignUpwithBirthdayForNrewardingDays(String email, String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		CreateDateTime.getPastYearsDate(20);
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("email", email);
			requestParams.put("password", Utilities.getApiConfigProperty("password"));
			requestParams.put("first_name",
					CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("firstName")));
			requestParams.put("last_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("lastName")));
			requestParams.put("zip_code", Utilities.getApiConfigProperty("zipCode"));
			requestParams.put("birthday", CreateDateTime.getFutureDate(7));
			request.queryParam("location_key", locationKey);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.posSignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			// TODO Auto-generated catch block
		}
		return response;
	}

	public Response identityGenerateBrandLevelToken(String client, String secret) {
		Response response = null;
		try {
			RequestSpecification requestSpec = RestAssured.given().log().everything();
			String identityURL = getGuestIdentityBaseUrl();
			// URL
			requestSpec.baseUri(identityURL);
			String basePath = ApiConstants.identityGenerateBrandLevelToken;
			requestSpec.basePath(basePath);
			logger.info("Using basepath as ==> " + basePath);
			// Body
			String body = new ObjectMapper()
					.writeValueAsString(apipaylods.identityGenerateBrandLevelTokenPayload(client));
			requestSpec.body(body);
			// Headers
			Map<String, Object> headers = new HashMap<String, Object>();
			headers.put("Content-Type", "application/json");
			String payload = basePath + body;
			// logger.info("Using payload as ==> " + payload);
			headers.put("x-pch-digest", apiUtils.getSignature(secret, payload));
			requestSpec.headers(headers);
			// Response
			response = requestSpec.post();
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("error", e.getMessage());
		}
		return response;
	}

	public Response identityUserSignUp(String brandLevelToken, String client, boolean includePassword,
			String punchhAppDeviceId, String userAgent) {
		Response response = null;
		try {
			RequestSpecification requestSpec = RestAssured.given().log().everything();
			// URL
			String identityURL = getGuestIdentityBaseUrl();
			requestSpec.baseUri(identityURL);
			String basePath = ApiConstants.identityUserSignUp;
			requestSpec.basePath(basePath);
			logger.info("Using basepath as ==> " + basePath);
			// Body
			String body = new ObjectMapper()
					.writeValueAsString(apipaylods.identityUserSignUpPayload(client, brandLevelToken, includePassword));
			requestSpec.body(body);
			// Headers
			Map<String, Object> headers = new HashMap<String, Object>();
			headers.put("Content-Type", "application/json");
			headers.put("punchh-app-device-id", punchhAppDeviceId);
			headers.put("User-Agent", userAgent);
			requestSpec.headers(headers);
			// Response
			response = requestSpec.post();
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("error", e.getMessage());
		}
		return response;
	}

	public Response identityUserSignOut(String brandLevelToken, String userAccessToken) {
		Response response = null;
		try {
			RequestSpecification requestSpec = RestAssured.given().log().everything();
			// URL
			String identityURL = getGuestIdentityBaseUrl();
			requestSpec.baseUri(identityURL);
			String basePath = ApiConstants.identityUserSignOut;
			requestSpec.basePath(basePath);
			logger.info("Using basepath as ==> " + basePath);
			// Body
			String body = new ObjectMapper()
					.writeValueAsString(apipaylods.identityUserSignOutPayload(brandLevelToken, userAccessToken));
			requestSpec.body(body);
			// Headers
			Map<String, Object> headers = new HashMap<String, Object>();
			headers.put("Content-Type", "application/json");
			requestSpec.headers(headers);
			// Response
			response = requestSpec.delete();
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("error", e.getMessage());
		}
		return response;
	}

	public Response identityUserSignInWithMobileAPI2AndIdentityClientSecret(String email, String client, String secret,
			String punchhAppDeviceId, String userAgent) {
		Response response = null;
		try {
			RequestSpecification requestSpec = RestAssured.given().log().everything();
			// URL
			logger.info("Punchh base Url is ==> " + baseUri);
			requestSpec.baseUri(baseUri);
			String basePath = ApiConstants.identityPunchhNewSignIn;
			requestSpec.basePath(basePath);
			logger.info("Using basepath as ==> " + basePath);
			// Body
			String body = new ObjectMapper().writeValueAsString(
					apipaylods.identityUserSignInWithMobileAPI2AndIdentityClientSecretPayload(email, client));
			requestSpec.body(body);
			// Headers
			Map<String, Object> headers = new HashMap<String, Object>();
			headers.put("Content-Type", "application/json");
			String payload = basePath + body;
			logger.info("Using payload as ==> " + payload);
			headers.put("x-pch-digest", apiUtils.getSignature(secret, payload));
			headers.put("punchh-app-device-id", punchhAppDeviceId);
			headers.put("User-Agent", userAgent);
			requestSpec.headers(headers);
			// Response
			response = requestSpec.post();
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("error", e.getMessage());
		}
		return response;
	}

	public Response api2UpdateUserExternalSourceAndID(String user_id, String ext_source, String ext_source_id,
			String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Update a User======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().everything();
			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			// Body
			String body = new ObjectMapper().writeValueAsString(
					apipaylods.api2UpdateUserExternalSourceAndIDPayload(user_id, ext_source, ext_source_id));
			request.body(body);
			response = request.patch(ApiConstants.updateUser);
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("error", e.getMessage());
		}
		return response;
	}

	public Response identityGenerateBrandUserToken(String email, String brand) {
		Response response = null;
		try {
			RequestSpecification requestSpec = RestAssured.given().log().everything();
			Map<String, Object> headers = new HashMap<String, Object>();
			// URL
			String brandUrl = "";
			String body = "";
			switch (brand.toLowerCase()) {
			case "menu":
				brandUrl = "https://api-playground.menu.app/api/customers/register";
				headers.put("Content-Type", "application/json");
				headers.put("Accept", "application/json");
				headers.put("Application",
						utils.decrypt("zR2CVUhtdpzo9ElhntxzmcPWiZ3Nix00V/9Bie1Jx/PaQzYRDY2S9WVv0us+O/r/"));
				body = new ObjectMapper()
						.writeValueAsString(apipaylods.identityGenerateBrandUserTokenPayload(email, brand));
				break;
			}
			logger.info("Brand (" + brand + ") base Url is ==> " + brandUrl);
			requestSpec.baseUri(brandUrl);
			requestSpec.body(body);
			requestSpec.headers(headers);
			response = requestSpec.post();
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("error", e.getMessage());
		}
		return response;
	}

	public Response identityUserSignIn(String brandLevelToken, String client, String email, String brand) {
		return identityUserSignIn(brandLevelToken, client, email, brand, null);
	}

	public Response identityUserSignIn(String brandLevelToken, String client, String email, String brand,
			String verificationKey) {
		Response response = null;
		try {
			RequestSpecification requestSpec = RestAssured.given().log().everything();
			// URL
			String identityURL = getGuestIdentityBaseUrl();
			requestSpec.baseUri(identityURL);
			String basePath = ApiConstants.identityUserSignIn;
			requestSpec.basePath(basePath);
			logger.info("Using basepath as ==> " + basePath);
			// Generate Brand User token
			String brandUserToken = "";
			switch (brand.toLowerCase()) {
			case "menu":
				brandUserToken = identityGenerateBrandUserToken(email, brand).jsonPath().get("data.token.value");
			}
			logger.info("brandUserToken ==> " + brandUserToken.toString());
			// Body
			String body = new ObjectMapper().writeValueAsString(
					apipaylods.identityUserSignInPayload(brandLevelToken, brandUserToken, verificationKey));
			requestSpec.body(body);
			// Headers
			Map<String, Object> headers = new HashMap<String, Object>();
			headers.put("Content-Type", "application/json");
			requestSpec.headers(headers);
			// Response
			response = requestSpec.post();
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("error", e.getMessage());
		}
		return response;
	}

	public Response identityUserSync(String punchh_user_id, String email) {
		Response response = null;
		try {
			RequestSpecification requestSpec = RestAssured.given().log().everything();
			// URL
			String identityURL = getGuestIdentityBaseUrl();
			requestSpec.baseUri(identityURL);
			String basePath = ApiConstants.identityUserSync;
			requestSpec.basePath(basePath);
			logger.info("Using basepath as ==> " + basePath);
			// Body
			Map<String, Object> payload = new HashMap<>();
			payload.put("punchh_user_id", punchh_user_id);
			payload.put("email", email);
			String body = new ObjectMapper().writeValueAsString(payload);
			requestSpec.body(body);
			// Headers
			Map<String, Object> headers = new HashMap<String, Object>();
			headers.put("Content-Type", "application/json");
			requestSpec.headers(headers);
			// Response
			response = requestSpec.patch();
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("error", e.getMessage());
		}
		return response;
	}

	public Response advancedAuthToken(String client, String validationMethod, String email, String phone_ext,
			String phone_number, String codeChallenge, Boolean privacyPolicy, Boolean termsAndConditions) {
		return advancedAuthToken(client, validationMethod, email, phone_ext, phone_number, codeChallenge, privacyPolicy,
				termsAndConditions, null);
	}

	// With user_agent
	public Response advancedAuthToken(String client, String validationMethod, String email, String phone_ext,
			String phone_number, String codeChallenge, Boolean privacyPolicy, Boolean termsAndConditions,
			String userAgent) {
		Response response = null;
		try {
			RequestSpecification requestSpec = RestAssured.given().log().everything();
			String baseUri = getGuestIdentityBaseUrl();
			// Uri
			requestSpec.baseUri(baseUri);
			logger.info("Using base Uri as ==> " + baseUri);
			// Path
			String basePath = ApiConstants.advanceAuthToken;
			requestSpec.basePath(basePath);
			logger.info("Using basepath as ==> " + basePath);
			// Body
			Map<String, Object> payload = new HashMap<>();
			payload.put("client", client);
			payload.put("validation_method", validationMethod);
			putIfNotEmpty(payload, "email", email);
			putIfNotEmpty(payload, "phone_ext", phone_ext);
			putIfNotEmpty(payload, "phone_number", phone_number);
			payload.put("code_challenge", codeChallenge);
			payload.put("privacy_policy", privacyPolicy);
			payload.put("terms_and_conditions", termsAndConditions);
			String body = new ObjectMapper().writeValueAsString(payload);
			requestSpec.body(body);
			// Headers
			Map<String, Object> headers = new HashMap<String, Object>();
			headers.put("Content-Type", "application/json");
			requestSpec.headers(headers);
			// Response
			response = requestSpec.post();
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("error", e.getMessage());
		}
		return response;
	}

	// For Iframe SSO with redirect_uri
	public Response advancedAuthVerify(String client, String email, String phone_ext, String phone_number, String token,
			String codeVerifier, String clientTypeHeader, String redirect_uri) {
		return advancedAuthVerify(client, email, phone_ext, phone_number, token, codeVerifier, clientTypeHeader,
				redirect_uri, null);
	}

	// With user_agent
	public Response advancedAuthVerify(String client, String email, String phone_ext, String phone_number, String token,
			String codeVerifier, String clientTypeHeader) {
		return advancedAuthVerify(client, email, phone_ext, phone_number, token, codeVerifier, clientTypeHeader, null,
				null);
	}

	// For Iframe SSO with redirect_uri and user_agent
	public Response advancedAuthVerify(String client, String email, String phone_ext, String phone_number, String token,
			String codeVerifier, String clientTypeHeader, String redirect_uri, String user_agent) {
		Response response = null;
		try {
			RequestSpecification requestSpec = RestAssured.given().log().everything();
			// Uri
			String baseUri = getGuestIdentityBaseUrl();
			requestSpec.baseUri(baseUri);
			logger.info("Using base Uri as ==> " + baseUri);
			// Path
			String basePath = ApiConstants.advanceAuthVerify;
			requestSpec.basePath(basePath);
			logger.info("Using basepath as ==> " + basePath);
			// Body
			Map<String, Object> payload = new HashMap<>();
			putIfNotEmpty(payload, "token", token);
			putIfNotEmpty(payload, "email", email);
			putIfNotEmpty(payload, "phone_ext", phone_ext);
			putIfNotEmpty(payload, "phone_number", phone_number);
			putIfNotEmpty(payload, "redirect_uri", redirect_uri);
			payload.put("code_verifier", codeVerifier);
			payload.put("client", client);
			String body = new ObjectMapper().writeValueAsString(payload);
			requestSpec.body(body);
			// Headers
			Map<String, Object> headers = new HashMap<String, Object>();
			headers.put("Content-Type", "application/json");
			headers.put("client-type", clientTypeHeader);
			putIfNotEmpty(headers, "User-Agent", user_agent);
			requestSpec.headers(headers);
			// Response
			response = requestSpec.post();
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("error", e.getMessage());
		}
		return response;
	}

	public Response advancedAuthRefresh(String client, String refresh_token) {
		Response response = null;
		try {
			RequestSpecification requestSpec = RestAssured.given().log().everything();
			// Uri
			String baseUri = getGuestIdentityBaseUrl();
			requestSpec.baseUri(baseUri);
			String basePath = ApiConstants.advanceAuthRefresh;
			requestSpec.basePath(basePath);
			Map<String, Object> payload = new HashMap<>();
			payload.put("refresh_token", refresh_token);
			payload.put("client", client);
			String body = new ObjectMapper().writeValueAsString(payload);
			requestSpec.body(body);
			Map<String, Object> headers = new HashMap<String, Object>();
			headers.put("Content-Type", "application/json");
			requestSpec.headers(headers);
			response = requestSpec.post();
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("error", e.getMessage());
		}
		return response;
	}

	public Response advancedAuthSignOut(String client, String refresh_token, String access_token) {
		Response response = null;
		try {
			RequestSpecification requestSpec = RestAssured.given().log().everything();
			String baseUri = getGuestIdentityBaseUrl();
			requestSpec.baseUri(baseUri);
			String basePath = ApiConstants.advanceAuthSignOut;
			requestSpec.basePath(basePath);
			Map<String, Object> payload = new HashMap<>();
			payload.put("refresh_token", refresh_token);
			payload.put("access_token", access_token);
			payload.put("client", client);
			String body = new ObjectMapper().writeValueAsString(payload);
			requestSpec.body(body);
			Map<String, Object> headers = new HashMap<String, Object>();
			headers.put("Content-Type", "application/json");
			requestSpec.headers(headers);
			response = requestSpec.delete();
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("error", e.getMessage());
		}
		return response;
	}

	public Response enqueueIdentityWorker(String workerName) throws Exception {
		Response response = null;
		try {
			String digest = apiUtils.getSignature(
					utils.decrypt(Utilities.getConfigProperty(env.toLowerCase() + ".cron_digest_token")), workerName);
			logger.info("Generated digest ==> " + digest);
			String baseUri = getGuestIdentityBaseUrl();
			RequestSpecification requestSpec = RestAssured.given().log().everything();
			// Uri
			requestSpec.baseUri(baseUri);
			logger.info("Using base Uri as ==> " + baseUri);
			// Path
			String basePath = ApiConstants.enqueueWorker;
			requestSpec.basePath(basePath);
			logger.info("Using basepath as ==> " + basePath);
			// Headers
			Map<String, Object> headers = new HashMap<String, Object>();
			headers.put("Content-Type", "application/json");
			headers.put("digest", digest);
			requestSpec.headers(headers);
			// Params
			requestSpec.queryParam("worker_name", workerName);
			// Response
			response = requestSpec.post();
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("error", e.getMessage());
		}
		return response;
	}

	public Response enqueueWorker(String workerName) {
		Response response = null;
		try {
			String digest = apiUtils.getSignature(
					utils.decrypt(Utilities.getConfigProperty(env.toLowerCase() + ".cron_digest_token")), workerName);
			logger.info("Generated digest ==> " + digest);
			RequestSpecification requestSpec = RestAssured.given().log().everything();
			// Uri
			requestSpec.baseUri(baseUri);
			logger.info("Using base Uri as ==> " + baseUri);
			// Path
			String basePath = ApiConstants.enqueueWorker;
			requestSpec.basePath(basePath);
			logger.info("Using basepath as ==> " + basePath);
			// Headers
			Map<String, Object> headers = new HashMap<String, Object>();
			headers.put("Content-Type", "application/json");
			headers.put("digest", digest);
			requestSpec.headers(headers);
			// Params
			requestSpec.queryParam("worker_name", workerName);
			// Response
			response = requestSpec.post();
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("error", e.getMessage());
		}
		return response;
	}

	public Response getSegmentCount(String authorization, String segmentId) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("Hitting Get segment guest count api");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Accept", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + authorization);
			response = request.get(ApiConstants.fetchSegmentGuestCount + segmentId + "/segment_size");
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response fetchActiveBasketPOSAPI(String userID, String locationKey, String externalUID) {

		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.fetchActiveBasketPOSAPIPayload(userID, externalUID);
			request.header("Authorization", "Token token=" + locationKey);
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.body(body);
			response = request.get(ApiConstants.getDiscountBasketPOS);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;

	}

	public Response removeDiscountFromBasketPOSAPI(String locationKey, String userID, String discount_basket_item_id,
			String externalUID) {
		Response response = null;

		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.removedDiscountFromBasketPOSApiPayload(userID, discount_basket_item_id,
					externalUID);
			request.header("Authorization", "Token token=" + locationKey);
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.header("Accept-Language", "es");
			request.body(body);
			response = request.delete(ApiConstants.deleteBasketForUserPOS);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response basketUnlockPOSAPI(String locationKey, String userID, String externalUID) {
		Response response = null;

		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.basketUnlockPOSApiPayload(userID, externalUID);
			request.header("Authorization", "Token token=" + locationKey);
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.header("Accept-Language", "es");
			request.body(body);
			response = request.put(ApiConstants.deleteBasketForUserPOS);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response addDiscountToBasketAUTH(String authToken, String client, String secret, String discountType,
			String discountID, String externalUid) {
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.addDiscountItemPAuthApiayload(discountType, discountID, externalUid);
			String URL = ApiConstants.discountBasketAddedAUTH + "?client=" + client;
			String payload = URL + body;
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			request.header("Authorization", "Bearer " + authToken);
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.discountBasketAddedAUTH);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response fetchActiveBasketAuthApi(String authToken, String client, String secret, String externalUid) {
		Response response = null;
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.fetchActiveBasketAuthApiayload(externalUid);
			// param = requestParams.toString();
			String URL = ApiConstants.getDiscountBasketAUTH + "?client=" + client;
			String payload = URL + body;
			request.body(requestParams.toJSONString());
			// Headers passing
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			defaultHeaderMap.put("Accept", prop.getProperty("contentType"));
			defaultHeaderMap.put("Accept-Language", "en");
			request.header("Authorization", "Bearer " + authToken);
			request.headers(defaultHeaderMap);
			request.queryParam("client", client);
			request.body(body);
			response = request.get(ApiConstants.getDiscountBasketAUTH);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response userLookupPosApi(String lookUpField, String userEmail, String locationkey, String externalUid)
			throws InterruptedException {
		Thread.sleep(2000);
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.userLookupPosAPIPayload(lookUpField, userEmail, externalUid);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());
			request.header("Authorization", "Token token=" + locationkey);
			request.body(body);
			response = request.get(ApiConstants.posUserLookup);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1MobileUpdateGuestDetails(String Fname, String Lname, String Npwd, String client, String secret,
			String token, String newEmail) {

		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API1UpdateGuestDetails(Fname, Lname, Npwd, newEmail);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1UpdateGuestDetails + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;
			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept", api1Headers.accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.put(ApiConstants.mobApi1UpdateGuestDetails);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1MobileUpdateGuestDetailsWithoutEmail(String Npwd, String phone, String client, String secret,
			String token) {

		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API1UpdateGuestDetailsWithoutEmailPayload(Npwd, phone);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1UpdateGuestDetails + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;
			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept", api1Headers.accept());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.put(ApiConstants.mobApi1UpdateGuestDetails);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2GiftaCardWithOrderID(String email, String client, String secret, String access_token,
			String design_id, String amount, String expDate, String firstName, String orderID) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2  Gift a Card======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2GiftaCardPayloadWithOrderID(email, client, design_id, amount, expDate,
					firstName, orderID);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2GiftaCard + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Accept", api2Headers.api2Accept());
			request.header("Authorization", "Bearer " + access_token);
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Content-Type", api2Headers.api2ContentType());

			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.post(ApiConstants.mobApi2GiftaCard);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get()
					.info(ApiConstants.mobApi2GiftaCard + " API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// For batch process AUTH API
	public Response discountLookUpPosApi(String locationKey, String userID, String item_id, String amount,
			String externalUid) throws InterruptedException {
		Thread.sleep(7000);
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.discountLookUpPosApiPayload(locationKey, userID, item_id, externalUid, amount);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.discountLookupPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processDiscountLookup() END");
			TestListeners.extentTest.get().info("POS Discount Lookup API response: " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionPosApiPayload(String locationKey, String userID, String Amount, String qty,
			String id, String externalUid) {
		logger.info("Endpoints.processBatchRedemptionOfBasketPOS()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.processBatchRedemptionPosApiPayload(locationKey, userID, Amount, qty, id,
					externalUid);
			request.header("Content-Type", "application/json");
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionAUTHAPI(String client, String secret, String locationKey, String authToken,
			String userID, String item_id, String externalUid) {
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.getBatchRedemptionAuthApiPayload(locationKey, userID, item_id, externalUid);
			String URL = ApiConstants.batchRedemptionAuthAPI + "?client=" + client;
			String payload = URL + body;
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			request.header("Authorization", "Bearer " + authToken);
			request.header("cache-control", "no-cache");
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionAuthAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response autoSelectPosApi(String userID, String Amount, String qty, String itemId, String externalUid,
			String locationkey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.autoSelectPOSAPiPayload(userID, Amount, qty, itemId, externalUid);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());
			request.body(body);
			request.queryParam("location_key", locationkey);
			response = request.post(ApiConstants.autoUnlockPosAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response ssoUserTokenMobileApi(String client, String secret, String token, String rewardId,
			String paymentType, String paymentToken, String giftCardUuid, String redeemableId, String redemptionCode,
			String coupon) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String hash = apiUtils.getHash(secret + epoch);

			String body = apipaylods.ssoUserTokenMobilePayload(rewardId, paymentType, paymentToken, giftCardUuid,
					redeemableId, redemptionCode, coupon);
			String URL = ApiConstants.mobApi2ssoUserTokensMobile + "?client=" + client;
			String payload = URL + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			request.headers("x-pch-hash", hash);
			request.headers("Authorization", "Bearer " + token);
			request.body(body);
			request.queryParam("client", client);
			response = request.post(ApiConstants.mobApi2ssoUserTokensMobile);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response createLocationMultipleRedemption(String name, String store_number, String authorization,
			String locationId, String enable_multiple_redemptions) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Create Location======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Accept", api2Headers.api2Accept());
			request.header("Content-Type", api2Headers.api2ContentType());
			// String URL = ApiConstants.createLocation +"?location_id="+locationId;
			String body = apipaylods.createLocationMultipleLocationPayload(name, store_number,
					enable_multiple_redemptions, locationId);
			request.body(body);
			response = request.post(ApiConstants.createLocation);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response updateLocationMultipleRedemption(String location_id, String authorization,
			String enable_multiple_redemptions) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Update Location======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			String body = apipaylods.updateLocationMultipleLocationPayload(location_id, enable_multiple_redemptions);
			request.body(body);
			response = request.patch(ApiConstants.updateLocation);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1TransferLoyaltyPointsToUser(String email, String points, String client, String secret,
			String token) {

		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API1TransferLoyaltyPointsToUser(email, points);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1TransferLoyaltypoints + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept", api1Headers.accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1TransferLoyaltypoints);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2ReloadGiftCardWithOrderID(String client, String secret, String access_token, String uuid,
			String amount, String firstName, String expDate, String orderID) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Reload Gift Card======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2ReloadGiftCardPayloadWithOrderID(client, amount, firstName, expDate, orderID);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2ReloadGiftCard + uuid + "/reload" + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Accept", api2Headers.api2Accept());
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.body(requestParams.toJSONString());
			request.body(body);
			response = request.post(ApiConstants.mobApi2ReloadGiftCard + uuid + "/reload");
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response getDashboardBusinessConfig(String authorization, String slugId) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======Dashboard API Get Dashboard Business Config======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			response = request.get(ApiConstants.getDashboardBusinessConfig + slugId);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2PurchaseGiftCardWithOrderID(String client, String secret, String access_token, String design_id,
			String amount, String expDate, String firstName, String orderID) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Purchase Gift Card======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2PurchaseGiftCardPayloadWithOrderID(client, access_token, design_id, amount,
					expDate, firstName, orderID);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2PurchaseGiftCard + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Content-Type", api2Headers.api2ContentType());

			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.post(ApiConstants.mobApi2PurchaseGiftCard);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response updateDashboardBusinessConfig(String authorization, String slugId, String res) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======Dashboard API Update Dashboard Business Config======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			String body = apipaylods.updateDashboardBusinessConfigPayload(res);
			request.body(body);
			response = request.put(ApiConstants.getDashboardBusinessConfig + slugId);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response updateDashboardBusinessConfigSingleKey(String authorization, String slugId, String res) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======Dashboard API Update Dashboard Business Config======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			String body = apipaylods.updateDashboardBusinessConfigPayloadSingleKey(res);
			request.body(body);
			response = request.put(ApiConstants.getDashboardBusinessConfig + slugId);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response closeOrderOnline(String client, String secret, String slugName, String externalUid,
			String authorization, String emailID, String storeNumber, String posRef, String oloSlug, String csID,
			String orderID) {
		String oloTimeStamp = CreateDateTime.getTimeDateString();
		String oloSecretKey = "et3LdDo-SvqsaGQeqICXFVoMNaJUbm5ugePTOoN65rWfmJaHrhjo34QLQfow-e8e";
		UUID uuid = UUID.randomUUID();
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Purchase Gift Card======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.closeOrderOnlinePayload(externalUid, emailID, storeNumber, posRef, oloSlug, csID,
					orderID);

			String closeOrderOnineURL = baseUri + ApiConstants.closeOrderOnine.replace("$slugName", slugName);

			// Headers passing Signature and content type
			String payload = closeOrderOnineURL + "\n" + body + "\n" + uuid.toString() + "\n" + oloTimeStamp;

			String signature = apiUtils.getOloSignature(oloSecretKey, payload);
			logger.info("Signature -------- " + signature);

			request.header("Accept", "application/json");
			request.header("X-Olo-event-Type", "OrderClosed");
			request.header("Content-Type", "application/json");
			request.header("X-Olo-Signature", signature);
			request.header("X-Olo-Timestamp", oloTimeStamp);
			request.header("X-Olo-Message-Id", uuid.toString());
			request.headers("Authorization", "Bearer " + authorization);
			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.post(closeOrderOnineURL);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}

		return response;
	}

	public Response Api2SubscriptionPurchase(String token, String plan_id, String client, String secret,
			String purchase_price, String uuid, String subscriptionAdaptorType) {
		Response response = null;
		String body = "";
		// requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			switch (subscriptionAdaptorType) {
			case "Heartland":
				body = apipaylods.api2PurchaseSubscriptionPayloadWithPaymentCardWithoutStartEndDateTime(plan_id,
						purchase_price, uuid);
				break;
			default:
				body = apipaylods.api2PurchaseSubscriptionPayloadWithPaymentCardUuid(plan_id, purchase_price, uuid);
				break;
			}
			String payload = ApiConstants.mobApi2PurchaseSubscription + "?client=" + client + body;
			// Headers
			request.header("Authorization", "Bearer " + token);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.mobApi2PurchaseSubscription);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authOnlineRewardRedemption(String client, String secret, String authentication_token,
			String reward_id, String subAmount, Map<String, Map<String, String>> parentMap) {
		Response response = null;
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.authRedemptionwithRewardPayload(authentication_token, reward_id, subAmount, client,
					parentMap);
			String payload = ApiConstants.authOnlineRedemption + body;
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			request.headers(defaultHeaderMap);
			request.body(body);
			response = request.post(ApiConstants.authOnlineRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response headRequest(String url) {
		Response response = null;
		try {
			logger.info("======Verify Apple Pass URL======");
			RestAssured.baseURI = url;
			RequestSpecification request = RestAssured.given().log().all();
			response = request.head();
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posCheckinN_QC(String client, String secret, String locationkey, String subAmount, String email,
			String date, String external_uid, String receiptAmt, Map<String, Map<String, String>> parentMap) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			String body = apipaylods.posCheckinWith_N_QCPayload(email, subAmount, client, date, external_uid,
					receiptAmt, parentMap);
			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request1.header("Authorization", "Token token=" + locationkey);
			request1.body(body);
			response = request1.post(ApiConstants.posCheckin);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posUserLookupSingleScanToken(String single_scan_code, String location) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("*****POS User lookup Fetch Balance API*****");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.queryParam("single_scan_code", single_scan_code);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			requestParams.put("location_key", location);
			request.body(requestParams.toJSONString());
			response = request.get(ApiConstants.userLookupAndFetchBalance);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2FetchUserExpiringPoints(String client, String secret, String access_token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======API2 fetch user expiring points======");
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			requestParams.put("client", client);
			requestParams.put("access_token", access_token);
			request.body(requestParams.toJSONString());
			param = requestParams.toString();
			String payload = ApiConstants.mobApi2FetchUserExpiringPoints + param;
			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			// request.header("Authorization", "Bearer " + access_token);
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			response = request.get(ApiConstants.mobApi2FetchUserExpiringPoints);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response onlineOrderCheckin(String authenticationToken, String amount, String client, String secret,
			String storeNumber) {
		Response response = null;
		try {
			transactionNumber = "123456" + CreateDateTime.getTimeDateString();
			externalUid = "TestUid" + CreateDateTime.getTimeDateString();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = ApiPayloads.onlineCheckinPayLoad(authenticationToken, amount, client, transactionNumber,
					externalUid, storeNumber);
			String payload = ApiConstants.onlineOrderCheckin + body;
			request.headers(header.onlineOrderCheckinHeader(payload.toString(), secret));
			request.body(body);
			response = request.post(ApiConstants.onlineOrderCheckin);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response v2Checkin(String client, String secret, String locationkey, String userEmail, String amount,
			String barcode, String manuItem1, String manuItem2, String receipt_datetime, String transaction_no,
			String channel) throws InterruptedException {
		Thread.sleep(10000);
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			logger.info("====== v2 checkin ======");
			// Headers passing Signature and content type
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Token token=" + locationkey);
			request.header("Accept", "*/*");
			// Body
			String body = apipaylods.v2CheckinPayload(userEmail, amount, barcode, manuItem1, manuItem2,
					receipt_datetime, transaction_no, channel);
			// String body = new ObjectMapper().writeValueAsString(
			// apipaylods.v2CheckinPayload(userEmail, amount,barcode, manuItem1, manuItem2,
			// receipt_datetime, transaction_no, channel));
			request.body(body);
			response = request.post(ApiConstants.V2checkins);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// To send challenge campaign to user
	public Response API2SendMessageToUserChallengeCampaign(String choice, String userID, String authToken,
			String challenge_campaign_id, String progress_count) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Send message to user======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2SendMessageToUserChallengeCampaignPayload(choice, userID,
					challenge_campaign_id, progress_count);
			// Headers
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + authToken);
			request.body(body);
			response = request.post(ApiConstants.sendMessageToUser);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response addDiscountAmountToBasketAUTH(String authToken, String client, String secret, String discountType,
			String discountID, String externalUid) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.addDiscountAmountPAuthApiayload(discountType, discountID, externalUid);
			String URL = ApiConstants.discountBasketAddedAUTH + "?client=" + client;
			String payload = URL + body;
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			request.header("Authorization", "Bearer " + authToken);
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.discountBasketAddedAUTH);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response removeDiscountBasketExtUIDSecureAPI(String authToken, String client, String secret,
			String discount_basket_item_id, String external_uid) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String epoch = apiUtils.getSpan();
			String hash = apiUtils.getHash(secret + epoch);
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.removeDiscountBasketExtUIDSecureAPIPayload(discount_basket_item_id, external_uid);
			String URL = ApiConstants.deleteBasketForUserAPI + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;
			request.header("x-pch-digest", apiUtils.getSignature(secret, payload));
			request.header("Authorization", "Bearer " + authToken);
			request.header("Content-Type", "application/json");
			request.header("x-pch-hash", hash);
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.delete(ApiConstants.deleteBasketForUserAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response pickItemRecommendationSystemAPI(String client, String secret, String bid, String token,
			String buuid, String uid) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			apiUtils.getHash(secret + epoch);
			String url = ApiConstants.systemRecommendation + "?client=" + client + "&hash=" + epoch + "&bid=" + bid
					+ "&buuid=" + buuid + "&uid=" + uid;
			request.header("x-pch-digest", api1Headers.signature(url, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Connection", "keep-alive");
			request.header("Content-Type", api1Headers.contentType());
			request.header("User-Agent", api1Headers.userAgent());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.queryParam("bid", bid);
			request.queryParam("buuid", buuid);
			request.queryParam("uid", uid);

			response = request.get(ApiConstants.systemRecommendation);
			TestListeners.extentTest.get().info("Mobile API IRS Pick System response: " + response.asString());
			logger.info("Mobile API IRS Pick System response: " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response getItemRecommendationsAPI(String client, String secret, String token, String getItemRecsVars,
			String cids, String bid, String buuid, String uid) {
		Response response = null;
		requestParams = new JSONObject();
		String eid = getItemRecsVars.split("&")[0].replace("eid=", "");
		String expVar = getItemRecsVars.split("&")[1].replace("exp_var=", "");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			apiUtils.getHash(secret + epoch);
			String url = ApiConstants.itemRecommendation + "?client=" + client + "&hash=" + epoch + "&uid=" + uid
					+ "&eid=" + eid + "&exp_var=" + expVar + "&bid=" + bid + "&buuid=" + buuid + "&cids=" + cids;
			request.header("x-pch-digest", api1Headers.signature(url, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Connection", "keep-alive");
			request.header("Content-Type", api1Headers.contentType());
			request.header("User-Agent", api1Headers.userAgent());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.queryParam("uid", uid);
			request.queryParam("eid", eid);
			request.queryParam("exp_var", expVar);
			request.queryParam("bid", bid);
			request.queryParam("buuid", buuid);
			request.queryParam("cids", cids);

			response = request.get(ApiConstants.itemRecommendation);
			TestListeners.extentTest.get().info("Mobile API IRS Get items response: " + response.asString());
			logger.info("Mobile API IRS Get items response: " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response itemRecSysServiceApi(String requestType, String apiKey, String buuid, String getItemRecsVars,
			String uid, String cids, String mids, String bid) {
		Response response = null;
		try {
			RestAssured.baseURI = prop.getProperty("irsServiceBaseUrl");
			RequestSpecification request = RestAssured.given().log().all();
			request.header("apikey", apiKey);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			String path = "";
			if (requestType.equals("pick system")) {
				path = "/item-rec/system?buuid=" + buuid;
				request.queryParam("buuid", buuid);
				if (!bid.isEmpty()) {
					path += "&bid=" + bid;
					request.queryParam("bid", bid);
				}
				if (!uid.isEmpty()) {
					path += "&uid=" + uid;
					request.queryParam("uid", uid);
				}
				response = request.get(ApiConstants.itemRecServiceApi + "/system");
			} else if (requestType.equals("get items")) {
				path = "/item-rec/items?buuid=" + buuid;
				request.queryParam("buuid", buuid);
				if (!getItemRecsVars.isEmpty()) {
					String eid = getItemRecsVars.split("&")[0].replace("eid=", "");
					String expVar = getItemRecsVars.split("&")[1].replace("exp_var=", "");
					path += "&eid=" + eid + "&exp_var=" + expVar;
					request.queryParam("eid", eid);
					request.queryParam("exp_var", expVar);
				}
				if (!uid.isEmpty()) {
					path += "&uid=" + uid;
					request.queryParam("uid", uid);
				}
				if (!bid.isEmpty()) {
					path += "&bid=" + bid;
					request.queryParam("bid", bid);
				}
				if (!cids.isEmpty()) {
					path += "&cids=" + cids;
					request.queryParam("cids", cids);
				}
				if (!mids.isEmpty()) {
					path += "&mids=" + mids;
					request.queryParam("mids", mids);
				}
				response = request.get(ApiConstants.itemRecServiceApi + "/items");
			} else if (requestType.contains("ping")) {
				path = "/ping";
				if (requestType.equals("ping full check")) {
					path = path + "?full_check=1";
					request.queryParam("full_check", 1);
				}
				response = request.get(path);
			}
			utils.logit(path + " response: " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response itemRecSysAdminApi(String requestType, String apiKey, String buuid, String otherParam) {
		utils.longWaitInSeconds(2); // delay added to ensure API stability
		Response response = null;
		try {
			RestAssured.baseURI = prop.getProperty("irsAdminBaseUrl");
			RequestSpecification request = RestAssured.given().log().all();
			request.header("apikey", apiKey);
			String path = "";
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			if (requestType.equals("ping")) {
				path = "/ping";
				response = request.get(path);
			} else if (requestType.equals("ping full check")) {
				path = "/ping?full_check=" + otherParam;
				request.queryParam("full_check", otherParam);
				response = request.get(path);
			} else if (requestType.equals("get settings")) {
				path = "/settings";
				response = request.get(ApiConstants.itemRecAdminApi + buuid + path);
			} else if (requestType.equals("get user")) {
				path = "/user/";
				response = request.get(ApiConstants.itemRecAdminApi + buuid + path + otherParam);
			} else if (requestType.equals("delete user")) {
				path = "/user/";
				response = request.delete(ApiConstants.itemRecAdminApi + buuid + path + otherParam);
			} else if (requestType.equals("get specific menu")) {
				path = "/menu?menu_id=" + otherParam;
				request.queryParam("menu_id", otherParam);
				response = request.get(ApiConstants.itemRecAdminApi + buuid + path);
			} else if (requestType.equals("get menu")) {
				path = "/menu";
				response = request.get(ApiConstants.itemRecAdminApi + buuid + path);
			}
			utils.logit(path + " response: " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response irsMenuAdjusterBulkGetApi(String requestType, String apiKey, String buuid) {
		utils.longWaitInSeconds(2); // delay added to ensure API stability
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = prop.getProperty("irsAdminBaseUrl");
			RequestSpecification request = RestAssured.given().log().all();
			request.header("apikey", apiKey);
			String path = "";
			if (requestType.equals("menu adjuster bulk")) {
				path = "/settings/menu-adjuster-bulk";
				response = request.get(ApiConstants.itemRecAdminApi + buuid + path);
			} else if (requestType.equals("menu adjuster bulk other")) {
				path = "/settings/" + buuid + "/menu-adjuster-bulk";
				response = request.get(path);
			}
			utils.logit(path + " response: " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response irsMenuAdjusterBulkPostApi(String requestType, String apiKey, String buuid, String attribute1Key,
			Object attribute1Value, String attribute2Key, Object attribute2Value) {
		utils.longWaitInSeconds(2); // delay added to ensure API stability
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = prop.getProperty("irsAdminBaseUrl");
			RequestSpecification request = RestAssured.given().log().all();
			request.header("apikey", apiKey);
			String path = "";
			if (requestType.equals("menu adjuster bulk other")) {
				path = "/settings/" + buuid + "/menu-adjuster-bulk";
				requestParams.put(attribute1Key, attribute1Value);
				requestParams.put(attribute2Key, attribute2Value);
				param = requestParams.toString();
				request.header("Content-Type", prop.getProperty("contentType"));
				request.body(param);
				response = request.post(path);
			} else {
				path = "/settings/menu-adjuster-bulk";
				request.header("Content-Type", prop.getProperty("contentType"));
				if (requestType.equals("menu adjuster bulk valid")) {
					requestParams.put(attribute1Key, attribute1Value);
					requestParams.put(attribute2Key, attribute2Value);
					param = requestParams.toString();
					request.body(param);
				} else if (requestType.equals("menu adjuster bulk invalid")) {
					String payload = apipaylods.menuAdjusterInvalidPayload();
					request.body(payload);
				} else if (requestType.equals("menu adjuster bulk empty")) {
					param = requestParams.toString();
					request.body(param);
				}
				response = request.post(ApiConstants.itemRecAdminApi + buuid + path);
			}
			utils.logit(path + " response: " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response irsMenuAdjusterPostApi(String requestType, String apiKey, String buuid, String attribute_name,
			String attribute_value, String attribute_type) {
		utils.longWaitInSeconds(4); // delay added to ensure API stability
		Response response = null;
		try {
			RestAssured.baseURI = prop.getProperty("irsAdminBaseUrl");
			RequestSpecification request = RestAssured.given().log().all();
			request.header("apikey", apiKey);
			String path = "";
			if (requestType.equals("update value other")) {
				path = "/settings/" + buuid + "/menu-adjuster?attribute_name=" + attribute_name + "&attribute_value="
						+ attribute_value;
				request.queryParam("attribute_name", attribute_name);
				request.queryParam("attribute_value", attribute_value);
				response = request.post(path);
			} else {
				path = "/settings/menu-adjuster?attribute_name=" + attribute_name;
				request.queryParam("attribute_name", attribute_name);
				if (requestType.equals("update value")) {
					path = path + "&attribute_value=" + attribute_value;
					request.queryParam("attribute_value", attribute_value);
				} else if (requestType.equals("update unquoted type")) {
					path = path + "&attribute_value=" + attribute_value + "&attribute_type=" + attribute_type;
					request.queryParam("attribute_value", attribute_value);
					request.queryParam("attribute_type", attribute_type);
				} else if (requestType.equals("update quoted string")) {
					path = path + "&attribute_value=" + attribute_value;
					request.queryParam("attribute_value", attribute_value);
				} else if (requestType.equals("update dictionary")) {
					requestParams = new JSONObject();
					requestParams.put("key1", "value1");
					requestParams.put("key2", 2);
					requestParams.put("key3", true);
					param = requestParams.toString();
					request.header("Content-Type", prop.getProperty("contentType"));
					request.body(param);
				}
				response = request.post(ApiConstants.itemRecAdminApi + buuid + path);
			}
			utils.logit(path + " response: " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response irsMenuAdjusterGetApi(String requestType, String apiKey, String buuid, String attribute_name) {
		utils.longWaitInSeconds(2); // delay added to ensure API stability
		Response response = null;
		try {
			RestAssured.baseURI = prop.getProperty("irsAdminBaseUrl");
			RequestSpecification request = RestAssured.given().log().all();
			request.header("apikey", apiKey);
			String path = "";
			request.queryParam("attribute_name", attribute_name);
			if (requestType.equals("get value")) {
				path = "/settings/menu-adjuster?attribute_name=" + attribute_name;
				response = request.get(ApiConstants.itemRecAdminApi + buuid + path);
			} else if (requestType.equals("get value other")) {
				path = "/settings/" + buuid + "/menu-adjuster?attribute_name=" + attribute_name;
				response = request.get(path);
			}
			utils.logit(path + " response: " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response singleScanCodeWithRewardIDSecureApi(String client, String secret, String token, String id) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.singleScanrewardIDPayload(id);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.secureSingleScanCode + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());

			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.secureSingleScanCode);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response redemptionOfCardCompletionApi2(String client, String secret, String token, String locationId) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.redemptionOfCardCompletionApi2Payload(client, locationId);
			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2CardCompletionRedemption + body;
			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + token);
			request.body(body);
			response = request.post(ApiConstants.mobApi2CardCompletionRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;

	}

	// To send rewards to user with end date YYYY-MM-DD
	public Response sendMessageToUserWithEndDate(String userID, String authToken, String amount, String reedemable_id,
			String fuelAmount, String gift_count, String end_date) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Send message to user======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2SendMessageToUserWithEndDatePayload(userID, amount, reedemable_id, fuelAmount,
					gift_count, end_date);
			// Headers
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + authToken);
			request.body(body);
			response = request.post(ApiConstants.sendMessageToUser);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posBatchRedemptionWithQueryTrue(String locationKey, String userID, String item_id,
			String external_uid) {
		logger.info("Endpoints.processBatchRedemptionOfBasket()START ");
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getBatchRedemptionPayloadWithQueryTrueAuth(locationKey, userID, item_id,
					external_uid);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authListDiscountBasketAddedForPOSAPIWithExt_Uid(String locationKey, String userID,
			String discountType, String discountID, String externalUid) {
		Response response = null;
		String body = "";
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			if (discountType.equalsIgnoreCase("discount_amount")) {
				body = apipaylods.discountBasketItemsAttributesDiscountAmpountPOS(userID, discountType, discountID);
			} else {
				body = apipaylods.discountBasketItemsAttributesWithExt_Uid(userID, discountType, discountID,
						externalUid);
			}
			request.header("Authorization", "Token token=" + locationKey);
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.header("Accept-Language", "es");
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.discountBasketAddedPOS);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response singleScanCodeSecureApi(String client, String secret, String token, String payment_type_name,
			String payment_type_value, String payment_type_id) {
		Response response = null;
		String body = "";
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			if (payment_type_value.equalsIgnoreCase("")) {
				body = apipaylods.singleScanCodeWithoutPaymentTypeValueSecureApiPayload(payment_type_name);
			} else {
				body = apipaylods.singleScanCodeWithPaymentTypeValueSecureApiPayload(payment_type_name,
						payment_type_value, payment_type_id);
			}
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.secureSingleScanCode + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.secureSingleScanCode);
			logger.info("********************************");
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("*********************************");
			RestAssured.given().log().all();
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response onlineOrderWithOrderModeAndClientPlatformTest(String client, String secret, String externalUid,
			String authentication_token, String discount_type, String discount_typeName, String discount_typeValue,
			String order_mode, String client_platform, String amount, Map<String, Map<String, String>> parentMap) {
		Response response = null;
		String body = "";
		requestParams = new JSONObject();
		try {

			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			if (discount_typeName.equalsIgnoreCase("")) {
				body = apipaylods.onlineOrderWithOrderModeAndClientPlatformTestCardompletionPayload(
						authentication_token, discount_type, order_mode, client_platform, amount, client, externalUid,
						parentMap);
			} else {
				body = apipaylods.onlineOrderWithOrderModeAndClientPlatformTestPayload(authentication_token,
						discount_type, discount_typeName, discount_typeValue, order_mode, client_platform, amount,
						client, externalUid, parentMap);
			}
			String payload = ApiConstants.authOnlineRedemption + body;
			request.headers(header.onlineOrderCheckinHeader(payload.toString(), secret));
			request.body(body);
			response = request.post(ApiConstants.authOnlineRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2Meta(String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// String body = apipaylods.API1PurchaseGiftcard(amount);
			String URL = ApiConstants.mobApi2V2MetaAPI + "?client=" + client;
			String payload = URL;
			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			request.queryParam("client", client);
			// request.body(body);
			response = request.get(ApiConstants.mobApi2V2MetaAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2SubscriptionRedemption(String client, String secret, String subscriptionId, String authToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Send message to user======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = ApiPayloads.subscriptionPlanRedemptionCode(client, subscriptionId);
			String payload = ApiConstants.mobApi2SubscriptionRedemption + body;
			// Headers

			request.header("Authorization", "Bearer " + authToken);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			// request.queryParam("client", client);
			request.body(body);

			response = request.post(ApiConstants.mobApi2SubscriptionRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1MobileForgotPassword(String email, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API1Email(email);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobAPi1ForgotPassword + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobAPi1ForgotPassword);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1MobileChangePassword(String client, String secret, String resetPasswordToken,
			String currentPassword, String newPassword) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Body
			String body = new ObjectMapper().writeValueAsString(
					apipaylods.api1MobileChangePasswordPayLoad(resetPasswordToken, currentPassword, newPassword));

			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1ChangePassword + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());

			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.patch(ApiConstants.mobApi1ChangePassword);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authForgotPassword(String email, String client, String secret) {
		Response response = null;
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.authForgotPassword(email, client);
			String payload = ApiConstants.authForgotPassword + body;
			request.headers(header.onlineOrderCheckinHeader(payload.toString(), secret));
			request.body(body);
			response = request.post(ApiConstants.authForgotPassword);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response userLookupPosApiWithoutExt_uid(String lookUpField, String userEmail, String locationkey)
			throws InterruptedException {
		Thread.sleep(2000);
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.userLookupPosAPIWithouttExt_uidPayload(lookUpField, userEmail);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());
			request.header("Authorization", "Token token=" + locationkey);
			request.body(body);
			response = request.get(ApiConstants.posUserLookup);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2UserSubscription(String token, String client, String secret) {
		Response response = null;
		// requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// String body = apipaylods.api2PurchaseSubscriptionPayload(plan_id,
			// purchase_price);
			String payload = ApiConstants.mobApi2UserSubscriptionAPI + "?client=" + client;
			// Headers
			request.header("Authorization", "Bearer " + token);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			request.queryParam("client", client);
			response = request.get(ApiConstants.mobApi2UserSubscriptionAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response apiV1Redemption(String emailID, String subscriptionID, String punchhAppKey, String password) {
		Response response = null;
		requestParams = new JSONObject();
		String basicAuth = getBasicAuthenticationHeader(emailID, password);
		logger.info("basicAuth== " + basicAuth);
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			logger.info("====== v1 Redemption POST======");
			// Headers passing Signature and content type
			request.header("Content-Type", "application/json");
			request.header("Authorization", basicAuth);
			request.header("punchh-app-key", punchhAppKey);
			request.header("Accept", "*/*");
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			// Body
			String body = apipaylods.v1RedemptionPayload(emailID, subscriptionID);
			request.body(body);
			response = request.post(ApiConstants.v1Redemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;

	}

	public static final String getBasicAuthenticationHeader(String username, String password) {
		String valueToEncode = username + ":" + password;
		return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
	}

	public Response Api2UserSubscriptionWithCancellation(String token, String client, String secret) {
		Response response = null;
		// requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String payload = ApiConstants.mobApi2UserSubscriptionAPI + "?client=" + client + "&filter=cancelled";
			// Headers
			request.header("Authorization", "Bearer " + token);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			request.queryParam("client", client);
			request.queryParam("filter", "cancelled");
			response = request.get(ApiConstants.mobApi2UserSubscriptionAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response ISLReceiptDetailsAPI(String locationKey, String punchhKey) {
		Response response = null;
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = "https://isl.staging.punchh.io";
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.islReceiptDetialsPayload(punchhKey);
			// String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", "application/json");
			defaultHeaderMap.put("Authorization", "Token token=" + locationKey);
			request.headers(defaultHeaderMap);
			request.body(body);
			response = request.post(ApiConstants.islReceipt);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// For batch process AUTH API
	public Response voidProcessBatchRedemptionOfBasketAUTHAPI(String client, String secret, String authToken,
			String redemption_ref) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String URL = ApiConstants.batchRedemptionAuthAPI + "/" + redemption_ref + "?client=" + client;
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, URL));
			request.header("Authorization", "Bearer " + authToken);
			request.queryParam("client", client);
			response = request.delete(ApiConstants.batchRedemptionAuthAPI + "/" + redemption_ref);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response discountUnlockPOSAPI(String locationKey, String userID, String externalUID) {
		Response response = null;

		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.discountUnlockPOSApiPayload(userID, externalUID);
			request.header("Authorization", "Token token=" + locationKey);
			request.header("cache-control", "no-cache");
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.header("Accept-Language", "es");
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.put(ApiConstants.basketUnlockPOSAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response deleteDiscountBasketForUserWithExt_UidAUTH(String authToken, String client, String secret,
			String discount_basket_item_id, String external_uid) {
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.deleteDiscountBasketWithExt_UidPayload(discount_basket_item_id, external_uid);
			String URL = ApiConstants.deleteBasketForUserAUTH + "?client=" + client;
			String payload = URL + body;
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			request.header("Authorization", "Bearer " + authToken);
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.body(body);
			response = request.delete(ApiConstants.deleteBasketForUserAUTH);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authCardsAPI(String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String URL = ApiConstants.authfetchCard + "?client=" + client;
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, URL));
			request.queryParam("client", client);
			response = request.get(ApiConstants.authfetchCard);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authAutoSelectAPI(String client, String secret, String token, String subAmount, String external_uid,
			Map<String, Map<String, String>> parentMap) {
		Response response = null;
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.authAutoSelectPayload(subAmount, client, external_uid, parentMap);
			String payload = ApiConstants.authAutoSelect + body;
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			defaultHeaderMap.put("Accept", "application/json");
			defaultHeaderMap.put("Authorization", "Bearer " + token);
			// defaultHeaderMap.put("authentication_token", authentication_token);
			request.headers(defaultHeaderMap);
			request.body(body);
			response = request.post(ApiConstants.authAutoSelect);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posAutoSelectAPI(String user_id, String locationKey, String amount, String external_uid,
			Map<String, Map<String, String>> parentMap) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.posAutoSelectPayload(amount, external_uid, parentMap);
			request.header("Content-Type", "application/json");
			request.body(body);
			request.queryParam("user_id", user_id);
			request.queryParam("location_key", locationKey);
			response = request.post(ApiConstants.autoUnlockPosAPI);
			logger.info("POS Auto Select API response status code: " + response.statusCode());
			utils.logit("POS Auto Select API response: " + response.asPrettyString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// shashank
	public Response paymentAgreementTokenAPI(String client, String secret, String token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			param = requestParams.toString();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.paypalAgreementTokenAPI + "?client=" + client + "&hash=" + epoch;
			String payload = URL + param;
			request.headers(header.api1SignUpHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + token);
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.get(ApiConstants.paypalAgreementTokenAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// shashank
	public Response POSPayment(String lookupType, String emailID, String singleScanCode, String paymentType,
			String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		CreateDateTime.getCurrentDate();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			String body = apipaylods.getPSOPaymentPayload(lookupType, emailID, singleScanCode, paymentType,
					locationKey);
			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request1.header("Authorization", "Token token=" + locationKey);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request1);
			}
			request1.body(body);
			response = request1.post(ApiConstants.posPayment);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response voidProcessBatchRedemptionOfBasketPOSAPI(String client, String secret, String userID,
			String locationKey, String redemption_ref) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.queryParam("location_key", locationKey);
			request.queryParam("user_id", userID);
			response = request.delete(ApiConstants.batchRedemptionPOSAPI + "/" + redemption_ref);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posSignUpWithoutEmail(long phone, String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("*****POS Signup API*****");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("phone", phone);
			requestParams.put("password", Utilities.getApiConfigProperty("password"));
			requestParams.put("first_name",
					CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("firstName")));
			requestParams.put("last_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("lastName")));
			requestParams.put("zip_code", Utilities.getApiConfigProperty("zipCode"));
			request.queryParam("location_key", locationKey);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.posSignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posSignUpWithLoyaltyCardOnly(String cardNumber, String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("*****POS Signup API*****");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("card_number", cardNumber);
			requestParams.put("password", Utilities.getApiConfigProperty("password"));
			requestParams.put("first_name",
					CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("firstName")));
			requestParams.put("last_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("lastName")));
			requestParams.put("zip_code", Utilities.getApiConfigProperty("zipCode"));
			request.queryParam("location_key", locationKey);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.posSignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// Shashank
	public Response POSPaymentStatus(String paymentReferenceID, String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			param = requestParams.toString();
			apiUtils.getSpan();
			// request.headers(header.api1SignUpHeader(payload, epoch, secret));
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(requestParams.toJSONString());
			request.queryParam("payment_reference_id", paymentReferenceID);
			request.queryParam("location_key", locationKey);
			response = request.get(ApiConstants.posPaymentStatus);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response POSPaymentPUT(String paymentReferenceId, String locationKey, String status) {
		Response response = null;
		requestParams = new JSONObject();

		CreateDateTime.getCurrentDate();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			String body = apipaylods.putPOSPaymentPayload(paymentReferenceId, locationKey, status);
			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request1);
			}
			request1.body(body);
			response = request1.put(ApiConstants.posPayment);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// shashank
	public Response POSPaymentRefund(String paymentReferenceId, String locationKey) {
		Response response = null;
		requestParams = new JSONObject();

		CreateDateTime.getCurrentDate();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			String body = apipaylods.POSPaymentRefundPayload(paymentReferenceId, locationKey);
			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request1);
			}
			request1.body(body);
			response = request1.post(ApiConstants.posRefundPayment);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// shashank
	public Response POSPaymentCard(String client, String secret, String token, String singleScanCode) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.posPaymentCardPayload(singleScanCode);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.posPaymentCard + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());

			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.posPaymentCard);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1PurchaseGiftCardWithTransactionID(String client, String secret, String amount, String token,
			String transactionToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API1PurchaseGiftcardWithTransactionIDRecurring(amount, transactionToken);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1PurchaseGiftCard + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());

			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1PurchaseGiftCard);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1ReloadGiftCardRecurring(String email, String client, String secret, String amount, String token,
			String uid, String uuidPaymentCard) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API1PurchaseGiftcardWithTransactionIDRecurring(amount, uuidPaymentCard);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1ReloadGiftCard + uid + "/reload" + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());

			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1ReloadGiftCard + uid + "/reload");
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1PurchaseGiftCardWithSingleScanToken(String client, String secret, String amount, String token,
			String singleScanToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API1PurchaseGiftcardWithSingleScanCode(amount, singleScanToken);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1PurchaseGiftCard + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());

			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1PurchaseGiftCard);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response generateHeartlandPaymentToken(String authorizationKey, String api_key) {
		String body = apipaylods.getHeartlandPaymentTokenPayload();
		Response response = null;
		requestParams = new JSONObject();
		String baseURL = "https://cert.api2.heartlandportico.com/Hps.Exchange.PosGateway.Hpf.v1/api/token?";
		try {
			RestAssured.baseURI = baseURL;
			RequestSpecification request = RestAssured.given().log().all();
			// request.header("Authorization", "Basic "+ authorizationKey);
			request.header("Content-Type", "application/json");
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("api_key", api_key);
			request.body(body);
			response = request.post();
			logger.info(response.asPrettyString());
			logger.info("Response code: " + response.getStatusCode());
			return response;

		} catch (Exception e) {
			logger.info("Endpoints.generateHeartlandPaymentToken()");
		}
		return response;

	}

	public Response inboundSegmentEventGenerate(String proxyUrl, String hostURL, String apikey, String segName,
			String SegID, String action, String msgUUID) {
		String body = apipaylods.genInboundSegmentPayload(hostURL, apikey, segName, SegID, action, msgUUID);
		Response response = null;
		requestParams = new JSONObject();
		String baseURL = proxyUrl + ApiConstants.mParticeInboundSegment;
		try {
			RestAssured.baseURI = baseURL;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Content-Type", "application/json");
			request.header("Accept-Encoding", "gzip, br");
			request.body(body);
			response = request.post();
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;

	}

	public Response inboundSegmentUserEventGenerate(String proxyUrl, String hostURL, String apikey, String segName,
			String SegID, String action, String msgUUID, String email, boolean isHost, String hostVal, String mpid) {
		String body = apipaylods.genInboundSegmentUserPayload(hostURL, apikey, segName, SegID, action, msgUUID, email,
				mpid);
		Response response = null;
		requestParams = new JSONObject();
		String baseURL = proxyUrl + ApiConstants.mParticeInboundSegmentUser;
		try {
			RestAssured.baseURI = baseURL;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Content-Type", "application/json");
			request.header("Accept-Encoding", "gzip, br");
			request.body(body);
			response = request.post();
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;

	}

	public Response metaAPI2SubscriptionCancelReason(String client, String secret) {

		Response response = null;
		// requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// String body = apipaylods.api2PurchaseSubscriptionPayload(plan_id,
			// purchase_price);
			String payload = ApiConstants.mobApi2MetaV2ApiSubscriptionCancelReasons + "?client=" + client;
			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			request.queryParam("client", client);
			response = request.get(ApiConstants.mobApi2MetaV2ApiSubscriptionCancelReasons);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;

	}

	public Response authOnlineOrderCheckinWithQC(String authenticationToken, String amount, String client,
			String secret, String txn, String externalUid, String date, Map<String, Map<String, String>> parentMap) {
		Response response = null;
		requestParams = new JSONObject();
		try {

			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = ApiPayloads.authOnlineCheckinWithQCPayLoad(authenticationToken, amount, client, txn,
					externalUid, date, parentMap);
			String payload = ApiConstants.onlineOrderCheckin + body;
			request.headers(header.onlineOrderCheckinHeader(payload.toString(), secret));
			request.body(body);
			response = request.post(ApiConstants.onlineOrderCheckin);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;

	}

	public Response Api1MobileUpdateGuestEmailDetails(String email, String client, String secret, String token) {

		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API1UpdateGuestEmailDetails(email);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1UpdateGuestDetails + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;
			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept", api1Headers.accept());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.put(ApiConstants.mobApi1UpdateGuestDetails);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posVoidRedemptionAPI(String email, String discount_type_name, String discount_type_value,
			String locationKey, String txn) {
		Response response = null;
		String body;
		requestParams = new JSONObject();
		try {
			logger.info("*****POS Void Redemption API*****");
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			if (txn.equalsIgnoreCase("")) {
				body = apipaylods.posVoidRedemptionPayloadWithoutTxn(email, discount_type_name, discount_type_value);
			} else {
				body = apipaylods.posVoidRedemptionPayloadWithTxn(email, discount_type_name, discount_type_value, txn);
			}
			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request1.header("Authorization", "Token token=" + locationKey);
			request1.body(body);
			response = request1.delete(ApiConstants.posRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response userSubcriptionForApi2(String client, String secret, String token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			param = requestParams.toString();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi2UserSubscriptionAPI + "?client=" + client + "&hash=" + epoch;
			String payload = URL + param;

			request.headers(header.api1SignUpHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + token);
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.get(ApiConstants.mobApi2UserSubscriptionAPI);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response MobAPiCancelSubscription(String client, String secret, String token, String subcriptionID,
			String cancelId, String cancelResaon) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.ApiCancelSubcription(subcriptionID, cancelId, cancelResaon);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApiCancelSubscription + "?client=" + client + "&hash=" + epoch;
			// + "&hash=" + epoch
			String payload = URL + body;
			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept", api1Headers.accept());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.put(ApiConstants.mobApiCancelSubscription);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response MobAPi2CancelSubscription(String client, String secret, String token, String subcriptionID,
			String cancelId, String cancelResaon) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.ApiCancelSubcription(subcriptionID, cancelId, cancelResaon);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi2CancelSubscription + "?client=" + client + "&hash=" + epoch;
			// + "&hash=" + epoch
			String payload = URL + body;
			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept", api1Headers.accept());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.put(ApiConstants.mobApi2CancelSubscription);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response rollingToInactivityExpiryAPI2(String businessKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.rollingToInactivityExpiry();
			apiUtils.getSpan();
			request.header("Authorization", "Bearer " + businessKey);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept", api1Headers.accept());
			request.body(body);
			response = request.post(ApiConstants.rollingToInactiveExpiry);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response dashboardApiEClubUploadPrivacyAndTerms(String email, String token, String storeNumber) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.elubGuestPayloadWithEmailPrivacyAndTerms(email, storeNumber);
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept", api1Headers.accept());
			request.body(body);
			response = request.post(ApiConstants.eClubGuestUpload);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiSignUpPrivacyAndTerms(String email, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("email", email);
			hashmap.put("password", Utilities.getApiConfigProperty("password"));
			hashmap.put("first_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("firstName")));
			hashmap.put("last_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("lastName")));
			// hashmap.put("password", Utilities.getApiConfigProperty("password"));
			hashmap.put("birthday", Utilities.getApiConfigProperty("birthday"));
			hashmap.put("anniversary", Utilities.getApiConfigProperty("anniversary"));
			hashmap.put("terms_and_conditions", true);
			hashmap.put("privacy_policy", true);
			// hashmap.put("phone", Utilities.getRandomNo(1000) +
			// CreateDateTime.getTimeDateString());
			hashmap.put("signup_channel", Utilities.getApiConfigProperty("ssoSignupChannel"));
			requestParams.put("user", hashmap);
			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.authApiSignUp + param;
			// Headers passing
			request.headers(header.authAPISignUpHeader(payload, secret));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.authApiSignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response ABC(String businessKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.rollingToInactivityExpiry();
			apiUtils.getSpan();
			request.header("Authorization", "Bearer " + businessKey);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept", api1Headers.accept());
			request.body(body);
			response = request.post(ApiConstants.rollingToInactiveExpiry);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// shashank - Using in Integration TC to fetch single scan token details
	public Response posUserLookupFetchDetails(String singleScanToken, String location) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("*****POS User lookup Fetch Balance API*****");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.queryParam("single_scan_code", singleScanToken);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			requestParams.put("location_key", location);
			request.body(requestParams.toJSONString());
			response = request.get(ApiConstants.userLookupAndFetchBalance);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2SubscriptionPurchaseFutureDate(String token, String plan_id, String client, String secret,
			String purchase_price) {
		Response response = null;
		// requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.api2PurchaseSubscriptionPayloadWithFutureDate(plan_id, purchase_price);
			String payload = ApiConstants.mobApi2PurchaseSubscription + "?client=" + client + body;
			// Headers
			request.header("Authorization", "Bearer " + token);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.mobApi2PurchaseSubscription);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authSubscriptionCancel(String client, String secret, String accessToken, String subscriptionId,
			String cancellationFeedback, String cancellationReasonId, String cancellationType) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.authSubscriptionCancelPayload(client, subscriptionId, cancellationFeedback,
					cancellationReasonId, cancellationType);
			String URL = ApiConstants.authSubscriptionCancel;
			String payload = URL + body;
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			request.header("Accept", api1Headers.accept());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Authorization", "Bearer " + accessToken);
			request.header("Content-Type", api1Headers.contentType());
			request.header("x-pch-digest", signature);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.put(ApiConstants.authSubscriptionCancel);
			TestListeners.extentTest.get().info(response.getStatusCode() + " response: " + response.asString());
			logger.info(response.getStatusCode() + " response: " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2SubscriptionPurchaseWithOutDate(String token, String plan_id, String client, String secret,
			String purchase_price) {
		Response response = null;
		// requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.api2PurchaseSubscriptionPayloadWithoutDate(plan_id, purchase_price);
			String payload = ApiConstants.mobApi2PurchaseSubscription + "?client=" + client + body;
			// Headers
			request.header("Authorization", "Bearer " + token);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.mobApi2PurchaseSubscription);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authSubscriptionMeta(String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2ClientPayload(client);
			String URL = ApiConstants.authSubscriptionMeta;
			String payload = URL + body;
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			request.header("Accept", api1Headers.accept());
			request.header("x-pch-digest", signature);
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Content-Type", api1Headers.contentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.get(ApiConstants.authSubscriptionMeta);
			TestListeners.extentTest.get().info(response.getStatusCode() + " response: " + response.asString());
			logger.info(response.getStatusCode() + " response: " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posFetchAccountBalance(String userId, String discountTypeValue, String locationkey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			// String body = apipaylods.posFetchAccountBalancePayload(userId,
			// discountTypeValue);
			request1.header("Accept", "application/json");
			request1.queryParam("user_id", userId);
			request1.queryParam("discount_type", discountTypeValue);
			request1.header("Authorization", "Token token=" + locationkey);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request1);
			}
			// request1.body(body);
			response = request1.get(ApiConstants.posFetchAccountBalance);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2MobileDeleteVault(String client, String secret, String uuid, String token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi2DeletePaymentCard + "/" + uuid + "?client=" + client + "&hash=" + epoch;
			String payload = URL;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());
			request.header("User-Agent", api1Headers.userAgent());
			request.header("punchh-app-device-id", api1Headers.appDeviceId());
			request.header("app_build", api1Headers.appBuild());
			request.header("app_model", api1Headers.appModel());
			request.header("app_os", api1Headers.appOs());
			// request.header("app_version",)
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.delete(ApiConstants.mobApi2DeletePaymentCard + "/" + uuid);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2UserSubscriptionWithPastSubscription(String token, String client, String secret) {
		Response response = null;
		// requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String payload = ApiConstants.mobApi2UserSubscriptionAPI + "?client=" + client
					+ "&filter=past_subscriptions";
			// Headers
			request.header("Authorization", "Bearer " + token);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			request.queryParam("client", client);
			request.queryParam("filter", "past_subscriptions");
			response = request.get(ApiConstants.mobApi2UserSubscriptionAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response AuthUserSubscriptionWithPastSubscription(String token, String client, String secret,
			String filter) {
		Response response = null;
		// requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			;
			// Headers
			request.header("Authorization", "Bearer " + token);
			// request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			request.queryParam("client", client);
			request.queryParam("filter", filter);
			// request.body(body);
			response = request.get(ApiConstants.authSubscriptionUser);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response deletePaymentCard(String client, String secret, String token, String uuid, String passcode) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi2DeletePaymentCard + "/" + uuid + "?client=" + client + "&hash=" + epoch
					+ "&passcode=" + passcode;
			String payload = URL;
			// headers
			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());
			request.header("punchh-app-device-id", api1Headers.appDeviceId());
			request.header("User-Agent", api1Headers.userAgent());

			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.queryParam("passcode", passcode);
			response = request.delete(ApiConstants.mobApi2DeletePaymentCard + "/" + uuid);
			logger.info(response.statusCode());
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response WebHookAttentiveAPI(String client, String secret, String phoneNum, String fName) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.webHookAttentive(phoneNum, fName);
			apiUtils.getSpan();
			String URL = ApiConstants.webHookAttentive + "?client=" + client;
			String payload = URL + body;
			// Headers

			// Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
			// SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(),
			// "HmacSHA256");
			// sha256_HMAC.init(secret_key);
			// String signature =
			// Hex.encodeHexString(sha256_HMAC.doFinal(payload.getBytes()));

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("Content-Type", "application/json");
			request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.webHookAttentive);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response attentiveWebhookAPI(String client, String secret, String phoneNum, String email, String user_type,
			boolean smsSubscribe, boolean isTextToJoin, boolean isSmsAcquisition) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.genericAttentiveWebhookPayload(phoneNum, email, user_type, smsSubscribe,
					isTextToJoin, isSmsAcquisition);
			apiUtils.getSpan();
			String URL = ApiConstants.webHookAttentive + "?client=" + client;
			String payload = URL + body;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("Content-Type", "application/json");
			request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.webHookAttentive);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response smsWebhookAPI(String client, String secret, String smsAdapter, String buuid, String phoneNum,
			String email, String user_type, boolean smsSubscribe, boolean isTextToJoin, boolean isSmsAcquisition) {

		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.genericSmsWebhookPayload(phoneNum, email, user_type, smsSubscribe, isTextToJoin,
					isSmsAcquisition);
			apiUtils.getSpan();
			String webhookUrl = "/hooks/" + smsAdapter + "/" + buuid + "/conversations/status";
			String URL = webhookUrl + "?client=" + client;

			String payload = URL + body;
			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("Content-Type", "application/json");
			request.queryParam("client", client);
			request.body(body);
			response = request.post(webhookUrl);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response iFramePrefilledURL(String adminKey, String phonenum, String fName) {

		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.iframePrefilledURLPayload(phonenum, fName);
			apiUtils.getSpan();
			request.header("Content-Type", "application/json");
			request.header("Authorization", "Bearer " + adminKey);

			request.body(body);
			response = request.post(ApiConstants.iframePrifilledURL);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response dashboardAPI2createFeedback(String businessKey, String userID, String rating, String message) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.dashboardAPI2createFeedback(userID, rating, message);

			// Headers
			request.header("Content-Type", "application/json");
			request.header("Authorization", "Bearer " + businessKey);

			request.body(body);
			response = request.post(ApiConstants.dashboardAPI2createFeedback);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response dashboardAPI2updateFeedback(String businessKey, String userID, String feedbackID, String rating,
			String msg) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.dashboardAPI2updateFeedback(userID, rating, msg);

			// Headers
			request.header("Content-Type", "application/json");
			request.header("Authorization", "Bearer " + businessKey);

			request.body(body);
			response = request.patch(ApiConstants.dashboardAPI2createFeedback + "/" + feedbackID);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2FetchCheckinDashboard(String adminKey, String userSearchType, String userSearchValue,
			String startTime) {
		Response response = null;
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.api2DashboardCheckinPayload(userSearchType, userSearchValue, startTime);
			logger.info(body);
			Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
			// defaultHeaderMap.put("Accept-Timezone", "Etc/UTC");
			defaultHeaderMap.put("Content-Type", "application/json");
			defaultHeaderMap.put("Authorization", "Bearer " + adminKey);

			request.headers(defaultHeaderMap);
			request.body(body);
			response = request.post(ApiConstants.api2DashboardCheckinsURL);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response dashboardAPI2Meta(String apiKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Accept", api2Headers.api2Accept());
			request.header("Authorization", "Bearer " + apiKey);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			response = request.get(ApiConstants.dashboardAPI2Meta);
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiOrderingMeta(String filter, String client, String secret, String accessToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2ClientPayload(client);
			String URL = ApiConstants.authOrderingMeta + "?filter=" + filter;
			String payload = URL + body;
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			request.header("Accept", api2Headers.api2Accept());
			request.header("Authorization", "Bearer " + accessToken);
			request.header("x-pch-digest", signature);
			request.header("User-Agent", api1Headers.userAgent());
			request.header("Content-Type", api1Headers.contentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("filter", filter);
			request.body(body);
			response = request.get(ApiConstants.authOrderingMeta);
			TestListeners.extentTest.get().info(response.getStatusCode() + " response: " + response.asString());
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1GiftRewardToOtherUser(String client, String secret, String token, String recipientEmail,
			String rewardID) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.Api1GiftRewardToOtherUserPayload(recipientEmail, rewardID);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobAPi1RewardGiftedForType + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobAPi1RewardGiftedForType);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1UserSubscriptionWithPastSubscription(String token, String client, String secret) {
		Response response = null;
		// requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			String epoch = apiUtils.getSpan();
			RequestSpecification request = RestAssured.given().log().all();
			String payload = ApiConstants.userSubscriptionAPI1 + "?client=" + client + "&hash=" + epoch
					+ "&filter=past_subscriptions";
			// Headers
			request.header("Authorization", "Bearer " + token);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.queryParam("filter", "past_subscriptions");
			response = request.get(ApiConstants.userSubscriptionAPI1);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response APi1GamingAchievements(String client, String secret, String token, String kind, String level,
			String score, String gaming_level_id) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.APi1GamingAchievementsPayload(kind, level, score, gaming_level_id);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobAPi1GamingAchievements + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobAPi1GamingAchievements);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// Shashank
	public Response posRedemptionOfRewardWithoutUUID(String email, String redeemAmount, String locationKey,
			Map<String, Map<String, String>> parentMap, String rewardID) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("*****POS Redemption of Amount API*****");
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			String body = apipaylods.posCheckinWithQCPayloadWithoutUUID(email, redeemAmount, rewardID, redeemAmount,
					parentMap);
			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request1.header("Authorization", "Token token=" + locationKey);
			request1.body(body);
			response = request1.post(ApiConstants.posRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response ApiSubscriptionPurchase(String token, String plan_id, String client, String secret,
			String purchase_price, String endDateTime, String autoRenewal, String... taxValueLocationIdDiscountValue) {
		Response response = null;
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApiSubscriptionPurchase + "?client=" + client + "&hash=" + epoch;
			String body = apipaylods.apiPurchaseSubscriptionPayload(plan_id, purchase_price, endDateTime, autoRenewal,
					taxValueLocationIdDiscountValue);
			String payload = ApiConstants.mobApiSubscriptionPurchase + "?client=" + client + "&hash=" + epoch + body;
			// Headers
			request.header("Authorization", "Bearer " + token);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			request.body(body);
			response = request.post(URL);
			logger.info("Response code: " + response.getStatusCode());
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response getSFMCAccessToken(String et_subdomain, String et_clientId, String et_clientSecret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("*****SFMC get access token *****");
			RestAssured.baseURI = "https://" + et_subdomain + ".auth.marketingcloudapis.com";
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getSFMCAccessToken(et_clientId, et_clientSecret);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.body(body);
			response = request.post(ApiConstants.fetchSFMCAccessToken);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2SubscriptionPurchaseAutorenewal(String token, String plan_id, String client, String secret,
			String purchase_price, String endDateTime, String autoRenewal) {
		Response response = null;
		// requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.apiPurchaseSubscriptionPayload(plan_id, purchase_price, endDateTime, autoRenewal);
			String payload = ApiConstants.mobApi2PurchaseSubscription + "?client=" + client + body;
			// Headers
			request.header("Authorization", "Bearer " + token);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.mobApi2PurchaseSubscription);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response getSFMCSegmentExportDetails(String et_subdomain, String access_token, String segmentName) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("*****SFMC get segment export details *****");
			RestAssured.baseURI = "https://" + et_subdomain + ".rest.marketingcloudapis.com";
			RequestSpecification request = RestAssured.given().log().all();
			// String body = apipaylods.getSFMCAccessToken(et_clientId,et_clientSecret);
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			// request.header("Host", "");
			// request.body(body);
			response = request.get(ApiConstants.fetchSFMCFSegmentExportDetails + segmentName + "/rowset");
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response getSFMCFolderCategoryID(String et_subdomain, String access_token, String segmentName) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("*****Get SFMC Folder CategoryID *****");
			RestAssured.baseURI = "https://" + et_subdomain + ".soap.marketingcloudapis.com";
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getSFMCFolderCategoryID(et_subdomain, access_token, segmentName);
			request.header("Content-Type", "application/soap+xml; charset=UTF-8");
			request.header("Accept", "text/xml");
			request.body(body);
			response = request.post("Service.asmx");
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response deleteSFMCFolder(String et_subdomain, String access_token, String categoryID) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("*****Delete SFMC Folder *****");
			RestAssured.baseURI = "https://" + et_subdomain + ".soap.marketingcloudapis.com";
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.deleteSFMCFolder(et_subdomain, access_token, categoryID);
			request.header("Content-Type", "application/soap+xml; charset=UTF-8");
			request.header("Accept", "text/xml");
			request.body(body);
			response = request.post("Service.asmx");
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiSubscriptionPurchase(String token, String plan_id, String client, String secret,
			String purchase_price, String startDateTime, String endDateTime, String autoRenewal) {
		Response response = null;
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.authApiSubscriptionPurchase(plan_id, purchase_price, startDateTime, endDateTime,
					autoRenewal, client, token);
			String payload = ApiConstants.authSubscriptionPurchase + body;
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			request.header("x-pch-digest", signature);
			request.header("Content-Type", prop.getProperty("contentType"));
			request.header("Accept-Language", "en");
			request.header("Accept", prop.getProperty("contentType"));
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			// request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.authSubscriptionPurchase);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;

	}

	public Response dashboardSubscriptionPurchase(String token, String plan_id, String client, String secret,
			String purchase_price, String endDateTime, String autoRenewal, String userID,
			String... taxValueLocationIdDiscountValue) {
		Response response = null;
		// requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.authPurchaseSubscriptionPayload(plan_id, purchase_price, endDateTime, autoRenewal,
					userID, taxValueLocationIdDiscountValue);
			// Headers
			request.header("Authorization", "Bearer " + token);
			// request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			// request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			// request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.dashboardSubscriptionPurchase);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response apiV1UserOffers(String punchhAppKey, String emailID, String password) {
		Response response = null;
		requestParams = new JSONObject();
		String basicAuth = getBasicAuthenticationHeader(emailID, password);
		// logger.info("basicAuth== " + basicAuth);
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// Headers passing Signature and content type
			request.header("Content-Type", "application/json");
			request.header("Authorization", basicAuth);
			request.header("punchh-app-key", punchhAppKey);
			request.header("Accept", "*/*");
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			response = request.get(ApiConstants.v1UserOffer);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response webApi(String punchhAppKey, String emailID, String password, String slug, String rewardId) {
		Response response = null;
		requestParams = new JSONObject();
		String basicAuth = getBasicAuthenticationHeader(emailID, password);
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// Headers passing Signature and content type
			String body = apipaylods.webApiPayload(emailID, rewardId);
			request.header("Content-Type", "application/json");
			request.header("Authorization", basicAuth);
			request.body(body);
			response = request.get(ApiConstants.webApi + slug + "/users/offers");
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// User Present in segment (True/False)

	public Response createBusinessMigrationUse(String userEmail, String authorization, String initialPoints,
			String originalPoints) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String first_name = CreateDateTime.getUniqueString(prop.getProperty("firstName"));
			String last_name = CreateDateTime.getUniqueString(prop.getProperty("lastName"));
			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			String body = apipaylods.createBusinessMigrationUser(userEmail, first_name, last_name, initialPoints,
					originalPoints);
			request.body(body);
			response = request.post(ApiConstants.createBusinessMigrationUser);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response userInSegment(String userEmail, String authorization, String segmentId) {
		Response response = null;
		int maxAttempts = 10;
		int waitTimeSeconds = 2;
		int attempts = 0;

		while (attempts < maxAttempts) {
			utils.longWaitInSeconds(waitTimeSeconds);
			try {
				RestAssured.baseURI = baseUri;
				RequestSpecification request = RestAssured.given().log().all();
				// Headers passing Signature and content type
				request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
				request.header("Accept", Utilities.getApiConfigProperty("contentType"));
				request.header("Authorization", "Bearer " + authorization);
				response = request.get(ApiConstants.userInSegment + segmentId + "/in_segment?guest_email=" + userEmail);

				// If API is successful, check result
				if (response.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
					boolean result = response.jsonPath().getBoolean("result");
					if (result) {
						utils.logInfo("User found in segment on attempt: " + (attempts + 1));
						break;
					} else {
						utils.logInfo("User not yet in segment. Retrying... Attempt: " + (attempts + 1));
					}
				} else {
					utils.logWarn("Received non-200 status: " + response.getStatusCode());
				}

			} catch (Exception e) {
				utils.logFail("Exception while calling userInSegment API :: " + e);
			}
			attempts++;
		}
		if (response != null) {
			utils.logit("Final API Response: " + response.asString());
			utils.logit("Final Response Code: " + response.getStatusCode());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} else {
			utils.logFail("API response is null after polling");
		}

		return response;
	}

	public Response createBusinessMigrationUser(String userEmail, String authorization, String originalPoints,
			String initialPoints) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Create Business Migration User======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			String body = apipaylods.createBusinessMigrationUser(userEmail, originalPoints, initialPoints);
			request.body(body);
			response = request.post(ApiConstants.createBusinessMigrationUser);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response userBalanceApiV1(String emailID, String password, String punchhAppKey) {
		Response response = null;
		requestParams = new JSONObject();
		String basicAuth = getBasicAuthenticationHeader(emailID, password);
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", basicAuth);
			request.headers("punchh-app-key", punchhAppKey);
			response = request.get(ApiConstants.userBalanceApiV1);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response checkinsBalanceApiV1(String emailID, String password, String punchhAppKey) {
		Response response = null;
		requestParams = new JSONObject();
		String basicAuth = getBasicAuthenticationHeader(emailID, password);
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", basicAuth);
			request.headers("punchh-app-key", punchhAppKey);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			response = request.get(ApiConstants.checkinsBalanceApiV1);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1MobileCheckinsBalance(String token, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// requestParams.put("redeemed_points", redeemed_points);
			param = requestParams.toString();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobAPi1CheckinsBalance + "?client=" + client + "&hash=" + epoch;
			String payload = URL + param;

			request.headers(header.api1SignUpHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + token);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			// request.headers(headersMap);
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.get(ApiConstants.mobAPi1CheckinsBalance);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2CheckinAccountBalance(String client, String secret, String access_token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			requestParams.put("client", client);
			request.body(requestParams.toJSONString());
			param = requestParams.toString();
			String payload = ApiConstants.mobApi2CheckinAccountBalance + param;
			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + access_token);
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			response = request.get(ApiConstants.mobApi2CheckinAccountBalance);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiUserBalance(String authToken, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("client", client);
			requestParams.put("authentication_token", authToken);
			param = requestParams.toString();
			String payload = ApiConstants.authUserBalance + param;
			request.body(requestParams.toJSONString());
			// Headers passing
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			defaultHeaderMap.put("Accept", prop.getProperty("contentType"));
			defaultHeaderMap.put("Accept-Language", "en");
			request.headers(defaultHeaderMap);
			response = request.get(ApiConstants.authUserBalance);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processAuthBatchRedemptionUsingStoreNum(String client, String secret, String storeNum,
			String authToken, String userID, String item_id) {
		Response response = null;
		requestParams = new JSONObject();
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("client", client);
			requestParams.put("authentication_token", authToken);
			param = requestParams.toString();
			String payload = ApiConstants.authUserBalance + param;
			request.body(requestParams.toJSONString());
			// Headers passing
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			defaultHeaderMap.put("Accept", prop.getProperty("contentType"));
			defaultHeaderMap.put("Accept-Language", "en");
			request.headers(defaultHeaderMap);
			response = request.get(ApiConstants.authUserBalance);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1UserMembershipLevel(String client, String secret, String token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobAPi1UserMembershipLevel + "?client=" + client + "&hash=" + epoch;
			String payload = URL;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());

			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.get(ApiConstants.mobAPi1UserMembershipLevel);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response userMembershipLevelsApiV1(String emailID, String password, String punchhAppKey) {
		Response response = null;
		requestParams = new JSONObject();
		String basicAuth = getBasicAuthenticationHeader(emailID, password);
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", basicAuth);
			request.headers("punchh-app-key", punchhAppKey);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			response = request.get(ApiConstants.userMembershipLevelsApiV1);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response userMembershipLevelMobileApi(String client, String secret, String access_token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			requestParams.put("client", client);
			request.body(requestParams.toJSONString());
			param = requestParams.toString();
			String payload = ApiConstants.mobApi2UserMembershipLevelMobileApi + param;
			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + access_token);
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			response = request.get(ApiConstants.mobApi2UserMembershipLevelMobileApi);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2SubscriptionPurchaseWithStartTimeAutorenewal(String token, String plan_id, String client,
			String secret, String purchase_price, String endDateTime, String autoRenewal) {
		Response response = null;
		// requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.apiPurchaseSubscriptionWithStartTimePayload(plan_id, purchase_price, endDateTime,
					autoRenewal);
			String payload = ApiConstants.mobApi2PurchaseSubscription + "?client=" + client + body;
			// Headers
			request.header("Authorization", "Bearer " + token);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.mobApi2PurchaseSubscription);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response ApiListSubscription(String token, String client, String secret, String language) {
		utils.longWaitInSeconds(120);
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApiSubscriptionPurchase + "?client=" + client + "&hash=" + epoch;
			String payload = ApiConstants.mobApiSubscriptionPurchase + "?client=" + client + "&hash=" + epoch;
			// Headers
			request.header("Authorization", "Bearer " + token);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", language);
			request.header("Accept", api2Headers.api2Accept());
			response = request.get(URL);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1UserSubscriptionWithPastSubscriptionwithlanguage(String token, String client, String secret,
			String language) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.userSubscriptionAPI1 + "?client=" + client + "&hash=" + epoch;
			String payload = ApiConstants.userSubscriptionAPI1 + "?client=" + client + "&hash=" + epoch;
			// Headers
			request.header("Authorization", "Bearer " + token);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", language);
			request.header("Accept", api2Headers.api2Accept());
			response = request.get(URL);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Map<String, String> getDiscountDetailsMapExceptDiscountAmountFuel(String discount_type, String discount_id) {

		Map<String, String> discountMap = new HashMap<String, String>();
		discountMap.put("discount_type", discount_type);
		discountMap.put("discount_id", discount_id);

		return discountMap;
	}

	public Map<String, String> getDiscountDetailsMapForDiscountAmountFuel(String discount_type, String discount_value) {

		Map<String, String> discountMap = new HashMap<String, String>();
		discountMap.put("discount_type", discount_type);
		discountMap.put("discount_value", discount_value);

		return discountMap;
	}

	public Response mobileDiscountBasketforMultipleDiscountTypes(String authToken, String client, String secret,
			Map<String, Map<String, String>> parentMap) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String epoch = apiUtils.getSpan();
			String hash = apiUtils.getHash(secret + epoch);
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String URL = ApiConstants.discountBasketAdded + "?client=" + client + "&hash=" + epoch;
			String body = apipaylods.finalPayloadAfterAddingMultipleDiscounts(parentMap);
			String payload = URL + body;
			request.header("x-pch-digest", apiUtils.getSignature(secret, payload));
			request.header("Authorization", "Bearer " + authToken);
			request.header("Content-Type", "application/json");
			request.header("x-pch-hash", hash);
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.discountBasketAdded);
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1MobileUsersMembershipLevel(String token, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.mobApi2UserMemberShipLevel + param;
			// headers
			request.headers(header.api2LoginHeader(payload, secret));
			request.header("Authorization", "Bearer " + token);
			request.body(requestParams.toJSONString());
			response = request.get(ApiConstants.mobApi2FetchUserInfo);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response mobileAPI2ReactivationRequest(String client, String secret, String emailID) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = "{\"email\":\"" + emailID + "\"}";
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi2Reactivationrequest + "?client=" + client + "&hash=" + epoch;
			// + "&hash=" + epoch
			String payload = URL + body;
			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			// request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept", api1Headers.accept());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi2Reactivationrequest);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionOfBasketWithQueryParamPOSAPI(String locationKey, String userID,
			String subAmount, String query, Map<String, Map<String, String>> parentMap) {
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {

			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.generateReceiptWithQueryParam(locationKey, userID, subAmount, query, parentMap);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response fetchChallengeDetails(String client, String secret, String accessToken, String challengeID,
			String language) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.fetchChallengeDetails(client, accessToken);
			String URL = ApiConstants.mobApi2ListChallenges + "/" + challengeID;
			String payload = URL + body;
			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("Content-Type", "application/json");
			request.header("Accept-Language", language);
			request.header("Accept", api1Headers.accept());
			request.header("User-Agent", api1Headers.userAgent());
			request.body(body);
			response = request.get(ApiConstants.mobApi2ListChallenges + "/" + challengeID);
			TestListeners.extentTest.get().info(response.statusCode() + "; " + response.asString());
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response apiMobileChallenge(String client, String secret, String accessToken, String language) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			apiUtils.getHash(secret + epoch);
			// Headers passing Signature and content type
			String payload = ApiConstants.mobileApiChallenge + "?client=" + client + "&hash=" + epoch;
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			// request.pathParam("hash", epoch);
			request.header("Accept", api1Headers.accept());
			request.header("Accept-Language", language);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + accessToken);
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			response = request.get(ApiConstants.mobileApiChallenge);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response apiAuthChallenge(String client, String secret, String authenticationToken, String language) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String payload = ApiConstants.apiAuthChallenge + "?client=" + client + "&authentication_token="
					+ authenticationToken;
			request.header("Accept", api1Headers.accept());
			request.header("Accept-Language", language);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			response = request.get(ApiConstants.apiAuthChallenge + "?client=" + client + "&authentication_token="
					+ authenticationToken);
			response = request.get(ApiConstants.apiAuthChallenge);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1fetchChallengeDetails(String client, String secret, String accessToken, String challengeID,
			String language) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String epoch = apiUtils.getSpan();
			apiUtils.getHash(secret + epoch);

			// String body = apipaylods.fetchChallengeDetails(client,accessToken);
			String URL = ApiConstants.mobileApiChallenge + "/" + challengeID + "?client=" + client + "&hash=" + epoch;
			String payload = URL;// + body;
			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("Content-Type", "application/json");
			request.header("Accept", api1Headers.accept());
			request.header("Accept-Language", language);
			request.header("User-Agent", api1Headers.userAgent());
			request.header("Authorization", "Bearer " + accessToken);
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			// request.body(body);
			response = request
					.get(ApiConstants.mobileApiChallenge + "/" + challengeID + "?client=" + client + "&hash=" + epoch);
			TestListeners.extentTest.get().info(response.statusCode() + "; " + response.asString());
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response apiAuthChallengeDetails(String client, String secret, String authenticationToken,
			String challengeID, String language) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String payload = ApiConstants.apiAuthChallenge + "/" + challengeID + "?client=" + client
					+ "&authentication_token=" + authenticationToken;
			request.header("Accept", api1Headers.accept());
			request.header("Accept-Language", language);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			response = request.get(ApiConstants.apiAuthChallenge + "/" + challengeID + "?client=" + client
					+ "&authentication_token=" + authenticationToken);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response apiPosChallenge(String challengeID, String email, String locationKey, String language) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Accept-Language", language);
			response = request.get(ApiConstants.apiPosChallenge + "/" + challengeID + "?email=" + email
					+ "&location_key=" + locationKey);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response createBusinessMigrWithSingleCardDetails(String userEmail, String authorization, String cardumber,
			String epin) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Create Business Migration User======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			String body = apipaylods.createMigrationUserWithSingleCard(userEmail, cardumber, epin);
			request.body(body);
			response = request.post(ApiConstants.createBusinessMigrationUser);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response createBusinessMigrWithDoubleCardDetails(String userEmail, String authorization, String cardumber1,
			String epin1, String cardumber2, String epin2) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Create Business Migration User======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			String body = apipaylods.createMigrationUserWithDoubleCard(userEmail, cardumber1, epin1, cardumber2, epin2);
			request.body(body);
			response = request.post(ApiConstants.createBusinessMigrationUser);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2UserSubscriptionWithLanguage(String token, String client, String secret, String language) {
		Response response = null;
		// requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// String body = apipaylods.api2PurchaseSubscriptionPayload(plan_id,
			// purchase_price);
			String payload = ApiConstants.mobApi2UserSubscriptionAPI + "?client=" + client;
			// Headers
			request.header("Authorization", "Bearer " + token);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", language);
			request.header("Accept", api2Headers.api2Accept());
			request.queryParam("client", client);
			response = request.get(ApiConstants.mobApi2UserSubscriptionAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response addBulkUserToCustomSegment(String name, int customSegmentId, String csvPath, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			request.header("Authorization", "Bearer " + authorization);
			// request.header("contentType","multipart/form-data");

			request.multiPart("bulk_guest_activity_file", new File(csvPath), "multipart/form-data");
			request.multiPart("name", name);
			request.multiPart("custom_segment_id", customSegmentId);

			response = request.post(ApiConstants.bulkAdd);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response removeBulkUserfromCustomSegment(String name, int customSegmentId, String csvPath,
			String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			request.header("Authorization", "Bearer " + authorization);
			// request.header("contentType","multipart/form-data");
			request.multiPart("bulk_guest_activity_file", new File(csvPath), "multipart/form-data");
			request.multiPart("name", name);
			request.multiPart("custom_segment_id", customSegmentId);

			response = request.delete(ApiConstants.bulkRemove);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response searchUserExistsInCustomSegment(String segmentId3, String userEmail, String authorization) {
		utils.longWaitInSeconds(10);
		Response response = null;
		requestParams = new JSONObject();
		logger.info("Updating Custom segment");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.queryParam("email", userEmail);
			request.queryParam("custom_segment_id", segmentId3);
			// request.header("Accept", Utilities.getApiConfigProperty("contentType"));
			request.header("Accept", "application/json");
			request.header("Authorization", "Bearer " + authorization);
			response = request.get(ApiConstants.customSegmentMembers);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2FetchActivePurchasableSubscriptionPlans(String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// String body =
			// apipaylods.fetchActivePurchasableSubscriptionPlanPayload(client);
			String body = apipaylods.fetchActivePurchasableSubscriptionPlanPayload(client);
			String payload = ApiConstants.mobApi2PurchaseSubscription + body;
			// Headers
			request.headers(header.api2SignUpHeader(payload, secret));
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			// request.queryParam("client", client);
			request.body(body);
			response = request.get(ApiConstants.mobApi2PurchaseSubscription);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2markOffersAsRead(String token, String client, String secret, String rewards,
			String event_type) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// request.queryParam("client", client);
			String body = apipaylods.markoffersAsReadPayload(client, rewards, event_type);
			String payload = ApiConstants.mobApi2MarkOfferAsRead + body;
			request.headers(header.userOffersHeader(payload, secret, token));
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.put(ApiConstants.mobApi2MarkOfferAsRead);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response createMigrationUserWithGiftID(String userEmail, String authorization, String cardumber,
			String giftID) {

		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Create Business Migration User======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			String body = apipaylods.createMigrationUserWithGiftID(userEmail, cardumber, giftID);
			request.body(body);
			response = request.post(ApiConstants.createBusinessMigrationUser);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posVoidRedemptionPolling(String email, String redemption_id, String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		int attempts = 0;
		while (attempts < 15) {
			Utilities.longWait(1500);
			try {
				logger.info("*****POS Void Redemption API*****");
				RestAssured.baseURI = baseUri;
				RequestSpecification request1 = RestAssured.given().log().all();
				String body = apipaylods.posVoidRedemptionPayload(email, redemption_id);
				request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
				request1.header("Authorization", "Token token=" + locationKey);
				request1.body(body);
				response = request1.delete(ApiConstants.posRedemption);

			} catch (Exception e) {
				logger.error(e.getMessage());
			}
			if (response.getStatusCode() == 202) {
				break;
			}
			attempts++;
		}
		logger.info("API Response:" + response.asString());
		TestListeners.extentTest.get().info("API response is : " + response.asString());
		logger.info("Response code: " + response.getStatusCode());
		return response;
	}

	public Response posCheckinWithItemID(String date, String email, String key, String txn_no, String locationkey,
			String itemId1, String itemId2) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			String body = apipaylods.posCheckinPayloadWithItemId(date, email, key, txn_no, itemId1, itemId2);
			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request1.header("Authorization", "Token token=" + locationkey);
			request1.body(body);
			response = request1.post(ApiConstants.posCheckin);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posRedemptionOfRewardWithItemID(String email, String locationKey, String rewardId, String itemID) {
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			String body = apipaylods.posRedemptionofRewardPayloadWithItemID(email, date, key, txn, locationKey,
					rewardId, itemID);
			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			// request1.header("Authorization", locationKey);
			request1.body(body);
			response = request1.post(ApiConstants.posRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response bulkUploadOfBMUusers(String fileName, File csvFilePath, String apiKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			new HashMap<String, Object>();
			request.multiPart("name", fileName);
			request.multiPart("bulk_guest_activity_file", csvFilePath);
			request.header("Accept", "application/json");
			request.header("Authorization", "Bearer " + apiKey);
			response = request.post(ApiConstants.bulkUploadBmu);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response bulkDeleteLoayltyUsers(String fileName, File csvFilePath, String apiKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			new HashMap<String, Object>();
			request.multiPart("name", fileName);
			request.multiPart("bulk_guest_activity_file", csvFilePath);
			request.multiPart("reason", "");
			// request.header("Accept", "*/*");
			// request.header("Content-Type","multipart/form-data");

			request.header("Authorization", apiKey);
			response = request.delete(ApiConstants.bulkDeleteLoyatyUsersApi);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response bulkDeactivateLoayltyUsers(String fileName, File csvFilePath, String apiKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			new HashMap<String, Object>();
			request.multiPart("name", fileName);
			request.multiPart("bulk_guest_activity_file", csvFilePath);

			request.header("Authorization", apiKey);
			response = request.delete(ApiConstants.bulkDeactivateLayltyUsersAPi);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// add discount using POS API : shashank
	public Response authListDiscountBasketAddedForPOSAPIWithLanguage(String locationKey, String userID,
			String discountType, String discountID, String langName) {
		Response response = null;
		String body = "";
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			if (discountType.equalsIgnoreCase("discount_amount")) {
				body = apipaylods.discountBasketItemsAttributesDiscountAmpountPOS(userID, discountType, discountID);
			} else {
				body = apipaylods.discountBasketItemsAttributes(userID, discountType, discountID);
			}
			request.header("Authorization", "Token token=" + locationKey);
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.header("Accept-Language", langName);
			request.body(body);
			response = request.post(ApiConstants.discountBasketAddedPOS);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// for AUTH API: shashank
	public Response getUserDiscountBasketDetailsUsingAUTHWithLanguage(String authToken, String client, String secret,
			String languageName) {
		Response response = null;
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.getDiscountBasketAUTH + param;
			request.body(requestParams.toJSONString());
			// Headers passing
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			defaultHeaderMap.put("Accept", prop.getProperty("contentType"));
			defaultHeaderMap.put("Accept-Language", languageName);
			request.header("Authorization", "Bearer " + authToken);
			request.headers(defaultHeaderMap);
			response = request.get(ApiConstants.getDiscountBasketAUTH);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// for AUTH API: shashank
	public Response authListDiscountBasketAddedAUTHWithLanguage(String authToken, String client, String secret,
			String discountType, String discountID, String languageName) {
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.discountBasketItemsAttributes(discountType, discountID);
			String URL = ApiConstants.discountBasketAddedAUTH + "?client=" + client;
			String payload = URL + body;
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			request.header("Authorization", "Bearer " + authToken);
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.put("Accept-Language", languageName);

			request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.discountBasketAddedAUTH);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response POSDiscountLookupWithLanguage(String locationKey, String userID, String item_id,
			String languageName) {
		logger.info("Endpoints.processBatchRedemptionOfBasketPOS()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.posAPIDiscountLookUpPayload(locationKey, userID, item_id);
			request.header("Content-Type", "application/json");
			request.header("Accept-Language", languageName);
			request.body(body);
			response = request.post(ApiConstants.batchPOSDiscountAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posBatchRedemptionWithQueryTrueWithAcceptLanguage(String locationKey, String userID, String item_id,
			String external_uid, String langName) {
		logger.info("Endpoints.processBatchRedemptionOfBasket()START ");
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getBatchRedemptionPayloadWithQueryTrueAuth(locationKey, userID, item_id,
					external_uid);
			request.header("Content-Type", "application/json");
			request.header("Accept-Language", langName);
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionAUTHAPIWithAcceptLanguage(String client, String secret, String locationKey,
			String authToken, String userID, String item_id, String externalUid, String langName) {
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.getBatchRedemptionAuthApiPayload(locationKey, userID, item_id, externalUid);
			String URL = ApiConstants.batchRedemptionAuthAPI + "?client=" + client;
			String payload = URL + body;
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			request.header("Authorization", "Bearer " + authToken);
			request.header("cache-control", "no-cache");
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.header("Accept-Language", langName);
			request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionAuthAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response getUserDiscountBasketDetailsUsingAPI2WithAcceptLanguage(String authToken, String client,
			String secret, String langName) {
		Response response = null;
		requestParams = new JSONObject();

		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String URL = ApiConstants.mobApi2GetDiscountBasketDetailsAPI + "?client=" + client + "&access_token="
					+ authToken;
			String payload = URL;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.queryParam("access_token", authToken);
			request.header("Accept-Language", langName);
			response = request.get(ApiConstants.mobApi2GetDiscountBasketDetailsAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;

	}

	// API-Nwew - Delete
	public Response deleteDiscountBasketForUserAPIWithAcceptLanguage(String authToken, String client, String secret,
			String discount_basket_item_id, String langName) {
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			String epoch = apiUtils.getSpan();
			String hash = apiUtils.getHash(secret + epoch);
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.deleteDiscountBasketPayload(discount_basket_item_id);
			String URL = ApiConstants.deleteBasketForUserAPI + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;
			request.header("x-pch-digest", apiUtils.getSignature(secret, payload));
			request.header("Authorization", "Bearer " + authToken);
			request.header("Content-Type", "application/json");
			request.header("x-pch-hash", hash);
			request.header("Accept", "application/json");
			request.header("Accept-Language", langName);
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.delete(ApiConstants.deleteBasketForUserAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response identityUserSignUpWithEmail(String brandLevelToken, String client, boolean includePassword,
			String punchhAppDeviceId, String userAgent, String userEmailID, String brandUserToken) {
		Response response = null;
		try {
			RequestSpecification requestSpec = RestAssured.given().log().everything();
			// URL
			String identityURL = getGuestIdentityBaseUrl();
			requestSpec.baseUri(identityURL);
			String basePath = ApiConstants.identityUserSignUp;
			requestSpec.basePath(basePath);
			logger.info("Using basepath as ==> " + basePath);
			// Body
			String body = new ObjectMapper().writeValueAsString(apipaylods.identityUserSignUpWithEmailIdPayload(client,
					brandLevelToken, includePassword, userEmailID, brandUserToken));
			requestSpec.body(body);
			// Headers
			Map<String, Object> headers = new HashMap<String, Object>();
			headers.put("Content-Type", "application/json");
			headers.put("punchh-app-device-id", punchhAppDeviceId);
			headers.put("User-Agent", userAgent);
			requestSpec.headers(headers);
			// Response
			response = requestSpec.post();
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("error", e.getMessage());
		}
		return response;
	}

	public Response posSignUpWithPhone(String email, String locationKey, String phoneNumber) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("*****POS Signup API*****");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			putIfNotEmpty(requestParams, "email", email);
			putIfNotEmpty(requestParams, "phone", phoneNumber);
			requestParams.put("password", Utilities.getApiConfigProperty("password"));
			requestParams.put("first_name",
					CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("firstName")));
			requestParams.put("last_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("lastName")));
			requestParams.put("zip_code", Utilities.getApiConfigProperty("zipCode"));
			request.queryParam("location_key", locationKey);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.posSignUp);
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("error", e.getMessage());
		}
		return response;
	}

	public Response Api2RedemptionWithRewardIdAndLocationId(String client, String secret, String access_token,
			String reward_id, String locationId) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.Api2RedemptionWithRewardIdAndLocationId(client, reward_id, locationId);
			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2RedemptionsUsingRewardId + body;

			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + access_token);
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.body(body);
			response = request.post(ApiConstants.mobApi2RedemptionsUsingRewardId);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2RedemptionWitReedemableIdAndLocationId(String client, String secret, String access_token,
			String redeemableId, String locationId) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.Api2RedemptionWitReedemableIdAndLocationId(client, redeemableId, locationId);
			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2RedemptionsRedeemableId + body;
			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + access_token);
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.body(body);
			response = request.post(ApiConstants.mobApi2RedemptionsRedeemableId);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2SubscriptionRedemptionWithLocationId(String client, String secret, String subscriptionId,
			String authToken, String locationId) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Send message to user======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = ApiPayloads.subscriptionPlanRedemptionCodeWithLocationId(client, subscriptionId, locationId);
			String payload = ApiConstants.mobApi2SubscriptionRedemption + body;
			// Headers

			request.header("Authorization", "Bearer " + authToken);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			// request.queryParam("client", client);
			request.body(body);

			response = request.post(ApiConstants.mobApi2SubscriptionRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;

	}

	public Response authUserSubscriptionMeta(String locationKey, String phone) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.queryParam("phone", phone);
			param = requestParams.toString();
			request.header("Accept", "application/json");
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Content-Type", api1Headers.contentType());
			request.header("Authorization", "Token token=" + locationKey);
			response = request.get(ApiConstants.userLookupAndFetchBalance);
			TestListeners.extentTest.get().info(response.getStatusCode() + " response: " + response.asString());
			logger.info(response.getStatusCode() + " response: " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response createLISUsingApi(String adminKey, String lisName, String external_id, String filter_item_set,
			List<BaseItemClauses> baseItemClausesList, List<ModifiersItemsClauses> modifireItemClausesList,
			int modifire_max_discount_units, String modifire_processing_method, String lisPayloadVar) {
		String lisPayload = "";
		if (!lisPayloadVar.equalsIgnoreCase("")) {
			lisPayload = lisPayloadVar;
		} else {
			lisPayload = LineItemSelectorsJsonCreation.createLisJson(lisName, external_id, filter_item_set,
					baseItemClausesList, modifireItemClausesList, modifire_max_discount_units,
					modifire_processing_method);
		}
		Response response = null;
		RestAssured.baseURI = baseUri;
		RequestSpecification request = RestAssured.given().log().all();
		request.header("Content-Type", "application/json");
		request.header("Authorization", "Bearer " + adminKey);
		request.body(lisPayload.toString());
		response = request.post(ApiConstants.createLisAPI);
		logger.info("API Response:" + response.asString());
		TestListeners.extentTest.get().info(ApiConstants.createLisAPI + " api response is : " + response.asString());
		logger.info(response.statusCode());
		return response;
	}

	public Response getLisOfLineItemSelectorsUsingApi(String adminKey) {
		Response response = null;
		RestAssured.baseURI = baseUri;
		RequestSpecification request = RestAssured.given().log().all();
		request.header("Content-Type", "application/json");
		request.header("Authorization", "Bearer " + adminKey);
		response = request.get(ApiConstants.createLisAPI);
		logger.info("API Response:" + response.asString());
		TestListeners.extentTest.get().info(ApiConstants.createLisAPI + " api response is : " + response.asString());
		logger.info(response.statusCode());
		return response;
	}

	public Response apiV1UserSignup(String email, String punchhAppKey) {
		Response response = null;
		// requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.apiV1UserSignupPayload(email);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("punchh-app-key", punchhAppKey);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body.toString());
			response = request.post(ApiConstants.apiV1SserSignup);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response apiV1UserLogin(String email, String password, String punchhAppKey, String punchhAppDeviceid) {
		Response response = null;
		// requestParams = new JSONObject();
		// String basicAuth = getBasicAuthenticationHeader(email, password);
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.apiV1UserLoginPayload(email, password);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("punchh-app-device-id", punchhAppDeviceid);
			request.header("punchh-app-key", punchhAppKey);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body.toString());
			response = request.post(ApiConstants.apiV1UserLogin);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2SubscriptionPurchase(String token, String plan_id, String client, String secret,
			String purchase_price, String uuid, String subscriptionAdaptorType, String autoRenewal) {
		Response response = null;
		String body = "";
		// requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			switch (subscriptionAdaptorType) {
			case "Heartland":
				body = apipaylods.api2PurchaseSubscriptionPayloadWithPaymentCardWithoutStartEndDateTime(plan_id,
						purchase_price, uuid, autoRenewal);
				break;
			default:
				body = apipaylods.api2PurchaseSubscriptionPayloadWithPaymentCardUuid(plan_id, purchase_price, uuid);
				break;
			}
			String payload = ApiConstants.mobApi2PurchaseSubscription + "?client=" + client + body;
			// Headers
			request.header("Authorization", "Bearer " + token);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.mobApi2PurchaseSubscription);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response ApiSubscriptionPurchase(String token, String plan_id, String client, String secret,
			String purchase_price, String uuid, String subscriptionAdaptorType, String autoRenewal) {
		Response response = null;
		String body = "";
		String epoch = apiUtils.getSpan();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			switch (subscriptionAdaptorType) {
			case "Heartland":
				body = apipaylods.api2PurchaseSubscriptionPayloadWithPaymentCardWithoutStartEndDateTime(plan_id,
						purchase_price, uuid, autoRenewal);
				break;
			default:
				body = apipaylods.api2PurchaseSubscriptionPayloadWithPaymentCardUuid(plan_id, purchase_price, uuid);
				break;
			}
			String payload = ApiConstants.mobApiSubscriptionPurchase + "?client=" + client + "&hash=" + epoch + body;
			// Headers
			request.header("Authorization", "Bearer " + token);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			request.body(body);
			response = request.post(ApiConstants.mobApiSubscriptionPurchase + "?client=" + client + "&hash=" + epoch);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// Bulk Lis creation using API
	public Response bulkCreationLISUsingApi(String adminKey, Map<String, Map<String, String>> lisDataMap) {
		GenerateJson GenerateJsonObj = new GenerateJson();
		// Root object
		org.json.JSONObject root = new org.json.JSONObject();

		// Create "data" array
		org.json.JSONArray dataArray = new org.json.JSONArray();

		for (Map.Entry<String, Map<String, String>> outerEntry : lisDataMap.entrySet()) {
			String outerKey = outerEntry.getKey();
			// Map<String, String> innerMap = outerEntry.getValue();
			logger.info("Outer Key: " + outerKey);
			Map<String, String> innerMap1 = lisDataMap.get(outerKey);
			// Iterating over the inner map
			dataArray.put(GenerateJsonObj.createListItemWithModifiers(outerKey, innerMap1.get("externalId"),
					innerMap1.get("filterItemSet"), innerMap1.get("excludeNonPayable"), innerMap1.get("attribute"),
					innerMap1.get("operator"), innerMap1.get("value"), innerMap1.get("modifierAttributes"),
					innerMap1.get("modifierOperator"), innerMap1.get("modifierValue"),
					innerMap1.get("maxDiscountUnits"), innerMap1.get("processingMethod")));

		}
		root.put("data", dataArray);
		// Attach the data array to the root object

		// Print the generated JSON
		logger.info(root.toString(2)); // Indent with 2 spaces
		String lisPayload = root.toString(2);
		logger.info("lisPayload-- " + lisPayload);
		Response response = null;
		RestAssured.baseURI = baseUri;
		RequestSpecification request = RestAssured.given().log().all();
		request.header("Content-Type", "application/json");
		request.header("Authorization", "Bearer " + adminKey);
		request.body(lisPayload.toString());
		response = request.post(ApiConstants.createLisAPI);
		logger.info("API Response:" + response.asString());
		TestListeners.extentTest.get().info(ApiConstants.createLisAPI + " api response is : " + response.asString());
		logger.info(response.statusCode());
		return response;
	}

	public Response apiV1UserBalance(String emailID, String password, String punchhAppKey) {
		Response response = null;
		// requestParams = new JSONObject();
		String basicAuth = getBasicAuthenticationHeader(emailID, password);
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// String body = apipaylods.apiV1UserSignupPayload(email);
			request.header("Authorization", basicAuth);
			request.header("punchh-app-key", punchhAppKey);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			// request.body(body.toString());
			response = request.get(ApiConstants.apiV1Userbalance);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response apiV1GetRichMessages(String emailID, String password, String punchhAppKey) {
		Response response = null;
		// requestParams = new JSONObject();
		String basicAuth = getBasicAuthenticationHeader(emailID, password);
		try {
			logger.info("======API V1 GetRichMessages======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Authorization", basicAuth);
			request.header("punchh-app-key", punchhAppKey);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			response = request.get(ApiConstants.apiV1GetRichMessages);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response redeemableListAPi(String authorization, String queryParameter, String pageNo,
			String redeemablePerPage) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("====== List Redemable Name ======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			// request.header("Accept", api2Headers.api2Accept());
			request.header("Content-Type", api2Headers.api2ContentType());
			// String URL = ApiConstants.createLocation +"?location_id="+locationId;
			String body = apipaylods.redeemableListAPiPayload(queryParameter, pageNo, redeemablePerPage);
			request.body(body);
			response = request.get(ApiConstants.redeemableListAPi);
			logger.info("API Response:" + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response getLisOfLineItemSelectorsUsingApiWithPagePerItem(String adminKey, String queryKey,
			String lisFilterQuery, String pageNo, String perPage) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("====== List Redemable Name ======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + adminKey);
			request.header("Content-Type", api2Headers.api2ContentType());
			String body = apipaylods.getLisDataPerPage(queryKey, lisFilterQuery, pageNo, perPage);
			request.body(body);
			response = request.get(ApiConstants.createLisAPI);
			logger.info("API Response:" + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response generateHeartlandPaymentTokenPolling(String authorizationKey, String api_key) {
		String body = apipaylods.getHeartlandPaymentTokenPayload();
		Response response = null;
		requestParams = new JSONObject();
		String baseURL = "https://cert.api2.heartlandportico.com/Hps.Exchange.PosGateway.Hpf.v1/api/token?";
		int attempts = 0;
		while (attempts < 5) {
			utils.longWaitInSeconds(2);
			RestAssured.baseURI = baseURL;
			RequestSpecification request = RestAssured.given().log().all();
			// request.header("Authorization", "Basic "+ authorizationKey);
			request.header("Content-Type", "application/json");
			request.queryParam("api_key", api_key);
			request.body(body);
			response = request.post();
			String token = response.jsonPath().getString("token_value");
			if (!token.contains("io.restassured")) {
				break;
			}
			logger.info("Attemp number for generating Heartland token " + attempts);
			TestListeners.extentTest.get().info("Attemp number for generating Heartland token " + attempts);
			attempts++;
		}
		logger.info("API Response:" + response.asString());
		logger.info("Response code: " + response.getStatusCode());
		return response;
	}

	public Response updateLISUsingAPI(String adminKey, String lisName, String external_id, String filter_item_set,
			List<BaseItemClauses> baseItemClausesList, List<ModifiersItemsClauses> modifireItemClausesList,
			int modifire_max_discount_units, String modifire_processing_method) {
		String lisPayload = LineItemSelectorsJsonCreation.createLisJson(lisName, external_id, filter_item_set,
				baseItemClausesList, modifireItemClausesList, modifire_max_discount_units, modifire_processing_method);
		Response response = null;
		RestAssured.baseURI = baseUri;
		RequestSpecification request = RestAssured.given().log().all();
		request.header("Content-Type", "application/json");
		request.header("Authorization", "Bearer " + adminKey);
		request.body(lisPayload.toString());
		response = request.patch(ApiConstants.createLisAPI);
		logger.info("API Response:" + response.asString());
		TestListeners.extentTest.get().info(ApiConstants.createLisAPI + " api response is : " + response.asString());
		logger.info(response.statusCode());
		return response;
	}

	public Response getLisOfRedeemableUsingApiWithPagePerItem(String adminKey, String queryKey, String lisFilterQuery,
			String pageNo, String perPage) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("====== List Redemable Name ======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + adminKey);
			request.header("Content-Type", api2Headers.api2ContentType());
			String body = apipaylods.getLisDataPerPage(queryKey, lisFilterQuery, pageNo, perPage);
			request.body(body);
			response = request.get(ApiConstants.getRedeemableListAPI);
			logger.info("API Response:" + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response createQualificationCriteriaUsingApi(String adminKey, String qcName, String qcExternalID,
			String line_item_selector_id, String qc_processing_function, String percentage_of_processed_amount) {
		String lisPayload = apipaylods.getQualificationCriteraPayload(qcName, qcExternalID, line_item_selector_id,
				qc_processing_function, percentage_of_processed_amount);
		Response response = null;
		RestAssured.baseURI = baseUri;
		RequestSpecification request = RestAssured.given().log().all();
		request.header("Content-Type", "application/json");
		request.header("Authorization", "Bearer " + adminKey);
		request.body(lisPayload.toString());
		response = request.post(ApiConstants.createQualificationCriteria_API);
		logger.info("API Response:" + response.asString());
		TestListeners.extentTest.get()
				.info(ApiConstants.createQualificationCriteria_API + " api response is : " + response.asString());
		logger.info(response.statusCode());
		return response;
	}

	public Response updateQualificationCriteriaUsingApi(String adminKey, String qcName, String qcExternalID,
			String line_item_selector_id, String qc_processing_function, String percentage_of_processed_amount) {
		String lisPayload = apipaylods.getQualificationCriteraPayload(qcName, qcExternalID, line_item_selector_id,
				qc_processing_function, percentage_of_processed_amount);
		Response response = null;
		RestAssured.baseURI = baseUri;
		RequestSpecification request = RestAssured.given().log().all();
		request.header("Content-Type", "application/json");
		request.header("Authorization", "Bearer " + adminKey);
		request.body(lisPayload.toString());
		response = request.patch(ApiConstants.createQualificationCriteria_API);
		logger.info("API Response:" + response.asString());
		TestListeners.extentTest.get()
				.info(ApiConstants.createQualificationCriteria_API + " api response is : " + response.asString());
		logger.info(response.statusCode());
		return response;

	}

	public Response forceRedeemption(String authorization, String userId, String forceRedemptioTypeVar,
			String requestedValueVar, String requestedValue, String choice) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Process Redemption Code ======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = "";
			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			switch (choice) {
			case "fuel":
				body = apipaylods.forceFuelRedeemptionPayload(userId, forceRedemptioTypeVar, requestedValueVar,
						requestedValue);
				break;
			case "reward":
				body = apipaylods.forceRedeemptionPayload(userId, forceRedemptioTypeVar, requestedValueVar,
						requestedValue);
				break;

			case "points":
				body = apipaylods.forceRedeemptionPayload(userId, forceRedemptioTypeVar, requestedValueVar,
						requestedValue);
				break;
			}

			request.body(body);
			response = request.post(ApiConstants.forceRedeem);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// Bulk Lis Updation using API
	public Response bulkUpdationLISUsingApi(String adminKey, Map<String, Map<String, String>> lisDataMap) {
		GenerateJson GenerateJsonObj = new GenerateJson();
		// Root object
		org.json.JSONObject root = new org.json.JSONObject();

		// Create "data" array
		org.json.JSONArray dataArray = new org.json.JSONArray();

		for (Map.Entry<String, Map<String, String>> outerEntry : lisDataMap.entrySet()) {
			String outerKey = outerEntry.getKey();
			// Map<String, String> innerMap = outerEntry.getValue();

			logger.info("Outer Key: " + outerKey);

			Map<String, String> innerMap1 = lisDataMap.get(outerKey);
			// Iterating over the inner map
			dataArray.put(GenerateJsonObj.createListItemWithModifiers(outerKey, innerMap1.get("externalId"),
					innerMap1.get("filterItemSet"), innerMap1.get("excludeNonPayable"), innerMap1.get("attribute"),
					innerMap1.get("operator"), innerMap1.get("value"), innerMap1.get("modifierAttributes"),
					innerMap1.get("modifierOperator"), innerMap1.get("modifierValue"),
					innerMap1.get("maxDiscountUnits"), innerMap1.get("processingMethod")));

		}
		root.put("data", dataArray);
		// Attach the data array to the root object

		// Print the generated JSON
		logger.info(root.toString(2)); // Indent with 2 spaces

		String lisPayload = root.toString(2);

		logger.info("lisPayload-- " + lisPayload);
		Response response = null;
		RestAssured.baseURI = baseUri;
		RequestSpecification request = RestAssured.given().log().all();
		request.header("Content-Type", "application/json");
		request.header("Authorization", "Bearer " + adminKey);
		request.body(lisPayload.toString());
		response = request.patch(ApiConstants.createLisAPI);
		logger.info("API Response:" + response.asString());
		TestListeners.extentTest.get().info(ApiConstants.createLisAPI + " api response is : " + response.asString());
		logger.info(response.statusCode());
		return response;
	}

	// create redeemable // incomplete
	public Response createRedeemableUsingAPI(String adminKey, Map<String, String> detailsMap) {
		String redeemablePayload = ApiPayloads.getRedeemablePayload(detailsMap);
		Response response = null;
		RestAssured.baseURI = baseUri;
		RequestSpecification request = RestAssured.given().log().all();
		request.header("Content-Type", "application/json");
		request.header("Authorization", "Bearer " + adminKey);
		request.body(redeemablePayload.toString());
		response = request.post(ApiConstants.getRedeemableListAPI);
		logger.info("API Response:" + response.asString());
		TestListeners.extentTest.get()
				.info(ApiConstants.getRedeemableListAPI + " api response is : " + response.asString());
		logger.info(response.statusCode());
		return response;
	}

	// posAutoSelectAPI api/pos/auto_select";
	public Response posAutoSelectAPI(String userID, String Amount, String qty, String itemId, String externalUid,
			String locationkey, String langName) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.autoSelectPOSAPiPayload(userID, Amount, qty, itemId, externalUid);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", langName);
			request.header("Accept", api1Headers.accept());
			request.body(body);
			request.queryParam("location_key", locationkey);
			response = request.post(ApiConstants.autoUnlockPosAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApplicableOffersNew(String client, String secret, String itemID1, String itemID2,
			String authToken, String channel) {
		Response response = null;
		requestParams = new JSONObject();
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getAuthApplicableOfferPayload(client, itemID1, itemID2, authToken, channel);
			// request.body(body);
			String URL = ApiConstants.authRedemptionsApplicableOffers + "?client=" + client + "&authentication_token="
					+ authToken;
			String payload = URL + body;
			request.queryParam("client", client);
			request.queryParam("authentication_token", authToken);
			request.body(body);
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			defaultHeaderMap.put("Accept", prop.getProperty("contentType"));
			request.headers(defaultHeaderMap);
			response = request.get(ApiConstants.authRedemptionsApplicableOffers);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response createBusinessMigrationUserCookies(String userEmail, String authorization, String cookiess) {
		Response response = null;
		requestParams = new JSONObject();
		String[] parts = cookiess.split("Path", 2);
		String beforePath = parts[0].replace(";", ""); // .replace("a", "b");
		logger.info(beforePath);
		try {
			logger.info("======PLATFORM FUNCTIONS API Create Business Migration User======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Cookie", beforePath);
			String body = apipaylods.createBusinessMigrationUser(userEmail);
			request.body(body);
			response = request.post(ApiConstants.createBusinessMigrationUser);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response pointForceRedeemWithType(String authorization, String userId, String points, String type) {

		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Process Redemption Code ======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			String body = apipaylods.pointForceRedeemWithType(userId, points, type);
			request.body(body);
			response = request.post(ApiConstants.forceRedeem);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response checkinsApiV1(String emailID, String password, String punchhAppKey) {
		Response response = null;
		requestParams = new JSONObject();
		String basicAuth = getBasicAuthenticationHeader(emailID, password);
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// Headers passing Signature and content type
			request.headers("Authorization", basicAuth);
			request.headers("punchh-app-key", punchhAppKey);
			request.headers("Content-Type", "application/json");
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			response = request.get(ApiConstants.checkinsApiV1);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response mobAPi1FetchCheckin(String token, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			param = requestParams.toString();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobAPi1FetchCheckin + "?client=" + client + "&hash=" + epoch;
			String payload = URL + param;
			request.headers(header.api1SignUpHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + token);
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.get(ApiConstants.mobAPi1FetchCheckin);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// used and checked
	public Response createRedeemablesUsingAPI(String adminKey, Map<String, String> detailsMap) {
		String redeemablePayload = ApiPayloads.getRedeemablePayloads(detailsMap);
		Response response = null;
		RestAssured.baseURI = baseUri;
		RequestSpecification request = RestAssured.given().log().all();
		request.header("Content-Type", "application/json");
		request.header("Authorization", "Bearer " + adminKey);
		request.body(redeemablePayload.toString());
		response = request.post(ApiConstants.getRedeemableListAPI);
		logger.info("API Response:" + response.asString());
		TestListeners.extentTest.get()
				.info(ApiConstants.getRedeemableListAPI + " api response is : " + response.asString());
		logger.info(response.statusCode());
		return response;
	}

	// create redeemable // incomplete
	public Response updateRedeemableUsingAPI(String adminKey, Map<String, String> detailsMap) {
		String redeemablePayload = ApiPayloads.getRedeemablePayload(detailsMap);
		Response response = null;
		RestAssured.baseURI = baseUri;
		RequestSpecification request = RestAssured.given().log().all();
		request.header("Content-Type", "application/json");
		request.header("Authorization", "Bearer " + adminKey);
		request.body(redeemablePayload.toString());
		response = request.patch(ApiConstants.getRedeemableListAPI);
		logger.info("API Response:" + response.asString());
		TestListeners.extentTest.get()
				.info(ApiConstants.getRedeemableListAPI + " api response is : " + response.asString());
		logger.info(response.statusCode());
		return response;
	}

	public Response getQualificationListUsingAPI(String authorization, String queryParameter, String pageNo,
			String redeemablePerPage) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("====== List QC Name ======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			// request.header("Accept", api2Headers.api2Accept());
			request.header("Content-Type", api2Headers.api2ContentType());
			// String URL = ApiConstants.createLocation +"?location_id="+locationId;
			// getting payload for qualification criteria using same payload as get
			// redeemable
			String body = apipaylods.redeemableListAPiPayload(queryParameter, pageNo, redeemablePerPage);
			request.body(body);
			response = request.get(ApiConstants.createQualificationCriteria_API);
			logger.info("API Response:" + response.asString());
			logger.info("Response code: " + response.getStatusCode());
			TestListeners.extentTest.get().info("API response is: " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response uploadRedeemableImage(String adminKey, String external_id, String image_url) {
		// Create the JSONObject for the "data" array
		JSONObject dataObject = new JSONObject();
		dataObject.put("external_id", external_id);
		dataObject.put("image_url", image_url);

		// Create the JSONArray and add the dataObject to it
		JSONArray dataArray = new JSONArray();
		dataArray.add(dataObject);

		// Create the final JSON object and add the dataArray to it
		JSONObject payload = new JSONObject();
		payload.put("data", dataArray);
		Response response = null;
		requestParams = new JSONObject();
		RestAssured.baseURI = baseUri;
		logger.info("Upload Redeemable Image API");
		RequestSpecification request = RestAssured.given().log().all();
		request.header("Content-Type", "application/json");
		request.header("Authorization", "Bearer " + adminKey);
		request.body(payload.toString());
		response = request.post(ApiConstants.uploadRedeemableImage);
		logger.info("API Response:" + response.asString());
		TestListeners.extentTest.get()
				.info(ApiConstants.uploadRedeemableImage + " api response is : " + response.asString());
		logger.info(response.statusCode());
		return response;
	}

	public Response createQCUsingApi(String adminKey, String qcName, String qcExternalID, String line_item_selector_id,
			String line_item_selector_id2, String qc_processing_function, String percentage_of_processed_amount,
			String locationId, Map<String, String> detailsMap) {
		String lisPayload = apipaylods.getQCPayload(qcName, qcExternalID, line_item_selector_id, line_item_selector_id2,
				qc_processing_function, percentage_of_processed_amount, locationId, detailsMap);
		Response response = null;
		RestAssured.baseURI = baseUri;
		RequestSpecification request = RestAssured.given().log().all();
		request.header("Content-Type", "application/json");
		request.header("Authorization", "Bearer " + adminKey);
		request.body(lisPayload.toString());
		response = request.post(ApiConstants.createQualificationCriteria_API);
		logger.info("API Response:" + response.asString());
		TestListeners.extentTest.get()
				.info(ApiConstants.createQualificationCriteria_API + " api response is : " + response.asString());
		logger.info(response.statusCode());
		return response;
	}

	public Response offerApi2UpdateRedeemableEndTime(String externalId, String endTime, String apiKey) {

		Response response = null;

		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2UpdateRedeemableEndTime(externalId, endTime);

			// String payload = ApiConstants.api2UpdateRedeemable + body;
			// Headers
			request.header("Authorization", "Bearer " + apiKey);
			request.header("Content-Type", api2Headers.api2ContentType());

			request.body(body);
			response = request.patch(ApiConstants.api2UpdateRedeemable);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// this functions is using in android app login test cases
	public Response Api1UserLoginForAndroidApp(String email, String client, String secret, String pwd) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("email", email);
			hashmap.put("password", pwd);
			requestParams.put("user", hashmap);
			param = requestParams.toString();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1Login + "?client=" + client + "&hash=" + epoch;
			String payload = URL + param;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.post(ApiConstants.mobApi1Login);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// incomplete method
	public Response Api2SignUpwithAnniversaryAndroid(String client, String secret, String email, String password,
			String phoneNumber, String birthday) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile Api2 SignUp======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			// String phoneNumber =
			// CreateDateTime.getUniqueString(prop.getProperty("countryCode"));
			hashmap.put("email", email);
			hashmap.put("password", password);
			hashmap.put("phone", phoneNumber);
			hashmap.put("first_name", CreateDateTime.getUniqueString(prop.getProperty("firstName")));
			hashmap.put("last_name", CreateDateTime.getUniqueString(prop.getProperty("lastName")));
			// hashmap.put("zip_code", prop.getProperty("zipCode"));
			// hashmap.put("marketing_email_subscription", "true");
			hashmap.put("signup_channel", prop.getProperty("signUpChannel"));
			// hashmap.put("privacy_policy", prop.getProperty("privatePolicy"));
			hashmap.put("birthday", birthday);
			// hashmap.put("gender", "Male");
			// hashmap.put("anniversary", anniversary);
			// logger.info("Email in method for API2 ==> " + hashmap);
			requestParams.put("user", hashmap);
			requestParams.put("client", client);
			param = requestParams.toString();
			// logger.info("Email in method for API2 ==> " + param);
			String payload = ApiConstants.mobApi2SignUp + param;
			request.headers(header.api2SignUpHeader(payload, secret));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.mobApi2SignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response fetchConfig(String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======PPCC Fetch configuration api======");
		try {
			String ppccServiceBaseUrl = Utilities.getApiConfigProperty(env.toLowerCase() + ".ppccServiceBaseUrl");
			logger.info("PPCC Service base Url is ==> " + ppccServiceBaseUrl);
			RestAssured.baseURI = ppccServiceBaseUrl;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Token token=" + locationKey + ",btoken=");
			response = request.get(ApiConstants.fetchConfig);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response heartbeatApi(String locationKey, String packageVersion, String packageVersionId,
			String lastUpdatedAt) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======PPCC heartbeat configuration api======");
		try {
			String ppccServiceBaseUrl = Utilities.getApiConfigProperty(env.toLowerCase() + ".ppccServiceBaseUrl");
			logger.info("PPCC Service base Url is ==> " + ppccServiceBaseUrl);
			RestAssured.baseURI = ppccServiceBaseUrl;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.heartbeatApi(packageVersion, packageVersionId, lastUpdatedAt);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Token token=" + locationKey + ",btoken=");
			request.body(body.toString());
			response = request.post(ApiConstants.heartbeatApi);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response getConfigurations(String accessToken, String queryParamter) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======PPCC get configurations api======");
		try {
			String ppccServiceBaseUrl = Utilities.getApiConfigProperty(env.toLowerCase() + ".ppccServiceBaseUrl");
			logger.info("PPCC Service base Url is ==> " + ppccServiceBaseUrl);
			RestAssured.baseURI = ppccServiceBaseUrl;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", accessToken);
			response = request.get(ApiConstants.getConfigurations + queryParamter);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public String getAuthTokenForPPCC(int businessId, String slug, String businessName) {
		try {
			String token;
			Instant now = Instant.now();
			Instant expiry = now.plus(30, ChronoUnit.MINUTES);

			Map<String, Object> claims = new HashMap<>();
			claims.put("aud", "client_id");
			claims.put("iss", "https://dashboard.staging.punchh.io");
			claims.put("sub", "uuid-of-user");
			claims.put("admin_id", "65");
			claims.put("business_id", businessId);
			claims.put("business_uuid", "33c2a9ea-f5f8-4e66-9bfd-b07a24bb5153");
			claims.put("tenant", "mothership");
			claims.put("exp", expiry.getEpochSecond());
			claims.put("email", "testAutomation@ppcc.com");
			claims.put("business_name", businessName);
			claims.put("slug", slug);
			claims.put("role", "superAdmin");
			claims.put("permission", "manage");
			claims.put("sua", true);
			claims.put("subscription_type", "premium");

			String jwt = Jwts.builder().setClaims(claims)
					.signWith(Keys.hmacShaKeyFor(prop.getProperty("secret").getBytes(StandardCharsets.UTF_8)),
							SignatureAlgorithm.HS256)
					.compact();

			String encryptedJWT = encryptToken(jwt, prop.getProperty("encryptionKey"));
			String encryptedTime = encryptToken(String.valueOf(Instant.now().getEpochSecond()),
					prop.getProperty("encryptionKey"));

			token = "Bearer " + encryptedJWT + "." + encryptedTime;
			return token;
		} catch (Exception e) {
			throw new RuntimeException("Failed to generate auth token for PPCC", e);
		}
	}

	private String encryptToken(String data, String keyStr) throws Exception {
		byte[] keyBytes = keyStr.getBytes(StandardCharsets.UTF_8);
		SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
		IvParameterSpec iv = new IvParameterSpec(keyBytes);

		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);
		byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
		return Base64.getEncoder().encodeToString(encrypted);
	}

	public Response provisionApi(String accessToken, int policyId, List<Integer> locationIds, String packageVersionId) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======PPCC location provision api======");
		try {
			String ppccServiceBaseUrl = Utilities.getApiConfigProperty(env.toLowerCase() + ".ppccServiceBaseUrl");
			logger.info("PPCC Service base Url is ==> " + ppccServiceBaseUrl);
			RestAssured.baseURI = ppccServiceBaseUrl;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.provisionApi(policyId, locationIds, packageVersionId);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", accessToken);
			request.body(body);
			response = request.put(ApiConstants.provisionApi);
			logger.info("API Response of provision API is: " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response overrideConfig(String accessToken, List<Integer> locationIds, JSONObject configsToOverride) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======PPCC config override api======");
		try {
			String ppccServiceBaseUrl = Utilities.getApiConfigProperty(env.toLowerCase() + ".ppccServiceBaseUrl");
			logger.info("PPCC Service base Url is ==> " + ppccServiceBaseUrl);
			RestAssured.baseURI = ppccServiceBaseUrl;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.configOverride(locationIds, configsToOverride);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", accessToken);
			request.body(body);
			response = request.put(ApiConstants.configOverride);
			logger.info("API Response of config override API is: " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response getPolicyList(String accessToken, String queryParamter) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======PPCC get policy list api======");
		try {
			String ppccServiceBaseUrl = Utilities.getApiConfigProperty(env.toLowerCase() + ".ppccServiceBaseUrl");

			logger.info("PPCC Service base Url is ==> " + ppccServiceBaseUrl);
			RestAssured.baseURI = ppccServiceBaseUrl;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", accessToken);
			response = request.get(ApiConstants.policyList + queryParamter);
			logger.info("API Response of get policy list API is: " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response getLocationList(String accessToken, String queryParamter) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======PPCC get location list api======");
		try {
			String ppccServiceBaseUrl = Utilities.getApiConfigProperty(env.toLowerCase() + ".ppccServiceBaseUrl");
			logger.info("PPCC Service base Url is ==> " + ppccServiceBaseUrl);
			RestAssured.baseURI = ppccServiceBaseUrl;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", accessToken);
			response = request.get(ApiConstants.locationList + queryParamter);
			logger.info("API Response of get location list API is: " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response deprovisionApi(String accessToken, List<Integer> locationIds) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======PPCC location deprovision api======");
		try {
			String ppccServiceBaseUrl = Utilities.getApiConfigProperty(env.toLowerCase() + ".ppccServiceBaseUrl");
			logger.info("PPCC Service base Url is ==> " + ppccServiceBaseUrl);
			RestAssured.baseURI = ppccServiceBaseUrl;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.deprovisionApi(locationIds);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", accessToken);
			request.body(body);
			response = request.put(ApiConstants.deprovisionApi);
			logger.info("API Response of deprovision API is: " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response deletePolicy(String accessToken, int policyId) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======PPCC policy delete api======");
		try {
			String ppccServiceBaseUrl = Utilities.getApiConfigProperty(env.toLowerCase() + ".ppccServiceBaseUrl");
			logger.info("PPCC Service base Url is ==> " + ppccServiceBaseUrl);
			RestAssured.baseURI = ppccServiceBaseUrl;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", accessToken);
			String path = (ApiConstants.deletePolicy).replace("{policyId}", String.valueOf(policyId));
			logger.info("path is delete API" + path);
			response = request.delete(path);
			logger.info("API Response of policy delete API is: " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response getLocationListAuditLogs(String accessToken, String queryParamter) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======PPCC get location audit list api======");
		try {
			String ppccServiceBaseUrl = Utilities.getApiConfigProperty(env.toLowerCase() + ".ppccServiceBaseUrl");

			logger.info("PPCC Service base Url is ==> " + ppccServiceBaseUrl);
			RestAssured.baseURI = ppccServiceBaseUrl;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", accessToken);
			response = request.get(ApiConstants.listLocationAuditLogs + queryParamter);
			logger.info("API Response of get location audit list API is: " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response getLocationAuditLogsFilters(String accessToken, String queryParamter) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======PPCC get Location Audit Logs meta data Filters api======");
		try {
			String ppccServiceBaseUrl = Utilities.getApiConfigProperty(env.toLowerCase() + ".ppccServiceBaseUrl");

			logger.info("PPCC Service base Url is ==> " + ppccServiceBaseUrl);
			RestAssured.baseURI = ppccServiceBaseUrl;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", accessToken);
			response = request.get(ApiConstants.getLocationAuditLogsFilters + queryParamter);
			logger.info("API Response of get Location Audit Logs meta data Filters API is: " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(
					"Response code of get Location Audit Logs meta data Filters API is: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response remoteUpgrade(String accessToken, List<Integer> locationIds, String packageVersionId) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======PPCC locations remote upgrade api======");
		try {
			String ppccServiceBaseUrl = Utilities.getApiConfigProperty(env.toLowerCase() + ".ppccServiceBaseUrl");

			logger.info("PPCC Service base Url is ==> " + ppccServiceBaseUrl);
			RestAssured.baseURI = ppccServiceBaseUrl;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.remoteUpgrade(packageVersionId, locationIds);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", accessToken);
			request.body(body);
			response = request.put(ApiConstants.remoteUpgrade);
			logger.info("API Response of remote upgrade API is: " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response initiateUpdate(String accessToken, List<Integer> locationIds) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======PPCC locations initiate update api======");
		try {
			String ppccServiceBaseUrl = Utilities.getApiConfigProperty(env.toLowerCase() + ".ppccServiceBaseUrl");

			logger.info("PPCC Service base Url is ==> " + ppccServiceBaseUrl);
			RestAssured.baseURI = ppccServiceBaseUrl;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.initiateUpdate(locationIds);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", accessToken);
			request.body(body);
			response = request.put(ApiConstants.initiateUpdate);
			logger.info("API Response of initiate update api is: " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response cancelUpdate(String accessToken, List<Integer> locationIds) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======PPCC locations cancel update api======");
		try {
			String ppccServiceBaseUrl = Utilities.getApiConfigProperty(env.toLowerCase() + ".ppccServiceBaseUrl");

			logger.info("PPCC Service base Url is ==> " + ppccServiceBaseUrl);
			RestAssured.baseURI = ppccServiceBaseUrl;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.cancelUpdate(locationIds);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", accessToken);
			request.body(body);
			response = request.put(ApiConstants.cancelUpdate);
			logger.info("API Response of cancel update api is: " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response reprovision(String accessToken, List<Integer> locationIds, int policyId) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======PPCC locations reprovision api======");
		try {
			String ppccServiceBaseUrl = Utilities.getApiConfigProperty(env.toLowerCase() + ".ppccServiceBaseUrl");

			logger.info("PPCC Service base Url is ==> " + ppccServiceBaseUrl);
			RestAssured.baseURI = ppccServiceBaseUrl;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.reprovisionLocation(locationIds, policyId);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", accessToken);
			request.body(body);
			response = request.put(ApiConstants.reprovision);
			logger.info("API Response of reprovision api is: " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response retrieveLocationAuditLogs(String accessToken, String queryParamter, int auditLogId) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======PPCC retrieve Location Audit Logs list api======");
		try {
			String ppccServiceBaseUrl = Utilities.getApiConfigProperty(env.toLowerCase() + ".ppccServiceBaseUrl");

			logger.info("PPCC Service base Url is ==> " + ppccServiceBaseUrl);
			RestAssured.baseURI = ppccServiceBaseUrl;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", accessToken);
			String path = (ApiConstants.retrieveLocationAuditLogs).replace("{audit_log_id}",
					String.valueOf(auditLogId));
			response = request.get(path + queryParamter);
			logger.info("API Response of retrieve Location Audit Logs list API is: " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response getPolicyListAuditLogs(String accessToken, String queryParamter) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======PPCC get policy audit list api======");
		try {
			String ppccServiceBaseUrl = Utilities.getApiConfigProperty(env.toLowerCase() + ".ppccServiceBaseUrl");

			logger.info("PPCC Service base Url is ==> " + ppccServiceBaseUrl);
			RestAssured.baseURI = ppccServiceBaseUrl;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", accessToken);
			response = request.get(ApiConstants.listPolicyAuditLogs + queryParamter);
			logger.info("API Response of get policy audit list API is: " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response getPolicyAuditLogsFilters(String accessToken, String queryParamter) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======PPCC get Policy Audit Logs meta data Filters api======");
		try {
			String ppccServiceBaseUrl = Utilities.getApiConfigProperty(env.toLowerCase() + ".ppccServiceBaseUrl");

			logger.info("PPCC Service base Url is ==> " + ppccServiceBaseUrl);
			RestAssured.baseURI = ppccServiceBaseUrl;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", accessToken);
			response = request.get(ApiConstants.getPolicyAuditLogsFilters + queryParamter);
			logger.info("API Response of get Policy Audit Logs meta data Filters API is: " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response retrievePolicyAuditLogs(String accessToken, String queryParamter, int auditLogId) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======PPCC retrieve policy Audit Logs list api======");
		try {
			String ppccServiceBaseUrl = Utilities.getApiConfigProperty(env.toLowerCase() + ".ppccServiceBaseUrl");

			logger.info("PPCC Service base Url is ==> " + ppccServiceBaseUrl);
			RestAssured.baseURI = ppccServiceBaseUrl;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", accessToken);
			String path = (ApiConstants.retrievePolicyAuditLogs).replace("{audit_log_id}", String.valueOf(auditLogId));
			response = request.get(path + queryParamter);
			logger.info("API Response of retrieve policy Audit Logs list API is: " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// To fetch user offers rewards coupons
	public Response getStatusOfFlagFromConfigurationPageInWebhook(String type, String cookies) {
		Response response = null;
		requestParams = new JSONObject();
		String apiEndPoint = "";
		if (type.equalsIgnoreCase("webhook")) {
			apiEndPoint = ApiConstants.getWebhookStatusAPI;
		} else if (type.equalsIgnoreCase("adapter")) {
			apiEndPoint = ApiConstants.getAdapterStatusAPI;
		} else if (type.equalsIgnoreCase("configuration")) {
			apiEndPoint = ApiConstants.getWebhookConfigurationAPI;
		}

		try {
			RestAssured.baseURI = "https://eventframework.staging.punchh.io";
			RequestSpecification request = RestAssured.given().log().all();
			request.header("accept", "application/json, text/plain, */*");
			request.header("cookie", cookies);
			response = request.get(apiEndPoint);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
			// logger.info("Response code: "+response.getStatusCode());
			logger.info("API Response:" + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}

		return response;

	}

	public Response processBatchRedemptionPosApi(String locationKey, String userID, String Amount, String id,
			String externalUid, Map<String, String> detailsMap) {
		// this is the dynamic form of POS batch redemption API whwre we can
		// parameterize the different keys and their
		// respective values
		logger.info("Endpoints.processBatchRedemptionOfBasketPOS()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.BatchRedemptionPosApiPayload(locationKey, userID, Amount, id, externalUid,
					detailsMap);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// For batch process POS API
	public Response discountLookUpApiPos(String locationKey, String userID, String item_id, String amount,
			String externalUid, Map<String, String> detailsMap) throws InterruptedException {
		Thread.sleep(7000);
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.discountLookUpPosPayload(locationKey, userID, item_id, externalUid, amount,
					detailsMap);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.discountLookupPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processDiscountLookup() END");

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posPossibleRedemptionOfRedeemable(String email, String redeemable_id, String locationKey,
			String itemId) {
		String txn_no = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			String body = apipaylods.posPossibleRedemptionofRedeemablePayload(email, date, key, txn_no, redeemable_id,
					itemId);
			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request1.header("Authorization", "Token token=" + locationKey);
			request1.body(body);
			response = request1.post(ApiConstants.posPossibleRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response secureApiDiscountBasketAdded(String authToken, String client, String secret, String discountType,
			String discountID, String externalUid) {
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			String epoch = apiUtils.getSpan();
			String hash = apiUtils.getHash(secret + epoch);
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.secureApiDiscountBasketAddedPayload(discountType, discountID, externalUid);
			String URL = ApiConstants.discountBasketAdded + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;
			request.header("x-pch-digest", apiUtils.getSignature(secret, payload));
			request.header("Authorization", "Bearer " + authToken);
			request.header("Content-Type", "application/json");
			request.header("x-pch-hash", hash);
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			// request.queryParam("location_id", location_id);
			request.body(body);
			response = request.post(ApiConstants.discountBasketAdded);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionsAUTHAPI(String client, String secret, String locationKey, String authToken,
			String userID, String item_id, String externalUid, Map<String, String> detailsMap) {
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.getBatchRedemptionsAuthApiPayload(locationKey, userID, item_id, externalUid,
					detailsMap);
			String URL = ApiConstants.batchRedemptionAuthAPI + "?client=" + client;
			String payload = URL + body;
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			request.header("Authorization", "Bearer " + authToken);
			request.header("cache-control", "no-cache");
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionAuthAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response addDiscountToBasketWithExtIdAPI2(String access_token, String client, String secret,
			String discountType, String discountID, String externalUid) {
		Response response = null;
		requestParams = new JSONObject();

		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = "";
			if (discountType.equalsIgnoreCase("discount_amount")) {
				body = apipaylods.discountAmountItemWithExtIdPayload(discountType, discountID, externalUid);
			} else {

				body = apipaylods.discountBasketItemsAttributesWithExtUid(discountType, discountID, externalUid);
			}
			String URL = ApiConstants.mobApi2DiscountBasketAddeddAPI + "?client=" + client + "&access_token="
					+ access_token;
			String payload = URL + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));

			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.queryParam("access_token", access_token);

			request.body(body);
			response = request.post(ApiConstants.mobApi2DiscountBasketAddeddAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response deleteDiscountToBasketWithExtUidAPI2(String access_token, String client, String secret,
			String basketID, String externalUid) {
		Response response = null;
		requestParams = new JSONObject();

		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.removeDiscountBasketExtUIDMobileAPIPayload(basketID, externalUid);
			String URL = ApiConstants.mobApi2DeleteBasketForUserAPI + "?client=" + client + "&access_token="
					+ access_token;
			String payload = URL + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.queryParam("access_token", access_token);
			request.body(body);
			response = request.delete(ApiConstants.mobApi2DeleteBasketForUserAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;

	}

	public Response processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(String locationKey, String userID,
			String subAmount, String externalUid, Map<String, Map<String, String>> parentMap) {
		logger.info("Endpoints.processBatchRedemptionOfBasketPOS()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.processBatchRedemptionOfBasketPOSDiscountLookupWithExtUidPayload(locationKey,
					userID, subAmount, externalUid, parentMap);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchPOSDiscountAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(String locationKey, String userID,
			String subAmount, String query, String externalUid, Map<String, Map<String, String>> parentMap) {
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {

			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.generateReceiptWithQueryParamWithExtUid(locationKey, userID, subAmount, query,
					externalUid, parentMap);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posCheckinPhone(String phone, String authorization, String amount) {
		Response response = null;
		requestParams = new JSONObject();
		punchhKey = CreateDateTime.getRandomNumberSixDigit() + CreateDateTime.getTimeDateString();
		try {
			logger.info("*****POS Checkin API*****");
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			String body = ApiPayloads.posCheckinPhonePayLoad(phone, punchhKey, amount);
			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request1.header("Authorization", "Token token=" + authorization);
			request1.body(body.toString());
			response = request1.post(ApiConstants.posCheckin);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1UpdateUser(String client, String secret, String accessToken, String fieldName,
			String fieldValue) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1UpdateUser + "?client=" + client + "&hash=" + epoch;
			String body = apipaylods.api1UpdateUserDynamicFieldPayload(fieldName, fieldValue);
			String payload = URL + body;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + accessToken);
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.put(ApiConstants.mobApi1UpdateUser);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1UpdateUserEmailPhone(String client, String secret, String accessToken, String email,
			String phone, String fName, String lName) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1UpdateUser + "?client=" + client + "&hash=" + epoch;
			Map<String, Object> payloadMap = apipaylods.api1UpdateUserEmailPhonePayload(email, phone, fName, lName);
			String body = new JSONObject(payloadMap).toString();
			String payload = URL + body;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + accessToken);
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.put(ApiConstants.mobApi1UpdateUser);
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("error", e.getMessage());
		}
		return response;
	}

	public Response posDiscountLookupAPIInputPayload(List<Map<String, Object>> lineItems, String receipt_datetime,
			double subtotal_amount, double receipt_amount, String punchh_key, String transaction_no, String userID,
			String location_key, String external_uid) {

		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.finalPayloadAfterAddingMultipleLineItems(lineItems, receipt_datetime,
					subtotal_amount, receipt_amount, punchh_key, transaction_no, userID, location_key, external_uid);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.discountLookupPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			TestListeners.extentTest.get().info("POS Discount Lookup API response: " + response.asString());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response webhookZiplineAPI(String client, String secret, String email, String cardNumber, String status,
			String action, String uuid) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.webhookZiplinePayload(email, cardNumber, status, action);
			apiUtils.getSpan();
			String URL = ApiConstants.webhookZipline + uuid + "/loyalty_cards" + "?client=" + client;
			String payload = URL + body;
			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("Content-Type", "application/json");
			request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.webhookZipline + uuid + "/loyalty_cards");
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1LoyaltyCardCreateAPI(String client, String secret, String token, String card_number,
			boolean isCreate) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobAPi1LoyaltyCard + "?client=" + client + "&hash=" + epoch;
			String body = apipaylods.loyaltyCardPayload(card_number, isCreate);
			String payload = URL + body;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + token);
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobAPi1LoyaltyCard);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1LoyaltyCardUpdateAPI(String client, String secret, String token, String cardUUID,
			String new_card_number, boolean isCreate) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobAPi1LoyaltyCard + "/" + cardUUID + "?client=" + client + "&hash=" + epoch;
			String body = apipaylods.loyaltyCardPayload(new_card_number, isCreate);
			String payload = URL + body;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + token);
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.put(ApiConstants.mobAPi1LoyaltyCard + "/" + cardUUID);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1LoyaltyCardInfoAPI(String client, String secret, String token, String cardUUID) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			param = requestParams.toString();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobAPi1LoyaltyCard + "/" + cardUUID + "?client=" + client + "&hash=" + epoch;
			String payload = URL + param;

			request.headers(header.api1SignUpHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + token);
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.get(ApiConstants.mobAPi1LoyaltyCard + "/" + cardUUID);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1LoyaltyCardDelete(String client, String secret, String token, String cardUUID) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String payload = ApiConstants.mobAPi1LoyaltyCard + "/" + cardUUID + "?client=" + client + "&hash=" + epoch;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + token);
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.delete(ApiConstants.mobAPi1LoyaltyCard + "/" + cardUUID);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1UserSignUpWithLoyaltyCard(String client, String secret, String email, String card_number) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("email", email);
			hashmap.put("password", prop.getProperty("password"));
			hashmap.put("first_name", prop.getProperty("firstName"));
			hashmap.put("last_name", prop.getProperty("lastName"));
			hashmap.put("marketing_email_subscription", prop.getProperty("marketingEmailSubscription"));
			hashmap.put("marketing_pn_subscription", prop.getProperty("marketingPnSubscription"));
			hashmap.put("card_number", card_number);
			requestParams.put("user", hashmap);
			param = requestParams.toString();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1SignUp + "?client=" + client + "&hash=" + epoch;
			String payload = URL + param;
			request.headers(header.api1SignUpHeader(payload, epoch, secret));
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.post(ApiConstants.mobApi1SignUp);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1UpdateUserWithLoyaltyCard(String client, String secret, String accessToken,
			String card_number) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1UpdateUser + "?client=" + client + "&hash=" + epoch;
			String body = apipaylods.api1UpdateUserLoyaltyCard(card_number);
			String payload = URL + body;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + accessToken);
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.put(ApiConstants.mobApi1UpdateUser);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posUserLookupWithLoyaltyCard(String card_number, String location) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("*****POS User lookup Fetch Balance API*****");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.queryParam("card_number", card_number);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			requestParams.put("location_key", location);
			request.body(requestParams.toJSONString());
			response = request.get(ApiConstants.userLookupAndFetchBalance);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posCheckinWithLoyaltyCard(String card_number, String amount, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		punchhKey = CreateDateTime.getRandomNumberSixDigit() + CreateDateTime.getTimeDateString();
		try {
			logger.info("*****POS Checkin API*****");
			amount = Utilities.getApiConfigProperty("checkinAmount");
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given().log().all();
			String body = ApiPayloads.posCheckinLoyaltyCardPayLoad(card_number, punchhKey, amount);
			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request1.header("Authorization", "Token token=" + authorization);
			request1.body(body.toString());
			response = request1.post(ApiConstants.posCheckin);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public String getAuthForPackagesService(String claimValue) {
		logger.info("======Get packages with JWT Bearer Auth======");
		String secret = prop.getProperty("apiKey");

		Map<String, Object> claims = new HashMap<>();
		claims.put("origin", claimValue);

		SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		long expirationEpoch = 1799813272L * 1000;
		Date expirationDate = new Date(expirationEpoch);

		String jwtToken = Jwts.builder().setClaims(claims).setExpiration(expirationDate)
				.signWith(key, SignatureAlgorithm.HS256).compact();

		logger.info("Generated JWT token: " + jwtToken);
		return jwtToken;
	}

	public Response getPackageList(String token, String queryParamter) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Package get package list api======");
		try {
			String packageServiceBaseUrl = Utilities.getApiConfigProperty(env.toLowerCase() + ".packageServiceBaseUrl");
			logger.info("package Service base Url is ==> " + packageServiceBaseUrl);
			RestAssured.baseURI = packageServiceBaseUrl;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + token);
			response = request.get(ApiConstants.packagesList + queryParamter);
			logger.info("API Response of get packages list API is: " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code of get packages list API is: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response getPackageDownloadLink(String token, String packageId) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Package get package download link api======");
		try {
			String packageServiceBaseUrl = Utilities.getApiConfigProperty(env.toLowerCase() + ".packageServiceBaseUrl");
			logger.info("package Service base Url is ==> " + packageServiceBaseUrl);
			RestAssured.baseURI = packageServiceBaseUrl;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + token);

			String path = (ApiConstants.getDownloadPackageLink).replace("{packageId}", packageId);
			response = request.get(path);
			logger.info("API Response of get packages download link API is: " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code of get packages download link API is: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response updatePackage(String packageId, String description, String token) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("====== Update Package api======");
		try {
			String packageServiceBaseUrl = Utilities.getApiConfigProperty(env.toLowerCase() + ".packageServiceBaseUrl");
			logger.info("package Service base Url is ==> " + packageServiceBaseUrl);
			RestAssured.baseURI = packageServiceBaseUrl;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.updatePackagePayload(description);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + token);
			request.body(body);
			String path = (ApiConstants.updatePackage).replace("{packageId}", packageId);
			response = request.put(path);
			logger.info("API Response of update package API is: " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code of update package API is: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response getPackageDetails(String packageId, String token) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("====== Get Package details api======");
		try {
			String packageServiceBaseUrl = Utilities.getApiConfigProperty(env.toLowerCase() + ".packageServiceBaseUrl");
			logger.info("package Service base Url is ==> " + packageServiceBaseUrl);
			RestAssured.baseURI = packageServiceBaseUrl;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + token);
			String path = (ApiConstants.getPackage).replace("{packageId}", packageId);
			response = request.get(path);
			logger.info("API Response of get a package details API is: " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code of get a package details API is: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response deletePackage(String token, String packageId) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("====== Delete Package api======");
		try {
			String packageServiceBaseUrl = Utilities.getApiConfigProperty(env.toLowerCase() + ".packageServiceBaseUrl");
			logger.info("package Service base Url is ==> " + packageServiceBaseUrl);
			RestAssured.baseURI = packageServiceBaseUrl;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Authorization", "Bearer " + token);
			String path = (ApiConstants.deletePackage).replace("{packageId}", packageId);
			response = request.delete(path);
			logger.info("API Response of delete package API is: " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code of delete package API is: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response publishPackage(String token, String packageFilePath, String metaFilePath, String version) {
		Response response = null;
		logger.info("====== Publish Package API ======");

		try {
			String packageServiceBaseUrl = Utilities.getApiConfigProperty(env.toLowerCase() + ".packageServiceBaseUrl");
			logger.info("Package Service base URL: " + packageServiceBaseUrl);
			RestAssured.baseURI = packageServiceBaseUrl;
			RequestSpecification request = RestAssured.given().log().all();

			request.multiPart("package_file", new File(packageFilePath), "application/zip");
			request.multiPart("package_meta_file", new File(metaFilePath), "application/json");
			request.multiPart("version", version);
			request.multiPart("is_assignable", "true");
			request.multiPart("type_id", "1");
			request.multiPart("pos_type", "[1]");
			request.multiPart("stage", "dev");

			request.header("Authorization", "Bearer " + token);

			String path = ApiConstants.publishPackage;
			response = request.post(path);

			logger.info("API Response from publish package API: " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code from publish package API: " + response.getStatusCode());

		} catch (Exception e) {
			logger.error("Error occurred during publishPackage call: " + e.getMessage());
		}

		return response;
	}

	public Response Api1UserSignUp(String email, String client, String secret, String emailSubscription,
			String pnSubscription) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======API1 User SignUp======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("email", email);
			hashmap.put("password", Utilities.getApiConfigProperty("password"));
			hashmap.put("first_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("firstName")));
			hashmap.put("last_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("lastName")));
			hashmap.put("zip_code", Utilities.getApiConfigProperty("zipCode"));
			hashmap.put("signup_channel", Utilities.getApiConfigProperty("signUpChannel"));
			hashmap.put("referral_code", Utilities.getApiConfigProperty("referralCode"));
			hashmap.put("birthday", Utilities.getApiConfigProperty("birthday"));
			hashmap.put("privacy_policy", "true");
			hashmap.put("marketing_email_subscription", emailSubscription);
			hashmap.put("marketing_pn_subscription", pnSubscription);
			requestParams.put("user", hashmap);
			param = requestParams.toString();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1SignUp + "?client=" + client + "&hash=" + epoch;
			String payload = URL + param;
			request.headers(header.api1SignUpHeader(payload, epoch, secret));
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.post(ApiConstants.mobApi1SignUp);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1GoogleSignUp(String client, String secret, String JWT_Token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======Mobile Google API1 User SignUp======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("google_id_token", JWT_Token);
			requestParams.put("google_id_token", JWT_Token);
			param = requestParams.toString();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobAPi1GoogleSignup + "?client=" + client + "&hash=" + epoch;
			String payload = URL + param;
			request.headers(header.api1SignUpHeader(payload, epoch, secret));
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.post(ApiConstants.mobAPi1GoogleSignup);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2GoogleSignUp(String client, String secret, String JWT_Token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======Mobile API2 Google User SignUp======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("google_id_token", JWT_Token);
			requestParams.put("google_id_token", JWT_Token);
			param = requestParams.toString();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobAPi2GoogleSignup + "?client=" + client + "&hash=" + epoch;
			String payload = URL + param;
			request.headers(header.api1SignUpHeader(payload, epoch, secret));
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.post(ApiConstants.mobAPi2GoogleSignup);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authGoogleUserSignUp(String client, String secret, String JWT_Token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("=== Auth Google User SignUp ===");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			requestParams.put("google_id_token", JWT_Token);
			param = requestParams.toString();
			String payload = ApiConstants.authGoogleSignup + "?client=" + client + param;
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			request.header("Content-Type", prop.getProperty("contentType"));
			request.header("x-pch-digest", signature);
			request.queryParam("client", client);
			request.body(param);
			response = request.post(ApiConstants.authGoogleSignup);
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;

	}

	public Response getDiscountBasketByUIDUsingApiMobile(String authToken, String client, String secret,
			String externalUid) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			String epoch = apiUtils.getSpan();
			String hash = apiUtils.getHash(secret + epoch);
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.fetchActiveBasketAuthApiayload(externalUid);
			String URL = ApiConstants.getDiscountBasketAPI + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;
			request.header("x-pch-digest", apiUtils.getSignature(secret, payload));
			request.header("Authorization", "Bearer " + authToken);
			request.header("Content-Type", "application/json");
			request.header("x-pch-hash", hash);
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.get(ApiConstants.getDiscountBasketAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response deleteMobileDiscountBasketByUID(String authToken, String client, String secret,
			String discount_basket_item_id, String externalUid) {
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			String epoch = apiUtils.getSpan();
			String hash = apiUtils.getHash(secret + epoch);
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.deleteBasketItem_Payload(discount_basket_item_id, externalUid);
			String URL = ApiConstants.deleteBasketForUserAPI + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;
			request.header("x-pch-digest", apiUtils.getSignature(secret, payload));
			request.header("Authorization", "Bearer " + authToken);
			request.header("Content-Type", "application/json");
			request.header("x-pch-hash", hash);
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.delete(ApiConstants.deleteBasketForUserAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response getDiscountBasketByUIDUsingApi2Mobile(String authToken, String client, String secret,
			String externalUid) {
		Response response = null;
		requestParams = new JSONObject();

		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			String body = apipaylods.fetchActiveBasketAuthApiayload(externalUid);
			RequestSpecification request = RestAssured.given().log().all();
			String URL = ApiConstants.mobApi2GetDiscountBasketDetailsAPI + "?client=" + client + "&access_token="
					+ authToken;
			String payload = URL + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.queryParam("access_token", authToken);
			request.body(body);
			response = request.get(ApiConstants.mobApi2GetDiscountBasketDetailsAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;

	}

	public Response deleteMobileDiscountBasketByUIDApi2(String access_token, String client, String secret,
			String discount_basket_item_id, String externalUid) {
		Response response = null;
		requestParams = new JSONObject();

		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.deleteBasketItem_Payload(discount_basket_item_id, externalUid);

			String URL = ApiConstants.mobApi2DeleteBasketForUserAPI + "?client=" + client + "&access_token="
					+ access_token;
			String payload = URL + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.queryParam("access_token", access_token);

			request.body(body);
			response = request.delete(ApiConstants.mobApi2DeleteBasketForUserAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;

	}

	public Response authBatchRedemptionWithQueryParam(List<Map<String, Object>> lineItems, String receipt_datetime,
			double subtotal_amount, double receipt_amount, String punchh_key, String transaction_no, String userID,
			String client, String secret, String authToken, String external_uid, String query, String location_key,
			int storeNum) {

		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();

		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.finalPayloadAfterAddingMultipleLineItemsWithQueryParam(lineItems, receipt_datetime,
					subtotal_amount, receipt_amount, punchh_key, transaction_no, userID, location_key, external_uid,
					query, storeNum);
			String URL = ApiConstants.batchRedemptionAuthAPI + "?client=" + client;
			String payload = URL + body;
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			request.header("Authorization", "Bearer " + authToken);
			request.header("cache-control", "no-cache");
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionAuthAPI);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response getWorldpayTransactionSetupId(String url, String token, boolean isCaptchaPresent) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = url;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getWorldpayTransactionSetupIdPayload(token, isCaptchaPresent);
			request.header("Content-Type", "text/xml");
			request.body(body);
			response = request.post();
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response getPaymentToken(String url, String token, String cardToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = url;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.getWorldpayPaymentAccountID(token, cardToken);
			request.header("Content-Type", "text/xml");
			request.body(body);
			response = request.post();
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response createPaymentCard(String client, String secret, String token, String padp_code, String pay_token,
			String pcard_name, boolean pcard_pref) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.paymentCardPayload(padp_code, pay_token, pcard_name, pcard_pref);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobAPi1createPaymentCard + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());

			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobAPi1createPaymentCard);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response POSDiscountLookupWithChannel(String locationKey, String userID, String item_id, String externalUID,
			String channel) {
		logger.info("Endpoints.processBatchRedemptionOfBasketPOS()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.posAPIDiscountLookUpPayloadWithChannel(locationKey, userID, item_id, externalUID,
					channel);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchPOSDiscountAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1PurchaseGiftCardWithRecurring(String client, String secret, String design_id, String amount,
			String token, String transactionToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API1PurchaseGiftcardWithRecurring(design_id, amount, transactionToken);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1PurchaseGiftCard + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());

			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1PurchaseGiftCard);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1ParPaymentGetClientToken(String client, String secret, String userToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			param = requestParams.toString();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.parPaymentGetClientToken + "?client=" + client + "&hash=" + epoch;
			String payload = URL + param;

			request.headers(header.api1SignUpHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + userToken);
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.get(ApiConstants.parPaymentGetClientToken);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1IssuanceGiftCard(String client, String secret, String design_id, String token) {

		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API1IssuanceGiftCard(design_id);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1IssuanceGiftcard + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());

			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1IssuanceGiftcard);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}

	public Response Api2RevokeGiftCard(String client, String secret, String access_token, String uuid, String email) {
		Response response = null;
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2ClientPayload(client, email);

			String payload = ApiConstants.mobApi2GiftCard + uuid + "/revoke" + body;

			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());

			request.body(body);
			response = request.delete(ApiConstants.mobApi2GiftCard + uuid + "/revoke");
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}

	public Response api1ConsolidateGiftCard(String client, String secret, String accessToken, String targetGiftCardUuid,
			String sourceGiftCardUuid) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String body = apipaylods.API1GiftCardConsolidate(sourceGiftCardUuid);
			String URL = ApiConstants.mobApi1GiftCard + targetGiftCardUuid + "/consolidate" + "?client=" + client
					+ "&hash=" + epoch;
			String payload = URL + body;
			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + accessToken);
			request.header("Content-Type", api1Headers.contentType());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.patch(ApiConstants.mobApi1GiftCard + targetGiftCardUuid + "/consolidate");
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}

	public Response Api1ReloadGiftCardRecurring(String client, String secret, String amount, String token, String uid,
			String uuidPaymentCard) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API1PurchaseGiftcardWithTransactionIDRecurring(amount, uuidPaymentCard);
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1ReloadGiftCard + uid + "/reload" + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());

			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1ReloadGiftCard + uid + "/reload");
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}

	public Response api1UpdateGiftCardwithAutoreloadConfig(String client, String secret, String accessToken,
			String giftCardUuid, boolean preferred, boolean enableAutoreload, String thrAmt, String defAmt,
			String payCardUUID) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API1GiftCardAutoreloadConfig(preferred, enableAutoreload, thrAmt, defAmt,
					payCardUUID);

			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1GiftCard + giftCardUuid + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;
			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Authorization", "Bearer " + accessToken);
			request.header("Content-Type", api1Headers.contentType());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.put(ApiConstants.mobApi1GiftCard + giftCardUuid);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}

	public Response api1FetchGiftCardList(String client, String secret, String accessToken, String giftCardUuid) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String payload = ApiConstants.mobApi1GiftCard + "?client=" + client + "&hash=" + epoch;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + accessToken);
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.get(ApiConstants.mobApi1GiftCard);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());

		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}

	public Response bulkAddMemberesInSegment(String name, String segmentId, File csvFilePath, String apiKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			new HashMap<String, Object>();
			request.multiPart("name", name);
			request.multiPart("custom_segment_id", segmentId);
			request.multiPart("bulk_guest_activity_file", csvFilePath);

			new HashMap<String, Object>();
			request.multiPart("name", name);
			request.multiPart("custom_segment_id", segmentId);
			request.multiPart("bulk_guest_activity_file", csvFilePath);

			request.header("Authorization", apiKey);
			request.header("Accept", "application/json");
			response = request.post(ApiConstants.bulkAddUsersInSegmentAPi);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2ListAllDeals2(String client, String secret, String token, String perPage, String page) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.reset();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// Query param
			request.queryParam("client", client);
			request.queryParam("per_page", perPage);
			request.queryParam("page", page);
			request.queryParam("order_by", "created_at");
			// Payload
			String payload = ApiConstants.mobApi2ListAllDeals + "?client=" + client + "&per_page=" + perPage + "&page="
					+ page + "&order_by=created_at";
			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + token);
			request.header("Connection", "keep-alive");
			request.header("User-Agent", "PostmanRuntime/7.28.4");
			request.header("cache-control", "no-cache");
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			response = request.get(ApiConstants.mobApi2ListAllDeals);

			logger.info(response.asPrettyString());
			// logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			int i = 0;
			while (i < 60) {
				if (response.asString().equals("[]")) {
					Thread.sleep(1000);
					response = request.get(ApiConstants.mobApi2ListAllDeals);
				} else {
					break;
				}
				i++;
			}
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response addPolicy(String accessToken, String policyName, int posTypeId, String queryParam, String status) {
		return sendPolicyRequest(accessToken, policyName, posTypeId, queryParam, "======PPCC add policy api======",
				status);
	}

	public Response duplicatePolicy(String accessToken, String policyName, int posTypeId, String queryParam,
			String status) {
		return sendPolicyRequest(accessToken, policyName, posTypeId, queryParam,
				"======PPCC duplicate policy api======", status);
	}

	private Response sendPolicyRequest(String accessToken, String policyName, int posTypeId, String queryParam,
			String logMessage, String status) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info(logMessage);
		try {
			String ppccServiceBaseUrl = Utilities.getApiConfigProperty(env.toLowerCase() + ".ppccServiceBaseUrl");
			logger.info("PPCC Service base Url is ==> " + ppccServiceBaseUrl);
			RestAssured.baseURI = ppccServiceBaseUrl;

			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.policyPayload(policyName, posTypeId, status);

			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", accessToken);
			request.body(body);

			String path = ApiConstants.addPolicy + queryParam;
			response = request.post(path);

			logger.info("API Response of policy API is: " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error("Error occurred while sending policy request: " + e.getMessage(), e);
		}
		return response;
	}

	public Response getPolicyDetails(String accessToken, int policyId) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======PPCC get policy details api======");
		try {
			String ppccServiceBaseUrl = Utilities.getApiConfigProperty(env.toLowerCase() + ".ppccServiceBaseUrl");
			;
			logger.info("PPCC Service base Url is ==> " + ppccServiceBaseUrl);
			RestAssured.baseURI = ppccServiceBaseUrl;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", accessToken);
			String path = (ApiConstants.policyDetails).replace("{policyId}", String.valueOf(policyId));
			response = request.get(path);
			logger.info("API Response of get policy details API is: " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response updatePolicy(String accessToken, int policyId, String policyName, int posTypeId, String status) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======PPCC update policy api======");
		try {
			String ppccServiceBaseUrl = Utilities.getApiConfigProperty(env.toLowerCase() + ".ppccServiceBaseUrl");
			logger.info("PPCC Service base Url is ==> " + ppccServiceBaseUrl);
			RestAssured.baseURI = ppccServiceBaseUrl;
			RequestSpecification request = RestAssured.given().log().all();
			String updatedPolicyName = "Updated " + policyName;
			String body = apipaylods.policyPayload(updatedPolicyName, posTypeId, status);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", accessToken);
			request.body(body);
			String path = (ApiConstants.updatePolicy).replace("{policyId}", String.valueOf(policyId));
			response = request.put(path);
			logger.info("API Response of update policy API is: " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response getCampaignsFromSuperAdminScheduledPage(int page) {
		Response response = null;
		requestParams = new JSONObject();

		logger.info("======Get Campaigns From SuperAdmin Scheduled Page ======");
		TestListeners.extentTest.get().info("======Get Campaigns From SuperAdmin Scheduled Page ======");

		String password = prop.getProperty("password");
		String email = prop.getProperty("email");

		String basicAuth = getBasicAuthenticationHeader(email, password);

		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Authorization", basicAuth);
			request.queryParam("page", page);
			response = request.get(ApiConstants.getCampaignNameFromSuperadmin);
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response kouponMediaAgeVerification(String slug, String authorizationToken, String userID, String status,
			String dateTime) {
		Response response = null;
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.kouponMediaAgeVerificationPayload(userID, status, dateTime);
			request.header("Authorization", "Bearer " + authorizationToken);
			request.header("User-Agent", "KM");
			request.header("Content-Type", "application/json");
			request.body(body.toString());
			response = request.post((ApiConstants.apiKMAgeVerification).replace("$slug", slug));
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response supportGiftingToUserFlagon(String userID, String authToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Support Gifting to a User======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.supportGiftingToUserFalse(userID);
			// Headers
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + authToken);
			request.body(body);
			response = request.post(ApiConstants.supportGiftingToUser);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionPosApiPayloadWithMap(String locationKey, String userID, String Amount,
			String qty, String id, String externalUid, Map<String, String> DetailsMap) {
		logger.info("Endpoints.processBatchRedemptionOfBasketPOS()START ");
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.processBatchRedemptionPosApiPayload(locationKey, userID, Amount, qty, id,
					externalUid, DetailsMap);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posBatchRedemptionWithLineItemsMap(List<Map<String, Object>> lineItems, String receipt_datetime,
			String subtotal_amount, String receipt_amount, String punchh_key, String transaction_no, String userID,
			String location_key, String external_uid) {
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.posBatchRedemptionPayload(lineItems, receipt_datetime, subtotal_amount,
					receipt_amount, punchh_key, transaction_no, userID, location_key, external_uid);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			TestListeners.extentTest.get().info("POS Batch Redemption API response: " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response discountLookUpPosApiWithMap(String locationKey, String userID, String item_id, String amount,
			String externalUid, Map<String, String> DetailsMap) throws InterruptedException {
		Thread.sleep(7000);
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.discountLookUpPosApiPayload(locationKey, userID, item_id, externalUid, amount,
					DetailsMap);
			request.header("Content-Type", "application/json");
			request.body(body);
			response = request.post(ApiConstants.discountLookupPOSAPI);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processDiscountLookup() END");
			TestListeners.extentTest.get().info("POS Discount Lookup API response: " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// Send points/punches to user
	public Response sendPointsToUserFlagOn(String userId, String authToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.sendPointsToUserFalse(userId);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + authToken);
			request.body(body);
			response = request.post(ApiConstants.sendMessageToUser);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response supportGiftingToUserFlagonParamTrue(String userID, String authToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Support Gifting to a User======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.supportGiftingToUserTrue(userID);
			// Headers
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + authToken);
			request.body(body);
			response = request.post(ApiConstants.supportGiftingToUser);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// Send points/punches to user
	public Response sendPointsToUserFlagOnParamTrue(String userId, String authToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.sendPointsToUserTrue(userId);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + authToken);
			request.body(body);
			response = request.post(ApiConstants.sendMessageToUser);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiGetPromotionAccountBalance(String accountId, String client, String secret) {
		Response response = null;
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams = new JSONObject();

			// Build signature
			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.authGetPromotionAccounts + accountId + param;
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			request.body(requestParams.toJSONString());

			// Set headers
			Map<String, Object> headers = new HashMap<>();
			headers.put("Content-Type", prop.getProperty("contentType"));
			headers.put("X-Promo-Key", client);
			headers.put("X-Promo-Signature", signature);
			request.headers(headers);

			// Construct endpoint
			String endpoint = ApiConstants.authGetPromotionAccounts.endsWith("/")
					? ApiConstants.authGetPromotionAccounts + accountId
					: ApiConstants.authGetPromotionAccounts + "/" + accountId;

			response = request.get(endpoint);

			logger.info("API Response: " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Status Code: " + response.statusCode());

		} catch (Exception e) {
			logger.error("Exception in authApiGetPromotionAccountBalance: ", e);
		}
		return response;
	}

	public Response Api2MobileSegmentEligibility(String client, String secret, String token, String userId,
			String segmentId) {
		Response response = null;
		requestParams = new JSONObject();
		String randomString = Utilities.phonenumber();
		try {
			String baseUri = getMobileApiBaseUrl();
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.queryParam("client", client);
			request.queryParam("dlc_identifier", userId);
			request.queryParam("segment_id", segmentId);
			String payload = ApiConstants.mobApi2SegmentEligibility + "/" + randomString + "/segments?client=" + client
					+ "&dlc_identifier=" + userId + "&segment_id=" + segmentId;

			request.header("Authorization", "Bearer " + token);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept", api2Headers.api2Accept());
			request.header("User-Agent", api2Headers.api2userAgent());
			response = request.get(ApiConstants.mobApi2SegmentEligibility + "/" + randomString + "/segments");
			logger.info("Response code: " + response.getStatusCode());
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error("Exception in authApiGetPromotionAccountBalance: ", e);
		}
		return response;
	}

	// promotions/accruals api used
	public Response authApiGetPromotionsAccruals(String accountId, String client, String secret,
			Map<String, Object> mapOfDetails) {
		Response response = null;
		requestParams = new JSONObject();
		utils.logInfo("== Post Promotions Accruals API ==");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String URL = ApiConstants.authGetPromotionAccruals;
			String body = apipaylods.getPromotionalAccrualsAPIPayload(accountId, mapOfDetails);
			String payload = URL + body;
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			request.header("X-Promo-Key", client);
			request.header("X-Promo-Signature", signature);
			request.header("Content-Type", "application/json");
			request.header("Accept-Timezone", "Etc/UTC");
			request.body(body);
			response = request.post(ApiConstants.authGetPromotionAccruals);
			utils.logit(response);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// promotions/accruals api used
	public Response authApiGetPromotionsAccrualsDelete(String checkinID, String accountID, String client, String secret,
			Map<String, Object> mapOfDetails) {
		Response response = null;
		requestParams = new JSONObject();
		utils.logInfo("== Delete Promotions Accruals API ==");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String URL = ApiConstants.authGetPromotionAccruals + "/" + checkinID;
			String body = apipaylods.getPromotionalAccrualsAPIPayloadForDelete(accountID, mapOfDetails);
			String payload = URL + body;
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			request.header("X-Promo-Key", client);
			request.header("X-Promo-Signature", signature);
			request.header("Content-Type", "application/json");
			request.header("Accept-Timezone", "Etc/UTC");
			request.body(body);
			response = request.delete(ApiConstants.authGetPromotionAccruals + "/" + checkinID);
			utils.logit(response);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}

		return response;
	}

	// /promotions/redemptions api used
	public Response authApiGetPromotionsAccrualsRedemption(String accountId, String client, String secret,
			Map<String, Object> mapOfDetails) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String URL = ApiConstants.authGetPromotionAccrualsRedemptions;
			String body = apipaylods.getPromotionalAccrualsAPIPayload(accountId, mapOfDetails);
			String payload = URL + body;
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			request.header("X-Promo-Key", client);
			request.header("X-Promo-Signature", signature);
			request.header("Content-Type", "application/json");
			request.header("Accept-Timezone", "Etc/UTC");
			request.body(body);
			response = request.post(ApiConstants.authGetPromotionAccrualsRedemptions);
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// /promotions/redemptions api used
	public Response authApiGetPromotionsAccrualsValidate(String accountId, String client, String secret,
			Map<String, Object> mapOfDetails) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String URL = ApiConstants.authGetPromotionAccrualsValidate;
			String body = apipaylods.getPromotionalAccrualsAPIPayload(accountId, mapOfDetails);
			String payload = URL + body;
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			request.header("X-Promo-Key", client);
			request.header("X-Promo-Signature", signature);

			request.header("Content-Type", "application/json");

			request.header("Accept-Timezone", "Etc/UTC");

			request.body(body);
			response = request.post(ApiConstants.authGetPromotionAccrualsValidate);
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// POS Payment Void API
	public Response POSPaymentVoid(String paymentReferenceId, String locationKey) {
		Response response = null;
		requestParams = new JSONObject();

		CreateDateTime.getCurrentDate();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.POSPaymentRefundPayload(paymentReferenceId, locationKey);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.body(body);
			response = request.delete(ApiConstants.posPayment);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public void cloudflareToken(RequestSpecification request) throws Exception {
		// Add Cloudflare token for QA environment
		request.header("x-px-access-token", cloudFareToken);
		request.header("x-bypass-cloudflare-waf", cloudFareBypass);
	}

	public Response inboundSegmentBulkUserEventGenerate(String proxyUrl, String hostURL, String appkey, int accountId,
			List<Map<String, String>> userProfiles) {
		String body = apipaylods.generateInboundBulkPayload(userProfiles, hostURL, appkey, accountId);
		Response response = null;
		requestParams = new JSONObject();
		String baseURL = proxyUrl + ApiConstants.mParticeInboundSegmentUser;
		try {
			RestAssured.baseURI = baseURL;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Content-Type", "application/json");
			request.header("Accept-Encoding", "gzip, br");
			request.body(body);
			response = request.post();
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			TestListeners.extentTest.get().info("Inbound Segment Bulk User Event API Payload: " + body);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;

	}

	public Response segmentCloseOrderOnline(String client, String secret, String slugName, String externalUid,
			String authorization, String emailID, String storeNumber, String posRef, String oloSlug, String csID,
			String orderID) {
		String oloTimeStamp = CreateDateTime.getTimeDateString();
		String oloSecretKey = "et3LdDo-SvqsaGQeqICXFVoMNaJUbm5ugePTOoN65rWfmJaHrhjo34QLQfow-e8e";
		UUID uuid = UUID.randomUUID();
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Purchase Gift Card======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.segmentCloseOrderOnlinePayload(externalUid, emailID, storeNumber, posRef, oloSlug,
					csID, orderID);

			String closeOrderOnineURL = baseUri + ApiConstants.closeOrderOnine.replace("$slugName", slugName);

			// Headers passing Signature and content type
			String payload = closeOrderOnineURL + "\n" + body + "\n" + uuid.toString() + "\n" + oloTimeStamp;

			String signature = apiUtils.getOloSignature(oloSecretKey, payload);
			logger.info("Signature -------- " + signature);

			request.header("Accept", "application/json");
			request.header("X-Olo-event-Type", "OrderClosed");
			request.header("Content-Type", "application/json");
			request.header("X-Olo-Signature", signature);
			request.header("X-Olo-Timestamp", oloTimeStamp);
			request.header("X-Olo-Message-Id", uuid.toString());
			request.header("Accept-Language", "en;q=1");
			request.header("app_build", "2");
			// request.header("app_version", "4.0.0");
			// request.headers("Authorization", "Bearer " + authorization);
			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.post(closeOrderOnineURL);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}

		return response;
	}

	public Response updateLocationUsingPOJO(String jsonBody, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Update Location======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.body(jsonBody);
			response = request.patch(ApiConstants.updateLocation);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response apiAddExistingGiftCard(String design_id, String card_number, String epin, String client,
			String access_token, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Add Existing Gift Card======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.api2AddExistingGiftCardPayload(design_id, card_number, epin, client, access_token);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2AddExistingGiftCard + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Content-Type", api2Headers.api2ContentType());

			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.post(ApiConstants.mobApi2AddExistingGiftCard);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// Enrich Online Order API
	public Response enrichOnlineOrderApi(String txn, String adminKey) {
		Response response = null;

		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.queryParam("transaction_no", txn);
			request.header("Accept", "application/json");
			request.header("Authorization", "Bearer " + adminKey);
			response = request.get(ApiConstants.enrichOnlineOrderApi2 + "?transaction_no=" + txn);
			logger.info("API Response of enrich Online Order API is: " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			cloudflareToken(request);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// curl --location --request DELETE
	// 'https://bhanubelwal.punchh.io/promotions/redemptions/2AR5hyfF-k83UQofPzEH'
	public Response authApiGetPromotionsRedemptionDelete(String uuid, String accountID, String client, String secret,
			Map<String, Object> mapOfDetails) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String URL = ApiConstants.authGetPromotionAccrualsRedemptions + "/" + uuid;
			String body = apipaylods.getPromotionRedemptionsAPIPayloadForDelete(accountID, mapOfDetails);
			String payload = URL + body;
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			request.header("X-Promo-Key", client);
			request.header("X-Promo-Signature", signature);
			request.header("Content-Type", "application/json");
			request.header("Accept-Timezone", "Etc/UTC");
			request.body(body);
			response = request.delete(ApiConstants.authGetPromotionAccrualsRedemptions + "/" + uuid);
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}

		return response;
	}

	public Map<String, Object> getPromotionAccrualsRewardsObjectArray(String id, String provider, String level,
			String product, double discount, String type) {
		Map<String, Object> detailsMap = new HashMap<String, Object>();
		detailsMap.put("id", id);
		detailsMap.put("provider", provider);
		detailsMap.put("level", level);
		detailsMap.put("product", product);
		detailsMap.put("discount", discount);
		detailsMap.put("type", type);
		return detailsMap;
	}

	// promotions/accruals api used
	public Response authApiGetPromotionsAccrualsNew(String accountId, String client, String secret,
			Map<String, Object> mapOfDetails, Map<String, Map<String, Object>> rewardsMap) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String URL = ApiConstants.authGetPromotionAccruals;
			String body = apipaylods.getPromotionalAccrualsAPIPayloadNew(accountId, mapOfDetails, rewardsMap);
			String payload = URL + body;
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			request.header("X-Promo-Key", client);
			request.header("X-Promo-Signature", signature);
			request.header("Content-Type", "application/json");
			request.header("Accept-Timezone", "Etc/UTC");
			request.body(body);
			response = request.post(ApiConstants.authGetPromotionAccruals);
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api2SingleScanCodeForTipType(String authToken, String payment_type, String transaction_token,
			String client, String secret, String tip_type, String tip) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.api2SingleScanCodeForTipTypePayload(payment_type, transaction_token, client,
					transaction_token, tip);

			// Build the payload dynamically based on the presence of tipType and tip

			if (tip_type != null && !tip_type.trim().isEmpty()) {
				body = apipaylods.api2SingleScanCodeForTipTypePayload(payment_type, transaction_token, client, tip_type,
						tip);
			} else {
				body = apipaylods.api2SingleScanCodeForWithoutTipTypePayload(payment_type, transaction_token, client,
						tip);
			}
			// Headers
			String payload = ApiConstants.mobApi2SingleScanCode + body;
			request.header("Authorization", "Bearer " + authToken);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			request.header("User-Agent", api2Headers.api2userAgent());
			request.header("punchh-app-device-id", api2Headers.api2appDeviceId());
			request.body(body);
			response = request.post(ApiConstants.mobApi2SingleScanCode);
			TestListeners.extentTest.get().info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response createLIS(String jsonBody, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Create LIS======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.body(jsonBody);
			response = request.post(ApiConstants.createLisAPI);
			utils.logit("Create LIS API Response: " + response.asPrettyString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response createQC(String jsonBody, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Create QC======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.body(jsonBody);
			response = request.post(ApiConstants.createQualificationCriteria_API);
			utils.logit("Create QC API Response: " + response.asPrettyString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response createRedeemable(String jsonBody, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Create Redeemable======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.body(jsonBody);
			response = request.post(ApiConstants.redeemableListAPi);
			utils.logit("Create Redeemable API Response: " + response.asPrettyString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response parMenuOrder(String slugName, String externalUid, String authorization, String emailID,
			String storeNumber, String posRef, String oloSlug, String csID, String orderID, String parSecretKey) {
		String parTimeStamp = CreateDateTime.getTimeDateString();
		UUID uuid = UUID.randomUUID();
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Purchase Gift Card======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.closeOrderOnlinePayload(externalUid, emailID, storeNumber, posRef, oloSlug, csID,
					orderID);
			logger.info(body);
			String parMenuOrderURL = baseUri + ApiConstants.parMenuOrder.replace("$slugName", slugName);
			String payload = parMenuOrderURL + "\n" + uuid + "\n" + parTimeStamp;

			String signature = apiUtils.getOloSignature(parSecretKey, payload);
			logger.info("Signature -------- " + signature);

			request.header("Accept", "application/json");
			request.header("Accept-Language", "en;q=1");
			request.header("app_build", "2");
			request.header("Content-Type", "application/json");
			request.header("X-Par-ordering-event-Type", "OrderClosed");
			request.header("X-Par-ordering-Signature", signature);
			request.header("X-Par-ordering-Timestamp", parTimeStamp);
			request.header("X-Par-ordering-Message-Id", uuid.toString());
			request.header("Authorization", "Bearer " + authorization);
			request.body(body);
			response = request.post(parMenuOrderURL);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}

		return response;
	}

	public Response parOrderPlacedAndOrderCancelled(String slugName, String authorization, String emailID,
			String storeNumber, String orderID, String parSecretKey, String eventType, String randomFirstName,
			String randomLastName) {
		String parTimeStamp = CreateDateTime.getTimeDateString();
		UUID uuid = UUID.randomUUID();
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.orderPlacedAndCancelledPayload(emailID, storeNumber, orderID, randomFirstName,
					randomLastName);

			String parMenuOrderURL = baseUri + ApiConstants.parMenuOrder.replace("$slugName", slugName);
			String payload = parMenuOrderURL + "\n" + uuid + "\n" + parTimeStamp;

			String signature = apiUtils.getOloSignature(parSecretKey, payload);

			request.header("Accept", api1Headers.accept());
			request.header("Content-Type", api1Headers.contentType());
			request.header("X-Par-ordering-event-Type", eventType);
			request.header("X-Par-ordering-Signature", signature);
			request.header("X-Par-ordering-Timestamp", parTimeStamp);
			request.header("X-Par-ordering-Message-Id", uuid.toString());
			request.body(body);
			response = request.post(parMenuOrderURL);

		} catch (Exception e) {
			logger.error(e.getMessage());
		}

		return response;
	}

	public Response parOrderPlacedAndOrderCancelledWithRetry(String slugName, String authorization, String emailID,
			String storeNumber, String orderID, String parSecretKey, String eventType, String randomFirstName,
			String randomLastName) throws InterruptedException {
		int maxRetries = 5;
		int waitTimeSeconds = 2;
		Response response = null;
		for (int attempt = 1; attempt <= maxRetries; attempt++) {
			response = parOrderPlacedAndOrderCancelled(slugName, authorization, emailID, storeNumber, orderID,
					parSecretKey, eventType, randomFirstName, randomLastName);
			String responseBody = response.asString();
			int statusCode = response.getStatusCode();
			utils.logit("Attempt " + attempt + " | Status: " + statusCode + " | Response: " + responseBody);

			if (statusCode == 200 && responseBody.contains("Processed Successfully")) {
				return response;
			}
			// RETRY ONLY FOR INGESTION FAILURE
			if (responseBody.contains("Error in Ingesting")) {
				utils.longWaitInSeconds(waitTimeSeconds);
				waitTimeSeconds *= 2; // exponential backoff
			} else {
				break; // unexpected error → fail fast
			}
		}
		return response;
	}

	public Response authOnlineSubscriptionRedemption(String authentication_token, String client, String secret,
			String item_id, String subscription_id) {
		Response response = null;
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.authOnlineSubscriptionRedemptionPayload(authentication_token, client, item_id,
					subscription_id);
			String payload = ApiConstants.authOnlineRedemption + body;
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			request.headers(defaultHeaderMap);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.post(ApiConstants.authOnlineRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response createLocationCallsFromYext(String name, String store_number, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Create Location======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.queryParam("context", "callsfromyext");
			cloudflareToken(request);
			String body = apipaylods.createLocation(name, store_number);
			request.body(body);
			response = request.post(ApiConstants.createLocation);
			utils.logit("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response updateLocationCallsFromYext(String location_id, String store_number, String authorization,
			String name) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Update Location======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.queryParam("context", "callsfromyext");
			cloudflareToken(request);
			String body = apipaylods.updateLocationCallsFromYext(location_id, store_number, name);
			request.body(body);
			response = request.patch(ApiConstants.updateLocation);
			utils.logit("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response deleteLocationCallsFromYext(String location_id, String store_number, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Delete Location======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.queryParam("context", "callsfromyext");
			cloudflareToken(request);
			String body = apipaylods.deleteLocation(location_id, store_number);
			request.body(body);
			response = request.delete(ApiConstants.deleteLocation);
			utils.logit("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1PointTransferToOtherUser(String recipientEmail, String point, String client, String secret,
			String accessToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1PointTransfer + "?client=" + client + "&hash=" + epoch;
			String body = apipaylods.api1CurrencyTransferToOtherUser(recipientEmail, point);
			String payload = URL + body;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			request.header("Authorization", "Bearer " + accessToken);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1PointTransfer);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1ChallengeOptIn(int id, String client, String secret, String token, String action) {
		Response response = null;
		requestParams = new JSONObject();
		String body = "";
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			if (action.equals("body")) {
				body = apipaylods.challengeOptInPayload(id);
			} else if (action.equals("empty")) {
				logger.info("Empty Body for Challenge Opt In API");
				TestListeners.extentTest.get().info("Empty Body for Challenge Opt In API");
			}
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1ChallengeOptIn + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Authorization", "Bearer " + token);
			request.header("Accept", api1Headers.accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1ChallengeOptIn);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiUpdateUserInfoAndPassword(String client, String secret, String authToken, String email,
			String fName, String lName) {
		Response response = null;
		requestParams = new JSONObject();
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("email", email);
			hashmap.put("current_password", "password@123");
			hashmap.put("password", Utilities.getApiConfigProperty("updatedPassword"));
			hashmap.put("first_name", fName);
			hashmap.put("last_name", lName);
			hashmap.put("birthday", Utilities.getApiConfigProperty("updatedBirthday"));
			hashmap.put("anniversary", Utilities.getApiConfigProperty("updatedAnniversary"));
			requestParams.put("user", hashmap);
			requestParams.put("client", client);
			requestParams.put("authentication_token", authToken);
			param = requestParams.toString();
			String payload = ApiConstants.authUpdateUserInfo + param;
			// Headers passing
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			request.headers(defaultHeaderMap);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(requestParams.toJSONString());
			response = request.put(ApiConstants.authUpdateUserInfo);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1DriveThruShortCode(String loc_id, String client, String secret, String token, String action) {
		Response response = null;
		requestParams = new JSONObject();
		String body = "";
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			if (action.equals("body")) {
				body = apipaylods.apiV1DriveThruCodePayload(loc_id);
			} else if (action.equals("empty")) {
				logger.info("Empty Body for Drive Thru Code Generation API");
				TestListeners.extentTest.get().info("Empty Body for Drive Thru Code Generation API");
			}
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1DriveThruCode + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Authorization", "Bearer " + token);
			request.header("Accept", api1Headers.accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1DriveThruCode);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2DriveThruShortCode(String loc_id, String client, String secret, String token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.apiV2DriveThruCodePayload(loc_id, client);
			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2DriveThruCode + body;
			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.body(body);
			response = request.post(ApiConstants.mobApi2DriveThruCode);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response getInboundWebhookFilterLogs(String webhookUrl, String Cookie, String eventType,
			long fromTimeStamp) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = webhookUrl;
			RequestSpecification request = RestAssured.given().log().all();
			request.queryParam("event_type", eventType);
			request.queryParam("from_timestamp", fromTimeStamp);
			request.header("Cookie", Cookie);
			response = request.get(ApiConstants.getInboundwebhookFilterLogs);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response getInboundWebhookMessageContent(String webhookUrl, String Cookie, String contentId) {
		Response response = null;
		try {
			RestAssured.baseURI = webhookUrl;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Cookie", Cookie);
			response = request.get(ApiConstants.getWebhookMessageContent + "/" + contentId);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response updateQC(String jsonBody, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Update QC======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.body(jsonBody);
			response = request.patch(ApiConstants.createQualificationCriteria_API);
			logger.info("API Response:" + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// Update redeemable
	public Response updateRedeemable(String jsonBody, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Update Redeemable======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.body(jsonBody);
			response = request.patch(ApiConstants.api2UpdateRedeemable);
			logger.info("API Response:" + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2UpdateUserProfile2(String client, String email, String secret, String access_token,
			String existingPassword, String newPassword, String secondaryEmail, String phone) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = "";
			if (email.isEmpty() && existingPassword.isEmpty() && newPassword.isEmpty() && !secondaryEmail.isEmpty()
					&& !phone.isEmpty()) {
				body = apipaylods.api2UpdateUserSecondaryEmailPayload(client, secondaryEmail, phone);
			} else {
				body = apipaylods.API2UpdateUserProfilePayload2(client, email, existingPassword, newPassword);
			}
			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2UpdateUserInfo + body;
			request.headers(header.api2SignUpHeader(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			// request.body(requestParams.toJSONString());
			request.body(body);

			response = request.put(ApiConstants.mobApi2UpdateUserInfo);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authApiUpdateUser(String client, String secret, String accessToken, String email, String phone,
			String firstName, String lastName, String currentPassword, String newPassword) {
		Response response = null;
		requestParams = new JSONObject();
		Map<String, Object> defaultHeaderMap = new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			Map<String, Object> userData = apipaylods.authApiUpdateUserPayload(email, phone, firstName, lastName,
					currentPassword, newPassword);
			requestParams.put("user", userData);
			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.authUpdateUserInfo + param;
			String signature = apiUtils.getSignatureSHA1(secret, payload);
			defaultHeaderMap.put("Content-Type", prop.getProperty("contentType"));
			defaultHeaderMap.put("x-pch-digest", signature);
			putIfNotEmpty(defaultHeaderMap, "Authorization", "Bearer " + accessToken);
			request.headers(defaultHeaderMap);
			request.body(requestParams.toJSONString());
			response = request.put(ApiConstants.authUpdateUserInfo);
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("Error", e.getMessage());
		}
		return response;
	}

	public Response Api2AsynchronousUserUpdate(String client, String email, String secret, String access_token,
			String currentPassword, String newPassword) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2UpdateAsyncUserProfile(client, email, currentPassword, newPassword);
			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2asyncUserUpdate + body;

			// Headers
			Map<String, Object> headers = new HashMap<String, Object>();
			putIfNotEmpty(headers, "Authorization", "Bearer " + access_token);
			request.headers(headers);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);

			response = request.patch(ApiConstants.mobApi2asyncUserUpdate);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2UpdateUser(String client, String secret, String access_token, String email, String phone,
			String firstName, String lastName, String currentPassword, String newPassword) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			Map<String, Object> bodyMap = apipaylods.Api2UpdateUserPasswordPayload(client, email, phone, firstName,
					lastName, currentPassword, newPassword);
			String bodyJson = new JSONObject(bodyMap).toJSONString();
			String payload = ApiConstants.mobApi2UpdateUserInfo + bodyJson;
			request.headers(header.api2SignUpHeader(payload, secret));
			Map<String, Object> headers = new HashMap<String, Object>();
			putIfNotEmpty(headers, "Authorization", "Bearer " + access_token);
			request.headers(headers);
			request.body(bodyJson);
			response = request.put(ApiConstants.mobApi2UpdateUserInfo);
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("error", e.getMessage());
		}
		return response;
	}

	public Response Api2SecureAsynchronousUserUpdate(String client, String email, String secret, String access_token,
			String currentPassword, String newPassword) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String body = apipaylods.API2UpdateAsyncUserProfile(client, email, currentPassword, newPassword);
			String URL = ApiConstants.mobApi1SecureAsyncUserUpdate + "?client=" + client + "&hash=" + epoch;
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			String payload = URL + body;

			// Headers
			Map<String, Object> headers = new HashMap<String, Object>();
			putIfNotEmpty(headers, "Authorization", "Bearer " + access_token);
			request.headers(headers);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", "application/json");
			request.header("User-Agent", api1Headers.userAgent());
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("Content-Type", api1Headers.contentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.body(body);
			response = request.put(ApiConstants.mobApi1SecureAsyncUserUpdate);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2ChallengeOptIn(int id, String client, String secret, String token, String action) {
		Response response = null;
		requestParams = new JSONObject();
		String body = "";
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			if (action.equals("body")) {
				body = apipaylods.challengeOptInPayload(id);
			} else if (action.equals("empty")) {
				logger.info("Empty Body for Challenge Opt In API");
				TestListeners.extentTest.get().info("Empty Body for Challenge Opt In API");
			}
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi2ChallengeOptIn + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Authorization", "Bearer " + token);
			request.header("Accept", api1Headers.accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi2ChallengeOptIn);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1ChallengeOptOut(int id, String client, String secret, String token, String action) {
		Response response = null;
		requestParams = new JSONObject();
		String body = "";
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			if (action.equals("body")) {
				body = apipaylods.challengeOptInPayload(id);
			} else if (action.equals("empty")) {
				logger.info("Empty Body for Challenge Opt Out API");
				TestListeners.extentTest.get().info("Empty Body for Challenge Opt Out API");
			}
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1ChallengeOptOut + "?client=" + client + "&hash=" + epoch;
			String payload = URL + body;

			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Authorization", "Bearer " + token);
			request.header("Accept", api1Headers.accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.put(ApiConstants.mobApi1ChallengeOptOut);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api2ChallengeOptOut(String id, String client, String secret, String accessToken) {
		Response response = null;
		requestParams = new JSONObject();
		utils.logit("== Mobile API2 Challenge Opt Out ==");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("id", id);
			param = requestParams.toString();
			String payload = ApiConstants.mobApi2ChallengeOptOut + "?client=" + client + param;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Authorization", "Bearer " + accessToken);
			request.header("Accept", api2Headers.api2Accept());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			request.queryParam("client", client);
			request.body(param);
			response = request.put(ApiConstants.mobApi2ChallengeOptOut);
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("Exception: ", e.getMessage());
		}
		return response;
	}

	public Response authApiChangePasswordAdvanceAuthWithResetPasswordToken(String access_token, String client,
			String secret, String password, String resetPasswordToken) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("====== Auth Api Change Password of the user ======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			Map<String, Object> body = apipaylods.authApiChangePasswordAdvanceAuthWithResetPasswordTokenPayload(client,
					password, resetPasswordToken);
			String URL = ApiConstants.authApiChangePassword;
			String payload = URL + new ObjectMapper().writeValueAsString(body);
			request.header("Accept", "application/json");
			request.header("Content-Type", "application/json");
			request.header("x-pch-digest", authHeaders.authApiSignature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.body(body);
			response = request.patch(ApiConstants.authApiChangePassword);
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("error", e.getMessage());
		}
		return response;
	}

	public Response getWebhookRateLimitPerformanceLog(String webhookUrl, String cookie, String webhookId,
			long fromTimeStamp, long toTimeStamp) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = webhookUrl;
			RequestSpecification request = RestAssured.given().log().all();
			request.queryParam("from_timestamp", fromTimeStamp);
			request.queryParam("to_timestamp", toTimeStamp);
			request.header("Cookie", cookie);
			response = request.get(ApiConstants.getWebhookRateLimitPerformanceLog + "/" + webhookId);

			logger.info("API Response: " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());

		} catch (Exception e) {
			logger.error("Error in getWebhookRateLimitPerformanceLog : " + e.getMessage());
		}
		return response;
	}

	public Response getWebhookBarGraphLogs(String webhookUrl, String cookie, long fromTimestamp, long toTimestamp) {
		Response response = null;
		try {
			RestAssured.baseURI = webhookUrl;
			RequestSpecification request = RestAssured.given().log().all().queryParam("from_timestamp", fromTimestamp)
					.queryParam("to_timestamp", toTimestamp).header("Cookie", cookie);
			response = request.get(ApiConstants.getWebhookBarGraphLogs);

			logger.info("API Response: " + response.asString());
			TestListeners.extentTest.get().info("API Response: " + response.asString());

		} catch (Exception e) {
			logger.error("Error in getBarGraphLogs : " + e.getMessage());
		}
		return response;
	}

	public Response updateBusinessConfig(String businessEncryptedKey) {
		Response response = null;
		try {
			RequestSpecification request = RestAssured.given().log().all();
			String baseUri = getGuestIdentityBaseUrl();
			request.baseUri(baseUri);
			Map<String, Object> headers = new HashMap<String, Object>();
			putIfNotEmpty(headers, "Content-Type", "application/json");
			request.headers(headers);
			Map<String, Object> body = new HashMap<>();
			body.put("business", businessEncryptedKey);
			request.body(body);
			response = request.post(ApiConstants.updateBusinessConfig);
			utils.logit(response);
		} catch (Exception e) {
			utils.logit("error", e.getMessage());
		}
		return response;
	}

	public Response getEventFrameWorkGlobalConfig(String baseUrl, String cookie) {
		Response response = null;
		try {
			RestAssured.baseURI = baseUrl;
			RequestSpecification request = RestAssured.given().log().all().header("Cookie", cookie);
			response = request.get(ApiConstants.apiV1WebhookGlobalConfig);

			logger.info("Global Config Response: " + response.asString());
			TestListeners.extentTest.get().info("Global Config Response: " + response.asString());

		} catch (Exception e) {
			logger.error("Error while calling getGlobalConfig: " + e.getMessage());
		}
		return response;
	}

	public Response getPayloadDetailsOfWebhookEventThroughAPI(String webhookUrl, String cookie, long fromTimeStamp,
			String page, String per, String webhookID) {
		Response response = null;
		try {
			RestAssured.baseURI = webhookUrl;
			RequestSpecification request = RestAssured.given().log().all().header("Cookie", cookie);
			request.queryParam("from_timestamp", fromTimeStamp);
			request.queryParam("page", page);
			request.queryParam("per", per);
			request.queryParam("webhook_id", webhookID);
			response = request.get(ApiConstants.getWebhookLogsApi);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error("Error while calling getWebhookLogsApi : " + e.getMessage());
		}
		return response;
	}

	public Response updateUserPasswordDashboardApi(String user_id, String email, String newpassword,
			String currentPassword, String authorization) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			logger.info("======PLATFORM FUNCTIONS API Update a User======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}
			String body = apipaylods.updateUserPasswordDashboardApi(user_id, email, newpassword, currentPassword);
			request.body(body);
			response = request.patch(ApiConstants.updateUser);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
			utils.logit("Error", e.getMessage());
		}
		return response;
	}

	public Response apiGetGameUrlsCommon(String endpoint, String client, String secret, String authToken, String page,
			String perPage, String channel) {
		Response response = null;

		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String epoch = apiUtils.getSpan();

			// Build URL for signature calculation
			String url = endpoint + "?client=" + client + "&hash=" + epoch;
			if (page != null)
				url += "&page=" + page;
			if (perPage != null)
				url += "&per_page=" + perPage;
			if (channel != null)
				url += "&channel=" + channel;

			// Security Headers
			request.header("x-pch-digest", api1Headers.signature(url, secret));
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));

			// Use API1Headers methods
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept", api1Headers.accept());

			// Authorization
			if (authToken != null && !authToken.isEmpty()) {
				request.header("Authorization", "Bearer " + authToken);
			}

			// Query Params
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			if (page != null)
				request.queryParam("page", page);
			if (perPage != null)
				request.queryParam("per_page", perPage);
			if (channel != null)
				request.queryParam("channel", channel);

			// API Call
			response = request.get(endpoint);

			logger.info(response.getStatusCode() + "; " + response.asPrettyString());
			TestListeners.extentTest.get().info(response.getStatusCode() + "; " + response.asString());

		} catch (Exception e) {
			logger.error("Error in API request: " + e.getMessage(), e);
		}

		return response;
	}

	public Response apiGetGameUrls(String client, String secret, String authToken, String page, String perPage,
			String channel) {
		return apiGetGameUrlsCommon(ApiConstants.gameUrlsEndpoint, client, secret, authToken, page, perPage, channel);
	}

	public Response apiGetGameUrlsApi2(String client, String secret, String authToken, String page, String perPage,
			String channel) {
		return apiGetGameUrlsCommon(ApiConstants.mobApi2GameUrlsEndpointApi, client, secret, authToken, page, perPage,
				channel);
	}

	public Response getUnifiedTenantsAccess(String cookieHeader) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("====== Unified service users me API ======");
		try {
			String unifiedServiceBaseUrl = Utilities.getApiConfigProperty(env.toLowerCase() + ".unifiedServiceBaseUrl");
			logger.info("Unified Service base Url is ==> " + unifiedServiceBaseUrl);
			RestAssured.baseURI = unifiedServiceBaseUrl;
			RequestSpecification request = RestAssured.given().log().all();
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Cookie", cookieHeader);
			response = request.get(ApiConstants.meApi);
			logger.info("API Response of Unified service users me API is: " + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error("Error while calling Unified service users me API", e);
		}
		return response;
	}

	// State codes api
	public Response getStateCodes(String authorization) {
		Response response = null;
		try {
			logger.info("======Dashboard API Get State Codes======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			cloudflareToken(request);
			response = request.get(ApiConstants.getStateCodes);
			logger.info(response.prettyPrint());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response searchUserByNfcToken(String queryParamKey, String nfcToken, String location) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.queryParam(queryParamKey, nfcToken);
			request.queryParam("location_key", location);
			response = request.get(ApiConstants.userLookupAndFetchBalance);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authOnlineOrderForOfferModularization(String env, String authentication_token, String client,
			String secret, String body) {
		Response response = null;
		String instanceType = null;
		try {
			if (env != null && env.contains("_local")) {
				instanceType = Utilities.getInstance(env);
				String key = instanceType + ".offerservice.baseurl";

				String configuredBaseUri = configProp.getProperty(key);
				if (configuredBaseUri == null && configuredBaseUri.isEmpty()) {
					utils.logit("ERROR", "Missing or empty configuration of BaseURL for key: " + key);
					throw new IllegalStateException("Missing or empty configuration of BaseURL for key: " + key);
				}

				baseUri = configuredBaseUri;

			} else {
				instanceType = "https://dashboard";
				baseUri = baseUri.replace(instanceType, "https://offersservice");
			}

			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// String body = apipaylods.API2CreateUserRelationPayload(client);

			String payload = ApiConstants.authOnlineRedemption + body;
			// Headers
			request.header("Authorization", "Bearer " + authentication_token);
			// request.header("x-pch-digest", api2Headers.api2Signature(payload,
			// secret));getSignatureSHA1
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.body(body);
			response = request.post(ApiConstants.authOnlineRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authOnlineVoidRedemptionOfferModularization(String env, String authentication_token, String client,
			String secret, String body) {
		Response response = null;
		String instanceType = null;
		try {
			if (env != null && env.contains("_local")) {
				instanceType = Utilities.getInstance(env);
				String key = instanceType + ".offerservice.baseurl";

				String configuredBaseUri = configProp.getProperty(key);
				if (configuredBaseUri == null && configuredBaseUri.isEmpty()) {
					utils.logit("ERROR", "Missing or empty configuration of BaseURL for key: " + key);
					throw new IllegalStateException("Missing or empty configuration of BaseURL for key: " + key);
				}

				baseUri = configuredBaseUri;

			} else {
				instanceType = "https://dashboard";
				baseUri = baseUri.replace(instanceType, "https://offersservice");
			}

			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// String body = apipaylods.API2CreateUserRelationPayload(client);

			String payload = ApiConstants.authVoidRedemption + body;
			// Headers
			request.header("Authorization", "Bearer " + authentication_token);
			// request.header("x-pch-digest", api2Headers.api2Signature(payload,
			// secret));getSignatureSHA1
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.body(body);
			response = request.delete(ApiConstants.authVoidRedemption);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response userLookupPosApiWithNfc(String lookUpField, String nfcToken, String locationkey, String externalUid)
			throws InterruptedException {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.userLookupPosAPIPayloadWithNfc(lookUpField, nfcToken, externalUid);
			request.header("Content-Type", api1Headers.contentType());
			request.header("Accept-Language", api1Headers.acceptLanguage());
			request.header("Accept", api1Headers.accept());
			request.header("Authorization", "Token token=" + locationkey);
			request.body(body);
			response = request.get(ApiConstants.posUserLookup);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1facebookSignUp(String client, String accessToken, String fbUid, String secret, String email) {

		Response response = null;

		try {
			RestAssured.baseURI = baseUri;

			RequestSpecification request = RestAssured.given().log().all();

			// Request body
			JSONObject requestBody = new JSONObject();
			requestBody.put("access_token", accessToken);
			requestBody.put("fb_uid", fbUid);
			requestBody.put("email", email);

			String requestBodyToString = requestBody.toString();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.facebookSocialLogin + "?client=" + client + "&hash=" + epoch;
			String payload = URL + requestBodyToString;

			String signature = apiUtils.getSignature(secret, payload);
			String hash = apiUtils.getHash(secret + epoch);
			// Headers
			request.header("Content-Type", "application/json");
			request.header("x-pch-digest", signature);
			request.header("x-pch-hash", hash);

			request.body(requestBody.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.post(ApiConstants.facebookSocialLogin);
			logger.info("API Response:" + response.asString());
			TestListeners.extentTest.get().info("API response is : " + response.asString());
			logger.info("Response code: " + response.getStatusCode());

		} catch (Exception e) {
			logger.error("Exception while calling connectWithFacebookApi", e);
		}
		return response;
	}

	public Response segmentCreationUsingBuilderClause(String authorization, String segmentName,
			String builder_clauses) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			utils.logit("======Dashboard API for Segment Creation using builder clause ======");
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			request.headers("Authorization", "Bearer " + authorization);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept", api2Headers.api2Accept());
			String body = apipaylods.segmentCreationUsingBuilderClausePayload(segmentName, builder_clauses);
			request.body(body);
			response = request.post(ApiConstants.segmentCreationUsingBuilderClause);
			utils.logit("API Response:" + response.asString());
			utils.logit("Response code: " + response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api2facebookSignUp(String client, String token, String secret) {

		Response response = null;
		requestParams = new JSONObject();
		HashMap<String, Object> hashmap = new HashMap<String, Object>();
		utils.logInfo("api2facebookSignUp api starts");

		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			hashmap.put("signup_channel", "WebFacebook");
			hashmap.put("facebook_access_token", token);
			requestParams.put("user", hashmap);
			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.Api2facebookSocialLogin + param;
			// Headers
			request.header("Content-Type", "application/json");
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.Api2facebookSocialLogin);
			utils.logit(response);

		} catch (Exception e) {
			logger.error("Exception while calling connectWithFacebookApi", e);
		}
		return response;
	}

	public Response Api2RedemptionWithBankedCurrencyDynamic(String client, String secret, String accessToken,
			String locationId, double bankedCurrency, double latitude, double longitude, int gpsAccuracy) {

		Response response = null;

		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.API2RedemptionWithBankedCurrencyDynamic(client, locationId, bankedCurrency,
					latitude, longitude, gpsAccuracy);

			String payload = ApiConstants.mobApi2RedemptionsUsingBankedCurrency + body;

			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Authorization", "Bearer " + accessToken);
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());

			if (env.equalsIgnoreCase("qa")) {
				cloudflareToken(request);
			}

			request.body(body);
			response = request.post(ApiConstants.mobApi2RedemptionsUsingBankedCurrency);

			logger.info("API Response: " + response.asString());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}

		return response;
	}

	public Response ApiAuthFacebookSocialLogin(String client, String token, String secret, String fbUid) {

		Response response = null;
		requestParams = new JSONObject();
		utils.logInfo("ApiAuthFacebookSocialLogin api starts");

		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("client", client);
			requestParams.put("fb_uid", fbUid);
			requestParams.put("access_token", token);
			param = requestParams.toString();
			String payload = ApiConstants.ApiAuthFacebookSocialLogin + param;
			// Headers
			request.header("Content-Type", "application/json");
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.ApiAuthFacebookSocialLogin);
			utils.logit(response);

		} catch (Exception e) {
			logger.error("Exception while calling connectWithFacebookApi", e);
		}
		return response;
	}
}