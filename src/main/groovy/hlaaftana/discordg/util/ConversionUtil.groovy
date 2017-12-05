package hlaaftana.discordg.util

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

@CompileStatic
class ConversionUtil {
	static Set<Class> imagable = new HashSet<>([File, InputStream, URL, String, byte[]])
	private static int[] dateFields = [Calendar.YEAR, Calendar.MONTH,
		Calendar.DAY_OF_MONTH, Calendar.HOUR_OF_DAY,
		Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND] as int[]

	static String encodeImage(byte[] bytes, String type = 'jpg'){
		"data:image/$type;base64," + bytes.encodeBase64().toString()
	}

	static String encodeImage(String pathToImage){
		encodeImage(pathToImage ==~ /https?:\/\/(?:.|\n)*/ ?
			new URL(pathToImage) : new File(pathToImage))
	}

	@CompileDynamic
	static String encodeImage(imagable){
		encodeImage(getBytes(imagable))
	}

	static byte[] getBytes(thing){
		try { getBytesProperty(thing) }
		catch (ignored) { throw new UnsupportedOperationException("Cannot get byte array of $thing") }
	}

	@CompileDynamic
	private static byte[] getBytesProperty(thing) { thing.bytes }
	static byte[] getBytes(byte[] thing) { thing }
	static byte[] getBytes(File thing) { thing.bytes }
	static byte[] getBytes(InputStream thing) { thing.bytes }
	static byte[] getBytes(URL thing) { thing.bytes }
	static byte[] getBytes(String thing) { thing.bytes }
	static byte[] getBytes(ByteArrayOutputStream stream){ stream.toByteArray() }

	static boolean isImagable(thing){
		try{
			thing instanceof byte[] || imagable.contains(thing.class) || getBytes(thing) != null
		}catch (UnsupportedOperationException ignored){
			false
		}
	}

	static Date fromJsonDate(String string, TimeZone tz = TimeZone.getTimeZone('Etc/UTC')){
		Calendar cal = Calendar.getInstance(tz)
		cal.clear()
		def x = string.split(/\D+/)
		for (int i = 0; i < x.length; ++i) {
			cal.set(dateFields[i], new Integer(x[i]) + (i == 1 ? -1 : 0))
		}
		cal.time
	}
}
