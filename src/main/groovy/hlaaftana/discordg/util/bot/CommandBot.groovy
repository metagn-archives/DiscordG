package hlaaftana.discordg.util.bot

import groovy.transform.*
import hlaaftana.discordg.Client
import hlaaftana.discordg.DiscordObject
import hlaaftana.discordg.logic.BasicListenerSystem
import hlaaftana.discordg.logic.ListenerSystem
import hlaaftana.discordg.objects.Message
import hlaaftana.discordg.util.Log

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * A simple bot implementation.
 * @author Hlaaftana
 */
@CompileStatic
class CommandBot implements Triggerable {
	String logName = 'CommandBot'
	Log log
	CommandType defaultCommandType = CommandType.PREFIX
	Client client
	List<Command> commands = []
	Map<String, Closure> extraCommandArgs = [:]
	boolean acceptOwnCommands = false
	boolean loggedIn = false
	Closure commandListener
	Closure commandRunnerListener
	Closure exceptionListener
	Closure<String> formatter
	ListenerSystem<BotEventData> listenerSystem = new BasicListenerSystem<>()
	String token

	CommandBot(Map<String, Object> config){
		for (e in config)
			if (e.key == 'trigger') addTrigger(e.value)
			else if (e.key == 'triggers') addTriggers(e.value as Collection)
			else setProperty(e.key, e.value)
		if (!log) log = new Log(logName)
		if (!client) client = new Client(bot: true)
	}

	def addCommand(Command command){
		commands.add(command)
	}

	def addCommands(List<Command> commands){
		this.commands.addAll(commands)
	}

	void setTriggers(Collection s) { for (a in s) addTrigger(a) }

	DSLCommand command(Map info, alias, trigger = [],
	                   @DelegatesTo(value = CommandEventData, strategy = Closure.DELEGATE_FIRST) Closure closure){
		DSLCommand hey = new DSLCommand(info, closure, this, alias, trigger)
		commands.add(hey)
		hey
	}

	DSLCommand command(alias, trigger = [],
	                   @DelegatesTo(value = CommandEventData, strategy = Closure.DELEGATE_FIRST) Closure closure){
		command([:], alias, trigger, closure)
	}

	Command command(Class<? extends Command> commandClass, ...arguments){
		Object[] arr = new Object[arguments.length + 1]
		arr[0] = this
		System.arraycopy(arguments, 0, arr, 1, arguments.length)
		commandClass.newInstance(arr)
	}

	Command command(Class<? extends Command> commandClass, List arguments){
		Object[] arr = new Object[arguments.size() + 1]
		arr[0] = this
		System.arraycopy(arguments, 0, arr, 1, arguments.size())
		commandClass.newInstance(arr)
	}

	Closure commandBuilder(Class<? extends Command> commandClass){
		this.&command.curry(commandClass)
	}

	void login(String token, boolean threaded = true){
		loggedIn = true
		client.login(token, true, threaded)
	}

	/**
	 * Starts the bot. You don't have to enter any parameters if you ran #login already.
	 * @param email - the email to log in with.
	 * @param password - the password to log in with.
	 */
	void initialize() {
		exceptionListener = listenerSystem.addListener(Events.EXCEPTION) { BotExceptionEventData d ->
			d.exception.printStackTrace()
			log.error 'Command threw exception'
		}
		commandRunnerListener = listenerSystem.addListener(Events.COMMAND) { BotCommandEventData d ->
			try {
				d.command.call(d.commandData)
				d.command.uses += 1
			} catch (ex) {
				def c = new BotExceptionEventData(this, d, ex)
				listenerSystem.dispatchEvent(Events.EXCEPTION, c)
			}
		}
		commandListener = client.addListener('message') { Map data ->
			if (!acceptOwnCommands && ((Map) data.author).id == client.id) return
			boolean anyPassed = false
			Message msg = (Message) data.message
			for (c in new ArrayList<Command>(commands)){
				Tuple2<CommandPattern, CommandPattern> match
				if ((match = c.match(msg))){
					anyPassed = true

					CommandEventData ced = new CommandEventData(c, match.second, match.first,
							c.arguments(msg), msg)
					ced.captures = c.captures(msg)
					ced.allCaptures = c.allCaptures(msg)

					listenerSystem.dispatchEvent(Events.COMMAND, new BotCommandEventData(this, ced))
				}
			}

			if (!anyPassed) listenerSystem.dispatchEvent(Events.NO_COMMAND, new BotMapEventData(this, data))
		}
		listenerSystem.dispatchEvent(Events.INITIALIZE, null)
		if (null != token && !loggedIn) login(token)
	}

