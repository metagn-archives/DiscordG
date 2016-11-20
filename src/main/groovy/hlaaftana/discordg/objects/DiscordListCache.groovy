package hlaaftana.discordg.objects

import java.util.List
import java.util.Map

import hlaaftana.discordg.Client;
import hlaaftana.discordg.util.Cache;
import hlaaftana.discordg.util.DynamicList

class DiscordListCache extends Cache { // DO stands for DiscordObject
	Class<? extends DiscordObject> class_
	Client client
	DiscordListCache(List list, Client client, Class<? extends DiscordObject> class_ = DiscordObject){
		super(list.collectEntries { it instanceof DiscordObject ? [(it.id): it.object] : [(it.id): it] }, client)
		this.client = client
		this.class_ = class_
	}

	Map getMap(){
		Map map = [:]
		list.each {
			map[it.id] = it instanceof Map ? class_.newInstance(client, it) : it
		}
		map
	}

	List getList(){
		mapList.collect { class_.newInstance(client, it) }
	}

	List getMapList(){
		try{
			store.values().collect()
		}catch (ConcurrentModificationException ex){
			Thread.sleep 250
			mapList
		}
	}

	DynamicList getModifiableList(){
		DynamicList ass = mapList
		ass.on("set"){ element, index ->
			store[element.id] = element
		}
		ass.on("add"){ element, index ->
			add(element)
		}
		ass.on("remove"){ uhh ->
			if (uhh instanceof Integer) remove(ass[uhh])
			else remove(uhh)
		}
		ass
	}

	List setRawList(List newList){
		newList.each { store[it.id] = it instanceof DiscordObject ? it.object : it }
	}

	List getRawList(){
		store.values().collect()
	}

	def get(key){
		store[DiscordObject.id(key)]
	}

	def add(Map object){
		store[class_ == Member ? object.user.id : object.id] = object
	}

	def add(DiscordObject uh){
		store[uh.id] = uh.object
	}

	def remove(key){
		store.remove(DiscordObject.id(key))
	}
}