package hlaaftana.discordg.util

class ConversionUtil {
	static List imagable = [File, InputStream, URL, String, byte[]]

	static Map fixImages(Map data, String... keys = ["avatar", "icon"]){
		Map a = data.clone()
		keys.each { String key ->
			if (a.containsKey(key)){
				if (isImagable(a[key])){
					a[key] = ConversionUtil.encodeImage(a[key])
				}else{
					throw new IllegalArgumentException("$key cannot be resolved " +
						"for class ${data[key].getClass()}")
				}
			}
		}
		a
	}

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
			Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", string.replaceAll(/(?!\.)\d{3}\+00:00/, "+00:00"))
		}catch (ex){ null }
	}
}
