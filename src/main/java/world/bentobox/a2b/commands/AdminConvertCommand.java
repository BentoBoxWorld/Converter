package world.bentobox.a2b.commands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import world.bentobox.a2b.converter.ChallengesConverter;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.database.objects.Names;
import world.bentobox.bentobox.database.objects.Players;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bskyblock.BSkyBlock;
import world.bentobox.bskyblock.Settings;
import world.bentobox.warps.Warp;

public class AdminConvertCommand extends CompositeCommand {

    private final static List<String> ENTITIES = Arrays.asList(EntityType.values()).stream().map(EntityType::name).collect(Collectors.toList());

    private Database<Players> handler;
    private Database<Names> names;

    // Owner, island
    Map<UUID, Island> islands = new HashMap<>();
    Map<UUID, List<UUID>> teamMembers = new HashMap<>();

    private YamlConfiguration config;

    private BSkyBlock gm;

    private Map<Flag, Integer> defaultIslandProtectionFlags;

    /**
     * Init ChallengeConverter class.
     */
    private final ChallengesConverter challengesConverter = new ChallengesConverter();


    public AdminConvertCommand(CompositeCommand parent, BSkyBlock gm) {
        super(parent, "convert");
        this.gm = gm;
    }

    @Override
    public void setup() {
        setPermission("admin.convert");
        setOnlyPlayer(false);
        setParametersHelp("commands.admin.convert.parameters");
        setDescription("commands.admin.convert.description");
        // Set up the database handler to store and retrieve Players classes
        handler = new Database<>(getPlugin(), Players.class);
        // Set up the names database
        names = new Database<>(getPlugin(), Names.class);
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (!args.isEmpty()) {
            // Show help
            showHelp(this, user);
            return false;
        }
        // Load ASkyBlock world
        File ASBconfig = new File(getPlugin().getDataFolder(), "../ASkyBlock/config.yml");
        if (!ASBconfig.exists()) {
            user.sendRawMessage("Cannot find ASkyBlock config.yml file!");
            return false;
        }
        config = new YamlConfiguration();
        try {
            config.load(ASBconfig);
        } catch (IOException | InvalidConfigurationException e) {
            user.sendRawMessage("Cannot load ASkyBlock config.yml file! " + e.getMessage());
            return false;
        }

        return true;
    }


    @Override
    public boolean execute(User user, String label, List<String> args) {
        user.sendRawMessage("Getting the world settings");
        try {
            getConfigs(user);
        } catch (Exception e) {
            getAddon().logError("Config conversion error: " + e.getMessage());
            return false;
        }

        if (this.challengesConverter.canConvert())
        {
            try
            {
                user.sendRawMessage("Converting challenges");
                this.challengesConverter.convertChallengesAndLevels(user, this.gm.getOverWorld());
            }
            catch (Exception e)
            {
                user.sendRawMessage("Could not convert Challenges. Error: " + e.getMessage());
                getAddon().logError("Challenge conversion error: " + e.getMessage());
                // Challenge conversion failing should not affect other conversions.
            }
        }

        user.sendRawMessage("Converting players and islands");
        try {
            convertplayers(user);
        } catch (Exception e) {
            getAddon().logError("Player conversion error: " + e.getMessage());
            return false;
        }
        // Warps
        user.sendRawMessage("Converting warps");
        try {
            convertWarps(user);
        } catch (Exception e) {
            getAddon().logError("Warps conversion error: " + e.getMessage());
            return false;
        }
        user.sendRawMessage("Complete! Now, stop the server and restart to use BSkyBlock! It is safe to remove a2b converter addon.");
        return true;
    }

