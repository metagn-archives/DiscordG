package hlaaftana.discordg

import hlaaftana.discordg.util.Log
import hlaaftana.discordg.util.MiscUtil

class DiscordG {
	static final String VERSION = "4.2.0"
	static final String GITHUB = "https://github.com/hlaaftana/DiscordG"
	static final String USER_AGENT = "groovy/$GroovySystem.version DiscordBot ($GITHUB, $VERSION)"

	static Client withLogin(String email, String password){
		Client client = new Client()
		client.login(email, password)
		return client
	}

	static Client withToken(String token, boolean bot = true){
		Client client = new Client()
		client.login(token, bot)
		return client
	}

	static Client withToken(String botName, String token){
		Client client = new Client()
		client.login(botName){ token }
		return client
	}
}
