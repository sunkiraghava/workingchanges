package com.punchh.server.apimodel.redeemable;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RedeemableData {
	private String name;

	@Builder.Default
	private List<RedeemableTranslation> alternate_locale_name = new ArrayList<>();

	@Builder.Default
	private List<RedeemableTranslation> alternate_locale_description = new ArrayList<>();

	private String external_id;
	private String description;
	private String note;
	private Boolean allow_for_support_gifting;
	private Boolean available_as_template;
	private Boolean distributable;
	private Boolean distributable_to_all_users;
	private String segment_definition_id;
	private Boolean auto_applicable;
	private RedeemableReceiptRule receipt_rule;
	private Boolean activate_now;
	private String start_time;
	private Boolean indefinetely;
	private Integer expiry_days;
	private String end_time;
	private String timezone;
	private Integer remind_before;
	private String discount_channel;
	private Integer points;
	private Integer redemption_code_expiry_mins;
	private Boolean applicable_as_loyalty_redemption;
	private Boolean expire_redemption_code_with_reward_end_date;
	private RedeemableTemplate template;
	private RedeemableLagDuration lag_duration;
	private RedeemableRecurrenceSchedule recurrence_schedule;
	private String effective_location;
	private String meta_data;
	private Double discount_amount ; 
	private Boolean additional_steps ;
}
