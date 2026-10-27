package vn.mcfk.tools;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/** GUI rương phía server: người chơi không cần mod, không lấy được món nào ngoài nút bấm. */
public final class Menus {

    public static void openTools(ServerPlayer sp) {
        sp.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new Gui(id, inv, sp, false), Component.literal("Công cụ MCFK")));
    }

    public static void openVein(ServerPlayer sp) {
        sp.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new Gui(id, inv, sp, true), Component.literal("Cài đặt Veinminer")));
    }

    static ItemStack icon(Item item, String name, String... lore) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(st -> st.withItalic(false)));
        if (lore.length > 0) {
            List<Component> l = new ArrayList<>();
            for (String x : lore) l.add(Component.literal(x).withStyle(st -> st.withItalic(false)));
            s.set(DataComponents.LORE, new ItemLore(l));
        }
        return s;
    }

    private static final class Gui extends ChestMenu {
        private final SimpleContainer box;
        private final ServerPlayer sp;
        private final boolean vein;

        Gui(int id, Inventory inv, ServerPlayer sp, boolean vein) {
            this(id, inv, sp, vein, new SimpleContainer(27));
        }

        private Gui(int id, Inventory inv, ServerPlayer sp, boolean vein, SimpleContainer box) {
            super(MenuType.GENERIC_9x3, id, inv, box, 3);
            this.box = box;
            this.sp = sp;
            this.vein = vein;
            render();
        }

        private void render() {
            ItemStack filler = icon(Items.GRAY_STAINED_GLASS_PANE, " ");
            for (int i = 0; i < 27; i++) box.setItem(i, filler.copy());
            if (!vein) {
                box.setItem(10, Tools.PICKAXE.create());
                box.setItem(12, Tools.SHOVEL.create());
                box.setItem(14, Tools.AXE.create());
                box.setItem(16, Tools.WATER.create());
                box.setItem(22, icon(Items.IRON_PICKAXE, "Cài đặt Veinminer", "Bấm để chỉnh khoảng cách liên kết"));
            } else {
                Store.S s = Store.get(sp.getUUID());
                box.setItem(4, icon(Items.BOOK, "Veinminer", "Đào cả mạch quặng cùng loại"));
                box.setItem(10, icon(Items.RED_STAINED_GLASS_PANE, "- Giảm khoảng cách"));
                box.setItem(11, icon(Items.ENDER_EYE, "Khoảng cách liên kết: " + s.range,
                        "Tối đa " + Config.I.maxRangeCap, "1 = chỉ block chạm nhau"));
                box.setItem(12, icon(Items.LIME_STAINED_GLASS_PANE, "+ Tăng khoảng cách"));
                box.setItem(14, icon(Items.RED_STAINED_GLASS_PANE, "- Giảm số block", "Shift-click: giảm 32"));
                box.setItem(15, icon(Items.CHEST, "Số block tối đa: " + s.maxBlocks, "Tối đa " + Config.I.maxBlocksCap));
                box.setItem(16, icon(Items.LIME_STAINED_GLASS_PANE, "+ Tăng số block", "Shift-click: tăng 32"));
                box.setItem(22, icon(Items.COMPARATOR, "Chế độ: " + modeName(s.mode), "Bấm để đổi: Tắt / Giữ Shift / Luôn bật"));
                box.setItem(26, icon(Items.ARROW, "Quay lại"));
            }
        }

        private static String modeName(String m) {
            return switch (m) {
                case "OFF" -> "Tắt";
                case "ALWAYS" -> "Luôn bật";
                default -> "Giữ Shift";
            };
        }

        @Override
        public void clicked(int slot, int button, ClickType type, Player player) {
            if (slot >= 0 && slot < 27 && (type == ClickType.PICKUP || type == ClickType.QUICK_MOVE)) {
                handle(slot, type == ClickType.QUICK_MOVE);
            }
            this.sendAllDataToRemote();
        }

        private void handle(int slot, boolean shift) {
            if (!vein) {
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
                } else if (slot == 22) {
                    Menus.openVein(sp);
                }
                return;
            }
            Store.S s = Store.get(sp.getUUID());
            int step = shift ? 32 : 8;
            switch (slot) {
                case 10 -> s.range--;
                case 12 -> s.range++;
                case 14 -> s.maxBlocks -= step;
                case 16 -> s.maxBlocks += step;
                case 22 -> s.mode = "OFF".equals(s.mode) ? "SNEAK" : "SNEAK".equals(s.mode) ? "ALWAYS" : "OFF";
                case 26 -> {
                    Menus.openTools(sp);
                    return;
                }
                default -> { return; }
            }
            Store.clamp(s);
            Store.save();
            render();
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
