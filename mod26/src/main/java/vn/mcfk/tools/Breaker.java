package vn.mcfk.tools;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.*;

/** Cúp/xẻng 3x3 và rìu chặt cây. Mọi block phá qua gameMode.destroyBlock nên có drop, Fortune, độ bền, bảo vệ. */
public final class Breaker {
    private static boolean busy = false;
    private static final Map<UUID, Direction> FACES = new HashMap<>();
    private static final List<Job> JOBS = new ArrayList<>();

    private static final class Job {
        final UUID player;
        final ServerLevel level;
        final Deque<BlockPos> queue;

        Job(UUID player, ServerLevel level, Deque<BlockPos> queue) {
            this.player = player;
            this.level = level;
            this.queue = queue;
        }
    }

    public static void init() {
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, be) -> {
            if (!busy && player instanceof ServerPlayer sp) FACES.put(sp.getUUID(), faceOf(sp, pos));
            return true;
        });
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, be) -> {
            if (busy) return;
            if (player instanceof ServerPlayer sp && level instanceof ServerLevel sl) onBroken(sl, sp, pos, state);
        });
        ServerTickEvents.END_SERVER_TICK.register(Breaker::tick);
    }

    private static Direction faceOf(ServerPlayer sp, BlockPos pos) {
        HitResult hr = sp.pick(6.0, 1.0F, false);
        if (hr instanceof BlockHitResult bhr && bhr.getBlockPos().equals(pos)) return bhr.getDirection();
        return Direction.orderedByNearest(sp)[0].getOpposite();
    }

    private static boolean valid(ServerLevel lv, BlockPos p, BlockState s, TagKey<Block> tag) {
        if (s.isAir() || !s.is(tag)) return false;
        if (s.getBlock() instanceof LiquidBlock) return false;
        if (s.getDestroySpeed(lv, p) < 0) return false;
        return Config.I.breakBlockEntities || lv.getBlockEntity(p) == null;
    }

    private static void onBroken(ServerLevel lv, ServerPlayer sp, BlockPos pos, BlockState state) {
        ItemStack tool = sp.getMainHandItem();
        if (sp.isShiftKeyDown() && Config.I.sneakDisables3x3) return;
        Direction face = FACES.getOrDefault(sp.getUUID(), Direction.UP);

        if (Tools.PICKAXE.matches(tool) && state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            area(lv, sp, pos, face, Tools.PICKAXE, BlockTags.MINEABLE_WITH_PICKAXE);
        } else if (Tools.SHOVEL.matches(tool) && state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            area(lv, sp, pos, face, Tools.SHOVEL, BlockTags.MINEABLE_WITH_SHOVEL);
        } else if (Tools.AXE.matches(tool) && state.is(BlockTags.LOGS)) {
            Deque<BlockPos> q = tree(lv, pos);
            if (!q.isEmpty()) JOBS.add(new Job(sp.getUUID(), lv, q));
        }
    }

    private static void area(ServerLevel lv, ServerPlayer sp, BlockPos pos, Direction face, Tools tool, TagKey<Block> tag) {
        busy = true;
        try {
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    if (a == 0 && b == 0) continue;
                    BlockPos p = switch (face.getAxis()) {
                        case Y -> pos.offset(a, 0, b);
                        case X -> pos.offset(0, a, b);
                        default -> pos.offset(a, b, 0);
                    };
                    if (!tool.matches(sp.getMainHandItem())) return; // công cụ đã hỏng
                    if (!lv.hasChunkAt(p)) continue;
                    BlockState s = lv.getBlockState(p);
                    if (valid(lv, p, s, tag)) sp.gameMode.destroyBlock(p);
                }
            }
        } finally {
            busy = false;
        }
    }

    private static Deque<BlockPos> tree(ServerLevel lv, BlockPos start) {
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> work = new ArrayDeque<>();
        List<BlockPos> out = new ArrayList<>();
        work.add(start);
        seen.add(start);
        int cap = Config.I.maxTreeBlocks;
        int r = Config.I.treeRadius;
        while (!work.isEmpty() && out.size() < cap) {
            BlockPos c = work.poll();
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dy == 0 && dz == 0) continue;
                BlockPos n = c.offset(dx, dy, dz);
                if (seen.contains(n)) continue;
                if (Math.abs(n.getX() - start.getX()) > r || Math.abs(n.getY() - start.getY()) > r
                        || Math.abs(n.getZ() - start.getZ()) > r) continue;
                if (!lv.hasChunkAt(n)) continue;
                seen.add(n);
                if (lv.getBlockState(n).is(BlockTags.LOGS)) {
                    out.add(n);
                    work.add(n);
                    if (out.size() >= cap) break;
                }
            }
        }
        if (Config.I.requireLeaves) {
            int leaves = 0;
            for (BlockPos p : out) {
                for (Direction d : Direction.values()) {
                    if (lv.getBlockState(p.relative(d)).is(BlockTags.LEAVES)) leaves++;
                }
            }
            if (leaves < 3) return new ArrayDeque<>();
        }
        out.sort(Comparator.comparingDouble(p -> p.distSqr(start)));
        return new ArrayDeque<>(out);
    }

    // xử lý dần theo tick để không lag
    private static void tick(MinecraftServer server) {
        Iterator<Job> it = JOBS.iterator();
        while (it.hasNext()) {
            Job j = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(j.player);
            if (p == null || p.level() != j.level) {
                it.remove();
                continue;
            }
            int n = 0;
            busy = true;
            try {
                while (n < Config.I.maxBlocksPerTick && !j.queue.isEmpty()) {
                    BlockPos pos = j.queue.poll();
                    if (!Tools.AXE.matches(p.getMainHandItem())) {
                        j.queue.clear();
                        break;
                    }
                    if (j.level.getBlockState(pos).is(BlockTags.LOGS)) {
                        p.gameMode.destroyBlock(pos);
                        n++;
                    }
                }
            } finally {
                busy = false;
            }
            if (j.queue.isEmpty()) it.remove();
        }
        Water.tick(server);
    }

    private Breaker() {}
}
