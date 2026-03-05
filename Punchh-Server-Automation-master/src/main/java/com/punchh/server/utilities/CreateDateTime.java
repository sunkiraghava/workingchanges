package com.punchh.server.utilities;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.Listeners;

@Listeners(TestListeners.class)
public class CreateDateTime {
	static Logger logger = LogManager.getLogger(CreateDateTime.class);

	public static String createDateTimeFolder(String folderName) {
		Date now = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy HH-mm-ss");
		String time = dateFormat.format(now);
		time = folderName + "/" + time;
		File dir = new File(time);
		dir.mkdirs();
		return time;
	}

	public static void makeDirectory(String folderpath) {
		File dir = new File(folderpath);
		dir.mkdir();
	}

	public static String getRandomNumberSixDigit() {
		// It will generate 6 digit random Number.
		// from 0 to 999999
		Random rnd = new Random();
		int number = rnd.nextInt(999999);
		// this will convert any number sequence into 6 character.
		return String.format("%06d", number);
	}

	public static String getCurrentSystemDateAndYear() {
		Calendar mCalendar = Calendar.getInstance();
		String monthNumber = new SimpleDateFormat("MM").format(mCalendar.getTime());
		String yearVal = new SimpleDateFormat("YYYY").format(mCalendar.getTime());
		int dateNum = mCalendar.get(Calendar.DATE);
		// if (dateNum < 10)
		// dateVal = "0" + (dateNum);
		// String currentDateAndYear = dateVal + " " + monthName + " " + yearVal;
		String currentDateAndYear = yearVal + monthNumber + Integer.toString(dateNum);
		return currentDateAndYear;
	}

	/*
	 * Pass val argument 0 for get hour 1 for get minutes 2 for get seconds
	 */
	public static int getHourAndMinuteForCurrentSystemTime(int val) {
		Calendar mCalendar = Calendar.getInstance();
		String timeValue = new SimpleDateFormat("HH mm ss").format(mCalendar.getTime());
		String[] stringArr = timeValue.split(" ");
		if (val == 0)
			return Integer.parseInt(stringArr[0]);
		else if (val == 1)
			return Integer.parseInt(stringArr[1]);
		else if (val == 2)
			return Integer.parseInt(stringArr[2]);
		else {
			logger.error("Problem in value");
			return 0;
		}

	}

	public static String getTimeDateString() {
		Date now = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("HHmmssddMMyyyy");
		String time = dateFormat.format(now);
		return time;
	}

	public static String getTimeDateAsneed(String format) {
		Date now = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat(format);
		String time = dateFormat.format(now);
		return time;
	}

	public static String getUniqueString(String val) {
		Date now = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("HHmmssddMMyyyy");
		String time = dateFormat.format(now);
		return val + time;
	}

	public static String getCurrentDate() {
		Date now = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String time = dateFormat.format(now);
		return time;
	}

	public static String getTomorrowsDate() {
		String date = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		return date;
	}

	public static String getYesterdaysDate() {
		Calendar cal = Calendar.getInstance();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		cal.add(Calendar.DATE, -1);
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		return recieptDateTime;
	}

