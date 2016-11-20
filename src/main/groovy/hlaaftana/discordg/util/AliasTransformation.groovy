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
			!(nodes[0].classNode?.name == Aliased.class.name) ||
			!(nodes[1].class in [MethodNode, FieldNode, PropertyNode])){
			addError("Internal error: wrong arguments", nodes[0], source)
			return
		}

		def declaringClass = nodes[1].declaringClass
		def adderMethodName = "add" +
			nodes[1].class.simpleName[0..-5]

		def alias = nodes[0].members["value"].text
		def aliasedNode = withNewName(nodes[1], alias)

		declaringClass."$adderMethodName"(aliasedNode)
	}

	void addError(String msg, ASTNode node, SourceUnit source) {
		def (line, col) = [node.lineNumber, node.columnNumber]
		SyntaxException se = new SyntaxException(msg + '\n', line, col)
		SyntaxErrorMessage sem = new SyntaxErrorMessage(se, source)
		source.errorCollector.addErrorAndContinue(sem)
	}

	static PropertyNode withNewName(PropertyNode node, String newName){
		new PropertyNode(newName, node.modifiers,
			node.type, node.originType, node.initialExpression,
			node.getterBlock, node.setterBlock)
	}

	static FieldNode withNewName(FieldNode node, String newName){
		new FieldNode(newName, node.modifiers,
			node.type, node.originType, node.initialExpression)
	}

	static MethodNode withNewName(MethodNode node, String newName){
		new MethodNode(newName, node.modifiers,
			node.returnType, node.parameters,
			node.exceptions, node.code)
	}
}
