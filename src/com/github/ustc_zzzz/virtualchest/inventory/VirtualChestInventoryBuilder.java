package com.github.ustc_zzzz.virtualchest.inventory;

import com.github.ustc_zzzz.virtualchest.VirtualChestPlugin;
import com.github.ustc_zzzz.virtualchest.inventory.item.VirtualChestItem;
import com.github.ustc_zzzz.virtualchest.inventory.trigger.VirtualChestTriggerItem;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.persistence.DataBuilder;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.item.inventory.property.SlotIndex;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Optional;

/**
 * @author ustc_zzzz
 */
@NonnullByDefault
public class VirtualChestInventoryBuilder implements DataBuilder<VirtualChestInventory>
{
    private final VirtualChestPlugin plugin;

    int height = 0;
    Text title = Text.of();
    int updateIntervalTick = 0;
    VirtualChestTriggerItem triggerItem = new VirtualChestTriggerItem();
    Multimap<SlotIndex, VirtualChestItem> items = ArrayListMultimap.create();

    public VirtualChestInventoryBuilder(VirtualChestPlugin plugin)
    {
        this.plugin = plugin;
    }


    public VirtualChestInventoryBuilder title(Text title)
    {
        this.title = title;
        return this;
    }

    public VirtualChestInventoryBuilder height(int height)
    {
        this.height = height;
        return this;
    }

    public VirtualChestInventoryBuilder item(SlotIndex pos, VirtualChestItem item)
    {
        this.items.put(pos, item);
        return this;
    }

    public VirtualChestInventoryBuilder updateIntervalTick(int updateIntervalTick)
    {
        this.updateIntervalTick = updateIntervalTick;
        return this;
    }

    public VirtualChestInventoryBuilder triggerItem(VirtualChestTriggerItem triggerItem)
    {
        this.triggerItem = triggerItem;
        return this;
    }

    public VirtualChestInventory build()
    {
        if (this.title.isEmpty())
        {
            throw new InvalidDataException("Expected title");
        }
        if (this.height == 0)
        {
            throw new InvalidDataException("Expected height");
        }
        return new VirtualChestInventory(this.plugin, this);
    }

    @Override
    public Optional<VirtualChestInventory> build(DataView view) throws InvalidDataException
    {
        this.items.clear();
        for (DataQuery key : view.getKeys(false))
        {
            String keyString = key.toString();
            if (keyString.startsWith(VirtualChestInventory.KEY_PREFIX))
            {
                SlotIndex slotIndex = VirtualChestInventory.keyToSlotIndex(keyString);
                for (DataView dataView : VirtualChestItem.getViewListOrSingletonList(key, view))
                {
                    VirtualChestItem item = VirtualChestItem.deserialize(plugin, dataView);
                    this.items.put(slotIndex, item);
                }
            }
        }

        this.title = view.getString(VirtualChestInventory.TITLE)
                .map(TextSerializers.FORMATTING_CODE::deserialize)
                .orElseThrow(() -> new InvalidDataException("Expected title"));

        this.height = view.getInt(VirtualChestInventory.HEIGHT)
                .orElseThrow(() -> new InvalidDataException("Expected height"));

        this.triggerItem = view.getView(VirtualChestInventory.TRIGGER_ITEM)
                .map(VirtualChestTriggerItem::new).orElseGet(VirtualChestTriggerItem::new);

        this.updateIntervalTick = view.getInt(VirtualChestInventory.UPDATE_INTERVAL_TICK).orElse(0);

        return Optional.of(new VirtualChestInventory(this.plugin, this));
    }

    @Override
    public VirtualChestInventoryBuilder from(VirtualChestInventory value)
    {
        this.title = value.title;
        this.height = value.height;
        this.triggerItem = value.triggerItem;
        this.updateIntervalTick = value.updateIntervalTick;
        this.items.clear();
        this.items.putAll(value.items);
        return this;
    }

    @Override
    public VirtualChestInventoryBuilder reset()
    {
        this.height = 0;
        this.title = Text.of();
        this.updateIntervalTick = 0;
        this.triggerItem = new VirtualChestTriggerItem();
        this.items.clear();
        return this;
    }
}
