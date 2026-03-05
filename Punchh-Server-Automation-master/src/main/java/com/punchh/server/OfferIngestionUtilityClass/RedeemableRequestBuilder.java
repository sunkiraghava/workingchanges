package com.punchh.server.OfferIngestionUtilityClass;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;

import java.util.*;

public class RedeemableRequestBuilder {

	// ---------- Top-Level Fields ----------
	private String name;
	private String externalId = "";
	private String alternateLocaleName;
	private String note;
	private boolean allowForSupportGifting = true;
	private boolean availableAsTemplate = false;
	private boolean earlyAccess = true;
	private boolean distributable = false;
	private boolean distributableToAllUsers = false;
	private String segmentDefinitionId = null;
	private boolean autoApplicable = true;

	// ---------- Nested Objects ----------
	private ReceiptRule receiptRule;
	private boolean activateNow = false;
	private String startTime;
	private boolean indefinetely = false;
	private Object expiryDays = null;
	private String endTime;
	private String timezone = "Asia/Kolkata";
	private int remindBefore = 3;
	private String discountChannel = "all";
	private int points = 12;
	private int redemptionCodeExpiryMins = 3;
	private boolean applicableAsLoyaltyRedemption = false;
	private boolean expireRedemptionCodeWithRewardEndDate = false;
	private Template template;
	private LagDuration lagDuration;
	private RecurrenceSchedule recurrenceSchedule;
	private String effectiveLocation = null;
	private String metaData;
    private final Map<String, Object> dynamicFields = new LinkedHashMap<>();


	// ---------- Builder Setters ----------
	public RedeemableRequestBuilder withName(String name) {
		this.name = name;
		return this;
	}

	public RedeemableRequestBuilder withNote(String note) {
		this.note = note;
		return this;
	}

	public RedeemableRequestBuilder withAlternateLocaleName(String name) {
		this.alternateLocaleName = name;
		return this;
	}

	public RedeemableRequestBuilder withReceiptRule(ReceiptRule rule) {
		this.receiptRule = rule;
		return this;
	}

	public RedeemableRequestBuilder withTemplate(String redemptionMessage, String shortPrompt, String standardPrompt) {
		this.template = Template.builder().redemptionMessage(redemptionMessage).shortPrompt(shortPrompt)
				.standardPrompt(standardPrompt).build();
		return this;
	}

	public RedeemableRequestBuilder withLagDuration(int value, String units) {
		this.lagDuration = LagDuration.builder().value(value).units(units).build();
		return this;
	}

	public RedeemableRequestBuilder withRecurrenceSchedule(int occurrences, int daysDistance) {
		this.recurrenceSchedule = RecurrenceSchedule.builder().occurrences(occurrences).daysDistance(daysDistance)
				.build();
		return this;
	}

	public RedeemableRequestBuilder withStartAndEndTime(String start, String end) {
		this.startTime = start;
		this.endTime = end;
		return this;
	}

	public RedeemableRequestBuilder withMetaData(String metaData) {
		this.metaData = metaData;
		return this;
	}
	
	// ---------- Dynamic Field Handling ----------
    public RedeemableRequestBuilder addField(String key, Object value) {
        dynamicFields.put(key, value);
        return this;
    }

    public RedeemableRequestBuilder removeField(String key) {
        dynamicFields.remove(key);
        return this;
    }


	// ---------- Build JSON ----------
	public String buildJson() throws JsonProcessingException {
		if (name == null || receiptRule == null) {
			throw new IllegalArgumentException("Name and ReceiptRule are mandatory");
		}

		RedeemableData data = RedeemableData.builder().name(name).externalId(externalId)
				.alternateLocaleName(alternateLocaleName).note(note).allowForSupportGifting(allowForSupportGifting)
				.availableAsTemplate(availableAsTemplate).earlyAccess(earlyAccess).distributable(distributable)
				.distributableToAllUsers(distributableToAllUsers).segmentDefinitionId(segmentDefinitionId)
				.autoApplicable(autoApplicable).receiptRule(receiptRule).activateNow(activateNow).startTime(startTime)
				.indefinetely(indefinetely).expiryDays(expiryDays).endTime(endTime).timezone(timezone)
				.remindBefore(remindBefore).discountChannel(discountChannel).points(points)
				.redemptionCodeExpiryMins(redemptionCodeExpiryMins)
				.applicableAsLoyaltyRedemption(applicableAsLoyaltyRedemption)
				.expireRedemptionCodeWithRewardEndDate(expireRedemptionCodeWithRewardEndDate).template(template)
				.lagDuration(lagDuration).recurrenceSchedule(recurrenceSchedule).effectiveLocation(effectiveLocation)
				.metaData(metaData).build();

		Map<String, Object> root = new HashMap<>();
		root.put("data", Collections.singletonList(data));

		ObjectMapper mapper = new ObjectMapper();
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
	}