	def initialize(String token){
		initialize()
		login(token)
	}

	def uninitialize(){
		listenerSystem.removeListener(Events.COMMAND, commandRunnerListener)
		client.removeListener('message', commandListener)
	}

	static enum Events {
		INITIALIZE,
		COMMAND,
		NO_COMMAND,
		EXCEPTION
	}
}

@CompileStatic
class BotEventData {
	CommandBot bot

	BotEventData(CommandBot bot) {
		this.bot = bot
	}
}

@CompileStatic
class BotMapEventData extends BotEventData {
	Map data

	BotMapEventData(CommandBot bot, Map data) {
		super(bot)
		this.data = data
	}
}

class BotCommandEventData extends BotEventData {
	@Delegate CommandEventData commandData

	BotCommandEventData(CommandBot bot, CommandEventData commandData) {
		super(bot)
		this.commandData = commandData
	}
}

class BotExceptionEventData extends BotEventData {
	Exception exception
	@Delegate BotCommandEventData commandEventData

	BotExceptionEventData(CommandBot bot, BotCommandEventData commandEventData, Exception exception) {
		super(bot)
		this.commandEventData = commandEventData
		this.exception = exception
	}
}

@CompileStatic
class CommandType {
	private static quote(CommandPattern aot){
		String g = aot.toString()
		aot.regex ? g : Pattern.quote(g)
	}
	// These have IDs for convenience sake
	static final CommandType PREFIX = de { CommandPattern trigger, CommandPattern alias ->
		/(?i)(/ + quote(trigger) + quote(alias) + /)(?:\s+(?:.|\n)*)?/
	}
	static final CommandType SUFFIX = de { CommandPattern trigger, CommandPattern alias ->
		/(?i)(/ + quote(alias) + quote(trigger) + /)(?:\s+(?:.|\n)*)?/
	}
	static final CommandType REGEX = de { CommandPattern trigger, CommandPattern alias ->
		/(/ + trigger.toString() + alias.toString() + /)(?:\s+(?:.|\n)*)?/
	}
	static final CommandType REGEX_SUFFIX = de { CommandPattern trigger, CommandPattern alias ->
		/(/ + alias.toString() + trigger.toString() + /)(?:\s+(?:.|\n)*)?/
	}

	Closure<List> customCaptures = { List it -> it.drop(1) }
	Closure<String> commandMatcher
	CommandType(Closure<String> commandMatcher){ this.commandMatcher = commandMatcher }

	static CommandType 'de'(Closure<String> commandMatcher){
		new CommandType(commandMatcher)
	}
}

trait Restricted {
	boolean black = false
	boolean white = false
	Map<String, Set> blacklist = [guild: [], channel: [], author: [], role: []]
	Map<String, Set> whitelist = [guild: [], channel: [], author: [], role: []]

	def whitelist(){ white = true; this }
	def blacklist(){ black = true; this }
	def greylist(){ black = white = true; this }

	def whitelist(String type, thing){ whitelist(); allow(type, thing); this }
	def blacklist(String type, thing){ blacklist(); disallow(type, thing); this }

	def allow(String type, ...thing){ allow(type, thing as Set) }

