package hlaaftana.discordg

import groovy.transform.CompileStatic
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer

import hlaaftana.discordg.dsl.*

@CompileStatic
class DSLMain {
	static void main(String[] args) {
		def imports = new ImportCustomizer()
		imports.addStarImports('hlaaftana.discordg', 'hlaaftana.discordg.dsl',
				'hlaaftana.discordg.data',
				'hlaaftana.discordg.net', 'hlaaftana.discordg.util', 'hlaaftana.discordg.util.bot',
				'hlaaftana.discordg.exceptions', 'hlaaftana.discordg.logic')
		def cc = new CompilerConfiguration()
		cc.addCompilationCustomizers(imports)
		cc.scriptBaseClass = DelegatingScript.name
		def sh = new GroovyShell(new Binding(), cc)
		def script = (DelegatingScript) sh.parse(new File(args[0]))
		def dsl = new GroovyBot()
		script.delegate = dsl
		script.run()
		if (null != dsl.bot) dsl.bot.initialize()
		else if (null != dsl.client) Thread.start { dsl.client.login() }
		else throw new IllegalArgumentException('Why run a DSL if you aren\'t going to use it?')
	}
}
