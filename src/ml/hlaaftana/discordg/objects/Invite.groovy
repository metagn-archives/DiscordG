package ml.hlaaftana.discordg.objects

import ml.hlaaftana.discordg.util.*

/**
 * An invite to a Discord server.
 * @author Hlaaftana
 */
class Invite extends Base {
	Invite(API api, Map object){ super(api, object) }

	/**
	 * @return the amount of... some sort of time measure until this invite expires
	 */
	int getMaxAge(){ return object["max_age"] }
	/**
	 * @return the code for this invite
	 */
	String getCode(){ return object["code"] }
	/**
	 * @return the code for this invite
	 */
	String getId(){ return object["code"] }
	/**
	 * @return the server where this invite is. This will usually return null. Use #getBaseServer().
	 */
	Server getServer(){
		return api.client.getServerById(object["guild"]["id"])
	}
	/**
	 * @return a Base containing the ID and name of the server.
	 */
	Base getBaseServer(){ return new Base(api, object["guild"]) }
	/**
	 * @return whether or not the invite was revoked.
	 */
	boolean isRevoked(){ return object["revoked"] }
	/**
	 * @return when the invite was created.
	 */
	String getCreateTimeRaw(){ return object["created_at"] }
	/**
	 * @return when the invite was created.
	 */
	Date getCreateTime(){ return ConversionUtil.toDiscordDate(object["created_at"]) }
	/**
	 * @return whether or not the invite is temporary.
	 */
	boolean isTemporary(){ return object["temporary"] }
	/**
	 * @return the amount of uses for this invite.
	 */
	int getUses(){ return object["uses"] }
	/**
	 * @return the max amount of uses for this invite.
	 */
	int getMaxUses(){ return object["max_uses"] }
	/**
	 * @return the person who created the invite.
	 */
	User getInviter(){ return new User(api, object["inviter"]) }
	/**
	 * @return a base object containing the name, ID and type of the channel. You will, however, have to get the type by doing ".object.type".
	 */
	Base getBaseChannel(){ return new Base(api, object["channel"]) }

	/**
	 * Parses a URL string into an invite ID.
	 * @param url - the URL string.
	 * @return the parsed ID.
	 */
	static String parseId(String url){
		return url.replaceFirst("https://", "").replaceFirst("http://", "").replaceFirst("discord.gg/", "").replaceFirst("discordapp.com/invite/", "")
	}
}
