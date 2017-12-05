package hlaaftana.discordg.objects

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
	int getMaxAge() { (int) object.max_age }
	String getCode() { (String) object.code }
	String getId() { (String) object.code }
	String getUrl(){ "https://discord.gg/${id}" }
	Guild getGuild(){ client.guild((String) guildObject.id) }
	Map getGuildObject() { (Map) object.guild }
	boolean isRevoked() { (boolean) object.revoked }
	boolean isDeleted(){ revoked }
	String getRawCreatedAt() { (String) object.created_at }
	Date getCreatedAt(){ ConversionUtil.fromJsonDate(rawCreatedAt) }
	boolean isTemporary() { (boolean) object.temporary }
	int getUses() { (int) object.uses }
	int getMaxUses() { (int) object.max_uses }
	User getInviter(){ new User(client, (Map) object.inviter) }
	Channel getChannel(){ client.channel((String) channelObject.id) }
	Map getChannelObject() { (Map) object.channel }
	DiscordObject getParent(){ channel }
	String toString(){ url }
	
	void delete(){ client.deleteInvite(id) }

	static String parseId(i){
		if (i instanceof Invite) return i
		def urlText = i.toString()
		URL urlObj = i instanceof URL ? (URL) i : new URL(i.toString())
		urlText.startsWith('http:') || urlText.startsWith('https:') ?
			urlObj.file.replaceAll(/\/(?:.+?\/)*/, '') : urlText
	}
}