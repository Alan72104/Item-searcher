package a7.itemSearcher;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class Searcher {
    public boolean isOn() {
        return true;
    }

    public boolean stackMatches(ItemStack stack) {
        NBTBase nbt = getExtraAttributes(stack, "enchantments.harvesting");
        if (nbt == null)
            return false;
        return ((NBTBase.NBTPrimitive) nbt).getInt() == 6;
    }

    // Gets the nbt from a path defined like "tag1.tag2[5].tag"
    public static NBTBase getNbt(ItemStack stack, String path) {
        return getNbt(stack.getTagCompound(), path);
    }

    // Gets the nbt from ExtraAttributes from a path defined like "tag1.tag2[5].tag"
    public static NBTBase getExtraAttributes(ItemStack stack, String path) {
        return getNbt(getExtraAttributes(stack), path);
    }

    // Gets the ExtraAttributes
    public static NBTTagCompound getExtraAttributes(ItemStack stack) {
        if (stack == null)
            return null;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null)
            return null;
        return tag.getCompoundTag("ExtraAttributes");
    }

    // Gets the nbt from a path defined like "tag1.tag2[5].tag"
    public static NBTBase getNbt(NBTBase nbt, String path) {
        final int NBT_LIST = 9;
        final int NBT_COMPOUND = 10;
        if (nbt == null)
            return null;
        NBTBase cur = nbt;
        String[] eles = path.split("\\.");
        for (String ele : eles) {
            if (cur.getId() != NBT_LIST || cur.getId() != NBT_COMPOUND)
                return null;

            if (ele.endsWith("]")) {
                if (!(cur instanceof NBTTagList))
                    return null;
                NBTTagList list = (NBTTagList) cur;
                int index = Integer.parseInt(
                        ele.substring(
                                ele.indexOf('['),
                                ele.length() - 1
                        )
                ) - 1;
                if (index >= list.tagCount())
                    return null;
                cur = list.get(index);
            } else {
                cur = ((NBTTagCompound) cur).getTag(ele);
            }
        }
        return cur;
    }
}