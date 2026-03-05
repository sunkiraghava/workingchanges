package com.punchh.server.utilities;

public class MessagesConstants {

	// Oauth Apps
	public final static String oauthAppDeleteSuccessMsg = "We have marked the Oauth app for deletion. It will take another 24-48 hours to delete all the active tokens associated with the same";
	public final static String oauthAppCreateSuccessMsg = "Application created";
	public final static String oauthAppUpdateSuccessMsg = "Application updated.";
	public final static String basketLockedErrMsg = "Unable to access the user’s Discount Basket, the Basket is currently locked.";
	public final static String disabledBasicAuthErrorMessage = "Basic Authentication is disabled. Please contact your Punchh administrator.";
	public final static String GISEnablementRequiredMsg = "Identity service configurations needs to be enabled for this business unit.";
    public final static String identityServiceNotEnabledMsg = "Identity service not enabled. Please contact your support.";

	// Guest Identity Management
	public final static String guestIdentityConfigUpdateSuccessMsg = "Identity Authentication settings updated successfully.";
    public final static String deactivatedUserMessage = "User deactivated - please contact support.";

	// Guest Basic Auth Password Policy
	public final static String guestBasicAuthPPStrong = "Strong Password - minimum 8 characters with letter, symbol and number";
	public final static String guestBasicAuthPPModerate = "Moderate Password - minimum 8 characters";
	public final static String guestBasicAuthPPWeak = "Weak Password - minimum 6 characters";

	// Audit logs table
	public final static String auditLogsUserNotFound = "User not found";

	// CX MobileConfiguration
	public final static String toastMessage = "Unless Notification Templates are filled and saved, notifications will not be sent out to guests. "
			+ "If you choose not to customize the default notification message, you need to at least save it. Disable & Delete buttons will appear next to an activated notification.";
	public final static String notificationTemplateSaveMessage = "Notification Template saved.";
	public final static String membershipLevelUpdatedMessage = "Membership Level was successfully updated.";
	public final static String surveysUpdated = "Survey updated";
	public final static String themesInfoBanner = "Changes won’t be published for earlier versions. Check your version in the Framework Version Helper.";
	public final static String themesDescription = "Customize your mobile app's appearance by adjusting colors, layout, and icons. Preview changes live and publish instantly.";
	public final static String colorsInfoBanner = "The colors listed below are used throughout the mobile application. To change a color, select the checkbox next to it and use the color picker to choose a new one. Your selection will replace the default color.";
	public final static String brandDescription = "Brand colors define the core identity of your mobile application and are used for key elements like buttons, links, and headers. When selecting brand colors, choose those that reflect the brand’s personality, ensuring consistency and recognition throughout the interface.";
	public final static String primaryDescription = "The core brand color used across the interface for key interactive elements.";
	public final static String primaryTooltip = "Used for buttons, links, headers, bottom navigation, segments, and other primary elements.";
	public final static String primaryContrastDescription = "A contrasting color that ensures readability and accessibility when paired with the primary brand color.";
	public final static String primaryContrastTooltip = "Used for text and icons on primary brand backgrounds.";
	public final static String secondaryDescription = "A complementary brand color for less prominent interactive elements and accents.";
	public final static String secondaryTooltip = "Used for supporting elements like checkboxes, radio buttons, progress bars, and inputs.";
	public final static String accentDescription = "An additional color used to highlight supporting elements and create contrast. Ensure accessibility when paired with text.";
	public final static String accentTooltip = "Used for backgrounds like banners, section headers, and footers.";
	public final static String backgroundDescription = "The default background color applied across all screens.";
	public final static String backgroundTooltip = "Neutral canvas for text, cards, inputs, and other elements with sufficient contrast for accessibility.";
	public final static String utilityDescription = "Utility colors are used for functional elements like text, borders, backgrounds, and icons. These colors help organize content, improve readability, and provide visual separation. When selecting utility colors, prioritize accessibility and ensure they complement text and other elements for an optimal user experience.";
	public final static String baseDescription = "The primary neutral background color for content areas.";
	public final static String baseTooltip = "Used for cards, headers, tags, and container backgrounds.";
	public final static String borderDescription = "A neutral color used to divide content and define boundaries.";
	public final static String borderTooltip = "Used for borders, input outlines, and section dividers.";
	public final static String defaultDescription = "The default color for text, icons, and components not styled with brand colors.";
	public final static String defaultTooltip = "Used for primary text, icons, and general-purpose components.";
	public final static String resetToDefaultModalDescription = "Are you sure you want to reset your mobile application to its default settings? This will remove all customizations in the Themes Editor and return the app to its original state.\nContact the Professional Services team to verify the original configurations.";
	public final static String publishModalDescription = "Are you ready to publish updates? Once published, all changes will show up live for all mobile application users. Please make sure before proceeding.";
	public final static String publishSuccessMessage = "Theme published successfully\nChanges are now live in all mobile applications.";
	public final static String publishErrorMessageDefault = "We couldn’t publish your changes\nDefault\n: Enter a valid hex value";
	public final static String invalidHexInlineError = "Enter a valid hex value";
	public final static String layoutSetUpBottomNavigationText = "Start customizing by adding your bottom navigation items";
	public final static String addBottomNavInfoBanner = "Some required items have been added to speed up your setup. You can add up to 5 items and customize them once it’s added.";
	public final static String setAsDefaultModalHeader = "Set temp1 as default?";
	public final static String setAsDefaultModalDescription = "Are you sure you want to set temp1 as the default landing screen?";
	public final static String setAsDefaultModalYesButton = "Yes, Set as default";
	public final static String removeModalHeader = "Remove temp1?";
	public final static String removeModalDescription = "Are you sure you want to remove temp1 from your bottom navigation? By removing temp1, it will automatically show up in your More Menu by default.";
	public final static String removeModalYesButton = "Yes, Remove";
	public final static String removeModalNoButton = "No, Keep temp1";
	public final static String navMaxLimitReachedHeader = "Maximum 5 limit reached";
	public final static String navMaxLimitReachedDescription = "You have reached the maximum limit of 5 bottom navigation items. Please remove one before adding any new items.";
	public final static String navMinLimitReachedHeader = "Minimum options required";
	public final static String navMinLimitReachedDescription = "A minimum of 3 bottom navigation items is required. Please adjust your selections and try again.";
	public final static String visualizerInfo = "Your preview will appear once set up is complete";
	public final static String visualizerPreviewInfo = "This preview does not reflect your live app. Only the uploaded icons will appear in the live preview.";
	public final static String iconsSetUpBottomNavigationText = "To customize your icons, you need to set up your bottom navigation layout first. This will make sure that your icons will show up correctly and work as expected.";
	public final static String iconsBottomNavigationDescription = "Bottom navigation icons appear at the bottom of the screen. To update icons, upload image file(s). SVG only. 50 KB maximum file size.";
	public final static String layoutBottomNavigationDescription = "Customize the bottom navigation by dragging items to reorder them, add or remove items, and set a default screen.";
	public final static String publishErrorMessageInvalidFileType = "We couldn’t publish your changes\nMore\n: Upload a valid icon";
	public final static String iconsRemoveImageDescription = "Are you sure you want to remove this image? This cannot be undone.";
	public final static String iconsTabTooltipText = "Looks like new bottom navigation item(s) have been added. If you want to upload an icon for them, do it in the Icons tab";
	public final static String publishErrorMessageifFeatureFlagoff = "Gift Cards is not available. Please remove it from your layout to publish";
	public final static String deactivatedUser = "This guest has been deactivated from the loyalty program.";
	public final static String adminInviteInstructionPartialText = "Please click on the link below to accept the invitation.";

