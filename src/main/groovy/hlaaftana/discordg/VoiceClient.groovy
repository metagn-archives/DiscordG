package hlaaftana.discordg

import groovy.lang.Closure
import hlaaftana.discordg.logic.ParentListenerSystem;
import hlaaftana.discordg.net.VoiceWSClient
import hlaaftana.discordg.objects.*
//import hlaaftana.discordg.util.AudioUtil
import hlaaftana.discordg.util.Log
import java.nio.ByteBuffer

class VoiceClient extends DiscordObject {
	String endpoint
	String getEndpoint(){ fixEndpoint(this.@endpoint) }
	int port
	long heartbeatInterval
	BigInteger ssrc
	List<Long> pingIntervals = []
	boolean voiceStateUpdated
	boolean voiceServerUpdated
	VoiceWSClient ws
	String encryptionMode = "xsalsa20_poly1305"
	@Delegate(excludes = ["parseEvent", "listenerError", "toString"])
	ParentListenerSystem listenerSystem = new ParentListenerSystem(this)

	String logName = client.logName + "Voice($id)"
	Log log

	VoiceClient(Map opts = [:], Client client, Channel channel){
		super(client, [
			id: channel.server?.id ?: channel.id,
			name: channel.server?.name ?: channel.name,
			guild_id: channel.server?.id,
			channel_id: channel.id,
			self_mute: opts.mute as boolean,
			self_deaf: opts.deaf as boolean
		])
		opts.each { k, v ->
			try {
				this[k] = v
			}catch (MissingPropertyException ex){}
		}
		if (!log){
			log = new Log(client.log)
			log.name = logName
		}
	}

	VoiceClient(Client client, Channel channel, Map opts){
		this(opts, client, channel)
	}

	@groovy.transform.Memoized
	static String fixEndpoint(String endp){
		String ass = endp
		if (!ass.startsWith("wss://")) ass = "wss://$ass"
		ass.replaceAll(/:\d+/, "")
	}

	Server getServer(){ client.server(object["guild_id"]) }
	Channel getChannel(){ client.channel(object["channel_id"]) }

	boolean isMuted(){
		object["self_mute"] as boolean
	}
	boolean isSelfMuted(){ muted }
	boolean isMute(){ muted }
	boolean isSelfMute(){ muted }
	boolean isDeaf(){
		object["self_deaf"] as boolean
	}
	boolean isSelfDeaf(){ deaf }
	boolean isDeafened(){ deaf }
	boolean isSelfDeafened(){ deaf }

	def handleVoiceServerUpdate(Map data){
		endpoint = data.endpoint
		voiceServerUpdated = true
	}

	def handleVoiceStateUpdate(Map data){
		voiceStateUpdated = true
	}

	VoiceClient connect(){
		client.ws.send op: 4, d: object
		this
	}

	VoiceClient move(channel){
		object.channel_id = id(channel)
		update()
	}

	VoiceClient disconnect(){
		move(null)
	}

	VoiceClient update(){
		if (ws?.session) client.ws.send op: 4, d: object
		this
	}

	VoiceClient mute(){
		object["mute"] = true
		update()
	}

	VoiceClient unmute(){
		object["mute"] = false
		update()
	}

	VoiceClient deafen(){
		object["deaf"] = true
		update()
	}

	VoiceClient undeafen(){
		object["deaf"] = false
		update()
	}

	VoiceClient muteAndDeafen(){
		object["mute"] = true
		object["deaf"] = true
		update()
	}

	VoiceClient unmuteAndUndeafen(){
		object["mute"] = false
		object["deaf"] = false
		update()
	}

	String parseEvent(param){
		VoiceEvents.get(param).name()
	}

	def listenerError(String event, Throwable ex, Closure closure, data) {
		ex.printStackTrace()
		log.info "Ignoring exception from event $event"
	}
}

enum VoiceEvents {
	READY(2),
	PING(3),
	CONNECT(4),
	SPEAKING(5),
	CLOSE(-1),
	UNKNOWN(-100)

	int op
	VoiceEvents(int op){ this.op = op }

	static VoiceEvents get(op){
		{ ->
			if (op instanceof VoiceEvents) op
			else if (op instanceof String){
				op = op.trim()
				if (op.number) VoiceEvents.values().find { it.op == op.toInteger() }
				else VoiceEvents.values().find { it.name() == op }
			}else VoiceEvents.values().find { it.op == op.toInteger() }
		}() ?: UNKNOWN
	}
}