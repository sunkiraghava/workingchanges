package com.punchh.server.utilities;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.testng.annotations.Listeners;

import javax.mail.*;
import javax.mail.search.AndTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.FromStringTerm;
import javax.mail.search.RecipientStringTerm;
import javax.mail.search.SearchTerm;
import javax.mail.search.SubjectTerm;

@Listeners(TestListeners.class)
public class GmailConnection {
	static Logger logger = LogManager.getLogger(GmailConnection.class);
	static Properties prop = new Properties();
	private static GmailConnection instance;
	private static Folder inboxFolderInstance; // Singleton instance

	private static Store store;
	Utilities utilities = new Utilities();

	private GmailConnection() {
		
	}

	// Singleton accessor
	public static synchronized GmailConnection getInstance() throws Exception {
		if (instance == null) {
			instance = new GmailConnection();
			instance.connectToGmailServer();
			
		}
		return instance;
	}

	// Get Store object
	public Store getStore() {
		return store;
	}
	

	// Actual Gmail connection
	private void connectToGmailServer() throws Exception {
		logger.info("Connecting to the Gmail server.");
		Properties properties = getGmailProperties();
		Session emailSession = Session.getInstance(properties);
		store = emailSession.getStore(properties.getProperty("mail.store.protocol"));
		store.connect(getConfigProperty("gmail.username"), utilities.decrypt(getConfigProperty("gmail.password")));
		logger.info("Successfully connected to the Gmail server.");
	}

	private static String getPropertyValue(String fileName, String key) {
		String propFilePath = System.getProperty("user.dir") + "/resources/Properties/" + fileName;
		String value = "";

		FileInputStream fis;
		try {
			fis = new FileInputStream(propFilePath);
			prop.load(fis);
		} catch (Exception e) {
			e.printStackTrace();
		}

		value = prop.get(key).toString();
		if (StringUtils.isEmpty(value)) {
			try {
				throw new Exception("Unspecified for key: " + key + " in properties file: " + fileName);
			} catch (Exception e) {
			}
		}
		return value;
	}

	public static String getConfigProperty(String key) {
		return getPropertyValue("config.properties", key);
	}

	// Gmail properties loader
	public static Properties getGmailProperties() {
		logger.info("Loading configuration for Gmail.");
		Properties properties = new Properties();
		properties.put("mail.store.protocol", getConfigProperty("gmail.store.protocol"));
		properties.put("mail.imaps.host", getConfigProperty("gmail.imaps.host"));
		properties.put("mail.imaps.port", getConfigProperty("gmail.imaps.port"));
		properties.put("mail.imaps.starttls.enable", getConfigProperty("gmail.imaps.starttls.enable"));
		return properties;
	}

	public static synchronized Folder openInboxFolder(Store store) throws Exception {
		if (inboxFolderInstance == null || !inboxFolderInstance.isOpen()) {
			// If folder is null or was closed, reinitialize
			inboxFolderInstance = store.getFolder("INBOX");
			inboxFolderInstance.open(Folder.READ_WRITE);
		} else {
			
		}
		return inboxFolderInstance;
	}

	public static String getGmailEmailBody(String searchBy, String searchValue, boolean withPolling) throws Exception {
		Store store = getInstance().getStore();
		Folder emailFolder = openInboxFolder(store);

		String body = "";
		boolean emailFound = false;
		int maxAttempts = 24;
		int attempts = 0;
		String subject = "";
		
		// Polling loop: retry searching for emails if not found

		while (attempts < maxAttempts && !emailFound) {
			// Refresh folder state (forces the folder to sync with the server)
			emailFolder.getMessageCount();

			Message[] messages = searchEmails(emailFolder, searchValue, searchBy);

			if (messages.length > 0) {
				logger.info("Matching emails found: " + messages.length);
				TestListeners.extentTest.get().info("Matching emails found: " + messages.length);
				// Extract the text content from the first matching email body
				body = extractTextFromEmailMessage(messages[0]);
				logger.info("First Matching Email Body: " + body);
				TestListeners.extentTest.get().info("First Matching Email Body: " + body);
				
				// Exit loop after finding an email
				emailFound = true;
			} else {
				logger.info("No matching emails found, retrying... (attempt " + (attempts + 1) + " of " + maxAttempts + ")");
				TestListeners.extentTest.get().info("No matching emails found, retrying... (attempt " + (attempts + 1) + " of " + maxAttempts + ")");
				attempts++;
				if (attempts < maxAttempts) {
					// Wait for 5 seconds before retrying
					Thread.sleep(5000);
				} else {
					logger.info("Max attempts reached. Stopping search.");
					TestListeners.extentTest.get().info("Max attempts reached. Stopping search.");
				}
			}
		}

		if (!emailFound) {
			logger.info("Error No matching emails found after " + maxAttempts + " attempt(s)");
			TestListeners.extentTest.get().info("Error No matching emails found after " + maxAttempts + " attempt(s)");
		}

		return body;
	}

