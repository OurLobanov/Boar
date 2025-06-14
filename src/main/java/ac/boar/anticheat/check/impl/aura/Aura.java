package ac.boar.anticheat.check.impl.aura;


import ac.boar.anticheat.check.api.annotations.CheckInfo;
import ac.boar.anticheat.check.api.annotations.Experimental;
import ac.boar.anticheat.check.api.impl.PacketCheck;
import ac.boar.anticheat.compensated.cache.entity.EntityCache;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.MathUtil;
import ac.boar.anticheat.util.math.Box;
import ac.boar.anticheat.util.math.Vec3;
import ac.boar.protocol.event.CloudburstPacketEvent;
import lombok.Getter;
import org.cloudburstmc.protocol.bedrock.data.GameType;
import org.cloudburstmc.protocol.bedrock.data.InputMode;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType;
import org.cloudburstmc.protocol.bedrock.packet.AnimatePacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketType;
import org.cloudburstmc.protocol.bedrock.packet.InteractPacket;
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@Experimental
@CheckInfo(name = "Aura", type = "A")
public final class Aura extends PacketCheck {

    /**
     * Буфер из трёх последних пакетов
     */
    @Getter
    private final ArrayDeque<BedrockPacketType> lastThree = new ArrayDeque<>(3);

    /**
     * Метки времени по категориям за последнюю минуту
     */
    private final Map<Category, Deque<Long>> anticheat = new EnumMap<>(Category.class);

    public Aura(BoarPlayer player) {
        super(player);

        for (Category c : Category.values()) {
            anticheat.put(c, new ArrayDeque<>());   // ← всегда кладём очередь
        }
    }

    @Override
    public void onPacketReceived(final CloudburstPacketEvent event) {
        // Not an attack packet.
        pushPacket(event.getPacket().getPacketType());

        if (!(event.getPacket() instanceof InventoryTransactionPacket packet) || packet.getActionType() != 1 || packet.getTransactionType() != InventoryTransactionType.ITEM_USE_ON_ENTITY) {
            return;
        }

        if (lastThree.size() != 3) {
            return;
        }

        BedrockPacketType[] ar = lastThree.toArray(new BedrockPacketType[0]);

        if (ar[0] == BedrockPacketType.PLAYER_AUTH_INPUT && ar[1] == BedrockPacketType.ANIMATE) {
            mark(Category.ANIMATE1);
        } else if (ar[0] == BedrockPacketType.ANIMATE && ar[1] == BedrockPacketType.ANIMATE) {
            mark(Category.ANIMATE2);
        } else if (ar[1] == BedrockPacketType.PLAYER_AUTH_INPUT) {
            mark(Category.AUTH_INPUT);
        } else {
            mark(Category.IN_LEGAL);
            for (BedrockPacketType bedrockPacketType : ar) {
                player.getSession().sendMessage("packet " + bedrockPacketType);
            }
        }

        // ChatUtil.alert(player,"d=" + distance);
    }

    public void pushPacket(BedrockPacketType pkt) {
        if (pkt == BedrockPacketType.NETWORK_STACK_LATENCY) {
            return;
        }
        if (lastThree.size() == 3) {
            lastThree.removeFirst();
        }
        lastThree.addLast(pkt);
    }

    public void mark(Category c) {
        Deque<Long> q = anticheat.computeIfAbsent(c, k -> new ArrayDeque<>());
        // теперь q никогда не null
        player.getSession().sendMessage("Marking " + c.name() + " at " + q.size());
        q.add(System.nanoTime());
    }

    public enum Category {
        ANIMATE1, ANIMATE2, AUTH_INPUT, IN_LEGAL
    }
}