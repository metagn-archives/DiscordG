package hlaaftana.discordg.status

import groovy.transform.CompileStatic
import hlaaftana.discordg.util.ConversionUtil

@CompileStatic
class IncidentUpdate extends MapObject {
	IncidentUpdate(Map object) { super(object) }

	String getStatus() { (String) object.status }
	String getBody() { (String) object.body }
	String getRawUpdatedAt() { (String) object.updated_at }
	Date getUpdatedAt() { ConversionUtil.fromJsonDate((String) object.updated_at) }
	String getRawCreatedAt() { (String) object.created_at }
	Date getCreatedAt() { ConversionUtil.fromJsonDate((String) object.created_at) }
	String getRawDisplayAt() { (String) object.display_at }
	Date getDisplayAt() { ConversionUtil.fromJsonDate((String) object.display_at) }
	String getId() { (String) object.id }
	String getIncidentId() { (String) object.incident_id }
}
