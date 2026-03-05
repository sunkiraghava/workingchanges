package com.punchh.server.api.payloadbuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.punchh.server.apimodel.qc.QCData;
import com.punchh.server.apimodel.redeemable.RedeemableData;
import com.punchh.server.apimodel.redeemable.RedeemableLagDuration;
import com.punchh.server.apimodel.redeemable.RedeemableReceiptRule;
import com.punchh.server.apimodel.redeemable.RedeemableRecurrenceSchedule;
import com.punchh.server.apimodel.redeemable.RedeemableRequest;
import com.punchh.server.apimodel.redeemable.RedeemableTemplate;
import com.punchh.server.apimodel.redeemable.RedeemableTranslation;

import lombok.Getter;

@Getter
public class RedeemablePayloadBuilder {

	private final List<RedeemableData> dataList = new ArrayList<>();
	private RedeemableData.RedeemableDataBuilder dataBuilder;

	private final List<RedeemableTranslation> currentAlternateLocaleNames = new ArrayList<>();
	private final List<RedeemableTranslation> currentAlternateLocaleDescriptions = new ArrayList<>();

	private final Map<String, Consumer<Boolean>> booleanFieldSetters = new HashMap<>();
	private ObjectMapper mapper;

	/** Start a new RedeemableData object */
	public RedeemablePayloadBuilder startNewData() {
		dataBuilder = RedeemableData.builder();
		currentAlternateLocaleNames.clear();
		currentAlternateLocaleDescriptions.clear();

		// Prepare nullable boolean setters
		booleanFieldSetters.clear();
		booleanFieldSetters.put("allow_for_support_gifting", dataBuilder::allow_for_support_gifting);
		booleanFieldSetters.put("available_as_template", dataBuilder::available_as_template);
		booleanFieldSetters.put("distributable", dataBuilder::distributable);
		booleanFieldSetters.put("distributable_to_all_users", dataBuilder::distributable_to_all_users);
		booleanFieldSetters.put("auto_applicable", dataBuilder::auto_applicable);
		booleanFieldSetters.put("activate_now", dataBuilder::activate_now);
		booleanFieldSetters.put("indefinetely", dataBuilder::indefinetely);
		booleanFieldSetters.put("applicable_as_loyalty_redemption", dataBuilder::applicable_as_loyalty_redemption);
		booleanFieldSetters.put("expire_redemption_code_with_reward_end_date",
				dataBuilder::expire_redemption_code_with_reward_end_date);
		booleanFieldSetters.put("additional_steps", dataBuilder::additional_steps);

		return this;
	}

	public RedeemablePayloadBuilder setName(String name) {
		if (name == null || name.isEmpty())
			throw new IllegalArgumentException("name is mandatory");
		dataBuilder.name(name);
		return this;
	}

	public RedeemablePayloadBuilder setExternalId(String externalId) {
		dataBuilder.external_id(externalId);
		return this;
	}

	public RedeemablePayloadBuilder setAutoApplicable(boolean auto_applicable) {
		dataBuilder.auto_applicable(auto_applicable);
		return this;
	}

	public RedeemablePayloadBuilder setDescription(String description) {
		dataBuilder.description(description);
		return this;
	}

	public RedeemablePayloadBuilder setPoints(Integer points) {
		dataBuilder.points(points);
		return this;
	}

	public RedeemablePayloadBuilder addAlternateLocaleName(String language, String translation) {
		currentAlternateLocaleNames
				.add(RedeemableTranslation.builder().language(language).translation(translation).build());
		return this;
	}

	public RedeemablePayloadBuilder addAlternateLocaleDescription(String language, String translation) {
		currentAlternateLocaleDescriptions
				.add(RedeemableTranslation.builder().language(language).translation(translation).build());
		return this;
	}

	public RedeemablePayloadBuilder setTemplate(RedeemableTemplate template) {
		dataBuilder.template(template);
		return this;
	}

	public RedeemablePayloadBuilder setLagDuration(RedeemableLagDuration lagDuration) {
		dataBuilder.lag_duration(lagDuration);
		return this;
	}

	public RedeemablePayloadBuilder setRecurrenceSchedule(RedeemableRecurrenceSchedule recurrenceSchedule) {
		dataBuilder.recurrence_schedule(recurrenceSchedule);
		return this;
	}

	public RedeemablePayloadBuilder setEffectiveLocation(String effectiveLocation) {
		dataBuilder.effective_location(effectiveLocation);
		return this;
	}

	public RedeemablePayloadBuilder setMetaData(String metaData) {
		dataBuilder.meta_data(metaData);
		return this;
	}

