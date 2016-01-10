# DiscordG
Developed by Hlaaftana.

## What is this?
This is a Groovy wrapper for the complicated connection stuff Discord needs you to do to connect to it and use its services.

## What's Groovy?
Groovy is a programming language which compiles into Java Virtual Machine bytecode, so you can run it like regular Java code, however don't write it like it.

Groovy can be written in script form or regular Java form. The examples we give here are scripts, and I personally recommend running bots on scripts.

## Why is this Groovy?
Well, there are tons of other wrappers for Discord for different languages out there (which you can see the list of and see the server for discussing them [here](https://www.reddit.com/r/discordapp/comments/3hgipw/unofficial_discord_api_server_reverse_engineering/))
and I really like Groovy myself. You can also technically use this in Java, however there are way too many Java libs and people would be unimpressed if I made this Java. Suit yourself however.

## OK, how do I "use" it?
Here's a simple bot that responds to "!ping":

```groovy
import ml.hlaaftana.discordg.objects.API
import ml.hlaaftana.discordg.objects.Event
import ml.hlaaftana.discordg.APIBuilder

API api = APIBuilder.build("example@example.com", "example123")
api.addListener("message create") { Event e ->
  if (e.data.message.content.startsWith("!ping")){
    e.data.message.channel.sendMessage("Pong!")
  }
}
```

**PLEASE** check out the examples [here](https://github.com/hlaaftana/DiscordG/tree/master/examples). I provide important explanations in some of them (PingPong and PingPongPlus especially)

## Where can I find the documentation?
[Here](http://hlaaftana.ml/discordg/docs/), for one.

You can also check the source code which is populated with Groovydoc.

## Where do I install this?
Check the releases for this repository. You'll find .jar files which you can add to your build path in your IDE or add to -cp in your javac and java commands.

# Dependencies:

[Unirest](http://unirest.io/java.html)

[Jetty](http://www.eclipse.org/jetty/)