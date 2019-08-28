# Converter
ASkyBlock to BSkyBlock converter.

## Compatibility

This converter should be used with the latest version of BentoBox and BSkyBlock.

## Introduction

This converter takes the ASkyBlock data files and creates new versions in the BSkyBlock/BentoBox database. The following items are converted:

* Players and teams
* Islands
* Warps
* Most Config.yml settings
* Challenges

The following are not converted:

* Schematics - not supported in BSkyBlock. Use BentoBox blueprints instead.
* Biomes - not supported in BSkyBlock itself. Use the Biome addon instead.
* Magic Cobblestone - not supported in BSkyBlock itself. An addon for this is in progress
* Acid water or rain settings - not supported in BSkyBlock.
* Team chat - not supported in BSkyBlock.
* Coop conversion - coops are handled differently in BSkyBlock so they will need redoing manually by players.
* Level related settings - not supported in BSkyBlock itself. Use the Level addon.

## Backup
**Warning:**
This software is provided AS-IS without warranty. Use at your own risk and make sure you make a backup copy of your server files and folders. It is very important to do this!

## Conversion
Note: No changes are made to the ASkyBlock world except by the server upgrading to the latest version. You will use the same world as you did before with BSkyBlock after conversion.

If your current server runs on 1.12.2 then you must upgrade your server to 1.14.4 at least.

**Steps**

Note that if you world is LARGE then you will need to change the timeout of the server so that the watchdog timer does not stop the server during conversion.

*You remembered to make a backup of your data right?*

0. Edit spigot.yml and change **timeout-time:** to something large, like 60000.
1. Stop the server and add the Spigot 1.14.4 (or later) server jar to your server folder.
2. Remove the ASkyBlock.jar from your plugins folder. Do NOT remove the ASkyBlock folder or worlds.
3. Install BentoBox Version 1.7.0 (or higher) to your plugins folder.
4. Start the new server with the **--forceUpgrade** option. This will upgrade all your worlds to the new format.
5. After everything is fully loaded and you see the BentoBox logo, stop the server.
6. Place the **BSkyBlock** addon, the **Challenges** addon, the **Warps** addon and the **Converter** addon into the BentoBox addons folder.
7. Restart the server, again with the **--forceUpgrade** option.
8. Once the server is loaded and you see the BentoBox logo, start conversion in the console by entering: **bsb convert**.
9. After the conversion is complete, stop the server. VERY IMPORTANT. STOP THE SERVER! DO NOT RELOAD!!! This will register the correct world generator.
10. Edit the BSkyBlock config.yml as you see fit in the settings.
11. Edit spigot.yml and return **timeout-time:** to something small, like 60.
12. (Optional) Remove the converter addon and the default BSkyBlock world folders that were created.
13. Restart the server. You do not need to use the forceUpgrade option any more. BSkyBlock addon will use the ASkyBlock world.

**Note:** Bentobox uses PAPI for placeholders. If you are interested in using placeholders, read the wikis.
**Note:** Challenges and Warps are not required. Converter can work without them, but data will not be converted.


