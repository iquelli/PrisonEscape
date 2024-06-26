package net.tiagofar78.prisonescape.game.prisonbuilding;

import net.tiagofar78.prisonescape.items.ToolItem;
import net.tiagofar78.prisonescape.managers.MessageLanguageManager;

import org.bukkit.Location;

public abstract class Obstacle {

    private static final int STARTING_DURABILITY = 100;

    private double _durability = STARTING_DURABILITY;

    public double getDurability() {
        return _durability;
    }

    /**
     * @return 0 if durability is now 0 or less<br>
     *         durability value if the durability is higher than 0<br>
     *         -1 if the tool can not be used to this block
     *
     */
    public double takeDamage(ToolItem tool) {
        if (!isEffectiveTool(tool)) {
            return -1;
        }

        tool.decreaseDurability();
        _durability -= tool.damageToBlock();

        return _durability <= 0 ? 0 : _durability;
    }

    public abstract boolean isEffectiveTool(ToolItem tool);

    public abstract String getEffectiveToolMessage(MessageLanguageManager messages);

    public abstract boolean contains(Location location);

    public abstract void removeFromWorld();

}
