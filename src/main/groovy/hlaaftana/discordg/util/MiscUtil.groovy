package hlaaftana.discordg.util

import java.awt.*
import java.util.List
import java.awt.datatransfer.*
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.Memoized
import groovy.transform.InheritConstructors
import hlaaftana.discordg.objects.DiscordObject
import hlaaftana.discordg.Client

/**
 * Utilities unrelated to the actual API itself. In fact, most are not even used.
 * @author Hlaaftana
 */
class MiscUtil {
	static Map<String, Integer> namedColors = [
		aliceblue: 0xf0f8ff, antiquewhite: 0xfaebd7, aqua: 0x00ffff,
		aquamarine: 0x7fffd4, azure: 0xf0ffff, beige: 0xf5f5dc, bisque: 0xffe4c4,
		black: 0x000000,  blanchedalmond: 0xffebcd, blue: 0x0000ff,
		blueviolet: 0x8a2be2, brown: 0xa52a2a, burlywood: 0xdeb887,
		cadetblue: 0x5f9ea0, chartreuse: 0x7fff00, chocolate: 0xd2691e,
		coral: 0xff7f50, cornflowerblue: 0x6495ed, cornsilk: 0xfff8dc,
		crimson: 0xdc143c, cyan: 0x00ffff, darkblue: 0x00008b, darkcyan: 0x008b8b,
		darkgoldenrod: 0xb8860b, darkgray: 0xa9a9a9, darkgreen: 0x006400,
		darkgrey: 0xa9a9a9, darkkhaki: 0xbdb76b, darkmagenta: 0x8b008b,
		darkolivegreen: 0x556b2f, darkorange: 0xff8c00, darkorchid: 0x9932cc,
		darkred: 0x8b0000, darksalmon: 0xe9967a, darkseagreen: 0x8fbc8f,
		darkslateblue: 0x483d8b, darkslategray: 0x2f4f4f, darkslategrey: 0x2f4f4f,
		darkturquoise: 0x00ced1, darkviolet: 0x9400d3, deeppink: 0xff1493,
		deepskyblue: 0x00bfff, dimgray: 0x696969, dimgrey: 0x696969,
		dodgerblue: 0x1e90ff, firebrick: 0xb22222, floralwhite: 0xfffaf0,
		forestgreen: 0x228b22, fuchsia: 0xff00ff, gainsboro: 0xdcdcdc,
		ghostwhite: 0xf8f8ff, gold: 0xffd700, goldenrod: 0xdaa520, gray: 0x808080,
		green: 0x008000, greenyellow: 0xadff2f, grey: 0x808080, honeydew: 0xf0fff0,
		hotpink: 0xff69b4, indianred: 0xcd5c5c, indigo: 0x4b0082, ivory: 0xfffff0,
		khaki: 0xf0e68c, lavender: 0xe6e6fa, lavenderblush: 0xfff0f5,
		lawngreen: 0x7cfc00, lemonchiffon: 0xfffacd, lightblue: 0xadd8e6,
		lightcoral: 0xf08080, lightcyan: 0xe0ffff, lightgoldenrodyellow: 0xfafad2,
		lightgray: 0xd3d3d3, lightgreen: 0x90ee90, lightgrey: 0xd3d3d3,
		lightpink: 0xffb6c1, lightsalmon: 0xffa07a, lightseagreen: 0x20b2aa,
		lightskyblue: 0x87cefa, lightslategray: 0x778899, lightslategrey: 0x778899,
		lightsteelblue: 0xb0c4de, lightyellow: 0xffffe0, lime: 0x00ff00,
		limegreen: 0x32cd32, linen: 0xfaf0e6, magenta: 0xff00ff, maroon: 0x800000,
		mediumaquamarine: 0x66cdaa, mediumblue: 0x0000cd, mediumorchid: 0xba55d3,
		mediumpurple: 0x9370db, mediumseagreen: 0x3cb371, mediumslateblue: 0x7b68ee,
		mediumspringgreen: 0x00fa9a, mediumturquoise: 0x48d1cc,
		mediumvioletred: 0xc71585, midnightblue: 0x191970, mintcream: 0xf5fffa,
		mistyrose: 0xffe4e1, moccasin: 0xffe4b5, navajowhite: 0xffdead,
		navy: 0x000080, oldlace: 0xfdf5e6, olive: 0x808000, olivedrab: 0x6b8e23,
		orange: 0xffa500, orangered: 0xff4500, orchid: 0xda70d6,
		palegoldenrod: 0xeee8aa, palegreen: 0x98fb98, paleturquoise: 0xafeeee,
		palevioletred: 0xdb7093, papayawhip: 0xffefd5, peachpuff: 0xffdab9,
		peru: 0xcd853f, pink: 0xffc0cb, plum: 0xdda0dd, powderblue: 0xb0e0e6,
		purple: 0x800080, red: 0xff0000, rosybrown: 0xbc8f8f, royalblue: 0x4169e1,
		saddlebrown: 0x8b4513, salmon: 0xfa8072, sandybrown: 0xf4a460,
		seagreen: 0x2e8b57, seashell: 0xfff5ee, sienna: 0xa0522d, silver: 0xc0c0c0,
		skyblue: 0x87ceeb, slateblue: 0x6a5acd, slategray: 0x708090,
		slategrey: 0x708090, snow: 0xfffafa, springgreen: 0x00ff7f,
		steelblue: 0x4682b4, tan: 0xd2b48c, teal: 0x008080, thistle: 0xd8bfd8,
		tomato: 0xff6347, turquoise: 0x40e0d0, violet: 0xee82ee, wheat: 0xf5deb3,
		white: 0xffffff, whitesmoke: 0xf5f5f5, yellow: 0xffff00,
		yellowgreen: 0x9acd32
	]

