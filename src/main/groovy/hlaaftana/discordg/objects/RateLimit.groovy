package hlaaftana.discordg.objects

class RateLimit extends MapObject {
	RateLimit(Map object){ super(object) }

	String getBucket(){ return this.object["bucket"] }
	String getMessage(){ return this.object["message"] }
	long getRetryTime(){ return this.object["retry_after"] }
}
