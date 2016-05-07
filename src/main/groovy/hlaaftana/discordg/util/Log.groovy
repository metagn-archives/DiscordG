package io.github.hlaaftana.discordg.util

import java.time.*
import java.util.logging.Level

import io.github.hlaaftana.discordg.objects.Client

/**
 * Adds methods to provide ease for logging.
 * @author Hlaaftana
 */
class Log {
	static boolean enableInfo = true
	static boolean enableError = true
	static boolean enableWarn = true
	static boolean enableDebug = false
	static boolean enableListenerCrashes = true
	static boolean enableEventRegisteringCrashes = false

	/**
	 * Prints out a log entry.
	 * @param str - Your message.
	 * @param level - The level of the entry. String.
	 * @param by - What the name of the logger will be. "DiscordG" by default.
	 */
	static log(String str, String level, String by="DiscordG"){
		println String.format("<%s|%s> [%s] [%s]: %s", LocalDate.now(), LocalTime.now(), level.toUpperCase(), by, str)
	}

	/**
	 * Prints out a log entry with the level "info". Toggle with the static boolean "Log.info".
	 * @param str - Your message.
	 * @param by - What the name of the logger will be. "DiscordG" by default.
	 */
	static info(String str, String by="DiscordG"){
		if (enableInfo)
			log(str, "info", by)
	}

	/**
	 * Prints out a log entry with the level "debug". Toggle with the static boolean "Log.debug".
	 * @param str - Your message.
	 * @param by - What the name of the logger will be. "DiscordG" by default.
	 */
	static debug(String str, String by="DiscordG"){
		if (enableDebug)
			log(str, "debug", by)
	}

	/**
	 * Prints out a log entry with the level "error". Toggle with the static boolean "Log.error".
	 * @param str - Your message.
	 * @param by - What the name of the logger will be. "DiscordG" by default.
	 */
	static error(String str, String by="DiscordG"){
		if (enableError)
			log(str, "error", by)
	}

	/**
	 * Prints out a log entry with the level "warn". Toggle with the static boolean "Log.warn".
	 * @param str - Your message.
	 * @param by - What the name of the logger will be. "DiscordG" by default.
	 */
	static warn(String str, String by="DiscordG"){
		if (enableWarn)
			log(str, "warn", by)
	}
}
