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
				'hlaaftana.discordg.net', 'hlaaftana.discordg.util')
		CompilerConfiguration cc = new CompilerConfiguration()
		cc.addCompilationCustomizers(imports)
		cc.scriptBaseClass = DelegatingScript.name
		GroovyShell sh = new GroovyShell(new Binding(), cc)
		DelegatingScript script = (DelegatingScript) sh.parse(new File(args[0]))
		script.delegate = new GroovyBot()
		script.run()
	}
}
