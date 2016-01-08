package ml.hlaaftana.discordg.util

import java.time.*
import java.util.logging.Level

import ml.hlaaftana.discordg.objects.API

class Log {
	static log(String str, String level, String by="DiscordG"){
		println String.format("<%s|%s> [%s] [%s]: %s", LocalDate.now(), LocalTime.now(), level.toUpperCase(), by, str)
	}

	static info(String str, String by="DiscordG"){
		log(str, "info", by)
	}

	static debug(String str, String by="DiscordG"){
		if (API.debug){
			log(str, "debug", by)
		}
	}

	static error(String str, String by="DiscordG"){
		log(str, "error", by)
	}
}
