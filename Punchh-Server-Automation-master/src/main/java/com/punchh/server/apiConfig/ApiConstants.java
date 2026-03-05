package com.punchh.server.apiConfig;

import java.util.Properties;

import com.punchh.server.utilities.Utilities;

public class ApiConstants {

	static Properties prop = Utilities.loadPropertiesFile("apiConfig.properties");
	// public final static String baseUri = prop.getProperty("baseUrl");
	// Mobile API2

	// public final static String baseUri = prop.getProperty("baseUrl");
	// Mobile API2
	public final static String mobApi2Login = "/api2/mobile/users/login";
	public final static String mobApi2Logout = "/api2/mobile/users/logout";
	public final static String mobApi2FetchUserBalance = "/api2/mobile/users/balance";
	public final static String mobApi2FetchUserExpiringPoints = "/api2/mobile/users/points_expiry_timeline";
	public final static String mobApi2BalanceTimeline = "/api2/mobile/balance_timelines";
	public final static String mobApi2FetchUserInfo = "/api2/mobile/users/profile";
	public final static String mobApi2UpdateUserInfo = "/api2/mobile/users";
	public final static String mobApi2CreateUserrelation = "/api2/mobile/user_relations";
	public final static String mobApi2UpdateUserrelation = "/api2/mobile/user_relations/";
	public final static String mobApi2DeleteUserrelation = "/api2/mobile/user_relations/";
	public final static String mobApi2SignUp = "/api2/mobile/users";
	public final static String mobApi2UserShow = "/api2/mobile/users/";
	public final static String mobApi2MigrationLookup = "/api2/mobile/migration_lookup";
	public final static String mobApi2LifetimeStats = "/api2/mobile/users/lifetime_stats";
	public final static String mobApi2SingleScanCode = "/api2/mobile/single_scan_tokens";
	public final static String mobApi2PaymentCard = "/api2/mobile/payment_cards";
	public final static String mobApi2asyncUserUpdate = "/api2/mobile/async_users";
	public final static String mobApi2EstimatePointsEarning = "/api2/mobile/estimate_points";
	public final static String mobApi2ForgotPassword = "/api2/mobile/users/forgot_password";
	public final static String mobApi2Sessiontoken = "/api2/mobile/session_tokens";
	public final static String mobApi2SendVerificationEmail = "/api2/mobile/users/send_verification_email";
	public final static String mobApi2PurchaseSubscription = "/api2/mobile/subscriptions";
	public final static String mobApi2SubscriptionRedemption = "/api2/mobile/redemptions/subscription";
	public final static String mobApi2UserOffer = "/api2/mobile/offers";
	public final static String mobApi2MarkOfferAsRead = "/api2/mobile/offers/mark_read";
	public final static String mobApi2RedemptionsRedeemableId = "/api2/mobile/redemptions/redeemable";
	public final static String mobApi2RedemptionsUsingVisits = "/api2/mobile/redemptions/redeemable";
	public final static String mobApi2RedemptionsUsingBankedCurrency = "/api2/mobile/redemptions/banked_currency";
	public final static String mobApi2RedemptionsUsingRewardId = "/api2/mobile/redemptions/reward";
	public final static String mobApi2TransferReward = "/api2/mobile/loyalty_transfers/reward";
	public final static String mobApi2TransferCurrency = "/api2/mobile/loyalty_transfers/currency";
	public final static String mobApi2ListAllDeals = "/api2/mobile/deals";
	public final static String mobApi2PointConversion = "/api2/mobile/conversions";
	public final static String mobApi2UserAccountHistory = "/api2/mobile/users/account_history";
	public final static String mobApi2GetDetailsDeals = "/api2/mobile/deals";
	public final static String mobApi2SaveSelectedDeal = "/api2/mobile/deals";
	public final static String mobApi2CheckinReceiptImage = "/api2/mobile/checkins/receipt_image";
	public final static String mobApi2CheckinBarCode = "/api2/mobile/checkins/barcode";
	public final static String mobApi2CheckinQRCode = "/api2/mobile/checkins/qrcode";
	public final static String mobApi2FetchCheckin = "/api2/mobile/checkins";
	public final static String mobApi2AccountBalance = "/api2/mobile/checkins/account_balance";
	public final static String mobApi2TransactionDetails = "/api2/mobile/checkins/transactions";
	public final static String mobApi2ListUserOffers = "/api2/mobile/offers";
	public final static String mobApi2ListApplicableOffers = "/api2/mobile/redemptions/applicable_offers";
	public final static String mobApi2PurchaseGiftCard = "/api2/mobile/gift_cards/purchase";
	public final static String mobApi2UpdateGiftCard = "/api2/mobile/gift_cards/";
	public final static String mobApi2ReloadGiftCard = "/api2/mobile/gift_cards/";
	public final static String mobApi2FetchGiftCard = "/api2/mobile/gift_cards";
	public final static String mobApi2FetchGiftCardBalance = "/api2/mobile/gift_cards/";
	public final static String mobApi2GiftCardTransactionHistory = "/api2/mobile/gift_cards/";
	public final static String mobApi2TransferGiftCard = "/api2/mobile/gift_cards/";
	public final static String mobApi2ShareGiftCard = "/api2/mobile/gift_cards/";
	public final static String mobApi2TipGiftCard = "/api2/mobile/gift_cards/";
	public final static String mobApi2DeleteGiftCard = "/api2/mobile/gift_cards/";
	public final static String mobApi2GiftaCard = "/api2/mobile/gift_cards/gift";
	public final static String mobApi2FetchNotifications = "/api2/mobile/notifications";
	public final static String mobApi2DeleteNotifications = "/api2/mobile/notifications/";
	public final static String mobApi2FetchMessages = "/api2/mobile/messages";
	public final static String mobApi2CreateFeedBack = "/api2/mobile/feedbacks";
	public final static String mobApi2UpdateFeedBack = "/api2/mobile/feedbacks/";
	public final static String mobApi2FetchClientToken = "/api2/mobile/secure_tokens/ordering";
	public final static String mobApi2ListChallenges = "/api2/mobile/challenges";
	public final static String mobApi2ChallengeOptOut = "/api2/mobile/challenge_opt_out";
	public final static String mobApi2ForgotPasscode = "/api2/mobile/passcodes/forgot_passcode";
	public final static String mobApi2CreatePasscode = "/api2/mobile/passcodes";
	public final static String mobApi2GenerateEpin = "/api2/mobile/gift_cards/";
	public final static String mobApi2CreateGiftCardClaimToken = "/api2/mobile/invitations";
	public final static String mobApi2GetInvitations = "/api2/mobile/invitations";
	public final static String mobApi2StatusOfClaimToken = "/api2/mobile/invitations/check_status";
	public final static String mobApi2TransferClaimToken = "/api2/mobile/invitations/transfer";
	public final static String mobApi2DeleteClaimToken = "/api2/mobile/invitations/";
	public final static String mobApi2SocialCauseCampaign = "/api2/mobile/social_causes";
	public final static String mobApi2CreateDonation = "/api2/mobile/social_causes";
	public final static String mobApi2SocialCauseCampaignDetails = "/api2/mobile/social_causes/";
	public final static String mobApi2FetchGameCards = "/api2/mobile/games/cards";
	public final static String mobApi2MarkMessagesRead = "/api2/mobile/message_readerships";
	public final static String mobApi2DeleteMessages = "/api2/mobile/message_readerships/";
	public final static String mobApi2GenerateOtpToken = "/api2/mobile/verify_token";
	public final static String mobApi2CardCompletionRedemption = "/api2/mobile/redemptions/visits";
	public final static String mobApi2V2MetaAPI = "/api2/mobile/meta";
	public final static String mobApi2CancelSubscription = "/api2/mobile/subscriptions/cancel";
	public final static String mobApi2CheckinAccountBalance = "/api2/mobile/checkins/account_balance";
	public final static String mobApi2UserMembershipLevelMobileApi = "/api2/mobile/users/membership_levels";
	public final static String mobApi2RedemptionsUsingVisit = "/api2//mobile/redemptions/visits";
	public final static String mobApi2SearchLocations = "/api2/mobile/locations/search";
	public final static String mobApi2LocationConfiguration = "/api2/mobile/locations/configuration";
	public final static String mobApi2DiagnosticLogs = "/api2/mobile/locations";
	public final static String mobApi2GetParPaytoken = "/api2/mobile/iframe_payments/new";
	public final static String mobApi2RecordPayment = "/api2/mobile/payments";
	public final static String mobApi2GetClientToken = "api2/mobile/payments/client_token";
	public final static String mobApi2FetchClientTokens = "api2/mobile/secure_tokens/service";
	public final static String mobApi2MigrationLookUp = "api2/mobile/migration_lookup";
	public final static String mobApi2GoogleLogin = "/api2/mobile/users/google_sign_in";
	public final static String mobApi2UpdateBadgeWithStoryId = "/api2/mobile/badges/";
	public final static String mobApi2ApplyCoupons = "/api2/mobile/coupons";
	public final static String mobApi2ListGiftCardDesign = "api2/mobile/card_designs";
	public final static String mobApi2CreatePaymentCard = "/api2/mobile/payment_cards";
	public final static String mobApi2FetchAllPaymentCard = "/api2/mobile/payment_cards";
	public final static String mobApi2UpdatePaymentCard = "/api2/mobile/payment_cards";
	public final static String mobApi2BeaconEntry = "/api2/mobile/locations/beacon_entry";
	public final static String mobApi2BeaconExit = "/api2/mobile/locations/beacon_exit";
	public final static String mobApi2GenerateSingleScanCode = "/api2/mobile/single_scan_tokens";
	public final static String mobAPi2RemoveItemFromDiscountbasket = "api2/mobile/discounts/unselect";
	public final static String mobApi2FetchActivePurchasableSubscription = "/api2/mobile/subscriptions";
	public final static String mobApi2AccountDeletionRequest = "/api2/mobile/users";
	public final static String mobApi2GiftCard = "/api2/mobile/gift_cards/";
	public final static String mobApi2SegmentEligibility = "/api2/mobile/customers";
	public final static String mobAPi2GoogleSignup = "/api2/mobile/users/google_sign_in";
	public final static String mobApi2DriveThruCode = "/api2/mobile/drivethru_code";
	public final static String mobApi2GameUrlsEndpointApi = "/api2/mobile/par_games";
	public final static String mobApi2SubscriptionApplicableTaxes = "/api2/mobile/subscription_taxes/applicable_taxes";
	public final static String mobApi2cards = "/api2/mobile/meta.json";
	public final static String mobApi2AddExistingGiftCard = "/api2/mobile/gift_cards";
	public final static String mobApi2ChallengeOptIn = "/api2/mobile/challenge_opt_in";
	public final static String mobApi2DiscountBasketAddeddAPI = "/api2/mobile/discounts/select";
	public final static String mobApi2GetDiscountBasketDetailsAPI = "/api2/mobile/discounts/active";
	public final static String mobApi2DeleteBasketForUserAPI = "/api2/mobile/discounts/unselect";
	public final static String mobApi2ssoUserTokensMobile = "/api2/mobile/user_tokens/generate";
	public final static String mobApi2GetAccessCodeAPI = "/api2/mobile/single_scan_tokens";
	public final static String mobApi2UserSubscriptionAPI = "/api2/mobile/user_subscriptions";
	public final static String mobApi2MetaV2ApiSubscriptionCancelReasons = "/api2/mobile/meta.json";
	public final static String mobApi2DeletePaymentCard = "/api2/mobile/payment_cards";
	public final static String mobApi2UserMemberShipLevel = "/api2/mobile/users/membership_levels";
	public final static String mobApi2Reactivationrequest = "/api2/mobile/users/request_reactivation";

