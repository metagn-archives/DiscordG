package hlaaftana.discordg.objects

import java.text.SimpleDateFormat
import java.util.List
import java.awt.Color
import java.net.URL

import hlaaftana.discordg.util.*

/**
 * A member of a server. Extends the User object.
 * @author Hlaaftana
 */
class Member extends User{
	Member(Client client, Map object){
		super(client, object + object["user"])
	}

	/**
	 * @return the User which this Member is.
	 */
	User getUser(){ return new User(client, this.object["user"]) }
	String getNick(){ return this.object["nick"] ?: this.name }
	String getRawNick(){ return this.object["nick"] }
	/**
	 * @return the server the member comes from.
	 */
	Server getServer(){ return client.getServerById(this.object["guild_id"]) }
	/**
	 * @return a timestamp of when the member joined the server.
	 */
	String getRawJoinDate(){ return this.object["joined_at"] }
	/**
	 * @return a Date object of when the member joined the server.
	 */
	Date getJoinDate(){ return ConversionUtil.fromJsonDate(this.rawJoinDate) }
	/**
	 * @return the roles this member has.
	 */
	List<Role> getRoles(){
		List array = this.object["roles"]
		List<Role> roles = new ArrayList<Role>()
		for (o in array){
			for (r in this.server.roles){
				if (o == r.id) roles.add(r)
			}
		}
		return roles
	}

	/**
	 * @return the current game the member is playing. Can be null.
	 */
	Presence.Game getGame(){
		return this.presence?.game ?: null
	}

	Presence getPresence(){
		return this.server.presenceMap[this.id]
	}

	/**
	 * Mutes or deafens the user.
	 * @param data - the map. <br>
	 * [mute: true, deaf: false]
	 */
	void edit(Map data){
		client.requester.patch("guilds/${this.server.id}/members/${this.id}", data)
	}

	String changeNick(String newNick){ edit(nick: newNick) }
	String nick(String newNick){ return changeNick(newNick) }
	String editNick(String newNick){ return changeNick(newNick) }
	String resetNick(){ return changeNick("") }

	void mute(){ this.edit(mute: true) }
	void unmute(){ this.edit(mute: false) }
	void deafen(){ this.edit(deaf: true) }
	void undeafen(){ this.edit(deaf: false) }
	void ban(){ this.server.ban(this) }
	void unban(){ this.server.unban(this) }
	boolean isMute(){ return this.object["mute"] }
	boolean isDeaf(){ return this.object["deaf"] }
	boolean isDeafened(){ return this.object["deaf"] }

	/**
	 * @return the status of the user. e.g. "online", "offline", "idle"...
	 */
	String getStatus(){
		return this.presence?.status ?: "offline"
	}

	Role getPrimaryRole(){ return this.roles.max { it.position } }
	int getColorValue(){ return this.roles.findAll { it.colorValue != 0 }.max { it.position }.colorValue }
	Color getColor(){ return new Color(this.colorValue) }
	Permissions getPermissions(Permissions initialPerms = Permissions.ALL_FALSE){
		Permissions full = initialPerms
		for (Permissions perms in this.roles*.permissions){
			if (perms["manageRoles"]){
				full += Permissions.ALL_TRUE
				break
			}else{
				full += perms
			}
		}
		return full
	}
	Permissions permissionsFor(Channel channel, Permissions initialPerms = Permissions.ALL_FALSE){
		Permissions doodle = initialPerms
		Permissions handicaps = Permissions.ALL_FALSE
		boolean doneWithHandicaps = false
		List allOverwrites = channel.permissionOverwrites.findAll { it.involves(this) }.sort { it.role ? it.position : server.roles.size() + 1 }
		for (Channel.PermissionOverwrite overwrite in allOverwrites){
			if (doodle["administrator"]){
				if (!doneWithHandicaps){
					handicaps = Permissions.CHANNEL_ALL_TRUE - overwrite.allowed
					if (overwrite.denied["managePermissions"]){
						doodle -= handicaps
						doneWithHandicaps = true
					}else{
						doodle += Permissions.CHANNEL_ALL_TRUE
					}
				}else{
					doodle += overwrite.allowed
					if (!overwrite.role || (overwrite.name == "@everyone" && overwrite.role)) doodle -= overwrite.denied
				}
				continue
			}
			if (overwrite.allowed["managePermissions"]){
				doodle += Permissions.CHANNEL_ALL_TRUE
				break
			}else{
				doodle += overwrite.allowed
				if (!overwrite.role || (overwrite.name == "@everyone" && overwrite.role)) doodle -= overwrite.denied
			}
		}
		return doodle
	}
	Permissions fullPermissionsFor(Channel channel){
		return permissionsFor(channel, this.permissions)
	}

	/**
	 * Overrides the roles for this member.
	 * @param roles - a list of roles to add.
	 */
	void editRoles(List<Role> roles) {
		this.server.editRoles(this, roles)
	}

	/**
	 * Adds roles to this member.
	 * @param roles - the roles to add.
	 */
	void addRoles(List<Role> roles) {
		this.server.addRoles(this, roles)
	}

	void addRole(Role role){ this.addRoles([role]) }

	/**
	 * Kicks the member from its server.
	 */
	void kick() {
		this.server.kick(this)
	}

	void moveTo(VoiceChannel channel){
		client.requester.patch("guilds/${this.server.id}/members/{this.id}", ["channel_id": channel.id])
	}

	User toUser(){ return new User(client, this.object["user"]) }
	def asType(Class target){
		if (target == User) return this.toUser()
		else return super.asType(target)
	}
}
