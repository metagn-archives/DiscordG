package hlaaftana.discordg.util

import com.mashape.unirest.http.Unirest
import groovy.json.*
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import hlaaftana.discordg.DiscordG

import java.nio.charset.Charset

/**
 * Methods as utilities for JSON using the groovy.json package.
 * @author Hlaaftana
 */
@CompileStatic
class JSONUtil {
	static JsonSlurper slurper = new JsonSlurper()

	static parse(String string){
		slurper.parseText(string)
	}

	static parse(File file, String charset = 'UTF-8'){ parse(file.getText(charset)) }

	static String json(thing){
		JsonOutput.toJson(thing instanceof JSONable ? thing.json() : thing)
	}

	static String pjson(thing){ JsonOutput.prettyPrint(json(thing)) }

	static File dump(String filename, thing, String charset = 'UTF-8'){ dump(new File(filename), thing, charset) }

	static File dump(File file, thing, String charset = 'UTF-8'){
		if (!file.exists()) file.createNewFile()
		file.write(pjson(thing), charset)
		file
	}

	static File modify(String filename, Map newData){ modify(new File(filename), newData) }

	static File modify(File file, Map newData){
		if (!file.exists()) return dump(file, newData)
		def a = parse(file)
		if (!(a instanceof Map))
			throw new UnsupportedOperationException("Can't modify file $file because it's not a map")
		def oldData = (Map) a
		// build the maps in case theyre lazy
		oldData.toString()
		newData.toString()
		def x = modifyMaps(oldData, newData)
		x.toString()
		dump(file, x)
	}
	
	static Map modifyMaps(Map x, Map y){
		def a = null == x ? new HashMap() : new HashMap(x)
		for (e in y) {
			def k = e.key, v = e.value
			if (a.containsKey(k)){
				if (v instanceof Collection) doSomething a, k, v
				else if (v instanceof Map) ((Map) a[k]) << modifyMaps((Map) a[k], v)
				else a.put k, v
			}
			else a.put k, v
		}
		a
	}

	@CompileDynamic
	private static void doSomething(Map a, k, v) {
		a.put(k, a.get(k) + v)
	}
}

interface JSONable {
	// this is NOT supposed to return raw JSON, just a Groovy representation of an object
	// that will regularly be translated to JSON
	def json()
}

// there used to be a JSONPath class here
// it was bad. i replaced it with the Path class in my Kismet language

class JSONSimpleHTTP {
	static get(String url){
		JSONUtil.parse(Unirest.get(url).header('User-Agent', DiscordG.USER_AGENT).asString().getBody())
	}

	static delete(String url){
		JSONUtil.parse(Unirest.delete(url).header('User-Agent', DiscordG.USER_AGENT).asString().getBody())
	}

	static post(String url, Map body){
		JSONUtil.parse(Unirest.post(url).header('User-Agent', DiscordG.USER_AGENT).body(JSONUtil.json(body)).asString().getBody())
	}

	static patch(String url, Map body){
		JSONUtil.parse(Unirest.patch(url).header('User-Agent', DiscordG.USER_AGENT).body(JSONUtil.json(body)).asString().getBody())
	}

	static put(String url, Map body){
		JSONUtil.parse(Unirest.put(url).header('User-Agent', DiscordG.USER_AGENT).body(JSONUtil.json(body)).asString().getBody())
	}
}
