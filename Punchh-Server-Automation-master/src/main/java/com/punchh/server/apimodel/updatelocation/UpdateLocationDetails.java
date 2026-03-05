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
public class UpdateLocationDetails {

	private String address;
	private String city;
	private String country;

	@JsonProperty("external_store_id")
	private String externalStoreId;

	private Double latitude;
	private Double longitude;
	private String name;

	@JsonProperty("phone_number")
	private String phoneNumber;

	@JsonProperty("location_groups")
	private List<UpdateLocationGroup> locationGroups;

	@JsonProperty("loc_email")
	private String locEmail;

	@JsonProperty("post_code")
	private String postCode;

	private String state;

	@JsonProperty("store_tags")
	private String storeTags;

	@JsonProperty("location_extra_attributes")
	private UpdateLocationExtraAttributes locationExtraAttributes;

	@JsonProperty("time_zone")
	private String timeZone;

	@JsonProperty("generate_barcodes")
	private Boolean generateBarcodes;
}