package hlaaftana.discordg

import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer

import hlaaftana.discordg.dsl.*

class DSLMain {
	static main(args){
		ImportCustomizer imports = new ImportCustomizer()
		imports.addStarImports("hlaaftana.discordg", "hlaaftana.discordg.dsl", "hlaaftana.discordg.objects", "hlaaftana.discordg.oauth", "hlaaftana.discordg.status", "hlaaftana.discordg.conn", "hlaaftana.discordg.util")
		CompilerConfiguration cc = new CompilerConfiguration()
		cc.addCompilationCustomizers(imports)
		cc.scriptBaseClass = DelegatingScript.class.name
		GroovyShell sh = new GroovyShell(new Binding(), cc)
		DelegatingScript script = (DelegatingScript) sh.parse(new File(args[0]))
		script.setDelegate(new GroovyBot())
		script.run()
	}
}
