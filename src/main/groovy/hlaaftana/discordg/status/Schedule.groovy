package io.github.hlaaftana.discordg.status

import io.github.hlaaftana.discordg.objects.MapObject

class Schedule extends MapObject {
	Schedule(Map object){ super(object) }

	StatusPage getPage(){ return new StatusPage(this.object["page"]) }
	StatusPage getStatusPage(){ return new StatusPage(this.object["page"]) }
	List<Maintenance> getScheduledMaintenances(){ return this.object["scheduled_maintanences"].collect { new Maintenance(it) } }
	List<Maintenance> getMaintenances(){ return this.object["scheduled_maintanences"].collect { new Maintenance(it) } }
}
