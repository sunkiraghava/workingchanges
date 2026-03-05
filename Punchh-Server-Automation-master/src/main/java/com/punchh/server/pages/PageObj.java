package com.punchh.server.pages;

import org.openqa.selenium.WebDriver;

import com.punchh.server.apiConfig.Endpoints;
import com.punchh.server.apiConfig.LineItemSelectorsJsonCreation;
import com.punchh.server.apiConfig.ProdEndpoints;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.GoogleServiceAccountJwtGenerator;
import com.punchh.server.utilities.MongoDBUtils;
import com.punchh.server.utilities.ReadData;
import com.punchh.server.utilities.SingletonDBUtils;
import com.punchh.server.utilities.Utilities;

import Support.ConfigurationClass;
import Support.GetEnvDetails;

public class PageObj {

	public WebDriver driver;
	private IframeLoginSignUpPage iframeSingUpPage;
	private InstanceDashboard instanceDashboardPage;
	private GuestTimelinePage guestTimelinePage;
	private CampaignsPage campaignspage;
	private DataExportPage dataExportPage;
	private SegmentsPage segmentsPage;
	private SsoLoginSignUpPage ssoLoginSingupPage;
	private MenuPage menupage;
	private SignupCampaignPage signupcampaignpage;
	private CampaignSplitPage campaignsplitpage;
	private SchedulesPage schedulespage;
	private Endpoints endpoints;
	private ProdEndpoints prodEndpoints;
	private JourneysPage journeysPage;
	private SchedulePage schedulePage;
	private DashboardPage dashboardpage;
	private AccountHistoryPage accounthistoryPage;
	private ForceRedemptionPage forceredemptionPage;
	private GiftCardsPage giftcardsPage;
	private PaymentReportPage paymentreportPage;
	private ConsolePage consolePage;
	private ApiUtils apiUtils;
	private AdminUsersPage adminUsersPage;
	private EClubGuestPage eClubGuestPage;
	private RedemptionLogPage redemptionLogPage;
	private AwaitingMigrationPage awaitingMigrationPage;
	private SegmentsBetaPage segmentsBetaPage;
	private LocationPage locationPage;
	private EarningPage earningPage;
	private MobileConfigurationPage mobileconfigurationPage;
	private CampaignsBetaPage campaignsbetaPage;
	private CampaignSetPage campaignsetPage;
	private QualificationCriteriaPage qualificationcriteriapage;
	private LineItemSelectorPage lineItemSelectorPage;
	private LapsedGuestPage lapsedguestPage;
	private SettingsPage settingsPage;
	private OAuthAppPage oAuthAppPage;
	private RedeemablesPage redeemablesPage;
	private EmailTemplatePage emailTemplatePage;
	private RedeemablePage redeemablePage;
	private AccountDeletionPage Accountdeletionpage;
	private PunchhPickupPage punchhpickuppage;
	private CockpitEarningPage cockpitearningPage;
	private NotificationTemplatePage notificationTemplatePage;
	private PosStatsPage posStatsPage;
	private FeedbackPage feedbackPage;
	private WhitelabelPage whitelabelPage;
	private CockpitGuestPage cockpitGuestPage;
	private IframeConfigurationPage iframeConfigurationPage;
	private RedemptionsPage redemptionsPage;
	private NewCamHomePage newCamHomePage;
	private IntegrationServicesPage integrationServicesPage;
	private GetEnvDetails getEnvDetails;
	private ConfigurationClass configurationClass;
	private IframeFieldValidationPage iframeSingUpValidationPage;
	private ItemCatalogPage itemCatalogPage;
	private SubscriptionPlansPage subscriptionPlansPage;
	private CockpitRedemptionsPage cockpitPage;
	private SidekiqPage sidekiqPage;
	private VerificationsPortalPage verificationsPortalPage;
	private CockpitLocationPage cockpitLocationPage;

	private BarcodeLookupPage barcodeLookupPage;
	private PayWithPayPalPage payWithPayPalPage;
	private WorldPayPaymentPage worldPayPaymentPage;

