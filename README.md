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

You can see more examples [here](https://github.com/hlaaftana/DiscordG/tree/master/examples). I provide explanations in some of them too.

## Where can I find the documentation?
All classes are Javadoc'd however I don't have a Javadoc distribution yet. So, in my opinion, using your IDE, you should check for the classes and their methods themselves and read their doc. Most of the methods you will use will originate from classes in ml.hlaaftana.discordg.objects, especially API and Client. You can also check the source code.

## Where do I install this?
Check the releases for this repository. You'll find .jar files which you can add to your build path in your IDE or add to -cp in your javac and java commands.