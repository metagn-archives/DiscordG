package hlaaftana.discordg.objects

import groovy.lang.Closure;
import groovy.transform.InheritConstructors
import hlaaftana.discordg.Client
import hlaaftana.discordg.VoiceClient
import hlaaftana.discordg.collections.DiscordListCache;
import hlaaftana.discordg.exceptions.MessageInvalidException
import hlaaftana.discordg.exceptions.NoPermissionException
import hlaaftana.discordg.util.JSONUtil
import hlaaftana.discordg.util.ConversionUtil

import java.util.Map;

import com.mashape.unirest.http.Unirest

/**
 * A Discord channel.
 * @author Hlaaftana
 */
class Channel extends DiscordObject {
	Channel(Client client, Map object){
		super(client, object)
	}

	String getMention(){ "<#$id>" }
	Integer getPosition(){ object["position"] }
	Integer getType(){ object.type }
	boolean isText(){ type == 0 }
	boolean isPrivate(){ object.is_private || dm || group }
	boolean isDm(){ type == 1 }
	boolean isVoice(){ type == 2 }
	boolean isGroup(){ type == 3 }
	boolean isInServer(){ text || voice }
	String getTopic(){ object.topic }
	Server getServer(){ dm || group ? null : client.server(serverId) }
	String getServerId(){ object["guild_id"] }
	Server getParent(){ server }
	List<User> getUsers(){
		inServer ? members : (recipients + client.user)
	}
	List<User> getRecipients(){ object.recipients.list }
	Map<String, User> getRecipientMap(){ object.recipients.map }
	User getUser(){ recipients[0] }
	User getRecipient(){ user }
	String getName(){
		dm ? user.name : group ? (object.recipients*.name.join(", ") ?: "Unnamed")
			: object.name
	}

	void addRecipient(user){
		client.addChannelRecipient(this, user)
	}

	void removeRecipient(user){
		client.removeChannelRecipient(this, user)
	}

	List<PermissionOverwrite> getPermissionOverwrites(){
		object["permission_overwrites"]?.list ?: []
	}

	List<PermissionOverwrite> getOverwrites(){ permissionOverwrites }

	Map<String, PermissionOverwrite> getPermissionOverwriteMap(){
		object["permission_overwrites"]?.map ?: [:]
	}

	Map<String, PermissionOverwrite> getOverwriteMap(){ permissionOverwriteMap }

	PermissionOverwrite permissionOverwrite(ass){
		find(object.permission_overwrites, client, ass)
	}

	PermissionOverwrite overwrite(ass){
		permissionOverwrite(ass)
	}

	Permissions permissionsFor(user, Permissions initialPerms){
		if (this.private) return Permissions.PRIVATE_CHANNEL
		Member member = server.member(user)
		if (!member) return Permissions.ALL_FALSE
		Map owMap = overwriteMap
		Permissions doodle = new Permissions(initialPerms.value)
		PermissionOverwrite everyoneOw = owMap[object.guild_id]
		doodle -= everyoneOw.denied
		doodle += everyoneOw.allowed
		List<PermissionOverwrite> roleOws = member.roleIds
			.findAll { owMap.containsKey(it) }.collect { owMap[it] }
		if (roleOws){
			doodle -= (roleOws*.denied*.value).inject { a, b -> a | b }
			doodle += (roleOws*.allowed*.value).inject { a, b -> a | b }
		}
		if (owMap.containsKey(id(user))){
			PermissionOverwrite userOw = owMap[id(user)]
			doodle -= userOw.denied
			doodle += userOw.allowed
		}
		doodle
	}

	Permissions permissionsFor(user){
		permissionsFor(user, server.member(user).permissions)
	}

	boolean canSee(user){
		if (this.private) id(user) in users*.id
		else permissionsFor(user)["readMessages"]
	}

	List<Invite> requestInvites(){
		client.requestChannelInvites(this)
	}

	Invite createInvite(Map data = [:]){
		client.createInvite(data, this)
	}

	void startTyping() {
		client.startTyping(this)
	}

	void delete() {
		client.deleteChannel(this)
	}

	Channel edit(Map data = [:]) {
		client.editChannel(data, this)
	}

	void editPermissions(Map d, t){ client.editChannelOverwrite(d, this, t) }
	void editPermissions(t, Map d){ client.editChannelOverwrite(d, this, t) }
	void addPermissions(Map d, t){ client.editChannelOverwrite(d, this, t) }
	void addPermissions(t, Map d){ client.editChannelOverwrite(d, this, t) }
	void createPermissions(Map d, t){ client.editChannelOverwrite(d, this, t) }
	void createPermissions(t, Map d){ client.editChannelOverwrite(d, this, t) }

	void deletePermissions(t){ client.deleteChannelOverwrite(this, t) }

	Webhook createWebhook(Map data = [:]){
		client.createWebhook(data, this)
	}

	List<Webhook> requestWebhooks(){
		client.requestChannelWebhooks(this)
	}

	Message sendMessage(content, boolean tts=false) {
		client.sendMessage(content: content, tts: tts, this)
	}

	Message sendMessage(Map data){
		client.sendMessage(data, this)
	}

	Message send(Map data){ sendMessage(data) }
	Message send(content, boolean tts = false){ sendMessage(content, tts) }

	Message editMessage(message, content){
		editMessage(message, content: content)
	}

