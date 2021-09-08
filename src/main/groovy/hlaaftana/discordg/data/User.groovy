package hlaaftana.discordg.data

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors

import hlaaftana.discordg.Client
import hlaaftana.discordg.DiscordObject

import java.util.regex.Pattern

/**
 * A Discord user.
 * @author Hlaaftana
 */
@CompileStatic
@InheritConstructors
class User extends DiscordObject {
	static final Pattern MENTION_REGEX = ~/<@!?(\d+)>/

	Snowflake id
	String username, avatarHash, discriminator, email, password
	boolean bot

	static final Map<String, Integer> FIELDS = Collections.unmodifiableMap(
			id: 1, username: 2, avatar: 3, discriminator: 4, email: 5, password: 5, bot: 6)

	void jsonField(String name, value) {
		final field = FIELDS.get(name)
		if (null != field) jsonField(field, value)
		else client.log.debug("Unknown field $name for ${this.class}")
	}

	void jsonField(Integer field, value) {
		if (null == field) return
		int f = field.intValue()
		if (f == 1) {
			id = Snowflake.swornString(value)
		} else if (f == 2) {
			username = (String) value
		} else if (f == 3) {
			avatarHash = (String) value
		} else if (f == 4) {
			discriminator = (String) value
		} else if (f == 5) {
			email = (String) value
		} else if (f == 6) {
			password = (String) value
		} else if (f == 7) {
			bot = (boolean) value
		} else client.log.debug("Unknown field number $field for ${this.class}")
	}

	List<Presence> getPresences() { client.members(this)*.presence - (Object) null }
	String getStatus() { presences[0]?.status ?: 'offline' }
	Game getGame() { presences[0]?.game }
	boolean isOnline() { status == 'online' }
	boolean isOffline() { status == 'offline' }
	boolean isIdle() { status == 'idle' }
	boolean isAway() { status == 'idle' }
	String getName() { username }
	String getRawAvatarHash() { avatarHash }
	boolean hasAvatar() { avatarHash }
	int getDefaultAvatarType() { Integer.parseInt(discriminator) % 5 }
	String getAvatar() { hasAvatar() ?
		"https://cdn.discordapp.com/avatars/$id/${avatarHash}.jpg" : '' }
	InputStream newAvatarInputStream() { inputStreamFromDiscord(avatar) }
	File downloadAvatar(file) { downloadFileFromDiscord(avatar, file) }
	String getDiscrim() { discriminator }
	String getNameAndDiscrim() {
		final namearr = name.toCharArray(), discrimarr = discriminator.toCharArray()
		final namelen = namearr.length
		def result = new char[namelen + 5]
		System.arraycopy(namearr, 0, result, 0, namelen)
		result[namelen] = (char) '#'
		System.arraycopy(discrimarr, 0, result, namelen + 1, discrimarr.length)
		String.valueOf(result)
	}
	String getUniqueName() { nameAndDiscrim }
	String getUnique() { nameAndDiscrim }

	Channel getPrivateChannel() {
		client.userDmChannel(id)
	}
	
	Channel getChannel() {
		createOrGetPrivateChannel()
	}

	Channel createPrivateChannel() {
		client.createPrivateChannel(this)
	}

	Channel createOrGetPrivateChannel() {
		privateChannel ?: createPrivateChannel()
	}

	Relationship getRelationship() {
		client.relationshipCache.get(id)
	}

	void addRelationship(type) {
		client.addRelationship(id, type)
	}

	void removeRelationship() {
		client.removeRelationship(id)
	}

	Permissions permissionsFor(Channel channel) {
		channel.permissionsFor(this)
	}

	List<Guild> getSharedGuilds() { client.members(this)*.guild }
	String getMention() { "<@$id>" }
	Member getMember(guild) { client.guild(guild).member(this) }
	Member member(guild) { client.guild(guild).member(this) }

	Message sendMessage(content, boolean tts=false) { channel.sendMessage(content, tts) }
	Message sendMessage(Map data) { channel.sendMessage(data) }
	Message send(Map data) { sendMessage(data) }
	Message send(content, boolean tts = false) { sendMessage(content, tts) }

	Message sendFile(Map data, implicatedFile, filename = null) {
		channel.sendFile(data, implicatedFile, filename)
	}

	Message sendFile(implicatedFile, filename = null) {
		sendFile([:], implicatedFile, filename)
	}
}

@InheritConstructors
@CompileStatic
@InheritConstructors
class Connection extends DiscordObject {
	Snowflake id
	String name, type
	boolean revoked
	List<Integration> integrations

	static final Map<String, Integer> FIELDS = Collections.unmodifiableMap(
			id: 1, name: 2, description: 3, icon: 4, secret: 5, redirect_uris: 5, bot: 6)

