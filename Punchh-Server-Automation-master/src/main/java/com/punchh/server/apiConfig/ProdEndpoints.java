package com.punchh.server.apiConfig;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

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

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import net.minidev.json.JSONObject;

@Listeners(TestListeners.class)
public class ProdEndpoints {
	static Logger logger = LogManager.getLogger(ProdEndpoints.class);
	// public WebDriver driver;
	// PageObj pageObj = new PageObj(driver);
	PageObj pageObj = new PageObj();
	ApiUtils apiUtils;
	Properties prop;
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
	String cloudflarTokeneKey = "x-px-access-token";
	String cloudflareTokenValue = "YQ9MjfGfxD4UXtof2IeSibByNcAXVYV2RgU7qHQ2elkNj3Ss3wcCU4wBTYrLwZxc";
	String cloudflareWafKey = "x-bypass-cloudflare-waf";
	String cloudflareWafValue = "u3k7gZap%Dv@rP@PWXzRCiVfXMutm$RSth5qS6IRcuLrkaSXq!dwObRjn00u5y!f";

	public String getBaseUri() {
		baseUri = pageObj.getEnvDetails().setBaseUrl();
		TestListeners.extentTest.get().info("Base Uri is :" + baseUri);
		return baseUri;
	}

	public ProdEndpoints() {
		apiUtils = new ApiUtils();
		prop = Utilities.loadPropertiesFile("apiConfig.properties");
		header = new HeaderConfig();
		utils = new Utilities();
		apipaylods = new ApiPayloads();
		authHeaders = new AuthHeaders();
		api2Headers = new Api2Headers();
		api1Headers = new API1Headers();
		authHeaders = new AuthHeaders();
	}