	Message editMessage(Map data, message){
		editMessage(message, data)
	}

	Message editMessage(message, Map data){
		client.editMessage(data, this, message)
	}

	void deleteMessage(message){
		client.deleteMessage(this, message)
	}

	Message sendFile(Map data, implicatedFile, filename = null) {
		client.sendFile(data, this, implicatedFile, filename)
	}

	Message sendFile(implicatedFile, filename = null) {
		sendFile([:], implicatedFile, filename)
	}

	Message requestMessage(message, boolean atc = true){
		client.requestMessage(this, message, atc)
	}

	Message message(message, boolean aa = true){
		client.message(this, message, aa)
	}

	def pinMessage(message){ client.pinMessage(this, message) }
	def pin(message){ pinMessage(message) }

	def unpinMessage(message){ client.unpinMessage(this, message) }
	def unpin(message){ unpinMessage(message) }

	Collection<Message> requestPinnedMessages(){ client.requestPinnedMessages(this) }
	Collection<Message> requestPins(){ requestPinnedMessages() }

	void react(message, emoji){ client.reactToMessage(this, message, emoji) }
	void unreact(m, e, u = "@me"){ client.unreactToMessage(this, m, e, u) }

	List<User> requestReactors(m, e, int l = 100){ client.requestReactors(this, m, e, l) }

	List<Message> requestLogs(int m = 100, b = null,
		boolean a = false){ client.requestChannelLogs(this, m, b, a) }
	List<Message> logs(int m = 100, b = null,
		boolean a = false){ requestLogs(m, b, a) }
	List<Message> forceRequestLogs(int m = 100, b = null, boolean a = false){
		client.forceRequestChannelLogs(m, b, a) }

	List<Message> getCachedLogs(){ client.messages[id]?.list ?: [] }

	Map<String, Message> getCachedLogMap(){ client.messages[id]?.map ?: [:] }

	def clear(int number = 100){ clear(logs(number)*.id) }

	def clear(int number = 100, Closure closure){ clear(logs(number).findAll { closure(it) }) }

	def clear(List ids){
		client.bulkDeleteMessages(this, ids)
	}

	def clear(user, int number = 100){
		clear(number){ it.object.author.id == id(user) }
	}

	Message find(int number = 100, int maxTries = 10, Closure closure){
		List<Message> messages = logs(number)
		Message ass = messages.find { closure(it) }
		if (ass) ass
		else {
			while (!ass){
				if (((messages.size() - 100) / 50) > maxTries) return null
				maxTries++
				messages = logs(number, messages.min { it.id })
				ass = messages.find { closure(it) }
			}
			ass
		}
	}

	String getLastMessageId(){
		object["last_message_id"]
	}

	List<VoiceState> getVoiceStates(){ server.voiceStates.findAll { it.channel == this } }
	List<Member> getMembers(){
		inServer ? (text ? server.members.findAll { canSee(it) } : voiceStates*.member) :
			users
	}
	Map<String, VoiceState> getVoiceStateMap(){ voiceStates.collectEntries { [(it.id): it] } }
	Map<String, Member> getMemberMap(){ members.collectEntries { [(it.id): it] } }

	VoiceState voiceState(thing){ find(voiceStates, voiceStateMap, thing) }
	Member member(thing){ find(members, memberMap, thing) }

	int getBitrate(){ object["bitrate"] }
	int getUserLimit(){ object["user_limit"] }

	boolean isFull(){
		members.size() == userLimit
	}

	boolean canJoin(){
		Permissions ass = server.me.permissionsFor(this)
		ass["connect"] && (userLimit ? full : true || ass["moveMembers"])
	}

	void move(member){
		client.moveMemberVoiceChannel(serverId, member, this)
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

	static Map construct(Client client, Map c, String serverId = null){
		if (serverId) c["guild_id"] = serverId
		if (c["guild_id"]){
			def po = c["permission_overwrites"]
			if (po instanceof List) c["permission_overwrites"] = new DiscordListCache(po.collect { it << [channel_id: c["id"]] }, client, PermissionOverwrite)
		}else{
			c["recipients"] = new DiscordListCache(c["recipients"], client, User)
		}
		c
	}
}

class PermissionOverwrite extends DiscordObject {
	PermissionOverwrite(Client client, Map object){ super(client, object) }

	Permissions getAllowed(){ new Permissions(object["allow"] ?: 0) }
	Permissions getDenied(){ new Permissions(object["deny"] ?: 0) }
	String getType(){ object["type"] }
	DiscordObject getAffected(){
		if (type == "role"){
			channel.server.role(id)
		}else if (type == "member"){
			channel.server.member(id)
		}
	}
	String getChannelId(){ object.channel_id }
	Channel getChannel(){ client.channel(channelId) }
	Channel getParent(){ channel }
	void edit(Map a){
		def de = [allow: allowed, deny: denied]
		client.editChannelOverwrite(a, channelId, id)
	}
	void delete(){ client.deleteChannelOverwrite(channelId, id) }
	String getName(){ affected.name }
	boolean involves(DiscordObject involved){
		if (involved instanceof User){
			if (affected instanceof Role) involved in affected.members
			else involved == affected
		}else if (involed instanceof Role){
			involved == affected
		}
	}
	boolean isRole(){ type == "role" }
	boolean isMember(){ type == "member" }
	boolean isUser(){ type == "member" }
}