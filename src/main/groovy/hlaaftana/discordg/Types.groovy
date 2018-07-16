package hlaaftana.discordg

interface Types {}

interface DefaultMessageNotificationLevelTypes extends Types {
	final int ALL_MESSAGES = 0
	final int ONLY_MENTIONS = 1
}

interface ExplicitContentFilterLevelTypes extends Types {
	final int DISABLED = 0
	final int MEMBERS_WITHOUT_ROLES = 1
	final int ALL_MEMBERS = 2
}

interface VerificationLevelTypes extends Types {
	final int NONE = 0
	final int LOW = 1
	final int MEDIUM = 2
	final int HIGH = 3
	final int VERY_HIGH = 4
}

interface MFALevelTypes extends Types {
	final int NONE = 0
	final int ELEVATED = 1
}

interface ChannelTypes extends Types {
	final int TEXT = 0
	final int DM = 1
	final int VOICE = 2
	final int GROUP = 3
	final int CATEGORY = 4
}

interface MessageTypes extends Types {
	final int NORMAL = 0
	final int RECIPIENT_ADD = 1
	final int RECIPIENT_REMOVE = 2
	final int CALL = 3
	final int CHANNEL_NAME_CHANGE = 4
	final int CHANNEL_ICON_CHANGE = 5
}

interface OverwriteTypes extends Types {
	final String MEMBER = 'member'
	final String USER = 'member'
	final String ROLE = 'role'
}

interface RelationshipTypes extends Types {
	final int NONE = 0
	final int FRIEND = 1
	final int BLOCKED = 2
	final int PENDING_INCOMING = 3
	final int PENDING_OUTGOING = 4
}

interface GameTypes extends Types {
	final int NORMAL = 0
	final int STREAMING_TWITCH = 1
}

interface StatusTypes extends Types {
	final String ONLINE = 'online'
	final String IDLE = 'idle'
	final String AWAY = IDLE
	final String DND = 'dnd'
	final String DO_NOT_DISTURB = DND
	final String INVISIBLE = 'invisible' // will be 'offline' if from another member
	final String OFFLINE = 'offline'
}

interface ConnectionTypes extends Types {
	final String YOUTUBE = 'youtube'
	final String TWITCH = 'twitch'
}

interface OAuthScopes extends Types {
	final String IDENTIFY = 'identify'
	final String EMAIL = 'email'
	final String CONNECTIONS = 'connections'
	final String GUILDS = 'guilds'
	final String JOIN_GUILDS = 'guilds.join'
	final String RPC = 'rpc'
	final String RPC_API = 'rpc.api'
	final String BOT = 'bot'
}

interface DefaultAvatarTypes extends Types {
	final int BLUE = 0
	final int GREY = 1
	final int GRAY = 1
	final int GREEN = 2
	final int YELLOW = 3
	final int RED = 4
}