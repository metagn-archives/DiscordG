package io.github.hlaaftana.discordg

import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer

import io.github.hlaaftana.discordg.dsl.*

class DSLMain {
	static main(args){
		ImportCustomizer imports = new ImportCustomizer()
		imports.addStarImports("io.github.hlaaftana.discordg", "io.github.hlaaftana.discordg.dsl", "io.github.hlaaftana.discordg.objects", "io.github.hlaaftana.discordg.oauth", "io.github.hlaaftana.discordg.status", "io.github.hlaaftana.discordg.request", "io.github.hlaaftana.discordg.util")
		CompilerConfiguration cc = new CompilerConfiguration()
		cc.addCompilationCustomizers(imports)
		cc.scriptBaseClass = DelegatingScript.class.name
		GroovyShell sh = new GroovyShell(new Binding(), cc)
		DelegatingScript script = (DelegatingScript) sh.parse(new File(args[0]))
		script.setDelegate(new GroovyBot())
		script.run()
	}
}
