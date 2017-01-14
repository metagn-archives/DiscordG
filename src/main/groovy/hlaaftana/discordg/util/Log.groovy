package hlaaftana.discordg.util

import java.time.LocalDateTime

import hlaaftana.discordg.collections.DynamicList

/**
 * A groovy way to log messages.
 * @author Hlaaftana
 */
class Log {
	static class Level {
		String name
		boolean enabled = true

		Level enable(){ enabled = true; this }
		Level disable(){ enabled = false; this }
		boolean equals(Level other){ name == other.name }
	}

	static class Message {
		String by
		Level level
		String content
		LocalDateTime time = LocalDateTime.now()
		Map info = [:]

		String toString(){ toString(Log.defaultFormatter) }
		String toString(Log log){ toString(log.formatter) }
		String toString(Closure formatter){ formatter(this) }
	}

	static DynamicList defaultLevels = [
		new Level(name: "info"),
		new Level(name: "error"),
		new Level(name: "warn"),
		new Level(name: "debug").disable(),
		new Level(name: "trace").disable()
	]

	static Closure defaultFormatter = { Message message ->
		String.format("<%s|%s> [%s] [%s]: %s",
			message.time.toLocalDate(),
			message.time.toLocalTime(),
			message.level.name.toUpperCase(),
			message.by, message.content)
	}

	Closure formatter = defaultFormatter

	DynamicList levels = defaultLevels

	DynamicList messages = []

	DynamicList listeners = [{ if (it.level.enabled) println formatter(it) }]

	String name
	Log(String name){ this.name = name }

	Log(Log parent){
		formatter = parent.formatter
		name = parent.name
	}

	def listen(Closure ass){
		listeners.add ass
	}

	def call(Message message){
		listeners.each { it message }
	}

	def level(String name){
		Level ass = levels.find("name", name)
		if (!ass){
			ass = new Level(name: name)
			levels.add ass
		}
		ass
	}

	def level(Level level){
		if (level in levels) level
		else {
			levels.add level
			level
		}
	}

	def propertyMissing(String name){
		level(name)
	}

	def methodMissing(String name, args){
		Level level = propertyMissing(name)
		boolean argsIsMultiple = args instanceof Collection || args.class.array
		if (args instanceof Message || (argsIsMultiple && args[0] instanceof Message)){
			log(args)
		}else{
			List ahh = [level] + (argsIsMultiple ? args as List : args)
			log(ahh)
		}
	}

	def log(Level level, content, String by = name){
		Message ass = new Message(level: level, content: content.toString(), by: by)
		log(ass)
	}

	def log(Message message){
		messages += message
		call(message)
	}
}
