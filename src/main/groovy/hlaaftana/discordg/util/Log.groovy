package hlaaftana.discordg.util

import groovy.transform.CompileStatic

import java.time.LocalDateTime

@CompileStatic
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

		String toString(){ toString(defaultFormatter) }
		String toString(Log log){ toString(log.formatter) }
		String toString(Closure formatter){ formatter(this) }
	}

	static List<Level> defaultLevels = [
		new Level(name: 'info'),
		new Level(name: 'error'),
		new Level(name: 'warn'),
		new Level(name: 'debug').disable(),
		new Level(name: 'trace').disable()
	]

	static Closure<String> defaultFormatter = { Message message ->
		String.format('<%s|%s> [%s] [%s]: %s',
			message.time.toLocalDate(),
			message.time.toLocalTime(),
			message.level.name.toUpperCase(),
			message.by, message.content)
	}

	Closure<String> formatter = defaultFormatter

	List<Level> levels = defaultLevels

	List<Message> messages = []

	List<Closure> listeners = [{ Message it -> if (it.level.enabled) println formatter.call(it) }]

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
		for (it in listeners) { it message }
	}

	Level level(String name){
		Level ass = levels.find { it.name == name }
		if (null == ass) {
			ass = new Level(name: name)
			levels.add ass
		}
		ass
	}

	Level level(Level level){
		if (level in levels) level
		else {
			levels.add level
			level
		}
	}

	Level propertyMissing(String name){
		level(name)
	}

	def methodMissing(String name, args){
		Level level = (Level) propertyMissing(name)
		if (args.class.array) args = ((Object[]) args).toList()
		boolean argsIsMultiple = args instanceof Collection
		if (args instanceof Message || (argsIsMultiple && args.first() instanceof Message)){
			invokeMethod('log', args)
		}else{
			def ahh = argsIsMultiple ? [level, *args] : [level, args]
			invokeMethod('log', ahh)
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

	def info(content, String by = name) {
		log(level('info'), content, by)
	}

	def warn(content, String by = name) {
		log(level('warn'), content, by)
	}

	def debug(content, String by = name) {
		log(level('debug'), content, by)
	}

	def error(content, String by = name) {
		log(level('error'), content, by)
	}
}
