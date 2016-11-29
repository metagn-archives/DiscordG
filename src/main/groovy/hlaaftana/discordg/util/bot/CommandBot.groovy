package hlaaftana.discordg.util.bot

import static hlaaftana.discordg.util.MiscUtil.dump

import groovy.transform.*

import hlaaftana.discordg.Client
import hlaaftana.discordg.DiscordG
import hlaaftana.discordg.Events
import hlaaftana.discordg.logic.EventData;
import hlaaftana.discordg.objects.Member
import hlaaftana.discordg.objects.Message
import hlaaftana.discordg.objects.DiscordObject
import hlaaftana.discordg.util.ClosureString
import hlaaftana.discordg.util.Log

import java.util.regex.Pattern

/**
 * A simple bot implementation.
 * @author Hlaaftana
 */
class CommandBot implements Triggerable {
	String logName = "DiscordG|CommandBot"
	Log log
	BotType type = BotType.PREFIX
	volatile Client client
	List commands = []
	Map<String, Closure> extraCommandArgs = [:]
	boolean acceptOwnCommands = false
	boolean loggedIn = false
	Closure commandListener

	CommandBot(Map config = [:]){
		config.each { k, v ->
			if (k.startsWith("trigger"))
				this.addTrigger(v)
			else
				this[k] = v
		}
		if (!log) log = new Log(logName)
		if (!client) client = new Client()
	}

	def addCommand(Command command){
		commands.add(command)
	}

	def addCommands(List<Command> commands){
		commands.addAll(commands)
	}

	DSLCommand command(Map info, alias, trigger = [], Closure closure){
		DSLCommand hey = DSLCommand.new(info, this, alias, trigger, closure)
		commands.add(hey)
		hey
	}

	DSLCommand command(alias, trigger = [], Closure closure){
		command([:], alias, trigger, closure)
	}

	Command command(Class<? extends Command> commandClass, ...args){
		commandClass.&newInstance([this] + (args as List))
	}

	Command command(Class<? extends Command> commandClass, List args){
		commandClass.&newInstance([this] + args)
	}

	Closure commandBuilder(Class<? extends Command> commandClass){
		this.&command.curry(commandClass)
	}

