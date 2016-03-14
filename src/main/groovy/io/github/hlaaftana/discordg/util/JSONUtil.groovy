package io.github.hlaaftana.discordg.util

import groovy.json.*

/**
 * Methods as utilities for JSON using the groovy.json package.
 * @author Hlaaftana
 */
class JSONUtil {
	/**
	 * Parses JSON from a string.
	 * @param string - The string.
	 * @return a Map or a List containing the object.
	 */
	static parse(String string){
		return new JsonSlurper().parseText(string)
	}

	/**
	 * Converts an object to JSON. Can be a Map, List, or anything JsonOutput.toJson can convert.
	 * @param object - The object to convert.
	 * @return a JSON string.
	 */
	static String json(object){
		return JsonOutput.toJson(object)
	}
}
