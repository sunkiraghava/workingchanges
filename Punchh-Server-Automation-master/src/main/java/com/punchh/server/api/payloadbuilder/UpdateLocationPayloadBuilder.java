package com.punchh.server.api.payloadbuilder;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.punchh.server.apimodel.updatelocation.UpdateLocationAdditionalUrl;
import com.punchh.server.apimodel.updatelocation.UpdateLocationDetails;
import com.punchh.server.apimodel.updatelocation.UpdateLocationExtraAttributes;
import com.punchh.server.apimodel.updatelocation.UpdateLocationGroup;
import com.punchh.server.apimodel.updatelocation.UpdateLocationRequest;
import com.punchh.server.apimodel.updatelocation.UpdateLocationStoreTime;

public class UpdateLocationPayloadBuilder {

	private String locationId; // mandatory
	private String storeNumber;
	private String city;
	private String country;
	private String address;
	private String externalStoreId;
	private Double latitude;
	private Double longitude;
	private String name;
	private String phoneNumber;
	private String postCode;
	private String state;
	private String storeTags;
	private String timeZone;
	private Boolean generateBarcodes;
	private Boolean enableMultipleRedemptions;
	private List<UpdateLocationAdditionalUrl> additionalUrls;
	private List<UpdateLocationStoreTime> storeTimes;
	private List<UpdateLocationGroup> locationGroups;
	private ObjectMapper mapper;

	// ------------------- Builder Methods -------------------
	public UpdateLocationPayloadBuilder withLocationId(String locationId) {
		this.locationId = locationId;
		return this;
	}

	public UpdateLocationPayloadBuilder withStoreNumber(String storeNumber) {
		this.storeNumber = storeNumber;
		return this;
	}

	public UpdateLocationPayloadBuilder withCity(String city) {
		this.city = city;
		return this;
	}

	public UpdateLocationPayloadBuilder withCountry(String country) {
		this.country = country;
		return this;
	}

	public UpdateLocationPayloadBuilder withAddress(String address) {
		this.address = address;
		return this;
	}

	public UpdateLocationPayloadBuilder withExternalStoreId(String externalStoreId) {
		this.externalStoreId = externalStoreId;
		return this;
	}

	public UpdateLocationPayloadBuilder withLatitude(Double latitude) {
		this.latitude = latitude;
		return this;
	}

	public UpdateLocationPayloadBuilder withLongitude(Double longitude) {
		this.longitude = longitude;
		return this;
	}

	public UpdateLocationPayloadBuilder withName(String name) {
		this.name = name;
		return this;
	}

	public UpdateLocationPayloadBuilder withPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
		return this;
	}

	public UpdateLocationPayloadBuilder withPostCode(String postCode) {
		this.postCode = postCode;
		return this;
	}

	public UpdateLocationPayloadBuilder withState(String state) {
		this.state = state;
		return this;
	}

	public UpdateLocationPayloadBuilder withStoreTags(String storeTags) {
		this.storeTags = storeTags;
		return this;
	}

	public UpdateLocationPayloadBuilder withTimeZone(String timeZone) {
		this.timeZone = timeZone;
		return this;
	}

	public UpdateLocationPayloadBuilder withGenerateBarcodes(Boolean generateBarcodes) {
		this.generateBarcodes = generateBarcodes;
		return this;
	}

	public UpdateLocationPayloadBuilder withEnableMultipleRedemptions(Boolean enable) {
		this.enableMultipleRedemptions = enable;
		return this;
	}

	public UpdateLocationPayloadBuilder withAdditionalUrls(List<UpdateLocationAdditionalUrl> urls) {
		this.additionalUrls = urls;
		return this;
	}

	public UpdateLocationPayloadBuilder withStoreTimes(List<UpdateLocationStoreTime> times) {
		this.storeTimes = times;
		return this;
	}

	public UpdateLocationPayloadBuilder withLocationGroups(List<UpdateLocationGroup> groups) {
		this.locationGroups = groups;
		return this;
	}

	// ------------------- Build Method -------------------
	public String buildJson() throws JsonProcessingException {
		if (locationId == null) {
			throw new IllegalArgumentException("locationId is mandatory");
		}

		// Build LocationExtraAttributes if any field is provided
		UpdateLocationExtraAttributes extraAttributes = null;
		if (enableMultipleRedemptions != null || (additionalUrls != null && !additionalUrls.isEmpty())
				|| (storeTimes != null && !storeTimes.isEmpty())) {
			extraAttributes = UpdateLocationExtraAttributes.builder()
					.enableMultipleRedemptions(enableMultipleRedemptions).additionalUrl(additionalUrls)
					.storeTimes(storeTimes).build();
		}

		// Build Location if any location field is provided
		UpdateLocationDetails location = null;
		if (city != null || country != null || address != null || externalStoreId != null || latitude != null
				|| longitude != null || name != null || phoneNumber != null || postCode != null || state != null
				|| storeTags != null || timeZone != null || generateBarcodes != null || extraAttributes != null
				|| (locationGroups != null && !locationGroups.isEmpty())) {

			location = UpdateLocationDetails.builder().city(city).country(country).address(address)
					.externalStoreId(externalStoreId).latitude(latitude).longitude(longitude).name(name)
					.phoneNumber(phoneNumber).postCode(postCode).state(state).storeTags(storeTags).timeZone(timeZone)
					.generateBarcodes(generateBarcodes).locationExtraAttributes(extraAttributes)
					.locationGroups(locationGroups).build();
		}

		UpdateLocationRequest request = UpdateLocationRequest.builder().locationId(locationId).storeNumber(storeNumber)
				.location(location).build();
		// Convert to JSON
		mapper = new ObjectMapper();
		System.out.println("Generated JSON:\n" + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request));
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);

	}
}