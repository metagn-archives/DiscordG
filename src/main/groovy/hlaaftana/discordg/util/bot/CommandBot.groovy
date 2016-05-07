package hlaaftana.discordg.util.bot

import static hlaaftana.discordg.util.MiscUtil.dump
import hlaaftana.discordg.DiscordG
import hlaaftana.discordg.dsl.DelegatableEvent
import hlaaftana.discordg.oauth.BotClient
import hlaaftana.discordg.objects.Client
import hlaaftana.discordg.objects.Message
import hlaaftana.discordg.objects.Events
import hlaaftana.discordg.objects.DiscordObject
import hlaaftana.discordg.util.Log

import java.util.regex.Pattern

/**
 * A simple bot implementation.
 * @author Hlaaftana
 */
class CommandBot implements Triggerable {
	String logName = "DiscordG|CommandBot"
	BotType type = BotType.PREFIX
	Client client
	List commands = []
	boolean acceptOwnCommands = false
	boolean loggedIn = false

	/**
	 * @param client - The API object this bot should use.
	 * @param commands - A List of Commands you want to register right off the bat. Empty by default.
	 */
	CommandBot(Map config = [:]){
		config.each { k, v ->
			if (k.startsWith("trigger"))
				this.addTrigger(v)
			else
				this[k] = v
		}
		if (client == null) client = new BotClient()
	}

	static def create(Map config = [:], String botName, Closure tokenGetter){
		return new CommandBot(config).with { login(botName, tokenGetter); delegate }
	}

	static def create(Map config = [:], String botName, String token){
		return new CommandBot(config).with { login(botName, token); delegate }
	}

	static def create(Map config = [:], String token){
		return new CommandBot(config).with { login(token); delegate }
	}

	static def create(Map config = [:]){
		return new CommandBot(config)
	}

	/**
	 * Adds a command.
	 * @param command - the command.
	 */
	def addCommand(Command command){
		this.commands.add(command)
	}

	/**
	 * Adds a List of Commands.
	 * @param commands - the list of commands.
	 */
	def addCommands(List<Command> commands){
		this.commands.addAll(commands)
	}

	DSLCommand command(Map info, alias, trigger = [], Closure closure){
		DSLCommand hey = new DSLCommand(info, closure, this, alias, trigger)
		this.commands.add(hey)
		return hey
	}

	DSLCommand command(alias, trigger = [], Closure closure){
		DSLCommand hey = new DSLCommand([:], closure, this, alias, trigger)
		this.commands.add(hey)
		return hey
	}

	def login(String token){
		loggedIn = true
		client.login(token)
	}

	def login(String botName, String token){
		loggedIn = true
		client.login(botName){ token }
	}

	def login(String botName, Closure tokenGetter){
		loggedIn = true
		client.login(botName, tokenGetter)
	}

	/**
	 * Starts the bot. You don't have to enter any parameters if you ran #login already.
	 * @param email - the email to log in with.
	 * @param password - the password to log in with.
	 */
	def initialize(){
		client.addListener(Events.MESSAGE) { Map d ->
			for (Command c in commands){
				if (c.match(d.message)){
					try{
						if (acceptOwnCommands || !(d.author == client.user)){
							c.run(d)
						}
					}catch (ex){
						ex.printStackTrace()
						Log.error "Command threw exception", this.logName
					}
				}
			}
		}
	}

	def initialize(String token){
		initialize()
		login(token)
	}

	def initialize(String botName, String token){
		initialize()
		login(botName){ token }
	}

	def initialize(String botName, Closure tokenGetter){
		initialize()
		login(botName, tokenGetter)
	}
}

enum BotType {
	// These have IDs for convenience sake
	PREFIX(0, { Trigger trigger, Alias alias ->
		Pattern.quote(trigger.toString()) + Pattern.quote(alias.toString()) + /\s?((?:.|\n)*)/
	}),
	SUFFIX(1, { Trigger trigger, Alias alias ->
		Pattern.quote(alias.toString()) + Pattern.quote(trigger.toString()) + /\s?((?:.|\n)*)/
	}),
	REGEX(2, { Trigger trigger, Alias alias ->
		trigger.toString() + alias.toString() + /\s?((?:.|\n)*)/
	})

	Closure commandMatcher
	int id
	BotType(int id, Closure commandMatcher){ this.id = id; this.commandMatcher = commandMatcher }
}

trait Restricted {
	boolean black = false
	boolean white = false
	Set blacklist = []
	Set whitelist = []

	def whitelist(){ white = true; this }
	def blacklist(){ black = true; this }
	def greylist(){ black = white = true; this }

	def whitelist(thing){ whitelist(); allow(thing); this }
	def blacklist(thing){ blacklist(); disallow(thing); this }

	def allow(...thing){ allow(thing as Set) }

	def allow(thing){
		if (thing instanceof Collection || thing.class.array)
			thing.each { allow(it) }
		if (white)
			if (thing instanceof Closure)
				whitelist += thing
			else whitelist += DiscordObject.resolveId(thing)
		if (black)
			if (thing instanceof Closure)
				blacklist -= thing
			else blacklist -= DiscordObject.resolveId(thing)
		this
	}

	def disallow(...thing){ disallow(thing as Set) }

	def disallow(thing){
		if (thing instanceof Collection || thing.class.array)
			thing.each { disallow(it) }
		if (white)
			if (thing instanceof Closure)
				whitelist -= thing
			else whitelist -= DiscordObject.resolveId(thing)
		if (black)
			if (thing instanceof Closure)
				blacklist += thing
			else blacklist += DiscordObject.resolveId(thing)
		this
	}

	def deny(...thing){ disallow(thing as Set) }

	def deny(thing){
		disallow(thing)
	}