	public final static String getCampaignNameFromSuperadmin = "/superadmin/mass_campaigns/scheduled";
	public final static String wifiSignUp = "/api2/wifi/users";

	// API1 resources
	public final static String mobApi1Login = "/api/mobile/customers/login";
	public final static String mobApi1SignUp = "/api/mobile/customers";
	public final static String mobApi1UserShow = "/api/mobile/customers";
	public final static String mobApi1Logout = "/api/mobile/customers/logout";
	public final static String mobApi1FetchMessages = "/api/mobile/messages";
	public final static String mobApi1UpdateUser = "/api/mobile/users";
	public final static String mobApi1CreateFeedback = "/api/mobile/feedbacks";
	public final static String mobApi1BraintreeToken = "/api/mobile/braintree_payments/client_token";
	public final static String mobApi1GiftCard = "/api/mobile/gift_cards/";
	public final static String mobApi1CreatePasscode = "/api/mobile/passcodes";
	public final static String mobApi1UpdatePasscode = "/api/mobile/passcodes/forgot_passcode";
	public final static String mobApi1CurrencyTransfer = "/api/mobile/loyalty_transfers/currency";
	public final static String mobApi1PointTransfer = "/api/mobile/loyalty_transfers/points";
	public final static String mobApi1ImportGiftCard = "/api/mobile/gift_cards";
	public final static String mobApi1ScratchBoard = "/api/mobile/scratch_boards";
	public final static String mobApi1FetchNotifications = "/api/mobile/users/notifications";
	public final static String mobApi1VersionNotes = "/api/mobile/version_notes";
	public final static String mobApi1MigrationUserLookup = "/api/mobile/migrations_lookup";
	public final static String mobApi1FetchUserOffers = "/api/mobile/offers";
	public final static String mobApi1GenerateOtpToken = "/api/mobile/verify_token";
	public final static String mobApi1BeaconEntry = "/api/mobile/locations/beacon_entry";
	public final static String mobApi1BeaconExit = "/api/mobile/locations/beacon_exit";
	public final static String mobApi1SocialCause = "/api/mobile/social_causes";
	public final static String mobApi1Redemption = "/api/mobile/redemptions";
	public final static String mobApi1UpdateGuestDetails = "/api/mobile/customers/";
	public final static String mobApi1TransferLoyaltypoints = "/api/mobile/loyalty_transfers/points";
	public final static String mobApi1PurchaseGiftCard = "/api/mobile/gift_cards/purchase";
	public final static String mobApi1ReloadGiftCard = "/api/mobile/gift_cards/";
	public final static String mobApi1BarCodeCheckin = "/api/mobile/checkins";
	public final static String mobApi1createPickupOrder = "/api/mobile/orders";
	public final static String mobApi1cards = "/api/mobile/cards";
	public final static String mobApi1Deals = "/api/mobile/deals";
	public final static String mobApi1accounts = "/api/mobile/accounts";
	public final static String mobApi1usersbalance = "/api/mobile/users/balance";
	public final static String updateCustomerStatus = "/api/mobile/orders/";
	public final static String mobApi1createVehicle = "/api/mobile/vehicles/";
	public final static String mobApi1CouponRedemptionOnMobile = "/api/mobile/coupons";
	public final static String mobAPi1ForgotPassword = "/api/mobile/customers/forgot_password";
	public final static String mobApi1ChangePassword = "/api/mobile/customers/change_password";
	public final static String userSubscriptionAPI1 = "/api/mobile/user_subscriptions";
	public final static String mobAPi1RewardGiftedForType = "/api/mobile/loyalty_transfers/reward";
	public final static String mobAPi1GamingAchievements = "/api/mobile/gaming_achievements";
	public final static String mobAPi1CheckinsBalance = "/api/mobile/checkins/balance";
	public final static String mobAPi1UserMembershipLevel = "/api/mobile/users/membership_levels";
	public final static String mobAPi1FetchCheckin = "/api/mobile/checkins";
	public final static String mobAPi1LoyaltyCard = "/api/mobile/loyalty_cards";
	public final static String mobAPi1GoogleSignup = "/api/mobile/customers/google_sign_in";
	public final static String mobApi1IssuanceGiftcard = "/api/mobile/gift_cards/issuance";
	public final static String mobAPi1createPaymentCard = "/api/mobile/payment_cards";
	public final static String parPaymentGetClientToken = "/api/mobile/par_payments/get_client_token";
	public final static String mobApi1ChallengeOptIn = "/api/mobile/challenge_opt_in";
	public final static String mobApi1DriveThruCode = "/api/mobile/drivethru_code";
	public final static String gameUrlsEndpoint = "/api/mobile/par_games";
	public final static String mobApi1ChallengeOptOut = "/api/mobile/challenge_opt_out";
	public final static String mobApi1SubscriptionApplicableTaxes = "/api/mobile/subscription_taxes/applicable_taxes";
	public final static String mobSingleScanCode = "/api/mobile/user_tokens/generate";
	public final static String secureSingleScanCode = "/api/mobile/single_scan_tokens";
	public final static String mobApiCancelSubscription = "/api/mobile/subscriptions/cancel";
	public final static String mobApiSubscriptionPurchase = "/api/mobile/subscriptions";
	public final static String mobApi1SecureAsyncUserUpdate = "/api/mobile/secure_async_users";

