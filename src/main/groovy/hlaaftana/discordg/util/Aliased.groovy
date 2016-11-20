package hlaaftana.discordg.util

import java.lang.annotation.*
import org.codehaus.groovy.transform.GroovyASTTransformationClass

@Retention(RetentionPolicy.SOURCE)
@Target([ElementType.METHOD, ElementType.FIELD,
	ElementType.TYPE, ElementType.LOCAL_VARIABLE])
@GroovyASTTransformationClass(["hlaaftana.discordg.util.AliasTransformation"])
@interface Aliased {
	String value()
}
