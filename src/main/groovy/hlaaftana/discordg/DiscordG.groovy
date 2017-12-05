package hlaaftana.discordg

import groovy.transform.CompileStatic

@CompileStatic
class DiscordG {
	static final String VERSION = '4.2.0'
	static final String GITHUB = 'https://github.com/hlaaftana/DiscordG'
	static final String USER_AGENT = "groovy/$GroovySystem.version DiscordBot ($GITHUB, $VERSION)"

	static Client withLogin(String email, String password){
		Client client = new Client()
		client.login(email, password)
		client
	}

	static Client withToken(String token, boolean bot = true){
		Client client = new Client()
		client.login(token, bot)
		client
	}

	static Client withToken(String botName, String token){
		Client client = new Client()
		client.login(botName){ token }
		client
	}
}
