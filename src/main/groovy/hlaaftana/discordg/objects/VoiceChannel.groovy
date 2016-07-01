package hlaaftana.discordg.objects

import hlaaftana.discordg.Client
import hlaaftana.discordg.VoiceClient;
import hlaaftana.discordg.conn.VoiceWSClient
import hlaaftana.discordg.exceptions.NoPermissionException

import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest
import org.eclipse.jetty.websocket.client.WebSocketClient

/**
 * A voice channel. Extends Channel.
 * @author Hlaaftana
 */
@groovy.transform.InheritConstructors
class VoiceChannel extends Channel {
	List<Server.VoiceState> getVoiceStates(){ server.voiceStates.findAll { it.channel == this } }
	List<Member> getMembers(){ voiceStates*.member }

	Server.VoiceState voiceState(thing){ find(voiceStates, thing) }
	Member member(thing){ find(members, thing) }

	int getBitrate(){ object["bitrate"] }
	int getUserLimit(){ object["user_limit"] }

	boolean isFull(){
		members.size() == userLimit
	}

	boolean canJoin(){
		Permissions ass = server.me.fullPermissionsFor(this)
		ass["connect"] && (userLimit ? full : true || ass["moveMembers"])
	}

	void move(member){
		find(server.members, member)?.moveTo(this)
	}

	VoiceClient join(Map opts = [:]){
		if (!canJoin()) throw new NoPermissionException(full ? "Channel is full" : "Insufficient permissions to join voice channel ${inspect()}")
		VoiceClient vc = new VoiceClient(client, this, opts)
		client.voiceClients[server] = vc
		vc.connect()

		/*Thread.start {
			while (client.voiceData["endpoint"] == null){}
			SslContextFactory sslFactory = new SslContextFactory()
			WebSocketClient client = new WebSocketClient(sslFactory)
			VoiceWSClient socket = new VoiceWSClient(client)
			client.start()
			ClientUpgradeRequest upgreq = new ClientUpgradeRequest()
			client.connect(socket, new URI("wss://" + client.voiceData["endpoint"].replace(":80", "")), upgreq)
			client.voiceWsClient = socket
		}*
		client.voiceClient*/
	}
}