	private IntellumPage intellumPage;
	private PpccPolicyPage ppccPolicyPage;
	private PpccPackagePage ppccPackagePage;
	private PpccPolicyAuditLogs ppccPolicyAuditLogs;
	private PpccSettingsPage ppccSettingsPage;
	private PpccUtilities ppccUtilities;
	private UnifiedLoginPage unifiedLoginPage;
	private UnifiedDashboardPage unifiedDashboardPage;

	private PosIntegrationPage posIntegrationPage;

	private ReadData readData;
	private SingletonDBUtils singletonDBObj;
	private GoogleServiceAccountJwtGenerator googleServiceAccountJwtGenerator;
	private WebhookManagerPage webhookManagerObj;
	private LineItemSelectorsJsonCreation lineItemSelectorsJsonCreation;
	private NewSegmentHomePage newSegmentHomePage;
	private AllBusinessPage allbusinessPage;
	private OlfDBQueriesPage olfdbQueriesPage;
	private DestructionPage destructionPage;
	private CockpitTableauPage cockpitTableauPage;
	private PpccLocationPage ppccLocationPage;
	private ParPaymentTokenGenPage parPaymentTokenGenPage;
	private MongoDBUtils mongoDBUtils;
	private CockpitPhysicalCardPage cockpitPhysicalCardPage;

	private PpccLocationAuditLogPage ppccLocationAuditLogPage;
	private SuperadminMassCampaignPage superadminMassCampaignPage;
	private CockpitDashboardMiscPage cockpitDashboardMiscPage;
	private ContentLibraryPage contentLibraryPage;
	private GamesPage gamesPage;
	private MembershipsPage membershipsPage;
	private SurveysPage surveysPage;
	private CockpitRedemptionsCodePage cockpitRedemptionsCodePage;
	private CampaignPerformancePage campaignPerformancePage;
	private AdminRolesPage adminRolesPage;
	private GamingLevelPage gamingLevelPage;
	private ChromeNetworkCallPage chromeNetworkCallPage;
	private SubscriptionTaxationPage subscriptionTaxationPage;
	private PARCommerceIframePage parCommerceIframePage;
	private CockpitTransferPage cockpitTransferPage;
	private DynamicRewardsPage dynamicRewardsPage;
	public CollectibleCategoryPage collectibleCategoryPage;

	private Utilities utils;

	public PageObj(WebDriver driver) {
		this.driver = driver;
	}

	public PageObj() {
	}

	// For Ui classes
	public IframeLoginSignUpPage iframeSingUpPage() {
		if (iframeSingUpPage == null) {
			iframeSingUpPage = new IframeLoginSignUpPage(driver);
		}
		return iframeSingUpPage;
	}

	public InstanceDashboard instanceDashboardPage() {
		if (instanceDashboardPage == null) {
			instanceDashboardPage = new InstanceDashboard(driver);
		}
		return instanceDashboardPage;
	}

	public GuestTimelinePage guestTimelinePage() {

		if (guestTimelinePage == null) {
			guestTimelinePage = new GuestTimelinePage(driver);
		}
		return guestTimelinePage;
	}

	public CampaignsPage campaignspage() {
		if (campaignspage == null) {
			campaignspage = new CampaignsPage(driver);
		}
		return campaignspage;
	}

	public DataExportPage dataExportPage() {
		if (dataExportPage == null) {
			dataExportPage = new DataExportPage(driver);
		}
		return dataExportPage;
	}

	public SegmentsPage segmentsPage() {
		if (segmentsPage == null) {
			segmentsPage = new SegmentsPage(driver);
		}
		return segmentsPage;
	}

	public SsoLoginSignUpPage ssoLoginSingupPage() {
		if (ssoLoginSingupPage == null) {
			ssoLoginSingupPage = new SsoLoginSignUpPage(driver);
		}
		return ssoLoginSingupPage;
	}

