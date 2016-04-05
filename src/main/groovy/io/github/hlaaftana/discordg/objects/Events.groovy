package io.github.hlaaftana.discordg.objects

enum Events {
	READY("READY"),
	MESSAGE_CREATE("MESSAGE_CREATE"),
	MESSAGE("MESSAGE_CREATE"),
	MESSAGE_DELETE("MESSAGE_DELETE"),
	MESSAGE_UPDATE("MESSAGE_UPDATE"),
	MESSAGE_ACK("MESSAGE_ACK"),
	CHANNEL_CREATE("CHANNEL_CREATE"),
	CHANNEL_UPDATE("CHANNEL_UPDATE"),
	CHANNEL_DELETE("CHANNEL_DELETE"),
	GUILD_BAN_ADD("GUILD_BAN_ADD"),
	GUILD_BAN_REMOVE("GUILD_BAN_REMOVE"),
	GUILD_CREATE("GUILD_CREATE"),
	GUILD_UPDATE("GUILD_UPDATE"),
	GUILD_DELETE("GUILD_DELETE"),
	GUILD_INTEGRATIONS_UPDATE("GUILD_INTEGRATIONS_UPDATE"),
	GUILD_EMOJIS_UPDATE("GUILD_EMOJIS_UPDATE"),
	GUILD_MEMBER_ADD("GUILD_MEMBER_ADD"),
	GUILD_MEMBER_UPDATE("GUILD_MEMBER_UPDATE"),
	GUILD_MEMBER_REMOVE("GUILD_MEMBER_REMOVE"),
	GUILD_ROLE_CREATE("GUILD_ROLE_CREATE"),
	GUILD_ROLE_UPDATE("GUILD_ROLE_UPDATE"),
	GUILD_ROLE_DELETE("GUILD_ROLE_DELETE"),
	PRESENCE_UPDATE("PRESENCE_UPDATE"),
	TYPING_START("TYPING_START"),
	VOICE_STATE_UPDATE("VOICE_STATE_UPDATE"),
	VOICE_SERVER_UPDATE("VOICE_SERVER_UPDATE"),
	USER_UPDATE("USER_UPDATE"),
	USER_GUILD_SETTINGS_UPDATE("USER_GUILD_SETTINGS_UPDATE"),
	USER_SETTINGS_UPDATE("USER_SETTINGS_UPDATE"),
	UNRECOGNIZED("UNRECOGNIZED"),
	ALL("ALL")

	String type
	Events(String type){this.type = type}

	/**
	 * Returns an event name from a string by; <br>
	 * 1. Replacing "change" with "update" and "server" with "guild" (case insensitive), <br>
	 * 2. Making it uppercase, and <br>
	 * 3. Replacing spaces with underscores.
	 * @param str - the string.
	 * @return the event name.
	 */
	static String parseEventType(String str){
		return str.toUpperCase().replaceAll(/\s+/, '_').replace("CHANGE", "UPDATE").replaceAll(/^(?!VOICE_)SERVER/, "GUILD")
	}

	static Events get(type){ return (type instanceof Events) ? type : Events.class.enumConstants.find { it.type == this.parseEventType(type.toString()) } ?: Events.UNRECOGNIZED }
}
