package io.github.hlaaftana.discordg

import io.github.hlaaftana.discordg.objects.Client
import io.github.hlaaftana.discordg.util.Log
import io.github.hlaaftana.discordg.util.MiscUtil

class DiscordG {
	static final String VERSION = "3.0.0"
	static final String GITHUB = "https://github.com/hlaaftana/DiscordG"
	static final String USER_AGENT = "DiscordBot ($GITHUB, $VERSION)"

	/**
	 * Instantiates a client object and logs in with basic setup.
	 * @param email - the email to log in with.
	 * @param password - the password to log in with.
	 * @return the object.
	 */
	static Client withLogin(String email, String password){
		Client client = new Client()
		client.login(email, password)
		return client
	}
}
