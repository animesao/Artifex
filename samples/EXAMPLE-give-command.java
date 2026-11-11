import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.commands.CustomCommand;
import me.dcplugin.artifex.models.CustomItem;
import me.dcplugin.artifex.utils.EnumCache;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /give command — giving items (custom + vanilla).
 *
 * Usage:
 *   /give <id>                  — give yourself
 *   /give <id> <player>         — give a player
 *   /give <id> <player> <amount>— give N pieces
 *   /give <id> all              — give to everyone online
 *   /give list                  — a list of the custom items
 *   /give materials             — search the vanilla materials
 *
 * Supports:
 *   - Custom items: /give vampire-blade
 *   - Vanilla materials: /give diamond, /give netherite_sword, /give cooked_beef 64
 */
public class GiveCommand extends CustomCommand {

    public GiveCommand() {
        super("give", "Give an item (custom or vanilla)",
              "/give <id|material> [player] [amount]", "artifex.give");
        setAliases("g");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Main plugin = Main.getInstance();
        if (plugin == null) return true;

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        // /give list
        if (args[0].equalsIgnoreCase("list")) {
            return handleList(sender);
        }

        // /give materials <filter>
        if (args[0].equalsIgnoreCase("materials")) {
            return handleMaterials(sender, args.length >= 2 ? args[1] : "");
        }

        String input = args[0];

        // 1. Look among the YAML items
        CustomItem customItem = plugin.getItemHandler().getCustomItem(input.toLowerCase());

        // 2. Look among the Java API items (AbstractCustomItem)
        AbstractCustomItem javaItem = null;
        if (customItem == null) {
            javaItem = plugin.getApiItemRegistry().getItem(input);
            if (javaItem == null) javaItem = plugin.getApiItemRegistry().getItem(input.toLowerCase());
        }

        // 3. If not found — look for it as a vanilla material
        Material vanillaMaterial = null;
        if (customItem == null && javaItem == null) {
            vanillaMaterial = EnumCache.getMaterial(input);
        }

        if (customItem == null && javaItem == null && vanillaMaterial == null) {
            msg(sender, "&cItem '&e" + input + "&c' not found!");
            msg(sender, "&7Try &e/give list &7or &e/give materials");
            return true;
        }

        // Determine the recipient
        Player target;
        int amount = 1;

        if (args.length >= 2) {
            if (args[1].equalsIgnoreCase("all")) {
                int amt = args.length >= 3 ? parseAmount(args[2], 1) : 1;
                return handleGiveAll(sender, customItem, javaItem, vanillaMaterial, input, amt);
            }
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                msg(sender, "&cPlayer '&e" + args[1] + "&c' not found!");
                return true;
            }
            if (args.length >= 3) {
                amount = parseAmount(args[2], 1);
            }
        } else {
            if (!(sender instanceof Player)) {
                msg(sender, "&cPlease specify a player: /give <id> <player>");
                return true;
            }
            target = (Player) sender;
        }

        // Limit the amount
        amount = Math.max(1, Math.min(amount, 64));

        // Create the ItemStack
        ItemStack itemStack;
        String displayName;

        if (customItem != null) {
            itemStack = customItem.getItemStack().clone();
            displayName = customItem.getId();
        } else if (javaItem != null) {
            itemStack = javaItem.createItemStack();
            displayName = javaItem.getId();
        } else {
            itemStack = new ItemStack(vanillaMaterial, amount);
            displayName = vanillaMaterial.name().toLowerCase().replace("_", " ");
        }
        itemStack.setAmount(amount);

        // Give it
        target.getInventory().addItem(itemStack);

        // Message
        String targetName = target.equals(sender) ? "yourself" : target.getName();
        msg(sender, "&aGave &e" + displayName + " &ax" + amount + " &7→ &f" + targetName);

