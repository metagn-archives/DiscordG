package io.github.hlaaftana.discordg.util

import java.text.SimpleDateFormat
import java.util.Date;

class ConversionUtil {
	static String encodeToBase64(File image){
		return "data:image/jpg;base64," + image.bytes.encodeBase64().toString()
	}

	static String encodeToBase64(String pathToImage){
		return this.encodeToBase64(new File(pathToImage))
	}

	static Date fromJsonDate(String string){
		return new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSSSSXXX").parse(string)
	}
}
