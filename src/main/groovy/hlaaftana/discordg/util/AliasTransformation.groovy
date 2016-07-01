package hlaaftana.discordg.util

import org.codehaus.groovy.ast.*
import org.codehaus.groovy.control.*
import org.codehaus.groovy.control.messages.SyntaxErrorMessage
import org.codehaus.groovy.syntax.SyntaxException
import org.codehaus.groovy.transform.*
import javax.swing.JOptionPane

@GroovyASTTransformation(phase = CompilePhase.INSTRUCTION_SELECTION)
class AliasTransformation implements ASTTransformation {
	void visit(ASTNode[] nodes, SourceUnit source) {
		if (!nodes || !nodes[0] || !nodes[1] ||
			!(nodes[0] instanceof AnnotationNode) ||
			!(nodes[0].classNode?.name == Aliased.class.name)){
			addError("Internal error: wrong arguments", nodes[0], source)
			return
		}
		nodes[0].members.values()*.class.toString()
	}

	void addError(String msg, ASTNode node, SourceUnit source) {
		def (line, col) = [node.lineNumber, node.columnNumber]
		SyntaxException se = new SyntaxException(msg + '\n', line, col)
		SyntaxErrorMessage sem = new SyntaxErrorMessage(se, source)
		source.errorCollector.addErrorAndContinue(sem)
	}
}
