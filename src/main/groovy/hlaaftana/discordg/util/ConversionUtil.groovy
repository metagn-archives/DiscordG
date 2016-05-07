package hlaaftana.discordg.util

class ConversionUtil {
	static List imagable = [File, InputStream, URL, String, byte[]]

	static String encodeImage(byte[] bytes){
		return "data:image/jpg;base64," + bytes.encodeBase64().toString()
	}

	static String encodeImage(String pathToImage){
		return this.encodeImage(pathToImage ==~ /https?:\/\/(?:.|\n)*/ ? new URL(pathToImage) :new File(pathToImage))
	}

	static String encodeImage(File file){
		return encodeImage(file.bytes)
	}

	static String encodeImage(InputStream is){
		return encodeImage(is.bytes)
	}

	static String encodeImage(URL url){
		return encodeImage(url.bytes)
	}

	static Date fromJsonDate(String string){
		return {
			try {
				Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", string.replaceAll(/\d{3}\+00:00/, "+00:00"))
			}catch (ex){
				// god fucking kill me
			}
		}()
	}
}
