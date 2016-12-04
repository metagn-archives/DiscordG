package hlaaftana.discordg.objects

import groovy.transform.InheritConstructors

import hlaaftana.discordg.Client
import hlaaftana.discordg.util.ConversionUtil
import hlaaftana.discordg.util.JSONUtil

/**
 * A Discord user.
 * @author Hlaaftana
 */
class User extends DiscordObject{
	static final MENTION_REGEX = { String id = /\d+/ -> /<@!?$id>/ }

	User(Client client, Map object){
		super(client, object)
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
	boolean hasAvatar(){ object["avatar"] }
	int getDefaultAvatarType(){ Integer.parseInt(discriminator) % 5 }
	String getAvatar(){ hasAvatar() ?
		"https://cdn.discordapp.com/avatars/${id}/${avatarHash}.jpg" : "" }
	InputStream getAvatarInputStream(){ inputStreamFromDiscord(avatar) }
	File downloadAvatar(file){ downloadFileFromDiscord(avatar, file) }
	String getDiscriminator(){ object["discriminator"] }
	String getDiscrim(){ object["discriminator"] }
	String getNameAndDiscrim(){ "$name#$discrim" }
	String getUniqueName(){ "$name#$discrim" }
	String getUnique(){ "$name#$discrim" }
	boolean isBot(){ object["bot"] }
	String getEmail(){ object["email"] }
	String getPassword(){ object["password"] }

	Channel getPrivateChannel(){
		client.userDmChannel(id)
	}

	Channel createPrivateChannel(){
		client.createPrivateChannel(this)
	}

	Channel createOrGetPrivateChannel(){
		privateChannel ?: createPrivateChannel()
	}

	Permissions permissionsFor(Channel channel){
		channel.permissionsFor(this)
	}

	List<Server> getSharedServers(){ client.members(this)*.server }
	String getMention(){ "<@$id>" }
	Member getMember(server){ client.server(server).member(this) }
	Member member(server){ client.server(server).member(this) }

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
		super(client, object)
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
	boolean hasIcon(){ object["icon"] }
	InputStream getIconInputStream(){ inputStreamFromDiscord(icon) }
	File downloadIcon(file){ downloadFileFromDiscord(icon, file) }

	Application edit(Map data){
		client.editApplication(data, id)
	}

	void delete(){
		client.deleteApplication(id)
	}

	BotAccount createBot(String oldAccountToken = null){
		client.createApplicationBotAccount(this, oldAccountToken)
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
		super(client, object + object.user)
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