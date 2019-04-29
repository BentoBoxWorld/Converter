package world.bentobox.a2b;

import java.io.File;
import java.io.IOException;

import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.World.Environment;
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
    private static final String THE_END = "_the_end";

    // Settings
    private ChunkGeneratorWorld chunkGenerator;
    private @Nullable Settings settings;
    private World islandWorld2;
    private Object netherWorld2;
    private Object endWorld2;


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
        // ASkyBlock
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

        // BSkyBlock
        String worldName2 = settings.getWorldName();
        if (getServer().getWorld(worldName2) == null) {
            log("Creating BSkyBlock world ...");
        }
        chunkGenerator = settings.isUseOwnGenerator() ? null : new ChunkGeneratorWorld();
        // Create the world if it does not exist
        islandWorld2 = getWorld(worldName, World.Environment.NORMAL, chunkGenerator);

        // Make the nether if it does not exist
        if (settings.isNetherGenerate()) {
            if (getServer().getWorld(worldName + NETHER) == null) {
                log("Creating BSkyBlock's Nether...");
            }
            netherWorld2 = settings.isNetherIslands() ? getWorld(worldName, World.Environment.NETHER, chunkGenerator) : getWorld(worldName, World.Environment.NETHER, null);
        }
        // Make the end if it does not exist
        if (settings.isEndGenerate()) {
            if (getServer().getWorld(worldName + THE_END) == null) {
                log("Creating BSkyBlock's End World...");
            }
            endWorld2 = settings.isEndIslands() ? getWorld(worldName, World.Environment.THE_END, chunkGenerator) : getWorld(worldName, World.Environment.THE_END, null);
        }
    }

    /**
     * Gets a world or generates a new world if it does not exist
     * @param worldName - the overworld name
     * @param env - the environment
     * @param chunkGenerator2 - the chunk generator. If <tt>null</tt> then the generator will not be specified
     * @return world loaded or generated
     */
    private World getWorld(String worldName, Environment env, ChunkGeneratorWorld chunkGenerator2) {
        // Set world name
        worldName = env.equals(World.Environment.NETHER) ? worldName + NETHER : worldName;
        worldName = env.equals(World.Environment.THE_END) ? worldName + THE_END : worldName;
        WorldCreator wc = WorldCreator.name(worldName).type(WorldType.FLAT).environment(env);
        return settings.isUseOwnGenerator() ? wc.createWorld() : wc.generator(chunkGenerator2).createWorld();
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

    /**
     * @return the islandWorld2
     */
    public World getIslandWorld2() {
        return islandWorld2;
    }

    /**
     * @return the netherWorld2
     */
    public Object getNetherWorld2() {
        return netherWorld2;
    }

    /**
     * @return the endWorld2
     */
    public Object getEndWorld2() {
        return endWorld2;
    }

}
