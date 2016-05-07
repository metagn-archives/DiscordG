package hlaaftana.discordg.objects

import hlaaftana.discordg.conn.VoiceWSClient
import hlaaftana.discordg.objects.VoiceClient
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest
import org.eclipse.jetty.websocket.client.WebSocketClient

/**
 * A voice channel. Extends Channel.
 * @author Hlaaftana
 */
class VoiceChannel extends Channel{
	VoiceChannel(Client client, Map object){ super(client, object) }

	List<Server.VoiceState> getVoiceStates(){ return this.server.voiceStates.findAll { it.channel == this } }
	List<Member> getMembers(){ return this.voiceStates*.member }

	int getBitrate(){ return this.object["bitrate"] }

	void moveMember(Member member){
		client.requester.patch("guilds/${member.server.id}/members/{member.id}", ["channel_id": this.id])
	}

	VoiceClient join(Map muteDeaf=[:]){
		client.wsClient.send([
			"op": 4,
			"d": [
				"guild_id": this.server.id,
				"channel_id": this.id,
				"self_mute": muteDeaf["mute"] as boolean,
				"self_deaf": muteDeaf["deaf"] as boolean,
			]
		])
		while (client.voiceData == [:]){}
		client.voiceData << [
			channel: this,
			guild: this.server
		]
		Map temp = client.voiceData
		client.voiceData = temp.findAll { k, v -> k != "guild_id" }
		client.voiceClient = new VoiceClient(client)
		/*Thread.start {
			while (client.voiceData["endpoint"] == null){}
			SslContextFactory sslFactory = new SslContextFactory()
			WebSocketClient client = new WebSocketClient(sslFactory)
			VoiceWSClient socket = new VoiceWSClient(client)
			client.start()
			ClientUpgradeRequest upgreq = new ClientUpgradeRequest()
			client.connect(socket, new URI("wss://" + client.voiceData["endpoint"].replace(":80", "")), upgreq)
			client.voiceWsClient = socket
		}*/
		return client.voiceClient
	}
}
