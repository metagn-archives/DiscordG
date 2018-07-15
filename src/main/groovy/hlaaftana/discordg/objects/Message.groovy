package hlaaftana.discordg.objects

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import hlaaftana.discordg.Client
import hlaaftana.discordg.DiscordObject
import hlaaftana.discordg.Permissions
import hlaaftana.discordg.util.*
import org.codehaus.groovy.runtime.DefaultGroovyMethods

/**
 * A Discord message.
 * @author Hlaaftana
 */
@CompileStatic
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

	Message(Client client, Map object) {
		super(client, object)
	}

	String getName() { content }
	def getNonce() { object.nonce }
	String getContent() { (String) object.content }
	String getRawEditedAt() { (String) object.edited_timestamp }
	Date getEditedAt() { ConversionUtil.fromJsonDate(rawEditedAt) }
	String getRawTimestamp() { (String) object.timestamp }
	Date getTimestamp() { ConversionUtil.fromJsonDate(rawTimestamp) }
	boolean isTts() { (boolean) object.tts }
	int getType() { (int) object.type }
	boolean isMentionsEveryone() { (boolean) object.mention_everyone }
	String getChannelId() { (String) object.channel_id }
	boolean isPrivate() { client.privateChannelCache.containsKey(channelId) }
	boolean isDm() { channel.dm }
	boolean isGroup() { channel.group }

	Attachment getAttachment() { attachments[0] }
	List<Attachment> getAttachments() {
		((List<Map>) object.attachments).collect { new Attachment(client, it + [message_id: id]) }
	}
	List<Embed> getEmbeds() {
		((List<Map>) object.embeds).collect { new Embed(client, it + [message_id: id]) }
	}

	List<String> getUrls() {
		(content =~ urlPattern).collect() as List<String>
	}

	List<URL> getUrlObjects() { urls.collect(DefaultGroovyMethods.&toURL) }
	String getAuthorId() {
		(String) ((Map<String, Object>) object.author).id
	}
	User getAuthor() { getAuthor(false) }
	User getAuthor(boolean member) {
		resolveMember((Map) object.author, member)
	}
	User author(boolean member = false) {
		getAuthor(member)
	}
	User getSender() { author }
	User resolveMember(User user, boolean member = true) {
		Member ass = guild?.member(user)
		if (ass && member) ass
		else user
	}
	User resolveMember(Map object, boolean member = true) {
		Member ass = guild?.member(object)
		if (ass && member) ass
		else new User(client, object)
	}
	Member getMember() { (Member) author(true) }
	boolean isByWebhook() { webhookId }
	String getWebhookId() { (String) object.webhook_id }
	Webhook requestWebhook() { client.requestWebhook(webhookId) }

	String getGuildId() { client.channelGuildIdMap[channelId] }
	Guild getGuild() { channel?.guild }
	Channel getParent() { channel }
	Channel getChannel() { client.channel(channelId) }

	List<User> getMentions() { getMentions(false) }
	List<User> getMentions(boolean member) { ((List<Map>) object.mentions).collect { resolveMember(it, member) } }
	List mentions(boolean member = false) { getMentions(member) }
	List<Role> getMentionedRoles() { ((List<String>) object.mention_roles).collect { guild.roleMap[it] } }
	List<Role> getRoleMentions() { mentionedRoles }

	List<String> getMentionedChannelIds() { channelIdMentions }
	List<String> getChannelMentionIds() { channelIdMentions }
	List<String> getChannelIdMentions() {
		(content =~ /<#(\d+)>/).collect { full, String id -> id }
	}

	List<Channel> getMentionedChannels() { channelMentions }
	List<Channel> getChannelMentions() {
		channelIdMentions.collect(guild.&channel) - (Object) null
	}
	
	Permissions getAuthorPermissions() {
		channel.permissionsFor(author())
	}

	boolean isMentioned(thing = client.user) {
		id(thing) in mentions.collect(this.&id) ||
			guild.member(thing)?.roles?.any { it in mentionedRoles } ||
			id(thing) in object.mention_roles
	}

	boolean isPinned() { (boolean) object.pinned }
	def pin() { client.pinMessage(channelId, id) }
	def unpin() { client.unpinMessage(channelId, id) }

	List<Reaction> getReactions() { ((List<Map>) object.reactions)?.collect { new Reaction(client, it) } }
	List<Reaction> getCachedReactions() { client.reactions[id]?.collect { new Reaction(client, it) } }
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
	void deleteIf(Closure closure) { if (closure(this)){ delete() } }
	Message editIf(String newContent, Closure closure) { if (closure(this)){ edit(newContent) } }

	String toString() { "$author.name: $content" }
}

