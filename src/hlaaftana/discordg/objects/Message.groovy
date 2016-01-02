package hlaaftana.discordg.objects

import java.util.List

import hlaaftana.discordg.util.JSONUtil

class Message extends Base{
	Message(API api, Map object){
		super(api, object)
	}

	String getContent(){ return object["content"] }
	boolean isTTS(){ return object["tts"] }
	List getAttachments(){ return object["attachments"] }
	List getEmbeds() { return object["embeds"] }

	User getAuthor() {
		return new User(api, object["author"])
	}

	User getSender() {
		return this.getAuthor()
	}

	TextChannel getTextChannel() {
		for (s in api.client.getServers()){
			for (c in s.getTextChannels()){
				if (c.getID().equals(object["channel_id"])) return c
			}
		}
	}

	Message edit(String newContent) {
		return new Message(api, JSONUtil.parse(api.getRequester().patch("https://discordapp.com/api/channels/${object["channel_id"]}/messages/${this.getID()}", ["content": newContent])))
	}

	void delete() {
		api.getRequester().delete("https://discordapp.com/api/channels/${object["channel_id"]}/messages/${this.getID()}")
	}

	void acknowledge() {
		api.getRequester().post("https://discordapp.com/api/channels/${object["channel_id"]}/messages/${this.getID()}/ack", [:])
	}
}