	public RedeemablePayloadBuilder setDiscountChannel(String discountChannel) {
		dataBuilder.discount_channel(discountChannel);
		return this;
	}

	/** Set optional boolean field. Only included if called */
	public RedeemablePayloadBuilder setBooleanField(String fieldName, Boolean value) {
		Consumer<Boolean> setter = booleanFieldSetters.get(fieldName);
		if (setter != null && value != null)
			setter.accept(value);
		return this;
	}

	public RedeemablePayloadBuilder setReceiptRule(RedeemableReceiptRule receiptRule) {
		if (receiptRule == null)
			throw new IllegalArgumentException("receipt_rule is mandatory");
		dataBuilder.receipt_rule(receiptRule);
		return this;
	}

	// Method 1 — When user passes a Map<String,Object>
	public RedeemablePayloadBuilder withRedeemingCriterion(Map<String, Object> criterion) {
		if (criterion == null) {
			throw new IllegalArgumentException("redeeming_criterion cannot be null");
		}

		// Get current receipt rule from the data builder
		RedeemableReceiptRule currentRule = dataBuilder.build().getReceipt_rule();
		if (currentRule == null) {
			throw new IllegalStateException("receipt_rule must be set before adding redeeming_criterion");
		}

		// Set redeeming_criterion
		currentRule.setRedeeming_criterion(criterion);

		// Put back updated rule
		dataBuilder.receipt_rule(currentRule);

		return this;
	}

	// Method 2 — Converts QCData object to Map and calls Method 1
	public RedeemablePayloadBuilder withRedeemingCriterion(QCData qcData) {
		if (qcData == null) {
			throw new IllegalArgumentException("QCData cannot be null");
		}

		ObjectMapper mapper = new ObjectMapper();

		@SuppressWarnings("unchecked")
		Map<String, Object> map = mapper.convertValue(qcData, Map.class);

		return withRedeemingCriterion(map);
	}

	// shashank
	public RedeemablePayloadBuilder setDistributableToAllUsers(boolean distributableToAllUsers) {
		dataBuilder.distributable_to_all_users(distributableToAllUsers);
		return this;
	}

	// shashank
	public RedeemablePayloadBuilder setDistributable(boolean distributable) {
		dataBuilder.distributable(distributable);
		return this;
	}

	// shashank
	public RedeemablePayloadBuilder setAdditionalStepsFlag(boolean additionalSteps) {
		dataBuilder.additional_steps(additionalSteps);
		return this;
	}

	// shashank
	public RedeemablePayloadBuilder setApplicable_as_loyalty_redemptionFlag(boolean applicable_as_loyalty_redemption) {
		dataBuilder.applicable_as_loyalty_redemption(applicable_as_loyalty_redemption);
		return this;
	}

	// shashank
	public RedeemablePayloadBuilder setExpiryDays(Integer expiry_days) {
		dataBuilder.expiry_days(expiry_days);
		return this;
	}
	
	public RedeemablePayloadBuilder setTimeZone(String timeZone) {
		dataBuilder.timezone(timeZone);
		return this;
	}
	
	public RedeemablePayloadBuilder setEndTime(String end_time) {
		dataBuilder.end_time(end_time);
		return this;
	}

	/** Adds current RedeemableData to the list */
	public RedeemablePayloadBuilder addCurrentData() {
		if (dataBuilder != null) {
			dataBuilder
					.alternate_locale_name(!currentAlternateLocaleNames.isEmpty() ? currentAlternateLocaleNames : null);
			dataBuilder.alternate_locale_description(
					!currentAlternateLocaleDescriptions.isEmpty() ? currentAlternateLocaleDescriptions : null);

			RedeemableData dataObj = dataBuilder.build();

			if (dataObj.getReceipt_rule() == null)
				throw new IllegalArgumentException("receipt_rule is mandatory in RedeemableData");

			dataList.add(dataObj);
			dataBuilder = null; // reset for next object
		}
		return this;
	}

	/**
	 * Build final RedeemableRequest
	 * 
	 * @throws JsonProcessingException
	 */
	public String build() throws JsonProcessingException {
		addCurrentData(); // Ensure last data is added
		if (dataList.isEmpty()) {
			throw new IllegalArgumentException("At least one RedeemableData object must be added");
		}

		mapper = new ObjectMapper();
		System.out.println("Generated JSON:\n" + mapper.writerWithDefaultPrettyPrinter()
				.writeValueAsString(RedeemableRequest.builder().data(dataList).build()));

		return mapper.writerWithDefaultPrettyPrinter()
				.writeValueAsString(RedeemableRequest.builder().data(dataList).build());

	}

}
