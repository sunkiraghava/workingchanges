package com.punchh.server.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.testng.Assert;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ReadData {

	public Map<String, String> readTestData = new ConcurrentHashMap<>();
	public Map<String, String> writeTestData = new ConcurrentHashMap<>();
	private static Logger logger = LogManager.getLogger(ReadData.class);

	public ReadData() {

	}

	/**
	 * This method returns the path of json file as per the execution environment
	 *
	 * @param env= json file name
	 */

	/*public String getJsonFilePath(String env) {
		// String filepath = "src/test/resources/Testdata/testdata_" + env + ".json";
		String filepath = null;
		env = env.toLowerCase();
		if (env.contains("local")) {
			filepath = System.getProperty("user.dir") + "/resources/TestdataLocal/testdata_" + env + ".json";
			System.out.println("Test data File Path is :  " + filepath);
		} else {
			//filepath = System.getProperty("user.dir") + "/resources/Testdata/" + env + "/testdata_" + env + ".json";
			filepath = System.getProperty("user.dir") + "/resources/Testdata/testdata_" + env + ".json";
			System.out.println("Test data File Path is :  " + filepath);
		}
		return filepath;
	}*/
	// To read data from test data json file based on run and env
	
	public String getJsonFilePath(String run, String env) {
		
		String filepath = null;
		run = run.toLowerCase();
		env = env.toLowerCase();
		String ruTypeenv = run+env;
		if (env.contains("local")) {
			filepath = System.getProperty("user.dir") + "/resources/TestdataLocal/" + env + "/testdata_" + ruTypeenv + ".json";
			System.out.println("Test data File Path is :  " + filepath);
		} else {
			filepath = System.getProperty("user.dir") + "/resources/Testdata/" + env + "/testdata_" + ruTypeenv + ".json";
			System.out.println("Test data File Path is :  " + filepath);
		}
		return filepath;
	}
	
	// overloaded method to read data from secrets json file based on run, env and secrets
	public String getJsonFilePath(String run, String env, String secrets) {

		String filepath = null;
		run = run.toLowerCase();
		env = env.toLowerCase();
		secrets = secrets.toLowerCase();
		String ruTypeenv = run + env + secrets.trim();
		if (env.contains("local")) {
			filepath = System.getProperty("user.dir") + "/resources/TestdataLocal/" + env + "/testdata_" + ruTypeenv
					+ ".json";
			System.out.println("Test data File Path is :  " + filepath);
		} else {
			filepath = System.getProperty("user.dir") + "/resources/Testdata/" + env + "/testdata_" + ruTypeenv
					+ ".json";
			System.out.println("Test data File Path is :  " + filepath);
		}
		return filepath;
	}

	/**
	 * This method you can edit a particular field for given json object or create a
	 * new Filed in given json object
	 *
	 * @param jsonFilePath= json file path
	 * @param scenario      (Optional) = name of the scenario in which you wan to
	 *                      write the data,if not given it will take first object
	 *                      from json file @author=
	 */
	public synchronized void EditOrAddNewGivenFieldForGivenScenarioFromJson(String jsonFilePath, String... scenario) {
		try {
			byte[] jsonData = Files.readAllBytes(Paths.get(jsonFilePath));
			ObjectMapper objectMapper = new ObjectMapper();
			ObjectWriter writer = objectMapper.writer(new DefaultPrettyPrinter());
			// create JsonNode
			JsonNode rootNode = objectMapper.readTree(jsonData);
			for (String key : writeTestData.keySet()) {
				// Here we will check if the particular scenario name is given
				if (scenario.length > 0) {
					((ObjectNode) rootNode.get(scenario[0])).put(key, writeTestData.get(key));
				} else {
					((ObjectNode) rootNode).put(key, writeTestData.get(key));
				}
			}
			writer.writeValue(new File(jsonFilePath), rootNode);
			// clear the data from hash table after writing
			writeTestData.clear();
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("Fail to write the data");
		}
	}

	/**
	 * This method will Read the given Json file and return the map with key value
	 * pair
	 *
	 * @param jsonFilePath =json file path
	 * @param scenarioName = name of the scenario
	 * @return = map with test data key value for given json object
	 *
	 */

	public synchronized void ReadDataFromJsonFile(String jsonFilePath, String... scenarioName) {

		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			Map<?, ?> outerMap = mapper.readValue(new FileInputStream(jsonFilePath), Map.class);
			if (scenarioName.length > 0) {
				Map<String, String> innerMap = (Map<String, String>) outerMap.get(scenarioName[0]);
				readTestData = innerMap;
			} else {
				readTestData = (Map<String, String>) outerMap;
			}
			if (readTestData == null) {
				throw new Exception(
						"Fail to read test data, Please verify filename and test case name in test data file");
			}
		} catch (Exception ex) {
			logger.error("Fail to read test data, Please verify filename and test case name in test data file");
			ex.printStackTrace();
		}
	}

	/**
	 * This method returns the test data for the key provided from the hash table
	 *
	 * @param Key= key associated with data that we want
	 */
	/*
	 * public static synchronized String getData(String Key) { if
	 * (!readTestData.isEmpty() && readTestData.containsKey(Key)) { return
	 * readTestData.get(Key); } else {
	 * Assert.fail("Their is no data present for this key"); return null; } }
	 */

	/**
	 * This method writes the data provided in temp map to static writeTestData
	 * after verifying that temp map is not empty and return true/false as per the
	 * result
	 *
	 * @param data= temp map
	 */
	public Boolean AddTestDataToWriteInJSON(Map data) {
		if (!data.isEmpty()) {
			writeTestData = data;
			return true;
		} else {
			System.out.println("No data to add");
			return false;
		}
	}

	/**
	 * This method writes the data provided in key,value to static writeTestData
	 * after verifying that temp map is not empty and return true/false as per the
	 * result
	 *
	 * @param key=   test data key to add in json file
	 * @param value= test data key to add in json file
	 */
	public Boolean AddTestDataToWriteInJSON(String key, String value) {
		if (!(key.isEmpty() && value.isEmpty())) {
			writeTestData.put(key, value);
			return true;
		} else {
			System.out.println("Please enter non empty data to put into json");
			return false;
		}
	}

	public synchronized void ReadDataFromJsonFileForClientSecretKey(String jsonFilePath, String... slugName) {

		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			Map<?, ?> outerMap = mapper.readValue(new FileInputStream(jsonFilePath), Map.class);
			if (slugName.length > 0) {
				Map<String, String> innerMap = (Map<String, String>) outerMap.get(slugName[0]);
				readTestData = innerMap;
			} else {
				readTestData = (Map<String, String>) outerMap;
			}
		} catch (Exception ex) {
			System.out.println("Fail to read test data, Please verify filename and test case name properly added");
			ex.printStackTrace();
		}
		if (readTestData == null) {
			Assert.fail("Fail to read test data, Please verify filename and test case name in test data file");
		}
	}

	public JSONObject readApplePass_JsonFIle(String jsonFilePath)
			throws FileNotFoundException, IOException, ParseException {
		JSONParser parser = new JSONParser();
		Object obj = parser.parse(new FileReader(jsonFilePath));
		JSONObject jsonObject = (JSONObject) obj;
		return jsonObject;

	}

	public static synchronized Map<?, ?> getMenuAndSubMenuDetails(String jsonFilePath) {
		Map<?, ?> outerMap = null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			outerMap = mapper.readValue(new FileInputStream(jsonFilePath), Map.class);
		} catch (Exception ex) {
			logger.error("Fail to read the data, Please verify filename and test case id");
			logger.error(ex.getMessage());
		}
		return outerMap;
	}
}