	public static Message[] searchEmails(Folder emailFolder, String searchValue, String searchBy) throws Exception {
		if (searchValue == null || searchValue.isEmpty()) {
			throw new IllegalArgumentException("searchValue must not be null or empty.");
		}

		logger.info("Searching unread emails with search by: " + searchBy + " with value: " + searchValue);
		TestListeners.extentTest.get().info("Searching unread emails with search by: " + searchBy + " with value: " + searchValue);

		// Define a search term for unread emails
		SearchTerm statusTerm = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
		SearchTerm searchCondition;

		if ("toAndFromEmail".equalsIgnoreCase(searchBy)) {
			// Handle toAndFromEmail search
			if (!searchValue.contains(",")) {
				throw new IllegalArgumentException(
						"searchValue must be in format 'toEmail,fromEmail' for toAndFromEmail search.");
			}
			String[] parts = searchValue.split(",", 2);
			String toEmail = parts[0].trim();
			String fromEmail = parts[1].trim();

			SearchTerm toTerm = new RecipientStringTerm(Message.RecipientType.TO, toEmail);
			SearchTerm fromTerm = new FromStringTerm(fromEmail);
			searchCondition = new AndTerm(new SearchTerm[] { statusTerm, toTerm, fromTerm });
		} else {
			// Handle single criteria search
			SearchTerm valueTerm = null;
			if ("subject".equalsIgnoreCase(searchBy)) {
				valueTerm = new SubjectTerm(searchValue);
			} else if ("toEmail".equalsIgnoreCase(searchBy)) {
				valueTerm = new RecipientStringTerm(Message.RecipientType.TO, searchValue);
			} else if ("fromEmail".equalsIgnoreCase(searchBy)) {
				valueTerm = new FromStringTerm(searchValue);
			} else {
				logger.info("Warn Invalid searchBy parameter: " + searchBy);
				TestListeners.extentTest.get().info("Warn Invalid searchBy parameter: " + searchBy);
				throw new IllegalArgumentException(
						"Invalid searchBy parameter. Use 'subject', 'toEmail', 'fromEmail', or 'toAndFromEmail'.");
			}
			searchCondition = new AndTerm(statusTerm, valueTerm);
		}

		// Perform the search
		Message[] messages = emailFolder.search(searchCondition);
		logger.info("Found " + messages.length + " unread emails matching the criteria.");
		TestListeners.extentTest.get().info("Found " + messages.length + " unread emails matching the criteria.");
		return messages;
	}

	public static String extractTextFromEmailMessage(Message message) throws Exception {
		StringBuilder result = new StringBuilder();
		if (message.isMimeType("text/plain")) {
			result.append(message.getContent().toString());
		} else if (message.isMimeType("text/html")) {
			String html = message.getContent().toString();
			result.append(Jsoup.parse(html).text());
		} else if (message.isMimeType("multipart/*")) {
			Multipart multipart = (Multipart) message.getContent();
			for (int i = 0; i < multipart.getCount(); i++) {
				BodyPart bodyPart = multipart.getBodyPart(i);
				if (bodyPart.isMimeType("text/plain")) {
					result.append(bodyPart.getContent().toString());
				} else if (bodyPart.isMimeType("text/html")) {
					String html = bodyPart.getContent().toString();
					result.append(Jsoup.parse(html).text());
				} else if (bodyPart.getContent() instanceof Multipart) {
					Multipart nestedMultipart = (Multipart) bodyPart.getContent();
					for (int j = 0; j < nestedMultipart.getCount(); j++) {
						BodyPart nestedPart = nestedMultipart.getBodyPart(j);
						if (nestedPart.isMimeType("text/plain")) {
							result.append(nestedPart.getContent().toString());
						} else if (nestedPart.isMimeType("text/html")) {
							String nestedHtml = nestedPart.getContent().toString();
							result.append(Jsoup.parse(nestedHtml).text());
						} else {
							logger.info("Warn, Skipping unsupported nested MIME type: " + nestedPart.getContentType());
							TestListeners.extentTest.get().info("Warn, Skipping unsupported nested MIME type: " + nestedPart.getContentType());
						}
					}
				} else {
					logger.info("Warn , Skipping unsupported MIME type: " + bodyPart.getContentType());
				}
			}
		} else {
			logger.info("Warn Unsupported message type: " + message.getContentType());
			TestListeners.extentTest.get().info("Warn Unsupported message type: " + message.getContentType());
		}
		logger.info("Extracted text: " + result.toString());
		return result.toString();
	}