	// dashboard API's
	public final static String eClubGuestUpload = "api2/dashboard/eclub_guests";
	public final static String customSegments = "/api2/dashboard/custom_segments";
	public final static String customSegmentMembers = "/api2/dashboard/custom_segments/members";
	public final static String createMassCampaign = "/api2/dashboard/campaigns";
	public final static String segmentList = "/api2/dashboard/segments";
	public final static String redeemableList = "/api2/dashboard/redeemables";
	public final static String couponCampaignList = "/api2/dashboard/coupon_campaigns";
	public final static String dynamicCoupon = "/api2/dashboard/dynamic_coupons";
	public final static String createLocation = "/api2/dashboard/locations";
	public final static String getLocation = "/api2/dashboard/locations";
	public final static String updateLocation = "/api2/dashboard/locations";
	public final static String getLocationDetails = "/api2/dashboard/locations";
	public final static String createLocationgroup = "/api2/dashboard/location_groups";
	public final static String getLocationGroupListDetails = "/api2/dashboard/location_groups";
	public final static String getLocationGroupDetails = "/api2/dashboard/location_groups";
	public final static String addLocationtoGroup = "/api2/dashboard/location_groups/add_location";
	public final static String deleteLocationFromGroup = "/api2/dashboard/location_groups/remove_location";
	public final static String deleteLocation = "/api2/dashboard/locations";
	public final static String fetchSegmentGuestCount = "/api2/dashboard/segments/";
	public final static String getDashboardBusinessConfig = "/api2/dashboard/businesses/";
	public final static String banUser = "/api2/dashboard/users/ban";
	public final static String unBanUser = "/api2/dashboard/users/ban";
	public final static String sendMessageToUser = "/api2/dashboard/users/send_message";
	public final static String supportGiftingToUser = "/api2/dashboard/users/support";
	public final static String newSupportGifting = "/api2/dashboard/users/support_gifting";
	public final static String deactivateUser = "/api2/dashboard/users/deactivate";
	public final static String reactivateUser = "/api2/dashboard/users/reactivate";
	public final static String updateUser = "/api2/dashboard/users";
	public final static String userExport = "/api2/dashboard/users/send_user_details_export";
	public final static String extendedUserHistory = "/api2/dashboard/users/extensive_timeline";
	public final static String fetchUserFavLocation = "/api2/dashboard/user_favourite_locations";
	public final static String deleteUserFavLocation = "/api2/dashboard/user_favourite_locations";
	public final static String deleteUser = "/api2/dashboard/users";
	public final static String createBusinessMigrationUser = "/api2/dashboard/migration_users";
	public final static String updateBusinessMigrationUser = "/api2/dashboard/migration_users/";
	public final static String deleteBusinessMigrationUser = "/api2/dashboard/migration_users/";
	public final static String getAdminRolesList = "/api2/dashboard/roles";
	public final static String createBusinessAdmin = "/api2/dashboard/business_admins";
	public final static String updateBusinessAdmin = "/api2/dashboard/business_admins";
	public final static String showBusinessAdmin = "/api2/dashboard/business_admins";
	public final static String deleteBusinessAdmin = "/api2/dashboard/business_admins";
	public final static String dashboardAPI2Meta = "/api2/dashboard/meta";
	public final static String inviteBusinessAdmin = "/api2/dashboard/business_admins/invite";
	public final static String cReateFranchise = "/api2/dashboard/franchisees";
	public final static String uPdateFranchise = "/api2/dashboard/franchisees";
	public final static String dEleteFranchise = "/api2/dashboard/franchisees";
	public final static String createSocialCauesCampaign = "/api2/dashboard/social_cause_campaigns";
	public final static String activateSocialCauesCampaign = "/api2/dashboard/social_cause_campaigns/activate";
	public final static String deactivateSocialCauesCampaign = "/api2/dashboard/social_cause_campaigns/deactivate";
	public final static String searchRedemptionCode = "/api2/dashboard/redemptions";
	public final static String processRedemptionCode = "/api2/dashboard/redemptions";
	public final static String forceRedeem = "/api2/dashboard/redemptions/force_redeem";
	public final static String rollingToInactiveExpiry = "/api2/dashboard/expiry_restructures/rolling_to_inactivity";
	public final static String dashboardAPI2createFeedback = "/api2/dashboard/feedbacks";
	public final static String dashboardSubscriptionPurchase = "/api2/dashboard/subscriptions/purchase";
	public final static String dashboardSubscriptionCancel = "/api2/dashboard/subscriptions/cancel";
	public final static String userInSegment = "/api2/dashboard/segments/";
	public final static String bulkAdd = "/api2/dashboard/custom_segments/members/bulk_add";
	public final static String bulkRemove = "/api2/dashboard/custom_segments/members/bulk_remove";
	public final static String bulkUploadBmu = "/api2/dashboard/migration_users/bulk_bmu_upload";
	public final static String bulkDeleteLoyatyUsersApi = "api2/dashboard/users/bulk_delete";
	public final static String bulkDeactivateLayltyUsersAPi = "api2/dashboard/users/bulk_deactivate";
	public final static String bulkAddUsersInSegmentAPi = "api2/dashboard/custom_segments/members/bulk_add";
	public final static String redeemableListAPi = "api2/dashboard/offers/redeemable";
	public final static String uploadRedeemableImage = "api2/dashboard/offers/upload_redeemable_image";
	public final static String segmentBuilder = "/api2/dashboard/segments";
	public final static String getStateCodes = "api2/dashboard/state_codes";
	public final static String segmentCreationUsingBuilderClause = segmentBuilder;
	public final static String enrichOnlineOrderApi2 = "/api2/dashboard/users/redemption";
	public final static String dashboardAPIsendMessageToUser = "/api2/dashboard/users/send_message";
	public final static String dashboardAPiRenewalSubscription = "/api2/dashboard/subscriptions/renew";
	public final static String dashboardAPiUsersInfo = "/api2/dashboard/users/info";

