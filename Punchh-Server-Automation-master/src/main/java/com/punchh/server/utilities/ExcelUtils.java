package com.punchh.server.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.ITestResult;

import com.opencsv.CSVReader;

import Support.ConfigurationClass;

public class ExcelUtils {
	static Logger logger = LogManager.getLogger(ExcelUtils.class);

	public static Map<String, String> executionStatusMap = new HashMap<String, String>();
	public static Map<String, String> failureReasonMap = new HashMap<String, String>();

	public static void updateLocalRegressionExecutionResultInSheet(ITestContext context) {
		Workbook workbook;
		try {
			String filePath = System.getProperty("user.dir")
					+ "/resources/localRegressionResult/localRegressionStatus.xlsx";
			workbook = WorkbookFactory.create(new FileInputStream(filePath));

			String suite = "localRegression";//ConfigurationClass.suite;
			switch (suite) {
			case "localRegression":
				Set<ITestResult> passTestCaseSet = context.getPassedTests().getAllResults();
				for (ITestResult result : passTestCaseSet) {
					if (result.SUCCESS == result.getStatus()) {
						executionStatusMap.put(result.getName(), "PASS");
					} else {
					}

				}
				Set<ITestResult> failedTestCaseSet = context.getFailedTests().getAllResults();
				for (ITestResult result : failedTestCaseSet) {
					if (result.FAILURE == result.getStatus()) {
						executionStatusMap.put(result.getName(), "FAIL");
						failureReasonMap.put(result.getName(), result.getThrowable().getMessage());
						System.out.println(result.getThrowable().getMessage());
					} else {
					}

				}

				try {
					workbook = WorkbookFactory.create(new FileInputStream(filePath));

					CreateDateTime createDateTime = new CreateDateTime();
					String sheetName = CreateDateTime.getFutureDateTimeUTC(0);
					Sheet sheet = workbook.getSheet(sheetName);
					Row row = null;
					if (sheet == null) {
						sheet = workbook.createSheet(sheetName);
						int rowNum = 0;
						row = sheet.createRow(rowNum);
						row.createCell(0).setCellValue("Test case name");
						row.createCell(1).setCellValue("First Run Status");
						row.createCell(2).setCellValue("Failure Reason");
						row.createCell(3).setCellValue("Second Run Status ");
						row.createCell(4).setCellValue("Failure Reason");
						for (Map.Entry<String, String> entry : executionStatusMap.entrySet()) {
							rowNum++;
							String key = entry.getKey();
							String keyValuePassFail = entry.getValue();
							row = sheet.createRow(rowNum);
							row.createCell(0).setCellValue(key);
							row.createCell(1).setCellValue(keyValuePassFail);
							if (keyValuePassFail.equalsIgnoreCase("FAIL")) {
								String failedReasonForKey = failureReasonMap.get(key);
								row.createCell(2).setCellValue(failedReasonForKey);
								row.createCell(3).setCellValue("");
								row.createCell(4).setCellValue("");
							}
						}

					} else {
						int totalRows = sheet.getLastRowNum();
						for (Map.Entry<String, String> entry : executionStatusMap.entrySet()) {
							String tcName = entry.getKey();
							String tcExeStatus = entry.getValue();
							for (int i = 0; i <= totalRows; i++) {
								String sheetTestCaseName = sheet.getRow(i).getCell(0).getStringCellValue();
								String tcFirstExeStatus = sheet.getRow(i).getCell(1).getStringCellValue();
								if ((tcName.equalsIgnoreCase(sheetTestCaseName))
										&& (!tcFirstExeStatus.equalsIgnoreCase(""))) {
									if (tcExeStatus.equalsIgnoreCase("FAIL")) {
										sheet.getRow(i).createCell(3).setCellValue(tcExeStatus);
										sheet.getRow(i).createCell(4).setCellValue(failureReasonMap.get(tcName));

									} else {
										sheet.getRow(i).createCell(3).setCellValue(tcExeStatus);
										sheet.getRow(i).createCell(4).setCellValue("");
									}
								}
							}

						} // end of map looping

					}
					FileOutputStream fileOut = new FileOutputStream(filePath);
					workbook.write(fileOut);

				} catch (EncryptedDocumentException e) {
					e.printStackTrace();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				break;

			default:
				break;
			}

		} catch (Exception ne) {
			// TODO: handle exception
		}
	}

	public static List<Map<String, String>> readCSV(File filePath) {
		List<Map<String, String>> records = new ArrayList<>();
		try (FileReader reader = new FileReader(filePath);
				CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
			for (CSVRecord csvRecord : csvParser) {
				records.add(csvRecord.toMap());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return records;
	}

	public static void updateColumnValues(List<Map<String, String>> records, String columnName,
			List<String> userNameList) {
		int counter = 0;
		if (records.size() == userNameList.size()) {
			for (Map<String, String> record : records) {
				record.put(columnName, userNameList.get(counter));
				counter++;
			}
		}
	}

	public static void writeCSV(File filePath, List<Map<String, String>> records) {
		if (records.isEmpty()) {
			return;
		}

		try (FileWriter writer = new FileWriter(filePath);
				CSVPrinter csvPrinter = new CSVPrinter(writer,
						CSVFormat.DEFAULT.withHeader(records.get(0).keySet().toArray(new String[0])))) {
			for (Map<String, String> record : records) {
				csvPrinter.printRecord(record.values());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static List<String> getCsvFileNameFromDir(String dirPath) {

		// Path to the directory
		String directoryPath = dirPath;

		List<String> listOfCsvFiles = new ArrayList<String>();

		// Create a File object for the directory
		File directory = new File(directoryPath);

		// Define a FilenameFilter to filter .csv files
		FilenameFilter csvFilter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".csv");
			}
		};

		// List all .csv files in the directory
		File[] csvFiles = directory.listFiles(csvFilter);

		// Check if the directory is valid and listFiles is not null
		if (csvFiles != null) {
			// Print the names of the .csv files
			for (File csvFile : csvFiles) {
				listOfCsvFiles.add(csvFile.getName());
			}
		} else {
		}
		return listOfCsvFiles;

	}

	public static Map<String, Map<String, String>> readDataFromDataBaseForDP(String env, String primaryKey,
			String sqlQuery) throws Exception {

		logger.info("== Data export validation test ==");

		SingletonDBUtils singletonDBObj = new SingletonDBUtils();
		ResultSet resultSet = DBUtils.getResultSet(env, sqlQuery);

		// 4. Get the ResultSetMetaData
		ResultSetMetaData metaData = resultSet.getMetaData();

		// 6. Print the column names
		Map<String, Map<String, String>> parentMap_DB = new LinkedHashMap<String, Map<String, String>>();

		// 7. Iterate through the ResultSet and print column values
		while (resultSet.next()) {
			Map<String, String> colValueMap_DB = new LinkedHashMap<String, String>();
			String userIDKey = "";
			for (int i = 1; i <= metaData.getColumnCount(); i++) {
				String columnName = metaData.getColumnName(i);
				String columnValues = resultSet.getString(i).toString();
				colValueMap_DB.put(columnName, columnValues);

				if (columnName.equalsIgnoreCase(primaryKey)) {
					userIDKey = resultSet.getString(i);

				}

			}
			parentMap_DB.put(userIDKey, colValueMap_DB);
		}

		return parentMap_DB;

	}

	public static Map<String, Map<String, String>> readCSVFile(String primaryKey, String fileName,
			int headerRowNumber) {

		Map<String, Map<String, String>> parentMapForCSV = new LinkedHashMap<String, Map<String, String>>();

		String csvFile = System.getProperty("user.dir") + "/resources/Testdata/" + fileName;

		try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
			List<String[]> rowsList = reader.readAll();

			for (int i = headerRowNumber; i < rowsList.size(); i++) {
				Map<String, String> subDetailsMap = new LinkedHashMap<>();
				int counter = 0;
				int primaryKeyColCounter = 0;
				String[] rowArrHeader = rowsList.get(headerRowNumber - 1);
				String[] colValuesArr = rowsList.get(i);

				for (String strVal : rowArrHeader) {
					strVal = strVal.replace(" ", "_").toLowerCase();
					subDetailsMap.put(strVal, colValuesArr[counter]);
					counter++;
				}
				for (String strVal : rowArrHeader) {
					if (strVal.equalsIgnoreCase(primaryKey)) {
						strVal = strVal.replace(" ", "_").toLowerCase();
						subDetailsMap.put(strVal, colValuesArr[primaryKeyColCounter]);
						parentMapForCSV.put(colValuesArr[primaryKeyColCounter], subDetailsMap);
					} else {
						primaryKeyColCounter++;
					}
				}

			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return parentMapForCSV;
	}

	public static List<String> validateExportDataWithDBData(Map<String, Map<String, String>> parentMapFromCSVFile,
			Map<String, Map<String, String>> parentMapFromDataBase) {
		List<String> failedColumnNameList = new ArrayList<String>();
		if (parentMapFromDataBase.size() == parentMapFromCSVFile.size()) {
			for (Entry<String, Map<String, String>> pMap : parentMapFromCSVFile.entrySet()) {
				String pKeyCSV = pMap.getKey();

				logger.info(
						"**********Validation START for Exported TO Databse  Data for the Primary KEY --- " + pKeyCSV);
				TestListeners.extentTest.get().info(
						"**********Validation START for Exported TO Databse  Data for the Primary KEY --- " + pKeyCSV);

				Map<String, String> childMapCSV_New = pMap.getValue();

				Map<String, String> childDataBaseDetailMap = parentMapFromDataBase.get(pKeyCSV);

				for (String childCSVMap_Key : childMapCSV_New.keySet()) {

					String childCSVKey_value = childMapCSV_New.get(childCSVMap_Key);
					String childDBKey_value = childDataBaseDetailMap.get(childCSVMap_Key);

					if (childDataBaseDetailMap.containsKey(childCSVMap_Key)) {
						if (childCSVKey_value.equalsIgnoreCase(childDBKey_value)) {

							logger.info(childCSVMap_Key + " key value =  " + childCSVKey_value
									+ "   is MATCHED with Database value which is " + childDBKey_value);
							TestListeners.extentTest.get().pass(childCSVMap_Key + " key value =  " + childCSVKey_value
									+ "   is MATCHED with Database value which is " + childDBKey_value);

						} else {

							logger.info(childCSVMap_Key + " key value =  " + childCSVKey_value
									+ "   is NOT MATCHED with Database value which is " + childDBKey_value);
							TestListeners.extentTest.get().fail(childCSVMap_Key + " key value =  " + childCSVKey_value
									+ "   is NOT MATCHED with Database value which is " + childDBKey_value);

						}

					} else {

						logger.info(childCSVMap_Key
								+ "  column is present in CSV but not the part of DB query , Please check it manually ");
						TestListeners.extentTest.get().fail(childCSVMap_Key
								+ "  column is present in CSV but not the part of DB query , Please check it manually ");
						failedColumnNameList.add(childCSVMap_Key);
					}
				}
				logger.info(
						"**********Validation END for Exported TO Databse  Data for the Primary KEY --- " + pKeyCSV);
				TestListeners.extentTest.get().info(
						"**********Validation END for Exported TO Databse  Data for the Primary KEY --- " + pKeyCSV);

			}

		}
		return failedColumnNameList;
	}

	public static List<String> validateDatabaseDataWithExportData(
			Map<String, Map<String, String>> parentMapFromDataBase,
			Map<String, Map<String, String>> parentMapFromCSVFile) {

		List<String> failedColumnNameList = new ArrayList<String>();

		if (parentMapFromDataBase.size() == parentMapFromCSVFile.size()) {
			for (Entry<String, Map<String, String>> pMap : parentMapFromDataBase.entrySet()) {

				String pKeyDB = pMap.getKey();
				logger.info(
						"**********Validation START for Databse To Exported Data for the Primary KEY --- " + pKeyDB);
				TestListeners.extentTest.get().info(
						"**********Validation START for Databse To Exported Data for the Primary KEY --- " + pKeyDB);

				Map<String, String> childMapDB = pMap.getValue();

				Map<String, String> childCsvMap = parentMapFromCSVFile.get(pKeyDB);

				for (String childDBMap_Key : childMapDB.keySet()) {

					String childDBKey_value = childMapDB.get(childDBMap_Key);
					String childCsvKey_value = childCsvMap.get(childDBMap_Key);
					if (childCsvMap.containsKey(childDBMap_Key)) {

						if (childDBKey_value.equalsIgnoreCase(childCsvKey_value)) {

							logger.info(childDBMap_Key + " Database key value =  " + childDBKey_value
									+ "   is MATCHED with csv value which is " + childCsvKey_value);
							TestListeners.extentTest.get().pass(childDBMap_Key + " Database key value =  "
									+ childDBKey_value + "   is MATCHED with csv value which is " + childCsvKey_value);
						} else {
							logger.info(childDBMap_Key + " Database key value =  " + childDBKey_value
									+ "   is NOT MATCHED with csv value which is " + childCsvKey_value);
							TestListeners.extentTest.get()
									.fail(childDBMap_Key + " Database key value =  " + childDBKey_value
											+ "   is NOT MATCHED with csv value which is " + childCsvKey_value);

						}

					} else {
						failedColumnNameList.add(childDBMap_Key);
						logger.info(childDBMap_Key
								+ " Column is not present in csv data file , but we have selected it from UI");
						TestListeners.extentTest.get().fail(childDBMap_Key
								+ " Column is not present in csv data file , but we have selected it from UI");

					}
				}
				logger.info("**********Validation END for Databse To Exported Data for the Primary KEY --- " + pKeyDB);
				TestListeners.extentTest.get().info(
						"**********Validation END for Databse To Exported Data for the Primary KEY --- " + pKeyDB);
			}

		}

		return failedColumnNameList;
	}

	public static void getDataTypeOfVaue(String expectedValue, String actualValue) {

		String expType = getDataTypeOfValue(expectedValue);
		String actualType = getDataTypeOfValue(actualValue);

		if ((expType.equalsIgnoreCase("String")) && ((actualType.equalsIgnoreCase("String")))) {

			String strExpVal = expectedValue.toString();
			String strActualVal = actualValue.toString();
			Assert.assertEquals(strActualVal, strExpVal,
					actualValue + " actual value is not matched with expected value :" + expectedValue);

			logger.info(actualValue + " actual value is matched with expected value :" + expectedValue);
			TestListeners.extentTest.get()
					.info(actualValue + " actual value is matched with expected value :" + expectedValue);

		} else if ((expType.equalsIgnoreCase("Integer")) && ((actualType.equalsIgnoreCase("Integer")))) {
			int intExpVal = Integer.parseInt(expectedValue);
			int intActualVal = Integer.parseInt(actualValue);
			if (intActualVal == intExpVal) {
				logger.info(actualValue + " actual value is matched with expected value :" + expectedValue);
				TestListeners.extentTest.get()
						.info(actualValue + " actual value is matched with expected value :" + expectedValue);
			} else {
				logger.info(actualValue + " actual value is not matched with expected value :" + expectedValue);
				TestListeners.extentTest.get()
						.fail(actualValue + " actual value is not matched with expected value :" + expectedValue);
			}

		} else if ((expType.equalsIgnoreCase("Double")) && ((actualType.equalsIgnoreCase("Double")))) {
			double intExpVal = Double.parseDouble(expectedValue);
			double intActualVal = Double.parseDouble(actualValue);
			if (intActualVal == intExpVal) {
				logger.info(actualValue + " actual value is matched with expected value :" + expectedValue);
				TestListeners.extentTest.get()
						.info(actualValue + " actual value is matched with expected value :" + expectedValue);
			} else {
				logger.info(actualValue + " actual value is not matched with expected value :" + expectedValue);
				TestListeners.extentTest.get()
						.fail(actualValue + " actual value is not matched with expected value :" + expectedValue);
			}
		} else if ((expType.equalsIgnoreCase("Long")) && ((actualType.equalsIgnoreCase("Long")))) {
			long intExpVal = Long.parseLong(expectedValue);
			long intActualVal = Long.parseLong(actualValue);
			if (intActualVal == intExpVal) {
				logger.info(actualValue + " actual value is matched with expected value :" + expectedValue);
				TestListeners.extentTest.get()
						.info(actualValue + " actual value is matched with expected value :" + expectedValue);
			} else {
				logger.info(actualValue + " actual value is matched with expected value :" + expectedValue);
				TestListeners.extentTest.get()
						.fail(actualValue + " actual value is not matched with expected value :" + expectedValue);
			}
		} else {
			logger.info(actualValue + " actual value data type not matched with expected value data type --"
					+ expectedValue);
			TestListeners.extentTest.get().fail(actualValue
					+ " actual value data type not matched with expected value data type --" + expectedValue);
		}
	}

	public static String isInteger(String value) {
		try {
			Integer.parseInt(value);
			return "Integer";
		} catch (NumberFormatException e) {
			return "";
		}
	}

	public static String isLong(String value) {
		try {
			Long.parseLong(value);
			return "Long";
		} catch (NumberFormatException e) {
			return "";
		}
	}

	public static String isFloat(String value) {
		try {

			Float.parseFloat(value);
			return "Float";
		} catch (NumberFormatException e) {
			return "";
		}
	}

	public static String isDouble(String value) {
		try {

			Double.parseDouble(value);
			return "Double";
		} catch (NumberFormatException e) {
			return "";
		}
	}

	public static String getDataTypeOfValue(String value) {

		if (isInteger(value).equalsIgnoreCase("Integer")) {
			return "Integer";
		} else if (isLong(value).equalsIgnoreCase("Long")) {
			return "Long";
		} else if (isDouble(value).equalsIgnoreCase("Double")) {
			return "Double";
		} else {
			return "String";
		}

	}

	public static void updateColumnValuesOfCsvFile(List<Map<String, String>> records, String columnName, List<String> userNameList) {
		// Clear old rows
		records.clear();
		for (String userId : userNameList) {
			Map<String, String> newRow = new LinkedHashMap<>();
			newRow.put(columnName, userId);           
			newRow.put("reason", "Delete-TestData");  
			records.add(newRow);
		}
	}

}
