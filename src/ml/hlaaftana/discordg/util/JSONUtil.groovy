package ml.hlaaftana.discordg.util

import groovy.json.*

class JSONUtil {
	static parse(String string){
		return new JsonSlurper().parseText(string)
	}

	static String json(Map map){
		return JsonOutput.toJson(map)
	}
}
