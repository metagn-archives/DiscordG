package hlaaftana.discordg

import groovy.transform.CompileStatic

@CompileStatic
class DiscordG {
	static final String VERSION = '5.1.1'
	static final String GITHUB = 'https://github.com/metagodcore/DiscordG'
	static final String USER_AGENT = "groovy/$GroovySystem.version DiscordBot ($GITHUB, $VERSION)"
	static final int DEFAULT_GATEWAY_VERSION = 9

	static Client withLogin(String email, String password, boolean threaded = true) {
		Client client = new Client()
		def a = { client.login(email, password) }
		if (threaded) Thread.start(a) else a()
		client
	}

	static Client withToken(String token, boolean bot = true, boolean threaded = true) {
		Client client = new Client()
		def a = { client.login(token, bot) }
		if (threaded) Thread.start(a) else a()
		client
	}

	static Client withToken(String botName, String token, boolean threaded = true) {
		Client client = new Client()
		def a = { client.login(botName) { token } }
		if (threaded) Thread.start(a) else a()
		client
	}
}
