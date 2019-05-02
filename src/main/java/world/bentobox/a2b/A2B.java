package world.bentobox.a2b;

import java.io.File;
import java.io.IOException;

import com.google.common.io.Files;

import world.bentobox.a2b.commands.AdminConvertCommand;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bskyblock.BSkyBlock;

/**
 * Convert ASkyBlock to BSkyBlock
 * @author tastybento
 */
public class A2B extends Addon {

    @Override
    public void onLoad() {
        // Import settings
        importASkyBlockconfig();
    }

    @Override
    public void onEnable(){
        if(getAddonByName("BSkyBlock").map(BSkyBlock.class::cast).map(gm -> {
            // Register commands
            gm.getAdminCommand().ifPresent(adminCommand ->  {
                new AdminConvertCommand(adminCommand, gm);
            });
            return false;
        }).orElse(true)) {
            logError("a2b requires BSkyBlock addon to be present! Disabling...");
            this.setState(State.DISABLED);
            return;
        }

    }

    private void importASkyBlockconfig() {
        // Get the ASkyBlock config
        File ASBconfig = new File(getPlugin().getDataFolder(), "../ASkyBlock/config.yml");
        if (!ASBconfig.exists()) {
            logError("Cannot find ASkyBlock config.yml file! It should be at " + ASBconfig.getAbsolutePath());
            this.setState(State.DISABLED);
            return;
        }
        // Make a destination folder
        this.getDataFolder().mkdirs();
        // Make new config
        File newConfig = new File(getDataFolder(), "config.yml");
        // Copy to this folder
        try {
            Files.copy(ASBconfig, newConfig);
        } catch (IOException e1) {
            logError("Cannot copy ASkyBlock config.yml file to a2b data folder! " + e1.getMessage());
            this.setState(State.DISABLED);
            return;
        }
    }

    @Override
    public void onDisable() {
        // Nothing to do here
    }

    @Override
    public void onReload() {
    }

}
