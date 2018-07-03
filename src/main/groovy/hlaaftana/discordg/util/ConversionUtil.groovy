package hlaaftana.discordg.util

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

@CompileStatic
class ConversionUtil {
	static Set<Class> imagable = new HashSet<>([File, InputStream, URL, String, byte[]])
	private static int[] dateFields = [Calendar.YEAR, Calendar.MONTH,
		Calendar.DAY_OF_MONTH, Calendar.HOUR_OF_DAY,
		Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND] as int[]

	static String encodeImageBase64(byte[] bytes, String type = 'jpg') {
		"data:image/$type;base64," + bytes.encodeBase64().toString()
	}

	static String encodeImageBase64(String pathToImage) {
		encodeImageBase64(pathToImage ==~ /https?:\/\/(?:.|\n)*/ ?
			new URL(pathToImage) : new File(pathToImage))
	}

	static String encodeImageBase64(imagable) {
		if (imagable instanceof String) encodeImageBase64((String) imagable)
		else encodeImageBase64(getBytes(imagable))
	}

	@CompileDynamic
	static byte[] getBytes(thing) {
		try { getBytes$(thing) }
		catch (MissingMethodException ignore) { try { getBytesProperty(thing) }
		catch (ex) { throw new UnsupportedOperationException("Cannot get byte array of $thing", ex) } }
	}

	@CompileDynamic
	private static byte[] getBytesProperty(thing) { thing.bytes }
	static byte[] getBytes$(byte[] thing) { thing }
	static byte[] getBytes$(File thing) { thing.bytes }
	static byte[] getBytes$(InputStream thing) { thing.bytes }
	static byte[] getBytes$(URL thing) { thing.bytes }
	static byte[] getBytes$(String thing) { thing.bytes }
	static byte[] getBytes$(ByteArrayOutputStream stream) { stream.toByteArray() }

	static boolean isImagable(thing) {
		try{
			thing instanceof byte[] || imagable.contains(thing.class) || getBytes(thing) != null
		}catch (UnsupportedOperationException ignored) {
			false
		}
	}

	static Date fromJsonDate(boolean discord = true, String string, TimeZone tz = TimeZone.getTimeZone('Etc/UTC')){
		Calendar cal = Calendar.getInstance(tz)
		cal.clear()
		def x = string.split(/\D+/)
		for (int i = 0; i < Math.min(x.length, dateFields.length); ++i) {
			final field = dateFields[i]
			if (discord && field == Calendar.MILLISECOND) {
				cal.set(field, (int) (Integer.parseInt(x[i]) / 1000))
			} else {
				cal.set(field, Integer.parseInt(x[i]) + (i == 1 ? -1 : 0))
			}
		}
		cal.time
	}
}
