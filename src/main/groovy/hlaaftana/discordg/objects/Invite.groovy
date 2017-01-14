package hlaaftana.discordg.objects

import hlaaftana.discordg.util.*

/**
 * An invite to a Discord server.
 * @author Hlaaftana
 */
@groovy.transform.InheritConstructors
class Invite extends DiscordObject {
	int getMaxAge(){ object["max_age"] }
	String getCode(){ object["code"] }
	String getId(){ object["code"] }
	String getUrl(){ "https://discord.gg/${id}" }
	Server getServer(){ client.server(object["guild"]["id"]) ?:
		new DiscordObject(client, object["guild"]) }
	boolean isRevoked(){ object["revoked"] }
	boolean isDeleted(){ revoked }
	String getRawCreatedAt(){ object["created_at"] }
	Date getCreatedAt(){ ConversionUtil.fromJsonDate(object["created_at"]) }
	boolean isTemporary(){ object["temporary"] }
	int getUses(){ object["uses"] }
	int getMaxUses(){ object["max_uses"] }
	User getInviter(){ new User(client, object["inviter"]) }
	DiscordObject getChannel(){ client.channel(object["channel"]["id"]) ?:
		new DiscordObject(client, object["channel"]) }
	DiscordObject getParent(){ channel }
	String toString(){ url }
	
	void delete(){ client.deleteInvite(id) }

	static String parseId(String url){
		url.startsWith("http:") || url.startsWith("https:") ?
			new URL(url).file.replaceAll(/\/(?:.+?\/)*/, '') : url
	}
}