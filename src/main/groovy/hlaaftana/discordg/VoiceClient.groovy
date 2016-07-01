package hlaaftana.discordg

import groovy.lang.Closure;
import hlaaftana.discordg.conn.VoiceWSClient
import hlaaftana.discordg.objects.*
import hlaaftana.discordg.util.AudioUtil
import hlaaftana.discordg.util.Log

class VoiceClient extends DiscordObject {
	String endpoint
	String getEndpoint(){ fixEndpoint(this.@endpoint) }
	Log log
	int ssrc
	boolean voiceStateUpdated
	boolean voiceServerUpdated
	VoiceWSClient wsClient
	ParentListenerSystem listenerSystem = new ParentListenerSystem(this)

	VoiceClient(Map opts = [:], Client client, VoiceChannel channel){
		super(client, [
			id: channel.server.id,
			name: channel.server.name,
			guild_id: channel.server.id,
			channel_id: channel.id,
			self_mute: opts.mute as boolean,
			self_deaf: opts.deaf as boolean
		])
		log = new Log(client.log)
		log.name += "Voice-Server($id)"
	}

	VoiceClient(Client client, VoiceChannel channel, Map opts){
		this(opts, client, channel)
	}

	@groovy.transform.Memoized
	static String fixEndpoint(String endp){
		String ass = endp
		if (!ass.startsWith("wss://")) ass = "wss://$ass"
		ass.replaceAll(/:\d+/, "")
	}

	Server getServer(){ client.server(object["guild_id"]) }
	VoiceChannel getChannel(){ client.voiceChannel(object["channel_id"]) }

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
		client.wsClient.send op: 4, d: object
		this
	}

	VoiceClient update(){
		if (wsClient?.session) client.wsClient.send op: 4, d: object
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

	def addListener(event, boolean temporary = false, Closure closure) {
		listenerSystem.addListener(event, temporary, closure)
	}

	def removeListener(event, Closure closure) {
		listenerSystem.removeListener(event, closure)
	}

	def removeListenersFor(event){
		listenerSystem.removeListenersFor(event)
	}

	def removeAllListeners(){
		listenerSystem.removeAllListeners()
	}

	def dispatchEvent(type, data){
		listenerSystem.dispatchEvent(type, data)
	}
}

enum VoiceEvents {
	READY(2),
	PING(3),
	CONNECT(4),
	SPEAKING(5)

	int op
	VoiceEvents(int op){ this.op = op }

	static VoiceEvents get(op){ op instanceof VoiceEvents ? op :
		op instanceof String ? VoiceEvents.values().find { it.name() == op.trim().toUpperCase() }
		: VoiceEvents.values().find { it.op == op } }
}