# a2b
ASkyBlock to BSkyBlock converter.

## Instructions
This software is provided AS-IS without warranty. Use at your own risk.

### Backup
Before you do anything, make a copy of your server. It is very important to do this so do not skip it because things can go wrong during the conversion process.

### Conversion
If your current server runs on 1.12.2 then you must upgrade your server to 1.13.2 at least.

1. Stop the server
2. Remove the ASkyBlock.jar from your plugins folder. Do NOT remove the ASkyBlock folder or worlds.
3. Install BentoBox to your plugins folder
4. Start the server with the --forceUpgrade option
5. After everything is fully loaded and you see the BentoBox logo, stop the server
6. Place BSkyBlock addon and the a2b addon into the BentoBox addons folder
7. Restart the server, again with the --forceUpgrade option
8. Once the server is loaded and you see the BentoBox logo, start conversion in the console by entering: a2b convert
9. After the conversion is complete, stop the server
10. Edit the BSkyBlock config.yml as you see fit in the settings.

