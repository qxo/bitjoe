package io.tradle.joe.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Gsons {

	private static final Gson regular = new GsonBuilder().disableHtmlEscaping().create();
	private static final Gson prettyGson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
	
	public static Gson pretty() {
		return prettyGson;
	}

	public static Gson ugly() {
		return regular;
	}

	public static Gson def() {
		return regular;
	}
}
