package hlaaftana.discordg.objects

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors

import hlaaftana.discordg.Client
import hlaaftana.discordg.DiscordObject
import hlaaftana.discordg.Permissions

import java.util.regex.Pattern

/**
 * A Discord user.
 * @author Hlaaftana
 */
@CompileStatic
class User extends DiscordObject{
	static final Pattern MENTION_REGEX = ~/<@!?(\d+)>/

	User(Client client, Map object){
		super(client, object)
	}

	List<Presence> getPresences(){ client.members(this)*.presence - (Object) null }
	String getStatus(){ presences[0]?.status ?: 'offline' }
	Game getGame(){ presences[0]?.game }
	boolean isOnline(){ status == 'online' }
	boolean isOffline(){ status == 'offline' }
	boolean isIdle(){ status == 'idle' }
	boolean isAway(){ status == 'idle' }
	String getName(){ username }
	String getUsername() { (String) object.username }
	String getAvatarHash() { (String) object.avatar }
	String getRawAvatarHash() { (String) object.avatar }
	boolean hasAvatar(){ object.avatar }
	int getDefaultAvatarType(){ Integer.parseInt(discriminator) % 5 }
	String getAvatar(){ hasAvatar() ?
		"https://cdn.discordapp.com/avatars/${id}/${avatarHash}.jpg" : '' }
	InputStream newAvatarInputStream(){ inputStreamFromDiscord(avatar) }
	File downloadAvatar(file){ downloadFileFromDiscord(avatar, file) }
	String getDiscriminator() { (String) object.discriminator }
	String getDiscrim() { (String) object.discriminator }
	String getNameAndDiscrim(){ "$name#$discrim" }
	String getUniqueName(){ "$name#$discrim" }
	String getUnique(){ "$name#$discrim" }
	boolean isBot() { (boolean) object.bot }
	String getEmail() { (String) object.email }
	String getPassword() { (String) object.password }

	Channel getPrivateChannel(){
		client.userDmChannel(id)
	}
	
	Channel getChannel(){
		createOrGetPrivateChannel()
	}

	Channel createPrivateChannel(){
		client.createPrivateChannel(this)
	}

	Channel createOrGetPrivateChannel(){
		privateChannel ?: createPrivateChannel()
	}

	Relationship getRelationship(){
		client.relationshipCache.at(id)
	}

	void addRelationship(type){
		client.addRelationship(id, type)
	}

	void removeRelationship(){
		client.removeRelationship(id)
	}

	Permissions permissionsFor(Channel channel){
		channel.permissionsFor(this)
	}

	List<Guild> getSharedGuilds(){ client.members(this)*.guild }
	String getMention(){ "<@$id>" }
	Member getMember(guild){ client.guild(guild).member(this) }
	Member member(guild){ client.guild(guild).member(this) }

	Message sendMessage(content, boolean tts=false) { channel.sendMessage(content, tts) }
	Message sendMessage(Map data){ channel.sendMessage(data) }
	Message send(Map data){ sendMessage(data) }
	Message send(content, boolean tts = false){ sendMessage(content, tts) }

	Message sendFile(Map data, implicatedFile, filename = null) {
		channel.sendFile(data, implicatedFile, filename)
	}

	Message sendFile(implicatedFile, filename = null) {
		sendFile([:], implicatedFile, filename)
	}
}

@InheritConstructors
@CompileStatic
class Connection extends DiscordObject {
	List<Integration> getIntegrations(){ ((List<Map>) object.integrations).collect { new Integration(client, it) } }
	boolean isRevoked() { (boolean) object.revoked }
	String getType() { (String) object.type }
}

@CompileStatic
class Application extends DiscordObject {
	Application(Client client, Map object){
		super(client, object)
	}

	String getSecret() { (String) object.secret }
	List getRedirectUris() { (List) object.redirect_uris }
	String getDescription(){ (String) object.description ?: '' }
	BotAccount getBot(){ new BotAccount(client, (Map) object.bot) }
	BotAccount getBotAccount(){ botAccount }
	String getIconHash() { (String) object.icon }
	String getIcon() {
		iconHash ? "https://cdn.discordapp.com/app-icons/$id/${iconHash}.jpg"
			: ''
	}
	boolean hasIcon(){ (boolean) object.icon }
	InputStream newIconInputStream(){ inputStreamFromDiscord(icon) }
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

	String getApplicationLink(Permissions perms = null){
		Client.getApplicationLink(this, perms)
	}

	String getAppLink(Permissions perms = null){ getApplicationLink(perms) }
	String applicationLink(Permissions perms = null){ getApplicationLink(perms) }
	String appLink(Permissions perms = null){ getApplicationLink(perms) }
}

@InheritConstructors
@CompileStatic
class BotAccount extends User {
	String getToken() { (String) object.token }
}

@CompileStatic
class Profile extends User {
	Profile(Client client, Map object){
		super(client, object + (Map) object.user)
	}

	User getUser(){ new User(client, (Map) object.user) }
	boolean isPremium() { (boolean) object.premium }
	List<Account> getAccounts(){ ((List<Map>) object.connected_accounts).collect { new Account(client, it) } }
	List<String> getMutualGuildIds(){ ((List<Map>) object.mutual_guilds)*.get('id') }
	List<Guild> getMutualGuilds(){ mutualGuildIds.collect { client.guild(it) } }
	Map<String, String> getMutualGuildNickMap(){
		((List<Map>) object.mutual_guilds).collectEntries { [(it.id): it.nick] }
	}

	@InheritConstructors
	static class Account extends DiscordObject {
		String getType() { (String) object.type }
	}
}

@CompileStatic
class Relationship extends User {
	Relationship(Client client, Map object){ super(client, object + (Map) object.user) }

	User getUser(){ new User(client, (Map) object.user) }
	int getType() { (int) object.type }
}