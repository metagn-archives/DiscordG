package ml.hlaaftana.discordg.objects

import ml.hlaaftana.discordg.request.VoiceWSClient
import ml.hlaaftana.discordg.objects.VoiceClient
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest
import org.eclipse.jetty.websocket.client.WebSocketClient

/**
 * A voice channel. Extends Channel.
 * @author Hlaaftana
 */
class VoiceChannel extends Channel{
	VoiceChannel(API api, Map object){ super(api, object) }

	void moveMember(Member member){
		api.requester.patch("https://discordapp.com/api/guilds/${member.server.id}/members/{member.id}", ["channel_id": this.id])
	}

	VoiceClient join(Map muteDeaf=[:]){
		api.wsClient.send([
			"op": 4,
			"d": [
				"guild_id": this.server.id,
				"channel_id": this.id,
				"self_mute": muteDeaf["mute"] as boolean,
				"self_deaf": muteDeaf["deaf"] as boolean,
			]
		])
		while (api.voiceData == [:]){}
		api.voiceData << [
			channel: this,
			guild: this.server.id
		]
		Map temp = api.voiceData
		api.voiceData = temp.findAll { k, v -> k != "guild_id" }
		api.voiceClient = new VoiceClient(api)
		return api.voiceClient
	}
}
