package net.tiagofar78.prisonescape;

import net.tiagofar78.prisonescape.bukkit.BukkitItems;
import net.tiagofar78.prisonescape.game.PrisonEscapeGame;
import net.tiagofar78.prisonescape.game.PrisonEscapeItem;
import net.tiagofar78.prisonescape.game.prisonbuilding.ClickReturnAction;
import net.tiagofar78.prisonescape.game.prisonbuilding.PrisonEscapeLocation;
import net.tiagofar78.prisonescape.managers.ConfigManager;
import net.tiagofar78.prisonescape.managers.GameManager;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

public class Events implements Listener {

    private Map<Block, Integer> blockDurabilityMap = new HashMap<>();

//	#########################################
//	#                 Player                #
//	#########################################

    @EventHandler
    public void playerMove(PlayerMoveEvent e) {
        PrisonEscapeGame game = GameManager.getGame();
        if (game == null) {
            return;
        }

        Location bukkitLocFrom = e.getFrom();
        int xFrom = bukkitLocFrom.getBlockX();
        int yFrom = bukkitLocFrom.getBlockY();
        int zFrom = bukkitLocFrom.getBlockZ();

        Location bukkitLoc = e.getTo();
        int x = bukkitLoc.getBlockX();
        int y = bukkitLoc.getBlockY();
        int z = bukkitLoc.getBlockZ();

        if (xFrom == x && yFrom == y && zFrom == z) {
            return;
        }

        PrisonEscapeLocation location = new PrisonEscapeLocation(x, y, z);

        game.playerMove(e.getPlayer().getName(), location);
    }

    @EventHandler
    public void playerInteractWithPrison(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        Player player = e.getPlayer();
        if (block == null) {
            return;
        }

        PrisonEscapeGame game = GameManager.getGame();
        if (game == null) {
            return;
        }

        if (block.getType() == Material.DIRT || block.getType() == Material.GRASS_BLOCK) {
            int durability = 100;
            if (blockDurabilityMap.containsKey(block)) {
                durability = blockDurabilityMap.get(block);
            } else {
                blockDurabilityMap.put(block, durability);
            }

            durability -= 1;
            double progress = (double) durability / ConfigManager.getInstance().getDirtBlockMaxDurability();
            game.playerBreakDirt(player.getName(), progress);
            return;
        }

        @SuppressWarnings("deprecation")
        PrisonEscapeItem itemInHand = BukkitItems.convertToPrisonEscapeItem(
                e.getPlayer().getItemInHand()
        );
        PrisonEscapeLocation location = new PrisonEscapeLocation(block.getX(), block.getY(), block.getZ());

        int returnCode = game.playerInteractWithPrison(e.getPlayer().getName(), location, itemInHand);
        if (returnCode == 0) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void playerLeave(PlayerQuitEvent e) {
        PrisonEscapeGame game = GameManager.getGame();
        if (game == null) {
            return;
        }

        game.playerCloseMenu(e.getPlayer().getName());
    }

    @EventHandler
    public void onPlayerLoseHealth(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            World world = player.getWorld();

            if (world.getName().equals(ConfigManager.getInstance().getWorldName())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (player.getWorld().getName().equals(ConfigManager.getInstance().getWorldName())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void playerCloseInventory(InventoryCloseEvent e) {
        PrisonEscapeGame game = GameManager.getGame();
        if (game == null) {
            return;
        }

        game.playerCloseMenu(e.getPlayer().getName());
    }

    @EventHandler
    public void playerClickInventory(InventoryClickEvent e) {
        PrisonEscapeGame game = GameManager.getGame();
        if (game == null) {
            return;
        }

        if (e.getClickedInventory() == null) {
            return;
        }

        boolean isPlayerInv = false;
        if (e.getClickedInventory().getType() == InventoryType.PLAYER) {
            Inventory topInv = e.getView().getTopInventory();
            if (topInv == null) {
                e.setCancelled(true);
                return;
            }

            isPlayerInv = true;
        }

        ItemStack cursor = e.getCursor();
        ItemStack current = e.getCurrentItem();

        Player player = (Player) e.getWhoClicked();
        PrisonEscapeItem item = BukkitItems.convertToPrisonEscapeItem(e.getCursor());
        ClickReturnAction returnAction = game.playerClickMenu(player.getName(), e.getSlot(), item, isPlayerInv);
        if (returnAction == ClickReturnAction.IGNORE) {
            return;
        }

        e.setCancelled(true);

        if (returnAction == ClickReturnAction.DELETE_HOLD_AND_SELECTED) {
            player.setItemOnCursor(null);
            e.setCurrentItem(null);
        } else if (returnAction == ClickReturnAction.CHANGE_HOLD_AND_SELECTED) {
            // NOTE: cursor and current variables must be defined before game.playerClickMenu() is executed.
            player.setItemOnCursor(current);
            e.setCurrentItem(cursor);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        PrisonEscapeGame game = GameManager.getGame();
        if (game == null) {
            return;
        }

        String teamChatPrefix = ConfigManager.getInstance().getTeamChatPrefix();
        String playerName = event.getPlayer().getName();
        String message = event.getMessage();

        if (message.startsWith(teamChatPrefix)) {
            game.sendTeamOnlyMessage(playerName, message.replace(teamChatPrefix, ""));
        } else {
            game.sendGeneralMessage(playerName, message);
        }

        event.setCancelled(true);
    }

//	#########################################
//	#                 World                 #
//	#########################################

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (event.toWeatherState()) {
            event.setCancelled(true);
        }
    }
/* 
    @EventHandler
    public void onBlockBreak(BlockDamageEvent event) {
        PrisonEscapeGame game = GameManager.getGame();
        if (game == null) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (!player.getWorld().getName().equals(ConfigManager.getInstance().getWorldName())) {
            return;
        }

        if ((block.getType() != Material.DIRT) && (block.getType() != Material.GRASS_BLOCK)) {
            event.setCancelled(true);
            return;
        }

        int durability = block.getMetadata("durability").isEmpty() ? 0 : block.getMetadata("durability").get(0).asInt();
        if (durability > 0) {
            durability -= 1;
            block.setMetadata("durability", new FixedMetadataValue(PrisonEscape.getPrisonEscape(), durability));

            double progress = (double) durability / ConfigManager.getInstance().getDirtBlockMaxDurability();
            game.playerBreakDirt(player.getName(), progress);
        }
    }
*/

}
