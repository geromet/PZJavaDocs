/*
 * Decompiled with CFR 0.152.
 */
package zombie.util;

import java.util.function.BiFunction;

public class StringUtils {
    public static final String s_emptyString = "";
    public static final char UTF8_BOM = '\ufeff';

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static boolean isNullOrWhitespace(String s) {
        return StringUtils.isNullOrEmpty(s) || StringUtils.isWhitespace(s);
    }

    private static boolean isWhitespace(String s) {
        int length = s.length();
        if (length > 0) {
            int start = 0;
            int middle = length / 2;
            int end = length - 1;
            while (start <= middle) {
                if (!Character.isWhitespace(s.charAt(start)) || !Character.isWhitespace(s.charAt(end))) {
                    return false;
                }
                ++start;
                --end;
            }
            return true;
        }
        return false;
    }

    public static String discardNullOrWhitespace(String str) {
        return StringUtils.isNullOrWhitespace(str) ? null : str;
    }

    public static String trimPrefix(String str, String prefix) {
        if (str.startsWith(prefix)) {
            return str.substring(prefix.length());
        }
        return str;
    }

    public static String trimSuffix(String str, String suffix) {
        if (str.endsWith(suffix)) {
            return str.substring(0, str.length() - suffix.length());
        }
        return str;
    }

    public static boolean equals(String a, String b) {
        if (a == b) {
            return true;
        }
        return a != null && a.equals(b);
    }

