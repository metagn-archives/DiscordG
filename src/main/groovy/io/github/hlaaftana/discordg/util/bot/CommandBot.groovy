package io.github.hlaaftana.discordg.util.bot

import io.github.hlaaftana.discordg.DiscordG

import io.github.hlaaftana.discordg.objects.Client
import io.github.hlaaftana.discordg.util.Log

/**
 * A simple bot implementation.
 * @author Hlaaftana
 */
class CommandBot {
	static class Configuration {
		String logName = "DiscordG|CommandBot"
		def trigger = []
		BotType type = BotType.PREFIX
		Client client = new Client()
		List commands = []
		boolean acceptOwnCommands = false

		Configuration(Map map=[:]){
			this.properties.findAll { k, v -> !(k in ["class"]) && (k in map) }.each { k, v ->
				this[k] = map[k]
			}
		}

		Configuration leftShift(Configuration other){
			this.properties.findAll { k, v -> !(k in ["class"]) && (k in map) }.each { k, v ->
				this[k] = map[k]
			}
			return this
		}
	}
	static enum BotType {
		// These have IDs for convenience sake
		PREFIX(0),
		SUFFIX(1),
		REGEX(2)

		int id
		BotType(int id){ this.id = id }
	}
	String logName
	def trigger
	BotType type
	Client client
	List commands
	boolean acceptOwnCommands
	private boolean loggedIn = false

	/**
	 * @param client - The API object this bot should use.
	 * @param commands - A List of Commands you want to register right off the bat. Empty by default.
	 */
	CommandBot(Configuration config = new Configuration()){
		this.logName = config.logName
		this.trigger = config.trigger
		this.type = config.type
		this.client = config.client
		this.commands = config.commands
		this.acceptOwnCommands = config.acceptOwnCommands
	}

	static def create(String email, String password, def config = new Configuration()){
		return new CommandBot((config instanceof Configuration ? config : new Configuration(config)) << new Configuration(client: DiscordG.withLogin(email, password)))
	}