	public Response Api2Login(String email, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile Api2 SignIn======");
		try {
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.mobApi2Login);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2SignUp(String email, String client, String secret) {
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
			// logger.info("Email in method for API2 ==> " + hashmap);
			requestParams.put("user", hashmap);
			requestParams.put("client", client);
			param = requestParams.toString();
			// logger.info("Email in method for API2 ==> " + param);
			String payload = ApiConstants.mobApi2SignUp + param;
			// headres
			request.headers(header.api2SignUpHeader(payload, secret));
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.mobApi2SignUp);

			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
			// logger.info(response.getStatusCode());
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

			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
			// logger.info(response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api1UserSignUp(String email, String client, String secret) {
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
			requestParams.put("user", hashmap);
			param = requestParams.toString();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1SignUp + "?client=" + client + "&hash=" + epoch;
			String payload = URL + param;
			request.headers(header.api1SignUpHeader(payload, epoch, secret));
			// cloudflare token
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.post(ApiConstants.mobApi1SignUp);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			RequestSpecification request = RestAssured.given();
			HashMap<String, Object> hashmap = new HashMap<String, Object>();
			hashmap.put("email", email);
			hashmap.put("password", Utilities.getApiConfigProperty("password"));
			requestParams.put("user", hashmap);
			param = requestParams.toString();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi1Login + "?client=" + client + "&hash=" + epoch;
			String payload = URL + param;
			request.headers(header.api1LoginHeader(payload, epoch, secret));
			// cloudflare token
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.post(ApiConstants.mobApi1Login);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.posSignUp);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			request.body(body);
			response = request.post(ApiConstants.onlineOrderCheckin);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.statusCode());
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
			// cloudflare header
			request1.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request1.headers(cloudflareWafKey, cloudflareWafValue);
			request1.body(body.toString());
			response = request1.post(ApiConstants.posCheckin);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posCheckin(String email, String authorization, String amount) {
		Response response = null;
		requestParams = new JSONObject();
		punchhKey = CreateDateTime.getTimeDateString();
		try {
			// amount = Utilities.getApiConfigProperty("checkinAmount");
			RestAssured.baseURI = baseUri;
			RequestSpecification request1 = RestAssured.given();
			String body = ApiPayloads.posCheckinPayLoad(email, punchhKey, amount);
			request1.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request1.header("Authorization", "Token token=" + authorization);
			request1.body(body.toString());
			response = request1.post(ApiConstants.posCheckin);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
	 * logger.info(response.asString());
	 * TestListeners.extentTest.get().info("response is : "+ response.asString());
	 * logger.info(response.getStatusCode()); } catch (Exception e) {
	 * logger.error(e.getMessage()); } return response; }
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
			request1.header("Authorization", "Token token=" + locationKey);
			request1.body(body);
			response = request1.post(ApiConstants.posRedemption);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			request1.body(body);
			response = request1.post(ApiConstants.posRedemption);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			request1.body(body);
			response = request1.post(ApiConstants.posPossibleRedemption);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			request1.body(body);
			response = request1.delete(ApiConstants.posRedemption);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2SubscriptionPurchase(String token, String plan_id, String client, String secret,
			String purchase_price, String endDateTime) {
		Response response = null;
		// requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.api2PurchaseSubscriptionPayload(plan_id, purchase_price, endDateTime);
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.post(ApiConstants.mobApi1Redemption);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			// cloudflare token
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			// request.headers(headersMap);
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.get(ApiConstants.mobApi1accounts);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			// cloudflare token
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			// request.headers(headersMap);
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.get(ApiConstants.mobApi1usersbalance);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.authApiSignUp);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.authApiLogin);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.queryParam("client", client);
			String payload = ApiConstants.mobApi2UserOffer + "?client=" + client;
			request.headers(header.userOffersHeader(payload, secret, authorization));
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			response = request.get(ApiConstants.mobApi2UserOffer);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
			// logger.info(response.getStatusCode());
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			// cloudflare header
			request1.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request1.body(body);
			response = request1.post(ApiConstants.authOnlineRedemption);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			requestParams.put("location_key", location);
			request.body(requestParams.toJSONString());
			response = request.get(ApiConstants.userLookupAndFetchBalance);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2FetchUserInfo(String access_token, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.mobApi2FetchUserInfo + param;
			// headers
			request.headers(header.api2LoginHeader(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			request.body(requestParams.toJSONString());
			response = request.get(ApiConstants.mobApi2FetchUserInfo);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api2UserShow(String access_token, String user_id, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.mobApi2UserShow + user_id + param;
			// Headers
			request.headers(header.api2LoginHeader(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.body(requestParams.toJSONString());

			response = request.get(ApiConstants.mobApi2UserShow + user_id);
			logger.info(response.getStatusCode() + "; " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api2LifetimeStats(String client, String secret, String access_token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
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
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api2MigrationLookup(String original_membership_no, String email, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			requestParams.put("client", client);
			requestParams.put("card_number", original_membership_no);
			requestParams.put("email", email);
			param = requestParams.toString();
			String payload = ApiConstants.mobApi2MigrationLookup + param;
			// Headers
			request.headers(header.api2LoginHeader(payload, secret));
			request.body(requestParams.toJSONString());

			response = request.get(ApiConstants.mobApi2MigrationLookup);
			logger.info(response.getStatusCode() + "; " + response.asString());
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
			request.body(body);

			response = request.post(ApiConstants.mobApi2SingleScanCode);
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
			request.body(body);

			response = request.post(ApiConstants.mobApi2SingleScanCode);
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
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.get(ApiConstants.mobApi1FetchMessages);
			logger.info(response.getStatusCode() + "; " + response.asString());
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
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.api2ReloadGiftCardWithRandomAmountPayload(userEmail, client, designId,
					transactionToken, cardHolderName, expDate, cardType);
			String payload = ApiConstants.mobApi2ReloadGiftCard + uuid + "/reload" + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Accept", api2Headers.api2Accept());
			request.header("Authorization", "Bearer " + accessToken);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.body(body);
			response = request.post(ApiConstants.mobApi2ReloadGiftCard + uuid + "/reload");
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api2UserAccountDeletion(String token, String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("client", client);
			param = requestParams.toString();
			String payload = ApiConstants.mobApi2AccountDeletionRequest + param;
			request.headers(header.api2SignUpHeader(payload, secret));
			request.header("Authorization", "Bearer " + token);
			request.body(requestParams.toJSONString());
			response = request.delete(ApiConstants.mobApi2AccountDeletionRequest);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.delete(ApiConstants.mobApi1Logout);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1CreateFeedback);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.get(ApiConstants.mobApi1ScratchBoard);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1CurrencyTransfer);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.get(ApiConstants.mobApi1BraintreeToken);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1PurchaseGiftCard);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1PurchaseGiftCard);
			logger.info(response.getStatusCode() + "; " + response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.queryParam("preferred", preferred);
			response = request.put(ApiConstants.mobApi1GiftCard + giftCardUuid);
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
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.get(ApiConstants.mobApi1GiftCard + giftCardUuid + "/balance");
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
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.get(ApiConstants.mobApi1GiftCard + giftCardUuid + "/history");
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
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1GiftCard + giftCardUuid + "/reload");
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
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1GiftCard + giftCardUuid + "/reload");
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
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.get(ApiConstants.mobApi1FetchNotifications);
			logger.info(response.getStatusCode() + "; " + response.asString());
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
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.queryParam("checkin_id", checkinId);
			request.queryParam("tip", tip);
			if (requestType.equalsIgnoreCase("post")) {
				response = request.post(ApiConstants.mobApi1GiftCard + giftCardUuid + "/tip");
			} else if (requestType.equalsIgnoreCase("get")) {
				response = request.get(ApiConstants.mobApi1GiftCard + giftCardUuid + "/tip");
			}
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
			request1.body(body);
			response = request1.delete(ApiConstants.voidMultipleRedemption);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
	// logger.info(response.asString());
	// TestListeners.extentTest.get().info("response is : "+ response.asString());
	// logger.info(response.getStatusCode());
	// } catch (Exception e) {
	// logger.error(e.getMessage());
	// }
	// return response;
	// }

	public Response Api2UpdateUserProfile(String client, String email, String secret, String access_token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2UpdateUserProfilePayload(client, email);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2UpdateUserInfo + body;
			request.headers(header.api2SignUpHeader(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			// request.body(requestParams.toJSONString());
			request.body(body);

			response = request.put(ApiConstants.mobApi2UpdateUserInfo);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2UpdateUserEmailPhone(String client, String secret, String access_token, String email, String phone, String fName, String lName) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			Map<String, Object> payloadMap = apipaylods.API2UpdateUserEmailPhonePayload(client, email, phone, fName, lName);
			String body = new JSONObject(payloadMap).toString();
			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2UpdateUserInfo + body;
			request.headers(header.api2SignUpHeader(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.put(ApiConstants.mobApi2UpdateUserInfo);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2CreateUserrelation(String client, String secret, String access_token) {
		Response response = null;

		try {
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			request.body(body);
			response = request.post(ApiConstants.mobApi2CreateUserrelation);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2CreateUserrelationSpouseBirthdate(String client, String secret, String access_token,
			String birthdate) {
		Response response = null;

		try {
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2CreateUserrelationKidsBirthdate(String client, String secret, String access_token,
			String kidNane, String birthdate) {
		Response response = null;

		try {
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2UpdateUserrelation(String client, String secret, String access_token, int id) {
		Response response = null;

		try {
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			request.body(body);
			response = request.put(ApiConstants.mobApi2UpdateUserrelation + id);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2DeleteUserRelation(String client, String secret, String access_token, String id) {
		Response response = null;
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2ClientPayload(client);

			String payload = ApiConstants.mobApi2DeleteUserrelation + id + body;

			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			request.body(body);
			response = request.delete(ApiConstants.mobApi2DeleteUserrelation + id);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			requestParams.put("client", client);
			requestParams.put("access_token", access_token);

			param = requestParams.toString();
			String payload = ApiConstants.mobApi2Logout + param;
			request.headers(header.api2LoginHeader(payload, secret));
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			request.body(requestParams.toJSONString());
			response = request.delete(ApiConstants.mobApi2Logout);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			defaultHeaderMap.put(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			request.headers(defaultHeaderMap);
			request.body(requestParams.toJSONString());
			response = request.put(ApiConstants.authUpdateUserInfo);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2UpdateUserProfilePayload(client, email);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2asyncUserUpdate + body;

			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			request.body(body);

			response = request.patch(ApiConstants.mobApi2asyncUserUpdate);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			defaultHeaderMap.put(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(defaultHeaderMap);
			request.body(requestParams.toJSONString());
			response = request.get(ApiConstants.authUpdateUserInfo);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			response = request.post(ApiConstants.mobApi2Sessiontoken);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			defaultHeaderMap.put(cloudflarTokeneKey, cloudflareTokenValue);
			defaultHeaderMap.put(cloudflareWafKey, cloudflareWafValue);
			if (utils.isJwtToken(authToken))
				defaultHeaderMap.put("Authorization", "Bearer " + authToken);
			request.headers(defaultHeaderMap);
			response = request.get(ApiConstants.authAccountHistory);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			response = request.post(ApiConstants.mobApi2SendVerificationEmail);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			defaultHeaderMap.put(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(defaultHeaderMap);
			response = request.get(ApiConstants.authAccountBalance);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			request.body(body);

			response = request.post(ApiConstants.mobApi2ForgotPassword);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			defaultHeaderMap.put(cloudflarTokeneKey, cloudflareTokenValue);
			defaultHeaderMap.put(cloudflareWafKey, cloudflareWafValue);
			request.headers(defaultHeaderMap);
			response = request.get(ApiConstants.authFetchUserBalance);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			response = request.get(ApiConstants.mobApi2FetchUserBalance);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			defaultHeaderMap.put(cloudflarTokeneKey, cloudflareTokenValue);
			defaultHeaderMap.put(cloudflareWafKey, cloudflareWafValue);
			request.headers(defaultHeaderMap);
			response = request.post(ApiConstants.authForgotPassword);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			response = request.get(ApiConstants.mobApi2BalanceTimeline);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			response = request.delete(ApiConstants.authVoidRedemption);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2EstimatePointsEarning(client, access_token);
			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2EstimatePointsEarning + body;
			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			request.body(body);
			response = request.get(ApiConstants.mobApi2EstimatePointsEarning);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			response = request.get(ApiConstants.authListAvailableRewards);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			request.body(body);
			response = request.post(ApiConstants.mobApi2RedemptionsRedeemableId);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			request.body(body);
			response = request.post(ApiConstants.sendMessageToUser);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			request.body(body);
			response = request.post(ApiConstants.mobApi2RedemptionsUsingVisits);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			request.body(body);
			response = request.post(ApiConstants.mobApi2RedemptionsUsingBankedCurrency);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.body(body);
			response = request.post(ApiConstants.mobApi2RedemptionsUsingRewardId);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2GiftrewardtoUser(String client, String secret, String reward_id, String email, String token) {
		Response response = null;
		// requestParams = new JSONObject();
		try {
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
			request.queryParam("client", client);
			request.body(body);

			response = request.post(ApiConstants.mobApi2TransferReward);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			request.queryParam("client", client);
			request.body(body);

			response = request.post(ApiConstants.mobApi2TransferCurrency);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
	 * logger.info(response.asString());
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
	 * redeemable_uuid); logger.info(response.asString());
	 * TestListeners.extentTest.get().info("response is : "+ response.asString());
	 * logger.info(response.statusCode()); } catch (Exception e) {
	 * logger.error(e.getMessage()); } return response; }
	 */

	public Response Api2PointConversion(String client, String secret, String token, String conversionRuleId) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.API2PointConversionPayload(client, conversionRuleId);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2PointConversion + body;

			// Headers
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + token);
			request.header("Content-Type", api2Headers.api2ContentType());

			request.body(body);

			response = request.post(ApiConstants.mobApi2PointConversion);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			request.body(body);
			response = request.post(ApiConstants.onlineOrderCheckin);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			request.body(body);

			response = request.get(ApiConstants.mobApi2UserAccountHistory);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.statusCode());
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
			request.body(body);
			response = request.post(ApiConstants.authOnlineRedemption);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2ListAllDeals(String client, String secret, String token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
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
			response = request.get(ApiConstants.mobApi2ListAllDeals);

			logger.info(response.asPrettyString());
			Thread.sleep(20000);
			// logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			response = request.get(ApiConstants.mobApi2GetDetailsDeals + "/" + redeemable_uuid);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2LoyaltyCheckinReceiptImage(String client, String secret, String token, String locationid) {
		Response response = null;
		requestParams = new JSONObject();
		try {
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			request.body(body);
			response = request.post(ApiConstants.mobApi2CheckinReceiptImage);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			request.body(body);
			response = request.post(ApiConstants.authOnlineRedemption);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2LoyaltyCheckinBarCode(String client, String secret, String token, String barCode) {
		Response response = null;
		requestParams = new JSONObject();
		try {
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			request.body(body);
			response = request.post(ApiConstants.authOnlineRedemption);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2FetchCheckin(String client, String secret, String token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			request.body(body);
			response = request.get(ApiConstants.mobApi2FetchCheckin);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2AccountBalance(String client, String secret, String token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			request.body(body);
			response = request.get(ApiConstants.mobApi2AccountBalance);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			request.body(body);
			response = request.get(ApiConstants.mobApi2TransactionDetails);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
	// logger.info(response.asString());
	// TestListeners.extentTest.get().info("response is : "+ response.asString());
	// logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2ListOffers(String client, String secret, String token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.body(body);

			response = request.get(ApiConstants.mobApi2ListUserOffers);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.queryParam("client", client);
			request.queryParam("location_id", location_id);
			request.body(body);
			response = request.get(ApiConstants.mobApi2ListApplicableOffers);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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

			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1ReloadGiftCard + uid + "/share");
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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

			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1ReloadGiftCard + uid + "/transfer");
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			// logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			// logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			// logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			// logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
	 * logger.info(response.asString());
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
	 * logger.info(response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
		String epoch = apiUtils.getSpan();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			String payload = ApiConstants.menuorderingtoken + "?client=" + client + "&hash=" + epoch;
			request.header("x-pch-digest", api1Headers.signature(payload, secret));
			request.header("Authorization", "Bearer " + token);
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			response = request.get(ApiConstants.menuorderingtoken);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// @author=Rajasekhar added on 3/may/2024

	public Response MenuFetchUserProfile(String menuid, String menuaccesstoken) {
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
			// logger.info("Endpoint url"+complete_endpoint);
			request.header("Application", "PunchhTeamMobilePgPreproduction");
			request.header("Authorization", "Bearer" + menuaccesstoken);
			// request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			response = request.get(complete_endpoint);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
	// logger.info(response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			request.body(requestParams.toJSONString());
			response = request.get(ApiConstants.customSegments);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			requestParams.put("name", segmentName);
			requestParams.put("description", "Auto custom segment " + segmentName);
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.customSegments);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			response = request.patch(ApiConstants.customSegments);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			response = request.delete(ApiConstants.customSegments);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			response = request.get(ApiConstants.customSegments);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			requestParams.put("custom_segment_id", segmentId);
			requestParams.put("email", userEmail);
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.customSegmentMembers);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			response = request.get(ApiConstants.customSegmentMembers);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			response = request.delete(ApiConstants.customSegmentMembers);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			response = request.get(ApiConstants.wifiSignUp);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			// request.body(body);
			response = request.get(ApiConstants.mobApi1cards);
			logger.info(response.asString());
//			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2Cards(String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// String body = apipaylods.API1PurchaseGiftcard(amount);
			// String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApi2cards + "?client=" + client;
			String payload = URL;

			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.queryParam("client", client);

			// request.body(body);
			response = request.get(ApiConstants.mobApi2cards);
			logger.info(response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			// request.header("Accept", prop.getProperty("contentType"));
			// request.headers(defaultHeaderMap);
			response = request.get(ApiConstants.posActiveRedemptions);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			response = request.get(ApiConstants.mobApi1Deals);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2PostDeals(String client, String secret, String token, String redeemableUuid) {
		Response response = null;
		requestParams = new JSONObject();
		try {
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			RequestSpecification request = RestAssured.given();
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
			logger.info(response.asPrettyString());
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
			// request.body(body);
			response = request.get(ApiConstants.posLocationConfiguration);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2PurchaseGiftCardPayload(client, access_token, design_id);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2PurchaseGiftCard + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Accept", api2Headers.api2Accept());
			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Content-Type", api2Headers.api2ContentType());

			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.post(ApiConstants.mobApi2PurchaseGiftCard);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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

			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.post(ApiConstants.mobApi2PurchaseGiftCard);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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

			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.put(ApiConstants.mobApi2UpdateGiftCard + uuid);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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

			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.get(ApiConstants.mobApi2FetchGiftCard);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2OnlyClientBodyPayload(client);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2FetchGiftCardBalance + uuid + "/balance" + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Accept", api2Headers.api2Accept());
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());

			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.get(ApiConstants.mobApi2FetchGiftCardBalance + uuid + "/balance");
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2OnlyClientBodyPayload(client);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2GiftCardTransactionHistory + uuid + "/history" + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Accept", api2Headers.api2Accept());
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());

			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.get(ApiConstants.mobApi2GiftCardTransactionHistory + uuid + "/history");
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2TransferGiftCardPayload(client, email);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2TransferGiftCard + uuid + "/transfer" + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Accept", api2Headers.api2Accept());
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());

			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.post(ApiConstants.mobApi2TransferGiftCard + uuid + "/transfer");
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2ShareGiftCardPayload(client, email);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2ShareGiftCard + uuid + "/share" + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Accept", api2Headers.api2Accept());
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());

			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.post(ApiConstants.mobApi2ShareGiftCard + uuid + "/share");
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2TipGiftCardPayload(client, checkin_id);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2TipGiftCard + uuid + "/tip" + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Accept", api2Headers.api2Accept());
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());

			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.post(ApiConstants.mobApi2ShareGiftCard + uuid + "/tip");
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2OnlyClientBodyPayload(client);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2DeleteGiftCard + uuid + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());

			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.delete(ApiConstants.mobApi2FetchGiftCardBalance + uuid);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2OnlyClientBodyPayload(client);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2FetchNotifications + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.get(ApiConstants.mobApi2FetchNotifications);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2OnlyClientBodyPayload(client);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2DeleteNotifications + notification_id + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.delete(ApiConstants.mobApi2DeleteNotifications + notification_id);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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

			request.body(requestParams.toJSONString());
			response = request.get(ApiConstants.mobApi2FetchMessages);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2CreateFeedbackPayload(client);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2CreateFeedBack + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.post(ApiConstants.mobApi2CreateFeedBack);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2UpdateFeedbackPayload(client);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2UpdateFeedBack + "?feedback_id=" + feedback_id + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.post(ApiConstants.mobApi2UpdateFeedBack + "?feedback_id=" + feedback_id);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2FetchClientToken + "?client=" + client;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			response = request.get(ApiConstants.mobApi2FetchClientToken + "?client=" + client);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2ListChallenges(String client, String secret) {
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 List Challenges======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2ListChallenges + "?client=" + client;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());

			response = request.get(ApiConstants.mobApi2ListChallenges + "?client=" + client);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2OnlyClientBodyPayload(client);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2ForgotPasscode + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());

			// request.body(requestParams.toJSONString());
			request.body(body);
			response = request.post(ApiConstants.mobApi2ForgotPasscode);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2CreatePasscode + "?passcode=" + passcode + "&client=" + client;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());

			response = request.post(ApiConstants.mobApi2CreatePasscode + "?passcode=" + passcode + "&client=" + client);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2GenerateEpin + uuid + "/epin" + "?passcode=" + passcode + "&client="
					+ client;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());

			response = request.get(
					ApiConstants.mobApi2GenerateEpin + uuid + "/epin" + "?passcode=" + passcode + "&client=" + client);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.CreateGiftCardClaimToken(uuid);

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2CreateGiftCardClaimToken + "?client=" + client + body;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			request.body(body);
			response = request.post(ApiConstants.mobApi2CreateGiftCardClaimToken + "?client=" + client);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2GetInvitations + "?client=" + client;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());

			response = request.get(ApiConstants.mobApi2GetInvitations + "?client=" + client);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2StatusOfClaimToken + "?client=" + client + "&claim_token="
					+ claim_token;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);

			response = request
					.get(ApiConstants.mobApi2StatusOfClaimToken + "?client=" + client + "&claim_token=" + claim_token);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2TransferClaimToken + "?client=" + client + "&claim_token="
					+ claim_token;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);

			response = request.patch(
					ApiConstants.mobApi2TransferClaimToken + "?client=" + client + "&claim_token=" + claim_token);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2DeleteClaimToken + invitation_id + "?client=" + client;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());

			response = request.delete(ApiConstants.mobApi2DeleteClaimToken + invitation_id + "?client=" + client);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			// Headers passing Signature and content type
			String payload = ApiConstants.mobApi2SocialCauseCampaign + "/" + social_cause_id
					+ "?page=1&per_page=1&query=123456" + "&client=" + client;
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + access_token);
			request.header("Content-Type", api2Headers.api2ContentType());
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			response = request.get(ApiConstants.mobApi2SocialCauseCampaign + "/" + social_cause_id
					+ "?page=1&per_page=1&query=123456" + "&client=" + client);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			String body = apipaylods.createLocation(name, store_number);
			request.body(body);
			response = request.post(ApiConstants.createLocation);
			logger.info(response.prettyPrint());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			// String body = apipaylods.createLocation(name,store_number);
			// request.body(body);
			// param = requestParams.toString();
			response = request.get(ApiConstants.getLocation);
			logger.info(response.prettyPrint());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			String body = apipaylods.updateLocation(location_id, store_number);
			request.body(body);
			response = request.patch(ApiConstants.updateLocation);
			logger.info(response.prettyPrint());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			// String body = apipaylods.createLocation(name,store_number);
			// request.body(body);
			// param = requestParams.toString();
			response = request.get(
					ApiConstants.getLocationDetails + "?location_id=" + location_id + "&store_number=" + store_number);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			String body = apipaylods.createLocationGroup(name, store_number, location_id);
			request.body(body);
			response = request.post(ApiConstants.createLocationgroup);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			String body = apipaylods.updateLocationGroup(locationGroupName + "Updated", locationGroup_id);
			request.body(body);
			response = request.patch(ApiConstants.createLocationgroup);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			String body = apipaylods.deleteLocationGroup(locationGroup_id);
			request.body(body);
			response = request.delete(ApiConstants.createLocationgroup);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			response = request.get(ApiConstants.getLocationGroupListDetails);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			// String body = apipaylods.createLocation(name,store_number);
			// request.body(body);
			// param = requestParams.toString();
			response = request.get(ApiConstants.getLocationGroupDetails + "?location_group_id=" + location_group_id);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			String body = apipaylods.adddLocationtoGroup(location_group_id, store_number, location_id);
			request.body(body);
			// param = requestParams.toString();
			response = request.post(ApiConstants.addLocationtoGroup);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			String body = apipaylods.deleteLocationFromGroup(location_group_id, store_number, location_id);
			request.body(body);
			// param = requestParams.toString();
			response = request.delete(ApiConstants.deleteLocationFromGroup);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			String body = apipaylods.deleteLocation(location_id, store_number);
			request.body(body);
			response = request.delete(ApiConstants.deleteLocation);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			// String body = apipaylods.deleteLocation(location_id,store_number);
			// request.body(body);
			response = request.post(ApiConstants.banUser + "?reason=apitest" + "&user_id=" + user_id);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			// String body = apipaylods.deleteLocation(location_id,store_number);
			// request.body(body);
			response = request.delete(ApiConstants.unBanUser + "?reason=apitest" + "&user_id=" + user_id);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			request.body(body);
			response = request.post(ApiConstants.supportGiftingToUser);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			// String body = apipaylods.deleteLocation(location_id,store_number);
			// request.body(body);
			response = request.delete(ApiConstants.deactivateUser + "?user_id=" + user_id);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			// String body = apipaylods.deleteLocation(location_id,store_number);
			// request.body(body);
			response = request.post(ApiConstants.reactivateUser + "?user_id=" + user_id);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			String body = apipaylods.updateUser(user_id, email, location_id);
			request.body(body);
			response = request.patch(ApiConstants.updateUser);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			String body = apipaylods.userExport(user_id);
			request.body(body);
			response = request.post(ApiConstants.userExport);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			// String body = apipaylods.userExport(user_id);
			// request.body(body);
			response = request.get(ApiConstants.extendedUserHistory + "?user_id=" + user_id);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			// String body = apipaylods.userExport(user_id);
			// request.body(body);
			response = request.get(ApiConstants.fetchUserFavLocation + "?user_id=" + user_id);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			// String body = apipaylods.userExport(user_id);
			// request.body(body);
			response = request.delete(ApiConstants.deleteUserFavLocation + "?user_favourite_location_id="
					+ favourite_location_id + "&user_id=" + user_id);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			String body = apipaylods.deleteUser(user_id);
			request.body(body);
			response = request.delete(ApiConstants.deleteUser);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			String body = apipaylods.createBusinessMigrationUser(userEmail);
			request.body(body);
			response = request.post(ApiConstants.createBusinessMigrationUser);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			String body = apipaylods.updateBusinessMigrationUser(userEmail);
			request.body(body);
			response = request.patch(ApiConstants.updateBusinessMigrationUser + migration_user_id);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			// String body = apipaylods.updateBusinessMigrationUser(userEmail);
			// request.body(body);
			response = request.delete(ApiConstants.deleteBusinessMigrationUser + migration_user_id);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			// String body = apipaylods.userExport(user_id);
			// request.body(body);
			response = request.get(ApiConstants.getAdminRolesList);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			String body = apipaylods.createBusinessAdmin(userEmail, role_id);
			request.body(body);
			response = request.post(ApiConstants.createBusinessAdmin);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			String body = apipaylods.updateBusinessAdmin(business_admin_id, userEmail, role_id);
			request.body(body);
			response = request.patch(ApiConstants.updateBusinessAdmin);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			String body = apipaylods.showBusinessAdmin(business_admin_id);
			request.body(body);
			response = request.get(ApiConstants.showBusinessAdmin);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			String body = apipaylods.showBusinessAdmin(business_admin_id);
			request.body(body);
			response = request.delete(ApiConstants.deleteBusinessAdmin);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			String body = apipaylods.inviteBusinessAdmin(userEmail, role_id);
			request.body(body);
			response = request.post(ApiConstants.inviteBusinessAdmin);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			String body = apipaylods.eClubGuestUpload(userEmail, storeNumber);
			request.body(body);
			response = request.post(ApiConstants.eClubGuestUpload);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			String body = apipaylods.cReateFranchise(userEmail);
			request.body(body);
			response = request.post(ApiConstants.cReateFranchise);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			String body = apipaylods.uPdateFranchise(userEmail, franchisee_id);
			request.body(body);
			response = request.patch(ApiConstants.uPdateFranchise);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			String body = apipaylods.dEleteFranchise(franchisee_id);
			request.body(body);
			response = request.delete(ApiConstants.dEleteFranchise);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			String body = apipaylods.cReateSocialcauseCampaign(campaignName);
			request.body(body);
			response = request.post(ApiConstants.createSocialCauesCampaign);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			String body = apipaylods.activateSocialCampaign(campaigId);
			request.body(body);
			response = request.patch(ApiConstants.activateSocialCauesCampaign);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			String body = apipaylods.activateSocialCampaign(campaigId);
			request.body(body);
			response = request.patch(ApiConstants.deactivateSocialCauesCampaign);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			String body = apipaylods.enrollGuestForWifi(email, client, location_id);
			request.body(body);
			// param = requestParams.toString();
			response = request.post(ApiConstants.wifiSignUp);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.API2CreateDonation(client, social_cause_id);

			String payload = ApiConstants.mobApi2CreateDonation + body;
			// Headers
			request.header("Authorization", "Bearer " + access_token);
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("Accept", api2Headers.api2Accept());
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			request.body(body);
			response = request.post(ApiConstants.mobApi2CreateDonation);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			// request.body(body);
			response = request
					.get(ApiConstants.mobApi2SocialCauseCampaignDetails + social_cause_id + "?client=" + client);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			request.body(body);
			response = request.post(ApiConstants.dashboardAPIsendMessageToUser);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			request.body(body);
			response = request.post(ApiConstants.v5RedemptionsUsingRewardId);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			// String payload = ApiConstants.dashboardAPiRenewalSubscription + "?client=" +
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			// String payload = ApiConstants.dashboardAPiRenewalSubscription + "?client=" +
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response authListDiscountBasketAdded(String authToken, String client, String secret, String discuntType,
			String discountID) {
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			String epoch = apiUtils.getSpan();
			String hash = apiUtils.getHash(secret + epoch);
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.discountBasketItemsAttributes(discuntType, discountID);
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response addDiscountAmountToDiscountBasket(String authToken, String client, String secret,
			String discuntType, String discountID) {
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			String epoch = apiUtils.getSpan();
			String hash = apiUtils.getHash(secret + epoch);
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.discountAmountItemPayload(discuntType, discountID);
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// add discount using POS API : shashank
	public Response authListDiscountBasketAddedForPOSAPI(String locationKey, String userID, String discuntType,
			String discountID) {
		Response response = null;
		String body = "";
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			if (discuntType.equalsIgnoreCase("discount_amount")) {
				body = apipaylods.discountBasketItemsAttributesDiscountAmpountPOS(userID, discuntType, discountID);
			} else {
				body = apipaylods.discountBasketItemsAttributes(userID, discuntType, discountID);
			}
			request.header("Authorization", "Token token=" + locationKey);
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.header("Accept-Language", "es");
			request.body(body);
			response = request.post(ApiConstants.discountBasketAddedPOS);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// for AUTH API: shashank
	public Response authListDiscountBasketAddedAUTH(String authToken, String client, String secret, String discuntType,
			String discountID) {
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.discountBasketItemsAttributes(discuntType, discountID);
			String URL = ApiConstants.discountBasketAddedAUTH + "?client=" + client;
			String payload = URL + body;
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			request.header("Authorization", "Bearer " + authToken);
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.discountBasketAddedAUTH);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response addDiscountToBasketAPI2(String access_token, String client, String secret, String discuntType,
			String discountID) {
		Response response = null;
		requestParams = new JSONObject();

		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = "";
			if (discuntType.equalsIgnoreCase("discount_amount")) {
				body = apipaylods.discountAmountItemPayload(discuntType, discountID);
			} else {

				body = apipaylods.discountBasketItemsAttributes(discuntType, discountID);
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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

			request.header("Accept", "application/json");
			request.header("Content-Type", "application/json");
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			// authHeaders.authApiSignature(payload, secret));

			request.body(body);
			response = request.post(ApiConstants.authApiFetchRedemptionCode);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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

			request.body(body);
			response = request.get(ApiConstants.authApifetchAvailableOffersOfTheUser);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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

			request.headers(header.onlineOrderCheckinHeader(payload.toString(), secret));
			request.body(body);
			response = request.get(ApiConstants.authApiFetchACheckinByExternal_uid);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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

			request.body(body);
			response = request.post(ApiConstants.authApiCreateAccessToken);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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

			request.body(body);
			response = request.patch(ApiConstants.authApiChangePassword);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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

			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.authApiresetPasswordTokenOfTheUser);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			request.body(body);
			response = request.get(ApiConstants.authApiEstimateLoyaltyPointsEarning);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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

			request.header("cache-control", "no cache");
			request.header("Content-Type", "application/json");
			request.header("x-pch-digest", authHeaders.authApiSignature(payload, secret));
			request.body(body);
			response = request.post(ApiConstants.authApiPointConversionAPI);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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

			String URL = ApiConstants.authApiGetTheDealDetail;
			String payload = URL + body;

			// request.pathParam("redeemable_uuid", redeemable_uuid);
			//
			// request.queryParam("authentication_token", auth_token);
			// request.queryParam("client", client);

			request.header("Accept", "application/json");
			request.header("Accept-Language", "en");
			request.header("Content-Type", "application/json");
			request.header("x-pch-digest", authHeaders.authApiSignature(payload, secret));
			request.body(body);
			response = request.get(ApiConstants.authApiGetTheDealDetail);

			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.statusCode());
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

			request.header("Accept", "application/json");
			request.header("Content-Type", "application/json");
			request.header("x-pch-digest", authHeaders.authApiSignature(payload, secret));
			request.body(body);
			// request.body(requestParams.toJSONString());
			response = request.get(ApiConstants.authApiEstimatePointsEarning);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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

			request.body(body);
			response = request.get(ApiConstants.authApiBalanceTimelines);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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

			request.header("cache-control", "no cache");
			request.header("Content-Type", "application/json");
			request.header("x-pch-digest", authHeaders.authApiSignature(payload, secret));
			request.body(body);
			response = request.post(ApiConstants.authApiUserEnrollment);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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

			request.header("cache-control", "no cache");
			request.header("Content-Type", "application/json");
			request.header("x-pch-digest", authHeaders.authApiSignature(payload, secret));
			request.body(body);
			response = request.delete(ApiConstants.authApiUserDisenrollment);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			request.body(body);
			response = request.post(ApiConstants.v5RedemptionsUsingRewardId);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response addDiscountAmountToDiscountBasketAUTH(String authToken, String client, String secret,
			String discuntType, String discountID) {
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.discountAmountItemPayload(discuntType, discountID);
			String URL = ApiConstants.discountBasketAddedAUTH + "?client=" + client;
			String payload = URL + body;
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			request.header("Authorization", "Bearer " + authToken);
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.discountBasketAddedAUTH);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			// cloudflare header
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			request.body(body);
			response = request.post(ApiConstants.sendMessageToUser);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
				response = request
						.get(ApiConstants.dashboardAPiUsersInfo + "?phone=" + phone + "&email=" + userEmail
								+ "&user_id="
								+ userId);
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			request.formParam("amount", "3");
			request.formParam("receipt_datetime", receipt_datetime);
			request.formParam("menu_items[]", "Iced Tea|1|2|M|110002|1|38");
			request.formParam("email", email);
			request.formParam("subtotal_amount", "4");
			request.formParam("transaction_no", transaction_no);
			response = request.post(ApiConstants.V5checkins);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			// logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());

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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());

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
			request.body(body);
			response = request.post(ApiConstants.mobApi2MarkMessagesRead);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			request.body(body);
			response = request.delete(ApiConstants.mobApi2DeleteMessages + message_id);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			request.body(body);
			response = request.post(ApiConstants.mobApi2GenerateOtpToken);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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

			request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Authorization", "Token token=" + locationKey);
			request.header("Content-Type", api2Headers.api2ContentType());
			response = request.get(ApiConstants.posProgramMeta);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			request.body(body);
			response = request.post(ApiConstants.posApplicableOffers);
			logger.info(response.statusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
			logger.info("Endpoints.processBatchRedemptionOfBasketPOS() END");
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response processBatchRedemptionOfBasketWithProcessTrueAUTHAPI(String client, String secret,
			String locationKey, String authToken, String userID, String item_id) {
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.getBatchRedemptionPayloadWithProcessTrueAuth(locationKey, userID, item_id);
			String URL = ApiConstants.batchRedemptionAuthAPI + "?client=" + client;
			String payload = URL + body;
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			request.header("Authorization", "Bearer " + authToken);
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.batchRedemptionAuthAPI);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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

			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
			// logger.info(response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posSignUpwithAnniversaryForNrewardingDays(String email, String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		Date now = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		dateFormat.format(now);
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
		} catch (Exception e) {
			// TODO Auto-generated catch block
		}
		return response;
	}

	public Response identityGenerateBrandLevelToken(String client, String secret) {
		Response response = null;
		try {
			RequestSpecification requestSpec = RestAssured.given().log().everything();
			// URL
			String identityURL = Utilities.getApiConfigProperty(env.toLowerCase() + ".identityBaseUrl");
			logger.info("Identity base Url is ==> " + identityURL);
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
			logger.info(" =====  Response  ===== ");
			logger.info(response.then().log().everything());
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response identityUserSignUp(String brandLevelToken, String client, boolean includePassword,
			String punchhAppDeviceId, String userAgent) {
		Response response = null;
		try {
			RequestSpecification requestSpec = RestAssured.given().log().everything();
			// URL
			String identityURL = Utilities.getApiConfigProperty(env.toLowerCase() + ".identityBaseUrl");
			logger.info("Identity base Url is ==> " + identityURL);
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
			logger.info(" =====  Response  ===== ");
			logger.info(response.then().log().everything());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response identityUserSignOut(String brandLevelToken, String userAccessToken) {
		Response response = null;
		try {
			RequestSpecification requestSpec = RestAssured.given().log().everything();
			// URL
			String identityURL = Utilities.getApiConfigProperty(env.toLowerCase() + ".identityBaseUrl");
			logger.info("Identity base Url is ==> " + identityURL);
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
			logger.info(" =====  Response  ===== ");
			logger.info(response.then().log().everything());
		} catch (Exception e) {
			logger.error(e.getMessage());
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
			logger.info(" =====  Response  ===== ");
			logger.info(response.then().log().everything());
		} catch (Exception e) {
			logger.error(e.getMessage());
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
			logger.info(" =====  Response  ===== ");
			logger.info(response.then().log().everything());
		} catch (Exception e) {
			logger.error(e.getMessage());
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
			logger.info(" =====  Response  ===== ");
			logger.info(response.then().log().everything());
		} catch (Exception e) {
			logger.error(e.getMessage());
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
			String identityURL = Utilities.getApiConfigProperty(env.toLowerCase() + ".identityBaseUrl");
			logger.info("Identity base Url is ==> " + identityURL);
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
			logger.info(" =====  Response  ===== ");
			logger.info(response.then().log().everything());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response identityUserSync(String punchh_user_id, String email) {
		Response response = null;
		try {
			RequestSpecification requestSpec = RestAssured.given().log().everything();
			// URL
			String identityURL = Utilities.getApiConfigProperty(env.toLowerCase() + ".identityBaseUrl");
			logger.info("Identity base Url is ==> " + identityURL);
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
			logger.info(" =====  Response  ===== ");
			logger.info(response.then().log().everything());
		} catch (Exception e) {
			logger.error(e.getMessage());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.statusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response addDiscountToBasketAUTH(String authToken, String client, String secret, String discuntType,
			String discountID, String externalUid) {
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.addDiscountItemPAuthApiayload(discuntType, discountID, externalUid);
			String URL = ApiConstants.discountBasketAddedAUTH + "?client=" + client;
			String payload = URL + body;
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			request.header("Authorization", "Bearer " + authToken);
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.discountBasketAddedAUTH);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare token
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.put(ApiConstants.mobApi1UpdateGuestDetails);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get()
					.info(ApiConstants.mobApi2GiftaCard + " API response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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

			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobApi1TransferLoyaltypoints);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			response = request.get(ApiConstants.getDashboardBusinessConfig + slugId);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			String body = apipaylods.updateDashboardBusinessConfigPayloadSingleKey(res);
			request.body(body);
			response = request.put(ApiConstants.getDashboardBusinessConfig + slugId);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response closeOrderOnline(String client, String secret, String slugName, String externalUid,
			String authorization, String emailID, String storeNumber, String posRef, String oloSlug, String csID, String orderID) {
		String oloTimeStamp = CreateDateTime.getTimeDateString();
		String oloSecretKey = "et3LdDo-SvqsaGQeqICXFVoMNaJUbm5ugePTOoN65rWfmJaHrhjo34QLQfow-e8e";
		UUID uuid = UUID.randomUUID();
		Response response = null;
		requestParams = new JSONObject();
		logger.info("======Mobile API2 Purchase Gift Card======");
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String body = apipaylods.closeOrderOnlinePayload(externalUid, emailID, storeNumber, posRef, oloSlug, csID, orderID);

			String closeOrderOnineURL = baseUri + ApiConstants.closeOrderOnine.replace("$slugName", slugName);

			// Headers passing Signature and content type
			String payload = closeOrderOnineURL + "\n" + body + "\n" + uuid.toString() + "\n" + oloTimeStamp;

			String signature = apiUtils.getOloSignature(oloSecretKey, payload);

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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());

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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response addDiscountAmountToBasketAUTH(String authToken, String client, String secret, String discuntType,
			String discountID, String externalUid) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.addDiscountAmountPAuthApiayload(discuntType, discountID, externalUid);
			String URL = ApiConstants.discountBasketAddedAUTH + "?client=" + client;
			String payload = URL + body;
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			request.header("Authorization", "Bearer " + authToken);
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.discountBasketAddedAUTH);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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

	public Response itemRecSysServiceApi(String operation, String apiKey, String getItemRecsVars, String cids,
			String buuid, String uid) {
		Response response = null;
		try {
			RestAssured.baseURI = "https://itemrecsysservice.staging.punchh.io";
			RequestSpecification request = RestAssured.given().log().all();
			if (operation.equals("Get items")) {
				String eid = getItemRecsVars.split("&")[0].replace("eid=", "");
				String expVar = getItemRecsVars.split("&")[1].replace("exp_var=", "");
				request.queryParam("eid", eid);
				request.queryParam("exp_var", expVar);
			}
			request.header("apikey", apiKey);
			request.queryParam("buuid", buuid);
			request.queryParam("uid", uid);

			if (operation.equals("Pick system")) {
				response = request.get(ApiConstants.itemRecServiceApi + "/system");
			} else if (operation.equals("Get items")) {
				response = request.get(ApiConstants.itemRecServiceApi + "/items");
			}
			TestListeners.extentTest.get().info(operation + " response: " + response.asString());
			logger.info(operation + " response: " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response itemRecSysAdminApi(String apiKey, String buuid) {
		Response response = null;
		try {
			RestAssured.baseURI = "https://itemrecsysadmin.staging.punchh.io";
			RequestSpecification request = RestAssured.given().log().all();
			request.header("apikey", apiKey);
			response = request.get(ApiConstants.itemRecAdminApi + buuid + "/settings");
			TestListeners.extentTest.get().info("Get Business Settings response: " + response.asString());
			logger.info("Get Business Settings response: " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			String discuntType, String discountID, String externalUid) {
		Response response = null;
		String body = "";
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			if (discuntType.equalsIgnoreCase("discount_amount")) {
				body = apipaylods.discountBasketItemsAttributesDiscountAmpountPOS(userID, discuntType, discountID);
			} else {
				body = apipaylods.discountBasketItemsAttributesWithExt_Uid(userID, discuntType, discountID,
						externalUid);
			}
			request.header("Authorization", "Token token=" + locationKey);
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.header("Accept-Language", "es");
			request.body(body);
			response = request.post(ApiConstants.discountBasketAddedPOS);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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

			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.secureSingleScanCode);
			logger.info("********************************");
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info("*********************************");
			RestAssured.given().log().all();
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			// cloudflare token
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobAPi1ForgotPassword);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response apiV1Redemption(String client, String secret, String emailID, String subscriptionID,
			String bearerToken, String punchhAppKey, String password) {
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
			// Body
			String body = apipaylods.v1RedemptionPayload(emailID, subscriptionID);
			request.body(body);
			response = request.post(ApiConstants.v1Redemption);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			request.body(body);
			response = request.put(ApiConstants.basketUnlockPOSAPI);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.statusCode());

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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			request1.body(body);
			response = request1.post(ApiConstants.posPayment);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
		} catch (Exception e) {
			// TODO Auto-generated catch block
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
			request.body(requestParams.toJSONString());
			request.queryParam("payment_reference_id", paymentReferenceID);
			request.queryParam("location_key", locationKey);
			response = request.get(ApiConstants.posPaymentStatus);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			request1.body(body);
			response = request1.put(ApiConstants.posPayment);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			request1.body(body);
			response = request1.post(ApiConstants.posRefundPayment);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			request.queryParam("api_key", api_key);
			request.body(body);
			response = request.post();
			logger.info(response.asPrettyString());
			logger.info(response.getStatusCode());
			return response;

		} catch (Exception e) {
			logger.info("Endpoints.generateHeartlandPaymentToken()");
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response userSubcriptionForApi2(String client, String secret, String token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			// request1.body(body);
			response = request1.get(ApiConstants.posFetchAccountBalance);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2MobileDeleteVault(String client, String secret, String uuid, String token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response AuthUserSubscriptionWithPastSubscription(String token, String client, String secret) {
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
			request.queryParam("filter", "past_subscriptions");
			// request.body(body);
			response = request.get(ApiConstants.authSubscriptionUser);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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

			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobAPi1RewardGiftedForType);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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

			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			request.body(body);
			response = request.post(ApiConstants.mobAPi1GamingAchievements);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response ApiSubscriptionPurchase(String token, String plan_id, String client, String secret,
			String purchase_price, String endDateTime, String autoRenewal) {
		Response response = null;
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String epoch = apiUtils.getSpan();
			String URL = ApiConstants.mobApiSubscriptionPurchase + "?client=" + client + "&hash=" + epoch;
			String body = apipaylods.apiPurchaseSubscriptionPayload(plan_id, purchase_price, endDateTime, autoRenewal);
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
			logger.info(response.getStatusCode());
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.authSubscriptionPurchase);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;

	}

	public Response dashboardSubscriptionPurchase(String token, String plan_id, String client, String secret,
			String purchase_price, String endDateTime, String autoRenewal, String userID) {
		Response response = null;
		// requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.authPurchaseSubscriptionPayload(plan_id, purchase_price, endDateTime, autoRenewal,
					userID);
			// Headers
			request.header("Authorization", "Bearer " + token);
			// request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Content-Type", api2Headers.api2ContentType());
			// request.header("Accept-Language", api2Headers.api2acceptLanguage());
			request.header("Accept", api2Headers.api2Accept());
			// request.queryParam("client", client);
			request.body(body);
			response = request.post(ApiConstants.dashboardSubscriptionPurchase);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			response = request.get(ApiConstants.v1UserOffer);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response userInSegment(String userEmail, String authorization, String segmentId) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			// Headers passing Signature and content type
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.header("Accept", Utilities.getApiConfigProperty("contentType"));
			request.header("Authorization", "Bearer " + authorization);
			response = request.get(ApiConstants.userInSegment + segmentId + "/in_segment?guest_email=" + userEmail);

			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			response = request.get(ApiConstants.checkinsBalanceApiV1);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// cloudflare token
			request.headers(cloudflarTokeneKey, cloudflareTokenValue);
			request.headers(cloudflareWafKey, cloudflareWafValue);
			// request.headers(headersMap);
			request.body(requestParams.toJSONString());
			request.queryParam("client", client);
			request.queryParam("hash", epoch);
			response = request.get(ApiConstants.mobAPi1CheckinsBalance);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response Api2CheckinAccountBalance(String client, String secret, String access_token) {
		Response response = null;
		requestParams = new JSONObject();
		try {
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			response = request.get(ApiConstants.userMembershipLevelsApiV1);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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

	public Response fetchChallengeDetails(String client, String secret, String accessToken, String challengeID) {
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
			request.header("Accept", api1Headers.accept());
			request.header("User-Agent", api1Headers.userAgent());
			request.body(body);
			response = request.get(ApiConstants.mobApi2ListChallenges + "/" + challengeID);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response apiMobileChallenge(String client, String secret, String accessToken) {
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
			request.header("Accept-Language", "es");
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("x-pch-digest", api2Headers.api2Signature(payload, secret));
			request.header("Authorization", "Bearer " + accessToken);
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			response = request.get(ApiConstants.mobileApiChallenge);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response apiAuthChallenge(String client, String secret, String authenticationToken) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String payload = ApiConstants.apiAuthChallenge + "?client=" + client + "&authentication_token="
					+ authenticationToken;
			request.header("Accept", api1Headers.accept());
			request.header("Accept-Language", "es");
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			response = request.get(ApiConstants.apiAuthChallenge + "?client=" + client + "&authentication_token="
					+ authenticationToken);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
			response = request.get(ApiConstants.apiAuthChallenge);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response api1fetchChallengeDetails(String client, String secret, String accessToken, String challengeID) {
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
			request.header("User-Agent", api1Headers.userAgent());
			request.header("Authorization", "Bearer " + accessToken);
			request.header("x-pch-hash", api1Headers.hash(secret, epoch));
			// request.body(body);
			response = request
					.get(ApiConstants.mobileApiChallenge + "/" + challengeID + "?client=" + client + "&hash=" + epoch);
			logger.info(response.asPrettyString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response apiAuthChallengeDetails(String client, String secret, String authenticationToken,
			String challengeID) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			String payload = ApiConstants.apiAuthChallenge + "/" + challengeID + "?client=" + client
					+ "&authentication_token=" + authenticationToken;
			request.header("Accept", api1Headers.accept());
			request.header("Accept-Language", "es");
			request.header("Content-Type", api2Headers.api2ContentType());
			request.header("x-pch-digest", apiUtils.getSignatureSHA1(secret, payload));
			response = request.get(ApiConstants.apiAuthChallenge + "/" + challengeID + "?client=" + client
					+ "&authentication_token=" + authenticationToken);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response apiPosChallenge(String challengeID, String email, String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			response = request.get(ApiConstants.apiPosChallenge + "/" + challengeID + "?email=" + email
					+ "&location_key=" + locationKey);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response addBulkUserToCustomSegment(String name, int customSegmentId, String csvPath, String client,
			String authorization, String secret) {
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response removeBulkUserfromCustomSegment(String name, int customSegmentId, String csvPath, String client,
			String authorization, String secret) {
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());

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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			// request.queryParam("client", client);
			request.body(body);
			response = request.get(ApiConstants.mobApi2PurchaseSubscription);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			request.body(body);
			response = request.put(ApiConstants.mobApi2MarkOfferAsRead);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	public Response posVoidRedemptionPolling(String email, String redemption_id, String locationKey) {
		Response response = null;
		requestParams = new JSONObject();
		int attempts = 0;
		while (attempts < 10) {
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
		logger.info(response.asString());
		TestListeners.extentTest.get().info("response is : " + response.asString());
		logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());

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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());

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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}

	// add discount using POS API : shashank
	public Response authListDiscountBasketAddedForPOSAPIWithLanguage(String locationKey, String userID,
			String discuntType, String discountID, String langName) {
		Response response = null;
		String body = "";
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();
			if (discuntType.equalsIgnoreCase("discount_amount")) {
				body = apipaylods.discountBasketItemsAttributesDiscountAmpountPOS(userID, discuntType, discountID);
			} else {
				body = apipaylods.discountBasketItemsAttributes(userID, discuntType, discountID);
			}
			request.header("Authorization", "Token token=" + locationKey);
			request.header("Content-Type", "application/json");
			request.header("Accept", "application/json");
			request.header("Accept-Language", langName);
			request.body(body);
			response = request.post(ApiConstants.discountBasketAddedPOS);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			String discuntType, String discountID, String languageName) {
		Response response = null;
		requestParams = new JSONObject();
		new HashMap<String, Object>();
		try {
			RestAssured.baseURI = baseUri;
			RequestSpecification request = RestAssured.given().log().all();

			String body = apipaylods.discountBasketItemsAttributes(discuntType, discountID);
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			String identityURL = Utilities.getApiConfigProperty(env.toLowerCase() + ".identityBaseUrl");
			logger.info("Identity base Url is ==> " + identityURL);
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
			logger.info(" =====  Response  ===== ");
			logger.info(response.then().log().everything());
		} catch (Exception e) {
			logger.error(e.getMessage());
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

			if (email != null) {
				requestParams.put("email", email);
			}
			if (phoneNumber != null) {
				requestParams.put("phone", phoneNumber);
			}

			requestParams.put("password", Utilities.getApiConfigProperty("password"));
			requestParams.put("first_name",
					CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("firstName")));
			requestParams.put("last_name", CreateDateTime.getUniqueString(Utilities.getApiConfigProperty("lastName")));
			requestParams.put("zip_code", Utilities.getApiConfigProperty("zipCode"));
			request.queryParam("location_key", locationKey);
			request.header("Content-Type", Utilities.getApiConfigProperty("contentType"));
			request.body(requestParams.toJSONString());
			response = request.post(ApiConstants.posSignUp);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
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
			int modifire_max_discount_units, String modifire_processing_method) {
		String lisPayload = LineItemSelectorsJsonCreation.createLisJson(lisName, external_id, filter_item_set,
				baseItemClausesList, modifireItemClausesList, modifire_max_discount_units, modifire_processing_method);
		Response response = null;
		RestAssured.baseURI = baseUri;
		RequestSpecification request = RestAssured.given().log().all();
		request.header("Content-Type", "application/json");
		request.header("Authorization", "Bearer " + adminKey);
		request.body(lisPayload.toString());
		response = request.post(ApiConstants.createLisAPI);
		logger.info(response.asString());
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
		logger.info(response.asString());
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
			request.body(body.toString());
			response = request.post(ApiConstants.apiV1SserSignup);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			request.body(body.toString());
			response = request.post(ApiConstants.apiV1UserLogin);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
		logger.info(response.asString());
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
			// request.body(body.toString());
			response = request.get(ApiConstants.apiV1Userbalance);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
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
			response = request.get(ApiConstants.apiV1GetRichMessages);
			logger.info(response.asString());
			TestListeners.extentTest.get().info("response is : " + response.asString());
			logger.info(response.getStatusCode());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return response;
	}
}
