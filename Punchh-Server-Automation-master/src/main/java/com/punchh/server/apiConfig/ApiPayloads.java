
package com.punchh.server.apiConfig;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import com.punchh.server.pages.InstanceDashboard;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.Utilities;

import net.minidev.json.JSONObject;

public class ApiPayloads {

	ApiUtils apiUtils;
	static Properties prop = Utilities.loadPropertiesFile("apiConfig.properties");
	static Logger logger = LogManager.getLogger(InstanceDashboard.class);

	public static String posCheckinPayLoad(String email, String punchhKey, String amount) {

		String transactionNumber = CreateDateTime.getRandomNumberSixDigit() + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		String payload = "{\n" + "     \"menu_items\":\n" + "    [\n" + "        {\n"
				+ "                \"item_name\":\"Margherita\",\n" + "                \"item_qty\":1,\n"
				+ "                \"item_amount\":100,\n" + "                \"menu_item_type\":\"M\",\n"
				+ "                \"menu_item_id\":\"110\",\n" + "                \"menu_family\":\"106\",\n"
				+ "                \"menu_major_group\":\"100\",\n" + "                \"serial_number\":1.0\n"
				+ "        },\n" + "        {\n" + "                \"item_name\":\"Pizza\",\n"
				+ "                \"item_qty\":1,\n" + "                \"item_amount\":110,\n"
				+ "                \"menu_item_type\":\"M\",\n" + "                \"menu_item_id\":\"111\",\n"
				+ "                \"menu_family\":\"106\",\n" + "                \"menu_major_group\":\"100\",\n"
				+ "                \"serial_number\":1.0\n" + "        }\n" + "    ],\n"
				+ "    \"receipt_datetime\": \"" + date + "\",\n" + "    \"subtotal_amount\": " + amount + ",\n"
				+ "    \"receipt_amount\": " + amount + ",\n" + "    \"email\":\"" + email + "\",\n"
				+ "    \"punchh_key\":\"" + punchhKey + "\",\n" + "    \"transaction_no\":\"" + "12345"
				+ transactionNumber + "\"" + "\n" + "}";
		return payload;
	}

	public static String posCheckinPhonePayLoad(String phone, String punchhKey, String amount) {

		String transactionNumber = CreateDateTime.getRandomNumberSixDigit() + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		String payload = "{\n" + "     \"menu_items\":\n" + "    [\n" + "        {\n"
				+ "                \"item_name\":\"Margherita\",\n" + "                \"item_qty\":1,\n"
				+ "                \"item_amount\":100,\n" + "                \"menu_item_type\":\"M\",\n"
				+ "                \"menu_item_id\":\"110\",\n" + "                \"menu_family\":\"106\",\n"
				+ "                \"menu_major_group\":\"100\",\n" + "                \"serial_number\":1.0\n"
				+ "        },\n" + "        {\n" + "                \"item_name\":\"Pizza\",\n"
				+ "                \"item_qty\":1,\n" + "                \"item_amount\":110,\n"
				+ "                \"menu_item_type\":\"M\",\n" + "                \"menu_item_id\":\"111\",\n"
				+ "                \"menu_family\":\"106\",\n" + "                \"menu_major_group\":\"100\",\n"
				+ "                \"serial_number\":1.0\n" + "        }\n" + "    ],\n"
				+ "    \"receipt_datetime\": \"" + date + "\",\n" + "    \"subtotal_amount\": " + amount + ",\n"
				+ "    \"receipt_amount\": " + amount + ",\n" + "    \"phone\":\"" + phone + "\",\n"
				+ "    \"punchh_key\":\"" + punchhKey + "\",\n" + "    \"transaction_no\":\"" + "12345"
				+ transactionNumber + "\"" + "\n" + "}";
		return payload;
	}

	public static String onlineCheckinPayLoad(String authenticationToken, String amount, String client,
			String transactionNumber, String externalUid) {
		Calendar cal = Calendar.getInstance();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		cal.add(Calendar.DATE, 0);
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		String payload = "{\n" + "	\"authentication_token\": \"" + authenticationToken + "\",\n"
				+ "	\"receipt_amount\": " + amount + ",\n" + "	\"cc_last4\": 4389,\n" + "	\"menu_items\": [{\n"
				+ "			\"item_name\": \"White rice\",\n" + "			\"item_qty\": 1,\n"
				+ "			\"item_amount\": 100,\n" + "			\"menu_item_type\": \"M\",\n"
				+ "			\"menu_item_id\": 110,\n" + "			\"menu_family\": \"106\",\n"
				+ "			\"menu_major_group\": \"100\",\n" + "			\"serial_number\": \"1.0\"\n"
				+ "		},\n" + "		{\n" + "			\"item_name\": \"Brown rice\",\n"
				+ "			\"item_qty\": 1,\n" + "			\"item_amount\": 7.86,\n"
				+ "			\"menu_item_type\": \"M\",\n" + "			\"menu_item_id\": 111,\n"
				+ "			\"menu_family\": \"106\",\n" + "			\"menu_major_group\": \"100\",\n"
				+ "			\"serial_number\": \"2.0\"\n" + "		},\n" + "		{\n"
				+ "			\"item_name\": \"Free rice\",\n" + "			\"item_qty\": 1,\n"
				+ "			\"item_amount\": 100,\n" + "			\"menu_item_type\": \"D\",\n"
				+ "			\"menu_item_id\": 3419,\n" + "			\"menu_family\": \"800\",\n"
				+ "			\"menu_major_group\": \"152\",\n" + "			\"serial_number\": \"3.0\"\n" + "		}\n"
				+ "	],\n" + "	\"subtotal_amount\": " + amount + ",\n" + "	\"receipt_datetime\": \"" + recieptDateTime
				+ "\",\n" + "	\"transaction_no\": \"" + transactionNumber + "\",\n" + "	\"external_uid\": \""
				+ externalUid + "\",\n" + "	\"client\": \"" + client + "\",\n" + "	\"channel\": \"online_order\""

				+ "}";
		return payload;
	}

	public static String authOnlineCheckinPayLoad(String authenticationToken, String amount, String client, String txn,
			String externalUid, String recieptDateTime) {
		String payload = "{\n" + "	\"authentication_token\": \"" + authenticationToken + "\",\n"
				+ "	\"receipt_amount\": " + amount + ",\n" + "	\"cc_last4\": 4389,\n" + "	\"menu_items\": [{\n"
				+ "			\"item_name\": \"White rice\",\n" + "			\"item_qty\": 1,\n"
				+ "			\"item_amount\": 20,\n" + "			\"menu_item_type\": \"M\",\n"
				+ "			\"menu_item_id\": 110,\n" + "			\"menu_family\": \"106\",\n"
				+ "			\"menu_major_group\": \"100\",\n" + "			\"serial_number\": \"1.0\"\n"
				+ "		},\n" + "		{\n" + "			\"item_name\": \"Brown rice\",\n"
				+ "			\"item_qty\": 1,\n" + "			\"item_amount\": 20,\n"
				+ "			\"menu_item_type\": \"M\",\n" + "			\"menu_item_id\": 111,\n"
				+ "			\"menu_family\": \"106\",\n" + "			\"menu_major_group\": \"100\",\n"
				+ "			\"serial_number\": \"2.0\"\n" + "		},\n" + "		{\n"
				+ "			\"item_name\": \"Free rice\",\n" + "			\"item_qty\": 1,\n"
				+ "			\"item_amount\": 20,\n" + "			\"menu_item_type\": \"D\",\n"
				+ "			\"menu_item_id\": 3419,\n" + "			\"menu_family\": \"800\",\n"
				+ "			\"menu_major_group\": \"152\",\n" + "			\"serial_number\": \"3.0\"\n" + "		}\n"
				+ "	],\n" + "	\"subtotal_amount\": " + amount + ",\n" + "	\"receipt_datetime\": \"" + recieptDateTime
				+ "\",\n" + "	\"transaction_no\": \"" + txn + "\",\n" + "	\"external_uid\": \"" + externalUid
				+ "\",\n" + "	\"client\": \"" + client + "\",\n" + "	\"channel\": \"online_order\"\n" + "}";
		return payload;
	}

	public String authOnlineVoidCheckin(String authenticationToken, String client, String externalUid) {

		String payload = "{\n" + "    \"client\":\"" + client + "\",\n" + "    \"authentication_token\":\""
				+ authenticationToken + "\",\n" + "    \"external_uid\":\"" + externalUid + "\"\n" + "}";
		return payload;
	}

	public String posCheckinPayload(String date, String email, String key, String txn_no) {

		String payload = "{" +

				"\"menu_items\": [" + "{" + "\"item_name\":\"Margherita\"," + "\"item_qty\":\"1\","
				+ "\"item_amount\":10," + "\"menu_item_type\":\"M\"," + "\"menu_item_id\":\"123\","
				+ "\"menu_family\":\"106\"," + "\"menu_major_group\":\"100\"," + "\"serial_number\":\"1.0\"}," +

				"{" + "\"item_name\":\"Pizza\"," + "\"item_qty\":\"1\"," + "\"item_amount\":10,"
				+ "\"menu_item_type\":\"M\"," + "\"menu_item_id\":\"124\"," + "\"menu_family\":\"108\","
				+ "\"menu_major_group\":\"101\"," + "\"serial_number\":\"2.0\"}" +

				"]," +

				"\"receipt_datetime\": \"" + date + "\"," + "\"subtotal_amount\": 20," + "\"receipt_amount\": 20,"
				+ "\"email\":\"" + email + "\"," + "\"punchh_key\": \"" + key + "\"," + "\"transaction_no\": \""
				+ txn_no + "\"" + "}";

		return payload;

	}

	public String posCheckinQCPayload(String date, String email, String key, String txn_no, String location_key,
			String menu_item_id) {

		double amount = CreateDateTime.getRnadomAmountInTwoGivenNumbers(10, 20);

		String payload = "{\n" + "    \"location_key\": \"" + location_key + "\",\n" + "    \"menu_items\": [\n"
				+ "        {\n" + "            \"item_name\": \"Whiterice\",\n" + "            \"item_qty\": 1,\n"
				+ "            \"item_amount\": 10.0,\n" + "            \"menu_item_type\": \"M\",\n"
				+ "            \"menu_item_id\": \"" + menu_item_id + "\",\n"
				+ "            \"menu_family\": \"800\",\n" + "            \"menu_major_group\": \"152\",\n"
				+ "            \"serial_number\": \"1.0\"\n" + "        }\n" + "    ],\n"
				+ "    \"receipt_datetime\": \"" + date + "\",\n" + "    \"subtotal_amount\": " + amount + ",\n"
				+ "    \"receipt_amount\": " + amount + ",\n" + "    \"email\": \"" + email + "\",\n"
				+ "    \"punchh_key\": \"" + key + "\",\n" + "    \"transaction_no\": \"" + txn_no + "\"\n" + "}";

		return payload;

	}

	public static String posCheckinPayLoad(String email) {
		String punchhKey = CreateDateTime.getTimeDateString();
		String transactionNumber = CreateDateTime.getTimeDateString();
		String payload = "{\n" + "     \"menu_items\":\n" + "    [\n" + "        {\n"
				+ "                \"item_name\":\"Margherita\",\n" + "                \"item_qty\":1,\n"
				+ "                \"item_amount\":10,\n" + "                \"menu_item_type\":\"M\",\n"
				+ "                \"menu_item_id\":\"110\",\n" + "                \"menu_family\":\"106\",\n"
				+ "                \"menu_major_group\":\"100\",\n" + "                \"serial_number\":1.0\n"
				+ "        },\n" + "        {\n" + "                \"item_name\":\"Pizza\",\n"
				+ "                \"item_qty\":1,\n" + "                \"item_amount\":11,\n"
				+ "                \"menu_item_type\":\"M\",\n" + "                \"menu_item_id\":\"111\",\n"
				+ "                \"menu_family\":\"106\",\n" + "                \"menu_major_group\":\"100\",\n"
				+ "                \"serial_number\":1.0\n" + "        }\n" + "    ],\n"
				+ "    \"receipt_datetime\": \"2021-07-12T10:50:00+05:30\",\n" + "    \"subtotal_amount\": 11,\n"
				+ "    \"receipt_amount\": 11,\n" + "    \"email\":\"" + email + "\",\n" + "    \"punchh_key\":\""
				+ punchhKey + "\",\n" + "    \"transaction_no\":\"" + "12345" + transactionNumber + "\"" + "\n" + "}";
		return payload;
	}

	public String posRedemptionofCodePayload(String email, String date, String redemption_code, String punchhKey,
			String txn_no) {

		String payload = "{\n" + "    \"email\": \"" + email + "\",\n" + "    \"receipt_amount\":12.00,\n"
				+ "    \"subtotal_amount\":12.00,\n" + "    \"receipt_datetime\":\"" + date + "\",\n"
				+ "    \"sequence_no\":\"2191\",\n" + "    \"discount_type\": \"redemption_code\",\n"
				+ "    \"redemption_code\": \"" + redemption_code + "\",\n" + "    \"punchh_key\":\"" + punchhKey
				+ "\",\n" + "    \"transaction_no\":\"" + txn_no + "\",\n" + "    \"menu_items\":\n" + "       [\n"
				+ "        	{\n" + "                \"item_name\":\"Philly Cheesesteak Omelette\",\n"
				+ "                \"item_qty\":1,\n" + "                \"item_amount\": 12.00,\n"
				+ "                \"menu_item_type\":\"M\",\n" + "                \"menu_item_id\":\"110011\",\n"
				+ "                \"menu_family\":\"106\",\n" + "                \"menu_major_group\":\"100\",\n"
				+ "                \"serial_number\":1.0\n" + "        	}\n" + "       ]\n" + "}";

		return payload;

	}

	public String posRedemptionofSubscriptionPayload(String email, String date, String subscription_id,
			String punchhKey, String txn_no, String amount, String itemID) {

		String payload = "{\n" + "    \"email\": \"" + email + "\",\n" + "    \"receipt_amount\": " + amount + ",\n"
				+ "    \"subtotal_amount\": " + amount + ",\n" + "    \"receipt_datetime\":\"" + date + "\",\n"
				+ "    \"sequence_no\": \"2191\",\n" + "    \"discount_type\": \"subscription\",\n"
				+ "    \"subscription_id\": " + subscription_id + ",\n" + "    \"punchh_key\":\"" + punchhKey + "\",\n"
				+ "    \"transaction_no\":\"" + txn_no + "\",\n" + "    \"menu_items\": [\n" + "         {\n"
				+ "      \"item_name\": \"Margherita\",\n" + "      \"item_qty\": 1,\n" + "      \"item_amount\": "
				+ amount + ",\n" + "      \"menu_item_type\": \"M\",\n" + "      \"menu_item_id\": " + itemID + ",\n"
				+ "      \"menu_family\": \"106\",\n" + "      \"menu_major_group\": \"100\",\n"
				+ "      \"serial_number\": \"1.0\"\n" + "    }\n" + "    ]\n" + "}";

		return payload;

	}

	public String posRedemptionofFreeGiftPayload(String email, String date, String redemption_code, String punchhKey,
			String txn_no) {

		String payload = "{\n" + "    \"email\": \"" + email + "\",\n" + "    \"receipt_amount\":12.00,\n"
				+ "    \"subtotal_amount\":12.00,\n" + "    \"receipt_datetime\":\"" + date + "\",\n"
				+ "    \"sequence_no\":\"2191\",\n" + "    \"discount_type\": \"redemption_code\",\n"
				+ "    \"redemption_code\": \"" + redemption_code + "\",\n" + "    \"punchh_key\":\"" + punchhKey
				+ "\",\n" + "    \"transaction_no\":\"" + txn_no + "\",\n" + "    \"menu_items\": [\n" + "        {\n"
				+ "            \"item_name\": \"Burger\",\n" + "            \"item_qty\": 1,\n"
				+ "            \"item_amount\": 10.00,\n" + "            \"menu_item_type\": \"M\",\n"
				+ "            \"menu_item_id\": \"1002\",\n" + "            \"menu_family\": \"100\",\n"
				+ "            \"menu_major_group\": \"100\",\n" + "            \"serial_number\": 1.0\n"
				+ "        },\n" + "        {\n" + "            \"item_name\": \"Coffee\",\n"
				+ "            \"item_qty\": 1,\n" + "            \"item_amount\": 2.00,\n"
				+ "            \"menu_item_type\": \"M\",\n" + "            \"menu_item_id\": \"90022\",\n"
				+ "            \"menu_family\": \"106\",\n" + "            \"menu_major_group\": \"100\",\n"
				+ "            \"serial_number\": 1.0\n" + "        }\n" + "    ]\n" + "}";
		return payload;

	}

	public String posRedemptionofBogoPayload(String email, String date, String redemption_code, String punchhKey,
			String txn_no) {

		String payload = "{\n" + "    \"email\": \"" + email + "\",\n" + "    \"receipt_amount\":16.00,\n"
				+ "    \"subtotal_amount\":16.00,\n" + "    \"receipt_datetime\":\"" + date + "\",\n"
				+ "    \"sequence_no\":\"2191\",\n" + "    \"discount_type\": \"redemption_code\",\n"
				+ "    \"redemption_code\": \"" + redemption_code + "\",\n" + "    \"punchh_key\":\"" + punchhKey
				+ "\",\n" + "    \"transaction_no\":\"" + txn_no + "\",\n" + "    \"menu_items\": [\n" + "        {\n"
				+ "            \"item_name\": \"Burger\",\n" + "            \"item_qty\": 1,\n"
				+ "            \"item_amount\": 6.00,\n" + "            \"menu_item_type\": \"M\",\n"
				+ "            \"menu_item_id\": \"1002\",\n" + "            \"menu_family\": \"100\",\n"
				+ "            \"menu_major_group\": \"100\",\n" + "            \"serial_number\": 1.0\n"
				+ "        },\n" + "         {\n" + "            \"item_name\": \"Sandwich\",\n"
				+ "            \"item_qty\": 2,\n" + "            \"item_amount\": 10.00,\n"
				+ "            \"menu_item_type\": \"M\",\n" + "            \"menu_item_id\": \"2002\",\n"
				+ "            \"menu_family\": \"100\",\n" + "            \"menu_major_group\": \"100\",\n"
				+ "            \"serial_number\": 1.0\n" + "        }\n" + "    ]\n" + "}";

		return payload;

	}

	public String posRedemptionofPromotionaltypeRedeemable(String email, String date, String redemption_code,
			String punchhKey, String txn_no) {

		String payload = "{\n" + "    \"email\": \"" + email + "\",\n" + "    \"receipt_amount\":16.5,\n"
				+ "    \"subtotal_amount\":16.5,\n" + "    \"receipt_datetime\":\"" + date + "\",\n"
				+ "    \"sequence_no\":\"2191\",\n" + "    \"discount_type\": \"redemption_code\",\n"
				+ "    \"redemption_code\": \"" + redemption_code + "\",\n" + "    \"punchh_key\":\"" + punchhKey
				+ "\",\n" + "    \"transaction_no\":\"" + txn_no + "\",\n" + "    \"menu_items\": [\n" + "         {\n"
				+ "      \"item_name\": \"Whiterice\",\n" + "      \"item_qty\": 1,\n" + "      \"item_amount\": 6.5,\n"
				+ "      \"menu_item_type\": \"M\",\n" + "      \"menu_item_id\": 12,\n"
				+ "      \"menu_family\": \"800\",\n" + "      \"menu_major_group\": \"152\",\n"
				+ "      \"serial_number\": \"1.0\"\n" + "    },\n" + "    {\n"
				+ "      \"item_name\": \"Brownrice\",\n" + "      \"item_qty\": 2,\n" + "      \"item_amount\": 10,\n"
				+ "      \"menu_item_type\": \"M\",\n" + "      \"menu_item_id\": 3418,\n"
				+ "      \"menu_family\": \"800\",\n" + "      \"menu_major_group\": \"152\",\n"
				+ "      \"serial_number\": \"2.0\"\n" + "    }\n" + "    ]\n" + "}";

		return payload;

	}

	public String posRedemptionofRedeemablePayload(String email, String date, String punchhKey, String txn_no,
			String redeemable_id, String item_id) {
		String payload = "{\n" + "    \"email\": \"" + email + "\",\n" + "    \"receipt_amount\":10.00,\n"
				+ "    \"subtotal_amount\":10.00,\n" + "    \"receipt_datetime\":\"" + date + "\",\n"
				+ "    \"sequence_no\":\"2192\",\n" + "    \"discount_type\": \"redeemable\",\n"
				+ "    \"redeemable_id\": \"" + redeemable_id + "\",\n" + "    \"punchh_key\":\"" + punchhKey + "\",\n"
				+ "    \"transaction_no\":\"" + txn_no + "\",\n" + "    \"menu_items\":\n" + "       [\n"
				+ "        	{\n" + "                \"item_name\":\"Philly Cheesesteak Omelette\",\n"
				+ "                \"item_qty\":1,\n" + "                \"item_amount\": 10.00,\n"
				+ "                \"menu_item_type\":\"M\",\n" + "                \"menu_item_id\":\"" + item_id
				+ "\",\n" + "                \"menu_family\":\"106\",\n"
				+ "                \"menu_major_group\":\"100\",\n" + "                \"serial_number\":1.0\n"
				+ "        	}\n" + "       ]\n" + "}";
		return payload;
	}

	public String posRedemptionofAmountPayload(String email, String date, String punchhKey, String txn_no,
			String redeemAmount) {

		String payload = "{\n" + "    \"email\": \"" + email + "\",\n" + "    \"receipt_amount\": 10.00,\n"
				+ "    \"subtotal_amount\": 10.00,\n" + "    \"receipt_datetime\": \"" + date + "\",\n"
				+ "    \"sequence_no\": \"2191\",\n" + "    \"discount_type\": \"discount_amount\",\n"
				+ "    \"redeemed_points\": " + redeemAmount + ",\n" + "    \"punchh_key\": \"" + punchhKey + "\",\n"
				+ "    \"transaction_no\": \"" + txn_no + "\",\n" + "    \"menu_items\": [\n" + "        {\n"
				+ "            \"item_name\": \"Philly Cheesesteak Omelette\",\n" + "            \"item_qty\": 1,\n"
				+ "            \"item_amount\": 10.00,\n" + "            \"menu_item_type\": \"M\",\n"
				+ "            \"menu_item_id\": \"101\",\n" + "            \"menu_family\": \"106\",\n"
				+ "            \"menu_major_group\": \"100\",\n" + "            \"serial_number\": 1.0\n"
				+ "        }\n" + "    ]\n" + "}";

		return payload;

	}

	public String posRedemptionofCardPayload(String email, String date, String punchhKey, String txn_no) {

		String payload = "{\n" + "    \"email\": \"" + email + "\",\n" + "    \"receipt_amount\": 15.00,\n"
				+ "    \"subtotal_amount\": 15.00,\n" + "    \"receipt_datetime\": \"" + date + "\",\n"
				+ "    \"sequence_no\": \"2195\",\n" + "    \"discount_type\": \"card_completion\",\n"
				+ "    \"punchh_key\": \"" + punchhKey + "\",\n" + "    \"transaction_no\": \"" + txn_no + "\",\n"
				+ "    \"menu_items\": [\n" + "        {\n"
				+ "            \"item_name\": \"Philly Cheesesteak Omelette\",\n" + "            \"item_qty\": 1,\n"
				+ "            \"item_amount\": 15.00,\n" + "            \"menu_item_type\": \"M\",\n"
				+ "            \"menu_item_id\": \"101\",\n" + "            \"menu_family\": \"106\",\n"
				+ "            \"menu_major_group\": \"100\",\n" + "            \"serial_number\": 1.0\n"
				+ "        }\n" + "    ]\n" + "}\n" + "";

		return payload;

	}

	public String posRedemptionofRewardPayload(String email, String date, String punchhKey, String txn_no,
			String locationKey, String rewardId) {

		String payload = "{\n" + "    \"location_key\": \"" + locationKey + "\",\n" + "    \"email\": \"" + email
				+ "\",\n" + "    \"receipt_amount\": 10.00,\n" + "    \"subtotal_amount\": 10.00,\n"
				+ "    \"receipt_datetime\": \"" + date + "\",\n" + "    \"sequence_no\": \"2191\",\n"
				+ "    \"discount_type\": \"reward\",\n" + "    \"reward_id\": \"" + rewardId + "\",\n"
				+ "    \"redeemed_points\": 40,\n" + "    \"punchh_key\": \"" + punchhKey + "\",\n"
				+ "    \"transaction_no\": \"" + txn_no + "\",\n" + "    \"menu_items\": [\n" + "        {\n"
				+ "            \"item_name\": \"Philly Cheesesteak Omelette\",\n" + "            \"item_qty\": 1,\n"
				+ "            \"item_amount\": 10.00,\n" + "            \"menu_item_type\": \"M\",\n"
				+ "            \"menu_item_id\": \"123\",\n" + "            \"menu_family\": \"106\",\n"
				+ "            \"menu_major_group\": \"100\",\n" + "            \"serial_number\": 1.0\n"
				+ "        }\n" + "    ]\n" + "}";

		return payload;

	}

	public String posRedemptionofFuelPayload(String email, String date, String punchhKey, String txn_no) {

		String payload = "{\n" + "    \"email\": \"" + email + "\",\n" + "    \"receipt_amount\": 15.00,\n"
				+ "    \"subtotal_amount\": 15.00,\n" + "    \"receipt_datetime\": \"" + date + "\",\n"
				+ "    \"sequence_no\": \"2195\",\n" + "    \"discount_type\": \"fuel_reward\",\n"
				+ "    \"item_qty\": 5,\n" + "    \"punchh_key\": \"" + punchhKey + "\",\n"
				+ "    \"transaction_no\": \"" + txn_no + "\",\n" + "    \"menu_items\": [\n" + "        {\n"
				+ "            \"item_name\": \"Philly Cheesesteak Omelette\",\n" + "            \"item_qty\": 1,\n"
				+ "            \"item_amount\": 15.00,\n" + "            \"menu_item_type\": \"M\",\n"
				+ "            \"menu_item_id\": \"101\",\n" + "            \"menu_family\": \"106\",\n"
				+ "            \"menu_major_group\": \"100\",\n" + "            \"serial_number\": 1.0\n"
				+ "        }\n" + "    ]\n" + "}\n" + "";

		return payload;

	}

	public String posVoidRedemptionPayload(String email, String redemption_id) {

		String payload = "{\n" + "  \"email\": \"" + email + "\",\n" + "  \"redemption_id\": \"" + redemption_id
				+ "\"\n" + "}";

		return payload;

	}

	public String api2PurchaseSubscriptionPayload(String plan_id, String purchase_price, String endDateTime,
			String... taxValueLocationIdDiscountValue) {

		String taxRateField = (taxValueLocationIdDiscountValue.length > 0)
				? ",\n\"tax_value\": \"" + taxValueLocationIdDiscountValue[0] + "\"" + ",\n\"location_id\": \""
						+ taxValueLocationIdDiscountValue[1] + "\"" + ",\n\"discount_value\": \""
						+ taxValueLocationIdDiscountValue[2] + "\""
				: "";

		String startTime = CreateDateTime.getYesterdaysDate() + " 01:00:00";
		if (endDateTime == "") {
			endDateTime = CreateDateTime.getFutureDate() + " 21:09:38";
		}
		String payload = "{\n" + "   \"plan_id\": \"" + plan_id + "\",\n" + "   \"start_time\": \"" + startTime
				+ "\",\n" + "   \"end_time\": \"" + endDateTime + "\",\n" + "   \"purchase_price\": \"" + purchase_price
				+ "\",\n" + "   \"auto_renewal\": \"true\"\n" + taxRateField + "}";

		return payload;

	}

	public String posRedemptionofRewardIdAuthOnlineOrder(String authentication_token, String reward_id, String client) {

		String payload = "{\n" + "	\"store_number\": \"7\",\n" + "    \"authentication_token\":\""
				+ authentication_token + "\",\n" + "	\"client\":\"" + client + "\",\n"
				+ "    \"receipt_amount\":4.0,\n" + "    \"subtotal_amount\":4.0,\n"
				+ "    \"receipt_datetime\":\"2021-08-30T17:57:32Z\",\n" + "    \"sequence_no\":\"2129\",\n"
				+ "    \"discount_type\":\"reward\",\n" + "    \"reward_id\": \"" + reward_id + "\",\n"
				+ "    \"transaction_no\":\"9192\",\n" + "    \"menu_items\":\n" + "       [\n" + "        	{\n"
				+ "                \"item_name\":\"Coffee\",\n" + "                \"item_qty\":1,\n"
				+ "                \"item_amount\":4.0,\n" + "                \"menu_item_type\":\"M\",\n"
				+ "                \"menu_item_id\":\"101\",\n" + "                \"menu_family\":\"106\",\n"
				+ "                \"menu_major_group\":\"100\",\n" + "                \"serial_number\":1.0\n"
				+ "        	}\n" + "       ]\n" + "}";

		return payload;

	}

	public String posVoidMultipleRedemptionPayload(String email, ArrayList<String> redemptionIdList) {

		String payload = "{\n" + "    \"email\":\"" + email + "\",\n" + "    \"redemption_id\":[\n" + "        \""
				+ redemptionIdList.get(0) + "\",\n" + "        \"" + redemptionIdList.get(1) + "\",\n" + "        \""
				+ redemptionIdList.get(2) + "\", \n" + "        \"" + redemptionIdList.get(3) + "\" \n" + "        ]\n"
				+ "}";

		return payload;

	}

	public String API2UpdateUserProfilePayload(String client, String email) {

		String payload = "{\n" + "  \"client\": \"" + client + "\",\n" + "	\"user\": {\n" + "		\"email\": \""
				+ email + "\",\n" + "		\"current_password\": \"password@123\",\n"
				+ "		\"password\": \"password@1\",\n" + "		\"password_confirmation\": \"password@1\",\n"
				+ "		\"gender\": \"Female\"\n" + "	}\n" + "}";

		return payload;

	}

	public Map<String, Object> API2UpdateUserEmailPhonePayload(String client, String email, String phone, String fName,
			String lName) {
		Map<String, Object> payload = new HashMap<>();
		payload.put("client", client);
		Map<String, Object> user = new HashMap<>();
		putIfNotEmpty(user, "email", email);
		putIfNotEmpty(user, "phone", phone);
		putIfNotEmpty(user, "first_name", fName);
		putIfNotEmpty(user, "last_name", lName);
		payload.put("user", user);
		return payload;
	}

	// Helper method to add non-empty values to map
	public void putIfNotEmpty(Map<String, Object> map, String key, String value) {
		if (value != null && !value.trim().isEmpty()) {
			map.put(key, value);
		}
	}

	public Map<String, Object> Api2UpdateUserPayload(String client, String email, String phone, String firstName,
			String lastName) {
		Map<String, Object> payload = new HashMap<>();
		payload.put("client", client);
		Map<String, Object> user = new HashMap<>();
		putIfNotEmpty(user, "email", email);
		putIfNotEmpty(user, "phone", phone);
		putIfNotEmpty(user, "first_name", firstName);
		putIfNotEmpty(user, "last_name", lastName);
		payload.put("user", user);
		return payload;
	}

	public String API2CreateUserRelationPayload(String client) {

		String payload = "{\n" + "  \"client\": \"" + client + "\",\n" + "  \"relation\": \"spouse\",\n"
				+ "  \"name\": \"Rose turner\",\n" + "  \"birthday\": \"1988-07-18\"\n" + "}";
		return payload;

	}

	public String API2CreateUserRelationSpouseBirthdatePayload(String client, String birthdate) {

		String payload = "{\n" + "  \"client\": \"" + client + "\",\n" + "  \"relation\": \"spouse\",\n"
				+ "  \"name\": \"Rose turner\",\n" + "  \"birthday\": \"" + birthdate + "\"\n" + "}";
		return payload;

	}

	public String API2CreateUserRelationKidBirthdatePayload(String client, String name, String birthdate) {

		String payload = "{\n" + "  \"client\": \"" + client + "\",\n" + "  \"relation\": \"kid\",\n" + "  \"name\": \""
				+ name + "\",\n" + "  \"birthday\": \"" + birthdate + "\"\n" + "}";
		return payload;

	}

	public String API2UpdateUserRelationPayload(String client) {

		String payload = "{\n" + "  \"client\": \"" + client + "\",\n" + "  \"relation\": \"kid\",\n"
				+ "  \"name\": \"Jack Junior\",\n" + "  \"birthday\": \"2014-12-31\"\n" + "}";
		return payload;

	}

	public String API2ClientPayload(String client) {

		String payload = "{\n" + "  \"client\": \"" + client + "\"\n" + "}";
		return payload;
	}

	public String authGrantLoyaltyPayload(String token, String client, String amount) {
		Calendar cal = Calendar.getInstance();
		String externalUid = "56432 " + CreateDateTime.getTimeDateString();
		String transactionNumber = "123456" + CreateDateTime.getTimeDateString();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		cal.add(Calendar.DATE, -1);
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		String payload = "{		" + "\"authentication_token\":\"" + token + "\",\n" + "			\"receipt_amount\":"
				+ amount + ",\n" + "			\"cc_last4\":4387,\n" + "			\"employee_id\":7,\n"
				+ "			\"employee_name\":\"Test User\",\n" + "			\"store_number\":\"58\",\n"
				+ "			\"menu_items\":[  \n" + "			{\n" + "			\"item_name\":\"White rice\",\n"
				+ "			\"item_qty\":1,\n" + "			\"item_amount\":2.86,\n"
				+ "			\"menu_item_type\":\"M\",\n" + "			\"menu_item_id\":3419,\n"
				+ "			\"menu_family\":\"800\",\n" + "			\"menu_major_group\":\"152\",\n"
				+ "			\"serial_number\":\"1.0\"\n" + "			},\n" + "			{\n"
				+ "			\"item_name\":\"Brown rice\",\n" + "			\"item_qty\":1,\n"
				+ "			\"item_amount\":7.86,\n" + "			\"menu_item_type\":\"M\",\n"
				+ "			\"menu_item_id\":3418,\n" + "			\"menu_family\":\"800\",\n"
				+ "			\"menu_major_group\":\"152\",\n" + "			\"serial_number\":\"2.0\"\n"
				+ "			},\n" + "			{\n" + "			\"item_name\":\"Free rice\",\n"
				+ "			\"item_qty\":1,\n" + "			\"item_amount\":2.86,\n"
				+ "			\"menu_item_type\":\"D\",\n" + "			\"menu_item_id\":3419,\n"
				+ "			\"menu_family\":\"800\",\n" + "			\"menu_major_group\":\"152\",\n"
				+ "			\"serial_number\":\"3.0\"\n" + "			}\n" + "			],\n"
				+ "			\"subtotal_amount\":" + amount + ",\n" + "			\"receipt_datetime\":\""
				+ recieptDateTime + "\",\n" + "			\"transaction_no\":\"" + transactionNumber + "\",\n"
				+ "			\"external_uid\":\"" + externalUid + "\",\n" + "			\"client\":\"" + client
				+ "\",\n" + "			\"channel\":\"online_order\"\n" + "			}";
		return payload;
	}

	public String authOnlineBankCurrencyRedemptionPayload(String authentication_token, String client) {
		Calendar cal = Calendar.getInstance();
		String externalUid = "a unique id " + CreateDateTime.getTimeDateString();
		String transactionNumber = CreateDateTime.getTimeDateString();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		cal.add(Calendar.DATE, -1);
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		String payload = "{\n" + "  \"store_number\": \"58\",\n" + "  \"receipt_amount\": 5,\n"
				+ "  \"discount_type\": \"discount_amount\",\n" + "  \"redeemed_points\": \"2\",\n"
				+ "  \"authentication_token\": \"" + authentication_token + "\",\n" + "  \"menu_items\": [\n"
				+ "    {\n" + "      \"item_name\": \"Small Pizza\",\n" + "      \"item_qty\": 1,\n"
				+ "      \"item_amount\": 4,\n" + "      \"menu_item_type\": \"M\",\n"
				+ "      \"menu_item_id\": 101,\n" + "      \"menu_family\": \"106\",\n"
				+ "      \"menu_major_group\": \"100\",\n" + "      \"serial_number\": \"1.0\"\n" + "    },\n"
				+ "    {\n" + "      \"item_name\": \"Cheese\",\n" + "      \"item_qty\": 1,\n"
				+ "      \"item_amount\": 1,\n" + "      \"menu_item_type\": \"M\",\n"
				+ "      \"menu_item_id\": 3418,\n" + "      \"menu_family\": \"800\",\n"
				+ "      \"menu_major_group\": \"152\",\n" + "      \"serial_number\": \"1.1\"\n" + "    }\n" + "  ],\n"
				+ "  \"subtotal_amount\": 2,\n" + "  \"receipt_datetime\": \"" + recieptDateTime + "\",\n"
				+ "  \"transaction_no\": \"" + transactionNumber + "\",\n" + "  \"external_uid\": \"" + externalUid
				+ "\",\n" + "  \"client\": \"" + client + "\",\n" + "  \"process\": true\n" + "}";
		return payload;
	}

	public String API2ForgotPassword(String client, String email) {

		String payload = "{\n" + "  \"client\": \"" + client + "\",\n" + "  \"user\": {\n" + "    \"email\": \"" + email
				+ "\"\n" + "  }\n" + "}";
		return payload;
	}

	public String API2EstimatePointsEarning(String client, String access_token) {

		String payload = "{\n" + "  \"client\": \"" + client + "\",\n" + "    \"access_token\": \"" + access_token
				+ "\",\n" + "    \"subtotal_amount\": 10,\n" + "    \"receipt_amount\": 10,\n" + "  \"menu_items\": [\n"
				+ "    {\n" + "      \"item_name\": \"Brown rice\",\n" + "      \"item_qty\": 1,\n"
				+ "      \"item_amount\": 10,\n" + "      \"menu_item_type\": \"M\",\n"
				+ "      \"menu_item_id\": 3418,\n" + "      \"menu_family\": \"800\",\n"
				+ "      \"menu_major_group\": \"152\",\n" + "      \"serial_number\": \"2.0\"\n" + "    }\n" + "  ]\n"
				+ "}";
		return payload;

	}

	public String authOnlineCardBasedRedemptionPayload(String authentication_token, String client) {
		Calendar cal = Calendar.getInstance();
		String externalUid = "a unique id " + CreateDateTime.getTimeDateString();
		String transactionNumber = CreateDateTime.getTimeDateString();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		cal.add(Calendar.DATE, -1);
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		String payload = "{\n" + "  \"store_number\": \"58\",\n" + "  \"receipt_amount\": 5,\n"
				+ "  \"discount_type\": \"card_completion\",\n" + "  \"redeemed_points\": \"2\",\n"
				+ "  \"authentication_token\": \"" + authentication_token + "\",\n" + "  \"menu_items\": [\n"
				+ "    {\n" + "      \"item_name\": \"Small Pizza\",\n" + "      \"item_qty\": 1,\n"
				+ "      \"item_amount\": 4,\n" + "      \"menu_item_type\": \"M\",\n"
				+ "      \"menu_item_id\": 101,\n" + "      \"menu_family\": \"106\",\n"
				+ "      \"menu_major_group\": \"100\",\n" + "      \"serial_number\": \"1.0\"\n" + "    },\n"
				+ "    {\n" + "      \"item_name\": \"Cheese\",\n" + "      \"item_qty\": 1,\n"
				+ "      \"item_amount\": 1,\n" + "      \"menu_item_type\": \"M\",\n"
				+ "      \"menu_item_id\": 3418,\n" + "      \"menu_family\": \"800\",\n"
				+ "      \"menu_major_group\": \"152\",\n" + "      \"serial_number\": \"1.1\"\n" + "    }\n" + "  ],\n"
				+ "  \"subtotal_amount\": 2,\n" + "  \"receipt_datetime\": \"" + recieptDateTime + "\",\n"
				+ "  \"transaction_no\": \"" + transactionNumber + "\",\n" + "  \"external_uid\": \"" + externalUid
				+ "\",\n" + "  \"client\": \"" + client + "\",\n" + "  \"process\": true\n" + "}";

		return payload;
	}

	public String authOnlineRedeemableRedemptionPayload(String authentication_token, String client, String item_id,
			String redeemable_id) {
		Calendar cal = Calendar.getInstance();
		String externalUid = "a unique id " + CreateDateTime.getTimeDateString();
		String transactionNumber = CreateDateTime.getTimeDateString();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		cal.add(Calendar.DATE, -1);
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		String payload = "{\n" + "  \"store_number\": \"58\",\n" + "  \"receipt_amount\": 5,\n"
				+ "  \"redeemable_id\": \"" + redeemable_id + "\",\n" + "  \"discount_type\": \"redeemable\",\n"
				+ "  \"redeemed_points\": \"2\",\n" + "  \"authentication_token\": \"" + authentication_token + "\",\n"
				+ "  \"menu_items\": [\n" + "    {\n" + "      \"item_name\": \"Small Pizza\",\n"
				+ "      \"item_qty\": 1,\n" + "      \"item_amount\": 4,\n" + "      \"menu_item_type\": \"M\",\n"
				+ "      \"menu_item_id\": \"" + item_id + "\",\n" + "      \"menu_family\": \"106\",\n"
				+ "      \"menu_major_group\": \"100\",\n" + "      \"serial_number\": \"1.0\"\n" + "    },\n"
				+ "    {\n" + "      \"item_name\": \"Cheese\",\n" + "      \"item_qty\": 1,\n"
				+ "      \"item_amount\": 1,\n" + "      \"menu_item_type\": \"M\",\n"
				+ "      \"menu_item_id\": 3418,\n" + "      \"menu_family\": \"800\",\n"
				+ "      \"menu_major_group\": \"152\",\n" + "      \"serial_number\": \"1.1\"\n" + "    }\n" + "  ],\n"
				+ "  \"subtotal_amount\": 400,\n" + "  \"receipt_datetime\": \"" + recieptDateTime + "\",\n"
				+ "  \"transaction_no\": \"" + transactionNumber + "\",\n" + "  \"external_uid\": \"" + externalUid
				+ "\",\n" + "  \"client\": \"" + client + "\",\n" + "  \"process\": true\n" + "}";

		return payload;
	}

	public String authOnlineCouponPromoRedemptionPayload(String authentication_token, String client,
			String couponCode) {
		Calendar cal = Calendar.getInstance();
		// String externalUid = "a unique id " + CreateDateTime.getTimeDateString();
		String transactionNumber = CreateDateTime.getTimeDateString();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		cal.add(Calendar.DATE, -1);
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		String payload = "{\n" + "    \"authentication_token\": \"" + authentication_token + "\",\n"
				+ "    \"discount_type\": \"redemption_code\",\n" + "    \"redemption_code\":\"" + couponCode + "\",\n"
				+ "    \"receipt_amount\": 12.72,\n" + "    \"cc_last4\": 4387,\n" + "    \"employee_id\": \"7\",\n"
				+ "    \"employee_name\": \"Test User\",\n" + "    \"store_number\": \"58\",\n"
				+ "    \"menu_items\": [\n" + "        {\n" + "            \"item_name\": \"White rice\",\n"
				+ "            \"item_qty\": 1,\n" + "            \"item_amount\": 2.86,\n"
				+ "            \"menu_item_type\": \"M\",\n" + "            \"menu_item_id\": 3419,\n"
				+ "            \"menu_family\": \"800\",\n" + "            \"menu_major_group\": \"152\",\n"
				+ "            \"serial_number\": \"1.0\"\n" + "        },\n" + "        {\n"
				+ "            \"item_name\": \"Brown rice\",\n" + "            \"item_qty\": 1,\n"
				+ "            \"item_amount\": 7.86,\n" + "            \"menu_item_type\": \"M\",\n"
				+ "            \"menu_item_id\": 3418,\n" + "            \"menu_family\": \"800\",\n"
				+ "            \"menu_major_group\": \"152\",\n" + "            \"serial_number\": \"2.0\"\n"
				+ "        }\n" + "    ],\n" + "    \"subtotal_amount\": 12.72,\n" + "    \"receipt_datetime\": \""
				+ recieptDateTime + "\",\n" + "    \"transaction_no\": \"" + transactionNumber + "\",\n"
				+ "    \"client\": \"" + client + "\"\n" + "}";
		return payload;
	}

	public String API2RedemptionWitReedemable_id(String client) {

		String payload = "{\n" + "  \"client\": \"" + client + "\",\n" + "  \"redeemable_id\": 9145,\n"
				+ "  \"location_id\": \"304155\",\n" + "  \"latitude\": 26.9167509,\n"
				+ "  \"longitude\": 75.8136926,\n" + "  \"gps_accuracy\": 27\n" + "}";

		return payload;

	}

	public String API2SendMessageToUser(String userID, String amount, String reedemable_id, String fuelAmount,
			String gift_count) {

		String payload = "{\n" + "   \"user_id\": \"" + userID + "\",\n"
				+ "   \"message\": \"Automation test message\",\n" + "   \"subject\": \"Message from automation\",\n"
				+ "   \"reward_amount\": \"" + amount + "\",\n" + "   \"gift_count\": \"" + gift_count + "\",\n"
				+ "   \"fuel_amount\": \"" + fuelAmount + "\",\n" + "   \"redeemable_id\": \"" + reedemable_id + "\",\n"
				+ "   \"end_date\": \"\",\n" + "   \"gift_reason\": \"\"\n" + ",  \"end_date\": \"\"}";
		return payload;
	}

	public String API2SendMessageToUser(String userID, String reward_amount, String reedemable_id) {

		String payload = "{\n" + "   \"user_id\": \"" + userID + "\",\n"
				+ "   \"message\": \"Automation test message\",\n" + "   \"subject\": \"Message from automation\",\n"
				+ "   \"reward_amount\": \"" + reward_amount + "\",\n" + "   \"gift_count\": \"200\",\n"
				+ "   \"redeemable_id\": \"" + reedemable_id + "\",\n" + "   \"end_date\": \"\",\n"
				+ "   \"gift_reason\": \"\"\n" + "}";

		return payload;

	}

	public String API2RedemptionWithBankedCurrency(String client) {

		String payload = "{\n" + "  \"client\": \"" + client + "\",\n" + "  \"banked_currency\": 20,\n"
				+ "  \"location_id\": \"304155\",\n" + "  \"latitude\": 26.9167509,\n"
				+ "  \"longitude\": 75.8136926,\n" + "  \"gps_accuracy\": 27\n" + "}";

		return payload;

	}

	public String API2RedemptionWithRewardId(String client, String reward_id) {

		String payload = "{\n" + "  \"client\": \"" + client + "\",\n" + "  \"reward_id\": " + reward_id + ",\n"
				+ "  \"location_id\": \"304155\",\n" + "  \"latitude\": 26.9167509,\n"
				+ "  \"longitude\": 75.8136926,\n" + "  \"gps_accuracy\": 27\n" + "}";

		return payload;

	}

	public String API2GiftReardtoOtherUser(String email, String reward_id) {

		String payload = "{\n" + "  \"recipient_email\": \"" + email + "\",\n" + "  \"reward_to_transfer\": \""
				+ reward_id + "\"\n" + "}";

		return payload;

	}

	public String API2GiftAmounttoOtherUser(String email) {

		String payload = "{\n" + "  \"recipient_email\": \"" + email + "\",\n" + "  \"amount_to_transfer\": \"10\"\n"
				+ "}";

		return payload;

	}

	public String API2PointConversionPayload(String client, String conversionRuleId) {

		String payload = "{\n" + "  \"client\": \"" + client + "\",\n" + "     \"conversion_rule_id\": "
				+ conversionRuleId + ",\n" + "     \"source_value\": 100,\n" + "     \"converted_value\":\"10\"\n"
				+ "}";

		return payload;

	}

	public String API2LoyaltyCheckinReceiptImage(String client, String access_token, String locationid) {

		String payload = "{\n" + "  \"client\": \"" + client + "\",\n" + "    \"access_token\": \"" + access_token
				+ "\",\n" + "    \"location_id\": \"" + locationid + "\",\n"
				+ "    \"receipt_url\": \"https://upload.wikimedia.org/wikipedia/commons/0/0b/ReceiptSwiss.jpg\",\n"
				+ "    \"gps_accuracy\":\"27\"\n" + "}";

		return payload;

	}

	public String API2LoyaltyCheckinBarCode(String barCode, String client) {

		String payload = "{\n" + "  \"bar_code\": \"" + barCode + "\",\n" + "  \"client\": \"" + client + "\",\n"
				+ "  \"gps_accuracy\": \"27\",\n" + "  \"latitude\": \"33.9077177\",\n"
				+ "  \"longitude\": \"-84.3595428\"\n" + "}";

		return payload;

	}

	public String API2LoyaltyCheckinQRCode(String qrCode, String client, String access_token) {

		String payload = "{\n" + "    \"qr_decoded\":\"" + qrCode + "\",\n" + "  \"client\": \"" + client + "\",\n"
				+ "    \"access_token\":\"" + access_token + "\",\n" + "    \"location_id\": \"\"\n" + "}";

		return payload;

	}

	public String API2TransactionDetails(String client, String checkin_id) {

		String payload = "{\n" + "  \"client\": \"" + client + "\",\n" + "  \"transaction_id\": \"" + checkin_id
				+ "\"\n" + "}";

		return payload;
	}

	public String API2ListApplicableOffers() {

		String payload = "{\n" + "  \"receipt_amount\": 8.0,\n" + "  \"menu_items\": [\n" + "    {\n"
				+ "      \"item_name\": \"Whiterice\",\n" + "      \"item_qty\": 1,\n"
				+ "      \"item_amount\": 9.950,\n" + "      \"menu_item_type\": \"M\",\n"
				+ "      \"menu_item_id\": 12,\n" + "      \"menu_family\": \"800\",\n"
				+ "      \"menu_major_group\": \"152\",\n" + "      \"serial_number\": \"1.0\"\n" + "    }\n" + "  ]\n"
				+ "}";
		return payload;
	}

	public String API1PurchaseGiftcard() {

		String payload = "{\n" + "    \"design_id\":186,\n" + "    \"amount\":\"10\",\n"
				+ "    \"transaction_token\":\"fake-valid-nonce\",\n" + "    \"cardholder_name\":\"Akansha Jain\",\n"
				+ "    \"exp_date\":\"0422\",\n" + "    \"type\":\"VISA\"\n" + "}";
		return payload;
	}

	public String API1ReloadGiftcard(String amount, String trToken) {

		String payload = "{\n" + "    \"design_id\": \"186\",\n" + "    \"amount\":\"" + amount + "\",\n"
				+ "    \"transaction_token\":\"" + trToken + "\"\n"
				+ ",\"type\":\"visa\",\"cardholder_name\":\"TestFName10100919102023\",\"exp_date\":\"1230\"}";
		return payload;
	}

	public String API1PurchaseGiftcard(String amount, String cardId) {

		String payload = "{\n" + "    \"design_id\":\"" + cardId + "\",\n" + "    \"amount\":\"" + amount + "\",\n"
				+ "    \"transaction_token\":\"fake-valid-nonce\",\n" + "    \"cardholder_name\":\"Akansha Jain\",\n"
				+ "    \"exp_date\":\"0428\",\n" + "    \"type\":\"VISA\"\n" + "}";
		return payload;
	}

	public String API1Email(String email) {

		String payload = "{\n" + "    \"email\":\"" + email + "\"\n" + "}";
		return payload;
	}

	public String elubGuestPayloadWithEmail(String email, String storeNumber, Boolean flag) {
		String fName = email.replace("Auto", "fName").replace("@punchh.com", "");
		String lName = email.replace("Auto", "lName").replace("@punchh.com", "");
		String payload = "{\n" + "  \"store_number\":\"" + storeNumber + "\",\n" + "  \"user\": \n" + "  {\n"
				+ "    \"email\":\"" + email + "\",\n" + "    \"phone\":\"9911223344\",\n" + "    \"first_name\":\""
				+ fName + "\",\n" + "    \"last_name\":\"" + lName + "\",\n" + "    \"send_compliance_sms\":\"" + flag
				+ "\",\n" + "    \"active_registration\":\"" + flag + "\",\n"
				+ "    \"marketing_email_subscription\":\"" + flag + "\"\n" + "\n" + "  }\n" + "}";
		return payload;
	}

	/*
	 * public String elubGuestPayloadWithPhone(String phoneNumber, String
	 * storeNumber) { String fName = "fName" + phoneNumber; String lName = "lName" +
	 * phoneNumber; String payload = "{\n" + "  \"store_number\":\"" + storeNumber +
	 * "\",\n" + "  \"user\": \n" + "  {\n" + "    \"phone\":\"" + phoneNumber +
	 * "\",\n" + "    \"first_name\":\"" + fName + "\",\n" + "    \"last_name\":\""
	 * + lName + "\",\n" + "    \"send_compliance_sms\":\"true\",\n" +
	 * "    \"active_registration\":\"true\",\n" +
	 * "    \"marketing_email_subscription\":\"true\"\n" + "\n" + "  }\n" + "}";
	 * return payload; }
	 */

	public String elubGuestPayloadWithPhone(String phoneNumber, String storeNumber, String email) {
		String fName = "fName" + phoneNumber;
		String lName = "lName" + phoneNumber;
		String payload = "{\n" + "  \"store_number\":\"" + storeNumber + "\",\n" + "  \"user\": \n" + "  {\n"
				+ "    \"email\":\"" + email + "\",\n" + "    \"phone\":\"9911223344\",\n" + "    \"first_name\":\""
				+ fName + "\",\n" + "    \"last_name\":\"" + lName + "\",\n" + "    \"send_compliance_sms\":\"true\",\n"
				+ "    \"active_registration\":\"true\",\n" + "    \"marketing_email_subscription\":\"true\"\n" + "\n"
				+ "  }\n" + "}";
		return payload;
	}

	public String API1TransferGiftCard(String email, String amount) {

		String payload = "{\n" + "	\"email\":\"" + email + "\",\n" + "	\"full_transfer\":false,\n" + "	\"amount\":"
				+ amount + "\n" + "}";
		return payload;
	}

	public String API1BarCodeCheckin(String barCode) {

		String payload = "{\n" + "  \"bar_code\":\"" + barCode + "\"\n" + "}";
		return payload;
	}

	/*
	 * public String Api1CreatePickupOrderPayload(String email, String orderId,
	 * String timeReady, String deliveryMethod) {
	 * 
	 * String payload = "{\n" + "  \"customer\": {\n" +
	 * "    \"contactNumber\": \"9912435465\",\n" + "    \"customerId\": 63777,\n" +
	 * "    \"email\": \"" + email + "\",\n" + "    \"firstName\": \"Fname\",\n" +
	 * "    \"lastName\": \"Lname \"\n" + "    },\n" +
	 * "  \"deliveryMethod\": \""+deliveryMethod+"\",\n" +
	 * "  \"handoff_mode\":\"curbside\",\n" + "   \"external_order_id\":\"" +
	 * orderId + "\",\n" + "  \"items\": [\n" + "    {\n" +
	 * "      \"description\": \"Four Chocolate Chunk Cookies Double Chocolate\", \n"
	 * + "      \"quantity\": 1, \n" + "      \"selling_price\": 5},\n" +
	 * "    { \"description\": \"2 Chocolate Chunk Cookies\", \n" +
	 * "\"quantity\": 1, \n" + "\"selling_price\": 5\n" + "          }\n" +
	 * "             ],\n" + "\n" + "  \"storeNumber\": 66233,\n" +
	 * "  \"timePlaced\": \"20210915 19:14\",\n" +
	 * "  \"timeReady\": \""+timeReady+"\",\n" + " \"storeUtcOffset\":5.5 ,\n" +
	 * "  \"timeWanted\": \"20210917 16:28\",\n" + "  \"totals\": {\n" +
	 * "    \"discount\": 1,\n" + "    \"feesTotal\": 1,\n" +
	 * "    \"salesTax\": 1,\n" + "    \"subTotal\": 1,\n" + "    \"tip\": 1,\n" +
	 * "    \"total\":10.99\n" + "    },\n" + "  \"user_location\": {\n" +
	 * "		\"long\": \"74.94873046875001\",\n" +
	 * "		\"lat\": \"26.52956523826758\",\n" +
	 * "		\"status\": \"claimed\",\n" + "		\"distance\": \"\",\n" +
	 * "		\"arrival_time\": \"\",\n" + "		\"location_service\": false,\n"
	 * + "        \"contextual_arrival_info\": \"1B\"\n" +
	 * "    },\"is_replacement\": true,\n" + "    \"clientPlatform\":\"iOS\",\n" +
	 * "  \"vehicle_id\": \"123456\",\n" + "  \"olo_uuid\":17689090933784,\n" +
	 * "  \"external_order_uuid\": 9879879871034}"; return payload; }
	 */

	public String regApplicableOffers(String item_qty, String item_amount) {

		String payload = "{\n" + "  \"receipt_amount\": 8.0,\n" + "  \"menu_items\": [\n" + "    {\n"
				+ "      \"item_name\": \"Whiterice\",\n" + "      \"item_qty\": " + item_qty + ",\n"
				+ "      \"item_amount\": " + item_amount + ",\n" + "      \"menu_item_type\": \"M\",\n"
				+ "      \"menu_item_id\": 12,\n" + "      \"menu_family\": \"800\",\n"
				+ "      \"menu_major_group\": \"152\",\n" + "      \"serial_number\": \"1.0\"\n" + "    }\n" + "  ]\n"
				+ "}";
		return payload;
	}

	public String AuthListApplicableOffers(String itemQty, String itemAmount) {
		String payload = "{\n" + "    \"menu_items\": [\n" + "        {\n" + "            \"item_name\": \"coffee\",\n"
				+ "            \"item_qty\": " + itemQty + ",\n" + "            \"item_amount\": " + itemAmount + ",\n"
				+ "            \"menu_item_type\": \"M\",\n" + "            \"menu_item_id\": 101,\n"
				+ "            \"menu_family\": \"800\",\n" + "            \"menu_major_group\": \"152\"\n"
				+ "        }\n" + "    ],\n" + "    \"subtotal_amount\": " + itemAmount + ",\n"
				+ "    \"receipt_amount\": " + itemAmount + "\n" + "}";
		return payload;
	}

	public String createCampaignPayload(String redeemableUuId, String segmentId, String startTime) {
		String payload = "{\n" + "    \"redeemable_uuid\": \"" + redeemableUuId + "\",\n" + "    \"segment_id\": "
				+ segmentId + ",\n" + "    \"category\": \"gift_redeemable\",\n"
				+ "    \"campaign_type\": \"mass_gifting\",\n" + "    \"name\": \"API event\",\n"
				+ "    \"start_time\": \"" + startTime + "\",\n" + "    \"external_campaign_id\": \"2\"\n" + "}";
		return payload;
	}

	public String createCampaignPayload(String redeemableUuId, String segmentId, String startTime,
			String campaignType) {
		String payload = "{\n" + "    \"redeemable_uuid\": \"" + redeemableUuId + "\",\n" + "    \"segment_id\": "
				+ segmentId + ",\n" + "    \"category\": \"gift_redeemable\",\n" + "    \"campaign_type\": \""
				+ campaignType + "\",\n" + "    \"name\": \"API event\",\n" + "    \"start_time\": \"" + startTime
				+ "\",\n" + "    \"external_campaign_id\": \"2\"\n" + "}";
		return payload;
	}

	public String createCampaignCategoryPayload(String redeemableUuId, String segmentId, String startTime,
			String category) {
		String payload = "{\n" + "    \"redeemable_uuid\": \"" + redeemableUuId + "\",\n" + "    \"segment_id\": "
				+ segmentId + ",\n" + "    \"category\": \"" + category + "\",\n"
				+ "    \"campaign_type\": \"mass_gifting\",\n" + "    \"name\": \"API event\",\n"
				+ "    \"start_time\": \"" + startTime + "\",\n" + "    \"external_campaign_id\": \"2\"\n" + "}";
		return payload;
	}

	// public String dynamicCoupunPayload(String email, String campaignUuid) {
	// String payload = "{\n"
	// + " \"email\": \""+email+"\",\n"
	// + " \"campaign_uuid\": \""+campaignUuid+"\"\n"
	// + "}";
	// return payload;
	// }

	public String authOnlineCouponRedemptionPayload(String authentication_token, String client, String couponCode) {
		Calendar cal = Calendar.getInstance();
		String externalUid = "a unique id " + CreateDateTime.getTimeDateString();
		String transactionNumber = CreateDateTime.getTimeDateString();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		cal.add(Calendar.DATE, -1);
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		String payload = "{\n" + "  \"store_number\": \"58\",\n" + "  \"receipt_amount\": 5,\n"
				+ "  \"redemption_code\": \"" + couponCode + "\",\n" + "  \"discount_type\": \"redeemable\",\n"
				+ "  \"redeemed_points\": \"2\",\n" + "  \"authentication_token\": \"" + authentication_token + "\",\n"
				+ "  \"menu_items\": [\n" + "    {\n" + "      \"item_name\": \"Small Pizza\",\n"
				+ "      \"item_qty\": 1,\n" + "      \"item_amount\": 4,\n" + "      \"menu_item_type\": \"M\",\n"
				+ "      \"menu_item_id\": 1001,\n" + "      \"menu_family\": \"106\",\n"
				+ "      \"menu_major_group\": \"100\",\n" + "      \"serial_number\": \"1.0\"\n" + "    },\n"
				+ "    {\n" + "      \"item_name\": \"Cheese\",\n" + "      \"item_qty\": 1,\n"
				+ "      \"item_amount\": 1,\n" + "      \"menu_item_type\": \"M\",\n"
				+ "      \"menu_item_id\": 3418,\n" + "      \"menu_family\": \"800\",\n"
				+ "      \"menu_major_group\": \"152\",\n" + "      \"serial_number\": \"1.1\"\n" + "    }\n" + "  ],\n"
				+ "  \"subtotal_amount\": 2,\n" + "  \"receipt_datetime\": \"" + recieptDateTime + "\",\n"
				+ "  \"transaction_no\": \"" + transactionNumber + "\",\n" + "  \"external_uid\": \"" + externalUid
				+ "\",\n" + "  \"client\": \"" + client + "\",\n" + "  \"process\": true\n" + "}";

		return payload;
	}

	public String posPayload(String email, String date, String punchhKey, String txn_no, String redeemAmount) {

		String payload = "{\n" + "    \"email\": \"" + email + "\",\n" + "    \"receipt_amount\": 10.00,\n"
				+ "    \"subtotal_amount\": 10.00,\n" + "    \"receipt_datetime\": \"" + date + "\",\n"
				+ "    \"sequence_no\": \"2191\",\n" + "    \"discount_type\": \"discount_amount\",\n"
				+ "    \"redeemed_points\": " + redeemAmount + ",\n" + "    \"punchh_key\": \"" + punchhKey + "\",\n"
				+ "    \"transaction_no\": \"" + txn_no + "\",\n" + "    \"menu_items\": [\n" + "        {\n"
				+ "            \"item_name\": \"Philly Cheesesteak Omelette\",\n" + "            \"item_qty\": 1,\n"
				+ "            \"item_amount\": 10.00,\n" + "            \"menu_item_type\": \"M\",\n"
				+ "            \"menu_item_id\": \"101\",\n" + "            \"menu_family\": \"106\",\n"
				+ "            \"menu_major_group\": \"100\",\n" + "            \"serial_number\": 1.0\n"
				+ "        }\n" + "    ]\n" + "}";
		return payload;
	}

	public String API2PurchaseGiftCardPayload(String client, String access_token, String design_id) {
		int lowerBound = 10;
		int upperBound = 20;
		int randomAmount = (int) (Math.random() * (upperBound - lowerBound + 1)) + lowerBound;

		String payload = "{\n" + "    \"client\":\"" + client + "\",\n" + "    \"access_token\":\"" + access_token
				+ "\",\n" + "    \"design_id\":\"" + design_id + "\",\n" + "    \"amount\":\"" + randomAmount + "\",\n"
				+ "    \"transaction_token\":\"fake-valid-nonce\",\n" + "    \"cardholder_name\":\"Test Name\",\n"
				+ "    \"exp_date\":\"0825\",\n" + "    \"type\":\"VISA\"\n" + "}";
		return payload;
	}

	public String API2PurchaseGiftCardPayload(String client, String access_token, String design_id, String amount,
			String expDate, String firstName) {

		String payload = "{\n" + "    \"client\":\"" + client + "\",\n" + "    \"access_token\":\"" + access_token
				+ "\",\n" + "    \"design_id\":\"" + design_id + "\",\n" + "    \"amount\": \"" + amount + "\",\n"
				+ "    \"transaction_token\":\"fake-valid-nonce\",\n" + "    \"cardholder_name\":\"" + firstName
				+ "\",\n" + "    \"exp_date\":\"" + expDate + "\",\n" + "    \"type\":\"VISA\"\n" + "}";
		return payload;
	}

	public String API2GiftaCardPayload(String email, String client, String design_id, String amount, String expDate,
			String firstName) {

		String payload = "{\n" + "  \"recipient_email\": \"" + email + "\",\n" + "  \"client\": \"" + client + "\",\n"
				+ "  \"amount\": \"12\",\n" + "  \"design_id\": \"" + design_id + "\",\n" + "    \"amount\": \""
				+ amount + "\",\n" + "  \"transaction_token\": \"fake-valid-nonce\",\n" + "    \"cardholder_name\":\""
				+ firstName + "\",\n" + "    \"exp_date\":\"" + expDate + "\",\n" + "  \"type\": \"VISA\"\n" + "}";

		return payload;
	}

	public String API2ReloadGiftCardPayload(String client, String amount, String firstName, String expDate) {

		String payload = "{\n" + "    \"client\": \"" + client + "\",\n" + "    \"amount\":\"" + amount + "\",\n"
				+ "    \"transaction_token\": \"fake-valid-nonce\",\n" + "    \"cardholder_name\": \"" + firstName
				+ "\",\n" + "    \"exp_date\": \"" + expDate + "\",\n" + "    \"type\": \"VISA\"\n" + "}";

		return payload;

	}

	public String API2UpdateGiftCardPayload(String client) {

		String payload = "{\n" + "  \"client\": \"" + client + "\",\n" + "  \"amount\": \"10\",\n"
				+ "  \"transaction_token\": \"fake-valid-nonce\",\n" + "  \"cardholder_name\": \"Test Name\",\n"
				+ "  \"exp_date\": 825,\n" + "  \"type\": \"VISA\"\n" + "}";

		return payload;

	}

	public String API2ReloadGiftCardPayload(String client) {

		String payload = "{\n" + "    \"client\": \"" + client + "\",\n" + "    \"amount\": \"15\",\n"
				+ "    \"transaction_token\": \"fake-valid-nonce\",\n" + "    \"cardholder_name\": \"Test Name\",\n"
				+ "    \"exp_date\": 825,\n" + "    \"type\": \"VISA\"\n" + "}";

		return payload;

	}

	public String API2FetchGiftCardPayload(String client) {

		String payload = "{\n" + "    \"client\": \"" + client + "\",\n" + "    \"passcode\": \"123456\"\n" + "}";

		return payload;

	}

	public String API2OnlyClientBodyPayload(String client) {

		String payload = "{\n" + "		\"client\":\"" + client + "\"\n" + "}";

		return payload;

	}

	public String API2TransferGiftCardPayload(String client, String email) {

		String payload = "{\n" + "  \"client\": \"" + client + "\",\n" + "  \"recipient_email\": \"" + email + "\",\n"
				+ "  \"passcode\": \"1234\",\n" + "  \"amount\": \"10\",\n" + "  \"full_transfer\": false\n" + "}";

		return payload;

	}

	public String API2ShareGiftCardPayload(String client, String email) {

		String payload = "{\n" + "  \"client\": \"" + client + "\",\n" + "  \"recipient_email\": \"" + email + "\",\n"
				+ "  \"passcode\": \"1234\"\n" + "}";

		return payload;

	}

	public String API2TipGiftCardPayload(String client, String checkin_id) {

		String payload = "{\n" + "  \"client\": \"" + client + "\",\n" + "  \"checkin_id\": " + checkin_id + ",\n"
				+ "  \"tip\": 5.00\n" + "}";

		return payload;

	}

	public String API2GiftaCardPayload(String email, String client, String design_id) {

		String payload = "{\n" + "  \"recipient_email\": \"" + email + "\",\n" + "  \"client\": \"" + client + "\",\n"
				+ "  \"amount\": \"12\",\n" + "  \"design_id\": \"" + design_id + "\",\n"
				+ "  \"transaction_token\": \"fake-valid-nonce\",\n" + "  \"cardholder_name\": \"Test Name\",\n"
				+ "  \"exp_date\": 825,\n" + "  \"type\": \"VISA\"\n" + "}";

		return payload;

	}

	public String api2GiftCardGiftedWithRandomAmountPayload(String email, String client, String designId,
			String transactionToken, String cardHolderName, String expDate, String cardType) {
		int randomAmount = Utilities.getRandomNoFromRange(10, 20);
		String payload = "{\n" + "  \"recipient_email\": \"" + email + "\",\n" + "  \"client\": \"" + client + "\",\n"
				+ "  \"amount\": \"" + randomAmount + "\",\n" + "  \"design_id\": \"" + designId + "\",\n"
				+ "  \"transaction_token\": \"" + transactionToken + "\",\n" + "  \"cardholder_name\": \""
				+ cardHolderName + "\",\n" + "  \"exp_date\": " + expDate + ",\n" + "  \"type\": \"" + cardType + "\"\n"
				+ "}";
		return payload;
	}

	public String api2ReloadGiftCardWithRandomAmountPayload(String email, String client, String designId,
			String transactionToken, String cardHolderName, String expDate, String cardType) {
		int randomAmount = Utilities.getRandomNoFromRange(10, 20);
		String payload = "{\n" + "    \"client\": \"" + client + "\",\n" + "    \"amount\": \"" + randomAmount + "\",\n"
				+ "    \"transaction_token\": \"" + transactionToken + "\",\n" + "    \"cardholder_name\": \""
				+ cardHolderName + "\",\n" + "    \"exp_date\": " + expDate + ",\n" + "    \"type\": \"" + cardType
				+ "\"\n" + "}";
		return payload;
	}

	public String API2CreateFeedbackPayload(String client) {

		String payload = "{\n" + "  \"client\": \"" + client + "\",\n" + "  \"message\": \"Good Coffee.\",\n"
				+ "  \"rating\": 5,\n" + "  \"republishable\": false,\n" + "  \"requires_response\": false\n" + "}";

		return payload;

	}

	public String API2UpdateFeedbackPayload(String client) {

		String payload = "{\n" + "  \"client\": \"" + client + "\",\n" + "  \"message\": \"test message\"\n" + "}";

		return payload;

	}

	public String CreateGiftCardClaimToken(String uuid) {

		String payload = "{\n" + "  \"uuid\": \"" + uuid + "\"\n" + "}";

		return payload;

	}

	public String createLocation(String name, String store_number) {

		String payload = "{\n" + "	\"location\": {\n" + "		\"address\": \"201 San Antonio Circle\",\n"
				+ "		\"city\": \"Mountain View\",\n" + "		\"country\": \"United States\",\n"
				+ "		\"latitude\": 37.406658,\n" + "		\"longitude\": -122.109061,\n" + "		\"name\": \"" + name
				+ "\",\n" + "		\"phone_number\": \"12345678\",\n"
				+ "		\"loc_email\": \"location@business.com\",\n" + "		\"post_code\": \"94040\",\n"
				+ "		\"state\": \"California\",\n" + "		\"store_number\": \"" + store_number + "\",\n"
				+ "		\"time_zone\": \"America/Los_Angeles\",\n" + "		\"validation_type\": \"qrcode\",	\n"
				+ "\"enable_daily_redemption_report\": true, \n" + "\"enable_weekly_redemption_report\": true \n"
				+ "	}\n" + "}";

		return payload;

	}

	public String updateLocation(String location_id, String store_number) {

		String payload = "{\n" + "  \"location_id\": " + location_id + ",\n" + "  \"store_number\": \"" + store_number
				+ "\",\n" + "  \"location\": {\n"
				// + " \"store_tags\": \"WiFi,Loyalty Program\",\n"
				+ "    \"location_extra_attributes\": {\n" + "      \"brand\": \"Punchh\",\n"
				+ "      \"online_order_url\": \"https://example.com\",\n" + "      \"store_times\": [\n"
				+ "        {\n" + "          \"day\": \"Mon\",\n" + "          \"start_time\": \"5:30 AM\",\n"
				+ "          \"end_time\": \"7:00 PM\"\n" + "        },\n" + "        {\n"
				+ "          \"day\": \"Tue\",\n" + "          \"start_time\": \"5:30 AM\",\n"
				+ "          \"end_time\": \"8:00 PM\"\n" + "        },\n" + "        {\n"
				+ "          \"day\": \"Wed\",\n" + "          \"start_time\": \"5:30 AM\",\n"
				+ "          \"end_time\": \"8:00 PM\"\n" + "        },\n" + "        {\n"
				+ "          \"day\": \"Thu\",\n" + "          \"start_time\": \"5:30 AM\",\n"
				+ "          \"end_time\": \"8:00 PM\"\n" + "        },\n" + "        {\n"
				+ "          \"day\": \"Fri\",\n" + "          \"start_time\": \"5:30 AM\",\n"
				+ "          \"end_time\": \"8:00 PM\"\n" + "        },\n" + "        {\n"
				+ "          \"day\": \"Sat\",\n" + "          \"start_time\": \"6:30 AM\",\n"
				+ "          \"end_time\": \"8:00 PM\"\n" + "        },\n" + "        {\n"
				+ "          \"day\": \"Sun\",\n" + "          \"start_time\": \"6:30 AM\",\n"
				+ "          \"end_time\": \"6:00 PM\"\n" + "        }\n" + "      ]\n" + "    },\n"
				+ "    \"time_zone\": \"America/Los_Angeles\",\n" + "\"generate_barcodes\": true\n,"
				+ "\"enable_daily_redemption_report\": true, \n" + "\"enable_weekly_redemption_report\": true \n"
				+ "  }\n" + "}";

		return payload;
	}

	public String createLocationWithTaxRate(String name, String store_number, String tax_rate) {

		String payload = "{\n" + "    \"location\": {\n" + "        \"address\": \"201 San Antonio Circle\",\n"
				+ "        \"city\": \"Mountain View\",\n" + "        \"country\": \"United States\",\n"
				+ "        \"latitude\": 37.406658,\n" + "        \"longitude\": -122.109061,\n"
				+ "        \"name\": \"" + name + "\",\n" + "        \"phone_number\": \"12345678\",\n"
				+ "        \"loc_email\": \"location@business.com\",\n" + "        \"post_code\": \"94040\",\n"
				+ "        \"state\": \"California\",\n" + "        \"store_number\": \"" + store_number + "\",\n"
				+ "        \"tax_rate\": \"" + tax_rate + "\",\n" + "        \"time_zone\": \"America/Los_Angeles\",\n"
				+ "        \"validation_type\": \"qrcode\",\n" + "        \"enable_daily_redemption_report\": true,\n"
				+ "        \"enable_weekly_redemption_report\": true\n" + "    }\n" + "}";

		return payload;

	}

	public String updateLocationTaxRate(String location_id, String store_number, String tax_rate) {

		String payload = "{\n" + "  \"location_id\": " + location_id + ",\n" + "  \"store_number\": \"" + store_number
				+ "\",\n" + "  \"location\": {\n" + "    \"location_extra_attributes\": {\n"
				+ "      \"brand\": \"Punchh\",\n" + "      \"online_order_url\": \"https://example.com\",\n"
				+ "      \"store_times\": [\n" + "        {\n" + "          \"day\": \"Mon\",\n"
				+ "          \"start_time\": \"5:30 AM\",\n" + "          \"end_time\": \"7:00 PM\"\n" + "        },\n"
				+ "        {\n" + "          \"day\": \"Tue\",\n" + "          \"start_time\": \"5:30 AM\",\n"
				+ "          \"end_time\": \"8:00 PM\"\n" + "        },\n" + "        {\n"
				+ "          \"day\": \"Wed\",\n" + "          \"start_time\": \"5:30 AM\",\n"
				+ "          \"end_time\": \"8:00 PM\"\n" + "        },\n" + "        {\n"
				+ "          \"day\": \"Thu\",\n" + "          \"start_time\": \"5:30 AM\",\n"
				+ "          \"end_time\": \"8:00 PM\"\n" + "        },\n" + "        {\n"
				+ "          \"day\": \"Fri\",\n" + "          \"start_time\": \"5:30 AM\",\n"
				+ "          \"end_time\": \"8:00 PM\"\n" + "        },\n" + "        {\n"
				+ "          \"day\": \"Sat\",\n" + "          \"start_time\": \"6:30 AM\",\n"
				+ "          \"end_time\": \"8:00 PM\"\n" + "        },\n" + "        {\n"
				+ "          \"day\": \"Sun\",\n" + "          \"start_time\": \"6:30 AM\",\n"
				+ "          \"end_time\": \"6:00 PM\"\n" + "        }\n" + "      ]\n" + "    },\n"
				+ "    \"tax_rate\": \"" + tax_rate + "\",\n" + "    \"time_zone\": \"America/Los_Angeles\",\n"
				+ "    \"generate_barcodes\": true,\n" + "    \"enable_daily_redemption_report\": true,\n"
				+ "    \"enable_weekly_redemption_report\": true\n" + "  }\n" + "}";

		return payload;
	}

	public String subscriptionTaxesPayload(String client, String planId, String locationId) {
		String payload = "{\"client\":\"" + client + "\",\"plan_id\":" + planId + ",\"location_id\":" + locationId
				+ "}";
		return payload;
	}

	public String mobileSubscriptionTaxesPayload(String planId, String locationId) {
		String payload = "{\"plan_id\":" + planId + ",\"location_id\":" + locationId + "}";
		return payload;
	}

	public String createLocationGroup(String name, String store_number, String location_id) {

		String payload = "{\n" + "  \"name\": \"" + name + "\",\n" + "  \"store_number\": \"" + store_number + "\",\n"
				+ "  \"location_id\": \"" + location_id + "\"\n" + "}";

		return payload;
	}

	public String updateLocationGroup(String locationGroupName, String locationGroup_id) {

		String payload = "{\n" + "  \"location_group_id\": \"" + locationGroup_id + "\",\n" + "  \"name\": \""
				+ locationGroupName + "\"\n" + "}";

		return payload;
	}

	public String deleteLocationGroup(String locationGroup_id) {
		String payload = "{\n" + "  \"location_group_id\": \"" + locationGroup_id + "\"\n" + "}";
		return payload;
	}

	// public String adddLocationtoGroup() {
	//
	// String payload = "{\n" + " \"location_group_id\": \"16682\",\n" + "
	// \"store_number\": \"12235\",\n"
	// + " \"location_id\": \"385469\"\n" + "}";
	// return payload;
	//
	// }

	public String adddLocationtoGroup(String location_group_id, String store_number, String location_id) {
		String payload = "{\n" + "  \"location_group_id\": \"" + location_group_id + "\",\n" + "  \"store_number\": \""
				+ store_number + "\",\n" + "  \"location_id\": \"" + location_id + "\"\n" + "}";
		return payload;
	}

	// public String deleteLocationFromGroup() {
	//
	// String payload = "{\n" + " \"location_group_id\": \"16682\",\n" + "
	// \"location_id\": 385469,\n"
	// + " \"store_number\": \"12235\"\n" + "}";
	// return payload;
	// }

	public String deleteLocationFromGroup(String location_group_id, String store_number, String location_id) {
		String payload = "{\n" + "  \"location_group_id\": \"" + location_group_id + "\",\n" + "  \"location_id\": "
				+ location_id + ",\n" + "  \"store_number\": \"" + store_number + "\"\n" + "}";
		return payload;
	}

	public String deleteLocation(String location_id, String store_number) {

		String payload = "{\n" + "  \"location_id\": \"" + location_id + "\",\n" + "  \"store_number\": \""
				+ store_number + "\"\n" + "}";

		return payload;

	}

	public String supportGiftingToUser(String userID) {

		String payload = "{\n" + "  \"user_id\": " + userID + ",\n" + "  \"subject\": \"Gifts from automation\",\n"
				+ "  \"message\": \"Thank you for contacting us. Here are 50 extra points to make your day from automation test.\",\n"
				+ "  \"gift_reason\": \"api test\",\n" + "  \"gift_count\": 50\n" + "}";

		return payload;

	}

	public String updateUser(String user_id, String email, String location_id) {

		String payload = "{\n" + "  \"id\": \"" + user_id + "\",\n" + "  \"email\": \"" + email + "\",\n"
				+ "  \"user\": {\n" + "    \"email\": \"" + email + "\",\n" + "    \"last_name\": \"Kumar\",\n"
				+ "    \"first_name\": \"Ravi\",\n" + "    \"preferred_location_ids\": [\n" + "      " + location_id
				+ "\n" + "    ]\n" + "  }\n" + "}";

		return payload;

	}

	public Map<String, Object> updateUserPhone(String user_id, String email, String phone) {
		Faker faker = new Faker();
		Map<String, Object> payload = new HashMap<>();
		payload.put("id", user_id);
		payload.put("email", email);

		Map<String, Object> userMap = new HashMap<>();
		userMap.put("phone", phone);
		userMap.put("first_name", faker.name().firstName());
		userMap.put("last_name", faker.name().lastName());
		payload.put("user", userMap);
		return payload;
	}

	public String userExport(String user_id) {

		String payload = "{\n" + "  \"email_admin_only\": true,\n" + "  \"user_id\": " + user_id + "\n" + "}";

		return payload;

	}

	public String deleteUser(String user_id) {

		String payload = "{\n" + "  \"user_id\": \"" + user_id + "\",\n" + "  \"reason\": \"delete_general\"\n" + "}";

		return payload;

	}

	public Map<String, Object> anonymiseUser(String user_id) {
		Map<String, Object> payload = new HashMap<>();
		payload.put("user_id", user_id);
		payload.put("reason", "anonymize_forget_me");
		return payload;
	}

	// @author=Rajasekhar
	public String Api1CreateCurbsideOrderPayload(String email, String orderId, String timeReady, String deliveryMethod,
			String vechicleid) {
		String payload = "{\n" + " \"customer\": {\n" + "  \"contactNumber\": \"9912435465\",\n"
				+ "  \"customerId\": 63777,\n" + "  \"email\": \"" + email + "\",\n" + "  \"firstName\": \"Fname\",\n"
				+ "  \"lastName\": \"Lname \"\n" + "  },\n" + " \"deliveryMethod\": \"" + deliveryMethod + "\",\n"
				+ " \"handoff_mode\":\"\",\n" + "  \"external_order_id\":\"" + orderId + "\",\n" + " \"items\": [\n"
				+ "  {\n" + "   \"description\": \"Four Chocolate Chunk Cookies Double Chocolate\", \n"
				+ "   \"quantity\": 1, \n" + "   \"selling_price\": 5},\n"
				+ "  { \"description\": \"2 Chocolate Chunk Cookies\", \n" + "\"quantity\": 1, \n"
				+ "\"selling_price\": 5\n" + "     }\n" + "       ],\n" + "\n" + " \"storeNumber\": \"66233\",\n"
				+ " \"timePlaced\": \"20210915 19:14\",\n" + " \"timeReady\": \"" + timeReady + "\",\n"
				+ " \"storeUtcOffset\":5.5 ,\n" + " \"timeWanted\": \"20210917 16:28\",\n" + " \"totals\": {\n"
				+ "  \"discount\": 1,\n" + "  \"feesTotal\": 1,\n" + "  \"salesTax\": 1,\n" + "  \"subTotal\": 1,\n"
				+ "  \"tip\": 1,\n" + "  \"total\":10.99\n" + "  },\n" + " \"user_location\": {\n"
				+ "		\"long\": \"74.94873046875001\",\n" + "		\"lat\": \"26.52956523826758\",\n"
				+ "		\"status\": \"claimed\",\n" + "		\"distance\": \"\",\n" + "		\"arrival_time\": \"\",\n"
				+ "		\"location_service\": false,\n" + "    \"contextual_arrival_info\": \"1B\"\n"
				+ "  },\"is_replacement\": true,\n" + "  \"clientPlatform\":\"iOS\",\n" + " \"vehicle_id\": \""
				+ vechicleid + "\",\n" + " \"olo_uuid\":17689090933784,\n" + " \"external_order_uuid\": 9879879871034}";
		return payload;
	}

	// @author=Rajasekhar
	public String Api1CreatePickupOrderPayload(String email, String orderId, String timeReady, String deliveryMethod,
			String StoreNumber) {
		String payload = "{\n" + " \"customer\": {\n" + "  \"contactNumber\": \"9912435465\",\n"
				+ "  \"customerId\": 63777,\n" + "  \"email\": \"" + email + "\",\n" + "  \"firstName\": \"Fname\",\n"
				+ "  \"lastName\": \"Lname \"\n" + "  },\n" + " \"deliveryMethod\": \"" + deliveryMethod + "\",\n"
				+ " \"handoff_mode\":\"curbside\",\n" + "  \"external_order_id\":\"" + orderId + "\",\n"
				+ " \"items\": [\n" + "  {\n"
				+ "   \"description\": \"Four Chocolate Chunk Cookies Double Chocolate\", \n" + "   \"quantity\": 1, \n"
				+ "   \"selling_price\": 5},\n" + "  { \"description\": \"2 Chocolate Chunk Cookies\", \n"
				+ "\"quantity\": 1, \n" + "\"selling_price\": 5\n" + "     }\n" + "       ],\n" + "\n"
				+ " \"storeNumber\": \"" + StoreNumber + "\",\n" + " \"timePlaced\": \"20210915 19:14\",\n"
				+ " \"timeReady\": \"" + timeReady + "\",\n" + " \"storeUtcOffset\":5.5 ,\n"
				+ " \"timeWanted\": \"20210917 16:28\",\n" + " \"totals\": {\n" + "  \"discount\": 1,\n"
				+ "  \"feesTotal\": 1,\n" + "  \"salesTax\": 1,\n" + "  \"subTotal\": 1,\n" + "  \"tip\": 1,\n"
				+ "  \"total\":10.99\n" + "  },\n" + " \"user_location\": {\n"
				+ "		\"long\": \"74.94873046875001\",\n" + "		\"lat\": \"26.52956523826758\",\n"
				+ "		\"status\": \"claimed\",\n" + "		\"distance\": \"\",\n" + "		\"arrival_time\": \"\",\n"

				+ "		\"location_service\": false,\n" + "    \"contextual_arrival_info\": \"1B\"\n"
				+ "  },\"is_replacement\": true,\n" + "  \"clientPlatform\":\"iOS\",\n"
				+ " \"vehicle_id\": \"123456\",\n" + " \"olo_uuid\":17689090933784,\n"
				+ " \"external_order_uuid\": 9879879871034}";
		return payload;
	}

	// @author=Rajasekhar
	public String ApiUpdateCustomerStatusPayload(String cus_status) {
		String payload = "{\n" + " \"user_location\": {\n" + "	\"long\": 74.94873046875001,\n "
				+ "	\"lat\": 26.52956523826758,\n" + "	\"status\": \"" + cus_status + "\", \n"
				+ "	\"distance\": \" \",\n" + "	\"location_service\": false },\n"
				+ " \"contextual_arrival_info\":\"\"}";
		return payload;
	}

	// @author=Rajasekhar
	public String Api1CreateVehiclePayload() {
		String payload = "{\r\n" + "  \"name\": \"CAR1\",\r\n" + "  \"make\": \"Hyundaii\",\r\n"
				+ "  \"model\": \"Creta\",\r\n" + "  \"color\": \"white\",\r\n" + "  \"is_default\": true\r\n" + "}";
		return payload;
	}

	// @author=Rajasekhar
	public String ApiUpdateCustomerStatusPayload1(String cus_status) {
		String payload = "{\n" + " \"user_location\": {\n" + "	\"long\": 74.94873046875001,\n "
				+ "	\"lat\": 26.52956523826758,\n" + "	\"status\": \"" + cus_status + "\", \n"
				+ "	\"distance\": \" \",\n" + "	\"location_service\": false },\n"
				+ " \"contextual_arrival_info\":\"12A\"}";
		return payload;
	}

	public String createBusinessMigrationUser(String userEmail) {
		String expirationDate = CreateDateTime.getFutureDate(30);
		String payload = "{\n" + "  \"name\": \"Incumbent Loyalty Program\",\n" + "  \"birthday\": \"1995-03-21\",\n"
				+ "  \"phone\": 9876543210,\n" + "  \"email\": \"" + userEmail + "\",\n"
				+ "  \"original_membership_no\": \"123456789\",\n"
				+ "  \"registration_date\": \"2008-10-26T23:59:59-07:00\",\n" + "  \"first_name\": \"John\",\n"
				+ "  \"last_name\": \"Doe\",\n" + "  \"original_phone\": \"9876543210\",\n"
				+ "  \"original_points\": 97,\n" + "  \"initial_points\": 3,\n" + "  \"migrated_rewards\": 10.9,\n"
				+ "  \"migrated_rewards_expiration_date\": \"" + expirationDate + "\",\n"
				+ "  \"marketing_pn_subscription\": \"true\",\n" + "  \"marketing_email_subscription\": \"false\",\n"
				+ "  \"address_line1\": \"201 San Antonio Circle, Suite 250\",\n" + "  \"city\": \"Mountain View\",\n"
				+ "  \"state\": \"California\",\n" + "  \"zip_code\": 94040\n" + "}";

		return payload;

	}

	public String updateBusinessMigrationUser(String userEmail) {
		String expirationDate = CreateDateTime.getFutureDate(30);
		String payload = "{\n" + "  \"name\": \"Incumbent Loyalty Program\",\n" + "  \"birthday\": \"1995-03-21\",\n"
				+ "  \"phone\": 9876543210,\n" + "  \"email\": \"" + userEmail + "\",\n"
				+ "  \"original_membership_no\": \"123456789\",\n"
				+ "  \"registration_date\": \"2008-10-26T23:59:59-07:00\",\n" + "  \"first_name\": \"Johny\",\n"
				+ "  \"last_name\": \"English\",\n" + "  \"original_phone\": \"9876543210\",\n"
				+ "  \"original_points\": 97,\n" + "  \"initial_points\": 3,\n" + "  \"migrated_rewards\": 10.9,\n"
				+ "  \"migrated_rewards_expiration_date\": \"" + expirationDate + "\",\n"
				+ "  \"marketing_pn_subscription\": \"true\",\n" + "  \"marketing_email_subscription\": \"false\",\n"
				+ "  \"address_line1\": \"201 San Antonio Circle, Suite 250\",\n" + "  \"city\": \"Mountain View\",\n"
				+ "  \"state\": \"California\",\n" + "  \"zip_code\": 94040\n" + "}";

		return payload;

	}

	public String createBusinessAdmin(String userEmail, String role_id) {

		String payload = "{\n" + "  \"business_admin\": {\n" + "    \"email\": \"" + userEmail + "\",\n"
				+ "    \"first_name\": \"Test Name\",\n" + "    \"last_name\": \"Admin\",\n"
				+ "    \"timezone\": \"America/Los_Angeles\",\n" + "    \"role_id\": " + role_id + ",\n"
				+ "    \"read_only\": false,\n" + "    \"suspend\": false\n" + "  }\n" + "}";

		return payload;

	}

	public String updateBusinessAdmin(String business_admin_id, String userEmail, String role_id) {

		String payload = "{\n" + "  \"business_admin_id\": \"" + business_admin_id + "\",\n"
				+ "  \"business_admin\": {\n" + "    \"email\": \"" + userEmail + "\",\n"
				+ "    \"first_name\": \"Test Name\",\n" + "    \"last_name\": \"Admin Name\",\n"
				+ "    \"timezone\": \"America/Los_Angeles\",\n" + "    \"role_id\": " + role_id + ",\n"
				+ "    \"read_only\": false,\n" + "    \"suspend\": false\n" + "  }\n" + "}";

		return payload;

	}

	public String showBusinessAdmin(String business_admin_id) {

		String payload = "{\n" + "  \"business_admin_id\": \"" + business_admin_id + "\"\n" + "}";

		return payload;

	}

	public String inviteBusinessAdmin(String userEmail, String role_id) {

		String payload = "{\n" + "  \"business_admin\": {\n" + "    \"email\": \"" + userEmail + "\",\n"
				+ "    \"first_name\": \"Test Name\",\n" + "    \"last_name\": \"Admin\",\n"
				+ "    \"timezone\": \"America/Los_Angeles\",\n" + "    \"role_id\": " + role_id + ",\n"
				+ "    \"read_only\": false,\n" + "    \"suspend\": false\n" + "  }\n" + "}";

		return payload;

	}

	public String eClubGuestUpload(String userEmail, String storeNumber) {

		String payload = "{\n" + "  \"store_number\": \"" + storeNumber + "\",\n" + "  \"source\": \"test\",\n"
				+ "  \"user\": {\n" + "    \"email\": \"" + userEmail + "\",\n"
				+ "    \"first_name\": \"FIRST_NAME_GOES_HERE\",\n" + "    \"last_name\": \"LAST_NAME_GOES_HERE\",\n"
				+ "    \"phone\": \"\",\n" + "    \"address_line1\": \"\",\n" + "    \"state\": \"\",\n"
				+ "    \"zip_code\": \"302001\",\n" + "    \"birthday\": \"1999-01-01\",\n"
				+ "    \"program_anniversary\": \"2008-10-26T23:59:59-07:00\",\n"
				+ "    \"marketing_email_subscription\": \"1\",\n" + "    \"active_registration\": \"0\",\n"
				+ "    \"send_compliance_sms\": \"0\",\n" + "    \"title\": \"Mr.\",\n" + "    \"gender\": \"Male\",\n"
				+ "    \"Test2\": \"Oreo|Kit Kat\"\n" + "  }\n" + "}";

		return payload;

	}

	public String cReateFranchise(String userEmail) {

		String payload = "{\n" + "  \"franchisee\": {\n" + "    \"name\": \"New Franchisee\",\n" + "    \"email\": \""
				+ userEmail + "\",\n" + "    \"phone\": \"1111111111\",\n"
				+ "    \"address\": \"New Franchisee Address\",\n" + "    \"state\": \"New Franchisee State\",\n"
				+ "    \"zip_code\": \"909887\",\n" + "    \"enable_guest_upload\": true\n" + "  }\n" + "}";

		return payload;

	}

	public String uPdateFranchise(String userEmai, String franchisee_id) {

		String payload = "{\n" + "  \"id\": \"" + franchisee_id + "\",\n" + "  \"franchisee\": {\n"
				+ "    \"name\": \"New Updated Franchisee\",\n" + "    \"email\": \"" + userEmai + "\",\n"
				+ "    \"phone\": \"1010101010\",\n" + "    \"address\": \"New Franchisee Address\",\n"
				+ "    \"state\": \"New Franchisee State\",\n" + "    \"zip_code\": \"909887\",\n"
				+ "    \"enable_guest_upload\": true\n" + "  }\n" + "}";

		return payload;

	}

	public String dEleteFranchise(String franchisee_id) {

		String payload = "{\n" + "  \"id\": \"" + franchisee_id + "\"\n" + "}";

		return payload;

	}

	public String cReateSocialcauseCampaign(String campaignName) {

		String payload = "{\n" + "  \"social_cause_campaign\": {\n" + "    \"name\": \"" + campaignName + "\",\n"
				+ "    \"description\": \"social_cause_campaign_description\",\n" + "    \"city\": \"test_city\",\n"
				+ "    \"state\": \"Rajasthan\",\n" + "    \"street\": \"ADDRESS_GOES_HERE\",\n"
				+ "    \"zip\": \"302001\",\n" + "    \"phone_number\": \"1111111111\",\n"
				+ "    \"email\": \"test@example.com\",\n" + "    \"address\": \"Test Address\"\n" + "  }\n" + "}";

		return payload;

	}

	public String activateSocialCampaign(String campaignId) {

		String payload = "{\n" + "  \"social_cause_id\": \"" + campaignId + "\"\n" + "}";

		return payload;

	}

	public String enrollGuestForWifi(String email, String client, String location_id) {

		String payload = "{\n" + "  \"client\": \"" + client + "\",\n" + "  \"location_id\": " + location_id + ",\n"
				+ "  \"user\": {\n" + "    \"email\": \"" + email + "\",\n" + "    \"first_name\": \"FIRST_NAME\",\n"
				+ "    \"last_name\": \"LAST_NAME\",\n" + "    \"phone\": \"1111111111\",\n"
				+ "    \"address_line1\": \"ADDRESS\",\n" + "    \"state\": \"California\",\n"
				+ "    \"zip_code\": \"94040\",\n" + "    \"birthday\": \"1999-01-01\",\n"
				+ "    \"marketing_email_subscription\": \"1\"\n" + "  }\n" + "}";

		return payload;

	}

	public String processRedemption(String redemptioncode, String location_id) {

		String payload = "{\n" + "  \"redemption_code\": \"" + redemptioncode + "\",\n" + "  \"location_id\": "
				+ location_id + "\n" + "}";

		return payload;
	}

	public String forceRedeem(String rewardId, String userId) {

		String payload = "{\n" + "  \"user_id\": " + userId + ",\n" + "  \"redemption\": {\n"
				+ "    \"requested_punches\": 10,\n" + "    \"force_message\": \"This is a test\",\n"
				+ "    \"reward_id\":  \"" + rewardId + "\"\n" + "  }\n" + "}";

		return payload;
	}

	public String API2CreateDonation(String client, String social_cause_id) {

		String payload = "{\n" + "  \"client\": \"" + client + "\",\n" + "  \"social_cause_id\": \"" + social_cause_id
				+ "\",\n" + "  \"donation_type\": \"currency\",\n" + "  \"item_to_donate\": 5\n" + "}";
		return payload;

	}

	public String api2PurchaseSubscriptionPayloadRenew(String subscription_id, String purchase_price) {

		String startTime = CreateDateTime.getCurrentDate() + " 21:09:38";
		String endTime = CreateDateTime.getFutureDate() + " 21:09:38";

		String payload = "{\"subscription_id\": " + subscription_id + ", \"start_time\": \"" + startTime
				+ "\", \"end_time\": \"" + endTime + "\" ,\"purchase_price\": " + purchase_price + "\n" + "}";

		return payload;

	}

	public String discountBasketItemsAttributes(String discount_type, String discount_id) {

		String payLoad = "{\"discount_basket_items_attributes\": [{\"discount_type\": \"" + discount_type
				+ "\",\"discount_id\": \"" + discount_id + "\"}]}";

		return payLoad;
	}

	public String discountAmountItemPayload(String discount_type, String discount_amount) {

		String payLoad = "{\"discount_basket_items_attributes\": [{\"discount_type\": \"" + discount_type
				+ "\",\"discount_value\": \"" + discount_amount + "\"}]}";

		return payLoad;
	}

	// sha or POS API
	public String discountBasketItemsAttributes(String userID, String discount_type, String discount_id) {

		String payLoad = "{\n" + "    \"user_id\": \"" + userID + "\",\n" + "    \n"
				+ "    \"discount_basket_items_attributes\": [\n" + "       {\n" + "        \"discount_type\": \""
				+ discount_type + "\",\n" + "        \"discount_id\": \"" + discount_id + "\"\n" + "        }\n"
				+ "    ]\n" + "}\n" + "";

		return payLoad;
	}

	// sha or POS API
	public String discountBasketItemsAttributesDiscountAmpountPOS(String userID, String discount_type,
			String discount_value) {

		String payLoad = "{\n" + "    \"user_id\": \"" + userID + "\",\n" + "    \n"
				+ "    \"discount_basket_items_attributes\": [\n" + "       {\n" + "        \"discount_type\": \""
				+ discount_type + "\",\n" + "        \"discount_value\": \"" + discount_value + "\"\n" + "        }\n"
				+ "    ]\n" + "}\n" + "";
		return payLoad;

	}

	public String deleteDiscountBasketPayload(String basketID) {
		String payload = "{\"discount_basket_item_ids\":[\"" + basketID + "\"]}";
		return payload;
	}

	public String deleteDiscountBasketPayload(String userID, String basketID) {
		String payload = "{\"user_id\":\"" + userID + "\",\"discount_basket_item_ids\":[\"" + basketID + "\"]}";
		return payload;
	}

	public String getBatchRedemptionPayloadOMMM(String locationKey, String userID, String item_id) {

		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String payLoad = "{\"location_key\":\"" + locationKey
				+ "\",\"line_items\":[{\"item_name\":\"Pizza1\",\"item_qty\":1,\"amount\":10,\"item_type\":\"M\",\"item_id\":\""
				+ item_id
				+ "\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1}],\"receipt_datetime\":\"2023-01-20T07:17:00Z\",\"subtotal_amount\":10,\"receipt_amount\":10,\"punchh_key\":\""
				+ punch_Key + "\",\"transaction_no\":" + transaction_no + ",\"user_id\":\"" + userID + "\"}";

		logger.info("payLoad for batch redemption :: - " + payLoad);

		return payLoad;

	}

	public String v5RedemptionWithRewardId(String reward_id) {

		String payload = "{\n" + "    \"discount_type\": \"reward\",\n" + "    \"reward_id\": \"" + reward_id + "\".\n"
				+ "}";
		return payload;
	}

	public String v5RedemptionWithRedemptionId(String redeemable_id) {

		String payload = "{\n" + "    \"discount_type\": \"redeemable\",\n" + "    \"redeemable_id\": \""
				+ redeemable_id + "\".\n" + "}";
		return payload;
	}

	public String sendPointsToUser(String userId, String gift_count) {

		String payload = "{\n" + "		  \"user_id\": \"" + userId + "\",\n"
				+ "		  \"subject\": \"Send Message to user\",\n"
				+ "		  \"message\": \"Thank you for contacting us. Enjoy your reward\",\n"
				+ "		  \"gift_reason\": \"Api test\",\n" + "		  \"reward_amount\": \"\",\n"
				+ "		  \"gift_count\": \"" + gift_count + "\",\n" + "		  \"redeemable_id\": \"\",\n"
				+ "		  \"end_date\": \"\"\n" + "}";
		return payload;
	}

	public String userLookUpApi() {
		// TODO Auto-generated method stub
		return null;
	}

	public static String onlineCheckinModifiedPayLoad(String order_mode, String client_platform,
			String authenticationToken, String amount, String client, String transactionNumber, String externalUid) {
		Calendar cal = Calendar.getInstance();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		cal.add(Calendar.DATE, -1);
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		String payload = "{\n" + "	\"order_mode\": \"" + order_mode + "\",\n" + "	\"client_platform\": \""
				+ client_platform + "\",\n" + "	\"authentication_token\": \"" + authenticationToken + "\",\n"
				+ "	\"receipt_amount\": " + amount + ",\n" + "	\"cc_last4\": 4389,\n" + "	\"menu_items\": [{\n"
				+ "			\"item_name\": \"White rice\",\n" + "			\"item_qty\": 1,\n"
				+ "			\"item_amount\": 100,\n" + "			\"menu_item_type\": \"M\",\n"
				+ "			\"menu_item_id\": 110,\n" + "			\"menu_family\": \"106\",\n"
				+ "			\"menu_major_group\": \"100\",\n" + "			\"serial_number\": \"1.0\"\n"
				+ "		},\n" + "		{\n" + "			\"item_name\": \"Brown rice\",\n"
				+ "			\"item_qty\": 1,\n" + "			\"item_amount\": 7.86,\n"
				+ "			\"menu_item_type\": \"M\",\n" + "			\"menu_item_id\": 111,\n"
				+ "			\"menu_family\": \"106\",\n" + "			\"menu_major_group\": \"100\",\n"
				+ "			\"serial_number\": \"2.0\"\n" + "		},\n" + "		{\n"
				+ "			\"item_name\": \"Free rice\",\n" + "			\"item_qty\": 1,\n"
				+ "			\"item_amount\": 100,\n" + "			\"menu_item_type\": \"D\",\n"
				+ "			\"menu_item_id\": 3419,\n" + "			\"menu_family\": \"800\",\n"
				+ "			\"menu_major_group\": \"152\",\n" + "			\"serial_number\": \"3.0\"\n" + "		}\n"
				+ "	],\n" + "	\"subtotal_amount\": " + amount + ",\n" + "	\"receipt_datetime\": \"" + recieptDateTime
				+ "\",\n" + "	\"transaction_no\": \"" + transactionNumber + "\",\n" + "	\"external_uid\": \""
				+ externalUid + "\",\n" + "	\"client\": \"" + client + "\",\n" + "	\"channel\": \"online_order\"\n"
				+ "}";
		return payload;
	}

	public String API2MarkMessagesRead(String client) {

		String payload = "{\n" + "	\"client\":\"" + client + "\"," + " \"user_rich_notifications\": \"812\" \n" + "}";

		return payload;

	}

	public String posApplicableOffers(String email, String amount) {
		Calendar cal = Calendar.getInstance();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		cal.add(Calendar.DATE, -1);
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		String payload = "{\n" + "\"email\": \"" + email + "\",\n" + "	\"receipt_amount\": " + amount + ",\n"
				+ "\"menu_items\":\n" + "[\n" + "    {\n" + "        \"item_name\":\"coffee\",\n"
				+ "        \"item_qty\": 1,\n" + "  \"item_amount\": 7,\n" + "        \"menu_item_type\":\"M\",\n"
				+ "        \"menu_item_id\":101,\n" + "        \"menu_family\":\"800\",\n"
				+ "        \"menu_major_group\":\"152\"\n" + "    } ,  \n" + "{\n" + "        \"item_name\":\"tea\",\n"
				+ "        \"item_qty\": 1,\n" + "  \"item_amount\": 7,\n" + "        \"menu_item_type\":\"M\",\n"
				+ "        \"menu_item_id\":101,\n" + "        \"menu_family\":\"800\",\n"
				+ "        \"menu_major_group\":\"152\"\n" + "    }   \n" + "],\n" + "\"subtotal_amount\":9.950,\n"
				+ "\"amount\":9.950,\n" + "	\"receipt_datetime\": \"" + recieptDateTime + "\"\n" + "}";
		return payload;
	}

	public String authApifetchRedemptionCode(String client, String location_id, String redeemed_points,
			String reward_id) {
		String payload = "{\n" + "  \"reward_id\": \"" + reward_id + "\",\n" + "  \"client\": \"" + client + "\",\n"
				+ "  \"redeemed_points\": \"" + redeemed_points + "\",\n" + "  \"location_id\": \"" + location_id
				+ "\"\n" + "}";
		return payload;
	}

	public String authApifetchAvailableOffersOfTheUser(String client, String auth_token) {
		String payload = "{\n" + "  \"authentication_token\": \"" + auth_token + "\",\n" + "  \"client\": \"" + client
				+ "\"\n" + "}";
		return payload;
	}

	public String authApiCreateLoyaltyCheckin(String client, String auth_token, String store_num, String barcode) {
		String payload = "{\n" + "  \"bar_code\" : \"" + barcode + "\",\n" + "  \"client\": \"" + client + "\",\n"
				+ "  \"store_number\": \"" + store_num + "\",\n" + "  \"authentication_token\": \"" + auth_token
				+ "\"\n" + "}";
		return payload;
	}

	public String authApiFetchACheckinByExternal_uid(String auth_token, String client, String external_uid) {
		String payload = "{\n" + "    \"authentication_token\": \"" + auth_token + "\",\n" + "    \"client\": \""
				+ client + "\",\n" + "    \"external_uid\": \"" + external_uid + "\"\n" + "}";
		return payload;
	}

	public String authUpdateLoyaltyCheckinPayload(String authToken, String client, String externalUid, String state) {
		String payload = "{\"authentication_token\": \"" + authToken + "\",\"client\":\"" + client
				+ "\",\"external_uid\":\"" + externalUid + "\",\"state\":\"" + state + "\"}";
		return payload;
	}

	public String authApiCreateAccessToken(String client, String security_token) {
		String payload = "{\n" + "  \"client\" : \"" + client + "\",\n" + "  \"security_token\" : \"" + security_token
				+ "\"\n" + "}";
		return payload;
	}

	public String authApiChangePassword(String client, String auth_token, String password) {
		String payload = "{\n" + "  \"user\": {\n" + "    \"password\": \"" + password + "\",\n"
				+ "    \"password_confirmation\": \"" + password + "\"\n" + "  },\n" + "  \"client\" : \"" + client
				+ "\",\n" + "  \"authentication_token\": \"" + auth_token + "\"\n" + "}";
		return payload;
	}

	public Map<String, Object> authApiChangePasswordAdvanceAuthPayload(String client, String password) {
		Map<String, Object> payloadMap = new HashMap<>();
		Map<String, String> userMap = new HashMap<>();
		userMap.put("password", password);
		userMap.put("password_confirmation", password);
		payloadMap.put("user", userMap);
		payloadMap.put("client", client);
		return payloadMap;
	}

	public String authApiresetPasswordTokenOfTheUser(String client, String email) {
		String payload = "{\n" + "  \"user\": {\n" + "    \"email\": \"" + email + "\",\n" + "  },\n"
				+ "  \"client\" : \"" + client + "\"\n" + "}";
		return payload;
	}

	public String authApiEstimatePointsEarning(String auth_token, String client, String item_amount,
			String receipt_amount, String subtotal_amount) {
		String payload = "{\"authentication_token\":\"" + auth_token + "\",\"client\":\"" + client
				+ "\",\"menu_items\":[{\"item_name\":\"White rice\",\"item_qty\":1,\"item_amount\":\"" + item_amount
				+ "\",\"menu_item_type\":\"M\",\"menu_item_id\":3419,\"menu_family\":\"800\",\"menu_major_group\":\"152\",\"serial_number\":\"1.0\",\"subtotal_amount\":\""
				+ subtotal_amount + "\",\"receipt_amount\":\"" + receipt_amount + "\"}]}";
		return payload;
	}

	public Map<String, String> authApiGetSSOToken(String auth_token, String client, String secret, String redirect_uri,
			String grant_type) {
		Map<String, String> payload = new HashMap<>();
		payload.put("code", auth_token);
		payload.put("client_id", client);
		payload.put("client_secret", secret);
		payload.put("redirect_uri", redirect_uri);
		payload.put("grant_type", grant_type);
		return payload;
	}

	public String authApiUserEnrollment(String item_id, String auth_token, String client) {
		String payload = "{\n" + " \"item_type\":\"social_cause_campaign\",\n" + " \"item_id\":\"" + item_id + "\",\n"
				+ " \"authentication_token\":\"" + auth_token + "\",\n" + "  \"client\" : \"" + client + "\"\n" + "}";
		return payload;
	}

	public String authApiEstimateLoyaltyPointsEarning(String auth_token, String client, String subtotal_amount) {
		String payload = "{\n" + "\"authentication_token\":\"" + auth_token + "\",\n" + "\"client\":\"" + client
				+ "\",\n" + "\"subtotal_amount\" : \"" + subtotal_amount + "\"\n" + "}";
		return payload;
	}

	public String authApiBalanceTimelines(String auth_token, String client) {
		String payload = "{\n" + "  \"authentication_token\": \"" + auth_token + "\",\n" + "  \"client\": \"" + client
				+ "\"\n" + "}";
		return payload;
	}

	public String authApiPointConversionAPI(String auth_token, String client, String conversion_rule_id,
			String converted_value, String source_value, String social_cause_campaign_id) {
		String payload = "{\n" + "  \"authentication_token\": \"" + auth_token + "\",\n" + "  \"client\": \"" + client
				+ "\",\n" + "  \"conversion_rule_id\": \"" + conversion_rule_id + "\",\n" + "  \"converted_value\": \""
				+ converted_value + "\",\n" + "  \"source_value\": \"" + source_value + "\",\n"
				+ "  \"social_cause_campaign_id\": \"" + social_cause_campaign_id + "\"\n" + "}";
		return payload;
	}

	public String authApiGetTheDealDetail(String auth_token, String client, String redeemable_uuid) {
		String payload = "{\n" + "  \"authentication_token\": \"" + auth_token + "\",\n" + "  \"client\": \"" + client
				+ "\",\n" + "  \"redeemable_uuid\": \"" + redeemable_uuid + "\"\n" + "}";
		return payload;
	}

	// Used for test case OMM-T78 (1.0)
	public String getBatchRedemptionPayloadOMMMNew(String locationKey, String userID) {

		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String payLoad = "{\"location_key\":\"" + locationKey
				+ "\",\"line_items\":[{\"item_name\":\"Pizza1\",\"item_qty\":1,\"amount\":\"10\",\"item_type\":\"M\",\"item_id\":\"101\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1},{\"item_name\":\"Pizza2\",\"item_qty\":1,\"amount\":\"7\",\"item_type\":\"M\",\"item_id\":\"101\",\"item_family\":\"10\",\"item_group\":\"888\",\"serial_number\":2},{\"item_name\":\"Coffee\",\"item_qty\":1,\"amount\":\"4\",\"item_type\":\"M\",\"item_id\":\"201\",\"item_family\":\"10\",\"item_group\":\"889\",\"serial_number\":3}],\"receipt_datetime\":\"2022-11-24T14:14:07+05:30\",\"subtotal_amount\":10,\"receipt_amount\":10,\"punchh_key\":\""
				+ punch_Key + "\",\"transaction_no\":" + transaction_no + ",\"user_id\":\"" + userID + "\"}";

		logger.info("payLoad for batch redemption :: - " + payLoad);

		return payLoad;

	}

	// Used for test case OMM-T78 (1.0)
	public String getBatchRedemptionPayloadOMMMNew(String locationKey, String userID, String subAmount) {

		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String payLoad = "{\"location_key\":\"" + locationKey
				+ "\",\"line_items\":[{\"item_name\":\"Pizza1\",\"item_qty\":1,\"amount\":\"10\",\"item_type\":\"M\",\"item_id\":\"101\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1},{\"item_name\":\"Pizza2\",\"item_qty\":1,\"amount\":\"7\",\"item_type\":\"M\",\"item_id\":\"101\",\"item_family\":\"10\",\"item_group\":\"888\",\"serial_number\":2},{\"item_name\":\"Coffee\",\"item_qty\":1,\"amount\":\"4\",\"item_type\":\"M\",\"item_id\":\"201\",\"item_family\":\"10\",\"item_group\":\"889\",\"serial_number\":3}],\"receipt_datetime\":\"2022-11-24T14:14:07+05:30\",\"subtotal_amount\":"
				+ subAmount + ",\"receipt_amount\":" + subAmount + ",\"punchh_key\":\"" + punch_Key
				+ "\",\"transaction_no\":" + transaction_no + ",\"user_id\":\"" + userID + "\"}";

		logger.info("payLoad for batch redemption :: - " + payLoad);

		return payLoad;

	}

	// Used for test case OMM-T78 (1.0)
	public String getBatchRedemptionPayloadOMMMForDollar1_Off(String locationKey, String userID, String subAmount) {

		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String payLoad = "{\"location_key\":\"" + locationKey
				+ "\",\"line_items\":[{\"item_name\":\"Pizza1\",\"item_qty\":1,\"amount\":\"8\",\"item_type\":\"M\",\"item_id\":\"101\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1},{\"item_name\":\"Pizza2\",\"item_qty\":1,\"amount\":\"2\",\"item_type\":\"M\",\"item_id\":\"101\",\"item_family\":\"10\",\"item_group\":\"888\",\"serial_number\":2},{\"item_name\":\"Coffee\",\"item_qty\":1,\"amount\":\"4\",\"item_type\":\"M\",\"item_id\":\"201\",\"item_family\":\"10\",\"item_group\":\"889\",\"serial_number\":3}],\"receipt_datetime\":\"2022-11-24T14:14:07+05:30\",\"subtotal_amount\":"
				+ subAmount + ",\"receipt_amount\":" + subAmount + ",\"punchh_key\":\"" + punch_Key
				+ "\",\"transaction_no\":" + transaction_no + ",\"user_id\":\"" + userID + "\"}";

		logger.info("payLoad for batch redemption :: - " + payLoad);

		return payLoad;

	}

	public String getBatchRedemptionPayloadOMMM(String locationKey, String userID, String item_id, String subAmount,
			String recpAmount1, String recpAmount2, String recpAmount3) {

		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String payLoad = "{\"location_key\":\"" + locationKey
				+ "\",\"line_items\":[{\"item_name\":\"Pizza1\",\"item_qty\":1,\"amount\":\"" + recpAmount1
				+ "\",\"item_type\":\"M\",\"item_id\":\"101\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1},{\"item_name\":\"Pizza2\",\"item_qty\":1,\"amount\":\""
				+ recpAmount2
				+ "\",\"item_type\":\"M\",\"item_id\":\"101\",\"item_family\":\"10\",\"item_group\":\"888\",\"serial_number\":2},{\"item_name\":\"Coffee\",\"item_qty\":1,\"amount\":\""
				+ recpAmount3
				+ "\",\"item_type\":\"M\",\"item_id\":\"201\",\"item_family\":\"10\",\"item_group\":\"889\",\"serial_number\":3}],\"receipt_datetime\":\"2022-11-24T14:14:07+05:30\",\"subtotal_amount\":"
				+ subAmount + ",\"receipt_amount\":" + subAmount + ",\"punchh_key\":\"" + punch_Key
				+ "\",\"transaction_no\":" + transaction_no + ",\"user_id\":\"" + userID + "\"}";

		logger.info("payLoad for batch redemption :: - " + payLoad);

		return payLoad;

	}

	public String generateReceipt(String locationKey, String userID, String subAmount,
			Map<String, Map<String, String>> parentMap) {
		String recieptPayload = "";
		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		// String transaction_no =
		// Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String head = "{\"location_key\":\"" + locationKey + "\",\"line_items\":[";

		String base = "],\"receipt_datetime\":\"2022-11-24T14:14:07+05:30\",\"subtotal_amount\":" + subAmount
				+ ",\"receipt_amount\":" + subAmount + ",\"punchh_key\":\"" + punch_Key + "\",\"transaction_no\":"
				+ punch_Key + ",\"user_id\":\"" + userID + "\"}";

		for (Entry<String, Map<String, String>> entry : parentMap.entrySet()) {
			Map<String, String> valuesMap = entry.getValue();
			String str = "{\"item_name\":\"" + valuesMap.get("item_name") + "\",\"item_qty\":"
					+ valuesMap.get("item_qty") + ",\"amount\":\"" + valuesMap.get("amount") + "\",\"item_type\":\""
					+ valuesMap.get("item_type") + "\",\"item_id\":\"" + valuesMap.get("item_id")
					+ "\",\"item_family\":\"" + valuesMap.get("item_family") + "\",\"item_group\":\""
					+ valuesMap.get("item_group") + "\",\"serial_number\":" + valuesMap.get("serial_number") + "},";
			recieptPayload = recieptPayload + str;
		}
		recieptPayload = recieptPayload.substring(0, recieptPayload.length() - 1);
		String finalRecieptPayLoad = head + recieptPayload + base;
		logger.info("Test3234.generateReceipt()finalRecieptPayLoad =" + finalRecieptPayLoad);
		return finalRecieptPayLoad;

	}

	public String deleteBasketItem_Payload(String basketItemID, String external_uid) {
		String payload = "{\n" + "  \"discount_basket_item_ids\": \"" + basketItemID + "\",\n"
				+ "  \"external_uid\": \"" + external_uid + "\"\n" + "}";
		return payload;
	}

	public String posAPIDiscountLookUpPayload(String locationKey, String userID, String item_id) {
		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));
		String payLoad = "{\"location_key\":\"" + locationKey
				+ "\",\"line_items\":[{\"item_name\":\"Pizza1\",\"item_qty\":1,\"amount\":10,\"item_type\":\"M\",\"item_id\":\""
				+ item_id
				+ "\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1}],\"receipt_datetime\":\"2023-01-20T07:17:00Z\",\"subtotal_amount\":10,\"receipt_amount\":10,\"punchh_key\":\""
				+ punch_Key + "\",\"transaction_no\":" + transaction_no + ",\"user_id\":\"" + userID + "\"}";
		logger.info("payLoad for batch redemption :: - " + payLoad);
		return payLoad;
	}

	public String getBatchRedemptionPayloadOMMMFor144_9(String locationKey, String userID, String Amount,
			String recpAmount1, String recpAmount2) {
		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));
		String payLoad = "{\"location_key\":\"" + locationKey
				+ "\",\"line_items\":[{\"item_name\":\"Coffee\",\"item_qty\":2,\"amount\":\"" + recpAmount1
				+ "\",\"item_type\":\"M\",\"item_id\":\"2001\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1},{\"item_name\":\"Pizza\",\"item_qty\":1,\"amount\":\""
				+ recpAmount2
				+ "\",\"item_type\":\"M\",\"item_id\":\"1001\",\"item_family\":\"10\",\"item_group\":\"888\",\"serial_number\":2}],\"receipt_datetime\":\"2022-11-24T14:14:07+05:30\",\"subtotal_amount\":"
				+ Amount + ",\"receipt_amount\":" + Amount + ",\"punchh_key\":\"" + punch_Key + "\",\"transaction_no\":"
				+ transaction_no + ",\"user_id\":\"" + userID + "\"}";
		System.out.println("payLoad for batch redemption :: - " + payLoad);
		return payLoad;
	}

	public String pointForceRedeemPayload(String userId, String points) {

		String payload = "{\"user_id\":" + userId + ",\"redemption\":{\"requested_punches\":" + points
				+ ",\"force_message\":\"This is a test\"}}";

		return payload;
	}

	public String getBatchRedemptionPayloadWithQueryTrueAuth(String locationKey, String userID, String item_id,
			String external_uid) {

		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String payLoad = "{\n" + "    \"location_key\": \"" + locationKey + "\",\n" + "    \"line_items\": [\n"
				+ "        {\n" + "            \"item_name\": \"Pizza3\",\n" + "            \"item_qty\": 1,\n"
				+ "            \"amount\": \"4\",\n" + "            \"item_type\": \"M\",\n"
				+ "            \"item_id\": \"" + item_id + "\",\n" + "            \"item_family\": \"106\",\n"
				+ "            \"item_group\": \"999\",\n" + "            \"serial_number\": 1.2\n" + "        }\n"
				+ "    ],\n" + "    \"receipt_datetime\": \"2023-06-26T07:17:00Z\",\n"
				+ "    \"subtotal_amount\": 21,\n" + "    \"receipt_amount\": 30,\n" + "    \"punchh_key\": \""
				+ punch_Key + "\",\n" + "    \"transaction_no\": " + transaction_no + ",\n" + "    \"query\":true,\n"
				+ "   \"external_uid\":\"" + external_uid + "\",\n" + "    \"user_id\": \"" + userID + "\"\n" + "}";
		return payLoad;

	}

	public String getBatchRedemptionPayloadOMMM150(String locationKey, String userID, String Amount, String qty,
			String id) {

		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String payLoad = "{\"location_key\":\"" + locationKey

				+ "\",\"line_items\":[{\"item_name\":\"Pizza1\",\"item_qty\":" + qty + ",\"amount\":\"" + Amount
				+ "\",\"item_type\":\"M\",\"item_id\":\"" + id
				+ "\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1}],\"receipt_datetime\":\"2022-11-24T14:14:07+05:30\",\"subtotal_amount\":"
				+ Amount + ",\"receipt_amount\":" + Amount + ",\"punchh_key\":\"" + punch_Key + "\",\"transaction_no\":"
				+ transaction_no + ",\"user_id\":\"" + userID + "\"}";

		System.out.println("payLoad for batch redemption :: - " + payLoad);

		return payLoad;

	}

	public String getBatchRedemptionPayloadOMMM198(String locationKey, String userID, String subAmount,
			String recpAmount1, String recpAmount2, String recpAmount3) {

		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String payLoad = "{\"location_key\":\"" + locationKey
				+ "\",\"line_items\":[{\"item_name\":\"Pizza1\",\"item_qty\":2,\"amount\":\"" + recpAmount1
				+ "\",\"item_type\":\"M\",\"item_id\":\"101\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1},{\"item_name\":\"Pizza2\",\"item_qty\":1,\"amount\":\""
				+ recpAmount2
				+ "\",\"item_type\":\"M\",\"item_id\":\"101\",\"item_family\":\"10\",\"item_group\":\"888\",\"serial_number\":2},{\"item_name\":\"Coffee\",\"item_qty\":1,\"amount\":\""
				+ recpAmount3
				+ "\",\"item_type\":\"M\",\"item_id\":\"201\",\"item_family\":\"10\",\"item_group\":\"889\",\"serial_number\":3}],\"receipt_datetime\":\"2022-11-24T14:14:07+05:30\",\"subtotal_amount\":"
				+ subAmount + ",\"receipt_amount\":" + subAmount + ",\"punchh_key\":\"" + punch_Key
				+ "\",\"transaction_no\":" + transaction_no + ",\"user_id\":\"" + userID + "\"}";

		System.out.println("payLoad for batch redemption :: - " + payLoad);

		return payLoad;
	}

	public String getDiscountLookupPayloadOMMM(String locationKey, String userID, String subAmount, String recpAmount1,
			String recpAmount2, String recpAmount3) {

		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String payLoad = "{\"location_key\":\"" + locationKey
				+ "\",\"line_items\":[{\"item_name\":\"Pizza1\",\"item_qty\":1,\"amount\":\"" + recpAmount1
				+ "\",\"item_type\":\"M\",\"item_id\":\"101\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1},{\"item_name\":\"Pizza2\",\"item_qty\":1,\"amount\":\""
				+ recpAmount2
				+ "\",\"item_type\":\"M\",\"item_id\":\"101\",\"item_family\":\"10\",\"item_group\":\"888\",\"serial_number\":2},{\"item_name\":\"Coffee\",\"item_qty\":1,\"amount\":\""
				+ recpAmount3
				+ "\",\"item_type\":\"M\",\"item_id\":\"201\",\"item_family\":\"10\",\"item_group\":\"889\",\"serial_number\":3}],\"receipt_datetime\":\"2022-11-24T14:14:07+05:30\",\"subtotal_amount\":"
				+ subAmount + ",\"receipt_amount\":" + subAmount + ",\"punchh_key\":\"" + punch_Key
				+ "\",\"transaction_no\":" + transaction_no + ",\"user_id\":\"" + userID + "\"}";

		System.out.println("payLoad for batch redemption :: - " + payLoad);

		return payLoad;
	}

	public String singleScanPaypalBAZPayload() {
		String payLoad = "{\"payment_type\":\"PaypalBA\"}";
		System.out.println("payLoad for singleScanPaypalBA :: - " + payLoad);
		return payLoad;
	}

	public String userLookupPayload(String code) {
		String payLoad = "{\"lookup_field\":\"single_scan_code\",\"lookup_value\":\"" + code + "\"}";
		System.out.println("payLoad for singleScanPaypalBA :: - " + payLoad);
		return payLoad;
	}

	public String singleScanGiftCardPayload(String uuid) {
		String payLoad = "{\"payment_type\":\"GiftCard\",\"gift_card_uuid\":\"" + uuid + "\"}";
		System.out.println("payLoad for singleScanPaypalBA :: - " + payLoad);
		return payLoad;
	}

	public String singleScanCreditCardPayload() {
		String payLoad = "{\"payment_type\":\"CreditCard\",\"transaction_token\":\"123\"}";
		System.out.println("payLoad for singleScanPaypalBA :: - " + payLoad);
		return payLoad;
	}

	public String singleScanrewardIDPayload(String id) {
		String payLoad = "{\"reward_id\":\"" + id + "\"}";
		System.out.println("payLoad for singleScanPaypalBA :: - " + payLoad);
		return payLoad;
	}

	public String singleScanRedeemableIDPayload(String id) {
		String payLoad = "{\"redeemable_id\":\"" + id + "\"}";
		System.out.println("payLoad for singleScanRedeemableIDPayload :: - " + payLoad);
		return payLoad;
	}

	public String singleScanEmptyPayload() {
		String payLoad = "{}";
		System.out.println("payLoad for singleScanEmptyPayload :: - " + payLoad);
		return payLoad;
	}

	public String getBatchRedemptionPayloadWithProcessTrueAuth(String locationKey, String userID, String item_id) {

		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String payLoad = "{\"location_key\":\"" + locationKey
				+ "\",\"line_items\":[{\"item_name\":\"Pizza1\",\"item_qty\":1,\"amount\":10,\"item_type\":\"M\",\"item_id\":\""
				+ item_id
				+ "\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1}],\"receipt_datetime\":\"2023-01-20T07:17:00Z\",\"subtotal_amount\":10,\"receipt_amount\":10,\"punchh_key\":\""
				+ punch_Key + "\",\"transaction_no\":" + transaction_no + ",\"user_id\":\"" + userID
				+ "\",\"process\":true\n" + "}";

		System.out.println("payLoad for batch redemption :: - " + payLoad);

		return payLoad;

	}

	public String getBatchRedemptionPayloadWithProcessValueAuth(String locationKey, String userID, String item_id,
			String processValue, String amount, String externalUid) {

		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String payLoad = "{\n" + "    \"location_key\": \"" + locationKey + "\",\n" + "    \"line_items\": [\n"
				+ "        {\n" + "            \"item_name\": \"Pizza1\",\n" + "            \"item_qty\": 1,\n"
				+ "            \"amount\": " + amount + ",\n" + "            \"item_type\": \"M\",\n"
				+ "            \"item_id\": \"" + item_id + "\",\n" + "            \"item_family\": \"10\",\n"
				+ "            \"item_group\": \"999\",\n" + "            \"serial_number\": 1\n" + "        }\n"
				+ "    ],\n" + "    \"receipt_datetime\": \"2023-01-20T07:17:00Z\",\n" + "    \"subtotal_amount\": "
				+ amount + ",\n" + "    \"receipt_amount\": " + amount + ",\n" + "    \"punchh_key\": \"" + punch_Key
				+ "\",\n" + "    \"transaction_no\": " + transaction_no + ",\n" + "    \"external_uid\": \""
				+ externalUid + "\",\n" + "    \"query\": " + processValue + ",\n" + "    \"user_id\": \"" + userID
				+ "\"\n" + "}";

		logger.info("payLoad for batch redemption :: - " + payLoad);

		return payLoad;

	}

	public String dynamicCoupunPayload(String email, String campaignUuid) {
		String payload = "{\n" + "    \"email\": \"" + email + "\",\n" + "    \"campaign_uuid\": \"" + campaignUuid
				+ "\"\n" + "}";
		return payload;
	}

	public String menuAdjusterInvalidPayload() {
		String payload = "{\"allow_expensive_items\": false,\"min_price\": }";
		return payload;
	}

	public String getBatchRedemptionPayloadOMMMFor111(String locationKey, String userID, String Amount, String qty,
			String qty1) {

		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String payLoad = "{\"location_key\":\"" + locationKey
				+ "\",\"line_items\":[{\"item_name\":\"Coffee\",\"item_qty\":" + qty
				+ ",\"amount\":\"10\",\"item_type\":\"M\",\"item_id\":\"2001\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1},{\"item_name\":\"Pizza\",\"item_qty\":"
				+ qty1
				+ ",\"amount\":\"10\",\"item_type\":\"M\",\"item_id\":\"1001\",\"item_family\":\"10\",\"item_group\":\"888\",\"serial_number\":2}],\"receipt_datetime\":\"2022-11-24T14:14:07+05:30\",\"subtotal_amount\":"
				+ Amount + ",\"receipt_amount\":" + Amount + ",\"punchh_key\":\"" + punch_Key + "\",\"transaction_no\":"
				+ transaction_no + ",\"user_id\":\"" + userID + "\"}";

		System.out.println("payLoad for batch redemption :: - " + payLoad);

		return payLoad;

	}

	public String getBatchRedemptionPayloadOMMMFor107(String locationKey, String userID, String Amount,
			String recpAmount1, String recpAmount2) {

		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String payLoad = "{\"location_key\":\"" + locationKey
				+ "\",\"line_items\":[{\"item_name\":\"Coffee\",\"item_qty\":1,\"amount\":\"" + recpAmount1
				+ "\",\"item_type\":\"M\",\"item_id\":\"2001\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1},{\"item_name\":\"Pizza\",\"item_qty\":1,\"amount\":\""
				+ recpAmount2
				+ "\",\"item_type\":\"M\",\"item_id\":\"1001\",\"item_family\":\"10\",\"item_group\":\"888\",\"serial_number\":2}],\"receipt_datetime\":\"2022-11-24T14:14:07+05:30\",\"subtotal_amount\":"
				+ Amount + ",\"receipt_amount\":" + Amount + ",\"punchh_key\":\"" + punch_Key + "\",\"transaction_no\":"
				+ transaction_no + ",\"user_id\":\"" + userID + "\"}";

		System.out.println("payLoad for batch redemption :: - " + payLoad);

		return payLoad;

	}

	public String getBatchRedemptionPayloadOMMM107(String locationKey, String userID, String subAmount,
			String recpAmount1, String recpAmount2, String recpAmount3, String item_id1, String item_id2,
			String item_id3) {

		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String payLoad = "{\"location_key\":\"" + locationKey
				+ "\",\"line_items\":[{\"item_name\":\"Pizza1\",\"item_qty\":1,\"amount\":\"" + recpAmount1
				+ "\",\"item_type\":\"M\",\"item_id\":\" " + item_id1
				+ "\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1},{\"item_name\":\"Pizza2\",\"item_qty\":1,\"amount\":\""
				+ recpAmount2 + "\",\"item_type\":\"M\",\"item_id\":\" " + item_id2
				+ "\",\"item_family\":\"10\",\"item_group\":\"888\",\"serial_number\":2},{\"item_name\":\"Pizza3\",\"item_qty\":1,\"amount\":\""
				+ recpAmount3 + "\",\"item_type\":\"M\",\"item_id\":\"" + item_id3
				+ "\",\"item_family\":\"10\",\"item_group\":\"889\",\"serial_number\":3}],\"receipt_datetime\":\"2022-11-24T14:14:07+05:30\",\"subtotal_amount\":"
				+ subAmount + ",\"receipt_amount\":" + subAmount + ",\"punchh_key\":\"" + punch_Key
				+ "\",\"transaction_no\":" + transaction_no + ",\"user_id\":\"" + userID + "\"}";

		System.out.println("payLoad for batch redemption :: - " + payLoad);

		return payLoad;
	}

	public String getBatchRedemptionPayloadOMMM110(String locationKey, String userID, String subAmount,
			String recpAmount1, String recpAmount2, String recpAmount3) {

		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String payLoad = "{\"location_key\":\"" + locationKey
				+ "\",\"line_items\":[{\"item_name\":\"Pizza1\",\"item_qty\":1,\"amount\":\"" + recpAmount1
				+ "\",\"item_type\":\"M\",\"item_id\":\"1001\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1},{\"item_name\":\"Coffee1\",\"item_qty\":1,\"amount\":\""
				+ recpAmount2
				+ "\",\"item_type\":\"M\",\"item_id\":\"2001\",\"item_family\":\"10\",\"item_group\":\"888\",\"serial_number\":2},{\"item_name\":\"Coffee2\",\"item_qty\":1,\"amount\":\""
				+ recpAmount3
				+ "\",\"item_type\":\"M\",\"item_id\":\"2001\",\"item_family\":\"10\",\"item_group\":\"889\",\"serial_number\":3}],\"receipt_datetime\":\"2022-11-24T14:14:07+05:30\",\"subtotal_amount\":"
				+ subAmount + ",\"receipt_amount\":" + subAmount + ",\"punchh_key\":\"" + punch_Key
				+ "\",\"transaction_no\":" + transaction_no + ",\"user_id\":\"" + userID + "\"}";

		System.out.println("payLoad for batch redemption :: - " + payLoad);

		return payLoad;
	}

	public String getBatchRedemptionPayloadOMMMFor145_1(String locationKey, String userID, String Amount,
			String recpAmount1, String recpAmount2) {

		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String payLoad = "{\"location_key\":\"" + locationKey
				+ "\",\"line_items\":[{\"item_name\":\"Coffee\",\"item_qty\":4,\"amount\":\"" + recpAmount1
				+ "\",\"item_type\":\"M\",\"item_id\":\"2001\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1},{\"item_name\":\"Pizza\",\"item_qty\":1,\"amount\":\""
				+ recpAmount2
				+ "\",\"item_type\":\"M\",\"item_id\":\"1001\",\"item_family\":\"10\",\"item_group\":\"888\",\"serial_number\":2}],\"receipt_datetime\":\"2022-11-24T14:14:07+05:30\",\"subtotal_amount\":"
				+ Amount + ",\"receipt_amount\":" + Amount + ",\"punchh_key\":\"" + punch_Key + "\",\"transaction_no\":"
				+ transaction_no + ",\"user_id\":\"" + userID + "\"}";

		System.out.println("payLoad for batch redemption :: - " + payLoad);

		return payLoad;

	}

	public String getBatchRedemptionPayloadOMMM144_5(String locationKey, String userID, String subAmount,
			String recpAmount1, String recpAmount2, String recpAmount3, String recpAmount4, String recpAmount5) {

		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String payLoad = "{\"location_key\":\"" + locationKey
				+ "\",\"line_items\":[{\"item_name\":\"Pizza1\",\"item_qty\":1,\"amount\":\"" + recpAmount1
				+ "\",\"item_type\":\"M\",\"item_id\":\"1001\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1},{\"item_name\":\"Pizza2\",\"item_qty\":1,\"amount\":\""
				+ recpAmount2
				+ "\",\"item_type\":\"M\",\"item_id\":\"2001\",\"item_family\":\"10\",\"item_group\":\"888\",\"serial_number\":2},{\"item_name\":\"Pizza3\",\"item_qty\":1,\"amount\":\""
				+ recpAmount3
				+ "\",\"item_type\":\"M\",\"item_id\":\"101\",\"item_family\":\"10\",\"item_group\":\"889\",\"serial_number\":3},{\"item_name\":\"Pizza4\",\"item_qty\":1,\"amount\":\""
				+ recpAmount4
				+ "\",\"item_type\":\"M\",\"item_id\":\"2001\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1},{\"item_name\":\"Pizza5\",\"item_qty\":1,\"amount\":\""
				+ recpAmount5
				+ "\",\"item_type\":\"M\",\"item_id\":\"2001\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1}],\"receipt_datetime\":\"2022-11-24T14:14:07+05:30\",\"subtotal_amount\":"
				+ subAmount + ",\"receipt_amount\":" + subAmount + ",\"punchh_key\":\"" + punch_Key
				+ "\",\"transaction_no\":" + transaction_no + ",\"user_id\":\"" + userID + "\"}";

		System.out.println("payLoad for batch redemption :: - " + payLoad);

		return payLoad;
	}

	public String getBatchRedemptionPayloadOMMMFor108(String locationKey, String userID, String Amount,
			String recpAmount1, String recpAmount2) {

		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String payLoad = "{\"location_key\":\"" + locationKey
				+ "\",\"line_items\":[{\"item_name\":\"Coffee\",\"item_qty\":2,\"amount\":\"" + recpAmount1
				+ "\",\"item_type\":\"M\",\"item_id\":\"2001\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1},{\"item_name\":\"Pizza\",\"item_qty\":1,\"amount\":\""
				+ recpAmount2
				+ "\",\"item_type\":\"M\",\"item_id\":\"1001\",\"item_family\":\"10\",\"item_group\":\"888\",\"serial_number\":2}],\"receipt_datetime\":\"2022-11-24T14:14:07+05:30\",\"subtotal_amount\":"
				+ Amount + ",\"receipt_amount\":" + Amount + ",\"punchh_key\":\"" + punch_Key + "\",\"transaction_no\":"
				+ transaction_no + ",\"user_id\":\"" + userID + "\"}";

		System.out.println("payLoad for batch redemption :: - " + payLoad);

		return payLoad;

	}

	public String getBatchRedemptionPayloadOMMM144(String locationKey, String userID, String Amount, String recpAmount1,
			String recpAmount2, String qty, String qty1) {

		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String payLoad = "{\"location_key\":\"" + locationKey
				+ "\",\"line_items\":[{\"item_name\":\"Pizza1\",\"item_qty\":" + qty + ",\"amount\":\"" + recpAmount1
				+ "\",\"item_type\":\"M\",\"item_id\":\"1001\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1},{\"item_name\":\"Pizza2\",\"item_qty\":"
				+ qty1 + ",\"amount\":\"" + recpAmount2
				+ "\",\"item_type\":\"M\",\"item_id\":\"2001\",\"item_family\":\"10\",\"item_group\":\"888\",\"serial_number\":2}],\"receipt_datetime\":\"2022-11-24T14:14:07+05:30\",\"subtotal_amount\":"
				+ Amount + ",\"receipt_amount\":" + Amount + ",\"punchh_key\":\"" + punch_Key + "\",\"transaction_no\":"
				+ transaction_no + ",\"user_id\":\"" + userID + "\"}";

		System.out.println("payLoad for batch redemption :: - " + payLoad);

		return payLoad;

	}

	public String getBatchRedemptionPayloadOMMM108(String locationKey, String userID, String subAmount,
			String recpAmount1, String recpAmount2, String recpAmount3, String recpAmount4) {

		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String payLoad = "{\"location_key\":\"" + locationKey
				+ "\",\"line_items\":[{\"item_name\":\"coffee1\",\"item_qty\":1,\"amount\":\"" + recpAmount1
				+ "\",\"item_type\":\"M\",\"item_id\":\"2001\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1},{\"item_name\":\"Coffee2\",\"item_qty\":1,\"amount\":\""
				+ recpAmount2
				+ "\",\"item_type\":\"M\",\"item_id\":\"2001\",\"item_family\":\"10\",\"item_group\":\"888\",\"serial_number\":2},{\"item_name\":\"Coffee3\",\"item_qty\":1,\"amount\":\""
				+ recpAmount3
				+ "\",\"item_type\":\"M\",\"item_id\":\"2001\",\"item_family\":\"10\",\"item_group\":\"889\",\"serial_number\":3},{\"item_name\":\"Pizza1\",\"item_qty\":1,\"amount\":\""
				+ recpAmount4
				+ "\",\"item_type\":\"M\",\"item_id\":\"1001\",\"item_family\":\"10\",\"item_group\":\"889\",\"serial_number\":3}],\"receipt_datetime\":\"2022-11-24T14:14:07+05:30\",\"subtotal_amount\":"
				+ subAmount + ",\"receipt_amount\":" + subAmount + ",\"punchh_key\":\"" + punch_Key
				+ "\",\"transaction_no\":" + transaction_no + ",\"user_id\":\"" + userID + "\"}";

		System.out.println("payLoad for batch redemption :: - " + payLoad);

		return payLoad;
	}

	public String getBatchRedemptionPayloadOMMM109(String locationKey, String userID, String subAmount,
			String recpAmount1, String recpAmount2, String recpAmount3) {

		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String payLoad = "{\"location_key\":\"" + locationKey
				+ "\",\"line_items\":[{\"item_name\":\"Pizza1\",\"item_qty\":1,\"amount\":\"" + recpAmount1
				+ "\",\"item_type\":\"M\",\"item_id\":\" 1001\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1},{\"item_name\":\"Pizza2\",\"item_qty\":1,\"amount\":\""
				+ recpAmount2
				+ "\",\"item_type\":\"M\",\"item_id\":\"1001\",\"item_family\":\"10\",\"item_group\":\"888\",\"serial_number\":2},{\"item_name\":\"Pizza3\",\"item_qty\":1,\"amount\":\""
				+ recpAmount3
				+ "\",\"item_type\":\"M\",\"item_id\":\"1001\",\"item_family\":\"10\",\"item_group\":\"889\",\"serial_number\":3}],\"receipt_datetime\":\"2022-11-24T14:14:07+05:30\",\"subtotal_amount\":"
				+ subAmount + ",\"receipt_amount\":" + subAmount + ",\"punchh_key\":\"" + punch_Key
				+ "\",\"transaction_no\":" + transaction_no + ",\"user_id\":\"" + userID + "\"}";

		System.out.println("payLoad for batch redemption :: - " + payLoad);

		return payLoad;
	}

	public String getBatchRedemptionPayloadOMMM108_1(String locationKey, String userID, String subAmount,
			String recpAmount1, String recpAmount2, String recpAmount3) {

		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String payLoad = "{\"location_key\":\"" + locationKey
				+ "\",\"line_items\":[{\"item_name\":\"Pizza1\",\"item_qty\":1,\"amount\":\"" + recpAmount1
				+ "\",\"item_type\":\"M\",\"item_id\":\" 1001\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1},{\"item_name\":\"Pizza2\",\"item_qty\":1,\"amount\":\""
				+ recpAmount2
				+ "\",\"item_type\":\"M\",\"item_id\":\"1001\",\"item_family\":\"10\",\"item_group\":\"888\",\"serial_number\":2},{\"item_name\":\"Coffee1\",\"item_qty\":2,\"amount\":\""
				+ recpAmount3
				+ "\",\"item_type\":\"M\",\"item_id\":\"2001\",\"item_family\":\"10\",\"item_group\":\"889\",\"serial_number\":3}],\"receipt_datetime\":\"2022-11-24T14:14:07+05:30\",\"subtotal_amount\":"
				+ subAmount + ",\"receipt_amount\":" + subAmount + ",\"punchh_key\":\"" + punch_Key
				+ "\",\"transaction_no\":" + transaction_no + ",\"user_id\":\"" + userID + "\"}";

		System.out.println("payLoad for batch redemption :: - " + payLoad);

		return payLoad;
	}

	public String getBatchRedemptionPayloadOMMMForDollar10(String locationKey, String userID, String Amount) {

		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String payLoad = "{\"location_key\":\"" + locationKey
				+ "\",\"line_items\":[{\"item_name\":\"Pizza1\",\"item_qty\":1,\"amount\":\"10\",\"item_type\":\"M\",\"item_id\":\"101\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1},{\"item_name\":\"Pizza2\",\"item_qty\":1,\"amount\":\"2\",\"item_type\":\"M\",\"item_id\":\"101\",\"item_family\":\"10\",\"item_group\":\"888\",\"serial_number\":2},{\"item_name\":\"Coffee\",\"item_qty\":1,\"amount\":\"4\",\"item_type\":\"M\",\"item_id\":\"201\",\"item_family\":\"10\",\"item_group\":\"889\",\"serial_number\":3}],\"receipt_datetime\":\"2022-11-24T14:14:07+05:30\",\"subtotal_amount\":"
				+ Amount + ",\"receipt_amount\":" + Amount + ",\"punchh_key\":\"" + punch_Key + "\",\"transaction_no\":"
				+ transaction_no + ",\"user_id\":\"" + userID + "\"}";

		System.out.println("payLoad for batch redemption :: - " + payLoad);

		return payLoad;

	}

	public Map<String, Object> api1MobileChangePasswordPayLoad(String resetPasswordToken, String currentPassword,
			String newPassword) {
		Map<String, Object> payload = new HashMap<>();
		payload.put("reset_password_token", resetPasswordToken);
		payload.put("current_password", currentPassword);
		payload.put("password", newPassword);
		payload.put("password_confirmation", newPassword);
		return payload;
	}

	public Map<String, Object> identityGenerateBrandLevelTokenPayload(String client) {
		Map<String, Object> payload = new HashMap<>();
		payload.put("client", client);
		return payload;
	}

	public Map<String, Object> identityUserSignUpPayload(String client, String brandLevelToken,
			Boolean includePassword) {
		Map<String, Object> payload = new HashMap<>();
		String email = "identity_auto_" + CreateDateTime.getTimeDateAsneed("yyyyMMddHHmmss") + "@partech.com";
		Faker faker = new Faker();

		payload.put("access_token", brandLevelToken);
		payload.put("client", client);
		payload.put("email", email);
		if (includePassword)
			payload.put("password", "punchh@123<>&");
		payload.put("first_name", faker.name().firstName());
		payload.put("last_name", faker.name().lastName());
		payload.put("phone", "9" + faker.numerify("#########").toString());
		payload.put("birthday", "1991-09-18");
		payload.put("secondary_email", "sec_" + email);
		payload.put("marketing_email_subscription", true);
		payload.put("marketing_pn_subscription", true);
		payload.put("anniversary", "2020-09-18");
		payload.put("zip_code", faker.address().zipCode());
		payload.put("address", faker.address().streetAddress());
		payload.put("city", faker.address().city());
		payload.put("state", faker.address().state());
		payload.put("gender", "male");
		payload.put("send_compliance_sms", "true");
		payload.put("signup_channel", "Wifi");
		payload.put("title", "Mr.");
		payload.put("preferred_locale", "English");
		payload.put("age_verified", "true");
		payload.put("privacy_policy", "true");
		return payload;
	}

	public Map<String, Object> identityUserSignInPayload(String brandLevelToken, String brandUserToken,
			String verificationKey) {
		Map<String, Object> payload = new HashMap<>();
		payload.put("access_token", brandLevelToken);
		payload.put("subject_token", brandUserToken);
		putIfNotEmpty(payload, "verification_key", verificationKey);
		return payload;
	}

	public Map<String, Object> identityUserSignOutPayload(String brandLevelToken, String userAccessToken) {
		Map<String, Object> payload = new HashMap<>();
		payload.put("access_token", brandLevelToken);
		payload.put("subject_token", userAccessToken);
		return payload;
	}

	public Map<String, Object> identityGenerateBrandUserTokenPayload(String email, String brand) throws Exception {
		Map<String, Object> payload = new HashMap<>();
		Faker faker = new Faker();
		Utilities utils = new Utilities();
		switch (brand.toLowerCase()) {
		case "menu":
			payload.put("first_name", faker.name().firstName());
			payload.put("last_name", faker.name().lastName());
			payload.put("password", "password@123");
			payload.put("email", email);
			Map<String, Object> device_token = new HashMap<>();
			device_token.put("device_token", utils.decrypt(
					"sj/QpM9MAoww9u/0Ube2RUENNHDmmE8/qjGO/UnIGileMHkEIEBKfrr60HL6vH2q/H5meZkXwuMKxB0mqfozGT4Kw+9+zPsGrus77xJe0/E="));
			payload.put("device_info", device_token);
			break;
		}
		return payload;
	}

	public String fetchActiveDiscountWithExternalUidPosApiPayload(String userID, String externalUid) {
		String payload = "{\"user_id\":\"" + userID + ",\"external_uid\":\"" + externalUid + "\"}";
		return payload;
	}

	public String removedDiscountFromBasketPOSApiPayload(String userID, String basketID, String externalUid) {
		String payload = "{\"user_id\":\"" + userID + "\",\"discount_basket_item_ids\":\"" + basketID
				+ "\",\"external_uid\":\"" + externalUid + "\"}";
		return payload;
	}

	public String basketUnlockPOSApiPayload(String userID, String externalUid) {
		String payload = "{\n" + "    \"user_id\": \"" + userID + "\",\n" + "    \"external_uid\":\"" + externalUid
				+ "\"\n" + "}";
		return payload;
	}

	public String addDiscountItemPAuthApiayload(String discount_type, String reward, String externalUid) {

		String payload = "{\"discount_basket_items_attributes\":[{\"discount_type\":\"" + discount_type
				+ "\",\"discount_id\":\"" + reward + "\"}],\"external_uid\":\"" + externalUid + "\"}";
		return payload;
	}

	public String fetchActiveBasketAuthApiayload(String externalUid) {

		String payload = "{\"external_uid\":\"" + externalUid + "\"}";
		return payload;
	}

	public String userLookupPosAPIPayload(String lookUpField, String userEmail, String externalUid) {
		String payLoad = "{\"lookup_field\":\"" + lookUpField + "\",\"lookup_value\":\"" + userEmail
				+ "\",\"external_uid\":\"" + externalUid + "\"}";
		return payLoad;
	}

	public String discountLookUpPosApiPayload(String locationKey, String userID, String item_id, String externalUid,
			String amount) {
		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String payLoad = "{\"location_key\":\"" + locationKey
				+ "\",\"line_items\":[{\"item_name\":\"Pizza3\",\"item_qty\":1,\"amount\":\"" + amount + "\","
				+ "\"item_type\":\"M\",\"item_id\":\"" + item_id
				+ "\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":3}],"
				+ "\"receipt_datetime\":\"2023-06-26T14:14:07+05:30\",\"subtotal_amount\":" + amount
				+ ",\"receipt_amount\":" + amount + ",\"punchh_key\":\"" + punch_Key + "\"," + "\"transaction_no\":"
				+ transaction_no + ",\"external_uid\":\"" + externalUid + "\",\"user_id\":\"" + userID + "\"}";

		logger.info("payLoad for batch redemption :: - " + payLoad);
		return payLoad;

	}

	public String processBatchRedemptionPosApiPayload(String locationKey, String userID, String Amount, String qty,
			String id, String externalUid) {

		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String payLoad = "{\"location_key\":\"" + locationKey

				+ "\",\"line_items\":[{\"item_name\":\"Pizza1\",\"item_qty\":" + qty + ",\"amount\":\"" + Amount
				+ "\",\"item_type\":\"M\",\"item_id\":\"" + id
				+ "\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1}],\"receipt_datetime\":\"2022-11-24T14:14:07+05:30\",\"subtotal_amount\":"
				+ Amount + ",\"receipt_amount\":" + Amount + ",\"punchh_key\":\"" + punch_Key + "\",\"transaction_no\":"
				+ transaction_no + ",\"external_uid\":\"" + externalUid + "\",\"user_id\":\"" + userID + "\"}";

		System.out.println("payLoad for batch redemption :: - " + payLoad);

		return payLoad;

	}

	public String getBatchRedemptionAuthApiPayload(String locationKey, String userID, String item_id,
			String externalUid) {

		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String payLoad = "{\"location_key\":\"" + locationKey
				+ "\",\"line_items\":[{\"item_name\":\"Pizza1\",\"item_qty\":1,\"amount\":10,\"item_type\":\"M\",\"item_id\":\""
				+ item_id
				+ "\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1}],\"receipt_datetime\":\"2023-01-20T07:17:00Z\",\"subtotal_amount\":10,\"receipt_amount\":10,\"punchh_key\":\""
				+ punch_Key + "\",\"transaction_no\":" + transaction_no + ",\"external_uid\":\"" + externalUid
				+ "\",\"user_id\":\"" + userID + "\"}";

		logger.info("payLoad for batch redemption :: - " + payLoad);

		return payLoad;

	}

	public String autoSelectPOSAPiPayload(String userID, String Amount, String qty, String id, String externalUid) {

		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String payLoad = "{\"line_items\":[{\"item_name\":\"Pizza1\",\"item_qty\":" + qty + ",\"amount\":\"" + Amount
				+ "\",\"item_type\":\"M\",\"item_id\":\"" + id
				+ "\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1}],\"receipt_datetime\":\"2022-11-24T14:14:07+05:30\",\"subtotal_amount\":"
				+ Amount + ",\"receipt_amount\":" + Amount + ",\"punchh_key\":\"" + punch_Key + "\",\"transaction_no\":"
				+ transaction_no + ",\"external_uid\":\"" + externalUid + "\",\"user_id\":\"" + userID + "\"}";

		System.out.println("payLoad for batch redemption :: - " + payLoad);

		return payLoad;

	}

	public String ssoUserTokenMobilePayload(String rewardId, String paymentType, String paymentToken,
			String giftCardUuid, String redeemableId, String redemptionCode, String coupon) {

		String payload = "{\n" + "  \"payment_type\": \"" + paymentType + "\",\n" + "  \"payment_token\": \""
				+ paymentToken + "\",\n" + "  \"gift_card_uuid\": \"" + giftCardUuid + "\",\n" + "  \"tip\": \"10\",\n"
				+ "  \"reward_id\": \"" + rewardId + "\",\n" + "  \"redeemable_id\": \"" + redeemableId + "\",\n"
				+ "  \"banked_reward_amount\": \"5\",\n" + "  \"redeemable_card_count\": \"1\",\n"
				+ "  \"redemption_code\": \"" + redemptionCode + "\",\n" + "  \"coupon\": \"" + coupon + "\"\n" + "}";
		return payload;
	}

	public String createLocationMultipleLocationPayload(String name, String store_number,
			String enable_multiple_redemptions, String locationId) {

		String payload = "{\n" + "  \"store_number\": \"" + store_number + "\",\n" + "  \"location\": {\n"
				+ "    \"address\": \"201 San Antonio Circle, Suite 250\",\n" + "    \"city\": \"River View\",\n"
				+ "    \"country\": \"United \",\n" + "    \"latitude\": 37.406658,\n"
				+ "    \"longitude\": -122.109061,\n" + "    \"name\": \"" + name + "\",\n" + "    \"location_id\": \""
				+ locationId + "\",\n" + "    \"post_code\": \"943040\",\n" + "    \"state\": \"California\",\n"
				+ "    \"time_zone\": \"America/Los_Angeles\",\n" + "    \"location_extra_attributes\": {\n"
				+ "      \"enable_multiple_redemptions\": \"" + enable_multiple_redemptions + "\"\n" + "    }\n"
				+ "  }\n" + "}";

		return payload;

	}

	public String updateLocationMultipleLocationPayload(String location_id, String enable_multiple_redemptions) {

		String payload = "{\n" + "  \"location_id\": \"" + location_id + "\",\n" + "  \"location\": {\n"
				+ "    \"latitude\": 37.406658,\n" + "    \"longitude\": -122.109061,\n"
				+ "    \"location_extra_attributes\": {\n" + "      \"enable_multiple_redemptions\": \""
				+ enable_multiple_redemptions + "\"\n" + "    }\n" + "  }\n" + "}";

		return payload;
	}

	public String API1UpdateGuestDetails(String Fname, String Lname, String Npwd, String newEmail) {
		String payload = "{\n" + "    \"current_password\": \"password@1\",\n" + "    \"password\": \"" + Npwd + "\",\n"
				+ "    \"password_confirmation\": \"" + Npwd + "\",\n" + "    \"first_name\": \"" + Fname + "\",\n"
				+ "   \"last_name\": \"" + Lname + "\",\n" + " \"email\": \"" + newEmail + "\"\n" + "}";
		return payload;
	}

	public String API1TransferLoyaltyPointsToUser(String email, String points) {
		String payload = "{\n" + "    \"recipient_email\": \"" + email + "\",\n" + "    \"points_to_transfer\": \""
				+ points + "\"\n" + "}";
		return payload;
	}

	public Map<String, Object> identityUserSignInWithMobileAPI2AndIdentityClientSecretPayload(String email,
			String client) {
		Map<String, Object> payload = new HashMap<>();
		Map<String, Object> payloadUser = new HashMap<>();
		payloadUser.put("email", email);
		payload.put("user", payloadUser);
		payload.put("client", client);
		return payload;
	}

	public Map<String, Object> api2UpdateUserExternalSourceAndIDPayload(String user_id, String external_source,
			String external_source_id) {
		Map<String, Object> payload = new HashMap<>();
		Faker faker = new Faker();
		Map<String, Object> user = new HashMap<>();
		user.put("first_name", faker.name().firstName());
		user.put("last_name", faker.name().lastName());
		user.put("external_source", external_source);
		user.put("external_source_id", external_source_id);
		payload.put("id", user_id);
		payload.put("user", user);

		return payload;
	}

	public String updateDashboardBusinessConfigPayload(String res) {
		String payload = "{\n" + "    \"business\": " + res + " \n" + "}";
		return payload;
	}

	public String updateDashboardBusinessConfigPayloadSingleKey(String res) {
		String payload = "{\n" + " \"business\":{\n" + "" + res + "" + " }\n" + "}";
		return payload;
	}

	public String authRedemptionwithRewardPayload(String authentication_token, String reward_id, String subAmount,
			String client, Map<String, Map<String, String>> parentMap) {
		String recieptPayload = "";
		// String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000,
		// 500000000));
		// String transaction_no =
		// Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String head = "{\n" + "  \"store_number\": \"ND-002\",\n" + "  \"authentication_token\": \""
				+ authentication_token + "\",\n" + "  \"discount_type\": \"reward\",\n" + "  \"reward_id\": \""
				+ reward_id + "\",\n" + "  \"menu_items\": [";

		String base = "],\n" + "  \"subtotal_amount\": \"" + subAmount + "\",\n" + "  \"receipt_amount\": \""
				+ subAmount + "\",\n" + "  \"receipt_datetime\": \"2023-05-29T13:10:01Z\",\n"
				+ "  \"transaction_no\": 8909312992,\n" + "  \"external_uid\": \"23210012794\",\n" + "  \"client\": \""
				+ client + "\"\n" + "}";

		for (Entry<String, Map<String, String>> entry : parentMap.entrySet()) {
			Map<String, String> valuesMap = entry.getValue();
			String str = "{\"item_name\":\"" + valuesMap.get("item_name") + "\",\"item_qty\":"
					+ valuesMap.get("item_qty") + ",\"item_amount\":\"" + valuesMap.get("amount")
					+ "\",\"menu_item_type\":\"" + valuesMap.get("item_type") + "\",\"menu_item_id\":\""
					+ valuesMap.get("item_id") + "\",\"menu_family\":\"" + valuesMap.get("item_family")
					+ "\",\"menu_major_group\":\"" + valuesMap.get("item_group") + "\",\"serial_number\":"
					+ valuesMap.get("serial_number") + "},";
			recieptPayload = recieptPayload + str;
		}
		recieptPayload = recieptPayload.substring(0, recieptPayload.length() - 1);
		String finalRecieptPayLoad = head + recieptPayload + base;
		logger.info("Redemption of reward =" + finalRecieptPayLoad);
		return finalRecieptPayLoad;

	}

	public String posCheckinWith_N_QCPayload(String email, String subAmount, String client, String date,
			String external_uid, String receiptAmt, Map<String, Map<String, String>> parentMap) {
		String recieptPayload = "";
		String key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String txn_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));
		String seqNo = Integer.toString(Utilities.getRandomNoFromRange(50000000, 90000000));

		String head = "{\n" + "  \"receipt_amount\": \"" + receiptAmt + "\",\n" + "  \"receipt_datetime\": \"" + date
				+ "\",\n" + "  \"sequence_no\": \"" + seqNo + "\",\n" + "  \"punchh_key\": \"" + key + "\",\n"
				+ "  \"transaction_no\": \"" + txn_no + "\",\n" + "  \"external_uid\": \"" + external_uid + "\",\n"
				+ "  \"menu_items\": [";

		String base = "],\n" + "  \"subtotal_amount\": \"" + subAmount + "\",\n" + "  \"payable\": \"" + subAmount
				+ "\",\n" + "  \"email\": \"" + email + "\"\n" + "}";

		for (Entry<String, Map<String, String>> entry : parentMap.entrySet()) {
			Map<String, String> valuesMap = entry.getValue();
			String str = "{\"item_name\":\"" + valuesMap.get("item_name") + "\",\"item_qty\":"
					+ valuesMap.get("item_qty") + ",\"item_amount\":\"" + valuesMap.get("amount")
					+ "\",\"menu_item_type\":\"" + valuesMap.get("item_type") + "\",\"menu_item_id\":\""
					+ valuesMap.get("item_id") + "\",\"menu_family\":\"" + valuesMap.get("item_family")
					+ "\",\"menu_major_group\":\"" + valuesMap.get("item_group") + "\",\"serial_number\":"
					+ valuesMap.get("serial_number") + "},";
			recieptPayload = recieptPayload + str;
		}
		recieptPayload = recieptPayload.substring(0, recieptPayload.length() - 1);
		String finalRecieptPayLoad = head + recieptPayload + base;
		logger.info("Redemption of reward =" + finalRecieptPayLoad);
		return finalRecieptPayLoad;

	}

	public String v2CheckinPayload(String userEmail, String amount, String barcode, String manuItem1, String manuItem2,
			String receipt_datetime, String transaction_no, String channel) {
		String payload = "{\n" + "\"email\":\"" + userEmail + "\",\n" + "\"amount\":\"" + amount + "\",\n"
				+ "\"bar_code\":\"" + barcode + "\",\n" + "\"menu_items\":\"" + manuItem1 + "\",\n"
				+ "\"menu_items\":\"" + manuItem2 + "\",\n" + "\"receipt_datetime\":\"" + receipt_datetime + "\",\n"
				+ "\"transaction_no\":\"" + transaction_no + "\",\n" + "\"subtotal_amount\":\"" + amount + "\",\n"
				+ "\"channel\":\"" + channel + "\"\n" + "}";

		return payload;
	}

	public String API2SendMessageToUserChallengeCampaignPayload(String choice, String userID,
			String challenge_campaign_id, String progress_count) {
		String payload = "";
		switch (choice) {

		case "challengeCampaign":
			payload = "{\n" + "  \"user_id\": \"" + userID + "\",\n" + "  \"challenge_campaign_id\": \""
					+ challenge_campaign_id + "\",\n" + "  \"progress_count\":\"" + progress_count + "\"\n" + "}";
			break;
		case "withoutChallengeCampaign":
			payload = "{\n" + "  \"user_id\": \"" + userID + "\",\n" + "  \"progress_count\":\"" + progress_count
					+ "\"\n" + "}";
			break;
		}
		return payload;
	}

	public String API2GiftaCardPayloadWithOrderID(String email, String client, String design_id, String amount,
			String expDate, String firstName, String orderID) {

		String payload = "{\n" + "  \"recipient_email\": \"" + email + "\",\n" + "  \"client\": \"" + client + "\",\n"
				+ "  \"amount\": \"12\",\n" + "  \"design_id\": \"" + design_id + "\",\n" + "    \"amount\": \""
				+ amount + "\",\n" + "  \"order_id\": \"" + orderID + "\",\n" + "    \"cardholder_name\":\"" + firstName
				+ "\",\n" + "    \"exp_date\":\"" + expDate + "\",\n" + "  \"type\": \"VISA\"\n" + "}";

		return payload;
	}

	public String API2ReloadGiftCardPayloadWithOrderID(String client, String amount, String firstName, String expDate,
			String orderID) {

		String payload = "{\n" + "    \"client\": \"" + client + "\",\n" + "    \"amount\":\"" + amount + "\",\n"
				+ "    \"order_id\": \"" + orderID + "\",\n" + "    \"cardholder_name\": \"" + firstName + "\",\n"
				+ "    \"exp_date\": \"" + expDate + "\",\n" + "    \"type\": \"VISA\"\n" + "}";

		return payload;

	}

	public String API2PurchaseGiftCardPayloadWithOrderID(String client, String access_token, String design_id,
			String amount, String expDate, String firstName, String orderID) {

		String payload = "{\n" + "    \"client\":\"" + client + "\",\n" + "    \"access_token\":\"" + access_token
				+ "\",\n" + "    \"design_id\":\"" + design_id + "\",\n" + "    \"amount\": \"" + amount + "\",\n"
				+ "    \"order_id\":\"" + orderID + "\",\n" + "    \"cardholder_name\":\"" + firstName + "\",\n"
				+ "    \"exp_date\":\"" + expDate + "\",\n" + "    \"type\":\"VISA\"\n" + "}";
		return payload;

	}

	public String addDiscountAmountPAuthApiayload(String discount_type, String reward, String externalUid) {
		String payload = "{\"discount_basket_items_attributes\":[{\"discount_type\":\"" + discount_type
				+ "\",\"discount_value\":\"" + reward + "\"}],\"external_uid\":\"" + externalUid + "\"}";
		return payload;
	}

	public String removeDiscountBasketExtUIDSecureAPIPayload(String basketID, String external_uid) {
		String payload = "{\n" + "    \"discount_basket_item_ids\":\"" + basketID + "\",\n" + "    \"external_uid\":\""
				+ external_uid + "\"\n" + "}";
		return payload;
	}

	public String fetchActiveBasketPOSAPIPayload(String userId, String external_uid) {
		String payload = "{\n" + "  \"user_id\": " + userId + ",\n" + "  \"external_uid\": \"" + external_uid + "\"\n"
				+ "}";
		return payload;
	}

	public String redemptionOfCardCompletionApi2Payload(String client, String location_id) {

		String payload = "{\n" + "  \"client\": \"" + client + "\",\n" + "  \"location_id\": \"" + location_id + "\"\n"
				+ "}";

		return payload;

	}

	public String closeOrderOnlinePayload(String externalUID, String emailID, String storeNumber, String posRef,
			String oloSlug, String csID, String orderID) {
		Calendar cal = Calendar.getInstance();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		cal.add(Calendar.DATE, 0);
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		String extRef = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		String payload = "{\"id\":\"" + externalUID + "\",\"orderId\":\"OID" + orderID
				+ "\",\"externalReference\":\"EXTREF" + extRef + "\",\"posRef\":\"" + posRef + "\",\"storeNumber\":"
				+ storeNumber + ",\"storeUtcOffset\":5.5,\"timePlaced\":\"" + recieptDateTime
				+ "\",\"timeWanted\":null,\"timeReady\":\"" + recieptDateTime + "\",\"customer\":{\"customerId\":\""
				+ csID
				+ "\",\"externalReference\":\"\",\"firstName\":\"First_61281953\",\"lastName\":\"Last_61281953\",\"contactNumber\":\"61281953\",\"email\":\""
				+ emailID
				+ "\",\"membershipNumber\":null,\"loyaltyScheme\":null,\"loginProviders\":[{\"name\":\"Punchh\",\"slug\":\""
				+ oloSlug
				+ "\"}]},\"deliveryMethod\":\"\",\"deliveryAddress\":null,\"items\":[{\"productId\":120537,\"specialInstructions\":\"\",\"quantity\":1,\"recipientName\":\"\",\"customValues\":[],\"description\":\"Banana Pecan Pancake Breakfast\",\"sellingPrice\":-8.5900,\"modifiers\":[{\"modifierId\":294881,\"vendorSpecificModifierId\":171675236,\"modifierQuantity\":1,\"customFields\":[],\"description\":\"Egg Whites\",\"sellingPrice\":0.0000,\"modifiers\":[]},{\"modifierId\":294882,\"vendorSpecificModifierId\":171675237,\"modifierQuantity\":1,\"customFields\":[],\"description\":\"Turkey Bacon\",\"sellingPrice\":-2.0000,\"modifiers\":[]}]},{\"productId\":120524,\"specialInstructions\":\"\",\"quantity\":1,\"recipientName\":\"\",\"customValues\":[],\"description\":\"Caramel Apple Pie Crisp\",\"sellingPrice\":3.4900,\"modifiers\":[]}],\"customFields\":[],\"totals\":{\"subTotal\":3.00,\"salesTax\":1.0400,\"tip\":1.0000,\"delivery\":1.0000,\"feesTotal\":1.0000,\"discount\":1.0000,\"total\":1.0000,\"customerDelivery\":1.0000},\"payments\":[]}";
		return payload;

	}

	public String orderPlacedAndCancelledPayload(String emailID, String storeNumber, String orderID,
			String randomFirstName, String randomLastName) {

		String payload = "{\n" + "    \"id\": \"2b9888ca-24bc-e713-a977-0afcc1bdfe02\",\n" + "    \"orderId\": \""
				+ orderID + "\",\n" + "    \"timeReady\": \"20250901 16:15\",\n" + "    \"storeNumber\": \""
				+ storeNumber + "\",\n" + "    \"deliveryMethod\": \"pickup\",\n"
				+ "    \"externalReference\": \"string\",\n" + "    \"brandName\": \"Foosburgers\",\n"
				+ "    \"brandId\": \"cea27ddd-06cd-4a62-93af-858cb3e32dc9\",\n" + "    \"storeUtcOffset\": 5.5,\n"
				+ "    \"timePlaced\": \"20230529 19:38\",\n" + "    \"timeWanted\": \"20230529 19:38\",\n"
				+ "    \"customer\": {\n" + "        \"customerId\": 637722,\n" + "        \"email\": \"" + emailID
				+ "\",\n" + "        \"firstName\": \"" + randomFirstName + "\",\n" + "        \"lastName\": \""
				+ randomLastName + "\",\n" + "        \"contactNumber\": \"15055555555\"\n" + "    },\n"
				+ "    \"deliveryAddress\": {\n" + "        \"streetAddress1\": \"26 Broadway, Building 3\",\n"
				+ "        \"streetAddress2\": \"string\",\n" + "        \"city\": \"New York\",\n"
				+ "        \"postalCode\": \"10004\",\n" + "        \"comments\": \"Wait in lobby\",\n"
				+ "        \"coordinates\": {\n" + "            \"latitude\": 40.7055092,\n"
				+ "            \"longitude\": -74.0131178\n" + "        }\n" + "    },\n" + "    \"items\": [\n"
				+ "        {\n" + "            \"productId\": 122483,\n"
				+ "            \"specialInstructions\": \"string\",\n" + "            \"quantity\": 1,\n"
				+ "            \"recipientName\": \"string\",\n" + "            \"customValues\": [\n"
				+ "                {\n" + "                    \"label\": \"Writing on the cake\",\n"
				+ "                    \"value\": \"Happy Birthday!\",\n"
				+ "                    \"vendorOptionGroupChoiceId\": 894675\n" + "                }\n"
				+ "            ],\n" + "            \"description\": \"string\",\n"
				+ "            \"sellingPrice\": 1.99,\n" + "            \"modifiers\": [\n" + "                {\n"
				+ "                    \"modifierId\": 382903,\n"
				+ "                    \"vendorSpecificModifierId\": 214931301,\n"
				+ "                    \"modifierQuantity\": 1,\n"
				+ "                    \"description\": \"string\",\n" + "                    \"sellingPrice\": 0.99,\n"
				+ "                    \"modifiers\": [\n" + "                        {}\n" + "                    ],\n"
				+ "                    \"customFields\": [\n" + "                        {\n"
				+ "                            \"key\": \"Name\",\n"
				+ "                            \"value\": \"Value\"\n" + "                        }\n"
				+ "                    ]\n" + "                }\n" + "            ]\n" + "        }\n" + "    ],\n"
				+ "    \"customFields\": [\n" + "        {\n" + "            \"key\": \"Name\",\n"
				+ "            \"value\": \"Value\"\n" + "        }\n" + "    ],\n" + "    \"customFees\": [\n"
				+ "        {\n" + "            \"amount\": 0,\n" + "            \"description\": \"Service Fee\",\n"
				+ "            \"internalName\": \"Systemwide Service Fee\"\n" + "        }\n" + "    ],\n"
				+ "    \"totals\": {\n" + "        \"subTotal\": 0,\n" + "        \"salesTax\": 0,\n"
				+ "        \"feesTotal\": 0,\n" + "        \"tip\": 0,\n" + "        \"discount\": 0,\n"
				+ "        \"total\": 0,\n" + "        \"customerDelivery\": 0,\n" + "        \"delivery\": 0\n"
				+ "    },\n" + "    \"payments\": [\n" + "        {\n" + "            \"type\": \"instore\",\n"
				+ "            \"description\": \"Cash\",\n" + "            \"amount\": 6.99\n" + "        }\n"
				+ "    ],\n" + "    \"clientPlatform\": \"Web\",\n" + "    \"location\": {\n"
				+ "        \"name\": \"Kitchen Sink Demo Vendor\",\n" + "        \"logo\": \"string\",\n"
				+ "        \"latitude\": 40.7270278,\n" + "        \"longitude\": -73.9918977\n" + "    },\n"
				+ "    \"coupon\": {\n" + "        \"couponCode\": \"25OFF\",\n"
				+ "        \"description\": \"Take 25% off entire order\"\n" + "    }\n" + "}";

		return payload;
	}

	public String api2PurchaseSubscriptionPayloadWithPaymentCardUuid(String plan_id, String purchase_price,
			String uuid) {

		String startTime = CreateDateTime.getYesterdaysDate() + " 01:00:00";
		String endTime = CreateDateTime.getFutureDate() + " 21:09:38";

		String payload = "{\"plan_id\":\"" + plan_id + "\",\"external_plan_identifier\":\"\",\"start_time\":\""
				+ startTime + "\",\"end_time\":\"" + endTime + "\",\"purchase_price\":\"" + purchase_price
				+ "\",\"auto_renewal\":\"true\",\"payment_card_uuid\":\"" + uuid + "\"}";

		return payload;

	}

	public String API2SendMessageToUserWithEndDatePayload(String userID, String amount, String reedemable_id,
			String fuelAmount, String gift_count, String end_date) {

		String payload = "{\n" + "   \"user_id\": \"" + userID + "\",\n"
				+ "   \"message\": \"Automation test message\",\n" + "   \"subject\": \"Message from automation\",\n"
				+ "   \"reward_amount\": \"" + amount + "\",\n" + "   \"gift_count\": \"" + gift_count + "\",\n"
				+ "   \"fuel_amount\": \"" + fuelAmount + "\",\n" + "   \"redeemable_id\": \"" + reedemable_id + "\",\n"
				+ "   \"gift_reason\": \"\"\n" + ",  \"end_date\": \"" + end_date + "\"}";
		return payload;
	}

	public String discountBasketItemsAttributesWithExt_Uid(String userID, String discount_type, String discount_id,
			String externalUid) {

		String payLoad = "{\n" + "    \"user_id\": \"" + userID + "\",\n" + "    \n"
				+ "    \"discount_basket_items_attributes\": [\n" + "       {\n" + "        \"discount_type\": \""
				+ discount_type + "\",\n" + "        \"discount_id\": \"" + discount_id + "\"\n" + "        }\n"
				+ "    ],\"external_uid\":\"" + externalUid + "\"}";

		return payLoad;
	}

	public String singleScanCodeWithPaymentTypeValueSecureApiPayload(String payment_type_name,
			String payment_type_value, String payment_type_id) {
		String payLoad = "{\n" + "  \"payment_type\": \"" + payment_type_name + "\",\n" + "  \"" + payment_type_value
				+ "\": \"" + payment_type_id + "\"\n" + "}";
		return payLoad;
	}

	public String singleScanCodeWithoutPaymentTypeValueSecureApiPayload(String payment_type_name) {
		String payLoad = "{\n" + "  \"payment_type\": \"" + payment_type_name + "\"\n" + "}";
		return payLoad;
	}

	public String onlineOrderWithOrderModeAndClientPlatformTestPayload(String authentication_token,
			String discount_type, String discount_typeName, String discount_typeValue, String order_mode,
			String client_platform, String amount, String client, String externalUid,
			Map<String, Map<String, String>> parentMap) {
		String recieptPayload = "";
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String head = "{\n" + "  \"authentication_token\": \"" + authentication_token + "\",\n"
				+ "  \"discount_type\": \"" + discount_type + "\",\n" + "  \"" + discount_typeName + "\": \""
				+ discount_typeValue + "\",\n" + "  \"order_mode\" : \"" + order_mode + "\",\n"
				+ "  \"client_platform\" : \"" + client_platform + "\",\n" + "  \"menu_items\": [";

		String base = "],\n" + "  \"subtotal_amount\": " + amount + ",\n" + "  \"receipt_amount\": " + amount + ",\n"
				+ "  \"receipt_datetime\": \"2023-07-10T18:05:01+05:30\",\n" + "  \"transaction_no\": " + transaction_no
				+ ",\n" + "	 \"external_uid\": \"" + externalUid + "\",\n" + "  \"client\": \"" + client + "\"\n" + "}";

		for (Entry<String, Map<String, String>> entry : parentMap.entrySet()) {
			Map<String, String> valuesMap = entry.getValue();
			String str = "{\"item_name\":\"" + valuesMap.get("item_name") + "\",\"item_qty\":"
					+ valuesMap.get("item_qty") + ",\"item_amount\":\"" + valuesMap.get("amount")
					+ "\",\"menu_item_type\":\"" + valuesMap.get("item_type") + "\",\"menu_item_id\":\""
					+ valuesMap.get("item_id") + "\",\"menu_family\":\"" + valuesMap.get("item_family")
					+ "\",\"menu_major_group\":\"" + valuesMap.get("item_group") + "\",\"serial_number\":"
					+ valuesMap.get("serial_number") + "},";
			recieptPayload = recieptPayload + str;
		}
		recieptPayload = recieptPayload.substring(0, recieptPayload.length() - 1);
		String finalRecieptPayLoad = head + recieptPayload + base;
		return finalRecieptPayLoad;

	}

	public String onlineOrderWithOrderModeAndClientPlatformTestCardompletionPayload(String authentication_token,
			String discount_type, String order_mode, String client_platform, String amount, String client,
			String externalUid, Map<String, Map<String, String>> parentMap) {
		String recieptPayload = "";
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String head = "{\n" + "  \"authentication_token\": \"" + authentication_token + "\",\n"
				+ "  \"discount_type\": \"" + discount_type + "\",\n" + "  \"order_mode\" : \"" + order_mode + "\",\n"
				+ "  \"client_platform\" : \"" + client_platform + "\",\n" + "  \"menu_items\": [";

		String base = "],\n" + "  \"subtotal_amount\": " + amount + ",\n" + "  \"receipt_amount\": " + amount + ",\n"
				+ "  \"receipt_datetime\": \"2023-07-10T18:05:01+05:30\",\n" + "  \"transaction_no\": " + transaction_no
				+ ",\n" + "	 \"external_uid\": \"" + externalUid + "\",\n" + "  \"client\": \"" + client + "\"\n" + "}";

		for (Entry<String, Map<String, String>> entry : parentMap.entrySet()) {
			Map<String, String> valuesMap = entry.getValue();
			String str = "{\"item_name\":\"" + valuesMap.get("item_name") + "\",\"item_qty\":"
					+ valuesMap.get("item_qty") + ",\"item_amount\":\"" + valuesMap.get("amount")
					+ "\",\"menu_item_type\":\"" + valuesMap.get("item_type") + "\",\"menu_item_id\":\""
					+ valuesMap.get("item_id") + "\",\"menu_family\":\"" + valuesMap.get("item_family")
					+ "\",\"menu_major_group\":\"" + valuesMap.get("item_group") + "\",\"serial_number\":"
					+ valuesMap.get("serial_number") + "},";
			recieptPayload = recieptPayload + str;
		}
		recieptPayload = recieptPayload.substring(0, recieptPayload.length() - 1);
		String finalRecieptPayLoad = head + recieptPayload + base;
		return finalRecieptPayLoad;

	}

	public static String onlineCheckinPayLoad(String authenticationToken, String amount, String client,
			String transactionNumber, String externalUid, String storenumber) {
		Calendar cal = Calendar.getInstance();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		cal.add(Calendar.DATE, 0);
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		String payload = "{\n" + "	\"authentication_token\": \"" + authenticationToken + "\",\n"
				+ "	\"receipt_amount\": " + amount + ",\n" + "	\"cc_last4\": 4389,\n" + "	\"menu_items\": [{\n"
				+ "			\"item_name\": \"White rice\",\n" + "			\"item_qty\": 1,\n"
				+ "			\"item_amount\": 100,\n" + "			\"menu_item_type\": \"M\",\n"
				+ "			\"menu_item_id\": 110,\n" + "			\"menu_family\": \"106\",\n"
				+ "			\"menu_major_group\": \"100\",\n" + "			\"serial_number\": \"1.0\"\n"
				+ "		},\n" + "		{\n" + "			\"item_name\": \"Brown rice\",\n"
				+ "			\"item_qty\": 1,\n" + "			\"item_amount\": 7.86,\n"
				+ "			\"menu_item_type\": \"M\",\n" + "			\"menu_item_id\": 111,\n"
				+ "			\"menu_family\": \"106\",\n" + "			\"menu_major_group\": \"100\",\n"
				+ "			\"serial_number\": \"2.0\"\n" + "		},\n" + "		{\n"
				+ "			\"item_name\": \"Free rice\",\n" + "			\"item_qty\": 1,\n"
				+ "			\"item_amount\": 100,\n" + "			\"menu_item_type\": \"D\",\n"
				+ "			\"menu_item_id\": 3419,\n" + "			\"menu_family\": \"800\",\n"
				+ "			\"menu_major_group\": \"152\",\n" + "			\"serial_number\": \"3.0\"\n" + "		}\n"
				+ "	],\n" + "	\"subtotal_amount\": " + amount + ",\n" + "	\"receipt_datetime\": \"" + recieptDateTime
				+ "\",\n" + "	\"transaction_no\": \"" + transactionNumber + "\",\n" + "	\"external_uid\": \""
				+ externalUid + "\",\n" + "	\"client\": \"" + client + "\",\n"
				+ "	\"channel\": \"online_order\",\n  \"store_number\": \"" + storenumber + "\""

				+ "}";
		return payload;
	}

	public static String subscriptionPlanRedemptionCode(String client, String subscription_id) {
		String payload = "{\n" + " \"client\":\"" + client + "\",\n" + "\"subscription_id\":\"" + subscription_id + "\""
				+ "}";
		return payload;
	}

	public String API1UpdateGuestEmailDetails(String newEmail) {
		String payload = "{\n" + " \"email\":\"" + newEmail + "\"" + "}";
		return payload;
	}

	public String v1RedemptionPayload(String emailID, String subscriptionID) {

		String payload = "{\"email\": \"" + emailID + "\",\"subscription_id\": \"" + subscriptionID + "\"}";
		return payload;

	}

	public String islReceiptDetialsPayload(String punchhKey) {

		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String payload = "{\"subtotal_amount\":\"22.61\",\"amount\":\"22.61\",\"receipt_amount\":\"22.61\",\"payable\":\"24.28\",\"receipt_datetime\":\"2023-10-10 T17:58:40\",\"transaction_no\":\""
				+ transaction_no
				+ "\",\"employee_id\":\"10\",\"employee_name\":\"PradeepKumar9\",\"revenue_id\":\"3\",\"revenue_code\":\"Gorai123458\",\"first_name\":\"Pradeep3\",\"last_name\":\"Gorai\",\"cc_last4\":1234,\"menu_items\":[{\"item_amount\":\"8.95\",\"item_name\":\"Crispy Taco2\",\"item_qty\":\"5\",\"menu_family\":\"TACO\",\"menu_item_id\":\"100\",\"menu_item_type\":\"M\",\"menu_major_group\":\"TACO\"},{\"item_amount\":\"5.58\",\"item_name\":\"SoftShell Chk\",\"item_qty\":\"2\",\"menu_family\":\"TACO\",\"menu_item_id\":\"210\",\"menu_item_type\":\"M\",\"menu_major_group\":\"TACO\"},{\"item_amount\":\"5.09\",\"item_name\":\"Quesadilla Chk\",\"item_qty\":\"1\",\"menu_family\":\"QUESADILLA\",\"menu_item_id\":\"2510\",\"menu_item_type\":\"M\",\"menu_major_group\":\"QUESADILLA\"},{\"item_amount\":\"2.99\",\"item_name\":\"Lg Diet Pepsi\",\"item_qty\":\"1\",\"menu_family\":\"BEVERAGE\",\"menu_item_id\":\"7012\",\"menu_item_type\":\"M\",\"menu_major_group\":\"BEVERAGE\"}],\"pos_type\":\"Aloha(secure)\",\"pos_version\":\"11\",\"punchh_key\":\""
				+ punchhKey + "\"}";

		return payload;

	}

	public String authForgotPassword(String email, String client) {

		String payload = "{\n" + "  \"client\": \"" + client + "\",\n" + "  \"user\":{\n" + "  \"email\": \"" + email
				+ "\"\n" + "  }\n" + "}";

		return payload;
	}

	public String api2SingleScanCodePayload(String payment_type, String transaction_token, String client) {
		String payload = "{\"client\": \"" + client + "\", \"payment_type\": \"" + payment_type
				+ "\", \"transaction_token\": \"" + transaction_token
				+ "\", \"redeemable_id\": \"\", \"reward_id\": \"\", \"coupon\": \"\", \"tip\": \"1\"\n}";
		return payload;
	}

	public String api2GetAccessCodePayload(String payment_type, String gift_card_uuid, String client) {
		String payload = "{\"client\": \"" + client + "\", \"payment_type\": \"" + payment_type
				+ "\", \"gift_card_uuid\": \"" + gift_card_uuid + "\", \"tip\": \"1\"}";
		return payload;
	}

	public String api2CreatePaymentCardPayload(String heartlandToken, String adapterCode, String client) {
		String payload = "{\"client\": \"" + client + "\",\"transaction_token\": \"" + heartlandToken + "\","
				+ "\"adapter_code\": \"" + adapterCode
				+ "\",\"preferred\": true,\"nickname\": \"Test\",\"billing_info\": {\"name\": \"Punchh Tech\",\"country\": \"USA\",\"zip_code\": \"06106\"}}";
		return payload;
	}

	public String api2UpdatePaymentCardPayload(String nicknameToUpdate, String client) {
		String payload = "{\"client\": \"" + client + "\",\"nickname\": \"" + nicknameToUpdate
				+ "\",\"preferred\": false}";
		return payload;
	}

	public String api2FetchPaymentCardPayload(String adapterCode, String client) {
		String payload = "{\"client\": \"" + client + "\",\"adapter_code\": \"" + adapterCode
				+ "\",\"passcode\": \"1234\"}";
		return payload;
	}

	public String api2DeletePaymentCardPayload(String client) {
		String payload = "{\"client\": \"" + client + "\",\"passcode\": \"1234\"}";
		return payload;
	}

	public String api1UpdateUserPayload(String signupChannel) {
		String payload = "{\"signup_channel\": \"" + signupChannel + "\"}";
		return payload;
	}

	public Map<String, Object> api1UpdateUserEmailPhonePayload(String email, String phone, String firstName,
			String lastName) {
		Map<String, Object> payload = new HashMap<>();
		putIfNotEmpty(payload, "email", email);
		putIfNotEmpty(payload, "phone", phone);
		putIfNotEmpty(payload, "first_name", firstName);
		putIfNotEmpty(payload, "last_name", lastName);
		return payload;
	}

	public String api1UpdateUserDynamicFieldPayload(String fieldName, String fieldValue) {
		String payload = "{\"" + fieldName + "\": \"" + fieldValue + "\"}";
		return payload;
	}

	public Map<String, Object> api1UpdateUserPayload(String email, String phone, String firstName, String lastName) {
		Map<String, Object> payload = new HashMap<>();
		putIfNotEmpty(payload, "email", email);
		putIfNotEmpty(payload, "phone", phone);
		putIfNotEmpty(payload, "first_name", firstName);
		putIfNotEmpty(payload, "last_name", lastName);
		return payload;
	}

	public Map<String, Object> authApiUpdateUserPayload(String email, String phone, String firstName, String lastName) {
		Map<String, Object> payload = new HashMap<>();
		putIfNotEmpty(payload, "email", email);
		putIfNotEmpty(payload, "phone", phone);
		putIfNotEmpty(payload, "first_name", firstName);
		putIfNotEmpty(payload, "last_name", lastName);
		return payload;
	}

	public String api1CreateDonationPayload(String social_cause_id, String donation_type, String item_to_donate) {
		String payload = "{\"social_cause_id\": \"" + social_cause_id + "\",\"donation_type\": \"" + donation_type
				+ "\",\"item_to_donate\": " + item_to_donate + "}";
		return payload;
	}

	public String api1GiftaCardWithRandomAmountPayload(String userEmail, String designId, String transactionToken) {
		int lowerBound = 10;
		int upperBound = 20;
		int randomAmount = (int) (Math.random() * (upperBound - lowerBound + 1)) + lowerBound;
		String payload = "{\"email\":\"" + userEmail + "\",\"amount\":\"" + randomAmount + "\",\"design_id\":\""
				+ designId + "\",\"transaction_token\":\"" + transactionToken + "\"}";
		return payload;
	}

	public String api1GiftaCardPayload(String userEmail, String amount, String designId, String transactionToken) {
		String payload = "{\"email\":\"" + userEmail + "\",\"amount\":\"" + amount + "\",\"design_id\":\"" + designId
				+ "\",\"transaction_token\":\"" + transactionToken + "\"}";
		return payload;
	}

	public String api1CreateFeedbackPayload(String rating, String message) {
		String payload = "{\"message\":\"" + message
				+ "\",\"checkin_id\":\"90643848\",\"republishable\":false,\"rating\":" + rating
				+ ",\"fb_id\":\"1781956771860742\"}";
		return payload;
	}

	public String api1CurrencyTransferToOtherUser(String email, String amount) {
		String payload = "{\"recipient_email\": \"" + email + "\",\n" + "  \"amount_to_transfer\": \"" + amount + "\"}";
		return payload;
	}

	public String dashboardSubscriptionCancelPayload(String subscriptionID, String cancellationReason,
			String cancellationType) {
		String payload = "{\"subscription_id\": \"" + subscriptionID + "\",\"cancellation_reason\": \""
				+ cancellationReason + "\",\"cancellation_type\": \"" + cancellationType + "\"}";
		return payload;
	}

	public String api2BeaconPayload(String client, String accessToken, String beaconMajor, String beaconMinor) {
		String payload = "{\"client\": \"" + client + "\",\"access_token\": \"" + accessToken + "\",\"beacon_major\": "
				+ beaconMajor + ",\"beacon_minor\": " + beaconMinor + "}";
		return payload;
	}

	public String api1ImportGiftCardPayload(String designId, String cardNumber, String epin) {
		String payload = "{\"design_id\":\"" + designId + "\",\"card_number\":\"" + cardNumber + "\",\"epin\":\"" + epin
				+ "\"}";
		return payload;
	}

	public String api2ImportGiftCardPayload(String client, String designId, String cardNumber, String epin,
			String giftCardName) {
		String payload = "{\"client\": \"" + client + "\",\"design_id\": \"" + designId + "\",\"card_number\": \""
				+ cardNumber + "\",\"epin\": \"" + epin + "\",\"name\": \"" + giftCardName + "\"}";
		return payload;
	}

	public String api1PurchaseGiftCardPayload(String amount, String designId, String transactionToken, String expDate) {
		int lowerBound = 10;
		int upperBound = 20;
		int randomAmount = (int) (Math.random() * (upperBound - lowerBound + 1)) + lowerBound;
		String payload = "{\"design_id\": \"" + designId + "\",\"amount\": \"" + randomAmount
				+ "\",\"transaction_token\": \"" + transactionToken
				+ "\",\"cardholder_name\":\"Automation Test\",\"exp_date\": \"" + expDate + "\",\"type\": \"VISA\"}";
		return payload;
	}

	public String api1PurchaseGiftCardPayloadBySendingAmount(String amount, String designId, String transactionToken,
			String expDate) {
		String payload = "{\"design_id\": \"" + designId + "\",\"amount\": \"" + amount + "\",\"transaction_token\": \""
				+ transactionToken + "\",\"cardholder_name\":\"Automation Test\",\"exp_date\": \"" + expDate
				+ "\",\"type\": \"VISA\"}";
		return payload;
	}

	public String api1ReloadGiftCardPayload(String amount, String designId, String transactionToken) {
		int lowerBound = 10;
		int upperBound = 20;
		int randomAmount = (int) (Math.random() * (upperBound - lowerBound + 1)) + lowerBound;
		String payload = "{\"design_id\": \"" + designId + "\",\"amount\": \"" + randomAmount
				+ "\",\"transaction_token\": \"" + transactionToken + "\"}";
		return payload;
	}

	public String api1ReloadGiftCardPayloadBySendingAmount(String amount, String designId, String transactionToken) {
		String payload = "{\"design_id\": \"" + designId + "\",\"amount\": \"" + amount + "\",\"transaction_token\": \""
				+ transactionToken + "\"}";
		return payload;
	}

	public String getPSOPaymentPayload(String lookupType, String emailID, String singleScanCode, String paymentType,
			String locationKey) {
		String payload = "";
		switch (lookupType) {
		case "email":
			payload = "{\"email\":\"" + emailID + "\",\"location_key\":\"" + locationKey + "\",\"payment_type\":\""
					+ paymentType
					+ "\",\"payable\":6.32,\"pos_type\":\"aloha\",\"pos_version\":\"V03\",\"cc_last4\":\"0000\",\"employee_id\":\"102\",\"employee_name\":\"Punchh Tech\",\"revenue_id\":\"1\",\"revenue_code\":\"DINE IN\",\"amount\":6.22,\"subtotal_amount\":6.22,\"receipt_datetime\":\"2022-12-01T21:38:18Z\",\"transaction_no\":\"330059902-10009\",\"sequence_no\":\"33010007\",\"external_uid\":\"20201125163818-10009\",\"abbr_transaction_no\":\"1004\",\"punchh_key\":\"46523973360\",\"mac_address\":\"000C295D3860\",\"menu_items\":[{\"item_name\":\"IND GRILLED TENDER\",\"item_qty\":3,\"item_amount\":2.22,\"menu_item_type\":\"M\",\"menu_item_id\":5500,\"menu_family\":\"1.0\",\"menu_major_group\":\"1.0\",\"serial_number\":1.0},{\"item_name\":\"3 PC TENDER\",\"item_qty\":3,\"item_amount\":4.0,\"menu_item_type\":\"M\",\"menu_item_id\":10000,\"menu_family\":\"1.0\",\"menu_major_group\":\"1.0\",\"serial_number\":2.0},{\"item_name\":\"3 PC TENDER\",\"item_qty\":1,\"item_amount\":0,\"menu_item_type\":\"M\",\"menu_item_id\":20000,\"menu_family\":\"1.0\",\"menu_major_group\":\"1.0\",\"serial_number\":1.1},{\"item_name\":\"Punchh Discount\",\"item_qty\":1,\"item_amount\":0.35,\"menu_item_type\":\"T\",\"menu_item_id\":30000,\"menu_family\":\"1.0\",\"menu_major_group\":\"1.0\",\"serial_number\":3.0},{\"item_name\":\"Punchh Discount\",\"item_qty\":1,\"item_amount\":0.25,\"menu_item_type\":\"D\",\"menu_item_id\":30000,\"menu_family\":\"1.0\",\"menu_major_group\":\"1.0\",\"serial_number\":4.0}]}";
			break;

		case "singleScan":
			payload = "{\"single_scan_code\":\"" + singleScanCode + "\",\"location_key\":\"" + locationKey
					+ "\",\"payment_type\":\"" + paymentType
					+ "\",\"payable\":6.32,\"pos_type\":\"aloha\",\"pos_version\":\"V03\",\"cc_last4\":\"0000\",\"employee_id\":\"102\",\"employee_name\":\"Punchh Tech\",\"revenue_id\":\"1\",\"revenue_code\":\"DINE IN\",\"amount\":6.22,\"subtotal_amount\":6.22,\"receipt_datetime\":\"2022-12-01T21:38:18Z\",\"transaction_no\":\"330059902-10009\",\"sequence_no\":\"33010007\",\"external_uid\":\"20201125163818-10009\",\"abbr_transaction_no\":\"1004\",\"punchh_key\":\"46523973360\",\"mac_address\":\"000C295D3860\",\"menu_items\":[{\"item_name\":\"IND GRILLED TENDER\",\"item_qty\":3,\"item_amount\":2.22,\"menu_item_type\":\"M\",\"menu_item_id\":5500,\"menu_family\":\"1.0\",\"menu_major_group\":\"1.0\",\"serial_number\":1.0},{\"item_name\":\"3 PC TENDER\",\"item_qty\":3,\"item_amount\":4.0,\"menu_item_type\":\"M\",\"menu_item_id\":10000,\"menu_family\":\"1.0\",\"menu_major_group\":\"1.0\",\"serial_number\":2.0},{\"item_name\":\"3 PC TENDER\",\"item_qty\":1,\"item_amount\":0,\"menu_item_type\":\"M\",\"menu_item_id\":20000,\"menu_family\":\"1.0\",\"menu_major_group\":\"1.0\",\"serial_number\":1.1},{\"item_name\":\"Punchh Discount\",\"item_qty\":1,\"item_amount\":0.35,\"menu_item_type\":\"T\",\"menu_item_id\":30000,\"menu_family\":\"1.0\",\"menu_major_group\":\"1.0\",\"serial_number\":3.0},{\"item_name\":\"Punchh Discount\",\"item_qty\":1,\"item_amount\":0.25,\"menu_item_type\":\"D\",\"menu_item_id\":30000,\"menu_family\":\"1.0\",\"menu_major_group\":\"1.0\",\"serial_number\":4.0}]}";
			break;

		}
		return payload;

	}

	public String putPOSPaymentPayload(String payment_reference_id, String location_key, String status) {
		String payload = "{\"payment_reference_id\":\"" + payment_reference_id + "\",\"location_key\":\"" + location_key
				+ "\",\"status\":\"" + status + "\"}";

		return payload;
	}

	public String POSPaymentRefundPayload(String payment_reference_id, String location_key) {

		String payload = "{\"payment_reference_id\":\"" + payment_reference_id + "\",\"location_key\":\"" + location_key
				+ "\"}";
		return payload;
	}

	public String posPaymentCardPayload(String singleScanCode) {
		String payload = "{\"transaction_token\":\"" + singleScanCode
				+ "\",\"adapter_code\":\"heartland\",\"billing_info\":{\"name\":\"Ankur Nehra\",\"country\":\"USA\"}}";

		return payload;
	}
	// shashank
	// public String API1PurchaseGiftcardWithTransactionID(String amount, String
	// trToken) {
	//
	// String payload = "{\"design_id\":566,\"amount\":\"" + amount +
	// "\",\"transaction_token\":\"" + trToken + "\"}";
	// return payload;
	// }

	public String API1PurchaseGiftcardWithTransactionIDRecurring(String amount, String trToken) {

		String payload = "{\"design_id\":566,\"amount\":\"" + amount + "\",\"transaction_token\":\"" + trToken
				+ "\",\"payment_type\":\"recurring\"}";
		return payload;
	}

	public String API1PurchaseGiftcardWithSingleScanCode(String amount, String trToken) {

		String payload = "{\"design_id\":566,\"amount\":\"" + amount + "\",\"transaction_token\":\"" + trToken + "\"}";
		return payload;
	}

	public String userLookupPosAPIWithouttExt_uidPayload(String lookUpField, String userEmail) {
		String payLoad = "{\n" + "  \"lookup_field\": \"" + lookUpField + "\",\n" + "  \"lookup_value\": \"" + userEmail
				+ "\"\n" + "}";
		return payLoad;
	}

	public String ApiCancelSubcription(String subcriptionId, String cancelId, String cancelResaon) {
		String payLoad = "{\n" + "  \"subscription_id\": \"" + subcriptionId + "\",\n"
				+ "  \"cancellation_reason_id\": \"" + cancelId + "\",\n" + "  \"cancellation_reason\": \""
				+ cancelResaon + "\"" + "}";
		return payLoad;
	}

	public String rollingToInactivityExpiry() {
		String payLoad = "{\"inactive_days\": 180,\n" + "\"perform_force_redemption\":\"\",\n"
				+ "\"force_message\": \"This should be done\"}";
		return payLoad;
	}

	public String discountUnlockPOSApiPayload(String userID, String externalUid) {
		String payload = "{\n" + "  \"user_id\": \"" + userID + "\",\n" + "  \"external_uid\": \"" + externalUid
				+ "\"\n" + "}";
		return payload;
	}

	public String deleteDiscountBasketWithExt_UidPayload(String basketID, String external_uid) {
		String payload = "{\n" + "  \"discount_basket_item_ids\": \"" + basketID + "\",\n" + "  \"external_uid\": \""
				+ external_uid + "\"\n" + "}";
		return payload;
	}

	public String authAutoSelectPayload(String subAmount, String client, String external_uid,
			Map<String, Map<String, String>> parentMap) {
		String recieptPayload = "";
		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String head = "{\n" + "  \"line_items\": [";

		String base = "],\n" + "  \"receipt_datetime\": \"2023-05-29T13:10:01Z\",\n" + "  \"subtotal_amount\": "
				+ subAmount + ",\n" + "  \"receipt_amount\": " + subAmount + ",\n" + "  \"punchh_key\": \"" + punch_Key
				+ "\",\n" + "  \"transaction_no\": \"" + transaction_no + "\",\n" + "  \"external_uid\": \""
				+ external_uid + "\",\n" + "  \"client\": \"" + client + "\"\n" + "}";

		for (Entry<String, Map<String, String>> entry : parentMap.entrySet()) {
			Map<String, String> valuesMap = entry.getValue();
			String str = "{\"item_name\":\"" + valuesMap.get("item_name") + "\",\"item_qty\":"
					+ valuesMap.get("item_qty") + ",\"amount\":\"" + valuesMap.get("amount") + "\",\"item_type\":\""
					+ valuesMap.get("item_type") + "\",\"item_id\":\"" + valuesMap.get("item_id")
					+ "\",\"item_family\":\"" + valuesMap.get("item_family") + "\",\"item_group\":\""
					+ valuesMap.get("item_group") + "\",\"serial_number\":" + valuesMap.get("serial_number") + "},";
			recieptPayload = recieptPayload + str;
		}
		recieptPayload = recieptPayload.substring(0, recieptPayload.length() - 1);
		String finalRecieptPayLoad = head + recieptPayload + base;
		logger.info("Redemption of reward =" + finalRecieptPayLoad);
		return finalRecieptPayLoad;

	}

	public String posAutoSelectPayload(String amount, String external_uid, Map<String, Map<String, String>> parentMap) {
		String receiptPayload = "";
		String punchhKey = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transactionNo = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));
		String receiptDateTime = CreateDateTime.getCurrentDateTimeInUtc();

		String head = "{\n" + "  \"line_items\": [";
		String base = "],\n" + "  \"receipt_datetime\": \"" + receiptDateTime + "\",\n" + "  \"subtotal_amount\": "
				+ amount + ",\n" + "  \"receipt_amount\": " + amount + ",\n" + "  \"punchh_key\": \"" + punchhKey
				+ "\",\n" + "  \"transaction_no\": \"" + transactionNo + "\",\n" + "  \"external_uid\": \""
				+ external_uid + "\"}";

		for (Entry<String, Map<String, String>> entry : parentMap.entrySet()) {
			Map<String, String> valuesMap = entry.getValue();
			String str = "{\"item_name\":\"" + valuesMap.get("item_name") + "\",\"item_qty\":"
					+ valuesMap.get("item_qty") + ",\"amount\":" + valuesMap.get("amount") + ",\"item_type\":\""
					+ valuesMap.get("item_type") + "\",\"item_id\":\"" + valuesMap.get("item_id")
					+ "\",\"item_family\":\"" + valuesMap.get("item_family") + "\",\"item_group\":\""
					+ valuesMap.get("item_group") + "\",\"serial_number\":\"" + valuesMap.get("serial_number") + "\"},";
			receiptPayload = receiptPayload + str;
		}
		receiptPayload = receiptPayload.substring(0, receiptPayload.length() - 1);
		String finalRecieptPayLoad = head + receiptPayload + base;
		return finalRecieptPayLoad;
	}

	public String API1UpdateGuestDetailsWithoutEmailPayload(String Npwd, String phone) {
		String payload = "{\n" + "    \"current_password\": \"password@1\",\n" + "    \"password\": \"" + Npwd + "\",\n"
				+ "    \"password_confirmation\": \"" + Npwd + "\",\n" + "    \"phone\":\"" + phone + "\"\n" + "}";
		return payload;
	}

	public static String authOnlineCheckinWithQCPayLoad(String authenticationToken, String amount, String client,
			String txn, String externalUid, String recieptDateTime, Map<String, Map<String, String>> parentMap) {
		String recieptPayload = "";

		String head = "{\n" + "	\"authentication_token\": \"" + authenticationToken + "\",\n" + " \"external_uid\": \""
				+ externalUid + "\",\n" + "    \"payable\": \"" + amount + "\",\n" + "    \"subtotal_amount\": \""
				+ amount + "\",\n" + "    \"receipt_amount\": \"" + amount + "\",\n" + "    \"receipt_datetime\": \""
				+ recieptDateTime + "\",\n" + "    \"transaction_no\": \"" + txn + "\",\n"
				+ "    \"store_number\": \"345345436\",\n" + "    \"menu_items\":\n" + "    [";

		String base = " ],\n" + "    \"client\": \"" + client + "\"\n" + "}";

		for (Entry<String, Map<String, String>> entry : parentMap.entrySet()) {
			Map<String, String> valuesMap = entry.getValue();
			String str = "{\"item_name\":\"" + valuesMap.get("item_name") + "\",\"item_qty\":"
					+ valuesMap.get("item_qty") + ",\"item_amount\":\"" + valuesMap.get("amount")
					+ "\",\"menu_item_type\":\"" + valuesMap.get("item_type") + "\",\"menu_item_id\":\""
					+ valuesMap.get("item_id") + "\",\"menu_family\":\"" + valuesMap.get("item_family")
					+ "\",\"menu_major_group\":\"" + valuesMap.get("item_group") + "\",\"serial_number\":"
					+ valuesMap.get("serial_number") + "},";
			recieptPayload = recieptPayload + str;
		}

		recieptPayload = recieptPayload.substring(0, recieptPayload.length() - 1);
		String finalRecieptPayLoad = head + recieptPayload + base;
		logger.info("Redemption of reward =" + finalRecieptPayLoad);
		return finalRecieptPayLoad;
	}

	public String posVoidRedemptionPayloadWithTxn(String email, String discount_type_name, String discount_type_value,
			String tnx) {
		String payload = "{\n" + "  \"email\": \"" + email + "\",\n" + "  \"" + discount_type_name + "\": \""
				+ discount_type_value + "\",\n" + "  \"transaction_no\": \"" + tnx + "\"\n" + "}";

		return payload;
	}

	public String posVoidRedemptionPayloadWithoutTxn(String email, String discount_type_name,
			String discount_type_value) {
		String payload = "{\n" + "  \"email\": \"" + email + "\",\n" + "  \"" + discount_type_name + "\": \""
				+ discount_type_value + "\"\n" + "}";

		return payload;
	}

	public String elubGuestPayloadWithEmailPrivacyAndTerms(String email, String storeNumber) {
		String payload = "{\n" + "  \"store_number\":\"" + storeNumber + "\",\n" + "  \"user\": \n" + "  {\n"
				+ "    \"email\": \"" + email + "\", \n" + "    \"first_name\":\"test\",\n"
				+ "    \"last_name\":\"6\",\n" + "    \"gender\":\"Male\",\n" + "    \"signup_channel\":\"eClub\",\n"
				+ "    \"terms_and_conditions\":true,\n" + "    \"privacy_policy\":true\n" + "  }\n" + "}";
		return payload;
	}

	public String posFetchAccountBalancePayload(String userId, String discountTypeValue) {
		String payload = "{\n" + "  \"user_id\": \"" + userId + "\",\n" + "  \"discount_type\": \"" + discountTypeValue
				+ "\"\n" + "}";

		return payload;

	}

	public String api2PurchaseSubscriptionPayloadWithPaymentCardWithoutStartEndDateTime(String plan_id,
			String purchase_price, String uuid) {
		String payload = "{\"plan_id\":\"" + plan_id
				+ "\",\"external_plan_identifier\":\"\",\"auto_renewal\":\"true\",\"payment_card_uuid\":\"" + uuid
				+ "\"}";
		return payload;

	}

	public String api2PurchaseSubscriptionPayloadWithFutureDate(String plan_id, String purchase_price) {
		String startTime = CreateDateTime.getTomorrowDate() + " 01:00:00";
		String endTime = CreateDateTime.getTomorrowDate() + " 21:09:38";
		String payload = "{\n" + "   \"plan_id\": \"" + plan_id + "\",\n" + "   \"start_time\": \"" + startTime
				+ "\",\n" + "   \"end_time\": \"" + endTime + "\",\n" + "   \"purchase_price\": \"" + purchase_price
				+ "\",\n" + "   \"auto_renewal\": \"true\"\n" + "}";
		return payload;

	}

	public String api2PurchaseSubscriptionPayloadWithoutDate(String plan_id, String purchase_price) {

		// String startTime = CreateDateTime.getYesterdaysDate() + " 01:00:00";
		// String endTime = CreateDateTime.getFutureDate() + " 21:09:38";

		String payload = "{\n" + "   \"plan_id\": \"" + plan_id + "\",\n" + "   \"auto_renewal\": \"true\"\n" + "}";

		return payload;

	}

	public String getHeartlandPaymentTokenPayload() {

		String payLoad = "{\"card\":{\"number\":\"4111111111111111\",\"exp_month\":\"12\",\"exp_year\":\"2029\",\"cvc\":\"123\"},\"object\":\"token\",\"token_type\":\"supt\"}";

		return payLoad;
	}

	public String genInboundSegmentPayload(String hostURL, String apiKey, String audienceName, String audienceId,
			String action, String msgUUID) {

		String payLoad = "{\"type\":\"audience_subscription_request\",\"id\":\"" + msgUUID + "\",\"timestamp_ms\":"
				+ Long.parseLong(CreateDateTime.getTimeDateString()) + ","
				+ "\"account\":{\"account_id\":6071,\"account_settings\":{\"hostURL\":\"https://" + hostURL
				+ "\",\"apiKey\":\"" + apiKey + "\"}},\"audience_id\":" + audienceId + ",\"audience_name\":\""
				+ audienceName + "\"," + "\"audience_subscription_settings\":{},\"action\":\"" + action + "\"}";
		return payLoad;
	}

	public String genInboundSegmentUserPayload(String hostURL, String apiKey, String audienceName, String audienceId,
			String action, String msgUUID, String email, String mpid) {

		String payLoad = "{\"type\": \"audience_membership_change_request\", \"id\": \"" + msgUUID
				+ "\", \"timestamp_ms\": " + Long.parseLong(CreateDateTime.getTimeDateString()) + ", "
				+ "\"user_profiles\": [{\"user_identities\": [{\"type\": \"email\", \"encoding\": \"raw\", \"value\": \""
				+ email + "\"}], " + "\"device_identities\": [], \"audiences\": [{\"audience_id\": " + audienceId
				+ ", \"audience_name\": \"" + audienceName + "\", " + "\"action\": \"" + action
				+ "\", \"user_attributes\": []}], \"partner_identities\": [{\"type\": \"punchh_user_id\", "
				+ "\"encoding\": \"raw\", \"value\": \"partnerId\"}], \"mpid\": \"" + mpid
				+ "\"}], \"account\": {\"account_id\": 6071, " + "\"account_settings\": {\"hostURL\": \"https://"
				+ hostURL + "\", \"apiKey\": \"" + apiKey + "\"}}, " + "\"firehose_version\": \"2.6.0\"}";
		return payLoad;
	}

	public String deletePaymentCard(String clientId) {
		String payLoad = "{\n" + "  \"client\":\"" + clientId + "\",\n" + "  \"passcode\": \"1234\"\n" + "}";
		return payLoad;
	}

	public String userSubscription(String client) {
		String payLoad = "{\n" + "  \"client\":\"" + client + "\"\n" + "}";
		return payLoad;
	}

	public String authSubscriptionCancelPayload(String client, String subscriptionId, String cancellationFeedback,
			String cancellationReasonId, String cancellationType) {
		String payload = "{\n" + "  \"client\": \"" + client + "\",\n" + "  \"subscription_id\": " + subscriptionId
				+ ",\n" + "  \"cancellation_feedback\": \"" + cancellationFeedback + "\",\n"
				+ "  \"cancellation_reason_id\": \"" + cancellationReasonId + "\",\n" + "  \"cancellation_type\": \""
				+ cancellationType + "\"\n" + "}";
		return payload;
	}

	public String webHookAttentive(String phoneNumber, String fName) {

		String payload = "{\"phone\":\"" + phoneNumber + "\",\"sms_subscription\":\"true\",\"first_name\":\"" + fName
				+ "\",\"last_name\":\"CBZA\",\"user_type\":\"loyalty\",\"text_to_join\":\"true\"}";

		return payload;
	}

	public String genericAttentiveWebhookPayload(String phoneNumber, String email, String user_type,
			boolean smsSubscribe, boolean isTextToJoin, boolean isSmsAcquisition) {

		String emailPart = (email != null) ? "\"email\":\"" + email + "\"," : "";

		String payload = "{" + "\"phone\":\"" + phoneNumber + "\"," + "\"sms_subscription\":\"" + smsSubscribe + "\","
				+ emailPart + "\"first_name\":\"Attentive SMS\"," + "\"last_name\":\"Punchh User\","
				+ "\"user_type\":\"" + user_type + "\"," + "\"text_to_join\":\"" + isTextToJoin + "\","
				+ "\"sms_signup\":\"" + isSmsAcquisition + "\"" + "}";

		return payload;
	}

	public String genericSmsWebhookPayload(String phoneNumber, String email, String user_type, boolean smsSubscribe,
			boolean isTextToJoin, boolean isSmsAcquisition) {

		String emailPart = (email != null) ? "\"email\":\"" + email + "\"," : "";

		String payload = "{" + "\"phone\":\"" + phoneNumber + "\"," + "\"sms_subscription\":\"" + smsSubscribe + "\","
				+ emailPart + "\"first_name\":\"Attentive SMS\"," + "\"last_name\":\"Punchh User\","
				+ "\"user_type\":\"" + user_type + "\"," + "\"text_to_join\":\"" + isTextToJoin + "\","
				+ "\"sms_signup\":\"" + isSmsAcquisition + "\"" + "}";

		return payload;
	}

	public String iframePrefilledURLPayload(String phoneNumber, String fName) {

		String payload = "{\"first_name\":\"" + fName + "\",\"last_name\":\"CBZA\",\"phone\":\"" + phoneNumber + "\"}";

		return payload;
	}

	public String dashboardAPI2createFeedback(String userID, String rating, String message) {
		String payload = "{\"rating\": \"" + rating + "\",\"user_id\": \"" + userID + "\",\"message\": \"" + message
				+ "\"}";
		return payload;
	}

	public String dashboardAPI2updateFeedback(String userID, String rating, String message) {
		String payload = "{\"rating\": \"" + rating + "\",\"user_id\": \"" + userID + "\",\"message\": \"" + message
				+ "\"}";
		return payload;
	}

	public String api2DashboardCheckinPayload(String userSearchType, String userSearchValue, String startTime) {
		String randomValue = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String payload = "{\"" + userSearchType + "\":\"" + userSearchValue + "\",\"transaction_no\":\"" + randomValue
				+ "\",\"external_uid\":\"" + randomValue + "\",\"receipt_datetime\":\"" + startTime
				+ "T12:57:32.753Z\",\"subtotal_amount\":\"5.00\",\"receipt_amount\":\"5.00\",\"payable\":\"5.48\",\"employee_name\":\"John Doe\",\"employee_id\":\"7\",\"menu_items\":[{\"item_name\":\"2 for $5 Mix and Match Value Bundle\",\"item_qty\":1,\"item_amount\":5.0,\"menu_item_type\":\"M\",\"menu_item_id\":\"1395\",\"menu_family\":\"1\",\"menu_major_group\":\"1\",\"serial_number\":\"1.0\"},{\"item_name\":\"Spicy Chicken Sandwich\",\"item_qty\":1,\"item_amount\":0.0,\"menu_item_type\":\"M\",\"menu_item_id\":\"6029\",\"menu_family\":\"1\",\"menu_major_group\":\"1\",\"serial_number\":\"1.1\"},{\"item_name\":\"Spicy Chicken Sandwich\",\"item_qty\":1,\"item_amount\":0.0,\"menu_item_type\":\"M\",\"menu_item_id\":\"6029\",\"menu_family\":\"1\",\"menu_major_group\":\"1\",\"serial_number\":\"1.2\"},{\"item_name\":\"Steak and Egg Burrito\",\"item_qty\":1,\"item_amount\":6.99,\"menu_item_type\":\"M\",\"menu_item_id\":\"10000611\",\"menu_family\":\"1\",\"menu_major_group\":\"1\",\"serial_number\":\"2.0\"},{\"item_name\":\"Breakfast Burrito\",\"item_qty\":1,\"item_amount\":6.99,\"menu_item_type\":\"D\",\"menu_item_id\":\"27684\",\"menu_family\":\"discount\",\"menu_major_group\":\"discount\",\"serial_number\":\"\"}],\"cc_last4\":\"5678\",\"revenue_id\":\"website\",\"revenue_code\":\"takeaway\",\"store_number\":\"1234\"}";
		return payload;
	}

	public String posCheckinWithQCPayloadWithoutUUID(String email, String subAmount, String rewardID, String receiptAmt,
			Map<String, Map<String, String>> parentMap) {
		String recieptPayload = "";
		// String key = Integer.toString(Utilities.getRandomNoFromRange(100000000,
		// 500000000));
		// String txn_no = Integer.toString(Utilities.getRandomNoFromRange(500000000,
		// 900000000));
		String seqNo = Integer.toString(Utilities.getRandomNoFromRange(50000000, 90000000));

		String head = "{\"email\":\"" + email + "\",\"receipt_amount\":" + receiptAmt + ",\"subtotal_amount\":"
				+ subAmount + ",\"sequence_no\":\"" + seqNo + "\",\"discount_type\":\"reward\",\"reward_id\":\""
				+ rewardID + "\",\"menu_items\":[";

		String base = "]}";

		for (Entry<String, Map<String, String>> entry : parentMap.entrySet()) {
			Map<String, String> valuesMap = entry.getValue();
			String str = "{\"item_name\":\"" + valuesMap.get("item_name") + "\",\"item_qty\":"
					+ valuesMap.get("item_qty") + ",\"item_amount\":\"" + valuesMap.get("amount")
					+ "\",\"menu_item_type\":\"" + valuesMap.get("item_type") + "\",\"menu_item_id\":\""
					+ valuesMap.get("item_id") + "\",\"menu_family\":\"" + valuesMap.get("item_family")
					+ "\",\"menu_major_group\":\"" + valuesMap.get("item_group") + "\",\"serial_number\":"
					+ valuesMap.get("serial_number") + "},";
			recieptPayload = recieptPayload + str;
		}
		recieptPayload = recieptPayload.substring(0, recieptPayload.length() - 1);
		String finalRecieptPayLoad = head + recieptPayload + base;
		logger.info("Redemption of reward =" + finalRecieptPayLoad);
		return finalRecieptPayLoad;

	}

	public String apiPurchaseSubscriptionPayload(String plan_id, String purchase_price, String endDateTime,
			String autoRenewalFlag, String... taxValueLocationIdDiscountValue) {

		String taxRateField = (taxValueLocationIdDiscountValue.length > 0)
				? ",\n\"tax_value\": \"" + taxValueLocationIdDiscountValue[0] + "\"" + ",\n\"location_id\": \""
						+ taxValueLocationIdDiscountValue[1] + "\"" + ",\n\"discount_value\": \""
						+ taxValueLocationIdDiscountValue[2] + "\""
				: "";

		String startTime = CreateDateTime.getYesterdaysDate() + " 01:00:00";
		if (endDateTime == "") {
			endDateTime = CreateDateTime.getFutureDate() + " 21:09:38";
		}
		String payload = "{\n" + "   \"plan_id\": \"" + plan_id + "\",\n" + "   \"start_time\": \"" + startTime
				+ "\",\n" + "   \"end_time\": \"" + endDateTime + "\",\n" + "   \"purchase_price\": \"" + purchase_price
				+ "\",\n" + "   \"auto_renewal\":\"" + autoRenewalFlag + "\"\n" + taxRateField + "\n" + "}";

		return payload;
	}

	public String authApiSubscriptionPurchase(String plan_id, String purchase_price, String startTime,
			String endDateTime, String autoRenewalFlag, String client, String token) {
		if (endDateTime == "") {
			endDateTime = CreateDateTime.getFutureDate() + " 21:09:38";
		}
		String payload = "{\n" + "   \"plan_id\": \"" + plan_id + "\",\n" + "   \"start_time\": \"" + startTime
				+ "\",\n" + "   \"end_time\": \"" + endDateTime + "\",\n" + "   \"purchase_price\": " + purchase_price
				+ ",\n" + "   \"auto_renewal\":\"" + autoRenewalFlag + "\",\n" + "   \"client\": \"" + client + "\",\n"
				+ "   \"authentication_token\":\"" + token + "\"\n" + "}";

		return payload;
	}

	public String authPurchaseSubscriptionPayload(String plan_id, String purchase_price, String endDateTime,
			String autoRenewalFlag, String userID, String... taxValueLocationIdDiscountValue) {

		String taxRateField = (taxValueLocationIdDiscountValue.length > 0)
				? ",\n\"tax_value\": \"" + taxValueLocationIdDiscountValue[0] + "\"" + ",\n\"location_id\": \""
						+ taxValueLocationIdDiscountValue[1] + "\"" + ",\n\"discount_value\": \""
						+ taxValueLocationIdDiscountValue[2] + "\""
				: "";

		String startTime = CreateDateTime.getYesterdaysDate() + " 01:00:00";
		if (endDateTime == "") {
			endDateTime = CreateDateTime.getFutureDate() + " 21:09:38";
		}
		String payload = "{\n" + "   \"plan_id\": \"" + plan_id + "\",\n" + "   \"start_time\": \"" + startTime
				+ "\",\n" + "   \"end_time\": \"" + endDateTime + "\",\n" + "   \"purchase_price\": \"" + purchase_price
				+ "\",\n" + "   \"auto_renewal\":\"" + autoRenewalFlag + "\",\n" + "   \"user_id\": \"" + userID
				+ "\"\n" + taxRateField + "\n" + "}";
		return payload;
	}

	public String Api1GiftRewardToOtherUserPayload(String recipientEmail, String rewardID) {
		String payload = "{\n" + "  \"recipient_email\": \"" + recipientEmail + "\",\n" + "  \"reward_to_transfer\": \""
				+ rewardID + "\"\n" + "}";
		return payload;
	}

	public String APi1GamingAchievementsPayload(String kind, String level, String score, String gaming_level_id) {
		String payload = "{\n" + "  \"kind\": \"" + kind + "\",\n" + "  \"level\": \"" + level + "\",\n"
				+ "  \"score\": \"" + score + "\",\n" + "  \"gaming_level_id\": " + gaming_level_id + "\n" + "}";
		return payload;
	}

	public String getSFMCAccessToken(String et_clientId, String et_clientSecret) {
		String payload = "{\n" + "    \"grant_type\": \"client_credentials\",\n" + "    \"client_id\": \"" + et_clientId
				+ "\",\n" + "    \"client_secret\": \"" + et_clientSecret + "\"\n" + "}";
		return payload;
	}

	public String getSFMCFolderCategoryID(String et_subdomain, String access_token, String segmentName) {
		String payload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:u=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"><s:Header><a:Action s:mustUnderstand=\"1\">Retrieve</a:Action><a:To s:mustUnderstand=\"1\">https://"
				+ et_subdomain
				+ ".soap.marketingcloudapis.com/Service.asmx</a:To><fueloauth xmlns=\"http://exacttarget.com\">"
				+ access_token
				+ "</fueloauth></s:Header><s:Body xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"><RetrieveRequestMsg xmlns=\"http://exacttarget.com/wsdl/partnerAPI\"><RetrieveRequest><ObjectType>DataExtension</ObjectType><Properties>ObjectID</Properties><Properties>CustomerKey</Properties><Properties>Name</Properties><Properties>CategoryID</Properties><Properties>IsSendable</Properties><Properties>SendableSubscriberField.Name</Properties><Filter xsi:type=\"SimpleFilterPart\"><Property>CustomerKey</Property><SimpleOperator>equals</SimpleOperator><Value>"
				+ segmentName + "</Value></Filter></RetrieveRequest></RetrieveRequestMsg></s:Body></s:Envelope>";
		return payload;
	}

	public String deleteSFMCFolder(String et_subdomain, String access_token, String categoryID) {
		String payload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:u=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"><s:Header><a:Action s:mustUnderstand=\"1\">Delete</a:Action><a:To s:mustUnderstand=\"1\">https://"
				+ et_subdomain
				+ ".soap.marketingcloudapis.com/Service.asmx</a:To><fueloauth xmlns=\"http://exacttarget.com\">"
				+ access_token
				+ "</fueloauth></s:Header><s:Body xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"><DeleteRequest xmlns=\"http://exacttarget.com/wsdl/partnerAPI\"><Options/><ns1:Objects xmlns:ns1=\"http://exacttarget.com/wsdl/partnerAPI\" xsi:type=\"ns1:DataFolder\"><ns1:ModifiedDate xsi:nil=\"true\"/><ns1:ID>"
				+ categoryID
				+ "</ns1:ID><ns1:ObjectID xsi:nil=\"true\"/></ns1:Objects></DeleteRequest></s:Body></s:Envelope>";
		return payload;
	}

	public String webApiPayload(String userEmail, String rewardId) {
		String payload = "{\n" + "  \"email\": \"" + userEmail + "\",\n" + "  \"reward_id\": \"" + rewardId + "\"\n"
				+ "}";
		return payload;
	}

	public String createBusinessMigrationUser(String userEmail, String firstName, String lastName, String initialPoints,
			String originalPoints) {
		String name = firstName + lastName;
		String payload = "{\n" + "  \"name\": \"" + name + "\",\n" + "  \"email\": \"" + userEmail + "\",\n"
				+ "  \"first_name\": \"" + firstName + "\",\n" + "  \"last_name\": \"" + lastName + "\",\n"
				+ "  \"initial_points\": \"" + initialPoints + "\",\n" + "  \"original_points\": \"" + originalPoints
				+ "\"\n" + "}";

		return payload;

	}

	public String createBusinessMigrationUser(String userEmail, String originalPoints, String initialPoints) {
		String expiration_date = CreateDateTime.getFutureDateTimeInGivenFormate(365, "yyyy-MM-dd");
		String payload = "{\n" + "  \"name\": \"Incumbent Loyalty Program\",\n" + "  \"birthday\": \"1995-03-21\",\n"
				+ "  \"phone\": 9876543210,\n" + "  \"email\": \"" + userEmail + "\",\n"
				+ "  \"original_membership_no\": \"123456789\",\n"
				+ "  \"registration_date\": \"2008-10-26T23:59:59-07:00\",\n" + "  \"first_name\": \"John\",\n"
				+ "  \"last_name\": \"Doe\",\n" + "  \"original_phone\": \"9876543210\",\n" + "  \"original_points\": "
				+ originalPoints + ",\n" + "  \"initial_points\": " + initialPoints + ",\n"
				+ "  \"migrated_rewards\": 10.9,\n" + "  \"migrated_rewards_expiration_date\": \"" + expiration_date
				+ "\",\n" + "  \"marketing_pn_subscription\": \"true\",\n"
				+ "  \"marketing_email_subscription\": \"false\",\n"
				+ "  \"address_line1\": \"201 San Antonio Circle, Suite 250\",\n" + "  \"city\": \"Mountain View\",\n"
				+ "  \"state\": \"California\",\n" + "  \"zip_code\": 94040\n" + "}";

		return payload;

	}

	public String discountBasketItemsAttributesWithDiscountVal(String discount_type, String discount_value) {

		String payLoad = "{\"discount_type\": \"" + discount_type + "\",\"discount_value\": \"" + discount_value
				+ "\"},";

		return payLoad;
	}

	public String discountBasketItemsAttributesWithDiscountID(String discount_type, String discount_value) {

		String payLoad = "{\"discount_type\": \"" + discount_type + "\",\"discount_id\": \"" + discount_value + "\"},";

		return payLoad;
	}

	public String finalPayloadAfterAddingMultipleDiscounts(Map<String, Map<String, String>> parentMap) {

		String finalString = "";
		String strhead = "{\"discount_basket_items_attributes\": [";
		String strBase = "]}";
		String str = "";

		for (Entry<String, Map<String, String>> entry : parentMap.entrySet()) {
			Map<String, String> valuesMap = entry.getValue();
			if (valuesMap.get("discount_type").equalsIgnoreCase("fuel_reward")
					|| valuesMap.get("discount_type").equalsIgnoreCase("discount_amount")) {
				str = discountBasketItemsAttributesWithDiscountVal(valuesMap.get("discount_type"),
						valuesMap.get("discount_value"));
			} else {
				str = discountBasketItemsAttributesWithDiscountID(valuesMap.get("discount_type"),
						valuesMap.get("discount_id"));
			}

			finalString = finalString + str;
		}

		finalString = strhead + finalString.substring(0, finalString.length() - 1) + strBase;
		return finalString;

	}

	public String getAuthBatchRedemptionPayloadOMMM(String storeNum, String userID, String item_id) {

		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String payLoad = "{\"store_number\":\"" + storeNum
				+ "\",\"line_items\":[{\"item_name\":\"Pizza1\",\"item_qty\":1,\"amount\":10,\"item_type\":\"M\",\"item_id\":\""
				+ item_id
				+ "\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1}],\"receipt_datetime\":\"2023-01-20T07:17:00Z\",\"subtotal_amount\":10,\"receipt_amount\":10,\"punchh_key\":\""
				+ punch_Key + "\",\"transaction_no\":" + transaction_no + ",\"user_id\":\"" + userID + "\"}";

		logger.info("payLoad for batch redemption :: - " + payLoad);

		return payLoad;

	}

	public String apiPurchaseSubscriptionWithStartTimePayload(String plan_id, String purchase_price, String endDateTime,
			String autoRenewalFlag) {

		String startTime = CreateDateTime.getCurrentDate() + " 00:00:01";
		if (endDateTime == "") {
			endDateTime = CreateDateTime.getFutureDate() + " 21:09:38";
		}
		String payload = "{\n" + "   \"plan_id\": \"" + plan_id + "\",\n" + "   \"start_time\": \"" + startTime
				+ "\",\n" + "   \"end_time\": \"" + endDateTime + "\",\n" + "   \"purchase_price\": \"" + purchase_price
				+ "\",\n" + "   \"auto_renewal\":\"" + autoRenewalFlag + "\"\n" + "}";

		return payload;
	}

	public String generateReceiptWithQueryParam(String locationKey, String userID, String subAmount, String query,
			Map<String, Map<String, String>> parentMap) {
		String recieptPayload = "";
		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));
		String head = "{\"location_key\":\"" + locationKey + "\",\"line_items\":[";
		String base = "],\"receipt_datetime\":\"2022-11-24T14:14:07+05:30\",\"subtotal_amount\":" + subAmount
				+ ",\"receipt_amount\":" + subAmount + ",\"punchh_key\":\"" + punch_Key + "\",\"transaction_no\":"
				+ transaction_no + ",\"query\":" + query + ",\"user_id\":\"" + userID + "\"}";

		for (Entry<String, Map<String, String>> entry : parentMap.entrySet()) {
			Map<String, String> valuesMap = entry.getValue();
			String str = "{\"item_name\":\"" + valuesMap.get("item_name") + "\",\"item_qty\":"
					+ valuesMap.get("item_qty") + ",\"amount\":\"" + valuesMap.get("amount") + "\",\"item_type\":\""
					+ valuesMap.get("item_type") + "\",\"item_id\":\"" + valuesMap.get("item_id")
					+ "\",\"item_family\":\"" + valuesMap.get("item_family") + "\",\"item_group\":\""
					+ valuesMap.get("item_group") + "\",\"serial_number\":" + valuesMap.get("serial_number") + "},";
			recieptPayload = recieptPayload + str;
		}
		recieptPayload = recieptPayload.substring(0, recieptPayload.length() - 1);
		String finalRecieptPayLoad = head + recieptPayload + base;
		logger.info("Test3234.generateReceipt()finalRecieptPayLoad =" + finalRecieptPayLoad);
		return finalRecieptPayLoad;

	}

	public String fetchChallengeDetails(String client, String accessToken) {
		String payload = "{\n" + "\"client\":\"" + client + "\",\n" + "   \"access_token\": \"" + accessToken + "\"\n"
				+ "}";
		return payload;
	}

	public String createMigrationUserWithSingleCard(String userEmail, String card_number, String epin) {
		String payload = "{\"name\":\"Incumbent Loyalty Program\",\"email\":\"" + userEmail
				+ "\",\"first_name\":\"FirstName\",\"last_name\":\"LastName\",\"initial_points\":\"900\",\"original_points\":\"200\",\"gift_cards\":[{\"card_number\":\""
				+ card_number + "\",\"epin\":\"" + epin + "\"}]}";
		return payload;
	}

	public String createMigrationUserWithDoubleCard(String userEmail, String card_number1, String epin1,
			String card_number2, String epin2) {

		String payload = "{\"name\":\"Incumbent Loyalty Program\",\"email\":\"" + userEmail
				+ "\",\"first_name\":\"FName\",\"last_name\":\"LName\",\"initial_points\":\"900\",\"original_points\":\"200\",\"gift_cards\":[{\"card_number\":\""
				+ card_number1 + "\",\"epin\":\"" + epin1 + "\"},{\"card_number\":\"" + card_number2 + "\",\"epin\":\""
				+ epin2 + "\"}]}";
		return payload;
	}

	public String createMigrationUserWithGiftID(String userEmail, String cardNumber, String GiftID) {
		String payload = "{\n" + "  \"name\": \"Incumbent Loyalty Program\",\n" + "  \"birthday\": \"1999-01-01\",\n"
				+ "  \"phone\": 1111111111,\n" + "  \"email\": \"" + userEmail + "\",\n"
				+ "  \"original_membership_no\": 123456789,\n"
				+ "  \"registration_date\": \"2008-10-26T23:59:59-07:00\",\n"
				+ "  \"first_name\": \"FIRST_NAME_GOES_HERE\",\n" + "  \"last_name\": \"LAST_NAME_GOES_HERE\",\n"
				+ "  \"original_phone\": \"1111111111\",\n" + "  \"original_points\": 100,\n"
				+ "   \"rate_of_conversion\": \"\",\n" + "  \"fb_uid\": \"\",\n" + "  \"initial_points\": 3,\n"
				+ "  \"migrated_rewards_expiration_date\": \"2028-01-31\",\n"
				+ "  \"marketing_pn_subscription\": true,\n" + "  \"marketing_email_subscription\": false,\n"
				+ "  \"address_line1\": \"ADDRESS_GOES_HERE\",\n" + "  \"city\": \"Mountain View\",\n"
				+ "  \"state\": \"California\",\n" + "  \"zip_code\": \"94040\",\n"
				+ "  \"preferred_location\": \"123\",\n" + "  \"gift_cards\": [\n" + "    {\n"
				+ "      \"card_number\": \"" + cardNumber + "\",\n" + "      \"card_design_id\": \"" + GiftID + "\"\n"
				+ "    }\n" + "  ]\n" + "}";
		return payload;
	}

	public String fetchActivePurchasableSubscriptionPlanPayload(String client) {
		String payload = "{\n" + "   \"client\": \"" + client + "\"\n" + "}";
		return payload;
	}

	public String markoffersAsReadPayload(String client, String rewards, String event_type) {
		String payload = "{\n" + "   \"rewards\": \"" + rewards + "\",\n" + "   \"event_type\": \"" + event_type
				+ "\",\n" + "   \"client\": \"" + client + "\"\n" + "}";
		return payload;
	}

	public String posCheckinPayloadWithItemId(String date, String email, String key, String txn_no, String itemId1,
			String itemId2) {
		String payload = "{" +

				"\"menu_items\": [" + "{" + "\"item_name\":\"Margherita\"," + "\"item_qty\":\"1\","
				+ "\"item_amount\":10," + "\"menu_item_type\":\"M\"," + "\"menu_item_id\":\"" + itemId1 + "\","
				+ "\"menu_family\":\"106\"," + "\"menu_major_group\":\"100\"," + "\"serial_number\":\"1.0\"}," +

				"{" + "\"item_name\":\"Pizza\"," + "\"item_qty\":\"1\"," + "\"item_amount\":10,"
				+ "\"menu_item_type\":\"M\"," + "\"menu_item_id\":\"" + itemId2 + "\"," + "\"menu_family\":\"108\","
				+ "\"menu_major_group\":\"101\"," + "\"serial_number\":\"2.0\"}" +

				"]," +

				"\"receipt_datetime\": \"" + date + "\"," + "\"subtotal_amount\": 20," + "\"receipt_amount\": 20,"
				+ "\"email\":\"" + email + "\"," + "\"punchh_key\": \"" + key + "\"," + "\"transaction_no\": \""
				+ txn_no + "\"" + "}";
		return payload;
	}

	public String posRedemptionofRewardPayloadWithItemID(String email, String date, String punchhKey, String txn_no,
			String locationKey, String rewardId, String itemID) {

		String payload = "{\n" + "    \"location_key\": \"" + locationKey + "\",\n" + "    \"email\": \"" + email
				+ "\",\n" + "    \"receipt_amount\": 10.00,\n" + "    \"subtotal_amount\": 10.00,\n"
				+ "    \"receipt_datetime\": \"" + date + "\",\n" + "    \"sequence_no\": \"2191\",\n"
				+ "    \"discount_type\": \"reward\",\n" + "    \"reward_id\": \"" + rewardId + "\",\n"
				+ "    \"redeemed_points\": 40,\n" + "    \"punchh_key\": \"" + punchhKey + "\",\n"
				+ "    \"transaction_no\": \"" + txn_no + "\",\n" + "    \"menu_items\": [\n" + "        {\n"
				+ "            \"item_name\": \"Philly Cheesesteak Omelette\",\n" + "            \"item_qty\": 1,\n"
				+ "            \"item_amount\": 10.00,\n" + "            \"menu_item_type\": \"M\",\n"
				+ "            \"menu_item_id\": \"" + itemID + "\",\n" + "            \"menu_family\": \"106\",\n"
				+ "            \"menu_major_group\": \"100\",\n" + "            \"serial_number\": 1.0\n"
				+ "        }\n" + "    ]\n" + "}";

		return payload;
	}

	public Map<String, Object> identityUserSignUpWithEmailIdPayload(String client, String brandLevelToken,
			Boolean includePassword, String userEmail, String brandUserToken) {
		Map<String, Object> payload = new HashMap<>();
		String email = userEmail;
		Faker faker = new Faker();
		payload.put("access_token", brandLevelToken);
		payload.put("client", client);
		payload.put("email", email);
		if (includePassword)
			payload.put("password", "punchh@123<>&");
		payload.put("first_name", faker.name().firstName());
		payload.put("last_name", faker.name().lastName());
		payload.put("phone", "9" + faker.numerify("#########").toString());
		payload.put("birthday", "1991-09-18");
		payload.put("secondary_email", "sec_" + email);
		payload.put("marketing_email_subscription", true);
		payload.put("marketing_pn_subscription", true);
		payload.put("anniversary", "2020-09-18");
		payload.put("zip_code", faker.address().zipCode());
		payload.put("address", faker.address().streetAddress());
		payload.put("city", faker.address().city());
		payload.put("state", faker.address().state());
		payload.put("gender", "male");
		payload.put("send_compliance_sms", "true");
		payload.put("signup_channel", "Wifi");
		payload.put("title", "Mr.");
		payload.put("preferred_locale", "English");
		payload.put("age_verified", "true");
		payload.put("privacy_policy", "true");
		payload.put("subject_token", brandUserToken);
		return payload;
	}

	public String Api2RedemptionWithRewardIdAndLocationId(String client, String reward_id, String locationId) {
		String payload = "{\n\"client\":\"" + client + "\"," + "\n\"reward_id\": " + reward_id + ","
				+ "\n\"location_id\": \"" + locationId + "\"\n}";
		return payload;
	}

	public String Api2RedemptionWitReedemableIdAndLocationId(String client, String redeemableId, String locationId) {
		String payload = "{\n\"client\":\"" + client + "\"," + "\n\"redeemable_id\": " + redeemableId + ","
				+ "\n\"location_id\": " + locationId + "\n}";
		return payload;
	}

	public static String subscriptionPlanRedemptionCodeWithLocationId(String client, String subscription_id,
			String locationId) {
		String payload = "{\n\"client\":\"" + client + "\"," + "\n\"subscription_id\": " + subscription_id + ","
				+ "\n\"location_id\": " + locationId + "\n}";
		return payload;
	}

	public String apiV1UserSignupPayload(String email) {
		String fname = CreateDateTime.getUniqueString(prop.getProperty("firstName"));
		String lname = CreateDateTime.getUniqueString(prop.getProperty("lastName"));
		String password = prop.getProperty("password");
		String payload = "{\n" + "    \"user\": {\n" + "        \"email\": \"" + email + "\",\n"
				+ "        \"password\": \"" + password + "\",\n" + "        \"first_name\": \"" + fname + "\",\n"
				+ "        \"last_name\": \"" + lname + "\"\n" + "    }\n" + "}";
		return payload;
	}

	public String api2PurchaseSubscriptionPayloadWithPaymentCardWithoutStartEndDateTime(String plan_id,
			String purchase_price, String uuid, String auto_renewal) {
		String payload = "{\"plan_id\":\"" + plan_id + "\",\"external_plan_identifier\":\"\",\"auto_renewal\":\""
				+ auto_renewal + "\",\"payment_card_uuid\":\"" + uuid + "\"}";
		return payload;
	}

	public String apiV1UserLoginPayload(String email, String password) {
		String payload = "{\n" + "    \"user\": {\n" + "        \"email\": \"" + email + "\",\n"
				+ "        \"password\": \"" + password + "\"\n" + "    }\n" + "}";
		return payload;
	}

	public String redeemableListAPiPayload(String queryParameter, String pageNo, String redeemablePerPage) {
		String payload = "{\n" + "    \"query\": \"" + queryParameter + "\",\n" + "    \"page\": \"" + pageNo + "\",\n"
				+ "    \"per_page\": " + redeemablePerPage + "\n" + "}";
		return payload;
	}

	public String getLisDataPerPage(String queryKey, String queryKeyValuelisFilterQuery, String pageNo,
			String perPage) {
		String payload = "";
		if (queryKey != "") {
			payload = "{\"" + queryKey + "\":\"" + queryKeyValuelisFilterQuery + "\",\"page\":\"" + pageNo
					+ "\",\"per_page\":\"" + perPage + "\"}";
		} else {
			payload = "{\"query\":\"" + queryKeyValuelisFilterQuery + "\",\"page\":\"" + pageNo + "\",\"per_page\":\""
					+ perPage + "\"}";
		}
		return payload;
	}

	public String forceRedeemptionPayload(String userId, String forceRedemptioTypeVar, String requestedValueVar,
			String requestedValue) {
		String payload = "{\n" + "    \"user_id\": " + userId + ",\n" + "    \"redemption\": {\n"
				+ "        \"force_message\": \"Test\",\n" + "        \"force_redemption_type\": \""
				+ forceRedemptioTypeVar + "\",\n" + "        \"" + requestedValueVar + "\": " + requestedValue + "\n"
				+ "    }\n" + "}";
		return payload;
	}

	public String forceFuelRedeemptionPayload(String userId, String forceRedemptioTypeVar, String requestedValueVar,
			String requestedValue) {

		String payload = "{\n" + "    \"user_id\": " + userId + ",\n" + "    \"redemption\": {\n"
				+ "        \"force_message\": \"Test\",\n" + "        \"force_redemption_type\": \""
				+ forceRedemptioTypeVar + "\",\n" + "        \"" + requestedValueVar + "\": " + requestedValue + ",\n"
				+ "        \"fuel_redemption\": \"true\"\n" + "    }\n" + "}";
		return payload;
	}

	// shashank
	public String getQualificationCriteraPayload(String qcName, String qcExternalID, String line_item_selector_id,
			String qc_processing_function, String percentage_of_processed_amount) {
		String payload = "{\"data\":[{\"name\":\"" + qcName + "\",\"external_id\":\"" + qcExternalID
				+ "\",\"amount_cap\":\"1\",\"percentage_of_processed_amount\":\"" + percentage_of_processed_amount
				+ "\",\"qc_processing_function\":\"" + qc_processing_function
				+ "\",\"rounding_rule\":\"floor\",\"effective_location\":\"location:400707\",\"stack_discounting\":false,\"reuse_qualifying_items\":false,\"line_item_filters\":[{\"line_item_selector_id\":\""
				+ line_item_selector_id
				+ "\",\"processing_method\":\"\",\"quantity\":\"1\"}],\"enable_menu_item_aggregator\":false,\"aggregator_grouping_attributes\":{\"item_name\":true,\"item_id\":false,\"item_major_group\":false,\"item_family\":false,\"line_item_type\":false},\"item_qualifiers\":[{\"expression_type\":\"line_item_exists\",\"line_item_selector_id\":\""
				+ line_item_selector_id
				+ "\",\"net_value\":null}],\"receipt_qualifiers\":[{\"attribute\":\"total_amount\",\"operator\":\">=\",\"value\":10},{\"attribute\":\"receipt_hour\",\"operator\":\"in\",\"value\":1},{\"attribute\":\"receipt_week_day\",\"operator\":\"in\",\"value\":1},{\"attribute\":\"receipt_day\",\"operator\":\"in\",\"value\":10},{\"attribute\":\"subtotal_amount\",\"operator\":\">=\",\"value\":10},{\"attribute\":\"receipt_month\",\"operator\":\"==\",\"value\":10},{\"attribute\":\"receipt_year\",\"operator\":\"==\",\"value\":2024},{\"attribute\":\"revenue_code\",\"operator\":\"in\",\"value\":\"Online\"},{\"attribute\":\"revenue_id\",\"operator\":\"in\",\"value\":10},{\"attribute\":\"employee_id\",\"operator\":\"in\",\"value\":10},{\"attribute\":\"employee_name\",\"operator\":\"in\",\"value\":\"john\"},{\"attribute\":\"receipt_week\",\"operator\":\"==\",\"value\":10},{\"attribute\":\"receipt_minute\",\"operator\":\"in\",\"value\":10},{\"attribute\":\"channel\",\"operator\":\"in\",\"value\":\"POS\"},{\"attribute\":\"transaction_no\",\"operator\":\"mod\",\"value\":1001}]}]}";
		return payload;
	}

	public String getAuthApplicableOfferPayload(String client, String menu_item_id, String menu_item_id2,
			String authentication_token, String channel) {

		String payload = "{\"client\":\"" + client
				+ "\",\"menu_items\":[{\"item_name\":\"Pizza with Pepperoni Topping\",\"item_qty\":1,\"item_amount\":10.99,\"menu_item_type\":\"M\",\"menu_item_id\":\""
				+ menu_item_id
				+ "\",\"menu_family\":\"Pizza\",\"menu_major_group\":\"Pizza\",\"serial_number\":\"1.0\"},{\"item_name\":\"Pepperoni Topping\",\"item_qty\":1,\"item_amount\":0.99,\"menu_item_type\":\"M\",\"menu_item_id\":\""
				+ menu_item_id2
				+ "\",\"menu_family\":\"Pizza Topping\",\"menu_major_group\":\"Pizza\",\"serial_number\":\"1.1\"},{\"item_name\":\"Discount\",\"item_qty\":1,\"item_amount\":0.99,\"menu_item_type\":\"D\",\"menu_item_id\":\"4001\",\"menu_family\":\"Discount\",\"menu_major_group\":\"Discount\",\"serial_number\":\"2.0\"}],\"subtotal_amount\":10.00,\"receipt_amount\":10.00,\"receipt_datetime\":\"2022-09-012T07:42:03-00:00\",\"store_number\":\"001\",\"channel\":\""
				+ channel + "\",\"authentication_token\":\"" + authentication_token + "\"}";

		return payload;

	}

	public static String getRedeemablePayload(Map<String, String> redeemableDetailsMap) {
		String receiptRulePayload = getRedeemableReceiptRulePayloadNew(redeemableDetailsMap);
		String indefinetely = "false";
		String distributable = "false";
		String segment_definition_id = null;
		String description = "\"Test description\"";
		String startTime = "2028-06-25T23:59:59";
		String applicable_as_loyalty_redemption = "false";
		if (redeemableDetailsMap.get("indefinetely") != null) {
			indefinetely = redeemableDetailsMap.get("indefinetely");
		}
		if (redeemableDetailsMap.get("distributable") != null
				&& redeemableDetailsMap.get("segment_definition_id") != null) {
			distributable = redeemableDetailsMap.get("distributable");
			segment_definition_id = redeemableDetailsMap.get("segment_definition_id");
		}
		if (redeemableDetailsMap.get("description") != null) {
			description = redeemableDetailsMap.get("description");
		}
		if (redeemableDetailsMap.get("start_time") != null) {
			startTime = redeemableDetailsMap.get("start_time");
		}

		// Dyanmic update of applicable_as_loyalty_redemption
		if (redeemableDetailsMap.get("applicable_as_loyalty_redemption") != null) {
			applicable_as_loyalty_redemption = redeemableDetailsMap.get("applicable_as_loyalty_redemption");
		}

		String payload = "{\"data\":[{\"name\":\"" + redeemableDetailsMap.get("redeemableName")
				+ "\",\"external_id\":\"" + redeemableDetailsMap.get("external_id_redeemable")
				+ "\",\"alternate_locale_name\":" + description
				+ ",\"note\":\"Notes here\",\"allow_for_support_gifting\":true,\"available_as_template\":false,\"early_access\":true,\"distributable\":"
				+ distributable + ",\"distributable_to_all_users\":false,\"segment_definition_id\":"
				+ segment_definition_id + ",\"auto_applicable\":true,\"receipt_rule\":" + receiptRulePayload
				+ ",\"activate_now\":false,\"start_time\":\"" + startTime + "\",\"indefinetely\":" + indefinetely
				+ ",\"expiry_days\":" + redeemableDetailsMap.get("expiry_days") + ",\"end_time\":\""
				+ redeemableDetailsMap.get("end_time")
				+ "\",\"timezone\":\"Asia/Kolkata\",\"remind_before\":3,\"discount_channel\":\"all\",\"points\":12,\"redemption_code_expiry_mins\":3,\"applicable_as_loyalty_redemption\":"
				+ applicable_as_loyalty_redemption
				+ ",\"expire_redemption_code_with_reward_end_date\":false,\"template\":{\"redemption_message\":\"Countdown Message text\",\"short_prompt\":\"Short Prompt text\",\"standard_prompt\":\"Standard Prompt text\"},\"lag_duration\":{\"value\":2,\"units\":\"days\"},\"recurrence_schedule\":{\"occurrences\":10,\"days_distance\":2},\"effective_location\":null,\"meta_data\":\"meta data\"}]}";
		return payload;
	}

	public String pointForceRedeemWithType(String userId, String points, String type) {

		String payload = "{\"user_id\":" + userId + ",\"redemption\":{\"requested_punches\":" + points
				+ ",\"force_message\":\"This is a test\",\"force_redemption_type\":\"" + type + "\"}}";

		return payload;
	}

	public static String getRedeemableReceiptRulePayloadNew(Map<String, String> receiptRuleDetailsMap) {

		String inputStringFor_LIS = receiptRuleDetailsMap.get("lineitemSelector");

		if (receiptRuleDetailsMap.get("lineitemSelector") == null) {
			inputStringFor_LIS = "";
		} else {
			inputStringFor_LIS = receiptRuleDetailsMap.get("lineitemSelector");
		}

		String finalLineItemSelectorString = "";

		// checking line item selector keys e.g expression_type, line_item_selector_id,
		// net_value are part of input string or not if yes then update accordingly
		if (inputStringFor_LIS.contains("line_item_selector_id") || inputStringFor_LIS.contains("processing_method")
				|| inputStringFor_LIS.contains("quantity") && inputStringFor_LIS != null) {
			finalLineItemSelectorString = inputStringFor_LIS;
		} else {
			String[] str = inputStringFor_LIS.split("/");
			for (String s : str) {

				String innerStr[] = s.split(",");
				finalLineItemSelectorString = finalLineItemSelectorString + "{\"line_item_selector_id\":\""
						+ innerStr[0] + "\",\"processing_method\":\"" + innerStr[1] + "\",\"quantity\":" + innerStr[2]
						+ "},";
			}
			finalLineItemSelectorString = finalLineItemSelectorString.substring(0,
					finalLineItemSelectorString.length() - 1);

		}
		// checking item_qualifiers keys e.g expression_type, line_item_selector_id,
		// net_value are part of input string or not if yes then update accordingly
		String finalitem_qualifiers = "";
		if (receiptRuleDetailsMap.get("item_qualifiers") != null) {
			if (receiptRuleDetailsMap.get("item_qualifiers").contains("expression_type")
					|| receiptRuleDetailsMap.get("item_qualifiers").contains("line_item_selector_id")
					|| receiptRuleDetailsMap.get("item_qualifiers").contains("net_value")) {
				finalitem_qualifiers = receiptRuleDetailsMap.get("item_qualifiers");
			} else {
				finalitem_qualifiers = "{\"expression_type\":\"line_item_exists\",\"line_item_selector_id\":\""
						+ receiptRuleDetailsMap.get("line_item_selector_id") + "\",\"net_value\":null} ";

			}
		} else {
			finalitem_qualifiers = "{\"expression_type\":\"line_item_exists\",\"line_item_selector_id\":\""
					+ receiptRuleDetailsMap.get("line_item_selector_id") + "\",\"net_value\":null} ";

		}

		String receipt_qualifiers = "";
		if (receiptRuleDetailsMap.get("receipt_qualifiers") == null) {
			receipt_qualifiers = "[{\"attribute\":\"total_amount\",\"operator\":\">=\",\"value\":10},{\"attribute\":\"receipt_hour\",\"operator\":\"in\",\"value\":1}]";
		} else {
			receipt_qualifiers = receiptRuleDetailsMap.get("receipt_qualifiers");
		}

		String payload = "{\"qualifier_type\":\"" + receiptRuleDetailsMap.get("qualifier_type")
				+ "\",\"redeeming_criterion_id\":\"" + receiptRuleDetailsMap.get("redeeming_criterion_id")
				+ "\",\"discount_amount\":" + receiptRuleDetailsMap.get("discount_amount")
				+ ",\"redeeming_criterion\":{\"name\":\"" + receiptRuleDetailsMap.get("name") + "\",\"external_id\":\""
				+ receiptRuleDetailsMap.get("external_id") + "\",\"amount_cap\":"
				+ receiptRuleDetailsMap.get("amount_cap") + ",\"percentage_of_processed_amount\":"
				+ receiptRuleDetailsMap.get("percentage_of_processed_amount") + ",\"qc_processing_function\":\""
				+ receiptRuleDetailsMap.get("qc_processing_function")
				+ "\",\"rounding_rule\":\"ceil\",\"max_discount_units\":2,\"unit_discount\":10,\"minimum_unit_rate\":0.01,\"target_price\":1,\"effective_location\":null,\"stack_discounting\":false,\"reuse_qualifying_items\":false,\"line_item_filters\":["
				+ finalLineItemSelectorString
				+ "],\"enable_menu_item_aggregator\":false,\"aggregator_grouping_attributes\":{\"item_name\":false,\"item_id\":false,\"item_major_group\":false,\"item_family\":false,\"line_item_type\":false},\"item_qualifiers\":["
				+ finalitem_qualifiers + "],\"receipt_qualifiers\":" + receipt_qualifiers + "}}";

		return payload;
	}

	// vansham
	public static String getRedeemablePayloads(Map<String, String> redeemableDetailsMap) {

		String receiptRulePayload = getRedeemableReceiptRulePayloadNew(redeemableDetailsMap);
		String indefinetely = "false";
		String distributable = "false";
		String startTime = "2027-06-25T23:59:59";
		String segment_definition_id = null;
		if (redeemableDetailsMap.get("indefinetely") != null) {
			indefinetely = redeemableDetailsMap.get("indefinetely");
		}
		if (redeemableDetailsMap.get("distributable") != null
				&& redeemableDetailsMap.get("segment_definition_id") != null) {
			distributable = redeemableDetailsMap.get("distributable");
			segment_definition_id = redeemableDetailsMap.get("segment_definition_id");
		}
		if (redeemableDetailsMap.get("start_time") != null) {
			startTime = redeemableDetailsMap.get("start_time");
		}

		String payload = "{\"data\":[{\"name\":\"" + redeemableDetailsMap.get("redeemableName")
				+ "\",\"external_id\":\" " + redeemableDetailsMap.get("external_id_redeemable")
				+ "\",\"description\":\"description\",\"note\":\"Notes here\",\"allow_for_support_gifting\":true,\"available_as_template\":false,\"early_access\":true,\"distributable\":"
				+ distributable + ",\"distributable_to_all_users\":false,\"segment_definition_id\":"
				+ segment_definition_id + ",\"auto_applicable\":true,\"receipt_rule\":" + receiptRulePayload
				+ ",\"activate_now\":\"" + redeemableDetailsMap.get("active_now") + "\",\"start_time\":\"" + startTime
				+ "\",\"indefinetely\":" + indefinetely + ",\"expiry_days\":" + redeemableDetailsMap.get("expiry_days")
				+ ",\"end_time\":\"" + redeemableDetailsMap.get("end_time")
				+ "\",\"timezone\":\"Asia/Kolkata\",\"remind_before\":3,\"discount_channel\":\"all\",\"points\":12,\"redemption_code_expiry_mins\":3,\"applicable_as_loyalty_redemption\":false,\"expire_redemption_code_with_reward_end_date\":false,\"template\":{\"redemption_message\":\"Countdown Message text\",\"short_prompt\":\"Short Prompt text\",\"standard_prompt\":\"Standard Prompt text\"},\"lag_duration\":{\"value\":2,\"units\":\"days\"},\"recurrence_schedule\":{\"occurrences\":10,\"days_distance\":2},\"effective_location\":null,\"meta_data\":\"meta data\"}]}";
		return payload;

	}

	public String getQCPayload(String qcName, String qcExternalID, String line_item_selector_id,
			String line_item_selector_id2, String qc_processing_function, String percentage_of_processed_amount,
			String locationId, Map<String, String> DetailsMap) {
		String receipt_qualifiers = "";
		String itemQualifiers = "";
		String amount_cap, max_discount_units = "";
		String lif = "";
		String targetPrice = "";
		String rounding_rule = "";

		if (DetailsMap.get("receipt_qualifiers") == null) {
			receipt_qualifiers = "[{\"attribute\":\"total_amount\",\"operator\":\">=\",\"value\":10},{\"attribute\":\"receipt_hour\",\"operator\":\"in\",\"value\":1},{\"attribute\":\"receipt_week_day\",\"operator\":\"in\",\"value\":1},{\"attribute\":\"receipt_day\",\"operator\":\"in\",\"value\":10},{\"attribute\":\"subtotal_amount\",\"operator\":\">=\",\"value\":10},{\"attribute\":\"receipt_month\",\"operator\":\"==\",\"value\":10},{\"attribute\":\"receipt_year\",\"operator\":\"==\",\"value\":2024},{\"attribute\":\"revenue_code\",\"operator\":\"in\",\"value\":\"Online\"},{\"attribute\":\"revenue_id\",\"operator\":\"in\",\"value\":10},{\"attribute\":\"employee_id\",\"operator\":\"in\",\"value\":10},{\"attribute\":\"employee_name\",\"operator\":\"in\",\"value\":\"john\"},{\"attribute\":\"receipt_week\",\"operator\":\"==\",\"value\":10},{\"attribute\":\"receipt_minute\",\"operator\":\"in\",\"value\":10},{\"attribute\":\"channel\",\"operator\":\"in\",\"value\":\"POS\"},{\"attribute\":\"transaction_no\",\"operator\":\"mod\",\"value\":1001}]";
		} else {
			receipt_qualifiers = DetailsMap.get("receipt_qualifiers");
		}
		if (DetailsMap.get("item_qualifiers") == null) {
			itemQualifiers = "[{\"expression_type\":\"line_item_exists\",\"line_item_selector_id\":\""
					+ line_item_selector_id
					+ "\",\"net_value\":null},{\"expression_type\":\"line_item_exists\",\"line_item_selector_id\":\""
					+ line_item_selector_id
					+ "\",\"net_value\":null},{\"expression_type\":\"line_item_exists\",\"line_item_selector_id\":\""
					+ line_item_selector_id
					+ "\",\"net_value\":null},{\"expression_type\":\"line_item_exists\",\"line_item_selector_id\":\""
					+ line_item_selector_id
					+ "\",\"net_value\":null},{\"expression_type\":\"line_item_exists\",\"line_item_selector_id\":\""
					+ line_item_selector_id
					+ "\",\"net_value\":null},{\"expression_type\":\"line_item_exists\",\"line_item_selector_id\":\""
					+ line_item_selector_id2 + "\",\"net_value\":null}]";

		} else {
			itemQualifiers = DetailsMap.get("item_qualifiers");
		}

		if (DetailsMap.get("amount_cap") == null) {
			amount_cap = "1";
		} else {
			amount_cap = DetailsMap.get("amount_cap");
		}

		// String payload = "{\"data\":[{\"name\":\""+ qcName + "\",\"external_id\":\""+
		// qcExternalID +"\",\"amount_cap\":
		// "+amount_cap+",\"percentage_of_processed_amount\":\""+
		// percentage_of_processed_amount +"\",\"qc_processing_function\":\""+
		// qc_processing_function +"\",\"rounding_rule\":\"floor\",\"target_price\":
		// \"1\",\"effective_location\":\"location:"+locationId+"\",\"stack_discounting\":false,\"reuse_qualifying_items\":false,\"line_item_filters\":"+
		// lif
		// +",\"enable_menu_item_aggregator\":false,\"aggregator_grouping_attributes\":{\"item_name\":true,\"item_id\":false,\"item_major_group\":false,\"item_family\":false,\"line_item_type\":false},\"item_qualifiers\":"+
		// itemQualifiers +",\"receipt_qualifiers\":"+receipt_qualifiers+"}]}";
		if (DetailsMap.get("line_item_filters") == null) {
			lif = "[{\"line_item_selector_id\":\"" + line_item_selector_id
					+ "\",\"processing_method\":\"\",\"quantity\":\"1\"},{\"line_item_selector_id\":\""
					+ line_item_selector_id
					+ "\",\"processing_method\":\"\",\"quantity\":\"1\"},{\"line_item_selector_id\":\""
					+ line_item_selector_id
					+ "\",\"processing_method\":\"\",\"quantity\":\"1\"},{\"line_item_selector_id\":\""
					+ line_item_selector_id
					+ "\",\"processing_method\":\"\",\"quantity\":\"1\"},{\"line_item_selector_id\":\""
					+ line_item_selector_id
					+ "\",\"processing_method\":\"\",\"quantity\":\"1\"},{\"line_item_selector_id\":\""
					+ line_item_selector_id2 + "\",\"processing_method\":\"\",\"quantity\":\"1\"}]";
		} else {
			lif = DetailsMap.get("line_item_filters");
		}
		if (DetailsMap.get("target_price") == null) {
			targetPrice = "1";
		} else {
			targetPrice = DetailsMap.get("target_price");
		}

		if (DetailsMap.get("rounding_rule") == null) {
			rounding_rule = "";
		} else {
			rounding_rule = DetailsMap.get("rounding_rule");
		}

		if (DetailsMap.get("max_discount_units") == null) {
			max_discount_units = "2";
		} else {
			max_discount_units = DetailsMap.get("max_discount_units");
		}

		String payload = "{\"data\":[{\"name\":\"" + qcName + "\",\"external_id\":\"" + qcExternalID
				+ "\",\"amount_cap\":" + amount_cap + ",\"percentage_of_processed_amount\":\""
				+ percentage_of_processed_amount + "\",\"qc_processing_function\":\"" + qc_processing_function + "\","
				+ rounding_rule + "\"target_price\": \"" + targetPrice + "\",\"max_discount_units\": "
				+ max_discount_units + ",\"unit_discount\": \"2\",\"effective_location\": [\"" + locationId + "\"]"
				+ ",\"stack_discounting\":false,\"reuse_qualifying_items\":false,\"line_item_filters\":" + lif
				+ ",\"enable_menu_item_aggregator\":false,\"aggregator_grouping_attributes\":{\"item_name\":true,\"item_id\":false,\"item_major_group\":false,\"item_family\":false,\"line_item_type\":false},\"item_qualifiers\":"
				+ itemQualifiers + ",\"receipt_qualifiers\":" + receipt_qualifiers + "}]}";
		return payload;

	}

	public String API2UpdateRedeemableEndTime(String external_id, String endTime) {

		String payload = "{\n" + "    \"data\": [\n" + "        {\n" + "            \"external_id\": \"" + external_id
				+ "\",\n" + "            \"end_time\": \"" + endTime + "\"\n" + "        }\n" + "    ]\n" + "}";
		return payload;

	}

	public String heartbeatApi(String packageVersion, String packageVersionId, String LastUpdatedAt) {
		String Payload = "{\n" + "    \"package_version\": \"" + packageVersion + "\",\n"
				+ "    \"package_version_id\": \"" + packageVersionId + "\",\n" + "    \"last_updated_at\": \""
				+ LastUpdatedAt + "\",\n" + "    \"errors\": [\n" + "          {\n"
				+ "            \"eventId\": \"eventuid\",\n" + "            \"datetime\": \"2024-18-1 12:12:12.122\",\n"
				+ "            \"code\": 122,\n" + "            \"message1\": \"Message-555\"\n" + "        }\n"
				+ "     ]\n" + "\n" + "       \n" + "}";
		return Payload;
	}

	public String getWorldpayTransactionSetupIdPayload(String accountToken, boolean isCaptchaEnabled) {
		long timestamp = System.currentTimeMillis();
		String payload = "<TransactionSetup xmlns=\"https://transaction.elementexpress.com\"><Credentials><AccountID>1056415</AccountID>"
				+ "<AccountToken>" + accountToken + "</AccountToken>"
				+ "<AcceptorID>874767461</AcceptorID></Credentials><Application><ApplicationID>9472</ApplicationID>"
				+ "<ApplicationVersion>1.0</ApplicationVersion><ApplicationName>PaymentIntegrations</ApplicationName></Application>"
				+ "<Transaction><TransactionAmount>5.00</TransactionAmount><ReferenceNumber>" + timestamp
				+ "</ReferenceNumber>" + "<TicketNumber>" + timestamp
				+ "</TicketNumber><MarketCode>3</MarketCode></Transaction><Terminal>"
				+ "<TerminalID>011</TerminalID><CardholderPresentCode>7</CardholderPresentCode><CardInputCode>4</CardInputCode>"
				+ "<TerminalCapabilityCode>5</TerminalCapabilityCode><TerminalEnvironmentCode>6</TerminalEnvironmentCode>"
				+ "<CardPresentCode>3</CardPresentCode><MotoECICode>7</MotoECICode><TerminalType>2</TerminalType>"
				+ "<CVVPresenceCode>1</CVVPresenceCode></Terminal><TransactionSetup><TransactionSetupMethod>2</TransactionSetupMethod>"
				+ "<Embedded>1</Embedded><DeviceInputCode>3</DeviceInputCode><CVVRequired>1</CVVRequired>"
				+ (isCaptchaEnabled ? "<EnableCaptcha>1</EnableCaptcha>" : "")
				+ "</TransactionSetup><PaymentAccount><PaymentAccountType>0</PaymentAccountType>"
				+ "<PaymentAccountReferenceNumber>5435184541212311121</PaymentAccountReferenceNumber></PaymentAccount><Address>"
				+ "<BillingAddress1>4</BillingAddress1><BillingZipcode>30329</BillingZipcode></Address></TransactionSetup>";
		return payload;
	}

	public String getWorldpayPaymentAccountID(String accountToken, String accountId) {
		String payload = "<PaymentAccountCreateWithTransID xmlns=\"https://services.elementexpress.com\"><Credentials>"
				+ "<AccountID>1056415</AccountID><AccountToken>" + accountToken
				+ "</AccountToken><AcceptorID>874767461</AcceptorID>"
				+ "</Credentials><Application><ApplicationID>9472</ApplicationID><ApplicationVersion>1.0</ApplicationVersion>"
				+ "<ApplicationName>PaymentIntegrations</ApplicationName></Application><Transaction><TransactionID>"
				+ accountId + "</TransactionID>"
				+ "</Transaction><PaymentAccount><PaymentAccountType>0</PaymentAccountType>"
				+ "<PaymentAccountReferenceNumber>234567897654323</PaymentAccountReferenceNumber></PaymentAccount>"
				+ "</PaymentAccountCreateWithTransID>";
		return payload;
	}

	public String BatchRedemptionPosApiPayload(String locationKey, String userID, String Amount, String id,
			String externalUid, Map<String, String> DetailsMap) {

		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));
		String itemQuantity = "";
		if (DetailsMap.get("item_qty") == null) {
			itemQuantity = "1";
		} else {
			itemQuantity = DetailsMap.get("item_qty");
		}

		String payLoad = "{\"location_key\":\"" + locationKey

				+ "\",\"line_items\":[{\"item_name\":\"Pizza1\",\"item_qty\":" + itemQuantity + ",\"amount\":\""
				+ Amount + "\",\"item_type\":\"M\",\"item_id\":\"" + id
				+ "\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1}],\"receipt_datetime\":\"2022-11-24T14:14:07+05:30\",\"subtotal_amount\":"
				+ Amount + ",\"receipt_amount\":" + Amount + ",\"punchh_key\":\"" + punch_Key + "\",\"transaction_no\":"
				+ transaction_no + ",\"external_uid\":\"" + externalUid + "\",\"user_id\":\"" + userID + "\"}";

		System.out.println("payLoad for batch redemption :: - " + payLoad);

		return payLoad;

	}

	public String discountLookUpPosPayload(String locationKey, String userID, String item_id, String externalUid,
			String amount, Map<String, String> DetailsMap) {
		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));
		String itemQuantity = "";
		if (DetailsMap.get("item_qty") == null) {
			itemQuantity = "1";
		} else {
			itemQuantity = DetailsMap.get("item_qty");
		}

		String payLoad = "{\"location_key\":\"" + locationKey
				+ "\",\"line_items\":[{\"item_name\":\"Pizza3\",\"item_qty\":" + itemQuantity + ",\"amount\":\""
				+ amount + "\"," + "\"item_type\":\"M\",\"item_id\":\"" + item_id
				+ "\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":3}],"
				+ "\"receipt_datetime\":\"2023-06-26T14:14:07+05:30\",\"subtotal_amount\":" + amount
				+ ",\"receipt_amount\":" + amount + ",\"punchh_key\":\"" + punch_Key + "\"," + "\"transaction_no\":"
				+ transaction_no + ",\"external_uid\":\"" + externalUid + "\",\"user_id\":\"" + userID + "\"}";

		logger.info("payLoad for batch redemption :: - " + payLoad);
		return payLoad;

	}

	public String posPossibleRedemptionofRedeemablePayload(String email, String date, String punchhKey, String txn_no,
			String redeemable_id, String itemId) {

		String payload = "{\n" + "    \"email\": \"" + email + "\",\n" + "    \"receipt_amount\": 10.00,\n"
				+ "    \"subtotal_amount\": 10.00,\n" + "    \"receipt_datetime\": \"" + date + "\",\n"
				+ "    \"sequence_no\": \"2191\",\n" + "    \"discount_type\": \"redeemable\",\n"
				+ "    \"redeemable_id\": " + redeemable_id + ",\n" + "    \"punchh_key\": \"" + punchhKey + "\",\n"
				+ "    \"transaction_no\": \"" + txn_no + "\",\n" + "    \"menu_items\": [\n" + "        {\n"
				+ "            \"item_name\": \"Philly Cheesesteak Omelette\",\n" + "            \"item_qty\": 1,\n"
				+ "            \"item_amount\": 10.00,\n" + "            \"menu_item_type\": \"M\",\n"
				+ "            \"menu_item_id\": \"" + itemId + "\",\n" + "            \"menu_family\": \"106\",\n"
				+ "            \"menu_major_group\": \"100\",\n" + "            \"serial_number\": 1.0\n"
				+ "        }\n" + "    ]\n" + "}";

		return payload;
	}

	public String secureApiDiscountBasketAddedPayload(String discount_type, String discount_id, String externalUid) {

		String payLoad = "{\n" + "    \"discount_basket_items_attributes\": [\n" + "        {\n"
				+ "            \"discount_type\": \"" + discount_type + "\",\n" + "            \"discount_id\": \""
				+ discount_id + "\"\n" + "        }\n" + "    ],\n" + "    \"external_uid\": \"" + externalUid + "\"\n"
				+ "}";

		return payLoad;
	}

	public String discountAmountItemWithExtIdPayload(String discount_type, String discount_amount, String externalUid) {

		String payLoad = "{\n" + "    \"discount_basket_items_attributes\": [\n" + "        {\n"
				+ "            \"discount_type\": \"" + discount_type + "\",\n" + "            \"discount_value\": \""
				+ discount_amount + "\"\n" + "        }\n" + "    ],\n" + "    \"external_uid\": \"" + externalUid
				+ "\"\n" + "}";

		return payLoad;
	}

	public String discountBasketItemsAttributesWithExtUid(String discount_type, String discount_id,
			String externalUid) {

		String payLoad = "{\n" + "    \"discount_basket_items_attributes\": [\n" + "        {\n"
				+ "            \"discount_type\": \"" + discount_type + "\",\n" + "            \"discount_id\": \""
				+ discount_id + "\"\n" + "        }\n" + "    ],\n" + "    \"external_uid\": \"" + externalUid + "\"\n"
				+ "}";

		return payLoad;
	}

	public String removeDiscountBasketExtUIDMobileAPIPayload(String basketID, String external_uid) {
		String payload = "{\n" + "    \"discount_basket_item_ids\":\"" + basketID + "\",\n" + "    \"external_uid\":\""
				+ external_uid + "\"\n" + "}";
		return payload;
	}

	public String processBatchRedemptionOfBasketPOSDiscountLookupWithExtUidPayload(String locationKey, String userID,
			String subAmount, String externalUid, Map<String, Map<String, String>> parentMap) {
		String recieptPayload = "";
		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String head = "{\"location_key\":\"" + locationKey + "\",\"line_items\":[";

		String base = " ],\n" + "    \"receipt_datetime\": \"2022-11-24T14:14:07+05:30\",\n"
				+ "    \"subtotal_amount\": " + subAmount + ",\n" + "    \"receipt_amount\": " + subAmount + ",\n"
				+ "    \"punchh_key\": \"" + punch_Key + "\",\n" + "    \"transaction_no\": " + transaction_no + ",\n"
				+ "    \"external_uid\": \"" + externalUid + "\",\n" + "    \"user_id\": \"" + userID + "\"\n" + "}";

		for (Entry<String, Map<String, String>> entry : parentMap.entrySet()) {
			Map<String, String> valuesMap = entry.getValue();
			String str = "{\"item_name\":\"" + valuesMap.get("item_name") + "\",\"item_qty\":"
					+ valuesMap.get("item_qty") + ",\"amount\":\"" + valuesMap.get("amount") + "\",\"item_type\":\""
					+ valuesMap.get("item_type") + "\",\"item_id\":\"" + valuesMap.get("item_id")
					+ "\",\"item_family\":\"" + valuesMap.get("item_family") + "\",\"item_group\":\""
					+ valuesMap.get("item_group") + "\",\"serial_number\":" + valuesMap.get("serial_number") + "},";
			recieptPayload = recieptPayload + str;
		}
		recieptPayload = recieptPayload.substring(0, recieptPayload.length() - 1);
		String finalRecieptPayLoad = head + recieptPayload + base;
		logger.info("Test3234.generateReceipt()finalRecieptPayLoad =" + finalRecieptPayLoad);
		return finalRecieptPayLoad;
	}

	public String generateReceiptWithQueryParamWithExtUid(String locationKey, String userID, String subAmount,
			String query, String externalUid, Map<String, Map<String, String>> parentMap) {
		String recieptPayload = "";
		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));
		String head = "{\"location_key\":\"" + locationKey + "\",\"line_items\":[";
		String base = "],\n" + "    \"receipt_datetime\": \"2022-11-24T14:14:07+05:30\",\n"
				+ "    \"subtotal_amount\": " + subAmount + ",\n" + "    \"receipt_amount\": " + subAmount + ",\n"
				+ "    \"punchh_key\": \"" + punch_Key + "\",\n" + "    \"transaction_no\": " + transaction_no + ",\n"
				+ "    \"query\": " + query + ",\n" + "    \"external_uid\": \"" + externalUid + "\",\n"
				+ "    \"user_id\": \"" + userID + "\"\n" + "}";

		for (Entry<String, Map<String, String>> entry : parentMap.entrySet()) {
			Map<String, String> valuesMap = entry.getValue();
			String str = "{\"item_name\":\"" + valuesMap.get("item_name") + "\",\"item_qty\":"
					+ valuesMap.get("item_qty") + ",\"amount\":\"" + valuesMap.get("amount") + "\",\"item_type\":\""
					+ valuesMap.get("item_type") + "\",\"item_id\":\"" + valuesMap.get("item_id")
					+ "\",\"item_family\":\"" + valuesMap.get("item_family") + "\",\"item_group\":\""
					+ valuesMap.get("item_group") + "\",\"serial_number\":" + valuesMap.get("serial_number") + "},";
			recieptPayload = recieptPayload + str;
		}
		recieptPayload = recieptPayload.substring(0, recieptPayload.length() - 1);
		String finalRecieptPayLoad = head + recieptPayload + base;
		logger.info("Test3234.generateReceipt()finalRecieptPayLoad =" + finalRecieptPayLoad);
		return finalRecieptPayLoad;
	}

	public String getBatchRedemptionsAuthApiPayload(String locationKey, String userID, String item_id,
			String externalUid, Map<String, String> DetailsMap) {

		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));
		String itemQuantity = "";
		if (DetailsMap.get("item_qty") == null) {
			itemQuantity = "1";
		} else {
			itemQuantity = DetailsMap.get("item_qty");
		}

		String payLoad = "{\"location_key\":\"" + locationKey
				+ "\",\"line_items\":[{\"item_name\":\"Pizza1\",\"item_qty\":" + itemQuantity
				+ ",\"amount\":10,\"item_type\":\"M\",\"item_id\":\"" + item_id
				+ "\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1}],\"receipt_datetime\":\"2023-01-20T07:17:00Z\",\"subtotal_amount\":10,\"receipt_amount\":10,\"punchh_key\":\""
				+ punch_Key + "\",\"transaction_no\":" + transaction_no + ",\"external_uid\":\"" + externalUid
				+ "\",\"user_id\":\"" + userID + "\"}";

		logger.info("payLoad for batch redemption :: - " + payLoad);

		return payLoad;
	}

	public String provisionApi(int policyId, List<Integer> locationIds, String packageVersionId) {
		String locationIdsJson = locationIds.stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));

		String payload = "{\n" + "    \"pos_type_id\": " + 1 + ",\n" + "    \"policy_id\":  " + policyId + ",\n"
				+ "    \"location_ids\": " + locationIdsJson + ",\n" + "    \"selection_type\": \"custom\",\n"
				+ "    \"remote_upgrade\": " + true + ",\n" + "    \"package_version_id\": \"" + packageVersionId
				+ "\"\n" + "}";

		return payload;
	}

	public String deprovisionApi(List<Integer> locationIds) {
		String locationIdsJson = locationIds.stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));

		String payload = "{\n" + "    \"selection_type\": \"custom\",\n" + "    \"location_ids\": " + locationIdsJson
				+ "\n" + "}";
		return payload;
	}

	public String configOverride(List<Integer> locationIds, JSONObject configsToOverride) {
		String locationIdsJson = locationIds.stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));

		String payload = "{\n" + "    \"selection_type\": \"custom\",\n" + "    \"config_overrides\": "
				+ configsToOverride + ",\n" + "    \"location_ids\": " + locationIdsJson + ",\n"
				+ "    \"pos_type_id\": 1\n" +

				"}";

		return payload;
	}

	// public String finalPayloadAfterAddingMultipleLineItems(List<Map<String,
	// Object>> lineItems, String receipt_datetime,
	public String finalPayloadAfterAddingMultipleLineItems(List<Map<String, Object>> lineItems, String receipt_datetime,
			double subtotal_amount, double receipt_amount, String punchh_key, String transaction_no, String user_id,
			String location_key, String external_uid) throws JsonProcessingException {

		// Prepare the data
		Map<String, Object> payload = new HashMap<>();

		// Add line_items to payload
		payload.put("line_items", lineItems);

		// Add other required fields to payload
		payload.put("receipt_datetime", receipt_datetime);
		payload.put("subtotal_amount", subtotal_amount);
		payload.put("receipt_amount", receipt_amount);
		payload.put("punchh_key", punchh_key);
		payload.put("transaction_no", transaction_no);
		payload.put("user_id", user_id);
		payload.put("location_key", location_key);
		payload.put("external_uid", external_uid);

		// Convert the payload map to a JSON string using Jackson ObjectMapper
		ObjectMapper objectMapper = new ObjectMapper();
		String jsonPayload = objectMapper.writeValueAsString(payload);
		return jsonPayload;

	}

	public String webhookZiplinePayload(String email, String cardNumber, String status, String action) {
		String payload = "";
		long cardDate = System.currentTimeMillis() / 1000;
		if ("create".equals(action)) {
			payload = "{\"UserData\":[{\"CardNumber\":\"" + cardNumber
					+ "\",\"FirstName\":\"TEST1\",\"LastName\":\"LastTEST\"," + "\"Email\":\"" + email
					+ "\",\"Status\":\"" + status + "\","
					+ "\"Description\":\"NAP Hold- Identity verification needed.\",\"StreetAddress\":\"555 Some Street\","
					+ "\"City\":\"Portsmouth\",\"State\":\"ME\",\"ZipCode\":\"01234\",\"PhoneNumber\":\"5063832444\","
					+ "\"MobilePhoneNumber\":\"\",\"Birthdate\":\"1987-10-02\"}]}";
		} else {
			payload = "{\"CardStatusChanges\":[{\"CardNumber\":\"" + cardNumber + "\",\"Status\":\"" + status
					+ "\",\"LastModDate\":" + (cardDate - 60) + ",\"EnrollDate\":" + cardDate
					+ ",\"Description\":\"NAP Hold- Identity verification needed.\"}]}";

		}

		return payload;
	}

	public String loyaltyCardPayload(String cardNumber, boolean isCreate) {
		String key = isCreate ? "card_number" : "new_card_number";
		return "{\"" + key + "\":\"" + cardNumber + "\"}";
	}

	public String api1UpdateUserLoyaltyCard(String card_number) {
		String payload = "{\"card_number\": \"" + card_number + "\"}";
		return payload;
	}

	public static String posCheckinLoyaltyCardPayLoad(String card_number, String punchhKey, String amount) {
		String transactionNumber = CreateDateTime.getRandomNumberSixDigit() + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		String payload = "{\n" + "     \"menu_items\":\n" + "    [\n" + "        {\n"
				+ "                \"item_name\":\"Margherita\",\n" + "                \"item_qty\":1,\n"
				+ "                \"item_amount\":100,\n" + "                \"menu_item_type\":\"M\",\n"
				+ "                \"menu_item_id\":\"110\",\n" + "                \"menu_family\":\"106\",\n"
				+ "                \"menu_major_group\":\"100\",\n" + "                \"serial_number\":1.0\n"
				+ "        },\n" + "        {\n" + "                \"item_name\":\"Pizza\",\n"
				+ "                \"item_qty\":1,\n" + "                \"item_amount\":110,\n"
				+ "                \"menu_item_type\":\"M\",\n" + "                \"menu_item_id\":\"111\",\n"
				+ "                \"menu_family\":\"106\",\n" + "                \"menu_major_group\":\"100\",\n"
				+ "                \"serial_number\":1.0\n" + "        }\n" + "    ],\n"
				+ "    \"receipt_datetime\": \"" + date + "\",\n" + "    \"subtotal_amount\": " + amount + ",\n"
				+ "    \"receipt_amount\": " + amount + ",\n" + "    \"card_number\":\"" + card_number + "\",\n"
				+ "    \"punchh_key\":\"" + punchhKey + "\",\n" + "    \"transaction_no\":\"" + "12345"
				+ transactionNumber + "\"" + "\n" + "}";
		return payload;
	}

	public String API2PointConversionPayload(String client, String conversionRuleId, String sourceValue,
			String convertedValue) {

		String payload = "{\n" + "  \"client\": \"" + client + "\",\n" + "     \"conversion_rule_id\": "
				+ conversionRuleId + ",\n" + "     \"source_value\": 100,\n" + "     \"converted_value\":\"10\"\n"
				+ "}";

		return payload;

	}

	public String updatePackagePayload(String description) {
		String payload = "{\n" + "    \"is_assignable\": true,\n" + "    \"is_generic\": true,\n"
				+ "    \"pos_type\": [1],\n" + "    \"description\": \"" + description + "\",\n"
				+ "    \"stage\": \"dev\",\n" + "    \"components\": [\n" + "        {\n"
				+ "            \"name\": \"UpdatedPunchhMonitor.exe\",\n"
				+ "            \"checksum\": \"Updatedc94fcb5ce8cd2171c55381db97b68a4bc4990707710116d3af2ccd9947b92989\",\n"
				+ "            \"version\": \"Updated10.7.31.0\"\n" + "        }\n" + "    ],\n"
				+ "    \"release_notes\": {\n"
				+ "        \"internal\": \"Updated internal release notes for package with automation\",\n"
				+ "        \"external\": \"Updated external release notes for package with automation\"\n" + "    }\n"
				+ "}";

		return payload;
	}

	public String policyPayload(String policyName, int posTypeId, String status) {
		String payload = "{\n" + "    \"name\": \"" + policyName + "\",\n" + "    \"description\": \"" + policyName
				+ " Description" + "\",\n" + "    \"status\": \"" + status + "\",\n" + "    \"pos_type_id\": "
				+ posTypeId + ",\n" + "    \"config\": {\n" + "        \"common_config\": {\n"
				+ "            \"POS URL\": \"https://mobileapi.staging.punchh.io\",\n"
				+ "            \"ISL URL\": \"https://isl.staging.punchh.io/\",\n"
				+ "            \"LOG UPLOAD URL\": \"https://punchhapi.staging.punchh.io\",\n"
				+ "            \"Log Level\": \"6\",\n" + "            \"POS Configuration Update Interval\": \"60\",\n"
				+ "            \"Maintenance Start Time\": \"4:00 AM\",\n" + "            \"Language\": \"en-US\",\n"
				+ "            \"Regex Filter\": \"\",\n" + "            \"Keep Socket Open\": \"false\",\n"
				+ "            \"XML Mode\": \"true\"\n" + "        },\n" + "        \"pos_config\": {\n"
				+ "            \"Update Time Window\": [\n" + "                \"09:00 PM-2:00 AM\"\n"
				+ "            ],\n" + "            \"Port\": \"8008\",\n" + "            \"Comp ID\": \"1222\",\n"
				+ "            \"Punchh Item ID\": \"1111\",\n" + "            \"Redeem Item ID\": \"1000\",\n"
				+ "            \"Void Reason\": \"999\",\n" + "            \"PayPal Tender ID\": \"12\",\n"
				+ "            \"PayPal Item ID\": \"8008\",\n" + "            \"Venmo Tender ID\": \"8008\",\n"
				+ "            \"Venmo Item ID\": \"8008\",\n" + "            \"Payment Tender ID\": \"\",\n"
				+ "            \"Third Party Item ID\": \"\",\n" + "            \"Payment Item ID\": \"\",\n"
				+ "            \"Points Item Category\": \"\",\n"
				+ "            \"Redeem Message\": \"Redeem Message\",\n" + "            \"Earn Message\": \"\",\n"
				+ "            \"Order Items\": \"false\",\n" + "            \"Scan Any Time\": \"false\",\n"
				+ "            \"UI Mode\": \"FULL\",\n" + "            \"Use Barcode Scan Interface\": \"false\",\n"
				+ "            \"Use MSR\": \"false\",\n" + "            \"Enable Barcode Printing\": \"true\",\n"
				+ "            \"Barcode on Redeem\": \"false\",\n" + "            \"Print QRC\": \"false\",\n"
				+ "            \"Disable By Order Mode\": \"\",\n"
				+ "            \"Disable By Revenue Center\": \"\",\n"
				+ "            \"Barcode On Reprint\": \"false\",\n"
				+ "            \"Barcode Only on Closed Check\": \"true\",\n"
				+ "            \"Filter Item Category\": \"\",\n" + "            \"Points Only Customer\": \"false\",\n"
				+ "            \"Allow Single Sign On\": \"false\",\n"
				+ "            \"SSF Auto Apply Payment\": \"true\",\n"
				+ "            \"Allow Alpha Keyboard Popup\": \"true\",\n"
				+ "            \"Allow Be Back\": \"true\",\n" + "            \"Auto Create Customer\": \"false\",\n"
				+ "            \"Auto Check-In\": \"true\",\n" + "            \"Auto Close On Redeem\": \"true\",\n"
				+ "            \"Coupon Prefix\": \"\",\n" + "            \"Menu Item Prefix\": \"\",\n"
				+ "            \"Barcode On Checkin\": \"false\",\n"
				+ "            \"Barcode On Zero Check\": \"false\",\n"
				+ "            \"Enable Loyalty Chit\": \"false\",\n"
				+ "            \"Has Gift Card Integration\": \"false\",\n"
				+ "            \"Gift Card Vendor\": \"\",\n" + "            \"Gift Card User\": \"\",\n"
				+ "            \"Gift Card Password\": \"\",\n" + "            \"Gift Card Merchant ID\": \"\",\n"
				+ "            \"Gift Card Merchant Name\": \"\",\n" + "            \"Use Spanish\": \"0\",\n"
				+ "            \"Gift Card Store ID\": \"\",\n" + "            \"Gift Card Routing ID\": \"\",\n"
				+ "            \"Gift Card Item ID\": \"\",\n" + "            \"Gift Card Tender ID\": \"\",\n"
				+ "            \"Gift Card Proxy Port\": \"\",\n" + "            \"Gift Card Url\": \"\"\n"
				+ "        }\n" + "    }\n" + "}";

		return payload;
	}

	public String remoteUpgrade(String packageVersionId, List<Integer> locationIds) {
		String locationIdsJson = locationIds.stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));
		String payload = "{\n" + "    \"pos_type_id\": 1,\n" + "    \"package_version_id\": \"" + packageVersionId
				+ "\",\n" + "    \"location_ids\": " + locationIdsJson + ",\n" + "    \"selection_type\": \"custom\"\n"
				+ "}";

		return payload;
	}

	public String initiateUpdate(List<Integer> locationIds) {
		String locationIdsJson = locationIds.stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));
		String payload = "{\n" + "    \"location_ids\": " + locationIdsJson + ",\n"
				+ "    \"selection_type\": \"custom\"\n" + "}";

		return payload;
	}

	public String cancelUpdate(List<Integer> locationIds) {
		String locationIdsJson = locationIds.stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));
		String payload = "{\n" + "    \"location_ids\": " + locationIdsJson + ",\n"
				+ "    \"selection_type\": \"custom\"\n" + "}";

		return payload;
	}

	public String reprovisionLocation(List<Integer> locationIds, int policyId) {
		String locationIdsJson = locationIds.stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));
		String payload = "{\n" + "    \"location_ids\": " + locationIdsJson + ",\n"
				+ "    \"selection_type\": \"custom\",\n" + "    \"policy_id\":  " + policyId + "\n" + "}";

		return payload;
	}

	public static Map<String, Object> getInputForReceiptItems(String itemName, int itemQty, double amount,
			String itemType, String itemId, String itemFamily, String itemGroup, String serialNumber) {
		Map<String, Object> lineItem = new HashMap<>();
		lineItem.put("item_name", itemName);
		lineItem.put("item_qty", itemQty);
		lineItem.put("amount", amount);
		lineItem.put("item_type", itemType);
		lineItem.put("item_id", itemId);
		lineItem.put("item_family", itemFamily);
		lineItem.put("item_group", itemGroup);
		lineItem.put("serial_number", serialNumber);
		return lineItem;
	}

	public String paymentCardPayload(String adapter_code, String tx_token, String nickname, boolean preferred) {
		String payload = "{\"transaction_token\":\"" + tx_token + "\",\"adapter_code\":\"" + adapter_code
				+ "\",\"nickname\":\"" + nickname + "\",\"preferred\":\"" + preferred
				+ "\",\"billing_info\":{\"name\":\"Ankur Nehra\",\"country\":\"USA\"}}";
		return payload;
	}

	public String API1PurchaseGiftcardWithRecurring(String design_id, String amount, String trToken) {

		String payload = "{\"design_id\":\"" + design_id + "\",\"amount\":\"" + amount + "\",\"transaction_token\":\""
				+ trToken + "\",\"payment_type\":\"recurring\"}";
		return payload;
	}

	public String finalPayloadAfterAddingMultipleLineItemsWithQueryParam(List<Map<String, Object>> lineItems,
			String receipt_datetime, double subtotal_amount, double receipt_amount, String punchh_key,
			String transaction_no, String user_id, String location_key, String external_uid, String query, int storeNum)
			throws JsonProcessingException {

		// Prepare the data
		Map<String, Object> payload = new HashMap<>();

		// Add line_items to payload
		payload.put("line_items", lineItems);

		// Add other required fields to payload
		payload.put("receipt_datetime", receipt_datetime);
		payload.put("subtotal_amount", subtotal_amount);
		payload.put("receipt_amount", receipt_amount);
		payload.put("punchh_key", punchh_key);
		payload.put("transaction_no", transaction_no);
		payload.put("user_id", user_id);
		payload.put("location_key", location_key);
		payload.put("external_uid", external_uid);
		payload.put("query", query);
		payload.put("store_number", storeNum);

		// Convert the payload map to a JSON string using Jackson ObjectMapper
		ObjectMapper objectMapper = new ObjectMapper();
		String jsonPayload = objectMapper.writeValueAsString(payload);
		return jsonPayload;
	}

	public String API1IssuanceGiftCard(String design_id) {

		String payload = "{\"design_id\":\"" + design_id + "\"}";
		return payload;
	}

	public String API2ClientPayload(String client, String email) {

		String payload = "{\n" + "  \"client\": \"" + client + "\",\n" + "  \"sharer_email\": \"" + email + "\"\n"
				+ "}";
		return payload;
	}

	public String API1GiftCardConsolidate(String gcUUID) {
		String payload = "{\"source_card\":\"" + gcUUID + "\"}";
		return payload;
	}

	public String API1GiftCardAutoreloadConfig(boolean preferred, boolean enableAutoreload, String thrAmt,
			String defAmt, String payCardUUID) {

		String payload = "{\"preferred\":\"" + preferred + "\",\"auto_reload_enabled\":\"" + enableAutoreload
				+ "\",\"threshold_amount\":\"" + thrAmt + "\",\"default_amount\":\"" + defAmt
				+ "\",\"payment_card_id\":\"" + payCardUUID + "\"}";
		return payload;
	}

	public String posAPIDiscountLookUpPayloadWithChannel(String locationKey, String userID, String item_id,
			String externalUID, String channel) {
		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));
		String payLoad = "{\"location_key\":\"" + locationKey
				+ "\",\"line_items\":[{\"item_name\":\"Pizza1\",\"item_qty\":1,\"amount\":10,\"item_type\":\"M\",\"item_id\":\""
				+ item_id
				+ "\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1}],\"receipt_datetime\":\"2023-01-20T07:17:00Z\",\"subtotal_amount\":10,\"receipt_amount\":10,\"punchh_key\":\""
				+ punch_Key + "\",\"transaction_no\":" + transaction_no + ",\"user_id\":\"" + userID
				+ "\",\"channel\": \"" + channel + "\",\"external_uid\": \"" + externalUID + "\"}";
		logger.info("payLoad for batch redemption :: - " + payLoad);
		return payLoad;
	}

	public String kouponMediaAgeVerificationPayload(String userID, String status, String dateTime) {
		String payLoad = "{\n" + "    \"user_id\" : " + userID + ",\n" + "    \"age_verified\" : " + status + ",\n"
				+ "    \"verification_at\" : \"" + dateTime + "\" \n" + "}";
		logger.info("payLoad is ---- " + payLoad);
		return payLoad;
	}

	public String processBatchRedemptionPosApiPayload(String locationKey, String userID, String Amount, String qty,
			String id, String externalUid, Map<String, String> DetailsMap) {

		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));
		String lineItems = "";
		if (DetailsMap.get("line_items") == null) {
			lineItems = "[{\"item_name\":\"Pizza1\",\"item_qty\":" + qty + ",\"amount\":" + Amount
					+ ",\"item_type\":\"M\",\"item_id\":\"" + id
					+ "\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":1}]";
		} else {
			lineItems = DetailsMap.get("line_items");
		}
		String payLoad = "{\"location_key\":\"" + locationKey

				+ "\",\"line_items\": " + lineItems
				+ ",\"receipt_datetime\":\"2022-11-24T14:14:07+05:30\",\"subtotal_amount\":" + Amount
				+ ",\"receipt_amount\":" + Amount + ",\"punchh_key\":\"" + punch_Key + "\",\"transaction_no\":"
				+ transaction_no + ",\"external_uid\":\"" + externalUid + "\",\"user_id\":\"" + userID + "\"}";
		logger.info("payLoad for batch redemption :: - " + payLoad);

		return payLoad;
	}

	public String posBatchRedemptionPayload(List<Map<String, Object>> lineItems, String receipt_datetime,
			String subtotal_amount, String receipt_amount, String punchh_key, String transaction_no, String user_id,
			String location_key, String external_uid) throws Exception {
		Map<String, Object> payload = new HashMap<>();
		payload.put("line_items", lineItems);
		payload.put("receipt_datetime", receipt_datetime);
		payload.put("subtotal_amount", Double.parseDouble(subtotal_amount));
		payload.put("receipt_amount", Double.parseDouble(receipt_amount));
		payload.put("punchh_key", punchh_key);
		payload.put("transaction_no", transaction_no);
		payload.put("user_id", user_id);
		payload.put("location_key", location_key);
		payload.put("external_uid", external_uid);

		ObjectMapper objectMapper = new ObjectMapper();
		String jsonPayload = objectMapper.writeValueAsString(payload);
		return jsonPayload;
	}

	public String discountLookUpPosApiPayload(String locationKey, String userID, String item_id, String externalUid,
			String amount, Map<String, String> DetailsMap) {
		String lineItems = "";
		if (DetailsMap.get("line_items") == null) {
			lineItems = "[{\"item_name\":\"Pizza3\",\"item_qty\":1,\"amount\":\"" + amount + "\",\"\n"
					+ "+ \"\"item_type\":\"M\",\"item_id\":\"" + item_id
					+ "+ \"\",\"item_family\":\"10\",\"item_group\":\"999\",\"serial_number\":3}]";
		} else {
			lineItems = DetailsMap.get("line_items");
		}
		String punch_Key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		String payLoad = "{\"location_key\":\"" + locationKey + "\",\"line_items\": " + lineItems
				+ ",\"receipt_datetime\":\"2023-06-26T14:14:07+05:30\",\"subtotal_amount\":" + amount
				+ ",\"receipt_amount\":" + amount + ",\"punchh_key\":\"" + punch_Key + "\"," + "\"transaction_no\":"
				+ transaction_no + ",\"external_uid\":\"" + externalUid + "\",\"user_id\":\"" + userID + "\"}";

		logger.info("payLoad for batch redemption :: - " + payLoad);
		return payLoad;
	}

	public String supportGiftingToUserFalse(String userID) {

		String payload = "{\n" + "  \"user_id\": " + userID + ",\n" + "  \"subject\": \"Gifts from automation\",\n"
				+ "  \"message\": \"Thank you for contacting us. Here are 50 extra points to make your day from automation test.\",\n"
				+ "  \"gift_reason\": \"api test\",\n" + "   \"gift_count\": \"200\",\n"
				+ "  \"exclude_from_membership_points\": false\n" + "}";
		return payload;
	}

	public String sendPointsToUserFalse(String userID) {

		String payload = "{\n" + "   \"user_id\": \"" + userID + "\",\n"
				+ "   \"message\": \"Automation test message\",\n" + "   \"subject\": \"Message from automation\",\n"
				+ "   \"gift_count\": \"200\",\n" + "   \"gift_reason\": \"\",\n"
				+ "  \"exclude_from_membership_points\": false\n" + "}";

		return payload;
	}

	public String supportGiftingToUserTrue(String userID) {

		String payload = "{\n" + "  \"user_id\": " + userID + ",\n" + "  \"subject\": \"Gifts from automation\",\n"
				+ "  \"message\": \"Thank you for contacting us. Here are 50 extra points to make your day from automation test.\",\n"
				+ "  \"gift_reason\": \"api test\",\n" + "   \"gift_count\": \"200\",\n"
				+ "  \"exclude_from_membership_points\": true\n" + "}";

		return payload;
	}

	public String sendPointsToUserTrue(String userID) {

		String payload = "{\n" + "   \"user_id\": \"" + userID + "\",\n"
				+ "   \"message\": \"Automation test message\",\n" + "   \"subject\": \"Message from automation\",\n"
				+ "   \"gift_count\": \"200\",\n" + "   \"gift_reason\": \"\",\n"
				+ "  \"exclude_from_membership_points\": true\n" + "}";
		return payload;
	}

	public String API2SendPointsToUser(String userID, String gift_count) {

		String payload = "{\n" + "   \"user_id\": \"" + userID + "\",\n"
				+ "   \"message\": \"Automation test message\",\n" + "   \"subject\": \"Message from automation\",\n"
				+ "   \"gift_count\": \"" + gift_count + "\", \n" + "   \"reset_guest_last_activity\": \"" + true
				+ "\", \n" + "   \"gift_reason\": \"Test\"\n" + "}";
		return payload;
	}

	public String API2SendPointsRedeemableToUser(String userID, String gift_count, String redeemable_id) {

		String payload = "{\n" + "   \"user_id\": \"" + userID + "\",\n"
				+ "   \"message\": \"Automation test message\",\n" + "   \"subject\": \"Message from automation\",\n"
				+ "   \"redeemable_id\": \"" + redeemable_id + "\",\n" + "   \"gift_count\": \"" + gift_count + "\", \n"
				+ "   \"reset_guest_last_activity\": \"" + true + "\", \n" + "   \"gift_reason\": \"Test\"\n" + "}";

		return payload;
	}

	public String API2SendCurrencyToUser(String userID, String reward_amount) {

		String payload = "{\n" + "   \"user_id\": \"" + userID + "\",\n"
				+ "   \"message\": \"Automation test message\",\n" + "   \"subject\": \"Message from automation\",\n"
				+ "   \"reward_amount\": \"" + reward_amount + "\", \n" + "   \"gift_reason\": \"Test\"\n" + "}";
		return payload;
	}

	public String API2SendPointsWitLocationToUser(String userID, String gift_count, String location_id) {

		String payload = "{\n" + "   \"user_id\": \"" + userID + "\",\n"
				+ "   \"message\": \"Automation test message\",\n" + "   \"subject\": \"Message from automation\",\n"
				+ "    \"gift_count\": \"" + gift_count + "\", \n" + "   \"location_id\": \"" + location_id + "\",\n"
				+ "   \"reset_guest_last_activity\": \"" + true + "\", \n" + "   \"gift_reason\": \"Test\"\n" + "}";

		return payload;
	}

	public String API2SendOfferToUser(String userID, String redeemable_id, String end_date) {

		String payload = "{\n" + "   \"user_id\": \"" + userID + "\",\n"
				+ "   \"message\": \"Automation test message\",\n" + "   \"subject\": \"Message from automation\",\n"
				+ "   \"redeemable_id\": \"" + redeemable_id + "\",\n" + "   \"end_date\": \"" + end_date + "\", \n"
				+ "   \"gift_reason\": \"Test\"\n" + "}";

		return payload;
	}

	public String API2SendPointsToUserInvalidJson(String userID, String gift_count) {

		String payload = "{\n" + "   \"user_id\": \"" + userID + "\"\n"
				+ "   \"message\": \"Automation test message\",\n" + "   \"subject\": \"Message from automation\",\n"
				+ "   \"gift_count\": \"" + gift_count + "\", \n" + "   \"reset_guest_last_activity\": \"" + true
				+ "\", \n" + "   \"gift_reason\": \"Test\"\n" + "}";
		return payload;
	}

	public String API2SendPointsToUserWithExcludeFlagFalse(String userID, String gift_count) {

		String payload = "{\n" + "   \"user_id\": \"" + userID + "\",\n"
				+ "   \"message\": \"Automation test message\",\n" + "   \"subject\": \"Message from automation\",\n"
				+ "   \"gift_count\": \"" + gift_count + "\", \n" + "   \"reset_guest_last_activity\": \"" + true
				+ "\", \n" + "   \"gift_reason\": \"Test\", \n" + "   \"exclude_from_membership_points\": " + false
				+ "\n" + "}";
		return payload;
	}

	public String API2SendPointsToUserWithExcludeFlagTrue(String userID, String gift_count) {

		String payload = "{\n" + "   \"user_id\": \"" + userID + "\",\n"
				+ "   \"message\": \"Automation test message\",\n" + "   \"subject\": \"Message from automation\",\n"
				+ "   \"gift_count\": \"" + gift_count + "\", \n" + "   \"reset_guest_last_activity\": \"" + true
				+ "\", \n" + "   \"gift_reason\": \"Test\", \n" + "   \"exclude_from_membership_points\": " + true
				+ "\n" + "}";
		return payload;
	}

	public String API2SendOfferToUserWithExcludeFlag(String userID, String redeemable_id, String end_date) {

		String payload = "{\n" + "   \"user_id\": \"" + userID + "\",\n"
				+ "   \"message\": \"Automation test message\",\n" + "   \"subject\": \"Message from automation\",\n"
				+ "   \"redeemable_id\": \"" + redeemable_id + "\",\n" + "   \"end_date\": \"" + end_date + "\", \n"
				+ "   \"gift_reason\": \"Test\", \n" + "   \"exclude_from_membership_points\": " + true + "\n" + "}";
		return payload;
	}

	public String getPromotionalAccrualsAPIPayload(String accountID, Map<String, Object> mapOfDetails) {

		String rewardsObject = (String) mapOfDetails.getOrDefault("rewards", "");
		String couponsObject = (String) mapOfDetails.getOrDefault("coupons", "");
		String storeNumber = (String) mapOfDetails.getOrDefault("storeNumber", "1234");

		String itemID = (String) mapOfDetails.getOrDefault("itemID", "101");

		String productID_AsQCID = (String) mapOfDetails.getOrDefault("productID_AsQCID", "");

		Object orderIdValue = mapOfDetails.getOrDefault("orderId", "15944668");

		orderIdValue = mapOfDetails.getOrDefault("orderId", "1" + CreateDateTime.getTimeDateString());
		String finalOrderIdValue = null;

		String transactionNumber = CreateDateTime.getRandomNumberSixDigit() + CreateDateTime.getTimeDateString();

		if ((orderIdValue instanceof String) || (orderIdValue.getClass().getName().equals("java.lang.String"))) {
			finalOrderIdValue = "\"" + orderIdValue + "\"";
		} else if (orderIdValue instanceof Integer || orderIdValue instanceof Float || orderIdValue instanceof Double
				|| orderIdValue instanceof Boolean) {
			finalOrderIdValue = orderIdValue + "";
		}

		// true / String , integer , float , double
		Object basketID = mapOfDetails.getOrDefault("basketID", "1" + CreateDateTime.getTimeDateString());
		String finalbasketIDValue = null;

		if ((basketID instanceof String) || (basketID.getClass().getName().equals("java.lang.String"))) {
			finalbasketIDValue = "\"" + basketID + "\"";
		} else if (basketID instanceof Integer || basketID instanceof Float || basketID instanceof Double
				|| basketID instanceof Boolean) {
			finalbasketIDValue = basketID + "";
		}

		String subtotalAmt = (String) mapOfDetails.getOrDefault("subtotal", "50");

		String payload = "{\"orderId\":" + finalOrderIdValue + ",\"accountId\":\"" + accountID
				+ "\",\"source\":\"Web\",\"handoff\":\"delivery\",\"currency\":\"USD\",\"placed\":\"2026-08-29T18:00:00.000Z\",\"wanted\":\"2026-08-29T18:00:00.000Z\",\"storeNumber\":\""
				+ storeNumber + "\",\"restaurant\":\"101300\",\"brand\":\"390\",\"subtotal\":" + subtotalAmt
				+ ",\"tax\":12.25,\"tip\":9,\"delivery\":2.5,\"customFees\":null,\"discount\":10,\"total\":52.25,\"address\":{\"street\":\"123 Main St.\",\"city\":\"Promoville\",\"code\":\"53210\",\"country\":\"USA\"},\"payments\":[{\"tender\":\"credit\",\"issuer\":\"visa\",\"suffix\":\"4060\",\"amount\":52.25}],\"basket\":{\"id\":"
				+ basketID + ",\"rewards\":[" + rewardsObject + "],\"coupons\":[" + couponsObject
				+ "],\"entries\":[{\"quantity\":1,\"item\":{\"product\":\"10508\",\"label\":\"Chocolate Shake\",\"cost\":5}}],\"posEntries\":[{\"quantity\":1,\"posItem\":{\"product\":\""
				+ productID_AsQCID
				+ "\",\"categories\":[\"987418\",\"361542\"],\"label\":\"Chocolate Shake\",\"cost\":5,\"modifiers\":[{\"id\":\""
				+ itemID + "\",\"quantity\":1,\"product\":\"" + productID_AsQCID
				+ "\",\"categories\":[\"761230\"],\"label\":\"Add Whipped Cream\",\"cost\":1.15,\"modifiers\":null}]}}]}}";

//	String payload = "{\"orderId\":" + finalOrderIdValue + ",\"accountId\":\"" + accountID
//			+ "\",\"source\":\"Web\",\"handoff\":\"delivery\",\"currency\":\"USD\",\"placed\":\"2026-08-29T18:00:00.000Z\",\"wanted\":\"2026-08-29T18:00:00.000Z\",\"storeNumber\":\""
//			+ storeNumber
//			+ "\",\"restaurant\":\"101300\",\"brand\":\"390\",\"subtotal\":50,\"tax\":12.25,\"tip\":9,\"delivery\":2.5,\"customFees\":null,\"discount\":10,\"total\":52.25,\"address\":{\"street\":\"123 Main St.\",\"city\":\"Promoville\",\"code\":\"53210\",\"country\":\"USA\"},\"payments\":[{\"tender\":\"credit\",\"issuer\":\"visa\",\"suffix\":\"4060\",\"amount\":52.25}],\"basket\":{\"id\":\""
//			+ transactionNumber + "\",\"rewards\":[" + rewardsObject + "],\"coupons\":[" + couponsObject
//			+ "],\"entries\":[{\"quantity\":1,\"item\":{\"product\":\"10508\",\"label\":\"Chocolate Shake\",\"cost\":5}}],\"posEntries\":[{\"quantity\":1,\"posItem\":{\"product\":\"101\",\"categories\":[\"987418\",\"361542\"],\"modifiers\":[{\"id\":\"253941\",\"quantity\":1,\"product\":\"416509\",\"categories\":[\"761230\"],\"label\":\"Add Whipped Cream\",\"cost\":1.15,\"modifiers\":null}],\"label\":\"Chocolate Shake\",\"cost\":5}}]}}";

		return payload;

	}

	public String getPromotionalAccrualsAPIPayloadForDelete(String accountID, Map<String, Object> mapOfDetails) {

		String storeNumber = (String) mapOfDetails.getOrDefault("storeNumber", "1234");
		Object orderIdValue = mapOfDetails.getOrDefault("orderId", "15944668");
		String finalOrderIdValue = null;

		if ((orderIdValue instanceof String) || (orderIdValue.getClass().getName().equals("java.lang.String"))) {
			finalOrderIdValue = "\"" + orderIdValue + "\"";
		} else if (orderIdValue instanceof Integer || orderIdValue instanceof Float || orderIdValue instanceof Double) {
			finalOrderIdValue = orderIdValue + "";
		}

		String payload = "{\"orderId\":" + finalOrderIdValue + ",\"accountId\":\"" + accountID
				+ "\",\"brand\":\"390\",\"storeNumber\":\"" + storeNumber + "\",\"restaurant\":\"5600\"}";

		return payload;

	}

	public String generateInboundBulkPayload(List<Map<String, String>> users, String hostURL, String apiKey,
			int accountId) {
		String PAYLOAD_TYPE = "audience_membership_change_request";

		String USER_PROFILE_TEMPLATE = "{\"user_identities\":[{\"type\":\"%s\",\"encoding\":\"raw\",\"value\":\"%s\"}],"
				+ "\"device_identities\":[],"
				+ "\"audiences\":[{\"audience_id\":%s,\"audience_name\":\"%s\",\"action\":\"%s\",\"user_attributes\":[]}],"
				+ "\"partner_identities\":[{\"type\":\"%s\",\"encoding\":\"raw\",\"value\":\"%s\"}],"
				+ "\"mpid\":\"%s\"}";

		String PAYLOAD_TEMPLATE = "{\"type\":\"%s\"," + "\"id\":\"%s\",\"timestamp_ms\":%d," + "\"user_profiles\":[%s],"
				+ "\"account\":{\"account_id\":%d," + "\"account_settings\":{\"hostURL\":\"%s\",\"apiKey\":\"%s\"}},"
				+ "\"firehose_version\":\"2.6.0\"}";

		String allProfiles = users.stream()
				.map(u -> String.format(USER_PROFILE_TEMPLATE, String.valueOf(u.get("user_identities.type")),
						String.valueOf(u.get("user_identities.value")), String.valueOf(u.get("audiences.audience_id")),
						String.valueOf(u.get("audiences.audience_name")), String.valueOf(u.get("audiences.action")),
						String.valueOf(u.get("partner_identities.type")),
						String.valueOf(u.get("partner_identities.value")),
						String.valueOf(u.get("user_profile.mpid") != null ? u.get("user_profile.mpid") : "")))
				.collect(Collectors.joining(","));

		return String.format(PAYLOAD_TEMPLATE, PAYLOAD_TYPE, UUID.randomUUID(), System.currentTimeMillis(), allProfiles,
				accountId, hostURL, apiKey);
	}

	public String api2SingleScanCodeForTipTypePayload(String payment_type, String transaction_token, String client,
			String tip_type, String tip) {
		String payload = "{" + "\"client\": \"" + client + "\", " + "\"payment_type\": \"" + payment_type + "\", "
				+ "\"transaction_token\": \"" + transaction_token + "\", " + "\"tip_type\": \"" + tip_type + "\", "
				+ "\"tip\": \"" + tip + "\" " + "}";
		return payload;
	}

	public String api2SingleScanCodeForWithoutTipTypePayload(String payment_type, String transaction_token,
			String client, String tip) {
		String payload = "{" + "\"client\": \"" + client + "\", " + "\"payment_type\": \"" + payment_type + "\", "
				+ "\"transaction_token\": \"" + transaction_token + "\", " + "\"tip\": \"" + tip + "\" " + "}";
		return payload;
	}

	public String api2AddExistingGiftCardPayload(String designId, String cardNumber, String epin, String client,
			String accessToken) {

		return "{\n" + "  \"client\": \"" + client + "\",\n" + "  \"access_token\": \"" + accessToken + "\",\n"
				+ "  \"design_id\": \"" + designId + "\",\n" + "  \"card_number\": \"" + cardNumber + "\",\n"
				+ "  \"epin\": \"" + epin + "\"\n" + "}";

	}

	public String segmentCloseOrderOnlinePayload(String externalUID, String emailID, String storeNumber, String posRef,
			String oloSlug, String csID, String orderID) {
		Calendar cal = Calendar.getInstance();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		cal.add(Calendar.DATE, 0);
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		String extRef = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		String payload = "{\n" + "    \"id\": \"" + externalUID + "\",\n" + "    \"orderId\": \"" + orderID + "\",\n"
				+ "    \"externalReference\": \"EXTREF" + extRef + "\",\n" + "    \"posRef\": \"PosRef" + posRef
				+ "End\",\n" + "    \"storeNumber\": \"" + storeNumber + "\",\n" + "    \"storeUtcOffset\": 5.5,\n"
				+ "    \"timePlaced\": \"" + recieptDateTime + "\",\n" + "    \"timeWanted\": null,\n"
				+ "    \"timeReady\": \"" + recieptDateTime + "\",\n" + "    \"customer\": {\n"
				+ "        \"customerId\": \"" + csID + "\",\n" + "        \"externalReference\": \"\",\n"
				+ "        \"firstName\": \"First_61281953\",\n" + "        \"lastName\": \"Last_61281953\",\n"
				+ "        \"contactNumber\": \"61281953\",\n" + "        \"membershipNumber\": null,\n"
				+ "        \"loyaltyScheme\": null,\n" + "        \"email\": \"" + emailID + "\", \n"
				+ "        \"loginProviders\": [\n" + "            {\n" + "                \"name\": \"Punchh\",\n"
				+ "                \"slug\": \"" + oloSlug + "\"\n" + "            }\n" + "        ]\n" + "    },\n"
				+ "    \"deliveryMethod\": \"\",\n" + "    \"deliveryAddress\": null,\n" + "    \"items\": [\n"
				+ "        {\n" + "            \"productId\": 120537,\n"
				+ "            \"specialInstructions\": \"\",\n" + "            \"quantity\": 1,\n"
				+ "            \"recipientName\": \"\",\n" + "            \"customValues\": [],\n"
				+ "            \"description\": \"Banana Pecan Pancake Breakfast\",\n"
				+ "            \"sellingPrice\": -8.5900,\n" + "            \"modifiers\": [\n" + "                {\n"
				+ "                    \"modifierId\": 294881,\n"
				+ "                    \"vendorSpecificModifierId\": 171675236,\n"
				+ "                    \"modifierQuantity\": 1,\n" + "                    \"customFields\": [],\n"
				+ "                    \"description\": \"Egg Whites\",\n"
				+ "                    \"sellingPrice\": 0.0000,\n" + "                    \"modifiers\": []\n"
				+ "                },\n" + "                {\n" + "                    \"modifierId\": 294882,\n"
				+ "                    \"vendorSpecificModifierId\": 171675237,\n"
				+ "                    \"modifierQuantity\": 1,\n" + "                    \"customFields\": [],\n"
				+ "                    \"description\": \"Turkey Bacon\",\n"
				+ "                    \"sellingPrice\": -2.0000,\n" + "                    \"modifiers\": []\n"
				+ "                }\n" + "            ]\n" + "        },\n" + "        {\n"
				+ "            \"productId\": 120524,\n" + "            \"specialInstructions\": \"\",\n"
				+ "            \"quantity\": 1,\n" + "            \"recipientName\": \"\",\n"
				+ "            \"customValues\": [],\n" + "            \"description\": \"Caramel Apple Pie Crisp\",\n"
				+ "            \"sellingPrice\": 3.4900,\n" + "            \"modifiers\": []\n" + "        }\n"
				+ "    ],\n" + "    \"customFields\": [],\n" + "    \"totals\": {\n" + "        \"subTotal\": 3.00,\n"
				+ "        \"salesTax\": 1.0400,\n" + "        \"tip\": 1.0000,\n" + "        \"delivery\": 1.0000,\n"
				+ "        \"feesTotal\": 1.0000,\n" + "        \"discount\": 1.0000,\n"
				+ "        \"total\": 1.0000,\n" + "        \"customerDelivery\": 1.0000\n" + "    },\n"
				+ "    \"payments\": [],\n" + "    \"clientPlatform\": \"MobileWeb\",\n" + "    \"location\": {\n"
				+ "        \"name\": \"Denny's Demo Vendor\",\n" + "        \"logo\": \"\",\n"
				+ "        \"latitude\": 40.7052022,\n" + "        \"longitude\": -74.0131655\n" + "    },\n"
				+ "    \"orderingProvider\": null,\n" + "    \"coupon\": \"\",\n" + "    \"brandName\": \"Denny's\",\n"
				+ "    \"brandId\": \"7658e190-4000-e811-a979-0afcc1bd9d86\",\n" + "    \"customFees\": []\n" + "}";
		return payload;
	}

	public String getPromotionRedemptionsAPIPayloadForDelete(String accountID, Map<String, Object> mapOfDetails) {
		String couponsObject = (String) mapOfDetails.getOrDefault("coupons", "");
		String rewardsObject = (String) mapOfDetails.getOrDefault("rewards", "");

		String storeNumber = (String) mapOfDetails.getOrDefault("storeNumber", "1234");
		Object orderIdValue = mapOfDetails.getOrDefault("orderId", "1" + CreateDateTime.getTimeDateString());
		String finalOrderIdValue = null;

		if ((orderIdValue instanceof String) || (orderIdValue.getClass().getName().equals("java.lang.String"))) {
			finalOrderIdValue = "\"" + orderIdValue + "\"";
		} else if (orderIdValue instanceof Integer || orderIdValue instanceof Float || orderIdValue instanceof Double) {
			finalOrderIdValue = orderIdValue + "";
		}
		String payload = "{\"orderId\":" + finalOrderIdValue + ",\"accountId\":\"" + accountID + "\",\"couponCodes\":["
				+ couponsObject + "],\"rewardIds\":[\"" + rewardsObject
				+ "\"],\"brand\":\"390\",\"storeNumber\":\"7000\",\"restaurant\":\"5600\"}";

		return payload;

	}

	public String updateLocationCallsFromYext(String location_id, String store_number, String name) {

		String payload = "{\n" + "  \"location_id\": " + location_id + ",\n" + "  \"store_number\": \"" + store_number
				+ "\",\n" + "  \"location\": {\n" + "  \"name\": \"" + name + "\",\n"
				+ "    \"location_extra_attributes\": {\n" + "      \"brand\": \"Punchh\",\n"
				+ "      \"online_order_url\": \"https://example.com\",\n" + "      \"store_times\": [\n"
				+ "        {\n" + "          \"day\": \"Mon\",\n" + "          \"start_time\": \"5:30 AM\",\n"
				+ "          \"end_time\": \"7:00 PM\"\n" + "        },\n" + "        {\n"
				+ "          \"day\": \"Tue\",\n" + "          \"start_time\": \"5:30 AM\",\n"
				+ "          \"end_time\": \"8:00 PM\"\n" + "        },\n" + "        {\n"
				+ "          \"day\": \"Wed\",\n" + "          \"start_time\": \"5:30 AM\",\n"
				+ "          \"end_time\": \"8:00 PM\"\n" + "        },\n" + "        {\n"
				+ "          \"day\": \"Thu\",\n" + "          \"start_time\": \"5:30 AM\",\n"
				+ "          \"end_time\": \"8:00 PM\"\n" + "        },\n" + "        {\n"
				+ "          \"day\": \"Fri\",\n" + "          \"start_time\": \"5:30 AM\",\n"
				+ "          \"end_time\": \"8:00 PM\"\n" + "        },\n" + "        {\n"
				+ "          \"day\": \"Sat\",\n" + "          \"start_time\": \"6:30 AM\",\n"
				+ "          \"end_time\": \"8:00 PM\"\n" + "        },\n" + "        {\n"
				+ "          \"day\": \"Sun\",\n" + "          \"start_time\": \"6:30 AM\",\n"
				+ "          \"end_time\": \"6:00 PM\"\n" + "        }\n" + "      ]\n" + "    },\n"
				+ "    \"time_zone\": \"America/Los_Angeles\",\n" + "\"generate_barcodes\": true\n,"
				+ "\"enable_daily_redemption_report\": true, \n" + "\"enable_weekly_redemption_report\": true \n"
				+ "  }\n" + "}";

		return payload;
	}

	public String getPromotionalAccrualsAPIPayloadNew(String accountID, Map<String, Object> mapOfDetails,
			Map<String, Map<String, Object>> rewardsMap) {
		String rewardsObject = "";

		if (rewardsMap.size() != 0) {
			for (Entry<String, Map<String, Object>> entry : rewardsMap.entrySet()) {
				Map<String, Object> valuesMap = entry.getValue();
				String str = "{\"id\":\"" + valuesMap.get("id") + "\",\"provider\":\"" + valuesMap.get("provider")
						+ "\",\"level\":\"" + valuesMap.get("level") + "\",\"product\":\"" + valuesMap.get("product")
						+ "\",\"discount\":" + valuesMap.get("discount") + ",\"type\":\"" + valuesMap.get("type")
						+ "\"},";

				rewardsObject = rewardsObject + str;
			}

		}
		rewardsObject = rewardsObject.substring(0, rewardsObject.length() - 1);
		String couponsObject = (String) mapOfDetails.getOrDefault("coupons", "");
		String storeNumber = (String) mapOfDetails.getOrDefault("storeNumber", "1234");

		String itemID = (String) mapOfDetails.getOrDefault("itemID", "101");

		String productID_AsQCID = (String) mapOfDetails.getOrDefault("productID_AsQCID", "");

		Object orderIdValue = mapOfDetails.getOrDefault("orderId", "15944668");

		orderIdValue = mapOfDetails.getOrDefault("orderId", "1" + CreateDateTime.getTimeDateString());
		String finalOrderIdValue = null;

		String transactionNumber = CreateDateTime.getRandomNumberSixDigit() + CreateDateTime.getTimeDateString();

		if ((orderIdValue instanceof String) || (orderIdValue.getClass().getName().equals("java.lang.String"))) {
			finalOrderIdValue = "\"" + orderIdValue + "\"";
		} else if (orderIdValue instanceof Integer || orderIdValue instanceof Float || orderIdValue instanceof Double
				|| orderIdValue instanceof Boolean) {
			finalOrderIdValue = orderIdValue + "";
		}

		// true / String , integer , float , double
		Object basketID = mapOfDetails.getOrDefault("basketID", "1" + CreateDateTime.getTimeDateString());
		String finalbasketIDValue = null;

		if ((basketID instanceof String) || (basketID.getClass().getName().equals("java.lang.String"))) {
			finalbasketIDValue = "\"" + basketID + "\"";
		} else if (basketID instanceof Integer || basketID instanceof Float || basketID instanceof Double
				|| basketID instanceof Boolean) {
			finalbasketIDValue = basketID + "";
		}

		String subtotalAmt = (String) mapOfDetails.getOrDefault("subtotal", "50");

		String payload = "{\"orderId\":" + finalOrderIdValue + ",\"accountId\":\"" + accountID
				+ "\",\"source\":\"Web\",\"handoff\":\"delivery\",\"currency\":\"USD\",\"placed\":\"2026-08-29T18:00:00.000Z\",\"wanted\":\"2026-08-29T18:00:00.000Z\",\"storeNumber\":\""
				+ storeNumber + "\",\"restaurant\":\"101300\",\"brand\":\"390\",\"subtotal\":" + subtotalAmt
				+ ",\"tax\":12.25,\"tip\":9,\"delivery\":2.5,\"customFees\":null,\"discount\":10,\"total\":52.25,\"address\":{\"street\":\"123 Main St.\",\"city\":\"Promoville\",\"code\":\"53210\",\"country\":\"USA\"},\"payments\":[{\"tender\":\"credit\",\"issuer\":\"visa\",\"suffix\":\"4060\",\"amount\":52.25}],\"basket\":{\"id\":"
				+ basketID + ",\"rewards\":[" + rewardsObject + "],\"coupons\":[" + couponsObject
				+ "],\"entries\":[{\"quantity\":1,\"item\":{\"product\":\"10508\",\"label\":\"Chocolate Shake\",\"cost\":5}}],\"posEntries\":[{\"quantity\":1,\"posItem\":{\"product\":\""
				+ productID_AsQCID
				+ "\",\"categories\":[\"987418\",\"361542\"],\"label\":\"Chocolate Shake\",\"cost\":5,\"modifiers\":[{\"id\":\""
				+ itemID + "\",\"quantity\":1,\"product\":\"" + productID_AsQCID
				+ "\",\"categories\":[\"761230\"],\"label\":\"Add Whipped Cream\",\"cost\":1.15,\"modifiers\":null}]}}]}}";

//		String payload = "{\"orderId\":" + finalOrderIdValue + ",\"accountId\":\"" + accountID
//				+ "\",\"source\":\"Web\",\"handoff\":\"delivery\",\"currency\":\"USD\",\"placed\":\"2026-08-29T18:00:00.000Z\",\"wanted\":\"2026-08-29T18:00:00.000Z\",\"storeNumber\":\""
//				+ storeNumber
//				+ "\",\"restaurant\":\"101300\",\"brand\":\"390\",\"subtotal\":50,\"tax\":12.25,\"tip\":9,\"delivery\":2.5,\"customFees\":null,\"discount\":10,\"total\":52.25,\"address\":{\"street\":\"123 Main St.\",\"city\":\"Promoville\",\"code\":\"53210\",\"country\":\"USA\"},\"payments\":[{\"tender\":\"credit\",\"issuer\":\"visa\",\"suffix\":\"4060\",\"amount\":52.25}],\"basket\":{\"id\":\""
//				+ transactionNumber + "\",\"rewards\":[" + rewardsObject + "],\"coupons\":[" + couponsObject
//				+ "],\"entries\":[{\"quantity\":1,\"item\":{\"product\":\"10508\",\"label\":\"Chocolate Shake\",\"cost\":5}}],\"posEntries\":[{\"quantity\":1,\"posItem\":{\"product\":\"101\",\"categories\":[\"987418\",\"361542\"],\"modifiers\":[{\"id\":\"253941\",\"quantity\":1,\"product\":\"416509\",\"categories\":[\"761230\"],\"label\":\"Add Whipped Cream\",\"cost\":1.15,\"modifiers\":null}],\"label\":\"Chocolate Shake\",\"cost\":5}}]}}";

		return payload;
	}

	public String authOnlineSubscriptionRedemptionPayload(String authentication_token, String client, String item_id,
			String subscription_id) {
		Calendar cal = Calendar.getInstance();
		String externalUid = "a unique id " + CreateDateTime.getTimeDateString();
		String transactionNumber = CreateDateTime.getTimeDateString();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		cal.add(Calendar.DATE, -1);
		String receiptDateTime = dateFormat.format(cal.getTime()).toString();
		String payload = "{\n" + "  \"store_number\": \"58\",\n" + "  \"receipt_amount\": 5,\n"
				+ "  \"subscription_id\": \"" + subscription_id + "\",\n" + "  \"discount_type\": \"subscription\",\n"
				+ "  \"redeemed_points\": \"2\",\n" + "  \"authentication_token\": \"" + authentication_token + "\",\n"
				+ "  \"menu_items\": [\n" + "    {\n" + "      \"item_name\": \"Small Pizza\",\n"
				+ "      \"item_qty\": 1,\n" + "      \"item_amount\": 4,\n" + "      \"menu_item_type\": \"M\",\n"
				+ "      \"menu_item_id\": \"" + item_id + "\",\n" + "      \"menu_family\": \"106\",\n"
				+ "      \"menu_major_group\": \"100\",\n" + "      \"serial_number\": \"1.0\"\n" + "    },\n"
				+ "    {\n" + "      \"item_name\": \"Cheese\",\n" + "      \"item_qty\": 1,\n"
				+ "      \"item_amount\": 1,\n" + "      \"menu_item_type\": \"M\",\n"
				+ "      \"menu_item_id\": 3418,\n" + "      \"menu_family\": \"800\",\n"
				+ "      \"menu_major_group\": \"152\",\n" + "      \"serial_number\": \"1.1\"\n" + "    }\n" + "  ],\n"
				+ "  \"subtotal_amount\": 400,\n" + "  \"receipt_datetime\": \"" + receiptDateTime + "\",\n"
				+ "  \"transaction_no\": \"" + transactionNumber + "\",\n" + "  \"external_uid\": \"" + externalUid
				+ "\",\n" + "  \"client\": \"" + client + "\",\n" + "  \"process\": true\n" + "}";

		return payload;
	}

	public String challengeOptInPayload(int id) {
		String payload = "{\n" + "    \"id\": " + id + "\n" + "}";
		return payload;
	}

	public String api1PointTransferToOtherUser(String email, String amount) {
		String payload = "{\"recipient_email\": \"" + email + "\",\n" + "  \"points_to_transfer\": \"" + amount + "\"}";
		return payload;
	}
	
	public String segmentBuilderDashboardApiPayload(String segmentName, String segmentBuilder) {


		String payload = "{\n"
				+ "    \"segment_definition\": {\n"
				+ "        \"name\": \""+segmentName+"\",\n"
				+ "        \"builder_clauses\": "+segmentBuilder+"    \n"
				+ "    }\n"
				+ "}";
		return payload;
	}
	
	public String apiV1DriveThruCodePayload(String loc_id) {
		String payload = "{\"location_id\": \"" + loc_id + "\",\n" + " \"allow_payment\": true\n" + "}";
		return payload;
	}

	public String apiV2DriveThruCodePayload(String loc_id, String client) {
		String payload = "{\"client\": \"" + client + "\", \"location_id\": \"" + loc_id + "\",\n"
				+ " \"allow_payment\": true\n" + "}";
		return payload;
	}

	public String posRedemptionPayload(String primaryEmail, String secondaryEmail, String phone, String date,
			String redemptionCode, String punchhKey, String txnNumber) throws JsonProcessingException {
		String menuItems = "[{\"item_name\":\"White rice\",\"item_qty\":1,\"item_amount\":10.00,\"menu_item_type\":\"M\",\"menu_item_id\":\"101\",\"menu_family\":\"800\",\"menu_major_group\":\"152\",\"serial_number\":\"1.0\"}]";
		// Outer keys
		Map<String, Object> body = new HashMap<>();
		body.put("email", primaryEmail);
		body.put("secondary_email", secondaryEmail);
		body.put("phone_number", phone);
		body.put("subtotal_amount", 10.00);
		body.put("receipt_amount", 10.00);
		body.put("discount_type", "redemption_code");
		body.put("redemption_code", redemptionCode);
		body.put("receipt_datetime", date);
		body.put("transaction_no", txnNumber);
		body.put("punchh_key", punchhKey);
		// Serialize map and inject menu_items into final JSON
		ObjectMapper om = new ObjectMapper();
		String base = om.writeValueAsString(body);
		String payload = "{\"menu_items\":" + menuItems + "," + base.substring(1);
		return payload;
	}

	public String api2UpdateUserSecondaryEmailPayload(String client, String secondaryEmail, String phone) {
		String payload = "{\"client\": \"" + client + "\",\"user\": {\"secondary_email\": \"" + secondaryEmail
				+ "\",\"phone\": \"" + phone + "\"}}";
		return payload;
	}

	public String API2UpdateUserProfilePayload2(String client, String email, String existingPassword,
			String newPassword) {

        String payload = "{\n" + "  \"client\": \"" + client + "\",\n" + "	\"user\": {\n" + "		\"email\": \""
                + email + "\",\n" + "		\"current_password\": \""+existingPassword+"\",\n"
                + "		\"password\": \""+newPassword+"\",\n" + "		\"password_confirmation\": \""+newPassword+"\",\n"
                + "		\"gender\": \"Female\"\n" + "	}\n" + "}";
     
		return payload;

	}

	public Map<String, Object> authApiUpdateUserPayload(String email, String phone, String firstName, String lastName,
			String currentPassword, String newPassword) {
		Map<String, Object> payload = new HashMap<>();
		putIfNotEmpty(payload, "email", email);
		putIfNotEmpty(payload, "phone", phone);
		putIfNotEmpty(payload, "first_name", firstName);
		putIfNotEmpty(payload, "last_name", lastName);
		putIfNotEmpty(payload, "current_password", currentPassword);
		putIfNotEmpty(payload, "password", newPassword);
		putIfNotEmpty(payload, "last_name", lastName);
		return payload;
	}

	public String API2UpdateAsyncUserProfile(String client, String email, String currentPassword, String newPassword) {

		String payload = "{\n" + "  \"client\": \"" + client + "\",\n" + "	\"user\": {\n" + "		\"email\": \""
				+ email + "\",\n" + "		\"current_password\": \"" + currentPassword + "\",\n"
				+ "		\"password\": \"" + newPassword + "\",\n" + "		\"password_confirmation\": \"" + newPassword
				+ "\",\n" + "		\"gender\": \"Female\"\n" + "	}\n" + "}";

		return payload;

	}

	public Map<String, Object> Api2UpdateUserPasswordPayload(String client, String email, String phone,
			String firstName, String lastName, String currentPassword, String newPassword) {
		Map<String, Object> payload = new HashMap<>();
		payload.put("client", client);
		Map<String, Object> user = new HashMap<>();
		putIfNotEmpty(user, "email", email);
		putIfNotEmpty(user, "phone", phone);
		putIfNotEmpty(user, "first_name", firstName);
		putIfNotEmpty(user, "last_name", lastName);
		putIfNotEmpty(user, "current_password", currentPassword);
		putIfNotEmpty(user, "password", newPassword);
		putIfNotEmpty(user, "password_confirmation", newPassword);
		payload.put("user", user);
		return payload;
	}

	public String API2UpdateSecureAsyncUserProfile(String client, String email, String currentPassword,
			String newPassword) {

		String payload = "{\n" + "	\"user\": {\n" + "		\"email\": \"" + email + "\",\n"
				+ "		\"current_password\": \"" + currentPassword + "\",\n" + "		\"password\": \"" + newPassword
				+ "\",\n" + "		\"password_confirmation\": \"" + newPassword + "\",\n"
				+ "		\"gender\": \"Female\"\n" + "	}\n" + "}";

		return payload;

	}

	public Map<String, Object> authApiChangePasswordAdvanceAuthWithResetPasswordTokenPayload(String client,
			String password, String resetPasswordToken) {
		Map<String, Object> payloadMap = new HashMap<>();
		Map<String, String> userMap = new HashMap<>();
		userMap.put("password", password);
		userMap.put("password_confirmation", password);
		payloadMap.put("user", userMap);
		payloadMap.put("client", client);
		payloadMap.put("reset_password_token", resetPasswordToken);
		return payloadMap;
	}

	public String updateUserPasswordDashboardApi(String user_id, String email, String password,
			String currentPassword) {

		String payload = "{\n" + "    \"id\": \"" + user_id + "\",\n" + "    \"email\": \"" + email + "\",\n"
				+ "    \"user\": {\n" + "    \n" + "        \"current_password\": \"" + currentPassword + "\",\n"
				+ "        \"password\": \"" + password + "\",\n" + "        \"password_confirmation\": \"" + password
				+ "\"\n" + "    }\n" + "}";

		return payload;

	}

	public String userLookupPosAPIPayloadWithNfc(String lookUpField, String lookUpValue, String externalUid) {
		String payLoad = "{\"lookup_field\":\"" + lookUpField + "\",\"lookup_value\":\"" + lookUpValue
				+ "\",\"external_uid\":\"" + externalUid + "\"}";
		return payLoad;
	}

    public String segmentCreationUsingBuilderClausePayload(String segmentName, String builder_clauses) {
		String payload = "{\n" + "    \"segment_definition\": {\n" + "        \"name\": \"" + segmentName + "\",\n"
				+ "        \"builder_clauses\": " + builder_clauses + "\n" + "    }\n" + "}";

		return payload;
	}
	public String API2RedemptionWithBankedCurrencyDynamic(
            String client,
            String locationId,
            double bankedCurrency,
            double latitude,
            double longitude,
            int gpsAccuracy
    ) 
	{

        return "{\n" +
                "  \"client\": \"" + client + "\",\n" +
                "  \"banked_currency\": " + bankedCurrency + ",\n" +
                "  \"location_id\": \"" + locationId + "\",\n" +
                "  \"latitude\": " + latitude + ",\n" +
                "  \"longitude\": " + longitude + ",\n" +
                "  \"gps_accuracy\": " + gpsAccuracy + "\n" +
                "}";
    }
}