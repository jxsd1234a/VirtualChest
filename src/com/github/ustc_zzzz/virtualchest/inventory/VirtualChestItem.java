package com.github.ustc_zzzz.virtualchest.inventory;

import com.github.ustc_zzzz.virtualchest.VirtualChestPlugin;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.*;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.meta.ItemEnchantment;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.item.Enchantment;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author ustc_zzzz
 */
public class VirtualChestItem
{
    public static final DataQuery ITEM = DataQuery.of("Item");
    public static final DataQuery KEEP_OPEN = DataQuery.of("KeepOpen");
    public static final DataQuery PRIMARY_ACTION = DataQuery.of("PrimaryAction");
    public static final DataQuery SECONDARY_ACTION = DataQuery.of("SecondaryAction");
    public static final DataQuery REQUIRED_PERMISSIONS = DataQuery.of("RequiredPermissions");
    public static final DataQuery REJECTED_PERMISSIONS = DataQuery.of("RejectedPermissions");

    private final VirtualChestPlugin plugin;

    private final DataView serializedStack;
    private final String primaryAction;
    private final String secondaryAction;
    private final boolean keepInventoryOpen;
    private final List<String> requiredPermissions;
    private final List<String> rejectedPermissions;

    public static DataContainer serialize(VirtualChestPlugin plugin, VirtualChestItem item) throws InvalidDataException
    {
        DataContainer data = new MemoryDataContainer().set(ITEM, item.serializedStack).set(KEEP_OPEN, item.keepInventoryOpen);
        if (!item.primaryAction.isEmpty())
        {
            data.set(PRIMARY_ACTION, item.primaryAction);
        }
        if (!item.secondaryAction.isEmpty())
        {
            data.set(SECONDARY_ACTION, item.secondaryAction);
        }
        if (!item.requiredPermissions.isEmpty())
        {
            data.set(REQUIRED_PERMISSIONS, item.requiredPermissions);
        }
        if (!item.rejectedPermissions.isEmpty())
        {
            data.set(REJECTED_PERMISSIONS, item.rejectedPermissions);
        }
        return data;
    }

    public static VirtualChestItem deserialize(VirtualChestPlugin plugin, DataView data) throws InvalidDataException
    {
        return new VirtualChestItem(plugin, data.getView(ITEM).orElseThrow(() -> new InvalidDataException("Expected Item")),
                data.getString(PRIMARY_ACTION).orElse(""), data.getString(SECONDARY_ACTION).orElse(""),
                data.getBoolean(KEEP_OPEN).orElse(Boolean.FALSE),
                data.getStringList(REQUIRED_PERMISSIONS).orElse(ImmutableList.of()),
                data.getStringList(REJECTED_PERMISSIONS).orElse(ImmutableList.of()));
    }

    private VirtualChestItem(VirtualChestPlugin plugin, DataView stack, String primaryAction, String secondaryAction,
                             boolean keeyOpen, List<String> requiredPermissions, List<String> rejectedPermissions)
    {
        this.plugin = plugin;

        this.serializedStack = stack;
        this.primaryAction = primaryAction;
        this.secondaryAction = secondaryAction;
        this.keepInventoryOpen = keeyOpen;
        this.requiredPermissions = requiredPermissions;
        this.rejectedPermissions = rejectedPermissions;
    }

    private void doAction(Player player, String actionCommand)
    {
        this.plugin.getVirtualChestActions().runCommand(player, actionCommand);
    }

    private String getActionCommand(ClickInventoryEvent event)
    {
        String actionCommand = "";
        if (event instanceof ClickInventoryEvent.Primary)
        {
            actionCommand = primaryAction;
        }
        if (event instanceof ClickInventoryEvent.Secondary)
        {
            actionCommand = secondaryAction;
        }
        return actionCommand;
    }

    public boolean setInventory(Player player, Inventory inventory)
    {
        Optional<ItemStack> stackOptional = new VirtualChestItemStackBuilder(plugin, player).build(serializedStack);
        if (stackOptional.isPresent())
        {
            for (String permission : this.requiredPermissions)
            {
                if (!player.hasPermission(permission))
                {
                    return false;
                }
            }
            for (String permission : this.rejectedPermissions)
            {
                if (player.hasPermission(permission))
                {
                    return false;
                }
            }
            inventory.set(stackOptional.get());
            return true;
        }
        else
        {
            return false;
        }
    }

    public Action fireEvent(Player player, ClickInventoryEvent event)
    {
        String actionCommandSequence = this.getActionCommand(event);
        if (!actionCommandSequence.isEmpty())
        {
            Sponge.getScheduler().createTaskBuilder().delayTicks(2).name("VirtualChestItemAction")
                    .execute(task -> this.doAction(player, actionCommandSequence)).submit(this.plugin);
        }
        return this.keepInventoryOpen ? Action.KEEP_INVENTORY_OPEN : Action.CLOSE_INVENTORY;
    }

    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("Item", this.serializedStack)
                .add("PrimaryAction", this.primaryAction)
                .add("SecondaryAction", this.secondaryAction)
                .add("KeepOpen", this.keepInventoryOpen)
                .add("RequiredPermissions", this.requiredPermissions)
                .add("RejectedPermissions", this.rejectedPermissions)
                .toString();
    }

    public enum Action
    {
        CLOSE_INVENTORY, KEEP_INVENTORY_OPEN
    }
}
