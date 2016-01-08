package ml.hlaaftana.discordg.objects

import ml.hlaaftana.discordg.util.Log

class Invite extends Base {
	Invite(API api, Map object){ super(api, object) }

	int getMaxAge(){ return object["max_age"] }
	String getCode(){ return object["code"] }
	Server getServer(){
		// This will usually return null. Use #getBaseServer().
		return api.client.getServerById(object["guild"]["id"])
	}
	Base getBaseServer(){ return new Base(api, object["guild"]) }
	boolean isRevoked(){ return object["revoked"] }
	String getCreatedDate(){ return object["created_at"] }
	boolean isTemporary(){ return object["temporary"] }
	int getUses(){ return object["uses"] }
	int getMaxUses(){ return object["max_uses"] }
	User getInviter(){ return new User(api, object["inviter"]) }
	// I won't have a #getChannel().
	Base getBaseChannel(){ return new Base(api, object["channel"]) }

	static String parseId(String url){
		return url.replaceFirst("https://", "").replaceFirst("http://", "").replaceFirst("discord.gg/", "").replaceFirst("discordapp.com/invite/", "")
	}
}
