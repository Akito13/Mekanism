package mekanism.common.content.qio;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import mekanism.api.Action;
import mekanism.common.Mekanism;
import mekanism.common.content.qio.IQIODriveItem.DriveMetadata;
import mekanism.common.lib.inventory.HashedItem;
import net.minecraft.world.item.ItemStack;

public class QIODriveData {

    private final QIODriveKey key;
    private final long countCapacity;
    private final int typeCapacity;
    private final Object2LongMap<HashedItem> itemMap = new Object2LongOpenHashMap<>();
    private long itemCount;

    public QIODriveData(QIODriveKey key) {
        this.key = key;
        ItemStack stack = key.getDriveStack();
        IQIODriveItem item = (IQIODriveItem) stack.getItem();
        // load capacity values
        countCapacity = item.getCountCapacity(stack);
        typeCapacity = item.getTypeCapacity(stack);
        // load item map from drive stack
        item.loadItemMap(stack, this);
        // update cached item count value
        itemCount = itemMap.values().longStream().sum();

        key.updateMetadata(this);
    }

    public long add(HashedItem type, long amount, Action action) {
        long stored = getStored(type);
        // fail if we've reached item count capacity or adding this item would make us exceed type capacity
        if (itemCount == countCapacity || (stored == 0 && itemMap.size() == typeCapacity)) {
            return amount;
        }
        long toAdd = Math.min(amount, countCapacity - itemCount);
        if (action.execute()) {
            itemMap.put(type, stored + toAdd);
            itemCount += toAdd;
            key.updateMetadata(this);
            key.dataUpdate();
        }
        return amount - toAdd;
    }

    public long remove(HashedItem type, long amount, Action action) {
        long stored = getStored(type);
        long removed = Math.min(amount, stored);
        if (action.execute()) {
            long remaining = stored - removed;
            if (remaining > 0) {
                itemMap.put(type, remaining);
            } else {
                itemMap.removeLong(type);
            }
            itemCount -= removed;
            key.updateMetadata(this);
            key.dataUpdate();
        }
        return removed;
    }

    public long getStored(HashedItem type) {
        return itemMap.getOrDefault(type, 0L);
    }

    public Object2LongMap<HashedItem> getItemMap() {
        return itemMap;
    }

    public QIODriveKey getKey() {
        return key;
    }

    public long getCountCapacity() {
        return countCapacity;
    }

    public int getTypeCapacity() {
        return typeCapacity;
    }

    public long getTotalCount() {
        return itemCount;
    }

    public int getTotalTypes() {
        return itemMap.size();
    }

    public record QIODriveKey(IQIODriveHolder holder, int driveSlot) {

        public void save(QIODriveData data) {
            holder.save(driveSlot, data);
        }

        public void dataUpdate() {
            holder.onDataUpdate();
        }

        public void updateMetadata(QIODriveData data) {
            ItemStack stack = getDriveStack();
            if (stack.getItem() instanceof IQIODriveItem) {
                DriveMetadata meta = new DriveMetadata(data.itemCount, data.itemMap.size());
                meta.write(stack);
            } else {
                Mekanism.logger.error("Tried to update QIO meta values on an invalid ItemStack. Something has gone very wrong!");
            }
        }

        public ItemStack getDriveStack() {
            return holder.getDriveSlots().get(driveSlot).getStack();
        }
    }
}
