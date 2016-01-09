package ml.hlaaftana.discordg

import ml.hlaaftana.discordg.objects.API
import ml.hlaaftana.discordg.util.Log

/**
 * Utility for instantiating the API class.
 * @author Hlaaftana
 */
class APIBuilder {
	/**
	 * Instantiates an API object and logs in.
	 * @param email - the email to log in with.
	 * @param password - the password to log in with.
	 * @return the API object.
	 */
	static API build(String email, String password){
		API api = new API()
		api.login(email, password)
		return api
	}

	/**
	 * Instantiates an API object.
	 * @return the API object.
	 */
	static API build(){
		return new API()
	}
}
