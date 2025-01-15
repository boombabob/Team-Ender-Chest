package boombabob.teamechest;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Hashtable;


public class EChestInventory implements Inventory {

    public DefaultedList<ItemStack> items;
    public Hashtable<PlayerEntity, BlockPos> playerAccessPositions = new Hashtable<>();

    EChestInventory(TeamEChests.EChestType eChestType) {
        items = DefaultedList.ofSize(eChestType.rows * TeamEChests.ROW_WIDTH, ItemStack.EMPTY);
    }

    // Quite revolutionary overrides...
    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return items.get(slot).split(amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return items.remove(slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        items.set(slot, stack);
    }

    @Override
    public void markDirty() {

    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        items.clear();
    }

    @Override
    public void onOpen(PlayerEntity player) {
        World world = player.getWorld();
        // Pretty much the same code as the ender chest open sound.
        world.playSound(
                null,
                playerAccessPositions.get(player),
                SoundEvents.BLOCK_ENDER_CHEST_OPEN,
                SoundCategory.BLOCKS,
                0.5F,
                world.random.nextFloat() * 0.1F + 0.9F
        );
    }

    @Override
    public void onClose(PlayerEntity player) {
        World world = player.getWorld();
        // Pretty much the same code as the ender chest close sound.
        world.playSound(
                null,
                playerAccessPositions.get(player),
                SoundEvents.BLOCK_ENDER_CHEST_CLOSE,
                SoundCategory.BLOCKS,
                0.5F,
                world.random.nextFloat() * 0.1F + 0.9F
        );
        // Don't need the position anymore, so delete it.
        playerAccessPositions.remove(player);
    }
}
