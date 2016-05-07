package hlaaftana.discordg.objects

class ChannelListCache extends DiscordListCache {
	ChannelListCache(List list, Client client){
		super(list, client, Channel)
	}

	Map getMap(){
		Map map = [:]
		this.list.each {
			map[it.id] = Channel.typed(it)
		}
		return map
	}

	List getList(){
		return super.list.collect { Channel.typed(it) }
	}
}
