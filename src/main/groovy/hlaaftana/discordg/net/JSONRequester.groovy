package hlaaftana.discordg.net

import hlaaftana.discordg.DiscordG
import hlaaftana.discordg.util.JSONUtil

import com.mashape.unirest.http.Unirest

class JSONRequester {
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
