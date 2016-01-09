package ml.hlaaftana.discordg.util

import java.time.*
import java.util.logging.Level

import ml.hlaaftana.discordg.objects.API

/**
 * Adds methods to provide ease for logging.
 * @author Hlaaftana
 */
class Log {
	static boolean info = true
	static boolean error = true
	static boolean warn = true
	static boolean debug = false

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
		if (info)
			log(str, "info", by)
	}

	/**
	 * Prints out a log entry with the level "debug". Toggle with the static boolean "Log.debug".
	 * @param str - Your message.
	 * @param by - What the name of the logger will be. "DiscordG" by default.
	 */
	static debug(String str, String by="DiscordG"){
		if (debug)
			log(str, "debug", by)
	}

	/**
	 * Prints out a log entry with the level "error". Toggle with the static boolean "Log.error".
	 * @param str - Your message.
	 * @param by - What the name of the logger will be. "DiscordG" by default.
	 */
	static error(String str, String by="DiscordG"){
		if (error)
			log(str, "error", by)
	}

	/**
	 * Prints out a log entry with the level "warn". Toggle with the static boolean "Log.warn".
	 * @param str - Your message.
	 * @param by - What the name of the logger will be. "DiscordG" by default.
	 */
	static warn(String str, String by="DiscordG"){
		if (warn)
			log(str, "warn", by)
	}
}
