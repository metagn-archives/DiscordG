# DiscordG

Really old (most development in 2016) wrapper for the Discord bot API in Groovy.
Code might not be the cleanest and some variable names are not pretty because I was
13 when I wrote most of this (a lot of commit names have been scrubbed for the same reason).
Consider using (not sure which of these are active anymore):

[JDA](https://github.com/DV8FromTheWorld/JDA)

[Discord4J](https://github.com/austinv11/Discord4J)

[Javacord](https://github.com/BtoBastian/Javacord)

Features, good or bad:

* No event enum or classes, so doesn't crash when Discord's API adds a new event.
* Raw websocket json listener
* Channel/message types are ints, but constants and helper methods exist.
* Permissions are handled
* No async. Threadpool for websocket events.
* No voice
* No embed DSL, you just use maps. So even JsonBuilder would work.
* Pretty much no features after audit logs, except intents
* Used to have a status package which supported the status.discordapp.com api.
* Small binary size (?) considering.
* DSL, see examples/dsl
* CommandBot, see examples/bot
* @CompileStatic pretty much everywhere, so as fast as regular Java
* `guild.member()`, `client.channel()` methods only work on names or IDs

```groovy
// discordg:
role.edit(name: "New role name", color: 0xFF00FF)

// discord4j:
role.edit(new Color(0xFF00FF), role.hoisted, "New role name", role.permissions, role.mentionable)

// jda:
role.managerUpdatable.nameField.setValue("New role name").colorField.setValue(new Color(0xFF00FF)).update().queue()
// alternatively
role.managerUpdatable.with {
    nameField.value = "New role name"
    colorField.value = new Color(0xFF00FF)
}.update().queue()

// javacord:
role.with {
    updateName "New role name" get()
    updateColor new Color(0xFF00FF) get()
}
// or same as discord4j except the edit method is called update
```

```groovy
def client = new Client()

def webhook = [webhook: true, id: 300000000000, token: "300000"]
client.sendMessage(webhook, "Hello")

def webhook = new Webhook(client)
webhook.id = 300000000000
webhook.token = "300000"
webhook.sendMessage("Hello")

def webhook = client.requestWebhook(300000000000, "300000")
```
