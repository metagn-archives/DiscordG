package hlaaftana.discordg.collections

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import hlaaftana.discordg.Client
import hlaaftana.discordg.DiscordObject
import hlaaftana.discordg.objects.Member

@CompileStatic
class DiscordListCache<T extends DiscordObject> extends Cache<String, Map<String, Object>> {
	Class<T> class_
	Client client
	DiscordListCache(List list, Client client, Class<T> class_ = (Class<T>) DiscordObject){
		super(superlister(list), client)
		this.client = client
		this.class_ = class_
	}

	private static Map superlister(List list) {
		def r = new HashMap(list.size())
		for (a in list) {
			if (a instanceof DiscordObject) r.put(((DiscordObject) a).id, ((DiscordObject) a).object)
			else r.put(getIdProperty(a), a)
		}
		r
	}

	@CompileDynamic
	private static String getIdProperty(it) { it.id.toString() }

	Class<T> 'class'() { class_ }
	Client client() { client }

	Map<String, T> map() {
		def l = list()
		Map<String, T> map = new HashMap<>(l.size())
		for (it in l) map.put(it.id, it instanceof Map ? class_.newInstance(client, it) : it)
		map
	}

	List<T> list() {
		def m = rawList()
		def a = new ArrayList<T>(m.size())
		for (x in m) a.add(class_.newInstance(client, x))
		a
	}

	T at(id) {
		class_.newInstance(client, DiscordObject.id(id))
	}

	List<Map<String, Object>> rawList() {
		try{
			store.values().collect()
		}catch (ConcurrentModificationException ignored){
			println 'inform me'
			Thread.sleep 250
			rawList()
		}
	}

	List<Map<String, Object>> rawList(List newList){
		for (it in newList) {
			if (it instanceof DiscordObject) put(((DiscordObject) it).id, ((DiscordObject) it).object)
			else if (it instanceof Map) put(((Map) it).id.toString(), it)
			else throw new UnsupportedOperationException("Unknown type in raw list ${it.class}")
		}
		newList
	}

	Map get(key){
		super.get(DiscordObject.id(key))
	}

	DiscordListCache plus(DiscordListCache other){
		new DiscordListCache(rawList() + other.rawList(), client, class_)
	}

	def add(Map object){
		put((class_ == Member ? ((Map<String, Object>) object.user).id : object.id).toString(), object)
	}

	def add(DiscordObject uh){
		store[uh.id] = uh.object
	}

	Map<String, Object> remove(key){
		store.remove(DiscordObject.id(key))
	}
}