	// get past month date
	public static String getPastMonthDate() {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -1);
		String date = format.format(cal.getTime()).toString();
		return date;
	}

	// get past year date
	public static String getPastYearsDate(int years) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, -years);
		String date = format.format(cal.getTime()).toString();
		return date;
	}

	// get past year date
	public static String getFutureYearsDate(int years) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, +years);
		String date = format.format(cal.getTime()).toString();
		return date;
	}

	// get past year date
	public static String getPastMonthsDate(int months) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -months);
		String date = format.format(cal.getTime()).toString();
		return date;
	}

	// get past year date
	public static String getPastYearsWithFutureDate(int years, int afterDay) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, -years);
		cal.add(Calendar.DATE, +afterDay);
		String date = format.format(cal.getTime()).toString();
		return date;
	}

	// get past year date
	public static String getPastYearsWithFutureMonthAndDate(int years, int month, int afterDay) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, -years);
		cal.add(Calendar.MONTH, +month);
		cal.add(Calendar.DATE, +afterDay);
		String date = format.format(cal.getTime()).toString();
		return date;
	}

	public static String getNyearAgoYesterdayDate(int years) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, -years); // go back n year
		cal.add(Calendar.DATE, -1); // subtract 1 day
		return format.format(cal.getTime());
	}

	public static String getFutureDate() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date currentDate = new Date();

		// convert date to calendar
		Calendar c = Calendar.getInstance();
		c.setTime(currentDate);

		// manipulate date
		c.add(Calendar.YEAR, 1);
		c.add(Calendar.MONTH, 1);
		c.add(Calendar.DATE, 1);
		/*
		 * c.add(Calendar.HOUR, 1); c.add(Calendar.MINUTE, 1); c.add(Calendar.SECOND,
		 * 1);
		 */

		// convert calendar to date
		Date currentDatePlusOne = c.getTime();

		String time = dateFormat.format(currentDatePlusOne);
		return time;
	}

	public static String getRandomString(int n) {

		String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "0123456789" + "abcdefghijklmnopqrstuvxyz";

		StringBuilder sb = new StringBuilder(n);
		for (int i = 0; i < n; i++) {
			int index = (int) (AlphaNumericString.length() * Math.random());
			sb.append(AlphaNumericString.charAt(index));
		}
		return sb.toString();
	}

	public static double getRnadomAmountInTwoGivenNumbers(int min, int max) {
		Random rand = new Random();
		// Generate a random integer between min and max (inclusive)
		int randomInt = rand.nextInt((max - min) + 1) + min;
		// Cast it to a double (no decimal part)
		double randomDouble = (double) randomInt;
		return randomDouble;
	}

	public static String getDateTimeString() {

		Date date = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyyHHmmss");
		String time = formatter.format(date);
		return time;
	}

	public static String getTomorrowDate() {
		Calendar cal = Calendar.getInstance();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		cal.add(Calendar.DATE, +1);
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		return recieptDateTime;
	}

	public static String getTomorrowDate2() {
		Calendar cal = Calendar.getInstance();
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
		cal.add(Calendar.DATE, +1);
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		return recieptDateTime;
	}

	public static String getTomorrowDateTime() {
		Calendar cal = Calendar.getInstance();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd' 'hh:mm aa");
		cal.add(Calendar.DATE, +1);
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		return recieptDateTime;
	}

	public static String getFutureDateTime(int afterDays) {
		Calendar cal = Calendar.getInstance();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd' 'hh:mm aa");
		cal.add(Calendar.DATE, +afterDays);
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		return recieptDateTime;
	}

	public static String getFutureDate(int afterDays) {
		Calendar cal = Calendar.getInstance();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		cal.add(Calendar.DATE, +afterDays);
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		return recieptDateTime;
	}

	public static String getCurrentDateAATime() {
		Calendar cal = Calendar.getInstance();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd' 'hh:mm aa");
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		return recieptDateTime;
	}

	public static String getDesiredMinAheadCurrentDateAATime(int min) {
		Calendar cal = Calendar.getInstance();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd' 'hh:mm aa");
		cal.add(Calendar.MINUTE, +min);
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		return recieptDateTime;
	}

	public static String getYesterdayDateTime() {
		Calendar cal = Calendar.getInstance();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd' 'hh:mm aa");
		cal.add(Calendar.DATE, -1);
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		return recieptDateTime;
	}

	public static String getCurrentDateTimeInUtc() {
		Calendar cal = Calendar.getInstance();
		// cal.add(Calendar.DATE, +1);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		return recieptDateTime;
	}

	public static String get15MinAheadTimeInUtc() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, +15);
		cal.add(Calendar.SECOND, +32);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		return recieptDateTime;
	}

	public static String timeFormatToAmPm(String recieptDateTime) throws ParseException {
		DateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		DateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm aa");
		String timeDate = outputFormat.format(inputFormat.parse(recieptDateTime));
		return timeDate;
	}

	public static String get15MinAheadTime() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, +15);
		// cal.add(Calendar.SECOND, +32);
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd HH:mm");
		// dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		return recieptDateTime;
	}

	public static String get15MinAheadTimeInIST() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, +15);
		// cal.add(Calendar.SECOND, +32);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
		// dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		return recieptDateTime;
	}