	/*
	 * Retrieves both the email body and attachment filenames from the first
	 * matching email
	 */
	public static Map<String, Object> getGmailEmailBodyAndAttachments(String searchBy, String searchValue) throws Exception {
		Store store = getInstance().getStore();
		Folder emailFolder = openInboxFolder(store);

		String body = "";
		List<String> attachments = new ArrayList<>();
		boolean emailFound = false;
		int maxAttempts = 10;
		int attempts = 0;
		// Retry searching for email until found
		while (attempts < maxAttempts && !emailFound) {
			emailFolder.getMessageCount();
			Message[] messages = searchEmails(emailFolder, searchValue, searchBy);

			if (messages.length > 0) {
				logger.info("Matching emails found: " + messages.length);
				TestListeners.extentTest.get().info("Matching emails found: " + messages.length);
				body = extractTextFromEmailMessage(messages[0]);
				attachments = extractAttachmentFromEmailMessage(messages[0]);
				emailFound = true;
			} else {
				logger.info("No matching emails found, retrying... (attempt " + (attempts + 1) + " of " + maxAttempts + ")");
				TestListeners.extentTest.get().info("No matching emails found, retrying... (attempt " + (attempts + 1) + " of " + maxAttempts + ")");
				attempts++;
				if (attempts < maxAttempts) {
					Thread.sleep(5000);
				}
			}
		}

		if (!emailFound) {
			logger.info("Error", "No matching emails found after " + maxAttempts + " attempt(s)");
			TestListeners.extentTest.get().info("Error No matching emails found after " + maxAttempts + " attempt(s)");
		}
//		closeEmailSession(emailFolder, store);
		// store the body and attachments in a map and return
		Map<String, Object> result = new HashMap<>();
		result.put("body", body);
		result.put("attachments", attachments);
		return result;
	}
	
	// Extracts attachments from an email message and saves them
	public static List<String> extractAttachmentFromEmailMessage(Message message) throws Exception {
		List<String> attachments = new ArrayList<>();
		String downloadedFilePath = Paths.get(System.getProperty("user.dir"), "resources", "ExportData").toString();

		if (message.isMimeType("multipart/*")) {
			Multipart multipart = (Multipart) message.getContent();
			for (int i = 0; i < multipart.getCount(); i++) {
				BodyPart bodyPart = multipart.getBodyPart(i);
				String disposition = bodyPart.getDisposition();
				if (disposition != null && (disposition.equalsIgnoreCase(BodyPart.ATTACHMENT)
						|| disposition.equalsIgnoreCase(BodyPart.INLINE))) {
					String fileName = bodyPart.getFileName();
					if (fileName != null) {
						attachments.add(fileName);
						// Save the attachment, overwrite if exists
						InputStream is = bodyPart.getInputStream();
						Files.copy(is, Paths.get(downloadedFilePath, fileName), StandardCopyOption.REPLACE_EXISTING);
						is.close();
						logger.info("Saved attachment: " + fileName + " to " + downloadedFilePath);
						TestListeners.extentTest.get().info("Saved attachment: " + fileName + " to " + downloadedFilePath);
					}
				}
			}
		}
		return attachments;
	}

	public static void closeEmailSession() throws Exception {
		logger.info("Closing email session...START");
//		GmailConnection gc = new GmailConnection();
		Store store1 = store ; 
		Folder emailFolder = null ;
		if(store1 != null && store1.isConnected()) {
			emailFolder= 	store1.getFolder("INBOX");
		}
		 // Close the folder and store connections
		
		try {
			if (emailFolder != null && emailFolder.isOpen()) {
				emailFolder.close(false);
				logger.info("Email folder closed.");
				
			}else {
				
				logger.info("No Gmail INBOX Connection found to close.");
			}
		} catch (Exception e) {
			logger.info("info","Failed to close email folder: " + e.getMessage());
		}

		try {
			if (store1 != null && store1.isConnected()) {
				store1.close();
				logger.info("Email store closed.");
			}else {
				
				logger.info("No Gmail STORE Connection found to close.");
			}
		} catch (Exception e) {
			logger.info("Failed to close email store: " + e.getMessage());
		}

		logger.info("Closing email session...END");
	}

	/**
	 * Logs a message with the specified status and updates the Extent Test Listener
	 * based on the provided status.
	 *
	 * @param message The message to log and report.
	 * @param status  The status of the test (e.g., "PASS", "FAIL", "ERROR", "WARN",
	 *                "INFO", "WARNING" etc.).
	 */
	public  void logit(String status, String message) {
		var extentTest = TestListeners.extentTest.get();
		switch (status.toUpperCase()) {
		case "FAIL":
		case "ERROR":
			logger.error(message);
			if (extentTest != null)
				extentTest.fail(message);
			break;
		case "WARN":
		case "WARNING":
			logger.warn(message);
			if (extentTest != null)
				extentTest.warning(message);
			break;
		case "PASS":
			logger.info(message);
			if (extentTest != null)
				extentTest.pass(message);
			break;
		case "SKIP":
			logger.info(message);
			if (extentTest != null)
				extentTest.skip(message);
			break;
		case "INFO":
		default:
			logger.info(message);
			if (extentTest != null)
				extentTest.info(message);
			break;
		}
	}

	public void logit(String message) {
		logit("info", message);
	}

} // end of class
