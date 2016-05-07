package hlaaftana.discordg.util

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

	static parse(File file){ return parse(file.text) }

	/**
	 * Converts an object to JSON. Can be a Map, List, or anything JsonOutput.toJson can convert.
	 * @param thing - The thing to convert.
	 * @return a JSON string.
	 */
	static String json(thing){
		return JsonOutput.toJson(thing)
	}

	static File dump(String filename, thing){ return dump(new File(filename), thing) }

	static File dump(File file, thing){
		if (!file.exists()) file.createNewFile()
		file.write(JsonOutput.prettyPrint(json(thing)))
		return file
	}

	static File modify(String filename, newData){ return modify(new File(filename), newData) }

	static File modify(File file, newData){
		if (!file.exists()) return dump(file, newData)
		def oldData = parse(file)
		if (oldData instanceof List){
			oldData += newData
		}else if (oldData instanceof Map){
			if (newData instanceof Map){
				oldData << newData
			}else{
				oldData << newData.toSpreadMap()
			}
		}
		return dump(file, oldData)
	}
}
