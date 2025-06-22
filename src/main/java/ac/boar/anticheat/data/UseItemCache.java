package ac.boar.anticheat.data;

import ac.boar.anticheat.data.cache.UseDurationCache;
import ac.boar.anticheat.player.BoarPlayer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.item.Items;

import java.util.function.Consumer;

@RequiredArgsConstructor
public class UseItemCache {
    private final BoarPlayer player;

    private int previousUseItem;

    private int useItemJavaId = -1;
    private ItemData useItem = ItemData.AIR;
    private int useItemRemaining;

    private int goatHornCooldown;

    @Getter
    @Setter
    private Consumer<UseItemCache> consumer;

    public void tick() {
        if (this.goatHornCooldown > 0) {
            this.goatHornCooldown--;
        }

        if (useItemRemaining <= 0 || useItem == ItemData.AIR) {
            return;
        }

        if (player.compensatedInventory.inventoryContainer.getHeldItemData().equals(useItem, false, false, false)) {
            if (--this.useItemRemaining == 0) {
                if (player.getInputData().contains(PlayerAuthInputData.START_USING_ITEM)) {
                    this.useItemRemaining = this.previousUseItem;
                } else {
                    this.release();
                }
            }
        } else {
            this.release();
        }
    }

    public void release() {
        if (this.useItemJavaId == Items.TRIDENT.javaId()) {
            player.setDirtyRiptide(72000 - this.useItemRemaining, this.useItem);
        }

        this.useItem = ItemData.AIR;
        this.useItemJavaId = -1;
        this.useItemRemaining = 0;

        player.getFlagTracker().set(EntityFlag.USING_ITEM, false);
    }

    public boolean isUsingItem() {
        return player.getFlagTracker().has(EntityFlag.USING_ITEM);
    }

    public void use(final ItemData useItem) {
        int itemId = player.compensatedInventory.translate(useItem).getId();
        int useDuration = UseDurationCache.getUseDuration(itemId);

        if (itemId == Items.FIREWORK_ROCKET.javaId() && player.getFlagTracker().has(EntityFlag.GLIDING)) {
            player.glideBoostTicks = 20;
        }

        if (useDuration == -1) {
            return;
        }

        previousUseItem = useDuration;
        this.consumer = useItemCache -> {
            if (itemId == Items.GOAT_HORN.javaId()) {
                if (goatHornCooldown > 0) {
                    return;
                }

                this.goatHornCooldown = useDuration;
            }

            useItemCache.useItemJavaId = itemId;

            useItemCache.useItem = useItem;
            useItemCache.useItemRemaining = useDuration;

            player.getFlagTracker().set(EntityFlag.USING_ITEM, true);
        };
    }
}