	static def create(def config = new Configuration()){
		return new CommandBot(config instanceof Configuration ? config : new Configuration(config))
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

	/**
	 * Logs in with the API before initalizing. You don't have to type your email and password when calling #initalize.
	 * @param email - the email to log in with.
	 * @param password - the password to log in with.
	 */
	def login(String email, String password){
		loggedIn = true
		client.login(email, password)
	}

	/**
	 * Starts the bot. You don't have to enter any parameters if you ran #login already.
	 * @param email - the email to log in with.
	 * @param password - the password to log in with.
	 */
	def initialize(String email="", String password=""){
		if (this.type.id == BotType.PREFIX.id){
			client.addListener("message create") { Map d ->
				for (c in commands){
					for (p in c.triggers){
						for (a in c.aliases){
							if ((d.message.content + " ").toLowerCase().startsWith(p.toLowerCase() + a.toLowerCase() + " ")){
								try{
									if (acceptOwnCommands){
										c.run(d)
									}else if (!(d.message.author.id == client.user.id)){
										c.run(d)
									}
								}catch (ex){
									ex.printStackTrace()
									Log.error "Command threw exception", this.name
								}
							}
						}
					}
				}
			}
		}else if (this.type.id == BotType.SUFFIX.id){
			client.addListener("message create") { Map d ->
				for (c in commands){
					for (p in c.triggers){
						for (a in c.aliases){
							if ((d.message.content + " ").toLowerCase().startsWith(a.toLowerCase() + p.toLowerCase() + " ")){
								try{
									if (acceptOwnCommands){
										c.run(d)
									}else if (!(d.message.author.id == client.user.id)){
										c.run(d)
									}
								}catch (ex){
									ex.printStackTrace()
									Log.error "Command threw exception", this.name
								}
							}
						}
					}
				}
			}
		}else if (this.type.id == BotType.REGEX.id){
			client.addListener("message create") { Map d ->
				for (c in commands){
					for (p in c.triggers){
						for (a in c.aliases){
							if ((d.message.content + " ").toLowerCase() ==~ (p.toLowerCase() + a.toLowerCase() + " .*")){
								try{
									if (acceptOwnCommands){
										c.run(d)
									}else if (!(d.message.author.id == client.user.id)){
										c.run(d)
									}
								}catch (ex){
									ex.printStackTrace()
									Log.error "Command threw exception", this.name
								}
							}
						}
					}
				}
			}
		}
		if (!loggedIn){
			if (email.empty || password.empty) throw new Exception()
			client.login(email, password)
		}
	}

	/**
	 * A command.
	 * @author Hlaaftana
	 */
	abstract class Command{
		List triggers = []
		List aliases = []

		/**
		 * @param aliasOrAliases - A String or List of Strings of aliases this command will trigger with.
		 * @param triggerOrTriggers - A String or List of Strings this command will be triggerd by. Note that this is optional, and is CommandBot.defaultTrigger by default.
		 */
		Command(def aliasOrAliases, def triggerOrTriggers=CommandBot.this.trigger){
			if (aliasOrAliases instanceof List || aliasOrAliases instanceof Object[]){
				aliases.addAll(aliasOrAliases)
			}else{
				aliases.add(aliasOrAliases.toString())
			}
			if (triggerOrTriggers instanceof List || triggerOrTriggers instanceof Object[]){
				triggers.addAll(triggerOrTriggers)
			}else{
				triggers.add(triggerOrTriggers.toString())
			}
		}

		/**
		 * Gets the text after the command trigger for this command.
		 * @param d - the event data.
		 * @return the arguments as a string.
		 */
		def args(Map d){
			try{
				if (CommandBot.this.type.id == BotType.PREFIX.id){
					for (p in triggers){
						for (a in aliases){
							if ((d.message.content + " ").toLowerCase().startsWith(p.toLowerCase() + a.toLowerCase() + " ")){
								return d.message.content.substring((p + a + " ").length())
							}
						}
					}
				}else if (CommandBot.this.type.id == BotType.SUFFIX.id){
					for (p in triggers){
						for (a in aliases){
							if ((d.message.content + " ").toLowerCase().startsWith(a.toLowerCase() + p.toLowerCase() + " ")){
								return d.message.content.substring((a + p + " ").length())
							}
						}
					}
				}else if (CommandBot.this.type.id == BotType.REGEX.id){
					def value
					try{
						value = d.message.content.substring(this.allCaptures(d)[0].length() + 1)
					}catch (ex){
						value = ""
					}
					return value
				}
			}catch (ex){
				return ""
			}
		}

		// only for regex
		def captures(Map d){
			if (CommandBot.this.type.id == BotType.REGEX.id){
				return this.allCaptures(d).with { remove(0); delegate }
			}else{ return [this.args(d)] }
		}

		// only for regex
		def allCaptures(Map d){
			try{
				if (CommandBot.this.type.id == BotType.REGEX.id){
					for (p in triggers){
						for (a in aliases){
							if ((d.message.content + " ") ==~ (p + a + " .*")){
								def match = (d.message.content =~ p + a).collect{it}[0]
								List holyShit = (match instanceof String) ? [match] : match
								return holyShit
							}
						}
					}
				}else{
					throw new Exception("da")
				}
			}catch (ex){
				return [d.message.content.substring(0, d.message.content.indexOf(this.args(d))), this.args(d)]
			}
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
		String response

		/**
		 * @param response - a string to respond with to this command. <br>
		 * The rest of the parameters are Command's parameters.
		 */
		ResponseCommand(String response, def aliasOrAliases, def triggerOrTriggers=CommandBot.this.trigger){
			super(aliasOrAliases, triggerOrTriggers)
			this.response = response
		}

		def run(Map d){
			d.sendMessage(response)
		}
	}

	/**
	 * An implementation of Command with a closure response.
	 * @author Hlaaftana
	 */
	class ClosureCommand extends Command{
		Closure response

		/**
		 * @param response - a closure to respond with to this command. Can take one parameter, which is the data of the event.
		 */
		ClosureCommand(Closure response, def aliasOrAliases, def triggerOrTriggers=CommandBot.this.trigger){
			super(aliasOrAliases, triggerOrTriggers)
			response.delegate = this
			this.response = response
		}

		def run(Map d){
			response(d)
		}
	}
}
