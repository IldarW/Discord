# Custom discord mod for Wurm Unlimited (Client)

Requires [Ago's Client Mod Launcher](https://github.com/ago1024/WurmClientModLauncher/releases) to run.

This mod is free software: you can redistribute it and/or modify it under the terms of the [GNU Lesser General Public License](http://www.gnu.org/licenses/lgpl-3.0.en.html) as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

**Setup instructions**:
1. Install mod following [instructions](https://forum.wurmonline.com/index.php?/topic/134945-released-client-mod-loader) of Ago's Client Mod Launcher
2. Modify the Discord.properties file. This file must contain the discord bot token, the name of discord server and the name of discord chat room.
3. Download [JDA-3.4.0_317-withDependencies.jar](https://github.com/DV8FromTheWorld/JDA/releases/tag/v3.4.0) and copy to the directory where WurmLauncher.exe file lies. 

**How to use**:

In the game console type "discord on". Any messages you get in village tab will be transferred to discord chat room and vice versa.

If someone will type "!who" in the discord chat room - the bot will answer with the list of online players in your village. The timeout is 10 seconds. 

"discord off" in game console will turn mod off.
