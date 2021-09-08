package hlaaftana.discordg.data

import groovy.transform.CompileStatic
import groovy.transform.Memoized

@CompileStatic
class Permissions {
	static final Permissions ALL_FALSE = new Permissions(0)
	static final Permissions ALL_TRUE = new Permissions(0b1111111111101111111110001111111)
	static final Permissions CHANNEL_ALL_TRUE = new Permissions(0b0010011111101111111110001011001)
	static final Permissions TEXT_ALL_TRUE = new Permissions(0b0000000000001111111110001011001)
	static final Permissions VOICE_ALL_TRUE = new Permissions(0b0000011111100000000000000011001)
	static final Permissions ROLE_ALL_TRUE = new Permissions(0b1111111111101111111110001111111)
	static final Permissions PRIVATE_CHANNEL = new Permissions(0b0000000000000011101110000000000)

	long value
	Permissions(Map<String, Boolean> defaults) { for (e in defaults) set(e.key, e.value) }
	Permissions(long v = 0) { value = v }
	Permissions(Permissions perms) { this(perms.value) }
	Permissions(Permission... permissions) { for (final p : permissions) add(p) }

	int size() { Long.bitCount(value) }
	boolean isEmpty() { value == 0 }
	boolean contains(Permission o) { has(o) }
	Iterator<Permission> iterator() {
		new Iterator<Permission>() {
			int i = 0

			boolean hasNext() {
				i < Permission.values().length
			}

			@Override
			Permission next() {
				while (!has(i)) { ++i }
				Permission.values()[i++]
			}
		}
	}

	@Deprecated
	Map<BitOffsets, Boolean> getOffsetMap() {
		Map<BitOffsets, Boolean> r = new HashMap<>()
		for (b in BitOffsets.values()) {
			r.put(b, get(b.offset))
		}
		r
	}

	@Deprecated
	Map<String, Boolean> getLocalizedMap() {
		Map<String, Boolean> a = new HashMap<>()
		for (e in offsetMap) for (l in e.key.locals) a.put(l, e.value)
		a
	}

	@Deprecated
	Map<String, Boolean> getMap() { localizedMap }

	@Deprecated
	Permissions set(String permissionName, boolean truth = true) {
		putAt(BitOffsets.get(permissionName).offset, truth)
		this
	}

	Permissions set(Permission permission) {
		value |= permission.value()
		this
	}

	Permissions leftShift(Permission permission) { set(permission) }
	Permissions leftShift(Permissions permissions) { value |= permissions.value; this }

	void add(Permission permission) {
		set(permission)
	}

	void add(Permissions permissions) {
		value |= permissions.value
	}

	void remove(Permission permission) {
		// https://issues.apache.org/jira/browse/GROOVY-9704
		value &= permission.value().bitwiseNegate()
	}

	void remove(Permissions permissions) {
		value &= permissions.value.bitwiseNegate()
	}

	Permissions or(Permissions other) { new Permissions(value | other.value) }
	Permissions and(Permissions other) { new Permissions(value & other.value) }
	Permissions minus(Permissions other) { new Permissions(value & other.value.bitwiseNegate()) }

	Permissions or(Permission other) { new Permissions(value | other.value()) }
	Permissions and(Permission other) { new Permissions(value & other.value()) }
	Permissions minus(Permission other) { new Permissions(value & other.value().bitwiseNegate()) }

	Permissions plus(Permissions other) { or(other) }
	Permissions multiply(Permissions other) { and(other) }

	Permissions plus(Permission other) { or(other) }
	Permissions multiply(Permission other) { and(other) }

	boolean get(BitOffsets bitOffset) { getAt(BitOffsets.get(bitOffset).offset) }
	boolean get(bitOffset) { getAt(BitOffsets.get(bitOffset).offset) }
	boolean get(Permission index) { (value & index.value()) != 0 }
	boolean get(int index) { ((value >> index) & 1) == 1 }

	boolean has(BitOffsets w) { get(w) }
	boolean has(Permission w) { get(w) }
	boolean has(int w) { get(w) }
	boolean has(w) { get(w) }

	boolean getAt(BitOffsets w) { get(w) }
	boolean getAt(Permission w) { get(w) }
	boolean getAt(int w) { get(w) }
	boolean getAt(w) { get(w) }

	int putAt(int index, boolean truth) {
		if (truth) value |= (1 << index)
		else value &= (1 << index).bitwiseNegate()
		value
	}

	Permissions putAt(String permissionName, boolean truth) {
		set(permissionName, truth)
	}

	int hashCode() { Long.hashCode(value) }
	boolean equals(other) {
		(other instanceof Permissions && value == other.value) || (other instanceof Number && value == other.intValue())
	}

	def propertyMissing(String bitOffset) {
		get(bitOffset)
	}

	def propertyMissing(String bitOffset, value) {
		set(bitOffset, value as boolean)
	}

