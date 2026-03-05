package com.punchh.server.api.payloadbuilder;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Base class containing common payload fields that are used across multiple
 * API endpoints. These fields are generic and not tied to any specific API
 * domain.
 * 
 * This class can be extended by payload builders that need access to these
 * common fields.
 * 
 */
@Getter
@Setter
@Accessors(chain = true)
public class DynamicPayloadFields {

	private String client;
	private String email;
	private String password;
	private String password_confirmation;
	private String signup_channel;
	private Boolean terms_and_conditions;
	private Boolean privacy_policy;
	private String external_source;
	private String external_source_id;
    private String access_token;
    private String fb_uid;

}