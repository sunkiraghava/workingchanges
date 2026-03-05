package com.punchh.server.apimodel.updatelocation;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateLocationExtraAttributes {

	private String brand;

	@JsonProperty("online_order_url")
	private String onlineOrderUrl;

	@JsonProperty("alternate_store_number")
	private String alternateStoreNumber;

	@JsonProperty("enable_multiple_redemptions")
	private Boolean enableMultipleRedemptions;

	@JsonProperty("additional_url")
	private List<UpdateLocationAdditionalUrl> additionalUrl;

	@JsonProperty("store_times")
	private List<UpdateLocationStoreTime> storeTimes;
}