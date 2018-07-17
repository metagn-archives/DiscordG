package hlaaftana.discordg.objects

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import hlaaftana.discordg.DiscordObject
import hlaaftana.discordg.Permissions
import hlaaftana.discordg.Snowflake
import hlaaftana.discordg.util.*
import org.codehaus.groovy.runtime.DefaultGroovyMethods

/**
 * A Discord message.
 * @author Hlaaftana
 */
@CompileStatic
@InheritConstructors
class Message extends DiscordObject {
	// this is discord's url pattern (with a little fiddling)
	static String urlPattern = /https?:\/\/[^\s<]+[^<"{|^~`\[\s]/
	static Comparator<Message> byTimestamp = new Comparator<Message>() {
		@Override
		int compare(Message o1, Message o2) {
			o1.timestamp.compareTo(o2.timestamp)
		}
	}
	static Comparator<Message> byTimestampAscending = Collections.reverseOrder(byTimestamp)

	Snowflake id, channelId, webhookId, guildIdField
	def nonce
	String content, rawEditedAt, rawTimestamp
	boolean tts, mentionsEveryone, pinned
	int type
	User author
	Member memberField
	List<User> mentions
	Set<Snowflake> roleMentionIds
	List<Reaction> reactions
	List<Attachment> attachments
	List<Embed> embeds

	void fill(Map map) {
		final gid = map.guild_id
		if (null != gid) guildIdField = Snowflake.swornString(gid)
		super.fill(map)
	}

	static final Map<String, Integer> FIELDS = Collections.unmodifiableMap(
			id: 1, channel_id: 2, webhook_id: 3, nonce: 4, content: 5,
			edited_timestamp: 6, timestamp: 7, tts: 8, mention_everyone: 9,
			pinned: 10, type: 11, author: 12, mentions: 13, mention_roles: 14,
			reactions: 15, attachments: 16, embeds: 17, member: 18, guild_id: 19)

	void jsonField(String name, value) {
		final field = FIELDS.get(name)
		if (null != field) jsonField(field, value)
		else client.log.warn("Unknown field $name for ${this.class}")
	}

	void jsonField(Integer field, value) {
		if (null == field) return
		int f = field.intValue()
		if (f == 1) {
			id = Snowflake.swornString(value)
		} else if (f == 2) {
			channelId = Snowflake.swornString(value)
		} else if (f == 3) {
			webhookId = Snowflake.swornString(value)
		} else if (f == 4) {
			nonce = value
		} else if (f == 5) {
			content = (String) value
		} else if (f == 6) {
			rawEditedAt = (String) value
		} else if (f == 7) {
			rawTimestamp = (String) value
		} else if (f == 8) {
			tts = (boolean) value
		} else if (f == 9) {
			mentionsEveryone = (boolean) value
		} else if (f == 10) {
			pinned = (boolean) value
		} else if (f == 11) {
			type = (int) value
		} else if (f == 12) {
			author = new User(client, (Map) value)
		} else if (f == 13) {
			final lis = (List<Map>) value
			mentions = new ArrayList<>(lis.size())
			for (m in lis) mentions.add(new User(client, m))
		} else if (f == 14) {
			roleMentionIds = Snowflake.swornStringSet(value)
		} else if (f == 15) {
			final lis = (List<Map>) value
			reactions = new ArrayList<>(lis.size())
			for (m in lis) reactions.add(new Reaction(client, m))
		} else if (f == 16) {
			final lis = (List<Map>) value
			attachments = new ArrayList<>(lis.size())
			for (m in lis) attachments.add(new Attachment(client, m))
		} else if (f == 17) {
			final lis = (List<Map>) value
			embeds = new ArrayList<>(lis.size())
			for (m in lis) embeds.add(new Embed(client, m))
		} else if (f == 18) {
			final map = (Map) value
			if (null != guildIdField) {
				def mem = new Member(client)
				mem.guildId = guildIdField
				mem.fill(map)
				client.guildCache[guildIdField].memberCache[mem.id] = mem
				memberField = mem
			} else memberField = new Member(client, map)
		} else if (f == 19) {
			guildIdField = Snowflake.swornString(value)
		} else client.log.warn("Unknown field number $field for ${this.class}")
	}

	String getName() { content }
	Date getEditedAt() { ConversionUtil.fromJsonDate(rawEditedAt) }
	Date getTimestamp() { ConversionUtil.fromJsonDate(rawTimestamp) }
	boolean isPrivate() { client.privateChannelCache.containsKey(channelId) }
	boolean isDm() { channel.dm }
	boolean isGroup() { channel.group }

	Attachment getAttachment() { attachments[0] }

	List<String> getUrls() {
		(content =~ urlPattern).collect() as List<String>
	}

	List<URL> getUrlObjects() { urls.collect(DefaultGroovyMethods.&toURL) }
	DiscordObject getAuthor(boolean mem) { mem ? member : author }
	DiscordObject author(boolean mem = false) { getAuthor(mem) }
	User getSender() { author }
	DiscordObject resolveMember(User user, boolean mem = true) {
		mem ? guild?.memberCache[user.id] : user
	}
	Member getMember() { memberField ?: guild?.memberCache[author.id] }
	boolean isByWebhook() { null != webhookId }
	Webhook requestWebhook() { client.requestWebhook(webhookId) }

	Snowflake getGuildId() { client.channelGuildIdMap[channelId] }
	Guild getGuild() { channel?.guild }
	Channel getParent() { channel }
	Channel getChannel() { client.channel(channelId) }

	List<DiscordObject> getMentions(boolean member) { mentions.collect { resolveMember(it, member) } }
	List<DiscordObject> mentions(boolean member = false) { getMentions(member) }
	List<Member> getMemberMentions() { mentions.collect { guild.memberCache[it.id] } }
	Set<Snowflake> getMentionedRoleIds() { roleMentionIds }
	List<Role> getRoleMentions() { guild.roleCache.scoop(roleMentionIds) }
	List<Role> getMentionedRoles() { roleMentions }

	List<Snowflake> getMentionedChannelIds() { channelIdMentions }
	List<Snowflake> getChannelMentionIds() { channelIdMentions }
	List<Snowflake> getChannelIdMentions() {
		(content =~ /<#(\d+)>/).collect { full, String id -> new Snowflake(id) }
	}

	List<Channel> getMentionedChannels() { channelMentions }
	List<Channel> getChannelMentions() {
		channelIdMentions.collect(guild.&channel) - (Object) null
	}
	
	Permissions getAuthorPermissions() {
		channel.permissionsFor(author)
	}

	boolean isMentioned(thing = client.user) {
		final snow = Snowflake.from(thing)
		for (m in mentions) if (snow == m.id) return true
		if (thing instanceof Role || guild.roleCache.containsKey(snow)) {
			for (rm in roleMentions) if (snow == rm.id) return true
		} else if (thing instanceof Member || (thing = guild.memberCache[snow]) instanceof Member) {
			for (r in ((Member) thing).roleIds) for (rm in roleMentions) if (r == rm.id) return true
		}
		false
	}

	def pin() { client.pinMessage(channelId, id) }
	def unpin() { client.unpinMessage(channelId, id) }

	List<Reaction> getCachedReactions() { client.reactions[id] }
	List<Reaction> getAnyReactions() { reactions ?: cachedReactions }

	void react(emoji) {
		client.reactToMessage(channelId, this, emoji)
	}

	void unreact(emoji, user = '@me') {
		client.unreactToMessage(channelId, this, emoji, user)
	}

	List<User> requestReactors(emoji, int limit = 100) {
		client.requestReactors(channelId, id, emoji, limit)
	}

	Message edit(String newContent) {
		edit(content: newContent)
	}

	Message edit(Map data) {
		client.editMessage(data, channelId, this)
	}

	void delete() {
		client.deleteMessage(channelId, this)
	}

	void deleteAfter(long ms) { Thread.sleep(ms); delete() }
	Message editAfter(String newContent, long ms) { Thread.sleep(ms); edit(newContent) }
	void deleteIf(Closure<Boolean> closure) { if (closure(this)) delete() }
	Message editIf(String newContent, Closure<Boolean> closure) { closure(this) ? edit(newContent) : null }

	String toString() { "$author.name: $content" }
}

@InheritConstructors
@CompileStatic
class Attachment extends DiscordObject {
	Snowflake id
	String filename, url, proxyUrl
	int width, height, size

	static final Map<String, Integer> FIELDS = Collections.unmodifiableMap(
			id: 2, filename: 3, url: 4, proxy_url: 5,
			width: 6, height: 7, size: 8)

	void jsonField(String name, value) {
		final field = FIELDS.get(name)
		if (null != field) jsonField(field, value)
		else client.log.warn("Unknown field $name for ${this.class}")
	}

	void jsonField(Integer field, value) {
		if (null == field) return
		int f = field.intValue()
		if (f == 2) {
			id = Snowflake.swornString(value)
		} else if (f == 3) {
			filename = (String) value
		} else if (f == 4) {
			url = (String) value
		} else if (f == 5) {
			proxyUrl = (String) value
		} else if (f == 6) {
			width = (int) value
		} else if (f == 7) {
			height = (int) value
		} else if (f == 8) {
			size = (int) value
		} else client.log.warn("Unknown field number $field for ${this.class}")
	}

	String getName() { filename }
	String getFileName() { filename }
	InputStream newInputStream() { inputStreamFromDiscord(url) }
	File download(file) { downloadFileFromDiscord(url, file) }
}

@InheritConstructors
@CompileStatic
class Embed extends DiscordObject {
	String title, type, description, url, timestamp
	int color
	Image thumbnail, image
	Provider provider
	Video video
	Footer footer
	Author author
	List<Field> fields

	static final Map<String, Integer> FIELDS = Collections.unmodifiableMap(
			title: 2, type: 3, description: 4, url: 5,
			timestamp: 6, color: 7, thumbnail: 8, image: 9,
			provider: 10, video: 11, footer: 12, author: 13, fields: 14)

	void jsonField(String name, value) {
		final field = FIELDS.get(name)
		if (null != field) jsonField(field, value)
		else client.log.warn("Unknown field $name for ${this.class}")
	}

	void jsonField(Integer field, value) {
		if (null == field) return
		int f = field.intValue()
		if (f == 2) {
			title = (String) value
		} else if (f == 3) {
			type = (String) value
		} else if (f == 4) {
			description = (String) value
		} else if (f == 5) {
			url = (String) value
		} else if (f == 6) {
			timestamp = (String) value
		} else if (f == 7) {
			color = (int) value
		} else if (f == 8) {
			thumbnail = new Image(client, (Map) value)
		} else if (f == 9) {
			image = new Image(client, (Map) value)
		} else if (f == 10) {
			provider = new Provider(client, (Map) value)
		} else if (f == 11) {
			video = new Video(client, (Map) value)
		} else if (f == 12) {
			footer = new Footer(client, (Map) value)
		} else if (f == 13) {
			author = new Author(client, (Map) value)
		} else if (f == 14) {
			final flds = (List<Map>) value
			fields = new ArrayList<>(flds.size())
			for (fi in flds) fields.add(new Field(client, fi))
		} else client.log.warn("Unknown field number $field for ${this.class}")
	}

	Snowflake getId() { null }
	String getName() { title }
	InputStream newInputStream() { inputStreamFromDiscord(url) }
	File download(file) { downloadFileFromDiscord(url, file) }


	@InheritConstructors
	static abstract class EmbedObject extends DiscordObject{
		Snowflake getId() { null }
	}

	@InheritConstructors
	static class Image extends EmbedObject {
		String url, proxyUrl
		int width, height

		static final Map<String, Integer> FIELDS = Collections.unmodifiableMap(
				url: 1, proxyUrl: 2, width: 3, height: 4)

		void jsonField(String name, value) {
			final field = FIELDS.get(name)
			if (null != field) jsonField(field, value)
			else client.log.warn("Unknown field $name for ${this.class}")
		}

		void jsonField(Integer field, value) {
			if (null == field) return
			int f = field.intValue()
			if (f == 1) {
				url = (String) value
			} else if (f == 2) {
				proxyUrl = (String) value
			} else if (f == 3) {
				width = (int) value
			} else if (f == 4) {
				height = (int) value
			} else client.log.warn("Unknown field number $field for ${this.class}")
		}

		String getName() { url }
		InputStream newInputStream() { inputStreamFromDiscord(url) }
		File download(file) { downloadFileFromDiscord(url, file) }
	}

	@InheritConstructors
	static class Provider extends EmbedObject {
		String name, url

		void jsonField(String name, value) {
			if (name == 'name') this.name = (String) value
			else if (name == 'url') url = (String) value
			else client.log.warn("Unknown field $name for ${this.class}")
		}

		InputStream newInputStream() { inputStreamFromDiscord(url) }
		File download(file) { downloadFileFromDiscord(url, file) }
	}

	@InheritConstructors
	static class Video extends EmbedObject {
		String url
		int width, height

		static final Map<String, Integer> FIELDS = Collections.unmodifiableMap(
				url: 1, width: 2, height: 3)

		void jsonField(String name, value) {
			final field = FIELDS.get(name)
			if (null != field) jsonField(field, value)
			else client.log.warn("Unknown field $name for ${this.class}")
		}

		void jsonField(Integer field, value) {
			if (null == field) return
			int f = field.intValue()
			if (f == 1) {
				url = (String) value
			} else if (f == 2) {
				width = (int) value
			} else if (f == 3) {
				height = (int) value
			} else client.log.warn("Unknown field number $field for ${this.class}")
		}

		String getName() { url }
		InputStream newInputStream() { inputStreamFromDiscord(url) }
		File download(file) { downloadFileFromDiscord(url, file) }
	}

	@InheritConstructors
	static class Author extends EmbedObject {
		String name, url, iconUrl, proxyIconUrl

		static final Map<String, Integer> FIELDS = Collections.unmodifiableMap(
				name: 1, url: 2, icon_url: 3, proxy_icon_url: 4)

		void jsonField(String name, value) {
			final field = FIELDS.get(name)
			if (null != field) jsonField(field, value)
			else client.log.warn("Unknown field $name for ${this.class}")
		}

		void jsonField(Integer field, value) {
			if (null == field) return
			int f = field.intValue()
			if (f == 1) {
				name = (String) value
			} else if (f == 2) {
				url = (String) value
			} else if (f == 3) {
				iconUrl = (String) value
			} else if (f == 4) {
				proxyIconUrl = (String) value
			} else client.log.warn("Unknown field number $field for ${this.class}")
		}

		InputStream newIconInputStream() { inputStreamFromDiscord(iconUrl) }
		File downloadIcon(file) { downloadFileFromDiscord(iconUrl, file) }
	}

	@InheritConstructors
	static class Footer extends EmbedObject {
		String text, iconUrl, proxyIconUrl

		static final Map<String, Integer> FIELDS = Collections.unmodifiableMap(
				text: 1, icon_url: 2, proxy_icon_url: 3)

		void jsonField(String name, value) {
			final field = FIELDS.get(name)
			if (null != field) jsonField(field, value)
			else client.log.warn("Unknown field $name for ${this.class}")
		}

		void jsonField(Integer field, value) {
			if (null == field) return
			int f = field.intValue()
			if (f == 1) {
				text = (String) value
			} else if (f == 2) {
				iconUrl = (String) value
			} else if (f == 3) {
				proxyIconUrl = (String) value
			} else client.log.warn("Unknown field number $field for ${this.class}")
		}

		String getName() { text }
		InputStream newIconInputStream() { inputStreamFromDiscord(iconUrl) }
		File downloadIcon(file) { downloadFileFromDiscord(iconUrl, file) }
	}

	@InheritConstructors
	static class Field extends EmbedObject {
		String name, value
		boolean inline

		static final Map<String, Integer> FIELDS = Collections.unmodifiableMap(
				name: 1, value: 2, inline: 3)

		void jsonField(String name, value) {
			final field = FIELDS.get(name)
			if (null != field) jsonField(field, value)
			else client.log.warn("Unknown field $name for ${this.class}")
		}

		void jsonField(Integer field, value) {
			if (null == field) return
			int f = field.intValue()
			if (f == 1) {
				name = (String) value
			} else if (f == 2) {
				this.value = (String) value
			} else if (f == 3) {
				inline = (boolean) value
			} else client.log.warn("Unknown field number $field for ${this.class}")
		}
	}
 }

@InheritConstructors
@CompileStatic
class Reaction extends DiscordObject {
	Snowflake id, userId
	String name
	int count
	boolean me

	static final Map<String, Integer> FIELDS = Collections.unmodifiableMap(
			emoji: 1, user_id: 2, count: 4, me: 5)

	void jsonField(String name, value) {
		final field = FIELDS.get(name)
		if (null != field) jsonField(field, value)
		else client.log.warn("Unknown field $name for ${this.class}")
	}

	void jsonField(Integer field, value) {
		if (null == field) return
		int f = field.intValue()
		if (f == 1) {
			final map = (Map) value
			id = Snowflake.swornString(map.id)
			name = (String) map.name
		} else if (f == 2) {
			userId = Snowflake.swornString(value)
		} else if (f == 4) {
			count = (int) value
		} else if (f == 5) {
			me = (boolean) value
		} else client.log.warn("Unknown field number $field for ${this.class}")
	}


	String getUrl() { "https://cdn.discordapp.com/emojis/${id}.png" }
	InputStream newInputStream() { inputStreamFromDiscord(url) }
	File download(file) { downloadFileFromDiscord(url, file) }
	boolean isCustom() { name ==~ /\w+/ }
}
