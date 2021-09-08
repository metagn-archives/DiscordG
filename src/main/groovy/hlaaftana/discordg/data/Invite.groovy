package hlaaftana.discordg.data

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import hlaaftana.discordg.DiscordObject
import hlaaftana.discordg.util.*

/**
 * An invite to a Discord guild.
 * @author Hlaaftana
 */
@InheritConstructors
@CompileStatic
class Invite extends DiscordObject {
	int maxAge, uses, maxUses
	String code, rawCreatedAt /*created_at*/
	Map guildObject /*guild*/, channelObject /*channel*/
	boolean revoked, temporary
	User inviter

	static final Map<String, Integer> FIELDS = Collections.unmodifiableMap(
			max_age: 1, uses: 2, max_uses: 3, code: 4, created_at: 5,
			guild: 6, channel: 7, revoked: 8, temporary: 9,
			inviter: 10)

	void jsonField(String name, value) {
		final field = FIELDS.get(name)
		if (null != field) jsonField(field, value)
		else client.log.debug("Unknown field $name for ${this.class}")
	}

	void jsonField(Integer field, value) {
		if (null == field) return
		int f = field.intValue()
		if (f == 1) {
			maxAge = (int) value
		} else if (f == 2) {
			uses = (int) value
		} else if (f == 3) {
			maxUses = (int) value
		} else if (f == 4) {
			code = (String) value
		} else if (f == 5) {
			rawCreatedAt = (String) value
		} else if (f == 6) {
			guildObject = (Map) value
		} else if (f == 7) {
			channelObject = (Map) value
		} else if (f == 8) {
			revoked = (boolean) value
		} else if (f == 9) {
			temporary = (boolean) value
		} else if (f == 10) {
			inviter = new User(client, (Map) value)
		} else client.log.debug("Unknown field number $field for ${this.class}")
	}

	String getName() { code }
	Snowflake getId() { null }
	String getUrl() { "https://discord.gg/".concat(name) }
	Guild getGuild() { client.guild(Snowflake.swornString(guildObject.id)) }
	boolean isDeleted() { revoked }
	Date getCreatedAt() { ConversionUtil.fromJsonDate(rawCreatedAt) }
	Channel getChannel() { client.channel(Snowflake.swornString(channelObject.id)) }
	DiscordObject getParent() { channel }
	String toString() { url }
	
	void delete() { client.deleteInvite(id) }

	static String parseId(i) {
		if (i instanceof Invite) return i
		def urlText = i.toString()
		URL urlObj = i instanceof URL ? (URL) i : new URL(i.toString())
		urlText.startsWith('http:') || urlText.startsWith('https:') ?
			urlObj.file.replaceAll(/\/(?:.+?\/)*/, '') : urlText
	}
}