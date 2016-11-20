package hlaaftana.discordg.util

class HTMLFormUtil {
	static String encode(Map map){
		map.collect { k, v ->
			URLEncoder.encode(k.toString()) +
				"=" + URLEncoder.encode(v.toString())
		}.join("&")
	}

	static Map<String, String> parse(String string){
		string.tokenize("&")*.tokenize("=").collectEntries { k, v ->
			[(URLDecoder.decode(k)): URLDecoder.decode(v)]
		}
	}
}
