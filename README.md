# DiscordG
Developed by Hlaaftana.

To ask me any questions other than "what's the difference between Groovy and Java", go [here](https://discord.gg/0SBTUU1wZTUMeVC4). If you were going to ask the difference between Groovy and Java, [see for yourself](http://www.vogella.com/tutorials/Groovy/article.html).

## What is this?
This is a Groovy wrapper for the complicated connection stuff Discord needs you to do to connect to it and use its services.

## What's Groovy?
Groovy is a programming language which compiles into Java Virtual Machine bytecode, so you can run it like regular Java code, however don't write it like it.

Groovy can be written in script form or regular Java form. The examples we give here are scripts, and I personally recommend running bots on scripts.

## Why is this Groovy?
Well, there are tons of other wrappers for Discord for different languages out there (which you can see the list of and see the server for discussing them [here](https://www.reddit.com/r/discordapp/comments/3hgipw/unofficial_discord_api_server_reverse_engineering/))
and I really like Groovy myself. You can also technically use this in Java, however there are way too many Java libs and people would be unimpressed if I made this Java. Suit yourself however.

## OK, how do I "use" it?
Here's a couple of simple bots that responds to "!ping":

```groovy
import hlaaftana.discordg.objects.BotClient
import hlaaftana.discordg.objects.Events

BotClient client = new BotClient()
client.addListener(Events.MESSAGE) { Map d ->
  if (d.message.content.startsWith("!ping")){
    d.sendMessage("Pong!")
  }
}
client.login("token")
```

```groovy
import hlaaftana.discordg.objects.BotClient
import hlaaftana.discordg.objects.Events
import hlaaftana.discordg.DiscordG

BotClient client = DiscordG.withToken("token")
client.addListener(Events.MESSAGE) { Map d ->
  if (d.message.content.startsWith("!ping")){
    d.sendMessage("Pong!")
  }
}
```

**PLEASE** check out the examples [here](https://github.com/hlaaftana/DiscordG/tree/master/examples). I provide important explanations in some of them (PingPong and PingPongPlus especially)

Oh, by the way,

## Where can I find the documentation?
[Here](http://hlaaftana.ml/discordg/docs/), for one.

You can also check the source code which is populated with Groovydoc.

If you need further help, contact me [here](https://discord.gg/0SBTUU1wZTUMeVC4).

## Where do I install this?
Check the releases for this repository. You'll find .jar files which you can add to your build path in your IDE or add to -cp in your javac and java commands.

# Dependencies:

[Unirest](http://unirest.io/java.html)

[Jetty](http://www.eclipse.org/jetty/)