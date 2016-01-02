package hlaaftana.discordg.objects

import java.util.List

import org.json.JSONArray
import org.json.JSONObject

class Message extends Base{
	Message(API api, JSONObject object){
		super(api, object)
	}

	String getContent(){ return object.getString("content") }
	boolean isTTS(){ return object.getBoolean("tts") }
	JSONArray getAttachments(){ return object.getJSONArray("attachments") }
	JSONArray getEmbeds() { return object.getJSONArray("embeds") }

	User getAuthor() {
		return new User(api, object.getJSONObject("author"))
	}

	User getSender() {
		return this.getAuthor()
	}

	TextChannel getTextChannel() {
		for (s in api.client.getServers()){
			for (c in s.getTextChannels()){
				if (c.getID().equals(object.getString("channel_id"))) return c
			}
		}
	}

	Message edit(String newContent) {
		return new Message(api, new JSONObject(api.getRequester().patch("https://discordapp.com/api/channels/${object.getString("channel_id")}/messages/${this.getID()}", new JSONObject().put("content", newContent))).put("channel_id", object.getString("channel_id")))
	}

	void delete() {
		api.getRequester().delete("https://discordapp.com/api/channels/${object.getString("channel_id")}/messages/${this.getID()}")
	}

	void acknowledge() {
		api.getRequester().post("https://discordapp.com/api/channels/${object.getString("channel_id")}/messages/${this.getID()}/ack")
	}
}
