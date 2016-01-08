package ml.hlaaftana.discordg.objects

import java.util.List

import ml.hlaaftana.discordg.util.JSONUtil

import com.mashape.unirest.http.Unirest

class TextChannel extends Channel {
	TextChannel(API api, Map object){
		super(api, object)
	}

	String getTopic() { return object["topic"] }
	String getMention() { return "<#${this.id}>" }

	void startTyping() {
		api.requester.post("https://discordapp.com/api/channels/${this.getId()}/typing", [])
	}

	Message sendMessage(String content, boolean tts=false) {
		if (content.length() > 2000) throw new Exception("You tried to send a message longer than 2000 characters.")
		return new Message(api, JSONUtil.parse(api.requester.post("https://discordapp.com/api/channels/${this.id}/messages", ["content": content, "tts": tts, "channel_id": this.id])))
	}

	Message sendFile(File file){
		return new Message(api, JSONUtil.parse(api.requester.headerUp(Unirest.post("https://discordapp.com/api/channels/${this.id}/messages")).field("file", file).asString().getBody()))
	}

	List<Message> getLogs(int max=100) {
		List<Message> logs = new ArrayList<Message>()
		List array = JSONUtil.parse(api.requester.get("https://discordapp.com/api/channels/${this.id}/messages?limit=50"))
		for (m in array){
			logs.add(new Message(api, m))
		}
		return logs
	}
}