	public MenuPage menupage() {

		if (menupage == null) {
			menupage = new MenuPage(driver);
		}
		return menupage;
	}

	public SignupCampaignPage signupcampaignpage() {

		if (signupcampaignpage == null) {
			signupcampaignpage = new SignupCampaignPage(driver);
		}
		return signupcampaignpage;
	}

	public CampaignSplitPage campaignsplitpage() {

		if (campaignsplitpage == null) {
			campaignsplitpage = new CampaignSplitPage(driver);
		}
		return campaignsplitpage;
	}

	public SchedulesPage schedulespage() {

		if (schedulespage == null) {
			schedulespage = new SchedulesPage(driver);
		}
		return schedulespage;
	}

	public SchedulePage schedulePage() {

		if (schedulePage == null) {
			schedulePage = new SchedulePage(driver);
		}
		return schedulePage;
	}

	public DashboardPage dashboardpage() {

		if (dashboardpage == null) {
			dashboardpage = new DashboardPage(driver);
		}
		return dashboardpage;
	}

	public AccountHistoryPage accounthistoryPage() {

		if (accounthistoryPage == null) {
			accounthistoryPage = new AccountHistoryPage(driver);
		}
		return accounthistoryPage;
	}

	public ForceRedemptionPage forceredemptionPage() {

		if (forceredemptionPage == null) {
			forceredemptionPage = new ForceRedemptionPage(driver);
		}
		return forceredemptionPage;
	}

	public ConsolePage consolePage() {

		if (consolePage == null) {
			consolePage = new ConsolePage(driver);
		}
		return consolePage;
	}

	public JourneysPage journeysPage() {
		if (journeysPage == null) {
			journeysPage = new JourneysPage(driver);
		}
		return journeysPage;
	}

	public AdminUsersPage AdminUsersPage() {
		if (adminUsersPage == null) {
			adminUsersPage = new AdminUsersPage(driver);
		}
		return adminUsersPage;
	}

	public GiftCardsPage giftcardsPage() {
		if (giftcardsPage == null) {
			giftcardsPage = new GiftCardsPage(driver);
		}
		return giftcardsPage;
	}

	public PaymentReportPage paymentreportPage() {
		if (paymentreportPage == null) {
			paymentreportPage = new PaymentReportPage(driver);
		}
		return paymentreportPage;
	}

	public EClubGuestPage EClubGuestPage() {
		if (eClubGuestPage == null)
			eClubGuestPage = new EClubGuestPage(driver);
		return eClubGuestPage;
	}

	public RedemptionLogPage RedemptionLogPage() {
		if (redemptionLogPage == null)
			redemptionLogPage = new RedemptionLogPage(driver);
		return redemptionLogPage;
	}

	public EarningPage earningPage() {
		if (earningPage == null)
			earningPage = new EarningPage(driver);
		return earningPage;
	}

	public MobileConfigurationPage mobileconfigurationPage() {
		if (mobileconfigurationPage == null)
			mobileconfigurationPage = new MobileConfigurationPage(driver);
		return mobileconfigurationPage;
	}

	public AwaitingMigrationPage awaitingMigrationPage() {
		if (awaitingMigrationPage == null) {
			awaitingMigrationPage = new AwaitingMigrationPage(driver);
		}
		return awaitingMigrationPage;
	}

	public SegmentsBetaPage segmentsBetaPage() {
		if (segmentsBetaPage == null) {
			segmentsBetaPage = new SegmentsBetaPage(driver);
		}
		return segmentsBetaPage;
	}

	public LocationPage locationPage() {

		if (locationPage == null) {
			locationPage = new LocationPage(driver);
		}
		return locationPage;
	}

	public CampaignsBetaPage campaignsbetaPage() {

		if (campaignsbetaPage == null) {
			campaignsbetaPage = new CampaignsBetaPage(driver);
		}
		return campaignsbetaPage;
	}

	public CampaignSetPage campaignsetPage() {

		if (campaignsetPage == null) {
			campaignsetPage = new CampaignSetPage(driver);
		}
		return campaignsetPage;
	}

