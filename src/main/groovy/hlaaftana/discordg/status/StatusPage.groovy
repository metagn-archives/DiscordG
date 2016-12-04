package hlaaftana.discordg.status

import hlaaftana.discordg.util.ConversionUtil

class StatusPage extends MapObject {
	StatusPage(Map object){ super(object) }

	String getId(){ object["id"] }
	String getName(){ object["name"] }
	String getUrl(){ object["url"] }
	String getRawUpdatedAt(){ object["updated_at"] }
	Date getUpdatedAt(){ ConversionUtil.fromJsonDate(object["updated_at"]) }
}
