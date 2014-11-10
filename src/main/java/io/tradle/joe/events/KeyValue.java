package io.tradle.joe.events;

import com.google.gson.JsonElement;

public class KeyValue implements JsonEventData {

	private final String key;
	private final String value;

	public KeyValue(String key, JsonElement json) {
		this.key = key;
		this.value = json.toString();
	}

	public KeyValue(String key, String value) {
		this.key = key;
		this.value = value;
	}
}
