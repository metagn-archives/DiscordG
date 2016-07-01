package hlaaftana.discordg.objects

import hlaaftana.discordg.Client;

class ChannelListCache extends DiscordListCache {
	ChannelListCache(List list, Client client){
		super(list, client, Channel)
	}

	Map getMap(){
		Map map = [:]
		list.each {
			map[it.id] = Channel.typed(it)
		}
		map
	}

	List getList(){
		super.list.collect { Channel.typed(it) }
	}
}
