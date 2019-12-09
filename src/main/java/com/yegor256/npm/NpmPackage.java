package com.yegor256.npm;

import javax.json.JsonObject;

public class NpmPackage {
	
	private final Description description;
	
	public NpmPackage(Description description) {
		this.description = description;
	}
	
	public boolean update(JsonObject jsonObject) {
		return description.update(jsonObject);
	}
}
