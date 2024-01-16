package metagn.discordg.util.bot

import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import metagn.discordg.data.Channel
import metagn.discordg.data.Message

import java.util.regex.Matcher

@CompileStatic
@AutoClone
class CommandEventData {
	Command command
	CommandPattern alias
	CommandPattern trigger
	Matcher matcher
	List<String> allCaptures
	List<String> captures
	String arguments
	@Delegate(excludes = ['getClass', 'toString']) Message message
	Map<String, Object> extra = [:]

	CommandEventData() {}

	CommandEventData(Map<String, Object> props = [:], Command command,
					 CommandPattern alias, CommandPattern trigger,
					 Matcher matcher, Message message) {
		for (e in props)
			this[e.key] = e.value
		this.command = command
		this.alias = alias
		this.trigger = trigger
		this.message = message
		this.matcher = matcher
		this.allCaptures = getAllCaptures()
		this.arguments = getArguments()
	}

	Matcher getMatcher() {
		if (null == this.@matcher)
			this.@matcher = message.content =~ command.type.matchCommand(trigger, alias)
		this.@matcher
	}

	List<String> getAllCaptures() {
		if (null == this.@allCaptures) {
			def aa = matcher.collect()
			String[] rid = []
			if (aa instanceof String) rid = [aa]
			else if (null != aa[0])
				if (aa[0] instanceof String) rid = [(String) aa[0]]
				else rid = aa[0] as String[]
			this.@allCaptures = []
			for (int i = 1; i < rid.size(); ++i) {
				this.@allCaptures[i - 1] = rid[i] ?: ''
			}
		}
		this.@allCaptures
	}

	List<String> getCaptures() {
		if (null == this.@captures)
			this.@captures = command.type.captures(allCaptures)
		this.@captures
	}

	String getArguments() {
		if (null == this.@arguments)
			try {
				this.@arguments = message.content.substring(allCaptures[0].length()).trim()
			} catch (ignored) {
				this.@arguments = ''
			}
		this.@arguments
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

	static Message sendFile(Channel chan, ...arguments) {
		(Message) chan.invokeMethod('sendFile', arguments)
	}

	Message sendFile(...arguments) {
		(Message) channel.invokeMethod('sendFile', arguments)
	}
}
