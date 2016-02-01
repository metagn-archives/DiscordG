package ml.hlaaftana.discordg.objects

/**
 * A voice channel. Extends Channel.
 * @author Hlaaftana
 */
class VoiceChannel extends Channel{
	VoiceChannel(API api, Map object){ super(api, object) }

	void moveMember(Member member){
		api.requester.patch("https://discordapp.com/api/guilds/${member.server.id}/members/{member.id}", ["channel_id": this.id])
	}
}
