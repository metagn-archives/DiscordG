package hlaaftana.discordg.status

import groovy.transform.CompileStatic
import hlaaftana.discordg.util.ConversionUtil

@CompileStatic
class StatusPage extends MapObject {
	StatusPage(Map object) { super(object) }

	String getId() { (String) object.id }
	String getName() { (String) object.name }
	String getUrl() { (String) object.url }
	String getRawUpdatedAt() { (String) object.updated_at }
	Date getUpdatedAt() { ConversionUtil.fromJsonDate((String) object.updated_at) }
}
