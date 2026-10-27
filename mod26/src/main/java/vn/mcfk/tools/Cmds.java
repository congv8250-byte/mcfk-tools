package vn.mcfk.tools;

import com.mojang.brigadier.CommandDispatcher;
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

    private static boolean isOp(CommandSourceStack s) {
        return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(s);
    }

    private static boolean canTools(CommandSourceStack s) {
        return Config.I.toolsForEveryone || isOp(s);
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
    }

    private Cmds() {}
}