	// ---------- Inner Model Classes ----------
	@Data
	@Builder
	public static class RedeemableData {
		private String name;
		private String externalId;
		private String alternateLocaleName;
		private String note;
		private boolean allowForSupportGifting;
		private boolean availableAsTemplate;
		private boolean earlyAccess;
		private boolean distributable;
		private boolean distributableToAllUsers;
		private String segmentDefinitionId;
		private boolean autoApplicable;
		private ReceiptRule receiptRule;
		private boolean activateNow;
		private String startTime;
		private boolean indefinetely;
		private Object expiryDays;
		private String endTime;
		private String timezone;
		private int remindBefore;
		private String discountChannel;
		private int points;
		private int redemptionCodeExpiryMins;
		private boolean applicableAsLoyaltyRedemption;
		private boolean expireRedemptionCodeWithRewardEndDate;
		private Template template;
		private LagDuration lagDuration;
		private RecurrenceSchedule recurrenceSchedule;
		private String effectiveLocation;
		private String metaData;
	}

	@Data
	@Builder
	public static class ReceiptRule {
		private String qualifierType;
		private String redeemingCriterionId;
		private Object discountAmount;
		private RedeemingCriterion redeemingCriterion;
	}

	@Data
	@Builder
	public static class RedeemingCriterion {
		private String name;
		private String externalId;
		private double amountCap;
		private int percentageOfProcessedAmount;
		private String qcProcessingFunction;
		private String roundingRule;
		private int maxDiscountUnits;
		private double unitDiscount;
		private double minimumUnitRate;
		private double targetPrice;
		private Object effectiveLocation;
		private boolean stackDiscounting;
		private boolean reuseQualifyingItems;
		private List<LineItemFilter> lineItemFilters;
		private boolean enableMenuItemAggregator;
		private AggregatorGroupingAttributes aggregatorGroupingAttributes;
		private List<ItemQualifier> itemQualifiers;
		private List<ReceiptQualifier> receiptQualifiers;
	}

	@Data
	@Builder
	public static class LineItemFilter {
		private String lineItemSelectorId;
		private String processingMethod;
		private int quantity;
	}

	@Data
	@Builder
	public static class AggregatorGroupingAttributes {
		private boolean itemName;
		private boolean itemId;
		private boolean itemMajorGroup;
		private boolean itemFamily;
		private boolean lineItemType;
	}

	@Data
	@Builder
	public static class ItemQualifier {
		private String expressionType;
		private String lineItemSelectorId;
		private Object netValue;
	}

	@Data
	@Builder
	public static class ReceiptQualifier {
		private String attribute;
		private String operator;
		private Object value;
	}

	@Data
	@Builder
	public static class Template {
		private String redemptionMessage;
		private String shortPrompt;
		private String standardPrompt;
	}

	@Data
	@Builder
	public static class LagDuration {
		private int value;
		private String units;
	}

	@Data
	@Builder
	public static class RecurrenceSchedule {
		private int occurrences;
		private int daysDistance;
	}

	// ---------- Example Usage ----------
	public static void main(String[] args) throws Exception {
		// Build inner redeeming criterion (nested object)
		RedeemingCriterion criterion = RedeemingCriterion.builder()
				.name("AutomationQC_API_" + System.currentTimeMillis()).externalId("").amountCap(10.0)
				.percentageOfProcessedAmount(1).qcProcessingFunction("sum_amounts").roundingRule("ceil")
				.maxDiscountUnits(2).unitDiscount(10).minimumUnitRate(0.01).targetPrice(1).stackDiscounting(false)
				.reuseQualifyingItems(false)
				.lineItemFilters(
						List.of(LineItemFilter.builder().lineItemSelectorId("20d5af22-f834-4cfb-92ea-64341fa2fa47")
								.processingMethod("max_price").quantity(5).build()))
				.enableMenuItemAggregator(false)
				.aggregatorGroupingAttributes(AggregatorGroupingAttributes.builder().itemName(false).itemId(false)
						.itemMajorGroup(false).itemFamily(false).lineItemType(false).build())
				.itemQualifiers(List.of(ItemQualifier.builder().expressionType("line_item_exists")
						.lineItemSelectorId("20d5af22-f834-4cfb-92ea-64341fa2fa47").netValue(null).build()))
				.receiptQualifiers(
						List.of(ReceiptQualifier.builder().attribute("total_amount").operator(">=").value(10).build(),
								ReceiptQualifier.builder().attribute("receipt_hour").operator("in").value(1).build()))
				.build();

		ReceiptRule receiptRule = ReceiptRule.builder().qualifierType("").redeemingCriterionId("1").discountAmount(null)
				.redeemingCriterion(criterion).build();

		String json = new RedeemableRequestBuilder().withName("AutomationRedeemable_API_" + System.currentTimeMillis())
				.withAlternateLocaleName("Test description").withNote("Notes here").withReceiptRule(receiptRule)
				.withTemplate("Countdown Message text", "Short Prompt text", "Standard Prompt text")
				.withLagDuration(2, "days").withRecurrenceSchedule(10, 2)
				.withStartAndEndTime("2028-06-25T23:59:59", "2026-12-13T20:53:18").withMetaData("meta data")
				.buildJson();

		System.out.println(json);
		
		
		
		
		
	}
}