	// auth API's
	public final static String authDeals = "/api/auth/deals";
	public final static String authApiSignUp = "/api/auth/customers.json";
	public final static String authApiLogin = "/api/auth/customers/sign_in";
	public final static String onlineOrderCheckin = "/api/auth/checkins/online_order";
	public final static String onlineOrderVoidCheckin = "/api/auth/checkins/";
	public final static String authOnlineRedemption = "/api/auth/redemptions/online_order";
	public final static String authUpdateUserInfo = "/api/auth/users";
	public final static String authGetUserInfo = "/api/auth/users";
	public final static String authAccountHistory = "/api/auth/accounts";
	public final static String authAccountBalance = "/api/auth/checkins/balance";
	public final static String authFetchUserBalance = "/api/auth/users/balance";
	public final static String authForgotPassword = "/api/auth/users/forgot_password";
	public final static String authVoidRedemption = "/api/auth/redemptions";
	public final static String authListAvailableRewards = "/api/auth/rewards";
	public final static String authRedemptionsApplicableOffers = "/api/auth/redemptions/applicable_offers";
	public final static String authFetchRedemption = "/api/auth/redemptions";
	public final static String authfetchCard = "/api/auth/cards";
	public final static String authSubscriptionMeta = "/api/auth/subscription_meta";
	public final static String authSubscriptionUser = "api/auth/user_subscriptions";
	public final static String authSubscriptionPurchase = "/api/auth/subscriptions";
	public final static String authSubscriptionCancel = "/api/auth/subscriptions/cancel";
	public final static String authUserBalance = "/api/auth/users/balance";
	public final static String authOrderingMeta = "/api/auth/ordering_meta";
	public final static String authGetPromotionAccounts = "/promotions/accounts/";
	public final static String authGetPromotionAccruals = "/promotions/accruals";
	public final static String authGoogleSignup = "/api/auth/users/connect_with_google";
	public final static String authGetPromotionAccrualsRedemptions = "/promotions/redemptions";
	public final static String authGetPromotionAccrualsValidate = "/promotions/validate";
	public final static String authSubscriptionApplicableTaxes = "/api/auth/subscription_taxes/applicable_taxes";