	def login(String token, boolean bot = true){
		loggedIn = true
		client.login(token, bot)
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
		commandListener = client.listener(Events.MESSAGE){
			try{
				if (!acceptOwnCommands && json.author.id == this.client.id) return
			}catch (ex){}
			for (Command c in commands){
				if (c.match(message)){
					try{
						c(it.clone())
					}catch (ex){
						ex.printStackTrace()
						log.error "Command threw exception"
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

	def uninitialize(){
		client.listenerSystem.removeListener("MESSAGE_CREATE", commandListener)
	}
}

class BotType {
	private static quote(aot){
		def g = aot.toString().toLowerCase()
		aot.regex ? g : Pattern.quote(g)
	}
	// These have IDs for convenience sake
	static final BotType PREFIX = BotType.new(0, false){ Trigger trigger, Alias alias ->
		/(?i)(/ + quote(trigger) + quote(alias) + /)(?:\s+(?:.|\n)*)?/
	}
	static final BotType SUFFIX = BotType.new(1, false){ Trigger trigger, Alias alias ->
		/(?i)(/ + quote(alias) + quote(trigger) + /)(?:\s+(?:.|\n)*)?/
	}
	static final BotType REGEX = BotType.new(2){ Trigger trigger, Alias alias ->
		/(/ + trigger.toString() + alias.toString() + /)(?:\s+(?:.|\n)*)?/
	}
	static final BotType REGEX_SUFFIX = BotType.new(3){ Trigger trigger, Alias alias ->
		/(/ + alias.toString() + trigger.toString() + /)(?:\s+(?:.|\n)*)?/
	}

	Closure commandMatcher
	int id
	boolean caseSensitive
	BotType(int id, Closure commandMatcher, boolean caseSensitive = true){ this.id = id; this.commandMatcher = commandMatcher; this.caseSensitive = caseSensitive }

	static BotType "new"(int id, boolean caseSensitive = true, Closure commandMatcher){
		new BotType(id, commandMatcher, caseSensitive)
	}
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
		if (white){
			if (thing instanceof Closure) whitelist += thing
			else whitelist += DiscordObject.resolveId(thing)
		}
		if (black){
			if (thing instanceof Closure) blacklist -= thing
			else blacklist -= DiscordObject.resolveId(thing)
		}
		this
	}

	def disallow(...thing){ disallow(thing as Set) }

	def disallow(thing){
		if (thing instanceof Collection || thing.class.array)
			thing.each { disallow(it) }
		if (white){
			if (thing instanceof Closure) whitelist -= thing
			else whitelist -= DiscordObject.resolveId(thing)
		}
		if (black){
			if (thing instanceof Closure) blacklist += thing
			else blacklist += DiscordObject.resolveId(thing)
		}
		this
	}

	def deny(...thing){ disallow(thing as Set) }

	def deny(thing){
		disallow(thing)
	}

	boolean allows(Message msg){
		(black ? !associatedIds(msg).any { inList(blacklist, it) } : true) && (white ? associatedIds(msg).any { inList(whitelist, it) } : true)
	}

	static List associatedIds(Message msg){
		// i have no idea why i'm including the message id
		List a = [msg?.id, msg?.channel?.id, msg?.author?.id, msg?.server?.id]
		def b = msg?.author(true)
		if (b instanceof Member) a += b?.roles?.id
		(a - null).unique()
	}

	static boolean inList(list, thing){
		DiscordObject.resolveId(thing) in list.collect { it instanceof Closure ? DiscordObject.resolveId(it(thing)) : it }
	}
}

trait Triggerable {
	List triggers = []

	def addTrigger(trigger){
		triggers = dump(triggers, trigger instanceof Triggerable ? trigger.triggers : trigger){ new Trigger(it) }
	}

	Trigger getTrigger(){
		triggers[0]
	}

	def addTriggers(trigger){ addTrigger(trigger) }

	def convertTriggers(List old){
		old.collect { new Trigger(it) }
	}
}

trait Aliasable {
	List aliases = []

	def addAlias(alias){
		aliases = dump(aliases, alias instanceof Aliasable ? alias.aliases : alias){ new Alias(it) }
	}

	def addAliases(alias){ addAlias(alias) }

	Alias getAlias(){
		aliases[0]
	}

	def convertAliases(List old){
		old.collect { new Alias(it) }
	}
}

@InheritConstructors
class Alias extends ClosureString implements Restricted {}
@InheritConstructors
class Trigger extends ClosureString implements Restricted {}

class Command implements Triggerable, Aliasable, Restricted {
	static BotType defaultBotType = BotType.PREFIX
	BotType type = defaultBotType

	Command(Triggerable parentT, alias, trigger = []){
		addAlias alias
		addTrigger trigger
		addTrigger parentT
		if (parentT instanceof CommandBot) type = parentT.type
	}

	static Command "new"(Triggerable parentT, alias, trigger = []){
		new Command(parentT, alias, trigger)
	}

	/// null if no match
	List match(Message msg){
		if (!allows(msg)) return null
		[triggers, aliases].combinations().find { Trigger trigger, Alias alias ->
			trigger.allows(msg) && alias.allows(msg) && msg.content ==~ type.commandMatcher(trigger, alias)
		}
	}

	Alias usedAlias(Message msg){
		(match(msg) ?: [])[1]
	}

	Trigger usedTrigger(Message msg){
		(match(msg) ?: [])[0]
	}

	boolean hasAlias(ahh){ aliases.any { it.toString() == ahh.toString() } }
	boolean hasTrigger(ahh){ triggers.any { it.toString() == ahh.toString() } }

	def matcher(Message msg){
		List pair = this.match(msg)
		if (!pair) return null
		msg.content =~ type.commandMatcher(pair)
	}

	/**
	 * Gets the text after the command trigger for this command.
	 * @param d - the event data.
	 * @return the arguments as a string.
	 */
	def args(Message msg){
		try{
			msg.content.substring(allCaptures(msg)[0].size()).trim()
		}catch (ex){
			""
		}
	}

	// only for regex
	List captures(Message msg){
		this.allCaptures(msg).drop(1)
	}

	// only for regex
	List allCaptures(Message msg){
		def aa = this.matcher(msg).collect()
		(aa instanceof String ? [aa] : aa[0] != null ?
			aa[0] instanceof String ? [aa[0]] : aa[0] : []).collect { it ?: "" }.drop(1)
	}

	/**
	 * Runs the command.
	 * @param d - the event data.
	 */
	def run(Map d){}
	def run(Message msg){}

	def call(Map d){ run(d); run(d.message) }
	def call(Message msg){ run(msg) }

	int hashCode(){
		aliases.hashCode() ^ triggers.hashCode()
	}
}

/**
 * An implementation of Command with a string response.
 * @author Hlaaftana
 */
class ResponseCommand extends Command {
	Closure response

	/**
	 * @param response - a string to respond with to this command. <br>
	 * The rest of the parameters are Command's parameters.
	 */
	ResponseCommand(Triggerable parentT, alias, trigger = [], response){
		super(parentT, alias, trigger)
		this.response = (response instanceof Closure) ? response : { Map d -> "$response" }
		this.response.delegate = this
	}

	static ResponseCommand "new"(Triggerable parentT, alias, trigger = [], response){
		new ResponseCommand(parentT, alias, trigger, response)
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
	ClosureCommand(Triggerable parentT, alias, trigger = [], Closure response){
		super(parentT, alias, trigger)
		response.delegate = this
		this.response = response
	}

	static ClosureCommand "new"(Triggerable parentT, alias, trigger = [], Closure response){
		new ResponseCommand(parentT, alias, trigger, response)
	}

	def run(Map d){
		response(d)
	}
}

class DSLCommand extends Command {
	Closure response
	Map<String, Closure> extraArgs = [:]
	Map info = [:]

	DSLCommand(Map info = [:], Closure response, Triggerable parentT, alias, trigger = []){
		super(parentT, alias, trigger)
		this.response = response
		info.each(this.&putAt)
		if (parentT instanceof CommandBot && parentT.extraCommandArgs)
			extraArgs << parentT.extraCommandArgs
	}

	static DSLCommand "new"(Map info = [:], Triggerable parentT, alias, trigger = [], Closure response){
		new DSLCommand(info, response, parentT, alias, trigger)
	}

	def propertyMissing(String name){
		if (info.containsKey(name)) info[name]
		else throw new MissingPropertyException(name, this.class)
	}

	def propertyMissing(String name, value){
		info[name] = value
	}

	def run(Map d){
		EventData aa = d
		aa["args"] = args(d.message)
		aa["captures"] = captures(d.message)
		aa["allCaptures"] = allCaptures(d.message)
		aa["match"] = match(d.message)
		aa["matcher"] = matcher(d.message)
		aa["usedAlias"] = usedAlias(d.message)
		aa["usedTrigger"] = usedTrigger(d.message)
		aa["command"] = this
		extraArgs.each { k, v ->
			aa[k] = v
		}
		aa << this.properties
		Closure copy = response.clone()
		copy.delegate = aa
		copy.resolveStrategy = Closure.DELEGATE_FIRST
		copy(aa)
	}
}