package hlaaftana.discordg.objects

class Permissions {
	static final Permissions ALL_FALSE = new Permissions(0)
	static final Permissions ALL_TRUE = new Permissions(0b1111111100111111110000111111)
	static final Permissions CHANNEL_ALL_TRUE = new Permissions(0b0011111100111111110000011001)
	static final Permissions GENERAL_ALL_TRUE = new Permissions(0b1100000000000000000000111111)
	static final Permissions TEXT_ALL_TRUE = new Permissions(0b0000000000111111110000011001)
	static final Permissions VOICE_ALL_TRUE = new Permissions(0b0011111100000000000000011001)
	static final Permissions ROLE_ALL_TRUE = new Permissions(0b1111111100111111110000111111)
	String bytes
	int value

	Permissions(int value=0){ this.value = value }
	Permissions(Map defaults){ defaults.each { k, v -> this.set(k, v) } }

	Map getMap(){
		Map<BitOffsets, Boolean> offsets = [:]
		BitOffsets.BIT_COUNT.times {
			offsets[BitOffsets.get(it)] = this[it]
		}
		Map a = [:]
		offsets.each { k, v ->
			k.locals.each {
				a[it] = v
			}
		}
		return a
	}

	Permissions set(String permissionName, boolean truth){
		this[BitOffsets.get(permissionName).offset] = truth
		return this
	}

	Permissions plus(Permissions other){
		return new Permissions(this.value | other.value)
	}

	Permissions minus(Permissions other){
		return new Permissions(this.value & ~other.value)
	}

	boolean getAt(String permissionName){
		return this.map[permissionName]
	}

	boolean getAt(int index){
		return ((this.value >> index) & 1) as boolean
	}

	int putAt(int index, boolean truth){
		if (truth) this.value |= (1 << index)
		else this.value &= ~(1 << index)
		return this.value
	}

	Permissions putAt(String permissionName, boolean truth){
		return set(permissionName, truth)
	}

	def asType(Class target){
		switch (target){
			case int:
			case Integer:
				return this.value
			case long:
			case Long:
				return this.value as Long
			default:
				return super.asType(target)
		}
	}

	String toString(){ return this.map.toString() }

	static enum BitOffsets {
		CREATE_INSTANT_INVITE("createInstantInvite", 0),
		KICK_MEMBERS("kick", 1),
		BAN_MEMBERS("ban", 2),
		ADMINISTRATOR(["administrator", "managePermissions"], 3),
		MANAGE_CHANNELS(["manageChannel", "manageChannels"], 4),
		MANAGE_GUILD("manageServer", 5),
		READ_MESSAGES("readMessages", 10),
		SEND_MESSAGES("sendMessages", 11),
		SEND_TTS_MESSAGES("sendTts", 12),
		MANAGE_MESSAGES(["manageMessages", "deleteMessages"], 13),
		EMBED_LINKS(["embed", "embedLinks"], 14),
		ATTACH_FILES(["sendFiles", "attachFiles"], 15),
		READ_MESSAGE_HISTORY("readMessageHistory", 16),
		MENTION_EVERYONE("mentionEveryone", 17),
		CONNECT("connect", 20),
		SPEAK(["speak", "talk"], 21),
		MUTE_MEMBERS("mute", 22),
		DEAFEN_MEMBERS("deafen", 23),
		MOVE_MEMBERS("move", 24),
		USE_VAD("voiceActivation", 25),
		CHANGE_NICKNAME(["changeNick", "changeOwnNick"], 26),
		MANAGE_NICKNAMES(["changeNicks", "manageNicks"], 27),
		MANAGE_ROLES("manageRoles", 28)

		static final int BIT_COUNT = 28

		List locals
		int offset
		BitOffsets(locals, int offset){ this.offset = offset; this.locals = locals instanceof Iterable ? locals.collect { it } : [locals] }

		static BitOffsets get(thing){
			if (thing instanceof Number){ return BitOffsets.class.enumConstants.find { it.offset == thing } }
			if (thing instanceof String){ return BitOffsets.class.enumConstants.find { thing in it.locals } }
			if (thing instanceof BitOffsets){ return thing }
		}
	}
}
