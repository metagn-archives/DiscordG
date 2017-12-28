# DiscordG
Discord library for Groovy. Documentation in https://hlaaftana.github.io/DiscordG downloads in releases page

Now you're like woah. Why would you make another Java library if there are already three!

([JDA](https://github.com/DV8FromTheWorld/JDA) |  [Discord4J](https://github.com/austinv11/Discord4J) |  [Javacord](https://github.com/BtoBastian/Javacord))


I am about to list the pros and cons, but first some that might be both pros and cons.

* Uses Groovy's JSON parsers which are very fast and/but directly return Java objects.

* Caches use JSON objects instead of immutable objects. This also allows for raw Discord data to be
observed. So if you did `group.object.application_id`, you would get the application_id Discord gave.

* No interfaces for Discord objects.

* When you call addListener, the event name is passed through being uppercased, changing SERVER to GUILD and
CHANGE to UPDATE, replacing spaces with _, being passed to a map of aliases and then finally becoming the raw Discord
event name. For example, "server deleted" => "SERVER DELETED" => "GUILD DELETED" => "GUILD_DELETED" => "GUILD_DELETE".
This also means no event enum or classes.

* Has raw listeners which take the JSON payload directly without the need for fluff DiscordG automatically adds.

* Channel/message types are ints, but can be accessed as constants from interfaces that extend Types, such as
MessageTypes, ChannelTypes etc. Also helper methods for channel types exist in channels.

* Permissions are dealt with by using a Permissions object which is an integer box. You can do
`perm['readMessages']`, `perm[Permissions.BitOffsets.READ_MESSAGES]` or `perm[10]` where 10 is the value of
`Permissions.BitOffsets.READ_MESSAGES.offset`. You can get the permissions of a user in a channel by doing
`user.permissionsFor(channel)` which returns a Permissions object.

* Webhooks still need Client instances, but the Client doesn't have to login. For example:
```groovy
def webhook = [id: 388383836469329933, token: "somethingludicrous"]
def client = new Client()
client.sendMessage(webhook, "Whats up")

// or something hackier

def client = new Client()
def webhook = new Webhook(client, [id: 388383836469329933, token: "somethingludicrous"])
webhook.sendMessage("Whats up")

// do note methods like webhook.name won't work for either of these. version superior to both:

def client = new Client()
def webhook = client.requestWebhook(388383836469329933, "somethingludicrous")
```

* Has a status package which has support for the status.discordapp.com api.

* Has util package with help for JSON, converting between camel/snake/constant cases, images to base64,
a command system in util.bot, a Cleverbot.io wrapper and a custom barebones dynamic Groovy logger.

because i dont poop i will list the cons before the pros

* No async. Threadpool for websocket events.

* Slight performance dip compared to Java. This used to be bigger because i didnt use @CompileStatic or wasnt
smart enough to optimize some poops.

* Other problems native to Groovy.

* No voice, although I personally don't care

* Lacking an embed DSL, however you can just use maps. So even JsonBuilder would work.

* Lacking users

* More. I will either list or fix when i find out

fun things:

* Extensive selection of wrapper methods.

* Small binary size considering all the Closure anonymous classes.

* Has a DSL option you can use on .bot files if you do `java -jar DiscordG.jar file.bot`
or if you download the zip/tar `bin/DiscordG file.bot` (example of .bot files in examples/dsl)

* Has a CommandBot which has a command DSL (see examples/bot)

* Has a .listen method which delegates the event data to the closure.

* Uses @CompileStatic pretty much everywhere, but you dont have to.

* `guild.member()`, `client.channel()` etc methods that check the argument for a pattern, and search the parent for the
child. Inspired by discordrb.

* Expressive statements. Compare:

```groovy
// discordg
role.edit(name: "ROLE FOR IMPKMAAAA", color: 0xFF00FF)

// discord4j
role.edit(new Color(0xFF00FF)), role.hoisted, "ROLE FOR IMPKMAAAA", role.permissions, role.mentionable)

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
