package world.bentobox.a2b.world;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Location;

import world.bentobox.a2b.A2B;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.Clipboard;
import world.bentobox.bentobox.blueprints.Paster;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.island.NewIsland;

public class IslandCopy {

    private A2B addon;
    private Clipboard clipboard;

    /**
     * Copy one island from one world to another
     * @param addon
     */
    public IslandCopy(A2B addon) {
        this.addon = addon;
        makeIslandEntries();
    }

    private void makeIslandEntries() {
        List<Island> islandList = addon.getIslands().getIslands().stream().filter(i -> i.getGameMode().equals(addon.getDescription().getName())).collect(Collectors.toList());
        addon.log("Making island entries in BSkyBlock world. " + islandList.size() + " to go...");
        islandList.forEach(i -> {
            try {
                User user = User.getInstance(i.getOwner());
                Island newIsland = NewIsland.builder()
                        .player(user)
                        .world(addon.getIslandWorld2())
                        .reason(Reason.CREATE)
                        .name(i.getName())
                        .noPaste()
                        .build();
                newIsland.setMembers(i.getMembers());
                newIsland.setGameMode("BSkyBlock");
                addon.log("added");
            } catch (IOException e) {
                addon.logError("Could not create island object. " + e.getMessage());
            }
        });
        addon.log("Done");
    }


    /**
     * Copy an island from one location to another
     * @param island - island to copy
     * @param to - location for new island
     */
    public void copyAndPaste(Island island, Location to) {
        copyIslandToClipboard(island);
        new Paster(addon.getPlugin(), clipboard, to);
    }

    private void copyIslandToClipboard(Island island) {
        clipboard = new Clipboard();
        clipboard.setOrigin(island.getCenter());
        clipboard.setPos1(new Location(island.getWorld(), island.getMinProtectedX(), 0, island.getMinProtectedZ()));
        clipboard.setPos2(new Location(island.getWorld(), island.getMinProtectedX() + (island.getProtectionRange() * 2), island.getWorld().getMaxHeight(), island.getMinProtectedZ() + (island.getProtectionRange() * 2)));
        try {
            clipboard.copy(false);
        } catch (IOException e) {
            addon.logError(e.getMessage());
        }
    }

}