    private void getConfigs(User user) throws InvalidConfigurationException {
        // Get config items for world
        // World or general settings
        Settings s = gm.getSettings();
        s.setWorldName(config.getString("general.worldName"));
        s.setMaxIslands(config.getInt("general.maxIslands"));
        s.setNetherGenerate(config.getBoolean("general.createnether", true));
        s.setNetherIslands(config.getBoolean("general.newnether", true));
        s.setNetherRoof(config.getBoolean("general.netherroof", true));
        s.setNetherSpawnRadius(config.getInt("general.netherspawnradius", 25));
        s.setVisitorBannedCommands(config.getStringList("general.visitorbannedcommands"));
        s.setOnJoinResetInventory(config.getBoolean("general.resetinventory"));
        s.setKickedKeepInventory(config.getBoolean("general.kickedkeepinv", true));
        s.setRemoveMobsWhitelist(cleanEntities(config.getStringList("general.mobwhitelist")));
        s.setLeaversLoseReset(config.getBoolean("general.leaversloseresets"));
        s.setOnJoinResetEnderChest(config.getBoolean("general.resetenderchest"));
        s.setMaxHomes(config.getInt("general.maxhomes",1));
        s.setDefaultBiome(cleanBiome(config.getString("biomesettings.defaultbiome", "PLAINS")));

        // Island settings
        s.setIslandXOffset(config.getInt("island.xoffset",0));
        s.setIslandZOffset(config.getInt("island.zoffset",0));
        s.setIslandDistance(config.getInt("island.distance") / 2);
        s.setIslandProtectionRange(config.getInt("island.protectionRange") / 2);
        s.setIslandStartX(config.getInt("island.startx",0));
        s.setIslandStartZ(config.getInt("island.startz",0));
        s.setSeaHeight(config.getInt("island.sealevel") != 0 ? config.getInt("island.sealevel") - 1 : 0);
        // ASkyBlock has its bedrock block center 5 below the island height
        int height = Math.max(0, config.getInt("island.islandlevel") - 5);
        s.setIslandHeight(height);

        Flags.PREVENT_TELEPORT_WHEN_FALLING.setSetting(getWorld(), config.getBoolean("general.allowfallingteleport", true));
        Flags.GEO_LIMIT_MOBS.setSetting(getWorld(), config.getBoolean("general.restrictwither", true));
        Flags.OBSIDIAN_SCOOPING.setSetting(getWorld(), config.getBoolean("general.allowobsidianscooping", true));
        Flags.OFFLINE_REDSTONE.setSetting(getWorld(), config.getBoolean("general.disableofflineredstone"));
        Flags.PISTON_PUSH.setSetting(getWorld(), config.getBoolean("general.allowTNTpushing", true));
        Flags.ANVIL.setSetting(getWorld(), config.getBoolean("protection.world.ANVIL"));
        Flags.ARMOR_STAND.setSetting(getWorld(), config.getBoolean("protection.world.ARMOR_STAND"));
        Flags.BEACON.setSetting(getWorld(), config.getBoolean("protection.world.BEACON"));
        Flags.BED.setSetting(getWorld(), config.getBoolean("protection.world.BED"));
        Flags.BREAK_BLOCKS.setSetting(getWorld(), config.getBoolean("protection.world.BREAK_BLOCKS"));
        Flags.BREEDING.setSetting(getWorld(), config.getBoolean("protection.world.BREEDING"));
        Flags.BREWING.setSetting(getWorld(), config.getBoolean("protection.world.BREWING"));
        Flags.BUCKET.setSetting(getWorld(), config.getBoolean("protection.world.BUCKET"));
        Flags.COLLECT_LAVA.setSetting(getWorld(), config.getBoolean("protection.world.COLLECT_LAVA"));
        Flags.COLLECT_WATER.setSetting(getWorld(), config.getBoolean("protection.world.COLLECT_WATER"));
        Flags.CONTAINER.setSetting(getWorld(), config.getBoolean("protection.world.CHEST"));
        Flags.CHORUS_FRUIT.setSetting(getWorld(), config.getBoolean("protection.world.CHORUS_FRUIT"));
        Flags.CRAFTING.setSetting(getWorld(), config.getBoolean("protection.world.CRAFTING"));
        Flags.CREEPER_DAMAGE.setSetting(getWorld(), config.getBoolean("protection.world.CREEPER_PAIN"));
        Flags.CROP_TRAMPLE.setSetting(getWorld(), config.getBoolean("protection.world.CROP_TRAMPLE"));
        Flags.DOOR.setSetting(getWorld(), config.getBoolean("protection.world.DOOR"));
        Flags.TRAPDOOR.setSetting(getWorld(), config.getBoolean("protection.world.DOOR"));
        Flags.EGGS.setSetting(getWorld(), config.getBoolean("protection.world.EGGS"));
        Flags.ENCHANTING.setSetting(getWorld(), config.getBoolean("protection.world.ENCHANTING"));
        Flags.ENDER_PEARL.setSetting(getWorld(), config.getBoolean("protection.world.ENDER_PEARL"));
        Flags.ENTER_EXIT_MESSAGES.setSetting(getWorld(), config.getBoolean("protection.world.ENTER_EXIT_MESSAGES"));
        Flags.FIRE_IGNITE.setSetting(getWorld(), config.getBoolean("protection.world.FIRE"));
        Flags.FIRE_SPREAD.setSetting(getWorld(), config.getBoolean("protection.world.FIRE_SPREAD"));
        Flags.FIRE_EXTINGUISH.setSetting(getWorld(), config.getBoolean("protection.world.FIRE_EXTINGUISH"));
        Flags.FIRE_BURNING.setSetting(getWorld(), config.getBoolean("protection.world.FIRE"));
        Flags.FURNACE.setSetting(getWorld(), config.getBoolean("protection.world.FURNACE"));
        Flags.GATE.setSetting(getWorld(), config.getBoolean("protection.world.GATE"));
        Flags.MOUNT_INVENTORY.setSetting(getWorld(), config.getBoolean("protection.world.HORSE_INVENTORY"));
        Flags.RIDING.setSetting(getWorld(), config.getBoolean("protection.world.HORSE_RIDING"));
        Flags.HURT_ANIMALS.setSetting(getWorld(), config.getBoolean("protection.world.HURT_MOBS"));
        Flags.HURT_VILLAGERS.setSetting(getWorld(), config.getBoolean("protection.world.HURT_MOBS"));
        Flags.HURT_MONSTERS.setSetting(getWorld(), config.getBoolean("protection.world.HURT_MONSTERS"));
        Flags.LEASH.setSetting(getWorld(), config.getBoolean("protection.world.LEASH"));
        Flags.LEVER.setSetting(getWorld(), config.getBoolean("protection.world.LEAVER_BUTTON"));
        Flags.BUTTON.setSetting(getWorld(), config.getBoolean("protection.world.LEAVER_BUTTON"));
        Flags.MILKING.setSetting(getWorld(), config.getBoolean("protection.world.MILKING"));
        Flags.ANIMAL_NATURAL_SPAWN.setSetting(getWorld(), config.getBoolean("protection.world.MOB_SPAWN"));
        Flags.MONSTER_NATURAL_SPAWN.setSetting(getWorld(), config.getBoolean("protection.world.MONSTER_SPAWN"));
        Flags.JUKEBOX.setSetting(getWorld(), config.getBoolean("protection.world.MUSIC"));
        Flags.NOTE_BLOCK.setSetting(getWorld(), config.getBoolean("protection.world.MUSIC"));
        Flags.PLACE_BLOCKS.setSetting(getWorld(), config.getBoolean("protection.world.PLACE_BLOCKS"));
        Flags.END_PORTAL.setSetting(getWorld(), config.getBoolean("protection.world.PORTAL"));
        Flags.NETHER_PORTAL.setSetting(getWorld(), config.getBoolean("protection.world.PORTAL"));
        Flags.PRESSURE_PLATE.setSetting(getWorld(), config.getBoolean("protection.world.PRESSURE_PLATE"));
        Flags.REDSTONE.setSetting(getWorld(), config.getBoolean("protection.world.REDSTONE"));
        Flags.SPAWN_EGGS.setSetting(getWorld(), config.getBoolean("protection.world.SPAWN_EGGS"));
        Flags.SHEARING.setSetting(getWorld(), config.getBoolean("protection.world.SHEARING"));
        Flags.TRADING.setSetting(getWorld(), config.getBoolean("protection.world.VILLAGER_TRADING"));
        Flags.ITEM_DROP.setSetting(getWorld(), config.getBoolean("protection.world.VISITOR_ITEM_DROP"));
        Flags.ITEM_PICKUP.setSetting(getWorld(), config.getBoolean("protection.world.VISITOR_ITEM_PICKUP"));
        // Default island protection flags
        defaultIslandProtectionFlags = new HashMap<>();
        ConfigurationSection islandProtection = config.getConfigurationSection("protection.island");
        if (islandProtection != null) {
            for (String flag : islandProtection.getKeys(false)) {
                getPlugin().getFlagsManager().getFlag(flag).ifPresent(f ->
                defaultIslandProtectionFlags.put(f, islandProtection.getBoolean(flag) ? RanksManager.MEMBER_RANK : RanksManager.VISITOR_RANK));
            }
        }
        // Hidden flags are all those not listed
        List<String> hiddenFlags = Flags.values().stream().filter(key -> !defaultIslandProtectionFlags.containsKey(key)).map(f -> f.getID()).collect(Collectors.toList());
        s.setHiddenFlags(hiddenFlags);

        user.sendRawMessage("Saving BSkyBlock config");
        gm.saveWorldSettings();

    }

