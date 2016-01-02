package hlaaftana.discordg.objects

import java.util.List

import org.json.JSONArray
import org.json.JSONObject

class TextChannel extends Channel {
	API api
	JSONObject object
	TextChannel(API api, JSONObject object){
		super(object)
		this.object = object
		this.api = api
	}

	String getTopic() { return object.getString("topic") }

	void startTyping() {
		api.getRequester().post("https://discordapp.com/api/channels/${this.getID()}/typing")
	}

	Message sendMessage(String content, boolean tts=false) {
		return new Message(api, new JSONObject(api.getRequester().post("https://discordapp.com/api/channels/${this.getID()}/messages", new JSONObject().put("content", content).put("tts", tts))).put("channel_id", this.getID()))
	}

	List<Message> getLogs(int max=100) {
		List<Message> logs = new ArrayList<Message>()
		JSONArray array = new JSONArray(api.getRequester().get("https://discordapp.com/api/channels/${this.getID()}/messages?limit=50"))
	}
}
