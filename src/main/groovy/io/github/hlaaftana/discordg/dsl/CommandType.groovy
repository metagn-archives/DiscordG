package io.github.hlaaftana.discordg.dsl

import io.github.hlaaftana.discordg.util.bot.CommandBot.Command
import io.github.hlaaftana.discordg.util.bot.CommandBot.ResponseCommand
import io.github.hlaaftana.discordg.util.bot.CommandBot.ClosureCommand

class CommandType {
	static CommandType regularAbstract = new CommandType(Command, )

	Class clazz
	def builder
	CommandType(Class clazz, def builder){ this.clazz = clazz; this.builder = builder }
}