	void jsonField(String name, value) {
		final field = FIELDS.get(name)
		if (null != field) jsonField(field, value)
		else client.log.debug("Unknown field $name for ${this.class}")
	}

	void jsonField(Integer field, value) {
		if (null == field) return
		int f = field.intValue()
		if (f == 1) {
			id = Snowflake.swornString(value)
		} else if (f == 2) {
			name = (String) value
		} else if (f == 3) {
			type = (String) value
		} else if (f == 4) {
			revoked = (boolean) value
		} else if (f == 5) {
			final lis = (List<Map>) value
			if (null == integrations) integrations = new ArrayList<>(lis.size())
			for (m in lis) integrations.add(new Integration(client, m))
		} else client.log.debug("Unknown field number $field for ${this.class}")
	}
}

@CompileStatic
@InheritConstructors
class Application extends DiscordObject {
	Snowflake id
	String name, description = '', iconHash, secret
	List<String> redirectUris
	Map bot

	static final Map<String, Integer> FIELDS = Collections.unmodifiableMap(
			id: 1, name: 2, description: 3, icon: 4, secret: 5, redirect_uris: 5, bot: 6)

	void jsonField(String name, value) {
		final field = FIELDS.get(name)
		if (null != field) jsonField(field, value)
		else client.log.debug("Unknown field $name for ${this.class}")
	}

	void jsonField(Integer field, value) {
		if (null == field) return
		int f = field.intValue()
		if (f == 1) {
			id = Snowflake.swornString(value)
		} else if (f == 2) {
			name = (String) value
		} else if (f == 3) {
			description = (String) value
		} else if (f == 4) {
			iconHash = (String) value
		} else if (f == 5) {
			secret = (String) value
		} else if (f == 6) {
			redirectUris = (List<String>) value
		} else if (f == 7) {
			bot = (Map) value
		} else client.log.debug("Unknown field number $field for ${this.class}")
	}

	Map getBotAccount() { botAccount }
	String getIcon() {
		iconHash ? "https://cdn.discordapp.com/app-icons/$id/${iconHash}.jpg"
			: ''
	}
	boolean hasIcon() { iconHash }
	InputStream newIconInputStream() { inputStreamFromDiscord(icon) }
	File downloadIcon(file) { downloadFileFromDiscord(icon, file) }

	Application edit(Map data) {
		client.editApplication(data, id)
	}

	void delete() {
		client.deleteApplication(id)
	}

	Map createBot(String oldAccountToken = null) {
		client.createApplicationBotAccount(this, oldAccountToken)
	}

	String getApplicationLink(Permissions perms = null) {
		Client.getApplicationLink(this, perms)
	}

	String getAppLink(Permissions perms = null) { getApplicationLink(perms) }
	String applicationLink(Permissions perms = null) { getApplicationLink(perms) }
	String appLink(Permissions perms = null) { getApplicationLink(perms) }
}

@CompileStatic
@InheritConstructors
class Profile extends DiscordObject {
	Snowflake id
	@Delegate(excludes = ['getClass', 'toString', 'getId']) User user
	boolean premium
	List<Map> accounts
	Set<Snowflake> mutualGuildIds
	Map<Snowflake, String> mutualGuildNickMap

	static final Map<String, Integer> FIELDS = Collections.unmodifiableMap(
			user: 1, premium: 2, accounts: 3, mutual_guilds: 4, id: 5)

	void jsonField(String name, value) {
		final field = FIELDS.get(name)
		if (null != field) jsonField(field, value)
		else client.log.debug("Unknown field $name for ${this.class}")
	}

	void jsonField(Integer field, value) {
		if (null == field) return
		int f = field.intValue()
		if (f == 1) {
			user = new User(client, (Map) value)
		} else if (f == 2) {
			premium = (boolean) value
		} else if (f == 3) {
			accounts = (List<Map>) value
		} else if (f == 4) {
			final lis = (List<Map>) value
			if (null == mutualGuildIds) mutualGuildIds = new HashSet<>(lis.size())
			if (null == mutualGuildNickMap) mutualGuildNickMap = new HashMap<>(lis.size())
			for (m in lis) {
				final id = Snowflake.swornString(m.id)
				mutualGuildIds.add(id)
				mutualGuildNickMap.put(id, (String) m.nick)
			}
		} else if (f == 5) {
			id = Snowflake.swornString(value)
		} else client.log.debug("Unknown field number $field for ${this.class}")
	}

	List<Guild> getMutualGuilds() { client.guildCache.scoop(mutualGuildIds) }
}

@CompileStatic
@InheritConstructors
class Relationship extends DiscordObject {
	@Delegate(excludes = ['getClass', 'toString']) User user
	int type

	void jsonField(String name, value) {
		if (name == 'user') user = new User(client, (Map) value)
		else if (name == 'type') type = (int) value
		else client.log.debug("Unknown field number $name for ${this.class}")
	}
}