    public static boolean startsWithIgnoreCase(String str, String prefix) {
        return str.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    public static boolean endsWithIgnoreCase(String str, String suffix) {
        int suffixLength = suffix.length();
        return str.regionMatches(true, str.length() - suffixLength, suffix, 0, suffixLength);
    }

    public static boolean containsIgnoreCase(String haystack, String needle) {
        for (int i = haystack.length() - needle.length(); i >= 0; --i) {
            if (!haystack.regionMatches(true, i, needle, 0, needle.length())) continue;
            return true;
        }
        return false;
    }

    public static boolean equalsIgnoreCase(String a, String b) {
        if (a == b) {
            return true;
        }
        if (StringUtils.isNullOrEmpty(a) && StringUtils.isNullOrEmpty(b)) {
            return true;
        }
        return a != null && a.equalsIgnoreCase(b);
    }

    public static boolean tryParseBoolean(String varStr) {
        if (StringUtils.isNullOrWhitespace(varStr)) {
            return false;
        }
        String processedVar = varStr.trim();
        return processedVar.equalsIgnoreCase("true") || processedVar.equals("1") || processedVar.equals("1.0");
    }

    public static boolean isBoolean(String varStr) {
        String processedVar = varStr.trim();
        if (processedVar.equalsIgnoreCase("true") || processedVar.equals("1") || processedVar.equals("1.0")) {
            return true;
        }
        return processedVar.equalsIgnoreCase("false") || processedVar.equals("0") || processedVar.equals("0.0");
    }

    public static boolean isFloat(String varStr) {
        if (StringUtils.isNullOrWhitespace(varStr)) {
            return false;
        }
        try {
            Float.parseFloat(varStr);
            return true;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }

    public static float tryParseFloat(String valueStr) {
        if (StringUtils.isNullOrWhitespace(valueStr)) {
            return 0.0f;
        }
        try {
            return Float.parseFloat(valueStr);
        }
        catch (NumberFormatException e) {
            return 0.0f;
        }
    }

    public static boolean isInt(String varStr) {
        if (StringUtils.isNullOrWhitespace(varStr)) {
            return false;
        }
        try {
            Integer.parseInt(varStr);
            return true;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }

    public static int tryParseInt(String valueStr) {
        if (StringUtils.isNullOrWhitespace(valueStr)) {
            return 0;
        }
        try {
            return Integer.parseInt(valueStr);
        }
        catch (NumberFormatException e) {
            return 0;
        }
    }

    public static <E extends Enum<E>> E tryParseEnum(Class<E> enumClass, String enumStr, E defaultVal) {
        try {
            return Enum.valueOf(enumClass, enumStr);
        }
        catch (Exception e) {
            return defaultVal;
        }
    }

    public static boolean contains(String[] array, String val, BiFunction<String, String, Boolean> equalizer) {
        return StringUtils.indexOf(array, val, equalizer) > -1;
    }

    public static int indexOf(String[] array, String val, BiFunction<String, String, Boolean> equalizer) {
        int indexOf = -1;
        for (int i = 0; i < array.length; ++i) {
            if (!equalizer.apply(array[i], val).booleanValue()) continue;
            indexOf = i;
            break;
        }
        return indexOf;
    }

    public static String indent(String text) {
        String firstLineTab = s_emptyString;
        String secondLineTab = "\t";
        return StringUtils.indent(text, s_emptyString, "\t");
    }

    private static String indent(String text, String firstLineTab, String secondLineTab) {
        String endln = System.lineSeparator();
        return StringUtils.indent(text, endln, firstLineTab, secondLineTab);
    }

    private static String indent(String text, String endln, String firstLineTab, String secondLineTab) {
        if (StringUtils.isNullOrEmpty(text)) {
            return text;
        }
        int length = text.length();
        StringBuilder out = new StringBuilder(length);
        StringBuilder line = new StringBuilder(length);
        int lineIdx = 0;
        block4: for (int i = 0; i < length; ++i) {
            char c = text.charAt(i);
            switch (c) {
                case '\r': {
                    continue block4;
                }
                case '\n': {
                    out.append((CharSequence)line);
                    out.append(endln);
                    line.setLength(0);
                    ++lineIdx;
                    continue block4;
                }
                default: {
                    if (line.isEmpty()) {
                        if (lineIdx == 0) {
                            line.append(firstLineTab);
                        } else {
                            line.append(secondLineTab);
                        }
                    }
                    line.append(c);
                }
            }
        }
        out.append((CharSequence)line);
        line.setLength(0);
        return out.toString();
    }

    public static String leftJustify(String text, int length) {
        if (text == null) {
            return StringUtils.leftJustify(s_emptyString, length);
        }
        int inTextLength = text.length();
        if (inTextLength >= length) {
            return text;
        }
        int difference = length - inTextLength;
        char[] differenceChars = new char[difference];
        for (int i = 0; i < difference; ++i) {
            differenceChars[i] = 32;
        }
        String suffix = new String(differenceChars);
        return text + suffix;
    }

    public static String moduleDotType(String module, String type) {
        if (type == null) {
            return null;
        }
        if (type.contains(".")) {
            return type;
        }
        return module + "." + type;
    }

    public static String stripModule(String type) {
        if (type == null) {
            return null;
        }
        int index = type.indexOf(46);
        return type.substring(index + 1);
    }

    public static String stripBOM(String line) {
        if (line != null && !line.isEmpty() && line.charAt(0) == '\ufeff') {
            return line.substring(1);
        }
        return line;
    }

    public static boolean containsDoubleDot(String str) {
        if (StringUtils.isNullOrEmpty(str)) {
            return false;
        }
        return str.contains("..") || str.contains("\u0000.\u0000.");
    }

    public static boolean containsWhitespace(String s) {
        return s.matches("(.*?)\\s(.*?)");
    }

    public static String removeWhitespace(String s) {
        return s.replaceAll("\\s", s_emptyString);
    }

    public static String[] trimArray(String[] arr) {
        for (int i = 0; i < arr.length; ++i) {
            if (arr[i] == null) continue;
            arr[i] = arr[i].trim();
        }
        return arr;
    }

    public static String trimSurroundingQuotes(String s) {
        return StringUtils.trimSurroundingQuotes(s, true);
    }

    public static String trimSurroundingQuotes(String s, boolean trim) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        if (trim) {
            s = s.trim();
        }
        if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    public static boolean isValidVariableName(String varName) {
        boolean valid = false;
        int length = varName.length();
        for (int i = 0; i < length; ++i) {
            char ch = varName.charAt(i);
            if (!StringUtils.isValidVariableChar(ch)) {
                return false;
            }
            valid = true;
        }
        return valid;
    }

    public static boolean isValidVariableChar(char ch) {
        return ch == '_' || StringUtils.isAlphaNumeric(ch);
    }

    public static boolean isAlpha(char ch) {
        return ch >= 'A' && ch <= 'Z' || ch >= 'a' && ch <= 'z';
    }

    public static boolean isNumeric(char ch) {
        return ch >= '0' && ch <= '9';
    }

    public static boolean isAlphaNumeric(char ch) {
        return StringUtils.isAlpha(ch) || StringUtils.isNumeric(ch);
    }

    public static int compareIgnoreCase(String a, String b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        return a.compareToIgnoreCase(b);
    }
}

