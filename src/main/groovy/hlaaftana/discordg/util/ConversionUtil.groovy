package hlaaftana.discordg.util

class ConversionUtil {
	static List imagable = [File, InputStream, URL, String, byte[]]

	static String encodeImage(byte[] bytes){
		return "data:image/jpg;base64," + bytes.encodeBase64().toString()
	}

	static String encodeImage(String pathToImage){
		return this.encodeImage(pathToImage ==~ /https?:\/\/(?:.|\n)*/ ? new URL(pathToImage) : new File(pathToImage))
	}

	static String encodeImage(imagable){
		return encodeImage(getBytes(imagable))
	}

	static byte[] getBytes(thing){
		if (thing instanceof byte[]) return thing
		else if (thing.class in imagable) return thing.bytes
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
		Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", string.replaceAll(/(?!\.)\d{3}\+00:00/, "+00:00"))
	}
}