	// Redemptions 1.0
	public final static String authApiFetchRedemptionCode = "/api/auth/redemptions";
	public final static String authApifetchAvailableOffersOfTheUser = "/api/auth/offers";
	public final static String authApiCreateLoyaltyCheckin = "/api/auth/checkins";
	public final static String authApiFetchACheckinByExternal_uid = "/api/auth/checkins";
	public final static String authApiCreateAccessToken = "/api/auth/sso";
	public final static String authApiChangePassword = "/api/auth/users/change_password";
	public final static String authApiresetPasswordTokenOfTheUser = "/api/auth/users/reset_password_token";
	public final static String authApiEstimateLoyaltyPointsEarning = "/api/auth/loyalty_points_estimator";
	public final static String authApiPointConversionAPI = "/api/auth/conversions";
	public final static String authApiGetTheDealDetail = "/api/auth/deals/{redeemable_uuid}";
	public final static String authApiEstimatePointsEarning = "/api/auth/estimate_points";
	public final static String authApiBalanceTimelines = "/api/auth/balance_timelines";
	public final static String authApiUserEnrollment = "/api/auth/user_enrollments";
	public final static String authApiUserDisenrollment = "/api/auth/user_enrollments";
	public final static String authApiGetSSOToken = "/oauth/token";

	// POS API
	public final static String posSignUp = "/api/pos/users";
	public final static String posCheckin = "/api/pos/checkins";
	public final static String posRedemption = "/api/pos/redemptions";
	public final static String posApplicableOffers = "/api/pos/redemptions/applicable_offers";
	public final static String posActiveRedemptions = "/api/pos/users/active_redemptions";
	public final static String userLookupAndFetchBalance = "/api/pos/users/search";
	public final static String voidMultipleRedemption = "/api/pos/redemptions/multiple_destroy";
	public final static String posEcrmCheckin = "/api/pos/transactions";
	public final static String posPossibleRedemption = "/api/pos/redemptions/possible";
	public final static String posLocationConfiguration = "/api/pos/locations/configuration";
	public final static String posPayment = "/api/pos/payments";
	public final static String posRefundPayment = "/api/pos/payments/refund";
	public final static String posProgramMeta = "/api/pos/meta";
	public final static String posFetchAccountBalance = "/api/pos/users/balance";
	// OMM API
	public final static String posUserLookup = "/api/pos/users/find";
	public final static String discountLookupPOSAPI = "/api/pos/discounts/lookup";

