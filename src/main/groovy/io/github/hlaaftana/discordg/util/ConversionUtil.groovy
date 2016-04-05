package io.github.hlaaftana.discordg.util

class ConversionUtil {
	static String encodeToBase64(File image){
		return "data:image/jpg;base64," + image.bytes.encodeBase64().toString()
	}

	static String encodeToBase64(String pathToImage){
		return this.encodeToBase64(new File(pathToImage))
	}

	static Date fromJsonDate(String string){
		return {
			try {
				Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", string.replaceAll(/\d{3}\+00:00/, "+00:00"))
			}catch (ex){
				
			}
		}()
	}
}
