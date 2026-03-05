package com.punchh.server.utilities;

import com.mongodb.client.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.testng.Assert;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MongoDBUtils {

	static Logger logger = LogManager.getLogger(MongoDBUtils.class);

	private static Connection dbConnection = null;
	public static Properties prop;
	public static Utilities utils;
	private Statement stmt = null;
	public static String host, port, userName, password = null, dbName;

	private static MongoClient mongoClient;
	private static MongoDatabase database;

	public MongoDBUtils() {
		prop = Utilities.loadPropertiesFile("dbConfig.properties");
		utils = new Utilities();
	}

	public static MongoDatabase getDatabaseConnection(String envName, String serviceName) {
		try {
			BrowserUtilities brw = new BrowserUtilities();
			String runType =   brw.getRunType();
			prop = Utilities.loadPropertiesFile("dbConfig.properties");
			if (prop == null) {
				throw new NullPointerException("Properties file could not be loaded.");
			}
			utils = new Utilities();
			String instance = Utilities.getInstance(envName);
			if(envName.equalsIgnoreCase("pp") || envName.equalsIgnoreCase("qa")) {
				instance = instance + "." + runType;
			}
			
			
			host = prop.getProperty(instance + ".MongoHOST");
			port = prop.getProperty(instance + ".MongoPort");

			String userProp = prop.getProperty(instance + ".MongoUsername");
			String passProp = prop.getProperty(instance + ".MongoPassword");

			if (userProp == null || passProp == null) {
				throw new NullPointerException("MongoDB credentials missing in properties.");
			}

			userName = utils.decrypt(userProp);
			password = utils.decrypt(passProp);
			dbName = databaseName(instance, serviceName);

			if (host == null || port == null || userName == null || password == null || dbName == null) {
				throw new NullPointerException("Required MongoDB config values are missing.");
			}

			if (mongoClient == null) {
				String uri = String.format("mongodb://%s:%s@%s:%s/%s", userName, password, host, port, dbName);
				mongoClient = MongoClients.create(uri);
				database = mongoClient.getDatabase(dbName);
				logger.info("MongoDB connection established to database: " + dbName);
			}
			return database;

		} catch (NullPointerException ne) {
			logger.error(envName + " DB details not found in DB config file: " + ne.getMessage());
			TestListeners.extentTest.get().fail(envName + " env DB details not found in DB config file ");
			Assert.fail(envName + " env DB details not found in DB config file ");
		} catch (Exception e) {
			logger.error("Error connecting to MongoDB: ", e);
			Assert.fail("Error connecting to MongoDB: " + e.getMessage());
		}

		return null;
	}

	public static String databaseName(String env, String serviceName) {
	    logger.info("Resolving database name for environment: " + env + ", service: " + serviceName);
	    
	    if (prop == null) {
	        logger.error("Properties not initialized when resolving database name.");
	        return null;
	    }
	    
	    String dataName = null;
	    
	    switch (env) {
	    case "pp":
	    case "qa":
	        if (serviceName != null) {
	            if (serviceName.equalsIgnoreCase("ef")) {
	                dataName = prop.getProperty(env + ".EventDBName");
	                return dataName;
	            } else if (serviceName.equalsIgnoreCase("inbound")) {
	                dataName = prop.getProperty(env + ".InboundDBName");
	                return dataName;
	            }
	        }
	        // For pp/qa with other service names, use CommonDBName
	        dataName = prop.getProperty(env + ".CommonDBName");
	        return dataName;
	    default:
	        dataName = prop.getProperty(env + ".CommonDBName");
	        return dataName;
	    }
	}

	public static Document getSingleDocument(String envName, String serviceName, String collectionName, Bson filter,
			Bson sort, int limit) {
		logger.info("Fetching single document from collection: " + collectionName);
		logger.debug("Filter: " + filter + ", Sort: " + sort + ", Limit: " + limit);

		MongoDatabase db = getDatabaseConnection(envName, serviceName);
		if (db == null) {
			logger.error("Database connection is null. Returning null.");
			return null;
		}

		MongoCollection<Document> collection = db.getCollection(collectionName);
		if (collection == null) {
			logger.error("Collection " + collectionName + " is null. Returning null.");
			return null;
		}

		FindIterable<Document> findIterable = (filter != null) ? collection.find(filter) : collection.find();

		if (sort != null) {
			findIterable = findIterable.sort(sort);
		}
		if (limit > 0) {
			findIterable = findIterable.limit(limit);
		}

		Document result = findIterable.first(); // Get the first document that matches the filter

		if (result != null) {
			logger.info("Single document found in collection: " + collectionName);
		} else {
			logger.info("No matching document found in collection: " + collectionName);
		}

		logger.debug("Fetched document: " + result);
		return result;
	}

	public static List<Document> getDocuments(String envName, String serviceName, String collectionName, Bson filter) {
		logger.info("Fetching documents from collection: " + collectionName);
		logger.debug("Using filter: " + filter);

		MongoDatabase db = getDatabaseConnection(envName, serviceName);
		if (db == null) {
			logger.error("Database connection is null. Returning empty list.");
			return new ArrayList<>();
		}

		MongoCollection<Document> collection = db.getCollection(collectionName);
		if (collection == null) {
			logger.error("Collection " + collectionName + " is null. Returning empty list.");
			return new ArrayList<>();
		}

		List<Document> documents = collection.find(filter).into(new ArrayList<>());

		logger.info("Retrieved " + documents.size() + " documents from collection: " + collectionName);
		return documents;
	}

	public static long deleteDocuments(String envName, String serviceName, String collectionName, Bson filter) {
		logger.info("Deleting documents from collection: " + collectionName);
		logger.debug("Delete filter: " + filter);

		MongoDatabase db = getDatabaseConnection(envName, serviceName);
		if (db == null) {
			logger.error("Database connection is null. Returning 0.");
			return 0;
		}

		MongoCollection<Document> collection = db.getCollection(collectionName);
		if (collection == null) {
			logger.error("Collection " + collectionName + " is null. Returning 0.");
			return 0;
		}

		long deletedCount = collection.deleteMany(filter).getDeletedCount();

		logger.info("Deleted " + deletedCount + " documents from collection: " + collectionName);
		return deletedCount;
	}

	public static Document getSingleDocumentWithPolling(String envName, String serviceName, String collectionName,
			Bson filter, Bson sort, int limit, int interval, int maxAttempts) {
		
		int attempt = 0;
		Document result = null;

		while (attempt < maxAttempts) {
			attempt++;
			logger.info("Polling attempt " + attempt + " of " + maxAttempts);

			result = getSingleDocument(envName, serviceName, collectionName, filter, sort, limit);

			if (result != null && !result.isEmpty()) {
				logger.info("Document found in attempt " + attempt + ": " + result.toJson());
				return result;
			}

			if (attempt < maxAttempts) {
				logger.info("No document found. Waiting for " + interval + " seconds before retrying...");
				utils.longWaitInSeconds(interval);
			} else {
				String message = String.format("Even after %d seconds (%d attempts), no document found in collection '%s' with filter: %s",
						interval * maxAttempts, maxAttempts, collectionName, filter);
				logger.error(message);
				TestListeners.extentTest.get().fail(message);
				Assert.assertNotNull(result, message);
			}
		}

		return null;
	}
	
	public static long updateDocuments(String envName, String serviceName, String collectionName, Bson filter, Bson update) {
	    logger.info("Updating documents in collection: " + collectionName);
	    logger.debug("Update filter: " + filter + ", Update operation: " + update);

	    MongoDatabase db = getDatabaseConnection(envName, serviceName);
	    if (db == null) {
	        logger.error("Database connection is null. Returning 0.");
	        return 0;
	    }

	    MongoCollection<Document> collection = db.getCollection(collectionName);
	    if (collection == null) {
	        logger.error("Collection " + collectionName + " is null. Returning 0.");
	        return 0;
	    }

	    long modifiedCount = collection.updateMany(filter, update).getModifiedCount();

	    logger.info("Updated " + modifiedCount + " documents in collection: " + collectionName);
	    return modifiedCount;
	}


	public static void closeConnection() {
		if (mongoClient != null) {
			mongoClient.close();
			mongoClient = null;
			database = null;
			logger.info("MongoDB connection closed.");
		}
	}
}