	// Omm API
	public final static String discountBasketAdded = "/api/mobile/discounts/select";
	public final static String discountBasketAddedPOS = "/api/pos/discounts/select";
	public final static String discountBasketAddedAUTH = "/api/auth/discounts/select";
	public final static String getDiscountBasketAPI = "/api/mobile/discounts/active";
	public final static String getDiscountBasketPOS = "/api/pos/discounts/active";
	public final static String getDiscountBasketAUTH = "/api/auth/discounts/active";
	public final static String deleteBasketForUserAPI = "/api/mobile/discounts/unselect";
	public final static String deleteBasketForUserAUTH = "/api/auth/discounts/unselect";
	public final static String deleteBasketForUserPOS = "/api/pos/discounts/unselect";
	public final static String batchRedemptionPOSAPI = "/api/pos/batch_redemptions";
	public final static String batchPOSDiscountAPI = "/api/pos/discounts/lookup";
	public final static String batchRedemptionAuthAPI = "/api/auth/batch_redemptions";
	public final static String deleteBasketID = "/api/auth/discounts/unselect";
	public final static String basketUnlockPOSAPI = "/api/pos/discounts/unlock";
	public final static String autoUnlockPosAPI = "/api/pos/discounts/auto_select";
	public final static String authAutoSelect = "/api/auth/discounts/auto_select";
	public final static String posAutoSelectAPI = "api/pos/discounts/select";