@InheritConstructors
@CompileStatic
class Attachment extends DiscordObject {
	String getMessageId() { (String) object.message_id }
	String getName() { (String) object.filename }
	String getFilename() { (String) object.filename }
	String getFileName() { (String) object.filename }
	int getSize() { (int) object.size }
	boolean isImage() { object.width && object.height }
	int getWidth() { (int) object.width }
	int getHeight() { (int) object.height }
	String getProxyUrl() { (String) object.proxy_url }
	String getUrl() { (String) object.url }
	InputStream newInputStream() { inputStreamFromDiscord(url) }
	File download(file) { downloadFileFromDiscord(url, file) }
}

@InheritConstructors
@CompileStatic
class Embed extends DiscordObject {
	String getMessageId() { (String) object.message_id }
	String getId() { messageId }
	String getName() { (String) object.title }
	String getTitle() { (String) object.title }
	String getType() { (String) object.type }
	String getDescription() { (String) object.description }
	String getUrl() { (String) object.url }
	InputStream newInputStream() { inputStreamFromDiscord(url) }
	File download(file) { downloadFileFromDiscord(url, file) }
	int getColor() { (int) object.color }
	String getTimestamp() { (String) object.timestamp }

	Image getThumbnail() { object.thumbnail ? new Image(client, (Map) object.thumbnail) : null }
	Provider getProvider() { object.provider ? new Provider(client, (Map) object.provider) : null }
	Video getVideo() { object.video ? new Video(client, (Map) object.video) : null }
	Image getImage() { object.image ? new Image(client, (Map) object.image) : null }
	Footer getFooter() { object.footer ? new Footer(client, (Map) object.footer) : null }
	Author getAuthor() { object.author ? new Author(client, (Map) object.author) : null }
	List<Field> getFields() { object.fields ? ((List<Map>) object.fields).collect { new Field(client, it) } : null }

	@InheritConstructors
	static class Image extends DiscordObject {
		String getName() { url }
		String getProxyUrl() { (String) object.proxy_url }
		String getUrl() { (String) object.url }
		int getWidth() { (int) object.width }
		int getHeight() { (int) object.height }
		InputStream newInputStream() { inputStreamFromDiscord(url) }
		File download(file) { downloadFileFromDiscord(url, file) }
	}

	@InheritConstructors
	static class Provider extends DiscordObject {
		String getName() { url }
		String getUrl() { (String) object.url }
		InputStream newInputStream() { inputStreamFromDiscord(url) }
		File download(file) { downloadFileFromDiscord(url, file) }
	}

	@InheritConstructors
	static class Video extends DiscordObject {
		String getName() { url }
		String getUrl() { (String) object.url }
		int getWidth() { (int) object.width }
		int getHeight() { (int) object.height }
		InputStream newInputStream() { inputStreamFromDiscord(url) }
		File download(file) { downloadFileFromDiscord(url, file) }
	}

	@InheritConstructors
	static class Author extends DiscordObject {
		String getName() { (String) object.name }
		String getIconUrl() { (String) object.icon_url }
		String getProxyIconUrl() { (String) object.proxy_icon_url }
		InputStream newIconInputStream() { inputStreamFromDiscord(iconUrl) }
		File downloadIcon(file) { downloadFileFromDiscord(iconUrl, file) }
	}

	@InheritConstructors
	static class Footer extends DiscordObject {
		String getName() { text }
		String getText() { (String) object.text }
		String getIconUrl() { (String) object.icon_url }
		String getProxyIconUrl() { (String) object.proxy_icon_url }
		InputStream newIconInputStream() { inputStreamFromDiscord(iconUrl) }
		File downloadIcon(file) { downloadFileFromDiscord(iconUrl, file) }
	}

	@InheritConstructors
	static class Field extends DiscordObject {
		String getName() { (String) object.name }
		String getValue() { (String) object.value }
		boolean isInline() { (boolean) object.inline }
	}
 }

@InheritConstructors
@CompileStatic
class Reaction extends DiscordObject {
	String getId() { (String) ((Map) object.emoji).id }
	String getName() { (String) ((Map) object.emoji).name }
	int getCount() { (int) object.count }
	String getUrl() { "https://cdn.discordapp.com/emojis/${id}.png" }
	InputStream newInputStream() { inputStreamFromDiscord(url) }
	File download(file) { downloadFileFromDiscord(url, file) }
	boolean isCustom() { name ==~ /\w+/ }
	boolean isByMe() { (boolean) object.me }
}
