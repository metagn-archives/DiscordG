package ml.hlaaftana.discordg.status

import ml.hlaaftana.discordg.objects.MapObject;
import ml.hlaaftana.discordg.util.ConversionUtil

class StatusPage extends MapObject {
	StatusPage(Map object){ super(object) }

	String getId(){ this.object["id"] }
	String getName(){ this.object["name"] }
	String getUrl(){ this.object["url"] }
	String getRawUpdateTime(){ return this.object["updated_at"] }
	Date getUpdateTime(){ return ConversionUtil.fromJsonDate(this.object["updated_at"]) }
}
