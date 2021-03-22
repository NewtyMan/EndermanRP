package io.github.newtyman.endermanrp;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class EndermanRP extends JavaPlugin implements Listener {
    HashMap<UUID, Long> playerToggle = new HashMap<>();
    List<UUID> playerInvisible = new ArrayList<>();
    List<UUID> playerData = new ArrayList<>();

    boolean enablePumpkinInvisibility;
    boolean enableFriendlyEnderman;
    boolean enableTeleportation;
    boolean enableWaterDamage;
    boolean enableSilkTouch;

    boolean requireFullArmor;
    int heightDifference;
    int teleportDistance;
    int damageAmount;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(this, this);
        saveDefaultConfig();

        enablePumpkinInvisibility = getConfig().getBoolean("enable-pumpkin-invisibility");
        enableFriendlyEnderman = getConfig().getBoolean("enable-friendly-enderman");
        enableTeleportation = getConfig().getBoolean("enable-teleportation");
        enableWaterDamage = getConfig().getBoolean("enable-water-damage");
        enableSilkTouch = getConfig().getBoolean("enable-silk-touch");

        requireFullArmor = getConfig().getBoolean("require-full-armor");
        heightDifference = getConfig().getInt("max-height-difference");
        damageAmount = getConfig().getInt("water-per-second-damage");
        teleportDistance = getConfig().getInt("teleport-distance");

        ConfigurationSection section = getConfig().getConfigurationSection("data");
        if (section != null) section.getKeys(false).forEach(key -> playerData.add(UUID.fromString(key)));

        if (!enableWaterDamage) return;
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!playerData.contains(player.getUniqueId())) continue;
                // Standing in rain damage
                if (player.getWorld().hasStorm() && player.getWorld().getBlockAt(player.getLocation()).getLightFromSky() == 15) player.damage(damageAmount);

                // Standing in water damage
                if (!requireFullArmor && Arrays.stream(player.getInventory().getArmorContents()).anyMatch(Objects::nonNull)) continue;
                else if (requireFullArmor && Arrays.stream(player.getInventory().getArmorContents()).allMatch(Objects::nonNull)) continue;

                // We do it the longer way, since 1.13 doesn't support player.isInWater()
                if (player.getWorld().getBlockAt(player.getLocation()).getType().equals(Material.WATER)) player.damage(damageAmount);
            }
        }, 20L, 20L);
    }

    @Override
    public void onDisable() {
        // Wipes data section and writes current values
        getConfig().createSection("data");
        for (UUID entry : playerData) getConfig().set("data." + entry.toString(), true);
        saveConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;

        Player player = ((Player) sender).getPlayer();
        assert player != null;

        if (label.equalsIgnoreCase("s")) {
            if (!playerData.contains(player.getUniqueId())) return false;
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_AMBIENT, 1.0f, 1.0f);
            return true;
        }

        if (label.equalsIgnoreCase("em")) {
            if (!player.hasPermission("enderman.set")) return false;
            if (args.length != 2){
                sendMessage(player, "&4[ERROR] &fInvalid syntax: /em {on | off} {player_name}");
                return false;
            }

            if (!args[0].equals("on") && !args[0].equals("off")) {
                sendMessage(player, "&4[ERROR] &fInvalid first argument (" + args[0] + ")");
                return false;
            }

            Player target = Bukkit.getServer().getPlayer(args[1]);
            if (target == null) {
                sendMessage(player, "&4[ERROR] &fPlayer is not online");
                return false;
            }

            boolean data = args[0].equals("on");
            if (data) playerData.add(target.getUniqueId());
            else playerData.remove(target.getUniqueId());

            String color = data ? "&2" : "&4";
            sendMessage(player, "&2[ENDERMAN] &fEnderman Status -> " + target.getDisplayName() + ": " + color + args[0]);
            sendMessage(target, "&2[ENDERMAN] &fEnderman Status Toggled: " + color + args[0]);

            if (data) updateEndermanInvisible(target);
            else playerUnhideAll(target);
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("em") && args.length == 1)
            return new ArrayList<String>(){{ add("on"); add("off"); }};
        return null;
    }

    @EventHandler
    public void onBreaksBlockEvent(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!enableSilkTouch) return;
        if (!playerData.contains(player.getUniqueId())) return;
        if (player.getInventory().getItemInMainHand().getType() != Material.AIR) return;

        player.getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(event.getBlock().getType()));
        event.getBlock().setType(Material.AIR);
    }

    @EventHandler
    public void onSneakToggle(PlayerToggleSneakEvent event) {
        if (!enableTeleportation) return;
        Player player = event.getPlayer();
        if (!playerData.contains(player.getUniqueId())) return;

        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
            if (System.currentTimeMillis() - playerToggle.getOrDefault(player.getUniqueId(), System.currentTimeMillis()) < 2800) return;
            sendMessage(player,"&fYou are ready to &2teleport");
        }, 20L * 3);

        if (event.isSneaking()) playerToggle.put(player.getUniqueId(), System.currentTimeMillis());
        else playerToggle.remove(player.getUniqueId());
    }

    @EventHandler
    public void onLeftClick(PlayerInteractEvent event) {
        if (!enableTeleportation) return;
        if (!playerToggle.containsKey(event.getPlayer().getUniqueId())) return;
        if (event.getAction() != Action.LEFT_CLICK_AIR) return;
        Player player = event.getPlayer();


        if (System.currentTimeMillis() - playerToggle.get(player.getUniqueId()) < 3000D) return;

        Location teleport = player.getLocation();
        for (int i=teleportDistance; i>0; i--) {
            teleport = firstEmptyBlock(player.getLocation().add(player.getLocation().clone().getDirection().normalize().multiply(i)));
            if (Math.abs(teleport.getY() - player.getLocation().getY()) < 6) break;
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        player.teleport(teleport);
        player.getWorld().playSound(teleport, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        playerToggle.remove(player.getUniqueId());
    }

    @EventHandler
    public void PlayerTargetEntity(EntityTargetLivingEntityEvent event) {
        if (!enableFriendlyEnderman) return;
        if (!event.getEntity().getType().equals(EntityType.ENDERMAN)) return;
        Player player = (Player) event.getTarget();
        assert player != null;
        event.setCancelled(playerData.contains(player.getUniqueId()));
    }

    @EventHandler
    public void InventoryClickEvent(InventoryClickEvent event) {
        if (!enablePumpkinInvisibility) return;
        Player player = (Player)event.getWhoClicked();
        if (event.getSlot() != 39) return;

        if (player.getItemOnCursor().getType().equals(Material.CARVED_PUMPKIN)) playerInvisible.add(player.getUniqueId());
        else playerInvisible.remove(player.getUniqueId());
        updateInvisiblePlayer(player);
    }

    @EventHandler
    public void PlayerJoinEvent(PlayerJoinEvent event) {
        if (!enablePumpkinInvisibility) return;
        Player player = event.getPlayer();

        // If enderman player joins, hide all players in playerInvisible list
        if (playerData.contains(player.getUniqueId())) updateEndermanInvisible(player);

        // Checked if player has carved pumpkin on head and put him on invisible list
        ItemStack item = player.getInventory().getItem(39);
        if (item == null) return;
        if (item.getType().equals(Material.CARVED_PUMPKIN)) playerInvisible.add(player.getUniqueId());
        updateInvisiblePlayer(player);
    }

    private void updateInvisiblePlayer(Player target) {
        for(UUID entry : playerData) {
            Player source = Bukkit.getPlayer(entry);
            if (source == null) continue;

            if (playerInvisible.contains(target.getUniqueId())) source.hidePlayer(this, target);
            else source.showPlayer(this, target);
        }
    }

    private void updateEndermanInvisible(Player source) {
        for (UUID entry : playerInvisible) {
            Player player = Bukkit.getPlayer(entry);
            if (player == null) continue;
            source.hidePlayer(this, player);
        }
    }

    private void playerUnhideAll(Player source) {
        for (UUID entry : playerInvisible) {
            Player player = Bukkit.getPlayer(entry);
            if (player == null) continue;
            source.showPlayer(this, player);
        }
    }

    private Location firstEmptyBlock(Location location) {
        while (!location.clone().add(0, 1, 0).getBlock().getType().equals(Material.AIR) ||
                !location.clone().add(0, 2, 0).getBlock().getType().equals(Material.AIR)) {
            location.add(0, 2, 0);
        }
        return location;
    }

    private void sendMessage(Player player, String text) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', text));
    }
}