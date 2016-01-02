package hlaaftana.discordg.objects

import java.util.List

import org.json.JSONArray
import org.json.JSONObject

class Message extends Base{
	JSONObject object
	API api
	Message(API api, JSONObject object){
		super(object)
		this.api = api
		this.object = object
	}

	String getContent(){ return object.getString("content") }
	boolean isTTS(){ return object.getBoolean("tts") }
	JSONArray getAttachments(){ return object.getJSONArray("attachments") }
	JSONArray getEmbeds() { return object.getJSONArray("embeds") }

	User getAuthor() {
		return null
	}

	User getSender() {
		return null
	}

	Channel getChannel() {
		return null
	}

	Message edit(String newContent) {
		return new Message(api, new JSONObject(api.getRequester().patch("https://discordapp.com/api/channels/${object.getString("channel_id")}/messages/${this.getID()}", new JSONObject().put("content", newContent))).put("channel_id", object.getString("channel_id")))
	}

	void delete() {

	}

	void acknowledge() {

	}
}
