package hlaaftana.discordg.objects

@groovy.transform.InheritConstructors
class Region extends DiscordObject {
	String getSampleHostname(){ object["sample_hostname"] }
	int getSamplePort(){ object["sample_port"] }
	boolean isVip(){ object["vip"] }
	boolean isOptimal(){ object["optimal"] }
}