	public QualificationCriteriaPage qualificationcriteriapage() {
		if (qualificationcriteriapage == null) {
			qualificationcriteriapage = new QualificationCriteriaPage(driver);
		}
		return qualificationcriteriapage;
	}

	public LineItemSelectorPage lineItemSelectorPage() {
		if (lineItemSelectorPage == null) {
			lineItemSelectorPage = new LineItemSelectorPage(driver);
		}
		return lineItemSelectorPage;
	}

	public LapsedGuestPage lapsedguestPage() {

		if (lapsedguestPage == null) {
			lapsedguestPage = new LapsedGuestPage(driver);
		}
		return lapsedguestPage;
	}

	public SettingsPage settingsPage() {
		if (settingsPage == null)
			settingsPage = new SettingsPage(driver);
		return settingsPage;
	}

	public OAuthAppPage oAuthAppPage() {
		if (oAuthAppPage == null)
			oAuthAppPage = new OAuthAppPage(driver);
		return oAuthAppPage;
	}

	public RedeemablesPage redeemablesPage() {
		if (redeemablesPage == null)
			redeemablesPage = new RedeemablesPage(driver);
		return redeemablesPage;
	}

	public EmailTemplatePage emailTemplatePage() {
		if (emailTemplatePage == null)
			emailTemplatePage = new EmailTemplatePage(driver);
		return emailTemplatePage;
	}

	public RedeemablePage redeemablePage() {
		if (redeemablePage == null)
			redeemablePage = new RedeemablePage(driver);
		return redeemablePage;
	}

	public PunchhPickupPage punchhpickuppage() {
		if (punchhpickuppage == null) {
			punchhpickuppage = new PunchhPickupPage(driver);
		}
		return punchhpickuppage;
	}

	public PosStatsPage posStatsPage() {
		if (posStatsPage == null) {
			posStatsPage = new PosStatsPage(driver);
		}
		return posStatsPage;
	}

	public RedemptionsPage redemptionsPage() {
		if (redemptionsPage == null) {
			redemptionsPage = new RedemptionsPage(driver);
		}
		return redemptionsPage;
	}

	public IframeFieldValidationPage iframeSingUpValidationPageClass() {
		if (iframeSingUpValidationPage == null)
			iframeSingUpValidationPage = new IframeFieldValidationPage(driver);
		return iframeSingUpValidationPage;
	}

	public SubscriptionPlansPage subscriptionPlansPage() {
		if (subscriptionPlansPage == null) {
			subscriptionPlansPage = new SubscriptionPlansPage(driver);
		}
		return subscriptionPlansPage;
	}

	public AccountDeletionPage Accountdeletionpage() {
		if (Accountdeletionpage == null) {
			Accountdeletionpage = new AccountDeletionPage(driver);
		}
		return Accountdeletionpage;
	}

	public CockpitRedemptionsPage cockpitRedemptionsPage() {
		if (cockpitPage == null) {
			cockpitPage = new CockpitRedemptionsPage(driver);
		}
		return cockpitPage;
	}

	public ItemCatalogPage itemCatalogPage() {
		if (itemCatalogPage == null) {
			itemCatalogPage = new ItemCatalogPage(driver);
		}
		return itemCatalogPage;
	}

	public SidekiqPage sidekiqPage() {
		if (sidekiqPage == null) {
			sidekiqPage = new SidekiqPage(driver);
		}
		return sidekiqPage;
	}

	public VerificationsPortalPage verificationsPortalPage() {
		if (verificationsPortalPage == null) {
			verificationsPortalPage = new VerificationsPortalPage(driver);
		}
		return verificationsPortalPage;
	}

	public CockpitEarningPage cockpitearningPage() {
		if (cockpitearningPage == null) {
			cockpitearningPage = new CockpitEarningPage(driver);
		}
		return cockpitearningPage;
	}

	public NotificationTemplatePage notificationTemplatePage() {
		if (notificationTemplatePage == null) {
			notificationTemplatePage = new NotificationTemplatePage(driver);
		}
		return notificationTemplatePage;
	}

