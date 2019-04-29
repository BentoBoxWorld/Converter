package world.bentobox.a2b;

import java.io.File;
import java.io.IOException;

import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.generator.ChunkGenerator;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.io.Files;

import world.bentobox.a2b.commands.AdminCommand;
import world.bentobox.a2b.world.ChunkGeneratorWorld;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.Config;
import world.bentobox.bentobox.api.configuration.WorldSettings;

/**
 * Convert ASkyBlock to BSkyBlock
 * @author tastybento
 */
public class A2B extends GameModeAddon {

    private static final String NETHER = "_nether";

    // Settings
    private ChunkGeneratorWorld chunkGenerator;
    private @Nullable Settings settings;


    @Override
    public void onLoad() {
        // Save the default config from config-converted.yml
        settings = new Settings();
        this.saveWorldSettings();
        // Settings
        importASkyBlockconfig();
    }

    @Override
    public void onEnable(){
        // Register commands
        adminCommand = new AdminCommand(this);
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

    @Override
    public void createWorlds() {
        String worldName = getConfig().getString("general.worldName", "ASkyBlock");
        boolean createNether = getConfig().getBoolean("general.createnether");
        boolean newNether = getConfig().getBoolean("general.newnether");
        // Open up the world
        ChunkGeneratorWorld chunkGenerator = new ChunkGeneratorWorld();
        islandWorld = WorldCreator.name(worldName).type(WorldType.FLAT).environment(World.Environment.NORMAL).generator(chunkGenerator).createWorld();

        // Get nether
        if (createNether && newNether) {
            netherWorld = WorldCreator.name(worldName + NETHER).type(WorldType.FLAT).environment(World.Environment.NORMAL).generator(chunkGenerator).createWorld();
        }

    }

    @Override
    public WorldSettings getWorldSettings() {
        return settings;
    }

    @Override
    public @NonNull ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return chunkGenerator;
    }

    @Override
    public void saveWorldSettings() {
        if (settings != null) {
            new Config<>(this, Settings.class).saveConfigObject(settings);
        }

    }

    public Settings getSettings() {
        return settings;
    }

}
