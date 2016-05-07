package io.github.hlaaftana.discordg.oauth

import io.github.hlaaftana.discordg.objects.*
import io.github.hlaaftana.discordg.util.*

class Application extends DiscordObject {
	Application(Client client, Map object){ super(client, object) }

	String getSecret(){ return this.object["secret"] }
	List getRedirectUris(){ return this.object["redirect_uris"] }
	String getDescription(){ return this.object["description"] ?: "" }
	BotAccount getBot(){ return new BotAccount(this.client, this.object["bot"]) }
	BotAccount getBotAccount(){ return new BotAccount(this.client, this.object["bot"]) }
	String getIconHash(){ return this.object["icon"] }
	String getIcon() {
		if (this.iconHash != null){
			return "https://cdn.discordapp.com/app-icons/${this.id}/${this.iconHash}.jpg"
		}else{
			return ""
		}
	}

	Application edit(Map data){
		Map map = ["icon": this.iconHash, "description": this.description, "redirect_uris": this.redirectUris, "name": this.name]
		if (data["icon"] != null){
			if (data["icon"] instanceof String && !(data["icon"].startsWith("data"))){
				data["icon"] = ConversionUtil.encodeToBase64(data["icon"] as File)
			}else if (data["icon"] instanceof File){
				data["icon"] = ConversionUtil.encodeToBase64(data["icon"])
			}
		}
		return new Application(this, JSONUtil.parse(client.requester.put("https://discordapp.com/api/oauth2/applications/${this.id}", map << data)))
	}

	void delete(){
		client.requester.delete("https://discordapp.com/api/oauth2/applications/${this.id}")
	}

	BotAccount createBot(String oldAccountToken=null){
		return new BotAccount(this, JSONUtil.parse(client.requester.post("https://discordapp.com/api/oauth2/applications/${this.id}/bot", (oldAccountToken == null) ? [:] : [token: oldAccountToken])))
	}
}
