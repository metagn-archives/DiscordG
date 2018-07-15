package hlaaftana.discordg.util

import groovy.transform.CompileStatic

import java.time.LocalDateTime

@CompileStatic
class Log<T extends Message> {
	static class Level {
		String name
		boolean enabled = true

		Level enable() { enabled = true; this }
		Level disable() { enabled = false; this }
		boolean equals(Level other) { name == other.name }
	}

	interface Message {
		boolean isEnabled()
	}

	static class SimpleMessage implements Message {
		String by
		Level level
		String content
		LocalDateTime time = LocalDateTime.now()
		Map info = [:]

		boolean isEnabled() { level.enabled }
		String toString() { toString(defaultFormatter) }
		String toString(Log log) { toString(log.formatter) }
		String toString(Closure formatter) { formatter(this) }
	}

	static List<Level> defaultLevels = [
		new Level(name: 'info'),
		new Level(name: 'error'),
		new Level(name: 'warn'),
		new Level(name: 'debug').disable(),
		new Level(name: 'trace').disable()
	]

	static Closure<String> defaultFormatter = { SimpleMessage message ->
		String.format('<%s|%s> [%s] [%s]: %s',
			message.time.toLocalDate(),
			message.time.toLocalTime(),
			message.level.name.toUpperCase(),
			message.by, message.content)
	}

	Closure<String> formatter = defaultFormatter

	List<Level> levels = defaultLevels

	List<T> messages = []

	List<Closure<Void>> listeners = [{ T it -> if (it.enabled) println formatter.call(it) }]

	String name
	Log(String name) { this.name = name }

	Log(Log<T> parent) {
		formatter = parent.formatter
		name = parent.name
	}

	def listen(Closure ass) {
		listeners.add ass
	}

	def call(T message) {
		for (it in listeners) { it message }
	}

	Level level(String name) {
		Level ass = levels.find { it.name == name }
		if (null == ass) {
			ass = new Level(name: name)
			levels.add ass
		}
		ass
	}

	Level level(Level level) {
		if (level in levels) level
		else {
			levels.add level
			level
		}
	}

	Level propertyMissing(String name) {
		level(name)
	}

	def methodMissing(String name, args) {
		Level level = (Level) propertyMissing(name)
		if (args.class.array) args = ((Object[]) args).toList()
		boolean argsIsMultiple = args instanceof Collection
		if (args instanceof Message ||
			(argsIsMultiple && ((Collection) args).first() instanceof Message)){
			invokeMethod('log', args)
		} else {
			def ahh = argsIsMultiple ? [level] + ((Collection) args) : [level, args]
			invokeMethod('log', ahh)
		}
	}

	/// requires T to be SimpleMessage or just Message
	def log(Level level, content, String by = name) {
		SimpleMessage ass = new SimpleMessage(level: level, content: content.toString(), by: by)
		log((T) ass)
	}

	def log(T message) {
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