	static Random listRandom = new Random()
	
	@CompileStatic
	static Clipboard getClipboard(){ Toolkit.defaultToolkit.systemClipboard }
	
	@CompileStatic
	static copy(String content){
		clipboard.setContents(new StringSelection(content), null)
	}
	
	@CompileStatic
	static String paste(){
		clipboard.getContents(null).getTransferData(DataFlavor.stringFlavor)
	}
	
	@CompileStatic
	static LocalDateTime dateToLDT(Date date = new Date(),
		ZoneId tz = ZoneId.systemDefault()){
		LocalDateTime.ofInstant(date.toInstant(), tz)
	}

	static dump(list, newItem, Closure doto = Closure.IDENTITY){
		def mock = list
		mock += newItem
		mock = mock.collect { doto(it) }
		mock
	}

	static undump(list, newItem, Closure doto = Closure.IDENTITY){
		def mock = list
		mock -= newItem
		mock = mock.collect { doto(it) }
		mock
	}

	static defaultValueOnException(defaultValue = null,
		Collection<Class> exceptions = [Exception],
		Closure expr){
		try{
			expr()
		}catch (ex){
			if (exceptions.any { ex in it }) defaultValue
			else throw ex
		}
	}
		
	@CompileStatic
	static List splitWhen(iter, Closure cond){
		def a = [[]]
		iter.each {
			if (cond(it)) a.add([it])
			else a.last().add(it)
		}
		a
	}
	
	@CompileStatic
	@Memoized
	static String uncapitalize(String s){
		s ? s[0].toLowerCase() + s.substring(1) : s
	}
	
	@CompileStatic
	@Memoized
	static String removeFromStart(String s, CharSequence x){
		s.startsWith(x.toString()) ? s.substring(x.size()) : s
	}
	
	@CompileStatic
	@Memoized
	static String removeFromEnd(String s, CharSequence x){
		s.endsWith(x.toString()) ? s.substring(0, s.size() - x.size()) : s
	}
	
	@CompileStatic
	@Memoized
	static String removeFormatting(String s){
		s.replace('~', '\\~').replace('_', '\\_').replace('*', '\\*')
			.replace('```', '\u200b`\u200b`\u200b`').replace('`', '\\`')
			.replace(':', '\\:').replace('`', '\\`').replace('/', '\\/')
			.replace('@', '\\@').replace('<', '\\<').replace('>', '\\>')
	}
	
	@CompileStatic
	@Memoized
	static String surround(String s, String sur){ "$sur$s$sur" }
	
	@CompileStatic
	@Memoized
	static String bold(String s){ surround(s, '**') }
	@CompileStatic
	@Memoized
	static String bolden(String s){ surround(s, '**') }
	@CompileStatic
	@Memoized
	static String italic(String s, boolean underscore = false){
		surround(s, underscore ? '_' : '*') }
	@CompileStatic
	@Memoized
	static String italicize(String s, boolean underscore = false){
		surround(s, underscore ? '_' : '*') }
	@CompileStatic
	@Memoized
	static String underline(String s){ surround(s, '__') }
	@CompileStatic
	@Memoized
	static String strikethrough(String s){ surround(s, '~~') }
	@CompileStatic
	@Memoized
	static String code(String s){ surround(s, '`') }
	@CompileStatic
	@Memoized
	static String block(String s, String language = ""){ "```$language\n$s```" }

	/**
	 * Registers a bunch of methods to help you with Discord formatting to the String meta class.
	 */
	static registerStringMethods(){
		MiscUtil.metaClass.methods.findAll {
			it.declaringClass.theClass == MiscUtil &&
				!(it.name in ["getProperty", "invokeMethod", "setProperty"]) &&
				String in it.parameterTypes*.theClass }.each { a ->
			String.metaClass."$a.name" = { ...args -> MiscUtil."$a.name"(delegate, *args) }
		}
	}

	static registerCollectionMethods(){
		Collection.metaClass.allAreEqual = {
			def a = delegate[0]
			delegate.every(a.&equals)
		}
		Collection.metaClass.sample = { Random random = listRandom ->
			delegate[random.nextInt(delegate.size())]
		}
	}
}

abstract class CasingType {
	static final CasingType CAMEL = [
		toWords: { MiscUtil.splitWhen(it.toString().toCharArray()){ Character.isUpperCase((char)
			it) }*.join().collect(MiscUtil.&uncapitalize) },
		fromWords: { it[0] + it.drop(1)*.capitalize().join() }
	] as CasingType
	static final CasingType SNAKE = [
		toWords: { it.split('_') },
		fromWords: { it.join('_') }
	] as CasingType
	static final CasingType CONSTANT = [
		toWords: { it.toLowerCase().split('_') },
		fromWords: { it.join('_').toUpperCase() }
	] as CasingType
	static final CasingType PASCAL = [
		toWords: { MiscUtil.splitWhen(it.toString().toCharArray()){ Character.isUpperCase(it) }
			.drop(1)*.join().collect(MiscUtil.&uncapitalize) },
		fromWords: { it*.capitalize().join() }
	] as CasingType
	static final CasingType KEBAB = [
		toWords: { it.split('-') },
		fromWords: { it.join('-') }
	] as CasingType

	static Map defaultCases = [camel: CAMEL, snake: SNAKE,
		constant: CONSTANT, pascal: PASCAL, kebab: KEBAB]
	
	abstract toWords(words)
	abstract fromWords(words)
	
	String convert(String text, CasingType casing){
		casing.fromWords(toWords(text))
	}
}
