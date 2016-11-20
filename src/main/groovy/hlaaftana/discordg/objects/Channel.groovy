package hlaaftana.discordg.objects

import groovy.lang.Closure;
import groovy.transform.InheritConstructors
import hlaaftana.discordg.Client
import hlaaftana.discordg.VoiceClient
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
		super(client, object, "channels/$object.id")
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
	Server getServer(){
		dm || group ? null : client.serverMap[object["guild_id"]]
	}
	Server getParent(){ server }
	List<User> getUsers(){ recipients + client.user }
	List<User> getRecipients(){ object.recipients.collect { new User(client, it) } }
	User getUser(){ recipients[0] }
	User getRecipient(){ user }
	String getName(){
		dm || group ? (object.recipients*.name.join(", ") ?: "Unnamed")
			: object.name
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
		find(permissionOverwrites, permissionOverwriteMap, ass)
	}

	PermissionOverwrite overwrite(ass){
		find(overwrites, overwriteMap, ass)
	}

	Permissions permissionsFor(user){
		if (server == null) return Permissions.PRIVATE_CHANNEL
		else server.member(user).permissionsFor(this)
	}

	Permissions fullPermissionsFor(user){
		if (server == null) return Permissions.PRIVATE_CHANNEL
		else server.member(user).fullPermissionsFor(this)
	}

	List<Invite> getInvites(){
		http.jsonGet("invites").collect { new Invite(client, it) }
	}

	Invite createInvite(Map data = [:]){
		client.createInvite(data, this)
	}

	void startTyping() {
		http.post("typing", [:])
	}

	void delete() {
		http.delete("")
	}

	Channel edit(Map data = [:]) {
		new Channel(client, http.jsonPatch("", patchableObject <<
			ConversionUtil.fixImages(data)))
	}

	void editPermissions(target, allow = 0, deny = 0){
		String id = id(target)
		String type = get(target, Role) ? "role" : "member"
		int allowBytes = allow.toInteger()
		int denyBytes = deny.toInteger()
		http.put("permissions/${id}", [allow: allowBytes, deny: denyBytes, id: id, type: type])
	}

	void addPermissions(target, allow, deny){
		editPermissions(target, allow, deny)
	}

	void createPermissions(target, allow, deny){
		addPermissions target, allow, deny
	}

	void deletePermissions(target){
		http.delete("permissions/${id(target)}")
	}

	Webhook createWebhook(Map data = [:]){
		new Webhook(client, http.jsonPost("webhooks", ConversionUtil.fixImages(data)))
	}

	List<Webhook> requestWebhooks(){
		http.jsonGet("webhooks").collect { new Webhook(client, it) }
	}

	String getQueueName(){ server ? server.id : "dm" }

	Message sendMessage(content, boolean tts=false) {
		sendMessage(content: content, tts: tts)
	}

	Message sendMessage(Map data){
		if (data.content != null){
			data.content = client.filterMessage(data.content)
			if (!data.content || data.content.size() > 2000)
				throw new MessageInvalidException(data.content)
		}
		client.askPool("sendMessages", queueName){
			new Message(client, http.jsonPost("messages", [channel_id: id] << data))
		}
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
		if (data.content != null){
			data.content = client.filterMessage(data.content)
			if (!data.content || data.content.size() > 2000)
				throw new MessageInvalidException(data.content)
		}
		client.askPool("sendMessages", queueName){ // that's right, they're in the same bucket
			new Message(client, http.jsonPatch("messages/${id(message)}", data))
		}
	}

	void deleteMessage(message){
		client.askPool("deleteMessages",
			queueName){ http.delete("messages/${id(message)}") }
	}

	def sendFileRaw(Map data = [:], file){
		List fileArgs = []
		if (file instanceof File){
			if (data["filename"]){
				fileArgs += file.bytes
				fileArgs += data["filename"]
			}else fileArgs += file
		}else{
			fileArgs += ConversionUtil.getBytes(file)
			if (!data["filename"]) throw new IllegalArgumentException("Tried to send non-file class ${file.class} and gave no filename")
			fileArgs += data["filename"]
		}
		def aa = Unirest.post("$http.baseUrl/messages")
			.header("Authorization", client.token)
			.header("User-Agent", client.fullUserAgent)
			.field("content", data["content"] == null ? "" : data["content"].toString())
			.field("tts", data["tts"] as boolean)
		if (fileArgs.size() == 1){
			aa = aa.field("file", fileArgs[0])
		}else if (fileArgs.size() == 2){
			aa = aa.field("file", fileArgs[0], fileArgs[1])
		}
		client.askPool("sendMessages", queueName){
			JSONUtil.parse(aa.asString().body)
		}
	}

	Message sendFile(Map data, implicatedFile, filename = null) {
		def file
		if (implicatedFile.class in [File, String]) file = implicatedFile as File
		else file = ConversionUtil.getBytes(implicatedFile)
		new Message(client, sendFileRaw((filename ? [filename: filename] : [:]) << data, file))
	}

	Message sendFile(implicatedFile, filename = null) {
		sendFile([:], implicatedFile, filename)
	}

	Message requestMessage(message, boolean atc = true){
		def m = new Message(client,
			http.jsonGet("messages/${id(message)}"))
		if (atc){
			if (client.messages[this]) client.messages[this].add(m)
			else {
				client.messages[this] = new DiscordListCache([m], client, Message)
				client.messages[this].root = this
			}
		}
		m
	}

	Message getMessage(message, boolean requestIfNotInCache = true){
		requestIfNotInCache ?
			cachedLogMap[id(message)] ?: requestMessage(message) :
			cachedLogMap[id(message)]
	}

	Message message(message, boolean aa = true){ getMessage message, aa }

	def pinMessage(message){
		http.put("pins/${id(message)}")
	}
	def pin(message){ pinMessage message }

	def unpinMessage(message){
		http.delete("pins/${id(message)}")
	}
	def unpin(message){ unpinMessage message }

	Collection<Message> requestPinnedMessages(){
		http.jsonGet("pins").collect { new Message(client, it) }
	}

	Collection<Message> requestPins(){ requestPinnedMessages() }

	void react(message, emoji){
		http.put("messages/${id(message)}/reactions/${translateEmoji(emoji)}/@me")
	}

	void unreact(message, emoji, user = "@me"){
		http.delete("messages/${id(message)}/reactions/${translateEmoji(emoji)}/${id(user)}")
	}

	List<Reaction> requestReactions(message){ requestMessage(message).reactions }

	List<User> requestReactors(message, emoji, int limit = 100){
		http.getJson("messages/${id(message)}/reactions/${translateEmoji(emoji)}?limit=$limit")
	}

	String translateEmoji(emoji){
		if (emoji ==~ /\d+/){
			translateEmoji(client.emojis.find { it.id == emoji })
		}else if (emoji instanceof Emoji){
			"$emoji.name:$emoji.id"
		}else if (emoji ==~ /\w+/){
			def i = ((server?.emojis ?: []) + (client.emojis - server?.emojis)
				).find { it.name == emoji }?.id
			i ? "$emoji:$i" : ":$emoji:"
		}else{
			emoji
		}
	}

	Collection<Message> getLogs(int max = 100) {
		List aa = cachedLogs
		if (aa.size() > max){
			aa.sort { -it.createTimeMillis }[0..max - 1]
		}else{
			List bb = aa ?
				requestLogs(max - aa.size(), aa.min { it.createTimeMillis }) :
				requestLogs(max - aa.size())
			bb.each {
				if (client.messages[this]) client.messages[this].add(it)
				else {
					client.messages[this] = new DiscordListCache([it], client, Message)
					client.messages[this].root = this
				}
			}
			(aa + bb).sort { -it.createTimeMillis }
		}
	}
	Collection<Message> logs(int max = 100){ getLogs(max) }

	List<Message> requestLogs(int max = 100, boundary = null, boolean after = false){
		rawRequestLogs(max, boundary, after).collect { new Message(client, it) }
	}

	List rawRequestLogs(int max, boundary = null, boolean after = false){
		Map params = [limit: max > 100 ? 100 : max]
		if (boundary){
			if (after) params.after = id(boundary)
			else params.before = id(boundary)
		}
		List messages = rawRequestLogs(params)
		if (max > 100){
			for (int m = 1; m < Math.floor(max / 100); m++){
				messages += rawRequestLogs(before: messages.last().id, limit: 100)
			}
			messages += rawRequestLogs(before: messages.last().id, limit: max % 100 ?: 100)
		}
		messages
	}

	List rawRequestLogs(Map data = [:]){
		String parameters = data ? "?" + data.collect { k, v ->
			URLEncoder.encode(k.toString()) + "=" + URLEncoder.encode(v.toString())
		}.join("&") : ""
		http.jsonGet("messages$parameters")
	}

	List<Message> getCachedLogs(){
		client.messages[id]?.list ?: []
	}

	Map<String, Message> getCachedLogMap(){
		client.messages[id]?.map ?: [:]
	}

	def clear(int number = 100){
		clear(getLogs(number)*.id)
	}

	def clear(int number = 100, Closure closure){
		clear(getLogs(number).findAll { closure(it) })
	}

	def clear(List ids){
		client.askPool("bulkDeleteMessages"){
			http.post("messages/bulk-delete", [messages: ids.collect { id(it) }])
		}
	}

	def clear(user, int number = 100){
		clear(number){ it.author.id == id(user) }
	}

	boolean clearMessagesOf(User user, int messageCount=100){
		try{
			getLogs(messageCount)*.deleteIf { it.author == user }
		}catch (ex){
			return false
		}
		true
	}

	Message find(int number = 100, int increment = 50, int maxTries = 10, Closure closure){
		List<Message> messages = getLogs(number)
		Message ass = messages.find { closure(it) }
		if (ass) ass
		else {
			while (!ass){
				if (((messages.size() - 100) / 50) > maxTries) return null
				number += increment
				messages = getLogs(number)
				ass = messages.find { closure(it) }
			}
			ass
		}
	}

	String getLastMessageId(){
		object["last_message_id"]
	}

	List<VoiceState> getVoiceStates(){ server.voiceStates.findAll { it.channel == this } }
	List<Member> getMembers(){ voiceStates*.member }
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

	static Map construct(Client client, Map c, String serverId = null){
		if (serverId) c["guild_id"] = serverId
		if (c["guild_id"]){
			def po = c["permission_overwrites"]
			if (po instanceof List) c["permission_overwrites"] = new DiscordListCache(po.collect { it << [channel_id: c["id"]] }, client, PermissionOverwrite)
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
			channel.server.roleMap[id]
		}else if (type == "member"){
			channel.server.memberMap[id]
		}
	}
	Channel getChannel(){ client.channelMap[object["channel_id"]] }
	Channel getParent(){ channel }
	void edit(allow = allowed, deny = denied){ channel.editPermissions(affected, allow, deny) }
	void edit(Map a){
		edit(a.allow ?: a.allowed ?: allowed, a.deny ?: a.denied ?: denied)
	}
	void delete(){ channel.deletePermissions(affected) }
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