	def asType(Class target) {
		switch (target) {
			case int:
			case Integer:
				return (int) value
			case long:
			case Long:
				return (long) value
			default:
				super.asType(target)
		}
	}

	String toString() { map.toString() }
	long toLong() { value }

	@Deprecated
	static enum BitOffsets {
		CREATE_INSTANT_INVITE('createInstantInvite'),
		KICK_MEMBERS('kick'),
		BAN_MEMBERS('ban'),
		ADMINISTRATOR('administrator'),
		MANAGE_CHANNELS('manageChannel', 'manageChannels'),
		MANAGE_GUILD('manageGuild'),
		ADD_REACTIONS('addReactions', 'react'),
		VIEW_AUDIT_LOG('viewAuditLog', 'auditLog'),
		PRIORITY_SPEAKER('prioritySpeaker'),
		STREAM('stream'),
		READ_MESSAGES('readMessages'),
		SEND_MESSAGES('sendMessages'),
		SEND_TTS_MESSAGES('sendTts'),
		MANAGE_MESSAGES('manageMessages', 'deleteMessages'),
		EMBED_LINKS('embed', 'embedLinks'),
		ATTACH_FILES('sendFiles', 'attachFiles'),
		READ_MESSAGE_HISTORY('readMessageHistory'),
		MENTION_EVERYONE('mentionEveryone'),
		USE_EXTERNAL_EMOJI('useExternalEmoji'),
		VIEW_GUILD_INSIGHTS('viewGuildInsights', 'guildInsights'),
		CONNECT('connect'),
		SPEAK('speak', 'talk'),
		MUTE_MEMBERS('mute'),
		DEAFEN_MEMBERS('deafen'),
		MOVE_MEMBERS('move'),
		USE_VAD('voiceActivation'),
		CHANGE_NICKNAME('changeNick', 'changeOwnNick'),
		MANAGE_NICKNAMES('changeNicks', 'manageNicks'),
		MANAGE_ROLES('manageRoles', 'managePermissions'),
		MANAGE_WEBHOOKS('manageWebhooks'),
		MANAGE_EMOJIS_AND_STICKERS('manageEmojis', 'manageEmoji', 'manageEmojisAndStickers'),
		USE_APPLICATION_COMMANDS('useApplicationCommands', 'applicationCommands'),
		REQUEST_TO_SPEAK('requestToSpeak'),
		MANAGE_THREADS('manageThreads'),
		USE_PUBLIC_THREADS('usePublicThreads', 'publicThreads'),
		USE_PRIVATE_THREADS('usePrivateThreads', 'privateThreads'),
		USE_EXTERNAL_STICKERS('useExternalStickers', 'externalStickers')

		Set<String> locals
		BitOffsets(List<String> locals) {
			this.locals = new HashSet<>(locals)
		}
		
		BitOffsets(String... locals) { this(Arrays.asList(locals)) }
		BitOffsets(String local) { this([local]) }

		int getOffset() { ordinal() }
		int value() { 1 << offset }

		@Memoized
		static BitOffsets get(thing) {
			if (thing instanceof Number) {
				for (v in values()) if (v.offset == thing as int) return v
				null
			}
			else if (thing instanceof String) {
				for (v in values()) if (v.locals.contains((String) thing)) return v
				null
			}
			else if (thing instanceof BitOffsets) (BitOffsets) thing
			else if (thing instanceof Permission) values()[thing.ordinal()]
			else null
		}
	}
}

@CompileStatic
enum Permission {
	CREATE_INSTANT_INVITE,
	KICK_MEMBERS,
	BAN_MEMBERS,
	ADMINISTRATOR,
	MANAGE_CHANNELS,
	MANAGE_GUILD,
	ADD_REACTIONS,
	VIEW_AUDIT_LOG,
	PRIORITY_SPEAKER,
	STREAM,
	READ_MESSAGES,
	SEND_MESSAGES,
	SEND_TTS_MESSAGES,
	MANAGE_MESSAGES,
	EMBED_LINKS,
	ATTACH_FILES,
	READ_MESSAGE_HISTORY,
	MENTION_EVERYONE,
	USE_EXTERNAL_EMOJI,
	VIEW_GUILD_INSIGHTS,
	CONNECT,
	SPEAK,
	MUTE_MEMBERS,
	DEAFEN_MEMBERS,
	MOVE_MEMBERS,
	USE_VAD,
	CHANGE_NICKNAME,
	MANAGE_NICKNAMES,
	MANAGE_ROLES,
	MANAGE_WEBHOOKS,
	MANAGE_EMOJIS_AND_STICKERS,
	USE_APPLICATION_COMMANDS,
	REQUEST_TO_SPEAK,
	MANAGE_THREADS,
	USE_PUBLIC_THREADS,
	USE_PRIVATE_THREADS,
	USE_EXTERNAL_STICKERS

	int value() { 1 << ordinal() }
}
