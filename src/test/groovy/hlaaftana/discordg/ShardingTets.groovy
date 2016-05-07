package hlaaftana.discordg

import hlaaftana.discordg.oauth.BotClient

class ShardingTets { // that's right. tets
	static main(args){
		BotClient client = new BotClient()
		client.login(args[0])
	}
}
