# DiscordG
Not virus finder for computers.

Documentation in https://metagodcore.github.io/Cringegodcord/ downloads in releases page

[JDA](https://github.com/DV8FromTheWorld/JDA)

[Discord4J](https://github.com/austinv11/Discord4J)

[Javacord](https://github.com/BtoBastian/Javacord)

Dubious claims marked with +^(+=^=(+'=^'=^('=+)))

* no event enum or classes.
* Raw websocket json listener
* Channel/message types are ints, but can be accessed as
* Permissions are dealt with
* No async. Threadpool for websocket events.
* No voice
* No embed DSL, you just use maps. So even JsonBuilder would work.
* Pretty much no features after audit logs, except intents
* Does not have a status package which has support for the status.discordapp.com api.
* Has creepy package
* Small binary size +^(+=^=(+'=^'=^('=+))) considering.
* DSL, see examples/dsl
* CommandBot, see examples/bot
* @CompileStatic pretty much everywhere
* `guild.member()`, `client.channel()` methods pretty useless only work on names or IDs

```groovy
// discordg
role.edit(name: "ROLE FOR IMPKMAAAA", color: 0xFF00FF)

// discord4j
role.edit(new Color(0xFF00FF), role.hoisted, "ROLE FOR IMPKMAAAA", role.permissions, role.mentionable)

// jda
role.managerUpdatable.nameField.setValue("ROLE FOR IMPKMAAA").colorField.setValue(new Color(0xFF00FF)).update().queue()
// alternatively
role.managerUpdatable.with {
    nameField.value = "ROLE FOR IMPKMAAA"
    colorField.value = new Color(0xFF00FF)
}.update().queue()

// javacord
role.with {
    updateName "ROLE FOR IMPKMAA" get()
    updateColor new Color(0xFF00FF) get()
}
// or same as discord4j except the edit method is called update
```

```groovy
def client = new Client()

def webhook = [webhook: true, id: 300000000000, token: "300000"]
client.sendMessage(webhook, "Whats up")

def webhook = new Webhook(client)
webhook.id = 300000000000
webhook.token = "300000"
webhook.sendMessage("Whats up")

def webhook = client.requestWebhook(300000000000, "300000")
```
