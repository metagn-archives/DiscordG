package hlaaftana.discordg.objects

import java.util.List

import hlaaftana.discordg.util.JSONUtil

class TextChannel extends Channel {
	TextChannel(API api, Map object){
		super(api, object)
	}

	String getTopic() { return object["topic"] }

	void startTyping() {
		api.getRequester().post("https://discordapp.com/api/channels/${this.getID()}/typing")
	}

	Message sendMessage(String content, boolean tts=false) {
		if (content.length() > 2000) throw new Exception("You tried to send a message longer than 2000 characters.")
		return new Message(api, JSONUtil.parse(api.getRequester().post("https://discordapp.com/api/channels/${this.getID()}/messages", ["content": content, "tts": tts, "channel_id": this.getID()])))
	}

	/*Message sendFile(String fileName, File file){
		FileDataBodyPart filePart = new FileDataBodyPart(fileName, file);

	}*/

	List<Message> getLogs(int max=100) {
		List<Message> logs = new ArrayList<Message>()
		List array = JSONUtil.parse(api.getRequester().get("https://discordapp.com/api/channels/${this.getID()}/messages?limit=50"))
		for (m in array){
			logs.add(new Message(api, m))
		}
		return logs
	}
}