	// Segments
	public final static String smartSegmentTypeTooltip = "Pre-built PAR segments based on guest frequency, spend and behavior.";

	// Games
	public final static String gamesDisabled = "Business does not have games enabled";
	public final static String insufficientPrivileges = "Insufficient Privileges to access this resource";
	public final static String cataboomRequireDisabledFlags = "Error updating business: Before enabling Cataboom Integration (also known as PAR Games), ensure that the flags \"Has Games\", \"Enable Slot Machine Game?\" and \"Enable Scratch Match & Win Game?\" are disabled first";
	public final static String successUpdate = "Business was successfully updated.";
	public final static String gamesPageHeading = "PAR Games";
	public final static String gamesPageSubHeading = "Make Your Loyalty Program Fun with Gamification";
	public final static String gamesPageDescription = "Choose from 150+ interactive game templates including:";
	public final static String gamesPageListItems = "Scratch-Off\nSlot Machine\nSpinner\nQuiz\nPoll\nPuzzle\nThrow\nStrike\nPop & Grab\nand many more..";

	// Template Tags
	public final static String currentYearTagDescription = "Represents current year on the system.";
	public final static String gameJwtTokenTagDescription = "Generates a unique game URL per member using JWT token approach.";

	// Wallets and Passes
	public final static String taxConfigSuccessUpdate = "Tax configuration was successfully updated.";
	public final static String businessSalesTaxHintText = "Apply a single tax rate across the entire business. If no tax rate is provided, the system will not apply any tax to subscription base price.";
	public final static String stateSalesTaxHintText = "Apply tax rates configured at the state level. Locations linked to a state will automatically use its tax rate. If no rate is defined, those locations will not apply any tax to the subscription base price.";
	public final static String locationSalesTaxHintText = "Apply tax rates configured at the individual location level. If no tax rate is provided, the subscription base price will not add any tax to it.";
	public final static String taxRateHintText = "Enter the Business Level Sales Tax percentage. All subscription plans across states and locations will use this tax rate to calculate the final subscription plan price.";
	public final static String taxRateError = "Error updating tax configuration: Business level tax rate must be less than or equal to 100";
	public final static String taxRateFieldInlineError = "must be less than or equal to 100";
	public final static String locationLevelTaxDescription = "HINT:\nLocation-level tax values are not applied unless Local Sales Tax Strategy is selected in Tax Rules. If you have selected Business Level Sales Tax or State Sales Tax, any values entered in Location Level Tax will not be applied.\nConfigure location-level tax to apply tax rates at the location level. Subscription plan purchase will automatically use the tax rate defined at the location level. If no rate is defined, those locations will not apply any tax to the subscription base price.";

