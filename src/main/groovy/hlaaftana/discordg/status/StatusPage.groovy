package hlaaftana.discordg.status

import hlaaftana.discordg.util.ConversionUtil

class StatusPage extends MapObject {
	StatusPage(Map object){ super(object) }

	String getId(){ object["id"] }
	String getName(){ object["name"] }
	String getUrl(){ object["url"] }
	String getRawUpdateTime(){ object["updated_at"] }
	Date getUpdateTime(){ ConversionUtil.fromJsonDate(object["updated_at"]) }
}
