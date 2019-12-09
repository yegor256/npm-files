package com.yegor256.npm;

import javax.json.JsonObject;

public class Npm {
	
	private final Storage storage;
	
	public Npm(Storage storage) {
		this.storage = storage;
	}
	
	public void publish(String prefix, JsonObject json) {
	
	}
}