	// Locations
	public final static String locationGroupCreateRequestSubmitted = "Location Group Creation Request Received. You will be notified of the status via email.";
	public final static String locationGroupCSVFormatIssue = "The uploaded CSV file has formatting issues. Please ensure all rows are properly structured and enclosed.";
	public final static String locationGroup20PlusInvalidLocationId = "More than 20 invalid locations provided";
	public final static String locationGroupSomeInvalidIDsEmailSubject = "Bulk Location Group Upload Processed – Some Location IDs Were Invalid";
	public final static String locationGroupSomeInvalidIDsEmailBody = "Your recent bulk upload for location group creation with name \"temp\" has been processed successfully. However, a few invalid Punchh location_ids in the CSV could not be added to the group. You can manually add the correct locations to the location group OR create a new location group by uploading a corrected CSV. Please click here to see the list of invalid location_ids that could not be processed.";
	public final static String locationListExportSuccessMessage = "The location list as a CSV file will be sent shortly to the email ID you are currently logged in with.";
	public final static String locationGroupListExportSuccessMessage = "The location group list as a CSV file will be sent shortly to the email ID you are currently logged in with.";
	public final static String franchiseListExportSuccessMessage = "The franchise list as a CSV file will be sent shortly to the email ID you are currently logged in with.";
	public final static String locationListExportEmailSubject = "Your PAR Punchh Location List is Ready for Download";
	public final static String locationListExportEmailBody = "As requested, your PAR Punchh location list is now ready and can be downloaded as a CSV file. Note: If any newly added locations are missing from this list, please try exporting again after some time. It may take up to 15 minutes for new locations to sync with the data lake, which is the source of this list.";
	public final static String locationCreateRequestSubmitted = "Bulk Location Creation Request Received. You will be notified of the status via email.";
	public final static String locationCreateAllBlankError = "Please review the problems below:\nName can't be blank\nBulk guest activity file can't be blank";
	public final static String cantBeBlankError = "can't be blank";
	public final static String locationCreateCSVDuplicateNameError = "Name has already been taken";
	public final static String locationCreateCSVBlankNameError = "Name can't be blank";
	public final static String locationCreateCSVInvalidLocGroupError = "Invalid location group";
	public final static String locationCreateCSVUnorderedColumnsError = "Either information not provided in same order or missing some columns in the CSV file.";
	public final static String locationCreateDuplicateNameError = "Please review the problems below:\nName has already been taken";
	public final static String alreadyTakenError = "has already been taken";

	// Redemptions
	public final static String rewardNotAccessible = "Given reward is not accessible for this guest";
	public final static String redemptionNotPossible = "Redemption not possible since amount is 0.";
	public final static String discountQualificationFailed = "Discount qualification on receipt failed";

	// Campaigns
	public final static String challengeCampaignJoinedMsg = "You have successfully joined the challenge.";
	public final static String challengeCampaignOptedOutMsg = "You have successfully opted out of the challenge.";
	public final static String challengeCampaignExplicitOptInDisabledMsg = "Challenge campaign does not have explicit opt in enabled.";

    public final static String emailConfirmationSuccessMessage = "Your email was successfully confirmed";
    // Cockpit Dashboard
    public final static String driveThruPayErrorMessage = "Error updating business: Since 'Enable Loyalty Identification at Drive-Thru' feature is disabled for this brand, the “Enable Drive-Thru Pay using Short Code (using SSF)” features cannot be enabled.";
    public final static String SHORT_CODE_HINT = "Short Code Only - Select this option to allow members to generate a 4-digit short code for payment.";
    public final static String shortCodeAndSingleScanHint = "Short Code and Single Scan Code Only - Select this option to allow members to choose between a 4-digit short code or QR code for payment.";
    public final static String collectiblecategoryerror="can't be blank";
    public final static String collectiblecategoryfielderror="is too long (maximum is 254 characters)";
    public final static String collectiblelimiterror="Only 25 active categories are allowed for this business";
    public final static String collectiblecategorynameerror="Name is too long (maximum is 254 characters)";

	public final static String challengesOptInDisabledMsg = "Business does not have challenges opt in enabled.";
	public final static String challengesOptOutDisabledMsg = "Business does not have challenges opt out enabled.";
	
	//Collectible 
	public final static String collectibleCreationErrorMessage = "Collectible could not be created.";
	public final static String collectibleUpdationErrorMessage = "Collectible could not be updated.";
	public final static String collectibleDateErrorMessage = "cannot be in the past";
	public final static String collectibleInvalidImageErrorMessage = "The image format has to be PNG or SVG";
	public final static String collectibleImageSizeErrorMessage = "must be in between 0 Bytes and 1 MB";


}