        if (!target.equals(sender)) {
            msg(target, "&aYou received &e" + displayName + " &ax" + amount);
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args, int position) {
        Main plugin = Main.getInstance();
        if (plugin == null) return new ArrayList<>();

        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();

            // Commands
            if ("list".startsWith(prefix)) completions.add("list");
            if ("materials".startsWith(prefix)) completions.add("materials");

            // YAML items
            for (String id : plugin.getItemHandler().getAllCustomItems().keySet()) {
                if (id.toLowerCase().startsWith(prefix)) {
                    completions.add(id);
                }
            }

            // Java API items
            for (String id : plugin.getApiItemRegistry().getAllIds()) {
                if (id.toLowerCase().startsWith(prefix) && !completions.contains(id)) {
                    completions.add(id);
                }
            }

            // Vanilla materials (popular ones)
            for (Material mat : POPULAR_MATERIALS) {
                String name = mat.name().toLowerCase();
                if (name.startsWith(prefix) || name.replace("_", "").startsWith(prefix.replace("_", ""))) {
                    completions.add(name);
                }
            }

            return completions;
        }

        if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            List<String> completions = new ArrayList<>();

            if ("all".startsWith(prefix)) completions.add("all");
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(prefix)) {
                    completions.add(online.getName());
                }
            }

            return completions;
        }

        if (args.length == 3) {
            return Arrays.asList("1", "5", "16", "32", "64");
        }

        return new ArrayList<>();
    }

    // ===== Internal =====

    private boolean handleList(CommandSender sender) {
        Main plugin = Main.getInstance();
        var allItems = plugin.getItemHandler().getAllCustomItems();
        var allJavaItems = plugin.getApiItemRegistry().getAllItems();

        if (allItems.isEmpty() && allJavaItems.isEmpty()) {
            msg(sender, "&7No custom items loaded.");
            msg(sender, "&7Use &e/give <material> &7for vanilla items");
            return true;
        }

        msg(sender, "&6=== Custom items (" + (allItems.size() + allJavaItems.size()) + ") ===");

        for (var entry : allItems.entrySet()) {
            CustomItem item = entry.getValue();
            String type = item.getType() != null ? item.getType() : "UNKNOWN";
            msg(sender, " &e" + entry.getKey() + " &7(" + type + ")");
        }

        // Java API items
        for (var entry : allJavaItems.entrySet()) {
            AbstractCustomItem item = entry.getValue();
            String type = item.getType() != null ? item.getType() : "UNKNOWN";
            msg(sender, " &e" + entry.getKey() + " &7(Java " + type + ")");
        }

        return true;
    }

    private boolean handleMaterials(CommandSender sender, String filter) {
        String filterLower = filter.toLowerCase();
        List<Material> found = new ArrayList<>();

        for (Material mat : Material.values()) {
            if (mat.isAir() || !mat.isItem()) continue;
            if (!filterLower.isEmpty() && !mat.name().toLowerCase().contains(filterLower)) continue;
            found.add(mat);
        }

        if (found.isEmpty()) {
            msg(sender, "&cNo materials matching '&e" + filter + "&c' were found.");
            return true;
        }

        // Limit the output
        int limit = Math.min(found.size(), 50);
        msg(sender, "&6=== Materials" + (filter.isEmpty() ? "" : " ('" + filter + "')") +
            " [" + found.size() + "]" + (found.size() > limit ? " (first " + limit + ")" : "") + " ===");

        StringBuilder line = new StringBuilder(" &e");
        for (int i = 0; i < limit; i++) {
            if (i > 0) line.append("&7, &e");
            line.append(found.get(i).name().toLowerCase());
            // Line break every 5 items
            if ((i + 1) % 5 == 0 && i + 1 < limit) {
                msg(sender, line.toString());
                line = new StringBuilder(" &e");
            }
        }
        if (line.length() > 3) {
            msg(sender, line.toString());
        }

        return true;
    }

    private boolean handleGiveAll(CommandSender sender, CustomItem customItem, AbstractCustomItem javaItem,
                                  Material vanillaMat, String name, int amount) {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        int count = 0;

        for (Player target : online) {
            ItemStack itemStack;
            if (customItem != null) {
                itemStack = customItem.getItemStack().clone();
            } else if (javaItem != null) {
                itemStack = javaItem.createItemStack();
            } else {
                itemStack = new ItemStack(vanillaMat, amount);
            }
            itemStack.setAmount(amount);
            target.getInventory().addItem(itemStack);
            String displayName = displayNameOf(customItem, javaItem, vanillaMat);
            msg(target, "&aYou received &e" + displayName + " &ax" + amount);
            count++;
        }

        String displayName = displayNameOf(customItem, javaItem, vanillaMat);
        msg(sender, "&aGave &e" + displayName + " &ax" + amount + " &7→ &fto everyone (" + count + " players)");

        return true;
    }

    private String displayNameOf(CustomItem customItem, AbstractCustomItem javaItem, Material vanillaMat) {
        if (customItem != null) return customItem.getId();
        if (javaItem != null) return javaItem.getId();
        return vanillaMat.name().toLowerCase().replace("_", " ");
    }

    private int parseAmount(String input, int defaultValue) {
        try {
            return Math.max(1, Math.min(64, Integer.parseInt(input)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void sendHelp(CommandSender sender) {
        msg(sender, "&6=== /give — Giving items ===");
        msg(sender, "&e/give <id|material> &7— give yourself");
        msg(sender, "&7  Java items too: &e/give dragonbone_armor");
        msg(sender, "&e/give <id|material> <player> &7— give a player");
        msg(sender, "&e/give <id|material> <player> <amount> &7— give N pieces");
        msg(sender, "&e/give <id|material> all &7— give to everyone online");
        msg(sender, "&7");
        msg(sender, "&e/give list &7— a list of the custom items");
        msg(sender, "&e/give materials &7— all vanilla materials");
        msg(sender, "&e/give materials diamond &7— search by filter");
        msg(sender, "&7");
        msg(sender, "&7Examples:");
        msg(sender, "&e/give vampire-blade &7— a custom item");
        msg(sender, "&e/give diamond &7— a diamond");
        msg(sender, "&e/give netherite_sword Steve 1 &7— a netherite sword");
        msg(sender, "&e/give cooked_beef all 16 &7— a steak for everyone");
    }

    // Popular vanilla materials for tab-completion
    private static final Material[] POPULAR_MATERIALS = {
        // Diamonds
        Material.DIAMOND, Material.DIAMOND_SWORD, Material.DIAMOND_PICKAXE,
        Material.DIAMOND_AXE, Material.DIAMOND_SHOVEL, Material.DIAMOND_HOE,
        Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
        // Netherite
        Material.NETHERITE_INGOT, Material.NETHERITE_SWORD, Material.NETHERITE_PICKAXE,
        Material.NETHERITE_AXE, Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE,
        Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
        // Iron
        Material.IRON_INGOT, Material.IRON_SWORD, Material.IRON_PICKAXE,
        Material.IRON_AXE, Material.IRON_HELMET, Material.IRON_CHESTPLATE,
        Material.IRON_LEGGINGS, Material.IRON_BOOTS,
        // Gold
        Material.GOLD_INGOT, Material.GOLDEN_APPLE, Material.GOLDEN_CARROT,
        // Food
        Material.COOKED_BEEF, Material.COOKED_PORKCHOP, Material.COOKED_MUTTON,
        Material.COOKED_CHICKEN, Material.COOKED_RABBIT, Material.COOKED_COD,
        Material.BREAD, Material.PUMPKIN_PIE, Material.COOKIE,
        // Potions
        Material.POTION, Material.SPLASH_POTION, Material.LINGERING_POTION,
        // Resources
        Material.ENDER_PEARL, Material.ENDER_EYE, Material.ELYTRA,
        Material.TOTEM_OF_UNDYING, Material.SHULKER_SHELL, Material.NETHER_STAR,
        Material.DRAGON_EGG, Material.BEACON, Material.CONDUIT,
        // Building blocks
        Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.BEDROCK,
        Material.END_STONE, Material.BRICKS, Material.DEEPSLATE_BRICKS,
        // Misc
        Material.BOW, Material.CROSSBOW, Material.SHIELD, Material.TRIDENT,
        Material.FLINT_AND_STEEL, Material.COMPASS, Material.CLOCK,
        Material.EXPERIENCE_BOTTLE, Material.EMERALD, Material.EMERALD_BLOCK,
    };
}
