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

import org.bukkit.block.Biome;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import world.bentobox.bentobox.api.commands.CompositeCommand;
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

public class AdminConvertCommand extends CompositeCommand {

    private final static List<String> ENTITIES = Arrays.asList(EntityType.values()).stream().map(EntityType::name).collect(Collectors.toList());

    private Database<Players> handler;
    private Database<Names> names;

    // Owner, island
    Map<UUID, Island> islands = new HashMap<>();
    Map<UUID, List<UUID>> teamMembers = new HashMap<>();

    private YamlConfiguration config;
    private int islandDistance;
    private int islandProtectionRange;
    private int islandXOffset;
    private int islandZOffset;
    private int islandStartX;
    private int islandStartZ;
    private int islandHeight;
    private int seaHeight;
    private BSkyBlock gm;

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
            e.printStackTrace();
            return false;
        }
        user.sendRawMessage("Converting players");
        try {
            convertplayers(user);
        } catch (Exception e) {
            getAddon().logError("Player conversion error: " + e.getMessage());
            return false;
        }
        return true;
    }

    private <T> T nullCheck(T object) throws InvalidConfigurationException {
        if (object == null) {
            throw new InvalidConfigurationException("Expected ASkyBlock config items not found!");
        }
        return object;
    }

    private void getConfigs(User user) throws InvalidConfigurationException {
        String worldName;
        // Get config items for world
        //try {
        // World settings
        worldName = config.getString("general.worldName");
        // Island settings
        islandDistance = nullCheck(config.getInt("island.distance"));
        islandProtectionRange = nullCheck(config.getInt("island.protectionRange"));
        islandXOffset = nullCheck(config.getInt("island.xoffset"));
        islandZOffset = nullCheck(config.getInt("island.zoffset"));
        islandStartX = nullCheck(config.getInt("island.startx"));
        islandStartZ = nullCheck(config.getInt("island.startz"));
        islandHeight = nullCheck(config.getInt("island.islandlevel")) - 5;
        seaHeight = nullCheck(config.getInt("island.sealevel"));
        //} catch (Exception e) {
        //    getAddon().logError(e.getMessage());
        //    return;
        //}

        // World or general settings
        Settings s = gm.getSettings();
        s.setWorldName(worldName);
        s.setMaxIslands(config.getInt("general.maxIslands"));
        s.setNetherGenerate(config.getBoolean("general.createnether", true));
        s.setNetherIslands(config.getBoolean("general.newnether", true));
        s.setNetherTrees(config.getBoolean("general.nethertrees", true));
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
        s.setIslandXOffset(islandXOffset);
        s.setIslandZOffset(islandZOffset);
        s.setIslandDistance(islandDistance / 2);
        s.setIslandProtectionRange(islandProtectionRange / 2);
        s.setIslandStartX(islandStartX);
        s.setIslandStartZ(islandStartZ);
        s.setSeaHeight(seaHeight != 0 ? seaHeight - 1 : 0);
        s.setIslandHeight(islandHeight);
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
        user.sendRawMessage("There are " + playerFiles.size() + " player files to process");
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
                    names.saveObject(new Names(player.getString("playerName"), uuid));
                }
                p.getDeaths().putIfAbsent(this.getWorld().getName(), player.getInt("deaths"));
                p.setLocale(player.getString("locale"));
                handler.saveObject(p);
                user.sendRawMessage("Saved " + (count++) + " players");
                // Handle island
                processIsland(user, uuid, player);
                // Rename file
                file.renameTo(new File(file.getParent(), file.getName() + ".done"));
            } catch (Exception e) {
                getAddon().logError("Error trying to import player file " + file.getName() + ": " + e.getMessage());
            }
        }
        user.sendRawMessage("Processing teams");
        processTeams();
        user.sendRawMessage("Storing islands");
        islands.values().forEach(i -> getIslands().getIslandCache().addIsland(i));
        user.sendRawMessage("done");
    }

    private void processTeams() {
        teamMembers.forEach((k,v) -> {
            islands.get(k).setOwner(k);
            v.forEach(member -> islands.get(k).addMember(member));
        });
    }

    private void processIsland(User user, UUID uuid, YamlConfiguration player) {
        user.sendRawMessage("Processing island");
        boolean hasIsland = player.getBoolean("hasIsland");
        if (!hasIsland) {
            // Unless the player has an island, ignore
            return;
        }
        String islandLocation = player.getString("islandLocation");
        String teamLeader = player.getString("teamLeader");
        List<String> members = player.getStringList("members");
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
        island.setOwner(teamLeader.isEmpty() ? uuid : UUID.fromString(teamLeader));
        island.setProtectionRange(protectionRange / 2);
        island.setSpawn(isSpawn);
        island.setCenter(Util.getLocationString(islandLocation));
        island.setSettingsFlag(Flags.LOCK, isLocked);
        members.stream().map(UUID::fromString).forEach(island::addMember);
        island.setRank(island.getOwner(), RanksManager.OWNER_RANK);
        island.setCreatedDate(System.currentTimeMillis());
        island.setUpdatedDate(System.currentTimeMillis());
        island.setGameMode(getAddon().getDescription().getName());
        island.setRange(this.islandDistance / 2 );
        islands.put(uuid, island);
    }
}
