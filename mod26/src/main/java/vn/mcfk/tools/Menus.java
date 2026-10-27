package vn.mcfk.tools;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/** GUI rương phía server: người chơi không cần mod, không lấy được món nào ngoài nút bấm. */
public final class Menus {

    public static void openTools(ServerPlayer sp) {
        sp.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new Gui(id, inv, sp), Component.literal("Công cụ MCFK")));
    }

    private static final class Gui extends ChestMenu {
        private final ServerPlayer sp;

        Gui(int id, Inventory inv, ServerPlayer sp) {
            this(id, inv, sp, new SimpleContainer(27));
        }

        private Gui(int id, Inventory inv, ServerPlayer sp, SimpleContainer box) {
            super(MenuType.GENERIC_9x3, id, inv, box, 3);
            this.sp = sp;
            box.setItem(10, Tools.PICKAXE.create());
            box.setItem(12, Tools.SHOVEL.create());
            box.setItem(14, Tools.AXE.create());
            box.setItem(16, Tools.WATER.create());
        }

        @Override
        public void clicked(int slot, int button, ContainerInput type, Player player) {
            if (type == ContainerInput.PICKUP || type == ContainerInput.QUICK_MOVE) {
                Tools t = switch (slot) {
                    case 10 -> Tools.PICKAXE;
                    case 12 -> Tools.SHOVEL;
                    case 14 -> Tools.AXE;
                    case 16 -> Tools.WATER;
                    default -> null;
                };
                if (t != null) {
                    ItemStack s = t.create();
                    if (!sp.getInventory().add(s)) sp.drop(s, false);
                }
            }
            this.sendAllDataToRemote();
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    private Menus() {}
}
