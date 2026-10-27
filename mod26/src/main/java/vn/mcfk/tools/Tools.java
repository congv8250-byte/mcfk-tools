package vn.mcfk.tools;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

/**
 * Công cụ đặc biệt = item vanilla + thẻ custom_data, nên người chơi không cần cài mod vẫn vào được server.
 */
public enum Tools {
    PICKAXE("pickaxe", "mcfk_pickaxe3x3", Items.DIAMOND_PICKAXE, "Cúp 3x3", "Đào vùng 3x3. Giữ Shift để đào 1 block."),
    SHOVEL("shovel", "mcfk_shovel3x3", Items.DIAMOND_SHOVEL, "Xẻng 3x3", "Đào vùng 3x3. Giữ Shift để đào 1 block."),
    AXE("axe", "mcfk_treeaxe", Items.DIAMOND_AXE, "Rìu Chặt Cây", "Chặt cả cây chỉ với một nhát."),
    WATER("water", "mcfk_infwater", Items.WATER_BUCKET, "Xô Nước Vô Hạn", "Đặt nước mãi mãi, xô không bao giờ cạn.");

    public final String arg;
    public final String key;
    public final Item base;
    public final String title;
    public final String lore;

    Tools(String arg, String key, Item base, String title, String lore) {
        this.arg = arg;
        this.key = key;
        this.base = base;
        this.title = title;
        this.lore = lore;
    }

    public ItemStack create() {
        ItemStack s = new ItemStack(base);
        CompoundTag t = new CompoundTag();
        t.putBoolean(key, true);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        s.set(DataComponents.CUSTOM_NAME,
                Component.literal(title).withStyle(ChatFormatting.AQUA).withStyle(st -> st.withItalic(false)));
        s.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal(lore).withStyle(ChatFormatting.GRAY).withStyle(st -> st.withItalic(false)))));
        s.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return s;
    }

    public boolean matches(ItemStack s) {
        if (s == null || s.isEmpty()) return false;
        CustomData cd = s.get(DataComponents.CUSTOM_DATA);
        return cd != null && cd.copyTag().contains(key);
    }

    public static Tools byArg(String a) {
        for (Tools t : values()) if (t.arg.equalsIgnoreCase(a)) return t;
        return null;
    }
}
