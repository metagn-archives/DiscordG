package hlaaftana.discordg

import hlaaftana.discordg.oauth.BotClient
import hlaaftana.discordg.objects.Client
import hlaaftana.discordg.util.Log
import hlaaftana.discordg.util.MiscUtil

class DiscordG {
	static final String VERSION = "3.1.0"
	static final String GITHUB = "https://github.com/hlaaftana/DiscordG"
	static final String USER_AGENT = "DiscordBot ($GITHUB, $VERSION)"

	static Client withLogin(String email, String password){
		Client client = new Client()
		client.login(email, password)
		return client
	}

	static BotClient withToken(String token){
		BotClient client = new BotClient()
		client.login(token)
		return client
	}

	static BotClient withToken(String botName, String token){
		BotClient client = new BotClient()
		client.login(botName){ token }
		return client
	}
}
