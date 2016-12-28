package com.raoulvdberge.refinedstorage.apiimpl.network.node;

import com.raoulvdberge.refinedstorage.RS;
import com.raoulvdberge.refinedstorage.RSItems;
import com.raoulvdberge.refinedstorage.RSUtils;
import com.raoulvdberge.refinedstorage.api.network.INetworkMaster;
import com.raoulvdberge.refinedstorage.api.network.INetworkNodeHolder;
import com.raoulvdberge.refinedstorage.api.storage.AccessType;
import com.raoulvdberge.refinedstorage.api.storage.IStorage;
import com.raoulvdberge.refinedstorage.api.storage.IStorageProvider;
import com.raoulvdberge.refinedstorage.api.util.IComparer;
import com.raoulvdberge.refinedstorage.apiimpl.storage.StorageFluidNBT;
import com.raoulvdberge.refinedstorage.apiimpl.storage.StorageItemNBT;
import com.raoulvdberge.refinedstorage.block.EnumFluidStorageType;
import com.raoulvdberge.refinedstorage.block.EnumItemStorageType;
import com.raoulvdberge.refinedstorage.inventory.IItemValidator;
import com.raoulvdberge.refinedstorage.inventory.ItemHandlerBasic;
import com.raoulvdberge.refinedstorage.inventory.ItemHandlerFluid;
import com.raoulvdberge.refinedstorage.inventory.ItemHandlerListenerNetworkNode;
import com.raoulvdberge.refinedstorage.tile.IStorageGui;
import com.raoulvdberge.refinedstorage.tile.TileDiskDrive;
import com.raoulvdberge.refinedstorage.tile.config.*;
import com.raoulvdberge.refinedstorage.tile.data.TileDataParameter;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class NetworkNodeDiskDrive extends NetworkNode implements IStorageGui, IStorageProvider, IComparable, IFilterable, IPrioritizable, IType, IExcessVoidable, IAccessType {
    public static final String ID = "disk_drive";

    public class StorageItem extends StorageItemNBT {
        private int lastState;

        public StorageItem(ItemStack disk) {
            super(disk.getTagCompound(), EnumItemStorageType.getById(disk.getItemDamage()).getCapacity(), NetworkNodeDiskDrive.this);

            lastState = TileDiskDrive.getDiskState(getStored(), getCapacity());
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        @Nullable
        public ItemStack insert(@Nonnull ItemStack stack, int size, boolean simulate) {
            if (!IFilterable.canTake(itemFilters, mode, getCompare(), stack)) {
                return ItemHandlerHelper.copyStackWithSize(stack, size);
            }

            return super.insert(stack, size, simulate);
        }

        @Override
        public AccessType getAccessType() {
            return accessType;
        }

        @Override
        public boolean isVoiding() {
            return voidExcess;
        }

        @Override
        public void onStorageChanged() {
            super.onStorageChanged();

            int currentState = TileDiskDrive.getDiskState(getStored(), getCapacity());

            if (lastState != currentState) {
                lastState = currentState;

                RSUtils.updateBlock(holder.world(), holder.pos());
            }
        }
    }

    public class StorageFluid extends StorageFluidNBT {
        private int lastState;

        public StorageFluid(ItemStack disk) {
            super(disk.getTagCompound(), EnumFluidStorageType.getById(disk.getItemDamage()).getCapacity(), NetworkNodeDiskDrive.this);

            lastState = TileDiskDrive.getDiskState(getStored(), getCapacity());
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        @Nullable
        public FluidStack insert(@Nonnull FluidStack stack, int size, boolean simulate) {
            if (!IFilterable.canTakeFluids(fluidFilters, mode, getCompare(), stack)) {
                return RSUtils.copyStackWithSize(stack, size);
            }

            return super.insert(stack, size, simulate);
        }

        @Override
        public AccessType getAccessType() {
            return accessType;
        }

        @Override
        public boolean isVoiding() {
            return voidExcess;
        }

        @Override
        public void onStorageChanged() {
            super.onStorageChanged();

            int currentState = TileDiskDrive.getDiskState(getStored(), getCapacity());

            if (lastState != currentState) {
                lastState = currentState;

                RSUtils.updateBlock(holder.world(), holder.pos());
            }
        }
    }

    private static final String NBT_PRIORITY = "Priority";
    private static final String NBT_COMPARE = "Compare";
    private static final String NBT_MODE = "Mode";
    private static final String NBT_TYPE = "Type";
    private static final String NBT_VOID_EXCESS = "VoidExcess";

    private ItemHandlerBasic disks = new ItemHandlerBasic(8, new ItemHandlerListenerNetworkNode(this), IItemValidator.STORAGE_DISK) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);

            if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER) {
                RSUtils.createStorages(getStackInSlot(slot), slot, itemStorages, fluidStorages, s -> new StorageItem(s), s -> new StorageFluid(s));

                if (network != null) {
                    network.getItemStorageCache().invalidate();
                    network.getFluidStorageCache().invalidate();
                }

                RSUtils.updateBlock(holder.world(), holder.pos());
            }
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (itemStorages[slot] != null) {
                itemStorages[slot].writeToNBT();
            }

            if (fluidStorages[slot] != null) {
                fluidStorages[slot].writeToNBT();
            }

            return super.extractItem(slot, amount, simulate);
        }
    };

    private ItemHandlerBasic itemFilters = new ItemHandlerBasic(9, new ItemHandlerListenerNetworkNode(this));
    private ItemHandlerFluid fluidFilters = new ItemHandlerFluid(9, new ItemHandlerListenerNetworkNode(this));

    private StorageItem itemStorages[] = new StorageItem[8];
    private StorageFluid fluidStorages[] = new StorageFluid[8];

    private AccessType accessType = AccessType.INSERT_EXTRACT;
    private int priority = 0;
    private int compare = IComparer.COMPARE_NBT | IComparer.COMPARE_DAMAGE;
    private int mode = IFilterable.WHITELIST;
    private int type = IType.ITEMS;
    private boolean voidExcess = false;

    public NetworkNodeDiskDrive(INetworkNodeHolder holder) {
        super(holder);
    }

    public StorageItem[] getItemStorages() {
        return itemStorages;
    }

    public StorageFluid[] getFluidStorages() {
        return fluidStorages;
    }

    @Override
    public int getEnergyUsage() {
        int usage = RS.INSTANCE.config.diskDriveUsage;

        for (int i = 0; i < disks.getSlots(); ++i) {
            if (!disks.getStackInSlot(i).isEmpty()) {
                usage += RS.INSTANCE.config.diskDrivePerDiskUsage;
            }
        }

        return usage;
    }

    public void onBreak() {
        for (StorageItem storage : this.itemStorages) {
            if (storage != null) {
                storage.writeToNBT();
            }
        }

        for (StorageFluid storage : this.fluidStorages) {
            if (storage != null) {
                storage.writeToNBT();
            }
        }
    }

    @Override
    public void onConnectedStateChange(INetworkMaster network, boolean state) {
        super.onConnectedStateChange(network, state);

        network.getItemStorageCache().invalidate();
        network.getFluidStorageCache().invalidate();

        RSUtils.updateBlock(holder.world(), holder.pos());
    }

    @Override
    public void addItemStorages(List<IStorage<ItemStack>> storages) {
        for (IStorage<ItemStack> storage : this.itemStorages) {
            if (storage != null) {
                storages.add(storage);
            }
        }
    }

    @Override
    public void addFluidStorages(List<IStorage<FluidStack>> storages) {
        for (IStorage<FluidStack> storage : this.fluidStorages) {
            if (storage != null) {
                storages.add(storage);
            }
        }
    }

    @Override
    public void read(NBTTagCompound tag) {
        super.read(tag);

        RSUtils.readItems(disks, 0, tag);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public NBTTagCompound write(NBTTagCompound tag) {
        super.write(tag);

        for (int i = 0; i < disks.getSlots(); ++i) {
            if (itemStorages[i] != null) {
                itemStorages[i].writeToNBT();
            }

            if (fluidStorages[i] != null) {
                fluidStorages[i].writeToNBT();
            }
        }

        RSUtils.writeItems(disks, 0, tag);

        return tag;
    }

    @Override
    public NBTTagCompound writeConfiguration(NBTTagCompound tag) {
        super.writeConfiguration(tag);

        RSUtils.writeItems(itemFilters, 1, tag);
        RSUtils.writeItems(fluidFilters, 2, tag);

        tag.setInteger(NBT_PRIORITY, priority);
        tag.setInteger(NBT_COMPARE, compare);
        tag.setInteger(NBT_MODE, mode);
        tag.setInteger(NBT_TYPE, type);
        tag.setBoolean(NBT_VOID_EXCESS, voidExcess);

        RSUtils.writeAccessType(tag, accessType);

        return tag;
    }

    @Override
    public void readConfiguration(NBTTagCompound tag) {
        super.readConfiguration(tag);

        RSUtils.readItems(itemFilters, 1, tag);
        RSUtils.readItems(fluidFilters, 2, tag);

        if (tag.hasKey(NBT_PRIORITY)) {
            priority = tag.getInteger(NBT_PRIORITY);
        }

        if (tag.hasKey(NBT_COMPARE)) {
            compare = tag.getInteger(NBT_COMPARE);
        }

        if (tag.hasKey(NBT_MODE)) {
            mode = tag.getInteger(NBT_MODE);
        }

        if (tag.hasKey(NBT_TYPE)) {
            type = tag.getInteger(NBT_TYPE);
        }

        if (tag.hasKey(NBT_VOID_EXCESS)) {
            voidExcess = tag.getBoolean(NBT_VOID_EXCESS);
        }

        accessType = RSUtils.readAccessType(tag);
    }

    @Override
    public int getCompare() {
        return compare;
    }

    @Override
    public void setCompare(int compare) {
        this.compare = compare;

        markDirty();
    }

    @Override
    public int getMode() {
        return mode;
    }

    @Override
    public void setMode(int mode) {
        this.mode = mode;

        markDirty();
    }

    @Override
    public String getGuiTitle() {
        return "block.refinedstorage:disk_drive.name";
    }

    @Override
    public TileDataParameter<Integer> getTypeParameter() {
        return TileDiskDrive.TYPE;
    }

    @Override
    public TileDataParameter<Integer> getRedstoneModeParameter() {
        return TileDiskDrive.REDSTONE_MODE;
    }

    @Override
    public TileDataParameter<Integer> getCompareParameter() {
        return TileDiskDrive.COMPARE;
    }

    @Override
    public TileDataParameter<Integer> getFilterParameter() {
        return TileDiskDrive.MODE;
    }

    @Override
    public TileDataParameter<Integer> getPriorityParameter() {
        return TileDiskDrive.PRIORITY;
    }

    @Override
    public TileDataParameter<Boolean> getVoidExcessParameter() {
        return TileDiskDrive.VOID_EXCESS;
    }

    @Override
    public TileDataParameter<AccessType> getAccessTypeParameter() {
        return TileDiskDrive.ACCESS_TYPE;
    }

    @Override
    public String getVoidExcessType() {
        return "items_fluids";
    }

    @Override
    public int getStored() {
        int stored = 0;

        for (int i = 0; i < disks.getSlots(); ++i) {
            ItemStack disk = disks.getStackInSlot(i);

            if (!disk.isEmpty()) {
                stored += disk.getItem() == RSItems.STORAGE_DISK ? StorageItemNBT.getStoredFromNBT(disk.getTagCompound()) : StorageFluidNBT.getStoredFromNBT(disk.getTagCompound());
            }
        }

        return stored;
    }

    @Override
    public int getCapacity() {
        int capacity = 0;

        for (int i = 0; i < disks.getSlots(); ++i) {
            ItemStack disk = disks.getStackInSlot(i);

            if (!disk.isEmpty()) {
                int diskCapacity = disk.getItem() == RSItems.STORAGE_DISK ? EnumItemStorageType.getById(disk.getItemDamage()).getCapacity() : EnumFluidStorageType.getById(disk.getItemDamage()).getCapacity();

                if (diskCapacity == -1) {
                    return -1;
                }

                capacity += diskCapacity;
            }
        }

        return capacity;
    }

    @Override
    public AccessType getAccessType() {
        return accessType;
    }

    @Override
    public void setAccessType(AccessType value) {
        this.accessType = value;

        if (network != null) {
            network.getFluidStorageCache().invalidate();
            network.getItemStorageCache().invalidate();
        }

        markDirty();
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = priority;

        markDirty();
    }

    public IItemHandler getDisks() {
        return disks;
    }

    @Override
    public boolean getVoidExcess() {
        return voidExcess;
    }

    @Override
    public void setVoidExcess(boolean voidExcess) {
        this.voidExcess = voidExcess;

        markDirty();
    }

    @Override
    public int getType() {
        return holder.world().isRemote ? TileDiskDrive.TYPE.getValue() : type;
    }

    @Override
    public void setType(int type) {
        this.type = type;

        markDirty();
    }

    @Override
    public IItemHandler getFilterInventory() {
        return getType() == IType.ITEMS ? itemFilters : fluidFilters;
    }

    @Override
    public IItemHandler getDrops() {
        return disks;
    }
}
