package hlaaftana.discordg.util.bot

import com.mashape.unirest.http.Unirest
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import hlaaftana.discordg.util.JSONUtil

/// A wrapper for https://cleverbot.io/
/// API documentation: https://docs.cleverbot.io/docs/querying-cleverbot
@CompileStatic
class CleverbotDotIO {
	String baseUrl = 'https://cleverbot.io/1.0/'
	String user
	String key
	String nick

	CleverbotDotIO(String u, String k, String n = null) {
		user = u
		key = k
		nick = n
	}

	def startSession() {
		nick = post('create', [:]).nick
	}

	String ask(text) {
		if (!nick) startSession()
		post('ask', [nick: nick, text: text]).response
	}

	Map post(String path, Map body = null) {
		Map response
		if (body != null) {
			if (!user || !key) noAuth()
			Map<String, Object> a = new HashMap<>()
			a.user = user
			a.key = key
			if (nick) a.nick = nick
			a << body
			response = (Map) JSONUtil.parse(
				Unirest.post(baseUrl + path)
					.fields(a)
					.asString().body)
		} else {
			response = (Map) JSONUtil.parse(
				Unirest.post(baseUrl + path)
					.asString().body)
		}
		checkResponse(response)
	}

	private static void noAuth() {
		throw new IllegalArgumentException('User or key not set.' +
			'Go to https://cleverbot.io/keys to get your user and key')
	}

	private static Map checkResponse(Map response) {
		if (response.status != 'success')
			throw new APIException("Something messed up: $response.status")
		else response
	}
}

@CompileStatic
@InheritConstructors
class APIException extends Exception {}