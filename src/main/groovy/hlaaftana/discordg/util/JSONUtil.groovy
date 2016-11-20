package hlaaftana.discordg.util

import com.mashape.unirest.http.Unirest
import groovy.json.*
import hlaaftana.discordg.DiscordG

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

	static parse(File file, charset = "UTF-8"){ return parse(file.getText(charset)) }

	/**
	 * Converts an object to JSON. Can be a Map, List, or anything JsonOutput.toJson can convert.
	 * @param thing - The thing to convert.
	 * @return a JSON string.
	 */
	static String json(thing){
		return JsonOutput.toJson(thing instanceof JSONable ? thing.json() : thing)
	}

	static File dump(String filename, thing, charset = "UTF-8"){ return dump(new File(filename), thing, charset) }

	static File dump(File file, thing, charset = "UTF-8"){
		if (!file.exists()) file.createNewFile()
		file.write(JsonOutput.prettyPrint(json(thing)), charset)
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

class JSONPath {
	List<Expression> parsedExpressions = [new Expression("", Expression.AccessType.OBJECT)]

	JSONPath(String aaa){
		aaa.toList().each {
			if (parsedExpressions.last().lastChar() == "\\"){
				parsedExpressions.last().removeLastChar()
				parsedExpressions.last() << it
			}else if (it == "."){
				parsedExpressions +=
					Expression.AccessType.OBJECT.express("")
			}else if (it == "["){
				parsedExpressions +=
					Expression.AccessType.ARRAY.express("")
			}else if (it != "]"){
				parsedExpressions.last() << it
			}
		}
	}

	static JSONPath parse(String aaa){ new JSONPath(aaa) }

	def parseAndApply(json){
		apply(JSONUtil.parse(json))
	}

	def apply(thing){
		def newValue = thing
		parsedExpressions.each { Expression it ->
			newValue = it.act(newValue)
		}
		newValue
	}

	static class Expression {
		String raw
		AccessType type
		Expression(String ahh, AccessType typ){ raw = ahh; type = typ }

		Expression leftShift(other){
			raw += other
			this
		}

		Expression plus(other){
			new Expression(raw, type) << other
		}

		Expression removeLastChar(){
			raw = raw[0..(raw.size() - 1)]
			this
		}

		String lastChar(){
			raw.toList() ? raw.toList().last() : ""
		}

		Closure getAction(){
			{ thing -> raw == "" ? thing.flatten() : raw == "*" ? thing.collect() : thing[raw.asType(type.accessor)] }
		}

		def act(thing){
			action.call(thing)
		}

		String toString(){ raw }

		static enum AccessType {
			OBJECT(String),
			ARRAY(int)

			Class accessor
			AccessType(Class ass){ accessor = ass }

			Expression express(ahde){
				new Expression(ahde.toString(), this)
			}
		}
	}
}

class JSONSimpleHTTP {
	static get(String url){
		JSONUtil.parse(Unirest.get(url).header("User-Agent", DiscordG.USER_AGENT).asString().getBody())
	}

	static delete(String url){
		JSONUtil.parse(Unirest.delete(url).header("User-Agent", DiscordG.USER_AGENT).asString().getBody())
	}

	static post(String url, Map body){
		JSONUtil.parse(Unirest.post(url).header("User-Agent", DiscordG.USER_AGENT).body(JSONUtil.json(body)).asString().getBody())
	}

	static patch(String url, Map body){
		JSONUtil.parse(Unirest.patch(url).header("User-Agent", DiscordG.USER_AGENT).body(JSONUtil.json(body)).asString().getBody())
	}

	static put(String url, Map body){
		JSONUtil.parse(Unirest.put(url).header("User-Agent", DiscordG.USER_AGENT).body(JSONUtil.json(body)).asString().getBody())
	}
}
