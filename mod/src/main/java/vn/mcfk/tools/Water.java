package vn.mcfk.tools;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Xô nước vô hạn: ghi nhận lúc người chơi dùng xô, nếu xô bị đổi thành xô rỗng thì trả lại xô vô hạn. */
public final class Water {
    private static final Map<UUID, InteractionHand> USED = new HashMap<>();

    public static void init() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            mark(player, hand);
            return InteractionResult.PASS;
        });
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            mark(player, hand);
            return InteractionResult.PASS;
        });
    }

    private static void mark(net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp && Tools.WATER.matches(sp.getItemInHand(hand))) {
            USED.put(sp.getUUID(), hand);
        }
    }

    public static void tick(MinecraftServer server) {
        if (USED.isEmpty()) return;
        for (Map.Entry<UUID, InteractionHand> e : USED.entrySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null) continue;
            ItemStack cur = p.getItemInHand(e.getValue());
            if (cur.is(Items.BUCKET) && cur.get(DataComponents.CUSTOM_DATA) == null) {
                p.setItemInHand(e.getValue(), Tools.WATER.create());
            }
        }
        USED.clear();
    }

    private Water() {}
}
