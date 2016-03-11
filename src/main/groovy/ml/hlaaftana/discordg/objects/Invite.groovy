package ml.hlaaftana.discordg.objects

import ml.hlaaftana.discordg.util.*

/**
 * An invite to a Discord server.
 * @author Hlaaftana
 */
class Invite extends DiscordObject {
	Invite(Client client, Map object){ super(client, object) }

	/**
	 * @return the amount of... some sort of time measure until this invite expires
	 */
	int getMaxAge(){ return this.object["max_age"] }
	/**
	 * @return the code for this invite
	 */
	String getCode(){ return this.object["code"] }
	/**
	 * @return the code for this invite
	 */
	String getId(){ return this.object["code"] }
	String getUrl(){ return "https://discord.gg/${this.id}" }
	/**
	 * @return the server where this invite is. This will usually return null. Use #getBaseServer().
	 */
	Server getServer(){
		return client.getServerById(this.object["guild"]["id"])
	}
	/**
	 * @return a Base containing the ID and name of the server.
	 */
	DiscordObject getBaseServer(){ return new DiscordObject(client, this.object["guild"]) }
	/**
	 * @return whether or not the invite was revoked.
	 */
	boolean isRevoked(){ return this.object["revoked"] }
	/**
	 * @return when the invite was created.
	 */
	String getRawCreateTime(){ return this.object["created_at"] }
	/**
	 * @return when the invite was created.
	 */
	Date getCreateTime(){ return ConversionUtil.fromJsonDate(this.object["created_at"]) }
	/**
	 * @return whether or not the invite is temporary.
	 */
	boolean isTemporary(){ return this.object["temporary"] }
	/**
	 * @return the amount of uses for this invite.
	 */
	int getUses(){ return this.object["uses"] }
	/**
	 * @return the max amount of uses for this invite.
	 */
	int getMaxUses(){ return this.object["max_uses"] }
	/**
	 * @return the person who created the invite.
	 */
	User getInviter(){ return new User(client, this.object["inviter"]) }
	/**
	 * @return a base object containing the name, ID and type of the channel. You will, however, have to get the type by doing ".object.type".
	 */
	DiscordObject getBaseChannel(){ return new DiscordObject(client, this.object["channel"]) }

	String toString(){ return this.url }

	/**
	 * Parses a URL string into an invite ID.
	 * @param url - the URL string.
	 * @return the parsed ID.
	 */
	static String parseId(String url){
		return url.replaceFirst("https://", "").replaceFirst("http://", "").replaceFirst("discord.gg/", "").replaceFirst("discordapp.com/invite/", "")
	}
}