	boolean allows(Message msg){
		return (black ? !associatedIds(msg).any { inList(blacklist, it) } : true) && (white ? associatedIds(msg).any { inList(whitelist, it) } : true)
	}

	static List associatedIds(Message msg){
		// i have no idea why i'm including the message id
		return [msg?.id, msg?.channel?.id, msg?.author?.id, msg?.server?.id] - null
	}

	static boolean inList(list, thing){
		return DiscordObject.resolveId(thing) in list.collect { it instanceof Closure ? DiscordObject.resolveId(it(thing)) : it }
	}
}

trait Triggerable {
	List triggers = []

	def addTrigger(trigger){
		triggers = dump(triggers, trigger instanceof Triggerable ? trigger.triggers : trigger){ new Trigger(it) }
	}

	def addTriggers(trigger){ addTrigger(trigger) }

	def convertTriggers(List old){
		return old.collect { new Trigger(it) }
	}
}

trait Aliasable {
	List aliases = []

	def addAlias(alias){
		aliases = dump(aliases, alias instanceof Aliasable ? alias.aliases : alias){ new Alias(it) }
	}

	def addAliases(alias){ addAlias(alias) }

	def convertAliases(List old){
		return old.collect { new Alias(it) }
	}
}

class Alias implements Restricted {
	def closure

	Alias(Closure closure){
		this.closure = closure
	}

	Alias(notClosure){
		this.closure = { "$notClosure" }
	}

	Alias(Alias otherTrigger){
		this.closure = otherTrigger.closure
	}

	def plus(smh){
		return "${closure()}$smh"
	}

	def plus(Trigger trigger){
		return new Trigger({ "$this$trigger" })
	}

	boolean equals(other){
		return this.is(other) || this.closure.is(other.closure) || this.toString() == other.toString()
	}

	String toString(){
		return "${closure()}"
	}
}

class Trigger implements Restricted {
	def closure

	Trigger(Closure closure){
		this.closure = closure
	}

	Trigger(notClosure){
		this.closure = { "$notClosure" }
	}

	Trigger(Trigger otherTrigger){
		this.closure = otherTrigger.closure
	}

	def plus(smh){
		return "${closure()}$smh"
	}

	def plus(Trigger trigger){
		return new Trigger({ "$this$trigger" })
	}

	boolean equals(other){
		return this.is(other) || this.closure.is(other.closure) || this.toString() == other.toString()
	}

	String toString(){
		return "${closure()}"
	}
}


/**
 * A command.
 * @author Hlaaftana
 */
abstract class Command implements Triggerable, Aliasable, Restricted {
	static BotType defaultBotType = BotType.PREFIX
	BotType type = defaultBotType

	Command(Triggerable parentT, alias, trigger = []){
		this.addAlias(alias)
		this.addTrigger(trigger)
		this.addTrigger(parentT)
		if (parentT instanceof CommandBot) this.type = parentT.type
	}

	/// null if no match
	List match(Message msg){
		if (!this.allows(msg)) return null
		return [triggers, aliases].combinations().find { Trigger trigger, Alias alias ->
			trigger.allows(msg) && alias.allows(msg) && msg.content ==~ (trigger.toString() + alias.toString() + /(?:.|\n)*/)
		}
	}

	def matcher(Message msg){
		List pair = this.match(msg)
		return msg.content =~ type.commandMatcher(pair)
	}

	/**
	 * Gets the text after the command trigger for this command.
	 * @param d - the event data.
	 * @return the arguments as a string.
	 */
	def args(Map d){
		return this.allCaptures(d).size() > 1 ? this.allCaptures(d).last() : ""
	}

	// only for regex
	List captures(Map d){
		return this.allCaptures(d).drop(1)
	}

	// only for regex
	List allCaptures(Map d){
		def aa = this.matcher(d.message).collect()
		return aa instanceof String ? [aa] : aa[0]
	}

	/**
	 * Runs the command.
	 * @param d - the event data.
	 */
	abstract def run(Map d)
}

/**
 * An implementation of Command with a string response.
 * @author Hlaaftana
 */
class ResponseCommand extends Command{
	Closure response

	/**
	 * @param response - a string to respond with to this command. <br>
	 * The rest of the parameters are Command's parameters.
	 */
	ResponseCommand(response, Triggerable parentT, alias, trigger = []){
		super(parentT, alias, trigger)
		this.response = (response instanceof Closure) ? response : { Map d -> "$response" }
		this.response.delegate = this
	}

	def run(Map d){
		d.sendMessage(response(d))
	}
}

/**
 * An implementation of Command with a closure response.
 * @author Hlaaftana
 */
class ClosureCommand extends Command {
	Closure response

	/**
	 * @param response - a closure to respond with to this command. Can take one parameter, which is the data of the event.
	 */
	ClosureCommand(Closure response, Triggerable parentT, alias, trigger = []){
		super(parentT, alias, trigger)
		response.delegate = this
		this.response = response
	}

	def run(Map d){
		response(d)
	}
}

class DSLCommand extends Command {
	Closure response
	Map info = [:]

	DSLCommand(Map info = [:], Closure response, Triggerable parentT, alias, trigger = []){
		super(parentT, alias, trigger)
		this.response = response
		this.info << info
	}

	def run(Map d){
		DelegatableEvent aa = new DelegatableEvent("MESSAGE_CREATE", d)
		aa.data["args"] = args(d)
		aa.data["captures"] = captures(d)
		aa.data["allCaptures"] = allCaptures(d)
		aa.data["match"] = match(d.message)
		aa.data["matcher"] = matcher(d.message)
		aa.data << this.properties
		Closure copy = response
		copy.delegate = aa
		copy.resolveStrategy = Closure.DELEGATE_FIRST
		copy(d)
	}
}