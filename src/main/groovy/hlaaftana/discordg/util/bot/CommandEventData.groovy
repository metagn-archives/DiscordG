package hlaaftana.discordg.util.bot

import groovy.transform.CompileStatic
import hlaaftana.discordg.objects.Channel
import hlaaftana.discordg.objects.Message

@CompileStatic
class CommandEventData {
	Command command
	CommandPattern alias
	CommandPattern trigger
	String arguments
	List<String> captures
	List<String> allCaptures
	@Delegate(excludes = ['getClass', 'toString']) Message message
	Map<String, Object> extra = [:]

	CommandEventData(Map<String, Object> props = [:], Command command,
	                 CommandPattern alias, CommandPattern trigger,
	                 String arguments, Message message) {
		for (e in props)
			this[e.key] = e.value
		this.command = command
		this.alias = alias
		this.trigger = trigger
		this.arguments = arguments
		this.message = message
	}

	def propertyMissing(String name) {
		if (extra.containsKey(name)) extra[name]
		else throw new MissingPropertyException(name, CommandEventData)
	}

	def propertyMissing(String name, value) {
		extra.put name, value
	}

	Message formatted(Channel chan = message.channel, String content) {
		(Message) chan.invokeMethod('sendMessage', command.parent.formatter.call(content))
	}

	Message sendMessage(Channel chan = message.channel, ...arguments) {
		(Message) chan.invokeMethod('sendMessage', arguments)
	}

	Message respond(Channel chan = message.channel, ...arguments) {
		(Message) chan.invokeMethod('sendMessage', arguments)
	}

	Message sendFile(Channel chan, ...arguments) {
		(Message) chan.invokeMethod('sendFile', arguments)
	}

	Message sendFile(...arguments) {
		(Message) channel.invokeMethod('sendFile', arguments)
	}

	CommandEventData clone() {
		new CommandEventData(extra, command, alias, trigger, arguments, message)
	}
}
