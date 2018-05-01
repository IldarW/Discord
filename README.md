# Custom discord mod for Wurm Unlimited (Client)

Requires [Ago's Client Mod Launcher](https://github.com/ago1024/WurmClientModLauncher/releases) to run.

This mod is free software: you can redistribute it and/or modify it under the terms of the [GNU Lesser General Public License](http://www.gnu.org/licenses/lgpl-3.0.en.html) as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

**Description**
This mod transfers messages from arbitrary chat tab(Village chat for example) to Discord channel.

**Setup instructions**:
1. Install mod following [instructions](https://forum.wurmonline.com/index.php?/topic/134945-released-client-mod-loader) of Ago's Client Mod Launcher
2. Modify the Discord.properties file. This file must contain the discord bot token, the name of discord server and the transfer list.  
>_Example_:  
>_discordTransfers=Village->villagechat;Alliance->alliancechat_  
>Messages from village or alliance chats will be transferred to corresponding Discord chat rooms - villagechat and alliancechat respectively
3. Download [JDA-3.4.0_317-withDependencies.jar](https://github.com/DV8FromTheWorld/JDA/releases/tag/v3.4.0) and copy to the directory where WurmLauncher.exe file lies. 

**How to use**:

* In the game console type "discord on". Any messages you get in configured chats will be transferred to discord and vice versa.  
* If someone will type "!who" in the discord chat room - the bot will answer with the list of online players in corresponding chat tab. The timeout is 10 seconds.  
* "discord off" in game console will turn mod off.  
* "discord info" will print out every transfers you configured