//	public static String startDate() {
//		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss aa");
//		Date date = new Date();
//		Calendar cal = Calendar.getInstance();
//		cal.setTime(date);
//		cal.add(Calendar.DATE, -7);
//		Date dateBefore7Days = cal.getTime();
//		return dateFormat.format(dateBefore7Days);
//	}
//
//	public static String endDate() {
//		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss aa");
//		Date date = new Date();
//		Calendar cal = Calendar.getInstance();
//		cal.setTime(date);
//		cal.add(Calendar.DATE, -7);
//		Date dateBefore7Days = cal.getTime();
//		return dateFormat.format(dateBefore7Days);
//	}

//	public static String createNewExcelFolder() {
//		String newFolderLoc = "src\\test\\resources\\TestData\\"
//				+ new SimpleDateFormat("dd MMM yyyy HH-mm-ss").format(new Date());
//		new File(newFolderLoc).mkdir();
//		return newFolderLoc;
//	}
	public static String getWeeklaterDate(int i) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date currentDate = new Date();

		// convert date to calendar
		Calendar c = Calendar.getInstance();
		c.setTime(currentDate);

		// manipulate date
		// c.add(Calendar.YEAR, 1);
		// c.add(Calendar.MONTH, 1);
		c.add(Calendar.DATE, i);
		/*
		 * c.add(Calendar.HOUR, 1); c.add(Calendar.MINUTE, 1); c.add(Calendar.SECOND,
		 * 1);
		 */

		// convert calendar to date
		Date currentDatePlusOne = c.getTime();

		String time = dateFormat.format(currentDatePlusOne);
		return time;
	}

	public static String getFirstDateOfMonth() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-01");
		Date currentDate = new Date();
		// convert date to calendar
		Calendar c = Calendar.getInstance();
		c.setTime(currentDate);
		// manipulate date
		c.add(Calendar.MONTH, 1);
		// convert calendar to date
		Date currentDatePlusOne = c.getTime();

		String time = dateFormat.format(currentDatePlusOne);
		return time;
	}

	public static String getFutureDateNew(int days) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date currentDate = new Date();
		Calendar c = Calendar.getInstance();
		c.setTime(currentDate);

		c.add(Calendar.YEAR, 1);
		c.add(Calendar.MONTH, 1);
		c.add(Calendar.DATE, days);
		Date currentDatePlusOne = c.getTime();

		String time = dateFormat.format(currentDatePlusOne);
		return time;
	}

	public static String convertToMonth(String inputDate) throws ParseException {
		SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd, yyyy HH:mm");
		Date date = inputFormat.parse(inputDate);
		String formattedDate = outputFormat.format(date);
		logger.info("Date format: " + formattedDate);
		return formattedDate;
	}

	public static String getYesterdayDateTimeUTC(int pastDaysNo) {

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -pastDaysNo);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		return recieptDateTime;
	}

	public static String getYesterdayDays(int pastDaysNo) {
		Calendar cal = Calendar.getInstance();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		cal.add(Calendar.DATE, -pastDaysNo);
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		return recieptDateTime;
	}

	public static String getCurrentDateOnly() {
		Date now = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd");
		String time = dateFormat.format(now);
		return time;
	}

	public static String getCurrentMonthOnly() {
		LocalDate currentdate = LocalDate.now();
		Month currentMonth = currentdate.getMonth();
		String month = currentMonth.toString();
		return month;
	}

	// Get current year only
	public static String getCurrentYear() {
		LocalDate currentDate = LocalDate.now();
		int currentYear = currentDate.getYear();
		return String.valueOf(currentYear);
	}

	public static String previousYearDateAndTime(int pastYearsFromNow) {
		Calendar cal = Calendar.getInstance();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		cal.add(Calendar.YEAR, -pastYearsFromNow);
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		return recieptDateTime;
	}

	public static String previousYearDate(int pastYearsFromNow) {
		Calendar cal = Calendar.getInstance();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		cal.add(Calendar.YEAR, -pastYearsFromNow);
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		return recieptDateTime;
	}

	public String convertUtcIn12HourFormat(String utcTime, String timezone) {
		// Parse the UTC time as a LocalTime object, assuming today's date
		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
		LocalTime utcLocalTime = LocalTime.parse(utcTime, timeFormatter);

		// Create a LocalDateTime object with today's date and UTC time
		LocalDate today = LocalDate.now();
		LocalDateTime utcDateTime = LocalDateTime.of(today, utcLocalTime);

		// Convert UTC time to timezone
		ZonedDateTime dateTime = utcDateTime.atZone(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.of(timezone));

		// Format the IST time in 12-hour format
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
		String timeStr = formatter.format(dateTime);

		return timeStr;
	}

	public static String getSundayDateOfCurrentWeek() {
		LocalDate today = LocalDate.now();
		// Go backward to get sunday
		LocalDate sunday = today;
		while (sunday.getDayOfWeek() != DayOfWeek.SUNDAY) {
			sunday = sunday.minusDays(1);
		}
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("LLLL dd, yyyy");
		String formattedString = sunday.format(formatter);
		return formattedString;
	}

	public static String getSaturdayDateOfCurrentWeek() {
		LocalDate today = LocalDate.now();
		// Go forward to get saturday
		LocalDate saturday = today;
		while (saturday.getDayOfWeek() != DayOfWeek.SATURDAY) {
			saturday = saturday.plusDays(1);
		}
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("LLLL dd, yyyy");
		String formattedString = saturday.format(formatter);
		return formattedString;
	}

	public static String getFutureDateTimeUTC(int pastDaysNo) {

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, +pastDaysNo);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		// dateFormat.setTimeZone(TimeZone.getTimeZone("IST"));
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		return recieptDateTime;
	}

	public static String getCurrentWeekDay() {
		Calendar calendar = Calendar.getInstance();
		Date date = calendar.getTime();
		String day = new SimpleDateFormat("EEEE", Locale.ENGLISH).format(date.getTime());
		return day;
	}

	public static String getPreviousDate(int str) {
		LocalDate today = LocalDate.now();
		LocalDate previousDate = today.minusDays(str);
		DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String formatedDate = previousDate.format(dateFormat);
		return formatedDate;
	}

	public static String convertDateFormat(String date) throws ParseException {
		SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy");
		String formattedDate = null;
		Date parsedDate = inputFormat.parse(date);
		formattedDate = outputFormat.format(parsedDate);
		return formattedDate;
	}

	public static String getPastDateString() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy");
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -1);
		String date = dateFormat.format(cal.getTime()).toString();
		return date;
	}

	public static String getCurrentDateString() {
		// SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy");
		// Calendar cal = Calendar.getInstance();
		Date now = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy");
		String date = dateFormat.format(now);
		return date;
	}

	public static String getPastDateStringNew() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -1);
		String date = dateFormat.format(cal.getTime()).toString();
		return date;
	}

	public static String getCurrentDateStringNew() {
		// SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy");
		// Calendar cal = Calendar.getInstance();
		Date now = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
		String date = dateFormat.format(now);
		return date;
	}

	public static String getPastYearDate(int years) {
		SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, -years);
		String date = format.format(cal.getTime()).toString();
		return date;
	}

	public static boolean compareTimeRange(String value) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy hh:mm a z");
		LocalDateTime dateTime = LocalDateTime.parse(value, formatter);
		LocalTime time = dateTime.toLocalTime();
		LocalTime startTime = LocalTime.MIDNIGHT;
		LocalTime endTime = LocalTime.of(15, 30);
		if (time.isAfter(startTime) && time.isBefore(endTime)) {
			return true;
		} else {
			return false;
		}
	}

	public static String getCurrentDateInPreviousYear(int number) {
		LocalDate currentDate = LocalDate.now();
		LocalDate previousYearDate = currentDate.minusYears(number);
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String formattedDate = previousYearDate.format(dateFormatter);
		LocalDateTime currentUtcTime = LocalDateTime.now(ZoneOffset.UTC);
		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
		String formattedTime = currentUtcTime.format(timeFormatter);
		String finalOutput = formattedDate + " " + formattedTime;
		return finalOutput;
	}

	public static String convertFormat(String dateToConvert) throws ParseException {
		SimpleDateFormat inputFormat = new SimpleDateFormat("ddMMyyyy");
		SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy");
		Date date = inputFormat.parse(dateToConvert);
		String outputDate = outputFormat.format(date);
		return outputDate;
	}

	public static String getPastYearDateInDifferentFormat(int years) {
		SimpleDateFormat format = new SimpleDateFormat("ddMMyyyy");
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, -years);
		String date = format.format(cal.getTime()).toString();
		return date;
	}

	public static String getMonthAndYear(String dateStr) {
		DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("ddMMyyyy");
		DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("MMMM yyyy");
		LocalDate date = LocalDate.parse(dateStr, inputFormatter);
		return date.format(outputFormatter);
	}

	public static int compareDates(String dateStr1, String dateStr2) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat("MMMM yyyy");
		Date date1 = formatter.parse(dateStr1);
		Date date2 = formatter.parse(dateStr2);
		if (date1.before(date2)) {
			return -1;
		} else if (date1.after(date2)) {
			return 1;
		} else {
			return 0;
		}
	}

	public static String convertMonthtoNumber(String monthName) throws ParseException {
		DateTimeFormatter parser = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH);
		Month month = Month.from(parser.parse(monthName));
		String monthNumber = String.valueOf(month.getValue());
		if (monthNumber.length() == 1) {
			monthNumber = "0" + monthNumber;
		}
		logger.info(monthNumber);
		return monthNumber;
	}

	public static String getYesterdaysDateInGivenFormate(String format) {
		Calendar cal = Calendar.getInstance();
		DateFormat dateFormat = new SimpleDateFormat(format);
		cal.add(Calendar.DATE, -1);
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		return recieptDateTime;
	}

	public static String getFutureDateTimeInGivenFormate(int afterDays, String format) {
		Calendar cal = Calendar.getInstance();
		DateFormat dateFormat = new SimpleDateFormat(format);
		cal.add(Calendar.DATE, +afterDays);
		String recieptDateTime = dateFormat.format(cal.getTime()).toString();
		return recieptDateTime;
	}

	public static String getPastYearsDateTimeInGivenFormate(int years, String format) {
		Calendar cal = Calendar.getInstance();
		DateFormat dateFormat = new SimpleDateFormat(format);
		cal.add(Calendar.YEAR, -years);
		String date = dateFormat.format(cal.getTime()).toString();
		return date;
	}

	public static String getCurrentDateTimeInGivenFormate(String format) {
		Date now = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat(format);
		String time = dateFormat.format(now);
		return time;
	}

	// For offer ingestion test cases
	public static String getDateInFormateNew(String originalDateStr) {
		// Step 1: Parse the original date string
		DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
		LocalDateTime dateTime = LocalDateTime.parse(originalDateStr, inputFormatter);

		// Step 2: Format the date and time in the desired format
		DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy hh:mm a");
		String formattedDate = dateTime.format(outputFormatter);
		return formattedDate;
	}

	public static String convertInToISTTimeZone(String originalDateStr) {
		// Step 1: Define the input formatter
		DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy hh:mm a");

		// Step 2: Parse the input date string to LocalDateTime
		LocalDateTime localDateTime = LocalDateTime.parse(originalDateStr, inputFormatter);

		// Step 3: Convert to IST (Indian Standard Time)
		ZoneId istZoneId = ZoneId.of("Asia/Kolkata");
		ZonedDateTime istDateTime = localDateTime.atZone(ZoneId.of("UTC")).withZoneSameInstant(istZoneId);

		// Step 4: Format the result
		DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
		String istFormattedDate = istDateTime.format(outputFormatter);

		// Print the IST time
		logger.info(istFormattedDate);

		return istFormattedDate;
	}

	public static String formatingISTIntoDesiredFormat(String istValueToConvert) {
		String expEndDateForUI1 = CreateDateTime.getDateInFormateNew(istValueToConvert);
		String expEndDateForUI = CreateDateTime.convertInToISTTimeZone(expEndDateForUI1);
		logger.info("expEndDateForUI-- " + expEndDateForUI);
		// Original date string
		String originalDateStr = expEndDateForUI;

		// Step 1: Define the input formatter
		DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

		// Step 2: Parse the input date string to ZonedDateTime
		ZonedDateTime istDateTime = ZonedDateTime.parse(originalDateStr, inputFormatter);

		// Step 3: Format the ZonedDateTime into the desired format
		DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy hh:mm a");
		String formattedDate = istDateTime.format(outputFormatter);

		// Print the result
		logger.info("After formating- " + formattedDate);
		return formattedDate;
	}

	public static String getAnniversaryDate() {
		Date now = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String date = dateFormat.format(now);
		return date;
	}

	public static boolean validatIsCurrentDate(String inputDate) {
		// Define the format of the input date string
		SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy hh:mm a z");

		// Get the current date
		Date currentDate = new Date();

		try {
			// Parse the input date string
			Date parsedDate = dateFormat.parse(inputDate);

			// Check if the parsed date is equal to the current date (ignoring time)
			// Set the time of both dates to 00:00:00 for comparison purposes
			SimpleDateFormat compareFormat = new SimpleDateFormat("yyyy-MM-dd");
			String formattedInputDate = compareFormat.format(parsedDate);
			String formattedCurrentDate = compareFormat.format(currentDate);

			return formattedInputDate.equals(formattedCurrentDate);

		} catch (ParseException e) {
			logger.info("Invalid date format.");
			return false;
		}
	}

	// Formats date time like "Jun 03, 2025 12:17 PM IST"
	public static String getFormattedDateTimeInIST() {
		Date now = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a z", Locale.ENGLISH);
		dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
		String formattedDateTime = dateFormat.format(now);
		return formattedDateTime;
	}

	public static String convertToISTTimeFromDateTime(String inputDateTime) {
		try {
			// Define the input date format (IST)
			SimpleDateFormat inputFormat = new SimpleDateFormat("MMMM dd, yyyy hh:mm a z");
			inputFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata")); // IST Time Zone

			// Parse the input string into Date
			Date parsedDate = inputFormat.parse(inputDateTime);

			// Define the output date format (Keep the same format)
			SimpleDateFormat outputFormat = new SimpleDateFormat("hh:mm a z");
			outputFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata")); // IST Time Zone

			// Format and return the date in desired output format
			return outputFormat.format(parsedDate);

		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String getIstTimzoneDateTimeFromAny(String datetime, String timzoneFrom) throws ParseException {

		String outputDateStr = null;
		// Define the input date format
		SimpleDateFormat inputFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a z");
		inputFormat.setTimeZone(TimeZone.getTimeZone(timzoneFrom)); // Set the input time zone

		// Parse the input date string
		Date date = inputFormat.parse(datetime);

		// Define the output date format
		SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd, yyyy hh:mm a z");
		outputFormat.setTimeZone(TimeZone.getTimeZone("IST")); // Set the output time zone

		// Format the date to IST
		outputDateStr = outputFormat.format(date);
		return outputDateStr;
	}

	public static String getAnyTimzoneDateTimeFromAny(String datetime, String timzoneFrom, String timzoneto)
			throws ParseException {

		String outputDateStr = null;
		// Define the input date format
		SimpleDateFormat inputFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a z");
		inputFormat.setTimeZone(TimeZone.getTimeZone(timzoneFrom)); // Set the input time zone

		// Parse the input date string
		Date date = inputFormat.parse(datetime);

		// Define the output date format
		SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd, yyyy hh:mm a z");
		outputFormat.setTimeZone(TimeZone.getTimeZone(timzoneto)); // Set the output time zone

		// Format the date to IST/EST any
		outputDateStr = outputFormat.format(date);
		return outputDateStr;
	}

	public static String convertISTtoOtherTimezone(String inputTime, String timeZone) {
		try {
			// Define the input date format (IST)
			SimpleDateFormat inputFormat = new SimpleDateFormat("hh:mm a");
			inputFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata")); // IST time zone

			// Parse the input time
			Date date = inputFormat.parse(inputTime);

			// Define the output date format (ACST)
			SimpleDateFormat outputFormat = new SimpleDateFormat("hh:mm a");
			// outputFormat.setTimeZone(TimeZone.getTimeZone("Australia/Darwin")); // ACST
			// time zone
			outputFormat.setTimeZone(TimeZone.getTimeZone(timeZone)); // ACST time zone

			// Format the date in ACST
			return outputFormat.format(date).toLowerCase();

		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}

	// future date in fromat "MMM dd, yyyy" Feb 26, 2025
	public static String getFutureDateinMonthDateYearFormat(int futureDays) {
		// Get today's date and add 7 days
		LocalDate futureDate = LocalDate.now().plusDays(futureDays);

		// Define the desired date format
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

		// Format the future date to the required format
		String formattedDate = futureDate.format(formatter);
		return formattedDate;
	}

	public static String getCurrentTimeInIST() {
		ZonedDateTime istTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss z");
		return istTime.format(formatter);
	}

	public static boolean isBeforeGivenTime(int hour, int minute) {
		LocalTime currentTime = LocalTime.now(ZoneId.of("Asia/Kolkata"));
		LocalTime targetTime = LocalTime.of(hour, minute);
		return currentTime.isBefore(targetTime);
	}

	public static boolean DateValidation(String dateString) {
		boolean flag = false;
		try {
			OffsetDateTime dateTime = OffsetDateTime.parse(dateString);
			return dateTime.getOffset().equals(ZoneOffset.UTC);
		} catch (Exception e) {
			logger.info("Invalid timestamp format: " + e.getMessage());
			TestListeners.extentTest.get().info("Invalid timestamp format: " + e.getMessage());
			return flag;
		}
	}

	public static String getDateTimePlusDays(String dateTime, int daysToAdd) {
		DateTimeFormatter formatter = null;
		LocalDateTime resultDateTime = null;
		try {
			formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a", Locale.US);
			LocalDateTime endDateTime = LocalDateTime.parse(dateTime, formatter);
			resultDateTime = endDateTime.plusDays(daysToAdd);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultDateTime.format(formatter);
	}

	public static String getDateTimeMinusDays(String inputDateStr, int daysToSubtract) {
		// Define the formatter with Locale.US for consistent AM/PM parsing
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a", Locale.US);

		// Parse the input string to LocalDateTime
		LocalDateTime dateTime = LocalDateTime.parse(inputDateStr, formatter);

		LocalDateTime threeDaysBefore = dateTime.minus(daysToSubtract, ChronoUnit.DAYS);

		// Format back to string and return
		return threeDaysBefore.format(formatter);
	}

	public static String convertDateFormatTo(String inputDate) {
		// Define input formatter (parse 2025-08-03 11:00 PM)
		DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a", Locale.US);

		// Parse the input string
		LocalDateTime dateTime = LocalDateTime.parse(inputDate, inputFormatter);

		// Define output formatter: "August 03, 2025 11:00 PM"
		DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy hh:mm a", Locale.US);

		// Format and return
		return dateTime.format(outputFormatter);
	}

	public static String generateTimestampFiveMinutesAhead() {
		Instant fiveMinutesLater = Instant.now().plus(5, ChronoUnit.MINUTES);
		ZonedDateTime utcTime = fiveMinutesLater.atZone(ZoneOffset.UTC);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
		String formattedDate = utcTime.format(formatter);
		return formattedDate;
	}

	// Get date from day number in current month and year in format YYYY-MM-DD
	public static String getDateFromDay(String dayStr) {
		int day = Integer.parseInt(dayStr); // convert string to int

		LocalDate today = LocalDate.now(); // current date
		LocalDate date = LocalDate.of(today.getYear(), today.getMonth(), day);

		return date.format(DateTimeFormatter.ISO_LOCAL_DATE); // YYYY-MM-DD
	}

	public static long convertTimeStampToUnixFormat(String input) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HHmmssddMMyyyy");
		LocalDateTime dateTime = LocalDateTime.parse(input, formatter);
		return dateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
	}
}
