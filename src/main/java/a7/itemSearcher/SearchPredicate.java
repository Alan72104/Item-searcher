package a7.itemSearcher;

import a7.itemSearcher.utils.McUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class SearchPredicate {
    // a ar
    // a arachno resistance
    // a arachno resistance > 5
    //
    // e gk
    // e kill
    // e giant killer
    // e giant killer > 5
    private static final Matcher attributeMatcher = Pattern.compile(
            "^(?<type>[ae]) (?<path>[A-Za-z ]+)(?: ?(?<operator>==|!==|=|!=|>=|>|<=|<) ?(?<value>\\d+))?$").matcher("");
    // s
    // star
    // star >= 5
    // ms = 5
    // mstar = 5
    // p
    // p = 15
    // potato = 15
    // f
    // fuming
    // fuming = 5
    private static final Matcher countMatcher = Pattern.compile(
            "^(?<type>s|star|ms|mstar|p|potato|f|fuming)(?: ?(?<operator>==|!==|=|!=|>=|>|<=|<) ?(?<value>\\d+))?$").matcher("");
    // n tag1.tag2[1].tag3
    // n tag1.tag2[1].tag3 >= 5
    // n tag1.tag2[1].tag3 = ab
    // n tag1.tag2[1].tag3 == abc full
    // n tag1.tag2[1].tag3 == Abc full
    private static final Matcher nbtMatcher = Pattern.compile(
            "^n (?<path>(?:[A-Za-z0-9_]+(?:\\[\\d+])?\\.)*[A-Za-z0-9_]+(?:\\[\\d+])?)(?: ?(?<operator>==|!==|=|!=|>=|>|<=|<) ?(?<value>.*))?$").matcher("");
    // recomb
    // aow

    // other strings go for text search

    // Creates a predicate group from multiple search strings separated by new line or |, returns null if parse failed
    public static SearchPredicate create(String searchString) {
        String[] lines = searchString.split("\\R|\\|");
        Group group = new Group();

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty())
                continue;

            if (attributeMatcher.reset(line).matches())
                group.add(new IdSet(
                        attributeMatcher.group("path"),
                        attributeMatcher.group("operator"),
                        attributeMatcher.group("value"),
                        attributeMatcher.group("type").equals("a") ? "attributes" : "enchantments"
                ));
            else if (countMatcher.reset(line).matches()) {
                switch (countMatcher.group("type")) {
                    case "s":
                    case "star":
                        group.add(new Nbt(
                                "upgrade_level",
                                countMatcher.group("operator"),
                                countMatcher.group("value")
                        ));
                        break;
                    case "ms":
                    case "mstar":
                        group.add(new Nbt(
                                "upgrade_level",
                                countMatcher.group("operator"),
                                countMatcher.group("value"),
                                5.0
                        ));
                        break;
                    case "p":
                    case "potato":
                        group.add(new Nbt(
                                "hot_potato_count",
                                countMatcher.group("operator"),
                                countMatcher.group("value")
                        ));
                        break;
                    case "f":
                    case "fuming":
                        group.add(new Nbt(
                                "hot_potato_count",
                                countMatcher.group("operator"),
                                countMatcher.group("value"),
                                10.0
                        ));
                        break;
                }
            } else if (nbtMatcher.reset(line).matches())
                group.add(new Nbt(
                        nbtMatcher.group("path"),
                        nbtMatcher.group("operator"),
                        nbtMatcher.group("value")
                ));
            else if (line.equalsIgnoreCase("recomb"))
                group.add(new Nbt(
                        "rarity_upgrades",
                        null,
                        null
                ));
            else if (line.equalsIgnoreCase("aow"))
                group.add(new Nbt(
                        "art_of_war_count",
                        null,
                        null
                ));
            else
                group.add(new Text(line));
        }

        return group;
    }

    public abstract boolean match(ItemStack stack);

    private static class Group extends SearchPredicate {
        private final List<SearchPredicate> list = new ArrayList<>();

        public void add(SearchPredicate predicate) {
            list.add(predicate);
        }

        @Override
        public boolean match(ItemStack stack) {
            for (SearchPredicate predicate : list)
                if (!predicate.match(stack))
                    return false;
            return true;
        }
    }

    private static class Text extends SearchPredicate {
        private final String key;

        public Text(String key) {
            this.key = key;
        }

        @Override
        public boolean match(ItemStack stack) {
            NBTTagString name = (NBTTagString) getNbt(stack, "display.Name");
            if (name != null) {
                String cleaned = McUtils.cleanColor(name.getString());
                if (matchStartsOfWordsFromPossibleAbbreviation(cleaned, key))
                    return true;
            }

            NBTTagList lore = (NBTTagList) getNbt(stack, "display.Lore");
            if (lore != null) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < lore.tagCount(); i++) {
                    String loreLine = lore.getStringTagAt(i);
                    loreLine = McUtils.cleanColor(loreLine);
                    sb.append(loreLine).append(' ');
                }
                String fullLore = sb.toString();
                return matchStartsOfWordsFromPossibleAbbreviation(fullLore, key);
            }

            return false;
        }
    }

    private static abstract class Comparison extends SearchPredicate {
        protected final String key;
        @Nullable
        protected final String operator;
        @Nullable
        protected final String value;

        public Comparison(String key, @Nullable String operator, @Nullable String value) {
            this.key = key;
            this.operator = operator;
            this.value = value;
        }
    }

    private static class Nbt extends Comparison {
        private final double numberOffset;

        public Nbt(String path, String operator, String value) {
            super(path, operator, value);
            this.numberOffset = 0.0;
        }

        public Nbt(String path, String operator, String value, double numberOffset) {
            super(path, operator, value);
            this.numberOffset = numberOffset;
        }

        @Override
        public boolean match(ItemStack stack) {
            NBTBase nbt = getExtraAttributes(stack, key);

            if (nbt == null)
                return false;

            // If nbt found but no value to match for
            if (operator == null || value == null)
                return true;

            Double valueDouble = tryParseDouble(value);

            // Not using toString() because it returns number suffix and double quote
            if (valueDouble != null && nbt instanceof NBTBase.NBTPrimitive) {
                return compare(((NBTBase.NBTPrimitive) nbt).getDouble() - numberOffset, operator, valueDouble);

            } else if (nbt instanceof NBTTagString) {
                String nbtString = ((NBTTagString) nbt).getString();

                // If nbt is a string, try number parsing first
                if (valueDouble != null) {
                    Double nbtDouble = tryParseDouble(nbtString);
                    if (nbtDouble != null && compare(nbtDouble - numberOffset, operator, valueDouble))
                        return true;
                }

                return compare(nbtString, operator, value);

            } else if (valueDouble != null && nbt instanceof NBTTagList) {
                int size = ((NBTTagList) nbt).tagCount();
                return compare(size, operator, valueDouble);
            }

            return false;
        }
    }

    private static class IdSet extends Comparison {
        private final String idSetPath;

        public IdSet(String key, String operator, String value, String idSetPath) {
            super(key, operator, value);
            this.idSetPath = idSetPath;
        }

        @Override
        public boolean match(ItemStack stack) {
            return matchDoubleNbtSet(stack, idSetPath);
        }

        protected boolean matchDoubleNbtSet(ItemStack stack, String path) {
            NBTBase nbt = getExtraAttributes(stack, path);

            if (nbt == null)
                return false;

            if (nbt.getId() != NbtType.COMPOUND)
                return false;

            NBTTagCompound compound = (NBTTagCompound) nbt;

            String attrKey = findStringFromPossibleAbbreviation(compound.getKeySet(), key);

            if (attrKey == null)
                return false;

            if (operator == null || value == null)
                return true;

            double level = compound.getDouble(attrKey);

            Double valueDouble = tryParseDouble(value);
            if (valueDouble == null)
                return false;

            return compare(level, operator, valueDouble);
        }
    }

    // region Misc

    private static Double tryParseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean compare(double a, String op, double b) {
        switch (op) {
            case "=":
            case "==":
                return a == b;
            case "!=":
            case "!==":
                return a != b;
            case ">":
                return a > b;
            case ">=":
                return a >= b;
            case "<":
                return a < b;
            case "<=":
                return a <= b;
        }
        return false;
    }

    private static boolean compare(String a, String op, String b) {
        String aLower = a.toLowerCase(Locale.ROOT);
        String bLower = b.toLowerCase(Locale.ROOT);
        switch (op) {
            case "=":
                return aLower.startsWith(bLower);
            case "==":
                return aLower.equals(bLower);
            case "!=":
                return !aLower.startsWith(bLower);
            case "!==":
                return !aLower.equals(bLower);
            default:
                return false;
        }
    }

    // Finds the string or id from a possible abbreviation string, or word that starts with the string, case insensitive
    // gk matches:
    // gk
    // gkurmom
    // giant killer
    // Giant killer
    // Giant_killer
    // wi matches:
    // wisdom
    // ultimate_wisdom
    private static String findStringFromPossibleAbbreviation(Iterable<String> strings, String query) {
        query = query.toLowerCase(Locale.ROOT);

        for (String s : strings) {
            String ogString = s;
            s = s.toLowerCase(Locale.ROOT);
            s = s.replace('_', ' ');

            if (matchStartsOfWords(s, query))
                return ogString;

            // Possible abbrev
            if (query.indexOf(' ') == -1) {
                char[] chars = query.toCharArray();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < chars.length; i++) {
                    sb.append(chars[i]);
                    if (i < chars.length - 1)
                        sb.append(' ');
                }

                if (matchStartsOfWords(s, sb.toString()))
                    return ogString;
            }
        }
        return null;
    }

    // Matches any substring that its words start with the words in the query (or chars in a one word query), case insensitive
    // ck matches:
    // ck
    // can kill
    // cannot kill
    //
    // can kill matches:
    // can kill
    // cannot kill
    private static boolean matchStartsOfWordsFromPossibleAbbreviation(String s, String query) {
        s = s.toLowerCase(Locale.ROOT);
        query = query.toLowerCase(Locale.ROOT);

        if (matchStartsOfWords(s, query))
            return true;

        // Search the abbrev
        if (query.indexOf(' ') == -1) {
            char[] chars = query.toCharArray();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < chars.length; i++) {
                sb.append(chars[i]);
                if (i < chars.length - 1)
                    sb.append(' ');
            }

            return matchStartsOfWords(s, sb.toString());
        }

        return false;
    }

    // Matches any substring that its words start with the words in the query
    private static boolean matchStartsOfWords(String s, String query) {
        int lastSpaceIndex = -1;
        int spaceIndex;
        String[] queryWords = query.split(" ");
        int queryWordsIndex = 0;

        if (s.isEmpty())
            return false;

        do {
            spaceIndex = s.indexOf(' ', lastSpaceIndex + 1);
            String word;
            if (spaceIndex == -1) // If no more space, get the last word
                word = s.substring(lastSpaceIndex + 1);
            else
                word = s.substring(lastSpaceIndex + 1, spaceIndex);

            if (word.length() > 0 && word.startsWith(queryWords[queryWordsIndex])) {
                queryWordsIndex++;
                if (queryWordsIndex == queryWords.length)
                    return true;
            } else {
                queryWordsIndex = 0;
            }

            lastSpaceIndex = spaceIndex;
        } while (spaceIndex != -1);

        return false;
    }

    // endregion Misc

    // region Nbt getters

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

    // Gets the nbt from a path defined like "tag1.tag2[5].tag", case insensitive
    public static NBTBase getNbt(NBTBase nbt, String path) {
        if (nbt == null)
            return null;
        NBTBase cur = nbt;
        String[] eles = path.split("\\.");
        for (String ele : eles) {
            if (cur.getId() != NbtType.COMPOUND)
                return null;

            boolean foundKey = false;
            NBTTagCompound tagCompound = (NBTTagCompound) cur;
            Set<String> keys = tagCompound.getKeySet();
            for (String key : keys) {
                if (key.equalsIgnoreCase(ele)) {
                    cur = tagCompound.getTag(key);
                    foundKey = true;
                    break;
                }
            }

            if (!foundKey)
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
            }
        }
        return cur;
    }

    private static class NbtType {
        public static final byte END = 0;
        public static final byte BYTE = 1;
        public static final byte SHORT = 2;
        public static final byte INT = 3;
        public static final byte LONG = 4;
        public static final byte FLOAT = 5;
        public static final byte DOUBLE = 6;
        public static final byte BYTEARRAY = 7;
        public static final byte STRING = 8;
        public static final byte LIST = 9;
        public static final byte COMPOUND = 10;
        public static final byte INTARRAY = 11;
    }

    // endregion Nbt getters
}
