package net.tiagofar78.prisonescape;

import net.tiagofar78.prisonescape.game.PEGame;
import net.tiagofar78.prisonescape.items.FunctionalItem;
import net.tiagofar78.prisonescape.items.Item;
import net.tiagofar78.prisonescape.managers.ConfigManager;
import net.tiagofar78.prisonescape.managers.GameManager;
import net.tiagofar78.prisonescape.menus.ClickReturnAction;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class Events implements Listener {

    private static final EntityType[] ALLOWED_MOBS =
            {EntityType.PRIMED_TNT, EntityType.PAINTING, EntityType.ARMOR_STAND, EntityType.ITEM_FRAME};

    @EventHandler
    public void playerMove(PlayerMoveEvent e) {
        PEGame game = GameManager.getGame();
        if (game == null) {
            return;
        }

        Location locFrom = e.getFrom();
        int xFrom = locFrom.getBlockX();
        int yFrom = locFrom.getBlockY();
        int zFrom = locFrom.getBlockZ();

        Location locTo = e.getTo();
        int x = locTo.getBlockX();
        int y = locTo.getBlockY();
        int z = locTo.getBlockZ();

        if (xFrom == x && yFrom == y && zFrom == z) {
            return;
        }

        Player player = e.getPlayer();
        game.playerMove(player.getName(), locTo, e);
    }

    @EventHandler
    public void playerInteractWithPrison(PlayerInteractEvent e) {
        PEGame game = GameManager.getGame();
        if (game == null) {
            return;
        }

        if (e.getAction() == Action.PHYSICAL) {
            return;
        }

        Block block = e.getClickedBlock();

        if (e.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        int itemSlot = e.getPlayer().getInventory().getHeldItemSlot();
        Location location = block == null ? null : block.getLocation();

        int returnCode = game.playerInteract(e.getPlayer().getName(), location, itemSlot, e);
        if (returnCode == 0) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void playerLeave(PlayerQuitEvent e) {
        PEGame game = GameManager.getGame();
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
        PEGame game = GameManager.getGame();
        if (game == null) {
            return;
        }

        InventoryType invType = e.getInventory().getType();
        if (invType == InventoryType.CRAFTING || invType == InventoryType.CREATIVE) {
            return;
        }

        game.playerCloseMenu(e.getPlayer().getName());
    }

    @EventHandler
    public void playerClickInventory(InventoryClickEvent e) {
        PEGame game = GameManager.getGame();
        if (game == null) {
            return;
        }

        if (e.getClickedInventory() == null) {
            return;
        }

        Player player = (Player) e.getWhoClicked();
        boolean isPlayerInv = e.getClickedInventory().getType() == InventoryType.PLAYER;

        if (isPlayerInv && e.getAction() == InventoryAction.DROP_ONE_SLOT) {
            if (game.playerDropItem(player.getName(), e.getSlot()) == -1) {
                e.setCancelled(true);
            }

            return;
        }

        ItemStack cursor = e.getCursor();
        ItemStack current = e.getCurrentItem();

        ClickReturnAction returnAction = game.playerClickMenu(player.getName(), e.getSlot(), isPlayerInv, e.getClick());
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
    public void onWeatherChange(WeatherChangeEvent event) {
        if (event.toWeatherState()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMobSpawn(EntitySpawnEvent e) {
        Entity entity = e.getEntity();
        if (entity instanceof Player) {
            return;
        }

        EntityType mob = e.getEntityType();
        for (EntityType allowedMob : ALLOWED_MOBS) {
            if (mob == allowedMob) {
                return;
            }
        }

        if (entity.getWorld().getName().equals(ConfigManager.getInstance().getWorldName())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        PEGame game = GameManager.getGame();
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

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (GameManager.getGame() == null && event.getPlayer().hasPermission(PrisonEscape.ADMIN_PERMISSION)) {
            return;
        }

        if (event.getPlayer().getWorld().getName().equals(ConfigManager.getInstance().getWorldName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (GameManager.getGame() == null && event.getPlayer().hasPermission(PrisonEscape.ADMIN_PERMISSION)) {
            return;
        }

        if (event.getPlayer().getWorld().getName().equals(ConfigManager.getInstance().getWorldName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockHangingBreak(HangingBreakEvent e) {
        EntityType type = e.getEntity().getType();
        for (EntityType mobType : ALLOWED_MOBS) {
            if (mobType == type) {
                e.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler
    public void onExplosion(EntityExplodeEvent e) {
        if (!e.getEntity().getWorld().getName().equals(ConfigManager.getInstance().getWorldName())) {
            return;
        }

        e.setCancelled(true);

        PEGame game = GameManager.getGame();
        if (game != null) {
            game.explosion(e.blockList());
        }
    }

    @EventHandler
    public void onPlayerInteractWithPlayer(PlayerInteractEntityEvent e) {
        PEGame game = GameManager.getGame();
        if (game == null) {
            return;
        }

        game.playerInteractWithPlayer(e.getPlayer().getName(), e.getPlayer().getInventory().getHeldItemSlot(), e);
    }

    @EventHandler
    public void onPlayerCombat(EntityDamageByEntityEvent e) {
        PEGame game = GameManager.getGame();
        if (game == null) {
            return;
        }

        Entity eAttacker = e.getDamager();
        if (!(eAttacker instanceof Player)) {
            return;
        }

        EntityType type = e.getEntity().getType();
        for (EntityType mobType : ALLOWED_MOBS) {
            if (mobType == type) {
                e.setCancelled(true);
                break;
            }
        }

        Player pAttacker = (Player) eAttacker;

        String attackerName = pAttacker.getName();
        Item item = game.getPEPlayer(attackerName).getItemAt(pAttacker.getInventory().getHeldItemSlot());

        if (item.isFunctional()) {
            ((FunctionalItem) item).use(e);
        }
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent e) {
        if (GameManager.getGame() == null) {
            return;
        }

        GameManager.getGame().playerSneak(e.getPlayer().getName());
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        PEGame game = GameManager.getGame();
        if (game == null) {
            return;
        }
        Player player = e.getPlayer();
        int slot = player.getInventory().getHeldItemSlot();

        int return_code = game.playerDropItem(player.getName(), slot);
        if (return_code == -1) {
            e.setCancelled(true);
        }
    }

}
