package hlaaftana.discordg

import groovy.transform.CompileStatic
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer

import hlaaftana.discordg.dsl.*

@CompileStatic
class DSLMain {
	static void main(String[] args){
		ImportCustomizer imports = new ImportCustomizer()
		imports.addStarImports('hlaaftana.discordg', 'hlaaftana.discordg.dsl',
				'hlaaftana.discordg.objects', 'hlaaftana.discordg.status',
				'hlaaftana.discordg.net', 'hlaaftana.discordg.util', 'hlaaftana.discordg.util.bot',
				'hlaaftana.discordg.exceptions', 'hlaaftana.discordg.logic')
		CompilerConfiguration cc = new CompilerConfiguration()
		cc.addCompilationCustomizers(imports)
		cc.scriptBaseClass = DelegatingScript.name
		GroovyShell sh = new GroovyShell(new Binding(), cc)
		DelegatingScript script = (DelegatingScript) sh.parse(new File(args[0]))
		def dsl = new GroovyBot()
		script.delegate = dsl
		script.run()
		if (null != dsl.bot) dsl.bot.initialize()
		else if (null != dsl.client) dsl.client.login()
		else throw new IllegalArgumentException('Why run a DSL if you aren\'t going to use it?')
	}
}
