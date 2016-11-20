package hlaaftana.discordg.util.bot

import hlaaftana.discordg.net.JSONRequester

class YandexDictionary {
	String key

	List<String> languages(){
		if (!key) throw new IllegalArgumentException("No key")
		JSONRequester.get("https://dictionary.yandex.net/api/v1/dicservice.json/getLangs?key=$key")
	}

	Result lookup(String text, String lang = "en", String trto = "en"){
		if (!key) throw new IllegalArgumentException("No key")
		def json = JSONRequester.get("https://dictionary.yandex.net/api/v1/dicservice.json/lookup?" +
			"key=$key&lang=$lang-$trto&text=$text")
		new Result(definitions: json.def.collect {
			new Definition(text: it.text, partOfSpeech: it.pos,
				translations: it.tr.collect {
					new Translation(text: it.text, partOfSpeech: it.pos,
						synonyms: it.syn.collect { new Word(it) },
						meanings: it.mean.collect { new Word(it) },
						examples: it.ex.collect {
							new Example(text: it.text,
								translations: it.tr.collect { new Word(it) })
						})
				})
		})
	}

	class Result {
		List<Definition> definitions

		String toString(){ definitions.join "\n" }
	}

	class Word {
		String text
		String partOfSpeech

		def setPos(a){ partOfSpeech = a }

		String toString(){
			if (partOfSpeech) "$text ($partOfSpeech)"
			else text
		}
	}

	class Definition extends Word {
		List<Translation> translations

		String toString(){ translations.join "\n" }
	}

	class Translation extends Word {
		List<Word> synonyms
		List<Word> meanings
		List<Example> examples

		String toString(){
			"""\
$text ($partOfSpeech)
Meaning:
${meanings.join ";"}.
Synonyms:
${synonyms.join ","}
Examples:
${examples.join ";"}"""
		}
	}

	class Example extends Word {
		List<Word> translations
	}
}