	// v5 Redemption
	public final static String v5RedemptionsUsingRewardId = "/api/v5/redemptions";

	// v5 API
	public final static String V5checkins = "/api/v5/checkins";

	// Deletedeactivate
	public final static String deleteUserAPI = "/api/mobile/customers";
	public final static String deactivateUserAPI = "/api/mobile/customers/deactivate";

	// PickupMenuSTS
	public final static String menuorderingtoken = "/api/mobile/customers/ordering_token";
	public final static String MenuFetchUserProfile = "/api/customer-accounts/";

	// v2 API
	public final static String V2checkins = "/api/v2/checkins";

	// IdentityService
	public final static String identityGenerateBrandLevelToken = "/api/tokens";
	public final static String identityUserSignUp = "/api/users/sign_up";
	public final static String identityUserSignIn = "/api/users/sign_in";
	public final static String identityPunchhNewSignIn = "/api2/mobile/users/auth_sign_in";
	public final static String identityUserSignOut = "/api/users/sign_out";
	public final static String identityUserSync = "/api/sync_user";

	// Advanced Auth
	public final static String advanceAuthToken = "/api2/password_less/token";
	public final static String advanceAuthVerify = "/api2/password_less/verify";
	public final static String advanceAuthRefresh = "/api2/password_less/refresh_token";
	public final static String advanceAuthSignOut = "/api2/password_less/sign_out";

	// Basic Auth
	public final static String basicAuthSignUp = "/api2/basic_auth/signup";
	public final static String basicAuthSignIn = "/api2/basic_auth/signin";
	public final static String basicAuthSignOut = "/api2/sign_out";
	public final static String basicAuthRefresh = "/api2/refresh_token";
	public final static String basicAuthForgotPassword = "/api2/basic_auth/forgot_password";
	public final static String basicAuthResetPassword = "/api2/basic_auth/reset_password";
	public final static String basicAuthChangePassword = "/api2/basic_auth/change_password";
	public final static String updateBusinessConfig = "api/businesses/update_business_config";

	// Enqueue worker
	public final static String enqueueWorker = "/enqueue_worker";

	// Item recommendation service
	public final static String systemRecommendation = "/api/mobile/item-rec/system";
	public final static String itemRecommendation = "/api/mobile/item-rec/items";
	public final static String itemRecServiceApi = "/item-rec";
	public final static String itemRecAdminApi = "/business/";
	public final static String closeOrderOnine = "/$slugName/online_orders";
	public final static String v1Redemption = "/api/v1/redemptions";
	public final static String v1UserOffer = "/api/v1/users/offers";
	public final static String parMenuOrder = "/$slugName/menu_orders";
	public final static String getInboundwebhookFilterLogs = "/api/v1/filter_logs";
	public final static String getWebhookMessageContent = "/api/v1/request_body_by_content_id";

	public final static String islReceipt = "/receipt_details";
	public final static String paypalAgreementTokenAPI = "/api/mobile/paypal_payments/agreement_tokens";
	public final static String posPaymentStatus = "/api/pos/payments/status";
	public final static String posPaymentCard = "/api/mobile/payment_cards";

	public final static String metaV2VersionNotes = "api2/mobile/version_notes";
	public final static String metaV2MakingBatchRequest = "api2/mobile/batch_api";

	public final static String webHookAttentive = "/hooks/attentive/f4208105-ac24-45aa-9fd9-0a257ac29a41/conversations/status";
	public final static String iframePrifilledURL = "/api2/dashboard/iframe/prefilled_url";
	public final static String webhookZipline = "/hooks/zipline/";
	public final static String api2DashboardCheckinsURL = "/api2/dashboard/checkins";

	// SFMC endpoints
	public final static String fetchSFMCAccessToken = "/v2/token";
	public final static String fetchSFMCFSegmentExportDetails = "/data/v1/customobjectdata/key/";

	// Web Api
	public final static String webApi = "/api/web/";

	// Api v1
	public final static String userBalanceApiV1 = "/api/v1/users/balance";
	public final static String checkinsBalanceApiV1 = "/api/v1/checkins/balance";
	public final static String userMembershipLevelsApiV1 = "/api/v1/users/membership_levels";

	// Web Api
	public final static String apiMobileMemberShipLevel = "api/mobile/users/membership_levels";

	// challenge
	public final static String mobileApiChallenge = "/api/mobile/challenges";
	public final static String apiAuthChallenge = "/api/auth/challenges";
	public final static String apiPosChallenge = "/api/pos/challenges";

	public final static String createLisAPI = "/api2/dashboard/offers/lis";
	public final static String createQualificationCriteria_API = "/api2/dashboard/offers/qc";

	// ApiV1

