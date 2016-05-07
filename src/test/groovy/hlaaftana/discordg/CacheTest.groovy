package hlaaftana.discordg

import hlaaftana.discordg.objects.Cache

class CacheTest {
	static main(args){
		Cache cache = Cache.empty()
		cache["dong"] = true
		println cache
	}
}
