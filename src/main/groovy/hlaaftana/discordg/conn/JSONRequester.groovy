package hlaaftana.discordg.conn

import hlaaftana.discordg.DiscordG
import hlaaftana.discordg.util.JSONUtil

import com.mashape.unirest.http.Unirest

class JSONRequester {
	/**
	 * GETs from a URL.
	 * @param url - the URL string.
	 * @return the response as a string.
	 */
	static get(String url){
		return JSONUtil.parse(Unirest.get(url).header("User-Agent", DiscordG.USER_AGENT).asString().getBody())
	}

	/**
	 * DELETEs a URL.
	 * @param url - the URL string.
	 * @return the response as a string.
	 */
	static delete(String url){
		return JSONUtil.parse(Unirest.delete(url).header("User-Agent", DiscordG.USER_AGENT).asString().getBody())
	}

	/**
	 * POSTs to a URL with a body.
	 * @param url - the URL string.
	 * @param body - the body as a Map which will be converted to JSON.
	 * @return the response as a string.
	 */
	static post(String url, Map body){
		return JSONUtil.parse(Unirest.post(url).header("User-Agent", DiscordG.USER_AGENT).body(JSONUtil.json(body)).asString().getBody())
	}

	/**
	 * PATCHes a URL with a body.
	 * @param url - the URL string.
	 * @param body - the body as a Map which will be converted to JSON.
	 * @return the response as a string.
	 */
	static patch(String url, Map body){
		return JSONUtil.parse(Unirest.patch(url).header("User-Agent", DiscordG.USER_AGENT).body(JSONUtil.json(body)).asString().getBody())
	}

	/**
	 * PUTs to a URL.
	 * @param url - the URL string.
	 * @return the response as a string.
	 */
	static put(String url, Map body){
		return JSONUtil.parse(Unirest.put(url).header("User-Agent", DiscordG.USER_AGENT).body(JSONUtil.json(body)).asString().getBody())
	}
}
