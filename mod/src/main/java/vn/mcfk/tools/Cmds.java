package vn.mcfk.tools;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class Cmds {

    // LƯU Ý 26.x: nếu hasPermission(int) không còn, đổi sang hệ thống permission mới của Minecraft.
    private static boolean isOp(CommandSourceStack s) {
        return s.hasPermission(2);
    }

    private static boolean canTools(CommandSourceStack s) {
        return Config.I.toolsForEveryone || isOp(s);
    }

    private static void tell(ServerPlayer p, String m) {
        p.displayClientMessage(Component.literal(m), false);
    }

    private static String modeName(String m) {
        return switch (m) {
            case "OFF" -> "Tắt";
            case "ALWAYS" -> "Luôn bật";
            default -> "Giữ Shift";
        };
    }

    private static void info(ServerPlayer p) {
        Store.S s = Store.get(p.getUUID());
        tell(p, "Veinminer: " + modeName(s.mode) + " | khoảng cách " + s.range + " (tối đa " + Config.I.maxRangeCap
                + ") | số block " + s.maxBlocks + " (tối đa " + Config.I.maxBlocksCap + ")");
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        LiteralArgumentBuilder<CommandSourceStack> tools = Commands.literal("tools")
                .requires(Cmds::canTools)
                .executes(c -> {
                    Menus.openTools(c.getSource().getPlayerOrException());
                    return 1;
                })
                .then(Commands.literal("give").requires(Cmds::isOp)
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("tool", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                                new String[]{"pickaxe", "shovel", "axe", "water"}, b))
                                        .executes(c -> {
                                            ServerPlayer target = EntityArgument.getPlayer(c, "player");
                                            Tools t = Tools.byArg(StringArgumentType.getString(c, "tool"));
                                            if (t == null) {
                                                c.getSource().sendFailure(Component.literal("Loại: pickaxe, shovel, axe, water"));
                                                return 0;
                                            }
                                            ItemStack s = t.create();
                                            if (!target.getInventory().add(s)) target.drop(s, false);
                                            c.getSource().sendSuccess(() -> Component.literal("Đã đưa " + t.title + " cho "
                                                    + target.getName().getString()), false);
                                            return 1;
                                        }))))
                .then(Commands.literal("reload").requires(Cmds::isOp)
                        .executes(c -> {
                            Config.load();
                            c.getSource().sendSuccess(() -> Component.literal("Đã tải lại config."), false);
                            return 1;
                        }));
        LiteralCommandNode<CommandSourceStack> node = d.register(tools);
        d.register(Commands.literal("congcu").requires(Cmds::canTools)
                .executes(c -> {
                    Menus.openTools(c.getSource().getPlayerOrException());
                    return 1;
                })
                .redirect(node));

        d.register(Commands.literal("veinminer")
                .executes(c -> {
                    info(c.getSource().getPlayerOrException());
                    return 1;
                })
                .then(Commands.literal("gui").executes(c -> {
                    Menus.openVein(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("range").then(Commands.argument("n", IntegerArgumentType.integer(1))
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            Store.S s = Store.get(p.getUUID());
                            int want = IntegerArgumentType.getInteger(c, "n");
                            s.range = want;
                            Store.clamp(s);
                            Store.save();
                            if (want != s.range) tell(p, "Tối đa là " + Config.I.maxRangeCap + ", đã đặt " + s.range + ".");
                            info(p);
                            return 1;
                        })))
                .then(Commands.literal("max").then(Commands.argument("n", IntegerArgumentType.integer(1))
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            Store.S s = Store.get(p.getUUID());
                            int want = IntegerArgumentType.getInteger(c, "n");
                            s.maxBlocks = want;
                            Store.clamp(s);
                            Store.save();
                            if (want != s.maxBlocks) tell(p, "Tối đa là " + Config.I.maxBlocksCap + ", đã đặt " + s.maxBlocks + ".");
                            info(p);
                            return 1;
                        })))
                .then(Commands.literal("mode").then(Commands.argument("m", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"off", "sneak", "always"}, b))
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            String m = StringArgumentType.getString(c, "m").toUpperCase();
                            if (!m.equals("OFF") && !m.equals("SNEAK") && !m.equals("ALWAYS")) {
                                tell(p, "Chế độ: off, sneak hoặc always");
                                return 0;
                            }
                            Store.S s = Store.get(p.getUUID());
                            s.mode = m;
                            Store.save();
                            info(p);
                            return 1;
                        })))
                .then(Commands.literal("toggle").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Store.S s = Store.get(p.getUUID());
                    s.mode = "OFF".equals(s.mode) ? "SNEAK" : "OFF";
                    Store.save();
                    info(p);
                    return 1;
                })));
    }

    private Cmds() {}
}
