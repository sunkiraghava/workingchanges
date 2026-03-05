package com.punchh.server.api.payloadbuilder;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Generic builder class for creating request payloads dynamically. Uses
 * reflection to automatically include all non-null fields in the payload map.
 * Supports String, primitive types, and their wrapper classes (Integer,
 * Boolean, etc.).
 * 
 * Configure the fields you need via the fluent setters and call
 * {@link #buildPayloadMap()} to obtain a map representation.
 * 
 * Example:
 * <pre>
 * new DynamicPayloadBuilder()
 *     .setClient("client")
 *     .setEmail("email")
 *     .setPassword("password")
 *     .buildPayloadMap();
 * </pre>
 * 
 * Note: This class performs no mandatory-field validation; callers are
 * responsible for enforcing any required fields.
 * 
 * This class is intended to be used as a common, reusable payload builder and
 * is not tied to any specific API (e.g. auth, redeemable, etc.).
 */

@Getter
@Setter
@Accessors(chain = true)
public class DynamicPayloadBuilder extends DynamicPayloadFields {

	/**
	 * Builds payload map using reflection to automatically include all non-null
	 * fields. Skips internal fields (static/transient). Includes fields from
	 * parent classes as well.
	 * 
	 * @return Map containing all non-null fields
	 */
	public Map<String, Object> buildPayloadMap() {
		Map<String, Object> payload = new HashMap<>();

		// Get all fields including inherited ones
		List<Field> allFields = getAllFields(this.getClass());
		for (Field field : allFields) {
			// Skip internal and non-payload fields (static / transient)
			if (isInternalField(field))
				continue;

			try {
				field.setAccessible(true);
				Object value = field.get(this);

				// Only include non-null values in the payload
				if (value != null) 
                    payload.put(field.getName(), value);
			} catch (IllegalAccessException e) {
				// Wrap in unchecked exception with clear context for easier debugging
				throw new RuntimeException(
						"Failed to access field '" + field.getName() + "' while building payload map", e);
			}
		}
		return payload;
	}

	/**
	 * Collects all fields from the class hierarchy, including inherited fields.
	 * 
	 * @param clazz The class to collect fields from
	 * @return List of all fields including inherited ones
	 */
	private List<Field> getAllFields(Class<?> clazz) {
		List<Field> fields = new ArrayList<>();
		Class<?> currentClass = clazz;
		while (currentClass != null && currentClass != Object.class) {
			for (Field field : currentClass.getDeclaredFields()) {
				fields.add(field);
			}
			currentClass = currentClass.getSuperclass();
		}
		return fields;
	}

	private boolean isInternalField(Field field) {
		int modifiers = field.getModifiers();
		return Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers);
	}
}