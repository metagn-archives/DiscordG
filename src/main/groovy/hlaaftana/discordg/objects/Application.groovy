package hlaaftana.discordg.objects

import hlaaftana.discordg.Client
import hlaaftana.discordg.util.*

class Application extends DiscordObject {
	Application(Client client, Map object){
		super(client, object, "oauth2/applications/$object.id")
	}

	String getSecret(){ object["secret"] }
	List getRedirectUris(){ object["redirect_uris"] }
	String getDescription(){ object["description"] ?: "" }
	BotAccount getBot(){ new BotAccount(client, object["bot"]) }
	BotAccount getBotAccount(){ new BotAccount(client, object["bot"]) }
	String getIconHash(){ object["icon"] }
	String getIcon() {
		if (iconHash != null){
			"https://cdn.discordapp.com/app-icons/$id/${iconHash}.jpg"
		}else{
			""
		}
	}

	Application edit(Map data){
		Map map = [icon: iconHash, description: description,
			redirect_uris: redirectUris, name: name]
		if (data["icon"] != null){
			if (data["icon"] instanceof String && !(data["icon"].startsWith("data"))){
				data["icon"] = ConversionUtil.encodeImage(data["icon"] as File)
			}else if (ConversionUtil.isImagable(data["icon"])){
				data["icon"] = ConversionUtil.encodeImage(data["icon"])
			}
		}
		new Application(this, requester.jsonPut("", map << data))
	}

	void delete(){
		requester.delete("")
	}

	BotAccount createBot(String oldAccountToken = null){
		new BotAccount(client, requester.jsonPost("bot",
			(oldAccountToken == null) ? [:] : [token: oldAccountToken]))
	}

	String getInviteLink(Permissions permissions = null){
		"https://discordapp.com/oauth2/authorize?client_id=$id&scope=bot" + (permissions ? "&permissions=$permissions.value" : "")
	}

	String inviteLink(Permissions permissions = null){
		getInviteLink(permissions)
	}

	String getInviteUrl(Permissions permissions = null){
		getInviteLink(permissions)
	}

	String inviteUrl(Permissions permissions = null){
		getInviteLink(permissions)
	}
}
