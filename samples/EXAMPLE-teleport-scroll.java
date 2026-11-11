import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.ItemAPI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;
import java.util.Random;

public class TeleportScroll extends AbstractCustomItem {

    private final Random random = new Random();
    private final java.util.Map<java.util.UUID, Integer> charges = new java.util.HashMap<>();

    @Override
    public String getId() { return "teleport_scroll"; }

    @Override
    public String getDisplayName() { return "&d📜 &fScroll of Teleportation"; }

    @Override
    public Material getMaterial() { return Material.PAPER; }

    @Override
    public List<String> getLore() {
        int currentCharges = charges.getOrDefault(
            org.bukkit.Bukkit.getPlayerExact("") != null ? 
            org.bukkit.Bukkit.getPlayerExact("").getUniqueId() : null, 3
        );
        return List.of(
            "",
            " &7Teleports you to a random player",
            " &7Right Click - teleport",
            "",
            " &d✦ &fUses: &e3/3",
            "",
            " &7&oThe item will disappear after"
            + " the charges run out"
        );
    }

    @Override
    public String getItemModel() { return "teleport_scroll"; }

    @Override
    public boolean hasCooldown() { return true; }

    @Override
    public int getCooldownSeconds() { return 10; }

    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        // Get the current charges
        int currentCharges = charges.getOrDefault(player.getUniqueId(), 3);

        if (currentCharges <= 0) {
            ItemAPI.message(player, "&c❌ &7The scroll has run out!");
            ItemAPI.sound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            player.getInventory().getItemInMainHand().setAmount(0);
            return;
        }

        // Find a random player
        List<Player> players = new java.util.ArrayList<>(org.bukkit.Bukkit.getOnlinePlayers());
        players.remove(player);

        if (players.isEmpty()) {
            ItemAPI.message(player, "&c❌ &7There are no players to teleport to!");
            return;
        }

        Player target = players.get(random.nextInt(players.size()));

        // Effect before the teleport
        ItemAPI.particles(player, Particle.PORTAL, 100);
        ItemAPI.sound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        ItemAPI.title(player, "&d📜 &fTELEPORTATION", "&7To " + target.getName() + "...");

        // Teleportation
        ItemAPI.teleportToPlayer(player, target, 2);

        // Effect after the teleport
        ItemAPI.particles(player, Particle.PORTAL, 100);
        ItemAPI.sound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        ItemAPI.title(player, "&a✅ &fARRIVED!", "&7You teleported to " + target.getName());

        // Decrease the charges
        currentCharges--;
        charges.put(player.getUniqueId(), currentCharges);

        if (currentCharges <= 0) {
            ItemAPI.message(player, "&d📜 &7The scroll has been fully used!");
            player.getInventory().getItemInMainHand().setAmount(0);
        } else {
            ItemAPI.message(player, "&d📜 &7Charges left: &e" + currentCharges);
        }
    }
}
