package hlaaftana.discordg.objects

import java.text.SimpleDateFormat
import java.util.List
import java.awt.Color
import java.net.URL

import hlaaftana.discordg.Client;
import hlaaftana.discordg.util.*

/**
 * A member of a server. Extends the User object.
 * @author Hlaaftana
 */
class Member extends User {
	Member(Client client, Map object){
		super(client, object + object["user"], "/guilds/${object["guild_id"]}/members/${object == client ? "@me" : object["id"]}")
	}

	User getUser(){ new User(client, object["user"]) }
	String getNick(){ object["nick"] ?: name }
	String getRawNick(){ object["nick"] }
	Server getServer(){ client.server(object["guild_id"]) }
	Server getParent(){ server }
	String getRawJoinDate(){ object["joined_at"] }
	Date getJoinDate(){ ConversionUtil.fromJsonDate(rawJoinDate) }
	List<Role> getRoles(){
		object["roles"].collect { server.role(it) }
	}

	Role role(ass){ find(roles, ass) }

	Presence.Game getGame(){
		presence?.game ?: null
	}

	Presence getPresence(){
		server.presenceMap[id]
	}

	void edit(Map data){
		client.askPool("editMembers", server.id){
			requester.patch("", data)
		}
	}

	void changeNick(String newNick){ this == client ? client.requester.patch("guilds/${server.id}/members/@me", [nick: newNick]) : edit(nick: newNick) }
	void nick(String newNick){ changeNick(newNick) }
	void editNick(String newNick){ changeNick(newNick) }
	void resetNick(){ changeNick("") }

	void mute(){ edit(mute: true) }
	void unmute(){ edit(mute: false) }
	void deafen(){ edit(deaf: true) }
	void undeafen(){ edit(deaf: false) }
	void ban(int days = 0){ server.ban this, days }
	void unban(){ server.unban this }
	boolean isMute(){ object["mute"] }
	boolean isDeaf(){ object["deaf"] }
	boolean isDeafened(){ object["deaf"] }

	String getStatus(){
		presence?.status ?: "offline"
	}

	Role getPrimaryRole(){ roles.max { it.position } }
	int getColorValue(){ roles.findAll { it.colorValue != 0 }.max { it.position }?.colorValue ?: 0 }
	Color getColor(){ new Color(colorValue) }
	Permissions getPermissions(Permissions initialPerms = Permissions.ALL_FALSE){
		Permissions full = initialPerms + server.defaultRole.permissions
		for (Permissions perms in roles*.permissions){
			if (perms["administrator"]){
				full += Permissions.ALL_TRUE
				break
			}else{
				full += perms
			}
		}
		full
	}

	Permissions fullPermissionsFor(Channel channel){
		permissionsFor(channel, permissions)
	}

	void editRoles(List<Role> roles) {
		server.editRoles(this, roles)
	}

	void addRoles(List<Role> roles) {
		server.addRoles(this, roles)
	}

	void addRole(Role role){ addRoles([role]) }

	void kick() {
		server.kick(this)
	}

	void moveTo(channel){
		requester.patch("", ["channel_id": id(channel)])
	}

	User toUser(){ new User(client, object["user"]) }
	def asType(Class target){
		if (target == User) toUser()
		else super.asType(target)
	}
	String toString(){ nick }
}
