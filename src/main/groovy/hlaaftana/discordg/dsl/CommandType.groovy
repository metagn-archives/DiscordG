package hlaaftana.discordg.dsl

class CommandType {
	static CommandType regularAbstract = new CommandType( )

	Class clazz
	def builder
	CommandType(Class clazz, def builder){ this.clazz = clazz; this.builder = builder }
}