    private Biome cleanBiome(String string) {
        return Arrays.asList(Biome.values()).stream().filter(b -> b.name().equals(string.toUpperCase())).findFirst().orElse(Biome.PLAINS);
    }

    private Set<EntityType> cleanEntities(List<String> stringList) {
        return stringList.stream().filter(ENTITIES::contains).map(EntityType::valueOf).collect(Collectors.toSet());
    }

    private void convertplayers(User user) throws InvalidConfigurationException, FileNotFoundException, IOException {
        File playerFolder = new File(getPlugin().getDataFolder(), "../ASkyBlock/players");
        if (!playerFolder.exists()) {
            throw new InvalidConfigurationException("Expected ASkyBlock player folder not found!");
        }
        FilenameFilter ymlFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                String lowercaseName = name.toLowerCase();
                if (lowercaseName.endsWith(".yml")) {
                    return true;
                } else {
                    return false;
                }
            }
        };
        PlayersManager pm = getPlugin().getPlayers();
        YamlConfiguration player = new YamlConfiguration();
        List<File> playerFiles = Arrays.asList(playerFolder.listFiles(ymlFilter));
        user.sendRawMessage("There are " + playerFiles.size() + " player files to process.");
        int count = 1;
        for (File file: playerFiles) {
            try {
                player.load(file);
                String uniqueId = file.getName().substring(0, file.getName().length() - 4);
                UUID uuid = UUID.fromString(uniqueId);
                Players p = new Players();
                if (pm.isKnown(uuid)) {
                    p = pm.getPlayer(uuid);
                } else {
                    // New player
                    p.setUniqueId(uniqueId);
                    p.setPlayerName(player.getString("playerName"));
                    names.saveObjectAsync(new Names(player.getString("playerName"), uuid));
                }
                p.getDeaths().putIfAbsent(config.getString("general.worldName"), player.getInt("deaths"));
                p.setLocale(player.getString("locale"));
                handler.saveObjectAsync(p);
                count++;
                if (count % 10 == 0) {
                    user.sendRawMessage("Saved " + (count++) + " players");
                }
                // Handle island
                processIsland(user, uuid, player);

                // Handle challenges
                try
                {
                    if (this.challengesConverter.canConvert())
                    {
                        this.challengesConverter.convertUserChallenges(User.getInstance(uuid), player, this.gm);
                        user.sendRawMessage("User challenges converted");
                    }
                }
                catch (Exception e)
                {
                    user.sendRawMessage("Failed on converting user challenges.");
                    // Do not mark as failed.
                }

                // Rename file
                file.renameTo(new File(file.getParent(), file.getName() + ".done"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        user.sendRawMessage("Processing teams");
        processTeams();
        user.sendRawMessage("Storing islands");
        islands.values().forEach(i -> getIslands().getIslandCache().addIsland(i));
    }

    private void processTeams() {
        teamMembers.forEach((k,v) -> {
            islands.get(k).setOwner(k);
            v.forEach(member -> islands.get(k).addMember(member));
        });
    }

    private void processIsland(User user, UUID uuid, YamlConfiguration player) {
        boolean hasIsland = player.getBoolean("hasIsland");
        if (!hasIsland) {
            // Unless the player has an island, ignore
            return;
        }
        String islandInfo = player.getString("islandInfo","");
        int protectionRange = 100;
        boolean isLocked = false;
        boolean isSpawn = false;
        if (!islandInfo.isEmpty()) {
            String[] split = islandInfo.split(":");
            try {
                protectionRange = Integer.parseInt(split[3]);
                isLocked = split[6].equalsIgnoreCase("true");
                isSpawn = split[5].equals("spawn");
            } catch (Exception e) {
                getAddon().logError("Problem parsing island settings");
            }
        }
        // Deal with owners or team leaders
        Island island = new Island();
        island.setUniqueId(getWorld().getName() + "_i_" + UUID.randomUUID().toString());
        island.setCenter(Util.getLocationString(player.getString("islandLocation")));
        island.setRange(config.getInt("island.distance") / 2);
        island.setOwner(player.getString("teamLeader").isEmpty() ? uuid : UUID.fromString(player.getString("teamLeader")));
        island.setProtectionRange(protectionRange / 2);
        island.setSpawn(isSpawn);
        island.setSettingsFlag(Flags.LOCK, isLocked);
        player.getStringList("members").stream().map(UUID::fromString).forEach(island::addMember);
        island.setRank(island.getOwner(), RanksManager.OWNER_RANK);
        island.setCreatedDate(System.currentTimeMillis());
        island.setUpdatedDate(System.currentTimeMillis());
        island.setGameMode(getAddon().getDescription().getName());
        island.setFlags(defaultIslandProtectionFlags);
        islands.put(uuid, island);
    }

    private void convertWarps(User user) throws FileNotFoundException, IOException, InvalidConfigurationException {
        // Warps
        Warp warpAddon = getAddon().getAddonByName("Warps").map(Warp.class::cast).orElse(null);
        if (warpAddon == null){
            user.sendRawMessage("Warp addon not found.");
            return;
        }
        YamlConfiguration warps = new YamlConfiguration();
        File warpFile = new File(getPlugin().getDataFolder(), "../ASkyBlock/warps.yml");
        if (!warpFile.exists()) {
            user.sendRawMessage("No warps found.");
            return;
        }
        warps.load(warpFile);
        if (warps.isConfigurationSection("warps")) {
            ConfigurationSection w = warps.getConfigurationSection("warps");
            for (String uuid: w.getKeys(false)) {
                UUID playerUUID = UUID.fromString(uuid);
                Location loc = Util.getLocationString(w.getString(uuid, ""));
                if (loc != null) {
                    warpAddon.getWarpSignsManager().addWarp(playerUUID, loc);
                }
            }
        } else {
            user.sendRawMessage("No warps.");
        }
    }
}
