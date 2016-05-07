package hlaaftana.discordg.objects

import java.util.List
import java.util.Map

class DiscordListCache extends Cache { // DO stands for DiscordObject
	boolean map
	Class<? extends DiscordObject> class_
	Client client
	DiscordListCache(List list, Client client, Class<? extends DiscordObject> class_ = DiscordObject){
		super(list.collect { it instanceof DiscordObject ? [(it.id): it.object] : [(it.id): it] }.sum(), client)
		this.client = client
		this.map = list[0] instanceof Map
		this.class_ = class_
	}

	Map getMap(){
		Map map = [:]
		this.list.each {
			map[it.id] = it instanceof Map ? class_.newInstance(client, it) : it
		}
		return map
	}

	List getList(){
		return this.store.values().collect().collect { class_.newInstance(client, it) }
	}

	List getMapList(){
		return this.store.values().collect()
	}

	List setRawList(List newList){
		return newList.each { store[it.id] = it instanceof DiscordObject ? it.object : it }
	}

	List getRawList(){
		return this.store.values().collect()
	}

	def add(Map object){
		this.store[object.id] = object
	}

	def remove(String id){
		this.store.remove(id)
	}
}