	public final static String apiV1SserSignup = "/api/v1/customers.json";
	public final static String apiV1Userbalance = "/api/v1/users/balance";
	public final static String apiV1UserLogin = "/api/v1/customers/sign_in.json";
	public final static String apiV1GetRichMessages = "/api/v1/messages";
	public final static String getRedeemableListAPI = "/api2/dashboard/offers/redeemable";
	public final static String checkinsApiV1 = "/api/v1/checkins";

	// mParticle Inbound Api
	public final static String mParticeInboundSegment = "/webhook/inbound/mparticle/segment";
	public final static String mParticeInboundSegmentUser = "/webhook/inbound/mparticle/users";

	// offer api
	public final static String api2UpdateRedeemable = "/api2/dashboard/offers/redeemable";

	// ppcc api
	public final static String fetchConfig = "api/mslpos/config/fetch";
	public final static String heartbeatApi = "api/mslpos/config/heartbeat";
	public final static String provisionApi = "api/upos/location/provision";
	public final static String deprovisionApi = "api/upos/location/deprovision";
	public final static String policyList = "api/upos/policy/policy-list";
	public final static String deletePolicy = "api/upos/policy/{policyId}/delete";
	public final static String locationList = "api/upos/location";
	public final static String getConfigurations = "api/upos/config";
	public final static String configOverride = "api/upos/location/config-override";
	public final static String cancelUpdate = "api/upos/location/cancel-update";
	public final static String initiateUpdate = "api/upos/location/initiate-update";
	public final static String remoteUpgrade = "api/upos/location/remote-upgrade";
	public final static String reprovision = "api/upos/location/reprovision";
	public final static String addPolicy = "api/upos/policy/add";
	public final static String updatePolicy = "api/upos/policy/{policyId}/update";
	public final static String policyDetails = "api/upos/policy/{policyId}/details";
	public final static String apiKMAgeVerification = "/$slug/km_age_verification";
	public final static String listLocationAuditLogs = "api/upos/location/audit-logs";
	public final static String getLocationAuditLogsFilters = "api/upos/location/audit-log-filter-metadata";
	public final static String retrieveLocationAuditLogs = "api/upos/location/audit-logs/{audit_log_id}";
	public final static String listPolicyAuditLogs = "api/upos/policy/audit-logs";
	public final static String getPolicyAuditLogsFilters = "api/upos/policy/audit-log-filter-metadata";
	public final static String retrievePolicyAuditLogs = "api/upos/policy/audit-logs/{audit_log_id}";

	// packages API
	public final static String packagesList = "package";
	public final static String getDownloadPackageLink = "package/{packageId}/download";
	public final static String updatePackage = "package/{packageId}";
	public final static String publishPackage = "package";
	public final static String deletePackage = "package/{packageId}";
	public final static String getPackage = "package/{packageId}";

	// wehbook / adapter
	public final static String getWebhookStatusAPI = "/api/webhook/adapter";
	public final static String getAdapterStatusAPI = "/api/v1/adapter_webhooks";
	public final static String getWebhookConfigurationAPI = "/api/v1/business_config";
	public final static String apiV1WebhookGlobalConfig = "/api/v1/global-config";
	public final static String getWebhookLogsApi = "/api/v1/filter_logs";
	public final static String getWebhookRateLimitPerformanceLog = "/api/v1/webhook_performance_log";
	public final static String getWebhookBarGraphLogs = "/api/v1/bar_graph_logs";

	// HTTP Status Codes
	public final static int HTTP_STATUS_OK = 200;
	public final static int HTTP_STATUS_CREATED = 201;
	public final static int HTTP_STATUS_ACCEPTED = 202;
	public final static int HTTP_STATUS_NO_CONTENT = 204;
	public final static int HTTP_STATUS_BAD_REQUEST = 400;
	public final static int HTTP_STATUS_UNAUTHORIZED = 401;
	public final static int HTTP_STATUS_FORBIDDEN = 403;
	public final static int HTTP_STATUS_NOT_FOUND = 404;
	public final static int HTTP_STATUS_NOT_ACCEPTABLE = 406;
	public final static int HTTP_STATUS_CONFLICT = 409;
	public final static int HTTP_STATUS_GONE = 410;
	public final static int HTTP_STATUS_PRECONDITION_FAILED = 412;
	public final static int HTTP_STATUS_EXPECTATION_FAILED = 417;
	public final static int HTTP_STATUS_UNPROCESSABLE_ENTITY = 422;

	// unified Service API
	public final static String meApi = "/api/v1/users/me";

	// facebook social login
	public final static String facebookSocialLogin = "/api/mobile/customers/connect_with_facebook";
	public final static String Api2facebookSocialLogin = "/api2/mobile/users/connect_with_facebook";
    public final static String ApiAuthFacebookSocialLogin = "/api/auth/users/connect_with_facebook";

}