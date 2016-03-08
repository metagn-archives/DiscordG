package ml.hlaaftana.discordg.util.bot

import ml.hlaaftana.discordg.APIBuilder
import ml.hlaaftana.discordg.objects.API
import ml.hlaaftana.discordg.util.Log

/**
 * A simple bot implementation.
 * @author Hlaaftana
 */
class RegexCommandBot {
	String name = "DiscordG|CommandBot"
	API api
	List commands = []
	static def defaultPrefix
	boolean acceptOwnCommands = false
	private boolean loggedIn = false

	/**
	 * @param api - The API object this bot should use.
	 * @param commands - A List of Commands you want to register right off the bat. Empty by default.
	 */
	RegexCommandBot(API api, List commands=[]){
		this.api = api
		this.commands += commands
	}

	static def create(String email, String password, List commands=[]){
		return new RegexCommandBot(APIBuilder.build(email, password), commands)
	}

	static def create(List commands=[]){
		return new RegexCommandBot(APIBuilder.build(), commands)
	}


	/**
	 * Adds a command.
	 * @param command - the command.
	 */
	def addCommand(Command command){
		commands.add(command)
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
		api.login(email, password)
	}

	/**
	 * Starts the bot. You don't have to enter any parameters if you ran #login already.
	 * @param email - the email to log in with.
	 * @param password - the password to log in with.
	 */
	def initialize(String email="", String password=""){
		api.addListener("message create") { Map d ->
			for (c in commands){
				for (p in c.prefixes){
					for (a in c.aliases){
						if ((d.message.content + " ").toLowerCase() ==~ (p.toLowerCase() + a.toLowerCase() + " .*")){
							try{
								if (acceptOwnCommands){
									c.run(d)
								}else if (!(d.message.author.id == api.client.user.id)){
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
		if (!loggedIn){
			if (email.empty || password.empty) throw new Exception()
			api.login(email, password)
		}
	}

	/**
	 * A command.
	 * @author Hlaaftana
	 */
	static abstract class Command{
		List prefixes = []
		List aliases = []

		/**
		 * @param aliasOrAliases - A String or List of Strings of aliases this command will trigger with.
		 * @param prefixOrPrefixes - A String or List of Strings this command will be triggered by. Note that this is optional, and is CommandBot.defaultPrefix by default.
		 */
		Command(def aliasOrAliases, def prefixOrPrefixes=RegexCommandBot.defaultPrefix){
			if (aliasOrAliases instanceof List || aliasOrAliases instanceof Object[]){
				aliases.addAll(aliasOrAliases)
			}else{
				aliases.add(aliasOrAliases.toString())
			}
			if (prefixOrPrefixes instanceof List || prefixOrPrefixes instanceof Object[]){
				prefixes.addAll(prefixOrPrefixes)
			}else{
				prefixes.add(prefixOrPrefixes.toString())
			}
		}

		// listen
		// i hate regex
		// and love it
		// just don't criticize me for this code

		/**
		 * Gets the text after the command trigger for this command.
		 * @param d - the event data.
		 * @return the arguments as a string.
		 */
		def args(Map d){
			def value
			try{
				value = d.message.content.substring(this.allCaptures(d)[0].length() + 1)
			}catch (ex){
				value = ""
			}
			return value
		}

		def captures(Map d){
			return this.allCaptures(d).with { remove(0); delegate }
		}

		def allCaptures(Map d){
			try{
				for (p in prefixes){
					for (a in aliases){
						if ((d.message.content + " ") ==~ (p + a + " .*")){
							def match = (d.message.content =~ p + a).collect{it}[0]
							List holyShit = (match instanceof String) ? [match] : match
							return holyShit
						}
					}
				}
			}catch (ex){
				return []
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
	static class ResponseCommand extends Command{
		String response

		/**
		 * @param response - a string to respond with to this command. <br>
		 * The rest of the parameters are Command's parameters.
		 */
		ResponseCommand(String response, def aliasOrAliases, def prefixOrPrefixes=CommandBot.defaultPrefix){
			super(aliasOrAliases, prefixOrPrefixes)
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
	static class ClosureCommand extends Command{
		Closure response

		/**
		 * @param response - a closure to respond with to this command. Can take one parameter, which is the data of the event.
		 */
		ClosureCommand(Closure response, def aliasOrAliases, def prefixOrPrefixes=CommandBot.defaultPrefix){
			super(aliasOrAliases, prefixOrPrefixes)
			response.delegate = this
			this.response = response
		}

		def run(Map d){
			response(d)
		}
	}
}