	def allow(String type, thing){
		if (thing instanceof Collection || thing.class.array)
			thing.each { allow(type, it) }
		if (white)
			whitelist[type].add(DiscordObject.id(thing))
		if (black)
			blacklist[type].remove(DiscordObject.id(thing))
		this
	}

	def disallow(String type, ...thing){ disallow(type, thing as Set) }

	def disallow(String type, thing){
		if (thing instanceof Collection || thing.class.array)
			thing.each { disallow(type, it) }
		if (white)
			whitelist[type].remove(DiscordObject.id(thing))
		if (black)
			blacklist[type].add(DiscordObject.id(thing))
		this
	}

	def deny(String type, ...thing){ disallow(type, thing as Set) }

	def deny(String type, thing){
		disallow(type, thing)
	}

	boolean allows(Message msg){
		boolean wh = true
		boolean bl = false
		if (white) {
			for (e in whitelist)
				wh |= !Collections.disjoint(e.value, e.key == 'role' ?
						msg.member.roles : [msg."$e.key".id])
		}
		if (black) {
			for (e in whitelist)
				bl |= !Collections.disjoint(e.value, e.key == 'role' ?
						msg.member.roles : [msg."$e.key".id])
		}
		wh && !bl
	}
}

trait Triggerable {
	Set<CommandPattern> triggers = []
	
	def addTrigger(trigger) {
		triggers.add(new CommandPattern(trigger))
	}

	def addTrigger(CommandPattern trigger) {
		triggers.add(trigger)
	}

	def addTrigger(Triggerable triggers){
		addTriggers(triggers.triggers)
	}

	def addTriggers(Collection trigger) {
		for (t in trigger)
			addTrigger(t)
	}

	CommandPattern getTrigger(){ triggers[0] }
}

trait Aliasable {
	Set<CommandPattern> aliases = []

	def addAlias(alias) {
		aliases.add(new CommandPattern(alias))
	}
	
	def addAlias(CommandPattern alias) {
		aliases.add(alias)
	}

	def addAlias(Aliasable aliases){
		addAliases(aliases.aliases)
	}

	def addAliases(Collection alias) {
		for (t in alias)
			addAlias(t)
	}

	CommandPattern getAlias(){ aliases[0] }
}

@CompileStatic
class CommandPattern<T> implements Restricted, CharSequence {
	boolean regex = false
	T inner

	CommandPattern(T inner) {
		if (inner instanceof Pattern) regex = true
		this.inner = inner
	}

	CharSequence getCharSequence() {
		if (inner instanceof CharSequence) (CharSequence) inner
		else if (inner instanceof Pattern) ((Pattern) inner).pattern()
		else if (inner instanceof Closure) ((Closure) inner).call(this)
		else throw new IllegalArgumentException('Unknown char sequence for command pattern class ' + inner.class)
	}

	@Override int length() { charSequence.length() }
	@Override char charAt(int index) { charSequence.charAt(index) }
	@Override CharSequence subSequence(int start, int end) { charSequence.subSequence(start, end) }
	@Override String toString() { charSequence.toString() }
}

@CompileStatic
class Command implements Triggerable, Aliasable, Restricted {
	CommandBot parent
	CommandType type
	int uses = 0

	Command(CommandBot parent, alias, trigger = []) {
		this(alias, trigger)
		addTrigger(parent)
		this.parent = parent
		type = parent.defaultCommandType
	}
	
	Command(alias, trigger){
		if (alias instanceof Collection || alias.class.array) addAliases(alias)
		else addAlias(alias)
		if (trigger instanceof Collection || trigger.class.array) addTriggers(trigger)
		else addTrigger(trigger)
	}

	/// null if no match
	Tuple2<CommandPattern, CommandPattern> match(Message msg){
		if (!allows(msg)) return null
		for (List<CommandPattern> x in (List<List<CommandPattern>>) [triggers, aliases].combinations())
			if (x[0].allows(msg) && x[1].allows(msg) && msg.content ==~ type.commandMatcher.call(x[0], x[1]))
				return new Tuple2(x[0], x[1])
		null
	}

