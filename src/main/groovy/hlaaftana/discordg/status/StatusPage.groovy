package hlaaftana.discordg.status

import hlaaftana.discordg.util.ConversionUtil

class StatusPage extends MapObject {
	StatusPage(Map object) { super(object) }

	String getId() { (String) object.id }
	String getName() { (String) object.name }
	String getUrl() { (String) object.url }
	String getRawUpdatedAt() { (String) object.updated_at }
	Date getUpdatedAt() { ConversionUtil.fromJsonDate(object.updated_at) }
}