	public FeedbackPage feedbackPage() {
		if (feedbackPage == null) {
			feedbackPage = new FeedbackPage(driver);
		}
		return feedbackPage;
	}

	public WhitelabelPage whitelabelPage() {
		if (whitelabelPage == null) {
			whitelabelPage = new WhitelabelPage(driver);
		}
		return whitelabelPage;
	}

	public CockpitGuestPage cockpitGuestPage() {
		if (cockpitGuestPage == null) {
			cockpitGuestPage = new CockpitGuestPage(driver);
		}
		return cockpitGuestPage;
	}

	public CockpitLocationPage cockpitLocationPage() {
		if (cockpitLocationPage == null) {
			cockpitLocationPage = new CockpitLocationPage(driver);
		}
		return cockpitLocationPage;
	}

	public IframeConfigurationPage iframeConfigurationPage() {
		if (iframeConfigurationPage == null) {
			iframeConfigurationPage = new IframeConfigurationPage(driver);
		}
		return iframeConfigurationPage;
	}

	public BarcodeLookupPage barcodelookup() {

		if (barcodeLookupPage == null) {
			barcodeLookupPage = new BarcodeLookupPage(driver);
		}
		return barcodeLookupPage;
	}

	public PayWithPayPalPage payWithPayPalPage() {

		if (payWithPayPalPage == null) {
			payWithPayPalPage = new PayWithPayPalPage(driver);
		}
		return payWithPayPalPage;
	}

	public IntellumPage intellumPage() {
		if (intellumPage == null) {
			intellumPage = new IntellumPage(driver);
		}
		return intellumPage;
	}

	public PpccPolicyPage ppccPolicyPage() {

		if (ppccPolicyPage == null) {
			ppccPolicyPage = new PpccPolicyPage(driver);
		}
		return ppccPolicyPage;
	}

	public UnifiedDashboardPage unifiedDashboardPage() {

		if (unifiedDashboardPage == null) {
			unifiedDashboardPage = new UnifiedDashboardPage(driver);
		}
		return unifiedDashboardPage;
	}

	public PpccSettingsPage ppccSettingsPage() {

		if (ppccSettingsPage == null) {
			ppccSettingsPage = new PpccSettingsPage(driver);
		}
		return ppccSettingsPage;
	}

	public PpccPackagePage ppccPackagePage() {

		if (ppccPackagePage == null) {
			ppccPackagePage = new PpccPackagePage(driver);
		}
		return ppccPackagePage;
	}

	public PpccPolicyAuditLogs ppccPolicyAuditLogs() {

		if (ppccPolicyAuditLogs == null) {
			ppccPolicyAuditLogs = new PpccPolicyAuditLogs(driver);
		}
		return ppccPolicyAuditLogs;
	}

	public PpccUtilities ppccUtilities() {
		if (ppccUtilities == null) {
			ppccUtilities = new PpccUtilities(driver);
		}
		return ppccUtilities;
	}

	public PosIntegrationPage posIntegrationPage() {

		if (posIntegrationPage == null) {
			posIntegrationPage = new PosIntegrationPage(driver);
		}
		return posIntegrationPage;
	}

	public ReadData readData() {

		if (readData == null) {
			readData = new ReadData();
		}
		return readData;
	}

	public NewCamHomePage newCamHomePage() {

		if (newCamHomePage == null) {
			newCamHomePage = new NewCamHomePage(driver);
		}
		return newCamHomePage;
	}

	public IntegrationServicesPage integrationServicesPage() {

		if (integrationServicesPage == null) {
			integrationServicesPage = new IntegrationServicesPage(driver);
		}
		return integrationServicesPage;
	}

	public OlfDBQueriesPage olfdbQueriesPage() {

		if (olfdbQueriesPage == null) {
			olfdbQueriesPage = new OlfDBQueriesPage(driver);
		}
		return olfdbQueriesPage;
	}