	CommandPattern hasAlias(ahh){ for (x in aliases) if (ahh.toString() == x.toString()) return x; null }
	CommandPattern hasTrigger(ahh){ for (x in triggers) if (ahh.toString() == x.toString()) return x; null }

	Matcher matcher(Message msg){
		Tuple2 pair = match(msg)
		if (!pair) return null
		msg.content =~ type.commandMatcher.call(pair)
	}

	/**
	 * Gets the text after the command trigger for this command.
	 * @param d - the event data.
	 * @return the arguments as a string.
	 */
	String arguments(Message msg){
		try{
			msg.content.substring(allCaptures(msg)[0].length()).trim()
		}catch (ignored){
			''
		}
	}

	// only for regex
	List captures(Message msg){
		type.customCaptures.call(allCaptures(msg))
	}

	// only for regex
	List<String> allCaptures(Message msg){
		def aa = matcher(msg).collect()
		String[] rid = []
		if (aa instanceof String) rid = [aa]
		else if (null != aa[0])
			if (aa[0] instanceof String) rid = [aa[0]]
			else rid = aa[0] as String[]
		List<String> res = []
		for (int i = 1; i < rid.size(); ++i) {
			res[i - 1] = rid[i] ?: ''
		}
		res
	}

	/**
	 * Runs the command.
	 * @param d - the event data.
	 */
	def run(CommandEventData d){}
	def run(Message msg){}

	def call(CommandEventData d){ run(d); run(d.message) }
	def call(Message msg){ run(msg) }
}

/**
 * An implementation of Command with a string response.
 * @author Hlaaftana
 */
@CompileStatic
class ResponseCommand extends Command {
	Closure response

	/**
	 * @param response - a string to respond with to this command. <br>
	 * The rest of the parameters are Command's parameters.
	 */
	ResponseCommand(CommandBot parentT, alias, trigger = [], response){
		super(parentT, alias, trigger)
		this.response = response instanceof Closure ? (Closure) response : { CommandEventData d -> "$response" }
		this.response.delegate = this
	}

	def run(CommandEventData d){
		d.invokeMethod('sendMessage', response(d))
	}
}

/**
 * An implementation of Command with a closure response.
 * @author Hlaaftana
 */
@CompileStatic
class ClosureCommand extends Command {
	Closure response

	/**
	 * @param response - a closure to respond with to this command. Can take one parameter, which is the data of the event.
	 */
	ClosureCommand(CommandBot parentT, alias, trigger = [], Closure response){
		super(parentT, alias, trigger)
		response.delegate = this
		this.response = response
	}

	def run(CommandEventData d){
		response(d)
	}
}

@CompileStatic
class DSLCommand extends Command {
	Closure response
	Map<String, Closure> extraArgs = [:]
	Map<String, Object> info = [:]

	DSLCommand(Map info = [:],
	           @DelegatesTo(value = CommandEventData, strategy = Closure.DELEGATE_FIRST) Closure response,
	           CommandBot parentT, alias, trigger = []){
		super(parentT, alias, trigger)
		this.response = (Closure) response.clone()
		info.each(this.&putAt)
		if (parentT instanceof CommandBot && parentT.extraCommandArgs)
			extraArgs << parentT.extraCommandArgs
	}

	def propertyMissing(String name){
		if (info.containsKey(name)) info[name]
		else throw new MissingPropertyException(name, this.class)
	}

	def propertyMissing(String name, value){
		info.put name, value
	}

	def run(CommandEventData d){
		CommandEventData aa = (CommandEventData) d.clone()
		for (a in extraArgs)
			aa.setProperty(a.key, a.value.call(d))
		Closure copy = (Closure) response.clone()
		copy.delegate = aa
		copy.resolveStrategy = Closure.DELEGATE_FIRST
		copy(aa)
	}
}