package ml.hlaaftana.discordg.util.bot

import ml.hlaaftana.discordg.objects.API
import ml.hlaaftana.discordg.objects.Event
import ml.hlaaftana.discordg.util.Log

class CommandBot {
	String name = "DiscordG|CommandBot"
	API api
	List commands = []
	static def defaultPrefix
	boolean acceptOwnCommands = false
	private boolean loggedIn = false

	CommandBot(API api, List commands=[]){
		this.api = api
		this.commands += commands
	}

	def addCommand(Command command){
		commands.add(command)
	}

	def addCommands(List<Command> commands){
		this.commands.addAll(commands)
	}

	def login(String email, String password){
		loggedIn = true
		api.login(email, password)
	}

	def initialize(String email="", String password=""){
		api.addListener("message create") { Event e ->
			for (c in commands){
				for (p in c.prefixes){
					for (a in c.aliases){
						if ((e.data.message.content + " ").toLowerCase().startsWith(p.toLowerCase() + a.toLowerCase() + " ")){
							try{
								if (acceptOwnCommands){
									c.run(e)
								}else if (!(e.data.message.author.id == api.client.user.id)){
									c.run(e)
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

	static abstract class Command{
		List prefixes = []
		List aliases = []

		Command(def aliasOrAliases, def prefixOrPrefixes=CommandBot.defaultPrefix){
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

		def args(Event e){
			try{
				for (p in prefixes){
					for (a in aliases){
						if ((e.data.message.content + " ").toLowerCase().startsWith(p.toLowerCase() + a.toLowerCase() + " ")){
							return e.data.message.content.substring((p + a + " ").length())
						}
					}
				}
			}catch (ex){
				return ""
			}
		}

		abstract def run(Event e)
	}

	static class ResponseCommand extends Command{
		String response

		ResponseCommand(String response, def aliasOrAliases, def prefixOrPrefixes=CommandBot.defaultPrefix){
			super(aliasOrAliases, prefixOrPrefixes)
			this.response = response
		}

		def run(Event e){
			e.data.sendMessage(response)
		}
	}

	static class ClosureCommand extends Command{
		Closure response

		ClosureCommand(Closure response, def aliasOrAliases, def prefixOrPrefixes=CommandBot.defaultPrefix){
			super(aliasOrAliases, prefixOrPrefixes)
			this.response = response
		}

		def run(Event e){
			if (response.maximumNumberOfParameters > 1){
				response(e, this)
			}else{
				response(e)
			}
		}
	}
}