	public DestructionPage destructionPage() {

		if (destructionPage == null) {
			destructionPage = new DestructionPage(driver);
		}
		return destructionPage;
	}

	public Utilities utils() {

		if (utils == null) {
			utils = new Utilities(driver);
		}
		return utils;
	}

	public NewSegmentHomePage newSegmentHomePage() {

		if (newSegmentHomePage == null) {
			newSegmentHomePage = new NewSegmentHomePage(driver);
		}
		return newSegmentHomePage;
	}

	public AllBusinessPage allbusinessPage() {
		if (allbusinessPage == null) {
			allbusinessPage = new AllBusinessPage(driver);
		}
		return allbusinessPage;
	}

	public CockpitTableauPage cockpitTableauPage() {
		if (cockpitTableauPage == null) {
			cockpitTableauPage = new CockpitTableauPage(driver);
		}
		return cockpitTableauPage;
	}

	public PpccLocationPage ppccLocationPage() {

		if (ppccLocationPage == null) {
			ppccLocationPage = new PpccLocationPage(driver);
		}
		return ppccLocationPage;
	}

	public ParPaymentTokenGenPage parPaymentTokenGenPage() {
		if (parPaymentTokenGenPage == null) {
			parPaymentTokenGenPage = new ParPaymentTokenGenPage(driver);
		}
		return parPaymentTokenGenPage;
	}

	public MongoDBUtils mongoDBUtils() {
		if (mongoDBUtils == null) {
			mongoDBUtils = new MongoDBUtils();
		}
		return mongoDBUtils;
	}

	public CockpitPhysicalCardPage cockpitPhysicalCardPage() {
		if (cockpitPhysicalCardPage == null) {
			cockpitPhysicalCardPage = new CockpitPhysicalCardPage(driver);
		}
		return cockpitPhysicalCardPage;
	}

	public PpccLocationAuditLogPage ppccLocationAuditLogPage() {

		if (ppccLocationAuditLogPage == null) {
			ppccLocationAuditLogPage = new PpccLocationAuditLogPage(driver);
		}
		return ppccLocationAuditLogPage;
	}

	public SuperadminMassCampaignPage superadminMassCampaignPage() {

		if (superadminMassCampaignPage == null) {
			superadminMassCampaignPage = new SuperadminMassCampaignPage(driver);
		}
		return superadminMassCampaignPage;
	}

	public WorldPayPaymentPage worldPayPaymentPage() {

		if (worldPayPaymentPage == null) {
			worldPayPaymentPage = new WorldPayPaymentPage(driver);
		}
		return worldPayPaymentPage;
	}

	public CockpitDashboardMiscPage cockpitDashboardMiscPage() {

		if (cockpitDashboardMiscPage == null) {
			cockpitDashboardMiscPage = new CockpitDashboardMiscPage(driver);
		}
		return cockpitDashboardMiscPage;
	}

	public ContentLibraryPage contentLibraryPage() {

		if (contentLibraryPage == null) {
			contentLibraryPage = new ContentLibraryPage(driver);
		}
		return contentLibraryPage;
	}

	public MembershipsPage membershipsPage() {

		if (membershipsPage == null) {
			membershipsPage = new MembershipsPage(driver);
		}
		return membershipsPage;
	}

	public SurveysPage surveysPage() {

		if (surveysPage == null) {
			surveysPage = new SurveysPage(driver);
		}
		return surveysPage;
	}

	public CockpitRedemptionsCodePage cockpitRedemptionsCodePage() {
		if (cockpitRedemptionsCodePage == null) {
			cockpitRedemptionsCodePage = new CockpitRedemptionsCodePage(driver);
		}
		return cockpitRedemptionsCodePage;
	}

	public CampaignPerformancePage campaignPerformancePage() {
		if (campaignPerformancePage == null) {
			campaignPerformancePage = new CampaignPerformancePage(driver);
		}
		return campaignPerformancePage;
	}

