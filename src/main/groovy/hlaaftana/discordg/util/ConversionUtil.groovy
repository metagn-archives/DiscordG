package hlaaftana.discordg.util

import groovy.transform.CompileStatic
import groovy.transform.Memoized

class ConversionUtil {
	static List imagable = [File, InputStream, URL, String, byte[]]
	private static List<List<Integer>> dateFields =
		[[Calendar.YEAR, Calendar.MONTH,
		Calendar.DAY_OF_MONTH, Calendar.HOUR_OF_DAY,
		Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND],
		[0, -1, 0, 0, 0, 0, 0]].transpose()

	static String encodeImage(byte[] bytes, String type = "jpg"){
		"data:image/$type;base64," + bytes.encodeBase64().toString()
	}

	static String encodeImage(String pathToImage){
		encodeImage(pathToImage ==~ /https?:\/\/(?:.|\n)*/ ?
			new URL(pathToImage) : new File(pathToImage))
	}

	static String encodeImage(imagable){
		encodeImage(getBytes(imagable))
	}

	static byte[] getBytes(thing){
		if (thing instanceof byte[]) thing
		else if (thing.class in imagable) thing.bytes
		else throw new UnsupportedOperationException("Cannot get byte array of $thing")
	}

	static byte[] getBytes(ByteArrayOutputStream stream){
		stream.toByteArray()
	}

	static boolean isImagable(thing){
		try{
			thing instanceof byte[] || thing.class in imagable || getBytes(thing) != null
		}catch (UnsupportedOperationException ex){
			false
		}
	}

	static Date fromJsonDate(String string){
		try{
			Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", string
				.replaceAll(/\.(\d{6})\+/){ full, num -> '.' + num[0..2] + '+' })
		}catch (ex){ null }
	}
	
	@CompileStatic
	@Memoized
	static Date experimentalDateParser(String string, TimeZone tz = TimeZone.getTimeZone("Etc/UTC")){
		Calendar cal = Calendar.getInstance(tz)
		cal.clear()
		[dateFields, string.split(/\D+/)].transpose()
			.each { List<Integer> x, String y ->
				cal.set(x[0], x[1] + y.toInteger()) }
		cal.time
	}
}
