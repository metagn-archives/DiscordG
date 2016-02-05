package ml.hlaaftana.discordg.objects

class Region extends DiscordObject {
	Region(API api, Map object){ super(api, object) }

	String getSampleHostname(){ return this.object["sample_hostname"] }
	int getSamplePort(){ return this.object["sample_port"] }
	boolean isVip(){ return this.object["vip"] }
}
