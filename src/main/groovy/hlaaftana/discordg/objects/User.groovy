package hlaaftana.discordg.objects

import groovy.transform.InheritConstructors

import hlaaftana.discordg.Client
import hlaaftana.discordg.util.JSONUtil

/**
 * A Discord user.
 * @author Hlaaftana
 */
class User extends DiscordObject{
	static final MENTION_REGEX = { String id = /\d+/ -> /<@!?$id>/ }

	User(Client client, Map object, String concatUrl = ""){
		super(client, object, concatUrl)
	}

	List<Presence> getPresences(){ client.members(this)*.presence - null }
	String getStatus(){ presences[0]?.status ?: "offline" }
	Game getGame(){ presences[0]?.game }
	boolean isOnline(){ status == "online" }
	boolean isOffline(){ status == "offline" }
	boolean isIdle(){ status == "idle" }
	boolean isAway(){ status == "idle" }
	String getMentionRegex(){ MENTION_REGEX(id) }
	String getName(){ username }
	String getUsername(){ object["username"] }
	String getAvatarHash(){ object["avatar"]}
	String getRawAvatarHash(){ object["avatar"] }
	int getDefaultAvatarType(){ Integer.parseInt(discriminator) % 5 }
	String getAvatar(){ "https://cdn.discordapp.com/avatars/${id}/${avatarHash}.jpg" }
	InputStream getAvatarInputStream(){
		avatar.toURL().newInputStream(requestProperties:
			["User-Agent": client.fullUserAgent, Accept: "*/*"])
	}
	File downloadAvatar(File file){
		file.withOutputStream { out ->
			out << avatarInputStream
			new File(file.path)
		}
	}
	String getDiscriminator(){ object["discriminator"] }
	String getDiscrim(){ object["discriminator"] }
	String getNameAndDiscrim(){ "$name#$discrim" }
	boolean isBot(){ object["bot"] }
	String getEmail(){ object["email"] }
	String getPassword(){ object["password"] }

	Channel getPrivateChannel(){
		client.privateChannels.find { it.dm && it.user.id == id }
	}

	Channel createPrivateChannel(){
		new Channel(client,
			client.http.jsonPost("users/@me/channels", [recipient_id: id]))
	}

	Channel createOrGetPrivateChannel(){
		privateChannel ?: createPrivateChannel()
	}

	Permissions permissionsFor(Channel channel){
		channel.permissionsFor(this)
	}

	List<Server> getSharedServers(){ client.servers.findAll { it.object.members.keySet().contains(id) } }
	String getMention(){ "<@$id>" }
	Member getMember(server){ get(this, server, Member) }
	Member member(server){ get(this, server, Member) }

	Message sendMessage(String message, boolean tts = false){ createOrGetPrivateChannel().sendMessage(message, tts) }
	Message sendFile(File file){ createOrGetPrivateChannel().sendFile(file) }
	Message sendFile(String filePath){ createOrGetPrivateChannel().sendFile(filePath) }
}

@InheritConstructors
class Connection extends DiscordObject {
	List<Integration> getIntegrations(){ object["integrations"].collect { new Integration(client, it) } }
	boolean isRevoked(){ object["revoked"] }
	String getType(){ object["type"] }
}

class Application extends DiscordObject {
	Application(Client client, Map object){
		super(client, object, "oauth2/applications/$object.id")
	}

	String getSecret(){ object["secret"] }
	List getRedirectUris(){ object["redirect_uris"] }
	String getDescription(){ object["description"] ?: "" }
	BotAccount getBot(){ new BotAccount(client, object["bot"]) }
	BotAccount getBotAccount(){ botAccount }
	String getIconHash(){ object["icon"] }
	String getIcon() {
		iconHash ? "https://cdn.discordapp.com/app-icons/$id/${iconHash}.jpg"
			: ""
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
		new Application(this, http.jsonPut("", map << data))
	}

	void delete(){
		http.delete("")
	}

	BotAccount createBot(String oldAccountToken = null){
		new BotAccount(client, http.jsonPost("bot",
			(oldAccountToken == null) ? [:] : [token: oldAccountToken]))
	}

	String getApplicationLink(app, perms = null){
		Client.getApplicationLink(app, perms)
	}

	String getAppLink(app, perms = null){ getApplicationLink(app, perms) }
	String applicationLink(app, perms = null){ getApplicationLink(app, perms) }
	String appLink(app, perms = null){ getApplicationLink(app, perms) }
}

@InheritConstructors
class BotAccount extends User {
	String getToken(){ object["token"] }
}

class Profile extends User {
	Profile(Client client, Map object){
		super(client, object + object.user, "users/$object.id/profile")
	}

	User getUser(){ new User(client, object.user) }
	boolean isPremium(){ object.premium }
	List<Account> getAccounts(){ object.connected_accounts.collect { new Account(client, it) } }
	List<String> getMutualServerIds(){ object.mutual_guilds*.id }
	List<Server> getMutualServers(){ mutualServerIds.collect { client.server(it) } }
	Map<String, String> getMutualServerNickMap(){
		object.mutual_guilds.collectEntries { [(it.id): it.nick] }
	}

	@InheritConstructors
	static class Account extends DiscordObject {
		String getType(){ object.type }
	}
}