	public GoogleServiceAccountJwtGenerator googleServiceAccountJwtGenerator() {
		if (googleServiceAccountJwtGenerator == null) {
			googleServiceAccountJwtGenerator = new GoogleServiceAccountJwtGenerator(driver);
		}
		return googleServiceAccountJwtGenerator;
	}

	public WebhookManagerPage webhookManagerPage() {
		if (webhookManagerObj == null) {
			webhookManagerObj = new WebhookManagerPage(driver);
		}
		return webhookManagerObj;
	}

	public GamesPage gamesPage() {
		if (gamesPage == null) {
			gamesPage = new GamesPage(driver);
		}
		return gamesPage;
	}

	public AdminRolesPage adminRolesPage() {
		if (adminRolesPage == null) {
			adminRolesPage = new AdminRolesPage(driver);
		}
		return adminRolesPage;
	}

	public DynamicRewardsPage dynamicRewardsPage() {
		if (dynamicRewardsPage == null) {
			dynamicRewardsPage = new DynamicRewardsPage(driver);
		}
		return dynamicRewardsPage;
	}

	// ***********************************************************//

	// For Api classes (do not pass driver instance)
	public Endpoints endpoints() {
		if (endpoints == null) {
			endpoints = new Endpoints();
		}
		return endpoints;
	}

	public ProdEndpoints prodEndpoints() {
		if (prodEndpoints == null) {
			prodEndpoints = new ProdEndpoints();
		}
		return prodEndpoints;
	}

	public ApiUtils apiUtils() {
		if (apiUtils == null) {
			apiUtils = new ApiUtils();
		}
		return apiUtils;
	}

	public GetEnvDetails getEnvDetails() {
		if (getEnvDetails == null) {
			getEnvDetails = new GetEnvDetails();
		}
		return getEnvDetails;
	}

	public ConfigurationClass configurationClass() {
		if (configurationClass == null) {
			configurationClass = new ConfigurationClass();
		}
		return configurationClass;
	}

	public SingletonDBUtils singletonDBUtilsObj() {
		if (singletonDBObj == null) {
			singletonDBObj = new SingletonDBUtils();
		}
		return singletonDBObj;
	}

	public LineItemSelectorsJsonCreation lineItemSelectorsJsonCreation() {
		if (lineItemSelectorsJsonCreation == null) {
			lineItemSelectorsJsonCreation = new LineItemSelectorsJsonCreation();
		}
		return lineItemSelectorsJsonCreation;
	}

	public GamingLevelPage gamingLevelPage() {
		if (gamingLevelPage == null) {
			gamingLevelPage = new GamingLevelPage(driver);
		}
		return gamingLevelPage;
	}

	public ChromeNetworkCallPage chromeNetworkCallPage() {
		if (chromeNetworkCallPage == null) {
			chromeNetworkCallPage = new ChromeNetworkCallPage(driver);
		}
		return chromeNetworkCallPage;
	}

	public SubscriptionTaxationPage subscriptionTaxationPage() {

		if (subscriptionTaxationPage == null) {
			subscriptionTaxationPage = new SubscriptionTaxationPage(driver);
		}
		return subscriptionTaxationPage;
	}

	public PARCommerceIframePage parCommerceIframePage() {

		if (parCommerceIframePage == null) {
			parCommerceIframePage = new PARCommerceIframePage(driver);
		}
		return parCommerceIframePage;
	}

	public CockpitTransferPage cockpitTransferPage() {

		if (cockpitTransferPage == null) {
			cockpitTransferPage = new CockpitTransferPage(driver);
		}
		return cockpitTransferPage;

	}

	public UnifiedLoginPage unifiedLoginPage() {

		if (unifiedLoginPage == null) {
			unifiedLoginPage = new UnifiedLoginPage(driver);
		}
		return unifiedLoginPage;
	}

	public CollectibleCategoryPage collectibleCategoryPage() {
	    if (collectibleCategoryPage == null) {
	        collectibleCategoryPage = new CollectibleCategoryPage(driver);
	    }
	    return collectibleCategoryPage;
	}

}