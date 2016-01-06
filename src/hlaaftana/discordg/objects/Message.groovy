package hlaaftana.discordg.objects

import java.util.List

import hlaaftana.discordg.util.JSONUtil

class Message extends Base{
	Message(API api, Map object){
		super(api, object)
	}

	String getName(){ return this.getContent() }
	String getContent(){ return object["content"] }
	boolean isTTS(){ return object["tts"] }
	List getAttachments(){ return object["attachments"] }
	List getEmbeds() { return object["embeds"] }
	User getAuthor() { return new User(api, object["author"]) }
	User getSender() { return this.getAuthor() }
	Server getServer() { return this.getTextChannel().getServer() }
	TextChannel getTextChannel() { return api.client.getTextChannelById(object["channel_id"]) }
	TextChannel getChannel() { return this.getTextChannel() }

	Message edit(String newContent) {
		return new Message(api, JSONUtil.parse(api.getRequester().patch("https://discordapp.com/api/channels/${object["channel_id"]}/messages/${this.getId()}", ["content": newContent])))
	}

	void delete() {
		api.getRequester().delete("https://discordapp.com/api/channels/${object["channel_id"]}/messages/${this.getId()}")
	}

	// removed ack method because of discord dev request
}
