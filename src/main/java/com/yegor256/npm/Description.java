package com.yegor256.npm;

import javax.json.JsonObject;
import java.nio.file.Path;

/**
 * The $package_name.json file.
 */
public class Description {
	
	/**
	 * Path to the file
	 */
	private final Path json;
	
	/**
	 * @param json path to the json file with meta information
	 */
	public Description(Path json) {
		this.json = json;
	}
	
	/**
	 * Update the description file.
	 *
	 * @param jsonObject new version of a package
	 * @return was the description updated?
	 */
	public boolean update(JsonObject jsonObject) {
		return false;
	}
}
